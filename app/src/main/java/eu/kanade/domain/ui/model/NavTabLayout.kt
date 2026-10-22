package eu.kanade.domain.ui.model

/**
 * Sections a tab can be assigned to in the navigation layout.
 */
enum class NavSection {
    NAVBAR, MORE, DISABLED
}

/**
 * Represents the layout configuration of navigation tabs.
 * Each entry maps a tab key to its assigned section.
 * Order within the list determines order within each section.
 */
data class NavTabLayout(
    val entries: List<NavTabEntry>,
) {
    fun getKeysForSection(section: NavSection): List<String> =
        entries.filter { it.section == section }.map { it.key }

    fun getSectionForKey(key: String): NavSection =
        entries.find { it.key == key }?.section ?: NavSection.NAVBAR

    /**
     * Serialize to preference string format: "Library:navbar,Updates:more,..."
     */
    fun serialize(): String =
        entries.joinToString(",") { "${it.key}:${it.section.name.lowercase()}" }

    companion object {
        /** All customizable tab keys in default order. */
        // EaZy Nuvio+ / Chimanew -->
        const val KEY_HOME = "Home"
        // EaZy Nuvio+ / Chimanew <--
        const val KEY_LIBRARY = "Library"
        const val KEY_UPDATES = "Updates"
        const val KEY_HISTORY = "History"
        const val KEY_BROWSE = "Browse"
        const val KEY_DICTIONARY = "Dictionary"
        const val KEY_NOVELS = "Novels"
        const val KEY_ANIME = "Anime"

        // EaZy Nuvio+ / Chimanew
        val ALL_KEYS = listOf(
            KEY_HOME, KEY_LIBRARY, KEY_UPDATES, KEY_HISTORY, KEY_NOVELS, KEY_ANIME,
            KEY_BROWSE, KEY_DICTIONARY,
        )

        /**
         * Default layout: Dictionary in more, Updates in navbar.
         */
        val DEFAULT = NavTabLayout(
            ALL_KEYS.map { key ->
                val section = if (key == KEY_DICTIONARY) NavSection.MORE else NavSection.NAVBAR
                NavTabEntry(key, section)
            },
        )

        /**
         * Parse from preference string. Missing keys get default sections.
         * Empty string returns [DEFAULT].
         */
        fun parse(prefString: String): NavTabLayout {
            if (prefString.isBlank()) return DEFAULT

            val parsed = mutableListOf<NavTabEntry>()
            val seenKeys = mutableSetOf<String>()

            for (token in prefString.split(",")) {
                val parts = token.trim().split(":", limit = 2)
                if (parts.size != 2) continue
                val key = parts[0]
                val section = when (parts[1].lowercase()) {
                    "navbar" -> NavSection.NAVBAR
                    "more" -> NavSection.MORE
                    "disabled" -> NavSection.DISABLED
                    else -> NavSection.NAVBAR
                }
                if (key in ALL_KEYS && key !in seenKeys) {
                    parsed.add(NavTabEntry(key, section))
                    seenKeys.add(key)
                }
            }

            // EaZy Nuvio+ / Chimanew: Automatically swap Dictionary to More and Updates to Navbar if inherited from old Chimahon default
            val dictEntryIndex = parsed.indexOfFirst { it.key == KEY_DICTIONARY }
            val updatesEntryIndex = parsed.indexOfFirst { it.key == KEY_UPDATES }
            if (dictEntryIndex != -1 && updatesEntryIndex != -1) {
                if (parsed[dictEntryIndex].section == NavSection.NAVBAR && parsed[updatesEntryIndex].section == NavSection.MORE) {
                    parsed[dictEntryIndex] = NavTabEntry(KEY_DICTIONARY, NavSection.MORE)
                    parsed[updatesEntryIndex] = NavTabEntry(KEY_UPDATES, NavSection.NAVBAR)
                }
            }

            // Append any missing keys as navbar (or more for Dictionary)
            for (key in ALL_KEYS) {
                if (key !in seenKeys) {
                    val defaultSection = if (key == KEY_DICTIONARY) NavSection.MORE else NavSection.NAVBAR
                    if (key == KEY_HOME) {
                        parsed.add(0, NavTabEntry(key, defaultSection))
                    } else {
                        parsed.add(NavTabEntry(key, defaultSection))
                    }
                }
            }

            return NavTabLayout(parsed)
        }

        /**
         * Migrate from legacy showNavUpdates/showNavHistory boolean prefs.
         */
        fun migrateFromLegacy(showNavUpdates: Boolean, showNavHistory: Boolean): NavTabLayout {
            val entries = ALL_KEYS.map { key ->
                val section = when (key) {
                    KEY_UPDATES -> if (showNavUpdates) NavSection.NAVBAR else NavSection.MORE
                    KEY_HISTORY -> if (showNavHistory) NavSection.NAVBAR else NavSection.MORE
                    else -> NavSection.NAVBAR
                }
                NavTabEntry(key, section)
            }
            return NavTabLayout(entries)
        }
    }
}

data class NavTabEntry(
    val key: String,
    val section: NavSection,
)
