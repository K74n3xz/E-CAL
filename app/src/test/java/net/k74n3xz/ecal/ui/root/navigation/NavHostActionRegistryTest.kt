package net.k74n3xz.ecal.ui.root.navigation

import net.k74n3xz.ecal.ui.module.eventedit.navigation.EventEditNavKey
import net.k74n3xz.ecal.ui.module.monthcalendar.navigation.MonthCalendarNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class NavHostActionRegistryTest {
    private val registry = NavHostActionRegistry()

    @Test
    fun registerAndGet_returnsActionForMatchingKeyOnly() {
        val action = TestAction()
        val key = EventEditNavKey("event-1")
        registry.register(key, action)

        assertSame(action, registry[key])
        assertNull(registry[EventEditNavKey("event-2")])
        assertNull(registry[MonthCalendarNavKey])
    }

    @Test
    fun register_sameKey_replacesPreviousAction() {
        val key = EventEditNavKey("event-1")
        val replacement = TestAction()
        registry.register(key, TestAction())

        registry.register(key, replacement)

        assertSame(replacement, registry[key])
    }

    @Test
    fun unregister_removesActionAndIsIdempotent() {
        val key = EventEditNavKey("event-1")
        registry.register(key, TestAction())

        registry.unregister(key)
        registry.unregister(key)

        assertNull(registry[key])
    }

    @Test
    fun unregister_oneKey_doesNotAffectAnotherKey() {
        val eventAction = TestAction()
        val monthAction = TestAction()
        val eventKey = EventEditNavKey("event-1")
        registry.register(eventKey, eventAction)
        registry.register(MonthCalendarNavKey, monthAction)

        registry.unregister(eventKey)

        assertNull(registry[eventKey])
        assertSame(monthAction, registry[MonthCalendarNavKey])
    }

    @Test
    fun replacedAction_onlyLatestActionIsInvoked() {
        val key = EventEditNavKey("event-1")
        val original = TestAction()
        val replacement = TestAction()
        registry.register(key, original)
        registry.register(key, replacement)

        registry[key]!!.requestBack()

        assertEquals(0, original.calls)
        assertEquals(1, replacement.calls)
    }
}

private class TestAction : NavHostAction {
    var calls = 0
    override fun requestBack() {
        calls++
    }
}
