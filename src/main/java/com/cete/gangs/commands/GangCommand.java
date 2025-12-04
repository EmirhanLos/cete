package com.emirhan.gangs.commands;

import com.emirhan.gangs.GangsPlugin;
import com.emirhan.gangs.gang.Gang;
import com.emirhan.gangs.manager.GangManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class CeteCommand implements CommandExecutor, TabCompleter {

    private final GangsPlugin plugin;
    private final GangManager gangManager;
    private final Map<UUID, String> pendingInvites = new HashMap<>();

    public CeteCommand(GangsPlugin plugin) {
        this.plugin = plugin;
        this.gangManager = plugin.getGangManager();
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private String prefix() {
        return color(plugin.getConfig().getString("prefix", "&8[&6Çete&8] &7"));
    }

    private String msg(String path, String def) {
        return prefix() + color(plugin.getConfig().getString("messages." + path, def));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cBu komut sadece oyun içinden kullanılabilir."));
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "yardım":
                sendHelp(player);
                break;

            case "kur": { // /cete kur <id> <isim>
                if (args.length < 3) {
                    player.sendMessage(prefix() + color("&cKullanım: /cete kur <id> <isim>"));
                    break;
                }
                if (gangManager.getGangByPlayer(uuid) != null) {
                    player.sendMessage(msg("already-in-gang", "&cZaten bir çetedesin."));
                    break;
                }
                String id = args[1];
                if (gangManager.gangExists(id)) {
                    player.sendMessage(msg("gang-id-used", "&cBu ID zaten kullanılıyor!"));
                    break;
                }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                Gang gang = gangManager.createGang(id, name, uuid);

                String m = plugin.getConfig().getString("messages.gang-created",
                        "&aÇete oluşturuldu: &e%name% &7(ID: %id%)");
                m = m.replace("%name%", gang.getName()).replace("%id%", gang.getId());
                player.sendMessage(prefix() + color(m));
                break;
            }

            case "dağıt": { // /cete dağıt
                Gang gang = gangManager.getGangByPlayer(uuid);
                if (gang == null) {
                    player.sendMessage(msg("not-in-gang", "&cHerhangi bir çetede değilsin."));
                    break;
                }
                if (!gang.getLeader().equals(uuid)) {
                    player.sendMessage(msg("not-leader", "&cBu işlemi sadece çete lideri yapabilir."));
                    break;
                }
                gangManager.disbandGang(gang);
                player.sendMessage(msg("gang-disbanded", "&cÇeteni dağıttın."));
                break;
            }

            case "davet": { // /cete davet <oyuncu>
                if (args.length < 2) {
                    player.sendMessage(prefix() + color("&cKullanım: /cete davet <oyuncu>"));
                    break;
                }
                Gang gang = gangManager.getGangByPlayer(uuid);
                if (gang == null) {
                    player.sendMessage(msg("not-in-gang", "&cÖnce bir çeteye sahip olmalısın."));
                    break;
                }

                boolean allowModInvite = plugin.getConfig().getBoolean("settings.allow-moderator-invite", true);
                if (!gang.getLeader().equals(uuid) && !(allowModInvite && gang.isModerator(uuid))) {
                    player.sendMessage(msg("not-leader-or-mod", "&cBu işlemi sadece lider veya moderatör yapabilir."));
                    break;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(msg("target-not-online", "&cOyuncu çevrim içi değil."));
                    break;
                }
                if (gangManager.getGangByPlayer(target.getUniqueId()) != null) {
                    player.sendMessage(msg("target-already-in-gang", "&cBu oyuncu zaten bir çetede."));
                    break;
                }

                pendingInvites.put(target.getUniqueId(), gang.getId());

                String sent = plugin.getConfig().getString("messages.invite-sent",
                        "&a%target% &7isimli oyuncuya davet gönderdin.");
                sent = sent.replace("%target%", target.getName());
                player.sendMessage(prefix() + color(sent));

                String received = plugin.getConfig().getString("messages.invite-received",
                        "&a%leader% &7seni çetesine davet etti: &e%gang%");
                received = received.replace("%leader%", player.getName())
                        .replace("%gang%", gang.getName());
                target.sendMessage(prefix() + color(received));

                String info = plugin.getConfig().getString("messages.invite-accept-info",
                        "&7Kabul etmek için: &a/cete katıl %id%");
                info = info.replace("%id%", gang.getId());
                target.sendMessage(prefix() + color(info));

                target.playSound(target.getLocation(), Sound.LEVEL_UP, 1f, 1f);
                break;
            }

            case "katıl": { // /cete katıl <id>
                if (args.length < 2) {
                    player.sendMessage(prefix() + color("&cKullanım: /cete katıl <id>"));
                    break;
                }
                if (gangManager.getGangByPlayer(uuid) != null) {
                    player.sendMessage(msg("already-in-gang", "&cZaten bir çetedesin."));
                    break;
                }
                String gangId = args[1].toLowerCase();
                Gang gang = gangManager.getGangById(gangId);
                if (gang == null) {
                    player.sendMessage(msg("gang-not-found", "&cBöyle bir çete yok."));
                    break;
                }
                String invitedGangId = pendingInvites.get(uuid);
                if (invitedGangId == null || !invitedGangId.equalsIgnoreCase(gangId)) {
                    player.sendMessage(msg("no-invite", "&cBu çete için geçerli bir davetin yok."));
                    break;
                }

                gangManager.addMember(gang, uuid);
                pendingInvites.remove(uuid);

                String joined = plugin.getConfig().getString("messages.joined-gang",
                        "&aArtık &e%gang% &açetesindesin.");
                joined = joined.replace("%gang%", gang.getName());
                player.sendMessage(prefix() + color(joined));
                break;
            }

            case "ayrıl": { // /cete ayrıl
                Gang gang = gangManager.getGangByPlayer(uuid);
                if (gang == null) {
                    player.sendMessage(msg("not-in-gang", "&cHerhangi bir çetede değilsin."));
                    break;
                }
                if (gang.getLeader().equals(uuid)) {
                    player.sendMessage(prefix() + color(plugin.getConfig().getString("messages.leave-leader",
                            "&cLidersin, ayrılmak yerine /cete dağıt kullan.")));
                    break;
                }
                gangManager.removeMember(gang, uuid);
                player.sendMessage(msg("left-gang", "&cÇeteden ayrıldın."));
                break;
            }

            case "bilgi": { // /cete bilgi [id]
                Gang gang;
                if (args.length >= 2) {
                    gang = gangManager.getGangById(args[1]);
                    if (gang == null) {
                        player.sendMessage(msg("gang-not-found", "&cBöyle bir çete yok."));
                        break;
                    }
                } else {
                    gang = gangManager.getGangByPlayer(uuid);
                    if (gang == null) {
                        player.sendMessage(msg("info-not-in-gang",
                                "&cÇetede değilsin, ID ver: /cete bilgi <id>"));
                        break;
                    }
                }

                player.sendMessage(color("&8&m------------------------------"));
                player.sendMessage(color("&6Çete: &e" + gang.getName() + " &7(ID: " + gang.getId() + ")"));
                String leaderName = Bukkit.getOfflinePlayer(gang.getLeader()).getName();
                player.sendMessage(color("&6Lider: &e" + leaderName));
                player.sendMessage(color("&6Üye Sayısı: &e" + gang.getMembers().size()));
                player.sendMessage(color("&6Online Üyeler: &e" + gangManager.getOnlineMembers(gang)));
                player.sendMessage(color("&6Kill: &e" + gang.getKills() +
                        " &6Death: &e" + gang.getDeaths()));
                player.sendMessage(color("&6Friendly Fire: &e" + (gang.isFriendlyFire() ? "AÇIK" : "KAPALI")));
                player.sendMessage(color("&8&m------------------------------"));
                break;
            }

            case "liste": { // /cete liste
                Collection<Gang> all = gangManager.getAllGangs();
                if (all.isEmpty()) {
                    player.sendMessage(msg("no-gangs", "&cSunucuda hiç çete yok."));
                    break;
                }

                String header = plugin.getConfig().getString("messages.list-header",
                        "&6Mevcut Çeteler (&e%count%&6):");
                header = header.replace("%count%", String.valueOf(all.size()));
                player.sendMessage(prefix() + color(header));

                for (Gang g : all) {
                    player.sendMessage(color("&e" + g.getId() + " &7- &f" + g.getName() +
                            " &7(" + g.getMembers().size() + " üye, Kill: " + g.getKills() + ")"));
                }
                break;
            }

            case "pvp": { // /cete pvp
                Gang gang = gangManager.getGangByPlayer(uuid);
                if (gang == null) {
                    player.sendMessage(msg("not-in-gang", "&cHerhangi bir çetede değilsin."));
                    break;
                }
                boolean allowModInvite = plugin.getConfig().getBoolean("settings.allow-moderator-invite", true);
                if (!gang.getLeader().equals(uuid) && !(allowModInvite && gang.isModerator(uuid))) {
                    player.sendMessage(msg("not-leader-or-mod",
                            "&cBu işlemi sadece lider veya moderatör yapabilir."));
                    break;
                }

                gang.setFriendlyFire(!gang.isFriendlyFire());
                String state = gang.isFriendlyFire() ? "AÇIK" : "KAPALI";
                String ffMsg = plugin.getConfig().getString("messages.ff-toggled",
                        "&aÇete içi PvP durumu: &e%state%");
                ffMsg = ffMsg.replace("%state%", state);
                player.sendMessage(prefix() + color(ffMsg));
                break;
            }

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(color(plugin.getConfig().getString("messages.help-header",
                "&8&m------------------------------")));
        for (String line : plugin.getConfig().getStringList("messages.help-lines")) {
            player.sendMessage(color(line));
        }
        player.sendMessage(color(plugin.getConfig().getString("messages.help-footer",
                "&8&m------------------------------")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (!(sender instanceof Player)) return list;

        if (args.length == 1) {
            list.addAll(Arrays.asList("yardım", "kur", "dağıt", "davet", "katıl",
                    "ayrıl", "bilgi", "liste", "pvp"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("davet")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(sender)) list.add(p.getName());
                }
            } else if (sub.equals("katıl") || sub.equals("bilgi")) {
                for (Gang g : gangManager.getAllGangs()) {
                    list.add(g.getId());
                }
            }
        }
        return list;
    }
}
