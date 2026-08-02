package net.k74n3xz.ecal.platform.android.components.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT
import android.media.MediaPlayer
import android.media.RingtoneManager.TYPE_RINGTONE
import android.media.RingtoneManager.getDefaultUri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.application.port.`in`.service.AttachmentQueryService
import net.k74n3xz.ecal.platform.android.constant.Action
import net.k74n3xz.ecal.platform.android.constant.Notification as NotificationConstant
import net.k74n3xz.ecal.platform.android.constant.RequestCode
import net.k74n3xz.ecal.platform.android.helper.notification.ReminderNotificationHelper

@AndroidEntryPoint
class AudioPlayerService : Service() {
    companion object {
        private const val TAG: String = "AudioPlayerService"

        private const val AUDIO_ATTACHMENT_ID_EXTRA_KEY: String = "AUDIO_ATTACHMENT_ID"

        fun startForeground(context: Context, attachmentId: Long?) {
            val intent = Intent(context, AudioPlayerService::class.java).apply {
                action = Action.ACTION_START_AUDIO_PLAYER
                attachmentId?.let { putExtra(AUDIO_ATTACHMENT_ID_EXTRA_KEY, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    @Inject
    lateinit var attachmentQueryService: AttachmentQueryService

    @Inject
    lateinit var reminderNotificationHelper: ReminderNotificationHelper

    private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mediaPlayerJobs: MutableMap<Int, Job> = mutableMapOf()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        } else {
            if (intent.action == Action.ACTION_START_AUDIO_PLAYER) {
                val pendingIntent = PendingIntent.getService(
                    applicationContext,
                    RequestCode.AUDIO_PLAYER_STOP_ACTION_REQUEST_CODE,
                    Intent(applicationContext, this::class.java).apply {
                        action = Action.ACTION_STOP_AUDIO_PLAYER
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                ServiceCompat.startForeground(
                    /* service = */
                    this,
                    /* id = */
                    NotificationConstant.Id.REMINDER_AUDIO_PLAYER_NOTIFICATION_ID,
                    /* notification = */
                    reminderNotificationHelper.buildAudioPlayerNotification(
                        title = getString(R.string.notification_default_title_reminder),
                        text = getString(R.string.notification_text_playing_reminder_audio),
                        stopAction = NotificationCompat.Action(
                            /* icon = */
                            0,
                            /* title = */
                            getString(R.string.notification_action_stop),
                            /* intent = */
                            pendingIntent
                        )
                    ),
                    /* foregroundServiceType = */
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    } else {
                        0
                    }
                )

                val audioPlayerId: Int = startId
                val job = serviceScope.launch(start = CoroutineStart.LAZY) {
                    var audioAttachmentInputStream: FileInputStream? = null
                    var mediaPlayer: MediaPlayer? = null

                    try {
                        if (intent.hasExtra(AUDIO_ATTACHMENT_ID_EXTRA_KEY)) {
                            audioAttachmentInputStream = attachmentQueryService.openAttachmentById(
                                intent.getLongExtra(AUDIO_ATTACHMENT_ID_EXTRA_KEY, -1)
                            )
                        }

                        mediaPlayer = MediaPlayer()
                        mediaPlayer.apply {
                            if (audioAttachmentInputStream == null) {
                                setDataSource(applicationContext, getDefaultUri(TYPE_RINGTONE))
                            } else {
                                audioAttachmentInputStream.use { setDataSource(it.fd) }
                            }
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(CONTENT_TYPE_MUSIC)
                                    .setUsage(USAGE_NOTIFICATION_EVENT)
                                    .build()
                            )
                        }

                        val preparationResult = CompletableDeferred<Unit>(this.coroutineContext.job)
                        mediaPlayer.setOnPreparedListener { preparationResult.complete(Unit) }
                        mediaPlayer.setOnErrorListener { _, what, extra ->
                            preparationResult.completeExceptionally(
                                IOException("MediaPlayer error: what=$what, extra=$extra")
                            )
                            true
                        }
                        mediaPlayer.prepareAsync()
                        preparationResult.await()

                        suspendCancellableCoroutine<Unit> { continuation ->
                            mediaPlayer.setOnCompletionListener { continuation.resumeWith(Result.success(Unit)) }

                            mediaPlayer.setOnErrorListener { _, what, extra ->
                                continuation.resumeWithException(
                                    IOException("MediaPlayer error: what=$what, extra=$extra")
                                )
                                true
                            }

                            continuation.invokeOnCancellation { mediaPlayer.stop() }

                            mediaPlayer.start()
                        }
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (exception: Exception) {
                        Log.e(TAG, "onStartCommand: Failed to start the MediaPlayer.", exception)
                    } finally {
                        audioAttachmentInputStream?.close()
                        mediaPlayer?.release()
                    }
                }.apply {
                    invokeOnCompletion {
                        Handler(Looper.getMainLooper()).post {
                            if (mediaPlayerJobs.remove(audioPlayerId) != null && mediaPlayerJobs.isEmpty()) {
                                stopSelf()
                            }
                        }
                    }
                }
                mediaPlayerJobs[audioPlayerId] = job
                job.start()
            } else if (intent.action == Action.ACTION_STOP_AUDIO_PLAYER) {
                mediaPlayerJobs.values.forEach { it.cancel() }
                mediaPlayerJobs.clear()
                stopSelf()
            }

            return START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
