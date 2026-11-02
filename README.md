# City Search & Spell-Check - DAA Assignment 4

**Zhursin Sirnebayev - SE-2509**

Java 17+ / Maven project implementing Levenshtein Edit Distance (plain recursion, full table,
two rows) and all-occurrence exact search (Naive, KMP). All positions and string lengths use
Java UTF-16 code units; matching is case-sensitive. Empty search patterns throw
`IllegalArgumentException`; a longer pattern returns an empty list. Algorithm objects reset
their counters on every call and should not be shared between concurrent threads.

## Build and test with one command

Install a JDK 17 or later and set `JAVA_HOME` to its installation directory. The official
Apache Maven Wrapper is included, pinned to Maven 3.9.12; the first build needs Internet
access to Maven Central for Maven and JUnit/plugin dependencies.

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

macOS / Linux:

```sh
sh mvnw clean verify
```

If Maven is already installed, `mvn clean verify` works too. Tests alone:
`mvn test` (or `.\mvnw.cmd test`). The expected result is **244 tests, zero failures,
zero errors, zero skipped**. Maven produces `target/daa-assignment4-1.0.jar`.

On the machine used for this submission, the JDK is `C:\Program Files\Java\jdk-25`.
If necessary, set it for the current PowerShell session before running the command:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
```

## IntelliJ IDEA

Open this directory or its `pom.xml` as a Maven project, select a JDK 17+ for the project,
and allow Maven import to finish. Open `src/main/java/edu/citysearch/Main.java` and run
`Main.main()` for the city example and overlapping matches. Run all tests from
`src/test/java`, or run Maven's `verify` lifecycle goal. The project can be imported
without any machine-specific `.idea` files.

## Demo and complete experiment

Run these commands from the project root after building:

```sh
java -jar target/daa-assignment4-1.0.jar demo
java -Xms256m -Xmx1g -jar target/daa-assignment4-1.0.jar experiment
```

The experiment replaces `results/results.csv` and `results/environment.md`. To preserve
the submitted measurements, provide a different directory:

```sh
java -Xms256m -Xmx1g -jar target/daa-assignment4-1.0.jar experiment results/recheck
```

The complete run is bounded to the predeclared sizes; the disjoint recursive n=12 case
may take seconds per invocation on some machines. The original machine took about
1.32 seconds per invocation for that case. Do not enlarge the recursive sizes casually:
the implementation deliberately has no memoization.

The seed is **20261103**. Inputs are regenerated independently for each case by the
custom xorshift64* generator. Every case gets three untimed warm-ups and five measured
batches, each lasting at least 20 ms. `time_ms` is elapsed batch time divided by the
number of invocations; plots and `summary.csv` use the median of five samples.
KMP preprocessing and instrumentation are included. Counters are per invocation,
not summed across the timing batch. Identical commands reproduce inputs, answers and
counters; elapsed times and timing-batch iteration counts naturally vary.

## Validate results, reproduce PNG plots and rebuild the report

Python 3.10+ is needed only for analysis and reporting; the Java application and JUnit
tests have no Python dependency. Install the optional plotting/report libraries:

```sh
python -m pip install -r scripts/requirements.txt
python scripts/analyze_results.py
python scripts/analyze_results.py --compare results/reproducibility.csv --no-plots
python scripts/build_report.py
```

After a fresh experiment in `results/recheck`, compare it with the primary dataset:

```sh
python scripts/analyze_results.py --compare results/recheck/results.csv --no-plots
```

The analysis validates sample count, five runs per case, size ranges, positive timing,
matching input fingerprints, algorithm agreement, and counter invariants. It writes
all 86 median/min/max summaries and four labeled PNG plots. The report script derives
all quoted numbers from the primary CSV and creates matching `REPORT.md` and a fixed
four-page `REPORT.pdf`. The original pre-run card is preserved unchanged.

## Files and measurements

```text
src/main/java/edu/citysearch/
  edit/          EditDistance interface and its three implementations
  search/        StringSearch interface, NaiveSearch, KmpSearch with public lps(String)
  metrics/       In-algorithm counters and immutable snapshots
  experiment/    Custom seeded generator and measurement runner
  Main.java      Demo and experiment entry point
src/test/java/   JUnit 5 reference, edge, random-agreement and counter tests
results/results.csv             All 430 timed samples from the primary execution
results/reproducibility.csv     All 430 timed samples from an independent repeat
results/summary.csv             Medians and ranges for 86 primary cases
results/plots/                  Four PNG charts
results/environment.md          JVM, OS and experimental settings
results/validation.txt          Recorded validation results
EXPERIMENT_DESIGN.md             Original plan, committed before execution
REPORT.md / REPORT.pdf           Report, with card first and all short answers
SUBMISSION_CHECKLIST.md          Every requirement mapped to evidence
scripts/                        Reproducible analysis/report scripts
```

CSV columns: `family,algorithm,input,n,m,run,seed,input_sha256,result,batch_iterations,
batch_elapsed_ms,time_ms,recursive_calls,dp_cells,char_comparisons,lps_comparisons,
total_comparisons,allocated_int_slots,max_recursion_depth`.

`result` is the distance for ED and occurrence count for search; full search lists are
checked before timing and in tests. `dp_cells` includes boundary cells exactly once.
`recursive_calls` includes base-case calls. `char_comparisons` counts evaluated
character-equality checks; `lps_comparisons` records the same checks during LPS creation.
`allocated_int_slots` counts slots at actual allocation sites, excluding object headers,
references and stack storage. Multiplying it by four gives the plotted integer payload.
The machine used Windows 11, Oracle HotSpot 25.0.1 and eight logical processors, with
`-Xms256m -Xmx1g`. CPU model was unavailable in the sandbox and has not been guessed.

## Git and publication

The local repository uses **main**, small descriptive commits and the final tag **v1.0**.
The submission ZIP includes `.git` to preserve the history and tag. GitHub publication
is intentionally left to the student as requested; no repository URL is invented.

After creating an empty GitHub repository, run from this project directory:

```sh
git remote add origin YOUR_GITHUB_REPOSITORY_URL
git push -u origin main
git push origin v1.0
```

Replace `YOUR_GITHUB_REPOSITORY_URL` with the real URL. Include that link in the Moodle
submission, and if you edit tracked submission files, commit those edits and update
the final release tag deliberately. The required archive filename is
`DAA_Assignment4_Zhursin_Sirnebayev_SE-2509.zip`. Uploading to Moodle is the student's step.

## AI-use acknowledgement

OpenAI Codex assisted with the algorithm implementations, JUnit tests, experiment
design and harness, analysis, plots, documentation and report. The CSV values came
from actual local executions, and reproducibility was checked with a second complete
run. The student should review the source and be able to explain the algorithms,
measurements and conclusions before submitting or defending the work.
