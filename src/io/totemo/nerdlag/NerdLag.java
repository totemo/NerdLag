package io.totemo.nerdlag;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.TimedRegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

// ----------------------------------------------------------------------------
/**
 * Track down lag.
 */
public class NerdLag extends JavaPlugin implements MaxTimedRegisteredListener.EventDurationHandler {
    // ------------------------------------------------------------------------
    /**
     * Handle commands:
     *
     * <ul>
     * <li>/nerdlag event watch (<plugin> | all) [<thresh_nanos>]</li>
     * <li>/nerdlag event unwatch (<plugin> | all)</li>
     * <li>/nerdlag event subscribe</li>
     * <li>/nerdlag event unsubscribe</li>
     * </ul>
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (command.getName().equalsIgnoreCase("nerdlag")) {
            // /nerdlag event subcommands:
            if (args.length >= 1 && args[0].equals("event")) {
                // /nerdlag event watch <plugin> [<thresh_nanos>]
                if (args.length >= 3 && args.length <= 4 && args[1].equals("watch")) {
                    Set<Plugin> plugins = getPlugins(args[2]);
                    if (plugins.size() == 0) {
                        sender.sendMessage(ChatColor.RED + "No matching plugins.");
                        return true;
                    }

                    long reportThresholdMicros = 0;
                    boolean report = false;
                    if (args.length == 4) {
                        try {
                            reportThresholdMicros = Integer.parseInt(args[3]);
                            report = true;
                        } catch (NumberFormatException ex) {
                            sender.sendMessage(ChatColor.RED + args[3] + " is not an integer.");
                            return true;
                        }
                    }
                    watchPlugins(plugins, report, reportThresholdMicros * 1000);

                    StringBuilder success = new StringBuilder();
                    success.append(ChatColor.GOLD).append("Watching ").append(args[2]);
                    if (report) {
                        success.append(" with threshold ").append(reportThresholdMicros).append(" \u00b5s");
                    }
                    sender.sendMessage(success.toString());
                    return true;

                } else if (args.length == 3 && args[1].equals("unwatch")) {
                    Set<Plugin> plugins = getPlugins(args[2]);
                    if (plugins.size() == 0) {
                        sender.sendMessage(ChatColor.RED + "No matching plugins.");
                        return true;
                    }
                    unwatchPlugins(plugins);
                    StringBuilder success = new StringBuilder();
                    success.append(ChatColor.GOLD).append("No longer watching ").append(args[2]);
                    sender.sendMessage(success.toString());
                    return true;

                } else if (args.length == 2 && args[1].equals("subscribe")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        _eventSubscribers.add(player.getUniqueId());
                        sender.sendMessage(ChatColor.GOLD + "You will receive event duration notifications.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "You have to be in-game to receive event duration notifications.");
                    }
                    return true;

                } else if (args.length == 2 && args[1].equals("unsubscribe")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        _eventSubscribers.remove(player.getUniqueId());
                        sender.sendMessage(ChatColor.GOLD + "You will no longer receive event duration notifications.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "You have to be in-game to unsubscribe from duration notifications.");
                    }
                    return true;
                }
            } // /nerdlag event subcommands
        }
        return false;
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * @see io.totemo.nerdlag.MaxTimedRegisteredListener.EventDurationHandler#reportDuration(io.totemo.nerdlag.MaxTimedRegisteredListener,
     *      long)
     *
     *      Log the current event and the worst case execution in microseconds.
     */
    @Override
    public void reportDuration(MaxTimedRegisteredListener reg, long durationNanos) {
        StringBuilder builder = new StringBuilder();
        builder.append(reg.getPlugin().getName()).append(" - ");
        builder.append(reg.getEventClass().getSimpleName()).append(" ");
        builder.append(nanosToMicros(durationNanos)).append(" \u00b5s (max ");
        builder.append(nanosToMicros(reg.getMaxDurationNanos())).append(" \u00b5s)");
        String message = builder.toString();
        getLogger().info(message);

        for (UUID uuid : _eventSubscribers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + message);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Convert nanoseconds to microseconds, rounded up to the nearest integer.
     *
     * @param nanos nanoseconds duration.
     * @return integer microseconds.
     */
    protected static long nanosToMicros(long nanos) {
        return (nanos + 500) / 1000;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a set of Plugins according to the specifier.
     *
     * @param specifier either a plugin name, or the word "all" for all plugins.
     * @return the specified plugins, or an empty array if none matched.
     */
    protected Set<Plugin> getPlugins(String specifier) {
        Set<Plugin> plugins = new HashSet<Plugin>();
        if (specifier.equals("all")) {
            Collections.addAll(plugins, Bukkit.getPluginManager().getPlugins());
        } else {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(specifier);
            if (plugin != null) {
                plugins.add(plugin);
            }
        }
        return plugins;
    }

    // ------------------------------------------------------------------------
    /**
     * Configure event handling for the specified plugins to monitor event
     * handler durations, including the maximum duration of any callback.
     *
     * @param plugins the set of plugins to instrument.
     * @param report if true, plugin event handler durations that exceed the
     *        threshold will be reported.
     * @param reportThresholdNanos the threshold duration in nanoseconds, above
     *        which an event handler execution is reportable.
     */
    protected void watchPlugins(Set<Plugin> plugins, boolean report, long reportThresholdNanos) {
        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            RegisteredListener[] listeners = handlerList.getRegisteredListeners();
            for (RegisteredListener oldListener : listeners) {
                if (plugins.contains(oldListener.getPlugin())) {
                    MaxTimedRegisteredListener newListener;
                    try {
                        newListener = new MaxTimedRegisteredListener(oldListener, report, reportThresholdNanos, this);
                        handlerList.unregister(oldListener);
                        handlerList.register(newListener);
                    } catch (Exception ex) {
                        getLogger().warning("watchPlugins(): " + ex.getClass().getName());
                    }
                }
            }
            handlerList.bake();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Remove monitoring of event handler durations for the specified plugins.
     *
     * @param plugins the set of plugins.
     */
    protected void unwatchPlugins(Set<Plugin> plugins) {
        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            RegisteredListener[] listeners = handlerList.getRegisteredListeners();
            for (RegisteredListener oldRL : listeners) {
                if (plugins.contains(oldRL.getPlugin()) && oldRL instanceof MaxTimedRegisteredListener) {
                    MaxTimedRegisteredListener oldMTRL = (MaxTimedRegisteredListener) oldRL;
                    RegisteredListener newRL;
                    if (Bukkit.getPluginManager().useTimings()) {
                        newRL = new TimedRegisteredListener(
                            oldMTRL.getListener(), oldMTRL.getEventExecutor(), oldMTRL.getPriority(),
                            oldMTRL.getPlugin(), oldMTRL.isIgnoringCancelled());
                    } else {
                        newRL = new RegisteredListener(
                            oldMTRL.getListener(), oldMTRL.getEventExecutor(), oldMTRL.getPriority(),
                            oldMTRL.getPlugin(), oldMTRL.isIgnoringCancelled());
                    }
                    handlerList.unregister(oldMTRL);
                    handlerList.register(newRL);
                }
            }
            handlerList.bake();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Set of UUIDs of all players who have subscribed to listen to event
     * duration reports.
     *
     * We look up Players by UUID and thereby don't have to worry about handling
     * logouts or player deaths invalidating the Player instance.
     */
    protected HashSet<UUID> _eventSubscribers = new HashSet<UUID>();
} // class NerdLag