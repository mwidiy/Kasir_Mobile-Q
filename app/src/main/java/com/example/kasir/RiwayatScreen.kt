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
import androidx.compose.ui.window.Dialog
import com.example.kasir.ui.theme.KasirTheme

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
data class Transaction(
    val id: String,
    val time: String,
    val description: String,
    val price: String,
    val isIncome: Boolean, // true = green, false = red
    val details: TransactionDetails? = null
)

data class TransactionDetails(
    val table: String,
    val cashier: String,
    val type: String,
    val fullTime: String,
    val status: String,
    val items: List<Pair<String, String>>, // Name, Price
    val subtotal: String,
    val tax: String,
    val total: String,
    val paymentMethod: String
)

val sampleTransactions = listOf(
    Transaction("0510", "14:30", "2x Nasi Gudeg, 1x Es Teh Manis", "Rp 85.000", true, 
        TransactionDetails("12", "Ahmed Ridlo", "Makan Di Tempat", "28 Okt 2025, 14:30 WIB", "Selesai",
            listOf("2x Nasi Gudeg" to "Rp 70.000", "1x Es Teh Manis" to "Rp 15.000"), "Rp 85.000", "Rp 0", "Rp 85.000", "Tunai")),
    Transaction("0509", "14:15", "1x Ayam Bakar, 2x Nasi Putih, 1x Jus Jeruk", "Rp 125.000", true, null),
    Transaction("0508", "13:45", "3x Soto Ayam, 3x Es Teh", "Rp 90.000", true, null),
    Transaction("0507", "13:20", "1x Rendang, 1x Nasi Goreng", "Rp 75.000", false, null),
    Transaction("0506", "12:55", "2x Gado-Gado, 2x Teh Botol", "Rp 60.000", true, null),
    Transaction("0505", "12:30", "1x Nasi Campur, 1x Kopi Susu", "Rp 55.000", true, null)
)

@Composable
fun RiwayatScreen(onNavigate: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    
    val totalIncome = when(selectedTab) {
        0 -> "Rp 8.542.000"
        1 -> "Rp 42.100.000"
        else -> "Rp 150.500.000"
    }
    val transCount = when(selectedTab) { 0 -> "142" 1 -> "850" else -> "3200" }
    val avgIncome = when(selectedTab) { 0 -> "Rp 60k" 1 -> "Rp 58k" else -> "Rp 62k" }

    val filteredList = sampleTransactions.filter { 
        it.id.contains(searchQuery, true) || it.description.contains(searchQuery, true)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
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
                    modifier = Modifier.clickable { /* Export logic */ }
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
                                                .clickable { selectedTab = index }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(title, color = if (isActive) Color.White else Color(0xFFCCCCCC), fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                Text("Total Pendapatan", color = Color(0xFFBDC3C7), fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                Text(totalIncome, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                UtilDivider(color = Color(0x1AFFFFFF))
                                
                                Row(modifier = Modifier.padding(top = 15.dp)) {
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(transCount, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Transaksi", color = Color(0xFFBDC3C7), fontSize = 11.sp)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0x33FFFFFF)))
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(avgIncome, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.size(46.dp).clickable { }
                        ) {
                             Box(contentAlignment = Alignment.Center) {
                                 Text("⚙️", fontSize = 20.sp)
                             }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(filteredList) { item ->
                    TransactionItem(item) { selectedTransaction = item }
                }
            }
        }

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
    }
}

@Composable
fun TransactionItem(item: Transaction, onClick: () -> Unit) {
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
        Column(modifier = Modifier.width(50.dp)) {
            Text(item.time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HistoryTextDark)
            Text("#${item.id}", fontSize = 11.sp, color = HistoryTextLightGray)
        }
        Text(
            item.description,
            fontSize = 13.sp,
            color = Color(0xFF4B5563),
            maxLines = 2,
            modifier = Modifier.weight(1f).padding(horizontal = 15.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                item.price, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold, 
                color = if (item.isIncome) HistoryPriceBlack else HistoryPriceRed
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (item.isIncome) HistoryPriceGreen else HistoryPriceRed)
            )
        }
    }
}

@Composable
fun ReceiptModal(transaction: Transaction, onDismiss: () -> Unit) {
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

                val details = transaction.details ?: TransactionDetails(
                    "12", "Ahmed Ridlo", "Makan Di Tempat", "28 Okt 2025, ${transaction.time} WIB", "Selesai",
                    listOf("Item Summary" to transaction.description), transaction.price, "Rp 0", transaction.price, "Tunai"
                )

                DetailRow("Meja:", details.table)
                DetailRow("Kasir:", details.cashier)
                DetailRow("Tipe:", details.type)
                DetailRow("Waktu:", details.fullTime)

                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = Color(0xFFDCFCE7),
                    contentColor = Color(0xFF166534),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Selesai", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                UtilDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Rincian Pesanan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HistoryTextDark)
                Spacer(modifier = Modifier.height(8.dp))
                details.items.forEach { (name, price) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, fontSize = 13.sp, color = Color(0xFF4B5563))
                        Text(price, fontSize = 13.sp, color = Color(0xFF4B5563))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                UtilDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Rincian Pembayaran", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HistoryTextDark)
                 Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", fontSize = 13.sp, color = HistoryTextLightGray)
                    Text(details.subtotal, fontSize = 13.sp, color = HistoryTextLightGray)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HistoryTextDark)
                    Text(details.total, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HistoryTextDark)
                }
                Text(details.paymentMethod, fontSize = 12.sp, color = HistoryTextLightGray, modifier = Modifier.align(Alignment.End))

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
