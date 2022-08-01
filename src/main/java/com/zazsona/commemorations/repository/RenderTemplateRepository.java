package com.zazsona.commemorations.repository;

import com.zazsona.commemorations.database.RenderedGraphic;
import com.zazsona.commemorations.database.TemplateGraphic;
import com.zazsona.commemorations.database.TemplateSkinRenderDefinition;
import com.zazsona.commemorations.image.GraphicRenderer;
import com.zazsona.commemorations.image.SkinRenderType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

public class RenderTemplateRepository
{
    private Connection conn;

    public RenderTemplateRepository(Connection dbConn)
    {
        this.conn = dbConn;
    }

    public boolean doesTemplateExist(String templateId) throws SQLException
    {
        String sql = "SELECT TemplateId FROM TemplateGraphic WHERE TemplateId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, templateId);
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        return selectResults.next();
    }

    public TemplateGraphic getTemplate(String templateId) throws SQLException, IOException
    {
        String sql = "SELECT ImageBase64, LastUpdated FROM TemplateGraphic WHERE TemplateId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, templateId);
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        if (!selectResults.next())
            return null;

        String imageBase64 = selectResults.getString("ImageBase64");
        long lastUpdated = selectResults.getLong("LastUpdated");
        TemplateGraphic templateGraphic = new TemplateGraphic(templateId, imageBase64, lastUpdated);
        return templateGraphic;
    }

    public ArrayList<TemplateSkinRenderDefinition> getSkinRenderDefinitions(String templateId) throws SQLException
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

    public TemplateGraphic addTemplate(String templateId, BufferedImage image, ArrayList<TemplateSkinRenderDefinition> skinDefinitions) throws SQLException, IOException
    {
        TemplateGraphic templateGraphic = addTemplate(templateId, image);
        updateTemplateSkinDefinitions(templateId, skinDefinitions);
        return templateGraphic;
    }

    public TemplateGraphic addTemplate(String templateId, String imageBase64, ArrayList<TemplateSkinRenderDefinition> skinDefinitions) throws SQLException
    {
        TemplateGraphic templateGraphic = addTemplate(templateId, imageBase64);
        updateTemplateSkinDefinitions(templateId, skinDefinitions);
        return templateGraphic;
    }

    public TemplateGraphic addTemplate(String templateId, BufferedImage image) throws SQLException, IOException
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteStream);
        String imageBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());
        return addTemplate(templateId, imageBase64);
    }

    public TemplateGraphic addTemplate(String templateId, String imageBase64) throws SQLException
    {
        if (!doesTemplateExist(templateId))
            throw new IllegalArgumentException("TemplateId does not exist.");

        String sql = "INSERT INTO TemplateGraphic (TemplateId, ImageBase64, LastUpdated)" +
                                          "VALUES (?,          ?,           ?          );";

        long lastUpdated = Instant.now().getEpochSecond();
        PreparedStatement insertStatement = conn.prepareStatement(sql);
        insertStatement.setString(1, templateId);
        insertStatement.setString(2, imageBase64);
        insertStatement.setLong(3, lastUpdated);
        insertStatement.executeUpdate();

        TemplateGraphic templateGraphic = new TemplateGraphic(templateId, imageBase64, lastUpdated);
        return templateGraphic;
    }

    public TemplateGraphic updateTemplateGraphic(String templateId, BufferedImage image) throws SQLException, IOException
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteStream);
        String imageBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());
        return updateTemplateGraphic(templateId, imageBase64);
    }

    public TemplateGraphic updateTemplateGraphic(String templateId, String imageBase64) throws SQLException
    {
        if (!doesTemplateExist(templateId))
            throw new IllegalArgumentException("TemplateId does not exist.");

        String sql = "UPDATE TemplateGraphic\n" +
                     "SET ImageBase64 = ?   \n" +
                     "  , LastUpdated = ?   \n" +
                     "WHERE TemplateId = ?;";

        long lastUpdated = Instant.now().getEpochSecond();
        PreparedStatement updateStatement = conn.prepareStatement(sql);
        updateStatement.setString(1, imageBase64);
        updateStatement.setLong(2, lastUpdated);
        updateStatement.setString(3, templateId);
        updateStatement.executeUpdate();

        TemplateGraphic templateGraphic = new TemplateGraphic(templateId, imageBase64, lastUpdated);
        return templateGraphic;
    }

    public ArrayList<TemplateSkinRenderDefinition> updateTemplateSkinDefinitions(String templateId, ArrayList<TemplateSkinRenderDefinition> skinDefinitions) throws SQLException
    {
        if (!doesTemplateExist(templateId))
            throw new IllegalArgumentException("TemplateId does not exist.");

        String deleteSql = "DELETE FROM TemplateSkinRenderDefinition WHERE TemplateId = ?;";
        PreparedStatement deleteStatement = conn.prepareStatement(deleteSql);
        deleteStatement.setString(1, templateId);
        deleteStatement.executeUpdate();

        String insertSql = "INSERT INTO TemplateSkinRenderDefinition (TemplateId, SkinRenderType, StartX, StartY, Width, Height) \n" +
                                                             "VALUES (?,          ?,              ?,      ?,      ?,     ?     );";

        for (TemplateSkinRenderDefinition skinDefinition : skinDefinitions)
        {
            PreparedStatement updateStatement = conn.prepareStatement(insertSql);
            updateStatement.setString(1, templateId);
            updateStatement.setString(2, skinDefinition.getSkinRenderType().toString());
            updateStatement.setInt(3, skinDefinition.getStartX());
            updateStatement.setInt(4, skinDefinition.getStartY());
            updateStatement.setInt(5, skinDefinition.getWidth());
            updateStatement.setInt(6, skinDefinition.getHeight());
            updateStatement.executeUpdate();
        }
        return skinDefinitions;
    }
}
