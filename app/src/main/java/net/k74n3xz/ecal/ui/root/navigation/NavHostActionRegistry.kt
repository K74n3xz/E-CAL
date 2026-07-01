package net.k74n3xz.ecal.ui.root.navigation

import androidx.navigation3.runtime.NavKey

class NavHostActionRegistry {
    private val map: MutableMap<NavKey, NavHostAction> = mutableMapOf()

    operator fun get(key: NavKey): NavHostAction? = map[key]

    fun register(key: NavKey, action: NavHostAction) {
        map[key] = action
    }

    fun unregister(key: NavKey) {
        map.remove(key)
    }
}