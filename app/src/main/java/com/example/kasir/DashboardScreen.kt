package com.example.kasir

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.data.model.*
import com.example.kasir.ui.components.CustomBottomNavigation
import com.example.kasir.ui.components.PaymentConfirmationDialog
import com.example.kasir.ui.components.PaymentSuccessDialog
import com.example.kasir.ui.components.CancellationReviewDialog
import com.example.kasir.ui.components.ForceCancelDialog
import com.example.kasir.viewmodel.DashboardViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions


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

val PaymentHighlight = Color(0xFFFDE047) // Yellow for visibility on Blue

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit, 
    viewModel: DashboardViewModel = viewModel(),
    profileViewModel: com.example.kasir.viewmodel.ProfileViewModel = viewModel() // Inject ProfileViewModel
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Collect Store State
    val storeState by profileViewModel.storeState.collectAsState()
    
    // State for QR Scanning
    var showConfirmationDialog by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var scannedOrder by remember { mutableStateOf<OrderResponse?>(null) }
    
    
    // --- UI/UX TOGGLE STATES (Source Feature) ---
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val isAlwaysOn by viewModel.isAlwaysOn.collectAsState()
    var showAlwaysOnGuide by remember { mutableStateOf(false) }

    // Logic Effects: Force Light Status Bar Icons (for Dark Header)
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // Force Refresh Data on Screen Enter
    LaunchedEffect(Unit) {
        viewModel.fetchOrders()
        profileViewModel.fetchStore()
    }

    var cancellationOrder by remember { mutableStateOf<OrderResponse?>(null) } // Local state for cancellation review
    var forceCancelOrder by remember { mutableStateOf<OrderResponse?>(null) } // Local state for force cancel

    // --- LOGIC: Always On Display (Moved to MainScreen) ---


    // --- LOGIC: New Order Sound Notification (Moved to MainScreen) ---


    DashboardScreenContent(
        orders = orders,
        isLoading = isLoading,
        error = error,
        store = storeState, // Pass store data
        isSoundEnabled = isSoundEnabled,
        isAlwaysOn = isAlwaysOn,
        onToggleSound = { viewModel.toggleSound(!isSoundEnabled) },
        onToggleAlwaysOn = { viewModel.toggleAlwaysOn(!isAlwaysOn) },
        onLongClickAlwaysOn = { showAlwaysOnGuide = true },
        onNavigate = onNavigate,
        onUpdateStatus = { id, status -> viewModel.updateStatus(id, status) },
        onScanClick = { onNavigate("bayar") },
        onReviewCancellation = { order -> cancellationOrder = order },
        onForceCancel = { order -> forceCancelOrder = order }
    )
    
    // Force Cancel Dialog
    if (forceCancelOrder != null) {
        ForceCancelDialog(
            onDismiss = { forceCancelOrder = null },
            onConfirm = { reason ->
                viewModel.rejectCancellation(forceCancelOrder!!.id, reason)
                forceCancelOrder = null
            }
        )
    }
    
    // Cancellation Review Dialog
    if (cancellationOrder != null) {
        CancellationReviewDialog(
            order = cancellationOrder!!,
            onDismiss = { cancellationOrder = null },
            onApprove = {
                viewModel.approveCancellation(cancellationOrder!!.id)
                cancellationOrder = null
            },
            onReject = {
                viewModel.rejectCancellation(cancellationOrder!!.id)
                cancellationOrder = null
            }
        )
    }
    
    // Dialogs
    if (showConfirmationDialog && scannedOrder != null) {
        PaymentConfirmationDialog(
            order = scannedOrder!!,
            onDismiss = { showConfirmationDialog = false },
            onConfirm = {
                 // Trigger status update to Processing (or Paid)
                 viewModel.updateStatus(scannedOrder!!.id, "Processing") 
                
                showConfirmationDialog = false
                showSuccessDialog = true
            }
        )
    }
    
    if (showSuccessDialog && scannedOrder != null) {
        PaymentSuccessDialog(
            totalAmount = scannedOrder!!.totalAmount,
            transactionCode = scannedOrder!!.transactionCode,
            onDismiss = {
                showSuccessDialog = false
                scannedOrder = null
            }
        )
    }
    
    // GUIDE DIALOG (Source Feature)
    if (showAlwaysOnGuide) {
        AlwaysOnGuideDialog(onDismiss = { showAlwaysOnGuide = false })
    }
}

@Composable
fun DashboardScreenContent(
    orders: List<OrderResponse>,
    isLoading: Boolean,
    error: String?,
    store: Store?,
    isSoundEnabled: Boolean,
    isAlwaysOn: Boolean,
    onToggleSound: () -> Unit,
    onToggleAlwaysOn: () -> Unit,
    onLongClickAlwaysOn: () -> Unit,
    onNavigate: (String) -> Unit,
    onUpdateStatus: (Int, String) -> Unit,
    onScanClick: () -> Unit,
    onReviewCancellation: (OrderResponse) -> Unit,
    onForceCancel: (OrderResponse) -> Unit,
    // SEARCH FOCUS PARAMS
    isSearchFocused: Boolean = false,
    onSearchFocusChange: (Boolean) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // all, Pending, Processing, Completed
    var isSearchFocused by remember { mutableStateOf(false) } // Local state for Dashboard Focus

    val filteredOrders = orders.filter { order ->
        if (searchQuery.isNotEmpty()) {
            // SMART SEARCH (Token Based + Comprehensive Scope)
            val queryTokens = searchQuery.lowercase().split(" ").filter { it.isNotBlank() }
            
            // Build Searchable Content
            val menuItems = order.items.joinToString(" ") { it.product.name }
            val orderTypeID = when(order.orderType.lowercase()) {
                "takeaway" -> "bungkus takeaway"
                "delivery" -> "antar delivery"
                else -> "makan ditempat dine-in"
            }
            // Combine all searchable fields
            val content = """
                ${order.customerName} 
                ${order.transactionCode} 
                ${order.table?.name ?: ""} 
                ${order.table?.location?.name ?: ""} 
                ${order.paymentMethod ?: ""} 
                $menuItems 
                $orderTypeID
            """.trimIndent().lowercase()

            // Logic: Order must contain ALL tokens (e.g. "Budi Nasi" -> finds Budi AND Nasi)
            queryTokens.all { token -> content.contains(token) }
        } else {
            // STANDARD FILTER (Existing Logic)
            when (selectedFilter) {
                "all" -> order.status != "Completed" && order.status != "Cancelled"
                "Pending" -> order.status == "Pending"
                "Processing" -> order.status == "Processing"
                "Completed" -> order.status == "Completed" || order.status == "Cancelled"
                else -> true
            }
        }
    }.sortedWith(Comparator { o1, o2 ->
        if (searchQuery.isNotEmpty()) {
             // PRIORITY SEARCH SORT (Pending > Processing > Completed/Cancelled)
             val p1 = when(o1.status) { "Pending" -> 1; "Processing" -> 2; else -> 3 }
             val p2 = when(o2.status) { "Pending" -> 1; "Processing" -> 2; else -> 3 }
             
             if (p1 != p2) {
                 p1 - p2 // Ascending Priority (1, 2, 3)
             } else {
                 if (p1 == 3) {
                     // If Completed/History: Newest First
                     o2.createdAt.compareTo(o1.createdAt)
                 } else {
                     // If Active Queue: Oldest First (FIFO)
                     o1.createdAt.compareTo(o2.createdAt)
                 }
             }
        } else {
            // STANDARD SORT
            if (selectedFilter == "Completed") {
                 // History: Newest First (Descending)
                 o2.createdAt.compareTo(o1.createdAt)
            } else {
                 // Active Queue: Oldest First (Ascending / FIFO)
                 o1.createdAt.compareTo(o2.createdAt)
            }
        }
    })

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Sync Focus State
    LaunchedEffect(isSearchFocused) {
        if (!isSearchFocused) {
            focusManager.clearFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgBody)) {
        Scaffold(
            containerColor = Color.Transparent, 
            topBar = {
                AnimatedVisibility(
                    visible = !isSearchFocused,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    DashboardTopBar(
                        storeName = store?.name ?: "Dapur QuackXel",
                        logoUrl = store?.logo,
                        isSoundEnabled = isSoundEnabled,
                        isAlwaysOn = isAlwaysOn,
                        onToggleSound = onToggleSound,
                        onToggleAlwaysOn = onToggleAlwaysOn,
                        onLongClickAlwaysOn = onLongClickAlwaysOn,
                        onProfileClick = { onNavigate("profile") }
                    )
                }
            },
            bottomBar = { /* Use custom overlay below */ }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .then(if (isSearchFocused) Modifier.statusBarsPadding().padding(top = 16.dp, start = 16.dp, end = 16.dp) else Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    isFocused = isSearchFocused,
                    onBack = { isSearchFocused = false },
                    onFocusTrigger = { isSearchFocused = true },
                    focusRequester = focusRequester
                )

                // Filter Chips (Hide when Focused)
                AnimatedVisibility(visible = !isSearchFocused) {
                    FilterSection(selectedFilter) { selectedFilter = it }
                }

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
                        contentPadding = PaddingValues(bottom = 120.dp) // Space for Floating Bottom Nav
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            KitchenOrderCard(
                                order = order, 
                                onUpdateStatus = onUpdateStatus,
                                onReviewCancellation = onReviewCancellation,
                                onForceCancel = onForceCancel
                            )
                        }
                    }
                }
            }
        }


    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardTopBar(
    storeName: String,
    logoUrl: String?,
    isSoundEnabled: Boolean,
    isAlwaysOn: Boolean,
    onToggleSound: () -> Unit,
    onToggleAlwaysOn: () -> Unit,
    onLongClickAlwaysOn: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBg)
            .statusBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
               painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo),
               contentDescription = null, 
               tint = Color(0xFFFDE047),
               modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = storeName,
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
        
        // Right Side: Toggles + Avatar
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Always On Toggle (Long Press Enabled)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = onToggleAlwaysOn,
                        onLongClick = onLongClickAlwaysOn
                    )
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isAlwaysOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Always On",
                    tint = if (isAlwaysOn) Color(0xFFFACC15) else Color.Gray
                )
            }
            
            // Sound Toggle
            IconButton(onClick = onToggleSound) {
                Icon(
                    imageVector = if (isSoundEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = "Sound",
                    tint = if (isSoundEnabled) Color(0xFFFACC15) else Color.Gray
                )
            }

            Spacer(Modifier.width(8.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE4E6))
                    .border(2.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                 val imageUrl = com.example.kasir.utils.ImageUtils.getDynamicImageUrl(logoUrl)
                 
                if (imageUrl != null) {
                     coil.compose.AsyncImage(
                        model = imageUrl,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextMain)
                }
            }
        }
    }
}

// ... SearchBar, FilterSection, FilterChip (Same as before) ...
@Composable
fun SearchBar(
    query: String, 
    onQueryChange: (String) -> Unit,
    isFocused: Boolean = false,
    onBack: () -> Unit = {},
    onFocusTrigger: () -> Unit = {},
    focusRequester: FocusRequester
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Animate Shape: Pill (50%) -> Rect (12.dp)
    val cornerRadius by animateDpAsState(
        targetValue = if (isFocused) 10.dp else 24.dp, // 24.dp approximates Pill for standard height
        label = "corner"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button (Only visible when focused) - PWA Style
        AnimatedVisibility(
            visible = isFocused,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextMain)
            }
        }
        
        Box(modifier = Modifier.weight(1f)) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { 
                    Text(
                        "Cari pelanggan, menu, meja...", // STATIC PLACEHOLDER
                        style = MaterialTheme.typography.bodyMedium, 
                        color = TextMuted
                    ) 
                },
                // PWA: Hides Search Icon when focused
                leadingIcon = if (isFocused) null else { 
                    { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) } 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerRadius)) // Animated Shape
                    .border(1.dp, if (isFocused) CardHeaderBg else Color.Transparent, RoundedCornerShape(cornerRadius))
                    .focusRequester(focusRequester),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isFocused) Color(0xFFF3F4F6) else Color.White, // PWA uses F3F4F6 (Greyish) when focused? Or always?
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain
                ),
                singleLine = true
            )

            // Clickable Overlay for Robust Focus Trigger
            if (!isFocused) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(cornerRadius))
                        .clickable {
                            onFocusTrigger()
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                )
            }
        }
    }
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
fun KitchenOrderCard(
    order: OrderResponse, 
    onUpdateStatus: (Int, String) -> Unit,
    onReviewCancellation: (OrderResponse) -> Unit,
    onForceCancel: (OrderResponse) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            CardHeader(order)

            Column(modifier = Modifier.padding(16.dp)) {
                OrderTypeBanner(order.orderType)

                Spacer(modifier = Modifier.height(16.dp))

                order.items.forEach { item ->
                    OrderItemRow(item)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!order.note.isNullOrEmpty()) {
                    InfoBox(Icons.Default.Info, "Catatan: ${order.note}", InfoNoteBg, InfoNoteText, InfoNoteBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (order.orderType.equals("delivery", ignoreCase = true)) {
                    val addr = if (!order.deliveryAddress.isNullOrEmpty()) order.deliveryAddress else "-"
                     InfoBox(Icons.Default.LocationOn, "Alamat: $addr", InfoAddressBg, InfoAddressText, InfoAddressBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                ActionButtons(order, onUpdateStatus, onReviewCancellation, onForceCancel)
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
    
    val locName = order.table?.location?.name ?: ""
    val tableName = order.table?.name ?: ""
    
    val topText = buildString {
        if (locName.isNotEmpty()) append("$locName ")
        if (tableName.isNotEmpty()) append(tableName)
        if (isEmpty()) append("-") 
    }

    val bottomText = order.customerName

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardHeaderBg)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top 
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = topText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$bottomText . ",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Normal
                    )
                )
                // REVISION: Use order.paymentMethod (e.g. Qris / Kasir)
                Text(
                    text = order.paymentMethod ?: "-",
                    style = MaterialTheme.typography.bodySmall.copy(
                         fontWeight = FontWeight.ExtraBold,
                         color = PaymentHighlight 
                    )
                )
            }
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

// ... BannerStyle, OrderTypeBanner, OrderItemRow, InfoBox, ActionButtons, EmptyState ...
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
fun ActionButtons(
    order: OrderResponse, 
    onUpdateStatus: (Int, String) -> Unit,
    onReviewCancellation: (OrderResponse) -> Unit,
    onForceCancel: (OrderResponse) -> Unit
) {
    if (order.cancellationStatus == "Requested") {
        Button(
            onClick = { onReviewCancellation(order) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(45.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tinjau Permintaan Batal", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (order.status == "Pending") {
            Button(
                onClick = { onForceCancel(order) },
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

@Composable
fun AlwaysOnGuideDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
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
                    Text(
                        text = "Mode Selalu Nyala",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMain,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "GuideAnim")
                    val handScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "HandScale"
                    )

                    val ringScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ), label = "RingScale"
                    )
                    
                    val ringAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ), label = "RingAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn, 
                            contentDescription = null, 
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer(
                                scaleX = ringScale,
                                scaleY = ringScale,
                                alpha = ringAlpha
                            )
                            .border(2.dp, Color(0xFFFACC15), CircleShape)
                    )

                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = TextMain.copy(alpha = 0.9f),
                        modifier = Modifier
                            .size(36.dp)
                            .offset(x = 12.dp, y = 12.dp)
                            .graphicsLayer(
                                scaleX = handScale,
                                scaleY = handScale
                            )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        append("Tekan tombol ini untuk mengaktifkan\nmode ")
                        withStyle(style = SpanStyle(color = TextMain, fontWeight = FontWeight.Bold)) {
                            append("pasti nyala")
                        }
                        append(" (Layar tidak akan mati).")
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray, // Muted default color
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HeaderBg,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mengerti")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    // Preview logic
}
