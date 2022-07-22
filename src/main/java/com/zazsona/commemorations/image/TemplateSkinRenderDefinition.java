package com.zazsona.commemorations.image;

public class TemplateSkinRenderDefinition
{
    private SkinRenderType skinRenderType;
    private int startX;
    private int startY;
    private int width;
    private int height;

    public TemplateSkinRenderDefinition(SkinRenderType skinRenderType, int startX, int startY, int width, int height)
    {
        this.skinRenderType = skinRenderType;
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
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
