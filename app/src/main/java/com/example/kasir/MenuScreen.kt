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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kasir.ui.theme.KasirTheme
import com.example.kasir.ui.banner.BannerListScreen
import com.example.kasir.ui.banner.BannerFormScreen
import com.example.kasir.ui.banner.BannerItem
import com.example.kasir.ui.menu.MenuFormScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.viewmodel.MenuViewModel
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
    val price: String,
    val isActive: Boolean
)

val initialMenuItems = listOf(
    MenuItem("1", "Nasi Goreng Special", "makanan", "Makanan Utama", "Rp 15.000", true),
    MenuItem("2", "Mie Goreng Jawa", "makanan", "Makanan Utama", "Rp 12.000", true),
    MenuItem("3", "Ayam Bakar Madu", "makanan", "Makanan Utama", "Rp 18.000", false),
    MenuItem("4", "Es Teh Manis", "minuman", "Minuman", "Rp 5.000", true),
    MenuItem("5", "Es Jeruk Peras", "minuman", "Minuman", "Rp 8.000", true),
    MenuItem("6", "Sate Ayam 10 Tusuk", "makanan", "Makanan Utama", "Rp 20.000", true),
    MenuItem("7", "Pisang Goreng Krispy", "cemilan", "Cemilan", "Rp 10.000", true),
    MenuItem("8", "Kopi Susu Gula Aren", "minuman", "Minuman", "Rp 12.000", true)
)

@Composable
fun MenuScreen(onNavigate: (String) -> Unit) {
    val viewModel: MenuViewModel = viewModel()
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    // Initialize with empty list, data will come from VM
    var menuList by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isFabExpanded by remember { mutableStateOf(false) }

    // Sync products from VM to local menuList (UI Model)
    LaunchedEffect(products) {
        menuList = products.map { product ->
            MenuItem(
                id = product.id.toString(),
                name = product.name,
                category = product.category.lowercase(),
                categoryDisplay = product.category,
                price = "Rp ${product.price}",
                isActive = product.isActive
            )
        }
    }
    
    // Modals & Sheets State
    var showActionSheet by remember { mutableStateOf<MenuItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<MenuItem?>(null) }
    var showAddCategoryModal by remember { mutableStateOf(false) }
    var showEditCategoryModal by remember { mutableStateOf<String?>(null) } // category id/name
    var showGuideModal by remember { mutableStateOf(false) }
    
    var activeTab by remember { mutableStateOf("menu") } // "menu" or "banner"
    var bannerScreenState by remember { mutableStateOf("list") } // "list", "add", "edit"
    var menuScreenState by remember { mutableStateOf("list") } // "list", "add_product", "edit_product"
    var selectedBannerToEdit by remember { mutableStateOf<BannerItem?>(null) }
    var selectedMenuToEdit by remember { mutableStateOf<MenuItem?>(null) }
    
    // Filtering (Menu)
    val filteredItems = menuList.filter { item ->
        (selectedCategory == "all" || item.category == selectedCategory) &&
        (searchQuery.isEmpty() || item.name.contains(searchQuery, ignoreCase = true))
    }

    Box(modifier = Modifier.fillMaxSize().background(MenuBg)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MenuPrimaryBlue)
            }
        } else if (errorMessage != null) {
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
            if (bannerScreenState == "list" && menuScreenState == "list") {
                // Header (Shared)
                Column(modifier = Modifier.background(Color.White).padding(top = 24.dp, start = 20.dp, end = 20.dp)) {
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
                if (menuScreenState == "list") {
                    // --- EXISTING MENU CONTENT ---
                    // Info Alert
                    Surface(
                        color = InfoBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { showGuideModal = true }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Text("ℹ️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Tekan & tahan tombol kategori untuk mengubah atau menghapus. (Klik untuk demo)",
                                color = InfoText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Search & Filter
                    Column(modifier = Modifier.background(Color.White).padding(bottom = 10.dp)) {
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
                            val categories = listOf(
                                "all" to "Semua", 
                                "makanan" to "Makanan Berat", 
                                "minuman" to "Minuman", 
                                "cemilan" to "Cemilan", 
                                "paket" to "Paket Hemat"
                            )
                            items(categories) { (id, label) ->
                                FilterChipCustom(
                                    label = label,
                                    isActive = selectedCategory == id,
                                    onClick = { selectedCategory = id },
                                    onLongClick = { if(id != "all") showEditCategoryModal = label }
                                )
                            }
                        }
                    }

                    // Product List
                    LazyColumn(
                        contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        items(filteredItems) { item ->
                            MenuItemRow(
                                item = item,
                                onToggle = { 
                                    menuList = menuList.map { if (it.id == item.id) it.copy(isActive = !it.isActive) else it }
                                },
                                onOptionClick = { showActionSheet = item }
                            )
                            Divider(color = Color(0xFFEEEEEE))
                        }
                    }
                } else if (menuScreenState == "add_product") {
                    // --- ADD PRODUCT FORM ---
                    MenuFormScreen(
                        onBack = { menuScreenState = "list" },
                        onSave = { name, category, price, desc, isActive ->
                            // Logic to save
                            menuScreenState = "list" 
                        }
                    )
                } else if (menuScreenState == "edit_product") {
                    // --- EDIT PRODUCT FORM ---
                    MenuFormScreen(
                        initialName = selectedMenuToEdit?.name ?: "",
                        initialCategory = selectedMenuToEdit?.categoryDisplay ?: "",
                        initialPrice = selectedMenuToEdit?.price ?: "",
                        initialDescription = "Menu spesial dengan bumbu rahasia yang lezat dan menggugah selera.", // Default from HTML
                        initialIsActive = selectedMenuToEdit?.isActive ?: true,
                        isEditMode = true,
                        onBack = { 
                            menuScreenState = "list"
                            selectedMenuToEdit = null
                        },
                        onSave = { name, category, price, desc, isActive ->
                            // Update logic (mock)
                            val priceFmt = if(price.startsWith("Rp")) price else "Rp $price"
                            // Here we would find and replace in menuList, but for now just returning to list
                            menuScreenState = "list"
                            selectedMenuToEdit = null
                        }
                    )
                }
            } else {
                // --- BANNER CONTENT ---
                when (bannerScreenState) {
                    "list" -> {
                        BannerListScreen(
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
                            onBack = { bannerScreenState = "list" },
                            onSave = { 
                                // Logic to save new banner
                                bannerScreenState = "list" 
                            }
                        )
                    }
                    "edit" -> {
                        BannerFormScreen(
                            title = "Edit Banner Promosi",
                            initialBanner = selectedBannerToEdit,
                            onBack = { 
                                bannerScreenState = "list"
                                selectedBannerToEdit = null
                            },
                            onSave = { 
                                // Logic to update banner
                                bannerScreenState = "list" 
                                selectedBannerToEdit = null
                            }
                        )
                    }
                }
            }
        }
            }

        // Floating Action Button (FAB) Area - ONLY for Menu Tab & List View
        if (activeTab == "menu" && menuScreenState == "list") {
            Box(modifier = Modifier.fillMaxSize()) {
                // Overlay
                 AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f)).clickable { isFabExpanded = false })
                }
    
                // FAB Items
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
                        FabSubButton("Tambah Produk", "🍳") { 
                           isFabExpanded = false
                           menuScreenState = "add_product"
                        }
                    }
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                         FabSubButton("Tambah Kategori", "📁") { 
                             isFabExpanded = false
                             showAddCategoryModal = true 
                         }
                    }
                }
    
                // Main FAB
                val rotation by animateFloatAsState(if (isFabExpanded) 45f else 0f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 100.dp, end = 20.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MenuPrimaryYellow)
                        .clickable { isFabExpanded = !isFabExpanded }
                        .shadow(elevation = 4.dp, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = MenuTextDark, modifier = Modifier.rotate(rotation))
                }
            }
        }

        // Bottom Nav - Hide when in full screen forms
        if (bannerScreenState == "list" && menuScreenState == "list") {
            AppBottomNavigation(
                currentScreen = "menu",
                onNavigate = onNavigate,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // --- MODALS ---
        
        // Action Sheet (Mocked as bottom dialog for simplicity, or centered dialog)
        if (showActionSheet != null) {
            ActionSheetModal(
                title = "Opsi Menu: ${showActionSheet!!.name}",
                onEdit = { 
                    val item = showActionSheet
                    showActionSheet = null // Close sheet (so modal disappears)
                    selectedMenuToEdit = item
                    menuScreenState = "edit_product"
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
                    menuList = menuList.filter { it.id != showDeleteConfirm!!.id }
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
                    // Add category logic here
                    showAddCategoryModal = false 
                },
                onCancel = { showAddCategoryModal = false }
            )
        }
        
        if (showEditCategoryModal != null) {
             InputModal(
                title = "Edit Kategori",
                label = "Nama Kategori",
                initialValue = showEditCategoryModal!!,
                onSave = { showEditCategoryModal = null },
                onCancel = { showEditCategoryModal = null }
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

@Composable
fun MenuItemRow(item: MenuItem, onToggle: () -> Unit, onOptionClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .then(if (!item.isActive) Modifier.alpha(0.5f) else Modifier),
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
            Text("IMG", fontSize = 10.sp, color = Color.White)
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
fun FabSubButton(text: String, icon: String, onClick: () -> Unit) {
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
             Text(icon, fontSize = 18.sp)
        }
    }
}

@Composable
fun ActionSheetModal(title: String, onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onEdit() }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFF2D3E50), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Edit Menu", fontWeight = FontWeight.SemiBold)
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

@Composable
fun GuideModal(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
             Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                 Text("Cara Mengelola Kategori", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                 Spacer(modifier = Modifier.height(20.dp))
                 Box(modifier = Modifier.height(100.dp).fillMaxWidth().background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                     Text("Animation Placeholder")
                 }
                 Spacer(modifier = Modifier.height(20.dp))
                 Text("Untuk mengubah nama atau menghapus kategori, cukup Tekan & Tahan (Long Press) pada tombol kategori.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 13.sp)
                 Spacer(modifier = Modifier.height(20.dp))
                 Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E50)), modifier = Modifier.fillMaxWidth()) { Text("Mengerti") }
             }
        }
    }
}
