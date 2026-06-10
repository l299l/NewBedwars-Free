package com.l299l.newbedwars.scoreboard;

import com.l299l.newbedwars.bossbar.TextEffect;

import java.util.List;

public record ScoreboardSave(boolean enabled, String title, TextEffect textEffect, List<String> lines) {
}
