# Experiment Design Card

Author: Zhursin Sirnebayev, SE-2509. Frozen before any algorithm tests or timing runs.

| Field | Plan written before the first run |
| --- | --- |
| Question: edit distance | How does repeated recursive work grow compared with DP, and how much integer storage do two rows save? |
| Question: string search | When the pattern grows with the text, does a late mismatch cause quadratic Naive work while KMP stays linear? |
| Sizes | Recursive ED: n = 2, 4, 6, 8, 10, 12; both strings have length n. Both DP versions use those same sizes and 64, 128, 256, 512, 1024. Search: text n = 4096, 8192, 16384, 32768, 65536 and pattern m = n / 64 (64..1024). Every algorithm has at least four sizes and a largest/smallest ratio of at least four. The recursion cap prevents runaway work; larger DP/search sizes give measurable times. |
| ED inputs and purpose | random: independent lowercase strings over a fixed 4-letter alphabet, exposing typical mismatches; disjoint: a^n versus b^n, so every non-base recursive state branches three ways. The disjoint case deliberately stresses plain recursion. |
| Search inputs and purpose | random: independent lowercase text/pattern over a fixed 4-letter alphabet, exposing early mismatches; late_mismatch: text a^n and pattern a^(m-1)b, making Naive compare m characters at every legal alignment; early_mismatch: text a^n and pattern b^m, a best-case contrast with one comparison per alignment. |
| Generator | Custom deterministic Java xorshift64* generator, seed 20261103. Each (family,input,n,m) gets its own derived seed, so algorithm order never changes input. SHA-256 fingerprints in CSV verify identical inputs. |
| Metrics | Five timed samples after three untimed warm-ups for every case. Each sample batches calls until at least 20 ms (minimum one call); record elapsed/batch as time_ms, then use the median of five. ED counts actual recursive calls / evaluated DP cells and character comparisons. Search counts text-pattern comparisons and LPS comparisons separately. Count allocated integer slots at allocation sites, measuring DP payload rather than claiming to measure total heap. |
| Prediction | Disjoint recursion grows exponentially; bottom-up state work is quadratic in n. Table storage grows quadratically, two-row storage linearly, but no universal timing winner is assumed. With m=n/64, late-mismatch Naive comparisons grow quadratically, KMP including preprocessing linearly; random and early-mismatch Naive should look roughly linear. Timing may be noisy or reordered by JIT, allocation and caching. |
| Plots | (1) Small ED work vs n, logarithmic y, to expose repeated work. (2) Large ED integer payload vs n, logarithmic y, to show the memory tradeoff. (3) Search comparisons including LPS vs n, log-log, for all three input types. (4) Median time with min-max ranges: large DP and late-mismatch search, log-log, to relate operation counts to elapsed time. Each figure has units, legend and a one-sentence takeaway in the report. |
| Reproduction / budget | Run the packaged Java experiment from the project root. Counters, outputs and fingerprints must be identical across runs; wall-clock times cannot be identical on a real computer. Results have one row per timed sample, not just medians. No input-plan changes are intended. n=12 is the largest planned recursive size, not a claim that larger strings are impossible. |

No measurements existed when this card was saved. The first Git commit records this plan; later commits add algorithms, tests, and only then measurements.
