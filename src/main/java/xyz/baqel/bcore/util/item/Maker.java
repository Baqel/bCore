package xyz.baqel.bcore.util.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.Validate;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import xyz.baqel.bcore.util.chat.Chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class Maker implements Cloneable {

    private Material type;

    private int data;

    private int amount;

    private String title;

    private List<String> lore;

    private Color color;

    private HashMap<Enchantment, Integer> enchantments;

    private boolean unbreakable;

    public Maker(Material type) {
        this(type, 1);
    }

    public Maker(Material type, int amount) {
        this(type, amount, 0);
    }

    public Maker(Material type, int amount, int data) {
        this.lore = new ArrayList<>();
        this.type = type;
        this.amount = amount;
        this.data = data;
        this.enchantments = new HashMap<>();
    }

    public Maker(ItemStack item) {
        this.lore = new ArrayList<>();
        Validate.notNull(item);
        this.enchantments = new HashMap<>();
        this.type = item.getType();
        this.data = item.getDurability();
        this.amount = item.getAmount();
        if (item.hasItemMeta()) {
            if (item.getItemMeta().hasDisplayName()) {
                this.title = item.getItemMeta().getDisplayName();
            }
            if (item.getItemMeta().hasLore()) {
                this.lore = item.getItemMeta().getLore();
            }
        }
        if (item.getEnchantments() != null) {
            this.enchantments.putAll(item.getEnchantments());
        }
        if (item.getType().toString().toLowerCase().contains("leather") && item.getItemMeta() instanceof LeatherArmorMeta) {
            LeatherArmorMeta lam = (LeatherArmorMeta)item.getItemMeta();
            this.color = lam.getColor();
        }
    }

    public Maker(Maker item) {
        this(item.build());
    }

    public Maker setUnbreakable(boolean flag) {
        this.unbreakable = flag;
        return this;
    }

    public Maker addLore(String... lore) {
        for (String s : lore) {
            this.lore.add(Chat.formatMessages(s));
        }
        return this;
    }

    public Maker setBase64() {
        return this;
    }

    public Maker setTexture() {
        return this;
    }

    public Maker setData(int data) {
        this.data = data;
        return this;
    }

    public Maker setAmount(int amount) {
        this.amount = amount;
        return this;
    }

    public Maker setName(String title) {
        this.title = Chat.formatMessages(title);
        return this;
    }

    public Maker setLore(String... lore) {
        this.lore = Chat.formatMessages(Arrays.asList(lore));
        return this;
    }

    public Maker setSkullType(SkullType type) {
        Validate.notNull(type);
        this.setData(type.data);
        return this;
    }

    public List<String> getLore() {
        return this.lore;
    }

    public Maker setLore(List<String> list) {
        this.lore = Chat.formatMessages(list);
        return this;
    }

    public Material getType() {
        return this.type;
    }

    public Maker setType(Material type) {
        this.type = type;
        return this;
    }

    public Maker addEnchantment(Enchantment e, int level) {
        this.enchantments.put(e, level);
        return this;
    }

    public Maker setColor(Color c) {
        if (!this.type.toString().toLowerCase().contains("leather")) {
            throw new RuntimeException("Cannot set translate of non-leather items.");
        }
        this.color = c;
        return this;
    }

    public int getAmount() {
        return amount;
    }

    public Color getColor() {
        return color;
    }

    public int getData() {
        return data;
    }

    public HashMap<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public String getTitle() {
        return title;
    }

    public ItemStack build() {
        Validate.noNullElements(new Object[] { this.type, this.data, this.amount });
        ItemStack stack = new ItemStack(this.type, this.amount, (short)this.data);
        ItemMeta im = stack.getItemMeta();
        if (this.title != null && this.title != "") {
            im.setDisplayName(this.title);
        }
        if (this.lore != null && !this.lore.isEmpty()) {
            im.setLore(this.lore);
        }
        if (this.color != null && this.type.toString().toLowerCase().contains("leather")) {
            ((LeatherArmorMeta) im).setColor(this.color);
        }
        stack.setItemMeta(im);
        if (this.enchantments != null && !this.enchantments.isEmpty()) {
            stack.addUnsafeEnchantments(this.enchantments);
        }
        if (this.unbreakable) {
            ItemMeta meta = stack.getItemMeta();
            meta.spigot().setUnbreakable(true);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public Maker clone() {
        return new Maker(this);
    }

    public enum SkullType {

        SKELETON(0), WITHER_SKELETON(1), ZOMBIE(2), PLAYER(3), CREEPER(4);

        private int data;

        SkullType(int data) {
            this.data = data;
        }

        public int getData() {
            return this.data;
        }
    }
}