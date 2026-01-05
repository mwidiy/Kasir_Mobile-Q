package com.example.kasir

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.data.model.*
import com.example.kasir.viewmodel.DashboardViewModel
import java.util.Locale

// --- COLOR PALETTE ---
val HeaderBg = Color(0xFF1F2937)
val CardHeaderBg = Color(0xFF1E3A8A)
val BgBody = Color(0xFFF3F4F6)
val TextMain = Color(0xFF1F2937)
val TextMuted = Color(0xFF6B7280)

// Badge Colors
val BadgeNewBg = Color(0xFFFDE047)
val BadgeNewText = Color(0xFF854D0E)
val BadgeProcessBorder = Color(0x99FFFFFF)

// Banner Colors
val BannerOrangeBg = Color(0xFFFFEDD5)
val BannerOrangeText = Color(0xFFC2410C)
val BannerOrangeBorder = Color(0xFFFED7AA)

val BannerBlueBg = Color(0xFFDBEAFE)
val BannerBlueText = Color(0xFF1D4ED8)
val BannerBlueBorder = Color(0xFFBFDBFE)

val BannerGreenBg = Color(0xFFDCFCE7)
val BannerGreenText = Color(0xFF15803D)
val BannerGreenBorder = Color(0xFFBBF7D0)

// Qty Badge
val QtyBg = Color(0xFFFEF08A)
val QtyText = Color(0xFF854D0E)

// Info Box
val InfoNoteBg = Color(0xFFFFF7ED)
val InfoNoteText = Color(0xFF9A3412)
val InfoNoteBorder = Color(0xFFFFEDD5)

val InfoAddressBg = Color(0xFFEFF6FF)
val InfoAddressText = Color(0xFF1E40AF)
val InfoAddressBorder = Color(0xFFDBEAFE)

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit, viewModel: DashboardViewModel = viewModel()) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    DashboardScreenContent(
        orders = orders,
        isLoading = isLoading,
        error = error,
        onNavigate = onNavigate,
        onUpdateStatus = { id, status -> viewModel.updateStatus(id, status) }
    )
}

@Composable
fun DashboardScreenContent(
    orders: List<OrderResponse>,
    isLoading: Boolean,
    error: String?,
    onNavigate: (String) -> Unit,
    onUpdateStatus: (Int, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // all, Pending, Processing, Completed

    val filteredOrders = orders.filter { order ->
        val matchesSearch = order.customerName.contains(searchQuery, ignoreCase = true) ||
                (order.table?.name?.contains(searchQuery, ignoreCase = true) == true)
        
        val matchesFilter = when (selectedFilter) {
            "all" -> true
            "Pending" -> order.status == "Pending"
            "Processing" -> order.status == "Processing"
            "Completed" -> order.status == "Completed"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    // Wrap in Box to overlay BottomNavigation similar to RiwayatScreen pattern
    Box(modifier = Modifier.fillMaxSize().background(BgBody)) {
        Scaffold(
            containerColor = Color.Transparent, 
            topBar = {
                DashboardTopBar()
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Bar
                SearchBar(searchQuery) { searchQuery = it }

                // Filter Chips
                FilterSection(selectedFilter) { selectedFilter = it }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CardHeaderBg)
                    }
                } else if (error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: $error", color = Color.Red)
                    }
                } else if (filteredOrders.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 120.dp) // Space for Bottom Nav
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            KitchenOrderCard(order = order, onUpdateStatus = onUpdateStatus)
                        }
                    }
                }
            }
        }

        // Bottom Navigation (Pinned to Bottom)
        AppBottomNavigation(
            currentScreen = "dashboard",
            onNavigate = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun DashboardTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Menu, contentDescription = null, tint = BadgeNewBg)
            Column {
                Text(
                    text = "Dapur QuackXel",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Pesanan Masuk",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF))
                )
            }
        }
        // Avatar Placeholder
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFE4E6))
                .border(2.dp, Color(0x33FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = TextMain)
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Cari Nomor Meja, Menu...", style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color.Transparent, RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

@Composable
fun FilterSection(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(label = "Semua", isSelected = selected == "all", onClick = { onSelect("all") })
        FilterChip(label = "Baru", isSelected = selected == "Pending", onClick = { onSelect("Pending") })
        FilterChip(label = "Diproses", isSelected = selected == "Processing", onClick = { onSelect("Processing") })
        FilterChip(label = "Selesai", isSelected = selected == "Completed", onClick = { onSelect("Completed") })
    }
}

@Composable
fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) HeaderBg else Color.White
    val textColor = if (isSelected) Color.White else TextMuted
    val borderColor = if (isSelected) HeaderBg else Color(0xFFE5E7EB)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = textColor))
    }
}

@Composable
fun KitchenOrderCard(order: OrderResponse, onUpdateStatus: (Int, String) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // HEADER (Standardized)
            CardHeader(order)

            Column(modifier = Modifier.padding(16.dp)) {
                // BANNER
                OrderTypeBanner(order.orderType)

                Spacer(modifier = Modifier.height(16.dp))

                // ITEM LIST
                order.items.forEach { item ->
                    OrderItemRow(item)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // INFO BOXES
                // Catatan (Displayed below items as requested)
                if (!order.note.isNullOrEmpty()) {
                    InfoBox(
                        icon = Icons.Default.Info,
                        text = "Catatan: ${order.note}",
                        bgColor = InfoNoteBg,
                        textColor = InfoNoteText,
                        borderColor = InfoNoteBorder
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Lokasi / Alamat (Only for Delivery as requested)
                if (order.orderType.equals("delivery", ignoreCase = true) && !order.deliveryAddress.isNullOrEmpty()) {
                    InfoBox(
                        icon = Icons.Default.LocationOn,
                        text = "Alamat: ${order.deliveryAddress}",
                        bgColor = InfoAddressBg,
                        textColor = InfoAddressText,
                        borderColor = InfoAddressBorder
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (order.orderType.equals("delivery", ignoreCase = true)) {
                    // Fallback if address empty but valid delivery type
                     InfoBox(
                        icon = Icons.Default.LocationOn,
                        text = "Alamat: -",
                        bgColor = InfoAddressBg,
                        textColor = InfoAddressText,
                        borderColor = InfoAddressBorder
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ACTION BUTTONS
                ActionButtons(order, onUpdateStatus)
            }
        }
    }
}

@Composable
fun CardHeader(order: OrderResponse) {
    val statusLabel = when (order.status) {
        "Pending" -> "Baru"
        "Processing" -> "Diproses"
        "Completed" -> "Selesai"
        else -> order.status
    }
    
    // STRICT HEADER LOGIC (User Request Round 4)
    // 1. Top Line: REQUIRED "Location Qr" + "Table Qr" (e.g. "UKMI 1")
    // 2. Bottom Line: "Customer Name"
    // 3. NO TRX Code.
    
    val locName = order.table?.location?.name ?: ""
    val tableName = order.table?.name ?: ""
    
    // Construct Top Text: "Location Name" + "Table Name"
    // If Location is empty, just Table Name. If both empty (shouldn't happen per user), fallback to "-"
    val topText = buildString {
        if (locName.isNotEmpty()) append("$locName ")
        if (tableName.isNotEmpty()) append(tableName)
        if (isEmpty()) append("-") // Fallback
    }

    val bottomText = order.customerName

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardHeaderBg)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // 1. Top Line: Location + Table (Bold)
            Text(
                text = topText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            // 2. Bottom Line: Customer Name (Normal)
            Text(
                text = bottomText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (order.status == "Pending") BadgeNewBg else Color.Transparent)
                .border(1.dp, if (order.status == "Pending") Color.Transparent else BadgeProcessBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (order.status == "Pending") BadgeNewText else Color.White
                )
            )
        }
    }
}

data class BannerStyle(
    val bg: Color,
    val text: Color,
    val border: Color,
    val icon: ImageVector,
    val label: String
)

@Composable
fun OrderTypeBanner(orderType: String) {
    val style = when (orderType.lowercase()) {
        "takeaway" -> BannerStyle(BannerOrangeBg, BannerOrangeText, BannerOrangeBorder, Icons.Default.ShoppingCart, "Bungkus / Takeaway") 
        "delivery" -> BannerStyle(BannerBlueBg, BannerBlueText, BannerBlueBorder, Icons.Default.Send, "Delivery / Antar") 
        else -> BannerStyle(BannerGreenBg, BannerGreenText, BannerGreenBorder, Icons.Default.Home, "Makan di Tempat") 
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(style.bg)
            .border(1.dp, style.border, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = style.icon, contentDescription = null, tint = style.text, modifier = Modifier.size(18.dp))
        Text(
            text = style.label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = style.text)
        )
    }
}

@Composable
fun OrderItemRow(item: OrderItemResponse) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(QtyBg),
            contentAlignment = Alignment.Center
        ) {
            Text("${item.quantity}x", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = QtyText))
        }
        Column {
            Text(
                text = item.product.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextMain)
            )
            if (!item.note.isNullOrEmpty()) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }
    }
}

@Composable
fun InfoBox(icon: ImageVector, text: String, bgColor: Color, textColor: Color, borderColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp).offset(y = 2.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall.copy(color = textColor), lineHeight = 18.sp)
    }
}

@Composable
fun ActionButtons(order: OrderResponse, onUpdateStatus: (Int, String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (order.status == "Pending") {
            Button(
                onClick = { /* Handle Reject Logic later */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(45.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tolak", fontSize = 14.sp)
            }
            Button(
                onClick = { onUpdateStatus(order.id, "Processing") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(45.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Terima", fontSize = 14.sp)
            }
        } else if (order.status == "Processing") {
            Button(
                onClick = { onUpdateStatus(order.id, "Completed") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(45.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Selesai / Antar", fontSize = 14.sp)
            }
        } else {
             Button(
                onClick = { },
                enabled = false,
                colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFF9CA3AF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(45.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Pesanan Selesai", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.List, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Belum ada pesanan masuk", style = MaterialTheme.typography.bodyLarge, color = TextMuted)
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    val sampleOrder = OrderResponse(
        id = 1,
        transactionCode = "TRX-123",
        customerName = "Budi (Preview)",
        table = OrderTableResponse(1, "01", "TBL-123-U", OrderLocationResponse("Indoor")),
        status = "Pending",
        paymentStatus = "Unpaid",
        orderType = "dinein",
        totalAmount = 30000,
        note = "Jangan pedas",
        deliveryAddress = null,
        createdAt = "2023-10-27T10:00:00",
        items = listOf(
            OrderItemResponse(1, 2, "Tanpa sayur", OrderProductResponse("Nasi Goreng", 15000, null))
        )
    )
    DashboardScreenContent(listOf(sampleOrder), false, null, {}, { _, _ -> })
}
