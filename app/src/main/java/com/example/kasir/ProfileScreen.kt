package com.example.kasir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow

// --- COLORS ---
val Navy = Color(0xFF2C3E50)
val QuackYellow = Color(0xFFF7DC6F)
val QuackYellowDark = Color(0xFFF0C92F)
val BackgroundLight = Color(0xFFF9FAFB)
val SurfaceLight = Color(0xFFFFFFFF)
val Danger = Color(0xFFEF4444)

@Composable
fun ProfileScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            ProfileTopBar(onBack = { onNavigate("dashboard") })
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Section 1: Restaurant Identity
            RestaurantIdentitySection()

            // Section 2: QRIS Management
            QrisManagementSection()

            // Section 3: Footer Actions
            Spacer(modifier = Modifier.weight(1f)) // Push to bottom if content is short
            FooterActions(onNavigate)
        }
    }
}

@Composable
fun ProfileTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Navy)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                //.background(Color.White.copy(alpha = 0.1f)) // Optional hover effect
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        Text(
            text = "Pengaturan Resto",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            ),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        
        // Spacer to balance the back button
        Spacer(modifier = Modifier.size(40.dp)) 
    }
}

@Composable
fun RestaurantIdentitySection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Logo Upload
        Box(
            modifier = Modifier.clickable { /* TODO: Implement Image Picker */ }
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .background(Color.Gray) // Placeholder bg
            ) {
                Image(
                    painter = rememberAsyncImagePainter("https://lh3.googleusercontent.com/aida-public/AB6AXuAEh5lgY19V90cD-UCzN8VRKApZwi2xxEBdfpYtFqe8FzQ6Kjj3n13sJtXEqXeaaMAF7zLXnzXwxXjqWvOjOuYwJB0SaB4yD2uZIDUmT1Wx0Rs9UC1JsksyDjrSfCmLdGW2rp-cDrI4dKDXyjWsRtpUhtlIuN5yYYaV54r3X1cmrgt-bO7Opbmr5XN7PZFwhAi9BqUFZigVtzAgKx7zY7esbg8EXVeZPrjSjJkwlHHEu_wtBMn3a9bvkaLi9pZ_ajX7zkVSJq1qYUc9"),
                    contentDescription = "Restaurant Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Camera Icon Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Navy, CircleShape)
                    .border(3.dp, BackgroundLight, CircleShape)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Logo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Name Input
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Nama Restoran",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Navy
                ),
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            
            var text by remember { mutableStateOf("Dapur QuackXel") }
            
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp)), // Gray-300
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Navy,
                    unfocusedTextColor = Navy
                ),
                trailingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF9CA3AF))
                },
                singleLine = true
            )
        }
    }
}

@Composable
fun QrisManagementSection() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp)) // Gray-100
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Navy.copy(alpha = 0.05f)), // Navy/5
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.List, contentDescription = null, tint = Navy)
                }
                Column {
                    Text(
                        text = "Metode Pembayaran",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )
                    )
                    Text(
                        text = "Upload QRIS Toko untuk pembayaran non-tunai",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280) // Gray-500
                        )
                    )
                }
            }

            // Upload Zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp)) // Gray-300 (Dashed logic would fail here in simple Compose, using solid)
                    //Ideally needs a DrawModifier for Dashed Border
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF9FAFB).copy(alpha = 0.5f)) // Gray-50/50
                    .clickable { /* TODO: Upload Logic */ }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // QR Preview
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .shadow(2.dp, RoundedCornerShape(8.dp)) // Helper shadow
                    ) {
                         Image(
                            painter = rememberAsyncImagePainter("https://lh3.googleusercontent.com/aida-public/AB6AXuAnWg7-cysbnkUL5bdDmxytCueYFQ0G23Gl94yZwpxAOOAVWz3pDzr_3zVbl6caDSS58NoB2PcBzDlvLrUZQy_Ab5a1zlgkd6YJfiIRGtG37TsqXfd6Mut9Suib8RJ2DUScFKEWxwAltWcKET6PwuLv6INS-5OJrj6cJ-AkKOYmYLuo7veET6i1QzDMH3kGacqKkXXHAGxpC_OF9kbqvqHIsun3yunKnsckmdjf3NOLFUPFwz79UXNDqE25eLogMUmwMDrfCHR8JVvX"),
                            contentDescription = "QRIS Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().alpha(0.8f)
                        )
                    }

                    // Button & Text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { /* TODO: Upload Logic */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Navy
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Ganti / Upload QRIS", fontWeight = FontWeight.Bold)
                        }
                        
                        Text(
                            text = "Pastikan kode QRIS terlihat jelas dan valid.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF9CA3AF), // Gray-400
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FooterActions(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Save Button
        Button(
            onClick = { /* TODO: Save Logic */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = QuackYellow,
                contentColor = Navy
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Simpan Perubahan", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        // Logout Button
        TextButton(
            onClick = { onNavigate("login") }, // Assuming Login screen exists
            colors = ButtonDefaults.textButtonColors(contentColor = Danger),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Keluar Akun", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(onNavigate = {})
}
