package com.fuzzyfilesearch.searchbox

import com.fuzzyfilesearch.settings.GlobalSettings
import java.awt.Color
import java.awt.Font
import javax.swing.UIManager


fun getFont(settings: GlobalSettings.SettingsState): Font {
    var fontName = ""
    if (settings.common.useDefaultFont) {
        fontName = UIManager.getFont("Label.font").fontName
    } else  {
        fontName = settings.common.selectedFontName
    }
    return Font(fontName, Font.PLAIN, settings.common.fontSize)
}

fun colorToHexWithAlpha(color: Color?): String {
    if (color == null) {
        return ""
    }
    return "#%02X%02X%02X%02X".format(color.red, color.green, color.blue, color.alpha)
}

fun hexToColorWithAlpha(hex: String): Color? {
    if (hex.length < 9) {
        return null
    }
    return Color(
        Integer.valueOf(hex.substring(1, 3), 16),
        Integer.valueOf(hex.substring(3, 5), 16),
        Integer.valueOf(hex.substring(5, 7), 16),
        Integer.valueOf(hex.substring(7, 9), 16)
    )
}

