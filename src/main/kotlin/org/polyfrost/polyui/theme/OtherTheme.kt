package org.polyfrost.polyui.theme

import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.LightColors
import org.polyfrost.polyui.color.PolyGlassDarkColors

class OtherTheme private constructor(override val colors: Colors) : Theme {


    override val switchProperties: Theme.SwitchProperties = Theme.SwitchProperties(
        notchRadius = { notchSize -> 0f },
        notchPalette = { it.brand.fg },
        radius = { size -> 0f }
    )

    override val buttonProperties: Theme.ButtonProperties = Theme.ButtonProperties(
        radius = { _, _ -> floatArrayOf(0f) }
    )

    override val checkboxProperties: Theme.CheckboxProperties = Theme.CheckboxProperties(
        radius = { 0f },
        borderSize = 0f,
        borderColor = { component.bg.normal }
    )


    companion object {
        val DARK = OtherTheme(PolyGlassDarkColors())
        val LIGHT = OtherTheme(LightColors())
    }
}
