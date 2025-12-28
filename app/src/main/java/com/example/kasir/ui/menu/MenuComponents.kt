package com.example.kasir.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- COLORS ---
private val FormBg = Color(0xFFF3F4F6)
private val CardWhite = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF1F2937)
private val TextGray = Color(0xFF6B7280)
private val BorderColor = Color(0xFFEEEEEE)
private val InputBorder = Color(0xFFE5E7EB)
private val BtnDark = Color(0xFF1F2937)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuFormScreen(
    initialName: String = "",
    initialCategory: String = "",
    initialPrice: String = "",
    initialDescription: String = "",
    initialIsActive: Boolean = true,
    isEditMode: Boolean = false,
    onBack: () -> Unit,
    onSave: (String, String, String, String, Boolean) -> Unit
) {
    var menuName by remember { mutableStateOf(initialName) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var price by remember { mutableStateOf(initialPrice.replace("Rp ", "").replace(".", "")) } // Strip formatting for input
    var description by remember { mutableStateOf(initialDescription) }
    var isActive by remember { mutableStateOf(initialIsActive) }
    var expandedCategory by remember { mutableStateOf(false) }
    
    val categories = listOf("Makanan Berat", "Minuman", "Cemilan")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormBg)
    ) {
        // Header
        Surface(
            color = CardWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(15.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = TextDark)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(if (isEditMode) "Edit Menu" else "Tambah Menu Baru", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Image Section (Upload or Edit)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(0.5.dp)
            ) {
                if (isEditMode) {
                    // Edit Mode: Image Preview with "Ubah Foto" Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        // Mock Image
                         Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(
                                 painter = painterResource(android.R.drawable.ic_menu_gallery),
                                 contentDescription = null,
                                 tint = Color.White,
                                 modifier = Modifier.size(64.dp)
                             )
                        }
                        
                        // "Ubah Foto" Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xD91F2937), // Dark semi-transparent
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clickable { /* Change Photo Logic */ }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(painter = painterResource(android.R.drawable.ic_menu_camera), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ubah Foto", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    // Add Mode: Upload Dashed Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(20.dp)
                            .border(
                                BorderStroke(2.dp, Color(0xFFD1D5DB)), // Dashed mock
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { /* Upload Logic */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_camera), 
                                contentDescription = null, 
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Upload Foto Menu", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF4B5563))
                            Text("Tap untuk menambahkan foto", fontSize = 13.sp, color = Color(0xFF9CA3AF))
                        }
                    }
                }
            }

            // 2. Form Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(0.5.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Detail Menu", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark, modifier = Modifier.padding(bottom = 20.dp))

                    // Menu Name
                    Text("Nama Menu", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF374151), modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = menuName,
                        onValueChange = { menuName = it },
                        placeholder = { Text("Masukkan nama menu", color = Color(0xFFD1D5DB)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = InputBorder,
                            focusedBorderColor = BtnDark
                        )
                    )

                    // Category Dropdown
                    Text("Kategori", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF374151), modifier = Modifier.padding(bottom = 8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedCategory,
                        onExpandedChange = { expandedCategory = !expandedCategory },
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Pilih Kategori", color = Color(0xFFD1D5DB)) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = InputBorder,
                                focusedBorderColor = BtnDark
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false }
                        ) {
                            categories.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        selectedCategory = selectionOption
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }

                    // Price
                    Text("Harga Jual", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF374151), modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        placeholder = { Text("0", color = Color(0xFFD1D5DB)) },
                        leadingIcon = { Text("Rp", fontWeight = FontWeight.Bold, color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = InputBorder,
                            focusedBorderColor = BtnDark
                        )
                    )

                    // Description
                    Text("Deskripsi Menu", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF374151), modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Masukkan deskripsi menu...", color = Color(0xFFD1D5DB)) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = InputBorder,
                            focusedBorderColor = BtnDark
                        )
                    )
                }
            }

            // 3. Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(0.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status Menu", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                        Text("Menu akan aktif dan dapat dipesan", fontSize = 12.sp, color = TextGray)
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CardWhite,
                            checkedTrackColor = BtnDark
                        )
                    )
                }
            }
        }

        // Bottom Button
        Surface(
            color = CardWhite,
            modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, BorderColor))
        ) {
            Box(modifier = Modifier.padding(20.dp)) {
                Button(
                    onClick = { onSave(menuName, selectedCategory, price, description, isActive) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BtnDark)
                ) {
                    Text("Simpan Perubahan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
