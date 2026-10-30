# CSD201 Group 5 — Nghiên Cứu Thực Nghiệm Các Thuật Toán Sắp Xếp & PDQSort

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8%2B-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.10.2-25A162?style=for-the-badge&logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Benchmark](https://img.shields.io/badge/Benchmark-RQ1%20%7C%20RQ2%20%7C%20RQ3-0A66C2?style=for-the-badge)](results/)

Dự án nghiên cứu thực nghiệm, cài đặt từ đầu và đánh giá hiệu năng các thuật toán sắp xếp nền tảng (**Quick Sort**, **Merge Sort**, **Heap Sort**, **Radix Sort LSD**) cùng thuật toán lai hiện đại **Pattern-Defeating Quicksort (PDQSort)** trên mảng số nguyên 32-bit không âm trong khoảng `[0, 2^31 - 1]`.

---

## 👥 Phân Công Nhóm (Group 5)

| Thành viên | Họ và tên | Hạng mục phụ trách chính |
|:---:|---|---|
| **TV1** | **Trần Văn Phúc** | Cài đặt `QuickSort` (Hoare + Median-of-Three), sinh dữ liệu chuẩn `D1`/`D2`, kiểm thử `QuickSortTest` & `KillerAdversaryTest`, điều phối thực nghiệm RQ1 |
| **TV2** | **Lương Đức Duy** | Cài đặt `MergeSort`, sinh dữ liệu `D3`/`D4`, điều phối `Benchmark` tổng hợp cho RQ1 & RQ2 |
| **TV3** | **Trần Minh Quân** | Cài đặt `HeapSort`, phân tích giới hạn bộ nhớ tại chỗ $O(1)$ và cung cấp sắp xếp đoạn con `[lo, hi)` |
| **TV4** | **Đặng Xuân Thiện** | Cài đặt `RadixSort` (LSD base 256), sinh dữ liệu `D5`, xây dựng `MeasureRunner` / `MeasureWorker` cô lập tiến trình JVM |
| **TV5** | **Nguyễn Quốc Bảo** | Cài đặt `PdqSort` (Pattern-Defeating Quicksort) và biến thể `noA`, thực nghiệm RQ3 đối sánh với QuickSort và HeapSort |
| **TV6** | **Lê Hoàng Khang** | Cài đặt biến thể `noB`, kiểm chứng độc lập độ đúng đắn, tổng hợp biểu đồ thực nghiệm và biên soạn báo cáo học thuật |

---

## 🗺️ Cấu Trúc Dự Án

```text
CSD201_Group5/
├── pom.xml
├── README.md
├── docs/
│   ├── Phan_cong_chuan_theo_de_Sorting_pdqsort.xlsx
│   ├── RQ1_Methods_and_Results_Report.md
│   ├── RQ1_QuickSort_Report.md
│   ├── RQ3_Methods_and_Results_Report.md
│   └── TV1_QuickSort_D1_D2_Technical_Report.md
├── results/
│   ├── benchmark.csv
│   ├── comparison_all_algorithms.csv
│   ├── quick_sort_results.csv
│   ├── rq1_benchmark.csv
│   ├── rq2_benchmark.csv
│   ├── rq2_full_comparison.csv
│   ├── rq3_pdqsort.csv
│   └── rq3_pdqsort_metrics.csv
└── src/
    ├── main/java/edu/csd201/sort/
    │   ├── Benchmark.java
    │   ├── DataGenerator.java
    │   ├── ExperimentRunner.java
    │   ├── HeapSort.java
    │   ├── MeasureRunner.java
    │   ├── MeasureWorker.java
    │   ├── MergeSort.java
    │   ├── PdqSort.java
    │   ├── QuickSort.java
    │   ├── RadixSort.java
    │   ├── Rq3Benchmark.java
    │   └── SortMetrics.java
    └── test/java/edu/csd201/sort/
        ├── KillerAdversaryTest.java
        ├── PdqSortTest.java
        ├── QuickSortTest.java
        └── RadixTest.java
```

| Thành phần | Đường dẫn | Vai trò kỹ thuật |
|---|---|---|
| `QuickSort` | `src/main/java/edu/csd201/sort/QuickSort.java` | Quick Sort phân hoạch Hoare kết hợp chọn chốt Median-of-Three và đệ quy hai phía |
| `MergeSort` | `src/main/java/edu/csd201/sort/MergeSort.java` | Merge Sort chia để trị ổn định sử dụng mảng phụ $O(N)$ |
| `HeapSort` | `src/main/java/edu/csd201/sort/HeapSort.java` | Heap Sort vun đống nhị phân tại chỗ với bộ nhớ phụ $O(1)$ |
| `RadixSort` | `src/main/java/edu/csd201/sort/RadixSort.java` | LSD Radix Sort cơ số 256 (8-bit mỗi lượt, 4 lượt chia lô) |
| `PdqSort` | `src/main/java/edu/csd201/sort/PdqSort.java` | Pattern-Defeating Quicksort kết hợp Insertion Sort, 3-way partition và fallback HeapSort |
| `SortMetrics` | `src/main/java/edu/csd201/sort/SortMetrics.java` | Thu thập số phép so sánh, partitions, partial insertions, HeapSort fallbacks và độ sâu |
| `DataGenerator` | `src/main/java/edu/csd201/sort/DataGenerator.java` | Bộ sinh 5 phân phối dữ liệu chuẩn `D1`–`D5` tái lập hoàn toàn theo `seed` |
| `Benchmark` | `src/main/java/edu/csd201/sort/Benchmark.java` | Trình chạy thực nghiệm RQ1, RQ2 và xuất bảng kết quả tổng hợp |
| `MeasureRunner` | `src/main/java/edu/csd201/sort/MeasureRunner.java` | Điều phối đo lường chuẩn đề bài bằng tiến trình JVM con độc lập cho mỗi lượt chạy |
| `ExperimentRunner` | `src/main/java/edu/csd201/sort/ExperimentRunner.java` | Trình chạy kiểm thử ngẫu nhiên (`D1`–`D5`) và đếm chỉ số (`--count`) cho `PdqSort` |
| `Rq3Benchmark` | `src/main/java/edu/csd201/sort/Rq3Benchmark.java` | Benchmark tái lập cho QuickSort, bốn cấu hình pdqsort và `Arrays.sort` |

---

## ✨ Thiết Kế Thuật Toán & Phương Pháp Đo Lường

### 1. Năm phân phối dữ liệu chuẩn (`D1` – `D5`)
- **D1 (Uniform Random):** $N$ số nguyên 32-bit không âm phân phối đều trên $[0, 2^{31}-1]$, sinh bằng `SplittableRandom(seed)` cùng phép mặt nạ bit `& 0x7FFFFFFF`.
- **D2 (Sorted Ascending):** Bản sao của `D1` đã sắp xếp không giảm.
- **D3 (Nearly Sorted):** Bản sao của `D2` thực hiện hoán đổi ngẫu nhiên $\lfloor N / 1000 \rfloor$ cặp vị trí.
- **D4 (Reverse Sorted):** Bản sao của `D2` đảo ngược thứ tự giảm dần.
- **D5 (Many Duplicates):** Sinh $K$ khóa phân biệt từ `seed`, mỗi phần tử chọn ngẫu nhiên đều trong $K$ khóa ($K \in \{1000, 10, 2, 1\}$).

### 2. Nguyên tắc tách biệt chế độ đo (Two-Mode Measurement)
- **Timing Mode (`sort`):** Thực thi thuật toán thuần túy để đo `elapsed_ms` và `heap_peak_bytes`, không chèn biến đếm tránh làm sai lệch tối ưu hóa JIT của JVM.
- **Counted Mode (`sortCounted`):** Thực thi trên bản sao dữ liệu độc lập để thu thập chính xác số phép so sánh khóa (`comparisons`), số phép gán/di chuyển (`assignments` / `writes`) và độ sâu đệ quy cực đại (`max_depth`).

---

## 🚀 Hướng Dẫn Build & Chạy

### Yêu cầu hệ thống
- **JDK:** Phiên bản 17 trở lên
- **Build Tool:** Apache Maven 3.8+

### 1. Biên dịch toàn bộ mã nguồn và test
```bash
mvn clean test-compile
```

### 2. Chạy thực nghiệm RQ1 và RQ2 tổng hợp (`results/rq1_benchmark.csv`, `results/rq2_benchmark.csv`)
```bash
java -Xmx6g -cp target/classes edu.csd201.sort.Benchmark rq1
java -Xmx6g -cp target/classes edu.csd201.sort.Benchmark rq2
java -Xmx6g -cp target/classes edu.csd201.sort.Benchmark full
```

### 3. Chạy bộ đo cô lập từng JVM riêng (`MeasureRunner` / `MeasureWorker`)
```bash
java -Xmx4g -cp target/classes edu.csd201.sort.MeasureRunner worker --algo quick --variant base --dataset D1 --n 1000000 --seed 20261016
java -Xmx4g -cp target/classes edu.csd201.sort.MeasureRunner worker --algo quick --variant base --dataset D1 --n 1000000 --seed 20261016 --count
java -cp target/classes edu.csd201.sort.MeasureRunner selftest
```

---

## 🧪 Kiểm Thử Độ Đúng Đắn (Testing)

### Chạy toàn bộ JUnit 5 Test Suite qua Maven
```bash
mvn test
```

### Chạy riêng từng bộ kiểm thử
```bash
mvn test -Dtest=QuickSortTest
```

```bash
mvn test -Dtest=KillerAdversaryTest
```

```bash
mvn test -Dtest=RadixTest
```

### Chạy kiểm thử thông qua CLI độc lập
```bash
java -cp "target/classes;target/test-classes" edu.csd201.sort.QuickSortTest --log=results/quick_sort_test_log.txt
```

```bash
java -cp "target/classes;target/test-classes" edu.csd201.sort.KillerAdversaryTest
```

### Kiểm thử pdqsort TV6
```bash
java -cp target/classes edu.csd201.sort.ExperimentRunner randomized-test 20261016 10000 results/randomized-test.log
```

Lệnh kiểm tra 10.000 mảng ngẫu nhiên và các ca biên/fallback với `Arrays.sort()` trên
các cấu hình `full`, `noA`, `noB` và `noA+noB`. Kiểm thử `PdqSortTest` độc lập:
```bash
javac -encoding UTF-8 -cp target/classes -d target/test-classes src/test/java/edu/csd201/sort/PdqSortTest.java
java -cp "target/classes;target/test-classes" edu.csd201.sort.PdqSortTest
```

### Tóm tắt độ phủ kiểm thử
- **`QuickSortTest` (7 nhóm JUnit 5):** Kiểm thử 5 ca cố định của đề và biên cực trị, hợp đồng sắp xếp đoạn con `sort(a, lo, hi)`, mảng kích thước lớn $N=100.000$ (bằng nhau/tăng/giảm/cực trị), đối chiếu `Arrays.sort` trên 1.000 mảng ngẫu nhiên toàn dải + 200 mảng trùng khóa + 1.000 mảng ngẫu nhiên trải đều đủ 5 họ `D1`–`D5`, kiểm tra độ sâu đệ quy `maxDepth <= 64`, vét cạn toàn bộ $3.280$ mảng tam phân độ dài $0..7$ cùng $46.233$ hoán vị độ dài $1..8$, và kiểm tra tính tái lập của `DataGenerator` (`D1`/`D2`).
- **`KillerAdversaryTest` (5 nhóm JUnit 5):** Kiểm thử sức chịu đựng của `QuickSort` trước 14 họ dữ liệu có cấu trúc (sawtooth, organ-pipe, median-of-three triples), 250 mảng ngẫu nhiên, thuật toán leo đồi (hill-climbing adversary), và dãy đối kháng **Musser / McIlroy Median-of-Three Killer** tại $N = 10^4, 10^5, 10^6$ đối sánh trực tiếp giữa `QuickSort` và `PdqSort`.
- **`RadixTest`:** Kiểm thử tính đúng đắn của `RadixSort` trên các phân phối dữ liệu.

---

## 📊 Tóm Tắt Kết Quả Thực Nghiệm (RQ1, RQ2 & RQ3)

### 1. RQ1 — Khả năng mở rộng theo kích thước $N$ trên phân phối ngẫu nhiên `D1`

Số liệu đối sánh thời gian chạy (ms) khi $N$ tăng từ $10^5$ đến $5 \times 10^7$ trên phân phối `D1` (trích từ [`results/comparison_all_algorithms.csv`](results/comparison_all_algorithms.csv) và [`results/rq1_benchmark.csv`](results/rq1_benchmark.csv)):

| Kích thước $N$ | QuickSort (ms) | MergeSort (ms) | HeapSort (ms) | RadixSort (ms) | PdqSort (ms) | Arrays.sort (ms) |
|---:|---:|---:|---:|---:|---:|---:|
| `100,000` | 14.49 | 16.85 | 15.68 | **1.05** | 27.63 | 9.97 |
| `1,000,000` | 100.18 | 116.48 | 111.37 | **9.95** | 118.96 | 62.48 |
| `5,000,000` | 561.51 | 1,220.48 | 2,576.57 | **84.09** | 2,710.51 | 1,062.80 |
| `10,000,000` | 2,955.62 | 4,480.05 | 5,852.87 | **235.74** | 6,882.81 | 2,033.62 |
| `20,000,000` | 5,352.74 | 9,822.90 | 13,058.99 | **511.26** | 16,493.78 | 4,614.35 |
| `50,000,000` | 9,027.94 | 13,458.22 | — | **1,057.49** | — | — |

- **Nhận xét RQ1:** `RadixSort` (LSD cơ số 256) đạt tốc độ nhanh nhất ở mọi mốc kích thước nhờ độ phức tạp tuyến tính $O(N)$, nhưng cần mảng phụ $4N$ byte ($200\text{ MB}$ tại $N=5\times 10^7$). Trong nhóm thuật toán so sánh tại chỗ ($O(1)$ bộ nhớ Heap phụ), `QuickSort` (`hoare-med3`) vượt trội hơn `MergeSort` ($1.5\times - 1.8\times$) và `HeapSort` ($2.4\times - 4.5\times$) nhờ tính cục bộ bộ nhớ đệm (cache locality).

---

### 2. RQ2 — Ảnh hưởng của phân phối dữ liệu (`D1` – `D5`) tại $N = 1,000,000$

Số liệu thời gian thực thi (ms) của các thuật toán trên 5 họ phân phối dữ liệu chuẩn (trích từ [`results/rq2_full_comparison.csv`](results/rq2_full_comparison.csv) và [`results/rq2_benchmark.csv`](results/rq2_benchmark.csv)):

| Phân phối dữ liệu | QuickSort (ms) | MergeSort (ms) | HeapSort (ms) | RadixSort (ms) | PdqSort Full (ms) | Arrays.sort (ms) |
|---|---:|---:|---:|---:|---:|---:|
| **D1 (Ngẫu nhiên đều)** | 133.07 | 182.46 | 162.44 | **9.07** | 194.71 | 100.10 |
| **D2 (Tăng dần sẵn)** | 34.25 | 70.73 | 76.42 | 25.11 | 15.66 | **1.04** |
| **D3 (Gần tăng dần)** | 22.68 | 72.76 | 89.39 | 23.89 | 31.51 | **16.34** |
| **D4 (Giảm dần)** | 23.69 | 77.41 | 87.81 | 21.62 | 13.12 | **2.25** |
| **D5 (Trùng lặp $K=1000$)** | 75.23 | 138.03 | 141.09 | **16.49** | 214.25 | 48.82 |
| **D5 (Trùng lặp $K=10$)** | 44.27 | 108.30 | 88.40 | 28.66 | 158.90 | **15.30** |
| **D5 (Trùng lặp $K=2$)** | 30.90 | 82.56 | 44.09 | 19.29 | 130.40 | **10.86** |

- **Nhận xét RQ2:**
  - Trên dữ liệu có thứ tự hoặc gần có thứ tự (`D2`, `D3`, `D4`), `QuickSort` (nhờ chọn chốt Median-of-Three chia đôi cân bằng với `max_depth = 22`) và `PdqSort` xử lý rất nhanh ($13 - 34\text{ ms}$), bỏ xa `MergeSort` và `HeapSort` ($70 - 89\text{ ms}$).
  - Trên dữ liệu nhiều khóa trùng lặp (`D5` với $K=1000, 10, 2$), phân hoạch Hoare hai chiều dừng tại các phần tử bằng chốt giúp `QuickSort` giữ cây đệ quy cân bằng và giảm thời gian từ $133.07\text{ ms}$ (`D1`) xuống $30.90\text{ ms}$ ($K=2$). `RadixSort` duy trì thời gian ổn định quanh $9 - 28\text{ ms}$ bất kể phân phối đầu vào.

---

### 3. RQ3 — Đánh giá `PdqSort` và Phân tích tách biệt Cơ chế A / Cơ chế B

Đối sánh các biến thể của **Pattern-Defeating Quicksort** (`PdqSort Full`, `PdqSort noA` tắt phát hiện đoạn có thứ tự / Insertion Sort sớm, `PdqSort noB` tắt phân hoạch gom khóa trùng `partitionLeft`) cùng `QuickSort` và `HeapSort` (trích từ [`results/rq2_full_comparison.csv`](results/rq2_full_comparison.csv) và [`KillerAdversaryTest.java`](src/test/java/edu/csd201/sort/KillerAdversaryTest.java)):

| Kịch bản / Phân phối ($N = 10^6$) | QuickSort (TV1) | HeapSort (TV3) | PdqSort Full (TV5) | PdqSort `noA` (TV5) | PdqSort `noB` (TV6) | Vai trò cơ chế nổi bật |
|---|---:|---:|---:|---:|---:|---|
| **D1 (Ngẫu nhiên đều)** | **133.07 ms** | 162.44 ms | 194.71 ms | 184.04 ms | 187.14 ms | Chi phí kiểm tra mẫu nhỏ trên dữ liệu ngẫu nhiên |
| **D2 (Tăng dần sẵn)** | 34.25 ms | 76.42 ms | **15.66 ms** | 36.98 ms | 3.20 ms | **Cơ chế A** giúp `Full` nhanh gấp $2.36\times$ so với `noA` |
| **D3 (Gần tăng dần)** | 22.68 ms | 89.39 ms | 31.51 ms | **18.73 ms** | 24.45 ms | Phân hoạch cân bằng kết hợp Insertion Sort đoạn ngắn |
| **D4 (Giảm dần)** | 23.69 ms | 87.81 ms | **13.12 ms** | 31.65 ms | 6.10 ms | **Cơ chế A** phát hiện mẫu giảm dần, nhanh gấp $2.41\times$ so với `noA` |
| **D5 ($K=10$, ít khóa)** | **44.27 ms** | 88.40 ms | 158.90 ms | 177.58 ms | 173.59 ms | **Cơ chế B** (`partitionLeft`) giảm số phép so sánh lặp trên khóa trùng |
| **D5 ($K=2$, nhị phân)** | **30.90 ms** | 44.09 ms | 130.40 ms | 113.29 ms | 121.50 ms | Gom khóa bằng chốt, ngăn suy biến độ sâu đệ quy |
| **Musser Killer ($N=10^5$)** | Suy biến $O(N^2)$ | $O(N \log N)$ | **Chặn đứng $O(N \log N)$** | Chặn đứng $O(N \log N)$ | Chặn đứng $O(N \log N)$ | **Bad-partition limit + Shuffle + Fallback `HeapSort`** bảo vệ cận trên |

- **Nhận xét RQ3:**
  - **Hiệu quả Cơ chế A (`PdqSort Full` vs `noA`):** Khi tắt Cơ chế A (`noA`), thời gian chạy trên dữ liệu đã sắp (`D2`) và giảm dần (`D4`) tăng vọt từ $15.66\text{ ms} \to 36.98\text{ ms}$ và $13.12\text{ ms} \to 31.65\text{ ms}$, chứng minh cơ chế phát hiện thứ tự sớm giúp tiết kiệm hơn $55\%$ thời gian.
  - **Hiệu quả Cơ chế B (`PdqSort Full` vs `noB`):** Trên dữ liệu trùng lặp cao (`D5`), khi tắt `partitionLeft` (`noB`), toàn bộ các phần tử bằng chốt tiếp tục bị đẩy vào nhánh đệ quy bên phải thay vì được cô lập, làm tăng số phép so sánh khóa.
  - **Chống suy biến đối kháng (Musser Median-of-Three Killer):** Trước dãy đối kháng cố tình đánh lừa chốt Median-of-Three của `QuickSort` cổ điển, `PdqSort` đếm số lần phân hoạch lệch (`badAllowed = log2(N)`), tự động xáo trộn mẫu ngẫu nhiên và chuyển sang `HeapSort` để giữ vững độ phức tạp $O(N \log N)$ trong mọi trường hợp.

#### Benchmark tái lập TV6: full, noA, noB và noA+noB

Phần dưới đây là benchmark RQ3 riêng của TV6, bổ sung cho bảng tổng hợp phía trên.
`Rq3Benchmark` chạy `QuickSort` giáo trình, pdqsort với bốn cấu hình và
`Arrays.sort()` trên cùng dữ liệu đầu vào:

- Kích thước `N = 1.000.000` và `10.000.000`.
- `D1` và `D3`: ba seed `20261016`, `20261017`, `20261018`; `D2` và `D4`: seed `20261016`.
- `D5`: `K = 1000`, `10`, `2`, mỗi giá trị có ba seed trên.
- Hai warm-up và ba lượt đo cho mỗi thuật toán/tổ hợp dữ liệu.

Runner xác thực đầu ra với oracle `Arrays.sort()` trước khi ghi CSV; các metrics
được thu ở lượt riêng ngoài thời gian đo. Hai file CSV đã lưu là
[`rq3_pdqsort.csv`](results/rq3_pdqsort.csv) (bản trước khi thêm comparison
counter) và [`rq3_pdqsort_metrics.csv`](results/rq3_pdqsort_metrics.csv) (bản
có comparison counts).

CSV metrics có 612 dòng: 17 tổ hợp dataset/seed × 2 kích thước × 6 thuật toán ×
3 lượt đo. Kiểm tra dữ liệu cho thấy tất cả 612 dòng có `status=PASS` và checksum
đầu ra. Bảng dưới đây là trung vị thời gian (ms) qua 51 lượt cho mỗi thuật toán
và kích thước, gộp 17 tổ hợp dataset/seed; đây không phải trung vị riêng từng
phân phối:

| Thuật toán / cấu hình | N = 1.000.000 | N = 10.000.000 |
|---|---:|---:|
| QuickSort (`hoare-med3`) | 38.235 | 442.948 |
| pdqsort `full` | 17.211 | 210.520 |
| pdqsort `noA` | 24.354 | 269.420 |
| pdqsort `noB` | 51.456 | 626.545 |
| pdqsort `noA+noB` | 57.376 | 730.871 |
| `Arrays.sort()` | 12.690 | 172.348 |

Đây là kết quả của workload và môi trường ghi trong CSV, không khẳng định thứ
hạng giống nhau trên từng dataset. Xem [báo cáo phương pháp và kết quả RQ3](docs/RQ3_Methods_and_Results_Report.md)
để biết comparison-count medians và phân tích chi tiết.

Chạy lại ma trận với một **tên file mới** (runner không ghi đè file đã tồn tại):
```bash
java -Xmx4g -cp target/classes edu.csd201.sort.Rq3Benchmark --output results/rq3_pdqsort_metrics_rerun.csv
```

**Giới hạn JDK:** CSV hiện tại được đo trên Java 17.0.12. Đề bài nêu
`Arrays.sort(int[])` của JDK 8 làm baseline, nhưng dự án đặt compiler release 17;
việc chấp nhận kết quả JDK 17 thay cho baseline JDK 8 chưa được xác nhận. Vì vậy
chưa thể khẳng định yêu cầu JDK 8 đã được đáp ứng.

---

### 📚 Tài Liệu & Báo Cáo Chi Tiết
- [Báo cáo Phương pháp & Kết quả RQ1 (Cặp 1)](docs/RQ1_Methods_and_Results_Report.md)
- [Báo cáo thực nghiệm RQ1 & Baseline RQ2/RQ3 QuickSort](docs/RQ1_QuickSort_Report.md)
- [Báo cáo kỹ thuật TV1 QuickSort & D1/D2](docs/TV1_QuickSort_D1_D2_Technical_Report.md)
- [Bảng tổng hợp kết quả RQ1 (`comparison_all_algorithms.csv`)](results/comparison_all_algorithms.csv)
- [Bảng tổng hợp kết quả RQ2 & RQ3 (`rq2_full_comparison.csv`)](results/rq2_full_comparison.csv)
