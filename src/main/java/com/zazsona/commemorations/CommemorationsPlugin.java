package com.zazsona.commemorations;

import com.zazsona.commemorations.database.GraphicRepository;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CommemorationsPlugin extends JavaPlugin
{
    private static String pluginName;
    private Connection conn;

    private GraphicRepository graphicRepository;

    public static CommemorationsPlugin getInstance()
    {
        if (pluginName != null)
            return (CommemorationsPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        else
            throw new NullPointerException("The plugin has not yet initialised.");
    }

    public GraphicRepository getGraphicRepository()
    {
        return graphicRepository;
    }


    @Override
    public void onLoad()
    {
        try
        {
            super.onLoad();
            pluginName = getDescription().getName();
            conn = connectToCommemorationsDatabase();
            SemanticVersion dbVer = getDatabaseVersion();
            ArrayList<String> dcmScripts = getRequiredDcmScripts(dbVer);
            executeDatabaseChangeManagement(dcmScripts);

            graphicRepository = new GraphicRepository(conn);
        }
        catch (SQLException | IOException e)
        {
            getLogger().severe("Unable to connect to database. Plugin could not be loaded.");
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

    private SemanticVersion getDatabaseVersion() throws SQLException
    {
        Statement metaCheckStatement = conn.createStatement();
        metaCheckStatement.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='PluginMeta';");
        ResultSet metaCheckResults = metaCheckStatement.getResultSet();
        if (!metaCheckResults.next())
            return new SemanticVersion(0, 0, 0);

        Statement versionStatement = conn.createStatement();
        versionStatement.execute("SELECT * FROM PluginMeta ORDER BY MajorVersion DESC, MinorVersion DESC, PatchVersion DESC LIMIT 1;");
        ResultSet verResults = metaCheckStatement.getResultSet();
        if (!verResults.next())
            return new SemanticVersion(0, 0, 0);

        int major = verResults.getInt(0);
        int minor = verResults.getInt(1);
        int patch = verResults.getInt(2);
        return new SemanticVersion(major, minor, patch);
    }

    private ArrayList<String> getRequiredDcmScripts(SemanticVersion currentVersion) throws IOException
    {
        try
        {
            String dcmDir = "dcm";
            ArrayList<String> requiredScripts = new ArrayList<>();
            final URI uri = this.getClass().getClassLoader().getResource(dcmDir).toURI();
            final FileSystem scriptsFileSys = FileSystems.newFileSystem(uri, Collections.emptyMap());
            Files.walk(scriptsFileSys.getPath("/" + dcmDir + "/")).forEach(path ->
                                                                        {
                                                                            String fileNameWithExtension = path.getFileName().toString();
                                                                            if (!Files.isDirectory(path))
                                                                            {
                                                                                String extension = fileNameWithExtension.substring(fileNameWithExtension.lastIndexOf("."));
                                                                                String fileName = fileNameWithExtension.replace(extension, "");
                                                                                if (fileName.startsWith("V"))
                                                                                {
                                                                                    String version = fileName.substring(1);
                                                                                    SemanticVersion fileVersion = new SemanticVersion(version);
                                                                                    if (fileVersion.compareTo(currentVersion) >= 1)
                                                                                        requiredScripts.add((dcmDir + "/" + fileNameWithExtension));
                                                                                }

                                                                            }
                                                                        });
            scriptsFileSys.close();
            requiredScripts.sort(Comparator.naturalOrder());
            return requiredScripts;
        }
        catch (Exception e)
        {
            throw new IOException("Unable to get DCM scripts.", e);
        }
    }

    private void executeDatabaseChangeManagement(ArrayList<String> dcmScriptPaths) throws IOException, SQLException
    {
        boolean isAutoCommit = conn.getAutoCommit();
        try
        {
            conn.setAutoCommit(false);
            for (String scriptPath : dcmScriptPaths)
            {
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(scriptPath);
                String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                String[] sqlStatements = sql.split(";");
                for (String sqlStatement : sqlStatements)
                {
                    Statement statement = conn.createStatement();
                    statement.execute(sqlStatement + ";");
                }
                conn.commit();
            }
        }
        catch (Exception e)
        {
            conn.rollback();
            throw e;
        }
        finally
        {
            conn.setAutoCommit(isAutoCommit);
        }
    }

    @Override
    public void onEnable()
    {
        super.onEnable();
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
