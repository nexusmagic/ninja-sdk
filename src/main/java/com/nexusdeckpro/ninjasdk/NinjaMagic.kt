package com.nexusdeckpro.ninjasdk

import android.app.Activity
import android.content.Context

object NinjaMagic {

    private var cfg: Config? = null

    data class Config(
        val signingSecret: String,
        val serverUrl: String,
        val mainActivity: Class<out Activity>,
        val appName: String,
        val accentColor: String = "#D4AF37",
        val bgColor: String = "#0A0A0F"
    )

    @JvmStatic
    fun init(
        context: Context,
        signingSecret: String,
        serverUrl: String,
        mainActivity: Class<out Activity>,
        appName: String = context.applicationInfo.loadLabel(context.packageManager).toString(),
        accentColor: String = "#D4AF37",
        bgColor: String = "#0A0A0F"
    ) {
        cfg = Config(
            signingSecret = signingSecret,
            serverUrl = serverUrl.trimEnd('/'),
            mainActivity = mainActivity,
            appName = appName,
            accentColor = accentColor,
            bgColor = bgColor
        )
    }

    internal fun config(): Config = cfg ?: error(
        "NinjaMagic no inicializado. Llama a NinjaMagic.init() en Application.onCreate() antes de usar el SDK."
    )
}
