package fr.tbrpower.ose_civilisation.commands;

import fr.tbrpower.ose_civilisation.OSE_Civilisation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class Freeze implements Listener {
    private final OSE_Civilisation plugin;

    public Freeze(OSE_Civilisation plugin) {
        this.plugin = plugin;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (plugin.getFrozen().contains(player.getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();

            to.set(from.getX(), from.getY(), from.getZ());

            event.setTo(to);
        }
    }

    public void freeze5s(Player player) {
        plugin.getFrozen().add(player.getUniqueId());
        player.setFreezeTicks(5*20);

        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getFrozen().remove(player.getUniqueId()), 20L*5);
    }
}
