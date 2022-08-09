package com.zazsona.commemorations;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.material.Directional;

import java.util.HashMap;

public class BlockSchematicBuilder
{

    /**
     * Tests if the location is suitable for the build
     * @param schematic - the schematic to check a build for
     * @param location - the location to check at
     * @param destroyExisting - whether non-Air blocks can be destroyed
     * @return
     */
    public boolean testBuildLocation(IBlockSchematic schematic, Location location, boolean destroyExisting)
    {
        if (destroyExisting)
            return (location.getWorld().getMaxHeight() < location.getBlockY() + schematic.getHeight());

        World world = location.getWorld();
        HashMap<Location, Material> blockMap = schematic.getBlockLocationMap();
        for (Location schematicLoc : blockMap.keySet())
        {
            Location worldLoc = new Location(world, location.getBlockX() + schematicLoc.getBlockX(), location.getBlockY() + schematicLoc.getBlockY(), location.getBlockZ() + schematicLoc.getBlockZ());
            boolean isAir = world.getBlockAt(worldLoc).getType().isAir();
            if (!isAir)
                return false;
        }
        return true;
    }

    /**
     * Builds the schematic
     * @param schematic - the schematic to build
     * @param location - the location to build at
     * @param destroyExisting - destroy any non-Air blocks
     * @throws IllegalArgumentException - provided location cannot be used for building
     */
    public void buildSchematic(IBlockSchematic schematic, Location location, boolean destroyExisting) throws IllegalArgumentException
    {
        if (!testBuildLocation(schematic, location, destroyExisting))
            throw new IllegalArgumentException("Unable to build the schematic at this location.");

        World world = location.getWorld();
        HashMap<Location, Material> blockMap = schematic.getBlockLocationMap();
        HashMap<Location, EntityType> entityMap = schematic.getEntityLocationMap();
        HashMap<Location, BlockFace> directionMap = schematic.getDirectionMap();

        for (Location schematicLoc : blockMap.keySet())
        {
            Location worldLoc = new Location(world, location.getBlockX() + schematicLoc.getBlockX(), location.getBlockY() + schematicLoc.getBlockY(), location.getBlockZ() + schematicLoc.getBlockZ());
            Material material = blockMap.get(schematicLoc);
            if (material.isBlock())
            {
                Block block = world.getBlockAt(worldLoc);
                block.setType(material);
                if (block instanceof Directional && directionMap.containsKey(schematicLoc))
                {
                    BlockFace direction = directionMap.get(schematicLoc);
                    ((Directional) block).setFacingDirection(direction);
                }
            }

        }

        for (Location schematicLoc : entityMap.keySet())
        {
            Location worldLoc = new Location(world, location.getBlockX() + schematicLoc.getBlockX(), location.getBlockY() + schematicLoc.getBlockY(), location.getBlockZ() + schematicLoc.getBlockZ());
            EntityType entityType = entityMap.get(schematicLoc);
            Entity entity = world.spawnEntity(worldLoc, entityType);
            if (entity instanceof Directional && directionMap.containsKey(schematicLoc))
            {
                BlockFace direction = directionMap.get(schematicLoc);
                ((Directional) entity).setFacingDirection(direction);
            }
        }
    }
}
