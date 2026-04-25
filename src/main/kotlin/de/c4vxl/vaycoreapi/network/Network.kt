package de.c4vxl.vaycoreapi.network

import eu.cloudnetservice.driver.inject.InjectionLayer
import eu.cloudnetservice.driver.provider.CloudServiceProvider
import eu.cloudnetservice.driver.registry.ServiceRegistry
import eu.cloudnetservice.modules.bridge.player.PlayerManager
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType
import org.bukkit.entity.Player

/**
 * Utilities for the CloudNet network
 */
object Network {
    /**
     * Holds a service provider
     */
    val serviceProvider: CloudServiceProvider =
        InjectionLayer.ext().instance(CloudServiceProvider::class.java)

    /**
     * Holds a player manager
     */
    val playerManager: PlayerManager =
        ServiceRegistry.registry().defaultInstance(PlayerManager::class.java)

    /**
     * Returns a CloudNet player executor of this player
     */
    val Player.cloudPlayerExecutor
        get() = playerManager.playerExecutor(this.uniqueId)

    /**
     * Returns a CloudNet player of this player
     */
    val Player.cloudPlayer
        get() = playerManager.onlinePlayer(this.uniqueId)

    /**
     * Connects a player to a specific service
     * @param serviceName The name of the service
     * @param selector The selector to choose the server by
     */
    fun Player.connectToService(serviceName: String, selector: ServerSelectorType = ServerSelectorType.HIGHEST_PLAYERS) {
        this.cloudPlayerExecutor.connectToTask(serviceName, selector)
    }
}