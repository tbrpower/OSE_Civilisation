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
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.JavaBean;
import java.beans.PersistenceDelegate;
import java.net.InetAddress;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.savePlayers;


public class CivCommands implements CommandExecutor, TabCompleter {


    private final OSE_Civilisation plugin;
    public CivCommands(OSE_Civilisation plugin) { this.plugin = plugin;}

    private Set<String> sessionUUIDs = new HashSet<>();

    public enum PendingAction {
        START_SESSION,
        CANCEL_SESSION,
        REMOVE_AREA,
        REVIVE_PLAYERS
    }

    public class PendingConfirmation{
        private final PendingAction action;
        private final Object data;
        private final UUID uniqueUserID;

        public PendingConfirmation(PendingAction action, Object data, UUID uniqueUserID) {
            this.action = action;
            this.data = data;
            this.uniqueUserID = uniqueUserID;
        }
    }

    public final List<PendingConfirmation> confirmationList = new ArrayList<PendingConfirmation>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cInvalid syntax. Correct use : /civ <pardonall|pardonperm|listtemp|listperm|toggle|info|newarea|rmarea|setpos|startsession|cancelsession|reload|confirm|cancel>");
            return true;
        }


        switch (args[0].toLowerCase()) {
            case "pardontemp" -> unbanDeadPlayers(sender);
            case "pardonperm" -> unbanPermaDeadPlayers(sender);
            case "listtemp" -> dumpDeadList(sender);
            case "listperm" -> dumpPermaDeadList(sender);
            case "toggle" -> toggleTempDeath(sender);
            case "info" -> sender.sendMessage("§fTemporary death is currently set to: "+ plugin.getConfig().getBoolean("temp-death"));
            case "newarea" -> newArea(sender, args);
            case "rmarea" -> removeArea(sender, args);
            case "setpos" -> setpos(sender, args);
            case "startsession" -> startSession(sender);
            case "cancelsession" -> cancelSession(sender);
            case "confirm" -> confirm(sender);
            case "cancel" -> cancel(sender);
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage("§dCiv plugin reloaded§r");
            }
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

    public void dumpPermaDeadList(CommandSender sender) {
        if (sender != null) {
            ProfileBanList profileBanList = (ProfileBanList) Bukkit.getBanList(BanList.Type.PROFILE);

            ArrayList<String> deadPlayers = new ArrayList<String>();

            for (BanEntry<? super PlayerProfile> entry : profileBanList.getEntries()) {
                PlayerProfile target = (PlayerProfile) entry.getBanTarget();
                if (entry.getSource().equals("permDeath")) {
                    deadPlayers.add(target.getName());
                }
            }

            if (deadPlayers.isEmpty()) {
                sender.sendMessage("§6§lNo permanently dead players :( !");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("<gold><bold>List of permanently dead players "
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

    public void unbanPermaDeadPlayers(CommandSender sender, Boolean confirmed) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCannot unban dead players as CONSOLE§r");
            return;
        }

        if (confirmed) {
            ProfileBanList profileBanList = (ProfileBanList) Bukkit.getBanList(BanList.Type.PROFILE);
            IpBanList ipBanList = (IpBanList) Bukkit.getBanList(BanList.Type.IP);

            ArrayList<String> revivedPlayers = new ArrayList<String>();

            for (BanEntry<? super PlayerProfile> entry : profileBanList.getEntries()) {
                PlayerProfile target = (PlayerProfile) entry.getBanTarget();
                if (! (entry.getSource().isEmpty())  && entry.getSource().equals("permDeath")) {
                    profileBanList.pardon(target);
                    ipBanList.pardon(target.getName());
                    revivedPlayers.add(target.getName());
                }
            }

            for (BanEntry<? super InetAddress> entry : ipBanList.getEntries()) {
                if (! (entry.getSource().isEmpty()) && entry.getSource().equals("permDeath")) {
                    ipBanList.pardon(entry.getTarget());
                }
            }

            if (revivedPlayers.isEmpty() ) {
                sender.sendMessage("§cNo permanently dead players !§r");
            } else if (revivedPlayers.size() == 1) {
                sender.sendMessage("§aSuccesfully revived 1 player !§r");
            } else {
                sender.sendMessage("§aSuccesfully revived "+ revivedPlayers.size() +" players !§r");
            }
        } else {
            sender.sendMessage("§c§lWARNING :§r§c UNBANS CANNOT BE REVERTED. If you want to proceed type '/civ confirm'. §a§lTo cancel type '/civ cancel'§r");
            confirmationList.add(new PendingConfirmation(PendingAction.REVIVE_PLAYERS, null ,player.getUniqueId()));
        }
    }

    public void unbanPermaDeadPlayers(CommandSender sender) {
        unbanPermaDeadPlayers(sender, false);
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

        if (plugin.getConfig().contains("area."+name)) {
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

        sender.sendMessage("§d"+name+"§a is set, define coordinates using /civ setpos§r");

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
                x = Integer.parseInt(args[3]);
                z = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cCoordinates <x> and <z> have to be integers !§r");
                return;
            }
        }

        if (Math.abs(x) > 29999999 || Math.abs(z) > 29999999)  {
            sender.sendMessage("§cCoordinates cannot be higher than 30 Million blocks§r");
            return;
        }

        String name = args[1].toLowerCase();

        if (!plugin.getConfig().contains("areas." + name)) {
            sender.sendMessage("§cArea §d"+name+"§c does not exist. Create it using /civ newarea§r");
            return;
        }

        plugin.getConfig().set("areas."+name+'.'+'x'+corner, x);
        plugin.getConfig().set("areas."+name+'.'+'z'+corner, z);

        plugin.saveConfig();

        sender.sendMessage("§aCorner §e"+ corner +"§a of area §d"+ name + "§a set to coordinates §e " + x + ' ' + z + "§r");

    }

    public void removeArea(CommandSender sender, String[] args, Boolean confirmed) {
        if (args.length != 2) {
            sender.sendMessage("§cWrong syntax : /civ rmarea <name>");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCannot remove area as CONSOLE§r");
            return;
        }

        String name = args[1].toLowerCase();

        if (!(plugin.getConfig().contains("areas."+name))) {
            sender.sendMessage("§eNo area §d"+ name + "§e found. Config was not modified.§r");
            return;
        }

        if (confirmed) {
            plugin.getConfig().set("areas." + name, null);

            plugin.saveConfig();

            sender.sendMessage("§aSuccessfully deleted area §d" + args[1] + ".§r");
        } else {
            sender.sendMessage("§eConfirm action using §2/civ confirm§r");
            confirmationList.add(new PendingConfirmation(PendingAction.REMOVE_AREA, args ,player.getUniqueId()));
        }
    }

    public void removeArea(CommandSender sender, String[] args) {
        removeArea(sender, args, false);
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

        plugin.getLogger().info(configWorld);

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
            rz = Math.min(z1, z2) + rand.nextInt(Math.abs(z1 - z2) + 1);

            Block pos = world.getHighestBlockAt(rx, rz);

            if (pos.getType() == Material.WATER || pos.getType() == Material.LAVA || pos.getType() == Material.KELP || pos.getType() == Material.TALL_SEAGRASS) {
                continue;
            } else {
                while (pos.isPassable()) {
                    pos = pos.getRelative(BlockFace.DOWN);
                }
            }

            plugin.getLogger().info(String.valueOf(pos.getX()) + "|" + i);
            plugin.getLogger().info(String.valueOf(pos.getZ()) + "|" + i);

            Location loc = new Location(world, rx + 0.5, pos.getY() + 1, rz + 0.5);

            player.teleportAsync(loc).thenAccept(success -> {
                if (success) {
                    plugin.getLogger().info("[OSE_Civilisation]" + player.getName() + "(" + player.getUniqueId().toString() + ") teleported to " + loc.getBlockX() + loc.getBlockY() + loc.getBlockZ());

                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10*20, 0, false, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 5*20, 0, false, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120*20, 0, true, true, false));

                    player.sendTitle("§6"+area.substring(0,1).toUpperCase()+area.substring(1) + "§r", "§2Bienvenue dans l'événement Civilisation ! §aBonne chance o7 !§r", 20, 5*20, 20*20);
                } else {
                    plugin.getLogger().warning("[OSE_Civilisation] Failed teleporting user " + player.getName() + "(" + player.getUniqueId().toString() + ")");
                }
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

    public void startSession(CommandSender sender, Boolean confirmed) {
        if (plugin.getConfig().getBoolean("session-started")) {
            sender.sendMessage("§cSession already started !§r");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCannot start session as CONSOLE§r");
            return;
        }

        if (confirmed) {
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
            List<Player> players = new ArrayList<>(onlinePlayers);

            for (Player p : players) {
                for (String name : areaNames) {
                    String permission = "oseciv.area." + name;
                    if (p.hasPermission(permission)) {
                        if (!tpPlayer(p, name, true)) {
                            plugin.getLogger().severe("[OSE_Civilisation] No suitable spawn point found, session start is impossible !");
                            return;
                        }
                        if (sessionUUIDs.add(p.getUniqueId().toString())) {
                            break;
                        }

                    }
                }
            }
            plugin.getConfig().set("teleported-players", sessionUUIDs);
            plugin.saveConfig();
        } else {
            sender.sendMessage("§eConfirm action using §2/civ confirm§r");
            confirmationList.add(new PendingConfirmation(PendingAction.START_SESSION, null ,player.getUniqueId()));
        }
    }

    public void startSession(CommandSender sender) {
        startSession(sender, false);
    }

    public void cancelSession(CommandSender sender, Boolean confirmed) {

        if (!(sender instanceof Player player)) {
            return;
        }

        if (plugin.getConfig().getBoolean("session-started")) {
            if (confirmed) {
                plugin.getConfig().set("session-started", false);
                plugin.getConfig().set("teleported-players", new ArrayList<String>());
                plugin.reloadConfig();
                sender.sendMessage("§aSession cancelled !");
            } else {
                sender.sendMessage("§eConfirm action using §2/civ confirm§r");
                confirmationList.add(new PendingConfirmation(PendingAction.CANCEL_SESSION, null ,player.getUniqueId()));
            }
        } else {
            sender.sendMessage("§cSession not active§r");
        }
    }

    public void cancelSession(CommandSender sender) {
        cancelSession(sender, false);
    }

    public void confirm(CommandSender sender) {
        UUID senderUUID;

        Boolean playerHasAction = false;

        if (sender instanceof Player player) {
            senderUUID = player.getUniqueId();
        } else {
            return;
        }

        if (confirmationList.isEmpty()) {
            sender.sendMessage("§cNo actions to confirm !");
            return;
        }

        for (PendingConfirmation confirmation : confirmationList) {
            if (confirmation.uniqueUserID == senderUUID) {
                playerHasAction = true;
                switch (confirmation.action) {
                    case CANCEL_SESSION ->
                            cancelSession(sender, true);
                    case START_SESSION ->
                            startSession(sender, true);
                    case REMOVE_AREA -> {
                            String[] args = (String[])confirmation.data;
                            removeArea(sender, args, true);
                        }
                    case REVIVE_PLAYERS -> unbanPermaDeadPlayers(sender, true);
                    }
                    confirmationList.remove(confirmation);
                }
                }
        if (!playerHasAction) {
            sender.sendMessage("§cNo actions to confirm !");
        }
        }

    public void cancel(CommandSender sender) {
        UUID senderUUID;

        Boolean playerHasAction = false;

        if (sender instanceof Player player) {
            senderUUID = player.getUniqueId();
        } else {
            return;
        }

        if (confirmationList.isEmpty()) {
            sender.sendMessage("§cNo actions left to cancel !");
            return;
        }

        for (PendingConfirmation confirmation : confirmationList) {
            if (confirmation.uniqueUserID == senderUUID) {
                playerHasAction = true;
                confirmationList.remove(confirmation);
            }
        }
        if (!playerHasAction) {
            sender.sendMessage("§cNo actions to cancel !");
        }
    }
    }


