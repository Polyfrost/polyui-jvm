package org.polyfrost.polyui.theme

import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.LightColors
import org.polyfrost.polyui.color.PolyGlassDarkColors

class PolyGlassTheme private constructor(override val colors: Colors) : Theme {

    override val switchProperties: Theme.SwitchProperties = Theme.SwitchProperties(
        notchRadius = { notchSize -> notchSize / 2f },
        notchPalette = { it.text.primary },
        radius = { size -> size / 1.75f }
    )

    override val buttonProperties: Theme.ButtonProperties = Theme.ButtonProperties(
        radius = { _, _ -> floatArrayOf(10f) }
    )

    override val checkboxProperties: Theme.CheckboxProperties = Theme.CheckboxProperties(
        radius = { 3f },
        borderSize = 1f,
        borderColor = { component.bg.pressed }
    )

    companion object {
        val DARK = PolyGlassTheme(PolyGlassDarkColors())
        val LIGHT = PolyGlassTheme(LightColors())
    }

}
