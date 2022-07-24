package com.zazsona.commemorations.repository;

import com.zazsona.commemorations.database.CommemorationsPlayer;
import com.zazsona.commemorations.image.SkinRenderType;
import com.zazsona.commemorations.image.SkinRenderer;
import org.bukkit.OfflinePlayer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class CommemorationsPlayerRepository
{
    private Connection conn;
    private SkinRenderer skinRenderer;

    public CommemorationsPlayerRepository(Connection dbConn, SkinRenderer skinRenderer)
    {
        this.conn = dbConn;
        this.skinRenderer = skinRenderer;
    }

    public CommemorationsPlayer getPlayer(UUID playerId) throws SQLException
    {
        String sql = "SELECT PlayerId, Username, SkinBase64, LastUpdated FROM Player WHERE PlayerId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, playerId.toString());
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        if (!selectResults.next())
            throw new IllegalArgumentException("PlayerId does not exist.");

        String username = selectResults.getString("Username");
        String skinBase64 = selectResults.getString("SkinBase64");
        long lastUpdated = selectResults.getLong("LastUpdated");

        CommemorationsPlayer player = new CommemorationsPlayer(playerId, username, skinBase64, lastUpdated);
        return player;
    }

    public CommemorationsPlayer registerPlayer(OfflinePlayer player) throws SQLException, IOException
    {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        long lastUpdated = Instant.now().getEpochSecond();
        BufferedImage skin = skinRenderer.renderSkin(playerId, SkinRenderType.TEXTURE);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(skin, "png", byteStream);
        String skinBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());

        String sql = "";
        if (isPlayerRegistered(playerId))
        {
            sql = "UPDATE Player\n" +
                  "SET Username = ? \n" +
                  "  , SkinBase64 = ? \n" +
                  "  , LastUpdated = ? \n" +
                  "WHERE PlayerId = ?;";
            // TODO: Update any linked renders
        }
        else
        {
            sql = "INSERT INTO Player (Username, SkinBase64, LastUpdated, PlayerId)" +
                  "VALUES (?, ?, ?, ?);";
        }

        PreparedStatement playerStatement = conn.prepareStatement(sql);
        playerStatement.setString(1, playerName);
        playerStatement.setString(2, skinBase64);
        playerStatement.setLong(3, lastUpdated);
        playerStatement.setString(4, playerId.toString());
        playerStatement.executeUpdate();

        CommemorationsPlayer commemorationsPlayer = new CommemorationsPlayer(playerId, playerName, skinBase64, lastUpdated);
        return commemorationsPlayer;
    }

    public boolean isPlayerRegistered(UUID playerId) throws SQLException
    {
        String sql = "SELECT PlayerId FROM Player WHERE PlayerId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, playerId.toString());
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        return selectResults.next();
    }
}
