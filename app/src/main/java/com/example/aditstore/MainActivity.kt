package com.example.aditstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Home : Screen
    data class Detail(val product: ProductItem) : Screen
    data class Form(val editing: ProductItem?) : Screen   // null = tambah baru
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.statusBarsPadding(), color = Cream) { App() }
            }
        }
    }
}

@Composable
private fun App() {
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf(emptyList<String>()) }
    var products by remember { mutableStateOf(emptyList<ProductItem>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    // Form state
    var fTitle by remember { mutableStateOf("") }
    var fBrand by remember { mutableStateOf("") }
    var fCategory by remember { mutableStateOf("") }
    var fPrice by remember { mutableStateOf("") }
    var fStock by remember { mutableStateOf("") }
    var fDesc by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ProductItem?>(null) }

    // READ: muat ulang saat query / kategori / reloadKey berubah (search di-debounce 350 ms).
    LaunchedEffect(query, selectedCategory, reloadKey) {
        if (query.isNotEmpty()) delay(350)
        loading = true; error = null
        try {
            products = ApiClient.getProducts(query, selectedCategory)
            categories = ApiClient.getCategories()
        } catch (e: ApiException) {
            error = e.message
        } finally {
            loading = false
        }
    }

    fun openForm(p: ProductItem?) {
        fTitle = p?.title ?: ""; fBrand = p?.brand?.takeIf { it != "-" } ?: ""
        fCategory = p?.category ?: ""
        fPrice = p?.price?.toString() ?: ""; fStock = p?.stock?.toString() ?: ""
        fDesc = p?.description ?: ""
        formError = null; saving = false
        screen = Screen.Form(p)
    }

    fun save(editing: ProductItem?) {
        val price = fPrice.replace(',', '.').toDoubleOrNull()
        val stock = fStock.toIntOrNull()
        formError = when {
            fTitle.isBlank() -> "Nama produk wajib diisi."
            fCategory.isBlank() -> "Kategori wajib diisi."
            price == null || price < 0 -> "Harga harus berupa angka (≥ 0)."
            stock == null || stock < 0 -> "Stok harus berupa bilangan bulat (≥ 0)."
            else -> null
        }
        if (formError != null) return
        val form = ProductForm(fTitle.trim(), fBrand.trim(), fCategory.trim().lowercase().replace(' ', '-'),
            price!!, stock!!, fDesc.trim())
        scope.launch {
            saving = true
            try {
                val result = if (editing == null) ApiClient.createProduct(form)
                else ApiClient.updateProduct(editing.id, form)
                reloadKey++
                screen = if (editing == null) Screen.Home else Screen.Detail(result)
            } catch (e: ApiException) {
                formError = e.message
            } finally {
                saving = false
            }
        }
    }

    BackHandler(enabled = screen !is Screen.Home) {
        screen = when (val s = screen) {
            is Screen.Form -> s.editing?.let { Screen.Detail(it) } ?: Screen.Home
            else -> Screen.Home
        }
    }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            query = query, onQueryChange = { query = it },
            categories = categories, selectedCategory = selectedCategory,
            onCategoryClick = { selectedCategory = it },
            products = products, loading = loading, errorMessage = error,
            onProductClick = { screen = Screen.Detail(it) },
            onRetry = { reloadKey++ },
            onAddClick = { openForm(null) }
        )
        is Screen.Detail -> DetailScreen(
            p = s.product, onBack = { screen = Screen.Home },
            onEdit = { openForm(s.product) },
            onDelete = { deleteTarget = s.product }
        )
        is Screen.Form -> ProductFormScreen(
            heading = if (s.editing == null) "Tambah Produk" else "Edit Produk",
            title = fTitle, onTitleChange = { fTitle = it },
            brand = fBrand, onBrandChange = { fBrand = it },
            category = fCategory, onCategoryChange = { fCategory = it },
            price = fPrice, onPriceChange = { fPrice = it },
            stock = fStock, onStockChange = { fStock = it },
            description = fDesc, onDescriptionChange = { fDesc = it },
            errorMessage = formError, saving = saving,
            onSave = { save(s.editing) },
            onCancel = { screen = s.editing?.let { Screen.Detail(it) } ?: Screen.Home }
        )
    }

    // DELETE: dialog konfirmasi lalu panggil API.
    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            productTitle = target.title,
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    try {
                        ApiClient.deleteProduct(target.id)
                        reloadKey++
                        screen = Screen.Home
                    } catch (e: ApiException) {
                        error = e.message
                        screen = Screen.Home
                    }
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}
