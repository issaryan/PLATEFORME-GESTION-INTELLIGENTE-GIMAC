package cm.gimac.des.audit.domain;

import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class JsonCanonicalizer {

    /**
     * Canonicalizes the given JSON string according to RFC 8785.
     *
     * @param json The input JSON string.
     * @return The canonicalized JSON string.
     */
    public String canonicalize(String json) {
        try {
            org.erdtman.jcs.JsonCanonicalizer jc = new org.erdtman.jcs.JsonCanonicalizer(json);
            return jc.getEncodedString();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON payload for canonicalization", e);
        }
    }
}
