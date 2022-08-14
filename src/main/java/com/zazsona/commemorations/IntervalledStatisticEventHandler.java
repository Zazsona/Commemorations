package com.zazsona.commemorations;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class IntervalledStatisticEventHandler implements Listener
{
    // This list is built from: https://www.spigotmc.org/threads/which-statistics-dont-trigger-playerstatisticincrementevent.569349/#post-4465106
    // TODO: Check for updates when updating Spigot
    public static final Statistic[] INTERVALLED_STATISTICS =
            {
                    Statistic.FALL_ONE_CM,
                    Statistic.BOAT_ONE_CM,
                    Statistic.CLIMB_ONE_CM,
                    Statistic.WALK_ON_WATER_ONE_CM,
                    Statistic.WALK_UNDER_WATER_ONE_CM,
                    Statistic.FLY_ONE_CM,
                    Statistic.HORSE_ONE_CM,
                    Statistic.MINECART_ONE_CM,
                    Statistic.PIG_ONE_CM,
                    Statistic.PLAY_ONE_MINUTE,
                    Statistic.SWIM_ONE_CM,
                    Statistic.WALK_ONE_CM,
                    Statistic.SPRINT_ONE_CM,
                    Statistic.CROUCH_ONE_CM,
                    Statistic.TIME_SINCE_DEATH,
                    Statistic.SNEAK_TIME,
                    Statistic.TOTAL_WORLD_TIME,
                    Statistic.TIME_SINCE_REST,
                    Statistic.AVIATE_ONE_CM,
                    Statistic.STRIDER_ONE_CM
            };

    private HashMap<UUID, HashMap<Statistic, Integer>> playerStatValueMap;
    private ArrayList<IIntervalledStatisticListener> listeners;
    private int intervalTicks;
    private Object statMapLock;
    private Object listenersLock;

    public IntervalledStatisticEventHandler(Plugin plugin, int intervalTicks)
    {
        playerStatValueMap = new HashMap<>();
        listeners = new ArrayList<>();
        this.intervalTicks = intervalTicks;

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> checkStatChanges(), intervalTicks, intervalTicks);
    }

    public void addListener(IIntervalledStatisticListener listener)
    {
        synchronized (listenersLock)
        {
            listeners.add(listener);
        }
    }

    public void removeListener(IIntervalledStatisticListener listener)
    {
        synchronized (listenersLock)
        {
            listeners.remove(listener);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        Player player = e.getPlayer();
        HashMap<Statistic, Integer> playerStats = new HashMap<>();
        for (Statistic statistic : INTERVALLED_STATISTICS)
           playerStats.put(statistic, player.getStatistic(statistic));
        synchronized (statMapLock)
        {
            playerStatValueMap.put(player.getUniqueId(), playerStats);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e)
    {
        synchronized (statMapLock)
        {
            playerStatValueMap.remove(e.getPlayer().getUniqueId());
        }
    }

    private void checkStatChanges()
    {
        synchronized (statMapLock)
        {
            for (Map.Entry<UUID, HashMap<Statistic, Integer>> entry : playerStatValueMap.entrySet())
            {
                Player player = Bukkit.getPlayer(entry.getKey());
                HashMap<Statistic, Integer> playerStats = entry.getValue();
                if (player == null || playerStats == null)
                    continue;

                for (Statistic statistic : INTERVALLED_STATISTICS)
                {
                    int previousValue = playerStats.get(statistic);
                    int newValue = player.getStatistic(statistic);
                    if (newValue <= previousValue)
                        continue;

                    PlayerStatisticIncrementEvent event = new PlayerStatisticIncrementEvent(player, statistic, previousValue, newValue);
                    synchronized (listenersLock)
                    {
                        for (IIntervalledStatisticListener listener : listeners)
                            listener.onIntervalledStatisticIncrement(event);
                    }
                }
            }
        }
    }
}
