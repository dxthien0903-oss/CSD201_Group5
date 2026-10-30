# CSD201 Group 5 — Sorting Algorithms & pdqsort Benchmark

> **Đề tài 1 (CSD201 - FA26):** Nghiên cứu, cài đặt và đánh giá thực nghiệm so sánh hiệu năng giữa **Quick Sort**, **Merge Sort**, **Heap Sort**, **Radix Sort** và **pdqsort** (Pattern-defeating Quicksort) trên các phân phối dữ liệu chuẩn $D_1 - D_5$.

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Platform](https://img.shields.io/badge/Platform-Cross--Platform-blue)]()

---

## 1. Thành viên nhóm & Phân công nhiệm vụ

| TV | Vai trò / Cặp RQ | Thuật toán sở hữu | Trách nhiệm dữ liệu & hạ tầng | Đầu ra chính |
| :---: | :---: | :--- | :--- | :--- |
| **TV1** | Cặp 1 — Chủ trì RQ1 | **Quick Sort** (Hoare + median-of-three) | Generator $D_1$ (Random), $D_2$ (Sorted) | `QuickSort.java`, test suite 46k permutations, CSV RQ1 |
| **TV2** | Cặp 1 — Chủ trì RQ1 | **Merge Sort** (Top-down, 1 temp array) | Generator $D_3$ (Nearly sorted), $D_4$ (Reverse) | `MergeSort.java`, `DataGenerator.java`, biểu đồ RQ1 |
| **TV3** | Cặp 2 — Chủ trì RQ2 | **Heap Sort** (Max-heap in-place) | Core Benchmark runner, `sort_subsegment` | `HeapSort.java`, framework đo bộ nhớ / thời gian |
| **TV4** | Cặp 2 — Chủ trì RQ2 | **Radix Sort** (LSD base 256, 4 passes) | Generator $D_5$ (Many duplicates: $K=1000, 10, 2$) | `RadixSort.java`, đếm $8N$ đọc / $4N$ ghi, CSV RQ2 |
| **TV5** | Cặp 3 — Chủ trì RQ3 | **pdqsort** (full & `noA`) | Tích hợp HeapSort fallback, randomized test | `PdqSort.java`, `SortMetrics.java`, phân tích cơ chế A |
| **TV6** | Cặp 3 — Chủ trì RQ3 | **pdqsort** (`noB`) & CLI | Thư viện baseline `Arrays.sort`, killer adversary | `ExperimentRunner.java`, dữ liệu đối kháng, phân tích cơ chế B |

---

## 2. Cấu trúc thư mục chuẩn mực (Maven Standard Layout)

Repository được tổ chức theo chuẩn quốc tế của Apache Maven, tập trung toàn bộ mã nguồn sản phẩm trong `src/main/java` và mã kiểm thử trong `src/test/java`, đồng thời bảo toàn các tài liệu và hồ sơ audit độc lập của từng thành viên:

```text
CSD201_Group5/
├── pom.xml                                  # Cấu hình dự án Maven chuẩn (Java 17, UTF-8)
├── README.md                                # Hướng dẫn dự án và tổng quan kiến trúc
├── .gitignore                               # Quy tắc loại trừ tệp nhị phân và artifact cục bộ
│
├── src/                                     # MÃ NGUỒN CHÍNH THỨC CỦA DỰ ÁN
│   ├── main/java/edu/csd201/sort/
│   │   ├── QuickSort.java                   # [TV1] Hoare partition, median-of-three pivot
│   │   ├── MergeSort.java                   # [TV2] Top-down merge sort, 1 temp array O(N)
│   │   ├── HeapSort.java                    # [TV3] Max-heap in-place, bottom-up build
│   │   ├── RadixSort.java                   # [TV4] LSD base 256, 4 passes cho int 32-bit
│   │   ├── PdqSort.java                     # [TV5/6] Pattern-defeating quicksort, cơ chế A/B/fallback
│   │   ├── SortMetrics.java                 # Thống nhất metrics: comparisons, assignments, reads, writes, maxDepth
│   │   ├── DataGenerator.java               # Bộ sinh dữ liệu chính thức D1–D5
│   │   ├── Benchmark.java                   # Runner chính đo thời gian và metrics cho cả 5 thuật toán
│   │   └── ExperimentRunner.java            # Runner kiểm thử ngẫu nhiên và đo đạc RQ3
│   └── test/java/edu/csd201/sort/
│       ├── QuickSortTest.java               # [TV1] 42 ca kiểm thử toàn diện + 46.233 hoán vị
│       ├── KillerAdversaryTest.java         # [TV1] 266 ca kiểm thử dữ liệu đối kháng & trường hợp xấu
│       ├── RadixSortTest.java               # [TV4] 1.043 ca kiểm thử giá trị biên và cơ số
│       └── Datasets.java                    # Bộ fixture kiểm thử độc lập
│
├── docs/                                    # TÀI LIỆU DỰ ÁN & BÁO CÁO NGHIÊN CỨU
│   ├── 261014-de-tai-1-sorting-pdqsort-csd201-fa26.docx  # Đề tài chính thức từ giảng viên
│   ├── Phan_cong_chuan_theo_de_Sorting_pdqsort.xlsx      # Bảng phân công ma trận công việc 6 TV
│   └── RQ1_Methods_and_Results_Report.md                 # Báo cáo phương pháp & kết quả thực nghiệm RQ1
│
├── results/                                 # KẾT QUẢ THỰC NGHIỆM CHÍNH THỨC
│   ├── benchmark.csv                        # Kết quả benchmark toàn diện
│   ├── rq1_benchmark.csv                    # Kết quả RQ1 (Scalability trên D1)
│   ├── rq2_benchmark.csv                    # Kết quả RQ2 (Phân phối dữ liệu D1–D5)
│   ├── quick_sort_test_log.txt              # [TV1] Log xác nhận 42 nhóm test ĐẠT
│   └── charts/                              # Biểu đồ phân tích thời gian và bộ nhớ phụ
│
└── tv4/                                     # HỒ SƠ BÀN GIAO CỦA THÀNH VIÊN 4
    ├── README.md                            # Đặc tả kỹ thuật RadixSort TV4
    ├── bench/plan_radix.csv                 # Kế hoạch đo thực nghiệm RadixSort
    └── results/radix_test_log.txt           # Test log xác nhận 1.043 kiểm tra ĐẠT
```

---

## 3. Hướng dẫn Biên dịch & Chạy Kiểm thử (Build & Verification)

### Yêu cầu môi trường
- **JDK 17** (hoặc OpenJDK 17+)
- **Apache Maven 3.8+**
- Hệ điều hành: Windows, Linux hoặc macOS

### 3.1. Biên dịch toàn bộ dự án qua Maven
```powershell
mvn clean compile
```

### 3.2. Chạy kiểm thử tự động toàn bộ thuật toán

#### Kiểm thử QuickSort (TV1) — 42 nhóm ca kiểm thử + 46.233 hoán vị + Dữ liệu đối kháng:
```powershell
# 1. Chạy từ mã nguồn đã chuẩn hóa và xuất log kiểm thử:
javac -encoding UTF-8 -d target/classes src/main/java/edu/csd201/sort/*.java
javac -encoding UTF-8 -cp target/classes -d target/test-classes src/test/java/edu/csd201/sort/QuickSortTest.java
java -cp "target/classes;target/test-classes" edu.csd201.sort.QuickSortTest --log=results/quick_sort_test_log.txt

# 2. Chạy kiểm thử dữ liệu đối kháng (Killer Adversary - 266 ca kiểm thử):
javac -encoding UTF-8 -cp target/classes -d target/test-classes src/test/java/edu/csd201/sort/KillerAdversaryTest.java
java -cp "target/classes;target/test-classes" edu.csd201.sort.KillerAdversaryTest
```

#### Kiểm thử RadixSort (TV4) — 1.043 ca kiểm thử:
```powershell
javac -encoding UTF-8 -cp target/classes -d target/test-classes src/test/java/edu/csd201/sort/Datasets.java src/test/java/edu/csd201/sort/RadixSortTest.java
java -cp "target/classes;target/test-classes" edu.csd201.sort.RadixSortTest
```

#### Kiểm thử ngẫu nhiên pdqsort (TV5/TV6) với 1.000 ca mảng ngẫu nhiên:
```powershell
java -cp target/classes edu.csd201.sort.ExperimentRunner randomized-test 20261016 1000 results/randomized-test.log
```

---

## 4. Hướng dẫn Chạy Thực nghiệm Benchmark (RQ1, RQ2, RQ3)

Runner tập trung `edu.csd201.sort.Benchmark` hỗ trợ đo thời gian chạy thực tế (`timeMs`), số phép so sánh (`comparisons`), số phép gán (`assignments`), bộ nhớ phụ (`extraMemBytes`) và độ sâu đệ quy (`max_depth`):

```powershell
# 1. Chạy xác thực tính đúng đắn trước khi đo:
java -cp target/classes edu.csd201.sort.Benchmark verify-only

# 2. Chạy bài toán RQ1 (Scalability trên D1, N từ 100.000 đến 20.000.000):
java -Xmx4g -cp target/classes edu.csd201.sort.Benchmark rq1

# 3. Chạy bài toán RQ2 (Phân phối dữ liệu D1–D5 với các mức N=10^6, 10^7):
java -Xmx4g -cp target/classes edu.csd201.sort.Benchmark rq2

# 4. Chạy toàn bộ ma trận (Full benchmark):
java -Xmx6g -cp target/classes edu.csd201.sort.Benchmark full
```

Kết quả sẽ tự động lưu vào thư mục `results/`:
- `results/rq1_benchmark.csv`
- `results/rq2_benchmark.csv`
- `results/benchmark.csv`

---

## 5. Quy ước Dữ liệu & Đạo đức Nghiên cứu
- **Kiểu dữ liệu:** Mảng `int[]` nguyên thủy, các khóa không âm nằm trong $[0, 2^{31} - 1]$.
- **Không trộn chế độ:** Lượt đo thời gian (`time`) chạy riêng biệt với lượt đếm (`count`) để tránh overhead của bộ đếm làm nhiễu kết quả `System.nanoTime()`.
- **Tái lập (Reproducibility):** Mọi thí nghiệm được gắn `seed` cố định (`20261016`, `20261017`, `20261018`) ghi rõ trong metadata và CSV.
