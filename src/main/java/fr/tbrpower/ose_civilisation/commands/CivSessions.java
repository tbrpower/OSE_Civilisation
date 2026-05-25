package fr.tbrpower.ose_civilisation.commands;

import fr.tbrpower.ose_civilisation.OSE_Civilisation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.maven.model.PluginConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CivSessions implements Listener {

    Component sessionPausedMessage = (Component) MiniMessage.miniMessage().deserialize("""
            <red><bold>L'événement est en pause !</bold></red>
            \s
            \s
            <white>Revenez demain à 20h !
            \s
            Faites une pause pendant ce temps là, allez toucher de l'herbe :)
            \s
            </white>
            \s
            """
    );

    private final OSE_Civilisation plugin;

    private final CivUtils civUtils;

    private final Freeze freeze;

    public CivSessions(OSE_Civilisation plugin, CivUtils civUtils, Freeze freeze) {
        this.plugin = plugin;
        this.civUtils = civUtils;
        this.freeze = freeze;
    }

    private Set<String> sessionUUIDs = new HashSet<>();

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
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getLogger().info("[OSE_Civilisation]" + player.getName() + "(" + player.getUniqueId().toString() + ") teleported to " + loc.getBlockX() +' '+ loc.getBlockY() +' '+ loc.getBlockZ());

                        freeze.freeze5s(player);

                        player.getWorld().strikeLightningEffect(player.getLocation());

                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 5 * 20, 0, false, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120 * 20, 1, true, true, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 30 * 20, 0, false, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 480 * 20, 0, true, true, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30 * 20, 0, false, false, false));

                        player.sendTitle("§6" + area.substring(0, 1).toUpperCase() + area.substring(1) + "§r", "§2Bienvenue dans l'événement Civilisation ! §aBonne chance o7 !§r", 20, 5 * 20, 20 * 20);
                    });

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

        // senderUUID
        UUID senderUUID;
        if (sender instanceof Player player) {
            senderUUID = player.getUniqueId();
        }
        else {
            // If sender is CONSOLE
            senderUUID = new UUID(0,0);
        }

        if (confirmed) {
            plugin.getConfig().set("session-started", true);
            plugin.getConfig().set("session-paused", false);
            plugin.saveConfig();

            ConfigurationSection areasSection = plugin.getConfig().getConfigurationSection("areas");

            if (areasSection == null) {
                plugin.getLogger().warning("[OSE_Civilisation] No areas config found");
                return;
            }

            String configWorld = plugin.getConfig().getString("world");

            World world;

            if (configWorld == null || configWorld.trim().isEmpty()) {
                world = Bukkit.getWorld("world");
            } else {
                world = Bukkit.getWorld(configWorld);
            }

            if (world == null) {
                plugin.getLogger().severe("[OSE_Civilisation] No world '" + configWorld + "' found !");
                return;
            }

            sender.sendMessage("§eUsing world §f: §e" + world.getName());

            Set<String> areaNames = areasSection.getKeys(false);

            sender.sendMessage("§eAreas found : §d" + String.join("§e, §d", areaNames) + "§e.§r");

            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            List<Player> players = new ArrayList<>(onlinePlayers);

            for (Player p : players) {
                if (p.hasPermission("oseciv.bypass")) continue;

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
            sender.sendMessage(civUtils.confirmRequestMessage);
            civUtils.confirmationList.add(civUtils.new PendingConfirmation(CivUtils.PendingAction.START_SESSION, null, senderUUID));
        }
    }

    public void startSession(CommandSender sender) {
        startSession(sender, false);
    }

    public void cancelSession(CommandSender sender, Boolean confirmed) {

        // senderUUID
        UUID senderUUID;
        if (sender instanceof Player player) {
            senderUUID = player.getUniqueId();
        }
        else {
            // If sender is CONSOLE
            senderUUID = new UUID(0,0);
        }

        if (plugin.getConfig().getBoolean("session-started")) {
            if (confirmed) {
                plugin.getConfig().set("session-started", false);
                plugin.getConfig().set("session-paused", false);
                plugin.getConfig().set("teleported-players", new ArrayList<String>());
                plugin.saveConfig();
                plugin.reloadConfig();
                sender.sendMessage("§aSession cancelled !");
            } else {
                sender.sendMessage(civUtils.confirmRequestMessage);
                civUtils.confirmationList.add(civUtils.new PendingConfirmation(CivUtils.PendingAction.CANCEL_SESSION, null, senderUUID));
            }
        } else {
            sender.sendMessage("§cSession not active§r");
        }
    }

    public void cancelSession(CommandSender sender) {
        cancelSession(sender, false);
    }

    public void pauseSession(CommandSender sender, boolean confirmed) {
        FileConfiguration config = plugin.getConfig();
        // senderUUID
        UUID senderUUID;
        if (sender instanceof Player player) {
            senderUUID = player.getUniqueId();
        }
        else {
            // If sender is CONSOLE
            senderUUID = new UUID(0,0);
        }
        if (config.getBoolean("session-started")) {
            if (confirmed) {
                if (config.getBoolean("session-paused")) {
                    config.set("session-paused", false);
                    plugin.saveConfig();

                    sender.sendMessage("§a§lSession unpaused ! §aPlayers can now join !§r");
                } else {
                    config.set("session-paused", true);
                    plugin.saveConfig();

                    new ArrayList<>(Bukkit.getOnlinePlayers()).stream()
                        .filter(p -> !p.hasPermission("oseciv.bypass"))
                        .forEach(p -> p.kick(sessionPausedMessage, PlayerKickEvent.Cause.PLUGIN));

                    sender.sendMessage("§a§lSession paused ! §aAll players were kicked !§r");
                }
            } else {
                sender.sendMessage(civUtils.confirmRequestMessage);
                civUtils.confirmationList.add(civUtils.new PendingConfirmation(CivUtils.PendingAction.PAUSE_SESSION, null, senderUUID));
              }
        } else {
            sender.sendMessage("§cSession is not started !§r");
        }
    }

    public void pauseSession(CommandSender sender) {
        pauseSession(sender, false);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getPlayer().hasPermission("oseciv.bypass")) {
            return;
        }

        if (plugin.getConfig().getBoolean("session-paused")) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, sessionPausedMessage);
            return;
        }

        ConfigurationSection areasSection = plugin.getConfig().getConfigurationSection("areas");
        if (areasSection == null) {
            plugin.getLogger().warning("[OSE_Civilisation] No areas config found");
            return;
        }
        Set<String> areaNames = areasSection.getKeys(false);

        if (plugin.getConfig().getStringList("teleported-players").contains(event.getPlayer().getUniqueId().toString())) {
            plugin.getLogger().info("§7Player " + event.getPlayer().getName() + "(" + event.getPlayer().getUniqueId() + ") has already been telported.");
            return;
        }

        for (String name : areaNames) {
            String permission = "oseciv.area." + name;
            if (event.getPlayer().hasPermission(permission)) {
                tpPlayer(event.getPlayer(), name);
                return;
            }
        }
        plugin.getLogger().warning("[OSE_Civilisation] User " + event.getPlayer().getName() + "(" + event.getPlayer().getUniqueId() + ") has no area defined !");
    }

}
