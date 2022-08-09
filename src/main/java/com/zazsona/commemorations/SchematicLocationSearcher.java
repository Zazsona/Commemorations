package com.zazsona.commemorations;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

/***
 * A class to try and find a valid location to build a schematic
 */
public class SchematicLocationSearcher
{
    public Location findSchematicBuildableLocation(IBlockSchematic schematic, BlockSchematicBuilder schematicBuilder, Location origin, boolean destroyExistingBlocks, int xRange, int yRange, int zRange, BlockFace[] directions)
    {
        World world = origin.getWorld();
        int originX = origin.getBlockX();
        int originY = origin.getBlockY();
        int originZ = origin.getBlockZ();

        for (BlockFace direction : directions)
        {
            schematic.setDirection(direction);
            for (int xOffset = 0; xOffset < xRange; xOffset++)
            {
                for (int zOffset = 0; zOffset < zRange; zOffset++)  // Work out from the origin, assuming that's preferred location
                {
                    for (int yOffset = 0; yOffset < yRange; yOffset++)
                    {
                        boolean isBuildable;
                        Location posLoc = new Location(world, originX + xOffset, originY + yOffset, originZ + zOffset);
                        isBuildable = schematicBuilder.testBuildLocation(schematic, posLoc, destroyExistingBlocks);
                        if (isBuildable)
                            return posLoc;

                        Location negLoc = new Location(world, originX - xOffset, originY - yOffset, originZ - zOffset);
                        isBuildable = schematicBuilder.testBuildLocation(schematic, negLoc, destroyExistingBlocks);
                        if (isBuildable)
                            return negLoc;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Unable to find suitable build location in range.");
    }
}
