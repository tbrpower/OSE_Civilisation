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

    public CivBans civBans;

    public CivAreas civAreas;

    public CivSessions civSessions;

    public String confirmRequestMessage = "§c%lWARNING : ACTION CANNOT BE REVERTED§r\n§eConfirm action using §2/civ confirm§r§e. Cancel using §2/civ cancel§r";

    public enum PendingAction {
        START_SESSION,
        CANCEL_SESSION,
        REMOVE_AREA,
        REVIVE_PLAYERS
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

    public void confirm(CommandSender sender) {
        UUID senderUUID;

        Boolean playerHasAction = false;

        if (sender instanceof Player player) {
            senderUUID = player.getUniqueId();
        } else {
            return;
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
                        civSessions.cancelSession(sender, true); iterator.remove();
                    }
                    case START_SESSION -> {
                        civSessions.startSession(sender, true);
                        iterator.remove();
                    }
                    case REMOVE_AREA -> {
                        String[] args = (String[])confirmation.data;
                        civAreas.removeArea(sender, args, true);
                        iterator.remove();
                    }
                    case REVIVE_PLAYERS -> {
                        civBans.unbanPlayersWithReason(sender, confirmation.data.toString(), true);
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
        UUID senderUUID;

        Boolean playerHasAction = false;

        if (sender instanceof Player player) {
            senderUUID = player.getUniqueId();
        } else {
            return;
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
}
