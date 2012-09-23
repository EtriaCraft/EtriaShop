package com.etriacraft.EtriaShop;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.material.Attachable;

public class BlockListener implements Listener {
    
    private Main plugin;
    private PlayerListener pListener;
    
    public BlockListener(Main instance, PlayerListener listen) {
        plugin = instance;
        pListener = listen;
    }
    
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if ((event.getLine(0).equalsIgnoreCase("[Shop]")) || (event.getLine(0).equalsIgnoreCase("[Buy]")) || (event.getLine(0).equalsIgnoreCase("[Sell]")) ||
                (event.getLine(0).equalsIgnoreCase("[ATM]"))) {

            org.bukkit.material.Sign s = (org.bukkit.material.Sign)event.getBlock().getState().getData();
            Attachable a = (Attachable)s;
            
            Material m = event.getBlock().getRelative(a.getAttachedFace()).getType();
            
            if ((m == Material.SAND) || (m == Material.GRAVEL)) {
                event.getPlayer().sendMessage(Main.PREFIX + "You can not place a shop sign on sand or gravel.");
                event.setCancelled(false);
                return;
            }
            
            System.out.println(event.getBlock().getLocation().getWorld().getName());

            if (event.getLine(0).equalsIgnoreCase("[Shop]")) {
                double buy_price = -1d;
                double sell_price = -1d;
                
                try {
                    if (event.getLine(3).contains(":")) {
                        String[] prices = event.getLine(3).replace(" ", "").split("[:]");
                        buy_price = Double.parseDouble(prices[0]);
                        sell_price = Double.parseDouble(prices[1]);
                    } else {
                        buy_price = Double.parseDouble(event.getLine(3));
                    }
                } catch (NumberFormatException e) {}
                
                if (buy_price < 0 || sell_price < 0) {
                    event.getPlayer().sendMessage(Main.PREFIX + "Price missing or invalid.");
                    event.setCancelled(true);
                    return;
                }
                
event.getPlayer().sendMessage(Main.PREFIX + "Shop sign created, click it with the item you'd like it to handle.");

                ShopSign ss = new ShopSign(event.getBlock().getLocation(), event.getLine(0), event.getPlayer().getName(), buy_price, sell_price);
                plugin.signs.put(event.getBlock().getLocation(), ss);
                
                if (event.getLine(3).contains(":")) event.setLine(3, Double.toString(buy_price).replace(".00", "").replace(".0", "") + " : " + Double.toString(sell_price).replace(".00", "").replace(".0", ""));
                else event.setLine(3, Double.toString(buy_price).replace(".0", ""));

            } else if (event.getLine(0).equalsIgnoreCase("[Buy]")) {
                double buy_price = -1d;
            
                try {
                    buy_price = Double.parseDouble(event.getLine(3));
                } catch (NumberFormatException e) {}

                if (buy_price < 0) {
                    event.getPlayer().sendMessage(Main.PREFIX + "Price missing or invalid.");
                    event.setCancelled(true);
                    return;
                }

event.getPlayer().sendMessage(Main.PREFIX + "Shop sign created, click it with the item you'd like it to handle.");

                ShopSign ss = new ShopSign(event.getBlock().getLocation(), event.getLine(0), event.getPlayer().getName(), buy_price, 0d);
                plugin.signs.put(event.getBlock().getLocation(), ss);
                
                event.setLine(3, Double.toString(buy_price).replace(".00", "").replace(".0", ""));
            } else if (event.getLine(0).equalsIgnoreCase("[Sell]")) {
                double sell_price = -1d;
            
                try {
                    sell_price = Double.parseDouble(event.getLine(3));
                } catch (NumberFormatException e) {}


                if (sell_price < 0) {
                    event.getPlayer().sendMessage(Main.PREFIX + "Price missing or invalid.");
                    event.setCancelled(true);
                    return;
                }

event.getPlayer().sendMessage(Main.PREFIX + "Shop sign created, click it with the item you'd like it to handle.");

                ShopSign ss = new ShopSign(event.getBlock().getLocation(), event.getLine(0), event.getPlayer().getName(), 0d, sell_price);
                plugin.signs.put(event.getBlock().getLocation(), ss);
                
                event.setLine(3, Double.toString(sell_price).replace(".00", "").replace(".0", ""));

            } else if (event.getPlayer().hasPermission("etriashop.atm")) {
                double buy_price = -1d;
                double sell_price = -1d;
                
                try {
                    if (event.getLine(3).contains(":")) {
                        String[] prices = event.getLine(3).replace(" ", "").split("[:]");
                        buy_price = Double.parseDouble(prices[0]);
                        sell_price = Double.parseDouble(prices[1]);
                    } else {
                        buy_price = Double.parseDouble(event.getLine(3));
                        sell_price = buy_price;
                    }
                } catch (NumberFormatException e) {}
                
                if (buy_price < 0) {
                    event.getPlayer().sendMessage(Main.PREFIX + "That price is invalid.");
                    event.setCancelled(true);
                    return;
                }

event.getPlayer().sendMessage(Main.PREFIX + "Shop sign created, click it with the item you'd like it to handle.");
                
                ShopSign ss = new ShopSign(event.getBlock().getLocation(), event.getLine(0), "ATM", buy_price, sell_price);
                plugin.signs.put(event.getBlock().getLocation(), ss);
                event.setLine(3, Double.toString(buy_price).replace(".00", "").replace(".0", "") + " : " + Double.toString(sell_price).replace(".00", "").replace(".0", ""));
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.signs.containsKey(event.getBlock().getLocation())) {
            Location loc = event.getBlock().getLocation();
            
            if ((!plugin.signs.get(loc).getOwner().equalsIgnoreCase(event.getPlayer().getName())) && (!event.getPlayer().hasPermission("etriashop.break"))) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Main.PREFIX + "You do not have permission to break this sign.");
                plugin.signs.get(loc).update();
                return;
            }
            
            if (plugin.signs.get(loc).hasChest()) {
                ResultSet rs = Connection.query("SELECT chestid FROM shops WHERE x = " + loc.getBlockX() + " AND y = " + loc.getBlockY() +
                    " AND z = " + loc.getBlockZ() + " LIMIT 1;", false);
                try {
                    rs.next();
                    int chest = rs.getInt("chestid");
                    
                    Connection.query("DELETE FROM chests WHERE id = " + chest + " LIMIT 1;", true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            
            Connection.query("DELETE FROM shops WHERE x = " + loc.getBlockX() + " AND y = " + loc.getBlockY() +
                    " AND z = " + loc.getBlockZ() + " LIMIT 1;", true);
            plugin.signs.remove(event.getBlock().getLocation());
            
            if (pListener.selected.containsKey(event.getPlayer())) pListener.selected.remove(event.getPlayer());
            if (pListener.chatInput.containsKey(event.getPlayer())) pListener.chatInput.remove(event.getPlayer());
        } else if (event.getBlock().getState() instanceof Chest) {
            Location loc = event.getBlock().getLocation();
            ResultSet rs = Connection.query("SELECT id, COUNT(*) AS 'count', owner FROM chests WHERE x = " + loc.getBlockX() + " AND y = " + loc.getBlockY() +
                    " AND z = " + loc.getBlockZ() + " LIMIT 1;", false);
            
            try {
                rs.next();
                if (rs.getInt("count") > 0) {
                    if ((!rs.getString("owner").equalsIgnoreCase(event.getPlayer().getName())) && (!event.getPlayer().hasPermission("etriashop.break"))) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(Main.PREFIX + "You do not have permission to break this chest.");
                        return;
                    }
                    
                    ResultSet rs2 = Connection.query("SELECT world, x, y, z FROM shops WHERE chestid = " + rs.getInt("id"), false);
                    try {
                        rs2.next();
                        Location l = new Location(plugin.getServer().getWorld(rs2.getString("world")), rs2.getInt("x"), rs2.getInt("y"), rs2.getInt("z"));
                        plugin.signs.get(l).chestDestroyed();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    
                    Connection.query("UPDATE shops SET chestid = NULL WHERE chestid = " + rs.getInt("id"), true);
                    Connection.query("DELETE FROM chests WHERE id = " + rs.getInt("id") + " LIMIT 1;", true);
                } else {
                    BlockFace[] faces = new BlockFace[] { BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };
                    for (BlockFace bf : faces) {
                        if (event.getBlock().getRelative(bf).getType() == Material.CHEST) {
                            ResultSet rs2 = Connection.query("SELECT id, COUNT(*) AS 'count', owner FROM chests WHERE x = " + event.getBlock().getRelative(bf).getLocation().getBlockX() +
                                    " AND y = " + event.getBlock().getRelative(bf).getLocation().getBlockY() +
                                    " AND z = " + event.getBlock().getRelative(bf).getLocation().getBlockZ() + " LIMIT 1;", false);
                            
                            rs2.next();
                            
                            if ((rs2.getInt("count") > 0) && (!rs2.getString("owner").equalsIgnoreCase(event.getPlayer().getName())) && (!event.getPlayer().hasPermission("etriashop.break"))) {
                                event.setCancelled(true);
                                event.getPlayer().sendMessage(Main.PREFIX + "You do not have permission to break this chest.");
                                return;
                            }

                            ResultSet rs3 = Connection.query("SELECT COUNT(*) AS 'count', world, x, y, z FROM shops WHERE chestid = " + rs2.getInt("id"), false);
                            try {
                                rs3.next();
                                
                                if (rs3.getInt("count") > 0) {
                                    Location l = new Location(plugin.getServer().getWorld(rs3.getString("world")), rs3.getInt("x"), rs3.getInt("y"), rs3.getInt("z"));
                                    plugin.signs.get(l).chestDestroyed();
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            Connection.query("UPDATE shops SET chestid = NULL WHERE chestid = " + rs2.getInt("id"), true);
                            Connection.query("DELETE FROM chests WHERE id = " + rs2.getInt("id") + " LIMIT 1;", true);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            BlockFace[] faces = new BlockFace[] { BlockFace.UP, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };
            for (BlockFace bf : faces) {
                if ((event.getBlock().getRelative(bf).getState() instanceof Sign) && (plugin.signs.containsKey(event.getBlock().getRelative(bf).getLocation()))) {
                    org.bukkit.material.Sign s = (org.bukkit.material.Sign)event.getBlock().getRelative(bf).getState().getData();
                    Attachable a = (Attachable)s;
                    if (!event.getBlock().getRelative(bf).getRelative(a.getAttachedFace()).equals(event.getBlock())) {
                        //Sign is not attached to this block - we don't need to worry about it.
                        return;
                    }
                    //else continue on and cancel the break.
                    Location loc = event.getBlock().getRelative(bf).getLocation();
                    
                    if ((!plugin.signs.get(loc).getOwner().equalsIgnoreCase(event.getPlayer().getName())) && (!event.getPlayer().hasPermission("etriashop.break"))) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(Main.PREFIX + "You do not have permission to break this block.");
                        return;
                    }
            
                    if (plugin.signs.get(loc).hasChest()) {
                        ResultSet rs = Connection.query("SELECT chestid FROM shops WHERE x = " + loc.getBlockX() + " AND y = " + loc.getBlockY() +
                            " AND z = " + loc.getBlockZ() + " LIMIT 1;", false);
                        try {
                            rs.next();
                            int chest = rs.getInt("chestid");

                            Connection.query("DELETE FROM chests WHERE id = " + chest + " LIMIT 1;", true);
                        } catch (SQLException e) {}
                    }

                    Connection.query("DELETE FROM shops WHERE x = " + loc.getBlockX() + " AND y = " + loc.getBlockY() +
                            " AND z = " + loc.getBlockZ() + " LIMIT 1;", true);
                    plugin.signs.remove(loc);
                    if (pListener.selected.containsKey(event.getPlayer())) pListener.selected.remove(event.getPlayer());
                    if (pListener.chatInput.containsKey(event.getPlayer())) pListener.chatInput.remove(event.getPlayer());
                }
            }
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.CHEST || event.getBlock().getType() == Material.SIGN || event.getBlock().getType() == Material.SIGN_POST) {
            BlockFace[] faces = new BlockFace[] { BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };
            for (BlockFace bf : faces) {
                if (event.getBlock().getRelative(bf).getType() == Material.CHEST) {
                    ResultSet rs = Connection.query("SELECT owner, COUNT(*) as 'count' FROM chests WHERE x = " + event.getBlock().getRelative(bf).getLocation().getBlockX() + " AND y = " +
                            event.getBlock().getRelative(bf).getLocation().getBlockY() +
                            " AND z = " + event.getBlock().getRelative(bf).getLocation().getBlockZ() + " LIMIT 1;", false);
                    
                    try {
                        rs.next();
                        
                        if ((rs.getInt("count") > 0)) {
                            if (!rs.getString("owner").equalsIgnoreCase(event.getPlayer().getName())) {
                                event.setCancelled(true);
                                event.getPlayer().sendMessage(Main.PREFIX + "You can't place this block here.");
                            }
                        } else {
                            Block b = event.getBlock().getRelative(bf);
                            for (BlockFace bf2 : faces) {
                                if (b.getRelative(bf2).getType() == Material.CHEST) {
                                    ResultSet rs2 = Connection.query("SELECT owner, COUNT(*) as 'count' FROM chests WHERE x = " + b.getRelative(bf2).getLocation().getBlockX() + " AND y = " +
                                            b.getRelative(bf2).getLocation().getBlockY() +
                                            " AND z = " + b.getRelative(bf2).getLocation().getBlockZ() + " LIMIT 1;", false);

                                    try {
                                        rs2.next();

                                        if ((rs2.getInt("count") > 0)) {
                                            if (!rs2.getString("owner").equalsIgnoreCase(event.getPlayer().getName())) {
                                                event.setCancelled(true);
                                                event.getPlayer().sendMessage(Main.PREFIX + "You can't place this block here.");
                                            }
                                        }
                                    } catch (SQLException e) {}
                                }
                            }
                        }
                    } catch (SQLException e) {
                        
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if ((b.getState() instanceof Sign) && (plugin.signs.containsKey(b.getLocation()))) {
                event.setCancelled(true);
                return;
            }
            
            BlockFace[] faces = new BlockFace[] { BlockFace.UP, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };
            for (BlockFace bf : faces) {
                if ((b.getRelative(bf).getState() instanceof Sign) && (plugin.signs.containsKey(b.getRelative(bf).getLocation()))) {
                    org.bukkit.material.Sign s = (org.bukkit.material.Sign)b.getRelative(bf).getState().getData();
                    Attachable a = (Attachable)s;
                    if (!b.getRelative(bf).getRelative(a.getAttachedFace()).equals(b)) {
                        //Sign is not attached to this block - we don't need to worry about it.
                        return;
                    }
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) return;
        Block block = event.getBlock();
        
        Block b = block.getRelative(event.getDirection(), 2);
        
        if ((b.getState() instanceof Sign) && (plugin.signs.containsKey(b.getLocation()))) {
                event.setCancelled(true);
                return;
            }
        
        BlockFace[] faces = new BlockFace[] { BlockFace.UP, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };
        for (BlockFace bf : faces) {
            if ((b.getRelative(bf).getState() instanceof Sign) && (plugin.signs.containsKey(b.getRelative(bf).getLocation()))) {
                org.bukkit.material.Sign s = (org.bukkit.material.Sign)b.getRelative(bf).getState().getData();
                Attachable a = (Attachable)s;
                if (!b.getRelative(bf).getRelative(a.getAttachedFace()).equals(b)) {
                    //Sign is not attached to this block - we don't need to worry about it.
                    return;
                }
                event.setCancelled(true);
            }
        }
    }
}