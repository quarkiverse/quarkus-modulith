package io.quarkiverse.modulith.events.neo4j.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * The hash of a serialized event that publications are looked up by, as in Spring Modulith: SHA-256, with MD5 as a
 * second candidate for publications written by older Spring Modulith versions.
 */
public final class EventHash {

    private static final HexFormat HEX = HexFormat.of();

    private EventHash() {
    }

    /**
     * @return the SHA-256 hash of the serialized event, as lower-case hexadecimal
     */
    public static String sha256(String serializedEvent) {
        return digest("SHA-256", serializedEvent);
    }

    /**
     * @return the MD5 hash of the serialized event, as older Spring Modulith versions wrote it, as lower-case
     *         hexadecimal
     */
    public static String md5(String serializedEvent) {
        return digest("MD5", serializedEvent);
    }

    /**
     * @return the hashes a stored publication of the serialized event may carry
     */
    public static List<String> candidates(String serializedEvent) {
        return List.of(sha256(serializedEvent), md5(serializedEvent));
    }

    static String digest(String algorithm, String serializedEvent) {
        try {
            return HEX.formatHex(MessageDigest.getInstance(algorithm)
                    .digest(serializedEvent.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("The JVM has no " + algorithm + " message digest", e);
        }
    }
}
