package com.example.supermercadosmart.data

import java.text.Normalizer

/**
 * Categorias dos itens, na ordem em que aparecem na lista (ordem dos corredores).
 * No banco fica salvo o [name] (ex.: "LIMPEZA"), então o [label] pode ser renomeado à vontade.
 * O ícone de cada uma fica em ui/screens/CategoryUi.kt.
 */
enum class Category(val label: String, val keywords: List<String>) {
    HORTIFRUTI(
        "Hortifrúti",
        listOf(
            "banana", "maca", "laranja", "limao", "tangerina", "tomate", "cebola", "cebolinha",
            "alho", "batata", "cenoura", "beterraba", "alface", "rucula", "agriao", "couve",
            "repolho", "brocolis", "cheiro verde", "coentro", "salsa", "salsinha", "hortela",
            "pimentao", "pimenta de cheiro", "abobora", "abobrinha", "chuchu", "pepino", "quiabo",
            "maxixe", "berinjela", "jambu", "macaxeira", "mandioca", "aipim", "inhame", "gengibre",
            "mamao", "manga", "abacaxi", "uva", "melancia", "melao", "morango", "pera", "abacate",
            "kiwi", "goiaba", "maracuja", "acai", "coco", "ovo", "fruta", "verdura", "legume"
        )
    ),
    PADARIA(
        "Padaria",
        listOf(
            "pao", "paes", "bisnaguinha", "baguete", "torrada", "bolo", "rosca", "croissant",
            "sonho", "broa", "pao de queijo"
        )
    ),
    CARNES(
        "Carnes e peixes",
        listOf(
            "carne", "frango", "peito de frango", "coxa", "sobrecoxa", "asa de frango", "file",
            "picanha", "alcatra", "patinho", "acem", "musculo", "costela", "maminha", "fraldinha",
            "bisteca", "porco", "suino", "linguica", "salsicha", "bacon", "hamburguer", "peixe",
            "pescada", "tilapia", "salmao", "dourada", "filhote", "tambaqui", "camarao", "charque",
            "carne seca", "carne moida"
        )
    ),
    LATICINIOS(
        "Frios e laticínios",
        listOf(
            "leite", "queijo", "requeijao", "iogurte", "manteiga", "margarina", "nata",
            "creme cheese", "presunto", "mortadela", "salame", "peito de peru", "apresuntado",
            "bebida lactea", "coalhada"
        )
    ),
    MERCEARIA(
        "Mercearia",
        listOf(
            "arroz", "feijao", "macarr", "espaguete", "lasanha seca", "farinha", "farofa", "fuba",
            "tapioca", "goma", "acucar", "adocante", "sal", "oleo", "azeite", "vinagre", "cafe",
            "achocolatado", "ovomaltine", "chocolate", "biscoito", "bolacha", "cereal", "aveia",
            "granola", "leite condensado", "creme de leite", "leite em po", "leite de coco",
            "molho", "extrato de tomate", "catchup", "ketchup", "maionese", "mostarda", "tempero",
            "caldo", "atum", "sardinha", "milho verde", "ervilha", "azeitona", "gelatina",
            "pipoca", "sopa", "batata palha", "salgadinho", "tucupi", "doce", "geleia", "mel"
        )
    ),
    BEBIDAS(
        "Bebidas",
        listOf(
            "agua", "agua de coco", "refrigerante", "refri", "suco", "cerveja", "vinho",
            "energetico", "isotonico", "cha gelado", "guarana", "coca", "vodka", "cachaca", "whisky"
        )
    ),
    CONGELADOS(
        "Congelados",
        listOf(
            "congelad", "sorvete", "picole", "pizza", "lasanha", "nuggets", "empanado", "polpa",
            "gelo", "batata pre frita"
        )
    ),
    LIMPEZA(
        "Limpeza",
        listOf(
            "detergente", "sabao", "lava roupa", "amaciante", "agua sanitaria", "cloro",
            "alvejante", "desinfetante", "multiuso", "limpador", "limpa vidro", "lustra moveis",
            "esponja", "palha de aco", "bom bril", "pano de chao", "pano de prato", "flanela",
            "vassoura", "rodo", "saco de lixo", "papel toalha", "inseticida", "lava louca"
        )
    ),
    HIGIENE(
        "Higiene e beleza",
        listOf(
            "sabonete", "shampoo", "xampu", "condicionador", "creme dental", "pasta de dente",
            "escova de dente", "fio dental", "enxaguante", "desodorante", "papel higienico",
            "absorvente", "aparelho de barbear", "barbeador", "cotonete", "hidratante",
            "protetor solar", "agua oxigenada", "algodao", "creme de cabelo", "creme para pentear"
        )
    ),
    BEBE(
        "Bebê",
        listOf(
            "fralda", "lenco umedecido", "papinha", "mamadeira", "chupeta", "formula infantil",
            "pomada para assadura"
        )
    ),
    PET(
        "Pet",
        listOf("racao", "petisco", "areia de gato", "areia sanitaria", "sache para gato", "sache para cachorro")
    ),
    OUTROS("Outros", emptyList());

    companion object {

        /** Pares (palavra-chave normalizada, categoria), da mais longa para a mais curta. */
        private val keywordIndex: List<Pair<String, Category>> by lazy {
            values()
                .flatMap { category -> category.keywords.map { normalize(it) to category } }
                .sortedByDescending { it.first.length }
        }

        /** Converte a chave salva no banco de volta para a categoria (desconhecida → Outros). */
        fun fromKey(key: String?): Category =
            values().firstOrNull { it.name == key } ?: OUTROS

        /**
         * Palpite da categoria pelo nome do produto.
         * Uma palavra-chave vale quando alguma palavra do nome começa com ela
         * ("bananas" → "banana"). As mais longas são testadas primeiro, então
         * "leite condensado" (Mercearia) ganha de "leite" (Laticínios).
         */
        fun guess(productName: String): Category {
            val text = " " + normalize(productName)
            if (text.isBlank()) return OUTROS
            return keywordIndex.firstOrNull { (keyword, _) -> text.contains(" $keyword") }?.second
                ?: OUTROS
        }

        /** Minúsculas, sem acentos e sem pontuação: "Pão-Francês" → "pao frances". */
        private fun normalize(value: String): String =
            Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), " ")
                .trim()
    }
}

/** Categoria do item como enum (o banco guarda só o texto da chave). */
val Item.categoryEnum: Category
    get() = Category.fromKey(category)
