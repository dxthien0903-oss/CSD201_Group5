package edu.csd201.sort;

/**
 * Heap Sort chuẩn (Max-Heap, bottom-up build, tại chỗ In-Place)
 * Phục vụ đo đạc benchmark và kiểm thử cho đồ án CSD201.
 */
public final class HeapSort {

    private HeapSort() {
    }

    public static void sort(int[] a) {
        if (a == null || a.length < 2) {
            return;
        }
        sort(a, 0, a.length);
    }

    public static void sort(int[] values, int low, int high) {
        int size = high - low;
        for (int root = size / 2 - 1; root >= 0; root--) {
            siftDown(values, low, root, size);
        }
        for (int end = size - 1; end > 0; end--) {
            swap(values, low, low + end);
            siftDown(values, low, 0, end);
        }
    }

    private static void siftDown(int[] values, int offset, int root, int size) {
        while (root * 2 + 1 < size) {
            int child = root * 2 + 1;
            if (child + 1 < size && values[offset + child] < values[offset + child + 1]) {
                child++;
            }
            if (values[offset + root] >= values[offset + child]) {
                return;
            }
            swap(values, offset + root, offset + child);
            root = child;
        }
    }

    private static void swap(int[] values, int first, int second) {
        int tmp = values[first];
        values[first] = values[second];
        values[second] = tmp;
    }
}
