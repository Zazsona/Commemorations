package com.zazsona.commemorations.database;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

public class TemplateGraphic
{
    private String templateId;
    private String backgroundImageBase64;
    private String foregroundImageBase64;
    private long lastUpdated;

    public TemplateGraphic(String templateId, String backgroundImageBase64, String foregroundImageBase64, long lastUpdated)
    {
        this.templateId = templateId;
        this.backgroundImageBase64 = backgroundImageBase64;
        this.foregroundImageBase64 = foregroundImageBase64;
        this.lastUpdated = lastUpdated;
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

    public String getBackgroundImageBase64()
    {
        return backgroundImageBase64;
    }

    public String getForegroundImageBase64()
    {
        return foregroundImageBase64;
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
     * Converts the Background Image Base64 into a BufferedImage
     * @return the rendered image
     * @throws IOException - Unable to generate image from Base64
     */
    public BufferedImage getBackgroundImage() throws IOException
    {
        byte[] decodedBytes = Base64.getDecoder().decode(backgroundImageBase64);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(decodedBytes));
        return image;
    }

    /**
     * Converts the Foreground Image Base64 into a BufferedImage
     * @return the rendered image
     * @throws IOException - Unable to generate image from Base64
     */
    public BufferedImage getForegroundImage() throws IOException
    {
        byte[] decodedBytes = Base64.getDecoder().decode(foregroundImageBase64);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(decodedBytes));
        return image;
    }
}
