package com.zazsona.commemorations.database;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

public class CommemorationsPlayer
{
    private UUID playerGuid;
    private String username;
    private String skinBase64;
    private long lastUpdated;

    public CommemorationsPlayer(UUID playerGuid, String username, String skinBase64, long lastUpdated)
    {
        this.playerGuid = playerGuid;
        this.username = username;
        this.skinBase64 = skinBase64;
        this.lastUpdated = lastUpdated;
    }

    public UUID getPlayerGuid()
    {
        return playerGuid;
    }

    public String getUsername()
    {
        return username;
    }

    public String getSkinBase64()
    {
        return skinBase64;
    }

    public long getLastUpdated()
    {
        return lastUpdated;
    }

    /**
     * Converts the Skin Base64 into a BufferedImage
     * @return the skin
     * @throws IOException - Unable to generate skin from Base64
     */
    public BufferedImage getSkin() throws IOException
    {
        byte[] decodedBytes = Base64.getDecoder().decode(skinBase64);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(decodedBytes));
        return image;
    }

    public Player getPlayer()
    {
        return Bukkit.getPlayer(playerGuid);
    }

    public OfflinePlayer getOfflinePlayer()
    {
        return Bukkit.getOfflinePlayer(playerGuid);
    }
}
