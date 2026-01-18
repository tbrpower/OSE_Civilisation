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


    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("&9[CivilisationDeathEffects]&r Plugin civilisation activé !");

        CivCommands cmd = new CivCommands();
        getCommand("civ").setExecutor(cmd);
        getCommand("civ").setTabCompleter(cmd);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        String playerName = event.getEntity().getName();
        if (event.getEntity().getAddress() == null) return;

        if (! event.getPlayer().hasPermission("civde.bypass")) {


            event.getEntity().banIp("nonPermDeath", //Ban message
                    (Duration) null, //Ban Duration, duration for test Instant.now().plus(Duration.ofSeconds(60))
                    "nonPermDeath", //Source of Ban
                    false //Kick on ban
            );

            event.getEntity().ban("nonPermDeath",
                    (Duration) null,
                    "nonPermDeath",
                    false
            );


            event.getEntity().kick((Component) messageday1);
        }
    }

    // BAN CHECK
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        PlayerProfile profile = event.getPlayerProfile();
        BanList ipBanList = Bukkit.getBanList(BanList.Type.IP);
        ProfileBanList profileBanList = (ProfileBanList) Bukkit.getBanList(BanList.Type.PROFILE);

        if (ipBanList.isBanned(ip)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, messageday1);
        } else if (profileBanList.isBanned(profile)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, messageday1);
        }
    }


}