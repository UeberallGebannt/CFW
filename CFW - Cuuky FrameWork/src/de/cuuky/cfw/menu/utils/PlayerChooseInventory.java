package de.cuuky.cfw.menu.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import de.cuuky.cfw.CuukyFrameWork;
import de.cuuky.cfw.item.ItemBuilder;
import de.cuuky.cfw.utils.JavaUtils;
import de.cuuky.cfw.utils.listener.InventoryClickUtil;
import de.cuuky.cfw.version.VersionUtils;
import de.cuuky.cfw.version.types.Materials;

public class PlayerChooseInventory {

	/**
	 * 
	 * @author Cuuky This definitely needs a recode
	 *
	 */

	public class PlayerAddEvent {

		private String displayName;
		private String[] lore;
		private Player toAdd;

		public PlayerAddEvent(Player toAdd) {
			this.toAdd = toAdd;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String[] getLore() {
			return lore;
		}

		public Player getToAdd() {
			return toAdd;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public void setLore(String[] lore) {
			this.lore = lore;
		}
	}

	public class PlayerChooseEvent {

		private boolean all;
		private Player choosen;

		public PlayerChooseEvent(Player choosen, boolean all) {
			this.choosen = choosen;
		}

		public boolean chosenAll() {
			return all;
		}

		public Player getChoosen() {
			return choosen;
		}
	}

	public interface PlayerChooseHandler {

		public void onPlayerChoose(PlayerChooseEvent event);

	}

	public interface PlayerInventoryHandler {

		public void onPlayerInventoryAdd(PlayerAddEvent event);

	}

	private CuukyFrameWork instance;

	@SuppressWarnings("unused")
	private PlayerChooseHandler chooseHandler;
	private Inventory inv;
	private PlayerInventoryHandler invHandler;
	private Listener listener;

	private int page;

	private Player player;

	private Player[] players;

	private int size;

	public PlayerChooseInventory(Player player, Player[] players, PlayerChooseHandler handler, CuukyFrameWork instance) {
		this(player, players, handler, null, instance);
	}

	public PlayerChooseInventory(Player player, Player[] players, PlayerChooseHandler handler, PlayerInventoryHandler invHandler, CuukyFrameWork instance) {
		this.player = player;
		this.chooseHandler = handler;
		this.size = 54;
		this.page = 1;
		this.invHandler = invHandler;
		this.players = players;
		this.instance = instance;

		addListener(handler);

		open();
	}

	private void addListener(PlayerChooseHandler handler) {
		this.listener = new Listener() {

			@EventHandler
			public void onInventoryClick(InventoryClickEvent event) {
				Inventory inventory = new InventoryClickUtil(event).getInventory();
				if (inventory == null || event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null || event.getCurrentItem().getItemMeta().getDisplayName() == null)
					return;

				if (!inv.equals(event.getInventory()))
					return;

				String displayname = event.getCurrentItem().getItemMeta().getDisplayName();

				if (displayname.contains("Backwards") || displayname.contains("Forwards")) {
					page = displayname.contains("Backwards") ? Integer.valueOf(displayname.split("| ")[1]) - 1 : Integer.valueOf(displayname.split("§8| §7")[1]) + 1;
					open();
					return;
				}

				event.setCancelled(true);

				Player choosen = instance.getPluginInstance().getServer().getPlayerExact(displayname.replaceFirst("§c", ""));
				handler.onPlayerChoose(new PlayerChooseEvent(choosen, displayname.equals("§aChoose all")));

				player.closeInventory();
			}

			@EventHandler
			public void onInventoryClose(InventoryCloseEvent event) {
				if (!inv.equals(event.getInventory()))
					return;

				handler.onPlayerChoose(new PlayerChooseEvent(null, false));
			}
		};

		instance.getPluginInstance().getServer().getPluginManager().registerEvents(listener, this.instance.getPluginInstance());
	}

	private void open() {
		inv = Bukkit.createInventory(null, 54, "§cChoose a player §8| §7" + page);

		int start = size * (page - 1);
		boolean notEnough = false;
		Player[] players = this.players != null ? this.players : (Player[]) VersionUtils.getOnlinePlayer().toArray();
		for (int i = 0; i != (size - 3); i++) {
			Player player;

			try {
				player = (Player) players[start];
			} catch (IndexOutOfBoundsException e) {
				break;
			}

			ItemStack stack = new ItemBuilder().player(player).build();
			if (invHandler != null) {
				PlayerAddEvent event = new PlayerAddEvent(player);
				invHandler.onPlayerInventoryAdd(event);
				if (event.getDisplayName() != null)
					stack.getItemMeta().setDisplayName(event.getDisplayName());

				if (event.getLore() != null)
					stack.getItemMeta().setLore(JavaUtils.collectionToArray(event.getLore()));
			}

			inv.setItem(start, stack);
			start++;

			if (start == size - 3)
				notEnough = true;
		}

		inv.setItem(51, new ItemBuilder().displayname("§aChoose all").itemstack(new ItemStack(Materials.SKELETON_SKULL.parseMaterial())).build());

		if (notEnough)
			inv.setItem(53, new ItemBuilder().displayname("§aForwards").itemstack(new ItemBuilder().playername("MHF_ArrowRight").buildSkull()).build());

		if (page != 1)
			inv.setItem(52, new ItemBuilder().displayname("§cBackwards").itemstack(new ItemBuilder().playername("MHF_ArrowLeft").buildSkull()).build());

		this.player.openInventory(inv);
	}
}