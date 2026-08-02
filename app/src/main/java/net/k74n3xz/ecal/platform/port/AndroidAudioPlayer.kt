package net.k74n3xz.ecal.platform.port

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import net.k74n3xz.ecal.core.application.port.outbound.platform.AudioPlayer
import net.k74n3xz.ecal.platform.android.components.service.AudioPlayerService

@Singleton
internal class AndroidAudioPlayer @Inject constructor(@param:ApplicationContext private val context: Context) :
    AudioPlayer {
    // TODO: Await confirmation that MediaPlayer has successfully started before reporting success;
    //  startForegroundService() only confirms that the service start request was accepted.
    override fun play(attachmentId: Long?): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            AudioPlayerService.startForeground(context.applicationContext, attachmentId)
            true
        } catch (_: ForegroundServiceStartNotAllowedException) {
            false
        }
    } else {
        AudioPlayerService.startForeground(context.applicationContext, attachmentId)
        true
    }
}
