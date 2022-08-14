package com.zazsona.commemorations.config;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KillAPlayerCommemorationConfig extends CommemorationConfig
{
    private static final String KILLER_PLAYER_REGISTRATION = "[Killer]";
    private static final String VICTIM_PLAYER_REGISTRATION = "[Victim]";

    public KillAPlayerCommemorationConfig(NamespacedKey triggerResourceKey, String templateId, List<String> featurePlayerRegistrations)
    {
        super(triggerResourceKey, templateId, featurePlayerRegistrations);
    }

    public List<UUID> resolveFeaturedPlayers(UUID killer, UUID victim)
    {
        ArrayList<UUID> orderedIds = new ArrayList<>();
        for (String featurePlayerRegistration : getFeaturePlayerRegistrations())
        {
            switch (featurePlayerRegistration)
            {
                case KILLER_PLAYER_REGISTRATION:
                    orderedIds.add(killer);
                    break;
                case VICTIM_PLAYER_REGISTRATION:
                    orderedIds.add(victim);
                    break;
                default:
                    orderedIds.add(UUID.fromString(featurePlayerRegistration));
            }
        }
        return orderedIds;
    }
}
