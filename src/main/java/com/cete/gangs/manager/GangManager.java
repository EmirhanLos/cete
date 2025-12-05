package com.emirhan.gangs.manager;

import com.emirhan.gangs.GangsPlugin;
import com.emirhan.gangs.gang.Gang;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class GangManager {

    private final GangsPlugin plugin;
    private final Map<String, Gang> gangsById;
    private final Map<UUID, String> playerGang;

    private File dataFile;
    private YamlConfiguration dataConfig;
    
    private MySQLManager mySQLManager;

    public GangManager(GangsPlugin plugin) {
        this.plugin = plugin;
        this.gangsById = new HashMap<>();
        this.playerGang = new HashMap<>();
        
        setupStorage();
    }
    
    private void setupStorage() {
        if (isMysqlEnabled()) {
            this.mySQLManager = new MySQLManager(plugin);
            this.mySQLManager.connect();
        } else {
            setupFile();
        }
    }
    
    private boolean isMysqlEnabled() {
        return plugin.getConfig().getString("storage.type", "yaml").equalsIgnoreCase("mysql");
    }
    
    public void reload() {
        // Reload öncesi mevcut verileri kaydet
        saveGangs();
        
        if (mySQLManager != null) {
            mySQLManager.disconnect();
        }
        
        setupStorage();
        loadGangs();
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

        if (isMysqlEnabled()) {
            loadGangsFromMySQL();
        } else {
            loadGangsFromYAML();
        }
        
        plugin.getLogger().info("Toplam " + gangsById.size() + " çete yüklendi.");
    }

    // --- YAML Load ---
    private void loadGangsFromYAML() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
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
            gang.setKills(section.getInt("kills", 0));
            gang.setDeaths(section.getInt("deaths", 0));

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
    }

    // --- MySQL Load ---
    private void loadGangsFromMySQL() {
        try {
            Connection conn = mySQLManager.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM cete_gangs");
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                UUID leader = UUID.fromString(rs.getString("leader"));
                
                Gang gang = new Gang(id, name, leader);
                gang.setFriendlyFire(rs.getBoolean("friendlyFire"));
                gang.deposit(rs.getDouble("bank"));
                gang.setKills(rs.getInt("kills"));
                gang.setDeaths(rs.getInt("deaths"));
                
                gangsById.put(id.toLowerCase(), gang);
            }
            rs.close();
            ps.close();

            PreparedStatement psMem = conn.prepareStatement("SELECT * FROM cete_members");
            ResultSet rsMem = psMem.executeQuery();
            
            while (rsMem.next()) {
                String gangId = rsMem.getString("gang_id").toLowerCase();
                UUID uuid = UUID.fromString(rsMem.getString("uuid"));
                String rank = rsMem.getString("rank");

                Gang gang = gangsById.get(gangId);
                if (gang != null) {
                    gang.getMembers().add(uuid);
                    playerGang.put(uuid, gangId);
                    if ("MODERATOR".equalsIgnoreCase(rank)) {
                        gang.getModerators().add(uuid);
                    }
                }
            }
            rsMem.close();
            psMem.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveGangs() {
        if (isMysqlEnabled()) {
            saveGangsToMySQL();
        } else {
            saveGangsToYAML();
        }
    }

    // --- YAML Save ---
    private void saveGangsToYAML() {
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

    // --- MySQL Save ---
    private void saveGangsToMySQL() {
        try {
            Connection conn = mySQLManager.getConnection();
            conn.setAutoCommit(false);
            
            String gangQuery = "INSERT INTO cete_gangs (id, name, leader, friendlyFire, bank, kills, deaths) VALUES (?,?,?,?,?,?,?) " +
                               "ON DUPLICATE KEY UPDATE name=?, leader=?, friendlyFire=?, bank=?, kills=?, deaths=?";
            
            PreparedStatement psGang = conn.prepareStatement(gangQuery);
            PreparedStatement psDelMem = conn.prepareStatement("DELETE FROM cete_members WHERE gang_id=?");
            PreparedStatement psInsMem = conn.prepareStatement("INSERT INTO cete_members (uuid, gang_id, rank) VALUES (?,?,?)");

            for (Gang gang : gangsById.values()) {
                // Gang Save
                psGang.setString(1, gang.getId());
                psGang.setString(2, gang.getName());
                psGang.setString(3, gang.getLeader().toString());
                psGang.setBoolean(4, gang.isFriendlyFire());
                psGang.setDouble(5, gang.getBank());
                psGang.setInt(6, gang.getKills());
                psGang.setInt(7, gang.getDeaths());
                // Update on duplicate
                psGang.setString(8, gang.getName());
                psGang.setString(9, gang.getLeader().toString());
                psGang.setBoolean(10, gang.isFriendlyFire());
                psGang.setDouble(11, gang.getBank());
                psGang.setInt(12, gang.getKills());
                psGang.setInt(13, gang.getDeaths());
                psGang.addBatch();

                // Members Save
                psDelMem.setString(1, gang.getId());
                psDelMem.executeUpdate(); 

                for (UUID uuid : gang.getMembers()) {
                    psInsMem.setString(1, uuid.toString());
                    psInsMem.setString(2, gang.getId());
                    String rank = "MEMBER";
                    if (gang.getLeader().equals(uuid)) rank = "LEADER";
                    else if (gang.getModerators().contains(uuid)) rank = "MODERATOR";
                    
                    psInsMem.setString(3, rank);
                    psInsMem.addBatch();
                }
            }
            
            psGang.executeBatch();
            psInsMem.executeBatch();
            
            conn.commit();
            conn.setAutoCommit(true);
            
        } catch (SQLException e) {
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
        
        if (isMysqlEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Connection conn = mySQLManager.getConnection();
                    PreparedStatement ps1 = conn.prepareStatement("DELETE FROM cete_gangs WHERE id=?");
                    ps1.setString(1, gang.getId());
                    ps1.executeUpdate();
                    
                    PreparedStatement ps2 = conn.prepareStatement("DELETE FROM cete_members WHERE gang_id=?");
                    ps2.setString(1, gang.getId());
                    ps2.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
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
            disbandGang(gang);
        }
    }

    public Collection<Gang> getAllGangs() {
        return gangsById.values();
    }

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