package sorandno.fireworkStick;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class GetFireworkStickCommand implements CommandExecutor {
    private final Plugin plugin;
    public GetFireworkStickCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fireworkstick.commands.getfireworkstick")) {
            sender.sendMessage("§4パーミッションを持っていません!");
            return false;
        }
        if (command.getName().equalsIgnoreCase("getfireworkstick")) { //親コマンドの判定
            if (args.length == 0) { //サブコマンドの個数が0、つまりサブコマンド無し
                Player p = Bukkit.getPlayer(sender.getName());
                String materialName = plugin.getConfig().getString("FireworkMaterial");
                Material fireworkItem = Material.valueOf(materialName);
                ItemStack items = new ItemStack(fireworkItem);
                ItemMeta meta = items.getItemMeta(); // ItemMetaを取得
                String itemName = plugin.getConfig().getString("FireworkItemName");
                meta.setDisplayName(itemName); // 名前を設定（カラーコードも使える）
                // NBTタグ（PDC）を付ける
                NamespacedKey key = FireworkListener.getWandKey();
                meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
                items.setItemMeta(meta); // ItemMetaをItemStackに反映
                p.getPlayer().getInventory().addItem(items);
                return true;
            }
        }
        return false;
    }
}
