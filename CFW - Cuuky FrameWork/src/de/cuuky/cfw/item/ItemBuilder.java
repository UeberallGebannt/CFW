package de.cuuky.cfw.item;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import de.cuuky.cfw.utils.JavaUtils;
import de.cuuky.cfw.version.BukkitVersion;
import de.cuuky.cfw.version.VersionUtils;
import de.cuuky.cfw.version.types.Materials;

public class ItemBuilder {

	private static Method addFlagMethod;
	private static String[] attributes;
	private static Class<?> itemFlagClass;
	private static Object[] itemFlags;

	static {
		try {
			loadReflections();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int amount;
	private String displayName;
	private List<String> lore;
	private Map<Enchantment, Integer> enchantments;
	private String playerName;
	private ItemStack stack;
	private Material material;

	public ItemBuilder() {
		amount = 1;
	}

	public ItemBuilder amount(int amount) {
		this.amount = amount;
		return this;
	}

	public ItemStack build() {
		if (stack == null)
			stack = new ItemStack(this.material);

		ItemMeta stackMeta = stack.getItemMeta();
		if (displayName != null && stack.getType() != Material.AIR)
			stackMeta.setDisplayName(displayName);

		if (lore != null)
			stackMeta.setLore(lore);

		if (enchantments != null)
			for (Enchantment ent : enchantments.keySet())
				stackMeta.addEnchant(ent, enchantments.get(ent), true);

		this.stack.setItemMeta(stackMeta);
		// this.deleteDamageAnnotation();
		this.stack.setAmount(amount);
		return stack;
	}

	public ItemStack buildSkull() {
		stack = Materials.PLAYER_HEAD.parseItem();
		SkullMeta skullMeta = (SkullMeta) stack.getItemMeta();

		skullMeta.setDisplayName(displayName != null ? displayName : playerName);
		skullMeta.setOwner(playerName != null ? playerName : displayName);

		if (lore != null)
			skullMeta.setLore(lore);

		stack.setItemMeta(skullMeta);
		stack.setAmount(amount);

		return stack;
	}

	public ItemBuilder addEnchantment(Enchantment enchantment, int amplifier) {
		if (enchantments == null)
			enchantments = new HashMap<>();

		enchantments.put(enchantment, amplifier);
		return this;
	}

	public ItemBuilder deleteDamageAnnotation() {
		ItemMeta meta = stack.getItemMeta();
		if (!VersionUtils.getVersion().isHigherThan(BukkitVersion.ONE_7)) {
			// for(Enchantment key : meta.getEnchants().keySet()) {
			// meta.removeEnchant(key);
			// }

			// ItemStack item = stack.clone();
			// net.minecraft.server.v1_7_R4.ItemStack nmsStack =
			// CraftItemStack.asNMSCopy(item);
			// NBTTagCompound tag;
			// if(!nmsStack.hasTag()) {
			// tag = new NBTTagCompound();
			// nmsStack.setTag(tag);
			// } else {
			// tag = nmsStack.getTag();
			// }
			// NBTTagList am = new NBTTagList();
			// tag.set("AttributeModifiers", (NBTBase) am);
			// nmsStack.setTag(tag);
			//
			// this.stack = (ItemStack)CraftItemStack.asCraftMirror(nmsStack);
			// TODO Hide other attributes?
		} else {
			// Reflections for errorless display of the menu for 1.7
			try {
				for (Object obj : itemFlags) {
					Object[] s = (Object[]) Array.newInstance(itemFlagClass, 1);
					Array.set(s, 0, obj);

					addFlagMethod.invoke(meta, new Object[] { s });
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		stack.setItemMeta(meta);
		return this;
	}

	public ItemBuilder material(Material material) {
		this.material = material;
		return this;
	}

	public ItemBuilder displayname(String displayname) {
		this.displayName = displayname;
		return this;
	}

	public ItemBuilder itemstack(ItemStack stack) {
		this.stack = stack;
		return this;
	}

	public ItemBuilder lore(ArrayList<String> lore) {
		this.lore = lore;
		return this;
	}

	public ItemBuilder lore(String lore) {
		this.lore = JavaUtils.collectionToArray(new String[] { lore });
		return this;
	}

	public ItemBuilder lore(String... lore) {
		this.lore = JavaUtils.collectionToArray(lore);
		return this;
	}

	public ItemBuilder player(Player player) {
		this.playerName = player.getName();
		return this;
	}

	public ItemBuilder playername(String playername) {
		this.playerName = playername;
		return this;
	}

	private static void loadReflections() throws Exception {
		if (VersionUtils.getVersion().isHigherThan(BukkitVersion.ONE_7)) {
			itemFlagClass = Class.forName("org.bukkit.inventory.ItemFlag");

			attributes = new String[] { "HIDE_ATTRIBUTES", "HIDE_DESTROYS", "HIDE_ENCHANTS", "HIDE_PLACED_ON", "HIDE_POTION_EFFECTS", "HIDE_UNBREAKABLE" };
			itemFlags = new Object[attributes.length];

			for (int i = 0; i < attributes.length; i++) {
				try {
					itemFlags[i] = itemFlagClass.getDeclaredField(attributes[i]).get(null);
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
					e.printStackTrace();
				}
			}

			try {
				addFlagMethod = Class.forName("org.bukkit.inventory.meta.ItemMeta").getDeclaredMethod("addItemFlags", Array.newInstance(itemFlagClass, 1).getClass());
				addFlagMethod.setAccessible(true);
			} catch (NoSuchMethodException | SecurityException | NegativeArraySizeException e) {
				e.printStackTrace();
			}
		}
	}
}