package edu.citysearch;

import edu.citysearch.experiment.InputGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputGeneratorTest {
    @Test void fixedSeedAndCaseIdentityReproduceTheExactInput() {
        InputGenerator a = new InputGenerator(InputGenerator.SEED);
        InputGenerator b = new InputGenerator(InputGenerator.SEED);
        assertEquals(a.randomString(100, "abcd"), b.randomString(100, "abcd"));
        var original = InputGenerator.edit("random", 64);
        InputGenerator.edit("random", 128); // Generation order does not influence the first case.
        assertEquals(original, InputGenerator.edit("random", 64));
        assertEquals(InputGenerator.fingerprint(original), InputGenerator.fingerprint(InputGenerator.edit("random", 64)));
        assertNotEquals(InputGenerator.fingerprint(original), InputGenerator.fingerprint(InputGenerator.edit("random", 128)));
    }

    @Test void specialInputsHaveTheirIntendedStructure() {
        assertEquals(new InputGenerator.Pair("aaaa", "bbbb"), InputGenerator.edit("disjoint", 4));
        assertEquals(new InputGenerator.Pair("aaaaaaaa", "aaab"), InputGenerator.search("late_mismatch", 8, 4));
        assertEquals(new InputGenerator.Pair("aaaaaaaa", "bbbb"), InputGenerator.search("early_mismatch", 8, 4));
    }
}
