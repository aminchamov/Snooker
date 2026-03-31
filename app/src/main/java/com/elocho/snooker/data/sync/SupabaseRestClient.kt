package com.elocho.snooker.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class SupabaseAuthToken(
    val accessToken: String,
    val expiresAtMs: Long
)

class SupabaseRestClient {

    suspend fun signInWithPassword(email: String, password: String): SupabaseAuthToken = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()

        val response = request(
            method = "POST",
            path = "/auth/v1/token?grant_type=password",
            bearer = null,
            body = body
        )
        val json = JSONObject(response)
        val token = json.getString("access_token")
        val expiresInSeconds = json.optLong("expires_in", 3600L).coerceAtLeast(60L)
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L) - 15_000L
        SupabaseAuthToken(accessToken = token, expiresAtMs = expiresAt)
    }

    suspend fun fetchRows(tableOrView: String, bearerToken: String? = null): JSONArray = withContext(Dispatchers.IO) {
        val response = request(
            method = "GET",
            path = "/rest/v1/$tableOrView?select=*",
            bearer = bearerToken
        )
        JSONArray(response)
    }

    suspend fun upsertRows(
        table: String,
        rows: JSONArray,
        bearerToken: String
    ) = withContext(Dispatchers.IO) {
        if (rows.length() == 0) return@withContext
        request(
            method = "POST",
            path = "/rest/v1/$table?on_conflict=id",
            bearer = bearerToken,
            body = rows.toString(),
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
        )
    }

    private fun request(
        method: String,
        path: String,
        bearer: String?,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        val url = URL(SupabaseConfig.projectUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 20_000
            doInput = true
            setRequestProperty("apikey", SupabaseConfig.publishableKey)
            setRequestProperty("Authorization", "Bearer ${bearer ?: SupabaseConfig.publishableKey}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            extraHeaders.forEach { (key, value) -> setRequestProperty(key, value) }
        }

        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.use { input ->
            BufferedReader(InputStreamReader(input)).use { reader -> reader.readText() }
        }.orEmpty()

        if (code !in 200..299) {
            throw IllegalStateException("Supabase request failed ($method $path): HTTP $code ${response.ifBlank { "no-body" }}")
        }
        return response
    }
}
