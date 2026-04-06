package net.sovereign.core.command

import org.bukkit.command.CommandSender

interface DevCommandSupport {
    
    fun buildNode(
        dispatch: (CommandSender, Array<String>) -> Unit
    ): com.mojang.brigadier.builder.LiteralArgumentBuilder<io.papermc.paper.command.brigadier.CommandSourceStack>

    
    fun handle(sender: CommandSender, args: Array<out String>)
}
