package fr.tbrpower.ose_civilisation.commands;

import fr.tbrpower.ose_civilisation.OSE_Civilisation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CivUtils {

    private final OSE_Civilisation plugin;

    public CivUtils(OSE_Civilisation plugin) { this.plugin = plugin;}

    public String confirmRequestMessage = "§c§lWARNING : ACTION CANNOT BE REVERTED§r\n§eConfirm action using §2/civ confirm§r§e. Cancel using §2/civ cancel§r";

    public enum PendingAction {
        START_SESSION,
        CANCEL_SESSION,
        REMOVE_AREA,
        REVIVE_PLAYERS,
        PAUSE_SESSION
    }

    public class PendingConfirmation{
        private final CivUtils.PendingAction action;
        private final Object data;
        private final UUID uniqueUserID;

        public PendingConfirmation(PendingAction action, Object data, UUID uniqueUserID) {
            this.action = action;
            this.data = data;
            this.uniqueUserID = uniqueUserID;
        }
    }

    public final List<CivUtils.PendingConfirmation> confirmationList = new ArrayList<CivUtils.PendingConfirmation>();

    public void confirm(CommandSender sender, CivBans civBans, CivAreas civAreas, CivSessions civSessions) {
        boolean playerHasAction = false;

        // senderUUID
        UUID senderUUID;
        if (sender instanceof Player player) {
            senderUUID = player.getUniqueId();
        }
        else {
            // If sender is CONSOLE
            senderUUID = new UUID(0,0);
        }

        if (confirmationList.isEmpty()) {
            sender.sendMessage("§cNo actions to confirm !");
            return;
        }

        var iterator = confirmationList.iterator();
        while (iterator.hasNext() && !playerHasAction) {
            PendingConfirmation confirmation = iterator.next();
            if (confirmation.uniqueUserID.equals(senderUUID)) {
                playerHasAction = true;
                switch (confirmation.action) {
                    case CANCEL_SESSION -> {
                        civSessions.cancelSession(sender, true);
                        iterator.remove();
                    }
                    case START_SESSION -> {
                        civSessions.startSession(sender, true);
                        iterator.remove();
                    }
                    case REMOVE_AREA -> {
                        if (confirmation.data instanceof String[] args) {
                            civAreas.removeArea(sender, args, true);
                        }
                        iterator.remove();
                    }
                    case REVIVE_PLAYERS -> {
                        if (confirmation.data instanceof String reason) {
                            civBans.unbanPlayersWithReason(sender, reason, true);
                        }
                        iterator.remove();
                    }
                    case PAUSE_SESSION -> {
                        civSessions.pauseSession(sender, true);
                        iterator.remove();
                    }
                }

            }
        }
        if (!playerHasAction) {
            sender.sendMessage("§cNo actions to confirm !");
        }
    }

    public void cancel(CommandSender sender) {

        Boolean playerHasAction = false;

        // senderUUID
        UUID senderUUID;
        if (sender instanceof Player player) {
            senderUUID = player.getUniqueId();
        }
        else {
            // If sender is CONSOLE
            senderUUID = new UUID(0,0);
        }

        if (confirmationList.isEmpty()) {
            sender.sendMessage("§cNo actions left to cancel !");
            return;
        }

        var iterator = confirmationList.iterator();
        while (iterator.hasNext()) {
            PendingConfirmation confirmation = iterator.next();
            if (confirmation.uniqueUserID.equals(senderUUID)) {
                iterator.remove();
                playerHasAction = true;
            }
        }

        if (!playerHasAction) {
            sender.sendMessage("§cNo actions to cancel !");
        }
    }

    public void reload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage("§dCiv plugin reloaded§r");
    }

    public void civInfo(CommandSender sender) {
        sender.sendMessage("§fTemporary death is currently set to: "+ plugin.getConfig().getBoolean("temp-death"));
        sender.sendMessage("§fSession is active : "+ plugin.getConfig().getBoolean("session-started"));
        sender.sendMessage("§fSession is paused : " + plugin.getConfig().getBoolean("session-paused"));
    }
}
