package com.example.kasir.ui.menu

import android.net.Uri
import android.webkit.WebSettings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Info
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.*
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import com.example.kasir.data.model.Product
import com.example.kasir.viewmodel.MenuViewModel
import com.example.kasir.data.model.ArAsset
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.utils.FileUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EditArScreen(
    product: Product,
    viewModel: MenuViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            val window = (view.context as Activity).window
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)

            // Save original status bar color (transparent from enableEdgeToEdge)
            val originalStatusBarColor = window.statusBarColor

            // ON ENTER: Force dark status bar background + white icons
            // This overrides the transparent status bar from enableEdgeToEdge(),
            // bypassing nested Scaffold containerColor conflicts.
            window.statusBarColor = android.graphics.Color.parseColor("#1E2A38")
            controller.isAppearanceLightStatusBars = false // White Icons

            onDispose {
                // ON EXIT: Restore original transparent status bar + black icons
                window.statusBarColor = originalStatusBarColor
                controller.isAppearanceLightStatusBars = true
            }
        }
    }

    var showInfoDialog by remember { mutableStateOf(false) }
    
    var isArActive by remember { mutableStateOf(product.isArActive) }
    var selectedModelUrl by remember { mutableStateOf(product.ar3dModel) }

    // Fix Bug 1: Sync local state when product updates (e.g. returning from preview)
    LaunchedEffect(product) {
        isArActive = product.isArActive
        selectedModelUrl = product.ar3dModel
    }
    
    var assets by remember { mutableStateOf<List<ArAsset>>(emptyList()) }
    var isLoadingAssets by remember { mutableStateOf(true) }
    
    // Replacement State
    var assetToReplace by remember { mutableStateOf<ArAsset?>(null) }

    // Upload Progress State
    var uploadProgress by remember { mutableStateOf(0f) }
    var isUploading by remember { mutableStateOf(false) }

    // Preview State
    var previewModelUrl by remember { mutableStateOf<String?>(null) }

    // Fetch Assets on Start
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.instance.getArAssets()
            if (response.success) {
                assets = response.data
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load assets: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoadingAssets = false
        }
    }

    // Split Assets based on FLAG (Robust)
    val defaultAssets = assets.filter { it.isDefault }
    val customAssets = assets.filter { !it.isDefault }

    // Upload Logic
    // Upload Logic
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isUploading = true
                uploadProgress = 0f
                var tempFile: java.io.File? = null
                try {
                    val file = FileUtils.getFileFromUri(context, uri)
                    tempFile = file // TRACK FOR DELETION
                    if (file != null) {
                        // 1. Strict Format Check
                        if (!file.name.endsWith(".glb", ignoreCase = true)) {
                             Toast.makeText(context, "Format salah! Hanya menerima .glb", Toast.LENGTH_SHORT).show()
                             isUploading = false
                             tempFile?.delete() // Cleanup on failure
                             return@launch
                        }

                        // 2. Strict Size Check (< 40MB)
                        val sizeInMb = file.length() / (1024 * 1024)
                        if (sizeInMb > 40) {
                             Toast.makeText(context, "File terlalu besar (Max 40MB)", Toast.LENGTH_SHORT).show()
                             isUploading = false
                             tempFile?.delete() // Cleanup on failure
                             return@launch
                        }

                        // Create Progress Request Body
                        val requestFile = com.example.kasir.utils.ProgressRequestBody(
                            file, 
                            "model/gltf-binary".toMediaTypeOrNull()
                        ) { progress ->
                            uploadProgress = progress
                        }

                        val body = MultipartBody.Part.createFormData("model", file.name, requestFile)
                        
                        // Use context.IO for upload if possible, but Retrofit is main-safe usually
                        // We run this in IO dispatcher to be safe with file reading in ProgressRequestBody
                        val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                             RetrofitClient.instance.uploadArAsset(body)
                        }

                        if (response.success && response.data != null) {
                            // Add to list and select it
                            val newAsset = response.data
                            
                            // IF REPLACING: Delete the old one
                            val oldAsset = assetToReplace
                            if (oldAsset != null && oldAsset.id != null) {
                                try {
                                    val deleteRes = RetrofitClient.instance.deleteArAsset(oldAsset.id)
                                    if (deleteRes.success) {
                                        // Filter out old asset
                                        assets = assets.filter { it.id != oldAsset.id } + newAsset
                                        Toast.makeText(context, "File diganti!", Toast.LENGTH_SHORT).show()
                                    } else {
                                         // Failed to delete old, but new is uploaded. Just add new.
                                         assets = assets + newAsset
                                         Toast.makeText(context, "Upload sukses, gagal hapus lama", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    assets = assets + newAsset
                                    // Ignore delete error
                                }
                                // Reset replace state
                                assetToReplace = null
                            } else {
                                // Normal Upload
                                assets = assets + newAsset
                                Toast.makeText(context, "Upload Success!", Toast.LENGTH_SHORT).show()
                            }
                            // FIX: Disable Auto-Select. User stays on current selection.
                        } else {
                            Toast.makeText(context, "Upload Failed: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error uploading: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploading = false
                    uploadProgress = 0f
                    tempFile?.delete() // DELETE TEMP FILE TO PREVENT EXHAUSTION DOS
                }
            }
        }
    }

    // Delete Logic
    val deleteAsset = { asset: ArAsset ->
        scope.launch {
            val assetId = asset.id
            if (assetId == null) {
                Toast.makeText(context, "Cannot delete default asset", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                // Confirm Dialog Logic could be here, but for now we execute
                val response = RetrofitClient.instance.deleteArAsset(assetId)
                if (response.success) {
                    assets = assets.filter { it.id != assetId }
                    if (selectedModelUrl == asset.url) {
                        selectedModelUrl = null // Deselect if deleted
                    }
                    Toast.makeText(context, "Asset deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to delete: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper to launch Browser (Backend HTML Version)
    val launchArInBrowser = { modelUrl: String ->
        try {
            val encodedModelUrl = java.net.URLEncoder.encode(modelUrl, "UTF-8")
            
            // USE BACKEND URL
            val apiBase = RetrofitClient.BASE_URL 
            val webBase = if (apiBase.contains("/api/")) apiBase.replace("/api/", "") else apiBase
            val cleanWebBase = if (webBase.endsWith("/")) webBase else "$webBase/"
            
            // Construct Target
            val targetUrl = "${cleanWebBase}ar_view.html?src=$encodedModelUrl"
            
            // LAUNCH CUSTOM WEBVIEW ACTIVITY (Fake Native)
            val intent = android.content.Intent(context, com.example.kasir.ui.ar.ArViewActivity::class.java)
            intent.putExtra("KEY_URL", targetUrl)
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka AR: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ROOT: Manual Column layout (bypasses nested Scaffold inset issue)
    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6F8))) {
        // Manual Header (same pattern as Dashboard/TableScreen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2A38))
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("AR Experience", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
        }
                // ... (UI Content same as before)
                // Helper to Auto-Save
                val saveChanges = { newIsArActive: Boolean, newModelUrl: String? ->
                    val updatedProduct = product.copy(
                        isArActive = newIsArActive,
                        ar3dModel = newModelUrl
                    )
                    // Pass null for imageUri/context as we are not updating the product image
                    viewModel.updateProduct(product.id, updatedProduct, null, context)
                }

                // 1. Activation Switch
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp), 
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aktifkan Fitur AR", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E2A38))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tampilkan model 3D di menu ini", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = Color(0xFF1E2A38),
                                    modifier = Modifier.size(18.dp).clickable { showInfoDialog = true }
                                )
                            }
                        }
                        Switch(
                            checked = isArActive, 
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (selectedModelUrl.isNullOrEmpty()) {
                                        Toast.makeText(context, "Pilih model 3D terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                        isArActive = false
                                        saveChanges(false, selectedModelUrl) // Ensure sync
                                    } else {
                                        isArActive = true
                                        saveChanges(true, selectedModelUrl) // Auto-save ON
                                    }
                                } else {
                                    isArActive = false
                                    saveChanges(false, selectedModelUrl) // Auto-save OFF
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2ECC71)
                            )
                        )
                    }
                }

                if (isLoadingAssets) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1E2A38))
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Header
                        item(span = { GridItemSpan(2) }) {
                             Text("Aset Default", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                        }

                        items(defaultAssets) { asset ->
                            ArAssetItem(
                                asset = asset,
                                isSelected = selectedModelUrl == asset.url,
                                onClick = { 
                                    if (isArActive) {
                                        Toast.makeText(context, "Nonaktifkan AR dulu untuk mengganti aset!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        selectedModelUrl = asset.url
                                        saveChanges(false, asset.url)
                                    }
                                },
                                onLongClick = { launchArInBrowser(asset.url) } // LAUNCH BROWSER
                            )
                        }
                        
                         // Divider
                        item(span = { GridItemSpan(2) }) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFE0E0E0))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Koleksi Saya", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                            }
                        }
                        
                         // Upload Btn (HIDDEN IF LIMIT REACHED)
                         if (customAssets.size < 2) {
                             item {
                                Column {
                                    Card(
                                        modifier = Modifier
                                            .height(140.dp)
                                            .fillMaxWidth()
                                            .clickable(enabled = !isUploading) { 
                                                assetToReplace = null // Ensure normal upload mode
                                                launcher.launch("*/*") 
                                            }, 
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f).let { Color.Gray }) 
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Gray, modifier = Modifier.size(32.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Upload .glb", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                    
                                    // HELPER TEXT BELOW CARD
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Ukuran maks: 40MB\nFormat: .glb saja",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        items(customAssets) { asset ->
                            ArAssetItem(
                                asset = asset,
                                isSelected = selectedModelUrl == asset.url,
                                onClick = { 
                                    if (isArActive) {
                                         Toast.makeText(context, "Nonaktifkan AR dulu untuk mengganti aset!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        selectedModelUrl = asset.url
                                        saveChanges(false, asset.url)
                                    }
                                },
                                onLongClick = { launchArInBrowser(asset.url) },
                                onDelete = { deleteAsset(asset) },
                                onReplace = { 
                                    // Set target for replacement
                                    assetToReplace = asset
                                    launcher.launch("*/*") 
                                }
                            )
                        }
                    }
                }
        } // end Column

        // GLOBAL UPLOAD PROGRESS DIALOG
        if (isUploading) {
            Dialog(onDismissRequest = {}) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { uploadProgress },
                            color = Color(0xFF1E2A38),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (assetToReplace != null) "Mengganti File..." else "Mengupload File...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E2A38)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(uploadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
        // INFO POPUP DIALOG
        if (showInfoDialog) {
            ArInfoDialog(onDismiss = { showInfoDialog = false })
        }
    }
}

@Composable
fun ArInfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title on Top
                Text("Cara Pakai AR", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2A38))
                Spacer(modifier = Modifier.height(20.dp))
                
                // Animation Area (Rectangular like MenuScreen)
                LongPressAnimation()
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Rich Text
                val infoText = androidx.compose.ui.text.buildAnnotatedString {
                    append("Ingin melihat ")
                    withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Preview 3D? ")
                    }
                    append("Cukup ")
                    withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Tahan (Hold) ")
                    }
                    append("kartu aset.\n\nAset yang terpilih akan otomatis ")
                    withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("ditampilkan dalam AR ")
                    }
                    append("pada aplikasi Pembeli.")
                }
                
                Text(
                    text = infoText,
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2A38)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mengerti", color = Color.White) // WHITE TEXT
                }
            }
        }
    }
}

@Composable
fun LongPressAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "press")
    
    // Hand Scale
    val handScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handScale"
    )
    
    // Ripple Ring
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )
    
    // Container (Rectangular Grey Bg)
    Box(
        modifier = Modifier
            .height(140.dp)
            .fillMaxWidth()
            .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Target Card (AR Asset Representation)
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(80.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
        )
        
        // Expansion Ring (Ripple)
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = ringScale
                    scaleY = ringScale
                    alpha = ringAlpha
                }
                .border(2.dp, Color(0xFF1E2A38), CircleShape)
        )

        // Hand Icon
        Icon(
            imageVector = Icons.Filled.TouchApp,
            contentDescription = "Hold",
            tint = Color(0xFF1E2A38),
            modifier = Modifier
                .size(32.dp)
                .offset(x = 8.dp, y = 8.dp)
                .graphicsLayer {
                    scaleX = handScale
                    scaleY = handScale
                }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ArAssetItem(
    asset: ArAsset, 
    isSelected: Boolean, 
    onClick: () -> Unit, 
    onLongClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onReplace: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .height(140.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color(0xFF2ECC71) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Visual Preview
            Column(
                modifier = Modifier.align(Alignment.Center).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 3D Model Representation
                Box(
                    modifier = Modifier.size(60.dp).background(Color(0xFFE0F2FE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "3D Model",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = asset.name.replace(".glb", "", ignoreCase = true),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color(0xFF1E2A38),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Selection Indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF2ECC71),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(Color.White, CircleShape)
                )
            }

            // More Options (Only if custom asset i.e. onDelete is not null)
            if (onDelete != null) {
                Box(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color.Gray
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ganti File") },
                            onClick = { 
                                showMenu = false
                                onReplace?.invoke() 
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus", color = Color.Red) },
                            onClick = { 
                                showMenu = false
                                onDelete.invoke() 
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                        )
                    }
                }
            }
        }
    }
}

// ArPreviewOverlay removed - using External Browser Intent for better WebXR support

