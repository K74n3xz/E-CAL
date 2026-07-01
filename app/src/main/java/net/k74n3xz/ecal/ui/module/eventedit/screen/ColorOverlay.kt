package net.k74n3xz.ecal.ui.module.eventedit.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.ui.compositionlocal.LocalTimeZone
import java.time.ZoneId

@Composable
fun ColorOverlay(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.Black.copy(alpha = 0.25f),
    contentOnOverlay: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        content()

        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color)
                    .clickable(onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                if (contentOnOverlay != null) {
                    contentOnOverlay()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ColorOverlayPreview() {
    CompositionLocalProvider(LocalTimeZone provides ZoneId.systemDefault()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ColorOverlay(
                enabled = true,
                contentOnOverlay = { CircularProgressIndicator() }
            ) {
                EventEditScreen(
                    event = Event(),
                    onCancel = {},
                    onSave = {}
                )
            }
        }
    }
}