package com.etriacraft.EtriaShop;

import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;

public class Econ {
    
    public static Economy handler;
    
    private Main plugin;
    
    public Econ(Main instance) {
        plugin = instance;
        handler = null;
    }
    
    public boolean setupEconomy() {
        RegisteredServiceProvider<Economy> ecoProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (ecoProvider != null) {
            handler = ecoProvider.getProvider();
        }
        
        return (handler != null);
    }

}