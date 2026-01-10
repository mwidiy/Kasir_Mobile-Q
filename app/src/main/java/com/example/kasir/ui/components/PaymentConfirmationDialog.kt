package com.example.kasir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kasir.data.model.OrderResponse
import com.example.kasir.data.model.OrderItemResponse
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PaymentConfirmationDialog(
    order: OrderResponse,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val tax = order.totalAmount * 0.1 // Assuming 10% tax
    val service = order.totalAmount * 0.05 // Assuming 5% service
    // Note: Adjust calculation logic based on your actual backend logic if needed.
    // Here we use simplified logic to match UI for "Total Amount" display.
    // Ideally, totalAmount from backend already includes everything or we calculate.
    // Let's assume order.totalAmount IS the final total for now, or display breakdown calculation.
    
    // Formatting currency
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
                // Header Icon
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                     Icon(
                         imageVector = Icons.Default.Info, 
                         contentDescription = null, 
                         tint = Color(0xFF4B5563),
                         modifier = Modifier.size(24.dp)
                     )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Konfirmasi Pembayaran",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = "QR Code berhasil dipindai",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Total Pembayaran",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
                )
                Text(
                    text = formatRp.format(order.totalAmount),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        fontSize = 32.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Info User
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF2C3E50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("M", color = Color.White, fontWeight = FontWeight.Bold) // Placeholder Icon
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = order.table?.name ?: "Meja -",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Pesanan ${order.transactionCode}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                            )
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                // Simplified Order Summary (Top 2 items + more)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Ringkasan Pesanan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    order.items.take(2).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                             Column(modifier = Modifier.weight(1f)) {
                                 Text(item.product.name, fontWeight = FontWeight.SemiBold)
                                 Text("${item.quantity}x @ ${formatRp.format(item.product.price)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                             }
                             Text(formatRp.format(item.product.price * item.quantity), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (order.items.size > 2) {
                        Text("+ ${order.items.size - 2} items lainnya", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                // Confirm Button
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50)),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konfirmasi Pembayaran", fontSize = 16.sp)
                }
            }
        }
    }
}
