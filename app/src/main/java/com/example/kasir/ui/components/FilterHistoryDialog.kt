package com.example.kasir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun FilterHistoryDialog(
    onDismiss: () -> Unit,
    onApply: (String, String) -> Unit // status, type
) {
    var selectedStatus by remember { mutableStateOf("All") }
    var selectedType by remember { mutableStateOf("All") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Filter Riwayat",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // STATUS SECTION
                Text("Status Pesanan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(label = "Semua", isSelected = selectedStatus == "All") { selectedStatus = "All" }
                    FilterChip(label = "Selesai", isSelected = selectedStatus == "Completed") { selectedStatus = "Completed" }
                    FilterChip(label = "Dibatalkan", isSelected = selectedStatus == "Cancelled") { selectedStatus = "Cancelled" }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // TYPE SECTION
                Text("Tipe Pesanan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(label = "Semua", isSelected = selectedType == "All") { selectedType = "All" }
                    FilterChip(label = "Dine In", isSelected = selectedType == "dinein") { selectedType = "dinein" }
                    FilterChip(label = "Takeaway", isSelected = selectedType == "takeaway") { selectedType = "takeaway" }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onApply(selectedStatus, selectedType) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E50)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Terapkan Filter", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF2D3E50) else Color(0xFFF3F4F6))
            .border(1.dp, if (isSelected) Color(0xFF2D3E50) else Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Color(0xFF4B5563),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
