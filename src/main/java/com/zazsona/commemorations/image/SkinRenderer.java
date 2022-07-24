package com.zazsona.commemorations.image;

import org.apache.commons.lang.NotImplementedException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class SkinRenderer
{
    public BufferedImage renderSkin(BufferedImage playerSkin, SkinRenderType renderType)
    {
        return renderOutputFormat(renderType, playerSkin);
    }

    public BufferedImage renderSkin(String skinBase64, SkinRenderType renderType) throws IOException
    {
        byte[] decodedBytes = Base64.getDecoder().decode(skinBase64);
        BufferedImage playerSkin = ImageIO.read(new ByteArrayInputStream(decodedBytes));
        return renderOutputFormat(renderType, playerSkin);
    }

    private BufferedImage renderOutputFormat(SkinRenderType renderType, BufferedImage playerSkin)
    {
        switch (renderType)
        {
            case TEXTURE:
                return playerSkin;
            case HEAD:
                return renderSkinHead(playerSkin, true);
            default:
                throw new NotImplementedException("This render type has not yet been implemented.");
        }
    }

    private BufferedImage renderSkinHead(BufferedImage playerSkin, boolean includeHood)
    {
        BufferedImage face = playerSkin.getSubimage(8, 8, 8, 8);
        if (!includeHood)
            return face;

        BufferedImage hood = playerSkin.getSubimage(40, 8, 8, 8);
        BufferedImage composite = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D skinGraphics = composite.createGraphics();
        skinGraphics.drawImage(face, 0, 0, 8, 8, null);
        skinGraphics.drawImage(hood, 0, 0, 8, 8, null);
        skinGraphics.dispose();
        return composite;
    }
}
