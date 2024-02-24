/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2024 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.polyfrost.polyui.renderer.impl

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.renderer.Window
import org.polyfrost.polyui.renderer.data.Cursor
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import java.awt.event.KeyEvent.*

class AWTWindow(val title: String, width: Int, height: Int) : Window(width, height) {
    init {
        if (PolyUI.isOnMac) System.setProperty("apple.awt.application.appearance", "system")
    }

    // JFrame has to be used instead of Frame because for some reason you can only
    // fullscreen JFrame's on macOS (see com.apple.eawt.FullScreenUtilities)
    val frame = javax.swing.JFrame().apply {
        setSize(width, height)
        isAutoRequestFocus = true
        setLocationRelativeTo(null)
        background = java.awt.Color.BLACK
        isVisible = true
        createBufferStrategy(2)
    }

    override fun open(polyUI: PolyUI): Window {
        polyUI.window = this
        frame.title = title
        var offset = frame.insets.top
        frame.setSize(width, height + offset)
        var res = true

        frame.addMouseListener(object : MouseListener {
            override fun mousePressed(e: MouseEvent) {
                polyUI.inputManager.mousePressed(e.button - 1)
            }

            override fun mouseReleased(e: MouseEvent) {
                polyUI.inputManager.mouseReleased(e.button - 1)
            }

            override fun mouseEntered(e: MouseEvent) {}

            override fun mouseExited(e: MouseEvent) {}

            override fun mouseClicked(e: MouseEvent) {}
        })

        frame.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseDragged(e: MouseEvent) {
                polyUI.inputManager.mouseMoved(e.x.toFloat(), e.y.toFloat() - offset)
            }

            override fun mouseMoved(e: MouseEvent) {
                polyUI.inputManager.mouseMoved(e.x.toFloat(), e.y.toFloat() - offset)
            }
        })

        frame.addMouseWheelListener { e ->
            polyUI.inputManager.mouseScrolled(0f, e.preciseWheelRotation.toFloat())
        }

        frame.addWindowListener(object : WindowListener {
            override fun windowOpened(e: WindowEvent) {}

            override fun windowClosing(e: WindowEvent) {
                frame.isVisible = false
            }

            override fun windowClosed(e: WindowEvent) {}

            override fun windowIconified(e: WindowEvent) {}

            override fun windowDeiconified(e: WindowEvent) {}

            override fun windowActivated(e: WindowEvent) {}

            override fun windowDeactivated(e: WindowEvent) {}
        })

        frame.addComponentListener(object : ComponentListener {
            override fun componentResized(e: ComponentEvent) {
                res = true
            }

            override fun componentMoved(e: ComponentEvent) {}

            override fun componentShown(e: ComponentEvent) {}

            override fun componentHidden(e: ComponentEvent) {}
        })

        frame.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
                polyUI.inputManager.keyTyped(e.keyChar)
            }

            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == VK_UNDEFINED) return
                when (e.keyCode) {
                    VK_SHIFT -> polyUI.inputManager.addModifier(KeyModifiers.LSHIFT.value)
                    VK_CONTROL -> polyUI.inputManager.addModifier(KeyModifiers.LCONTROL.value)
                    VK_ALT -> polyUI.inputManager.addModifier(KeyModifiers.LALT.value)
                    VK_META -> polyUI.inputManager.addModifier(KeyModifiers.LMETA.value)
                    else -> {
                        polyUI.inputManager.keyDown(e.keyCode)
                    }
                }
            }

            override fun keyReleased(e: KeyEvent) {
                if (e.keyCode == VK_UNDEFINED) return
                when (e.keyCode) {
                    VK_SHIFT -> polyUI.inputManager.removeModifier(KeyModifiers.LSHIFT.value)
                    VK_CONTROL -> polyUI.inputManager.removeModifier(KeyModifiers.LCONTROL.value)
                    VK_ALT -> polyUI.inputManager.removeModifier(KeyModifiers.LALT.value)
                    VK_META -> polyUI.inputManager.removeModifier(KeyModifiers.LMETA.value)
                    else -> {
                        polyUI.inputManager.keyUp(e.keyCode)
                    }
                }
            }
        })

        var t: Long = System.currentTimeMillis()
        val fpsCap = 1.0 / 60.0
        while (frame.isVisible) {
            if (res) {
                offset = frame.insets.top
                polyUI.resize(frame.width.toFloat(), frame.height.toFloat() - offset)
                (polyUI.renderer as AWTRenderer).begin(frame)
                res = false
                polyUI.master.needsRedraw = true
            }
            polyUI.render()
            if (polyUI.drew) frame.bufferStrategy.show()
            if (fpsCap != 0.0) {
                val e = System.currentTimeMillis() - t
                if (e < fpsCap) {
                    Thread.sleep(((fpsCap - e) * 1_000.0).toLong())
                }
                t = System.currentTimeMillis()
            }
        }
        polyUI.cleanup()
        frame.dispose()
        return this
    }

    override fun close() {
        frame.isVisible = false
    }

    override fun supportsRenderPausing() = true

    override fun getClipboard(): String? {
        return Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
    }

    override fun setClipboard(text: String?) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    override fun setCursor(cursor: Cursor) {
        frame.cursor = when (cursor) {
            Cursor.Clicker -> java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)
            Cursor.Pointer -> java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR)
            Cursor.Text -> java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR)
        }
    }

    override fun getKeyName(key: Int): String {
        return getKeyText(key)
    }
}
