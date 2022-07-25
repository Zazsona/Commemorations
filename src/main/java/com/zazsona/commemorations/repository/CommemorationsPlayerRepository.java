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

    public CommemorationsPlayer registerPlayer(UUID playerId) throws SQLException, IOException
    {
        ProfileResponse playerProfile = profileFetcher.fetchPlayerProfile(playerId);
        String playerName = playerProfile.getName();
        long lastUpdated = Instant.now().getEpochSecond();
        BufferedImage skin = profileFetcher.fetchPlayerSkin(playerId);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(skin, "png", byteStream);
        String skinBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());

        boolean isNewPlayer = isPlayerRegistered(playerId);
        String playerSql = "";
        if (!isNewPlayer)
        {
            playerSql = "UPDATE Player\n" +
                        "SET Username = ? \n" +
                        "  , SkinBase64 = ? \n" +
                        "  , LastUpdated = ? \n" +
                        "WHERE PlayerId = ?;";
        }
        else
        {
            playerSql = "INSERT INTO Player (Username, SkinBase64, LastUpdated, PlayerId)" +
                        "VALUES (?, ?, ?, ?);";
        }

        PreparedStatement playerStatement = conn.prepareStatement(playerSql);
        playerStatement.setString(1, playerName);
        playerStatement.setString(2, skinBase64);
        playerStatement.setLong(3, lastUpdated);
        playerStatement.setString(4, playerId.toString());
        playerStatement.executeUpdate();

        if (!isNewPlayer)
            updatePlayerRenders(playerId);

        CommemorationsPlayer commemorationsPlayer = new CommemorationsPlayer(playerId, playerName, skinBase64, lastUpdated);
        return commemorationsPlayer;
    }

    private void updatePlayerRenders(UUID playerId) throws SQLException, IOException
    {
        GraphicRepository graphicRepository = CommemorationsPlugin.getInstance().getGraphicRepository();
        String renderIdSql = "SELECT RenderId FROM BrgPlayerToRenderedGraphic WHERE PlayerId = ?;";
        PreparedStatement renderStatement = conn.prepareStatement(renderIdSql);
        renderStatement.setString(1, playerId.toString());
        renderStatement.execute();
        ResultSet renderIdResults = renderStatement.getResultSet();
        ArrayList<UUID> renderIds = new ArrayList<>();
        while (renderIdResults.next())
            renderIds.add(UUID.fromString(renderIdResults.getString("RenderId")));

        for (UUID renderId : renderIds)
            graphicRepository.updateRender(renderId);
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
