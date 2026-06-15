package net.k74n3xz.ecal.ui.module

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import net.k74n3xz.ecal.ui.module.eventedit.navigation.EventEdit
import net.k74n3xz.ecal.ui.module.eventedit.navigation.registerEventEditEntry
import net.k74n3xz.ecal.ui.module.monthcalendar.navigation.MonthCalendar
import net.k74n3xz.ecal.ui.module.monthcalendar.navigation.registerMonthCalendarEntry

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(MonthCalendar)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            // Add the default decorators for managing scenes and saving state
            rememberSaveableStateHolderNavEntryDecorator(),
            // Then add the view model store decorator
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = { slideInHorizontally { it } togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith slideOutHorizontally { it } },
        predictivePopTransitionSpec = { EnterTransition.None togetherWith slideOutHorizontally { it / 2 } },
        entryProvider = entryProvider {
            registerMonthCalendarEntry(
                navigateToAddEvent = { backStack.add(EventEdit(null)) },
                navigateToEditEvent = { backStack.add(EventEdit(it.uid)) }
            )
            registerEventEditEntry(
                backToParent = { backStack.removeAt(backStack.size - 1) }
            )
        }
    )
}