package com.zazsona.commemorations;

import org.bukkit.event.player.PlayerStatisticIncrementEvent;

public interface IIntervalledStatisticListener
{
    void onIntervalledStatisticIncrement(PlayerStatisticIncrementEvent e);
}
