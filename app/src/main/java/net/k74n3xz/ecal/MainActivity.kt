package net.k74n3xz.ecal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.k74n3xz.ecal.ui.components.MainScreen
import net.k74n3xz.ecal.ui.theme.ECALTheme
import net.k74n3xz.ecal.ui.viewmodel.MainScreenViewModel

class MainActivity : ComponentActivity() {
    private val mainScreenViewModel: MainScreenViewModel by viewModels { MainScreenViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ECALTheme {
                MainScreen(
                    mainScreenViewModel, modifier = Modifier.padding(top = 32.dp)
                )
            }
        }
    }
}

/*
* TODO: Add a preview for MainActivity.
* @Preview(showBackground = true)
* @Composable
* fun MainActivityPreview() {
*     val viewModel = MainScreenViewModel.Factory.create(MainScreenViewModel::class.java)
*
*     ECALTheme {
*         MainScreen(viewModel, modifier = Modifier.padding(top = 32.dp))
*     }
* }
* */