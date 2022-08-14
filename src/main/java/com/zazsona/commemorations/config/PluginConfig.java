package com.zazsona.commemorations.config;

import com.zazsona.commemorations.CommemorationsPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginConfig
{
    public static final String CFG_PLUGIN_ENABLED_KEY = "plugin-enabled";
    public static final String CFG_BUILDING_ENABLED_KEY = "build-signs";
    public static final String CFG_ADVANCEMENTS_KEY = "advancements";
    public static final String CFG_STATISTICS_KEY = "statistics";
    public static final String CFG_SPECIALS_KEY = "specials";

    public static final String CFG_ADVANCEMENTS_ID_KEY = "advancement";
    public static final String CFG_ADVANCEMENTS_TEMPLATE_KEY = "template";
    public static final String CFG_ADVANCEMENTS_FEATURE_PLAYERS_KEY = "featurePlayers";

    public static final String CFG_STATISTICS_ID_KEY = "statistic";
    public static final String CFG_STATISTICS_VALUE_KEY = "value";
    public static final String CFG_STATISTICS_TEMPLATE_KEY = "template";
    public static final String CFG_STATISTICS_FEATURE_PLAYERS_KEY = "featurePlayers";

    public static final String CFG_SPECIALS_ID_KEY = "type";
    public static final String CFG_SPECIALS_TEMPLATE_KEY = "template";
    public static final String CFG_SPECIALS_FEATURE_PLAYERS_KEY = "featurePlayers";

    public static final String SPECIALS_TYPES_KILL_A_PLAYER_KEY = "commemorations:kill_a_player";

    private static PluginConfig instance;

    private Plugin plugin;
    private boolean isPluginEnabled;
    private boolean isBuildingEnabled;
    private HashMap<NamespacedKey, AdvancementCommemorationConfig> advancementCommemorations;
    private HashMap<NamespacedKey, StatisticCommemorationConfig> statisticCommemorations;
    private HashMap<NamespacedKey, CommemorationConfig> specialCommemorations;

    public static PluginConfig getInstance()
    {
        if (instance == null)
            instance = new PluginConfig(CommemorationsPlugin.getInstance());
        return instance;
    }

    public PluginConfig(Plugin plugin)
    {
        plugin.getConfig().options().parseComments(true);
        plugin.saveDefaultConfig();

        FileConfiguration yamlConfig = plugin.getConfig();

        this.plugin = plugin;
        this.isPluginEnabled = yamlConfig.getBoolean(CFG_PLUGIN_ENABLED_KEY);
        this.isBuildingEnabled = yamlConfig.getBoolean(CFG_BUILDING_ENABLED_KEY);


        List<Map<?, ?>> advancementYamlConfigs = yamlConfig.getMapList(CFG_ADVANCEMENTS_KEY);
        for (Map<?, ?> advancementYamlConfig : advancementYamlConfigs)
        {
            String id = (String) advancementYamlConfig.get(CFG_ADVANCEMENTS_ID_KEY);
            NamespacedKey key = NamespacedKey.fromString(id);
            String templateId = (String) advancementYamlConfig.get(CFG_ADVANCEMENTS_TEMPLATE_KEY);
            List<String> playerRegistrations = (List<String>) advancementYamlConfig.get(CFG_ADVANCEMENTS_FEATURE_PLAYERS_KEY);
            AdvancementCommemorationConfig advancementConfig = new AdvancementCommemorationConfig(key, templateId, playerRegistrations);
            advancementCommemorations.put(key, advancementConfig);
        }

        List<Map<?, ?>> statisticYamlConfigs = yamlConfig.getMapList(CFG_STATISTICS_KEY);
        for (Map<?, ?> statisticYamlConfig : statisticYamlConfigs)
        {
            String id = (String) statisticYamlConfig.get(CFG_STATISTICS_ID_KEY);
            NamespacedKey key = NamespacedKey.fromString(id);
            int value = (int) statisticYamlConfig.get(CFG_STATISTICS_VALUE_KEY);
            String templateId = (String) statisticYamlConfig.get(CFG_STATISTICS_TEMPLATE_KEY);
            List<String> playerRegistrations = (List<String>) statisticYamlConfig.get(CFG_STATISTICS_FEATURE_PLAYERS_KEY);
            StatisticCommemorationConfig statisticConfig = new StatisticCommemorationConfig(key, templateId, playerRegistrations, value);
            statisticCommemorations.put(key, statisticConfig);
        }

        List<Map<?, ?>> specialYamlConfigs = yamlConfig.getMapList(CFG_SPECIALS_KEY);
        for (Map<?, ?> specialYamlConfig : specialYamlConfigs)
        {
            String id = (String) specialYamlConfig.get(CFG_SPECIALS_ID_KEY);
            NamespacedKey key = NamespacedKey.fromString(id);
            String templateId = (String) specialYamlConfig.get(CFG_SPECIALS_TEMPLATE_KEY);
            List<String> playerRegistrations = (List<String>) specialYamlConfig.get(CFG_SPECIALS_FEATURE_PLAYERS_KEY);

            CommemorationConfig specialConfig = null;
            switch (key.toString())
            {
                case SPECIALS_TYPES_KILL_A_PLAYER_KEY:
                    specialConfig = new KillAPlayerCommemorationConfig(key, templateId, playerRegistrations);
                    break;
            }
            specialCommemorations.put(key, specialConfig);
        }
    }

    public boolean isPluginEnabled()
    {
        return isPluginEnabled;
    }

    public void setPluginEnabled(boolean pluginEnabled)
    {
        isPluginEnabled = pluginEnabled;
        plugin.getConfig().set(CFG_PLUGIN_ENABLED_KEY, pluginEnabled);
        plugin.saveConfig();
    }

    public boolean isBuildingEnabled()
    {
        return isBuildingEnabled;
    }

    public void setBuildingEnabled(boolean buildingEnabled)
    {
        isBuildingEnabled = buildingEnabled;
        plugin.getConfig().set(CFG_BUILDING_ENABLED_KEY, buildingEnabled);
        plugin.saveConfig();
    }

    public CommemorationConfig getCommemoration(NamespacedKey key) throws IllegalArgumentException
    {
        if (advancementCommemorations.containsKey(key))
            return advancementCommemorations.get(key);
        else if (statisticCommemorations.containsKey(key))
            return statisticCommemorations.get(key);
        else if (specialCommemorations.containsKey(key))
            return specialCommemorations.get(key);
        else
            throw new IllegalArgumentException("Key does not map to any commemoration.");
    }
}
