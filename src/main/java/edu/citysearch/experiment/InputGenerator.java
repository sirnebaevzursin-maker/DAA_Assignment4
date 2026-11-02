package edu.citysearch.experiment;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** A specified xorshift64* PRNG, independent of JDK Random implementation details. */
public final class InputGenerator {
    public static final long SEED = 20261103L;
    private long state;
    public record Pair(String first, String second) {}

    public InputGenerator(long seed) { state = seed == 0 ? 0x9E3779B97F4A7C15L : seed; }

    private long nextLong() {
        state ^= state >>> 12; state ^= state << 25; state ^= state >>> 27;
        return state * 2685821657736338717L;
    }

    public String randomString(int length, String alphabet) {
        if (length < 0 || alphabet.isEmpty()) throw new IllegalArgumentException("Invalid generator arguments");
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            value.append(alphabet.charAt((int) Long.remainderUnsigned(nextLong(), alphabet.length())));
        return value.toString();
    }

    private static InputGenerator forCase(String family, String type, int n, int m) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : (family + ":" + type).getBytes(StandardCharsets.UTF_8)) {
            hash ^= b & 255L; hash *= 0x100000001b3L;
        }
        return new InputGenerator(SEED ^ hash ^ ((long) n << 32) ^ m);
    }

    public static Pair edit(String type, int n) {
        InputGenerator generator = forCase("edit", type, n, n);
        return switch (type) {
            case "random" -> new Pair(generator.randomString(n, "abcd"), generator.randomString(n, "abcd"));
            case "disjoint" -> new Pair("a".repeat(n), "b".repeat(n));
            default -> throw new IllegalArgumentException("Unknown ED input: " + type);
        };
    }

    public static Pair search(String type, int n, int m) {
        if (n < 0 || m <= 0) throw new IllegalArgumentException("Invalid search lengths");
        InputGenerator generator = forCase("search", type, n, m);
        return switch (type) {
            case "random" -> new Pair(generator.randomString(n, "abcd"), generator.randomString(m, "abcd"));
            case "late_mismatch" -> new Pair("a".repeat(n), "a".repeat(m - 1) + "b");
            case "early_mismatch" -> new Pair("a".repeat(n), "b".repeat(m));
            default -> throw new IllegalArgumentException("Unknown search input: " + type);
        };
    }

    public static String fingerprint(Pair pair) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : new String[] {pair.first(), pair.second()}) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
