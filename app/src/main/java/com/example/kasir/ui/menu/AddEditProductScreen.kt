package com.example.kasir.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.data.model.Product
import com.example.kasir.viewmodel.MenuViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    productId: String? = null,
    onBack: () -> Unit,
    viewModel: MenuViewModel
) {
    val isEditMode = productId != null
    val products by viewModel.products.collectAsState()
    
    // Initial state
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    // var image by remember { mutableStateOf("") } // Deprecated string URL
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf<String?>(null) }
    
    var isActive by remember { mutableStateOf(true) }
    var isInitialized by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    // Load data if edit mode
    LaunchedEffect(productId, products) {
        if (isEditMode && !isInitialized && products.isNotEmpty()) {
            val product = products.find { it.id.toString() == productId }
            if (product != null) {
                name = product.name
                price = product.price.toString()
                category = product.category
                description = product.description ?: ""
                // image = product.image ?: ""
                existingImageUrl = product.image
                isActive = product.isActive
                isInitialized = true
            }
        }
    }

    val categories = listOf(
        "makanan" to "Makanan Berat", 
        "minuman" to "Minuman", 
        "cemilan" to "Cemilan",
        "paket" to "Paket Hemat"
    )
    val categoryDisplay = categories.find { it.first == category }?.second ?: "Pilih Kategori"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Menu" else "Tambah Menu Baru", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF3F4F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Form Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Detail Menu", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(20.dp))

                    // Name
                    Text("Nama Menu", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color.Black
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Category
                    Text("Kategori", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = categoryDisplay,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = Color.Black
                            )
                        )
                        Box(modifier = Modifier
                            .matchParentSize()
                            .clickable { expanded = true })
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            categories.forEach { (id, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        category = id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Price
                    Text("Harga Jual", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = price,
                        onValueChange = { if (it.all { char -> char.isDigit() }) price = it },
                        prefix = { Text("Rp ", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color.Black
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    Text("Deskripsi Menu", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color.Black
                        ),
                        minLines = 3,
                        maxLines = 5
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Image Picker
                    Text("Gambar Produk", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri: android.net.Uri? ->
                        selectedImageUri = uri
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF3F4F6))
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            coil.compose.AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else if (!existingImageUrl.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = existingImageUrl,
                                contentDescription = "Existing Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                                Text("Pilih Foto", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                 Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status Menu", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1F2937))
                        Text(if (isActive) "Menu aktif dan dapat dipesan" else "Menu tidak tersedia", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1F2937))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Save Button
            val isFormValid = name.isNotBlank() && price.isNotBlank() && category.isNotBlank()
            val context = androidx.compose.ui.platform.LocalContext.current // Move Context here

            Button(
                onClick = {
                    val p = Product(
                        id = if (isEditMode) productId!!.toInt() else 0,
                        name = name,
                        price = price.toIntOrNull() ?: 0,
                        category = category,
                        description = description,
                        image = existingImageUrl, 
                        isActive = isActive
                    )
                    
                    if (isEditMode) {
                        viewModel.updateProduct(p.id, p, selectedImageUri, context)
                    } else {
                        viewModel.addProduct(p, selectedImageUri, context)
                    }
                    onBack()
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F2937),
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(if (isEditMode) "Simpan Perubahan" else "Simpan Menu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
