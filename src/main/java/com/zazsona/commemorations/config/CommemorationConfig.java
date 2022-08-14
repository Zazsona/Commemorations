package com.zazsona.commemorations.config;

import org.bukkit.NamespacedKey;

import java.util.List;

public class CommemorationConfig
{
    private NamespacedKey triggerResourceKey;
    private String templateId;
    private List<String> featurePlayerRegistrations;

    public CommemorationConfig(NamespacedKey triggerResourceKey, String templateId, List<String> featurePlayerRegistrations)
    {
        this.triggerResourceKey = triggerResourceKey;
        this.templateId = templateId;
        this.featurePlayerRegistrations = featurePlayerRegistrations;
    }

    public NamespacedKey getTriggerResourceKey()
    {
        return triggerResourceKey;
    }

    public String getTemplateId()
    {
        return templateId;
    }

    public List<String> getFeaturePlayerRegistrations()
    {
        return featurePlayerRegistrations;
    }
}
