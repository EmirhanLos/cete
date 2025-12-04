package com.emirhan.gangs.manager;

import com.emirhan.gangs.GangsPlugin;
import com.emirhan.gangs.gang.Gang;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GangManager {

    private final GangsPlugin plugin;
    private final Map<String, Gang> gangsById;
    private final Map<UUID, String> playerGang;

    private File dataFile;
    private YamlConfiguration dataConfig;

    public GangManager(GangsPlugin plugin) {
        this.plugin = plugin;
        this.gangsById = new HashMap<>();
        this.playerGang = new HashMap<>();
        setupFile();
    }

    private void setupFile() {
        dataFile = new File(plugin.getDataFolder(), "gangs.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("gangs.yml oluşturulamadı!");
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadGangs() {
        gangsById.clear();
        playerGang.clear();

        ConfigurationSection gangsSection = dataConfig.getConfigurationSection("gangs");
        if (gangsSection == null) return;

        for (String id : gangsSection.getKeys(false)) {
            ConfigurationSection section = gangsSection.getConfigurationSection(id);
            if (section == null) continue;

            String name = section.getString("name");
            String leaderStr = section.getString("leader");
            if (name == null || leaderStr == null) continue;

            UUID leader = UUID.fromString(leaderStr);
            Gang gang = new Gang(id, name, leader);

            gang.setFriendlyFire(section.getBoolean("friendlyFire", false));
            gang.deposit(section.getDouble("bank", 0));
            int kills = section.getInt("kills", 0);
            int deaths = section.getInt("deaths", 0);
            for (int i = 0; i < kills; i++) gang.addKill();
            for (int i = 0; i < deaths; i++) gang.addDeath();

            List<String> members = section.getStringList("members");
            for (String m : members) {
                UUID uuid = UUID.fromString(m);
                gang.getMembers().add(uuid);
                playerGang.put(uuid, id);
            }

            List<String> moderators = section.getStringList("moderators");
            for (String m : moderators) {
                UUID uuid = UUID.fromString(m);
                gang.getModerators().add(uuid);
            }

            gangsById.put(id.toLowerCase(), gang);
        }

        plugin.getLogger().info("Toplam " + gangsById.size() + " çete yüklendi.");
    }

    public void saveGangs() {
        dataConfig.set("gangs", null);

        for (Gang gang : gangsById.values()) {
            String path = "gangs." + gang.getId() + ".";
            dataConfig.set(path + "name", gang.getName());
            dataConfig.set(path + "leader", gang.getLeader().toString());
            dataConfig.set(path + "friendlyFire", gang.isFriendlyFire());
            dataConfig.set(path + "bank", gang.getBank());
            dataConfig.set(path + "kills", gang.getKills());
            dataConfig.set(path + "deaths", gang.getDeaths());

            List<String> members = new ArrayList<>();
            for (UUID uuid : gang.getMembers()) {
                members.add(uuid.toString());
            }
            dataConfig.set(path + "members", members);

            List<String> moderators = new ArrayList<>();
            for (UUID uuid : gang.getModerators()) {
                moderators.add(uuid.toString());
            }
            dataConfig.set(path + "moderators", moderators);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean gangExists(String id) {
        return gangsById.containsKey(id.toLowerCase());
    }

    public Gang createGang(String id, String name, UUID leader) {
        id = id.toLowerCase();
        Gang gang = new Gang(id, name, leader);
        gangsById.put(id, gang);
        playerGang.put(leader, id);
        return gang;
    }

    public void disbandGang(Gang gang) {
        gangsById.remove(gang.getId());
        for (UUID uuid : gang.getMembers()) {
            playerGang.remove(uuid);
        }
    }

    public Gang getGangById(String id) {
        return gangsById.get(id.toLowerCase());
    }

    public Gang getGangByPlayer(UUID uuid) {
        String gangId = playerGang.get(uuid);
        if (gangId == null) return null;
        return gangsById.get(gangId.toLowerCase());
    }

    public void addMember(Gang gang, UUID uuid) {
        gang.getMembers().add(uuid);
        playerGang.put(uuid, gang.getId());
    }

    public void removeMember(Gang gang, UUID uuid) {
        gang.getMembers().remove(uuid);
        gang.getModerators().remove(uuid);
        playerGang.remove(uuid);
        if (uuid.equals(gang.getLeader())) {
            // Lider ayrılırsa: çeteyi dağıtalım
            disbandGang(gang);
        }
    }

    public Collection<Gang> getAllGangs() {
        return gangsById.values();
    }

    // Kill/death işlemleri
    public void handleKill(UUID killer, UUID victim) {
        Gang killerGang = getGangByPlayer(killer);
        Gang victimGang = getGangByPlayer(victim);

        if (killerGang != null) {
            killerGang.addKill();
        }
        if (victimGang != null) {
            victimGang.addDeath();
        }
    }

    public int getOnlineMembers(Gang gang) {
        int count = 0;
        for (UUID uuid : gang.getMembers()) {
            if (Bukkit.getPlayer(uuid) != null && Bukkit.getPlayer(uuid).isOnline()) {
                count++;
            }
        }
        return count;
    }
}
