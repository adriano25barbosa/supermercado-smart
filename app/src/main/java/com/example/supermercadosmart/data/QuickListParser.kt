package com.example.supermercadosmart.data

/** Um item da "Monte sua lista antecipado": só nome e quantidade (o preço vem no mercado). */
data class QuickEntry(val name: String, val quantity: Int)

/**
 * Lê o que a pessoa digita ou cola na "Monte sua lista antecipado".
 *
 * - Uma linha = um item. Também separa por ";" e por ", " (vírgula seguida de espaço),
 *   para listas coladas como "arroz, feijão, leite". "Coca 1,5 L" não é separado.
 * - Tira marcadores do começo: "-", "•", "*", "☐", "[ ]", "1." e "1)".
 * - Quantidade: "2 leite", "2x leite", "leite x2", "leite 2x", "leite (2)" → 2.
 *   Números que fazem parte do nome ("Coca 2 L", "leite 1 litro") ficam no nome.
 */
object QuickListParser {

    private const val MAX_QUANTITY = 99
    private const val MAX_NAME = 80

    private val separators = Regex(";|,\\s+")
    private val bullet = Regex("^(\\[\\s?[xX]?\\s?]|[-–—•*·▪►✓✔☐☑]+|\\d{1,2}[.)])\\s*")
    private val leadingQuantity = Regex("^(\\d{1,2})\\s*(x|un\\.?|und\\.?|unid\\.?)?\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val trailingQuantity = Regex("^(.+?)\\s+(?:x\\s*(\\d{1,2})|(\\d{1,2})\\s*x|\\((\\d{1,2})\\))$", RegexOption.IGNORE_CASE)

    // "2 kg de carne", "1 litro" etc.: o número é medida, não quantidade
    private val unitWords = setOf(
        "kg", "g", "gr", "l", "lt", "lts", "litro", "litros", "ml", "metro", "metros",
        "pacote", "pacotes", "pct", "caixa", "caixas", "cx", "duzia", "dúzia", "duzias", "dúzias",
        "bandeja", "bandejas", "lata", "latas", "garrafa", "garrafas", "fardo", "fardos"
    )

    /** Separa um texto (uma ou várias linhas) em itens. Linhas vazias são ignoradas. */
    fun parse(text: String): List<QuickEntry> =
        text.split('\n', '\r')
            .flatMap { it.split(separators) }
            .mapNotNull { parseOne(it) }

    /** Um item só: "2 leite" → QuickEntry("Leite", 2). null se não sobrar nome. */
    fun parseOne(raw: String): QuickEntry? {
        var text = raw.trim().replace(Regex("\\s+"), " ")
        // tira marcadores ("- ", "1. ", "☐ "), inclusive repetidos ("1. - arroz")
        repeat(2) { text = text.replace(bullet, "").trim() }
        if (text.isEmpty()) return null

        var quantity = 1

        val lead = leadingQuantity.find(text)
        if (lead != null) {
            val rest = lead.groupValues[3]
            val firstWord = rest.substringBefore(' ').lowercase()
            val hasUnitMarker = lead.groupValues[2].isNotEmpty()
            // "2 kg de carne": o número é medida → fica tudo no nome
            if (hasUnitMarker || firstWord !in unitWords) {
                quantity = lead.groupValues[1].toInt()
                text = rest
            }
        } else {
            val trail = trailingQuantity.find(text)
            if (trail != null) {
                val number = trail.groupValues.drop(2).firstOrNull { it.isNotEmpty() }
                if (number != null) {
                    quantity = number.toInt()
                    text = trail.groupValues[1]
                }
            }
        }

        val name = text.trim().trimEnd(',', '.', ';', ':').trim()
        if (name.isEmpty() || name.none { it.isLetter() }) return null

        return QuickEntry(
            name = name.replaceFirstChar { it.uppercaseChar() }.take(MAX_NAME),
            quantity = quantity.coerceIn(1, MAX_QUANTITY)
        )
    }

    /** Chave para comparar nomes ("Leite " e "leite" são o mesmo item). */
    fun key(name: String): String =
        java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
}
