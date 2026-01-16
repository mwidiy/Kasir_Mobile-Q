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
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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

    // --- LEGACY GOOGLE SIGN IN SETUP ---
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
            // SHOW THE REAL ERROR CODE
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
        // --- BACKGROUND DECORATIONS ---
        
        // Kotak Krem di Kanan Atas
        // .bg-deco-top { width: 65%; height: 35%; ... border-bottom-left-radius: 40px; }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(0.65f)
                .fillMaxHeight(0.35f)
                .clip(RoundedCornerShape(bottomStart = 40.dp))
                .background(BgAccent)
        )

        // Lingkaran Samar di Bawah Kiri
        // .bg-deco-bottom { ... width: 250px; height: 250px; ... border-radius: 50%; opacity: 0.6; }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 50.dp) // Adjust offset to match CSS 'bottom: -50px; left: -50px' logic roughly
                .size(250.dp)
                .clip(CircleShape)
                .background(Color(0xFFF4F6F8).copy(alpha = 0.6f))
        )

        // --- CONTENT WRAPPER ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp), // padding: 40px 30px
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Logo
            // .logo-img { width: 120px; ... margin-bottom: 40px; }
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "QuackXel Logo",
                modifier = Modifier
                    .width(120.dp)
                    .padding(bottom = 40.dp),
                contentScale = ContentScale.Fit
            )

            // Teks Judul
            // h1 { font-size: 26px; ... margin-bottom: 15px; }
            Text(
                text = "Kelola Dasbor Anda",
                color = PrimaryBlue,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 15.dp)
            )

            // Teks Sub-judul
            // p.subtitle { font-size: 14px; ... margin-bottom: 50px; }
            Text(
                text = "Masuk dengan akun Google Anda untuk melanjutkan.",
                color = TextGrey,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(280.dp) // Max width constraint
                    .padding(bottom = 50.dp)
            )

            // --- GOOGLE BUTTON ---
            if (isLoading) {
                 androidx.compose.material3.CircularProgressIndicator(color = PrimaryBlue)
            } else {
                 GoogleSignInButton(onClick = { onGoogleLoginClick() })
            }
        }
    }
}



// --- DIAGNOSTIC HELPER ---
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

@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    val context = LocalContext.current
    // DEBUG: Get Real SHA-1
    val realSha1 = remember { getAppSignature(context) }
    Log.d("LoginDiag", "REAL SHA-1: $realSha1")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0)),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().height(55.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(15.dp)
            ) {
                 Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.White)
                ) {
                     Text("G", color = GoogleBlue, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Sign in with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3C4043)
                )
            }
        }
        
        // --- VISIBLE DIAGNOSTIC FOR USER ---
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "DEBUG INFO:\nPkg: ${context.packageName}\nSHA-1: $realSha1",
            fontSize = 10.sp,
            color = Color.Red,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier
                .background(Color.Yellow.copy(alpha = 0.3f))
                .padding(4.dp)
                .clickable { 
                    // Copy to clipboard logic could go here, but visual is enough
                    Toast.makeText(context, "Cek SHA-1 ini di Google Console!", Toast.LENGTH_LONG).show()
                }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    KasirTheme {
        LoginScreen()
    }
}
