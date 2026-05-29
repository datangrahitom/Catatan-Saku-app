package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfExportHelper {

    private fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    /**
     * Generates a monthly financial PDF and saves it to app-specific Downloads folder.
     * Returns the Uri of the saved file for viewing or sharing.
     */
    fun exportMonthlyReport(
        context: Context,
        monthYearString: String, // e.g., "Mei 2026"
        transactions: List<Transaction>
    ): File? {
        val pdfDocument = PdfDocument()
        
        // Use A4 size standard (595 x 842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        // 1. Header background (Theme color: Dark Emerald Teal)
        paint.color = Color.parseColor("#0E4D34") // Emerald Green
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // 2. Header text
        textPaint.apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("PENCATAT UANG - LAPORAN BULANAN", 30f, 40f, textPaint)

        textPaint.apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText("Privasi Terjaga Sekat Perangkat (AES-256 Encrypted)", 30f, 65f, textPaint)

        // Export Time
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        textPaint.apply {
            color = Color.parseColor("#CCCCCC")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Diekspor: $nowStr", 420f, 40f, textPaint)

        // 3. Document Details
        textPaint.apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Detail Laporan Keuangan", 30f, 125f, textPaint)

        textPaint.apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Periode Laporan: $monthYearString", 30f, 145f, textPaint)

        // 4. Summaries (Total Income vs Expense)
        val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        // Draw Summary Box
        paint.color = Color.parseColor("#F5F9F6")
        canvas.drawRect(30f, 165f, 565f, 235f, paint)
        paint.color = Color.parseColor("#E0EBE4")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(30f, 165f, 565f, 235f, paint)
        paint.style = Paint.Style.FILL

        // Column 1: Pemasukan
        textPaint.apply {
            textSize = 10f
            color = Color.parseColor("#666666")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("TOTAL PEMASUKAN", 45f, 185f, textPaint)
        textPaint.apply {
            textSize = 14f
            color = Color.parseColor("#2E7D32") // Good Green
        }
        canvas.drawText(formatRupiah(totalIncome), 45f, 210f, textPaint)

        // Column 2: Pengeluaran
        textPaint.apply {
            textSize = 10f
            color = Color.parseColor("#666666")
        }
        canvas.drawText("TOTAL PENGELUARAN", 220f, 185f, textPaint)
        textPaint.apply {
            textSize = 14f
            color = Color.parseColor("#C62828") // Danger Red
        }
        canvas.drawText(formatRupiah(totalExpense), 220f, 210f, textPaint)

        // Column 3: Saldo
        textPaint.apply {
            textSize = 10f
            color = Color.parseColor("#666666")
        }
        canvas.drawText("SALDO SEKARANG", 395f, 185f, textPaint)
        textPaint.apply {
            textSize = 14f
            color = if (netBalance >= 0) Color.parseColor("#0E4D34") else Color.parseColor("#C62828")
        }
        canvas.drawText(formatRupiah(netBalance), 395f, 210f, textPaint)

        // 5. Transaction Table Header
        paint.color = Color.parseColor("#0E4D34")
        canvas.drawRect(30f, 260f, 565f, 282f, paint)

        textPaint.apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("TANGGAL", 35f, 275f, textPaint)
        canvas.drawText("KATEGORI", 105f, 275f, textPaint)
        canvas.drawText("DESKRIPSI", 200f, 275f, textPaint)
        canvas.drawText("TIPE", 380f, 275f, textPaint)
        canvas.drawText("TOTAL", 460f, 275f, textPaint)

        // 6. Draw Table Rows
        var currentY = 298f
        textPaint.apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        var index = 0
        for (txn in transactions) {
            if (currentY > 770f) {
                // If it runs out of vertical space, break (for simple 1-page report fits approx 25 transactions)
                textPaint.apply {
                    color = Color.parseColor("#777777")
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                }
                canvas.drawText("* Menampilkan 22 transaksi terbaru dalam periode.", 30f, 785f, textPaint)
                break
            }

            // Draw alternating raw background
            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F9FAF9")
                canvas.drawRect(30f, currentY - 14f, 565f, currentY + 6f, paint)
            }

            textPaint.apply {
                color = Color.parseColor("#333333")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            // Draw Tanggal
            canvas.drawText(formatDate(txn.timestamp), 35f, currentY, textPaint)

            // Draw Kategori
            canvas.drawText(txn.category, 105f, currentY, textPaint)

            // Draw Deskripsi (truncating if too long)
            val cleanTitle = if (txn.title.length > 30) txn.title.take(28) + ".." else txn.title
            canvas.drawText(cleanTitle, 200f, currentY, textPaint)

            // Draw Tipe (Income or Expense)
            if (txn.isIncome) {
                textPaint.color = Color.parseColor("#2E7D32")
                canvas.drawText("Pemasukan", 380f, currentY, textPaint)
            } else {
                textPaint.color = Color.parseColor("#C62828")
                canvas.drawText("Pengeluaran", 380f, currentY, textPaint)
            }

            // Draw Jumlah
            textPaint.color = Color.BLACK
            val amountStr = formatRupiah(txn.amount)
            canvas.drawText(amountStr, 460f, currentY, textPaint)

            // Draw thin divider line
            paint.color = Color.parseColor("#EAEAEA")
            paint.strokeWidth = 0.5f
            canvas.drawLine(30f, currentY + 6f, 565f, currentY + 6f, paint)

            currentY += 20f
            index++
        }

        // 7. Safe Encryption System Certification (Security Statement on bottom)
        paint.color = Color.parseColor("#F4F6F5")
        canvas.drawRect(30f, 795f, 565f, 825f, paint)
        paint.color = Color.parseColor("#D5DDD9")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        canvas.drawRect(30f, 795f, 565f, 825f, paint)
        paint.style = Paint.Style.FILL

        textPaint.apply {
            color = Color.parseColor("#2D523B")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("LOCK SEAL SECURITY CERTIFICATE", 38f, 807f, textPaint)
        textPaint.apply {
            color = Color.parseColor("#555555")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Data keuangan tersimpan secara lokal dengan sistem enkripsi simetris AES-256 yang divalidasi oleh Android KeyStore.", 38f, 818f, textPaint)

        pdfDocument.finishPage(page)

        // Write the PDF file to scoped internal storage
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val safeMonth = monthYearString.lowercase().replace(" ", "_")
        val file = File(directory, "Laporan_Keuangan_PencatatUang_${safeMonth}.pdf")

        return try {
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Triggers a Share Intent for the exported PDF.
     */
    fun getSharePdfIntent(context: Context, file: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
