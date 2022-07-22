package com.zazsona.commemorations.image;

public class TemplateSkinRenderDefinition
{
    private SkinRenderType skinRenderType;
    private int startX;
    private int startY;
    private int width;
    private int length;

    public TemplateSkinRenderDefinition(SkinRenderType skinRenderType, int startX, int startY, int width, int length)
    {
        this.skinRenderType = skinRenderType;
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.length = length;
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

    public int getLength()
    {
        return length;
    }
}
