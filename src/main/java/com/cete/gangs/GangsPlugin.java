package com.emirhan.gangs;

import com.emirhan.gangs.commands.CeteCommand;
import com.emirhan.gangs.listeners.GangListener;
import com.emirhan.gangs.manager.GangManager;
import com.emirhan.gangs.placeholders.CetePlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class GangsPlugin extends JavaPlugin {

    private static GangsPlugin instance;
    private GangManager gangManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig(); // config.yml oluştur / yükle

        this.gangManager = new GangManager(this);
        this.gangManager.loadGangs();

        // Komut
        CeteCommand ceteCommand = new CeteCommand(this);
        getCommand("cete").setExecutor(ceteCommand);
        getCommand("cete").setTabCompleter(ceteCommand);

        // Listener
        Bukkit.getPluginManager().registerEvents(new GangListener(this), this);

        // PlaceholderAPI entegrasyonu
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CetePlaceholders(this).register();
            getLogger().info("PlaceholderAPI bulundu, %cete% placeholderları kayıt edildi.");
        } else {
            getLogger().warning("PlaceholderAPI bulunamadı! Placeholderlar çalışmayacak.");
        }

        getLogger().info("GangsPlugin (Çete sistemi) aktif!");
    }

    @Override
    public void onDisable() {
        if (gangManager != null) {
            gangManager.saveGangs(); // Sunucu kapanırken verileri kaydet
        }
        getLogger().info("GangsPlugin devre dışı bırakıldı ve çeteler kaydedildi.");
    }
    
    public void reloadPlugin() {
        reloadConfig();
        if (gangManager != null) {
            gangManager.reload(); // Configi ve veritabanı bağlantısını yenile
        }
    }

    public static GangsPlugin getInstance() {
        return instance;
    }

    public GangManager getGangManager() {
        return gangManager;
    }
}