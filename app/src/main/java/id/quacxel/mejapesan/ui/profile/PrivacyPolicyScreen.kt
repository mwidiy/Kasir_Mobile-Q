package id.quacxel.mejapesan.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ContactMail
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Re-use app colors for consistency
private val Navy = Color(0xFF2C3E50)
private val QuackYellow = Color(0xFFF7DC6F)
private val BackgroundLight = Color(0xFFF9FAFB)
private val GreenAccent = Color(0xFF22C55E)

// --- Data Model for Privacy Sections ---
private data class PrivacySection(
    val icon: ImageVector,
    val title: String,
    val contentParagraphs: List<String>,
    val bulletPoints: List<String> = emptyList(),
    val highlightNote: String? = null,
    val iconTint: Color = Navy
)

// --- Main Screen Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    val sections = remember { buildPrivacySections() }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kebijakan Privasi",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            AnimatedSectionItem(index = 0) {
                HeaderCard()
            }

            // Privacy Sections
            sections.forEachIndexed { index, section ->
                AnimatedSectionItem(index = index + 1) {
                    PrivacySectionCard(section = section)
                }
            }

            // Footer
            AnimatedSectionItem(index = sections.size + 1) {
                FooterCard()
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- Animated Wrapper for fade-in + slide-up ---
@Composable
private fun AnimatedSectionItem(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 80L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 400)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 400),
                    initialOffsetY = { it / 4 }
                )
    ) {
        content()
    }
}

// --- Header Card ---
@Composable
private fun HeaderCard() {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Navy),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = QuackYellow,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Qr Meja Pesan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Kebijakan Privasi Aplikasi",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = "Berlaku: 19 Maret 2026",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    labelColor = QuackYellow
                ),
                border = null
            )
        }
    }
}

// --- Section Card ---
@Composable
private fun PrivacySectionCard(section: PrivacySection) {
    OutlinedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = section.iconTint.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = null,
                            tint = section.iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Content Paragraphs
            section.contentParagraphs.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF475569),
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bullet Points
            if (section.bulletPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                section.bulletPoints.forEach { bullet ->
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Navy,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = bullet,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            // Highlight Note
            if (section.highlightNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GreenAccent.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "✅", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = section.highlightNote,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }
        }
    }
}

// --- Footer Card ---
@Composable
private fun FooterCard() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF1F5F9),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "© 2026 Qr Meja Pesan",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Terakhir diperbarui: 19 Maret 2026",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// --- Build All Privacy Sections ---
private fun buildPrivacySections(): List<PrivacySection> = listOf(
    PrivacySection(
        icon = Icons.Outlined.Info,
        title = "Pendahuluan",
        contentParagraphs = listOf(
            "Selamat datang di Qr Meja Pesan. Kami menghargai kepercayaan Anda dan berkomitmen untuk melindungi privasi serta keamanan data pribadi Anda.",
            "Kebijakan Privasi ini menjelaskan bagaimana kami mengumpulkan, menggunakan, menyimpan, dan melindungi informasi Anda saat menggunakan aplikasi Qr Meja Pesan.",
            "Dengan mengunduh, menginstal, atau menggunakan Aplikasi ini, Anda menyetujui praktik yang dijelaskan dalam Kebijakan Privasi ini."
        )
    ),
    PrivacySection(
        icon = Icons.Outlined.Storage,
        title = "Informasi yang Kami Kumpulkan",
        contentParagraphs = listOf(
            "Kami mengumpulkan informasi yang diperlukan untuk menyediakan layanan Point of Sale yang optimal:"
        ),
        bulletPoints = listOf(
            "Data Akun: Nama restoran, logo, informasi kontak, dan metode pembayaran yang Anda simpan di profil toko.",
            "Data Transaksi: Rincian pesanan, riwayat transaksi, jumlah pembayaran, dan metode pembayaran pelanggan.",
            "Data Menu: Nama, deskripsi, harga, kategori, dan gambar menu yang Anda daftarkan.",
            "Data Perangkat: Jenis perangkat dan versi OS untuk troubleshooting.",
            "Data Jaringan: Status koneksi internet untuk memastikan sinkronisasi berjalan optimal."
        )
    ),
    PrivacySection(
        icon = Icons.Outlined.CameraAlt,
        title = "Penggunaan Kamera & Izin",
        contentParagraphs = listOf(
            "Fitur kamera digunakan secara eksklusif untuk dua keperluan:"
        ),
        bulletPoints = listOf(
            "Pemindaian Kode QR: Memindai kode QR pada meja pelanggan untuk mengidentifikasi nomor meja dan menghubungkannya dengan pesanan.",
            "Augmented Reality (AR): Menampilkan visualisasi menu secara 3D/AR untuk pengalaman interaktif."
        ),
        highlightNote = "Kami TIDAK merekam, menyimpan, atau mengirimkan foto/video dari kamera Anda. Semua pemrosesan dilakukan secara lokal di perangkat."
    ),
    PrivacySection(
        icon = Icons.Outlined.Notifications,
        title = "Notifikasi",
        contentParagraphs = listOf(
            "Kami menggunakan izin POST_NOTIFICATIONS untuk mengirimkan notifikasi pembaruan pesanan secara real-time, seperti pesanan baru masuk dan perubahan status pesanan."
        ),
        bulletPoints = listOf(
            "Notifikasi hanya terkait aktivitas pesanan di restoran Anda.",
            "Kami tidak mengirimkan notifikasi iklan atau promosi dari pihak ketiga.",
            "Anda dapat menonaktifkan notifikasi kapan saja melalui pengaturan perangkat Android Anda."
        )
    ),
    PrivacySection(
        icon = Icons.Outlined.CloudSync,
        title = "Layanan Latar Belakang",
        contentParagraphs = listOf(
            "Kami menggunakan Foreground Service untuk menjalankan layanan sinkronisasi data pesanan secara real-time di latar belakang."
        ),
        bulletPoints = listOf(
            "Layanan ini memastikan data pesanan selalu tersinkronisasi antara perangkat dan server, bahkan saat aplikasi tidak di layar utama.",
            "Layanan menampilkan notifikasi persisten sesuai kebijakan Android.",
            "Koneksi Socket.io digunakan untuk menerima pembaruan pesanan secara instan.",
            "Layanan hanya aktif saat diperlukan dan dioptimalkan untuk penggunaan baterai minimal."
        )
    ),
    PrivacySection(
        icon = Icons.Outlined.Visibility,
        title = "Kami Tidak Menjual Data Anda",
        contentParagraphs = listOf(
            "Kami TIDAK menjual, memperdagangkan, atau menyewakan informasi pribadi Anda kepada pihak ketiga untuk tujuan komersial atau pemasaran apapun.",
            "Data Anda hanya digunakan semata-mata untuk operasional Aplikasi Qr Meja Pesan."
        ),
        highlightNote = "Data Anda tidak akan pernah dijual. Privasi Anda adalah prioritas utama kami."
    ),
    PrivacySection(
        icon = Icons.Outlined.Security,
        title = "Keamanan Data",
        contentParagraphs = listOf(
            "Kami menerapkan langkah-langkah keamanan untuk melindungi data Anda:"
        ),
        bulletPoints = listOf(
            "Enkripsi Transmisi: Semua komunikasi menggunakan HTTPS/TLS.",
            "Network Security: Aplikasi menolak koneksi cleartext (tidak terenkripsi).",
            "Pencadangan Terbatas: Auto-backup dinonaktifkan untuk melindungi data sensitif.",
            "Autentikasi: Akses ke data akun dilindungi sistem autentikasi yang aman."
        )
    ),
    PrivacySection(
        icon = Icons.Outlined.VerifiedUser,
        title = "Hak-Hak Anda",
        contentParagraphs = listOf(
            "Anda memiliki hak penuh atas data pribadi Anda:"
        ),
        bulletPoints = listOf(
            "Hak Akses: Meminta salinan data pribadi yang kami miliki tentang Anda.",
            "Hak Koreksi: Meminta perbaikan data yang tidak akurat.",
            "Hak Penghapusan: Meminta penghapusan data pribadi Anda.",
            "Hak Penarikan Persetujuan: Menonaktifkan izin aplikasi kapan saja melalui Pengaturan Android.",
            "Hak Kontrol Notifikasi: Mengaktifkan/menonaktifkan notifikasi melalui pengaturan perangkat."
        )
    ),
    PrivacySection(
        icon = Icons.Outlined.Gavel,
        title = "Perubahan Kebijakan",
        contentParagraphs = listOf(
            "Kami dapat memperbarui Kebijakan Privasi ini dari waktu ke waktu. Perubahan signifikan akan diberitahukan melalui notifikasi dalam Aplikasi atau pembaruan tanggal di halaman ini.",
            "Kami mendorong Anda untuk meninjau Kebijakan Privasi ini secara berkala."
        )
    ),
    PrivacySection(
        icon = Icons.Outlined.ContactMail,
        title = "Hubungi Kami",
        contentParagraphs = listOf(
            "Jika Anda memiliki pertanyaan tentang Kebijakan Privasi ini atau penanganan data pribadi Anda, silakan hubungi:"
        ),
        bulletPoints = listOf(
            "Nama Aplikasi: Qr Meja Pesan",
            "Email: fserdtfc@gmail.com"
        ),
        highlightNote = "Kami akan berusaha merespons permintaan Anda dalam waktu 14 hari kerja."
    )
)
