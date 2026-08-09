package cc.meteormc.xposedkit.jadx.util

import jadx.gui.utils.FontUtils
import java.awt.Canvas
import java.awt.Font
import kotlin.math.abs

object FontUtil {
    private val MONOSPACE_CHARS = charArrayOf('0', '1', 'x', 'W')

    fun loadFont(str: String): Font {
        return FontUtils.loadByStr(str)
    }

    fun isMonospace(font: Font): Boolean {
        val metrics = Canvas().getFontMetrics(font)
        val width = metrics.charWidth(MONOSPACE_CHARS[0])
        return width > 1 && MONOSPACE_CHARS.drop(1).none { c -> abs(metrics.charWidth(c) - width) > 2 }
    }
}