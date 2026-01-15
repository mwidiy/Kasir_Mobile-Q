package com.example.kasir

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.kasir.ui.theme.KasirTheme
import com.example.kasir.ui.components.FilterHistoryDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.viewmodel.RiwayatViewModel
import com.example.kasir.data.model.OrderResponse
import java.text.SimpleDateFormat
import java.util.Locale
import java.text.NumberFormat

// --- COLOR PALETTE (Scoped to History) ---
private val HistoryCardBg = Color(0xFF2D3E50)
private val HistoryTabBg = Color(0x1AFFFFFF) // rgba(255,255,255,0.1)
private val HistoryPriceGreen = Color(0xFF2ECC71)
private val HistoryPriceRed = Color(0xFFEF4444)
private val HistoryPriceBlack = Color(0xFF111827)
private val HistoryTextDark = Color(0xFF1F2937)
private val HistoryTextGray = Color(0xFF6B7280)
private val HistoryTextLightGray = Color(0xFF9CA3AF)



// --- DATA MODELS ---
// Moved to using OrderResponse directly, but keeping helpers for mapping if needed
// Or preferably, we map OrderResponse directly in the UI items




@Composable
fun RiwayatScreen(onNavigate: (String) -> Unit, viewModel: RiwayatViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedTransaction by remember { mutableStateOf<OrderResponse?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) } // State for Filter Dialog

    val transactions by viewModel.displayedOrders.collectAsState()
    val analysis by viewModel.analysis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(selectedTab) {
        viewModel.applyFilter(selectedTab)
    }

    LaunchedEffect(searchQuery) {
        viewModel.search(searchQuery)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        if (isLoading) {
             CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (error != null) {
             // ERROR STATE
             Column(
                 modifier = Modifier.align(Alignment.Center).padding(20.dp),
                 horizontalAlignment = Alignment.CenterHorizontally
             ) {
                 Text("Gagal Memuat Data", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 16.sp)
                 Spacer(modifier = Modifier.height(8.dp))
                 Text(error ?: "Unknown Error", color = Color.Gray, textAlign = TextAlign.Center)
                 Spacer(modifier = Modifier.height(16.dp))
                 Button(onClick = { viewModel.fetchHistory() }) {
                     Text("Coba Lagi")
                 }
             }
        }
        
        // Show Content only if no error (or even if error, maybe show cached, but here we block to force attention)
        if (error == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Riwayat & Laporan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, HistoryTextDark),
                    color = Color.Transparent,
                    modifier = Modifier.clickable { 
                        // EXPORT PDF LOGIC
                        val baseUrl = com.example.kasir.data.network.RetrofitClient.BASE_URL
                        val url = "${baseUrl}api/orders/export-pdf?status=${viewModel.statusFilter}&type=${viewModel.typeFilter}&search=${viewModel.currentQuery}"
                        
                        val request = DownloadManager.Request(Uri.parse(url))
                            .setTitle("Laporan Riwayat")
                            .setDescription("Mengunduh laporan PDF...")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Laporan_Riwayat_${System.currentTimeMillis()}.pdf")
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)

                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        try {
                            downloadManager.enqueue(request)
                            Toast.makeText(context, "Mulai mengunduh...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal mengunduh: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                         Text("📄", fontSize = 12.sp)
                         Text("Ekspor PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HistoryTextDark)
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = HistoryCardBg),
                            elevation = CardDefaults.cardElevation(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(HistoryTabBg, RoundedCornerShape(8.dp))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf("Hari Ini", "Minggu Ini", "Bulan Ini").forEachIndexed { index, title ->
                                        val isActive = selectedTab == index
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isActive) Color(0x40FFFFFF) else Color.Transparent)
                                                .clickable { viewModel.setTabFilter(index); selectedTab = index } // Use VM setter
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(title, color = if (isActive) Color.White else Color(0xFFCCCCCC), fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                Text("Total Pendapatan", color = Color(0xFFBDC3C7), fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                Text(analysis.totalIncome, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                UtilDivider(color = Color(0x1AFFFFFF))
                                
                                Row(modifier = Modifier.padding(top = 15.dp)) {
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(analysis.transactionCount, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Transaksi", color = Color(0xFFBDC3C7), fontSize = 11.sp)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0x33FFFFFF)))
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(analysis.avgIncome, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Rata-rata", color = Color(0xFFBDC3C7), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    decorationBox = { inner ->
                                        if (searchQuery.isEmpty()) Text("Cari Order ID...", color = Color.Gray, fontSize = 13.sp)
                                        inner()
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.size(46.dp).clickable { showFilterDialog = true }
                        ) {
                             Box(contentAlignment = Alignment.Center) {
                                 Text("⚙️", fontSize = 20.sp)
                             }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(transactions) { item ->
                    TransactionItem(item) { selectedTransaction = item }
                }
            }
        } // End of Column Content
        } // End of if (error == null)

        AppBottomNavigation(
            currentScreen = "riwayat",
            onNavigate = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (selectedTransaction != null) {
            ReceiptModal(
                transaction = selectedTransaction!!,
                onDismiss = { selectedTransaction = null }
            )
        }
        
        if (showFilterDialog) {
            FilterHistoryDialog(
                onDismiss = { showFilterDialog = false },
                onApply = { status, type ->
                    viewModel.setAdvancedFilter(status, type)
                    showFilterDialog = false
                }
            )
        }
    }
}

@Composable
fun TransactionItem(item: OrderResponse, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val date = try {
             val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
             val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
             parser.parse(item.createdAt)?.let { formatter.format(it) } ?: "-"
        } catch (e: Exception) { "-" }

        val isIncome = item.status == "Completed"
        val color = if (isIncome) HistoryPriceGreen else HistoryPriceRed

        Column(modifier = Modifier.width(50.dp)) {
            Text(date, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HistoryTextDark)
            Text("#${item.queueNumber ?: item.id}", fontSize = 11.sp, color = HistoryTextLightGray)
        }
        
        // Item Summary
        val itemSummary = if (item.items.isNotEmpty()) {
            val first = item.items[0]
            val others = item.items.size - 1
            "${first.quantity}x ${first.product?.name ?: "-"}" + if (others > 0) ", +$others lainnya" else ""
        } else "No items"

        Text(
            itemSummary,
            fontSize = 13.sp,
            color = Color(0xFF4B5563),
            maxLines = 2,
            modifier = Modifier.weight(1f).padding(horizontal = 15.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val priceStr = fmt.format(item.totalAmount).replace("Rp", "Rp ").replace(",00", "")
            Text(
                priceStr, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold, 
                color = if (isIncome) HistoryPriceBlack else HistoryPriceRed
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun ReceiptModal(transaction: OrderResponse, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                     Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                         Text("📄", fontSize = 24.sp)
                         Text("Detail Transaksi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                     }
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Text("#${transaction.id}", fontSize = 14.sp, color = HistoryTextGray)
                         Spacer(modifier = Modifier.width(8.dp))
                         Text("✕", fontSize = 20.sp, modifier = Modifier.clickable { onDismiss() })
                     }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                val dateStr = try {
                     val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                     val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                     parser.parse(transaction.createdAt)?.let { formatter.format(it) } ?: "-"
                } catch (e: Exception) { "-" }

                val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val totalStr = fmt.format(transaction.totalAmount).replace("Rp", "Rp ").replace(",00", "")

                DetailRow("Meja:", transaction.table?.id?.toString() ?: "Takeaway")
                DetailRow("Nama:", transaction.customerName)
                DetailRow("Tipe:", transaction.orderType ?: "-")
                DetailRow("Waktu:", dateStr)
                DetailRow("Status:", transaction.status)

                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = if (transaction.status == "Completed") Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    contentColor = if (transaction.status == "Completed") Color(0xFF166534) else Color(0xFF991B1B),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (transaction.status == "Completed") "Selesai" else if (transaction.status == "Cancelled") "Dibatalkan" else transaction.status, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 12.sp, 
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                UtilDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Rincian Pesanan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HistoryTextDark)
                Spacer(modifier = Modifier.height(8.dp))
                
                transaction.items.forEach { item ->
                     val pPrice = fmt.format((item.priceSnapshot ?: 0)).replace("Rp", "Rp ").replace(",00", "")
                     Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.quantity}x ${item.product?.name}", fontSize = 13.sp, color = Color(0xFF4B5563))
                        Text(pPrice, fontSize = 13.sp, color = Color(0xFF4B5563))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                UtilDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Rincian Pembayaran", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HistoryTextDark)
                 Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", fontSize = 13.sp, color = HistoryTextLightGray)
                    Text(totalStr, fontSize = 13.sp, color = HistoryTextLightGray)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HistoryTextDark)
                    Text(totalStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HistoryTextDark)
                }
                Text(transaction.paymentMethod ?: "-", fontSize = 12.sp, color = HistoryTextLightGray, modifier = Modifier.align(Alignment.End))

                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Text("Tutup", color = HistoryTextDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.width(80.dp), color = HistoryTextLightGray, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(value, fontWeight = FontWeight.Medium, color = HistoryTextDark, fontSize = 13.sp, textAlign = TextAlign.Right, modifier = Modifier.weight(1f))
    }
}

@Composable
fun UtilDivider(color: Color = Color(0xFFF3F4F6)) {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color))
}
