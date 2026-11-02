package edu.citysearch;

import edu.citysearch.edit.*;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class EditDistanceTest {
    static List<EditDistance> algorithms() {
        return List.of(new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance());
    }
    static Stream<Arguments> references() {
        return algorithms().stream().flatMap(a -> Stream.of(
                Arguments.of(a, "kitten", "sitting", 3),
                Arguments.of(a, "flaw", "lawn", 2),
                Arguments.of(a, "intention", "execution", 5),
                Arguments.of(a, "", "abc", 3),
                Arguments.of(a, "abc", "abc", 0),
                Arguments.of(a, "abc", "", 3),
                Arguments.of(a, "", "", 0)));
    }
    @ParameterizedTest(name = "{1} -> {2} = {3}") @MethodSource("references")
    void referenceDistances(EditDistance algorithm, String a, String b, int expected) {
        assertEquals(expected, algorithm.distance(a, b), algorithm.name());
    }

    @TestFactory Stream<DynamicTest> oneHundredRandomShortPairsAgree() {
        Random random = new Random(20261103L);
        return IntStream.range(0, 100).mapToObj(i -> {
            String a = randomWord(random, random.nextInt(7));
            String b = randomWord(random, random.nextInt(7));
            return DynamicTest.dynamicTest("random ED pair " + i + " [" + a + "," + b + "]", () -> {
                int expected = new TableEditDistance().distance(a, b);
                for (EditDistance algorithm : algorithms()) {
                    assertEquals(expected, algorithm.distance(a, b), algorithm.name());
                    assertEquals(expected, algorithm.distance(b, a), "symmetry " + algorithm.name());
                }
            });
        });
    }

    static String randomWord(Random random, int length) {
        StringBuilder text = new StringBuilder();
        for (int j = 0; j < length; j++) text.append((char) ('a' + random.nextInt(3)));
        return text.toString();
    }

    @Test void countersMeasureWorkAndResetPerInvocation() {
        RecursiveEditDistance recursive = new RecursiveEditDistance();
        assertEquals(1, recursive.distance("a", "b"));
        assertEquals(4, recursive.measurements().recursiveCalls());
        assertEquals(1, recursive.measurements().comparisons());
        assertEquals(2, recursive.measurements().maxRecursionDepth());
        recursive.distance("", "");
        assertEquals(1, recursive.measurements().recursiveCalls());
        assertEquals(0, recursive.measurements().comparisons());
        for (EditDistance algorithm : List.of(new TableEditDistance(), new TwoRowEditDistance())) {
            algorithm.distance("ab", "abc");
            assertEquals(12, algorithm.measurements().dpCells());
            assertEquals(6, algorithm.measurements().comparisons());
            assertEquals(algorithm instanceof TableEditDistance ? 12 : 8,
                    algorithm.measurements().allocatedIntSlots());
            algorithm.distance("", "");
            assertEquals(1, algorithm.measurements().dpCells());
            assertEquals(0, algorithm.measurements().comparisons());
        }
    }
}
