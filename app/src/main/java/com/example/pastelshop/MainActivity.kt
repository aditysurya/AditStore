package com.example.pastelshop // <-- ganti sesuai package proyek Anda

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/* ------------------------------------------------------------------ */
/*  Palet pastel                                                       */
/* ------------------------------------------------------------------ */
object Pastel {
    val Bg = Color(0xFFFFF8F3)
    val Ink = Color(0xFF4A4458)
    val InkSoft = Color(0xFF8C8699)
    val Pink = Color(0xFFFFD6E0)
    val Peach = Color(0xFFFFE5D0)
    val Yellow = Color(0xFFFFF4C2)
    val Mint = Color(0xFFD4F1E4)
    val Sky = Color(0xFFD6E8FF)
    val Lilac = Color(0xFFE6DAFB)
    val Accent = Color(0xFFB39DDB)
    val Star = Color(0xFFFFB74D)
    private val cards = listOf(Pink, Peach, Yellow, Mint, Sky, Lilac)
    fun card(i: Int) = cards[i % cards.size]
}

/* ------------------------------------------------------------------ */
/*  Model                                                              */
/* ------------------------------------------------------------------ */
data class Review(val rating: Int, val comment: String, val reviewerName: String)

data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val brand: String?,
    val thumbnail: String,
    val price: Double,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val availabilityStatus: String?,
    val shippingInformation: String?,
    val warrantyInformation: String?,
    val images: List<String>,
    val tags: List<String>,
    val reviews: List<Review>,
)

private fun JSONObject.strList(key: String): List<String> {
    val a = optJSONArray(key) ?: return emptyList()
    return List(a.length()) { a.getString(it) }
}

private fun JSONObject.toProduct(): Product {
    val rv = optJSONArray("reviews")
    return Product(
        id = getInt("id"),
        title = optString("title"),
        description = optString("description"),
        category = optString("category"),
        brand = optString("brand").ifBlank { null },
        thumbnail = optString("thumbnail"),
        price = optDouble("price", 0.0),
        discountPercentage = optDouble("discountPercentage", 0.0),
        rating = optDouble("rating", 0.0),
        stock = optInt("stock", 0),
        availabilityStatus = optString("availabilityStatus").ifBlank { null },
        shippingInformation = optString("shippingInformation").ifBlank { null },
        warrantyInformation = optString("warrantyInformation").ifBlank { null },
        images = strList("images"),
        tags = strList("tags"),
        reviews = if (rv == null) emptyList() else List(rv.length()) {
            val r = rv.getJSONObject(it)
            Review(r.optInt("rating"), r.optString("comment"), r.optString("reviewerName"))
        },
    )
}

/* ------------------------------------------------------------------ */
/*  API (https://dummyjson.com/docs/products)                          */
/* ------------------------------------------------------------------ */
object Api {
    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        try {
            if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun products(query: String): List<Product> {
        val url = if (query.isBlank()) "https://dummyjson.com/products?limit=0"
        else "https://dummyjson.com/products/search?limit=0&q=" + URLEncoder.encode(query, "UTF-8")
        val arr = JSONObject(get(url)).getJSONArray("products")
        return List(arr.length()) { arr.getJSONObject(it).toProduct() }
    }

    suspend fun detail(id: Int): Product =
        JSONObject(get("https://dummyjson.com/products/$id")).toProduct()
}

/* ------------------------------------------------------------------ */
/*  ViewModel                                                          */
/* ------------------------------------------------------------------ */
sealed interface UiState {
    object Loading : UiState
    object Error : UiState
    class Success(val items: List<Product>) : UiState
}

class HomeViewModel : ViewModel() {
    var query by mutableStateOf("")
        private set
    var state by mutableStateOf<UiState>(UiState.Loading)
        private set
    private var job: Job? = null

    init { load() }

    /** Dipanggil tiap teks berubah (debounce 450 ms). */
    fun onQueryChange(q: String) {
        query = q
        job?.cancel()
        job = viewModelScope.launch { delay(450); fetch() }
    }

    /** Dipanggil langsung (tombol search keyboard / coba lagi). */
    fun load() {
        job?.cancel()
        job = viewModelScope.launch { fetch() }
    }

    fun clear() = onQueryChange("").also { load() }

    private suspend fun fetch() {
        state = UiState.Loading
        state = try {
            UiState.Success(Api.products(query.trim()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiState.Error
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Activity + Navigasi                                                */
/* ------------------------------------------------------------------ */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Pastel.Accent,
                    background = Pastel.Bg,
                    surface = Pastel.Bg,
                    onBackground = Pastel.Ink,
                    onSurface = Pastel.Ink,
                )
            ) {
                val nav = rememberNavController()
                val vm: HomeViewModel = viewModel()
                NavHost(nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(vm) { p, colorIdx ->
                            nav.navigate("detail/${p.id}/$colorIdx")
                        }
                    }
                    composable(
                        "detail/{id}/{color}",
                        arguments = listOf(
                            navArgument("id") { type = NavType.IntType },
                            navArgument("color") { type = NavType.IntType },
                        ),
                    ) { entry ->
                        val id = entry.arguments!!.getInt("id")
                        val colorIdx = entry.arguments!!.getInt("color")
                        val preview = (vm.state as? UiState.Success)?.items?.find { it.id == id }
                        DetailScreen(id, preview, Pastel.card(colorIdx)) { nav.popBackStack() }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Halaman depan                                                      */
/* ------------------------------------------------------------------ */
@Composable
fun HomeScreen(vm: HomeViewModel, onOpen: (Product, Int) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Pastel.Bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Halo 👋", color = Pastel.InkSoft, fontSize = 14.sp)
                Text(
                    "Temukan produk\nfavoritmu",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 28.sp,
                    color = Pastel.Ink,
                )
            }
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Pastel.Lilac),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.ShoppingCart, null, tint = Pastel.Ink) }
        }

        // ---- Search ----
        TextField(
            value = vm.query,
            onValueChange = vm::onQueryChange,
            placeholder = { Text("Cari produk...", color = Pastel.InkSoft) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Pastel.InkSoft) },
            trailingIcon = {
                if (vm.query.isNotEmpty()) {
                    IconButton(onClick = { vm.clear() }) {
                        Icon(Icons.Default.Close, "Hapus", tint = Pastel.InkSoft)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.load() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Pastel.Sky.copy(alpha = .6f),
                unfocusedContainerColor = Pastel.Sky.copy(alpha = .6f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Pastel.Ink,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
        )

        when (val s = vm.state) {
            UiState.Loading -> CenterBox { CircularProgressIndicator(color = Pastel.Accent) }
            UiState.Error -> CenterBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📡", fontSize = 44.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Gagal memuat produk.\nPeriksa koneksi internet.",
                        color = Pastel.InkSoft,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = { vm.load() }) { Text("Coba lagi", color = Pastel.Accent) }
                }
            }
            is UiState.Success ->
                if (s.items.isEmpty()) {
                    CenterBox {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 44.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Produk tidak ditemukan", color = Pastel.InkSoft)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.navigationBarsPadding(),
                    ) {
                        items(s.items.size, key = { s.items[it].id }) { i ->
                            ProductCard(s.items[i], Pastel.card(i)) { onOpen(s.items[i], i) }
                        }
                    }
                }
        }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }

@Composable
fun ProductCard(p: Product, color: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(.68f)
            .clip(RoundedCornerShape(26.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = .55f))
        ) {
            AsyncImage(
                model = p.thumbnail,
                contentDescription = p.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
            )
            if (p.discountPercentage >= 1) {
                Text(
                    "-${p.discountPercentage.toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pastel.Ink,
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            p.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Pastel.Ink,
        )
        Text(
            p.category, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp, color = Pastel.InkSoft,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$%.2f".format(p.price),
                fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                color = Pastel.Ink, modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.Star, null, tint = Pastel.Star, modifier = Modifier.size(16.dp))
            Text(" %.1f".format(p.rating), fontSize = 12.sp, color = Pastel.Ink)
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Halaman detail                                                     */
/* ------------------------------------------------------------------ */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(id: Int, preview: Product?, color: Color, onBack: () -> Unit) {
    val full by produceState<Product?>(initialValue = null, key1 = id) {
        value = try { Api.detail(id) } catch (e: CancellationException) { throw e } catch (e: Exception) { null }
    }
    val p = full ?: preview
    val context = LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
            .background(color)
    ) {
        if (p == null) {
            CenterBox { CircularProgressIndicator(color = Pastel.Accent) }
        } else {
            val images = p.images.ifEmpty { listOf(p.thumbnail) }
            val pager = rememberPagerState(pageCount = { images.size })
            val oldPrice =
                if (p.discountPercentage > 0) p.price / (1 - p.discountPercentage / 100) else null

            // ---- Galeri gambar ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.44f)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                    AsyncImage(
                        model = images[page],
                        contentDescription = p.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 40.dp, end = 40.dp, top = 56.dp, bottom = 8.dp),
                    )
                }
                if (images.size > 1) {
                    Row(Modifier.padding(bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(images.size) { i ->
                            val active = pager.currentPage == i
                            Box(
                                Modifier
                                    .height(6.dp)
                                    .width(if (active) 18.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (active) Pastel.Ink else Pastel.Ink.copy(alpha = .25f))
                            )
                        }
                    }
                }
            }

            // ---- Tombol kembali ----
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = .8f)),
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Pastel.Ink) }

            // ---- Panel informasi ----
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(.58f)
                    .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                    .background(Pastel.Bg)
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(p.title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Pastel.Ink)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                listOfNotNull(p.brand, p.category).joinToString(" • "),
                                fontSize = 13.sp, color = Pastel.InkSoft,
                            )
                        }
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Pastel.Yellow)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Star, null, tint = Pastel.Star, modifier = Modifier.size(18.dp))
                            Text(" %.1f".format(p.rating), fontWeight = FontWeight.Bold, color = Pastel.Ink)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$%.2f".format(p.price), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Pastel.Ink)
                        if (oldPrice != null) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "$%.2f".format(oldPrice),
                                color = Pastel.InkSoft,
                                textDecoration = TextDecoration.LineThrough,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "-${p.discountPercentage.toInt()}%",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Pastel.Ink,
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Pastel.Pink)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(Pastel.Mint, "📦 ${p.availabilityStatus ?: "Stok"}: ${p.stock}")
                        p.shippingInformation?.let { InfoChip(Pastel.Sky, "🚚 $it") }
                        p.warrantyInformation?.let { InfoChip(Pastel.Lilac, "✅ $it") }
                    }

                    Spacer(Modifier.height(22.dp))
                    Text("Deskripsi", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Pastel.Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(p.description, color = Pastel.InkSoft, lineHeight = 22.sp)

                    if (p.tags.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.tags.forEachIndexed { i, t -> InfoChip(Pastel.card(i + 1), "#$t") }
                        }
                    }

                    if (p.reviews.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("Ulasan", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Pastel.Ink)
                        Spacer(Modifier.height(10.dp))
                        p.reviews.forEachIndexed { i, r ->
                            Column(
                                Modifier
                                    .padding(bottom = 10.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Pastel.card(i).copy(alpha = .55f))
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(r.reviewerName, fontWeight = FontWeight.Bold, color = Pastel.Ink, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.Star, null, tint = Pastel.Star, modifier = Modifier.size(16.dp))
                                    Text(" ${r.rating}", color = Pastel.Ink)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(r.comment, fontSize = 13.sp, color = Pastel.Ink)
                            }
                        }
                    }

                    if (full == null) {
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Pastel.Accent, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                Button(
                    onClick = { Toast.makeText(context, "Ditambahkan ke keranjang", Toast.LENGTH_SHORT).show() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pastel.Accent, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
                        .navigationBarsPadding()
                        .height(54.dp),
                ) {
                    Icon(Icons.Default.ShoppingCart, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tambah ke keranjang", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(bg: Color, text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = Pastel.Ink,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
