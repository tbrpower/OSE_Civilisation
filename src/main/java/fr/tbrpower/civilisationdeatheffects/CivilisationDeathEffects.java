package fr.tbrpower.civilisationdeatheffects;
import fr.tbrpower.civilisationdeatheffects.commands.CivCommands;

import java.net.InetAddress;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.event.EventHandler;

import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;




public class CivilisationDeathEffects extends JavaPlugin implements  Listener {

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
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("[CivilisationDeathEffects] Plugin civilisation activé !");

        CivCommands cmd = new CivCommands(this);
        getCommand("civ").setExecutor(cmd);
        getCommand("civ").setTabCompleter(cmd);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        boolean tempDeath = getConfig().getBoolean("temp-death");
        String deathSource;
        Component message;

        String playerName = event.getEntity().getName();
        if (event.getEntity().getAddress() == null) return;

        if (tempDeath) {deathSource = "nonPermDeath"; message = messageday1;} else {deathSource = "permDeath"; message = messageNormal;}

        if (! event.getPlayer().hasPermission("civde.bypass")) {


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


}