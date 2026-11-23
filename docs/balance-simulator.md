# Balance simulator

The repository contains a small tool in `tools/balance/` that was used to choose the generator speeds, game phases and upgrade prices of the built-in [game modes](game-modes.md) (Solo, Doubles, Squads, Rush, Duels). You don't need it to run NewBedwars. It is for server owners and contributors who want to tune their own values or check a change before trying it with players.

!!! warning "AI-generated"
    The simulator scripts, the tuning run and the first version of the mode values were made by an AI assistant (Claude, from Anthropic), working on the NewBedwars source code with the maintainer. The tool reads the plugin's real files and copies the plugin's generator and phase logic. Its player-behaviour assumptions and target times, however, are educated guesses, not measurements. Treat the results as a starting point that still has to be checked in real games.

## Why it exists

Generators, phases and prices interact. A faster team generator makes every shop item cheaper in practice. Pooled income in a 4-player team buys upgrades much sooner than a solo player can. A short mode like Duels never reaches the late game if the normal values are used. Before 2.8 the values were set by hand and used for every team size. The simulator made those effects visible. In the old values, for example, a squad maxed Protection at about minute 14 and still had more than 60 unspent diamonds when the beds broke.

## What it does

It is an **economy model, not a game simulation**: there is no combat, movement or map. For one team it answers *"with these files, at what minute can the team afford iron armor, Sharpness, diamond armor…, and how many diamonds are left over?"*.

Each simulated game runs second by second:

1. **Phases** follow your `GamePhases` list. As in the plugin, a phase's actions run when it starts, so island generators level up at the right time and beds break when they should.
2. **Generators** behave like the plugin's. The team generator drops one weighted resource every `SpawnDelay`, with an amount from 1 to `MaxAmount`, and Forge raises its level. Island generators drop on whole seconds. The team gets a share of the island drops, and that share changes from game to game: sometimes the team holds the middle, sometimes it doesn't.
3. **Players** collect their base and island drops from time to time and die now and then, which loses part of what they carry. They spend everything they collect following a shopping list (balanced, rusher or defender): swords and wool first, then Forge, armor, Sharpness, Protection and so on.
4. The tool records **milestones**: when every player has a stone sword, iron armor or diamond armor; when Forge, Sharpness and Protection are bought; and when Protection is maxed.

`sim.py` runs 200 games per mode and compares the median milestone times with target ranges. `tune.py` starts from the current values and adjusts a few multipliers per mode until the milestones land inside the ranges:

- team generator speed
- island generator speed
- upgrade prices
- Forge prices
- armor prices
- phase length

It prefers price changes over changing island generators, so maps still feel the same. `export.py` writes the results as the plugin's resource files.

## Using it

You need Python 3 with `numpy`, `matplotlib` and `PyYAML`.

```bash
cd tools/balance
python3 -I sim.py --preset baseline   # the old (pre-2.8) values
python3 -I sim.py                     # the built-in modes
python3 -I sim.py --mode squads --policy defender --games 500
python3 -I tune.py                    # search new values (about 2 minutes)
```

Reports and charts are written to `tools/balance/out/`. The inputs you are most likely to edit:

| File | What to change |
|---|---|
| `modes.yaml` | Your players and maps: teams, players per team, island generators, how often players collect and die |
| `targets.yaml` | When you want things to happen in each mode (time ranges in seconds) |
| `presets/<mode>.yml` | The values themselves: generators, phases, item and upgrade prices |

## Limits

- **Assumptions drive the results.** If your players fight over the middle more or less than `modes.yaml` assumes, the right prices change too. Adjust the file and run it again.
- **The targets are design choices.** The only public reference numbers found were older community guides for similar servers.
- **No combat value.** A Sharpness level and a stack of wool count the same if they cost the same. Whether diamond armor at minute 10 is "too strong" is still your decision.
- **Only the economy is modelled.** Traps, dragons, respawn times and bed defence are not simulated.

Use it to compare configurations and to catch outliers, then confirm in real games.
