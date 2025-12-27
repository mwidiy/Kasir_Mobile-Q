package com.example.kasir

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppBottomNavigation(
    currentScreen: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Surface(
            shadowElevation = 10.dp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItemShared("📊", "Dasbor", currentScreen == "dashboard") { onNavigate("dashboard") }
                NavItemShared("📋", "Kasir", currentScreen == "riwayat") { onNavigate("riwayat") }
                Spacer(modifier = Modifier.width(56.dp)) 
                NavItemShared("🍽️", "Menu", currentScreen == "menu") { onNavigate("menu") }
                NavItemShared("📷", "QR", currentScreen == "meja") { onNavigate("meja") }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 10.dp)
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF1F2937), Color(0xFF111827))))
                .border(4.dp, Color.White, CircleShape)
                .shadow(8.dp, CircleShape)
                .clickable { onNavigate("bayar") },
            contentAlignment = Alignment.Center
        ) {
            Text("📱", fontSize = 24.sp)
        }
        
        Text(
            "Bayar",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        )
    }
}

@Composable
fun NavItemShared(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(icon, fontSize = 20.sp)
        Text(
            label, 
            fontSize = 10.sp, 
            fontWeight = if(active) FontWeight.Bold else FontWeight.Normal,
            color = if(active) Color(0xFF1F2937) else Color.Gray
        )
    }
}
