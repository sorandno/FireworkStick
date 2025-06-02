package sorandno.fireworkStick;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockIterator;

import java.util.*;

public class FireworkListener implements Listener {

    private final Plugin plugin;
    private static final NamespacedKey FIREWORK_WAND_KEY = new NamespacedKey("yourplugin", "firework_wand");
    private Material fireworkItem;

    private static final List<Color> PRETTY_COLORS = Arrays.asList(
            Color.RED, Color.ORANGE, Color.YELLOW, Color.LIME, Color.AQUA,
            Color.BLUE, Color.FUCHSIA, Color.PURPLE, Color.WHITE
    );

    private final Random random = new Random();

    private final Map<UUID, FireworkEffect.Type> playerShapeMap = new HashMap<>();
    private final Map<UUID, Integer> playerRadiusMap = new HashMap<>();

    private static final FireworkEffect.Type RANDOM_TYPE = null;

    private static final FireworkEffect.Type[] SHAPES = {
            FireworkEffect.Type.BALL,
            FireworkEffect.Type.BALL_LARGE,
            FireworkEffect.Type.BURST,
            FireworkEffect.Type.STAR,
            FireworkEffect.Type.CREEPER,
            RANDOM_TYPE
    };

    public FireworkListener(Plugin plugin) {
        this.plugin = plugin;
    }

    public static NamespacedKey getWandKey() {
        return FIREWORK_WAND_KEY;
    }

    private boolean isValidWand(ItemStack item) {
        String materialName = plugin.getConfig().getString("FireworkMaterial");
        fireworkItem = Material.valueOf(materialName);

        if (item == null || item.getType() != fireworkItem) return false;
        String itemName = plugin.getConfig().getString("FireworkItemName");
        ItemMeta meta = item.getItemMeta();
        return meta != null &&
                meta.hasDisplayName() &&
                ChatColor.stripColor(meta.getDisplayName()).equals(itemName) &&
                meta.getPersistentDataContainer().has(FIREWORK_WAND_KEY, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isValidWand(item)) return;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK: {
                FireworkEffect.Type shape = playerShapeMap.getOrDefault(uuid, FireworkEffect.Type.BALL);
                if (shape == null) {
                    shape = SHAPES[random.nextInt(SHAPES.length - 1)];
                }

                int radius = playerRadiusMap.getOrDefault(uuid, 0);

                BlockIterator iterator = new BlockIterator(player, plugin.getConfig().getInt("MaxRange"));
                Location baseLoc = null;
                while (iterator.hasNext()) {
                    var block = iterator.next();
                    if (block.getType() != Material.AIR) {
                        baseLoc = block.getLocation().add(0.5, 1, 0.5);
                        break;
                    }
                }
                if (baseLoc == null) return;

                double dx = (radius == 0) ? 0 : random.nextInt(radius * 2 + 1) - radius;
                double dz = (radius == 0) ? 0 : random.nextInt(radius * 2 + 1) - radius;
                Location fireworkLoc = baseLoc.clone().add(dx, 0, dz);

                Firework firework = fireworkLoc.getWorld().spawn(fireworkLoc, Firework.class);
                FireworkMeta fwMeta = firework.getFireworkMeta();

                Collections.shuffle(PRETTY_COLORS);
                List<Color> selectedColors = PRETTY_COLORS.subList(0, 1 + random.nextInt(5));

                fwMeta.addEffect(FireworkEffect.builder()
                        .with(shape)
                        .withColor(selectedColors)
                        .withFade(selectedColors)
                        .trail(true)
                        .flicker(true)
                        .build());

                fwMeta.addEffect(FireworkEffect.builder()
                        .with(shape)
                        .withColor(Color.WHITE)
                        .flicker(true)
                        .build());

                fwMeta.setPower(0);
                firework.setFireworkMeta(fwMeta);
                break;
            }

            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK: {
                FireworkEffect.Type current = playerShapeMap.getOrDefault(uuid, SHAPES[0]);
                int index = Arrays.asList(SHAPES).indexOf(current);
                FireworkEffect.Type next = SHAPES[(index + 1) % SHAPES.length];
                playerShapeMap.put(uuid, next);

                String name = (next == null) ? "RANDOM" : next.name();
                player.sendActionBar("§d花火の形を切り替えました：§f" + name);
                break;
            }
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isValidWand(item)) return;

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        int radius = playerRadiusMap.getOrDefault(uuid, 0);
        radius = (radius + 1) % (plugin.getConfig().getInt("LaunchPointMaxRange") + 1);
        playerRadiusMap.put(uuid, radius);

        player.sendActionBar("§e花火発射範囲：半径 §a" + radius + " マス");
    }
}