package id.quacxel.mejapesan.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.quacxel.mejapesan.data.model.ShippingZone
import java.text.NumberFormat
import java.util.Locale

private val Navy = Color(0xFF2C3E50)
private val Yellow = Color(0xFFFACC15)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShippingZonesDialog(
    shippingZones: List<ShippingZone>,
    onDismiss: () -> Unit,
    onAdd: (String, Int) -> Unit,
    onUpdate: (Int, String, Int, Boolean) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<ShippingZone?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Kelola Ongkos Kirim",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Navy, modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = Color(0xFFF3F4F6))

                // Add Button (Moved to top)
                Box(modifier = Modifier.padding(20.dp)) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Yellow),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Navy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah Zona", color = Navy, fontWeight = FontWeight.Bold)
                    }
                }

                // List
                if (shippingZones.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Belum ada zona pengiriman.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(shippingZones) { zone ->
                            ShippingZoneItem(
                                zone = zone,
                                onEdit = { editingZone = zone },
                                onDelete = { onDelete(zone.id) },
                                onToggle = { isActive -> onUpdate(zone.id, zone.name, zone.fee, isActive) }
                            )
                        }
                    }
                }


            }
        }
    }

    if (showAddDialog || editingZone != null) {
        ShippingZoneFormDialog(
            initialZone = editingZone,
            onDismiss = {
                showAddDialog = false
                editingZone = null
            },
            onSave = { name, fee ->
                if (editingZone != null) {
                    onUpdate(editingZone!!.id, name, fee, editingZone!!.isActive)
                } else {
                    onAdd(name, fee)
                }
                showAddDialog = false
                editingZone = null
            }
        )
    }
}

@Composable
fun ShippingZoneItem(
    zone: ShippingZone,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.name,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                Text(
                    text = formatter.format(zone.fee),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(
                checked = zone.isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = Yellow, checkedTrackColor = Navy)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShippingZoneFormDialog(
    initialZone: ShippingZone?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialZone?.name ?: "") }
    var fee by remember { mutableStateOf(initialZone?.fee?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialZone == null) "Tambah Zona" else "Edit Zona", color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Zona / Kecamatan", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = Color.LightGray,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                OutlinedTextField(
                    value = fee,
                    onValueChange = { fee = it },
                    label = { Text("Ongkos Kirim (Rp)", color = Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = Color.LightGray,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val feeInt = fee.filter { it.isDigit() }.toIntOrNull() ?: 0
                    if (name.isNotBlank()) {
                        onSave(name, feeInt)
                    }
                }
            ) {
                Text("Simpan", color = Navy, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Gray)
            }
        },
        containerColor = Color.White
    )
}
