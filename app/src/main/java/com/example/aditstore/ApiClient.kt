package com.example.aditstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Klien REST untuk backend PHP (Caddy :8080). Base URL diatur di app/build.gradle.kts (API_BASE_URL). */
class ApiException(message: String) : IOException(message)

object ApiClient {
    private val base = BuildConfig.API_BASE_URL.trimEnd('/')

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** Satu fungsi request untuk semua method (GET/POST/PUT/PATCH/DELETE). Mengembalikan body JSON. */
    private suspend fun request(method: String, path: String, body: JSONObject? = null): String =
        withContext(Dispatchers.IO) {
            val conn = (URL(base + path).openConnection() as HttpURLConnection)
            try {
                conn.requestMethod = method
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Accept", "application/json")
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val msg = runCatching { JSONObject(text).optString("message", "") }.getOrDefault("")
                    throw ApiException(msg.ifBlank { "Server error ($code)" })
                }
                text
            } catch (e: ApiException) {
                throw e
            } catch (e: IOException) {
                throw ApiException("Tidak bisa terhubung ke server. Pastikan backend berjalan di $base")
            } finally {
                conn.disconnect()
            }
        }

    // READ
    suspend fun getProducts(query: String = "", category: String? = null): List<ProductItem> {
        val qs = buildString {
            append("?limit=0&sortBy=id&order=desc")
            if (query.isNotBlank()) append("&q=").append(enc(query.trim()))
            if (!category.isNullOrBlank()) append("&category=").append(enc(category))
        }
        val arr = JSONObject(request("GET", "/products$qs")).getJSONArray("products")
        return List(arr.length()) { parse(arr.getJSONObject(it)) }
    }

    suspend fun getCategories(): List<String> {
        val arr = JSONArray(request("GET", "/products/category-list"))
        return List(arr.length()) { arr.getString(it) }
    }

    // CREATE
    suspend fun createProduct(f: ProductForm): ProductItem =
        parse(JSONObject(request("POST", "/products/add", f.toJson())))

    // UPDATE
    suspend fun updateProduct(id: Int, f: ProductForm): ProductItem =
        parse(JSONObject(request("PUT", "/products/$id", f.toJson())))

    // DELETE
    suspend fun deleteProduct(id: Int) {
        request("DELETE", "/products/$id")
    }

    private fun parse(o: JSONObject): ProductItem {
        val price = o.optDouble("price", 0.0).let { if (it.isNaN()) 0.0 else it }
        val discount = o.optDouble("discountPercentage", 0.0).let { if (it.isNaN()) 0.0 else it }
        fun strings(a: JSONArray?) = if (a == null) emptyList() else List(a.length()) { a.optString(it) }
        return ProductItem(
            id = o.getInt("id"),
            title = o.optString("title"),
            description = o.optString("description").takeIf { !o.isNull("description") } ?: "",
            category = o.optString("category").takeIf { !o.isNull("category") } ?: "",
            price = price,
            discount = discount,
            finalPrice = price * (1 - discount / 100.0),
            rating = o.optDouble("rating", 0.0).let { if (it.isNaN()) 0.0 else it },
            stock = o.optInt("stock", 0),
            brand = o.optString("brand").takeIf { !o.isNull("brand") } ?: "-",
            thumbnail = o.optString("thumbnail").takeIf { !o.isNull("thumbnail") } ?: "",
            images = strings(o.optJSONArray("images")),
            tags = strings(o.optJSONArray("tags"))
        )
    }
}

/** Nilai mentah dari form Tambah/Edit. */
data class ProductForm(
    val title: String, val brand: String, val category: String,
    val price: Double, val stock: Int, val description: String
) {
    fun toJson() = JSONObject()
        .put("title", title).put("brand", brand).put("category", category)
        .put("price", price).put("stock", stock).put("description", description)
}
