package com.maxxcodebug.info

/** Pulls a short description and community links out of a ROM's README (markdown). */
object ReadmeParser {

    private val image = Regex("!\\[[^\\]]*\\]\\([^)]*\\)")
    private val mdLink = Regex("\\[([^\\]]*)\\]\\([^)]*\\)")
    private val html = Regex("<[^>]+>")
    private val marks = Regex("[*`>]")
    private val spaces = Regex("\\s+")
    private val url = Regex("https?://[^\\s)>\\]\"'<]+")
    private val host = Regex("https?://([^/]+)(/[^?#]*)?")

    /** First real paragraph of the README, cleaned and shortened. */
    fun about(readme: String): String {
        val paras = ArrayList<String>()
        val cur = StringBuilder()
        var inCode = false
        fun flush() {
            if (cur.isNotBlank()) paras.add(cur.toString())
            cur.setLength(0)
        }
        for (raw in readme.lines()) {
            val line = raw.trim()
            if (line.startsWith("```")) { inCode = !inCode; flush(); continue }
            if (inCode) continue
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("|") || line.startsWith("---")) {
                flush(); continue
            }
            cur.append(line).append(' ')
        }
        flush()
        for (p in paras) {
            val t = clean(p)
            if (t.length >= 40) {
                return if (t.length > 320) t.substring(0, 317).trimEnd() + "..." else t
            }
        }
        return ""
    }

    /** Telegram and Discord links found in the README as (label, url) pairs. */
    fun links(readme: String): List<Pair<String, String>> {
        val out = LinkedHashMap<String, String>()
        for (m in url.findAll(readme)) {
            val u = m.value.trimEnd('.', ',', ')', ';')
            val g = host.find(u)?.groupValues ?: continue
            val h = g[1].lowercase()
            val path = g.getOrElse(2) { "" }.trim('/')
            val label = when {
                h == "t.me" || h == "telegram.me" || h == "telegram.dog" ->
                    if (path.isNotEmpty()) "Telegram @" + path.substringBefore('/') else "Telegram"
                h.endsWith("discord.gg") || h.endsWith("discord.com") -> "Discord"
                else -> continue
            }
            out.putIfAbsent(u, label)
            if (out.size >= 5) break
        }
        return out.map { it.value to it.key }
    }

    private fun clean(s: String): String =
        s.replace(image, "")
            .replace(mdLink, "\$1")
            .replace(html, "")
            .replace(marks, "")
            .replace(spaces, " ")
            .trim()
}
