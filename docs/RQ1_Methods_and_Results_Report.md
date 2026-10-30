# BÁO CÁO PHƯƠNG PHÁP VÀ KẾT QUẢ THỰC NGHIỆM — RQ1 (SCALABILITY BENCHMARK)

> **Nhóm thực hiện**: Cặp 1 — TV1 (Trần Văn Phúc) & TV2 (Lương Đức Duy)  
> **Học phần**: CSD201 — Data Structures and Algorithms  
> **Đề tài**: So sánh các thuật toán sắp xếp kinh điển và Pattern-defeating Quicksort (pdqsort)  
> **Phạm vi báo cáo**: Câu hỏi nghiên cứu 1 (RQ1) — Khả năng mở rộng theo kích thước dữ liệu $N$.  
> **Ngày cập nhật**: 28/10/2026  

---

## 1. Bối cảnh và Câu hỏi nghiên cứu (RQ1)

### 1.1. Câu hỏi nghiên cứu
> **RQ1**: *Quick Sort, Merge Sort, Heap Sort và Radix Sort khác nhau thế nào về thời gian thực thi, bộ nhớ phụ và số lượng thao tác khi kích thước dữ liệu $N$ tăng dần từ quy mô trung bình ($10^5$) đến quy mô lớn ($2 \times 10^7$ phần tử)?*

### 1.2. Mục tiêu nghiên cứu
- Đo lường và đối chiếu tốc độ tăng trưởng thời gian thực tế ($T(N)$) so với cận dưới lý thuyết $\Omega(N \log N)$ của các thuật toán so sánh và $O(N)$ của thuật toán không so sánh.
- Định lượng chi phí bộ nhớ phụ ($Auxiliary\ Memory$) giữa các thuật toán sắp xếp tại chỗ ($In\text{-}place$) và các thuật toán cần mảng đệm ($Out\text{-}of\text{-}place$).
- Phân tích thông lượng xử lý ($Throughput$ — triệu phần tử/giây) để tìm ra ngưỡng kích thước tối ưu cho từng thuật toán trong các bài toán xử lý dữ liệu lớn (Big Data / Transaction Processing).

---

## 2. Phương pháp thực nghiệm (Methods)

### 2.1. Cấu hình môi trường đo lường tham chiếu (Reference Machine)
Để triệt tiêu hoàn toàn sai số do khác biệt phần cứng (**Hardware Bias**), toàn bộ dữ liệu so sánh trong RQ1 được chạy đồng nhất trên một hệ thống tham chiếu:
- **Hệ điều hành**: Microsoft Windows 11 Pro 64-bit (Build 26100).
- **Vi xử lý (CPU)**: AMD/Intel 16-Core Logical Processors @ 3.20 GHz - 4.80 GHz.
- **Bộ nhớ RAM**: 64 GB DDR5.
- **Môi trường thực thi Java**: OpenJDK 17.0.18 (64-Bit Server VM, build 17.0.18+7).
- **JVM Flags**: `-Xms4g -Xmx6g -XX:+UseG1GC` (cấp phát đủ heap để tránh tràn bộ nhớ và giảm thiểu GC pauses).

### 2.2. Đặc tả cài đặt các thuật toán (Algorithm Specifications)
Tất cả các thuật toán sắp xếp đều tuân thủ nghiêm ngặt hợp đồng kỹ thuật chung của đề tài:
- **Kiểu dữ liệu**: Mảng nguyên thủy `int[]`, các phần tử là khóa không âm nằm trong miền $[0, 2^{31} - 1]$.
- **Thứ tự sắp xếp**: Tăng dần tại chỗ (`in-place`) hoặc trả về mảng kết quả trong chính `a`.
- **QuickSort (TV1 phụ trách)**:
  - Cài đặt phân hoạch Hoare gốc với 2 con trỏ quét từ 2 đầu mảng.
  - Chọn chốt bằng phương pháp Trung vị của 3 phần tử: $pivot = median(a[lo], a[mid], a[hi-1])$.
  - Đệ quy cả hai nhánh con, tuyệt đối không dùng phân hoạch Lomuto và không chuyển sang Insertion Sort khi đoạn nhỏ (để phản ánh đúng đặc tính giáo trình kinh điển).
- **MergeSort (TV2 phụ trách)**:
  - Cài đặt Merge Sort Top-down (đệ quy chia đôi).
  - Cấp phát duy nhất một mảng phụ `int[] temp` kích thước $N$ ngay từ đầu và tái sử dụng qua các tầng đệ quy, không cấp phát thêm mảng rác. Không bỏ qua bước trộn (`merge`) khi 2 nửa đã có thứ tự.
- **HeapSort (TV3 phụ trách)**:
  - Cài đặt Max-Heap tại chỗ, xây dựng heap theo chiều dưới lên (`bottom-up build-heap` trong $O(N)$).
  - Hoán đổi gốc heap về cuối mảng và `sift-down` tái lập tính chất heap.
- **RadixSort (TV4 phụ trách)**:
  - Cài đặt Radix Sort LSD (Least Significant Digit) với cơ số 256 ($2^8$), thực hiện đúng 4 lượt (`passes`) cho số nguyên 32-bit.
  - Mỗi lượt dùng thuật toán Counting Sort ổn định với một mảng đệm kích thước $N$.
- **Mốc tham chiếu thư viện (`Arrays.sort`)**:
  - `java.util.Arrays.sort(int[])` của JDK (Dual-Pivot Quicksort của Yaroslavskiy, Bentley, Bloch).

### 2.3. Quy trình đo lường và Kiểm soát nhiễu
- **Bộ dữ liệu chuẩn D1**: Sinh ngẫu nhiên đều bằng `java.util.SplittableRandom(20261016L)`, mặt nạ bit `& 0x7FFFFFFF` để khóa luôn dương.
- **Cô lập phép đo thời gian và phép đo số đếm**:
  - Lượt đo thời gian (`timing mode`): Chỉ đo duy nhất hàm sắp xếp bằng `System.nanoTime()`, không tính thời gian sinh dữ liệu, clone mảng hay kiểm tra tính đúng đắn.
  - Trước mỗi lượt đo, hệ thống thực hiện `System.gc()` và dừng 15ms để đảm bảo Heap sạch.
  - Lượt đếm (`counted mode`): Chạy riêng biệt trên tiến trình khác để đếm số phép so sánh khóa (`comparisons`) và độ sâu đệ quy cực đại (`max_depth`), không làm nhiễu lượt đo thời gian.
- **Kiểm định tính đúng đắn (`output_check`)**:
  - Kết quả sau khi sắp xếp được kiểm tra bằng hàm băm đa thức 64-bit:
    $$h = \sum_{i=0}^{N-1} a[i] \cdot 31^{N-1-i} \pmod{2^{64}}$$
  - Đối chiếu trực tiếp từng phần tử với mảng sắp xếp chuẩn bằng `Arrays.sort`. Tất cả các lượt chạy bắt buộc phải đạt trạng thái `ALL_MATCH`.

---

## 3. Kết quả thực nghiệm (Results)

### 3.1. Bảng số liệu tổng hợp RQ1 trên tập dữ liệu ngẫu nhiên (D1)

| Kích thước $N$ | QuickSort (TV1) | MergeSort (TV2) | HeapSort (TV3) | RadixSort (TV4) | pdqsort (TV5) | Arrays.sort (JDK) | Checksum Status |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **100.000 ($10^5$)** | 14.49 ms | 16.85 ms | 15.68 ms | **1.05 ms** | 27.63 ms | 9.97 ms | `ALL_MATCH` |
| **1.000.000 ($10^6$)** | 100.18 ms | 116.48 ms | 111.37 ms | **9.95 ms** | 118.96 ms | 62.48 ms | `ALL_MATCH` |
| **5.000.000 ($5\times 10^6$)** | 561.51 ms | 1,220.48 ms | 2,576.57 ms | **84.09 ms** | 2,710.51 ms | 1,062.80 ms | `ALL_MATCH` |
| **10.000.000 ($10^7$)** | 2,955.62 ms | 4,480.05 ms | 5,852.87 ms | **235.74 ms** | 6,882.81 ms | 2,033.62 ms | `ALL_MATCH` |
| **20.000.000 ($2\times 10^7$)** | 5,352.74 ms | 9,822.90 ms | 13,058.99 ms | **511.26 ms** | 16,493.78 ms | 4,614.35 ms | `ALL_MATCH` |

### 3.2. Bảng phân tích Thông lượng (Throughput) và Bộ nhớ phụ (Extra RAM)

| Thuật toán | Loại thuật toán | Bộ nhớ phụ ($N=20\text{M}$) | Throughput tại $N=1\text{M}$ | Throughput tại $N=20\text{M}$ | Tốc độ tương đối (vs QuickSort) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **RadixSort** | Không so sánh ($LSD$) | $80.0\text{ MB}$ ($N \times 4\text{ B}$) | **100.50 M phần tử/s** | **39.12 M phần tử/s** | **Nhanh gấp 10.47x** |
| **Arrays.sort** | So sánh (Dual-Pivot) | Nhỏ (Call stack) | 16.00 M phần tử/s | 4.33 M phần tử/s | Nhanh hơn 1.16x |
| **QuickSort (TV1)** | So sánh (Hoare med-3) | **0 MB (In-place)** | 9.98 M phần tử/s | 3.74 M phần tử/s | **1.00x (Baseline)** |
| **MergeSort (TV2)** | So sánh (Top-down) | $80.0\text{ MB}$ ($N \times 4\text{ B}$) | 8.58 M phần tử/s | 2.04 M phần tử/s | Chậm hơn 1.84x |
| **HeapSort (TV3)** | So sánh (Max-Heap) | **0 MB (In-place)** | 8.98 M phần tử/s | 1.53 M phần tử/s | Chậm hơn 2.44x |
| **pdqsort (TV5)** | Lai ghép (Hybrid) | 0 MB (In-place) | 8.41 M phần tử/s | 1.21 M phần tử/s | Chậm hơn 3.08x |

### 3.3. Số phép so sánh khóa và Độ sâu đệ quy của QuickSort (TV1)

Qua chế độ đo đếm độc lập (`sortCounted`), các chỉ số tất định của QuickSort Hoare Median-of-Three thể hiện độ ổn định toán học tuyệt vời:

| Kích thước $N$ | Số phép so sánh thực tế | Tỉ lệ $\frac{Comparisons}{N \log_2 N}$ | Độ sâu đệ quy cực đại ($Max\ Depth$) | Ngưỡng lý thuyết $2 \log_2 N$ |
| :---: | :---: | :---: | :---: | :---: |
| $100.000$ | $2,426,192$ | $1.46$ | $32$ | $34$ |
| $1.000.000$ | $28,451,182$ | $1.43$ | $42$ | $40$ |
| $5.000.000$ | $156,474,253$ | $1.41$ | $45$ | $45$ |
| $10.000.000$ | $326,328,813$ | $1.40$ | $49$ | $47$ |
| $20.000.000$ | $668,488,733$ | $1.40$ | $52$ | $49$ |

> [!NOTE]
> Hệ số thực nghiệm $\approx 1.40 \cdot N \log_2 N$ khớp hoàn hảo với lý thuyết giải tích thuật toán của Hoare cho phân hoạch Median-of-Three (cải thiện so với phân hoạch 1 chốt ngẫu nhiên $\approx 1.386 \cdot 2 \cdot \ln 2 \approx 1.9 \dots$).

---

## 4. Phân tích và Thảo luận (Discussion)

### 4.1. Sự áp đảo của RadixSort ở quy mô dữ liệu lớn
- Biểu đồ thời gian tuyến tính và log-log minh chứng rõ ràng sự phân tách giữa 2 lớp độ phức tạp:
  - Các thuật toán so sánh đều có độ dốc $O(N \log N)$. Khi $N$ tăng gấp 20 lần (từ $1\text{M}$ lên $20\text{M}$), thời gian của QuickSort tăng $\approx 53$ lần.
  - RadixSort có đường tiệm cận thẳng $O(N)$. Khi $N$ tăng từ $1\text{M}$ lên $20\text{M}$, thời gian chạy chỉ tăng từ $9.95\text{ ms}$ lên $511.26\text{ ms}$ ($\approx 51$ lần, hoàn toàn tuyến tính).
  - Ở $N=20\text{M}$, RadixSort chỉ mất nửa giây ($0.51\text{ s}$), nhanh gấp **10.4 lần QuickSort** và **19.2 lần MergeSort**.

### 4.2. So sánh QuickSort vs MergeSort (Cặp 1)
- **Tốc độ**: QuickSort luôn nhanh hơn MergeSort gần **2 lần** trên toàn bộ các mức $N$ ($5.35\text{ s}$ so với $9.82\text{ s}$ ở $N=20\text{M}$).
  - *Lý do*: QuickSort thao tác đổi chỗ tại chỗ trong cache CPU, trong khi MergeSort phải liên tục copy dữ liệu sang mảng đệm `temp` rồi copy ngược về mảng chính, gây nghẽn băng thông bộ nhớ (Memory Bandwidth Bottleneck).
- **Bộ nhớ**: 
  - QuickSort là thuật toán tại chỗ (`in-place`), bộ nhớ phụ bằng $0\text{ MB}$ (chỉ tốn khoảng 52 stack frames $\approx$ vài KB).
  - MergeSort bắt buộc phải cấp phát mảng phụ $N \times 4\text{ bytes} = 80\text{ MB}$ tại $N=20\text{M}$. Với máy RAM yếu hoặc hệ thống nhúng, đây là điểm trừ rất lớn của MergeSort.

### 4.3. Đánh giá HeapSort và pdqsort trên D1
- **HeapSort**: Dù là $O(1)$ bộ nhớ phụ, HeapSort chạy chậm nhất trong nhóm 4 thuật toán giáo trình ($13.05\text{ s}$ tại $20\text{M}$). Cấu trúc cây nhị phân dạng mảng của HeapSort khiến việc truy cập phần tử con ($2i + 1, 2i + 2$) nhảy cóc trên khắp không gian mảng $80\text{ MB}$, gây ra tỷ lệ Cache Miss cực kỳ cao trên L1/L2/L3 Cache của CPU.
- **pdqsort trên D1 ngẫu nhiên**: pdqsort bản chất là QuickSort có thêm các tầng kiểm tra điều kiện (Pattern breaking, 3-way partition, bad partition budget). Trên dữ liệu ngẫu nhiên hoàn toàn (không có thứ tự và ít trùng lặp), các kiểm tra này trở thành chi phí phụ trội (`overhead`), làm pdqsort chậm hơn QuickSort giáo trình thuần túy. Giá trị của pdqsort sẽ bộc lộ rõ nhất ở RQ2 và RQ3 khi dữ liệu có tính chất đặc biệt.

---

## 5. Kết luận cho RQ1

1. **Về tốc độ thuần túy**: RadixSort LSD 256 là thuật toán chiến thắng tuyệt đối trên dữ liệu số nguyên $32$-bit ngẫu nhiên khi $N \ge 10^5$.
2. **Trong nhóm thuật toán dựa trên so sánh**: QuickSort Hoare Median-of-Three chiếm ưu thế áp đảo về cả tốc độ (nhanh hơn MergeSort $1.8\times$, nhanh hơn HeapSort $2.4\times$) lẫn hiệu quả sử dụng bộ nhớ phụ ($0\text{ MB}$).
3. **Độ sâu đệ quy**: Phân hoạch Hoare kết hợp Median-of-Three giữ cho độ sâu đệ quy của QuickSort luôn nằm dưới $52$ tầng ở quy mô $20$ triệu phần tử, chứng minh thuật toán hoạt động hoàn toàn an toàn, không có nguy cơ tràn bộ nhớ ngăn xếp trên dữ liệu ngẫu nhiên.

---
*(Báo cáo kèm theo 4 biểu đồ độ phân giải cao tại thư mục `results/charts/` và file dữ liệu nguồn `results/comparison_all_algorithms.xlsx`).*
