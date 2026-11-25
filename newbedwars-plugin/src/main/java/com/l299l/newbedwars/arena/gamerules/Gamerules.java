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

    public Boolean MakeToolsPermanent;
    public Boolean AllowMultipleGolems;

    public Boolean SplitPartiesToStart;

    public Boolean MapVote;

    public Boolean TeamSelection;
    public Boolean TeamSelectionBalanced;
    public Boolean TeamSelectionPartyLeader;
    public Boolean AllowSpectatingOtherTeams;
    public Boolean TeamChatByDefault;

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
        MakeToolsPermanent = gamerules.getOrDefault("MakeToolsPermanent", true);
        AllowMultipleGolems = gamerules.get("AllowMultipleGolems");
        SplitPartiesToStart = gamerules.get("SplitPartiesToStart");
        MapVote = gamerules.getOrDefault("MapVote", true);
        TeamSelection = orDefault(gamerules.get("TeamSelection"), false);
        TeamSelectionBalanced = orDefault(gamerules.get("TeamSelectionBalanced"), true);
        TeamSelectionPartyLeader = orDefault(gamerules.get("TeamSelectionPartyLeader"), true);
        AllowSpectatingOtherTeams = orDefault(gamerules.get("AllowSpectatingOtherTeams"), true);
        TeamChatByDefault = orDefault(gamerules.get("TeamChatByDefault"), false);
    }

    private static Boolean orDefault(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
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
            case "MakeToolsPermanent" -> MakeToolsPermanent = value;
            case "AllowMultipleGolems" -> AllowMultipleGolems = value;
            case "SplitPartiesToStart" -> SplitPartiesToStart = value;
            case "MapVote" -> MapVote = value;
            case "TeamSelection" -> TeamSelection = value;
            case "TeamSelectionBalanced" -> TeamSelectionBalanced = value;
            case "TeamSelectionPartyLeader" -> TeamSelectionPartyLeader = value;
            case "AllowSpectatingOtherTeams" -> AllowSpectatingOtherTeams = value;
            case "TeamChatByDefault" -> TeamChatByDefault = value;
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
                ", \"MakeToolsPermanent\":" + MakeToolsPermanent +
                ", \"AllowMultipleGolems\":" + AllowMultipleGolems +
                ", \"SplitPartiesToStart\":" + SplitPartiesToStart +
                ", \"MapVote\":" + MapVote +
                ", \"TeamSelection\":" + TeamSelection +
                ", \"TeamSelectionBalanced\":" + TeamSelectionBalanced +
                ", \"TeamSelectionPartyLeader\":" + TeamSelectionPartyLeader +
                ", \"AllowSpectatingOtherTeams\":" + AllowSpectatingOtherTeams +
                ", \"TeamChatByDefault\":" + TeamChatByDefault +
                '}';
    }
}
