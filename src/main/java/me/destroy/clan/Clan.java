package me.destroy.clan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin ;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public final class Clan extends JavaPlugin implements CommandExecutor, TabCompleter {

    public HashMap<UUID,String> clanInvites = new HashMap<>();
    public HashMap<UUID,String> allianceInvites = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupConfig();
    }

    @Override
    public void onDisable() {

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player p))return true;

        switch (cmd.getName().toLowerCase()){

            case "clan" -> {
                if(args.length == 0)return true;
                if(args.length == 1) {
                    switch (args[0].toLowerCase()) {
                        case "accept" -> {
                            if (clanInvites.containsKey(p.getUniqueId()) && getClan(p) == null) {
                                if (!joinClan(clanInvites.get(p.getUniqueId()), p)) {
                                    p.sendMessage("Failed to join clan");
                                    return true;
                                }
                            }
                        }
                        case "leave" -> {
                            if(!leaveClan(p)){
                                p.sendMessage(ChatColor.RED + "You are not in a clan");
                                return true;
                            }
                        }
                        case "disband" -> {
                            if(!disbandClan(p)){
                                p.sendMessage(ChatColor.RED + "You dont have permission");
                                return true;
                            }
                        }
                        case "info" -> {
                            if(getClan(p) == null){
                                p.sendMessage(ChatColor.RED + "You are not in a clan");
                                return true;
                            }
                            p.sendMessage(clanInfo(getClan(p)));
                            return true;
                        }
                        case "allyaccept" -> {
                            if(getClan(p) == null){
                                p.sendMessage(ChatColor.RED + "You are not in a clan");
                                return true;
                            }
                            if(!allyClan(allianceInvites.get(p.getUniqueId()),p)){
                                p.sendMessage(ChatColor.RED + "Alliance Failed");
                                return true;
                            }
                            return true;
                        }
                    }
                }
                if(args.length == 2){
                    switch (args[0].toLowerCase()){
                        case "create" -> {
                            if(!createClan(args[1],p)){
                               p.sendMessage(ChatColor.RED + "Clan already exists");
                                return true;
                            }
                        }
                        case "invite" -> {
                            OfflinePlayer x = Bukkit.getOfflinePlayer(args[1]).getPlayer();
                            if(x == null)return true;
                            if(!invitePlayer(p,x)){
                                p.sendMessage(ChatColor.RED + "Player already in a clan");
                                return true;
                            }
                            if(x.isOnline()){
                                if(x.getPlayer() == null)return true;
                                x.getPlayer().sendMessage("You have been invited to " + clanInvites.get(x.getUniqueId()));
                                x.getPlayer().sendMessage("/Clan Accept to join");
                                return true;
                            }
                        }
                        case "promote" -> {
                            if(!promotionClan(Bukkit.getPlayer(args[1]))){
                                p.sendMessage(ChatColor.RED + "Player is not in your clan");
                                return true;
                            }
                        }
                        case "kick" -> {
                            if(!kickFromClan(Bukkit.getPlayer(args[1]))){
                                p.sendMessage(ChatColor.RED + "Player is not in your clan");
                                return true;
                            }
                        }
                        case "pass" -> {
                            if(p.getUniqueId() != getClanCaptain(p)){
                                p.sendMessage(ChatColor.RED + "You are not clan captain");
                                return true;
                            }
                            if(!passClan(p)){
                                p.sendMessage(ChatColor.RED + "You are not in a clan");
                                return true;
                            }
                        }
                        case "ally" -> {
                            if(getClan(p) == null){
                                p.sendMessage(ChatColor.RED + "You are not in a clan");
                                return true;
                            }
                            String newAlly = args[1];
                            String newAllyCaptainID = getConfig().getString("Clans." + newAlly + ".Captain");
                            if(newAllyCaptainID == null)return true;
                            OfflinePlayer newAllyCaptain = Bukkit.getOfflinePlayer(UUID.fromString(newAllyCaptainID));
                            if(newAllyCaptain.getPlayer() != null && newAllyCaptain.isOnline()) {
                                allianceInvites.put(newAllyCaptain.getUniqueId(),getClan(p));
                                newAllyCaptain.getPlayer().sendMessage(inviteAlliance(getClan(p)));
                            }
                            //allyClan(newAlly,p);
                            return true;
                        }
                        case "breakalliance" -> {
                            if(getClan(p) == null){
                                p.sendMessage(ChatColor.RED + "You are not in a clan");
                                return true;
                            }
                            String alliedClan = args[1];
                            if(!allyBreakClan(alliedClan,p)){
                                p.sendMessage(ChatColor.RED + "Command Failed");
                                return true;
                            }
                            return true;
                        }
                    }
                }
            }
            case "clans" -> {
                if(args.length == 0)return true;
                if(args.length == 1){
                    if(clanInfo(args[0]) == null)return true;
                    p.sendMessage(clanInfo(args[0]));
                }
            }
        }
        return true;
    }

    public Component clanInfo(String clan){
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return null;
        if(!Clans.contains(clan))return null;
        String created = Clans.getString("Created");if(created == null)return null;
        List<String> Recruits = Clans.getStringList(clan + ".Recruits");
        List<String> Members = Clans.getStringList(clan + ".Members");
        List<String> Officers = Clans.getStringList(clan + ".Officers");
        List<String> Allies = Clans.getStringList(clan + ".Allies");
        String captain = Bukkit.getOfflinePlayer(UUID.fromString(Clans.getString(clan + ".Captain"))).getName();
        Component component = Component.text("[" + clan + "]",NamedTextColor.GREEN)
                .append(Component.text("Created: "  + created  + "\n",NamedTextColor.WHITE))
                .append(Component.text("Captain: "  + captain  + "\n",NamedTextColor.WHITE))
                .append(Component.text("Officers: " + Officers + "\n",NamedTextColor.WHITE))
                .append(Component.text("Members: "  + Members  + "\n",NamedTextColor.WHITE))
                .append(Component.text("Recruits: " + Recruits + "\n",NamedTextColor.WHITE))
                .append(Component.text("Allies: "   + Allies,NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC,false);

        return component;
    }

    public boolean createClan(String clan, OfflinePlayer p){
        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return false;
        if(Clans.contains(clan))return false;
        if(!getConfig().contains("List")) getConfig().set("List",new ArrayList<>());
        else getConfig().set("List", getConfig().getStringList("List").add(clan));

        Clans.createSection(clan);
        if (Clans.contains(clan) && Clans.getStringList("List").contains(clan)) {
            Clans.set(clan + ".Captain",p.getUniqueId().toString());
            Clans.set(clan + ".Officers",new ArrayList<>());
            Clans.set(clan + ".Members",new ArrayList<>());
            Clans.set(clan + ".Recruits",new ArrayList<>());
            Clans.set(clan + ".Players",new ArrayList<String>().add(p.getUniqueId().toString()));
            Clans.set(clan + ".MOTD","Welcome to " + clan);
            Clans.set(clan + ".Created",getCurrentDate());
            Clans.set(clan + ".Allies",new ArrayList<>());

            saveConfig();
            reloadConfig();
            announceClanCreate(clan);
            return true;
        }
        return false;
    }

    public void setupConfig(){
        if(!getConfig().contains("Clans")){
            getConfig().createSection("Clans");
        }
    }

    public boolean invitePlayer(OfflinePlayer inviter,OfflinePlayer invited){
        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return false;

        UUID inviterID = inviter.getUniqueId();
        UUID invitedID = invited.getUniqueId();
        if(getClan(invited) != null)return false;
        if(getClan(inviter) != null){
            clanInvites.put(invitedID,getClan(inviter));
        }
        return true;
    }

    public String getClan(OfflinePlayer p){
        UUID uuid = p.getUniqueId();

        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return null;
        for(String clan:getConfig().getStringList("List")){
            List<String> players = Clans.getStringList(clan + ".Players");
            if(players.contains(uuid.toString())){
                return clan;
            }
        }
        return null;
    }
    public UUID getClanCaptain(OfflinePlayer p){
        UUID uuid = p.getUniqueId();

        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return null;
        for(String clan:getConfig().getStringList("List")){
            List<String> players = Clans.getStringList(clan + ".Players");
            if(players.contains(uuid.toString())){
                if(Clans.getString(clan + ".Captain") == null)return null;
                String stringID = Clans.getString(clan + ".Captain");
                if(stringID == null)return null;
                return UUID.fromString(stringID);
            }
        }
        return null;
    }

    public boolean joinClan(String clan, OfflinePlayer p){
        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return false;

        if(getClan(p) != null)return false;
        if (Clans.contains(clan) && getConfig().getStringList("List").contains(clan)) {
            Clans.set(clan + ".Recruits",Clans.getStringList("Recruits").add(p.getUniqueId().toString()));
            Clans.set(clan + ".Players",Clans.getStringList("Players").add(p.getUniqueId().toString()));
            saveConfig();
            reloadConfig();
            announceClanJoin(clan,p);
            return true;
        }
        return false;
    }
    public boolean allyClan(String clan2, OfflinePlayer p){
        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return false;

        if(getClan(p) != null)return false;
        String clan = getClan(p);
        if(clan == null)return false;
        if (Clans.contains(clan) && getConfig().getStringList("List").contains(clan)) {
            Clans.set(clan + ".Allies",Clans.getStringList("Allies").add(clan2));
            Clans.set(clan2 + ".Allies",Clans.getStringList("Allies").add(clan));

            saveConfig();
            reloadConfig();
            announceClanAlliance(clan,clan2);
            return true;
        }
        return false;
    }
    public boolean allyBreakClan(String clan2, OfflinePlayer p){
        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return false;

        if(getClan(p) != null)return false;
        String clan = getClan(p);
        if(clan == null)return false;
        if (Clans.contains(clan) && getConfig().getStringList("List").contains(clan)) {
            Clans.set(clan + ".Allies",Clans.getStringList("Allies").remove(clan2));
            Clans.set(clan2 + ".Allies",Clans.getStringList("Allies").remove(clan));

            saveConfig();
            reloadConfig();
            announceClanAllianceBreak(clan,clan2);
            return true;
        }
        return false;
    }
    public boolean promotionClan(OfflinePlayer p){
        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return false;

        String clan = getClan(p);
        if(clan == null)return false;
        if (Clans.contains(clan) && getConfig().getStringList("List").contains(clan)) {
            if(Clans.getStringList("Recruits").contains(p.getUniqueId().toString())){
                Clans.set(clan + ".Recruits",Clans.getStringList("Recruits").remove(p.getUniqueId().toString()));
                Clans.set(clan + ".Members",Clans.getStringList("Members").add(p.getUniqueId().toString()));
                announceClanPromotionMember(clan,p);
            }
            if(Clans.getStringList("Members").contains(p.getUniqueId().toString())){
                Clans.set(clan + ".Members",Clans.getStringList("Members").remove(p.getUniqueId().toString()));
                Clans.set(clan + ".Officers",Clans.getStringList("Officers").add(p.getUniqueId().toString()));
                announceClanPromotionOfficer(clan,p);
            }
            if(Clans.getStringList("Officers").contains(p.getUniqueId().toString())){
                return false;
            }

            saveConfig();
            reloadConfig();
            return true;
        }
        return false;
    }
    public boolean passClan(OfflinePlayer p){
        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return false;

        String clan = getClan(p);
        if(clan == null)return false;
        if (Clans.contains(clan) && getConfig().getStringList("List").contains(clan)) {
            if(Clans.getStringList("Recruits").contains(p.getUniqueId().toString())){
                return false;
            }
            if(Clans.getStringList("Members").contains(p.getUniqueId().toString())){
                return false;
            }
            if(Clans.getStringList("Players").contains(p.getUniqueId().toString())){
                String stringID = Clans.getString(clan + ".Captain");
                if(stringID == null)return false;
                Clans.set(clan + ".Recruits", Clans.getStringList("Recruits").add(stringID));
                Clans.set(clan + ".Captain",p.getUniqueId().toString());
            }

            saveConfig();
            reloadConfig();
            announceClanPromotionCaptain(clan,p);
            return true;
        }
        return false;
    }
    public boolean leaveClan(OfflinePlayer p){
        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return false;

        String clan = getClan(p);
        if(clan == null)return false;
        if (Clans.contains(clan) && getConfig().getStringList("List").contains(clan)) {
            if(Clans.getStringList("Recruits").contains(p.getUniqueId().toString())){
                Clans.set(clan + ".Recruits",Clans.getStringList("Recruits").remove(p.getUniqueId().toString()));
            }
            if(Clans.getStringList("Members").contains(p.getUniqueId().toString())){
                Clans.set(clan + ".Members",Clans.getStringList("Members").remove(p.getUniqueId().toString()));
            }
            if(Clans.getStringList("Officers").contains(p.getUniqueId().toString())){
                Clans.set(clan + ".Officers",Clans.getStringList("Officers").remove(p.getUniqueId().toString()));
            }
            saveConfig();
            reloadConfig();
            announceClanLeave(clan,p);
            return true;
        }
        return false;
    }
    public boolean kickFromClan(OfflinePlayer p){
        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return false;

        String clan = getClan(p);
        if(clan == null)return false;
        if (Clans.contains(clan) && getConfig().getStringList("List").contains(clan)) {
            if(Clans.getStringList("Recruits").contains(p.getUniqueId().toString())){
                Clans.set(clan + ".Recruits",Clans.getStringList("Recruits").remove(p.getUniqueId().toString()));
            }
            if(Clans.getStringList("Members").contains(p.getUniqueId().toString())){
                Clans.set(clan + ".Members",Clans.getStringList("Members").remove(p.getUniqueId().toString()));
            }
            if(Clans.getStringList("Officers").contains(p.getUniqueId().toString())){
                Clans.set(clan + ".Officers",Clans.getStringList("Officers").remove(p.getUniqueId().toString()));
            }
            saveConfig();
            reloadConfig();
            announceClanKick(clan,p);
            return true;
        }
        return false;
    }
    public boolean disbandClan(OfflinePlayer p){
        reloadConfig();
        ConfigurationSection Clans = getConfig().getConfigurationSection("Clans");
        if(Clans == null)return false;

        String clan = getClan(p);
        if(clan == null)return false;
        if(p.getUniqueId() != getClanCaptain(p))return false;
        if (Clans.contains(clan) && getConfig().getStringList("List").contains(clan)) {

            getConfig().getStringList("List").remove(clan);
            Clans.set(clan,null);

            if(Clans.contains(clan))return false;
            if(getConfig().contains(clan))return false;

            saveConfig();
            reloadConfig();
            announceClanDisband(clan,p);
            return true;
        }
        return false;
    }

    public void announceClanCreate(String clan){
        for(Player p: Bukkit.getOnlinePlayers()){
            p.sendMessage(Component.text("[" + clan + "]", NamedTextColor.GREEN)
                    .append(Component.text(" has been created!",NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC,false));
        }
    }
    public void announceClanAlliance(String clan,String clan2){
        for(Player p: Bukkit.getOnlinePlayers()){
            p.sendMessage(Component.text("[" + clan + "]", NamedTextColor.GREEN)
                    .append(Component.text(" has been allied to " ,NamedTextColor.WHITE))
                    .append(Component.text("[" + clan2 + "]", NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC,false));
        }
    }
    public void announceClanAllianceBreak(String clan,String clan2){
        for(Player p: Bukkit.getOnlinePlayers()){
            p.sendMessage(Component.text("[" + clan + "]", NamedTextColor.GREEN)
                    .append(Component.text(" has broken alliance to " ,NamedTextColor.WHITE))
                    .append(Component.text("[" + clan2 + "]", NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC,false));
        }
    }
    public void announceClanJoin(String clan,OfflinePlayer p){
        if(p.getPlayer() == null)return;
        for(Player x: Bukkit.getOnlinePlayers()){
            x.sendMessage(p.getPlayer().displayName()
                    .append(Component.text(" has joined ",NamedTextColor.WHITE))
                            .append(Component.text("[" + clan + "]",NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC,false));
        }
    }
    public void announceClanLeave(String clan,OfflinePlayer p){
        if(p.getPlayer() == null)return;
        for(Player x: Bukkit.getOnlinePlayers()){
            x.sendMessage(p.getPlayer().displayName()
                    .append(Component.text(" has left ",NamedTextColor.WHITE))
                    .append(Component.text("[" + clan + "]",NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC,false));
        }
    }
    public void announceClanKick(String clan,OfflinePlayer p){
        if(p.getPlayer() == null)return;
        for(Player x: Bukkit.getOnlinePlayers()){
            x.sendMessage(p.getPlayer().displayName()
                    .append(Component.text(" has been kicked from ",NamedTextColor.WHITE))
                    .append(Component.text("[" + clan + "]",NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC,false));
        }
    }
    public void announceClanDisband(String clan,OfflinePlayer p){
        if(p.getPlayer() == null)return;
        for(Player x: Bukkit.getOnlinePlayers()){
            x.sendMessage(Component.text("[" + clan + "]",NamedTextColor.GREEN)
                    .append(Component.text(" has been disbanded by ",NamedTextColor.WHITE))
                    .append(p.getPlayer().displayName())
                    .decoration(TextDecoration.ITALIC,false));
        }
    }
    public void announceClanPromotionMember(String clan,OfflinePlayer p){
        if(p.getPlayer() == null)return;
        for(Player x: Bukkit.getOnlinePlayers()){
            x.sendMessage(Component.text("[" + clan + "] ",NamedTextColor.GREEN)
                    .append(p.getPlayer().displayName())
                    .append(Component.text(" has been promoted to ",NamedTextColor.WHITE))
                    .append(Component.text(" Member!",NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC,false));
        }
    }
    public void announceClanPromotionOfficer(String clan,OfflinePlayer p){
        if(p.getPlayer() == null)return;
        for(Player x: Bukkit.getOnlinePlayers()){
            x.sendMessage(Component.text("[" + clan + "] ",NamedTextColor.GREEN)
                    .append(p.getPlayer().displayName())
                    .append(Component.text(" has been promoted to ",NamedTextColor.WHITE))
                    .append(Component.text(" Officer!",NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC,false));
        }
    }
    public void announceClanPromotionCaptain(String clan,OfflinePlayer p){
        if(p.getPlayer() == null)return;
        for(Player x: Bukkit.getOnlinePlayers()){
            x.sendMessage(Component.text("[" + clan + "] ",NamedTextColor.GREEN)
                    .append(p.getPlayer().displayName())
                    .append(Component.text(" has been promoted to ",NamedTextColor.WHITE))
                    .append(Component.text(" Captain!",NamedTextColor.GOLD))
                    .decoration(TextDecoration.ITALIC,false));
        }
    }

    public Component inviteAlliance(String clan){
        Component component = Component.text("[" + clan + "]",NamedTextColor.GREEN)
                .append(Component.text(" would like to ally with you",NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC,false);

        return component;
    }

    public String getCurrentDate(){
        TimeZone timeZone = TimeZone.getTimeZone("US/Central");
        String dateFormat = "MM:dd:yyyy";
        Date todayDate = new Date();

        DateFormat todayDateFormat = new SimpleDateFormat(dateFormat);
        todayDateFormat.setTimeZone(timeZone);
        return todayDateFormat.format(todayDate);
    }

    public List<String> tabListClan1() {
        List<String> clan = new ArrayList<>();
        clan.add("Accept");
        clan.add("Leave");
        clan.add("Disband");
        clan.add("Info");
        clan.add("AllyAccept");
        return clan;
    }
    public List<String> tabListClan2() {
        List<String> clan = new ArrayList<>();
        clan.add("Create");
        clan.add("Invite");
        clan.add("Promote");
        clan.add("Kick");
        clan.add("Pass");
        clan.add("Ally");
        clan.add("BreakAlliance");
        return clan;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        Player p = (Player) sender;
        List<String> onlinePlayers = new ArrayList<>();
        for(Player x:Bukkit.getOnlinePlayers()){
            onlinePlayers.add(x.getName());
        }

        switch (cmd.getName().toLowerCase()){
            case "clans" -> {
                if(args.length == 0)return new ArrayList<>();
                if(args.length == 1){
                    return StringUtil.copyPartialMatches(args[0], tabListClan1(), new ArrayList<>());
                }
                if(args.length == 2){
                    return StringUtil.copyPartialMatches(args[0], tabListClan2(), new ArrayList<>());
                }
            }
        }
        return onlinePlayers;

    }
}
