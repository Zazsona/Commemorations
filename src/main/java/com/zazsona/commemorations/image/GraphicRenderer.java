package com.zazsona.commemorations.image;

import com.zazsona.commemorations.CommemorationsPlugin;
import com.zazsona.commemorations.database.TemplateSkinRenderDefinition;
import com.zazsona.commemorations.repository.CommemorationsPlayerRepository;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class GraphicRenderer
{
    private SkinRenderer skinRenderer;

    public GraphicRenderer(SkinRenderer skinRenderer)
    {
        this.skinRenderer = skinRenderer;
    }

    public BufferedImage renderGraphic(BufferedImage background, BufferedImage foreground, ArrayList<TemplateSkinRenderDefinition> skinDefinitions, ArrayList<UUID> playerGuids) throws SQLException, IOException
    {
        if (skinDefinitions.size() != playerGuids.size())
            throw new IllegalArgumentException("skinDefinitions and playerGuids must be of the same size.");

        BufferedImage render = new BufferedImage(background.getWidth(), background.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D renderGraphics = render.createGraphics();
        if (background != null)
            renderGraphics.drawImage(background, 0, 0, background.getWidth(), background.getHeight(), null);

        CommemorationsPlayerRepository playerRepository = CommemorationsPlugin.getInstance().getPlayerRepository();
        for (int i = 0; i < skinDefinitions.size(); i++)
        {
            TemplateSkinRenderDefinition skinDefinition = skinDefinitions.get(i);
            UUID playerGuid = playerGuids.get(i);

            String skinBase64 = playerRepository.getPlayer(playerGuid).getSkinBase64();
            BufferedImage skinRender = skinRenderer.renderSkin(skinBase64, skinDefinition.getSkinRenderType());
            renderGraphics.drawImage(skinRender, skinDefinition.getStartX(), skinDefinition.getStartY(), skinDefinition.getWidth(), skinDefinition.getHeight(), null);
        }

        if (foreground != null)
            renderGraphics.drawImage(foreground, 0, 0, foreground.getWidth(), foreground.getHeight(), null);
        renderGraphics.dispose();
        return render;
    }
}
