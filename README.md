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
│   └── TV1_QuickSort_D1_D2_Technical_Report.md
├── results/
│   ├── benchmark.csv
│   ├── quick_sort_results.csv
│   ├── rq1_benchmark.csv
│   └── rq2_benchmark.csv
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
    │   └── RadixSort.java
    └── test/java/edu/csd201/sort/
        ├── KillerAdversaryTest.java
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
| `DataGenerator` | `src/main/java/edu/csd201/sort/DataGenerator.java` | Bộ sinh 5 phân phối dữ liệu chuẩn `D1`–`D5` tái lập hoàn toàn theo `seed` |
| `Benchmark` | `src/main/java/edu/csd201/sort/Benchmark.java` | Trình chạy thực nghiệm RQ1, RQ2 và xuất bảng kết quả tổng hợp |
| `MeasureRunner` | `src/main/java/edu/csd201/sort/MeasureRunner.java` | Điều phối đo lường chuẩn đề bài bằng tiến trình JVM con độc lập cho mỗi lượt chạy |
| `ExperimentRunner` | `src/main/java/edu/csd201/sort/ExperimentRunner.java` | Trình chạy kiểm thử ngẫu nhiên (`D1`–`D5`) và đếm chỉ số (`--count`) cho `PdqSort` |

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

### Tóm tắt độ phủ kiểm thử
- **`QuickSortTest` (7 nhóm JUnit 5):** Kiểm thử 5 ca cố định của đề và biên cực trị, hợp đồng sắp xếp đoạn con `sort(a, lo, hi)`, mảng kích thước lớn $N=100.000$ (bằng nhau/tăng/giảm/cực trị), đối chiếu `Arrays.sort` trên 1.000 mảng ngẫu nhiên toàn dải + 200 mảng trùng khóa + 1.000 mảng ngẫu nhiên trải đều đủ 5 họ `D1`–`D5`, kiểm tra độ sâu đệ quy `maxDepth <= 64`, vét cạn toàn bộ $3.280$ mảng tam phân độ dài $0..7$ cùng $46.233$ hoán vị độ dài $1..8$, và kiểm tra tính tái lập của `DataGenerator` (`D1`/`D2`).
- **`KillerAdversaryTest` (5 nhóm JUnit 5):** Kiểm thử sức chịu đựng của `QuickSort` trước 14 họ dữ liệu có cấu trúc (sawtooth, organ-pipe, median-of-three triples), 250 mảng ngẫu nhiên, thuật toán leo đồi (hill-climbing adversary), và dãy đối kháng **Musser / McIlroy Median-of-Three Killer** tại $N = 10^4, 10^5, 10^6$ đối sánh trực tiếp giữa `QuickSort` và `PdqSort`.
- **`RadixTest`:** Kiểm thử tính đúng đắn của `RadixSort` trên các phân phối dữ liệu.

---

## 📊 Tóm Tắt Kết Quả Thực Nghiệm Tiêu Biểu (RQ1 — Phân phối D1)

Số liệu đối sánh tổng hợp trên phân phối ngẫu nhiên `D1` (trích từ [`results/comparison_all_algorithms.csv`](results/comparison_all_algorithms.csv) và [`docs/RQ1_Methods_and_Results_Report.md`](docs/RQ1_Methods_and_Results_Report.md)):

| Kích thước $N$ | QuickSort (ms) | MergeSort (ms) | HeapSort (ms) | RadixSort (ms) | PdqSort (ms) | Arrays.sort (ms) |
|---:|---:|---:|---:|---:|---:|---:|
| `100,000` | 14.49 | 16.85 | 15.68 | **1.05** | 27.63 | 9.97 |
| `1,000,000` | 100.18 | 116.48 | 111.37 | **9.95** | 118.96 | 62.48 |
| `5,000,000` | 561.51 | 1,220.48 | 2,576.57 | **84.09** | 2,710.51 | 1,062.80 |
| `10,000,000` | 2,955.62 | 4,480.05 | 5,852.87 | **235.74** | 6,882.81 | 2,033.62 |
| `20,000,000` | 5,352.74 | 9,822.90 | 13,058.99 | **511.26** | 16,493.78 | 4,614.35 |

Các báo cáo phân tích chuyên sâu được lưu trữ tại:
- [Báo cáo Phương pháp & Kết quả RQ1 (Cặp 1)](docs/RQ1_Methods_and_Results_Report.md)
- [Báo cáo thực nghiệm RQ1 & Baseline RQ2/RQ3 QuickSort](docs/RQ1_QuickSort_Report.md)
- [Báo cáo kỹ thuật TV1 QuickSort & D1/D2](docs/TV1_QuickSort_D1_D2_Technical_Report.md)
