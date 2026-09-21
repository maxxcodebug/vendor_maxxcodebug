// Copyright (c) 2026 Anshuman X (maxxcodebug). All rights reserved.
package com.maxxcodebug.info

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val ui = Handler(Looper.getMainLooper())
    private var buildDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val type = SystemProperties.get(PROP_TYPE, "UNOFFICIAL").trim().uppercase()
        val forWho = SystemProperties.get(PROP_FOR, "").trim()
        buildDate = SystemProperties.get(PROP_DATE, "")

        applyStatus(type, forWho)

        // Show bundled data first, then refresh from GitHub if online.
        render(type, loadAsset())
        Thread {
            val remote = fetchRemote()
            if (remote != null) ui.post { render(type, remote) }
        }.start()
    }

    private fun applyStatus(type: String, forWho: String) {
        val badge = findViewById<TextView>(R.id.badge)
        val msg = findViewById<TextView>(R.id.statusMessage)

        val (label, color, message) = when (type) {
            "OFFICIAL" -> Triple(
                getString(R.string.status_official),
                getColor(R.color.badge_official),
                getString(R.string.msg_official)
            )
            "PERSONAL" -> Triple(
                getString(R.string.status_personal),
                getColor(R.color.badge_personal),
                if (forWho.isNotEmpty()) getString(R.string.msg_personal, forWho)
                else getString(R.string.msg_personal_noname)
            )
            else -> Triple(
                getString(R.string.status_unofficial),
                getColor(R.color.badge_unofficial),
                getString(R.string.msg_unofficial)
            )
        }

        badge.text = label
        badge.background = GradientDrawable().apply {
            cornerRadius = 1000f
            setColor(color)
        }
        msg.text = message
    }

    private fun showBuildInfo(info: JSONObject) {
        val rom = info.optString("rom_name")
        val lines = mutableListOf(
            "ROM: " + if (rom.isNotEmpty()) "$rom (${Build.DISPLAY})" else Build.DISPLAY,
            "Device: ${Build.MANUFACTURER} ${Build.MODEL}",
            "Android: ${Build.VERSION.RELEASE}"
        )
        if (buildDate.isNotEmpty()) lines.add("Build date: $buildDate")
        findViewById<TextView>(R.id.buildText).text = lines.joinToString("\n")
    }

    private fun render(type: String, info: JSONObject) {
        findViewById<CollapsingToolbarLayout>(R.id.collapsing).title =
            info.optString("maintainer").ifEmpty { getString(R.string.header_title) }
        showBuildInfo(info)

        findViewById<TextView>(R.id.aboutText).text =
            info.optString("about").ifEmpty { getString(R.string.soon_about) }

        val etaView = findViewById<TextView>(R.id.etaText)
        if (type == "PERSONAL") {
            etaView.visibility = View.GONE
        } else {
            etaView.visibility = View.VISIBLE
            etaView.text = getString(
                R.string.next_update,
                info.optString("next_update_eta").ifEmpty { getString(R.string.soon) }
            )
        }

        val sb = StringBuilder()
        val log = info.optJSONArray("changelog")
        if (log != null) {
            for (i in 0 until log.length()) {
                val e = log.getJSONObject(i)
                if (i > 0) sb.append("\n\n")
                sb.append(e.optString("version"))
                val d = e.optString("date")
                if (d.isNotEmpty()) sb.append("  (").append(d).append(")")
                val changes = e.optJSONArray("changes")
                if (changes != null) {
                    for (j in 0 until changes.length()) {
                        sb.append("\n\u2022 ").append(changes.getString(j))
                    }
                }
            }
        }
        findViewById<TextView>(R.id.changelogText).text =
            if (sb.isEmpty()) getString(R.string.soon_changelog) else sb.toString()

        val container = findViewById<LinearLayout>(R.id.linksContainer)
        container.removeAllViews()
        val links = info.optJSONArray("links")
        if (links != null) {
            for (i in 0 until links.length()) {
                val l = links.getJSONObject(i)
                val url = l.optString("url")
                if (url.isEmpty() || url.contains("CHANGE_ME")) continue // hide unset links
                val style = if (l.optBoolean("primary", false))
                    com.google.android.material.R.attr.materialButtonStyle
                else
                    com.google.android.material.R.attr.materialButtonTonalStyle
                val btn = MaterialButton(this, null, style)
                btn.text = l.optString("label")
                btn.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (8 * resources.displayMetrics.density).toInt() }
                btn.setOnClickListener { openLink(url) }
                container.addView(btn)
            }
        }
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_link, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAsset(): JSONObject =
        try {
            JSONObject(assets.open("info.json").bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            JSONObject()
        }

    private fun fetchRemote(): JSONObject? =
        try {
            val c = URL(INFO_URL).openConnection() as HttpURLConnection
            c.connectTimeout = 6000
            c.readTimeout = 6000
            val text = c.inputStream.bufferedReader().use { it.readText() }
            c.disconnect()
            JSONObject(text)
        } catch (e: Exception) {
            null
        }

    companion object {
        private const val PROP_TYPE = "ro.anshumanx.build.type"
        private const val PROP_FOR = "ro.anshumanx.build.for"
        private const val PROP_DATE = "ro.anshumanx.build.date"

        // Create this repo/file on your GitHub, then edit info.json anytime
        // to update changelog, ETA and links without rebuilding the ROM.
        private const val INFO_URL =
            "https://raw.githubusercontent.com/maxxcodebug/vendor_maxxcodebug/main/apps/XHub/assets/info.json"
    }
}
