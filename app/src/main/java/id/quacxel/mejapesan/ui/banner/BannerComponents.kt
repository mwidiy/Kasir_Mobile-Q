package id.quacxel.mejapesan.ui.banner

import androidx.compose.foundation.background
import id.quacxel.mejapesan.BuildConfig
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.scale
import id.quacxel.mejapesan.data.model.Banner
import id.quacxel.mejapesan.viewmodel.BannerViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// --- COLORS ---
private val BannerBg = Color(0xFFF8F9FA)
private val CardDarkBg = Color(0xFF2D3E50)
private val StatusGreen = Color(0xFF2ECC71)
private val StatusGray = Color(0xFF9CA3AF)
private val InfoBg = Color(0xFFE0F2FE)
private val InfoText = Color(0xFF0369A1)
private val DeleteRed = Color(0xFFEF4444)
private val PrimaryBlue = Color(0xFF1E3A5F)
private val PrimaryYellow = Color(0xFFFDD85D)

@Composable
fun SvgIcon(pathData: String, tint: Color, modifier: Modifier = Modifier, viewportSize: Float = 24f) {
    val path = remember(pathData) { 
        androidx.core.graphics.PathParser.createPathFromPathData(pathData).asComposePath() 
    }
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val scaleX = size.width / viewportSize
        val scaleY = size.height / viewportSize
        
        scale(scaleX, scaleY, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(path, color = tint)
        }
    }
}

@Composable
fun BannerListScreen(
    viewModel: BannerViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Banner) -> Unit
) {
    val banners by viewModel.banners.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf<Banner?>(null) }
    var showInfoModal by remember { mutableStateOf(false) }

    // REFRESH ON ENTRY
    LaunchedEffect(Unit) {
        viewModel.fetchBanners()
    }

    val isLoading by viewModel.isLoading.collectAsState() // Added for Skeleton state

    Box(modifier = Modifier.fillMaxSize().background(BannerBg)) {
        // Only show full loading if strictly needed, otherwise trust local state
        // Skeleton block removed per Tahap 61
        
        Column(modifier = Modifier.fillMaxSize()) {
            // Info Alert
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(InfoBg)
                    .clickable { showInfoModal = true }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SvgIcon(
                        pathData = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z",
                        tint = InfoText,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Klik panel ini untuk informasi lengkap mengenai pengelolaan banner slide.",
                        color = InfoText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Banner List
            if (banners.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Belum ada Banner", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray)
                        Text("Buat banner promosi pertamamu!", fontSize = 14.sp, color = Color.LightGray)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(banners) { banner ->
                        BannerCard(
                            banner = banner,
                            onToggle = { onComplete ->
                                viewModel.toggleBannerStatus(banner = banner, onComplete = onComplete)
                            },
                            onEdit = { onNavigateToEdit(banner) },
                            onDelete = { showDeleteConfirm = banner }
                        )
                    }
                }
            }
        }
    }

        // --- Modals ---
        if (showDeleteConfirm != null) {
            BannerDeleteModal(
                onConfirm = {
                    viewModel.deleteBanner(showDeleteConfirm!!.id)
                    showDeleteConfirm = null
                },
                onCancel = { showDeleteConfirm = null }
            )
        }

        if (showInfoModal) {
            BannerInfoModal(onDismiss = { showInfoModal = false })
        }
    }

@Composable
fun BannerCard(
    banner: Banner,
    onToggle: (onComplete: () -> Unit) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // === ZERO-BOUNCE UI (Local Shadow State) ===
    var isLocalActive by remember(banner.id) { mutableStateOf(banner.isActive) }
    var isUpdating by remember(banner.id) { mutableStateOf(false) }
    
    // === HARD RATE LIMITING (Anti-Spam Bouncer) ===
    var clickTimestamps by remember { mutableStateOf(listOf<Long>()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Only accept Server's truth if we are NOT actively mutating (debouncing)
    LaunchedEffect(banner.isActive) {
        if (!isUpdating) {
            isLocalActive = banner.isActive
        }
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Part (Dark)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarkBg)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // IMAGE WITH COIL
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray)) {
                    val model = id.quacxel.mejapesan.utils.ImageUtils.getDynamicImageUrl(banner.image)
                    AsyncImage(
                        model = model,
                        contentDescription = banner.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = androidx.compose.ui.graphics.painter.ColorPainter(Color.Gray)
                    )
                }
                
                Spacer(modifier = Modifier.width(15.dp))
                Column {
                    Text(banner.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(banner.subtitle ?: "", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(banner.highlightText ?: "", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryYellow)
                }
            }
            
            // Bottom Part (White)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(android.R.drawable.ic_menu_sort_by_size), contentDescription = "Drag", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isLocalActive) "Status: Aktif" else "Status: Nonaktif",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isLocalActive) StatusGreen else StatusGray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // DECOUPLED SWITCH: Bound to Local State to prevent Server Race Conditions
                    val scope = rememberCoroutineScope()
                    Switch(
                        checked = isLocalActive,
                        onCheckedChange = { 
                            val currentTime = System.currentTimeMillis()
                            // Filter timestamps to only keep those within the last 2000ms
                            val recentClicks = clickTimestamps.filter { currentTime - it < 2000 }
                            
                            if (recentClicks.size >= 3) {
                                // SPAM DETECTED: Block action and warn user
                                android.widget.Toast.makeText(context, "Terlalu cepat! Tunggu sebentar ⏳", android.widget.Toast.LENGTH_SHORT).show()
                                clickTimestamps = recentClicks // Update memory
                                return@Switch
                            }
                            
                            // PASS THE BOUNCER: Record this click
                            clickTimestamps = recentClicks + currentTime
                            
                            isLocalActive = it // 1. Instant UI Change (Guaranteed No-Bounce)
                            isUpdating = true  // 2. Lock UI against Server refreshes
                            
                            // 3. Notify ViewModel to start Debounce, pass unlocking callback
                            onToggle {
                                isUpdating = false // 4. Unlocked ONLY when Server says "I'm Done"
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StatusGreen)
                    )
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp).clickable { onEdit() })
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DeleteRed, modifier = Modifier.size(20.dp).clickable { onDelete() })
                }
            }
        }
    }
}

@Composable
fun BannerDeleteModal(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
             Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                 Box(modifier = Modifier.size(64.dp).background(Color(0xFF1F2937), CircleShape), contentAlignment = Alignment.Center) {
                     Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                 }
                 Spacer(modifier = Modifier.height(20.dp))
                 Text("Hapus Banner Ini?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                 Spacer(modifier = Modifier.height(12.dp))
                 Text("Menghapus banner akan menghilangkannya secara permanen dari daftar promosi aplikasi.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color(0xFF6B7280), fontSize = 13.sp)
                 Spacer(modifier = Modifier.height(24.dp))
                 Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                     Button(onClick = onCancel, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)), modifier = Modifier.weight(1f)) { Text("Batal", color = Color.White) }
                     Button(onClick = onConfirm, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = DeleteRed), modifier = Modifier.weight(1f)) { Text("Hapus", color = Color.White) }
                 }
            }
        }
    }
}

@Composable
fun BannerInfoModal(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
             Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                 SvgIcon(
                    pathData = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(48.dp)
                 )
                 Spacer(modifier = Modifier.height(15.dp))
                 Text("Informasi Slide", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                 Spacer(modifier = Modifier.height(12.dp))
                 Text("Urutan banner di halaman ini menentukan urutan tampilan banner di aplikasi pelanggan.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color(0xFF6B7280), fontSize = 13.sp)
                 Spacer(modifier = Modifier.height(20.dp))
                 Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue), modifier = Modifier.fillMaxWidth()) { Text("Mengerti", color = Color.White) }
             }
        }
    }
}

@Composable
fun BannerFormScreen(
    title: String,
    initialBanner: Banner? = null,
    viewModel: BannerViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var bannerTitle by remember { mutableStateOf(initialBanner?.title ?: "") }
    var bannerDesc by remember { mutableStateOf(initialBanner?.subtitle ?: "") }
    var bannerPromo by remember { mutableStateOf(initialBanner?.highlightText ?: "") }
    var isSubmitting by remember { mutableStateOf(false) } // Instant Anti-Spam Lock
    val isLoading by viewModel.isLoading.collectAsState()

    // Unlock if the process finishes (e.g. error happened, so it didn't call onSuccess)
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isSubmitting = false
        }
    }
    
    val selectedImageUri = viewModel.selectedImageUri
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? -> 
        if(uri != null) viewModel.selectedImageUri = uri 
    }

    LaunchedEffect(initialBanner) {
        viewModel.selectedImageUri = null
    }

    val previewBanner = Banner(
        id = initialBanner?.id ?: 0, 
        title = bannerTitle.ifEmpty { "Judul Promo" }, 
        subtitle = bannerDesc.ifEmpty { "Keterangan singkat" }, 
        highlightText = bannerPromo.ifEmpty { "Info Diskon" }, 
        image = "", 
        isActive = true
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding() // Handled overlap
                    .padding(15.dp)
                    .border(0.dp, Color.Transparent),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1F2937))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
            }
            Divider()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(if(title.contains("Edit")) "Tampilan Preview" else "Preview Tampilan", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(bottom=10.dp))
                
                // Preview Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarkBg, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                   Box(modifier = Modifier.size(80.dp).background(Color.White.copy(alpha=0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                       if (selectedImageUri != null) {
                            AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                       } else if (initialBanner != null && initialBanner.image.isNotEmpty()) {
                            val model = id.quacxel.mejapesan.utils.ImageUtils.getDynamicImageUrl(initialBanner.image)
                            AsyncImage(model = model, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                       } else {
                            Icon(painter = painterResource(android.R.drawable.ic_menu_gallery), contentDescription = null, tint = Color.White.copy(alpha=0.3f))
                       }
                   }
                   Spacer(modifier = Modifier.width(16.dp))
                   Column {
                       Text(previewBanner.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                       Text(previewBanner.subtitle ?: "", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                       Text(previewBanner.highlightText ?: "", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryYellow)
                   }
                }
                
                Spacer(modifier = Modifier.height(30.dp))
                
                if (title.contains("Tambah")) Text("Detail Banner", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=10.dp))
                
                // Photo Upload
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                     Text("Foto Banner", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                     Box(
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(140.dp)
                             .padding(top = 8.dp)
                             .clip(RoundedCornerShape(12.dp))
                             .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
                             .clickable { launcher.launch("image/*") },
                         contentAlignment = Alignment.Center
                     ) {
                         if (selectedImageUri != null) {
                              AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                         } else if (initialBanner != null && initialBanner.image.isNotEmpty()) {
                              val model = if (initialBanner.image.startsWith("http")) initialBanner.image else "${BuildConfig.API_BASE_URL.removeSuffix("/")}${initialBanner.image}"
                              AsyncImage(model = model, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                              // Overlay hint to change
                              Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.3f)), contentAlignment=Alignment.Center) {
                                  Text("Ganti Foto", color = Color.White)
                              }
                         } else {
                              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                  Icon(painter = painterResource(android.R.drawable.ic_menu_upload), contentDescription = null, tint = Color.Gray)
                                  Text("Upload Foto Banner", fontWeight = FontWeight.SemiBold)
                                  Text("JPG/PNG", fontSize = 11.sp, color = Color.Gray)
                              }
                         }
                     }
                }

                // XSS, SQLi & Buffer Overflow Protection applied directly on Input
                BannerInputField("Judul Utama", "Contoh: Paket Hemat", bannerTitle, maxLength = 50) { 
                    bannerTitle = it.replace(Regex("[^a-zA-Z0-9 %!.,&#-]"), "")
                }
                BannerInputField("Sub-judul", "Contoh: Nasi + Ayam", bannerDesc, maxLength = 50) { 
                    bannerDesc = it.replace(Regex("[^a-zA-Z0-9 %!.,&#-]"), "")
                }
                BannerInputField("Teks Promo (Highlight Kuning)", "Contoh: 30% OFF", bannerPromo, maxLength = 20) { 
                    bannerPromo = it.replace(Regex("[^a-zA-Z0-9 %!.,&#-]"), "")
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                // Error & Loading UI
                val isLoading by viewModel.isLoading.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "", 
                        color = Color.Red, 
                        fontSize = 14.sp, 
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                val hasImage = selectedImageUri != null || (initialBanner != null && initialBanner.image.isNotEmpty())

                Button(
                    onClick = {
                        if (!isSubmitting && hasImage) {
                            isSubmitting = true // Lock the UI button instantly down to the millisecond
                            viewModel.saveBanner(
                                context = context,
                                id = if (initialBanner?.id != 0 && initialBanner?.id != null) initialBanner.id else null,
                                title = bannerTitle,
                                subtitle = bannerDesc,
                                highlightText = bannerPromo,
                                isActive = initialBanner?.isActive ?: true,
                                onSuccess = { onSave() } // Navigate purely on backend success, though UI reacts fast locally
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                         containerColor = if (!isSubmitting && hasImage) PrimaryBlue else Color.Gray,
                         disabledContainerColor = Color.Gray
                    ),
                    enabled = !isSubmitting && hasImage // Disable if submitting or image is missing
                ) {
                    if (isSubmitting || isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (title.contains("Edit")) "Simpan Perubahan" else "Terbitkan Banner", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(150.dp))
            }
        }
    }
}

@Composable
fun BannerInputField(label: String, placeholder: String, value: String, maxLength: Int = 100, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp), color = Color(0xFF374151))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            placeholder = { Text(placeholder, color = Color(0xFFD1D5DB)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )
    }
}

// --- BANNER SKELETON LOADING ---
@Composable
fun bannerShimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )

        val transition = androidx.compose.animation.core.rememberInfiniteTransition()
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing), 
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            )
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset.Zero
        )
    }
}

@Composable
fun BannerSkeletonLoading() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Mock Info Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .height(60.dp)
                .background(bannerShimmerBrush(), RoundedCornerShape(8.dp))
        )
        
        // Mock Cards
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            repeat(4) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Part
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2D3E50)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(80.dp).background(bannerShimmerBrush(), RoundedCornerShape(8.dp)))
                            Spacer(modifier = Modifier.width(15.dp))
                            Column {
                                Box(modifier = Modifier.width(150.dp).height(16.dp).background(bannerShimmerBrush(), RoundedCornerShape(4.dp)))
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(modifier = Modifier.width(100.dp).height(12.dp).background(bannerShimmerBrush(), RoundedCornerShape(4.dp)))
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(modifier = Modifier.width(80.dp).height(14.dp).background(bannerShimmerBrush(), RoundedCornerShape(4.dp)))
                            }
                        }
                        // Bottom Part
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(100.dp).height(20.dp).background(bannerShimmerBrush(), RoundedCornerShape(4.dp)))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.width(40.dp).height(20.dp).background(bannerShimmerBrush(), RoundedCornerShape(10.dp)))
                                Box(modifier = Modifier.size(20.dp).background(bannerShimmerBrush(), CircleShape))
                                Box(modifier = Modifier.size(20.dp).background(bannerShimmerBrush(), CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}
