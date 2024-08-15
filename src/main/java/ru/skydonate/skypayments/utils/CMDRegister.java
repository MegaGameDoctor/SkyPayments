package ru.skydonate.skypayments.utils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class CMDRegister extends Command implements PluginIdentifiableCommand {
    protected final CommandExecutor owner;
    protected final Object registeredWith;
    protected Plugin plugin;

    public CMDRegister(String[] aliases, String desc, String usage, CommandExecutor owner, Object registeredWith,
                       Plugin plugin) {
        super(aliases[0], desc, usage, Arrays.asList(aliases));
        this.owner = owner;
        this.plugin = plugin;
        this.registeredWith = registeredWith;
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        return this.owner.onCommand(sender, this, label, args);
    }
}