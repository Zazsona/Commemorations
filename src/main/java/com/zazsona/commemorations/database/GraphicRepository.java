package com.zazsona.commemorations.database;

import java.sql.*;

public class GraphicRepository
{
    private Connection conn;

    public GraphicRepository(Connection dbConn)
    {
        try
        {
            this.conn = dbConn;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
