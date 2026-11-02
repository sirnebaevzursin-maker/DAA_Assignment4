# Assignment 4 - requirement audit

Zhursin Sirnebayev | SE-2509

| Requirement | Evidence / status |
| --- | --- |
| Maven or Gradle project; builds in one command | `pom.xml`, official `mvnw` / `mvnw.cmd`; `clean verify` |
| Recursive ED without memory | `RecursiveEditDistance.java`: last-character recursion, no memo/cache |
| Bottom-up (n+1) x (m+1) table | `TableEditDistance.java` |
| Two rows, O(m) extra memory | `TwoRowEditDistance.java`: two arrays of b.length()+1 |
| Shared interface and int distance(String a,String b) | `EditDistance.java`, all three implementations; one experiment loop |
| State, transition and base case comments, 2-3 lines above each version | Three-line comment above each `distance` implementation |
| Empty strings and equal answers | Reference and empty tests; 100 random short pairs, both directions |
| Naive character-by-character search | `NaiveSearch.java` |
| KMP with public int[] lps(String p) | `KmpSearch.java`, public LPS method and tests |
| Increasing indices, all overlaps, empty list on missing match | Reference tests for [10], [0,1,2,3], [0,2,4], plus oracle cross-check |
| No backwards movement in KMP text | Search updates only j on fallback; i only increments |
| Empty pattern throws IllegalArgumentException | Tested with empty and non-empty text on both algorithms |
| Pattern longer than text returns empty list | Both algorithms tested |
| Counters inside algorithms, one or more per family | `Counters.java`; increments occur in loops/recursion and at allocation sites |
| Timing is median of 3-5 runs | Five samples per case, three warm-ups, >=20 ms normalized batches; 86 medians |
| Design Card before first run, at report start | First commit contains `EXPERIMENT_DESIGN.md`; card begins both report forms |
| Question, sizes with reasons, inputs with purpose, metrics, prediction, planned plots | All card fields completed before tests/experiments |
| >=4 sizes per algorithm; largest >=4x smallest | Recursion 6 sizes (2..12); DP 11 (2..1024); search 5 (4096..65536) |
| >=2 ED and >=2 search input types; deliberate worst case | ED random/disjoint; search random/late_mismatch/early_mismatch |
| Own generator, fixed seed and reproducible inputs | xorshift64*, seed 20261103, per-case derived state and SHA-256 |
| Clock not zero everywhere; feasible runtime | Every sample has positive normalized time; recursion capped at n=12 |
| results.csv, one row per timed run, required columns and counters | `results/results.csv`: 430 samples, 86 cases, five rows each |
| >=3 readable PNG plots with title, labeled axes and units | Four figures in `results/plots/`; each has a report caption/takeaway |
| At least one log axis or ratio plot | All four figures have a logarithmic axis |
| Reference ED distances, LPS arrays and search matches | All Section 7 values appear in JUnit 5 tests |
| 100 random texts compare Naive and KMP | 100 dynamic tests also compare against String.indexOf oracle |
| Report 2-4 pages | `REPORT.pdf`: exactly four pages; `REPORT.md` matching content |
| Prediction versus result for each family, explanations grounded in data | Report pages 2 and 3; no input-plan changes |
| Four short answers, 3-5 sentences each | Four answers of four sentences on report page 4 |
| Paragraph on measured time and limitations | Report page 3, “Measured time” |
| README build, tests, experiment and AI acknowledgement | All included in `README.md` |
| Git main, small clear commits and v1.0 tag | Local `.git` included in ZIP; final tag created after checks |
| Submission ZIP follows name/surname/group rule | `DAA_Assignment4_Zhursin_Sirnebayev_SE-2509.zip` |
| GitHub repository link, main and v1.0 published | Student action: publish local repository and provide real link |
| Moodle upload | Student action |

Rubric coverage: Edit Distance 25%, String Search 25%, Experiment Design 20%,
Plots & Conclusions 20%, Tests & Repo 10%. No score is assumed; the instructor grades
the submission. External publication/upload steps remain with the student by agreement.
