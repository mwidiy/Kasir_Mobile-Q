package com.example.kasir

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

// --- COLORS ---
private val ScanBg = Color(0xFF121212)
private val ScanCardBg = Color(0xFF1F1F1F)
private val ScanTextWhite = Color(0xFFFFFFFF)
private val ScanTextGray = Color(0xFF9E9E9E)
private val ScanYellow = Color(0xFFFDD835)
private val ScanGreen = Color(0xFF4CAF50)
private val ScanButtonBg = Color(0xFF2C2C2C)
private val ScanPrimaryBtn = Color(0xFF1F2937)

@Composable
fun ScanScreen(onNavigate: (String) -> Unit) {
    var showManualInput by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(ScanBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Back button (Functional but maybe less relevant in bottom nav mode, kept for design fidelity)
                IconButton(onClick = { onNavigate("dashboard") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ScanTextWhite)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pembayaran Kasir", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = ScanTextWhite)
                    Text("Restaurant Admin", fontSize = 12.sp, color = ScanTextGray)
                }
                Spacer(modifier = Modifier.width(40.dp)) // Balance layout
            }

            // Main Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Scan QR Code", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ScanTextWhite)
                Text("Arahkan kamera ke kode QR pelanggan", fontSize = 14.sp, color = ScanTextGray, modifier = Modifier.padding(bottom = 40.dp))

                // Scan Frame (Camera View)
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFF555555), RoundedCornerShape(24.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        CameraPreview()
                    } else {
                        Text("Izin Kamera Diperlukan", color = ScanTextGray, fontSize = 12.sp)
                    }
                    
                    // Loading Animation Overlay
                    ScanLoader()
                }
                
                Spacer(modifier = Modifier.height(30.dp))

                // Tips Card
                TipsCard()
            }

            // Bottom Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .padding(bottom = 80.dp), // Extra padding for bottom nav
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                ScanActionButton("Manual Input", android.R.drawable.ic_menu_edit, Modifier.weight(1f)) { showManualInput = true }
                ScanActionButton("Recent Scans", android.R.drawable.ic_menu_recent_history, Modifier.weight(1f)) { /* Logic */ }
            }
        }
        
        // Manual Input Sheet
        if (showManualInput) {
            ManualInputSheet(onDismiss = { showManualInput = false })
        }

        // Bottom Nav
        AppBottomNavigation(
            currentScreen = "bayar", 
            onNavigate = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun CameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
fun ScanLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_loader")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = Modifier.size(60.dp).rotate(angle)) {
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            style = Stroke(
                width = 4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f))
            )
        )
    }
}

@Composable
fun TipsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ScanCardBg)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 15.dp)) {
            Icon(painter = painterResource(android.R.drawable.ic_menu_info_details), contentDescription = null, tint = ScanYellow, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Tips untuk scanning yang optimal:", color = ScanTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        
        val tips = listOf(
            "Pastikan QR code berada dalam frame",
            "Jaga jarak 10-30cm dari layar",
            "Pastikan pencahayaan cukup terang"
        )
        
        tips.forEach { tip ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                Icon(painter = painterResource(android.R.drawable.checkbox_on_background), contentDescription = null, tint = ScanGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(tip, color = ScanTextGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ScanActionButton(text: String, iconId: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ScanButtonBg),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(56.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(iconId), contentDescription = null, tint = ScanTextWhite, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text, color = ScanTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputSheet(onDismiss: () -> Unit) {
    // Mimic bottom sheet using a Dialog aligned to bottom to avoid complex scaffold state handling for one feature
    // Ideally use data or modal bottom sheet
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Dismiss area
            Box(modifier = Modifier.fillMaxSize().clickable { onDismiss() })
            
            // Sheet Content
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().clickable(enabled = false) {} // Prevent click through
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Handle
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color(0xFFE0E0E0), CircleShape))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Masukkan Kode Manual", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Text("Tutup", color = Color(0xFF4B5563), fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onDismiss() })
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("ID Pesanan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var text by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Ketik ID Pesanan...") },
                        trailingIcon = { Text("#", fontWeight = FontWeight.Bold, color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScanPrimaryBtn),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cari Pesanan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
