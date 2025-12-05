package com.emirhan.gangs.gang;

import java.util.*;

public class Gang {

    private final String id;
    private String name;
    private UUID leader;
    private final Set<UUID> members;
    private final Set<UUID> moderators;
    private boolean friendlyFire;
    private int kills;
    private int deaths;
    private double bank;

    public Gang(String id, String name, UUID leader) {
        this.id = id.toLowerCase();
        this.name = name;
        this.leader = leader;
        this.members = new HashSet<>();
        this.moderators = new HashSet<>();
        this.friendlyFire = false;
        this.kills = 0;
        this.deaths = 0;
        this.bank = 0.0;
        this.members.add(leader);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getModerators() {
        return moderators;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        this.kills++;
    }

    // Yeni: Veritabanından yüklemek için
    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeath() {
        this.deaths++;
    }

    // Yeni: Veritabanından yüklemek için
    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public double getBank() {
        return bank;
    }

    public void deposit(double amount) {
        this.bank += amount;
    }

    public boolean withdraw(double amount) {
        if (this.bank >= amount) {
            this.bank -= amount;
            return true;
        }
        return false;
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isModerator(UUID uuid) {
        return moderators.contains(uuid);
    }

    public String getRank(UUID uuid) {
        if (uuid.equals(leader)) return "Leader";
        if (moderators.contains(uuid)) return "Moderator";
        if (members.contains(uuid)) return "Member";
        return "None";
    }
}