
package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import com.ninja.ghastutils.multiplier.MultiplierManager;
import com.ninja.ghastutils.utils.MessageUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

public class EventMultiplierCommand extends SubCommand {
    private final GhastUtils plugin;

    public EventMultiplierCommand(GhastUtils plugin) {
        super("event", "ghastutils.multiplier.event.admin", false);
        this.plugin = plugin;
        this.addAlias("events");
    }

    public String getDescription() {
        return "Manage event multipliers";
    }

    public String getUsage() {
        return "/gutil event <create/start/stop/list> [id] [name] [value] [duration]";
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!this.checkPermission(sender)) {
            Map<String, String> placeholders = MessageUtils.placeholders();
            placeholders.put("permission", this.getPermission());
            MessageUtils.sendMessage(sender, "permission.denied", placeholders);
            return true;
        } else if (args.length == 0) {
            sender.sendMessage("§cUsage: " + this.getUsage());
            return true;
        } else {
            MultiplierManager multiplierManager = this.plugin.getMultiplierManager();
            switch (args[0].toLowerCase()) {
                case "create":
                    if (args.length < 4) {
                        sender.sendMessage("§cUsage: /gutil event create <id> <name> <value> [duration]");
                        return true;
                    }

                    String eventId = args[1];
                    String eventName = args[2].replace("_", " ");
                    long duration = 0L;

                    double value;
                    try {
                        value = Double.parseDouble(args[3]);
                    } catch (NumberFormatException var27) {
                        sender.sendMessage("§cInvalid value: " + args[3]);
                        return true;
                    }

                    if (value <= (double)0.0F) {
                        sender.sendMessage("§cValue must be greater than 0");
                        return true;
                    }

                    if (args.length > 4) {
                        try {
                            duration = this.parseDuration(args[4]);
                        } catch (NumberFormatException var26) {
                            sender.sendMessage("§cInvalid duration: " + args[4]);
                            return true;
                        }
                    }

                    if (multiplierManager.createEventMultiplier(eventId, eventName, value, duration)) {
                        sender.sendMessage("§aEvent multiplier '" + eventId + "' created successfully");
                        if (duration > 0L) {
                            String var32 = this.formatTime(duration);
                            sender.sendMessage("§aEvent will last for " + var32 + ".");
                        } else {
                            sender.sendMessage("§aEvent is permanent and will not expire.");
                        }
                    } else {
                        sender.sendMessage("§cFailed to create event multiplier");
                    }
                    break;
                case "start":
                    if (args.length < 2) {
                        sender.sendMessage("§cUsage: /gutil event start <id>");
                        return true;
                    }

                    String startId = args[1];
                    if (multiplierManager.startEvent(startId, sender)) {
                    }
                    break;
                case "stop":
                    if (args.length < 2) {
                        sender.sendMessage("§cUsage: /gutil event stop <id>");
                        return true;
                    }

                    String stopId = args[1];
                    if (multiplierManager.stopEvent(stopId, sender)) {
                    }
                    break;
                case "remove":
                    if (args.length < 2) {
                        sender.sendMessage("§cUsage: /gutil event remove <id>");
                        return true;
                    }

                    String removeId = args[1];
                    if (multiplierManager.removeEventMultiplier(removeId)) {
                        sender.sendMessage("§aEvent multiplier '" + removeId + "' removed successfully");
                    } else {
                        sender.sendMessage("§cEvent multiplier '" + removeId + "' not found");
                    }
                    break;
                case "list":
                    Map<String, MultiplierManager.EventMultiplier> events = multiplierManager.getEventMultipliers();
                    if (events.isEmpty()) {
                        sender.sendMessage("§cNo active event multipliers");
                        return true;
                    }

                    sender.sendMessage("§6§l=== Active Event Multipliers ===");

                    for(MultiplierManager.EventMultiplier event : events.values()) {
                        String timeInfo = event.getEndTime() > 0L ? "§7(Ends in: " + event.getTimeRemaining() + ")" : "§7(Permanent)";
                        String var10001 = event.getId();
                        sender.sendMessage("§e" + var10001 + " §7- §f" + event.getName() + " §7- §f+" + String.format("%.2f", event.getValue()) + "x " + timeInfo);
                    }

                    ConfigurationSection eventTemplates = this.plugin.getConfig().getConfigurationSection("multiplier.event-templates");
                    if (eventTemplates != null && !eventTemplates.getKeys(false).isEmpty()) {
                        sender.sendMessage("§6§l=== Available Event Templates ===");

                        for(String id : eventTemplates.getKeys(false)) {
                            ConfigurationSection template = eventTemplates.getConfigurationSection(id);
                            if (template != null) {
                                String name = template.getString("name", id);
                                double templateValue = template.getDouble("value", (double)0.5F);
                                long templateDuration = template.getLong("duration", 3600L);
                                sender.sendMessage("§e" + id + " §7- §f" + name + " §7- §f+" + String.format("%.2f", templateValue) + "x §7(Duration: " + multiplierManager.formatTime(templateDuration) + ")");
                            }
                        }
                    }
                    break;
                default:
                    sender.sendMessage("§cUnknown action: " + action);
                    sender.sendMessage("§cUsage: " + this.getUsage());
            }

            return true;
        }
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!this.checkPermission(sender)) {
            return new ArrayList();
        } else if (args.length == 1) {
            return this.filterStartsWith(Arrays.asList("create", "start", "stop", "remove", "list"), args[0]);
        } else {
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("stop")) {
                    return this.filterStartsWith(new ArrayList(this.plugin.getMultiplierManager().getEventMultipliers().keySet()), args[1]);
                }

                if (args[0].equalsIgnoreCase("start")) {
                    ConfigurationSection templates = this.plugin.getConfig().getConfigurationSection("multiplier.event-templates");
                    if (templates != null) {
                        return this.filterStartsWith(new ArrayList(templates.getKeys(false)), args[1]);
                    }
                }
            } else {
                if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
                    return this.filterStartsWith(Arrays.asList("0.1", "0.25", "0.5", "1.0", "2.0"), args[3]);
                }

                if (args.length == 5 && args[0].equalsIgnoreCase("create")) {
                    return this.filterStartsWith(Arrays.asList("1h", "6h", "12h", "1d", "3d", "7d"), args[4]);
                }
            }

            return new ArrayList();
        }
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        return (List)list.stream().filter((s) -> s.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }

    private long parseDuration(String duration) {
        if (duration.matches("\\d+")) {
            return Long.parseLong(duration);
        } else {
            String value = duration.substring(0, duration.length() - 1);
            char unit = Character.toLowerCase(duration.charAt(duration.length() - 1));
            long seconds = Long.parseLong(value);
            switch (unit) {
                case 'd' -> {
                    return seconds * 60L * 60L * 24L;
                }
                case 'h' -> {
                    return seconds * 60L * 60L;
                }
                case 'm' -> {
                    return seconds * 60L;
                }
                case 's' -> {
                    return seconds;
                }
                default -> throw new NumberFormatException("Invalid duration unit: " + unit);
            }
        }
    }

    private String formatTime(long seconds) {
        if (seconds < 60L) {
            return seconds + " seconds";
        } else if (seconds < 3600L) {
            return seconds / 60L + " minutes";
        } else {
            return seconds < 86400L ? seconds / 3600L + " hours" : seconds / 86400L + " days";
        }
    }
}
