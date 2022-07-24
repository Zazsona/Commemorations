package com.zazsona.commemorations;

import com.zazsona.commemorations.database.CommemorationsPlayer;
import com.zazsona.commemorations.image.SkinRenderType;
import com.zazsona.commemorations.image.SkinRenderer;
import com.zazsona.commemorations.repository.CommemorationsPlayerRepository;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class CommemorationsPlayerDataUpdater implements Listener
{
    private SkinRenderer skinRenderer;

    public CommemorationsPlayerDataUpdater(SkinRenderer skinRenderer)
    {
        this.skinRenderer = skinRenderer;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        // 300,000ms == 5m
        if (e.getPlayer().getLastPlayed() + 300000 <= Instant.now().toEpochMilli())
        //if (true)
        {
            Bukkit.getScheduler().runTaskAsynchronously(CommemorationsPlugin.getInstance(), () ->
            {
                try
                {
                    UUID playerId = e.getPlayer().getUniqueId();
                    CommemorationsPlayerRepository playerRepository = CommemorationsPlugin.getInstance().getPlayerRepository();
                    if (!playerRepository.isPlayerRegistered(playerId))
                    {
                        playerRepository.registerPlayer(e.getPlayer());
                        return;
                    }

                    CommemorationsPlayer commPlayer = playerRepository.getPlayer(playerId);
                    if (!commPlayer.getUsername().equals(e.getPlayer().getName()))
                    {
                        playerRepository.registerPlayer(e.getPlayer());
                        return;
                    }

                    BufferedImage currentSkin = skinRenderer.renderSkin(playerId, SkinRenderType.TEXTURE);
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    ImageIO.write(currentSkin, "png", byteStream);
                    String currentSkinBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());
                    if (!currentSkinBase64.equals(commPlayer.getSkinBase64()))
                    {
                        playerRepository.registerPlayer(e.getPlayer());
                        return;
                    }
                }
                catch (SQLException | IOException ex)
                {
                    ex.printStackTrace();
                }
            });
        }
    }
}