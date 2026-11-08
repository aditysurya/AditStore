package com.example.aditstore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tampilan form Tambah/Edit & dialog Hapus. HANYA UI: tidak ada validasi, API, atau penyimpanan.
// Nilai field, pesan error, status saving, dan aksi simpan semuanya datang dari luar.

@Composable
fun ProductFormScreen(
    heading: String,                       // contoh: "Tambah Produk" / "Edit Produk"
    title: String, onTitleChange: (String) -> Unit,
    brand: String, onBrandChange: (String) -> Unit,
    category: String, onCategoryChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit,
    stock: String, onStockChange: (String) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit,
    errorMessage: String?, saving: Boolean,
    onSave: () -> Unit, onCancel: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onCancel) { Icon(Icons.Default.ArrowBack, "Kembali", tint = Ink) }
            Text(heading, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Spacer(Modifier.height(12.dp))
        FormField("Nama produk", title, onTitleChange)
        FormField("Brand", brand, onBrandChange)
        FormField("Kategori", category, onCategoryChange)
        FormField("Harga", price, onPriceChange, KeyboardType.Decimal)
        FormField("Stok", stock, onStockChange, KeyboardType.Number)
        FormField("Deskripsi", description, onDescriptionChange, singleLine = false)
        if (errorMessage != null) {
            Text(errorMessage, color = Color(0xFFB3261E), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(onSave, Modifier.fillMaxWidth().height(52.dp), enabled = !saving, shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(Peach, Ink)) {
            if (saving) CircularProgressIndicator(Modifier.size(22.dp), color = Ink, strokeWidth = 2.dp)
            else Text("Simpan", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FormField(
    label: String, value: String, onChange: (String) -> Unit,
    type: KeyboardType = KeyboardType.Text, singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Lavender, unfocusedContainerColor = Lavender,
            focusedBorderColor = Ink, unfocusedBorderColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    )
}

@Composable
fun DeleteConfirmDialog(productTitle: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Cream,
        title = { Text("Hapus produk?", color = Ink, fontWeight = FontWeight.Bold) },
        text = { Text("\"$productTitle\" akan dihapus.", color = Ink.copy(0.8f)) },
        confirmButton = { TextButton(onConfirm) { Text("Hapus", color = Color(0xFFB3261E)) } },
        dismissButton = { TextButton(onDismiss) { Text("Batal", color = Ink) } }
    )
}
