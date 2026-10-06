package com.example.supermercadosmart.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** De onde veio o produto encontrado pelo código de barras. */
enum class LookupSource(val label: String) {
    MY_LISTS("suas listas"),
    OPEN_FOOD_FACTS("Open Food Facts"),
    OPEN_BEAUTY_FACTS("Open Beauty Facts"),
    OPEN_PET_FOOD_FACTS("Open Pet Food Facts"),
    OPEN_PRODUCTS_FACTS("Open Products Facts")
}

/** Resultado da busca pelo código de barras. */
sealed class LookupResult {
    /**
     * Produto encontrado. [name] e [imageUri] podem vir vazios (o Open Food Facts às vezes
     * tem só a foto, ou só o nome). [category] null = o app usa o palpite pelo nome.
     */
    data class Found(
        val name: String?,
        val imageUri: String?,
        val category: Category?,
        val source: LookupSource
    ) : LookupResult()

    /** Código não cadastrado nas listas nem no Open Food Facts. */
    object NotFound : LookupResult()

    /** Sem internet, demora demais ou o serviço respondeu com erro. Dá para tentar de novo. */
    object Failed : LookupResult()
}

/**
 * Busca nome, foto e categoria de um produto pelo código de barras.
 * 1. Procura nas listas do próprio app (funciona sem internet).
 * 2. Se não achar, consulta o Open Food Facts (base aberta e gratuita, sem chave de acesso)
 *    e salva a foto no celular, para ela aparecer mesmo sem internet no mercado.
 * 3. Se o Open Food Facts responder que o código é de outro tipo de produto (higiene e beleza,
 *    ração, limpeza…), consulta a base irmã certa: Open Beauty Facts, Open Pet Food Facts
 *    ou Open Products Facts.
 * Usa só HttpURLConnection e org.json, que já vêm no Android (nenhuma biblioteca nova).
 */
class ProductLookup(
    context: Context,
    private val repository: ItemRepository
) {
    private val appContext = context.applicationContext

    suspend fun lookup(barcode: String): LookupResult = withContext(Dispatchers.IO) {
        val code = barcode.trim()

        val local = repository.findByBarcode(code)
        if (local != null && local.name.isNotBlank()) {
            return@withContext LookupResult.Found(
                name = local.name,
                imageUri = local.imageUri?.takeIf { localImageExists(it) },
                category = Category.fromKey(local.category),
                source = LookupSource.MY_LISTS
            )
        }

        // O Open Food Facts só tem códigos numéricos (EAN/UPC); QR codes e afins não adianta buscar
        if (code.isEmpty() || !code.all { it.isDigit() }) return@withContext LookupResult.NotFound

        try {
            when (val food = fetch(code, Base.FOOD)) {
                is FetchResult.Done -> food.result
                // "product found with a different product type: beauty" → consulta a base certa
                is FetchResult.OtherType -> {
                    val base = Base.forType(food.type)
                    if (base == null) {
                        LookupResult.NotFound
                    } else {
                        when (val other = fetch(code, base)) {
                            is FetchResult.Done -> other.result
                            is FetchResult.OtherType -> LookupResult.NotFound
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            LookupResult.Failed
        } catch (e: Exception) {
            // Resposta em formato inesperado: trata como "não encontrado"
            LookupResult.NotFound
        }
    }

    /** Resposta de uma das bases: resultado final, ou "o código é de outro tipo de produto". */
    private sealed class FetchResult {
        data class Done(val result: LookupResult) : FetchResult()
        data class OtherType(val type: String) : FetchResult()
    }

    /** Bases do projeto Open Food Facts: mesma API, cada uma com um tipo de produto. */
    private enum class Base(
        val host: String,
        val productType: String,
        val source: LookupSource,
        /** Categoria fixa da base (null = palpite pelo nome e pelas categorias do produto). */
        val category: Category?
    ) {
        FOOD("world.openfoodfacts.org", "food", LookupSource.OPEN_FOOD_FACTS, null),
        BEAUTY("world.openbeautyfacts.org", "beauty", LookupSource.OPEN_BEAUTY_FACTS, Category.HIGIENE),
        PET_FOOD("world.openpetfoodfacts.org", "petfood", LookupSource.OPEN_PET_FOOD_FACTS, Category.PET),
        PRODUCTS("world.openproductsfacts.org", "product", LookupSource.OPEN_PRODUCTS_FACTS, null);

        companion object {
            fun forType(type: String): Base? =
                values().firstOrNull { it != FOOD && it.productType == type }
        }
    }

    private fun fetch(code: String, base: Base): FetchResult {
        val url = URL("https://${base.host}/api/v2/product/$code.json?fields=$FIELDS")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            // O Open Food Facts pede que cada app se identifique
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            // O 404 também traz um JSON (que pode dizer "é de outro tipo de produto")
            val stream = when (status) {
                HttpURLConnection.HTTP_OK -> connection.inputStream
                HttpURLConnection.HTTP_NOT_FOUND -> connection.errorStream
                else -> null
            } ?: return FetchResult.Done(
                if (status == HttpURLConnection.HTTP_NOT_FOUND) LookupResult.NotFound
                else LookupResult.Failed
            )

            val body = stream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (json.optInt("status", 0) != 1) {
                val verbose = json.optString("status_verbose", "")
                val otherType = OTHER_TYPE_REGEX.find(verbose)?.groupValues?.get(1)
                return if (otherType != null && otherType != base.productType) {
                    FetchResult.OtherType(otherType)
                } else {
                    FetchResult.Done(LookupResult.NotFound)
                }
            }
            val product = json.optJSONObject("product")
                ?: return FetchResult.Done(LookupResult.NotFound)

            val name = buildName(product)
            val imageUrl = product.text("image_front_url") ?: product.text("image_url")
            val imageUri = imageUrl?.let { downloadImage(code, it) }

            if (name == null && imageUri == null) return FetchResult.Done(LookupResult.NotFound)

            val category = base.category
                ?: name?.let { Category.guess(it) }?.takeIf { it != Category.OUTROS }
                ?: categoryFromTags(product)

            return FetchResult.Done(
                LookupResult.Found(
                    name = name,
                    imageUri = imageUri,
                    category = category,
                    source = base.source
                )
            )
        } finally {
            connection.disconnect()
        }
    }

    /** Nome + marca + tamanho, sem repetir o que já está no nome. Ex.: "Leite Integral Italac 1 L". */
    private fun buildName(product: JSONObject): String? {
        val base = product.text("product_name_pt")
            ?: product.text("product_name")
            ?: product.text("generic_name_pt")
            ?: return null

        val parts = mutableListOf(prettify(base))
        val brand = product.text("brands")?.split(',')?.firstOrNull()?.trim()
        if (!brand.isNullOrBlank() && !base.contains(brand, ignoreCase = true)) {
            parts += prettify(brand)
        }
        val quantity = product.text("quantity")
        if (!quantity.isNullOrBlank() && !squash(base).contains(squash(quantity))) {
            parts += quantity
        }
        return parts.joinToString(" ").take(MAX_NAME_LENGTH)
    }

    /** "LEITE INTEGRAL" → "Leite Integral" (só mexe em textos todos em maiúsculas). */
    private fun prettify(text: String): String {
        val trimmed = text.trim().replace(Regex("\\s+"), " ")
        val letters = trimmed.filter { it.isLetter() }
        if (letters.isEmpty() || letters != letters.uppercase(LOCALE_BR)) return trimmed
        return trimmed.lowercase(LOCALE_BR).split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { it.titlecase(LOCALE_BR) }
        }
    }

    private fun squash(text: String) = text.lowercase(LOCALE_BR).replace(" ", "")

    /** Usa as categorias do Open Food Facts quando o nome não deu um palpite. */
    private fun categoryFromTags(product: JSONObject): Category? {
        val array = product.optJSONArray("categories_tags") ?: return null
        val tags = (0 until array.length()).map { array.optString(it) }.toSet()
        // A ordem importa: "en:milks" também está em "en:beverages", por exemplo
        return TAG_RULES.firstOrNull { (keys, _) -> keys.any { it in tags } }?.second
    }

    /** Baixa a foto para a pasta do app. Se falhar, o produto fica sem foto (não é erro). */
    private fun downloadImage(code: String, imageUrl: String): String? {
        val dir = File(appContext.filesDir, "product_images").apply { mkdirs() }
        val file = File(dir, "off_$code.jpg")
        val temp = File(dir, "off_$code.tmp")
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            if (temp.length() == 0L) {
                temp.delete()
                return null
            }
            file.delete()
            if (!temp.renameTo(file)) return null
            Uri.fromFile(file).toString()
        } catch (e: IOException) {
            temp.delete()
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** A foto de um item antigo pode ter sido apagada (as fotos da câmera ficam no cache). */
    private fun localImageExists(uri: String): Boolean {
        val parsed = Uri.parse(uri)
        if (parsed.scheme != "file") return true
        return parsed.path?.let { File(it).exists() } ?: false
    }

    private fun JSONObject.text(key: String): String? =
        optString(key, "").trim().takeIf { it.isNotEmpty() && it != "null" }

    private companion object {
        /** "product found with a different product type: beauty" → "beauty" */
        val OTHER_TYPE_REGEX = Regex("different product type:\\s*([a-z_]+)")
        const val FIELDS =
            "product_name,product_name_pt,generic_name_pt,brands,quantity,image_front_url,image_url,categories_tags"
        const val USER_AGENT =
            "SupermercadoSmart/1.0 (Android; github.com/adriano25barbosa/supermercado-smart)"
        const val TIMEOUT_MS = 8_000
        const val MAX_NAME_LENGTH = 80
        val LOCALE_BR: Locale = Locale("pt", "BR")

        val TAG_RULES: List<Pair<List<String>, Category>> = listOf(
            listOf("en:frozen-foods", "en:ice-creams", "en:frozen-desserts") to Category.CONGELADOS,
            listOf(
                "en:dairies", "en:milks", "en:cheeses", "en:yogurts", "en:butters",
                "en:hams", "en:cold-cuts"
            ) to Category.LATICINIOS,
            listOf("en:meats", "en:poultries", "en:fishes", "en:seafood") to Category.CARNES,
            listOf("en:breads", "en:cakes") to Category.PADARIA,
            listOf("en:fresh-fruits", "en:fresh-vegetables", "en:eggs") to Category.HORTIFRUTI,
            listOf(
                "en:coffees", "en:cereals-and-potatoes", "en:pastas", "en:rices", "en:legumes",
                "en:sugars", "en:biscuits", "en:chocolates", "en:snacks", "en:condiments",
                "en:sauces", "en:canned-foods", "en:breakfast-cereals", "en:spreads",
                "en:vegetable-oils", "en:flours"
            ) to Category.MERCEARIA,
            listOf("en:beverages", "en:alcoholic-beverages", "en:waters") to Category.BEBIDAS
        )
    }
}
