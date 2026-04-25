package de.c4vxl.vaycoreapi.language

import de.c4vxl.vaycoreapi.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Lookup table for translations based on keys
 * @param translations A map from key to translation
 * @param path The path to the language file
 * @param T The plugin the language is for
 */
class Lang<T : JavaPlugin>(
    private val translations: Map<String, String>,
    private val path: Path,
    val name: String
) {
    companion object {
        /**
         * Path to directory where translation files will be stored in
         */
        val translationsDirectory: Path
            get() = Path.of(Main.config.getString("language.translations-dir") ?: "./")

        /**
         * Returns the file of the language db
         */
        val langsDB
            get() = File(Main.config.getString("language.db") ?: "languages.yml")

        /**
         * Returns the default language name
         */
        val defaultLang: String
            get() = Main.config.getString("language.default") ?: "english"

        /**
         * Holds already loaded languages
         */
        val cache = ConcurrentHashMap<String, MutableMap<Class<*>, Lang<*>?>>()

        /**
         * Caches player languages
         */
        val playerCache = ConcurrentHashMap<UUID, String>()

        /**
         * Returns a language translation file
         * @param plugin The plugin to get the translation file for
         * @param name The name of the language
         */
         fun <T : JavaPlugin> getLanguageFile(plugin: Class<T>, name: String): File {
            val dir = translationsDirectory.resolve(plugin.packageName).toFile()
            return dir.resolve("$name.yml")
        }

        /**
         * Loads a language from disk
         * @param name The name of the language
         * @param T The plugin the language is for
         */
        inline fun <reified T : JavaPlugin> loadLanguage(name: String): Lang<T>? {
            val file = getLanguageFile(T::class.java, name)
            if (!file.isFile)
                return null

            val config = YamlConfiguration.loadConfiguration(file)

            // Load lookup table
            val translations = buildMap<String, String> {
                config.getKeys(true).forEach {
                    put(it, config.getString(it) ?: it)
                }
            }

            // Create language instance
            return Lang(translations, file.toPath(), name)
        }

        /**
         * Get a language from its name
         * @param name The name of the language
         * @param T The plugin the language is for
         */
        inline fun <reified T : JavaPlugin> get(name: String): Lang<T>? {
            val pluginMap = cache.computeIfAbsent(name) {
                ConcurrentHashMap<Class<*>, Lang<*>?>()
            }

            // Get from cache if present
            @Suppress("UNCHECKED_CAST")
            pluginMap[T::class.java]?.let { return it as Lang<T> }

            // Or try to load language
            val loaded = loadLanguage<T>(name) ?: return null

            // Cache loaded language
            pluginMap[T::class.java] = loaded
            return loaded
        }

        /**
         * Returns the language of a player
         */
        inline fun <reified T : JavaPlugin> CommandSender.getLang(): Lang<T> {
            // If not a player -> use default language
            val player = this as? Player ?: return get<T>(defaultLang)!!

            // Try to get player language from cache
            // If that fails -> load from disk
            // If that fails -> fallback to default language
            val preference = playerCache.computeIfAbsent(player.uniqueId) {
                YamlConfiguration.loadConfiguration(langsDB).getString(player.uniqueId.toString()) ?: defaultLang
            }

            // Get language
            return get<T>(preference) ?: error("Failed to load language: $defaultLang")
        }

        /**
         * Sets the language of a sender
         * @param lang The language to set
         */
        fun CommandSender.setLang(lang: String) {
            // Return if not a player
            val player = this as? Player ?: return

            // Update cache
            playerCache[player.uniqueId] = lang

            // Update db
            val config = YamlConfiguration.loadConfiguration(langsDB)
            config.set(player.uniqueId.toString(), lang)
            config.save(langsDB)
        }

        /**
         * Registers a specific language for a plugin
         * @param T The plugin to register the language for
         * @param language The language name
         * @param languageFileContent A yml-formatted string of the translations
         * @param overwrite If set to 'true' old extension will be overwritten
         */
        inline fun <reified T : JavaPlugin> provideLanguageTranslations(language: String, languageFileContent: String, overwrite: Boolean = false) {
            val file = getLanguageFile(T::class.java, language).also {
                it.parentFile.mkdirs()
            }

            // File exists
            if (!overwrite && file.exists())
                return

            // Save language
            file.writeText(languageFileContent)
        }
    }

    /**
     * Returns the translation of a key
     * @param key The translation key
     * @param args Arguments to the translation
     */
    fun get(key: String, vararg args: String): String {
        var value = resolveKey(key)

        // Handle arguments
        args.forEachIndexed { i, arg ->
            value = value.replace("$$i", arg)
        }

        return value
    }

    /**
     * Looks up the translation of a key
     * @param key The key to lookup
     * @param visited A list of previously visited keys to prevent circular key-references
     */
    private fun resolveKey(key: String, visited: MutableSet<String> = mutableSetOf()): String {
        // Key already visited
        // This prevents circular references leading to stack overflows
        if (!visited.add(key)) return key

        var value = translations.getOrDefault(key, key)

        // Resolve references
        value = Regex("""\$\{([^}]+)}""").replace(value) {
            resolveKey(
                it.groupValues[1],
                visited
            )
        }

        visited.remove(key)
        return value
    }

    /**
     * Returns a styled-component with the translation of a key
     * @param key The translation key
     * @param args Arguments to the translation
     */
    fun getCmp(key: String, vararg args: String): Component =
        MiniMessage.miniMessage().deserialize(get(key, *args))

    /**
     * Returns this language for another plugin
     * @param T The other plugin
     */
    inline fun <reified T : JavaPlugin> child(): Lang<T> =
        Companion.get<T>(this.name) ?: kotlin.run {
            Main.logger.warning("Failed to load child language of '$name' for ${T::class.java.name}")
            throw IllegalArgumentException()
        }
}