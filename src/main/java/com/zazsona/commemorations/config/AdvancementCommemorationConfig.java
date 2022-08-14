package com.zazsona.commemorations.config;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdvancementCommemorationConfig extends CommemorationConfig
{
    private static final String ADVANCER_PLAYER_REGISTRATION = "[Advancer]";

    public AdvancementCommemorationConfig(NamespacedKey triggerResourceKey, String templateId, List<String> featurePlayerRegistrations)
    {
        super(triggerResourceKey, templateId, featurePlayerRegistrations);
    }

    public Advancement getAdvancement()
    {
        return Bukkit.getAdvancement(getTriggerResourceKey());
    }

    public List<UUID> resolveFeaturedPlayers(UUID advancer)
    {
        ArrayList<UUID> orderedIds = new ArrayList<>();
        for (String featurePlayerRegistration : getFeaturePlayerRegistrations())
        {
            if (featurePlayerRegistration.equals(ADVANCER_PLAYER_REGISTRATION))
                orderedIds.add(advancer);
            else
                orderedIds.add(UUID.fromString(featurePlayerRegistration));
        }
        return orderedIds;
    }
}
