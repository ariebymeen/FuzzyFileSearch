package com.quickfilesearch.searchbox

val normalized = mapOf(
    0x00E1.toChar() to 'a', // WITH ACUTE, LATIN SMALL LETTER
    0x0103.toChar() to 'a', // WITH BREVE, LATIN SMALL LETTER
    0x01CE.toChar() to 'a', // WITH CARON, LATIN SMALL LETTER
    0x00E2.toChar() to 'a', // WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x00E4.toChar() to 'a', // WITH DIAERESIS, LATIN SMALL LETTER
    0x0227.toChar() to 'a', // WITH DOT ABOVE, LATIN SMALL LETTER
    0x1EA1.toChar() to 'a', // WITH DOT BELOW, LATIN SMALL LETTER
    0x0201.toChar() to 'a', // WITH DOUBLE GRAVE, LATIN SMALL LETTER
    0x00E0.toChar() to 'a', // WITH GRAVE, LATIN SMALL LETTER
    0x1EA3.toChar() to 'a', // WITH HOOK ABOVE, LATIN SMALL LETTER
    0x0203.toChar() to 'a', // WITH INVERTED BREVE, LATIN SMALL LETTER
    0x0101.toChar() to 'a', // WITH MACRON, LATIN SMALL LETTER
    0x0105.toChar() to 'a', // WITH OGONEK, LATIN SMALL LETTER
    0x1E9A.toChar() to 'a', // WITH RIGHT HALF RING, LATIN SMALL LETTER
    0x00E5.toChar() to 'a', // WITH RING ABOVE, LATIN SMALL LETTER
    0x1E01.toChar() to 'a', // WITH RING BELOW, LATIN SMALL LETTER
    0x00E3.toChar() to 'a', // WITH TILDE, LATIN SMALL LETTER
    0x0363.toChar() to 'a', // COMBINING LATIN SMALL LETTER
    0x0250.toChar() to 'a', // LATIN SMALL LETTER TURNED
    0x1E03.toChar() to 'b', // WITH DOT ABOVE, LATIN SMALL LETTER
    0x1E05.toChar() to 'b', // WITH DOT BELOW, LATIN SMALL LETTER
    0x0253.toChar() to 'b', // WITH HOOK, LATIN SMALL LETTER
    0x1E07.toChar() to 'b', // WITH LINE BELOW, LATIN SMALL LETTER
    0x0180.toChar() to 'b', // WITH STROKE, LATIN SMALL LETTER
    0x0183.toChar() to 'b', // WITH TOPBAR, LATIN SMALL LETTER
    0x0107.toChar() to 'c', // WITH ACUTE, LATIN SMALL LETTER
    0x010D.toChar() to 'c', // WITH CARON, LATIN SMALL LETTER
    0x00E7.toChar() to 'c', // WITH CEDILLA, LATIN SMALL LETTER
    0x0109.toChar() to 'c', // WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x0255.toChar() to 'c', // WITH CURL, LATIN SMALL LETTER
    0x010B.toChar() to 'c', // WITH DOT ABOVE, LATIN SMALL LETTER
    0x0188.toChar() to 'c', // WITH HOOK, LATIN SMALL LETTER
    0x023C.toChar() to 'c', // WITH STROKE, LATIN SMALL LETTER
    0x0368.toChar() to 'c', // COMBINING LATIN SMALL LETTER
    0x0297.toChar() to 'c', // LATIN LETTER STRETCHED
    0x2184.toChar() to 'c', // LATIN SMALL LETTER REVERSED
    0x010F.toChar() to 'd', // WITH CARON, LATIN SMALL LETTER
    0x1E11.toChar() to 'd', // WITH CEDILLA, LATIN SMALL LETTER
    0x1E13.toChar() to 'd', // WITH CIRCUMFLEX BELOW, LATIN SMALL LETTER
    0x0221.toChar() to 'd', // WITH CURL, LATIN SMALL LETTER
    0x1E0B.toChar() to 'd', // WITH DOT ABOVE, LATIN SMALL LETTER
    0x1E0D.toChar() to 'd', // WITH DOT BELOW, LATIN SMALL LETTER
    0x0257.toChar() to 'd', // WITH HOOK, LATIN SMALL LETTER
    0x1E0F.toChar() to 'd', // WITH LINE BELOW, LATIN SMALL LETTER
    0x0111.toChar() to 'd', // WITH STROKE, LATIN SMALL LETTER
    0x0256.toChar() to 'd', // WITH TAIL, LATIN SMALL LETTER
    0x018C.toChar() to 'd', // WITH TOPBAR, LATIN SMALL LETTER
    0x0369.toChar() to 'd', // COMBINING LATIN SMALL LETTER
    0x00E9.toChar() to 'e', // WITH ACUTE, LATIN SMALL LETTER
    0x0115.toChar() to 'e', // WITH BREVE, LATIN SMALL LETTER
    0x011B.toChar() to 'e', // WITH CARON, LATIN SMALL LETTER
    0x0229.toChar() to 'e', // WITH CEDILLA, LATIN SMALL LETTER
    0x1E19.toChar() to 'e', // WITH CIRCUMFLEX BELOW, LATIN SMALL LETTER
    0x00EA.toChar() to 'e', // WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x00EB.toChar() to 'e', // WITH DIAERESIS, LATIN SMALL LETTER
    0x0117.toChar() to 'e', // WITH DOT ABOVE, LATIN SMALL LETTER
    0x1EB9.toChar() to 'e', // WITH DOT BELOW, LATIN SMALL LETTER
    0x0205.toChar() to 'e', // WITH DOUBLE GRAVE, LATIN SMALL LETTER
    0x00E8.toChar() to 'e', // WITH GRAVE, LATIN SMALL LETTER
    0x1EBB.toChar() to 'e', // WITH HOOK ABOVE, LATIN SMALL LETTER
    0x025D.toChar() to 'e', // WITH HOOK, LATIN SMALL LETTER REVERSED OPEN
    0x0207.toChar() to 'e', // WITH INVERTED BREVE, LATIN SMALL LETTER
    0x0113.toChar() to 'e', // WITH MACRON, LATIN SMALL LETTER
    0x0119.toChar() to 'e', // WITH OGONEK, LATIN SMALL LETTER
    0x0247.toChar() to 'e', // WITH STROKE, LATIN SMALL LETTER
    0x1E1B.toChar() to 'e', // WITH TILDE BELOW, LATIN SMALL LETTER
    0x1EBD.toChar() to 'e', // WITH TILDE, LATIN SMALL LETTER
    0x0364.toChar() to 'e', // COMBINING LATIN SMALL LETTER
    0x029A.toChar() to 'e', // LATIN SMALL LETTER CLOSED OPEN
    0x025E.toChar() to 'e', // LATIN SMALL LETTER CLOSED REVERSED OPEN
    0x025B.toChar() to 'e', // LATIN SMALL LETTER OPEN
    0x0258.toChar() to 'e', // LATIN SMALL LETTER REVERSED
    0x025C.toChar() to 'e', // LATIN SMALL LETTER REVERSED OPEN
    0x01DD.toChar() to 'e', // LATIN SMALL LETTER TURNED
    0x1D08.toChar() to 'e', // LATIN SMALL LETTER TURNED OPEN
    0x1E1F.toChar() to 'f', // WITH DOT ABOVE, LATIN SMALL LETTER
    0x0192.toChar() to 'f', // WITH HOOK, LATIN SMALL LETTER
    0x01F5.toChar() to 'g', //  WITH ACUTE, LATIN SMALL LETTER
    0x011F.toChar() to 'g', //  WITH BREVE, LATIN SMALL LETTER
    0x01E7.toChar() to 'g', //  WITH CARON, LATIN SMALL LETTER
    0x0123.toChar() to 'g', //  WITH CEDILLA, LATIN SMALL LETTER
    0x011D.toChar() to 'g', //  WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x0121.toChar() to 'g', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x0260.toChar() to 'g', //  WITH HOOK, LATIN SMALL LETTER
    0x1E21.toChar() to 'g', //  WITH MACRON, LATIN SMALL LETTER
    0x01E5.toChar() to 'g', //  WITH STROKE, LATIN SMALL LETTER
    0x0261.toChar() to 'g', // , LATIN SMALL LETTER SCRIPT
    0x1E2B.toChar() to 'h', //  WITH BREVE BELOW, LATIN SMALL LETTER
    0x021F.toChar() to 'h', //  WITH CARON, LATIN SMALL LETTER
    0x1E29.toChar() to 'h', //  WITH CEDILLA, LATIN SMALL LETTER
    0x0125.toChar() to 'h', //  WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x1E27.toChar() to 'h', //  WITH DIAERESIS, LATIN SMALL LETTER
    0x1E23.toChar() to 'h', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x1E25.toChar() to 'h', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x02AE.toChar() to 'h', //  WITH FISHHOOK, LATIN SMALL LETTER TURNED
    0x0266.toChar() to 'h', //  WITH HOOK, LATIN SMALL LETTER
    0x1E96.toChar() to 'h', //  WITH LINE BELOW, LATIN SMALL LETTER
    0x0127.toChar() to 'h', //  WITH STROKE, LATIN SMALL LETTER
    0x036A.toChar() to 'h', // , COMBINING LATIN SMALL LETTER
    0x0265.toChar() to 'h', // , LATIN SMALL LETTER TURNED
    0x2095.toChar() to 'h', // , LATIN SUBSCRIPT SMALL LETTER
    0x00ED.toChar() to 'i', //  WITH ACUTE, LATIN SMALL LETTER
    0x012D.toChar() to 'i', //  WITH BREVE, LATIN SMALL LETTER
    0x01D0.toChar() to 'i', //  WITH CARON, LATIN SMALL LETTER
    0x00EE.toChar() to 'i', //  WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x00EF.toChar() to 'i', //  WITH DIAERESIS, LATIN SMALL LETTER
    0x1ECB.toChar() to 'i', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x0209.toChar() to 'i', //  WITH DOUBLE GRAVE, LATIN SMALL LETTER
    0x00EC.toChar() to 'i', //  WITH GRAVE, LATIN SMALL LETTER
    0x1EC9.toChar() to 'i', //  WITH HOOK ABOVE, LATIN SMALL LETTER
    0x020B.toChar() to 'i', //  WITH INVERTED BREVE, LATIN SMALL LETTER
    0x012B.toChar() to 'i', //  WITH MACRON, LATIN SMALL LETTER
    0x012F.toChar() to 'i', //  WITH OGONEK, LATIN SMALL LETTER
    0x0268.toChar() to 'i', //  WITH STROKE, LATIN SMALL LETTER
    0x1E2D.toChar() to 'i', //  WITH TILDE BELOW, LATIN SMALL LETTER
    0x0129.toChar() to 'i', //  WITH TILDE, LATIN SMALL LETTER
    0x0365.toChar() to 'i', // , COMBINING LATIN SMALL LETTER
    0x0131.toChar() to 'i', // , LATIN SMALL LETTER DOTLESS
    0x1D09.toChar() to 'i', // , LATIN SMALL LETTER TURNED
    0x1D62.toChar() to 'i', // , LATIN SUBSCRIPT SMALL LETTER
    0x2071.toChar() to 'i', // , SUPERSCRIPT LATIN SMALL LETTER
    0x01F0.toChar() to 'j', //  WITH CARON, LATIN SMALL LETTER
    0x0135.toChar() to 'j', //  WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x029D.toChar() to 'j', //  WITH CROSSED-TAIL, LATIN SMALL LETTER
    0x0249.toChar() to 'j', //  WITH STROKE, LATIN SMALL LETTER
    0x025F.toChar() to 'j', //  WITH STROKE, LATIN SMALL LETTER DOTLESS
    0x0237.toChar() to 'j', // , LATIN SMALL LETTER DOTLESS
    0x1E31.toChar() to 'k', //  WITH ACUTE, LATIN SMALL LETTER
    0x01E9.toChar() to 'k', //  WITH CARON, LATIN SMALL LETTER
    0x0137.toChar() to 'k', //  WITH CEDILLA, LATIN SMALL LETTER
    0x1E33.toChar() to 'k', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x0199.toChar() to 'k', //  WITH HOOK, LATIN SMALL LETTER
    0x1E35.toChar() to 'k', //  WITH LINE BELOW, LATIN SMALL LETTER
    0x029E.toChar() to 'k', // , LATIN SMALL LETTER TURNED
    0x2096.toChar() to 'k', // , LATIN SUBSCRIPT SMALL LETTER
    0x013A.toChar() to 'l', //  WITH ACUTE, LATIN SMALL LETTER
    0x019A.toChar() to 'l', //  WITH BAR, LATIN SMALL LETTER
    0x026C.toChar() to 'l', //  WITH BELT, LATIN SMALL LETTER
    0x013E.toChar() to 'l', //  WITH CARON, LATIN SMALL LETTER
    0x013C.toChar() to 'l', //  WITH CEDILLA, LATIN SMALL LETTER
    0x1E3D.toChar() to 'l', //  WITH CIRCUMFLEX BELOW, LATIN SMALL LETTER
    0x0234.toChar() to 'l', //  WITH CURL, LATIN SMALL LETTER
    0x1E37.toChar() to 'l', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x1E3B.toChar() to 'l', //  WITH LINE BELOW, LATIN SMALL LETTER
    0x0140.toChar() to 'l', //  WITH MIDDLE DOT, LATIN SMALL LETTER
    0x026B.toChar() to 'l', //  WITH MIDDLE TILDE, LATIN SMALL LETTER
    0x026D.toChar() to 'l', //  WITH RETROFLEX HOOK, LATIN SMALL LETTER
    0x0142.toChar() to 'l', //  WITH STROKE, LATIN SMALL LETTER
    0x2097.toChar() to 'l', // , LATIN SUBSCRIPT SMALL LETTER
    0x1E3F.toChar() to 'm', //  WITH ACUTE, LATIN SMALL LETTER
    0x1E41.toChar() to 'm', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x1E43.toChar() to 'm', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x0271.toChar() to 'm', //  WITH HOOK, LATIN SMALL LETTER
    0x0270.toChar() to 'm', //  WITH LONG LEG, LATIN SMALL LETTER TURNED
    0x036B.toChar() to 'm', // , COMBINING LATIN SMALL LETTER
    0x1D1F.toChar() to 'm', // , LATIN SMALL LETTER SIDEWAYS TURNED
    0x026F.toChar() to 'm', // , LATIN SMALL LETTER TURNED
    0x2098.toChar() to 'm', // , LATIN SUBSCRIPT SMALL LETTER
    0x0144.toChar() to 'n', //  WITH ACUTE, LATIN SMALL LETTER
    0x0148.toChar() to 'n', //  WITH CARON, LATIN SMALL LETTER
    0x0146.toChar() to 'n', //  WITH CEDILLA, LATIN SMALL LETTER
    0x1E4B.toChar() to 'n', //  WITH CIRCUMFLEX BELOW, LATIN SMALL LETTER
    0x0235.toChar() to 'n', //  WITH CURL, LATIN SMALL LETTER
    0x1E45.toChar() to 'n', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x1E47.toChar() to 'n', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x01F9.toChar() to 'n', //  WITH GRAVE, LATIN SMALL LETTER
    0x0272.toChar() to 'n', //  WITH LEFT HOOK, LATIN SMALL LETTER
    0x1E49.toChar() to 'n', //  WITH LINE BELOW, LATIN SMALL LETTER
    0x019E.toChar() to 'n', //  WITH LONG RIGHT LEG, LATIN SMALL LETTER
    0x0273.toChar() to 'n', //  WITH RETROFLEX HOOK, LATIN SMALL LETTER
    0x00F1.toChar() to 'n', //  WITH TILDE, LATIN SMALL LETTER
    0x2099.toChar() to 'n', // , LATIN SUBSCRIPT SMALL LETTER
    0x00F3.toChar() to 'o', //  WITH ACUTE, LATIN SMALL LETTER
    0x014F.toChar() to 'o', //  WITH BREVE, LATIN SMALL LETTER
    0x01D2.toChar() to 'o', //  WITH CARON, LATIN SMALL LETTER
    0x00F4.toChar() to 'o', //  WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x00F6.toChar() to 'o', //  WITH DIAERESIS, LATIN SMALL LETTER
    0x022F.toChar() to 'o', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x1ECD.toChar() to 'o', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x0151.toChar() to 'o', //  WITH DOUBLE ACUTE, LATIN SMALL LETTER
    0x020D.toChar() to 'o', //  WITH DOUBLE GRAVE, LATIN SMALL LETTER
    0x00F2.toChar() to 'o', //  WITH GRAVE, LATIN SMALL LETTER
    0x1ECF.toChar() to 'o', //  WITH HOOK ABOVE, LATIN SMALL LETTER
    0x01A1.toChar() to 'o', //  WITH HORN, LATIN SMALL LETTER
    0x020F.toChar() to 'o', //  WITH INVERTED BREVE, LATIN SMALL LETTER
    0x014D.toChar() to 'o', //  WITH MACRON, LATIN SMALL LETTER
    0x01EB.toChar() to 'o', //  WITH OGONEK, LATIN SMALL LETTER
    0x00F8.toChar() to 'o', //  WITH STROKE, LATIN SMALL LETTER
    0x1D13.toChar() to 'o', //  WITH STROKE, LATIN SMALL LETTER SIDEWAYS
    0x00F5.toChar() to 'o', //  WITH TILDE, LATIN SMALL LETTER
    0x0366.toChar() to 'o', // , COMBINING LATIN SMALL LETTER
    0x0275.toChar() to 'o', // , LATIN SMALL LETTER BARRED
    0x1D17.toChar() to 'o', // , LATIN SMALL LETTER BOTTOM HALF
    0x0254.toChar() to 'o', // , LATIN SMALL LETTER OPEN
    0x1D11.toChar() to 'o', // , LATIN SMALL LETTER SIDEWAYS
    0x1D12.toChar() to 'o', // , LATIN SMALL LETTER SIDEWAYS OPEN
    0x1D16.toChar() to 'o', // , LATIN SMALL LETTER TOP HALF
    0x1E55.toChar() to 'p', //  WITH ACUTE, LATIN SMALL LETTER
    0x1E57.toChar() to 'p', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x01A5.toChar() to 'p', //  WITH HOOK, LATIN SMALL LETTER
    0x209A.toChar() to 'p', // , LATIN SUBSCRIPT SMALL LETTER
    0x024B.toChar() to 'q', //  WITH HOOK TAIL, LATIN SMALL LETTER
    0x02A0.toChar() to 'q', //  WITH HOOK, LATIN SMALL LETTER
    0x0155.toChar() to 'r', //  WITH ACUTE, LATIN SMALL LETTER
    0x0159.toChar() to 'r', //  WITH CARON, LATIN SMALL LETTER
    0x0157.toChar() to 'r', //  WITH CEDILLA, LATIN SMALL LETTER
    0x1E59.toChar() to 'r', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x1E5B.toChar() to 'r', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x0211.toChar() to 'r', //  WITH DOUBLE GRAVE, LATIN SMALL LETTER
    0x027E.toChar() to 'r', //  WITH FISHHOOK, LATIN SMALL LETTER
    0x027F.toChar() to 'r', //  WITH FISHHOOK, LATIN SMALL LETTER REVERSED
    0x027B.toChar() to 'r', //  WITH HOOK, LATIN SMALL LETTER TURNED
    0x0213.toChar() to 'r', //  WITH INVERTED BREVE, LATIN SMALL LETTER
    0x1E5F.toChar() to 'r', //  WITH LINE BELOW, LATIN SMALL LETTER
    0x027C.toChar() to 'r', //  WITH LONG LEG, LATIN SMALL LETTER
    0x027A.toChar() to 'r', //  WITH LONG LEG, LATIN SMALL LETTER TURNED
    0x024D.toChar() to 'r', //  WITH STROKE, LATIN SMALL LETTER
    0x027D.toChar() to 'r', //  WITH TAIL, LATIN SMALL LETTER
    0x036C.toChar() to 'r', // , COMBINING LATIN SMALL LETTER
    0x0279.toChar() to 'r', // , LATIN SMALL LETTER TURNED
    0x1D63.toChar() to 'r', // , LATIN SUBSCRIPT SMALL LETTER
    0x015B.toChar() to 's', //  WITH ACUTE, LATIN SMALL LETTER
    0x0161.toChar() to 's', //  WITH CARON, LATIN SMALL LETTER
    0x015F.toChar() to 's', //  WITH CEDILLA, LATIN SMALL LETTER
    0x015D.toChar() to 's', //  WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x0219.toChar() to 's', //  WITH COMMA BELOW, LATIN SMALL LETTER
    0x1E61.toChar() to 's', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x1E9B.toChar() to 's', //  WITH DOT ABOVE, LATIN SMALL LETTER LONG
    0x1E63.toChar() to 's', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x0282.toChar() to 's', //  WITH HOOK, LATIN SMALL LETTER
    0x023F.toChar() to 's', //  WITH SWASH TAIL, LATIN SMALL LETTER
    0x017F.toChar() to 's', // , LATIN SMALL LETTER LONG
    0x00DF.toChar() to 's', // , LATIN SMALL LETTER SHARP
    0x209B.toChar() to 's', // , LATIN SUBSCRIPT SMALL LETTER
    0x0165.toChar() to 't', //  WITH CARON, LATIN SMALL LETTER
    0x0163.toChar() to 't', //  WITH CEDILLA, LATIN SMALL LETTER
    0x1E71.toChar() to 't', //  WITH CIRCUMFLEX BELOW, LATIN SMALL LETTER
    0x021B.toChar() to 't', //  WITH COMMA BELOW, LATIN SMALL LETTER
    0x0236.toChar() to 't', //  WITH CURL, LATIN SMALL LETTER
    0x1E97.toChar() to 't', //  WITH DIAERESIS, LATIN SMALL LETTER
    0x1E6B.toChar() to 't', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x1E6D.toChar() to 't', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x01AD.toChar() to 't', //  WITH HOOK, LATIN SMALL LETTER
    0x1E6F.toChar() to 't', //  WITH LINE BELOW, LATIN SMALL LETTER
    0x01AB.toChar() to 't', //  WITH PALATAL HOOK, LATIN SMALL LETTER
    0x0288.toChar() to 't', //  WITH RETROFLEX HOOK, LATIN SMALL LETTER
    0x0167.toChar() to 't', //  WITH STROKE, LATIN SMALL LETTER
    0x036D.toChar() to 't', // , COMBINING LATIN SMALL LETTER
    0x0287.toChar() to 't', // , LATIN SMALL LETTER TURNED
    0x209C.toChar() to 't', // , LATIN SUBSCRIPT SMALL LETTER
    0x0289.toChar() to 'u', //  BAR, LATIN SMALL LETTER
    0x00FA.toChar() to 'u', //  WITH ACUTE, LATIN SMALL LETTER
    0x016D.toChar() to 'u', //  WITH BREVE, LATIN SMALL LETTER
    0x01D4.toChar() to 'u', //  WITH CARON, LATIN SMALL LETTER
    0x1E77.toChar() to 'u', //  WITH CIRCUMFLEX BELOW, LATIN SMALL LETTER
    0x00FB.toChar() to 'u', //  WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x1E73.toChar() to 'u', //  WITH DIAERESIS BELOW, LATIN SMALL LETTER
    0x00FC.toChar() to 'u', //  WITH DIAERESIS, LATIN SMALL LETTER
    0x1EE5.toChar() to 'u', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x0171.toChar() to 'u', //  WITH DOUBLE ACUTE, LATIN SMALL LETTER
    0x0215.toChar() to 'u', //  WITH DOUBLE GRAVE, LATIN SMALL LETTER
    0x00F9.toChar() to 'u', //  WITH GRAVE, LATIN SMALL LETTER
    0x1EE7.toChar() to 'u', //  WITH HOOK ABOVE, LATIN SMALL LETTER
    0x01B0.toChar() to 'u', //  WITH HORN, LATIN SMALL LETTER
    0x0217.toChar() to 'u', //  WITH INVERTED BREVE, LATIN SMALL LETTER
    0x016B.toChar() to 'u', //  WITH MACRON, LATIN SMALL LETTER
    0x0173.toChar() to 'u', //  WITH OGONEK, LATIN SMALL LETTER
    0x016F.toChar() to 'u', //  WITH RING ABOVE, LATIN SMALL LETTER
    0x1E75.toChar() to 'u', //  WITH TILDE BELOW, LATIN SMALL LETTER
    0x0169.toChar() to 'u', //  WITH TILDE, LATIN SMALL LETTER
    0x0367.toChar() to 'u', // , COMBINING LATIN SMALL LETTER
    0x1D1D.toChar() to 'u', // , LATIN SMALL LETTER SIDEWAYS
    0x1D1E.toChar() to 'u', // , LATIN SMALL LETTER SIDEWAYS DIAERESIZED
    0x1D64.toChar() to 'u', // , LATIN SUBSCRIPT SMALL LETTER
    0x1E7F.toChar() to 'v', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x028B.toChar() to 'v', //  WITH HOOK, LATIN SMALL LETTER
    0x1E7D.toChar() to 'v', //  WITH TILDE, LATIN SMALL LETTER
    0x036E.toChar() to 'v', // , COMBINING LATIN SMALL LETTER
    0x028C.toChar() to 'v', // , LATIN SMALL LETTER TURNED
    0x1D65.toChar() to 'v', // , LATIN SUBSCRIPT SMALL LETTER
    0x1E83.toChar() to 'w', //  WITH ACUTE, LATIN SMALL LETTER
    0x0175.toChar() to 'w', //  WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x1E85.toChar() to 'w', //  WITH DIAERESIS, LATIN SMALL LETTER
    0x1E87.toChar() to 'w', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x1E89.toChar() to 'w', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x1E81.toChar() to 'w', //  WITH GRAVE, LATIN SMALL LETTER
    0x1E98.toChar() to 'w', //  WITH RING ABOVE, LATIN SMALL LETTER
    0x028D.toChar() to 'w', // , LATIN SMALL LETTER TURNED
    0x1E8D.toChar() to 'x', //  WITH DIAERESIS, LATIN SMALL LETTER
    0x1E8B.toChar() to 'x', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x036F.toChar() to 'x', // , COMBINING LATIN SMALL LETTER
    0x00FD.toChar() to 'y', //  WITH ACUTE, LATIN SMALL LETTER
    0x0177.toChar() to 'y', //  WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x00FF.toChar() to 'y', //  WITH DIAERESIS, LATIN SMALL LETTER
    0x1E8F.toChar() to 'y', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x1EF5.toChar() to 'y', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x1EF3.toChar() to 'y', //  WITH GRAVE, LATIN SMALL LETTER
    0x1EF7.toChar() to 'y', //  WITH HOOK ABOVE, LATIN SMALL LETTER
    0x01B4.toChar() to 'y', //  WITH HOOK, LATIN SMALL LETTER
    0x0233.toChar() to 'y', //  WITH MACRON, LATIN SMALL LETTER
    0x1E99.toChar() to 'y', //  WITH RING ABOVE, LATIN SMALL LETTER
    0x024F.toChar() to 'y', //  WITH STROKE, LATIN SMALL LETTER
    0x1EF9.toChar() to 'y', //  WITH TILDE, LATIN SMALL LETTER
    0x028E.toChar() to 'y', // , LATIN SMALL LETTER TURNED
    0x017A.toChar() to 'z', //  WITH ACUTE, LATIN SMALL LETTER
    0x017E.toChar() to 'z', //  WITH CARON, LATIN SMALL LETTER
    0x1E91.toChar() to 'z', //  WITH CIRCUMFLEX, LATIN SMALL LETTER
    0x0291.toChar() to 'z', //  WITH CURL, LATIN SMALL LETTER
    0x017C.toChar() to 'z', //  WITH DOT ABOVE, LATIN SMALL LETTER
    0x1E93.toChar() to 'z', //  WITH DOT BELOW, LATIN SMALL LETTER
    0x0225.toChar() to 'z', //  WITH HOOK, LATIN SMALL LETTER
    0x1E95.toChar() to 'z', //  WITH LINE BELOW, LATIN SMALL LETTER
    0x0290.toChar() to 'z', //  WITH RETROFLEX HOOK, LATIN SMALL LETTER
    0x01B6.toChar() to 'z', //  WITH STROKE, LATIN SMALL LETTER
    0x0240.toChar() to 'z', //  WITH SWASH TAIL, LATIN SMALL LETTER
    0x0251.toChar() to 'a', // , latin small letter script
    0x00C1.toChar() to 'A', //  WITH ACUTE, LATIN CAPITAL LETTER
    0x00C2.toChar() to 'A', //  WITH CIRCUMFLEX, LATIN CAPITAL LETTER
    0x00C4.toChar() to 'A', //  WITH DIAERESIS, LATIN CAPITAL LETTER
    0x00C0.toChar() to 'A', //  WITH GRAVE, LATIN CAPITAL LETTER
    0x00C5.toChar() to 'A', //  WITH RING ABOVE, LATIN CAPITAL LETTER
    0x023A.toChar() to 'A', //  WITH STROKE, LATIN CAPITAL LETTER
    0x00C3.toChar() to 'A', //  WITH TILDE, LATIN CAPITAL LETTER
    0x1D00.toChar() to 'A', // , LATIN LETTER SMALL CAPITAL
    0x0181.toChar() to 'B', //  WITH HOOK, LATIN CAPITAL LETTER
    0x0243.toChar() to 'B', //  WITH STROKE, LATIN CAPITAL LETTER
    0x0299.toChar() to 'B', // , LATIN LETTER SMALL CAPITAL
    0x1D03.toChar() to 'B', // , LATIN LETTER SMALL CAPITAL BARRED
    0x00C7.toChar() to 'C', //  WITH CEDILLA, LATIN CAPITAL LETTER
    0x023B.toChar() to 'C', //  WITH STROKE, LATIN CAPITAL LETTER
    0x1D04.toChar() to 'C', // , LATIN LETTER SMALL CAPITAL
    0x018A.toChar() to 'D', //  WITH HOOK, LATIN CAPITAL LETTER
    0x0189.toChar() to 'D', // , LATIN CAPITAL LETTER AFRICAN
    0x1D05.toChar() to 'D', // , LATIN LETTER SMALL CAPITAL
    0x00C9.toChar() to 'E', //  WITH ACUTE, LATIN CAPITAL LETTER
    0x00CA.toChar() to 'E', //  WITH CIRCUMFLEX, LATIN CAPITAL LETTER
    0x00CB.toChar() to 'E', //  WITH DIAERESIS, LATIN CAPITAL LETTER
    0x00C8.toChar() to 'E', //  WITH GRAVE, LATIN CAPITAL LETTER
    0x0246.toChar() to 'E', //  WITH STROKE, LATIN CAPITAL LETTER
    0x0190.toChar() to 'E', // , LATIN CAPITAL LETTER OPEN
    0x018E.toChar() to 'E', // , LATIN CAPITAL LETTER REVERSED
    0x1D07.toChar() to 'E', // , LATIN LETTER SMALL CAPITAL
    0x0193.toChar() to 'G', //  WITH HOOK, LATIN CAPITAL LETTER
    0x029B.toChar() to 'G', //  WITH HOOK, LATIN LETTER SMALL CAPITAL
    0x0262.toChar() to 'G', // , LATIN LETTER SMALL CAPITAL
    0x029C.toChar() to 'H', // , LATIN LETTER SMALL CAPITAL
    0x00CD.toChar() to 'I', //  WITH ACUTE, LATIN CAPITAL LETTER
    0x00CE.toChar() to 'I', //  WITH CIRCUMFLEX, LATIN CAPITAL LETTER
    0x00CF.toChar() to 'I', //  WITH DIAERESIS, LATIN CAPITAL LETTER
    0x0130.toChar() to 'I', //  WITH DOT ABOVE, LATIN CAPITAL LETTER
    0x00CC.toChar() to 'I', //  WITH GRAVE, LATIN CAPITAL LETTER
    0x0197.toChar() to 'I', //  WITH STROKE, LATIN CAPITAL LETTER
    0x026A.toChar() to 'I', // , LATIN LETTER SMALL CAPITAL
    0x0248.toChar() to 'J', //  WITH STROKE, LATIN CAPITAL LETTER
    0x1D0A.toChar() to 'J', // , LATIN LETTER SMALL CAPITAL
    0x1D0B.toChar() to 'K', // , LATIN LETTER SMALL CAPITAL
    0x023D.toChar() to 'L', //  WITH BAR, LATIN CAPITAL LETTER
    0x1D0C.toChar() to 'L', //  WITH STROKE, LATIN LETTER SMALL CAPITAL
    0x029F.toChar() to 'L', // , LATIN LETTER SMALL CAPITAL
    0x019C.toChar() to 'M', // , LATIN CAPITAL LETTER TURNED
    0x1D0D.toChar() to 'M', // , LATIN LETTER SMALL CAPITAL
    0x019D.toChar() to 'N', //  WITH LEFT HOOK, LATIN CAPITAL LETTER
    0x0220.toChar() to 'N', //  WITH LONG RIGHT LEG, LATIN CAPITAL LETTER
    0x00D1.toChar() to 'N', //  WITH TILDE, LATIN CAPITAL LETTER
    0x0274.toChar() to 'N', // , LATIN LETTER SMALL CAPITAL
    0x1D0E.toChar() to 'N', // , LATIN LETTER SMALL CAPITAL REVERSED
    0x00D3.toChar() to 'O', //  WITH ACUTE, LATIN CAPITAL LETTER
    0x00D4.toChar() to 'O', //  WITH CIRCUMFLEX, LATIN CAPITAL LETTER
    0x00D6.toChar() to 'O', //  WITH DIAERESIS, LATIN CAPITAL LETTER
    0x00D2.toChar() to 'O', //  WITH GRAVE, LATIN CAPITAL LETTER
    0x019F.toChar() to 'O', //  WITH MIDDLE TILDE, LATIN CAPITAL LETTER
    0x00D8.toChar() to 'O', //  WITH STROKE, LATIN CAPITAL LETTER
    0x00D5.toChar() to 'O', //  WITH TILDE, LATIN CAPITAL LETTER
    0x0186.toChar() to 'O', // , LATIN CAPITAL LETTER OPEN
    0x1D0F.toChar() to 'O', // , LATIN LETTER SMALL CAPITAL
    0x1D10.toChar() to 'O', // , LATIN LETTER SMALL CAPITAL OPEN
    0x1D18.toChar() to 'P', // , LATIN LETTER SMALL CAPITAL
    0x024A.toChar() to 'Q', //  WITH HOOK TAIL, LATIN CAPITAL LETTER SMALL
    0x024C.toChar() to 'R', //  WITH STROKE, LATIN CAPITAL LETTER
    0x0280.toChar() to 'R', // , LATIN LETTER SMALL CAPITAL
    0x0281.toChar() to 'R', // , LATIN LETTER SMALL CAPITAL INVERTED
    0x1D19.toChar() to 'R', // , LATIN LETTER SMALL CAPITAL REVERSED
    0x1D1A.toChar() to 'R', // , LATIN LETTER SMALL CAPITAL TURNED
    0x023E.toChar() to 'T', //  WITH DIAGONAL STROKE, LATIN CAPITAL LETTER
    0x01AE.toChar() to 'T', //  WITH RETROFLEX HOOK, LATIN CAPITAL LETTER
    0x1D1B.toChar() to 'T', // , LATIN LETTER SMALL CAPITAL
    0x0244.toChar() to 'U', //  BAR, LATIN CAPITAL LETTER
    0x00DA.toChar() to 'U', //  WITH ACUTE, LATIN CAPITAL LETTER
    0x00DB.toChar() to 'U', //  WITH CIRCUMFLEX, LATIN CAPITAL LETTER
    0x00DC.toChar() to 'U', //  WITH DIAERESIS, LATIN CAPITAL LETTER
    0x00D9.toChar() to 'U', //  WITH GRAVE, LATIN CAPITAL LETTER
    0x1D1C.toChar() to 'U', // , LATIN LETTER SMALL CAPITAL
    0x01B2.toChar() to 'V', //  WITH HOOK, LATIN CAPITAL LETTER
    0x0245.toChar() to 'V', // , LATIN CAPITAL LETTER TURNED
    0x1D20.toChar() to 'V', // , LATIN LETTER SMALL CAPITAL
    0x1D21.toChar() to 'W', // , LATIN LETTER SMALL CAPITAL
    0x00DD.toChar() to 'Y', //  WITH ACUTE, LATIN CAPITAL LETTER
    0x0178.toChar() to 'Y', //  WITH DIAERESIS, LATIN CAPITAL LETTER
    0x024E.toChar() to 'Y', //  WITH STROKE, LATIN CAPITAL LETTER
    0x028F.toChar() to 'Y', // , LATIN LETTER SMALL CAPITAL
    0x1D22.toChar() to 'Z', // , LATIN LETTER SMALL CAPITAL

    'Ắ' to 'A',
    'Ấ' to 'A',
    'Ằ' to 'A',
    'Ầ' to 'A',
    'Ẳ' to 'A',
    'Ẩ' to 'A',
    'Ẵ' to 'A',
    'Ẫ' to 'A',
    'Ặ' to 'A',
    'Ậ' to 'A',

    'ắ' to 'a',
    'ấ' to 'a',
    'ằ' to 'a',
    'ầ' to 'a',
    'ẳ' to 'a',
    'ẩ' to 'a',
    'ẵ' to 'a',
    'ẫ' to 'a',
    'ặ' to 'a',
    'ậ' to 'a',

    'Ế' to 'E',
    'Ề' to 'E',
    'Ể' to 'E',
    'Ễ' to 'E',
    'Ệ' to 'E',

    'ế' to 'e',
    'ề' to 'e',
    'ể' to 'e',
    'ễ' to 'e',
    'ệ' to 'e',

    'Ố' to 'O',
    'Ớ' to 'O',
    'Ồ' to 'O',
    'Ờ' to 'O',
    'Ổ' to 'O',
    'Ở' to 'O',
    'Ỗ' to 'O',
    'Ỡ' to 'O',
    'Ộ' to 'O',
    'Ợ' to 'O',

    'ố' to 'o',
    'ớ' to 'o',
    'ồ' to 'o',
    'ờ' to 'o',
    'ổ' to 'o',
    'ở' to 'o',
    'ỗ' to 'o',
    'ỡ' to 'o',
    'ộ' to 'o',
    'ợ' to 'o',

    'Ứ' to 'U',
    'Ừ' to 'U',
    'Ử' to 'U',
    'Ữ' to 'U',
    'Ự' to 'U',

    'ứ' to 'u',
    'ừ' to 'u',
    'ử' to 'u',
    'ữ' to 'u',
    'ự' to 'u',
)

// NormalizeRunes normalizes Latin script letters
fun normalizeRunes(runes: CharArray): CharArray {
    val ret = runes.copyOf() // Create a copy of the input array

    for (idx in runes.indices) {
        val r = runes[idx]
        // Check if the character is within the specified range
        if (r < '\u00C0' || r > '\u2184') {
            continue
        }
        val n = normalized[r] // Access the normalized value
        if (n != null) {
            ret[idx] = n // Normalize the character
        }
    }
    return ret
}
