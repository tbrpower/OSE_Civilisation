package fr.tbrpower.civilisationdeatheffects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;

public class CivilisationDeathEffects extends JavaPlugin implements  Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("&9[CivilisationDeathEffects]&r Plugin civilisation activé !");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        String playerName = event.getEntity().getName();
        //String playerAdress = event.getPlayer().getAddress().getAddress().getHostAddress();
        if (event.getEntity().getAddress() == null) return ;


        event.getEntity().banIp("§dVous êtes mort§r, merci d'avoir joué !", //Ban message
                (Duration) null, //Ban Duration, duration for test Instant.now().plus(Duration.ofSeconds(60))
                "day1 ban", //Source of Ban (no idea what it is for)
                false //Kick on ban
        );
        event.getEntity().kick(MiniMessage.miniMessage().deserialize(""" 
                <red><bold>Vous avez été tué !</bold></red>
                
                <orange><italic>Vous ne pouvez plus respawn...</italic></orange>
                
                
                <blue>CEPENDANT !
                Vous êtes mort au jour 1, vous réapparaitrez donc demain.</blue>
                
                
                """))
        ;



        Bukkit.getServer().broadcast(Component.text("§cJoueur§r" + playerName));
    }
}