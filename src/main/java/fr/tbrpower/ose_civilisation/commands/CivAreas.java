package fr.tbrpower.ose_civilisation.commands;

import fr.tbrpower.ose_civilisation.commands.CivUtils.PendingAction;
import fr.tbrpower.ose_civilisation.OSE_Civilisation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CivAreas {
    private final OSE_Civilisation plugin;
    private final CivUtils civUtils;

    public CivAreas(OSE_Civilisation plugin, CivUtils civUtils) {this.plugin = plugin; this.civUtils = civUtils;}

    public void newArea(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage("§cCorrect syntax : /civ newarea <name> [display-name>]§r");
            return;
        }

        String name = args[1].toLowerCase();

        if (plugin.getConfig().contains("area."+name)) {
            sender.sendMessage("§cThis area already exists. Use /civ rmarea "+name+" to delete it.§r");
            return;
        }

        if (args.length > 3) {
            sender.sendMessage("§cWrong syntax : /civ newarea <name>");
            return;
        }

        if (args.length == 3) {
            plugin.getConfig().set("areas."+name+".display-name", args[2]);
        } else {
            plugin.getConfig().set("areas."+name+".display-name", args[1]);
        }

        plugin.getConfig().set("areas."+name+".x1", 0);
        plugin.getConfig().set("areas."+name+".z1", 0);
        plugin.getConfig().set("areas."+name+".x2", 0);
        plugin.getConfig().set("areas."+name+".z2", 0);

        plugin.saveConfig();

        sender.sendMessage("§d"+name+"§a is set, define coordinates using /civ setpos§r");

    }

    public void removeArea(CommandSender sender, String[] args, Boolean confirmed) {
        if (args.length != 2) {
            sender.sendMessage("§cWrong syntax : /civ rmarea <name>");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCannot remove area as CONSOLE§r");
            return;
        }

        String name = args[1].toLowerCase();

        if (!(plugin.getConfig().contains("areas."+name))) {
            sender.sendMessage("§eNo area §d"+ name + "§e found. Config was not modified.§r");
            return;
        }

        if (confirmed) {
            plugin.getConfig().set("areas." + name, null);

            plugin.saveConfig();

            sender.sendMessage("§aSuccessfully deleted area §d" + args[1] + ".§r");
        } else {
            sender.sendMessage(civUtils.confirmRequestMessage);
            civUtils.confirmationList.add(civUtils.new PendingConfirmation(PendingAction.REMOVE_AREA, args ,player.getUniqueId()));
        }
    }

    public void removeArea(CommandSender sender, String[] args) {
        removeArea(sender, args, false);
    }

    public void setpos(CommandSender sender, String[] args) {
        int corner = 0;
        int x;
        int z;

        boolean usePlayerCoords = false;

        sender.sendMessage(String.valueOf(args.length));

        if (args.length != 5 && args.length != 3) {
            sender.sendMessage("§cCorrect syntax : /civ setpos <name> <pos1|pos2> <x> <z>§r");
            return;
        }

        if (args.length == 3) {
            usePlayerCoords = true;
        }


        switch (args[2]) {
            case "pos1" -> corner = 1;
            case "pos2" -> corner = 2;
            default -> {
                sender.sendMessage("§cSpecify corner : <pos1|pos2>. Correct syntax : /civ setpos <name> <pos1|pos2> <x> <z>§r");
                return;
            }
        }



        Player player = null;
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cConsole cannot execute this command");
            return;
        }
        player = p;

        if (usePlayerCoords) {
            x = player.getLocation().getBlockX();
            z = player.getLocation().getBlockZ();
        } else {
            try {
                x = Integer.parseInt(args[3]);
                z = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cCoordinates <x> and <z> have to be integers !§r");
                return;
            }
        }

        if (Math.abs(x) > 29999999 || Math.abs(z) > 29999999)  {
            sender.sendMessage("§cCoordinates cannot be higher than 30 Million blocks§r");
            return;
        }

        String name = args[1].toLowerCase();

        if (!plugin.getConfig().contains("areas." + name)) {
            sender.sendMessage("§cArea §d"+name+"§c does not exist. Create it using /civ newarea§r");
            return;
        }

        plugin.getConfig().set("areas."+name+'.'+'x'+corner, x);
        plugin.getConfig().set("areas."+name+'.'+'z'+corner, z);

        plugin.saveConfig();

        sender.sendMessage("§aCorner §e"+ corner +"§a of area §d"+ name + "§a set to coordinates §e " + x + ' ' + z + "§r");

    }

    public void areaDisplayName(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage("§cCorrect syntax : /civ areadisplayname <area> <newname>");
            return;
        }

        String area = args [1];
        String newname = args[2];

        plugin.getConfig().set("areas."+area+".display-name", newname);
        plugin.saveConfig();

        sender.sendMessage("§eArea §d"+area+"§e's display name has been renamed to §d"+newname);

    }
}
