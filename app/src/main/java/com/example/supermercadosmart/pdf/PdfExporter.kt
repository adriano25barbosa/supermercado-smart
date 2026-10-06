package com.example.supermercadosmart.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.supermercadosmart.data.Item
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gera um PDF estilo "nota de compras" com todos os itens, status (no carrinho ou não)
 * e o total geral, e invoca a folha de compartilhamento nativa do Android.
 */
object PdfExporter {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    fun exportAndShare(context: Context, items: List<Item>) {
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

        canvas.drawText("Supermercado Smart — Lista de Compras", marginLeft, y, titlePaint)
        y += 20f
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
        canvas.drawText("Gerado em: $dateStr", marginLeft, y, subtitlePaint)
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

        for (item in items) {
            if (y > pageHeight - 60f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            val checkbox = if (item.inCart) "[x]" else "[ ]"
            canvas.drawText(checkbox, marginLeft, y, bodyPaint)
            canvas.drawText(item.name.take(38), marginLeft + 25f, y, bodyPaint)
            canvas.drawText(item.quantity.toString(), marginLeft + 300f, y, bodyPaint)
            canvas.drawText(currencyFormat.format(item.unitPrice), marginLeft + 350f, y, bodyPaint)
            canvas.drawText(currencyFormat.format(item.totalPrice), marginLeft + 440f, y, bodyPaint)

            total += item.totalPrice
            y += 22f
        }

        y += 15f
        canvas.drawLine(marginLeft, y, pageWidth - marginLeft, y, subtitlePaint)
        y += 25f
        canvas.drawText("TOTAL GERAL: ${currencyFormat.format(total)}", marginLeft, y, totalPaint)

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

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar lista de compras"))
    }
}
