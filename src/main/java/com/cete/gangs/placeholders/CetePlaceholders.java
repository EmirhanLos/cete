package com.emirhan.gangs.placeholders;

import com.emirhan.gangs.GangsPlugin;
import com.emirhan.gangs.gang.Gang;
import com.emirhan.gangs.manager.GangManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CetePlaceholders extends PlaceholderExpansion {

    private final GangsPlugin plugin;
    private final GangManager gangManager;

    public CetePlaceholders(GangsPlugin plugin) {
        this.plugin = plugin;
        this.gangManager = plugin.getGangManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        // %cete_...%
        return "cete";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Emirhan";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * Placeholder formatı:
     * %cete% -> params = ""  (boş)   -> çete ismi
     * %cete_id% -> params = "id"
     * %cete_rütbe% -> params = "rütbe"
     * %cete_kill% -> params = "kill"
     * %cete_death% -> params = "death"
     * %cete_online% -> params = "online"
     * %cete_üye% -> params = "üye"
     */
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        Gang gang = gangManager.getGangByPlayer(player.getUniqueId());

        // Boşsa -> %cete% -> çete ismi
        if (params.isEmpty()) {
            return gang == null ? "Yok" : gang.getName();
        }

        switch (params.toLowerCase()) {
            case "id":
                return gang == null ? "Yok" : gang.getId();
            case "rütbe":
            case "rutbe":
                return gang == null ? "Yok" : gang.getRank(player.getUniqueId());
            case "kill":
                return gang == null ? "0" : String.valueOf(gang.getKills());
            case "death":
                return gang == null ? "0" : String.valueOf(gang.getDeaths());
            case "online":
                return gang == null ? "0" : String.valueOf(gangManager.getOnlineMembers(gang));
            case "üye":
            case "uye":
                return gang == null ? "0" : String.valueOf(gang.getMembers().size());
            default:
                return "";
        }
    }
}
