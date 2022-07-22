package com.zazsona.commemorations.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.UUID;

public class GraphicRenderer
{
    private SkinRenderer skinRenderer;

    public GraphicRenderer()
    {
        this.skinRenderer = new SkinRenderer();
    }

    public BufferedImage renderGraphic(BufferedImage template, ArrayList<TemplateSkinRenderDefinition> skinDefinitions, ArrayList<UUID> playerIds) throws IOException
    {
        if (skinDefinitions.size() != playerIds.size())
            throw new InvalidParameterException("skinDefinitions and playerIds must be of the same size.");

        BufferedImage render = new BufferedImage(template.getWidth(), template.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D renderGraphics = render.createGraphics();
        renderGraphics.drawImage(template, 0, 0, template.getWidth(), template.getHeight(), null);

        for (int i = 0; i < skinDefinitions.size(); i++)
        {
            TemplateSkinRenderDefinition skinDefinition = skinDefinitions.get(i);
            UUID playerId = playerIds.get(i);

            BufferedImage skinRender = skinRenderer.renderSkin(playerId, skinDefinition.getSkinRenderType());
            renderGraphics.drawImage(skinRender, skinDefinition.getStartX(), skinDefinition.getStartY(), skinDefinition.getWidth(), skinDefinition.getHeight(), null);
        }
        renderGraphics.dispose();
        return render;
    }
}
