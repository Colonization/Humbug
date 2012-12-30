package com.untamedears.humbug;

import java.util.Iterator;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Humbug extends JavaPlugin implements Listener {
  public static void severe(String message) {
    log_.severe("[Humbug] " + message);
  }

  public static void warning(String message) {
    log_.warning("[Humbug] " + message);
  }

  public static void info(String message) {
    log_.info("[Humbug] " + message);
  }

  public static void debug(String message) {
    if (debug_log_) {
      log_.info("[Humbug] " + message);
    }
  }

  public static Humbug getPlugin() {
    return plugin_;
  }

  private static final Logger log_ = Logger.getLogger("Humbug");
  private static Humbug plugin_ = null;
  private static int max_golden_apple_stack_ = 1;

  // ================================================
  // Configuration

  private static boolean debug_log_ = false;
  private static boolean anvil_enabled_ = false;
  private static boolean ender_chest_enabled_ = false;
  private static boolean villager_trades_enabled_ = false;
  private static boolean wither_enabled_ = true;
  private static boolean wither_explosions_enabled_ = false;
  private static boolean wither_insta_break_enabled_ = false;
  private static boolean cobble_from_lava_enabled_ = false;
  private static boolean ench_book_craftable_ = false;
  // For Enchanted GOLDEN_APPLES
  private static boolean ench_gold_app_edible_ = false;
  private static boolean ench_gold_app_craftable_ = false;

  static {
    max_golden_apple_stack_ = Material.GOLDEN_APPLE.getMaxStackSize();
    if (max_golden_apple_stack_ > 64) {
      max_golden_apple_stack_ = 64;
    }
  }

  public Humbug() {}

  // ================================================
  // Villager Trading

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    if (villager_trades_enabled_) {
      return;
    }
    Entity npc = event.getRightClicked();
    if (npc == null) {
        return;
    }
    if (npc.getType() == EntityType.VILLAGER) {
      event.setCancelled(true);
    }
  }

  // ================================================
  // Anvil and Ender Chest usage

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (anvil_enabled_ &&
        ender_chest_enabled_) {
      return;
    }
    Action action = event.getAction();
    Material material = event.getClickedBlock().getType();
    boolean anvil = !anvil_enabled_ &&
                    action == Action.RIGHT_CLICK_BLOCK &&
                    material.equals(Material.ANVIL);
    boolean ender_chest = !ender_chest_enabled_ &&
                          action == Action.RIGHT_CLICK_BLOCK &&
                          material.equals(Material.ENDER_CHEST);
    if (anvil || ender_chest) {
      event.setCancelled(true);
    }
  }

  // ================================================
  // Wither Insta-breaking and Explosions

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    if (wither_insta_break_enabled_) {
      return;
    }
    Entity npc = event.getEntity();
    if (npc == null) {
        return;
    }
    EntityType npc_type = npc.getType();
    if (npc_type.equals(EntityType.WITHER)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    if (wither_explosions_enabled_) {
      return;
    }
    boolean leave_blocks_intact = false;
    Entity npc = event.getEntity();
    if (npc == null) {
        return;
    }
    EntityType npc_type = npc.getType();
    if ((npc_type.equals(EntityType.WITHER) ||
         npc_type.equals(EntityType.WITHER_SKULL))) {
      event.blockList().clear();
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    if (wither_enabled_) {
      return;
    }
    if (!event.getEntityType().equals(EntityType.WITHER)) {
      return;
    }
    event.setCancelled(true);
  }

  // ================================================
  // Enchanted Golden Apple

  public boolean isEnchantedGoldenApple(ItemStack item) {
    // Golden Apples are GOLDEN_APPLE with 0 durability
    // Enchanted Golden Apples are GOLDEN_APPLE with 1 durability
    if (item == null) {
      return false;
    }
    if (item.getDurability() != 1) {
      return false;
    }
    Material material = item.getType();
    return material.equals(Material.GOLDEN_APPLE);
  }

  public void replaceEnchantedGoldenApple(
      String player_name, ItemStack item, int inventory_max_stack_size) {
    if (!isEnchantedGoldenApple(item)) {
      return;
    }
    int stack_size = max_golden_apple_stack_;
    if (inventory_max_stack_size < max_golden_apple_stack_) {
      stack_size = inventory_max_stack_size;
    }
    info(String.format(
          "Replaced %d Enchanted with %d Normal Golden Apples for %s",
          item.getAmount(), stack_size, player_name));
    item.setDurability((short)0);
    item.setAmount(stack_size);
  }

  public void removeRecipies() {
    if (ench_gold_app_craftable_) {
      return;
    }
    Iterator<Recipe> it = getServer().recipeIterator();
    while (it.hasNext()) {
      Recipe recipe = it.next();
      ItemStack resulting_item = recipe.getResult();
      if ( // !ench_gold_app_craftable_ &&
          isEnchantedGoldenApple(resulting_item)) {
        it.remove();
        info("Enchanted Golden Apple Recipe disabled");
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST) // ignoreCancelled=false
  public void onPlayerInteractAll(PlayerInteractEvent event) {
    // The event when eating is cancelled before even LOWEST fires when the
    //  player clicks on AIR.
    if (ench_gold_app_edible_) {
      return;
    }
    Player player = event.getPlayer();
    Inventory inventory = player.getInventory();
    ItemStack item = event.getItem();
    replaceEnchantedGoldenApple(
        player.getName(), item, inventory.getMaxStackSize());
  }

  // ================================================
  // Enchanted Book

  public boolean isNormalBook(ItemStack item) {
    if (item == null) {
      return false;
    }
    Material material = item.getType();
    return material.equals(Material.BOOK);
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
  public void onPrepareItemEnchantEvent(PrepareItemEnchantEvent event) {
    if (ench_book_craftable_) {
        return;
    }
    ItemStack item = event.getItem();
    if (isNormalBook(item)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
  public void onEnchantItemEvent(EnchantItemEvent event) {
    if (ench_book_craftable_) {
        return;
    }
    ItemStack item = event.getItem();
    if (isNormalBook(item)) {
      event.setCancelled(true);
      Player player = event.getEnchanter();
      warning(
          "Prevented book enchant. This should not trigger. Watch player " +
          player.getName());
    }
  }

  // ================================================
  // Stop Cobble generation from lava+water

  private final BlockFace[] faces_ = new BlockFace[] {
      BlockFace.NORTH,
      BlockFace.SOUTH,
      BlockFace.EAST,
      BlockFace.WEST,
      BlockFace.UP,
      BlockFace.DOWN
    };


  private BlockFace WaterAdjacentLava(Block lava_block) {
    for (BlockFace face : faces_) {
      Block block = lava_block.getRelative(face);
      Material material = block.getType();
      if (material.equals(Material.WATER) ||
          material.equals(Material.STATIONARY_WATER)) {
        return face;
      }
    }
    return BlockFace.SELF;
  }

  public void ConvertLava(Block block) {
    int data = (int)block.getData();
    if (data == 0) {
      return;
    }
    Material material = block.getType();
    if (!material.equals(Material.LAVA) &&
        !material.equals(Material.STATIONARY_LAVA)) {
      return;
    }
    if (isLavaSourceNear(block, 3)) {
      return;
    }
    BlockFace face = WaterAdjacentLava(block);
    if (face == BlockFace.SELF) {
      return;
    }
    block.setType(Material.AIR);
  }

  public boolean isLavaSourceNear(Block block, int ttl) {
    int data = (int)block.getData();
    if (data == 0) {
      Material material = block.getType();
      if (material.equals(Material.LAVA) ||
          material.equals(Material.STATIONARY_LAVA)) {
        return true;
      }
    }
    if (ttl <= 0) {
      return false;
    }
    for (BlockFace face : faces_) {
      Block child = block.getRelative(face);
      if (isLavaSourceNear(child, ttl - 1)) {
        return true;
      }
    }
    return false;
  }

  public void LavaAreaCheck(Block block, int ttl) {
    ConvertLava(block);
    if (ttl <= 0) {
      return;
    }
    for (BlockFace face : faces_) {
      Block child = block.getRelative(face);
      LavaAreaCheck(child, ttl - 1);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onBlockPhysicsEvent(BlockPhysicsEvent event) {
    if (cobble_from_lava_enabled_) {
      return;
    }
    Block block = event.getBlock();
    LavaAreaCheck(block, 1);
  }

  // ================================================
  // General

  public void onEnable() {
    registerEvents();
    loadConfiguration();
    removeRecipies();
    plugin_ = this;
    info("Enabled");
  }

  public boolean isInitiaized() {
    return plugin_ != null;
  }

  private void registerEvents() {
    getServer().getPluginManager().registerEvents(this, this);
  }

  private void loadConfiguration() {
    reloadConfig();
    FileConfiguration config = getConfig();
    config.options().copyDefaults(true);
    debug_log_ = config.getBoolean(
        "debug", false);
    anvil_enabled_ = config.getBoolean(
        "anvil", anvil_enabled_);
    ender_chest_enabled_ = config.getBoolean(
        "ender_chest", ender_chest_enabled_);
    villager_trades_enabled_ = config.getBoolean(
        "villager_trades", villager_trades_enabled_);
    wither_enabled_ = config.getBoolean(
        "wither", wither_enabled_);
    wither_explosions_enabled_ = config.getBoolean(
        "wither_explosions", wither_explosions_enabled_);
    wither_insta_break_enabled_ = config.getBoolean(
        "wither_insta_break", wither_insta_break_enabled_);
    ench_gold_app_edible_ = config.getBoolean(
        "ench_gold_app_edible", ench_gold_app_edible_);
    ench_gold_app_craftable_ = config.getBoolean(
        "ench_gold_app_craftable", ench_gold_app_craftable_);
    cobble_from_lava_enabled_ = config.getBoolean(
        "cobble_from_lava", cobble_from_lava_enabled_);
    ench_book_craftable_ = config.getBoolean(
        "ench_book_craftable", ench_book_craftable_);
  }
}
