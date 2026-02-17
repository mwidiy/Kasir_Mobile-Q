package com.example.kasir

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.os.Build
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.utils.PdfExporter
import com.example.kasir.viewmodel.RiwayatViewModel
import com.example.kasir.data.model.OrderResponse
import com.example.kasir.data.model.OrderItemResponse // Import OrderItemResponse
import java.text.SimpleDateFormat
import java.util.Locale
import java.text.NumberFormat

// --- COLORS (From Source) ---
val RiwayatBgBody = Color(0xFFF8F9FA)
val RiwayatCardBg = Color(0xFF2D3E50)
val RiwayatTabBg = Color(0x1AFFFFFF) // rgba(255,255,255,0.1)
val RiwayatTabActive = Color(0x40FFFFFF) // rgba(255,255,255,0.25)
val RiwayatBorder = Color(0xFFE5E7EB)
val RiwayatYellow = Color(0xFFFDE047)
val PriceGreen = Color(0xFF2ECC71)
val PriceRed = Color(0xFFEF4444)
val PriceBlack = Color(0xFF111827)
val RiwayatTextMain = Color(0xFF1F2937)
val RiwayatTextMuted = Color(0xFF6B7280)

@Composable
fun RiwayatScreen(onNavigate: (String) -> Unit, viewModel: RiwayatViewModel = viewModel()) {
    // STATE BINDING
    val transactions by viewModel.displayedOrders.collectAsState()
    val analysis by viewModel.analysis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0=Today, 1=Week, 2=Month (Logic adapter)
    
    var selectedTransaction by remember { mutableStateOf<OrderResponse?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // SEARCH FOCUS STATE
    var isSearchFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Back Handler to exit Focus Mode
    BackHandler(enabled = isSearchFocused) {
        isSearchFocused = false
        focusManager.clearFocus()
    }

    LaunchedEffect(isSearchFocused) {
        if (!isSearchFocused) focusManager.clearFocus()
    }

    val context = LocalContext.current

    // Logic Effects
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    LaunchedEffect(selectedTab) {
        viewModel.applyFilter(selectedTab)
    }

    LaunchedEffect(searchQuery) {
        viewModel.search(searchQuery)
        // Auto-switch to "Semua" (Tab 2) if searching to show global results
        if (searchQuery.isNotEmpty() && selectedTab != 2) {
             selectedTab = 2 
             viewModel.setTabFilter(2)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(RiwayatBgBody)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { 
                AnimatedVisibility(
                    visible = !isSearchFocused,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    RiwayatHeader(
                   onExportClick = {
                        // 1. Validation: Prevent export if no data
                        if (transactions.isEmpty()) {
                            val periodName = when(selectedTab) {
                                0 -> "Hari Ini"
                                1 -> "Bulan Ini"
                                else -> "yang dipilih"
                            }
                            Toast.makeText(context, "Belum ada transaksi di $periodName", Toast.LENGTH_SHORT).show()
                            return@RiwayatHeader
                        }

                        // 2. Calculate Date Strings for Report Header
                        val calendar = java.util.Calendar.getInstance()
                        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                        var startDateStr: String? = null
                        var endDateStr: String? = null
                        
                        when (selectedTab) {
                            0 -> { // Hari Ini
                                val today = sdf.format(calendar.time)
                                startDateStr = today
                                endDateStr = today
                            }
                            1 -> { // Bulan Ini
                                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                                startDateStr = sdf.format(calendar.time)
                                
                                calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                                endDateStr = sdf.format(calendar.time)
                            }
                            // else -> "Semua" (null dates)
                        }

                        // 3. Client-Side Export (NEW STRATEGY)
                        // No network, no tokens, no timeouts.
                        try {
                            Toast.makeText(context, "Memproses PDF...", Toast.LENGTH_SHORT).show()
                            
                            // Get Analysis Data from ViewModel
                            val analysis = viewModel.analysis.value
                            
                            // Export via Utility
                            PdfExporter.export(
                                context = context,
                                orders = transactions, // Currently filtered list
                                analysis = analysis,
                                startDate = startDateStr,
                                endDate = endDateStr
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal export: ${e.message}", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                   }
                ) 
            }
            },
            bottomBar = { /* Custom Bottom Nav via Box */ }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .then(if (isSearchFocused) Modifier.statusBarsPadding().padding(top = 16.dp) else Modifier) // FIX: Add padding when focused
                    .padding(bottom = 100.dp) // Space for bottom nav
            ) {
                // SUMMARY CARD (Bound to Analysis)
                AnimatedVisibility(visible = !isSearchFocused) {
                    SummarySection(
                        selectedTabIdx = selectedTab, 
                        totalIncome = analysis.totalIncome, 
                        transactionCount = analysis.transactionCount, 
                        avgIncome = analysis.avgIncome,
                        onTabSelect = { index -> 
                             selectedTab = index 
                             viewModel.setTabFilter(index)
                        }
                    )
                }

                // FILTER BAR (Bound to Search)
                FilterBar(
                    query = searchQuery, 
                    onFilterClick = { showFilterDialog = true }, 
                    onQueryChange = { searchQuery = it },
                    isFocused = isSearchFocused,
                    onBack = { isSearchFocused = false },
                    onFocusTrigger = { isSearchFocused = true },
                    focusRequester = focusRequester
                )

                // TRANSACTION LIST
                 if (isLoading) {
                     Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator(color = RiwayatCardBg)
                     }
                } else if (error != null) {
                      // Error State
                      Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                          Text("Gagal memuat data: $error", color = PriceRed)
                      }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(transactions) { item ->
                            TransactionItem(item) { selectedTransaction = item }
                        }
                    }
                }
            }
        }



        // RECEIPT MODAL
        if (selectedTransaction != null) {
            ReceiptModal(data = selectedTransaction!!, onDismiss = { selectedTransaction = null })
        }

        // FILTER MODAL
        if (showFilterDialog) {
        // FILTER MODAL
        if (showFilterDialog) {
            com.example.kasir.ui.components.FilterHistoryDialog(
                initialStatus = viewModel.statusFilter,
                initialType = viewModel.typeFilter,
                onDismiss = { showFilterDialog = false },
                onApply = { status, type ->
                    viewModel.setAdvancedFilter(status, type)
                    showFilterDialog = false
                }
            )
        }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun RiwayatHeader(onExportClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Riwayat & Laporan",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )
        )

        // Export Button
        Row(
            modifier = Modifier
                .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .clickable { onExportClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = RiwayatTextMain)
            Text("Ekspor PDF", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = RiwayatTextMain))
        }
    }
}

@Composable
fun SummarySection(
    selectedTabIdx: Int, 
    totalIncome: String, 
    transactionCount: String, 
    avgIncome: String,
    onTabSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RiwayatCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // TABS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RiwayatTabBg, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabItem("Hari Ini", selectedTabIdx == 0) { onTabSelect(0) }
                    TabItem("Bulan Ini", selectedTabIdx == 1) { onTabSelect(1) }
                    TabItem("Semua", selectedTabIdx == 2) { onTabSelect(2) }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // INCOME
                Text("Total Pendapatan", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color(0xFFBDC3C7), fontSize = 12.sp)
                Text(totalIncome, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = Color(0x33FFFFFF), thickness = 1.dp)

                Spacer(modifier = Modifier.height(15.dp))

                // STATS GRID
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(transactionCount, style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        Text("Transaksi", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBDC3C7), fontSize = 11.sp))
                    }
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0x33FFFFFF)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(avgIncome, style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        Text("Rata-rata", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBDC3C7), fontSize = 11.sp))
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TabItem(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) RiwayatTabActive else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color = if (isActive) Color.White else Color(0xFFCCCCCC)
            )
        )
    }
}

@Composable
fun FilterBar(
    query: String, 
    onFilterClick: () -> Unit, 
    onQueryChange: (String) -> Unit,
    isFocused: Boolean = false,
    onBack: () -> Unit = {},
    onFocusTrigger: () -> Unit = {},
    focusRequester: FocusRequester
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Animate Shape
    val cornerRadius by animateDpAsState(
        targetValue = if (isFocused) 12.dp else 10.dp,
        label = "corner"
    )

    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button (Start)
        AnimatedVisibility(
            visible = isFocused,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RiwayatTextMain)
            }
        }

        // Search Input
        Box(modifier = Modifier.weight(1f)) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { 
                    Text(
                        if (isFocused) "Cari Transaction ID, Pelanggan..." else "Cari Order ID...", 
                        fontSize = 13.sp, 
                        color = RiwayatTextMuted
                    ) 
                },
                leadingIcon = if (isFocused) null else {
                    { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(18.dp)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerRadius))
                    .border(1.dp, if (isFocused) RiwayatTextMain else RiwayatBorder, RoundedCornerShape(cornerRadius))
                    .focusRequester(focusRequester),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isFocused) Color(0xFFF3F4F6) else Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = RiwayatTextMain,
                    unfocusedTextColor = RiwayatTextMain,
                    cursorColor = RiwayatTextMain
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

        // Filter Button (Hidden when Focused)
        AnimatedVisibility(
            visible = !isFocused,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
             Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, RiwayatBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onFilterClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filter", tint = RiwayatTextMain, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun TransactionItem(item: OrderResponse, onClick: () -> Unit) {
    val date = try {
         val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
         val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
         parser.parse(item.createdAt)?.let { formatter.format(it) } ?: "-"
    } catch (e: Exception) { "-" }

    val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val totalStr = fmt.format(item.totalAmount).replace("Rp", "Rp ").replace(",00", "")
    
    val isSuccess = item.status == "Completed"
    
     // Item Summary (First item + count)
    val itemSummary = if (item.items.isNotEmpty()) {
        val first = item.items[0]
        val others = item.items.size - 1
        "${first.quantity}x ${first.product?.name ?: "-"}" + if (others > 0) ", +$others lainnya" else ""
    } else "No items"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Meta (Left)
        Column(modifier = Modifier.width(50.dp)) {
            Text(date, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RiwayatTextMain))
            Text("#${item.queueNumber ?: item.id}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = Color(0xFF9CA3AF)))
        }

        // Desc (Middle)
        Text(
            text = itemSummary,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp, 
                color = Color(0xFF4B5563),
                lineHeight = 18.sp 
            ),
            maxLines = 2
        )

        // End (Right)
        // End (Right)
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = totalStr,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp,
                    color = if (isSuccess) PriceBlack else PriceRed
                )
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isSuccess) PriceGreen else PriceRed)
            )
        }
    }
}

@Composable
fun ReceiptModal(data: OrderResponse, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp), 
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header Title
                Text(
                    text = "Bukti Transaksi",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B7280) // Gray 500
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Hero Price
                val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                fmt.maximumFractionDigits = 0
                val totalStr = fmt.format(data.totalAmount)

                Text(
                    text = totalStr,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827) // Gray 900
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Status Badge
                val isCompleted = data.status == "Completed"
                val statusLabel = when(data.status) {
                    "Completed" -> "LUNAS"
                    "Cancelled" -> "DIBATALKAN"
                    "Pending" -> "MENUNGGU"
                    "Processing" -> "DIPROSES"
                    else -> data.status?.uppercase() ?: "-"
                }
                val statusBg = when(data.status) {
                    "Completed" -> Color(0xFFDCFCE7) // Green 100
                    "Cancelled" -> Color(0xFFFEE2E2) // Red 100
                    else -> Color(0xFFFEF08A) // Yellow 100
                }
                val statusText = when(data.status) {
                    "Completed" -> Color(0xFF166534) // Green 800
                    "Cancelled" -> Color(0xFF991B1B) // Red 800
                    else -> Color(0xFF854D0E) // Yellow 800
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusBg)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusText,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Dashed Divider
                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp) // Simple divider
                
                Spacer(modifier = Modifier.height(24.dp))

                // 5. Metadata Grid
                val dateStr = try {
                     val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                     val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                     parser.parse(data.createdAt)?.let { formatter.format(it) } ?: "-"
                } catch (e: Exception) { "-" }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.Start) {
                         Text("Tanggal", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9CA3AF)))
                         Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF374151), fontWeight = FontWeight.Medium))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                         Text("Order ID", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9CA3AF)))
                         Text("#${data.queueNumber ?: data.id}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF374151), fontWeight = FontWeight.Medium))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.Start) {
                         Text("Pelanggan", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9CA3AF)))
                         Text(data.customerName, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF374151), fontWeight = FontWeight.Medium))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                         Text("Meja", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9CA3AF)))
                         Text(data.table?.name ?: "Takeaway", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF374151), fontWeight = FontWeight.Medium))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // 6. Section Header: Menu
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                     Text("Rincian Menu", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
                }
                Spacer(modifier = Modifier.height(12.dp))

                // 7. Item List (Scrollable if needed, but Dialog height constrains it)
                Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                     androidx.compose.foundation.lazy.LazyColumn(
                         verticalArrangement = Arrangement.spacedBy(12.dp)
                     ) {
                         items(data.items) { item ->
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.SpaceBetween
                             ) {
                                 Row(modifier = Modifier.weight(1f)) {
                                     Text(
                                         "${item.quantity}x", 
                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF374151)),
                                         modifier = Modifier.width(30.dp)
                                     )
                                     Text(
                                         item.product?.name ?: "-", 
                                         style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4B5563))
                                     )
                                 }
                                 val price = fmt.format((item.priceSnapshot ?: 0))
                                 Text(
                                     price, 
                                     style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Color(0xFF111827))
                                 )
                             }
                         }
                     }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // 8. Payment Method
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Metode Pembayaran", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)))
                    Text(data.paymentMethod ?: "-", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 9. Buttons
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)) // Dark Gray
                ) {
                    Text("Tutup", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF), fontSize = 13.sp))
        Text(value, modifier = Modifier.weight(1f), textAlign = TextAlign.Right, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = RiwayatTextMain, fontSize = 13.sp))
    }
}

@Composable
fun OrderRow(item: String, price: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(item, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4B5563), fontSize = 13.sp))
        Text(price, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4B5563), fontSize = 13.sp))
    }
}

@Composable
fun PaymentRow(label: String, price: String, isTotal: Boolean, fontSize: TextUnit = 13.sp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = if(isTotal) 8.dp else 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = if(isTotal) RiwayatTextMain else Color(0xFF9CA3AF), fontSize = if(isTotal) 16.sp else fontSize, fontWeight = if(isTotal) FontWeight.Bold else FontWeight.Normal))
        Text(price, style = MaterialTheme.typography.bodySmall.copy(color = if(isTotal) RiwayatTextMain else Color(0xFF9CA3AF), fontSize = if(isTotal) 16.sp else fontSize, fontWeight = if(isTotal) FontWeight.Bold else FontWeight.Normal))
    }
}

@Composable
fun RiwayatNavItem(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(60.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) RiwayatTextMain else Color(0xFF9CA3AF),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) RiwayatTextMain else Color(0xFF9CA3AF)
            )
        )
    }
}
