package com.example.kasir

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.ui.components.PaymentConfirmationDialog
import com.example.kasir.ui.components.PaymentSuccessDialog
import com.example.kasir.ui.components.RefundConfirmationDialog
import com.example.kasir.ui.components.RefundSuccessDialog
import com.example.kasir.viewmodel.ScanViewModel
import com.example.kasir.utils.ImageUtils
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- COLORS ---
private val ScanPrimaryBtn = Color(0xFF1F2937)
private val OverlayColor = Color(0x99000000)
private val CornerColor = Color.White
private val ScanYellow = Color(0xFFFDD835)

@Composable
fun ScanScreen(
    onNavigate: (String) -> Unit,
    viewModel: ScanViewModel = viewModel()
) {
    var showManualInput by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(true) }
    var isFlashOn by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isScanning = false 
            scope.launch {
                val decodedText = withContext(Dispatchers.IO) {
                    ImageUtils.decodeQrFromUri(context, uri)
                }
                
                if (decodedText != null) {
                    viewModel.fetchOrderByCode(decodedText)
                } else {
                    Toast.makeText(context, "QR Code tidak ditemukan dalam gambar.", Toast.LENGTH_SHORT).show()
                    isScanning = true 
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ViewModel State
    val scannedOrder by viewModel.scannedOrder.collectAsState()
    val isPaymentSuccess by viewModel.paymentSuccess.collectAsState()
    val refundOrder by viewModel.refundOrder.collectAsState()
    val isRefundSuccess by viewModel.refundSuccess.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(error) {
        if (error != null) {
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
            delay(2000)
            isScanning = true 
            viewModel.resetState()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Full Screen Camera Layer
        if (hasCameraPermission) {
            ZXingScannerView(
                isScanning = isScanning,
                isFlashOn = isFlashOn,
                onScanResult = { code ->
                    isScanning = false
                    viewModel.fetchOrderByCode(code)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Izin Kamera Diperlukan", color = Color.White)
            }
        }

        // 2. Dark Overlay & Finder Layer
        ScanOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // 3. UI Layer
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onNavigate("dashboard") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "Kasir", 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(24.dp)) // Balance
            }

            Text(
                "Scan Code Qr untuk Bayar Kasir", 
                color = Color.White, 
                fontSize = 14.sp, 
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)
            )

            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Loading Logic
        if (isLoading) {
             Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.3f)), contentAlignment=Alignment.Center) {
                 CircularProgressIndicator(color = Color.White)
             }
        }

        // Modals & Dialogs
         if (showManualInput) {
            ManualInputSheet(
                onDismiss = { showManualInput = false },
                onSubmit = { code -> showManualInput = false; viewModel.fetchOrderByCode(code) }
            )
        }
        if (scannedOrder != null && !isPaymentSuccess) {
            PaymentConfirmationDialog(scannedOrder!!, { viewModel.resetState(); isScanning = true }, { viewModel.confirmPayment(scannedOrder!!.id) })
        }
        if (isPaymentSuccess && scannedOrder != null) {
            PaymentSuccessDialog(scannedOrder!!.totalAmount, scannedOrder!!.transactionCode, { viewModel.resetState(); isScanning = true })
        }
        if (refundOrder != null && !isRefundSuccess) {
            RefundConfirmationDialog(refundOrder!!, { viewModel.resetState(); isScanning = true }, { viewModel.processRefund(refundOrder!!.transactionCode) })
        }
        if (isRefundSuccess) {
            RefundSuccessDialog { viewModel.resetState(); isScanning = true }
        }
        
        // Floating Icons Layer
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 200.dp) 
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
             // Flash Button
            FloatingActionButton(
                onClick = { isFlashOn = !isFlashOn },
                containerColor = Color.White,
                contentColor = if(isFlashOn) ScanYellow else Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(imageVector = if(isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = "Flash")
            }

            // Gallery Button
            FloatingActionButton(
                onClick = { 
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2),
                shape = CircleShape,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = "Gallery")
            }
        }

        // Bottom Sheet Controls
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() 
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 24.dp), 
                horizontalArrangement = Arrangement.Center
            ) {
                // Manual Input Card
                ScanModeCard(
                    iconRes = android.R.drawable.ic_menu_edit,
                    label = "Input Manual",
                    isSelected = true,
                    onClick = { showManualInput = true },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Vertical Divider
                 Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray).align(Alignment.CenterVertically))

                Spacer(modifier = Modifier.width(8.dp))

                // Reset Card (With Refresh Logic + Blue Icon)
                ScanModeCard(
                    iconRes = android.R.drawable.ic_menu_rotate,
                    label = "Reset Scan",
                    isSelected = true, // Force Selected for Blue Icon
                    onClick = { 
                        // Visual Refresh Sequence
                        isScanning = false
                        viewModel.resetState()
                        Toast.makeText(context, "Scanner di-refresh...", Toast.LENGTH_SHORT).show()
                        
                        scope.launch {
                            delay(200) // Brief delay
                            isScanning = true 
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ScanOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val scanSize = 280.dp.toPx()
        val scanLeft = (canvasWidth - scanSize) / 2
        val scanTop = (canvasHeight - scanSize) / 2
        val cornerLength = 30.dp.toPx()
        val strokeWidth = 5.dp.toPx()

        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            drawRect(Color(0x99000000))
            drawRoundRect(
                topLeft = Offset(scanLeft, scanTop),
                size = Size(scanSize, scanSize),
                cornerRadius = CornerRadius(20f, 20f),
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )
            restoreToCount(checkPoint)
        }

        val path = Path().apply {
            moveTo(scanLeft, scanTop + cornerLength)
            lineTo(scanLeft, scanTop)
            lineTo(scanLeft + cornerLength, scanTop)

            moveTo(scanLeft + scanSize - cornerLength, scanTop)
            lineTo(scanLeft + scanSize, scanTop)
            lineTo(scanLeft + scanSize, scanTop + cornerLength)

            moveTo(scanLeft + scanSize, scanTop + scanSize - cornerLength)
            lineTo(scanLeft + scanSize, scanTop + scanSize)
            lineTo(scanLeft + scanSize - cornerLength, scanTop + scanSize)

            moveTo(scanLeft + cornerLength, scanTop + scanSize)
            lineTo(scanLeft, scanTop + scanSize)
            lineTo(scanLeft, scanTop + scanSize - cornerLength)
        }

        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ScanModeCard(iconRes: Int, label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable { onClick() }.padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(iconRes), 
            contentDescription = null, 
            tint = if (isSelected) Color(0xFF1565C0) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label, 
            fontSize = 12.sp, 
            color = if (isSelected) Color(0xFF1565C0) else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun ZXingScannerView(isScanning: Boolean, isFlashOn: Boolean, onScanResult: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val compoundBarcodeView = remember {
        CompoundBarcodeView(context).apply {
            val settings = cameraSettings
            settings.isAutoTorchEnabled = false
            cameraSettings = settings
            setStatusText("") 
        }
    }
    
    DisposableEffect(Unit) {
        compoundBarcodeView.resume()
        onDispose { compoundBarcodeView.pause() }
    }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            compoundBarcodeView.decodeSingle(object : BarcodeCallback {
                override fun barcodeResult(result: BarcodeResult?) {
                    result?.text?.let { onScanResult(it) }
                }
                override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
            })
        } else {
            compoundBarcodeView.barcodeView.stopDecoding()
        }
    }
    
    LaunchedEffect(isFlashOn) {
        if (isFlashOn) compoundBarcodeView.setTorchOn() else compoundBarcodeView.setTorchOff()
    }

    AndroidView(factory = { compoundBarcodeView }, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputSheet(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        // Use full screen box to assist with alignment
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
                .imePadding(), 
            contentAlignment = Alignment.BottomCenter
        ) {
            // Content Card
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight() 
            ) {
                // Main Content Box for Absolute Positioning
                Box(modifier = Modifier.fillMaxWidth()) {
                    
                    // Close Button (Absolute Top Right)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp) // Adjusted padding
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }

                    Column(modifier = Modifier.padding(24.dp)) {
                        // Drag Handle
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.width(40.dp).height(5.dp).background(Color(0xFFE0E0E0), CircleShape))
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Header
                        Text(
                            "Manual Input", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color.Black
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Masukkan kode transaksi dari struk.", fontSize = 14.sp, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        var text by remember { mutableStateOf("") }
                        val focusRequester = remember { FocusRequester() }

                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text("Contoh: INV-88229", color = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = ScanPrimaryBtn,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                cursorColor = ScanPrimaryBtn
                            ),
                            singleLine = true
                        )
                        
                        LaunchedEffect(Unit) {
                            try { focusRequester.requestFocus() } catch(e: Exception) {}
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { if(text.isNotEmpty()) onSubmit(text) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScanPrimaryBtn),
                            shape = RoundedCornerShape(12.dp),
                            enabled = text.isNotEmpty()
                        ) {
                            Text(
                                "Cari Pesanan", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 16.sp,
                                color = Color.White // Set White Text
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
