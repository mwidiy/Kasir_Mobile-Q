package id.quacxel.mejapesan.ui.menu

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
import id.quacxel.mejapesan.data.model.Product
import id.quacxel.mejapesan.viewmodel.MenuViewModel
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
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf<String?>(null) }
    
    var isActive by remember { mutableStateOf(true) }
    var isToggling by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) } // Anti-spam lock
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Unlock if the process finishes (e.g. error happened, so it didn't call onSuccess)
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isSubmitting = false
        }
    }
    
    // Toggle state variables
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLocalActive by remember(isActive) { mutableStateOf(isActive) }
    var clickTimestamps by remember { mutableStateOf(listOf<Long>()) }

    LaunchedEffect(isActive) {
        if (!isToggling) {
            isLocalActive = isActive
        }
    }
    
    val isFormValid = name.isNotBlank() && price.isNotBlank() && category.isNotBlank()

    // Load data if edit mode
    LaunchedEffect(productId, products) {
        if (isEditMode && !isInitialized && products.isNotEmpty()) {
            val product = products.find { it.id.toString() == productId }
            if (product != null) {
                name = product.name
                price = product.price.toString()
                category = product.categoryId?.toString() ?: ""
                description = product.description ?: ""
                existingImageUrl = id.quacxel.mejapesan.utils.ImageUtils.getDynamicImageUrl(product.image)
                isActive = product.isActive
                isInitialized = true
            }
        }
    }

    val categoriesState by viewModel.categories.collectAsState()
    val categoryDisplay = categoriesState.find { it.id == category.toIntOrNull() }?.name ?: "Pilih Kategori"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Menu" else "Tambah Menu Baru", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1F2937),
                    navigationIconContentColor = Color(0xFF1F2937)
                )
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
                        onValueChange = { input -> 
                            // Sanitize: Alphanumeric and spaces/hyphens only. Max 20 chars
                            if (input.length <= 20) {
                                name = input.replace(Regex("[^a-zA-Z0-9 -]"), "")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Gray
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                        singleLine = true
                    )
                    Text(
                        text = "${name.length}/20",
                        color = if (name.length >= 20) Color.Red else Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
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
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
                        )
                        Box(modifier = Modifier
                            .matchParentSize()
                            .clickable { expanded = true })
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            categoriesState.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name, color = Color(0xFF1F2937)) },
                                    onClick = {
                                        category = cat.id.toString()
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
                        onValueChange = { input -> 
                            // Sanitize: purely digits
                            val sanitized = input.filter { it.isDigit() }
                            price = sanitized 
                        },
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
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    Text("Deskripsi Menu", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { input -> 
                            // Sanitize: Alphanumeric, spaces, basic punctuation allowed. Stop scripts/SQLi. Max 100 chars
                            if (input.length <= 100) {
                                description = input.replace(Regex("[^a-zA-Z0-9 .,!()\\n-]"), "")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color.Black
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                        minLines = 3,
                        maxLines = 5
                    )
                    Text(
                        text = "${description.length}/100",
                        color = if (description.length >= 100) Color.Red else Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Image Picker
                    Text("Gambar Produk", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                    Spacer(modifier = Modifier.height(8.dp))
                    
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
                        checked = isLocalActive,
                        onCheckedChange = { newStatus -> 
                            val currentTime = System.currentTimeMillis()
                            val recentClicks = clickTimestamps.filter { currentTime - it < 2000 }
                            
                            if (recentClicks.size >= 3) {
                                android.widget.Toast.makeText(context, "Terlalu cepat! Tunggu sebentar ⏳", android.widget.Toast.LENGTH_SHORT).show()
                                clickTimestamps = recentClicks
                                return@Switch
                            }
                            clickTimestamps = recentClicks + currentTime
                            
                            isLocalActive = newStatus // Instant UI Fix
                            isActive = newStatus // Reflect internal form state
                            
                            if (isEditMode && !isToggling) {
                                val currentProduct = products.find { it.id.toString() == productId }
                                if (currentProduct != null) {
                                    isToggling = true
                                    viewModel.toggleProductStatus(currentProduct) {
                                        isToggling = false
                                    }
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF1F2937),
                            uncheckedThumbColor = Color(0xFF1F2937),
                            uncheckedTrackColor = Color.LightGray,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }
            
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (isFormValid && !isSubmitting) {
                        isSubmitting = true // INSTANT LOCK (Anti-Spam)
                        val p = Product(
                            id = if (isEditMode) productId!!.toInt() else 0,
                            name = name,
                            price = price.toIntOrNull() ?: 0,
                            category = null, // Backend handles this
                            categoryId = category.toIntOrNull(),
                            description = description,
                            image = existingImageUrl, 
                            isActive = isActive
                        )
                        
                        if (isEditMode) {
                            viewModel.updateProduct(p.id, p, selectedImageUri, context, onSuccess = {
                                isSubmitting = false
                                onBack()
                            })
                        } else {
                            viewModel.addProduct(p, selectedImageUri, context, onSuccess = {
                                isSubmitting = false
                                onBack()
                            })
                        }
                        // Remove immediate onBack() so it waits for server validation
                        // Note: If server fails, the user must be able to click again. 
                        // The ViewModel sets an error message which shows a Toast. We can unlock isSubmitting after a delay if needed, 
                        // but ideally we'd observe a StateFlow. For now, we will unlock if the user changes an input.
                    } else if (!isFormValid) {
                            android.widget.Toast.makeText(context, "Mohon lengkapi Nama, Harga, dan Kategori", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid && !isSubmitting) Color(0xFF2D3E50) else Color.Gray,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(if (isEditMode) "Simpan Perubahan" else "Simpan Menu", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(100.dp)) // Add bottom padding for better scroll experience
        }
    }
}
