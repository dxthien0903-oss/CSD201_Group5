# RQ3 Methods and Results — pdqsort

## Research question

RQ3 asks how pdqsort compares with the course Quick Sort on the RQ2 input distributions, and how mechanisms A and B affect the sort. `Arrays.sort(int[])` is included as the library reference.

## Method

The results below are taken from [`../results/rq3_pdqsort_metrics.csv`](../results/rq3_pdqsort_metrics.csv). That file contains the measured runs; the earlier `rq3_pdqsort.csv` is retained unchanged and does not contain pdqsort comparison counts.

The benchmark used the assignment's RQ2 matrix:

- Sizes: 1,000,000 and 10,000,000.
- D1 and D3: seeds 20261016, 20261017, and 20261018.
- D2 and D4: seed 20261016; these datasets are deterministic under the specified generator.
- D5: K = 1,000, 10, and 2, each with the three seeds above.
- Algorithms: course QuickSort (`hoare-med3`), pdqsort full, noA, noB, noA+noB, and `Arrays.sort`.
- Two warm-ups and three timed repeats for each algorithm and dataset. Every algorithm receives the same generated input, by cloning it before each run.

Before timing, the runner creates the dataset and an oracle sorted with `Arrays.sort`. It checks every completed output against the oracle before accepting the row. `output_check` is the specified rolling hash of that sorted output. A mismatch aborts the run before a CSV is written.

Each timed sort runs with metric collection disabled. QuickSort and pdqsort metrics are collected in separate passes on clones of the same input, outside the timed interval, and those outputs are also checked against the oracle. The pdqsort comparison count includes each evaluated key comparison in pivot selection, partitioning, insertion sorts, and HeapSort fallback; index and loop-control comparisons are excluded. The counter only records data and does not change sort decisions.

The matrix has 17 dataset/seed combinations per size, 6 algorithms, and 3 measured repeats: 612 result rows. The CSV records JDK, OS, and available processor count for reproducibility.

## Results

The table gives median elapsed time in milliseconds over the 51 timed rows per algorithm and size (17 dataset/seed combinations × 3 repeats). These are medians over the combined RQ2 distributions, not per-distribution medians.

| Algorithm / variant | N = 1,000,000 | N = 10,000,000 |
|---|---:|---:|
| Course QuickSort (`hoare-med3`) | 38.235 | 442.948 |
| pdqsort full | 17.211 | 210.520 |
| pdqsort noA | 24.354 | 269.420 |
| pdqsort noB | 51.456 | 626.545 |
| pdqsort noA+noB | 57.376 | 730.871 |
| `Arrays.sort` | 12.690 | 172.347 |

Median pdqsort key-comparison counts over the same 51 rows:

| pdqsort variant | N = 1,000,000 | N = 10,000,000 |
|---|---:|---:|
| full | 12,669,941 | 145,656,508 |
| noA | 14,844,046 | 148,312,601 |
| noB | 37,245,964 | 624,703,087 |
| noA+noB | 80,497,007 | 964,993,826 |

Relative to full pdqsort's median comparison count, noA is higher by 17.2% and 1.8% at the two sizes; noB is higher by 194.0% and 328.9%; disabling both is higher by 535.3% and 562.5%. These are aggregate medians across the specified dataset matrix. They describe this implementation and workload; they are not a claim that either mechanism has the same effect on every individual distribution.

The full configuration's aggregate median elapsed time is below course QuickSort's at both sizes in this run. `Arrays.sort` has the lowest aggregate median among the six algorithms at both sizes. Runtime rankings vary by distribution, so the CSV should be consulted for per-dataset conclusions.

All 612 rows have `status=PASS` and an oracle checksum; no QuickSort stack overflow was recorded in the timed or counted runs.

## Runtime and JDK limitation

The assignment describes `java.util.Arrays.sort(int[])` of JDK 8 as its reference implementation. The recorded RQ3 run used Java 17.0.12 on Windows 11 (10.0), with 16 available processors. JDK 8 is installed on the machine, but it was not used for this run. The project POM sets `maven.compiler.release` to 17, and the source uses Java 17 language/API features. Therefore these CSV results are a JDK 17 baseline, not a verified JDK 8 baseline.

Whether JDK 17 is acceptable for the required JDK 8 reference has not been confirmed. No compiler target, source code, or runtime configuration was silently changed. The results must be rerun under an agreed JDK 8-compatible project/runtime setup, or the JDK 17 deviation must be explicitly accepted, before claiming the baseline requirement is met.

## Reproducibility

From the project root, after compiling with JDK 17:

```powershell
java -Xmx4g -cp target/classes edu.csd201.sort.Rq3Benchmark --output results/rq3_pdqsort_metrics.csv
```

The runner refuses to overwrite an existing output file. Use a new `--output` path for subsequent runs.
