package com.l299l.newbedwars.arena.gamerules;

import com.l299l.newbedwars.config.properties.Properties;

import java.util.HashMap;

public class Gamerules {
    public Boolean RandomTeams;
    public Boolean AllowParties;
    public Boolean AllowSpectators;
    public Boolean AllowTeamChat;
    public Boolean AllowGlobalChat;
    public Boolean AllowPrivateChat;
    public Boolean AllowTeamDamage;
    public Boolean MakeSwordsPermanent;

    public Gamerules() {
        HashMap<String, Boolean> gamerules = Properties.DefaultGamerules;
        RandomTeams = gamerules.get("RandomTeams");
        AllowParties = gamerules.get("AllowParties");
        AllowSpectators = gamerules.get("AllowSpectators");
        AllowTeamChat = gamerules.get("AllowTeamChat");
        AllowGlobalChat = gamerules.get("AllowGlobalChat");
        AllowPrivateChat = gamerules.get("AllowPrivateChat");
        AllowTeamDamage = gamerules.get("AllowTeamDamage");
        MakeSwordsPermanent = gamerules.get("MakeSwordsPermanent");
    }

    public void setGamerule(String name, Boolean value) {
        switch (name) {
            case "RandomTeams" -> RandomTeams = value;
            case "AllowParties" -> AllowParties = value;
            case "AllowSpectators" -> AllowSpectators = value;
            case "AllowTeamChat" -> AllowTeamChat = value;
            case "AllowGlobalChat" -> AllowGlobalChat = value;
            case "AllowPrivateChat" -> AllowPrivateChat = value;
            case "AllowTeamDamage" -> AllowTeamDamage = value;
            case "MakeSwordsPermanent" -> MakeSwordsPermanent = value;
        }
    }
    public String toJson() {
        return "{" +
                "\"RandomTeams\":" + RandomTeams +
                ", \"AllowParties\":" + AllowParties +
                ", \"AllowSpectators\":" + AllowSpectators +
                ", \"AllowTeamChat\":" + AllowTeamChat +
                ", \"AllowGlobalChat\":" + AllowGlobalChat +
                ", \"AllowPrivateChat\":" + AllowPrivateChat +
                ", \"AllowTeamDamage\":" + AllowTeamDamage +
                ", \"MakeSwordsPermanent\":" + MakeSwordsPermanent +
                '}';
    }
}
