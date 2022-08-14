package com.zazsona.commemorations.blockbuild;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.Objects;

public class SchematicLocation extends Location
{
    private BlockFace direction;

    public SchematicLocation(World world, double x, double y, double z, BlockFace schematicDirection)
    {
        super(world, x, y, z);
        this.direction = schematicDirection;
    }

    public SchematicLocation(Location location, BlockFace schematicDirection)
    {
        super(location.getWorld(), location.getX(), location.getY(), location.getZ());
        this.direction = schematicDirection;
    }

    public BlockFace getSchematicDirection()
    {
        return direction;
    }

    public void setSchematicDirection(BlockFace direction)
    {
        this.direction = direction;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SchematicLocation that = (SchematicLocation) o;
        return direction == that.direction;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), direction);
    }
}
