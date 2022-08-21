package com.zazsona.commemorations;

import com.zazsona.commemorations.blockbuild.*;
import com.zazsona.commemorations.config.AdvancementCommemorationConfig;
import com.zazsona.commemorations.config.KillAPlayerCommemorationConfig;
import com.zazsona.commemorations.config.PluginConfig;
import com.zazsona.commemorations.config.StatisticCommemorationConfig;
import com.zazsona.commemorations.database.RenderedGraphic;
import com.zazsona.commemorations.exception.UnableToBuildSchematicException;
import com.zazsona.commemorations.repository.CommemorationMapRepository;
import com.zazsona.commemorations.repository.RenderRepository;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.map.MapView;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class CommemorationTriggerListener implements Listener, IIntervalledStatisticListener
{
    private RenderRepository renderRepository;
    private CommemorationMapRepository mapRepository;
    private IBlockSchematic schematic;
    private BlockSchematicBuilder builder;
    private SchematicLocationSearcher searcher;

    public CommemorationTriggerListener(RenderRepository renderRepository, CommemorationMapRepository mapRepository, IBlockSchematic schematic, BlockSchematicBuilder builder, SchematicLocationSearcher searcher)
    {
        this.renderRepository = renderRepository;
        this.mapRepository = mapRepository;
        this.schematic = schematic;
        this.builder = builder;
        this.searcher = searcher;
    }

    // ===========================
    // Advancement Commemorations
    // ===========================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAdvancementDone(PlayerAdvancementDoneEvent e)
    {
        Player player = e.getPlayer();
        Advancement advancement = e.getAdvancement();
        NamespacedKey key = advancement.getKey();
        if (!PluginConfig.getInstance().isCommemoration(key))
            return;

        AdvancementCommemorationConfig commemorationConfig = (AdvancementCommemorationConfig) PluginConfig.getInstance().getCommemoration(key);
        String templateId = commemorationConfig.getTemplateId();
        ArrayList<UUID> featuredPlayers = (ArrayList<UUID>) commemorationConfig.resolveFeaturedPlayers(player.getUniqueId());
        Bukkit.getScheduler().runTask(CommemorationsPlugin.getInstance(), () ->
        {
            try
            {
                createCommemorationSign(player.getLocation(), templateId, featuredPlayers);
            }
            catch (IOException | SQLException ex)
            {
                CommemorationsPlugin.getInstance().getLogger().severe("Unable to create sign: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    // ===========================
    // Statistic Commemorations
    // ===========================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStatisticIncremented(PlayerStatisticIncrementEvent e)
    {
        Player player = e.getPlayer();
        Statistic statistic = e.getStatistic();
        NamespacedKey key = statistic.getKey();
        if (!PluginConfig.getInstance().isCommemoration(key))
            return;

        StatisticCommemorationConfig commemorationConfig = (StatisticCommemorationConfig) PluginConfig.getInstance().getCommemoration(key);
        if (e.getPreviousValue() < commemorationConfig.getStatisticValue() && e.getNewValue() >= commemorationConfig.getStatisticValue())
        {
            String templateId = commemorationConfig.getTemplateId();
            ArrayList<UUID> featuredPlayers = (ArrayList<UUID>) commemorationConfig.resolveFeaturedPlayers(player.getUniqueId());
            Bukkit.getScheduler().runTask(CommemorationsPlugin.getInstance(), () ->
            {
                try
                {
                    createCommemorationSign(player.getLocation(), templateId, featuredPlayers);
                }
                catch (IOException | SQLException ex)
                {
                    CommemorationsPlugin.getInstance().getLogger().severe("Unable to create sign: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        }
    }


    @Override
    public void onIntervalledStatisticIncrement(PlayerStatisticIncrementEvent e)
    {
        onStatisticIncremented(e);
    }

    // ===========================
    // Special Commemorations
    // ===========================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerKillPlayer(PlayerDeathEvent e)
    {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        NamespacedKey key = NamespacedKey.fromString(PluginConfig.SPECIALS_TYPES_KILL_A_PLAYER_KEY);
        if (!PluginConfig.getInstance().isCommemoration(key))
            return;

        if (killer != null)
        {
            KillAPlayerCommemorationConfig commemorationConfig = (KillAPlayerCommemorationConfig) PluginConfig.getInstance().getCommemoration(key);
            String templateId = commemorationConfig.getTemplateId();
            ArrayList<UUID> featuredPlayers = (ArrayList<UUID>) commemorationConfig.resolveFeaturedPlayers(killer.getUniqueId(), victim.getUniqueId());
            Bukkit.getScheduler().runTask(CommemorationsPlugin.getInstance(), () ->
            {
                try
                {
                    createCommemorationSign(victim.getLocation(), templateId, featuredPlayers);
                }
                catch (IOException | SQLException ex)
                {
                    CommemorationsPlugin.getInstance().getLogger().severe("Unable to create sign: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        }
    }

    private void createCommemorationSign(Location playerLocation, String templateId, List<UUID> featuredPlayers) throws IOException, SQLException
    {
        try
        {
            if (schematic instanceof CommemorationSignBlockSchematic)
            {
                //TODO: Ensure this runs async - It's very slow!
                RenderedGraphic graphic = renderRepository.createRender(templateId, (ArrayList<UUID>) featuredPlayers);
                MapView mapView = mapRepository.createCommemorationMap(playerLocation.getWorld(), graphic);
                ((CommemorationSignBlockSchematic) schematic).setMap(mapView);
            }

            // Check for ideal locations
            ArrayList<SchematicLocation> preferredLocations = getCommemorationSignPreferredLocations(playerLocation);
            for (SchematicLocation prefLoc : preferredLocations)
            {
                schematic.setDirection(prefLoc.getSchematicDirection());
                if (builder.testBuildLocation(schematic, prefLoc, false))
                {
                    builder.buildSchematic(schematic, prefLoc, false);
                    return;
                }
            }

            // No ideal location found, try to find any valid location
            HashSet<Location> locationBlacklist = new HashSet<>();
            locationBlacklist.add(playerLocation);
            Location buildableLoc = searcher.findSchematicBuildableLocation(schematic, builder, playerLocation, locationBlacklist,false, 2, 1, 2, schematic.getSupportedDirections());
            builder.buildSchematic(schematic, buildableLoc, false);
        }
        catch (UnableToBuildSchematicException e)
        {
            //NamespacedKey mapKey = NamespacedKey.fromString("commemorations:mapid");

            //ItemStack signStack = new ItemStack(Material.OAK_SIGN, 1);
            //ItemMeta itemMeta = signStack.getItemMeta();
            //PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
            //dataContainer.set();
            // TODO: If sign cannot be built, give to player / drop to ground
        }
    }

    private ArrayList<SchematicLocation> getCommemorationSignPreferredLocations(Location playerLocation)
    {
        schematic.setDirection(BlockFace.NORTH);
        int schematicHalfWidth = (int) Math.ceil(schematic.getWidth() / 2);
        int schematicHalfLength = (int) Math.ceil(schematic.getLength() / 2);
        SchematicLocation[] prefLocations = new SchematicLocation[]
                {
                        new SchematicLocation(playerLocation.clone().add(0, 0, schematicHalfLength), BlockFace.NORTH), // North
                        new SchematicLocation(playerLocation.clone().add(-schematicHalfWidth, 0, schematicHalfLength), BlockFace.NORTH), //North East
                        new SchematicLocation(playerLocation.clone().add(-schematicHalfLength, 0, 0), BlockFace.EAST), // East
                        new SchematicLocation(playerLocation.clone().add(-schematicHalfWidth, 0, -schematicHalfLength), BlockFace.SOUTH), // South East
                        new SchematicLocation(playerLocation.clone().add(0, 0, -schematicHalfLength), BlockFace.SOUTH), // South
                        new SchematicLocation(playerLocation.clone().add(schematicHalfWidth, 0, -schematicHalfLength), BlockFace.SOUTH), // South West
                        new SchematicLocation(playerLocation.clone().add(schematicHalfLength, 0, 0), BlockFace.WEST), // West
                        new SchematicLocation(playerLocation.clone().add(schematicHalfWidth, 0, schematicHalfLength), BlockFace.NORTH) // North West
                };

        ArrayList<SchematicLocation> orderedPrefLocations = new ArrayList<>();
        float yaw = playerLocation.getYaw();
        float yawIncrement = 45.0f;
        while (orderedPrefLocations.size() < prefLocations.length)
        {
            if (yaw > 180.0f)
                yaw -= 360.0f;

            if ((yaw >= 157.5f && yaw < 180.0f) || (yaw >= -180.0f && yaw < -157.5f)) // Player Facing North
                orderedPrefLocations.add(prefLocations[4]); // Sign Facing South
            else if (yaw >= -157.5f && yaw < -112.5f)       // Player Facing North East
                orderedPrefLocations.add(prefLocations[5]); // Sign Facing South West
            else if (yaw >= -112.5f && yaw < -67.5f)        // Player Facing East
                orderedPrefLocations.add(prefLocations[6]); // Sign Facing West
            else if (yaw >= -67.5f && yaw < -22.5f)         // Player Facing South East
                orderedPrefLocations.add(prefLocations[7]); // Sign Facing North West
            else if (yaw >= -22.5f && yaw < 22.5f)          // Player Facing South
                orderedPrefLocations.add(prefLocations[0]); // Sign Facing North
            else if (yaw >= 22.5f && yaw < 67.5f)           // Player Facing South West
                orderedPrefLocations.add(prefLocations[1]); // Sign Facing North East
            else if (yaw >= 67.5f && yaw < 112.5f)          // Player Facing West
                orderedPrefLocations.add(prefLocations[2]); // Sign Facing East
            else if (yaw >= 112.5f && yaw < 157.5f)         // Player Facing North West
                orderedPrefLocations.add(prefLocations[3]); // Player Facing South East

            yaw += yawIncrement;
        }
        return orderedPrefLocations;
    }
}
