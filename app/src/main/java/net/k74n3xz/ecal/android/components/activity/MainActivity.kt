package net.k74n3xz.ecal.android.components.activity

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import net.k74n3xz.ecal.android.components.activity.viewmodel.AppSettingsViewModel
import net.k74n3xz.ecal.ui.compositionlocal.LocalTimeZone
import net.k74n3xz.ecal.ui.module.AppNavHost
import net.k74n3xz.ecal.ui.theme.ECALTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appSettingsViewModel: AppSettingsViewModel by viewModels()

    private val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val timeZone by appSettingsViewModel.timeZone.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalTimeZone provides timeZone) {
                ECALTheme {
                    AppNavHost()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(
                    /* action = */ Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    /* uri = */ "package:${applicationContext.packageName}".toUri()
                )
                startActivity(intent)
            }
        }
    }
}