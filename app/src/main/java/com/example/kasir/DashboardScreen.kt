package com.example.kasir

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.data.model.Order
import com.example.kasir.data.model.OrderStatusRequest
import com.example.kasir.ui.theme.KasirTheme
import com.example.kasir.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.util.Locale
import java.text.SimpleDateFormat

// --- COLOR PALETTE ---
val HeaderBg = Color(0xFF1F2937)
val CardHeaderBg = Color(0xFF1E3A8A)
val BgBody = Color(0xFFF3F4F6)
val BadgeYellow = Color(0xFFFDE047)
val BadgeText = Color(0xFF854D0E)
val BtnRed = Color(0xFFEF4444)
val BtnGreen = Color(0xFF22C55E)
val BtnDark = Color(0xFF374151)
val QtyBg = Color(0xFFFEF08A)
val BannerBlueText = Color(0xFF1D4ED8)
val LightYellowBg = Color(0xFFFEF9C3)

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit, viewModel: DashboardViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    // Filter by backend status value
    var activeFilter by remember { mutableStateOf<String?>(null) } // null = All
    var showProfileDropdown by remember { mutableStateOf(false) }

    val orders by viewModel.orders.collectAsState()

    val filteredOrders = orders.filter { order ->
        (activeFilter == null || order.status == activeFilter) &&
        (searchQuery.isEmpty() ||
         (order.transactionCode ?: "").contains(searchQuery, ignoreCase = true) ||
         (order.customerName ?: "").contains(searchQuery, ignoreCase = true) ||
         (order.items ?: emptyList()).any { it.product.name.contains(searchQuery, ignoreCase = true) })
    }

    Box(modifier = Modifier.fillMaxSize().background(BgBody)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header
            TopHeader(onAvatarClick = { showProfileDropdown = !showProfileDropdown })

            // Content
            Column(modifier = Modifier.weight(1f)) {
                // Search & Filter
                SearchAndFilterSection(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    activeFilter = activeFilter,
                    onFilterChange = { activeFilter = it }
                )

                // Order List
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(filteredOrders) { order ->
                        KitchenOrderCard(
                            order = order,
                            onUpdateStatus = { newStatus ->
                                viewModel.updateStatus(order.id, newStatus)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Profile Dropdown
        if (showProfileDropdown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showProfileDropdown = false }
            )
            ProfileDropdown(modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 20.dp)
            )
        }

        // Bottom Navigation
        AppBottomNavigation(
            currentScreen = "dashboard",
            onNavigate = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun TopHeader(onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBg)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🍳", fontSize = 24.sp)
            Column {
                Text("Dapur QuackXel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Kitchen Display System", color = Color.Gray, fontSize = 11.sp)
            }
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFE4E6))
                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("👤", fontSize = 18.sp)
        }
    }
}

@Composable
fun ProfileDropdown(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(220.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Warung Oyan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Text("oyan@gmail.com", fontSize = 14.sp, color = Color.Gray)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE5E7EB))
            Row(modifier = Modifier.clickable { }, verticalAlignment = Alignment.CenterVertically) {
                Text("Logout", color = BtnRed, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SearchAndFilterSection(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilter: String?,
    onFilterChange: (String?) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Search
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) Text("Cari Pesanan...", color = Color.Gray, fontSize = 14.sp)
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(text = "Semua", isActive = activeFilter == null, onClick = { onFilterChange(null) }) }
            item { FilterChip(text = "Baru", isActive = activeFilter == "Pending", onClick = { onFilterChange("Pending") }) }
            item { FilterChip(text = "Dibayar", isActive = activeFilter == "Paid", onClick = { onFilterChange("Paid") }) }
            item { FilterChip(text = "Selesai", isActive = activeFilter == "Completed", onClick = { onFilterChange("Completed") }) }
        }
    }
}

@Composable
fun FilterChip(text: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) HeaderBg else Color.White
    val textColor = if (isActive) Color.White else Color.Gray
    val border = if (isActive) null else BorderStroke(1.dp, Color(0xFFE5E7EB))

    Surface(
        color = bgColor,
        shape = CircleShape,
        border = border,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun KitchenOrderCard(order: Order, onUpdateStatus: (String) -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    // Parse time
    val orderTime = try {
        val createdAt = order.createdAt ?: ""
        if (createdAt.contains("T")) {
             val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
             val date = parser.parse(createdAt.substringBefore(".")) 
             val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
             date?.let { formatter.format(it) } ?: createdAt
        } else {
             createdAt
        }
    } catch (e: Exception) {
        order.createdAt ?: "-"
    }

    // Helper Mapping (Terjemahan Tipe Pesanan)
    val typeLabel = when (order.orderType?.lowercase()) {
        "dinein" -> "🍽️ Makan di Sini"
        "takeaway" -> "🎁 Bungkus"
        "delivery" -> "🛵 Antar"
        else -> order.orderType?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Pesanan"
    }

    // Helper Mapping (Identitas Meja)
    val tableLabel = if (order.table != null) {
         "${order.table.location?.name ?: ""} ${order.table.name}".trim()
    } else {
        ""
    }

    // Header Logic (Judul Kartu)
    val locationText = if (order.table != null) {
        "$typeLabel • $tableLabel"
    } else {
        typeLabel
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header: Location (Left) & Time (Right)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardHeaderBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = locationText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = orderTime,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Customer Name
                Text(
                    text = order.customerName ?: "Pelanggan",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
                
                // Global Note (Catatan)
                val note = order.globalNote
                if (!note.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = LightYellowBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, BadgeYellow)
                    ) {
                        Text(
                            text = "Catatan: $note",
                            modifier = Modifier.padding(8.dp),
                            color = BadgeText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(16.dp))

                // Items List (Menu)
                Text("Menu:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                order.items?.forEach { item ->
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "${item.quantity}x",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BtnDark,
                                modifier = Modifier.width(32.dp)
                            )
                            Column {
                                Text(
                                    text = item.product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                                // Item Note
                                val itemNote = item.note
                                if (!itemNote.isNullOrEmpty()) {
                                    Text(
                                        text = itemNote,
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(16.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total
                    Column {
                        Text(text = "Total", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = currencyFormat.format(order.totalAmount),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = BtnGreen
                        )
                    }

                    // Payment Badge
                    Surface(
                        color = if (order.paymentStatus == "Paid") Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (order.paymentStatus == "Paid") "LUNAS" else "BELUM BAYAR",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (order.paymentStatus == "Paid") Color(0xFF15803D) else Color(0xFFB91C1C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button
                 if (order.status == "Pending") {
                    Button(
                        onClick = { onUpdateStatus("Paid") },
                        colors = ButtonDefaults.buttonColors(containerColor = BtnGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Proses Pesanan")
                    }
                } else if (order.status == "Paid" || order.status == "Processing") {
                     Button(
                        onClick = { onUpdateStatus("Completed") },
                        colors = ButtonDefaults.buttonColors(containerColor = BtnDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Selesai / Antar")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    KasirTheme {
        DashboardScreen(onNavigate = {})
    }
}
