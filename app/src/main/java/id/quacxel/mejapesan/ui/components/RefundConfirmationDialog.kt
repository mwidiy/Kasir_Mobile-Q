package id.quacxel.mejapesan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import id.quacxel.mejapesan.data.model.OrderResponse
import java.text.NumberFormat
import java.util.Locale
import id.quacxel.mejapesan.utils.LocalAdaptiveValues
import id.quacxel.mejapesan.utils.adaptiveDialogWidth

@Composable
fun RefundConfirmationDialog(
    order: OrderResponse,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    
    val adaptive = LocalAdaptiveValues.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.adaptiveDialogWidth(adaptive).fillMaxWidth().padding(16.dp)
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
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                var isSubmitting by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                // Confirm Button
                Button(
                    onClick = {
                        if (!isSubmitting) {
                            isSubmitting = true
                            onConfirm()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSubmitting) Color.Gray else Color(0xFFDC2626)),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Konfirmasi Refund", fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    Text("Batal", color = Color(0xFF6B7280))
                }
            }
        }
    }
}
