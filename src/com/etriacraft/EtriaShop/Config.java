package com.etriacraft.EtriaShop;


import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;

public class Config {

	private Main plugin;
	
	public static int restrict_x;
	public static int restrict_y;
	public static int restrict_z;
	public static int fee;
	public static String fee_account;
	
	public Config(Main instance) {
		plugin = instance;
		
		restrict_x = 20;
		restrict_y = 20;
		restrict_z = 20;
		fee = 200;
		fee_account = "EtriaCraft";
	}
	
	public void initialize() {
		restrict_x = 20;
		restrict_y = 20;
		restrict_z = 20;
		fee = 200;
		fee_account = "EtriaCraft";
		
		YamlConfiguration config = (YamlConfiguration)plugin.getConfig();
		
		if (!new File(plugin.getDataFolder(), "config.yml").exists()) {
			config.options().copyDefaults(true);
			plugin.saveConfig();
		}
		
		restrict_x = config.getInt("restrict.x", 20);
		restrict_y = config.getInt("restrict.y", 20);
		restrict_z = config.getInt("restrict.z", 20);
		
		fee = config.getInt("fee.amount", 200);
		fee_account = config.getString("fee.account", "EtriaCraft");
		
		Connection.host = config.getString("mysql.address", "localhost");
		Connection.db = config.getString("mysql.database", "shops");
		Connection.user = config.getString("mysql.user", "root");
		Connection.pass = config.getString("mysql.pass", "");
		Connection.port = config.getInt("mysql.port", 3306);
	}
	
}