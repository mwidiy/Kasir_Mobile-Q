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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.viewmodel.RiwayatViewModel
import com.example.kasir.data.model.OrderResponse
import com.example.kasir.data.model.OrderItemResponse // Import OrderItemResponse
import java.text.SimpleDateFormat
import java.util.Locale
import java.text.NumberFormat

// --- COLORS (From Source) ---
val RiwayatBgBody = Color(0xFFF8F9FA)
val RiwayatCardBg = Color(0xFF2D3E50)
val RiwayatTabBg = Color(0x1AFFFFFF) // rgba(255,255,255,0.1)
val RiwayatTabActive = Color(0x40FFFFFF) // rgba(255,255,255,0.25)
val RiwayatBorder = Color(0xFFE5E7EB)
val RiwayatYellow = Color(0xFFFDE047)
val PriceGreen = Color(0xFF2ECC71)
val PriceRed = Color(0xFFEF4444)
val PriceBlack = Color(0xFF111827)
val RiwayatTextMain = Color(0xFF1F2937)
val RiwayatTextMuted = Color(0xFF6B7280)

@Composable
fun RiwayatScreen(onNavigate: (String) -> Unit, viewModel: RiwayatViewModel = viewModel()) {
    // STATE BINDING
    val transactions by viewModel.displayedOrders.collectAsState()
    val analysis by viewModel.analysis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0=Today, 1=Week, 2=Month (Logic adapter)
    
    var selectedTransaction by remember { mutableStateOf<OrderResponse?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Logic Effects
    LaunchedEffect(selectedTab) {
        viewModel.applyFilter(selectedTab)
    }

    LaunchedEffect(searchQuery) {
        viewModel.search(searchQuery)
    }

    Box(modifier = Modifier.fillMaxSize().background(RiwayatBgBody)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { 
                RiwayatHeader(
                   onExportClick = {
                        // LOGIC: Export PDF
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
                ) 
            },
            bottomBar = { /* Custom Bottom Nav via Box */ }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = 100.dp) // Space for bottom nav
            ) {
                // SUMMARY CARD (Bound to Analysis)
                SummarySection(
                    selectedTabIdx = selectedTab, 
                    totalIncome = analysis.totalIncome, 
                    transactionCount = analysis.transactionCount, 
                    avgIncome = analysis.avgIncome,
                    onTabSelect = { index -> 
                         selectedTab = index 
                         viewModel.setTabFilter(index)
                    }
                )

                // FILTER BAR (Bound to Search)
                FilterBar(
                    query = searchQuery, 
                    onFilterClick = { showFilterDialog = true }, 
                    onQueryChange = { searchQuery = it }
                )

                // TRANSACTION LIST
                 if (isLoading) {
                     Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator(color = RiwayatCardBg)
                     }
                } else if (error != null) {
                      // Error State
                      Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                          Text("Gagal memuat data: $error", color = PriceRed)
                      }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(transactions) { item ->
                            TransactionItem(item) { selectedTransaction = item }
                        }
                    }
                }
            }
        }

        // BOTTOM NAV (Custom for Riwayat with Active State)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Transparent) 
                .navigationBarsPadding() 
                .height(100.dp)
        ) {
            // White Background Bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(80.dp)
                    .shadow(elevation = 20.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RiwayatNavItem(Icons.Filled.Dashboard, "Dasbor", false) { onNavigate("dashboard") }
                    RiwayatNavItem(Icons.Filled.ListAlt, "Riwayat", true) { /* Already here */ }
                    Spacer(modifier = Modifier.width(56.dp)) // Space for Middle Button
                    RiwayatNavItem(Icons.Filled.MenuBook, "Menu", false) { onNavigate("menu") }
                    RiwayatNavItem(Icons.Filled.QrCode, "Meja", false) { onNavigate("qr") }
                }
            }

            // Floating Middle Button (Bayar)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 10.dp)
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF1F2937), Color(0xFF111827))))
                    .clickable { onNavigate("bayar") }
                    .shadow(8.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Bayar", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // Text for Middle Button
            Text(
                text = "Bayar",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RiwayatTextMain),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }

        // RECEIPT MODAL
        if (selectedTransaction != null) {
            ReceiptModal(data = selectedTransaction!!, onDismiss = { selectedTransaction = null })
        }

        // FILTER MODAL
        if (showFilterDialog) {
            com.example.kasir.ui.components.FilterHistoryDialog(
                onDismiss = { showFilterDialog = false },
                onApply = { status, type ->
                    viewModel.setAdvancedFilter(status, type)
                    showFilterDialog = false
                }
            )
        }
    }
}

// --- COMPONENTS ---

@Composable
fun RiwayatHeader(onExportClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Riwayat & Laporan",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )
        )

        // Export Button
        Row(
            modifier = Modifier
                .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clickable { onExportClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = RiwayatTextMain)
            Text("Ekspor PDF", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = RiwayatTextMain))
        }
    }
}

@Composable
fun SummarySection(
    selectedTabIdx: Int, 
    totalIncome: String, 
    transactionCount: String, 
    avgIncome: String,
    onTabSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RiwayatCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // TABS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RiwayatTabBg, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabItem("Hari Ini", selectedTabIdx == 0) { onTabSelect(0) }
                    TabItem("Bulan Ini", selectedTabIdx == 1) { onTabSelect(1) }
                    TabItem("Semua", selectedTabIdx == 2) { onTabSelect(2) }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // INCOME
                Text("Total Pendapatan", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color(0xFFBDC3C7), fontSize = 12.sp)
                Text(totalIncome, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = Color(0x33FFFFFF), thickness = 1.dp)

                Spacer(modifier = Modifier.height(15.dp))

                // STATS GRID
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(transactionCount, style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        Text("Transaksi", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBDC3C7), fontSize = 11.sp))
                    }
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0x33FFFFFF)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(avgIncome, style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        Text("Rata-rata", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBDC3C7), fontSize = 11.sp))
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TabItem(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) RiwayatTabActive else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color = if (isActive) Color.White else Color(0xFFCCCCCC)
            )
        )
    }
}

@Composable
fun FilterBar(query: String, onFilterClick: () -> Unit, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Input
        Box(modifier = Modifier.weight(1f)) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Cari Order ID...", fontSize = 13.sp, color = RiwayatTextMuted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, RiwayatBorder, RoundedCornerShape(10.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }

        // Filter Button
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.dp, RiwayatBorder, RoundedCornerShape(10.dp))
                .clickable(onClick = onFilterClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.FilterList, contentDescription = "Filter", tint = RiwayatTextMain, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun TransactionItem(item: OrderResponse, onClick: () -> Unit) {
    val date = try {
         val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
         val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
         parser.parse(item.createdAt)?.let { formatter.format(it) } ?: "-"
    } catch (e: Exception) { "-" }

    val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val totalStr = fmt.format(item.totalAmount).replace("Rp", "Rp ").replace(",00", "")
    
    val isSuccess = item.status == "Completed"
    
     // Item Summary (First item + count)
    val itemSummary = if (item.items.isNotEmpty()) {
        val first = item.items[0]
        val others = item.items.size - 1
        "${first.quantity}x ${first.product?.name ?: "-"}" + if (others > 0) ", +$others lainnya" else ""
    } else "No items"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Meta (Left)
        Column(modifier = Modifier.width(50.dp)) {
            Text(date, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RiwayatTextMain))
            Text("#${item.queueNumber ?: item.id}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = Color(0xFF9CA3AF)))
        }

        // Desc (Middle)
        Text(
            text = itemSummary,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp, 
                color = Color(0xFF4B5563),
                lineHeight = 18.sp 
            ),
            maxLines = 2
        )

        // End (Right)
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = totalStr,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp,
                    color = if (isSuccess) PriceBlack else PriceRed
                )
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isSuccess) PriceGreen else PriceRed)
            )
        }
    }
}

@Composable
fun ReceiptModal(data: OrderResponse, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RiwayatCardBg.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(Icons.Filled.Receipt, contentDescription = null, tint = RiwayatCardBg, modifier = Modifier.size(24.dp))
                        }
                        Text("Detail Transaksi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RiwayatTextMain))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("#${data.id}", style = MaterialTheme.typography.bodySmall.copy(color = RiwayatTextMuted))
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(20.dp).clickable { onDismiss() }, tint = RiwayatTextMuted)
                    }
                }

                // DETAILS GRID
                val dateStr = try {
                     val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                     val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                     parser.parse(data.createdAt)?.let { formatter.format(it) } ?: "-"
                } catch (e: Exception) { "-" }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    DetailRow("Meja:", data.table?.name ?: "Takeaway")
                    DetailRow("Nama:", data.customerName)
                    DetailRow("Tipe:", data.orderType ?: "-")
                    DetailRow("Waktu:", dateStr)
                }

                // STATUS BADGE
                val isCompleted = data.status == "Completed"
                val statusColor = if (isCompleted) Color(0xFF166534) else if (data.status == "Cancelled") Color(0xFFB91C1C) else Color(0xFF854D0E)
                val statusBg = if (isCompleted) Color(0xFFDCFCE7) else if (data.status == "Cancelled") Color(0xFFFEE2E2) else Color(0xFFFEF08A)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusBg)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(if(isCompleted) Icons.Filled.Check else Icons.Filled.Info, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                        Text(
                            if(data.status == "Completed") "Selesai" else if(data.status == "Cancelled") "Dibatalkan" else data.status, 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusColor)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF3F4F6))
                
                // ORDER SUMMARY
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text("Rincian Pesanan", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = RiwayatTextMain), modifier = Modifier.padding(bottom = 12.dp))
                    val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                    data.items.forEach { item ->
                        val pPrice = fmt.format((item.priceSnapshot ?: 0)).replace("Rp", "Rp ").replace(",00", "")
                        OrderRow("${item.quantity}x ${item.product?.name}", pPrice)
                    }
                }
                
                HorizontalDivider(color = Color(0xFFF3F4F6))

                // PAYMENT DETAILS
                val totalStr = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(data.totalAmount).replace("Rp", "Rp ").replace(",00", "")
                
                Column(modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)) {
                    Text("Rincian Pembayaran", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = RiwayatTextMain), modifier = Modifier.padding(bottom = 12.dp))
                    PaymentRow("Subtotal", totalStr, false) // Needs separate subtotal if available, using total for now
                    PaymentRow("Total", totalStr, true)
                    PaymentRow(data.paymentMethod ?: "-", totalStr, false, fontSize = 12.sp)
                }
                
                // CLOSE BTN
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RiwayatTextMain),
                    border = BorderStroke(1.dp, RiwayatBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF), fontSize = 13.sp))
        Text(value, modifier = Modifier.weight(1f), textAlign = TextAlign.Right, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = RiwayatTextMain, fontSize = 13.sp))
    }
}

@Composable
fun OrderRow(item: String, price: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(item, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4B5563), fontSize = 13.sp))
        Text(price, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4B5563), fontSize = 13.sp))
    }
}

@Composable
fun PaymentRow(label: String, price: String, isTotal: Boolean, fontSize: TextUnit = 13.sp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = if(isTotal) 8.dp else 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = if(isTotal) RiwayatTextMain else Color(0xFF9CA3AF), fontSize = if(isTotal) 16.sp else fontSize, fontWeight = if(isTotal) FontWeight.Bold else FontWeight.Normal))
        Text(price, style = MaterialTheme.typography.bodySmall.copy(color = if(isTotal) RiwayatTextMain else Color(0xFF9CA3AF), fontSize = if(isTotal) 16.sp else fontSize, fontWeight = if(isTotal) FontWeight.Bold else FontWeight.Normal))
    }
}

@Composable
fun RiwayatNavItem(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(60.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) RiwayatTextMain else Color(0xFF9CA3AF),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) RiwayatTextMain else Color(0xFF9CA3AF)
            )
        )
    }
}
