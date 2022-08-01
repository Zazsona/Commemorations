package com.zazsona.commemorations.repository;

import com.zazsona.commemorations.database.RenderedGraphic;
import com.zazsona.commemorations.image.GraphicRenderer;
import com.zazsona.commemorations.database.TemplateSkinRenderDefinition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

public class RenderRepository
{
    private Connection conn;
    private GraphicRenderer graphicRenderer;
    private RenderTemplateRepository templateRepository;

    public RenderRepository(Connection dbConn, GraphicRenderer graphicRenderer, RenderTemplateRepository templateRepository)
    {
        this.conn = dbConn;
        this.graphicRenderer = graphicRenderer;
        this.templateRepository = templateRepository;
    }

    public RenderedGraphic createRender(String templateId, ArrayList<UUID> featuredPlayers) throws SQLException, IOException
    {
        BufferedImage render = renderGraphic(templateId, featuredPlayers);
        RenderedGraphic renderedGraphic = insertRender(templateId, render, featuredPlayers);
        return renderedGraphic;
    }

    public boolean doesRenderExist(UUID id) throws SQLException
    {
        String sql = "SELECT RenderGuid FROM RenderedGraphic WHERE RenderGuid = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, id.toString());
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        return selectResults.next();
    }

    public RenderedGraphic getRender(UUID renderGuid) throws SQLException
    {
        String sql = "SELECT RenderGuid, TemplateId, ImageBase64, LastUpdated FROM RenderedGraphic WHERE RenderGuid = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, renderGuid.toString());
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        if (!selectResults.next())
            throw new IllegalArgumentException("RenderGuid does not exist.");

        String templateId = selectResults.getString("TemplateId");
        String imageBase64 = selectResults.getString("ImageBase64");
        long lastUpdated = selectResults.getLong("LastUpdated");

        RenderedGraphic renderedGraphic = new RenderedGraphic(renderGuid, templateId, imageBase64, lastUpdated);
        return renderedGraphic;
    }

    public ArrayList<UUID> getRenderPlayerGuids(UUID renderGuid) throws SQLException
    {
        String sql = "SELECT PlayerGuid FROM BrgPlayerToRenderedGraphic WHERE RenderGuid = ? ORDER BY OrderIndex;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, renderGuid.toString());
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();

        ArrayList<UUID> playerGuids = new ArrayList<>();
        while (selectResults.next())
        {
            String columnValue = selectResults.getString("PlayerGuid");
            UUID playerGuid = UUID.fromString(columnValue);
            playerGuids.add(playerGuid);
        }
        return playerGuids;
    }

    public ArrayList<RenderedGraphic> getRendersWithPlayer(UUID playerGuid) throws SQLException
    {
        String sql = "SELECT Rnd.RenderGuid, Rnd.TemplateId, Rnd.ImageBase64, Rnd.LastUpdated         \n" +
                     "FROM RenderedGraphic Rnd                                                        \n" +
                     "INNER JOIN BrgPlayerToRenderedGraphic AS Brg on Rnd.RenderGuid = Brg.RenderGuid \n" +
                     "WHERE Brg.PlayerId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, playerGuid.toString());
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        ArrayList<RenderedGraphic> playerGraphics = new ArrayList<>();
        while (selectResults.next())
        {
            RenderedGraphic graphic = new RenderedGraphic(
                    UUID.fromString(selectResults.getString(1)),
                    selectResults.getString(2),
                    selectResults.getString(3),
                    selectResults.getLong(4)
            );
            playerGraphics.add(graphic);
        }
        return playerGraphics;
    }

    public ArrayList<RenderedGraphic> getRendersFromTemplate(String templateId) throws SQLException
    {
        String sql = "SELECT RenderGuid, TemplateId, ImageBase64, LastUpdated FROM RenderedGraphic WHERE TemplateId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, templateId);
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        ArrayList<RenderedGraphic> templateGraphics = new ArrayList<>();
        while (selectResults.next())
        {
            RenderedGraphic graphic = new RenderedGraphic(
                    UUID.fromString(selectResults.getString(1)),
                    selectResults.getString(2),
                    selectResults.getString(3),
                    selectResults.getLong(4)
            );
            templateGraphics.add(graphic);
        }
        return templateGraphics;
    }

    public RenderedGraphic refreshRender(UUID renderGuid) throws SQLException, IOException
    {
        if (!doesRenderExist(renderGuid))
            throw new IllegalArgumentException("RenderGuid does not exist.");

        RenderedGraphic oldRender = getRender(renderGuid);
        String templateId = oldRender.getTemplateId();
        ArrayList<UUID> featuredPlayers = getRenderPlayerGuids(renderGuid);
        BufferedImage renderedGraphic = renderGraphic(templateId, featuredPlayers);
        RenderedGraphic newRender = updateRender(renderGuid, templateId, renderedGraphic, featuredPlayers);
        return newRender;
    }

    private BufferedImage renderGraphic(String templateId, ArrayList<UUID> featuredPlayers) throws SQLException, IOException
    {
        BufferedImage template = templateRepository.getTemplate(templateId).getImage();
        ArrayList<TemplateSkinRenderDefinition> skinDefinitions = templateRepository.getSkinRenderDefinitions(templateId);
        if (template == null)
            throw new IllegalArgumentException("Template with Id " + templateId + " not found.");

        BufferedImage render = graphicRenderer.renderGraphic(template, skinDefinitions, featuredPlayers);
        return render;
    }

    private RenderedGraphic insertRender(String templateId, BufferedImage render, ArrayList<UUID> featuredPlayers) throws SQLException, IOException
    {
        boolean isAutoCommit = conn.getAutoCommit();
        try
        {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ImageIO.write(render, "png", byteStream);
            String imageBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());

            conn.setAutoCommit(false);

            String renderInsertSql = "INSERT INTO RenderedGraphic (RenderGuid, TemplateId, ImageBase64, LastUpdated)\n" +
                                                          "VALUES (?,          ?,          ?,           ?          )";

            UUID renderGuid = UUID.randomUUID();
            long lastUpdated = Instant.now().getEpochSecond();
            PreparedStatement renderInsert = conn.prepareStatement(renderInsertSql);
            renderInsert.setString(1, renderGuid.toString());
            renderInsert.setString(2, templateId);
            renderInsert.setString(3, imageBase64);
            renderInsert.setLong(4, lastUpdated);
            renderInsert.executeUpdate();

            if (featuredPlayers.size() > 0)
                insertRenderPlayerBridge(templateId, featuredPlayers);

            conn.commit();
            RenderedGraphic renderedGraphic = new RenderedGraphic(renderGuid, templateId, imageBase64, lastUpdated);
            return renderedGraphic;
        }
        catch (SQLException | IOException e)
        {
            conn.rollback();
            throw e;
        }
        finally
        {
            conn.setAutoCommit(isAutoCommit);
        }
    }

    private RenderedGraphic updateRender(UUID renderGuid, String templateId, BufferedImage render, ArrayList<UUID> featuredPlayers) throws SQLException, IOException
    {
        boolean isAutoCommit = conn.getAutoCommit();
        try
        {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ImageIO.write(render, "png", byteStream);
            String imageBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());

            String sql = "UPDATE RenderedGraphic\n" +
                    "SET TemplateId = ?\n" +
                    "  , ImageBase64 = ?\n" +
                    "  , LastUpdated = ?\n" +
                    "WHERE RenderGuid = ?;";

            long lastUpdated = Instant.now().getEpochSecond();
            PreparedStatement renderUpdate = conn.prepareStatement(sql);
            renderUpdate.setString(1, templateId);
            renderUpdate.setString(2, imageBase64);
            renderUpdate.setLong(3, lastUpdated);
            renderUpdate.setString(4, renderGuid.toString());
            renderUpdate.executeUpdate();

            String playerBrgDeleteSql = "DELETE FROM BrgPlayerToRenderedGraphic WHERE RenderGuid = ?;";
            PreparedStatement deleteStatement = conn.prepareStatement(playerBrgDeleteSql);
            deleteStatement.setString(1, renderGuid.toString());
            deleteStatement.executeUpdate();

            if (featuredPlayers.size() > 0)
                insertRenderPlayerBridge(templateId, featuredPlayers);

            conn.commit();
            RenderedGraphic renderedGraphic = new RenderedGraphic(renderGuid, templateId, imageBase64, lastUpdated);
            return renderedGraphic;
        }
        catch (IOException | SQLException e)
        {
            conn.rollback();
            throw e;
        }
        finally
        {
            conn.setAutoCommit(isAutoCommit);
        }
    }

    private void insertRenderPlayerBridge(String templateId, ArrayList<UUID> featuredPlayers) throws SQLException
    {
        String playerBrgInsertSql = "INSERT INTO BrgPlayerToRenderedGraphic (PlayerGuid, RenderGuid, OrderIndex)\n" +
                "VALUES (?,          ?,          ?         )";
        for (int i = 1; i < featuredPlayers.size(); i++)
            playerBrgInsertSql += ", (?, ?, ?)";

        int argNo = 0;
        PreparedStatement playerBrgInsert = conn.prepareStatement(playerBrgInsertSql);
        for (int i = 0; i < featuredPlayers.size(); i++)
        {
            playerBrgInsert.setString(++argNo, featuredPlayers.get(i).toString());
            playerBrgInsert.setString(++argNo, templateId);
            playerBrgInsert.setInt(++argNo, i);
        }
        playerBrgInsert.executeUpdate();
    }
}
