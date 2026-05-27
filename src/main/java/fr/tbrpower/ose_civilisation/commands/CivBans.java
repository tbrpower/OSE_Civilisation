package fr.tbrpower.ose_civilisation.commands;

import fr.tbrpower.ose_civilisation.OSE_Civilisation;
import fr.tbrpower.ose_civilisation.commands.CivUtils.PendingAction;
import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ban.IpBanList;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CivBans {


    private final OSE_Civilisation plugin;
    private final CivUtils civUtils;

    public CivBans(OSE_Civilisation plugin, CivUtils civUtils) { this.plugin = plugin; this.civUtils = civUtils;}

    public  void unbanPlayersWithReason(CommandSender sender, String banReason, Boolean confirmed) {
        if (!confirmed) {
            ProfileBanList profileBanList = (ProfileBanList) Bukkit.getBanList(BanList.Type.PROFILE);
//            IpBanList ipBanList = (IpBanList) Bukkit.getBanList(BanList.Type.IP);

            ArrayList<String> revivedPlayers = new ArrayList<String>();

            for (BanEntry<? super PlayerProfile> entry : profileBanList.getEntries()) {
                PlayerProfile target = (PlayerProfile) entry.getBanTarget();
                if (!(entry.getSource().isEmpty()) && entry.getSource().equals(banReason)) {
                    if (plugin.getConfig().getBoolean("session-started")) {
                        List<String> teleported = plugin.getConfig().getStringList("teleported-players");
                        if (teleported.contains(target.getId().toString())) {
                            teleported.remove(target.getId().toString());
                            plugin.getConfig().set("teleported-players", teleported);
                            plugin.saveConfig();
                        }

                    }
                    profileBanList.pardon(target);
//                    ipBanList.pardon(target.getName());
                    revivedPlayers.add(target.getName());
                }
            }

//            for (BanEntry<? super InetAddress> entry : ipBanList.getEntries()) {
//                if (!(entry.getSource().isEmpty()) && entry.getSource().equals(banReason)) {
//                    ipBanList.pardon(entry.getTarget());
//                }
//            }

            if (revivedPlayers.isEmpty()) {
                sender.sendMessage("§cNo revivable dead players !§r");
            } else {
                sender.sendMessage((revivedPlayers.size() == 1)
                        ? "§aSuccessfully revived 1 player !§r"
                        : "§aSuccessfully revived" + revivedPlayers.size() + " players !§r");
            }
        } else {
            // senderUUID
            UUID senderUUID;
            if (sender instanceof Player player) {
                senderUUID = player.getUniqueId();
            }
            else {
                // If sender is CONSOLE
                senderUUID = new UUID(0,0);
            }
            sender.sendMessage(civUtils.confirmRequestMessage);
            civUtils.confirmationList.add(civUtils.new PendingConfirmation(PendingAction.REVIVE_PLAYERS, banReason, senderUUID));
        }
    }

    public void unbanPlayersWithReason(CommandSender sender, String banReason) {
        unbanPlayersWithReason(sender, banReason, false);
    }

    public void dumpBannedPlayersWithReason(CommandSender sender, String banReason) {
        if (sender != null) {
            String adverb = (banReason.equals(OSE_Civilisation.BanReasons.TEMP_DEATH.getReason()) ? "temporarily" : "permanently");
            ProfileBanList profileBanList = (ProfileBanList) Bukkit.getBanList(BanList.Type.PROFILE);

            ArrayList<String> deadPlayers = new ArrayList<String>();

            for (BanEntry<? super PlayerProfile> entry : profileBanList.getEntries()) {
                PlayerProfile target = (PlayerProfile) entry.getBanTarget();
                if (entry.getSource().equals(banReason)) {
                    deadPlayers.add(target.getName());
                }
            }

            if (deadPlayers.isEmpty()) {
                sender.sendMessage("§6§lNo "+ adverb +" banned players :( !");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("<gold><bold>List of "+ adverb +" banned players "
                        + deadPlayers.size()
                        +"): </bold></gold>");

                for (String playerName : deadPlayers) {
                    sb.append("<#026440>\n - " + playerName + "</#026440>");
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(sb.toString()
                    ));
                }
            }


        }
    }

    public void toggleTempDeath(CommandSender sender) {
        plugin.getConfig().set("temp-death", ! (plugin.getConfig().getBoolean("temp-death")));
        plugin.saveConfig();
        sender.sendMessage("Temporary death is now set to : "+ plugin.getConfig().getBoolean("temp-death"));
    }

}
