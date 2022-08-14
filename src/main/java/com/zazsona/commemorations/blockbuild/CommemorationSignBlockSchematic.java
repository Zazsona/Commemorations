package com.zazsona.commemorations.blockbuild;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.map.MapView;

import java.util.ArrayList;
import java.util.HashMap;

public class CommemorationSignBlockSchematic implements IBlockSchematic
{
    private HashMap<Location, Material> locBlockMap;
    private HashMap<Location, EntityType> locEntityMap;
    private HashMap<Location, BlockFace> locDirectionMap;
    private int width;
    private int height;
    private int length;
    private BlockFace direction;
    private MapView map;

    public CommemorationSignBlockSchematic()
    {
        this.direction = BlockFace.NORTH;
        generateSchematic();
    }

    public CommemorationSignBlockSchematic(BlockFace direction)
    {
        this.direction = direction;
        generateSchematic();
    }

    @Override
    public void setDirection(BlockFace direction)
    {
        boolean isDirectionSupported = false;
        for (BlockFace supportedDirection : getSupportedDirections())
        {
            if (direction == supportedDirection)
            {
                isDirectionSupported = true;
                break;
            }
        }
        if (!isDirectionSupported)
            throw new IllegalArgumentException("Unsupported direction: " + direction.toString()+ ".");

        this.direction = direction;
        generateSchematic();
    }

    @Override
    public BlockFace getDirection()
    {
        return direction;
    }

    @Override
    public ArrayList<BlockFace> getSupportedDirections()
    {
        ArrayList<BlockFace> supportedDirections = new ArrayList<>();
        supportedDirections.add(BlockFace.NORTH);
        supportedDirections.add(BlockFace.EAST);
        supportedDirections.add(BlockFace.SOUTH);
        supportedDirections.add(BlockFace.WEST);
        return supportedDirections;
    }


    @Override
    public int getWidth()
    {
        return this.width;
    }

    @Override
    public int getHeight()
    {
        return this.height;
    }

    @Override
    public int getLength()
    {
        return this.length;
    }

    @Override
    public HashMap<Location, Material> getBlockLocationMap()
    {
        return locBlockMap;
    }

    @Override
    public HashMap<Location, EntityType> getEntityLocationMap()
    {
        return locEntityMap;
    }

    @Override
    public HashMap<Location, BlockFace> getDirectionMap()
    {
        return locDirectionMap;
    }

    /**
     * Gets the graphic map to display on this sign
     * @return map
     */
    public MapView getMap()
    {
        return map;
    }

    /**
     * Sets the graphic map to display on this sign
     * @return map
     */
    public void setMap(MapView map)
    {
        this.map = map;
    }

    private void generateSchematic()
    {
        locBlockMap = new HashMap<>();
        locEntityMap = new HashMap<>();
        locDirectionMap = new HashMap<>();

        boolean isXDirection = direction == BlockFace.EAST || direction == BlockFace.WEST;
        boolean isZDirection = direction == BlockFace.NORTH || direction == BlockFace.SOUTH;
        this.width = (isXDirection) ? 2 : 1;
        this.length = (isZDirection) ? 2 : 1;
        this.height = 2;

        generateBlockLocations(direction);
        generateEntityLocations(direction);
    }

    private void generateBlockLocations(BlockFace direction)
    {
        if (direction == BlockFace.EAST)
        {
            locBlockMap.put(new Location(null, 0, 0, 0), Material.OAK_FENCE);
            locBlockMap.put(new Location(null, 0, 1, 0), Material.OAK_FENCE);
            locBlockMap.put(new Location(null, 1, 1, 0), Material.AIR);
        }
        else if (direction == BlockFace.WEST)
        {
            locBlockMap.put(new Location(null, 0, 0, 0), Material.OAK_FENCE);
            locBlockMap.put(new Location(null, 0, 1, 0), Material.OAK_FENCE);
            locBlockMap.put(new Location(null, -1, 1, 0), Material.AIR);
        }
        else if (direction == BlockFace.NORTH)
        {
            locBlockMap.put(new Location(null, 0, 0, 0), Material.OAK_FENCE);
            locBlockMap.put(new Location(null, 0, 1, 0), Material.OAK_FENCE);
            locBlockMap.put(new Location(null, 0, 1, -1), Material.AIR);
        }
        else if (direction == BlockFace.SOUTH)
        {
            locBlockMap.put(new Location(null, 0, 0, 0), Material.OAK_FENCE);
            locBlockMap.put(new Location(null, 0, 1, 0), Material.OAK_FENCE);
            locBlockMap.put(new Location(null, 0, 1, 1), Material.AIR);
        }
    }

    private void generateEntityLocations(BlockFace direction)
    {
        if (direction == BlockFace.EAST)
        {
            Location location = new Location(null, 1, 1, 0);
            locEntityMap.put(location, EntityType.ITEM_FRAME);
            locDirectionMap.put(location, BlockFace.EAST);
        }
        else if (direction == BlockFace.WEST)
        {
            Location location = new Location(null, -1, 1, 0);
            locEntityMap.put(location, EntityType.ITEM_FRAME);
            locDirectionMap.put(location, BlockFace.WEST);
        }
        else if (direction == BlockFace.NORTH)
        {
            Location location = new Location(null, 0, 1, -1);
            locEntityMap.put(location, EntityType.ITEM_FRAME);
            locDirectionMap.put(location, BlockFace.NORTH);
        }
        else if (direction == BlockFace.SOUTH)
        {
            Location location = new Location(null, 0, 1, 1);
            locEntityMap.put(location, EntityType.ITEM_FRAME);
            locDirectionMap.put(location, BlockFace.SOUTH);
        }
    }
}
