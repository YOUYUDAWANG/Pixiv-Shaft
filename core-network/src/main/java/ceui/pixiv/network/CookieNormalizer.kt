package ceui.pixiv.network

object CookieNormalizer {
    private val LOGGED_IN_SESSION_VALUE = Regex("^[0-9]+_[A-Za-z0-9]+$")

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val byName = LinkedHashMap<String, String>()
        for (part in raw.split(';')) {
            val pair = part.trim()
            val eq = pair.indexOf('=')
            if (eq <= 0) continue
            val name = pair.substring(0, eq)
            val value = pair.substring(eq + 1)
            if (
                name == "PHPSESSID" &&
                    byName[name]?.let { LOGGED_IN_SESSION_VALUE.matches(it) } == true &&
                    !LOGGED_IN_SESSION_VALUE.matches(value)
            ) {
                continue
            }
            byName[name] = value
        }
        return byName.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }
}
