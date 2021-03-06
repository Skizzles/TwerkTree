package me.jantuck.twerktree.compatibility

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

class ModernProvider {

    private val boneMealItemStack by lazy {
        @Suppress("DEPRECATION")
        when (ReflectionSupplier.getLegacy().new) {
            true -> ReflectionSupplier
                .CRAFT_ITEM_STACK_METHOD_ACCESS
                .invoke(
                    null,
                    ReflectionSupplier.CRAFT_ITEM_STACK_AS_NMS_COPY,
                    ItemStack(Material.BONE_MEAL)
                )
            else -> ReflectionSupplier
                .CRAFT_ITEM_STACK_METHOD_ACCESS
                .invoke(
                    null,
                    ReflectionSupplier.CRAFT_ITEM_STACK_AS_NMS_COPY,
                    ItemStack(Material.getMaterial("INK_SACK")!!, 1, 15)
                )
        }
    }

    private fun getBlockPosition(block: Block): Array<out Any> {
        return when (ReflectionSupplier.getLegacy()) {
            ReflectionSupplier.LegacyType.OLD_OLD -> arrayOf(block.x, block.y, block.z)
            ReflectionSupplier.LegacyType.OLD -> arrayOf(
                ReflectionSupplier.NMS_BLOCK_POSITION_CONSTRUCTOR.invokeWithArguments(
                    block.x,
                    block.y,
                    block.z
                )
            )
            else -> arrayOf(
                ReflectionSupplier.CRAFT_BLOCK_METHOD_ACCESS.invoke(
                    block,
                    ReflectionSupplier.GET_POSITION_METHOD_INDEX
                )
            )
        }
    }


    private val cooldownMap by lazy { mutableMapOf<UUID, Long>()}
    private fun flushMap(){
        cooldownMap.entries.forEach {
            if (System.currentTimeMillis() > it.value) {
                cooldownMap.remove(it.key) // No more cooldown
            }
        }
    }

    /**
     * Config will contain the values of use-permissions and allow-particles in that order
     *
     * Cooldown will contain values enabled, and cooldown in miliseconds
     */
    fun boneMeal(block: Block, player: Player, config: Triple<Boolean, Boolean, Int>, cooldown: Pair<Boolean, Int>) {
        if (config.first && !player.hasPermission("twerktree.twerk"))
            return
        if (cooldown.first){
            flushMap() // Flush old cooldowns
            if (config.first && !player.hasPermission("twerktree.cooldownexempt") || !config.first) { // Check for permissions
                if (cooldownMap.containsKey(player.uniqueId)) // Check if player has cooldown and return
                    return
                cooldownMap[player.uniqueId] = System.currentTimeMillis() + cooldown.second // Set player cooldown
            }
        }
        ReflectionSupplier
            .NMS_BONE_MEAL_METHOD_ACCESS
            .invoke(
                null,
                ReflectionSupplier.NMS_BONE_MEAL_APPLY_INDEX,
                boneMealItemStack,
                ReflectionSupplier.CRAFT_WORLD_METHOD_ACCESS.invoke(
                    block.world,
                    ReflectionSupplier.CRAFT_WORLD_HANDLE_METHOD_INDEX
                ),
                *getBlockPosition(block)
            )
        applyEffect(block, player, config)
    }

    private fun applyEffect(block: Block, player: Player, config: Triple<Boolean, Boolean, Int>) {
        if (!config.second || config.first && !player.hasPermission("twerktree.particles"))
            return
        ReflectionSupplier.TRIGGER_EFFECT_METHOD_ACCESS.invoke(
            ReflectionSupplier.CRAFT_WORLD_METHOD_ACCESS.invoke(
                block.world,
                ReflectionSupplier.CRAFT_WORLD_HANDLE_METHOD_INDEX
            ),
            ReflectionSupplier.TRIGGER_EFFECT_INDEX,
            config.third,
            *getBlockPosition(block),
            0
        )
    }

}