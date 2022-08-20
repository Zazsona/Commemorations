package com.zazsona.commemorations.repository;

import com.zazsona.commemorations.CommemorationMapRenderer;
import com.zazsona.commemorations.database.MapIdRenderedGraphicPair;
import com.zazsona.commemorations.database.RenderedGraphic;
import com.zazsona.commemorations.database.TemplateGraphic;
import com.zazsona.commemorations.image.GraphicRenderer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.map.MapView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class CommemorationMapRepository
{
    private Connection conn;
    private RenderRepository renderRepository;

    public CommemorationMapRepository(Connection dbConn, RenderRepository renderRepository)
    {
        this.conn = dbConn;
        this.renderRepository = renderRepository;
    }

    public void loadCommemorationMaps() throws SQLException
    {
        List<MapIdRenderedGraphicPair> pairs = getMapGraphicPairs();
        for (MapIdRenderedGraphicPair pair : pairs)
            loadCommemorationMap(pair.getMapId());
    }

    private MapView loadCommemorationMap(int mapId) throws SQLException
    {
        MapView mapView = Bukkit.getMap(mapId);
        mapView.getRenderers().clear();
        mapView.addRenderer(new CommemorationMapRenderer(this, renderRepository));
        return mapView;
    }

    public MapView createCommemorationMap(World world, UUID renderGuid) throws SQLException
    {
        RenderedGraphic render = renderRepository.getRender(renderGuid);
        return createCommemorationMap(world, render);
    }

    public MapView createCommemorationMap(World world, RenderedGraphic render) throws SQLException
    {
        MapView mapView = Bukkit.createMap(world);
        insertMapGraphicPair(mapView.getId(), render.getRenderGuid());
        loadCommemorationMap(mapView.getId());
        return mapView;
    }

    public MapView getCommemorationMap(UUID renderGuid) throws SQLException
    {
        int mapId = getMapGraphicPair(renderGuid).getMapId();
        return getCommemorationMap(mapId);
    }

    public MapView getCommemorationMap(int mapId) throws SQLException
    {
        if (!isCommemorationMap(mapId))
            throw new IllegalArgumentException("MapId is not a Commemoration Map!");
        return Bukkit.getMap(mapId);
    }

    public MapIdRenderedGraphicPair getMapGraphicPair(int mapId) throws SQLException
    {
        String sql = "SELECT MapId, RenderGuid FROM MapIdRenderedGraphicPair WHERE MapId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setInt(1, mapId);
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        if (selectResults.next())
        {
            int dbMapId = selectResults.getInt(1);
            UUID dbRenderGuid = UUID.fromString(selectResults.getString(2));
            MapIdRenderedGraphicPair pair = new MapIdRenderedGraphicPair(dbMapId, dbRenderGuid);
            return pair;
        }
        else
            throw new IllegalArgumentException("MapId does not exist.");
    }

    public MapIdRenderedGraphicPair getMapGraphicPair(UUID renderGuid) throws SQLException
    {
        String sql = "SELECT MapId, RenderGuid FROM MapIdRenderedGraphicPair WHERE RenderGuid = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setString(1, renderGuid.toString());
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        if (selectResults.next())
        {
            int dbMapId = selectResults.getInt(1);
            UUID dbRenderGuid = UUID.fromString(selectResults.getString(2));
            MapIdRenderedGraphicPair pair = new MapIdRenderedGraphicPair(dbMapId, dbRenderGuid);
            return pair;
        }
        else
            throw new IllegalArgumentException("RenderGuid does not exist.");
    }

    public List<MapIdRenderedGraphicPair> getMapGraphicPairs() throws SQLException
    {
        String sql = "SELECT MapId, RenderGuid FROM MapIdRenderedGraphicPair;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        LinkedList<MapIdRenderedGraphicPair> mapGraphicPairs = new LinkedList<>();
        while (selectResults.next())
        {
            int dbMapId = selectResults.getInt(1);
            UUID dbRenderGuid = UUID.fromString(selectResults.getString(2));
            MapIdRenderedGraphicPair pair = new MapIdRenderedGraphicPair(dbMapId, dbRenderGuid);
            mapGraphicPairs.add(pair);
        }
        return mapGraphicPairs;
    }

    public boolean isCommemorationMap(int mapId) throws SQLException
    {
        String sql = "SELECT MapId FROM MapIdRenderedGraphicPair WHERE MapId = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(sql);
        selectStatement.setInt(1, mapId);
        selectStatement.execute();
        ResultSet selectResults = selectStatement.getResultSet();
        return selectResults.next();
    }

    private MapIdRenderedGraphicPair insertMapGraphicPair(int mapId, UUID renderGuid) throws SQLException
    {
        if (isCommemorationMap(mapId))
            throw new IllegalArgumentException("MapId already exists!");
        if (!renderRepository.doesRenderExist(renderGuid))
            throw new IllegalArgumentException("RenderGuid does not exist!");

        String sql = "INSERT INTO MapIdRenderedGraphicPair (MapId, RenderGuid)" +
                                                   "VALUES (?,     ?         );";

        long lastUpdated = Instant.now().getEpochSecond();
        PreparedStatement insertStatement = conn.prepareStatement(sql);
        insertStatement.setInt(1, mapId);
        insertStatement.setString(2, renderGuid.toString());
        insertStatement.executeUpdate();

        MapIdRenderedGraphicPair mapRenderPair = new MapIdRenderedGraphicPair(mapId, renderGuid);
        return mapRenderPair;
    }
}
