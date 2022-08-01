package com.zazsona.commemorations;

import com.zazsona.commemorations.database.RenderedGraphic;
import com.zazsona.commemorations.database.TemplateSkinRenderDefinition;
import com.zazsona.commemorations.image.SkinRenderType;
import com.zazsona.commemorations.repository.RenderRepository;
import com.zazsona.commemorations.repository.RenderTemplateRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TemplateDataUpdater
{
    public void loadTemplates(File templatesDirectory, RenderTemplateRepository templateRepository, RenderRepository renderRepository) throws IOException, SQLException
    {
        File[] templateFiles = templatesDirectory.listFiles();
        for (File templateFile : templateFiles)
        {
            String fileExtension = templateFile.getName().substring(templateFile.getName().lastIndexOf("[.]"));
            if (!fileExtension.equalsIgnoreCase("yml"))
                continue;

            YamlConfiguration templateYaml = YamlConfiguration.loadConfiguration(templateFile);

            String templateId = templateYaml.getString("id");
            String graphicFileName = templateYaml.getString("graphic");
            File graphicFile = new File(templatesDirectory.getAbsolutePath() + "/" + graphicFileName);

            FileTime metaModifiedTime = Files.getLastModifiedTime(templateFile.toPath());
            long metaModifiedUnixTime = metaModifiedTime.toMillis() / 1000;
            FileTime graphicModifiedTime = Files.getLastModifiedTime(graphicFile.toPath());
            long graphicModifiedUnixTime = graphicModifiedTime.toMillis() / 1000;
            boolean templateExists = templateRepository.doesTemplateExist(templateId);
            long existingTemplateModifiedUnixTime = (templateExists) ? templateRepository.getTemplate(templateId).getLastUpdated() : 0;

            if (!templateExists || metaModifiedUnixTime > existingTemplateModifiedUnixTime || graphicModifiedUnixTime > existingTemplateModifiedUnixTime)
            {
                BufferedImage graphic = ImageIO.read(graphicFile);
                List<ConfigurationSection> skinDefinitionYamls = (List<ConfigurationSection>) templateYaml.getList("skinDefinitions");
                ArrayList<TemplateSkinRenderDefinition> skinDefinitions = new ArrayList<>();
                for (ConfigurationSection skinDefinitionYaml : skinDefinitionYamls)
                {
                    SkinRenderType renderType = SkinRenderType.valueOf(skinDefinitionYaml.getString("skinRenderType"));
                    int startX = skinDefinitionYaml.getInt("startX");
                    int startY = skinDefinitionYaml.getInt("startY");
                    int width = skinDefinitionYaml.getInt("width");
                    int height = skinDefinitionYaml.getInt("height");

                    TemplateSkinRenderDefinition skinDefinition = new TemplateSkinRenderDefinition(templateId, renderType, startX, startY, width, height);
                    skinDefinitions.add(skinDefinition);
                }

                if (templateExists)
                {
                    templateRepository.updateTemplateGraphic(templateId, graphic);
                    templateRepository.updateTemplateSkinDefinitions(templateId, skinDefinitions);
                    ArrayList<RenderedGraphic> renders = renderRepository.getRendersFromTemplate(templateId);
                    for (RenderedGraphic render : renders)
                        renderRepository.refreshRender(render.getRenderGuid());
                }
                else
                {
                    templateRepository.addTemplate(templateId, graphic);
                    templateRepository.updateTemplateSkinDefinitions(templateId, skinDefinitions);
                }
            }
        }
    }
}
