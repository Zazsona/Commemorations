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

    public boolean isPlayerRegistered(UUID playerGuid) throws SQLException
    {
        String sql = "SELECT PlayerGuid FROM Player WHERE PlayerGuid = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, playerGuid.toString());
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        return selectResults.next();
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

    public CommemorationsPlayer addPlayer(UUID playerGuid, String username, String skinBase64) throws SQLException
    {
        String sql = "INSERT INTO Player (Username, SkinBase64, LastUpdated, PlayerGuid)" +
                "VALUES (?, ?, ?, ?);";

        long lastUpdated = Instant.now().getEpochSecond();
        PreparedStatement insertStatement = conn.prepareStatement(sql);
        insertStatement.setString(1, username);
        insertStatement.setString(2, skinBase64);
        insertStatement.setLong(3, lastUpdated);
        insertStatement.setString(4, playerGuid.toString());
        insertStatement.executeUpdate();

        CommemorationsPlayer commemorationsPlayer = new CommemorationsPlayer(playerGuid, username, skinBase64, lastUpdated);
        return commemorationsPlayer;
    }

    public CommemorationsPlayer updatePlayer(UUID playerGuid, String username, String skinBase64) throws SQLException
    {
        String sql = "UPDATE Player\n" +
                     "SET Username = ?     \n" +
                     "  , SkinBase64 = ?   \n" +
                     "  , LastUpdated = ?  \n" +
                     "WHERE PlayerGuid = ?;  ";

        long lastUpdated = Instant.now().getEpochSecond();
        PreparedStatement insertStatement = conn.prepareStatement(sql);
        insertStatement.setString(1, username);
        insertStatement.setString(2, skinBase64);
        insertStatement.setLong(3, lastUpdated);
        insertStatement.setString(4, playerGuid.toString());
        insertStatement.executeUpdate();

        CommemorationsPlayer commemorationsPlayer = new CommemorationsPlayer(playerGuid, username, skinBase64, lastUpdated);
        return commemorationsPlayer;
    }
}
