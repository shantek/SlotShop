package io.shantek.helpers;

import org.bukkit.inventory.ItemStack;

public class ChosenItem {
    private final int slot;
    private final ItemStack item;

    public ChosenItem(int slot, ItemStack item) {
        this.slot = slot;
        this.item = item;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getItem() {
        return item;
    }
}
