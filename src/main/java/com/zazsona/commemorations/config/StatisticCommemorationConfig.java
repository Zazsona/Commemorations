package com.zazsona.commemorations.config;

import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class StatisticCommemorationConfig extends CommemorationConfig
{
    private static final String ADVANCER_PLAYER_REGISTRATION = "/Advancer/";

    private int statisticValue;

    public StatisticCommemorationConfig(NamespacedKey triggerResourceKey, String templateId, List<String> featurePlayerRegistrations, int statisticValue)
    {
        super(triggerResourceKey, templateId, featurePlayerRegistrations);
        this.statisticValue = statisticValue;
    }

    public Statistic getStatistic()
    {
        NamespacedKey key = getTriggerResourceKey();
        Statistic[] statistics = Statistic.values();
        for (Statistic statistic : statistics)
        {
            if (statistic.getKey().equals(key))
                return statistic;
        }
        return null;
    }

    public int getStatisticValue()
    {
        return statisticValue;
    }

    public List<UUID> resolveFeaturedPlayers(UUID advancer)
    {
        ArrayList<UUID> orderedIds = new ArrayList<>();
        List<String> playerRegistrations = getFeaturePlayerRegistrations();
        playerRegistrations.forEach(reg ->
                                    {
                                        if (reg.equals(ADVANCER_PLAYER_REGISTRATION))
                                            orderedIds.add(advancer);
                                        else
                                            orderedIds.add(UUID.fromString(reg));
                                    });
        return orderedIds;
    }
}
