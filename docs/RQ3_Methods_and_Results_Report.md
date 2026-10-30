# RQ3 — pdqsort mechanism B

## Research question

How does disabling mechanism B, which excludes the pivot-equal range from further
partitioning, affect pdqsort correctness and runtime compared with full pdqsort
and the JDK `Arrays.sort(int[])` baseline?

## Implementation and reproducibility

`ExperimentRunner rq3` runs the same generated input through `pdqsort-full`,
`pdqsort-noB`, and `Arrays.sort`. Input generation and the correctness oracle are
outside the timed interval. Each algorithm receives a fresh clone of the same
input, and one warmup is performed per algorithm and dataset before timed runs.
The default seed is `20261016`, default size is `4096`, and default run count is
`3`; these values can be overridden on the command line.

The datasets are:

| Dataset | Construction |
|---|---|
| D1–D4 | Shared `DataGenerator`, using the supplied seed |
| D5_K2 | Shared many-duplicates generator with K = 2 |
| KILLER_DUPLICATE | All N keys equal `floor(N / 2)` |

`KILLER_DUPLICATE` is a targeted adversary for mechanism B's equal-key handling,
not a claim that the input is a universal or classical killer sequence for
other sorting algorithms. It is deterministic and has a value that aligns with
the current middle-sample index on an all-equal input. The full
variant excludes the equal range, whereas `noB` feeds that range through
repeated partitions until the bad-partition budget invokes the HeapSort
fallback. Compare the partition and fallback counts as well as elapsed time.
The fallback bounds this implementation's worst-case behavior; do not describe
this input as demonstrating quadratic runtime.

### Existing implementation caveat

The shared `PdqSort.choosePivot` currently returns the selected sample index,
while its caller uses that integer as the pivot key. This task does not modify
that shared algorithm file, as it is outside TV6's ownership. Consequently,
the standard D1–D5 runs characterize the current implementation and may not
isolate mechanism B cleanly when the selected index is not a key in the data.
The equal-key adversary is designed to exercise the equal-range path despite
that issue. Have the owner of `PdqSort.java` review/fix the pivot contract before
making broader claims about mechanism B from D1–D5.

Run from the repository root in PowerShell with JDK 17:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
New-Item -ItemType Directory -Force target\classes, target\test-classes | Out-Null
& "$env:JAVA_HOME\bin\javac.exe" -encoding UTF-8 -d target\classes src\main\java\edu\csd201\sort\*.java
& "$env:JAVA_HOME\bin\javac.exe" -encoding UTF-8 -cp target\classes -d target\test-classes src\test\java\edu\csd201\sort\PdqSortTest.java
& "$env:JAVA_HOME\bin\java.exe" -cp "target\classes;target\test-classes" edu.csd201.sort.PdqSortTest
& "$env:JAVA_HOME\bin\java.exe" -cp target\classes edu.csd201.sort.ExperimentRunner rq3 20261016 4096 3 results\rq3_benchmark.csv
```

The CLI form is:

```text
rq3 [seed] [size] [runs] [csv-file]
```

The output CSV includes the seed, dataset, algorithm and variant, per-run
elapsed time, correctness result, and pdqsort metrics (maximum depth, partition
count, HeapSort fallback count, and partial-insertion count). The JDK baseline
does not expose pdqsort metrics, so those cells are empty. A run is marked
`wrong` if its sorted result differs from the `Arrays.sort` oracle; the command
exits non-zero if any result is wrong.

## Mechanism analysis

Mechanism B is evaluated by comparing full pdqsort with `noB` on identical
inputs. D1–D4 provide ordinary ordering-pattern controls; D5_K2 and
KILLER_DUPLICATE emphasize equal keys. Compare medians across the recorded runs,
and interpret time alongside partition counts and maximum depth. The equal-key
adversary is especially relevant because the current `noB` path does not skip
the entire equal-key band. The JDK baseline is a practical reference point, not
an implementation-equivalent control: it is a separate library algorithm.

No RQ3 timing results are stated in this report. Generate the CSV on the target
machine and retain its seed, JDK version, hardware details, and command when
writing result claims.
