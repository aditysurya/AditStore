package com.example.aditstore

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// File ini HANYA tampilan (stateless). Tidak ada API, parsing, search, filter, atau navigasi.
// Semua data & aksi datang lewat parameter, jadi bagian itu kamu tulis sendiri.

val Ink = Color(0xFF3B3552)
val Lavender = Color(0xFFDCD6F7)
val Mint = Color(0xFFCDEFE3)
val Peach = Color(0xFFFFD9C7)
val Sky = Color(0xFFCFE8FF)
val Butter = Color(0xFFFFF3C4)
val Pink = Color(0xFFFBD5E5)
val Cream = Color(0xFFFFFBF7)
val Pastels = listOf(Mint, Peach, Sky, Butter, Pink, Lavender)

/** Model khusus tampilan. Isi dari data API milikmu (mapping kamu tulis sendiri). */
data class ProductItem(
    val id: Int, val title: String, val description: String, val category: String,
    val price: Double, val discount: Double, val finalPrice: Double,
    val rating: Double, val stock: Int, val brand: String,
    val thumbnail: String, val images: List<String>, val tags: List<String>
)

@Composable
fun Logo(size: Int) =
    Image(painterResource(R.drawable.adit_logo), "Logo Adit Store", Modifier.size(size.dp))

@Composable
private fun HomeContent(
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategoryClick: (String?) -> Unit,
    products: List<ProductItem>,
    loading: Boolean,
    errorMessage: String?,
    onProductClick: (ProductItem) -> Unit,
    onRetry: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Logo(48)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Adit Store", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Toko komputer & gadget pilihan", fontSize = 13.sp, color = Ink.copy(0.6f))
            }
        }
        OutlinedTextField(
            value = query, onValueChange = onQueryChange, singleLine = true,
            placeholder = { Text("Cari produk…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton({ onQueryChange("") }) { Icon(Icons.Default.Close, "Hapus") }
            },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Lavender, unfocusedContainerColor = Lavender,
                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        if (categories.isNotEmpty()) LazyRow(
            contentPadding = PaddingValues(16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Chip("Semua", selectedCategory == null) { onCategoryClick(null) } }
            items(categories) { c -> Chip(c.replace("-", " "), selectedCategory == c) { onCategoryClick(c) } }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Ink) }
            errorMessage != null -> Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(errorMessage, color = Ink); Spacer(Modifier.height(8.dp))
                Button(onRetry, colors = ButtonDefaults.buttonColors(Peach, Ink)) { Text("Coba lagi") }
            }
            products.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Produk tidak ditemukan.", color = Ink.copy(0.7f))
            }
            else -> LazyVerticalGrid(
                GridCells.Fixed(2), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products.size) { i ->
                    ProductCard(products[i], Pastels[i % Pastels.size]) { onProductClick(products[i]) }
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text, fontSize = 13.sp, color = Ink, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (active) Peach else Mint)
            .clickable(onClick = onClick).padding(14.dp, 8.dp)
    )
}

@Composable
fun ProductCard(p: ProductItem, bg: Color, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(24.dp)).background(bg).clickable(onClick = onClick).padding(10.dp)) {
        AsyncImage(p.thumbnail, p.title, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.5f)), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(8.dp))
        Text(p.title, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$${"%.2f".format(p.price)}", fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = Color(0xFFE8A23B))
            Text(" ${"%.1f".format(p.rating)}", fontSize = 12.sp, color = Ink)
        }
    }
}

@Composable
fun DetailScreen(p: ProductItem, onBack: () -> Unit, onEdit: () -> Unit = {}, onDelete: () -> Unit = {}) {
    var img by remember(p.id) { mutableStateOf(p.images.firstOrNull() ?: p.thumbnail) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(28.dp)).background(Lavender)) {
            AsyncImage(img, p.title, Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp), contentScale = ContentScale.Fit)
            IconButton(onBack, Modifier.padding(8.dp).clip(CircleShape).background(Color.White.copy(0.8f))) {
                Icon(Icons.Default.ArrowBack, "Kembali", tint = Ink)
            }
        }
        if (p.images.size > 1) LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(p.images) { u ->
                AsyncImage(u, null, Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(if (u == img) Peach else Mint)
                    .clickable { img = u }.padding(4.dp))
            }
        }
        Column(Modifier.padding(20.dp)) {
            Text(p.category.replace("-", " "), color = Ink.copy(0.6f), fontSize = 13.sp)
            Text(p.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("oleh ${p.brand}", color = Ink.copy(0.7f), fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$${"%.2f".format(p.finalPrice)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.width(10.dp))
                Text("$${"%.2f".format(p.price)}", color = Ink.copy(0.5f), textDecoration = TextDecoration.LineThrough)
                Spacer(Modifier.width(10.dp))
                Text("-${"%.0f".format(p.discount)}%", Modifier.clip(RoundedCornerShape(50)).background(Pink).padding(10.dp, 4.dp), color = Ink, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoBox("Rating", "★ ${"%.1f".format(p.rating)}", Butter, Modifier.weight(1f))
                InfoBox("Stok", "${p.stock}", Mint, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Text("Deskripsi", fontWeight = FontWeight.Bold, color = Ink)
            Text(p.description, color = Ink.copy(0.8f), lineHeight = 22.sp)
            if (p.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { p.tags.forEach { Chip(it, false) {} } }
            }
            Spacer(Modifier.height(20.dp))
            Button({}, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(Peach, Ink)) { Text("Tambah ke keranjang", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onEdit, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(Sky, Ink)) { Text("Edit") }
                Button(onDelete, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(Pink, Ink)) { Text("Hapus") }
            }
        }
    }
}

@Composable
fun InfoBox(label: String, value: String, bg: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(14.dp)) {
        Text(label, fontSize = 12.sp, color = Ink.copy(0.6f))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

/** Pembungkus beranda + tombol tambah (+). Tetap stateless. */
@Composable
fun HomeScreen(
    query: String, onQueryChange: (String) -> Unit, categories: List<String>,
    selectedCategory: String?, onCategoryClick: (String?) -> Unit, products: List<ProductItem>,
    loading: Boolean, errorMessage: String?, onProductClick: (ProductItem) -> Unit,
    onRetry: () -> Unit, onAddClick: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize()) {
        HomeContent(query, onQueryChange, categories, selectedCategory, onCategoryClick,
            products, loading, errorMessage, onProductClick, onRetry)
        FloatingActionButton(onAddClick, Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = Peach, contentColor = Ink) { Icon(Icons.Default.Add, "Tambah produk") }
    }
}
