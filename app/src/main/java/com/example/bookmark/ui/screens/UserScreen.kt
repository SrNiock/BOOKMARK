package com.example.bookmark.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.supaBase.Usuario
import com.example.bookmark.ui.utils.SessionManager
import kotlinx.coroutines.launch

@Composable
fun UserScreen() {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }
    val correoActual = sessionManager.obtenerCorreoSesion() ?: ""

    // Estados
    var bannerUrl by remember { mutableStateOf<String?>(null) }
    var profileUrl by remember { mutableStateOf<String?>(null) }
    var nicknameUsuario by remember { mutableStateOf("Usuario") }
    var estaCargando by remember { mutableStateOf(false) }

    // CARGA DE DATOS SEGURA
    LaunchedEffect(correoActual) {
        if (correoActual.isNotEmpty()) {
            authRepository.obtenerUsuario(correoActual).onSuccess { usuario ->
                nicknameUsuario = usuario.nickname
                bannerUrl = usuario.fotoBanner
                profileUrl = usuario.fotoPerfil
            }.onFailure {
                nicknameUsuario = "Error al cargar perfil"
            }
        }
    }

    fun manejarSubida(uri: Uri, esBanner: Boolean) {
        coroutineScope.launch {
            estaCargando = true // Empieza la barra
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val nombreArchivo = if (esBanner) "banner_${correoActual}.jpg" else "perfil_${correoActual}.jpg"

                    val resultado = authRepository.subirImagenStorage(nombreArchivo, bytes)

                    resultado.onSuccess { url ->
                        authRepository.actualizarFotoTabla(correoActual, if (esBanner) "fotoBanner" else "fotoPerfil", url)
                        if (esBanner) bannerUrl = url else profileUrl = url
                    }.onFailure {
                        println("Error en la subida")
                    }
                }
            } catch (e: Exception) {
                println("Error de lectura de archivo")
            } finally {
                estaCargando = false // ¡ESTO ES CLAVE! Pase lo que pase, la barra se quita aquí
            }
        }
    }

    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let { manejarSubida(it, true) } }
    val profileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let { manejarSubida(it, false) } }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).verticalScroll(scrollState)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            // Banner
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp).background(Color.DarkGray)
                    .clickable { bannerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            ) {
                AsyncImage(model = bannerUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }

            // Foto Perfil
            Box(
                modifier = Modifier.size(110.dp).align(Alignment.BottomStart).offset(x = 24.dp, y = 10.dp)
                    .clip(CircleShape).background(Color.Gray).border(3.dp, Color(0xFF00E5FF), CircleShape)
                    .clickable { profileLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            ) {
                AsyncImage(model = profileUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }

        if (estaCargando) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF00E5FF))

        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = nicknameUsuario, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))

            // BOTÓN DE CERRAR SESIÓN (Para limpiar el crasheo si el correo estaba mal)
            Button(
                onClick = {
                    sessionManager.cerrarSesion()
                    // Aquí deberías navegar al Login, pero para probar simplemente cierra la sesión
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar Sesión")
            }
        }
    }
}