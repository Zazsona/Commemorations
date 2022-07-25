package com.zazsona.commemorations.repository;

import com.zazsona.commemorations.CommemorationsPlugin;
import com.zazsona.commemorations.apiresponse.ProfileResponse;
import com.zazsona.commemorations.database.CommemorationsPlayer;
import com.zazsona.commemorations.image.PlayerProfileFetcher;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

public class CommemorationsPlayerRepository
{
    private Connection conn;
    private PlayerProfileFetcher profileFetcher;

    public CommemorationsPlayerRepository(Connection dbConn, PlayerProfileFetcher profileFetcher)
    {
        this.conn = dbConn;
        this.profileFetcher = profileFetcher;
    }

    public CommemorationsPlayer getPlayer(UUID playerGuid) throws SQLException
    {
        String sql = "SELECT PlayerGuid, Username, SkinBase64, LastUpdated FROM Player WHERE PlayerGuid = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, playerGuid.toString());
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        if (!selectResults.next())
            throw new IllegalArgumentException("PlayerGuid does not exist.");

        String username = selectResults.getString("Username");
        String skinBase64 = selectResults.getString("SkinBase64");
        long lastUpdated = selectResults.getLong("LastUpdated");

        CommemorationsPlayer player = new CommemorationsPlayer(playerGuid, username, skinBase64, lastUpdated);
        return player;
    }

    public CommemorationsPlayer registerPlayer(UUID playerGuid) throws SQLException, IOException
    {
        ProfileResponse playerProfile = profileFetcher.fetchPlayerProfile(playerGuid);
        String playerName = playerProfile.getName();
        long lastUpdated = Instant.now().getEpochSecond();
        BufferedImage skin = profileFetcher.fetchPlayerSkin(playerGuid);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(skin, "png", byteStream);
        String skinBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());

        boolean isNewPlayer = isPlayerRegistered(playerGuid);
        String playerSql = "";
        if (!isNewPlayer)
        {
            playerSql = "UPDATE Player\n" +
                        "SET Username = ? \n" +
                        "  , SkinBase64 = ? \n" +
                        "  , LastUpdated = ? \n" +
                        "WHERE PlayerGuid = ?;";
        }
        else
        {
            playerSql = "INSERT INTO Player (Username, SkinBase64, LastUpdated, PlayerGuid)" +
                        "VALUES (?, ?, ?, ?);";
        }

        PreparedStatement playerStatement = conn.prepareStatement(playerSql);
        playerStatement.setString(1, playerName);
        playerStatement.setString(2, skinBase64);
        playerStatement.setLong(3, lastUpdated);
        playerStatement.setString(4, playerGuid.toString());
        playerStatement.executeUpdate();

        if (!isNewPlayer)
            updatePlayerRenders(playerGuid);

        CommemorationsPlayer commemorationsPlayer = new CommemorationsPlayer(playerGuid, playerName, skinBase64, lastUpdated);
        return commemorationsPlayer;
    }

    private void updatePlayerRenders(UUID playerGuid) throws SQLException, IOException
    {
        GraphicRepository graphicRepository = CommemorationsPlugin.getInstance().getGraphicRepository();
        String sql = "SELECT RenderGuid FROM BrgPlayerToRenderedGraphic WHERE PlayerGuid = ?;";
        PreparedStatement renderStatement = conn.prepareStatement(sql);
        renderStatement.setString(1, playerGuid.toString());
        renderStatement.execute();
        ResultSet selectResults = renderStatement.getResultSet();
        ArrayList<UUID> renderGuids = new ArrayList<>();
        while (selectResults.next())
            renderGuids.add(UUID.fromString(selectResults.getString("RenderGuid")));

        for (UUID renderGuid : renderGuids)
            graphicRepository.updateRender(renderGuid);
    }

    public boolean isPlayerRegistered(UUID playerGuid) throws SQLException
    {
        String sql = "SELECT PlayerGuid FROM Player WHERE PlayerGuid = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, playerGuid.toString());
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        return selectResults.next();
    }
}
