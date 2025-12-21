package org.polyfrost.polyui.theme

import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.PolyColor

interface Theme {

    val colors: Colors
    val switchProperties: SwitchProperties
    val buttonProperties: ButtonProperties
    val checkboxProperties: CheckboxProperties

    interface BorderProperties {
        val borderSize: Float
        val borderColor: Colors.() -> PolyColor
    }

    data class SwitchProperties(
        val notchRadius: (notchSize: Float) -> Float,
        val notchPalette: (Colors) -> Colors.Palette,
        val radius: (size: Float) -> Float
    )

    data class ButtonProperties(
        val radius: (width: Float, height: Float) -> FloatArray,
    )

    data class CheckboxProperties(
        val radius: (size: Float) -> Float,
        override val borderSize: Float,
        override val borderColor: Colors.() -> PolyColor,
    ) : BorderProperties
}
