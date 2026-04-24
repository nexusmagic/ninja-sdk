package com.nexusdeckpro.ninjasdk

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class NinjaActivationActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var emailInput: EditText
    private lateinit var keyInput: EditText
    private lateinit var btnSolicitar: Button
    private lateinit var btnAcceder: Button
    private lateinit var loadingBar: ProgressBar
    private lateinit var statusText: TextView

    private val cfg get() = NinjaMagic.config()
    private val accent get() = try { Color.parseColor(cfg.accentColor) } catch (e: Exception) { Color.parseColor("#D4AF37") }
    private val bg get() = try { Color.parseColor(cfg.bgColor) } catch (e: Exception) { Color.parseColor("#0A0A0F") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUI()
    }

    private fun buildUI() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(64, 80, 64, 64)
            minimumHeight = resources.displayMetrics.heightPixels
        }

        root.addView(TextView(this).apply {
            text = "🔑"; textSize = 56f; gravity = Gravity.CENTER; setPadding(0, 0, 0, 16)
        })
        root.addView(TextView(this).apply {
            text = cfg.appName; textSize = 26f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent); gravity = Gravity.CENTER; letterSpacing = 0.05f
        })
        root.addView(TextView(this).apply {
            text = "Introduce tu email para recibir tu clave"
            textSize = 13f; setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER; setPadding(0, 10, 0, 48)
        })

        emailInput = EditText(this).apply {
            hint = "tu@email.com"
            setHintTextColor(Color.parseColor("#444444"))
            setTextColor(Color.WHITE); textSize = 15f
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or android.text.InputType.TYPE_CLASS_TEXT
            setBackgroundColor(Color.parseColor("#16161E")); setPadding(32, 32, 32, 32)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 12 }
        }
        root.addView(emailInput)

        btnSolicitar = Button(this).apply {
            text = "ENVIAR CLAVE AL EMAIL"
            setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E1E2E"))
            textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 36 }
            setOnClickListener { requestKey() }
        }
        root.addView(btnSolicitar)

        root.addView(TextView(this).apply {
            text = "— o introduce tu clave directamente —"
            textSize = 11f; setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 24)
        })

        keyInput = EditText(this).apply {
            hint = "XXXXXXXX-XXXXXXXX"
            setHintTextColor(Color.parseColor("#444444"))
            setTextColor(accent); textSize = 20f; gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            setBackgroundColor(Color.parseColor("#16161E")); setPadding(32, 32, 32, 32)
            inputType = android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or android.text.InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 12 }
        }
        root.addView(keyInput)

        btnAcceder = Button(this).apply {
            text = "ACTIVAR"; setTextColor(bg); setBackgroundColor(accent)
            textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener { activar() }
        }
        root.addView(btnAcceder)

        loadingBar = ProgressBar(this).apply {
            visibility = View.GONE
            indeterminateTintList = android.content.res.ColorStateList.valueOf(accent)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL; topMargin = 24
            }
        }
        root.addView(loadingBar)

        statusText = TextView(this).apply {
            textSize = 13f; gravity = Gravity.CENTER; setPadding(0, 16, 0, 0); visibility = View.GONE
        }
        root.addView(statusText)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun requestKey() {
        val email = emailInput.text.toString().trim().lowercase()
        if (email.isEmpty() || !email.contains("@")) { showStatus("Introduce un email válido", false); return }
        setLoading(true)
        val body = JSONObject().apply {
            put("email", email); put("app_name", cfg.appName)
        }.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("${cfg.serverUrl}/api/v1/auth/request-activation").post(body).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { setLoading(false); showStatus("Sin conexión", false) }
            }
            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "{}")
                runOnUiThread {
                    setLoading(false)
                    if (response.isSuccessful) {
                        showStatus("✅ Clave enviada. Revisa tu email.", true)
                        btnSolicitar.text = "REENVIAR CLAVE"
                    } else showStatus(json.optString("error", "Error desconocido"), false)
                }
            }
        })
    }

    private fun activar() {
        val key = keyInput.text.toString().trim().uppercase()
        if (key.isEmpty()) { showStatus("Introduce tu clave de acceso", false); return }
        setLoading(true)
        NinjaHandshake(this).validateLicense(key) { success, error ->
            runOnUiThread {
                setLoading(false)
                if (success) { startActivity(Intent(this, cfg.mainActivity)); finish() }
                else showStatus(error ?: "Clave incorrecta o expirada", false)
            }
        }
    }

    private fun setLoading(on: Boolean) {
        btnSolicitar.isEnabled = !on; btnAcceder.isEnabled = !on
        loadingBar.visibility = if (on) View.VISIBLE else View.GONE
        statusText.visibility = View.GONE
    }

    private fun showStatus(msg: String, ok: Boolean) {
        statusText.text = msg
        statusText.setTextColor(if (ok) Color.parseColor("#4ADE80") else Color.parseColor("#F87171"))
        statusText.visibility = View.VISIBLE
    }
}
