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

    public GraphicRepository(Connection dbConn)
    {
        this.conn = dbConn;
        this.graphicRenderer = new GraphicRenderer();
    }

    public RenderedGraphic createRender(UUID templateId, ArrayList<UUID> featuredPlayers) throws SQLException, IOException
    {
        BufferedImage render = createNewRender(templateId, featuredPlayers);
        RenderedGraphic renderedGraphic = saveRender(templateId, render);
        return renderedGraphic;
    }

    public RenderedGraphic getRender(UUID renderId) throws SQLException, IOException
    {
        return getCachedRender(renderId);
    }

    public boolean doesRenderExist(UUID id) throws SQLException
    {
        String sql = "SELECT RenderId FROM RenderedGraphic WHERE RenderId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        selectStatement.setString(1, id.toString());
        selectStatement.executeUpdate();
        ResultSet selectResults = selectStatement.getResultSet();
        return selectResults.next();
    }

    private RenderedGraphic getCachedRender(UUID renderId) throws SQLException, IOException
    {
        String sql = "SELECT RenderId, TemplateId, ImageBase64, LastUpdated FROM RenderedGraphic WHERE RenderId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        selectStatement.setString(1, renderId.toString());
        selectStatement.executeUpdate();
        ResultSet selectResults = selectStatement.getResultSet();
        if (!selectResults.next())
            throw new IllegalArgumentException("RenderId does not exist.");

        UUID templateId = UUID.fromString(selectResults.getString("TemplateId"));
        String imageBase64 = selectResults.getString("ImageBase64");
        long lastUpdated = selectResults.getLong("LastUpdated");

        RenderedGraphic renderedGraphic = new RenderedGraphic(renderId, templateId, imageBase64, lastUpdated);
        return renderedGraphic;
    }

    private BufferedImage getTemplate(UUID templateId) throws SQLException, IOException
    {
        String sql = "SELECT ImageBase64 FROM TemplateGraphic WHERE TemplateId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        selectStatement.setString(1, templateId.toString());
        selectStatement.executeUpdate();
        ResultSet selectResults = selectStatement.getResultSet();
        if (!selectResults.next())
            return null;

        String imageBase64 = selectResults.getString(0);
        byte[] decodedBytes = Base64.getDecoder().decode(imageBase64);
        BufferedImage templateImage = ImageIO.read(new ByteArrayInputStream(decodedBytes));
        return templateImage;
    }

    private ArrayList<TemplateSkinRenderDefinition> getSkinRenderDefinitions(UUID templateId) throws SQLException, IOException
    {
        String sql = "SELECT SkinRenderType, StartX, StartY, Width, Height FROM TemplateSkinRenderDefinition WHERE TemplateId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        selectStatement.setString(1, templateId.toString());
        selectStatement.executeUpdate();
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

    private BufferedImage createNewRender(UUID templateId, ArrayList<UUID> featuredPlayers) throws SQLException, IOException
    {
        BufferedImage template = getTemplate(templateId);
        ArrayList<TemplateSkinRenderDefinition> skinDefinitions = getSkinRenderDefinitions(templateId);
        if (template == null)
            throw new IllegalArgumentException("Template with Id " + templateId + " not found.");

        BufferedImage render = graphicRenderer.renderGraphic(template, skinDefinitions, featuredPlayers);
        return render;
    }

    private RenderedGraphic saveRender(UUID templateId, BufferedImage render) throws SQLException, IOException
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(render, "png", byteStream);
        String imageBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());

        String sql = "INSERT INTO RenderedGraphic (RenderId, TemplateId, ImageBase64, LastUpdated)\n" +
                "VALUES (?, ?, ?, ?)";

        UUID renderId = UUID.randomUUID();
        long lastUpdated = Instant.now().getEpochSecond();
        PreparedStatement insertStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        insertStatement.setString(1, renderId.toString());
        insertStatement.setString(2, templateId.toString());
        insertStatement.setString(3, imageBase64);
        insertStatement.setLong(4, lastUpdated);
        insertStatement.executeUpdate();

        RenderedGraphic renderedGraphic = new RenderedGraphic(renderId, templateId, imageBase64, lastUpdated);
        return renderedGraphic;
    }
}
