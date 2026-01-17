package fr.tbrpower.civilisationdeatheffects;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;

public class CivilisationDeathEffects extends JavaPlugin implements  Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("§9[CivilisationDeathEffects]§r Plugin civilisation activé !");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        String playerName = event.getEntity().getName();
        //String playerAdress = event.getPlayer().getAddress().getAddress().getHostAddress();
        if (event.getEntity().getAddress() == null) return ;
        event.getEntity().banIp("Vous êtes mort", Instant.now(), "Server", true);


        Bukkit.getServer().broadcast(Component.text("§cJoueur§r" + playerName));
    }
}