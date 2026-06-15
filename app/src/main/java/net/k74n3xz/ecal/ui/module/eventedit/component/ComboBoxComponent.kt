package net.k74n3xz.ecal.ui.module.eventedit.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.k74n3xz.ecal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ComboBoxComponent(
    text: String,
    items: Map<T, String>,
    onItemSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    canClear: Boolean = false,
    onClear: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {},
            modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            trailingIcon = {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (canClear) {
                                onClear()
                            }
                        },
                        modifier = Modifier.alpha(if (canClear) 1f else 0f),
                        enabled = canClear
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = stringResource(R.string.button_content_description_clear)
                        )
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(isExpanded)
                }
            }
        )

        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            scrollState = rememberScrollState(),
            matchAnchorWidth = true
        ) {
            for (item in items) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.value,
                            color = Color.Unspecified,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    onClick = {
                        onItemSelect(item.key)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

private enum class Fruit { APPLE, BANANA, CHERRY }

@Preview(showBackground = true)
@Composable
private fun ComboBoxComponentPreview() {
    val options = mapOf(Fruit.APPLE to "Apple", Fruit.BANANA to "Banana", Fruit.CHERRY to "Cherry")

    var choice: Fruit? by remember { mutableStateOf(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choice: $choice",
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )

            ComboBoxComponent(
                text = choice?.let { options[it] } ?: "",
                items = options,
                onItemSelect = { choice = it },
                canClear = choice != null,
                onClear = { choice = null }
            )
        }
    }
}