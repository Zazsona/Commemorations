package com.zazsona.commemorations.database;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

public class RenderedGraphic
{
    private UUID renderGuid;
    private String templateId;
    private String imageBase64;
    private long lastUpdated;

    public RenderedGraphic(UUID renderGuid, String templateId, String imageBase64, long lastUpdated)
    {
        this.renderGuid = renderGuid;
        this.templateId = templateId;
        this.imageBase64 = imageBase64;
        this.lastUpdated = lastUpdated;
    }

    /**
     * Gets renderGuid
     *
     * @return renderGuid
     */
    public UUID getRenderGuid()
    {
        return renderGuid;
    }

    /**
     * Gets templateId
     *
     * @return templateId
     */
    public String getTemplateId()
    {
        return templateId;
    }

    /**
     * Gets imageBase64
     *
     * @return imageBase64
     */
    public String getImageBase64()
    {
        return imageBase64;
    }

    /**
     * Gets lastUpdated
     *
     * @return lastUpdated
     */
    public long getLastUpdated()
    {
        return lastUpdated;
    }

    /**
     * Converts the Image Base64 into a BufferedImage
     * @return the rendered image
     * @throws IOException - Unable to generate image from Base64
     */
    public BufferedImage getImage() throws IOException
    {
        byte[] decodedBytes = Base64.getDecoder().decode(imageBase64);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(decodedBytes));
        return image;
    }
}
