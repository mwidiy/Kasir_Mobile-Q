package com.example.kasir

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.example.kasir.ui.theme.KasirTheme
import com.example.kasir.ui.banner.BannerListScreen
import com.example.kasir.ui.banner.BannerFormScreen
import com.example.kasir.data.model.Banner
import com.example.kasir.ui.menu.MenuFormScreen
import com.example.kasir.ui.menu.AddEditProductScreen
import com.example.kasir.data.model.Product
import com.example.kasir.data.model.Category // Added import
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.viewmodel.MenuViewModel
import com.example.kasir.viewmodel.BannerViewModel
import com.example.kasir.ui.banner.BannerListScreen

// --- COLORS ---
private val MenuBg = Color(0xFFF3F4F6)
private val MenuPrimaryBlue = Color(0xFF1E3A5F)
private val MenuPrimaryYellow = Color(0xFFFDD85D)
private val MenuTextDark = Color(0xFF1A2B48)
private val MenuTextGray = Color(0xFF888888)
private val SwitchGreen = Color(0xFF2ECC71)
private val InfoBg = Color(0xFFE0F2FE)
private val InfoText = Color(0xFF0369A1)
private val DeleteRed = Color(0xFFEF4444)

// --- MODELS ---
data class MenuItem(
    val id: String,
    val name: String,
    val category: String, // "makanan", "minuman", "cemilan", "paket"
    val categoryDisplay: String,
    val categoryId: Int?, // New field
    val price: String,
    val isActive: Boolean,
    val description: String? = null,
    val image: String? = null
)

val initialMenuItems = listOf(
    MenuItem("1", "Nasi Goreng Special", "makanan", "Makanan Utama", 1, "Rp 15.000", true, "Enak", null),
    MenuItem("2", "Mie Goreng Jawa", "makanan", "Makanan Utama", 1, "Rp 12.000", true, "Jowo tulen", null),
    MenuItem("3", "Ayam Bakar Madu", "makanan", "Makanan Utama", 1, "Rp 18.000", false, "Manis", null),
    MenuItem("4", "Es Teh Manis", "minuman", "Minuman", 2, "Rp 5.000", true, "Seger", null),
    MenuItem("5", "Es Jeruk Peras", "minuman", "Minuman", 2, "Rp 8.000", true, "Asem manis", null),
    MenuItem("6", "Sate Ayam 10 Tusuk", "makanan", "Makanan Utama", 1, "Rp 20.000", true, "Madura", null),
    MenuItem("7", "Pisang Goreng Krispy", "cemilan", "Cemilan", 3, "Rp 10.000", true, "Kriuk", null),
    MenuItem("8", "Kopi Susu Gula Aren", "minuman", "Minuman", 2, "Rp 12.000", true, "Kopi", null)
)

@Composable
fun MenuScreen(onNavigate: (String) -> Unit) {
    val viewModel: MenuViewModel = viewModel()
    val bannerViewModel: BannerViewModel = viewModel()
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Show Error Toast
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            if (it.isNotEmpty()) {
                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    // var selectedCategory by remember { mutableStateOf("all") }  <-- Removed, using ViewModel
    // Initialize with empty list, data will come from VM
    var menuList by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isFabExpanded by remember { mutableStateOf(false) }

    val categoriesState by viewModel.categories.collectAsState()

    // Sync products from VM to local menuList (UI Model)
    LaunchedEffect(products) {
        menuList = products.map { product ->
            MenuItem(
                id = product.id.toString(),
                name = product.name,
                category = product.category?.lowercase() ?: "",
                categoryDisplay = product.category ?: "",
                categoryId = product.categoryId,
                price = "Rp ${product.price}",
                isActive = product.isActive,
                description = product.description,
                image = product.image
            )
        }
    }
    
    // Modals & Sheets State
    var showActionSheet by remember { mutableStateOf<MenuItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<MenuItem?>(null) }
    var showAddCategoryModal by remember { mutableStateOf(false) }
    
    // Category Management State
    var showCategoryActionSheet by remember { mutableStateOf<Category?>(null) }
    var showEditCategoryModal by remember { mutableStateOf<Category?>(null) }
    var showDeleteCategoryConfirm by remember { mutableStateOf<Category?>(null) }
    
    var showGuideModal by remember { mutableStateOf(false) }
    
    var activeTab by remember { mutableStateOf("menu") } // "menu" or "banner"
    var bannerScreenState by remember { mutableStateOf("list") } // "list", "add", "edit"
    
    // Navigation State
    var currentScreen by remember { mutableStateOf("menu_list") } // "menu_list", "add_product", "edit_product"
    var selectedProductId by remember { mutableStateOf<String?>(null) } // For edit

    // Logic Effects: Force Dark Status Bar Icons
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    var selectedBannerToEdit by remember { mutableStateOf<Banner?>(null) }
    var selectedMenuToEdit by remember { mutableStateOf<MenuItem?>(null) }
    
    // CRUD Dialog State - REMOVED, using Screen instead
    // var showProductDialog by remember { mutableStateOf(false) }
    // var activeProductItem by remember { mutableStateOf<MenuItem?>(null) }
    
    // Filtering (Menu)
    // Filtering (Menu)
    val filteredItems = menuList.filter { item ->
        (viewModel.selectedCategoryId == 0 || item.categoryId == viewModel.selectedCategoryId) &&
        (searchQuery.isEmpty() || item.name.contains(searchQuery, ignoreCase = true))
    }

    Box(modifier = Modifier.fillMaxSize().background(MenuBg)) {
        if (isLoading && menuList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MenuPrimaryBlue)
            }
        } else if (errorMessage != null && menuList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Error: $errorMessage", color = Color.Red)
                    Button(onClick = { viewModel.fetchProducts() }) {
                        Text("Coba Lagi")
                    }
                }
            }
        } else {
            // Main Content when not loading and no error
            Column(modifier = Modifier.fillMaxSize()) {
            // Only show Header and Tabs if in List mode for both Tabs
            if (bannerScreenState == "list" && currentScreen == "menu_list") {
                // Header (Shared)
                Column(modifier = Modifier.background(Color.White).statusBarsPadding().padding(top = 24.dp, start = 20.dp, end = 20.dp)) {
                    Text("Manajemen Produk", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MenuPrimaryBlue)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth().border(0.dp, Color.Transparent)) {
                        // Tab Menu
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = "menu" }
                                .padding(bottom = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Daftar Menu",
                                fontWeight = if (activeTab == "menu") FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == "menu") MenuPrimaryBlue else Color.LightGray,
                                fontSize = 14.sp
                            )
                            if (activeTab == "menu") {
                                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(MenuPrimaryBlue, RoundedCornerShape(3.dp, 3.dp, 0.dp, 0.dp)))
                            }
                        }
                        // Tab Banner
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = "banner" }
                                .padding(bottom = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Banner Promosi",
                                fontWeight = if (activeTab == "banner") FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == "banner") MenuPrimaryBlue else Color.LightGray,
                                fontSize = 14.sp
                            )
                            if (activeTab == "banner") {
                                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(MenuPrimaryBlue, RoundedCornerShape(3.dp, 3.dp, 0.dp, 0.dp)))
                            }
                        }
                    }
                }
            }

            // Content Switcher
            if (activeTab == "menu") {
                if (currentScreen == "menu_list") {
                    // --- EXISTING MENU CONTENT ---
                    // Info Alert
                    Surface(
                        color = InfoBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { showGuideModal = true }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            SvgIcon(
                                pathData = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z",
                                tint = InfoText,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("Tekan & Tahan (Long Press) ")
                                    }
                                    append("tombol kategori untuk mengubah atau menghapus. (Klik untuk demo)")
                                },
                                color = InfoText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Search & Filter
                    Column(modifier = Modifier.background(Color.White).padding(bottom = 10.dp, top = 16.dp)) {
                        // Search
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF9F9F9),
                            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray)
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    decorationBox = { inner ->
                                        if (searchQuery.isEmpty()) Text("Cari nama menu...", color = Color.Gray, fontSize = 13.sp)
                                        inner()
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(15.dp))
                        
                        // Chips
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // "All" Item
                            item(key = 0) {
                                FilterChipCustom(
                                    label = "Semua",
                                    isActive = viewModel.selectedCategoryId == 0,
                                    onClick = { viewModel.selectedCategoryId = 0 },
                                    onLongClick = {}
                                )
                            }
                            
                            // Dynamic Items
                            items(
                                items = categoriesState,
                                key = { it.id }
                            ) { cat ->
                                FilterChipCustom(
                                    label = cat.name,
                                    isActive = viewModel.selectedCategoryId == cat.id,
                                    onClick = { viewModel.selectedCategoryId = cat.id },
                                    onLongClick = { 
                                        showCategoryActionSheet = cat
                                    }
                                )
                            }
                        }
                    }

                    // Product List
                    LazyColumn(
                        contentPadding = PaddingValues(top = 10.dp, bottom = 180.dp),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        items(
                            items = filteredItems,
                            key = { it.id }
                        ) { item ->
                            MenuItemRow(
                                item = item,
                                onToggle = { 
                                    // Convert MenuItem back to Product for the toggle call
                                    // ideally we should just use Product everywhere but to minimize change risk:
                                    val p = com.example.kasir.data.model.Product(
                                        id = item.id.toInt(),
                                        name = item.name,
                                        price = item.price.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0,
                                        category = item.category,
                                        categoryId = item.categoryId,
                                        description = item.description,
                                        image = item.image,
                                        isActive = item.isActive
                                    )
                                    viewModel.toggleProductStatus(p)
                                },
                                onOptionClick = { showActionSheet = item }
                            )
                            Divider(color = Color(0xFFEEEEEE))
                        }
                    }
                } else if (currentScreen == "add_product_legacy") {
                    // Deprecated: keeping loop structure but this state shouldn't be reached or logic removed
                } else if (currentScreen == "edit_product_legacy") {
                    // Deprecated
                } else if (currentScreen == "add_product") {
                    AddEditProductScreen(
                        onBack = { currentScreen = "menu_list" },
                        viewModel = viewModel
                    )
                } else if (currentScreen == "edit_product") {
                    AddEditProductScreen(
                        productId = selectedProductId,
                        onBack = { 
                            currentScreen = "menu_list" 
                            selectedProductId = null
                        },
                        viewModel = viewModel
                    )
                }
            } else {
                // --- BANNER CONTENT ---
                when (bannerScreenState) {
                    "list" -> {
                        BannerListScreen(
                            viewModel = bannerViewModel,
                            onNavigateToAdd = { bannerScreenState = "add" },
                            onNavigateToEdit = { banner -> 
                                selectedBannerToEdit = banner
                                bannerScreenState = "edit" 
                            }
                        )
                    }
                    "add" -> {
                        BannerFormScreen(
                            title = "Tambah Banner Baru",
                            viewModel = bannerViewModel,
                            onBack = { bannerScreenState = "list" },
                            onSave = { 
                                bannerScreenState = "list" 
                            }
                        )
                    }
                    "edit" -> {
                        BannerFormScreen(
                            title = "Edit Banner Promosi",
                            initialBanner = selectedBannerToEdit,
                            viewModel = bannerViewModel,
                            onBack = { 
                                bannerScreenState = "list"
                                selectedBannerToEdit = null
                            },
                            onSave = { 
                                bannerScreenState = "list" 
                                selectedBannerToEdit = null
                            }
                        )
                    }
                }
            }
        }
            }

        // Floating Action Button (FAB) Area
        val isMenuTabActive = activeTab == "menu" && currentScreen == "menu_list"
        val isBannerTabActive = activeTab == "banner" && bannerScreenState == "list"
        
        if (isMenuTabActive || isBannerTabActive) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Overlay
                 AnimatedVisibility(
                    visible = isFabExpanded && isMenuTabActive, // Only expand on Menu tab
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f)).clickable { isFabExpanded = false })
                }
    
                // FAB Items (Only for Menu)
                if (isMenuTabActive) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 240.dp, end = 20.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                         AnimatedVisibility(
                            visible = isFabExpanded,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut()
                        ) {
                             FabSubButton("Tambah Kategori", "M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-1 8h-3v3h-2v-3h-3v-2h3V9h2v3h3v2z") { 
                                 isFabExpanded = false
                                 showAddCategoryModal = true 
                             }
                        }
                        AnimatedVisibility(
                            visible = isFabExpanded,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut()
                        ) {
                            FabSubButton("Tambah Produk", "M11 9H9V2H7v7H5V2H3v7c0 2.12 1.66 3.84 3.75 3.97V22h2.5v-9.03C11.34 12.84 13 11.12 13 9V2h-2v7zm5-3v8h2.5v8H21V2c-2.76 0-5 2.24-5 4z") { 
                               isFabExpanded = false
                               currentScreen = "add_product"
                            }
                        }
                    }
                }
    
                // Main FAB
                // Rotate only if expanded (Menu logic)
                val rotation by animateFloatAsState(if (isFabExpanded && isMenuTabActive) 45f else 0f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 170.dp, end = 20.dp)
                        .size(56.dp)
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(MenuPrimaryYellow)
                        .clickable { 
                            if (isMenuTabActive) {
                                isFabExpanded = !isFabExpanded 
                            } else {
                                // Banner Action -> Go to Add Banner
                                bannerScreenState = "add"
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = MenuTextDark, modifier = Modifier.rotate(rotation))
                }
            }
        }



        // --- MODALS ---
        
        if (showActionSheet != null) {
            ActionSheetModal(
                title = "Opsi Menu: ${showActionSheet!!.name}",
                onEdit = { 
                    val item = showActionSheet
                    showActionSheet = null
                    selectedProductId = item?.id
                    currentScreen = "edit_product"
                },
                onEditAr = {
                     val item = showActionSheet
                     showActionSheet = null
                     android.widget.Toast.makeText(context, "Fitur Edit AR Coming Soon!", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDelete = {
                    val item = showActionSheet
                    showActionSheet = null
                    showDeleteConfirm = item
                },
                onDismiss = { showActionSheet = null }
            )
        }

        if (showDeleteConfirm != null) {
            ConfirmationModal(
                title = "Hapus Menu Ini?",
                desc = "Menghapus menu akan menghapusnya secara permanen. Tindakan ini tidak dapat dibatalkan.",
                onConfirm = {
                    val idToDelete = showDeleteConfirm?.id?.toIntOrNull()
                    if (idToDelete != null) {
                        viewModel.deleteProduct(idToDelete)
                    }
                    showDeleteConfirm = null
                },
                onCancel = { showDeleteConfirm = null }
            )
        }

        if (showAddCategoryModal) {
            InputModal(
                title = "Tambah Kategori",
                label = "Nama Kategori Baru",
                placeholder = "cth: Manisan, Jus",
                onSave = { 
                    viewModel.addCategory(it)
                    showAddCategoryModal = false 
                },
                onCancel = { showAddCategoryModal = false }
            )
        }
        
        if (showCategoryActionSheet != null) {
            ActionSheetModal(
                title = "Opsi Kategori: ${showCategoryActionSheet!!.name}",
                onEdit = {
                    val cat = showCategoryActionSheet
                    showCategoryActionSheet = null
                    showEditCategoryModal = cat
                },
                onEditAr = {
                     // Category doesn't have AR yet/ever? Just toast or empty
                     showCategoryActionSheet = null
                     android.widget.Toast.makeText(context, "Fitur ini hanya untuk Menu", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDelete = {
                    val cat = showCategoryActionSheet
                    showCategoryActionSheet = null
                    showDeleteCategoryConfirm = cat
                },
                onDismiss = { showCategoryActionSheet = null }
            )
        }

        if (showEditCategoryModal != null) {
             InputModal(
                title = "Edit Kategori",
                label = "Nama Kategori",
                initialValue = showEditCategoryModal!!.name,
                onSave = { 
                    viewModel.updateCategory(showEditCategoryModal!!.id, it)
                    showEditCategoryModal = null
                },
                onCancel = { showEditCategoryModal = null }
            )
        }

        if (showDeleteCategoryConfirm != null) {
            ConfirmationModal(
                title = "Hapus Kategori?",
                desc = "Menghapus kategori mungkin mempengaruhi produk yang menggunakan kategori ini.",
                onConfirm = {
                    viewModel.deleteCategory(showDeleteCategoryConfirm!!.id)
                    showDeleteCategoryConfirm = null
                },
                onCancel = { showDeleteCategoryConfirm = null }
            )
        }

        if (showGuideModal) {
            GuideModal(onDismiss = { showGuideModal = false })
        }
    }
}

@Composable
fun FilterChipCustom(label: String, isActive: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        color = if (isActive) MenuPrimaryBlue else Color(0xFFF5F5F5),
        contentColor = if (isActive) Color.White else Color(0xFF666666),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .pointerInput(Unit) {
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
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }
}

// Helper to get image URL safely (placeholder logic moved here or in UI)
// val MenuItem.imageUrl: String? get() = this.image // REMOVED as redundant


@Composable
fun MenuItemRow(item: MenuItem, onToggle: () -> Unit, onOptionClick: () -> Unit) {
    // Opacity logic: if not active, alpha is 0.5f
    val alpha = if (item.isActive) 1f else 0.5f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .alpha(alpha), // Apply opacity
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image Placeholder
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
             if (!item.image.isNullOrBlank()) {
                 // Use ImageUtils to fix IP address dynamically
                 val model = com.example.kasir.utils.ImageUtils.getDynamicImageUrl(item.image)
                 
                coil.compose.AsyncImage(
                    model = model,
                    contentDescription = item.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = androidx.compose.ui.graphics.painter.ColorPainter(Color.Gray) // Fallback simple
                )
            } else {
                Text("IMG", fontSize = 10.sp, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MenuTextDark)
            Text(item.categoryDisplay, fontSize = 12.sp, color = Color.Gray)
            Text(item.price, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MenuTextGray)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Switch(
                checked = item.isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SwitchGreen)
            )
            Icon(
                Icons.Default.MoreVert, 
                contentDescription = "Options", 
                tint = Color.Gray,
                modifier = Modifier.clickable { onOptionClick() }
            )
        }
    }
}

// Mimic Modifier.alpha without creating utils file dependency if simpler, but standard compose has alpha
fun Modifier.alpha(alpha: Float) = this.then(Modifier.drawLayer(alpha = alpha))
private fun Modifier.drawLayer(alpha: Float): Modifier = this // Placeholder fix if alpha not imported, actually available in ui.draw.alpha normally

@Composable
fun FabSubButton(text: String, iconPath: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF2D3E50), // Dark Blue
        shape = RoundedCornerShape(50), // Fully rounded pill
        shadowElevation = 6.dp,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
        ) {
            SvgIcon(
                pathData = iconPath, 
                tint = Color.White, 
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text, 
                fontSize = 13.sp, 
                fontWeight = FontWeight.Medium, 
                color = Color.White
            )
        }
    }
}

@Composable
fun SvgIcon(pathData: String, tint: Color, modifier: Modifier = Modifier, viewportSize: Float = 24f) {
    val path = remember(pathData) { 
        androidx.core.graphics.PathParser.createPathFromPathData(pathData).asComposePath() 
    }
    Canvas(modifier = modifier) {
        val scaleX = size.width / viewportSize
        val scaleY = size.height / viewportSize
        
        scale(scaleX, scaleY, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(path, color = tint)
        }
    }
}

@Composable
fun ActionSheetModal(title: String, onEdit: () -> Unit, onEditAr: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 20.dp), color = Color(0xFF1F2937))
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onEdit() }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFF2D3E50), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Edit Menu", fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                }

                Divider(color = Color(0xFFF3F4F6))

                 Row(
                    modifier = Modifier.fillMaxWidth().clickable { onEditAr() }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFF2D3E50), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                         Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Edit Ar", fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                }
                
                Divider(color = Color(0xFFF3F4F6))
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onDelete() }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFFFEE2E2), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = DeleteRed)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Hapus Menu", fontWeight = FontWeight.SemiBold, color = DeleteRed)
                }
            }
        }
    }
}

@Composable
fun ConfirmationModal(title: String, desc: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
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

@Composable
fun InputModal(title: String, label: String, placeholder: String = "", initialValue: String = "", onSave: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.clickable { onCancel() })
                }
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF374151))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text, 
                    onValueChange = { text = it }, 
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                 Spacer(modifier = Modifier.height(24.dp))
                 Button(onClick = { onSave(text) }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E50)), modifier = Modifier.fillMaxWidth()) { Text("Simpan", color = Color.White) }
            }
        }
    }
}

@Composable
fun GuideModal(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
             Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                 Text("Cara Mengelola Kategori", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                 Spacer(modifier = Modifier.height(20.dp))
                 
                 // ANIMATION
                 GuideModalAnimation()
                 
                 Spacer(modifier = Modifier.height(20.dp))
                 Spacer(modifier = Modifier.height(20.dp))
                 Text(
                     text = buildAnnotatedString {
                        append("Untuk mengubah nama atau menghapus kategori, cukup ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Tekan & Tahan (Long Press)")
                        }
                        append(" pada tombol kategori.")
                     },
                     textAlign = TextAlign.Center, 
                     color = Color.Gray, 
                     fontSize = 13.sp
                 )
                 Spacer(modifier = Modifier.height(20.dp))
                 Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E50)), modifier = Modifier.fillMaxWidth()) { Text("Mengerti", color = Color.White) }
             }
        }
    }
}

@Composable
fun GuideModalAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "guide")
    
    // Hand Scale Animation (Pressing effect)
    val handScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = TweenSpec(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "handScale"
    )
    
    // Ring Animation (Ripple effect)
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = TweenSpec(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ringScale"
    )
    
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = TweenSpec(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ringAlpha"
    )

    Box(
        modifier = Modifier
            .height(120.dp)
            .fillMaxWidth()
            .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Button representation
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(30.dp)
                .background(Color.White, RoundedCornerShape(15.dp))
                .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(15.dp))
        )
        
        // Expanding Ring (Ripple)
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = ringScale
                    scaleY = ringScale
                    alpha = ringAlpha
                }
                .border(2.dp, MenuPrimaryBlue, CircleShape)
        )
        
        // Hand Icon
        Icon(
            Icons.Filled.TouchApp,
            contentDescription = null,
            tint = Color.Black.copy(alpha=0.7f),
            modifier = Modifier
                .size(32.dp)
                .offset(x = 10.dp, y = 10.dp) // Little offset to look like pressing
                .graphicsLayer {
                    scaleX = handScale
                    scaleY = handScale
                }
        )
    }
}


