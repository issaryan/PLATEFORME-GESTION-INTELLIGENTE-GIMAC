package cm.gimac.des.certification.infrastructure.client;

import cm.gimac.des.certification.application.port.out.CachetPlateformePort;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut du cachet de la plateforme (développement, tests) : le coffre de clés et l'autorité
 * d'horodatage RFC 3161 ne sont pas encore livrés. Signature HMAC-SHA256 par une clé de substitution et
 * horodatage par l'horloge du service ; l'autorité se déclare « SUBSTITUTION », jamais en production.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.key-vault-signing", havingValue = "stub", matchIfMissing = true)
public class CachetPlateformeStubAdapter implements CachetPlateformePort {

    static final String AUTORITE = "SUBSTITUTION : cachet et horodatage locaux (coffre et TSA non livrés)";

    private final byte[] cle;
    private final Clock clock;
    private final SecureRandom aleas = new SecureRandom();

    public CachetPlateformeStubAdapter(@Value("${gimac.certification.cachet.cle-substitution:cle-de-substitution-du-poste}") String cle, Clock clock) {
        this.cle = cle.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    @Override
    public Cachet sceller(String empreinte) {
        byte[] n = new byte[16];
        aleas.nextBytes(n);
        String nonce = HexFormat.of().formatHex(n);
        Instant horodatage = clock.instant();
        String signature = hmac(empreinte);
        String jeton = Base64.getEncoder().encodeToString(
            (empreinte + "|" + nonce + "|" + horodatage + "|" + hmac(empreinte + "|" + nonce + "|" + horodatage)).getBytes(StandardCharsets.UTF_8)
        );
        return new Cachet(signature, jeton, nonce, horodatage, AUTORITE);
    }

    private String hmac(String texte) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(cle, "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(texte.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponible", e);
        }
    }
}
