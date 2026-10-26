# TV1 - Quick Sort (Hoare + median-of-three) - Cặp 1 / RQ1

Bàn giao TV1: mã thuật toán, bộ sinh D1–D2, test log và CSV kết quả RQ1.

## 1. Nội dung đã làm

- **Thuật toán** (`src/main/java/sorting/QuickSort.java`): phân hoạch **Hoare** 2 con trỏ (i chạy phải, j chạy trái, đổi chỗ từng cặp sai vị trí), pivot **median-of-three** trên `a[lo]`, `a[mid]`, `a[hi-1]`, **đệ quy cả hai nhánh**, không Lomuto, không chuyển insertion sort cho đoạn nhỏ.
- **API dùng chung**:
  - `QuickSort.sort(int[] a)` - sắp cả mảng.
  - `QuickSort.sort(int[] a, int lo, int hi)` - sắp nửa mở `[lo, hi)` (khớp quy ước `sort_subsegment(lo, hi)` của TV3).
  - `QuickSort.sortCounted(int[] a)` / `sortCounted(a, lo, hi)` - chế độ đếm, trả `comparisons` (số phép so sánh khóa) và `maxDepth` (độ sâu đệ quy lớn nhất).
- **Generator** (`src/main/java/sorting/DataGenerator.java`):
  - `d1(n, seed)`: `SplittableRandom(seed)`, khóa đều trong `[0, 2^31-1]`, tái lập theo seed.
  - `d2(d1)`: clone của D1 rồi `Arrays.sort` tăng dần, giữ nguyên đúng multiset của D1.
- **Test** (`src/test/java/sorting/QuickSortTest.java`): 5 ca cố định của đề, 4 ca lớn `N=100.000` (toàn bằng nhau / tăng / giảm / hai giá trị 0 và `Integer.MAX_VALUE`), 1.000 mảng ngẫu nhiên `N∈[0,100]` + 200 mảng trùng khóa, quét vét cạn toàn bộ 3.280 mảng dài 0–7 trên `{0,1,2}` và 46.233 hoán vị dài 1–8, kiểm tra generator. Kết quả: **42/42 PASS** trên cả hai runtime - log chính thức `out/quick_sort_test_log.txt` (JDK 1.8.0_45) và `out/quick_sort_test_log_jdk17.txt` (JDK 17.0.18).
- **Runner RQ1** (`src/main/java/sorting/bench/QuickBench.java`): đo `elapsed_ms` chỉ quanh lời gọi sort; sinh dữ liệu, clone để kiểm tra và `output_check` đều nằm **ngoài** vùng đo; xuất CSV + metadata JSON.

## 2. Cách chạy

Cách nhanh nhất: chạy script gộp (build → test → benchmark time → benchmark count):

```powershell
powershell -ExecutionPolicy Bypass -File tv1\run_all.ps1 -Include50m
```

Hoặc chạy tay từng bước như dưới đây.

Biên dịch (mã viết theo JDK 8, đã build bằng `--release 8`):

```powershell
javac --release 8 -d tv1/out/classes `
  tv1/src/main/java/sorting/QuickSort.java `
  tv1/src/main/java/sorting/DataGenerator.java `
  tv1/src/main/java/sorting/bench/QuickBench.java `
  tv1/src/test/java/sorting/QuickSortTest.java
```

Test đúng đắn:

```powershell
java -cp tv1/out/classes sorting.QuickSortTest --log=tv1/out/quick_sort_test_log.txt
```

Benchmark thời gian RQ1 (ghi tiếp vào cùng file CSV nếu file đã tồn tại):

```powershell
java -Xmx4g -cp tv1/out/classes sorting.bench.QuickBench --mode=time `
  --out=tv1/out/quick_sort_results.csv --meta=tv1/out/quick_sort_metadata.json --run-id=<id>
```

Lượt đếm, chạy **riêng** (không trộn với lượt đo thời gian):

```powershell
java -Xmx4g -cp tv1/out/classes sorting.bench.QuickBench --mode=count --include-50m `
  --out=tv1/out/quick_sort_results.csv --meta=tv1/out/quick_sort_metadata_count.json --run-id=<id>
```

Tùy chọn khác: `--ns=100000,1000000,...`, `--dataset=d1|d2`, `--seed=N`, `--reps=N`, `--include-50m`.

## 3. Kết quả RQ1 (D1, seed 20261016)

**Bảng chính thức - JDK 8 (`1.8.0_45`, HotSpot 64-bit, Intel i7-13620H 16 luồng, 16 GB RAM, `-Xmx4g`, heap trần 3.817.865.216 B, mỗi cấu hình 1 lượt)** - file `out/quick_sort_results.csv`:

| N | elapsed_ms | heap_before_bytes | heap_peak_bytes | comparisons | max_depth | sorted_ok |
| --- | --- | --- | --- | --- | --- | --- |
| 100.000 | 9,878 | 2.460.200 | 3.781.424 | 2.426.192 | 32 | true |
| 1.000.000 | 104,007 | 8.473.072 | 9.794.752 | 28.451.182 | 42 | true |
| 5.000.000 | 505,981 | 40.472.240 | 41.772.800 | 156.474.253 | 45 | true |
| 10.000.000 | 2.378,896 | 80.472.240 | 81.780.192 | 326.328.813 | 49 | true |
| 20.000.000 | 3.804,329 | 160.472.240 | 161.780.192 | 668.488.733 | 52 | true |
| 50.000.000 | 5.647,613 | 400.472.240 | 401.780.192 | 1.755.073.021 | 54 | true |

**Bảng tham chiếu - OpenJDK 17.0.18 (cùng máy, cùng seed)** - file `out/quick_sort_results_jdk17.csv`:

| N | elapsed_ms | comparisons | max_depth | sorted_ok |
| --- | --- | --- | --- | --- |
| 100.000 | 21,703 | 2.426.192 | 32 | true |
| 1.000.000 | 88,375 | 28.451.182 | 42 | true |
| 5.000.000 | 464,850 | 156.474.253 | 45 | true |
| 10.000.000 | 2.024,138 | 326.328.813 | 49 | true |
| 20.000.000 | 5.141,852 | 668.488.733 | 52 | true |
| 50.000.000 | 11.875,917 | 1.755.073.021 | 54 | true |

Ghi chú:

- `N=5×10^7` chỉ chạy vì máy đủ RAM (đề cho phép "nếu đủ RAM").
- `comparisons`, `max_depth` và `output_check` **giống tuyệt đối** giữa JDK 8 và JDK 17 trên cùng seed - thuật toán tất định và tái lập được; chỉ `elapsed_ms` thay đổi theo runtime/JIT (lần này JDK 8 nhanh hơn ở N lớn, ví dụ N=5×10^7: 5,65 s so với 11,88 s). Vì vậy **mọi so sánh giữa các thuật toán phải chạy trên cùng một JDK**.
- JDK 8 báo `os=Windows 8.1` do khả năng nhận diện hệ điều hành của bản `1.8.0_45`; máy thật là Windows 11.
- `max_depth` tăng theo `log2(N)` (54 so với `log2(5×10^7)≈25,6`).
- `comparisons/N` so với lý thuyết Hoare trung bình `1,386·log2(N)`: N=10^5 lệch +5,4%; N=10^6 +3,0%; N=5×10^6 +1,5%; N=10^7 +1,25%; N=2×10^7 −0,6%; N=5×10^7 −1,0% - đúng dạng "xấp xỉ hằng số nhân `N·log2(N)`" cộng thêm chi phí chọn pivot median-of-three (2-3 so sánh mỗi lần gọi partition).
- `output_check` trùng nhau giữa lượt `time` và lượt `count` cho cùng N (ví dụ N=100.000: `224665127727807511`) - dấu hiệu hai lượt thực sự sắp cùng một dữ liệu.

### 3.1. Chạy thêm trên D2 (đã sắp tăng) cho RQ2/RQ3

| N | dataset | JDK 8 elapsed_ms | JDK 17 elapsed_ms | comparisons | max_depth | sorted_ok |
| --- | --- | --- | --- | --- | --- | --- |
| 1.000.000 | d2 | 30,250 | 43,857 | 23.427.447 | 22 | true |
| 10.000.000 | d2 | 187,601 | 173,316 | 267.445.697 | 25 | true |

Nhận xét: trên D2 pivot median-of-three rơi đúng giữa dãy nên phân hoạch cân bằng gần hoàn hảo - `max_depth` chỉ 22/25 và thời gian nhanh hơn D1 khoảng 2 lần (N=10^6) và 11,7 lần (N=10^7). Đây là dữ liệu tốt nhất cho Quick Sort, ngược hẳn với lo ngại "Quick Sort sụp trên mảng đã sắp".

Còn thiếu để đủ ma trận RQ2: D3/D4 do TV2 sinh và D5 (K=1000, 10, 2) do TV4 sinh, cùng **3 seed chính thức ghi trong đề** - các seed này không có trong file phân công nên phải lấy từ đề tài trước khi chạy chốt.

## 4. Schema CSV (`out/quick_sort_results.csv`)

Thứ tự cột (các cột đầu đúng quy ước nhóm):

`algorithm, variant, dataset, N, K, seed, elapsed_ms, heap_before_bytes, heap_peak_bytes, output_check, comparisons, max_depth, mode, aux_bytes, sorted_ok, rep, jdk, os, arch, cores, max_heap_bytes, run_id`

- `mode=time`: có `elapsed_ms` + bộ nhớ, để trống `comparisons`/`max_depth`.
- `mode=count`: có `comparisons` + `max_depth`, để trống `elapsed_ms` + bộ nhớ (không trộn hai chế độ).
- `K` để trống với D1/D2; dành cho D5 của TV4.
- `output_check` = `h = 31*h + a[i]` với `h` kiểu `long`, khởi tạo 0, cho phép tràn; kiểm tra đúng/sai thực hiện bằng so từng phần tử với `Arrays.sort` **ngoài** vùng đo.
- Mọi lượt chạy lỗi (nếu có, ví dụ `StackOverflowError` khi chạy killer adversary với `--tolerate-errors`) được ghi riêng vào `out/quick_sort_results.errors.csv` để **không phá schema** của file kết quả chính; không có flag đó thì lỗi làm runner thoát với mã lỗi để không âm thầm che bug.
- `run_id` phân biệt từng lượt chạy; các file metadata `out/quick_sort_metadata*.json` ghi môi trường, `ns`, seed và mode của lượt tương ứng.

## 5. Giới hạn và việc còn lại

- **JDK**: bảng chính thức đã chạy trên JDK `1.8.0_45` (đúng yêu cầu Quy ước chung!B11). Bảng JDK 17 giữ lại chỉ để tham chiếu; khi so sánh thuật toán phải cùng JDK. `run_all.ps1` dùng `java` trên PATH, nên muốn tái lập JDK 8 thì đặt `C:\Program Files\Java\jdk1.8.0_45\bin` lên trước PATH.
- **Bộ nhớ phụ**: `heap_before_bytes`/`heap_peak_bytes` lấy bằng `Runtime.totalMemory()-freeMemory()` + thread lấy mẫu 1 ms. Đây là số **tạm**; số chính thức phải lấy từ framework của TV3 để Quick/Merge/Heap/Radix/pdq đo cùng một cách.
- **Số lượt**: mỗi cấu hình 1 lượt, cùng một JVM cho cả 6 mức N (chưa warmup/JIT tách biệt).
- **Killer adversary**: Quick Sort thuần (đúng đề, không cutoff) sẽ đệ quy sâu và có thể `StackOverflowError` trên dữ liệu Musser/McIlroy - TV6 tạo dữ liệu, TV1 chạy và ghi đúng trạng thái crash, không "sửa" thuật toán để né.
- **Tích hợp**: CSV này phải gửi cho TV2 (tổng hợp RQ1), TV5/TV6 (baseline RQ3); chạy thêm trên D3/D4 (TV2) và D5 (TV4) ở `N=10^6` và `10^7` trước khi chốt RQ2/RQ3.

## 6. Hậu kiểm sau audit (2026-10-16 21:15)

- `run_all.ps1` đã bỏ tiền tố `rtk` ở bước build: script chỉ cần `javac`/`java` trên PATH (khuyến nghị JDK 8 để khớp bảng chính thức). Lý do: `rtk` là công cụ cục bộ của máy tác giả, máy thành viên khác không có.
- Đã chạy lại script với `-OutDir` trỏ ra thư mục tạm: exit 0, test 42/42 PASS, tại `N=10^6` D1 cho `comparisons=28451182`, `max_depth=42`, `output_check=-4074488443990278306` - khớp bảng JDK 8; `tv1\out` chính thức không bị ghi đè.
- SHA-256 `run_all.ps1` sau sửa: `06072b0fe7bf04328b2d47a2421eb23d9362a458a42e341e4aa73bc1cfcf92f5` (bản audit chụp trước khi sửa: `6e3ea0ba4775c60a56f6202a0f9ec0e5e706247d9b0e555f9ed5357b952c3517`). Thuật toán, generator, test và CSV không đổi.
