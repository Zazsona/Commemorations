package com.zazsona.commemorations.database;

import java.util.UUID;

public class MapIdRenderedGraphicPair
{
    private int mapId;
    private UUID renderGuid;

    public MapIdRenderedGraphicPair(int mapId, UUID renderGuid)
    {
        this.mapId = mapId;
        this.renderGuid = renderGuid;
    }

    public int getMapId()
    {
        return mapId;
    }

    public UUID getRenderGuid()
    {
        return renderGuid;
    }
}
