package fr.tbrpower.civilisationdeatheffects;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CivilisationDeathEffects extends JavaPlugin implements  Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("CivilisationDeathEffects activé");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        String playerName = event.getEntity().getName();
        Bukkit.getServer().broadcast(Component.text("§cJoueur§r" + playerName));
    }
}