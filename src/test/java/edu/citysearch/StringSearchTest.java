package edu.citysearch;

import edu.citysearch.search.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

class StringSearchTest {
    static List<StringSearch> algorithms() { return List.of(new NaiveSearch(), new KmpSearch()); }
    static Stream<Arguments> referenceMatches() {
        return algorithms().stream().flatMap(a -> Stream.of(
                Arguments.of(a, "ABABDABACDABABCABAB", "ABABCABAB", List.of(10)),
                Arguments.of(a, "AAAAA", "AA", List.of(0, 1, 2, 3)),
                Arguments.of(a, "ABABABA", "ABA", List.of(0, 2, 4)),
                Arguments.of(a, "abc", "abc", List.of(0)),
                Arguments.of(a, "abc", "c", List.of(2)),
                Arguments.of(a, "abc", "z", List.of()),
                Arguments.of(a, "", "a", List.of()),
                Arguments.of(a, "ab", "abc", List.of())));
    }
    @ParameterizedTest @MethodSource("referenceMatches")
    void referenceSearch(StringSearch algorithm, String text, String pattern, List<Integer> expected) {
        assertEquals(expected, algorithm.search(text, pattern), algorithm.name());
    }

    @Test void emptyPatternThrowsInBothAlgorithms() {
        for (StringSearch algorithm : algorithms()) {
            assertThrows(IllegalArgumentException.class, () -> algorithm.search("abc", ""));
            assertThrows(IllegalArgumentException.class, () -> algorithm.search("", ""));
        }
    }

    @Test void referenceLpsArraysAndBorders() {
        KmpSearch kmp = new KmpSearch();
        assertArrayEquals(new int[]{0, 1, 2, 0, 1, 2, 3, 3}, kmp.lps("AAACAAAA"));
        assertArrayEquals(new int[]{0, 0, 1, 2, 0, 1, 2, 3, 4}, kmp.lps("ABABCABAB"));
        assertArrayEquals(new int[0], kmp.lps(""));
        assertArrayEquals(new int[]{0}, kmp.lps("A"));
        assertArrayEquals(new int[]{0, 1, 2, 3}, kmp.lps("AAAA"));
    }

    @TestFactory Stream<DynamicTest> oneHundredRandomTextsAgreeWithIndependentOracle() {
        Random random = new Random(314159L);
        return IntStream.range(0, 100).mapToObj(i -> {
            String text = EditDistanceTest.randomWord(random, random.nextInt(90));
            // Half the patterns are text substrings, to avoid testing only missing matches.
            String pattern;
            if (i % 2 == 0 && !text.isEmpty()) {
                int start = random.nextInt(text.length());
                pattern = text.substring(start, start + 1 + random.nextInt(text.length() - start));
            } else pattern = EditDistanceTest.randomWord(random, 1 + random.nextInt(12));
            return DynamicTest.dynamicTest("random search text " + i, () -> {
                List<Integer> expected = oracle(text, pattern);
                List<Integer> naive = new NaiveSearch().search(text, pattern);
                List<Integer> kmp = new KmpSearch().search(text, pattern);
                assertEquals(expected, naive); assertEquals(naive, kmp);
            });
        });
    }

    static List<Integer> oracle(String text, String pattern) {
        List<Integer> indices = new ArrayList<>();
        for (int start = text.indexOf(pattern); start >= 0; start = text.indexOf(pattern, start + 1))
            indices.add(start);
        return indices;
    }

    @Test void randomLpsAgreesWithBruteForceBorderDefinition() {
        Random random = new Random(271828L);
        KmpSearch kmp = new KmpSearch();
        for (int test = 0; test < 100; test++) {
            String p = EditDistanceTest.randomWord(random, random.nextInt(25));
            int[] expected = new int[p.length()];
            for (int end = 0; end < p.length(); end++)
                for (int length = 1; length <= end; length++)
                    if (p.substring(0, length).equals(p.substring(end + 1 - length, end + 1)))
                        expected[end] = length;
            assertArrayEquals(expected, kmp.lps(p), p);
        }
    }

    @Test void countersIncludeLpsResetAndRespectLinearSearchBound() {
        NaiveSearch naive = new NaiveSearch();
        naive.search("AAAAA", "AA");
        assertEquals(8, naive.measurements().comparisons());
        KmpSearch kmp = new KmpSearch();
        kmp.search("AAAAA", "AA");
        assertEquals(5, kmp.measurements().comparisons());
        assertEquals(1, kmp.measurements().lpsComparisons());
        String text = "a".repeat(10000), pattern = "a".repeat(499) + "b";
        assertTrue(kmp.search(text, pattern).isEmpty());
        assertTrue(kmp.measurements().comparisons() <= 2L * text.length());
        assertTrue(kmp.measurements().lpsComparisons() <= 2L * pattern.length());
        for (StringSearch algorithm : algorithms()) {
            algorithm.search("a", "longer");
            assertEquals(0, algorithm.measurements().totalComparisons());
        }
    }
}
