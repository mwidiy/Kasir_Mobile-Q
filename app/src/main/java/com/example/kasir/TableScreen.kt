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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.data.model.Location
import com.example.kasir.data.model.Table
import com.example.kasir.viewmodel.TableViewModel

// --- COLORS ---
private val QrBg = Color(0xFFF8F9FA)
private val QrPrimaryBlue = Color(0xFF1E3A5F)
private val QrPrimaryYellow = Color(0xFFFDD85D)
private val QrActiveGreen = Color(0xFF2ECC71)
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

data class QrStatus(
    val isOpen: Boolean = true,
    val openText: String = "Buka",
    val closedText: String = "Tutup"
)

@Composable
fun TableScreen(onNavigate: (String) -> Unit) {
    val viewModel: TableViewModel = viewModel()
    val locations by viewModel.locations.collectAsState()
    val tables by viewModel.tables.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var isFabExpanded by remember { mutableStateOf(false) }
    var globalStatus by remember { mutableStateOf(QrStatus()) }

    // Modals & Sheets State
    var showTableOptions by remember { mutableStateOf<Table?>(null) }
    var showDeleteTableConfirm by remember { mutableStateOf<Table?>(null) }
    var showQrModal by remember { mutableStateOf<Table?>(null) }
    
    var showAddTableModal by remember { mutableStateOf(false) }
    
    // Location Management
    var showAddLocationModal by remember { mutableStateOf(false) }
    var showLocationOptions by remember { mutableStateOf<Location?>(null) }
    var showEditLocationModal by remember { mutableStateOf<Location?>(null) }
    var showDeleteLocationConfirm by remember { mutableStateOf<Location?>(null) }

    // Use viewmodel selectedLocationId
    val filteredTables = if (viewModel.selectedLocationId == 0) {
        tables
    } else {
        tables.filter { it.locationId == viewModel.selectedLocationId }
    }

    Box(modifier = Modifier.fillMaxSize().background(QrBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D3E50))
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manajemen Meja & QR", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            if (isLoading && tables.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = QrPrimaryBlue)
                }
            } else {
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
                        // Location Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 15.dp)
                        ) {
                             // "Semua" Chip
                            item {
                                FilterPill(
                                    label = "Semua",
                                    isActive = viewModel.selectedLocationId == 0,
                                    onClick = { viewModel.selectedLocationId = 0 },
                                    onLongClick = {}
                                )
                            }
                            
                            // Dynamic Locations
                            // Removed the standalone "+" chip as requested.
                            // Added key for stability.
                            items(locations, key = { it.id }) { loc ->
                                FilterPill(
                                    label = loc.name,
                                    isActive = viewModel.selectedLocationId == loc.id,
                                    onClick = { viewModel.selectedLocationId = loc.id },
                                    onLongClick = { showLocationOptions = loc }
                                )
                            }
                        }
                    }

                    // Table Grid
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
                                            // Toggle functionality currently not in VM/API for 'isActive' update
                                            // Just placeholder or if API supported:
                                            // viewModel.updateTable(item.id, ..., !isActive)
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
                    
                    if (filteredTables.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("Tidak ada meja di lokasi ini", color = Color.Gray)
                            }
                        }
                    }
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
                    TableFabSubButton("Tambah Lokasi", "📍") { 
                        isFabExpanded = false
                        showAddLocationModal = true
                    }
                }
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                     TableFabSubButton("Tambah Meja", "🪑") { 
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
            TableActionSheetModal(
                title = "Opsi Meja: ${showTableOptions!!.name}",
                onEdit = { 
                    showTableOptions = null
                    // Edit logic not implemented yet
                },
                onDelete = {
                    val item = showTableOptions
                    showTableOptions = null
                    showDeleteTableConfirm = item
                },
                onDismiss = { showTableOptions = null }
            )
        }

        if (showDeleteTableConfirm != null) {
            TableConfirmationModal(
                title = "Hapus Meja Ini?",
                desc = "Menghapus meja akan menghilangkan QR code dan data terkait. Tindakan ini tidak dapat dibatalkan.",
                onConfirm = {
                    viewModel.deleteTable(showDeleteTableConfirm!!.id)
                    showDeleteTableConfirm = null
                },
                onCancel = { showDeleteTableConfirm = null }
            )
        }
        
        if (showAddTableModal) {
            AddTableDialog(
                locations = locations,
                onSave = { name, locId ->
                    viewModel.addTable(name, locId)
                    showAddTableModal = false
                },
                onCancel = { showAddTableModal = false }
            )
        }
        
        // Location Modals
        if (showAddLocationModal) {
            TableInputModal(
                title = "Tambah Lokasi",
                label = "Nama Lokasi",
                placeholder = "Contoh: Lantai 2, Outdoor",
                onSave = { name: String -> // Explicit type
                    viewModel.addLocation(name)
                    showAddLocationModal = false
                },
                onCancel = { showAddLocationModal = false }
            )
        }
        
        if (showLocationOptions != null) {
             TableActionSheetModal(
                title = "Opsi Lokasi: ${showLocationOptions!!.name}",
                onEdit = {
                    val loc = showLocationOptions
                    showLocationOptions = null
                    showEditLocationModal = loc
                },
                onDelete = {
                    val loc = showLocationOptions
                    showLocationOptions = null
                    showDeleteLocationConfirm = loc
                },
                onDismiss = { showLocationOptions = null }
            )
        }
        
        if (showEditLocationModal != null) {
            TableInputModal(
                title = "Edit Lokasi",
                label = "Nama Lokasi",
                initialValue = showEditLocationModal!!.name,
                onSave = { name: String -> // Explicit type
                    viewModel.updateLocation(showEditLocationModal!!.id, name)
                    showEditLocationModal = null
                },
                onCancel = { showEditLocationModal = null }
            )
        }
        
        if (showDeleteLocationConfirm != null) {
             TableConfirmationModal(
                title = "Hapus Lokasi?",
                desc = "Meja yang ada di lokasi ini mungkin akan kehilangan referensi lokasinya atau tidak tampil.",
                onConfirm = {
                    viewModel.deleteLocation(showDeleteLocationConfirm!!.id)
                    showDeleteLocationConfirm = null
                },
                onCancel = { showDeleteLocationConfirm = null }
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
                        Text("• ", color = if (status.isOpen) QrActiveGreen else DeleteRed, fontWeight = FontWeight.Bold)
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
fun FilterPill(label: String, isActive: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        color = if (isActive) Color(0xFF2D3E50) else Color.White,
        contentColor = if (isActive) Color.White else Color(0xFF666666),
        shape = RoundedCornerShape(50.dp),
        border = if (!isActive) BorderStroke(1.dp, Color.Transparent) else null,
        shadowElevation = if (isActive) 4.dp else 1.dp,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { onLongClick() }
            )
        }
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
fun TableCard(item: Table, onToggle: () -> Unit, onQrClick: () -> Unit, onOptionClick: () -> Unit) {
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
                        Text(
                            text = item.location?.name ?: "Unknown",
                            fontSize = 10.sp, 
                            color = QrTextMuted, 
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), 
                            fontWeight = FontWeight.Medium
                        )
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
                        painter = painterResource(id = android.R.drawable.ic_menu_camera),
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
fun QrModal(table: Table, onDismiss: () -> Unit) {
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
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Close", 
                        tint = Color.Gray, 
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(table.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = QrTextDark)
                
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
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

@Composable
fun AddTableDialog(
    locations: List<Location>,
    onSave: (String, Int) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Tambah Meja Baru", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))
                
                Text("Nomor / Nama Meja", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    placeholder = { Text("Contoh: Meja 12") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Lokasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                // DROPDOWN MENU
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedLocation?.name ?: "Pilih Lokasi",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "drop") },
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                        enabled = false, // Disable typing, handled by Box click
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledPlaceholderColor = Color.Gray,
                            disabledTrailingIconColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    // We need an invisible overlay or use ReadOnly TextField with interaction source
                    Box(modifier = Modifier.matchParentSize().clickable { expanded = true })

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f).background(Color.White)
                    ) {
                        locations.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(location.name) },
                                onClick = {
                                    selectedLocation = location
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { 
                        if (name.isNotEmpty() && selectedLocation != null) {
                            onSave(name, selectedLocation!!.id)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E50)),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotEmpty() && selectedLocation != null
                ) { 
                    Text("Simpan") 
                }
            }
        }
    }
}

// Reuse FabSubButton & Modals from MenuScreen if needed, but defining simple versions here to avoid breakage.
// Renamed to TableFabSubButton
@Composable
private fun TableFabSubButton(text: String, icon: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }) {
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D3E50)),
            contentAlignment = Alignment.Center
        ) {
             Text(icon, fontSize = 18.sp, color = Color.White)
        }
    }
}

// Simple Input Modal specific for Table/Location simple strings
// Renamed to TableInputModal
@Composable
private fun TableInputModal(title: String, label: String, placeholder: String = "", initialValue: String = "", onSave: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text, 
                    onValueChange = { text = it }, 
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                 Spacer(modifier = Modifier.height(24.dp))
                 Button(onClick = { onSave(text) }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E50)), modifier = Modifier.fillMaxWidth()) { Text("Simpan") }
            }
        }
    }
}

// Simple Action Sheet
// Renamed to TableActionSheetModal
@Composable
private fun TableActionSheetModal(title: String, onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 20.dp))
                Row(modifier = Modifier.fillMaxWidth().clickable { onEdit() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFF2D3E50), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Edit", fontWeight = FontWeight.SemiBold)
                }
                Divider(color = Color(0xFFF3F4F6))
                Row(modifier = Modifier.fillMaxWidth().clickable { onDelete() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFFFEE2E2), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = DeleteRed)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Hapus", fontWeight = FontWeight.SemiBold, color = DeleteRed)
                }
            }
        }
    }
}

// Simple Confirmation
// Renamed to TableConfirmationModal
@Composable
private fun TableConfirmationModal(title: String, desc: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                 Box(modifier = Modifier.size(72.dp).background(Color(0xFF1F2937), CircleShape), contentAlignment = Alignment.Center) {
                     Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                 }
                 Spacer(modifier = Modifier.height(16.dp))
                 Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                 Spacer(modifier = Modifier.height(8.dp))
                 Text(desc, textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp)
                 Spacer(modifier = Modifier.height(24.dp))
                 Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                     Button(onClick = onCancel, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E50)), modifier = Modifier.weight(1f)) { Text("Batal") }
                     Button(onClick = onConfirm, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = DeleteRed), modifier = Modifier.weight(1f)) { Text("Hapus") }
                 }
            }
        }
    }
}

// Made PRIVATE
private fun Modifier.scaleCustom(scale: Float) = this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))
