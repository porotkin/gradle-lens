package gradlelens.dependencies

data class UsageLink(
    val module: String,
    val dependent: String,
) {
    fun encode(): String = "$PROTOCOL$module$SEPARATOR$dependent"

    companion object {
        private const val PROTOCOL = "gradle-lens-usage:"

        private const val SEPARATOR = '|'

        fun parse(link: String): UsageLink? {
            if (!link.startsWith(PROTOCOL)) {
                return null
            }

            val parts = link.removePrefix(PROTOCOL).split(SEPARATOR, limit = 2)
            return if (parts.size == 2) UsageLink(parts[0], parts[1]) else null
        }
    }
}
