package com.zazsona.commemorations;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DatabaseChangeManager
{
    private Connection conn;

    public DatabaseChangeManager(Connection conn)
    {
        this.conn = conn;
    }

    public SemanticVersion getDatabaseVersion() throws SQLException
    {
        Statement metaCheckStatement = conn.createStatement();
        metaCheckStatement.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='PluginMeta';");
        ResultSet metaCheckResults = metaCheckStatement.getResultSet();
        if (!metaCheckResults.next())
            return new SemanticVersion(0, 0, 0);

        Statement versionStatement = conn.createStatement();
        versionStatement.execute("SELECT * FROM PluginMeta ORDER BY MajorVersion DESC, MinorVersion DESC, PatchVersion DESC LIMIT 1;");
        ResultSet verResults = versionStatement.getResultSet();
        if (!verResults.next())
            return new SemanticVersion(0, 0, 0);

        int major = verResults.getInt(1);
        int minor = verResults.getInt(2);
        int patch = verResults.getInt(3);
        return new SemanticVersion(major, minor, patch);
    }

    public void upgradeDatabase() throws SQLException, IOException
    {
        SemanticVersion currentVersion = getDatabaseVersion();
        SemanticVersion targetVersion = new SemanticVersion(9999, 9999, 9999);
        ArrayList<String> scripts = getUpgradeScripts(currentVersion, targetVersion);
        runUpgradeScripts(scripts);
    }

    public void upgradeDatabase(SemanticVersion targetVersion) throws SQLException, IOException
    {
        SemanticVersion currentVersion = getDatabaseVersion();
        ArrayList<String> scripts = getUpgradeScripts(currentVersion, targetVersion);
        runUpgradeScripts(scripts);
    }

    private ArrayList<String> getUpgradeScripts(SemanticVersion currentVersion, SemanticVersion targetVersion) throws IOException
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
                                                                                       if (fileVersion.compareTo(currentVersion) >= 1 && fileVersion.compareTo(targetVersion) <= 0)
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

    private void runUpgradeScripts(ArrayList<String> upgradeScripts) throws IOException, SQLException
    {
        boolean isAutoCommit = conn.getAutoCommit();
        try
        {
            conn.setAutoCommit(false);
            for (String scriptPath : upgradeScripts)
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
}
