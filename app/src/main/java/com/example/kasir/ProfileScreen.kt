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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // Add dependency if missing
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import com.example.kasir.viewmodel.ProfileViewModel
import com.example.kasir.utils.FileUtils
import androidx.compose.ui.platform.LocalContext

// --- COLORS ---
val Navy = Color(0xFF2C3E50)
val QuackYellow = Color(0xFFF7DC6F)
val QuackYellowDark = Color(0xFFF0C92F)
val BackgroundLight = Color(0xFFF9FAFB)
val SurfaceLight = Color(0xFFFFFFFF)
val Danger = Color(0xFFEF4444)

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val storeState by viewModel.storeState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val context = LocalContext.current
    
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
                verticalArrangement = Arrangement.spacedBy(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Section 1: Restaurant Identity
                RestaurantIdentitySection(
                    name = storeState?.name ?: "Nama Resto",
                    logoUrl = storeState?.logo,
                    onNameChange = { /* handled in button or on value change */ }, // simplified
                    onLogoClick = { logoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    viewModel = viewModel
                )

                // Section 2: QRIS Management
                QrisManagementSection(
                    qrisUrl = storeState?.qrisImage,
                    onUploadClick = { qrisLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )

                // Error Message
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = Danger, style = MaterialTheme.typography.bodySmall)
                }

                // Section 3: Footer Actions
                Spacer(modifier = Modifier.weight(1f)) // Push to bottom if content is short
                FooterActions(onNavigate)
            }
        }
    }
}

@Composable
fun ProfileTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Navy)
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Logo Upload
        Box(
            modifier = Modifier.clickable { onLogoClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .background(Color.Gray) // Placeholder bg
            ) {
                val imageUrl = if (!logoUrl.isNullOrEmpty()) {
                    if (logoUrl.startsWith("http")) logoUrl 
                    else "http://192.168.1.4:3000/uploads/$logoUrl" // IP hardcoded for demo, better inject base URL
                } else null

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
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Nama Restoran",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Navy
                ),
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            
            var text by remember(name) { mutableStateOf(name) }
            
            TextField(
                value = text,
                onValueChange = { 
                    text = it
                    // Optional: Auto save or wait for button. Implementing simple auto-save or call viewmodel
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp)), // Gray-300
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Navy,
                    unfocusedTextColor = Navy
                ),
                trailingIcon = {
                     IconButton(onClick = { viewModel.updateName(text) }) {
                        Icon(Icons.Default.Check, contentDescription = "Save Name", tint = Navy)
                     }
                },
                singleLine = true
            )
        }
    }
}

@Composable
fun QrisManagementSection(
    qrisUrl: String?,
    onUploadClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp)) // Gray-100
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header...
            Row(
                 verticalAlignment = Alignment.CenterVertically,
                 horizontalArrangement = Arrangement.spacedBy(12.dp),
                 modifier = Modifier.padding(bottom = 16.dp)
            ) {
                 Box(
                     modifier = Modifier.size(40.dp).clip(CircleShape).background(Navy.copy(alpha=0.05f)),
                     contentAlignment = Alignment.Center
                 ) {
                      Icon(Icons.Default.List, contentDescription = null, tint = Navy)
                 }
                 Column {
                     Text("Metode Pembayaran", style=MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.Bold, color=Navy))
                     Text("Upload QRIS Toko", style=MaterialTheme.typography.bodySmall.copy(color=Color.Gray))
                 }
            }

            // Upload Zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF9FAFB))
                    .clickable { onUploadClick() }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val imageUrl = if (!qrisUrl.isNullOrEmpty()) {
                         if (qrisUrl.startsWith("http")) qrisUrl 
                         else "http://192.168.1.4:3000/uploads/$qrisUrl" 
                    } else null
                    
                    // QR Preview
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .shadow(2.dp, RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUrl ?: "https://via.placeholder.com/150?text=No+QRIS"),
                            contentDescription = "QRIS Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Button(
                        onClick = onUploadClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Navy),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ganti / Upload QRIS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FooterActions(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Logout Button
        TextButton(
            onClick = { onNavigate("login") }, 
            colors = ButtonDefaults.textButtonColors(contentColor = Danger),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Keluar Akun", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    // Preview won't work well due to ViewModel dependency without mocking
}
