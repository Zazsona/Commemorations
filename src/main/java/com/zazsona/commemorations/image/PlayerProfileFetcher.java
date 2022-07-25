package com.zazsona.commemorations.image;

import com.google.gson.Gson;
import com.zazsona.commemorations.apiresponse.ProfileResponse;
import com.zazsona.commemorations.apiresponse.ProfileTextureData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.*;

public class PlayerProfileFetcher
{
    private HashMap<UUID, ProfileResponse> profileCache;
    private HashMap<UUID, Long> profileCacheTimestamps;
    private Object cacheLock;
    private Timer cacheCleanUpTaskTimer;

    public PlayerProfileFetcher()
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

    public BufferedImage fetchPlayerSkin(UUID playerGuid) throws IOException
    {
        ProfileResponse pr = fetchPlayerProfile(playerGuid);
        BufferedImage skin = getPlayerSkin(pr);
        return skin;
    }

    public BufferedImage getPlayerSkin(ProfileResponse playerProfile) throws IOException
    {
        String encodedTextureData = playerProfile.getPropertyByName("textures").getValue();
        String textureDataJson = new String(Base64.getDecoder().decode(encodedTextureData));
        Gson gson = new Gson();
        ProfileTextureData textureData = gson.fromJson(textureDataJson, ProfileTextureData.class);
        String skinURL = textureData.getTextures().getSkin().getUrl();
        BufferedImage skin = ImageIO.read(URI.create(skinURL).toURL());
        return skin;
    }

    public ProfileResponse fetchPlayerProfile(UUID playerGuid) throws IOException
    {
        if (profileCache.containsKey(playerGuid))
            return profileCache.get(playerGuid);

        String response = getApiResponse("https://sessionserver.mojang.com/session/minecraft/profile/"+playerGuid.toString());
        Gson gson = new Gson();
        ProfileResponse pr = gson.fromJson(response, ProfileResponse.class);
        if (pr.isSuccess())
        {
            synchronized (cacheLock)
            {
                profileCache.put(playerGuid, pr);
                profileCacheTimestamps.put(playerGuid, Instant.now().getEpochSecond());
            }
            return pr;
        }
        else
            throw new IOException(String.format("Profile returned error: %s", pr.getError()));
    }

    public void clearCache()
    {
        synchronized (cacheLock)
        {
            profileCache.clear();
            profileCacheTimestamps.clear();
        }
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
}
