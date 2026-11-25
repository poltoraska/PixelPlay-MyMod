package com.theveloper.pixelplay.data.preferences

import org.junit.Assert.*
import org.junit.Test

class EntityTitleStyleTest {
    @Test fun `auto chooses width from rendered text rather than character count`() {
        assertEquals(EntityTitleStyle.WIDE, automaticEntityTitleStyle(150f, 300f, "album:1"))
        assertEquals(EntityTitleStyle.COMPACT, automaticEntityTitleStyle(610f, 300f, "album:1"))
    }
    @Test fun `screen width and accessibility scale can alter automatic fitting`() {
        assertEquals(EntityTitleStyle.COMPACT, automaticEntityTitleStyle(610f, 300f, "artist:2"))
        assertNotEquals(EntityTitleStyle.COMPACT, automaticEntityTitleStyle(610f, 600f, "artist:2"))
    }
    @Test fun `mid length title has a stable identity across repeated visits`() {
        val selected = automaticEntityTitleStyle(350f, 300f, "album:42")
        assertTrue(selected == EntityTitleStyle.SOFT || selected == EntityTitleStyle.ACCENT)
        repeat(20) { assertEquals(selected, automaticEntityTitleStyle(350f, 300f, "album:42")) }
    }
    @Test fun `invalid saved style safely becomes auto`() {
        assertEquals(EntityTitleStyle.AUTO, EntityTitleStyle.fromStorage("future-style"))
        assertEquals(EntityTitleStyle.AUTO, EntityTitleStyle.fromStorage(null))
        assertEquals(EntityTitleStyle.WIDE, EntityTitleStyle.fromStorage("WIDE"))
    }
    @Test fun `whole title uses Nunito when cyrillic is mixed with latin`() {
        assertTrue(titleUsesCyrillic("Кино"))
        assertTrue(titleUsesCyrillic("Live — Кино 🎵"))
        assertTrue(titleUsesCyrillic("Її пісня"))
        assertFalse(titleUsesCyrillic("Björk — Live 🎵"))
    }
    @Test fun `invalid widths cannot select an oversized profile`() {
        assertEquals(EntityTitleStyle.COMPACT, automaticEntityTitleStyle(Float.NaN, 300f, "a"))
        assertEquals(EntityTitleStyle.COMPACT, automaticEntityTitleStyle(300f, 0f, "a"))
    }
}
