package com.example.kasir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kasir.ui.theme.KasirTheme

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.data.model.LoginRequest
import com.example.kasir.utils.SessionManager
import android.widget.Toast
import android.util.Log

// Colors from CSS
val PrimaryBlue = Color(0xFF1E3A5F)
val TextDark = Color(0xFF1A2B48)
val TextGrey = Color(0xFF6C757D)
val BgAccent = Color(0xFFFFFBF2)
val GoogleRed = Color(0xFFEA4335)
val GoogleBlue = Color(0xFF4285F4)
val GoogleYellow = Color(0xFFFBBC05)
val GoogleGreen = Color(0xFF34A853)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Auto-Login Check
    LaunchedEffect(Unit) {
        SessionManager.loadSession(context)
        if (SessionManager.isLoggedIn()) {
            onLoginSuccess()
        }
    }

    // --- LEGACY GOOGLE SIGN IN SETUP (TARGET LOGIC PRESERVED) ---
    val webClientId = BuildConfig.WEB_CLIENT_ID
    
    val gso = remember {
        com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    
    val googleSignInClient = remember {
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                Log.d("Login", "Google ID Token: $idToken")
                // Call Backward API
                scope.launch {
                    try {
                        val response = RetrofitClient.instance.googleLogin(LoginRequest(idToken))
                        if (response.isSuccessful && response.body()?.success == true) {
                            val body = response.body()!!
                            SessionManager.saveSession(context, body.token, body.user)
                            Toast.makeText(context, "Login Berhasil: ${body.user.store?.name}", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            Toast.makeText(context, "Login Gagal API: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error API: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Token Kosong", Toast.LENGTH_SHORT).show()
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            Log.e("Login", "SignInResult:failed code=" + e.statusCode)
            val errorMsg = when(e.statusCode) {
                10 -> "Error 10: SHA-1 Mismatch / Config Salah. Cek Google Console!"
                12500 -> "Error 12500: HP Gak Support / Update Play Services"
                else -> "Google Error: ${e.statusCode}"
            }
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    val onGoogleLoginClick = {
        isLoading = true
        googleSignInClient.signOut().addOnCompleteListener {
             val signInIntent = googleSignInClient.signInIntent
             launcher.launch(signInIntent)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- BACKGROUND DECORATIONS (FROM SOURCE) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(0.65f)
                .fillMaxHeight(0.35f)
                .clip(RoundedCornerShape(bottomStart = 40.dp))
                .background(BgAccent)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 50.dp)
                .size(250.dp)
                .clip(CircleShape)
                .background(Color(0xFFF4F6F8).copy(alpha = 0.6f))
        )

        // --- CONTENT WRAPPER ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Logo (Static - matches Position of Splash Animation end)
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "QuackXel Logo",
                modifier = Modifier
                    .width(120.dp)
                    .padding(bottom = 40.dp),
                contentScale = ContentScale.Fit
            )

            // Teks Judul
            Text(
                text = "Kelola Dasbor Anda",
                color = PrimaryBlue,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 15.dp)
            )

            // Teks Sub-judul
            Text(
                text = "Masuk dengan akun Google Anda untuk melanjutkan.",
                color = TextGrey,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(280.dp)
                    .padding(bottom = 50.dp)
            )

            // --- GOOGLE BUTTON (NEW COMPONENT, TARGET LOGIC) ---
            if (isLoading) {
                 androidx.compose.material3.CircularProgressIndicator(color = PrimaryBlue)
            } else {
                 com.example.kasir.ui.components.GoogleButton(onClick = { onGoogleLoginClick() })
            }
        }
    }
}

// --- DIAGNOSTIC HELPER (Optional, kept for debugging if needed) ---
fun getAppSignature(context: android.content.Context): String {
    try {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_SIGNATURES
        )
        val signatures = packageInfo.signatures
        val cert = signatures!![0].toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-1")
        val digest = md.digest(cert)
        return digest.joinToString(":") { "%02X".format(it) }
    } catch (e: Exception) {
        return "Error: ${e.message}"
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    KasirTheme {
        LoginScreen()
    }
}
