package com.zazsona.commemorations.repository;

import com.zazsona.commemorations.image.TemplateImageType;
import com.zazsona.commemorations.database.TemplateGraphic;
import com.zazsona.commemorations.database.TemplateSkinRenderDefinition;
import com.zazsona.commemorations.image.SkinRenderType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;

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
        String sql = "SELECT BackgroundImageBase64, ForegroundImageBase64, LastUpdated FROM TemplateGraphic WHERE TemplateId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, templateId);
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        if (!selectResults.next())
            return null;

        String backgroundImageBase64 = selectResults.getString("BackgroundImageBase64");
        String foregroundImageBase64 = selectResults.getString("ForegroundImageBase64");
        long lastUpdated = selectResults.getLong("LastUpdated");
        TemplateGraphic templateGraphic = new TemplateGraphic(templateId, backgroundImageBase64, foregroundImageBase64, lastUpdated);
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

    public TemplateGraphic addTemplate(String templateId, BufferedImage backgroundImage, BufferedImage foregroundImage, ArrayList<TemplateSkinRenderDefinition> skinDefinitions) throws SQLException, IOException
    {
        TemplateGraphic templateGraphic = addTemplate(templateId, backgroundImage, foregroundImage);
        updateTemplateSkinDefinitions(templateId, skinDefinitions);
        return templateGraphic;
    }

    public TemplateGraphic addTemplate(String templateId, String backgroundImageBase64, String foregroundImageBase64, ArrayList<TemplateSkinRenderDefinition> skinDefinitions) throws SQLException
    {
        TemplateGraphic templateGraphic = addTemplate(templateId, backgroundImageBase64, foregroundImageBase64);
        updateTemplateSkinDefinitions(templateId, skinDefinitions);
        return templateGraphic;
    }

    public TemplateGraphic addTemplate(String templateId, BufferedImage backgroundImage, BufferedImage foregroundImage) throws SQLException, IOException
    {
        String backgroundImageBase64 = "";
        if (backgroundImage != null)
        {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ImageIO.write(backgroundImage, "png", byteStream);
            backgroundImageBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());
        }

        String foregroundImageBase64 = "";
        if (foregroundImage != null)
        {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ImageIO.write(foregroundImage, "png", byteStream);
            foregroundImageBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());
        }
        return addTemplate(templateId, backgroundImageBase64, foregroundImageBase64);
    }

    public TemplateGraphic addTemplate(String templateId, String backgroundImageBase64, String foregroundImageBase64) throws SQLException
    {
        if (doesTemplateExist(templateId))
            throw new IllegalArgumentException("TemplateId already exists!");
        if ((backgroundImageBase64 == null || backgroundImageBase64.equals("")) && (foregroundImageBase64 == null || foregroundImageBase64.equals("")))
            throw new IllegalArgumentException("Both images cannot be null.");
        if (backgroundImageBase64 == null)
            backgroundImageBase64 = "";
        if (foregroundImageBase64 == null)
            foregroundImageBase64 = "";

        String sql = "INSERT INTO TemplateGraphic (TemplateId, BackgroundImageBase64, ForegroundImageBase64, LastUpdated)" +
                                          "VALUES (?,          ?,                     ?,                     ?          );";

        long lastUpdated = Instant.now().getEpochSecond();
        PreparedStatement insertStatement = conn.prepareStatement(sql);
        insertStatement.setString(1, templateId);
        insertStatement.setString(2, backgroundImageBase64);
        insertStatement.setString(3, foregroundImageBase64);
        insertStatement.setLong(4, lastUpdated);
        insertStatement.executeUpdate();

        TemplateGraphic templateGraphic = new TemplateGraphic(templateId, backgroundImageBase64, foregroundImageBase64, lastUpdated);
        return templateGraphic;
    }

    public void updateTemplateGraphic(String templateId, BufferedImage image, TemplateImageType imageType) throws SQLException, IOException
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteStream);
        String imageBase64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());
        updateTemplateGraphic(templateId, imageBase64, imageType);
    }

    public void updateTemplateGraphic(String templateId, String imageBase64, TemplateImageType imageType) throws SQLException
    {
        if (!doesTemplateExist(templateId))
            throw new IllegalArgumentException("TemplateId does not exist.");
        if (imageBase64 == null)
            imageBase64 = "";

        String imageColumn = getImageTypeColumn(imageType);
        String sql = "UPDATE TemplateGraphic      \n" +
                     "SET "+imageColumn+"   = ?   \n" +
                     "   , LastUpdated      = ?   \n" +
                     "WHERE TemplateId = ?;";

        long lastUpdated = Instant.now().getEpochSecond();
        PreparedStatement updateStatement = conn.prepareStatement(sql);
        updateStatement.setString(1, imageBase64);
        updateStatement.setLong(2, lastUpdated);
        updateStatement.setString(3, templateId);
        updateStatement.executeUpdate();
    }

    public void updateTemplateSkinDefinitions(String templateId, ArrayList<TemplateSkinRenderDefinition> skinDefinitions) throws SQLException
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
    }

    private String getImageTypeColumn(TemplateImageType type)
    {
        switch (type)
        {
            case BACKGROUND:
                return "BackgroundImageBase64";
            case FOREGROUND:
                return "ForegroundImageBase64";
            default:
                return null;
        }
    }
}
