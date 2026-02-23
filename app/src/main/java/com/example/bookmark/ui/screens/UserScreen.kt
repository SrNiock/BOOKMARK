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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// IMPORTANTE: Este es el import de la librería que acabamos de añadir
import coil.compose.AsyncImage

@Composable
fun UserScreen() {
    val scrollState = rememberScrollState()

    // --- ESTADOS DE LA PANTALLA ---
    // Aquí guardaremos temporalmente la ruta de la foto elegida para mostrarla
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var profileUri by remember { mutableStateOf<Uri?>(null) }

    // Aquí guardaremos el Nickname (de momento ponemos uno de prueba, luego lo traeremos de la BD)
    var nicknameUsuario by remember { mutableStateOf("Cargando...") }

    // --- LANZADORES DE LA GALERÍA ---
    // 1. Para el Banner
    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            bannerUri = uri
            // TODO: Más adelante, aquí subiremos la foto a Supabase Storage
            println("Banner seleccionado: $uri")
        }
    }

    // 2. Para la Foto de Perfil
    val profilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            profileUri = uri
            // TODO: Más adelante, aquí subiremos la foto a Supabase Storage
            println("Perfil seleccionado: $uri")
        }
    }

    // Colores
    val backgroundColor = Color(0xFF121212)
    val surfaceColor = Color(0xFF1E1E1E)
    val accentColor = Color(0xFF00E5FF)

    // Simulamos que cargamos el nickname desde la base de datos
    LaunchedEffect(Unit) {
        // TODO: Llamaremos a authRepository.obtenerUsuario()
        // De momento, lo fingimos:
        nicknameUsuario = "PauLibros" // El nickname de tu captura
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(scrollState)
    ) {
        // --- SECCIÓN 1: BANNER Y FOTO DE PERFIL ---
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp)
        ) {
            // 1.1 El Banner (Fondo)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.DarkGray)
                    .clickable {
                        // ¡MAGIA! Esto abre la galería pidiendo solo imágenes
                        bannerPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bannerUri != null) {
                    // Si hay foto, la pintamos ocupando todo el espacio
                    AsyncImage(
                        model = bannerUri,
                        contentDescription = "Banner del usuario",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // Recorta la foto para que encaje perfecto
                    )
                } else {
                    Text("Toca para añadir un Banner", color = Color.White)
                }
            }

            // 1.2 La Foto de Perfil (Superpuesta)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 24.dp, y = 10.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .border(3.dp, accentColor, CircleShape)
                    .clickable {
                        // Abre la galería para el perfil
                        profilePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (profileUri != null) {
                    AsyncImage(
                        model = profileUri,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Foto", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN 2: INFO DEL USUARIO ---
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = nicknameUsuario, // Usamos la variable que se actualizará con la BD
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Sci-Fi Enthusiast • Night Owl • Always looking for the next great space opera.",
                fontSize = 14.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SECCIÓN 3: AMISTADES ---
        Text(
            text = "Amistades", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor)
        ) {
            Text(
                text = "Aún no tienes amigos añadidos. ¡Pronto podrás buscarlos aquí!",
                color = Color.Gray, modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SECCIÓN 4: LOS 4 LIBROS FAVORITOS ---
        Text(
            text = "Mis 4 Favoritos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BookPlaceholderBox(modifier = Modifier.weight(1f))
            BookPlaceholderBox(modifier = Modifier.weight(1f))
            BookPlaceholderBox(modifier = Modifier.weight(1f))
            BookPlaceholderBox(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun BookPlaceholderBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("Libro", color = Color.Gray, fontSize = 12.sp)
    }
}