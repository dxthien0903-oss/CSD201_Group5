package edu.csd201.sort;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class DataGeneratorTest {
    @Test
    void d2AndD4AreDerivedFromTheSameD1Data() {
        int size = 2_048;
        long seed = 20261016L;
        int[] expectedD2 = DataGenerator.generate("D1", size, new Random(seed));
        Arrays.sort(expectedD2);

        assertArrayEquals(expectedD2,
                DataGenerator.generate("D2", size, new Random(seed)));

        int[] expectedD4 = expectedD2.clone();
        for (int low = 0, high = size - 1; low < high; low++, high--) {
            int value = expectedD4[low];
            expectedD4[low] = expectedD4[high];
            expectedD4[high] = value;
        }
        assertArrayEquals(expectedD4,
                DataGenerator.generate("D4", size, new Random(seed)));
    }

    @Test
    void d3UsesFloorNOver1000DistinctPositions() {
        long seed = 17L;
        int smallSize = 999;
        assertArrayEquals(
                DataGenerator.generate("D2", smallSize, new Random(seed)),
                DataGenerator.generate("D3", smallSize, new Random(seed)));

        int size = 3_000;
        Random random = new Random(seed);
        int[] expected = DataGenerator.generate("D1", size, random);
        Arrays.sort(expected);
        for (int swap = 0; swap < size / 1000; swap++) {
            int first = random.nextInt(size);
            int second = random.nextInt(size - 1);
            if (second >= first) {
                second++;
            }
            int value = expected[first];
            expected[first] = expected[second];
            expected[second] = value;
        }

        int[] actual = DataGenerator.generate("D3", size, new Random(seed));
        assertArrayEquals(expected, actual);
        assertArrayEquals(actual, DataGenerator.generate("D3", size, new Random(seed)));
    }

    @Test
    void specificationDatasetsAreReproducibleForTheSameSeed() {
        int size = 10_000;
        long seed = 20261016L;
        for (String dataset : new String[]{"D1", "D2", "D3", "D4"}) {
            assertArrayEquals(
                    DataGenerator.generateSpec(dataset, size, 0, seed),
                    DataGenerator.generateSpec(dataset, size, 0, seed),
                    dataset + " must be reproducible for a fixed seed");
        }
        assertArrayEquals(
                DataGenerator.generateSpec("D5", size, 10, seed),
                DataGenerator.generateSpec("D5", size, 10, seed),
                "D5 must be reproducible for a fixed seed");
    }
}
