/*
 * This code is not part of the Movecraft project but belongs to NuclearW
 *
 *  Attribtuion : http://forums.bukkit.org/threads/cardboard-serializable-itemstack-with-enchantments.75768/
 */

package net.countercraft.movecraft.utils.external;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A serializable ItemStack
 */
public class CardboardBox implements Serializable {
	private static final long serialVersionUID = 729890133797629668L;

	private final int type, amount;
	private final short damage;
	private final String name;

	private final HashMap<CardboardEnchantment, Integer> enchants;

	@SuppressWarnings("deprecation")
	public CardboardBox(ItemStack item) {
		this.type = item.getTypeId();
		this.amount = item.getAmount();
		this.damage = item.getDurability();
		if(item.hasItemMeta()){
			ItemMeta m = item.getItemMeta();
			this.name = m.getDisplayName();
		} else {
			this.name = "";
		}
		HashMap<CardboardEnchantment, Integer> map = new HashMap<CardboardEnchantment, Integer>();

		Map<Enchantment, Integer> enchantments = item.getEnchantments();

		for(Enchantment enchantment : enchantments.keySet()) {
			map.put(new CardboardEnchantment(enchantment), enchantments.get(enchantment));
		}

		this.enchants = map;
	}

	public ItemStack unbox() {
		@SuppressWarnings("deprecation")
		ItemStack item = new ItemStack(type, amount, damage);

		HashMap<Enchantment, Integer> map = new HashMap<Enchantment, Integer>();

		for(CardboardEnchantment cEnchantment : enchants.keySet()) {
			map.put(cEnchantment.unbox(), enchants.get(cEnchantment));
		}

		item.addUnsafeEnchantments(map);

		return item;
	}
}