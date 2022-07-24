package com.zazsona.commemorations.image;

import com.google.gson.Gson;
import com.zazsona.commemorations.apiresponse.ProfileResponse;
import com.zazsona.commemorations.apiresponse.ProfileTextureData;
import org.apache.commons.lang.NotImplementedException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.*;

public class SkinRenderer
{
    private HashMap<UUID, ProfileResponse> profileCache;
    private HashMap<UUID, Long> profileCacheTimestamps;
    private Object cacheLock;
    private Timer cacheCleanUpTaskTimer;

    public SkinRenderer()
    {
        this.profileCache = new HashMap<>();
        this.profileCacheTimestamps = new HashMap<>();
        this.cacheLock = new Object();
        cacheCleanUpTaskTimer = new Timer();

        TimerTask cacheCleanupTask = new TimerTask()
        {
            @Override
            public void run()
            {
                synchronized(cacheLock)
                {
                    long currentUnixEpoch = Instant.now().getEpochSecond();
                    long cacheTimeoutDuration = 1 * 60 * 60; // 1 Hour
                    Iterator<Map.Entry<UUID, Long>> iterator = profileCacheTimestamps.entrySet().iterator();
                    while (iterator.hasNext())
                    {
                        Map.Entry<UUID, Long> entry = iterator.next();
                        long cachedAtUnixEpoch = entry.getValue();
                        if (currentUnixEpoch >= (cachedAtUnixEpoch + cacheTimeoutDuration))
                        {
                            profileCache.remove(entry.getKey());
                            iterator.remove();
                        }
                    }
                }
            }
        };
        cacheCleanUpTaskTimer.schedule(cacheCleanupTask, (1000 * 60 * 30)); // 30 mins
    }

    public BufferedImage renderSkin(UUID playerId, SkinRenderType renderType) throws IOException
    {
        ProfileResponse playerProfile = fetchPlayerProfile(playerId);
        BufferedImage playerSkin = getPlayerSkin(playerProfile);
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

    public void clearCache()
    {
        synchronized (cacheLock)
        {
            profileCache.clear();
            profileCacheTimestamps.clear();
        }
    }

    private ProfileResponse fetchPlayerProfile(UUID uuid) throws IOException
    {
        if (profileCache.containsKey(uuid))
            return profileCache.get(uuid);

        String response = getApiResponse("https://sessionserver.mojang.com/session/minecraft/profile/"+uuid.toString());
        Gson gson = new Gson();
        ProfileResponse pr = gson.fromJson(response, ProfileResponse.class);
        if (pr.isSuccess())
        {
            synchronized (cacheLock)
            {
                profileCache.put(uuid, pr);
                profileCacheTimestamps.put(uuid, Instant.now().getEpochSecond());
            }
            return pr;
        }
        else
            throw new IOException(String.format("Profile returned error: %s", pr.getError()));
    }

    private String getApiResponse(String query) throws IOException
    {
        URL restRequest = new URL(query);
        InputStreamReader inputStreamReader = new InputStreamReader(restRequest.openStream());
        BufferedReader reader = new BufferedReader((inputStreamReader));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
        {
            responseBuilder.append(line);
        }
        return responseBuilder.toString();
    }

    private BufferedImage getPlayerSkin(ProfileResponse playerProfile) throws IOException
    {
        String encodedTextureData = playerProfile.getPropertyByName("textures").getValue();
        String textureDataJson = new String(Base64.getDecoder().decode(encodedTextureData));
        Gson gson = new Gson();
        ProfileTextureData textureData = gson.fromJson(textureDataJson, ProfileTextureData.class);
        String skinURL = textureData.getTextures().getSkin().getUrl();
        BufferedImage skin = ImageIO.read(URI.create(skinURL).toURL());
        return skin;
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
