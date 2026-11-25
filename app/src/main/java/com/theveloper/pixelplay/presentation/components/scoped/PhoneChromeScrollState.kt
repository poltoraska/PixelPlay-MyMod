package com.theveloper.pixelplay.presentation.components.scoped

/** Hysteresis in pixels; only the list's consumed, direct vertical drag is supplied here. */
internal class PhoneChromeScrollState(private val collapseDistance: Float, private val expandDistance: Float) {
    var collapsed: Boolean = false
        private set
    private var distance = 0f
    fun reset() { collapsed = false; distance = 0f }
    fun onScroll(delta: Float): Boolean {
        if (!delta.isFinite() || delta == 0f) return collapsed
        if (distance * delta < 0f) distance = 0f
        distance += delta
        if (!collapsed && distance <= -collapseDistance) { collapsed = true; distance = 0f }
        else if (collapsed && distance >= expandDistance) { collapsed = false; distance = 0f }
        return collapsed
    }
}
