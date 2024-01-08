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

package org.polyfrost.polyui.markdown

import dev.dediamondpro.minemark.providers.DefaultBrowserProvider
import dev.dediamondpro.minemark.style.*
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.renderer.data.Font
import java.awt.Color

class MarkdownStyle
    @JvmOverloads
    constructor(
        private val textStyle: MarkdownTextStyle = MarkdownTextStyle(),
        private val paragraphStyle: ParagraphStyleConfig = ParagraphStyleConfig(6f),
        private val linkStyle: LinkStyleConfig = LinkStyleConfig(Color(65, 105, 225), DefaultBrowserProvider.INSTANCE),
        private val headerStyle: HeadingStyleConfig =
            HeadingStyleConfig(
                HeadingLevelStyleConfig(32f, 12f, true, LINE_COLOR, 2f, 5f),
                HeadingLevelStyleConfig(24f, 10f, true, LINE_COLOR, 2f, 5f),
                HeadingLevelStyleConfig(19f, 8f),
                HeadingLevelStyleConfig(16f, 6f),
                HeadingLevelStyleConfig(13f, 4f),
                HeadingLevelStyleConfig(13f, 4f),
            ),
        private val horizontalRuleStyle: HorizontalRuleStyleConfig = HorizontalRuleStyleConfig(2f, 4f, LINE_COLOR),
        private val imageStyle: ImageStyleConfig = ImageStyleConfig(MarkdownImageProvider),
        private val listStyle: ListStyleConfig = ListStyleConfig(32f, 6f),
        private val blockquoteBlockStyle: BlockquoteStyleConfig = BlockquoteStyleConfig(6f, 4f, 2f, 10f, LINE_COLOR),
        private val codeBlockStyle: CodeBlockStyle = CodeBlockStyle(),
    ) : Style {
        override fun getTextStyle(): MarkdownTextStyle = textStyle

        override fun getParagraphStyle(): ParagraphStyleConfig = paragraphStyle

        override fun getLinkStyle(): LinkStyleConfig = linkStyle

        override fun getHeadingStyle(): HeadingStyleConfig = headerStyle

        override fun getHorizontalRuleStyle(): HorizontalRuleStyleConfig = horizontalRuleStyle

        override fun getImageStyle(): ImageStyleConfig = imageStyle

        override fun getListStyle(): ListStyleConfig = listStyle

        override fun getBlockquoteStyle(): BlockquoteStyleConfig = blockquoteBlockStyle

        override fun getCodeBlockStyle(): CodeBlockStyle = codeBlockStyle

        companion object {
            internal val LINE_COLOR = Color(80, 80, 80)
        }
    }

class MarkdownTextStyle(
    val normalFont: Font = PolyUI.defaultFonts.medium,
    val boldFont: Font = PolyUI.defaultFonts.bold,
    val italicNormalFont: Font = PolyUI.defaultFonts.mediumItalic,
    val italicBoldFont: Font = PolyUI.defaultFonts.boldItalic,
    defaultFontSize: Float = 16f,
    defaultTextColor: Color = Color.WHITE,
    padding: Float = (normalFont.lineSpacing - 1f) * defaultFontSize / 2f,
) : TextStyleConfig(defaultFontSize, defaultTextColor, padding)

class CodeBlockStyle(
    val codeFont: Font = PolyUI.monospaceFont,
    inlinePaddingLeftRight: Float = 2f,
    inlinePaddingTopBottom: Float = 1f,
    blockOutsidePadding: Float = 6f,
    blockInsidePadding: Float = 6f,
    color: Color = MarkdownStyle.LINE_COLOR,
) : CodeBlockStyleConfig(inlinePaddingLeftRight, inlinePaddingTopBottom, blockOutsidePadding, blockInsidePadding, color)
