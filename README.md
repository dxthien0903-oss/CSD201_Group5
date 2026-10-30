# CSD201 Group 5 — Nghiên Cứu Thực Nghiệm Các Thuật Toán Sắp Xếp & PDQSort

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8%2B-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.10.2-25A162?style=for-the-badge&logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Benchmark](https://img.shields.io/badge/Benchmark-RQ1%20%7C%20RQ2%20%7C%20RQ4-0A66C2?style=for-the-badge)](results/)

Dự án nghiên cứu thực nghiệm, cài đặt từ đầu và đánh giá hiệu năng các thuật toán sắp xếp nền tảng (**Quick Sort**, **Merge Sort**, **Heap Sort**, **Radix Sort LSD**) cùng thuật toán lai hiện đại **Pattern-Defeating Quicksort (PDQSort)** trên mảng số nguyên 32-bit không âm trong khoảng `[0, 2^31 - 1]`.

---

## 👥 Phân Công Nhóm (Group 5)

| Thành viên | Họ và tên | Hạng mục phụ trách chính |
|:---:|---|---|
| **TV1** | **Võ Hồng Phúc** | Cài đặt `QuickSort` (Hoare + Median-of-Three), sinh dữ liệu chuẩn `D1`/`D2`, kiểm thử `QuickSortTest` & `KillerAdversaryTest`, điều phối thực nghiệm RQ1 |
| **TV2** | **Lương Đức Duy** | Cài đặt `MergeSort`, sinh dữ liệu `D3`, điều phối `Benchmark` tổng hợp cho RQ1 & RQ2 |
| **TV3** | **Trần Minh Quân** | Cài đặt `HeapSort`, sinh dữ liệu `D4`, phân tích giới hạn bộ nhớ tại chỗ $O(1)$ |
| **TV4** | **Đặng Xuân Thiện** | Cài đặt `RadixSort` (LSD base 256), sinh dữ liệu `D5`, xây dựng `MeasureRunner` / `MeasureWorker` cô lập tiến trình JVM |
| **TV5** | **Nguyễn Quốc Bảo** | Cài đặt `PdqSort` (Pattern-Defeating Quicksort), thực nghiệm RQ4 đối sánh với QuickSort và HeapSort |
| **TV6** | **Lê Hoàng Khang** | Kiểm chứng độc lập độ đúng đắn, tổng hợp biểu đồ thực nghiệm và biên찬 báo cáo học thuật |

---

## 🗺️ Cấu Trúc Dự Án

```text
CSD201_Group5/
├── pom.xml
├── README.md
├── docs/
│   ├── Phan_cong_chuan_theo_de_Sorting_pdqsort.xlsx
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
| `QuickSort` | `src/main/java/edu/csd201/sort/QuickSort.java` | Quick Sort phân hoạch Hoare kết hợp chọn chốt Median-of-Three và khử đệ quy đuôi |
| `MergeSort` | `src/main/java/edu/csd201/sort/MergeSort.java` | Merge Sort chia để trị ổn định sử dụng mảng phụ $O(N)$ |
| `HeapSort` | `src/main/java/edu/csd201/sort/HeapSort.java` | Heap Sort vun đống nhị phân tại chỗ với bộ nhớ phụ $O(1)$ |
| `RadixSort` | `src/main/java/edu/csd201/sort/RadixSort.java` | LSD Radix Sort cơ số 256 (8-bit mỗi lượt, 4 lượt chia lô) |
| `PdqSort` | `src/main/java/edu/csd201/sort/PdqSort.java` | Pattern-Defeating Quicksort kết hợp Insertion Sort, 3-way partition và fallback HeapSort |
| `DataGenerator` | `src/main/java/edu/csd201/sort/DataGenerator.java` | Bộ sinh 5 phân phối dữ liệu chuẩn `D1`–`D5` tái lập hoàn toàn theo `seed` |
| `Benchmark` | `src/main/java/edu/csd201/sort/Benchmark.java` | Trình chạy thực nghiệm RQ1, RQ2 và xuất bảng kết quả tổng hợp |
| `MeasureRunner` | `src/main/java/edu/csd201/sort/MeasureRunner.java` | Điều phối đo lường chuẩn đề bài bằng tiến trình JVM con độc lập cho mỗi lượt chạy |
| `ExperimentRunner` | `src/main/java/edu/csd201/sort/ExperimentRunner.java` | Trình chạy thực nghiệm chuyên biệt cho `QuickSort` xuất `results/quick_sort_results.csv` |

---

## ✨ Thiết Kế Thuật Toán & Phương Pháp Đo Lường

### 1. Năm phân phối dữ liệu chuẩn (`D1` – `D5`)
- **D1 (Uniform Random):** $N$ số nguyên 32-bit không âm phân phối đều trên $[0, 2^{31}-1]$, sinh bằng `SplittableRandom(seed)` cùng phép mặt nạ bit `& 0x7FFFFFFF`.
- **D2 (Sorted Ascending):** Bản sao của `D1` đã sắp xếp không giảm.
- **D3 (Nearly Sorted):** Bản sao của `D2` thực hiện hoán đổi ngẫu nhiên $\lfloor N / 1000 \rfloor$ cặp vị trí.
- **D4 (Reverse Sorted):** Bản sao của `D2` đảo ngược thứ tự giảm dần.
- **D5 (Many Duplicates):** Sinh $K$ khóa phân biệt từ `seed`, mỗi phần tử chọn ngẫu nhiên đều trong $K$ khóa ($K \in \{1000, 10, 2, 1\}$).

### 2. Nguyên tắc tách biệt chế độ đo (Two-Mode Measurement)
- **Timing Mode (`sort`):** Thực thi thuật toán thuần túy để đo `time_ms` và `peak_aux_bytes`, không chèn biến đếm tránh làm sai lệch tối ưu hóa JIT của JVM.
- **Counted Mode (`sortCounted`):** Thực thi trên bản sao dữ liệu độc lập để thu thập chính xác số phép so sánh khóa (`comparisons`), số phép gán/di chuyển (`moves`) và độ sâu đệ quy cực đại (`max_depth`).

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
java -Xmx4g -cp target/classes edu.csd201.sort.Benchmark --rq1 --rq2
```

### 3. Chạy thực nghiệm chi tiết cho riêng QuickSort (`results/quick_sort_results.csv`)
```bash
java -Xmx4g -cp target/classes edu.csd201.sort.ExperimentRunner
```

### 4. Chạy bộ đo cô lập từng JVM riêng (`MeasureRunner`)
```bash
java -cp target/classes edu.csd201.sort.MeasureRunner --rq=RQ1 --algo=QuickSort --out=results/rq1_quicksort_jvm.csv
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
java -cp "target/classes;target/test-classes" edu.csd201.sort.QuickSortTest
```

```bash
java -cp "target/classes;target/test-classes" edu.csd201.sort.KillerAdversaryTest
```

### Tóm tắt độ phủ kiểm thử
- **`QuickSortTest` (7 nhóm JUnit 5):** Kiểm thử mảng biên, hợp đồng sắp xếp đoạn con `sort(a, lo, hi)`, mảng kích thước lớn $N=100.000$, đối chiếu `Arrays.sort` trên 1.200 mảng ngẫu nhiên, kiểm tra độ sâu đệ quy `maxDepth <= 64`, vét cạn toàn bộ $3.280$ mảng tam phân độ dài $0..7$ cùng $46.233$ hoán vị độ dài $1..8$, và kiểm tra tính tái lập của `DataGenerator` (`D1`/`D2`).
- **`KillerAdversaryTest` (4 nhóm JUnit 5):** Kiểm thử sức chịu đựng của `QuickSort` trước 14 họ dữ liệu đối kháng (sawtooth, organ-pipe, median-of-three killer triples), 250 mảng ngẫu nhiên và thuật toán leo đồi (hill-climbing adversary).
- **`RadixTest`:** Kiểm thử tính đúng đắn của `RadixSort` trên các phân phối dữ liệu.

---

## 📊 Tóm Tắt Kết Quả Thực Nghiệm Tiêu Biểu (RQ1 — Phân phối D1)

Số liệu trích xuất từ [`results/rq1_benchmark.csv`](results/rq1_benchmark.csv) (trung vị thời gian chạy tính bằng mili-giây trên phân phối ngẫu nhiên `D1`):

| Kích thước $N$ | QuickSort (ms) | MergeSort (ms) | HeapSort (ms) | RadixSort (ms) |
|---:|---:|---:|---:|---:|
| `100,000` | 9.09 | 12.16 | 12.40 | 2.66 |
| `1,000,000` | 81.91 | 126.98 | 246.30 | 10.67 |
| `5,000,000` | 435.56 | 656.83 | 1,794.96 | 48.03 |
| `10,000,000` | 901.79 | 1,353.30 | 4,060.69 | 123.70 |
| `20,000,000` | 1,860.45 | 2,789.84 | 9,151.31 | 190.47 |
| `50,000,000` | 4,904.14 | 7,330.72 | 25,849.68 | 512.01 |

Các báo cáo phân tích chuyên sâu được lưu trữ tại:
- [Báo cáo thực nghiệm RQ1 QuickSort](docs/RQ1_QuickSort_Report.md)
- [Báo cáo kỹ thuật TV1 QuickSort & D1/D2](docs/TV1_QuickSort_D1_D2_Technical_Report.md)
