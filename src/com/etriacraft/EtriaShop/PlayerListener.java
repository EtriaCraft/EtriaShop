package com.etriacraft.EtriaShop;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

	private Main plugin;

	public HashMap<Player, ShopSign> selected = new HashMap<Player, ShopSign>();
	public HashMap<Player, Pair<Material, Byte>> chatInput = new HashMap<Player, Pair<Material, Byte>>();
	public List<String> ignoring = new ArrayList<String>();

	public PlayerListener(Main instance) {
		plugin = instance;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		if ((!plugin.signs.containsKey(event.getClickedBlock().getLocation())) && (!selected.containsKey(event.getPlayer())) &&
				(event.getClickedBlock().getType() != Material.CHEST)) return;

		if (event.getClickedBlock().getState() instanceof Sign) {
			if ((event.getAction() == Action.RIGHT_CLICK_BLOCK)) event.setCancelled(true);
			ShopSign s = plugin.signs.get(event.getClickedBlock().getLocation());

			if ((s.getOwner().equalsIgnoreCase(event.getPlayer().getName())) || ((s.getOwner().equals("ATM")) && (event.getPlayer().hasPermission("etriashop.atm")) && (!s.hasBlock()))) {
				//Owner interacting with sign.
				if ((!s.hasBlock()) && (event.getPlayer().getItemInHand().getType() != Material.AIR)) {
					if (((event.getPlayer().getItemInHand().getType().getMaxDurability() > 0) && (event.getPlayer().getItemInHand().getDurability() > (short)0)) ||
							(event.getPlayer().getItemInHand().getEnchantments().size() > 0)) {
						event.setCancelled(true);
						event.setUseInteractedBlock(Event.Result.DENY);
						event.setUseItemInHand(Event.Result.DENY);
						event.getPlayer().sendMessage(Main.PREFIX + "You can not buy or sell damaged or enchanted items.");
						return;
					}
					chatInput.put(event.getPlayer(), new Pair(event.getPlayer().getItemInHand().getType(), event.getPlayer().getItemInHand().getData().getData()));
					selected.put(event.getPlayer(), s);

					String action;
					if (s.getType() == ShopSign.Type.COMBINED) action = "buy/sell";
					else if (s.getType() == ShopSign.Type.BUY) action = "sell";
					else if (s.getType() == ShopSign.Type.SELL) action = "buy";
					else action = "buy/sell";

					String actual_name = event.getPlayer().getItemInHand().getType().toString();
					Pair p = new Pair(event.getPlayer().getItemInHand().getTypeId(), (int)event.getPlayer().getItemInHand().getData().getData());
					if (plugin.material_names.containsKey(p))
						actual_name = plugin.material_names.get(p);

					event.getPlayer().sendMessage(Main.PREFIX + "How much§6 " + plugin.prettify(actual_name) + " §7would you like to " + action + " per transaction?");
				} else {
					if ((!selected.containsKey((event.getPlayer()))) && (!s.hasChest())) {
						selected.put(event.getPlayer(), s);
						event.getPlayer().sendMessage(Main.PREFIX + "You haven't linked a chest to this sign yet. Please left click the chest you'd like to link with this sign.");
					} else if (s.hasBlock()) {
						if (s.getType() == ShopSign.Type.BUY) {
							event.getPlayer().sendMessage(Main.PREFIX + "This shop is §9selling§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getBuyPrice()));
							s.recheck();
						} else if (s.getType() == ShopSign.Type.SELL) {
							event.getPlayer().sendMessage(Main.PREFIX + "This shop is §9buying§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getSellPrice()));
							s.recheck();
						} else {
							event.getPlayer().sendMessage(Main.PREFIX + "This shop is §9selling§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getBuyPrice()) +
									" §7and §9buying§7 for§6 " + Econ.handler.format(s.getSellPrice()));
							s.recheck();
						}
					}
				}
			} else {
				if (s.getItem() == null || (s.getAmount() == 0)) return;
				if (s.getType() == ShopSign.Type.COMBINED) {
					if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
						if (!event.getPlayer().isSneaking()) {
							event.getPlayer().sendMessage(Main.PREFIX + "Click use item to §9buy§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getBuyPrice()));
							if (s.getSellPrice() >= 0) event.getPlayer().sendMessage("§7Or, hold sneak and punch once to view sell options.");
						} else {
							if (s.getSellPrice() >= 0) event.getPlayer().sendMessage(Main.PREFIX + "Click use item to §9sell§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getSellPrice()));
						}
					} else {
						if (!event.getPlayer().isSneaking()) {
							//Buying
							if (Econ.handler.has(event.getPlayer().getName(), s.getBuyPrice())) {
								if (s.chestHasEnough()) {
									if (event.getPlayer().getInventory().firstEmpty() == -1) {
										HashMap<Integer, ? extends ItemStack> hm = event.getPlayer().getInventory().all(s.getItem());
										for (Integer i : hm.keySet()) {
											ItemStack is = hm.get(i);
											if (is.getAmount() <= (s.getItem().getMaxStackSize() - s.getAmount())) {
												buy(s, event.getPlayer());
												return;
											}
										}
										event.getPlayer().sendMessage(Main.PREFIX + "There isn't enough space in your inventory for that.");
									} else {
										buy(s, event.getPlayer());
									}
								} else {
									event.getPlayer().sendMessage(Main.PREFIX + "This shop is out of stock!");
									Player owner = plugin.getServer().getPlayer(s.getOwner());
									if ((owner != null) && (owner.isOnline()) && (!ignoring.contains(owner.getName()))) owner.sendMessage(Main.PREFIX + "Your§6 " + s.getItemName() + " §7store at [§9" +
											s.getLocation().getBlockX() + ", " + s.getLocation().getBlockY() + ", " + s.getLocation().getBlockZ() + "§7] is out of stock.");
								}
							} else {
								event.getPlayer().sendMessage(Main.PREFIX + "You can't afford that!");
							}
						} else {
							if (s.getSellPrice() < 0) return;
							if (event.getPlayer().getInventory().contains(s.getItem(), s.getAmount())) {
								if (s.chestHasSpace()) {
									if (Econ.handler.has(s.getOwner(), s.getSellPrice())) {
										sell(s, event.getPlayer());
									} else {
										event.getPlayer().sendMessage(Main.PREFIX + "The shop owner can't afford that!");
									}
								} else {
									event.getPlayer().sendMessage(Main.PREFIX + "This shop's chest is full!");
								}
							} else {
								event.getPlayer().sendMessage(Main.PREFIX + "You don't have enough to sell!");
							}
						}
					}
					s.recheck();
				} else if (s.getType() == ShopSign.Type.BUY) {
					if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
						event.getPlayer().sendMessage(Main.PREFIX + "Click use item to §9buy§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getBuyPrice()));
					} else {
						if (Econ.handler.has(event.getPlayer().getName(), s.getBuyPrice())) {
							if (s.chestHasEnough()) {
								if (event.getPlayer().getInventory().firstEmpty() == -1) {
									HashMap<Integer, ? extends ItemStack> hm = event.getPlayer().getInventory().all(s.getItem());
									for (Integer i : hm.keySet()) {
										ItemStack is = hm.get(i);
										if (is.getAmount() <= (s.getItem().getMaxStackSize() - s.getAmount())) {
											buy(s, event.getPlayer());
											return;
										}
									}

									event.getPlayer().sendMessage(Main.PREFIX + "There isn't enough room in your inventory for that.");
								} else {
									buy(s, event.getPlayer());
								}
							} else {
								event.getPlayer().sendMessage(Main.PREFIX + "This shop is out of stock!");
								Player owner = plugin.getServer().getPlayer(s.getOwner());
								if ((owner != null) && (owner.isOnline()) && (!ignoring.contains(owner.getName()))) owner.sendMessage(Main.PREFIX + "Your§6 " + s.getItemName() + " §7store at [§9" +
										s.getLocation().getBlockX() + ", " + s.getLocation().getBlockY() + ", " + s.getLocation().getBlockZ() + "§7] is out of stock.");
							}
						} else {
							event.getPlayer().sendMessage(Main.PREFIX + "You can't afford that!");
						}
					}
					s.recheck();
				} else if (s.getType() == ShopSign.Type.SELL) {
					if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
						event.getPlayer().sendMessage(Main.PREFIX + "Click use item to §9sell§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getSellPrice()));
					} else {
						if (s.chestHasSpace()) {
							if (Econ.handler.has(s.getOwner(), s.getSellPrice())) {
								sell(s, event.getPlayer());
							} else {
								event.getPlayer().sendMessage(Main.PREFIX + "The shop owner can't afford that!");
							}
						} else {
							event.getPlayer().sendMessage(Main.PREFIX + "This shop's chest is full!");
						}
					}
					s.recheck();
				} else {
					if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
						if (!event.getPlayer().isSneaking()) {
							event.getPlayer().sendMessage(Main.PREFIX + "Click use item to §9buy§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getBuyPrice()));
							if (s.getSellPrice() >= 0) event.getPlayer().sendMessage("§7Or, hold sneak and punch once to view sell options.");
						} else {
							event.getPlayer().sendMessage(Main.PREFIX + "Click use item to §9sell§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getSellPrice()));
						}
					} else {
						if (!event.getPlayer().isSneaking()) {
							//Buying
							if (Econ.handler.has(event.getPlayer().getName(), s.getBuyPrice())) {
								if (s.chestHasEnough()) {
									if (event.getPlayer().getInventory().firstEmpty() == -1) {
										HashMap<Integer, ? extends ItemStack> hm = event.getPlayer().getInventory().all(s.getItem());
										for (Integer i : hm.keySet()) {
											ItemStack is = hm.get(i);
											if (is.getAmount() <= (s.getItem().getMaxStackSize() - s.getAmount())) {
												buyATM(s, event.getPlayer());
												return;
											}
										}
										event.getPlayer().sendMessage(Main.PREFIX + "There isn't enough space in your inventory for that!");
									} else {
										buyATM(s, event.getPlayer());
									}
								} else {
									event.getPlayer().sendMessage(Main.PREFIX + "This shop is out of stock!");
									Player owner = plugin.getServer().getPlayer(s.getOwner());
									if ((owner != null) && (owner.isOnline()) && (!ignoring.contains(owner.getName()))) owner.sendMessage(Main.PREFIX + "Your§6 " + s.getItemName() + " §7store at [§9" +
											s.getLocation().getBlockX() + ", " + s.getLocation().getBlockY() + ", " + s.getLocation().getBlockZ() + "§7] is out of stock.");
								}
							} else {
								event.getPlayer().sendMessage(Main.PREFIX + "You can't afford that!");
							}
						} else {
							if (s.getSellPrice() < 0) return;

							if (s.chestHasSpace()) {
								sellATM(s, event.getPlayer());
							} else {
								event.getPlayer().sendMessage(Main.PREFIX + "This shop's chest is full!");
							}
						}
					}
				}
			}
			s.update();
		} else {
			if ((event.getClickedBlock().getType() == Material.CHEST) && (event.getAction() == Action.RIGHT_CLICK_BLOCK)) {

				ResultSet rs = Connection.query("SELECT COUNT(*) AS 'count', owner FROM chests WHERE x = " + event.getClickedBlock().getLocation().getBlockX() + " AND y = " + event.getClickedBlock().getLocation().getBlockY() +
						" AND z = " + event.getClickedBlock().getLocation().getBlockZ() + " LIMIT 1;", false);

				try {
					rs.next();
					if (rs.getInt("count") > 0) {
						if ((!rs.getString("owner").equalsIgnoreCase(event.getPlayer().getName())) && (!event.getPlayer().hasPermission("etriashop.break"))) {
							event.setCancelled(true);
							event.setUseInteractedBlock(Event.Result.DENY);
							event.setUseItemInHand(Event.Result.DENY);
							event.getPlayer().sendMessage(Main.PREFIX + "You do not have permission to look in this chest.");
							return;
						}
					} else {
						BlockFace[] faces = new BlockFace[] { BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };
						for (BlockFace bf : faces) {
							if (event.getClickedBlock().getRelative(bf).getType() == Material.CHEST) {
								ResultSet rs2 = Connection.query("SELECT id, COUNT(*) AS 'count', owner FROM chests WHERE x = " + event.getClickedBlock().getRelative(bf).getLocation().getBlockX() +
										" AND y = " + event.getClickedBlock().getRelative(bf).getLocation().getBlockY() +
										" AND z = " + event.getClickedBlock().getRelative(bf).getLocation().getBlockZ() + " LIMIT 1;", false);

								rs2.next();

								if ((rs2.getInt("count") > 0) && (!rs2.getString("owner").equalsIgnoreCase(event.getPlayer().getName())) && (!event.getPlayer().hasPermission("etriashop.break"))) {
									event.setCancelled(true);
									event.setUseInteractedBlock(Event.Result.DENY);
									event.setUseItemInHand(Event.Result.DENY);
									event.getPlayer().sendMessage(Main.PREFIX + "You do not have permission to look in this chest.");
									return;
								}
							}
						}
					}
				} catch (SQLException e) {}
			}

			if (selected.containsKey(event.getPlayer())) {
				if ((selected.get(event.getPlayer()).getAmount() > 0) && (!chatInput.containsKey(event.getPlayer())) && (event.getAction() == Action.LEFT_CLICK_BLOCK)) {
					if (event.getClickedBlock().getType() != Material.CHEST) return;

					ShopSign s = selected.get(event.getPlayer());

					Block block = event.getClickedBlock();

						boolean hasEnough = false;
						if (Config.fee == 0) hasEnough = true;
						else if (Econ.handler.has(event.getPlayer().getName(), Config.fee)) hasEnough = true;

						if (hasEnough && event.getPlayer().hasPermission("etriashop.nofee")) {
							if (s.linkChest((Chest)event.getClickedBlock().getState(), event.getPlayer().getName())) {
								event.getPlayer().sendMessage(Main.PREFIX + "You've successfully linked this chest to a shop, you were also exempt from the fee!");
								s.recheck();
								selected.remove(event.getPlayer());
							}
						} else if (hasEnough) {
							if (s.linkChest((Chest)event.getClickedBlock().getState(), event.getPlayer().getName())) {
								Econ.handler.withdrawPlayer(event.getPlayer().getName(), Config.fee);
								Econ.handler.depositPlayer(Config.fee_account, Config.fee);
								event.getPlayer().sendMessage(Main.PREFIX + "You've successfully linked this chest to a shop, and paid the§6 " + Econ.handler.format(Config.fee) + " §7fee!");
								s.recheck();
								selected.remove(event.getPlayer());
							}
						} else {
							event.getPlayer().sendMessage(Main.PREFIX + "There was an error in linking your chest. Please make sure you've set an item for the sign you're trying to link, and that this chest is not already linked by someone else.");
						}
					} else {
						event.getPlayer().sendMessage(Main.PREFIX + "You don't have enough money to create this shop. You need§6 " + Econ.handler.format(Config.fee) +
								" §7to create a shop. Please click the shop sign when you have enough money to restart the link process.");
						selected.remove(event.getPlayer());
					}
				} 
			}
		}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		if (!chatInput.containsKey(event.getPlayer())) return;

		if (event.getMessage().contains("cancel")) {
			event.getPlayer().sendMessage(Main.PREFIX + "You have cancelled item input. Click the sign to try again, or break it with an open fist.");
			chatInput.remove(event.getPlayer());
			event.setCancelled(true);
			return;
		}

		int amnt = 0;

		try {
			amnt = Integer.parseInt(event.getMessage().split(" ")[0]);
		} catch (NumberFormatException e) {}

		if (amnt > 64) amnt = 64;

		if (amnt > chatInput.get(event.getPlayer()).getFirst().getMaxStackSize()) {
			event.getPlayer().sendMessage(Main.PREFIX + "You can't handle more than the maximum stack size (" + chatInput.get(event.getPlayer()).getFirst().getMaxStackSize() + ") per transaction. Please try again.");
			event.setCancelled(true);
			return;
		}


		if (amnt > 0) {
			ShopSign s = selected.get(event.getPlayer());
			s.setItem(chatInput.get(event.getPlayer()).getFirst(), chatInput.get(event.getPlayer()).getSecond(), amnt);

			event.getPlayer().sendMessage(Main.PREFIX + "This sign will handle§6 " + amnt + " " + plugin.prettify(s.getItemName()) + " §7at a time.");
			if (s.getType() != ShopSign.Type.ATM) event.getPlayer().sendMessage(Main.PREFIX + "Please click the chest that will be linked with this sign.");
			else {
				selected.remove(event.getPlayer());
				s.setLine(0, "§b[§0ATM§b]");
			}

			chatInput.remove(event.getPlayer());
		} else {
			event.getPlayer().sendMessage(Main.PREFIX + "I'm sorry, I couldn't recognize that number. Please try again, or type §6cancel§7 to cancel.");
		}

		event.setCancelled(true);
	}

	private void buyATM(ShopSign s, Player p) {
		p.getInventory().addItem(new ItemStack(s.getItem(), s.getAmount(), (short)0, s.getData()));
		p.updateInventory();

		Econ.handler.withdrawPlayer(p.getName(), s.getBuyPrice());

		p.sendMessage(Main.PREFIX + "You §9bought§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getBuyPrice()) +
				" §7from§9 " + s.getOwner());
	}

	private void buy(ShopSign s, Player p) {
		p.getInventory().addItem(new ItemStack(s.getItem(), s.getAmount(), (short)0, s.getData()));
		p.updateInventory();
		s.removeFromChest();

		Econ.handler.withdrawPlayer(p.getName(), s.getBuyPrice());
		Econ.handler.depositPlayer(s.getOwner(), s.getBuyPrice());

		p.sendMessage(Main.PREFIX + "You §9bought§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getBuyPrice()) +
				" §7from§9 " + s.getOwner());
		Player owner = plugin.getServer().getPlayer(s.getOwner());
		if ((owner != null) && (owner.isOnline()) && (!ignoring.contains(owner.getName()))) owner.sendMessage(Main.PREFIX + "You sold§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getBuyPrice()) +
				" §7to§9 " + p.getName());
	}

	private void sellATM(ShopSign s, Player p) {
		boolean canSell = false;

		HashMap<Integer, ? extends ItemStack> hm = p.getInventory().all(s.getItem());

		for (Integer i : hm.keySet()) {
			ItemStack is = hm.get(i);
			if ((is.getType().getMaxDurability() > 0) && (is.getDurability() != (short)0)) continue;

			if (is.getData().getData() != s.getData()) continue;

			if (is.getEnchantments().size() > 0) continue;

			if (is.getAmount() < s.getAmount()) continue;

			canSell = true;
			break;
		}

		if (!canSell) {
			p.sendMessage(Main.PREFIX + "You don't have enough to sell.");
			return;
		}

		p.getInventory().removeItem(new ItemStack(s.getItem(), s.getAmount(), (short)0, s.getData()));
		p.updateInventory();

		Econ.handler.depositPlayer(p.getName(), s.getSellPrice());

		p.sendMessage(Main.PREFIX + "You §9sold§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getSellPrice()) +
				" §7to§9 " + s.getOwner());
	}

	private void sell(ShopSign s, Player p) {
		boolean canSell = false;

		HashMap<Integer, ? extends ItemStack> hm = p.getInventory().all(s.getItem());

		int cumulative = 0;

		for (Integer i : hm.keySet()) {
			ItemStack is = hm.get(i);
			if ((is.getType().getMaxDurability() > 0) && (is.getDurability() != (short)0)) continue;

			if (is.getData().getData() != s.getData()) continue;

			if (is.getEnchantments().size() > 0) continue;

			if (is.getAmount() < s.getAmount()) {
				cumulative += is.getAmount();
				continue;
			}

			canSell = true;
			break;
		}

		if (cumulative >= s.getAmount()) canSell = true;

		if (!canSell) {
			p.sendMessage(Main.PREFIX + "You don't have enough to sell.");
			return;
		}

		p.getInventory().removeItem(new ItemStack(s.getItem(), s.getAmount(), (short)0, s.getData()));
		p.updateInventory();
		s.addToChest();

		Econ.handler.depositPlayer(p.getName(), s.getSellPrice());
		Econ.handler.withdrawPlayer(s.getOwner(), s.getSellPrice());

		p.sendMessage(Main.PREFIX + "You §9sold§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getSellPrice()) +
				" §7to§9 " + s.getOwner());
		Player owner = plugin.getServer().getPlayer(s.getOwner());
		if ((owner != null) && (owner.isOnline()) && (!ignoring.contains(owner.getName()))) owner.sendMessage(Main.PREFIX + "You bought§6 " + s.getAmount() + " " + s.getItemName() + " §7for§6 " + Econ.handler.format(s.getSellPrice()) +
				" §7from§9 " + p.getName());
	}

}