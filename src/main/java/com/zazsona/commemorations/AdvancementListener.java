package com.zazsona.commemorations;

import com.zazsona.commemorations.repository.RenderRepository;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class AdvancementListener implements Listener
{
    private RenderRepository renderRepository;

    public AdvancementListener(RenderRepository renderRepository)
    {
        this.renderRepository = renderRepository;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAdvancementDone(PlayerAdvancementDoneEvent e)
    {
        try
        {
            Player player = e.getPlayer();
            Advancement advancement = e.getAdvancement();
            NamespacedKey key = advancement.getKey();
            ArrayList<UUID> featuredPlayers = new ArrayList<>();
            featuredPlayers.add(player.getUniqueId());

            if (key.getNamespace().equalsIgnoreCase("minecraft"))
            {
                switch (key.getKey())
                {
                    case "husbandry/plant_seed":
                        renderRepository.createRender("wanted", featuredPlayers);
                }
            }
        }
        catch (IOException | SQLException ex)
        {
            CommemorationsPlugin.getInstance().getLogger().severe("Unable to create sign: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


}
