package com.example.kasir

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.example.kasir.data.model.LocationData
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
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.scale
import kotlinx.coroutines.async


private val BASE_PWA_URL = BuildConfig.PWA_BASE_URL.removeSuffix("/")


// --- ANIMATION CONSTANTS ---
private const val ANIMATION_DURATION = 3000

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
fun TableScreen(
    onNavigate: (String) -> Unit, 
    viewModel: com.example.kasir.viewmodel.TableViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // State Management for Data from ViewModel
    val tables by viewModel.tables.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val isStoreOpen by viewModel.isStoreOpen.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val apiErrorMessage by viewModel.errorMessage.collectAsState()

    // Location Filter List
    val locationList = listOf(Location(-1, "Semua")) + locations.sortedBy { it.name }
    
    // Logic Effects: Force Light Status Bar Icons (White) like Dashboard
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Filter State
    var selectedLocation by remember { mutableStateOf("Semua") }

    // Sync Store Status
    var globalStatus by remember(isStoreOpen) { 
        mutableStateOf(QrStatus(isOpen = isStoreOpen)) 
    }

    // Modals & Dialogs
    var showTableOptions by remember { mutableStateOf<Table?>(null) }
    var showDeleteTableConfirm by remember { mutableStateOf<Table?>(null) }
    var showQrModal by remember { mutableStateOf<Table?>(null) }
    var showAddTableModal by remember { mutableStateOf(false) }

    // Edit Table State
    var currentEditingTable by remember { mutableStateOf<Table?>(null) }
    
    // UI Resilience (Snackbar)
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Anti-Spam (Race Condition) lock for individual table switches
    // submittingTableIds removed for local state shadow
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            errorMessage = null
        }
    }

    LaunchedEffect(apiErrorMessage) {
        apiErrorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }
    
    // New Dialog States
    var showAddOptionDialog by remember { mutableStateOf(false) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showStatusGuideDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    var showLocationOptions by remember { mutableStateOf<Location?>(null) } // New Action Sheet State

    // Location Edit/Delete States
    var activeLocationMenuId by remember { mutableStateOf<Int?>(null) }
    var showEditLocationDialog by remember { mutableStateOf<Location?>(null) }
    var showDeleteLocationConfirm by remember { mutableStateOf<Location?>(null) }

    // Filter Logic
    val filteredTables = if (selectedLocation == "Semua") {
        tables
    } else {
        tables.filter { it.location?.name == selectedLocation }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = QrBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D3E50))
                    .statusBarsPadding()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manajemen Meja & QR", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {

            if (isLoading && tables.isEmpty()) {
                // SHIMMER SKELETON UI LOADING
                TableSkeletonLoading()
            } else {
                // Fixed Header Area
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    // Status Card
                    StatusCard(globalStatus,
                        onToggle = { onComplete -> 
                            val newStatus = !globalStatus.isOpen
                            globalStatus = globalStatus.copy(isOpen = newStatus) // Optimistic Local for child tables
                            
                            viewModel.updateStoreStatus(newStatus) {
                                onComplete() // Release lock ONLY when server finishes
                            }
                        },
                        onInfoClick = { showStatusGuideDialog = true }
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Location Chips (Direct from API)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 5.dp) // Reduced bottom padding as grid handles spacing
                    ) {
                        items(locationList) { location ->
                            Box {
                                FilterPill(
                                    label = location.name,
                                    isActive = selectedLocation == location.name,
                                    onClick = { selectedLocation = location.name },
                                    onLongClick = {
                                        if (location.name != "Semua") {
                                            showLocationOptions = location
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Scrollable Content (Table Grid)
                LazyColumn(
                    contentPadding = PaddingValues(top = 10.dp, bottom = 180.dp, start = 20.dp, end = 20.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
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
                                        isGlobalOpen = globalStatus.isOpen,
                                        onToggle = { isActive, onComplete ->
                                            viewModel.updateTableStatus(item.id, isActive) {
                                                onComplete()
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

        // FAB - Refactored for Multi-Action (Matches MenuScreen)
        Box(modifier = Modifier.fillMaxSize()) {
            // Overlay
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { isFabExpanded = false }
                )
            }

            // FAB Items
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 240.dp, end = 20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    FabSubButton("Tambah Lokasi Baru", "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z") {
                        isFabExpanded = false
                        showAddLocationDialog = true
                    }
                }
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    FabSubButton("Tambah Meja / QR", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") {
                        isFabExpanded = false
                        currentEditingTable = null // Reset edit state
                        showAddTableModal = true
                    }
                }
            }

            // Main FAB
            val rotation by animateFloatAsState(if (isFabExpanded) 45f else 0f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 170.dp, end = 20.dp)
                    .size(56.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(QrPrimaryYellow)
                    .clickable { isFabExpanded = !isFabExpanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Add", 
                    tint = QrTextDark,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }

// Bottom Nav Removed (Handled by MainScreen)
        
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
                    viewModel.addLocation(name) { success ->
                        if (success) showAddLocationDialog = false
                    }
                },
                onCancel = { showAddLocationDialog = false }
            )
        }

        // 3. Edit Location Dialog
        if (showEditLocationDialog != null) {
            val editingLoc = showEditLocationDialog!!
            EditLocationDialog(
                location = editingLoc,
                onSave = { id, name ->
                    viewModel.updateLocation(id, name) { success ->
                        if (success) {
                            if (selectedLocation == editingLoc.name) selectedLocation = name
                            showEditLocationDialog = null
                        }
                    }
                },
                onCancel = { showEditLocationDialog = null }
            )
        }

        // New Location Action Sheet
        if (showLocationOptions != null) {
            TableActionSheetModal(
                title = "Opsi Lokasi: ${showLocationOptions!!.name}",
                onEdit = {
                    val loc = showLocationOptions
                    showLocationOptions = null
                    showEditLocationDialog = loc
                },
                onDelete = {
                     val loc = showLocationOptions
                    showLocationOptions = null
                    showDeleteLocationConfirm = loc
                },
                onDismiss = { showLocationOptions = null }
            )
        }

        // 4. Delete Location Confirm
        if (showDeleteLocationConfirm != null) {
            TableConfirmationModal(
                title = "Hapus Lokasi?",
                desc = "Menghapus lokasi '${showDeleteLocationConfirm!!.name}' mungkin memengaruhi meja yang ada di sana.",
                onConfirm = {
                    val locToDelete = showDeleteLocationConfirm!!
                    viewModel.deleteLocation(locToDelete.id) { success ->
                        if (success) {
                            if (selectedLocation == locToDelete.name) selectedLocation = "Semua"
                            showDeleteLocationConfirm = null
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

        if (showStatusGuideDialog) {
            StatusGuideModal(onDismiss = { showStatusGuideDialog = false })
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
                    val tableToDelete = showDeleteTableConfirm!!
                    viewModel.deleteTable(tableToDelete.id) { success ->
                        if (success) {
                            showDeleteTableConfirm = null
                        }
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
                    val locData = locationList.find { it.id == locationId }
                    if (locData != null) {
                        if (currentEditingTable != null) {
                            // === EDIT MODE ===
                            viewModel.updateTable(currentEditingTable!!.id, name, locationId) { success ->
                                if (success) {
                                    showAddTableModal = false
                                    currentEditingTable = null
                                }
                            }
                        } else {
                            // === CREATE MODE ===
                            viewModel.addTable(name, locationId) { success ->
                                if (success) {
                                    showAddTableModal = false
                                }
                            }
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
}

@Composable
fun StatusCard(status: QrStatus, onToggle: (onComplete: () -> Unit) -> Unit, onInfoClick: () -> Unit) {
    val context = LocalContext.current
    var clickTimestamps by remember { mutableStateOf(listOf<Long>()) }

    // === ZERO-BOUNCE UI ===
    var isLocalOpen by remember(status.isOpen) { mutableStateOf(status.isOpen) }
    var isUpdating by remember { mutableStateOf(false) }

    LaunchedEffect(status.isOpen) {
        if (!isUpdating) {
            isLocalOpen = status.isOpen
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLocalOpen) StatusOpenBg else StatusClosedBg),
        border = BorderStroke(2.dp, if (isLocalOpen) StatusOpenBorder else StatusClosedBorder),
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
                    Text("Status Operasional QR", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = QrTextDark)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text("• ", color = if (isLocalOpen) QrActiveGreen else DeleteRed, fontWeight = FontWeight.Bold)
                        Text(
                            if (isLocalOpen) status.openText else status.closedText,
                            color = if (isLocalOpen) QrActiveGreen else DeleteRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = isLocalOpen,
                        onCheckedChange = { isOpen -> 
                            val now = System.currentTimeMillis()
                            val recentClicks = clickTimestamps.filter { now - it < 1500 }
                            
                            if (recentClicks.size >= 3) {
                                Toast.makeText(context, "Terlalu cepat! Tunggu sebentar.", Toast.LENGTH_SHORT).show()
                                clickTimestamps = recentClicks + now
                                return@Switch
                            }
                            
                            clickTimestamps = recentClicks + now
                            isLocalOpen = isOpen
                            isUpdating = true
                            
                            onToggle {
                                isUpdating = false
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = QrActiveGreen)
                    )
                    if (!isLocalOpen) {
                        Text("OFF", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(15.dp))
            
            Surface(
                color = if (status.isOpen) InfoBoxBg else AlertRedBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (status.isOpen) InfoBoxBorder else AlertRedBorder),
                modifier = Modifier.fillMaxWidth().clickable { onInfoClick() }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    SvgIcon(
                        pathData = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z",
                        tint = if (status.isOpen) InfoBoxText else AlertRedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Tekan untuk mendapatkan informasi lengkap mengenai status operasional.",
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
fun StatusGuideModal(onDismiss: () -> Unit) {
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
                    Text("Status Operasional", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Animation Scene
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    GuideAnimation()
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = buildAnnotatedString {
                        append("Saat status '")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black)) {
                            append("Buka")
                        }
                        append("' (Aktif), QR code dapat discan pelanggan. Jika '")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black)) {
                            append("Tutup")
                        }
                        append("', pemesanan dihentikan.")
                    },
                    color = Color(0xFF6B7280),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QrPrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mengerti", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun GuideAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "guide")
    
    // Phase: 0-1 across 2.5 seconds (Faster, snappier)
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Timeline:
    // 0.0 - 0.2: Hand moves to switch
    // 0.2 - 0.35: Hand Press (Scale Down)
    // 0.35: TOGGLE SWITCH
    // 0.35 - 0.5: Hand Release (Scale Up)
    // 0.5 - 1.0: Hand moves away + Wait

    val togglePoint = 0.35f
    val isSwitchOn = phase < togglePoint || phase > 0.95f // Resets at end

    // Switch/Knob State
    val switchColor = if (isSwitchOn) QrActiveGreen else Color.Gray
    val knobOffset = if (isSwitchOn) 22.dp else 2.dp 

    // Hand Position Logic
    val handX = when {
        phase < 0.2f -> androidx.compose.ui.unit.lerp(80.dp, 20.dp, phase / 0.2f) // Move to switch
        phase < 0.5f -> 20.dp // Hold at switch
        phase < 0.8f -> androidx.compose.ui.unit.lerp(20.dp, 100.dp, (phase - 0.5f) / 0.3f) // Move away
        else -> 80.dp // Reset pos
    }
    
    val handY = when {
        phase < 0.2f -> androidx.compose.ui.unit.lerp(80.dp, 5.dp, phase / 0.2f) 
        phase < 0.5f -> 5.dp
        phase < 0.8f -> androidx.compose.ui.unit.lerp(5.dp, 100.dp, (phase - 0.5f) / 0.3f)
        else -> 80.dp
    }
    
    val handScale = if (phase in 0.2f..0.35f) 0.85f else 1f
    val handAlpha = if (phase > 0.8f) 0f else 1f

    // Card State
    val cardAlpha = if (!isSwitchOn) 0.4f else 1f
    val showLock = !isSwitchOn

    Box(modifier = Modifier.size(180.dp, 80.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp)) {
        // Content Layer
        Row(
            Modifier.fillMaxWidth().graphicsLayer { alpha = cardAlpha }, // Apply blur/dim here
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
             Box(Modifier.width(60.dp).height(8.dp).background(Color(0xFFE0E0E0), CircleShape))
             
             // Switch
             Box(
                 modifier = Modifier.width(40.dp).height(22.dp).background(switchColor, CircleShape)
             ) {
                 Box(Modifier.size(18.dp).offset(x = knobOffset, y = 2.dp).background(Color.White, CircleShape))
             }
        }
        
        // Lock Icon Overlay (Bounce In)
        AnimatedVisibility(
            visible = showLock,
            enter = androidx.compose.animation.scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = androidx.compose.animation.scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xCC000000), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
    
    // Hand Cursor
    Icon(
        imageVector = Icons.Default.TouchApp,
        contentDescription = "Hand",
        modifier = Modifier
            .offset(x = handX, y = handY)
            .scaleCustom(handScale)
            .graphicsLayer { alpha = handAlpha }
            .size(32.dp)
            .rotate(-20f), 
        tint = QrPrimaryBlue
    )
}

@Composable
fun ScanGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
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
                    Text("Panduan Pelanggan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Animation Scene
                Box(
                    modifier = Modifier
                        .size(200.dp, 160.dp)
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                   ScanAnimation()
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Pelanggan dapat melakukan scan pada QR code ini menggunakan kamera ponsel untuk membuka daftar menu.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                     colors = ButtonDefaults.buttonColors(containerColor = QrPrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mengerti", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ScanAnimation() {
    val transition = rememberInfiniteTransition(label = "scan_sequence")
    val duration = 4000 // Total loop time

    // 1. Phone Hover (Removed - Static)
    
    // 2. Hand Scale (Tap effect at 1000ms)
    val handScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = duration
                1f at 0
                1f at 800
                0.85f at 1000 // Press
                1f at 1200 // Release
                1f at duration
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "handTap"
    )

    // 3. Laser Visibility (Visible ONLY after tap, 1200ms+)
    val laserAlpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = duration
                0f at 0
                0f at 1200
                1f at 1300 // Fade in
                1f at 3800
                0f at 4000 // Fade out reset
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "laserAlpha"
    )

    // 4. Laser Movement (Scans during visibility)
    val laserY by transition.animateFloat(
        initialValue = -40f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = duration
                -40f at 0
                -40f at 1300 // Start position
                40f at 2500 // Scan Down
                -40f at 3700 // Scan Up
                -40f at duration
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "laserY"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) { // Constrain container
        
        // Step 1: Small Solid QR in Background (The target)
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .align(Alignment.Center) 
                .graphicsLayer { alpha = 0.2f } // Subtle background hint
        )

        // Step 2: Phone Frame (Hovering) - Smaller Size (70x120)
        Box(
            modifier = Modifier
                .size(70.dp, 120.dp)
                .background(Color(0xFF2D3E50), RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF4B5563), RoundedCornerShape(12.dp))
                .padding(3.dp)
        ) {
            // Screen
            Box(
                 modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE5E7EB), RoundedCornerShape(9.dp))
                    .clip(RoundedCornerShape(9.dp)),
                 contentAlignment = Alignment.Center
            ) {
                 // QR Pattern on Screen (Simulating Camera Feed)
                 QrPattern(modifier = Modifier.size(40.dp))
                 
                 // Viewfinder Corners
                 Box(Modifier.fillMaxSize().padding(6.dp).border(1.5.dp, QrPrimaryBlue.copy(alpha=0.6f), RoundedCornerShape(6.dp)))

                 // Laser Line (Step 3: Scanner appears)
                 Box(
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(2.dp)
                         .offset(y = laserY.dp)
                         .graphicsLayer { alpha = laserAlpha } // Controls visibility
                         .background(
                             brush = Brush.horizontalGradient(
                                 colors = listOf(Color.Transparent, QrActiveGreen, Color.Transparent)
                             )
                         )
                         .shadow(4.dp)
                 )
            }
        }
        
        // Step 2: Hand Interaction (Tapping) - Smaller Size
        Icon(
             Icons.Default.TouchApp,
             contentDescription = null,
             tint = QrPrimaryBlue,
             modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 15.dp, y = 30.dp) // Offset to bottom-right of center
                .graphicsLayer { 
                    scaleX = handScale 
                    scaleY = handScale 
                }
                .rotate(-20f)
                .size(40.dp)
        )
    }
}


@Composable
fun QrPattern(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val color = Color.Black.copy(alpha = 0.7f)
        
        // Helper to draw a finder pattern (Square in Square)
        fun drawFinder(left: Float, top: Float, sizeVal: Float) {
            drawRect(color, topLeft = Offset(left, top), size = Size(sizeVal, sizeVal))
            drawRect(Color.White, topLeft = Offset(left + sizeVal*0.16f, top + sizeVal*0.16f), size = Size(sizeVal*0.68f, sizeVal*0.68f))
            drawRect(color, topLeft = Offset(left + sizeVal*0.33f, top + sizeVal*0.33f), size = Size(sizeVal*0.34f, sizeVal*0.34f))
        }

        // 3 Finder Patterns (TopLeft, TopRight, BottomLeft)
        val finderSize = w * 0.3f
        drawFinder(0f, 0f, finderSize) // TL
        drawFinder(w - finderSize, 0f, finderSize) // TR
        drawFinder(0f, h - finderSize, finderSize) // BL

        // Random Data Dots (Simulated)
        val step = w * 0.1f
        for (i in 3 until 7) {
            for (j in 3 until 7) {
                if ((i + j) % 2 == 0) { // Checkered pattern
                     drawRect(color, topLeft = Offset(i * step, j * step), size = Size(step*0.8f, step*0.8f))
                }
            }
        }
         // Extra dots near finders
         drawRect(color, topLeft = Offset(w * 0.4f, 0f), size = Size(step, step))
         drawRect(color, topLeft = Offset(0f, h * 0.4f), size = Size(step, step))
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
fun TableCard(item: Table, isGlobalOpen: Boolean, onToggle: (Boolean, onComplete: () -> Unit) -> Unit, onQrClick: () -> Unit, onOptionClick: () -> Unit) {
    val isLocked = !isGlobalOpen
    val context = LocalContext.current
    
    // Anti-Spam UX logic
    var clickTimestamps by remember { mutableStateOf(listOf<Long>()) }

    // === ZERO-BOUNCE UI ===
    var isLocalActive by remember(item.id) { mutableStateOf(item.isActive) }
    var isUpdating by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(item.isActive) {
        if (!isUpdating) {
            isLocalActive = item.isActive
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) Color(0xFFF9FAFB) else Color.White // Dimmed if locked
        ),
        elevation = CardDefaults.cardElevation(if (isLocked) 0.dp else 2.dp),
        border = if (isLocked) BorderStroke(1.dp, Color(0xFFE5E7EB)) else null,
        modifier = Modifier.fillMaxWidth().then(
            if (isLocked) Modifier.alpha(0.8f) else Modifier
        )
    ) {
        Box {
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
                    
                    if (!isLocked) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = QrTextMuted,
                            modifier = Modifier.size(20.dp).clickable { onOptionClick() }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // QR Placeholder
                Surface(
                    color = if (isLocalActive) Color(0xFFF8FAFC) else Color(0xFFF1F5F9), 
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable(enabled = !isLocked && isLocalActive) { onQrClick() } // Disable click if locked
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (!item.qrCode.isNullOrBlank() && isLocalActive) {
                             QRCodeImage(
                                content = "$BASE_PWA_URL/?tableId=${item.qrCode}",
                                modifier = Modifier
                                    .size(90.dp)
                                    .padding(8.dp)
                                    .then(if(isLocked) Modifier.alpha(0.3f) else Modifier)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_camera),
                                contentDescription = "QR",
                                tint = if (isLocalActive && !isLocked) QrPrimaryBlue else Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Lihat QR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isLocalActive && !isLocked) QrPrimaryBlue else Color.Gray)
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
                    Text(if (isLocalActive) "Aktif" else "Nonaktif", fontSize = 11.sp, color = Color(0xFF888888), fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isLocalActive,
                        onCheckedChange = { isActive ->
                            // Rate Limiter Logic
                            val now = System.currentTimeMillis()
                            val recentClicks = clickTimestamps.filter { now - it < 1500 }
                            
                            if (recentClicks.size >= 3) {
                                // Spam detected!
                                Toast.makeText(context, "Terlalu cepat! Tunggu sebentar.", Toast.LENGTH_SHORT).show()
                                clickTimestamps = recentClicks + now
                                return@Switch
                            } 
                            
                            // Safe to toggle
                            clickTimestamps = recentClicks + now
                            isLocalActive = isActive
                            isUpdating = true
                            
                            onToggle(isActive) {
                                isUpdating = false
                            }
                        },
                        modifier = Modifier.scaleCustom(0.8f),
                        enabled = !isLocked, // Only disabled if globally locked by store
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White, 
                            checkedTrackColor = QrActiveGreen,
                            disabledCheckedTrackColor = QrActiveGreen.copy(alpha=0.5f),
                            disabledUncheckedTrackColor = Color.Gray.copy(alpha=0.3f)
                        )
                    )
                }
            }

            // LOCK OVERLAY
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = QrTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QrModal(table: Table, onDismiss: () -> Unit) {
    var showScanGuide by remember { mutableStateOf(false) }

    if (showScanGuide) {
        ScanGuideDialog(onDismiss = { showScanGuide = false })
    }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("QR Code Meja", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = QrPrimaryBlue,
                            modifier = Modifier.clickable { showScanGuide = true }
                        )
                    }
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Close", 
                        tint = Color.Gray, 
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(table.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                
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
                    Text("Download", fontWeight = FontWeight.Bold, color = Color.White)
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
    var isSubmitting by remember { mutableStateOf(false) } // Anti-spam lock

    Dialog(onDismissRequest = { if (!isSubmitting) onCancel() }) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if(isEditMode) "Edit Meja" else "Tambah Meja Baru", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    if (!isSubmitting) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.clickable { onCancel() }
                        )
                    }
                }
                
                Text("Nomor / Nama Meja", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, 
                    onValueChange = { newValue -> 
                        // Sanitasi XSS & Max Length 30 (Alphanumeric + Space/Dash)
                        if (newValue.length <= 30) {
                            name = newValue.replace(Regex("[^a-zA-Z0-9 -]"), "")
                        }
                    }, 
                    placeholder = { Text("Contoh: Meja 12") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Lokasi", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
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
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            cursorColor = Color.Black,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
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
                        if (name.isNotEmpty() && selectedLocation != null && !isSubmitting) {
                            isSubmitting = true
                            // Trim to remove trailing spaces mimicking spaces bypass
                            onSave(name.trim(), selectedLocation!!.id)
                            // isSubmitting reset intentionally left out, assuming parent closes dialog on success/catch
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubmitting) Color.Gray else Color(0xFF2D3E50)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotEmpty() && selectedLocation != null && !isSubmitting
                ) { 
                    Text(if (isSubmitting) "Menyimpan..." else "Simpan", color = Color.White) 
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
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isSubmitting) onCancel() }) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                
                 // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tambah Lokasi Baru", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    if (!isSubmitting) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.clickable { onCancel() }
                        )
                    }
                }
                
                Text("Nama Lokasi", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, 
                    onValueChange = { newValue -> 
                        // Sanitasi XSS & Max Length 50 (Lokasi butuh slightly longer misal "Lantai 2 - Samping Kaca")
                        if (newValue.length <= 50) {
                            name = newValue.replace(Regex("[^a-zA-Z0-9 -]"), "")
                        }
                    }, 
                    placeholder = { Text("Contoh: Rooftop") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { 
                        if (name.isNotEmpty() && !isSubmitting) {
                            isSubmitting = true
                            onSave(name.trim())
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubmitting) Color.Gray else Color(0xFF2D3E50)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotEmpty() && !isSubmitting
                ) { 
                    Text(if (isSubmitting) "Menyimpan..." else "Simpan", color = Color.White) 
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
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isSubmitting) onCancel() }) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Edit Lokasi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))
                
                Text("Nama Lokasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, 
                    onValueChange = { newValue -> 
                        if (newValue.length <= 50) {
                            name = newValue.replace(Regex("[^a-zA-Z0-9 -]"), "")
                        }
                    }, 
                    placeholder = { Text("Contoh: Rooftop") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { 
                        if (name.isNotEmpty() && !isSubmitting) {
                            isSubmitting = true
                            onSave(location.id, name.trim())
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubmitting) Color.Gray else Color(0xFF2D3E50)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotEmpty() && !isSubmitting
                ) { 
                    Text(if (isSubmitting) "Menyimpan..." else "Simpan Perubahan") 
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
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 20.dp), color = Color(0xFF1F2937))
                Row(modifier = Modifier.fillMaxWidth().clickable { onEdit() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFF2D3E50), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Edit", fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
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
    var isSubmitting by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = { if (!isSubmitting) onCancel() }) {
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
                     Button(
                         onClick = onCancel, 
                         shape = RoundedCornerShape(12.dp), 
                         colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E50)), 
                         modifier = Modifier.weight(1f),
                         enabled = !isSubmitting
                     ) { Text("Batal") }
                     
                     Button(
                         onClick = {
                             if (!isSubmitting) {
                                 isSubmitting = true
                                 onConfirm()
                             }
                         }, 
                         shape = RoundedCornerShape(12.dp), 
                         colors = ButtonDefaults.buttonColors(containerColor = if (isSubmitting) Color.Gray else DeleteRed), 
                         modifier = Modifier.weight(1f),
                         enabled = !isSubmitting
                     ) { Text(if (isSubmitting) "..." else "Hapus") }
                 }
            }
        }
    }
}

// Made PRIVATE
private fun Modifier.scaleCustom(scale: Float) = this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))

// =========================================================================
// SKELETON LOADING (SHIMMER EFFECT)
// =========================================================================

@Composable
fun TableSkeletonLoading() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val shimmerTranslate = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        start = Offset(shimmerTranslate.value - 200f, shimmerTranslate.value - 200f),
        end = Offset(shimmerTranslate.value, shimmerTranslate.value)
    )

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
        // Status Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Location Chips Skeleton
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(shimmerBrush)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Table Grid Skeleton
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) { // Rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(2) { // Columns
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}
