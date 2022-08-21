package com.zazsona.commemorations.blockbuild;

import com.zazsona.commemorations.exception.UnableToBuildSchematicException;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CommemorationSignBlockSchematicBuilder extends BlockSchematicBuilder
{
    @Override
    public void buildSchematic(IBlockSchematic schematic, Location location, boolean destroyExisting) throws UnableToBuildSchematicException
    {
        super.buildSchematic(schematic, location, destroyExisting);
        CommemorationSignBlockSchematic signSchematic = (CommemorationSignBlockSchematic) schematic;
        World world = location.getWorld();
        HashMap<Location, EntityType> entityMap = schematic.getEntityLocationMap();
        HashMap<Chunk, HashSet<Location>> itemFrameLocMap = new HashMap<>();
        for (Map.Entry<Location, EntityType> schematicEntity : entityMap.entrySet()) 
        {
            Location schematicLoc = schematicEntity.getKey();
            EntityType entityType = schematicEntity.getValue();
            if (entityType == EntityType.ITEM_FRAME || entityType == EntityType.GLOW_ITEM_FRAME)
            {
                Location worldLoc = new Location(world, location.getBlockX() + schematicLoc.getBlockX(), location.getBlockY() + schematicLoc.getBlockY(), location.getBlockZ() + schematicLoc.getBlockZ());
                Chunk worldChunk = worldLoc.getChunk();
                if (!itemFrameLocMap.containsKey(worldChunk))
                    itemFrameLocMap.put(worldChunk, new HashSet<>());
                itemFrameLocMap.get(worldChunk).add(worldLoc);
            }
        }

        for (Map.Entry<Chunk, HashSet<Location>> itemFrameLocEntry : itemFrameLocMap.entrySet())
        {
            Chunk chunk = itemFrameLocEntry.getKey();
            HashSet<Location> frameLocs = itemFrameLocEntry.getValue();

            Entity[] entities = chunk.getEntities();
            for (Entity entity : entities)
            {
                Location entityLoc = entity.getLocation();
                Location entityBlockLoc = new Location(world, entityLoc.getBlockX(), entityLoc.getBlockY(), entityLoc.getBlockZ());
                if (frameLocs.contains(entityBlockLoc))
                {
                    ItemFrame itemFrame = (ItemFrame) entity;
                    MapView mapView = signSchematic.getMap();
                    ItemStack mapStack = new ItemStack(Material.FILLED_MAP);
                    MapMeta mapMeta = (MapMeta) mapStack.getItemMeta();
                    mapMeta.setMapId(mapView.getId());
                    mapStack.setItemMeta(mapMeta);
                    itemFrame.setItem(mapStack);
                }
            }
        }
    }
}
