package com.zazsona.commemorations;

import com.zazsona.commemorations.database.RenderedGraphic;
import com.zazsona.commemorations.database.TemplateSkinRenderDefinition;
import com.zazsona.commemorations.image.SkinRenderType;
import com.zazsona.commemorations.repository.RenderRepository;
import com.zazsona.commemorations.repository.RenderTemplateRepository;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.FileUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TemplateDataUpdater
{
    public void loadTemplates(File templatesDirectory, RenderTemplateRepository templateRepository, RenderRepository renderRepository) throws IOException, SQLException
    {
        File[] templateFiles = templatesDirectory.listFiles();
        for (File templateFile : templateFiles)
        {
            String fileExtension = templateFile.getName().substring(templateFile.getName().lastIndexOf(".") + 1);
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
