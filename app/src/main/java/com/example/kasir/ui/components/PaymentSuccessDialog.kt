package com.example.kasir.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// --- Custom Confetti Implementation ---
data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var alpha: Float = 1f,
    var size: Float,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f
)

@Composable
fun ConfettiExplosion(modifier: Modifier = Modifier) {
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val colors = listOf(
        Color(0xFFFF5252), Color(0xFFFF4081), Color(0xFFE040FB),
        Color(0xFF7C4DFF), Color(0xFF536DFE), Color(0xFF448AFF),
        Color(0xFF40C4FF), Color(0xFF18FFFF), Color(0xFF64FFDA),
        Color(0xFF69F0AE), Color(0xFFB2FF59), Color(0xFFEEFF41),
        Color(0xFFFFEA00), Color(0xFFFFD740), Color(0xFFFFAB40)
    )

    // Initialize particles on first composition
    LaunchedEffect(Unit) {
        // Left Burst
        repeat(50) {
            val angle = Random.nextFloat() * -1.5f // Upward-ish arc
            val speed = Random.nextFloat() * 15f + 10f
            particles.add(
                ConfettiParticle(
                    x = 0f, 
                    y = 1500f, // Approximate bottom start
                    vx = cos(angle) * speed + 10f, // Push right
                    vy = sin(angle) * speed - 20f, // Push up
                    color = colors.random(),
                    size = Random.nextFloat() * 15f + 5f,
                    rotationSpeed = Random.nextFloat() * 10f - 5f
                )
            )
        }
        // Right Burst
        repeat(50) {
            val angle = Random.nextFloat() * -1.5f 
            val speed = Random.nextFloat() * 15f + 10f
            particles.add(
                ConfettiParticle(
                    x = 1000f, // Approximate right start
                    y = 1500f,
                    vx = cos(angle) * speed - 10f, // Push left
                    vy = sin(angle) * speed - 20f, // Push up
                    color = colors.random(),
                    size = Random.nextFloat() * 15f + 5f,
                    rotationSpeed = Random.nextFloat() * 10f - 5f
                )
            )
        }

        // Animation Loop
        val startTime = System.nanoTime()
        while (true) {
            withFrameNanos { time ->
                val dt = 1f // Simplified time step
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 0.5f // Gravity
                    p.rotation += p.rotationSpeed
                    p.alpha -= 0.005f // Fade out
                    
                    if (p.alpha <= 0f || p.y > 2000f) {
                        iterator.remove()
                    }
                }
            }
            if (particles.isEmpty()) break
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawCircle(
                color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                radius = p.size,
                center = Offset(p.x, p.y)
            )
        }
    }
}

@Composable
fun PaymentSuccessDialog(
    totalAmount: Int,
    transactionCode: String,
    onDismiss: () -> Unit
) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatRp.maximumFractionDigits = 0 // Remove decimals

    // Animation States
    var showCheckmark by remember { mutableStateOf(false) }
    
    // Trigger animations
    LaunchedEffect(Unit) {
        delay(100) // Slight delay for smoother entrance
        showCheckmark = true
    }

    // Full Screen Dialog
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Full screen
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF3F4F6) // Light Gray Background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                // Content Layer
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1F2937))
                        }
                        Text(
                            "Pembayaran Berhasil",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Main Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Success Visual with Animation
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7)), // Light Green
                            contentAlignment = Alignment.Center
                        ) {
                           androidx.compose.animation.AnimatedVisibility(
                               visible = showCheckmark,
                               enter = androidx.compose.animation.scaleIn(
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                    )
                               ) + androidx.compose.animation.fadeIn()
                           ) {
                               Box(
                                   modifier = Modifier
                                       .size(80.dp)
                                       .clip(CircleShape)
                                       .background(Color(0xFF22C55E)), // Green
                                   contentAlignment = Alignment.Center
                               ) {
                                   Icon(
                                       imageVector = Icons.Rounded.Check,
                                       contentDescription = null,
                                       tint = Color.White,
                                       modifier = Modifier.size(48.dp)
                                   )
                               }
                           }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Pembayaran Tunai Diterima",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = "Transaksi telah berhasil disimpan",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // Receipt Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Total Pembayaran", color = Color(0xFF6B7280), fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = formatRp.format(totalAmount),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1F2937)
                                )
                                
                                Divider(color = Color(0xFFE5E7EB), modifier = Modifier.padding(vertical = 24.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ID Transaksi", color = Color(0xFF6B7280))
                                    Text(transactionCode, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Metode", color = Color(0xFF6B7280))
                                    Text("Tunai / Cash", fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Status", color = Color(0xFF6B7280))
                                    Text(
                                        "LUNAS", 
                                        fontWeight = FontWeight.Bold, 
                                        color = Color(0xFF15803D),
                                        modifier = Modifier
                                            .background(Color(0xFFDCFCE7), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Button with Updated Padding
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 80.dp) // Moved up significantly (was 24.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15)), // Yellow
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Kembali ke Menu",
                                color = Color(0xFF1F2937),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                // Confetti Layer (On Top)
                ConfettiExplosion()
            }
        }
    }
}
