import sys
import unittest
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))

from bwsim.income import TeamGenerator, team_rates
from bwsim.load import RESOURCES, load_generators, load_phases
from bwsim.game import _phase_schedule


class IncomeTest(unittest.TestCase):
    def test_monte_carlo_matches_analytic(self):
        gens = load_generators(RESOURCES / "defaultGenerators.yml")
        rng = np.random.default_rng(7)
        for level in (1, 4, 9):
            gen = TeamGenerator(gens.team, rng)
            gen.level = level
            seconds = 120000
            got = dict.fromkeys(("Iron", "Gold"), 0)
            for _ in range(seconds):
                out = gen.step(1.0)
                for r in got:
                    got[r] += out[r]
            want = team_rates(gens.team, level)
            for r in got:
                self.assertAlmostEqual(got[r] / seconds / want[r], 1.0, delta=0.02, msg=f"{r} level {level}")

    def test_actions_run_at_phase_start(self):
        events, bed, end = _phase_schedule(load_phases("Default"))
        self.assertEqual(events[1][0], 300)
        self.assertEqual(bed, 1500)
        self.assertEqual(end, 2100)


if __name__ == "__main__":
    unittest.main()
