package com.nexusdeckpro.ninjasdk

import android.content.Context
import android.provider.Settings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class NinjaHandshake(private val context: Context) {

    private val client = OkHttpClient()

    fun getHardwareId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    private fun hmacSha256(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(NinjaMagic.config().signingSecret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun validateLicense(licenseKey: String, callback: (Boolean, String?) -> Unit) {
        val cfg = NinjaMagic.config()
        val hwid = getHardwareId()
        val timestamp = System.currentTimeMillis()
        val model = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL

        val payload = JSONObject().apply {
            put("license_key", licenseKey)
            put("hwid", hwid)
            put("timestamp", timestamp)
            put("device_model", model)
            put("app_name", cfg.appName)
        }

        val body = JSONObject().apply {
            put("payload", payload)
            put("signature", hmacSha256(payload.toString()))
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${cfg.serverUrl}/api/v1/validate")
            .post(body).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                callback(false, "Sin conexión con el servidor")
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val raw = response.body?.string() ?: "{}"
                val json = JSONObject(raw)
                if (response.isSuccessful) {
                    saveToken(json.getString("token"))
                    callback(true, null)
                } else {
                    callback(false, json.optString("error", "Licencia inválida"))
                }
            }
        })
    }

    fun autoReconnect(callback: (Boolean, String?) -> Unit) {
        val cfg = NinjaMagic.config()
        val hwid = getHardwareId()
        val timestamp = System.currentTimeMillis()
        val payloadStr = """{"hwid":"$hwid","timestamp":$timestamp}"""

        val body = JSONObject().apply {
            put("hwid", hwid)
            put("timestamp", timestamp)
            put("signature", hmacSha256(payloadStr))
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${cfg.serverUrl}/api/v1/device/reconnect")
            .post(body).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) { callback(false, null) }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val raw = response.body?.string() ?: "{}"
                if (response.isSuccessful) {
                    val token = JSONObject(raw).optString("token")
                    if (token.isNotEmpty()) { saveToken(token); callback(true, null) }
                    else callback(false, null)
                } else {
                    val error = JSONObject(raw).optString("error", null)
                    callback(false, error)
                }
            }
        })
    }

    fun isActivated(): Boolean =
        !context.getSharedPreferences("ninja_prefs", Context.MODE_PRIVATE)
            .getString("auth_token", null).isNullOrEmpty()

    fun logout() {
        context.getSharedPreferences("ninja_prefs", Context.MODE_PRIVATE)
            .edit().remove("auth_token").apply()
    }

    private fun saveToken(token: String) {
        context.getSharedPreferences("ninja_prefs", Context.MODE_PRIVATE)
            .edit().putString("auth_token", token).apply()
    }
}
