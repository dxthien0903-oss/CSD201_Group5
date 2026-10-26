# AUDIT TV1 — Quick Sort (Hoare + median-of-three)

Phạm vi audit: chỉ đọc `tv1\src`, `tv1\out`; chỉ ghi `tv1\audit\`.
Thời điểm chốt hash: 2026-10-16 ~21:04–21:20 (giờ máy, Asia/Bangkok), sau khi các artifact chính thức được tái sinh lúc 20:56–21:01.
JVM dùng cho mọi kiểm chứng độc lập trong báo cáo: OpenJDK `17.0.18` (Windows 11). CSV chính thức do JDK `1.8.0_45` chạy.
Mọi verdict giới hạn trong phạm vi TV1 (RQ1 + baseline RQ2/RQ3); không suy diễn sang TV2–TV6.

## 0. Revision identity (SHA-256, tính tại thời điểm viết báo cáo)

| Artifact | Bytes | SHA-256 |
|---|---|---|
| `C:\Users\phucv\Downloads\csd\Phan_cong_chuan_theo_de_Sorting_pdqsort.xlsx` | 23958 | `6edd286668a7f1229dd30afc0d02c1a8ca561aabd3d3360e517ec3eeff67c0b3` |
| `tv1\src\main\java\sorting\QuickSort.java` | 5641 | `38463374a70a50d254146dfcfa2b5c3cf03334bec575980b32f16abfee1c5c56` |
| `tv1\src\main\java\sorting\DataGenerator.java` | 1519 | `f2cde0c01db858af01cf3a00dd35f52d6b54a6a015ebde337973905a78f6728f` |
| `tv1\src\main\java\sorting\bench\QuickBench.java` | 16527 | `46ea277de7433aacb8075d288ddd5d9ef6067a8039d4e97d3f3dc54c5ba5a69c` |
| `tv1\src\test\java\sorting\QuickSortTest.java` | 14352 | `7c1e8da42a207f35bdbe7b86c3961c989845a9d69cd0931e58a43559c364c6f0` |
| `tv1\run_all.ps1` | 2592 | `6e3ea0ba4775c60a56f6202a0f9ec0e5e706247d9b0e555f9ed5357b952c3517` |
| `tv1\out\quick_sort_results.csv` (JDK8, official) | 2866 | `b505d3c8fee2c7837446923b8ada8743ef2979fcdf8723a36fbfb776c69c917e` |
| `tv1\out\quick_sort_results_jdk17.csv` | 2744 | `f11fc9cc7157b1d81ca44b231dd08faa83e8147fcf167aac18b0877e865b8d98` |
| `tv1\README.md` | 9399 | `0cfb89111967c2c8afb224a7adbb99abffb552ff2385bc4ff49555979b7bf7a1` |
| `tv1\out\quick_sort_test_log.txt` (JDK8) | 2105 | `1410e4b4bd815ebc093f840dbbe2e3b308e57c61af92c3d45c552b503a04a227` |
| `tv1\out\quick_sort_test_log_jdk17.txt` | 2095 | `7b713315c82fb5f452be3d5b61605ad48d8e00c80c4aa49adb57415caff58170` |
| `tv1\audit\harness\IndependentAudit.java` | 19307 | `15e15fca046e51dfa6ec0853f64ce45bb2071a6a0444b0fabe3e2b91cd86a2f5` |
| `tv1\audit\killer\KillerAttempt.java` | 8078 | `ad3e98224092fbbd5a813977cd45559913a61e1438eaa1a98e0a2a3a8315495a` |

Class biên dịch tại thời điểm audit:

| Class | SHA-256 |
|---|---|
| `out\classes\sorting\QuickSort.class` | `433630c220d3931b3af1bc2aad27bf18edcf7bcaba54980f8c55463d8ec79871` |
| `out\classes\sorting\DataGenerator.class` | `d04f4f8ec8d4a9d7a773ef33d14278c6e21c73271e0af39fe029ccaa2091cea1` |
| `out\classes\sorting\bench\QuickBench.class` | `e0fa231000e4c44c7dfb4eeda03171ccd578ff3f1b85ec69ef2581b95b6ecad6` |
| `out\classes\sorting\QuickSortTest.class` | `22e276bdd0da6c3e0b789ccee9996d1dc1a67004951fc6f24b2feaed85f8ef10` |

Ghi chú revision: `audit\revision\current_classes` là bản tự biên dịch `--release 8` từ `src` hiện tại; `QuickBench.class` ở đó trùng byte-for-byte với `out\classes` (`e0fa2310…`), xác nhận `out` khớp `src` hiện tại.

## 1. Revision drift trong lúc audit (đã đối chứng)

- `QuickBench.java` đổi 16371 → 16527 B lúc `20:56:06`, thêm `--tolerate-errors`, `writeErrorRow`, `errorPath`, `quote`.
- Lõi thuật toán không đổi: `QuickSort.class` và `DataGenerator.class` giống hệt byte-for-byte giữa bản compile trước thay đổi (`audit\source_classes`, 20:45), `out\classes` và bản compile sau thay đổi (`audit\revision\current_classes`).
- `QuickBench.class` đổi (`6c100ff7…` → `e0fa2310…`). So bytecode đã chuẩn hóa (bỏ constant-pool index/offset/branch target, `javap -p -c -constants`, script `audit\revision\compare_bench.py`) cho thấy `runTimed`, `runCounted`, `row`, `appendRow`, `writeMeta`, `generate`, `outputCheck`, `HeapSampler`, `CSV_HEADER` giống hệt; khác duy nhất: `main` (thêm catch ghi lỗi), `Options.parse` (thêm cờ) và 3 method mới. Kết luận: thay đổi cộng thêm, không chạm vùng đo thời gian, schema CSV hay logic đếm.
- `out\quick_sort_results.csv` bản JDK17 cũ đã bị thay bằng bản JDK8 official lúc `20:58:03`; bản JDK17 lưu tại `out\quick_sort_results_jdk17.csv`. Bằng chứng "pre-change" trong báo cáo này chỉ áp cho revision trước 20:56 và được đánh dấu rõ.

## 2. Lệnh tái lập

```powershell
# Biên dịch độc lập từ src hiện tại
rtk run -- javac --release 8 -d 'C:\Users\phucv\Downloads\csd\tv1\audit\revision\current_classes' `
  'C:\Users\phucv\Downloads\csd\tv1\audit\revision\src_snapshot\QuickSort.java' `
  'C:\Users\phucv\Downloads\csd\tv1\audit\revision\src_snapshot\DataGenerator.java' `
  'C:\Users\phucv\Downloads\csd\tv1\audit\revision\src_snapshot\QuickBench.java'

# Harness độc lập (brute force + generator + segment + count)
rtk run -- javac --release 8 -cp 'C:\Users\phucv\Downloads\csd\tv1\out\classes' `
  -d 'C:\Users\phucv\Downloads\csd\tv1\audit\harness\classes' `
  'C:\Users\phucv\Downloads\csd\tv1\audit\harness\IndependentAudit.java'
rtk run -- java -Xmx2g -cp 'C:\Users\phucv\Downloads\csd\tv1\audit\harness\classes;C:\Users\phucv\Downloads\csd\tv1\out\classes' audit.IndependentAudit

# Adversary có giới hạn, N=10000
rtk run -- javac --release 8 -cp 'C:\Users\phucv\Downloads\csd\tv1\out\classes' `
  -d 'C:\Users\phucv\Downloads\csd\tv1\audit\killer\classes' `
  'C:\Users\phucv\Downloads\csd\tv1\audit\killer\KillerAttempt.java'
rtk run -- java -Xmx2g -cp 'C:\Users\phucv\Downloads\csd\tv1\audit\killer\classes;C:\Users\phucv\Downloads\csd\tv1\out\classes' audit.killer.KillerAttempt

# Đường lỗi (OOM) — có/không cờ
rtk run -- java -Xmx12m -cp 'C:\Users\phucv\Downloads\csd\tv1\out\classes' sorting.bench.QuickBench --mode=time --dataset=d1 `
  --ns=5000000 --seed=20261016 --out='...\audit\revision\tol_errors_main.csv' --tolerate-errors --run-id=AUDIT-OOM-TOLERANT
rtk run -- java -Xmx12m -cp 'C:\Users\phucv\Downloads\csd\tv1\out\classes' sorting.bench.QuickBench --mode=time --dataset=d1 `
  --ns=5000000 --seed=20261016 --out='...\audit\revision\strict_main.csv' --run-id=AUDIT-OOM-STRICT
```

## 3. Verdict R1–R6

| Mã | Hạng mục (sheet `TV1`) | Verdict | Cơ sở chính |
|---|---|---|---|
| R1 | Code thuật toán: Hoare, median-of-three (đầu/giữa/cuối), đệ quy hai phía, không Lomuto, không chuyển Insertion Sort | **VERIFIED** | `QuickSort.java:104-133` (Hoare 2 con trỏ), `:105-106,135-147` (median of 3), `:82-86` (đệ quy cả hai phía), `:122-127` (guard tiến triển); không có Lomuto/3-way/insertion-cutoff trong code (chỉ nhắc trong comment `:5`); harness độc lập `RESULT PASS` |
| R2 | Dữ liệu: D1 `SplittableRandom(seed)` ∈ [0, 2^31−1]; D2 = clone D1 rồi sort tăng; tái lập theo seed; D2 giữ nguyên multiset | **VERIFIED** | `DataGenerator.java:25-35`, `:38-45`; tái lập độc lập D1 N=1e6 khớp byte-for-byte; `D1_REPRO_SAME_SEED=true`, `D1_DIFF_OTHER_SEED=true`, `D2_ASCENDING=true`, `D2_SAME_MULTISET=true` |
| R3 | Test: 5 ca cố định, 1.000 mảng ngẫu nhiên, mảng 100.000 toàn bằng nhau/tăng/giảm, đối chiếu `Arrays.sort` ngoài vùng đo | **VERIFIED** | `QuickSortTest.java` (mục `[fixed cases]` ~`:96`, random ~`:171`, large N=1e5 ~`:146`, counted ~`:206`, exhaustive ~`:295`); log JDK8 `SUMMARY passed=42 failed=0`; harness độc lập thêm 5 fixed + 25.200 random + brute force + 7 ca đối kháng N=1e5 |
| R4 | RQ1: Quick trên D1 đủ N, lưu thời gian/bộ nhớ/count/max_depth theo chế độ; CSV đủ seed/JDK/máy/trạng thái | **VERIFIED** (có limitation về bộ nhớ) | `out\quick_sort_results.csv`: 16 dòng, `jdk=1.8.0_45`, `run_id=tv1-rq1-jdk8-20261016-205643`; D1 đủ 6 mức N (1e5…5e7) × 2 mode; D2 pilot 1e6/1e7 × 2 mode; `sorted_ok=true` toàn bộ; `output_check` time = count cho cả 8 cặp; count D1 1e5 = `2426192` khớp tính độc lập |
| R5 | RQ2/RQ3: chạy Quick theo cấu hình RQ2, làm baseline RQ3, phối hợp Killer Adversary | **UNVERIFIED** (một phần) | Chỉ có baseline D1 (1e6/1e7) + D2 1e6/1e7; thiếu D3/D4/D5, 3 seed RQ2, variant pdq/noA/noB/lib; adversary chính thức (Musser/McIlroy, TV6) chưa chạy; thử nghiệm adversary có giới hạn của audit không tìm được counterexample (mục 5) |
| R6 | Bàn giao: commit mã, hướng dẫn chạy, test log, CSV | **VERIFIED** (riêng "commit" = NOT_APPLICABLE) | `README.md` (9399 B, 21:01) có hướng dẫn build/run; test log + CSV hiện diện; `tv1` và `csd` không có `.git` (`git rev-parse` → `not a git repository`) nên không có VCS để commit; tiêu chí "không chỉ gửi ảnh chụp" đạt bằng CSV/log/artifact thật |

## 4. Bằng chứng chi tiết

### 4.1 R1 — thuật toán (raw)

```text
API_EXPECTED_SIGNATURES_PRESENT=true      # 4 API public static: sort(int[]), sort(int[],int,int), sortCounted(int[]), sortCounted(int[],int,int)
FIXED_CASES=5
ALPHABET_TOTAL=9841                       # mọi mảng dài 0..8 trên {0,1,2}
PERMUTATION_TOTAL=409113                  # mọi hoán vị dài 1..9
TOTAL random=25200 adversarial=7 segments=10000 bad=0
RESULT PASS
```

Ca đối kháng N=100.000 (đếm khóa, `--count` semantics):

```text
all-equal-100000 comparisons=2130043 maxDepth=18 sorted=true
ascending-100000 comparisons=2003389 maxDepth=18 sorted=true
descending-100000 comparisons=2003391 maxDepth=18 sorted=true
organ-pipe-100000 comparisons=6307803 maxDepth=99 sorted=true
extremes-100000 comparisons=2156906 maxDepth=24 sorted=true
duplicate-heavy-100000 comparisons=2211750 maxDepth=26 sorted=true
sawtooth-100000 comparisons=2284649 maxDepth=27 sorted=true
```

Kiểm chứng trên bản tự biên dịch `audit\revision\current_classes` cho cùng kết quả `RESULT PASS`; đã chạy cả `out\classes` (sau rebuild 20:59) và bản độc lập.

### 4.2 R2 — generator (raw)

```text
D1_EQUAL_OWN_DELIVERED n=1000000 seed=20261016 equal=true
D1_REPRO_SAME_SEED=true
D1_DIFF_OTHER_SEED=true
D2_ASCENDING=true
D2_SAME_MULTISET=true
```

### 4.3 R3 — test

```text
QuickSort correctness suite (TV1)
java.version=1.8.0_45 java.vm.name=Java HotSpot(TM) 64-Bit Server VM
SUMMARY passed=42 failed=0
RESULT ALL_TESTS_PASSED
```

Log nhóm: `[fixed cases]`, `[sub-segment contract: sort(a, lo, hi)]`, `[large fixed-shape cases, N=100000]`, `[randomized comparison against Arrays.sort]`, `[counted mode]`, `[exhaustive small-input sweep]`, `[D1/D2 generators]`. Vùng đo tách biệt: `QuickBench.java:125-127` sinh + tạo reference trước, cửa sổ đo `:135-137` chỉ bọc `QuickSort.sort`, kiểm đúng `:142-143` sau khi dừng đo.

### 4.4 R4 — CSV official (JDK8) và đếm độc lập

Header thực tế (22 cột, khớp thứ tự tối thiểu của `Quy ước chung!B6` ở 10 cột đầu):

```text
algorithm,variant,dataset,N,K,seed,elapsed_ms,heap_before_bytes,heap_peak_bytes,output_check,comparisons,max_depth,mode,aux_bytes,sorted_ok,rep,jdk,os,arch,cores,max_heap_bytes,run_id
```

- 16 dòng: D1 6 N × 2 mode = 12; D2 1e6/1e7 × 2 mode = 4; `K` rỗng (chỉ dùng cho D5/TV4).
- Tất cả `jdk=1.8.0_45`, `os=Windows 8.1`, `arch=amd64`, `cores=16`, `max_heap_bytes=3817865216`, một `run_id`.
- 8 nhóm `(dataset,N)` có `output_check` time = count; JDK8 và JDK17 khớp nhau toàn bộ 8/8 cặp.
- `output_check` D1 N=1e5 = `224665127727807511`; N=1e6 = `-4074488443990278306`; khớp tính lại độc lập từ D1 + `h=31*h+a[i]`.
- Đếm khóa độc lập (mirror từng dòng của thuật toán, không dùng `QuickSort.Stats`) tại D1 N=1e5 seed 20261016:

```text
COUNT n=100000 seed=20261016 delivered=2426192 independent=2426192 delta=0 expected_csv=2426192
COUNT_DEPTH n=100000 delivered=32 independent=32
```

- Kiểm tra hợp lý thô (không phải chứng minh): tỉ lệ `comparisons/N` so với `1.386·log2(N)` lệch +5,39% (1e5) → +1,46% (5e6) → −0,98% (5e7); phù hợp chi phí cộng thêm và pivot median-of-three, không có dấu hiệu bậc hai.

### 4.5 R6 — đường lỗi OOM (chạy độc lập bởi audit)

```text
--tolerate-errors  -> exit=0, main CSV không có dòng, errors.csv có dòng:
quick,hoare-med3,d1,5000000,20261016,time,1,java.lang.OutOfMemoryError,"Java heap space","sorting.DataGenerator.d1(DataGenerator.java:30)"
không cờ          -> exit=1, không ghi file nào (strict_main.csv MISSING)
```

## 5. Adversary attempt (N=10.000)

Đã chạy `audit\killer\KillerAttempt.java`: 14 họ cấu trúc (all-equal, tăng, giảm, organ-pipe, sawtooth 101/997, hai cực trị, descending/ascending/median-middle/median-last triples, 3 giá trị, block 8/32 giảm), 200 hoán vị ngẫu nhiên seed cố định, 50 mảng ngẫu nhiên 8 giá trị, và hill-climb cục bộ tối đa hoá số so sánh (n=512 × 4000 eval; n=2048 × 1500 eval).

```text
KILLER_HILLCLIMB n=512 evaluations=4000 bestComparisons=8932 bestMaxDepth=24 nLog2n=4608
KILLER_HILLCLIMB n=2048 evaluations=1500 bestComparisons=40106 bestMaxDepth=30 nLog2n=22528
KILLER_CASES=266
KILLER_FAILURES=0
KILLER_WORST=organ-pipe comparisons=421537 maxDepth=62 n=10000
```

Không ca nào sai thứ tự, không `StackOverflowError`/`OutOfMemoryError`; ca xấu nhất ~3,17×`n·log2(n)`, độ sâu 62 — không có dấu hiệu suy thoái bậc hai.
**NOT RUN:** killer thích ứng kiểu Musser/McIlroy (cần bản sao thuật toán có oracle giá trị) và matrix N=1e4/1e5/1e6 do TV6 cung cấp. Kết quả trên là bằng chứng có giới hạn, không thay thế killer chính thức.

## 6. Counterexample / limitation — đã phân xử

Không tìm thấy counterexample cho tính đúng đắn (thứ tự, bất biến segment, tái lập generator, số đếm, schema CSV). Các điểm còn lại:

1. **Heap provisional** — `QuickBench.java:263-266,276-316`: dùng `Runtime.totalMemory()-freeMemory()` + sampler thread 1 ms; `aux_bytes` dương ~1,3 MB dù Quick Sort in-place không cấp phát trong vùng đo (nhiễu GC/JIT). **Documented limitation** trong source + metadata; số liệu bộ nhớ chính thức thuộc TV3.
2. **`reps=1`, không warmup** — `QuickBench.java:328` mặc định `reps=1`, `run_all.ps1` không chạy lặp. **Documented limitation** (README/metadata ghi `reps`); `Quy ước chung!B11` yêu cầu ghi số lần lặp — đã ghi (`rep=1`), nhưng chưa có lặp để lấy trung vị.
3. **`run_id` dùng chung** — 5 file metadata cũ dùng cùng run_id dù khác lượt chạy. **Documented limitation** về traceability; mỗi dòng CSV đều có `run_id` nên không mất dấu nguồn.
4. **N=5e7 chỉ được phủ bởi `quick_sort_metadata_50m.json`** — metadata chính khai 5 mức N, file 50m khai riêng. **Documented limitation**; CSV vẫn ghi `N=50000000` tường minh nên không mất thông tin.
5. **`os=Windows 8.1` trên JDK8** — JDK 1.8.0_45 báo `os.name=Windows 8.1` còn JDK 17.0.18 trên cùng máy báo `Windows 11`; đây là sai lệch do JVM cũ không nhận diện Windows 11. **Kiểm chứng độc lập: cùng một máy, hai JVM báo hai giá trị.** Rủi ro: trường môi trường chưa mô tả đúng host thực tế; đề nghị TV3 ghi OS version ngoài JVM (registry/`winver`) khi chốt môi trường chính thức.
6. **Timeout/OOM state** — main CSV không có cột trạng thái; lỗi được ghi sang `*.errors.csv` riêng khi bật `--tolerate-errors` (`QuickBench.java:87-122`), mặc định fail loudly. Đã kiểm chứng độc lập cả hai nhánh (mục 4.5). **Chấp nhận được** (schema CSV giữ ổn định), nhưng dòng thành công không ghi trạng thái `ok`/`timeout`; đây là **limitation đã biết**.
7. **Tên API phân đoạn** — `Quy ước chung!B4` nói `sort_subsegment(lo, hi)`; TV1 dùng `sort(int[],int,int)`/`sortCounted(int[],int,int)` half-open `[lo,hi)` và ghi rõ trong Javadoc + README. **Chưa phải spec violation** (quy ước đặt tên chưa chốt, TV3+TV5 chịu trách nhiệm thống nhất), nhưng cần đồng bộ trước khi tích hợp.
8. **R3 danh nghĩa 1.000 mảng ngẫu nhiên** — suite chạy đúng nhóm; audit độc lập chạy 25.200 mảng ngẫu nhiên. Không phát hiện lệch.

## 7. Residual uncertainty (handoff TV2/TV3/TV4)

- TV1 chưa chứng minh hành vi bậc hai/độ sâu cực đại dưới killer chính thức (Musser/McIlroy) tại N=1e4/1e5/1e6 — cần TV6 + TV5.
- Chưa có RQ2 đầy đủ (D3/D4/D5, 3 seed, K=1000/10/2) và RQ3 (pdq/noA/noB/lib-Arrays.sort) — ngoài phạm vi TV1.
- Số liệu thời gian là 1 lần chạy/JDK8, không median, không kiểm soát nhiễu hệ thống; mọi so sánh hiệu năng liên thuật toán cần TV3 chạy cùng khung.
- `jshell`/cross-JVM: output_check khớp giữa JDK8 và JDK17 trên máy này, nhưng chưa kiểm tra trên máy khác (portability của `SplittableRandom` đã được xác nhận trong cùng JVM).
- Chưa xác minh `run_all.ps1` trên máy sạch không có `rtk`/JDK8 (script gọi `rtk javac`).

## 8. Kết luận

Không phát hiện defect về tính đúng đắn trong phạm vi TV1. R1–R4, R6 đạt với các limitation đã liệt kê (bộ nhớ provisional, reps=1, metadata 50m tách rời, OS do JDK8 báo sai, commit N/A). R5 chưa đạt: thiếu matrix RQ2/RQ3 và killer chính thức. Điểm rủi ro lớn nhất còn lại là **hiệu năng/độ sâu dưới killer thích ứng** chứ không phải tính đúng đắn.
