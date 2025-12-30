package com.example.kasir.ui.banner

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.kasir.data.model.Banner
import com.example.kasir.viewmodel.BannerViewModel
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
fun BannerListScreen(
    viewModel: BannerViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Banner) -> Unit
) {
    val banners by viewModel.banners.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf<Banner?>(null) }
    var showInfoModal by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(BannerBg)) {
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
                Row(verticalAlignment = Alignment.Top) {
                    Text("ℹ️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Tekan & geser ikon titik-titik untuk mengubah urutan slide. (Klik untuk info)",
                        color = InfoText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Banner List
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(banners) { banner ->
                    BannerCard(
                        banner = banner,
                        onToggle = {
                             viewModel.saveBanner(
                                context = context, 
                                id = banner.id,
                                title = banner.title,
                                subtitle = banner.subtitle,
                                highlightText = banner.highlightText,
                                isActive = !banner.isActive
                            )
                        },
                        onEdit = { onNavigateToEdit(banner) },
                        onDelete = { showDeleteConfirm = banner }
                    )
                }
            }
        }
        
        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 20.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(PrimaryYellow)
                .clickable { onNavigateToAdd() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
              Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF1A2B48))
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
}

@Composable
fun BannerCard(
    banner: Banner,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    val model = if (banner.image.startsWith("http")) banner.image else "http://192.168.1.6:3000${banner.image}"
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
                        if (banner.isActive) "Status: Aktif" else "Status: Nonaktif",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (banner.isActive) StatusGreen else StatusGray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Switch(
                        checked = banner.isActive,
                        onCheckedChange = { onToggle() },
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
                     Button(onClick = onCancel, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)), modifier = Modifier.weight(1f)) { Text("Batal") }
                     Button(onClick = onConfirm, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = DeleteRed), modifier = Modifier.weight(1f)) { Text("Hapus") }
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
                 Icon(painter = painterResource(android.R.drawable.ic_dialog_info), contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(48.dp))
                 Spacer(modifier = Modifier.height(15.dp))
                 Text("Pengaturan Slide", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                 Spacer(modifier = Modifier.height(12.dp))
                 Text("Urutan banner di halaman ini menentukan urutan tampilan banner di aplikasi pelanggan.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color(0xFF6B7280), fontSize = 13.sp)
                 Spacer(modifier = Modifier.height(20.dp))
                 Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue), modifier = Modifier.fillMaxWidth()) { Text("Mengerti") }
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
                modifier = Modifier.fillMaxWidth().padding(15.dp).border(0.dp, Color.Transparent),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                            val model = if (initialBanner.image.startsWith("http")) initialBanner.image else "http://192.168.1.6:3000${initialBanner.image}"
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
                              val model = if (initialBanner.image.startsWith("http")) initialBanner.image else "http://192.168.1.6:3000${initialBanner.image}"
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

                BannerInputField("Judul Utama", "Contoh: Paket Hemat", bannerTitle) { bannerTitle = it }
                BannerInputField("Sub-judul", "Contoh: Nasi + Ayam", bannerDesc) { bannerDesc = it }
                BannerInputField("Teks Promo (Highlight Kuning)", "Contoh: 20% OFF", bannerPromo) { bannerPromo = it }
                
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        viewModel.saveBanner(
                            context = context,
                            id = if (initialBanner?.id != 0 && initialBanner?.id != null) initialBanner.id else null,
                            title = bannerTitle,
                            subtitle = bannerDesc,
                            highlightText = bannerPromo,
                            isActive = initialBanner?.isActive ?: true
                        )
                         onSave()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(if (title.contains("Edit")) "Simpan Perubahan" else "Terbitkan Banner", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun BannerInputField(label: String, placeholder: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFFD1D5DB)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )
    }
}
