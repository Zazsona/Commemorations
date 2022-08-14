package com.zazsona.commemorations.blockbuild;

import com.zazsona.commemorations.exception.UnableToBuildSchematicException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Set;

/***
 * A class to try and find a valid location to build a schematic
 */
public class SchematicLocationSearcher
{
    public SchematicLocation findSchematicBuildableLocation(IBlockSchematic schematic, BlockSchematicBuilder schematicBuilder, Location origin, Set<Location> locationBlacklist, boolean destroyExistingBlocks, int xRange, int yRange, int zRange, ArrayList<BlockFace> directions) throws UnableToBuildSchematicException
    {
        for (BlockFace direction : directions)
        {
            try
            {
                schematic.setDirection(direction);
                SchematicLocation validLocation = findSchematicBuildableLocation(schematic, schematicBuilder, origin, locationBlacklist, destroyExistingBlocks, xRange, yRange, zRange);
                if (validLocation != null) return validLocation;
            }
            catch (UnableToBuildSchematicException e)
            {
                continue;
            }
        }
        throw new UnableToBuildSchematicException("Unable to find suitable build location in range.");
    }

    private SchematicLocation findSchematicBuildableLocation(IBlockSchematic schematic, BlockSchematicBuilder schematicBuilder, Location origin, Set<Location> locationBlacklist, boolean destroyExistingBlocks, int xRange, int yRange, int zRange) throws UnableToBuildSchematicException
    {
        World world = origin.getWorld();
        int originX = origin.getBlockX();
        int originY = origin.getBlockY();
        int originZ = origin.getBlockZ();

        for (int xOffset = 0; xOffset < xRange; xOffset++)
        {
            for (int zOffset = 0; zOffset < zRange; zOffset++)  // Work out from the origin, assuming that's preferred location
            {
                for (int yOffset = 0; yOffset < yRange; yOffset++)
                {
                    SchematicLocation posLoc = new SchematicLocation(world, originX + xOffset, originY + yOffset, originZ + zOffset, schematic.getDirection());
                    if (isLocationBuildable(posLoc, schematic, schematicBuilder, locationBlacklist, destroyExistingBlocks))
                        return new SchematicLocation(posLoc, schematic.getDirection());

                    SchematicLocation negLoc = new SchematicLocation(world, originX - xOffset, originY - yOffset, originZ - zOffset, schematic.getDirection());
                    if (isLocationBuildable(negLoc, schematic, schematicBuilder, locationBlacklist, destroyExistingBlocks))
                        return new SchematicLocation(negLoc, schematic.getDirection());
                }
            }
        }
        throw new UnableToBuildSchematicException("Unable to find suitable build location in range.");
    }

    private boolean isLocationBuildable(Location location, IBlockSchematic schematic, BlockSchematicBuilder schematicBuilder, Set<Location> locationBlacklist, boolean destroyExistingBlocks)
    {
        boolean isBlacklisted = (locationBlacklist != null && locationBlacklist.contains(location));
        if (isBlacklisted) // Checking this first as it might save running the more expensive testBuildLocation()
            return false;
        else
            return schematicBuilder.testBuildLocation(schematic, location, destroyExistingBlocks);
    }
}
