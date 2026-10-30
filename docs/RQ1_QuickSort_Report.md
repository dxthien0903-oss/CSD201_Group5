# BÁO CÁO THỰC NGHIỆM RQ1 & BASELINE RQ2/RQ3 — QUICK SORT (TV1)

> **Thành viên thực hiện**: TV1 — Trần Văn Phúc (Cặp 1 — Điều phối RQ1)  
> **Báo cáo tổng hợp Cặp 1**: Xem thêm [`docs/RQ1_Methods_and_Results_Report.md`](RQ1_Methods_and_Results_Report.md)  
> **Dữ liệu CSV**: [`results/rq1_benchmark.csv`](../results/rq1_benchmark.csv), [`results/rq2_benchmark.csv`](../results/rq2_benchmark.csv), [`results/quick_sort_results.csv`](../results/quick_sort_results.csv)

---

## 1. Kết quả thực nghiệm RQ1 của QuickSort trên dữ liệu D1

Trên phân phối chuẩn `D1` (số nguyên 32-bit không âm phân phối đều bằng `SplittableRandom`), kết quả đo thời gian, bộ nhớ phụ, số phép so sánh và độ sâu đệ quy của `QuickSort` (`hoare-med3`) qua các mốc kích thước $N = 10^5 \to 5 \times 10^7$:

| Kích thước $N$ | Thời gian trung bình (ms) | Số phép so sánh (`comparisons`) | Tỷ lệ $\frac{\text{Comparisons}}{N \log_2 N}$ | Độ sâu đệ quy (`max_depth`) | Bộ nhớ phụ (`extraMemBytes`) | Trạng thái |
|---:|---:|---:|---:|---:|---:|:---:|
| `100,000` | 10.51 | 2,426,192 | 1.46 | 32 | 0 B | `OK` |
| `1,000,000` | 97.42 | 28,451,182 | 1.43 | 42 | 0 B | `OK` |
| `5,000,000` | 538.16 | 156,474,253 | 1.41 | 45 | 0 B | `OK` |
| `10,000,000` | 1,142.85 | 326,328,813 | 1.40 | 49 | 0 B | `OK` |
| `20,000,000` | 2,386.40 | 668,488,733 | 1.40 | 52 | 0 B | `OK` |
| `50,000,000` | 6,215.18 | 1,755,073,021 | 1.37 | 54 | 0 B | `OK` |

### Nhận xét thực nghiệm RQ1:
1. **Tốc độ tăng trưởng tiệm cận $\Theta(N \log N)$**: Tỷ lệ $\text{Comparisons} / (N \log_2 N)$ hội tụ ổn định quanh hằng số lý thuyết $\approx 1.37 - 1.40$ của phân hoạch Hoare kết hợp chọn chốt Median-of-Three.
2. **Độ sâu đệ quy an toàn**: Trên dữ liệu ngẫu nhiên `D1`, `max_depth` chỉ tăng từ `32` ($N=10^5$) lên `54` ($N=5\times 10^7$), xấp xỉ $2 \log_2 N$, sử dụng không đáng kể bộ nhớ ngăn xếp và $0\text{ B}$ bộ nhớ Heap phụ.
3. **Đối sánh trong RQ1**: `QuickSort` nhanh hơn `MergeSort` khoảng $1.8\times - 2.1\times$ và nhanh hơn `HeapSort` khoảng $2.4\times - 4.0\times$ ở quy mô lớn nhờ tính cục bộ bộ nhớ đệm (cache locality) vượt trội và không tốn băng thông sao chép mảng phụ $4N$ byte. `RadixSort` (LSD base 256) dẫn đầu tốc độ trên khóa nguyên 32-bit nhờ độ phức tạp tuyến tính $O(N)$ nhưng phải đánh đổi $4N$ byte mảng đệm.

---

## 2. Kết quả Baseline của QuickSort cho RQ2 & RQ3 ($N = 10^6$ và $N = 10^7$)

Bảng tổng hợp các chỉ số của `QuickSort` trên toàn bộ các cấu hình dữ liệu của RQ2/RQ3 (`D1`–`D4` và `D5` với $K \in \{1000, 10, 2\}$):

| Phân phối dữ liệu | $N = 10^6$ Thời gian (ms) | $N = 10^6$ Comparisons | $N = 10^6$ Max Depth | $N = 10^7$ Thời gian (ms) | $N = 10^7$ Comparisons | $N = 10^7$ Max Depth |
|---|---:|---:|---:|---:|---:|---:|
| **D1 (Ngẫu nhiên đều)** | 97.42 | 28,451,182 | 42 | 1,142.85 | 326,328,813 | 49 |
| **D2 (Tăng dần sẵn)** | 30.25 | 23,427,447 | 22 | 187.60 | 267,445,697 | 25 |
| **D3 (Gần tăng dần)** | 36.84 | 25,118,920 | 31 | 392.15 | 289,614,308 | 36 |
| **D4 (Giảm dần)** | 31.12 | 23,427,449 | 22 | 194.80 | 267,445,699 | 25 |
| **D5 ($K = 1000$)** | 71.15 | 26,842,110 | 38 | 812.40 | 308,119,450 | 44 |
| **D5 ($K = 10$)** | 39.97 | 22,915,408 | 28 | 448.60 | 261,840,112 | 31 |
| **D5 ($K = 2$)** | 28.27 | 21,694,812 | 22 | 312.50 | 250,112,640 | 25 |

### Phân tích đặc tính của QuickSort trên RQ2 & vai trò Baseline cho RQ3:
- **Trên dữ liệu có thứ tự (`D2`, `D4`)**: Chốt `medianOfThree(a[lo], a[mid], a[hi-1])` chọn trúng phần tử chính giữa, chia mảng thành hai nửa bằng nhau tuyệt đối (`max_depth = 22` ở $N=10^6$ và `25` ở $N=10^7$). Tuy nhiên `QuickSort` vẫn phải thực hiện đủ $\approx N \log_2 N$ phép so sánh, trong khi `PdqSort` (nhờ **Cơ chế A** — phát hiện đoạn đã sắp và dừng sớm bằng Partial Insertion Sort) giảm thời gian trên `D2`/`D4` xuống gần như tuyến tính $O(N)$.
- **Trên dữ liệu nhiều khóa trùng (`D5` với $K = 10, 2$)**: Phân hoạch Hoare 2 chiều dừng và đổi chỗ ở các phần tử bằng chốt nên không bị suy biến bậc hai (giữ `max_depth` từ `22` đến `31`), nhưng các phần tử trùng khóa vẫn bị phân hoạch lại qua $\log_2 N$ tầng đệ quy. Khi so với `PdqSort` có **Cơ chế B** (gom khóa trùng sang bên trái bằng `partitionLeft` / 3-way partition và loại bỏ ngay khỏi đệ quy), `PdqSort` giảm mạnh số phép so sánh khi $K$ rất nhỏ.

