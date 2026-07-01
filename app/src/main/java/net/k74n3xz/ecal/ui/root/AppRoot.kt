package net.k74n3xz.ecal.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.k74n3xz.ecal.ui.root.viewmodel.AppSettingsViewModel
import net.k74n3xz.ecal.ui.compositionlocal.LocalTimeZone
import net.k74n3xz.ecal.ui.root.navigation.AppNavHost
import net.k74n3xz.ecal.ui.theme.ECALTheme

@Composable
fun AppRoot() {
    val appSettingsViewModel: AppSettingsViewModel = hiltViewModel()
    val timeZone by appSettingsViewModel.timeZone.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalTimeZone provides timeZone) {
        ECALTheme {
            AppNavHost()
        }
    }
}