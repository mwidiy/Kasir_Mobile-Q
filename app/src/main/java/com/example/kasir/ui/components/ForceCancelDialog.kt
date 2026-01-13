package com.example.kasir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ForceCancelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val isEnabled = reason.isNotBlank() && reason.length > 3

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
                Text(
                    text = "Tolak & Batalkan Pesanan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tindakan ini tidak bisa dibatalkan. Pesanan akan langsung hangus.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Normal
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text("Alasan (Min. 4 huruf)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFDC2626),
                        unfocusedBorderColor = Color(0xFFD1D5DB)
                    ),
                    singleLine = false,
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal", color = Color(0xFF374151))
                    }
                    
                    Button(
                        onClick = { onConfirm(reason) },
                        enabled = isEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            disabledContainerColor = Color(0xFFFCA5A5)
                        ),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tolak Pesanan", color = Color.White)
                    }
                }
            }
        }
    }
}
