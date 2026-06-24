package id.quacxel.mejapesan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock // Added
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import id.quacxel.mejapesan.ui.profile.PrivacyPolicyScreen
import id.quacxel.mejapesan.ui.profile.ShippingZonesDialog
import id.quacxel.mejapesan.viewmodel.ProfileViewModel
import id.quacxel.mejapesan.utils.FileUtils
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import id.quacxel.mejapesan.R
import id.quacxel.mejapesan.utils.LocalAdaptiveValues

// --- COLORS ---
// --- COLORS ---
val Navy = Color(0xFF2C3E50)
val QuackYellow = Color(0xFFF7DC6F)
val QuackYellowDark = Color(0xFFF0C92F)
val BackgroundLight = Color(0xFFF9FAFB)
val SurfaceLight = Color(0xFFFFFFFF)
val Danger = Color(0xFFEF4444)


// --- LOGO STRATEGY START ---
// Sealed class to handle both Real Resources and Generated Initials
sealed class LogoSource {
    data class Resource(val id: Int) : LogoSource()
    data class Generated(val text: String, val color: Color) : LogoSource()
}
// --- LOGO STRATEGY END ---

// Top Level Logo Definitions
val bankLogos = mapOf(
    "BCA" to LogoSource.Resource(R.drawable.bank_bca),
    "BRI" to LogoSource.Resource(R.drawable.bank_bri),
    "Mandiri" to LogoSource.Resource(R.drawable.bank_mandiri),
    "BNI" to LogoSource.Resource(R.drawable.bank_bni),
    "BSI" to LogoSource.Resource(R.drawable.bank_bsi),
    "SeaBank" to LogoSource.Resource(R.drawable.bank_seabank),
    "Jago" to LogoSource.Resource(R.drawable.bank_jago),
    "CIMB Niaga" to LogoSource.Resource(R.drawable.bank_cimb),
    "Danamon" to LogoSource.Resource(R.drawable.bank_danamon),
    "Permata" to LogoSource.Resource(R.drawable.bank_permata),
    "Superbank" to LogoSource.Resource(R.drawable.bank_superbank)
)

val ewalletLogos = mapOf(
    "ShopeePay" to LogoSource.Resource(R.drawable.ewallet_shopeepay),
    "Dana" to LogoSource.Resource(R.drawable.ewallet_dana),
    "OVO" to LogoSource.Resource(R.drawable.ewallet_ovo),
    "Gopay" to LogoSource.Resource(R.drawable.ewallet_gopay),
)

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val balance by viewModel.balance.collectAsState() // Deprecated
    val availableBalance by viewModel.availableBalance.collectAsState()
    val pendingSettlement by viewModel.pendingSettlement.collectAsState()
    val storeState by viewModel.storeState.collectAsState()
    val history by viewModel.withdrawalHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Shipping Zones State
    val shippingZones by viewModel.shippingZones.collectAsState()
    var showShippingDialog by remember { mutableStateOf(false) }
    
    // WA Bot State
    val waStatus by viewModel.waStatus.collectAsState()
    val waQrCode by viewModel.waQrCode.collectAsState()
    val waPairingCode by viewModel.waPairingCode.collectAsState()
    val pairingSuccess by viewModel.pairingSuccess.collectAsState()
    val promotionStats by viewModel.promotionStats.collectAsState()
    val isPromoting by viewModel.isPromoting.collectAsState()
    val promotionMessage by viewModel.promotionMessage.collectAsState()
    val isSocketConnected by viewModel.isSocketConnected.collectAsState()
    val socketDebugMessage by viewModel.socketDebugMessage.collectAsState()
    var showWaBotDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    // Force Refresh Profile Data on Enter
    LaunchedEffect(Unit) {
        viewModel.fetchStore()
    }
    
    // Launchers
    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> 
        if (uri != null) viewModel.uploadLogo(uri, context)
    }

    val qrisLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> 
        if (uri != null) viewModel.uploadQris(uri, context)
    }

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Success Popup Logic
    var showSaveSuccessDialog by remember { mutableStateOf(false) }
    
    // Friendly Popup State
    var infoMessage by remember { mutableStateOf<String?>(null) }
    
    // Convert all errorMessages to Info Dialogs
    LaunchedEffect(errorMessage) {
        if (errorMessage == "Pengaturan pembayaran disimpan") {
            showSaveSuccessDialog = true
            viewModel.clearErrorMessage() // Consume the event immediately
        } else if (errorMessage != null) {
            infoMessage = errorMessage
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(socketDebugMessage) {
        if (socketDebugMessage != null) {
            android.widget.Toast.makeText(context, socketDebugMessage, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSocketDebug()
        }
    }

    if (infoMessage != null) {
        FriendlyInfoDialog(
            message = infoMessage!!,
            onDismiss = { infoMessage = null }
        )
    }

    if (showSaveSuccessDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSaveSuccessDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Success Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.1f)), // Light Green
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Berhasil Disimpan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Data metode pembayaran Anda telah berhasil diperbarui.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = { showSaveSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showWithdrawDialog) {
        WithdrawalDialog(
            balance = availableBalance,
            storeState = storeState,
            bankLogos = bankLogos, // Pass logos
            ewalletLogos = ewalletLogos, // Pass logos
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount, method ->
                val currentBalance = availableBalance
                if (amount > currentBalance) {
                     // errorMessage = "Saldo tidak mencukupi." // Cannot assign to val
                } else {
                     viewModel.requestWithdrawal(amount, method) {
                        showWithdrawDialog = false
                     }
                }
            }
        )
    }

    if (showHistoryDialog) {
        WithdrawalHistoryDialog(
            history = history,
            onDismiss = { showHistoryDialog = false }
        )
    }
    
    // Dialog Shipping Zones
    if (showShippingDialog) {
        ShippingZonesDialog(
            shippingZones = shippingZones,
            onDismiss = { showShippingDialog = false },
            onAdd = { name, fee -> viewModel.addShippingZone(name, fee) },
            onUpdate = { id, name, fee, isActive -> viewModel.updateShippingZone(id, name, fee, isActive, isSilentUpdate = true) },
            onDelete = { id -> viewModel.deleteShippingZone(id) }
        )
    }

    Scaffold(
        topBar = {
            ProfileTopBar(onBack = { onNavigate("dashboard") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val storeName = storeState?.name ?: "Restoran"
                    val message = "Halo Tim CS QuackXel, saya dari resto $storeName. Saya butuh bantuan terkait aplikasi kasir."
                    val url = "whatsapp://send?phone=6285113267327&text=${android.net.Uri.encode(message)}"
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                    intent.data = android.net.Uri.parse(url)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "WhatsApp tidak terinstall", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = Color(0xFF25D366),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_whatsapp),
                    contentDescription = "CS WhatsApp",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        val adaptive = LocalAdaptiveValues.current
        if (isLoading && storeState == null) {
             SettingSkeletonLoading(paddingValues)
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
             Column(
                modifier = Modifier
                    .then(
                        if (adaptive.isTablet) Modifier.widthIn(max = adaptive.contentMaxWidth)
                        else Modifier.fillMaxWidth()
                    )
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Section 0: Balance Card
                // Section 1: Restaurant Identity (Moved to Top)
                RestaurantIdentitySection(
                    name = storeState?.name ?: "Nama Resto",
                    logoUrl = storeState?.logo,
                    email = id.quacxel.mejapesan.utils.SessionManager.currentUser?.email,
                    onNameChange = { /* handled in button or on value change */ }, 
                    onLogoClick = { logoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    viewModel = viewModel,
                    onOpenShipping = { showShippingDialog = true }
                )

                // Section 2: Balance Card
                BalanceCard(
                    availableBalance = availableBalance,
                    pendingSettlement = pendingSettlement,
                    onWithdrawClick = { showWithdrawDialog = true },
                    onHistoryClick = { showHistoryDialog = true }
                )

                // Section 3: Withdrawal Settings
                WithdrawalSettingsSection(
                    storeState = storeState,
                    onSave = { bankName, bankNumber, bankHolder, ewalletType, ewalletNumber, ewalletName -> 
                        // Restore logic: This is the SAVE action
                        viewModel.updatePaymentSettings(bankName, bankNumber, bankHolder, ewalletType, ewalletNumber, ewalletName, isDelete = false)
                    },
                    viewModel = viewModel // Pass VM to handle logic internally
                )

                // Section 4: WhatsApp Bot (Gatekeeper)
                WhatsAppBotSection(
                    status = waStatus,
                    storeState = storeState,
                    promotionStats = promotionStats,
                    isPromoting = isPromoting,
                    promotionMessage = promotionMessage,
                    onToggleAutoReply = { viewModel.toggleAutoReply(it) },
                    onToggleAi = { viewModel.toggleAi(it) },
                    onStartPromotion = { viewModel.startPromotion(it) },
                    onClearPromoMessage = { viewModel.clearPromotionMessage() },
                    onDisconnect = { viewModel.disconnectWhatsApp() },
                    onClick = { 
                        showWaBotDialog = true
                        // viewModel.initWhatsApp() // REMOVED: User must choose type first
                    }
                )

                // Section 5: Footer Actions
                Spacer(modifier = Modifier.weight(1f)) // Push to bottom if content is short
                FooterActions(onNavigate)
            }
            } // Close the adaptive Box
        }
    }

    if (showWaBotDialog) {
        val pairingSuccess by viewModel.pairingSuccess.collectAsState()
        val promotionStats by viewModel.promotionStats.collectAsState()
        val isPromoting by viewModel.isPromoting.collectAsState()
        val promotionMessage by viewModel.promotionMessage.collectAsState()

        WhatsAppBotDialog(
            status = waStatus,
            qrCode = waQrCode,
            pairingCode = waPairingCode,
            pairingSuccess = pairingSuccess,
            isSocketConnected = isSocketConnected, // NEW
            storeState = storeState,
            promotionStats = promotionStats,
            isPromoting = isPromoting,
            promotionMessage = promotionMessage,
            onDismiss = { 
                showWaBotDialog = false 
                viewModel.fetchWhatsAppStatus()
                viewModel.clearPairingSuccess() // Clean up state
            },
            onDisconnect = { viewModel.disconnectWhatsApp() },
            onConnect = { waType -> viewModel.initWhatsApp(waType) }, // NEW
            onPing = { viewModel.pingServer() },
            onToggleAutoReply = { viewModel.toggleAutoReply(it) },
            onToggleAi = { viewModel.toggleAi(it) },
            onStartPromotion = { viewModel.startPromotion(it) },
            onClearPromoMessage = { viewModel.clearPromotionMessage() }
        )
    }
}

@Composable
fun WhatsAppBotSection(
    status: String,
    storeState: id.quacxel.mejapesan.data.model.Store?,
    promotionStats: id.quacxel.mejapesan.data.model.PromotionStats?,
    isPromoting: Boolean,
    promotionMessage: String?,
    onToggleAutoReply: (Boolean) -> Unit,
    onToggleAi: (Boolean) -> Unit,
    onStartPromotion: (String) -> Unit,
    onClearPromoMessage: () -> Unit,
    onDisconnect: () -> Unit,
    onClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isConnected = status == "connected"
    val context = LocalContext.current
    val whatsappNumber = storeState?.whatsappNumber ?: ""
    val isNumberEmpty = whatsappNumber.isEmpty()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = if (isConnected) listOf(Color(0xFF25D366).copy(alpha = 0.5f), Color(0xFF10B981).copy(alpha = 0.5f))
                             else if (isNumberEmpty) listOf(Color(0xFFFEE2E2), Color(0xFFFEE2E2))
                             else listOf(Color(0xFFF3F4F6), Color(0xFFF3F4F6))
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (isConnected) {
                            isExpanded = !isExpanded 
                        } else {
                            if (isNumberEmpty) {
                                android.widget.Toast.makeText(context, "Isi nomor WA di profil dulu bro!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                onClick()
                            }
                        }
                    }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) Color(0xFF25D366).copy(alpha = 0.1f)
                            else Color.Gray.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = if (isConnected) Color(0xFF25D366) else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Smart Auto-Order Bot",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = Navy)
                    )
                    Text(
                        if (isConnected) "Sistem AI Aktif & Berjalan" else "Hubungkan WhatsApp lu bro",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }

                if (isConnected) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                } else {
                    // Status Badge with Pulse Effect
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF3F4F6),
                    ) {
                        Text(
                            text = "OFFLINE",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.Gray
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isConnected && isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFF3F4F6))
                    
                    // Settings Section
                    Text(
                        "Pengaturan Bot",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Navy.copy(alpha=0.6f))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ServiceMethodToggle(
                            title = "Auto-Reply Pesan",
                            description = "Bales otomatis pake link pesenan.",
                            icon = Icons.Default.Campaign,
                            isActive = storeState?.isAutoReplyEnabled ?: false,
                            onCheckedChange = onToggleAutoReply
                        )

                        val isAutoReplyOn = storeState?.isAutoReplyEnabled ?: false
                        val isAiCurrentlyOn = storeState?.isAiEnabled ?: false
                        
                        Box(modifier = Modifier.alpha(if (isAiCurrentlyOn) 0.8f else 0.6f)) {
                            ServiceMethodToggle(
                                title = if (!isAiCurrentlyOn) "Integrasi AI (Segera Hadir)" else "Integrasi AI (Dalam Pengembangan)",
                                description = if (!isAiCurrentlyOn) "Fitur ini masih dalam tahap pengembangan." else "Matikan jika belum ingin menggunakan AI.",
                                icon = Icons.Default.SupportAgent,
                                isActive = isAiCurrentlyOn,
                                onCheckedChange = { newVal ->
                                    if (!newVal) {
                                        onToggleAi(false)
                                    }
                                }
                            )
                        }
                    }

                    // Customer Booster Section
                    Text(
                        "Customer Booster 🚀",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Navy.copy(alpha=0.6f))
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PromotionCard(
                            title = "Sapa Pelanggan Setia",
                            subtitle = "${promotionStats?.loyalCount ?: 0} orang sering beli",
                            icon = Icons.Default.Favorite,
                            color = Color(0xFFEC4899),
                            isLoading = isPromoting,
                            onClick = { onStartPromotion("LOYAL") }
                        )

                        PromotionCard(
                            title = "Panggil Pelanggan Lama",
                            subtitle = "${promotionStats?.churningCount ?: 0} orang sudah lama nggak mampir",
                            icon = Icons.Default.Notifications,
                            color = Color(0xFFF59E0B),
                            isLoading = isPromoting,
                            onClick = { onStartPromotion("CHURNING") }
                        )
                    }

                    if (promotionMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Navy, modifier = Modifier.size(14.dp))
                                Text(
                                    promotionMessage!!,
                                    style = MaterialTheme.typography.labelMedium.copy(color = Navy),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onClearPromoMessage, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Putuskan Koneksi", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppBotDialog(
    status: String,
    qrCode: String?,
    pairingCode: String?,
    pairingSuccess: Boolean,
    isSocketConnected: Boolean, // NEW
    storeState: id.quacxel.mejapesan.data.model.Store?,
    promotionStats: id.quacxel.mejapesan.data.model.PromotionStats?,
    isPromoting: Boolean,
    promotionMessage: String?,
    onDismiss: () -> Unit,
    onDisconnect: () -> Unit,
    onConnect: (String) -> Unit,
    onPing: () -> Unit, // NEW
    onToggleAutoReply: (Boolean) -> Unit,
    onToggleAi: (Boolean) -> Unit,
    onStartPromotion: (String) -> Unit,
    onClearPromoMessage: () -> Unit
) {
    val context = LocalContext.current
    val isConnected = status == "connected"
    var selectedWaType by remember { mutableStateOf("standard") }
    var isInitRequested by remember { mutableStateOf(false) }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (pairingSuccess) "Selesai!" else "Koneksi WhatsApp Bot",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy)
                        )
                        
                        // Socket Status Indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSocketConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                if (!pairingSuccess && !isConnected) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                            Text(
                                "Bot ini bakal nyambung ke nomor WA Profil lu: *${storeState?.whatsappNumber}*",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1E40AF))
                            )
                        }
                    }
                }

                if (pairingSuccess) {
                    Column(
                        modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color(0xFF10B981), modifier = Modifier.size(64.dp))
                        }
                        Text("Pairing Berhasil!", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Navy))
                        Text("Bot lu sekarang udah aktif bro.", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
                    }
                } else {
                    // Pairing State (NEW: Pairing Code Display)
                    if (pairingCode != null) {
                        var isCopied by remember { mutableStateOf(false) }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Navy.copy(alpha = 0.05f))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Pairing Code", pairingCode)
                                        clipboard.setPrimaryClip(clip)
                                        isCopied = true
                                        // Reset "Copied" state after 2 seconds
                                    }
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Kode Pairing Anda", style = MaterialTheme.typography.labelLarge.copy(color = Color.Gray, fontWeight = FontWeight.Bold))
                                    
                                    val formattedCode = if (pairingCode.length == 8) {
                                        "${pairingCode.substring(0, 4)} - ${pairingCode.substring(4)}"
                                    } else {
                                        pairingCode
                                    }

                                    Text(
                                        text = formattedCode,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Navy,
                                            letterSpacing = 2.sp,
                                            fontSize = 28.sp // Explicit size for guaranteed one-line
                                        ),
                                        maxLines = 1
                                    )
                                    
                                    LaunchedEffect(isCopied) {
                                        if (isCopied) {
                                            kotlinx.coroutines.delay(2000)
                                            isCopied = false
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isCopied) {
                                            Icon(Icons.Default.Check, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                            Text("Berhasil Tersalin!", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF10B981), fontWeight = FontWeight.Bold))
                                        } else {
                                            Text("Klik kotak untuk menyalin", style = MaterialTheme.typography.labelSmall.copy(color = Navy.copy(alpha=0.4f)))
                                        }
                                    }
                                }
                            }
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BackgroundLight).padding(16.dp)
                            ) {
                                Text("Langkah-langkah:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PairingStepItem("1", "Buka WhatsApp di HP utama lu")
                                    PairingStepItem("2", "Klik Perangkat Tertaut > Tautkan Perangkat")
                                    PairingStepItem("3", "Pilih 'Tautkan dengan nomor telepon saja'")
                                    PairingStepItem("4", "Masukkan kode di atas")
                                }
                            }
                        }
                    } else if (qrCode != null) {
                        Box(modifier = Modifier.size(240.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp))) {
                            id.quacxel.mejapesan.utils.QRCodeImage(content = qrCode, modifier = Modifier.fillMaxSize().padding(24.dp))
                        }
                    } else if (!isInitRequested) {
                        // Choice Screen
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Pilih Tipe WhatsApp Lu Bro",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy)
                            )
                            
                            // Selection Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Standard Card
                                Card(
                                    modifier = Modifier.weight(1f).clickable { selectedWaType = "standard" },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedWaType == "standard") Navy.copy(alpha=0.05f) else Color.White
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (selectedWaType == "standard") 2.dp else 1.dp,
                                        color = if (selectedWaType == "standard") Navy else Color(0xFFE5E7EB)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = id.quacxel.mejapesan.R.drawable.ic_whatsapp),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text("WA Biasa", fontWeight = FontWeight.Bold, color = Navy, fontSize = 12.sp)
                                    }
                                }

                                // Business Card
                                Card(
                                    modifier = Modifier.weight(1f).clickable { selectedWaType = "business" },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedWaType == "business") Color(0xFF25D366).copy(alpha=0.05f) else Color.White
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (selectedWaType == "business") 2.dp else 1.dp,
                                        color = if (selectedWaType == "business") Color(0xFF25D366) else Color(0xFFE5E7EB)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = id.quacxel.mejapesan.R.drawable.ic_whatsapp),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text("WA Bisnis", fontWeight = FontWeight.Bold, color = Navy, fontSize = 12.sp)
                                    }
                                }
                            }

                            Button(
                                onClick = { 
                                    isInitRequested = true
                                    onConnect(selectedWaType) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White)
                            ) {
                                Text("Minta Kode Pairing", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 40.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Navy)
                            Text("Menyiapkan Kode...", style = MaterialTheme.typography.bodyMedium.copy(color = Navy))
                        }
                        
                        // TAHAP 40: Fallback / Debug Action
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onPing,
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Navy.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cek Koneksi Socket", color = Navy)
                        }
                    }
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup", color = Navy)
                }
            }
        }
    }
}

@Composable
fun PairingStepItem(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Navy),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
        Text(text, style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
    }
}

@Composable
fun BalanceCard(
    availableBalance: Int,
    pendingSettlement: Int,
    onWithdrawClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "Saldo Cair (Siap Tarik)",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha=0.7f))
                )
                Text(
                    text = "Rp ${java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(availableBalance)}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                
                if (pendingSettlement > 0) {
                     Spacer(modifier = Modifier.height(8.dp))
                     Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(
                              Icons.Default.Info, 
                              contentDescription = "Info", 
                              tint = QuackYellow, 
                              modifier = Modifier.size(16.dp).clickable { showInfoDialog = true }
                          )
                          Spacer(modifier = Modifier.width(4.dp))
                          Text(
                             text = "Tertahan (24 Jam): Rp ${java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(pendingSettlement)}",
                             style = MaterialTheme.typography.bodySmall.copy(color = QuackYellow)
                         )
                     }
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onWithdrawClick,
                    colors = ButtonDefaults.buttonColors(containerColor = QuackYellow, contentColor = Navy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tarik Dana", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = onHistoryClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha=0.5f))
                ) {
                    Text("Riwayat")
                }
            }
        }
    }



    if (showInfoDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showInfoDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icon Header
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(QuackYellow.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = null, 
                            tint = QuackYellowDark, 
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Text Content
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tentang Saldo Tertahan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Dana dari pembayaran QRIS akan masuk ke 'Saldo Tertahan' selama 24 jam untuk proses settlement otomatis oleh sistem.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Button
                    Button(
                        onClick = { showInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mengerti, Terima Kasih", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalSettingsSection(
    storeState: id.quacxel.mejapesan.data.model.Store?,
    onSave: (String?, String?, String?, String?, String?, String?) -> Unit,
    viewModel: id.quacxel.mejapesan.viewmodel.ProfileViewModel
) {
    var selectedTab by remember { mutableStateOf("Bank") } // "Bank" or "ShopeePay"
    
    // UI State for Dialogs
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf("") } // "Bank" or "E-Wallet"

    // Bank State - Always start fresh/empty
    var bankName by remember { mutableStateOf("") }
    var bankNumber by remember { mutableStateOf("") }
    var bankHolder by remember { mutableStateOf("") }

    // E-Wallet State - Always start fresh/empty
    var ewalletType by remember { mutableStateOf("") } // Default empty
    var ewalletNumber by remember { mutableStateOf("") }
    var ewalletName by remember { mutableStateOf("") }

    // Check existing accounts from Store State
    val hasBank = !storeState?.bankName.isNullOrEmpty() && !storeState?.bankNumber.isNullOrEmpty()
    val hasEwallet = !storeState?.ewalletNumber.isNullOrEmpty()
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            
            // 1. Saved Methods Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Rekening Tersimpan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy))
                
                if (!hasBank && !hasEwallet) {
                    Text(
                        "Belum ada metode pencairan yang disimpan.", 
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                } else {
                    // Saved Bank Card
                    if (hasBank) {
                        SavedMethodItem(
                            title = "${storeState?.bankName} - ${storeState?.bankNumber}",
                            subtitle = storeState?.bankHolder ?: "",
                                icon = Icons.Default.List,
                                logo = bankLogos[storeState?.bankName],
                                onDelete = { 
                                    itemToDelete = "Bank"
                                    showDeleteDialog = true
                                }
                            )
                        }
                    
                    // Saved ShopeePay Card
                    if (hasEwallet) {
                        SavedMethodItem(
                            title = "${storeState?.ewalletType ?: "ShopeePay"} - ${storeState?.ewalletNumber}",
                            subtitle = storeState?.ewalletName ?: "",
                            icon = Icons.Default.Share, 
                            logo = ewalletLogos[storeState?.ewalletType ?: "ShopeePay"],
                            onDelete = { 
                                itemToDelete = "E-Wallet"
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
            
            // Delete Confirmation Dialog
            if (showDeleteDialog) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { showDeleteDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Danger Icon
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Danger.copy(alpha = 0.1f)), 
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Danger,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Hapus Rekening?",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Apakah Anda yakin ingin menghapus data ini? Anda perlu memasukkan ulang jika ingin menggunakannya lagi.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { showDeleteDialog = false },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                                ) {
                                    Text("Batal")
                                }

                                Button(
                                    onClick = {
                                        // DELETE ACTION
                                        if (itemToDelete == "Bank") {
                                            viewModel.updatePaymentSettings("", "", "", storeState?.ewalletType, storeState?.ewalletNumber, storeState?.ewalletName, isDelete = true)
                                        } else {
                                            viewModel.updatePaymentSettings(storeState?.bankName, storeState?.bankNumber, storeState?.bankHolder, "", "", "", isDelete = true)
                                        }
                                        showDeleteDialog = false
                                        // NOTE: isDelete=true prevents key "Pengaturan pembayaran disimpan" from being set, skipping popup.
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Hapus", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            
            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

            // 2. Add New Method Section
            // Always show Heading and Tabs
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Tambah Metode Baru", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy))

                // Method Selection Tabs
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = selectedTab == "Bank",
                        onClick = { selectedTab = "Bank" },
                        label = { Text("Bank Transfer") },
                        leadingIcon = { if (selectedTab == "Bank") Icon(Icons.Default.Check, null) else null },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Navy,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedTab == "ShopeePay",
                        onClick = { selectedTab = "ShopeePay" },
                        label = { Text("E-Wallet") },
                        leadingIcon = { if (selectedTab == "ShopeePay") Icon(Icons.Default.Check, null) else null },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Navy,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }

                if (selectedTab == "Bank") {
                    if (hasBank) {
                        // Show info message
                        Text(
                            "Data Bank sudah tersimpan. Hapus data di atas untuk mengubah.", 
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    } else {
                            // Bank Inputs (Added Superbank)
                            val banks = listOf("BCA", "BRI", "Mandiri", "BNI", "BSI", "SeaBank", "Jago", "CIMB Niaga", "Danamon", "Permata", "Superbank")
                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = bankName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Pilih Bank") },
                                    placeholder = { Text("Pilih Bank Tujuan") },
                                    leadingIcon = {
                                        if (bankName.isNotEmpty()) {
                                            LogoDisplay(
                                                source = bankLogos[bankName],
                                                modifier = Modifier.size(40.dp).padding(start=8.dp) // Increased size
                                            )
                                        }
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Navy,
                                        unfocusedTextColor = Navy,
                                        focusedBorderColor = Navy,
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedLabelColor = Navy,
                                        unfocusedLabelColor = Color.Gray
                                    ),
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    banks.forEach { bank ->
                                        DropdownMenuItem(
                                            text = { 
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    LogoDisplay(
                                                       source = bankLogos[bank],
                                                       modifier = Modifier.size(30.dp)
                                                    )
                                                    Text(bank, color = Navy) 
                                                }
                                            },
                                            onClick = {
                                                bankName = bank
                                                expanded = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = bankNumber,
                                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 20) bankNumber = it },
                                label = { Text("Nomor Rekening") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Navy,
                                    unfocusedTextColor = Navy,
                                    focusedBorderColor = Navy,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = Navy,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )

                            OutlinedTextField(
                                value = bankHolder,
                                onValueChange = { if (it.length <= 50 && it.matches(Regex("^[a-zA-Z0-9\\s.,'-]*$"))) bankHolder = it },
                                label = { Text("Atas Nama") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Navy,
                                    unfocusedTextColor = Navy,
                                    focusedBorderColor = Navy,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = Navy,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                        }
                    } else {
                        if (hasEwallet) {
                            Text(
                                "Data E-Wallet sudah tersimpan. Hapus data di atas untuk mengubah.", 
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            )
                        } else {
                            // E-Wallet Inputs
                            val ewallets = listOf("ShopeePay", "Dana", "OVO", "Gopay")
                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = ewalletType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Pilih E-Wallet") },
                                    placeholder = { Text("Pilih Jenis E-Wallet") },
                                    leadingIcon = {
                                        if (ewalletType.isNotEmpty()) {
                                            LogoDisplay(
                                                source = ewalletLogos[ewalletType],
                                                modifier = Modifier.size(40.dp).padding(start=8.dp) // Increased size
                                            )
                                        }
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Navy,
                                        unfocusedTextColor = Navy,
                                        focusedBorderColor = Navy,
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedLabelColor = Navy,
                                        unfocusedLabelColor = Color.Gray
                                    ),
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    ewallets.forEach { wallet ->
                                        DropdownMenuItem(
                                            text = { 
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    LogoDisplay(
                                                       source = ewalletLogos[wallet],
                                                       modifier = Modifier.size(30.dp)
                                                    )
                                                    Text(wallet, color = Navy) 
                                                }
                                            },
                                            onClick = {
                                                ewalletType = wallet
                                                expanded = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = ewalletNumber,
                                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 20) ewalletNumber = it },
                                label = { Text("Nomor HP (E-Wallet)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Navy,
                                    unfocusedTextColor = Navy,
                                    focusedBorderColor = Navy,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = Navy,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )

                            OutlinedTextField(
                                value = ewalletName,
                                onValueChange = { if (it.length <= 50 && it.matches(Regex("^[a-zA-Z0-9\\s.,'-]*$"))) ewalletName = it },
                                label = { Text("Atas Nama Akun") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Navy,
                                    unfocusedTextColor = Navy,
                                    focusedBorderColor = Navy,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = Navy,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                        }
                    }
                    
                    Button(
                        onClick = { 
                            if (selectedTab == "Bank") {
                                // Saving Bank -> Keep existing E-Wallet data
                                onSave(
                                    bankName, bankNumber, bankHolder, 
                                    storeState?.ewalletType ?: "ShopeePay", 
                                    storeState?.ewalletNumber ?: "", 
                                    storeState?.ewalletName ?: ""
                                )
                            } else {
                                // Saving E-Wallet -> Keep existing Bank data
                                onSave(
                                    storeState?.bankName ?: "", 
                                    storeState?.bankNumber ?: "", 
                                    storeState?.bankHolder ?: "", 
                                    ewalletType, 
                                    ewalletNumber, 
                                    ewalletName
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy, 
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.LightGray
                        ),
                        enabled = if (selectedTab == "Bank") {
                            !hasBank && bankName.isNotBlank() && bankNumber.isNotBlank() && bankHolder.isNotBlank()
                        } else {
                            !hasEwallet && ewalletType.isNotBlank() && ewalletNumber.isNotBlank() && ewalletName.isNotBlank()
                        }
                    ) {
                        Text("Simpan Metode", fontWeight = FontWeight.Bold)
                    }


                    // Disclaimer Text
                    Text(
                        text = "Pastikan nomor rekening atau e-wallet yang Anda masukkan sudah benar. Kesalahan input dapat menyebabkan kegagalan pencairan dana.", 
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray, 
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
    }
}
}
}

@Composable
fun SavedMethodItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    logo: LogoSource? = null,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Logo or Icon
        if (logo != null) {
            LogoDisplay(
                source = logo,
                modifier = Modifier.size(40.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Navy.copy(alpha=0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Navy)
            }
        }
        
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = Navy)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp).border(1.dp, Danger.copy(alpha=0.2f), CircleShape)
        ) {
            Icon(Icons.Default.Delete, null, tint = Danger, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun PaymentMethodTile(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    subtitle: String,
    logo: LogoSource?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (selected) 2.dp else 1.dp, 
                color = if (selected) QuackYellow else Color.Transparent, 
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color.White else Color.White.copy(alpha=0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            if (logo != null) {
                LogoDisplay(source = logo, modifier = Modifier.size(40.dp))
            } else {
                 Box(modifier = Modifier.size(40.dp).background(Color.LightGray, CircleShape)) 
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Navy)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            if (selected) {
                 Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Navy, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check, 
                        contentDescription = "Selected", 
                        tint = QuackYellow, 
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                 Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, Color.LightGray, CircleShape)
                )
            }
        }
    }
}

@Composable
fun WithdrawalDialog(
    balance: Int,
    storeState: id.quacxel.mejapesan.data.model.Store?,
    bankLogos: Map<String, LogoSource>,
    ewalletLogos: Map<String, LogoSource>,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("Bank Transfer") }
    
    val bankAvailable = !storeState?.bankName.isNullOrEmpty() && !storeState?.bankNumber.isNullOrEmpty()
    val shopeeAvailable = !storeState?.ewalletNumber.isNullOrEmpty()
    
    // Determine logos
    val bankLogo = if (bankAvailable) bankLogos[storeState?.bankName] else null
    val walletLogo = if (shopeeAvailable) ewalletLogos[storeState?.ewalletType] else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null, // Custom title inside content
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header
                Text(
                    text = "Tarik Dana",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )

                // Balance Info
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White), // White background for contrast against Navy dialog
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                         modifier = Modifier.padding(16.dp).fillMaxWidth(),
                         horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Saldo Cair", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            text = "Rp ${java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(balance)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF10B981)) // Green for money
                        )
                    }
                }
                
                // Destination Selection
                Text("Tujuan Pencairan:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha=0.7f), modifier = Modifier.padding(bottom=4.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bankAvailable) {
                         PaymentMethodTile(
                             selected = selectedMethod == "Bank Transfer",
                             onClick = { selectedMethod = "Bank Transfer" },
                             title = "Bank Transfer",
                             subtitle = "${storeState?.bankName} - ${storeState?.bankNumber}",
                             logo = bankLogo
                         )
                    }

                    if (shopeeAvailable) {
                         PaymentMethodTile(
                             selected = selectedMethod == "ShopeePay",
                             onClick = { selectedMethod = "ShopeePay" },
                             title = storeState?.ewalletType ?: "ShopeePay",
                             subtitle = "${storeState?.ewalletNumber}",
                             logo = walletLogo
                         )
                    }
                    
                    if (!bankAvailable && !shopeeAvailable) {
                        Text("Belum ada metode pembayaran yang diatur.", color = Danger, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Limit Information
                Text(
                    text = "Minimal penarikan Rp 50.000, maksimal Rp 3.000.000",
                    style = MaterialTheme.typography.bodySmall,
                    color = QuackYellow,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 9) amountText = it },
                    label = { Text("Jumlah (Rp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Navy,
                        unfocusedContainerColor = Navy,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = QuackYellow,
                        focusedIndicatorColor = QuackYellow,
                        unfocusedIndicatorColor = Color.White.copy(alpha=0.5f),
                        focusedLabelColor = QuackYellow,
                        unfocusedLabelColor = Color.White.copy(alpha=0.7f)
                    )
                )
                
                // Quick Actions
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toIntOrNull() ?: 0
                    if (amt > 0 && amt <= balance) {
                        onConfirm(amt, selectedMethod)
                    }
                },
                enabled = ((selectedMethod == "Bank Transfer" && bankAvailable) || (selectedMethod == "ShopeePay" && shopeeAvailable)) && 
                          ((amountText.toIntOrNull() ?: 0) in 50000..3000000) && 
                          ((amountText.toIntOrNull() ?: 0) <= balance),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuackYellow, // High contrast Yellow
                    contentColor = Navy, // Navy text
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.LightGray
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Konfirmasi Penarikan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss, 
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha=0.5f))
            ) { 
                Text("Batal", fontWeight = FontWeight.Bold) 
            }
        },
        containerColor = Navy, // Dark Background
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun SettingSkeletonLoading(paddingValues: PaddingValues) {
    val brush = settingShimmerBrush()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp)
    ) {
        // Shimmer: Logo Identity
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
            
            // Name Input Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
            )
            
            // WhatsApp Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
            )
        }

        // Shimmer: Balance Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(brush)
        )

        // Shimmer: Payment Methods Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(brush)
        )
    }
}

@Composable
fun settingShimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): androidx.compose.ui.graphics.Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )

        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(800),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ), label = ""
        )
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset.Zero
        )
    }
}

@Composable
fun WithdrawalHistoryDialog(
    history: List<id.quacxel.mejapesan.data.model.Withdrawal>,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Navy),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "Riwayat Penarikan",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                         Text("Belum ada riwayat penarikan.", color = Color.White.copy(alpha=0.7f))
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(history.size) { index ->
                            WithdrawalHistoryItem(history[index])
                        }
                    }
                }

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha=0.5f))
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WithdrawalHistoryItem(item: id.quacxel.mejapesan.data.model.Withdrawal) {
    val (statusText, statusColor, statusIcon) = when (item.status) {
        "Approved" -> Triple("Sukses", Color(0xFF10B981), Icons.Default.Check) // Green
        "Rejected" -> Triple("Ditolak", Danger, Icons.Default.Close) // Red
        else -> Triple("Pending", Color(0xFFF59E0B), Icons.Default.Info) // Yellow/Orange
    }

    val amountFormatted = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(item.amount)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            
            // Left: Icon & Info
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Status Icon Box
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(statusColor.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, statusColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "Penarikan Dana", 
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                        color = Navy
                    )
                    Text(
                        text = "${item.method} • ${item.createdAt.take(10)}", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.Gray
                    )
                }
            }
            
            // Right: Amount & Status Badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-Rp $amountFormatted", 
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                    color = Navy
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy)
            .statusBarsPadding() // Fix overlap with system status bar
            .height(60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        Text(
            text = "Pengaturan Resto",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            ),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        
        // Spacer to balance the back button
        Spacer(modifier = Modifier.size(40.dp)) 
    }
}

@Composable
fun RestaurantIdentitySection(
    name: String, 
    logoUrl: String?, 
    email: String? = null,
    onNameChange: (String) -> Unit,
    onLogoClick: () -> Unit,
    viewModel: ProfileViewModel,
    onOpenShipping: () -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadCustomSound(context)
    }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateCustomSound(it, context) }
    }

    if (showProfileDialog) {
        ProfilePictureDialog(
            logoUrl = logoUrl,
            onDismiss = { showProfileDialog = false },
            onChangePhoto = { 
                showProfileDialog = false
                onLogoClick() 
            }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Logo Upload
        Box(
            modifier = Modifier.clickable { showProfileDialog = true }
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .background(Color.Gray) // Placeholder bg
            ) {
                // Use ImageUtils for dynamic IP
                val imageUrl = id.quacxel.mejapesan.utils.ImageUtils.getDynamicImageUrl(logoUrl)

                if (imageUrl != null) {
                    coil.compose.AsyncImage(
                        model = imageUrl,
                        contentDescription = "Restaurant Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = androidx.compose.ui.res.painterResource(id = R.drawable.profile_alt)
                    )
                } else {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.profile_alt),
                        contentDescription = "Restaurant Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Camera Icon Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Navy, CircleShape)
                    .border(3.dp, BackgroundLight, CircleShape)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Logo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Email Badge
        if (!email.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Navy.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Navy.copy(alpha = 0.15f)),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.sym_action_email),
                        contentDescription = "Email",
                        tint = Navy.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = email,
                        fontSize = 13.sp,
                        color = Navy.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Name Input
        RestaurantNameEditor(
            initialName = name,
            onSave = { newName -> viewModel.updateName(newName) },
            viewModel = viewModel
        )

        // WhatsApp Number Input
        WhatsAppNumberEditor(
            initialNumber = viewModel.storeState.collectAsState().value?.whatsappNumber ?: "",
            onSave = { newNumber -> viewModel.updateWhatsApp(newNumber) }
        )

        // --- CASH PAYMENT SECTION (ANIMATED) ---
        val isCashActive = viewModel.storeState.collectAsState().value?.isCashActive ?: true
        val cashMode = viewModel.storeState.collectAsState().value?.cashPaymentMode ?: "post"
        val isKasirQrEnabled = viewModel.storeState.collectAsState().value?.isKasirQrVerificationEnabled ?: false

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isCashActive) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFE5E7EB)
            )
        ) {
            Column {
                // Header: Toggle Row
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isCashActive) Color(0xFF10B981).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (isCashActive) Color(0xFF10B981) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Terima Pembayaran Tunai",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Navy)
                            )
                            Text(
                                text = if (isCashActive) "Pelanggan bisa bayar Cash" else "Hanya melayani QRIS",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    androidx.compose.material3.Switch(
                        checked = isCashActive,
                        onCheckedChange = { viewModel.updateCashPaymentActive(it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.LightGray
                        )
                    )
                }

                // Dropdown Content (Animated)
                AnimatedVisibility(
                    visible = isCashActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF3F4F6))
                        
                        Text(
                            text = "Mode Pembayaran Tunai",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Navy.copy(alpha=0.6f))
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CashModeTile(
                                selected = cashMode == "post",
                                title = "Bayar Nanti (Post-Order)",
                                subtitle = "Pesanan masuk dulu, bayar belakangan",
                                onClick = { viewModel.updateCashPaymentMode("post") }
                            )
                            CashModeTile(
                                selected = cashMode == "pre",
                                title = "Bayar Di Awal (Pre-Order)",
                                subtitle = "Bayar dulu baru pesanan diproses",
                                onClick = { viewModel.updateCashPaymentMode("pre") }
                            )
                        }

                        if (cashMode == "post") {
                            HorizontalDivider(thickness = 1.dp, color = Color(0xFFF3F4F6))
                            
                            ServiceMethodToggle(
                                title = "Verifikasi QR Kasir",
                                description = "Wajibkan scan QR kasir sebelum bayar cash",
                                icon = Icons.Default.Check,
                                isActive = isKasirQrEnabled,
                                onCheckedChange = { viewModel.updateKasirQrVerification(it) }
                            )
                        }
                    }
                }
            }
        }

        // --- NEW: SERVICE METHODS TOGGLES (Dine-in, Takeaway, Delivery) ---
        var isServicesExpanded by remember { mutableStateOf(true) }
        
        ExpandableSettingCard(
            title = "Layanan Restoran",
            subtitle = "Atur metode pemesanan yang tersedia",
            icon = Icons.Default.Restaurant,
            isExpanded = isServicesExpanded,
            onExpandClick = { isServicesExpanded = !isServicesExpanded }
        ) {
            val isDineInActive = viewModel.storeState.collectAsState().value?.isDineInActive ?: true
            val isTakeawayActive = viewModel.storeState.collectAsState().value?.isTakeawayActive ?: true
            val isDeliveryActive = viewModel.storeState.collectAsState().value?.isDeliveryActive ?: true

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceMethodToggle(
                    title = "Makan di Sini (Dine-in)",
                    description = "Aktifkan jika restoran melayani makan di tempat",
                    icon = Icons.Default.Restaurant,
                    isActive = isDineInActive,
                    onCheckedChange = { viewModel.updateOrderMethodActive("dinein", it) }
                )

                ServiceMethodToggle(
                    title = "Bungkus (Takeaway)",
                    description = "Aktifkan jika pelanggan bisa pesan untuk dibawa pulang",
                    icon = Icons.Default.List,
                    isActive = isTakeawayActive,
                    onCheckedChange = { viewModel.updateOrderMethodActive("takeaway", it) }
                )

                ServiceMethodToggle(
                    title = "Antar (Delivery)",
                    description = "Aktifkan jika restoran melayani pengiriman pesanan",
                    icon = Icons.Default.Share,
                    isActive = isDeliveryActive,
                    onCheckedChange = { viewModel.updateOrderMethodActive("delivery", it) }
                )

                val isWaOrderNotificationActive = viewModel.storeState.collectAsState().value?.isWaOrderNotificationActive ?: false
                ServiceMethodToggle(
                    title = "Dapatkan Notifikasi Pesanan Masuk Lewat WA",
                    description = "Membuka WhatsApp pelanggan secara otomatis saat pesanan dibuat",
                    icon = Icons.Default.Call,
                    isActive = isWaOrderNotificationActive,
                    onCheckedChange = { viewModel.updateOrderMethodActive("wanotif", it) }
                )

            }
        }

        // --- SHIPPING ZONES CARD ---
        val isDeliveryActive = viewModel.storeState.collectAsState().value?.isDeliveryActive ?: true
        AnimatedVisibility(visible = isDeliveryActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenShipping() }
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Navy.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zona & Ongkos Kirim",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy)
                    )
                    Text(
                        text = "Atur biaya pengiriman ke berbagai area",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Buka",
                    tint = Color.Gray
                )
            }
        }
        }

        // --- NOTIFIKASI & SUARA ---
        val customSoundPath by viewModel.customSoundPath.collectAsState()
        var isNotificationExpanded by remember { mutableStateOf(false) }
        
        ExpandableSettingCard(
            title = "Nada Notifikasi Pesanan",
            subtitle = if (customSoundPath == null) "Default (MejaPesan Sound)" else "Custom Sound Aktif",
            icon = Icons.Default.Notifications,
            isExpanded = isNotificationExpanded,
            onExpandClick = { isNotificationExpanded = !isNotificationExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Navy.copy(alpha = 0.03f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Navy.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VolumeUp, null, tint = Navy, modifier = Modifier.size(20.dp))
                    }
                    
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            text = if (customSoundPath == null) "Suara Standar" else customSoundPath!!.substringAfterLast("/"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Navy)
                        )
                        Text(
                            text = "Akan berbunyi saat ada pesanan baru",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }

                    // Test Sound Button
                    IconButton(
                        onClick = { 
                            // Play test sound logic
                            viewModel.playTestSound(context)
                        },
                        modifier = Modifier.background(QuackYellow.copy(alpha = 0.2f), CircleShape).size(36.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, null, tint = QuackYellowDark, modifier = Modifier.size(18.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { audioLauncher.launch("audio/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Ganti Suara", fontSize = 12.sp, color = Color.White)
                    }

                    if (customSoundPath != null) {
                        OutlinedButton(
                            onClick = { viewModel.resetCustomSound(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.3f))
                        ) {
                            Text("Reset", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppNumberEditor(
    initialNumber: String,
    onSave: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var text by remember(initialNumber) { mutableStateOf(initialNumber) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) { 
             // Icon WA (Ensure ic_whatsapp.png exists in drawable)
             Image(
                 painter = painterResource(id = R.drawable.ic_whatsapp),
                 contentDescription = "WhatsApp",
                 modifier = Modifier.size(24.dp).padding(end=8.dp)
             )
             Text(
                text = "Nomor WhatsApp",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Navy
                ),
                modifier = Modifier.padding(bottom = 0.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> 
                        // Sanitize: hanya ambil angka saja (biar bisa paste nomor berformat +, -, atau spasi)
                        val filtered = input.filter { it.isDigit() }
                        if (filtered.length <= 15) text = filtered 
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF25D366), // WA Color
                        unfocusedBorderColor = Color.LightGray,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true,
                    placeholder = { Text("628...") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                
                // Save Button
                IconButton(
                    onClick = { 
                        onSave(text)
                        isEditing = false 
                    },
                    modifier = Modifier.background(Color(0xFF25D366), CircleShape).size(40.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                }

                // Cancel Button
                IconButton(
                    onClick = { 
                        text = initialNumber
                        isEditing = false 
                    },
                    modifier = Modifier.background(Color.LightGray.copy(alpha=0.2f), CircleShape).size(40.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Navy)
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (initialNumber.isNotEmpty()) {
                    Text(
                        text = initialNumber,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Navy),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Belum diset (Contoh: 628...)",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                IconButton(onClick = { isEditing = true }) {
                    // Lock icon implies "Secure/Fixed" until edited
                    Icon(
                        imageVector = if (initialNumber.isNotEmpty()) Icons.Default.Lock else Icons.Default.Edit, 
                        contentDescription = "Edit WA", 
                        tint = if (initialNumber.isNotEmpty()) Color.Gray else Navy
                    )
                }
            }
        }
    }
}

@Composable
fun RestaurantNameEditor(
    initialName: String,
    onSave: (String) -> Unit,
    viewModel: ProfileViewModel
) {
    var isEditing by remember { mutableStateOf(false) }
    var text by remember(initialName) { mutableStateOf(initialName) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Nama Restoran",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Navy
            ),
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { 
                        // Allow typing freely but restrict extreme lengths for UI safety (e.g. 30 chars)
                        if (it.length <= 30) text = it 
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = Color.LightGray,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true
                )
                
                // Save Button (WITH VALIDATION ON CLICK)
                val context = LocalContext.current
                IconButton(
                    onClick = { 
                        if (text.length > 10) {
                            // SHOW TOAST ONLY ON CLICK
                            android.widget.Toast.makeText(context, "Maksimal 10 huruf!", android.widget.Toast.LENGTH_SHORT).show()
                        } else if (text.isBlank()) {
                            android.widget.Toast.makeText(context, "Nama toko tidak boleh kosong!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onSave(text)
                            isEditing = false 
                        }
                    },
                    modifier = Modifier.background(Navy, CircleShape).size(40.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                }

                // Cancel Button
                IconButton(
                    onClick = { 
                        text = initialName
                        isEditing = false 
                    },
                    modifier = Modifier.background(Color.LightGray.copy(alpha=0.2f), CircleShape).size(40.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Navy)
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = initialName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Navy),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Navy)
                }
            }
        }
    }
}



@Composable
fun ProfilePictureDialog(
    logoUrl: String?,
    onDismiss: () -> Unit,
    onChangePhoto: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Foto Profil Restoran",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy)
                )

                // Large Image
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .border(4.dp, Navy, CircleShape)
                        .background(Color.Gray)
                ) {
                    val imageUrl = id.quacxel.mejapesan.utils.ImageUtils.getDynamicImageUrl(logoUrl)
                    if (imageUrl != null) {
                        coil.compose.AsyncImage(
                            model = imageUrl,
                            contentDescription = "Full Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = androidx.compose.ui.res.painterResource(id = R.drawable.profile_alt)
                        )
                    } else {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.profile_alt),
                            contentDescription = "Full Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Actions
                Button(
                    onClick = onChangePhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ganti Foto", fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onDismiss,
                     modifier = Modifier.fillMaxWidth(),
                     shape = RoundedCornerShape(8.dp),
                     border = androidx.compose.foundation.BorderStroke(1.dp, Navy),
                     colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FooterActions(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                // Professional Logout Sequence
                id.quacxel.mejapesan.utils.SessionManager.logout(context, scope) {
                    // Navigate to Login/Welcome by restarting the app
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                    val componentName = intent?.component
                    val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
                    context.startActivity(mainIntent)
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical=24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Privacy Policy Button
        OutlinedButton(
            onClick = { onNavigate("privacy_policy") },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy),
            border = androidx.compose.foundation.BorderStroke(1.dp, Navy.copy(alpha=0.3f))
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kebijakan Privasi", color = Navy, fontWeight = FontWeight.Bold)
        }

        // Logout Button
        Button(
            onClick = { showLogoutDialog = true },
             colors = ButtonDefaults.buttonColors(containerColor = Danger.copy(alpha=0.1f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Danger)
        ) {
            // Fallback to Close Icon if ExitToApp is not available
            Icon(Icons.Default.Close, contentDescription = null, tint = Danger)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Keluar Akun", color = Danger, fontWeight = FontWeight.Bold)
        }
        
        Text("Versi Aplikasi 1.0.3", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Danger.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Fallback to Close Icon if ExitToApp is not available
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = Danger,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Keluar Akun?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Anda harus login kembali untuk mengakses data kasir.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                        textAlign = TextAlign.Center
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Keluar", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    // Preview won't work well due to ViewModel dependency without mocking
}
@Composable
fun LogoDisplay(source: LogoSource?, modifier: Modifier = Modifier) {
    if (source == null) return
    
    when (source) {
        is LogoSource.Resource -> {
            Image(
                painter = painterResource(id = source.id),
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Fit
            )
        }
        is LogoSource.Generated -> {
            Box(
                modifier = modifier
                    .background(source.color.copy(alpha = 0.1f), CircleShape) // Light bg
                    .border(1.dp, source.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = source.text.take(2).uppercase(), // Initials
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = source.color,
                        fontSize = 10.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FriendlyInfoDialog(
    message: String,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Navy.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Navy,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Informasi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mengerti", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ServiceMethodToggle(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Navy)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            androidx.compose.material3.Switch(
                checked = isActive,
                onCheckedChange = onCheckedChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981), // Green
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }
    }
}

@Composable
fun CashModeTile(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF10B981).copy(alpha = 0.05f) else Color(0xFFF9FAFB))
            .border(
                1.dp,
                if (selected) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFE5E7EB),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF10B981),
                unselectedColor = Color.Gray
            )
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Navy)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun PromotionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Navy)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = color
                )
            } else {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ExpandableSettingCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Navy.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Navy, modifier = Modifier.size(20.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Navy)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}
