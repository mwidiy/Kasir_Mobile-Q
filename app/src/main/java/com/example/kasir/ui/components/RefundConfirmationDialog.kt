package com.example.kasir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kasir.data.model.OrderResponse
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RefundConfirmationDialog(
    order: OrderResponse,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon (Red Warning)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                     Icon(
                         imageVector = Icons.Default.Warning, 
                         contentDescription = null, 
                         tint = Color(0xFFDC2626),
                         modifier = Modifier.size(24.dp)
                     )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Verifikasi Refund",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = "Pastikan pelanggan menunjukkan bukti yang valid",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Nominal Refund",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
                )
                Text(
                    text = formatRp.format(order.totalAmount),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        fontSize = 32.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Info Order
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                     Column {
                        Text(
                            text = "Order ID",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                        )
                        Text(
                            text = order.transactionCode,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                     }
                     Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                        )
                        Text(
                            text = "Cancelled",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color.Red)
                        )
                     }
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                // Confirm Button
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konfirmasi Refund", fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batal", color = Color(0xFF6B7280))
                }
            }
        }
    }
}
