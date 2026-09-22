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
import android.text.format.DateUtils
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val ui = Handler(Looper.getMainLooper())
    private lateinit var repo: InfoRepository
    private lateinit var base: JSONObject
    private lateinit var scroll: NestedScrollView
    private lateinit var nav: LiquidGlassView
    private lateinit var navButtons: List<View>

    private var type = "UNOFFICIAL"
    private var forWho = ""
    private var buildDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        type = SystemProperties.get(PROP_TYPE, "UNOFFICIAL").trim().uppercase()
        forWho = SystemProperties.get(PROP_FOR, "").trim()
        buildDate = SystemProperties.get(PROP_DATE, "").trim()

        repo = InfoRepository(this, INFO_URL)
        base = repo.bundled()

        scroll = findViewById(R.id.scroll)
        nav = findViewById(R.id.nav)
        nav.source = scroll

        setupStatus()
        setupNav()
        setupButtons()

        render(merge(base, repo.cached()))
        playEntrance()
        refresh(force = false)
    }

    // ------------------------------------------------------------------ status

    private fun setupStatus() {
        val orb = findViewById<OrbView>(R.id.orb)
        orb.setStatusColor(
            Color.parseColor(
                when (type) {
                    "OFFICIAL" -> "#66BB6A"
                    "PERSONAL" -> "#B39DDB"
                    else -> "#FFA726"
                }
            )
        )
        orb.setOnClickListener {
            it.scaleX = 0.86f
            it.scaleY = 0.86f
            it.spring(DynamicAnimation.SCALE_X, 1f, SpringForce.STIFFNESS_LOW, SpringForce.DAMPING_RATIO_HIGH_BOUNCY)
            it.spring(DynamicAnimation.SCALE_Y, 1f, SpringForce.STIFFNESS_LOW, SpringForce.DAMPING_RATIO_HIGH_BOUNCY)
        }
        text(R.id.heroTitle).setText(
            when (type) {
                "OFFICIAL" -> R.string.hero_official
                "PERSONAL" -> R.string.hero_personal
                else -> R.string.hero_unofficial
            }
        )
        text(R.id.heroSub).setText(
            when (type) {
                "OFFICIAL" -> R.string.hero_sub_official
                "PERSONAL" -> R.string.hero_sub_personal
                else -> R.string.hero_sub_unofficial
            }
        )
    }

    // ---------------------------------------------------------------------- nav

    private fun setupNav() {
        navButtons = listOf(R.id.navHome, R.id.navChangelog, R.id.navLinks).map { findViewById<View>(it) }
        navButtons.forEach { it.pressSpring() }
        navButtons[0].setOnClickListener { goTo(0, null) }
        navButtons[1].setOnClickListener { goTo(1, findViewById(R.id.changelogCard)) }
        navButtons[2].setOnClickListener { goTo(2, findViewById(R.id.linksCard)) }
        selectNav(0)

        scroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, y, _, _ ->
            nav.invalidate()
            val links = findViewById<View>(R.id.linksCard).top - scroll.height / 2
            val log = findViewById<View>(R.id.changelogCard).top - scroll.height / 2
            selectNav(if (y >= links) 2 else if (y >= log) 1 else 0)
        })
    }

    private var selected = -1

    private fun selectNav(i: Int) {
        if (i == selected) return
        selected = i
        navButtons.forEachIndexed { j, b ->
            b.animate().alpha(if (j == i) 1f else 0.5f).setDuration(200).start()
            val s = if (j == i) 1.18f else 1f
            b.spring(DynamicAnimation.SCALE_X, s)
            b.spring(DynamicAnimation.SCALE_Y, s)
        }
    }

    private fun goTo(index: Int, target: View?) {
        selectNav(index)
        scroll.smoothScrollTo(0, if (target == null) 0 else target.top - dp(16f).toInt())
        nav.animateFor(700)
    }

    // ------------------------------------------------------------------ buttons

    private fun setupButtons() {
        val btnRefresh = findViewById<View>(R.id.btnRefresh)
        btnRefresh.pressSpring()
        btnRefresh.setOnClickListener {
            it.rotation = 0f
            it.animate().rotationBy(360f).setDuration(600).start()
            refresh(force = true)
        }
        val btnInfo = findViewById<View>(R.id.btnInfo)
        btnInfo.pressSpring()
        btnInfo.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.about_body) + "\n\n" + getString(R.string.copyright))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    // ---------------------------------------------------------------- animation

    private fun playEntrance() {
        val ids = listOf(
            R.id.header, R.id.heroCard, R.id.whoCard, R.id.romCard,
            R.id.buildCard, R.id.changelogCard, R.id.meCard, R.id.linksCard
        )
        ids.forEachIndexed { i, id -> findViewById<View>(id).springIn(i) }
        val orb = findViewById<View>(R.id.orb)
        orb.scaleX = 0.3f
        orb.scaleY = 0.3f
        orb.postDelayed({
            orb.spring(DynamicAnimation.SCALE_X, 1f, SpringForce.STIFFNESS_VERY_LOW, SpringForce.DAMPING_RATIO_LOW_BOUNCY)
            orb.spring(DynamicAnimation.SCALE_Y, 1f, SpringForce.STIFFNESS_VERY_LOW, SpringForce.DAMPING_RATIO_LOW_BOUNCY)
        }, 150)
        nav.animateFor(1800)
    }

    // --------------------------------------------------------------------- data

    private fun refresh(force: Boolean) {
        val fresh = System.currentTimeMillis() - repo.cachedAt() < TTL_MS
        if (!force && fresh && repo.cached() != null) {
            updateSyncText(false)
            return
        }
        updateSyncText(true)
        Thread {
            val auto = try { repo.fetch(base) } catch (e: Exception) { null }
            ui.post {
                if (isDestroyed) return@post
                if (auto != null && auto.length() > 0) render(merge(base, auto))
                updateSyncText(false)
                nav.animateFor(600)
            }
        }.start()
    }

    private fun updateSyncText(syncing: Boolean) {
        val at = repo.cachedAt()
        text(R.id.syncText).text = when {
            syncing -> getString(R.string.syncing)
            at > 0 -> getString(
                R.string.synced,
                DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            )
            else -> getString(R.string.offline)
        }
    }

    /** Bundled data first, then the maintainer's live info.json, then what was found on GitHub. */
    private fun merge(base: JSONObject, auto: JSONObject?): JSONObject {
        val m = JSONObject(base.toString())
        if (auto == null) return m
        auto.optJSONObject("remote")?.let { overlay(m, it) }

        val rom = m.optJSONObject("rom") ?: JSONObject().also { m.put("rom", it) }
        if (rom.s("about").isEmpty()) rom.put("about", auto.s("readme_about"))
        if (rom.s("about").isEmpty()) rom.put("about", auto.s("description"))
        if (rom.s("website").isEmpty()) rom.put("website", auto.s("homepage"))
        auto.optJSONArray("readme_links")?.let { rom.put("extra_links", it) }

        val manual = m.optJSONArray("changelog")
        if ((manual?.length() ?: 0) == 0) auto.optJSONArray("changelog")?.let { m.put("changelog", it) }
        auto.optJSONObject("update")?.let { m.put("update", it) }
        return m
    }

    private fun overlay(dst: JSONObject, src: JSONObject) {
        for (k in src.keys()) {
            val v = src.get(k)
            when {
                v is JSONObject && dst.optJSONObject(k) != null -> overlay(dst.getJSONObject(k), v)
                v is String && v.isEmpty() -> { }
                v is JSONArray && v.length() == 0 -> { }
                else -> dst.put(k, v)
            }
        }
    }

    // ------------------------------------------------------------------- render

    private fun render(info: JSONObject) {
        val rom = info.optJSONObject("rom") ?: JSONObject()
        val maintainer = info.s("maintainer").ifEmpty { getString(R.string.header_title) }
        val romName = rom.s("name").ifEmpty { info.s("rom_name") }

        text(R.id.title).text = maintainer
        text(R.id.subtitle).text =
            (if (romName.isNotEmpty()) "$romName \u00B7 " else "") + "${Build.MODEL} (${Build.DEVICE})"

        renderWho(maintainer)
        renderRom(rom, romName)
        renderTiles(info)
        renderChangelog(info)
        renderUpdate(info)

        text(R.id.aboutText).text = info.s("about").ifEmpty { getString(R.string.soon_about) }

        val links = findViewById<ChipGroup>(R.id.links)
        links.removeAllViews()
        info.optJSONArray("links")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                addChip(links, o.s("label"), o.s("url"))
            }
        }
    }

    private fun renderWho(maintainer: String) {
        val (label, value, sub) = when (type) {
            "OFFICIAL" -> Triple(getString(R.string.who_official), maintainer, getString(R.string.hero_official))
            "PERSONAL" -> Triple(
                getString(R.string.who_personal),
                forWho.ifEmpty { getString(R.string.who_private) },
                getString(R.string.hero_personal)
            )
            else -> Triple(getString(R.string.who_unofficial), getString(R.string.who_unofficial_v), getString(R.string.hero_unofficial))
        }
        text(R.id.whoLabel).text = label
        text(R.id.whoValue).text = value
        text(R.id.whoSub).text = sub
    }

    private fun renderRom(rom: JSONObject, romName: String) {
        text(R.id.romName).text = romName.ifEmpty { Build.DISPLAY }
        text(R.id.romAbout).text = rom.s("about").ifEmpty { getString(R.string.soon_about) }

        val group = findViewById<ChipGroup>(R.id.romLinks)
        group.removeAllViews()
        val seen = HashSet<String>()
        fun add(label: String, raw: String) {
            val url = fullUrl(raw)
            if (url.isNotEmpty() && seen.add(url.trimEnd('/').lowercase())) addChip(group, label, url)
        }
        add("Channel", rom.s("channel"))
        add("Discussion group", rom.s("group"))
        add("GitHub", rom.s("github"))
        add("Website", rom.s("website"))
        rom.optJSONArray("extra_links")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(o.s("label"), o.s("url"))
            }
        }
        group.visibility = if (group.childCount == 0) View.GONE else View.VISIBLE
    }

    private fun renderTiles(info: JSONObject) {
        val grid = findViewById<GridLayout>(R.id.tiles)
        grid.removeAllViews()
        val eta = info.s("next_update_eta").ifEmpty { getString(R.string.soon) }
        val tiles = listOf(
            Triple(getString(R.string.tile_device), Build.MODEL, false),
            Triple(getString(R.string.tile_android), Build.VERSION.RELEASE, false),
            Triple(getString(R.string.tile_date), buildDate.ifEmpty { "-" }, false),
            Triple(getString(R.string.tile_patch), Build.VERSION.SECURITY_PATCH, false),
            Triple(getString(R.string.tile_id), Build.ID, false),
            Triple(getString(R.string.tile_eta), eta, true)
        )
        for ((label, value, highlight) in tiles) grid.addView(tile(label, value, highlight))
    }

    private fun renderChangelog(info: JSONObject) {
        val sb = StringBuilder()
        val log = info.optJSONArray("changelog")
        if (log != null) {
            for (i in 0 until log.length()) {
                val e = log.getJSONObject(i)
                if (i > 0) sb.append("\n\n")
                sb.append(e.s("version"))
                val d = e.s("date")
                if (d.isNotEmpty()) sb.append("  (").append(d).append(")")
                val changes = e.optJSONArray("changes")
                if (changes != null) {
                    for (j in 0 until changes.length()) sb.append("\n\u2022 ").append(changes.getString(j))
                }
            }
        }
        text(R.id.changelogText).text = if (sb.isEmpty()) getString(R.string.soon_changelog) else sb.toString()
    }

    private fun renderUpdate(info: JSONObject) {
        val chip = text(R.id.updateChip)
        val upd = info.optJSONObject("update")
        val date = upd.s("date")
        if (upd != null && date.isNotEmpty() && buildDate.isNotEmpty() && date > buildDate) {
            chip.visibility = View.VISIBLE
            chip.text = getString(R.string.update_available, upd.s("tag"))
            chip.pressSpring()
            chip.setOnClickListener { open(upd.s("url")) }
        } else {
            chip.visibility = View.GONE
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun text(id: Int) = findViewById<TextView>(id)

    private fun fullUrl(raw: String): String {
        val s = raw.trim()
        return when {
            s.isEmpty() -> ""
            s.startsWith("http") -> s
            s.matches(Regex("[\\w.-]+/[\\w.-]+")) -> "https://github.com/$s"
            s.startsWith("t.me/") -> "https://$s"
            s.startsWith("@") -> "https://t.me/" + s.substring(1)
            else -> "https://$s"
        }
    }

    private fun themeColor(attr: Int): Int = MaterialColors.getColor(findViewById<View>(R.id.root), attr)

    private fun addChip(group: ChipGroup, label: String, rawUrl: String) {
        val url = fullUrl(rawUrl)
        if (label.isEmpty() || url.isEmpty() || url.contains("CHANGE_ME")) return
        val c = Chip(this)
        c.text = label
        c.isCheckable = false
        c.setOnClickListener { open(url) }
        c.pressSpring()
        group.addView(c)
    }

    private fun tile(label: String, value: String, highlight: Boolean): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val pad = dp(14f).toInt()
        box.setPadding(pad, pad, pad, pad)
        val bg = GradientDrawable()
        bg.cornerRadius = dp(22f)
        if (highlight) {
            bg.setColor(themeColor(com.google.android.material.R.attr.colorSecondaryContainer))
        } else {
            bg.setStroke(dp(1.5f).toInt(), themeColor(com.google.android.material.R.attr.colorOutline))
        }
        box.background = bg

        val l = TextView(this)
        l.text = label
        l.textSize = 11f
        l.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        val v = TextView(this)
        v.text = value
        v.textSize = 15f
        v.setTypeface(v.typeface, android.graphics.Typeface.BOLD)
        v.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
        box.addView(l)
        box.addView(v)

        val m = dp(4f).toInt()
        val lp = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f))
        lp.width = 0
        lp.setMargins(m, m, m, m)
        box.layoutParams = lp
        box.pressSpring()
        return box
    }

    private fun open(url: String) {
        if (url.isEmpty()) {
            Toast.makeText(this, R.string.no_link, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_link, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val PROP_TYPE = "ro.anshumanx.build.type"
        private const val PROP_FOR = "ro.anshumanx.build.for"
        private const val PROP_DATE = "ro.anshumanx.build.date"
        private const val TTL_MS = 6L * 60 * 60 * 1000

        // Live info.json in the maintainer's repo. Edit it on GitHub to change
        // ETA, links or about text without rebuilding the ROM.
        private const val INFO_URL =
            "https://raw.githubusercontent.com/maxxcodebug/vendor_maxxcodebug/main/apps/XHub/assets/info.json"
    }
}
