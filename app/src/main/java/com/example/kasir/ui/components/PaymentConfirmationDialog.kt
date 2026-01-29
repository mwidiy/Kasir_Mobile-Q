package com.example.kasir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RestaurantMenu
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
import java.text.SimpleDateFormat
import java.util.TimeZone

// Custom Colors
private val ColorTextPrimary = Color(0xFF1F2937)
private val ColorTextSecondary = Color(0xFF6B7280)
private val ColorGreenSuccess = Color(0xFF10B981)
private val ColorBgLight = Color(0xFFF9FAFB)
private val ColorCircleBg = Color(0xFF2C3E50)
private val ColorButton = Color(0xFF2C3E50)
private val ColorDivider = Color(0xFFE5E7EB)

@Composable
fun PaymentConfirmationDialog(
    order: OrderResponse,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PaymentConfirmationContent(order, onDismiss, onConfirm)
    }
}

@Composable
private fun PaymentConfirmationContent(
    order: OrderResponse,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val subtotal = order.items.sumOf { it.product.price * it.quantity }.toDouble()
    // Variables used for breakdown removed as requested


    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatRp.maximumFractionDigits = 0

    val timeString = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(order.createdAt)
        val formatter = SimpleDateFormat("HH:mm 'WIB'", Locale.getDefault())
        formatter.format(date ?: 0)
    } catch (e: Exception) {
        "Now"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            
            // Scrollable Area
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                 // 1. Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Konfirmasi Pembayaran", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ColorTextPrimary, fontSize = 20.sp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ColorTextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("QR Code berhasil dipindai", style = MaterialTheme.typography.bodyMedium.copy(color = ColorTextSecondary, fontSize = 14.sp))
                Spacer(modifier = Modifier.height(30.dp))

                // 2. Hero Price
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Pembayaran", style = MaterialTheme.typography.bodyMedium.copy(color = ColorTextSecondary))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatRp.format(order.totalAmount), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF2C3E50), fontSize = 36.sp))
                    Spacer(modifier = Modifier.height(4.dp))

                }
                Spacer(modifier = Modifier.height(30.dp))

                // 3. User Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(ColorCircleBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(order.table?.name ?: "Meja -", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ColorTextPrimary))
                        Text("Pesanan #${order.transactionCode.takeLast(4)}", style = MaterialTheme.typography.bodyMedium.copy(color = ColorTextSecondary))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                // 4. Status Cards
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val cardWidth = (maxWidth - 12.dp) / 2
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Card 1
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier
                                .width(cardWidth)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorBgLight)
                                .padding(12.dp)
                        ) {
                            Text("Waktu Order", style = MaterialTheme.typography.labelSmall.copy(color = ColorTextSecondary))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(timeString, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ColorTextPrimary))
                        }
                        
                        // Card 2
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier
                                .width(cardWidth)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorBgLight)
                                .padding(12.dp)
                        ) {
                            Text("Status", style = MaterialTheme.typography.labelSmall.copy(color = ColorTextSecondary))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Siap Bayar", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ColorGreenSuccess))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = ColorDivider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = ColorTextPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ringkasan Pesanan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ColorTextPrimary))
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // 5. Items
                for (item in order.items) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.Top
                    ) {
                        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth(0.6f)) {
                            Text(item.product.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ColorTextPrimary))
                            if (!item.note.isNullOrBlank()) {
                                Text(item.note, style = MaterialTheme.typography.bodySmall.copy(color = ColorTextSecondary))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.Surface(color = ColorBgLight, shape = RoundedCornerShape(4.dp)) {
                                Text("Jumlah: ${item.quantity}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(color = ColorTextSecondary))
                            }
                        }
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.End
                        ) {
                            Text(formatRp.format(item.product.price * item.quantity), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ColorTextPrimary))
                            Text("@${formatRp.format(item.product.price)}", style = MaterialTheme.typography.bodySmall.copy(color = ColorTextSecondary))
                        }
                    }
                    Divider(color = ColorDivider.copy(alpha=0.5f), thickness = 0.5.dp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // 6. Breakdown


                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = ColorDivider, thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp)) 
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ColorTextPrimary))
                    Text(formatRp.format(order.totalAmount), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ColorTextPrimary))
                }
            }
            
            // Footer
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorButton),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konfirmasi Pembayaran", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
