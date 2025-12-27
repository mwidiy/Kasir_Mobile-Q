package com.example.kasir

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kasir.ui.theme.KasirTheme

// --- COLORS ---
private val QrBg = Color(0xFFF8F9FA)
private val QrPrimaryBlue = Color(0xFF1E3A5F)
private val QrPrimaryYellow = Color(0xFFFDD85D)
private val QrActiveGreen = Color(0xFF2ECC71)
private val QrInactiveGray = Color(0xFFE0E0E0)
private val QrTextDark = Color(0xFF1F2937)
private val QrTextMuted = Color(0xFF9CA3AF)
private val StatusOpenBg = Color(0xFFFFFFFF)
private val StatusOpenBorder = Color(0xFFBBF7D0)
private val StatusClosedBg = Color(0xFFFFFFFF)
private val StatusClosedBorder = Color(0xFFFECACA)
private val AlertRedBg = Color(0xFFFEF2F2)
private val AlertRedText = Color(0xFFB91C1C)
private val AlertRedBorder = Color(0xFFFECACA)
private val InfoBoxBg = Color(0xFFF0FDF4)
private val InfoBoxText = Color(0xFF166534)
private val InfoBoxBorder = Color(0xFFBBF7D0)
private val DeleteRed = Color(0xFFEF4444)

// --- MODELS ---
data class TableItem(
    val id: String,
    val name: String,
    val area: String, // "lantai-1", "outdoor", "vip"
    val areaDisplay: String,
    val isActive: Boolean
)

val initialTableItems = listOf(
    TableItem("1", "Meja 01", "lantai-1", "Lantai 1", true),
    TableItem("2", "Meja 02", "lantai-1", "Lantai 1", true),
    TableItem("3", "Meja 03", "outdoor", "Outdoor", true),
    TableItem("4", "Meja 04", "outdoor", "Outdoor", true),
    TableItem("5", "Meja 05", "vip", "VIP", true),
    TableItem("6", "Meja 06", "vip", "VIP", true)
)

data class QrStatus(
    val isOpen: Boolean = true,
    val openText: String = "Buka",
    val closedText: String = "Tutup"
)

@Composable
fun TableScreen(onNavigate: (String) -> Unit) {
    var selectedArea by remember { mutableStateOf("all") }
    var tableList by remember { mutableStateOf(initialTableItems) }
    var globalStatus by remember { mutableStateOf(QrStatus()) }
    var isFabExpanded by remember { mutableStateOf(false) }

    // Modals & Sheets State
    var showTableOptions by remember { mutableStateOf<TableItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<TableItem?>(null) }
    var showQrModal by remember { mutableStateOf<TableItem?>(null) }
    var showAddTableModal by remember { mutableStateOf(false) }
    
    // Filtering
    val filteredTables = tableList.filter { item ->
        selectedArea == "all" || item.area == selectedArea
    }

    Box(modifier = Modifier.fillMaxSize().background(QrBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D3E50)) // Header color from CSS
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manajemen Meja & QR", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            // Scrollable Content
            LazyColumn(
                contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp, start = 20.dp, end = 20.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                item {
                    // Status Card
                    StatusCard(globalStatus, onToggle = { globalStatus = globalStatus.copy(isOpen = !globalStatus.isOpen) })
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    // Filter Tabs
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 5.dp) // padding for scrolling shadow or spacing
                    ) {
                        val areas = listOf("all" to "Semua", "lantai-1" to "Lantai 1", "outdoor" to "Outdoor", "vip" to "VIP")
                        items(areas) { (id, label) ->
                            FilterPill(
                                label = label,
                                isActive = selectedArea == id,
                                onClick = { selectedArea = id }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                }

                // Table Grid
                // We use a custom Grid layout inside LazyColumn via item chunking or simple FlowRow equivalent if available, 
                // but since we are inside a LazyColumn, we can't easily nest a LazyVerticalGrid.
                // We will manually chunk the list into rows of 2.
                items(filteredTables.chunked(2)) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (item in rowItems) {
                            Box(modifier = Modifier.weight(1f)) {
                                TableCard(
                                    item = item,
                                    onToggle = { 
                                        tableList = tableList.map { if (it.id == item.id) it.copy(isActive = !it.isActive) else it }
                                    },
                                    onQrClick = { showQrModal = item },
                                    onOptionClick = { showTableOptions = item }
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // FAB
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn(),
                exit = fadeOut(),
                 modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f)).clickable { isFabExpanded = false })
            }

            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 170.dp, end = 20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                 AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    FabSubButton("Tambah Lokasi", "📍") { /* Add Location Logic */ }
                }
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                     FabSubButton("Tambah Meja", "🪑") { 
                         isFabExpanded = false
                         showAddTableModal = true
                     }
                }
            }

            val rotation by animateFloatAsState(if (isFabExpanded) 45f else 0f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 20.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(QrPrimaryYellow)
                    .clickable { isFabExpanded = !isFabExpanded }
                    .shadow(elevation = 4.dp, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = QrTextDark, modifier = Modifier.rotate(rotation))
            }
        }

        // Bottom Nav
        AppBottomNavigation(
            currentScreen = "meja",
            onNavigate = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // --- MODALS ---
        if (showQrModal != null) {
            QrModal(
                table = showQrModal!!,
                onDismiss = { showQrModal = null }
            )
        }

        if (showTableOptions != null) {
            ActionSheetModal(
                title = "Opsi Meja: ${showTableOptions!!.name}",
                onEdit = { 
                    showTableOptions = null
                    // Edit logic 
                },
                onDelete = {
                    val item = showTableOptions
                    showTableOptions = null
                    showDeleteConfirm = item
                },
                onDismiss = { showTableOptions = null }
            )
        }

        if (showDeleteConfirm != null) {
            ConfirmationModal(
                title = "Hapus Meja Ini?",
                desc = "Menghapus meja akan menghilangkan QR code dan data terkait. Tindakan ini tidak dapat dibatalkan.",
                onConfirm = {
                    tableList = tableList.filter { it.id != showDeleteConfirm!!.id }
                    showDeleteConfirm = null
                },
                onCancel = { showDeleteConfirm = null }
            )
        }
        
        if (showAddTableModal) {
            InputModal(
                title = "Tambah Meja",
                label = "Nomor Meja",
                placeholder = "Contoh: Meja 12",
                onSave = { 
                    // Add table logic
                    showAddTableModal = false
                },
                onCancel = { showAddTableModal = false }
            )
        }
    }
}

@Composable
fun StatusCard(status: QrStatus, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (status.isOpen) StatusOpenBg else StatusClosedBg),
        border = BorderStroke(2.dp, if (status.isOpen) StatusOpenBorder else StatusClosedBorder),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Status Operasional Kantin", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = QrTextDark)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text("• ", color = if (status.isOpen) QrActiveGreen else DeleteRed, fontWeight = FontWeight.Bold) // Dot icon
                        Text(
                            if (status.isOpen) status.openText else status.closedText,
                            color = if (status.isOpen) QrActiveGreen else DeleteRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = status.isOpen,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = QrActiveGreen)
                    )
                    if (!status.isOpen) {
                        Text("OFF", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(15.dp))
            
            Surface(
                color = if (status.isOpen) InfoBoxBg else AlertRedBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (status.isOpen) InfoBoxBorder else AlertRedBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Text("ℹ️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (status.isOpen) "Semua QR code meja aktif. Matikan untuk menutup pesanan." else "Kantin tutup. Semua QR code dinonaktifkan.",
                        fontSize = 12.sp,
                        color = if (status.isOpen) InfoBoxText else AlertRedText,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FilterPill(label: String, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isActive) Color(0xFF2D3E50) else Color.White,
        contentColor = if (isActive) Color.White else Color(0xFF666666),
        shape = RoundedCornerShape(50.dp),
        border = if (!isActive) BorderStroke(1.dp, Color.Transparent) else null,
        shadowElevation = if (isActive) 4.dp else 1.dp,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun TableCard(item: TableItem, onToggle: () -> Unit, onQrClick: () -> Unit, onOptionClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = QrTextDark)
                    Surface(
                        color = Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(item.areaDisplay, fontSize = 10.sp, color = QrTextMuted, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Medium)
                    }
                }
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = QrTextMuted,
                    modifier = Modifier.size(20.dp).clickable { onOptionClick() }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // QR Placeholder
            Surface(
                color = if (item.isActive) Color(0xFFF8FAFC) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clickable(enabled = item.isActive) { onQrClick() }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_camera), // Placeholder icon
                        contentDescription = "QR",
                        tint = if (item.isActive) QrPrimaryBlue else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Lihat QR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (item.isActive) QrPrimaryBlue else Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Footer
            Row(
                modifier = Modifier.fillMaxWidth().border(0.dp, Color.Transparent).padding(top = 12.dp), // Separator visual via padding/border
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Determine visuals manually
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
            }
             Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (item.isActive) "Aktif" else "Nonaktif", fontSize = 11.sp, color = Color(0xFF888888), fontWeight = FontWeight.Medium)
                Switch(
                    checked = item.isActive,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.scaleCustom(0.8f),
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = QrActiveGreen)
                )
            }
        }
    }
}



@Composable
fun QrModal(table: TableItem, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("QR Code Meja", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel), // Fallback for close
                        contentDescription = "Close", 
                        tint = Color.Gray, 
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(table.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = QrTextDark)
                
                Spacer(modifier = Modifier.height(20.dp))

                // QR Preview (Mock)
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                      // Using a text placeholder if icon is problematic, or just a simple box
                      Text("QR CODE", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { /* Download Logic */ },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E50)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Download / Print", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Extension to scale modifier using built-in graphicsLayer
fun Modifier.scaleCustom(scale: Float) = this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))

