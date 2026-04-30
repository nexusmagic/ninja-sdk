package com.nexusdeckpro.ninjasdk

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class NinjaSplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildSplashUI()

        val handshake = NinjaHandshake(this)
        if (handshake.isActivated()) {
            handshake.autoReconnect { success, error ->
                runOnUiThread {
                    when {
                        success -> {
                            startActivity(Intent(this, NinjaMagic.config().mainActivity))
                            finish()
                        }
                        error != null -> {
                            // Licencia o trial expirado — bloquear la app con el mensaje del servidor
                            handshake.logout()
                            showBlockedDialog(error)
                        }
                        else -> {
                            // Sin conexión u error desconocido — volver a activación
                            handshake.logout()
                            startActivity(Intent(this, NinjaActivationActivity::class.java))
                            finish()
                        }
                    }
                }
            }
        } else {
            startActivity(Intent(this, NinjaActivationActivity::class.java))
            finish()
        }
    }

    private fun showBlockedDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Acceso bloqueado")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Cerrar") { _, _ -> finishAffinity() }
            .show()
    }

    private fun buildSplashUI() {
        val cfg = NinjaMagic.config()
        val bg = try { Color.parseColor(cfg.bgColor) } catch (e: Exception) { Color.parseColor("#0A0A0F") }
        val accent = try { Color.parseColor(cfg.accentColor) } catch (e: Exception) { Color.parseColor("#D4AF37") }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(bg)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(TextView(this).apply {
            text = cfg.appName
            textSize = 22f
            setTextColor(accent)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        })

        root.addView(ProgressBar(this).apply {
            indeterminateTintList = android.content.res.ColorStateList.valueOf(accent)
        })

        setContentView(root)
    }
}
