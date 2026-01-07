package com.example.kasir

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.example.kasir.data.model.Location
import com.example.kasir.data.model.Table
import com.example.kasir.data.model.TableRequest
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.kasir.utils.ImageSaver
import com.example.kasir.utils.QRCodeHelper
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.utils.QRCodeImage
import kotlinx.coroutines.launch

private val BASE_PWA_URL = BuildConfig.PWA_BASE_URL.removeSuffix("/")

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
    // State Management for Data
    var tableList by remember { mutableStateOf<List<Table>>(emptyList()) }
    
    // Location Filter List from API (storing Objects now)
    // Create a dummy Location for "Semua" to simplify the list
    var locationList by remember { mutableStateOf<List<Location>>(listOf(Location(-1, "Semua"))) }
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Filter State
    var selectedLocation by remember { mutableStateOf("Semua") }

    var globalStatus by remember { mutableStateOf(QrStatus()) }

    // Modals & Dialogs
    var showTableOptions by remember { mutableStateOf<Table?>(null) }
    var showDeleteTableConfirm by remember { mutableStateOf<Table?>(null) }
    var showQrModal by remember { mutableStateOf<Table?>(null) }
    var showAddTableModal by remember { mutableStateOf(false) }

    // Edit Table State
    var currentEditingTable by remember { mutableStateOf<Table?>(null) }
    
    // New Dialog States
    var showAddOptionDialog by remember { mutableStateOf(false) }
    var showAddLocationDialog by remember { mutableStateOf(false) }

    // Location Edit/Delete States
    var activeLocationMenuId by remember { mutableStateOf<Int?>(null) }
    var showEditLocationDialog by remember { mutableStateOf<Location?>(null) }
    var showDeleteLocationConfirm by remember { mutableStateOf<Location?>(null) }

    // Helper to refresh data
    val refreshData = {
        scope.launch {
            try {
                // Fetch Tables
                val tables = RetrofitClient.instance.getTables()
                tableList = tables
                
                // Fetch Locations
                val locations = RetrofitClient.instance.getLocations()
                // Sort by ID or Name if needed. Assuming server order or alphabetical
                val sortedLocs = locations.sortedBy { it.name }
                locationList = listOf(Location(-1, "Semua")) + sortedLocs
            } catch (e: Exception) {
                errorMessage = "Gagal memuat ulang data: ${e.localizedMessage}"
            }
        }
    }

    // Fetch Tables and Locations Initial
    LaunchedEffect(Unit) {
        isLoading = true
        refreshData()
        isLoading = false
    }

    // Filter Logic
    val filteredTables = if (selectedLocation == "Semua") {
        tableList
    } else {
        tableList.filter { it.location?.name == selectedLocation }
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

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = QrPrimaryBlue)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage ?: "Error", color = Color.Red, modifier = Modifier.padding(16.dp))
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
                        // Location Chips (Direct from API)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 15.dp)
                        ) {
                            items(locationList) { location ->
                                Box {
                                    FilterPill(
                                        label = location.name,
                                        isActive = selectedLocation == location.name,
                                        onClick = { selectedLocation = location.name },
                                        onLongClick = {
                                            if (location.name != "Semua") {
                                                activeLocationMenuId = location.id
                                            }
                                        }
                                    )
                                    
                                    // Dropdown Menu for Edit/Delete
                                    DropdownMenu(
                                        expanded = activeLocationMenuId == location.id,
                                        onDismissRequest = { activeLocationMenuId = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                activeLocationMenuId = null
                                                showEditLocationDialog = location
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Hapus", color = Color.Red) },
                                            onClick = {
                                                activeLocationMenuId = null
                                                showDeleteLocationConfirm = location
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                                        )
                                    }
                                }
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
                                        onToggle = { isActive ->
                                            // Optimistic Update
                                             val optimisticItem = item.copy(isActive = isActive)
                                             tableList = tableList.map { if (it.id == item.id) optimisticItem else it }

                                             scope.launch {
                                                 try {
                                                     val response = RetrofitClient.instance.updateTableStatus(item.id, mapOf("isActive" to isActive))
                                                     if (response.isSuccessful && response.body() != null) {
                                                         // Success: Sync with backend truth
                                                         val serverItem = response.body()!!
                                                         tableList = tableList.map { if (it.id == serverItem.id) serverItem else it }
                                                     } else {
                                                         // Failed: Revert to original item
                                                         tableList = tableList.map { if (it.id == item.id) item else it }
                                                         errorMessage = "Gagal update status: ${response.code()}"
                                                     }
                                                 } catch (e: Exception) {
                                                     // Error: Revert to original item
                                                     tableList = tableList.map { if (it.id == item.id) item else it }
                                                     errorMessage = "Error status: ${e.localizedMessage}"
                                                 }
                                             }
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

        // FAB - Refactored for Multi-Action
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 20.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(QrPrimaryYellow)
                .clickable { showAddOptionDialog = true }
                .shadow(elevation = 4.dp, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = QrTextDark)
        }

        // Bottom Nav
        AppBottomNavigation(
            currentScreen = "meja",
            onNavigate = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // --- MODALS ---
        
        // 1. Option Dialog (Selection)
        if (showAddOptionDialog) {
            Dialog(onDismissRequest = { showAddOptionDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth().padding(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Pilih Aksi", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = QrTextDark)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = { 
                                showAddOptionDialog = false
                                showAddLocationDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = QrPrimaryBlue),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Tambah Lokasi Baru")
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = { 
                                showAddOptionDialog = false
                                currentEditingTable = null // Reset edit state
                                showAddTableModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = QrPrimaryYellow),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Tambah Meja / QR", color = QrTextDark)
                        }
                    }
                }
            }
        }
        
        // 2. Add Location Dialog
        if (showAddLocationDialog) {
            AddLocationDialog(
                onSave = { name ->
                    scope.launch {
                        try {
                            RetrofitClient.instance.addLocation(mapOf("name" to name))
                            refreshData() // Refresh chips
                            showAddLocationDialog = false
                        } catch(e: Exception) {
                            e.printStackTrace()
                            errorMessage = "Gagal tambah lokasi: ${e.localizedMessage}"
                        }
                    }
                },
                onCancel = { showAddLocationDialog = false }
            )
        }

        // 3. Edit Location Dialog
        if (showEditLocationDialog != null) {
            EditLocationDialog(
                location = showEditLocationDialog!!,
                onSave = { id, name ->
                    scope.launch {
                        try {
                            RetrofitClient.instance.updateLocation(id, mapOf("name" to name))
                            refreshData()
                            showEditLocationDialog = null
                        } catch(e: Exception) {
                            errorMessage = "Gagal update lokasi: ${e.localizedMessage}"
                        }
                    }
                },
                onCancel = { showEditLocationDialog = null }
            )
        }

        // 4. Delete Location Confirm
        if (showDeleteLocationConfirm != null) {
            TableConfirmationModal(
                title = "Hapus Lokasi?",
                desc = "Menghapus lokasi '${showDeleteLocationConfirm!!.name}' mungkin memengaruhi meja yang ada di sana.",
                onConfirm = {
                    val id = showDeleteLocationConfirm!!.id
                    scope.launch {
                        try {
                            val res = RetrofitClient.instance.deleteLocation(id)
                            if (res.isSuccessful) {
                                // If current selected location is deleted, reset to "Semua"
                                if (selectedLocation == showDeleteLocationConfirm!!.name) {
                                    selectedLocation = "Semua"
                                }
                                refreshData()
                            } else {
                                errorMessage = "Gagal hapus: ${res.code()}"
                            }
                            showDeleteLocationConfirm = null
                        } catch(e: Exception) {
                            errorMessage = "Gagal hapus lokasi: ${e.localizedMessage}"
                        }
                    }
                },
                onCancel = { showDeleteLocationConfirm = null }
            )
        }

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
                    currentEditingTable = showTableOptions
                    showTableOptions = null
                    showAddTableModal = true // Reuse Add Modal for Edit
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
                    val id = showDeleteTableConfirm!!.id
                    scope.launch {
                        try {
                            val response = RetrofitClient.instance.deleteTable(id)
                            if (response.isSuccessful) {
                                tableList = tableList.filter { it.id != id }
                            } else {
                                errorMessage = "Gagal hapus meja: ${response.code()}"
                            }
                        } catch(e: Exception) {
                            errorMessage = "Gagal hapus meja: ${e.localizedMessage}"
                        }
                        showDeleteTableConfirm = null
                    }
                },
                onCancel = { showDeleteTableConfirm = null }
            )
        }
        
        if (showAddTableModal) {
            // Determine initial values based on edit mode
            val initialName = currentEditingTable?.name ?: ""
            val initialLocation = if (currentEditingTable != null) {
                 locationList.find { it.id == currentEditingTable!!.location?.id }
            } else null

            // Only pass actual locations
            AddTableDialog(
                locations = locationList.filter { it.name != "Semua" },
                initialName = initialName,
                initialLocation = initialLocation,
                isEditMode = currentEditingTable != null,
                onSave = { name, locationId ->
                    scope.launch {
                        try {
                            if (currentEditingTable != null) {
                                // UPDATE MODE
                                val qrCode = currentEditingTable!!.qrCode ?: "QR-${name}-${System.currentTimeMillis()}"
                                val tableRequest = TableRequest(
                                    name = name,
                                    locationId = locationId,
                                    qrCode = qrCode,
                                    isActive = currentEditingTable!!.isActive
                                )
                                val response = RetrofitClient.instance.updateTable(currentEditingTable!!.id, tableRequest)
                                if (response.isSuccessful && response.body() != null) {
                                    val updatedItem = response.body()!!
                                    
                                    // Live Update Logic
                                    val index = tableList.indexOfFirst { it.id == updatedItem.id }
                                    if (index != -1) {
                                        val mutableList = tableList.toMutableList()
                                        mutableList[index] = updatedItem
                                        tableList = mutableList
                                    }
                                    
                                    showAddTableModal = false
                                    currentEditingTable = null
                                } else {
                                    errorMessage = "Gagal update meja: ${response.code()}"
                                }
                            } else {
                                // CREATE MODE
                                val qrCode = "QR-${name}-${System.currentTimeMillis()}"
                                val tableRequest = TableRequest(
                                    name = name,
                                    locationId = locationId,
                                    qrCode = qrCode,
                                    isActive = true
                                )
                                val newTableResponse = RetrofitClient.instance.addTable(tableRequest)
                                if (newTableResponse.isSuccessful && newTableResponse.body() != null) {
                                    val newItem = newTableResponse.body()!!
                                    tableList = tableList + newItem
                                    showAddTableModal = false
                                } else {
                                    errorMessage = "Gagal tambah meja: ${newTableResponse.code()}"
                                }
                            }
                        } catch(e: Exception) {
                            e.printStackTrace()
                            errorMessage = "Error: ${e.localizedMessage}"
                        }
                    }
                },
                onCancel = { 
                    showAddTableModal = false 
                    currentEditingTable = null
                }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterPill(label: String, isActive: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        color = if (isActive) Color(0xFF2D3E50) else Color.White,
        contentColor = if (isActive) Color.White else Color(0xFF666666),
        shape = RoundedCornerShape(50.dp),
        border = if (!isActive) BorderStroke(1.dp, Color.Transparent) else null,
        shadowElevation = if (isActive) 4.dp else 1.dp,
        modifier = Modifier.clip(RoundedCornerShape(50.dp)).combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
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
fun TableCard(item: Table, onToggle: (Boolean) -> Unit, onQrClick: () -> Unit, onOptionClick: () -> Unit) {
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
                color = if (item.isActive != false) Color(0xFFF8FAFC) else Color(0xFFF1F5F9), // Handle default true if null? Boolean is non-null in data class
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
                    if (!item.qrCode.isNullOrBlank() && item.isActive) {
                         QRCodeImage(
                            content = "$BASE_PWA_URL/?tableId=${item.qrCode}",
                            modifier = Modifier
                                .size(90.dp)
                                .padding(8.dp)
                        )
                    } else {
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
                    onCheckedChange = { onToggle(it) },
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
                        .size(200.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                      if (!table.qrCode.isNullOrBlank()) {
                          QRCodeImage(
                              content = "$BASE_PWA_URL/?tableId=${table.qrCode}",
                              modifier = Modifier.fillMaxSize().padding(16.dp)
                          )
                      } else {
                          Text("QR CODE TIDAK TERSEDIA", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                      }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val qrString = "$BASE_PWA_URL/?tableId=${table.qrCode ?: table.id}"
                            val bitmap = QRCodeHelper.generateQrBitmap(qrString)
                            if (bitmap != null) {
                                val success = ImageSaver.saveBitmapToGallery(context, bitmap, "QR_${table.name}")
                                withContext(Dispatchers.Main) {
                                    if (success) Toast.makeText(context, "QR Tersimpan di Galeri!", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, "Gagal menyimpan QR", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTableDialog(
    locations: List<Location>, // Changed to accept List<Location>
    initialName: String = "",
    initialLocation: Location? = null,
    isEditMode: Boolean = false,
    onSave: (String, Int) -> Unit, // Changed to return Location ID
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedLocation by remember { mutableStateOf<Location?>(initialLocation) }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(if(isEditMode) "Edit Meja" else "Tambah Meja Baru", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                
                // Dropdown Menu
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedLocation?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Pilih Lokasi") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
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

@Composable
fun AddLocationDialog(
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Tambah Lokasi Baru", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))
                
                Text("Nama Lokasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    placeholder = { Text("Contoh: Rooftop") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { 
                        if (name.isNotEmpty()) {
                            onSave(name)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E50)),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotEmpty()
                ) { 
                    Text("Simpan") 
                }
            }
        }
    }
}

@Composable
fun EditLocationDialog(
    location: Location,
    onSave: (Int, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(location.name) }

    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Edit Lokasi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))
                
                Text("Nama Lokasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    placeholder = { Text("Contoh: Rooftop") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { 
                        if (name.isNotEmpty()) {
                            onSave(location.id, name)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E50)),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotEmpty()
                ) { 
                    Text("Simpan Perubahan") 
                }
            }
        }
    }
}

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
                HorizontalDivider(color = Color(0xFFF3F4F6))
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
