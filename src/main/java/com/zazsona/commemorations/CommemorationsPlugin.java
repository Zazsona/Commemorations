package com.zazsona.commemorations;

import com.zazsona.commemorations.blockbuild.CommemorationSignBlockSchematic;
import com.zazsona.commemorations.blockbuild.CommemorationSignBlockSchematicBuilder;
import com.zazsona.commemorations.blockbuild.SchematicLocationSearcher;
import com.zazsona.commemorations.config.PluginConfig;
import com.zazsona.commemorations.database.DatabaseChangeManager;
import com.zazsona.commemorations.image.GraphicRenderer;
import com.zazsona.commemorations.image.PlayerProfileFetcher;
import com.zazsona.commemorations.image.SkinRenderer;
import com.zazsona.commemorations.repository.CommemorationsPlayerRepository;
import com.zazsona.commemorations.repository.RenderRepository;
import com.zazsona.commemorations.repository.RenderTemplateRepository;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.sql.*;

public class CommemorationsPlugin extends JavaPlugin
{
    final static String TEMPLATES_DIRECTORY = "templates";

    private static String pluginName;
    private Connection conn;

    private RenderTemplateRepository renderTemplateRepository;
    private RenderRepository renderRepository;
    private CommemorationsPlayerRepository playerRepository;

    private PlayerProfileFetcher profileFetcher;
    private SkinRenderer skinRenderer;
    private GraphicRenderer graphicRenderer;

    public static CommemorationsPlugin getInstance()
    {
        if (pluginName != null)
            return (CommemorationsPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        else
            throw new NullPointerException("The plugin has not yet initialised.");
    }

    public RenderTemplateRepository getRenderTemplateRepository()
    {
        return renderTemplateRepository;
    }
    public RenderRepository getRenderRepository()
    {
        return renderRepository;
    }

    public CommemorationsPlayerRepository getPlayerRepository()
    {
        return playerRepository;
    }

    @Override
    public void onLoad()
    {
        try
        {
            super.onLoad();
            pluginName = getDescription().getName();
            conn = connectToCommemorationsDatabase();
            DatabaseChangeManager dbcm = new DatabaseChangeManager(conn);
            SemanticVersion existingVersion = dbcm.getDatabaseVersion();
            dbcm.upgradeDatabase();
            SemanticVersion currentVersion = dbcm.getDatabaseVersion();

            profileFetcher = new PlayerProfileFetcher();
            skinRenderer = new SkinRenderer();
            graphicRenderer = new GraphicRenderer(skinRenderer);

            renderTemplateRepository = new RenderTemplateRepository(conn);
            renderRepository = new RenderRepository(conn, graphicRenderer, renderTemplateRepository);
            playerRepository = new CommemorationsPlayerRepository(conn, profileFetcher);

            Path templatesDirPath = Paths.get(getDataFolder().getAbsolutePath(), TEMPLATES_DIRECTORY);
            File templatesDir = templatesDirPath.toFile();
            TemplateDataUpdater templateDataUpdater = new TemplateDataUpdater();
            templateDataUpdater.extractTemplateResourcesFromJar(templatesDir, existingVersion, currentVersion);
            templateDataUpdater.loadTemplates(templatesDir, renderTemplateRepository, renderRepository);
        }
        catch (SQLException | IOException | URISyntaxException | InvalidConfigurationException e)
        {
            getLogger().severe("Encountered an error when loading the plugin:");
            getLogger().severe(e.toString());
            e.printStackTrace();
        }
    }

    private Connection connectToCommemorationsDatabase() throws SQLException, IOException
    {
        final String sqlitePrefix = "jdbc:sqlite:";
        final Path dbPath = Paths.get(getDataFolder().toString(), "data", "commemorations.db");
        final File dbFile = dbPath.toFile();
        if (!dbFile.exists())
            dbFile.getParentFile().mkdirs();

        String url = sqlitePrefix + dbPath;
        Connection sqlConnection = DriverManager.getConnection(url);
        return sqlConnection;
    }

    @Override
    public void onEnable()
    {
        super.onEnable();
        PluginConfig.getInstance(); // Ensure an instance exists now so we don't suffer creation costs later.

        CommemorationsPlayerDataUpdater playerDataUpdater = new CommemorationsPlayerDataUpdater(profileFetcher, renderRepository);
        getServer().getPluginManager().registerEvents(playerDataUpdater, this);

        CommemorationSignBlockSchematic schematic = new CommemorationSignBlockSchematic();
        CommemorationSignBlockSchematicBuilder builder = new CommemorationSignBlockSchematicBuilder();
        SchematicLocationSearcher searcher = new SchematicLocationSearcher();
        IntervalledStatisticEventHandler intervalledStatisticEventHandler = new IntervalledStatisticEventHandler(this, 400);
        CommemorationTriggerListener commemorationTriggerListener = new CommemorationTriggerListener(renderRepository, schematic, builder, searcher);
        getServer().getPluginManager().registerEvents(commemorationTriggerListener, this);
        intervalledStatisticEventHandler.addListener(commemorationTriggerListener);
    }

    @Override
    public void onDisable()
    {
        try
        {
            conn.close();
        }
        catch (SQLException e)
        {
            getLogger().severe("Unable to disconnect database.");
            getLogger().severe(e.toString());
        }
        finally
        {
            super.onDisable();
        }

    }
}
