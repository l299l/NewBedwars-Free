import argparse
import math
import sys
from pathlib import Path

import numpy as np
import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))

from bwsim.agents import MILESTONES
from bwsim.game import Layout, simulate
from bwsim.income import team_rates
from bwsim.preset import load_mode

HERE = Path(__file__).resolve().parent
MODES = yaml.safe_load((HERE / "modes.yaml").read_text(encoding="utf-8"))
TARGETS = yaml.safe_load((HERE / "targets.yaml").read_text(encoding="utf-8"))


def layout_for(mode, policy=None):
    cfg = dict(MODES[mode])
    if policy:
        cfg["policy"] = policy
    return Layout(**cfg)


def pct(vals, q):
    v = float(np.percentile(np.where(np.isinf(vals), 1e12, vals), q))
    return INF if v >= 1e11 else v


INF = math.inf


def run(mode, preset=None, games=200, seed=1, fast=False, policy=None):
    gens, phases, prices, source = load_mode(mode, preset)
    layout = layout_for(mode, policy)
    rng = np.random.default_rng(seed)
    results = [simulate(gens, phases, prices, layout, rng, fast) for _ in range(1 if fast else games)]
    stats = {}
    for m in MILESTONES:
        vals = np.array([r.milestones[m] for r in results], dtype=float)
        stats[m] = (pct(vals, 10), pct(vals, 50), pct(vals, 90))
    stats["iron_per_player_min"] = (team_rates(gens.team, 1)["Iron"] * 60 / layout.team_size,) * 3
    banked = np.array([r.banked_at_beds["Diamond"] for r in results])
    stats["banked_at_beds"] = (float(np.percentile(banked, 10)), float(np.median(banked)), float(np.percentile(banked, 90)))
    return {"mode": mode, "source": source, "stats": stats, "bed_time": results[0].bed_time,
            "end_time": results[0].end_time, "phases": phases, "gens": gens, "layout": layout}


def score(mode, stats):
    misses, penalty = [], 0.0
    for key, (lo, hi) in TARGETS[mode].items():
        med = stats[key][1]
        if math.isinf(med):
            misses.append((key, med, lo, hi))
            penalty += 10.0
        elif med < lo or med > hi:
            misses.append((key, med, lo, hi))
            width = max(1.0, hi - lo)
            penalty += ((lo - med) if med < lo else (med - hi)) / width
    return penalty, misses


def fmt(v):
    if math.isinf(v):
        return "never"
    return f"{v:.0f}" if abs(v) >= 10 else f"{v:.1f}"


def report(result):
    mode, stats = result["mode"], result["stats"]
    penalty, misses = score(mode, stats)
    missed = {m[0] for m in misses}
    lines = [f"## {mode} ({result['source']}, policy {result['layout'].policy})", "",
             f"Beds break at {fmt(result['bed_time'])} s, game ends at {fmt(result['end_time'])} s. Score {penalty:.2f} (0 = all targets met).", "",
             "| Metric | p10 | median | p90 | target | |", "|---|---|---|---|---|---|"]
    for key, (lo, hi) in TARGETS[mode].items():
        p10, med, p90 = stats[key]
        lines.append(f"| {key} | {fmt(p10)} | {fmt(med)} | {fmt(p90)} | {lo}-{hi} | {'MISS' if key in missed else 'ok'} |")
    return "\n".join(lines) + "\n"


def chart(results, out):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    fig, axes = plt.subplots(1, len(results), figsize=(4.2 * len(results), 4.2), sharey=False, squeeze=False)
    for ax, res in zip(axes[0], results):
        mode = res["mode"]
        keys = [k for k in MILESTONES if k in TARGETS[mode]]
        y = np.arange(len(keys))
        for i, k in enumerate(keys):
            lo, hi = TARGETS[mode][k]
            ax.barh(i, (hi - lo) / 60, left=lo / 60, color="#cfe3d4", height=0.6)
            p10, med, p90 = res["stats"][k]
            if not math.isinf(med):
                ax.plot([p10 / 60, min(p90, 1e9) / 60], [i, i], color="#555", lw=1.2)
                ax.plot(med / 60, i, "o", color="#1f5fa8")
            else:
                ax.plot(res["end_time"] / 60, i, "x", color="#b3261e")
        ax.axvline(res["bed_time"] / 60, color="#b3261e", ls="--", lw=1)
        ax.set_yticks(y, keys)
        ax.set_title(f"{mode} ({res['source']})", fontsize=10)
        ax.set_xlabel("minutes (green = target, dot = median, line = p10-p90)")
    fig.tight_layout()
    fig.savefig(out, dpi=110)


def main():
    ap = argparse.ArgumentParser(description="Simulate NewBedwars economy per mode")
    ap.add_argument("--mode", default="all", help="solo, doubles, squads, rush, duels or all")
    ap.add_argument("--preset", default=None, help="preset name in presets/ (default: the mode's own), or baseline")
    ap.add_argument("--policy", default=None, help="override buying policy: balanced, rusher, defender")
    ap.add_argument("--games", type=int, default=200)
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--fast", action="store_true", help="expected-value income, one game")
    ap.add_argument("--out", default=str(HERE / "out"))
    args = ap.parse_args()
    modes = list(MODES) if args.mode == "all" else [args.mode]
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    results = [run(m, args.preset, args.games, args.seed, args.fast, args.policy) for m in modes]
    text = "# Balance simulation report\n\n" + "\n".join(report(r) for r in results)
    tag = args.preset or "presets"
    (out / f"report-{tag}.md").write_text(text, encoding="utf-8")
    chart(results, out / f"milestones-{tag}.png")
    print(text)
    total = sum(score(r["mode"], r["stats"])[0] for r in results)
    print(f"Total score: {total:.2f}")


if __name__ == "__main__":
    main()
