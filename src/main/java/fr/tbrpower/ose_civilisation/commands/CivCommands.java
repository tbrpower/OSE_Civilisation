package fr.tbrpower.ose_civilisation.commands;

import fr.tbrpower.ose_civilisation.OSE_Civilisation;
import net.kyori.adventure.text.minimessage.MiniMessage;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.apache.commons.lang3.ObjectUtils;
import org.bukkit.*;
import org.bukkit.ban.IpBanList;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PersistenceDelegate;
import java.net.InetAddress;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.savePlayers;


public class CivCommands implements CommandExecutor, TabCompleter {


    private final OSE_Civilisation plugin;
    public CivCommands(OSE_Civilisation plugin) { this.plugin = plugin;}


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cInvalid syntax. Correct use : /civ <pardonall|list|toggle|info|newarea|rmarea|setpos|startsession>");
            return true;
        }


        switch (args[0].toLowerCase()) {
            case "pardonall" -> unbanDeadPlayers(sender);
            case "list" -> dumpDeadList(sender);
            case "toggle" -> toggleTempDeath(sender);
            case "info" -> sender.sendMessage("§fTemporary death is currently set to: "+ plugin.getConfig().getBoolean("temp-death"));
            case "newarea" -> newArea(sender, args);
            case "rmarea" -> removeArea(sender, args);
            case "setpos" -> setpos(sender, args);
            case "startsession" -> startSession(sender);
            default -> sender.sendMessage("§cUnknown subcommand.");
        }


        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("pardonall", "list", "toggle", "info", "newarea", "rmarea", "setpos", "startsession")
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
                if (entry.getSource().equals("nonPermDeath")) {
                    deadPlayers.add(target.getName());
                }
            }

            if (deadPlayers.isEmpty()) {
                sender.sendMessage("§6§lNo temporarily banned players :( !");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("<gold><bold>List of temporarily banned players "
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

    public void unbanDeadPlayers(CommandSender sender) {
        ProfileBanList profileBanList = (ProfileBanList) Bukkit.getBanList(BanList.Type.PROFILE);
        IpBanList ipBanList = (IpBanList) Bukkit.getBanList(BanList.Type.IP);

        ArrayList<String> revivedPlayers = new ArrayList<String>();

        for (BanEntry<? super PlayerProfile> entry : profileBanList.getEntries()) {
            PlayerProfile target = (PlayerProfile) entry.getBanTarget();
            if (! (entry.getSource().isEmpty())  && entry.getSource().equals("nonPermDeath")) {
                profileBanList.pardon(target);
                ipBanList.pardon(target.getName());
                revivedPlayers.add(target.getName());
            }
        }

        for (BanEntry<? super InetAddress> entry : ipBanList.getEntries()) {
            if (! (entry.getSource().isEmpty()) && entry.getSource().equals("nonPermDeath")) {
                ipBanList.pardon(entry.getTarget());
            }
        }

            if (revivedPlayers.isEmpty() ) {
            sender.sendMessage("§cNo revivable dead players !§r");
        } else if (revivedPlayers.size() == 1) {
            sender.sendMessage("§aSuccesfully revived 1 player !§r");
        } else {
            sender.sendMessage("§aSuccesfully revived "+ revivedPlayers.size() +" players !§r");
        }

    }

    public void toggleTempDeath(CommandSender sender) {
        plugin.getConfig().set("temp-death", ! (plugin.getConfig().getBoolean("temp-death")));
        plugin.saveConfig();
        sender.sendMessage("Temporary death is now set to : "+ plugin.getConfig().getBoolean("temp-death"));
    }

    public void newArea(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage("§cMissing name : action failed.§r");
            return;
        }

        String name = args[1].toLowerCase();

        if (plugin.getConfig().contains(name)) {
            sender.sendMessage("§cThis area already exists. Use /civ rmarea "+name+" to delete it.§r");
            return;
        }

        if (args.length > 2) {
            sender.sendMessage("§cWrong syntax : /civ newarea <name>");
            return;
        }

        plugin.getConfig().set("areas."+name+".x1", 0);
        plugin.getConfig().set("areas."+name+".z1", 0);
        plugin.getConfig().set("areas."+name+".x2", 0);
        plugin.getConfig().set("areas."+name+".z2", 0);

        plugin.saveConfig();

        sender.sendMessage("§a"+name+" is set, define coordinates using /civ setpos§r");

    }

    public void setpos (CommandSender sender, String[] args) {
        int corner = 0;
        int x;
        int z;

        boolean usePlayerCoords = false;

        sender.sendMessage(String.valueOf(args.length));

        if (args.length != 5 && args.length != 3) {
            sender.sendMessage("§cCorrect syntax : /civ setpos <name> <pos1|pos2> <x> <z>§r");
            return;
        }

        if (args.length == 3) {
            usePlayerCoords = true;
        }


        switch (args[2]) {
            case "pos1" -> corner = 1;
            case "pos2" -> corner = 2;
            default -> {
                sender.sendMessage("§cSpecify corner : <pos1|pos2>. Correct syntax : /civ setpos <name> <pos1|pos2> <x> <z>§r");
                return;
            }
        }



        Player player = null;
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cConsole cannot execute this command");
            return;
        }
        player = p;

        if (usePlayerCoords) {
            x = player.getLocation().getBlockX();
            z = player.getLocation().getBlockZ();
        } else {
            try {
                x = Integer.parseInt(args[2]);
                z = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cCoordinates <x> and <z> have to be integers !§r");
                return;
            }
        }

        if (x > 29999999 || z > 29999999)  {
            sender.sendMessage("§cCoordinates cannot be higher than 30 Million blocks§r");
            return;
        }

        String name = args[1].toLowerCase();

        plugin.getConfig().set("areas."+name+'.'+'x'+corner, x);
        plugin.getConfig().set("areas."+name+'.'+'z'+corner, z);

        plugin.saveConfig();

        sender.sendMessage("§aCorner §e"+ corner +"§a of area §d"+ name + "§a set to coordinates §e " + x + ' ' + z + "§r");

    }

    public void removeArea(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§cWrong syntax : /civ rmarea <name>");
            return;
        }

        String name = args[1].toLowerCase();

        if (!(plugin.getConfig().contains("areas."+name))) {
            sender.sendMessage("§eNo area §d"+ name + "§e found. Config was not modified.§r");
            return;
        }

        plugin.getConfig().set("areas."+name, null);

        plugin.saveConfig();

        sender.sendMessage("§aSuccessfully deleted area §d" + args[1] + ".§r");
    }

    public boolean tpPlayer(Player player, String area, Boolean start) {
        String uuid = player.getUniqueId().toString();
        int x1 = plugin.getConfig().getInt("areas." + area + ".x1");
        int z1 = plugin.getConfig().getInt("areas." + area + ".z1");
        int x2 = plugin.getConfig().getInt("areas." + area + ".x2");
        int z2 = plugin.getConfig().getInt("areas." + area + ".z2");

        int rx;
        int rz;

        String configWorld = plugin.getConfig().getString("world");

        Random rand = new Random();

        World world;

        if (configWorld == null || configWorld.trim().isEmpty()) {
            world = Bukkit.getWorld("world");
        } else {
            world = Bukkit.getWorld(configWorld);
        }

        if (world == null) {
            plugin.getLogger().severe("[OSE_Civilisation] No world '" + configWorld + "' found !");
            return false;
        }

        for (int i = 0; i < 1000; i++) {
            rx = Math.min(x1, x2) + rand.nextInt(Math.abs(x1 - x2) + 1);
            rz = Math.min(x1, x2) + rand.nextInt(Math.abs(z1 - z2) + 1);

            Block pos = world.getHighestBlockAt(rx, rz);

            if (pos.getType() == Material.WATER || pos.getType() == Material.LAVA || pos.getType() == Material.KELP || pos.getType() == Material.TALL_SEAGRASS) {
                continue;
            } else {
                while (pos.isPassable()) {
                    pos = pos.getRelative(BlockFace.DOWN);
                }
            }

            Location loc = new Location(world, rx + 0.5, pos.getY() + 1, rz + 0.5);

            world.getChunkAtAsync(loc).thenAccept(chunk -> {
                player.teleportAsync(loc);
            });
            if (!start) {
                List<String> uuidlist = plugin.getConfig().getStringList("teleported-players");
                uuidlist.add(uuid);
                plugin.getConfig().set("teleported-players", uuidlist);
                plugin.saveConfig();
            }
            return true;


            //  Tom horror.z0 = faux Jola
        }
        return false;
    }

    public boolean tpPlayer(Player player, String area) {
        return tpPlayer(player, area, false);
    }

    private Set<String> sessionUUIDs = new HashSet<>();

    public void startSession(CommandSender sender) {
        if (plugin.getConfig().getBoolean("session-started")) {
            sender.sendMessage("§cSession already started !§r");
            return;
        }
        plugin.getConfig().set("session-started", true);
        plugin.saveConfig();

        ConfigurationSection areasSection = plugin.getConfig().getConfigurationSection("areas");

        if (areasSection == null) {
            plugin.getLogger().warning("[OSE_Civilisation] No areas config found");
            return;
        }

        Set<String> areaNames = areasSection.getKeys(false);

        sender.sendMessage("§eAreas found : §d" + String.join("§e, §d", areaNames) + "§e.§r");

        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

        for (Player p : onlinePlayers) {
            for (String name : areaNames) {
                String permission = "oseciv.area." + name;
                if (p.hasPermission(permission)) {
                    tpPlayer(p, name, true);
                    if (sessionUUIDs.add(p.getUniqueId().toString())) {
                        return;
                    } else {
                        plugin.getLogger().severe("[OSE_Civilisation] No suitable spawn point found, session start is impossible !");
                        return;
                    }
                }
            }
        }
        plugin.getConfig().set("teleported-players", sessionUUIDs);
        plugin.saveConfig();
    }

}

