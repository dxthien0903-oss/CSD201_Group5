# Phần của TV4 — Radix Sort, generator D5, runner, đếm thao tác (CSD201, Đề tài 1)

## 1. Các file
| File | Vai trò |
|---|---|
| `RadixSort.java` | Radix Sort LSD, cơ số 256, 4 lượt, counting sort ổn định, MỘT mảng phụ N. `sort()` để đo thời gian (không đếm); `sortCounting()` cho lượt `--count`. |
| `Datasets.java` | Generator D1–D5. **D5 là phần của TV4**; D1–D4 là bản tạm cho đến khi nhóm chốt generator chung. |
| `Worker.java` | Tiến trình con: một lượt đo trong một JVM riêng. (Bản core chính thức thuộc TV3; đây là bản mẫu cùng hợp đồng đầu ra.) |
| `Runner.java` | Tiến trình cha: `gen-plan`, `run`, `check`, `summary`, `selftest`. |
| `Main.java` | Điểm vào của jar (`worker ...` -> Worker, còn lại -> Runner). |
| `RadixTest.java` | Kiểm thử Radix + generator, ghi log, thoát mã 1 nếu lỗi. |
| `plan_radix.csv` | Danh sách 166 lượt chạy (sinh bằng `gen-plan`). |

## 2. Biên dịch
Đề yêu cầu **JDK 8**. Mã chỉ dùng API Java 8.
- NetBeans: Java Application, chép 6 file `.java` vào default package, Main class = `Main`, Clean and Build -> `dist/<ten>.jar`.
- Dòng lệnh: `javac -d out *.java`, rồi đóng jar có `Main-Class: Main`.

## 3. Quy trình đo chính thức (một máy, chạy tuần tự, không mở ứng dụng khác)
1. Commit mã nguồn (cột `git_commit` sẽ có hậu tố `-dirty` nếu còn thay đổi chưa commit).
2. `java -jar dist/Sorting.jar selftest`  — phải ra `SELFTEST: ĐẠT`.
3. `java RadixTest radix_test_log.txt`  — phải ra `KẾT QUẢ: ĐẠT` (nộp kèm log).
4. `java -jar dist/Sorting.jar gen-plan --plan bench/plan_radix.csv` (thêm `--big` nếu máy đủ RAM cho N = 5×10⁷).
5. `java -jar dist/Sorting.jar run --plan bench/plan_radix.csv --out results/raw/radix.csv --xmx 2g --machine-id M1`
6. `java -jar dist/Sorting.jar check --plan bench/plan_radix.csv --out results/raw/radix.csv --strict`
7. `java -jar dist/Sorting.jar summary --out results/raw/radix.csv --summary-out results/radix_summary.csv`

Nếu bị ngắt giữa chừng, chạy lại đúng lệnh ở bước 5: các `run_id` đã có được bỏ qua, dòng CSV bị cắt dở được loại tự động.
Chỉ chạy MỘT Runner cho mỗi file `--out`. Không dùng chung file CSV giữa nhiều máy.

Chạy lại một cấu hình riêng (dưới 60 giây):
`java -Xmx2g -jar dist/Sorting.jar worker --algo radix --variant base --dataset D5 --n 1000000 --k 10 --seed 1`
Thêm `--count` để chạy lượt đếm thao tác, `--timeout-sec S` để Worker tự canh thời gian sort.

Trên Windows, nếu tiếng Việt hiển thị `?`: `chcp 65001` rồi chạy `java -Dfile.encoding=UTF-8 -jar ...`.
Runner chạy Worker bằng đúng lệnh đo của quy định: `java -Xmx2g -jar ... worker ...`, **không thêm cờ JVM nào khác** (cột `jvm_flags` ghi lại `-Xmx2g`). Lệnh chạy lại một cấu hình ở trên dùng cùng cờ nên số bộ nhớ khớp với CSV.

## 4. Quy ước đo
- **Thời gian**: `elapsed_ms` chỉ đo quanh lệnh `sort`. Sinh dữ liệu, băm SHA-256, tạo oracle (`Arrays.sort`), so kết quả, tính `output_check` đều nằm ngoài vùng đo.
- **Làm nóng**: 3 lần trên 10⁵ phần tử, không ghi vào CSV. Ở N = 10⁵ thời gian rất ngắn nên dao động lớn; dùng trung vị của các lần lặp.
- **Timeout**: Runner gọi `waitFor(60 s)` rồi `destroyForcibly()` (đúng quy định). Giới hạn tính cho cả tiến trình, gồm khởi động JVM và sinh dữ liệu; với Radix phần chuẩn bị ≪ 60 s nên nhãn `timeout` vẫn đúng. Worker chỉ có một luồng, không có luồng giám sát.
- **Bộ nhớ phụ** = `heap_peak_bytes − heap_before_bytes` (tổng peak của các pool heap, sau `System.gc()` và `resetPeakUsage`). Mảng đầu vào và bản oracle đã nằm trong `heap_before`, nên với Radix kết quả ≈ 4N byte (một mảng `int[N]` phụ) cộng ~11 KB cố định.
  - **Không thêm `-Xms` lớn / cờ GC / TLAB.** Đã kiểm chứng: với đúng lệnh `java -Xmx2g` bộ nhớ phụ ≈ 4N ở mọi N (10⁵ … 2×10⁷, Serial và Parallel). Nếu ép heap khởi tạo ≥ 1 GB (ví dụ `-Xms2g`), mỗi luồng giữ một TLAB hàng chục MB, Eden tính cả vùng đó là "đã dùng" và `heap_peak − heap_before` ra **0** ở N nhỏ.
  - **Giới hạn độ phân giải (đã đo)**: heap khởi tạo mặc định của JVM bằng 1/64 RAM; TLAB của luồng tăng theo heap, và một mảng phụ nằm gọn trong TLAB thì mức dùng heap không tăng nên `heap_peak − heap_before` ra **0**. Ngưỡng N mà phép đo bắt đầu đúng (ParallelGC/SerialGC, `-Xmx2g`): heap khởi tạo 64–128 MB → đúng từ N ≈ 10⁵; 256 MB (máy ~16 GB RAM) → từ N ≈ 5×10⁵; 512 MB (~32 GB) → từ N ≈ 10⁶; ≥ 1 GB (≥ 64 GB RAM) → N = 10⁶ cũng sai. G1 (mặc định từ JDK 9) tệ hơn vì cấp phát theo vùng 1 MB.
  - **Hệ quả**: bộ nhớ phụ ở N nhỏ (đặc biệt N = 10⁵ của RQ1) có thể bị đo thấp tùy máy, với MỌI thuật toán, không riêng Radix. `selftest` bắt buộc phép đo đúng ở N ≥ 10⁶ (RQ2 dùng 10⁶ và 10⁷) và chỉ cảnh báo ở N nhỏ. Trong paper phải ghi rõ giới hạn này và không diễn giải bộ nhớ ở N nhỏ nếu `selftest` có cảnh báo.
  - Không tự thêm cờ JVM (`-Xms`, `-XX:-UseTLAB`, đổi GC) để "chữa": cờ khác nhau giữa các thuật toán làm bộ nhớ phụ không so sánh được. Nếu nhóm muốn đổi cách đo thì phải hỏi giảng viên và cả nhóm dùng chung.
- **Đếm thao tác của Radix** (`--count`): chỉ đếm đọc/ghi trên mảng khóa (`src`, `dst`); KHÔNG đếm mảng đếm `cnt[257]`. Mỗi lượt đọc 2N (histogram + rải), ghi N; 4 lượt => **8N đọc, 4N ghi, 12N thao tác** (N ≥ 2; với N < 2 là 0), không phụ thuộc dữ liệu. Lượt `--count` để trống `elapsed_ms`.
- **Trạng thái**: `ok`, `wrong` (kết quả khác `Arrays.sort`), `timeout`, `oom`, `crash` (JVM chết không in kết quả, hoặc tham số sai).
- **Lặp lại**: cùng `seed` thì cùng dữ liệu (cùng `input_sha256`); các lần `repeat` khác nhau của một seed chạy trên dữ liệu giống hệt.

## 5. Các cột CSV
`run_id, rq, algorithm, variant, dataset, n, params, seed, input_sha256, generator_version, repeat, elapsed_ms, heap_before_bytes, heap_peak_bytes, ops_count, output_check, status, xmx, machine_id, git_commit, array_reads, array_writes, max_depth, k, jdk, os, cpu_cores, ram_mb, jvm_flags`

- `ops_count = array_reads + array_writes` (chỉ ở lượt `--count`). `max_depth` để trống với Radix (không đệ quy).
- `output_check`: `h = 31*h + a[i]` (long, cho phép tràn, h khởi tạo 0) trên mảng đã sắp.
- `k`: số khóa phân biệt của D5 (trống với D1–D4). `jdk, os, cpu_cores, ram_mb` do chính Worker báo, nên ghi đúng JVM đã chạy.
- Các cột thêm cuối (`k` … `jvm_flags`) không làm đổi vị trí các cột cũ.

## 6. Giới hạn đã biết — cần thống nhất với nhóm
1. D1–D4 trong `Datasets.java` là bản tạm; dùng `input_sha256` để đối chiếu với generator của TV1/TV2.
2. Worker chính thức thuộc TV3: cần thống nhất ba loại dòng `ENV,...`, `SHA,...`, `RESULT,...` và cách truyền `--variant`.
3. `heap_peak` là tổng peak của từng pool nên có thể lớn hơn peak đồng thời thực; đây là cách của đoạn mã mẫu và áp dụng như nhau cho mọi thuật toán.
4. D1 chạy trùng ở RQ1 và RQ2 (cùng N = 10⁶, 10⁷); hai nhóm lượt được gắn `rq` khác nhau và có thể gộp khi phân tích.
5. Khóa phải thuộc [0, 2³¹−1]; khóa âm làm Radix sai (Worker báo `wrong`).
6. Số seed, số lần lặp, timeout, cách làm nóng và schema CSV phải đối chiếu với "Quy định chung" của đề.
