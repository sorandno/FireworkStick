package sorandno.fireworkStick;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ParticlePattern {
    private final List<PatternPoint> rawPoints = new ArrayList<>();
    private final List<PatternPoint> centeredPoints = new ArrayList<>();
    private final JavaPlugin plugin;

    public ParticlePattern(JavaPlugin plugin, File file) {
        this.plugin = plugin;
        loadPatternFromFile(file);
        centerPattern(0.1);  // インスタンス生成時に自動で中心化
    }

    public List<PatternPoint> getPattern() {
        return centeredPoints;
    }

    private void loadPatternFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                rawPoints.add(new PatternPoint(x, y));
            }
            plugin.getLogger().info("Loaded " + rawPoints.size() + " pattern points from " + file.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load pattern file: " + e.getMessage());
        }
    }

    public void centerPattern(double size) {
        double maxX = rawPoints.stream().mapToDouble(p -> p.x).max().orElse(0.0);
        double minX = rawPoints.stream().mapToDouble(p -> p.x).min().orElse(0.0);
        double maxY = rawPoints.stream().mapToDouble(p -> p.y).max().orElse(0.0);
        double minY = rawPoints.stream().mapToDouble(p -> p.y).min().orElse(0.0);

        double offsetX = (maxX + minX) / 2.0;
        double offsetY = (maxY + minY) / 2.0;

        centeredPoints.clear();
        for (PatternPoint p : rawPoints) {
            double centeredX = (p.x - offsetX) * size;
            double centeredY = -(p.y - offsetY) * size;
            centeredPoints.add(new PatternPoint(centeredX, centeredY));
        }
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}