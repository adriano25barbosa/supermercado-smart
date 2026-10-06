package com.example.supermercadosmart.pdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.supermercadosmart.data.Category
import com.example.supermercadosmart.data.Item
import com.example.supermercadosmart.data.categoryEnum
import com.example.supermercadosmart.data.hasPrice
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gera um PDF estilo "nota de compras" com todos os itens agrupados por categoria,
 * status (no carrinho ou não)
 * e o total geral. Pode abrir num leitor de PDF ([open]) ou na folha de
 * compartilhamento nativa do Android ([exportAndShare]).
 */
object PdfExporter {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    /** Gera o PDF e abre a tela de compartilhar do Android. */
    fun exportAndShare(context: Context, items: List<Item>, listName: String) {
        val uri = buildPdf(context, items, listName)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar lista de compras"))
    }

    /** Gera o PDF e abre num leitor de PDF do celular (se não houver, cai no compartilhar). */
    fun open(context: Context, items: List<Item>, listName: String) {
        val uri = buildPdf(context, items, listName)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum leitor de PDF encontrado. Escolha um app para abrir.", Toast.LENGTH_LONG).show()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Abrir lista de compras"))
        }
    }

    /** Desenha o PDF no cache do app e devolve o endereço (Uri) para abrir ou compartilhar. */
    private fun buildPdf(context: Context, items: List<Item>, listName: String): Uri {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.DKGRAY
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
        }
        val totalPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
        }

        var y = 40f
        val marginLeft = 40f

        val title = listName.ifBlank { "Lista de Compras" }.take(45)
        canvas.drawText(title, marginLeft, y, titlePaint)
        y += 20f
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
        canvas.drawText("Supermercado Smart · Gerado em: $dateStr", marginLeft, y, subtitlePaint)
        y += 30f

        canvas.drawText("✔", marginLeft, y, headerPaint)
        canvas.drawText("Produto", marginLeft + 25f, y, headerPaint)
        canvas.drawText("Qtd", marginLeft + 300f, y, headerPaint)
        canvas.drawText("Unit.", marginLeft + 350f, y, headerPaint)
        canvas.drawText("Subtotal", marginLeft + 440f, y, headerPaint)
        y += 10f
        canvas.drawLine(marginLeft, y, pageWidth - marginLeft, y, subtitlePaint)
        y += 20f

        var total = 0.0

        // Faixa sálvia clara atrás do nome de cada categoria
        val categoryBandPaint = Paint().apply {
            color = android.graphics.Color.rgb(0xD8, 0xF3, 0xDC)
            style = Paint.Style.FILL
        }
        val categoryPaint = Paint().apply {
            textSize = 13f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(0x1B, 0x43, 0x32)
        }

        fun newPageIfNeeded(spaceNeeded: Float) {
            if (y > pageHeight - 40f - spaceNeeded) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }
        }

        // Itens agrupados por categoria, na mesma ordem da tela
        val groups = Category.values().mapNotNull { category ->
            val itemsInCategory = items.filter { it.categoryEnum == category }
            if (itemsInCategory.isEmpty()) null else category to itemsInCategory
        }

        for ((category, itemsInCategory) in groups) {
            // cabeçalho + pelo menos um item na mesma página
            newPageIfNeeded(50f)
            val subtotal = itemsInCategory.sumOf { it.totalPrice }
            canvas.drawRect(marginLeft - 4f, y - 14f, pageWidth - marginLeft + 4f, y + 6f, categoryBandPaint)
            canvas.drawText("${category.label} (${itemsInCategory.size})", marginLeft, y, categoryPaint)
            canvas.drawText(currencyFormat.format(subtotal), marginLeft + 440f, y, categoryPaint)
            y += 24f

            for (item in itemsInCategory) {
                newPageIfNeeded(20f)

                val checkbox = if (item.inCart) "[x]" else "[ ]"
                canvas.drawText(checkbox, marginLeft, y, bodyPaint)
                canvas.drawText(item.name.take(38), marginLeft + 25f, y, bodyPaint)
                canvas.drawText(item.quantity.toString(), marginLeft + 300f, y, bodyPaint)
                // Item sem preço ("Monte sua lista antecipado"): traço no lugar de R$ 0,00
                val unitText = if (item.hasPrice) currencyFormat.format(item.unitPrice) else "—"
                val totalText = if (item.hasPrice) currencyFormat.format(item.totalPrice) else "—"
                canvas.drawText(unitText, marginLeft + 350f, y, bodyPaint)
                canvas.drawText(totalText, marginLeft + 440f, y, bodyPaint)

                total += item.totalPrice
                y += 22f
            }
            y += 8f
        }

        newPageIfNeeded(50f)
        y += 15f
        canvas.drawLine(marginLeft, y, pageWidth - marginLeft, y, subtitlePaint)
        y += 25f
        canvas.drawText("TOTAL GERAL: ${currencyFormat.format(total)}", marginLeft, y, totalPaint)
        val withoutPrice = items.count { !it.hasPrice }
        if (withoutPrice > 0) {
            y += 20f
            canvas.drawText(
                if (withoutPrice == 1) "1 item sem preço (não entra no total)"
                else "$withoutPrice itens sem preço (não entram no total)",
                marginLeft, y, subtitlePaint
            )
        }

        pdfDocument.finishPage(page)

        val pdfDir = File(context.cacheDir, "pdfs")
        if (!pdfDir.exists()) pdfDir.mkdirs()
        val fileName = "lista_compras_${System.currentTimeMillis()}.pdf"
        val file = File(pdfDir, fileName)

        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
