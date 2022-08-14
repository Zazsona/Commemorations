package com.zazsona.commemorations;

import com.zazsona.commemorations.blockbuild.*;
import com.zazsona.commemorations.config.AdvancementCommemorationConfig;
import com.zazsona.commemorations.config.KillAPlayerCommemorationConfig;
import com.zazsona.commemorations.config.PluginConfig;
import com.zazsona.commemorations.config.StatisticCommemorationConfig;
import com.zazsona.commemorations.exception.UnableToBuildSchematicException;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class CommemorationTriggerListener implements Listener, IIntervalledStatisticListener
{
    private RenderRepository renderRepository;
    private IBlockSchematic schematic;
    private BlockSchematicBuilder builder;
    private SchematicLocationSearcher searcher;

    public CommemorationTriggerListener(RenderRepository renderRepository, IBlockSchematic schematic, BlockSchematicBuilder builder, SchematicLocationSearcher searcher)
    {
        this.renderRepository = renderRepository;
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

        AdvancementCommemorationConfig commemorationConfig = (AdvancementCommemorationConfig) PluginConfig.getInstance().getCommemoration(key);
        String templateId = commemorationConfig.getTemplateId();
        ArrayList<UUID> featuredPlayers = (ArrayList<UUID>) commemorationConfig.resolveFeaturedPlayers(player.getUniqueId());
        Bukkit.getScheduler().runTaskAsynchronously(CommemorationsPlugin.getInstance(), () ->
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

        StatisticCommemorationConfig commemorationConfig = (StatisticCommemorationConfig) PluginConfig.getInstance().getCommemoration(key);
        if (e.getPreviousValue() < commemorationConfig.getStatisticValue() && e.getNewValue() >= commemorationConfig.getStatisticValue())
        {
            String templateId = commemorationConfig.getTemplateId();
            ArrayList<UUID> featuredPlayers = (ArrayList<UUID>) commemorationConfig.resolveFeaturedPlayers(player.getUniqueId());
            Bukkit.getScheduler().runTaskAsynchronously(CommemorationsPlugin.getInstance(), () ->
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

        if (killer != null)
        {
            KillAPlayerCommemorationConfig commemorationConfig = (KillAPlayerCommemorationConfig) PluginConfig.getInstance().getCommemoration(key);
            String templateId = commemorationConfig.getTemplateId();
            ArrayList<UUID> featuredPlayers = (ArrayList<UUID>) commemorationConfig.resolveFeaturedPlayers(killer.getUniqueId(), victim.getUniqueId());
            Bukkit.getScheduler().runTaskAsynchronously(CommemorationsPlugin.getInstance(), () ->
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
            renderRepository.createRender(templateId, (ArrayList<UUID>) featuredPlayers);
            //TODO: Create the map
            //Bukkit.createMap()

            // Check for ideal locations
            int schematicHalfWidth = (int) Math.ceil(schematic.getWidth() / 2);
            int schematicHalfLength = (int) Math.ceil(schematic.getLength() / 2);
            ArrayList<SchematicLocation> preferredLocations = new ArrayList<>();
            preferredLocations.add(new SchematicLocation(playerLocation.clone().add(schematicHalfWidth, 0, 0), BlockFace.WEST));
            preferredLocations.add(new SchematicLocation(playerLocation.clone().subtract(schematicHalfWidth, 0, 0), BlockFace.EAST));
            preferredLocations.add(new SchematicLocation(playerLocation.clone().add(0, 0, schematicHalfLength), BlockFace.NORTH));
            preferredLocations.add(new SchematicLocation(playerLocation.clone().subtract(0, 0, schematicHalfLength), BlockFace.SOUTH));
            for (SchematicLocation prefLoc : preferredLocations)
            {
                if (builder.testBuildLocation(schematic, prefLoc, false))
                {
                    builder.buildSchematic(schematic, prefLoc, false);
                    return;
                }
            }

            // No ideal location found, try to find any valid location
            HashSet<Location> locationBlacklist = new HashSet<>();
            locationBlacklist.add(playerLocation);
            Location buildableLoc = searcher.findSchematicBuildableLocation(schematic, builder, playerLocation, locationBlacklist,false, schematicHalfWidth, 1, schematicHalfLength, schematic.getSupportedDirections());
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
}
