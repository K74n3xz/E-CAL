package net.k74n3xz.ecal.ui.module.attendeemanagement.component.scaffold

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.k74n3xz.ecal.R

@Composable
internal fun AddAttendeeFabComponent(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.fab_content_description_add_attendee)
        )
    }
}
