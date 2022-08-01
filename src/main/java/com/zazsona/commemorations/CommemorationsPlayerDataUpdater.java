package com.zazsona.commemorations;

import com.zazsona.commemorations.apiresponse.ProfileResponse;
import com.zazsona.commemorations.database.CommemorationsPlayer;
import com.zazsona.commemorations.database.RenderedGraphic;
import com.zazsona.commemorations.image.PlayerProfileFetcher;
import com.zazsona.commemorations.repository.CommemorationsPlayerRepository;
import com.zazsona.commemorations.repository.RenderRepository;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

public class CommemorationsPlayerDataUpdater implements Listener
{
    private PlayerProfileFetcher profileFetcher;
    private RenderRepository renderRepository;

    public CommemorationsPlayerDataUpdater(PlayerProfileFetcher profileFetcher, RenderRepository renderRepository)
    {
        this.profileFetcher = profileFetcher;
        this.renderRepository = renderRepository;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        // 300,000ms == 5m
        if (e.getPlayer().getLastPlayed() + 300000 <= Instant.now().toEpochMilli())
        {
            Bukkit.getScheduler().runTaskAsynchronously(CommemorationsPlugin.getInstance(), () ->
            {
                try
                {
                    UUID playerGuid = e.getPlayer().getUniqueId();
                    CommemorationsPlayerRepository playerRepository = CommemorationsPlugin.getInstance().getPlayerRepository();
                    if (!playerRepository.isPlayerRegistered(playerGuid))
                    {
                        registerPlayer(playerGuid);
                        return;
                    }

                    CommemorationsPlayer commPlayer = playerRepository.getPlayer(playerGuid);
                    if (!commPlayer.getUsername().equals(e.getPlayer().getName()))
                    {
                        registerPlayer(playerGuid);
                        return;
                    }

                    BufferedImage currentSkin = profileFetcher.fetchPlayerSkin(playerGuid);
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    ImageIO.write(currentSkin, "png", byteStream);
                    String currentSkinBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());
                    if (!currentSkinBase64.equals(commPlayer.getSkinBase64()))
                    {
                        registerPlayer(playerGuid);
                        return;
                    }
                }
                catch (SQLException | IOException ex)
                {
                    CommemorationsPlugin.getInstance().getLogger().severe("Unable to register player \"" + e.getPlayer().getName()+"\":");
                    ex.printStackTrace();
                }
            });
        }
    }

    public CommemorationsPlayer registerPlayer(UUID playerGuid) throws SQLException, IOException
    {
        CommemorationsPlayerRepository playerRepository = CommemorationsPlugin.getInstance().getPlayerRepository();
        ProfileResponse playerProfile = profileFetcher.fetchPlayerProfile(playerGuid);
        String username = playerProfile.getName();
        BufferedImage skin = profileFetcher.fetchPlayerSkin(playerGuid);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(skin, "png", byteStream);
        String skinBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());

        if (playerRepository.isPlayerRegistered(playerGuid))
        {
            CommemorationsPlayer player = playerRepository.updatePlayer(playerGuid, username, skinBase64);
            ArrayList<RenderedGraphic> renders = renderRepository.getRendersWithPlayer(playerGuid);
            for (RenderedGraphic render : renders)
                renderRepository.refreshRender(render.getRenderGuid());
            return player;
        }
        else
            return playerRepository.addPlayer(playerGuid, username, skinBase64);
    }
}
