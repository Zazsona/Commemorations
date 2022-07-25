package com.zazsona.commemorations.repository;

import com.zazsona.commemorations.database.RenderedGraphic;
import com.zazsona.commemorations.image.GraphicRenderer;
import com.zazsona.commemorations.image.SkinRenderType;
import com.zazsona.commemorations.database.TemplateSkinRenderDefinition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

public class GraphicRepository
{
    private Connection conn;
    private GraphicRenderer graphicRenderer;

    public GraphicRepository(Connection dbConn, GraphicRenderer graphicRenderer)
    {
        this.conn = dbConn;
        this.graphicRenderer = graphicRenderer;
    }

    public RenderedGraphic createRender(String templateId, ArrayList<UUID> featuredPlayers) throws SQLException, IOException
    {
        BufferedImage render = createNewRender(templateId, featuredPlayers);
        RenderedGraphic renderedGraphic = insertRender(templateId, render);
        return renderedGraphic;
    }

    public RenderedGraphic updateRender(UUID renderGuid) throws SQLException, IOException
    {
        if (!doesRenderExist(renderGuid))
            throw new IllegalArgumentException("RenderGuid does not exist.");

        RenderedGraphic oldRender = getRender(renderGuid);
        String templateId = oldRender.getTemplateId();
        ArrayList<UUID> featuredPlayers = getExistingRenderPlayerGuids(renderGuid);
        BufferedImage renderedGraphic = createNewRender(templateId, featuredPlayers);
        RenderedGraphic newRender = updateRender(renderGuid, templateId, renderedGraphic);
        return newRender;
    }

    public RenderedGraphic getRender(UUID renderGuid) throws SQLException, IOException
    {
        return getExistingRender(renderGuid);
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

    private RenderedGraphic getExistingRender(UUID renderGuid) throws SQLException
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

    private ArrayList<UUID> getExistingRenderPlayerGuids(UUID renderGuid) throws SQLException
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

    private BufferedImage getTemplate(String templateId) throws SQLException, IOException
    {
        String sql = "SELECT ImageBase64 FROM TemplateGraphic WHERE TemplateId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, templateId);
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        if (!selectResults.next())
            return null;

        String imageBase64 = selectResults.getString(0);
        byte[] decodedBytes = Base64.getDecoder().decode(imageBase64);
        BufferedImage templateImage = ImageIO.read(new ByteArrayInputStream(decodedBytes));
        return templateImage;
    }

    private ArrayList<TemplateSkinRenderDefinition> getSkinRenderDefinitions(String templateId) throws SQLException
    {
        String sql = "SELECT SkinRenderType, StartX, StartY, Width, Height FROM TemplateSkinRenderDefinition WHERE TemplateId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, templateId);
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();

        ArrayList<TemplateSkinRenderDefinition> definitions = new ArrayList<>();
        while (selectResults.next())
        {
            SkinRenderType renderType = SkinRenderType.valueOf(selectResults.getString("SkinRenderType"));
            int startX = selectResults.getInt("StartX");
            int startY = selectResults.getInt("StartY");
            int width = selectResults.getInt("Width");
            int height = selectResults.getInt("Height");
            TemplateSkinRenderDefinition definition = new TemplateSkinRenderDefinition(templateId, renderType, startX, startY, width, height);
            definitions.add(definition);
        }
        return definitions;
    }

    private BufferedImage createNewRender(String templateId, ArrayList<UUID> featuredPlayers) throws SQLException, IOException
    {
        BufferedImage template = getTemplate(templateId);
        ArrayList<TemplateSkinRenderDefinition> skinDefinitions = getSkinRenderDefinitions(templateId);
        if (template == null)
            throw new IllegalArgumentException("Template with Id " + templateId + " not found.");

        BufferedImage render = graphicRenderer.renderGraphic(template, skinDefinitions, featuredPlayers);
        return render;
    }

    private RenderedGraphic insertRender(String templateId, BufferedImage render) throws SQLException, IOException
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(render, "png", byteStream);
        String imageBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());

        String sql = "INSERT INTO RenderedGraphic (RenderGuid, TemplateId, ImageBase64, LastUpdated)\n" +
                     "VALUES (?, ?, ?, ?)";

        UUID renderGuid = UUID.randomUUID();
        long lastUpdated = Instant.now().getEpochSecond();
        PreparedStatement renderInsert = conn.prepareStatement(sql);
        renderInsert.setString(1, renderGuid.toString());
        renderInsert.setString(2, templateId);
        renderInsert.setString(3, imageBase64);
        renderInsert.setLong(4, lastUpdated);
        renderInsert.executeUpdate();

        RenderedGraphic renderedGraphic = new RenderedGraphic(renderGuid, templateId, imageBase64, lastUpdated);
        return renderedGraphic;
    }

    private RenderedGraphic updateRender(UUID renderGuid, String templateId, BufferedImage render) throws SQLException, IOException
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

        RenderedGraphic renderedGraphic = new RenderedGraphic(renderGuid, templateId, imageBase64, lastUpdated);
        return renderedGraphic;
    }
}
