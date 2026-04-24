package com.nexusdeckpro.ninjasdk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class NinjaSplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (NinjaHandshake(this).isActivated()) {
            startActivity(Intent(this, NinjaMagic.config().mainActivity))
        } else {
            startActivity(Intent(this, NinjaActivationActivity::class.java))
        }
        finish()
    }
}
