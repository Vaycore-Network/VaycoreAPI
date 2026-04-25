package de.c4vxl.vaycoreapi

import de.c4vxl.vaycoreapi.loader.PluginLoader
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIPaperConfig
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

class Main : JavaPlugin() {
    companion object {
        lateinit var instance: Main
        lateinit var logger: Logger
        lateinit var config: FileConfiguration
    }

    override fun onLoad() {
        instance = this
        Main.logger = this.logger

        saveResource("config.yml", false)
        Main.config = this.config

        // Load CommandAPI
        CommandAPI.onLoad(
            CommandAPIPaperConfig(this)
                .silentLogs(true)
                .verboseOutput(false)
        )
    }

    override fun onEnable() {
        // Enable CommandAPI
        CommandAPI.onEnable()

        // Download plugins
        server.scheduler.runTask(this, Runnable {
            PluginLoader.deleteAllPlugins()
            PluginLoader.downloadAllPlugins()
        })

        logger.info("[+] $name has been enabled!")
    }

    override fun onDisable() {
        // Disable CommandAPI
        CommandAPI.onDisable()

        // Delete all downloaded plugins
        PluginLoader.deleteAllPlugins()

        logger.info("[+] $name has been disabled!")
    }
}