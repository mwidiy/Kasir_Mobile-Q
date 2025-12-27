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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kasir.ui.theme.KasirTheme

// --- COLOR PALETTE (From CSS) ---
val HeaderBg = Color(0xFF1F2937)
val CardHeaderBg = Color(0xFF1E3A8A)
val BgBody = Color(0xFFF3F4F6)
val BadgeYellow = Color(0xFFFDE047)
val BadgeText = Color(0xFF854D0E)
val BtnRed = Color(0xFFEF4444)
val BtnGreen = Color(0xFF22C55E)
val BtnDark = Color(0xFF374151)
val BannerOrangeBg = Color(0xFFFFEDD5)
val BannerOrangeText = Color(0xFFC2410C)
val BannerBlueBg = Color(0xFFDBEAFE)
val BannerBlueText = Color(0xFF1D4ED8)
val BannerGreenBg = Color(0xFFDCFCE7)
val BannerGreenText = Color(0xFF15803D)
val QtyBg = Color(0xFFFEF08A)

// --- DATA MODELS ---
data class OrderItem(
    val qty: Int,
    val name: String,
    val note: String
)

enum class OrderStatus { BARU, DIPROSES, SELESAI }
enum class OrderType { TAKEAWAY, DELIVERY, DINE_IN }

data class Order(
    val id: String,
    val tableOrName: String,
    val status: OrderStatus,
    val type: OrderType,
    val items: List<OrderItem>,
    val specialNote: String? = null,
    val address: String? = null
)

// --- DUMMY DATA ---
val sampleOrders = listOf(
    Order(
        "1", "Meja 03", OrderStatus.BARU, OrderType.TAKEAWAY,
        listOf(
            OrderItem(2, "Nasi Goreng Spesial", "+ Telur Mata Sapi, Kerupuk"),
            OrderItem(1, "Es Teh Manis", "Gula Normal")
        ),
        specialNote = "Tolong pisahkan sambel dan kerupuk"
    ),
    Order(
        "2", "R. Dosen (Gedung B)", OrderStatus.DIPROSES, OrderType.DELIVERY,
        listOf(
            OrderItem(3, "Paket Nasi Uduk", "Komplit dengan Lauk"),
            OrderItem(2, "Jus Alpukat", "Tanpa Gula")
        ),
        address = "Ruang Dosen Lt.2, Gedung B, Kampus Utara"
    ),
    Order(
        "3", "Meja 05", OrderStatus.BARU, OrderType.DINE_IN,
        listOf(
            OrderItem(1, "Mie Goreng Seafood", "Level 3 (Pedas)"),
            OrderItem(1, "Soto Ayam Kampung", "+ Nasi Putih")
        )
    )
)

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf<OrderStatus?>(null) } // null = All
    var showProfileDropdown by remember { mutableStateOf(false) }
    
    // Modal State
    var showModal by remember { mutableStateOf(false) }
    var modalType by remember { mutableStateOf("") } // "accept", "finish", "reject"
    var selectedOrder by remember { mutableStateOf<Order?>(null) }

    val filteredOrders = sampleOrders.filter { order ->
        (activeFilter == null || order.status == activeFilter) &&
        (searchQuery.isEmpty() || 
         order.tableOrName.contains(searchQuery, ignoreCase = true) ||
         order.items.any { it.name.contains(searchQuery, ignoreCase = true) })
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
                    contentPadding = PaddingValues(bottom = 100.dp), // Space for bottom nav
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(filteredOrders) { order ->
                        OrderCard(
                            order = order,
                            onAction = { type ->
                                selectedOrder = order
                                modalType = type
                                showModal = true
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Profile Dropdown Overlay
        if (showProfileDropdown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showProfileDropdown = false } // Close on click outside
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

        // Modal
        if (showModal) {
            ActionModal(
                type = modalType,
                onDismiss = { showModal = false },
                onConfirm = {
                    // Handle action logic here
                    showModal = false
                }
            )
        }
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
            // Icon Placeholder
            Text("🍳", fontSize = 24.sp) // Simple replacement for SVG
            Column {
                Text("Dapur QuackXel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Pesanan Masuk", color = Color.Gray, fontSize = 11.sp)
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
            // Avatar Image Placeholder
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
    activeFilter: OrderStatus?,
    onFilterChange: (OrderStatus?) -> Unit
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
                        if (query.isEmpty()) Text("Cari Nomor Meja, Menu...", color = Color.Gray, fontSize = 14.sp)
                        innerTextField()
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(text = "Semua", isActive = activeFilter == null, onClick = { onFilterChange(null) }) }
            item { FilterChip(text = "Baru", isActive = activeFilter == OrderStatus.BARU, onClick = { onFilterChange(OrderStatus.BARU) }) }
            item { FilterChip(text = "Diproses", isActive = activeFilter == OrderStatus.DIPROSES, onClick = { onFilterChange(OrderStatus.DIPROSES) }) }
            item { FilterChip(text = "Selesai", isActive = activeFilter == OrderStatus.SELESAI, onClick = { onFilterChange(OrderStatus.SELESAI) }) }
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
fun OrderCard(order: Order, onAction: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardHeaderBg)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(order.tableOrName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    
                    val badgeColor = if (order.status == OrderStatus.BARU) BadgeYellow else Color.Transparent
                    val badgeTxtColor = if (order.status == OrderStatus.BARU) BadgeText else Color.White
                    val badgeBorder = if (order.status == OrderStatus.BARU) null else BorderStroke(1.dp, Color.White.copy(alpha=0.6f))
                    
                    Surface(
                        color = badgeColor,
                        contentColor = badgeTxtColor,
                        shape = RoundedCornerShape(20.dp),
                        border = badgeBorder
                    ) {
                        Text(
                            text = order.status.name.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                // Banner
                val (bannerBg, bannerTxt, bannerLabel) = when (order.type) {
                    OrderType.TAKEAWAY -> Triple(BannerOrangeBg, BannerOrangeText, "Bungkus / Takeaway")
                    OrderType.DELIVERY -> Triple(BannerBlueBg, BannerBlueText, "Delivery / Antar")
                    OrderType.DINE_IN -> Triple(BannerGreenBg, BannerGreenText, "Makan di Tempat")
                }
                
                Surface(
                    color = bannerBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Text(
                        text = bannerLabel,
                        color = bannerTxt,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                // Items
                order.items.forEach { item ->
                    Row(modifier = Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(QtyBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${item.qty}x", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BadgeText)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(item.note, color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
                
                // Special Info
                if (order.specialNote != null) {
                    InfoBox(Color(0xFFFFF7ED), Color(0xFF9A3412), "Catatan: ${order.specialNote}")
                }
                if (order.address != null) {
                    InfoBox(Color(0xFFEFF6FF), Color(0xFF1E40AF), "Alamat: ${order.address}")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (order.status != OrderStatus.SELESAI) {
                        if (order.status == OrderStatus.BARU) {
                             Button(
                                onClick = { onAction("reject") },
                                colors = ButtonDefaults.buttonColors(containerColor = BtnRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Tolak") }
                            
                            Button(
                                onClick = { onAction("accept") },
                                colors = ButtonDefaults.buttonColors(containerColor = BtnGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Terima") }
                        } else {
                             Button(
                                onClick = { onAction("finish") },
                                colors = ButtonDefaults.buttonColors(containerColor = BtnDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Selesai / Antar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoBox(bgColor: Color, textColor: Color, text: String) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Text(text, color = textColor, fontSize = 12.sp)
        }
    }
}

@Composable
fun ActionModal(type: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val (title, desc, confirmColor, confirmText) = when (type) {
        "accept" -> Quadruple("Konfirmasi Pesanan?", "Anda yakin ingin memindahkan pesanan ke status 'Sedang Disiapkan'?", BadgeYellow, "Ya, Terima")
        "finish" -> Quadruple("Tandai Selesai?", "Anda yakin ingin memindahkan pesanan ke status 'Selesai'?", BadgeYellow, "Ya, Selesaikan")
        else -> Quadruple("Tolak Pesanan?", "Tindakan ini tidak dapat dibatalkan.", BtnRed, "Tolak Pesanan")
    }
    
    // Quick helper class to avoid Pair nesting
    val confirmTextColor = if (type == "reject" || type == "finish" /*wait, finish is yellow usually*/) 
                           if(type=="reject") Color.White else BadgeText 
                           else BadgeText 

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon placeholder
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(if(type=="reject") Color(0xFFFEE2E2) else HeaderBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if(type=="reject") "❌" else "✅", fontSize = 24.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(desc, color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Batal", color = BtnDark) }
                    
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text(confirmText, color = confirmTextColor) }
                }
            }
        }
    }
}

// Helper for quadruple
data class Quadruple<A,B,C,D>(val first: A, val second: B, val third: C, val fourth: D)

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    KasirTheme {
        DashboardScreen(onNavigate = {})
    }
}
