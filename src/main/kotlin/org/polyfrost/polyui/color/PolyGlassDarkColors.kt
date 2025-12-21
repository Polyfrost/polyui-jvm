package org.polyfrost.polyui.color

class PolyGlassDarkColors : DarkColors() {
    override val name: String = "PolyGlass Dark"
    override val page: Colors.Page = Colors.Page(
        bg = Colors.Palette(
            rgba(17, 23, 28, 0.95f),
            rgba(35, 45, 50, 0.35f),
            rgba(35, 45, 50, 0.35f),
            super.page.bg.disabled,
        ),
        super.page.bgOverlay,
        super.page.fg,
        super.page.fgOverlay,
        super.page.border20,
        rgba(255, 255, 255, 0.1f),
        rgba(255, 255, 255, 0.05f)
    )

    override val component: Colors.Component = Colors.Component(
        Colors.Palette(
            rgba(35, 45, 50, 0.7f),
            rgba(35, 45, 50, 1f),
            rgba(35, 45, 50, 0.5f),
            rgba(35, 45, 50, 0.1f),
        ),
        rgba(255, 255, 255, 0.1f)
    )


}
