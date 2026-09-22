package com.maxxcodebug.info

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Collects everything the app can find on its own:
 *  - the maintainer's live info.json (ETA, links, changelog edits)
 *  - the ROM's README (about text, Telegram/Discord links) and repo details
 *  - the latest release or recent commits of the changelog repo (changelog, update check)
 * Results are cached, so the app works offline and stays within GitHub's rate limit.
 */
class InfoRepository(private val ctx: Context, private val infoUrl: String) {

    private val prefs = ctx.getSharedPreferences("xhub_cache", Context.MODE_PRIVATE)

    fun bundled(): JSONObject =
        try {
            JSONObject(ctx.assets.open("info.json").bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            JSONObject()
        }

    fun cached(): JSONObject? =
        prefs.getString("auto", null)?.let {
            try { JSONObject(it) } catch (e: Exception) { null }
        }

    fun cachedAt(): Long = prefs.getLong("at", 0L)

    /** Blocking network work. Call from a background thread. */
    fun fetch(base: JSONObject): JSONObject {
        val auto = JSONObject()

        get(infoUrl)?.let {
            try { auto.put("remote", JSONObject(it)) } catch (e: Exception) { }
        }

        val rom = base.optJSONObject("rom")
        parseRepo(rom.s("github"))?.let { (owner, name) ->
            get("https://api.github.com/repos/$owner/$name/readme", raw = true)?.let { md ->
                auto.put("readme_about", ReadmeParser.about(md))
                val arr = JSONArray()
                for ((label, u) in ReadmeParser.links(md)) {
                    arr.put(JSONObject().put("label", label).put("url", u))
                }
                auto.put("readme_links", arr)
            }
            get("https://api.github.com/repos/$owner/$name")?.let {
                try {
                    val j = JSONObject(it)
                    auto.put("description", j.s("description"))
                    auto.put("homepage", j.s("homepage"))
                } catch (e: Exception) { }
            }
        }

        parseRepo(base.s("changelog_repo"))?.let { (owner, name) -> fetchChangelog(auto, owner, name) }

        if (auto.length() > 0) {
            prefs.edit().putString("auto", auto.toString()).putLong("at", System.currentTimeMillis()).apply()
        }
        return auto
    }

    private fun fetchChangelog(auto: JSONObject, owner: String, name: String) {
        val release = get("https://api.github.com/repos/$owner/$name/releases/latest")
        if (release != null) {
            try {
                val j = JSONObject(release)
                val tag = j.s("name").ifEmpty { j.s("tag_name") }
                val date = j.s("published_at").take(10)
                auto.put("changelog", JSONArray().put(entry(tag, date, bullets(j.s("body")))))
                auto.put("update", JSONObject().put("tag", tag).put("date", date).put("url", j.s("html_url")))
                return
            } catch (e: Exception) { }
        }
        val commits = get("https://api.github.com/repos/$owner/$name/commits?per_page=8") ?: return
        try {
            val arr = JSONArray(commits)
            val lines = ArrayList<String>()
            for (i in 0 until arr.length()) {
                val msg = arr.getJSONObject(i).getJSONObject("commit").s("message")
                lines.add(msg.lineSequence().first().trim())
            }
            auto.put("changelog", JSONArray().put(entry("Latest changes", "", lines)))
        } catch (e: Exception) { }
    }

    private fun entry(version: String, date: String, changes: List<String>): JSONObject {
        val list = JSONArray()
        changes.forEach { list.put(it) }
        return JSONObject().put("version", version).put("date", date).put("changes", list)
    }

    private fun bullets(body: String): List<String> =
        body.lines()
            .map { it.trim().trimStart('-', '*', '#', ' ', '\u2022') }
            .filter { it.isNotEmpty() }
            .take(10)

    /** Accepts https://github.com/owner/repo, github.com/owner/repo or owner/repo. */
    private fun parseRepo(s: String): Pair<String, String>? {
        val t = s.trim()
        if (t.isEmpty()) return null
        val m = Regex("github\\.com/([\\w.-]+)/([\\w.-]+)").find(t)
            ?: Regex("^([\\w.-]+)/([\\w.-]+)$").find(t)
            ?: return null
        return m.groupValues[1] to m.groupValues[2].removeSuffix(".git")
    }

    private fun get(url: String, raw: Boolean = false): String? =
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 7000
            c.readTimeout = 7000
            c.setRequestProperty("User-Agent", "XHub")
            if (url.contains("api.github.com")) {
                c.setRequestProperty(
                    "Accept",
                    if (raw) "application/vnd.github.raw+json" else "application/vnd.github+json"
                )
            }
            val text = if (c.responseCode in 200..299) c.inputStream.bufferedReader().use { it.readText() } else null
            c.disconnect()
            text
        } catch (e: Exception) {
            null
        }
}
