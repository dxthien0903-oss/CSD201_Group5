# CSD201_Group5

## TV5 - Dao Cao Duy (QE190089)

### Da cai dat

- `PdqSort`: pdqsort cho `int[]`, insertion sort khi doan nho hon 24 phan tu, median-of-three/ninther, partition ba vung, pattern breaking va heapsort fallback.
- `noA`: cung thuat toan nhung tat partial-insertion check (co che A); cac co che khac duoc giu nguyen.
- Randomized test so sanh ca hai bien the voi `Arrays.sort`, gom ca bien, input cuc tri va mau doi khang kiem tra heapsort fallback; mac dinh 1.000 mang ngau nhien co kich thuoc 0-100 va ghi seed/log de tai lap.
- Dem do sau de quy, so partition va so lan heapsort fallback bang lenh `--count`.

### Chay

Can JDK 17 va Maven.

```powershell
mvn -q package
java -cp target/classes edu.csd201.sort.ExperimentRunner randomized-test
java -cp target/classes edu.csd201.sort.ExperimentRunner randomized-test 20261016 1000 results/randomized-test.log
java -cp target/classes edu.csd201.sort.ExperimentRunner --count 9 2 7 1 5 3
```

Log mac dinh duoc ghi vao `results/randomized-test.log`. D1-D5 hien la cac phan phoi tam thoi de kiem thu: ngau nhien, tang dan, giam dan, gan tang dan va nhieu gia tri trung lap. Chung chua duoc xac nhan la bo du lieu chinh thuc cua RQ2.

### Can bo sung de chot RQ3 va ban giao

- Thay D1-D5 tam thoi bang dinh nghia va input chinh thuc trong cau hinh RQ2.
- Chay tren cung input voi Quick, noB va `Arrays.sort`, roi xuat CSV co nhan variant ro rang. Repo hien chua co implementation noB/Quick hoac dataset RQ2, nen chua the tao ket qua so sanh nghien cuu trung thuc.
- Xac nhan voi TV3 API mong muon cho `sort_subsegment` truoc khi tich hop.
- Luu test log va CSV ket qua benchmark sau khi chay bo du lieu chinh thuc.

Kiem thu xac nhan hien tai: 1.000 ca voi seed `20261016`, khong co sai khac voi `Arrays.sort`.