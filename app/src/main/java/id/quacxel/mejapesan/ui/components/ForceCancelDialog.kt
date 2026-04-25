package id.quacxel.mejapesan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
                        // SECURITY: Mencegah Paste/Type panjang & Karakter Aneh
                        value = reason,
                        onValueChange = { input -> 
                            val filtered = input.replace(Regex("[^a-zA-Z0-9 .,!?\\-]"), "")
                            reason = filtered.take(150)
                        },
                        placeholder = { Text("Alasan (Min. 4 huruf, Max 150)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = Color(0xFFD1D5DB),
                            focusedTextColor = Color(0xFF1F2937),
                            unfocusedTextColor = Color(0xFF1F2937)
                        ),
                        singleLine = false,
                        maxLines = 3
                    )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var isSubmitting by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSubmitting
                    ) {
                        Text("Batal", color = Color(0xFF374151))
                    }
                    
                    Button(
                        onClick = {
                            if (!isSubmitting) {
                                isSubmitting = true
                                onConfirm(reason)
                            }
                        },
                        enabled = isEnabled && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubmitting) Color.Gray else Color(0xFFDC2626),
                            disabledContainerColor = Color(0xFFFCA5A5)
                        ),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) {
                             CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Tolak", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
