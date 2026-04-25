package de.c4vxl.vaycoreapi.loader

import de.c4vxl.vaycoreapi.Main
import de.c4vxl.vaycoreapi.utils.DownloadUtils
import de.c4vxl.vaycoreapi.utils.GithubUtils
import org.bukkit.Bukkit
import java.io.File
import java.net.URI
import java.net.URL

object PluginLoader {
    data class LoadablePlugin(
        val name: String,
        val url: String,
        val fileName: String = "$name.jar",
        val isFromGitHub: Boolean = url.startsWith("gh:")
    ) {
        /**
         * Returns an url to download the plugin file from
         */
        val downloadURL: URL?
            get() {
                // Handle GitHub specific urls
                if (isFromGitHub)
                    return GithubUtils.latestReleaseFile(url)

                // Handle normal url
                return URI(url).toURL()
            }
    }

    /**
     * Loads a list of plugins to load from the config
     */
    val plugins: List<LoadablePlugin>
        get() {
            val section = Main.config.getConfigurationSection("plugins") ?: return emptyList()

            return section.getKeys(false).mapNotNull { name ->
                val url = section.getString("$name.url") ?: run {
                    Main.logger.warning("Wrong format for plugin: '$name'. URL Missing!")
                    return@mapNotNull null
                }

                return@mapNotNull LoadablePlugin(name, url)
            }
        }

    /**
     * Returns the plugins directory
     */
    val pluginsDir: File
        get() = Main.instance.dataFolder.parentFile

    /**
     * Downloads a plugin
     * @param plugin The plugin to download
     */
    fun downloadPlugin(plugin: LoadablePlugin) {
        // Get file download
        val downloadURL = plugin.downloadURL ?: run {
            Main.logger.warning("Tried to download plugin '${plugin.name}'. But couldn't parse download url!")
            return
        }

        // Get plugin file
        val file = pluginsDir.resolve(plugin.fileName)

        // Download plugin
        DownloadUtils.downloadFile(downloadURL, file)

        // Load plugin
        if (!Bukkit.getPluginManager().isPluginEnabled(plugin.name))
            Bukkit.getPluginManager().loadPlugin(file)
    }

    /**
     * Downloads all plugins found in [PluginLoader.plugins]
     */
    fun downloadAllPlugins() =
        plugins.forEach { downloadPlugin(it) }

    /**
     * Deletes all plugins
     */
    fun deleteAllPlugins() {
        plugins.forEach {
            pluginsDir.resolve(it.fileName).delete()
        }
    }
}