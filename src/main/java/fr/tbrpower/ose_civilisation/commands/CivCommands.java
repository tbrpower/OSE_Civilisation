package fr.tbrpower.ose_civilisation.commands;

import fr.tbrpower.ose_civilisation.OSE_Civilisation;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.entity.Player;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class CivCommands implements CommandExecutor, TabCompleter {


    private final OSE_Civilisation plugin;
    public CivCommands(OSE_Civilisation plugin) { this.plugin = plugin;}

    private Set<String> sessionUUIDs = new HashSet<>();

    public CivBans civBans;

    public CivUtils civUtils;

    public CivAreas civAreas;

    public CivSessions civSessions;


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cInvalid syntax. Correct use : /civ <pardonall|pardonperm|listtemp|listperm|toggle|info|newarea|rmarea|setpos|startsession|cancelsession|reload|confirm|cancel>");
            return true;
        }


        switch (args[0].toLowerCase()) {
            // CivBans
            case "pardontemp" -> civBans.unbanPlayersWithReason(sender, OSE_Civilisation.BanReasons.TEMP_DEATH.getReason());
            case "pardonperm" -> civBans.unbanPlayersWithReason(sender, OSE_Civilisation.BanReasons.PERM_DEATH.getReason());
            case "listtemp" -> civBans.dumpBannedPlayersWithReason(sender, OSE_Civilisation.BanReasons.TEMP_DEATH.getReason());
            case "listperm" -> civBans.dumpBannedPlayersWithReason(sender, OSE_Civilisation.BanReasons.PERM_DEATH.getReason());
            case "toggle" -> civBans.toggleTempDeath(sender);
            case "info" -> civBans.civInfo(sender);

            // CivAreas
            case "newarea" -> civAreas.newArea(sender, args);
            case "rmarea" -> civAreas.removeArea(sender, args);
            case "setpos" -> civAreas.setpos(sender, args);

            // CivSessions
            case "startsession" -> civSessions.startSession(sender);
            case "cancelsession" -> civSessions.cancelSession(sender);

            // CivUtils
            case "confirm" -> civUtils.confirm(sender);
            case "cancel" -> civUtils.cancel(sender);
            case "reload" -> civUtils.reload(sender);

            // DEFAULT
            default -> sender.sendMessage("§cUnknown subcommand.");
        }


        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("pardontemp", "pardonperm", "listtemp", "listperm", "toggle", "info", "newarea", "rmarea", "setpos", "startsession", "cancelsession", "reload", "confirm", "cancel")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }



        if (args.length == 2 && (args[0].equalsIgnoreCase("rmarea") || args[0].equalsIgnoreCase("setpos"))) {
            return Objects.requireNonNull(plugin.getConfig().getConfigurationSection("areas")).getKeys(false).stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}


