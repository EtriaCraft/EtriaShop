package com.etriacraft.EtriaShop;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;

public class ShopSign {
    
    public static Main plugin;
    
    public enum Type { BUY, SELL, COMBINED, ATM }
    
    private Location signLoc;
    private Location chestLoc;
    private Material item;
    private int amnt;
    private byte data;
    private double buy_price;
    private double sell_price;
    private Type type;
    private String owner;
    
    public boolean isActive;
    
    public ShopSign(Location loc, String firstLine, String p, double buy_price, double sell_price) {
        signLoc = loc;
        
        if (firstLine.equalsIgnoreCase("[Sell]")) {
            type = Type.SELL;
        } else if (firstLine.equalsIgnoreCase("[Buy]")) {
            type = Type.BUY;
        } else if (firstLine.equalsIgnoreCase("[Shop]")) {
            type = Type.COMBINED;
        } else {
            type = Type.ATM;
        }
        
        this.buy_price = buy_price;
        this.sell_price = sell_price;
        
        owner = p;
        
        item = null;
        amnt = 0;
        chestLoc = null;
        isActive = false;
        
        Connection.query("INSERT INTO shops (`world`, `x`, `y`, `z`, `owner`, `shopType`, `buy_price`, `sell_price`) VALUES ('" +
                loc.getWorld().getName() + "', " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() +
                ", '" + p + "', '" + type.toString() + "', " + buy_price + ", " + sell_price + ")", true);
    }
    
    public ShopSign(Location sign, Location chest, int item, int data, int amount, double buy_price, double sell_price, String type, String owner) {
        signLoc = sign;
        if (chest != null) chestLoc = chest;
        if (item > 0) this.item = Material.getMaterial(item);
        if (amount > 0) amnt = amount;
        if(type.equals("COMBINED")) {
            this.type = Type.COMBINED;
        } else if(type.equals("BUY")) {
            this.type = Type.BUY;
        } else if(type.equals("SELL")) {
            this.type = Type.SELL;
        } else {
            this.type = Type.ATM;
        }
        
        this.data = (byte)data;
        
        this.owner = owner;
        this.buy_price = buy_price;
        this.sell_price = sell_price;
    }
    
    public boolean hasBlock() {
        return (item != null);
    }
    
    public boolean hasChest() {
        if (type == Type.ATM) return true;
        return (chestLoc != null);
    }
    
    public Location getLocation() {
        return signLoc;
    }
    
    public Location getChestLocation() {
        return chestLoc;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public int getAmount() {
        return amnt;
    }
    
    public Material getItem() {
        return item;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public String getItemName() {
        String actual_name = item.toString();
        if (plugin.material_names.containsKey(new Pair(item.getId(), (int)data))) actual_name = plugin.material_names.get(new Pair(item.getId(), (int)data));
        StringBuilder b = new StringBuilder(actual_name.toLowerCase().replace("_", " "));
        int i = 0;
        do {
        b.replace(i, i + 1, b.substring(i,i + 1).toUpperCase());
        i = b.indexOf(" ", i) + 1;
        } while (i > 0 && i < b.length());

        return b.toString();
    }
    
    public double getBuyPrice() {
        return buy_price;
    }
    
    public double getSellPrice() {
        return sell_price;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setItem(Material item, byte data, int amount) {
        this.item = item;
        this.data = data;
        amnt = amount;
        Connection.query("UPDATE shops SET item = " + item.getId() + ", amount = " + amount + ", data = " + ((int)(this.data)) + " WHERE x = " + signLoc.getBlockX() + " AND y = " + signLoc.getBlockY() +
                " AND z = " + signLoc.getBlockZ() + " LIMIT 1;", true);
    }
    
    public boolean linkChest(Chest c, String player) {
        if (!hasBlock()) return false;
        
        if (!checkDistance(signLoc, c.getLocation())) return false;
        
        ResultSet rs = Connection.query("SELECT owner, COUNT(*) AS 'count' FROM chests WHERE x = " + c.getLocation().getBlockX() + " AND y = " + c.getLocation().getBlockY() +
                " AND z = " + c.getLocation().getBlockZ() + " LIMIT 1;", false);
        
        try {
            rs.next();
            if ((rs.getInt("count") > 0) && (!rs.getString("owner").equalsIgnoreCase(player))) return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        
        ResultSet rs2 = Connection.query("SELECT id AS 'count' FROM chests ORDER BY id DESC LIMIT 1;", false);
        int id = 1;
        try {
            rs2.next();
            id = (rs2.getInt("count") + 1);
        } catch (SQLException e) {
        }
        
        Connection.query("INSERT INTO chests VALUES (" + id + ", '" + c.getLocation().getWorld().getName() +
                "', " + c.getLocation().getBlockX() + ", " + c.getLocation().getBlockY() + ", " + c.getLocation().getBlockZ() + ", '" + owner + "')", true);
        Connection.query("UPDATE shops SET chestid = " + id + " WHERE x = " + signLoc.getBlockX() + " AND y = " + signLoc.getBlockY() +
                " AND z = " + signLoc.getBlockZ() + " LIMIT 1;", true);
        
        chestLoc = c.getLocation();
        
        return true;
    }
    
    private boolean checkDistance(Location a, Location b) {
        int xdiff = Math.abs(a.getBlockX() - b.getBlockX());
        int ydiff = Math.abs(a.getBlockY() - b.getBlockY());
        int zdiff = Math.abs(a.getBlockZ() - b.getBlockZ());
        if(xdiff > Config.restrict_x || ydiff > Config.restrict_y || zdiff > Config.restrict_z) return false;
        else return true;
    }
    
    public boolean chestHasEnough() {
        if (type == Type.ATM) return true;
        if ((item == null) || (amnt == 0) || (chestLoc == null)) return false;
        
        Chest c = (Chest)chestLoc.getBlock().getState();
        HashMap<Integer, ? extends ItemStack> hm = c.getInventory().all(item);
        
        int cumulative = 0;
        
        for (Integer i : hm.keySet()) {
            ItemStack is = hm.get(i);
            if ((is.getType().getMaxDurability() > 0) && (is.getDurability() > 0)) continue;
            
            if (!is.getEnchantments().isEmpty()) continue;
            
            if (is.getData().getData() != data) continue;
            
            if ((is.getAmount() >= amnt)) return true;
            else {
                cumulative += is.getAmount();
            }
        }
        
        if (cumulative >= amnt) return true;
        
        return false;
    }
    
    public boolean chestHasSpace() {
        if (type == Type.ATM) return true;
        if ((item == null) || (amnt == 0) || (chestLoc == null)) return false;
        
        Chest c = (Chest)chestLoc.getBlock().getState();
        
        if (c.getInventory().firstEmpty() != -1) return true;
        
        HashMap<Integer, ? extends ItemStack> hm = c.getInventory().all(item);
        for (Integer i : hm.keySet()) {
            ItemStack is = hm.get(i);
            if ((is.getAmount() <= (item.getMaxStackSize() - amnt)) && (is.getData().getData() == data)) {
                return true;
            }
        }
        
        return false;
    }
    
    public void removeFromChest() {
        if (type == Type.ATM) return;
        if ((item == null) || (amnt == 0) || (chestLoc == null)) return;
        
        Chest c = (Chest)chestLoc.getBlock().getState();
        c.getInventory().removeItem(new ItemStack(item, amnt, (short)0, data));
    }
    
    public void addToChest() {
		if (type == Type.ATM) return;
		if ((item == null) || (amnt == 0) || (chestLoc == null)) return;
		
		int temp_amnt = amnt;
		
		Chest c = (Chest)chestLoc.getBlock().getState();
		while (temp_amnt > item.getMaxStackSize()) {
		c.getInventory().addItem(new ItemStack(item, item.getMaxStackSize(), (short)0, data));
		temp_amnt -= item.getMaxStackSize();
		}
		if (temp_amnt > 0) c.getInventory().addItem(new ItemStack(item, temp_amnt, (short)0, data));
		    }
		    
		    public void update() {
		        Sign s = (Sign)signLoc.getBlock().getState();
		        s.update();
		    }
		    
		    public void setLine(int i, String text) {
		        Sign s = (Sign)signLoc.getBlock().getState();
		        s.setLine(i, text);
		        s.update();
		    }
		    
		    public void chestDestroyed() {
		        chestLoc = null;
		    }
		    
		    public void recheck() {
		        if (type == Type.BUY) {
		            if (chestHasEnough()) setLine(0, "§a[§0Buy§a]");
		            else setLine(0, "§4[§0Buy§4]");
		        } else if (type == Type.SELL) {
		            if (chestHasSpace()) setLine(0, "§a[§0Sell§a]");
		            else setLine(0, "§4[§0Sell§4]");
		        } else if (type == Type.COMBINED) {
		if (chestHasSpace() && chestHasEnough()) setLine(0, "§a[§0Shop§a]");
		else setLine(0, "§4[§0Shop§4]");
		if (chestHasSpace() && !chestHasEnough()) setLine(0, "§4[§0Shop§a]");
		if (!chestHasSpace() && chestHasEnough()) setLine(0, "§a[§0Shop§4]");
		}
    }
    
    public byte getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return "ShopSign(l: (" + signLoc.getWorld().getName() + ", " + signLoc.getBlockX() + ", " + signLoc.getBlockY() + ", " + signLoc.getBlockZ() + "), o: " + owner + ", t: " +
                type.toString();
    }

}