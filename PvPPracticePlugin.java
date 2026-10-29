package com.interio.pvppractice;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class PvPPracticePlugin extends JavaPlugin {
    private final Map<String, Npc> bots = new HashMap<>();
    private final Map<String, String> difficulties = new HashMap<>();
    private boolean npcApiReady;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (Bukkit.getPluginManager().getPlugin("FancyNpcs") == null) {
            getLogger().severe("FancyNpcs was not found. Install a Java 21-compatible FancyNpcs build for Paper 1.21.11, then restart.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        // FancyNpcs documents that NPC registration should wait until the server has finished starting.
        Bukkit.getScheduler().runTaskLater(this, () -> npcApiReady = true, 220L);
        getLogger().info("PvPPractice enabled. NPC registration will be available shortly.");
    }

    @Override
    public void onDisable() {
        for (Npc npc : new ArrayList<>(bots.values())) {
            try {
                npc.removeForAll();
                FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
            } catch (Exception ex) {
                getLogger().warning("Could not clean up NPC " + npc.getData().getName() + ": " + ex.getMessage());
            }
        }
        bots.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender, label);
            return true;
        }
        if (!sender.hasPermission("pvppractice.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        if (!npcApiReady) {
            sender.sendMessage("§ePvPPractice is still waiting for FancyNpcs to finish loading. Try again in a few seconds.");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "mass_spawn" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("§cRun this command in-game."); return true; }
                int amount = 10;
                if (args.length >= 2) {
                    try { amount = Integer.parseInt(args[1]); }
                    catch (NumberFormatException ex) { sender.sendMessage("§cUsage: /" + label + " mass_spawn [amount]"); return true; }
                }
                int max = Math.max(1, getConfig().getInt("max-mass-spawn", 10));
                if (amount < 1 || amount > max) {
                    sender.sendMessage("§cChoose an amount from 1 to " + max + ".");
                    return true;
                }
                int created = 0;
                for (int i = 0; i < amount; i++) {
                    Location location = randomNearby(player.getLocation(), getConfig().getDouble("spawn-radius", 4.0));
                    String name = nextBotName("InterioBot");
                    if (createBot(name, player.getUniqueId(), location, randomSkin())) created++;
                }
                sender.sendMessage("§aSpawned " + created + " player-style practice NPC(s). §7Note: this build creates NPCs; combat AI is a separate next stage.");
            }
            case "spawn" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("§cRun this command in-game."); return true; }
                if (args.length < 2) { sender.sendMessage("§cUsage: /" + label + " spawn <name> [skin]"); return true; }
                String name = sanitizeName(args[1]);
                String skin = args.length >= 3 ? args[2] : randomSkin();
                if (createBot(name, player.getUniqueId(), player.getLocation(), skin)) sender.sendMessage("§aCreated NPC §f" + name + "§a.");
                else sender.sendMessage("§cAn NPC with that name already exists, or creation failed.");
            }
            case "remove", "despawn" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /" + label + " remove <name>"); return true; }
                removeBot(sender, args[1]);
            }
            case "removeall", "despawnall" -> {
                int count = bots.size();
                for (String name : new ArrayList<>(bots.keySet())) removeBotInternal(name);
                sender.sendMessage("§aRemoved " + count + " PvPPractice NPC(s).");
            }
            case "list" -> {
                if (bots.isEmpty()) sender.sendMessage("§7No PvPPractice NPCs are currently tracked.");
                else sender.sendMessage("§aActive NPCs (" + bots.size() + "): §f" + String.join("§7, §f", bots.keySet()));
            }
            case "skin" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /" + label + " skin <name> <playerSkinName>"); return true; }
                Npc npc = bots.get(args[1].toLowerCase(Locale.ROOT));
                if (npc == null) { sender.sendMessage("§cNo tracked NPC named " + args[1] + "."); return true; }
                npc.getData().setSkin(args[2]);
                npc.updateForAll();
                sender.sendMessage("§aUpdated skin for §f" + npc.getData().getName() + "§a. If it doesn't refresh immediately, reconnect or respawn the NPC.");
            }
            case "difficulty" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /" + label + " difficulty <name> <easy|normal|hard>"); return true; }
                String name = args[1].toLowerCase(Locale.ROOT);
                if (!bots.containsKey(name)) { sender.sendMessage("§cNo tracked NPC named " + args[1] + "."); return true; }
                String level = args[2].toLowerCase(Locale.ROOT);
                if (!List.of("easy", "normal", "hard").contains(level)) { sender.sendMessage("§cDifficulty must be easy, normal, or hard."); return true; }
                difficulties.put(name, level);
                sender.sendMessage("§aStored difficulty §f" + level + "§a for §f" + bots.get(name).getData().getName() + "§a. §7Combat AI will use this setting in a later build.");
            }
            case "follow", "duel", "target", "stop", "kit", "setspawn", "reload" -> sender.sendMessage("§eThat command is planned but not implemented in this starter build yet. The current build manages player-style NPCs only; it does not make them move or fight.");
            default -> help(sender, label);
        }
        return true;
    }

    private boolean createBot(String rawName, UUID creator, Location location, String skin) {
        String name = sanitizeName(rawName);
        String key = name.toLowerCase(Locale.ROOT);
        if (bots.containsKey(key)) return false;
        try {
            NpcData data = new NpcData(name, creator, location);
            data.setSkin(skin);
            data.setDisplayName("<aqua>" + name + "</aqua>");
            Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
            npc.setSaveToFile(false);
            FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
            npc.create();
            npc.spawnForAll();
            bots.put(key, npc);
            difficulties.put(key, "normal");
            return true;
        } catch (Exception ex) {
            getLogger().warning("Failed to create NPC " + name + ": " + ex.getMessage());
            return false;
        }
    }

    private void removeBot(CommandSender sender, String name) {
        if (removeBotInternal(name)) sender.sendMessage("§aRemoved NPC §f" + name + "§a.");
        else sender.sendMessage("§cNo PvPPractice NPC named " + name + ".");
    }

    private boolean removeBotInternal(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        Npc npc = bots.remove(key);
        difficulties.remove(key);
        if (npc == null) return false;
        try {
            npc.removeForAll();
            FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
        } catch (Exception ex) {
            getLogger().warning("Failed to remove NPC " + name + ": " + ex.getMessage());
        }
        return true;
    }

    private Location randomNearby(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return center;
        double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
        double distance = ThreadLocalRandom.current().nextDouble(1.5, Math.max(1.6, radius));
        double x = center.getX() + Math.cos(angle) * distance;
        double z = center.getZ() + Math.sin(angle) * distance;
        int y = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;
        return new Location(world, x, y, z, center.getYaw(), 0);
    }

    private String randomSkin() {
        List<String> skins = getConfig().getStringList("default-skins");
        if (skins.isEmpty()) return "Steve";
        return skins.get(ThreadLocalRandom.current().nextInt(skins.size()));
    }

    private String nextBotName(String prefix) {
        int i = 1;
        while (bots.containsKey((prefix + i).toLowerCase(Locale.ROOT))) i++;
        return prefix + i;
    }

    private String sanitizeName(String input) {
        String safe = input.replaceAll("[^A-Za-z0-9_-]", "");
        if (safe.isBlank()) safe = "PracticeBot";
        return safe.length() > 16 ? safe.substring(0, 16) : safe;
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage("§b§lPvPPractice §7- §fcommands");
        sender.sendMessage("§e/" + label + " mass_spawn [amount] §7- spawn 10 NPCs by default");
        sender.sendMessage("§e/" + label + " spawn <name> [skin] §7- spawn one player-style NPC");
        sender.sendMessage("§e/" + label + " remove <name> §7- remove an NPC");
        sender.sendMessage("§e/" + label + " removeall §7- remove all tracked NPCs");
        sender.sendMessage("§e/" + label + " list §7- list tracked NPCs");
        sender.sendMessage("§e/" + label + " skin <name> <player> §7- change NPC skin");
        sender.sendMessage("§e/" + label + " difficulty <name> <easy|normal|hard> §7- store AI difficulty");
        sender.sendMessage("§7AI commands like follow and duel are planned; NPCs do not fight yet.");
    }
}
