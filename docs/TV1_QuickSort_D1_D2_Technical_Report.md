# BÁO CÁO KỸ THUẬT TV1 — QUICK SORT (HOARE + MEDIAN-OF-THREE), GENERATOR D1/D2 & KIỂM THỬ ĐỐI KHÁNG

> **Thành viên thực hiện**: TV1 — Trần Văn Phúc (Cặp 1 — Chủ trì RQ1)  
> **Học phần**: CSD201 — Cấu trúc dữ liệu và Giải thuật  
> **Phạm vi**: Thiết kế và cài đặt `QuickSort`, bộ sinh dữ liệu `D1`/`D2`, bộ kiểm thử `QuickSortTest` & `KillerAdversaryTest`, tích hợp đo lường RQ1 & RQ2/RQ3 baseline.

---

## 1. Thiết kế và Cài đặt `QuickSort`

### 1.1. Tuân thủ quy ước thuật toán giáo trình (Bảng 2 của đề)
Lớp [`QuickSort`](../src/main/java/edu/csd201/sort/QuickSort.java) cài đặt thuật toán Quick Sort giáo trình với các đặc tả bắt buộc:
- **Kiểu dữ liệu**: Mảng nguyên thủy `int[]`, khóa không âm trong miền $[0, 2^{31} - 1]$.
- **Chọn chốt (Pivot Selection)**: Trung vị của 3 phần tử đầu, giữa và cuối đoạn đang xét `[lo, hi)`:
  $$\text{mid} = \text{lo} + \lfloor (\text{hi} - \text{lo}) / 2 \rfloor, \quad \text{pivot} = \text{median}(a[\text{lo}], a[\text{mid}], a[\text{hi} - 1])$$
- **Phân hoạch Hoare (Hoare Partition)**:
  - Hai con trỏ `i` (khởi tạo `lo - 1`) và `j` (khởi tạo `hi`) quét ngược chiều nhau bằng vòng lặp `do ... while`.
  - Dừng ngay khi gặp phần tử bằng chốt (`a[i] >= pivot` và `a[j] <= pivot`), sau đó hoán đổi `a[i]` và `a[j]` nếu `i < j`.
  - Việc dừng cả hai con trỏ tại các khóa bằng `pivot` giúp chia đôi cân bằng ngay cả trên mảng toàn phần tử bằng nhau ($D_5$ với $K=1, 2$), giữ độ sâu đệ quy ở mức $\approx \log_2 N$ thay vì suy biến $O(N^2)$.
- **Đệ quy hai phía thuần túy**:
  - Sau khi phân hoạch trả về điểm cắt `p`, thuật toán gọi đệ quy trên cả hai nửa `quickSort(a, lo, p, c)` và `quickSort(a, p, hi, c)`.
  - Không dùng phân hoạch Lomuto, không chuyển sang Insertion Sort ở đoạn nhỏ.

### 1.2. Tách biệt chế độ Đo thời gian và Đếm thao tác (Two-Mode Execution)
- **Timing Mode (`QuickSort.sort(int[] a)` và `QuickSort.sort(int[] a, int lo, int hi)`)**:
  - Truyền `Counter = null`, không tạo đối tượng đếm và không cộng biến đếm trong vòng lặp phân hoạch nội tại để JIT Compiler tối ưu hóa tối đa khi đo `elapsed_ms` và `heap_peak_bytes`.
- **Counted Mode (`QuickSort.sortCounted(int[] a)` và `QuickSort.sortCounted(int[] a, int lo, int hi)`)**:
  - Chạy độc lập ngoài vùng đo thời gian, trả về `QuickSort.Stats` gồm tổng số phép so sánh khóa (`comparisons`) và độ sâu đệ quy cực đại (`maxDepth`).

---

## 2. Bộ sinh dữ liệu chuẩn `D1` và `D2` (`DataGenerator`)

Trong [`DataGenerator`](../src/main/java/edu/csd201/sort/DataGenerator.java), TV1 xây dựng bộ sinh dữ liệu chuẩn cho `D1` và `D2` và phối hợp chuẩn hóa `D1–D5` trong `DataGenerator.generateSpec()`:
- **`D1` (Ngẫu nhiên đều)**:
  - Sử dụng `java.util.SplittableRandom(seed)` kết hợp mặt nạ bit `rnd.nextInt() & 0x7FFFFFFF` để sinh $N$ số nguyên 32-bit không âm phân phối đều trên $[0, 2^{31} - 1]$.
  - Hoàn toàn tất định và tái lập chính xác cùng một mảng đầu vào trên mọi JVM khi dùng cùng `seed`.
- **`D2` (Tăng dần)**:
  - Tạo bản sao (`clone()`) từ mảng `D1` của cùng `seed` rồi sắp xếp tăng dần bằng `Arrays.sort()`, đảm bảo giữ nguyên 100% tập đa hợp giá trị (multiset) của `D1`.

---

## 3. Kiểm thử Độ đúng đắn và Dữ liệu Đối kháng

### 3.1. Bộ kiểm thử `QuickSortTest`
[`QuickSortTest`](../src/test/java/edu/csd201/sort/QuickSortTest.java) gồm 7 nhóm kiểm thử JUnit 5 (và CLI xuất [`results/quick_sort_test_log.txt`](../results/quick_sort_test_log.txt)):
1. **Ca cố định (Fixed Cases)**: Kiểm tra đầy đủ 5 ca cố định trong đề (`[7]`, `[2, 1]`, `[3, 3, 3]`, `[0, 2147483647, 0]`, `[5, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5]`) cùng các biên mảng rỗng, 1–3 phần tử cực trị.
2. **Hợp đồng đoạn con (`sort(a, lo, hi)`)**: Đảm bảo chỉ sắp xếp trong phạm vi `[lo, hi)` và giữ nguyên các phần tử ngoài biên.
3. **Mảng kích thước lớn ($N = 100.000$)**: Toàn bộ bằng nhau, tăng dần, giảm dần, và xen kẽ hai cực trị `0` / `Integer.MAX_VALUE`.
4. **Kiểm thử ngẫu nhiên (`Randomized Testing`)**:
   - 1.000 mảng ngẫu nhiên kích thước $N \in [0, 100]$ trên toàn dải khóa.
   - 200 mảng ngẫu nhiên miền giá trị hẹp $\{0, 1, 2, 3\}$.
   - 1.000 mảng ngẫu nhiên kích thước $N \in [0, 100]$ trải đều đủ 5 họ phân phối chuẩn `D1–D5` (`DataGenerator.generateSpec`), đối chiếu từng phần tử với `Arrays.sort`.
5. **Kiểm thử chế độ đếm (`Counted Mode`)**: Kiểm chứng `comparisons > 0` và độ sâu đệ quy `maxDepth <= 64` trên mảng $N = 100.000$ bằng nhau/tăng/giảm.
6. **Quét vét cạn (`Exhaustive Sweep`)**: Toàn bộ $3.280$ mảng độ dài $0..7$ trên $\{0, 1, 2\}$ và toàn bộ $46.233$ hoán vị độ dài $1..8$.
7. **Kiểm thử `DataGenerator` (`D1`/`D2`)**: Xác nhận tính tái lập theo `seed`, miền giá trị không âm và bất biến multiset giữa `D1` và `D2`.

### 3.2. Bộ kiểm thử đối kháng `KillerAdversaryTest` (Musser / McIlroy Killer)
[`KillerAdversaryTest`](../src/test/java/edu/csd201/sort/KillerAdversaryTest.java) đánh giá sức chịu đựng của `QuickSort` và đối sánh trực tiếp với `PdqSort`:
- **14 họ dữ liệu có cấu trúc & 250 mảng ngẫu nhiên ($N = 10.000$)**: `all-equal`, `ascending`, `descending`, `organ-pipe`, `sawtooth`, `median-of-three triples`, `blocks-of-8/32`. Nhờ phân hoạch Hoare dừng ở khóa bằng `pivot`, tất cả các mẫu tĩnh này đều được `QuickSort` xử lý cân bằng với `maxDepth <= 62`.
- **Dãy đối kháng Musser / McIlroy (`generateMed3Killer`) tại $N = 10^4, 10^5, 10^6$**:
  - Theo dõi vị trí hoán đổi của phân hoạch Hoare để đặt phần tử nhỏ nhất tại `lo` và phần tử nhỏ thứ hai tại `mid`, ép `medianOfThree` luôn chọn phần tử nhỏ thứ hai làm chốt ở mọi bước đệ quy.
  - Mỗi bước phân hoạch chỉ tách được đoạn con kích thước 2 (`[lo, lo+2)`), làm độ sâu đệ quy của `QuickSort` tăng tuyến tính $\approx N/2$ và số phép so sánh tăng bậc hai $\Theta(N^2)$:
    - Ở $N = 2.000$: `QuickSort` vẫn hoàn tất đúng nhưng `maxDepth = 1.000` và `comparisons = 1.002.997` ($\approx N^2 / 4$).
    - Ở $N = 10^4, 10^5, 10^6$: `QuickSort` giáo trình vượt giới hạn ngăn xếp của JVM và rơi vào trạng thái `crash` (`StackOverflowError`), trong khi `PdqSort` phát hiện phân hoạch lệch, kích hoạt `HeapSort` fallback và hoàn tất trong thời gian $O(N \log N)$ với `maxDepth <= 42`.

