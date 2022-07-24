package com.zazsona.commemorations.database;

import com.zazsona.commemorations.image.SkinRenderType;

import java.util.UUID;

public class TemplateSkinRenderDefinition
{
    private UUID templateId;
    private SkinRenderType skinRenderType;
    private int startX;
    private int startY;
    private int width;
    private int height;

    public TemplateSkinRenderDefinition(UUID templateId, SkinRenderType skinRenderType, int startX, int startY, int width, int height)
    {
        this.templateId = templateId;
        this.skinRenderType = skinRenderType;
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
    }

    public UUID getTemplateId()
    {
        return templateId;
    }

    public SkinRenderType getSkinRenderType()
    {
        return skinRenderType;
    }

    public int getStartX()
    {
        return startX;
    }

    public int getStartY()
    {
        return startY;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }
}
