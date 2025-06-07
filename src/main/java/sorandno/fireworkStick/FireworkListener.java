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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.*;

public class FireworkListener implements Listener {

    private final Plugin plugin;
    private final List<ParticlePattern> patterns;
    private final List<String> patternNames;
    public FireworkListener(Plugin plugin, List<ParticlePattern> patterns, List<String> patternNames) {
        this.plugin = plugin;
        this.patterns = patterns;
        this.patternNames = patternNames;

    }

    private static final NamespacedKey FIREWORK_WAND_KEY = new NamespacedKey("fireworckstick", "firework_wand");
    public static NamespacedKey getWandKey() {
        return FIREWORK_WAND_KEY;
    }

    private static final List<Color> PRETTY_COLORS = Arrays.asList(
            Color.RED, Color.ORANGE, Color.YELLOW, Color.LIME, Color.AQUA,
            Color.BLUE, Color.FUCHSIA, Color.PURPLE, Color.WHITE
    );

    private static final FireworkEffect.Type[] SHAPES = {
            FireworkEffect.Type.BALL,
            FireworkEffect.Type.BALL_LARGE,
            FireworkEffect.Type.BURST,
            FireworkEffect.Type.STAR,
            FireworkEffect.Type.CREEPER
    };

    private final Random random = new Random();
    private final Map<UUID, Integer> playerRadiusMap = new HashMap<>();
    private final Map<UUID, Integer> playerSelectedPattern = new HashMap<>();

    private boolean isValidWand(ItemStack item) {
        String materialName = plugin.getConfig().getString("FireworkMaterial");
        Material fireworkItem = Material.valueOf(materialName);

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
                int mode = playerSelectedPattern.getOrDefault(uuid, 0);

                // 発射地点の取得
                BlockIterator iterator = new BlockIterator(player, plugin.getConfig().getInt("MaxRange"));
                Location baseLoc = null;
                while (iterator.hasNext()) {
                    var block = iterator.next();
                    if (block.getType() != Material.AIR) {
                        baseLoc = block.getLocation().add(0.5, 1, 0.5);
                        break;
                    }
                }
                if (baseLoc == null) {
                    player.sendMessage("§c発射地点が見つかりません。");
                    return;
                }

                int totalShapes = SHAPES.length;      // 通常花火数（ランダム含む）
                int totalPatterns = patterns.size(); // パターン数

                int radius = playerRadiusMap.getOrDefault(uuid, 0);
                double dx = 0, dz = 0;
                if (radius > 0) {
                    dx = random.nextInt(radius * 2 + 1) - radius;
                    dz = random.nextInt(radius * 2 + 1) - radius;
                }
                Location fireworkLoc = baseLoc.clone().add(dx, 0, dz);

                if (mode < totalShapes) {
                    // 通常花火（順番に）発射
                    FireworkEffect.Type shape = SHAPES[mode];

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

                    fwMeta.setPower(0);
                    firework.setFireworkMeta(fwMeta);
                } else if (mode < totalShapes + totalPatterns) {
                    // パターン（順番に）発射
                    int patternIdx = mode - totalShapes;
                    if (patterns.isEmpty()) {
                        player.sendMessage("§cパターンが読み込まれていません。");
                        return;
                    }
                    ParticlePattern pattern = patterns.get(patternIdx);
                    shootPatternBeam(player, pattern, fireworkLoc);
                } else {
                    // ランダムモード3種類のいずれか
                    int randomModeIdx = mode - totalShapes - totalPatterns;

                    if (randomModeIdx == 0) {
                        // ランダム通常花火
                        FireworkEffect.Type shape = SHAPES[random.nextInt(SHAPES.length)];

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

                        fwMeta.setPower(0);
                        firework.setFireworkMeta(fwMeta);
                    } else if (randomModeIdx == 1) {
                        // ランダムパターンのみ
                        if (patterns.isEmpty()) {
                            player.sendMessage("§cパターンが読み込まれていません。");
                            return;
                        }
                        ParticlePattern pattern = patterns.get(random.nextInt(patterns.size()));
                        shootPatternBeam(player, pattern, fireworkLoc);
                    } else {
                        // ランダム 通常花火 or パターン混合
                        int choice = random.nextInt(totalShapes + totalPatterns);

                        if (choice < patterns.size()) {
                            ParticlePattern pattern = patterns.get(random.nextInt(patterns.size()));
                            shootPatternBeam(player, pattern, fireworkLoc);
                        } else {
                            FireworkEffect.Type shape = SHAPES[random.nextInt(SHAPES.length)];

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

                            fwMeta.setPower(0);
                            firework.setFireworkMeta(fwMeta);
                        }
                    }
                }

                event.setCancelled(true);
                break;
            }

            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK: {
                int totalShapes = SHAPES.length;
                int totalPatterns = patterns.size();
                int totalRandomModes = 3; // ランダム通常花火, ランダムパターン, ランダム通常花火orパターン

                int maxIndex = totalShapes + totalPatterns + totalRandomModes - 1;

                int mode = playerSelectedPattern.getOrDefault(uuid, -1);
                int nextMode = mode + 1;
                if (nextMode > maxIndex) nextMode = 0;

                playerSelectedPattern.put(uuid, nextMode);

                if (nextMode < totalShapes) {
                    FireworkEffect.Type shape = SHAPES[nextMode];
                    player.sendActionBar("§d形状切替: " + shape.name());
                } else if (nextMode < totalShapes + totalPatterns) {
                    int patternIndex = nextMode - totalShapes;
                    player.sendActionBar("§dパターン切替: " + (patternIndex + 1) + "：" + patternNames.get(patternIndex));
                } else {
                    int randomIndex = nextMode - totalShapes - totalPatterns;
                    if (randomIndex == 0) {
                        player.sendActionBar("§dランダム: 通常花火");
                    } else if (randomIndex == 1) {
                        player.sendActionBar("§dランダム: パターンのみ");
                    } else {
                        player.sendActionBar("§dランダム: 通常花火 or パターン");
                    }
                }
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

    private void shootPatternBeam(Player player, ParticlePattern pattern, Location fwLoc) {
        Location eyeLoc = player.getEyeLocation();
        org.bukkit.util.Vector directionEye = eyeLoc.getDirection().normalize();

        Location start = fwLoc.clone().add(0, 1, 0);       // 出発点（プレイヤーの目）
        Location target = start.clone().add(0.0001, 20, 0);
        org.bukkit.util.Vector direction = target.toVector().subtract(start.toVector()).normalize();

        org.bukkit.util.Vector right = directionEye.clone().crossProduct(new org.bukkit.util.Vector(0, 1, 0)).normalize();
        org.bukkit.util.Vector up = right.clone().crossProduct(directionEye).normalize();

        int index = random.nextInt(PRETTY_COLORS.size());

        final double stepSize = 0.5;
        final int maxSteps = (int)(64 / stepSize);

        eyeLoc.getWorld().playSound(eyeLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        fwLoc.getWorld().playSound(fwLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);

        new BukkitRunnable() {
            int step = 0;
            private final int stepi = 10+random.nextInt(10);

            @Override
            public void run() {
                if (step >= maxSteps) {
                    cancel();
                    return;
                }

                if (step < stepi) {
                    org.bukkit.util.Vector advance = direction.clone().multiply(step * stepSize);
                    Location base = fwLoc.clone().add(advance);
                    base.getWorld().spawnParticle(Particle.FLAME, base, 5, 0.1, 0.1, 0.1, 0.01);

                } else {
                    if (step == stepi) {
                        eyeLoc.getWorld().playSound(eyeLoc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 1.0f, 1.0f);
                        fwLoc.getWorld().playSound(fwLoc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 1.0f, 1.0f);
                    }

                    double fwSize;
                    if (step<stepi+20) {
                        fwSize = (step - stepi) * 0.01;
                    } else {
                        fwSize = (32 - stepi) * 0.01;
                    }
                    pattern.centerPattern( fwSize );

                    org.bukkit.util.Vector advance = direction.clone().multiply(stepi * stepSize);
                    Location base = fwLoc.clone().add(advance);

                    List<PatternPoint> patternPoints = pattern.getPattern();
                    for (PatternPoint p : patternPoints) {
                        Vector offset = right.clone().multiply(p.x).add(up.clone().multiply(p.y));
                        Location particleLoc = base.clone().add(offset);

                        Particle.DustOptions dust = new Particle.DustOptions(PRETTY_COLORS.get(index), 1.0f);
                        particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, dust);
                    }
                    step+=2;
                }

                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}