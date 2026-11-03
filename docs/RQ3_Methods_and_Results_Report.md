# RQ3 Methods and Results — pdqsort

## Research question

RQ3 asks how pdqsort compares with the course Quick Sort on the RQ2 input distributions, and how mechanisms A and B affect the sort. `Arrays.sort(int[])` is included as the library reference.

## Method

The latest three-variant run is recorded in [`../results/rq3_pdqsort_metrics_3variants_20261103.csv`](../results/rq3_pdqsort_metrics_3variants_20261103.csv). The older [`../results/rq3_pdqsort_metrics.csv`](../results/rq3_pdqsort_metrics.csv) contains a prior six-algorithm run including noA+noB and is historical only; `rq3_pdqsort.csv` is also retained unchanged.

The benchmark used the assignment's RQ2 matrix:

- Sizes: 1,000,000 and 10,000,000.
- D1 and D3: seeds 20261016, 20261017, and 20261018.
- D2 and D4: seed 20261016; these datasets are deterministic under the specified generator.
- D5: K = 1,000, 10, and 2, each with the three seeds above.
- Algorithms: course QuickSort (`hoare-med3`), pdqsort full, noA, noB, and `Arrays.sort`. The combined noA+noB variant is not part of the RQ3 comparison.
- Two warm-ups and three timed repeats for each algorithm and dataset. Every algorithm receives the same generated input, by cloning it before each run.

Before timing, the runner creates the dataset and an oracle sorted with `Arrays.sort`. It checks every completed output against the oracle before accepting the row. `output_check` is the specified rolling hash of that sorted output. A mismatch aborts the run before a CSV is written.

Each timed sort runs with metric collection disabled. QuickSort and pdqsort metrics are collected in separate passes on clones of the same input, outside the timed interval, and those outputs are also checked against the oracle. The pdqsort comparison count includes each evaluated key comparison in pivot selection, partitioning, insertion sorts, and HeapSort fallback; index and loop-control comparisons are excluded. The counter only records data and does not change sort decisions.

The current benchmark runner also records `heap_before_bytes` and `heap_peak_bytes`, sampled from the JVM heap pools around each timed sort. The historical 612-row CSV predates those columns: it has 21 columns, so its results do not include heap measurements. The latest three-variant CSV has 510 rows and 23 columns.

The matrix has 17 dataset/seed combinations per size, 5 algorithms (the three PDQSort variants plus course QuickSort and `Arrays.sort`), and 3 measured repeats: 510 result rows. The CSV records JDK, OS, and available processor count for reproducibility.

## Latest three-variant run

The full matrix completed on 2026-11-03 in 436.86 seconds using Java 17.0.18 on Windows 11, with 12 available processors. It used two warm-ups and three timed repeats for each algorithm and input. All 510 rows passed the output check; the three PDQSort variants are full, noA, and noB. For each of the 34 dataset/seed/size combinations, the output checksum was identical across all algorithms and repeats.

Mean elapsed time across the 17 dataset/seed combinations and 3 repeats per size:

| PDQSort variant | N = 1,000,000 | N = 10,000,000 |
|---|---:|---:|
| full | 43.312 ms | 453.401 ms |
| noA | 52.704 ms | 507.614 ms |
| noB | 62.903 ms | 687.623 ms |

Selected per-dataset means, averaged over the specified seeds and three repeats:

| Dataset | N | full | noA | noB |
|---|---:|---:|---:|---:|
| D1 | 1,000,000 | 126.65 ms | 148.71 ms | 124.81 ms |
| D1 | 10,000,000 | 1,308.09 ms | 1,369.43 ms | 1,287.53 ms |
| D2 | 1,000,000 | 3.46 ms | 28.46 ms | 3.41 ms |
| D2 | 10,000,000 | 33.32 ms | 322.58 ms | 33.00 ms |
| D3 | 1,000,000 | 21.84 ms | 29.85 ms | 20.06 ms |
| D3 | 10,000,000 | 248.24 ms | 319.70 ms | 248.54 ms |
| D5, K = 10 | 1,000,000 | 21.69 ms | 21.75 ms | 60.71 ms |
| D5, K = 10 | 10,000,000 | 229.69 ms | 222.80 ms | 696.43 ms |
| D5, K = 2 | 1,000,000 | 9.57 ms | 9.54 ms | 47.35 ms |
| D5, K = 2 | 10,000,000 | 88.69 ms | 88.83 ms | 552.48 ms |

On D1, the max-to-min spread among the three variant means was 16.1% of the maximum at N = 1,000,000 and 6.0% at N = 10,000,000. Thus D1 is reasonably close at the larger size, while the smaller-size run has a noticeable spread; these values alone do not establish whether the difference is statistically abnormal. On D2 and D3, noA was slower than full, consistent with Mechanism A helping on ordered and nearly ordered input. On D5 with K = 10 or 2, noB was substantially slower than full, consistent with Mechanism B helping duplicate-heavy inputs. These are observed elapsed-time trends for this run, not guarantees across machines.

## Results

The following values are historical measurements from the earlier six-algorithm run and are not the final result set for the current three-variant RQ3. The older CSV includes noA+noB and predates heap columns.

The table gives median elapsed time in milliseconds over the 51 timed rows per algorithm and size (17 dataset/seed combinations × 3 repeats). These are medians over the combined RQ2 distributions, not per-distribution medians.

| Algorithm / variant | N = 1,000,000 | N = 10,000,000 |
|---|---:|---:|
| Course QuickSort (`hoare-med3`) | 38.235 | 442.948 |
| pdqsort full | 17.211 | 210.520 |
| pdqsort noA | 24.354 | 269.420 |
| pdqsort noB | 51.456 | 626.545 |
| `Arrays.sort` | 12.690 | 172.347 |

Median pdqsort key-comparison counts over the same 51 rows:

| pdqsort variant | N = 1,000,000 | N = 10,000,000 |
|---|---:|---:|
| full | 12,669,941 | 145,656,508 |
| noA | 14,844,046 | 148,312,601 |
| noB | 37,245,964 | 624,703,087 |

Relative to full pdqsort's median comparison count, noA is higher by 17.2% and 1.8% at the two sizes; noB is higher by 194.0% and 328.9%. These are aggregate medians across the historical dataset matrix. They describe this implementation and workload; they are not a claim that either mechanism has the same effect on every individual distribution.

The full configuration's aggregate median elapsed time is below course QuickSort's at both sizes in this historical run. `Arrays.sort` has the lowest aggregate median among the five reported algorithms at both sizes. Runtime rankings vary by distribution, so the CSV should be consulted for per-dataset conclusions.

The historical CSV has 612 rows with `status=PASS` and an oracle checksum; no QuickSort stack overflow was recorded in the timed or counted runs. Its extra noA+noB rows mean it is not the final CSV for the current RQ3 comparison.

## Runtime and JDK limitation

The assignment describes `java.util.Arrays.sort(int[])` of JDK 8 as its reference implementation. The latest three-variant run used Java 17.0.18 on Windows 11 (10.0), with 12 available processors; the earlier six-algorithm run used Java 17.0.12 and 16 processors. Neither run verifies the required JDK 8 baseline. The project POM sets `maven.compiler.release` to 17, and the source uses Java 17 language/API features.

Whether JDK 17 is acceptable for the required JDK 8 reference has not been confirmed. No compiler target, source code, or runtime configuration was silently changed. The results must be rerun under an agreed JDK 8-compatible project/runtime setup, or the JDK 17 deviation must be explicitly accepted, before claiming the baseline requirement is met.

## Reproducibility

From the project root, after compiling with JDK 17:

```powershell
java -Xmx4g -cp target/classes edu.csd201.sort.Rq3Benchmark --output results/rq3_pdqsort_metrics_3variants_new.csv
```

The runner refuses to overwrite an existing output file. Use a new `--output` path for subsequent runs.
