package network.cow.frames.interactive.spigot

import com.comphenix.protocol.wrappers.WrappedDataWatcher
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType

/**
 * @author Benedikt Wüller
 */

private val defaultWatchers = mutableMapOf<EntityType, WrappedDataWatcher>()

fun getDataWatcher(type: EntityType): WrappedDataWatcher {
    val watcher = defaultWatchers.getOrPut(type) {
        val world = Bukkit.getWorlds().first()

        // Set block for hanging entities.
        world.getBlockAt(0, 255, 1).type = Material.BARRIER

        val entity = world.spawnEntity(Location(world, 0.0, 255.0, 0.0), type)
        val watcher = WrappedDataWatcher.getEntityWatcher(entity).deepClone()
        entity.remove()

        // Reset temporary block.
        world.getBlockAt(0, 255, 1).type = Material.AIR

        return@getOrPut watcher
    }
    return watcher.deepClone()
}
