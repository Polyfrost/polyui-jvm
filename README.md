![polyfrost-site_index – 6](https://github.com/Polyfrost/polyui-jvm/assets/62163840/768a4420-4ba3-4636-ad85-2dd89b18f936)
# PolyUI

PolyUI is a declarative UI framework developed for, and by [Polyfrost](https://polyfrost.src/main/kotlin/org/polyfrost).

It is designed to be lightweight, fast, extensible and easy to use, while still being very powerful. Make sure to check out the examples for more information.

PolyUI is split into two parts, being PolyUI and its logic, and it's a Renderer implementation. This allows us to have PolyUI in many places, including inside Minecraft!
It is **declarative**, meaning that you don't have to worry about the logic of the UI, just the layout and the components.


## Rendering Pipeline
PolyUI has the policy of ***'render what you need ONLY WHEN you need it'***.
Most of the time, PolyUI will be drawing frame buffers to the screen instead of drawing directly to the screen, as long as they are [suitably complex](src/main/kotlin/org/polyfrost/polyui/property/Settings.kt#minItemsForFramebuffer) for it to be worth it; or not drawing at all!
This allows us to have a very fast rendering pipeline, and allows us to have a lot of components on screen at once, without a performance hit.

Rendering can be [requested](src/main/kotlin/org/polyfrost/polyui/component/Component.kt#wantRedraw) by components, and if so, it will be rendered during the next frame. This should only be requested if it is necessary, for example to do an animation or something.

During a render cycle, PolyUI will systematically go through every layout, and [render](src/main/kotlin/org/polyfrost/polyui/layout/Layout.kt#reRenderIfNecessary) it to its framebuffer or to the screen. Each layout will then render its components and child layouts, and so on. Rendering happens in three steps:
 - [preRender](src/main/kotlin/org/polyfrost/polyui/component/Component.kt#preRender): This will do pre-rendering logic, such as setting up transformations, updating animations, and more.
 - [render](src/main/kotlin/org/polyfrost/polyui/component/Component.kt#render): This is where the actual rendering happens.
 - [postRender](src/main/kotlin/org/polyfrost/polyui/component/Component.kt#postRender): This will do post-rendering logic, such as cleaning up transformations.

Check out [some components](src/main/kotlin/org/polyfrost/polyui/component/impl) to see how this works.

## How it Works
 - [Components](src/main/kotlin/org/polyfrost/polyui/component/Drawable.kt) are the interactive parts of the UI, such as buttons, text fields, etc.

 - [Layouts](src/main/kotlin/org/polyfrost/polyui/layout/Layout.kt) are the containers for components, such as a grid layout, or a flex layout, etc. They are responsible for positioning and sizing the components.

 - [Properties](src/main/kotlin/org/polyfrost/polyui/property/Properties.kt) are the shared states or tokens for the components. They describe default values, and can be overridden by the components.

**Interactions** are driven by [events](src/main/kotlin/org/polyfrost/polyui/event/EventManager.kt), which thanks to Kotlin's inlining are a zero-overhead way of distributing events, such as [mouse clicks](src/main/kotlin/org/polyfrost/polyui/event/Events.kt#MouseClicked), or [key presses](src/main/kotlin/org/polyfrost/polyui/event/FocusedEvents.kt#KeyPressed).

PolyUI also supports a variety of [animations](src/main/kotlin/org/polyfrost/polyui/animate/Animation.kt) and [transitions](src/main/kotlin/org/polyfrost/polyui/animate/transitions/Transitions.kt), which can be used to make your UI more dynamic, along with dynamically adding and removing components.


## Examples
- find some [components here](src/main/kotlin/org/polyfrost/polyui/component/impl).
- find some [layouts here](src/main/kotlin/org/polyfrost/polyui/layout/impl).
- find a simple example [here](nanovg-impl/src/test/kotlin/org/polyfrost/polyui/Test.kt).
- find a rendering implementation [here](nanovg-impl/src/main/kotlin/org/polyfrost/polyui/renderer/impl/NVGRenderer.kt).
