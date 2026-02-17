package com.example.kasir

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
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
import com.example.kasir.viewmodel.ProfileViewModel
import com.example.kasir.utils.FileUtils
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.kasir.R

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
    "DANA" to LogoSource.Resource(R.drawable.ewallet_dana),
    "OVO" to LogoSource.Resource(R.drawable.ewallet_ovo),
    "GoPay" to LogoSource.Resource(R.drawable.ewallet_gopay)
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
    val isSaving = isLoading // Alias for clarity
    val errorMessage by viewModel.errorMessage.collectAsState()
    
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
    
    // Clean up error message on successful dialog show to prevent re-triggering on rotation/recomposition
    LaunchedEffect(errorMessage) {
        if (errorMessage == "Pengaturan pembayaran disimpan") {
            showSaveSuccessDialog = true
            viewModel.clearErrorMessage() // Consume the event immediately
        }
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

    Scaffold(
        topBar = {
            ProfileTopBar(onBack = { onNavigate("dashboard") })
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (isLoading) {
             Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = Navy)
             }
        } else {
             Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    onNameChange = { /* handled in button or on value change */ }, 
                    onLogoClick = { logoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    viewModel = viewModel
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



                // Error Message (Removed success text in favor of dialog)
                if (errorMessage != null && errorMessage != "Pengaturan pembayaran disimpan") {
                    Text(text = errorMessage!!, color = Danger, style = MaterialTheme.typography.bodySmall)
                }

                // Section 4: Footer Actions
                Spacer(modifier = Modifier.weight(1f)) // Push to bottom if content is short
                FooterActions(onNavigate)
            }
        }
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
    storeState: com.example.kasir.data.model.Store?,
    onSave: (String?, String?, String?, String?, String?, String?) -> Unit,
    viewModel: com.example.kasir.viewmodel.ProfileViewModel
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
                                onValueChange = { if (it.all { c -> c.isDigit() }) bankNumber = it },
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
                                onValueChange = { bankHolder = it },
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
                            val ewallets = listOf("ShopeePay", "DANA", "OVO", "GoPay")
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
                                onValueChange = { if (it.all { c -> c.isDigit() }) ewalletNumber = it },
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
                                onValueChange = { ewalletName = it },
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
                            !hasBank && bankName.isNotEmpty() && bankNumber.isNotEmpty() && bankHolder.isNotEmpty()
                        } else {
                            !hasEwallet && ewalletType.isNotEmpty() && ewalletNumber.isNotEmpty() && ewalletName.isNotEmpty()
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
    storeState: com.example.kasir.data.model.Store?,
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

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
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
                enabled = ((selectedMethod == "Bank Transfer" && bankAvailable) || (selectedMethod == "ShopeePay" && shopeeAvailable)) && (amountText.toIntOrNull() ?: 0) > 0,
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
fun WithdrawalHistoryDialog(
    history: List<com.example.kasir.data.model.Withdrawal>,
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
fun WithdrawalHistoryItem(item: com.example.kasir.data.model.Withdrawal) {
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
    onNameChange: (String) -> Unit,
    onLogoClick: () -> Unit,
    viewModel: ProfileViewModel
) {
    var showProfileDialog by remember { mutableStateOf(false) }

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
                val imageUrl = com.example.kasir.utils.ImageUtils.getDynamicImageUrl(logoUrl)

                Image(
                    painter = rememberAsyncImagePainter(imageUrl ?: "https://via.placeholder.com/150"),
                    contentDescription = "Restaurant Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
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

        // Name Input
        RestaurantNameEditor(
            initialName = name,
            onSave = { newName -> viewModel.updateName(newName) }
        )

        // WhatsApp Number Input
        WhatsAppNumberEditor(
            initialNumber = viewModel.storeState.collectAsState().value?.whatsappNumber ?: "",
            onSave = { newNumber -> viewModel.updateWhatsApp(newNumber) }
        )
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
                    onValueChange = { 
                        // Only allow numbers
                        if (it.all { char -> char.isDigit() }) text = it 
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
                        modifier = Modifier.weight(1f)
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
    onSave: (String) -> Unit
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
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = Color.LightGray,
                        focusedTextColor = Color.Black, // Ensure visibility
                        unfocusedTextColor = Color.Black // Ensure visibility
                    ),
                    singleLine = true
                )
                
                // Save Button
                IconButton(
                    onClick = { 
                        onSave(text)
                        isEditing = false 
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
                    modifier = Modifier.weight(1f)
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
                    val imageUrl = com.example.kasir.utils.ImageUtils.getDynamicImageUrl(logoUrl)
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl ?: "https://via.placeholder.com/300"),
                        contentDescription = "Full Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
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
                scope.launch {
                    com.example.kasir.utils.SessionManager.clear(context)
                    // Navigate to Login/Welcome
                    // Since onNavigate is String based, and graph might expect route. Assuming "login" or similar.
                    // But ProfileScreen usually just clears and relies on MainActivity to observe session or similar.
                    // For now, let's trigger the restart/navigation logic.
                    // If onNavigate expects a route:
                     val packageManager = context.packageManager
                     val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                     val componentName = intent?.component
                     val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
                     context.startActivity(mainIntent)
                     Runtime.getRuntime().exit(0)
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical=24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
        
        Text("Versi Aplikasi 1.0.0", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
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
