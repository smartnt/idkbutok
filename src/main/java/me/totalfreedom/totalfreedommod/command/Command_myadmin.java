package me.totalfreedom.totalfreedommod.command;

import java.util.Arrays;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.pravian.aero.util.Ips;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH)
@CommandParameters(description = "Manage my admin entry", usage = "/<command> [-o <admin>] <clearips | clearip <ip> | setlogin <message> | clearlogin | setacformat <format> | clearacformat> | oldtags | logstick | syncroles>")
public class Command_myadmin extends FreedomCommand
{

    @Override
    protected boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        checkPlayer();
        checkRank(Rank.SUPER_ADMIN);

        if (args.length < 1)
        {
            return false;
        }

        Player init = null;
        Admin target = getAdmin(playerSender);
        Player targetPlayer = playerSender;

        // -o switch
        if (args[0].equals("-o"))
        {
            checkRank(Rank.SENIOR_ADMIN);
            init = playerSender;
            targetPlayer = getPlayer(args[1]);
            if (targetPlayer == null)
            {
                msg(FreedomCommand.PLAYER_NOT_FOUND);
                return true;
            }

            target = getAdmin(targetPlayer);
            if (target == null)
            {
                msg("That player is not admin", ChatColor.RED);
                return true;
            }

            // Shift 2
            args = Arrays.copyOfRange(args, 2, args.length);
            if (args.length < 1)
            {
                return false;
            }
        }

        final String targetIp = Ips.getIp(targetPlayer);

        switch (args[0])
        {
            case "clearips":
            {
                if (args.length != 1)
                {
                    return false; // Double check: the player might mean "clearip"
                }

                if (init == null)
                {
                    FUtil.adminAction(sender.getName(), "Clearing my supered IPs", true);
                }
                else
                {
                    FUtil.adminAction(sender.getName(), "Clearing " + target.getName() + "' supered IPs", true);
                }

                int counter = target.getIps().size() - 1;
                target.clearIPs();
                target.addIp(targetIp);

                plugin.al.save();

                msg(counter + " IPs removed.");
                msg(targetPlayer, target.getIps().get(0) + " is now your only IP address");
                return true;
            }

            case "clearip":
            {
                if (args.length != 2)
                {
                    return false; // Double check: the player might mean "clearips"
                }

                if (!target.getIps().contains(args[1]))
                {
                    if (init == null)
                    {
                        msg("That IP is not registered to you.");
                    }
                    else
                    {
                        msg("That IP does not belong to that player.");
                    }
                    return true;
                }

                if (targetIp.equals(args[1]))
                {
                    if (init == null)
                    {
                        msg("You cannot remove your current IP.");
                    }
                    else
                    {
                        msg("You cannot remove that admin's current IP.");
                    }
                    return true;
                }

                FUtil.adminAction(sender.getName(), "Removing a supered IP" + (init == null ? "" : " from " + targetPlayer.getName() + "'s IPs"), true);

                target.removeIp(args[1]);
                plugin.al.save();
                plugin.al.updateTables();

                msg("Removed IP " + args[1]);
                msg("Current IPs: " + StringUtils.join(target.getIps(), ", "));
                return true;
            }

            case "setlogin":
            {
                if (args.length < 2)
                {
                    return false;
                }

                String msg = StringUtils.join(args, " ", 1, args.length);
                FUtil.adminAction(sender.getName(), "Setting personal login message" + (init == null ? "" : " for " + targetPlayer.getName()), false);
                target.setLoginMessage(msg);
                msg((init == null ? "Your" : targetPlayer.getName() + "'s") + " login message is now: ");
                msg("> " + ChatColor.AQUA + (msg.contains("%name%") ? "" : target.getName() + " is ") + FUtil.colorize(msg).replace("%name%", targetPlayer.getName()));
                plugin.al.save();
                plugin.al.updateTables();
                return true;
            }

            case "clearlogin":
            {
                FUtil.adminAction(sender.getName(), "Clearing personal login message" + (init == null ? "" : " for " + targetPlayer.getName()), false);
                target.setLoginMessage(null);
                plugin.al.save();
                plugin.al.updateTables();
                return true;
            }

            case "settag":
            {
                msg("Please use /tag set to set your tag.", ChatColor.RED);
                return true;
            }

            case "cleartag":
            {
                msg("Please use /tag off to remove your tag.", ChatColor.RED);
                return true;
            }
            case "setacformat":
            {
                String format = StringUtils.join(args, " ", 1, args.length);
                target.setAcFormat(format);
                plugin.al.save();
                plugin.al.updateTables();
                msg("Set admin chat format to \"" + format + "\".", ChatColor.GRAY);
                String example = format.replace("%name%", "ExampleAdmin").replace("%rank%", Rank.TELNET_ADMIN.getAbbr()).replace("%rankcolor%", Rank.TELNET_ADMIN.getColor().toString()).replace("%msg%", "The quick brown fox jumps over the lazy dog.");
                msg(ChatColor.GRAY + "Example: " + FUtil.colorize(example));
                return true;
            }
            case "clearacformat":
            {
                target.setAcFormat(null);
                plugin.al.save();
                plugin.al.updateTables();
                msg("Cleared admin chat format.", ChatColor.GRAY);
                return true;
            }
            case "oldtags":
            {
                target.setOldTags(!target.getOldTags());
                plugin.al.save();
                plugin.al.updateTables();
                msg((target.getOldTags() ? "Enabled" : "Disabled") + " old tags.");
                return true;
            }
            case "logstick":
            {
                target.setLogStick(!target.getLogStick());
                plugin.al.save();
                plugin.al.updateTables();
                msg((target.getLogStick() ? "Enabled" : "Disabled") + " log-stick lookup.");
                return true;
            }

            case "syncroles":
            {
                if (plugin.dc.enabled)
                {
                    if (!ConfigEntry.DISCORD_ROLE_SYNC.getBoolean())
                    {
                        msg("Role syncing is not enabled.", ChatColor.RED);
                        return true;
                    }
                    boolean synced = plugin.dc.syncRoles(target);
                    if (target.getDiscordID() == null)
                    {
                        msg("Please run /linkdiscord first!", ChatColor.RED);
                        return true;
                    }
                    if (synced)
                    {
                        msg("Successfully synced your roles.", ChatColor.GREEN);
                    }
                    else
                    {
                        msg("Failed to sync your roles, please check the console.", ChatColor.RED);
                    }
                }

                return true;
            }

            default:
            {
                return false;
            }
        }
    }
}
