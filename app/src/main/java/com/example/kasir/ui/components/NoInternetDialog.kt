package com.example.kasir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import com.example.kasir.R

@Composable
fun NoInternetDialog(
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = {}) { // Prevent dismissal by clicking outside
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Using a standard resource if available or text icon
                // Since we might not have a dedicated drawable for wifi_off, we use Text or Icon
                // For better look, let's use a Text Emoji large or Icon if available
                // Assuming Material Icons are available via androidx.compose.material.icons but we don't know if dependency is there.
                // We'll use a large Text Emoji for safety or a resource if user has it.
                // Let's use a Text Emoji "📡❌" or similar for now to avoid compilation error on missing resource.
                
                Text(
                    text = "📡❌", // Antenna + Cross
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Oops! Tidak Ada Internet",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Aplikasi ini membutuhkan koneksi internet untuk memproses pesanan secara real-time. Mohon periksa koneksi Anda.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Normal
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Keluar", color = Color(0xFF374151))
                    }
                    
                    // Retry Button
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB) // Blue
                        ),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Coba Lagi", color = Color.White)
                    }
                }
            }
        }
    }
}
