package com.zazsona.commemorations.blockbuild;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.HashMap;

public interface IBlockSchematic
{
    /**
     * Sets the direction of the schematic
     * @param direction
     */
    void setDirection(BlockFace direction);

    /**
     * Gets the direction of the schematic
     */
    BlockFace getDirection();

    /**
     * Gets the directions supported by this schematic
     * @return
     */
    ArrayList<BlockFace> getSupportedDirections();

    /**
     * Gets the schematic size on the X axis.
     * @return
     */
    int getWidth();

    /**
     * Gets the schematic size on the Y axis.
     * @return
     */
    int getHeight();

    /**
     * Gets the schematic size on the Z axis.
     * @return
     */
    int getLength();

    /**
     * Gets the block location offsets of this schematic
     * @return
     */
    HashMap<Location, Material> getBlockLocationMap();

    /**
     * Gets the entity location offsets of this schematic
     * @return
     */
    HashMap<Location, EntityType> getEntityLocationMap();

    /**
     * Gets the facing directions
     * @return
     */
    HashMap<Location, BlockFace> getDirectionMap();

}
