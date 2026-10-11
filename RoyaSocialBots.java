package com.roya.socialbots;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public final class RoyaSocialBots extends JavaPlugin implements Listener {
    private NamespacedKey botKey, typeKey, visualKey;
    private final Map<String, Integer> animationTicks = new HashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        botKey = new NamespacedKey(this, "social_bot");
        typeKey = new NamespacedKey(this, "social_type");
        visualKey = new NamespacedKey(this, "social_visual");
        Bukkit.getPluginManager().registerEvents(this, this);
        for (String id : new String[]{"discord", "youtube"}) if (getConfig().getBoolean("bots." + id + ".enabled", true)) ensureBot(id);
        long interval = Math.max(1L, getConfig().getLong("settings.respawn-check-seconds", 10L)) * 20L;
        Bukkit.getScheduler().runTaskTimer(this, this::ensureBotsExist, interval, interval);
        Bukkit.getScheduler().runTaskTimer(this, this::animateBots, 2L, 2L);
        getLogger().info("RoyaSocialBots enabled. Resource-pack prompt is " + (getConfig().getString("resource-pack.url", "").isBlank() ? "not configured (set resource-pack.url)" : "configured"));
        // Prompt players who are already online after a plugin reload.
        Bukkit.getOnlinePlayers().forEach(player -> Bukkit.getScheduler().runTaskLater(this, () -> promptResourcePack(player), 20L));
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> promptResourcePack(event.getPlayer()), 40L);
    }

    private void promptResourcePack(Player player) {
        if (!getConfig().getBoolean("resource-pack.enabled", true)) return;
        String url = getConfig().getString("resource-pack.url", "").trim();
        if (!isValidHttpUrl(url)) return; // A public direct-download URL is required by Minecraft clients.
        String hash = getConfig().getString("resource-pack.sha1", "").trim();
        if (!hash.matches("(?i)[0-9a-f]{40}")) hash = "";
        String prompt = color(getConfig().getString("resource-pack.prompt", "&dحمّل حزمة ROYA لإظهار الشخصيات ثلاثية الأبعاد."));
        try {
            player.setResourcePack(url, hash, getConfig().getBoolean("resource-pack.required", false), net.kyori.adventure.text.Component.text(ChatColor.stripColor(prompt)));
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Could not prompt resource pack: " + ex.getMessage());
        }
    }

    private void ensureBotsExist() {
        for (String id : new String[]{"discord", "youtube"}) {
            if (getConfig().getBoolean("bots." + id + ".enabled", true)) ensureBot(id); else removeBot(id);
        }
    }

    private void ensureBot(String id) {
        Entity root = findBot(id);
        if (root != null && root.isValid()) {
            if (findVisual(id) == null) spawnVisual(id);
            return;
        }
        Location loc = configuredLocation(id);
        if (loc == null) { getLogger().warning("World for bot '" + id + "' not found; update config.yml."); return; }
        EntityType type = id.equals("youtube") ? EntityType.ALLAY : EntityType.VILLAGER;
        Entity entity = loc.getWorld().spawnEntity(loc, type);
        if (entity instanceof LivingEntity livingRoot) livingRoot.setAI(false); entity.setInvulnerable(true); entity.setPersistent(true); entity.setGravity(false);
        entity.setSilent(true); entity.setVelocity(new Vector(0, 0, 0));
        entity.setCustomName(color(getConfig().getString("bots." + id + ".name", id.toUpperCase(Locale.ROOT))));
        entity.setCustomNameVisible(true);
        entity.getPersistentDataContainer().set(botKey, PersistentDataType.BYTE, (byte)1);
        entity.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, id);
        if (entity instanceof LivingEntity living) living.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        if (entity instanceof Villager v) { v.setRemoveWhenFarAway(false); v.setCanPickupItems(false); v.setProfession(Villager.Profession.CLERIC); v.setVillagerLevel(5); }
        if (entity instanceof Allay a) a.setCanPickupItems(false);
        animationTicks.put(id, 0);
        spawnVisual(id);
    }

    private void spawnVisual(String id) {
        Location loc = configuredLocation(id); if (loc == null) return;
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta(); meta.setCustomModelData(id.equals("discord") ? 1001 : 1002); item.setItemMeta(meta);
        ItemDisplay display = (ItemDisplay) loc.getWorld().spawnEntity(loc.clone().add(0, id.equals("discord") ? 0.95 : 0.75, 0), EntityType.ITEM_DISPLAY);
        display.setItemStack(item); display.setInvulnerable(true); display.setPersistent(true); display.setGravity(false);
        display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
        display.setInterpolationDuration(2); display.setTeleportDuration(2);
        display.getPersistentDataContainer().set(botKey, PersistentDataType.BYTE, (byte)1);
        display.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, id);
        display.getPersistentDataContainer().set(visualKey, PersistentDataType.BYTE, (byte)1);
    }

    private void animateBots() {
        for (String id : new String[]{"discord", "youtube"}) {
            Entity root = findBot(id); ItemDisplay display = findVisual(id); Location anchor = configuredLocation(id);
            if (root == null || display == null || anchor == null) continue;
            int tick = animationTicks.getOrDefault(id, 0) + 2; animationTicks.put(id, tick);
            double bob = id.equals("youtube") ? 0.32 + Math.sin(tick / 7.0) * 0.28 : Math.abs(Math.sin(tick / 8.0)) * 0.10;
            float yaw = id.equals("youtube") ? (float)((tick * 2.8) % 360.0) : (float)(Math.sin(tick / 20.0) * 24.0);
            Location rootLoc = anchor.clone(); rootLoc.setYaw(yaw); rootLoc.setPitch(0); root.teleport(rootLoc); root.setVelocity(new Vector(0,0,0));
            Location visualLoc = anchor.clone().add(0, bob + (id.equals("discord") ? 0.95 : 0.75), 0); visualLoc.setYaw(yaw); display.teleport(visualLoc);
            if (tick % (id.equals("youtube") ? 24 : 48) == 0) {
                anchor.getWorld().spawnParticle(id.equals("youtube") ? Particle.END_ROD : Particle.HAPPY_VILLAGER,
                    anchor.clone().add(0, bob + 1.4, 0), id.equals("youtube") ? 3 : 4, 0.22, 0.24, 0.22, 0.005);
            }
        }
    }

    private Location configuredLocation(String id) {
        String p = "bots." + id + "."; World w = Bukkit.getWorld(getConfig().getString(p + "world", "world"));
        if (w == null) return null;
        return new Location(w, getConfig().getDouble(p + "x", 0.5), getConfig().getDouble(p + "y", 80), getConfig().getDouble(p + "z", 0.5));
    }
    private Entity findBot(String id) {
        for (World w : Bukkit.getWorlds()) for (Entity e : w.getEntities()) {
            String t = e.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
            if (id.equals(t) && e.getPersistentDataContainer().has(botKey, PersistentDataType.BYTE)
                    && !e.getPersistentDataContainer().has(visualKey, PersistentDataType.BYTE)) return e;
        } return null;
    }
    private ItemDisplay findVisual(String id) {
        for (World w : Bukkit.getWorlds()) for (Entity e : w.getEntities()) {
            if (!(e instanceof ItemDisplay d)) continue;
            String t = e.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
            if (id.equals(t) && e.getPersistentDataContainer().has(visualKey, PersistentDataType.BYTE)) return d;
        } return null;
    }
    private void removeBot(String id) {
        Entity e = findBot(id); if (e != null) e.remove(); ItemDisplay d = findVisual(id); if (d != null) d.remove(); animationTicks.remove(id);
    }

    @EventHandler public void onBotClick(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked(); String id = entity.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (id == null || !entity.getPersistentDataContainer().has(botKey, PersistentDataType.BYTE)
                || entity.getPersistentDataContainer().has(visualKey, PersistentDataType.BYTE)) return;
        event.setCancelled(true); String url = getConfig().getString("bots." + id + ".link", "");
        if (!isValidHttpUrl(url) || url.contains("CHANGE_ME")) { event.getPlayer().sendMessage(ChatColor.RED + "الرابط غير مضبوط. عدّل bots." + id + ".link في config.yml"); return; }
        TextComponent c = new TextComponent("اضغط لفتح رابط " + id + ": ");
        TextComponent clickable = new TextComponent(url);
        clickable.setColor(net.md_5.bungee.api.ChatColor.AQUA);
        clickable.setUnderlined(true);
        clickable.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        c.addExtra(clickable);
        event.getPlayer().spigot().sendMessage(c);
        event.getPlayer().sendMessage(ChatColor.GRAY + "إذا لم يفتح الرابط، اضغط على الرابط الأزرق في الدردشة.");
    }
    @EventHandler public void onBotDamage(EntityDamageEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(botKey, PersistentDataType.BYTE)) event.setCancelled(true);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("socialbots")) return false;
        if (!sender.hasPermission("royasocialbots.admin")) { sender.sendMessage(ChatColor.RED + "ليس لديك صلاحية استخدام هذا الأمر."); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            for (String id : new String[]{"discord", "youtube"}) { removeBot(id); if (getConfig().getBoolean("bots." + id + ".enabled", true)) ensureBot(id); }
            sender.sendMessage(ChatColor.GREEN + "تم تحديث إعدادات RoyaSocialBots."); return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set") && (args[1].equalsIgnoreCase("discord") || args[1].equalsIgnoreCase("youtube"))) {
            if (!(sender instanceof Player player)) { sender.sendMessage("هذا الأمر للاعب داخل اللعبة فقط."); return true; }
            String id = args[1].toLowerCase(Locale.ROOT); Location l = player.getLocation(); String p = "bots." + id + ".";
            getConfig().set(p + "world", l.getWorld().getName()); getConfig().set(p + "x", l.getX()); getConfig().set(p + "y", l.getY()); getConfig().set(p + "z", l.getZ()); saveConfig();
            removeBot(id); ensureBot(id); sender.sendMessage(ChatColor.GREEN + "تم تحديد موقع بوت " + id + " عند موقعك الحالي."); return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("link") && (args[1].equalsIgnoreCase("discord") || args[1].equalsIgnoreCase("youtube"))) {
            if (!isValidHttpUrl(args[2])) { sender.sendMessage(ChatColor.RED + "استخدم رابطاً يبدأ بـ https:// أو http://"); return true; }
            String id = args[1].toLowerCase(Locale.ROOT); getConfig().set("bots." + id + ".link", args[2]); saveConfig();
            sender.sendMessage(ChatColor.GREEN + "تم حفظ رابط " + id + "."); return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "/socialbots set <discord|youtube> - ضبط موقع البوت");
        sender.sendMessage(ChatColor.YELLOW + "/socialbots link <discord|youtube> <url> - ضبط الرابط");
        sender.sendMessage(ChatColor.YELLOW + "/socialbots reload - إعادة تحميل الإعدادات");
        return true;
    }

    private boolean isValidHttpUrl(String value) {
        try { URI u = URI.create(value); return ("https".equalsIgnoreCase(u.getScheme()) || "http".equalsIgnoreCase(u.getScheme())) && u.getHost() != null; }
        catch (Exception e) { return false; }
    }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
