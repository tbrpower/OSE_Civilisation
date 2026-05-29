// ONLY $U$ EVENTS
// https://discord.gg/aBZwDmQrBE

package fr.tbrpower.ose_civilisation;
import fr.tbrpower.ose_civilisation.commands.*;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;

import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.*;


public class OSE_Civilisation extends JavaPlugin implements  Listener {

    public CivCommands civCommands;
    public CivUtils civUtils;
    public CivAreas civAreas;
    public CivBans civBans;
    public CivSessions civSessions;
    public Freeze freeze;

    public final Set<UUID> frozen = new HashSet<>();

    public Set<UUID> getFrozen() {
        return frozen;
    }

    public enum BanReasons{
        TEMP_DEATH("nonPermDeath"),
        PERM_DEATH("permDeath");

        private final String banReason;

        BanReasons(String banReason) {
            this.banReason = banReason;
        }

        public String getReason() {
            return banReason;
        }
    }

    Component tempDeathMessage = (Component) MiniMessage.miniMessage().deserialize("""
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

    Component permDeathMessage = (Component) MiniMessage.miniMessage().deserialize("""
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

    public Component sessionPausedMessage = (Component) MiniMessage.miniMessage().deserialize("""
            <red><bold>L'événement est en pause !</bold></red>
            \s
            \s
            <white>Revenez demain à 20h !
            \s
            Faites une pause pendant ce temps là, allez dormir :)
            \s
            </white>
            \s
            """
    );

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getConfig().addDefault("temp-death", false);
        getConfig().addDefault("world", "");
        getConfig().addDefault("session-started", false);
        getConfig().addDefault("session-paused", false);
        getConfig().addDefault("areas", new HashMap<String, Object>());
        getConfig().addDefault("teleported-players", new ArrayList<String>());

        getConfig().options().copyDefaults(true);

        YamlConfiguration config = (YamlConfiguration) getConfig();

        config.setComments("temp-death", List.of(
                "If true, players will be able to be revived with the /civ pardontemp command",
                "If false, players will be banned forever.",
                "Pardon perma-banned players using /civ pardonperm",
                "Pardon temp-banned players using /civ pardontemp"
        ));

        config.setComments("world", List.of(
                "World used by the plugin, leave blank to use default world"
        ));

        config.setComments("session-started", List.of(
                "Will tp players to their area if true.",
                "Set to false to pause the session"
        ));

        config.setComments("session-paused", List.of(
                "Players cannot join when session is paused."
        ));

        config.setComments("areas", List.of(
                "List all areas.",
                "Players must have oseciv.area.<area-name> permission for the tp to work.",
                "",
                "Correct syntax:",
                "area-name:",
                "  display-name: ''",
                "  x1:",
                "  z1:",
                "  x2:",
                "  z2:"
        ));

        config.setComments("teleported-players", List.of(
                "Lists players who have already been tp-ed to their area.",
                "UUID List - Do not edit manually"
        ));

        saveConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("[OSE_Civilisation] Plugin civilisation activé !");

        this.freeze = new Freeze(this);
        this.civUtils = new CivUtils(this);
        this.civAreas = new CivAreas(this, civUtils);
        this.civBans  = new CivBans(this, civUtils);
        this.civSessions = new CivSessions(this, civUtils, freeze);
        getServer().getPluginManager().registerEvents(civSessions, this);

        this.civCommands = new CivCommands(this, civBans, civUtils, civAreas, civSessions);

        getCommand("civ").setExecutor(civCommands);
        getCommand("civ").setTabCompleter(civCommands);

        Freeze freeze = new Freeze(this);
        getServer().getPluginManager().registerEvents(freeze, this);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        boolean tempDeath = getConfig().getBoolean("temp-death");
        String deathSource;
        Component message;

        String playerName = event.getEntity().getName();
        if (event.getEntity().getAddress() == null) return;

        if (tempDeath) {deathSource = BanReasons.TEMP_DEATH.getReason(); message = tempDeathMessage;} else {deathSource = BanReasons.PERM_DEATH.getReason(); message = permDeathMessage;}

        if (! event.getPlayer().hasPermission("oseciv.bypass")) {


//            event.getEntity().banIp(deathSource, //Ban message
//                    (Duration) null, //Ban Duration, duration for test Instant.now().plus(Duration.ofSeconds(60))
//                    deathSource, //Source of Ban
//                    false //Kick on ban
//            );

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
        Component message = permDeathMessage;
        if (getConfig().getBoolean("session-paused")) {
            message = sessionPausedMessage;
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
            return;
        }

        PlayerProfile profile = event.getPlayerProfile();
        ProfileBanList profileBanList = (ProfileBanList) Bukkit.getBanList(BanList.Type.PROFILE);
        BanEntry<PlayerProfile> profileBanEntry = profileBanList.getBanEntry(profile);


        if (profileBanEntry != null) {
            if (profileBanEntry.getSource().equals(BanReasons.TEMP_DEATH.getReason())) {message = tempDeathMessage;} else if (profileBanEntry.getSource().equals(BanReasons.PERM_DEATH.getReason())) { message = permDeathMessage;}
        }
         if (profileBanList.isBanned(profile)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
        }
    }





}