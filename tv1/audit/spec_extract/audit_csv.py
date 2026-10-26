import csv
import math

path = r"C:\Users\phucv\Downloads\csd\tv1\out\quick_sort_results.csv"
expected_prefix = [
    "algorithm", "variant", "dataset", "N", "K", "seed", "elapsed_ms",
    "heap_before_bytes", "heap_peak_bytes", "output_check",
]

with open(path, "r", encoding="utf-8", newline="") as handle:
    reader = csv.DictReader(handle)
    header = reader.fieldnames
    rows = list(reader)

print("HEADER=" + ",".join(header))
print("ROW_COUNT=" + str(len(rows)))
print("PREFIX_OK=" + str(header[:10] == expected_prefix))
print("COUNT_COLUMNS_PRESENT=" + str("comparisons" in header and "max_depth" in header))
print("REQUIRED_FIRST10=" + ",".join(expected_prefix))

mix_bad = 0
sort_bad = 0
for index, row in enumerate(rows, 2):
    mode = row["mode"]
    if mode == "time":
        time_ok = row["elapsed_ms"] != "" and row["heap_before_bytes"] != "" and row["heap_peak_bytes"] != ""
        count_empty = row["comparisons"] == "" and row["max_depth"] == ""
        if not time_ok or not count_empty:
            mix_bad += 1
            print("MIX_BAD_LINE=" + str(index) + " mode=time " + str(row))
    elif mode == "count":
        count_ok = row["comparisons"] != "" and row["max_depth"] != ""
        timing_empty = row["elapsed_ms"] == "" and row["heap_before_bytes"] == "" and row["heap_peak_bytes"] == ""
        if not count_ok or not timing_empty:
            mix_bad += 1
            print("MIX_BAD_LINE=" + str(index) + " mode=count " + str(row))
    else:
        mix_bad += 1
        print("MIX_BAD_LINE=" + str(index) + " unknown_mode=" + mode)
    if row["sorted_ok"] != "true":
        sort_bad += 1

print("MIX_BAD=" + str(mix_bad))
print("SORTED_OK_BAD=" + str(sort_bad))

keys = {}
for row in rows:
    key = (row["dataset"], row["N"], row["mode"])
    keys[key] = keys.get(key, 0) + 1
print("MATRIX=" + ";".join(
    "%s:N=%s:%s=%d" % (dataset, n, mode, count)
    for (dataset, n, mode), count in sorted(keys.items())))

output_ok = True
for dataset in ("d1", "d2"):
    for row_time in rows:
        if row_time["dataset"] != dataset or row_time["mode"] != "time":
            continue
        n = row_time["N"]
        match = [r for r in rows if r["dataset"] == dataset and r["N"] == n and r["mode"] == "count"]
        if len(match) != 1:
            output_ok = False
            print("OUTPUT_CHECK_NO_MATCH dataset=" + dataset + " N=" + n)
            continue
        row_count = match[0]
        same = row_time["output_check"] == row_count["output_check"]
        output_ok = output_ok and same
        print("OUTPUT_CHECK dataset=" + dataset + " N=" + n
              + " time=" + row_time["output_check"]
              + " count=" + row_count["output_check"]
              + " same=" + str(same))
print("OUTPUT_CHECK_ALL_TIME_COUNT_SAME=" + str(output_ok))

for row in rows:
    if row["mode"] != "count":
        continue
    n = int(row["N"])
    comparisons = int(row["comparisons"])
    ratio = comparisons / float(n)
    theory = 1.386 * math.log(n, 2)
    deviation = (ratio / theory - 1.0) * 100.0
    print("COUNT_RATIO dataset=" + row["dataset"] + " N=" + str(n)
          + " comparisons=" + str(comparisons)
          + " comparisons_per_n=%.6f" % ratio
          + " theory=%.6f" % theory
          + " deviation_pct=%.3f" % deviation)

for row in rows:
    if row["mode"] != "time":
        continue
    n = int(row["N"])
    before = int(row["heap_before_bytes"])
    peak = int(row["heap_peak_bytes"])
    print("HEAP dataset=" + row["dataset"] + " N=" + str(n)
          + " before=" + str(before)
          + " peak=" + str(peak)
          + " delta=" + str(peak - before)
          + " before_over_8N=%.3f" % (before / float(8 * n)))
