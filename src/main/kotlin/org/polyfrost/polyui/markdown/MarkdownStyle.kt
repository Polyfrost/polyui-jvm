package org.polyfrost.polyui.markdown

import dev.dediamondpro.minemark.providers.DefaultBrowserProvider
import dev.dediamondpro.minemark.providers.DefaultImageProvider
import dev.dediamondpro.minemark.style.*
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.renderer.data.Font
import java.awt.Color

class MarkdownStyle @JvmOverloads constructor(
    private val textStyle: MarkdownTextStyle = MarkdownTextStyle(),
    private val paragraphStyle: ParagraphStyleConfig = ParagraphStyleConfig(6f),
    private val linkStyle: LinkStyleConfig = LinkStyleConfig(Color(65, 105, 225), DefaultBrowserProvider.INSTANCE),
    private val headerStyle: HeadingStyleConfig = HeadingStyleConfig(
        HeadingLevelStyleConfig(32f, 12f, true, LINE_COLOR, 2f, 5f),
        HeadingLevelStyleConfig(24f, 10f, true, LINE_COLOR, 2f, 5f),
        HeadingLevelStyleConfig(19f, 8f),
        HeadingLevelStyleConfig(16f, 6f),
        HeadingLevelStyleConfig(13f, 4f),
        HeadingLevelStyleConfig(13f, 4f)
    ),
    private val horizontalRuleStyle: HorizontalRuleStyleConfig = HorizontalRuleStyleConfig(2f, 4f, LINE_COLOR),
    private val imageStyle: ImageStyleConfig = ImageStyleConfig(DefaultImageProvider.INSTANCE),
    private val listStyle: ListStyleConfig = ListStyleConfig(16f, 6f),
    private val blockquoteBlockStyle: BlockquoteStyleConfig = BlockquoteStyleConfig(6f, 4f, 2f, 10f, LINE_COLOR),
    private val codeBlockStyle: CodeBlockStyleConfig = CodeBlockStyleConfig(2f, 1f, 6f, 6f, LINE_COLOR)
) : Style {

    override fun getTextStyle(): MarkdownTextStyle = textStyle
    override fun getParagraphStyle(): ParagraphStyleConfig = paragraphStyle
    override fun getLinkStyle(): LinkStyleConfig = linkStyle
    override fun getHeadingStyle(): HeadingStyleConfig = headerStyle
    override fun getHorizontalRuleStyle(): HorizontalRuleStyleConfig = horizontalRuleStyle
    override fun getImageStyle(): ImageStyleConfig = imageStyle
    override fun getListStyle(): ListStyleConfig = listStyle
    override fun getBlockquoteStyle(): BlockquoteStyleConfig = blockquoteBlockStyle
    override fun getCodeBlockStyle(): CodeBlockStyleConfig = codeBlockStyle

    companion object {
        private val LINE_COLOR = Color(80, 80, 80);
    }
}

class MarkdownTextStyle(
    val normalFont: Font = PolyUI.defaultFonts.medium,
    val boldFont: Font = PolyUI.defaultFonts.bold,
    val italicNormalFont: Font = PolyUI.defaultFonts.mediumItalic,
    val italicBoldFont: Font = PolyUI.defaultFonts.boldItalic,
    defaultFontSize: Float = 16f,
    defaultTextColor: Color = Color.WHITE,
    padding: Float = (normalFont.lineSpacing - 1f) * defaultFontSize / 2f
) : TextStyleConfig(defaultFontSize, defaultTextColor, padding)
