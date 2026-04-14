package fr.tbrpower.ose_civilisation;
import fr.tbrpower.ose_civilisation.commands.CivCommands;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;

import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


public class OSE_Civilisation extends JavaPlugin implements  Listener {

    private CivCommands civCommands;

    Component messageday1 = (Component) MiniMessage.miniMessage().deserialize("""
           <red><bold>Vous avez été tué !</bold></red>
               \s
           <#900000><italic>Vous ne pouvez donc plus respawn...</italic></#900000>
               \s
               \s
           <#4573FF><bold>CEPENDANT !</bold>
               \s
           Vous êtes mort au jour 1, vous réapparaitrez donc demain.</#4573FF>
               \s
               \s
           \s""");

    Component messageNormal = (Component) MiniMessage.miniMessage().deserialize("""
           <red><bold>Vous avez été tué !</bold></red>
               \s
           <#900000><italic>Vous ne pouvez donc plus respawn.</italic></#900000>
               \s
               \s
           <#4573FF><bold>Merci d'avoir joué !</bold>
               \s
           Restez sur le discord, de prochains évènements seront annoncés...</#4573FF>
               \s
               \s
           \s""");

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getConfig().addDefault("temp-death", false);
        getConfig().addDefault("world", null);
        getConfig().addDefault("session-started", false);
        getConfig().addDefault("areas", new HashMap<String, Object>());
        getConfig().addDefault("teleported-players", new ArrayList<String>());

        getConfig().options().copyDefaults(true);
        saveConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("[OSE_Civilisation] Plugin civilisation activé !");

        this.civCommands = new CivCommands(this);
        getCommand("civ").setExecutor(civCommands);
        getCommand("civ").setTabCompleter(civCommands);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        boolean tempDeath = getConfig().getBoolean("temp-death");
        String deathSource;
        Component message;

        String playerName = event.getEntity().getName();
        if (event.getEntity().getAddress() == null) return;

        if (tempDeath) {deathSource = "nonPermDeath"; message = messageday1;} else {deathSource = "permDeath"; message = messageNormal;}

        if (! event.getPlayer().hasPermission("oseciv.bypass")) {


            event.getEntity().banIp(deathSource, //Ban message
                    (Duration) null, //Ban Duration, duration for test Instant.now().plus(Duration.ofSeconds(60))
                    deathSource, //Source of Ban
                    false //Kick on ban
            );

            event.getEntity().ban(deathSource,
                    (Duration) null,
                    deathSource,
                    false
            );

            event.setKeepInventory(false);
            event.getEntity().getInventory().clear();
            event.getEntity().getInventory().setArmorContents(null);
            event.getEntity().getInventory().setArmorContents(null);
            event.getEntity().getInventory().setItemInOffHand(null);

            event.getEntity().kick((Component) message);
        }
    }

    // BAN CHECK
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        PlayerProfile profile = event.getPlayerProfile();
        BanList ipBanList = Bukkit.getBanList(BanList.Type.IP);
        ProfileBanList profileBanList = (ProfileBanList) Bukkit.getBanList(BanList.Type.PROFILE);
        BanEntry ipentry = ipBanList.getBanEntry(ip);
        Component message = messageNormal;

        if (ipentry != null) {
            if (ipentry.getSource().equals("nonPermDeath")) {message = messageday1;} else if (ipentry.getSource().equals("permDeath")) { message = messageNormal;}
        }
        if (ipBanList.isBanned(ip)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
        } else if (profileBanList.isBanned(profile)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
        }
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (event.getPlayer().hasPermission("oseciv.bypass")) {
            return;
        }
        ConfigurationSection areasSection = getConfig().getConfigurationSection("areas");
        if (areasSection == null) {
            getLogger().warning("[OSE_Civilisation] No areas config found");
            return;
        }
        Set<String> areaNames = areasSection.getKeys(false);

        if (getConfig().getStringList("teleported-players").contains(event.getPlayer().getUniqueId().toString())) {
            getLogger().info("§7Player " + event.getPlayer().getName() + "(" + event.getPlayer().getUniqueId() + ") has already been telported.");
            return;
        }

        for (String name : areaNames) {
            String permission = "oseciv.area." + name;
            if (event.getPlayer().hasPermission(permission)) {
                civCommands.tpPlayer(event.getPlayer(), name);
                break;
            } else {
                getLogger().warning("[OSE_Civilisation] User " + event.getPlayer().getName() + "(" + event.getPlayer().getUniqueId() + ") has no area defined !");
                break;
            }
        }
    }



}