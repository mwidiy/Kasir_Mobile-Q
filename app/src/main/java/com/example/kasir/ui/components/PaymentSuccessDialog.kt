package com.example.kasir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Check
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
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PaymentSuccessDialog(
    totalAmount: Int,
    transactionCode: String,
    onDismiss: () -> Unit
) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    // Colors from HTML
    val BgDark = Color(0xFF1A2C4E)
    val PrimaryYellow = Color(0xFFF3E6A8)
    val SuccessMint = Color(0xFF4ADE80)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BgDark),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Icon Circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFBBF7D0)), // Light Mint
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(64.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Pembayaran Berhasil!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Terima kasih atas pembayaran Anda.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xB3DBEAFE))
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Amount Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Pembayaran", color = Color(0x99DBEAFE))
                        Spacer(modifier = Modifier.height(8.dp))
                         Text(
                            text = formatRp.format(totalAmount),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessMint,
                                fontSize = 36.sp
                            )
                        )
                        
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 20.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             Text("ID Pesanan: ", color = Color(0xE6FFFFFF))
                             Text(transactionCode, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Selesai",
                        color = BgDark,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
