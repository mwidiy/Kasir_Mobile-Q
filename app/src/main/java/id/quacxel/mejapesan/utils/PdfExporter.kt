package id.quacxel.mejapesan.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import id.quacxel.mejapesan.data.model.OrderResponse
import id.quacxel.mejapesan.viewmodel.RiwayatViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfExporter {

    suspend fun export(
        context: Context,
        orders: List<OrderResponse>,
        analysis: RiwayatViewModel.AnalysisData,
        startDate: String?,
        endDate: String?
    ) = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        // Page Config (A4)
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()

        // Layout Config
        val margin = 40f
        var yPos = margin

        // --- START PAGE 1 ---
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // 1. Header
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 20f
        titlePaint.color = Color.BLACK
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Laporan Riwayat Pesanan", pageWidth / 2f, yPos + 20, titlePaint)
        
        yPos += 50
        paint.textSize = 10f
        paint.color = Color.DKGRAY
        paint.textAlign = Paint.Align.CENTER
        val dateRange = if (startDate != null && endDate != null) "$startDate - $endDate" else "Semua Waktu"
        canvas.drawText("Periode: $dateRange", pageWidth / 2f, yPos, paint)
        yPos += 15
        val generatedDate = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).apply {
            timeZone = java.util.TimeZone.getTimeZone("GMT+7")
        }.format(Date())
        canvas.drawText("Generated: $generatedDate", pageWidth / 2f, yPos, paint)

        // 2. Summary Box
        yPos += 30
        val boxHeight = 60f
        val boxWidth = pageWidth - (2 * margin)
        val boxLeft = margin

        paint.style = Paint.Style.STROKE
        paint.color = Color.GRAY
        paint.strokeWidth = 1f
        canvas.drawRect(boxLeft, yPos, boxLeft + boxWidth, yPos + boxHeight, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.textSize = 12f
        paint.textAlign = Paint.Align.LEFT
        
        // Summary Text
        val summaryY = yPos + 25
        val col1 = boxLeft + 20
        val col2 = boxLeft + boxWidth / 2 + 20
        
        // Use default bold for labels
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Pendapatan (Selesai):", col1, summaryY, paint)
        canvas.drawText("Total Transaksi:", col2, summaryY, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(analysis.totalIncome, col1, summaryY + 20, paint)
        canvas.drawText(analysis.transactionCount, col2, summaryY + 20, paint)

        yPos += boxHeight + 30

        // 3. Table Header
        val colX = floatArrayOf(margin, margin + 30, margin + 130, margin + 230, margin + 330, margin + 420)
        // No, Kode, Pelanggan, Waktu, Status, Total
        val colWidths = floatArrayOf(30f, 100f, 100f, 100f, 90f, 80f)
        val headers = arrayOf("No", "Kode", "Pelanggan", "Waktu", "Status", "Total")

        paint.color = Color.LTGRAY
        canvas.drawRect(margin, yPos - 15, pageWidth - margin, yPos + 5, paint)
        
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT

        for (i in headers.indices) {
            canvas.drawText(headers[i], colX[i], yPos, paint)
        }

        yPos += 10
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, paint)
        yPos += 20

        // 4. Data Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f

        val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val dateFmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        for ((index, order) in orders.withIndex()) {
            // Check Page Break
            if (yPos > pageHeight - margin) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = margin + 20
                
                // Redraw Header on new page? Optional. Let's keep it simple.
            }

            // Prepare Data
            val no = (index + 1).toString()
            val kode = order.transactionCode
            val custBase = if (order.customerName.length > 12) order.customerName.take(12) + "..." else order.customerName
            val customer = if (order.shippingFee != null && order.shippingFee > 0) "$custBase (+Ongkir)" else custBase
            
            // Parse Date safely
            val dateStr = DateTimeUtils.formatToWIB(order.createdAt).replace(", ", " ").take(16)

            val status = order.status
            val total = fmt.format(order.totalAmount).replace("Rp", "Rp ").replace(",00", "")

            canvas.drawText(no, colX[0], yPos, paint)
            canvas.drawText(kode, colX[1], yPos, paint)
            canvas.drawText(customer, colX[2], yPos, paint)
            canvas.drawText(dateStr, colX[3], yPos, paint)
            
            // Color status
            val statusPaint = Paint(paint)
            if (status == "Completed") statusPaint.color = Color.parseColor("#10B981") // As Green
            else if (status == "Cancelled") statusPaint.color = Color.RED
            else statusPaint.color = Color.parseColor("#F59E0B") // Amber
            
            canvas.drawText(status, colX[4], yPos, statusPaint)
            
            canvas.drawText(total, colX[5], yPos, paint)

            yPos += 20
        }

        pdfDocument.finishPage(page)

        // 5. Save File
        val fileName = "Laporan_Riwayat_${System.currentTimeMillis()}.pdf"
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: Use MediaStore (No Permission needed)
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                
                if (uri != null) {
                    val outputStream: OutputStream? = resolver.openOutputStream(uri)
                    outputStream?.use {
                        pdfDocument.writeTo(it)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Laporan disimpan di Downloads", Toast.LENGTH_LONG).show()
                        openFile(context, uri)
                    }
                } else {
                    throw IOException("Failed to create MediaStore entry")
                }
            } else {
                // Android < 10: Use External Files Dir (App Specific) No Permission needed
                // Or Public Downloads if we had permission. Strict mode prefers App Specific.
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                val outputStream = FileOutputStream(file)
                pdfDocument.writeTo(outputStream)
                outputStream.close()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Disimpan di: Android/data/.../files/Download", Toast.LENGTH_LONG).show()
                }
                // Can't easily open without FileProvider, but at least it's saved.
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            pdfDocument.close()
        }
    }

    private fun openFile(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
            context.startActivity(Intent.createChooser(intent, "Buka Laporan"))
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi pembuka PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
