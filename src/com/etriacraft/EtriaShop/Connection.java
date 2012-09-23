package com.etriacraft.EtriaShop;

import java.sql.ResultSet;
import java.sql.SQLException;
import com.etriacraft.lib.MySQLConnection;

public class Connection {

    public static MySQLConnection con;
    
    public static String host;
    public static int port;
    public static String db;
    public static String user;
    public static String pass;
    
    public static void init() {
        Main.log.info(Main.PREFIX + "Establishing database connection...");
        
        try {
            con = new MySQLConnection(host, port, db, user, pass);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        if (con.connect(true)) {
            Main.log.info(Main.PREFIX + "Connection established!");
        } else {
            Main.log.warning(Main.PREFIX + "MySQL connection failed!");
        }
    }
    
    public static void disable() {
        con.disconnect();
    }
    
    public static ResultSet query(String query, boolean modifies) {
        try {
            return con.executeQuery(query, modifies);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
}
