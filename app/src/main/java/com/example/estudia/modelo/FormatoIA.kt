package com.example.estudia.modelo

// Formato de las respuestas de la IA: vienen en Markdown simple (negrita,
// listas, títulos) con las fórmulas entre $...$ en notación LaTeX. La
// pantalla las dibuja con formato (ui/componentes/TextoConFormato); acá están
// las partes puras: pasar una fórmula a texto legible y pasar todo a texto
// plano (para copiar o guardar como nota).

private val SIMBOLOS = linkedMapOf(
    "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\Gamma" to "Γ", "\\delta" to "δ", "\\Delta" to "Δ",
    "\\epsilon" to "ε", "\\varepsilon" to "ε", "\\theta" to "θ", "\\lambda" to "λ", "\\mu" to "μ", "\\pi" to "π",
    "\\rho" to "ρ", "\\sigma" to "σ", "\\Sigma" to "Σ", "\\tau" to "τ", "\\phi" to "φ", "\\varphi" to "φ",
    "\\omega" to "ω", "\\Omega" to "Ω", "\\eta" to "η", "\\nabla" to "∇",
    "\\cdot" to "·", "\\times" to "×", "\\div" to "÷", "\\pm" to "±", "\\mp" to "∓",
    "\\leq" to "≤", "\\le" to "≤", "\\geq" to "≥", "\\ge" to "≥", "\\neq" to "≠", "\\ne" to "≠",
    "\\approx" to "≈", "\\equiv" to "≡", "\\infty" to "∞", "\\rightarrow" to "→", "\\to" to "→",
    "\\leftarrow" to "←", "\\Rightarrow" to "⇒", "\\Leftrightarrow" to "⇔", "\\iff" to "⇔", "\\implies" to "⇒",
    "\\int" to "∫", "\\oint" to "∮", "\\sum" to "∑", "\\prod" to "∏", "\\partial" to "∂",
    "\\in" to "∈", "\\notin" to "∉", "\\subseteq" to "⊆", "\\subset" to "⊂", "\\cup" to "∪", "\\cap" to "∩",
    "\\emptyset" to "∅", "\\forall" to "∀", "\\exists" to "∃", "\\neg" to "¬", "\\land" to "∧", "\\lor" to "∨",
    "\\degree" to "°", "\\circ" to "°", "\\angle" to "∠", "\\perp" to "⊥", "\\parallel" to "∥",
    "\\lim" to "lim", "\\sin" to "sin", "\\cos" to "cos", "\\tan" to "tan", "\\log" to "log", "\\ln" to "ln",
    "\\exp" to "exp", "\\max" to "max", "\\min" to "min",
    "\\left" to "", "\\right" to "", "\\{" to "{", "\\}" to "}", "\\," to " ", "\\;" to " ", "\\:" to " ",
    "\\!" to "", "\\quad" to "  ", "\\qquad" to "   ", "\\ldots" to "…", "\\cdots" to "⋯", "\\dots" to "…"
)

private val SUPERINDICES = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
    '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾', 'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ', 'y' to 'ʸ',
    'a' to 'ᵃ', 'b' to 'ᵇ', 'c' to 'ᶜ', 'd' to 'ᵈ', 'e' to 'ᵉ', 'k' to 'ᵏ', 'm' to 'ᵐ', 't' to 'ᵗ', 'T' to 'ᵀ'
)
private val SUBINDICES = mapOf(
    '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄', '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
    '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎', 'a' to 'ₐ', 'e' to 'ₑ', 'i' to 'ᵢ', 'j' to 'ⱼ',
    'k' to 'ₖ', 'n' to 'ₙ', 'o' to 'ₒ', 'x' to 'ₓ', 't' to 'ₜ', 'm' to 'ₘ'
)

// Un exponente o subíndice: con los caracteres chiquitos de Unicode si existen
// todos (x² , aₙ); si no, con ^ o _ y paréntesis (e^(iπ)).
private fun indice(contenido: String, tabla: Map<Char, Char>, marca: String): String {
    val limpio = contenido.replace(" ", "")
    return if (limpio.isNotEmpty() && limpio.all { it in tabla }) limpio.map { tabla.getValue(it) }.joinToString("")
    else if (limpio.length == 1) "$marca$limpio" else "$marca($limpio)"
}

// Entre paréntesis si tiene más de un término (a+b)/2; si no, tal cual (1/2).
private fun agrupar(texto: String): String {
    val t = texto.trim()
    return if (t.length <= 1 || t.all { it.isLetterOrDigit() || it == '.' || it in "²³¹⁰⁴⁵⁶⁷⁸⁹" }) t else "($t)"
}

// Pasa una fórmula LaTeX simple a texto legible: \frac{a+b}{2} → (a+b)/2,
// x^2 → x², \sqrt{x} → √x, \alpha → α. Lo que no reconoce queda sin la barra.
fun formulaLegible(latex: String): String {
    var t = latex.trim()
    // \text{...} y \mathrm{...}: solo el texto.
    t = t.replace(Regex("""\\(?:text|mathrm|mathbf|mathit|operatorname)\{([^{}]*)\}""")) { it.groupValues[1] }
    // Fracciones y raíces, de adentro hacia afuera.
    repeat(10) {
        val antes = t
        t = t.replace(Regex("""\\[dt]?frac\{([^{}]*)\}\{([^{}]*)\}""")) { agrupar(it.groupValues[1]) + "/" + agrupar(it.groupValues[2]) }
        t = t.replace(Regex("""\\sqrt\[([^\]]*)\]\{([^{}]*)\}""")) { indice(it.groupValues[1], SUPERINDICES, "^") + "√" + agrupar(it.groupValues[2]) }
        t = t.replace(Regex("""\\sqrt\{([^{}]*)\}""")) { "√" + agrupar(it.groupValues[1]) }
        if (t == antes) return@repeat
    }
    // Símbolos (los nombres largos primero, para que \leq no quede como ≤q).
    SIMBOLOS.keys.sortedByDescending { it.length }.forEach { clave ->
        t = t.replace(Regex(Regex.escape(clave) + "(?![a-zA-Z])"), SIMBOLOS.getValue(clave))
    }
    // Exponentes y subíndices: ^{...}, ^x, _{...}, _x.
    t = t.replace(Regex("""\^\{([^{}]*)\}""")) { indice(it.groupValues[1], SUPERINDICES, "^") }
    t = t.replace(Regex("""\^([A-Za-z0-9+\-])""")) { indice(it.groupValues[1], SUPERINDICES, "^") }
    t = t.replace(Regex("""_\{([^{}]*)\}""")) { indice(it.groupValues[1], SUBINDICES, "_") }
    t = t.replace(Regex("""_([A-Za-z0-9])""")) { indice(it.groupValues[1], SUBINDICES, "_") }
    // Comandos que no se conocen: sin la barra. Llaves sueltas: afuera.
    t = t.replace(Regex("""\\([a-zA-Z]+)"""), "$1").replace("{", "").replace("}", "")
    return t.replace(Regex(" {2,}"), " ").trim()
}

// Las fórmulas de un texto: $...$ y $$...$$ (no toma "$" sueltos como "$500").
val PATRON_FORMULA = Regex("""\$\$(.+?)\$\$|\$([^\s$](?:[^$\n]*?[^\s$])?)\$""")

// Texto plano para copiar o guardar como nota: sin asteriscos ni numerales,
// con viñetas "•" y las fórmulas legibles.
fun textoPlano(markdown: String): String =
    markdown
        .replace(PATRON_FORMULA) { formulaLegible(it.groupValues[1].ifEmpty { it.groupValues[2] }) }
        .replace(Regex("""(?m)^```.*$\n?"""), "")
        .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
        .replace(Regex("""__(.+?)__"""), "$1")
        .replace(Regex("""(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])"""), "$1")
        .replace(Regex("""`([^`]+)`"""), "$1")
        .replace(Regex("""(?m)^#{1,6}\s*"""), "")
        .replace(Regex("""(?m)^(\s*)[*-]\s+"""), "$1• ")
        .trim()
