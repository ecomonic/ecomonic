package net.yellowstrawberry.ecomonic.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;

@SuppressWarnings("UnstableApiUsage")
public interface CommandRoot {
    LiteralCommandNode<CommandSourceStack> build();
}
