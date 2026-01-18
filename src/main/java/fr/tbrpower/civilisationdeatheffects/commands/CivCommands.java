package fr.tbrpower.civilisationdeatheffects.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.apache.maven.model.Profile;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;


public class CivCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Invalid syntax. Correct use : /civ <newday|list>");
            return true;
        }


        switch (args[0].toLowerCase()) {
            case "newday" -> sender.sendMessage("Unknown subcommand.");
            case "list" -> dumpDeadList(sender);
            default -> sender.sendMessage("Unknown subcommand.");
        }


        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("newday", "list")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }



        if (args.length == 2 && args[0].equalsIgnoreCase("ban")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    public void dumpDeadList(CommandSender sender) {
        if (sender != null) {
            ProfileBanList profileBanList = (ProfileBanList) Bukkit.getBanList(BanList.Type.PROFILE);

            ArrayList<String> deadPlayers = new ArrayList<String>();

            for (BanEntry<? super PlayerProfile> entry : profileBanList.getEntries()) {
                PlayerProfile target = (PlayerProfile) entry.getBanTarget();
                if (! (entry.getSource().isEmpty())  && entry.getSource().equals("nonPermDeath")) {
                    deadPlayers.add(target.getName());
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<gold><bold>Liste des joueurs bannis de façon non permanente ("
                    + deadPlayers.size()
                    +"): </bold></gold>");

            for (String playerName : deadPlayers) {
                sb.append("#013220>\n - " + playerName + "</#013220>");
            }


            sender.sendMessage(MiniMessage.miniMessage().deserialize(sb.toString()
            ));
        }
    }
}
