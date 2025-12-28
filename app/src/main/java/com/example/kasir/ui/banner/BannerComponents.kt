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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kasir.MenuItem
import com.example.kasir.MenuScreen
import com.example.kasir.R

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

data class BannerItem(
    val id: String,
    val title: String,
    val description: String,
    val price: String, // Or Promo text
    val isActive: Boolean
)

// Dummy Data
val sampleBanners = listOf(
    BannerItem("1", "Paket Mantap", "Nasi Katsu", "Rp 15.000", true),
    BannerItem("2", "Diskon Kopi", "Hari Ini", "20% OFF", true),
    BannerItem("3", "Promo Jumat", "Berkah", "Buy 2 Get 1", false)
)

@Composable
fun BannerListScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (BannerItem) -> Unit
) {
    var banners by remember { mutableStateOf(sampleBanners) }
    var showDeleteConfirm by remember { mutableStateOf<BannerItem?>(null) }
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
                    Text("ℹ️", fontSize = 14.sp) // Replace with icon if preferred
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
                            banners = banners.map { if (it.id == banner.id) it.copy(isActive = !it.isActive) else it }
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
                .padding(bottom = 100.dp, end = 20.dp) // Adjusted to match MenuScreen default FAB position
                .size(56.dp)
                .clip(CircleShape)
                .background(PrimaryYellow)
                .clickable { onNavigateToAdd() }
                .padding(16.dp), // Icon padding
            contentAlignment = Alignment.Center
        ) {
              Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF1A2B48))
        }

        // --- Modals ---
        if (showDeleteConfirm != null) {
            BannerDeleteModal(
                onConfirm = {
                    banners = banners.filter { it.id != showDeleteConfirm!!.id }
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
    banner: BannerItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().then(if (!banner.isActive) Modifier.background(Color.Transparent) else Modifier) // Opacity handled via content color/alpha usually
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
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray)) // Image Placeholder
                Spacer(modifier = Modifier.width(15.dp))
                Column {
                    Text(banner.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(banner.description, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(banner.price, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryYellow)
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
                    Icon(painter = painterResource(android.R.drawable.ic_menu_sort_by_size), contentDescription = "Drag", tint = Color.Gray, modifier = Modifier.size(20.dp)) // Mock drag icon
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
                 Text("Menghapus banner akan menghilangkannya secara permanen dari daftar promosi aplikasi. Tindakan ini tidak dapat dibatalkan.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color(0xFF6B7280), fontSize = 13.sp)
                 Spacer(modifier = Modifier.height(24.dp))
                 Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                     Button(onClick = onCancel, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)), modifier = Modifier.weight(1f)) { Text("Batal") }
                     Button(onClick = onConfirm, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = DeleteRed), modifier = Modifier.weight(1f)) { Text("Ya, Hapus") }
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
                 Text("Urutan banner di halaman ini menentukan urutan tampilan banner di aplikasi pelanggan. Banner yang berada paling atas akan muncul pertama kali.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color(0xFF6B7280), fontSize = 13.sp)
                 Spacer(modifier = Modifier.height(20.dp))
                 Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue), modifier = Modifier.fillMaxWidth()) { Text("Mengerti") }
             }
        }
    }
}

@Composable
fun BannerFormScreen(
    title: String,
    initialBanner: BannerItem? = null,
    onBack: () -> Unit,
    onSave: (BannerItem) -> Unit
) {
    var bannerTitle by remember { mutableStateOf(initialBanner?.title ?: "") }
    var bannerDesc by remember { mutableStateOf(initialBanner?.description ?: "") }
    var bannerPromo by remember { mutableStateOf(initialBanner?.price ?: "") }
    
    // Preview uses live updates
    val previewBanner = BannerItem(
        id = "preview", 
        title = bannerTitle.ifEmpty { "Judul Promo" }, 
        description = bannerDesc.ifEmpty { "Keterangan singkat" }, 
        price = bannerPromo.ifEmpty { "Info Diskon" }, 
        isActive = true
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(15.dp).border(0.dp, Color.Transparent), // Border bottom manually
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
                       // Placeholder Image or Icon
                       Icon(painter = painterResource(android.R.drawable.ic_menu_gallery), contentDescription = null, tint = Color.White.copy(alpha=0.3f))
                   }
                   Spacer(modifier = Modifier.width(16.dp))
                   Column {
                       Text(previewBanner.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                       Text(previewBanner.description, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                       Text(previewBanner.price, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryYellow)
                   }
                }
                
                Spacer(modifier = Modifier.height(30.dp))
                
                // Form
                if (title.contains("Tambah")) Text("Detail Banner", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=10.dp))
                
                // --- Form Fields --- (Simplified components)
                // Photo Upload (Mock)
                if (title.contains("Edit")) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                        .padding(20.dp)) {
                         Text("Foto Banner", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                         Spacer(modifier = Modifier.height(8.dp))
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Box(modifier = Modifier.size(64.dp).background(Color.LightGray, RoundedCornerShape(8.dp)))
                             Spacer(modifier = Modifier.width(16.dp))
                             Button(
                                 onClick = {}, 
                                 colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF9FAFB), contentColor = Color.Black),
                                 border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                             ) {
                                 Text("Ganti Foto")
                             }
                         }
                    }
                } else {
                     // Upload Area for Add
                     Column(modifier = Modifier.padding(bottom = 20.dp)) {
                         Text("Foto Banner", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                         Box(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .padding(top = 8.dp)
                                 .border(2.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp)) // Dashed if possible
                                 .padding(30.dp),
                             contentAlignment = Alignment.Center
                         ) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                 Icon(painter = painterResource(android.R.drawable.ic_menu_upload), contentDescription = null, tint = Color.Gray)
                                 Text("Upload Foto Banner", fontWeight = FontWeight.SemiBold)
                                 Text("PNG, JPG (Max 2MB)", fontSize = 11.sp, color = Color.Gray)
                             }
                         }
                     }
                }

                BannerInputField("Judul Utama", "Contoh: Paket Hemat", bannerTitle) { bannerTitle = it }
                BannerInputField("Sub-judul", "Contoh: Nasi + Ayam", bannerDesc) { bannerDesc = it }
                BannerInputField("Teks Promo (Highlight Kuning)", "Contoh: 20% OFF", bannerPromo) { bannerPromo = it }
            }
        }
        
        // Bottom Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, Color(0xFFEEEEEE))
                .padding(20.dp)
        ) {
            Button(
                onClick = { onSave(BannerItem(initialBanner?.id ?: "new", bannerTitle, bannerDesc, bannerPromo, true)) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (title.contains("Edit")) "Simpan Perubahan" else "Terbitkan Banner", fontWeight = FontWeight.Bold)
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
