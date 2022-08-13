package com.zazsona.commemorations;

import com.zazsona.commemorations.database.RenderedGraphic;
import com.zazsona.commemorations.database.TemplateSkinRenderDefinition;
import com.zazsona.commemorations.image.SkinRenderType;
import com.zazsona.commemorations.image.TemplateImageType;
import com.zazsona.commemorations.repository.RenderRepository;
import com.zazsona.commemorations.repository.RenderTemplateRepository;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TemplateDataUpdater
{
    public void loadTemplates(File templatesDirectory, RenderTemplateRepository templateRepository, RenderRepository renderRepository) throws IOException, SQLException, InvalidConfigurationException
    {
        File[] templateFiles = templatesDirectory.listFiles();
        for (File templateFile : templateFiles)
        {
            String fileExtension = templateFile.getName().substring(templateFile.getName().lastIndexOf(".") + 1);
            if (!fileExtension.equalsIgnoreCase("yml"))
                continue;

            // Parse YAML
            YamlConfiguration templateYaml = YamlConfiguration.loadConfiguration(templateFile);

            String templateId = templateYaml.getString("id");
            String bgGraphicFileName = templateYaml.getString("backgroundGraphic");
            String fgGraphicFileName = templateYaml.getString("foregroundGraphic");
            File bgGraphicFile = (!bgGraphicFileName.equals("")) ? new File(templatesDirectory.getAbsolutePath() + "/" + bgGraphicFileName) : null;
            File fgGraphicFile = (!fgGraphicFileName.equals("")) ? new File(templatesDirectory.getAbsolutePath() + "/" + fgGraphicFileName) : null;

            if (bgGraphicFile == null && fgGraphicFile == null)
                throw new InvalidConfigurationException("Invalid Template: " + templateFile.getName() +" must specify a graphic for the Background and/or the Foreground.");

            // Get Modified Times
            boolean templateExists = templateRepository.doesTemplateExist(templateId);
            long metaModifiedUnixTime = Files.getLastModifiedTime(templateFile.toPath()).toMillis() / 1000;
            long bgGraphicModifiedUnixTime = (bgGraphicFile != null) ? Files.getLastModifiedTime(bgGraphicFile.toPath()).toMillis() / 1000 : 0;
            long fgGraphicModifiedUnixTime = (fgGraphicFile != null) ? Files.getLastModifiedTime(fgGraphicFile.toPath()).toMillis() / 1000 : 0;
            long existingTemplateModifiedUnixTime = (templateExists) ? templateRepository.getTemplate(templateId).getLastUpdated() : 0;

            // Update Database
            if (!templateExists || metaModifiedUnixTime > existingTemplateModifiedUnixTime || bgGraphicModifiedUnixTime > existingTemplateModifiedUnixTime || fgGraphicModifiedUnixTime > existingTemplateModifiedUnixTime)
            {
                BufferedImage bgGraphic = (bgGraphicFile != null) ? ImageIO.read(bgGraphicFile) : null;
                BufferedImage fgGraphic = (fgGraphicFile != null) ? ImageIO.read(fgGraphicFile) : null;
                List<Map<?, ?>> skinDefinitionMaps = templateYaml.getMapList("skinDefinitions");
                ArrayList<TemplateSkinRenderDefinition> skinDefinitions = new ArrayList<>();
                for (Map<?, ?> skinDefinitionMap : skinDefinitionMaps)
                {
                    SkinRenderType renderType = SkinRenderType.valueOf((String) skinDefinitionMap.get("skinRenderType"));
                    int startX = (int) skinDefinitionMap.get("startX");
                    int startY = (int) skinDefinitionMap.get("startY");
                    int width = (int) skinDefinitionMap.get("width");
                    int height = (int) skinDefinitionMap.get("height");

                    TemplateSkinRenderDefinition skinDefinition = new TemplateSkinRenderDefinition(templateId, renderType, startX, startY, width, height);
                    skinDefinitions.add(skinDefinition);
                }

                if (templateExists)
                {
                    templateRepository.updateTemplateGraphic(templateId, bgGraphic, TemplateImageType.BACKGROUND);
                    templateRepository.updateTemplateGraphic(templateId, fgGraphic, TemplateImageType.FOREGROUND);
                    templateRepository.updateTemplateSkinDefinitions(templateId, skinDefinitions);
                    ArrayList<RenderedGraphic> renders = renderRepository.getRendersFromTemplate(templateId);
                    for (RenderedGraphic render : renders)
                        renderRepository.refreshRender(render.getRenderGuid());
                }
                else
                    templateRepository.addTemplate(templateId, bgGraphic, fgGraphic, skinDefinitions);
            }
        }
    }

    public void extractTemplateResourcesFromJar(File templatesDirectory, SemanticVersion previousVersion, SemanticVersion currentVersion) throws IOException, URISyntaxException
    {
        if (previousVersion.compareTo(currentVersion) >= 0)
            return;

        String internalTemplatesDirName = "templates";
        String templatesBackupDirName = String.format("V%s", previousVersion);
        Path externalTemplatesDirPath = templatesDirectory.toPath();
        Path templatesBackupDirPath = Paths.get(externalTemplatesDirPath.toString(), templatesBackupDirName);
        File templatesBackupDir = templatesBackupDirPath.toFile();

        ArrayList<Path> internalTemplatePathList = new ArrayList<>();
        URI uri = this.getClass().getClassLoader().getResource(internalTemplatesDirName).toURI();
        FileSystem templatesFileSys = FileSystems.newFileSystem(uri, Collections.emptyMap());
        Files.walk(templatesFileSys.getPath("/" + internalTemplatesDirName + "/")).forEach(path ->
                                                                       {
                                                                           if (!Files.isDirectory(path))
                                                                               internalTemplatePathList.add(path);
                                                                       });
        templatesFileSys.close();

        for (Path path : internalTemplatePathList)
        {
            String fileName = path.getFileName().toString();
            Path externalFilePath = Paths.get(externalTemplatesDirPath.toString(), fileName);
            File externalFile = externalFilePath.toFile();
            if (externalFile.exists())
            {
                if (!templatesBackupDir.exists())
                    templatesBackupDir.mkdirs();
                Files.move(externalFilePath, Paths.get(templatesBackupDirPath.toString(), fileName), StandardCopyOption.REPLACE_EXISTING);
            }


            String resourceName = path.toString().substring(1); // Remove initial /
            exportResourceFile(resourceName, externalFile);
        }
    }

    private void exportResourceFile(String resourceFile, File outputFile) throws IOException
    {
        if (!outputFile.exists())
        {
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        }

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceFile);
        OutputStream outputStream = new FileOutputStream(outputFile);
        byte[] buffer = new byte[8 * 1024];
        int providedBytes = inputStream.read(buffer);
        while (providedBytes != -1)
        {
            outputStream.write(buffer, 0, providedBytes);
            providedBytes = inputStream.read(buffer);
        }
    }
}
