package com.l299l.newbedwars.arena.generators.leveling;

import com.l299l.newbedwars.arena.generators.GeneratorType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class GeneratorSettings {
    private final int maxLevel;
    private final boolean showNextSpawnTime;
    private final List<Double> ironSpawnRate;
    private final List<Integer> ironSpawnAmount;
    private final List<Double> goldSpawnRate;
    private final List<Integer> goldSpawnAmount;
    private final List<Double> emeraldSpawnRate;
    private final List<Integer> emeraldSpawnAmount;
    private final List<Double> diamondSpawnRate;
    private final List<Integer> diamondSpawnAmount;
    private final List<Double> spawnDelay;

    public GeneratorSettings(ConfigurationSection configurationSection) {
        if (configurationSection == null) {
            throw new IllegalArgumentException("configurationSection cannot be null");
        }
        maxLevel = configurationSection.getInt("MaxLevel");
        showNextSpawnTime = configurationSection.getBoolean("ShowNextSpawnTime");
        if (maxLevel < 1) {
            throw new IllegalArgumentException("MaxLevel cannot be less than 1");
        }
        if (configurationSection.contains("Iron")) {
            ironSpawnRate = configurationSection.getDoubleList("Iron.SpawnRate");
            if (ironSpawnRate.size() != maxLevel && ironSpawnRate.size() != 1) {
                throw new IllegalArgumentException("Iron.SpawnRate size must be equal to MaxLevel");
            }else if(ironSpawnRate.size() == 1) {
                double spawnRate = ironSpawnRate.get(0);
                ironSpawnRate.clear();
                for (int i = 0; i < maxLevel; i++) {
                    ironSpawnRate.add(spawnRate);
                }
            }
            ironSpawnAmount = configurationSection.getIntegerList("Iron.MaxAmount");
            if (ironSpawnAmount.size() != maxLevel && ironSpawnAmount.size() != 1) {
                throw new IllegalArgumentException("Iron.MaxAmount size must be equal to MaxLevel");
            }else if(ironSpawnAmount.size() == 1) {
                int spawnAmount = ironSpawnAmount.get(0);
                ironSpawnAmount.clear();
                for (int i = 0; i < maxLevel; i++) {
                    ironSpawnAmount.add(spawnAmount);
                }
            }
        }else {
            ironSpawnRate = null;
            ironSpawnAmount = null;
        }
        if (configurationSection.contains("Gold")) {
            goldSpawnRate = configurationSection.getDoubleList("Gold.SpawnRate");
            if (goldSpawnRate.size() != maxLevel && goldSpawnRate.size() != 1) {
                throw new IllegalArgumentException("Gold.SpawnRate size must be equal to MaxLevel");
            } else if (goldSpawnRate.size() == 1) {
                double spawnRate = goldSpawnRate.get(0);
                goldSpawnRate.clear();
                for (int i = 0; i < maxLevel; i++) {
                    goldSpawnRate.add(spawnRate);
                }
            }
            goldSpawnAmount = configurationSection.getIntegerList("Gold.MaxAmount");
            if (goldSpawnAmount.size() != maxLevel && goldSpawnAmount.size() != 1) {
                throw new IllegalArgumentException("Gold.MaxAmount size must be equal to MaxLevel");
            } else if (goldSpawnAmount.size() == 1) {
                int spawnAmount = goldSpawnAmount.get(0);
                goldSpawnAmount.clear();
                for (int i = 0; i < maxLevel; i++) {
                    goldSpawnAmount.add(spawnAmount);
                }
            }
        }else {
            goldSpawnRate = null;
            goldSpawnAmount = null;
        }
        if (configurationSection.contains("Emerald")) {
            emeraldSpawnRate = configurationSection.getDoubleList("Emerald.SpawnRate");
            if (emeraldSpawnRate.size() != maxLevel && emeraldSpawnRate.size() != 1) {
                throw new IllegalArgumentException("Emerald.SpawnRate size must be equal to MaxLevel");
            } else if (emeraldSpawnRate.size() == 1) {
                double spawnRate = emeraldSpawnRate.get(0);
                emeraldSpawnRate.clear();
                for (int i = 0; i < maxLevel; i++) {
                    emeraldSpawnRate.add(spawnRate);
                }
            }
            emeraldSpawnAmount = configurationSection.getIntegerList("Emerald.MaxAmount");
            if (emeraldSpawnAmount.size() != maxLevel && emeraldSpawnAmount.size() != 1) {
                throw new IllegalArgumentException("Emerald.MaxAmount size must be equal to MaxLevel");
            } else if (emeraldSpawnAmount.size() == 1) {
                int spawnAmount = emeraldSpawnAmount.get(0);
                emeraldSpawnAmount.clear();
                for (int i = 0; i < maxLevel; i++) {
                    emeraldSpawnAmount.add(spawnAmount);
                }
            }
        }else {
            emeraldSpawnRate = null;
            emeraldSpawnAmount = null;
        }
        if (configurationSection.contains("Diamond")) {
            diamondSpawnRate = configurationSection.getDoubleList("Diamond.SpawnRate");
            if (diamondSpawnRate.size() != maxLevel && diamondSpawnRate.size() != 1) {
                throw new IllegalArgumentException("Diamond.SpawnRate size must be equal to MaxLevel");
            } else if (diamondSpawnRate.size() == 1) {
                double spawnRate = diamondSpawnRate.get(0);
                diamondSpawnRate.clear();
                for (int i = 0; i < maxLevel; i++) {
                    diamondSpawnRate.add(spawnRate);
                }
            }
            diamondSpawnAmount = configurationSection.getIntegerList("Diamond.MaxAmount");
            if (diamondSpawnAmount.size() != maxLevel && diamondSpawnAmount.size() != 1) {
                throw new IllegalArgumentException("Diamond.MaxAmount size must be equal to MaxLevel");
            } else if (diamondSpawnAmount.size() == 1) {
                int spawnAmount = diamondSpawnAmount.get(0);
                diamondSpawnAmount.clear();
                for (int i = 0; i < maxLevel; i++) {
                    diamondSpawnAmount.add(spawnAmount);
                }
            }
        }else {
            diamondSpawnRate = null;
            diamondSpawnAmount = null;
        }
        spawnDelay = configurationSection.getDoubleList("SpawnDelay");
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isShowNextSpawnTime() {
        return showNextSpawnTime;
    }

    public List<Double> getIronSpawnRate() {
        return ironSpawnRate;
    }

    public List<Integer> getIronSpawnAmount() {
        return ironSpawnAmount;
    }

    public List<Double> getGoldSpawnRate() {
        return goldSpawnRate;
    }

    public List<Integer> getGoldSpawnAmount() {
        return goldSpawnAmount;
    }

    public List<Double> getEmeraldSpawnRate() {
        return emeraldSpawnRate;
    }

    public List<Integer> getEmeraldSpawnAmount() {
        return emeraldSpawnAmount;
    }

    public List<Double> getDiamondSpawnRate() {
        return diamondSpawnRate;
    }

    public List<Integer> getDiamondSpawnAmount() {
        return diamondSpawnAmount;
    }

    public List<Double> getSpawnDelay() {
        return spawnDelay;
    }
}
