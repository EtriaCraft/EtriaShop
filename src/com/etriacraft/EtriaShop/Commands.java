package com.etriacraft.EtriaShop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

public class Commands {

	private Main plugin;
	private PlayerListener player;
	
	public Commands(Main instance, PlayerListener pL, Config con) {
		plugin = instance;
		player = pL;
	}
	
	public void initialize() {
		PluginCommand shop = plugin.getCommand("shop");
		
		CommandExecutor exe = new CommandExecutor() {
			@Override
			public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
				if (sender instanceof Player) {
					if (args.length > 0) {
						shop(sender, args);
					}
				}
				
				return true;
			}
		};
		
		if (shop != null) shop.setExecutor(exe);
	}
	
	private void shop(CommandSender sender, String[] args) {
		if (args.length < 1) {
			sender.sendMessage(Main.PREFIX + "Not enough arguments!");
			return;
		}
		
		if (args[0].equalsIgnoreCase("ignore")) {
			if (player.ignoring.contains(sender.getName())) {
				player.ignoring.remove(sender.getName());
				sender.sendMessage(Main.PREFIX + "You're now receiving shop alerts.");
			} else {
				player.ignoring.add(sender.getName());
				sender.sendMessage(Main.PREFIX + "You're now ignoring shop elerts.");
			}
		} else if (args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("etriashop.reload")) return;
			
			plugin.loadSigns();
			sender.sendMessage(Main.PREFIX + "EtriaShop reloaded");	
		}
	}
}
