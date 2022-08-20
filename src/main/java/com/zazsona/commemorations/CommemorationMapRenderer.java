package com.zazsona.commemorations;

import com.zazsona.commemorations.database.MapIdRenderedGraphicPair;
import com.zazsona.commemorations.database.RenderedGraphic;
import com.zazsona.commemorations.repository.CommemorationMapRepository;
import com.zazsona.commemorations.repository.RenderRepository;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.io.IOException;
import java.sql.SQLException;

public class CommemorationMapRenderer extends MapRenderer
{
    private CommemorationMapRepository mapRepository;
    private RenderRepository renderRepository;

    public CommemorationMapRenderer(CommemorationMapRepository mapRepository, RenderRepository renderRepository)
    {
        this.mapRepository = mapRepository;
        this.renderRepository = renderRepository;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player)
    {
        try
        {
            int mapId = map.getId();
            MapIdRenderedGraphicPair pair = mapRepository.getMapGraphicPair(mapId);
            RenderedGraphic render = renderRepository.getRender(pair.getRenderGuid());
            map.setScale(MapView.Scale.NORMAL);
            canvas.drawImage(0, 0, render.getImage());
        }
        catch (SQLException | IOException e)
        {
            CommemorationsPlugin.getInstance().getLogger().severe("Unable to render commemoration map: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
