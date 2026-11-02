"""Validate the experiment and draw figures using measurements, never fabricated data."""
import argparse
import csv
import math
from collections import defaultdict
from pathlib import Path
from statistics import median


KEY = ("family", "algorithm", "input", "n", "m")
COUNTERS = ("recursive_calls", "dp_cells", "char_comparisons", "lps_comparisons",
            "total_comparisons", "allocated_int_slots", "max_recursion_depth")
DETERMINISTIC = KEY + ("run", "seed", "input_sha256", "result") + COUNTERS


def load(path):
    with Path(path).open(newline="", encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream))
    assert len(rows) == 430, f"Expected 430 samples, found {len(rows)}"
    grouped = defaultdict(list)
    for row in rows:
        grouped[tuple(row[k] for k in KEY)].append(row)
        assert float(row["time_ms"]) > 0
        assert float(row["batch_elapsed_ms"]) >= 20
        assert int(row["batch_iterations"]) >= 1
        assert row["seed"] == "20261103"
        assert len(row["input_sha256"]) == 64
        assert math.isclose(float(row["time_ms"]), float(row["batch_elapsed_ms"]) /
                            int(row["batch_iterations"]), rel_tol=1e-5, abs_tol=1e-9)
        assert int(row["total_comparisons"]) == int(row["char_comparisons"]) + int(row["lps_comparisons"])
    assert len(grouped) == 86
    for key, samples in grouped.items():
        assert sorted(int(s["run"]) for s in samples) == [1, 2, 3, 4, 5], key
        for column in COUNTERS + ("input_sha256", "result"):
            assert len({s[column] for s in samples}) == 1, (key, column)
    for algorithm in ("recursive", "table", "two_rows", "naive", "kmp"):
        sizes = {int(r["n"]) for r in rows if r["algorithm"] == algorithm}
        assert len(sizes) >= 4 and max(sizes) >= 4 * min(sizes)
    case_inputs = defaultdict(set)
    case_results = defaultdict(set)
    for row in rows:
        case = (row["family"], row["input"], row["n"], row["m"])
        case_inputs[case].add(row["input_sha256"])
        case_results[case].add(row["result"])
        n, m = int(row["n"]), int(row["m"])
        # These formulas validate counters; all plotted counts came from inside the algorithms.
        if row["algorithm"] in ("table", "two_rows"):
            assert int(row["dp_cells"]) == (n + 1) * (m + 1)
            assert int(row["char_comparisons"]) == n * m
            slots = (n + 1) * (m + 1) if row["algorithm"] == "table" else 2 * (m + 1)
            assert int(row["allocated_int_slots"]) == slots
        if row["algorithm"] == "naive" and row["input"] == "late_mismatch":
            assert int(row["char_comparisons"]) == (n - m + 1) * m
        if row["algorithm"] == "kmp":
            assert int(row["char_comparisons"]) <= 2 * n
            assert int(row["lps_comparisons"]) <= 2 * m
    assert all(len(values) == 1 for values in case_inputs.values())
    assert all(len(values) == 1 for values in case_results.values())
    return rows, grouped


def summarize(grouped, destination):
    with destination.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(KEY) +
                                ["median_time_ms", "min_time_ms", "max_time_ms", "samples"] + list(COUNTERS))
        writer.writeheader()
        for key, rows in sorted(grouped.items()):
            times = [float(r["time_ms"]) for r in rows]
            row = dict(zip(KEY, key))
            row.update(median_time_ms=f"{median(times):.9f}", min_time_ms=f"{min(times):.9f}",
                       max_time_ms=f"{max(times):.9f}", samples=len(rows))
            row.update({k: rows[0][k] for k in COUNTERS})
            writer.writerow(row)


def figures(grouped, directory):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from matplotlib.ticker import NullFormatter
    plt.rcParams.update({"font.size": 11, "axes.spines.top": False, "axes.spines.right": False,
                         "axes.titleweight": "bold", "figure.facecolor": "white",
                         "axes.grid": True, "grid.alpha": .22, "savefig.dpi": 200})
    directory.mkdir(parents=True, exist_ok=True)
    colors = {"recursive": "#d97706", "table": "#2563eb", "two_rows": "#0f766e",
              "naive": "#c2410c", "kmp": "#2563eb"}

    def cases(algorithm, input_type, minimum=0, maximum=10**9):
        return sorted((int(k[3]), rows) for k, rows in grouped.items()
                      if k[1] == algorithm and k[2] == input_type and minimum <= int(k[3]) <= maximum)

    def save(fig, filename):
        fig.savefig(directory / filename, bbox_inches="tight")
        plt.close(fig)

    fig, ax = plt.subplots(figsize=(8.5, 3.3), layout="constrained")
    for input_type, style in (("disjoint", "-"), ("random", "--")):
        points = cases("recursive", input_type)
        ax.plot([n for n, _ in points], [int(r[0]["recursive_calls"]) for _, r in points],
                style + "o", color=colors["recursive"], label=f"Recursive / {input_type}")
    points = cases("table", "disjoint", maximum=12)
    ax.plot([n for n, _ in points], [int(r[0]["dp_cells"]) for _, r in points], "s-",
            color=colors["table"], label="Table = Two rows / both input types")
    ax.set(title="Edit distance: repeated recursive states", xlabel="Length of each string n (characters)",
           ylabel="State evaluations per call (count)", yscale="log", xticks=[2, 4, 6, 8, 10, 12])
    ax.legend(fontsize=9, loc="upper left")
    save(fig, "01_edit_work.png")

    fig, ax = plt.subplots(figsize=(8.5, 3.15), layout="constrained")
    for algorithm, label, marker in (("table", "Full table", "s"), ("two_rows", "Two rows", "o")):
        points = cases(algorithm, "disjoint", minimum=64)
        ax.plot([n for n, _ in points], [4 * int(r[0]["allocated_int_slots"]) for _, r in points],
                marker + "-", color=colors[algorithm], label=label)
    ax.set(title="Edit distance: integer payload allocated per call", xlabel="Length of each string n (characters)",
           ylabel="Integer payload (bytes, logarithmic)", yscale="log", xticks=[64, 128, 256, 512, 1024])
    ax.legend(loc="upper left")
    save(fig, "02_edit_memory.png")

    fig, ax = plt.subplots(figsize=(8.5, 3.35), layout="constrained")
    for input_type, style, marker in (("late_mismatch", "-", "o"), ("random", "--", "s"), ("early_mismatch", ":", "^")):
        for algorithm in ("naive", "kmp"):
            points = cases(algorithm, input_type)
            ax.plot([n for n, _ in points], [int(r[0]["total_comparisons"]) for _, r in points],
                    linestyle=style, marker=marker, color=colors[algorithm],
                    label=f"{algorithm.upper()} / {input_type.replace('_', ' ')}")
    ax.set(title="Search work: m = n / 64; KMP includes LPS", xlabel="Text length n (characters, logarithmic)",
           ylabel="Character comparisons per call (count)", xscale="log", yscale="log")
    ax.legend(fontsize=8, ncols=2, loc="upper left")
    save(fig, "03_search_work.png")

    fig, axes = plt.subplots(1, 2, figsize=(8.5, 3.25), layout="constrained")
    for ax, algorithms, input_type, minimum, title in (
        (axes[0], ("table", "two_rows"), "random", 64, "ED / random"),
        (axes[1], ("naive", "kmp"), "late_mismatch", 0, "Search / late mismatch")):
        for algorithm in algorithms:
            points = cases(algorithm, input_type, minimum=minimum)
            xs, mids, lows, highs = [], [], [], []
            for n, rows in points:
                times = [float(r["time_ms"]) for r in rows]
                xs.append(n); mids.append(median(times))
                lows.append(median(times) - min(times)); highs.append(max(times) - median(times))
            ax.errorbar(xs, mids, yerr=[lows, highs], marker="o", capsize=3,
                        color=colors[algorithm], label=algorithm.replace("_", " "))
        ax.set(title=title, xlabel="n (characters, logarithmic)",
               ylabel="Time per call (ms, logarithmic)", xscale="log", yscale="log")
        ax.legend(fontsize=9)
        tick_values = [64, 256, 1024] if input_type == "random" else [4096, 16384, 65536]
        ax.set_xticks(tick_values, [f"{x:,}" for x in tick_values])
        ax.xaxis.set_minor_formatter(NullFormatter())
    fig.suptitle("Elapsed time: median of 5 samples; bars show min-max", fontsize=12, weight="bold")
    save(fig, "04_median_time.png")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--results", type=Path, default=Path("results/results.csv"))
    parser.add_argument("--compare", type=Path)
    parser.add_argument("--no-plots", action="store_true")
    args = parser.parse_args()
    rows, groups = load(args.results)
    summarize(groups, args.results.parent / "summary.csv")
    if args.compare:
        other, _ = load(args.compare)
        signature = lambda source: sorted(tuple(row[k] for k in DETERMINISTIC) for row in source)
        assert signature(rows) == signature(other), "Deterministic measurements differ"
        print("PASS: counters, outputs and fingerprints match in all 430 repeated samples.")
    if not args.no_plots:
        figures(groups, args.results.parent / "plots")
    print("PASS: 430 samples / 86 cases, five runs each, positive times, valid sizes and counters.")


if __name__ == "__main__":
    main()
