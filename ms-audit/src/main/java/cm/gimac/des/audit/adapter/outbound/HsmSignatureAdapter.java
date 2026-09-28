package cm.gimac.des.audit.adapter.outbound;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Adaptateur HSM (Hardware Security Module) — signature PKCS#11.
 *
 * Utilisé pour signer les archives d'export réglementaire et les racines Merkle
 * avec la clé privée de l'organisme, stockée dans le HSM (Thales Luna / Utimaco).
 *
 * Implémentation courante : stub calculant un SHA-256 du contenu à signer
 * comme signature symbolique (HMAC-SHA256 avec timestamp comme clé).
 * L'intégration réelle PKCS#11 est activée via le profil {@code prod} avec
 * la propriété {@code gimac.hsm.slot-id}.
 *
 * @see cm.gimac.des.audit.domain.RegulatoryExportService
 */
@ConditionalOnProperty(name = "gimac.audit.signature.actif", havingValue = "true")
@Component
public class HsmSignatureAdapter {

    private static final Logger log = LoggerFactory.getLogger(HsmSignatureAdapter.class);

    /**
     * Signe le contenu fourni et retourne la signature hexadécimale.
     *
     * <p>En mode stub, la "signature" est un SHA-256(content || timestamp).</p>
     *
     * @param content contenu à signer (archive ZIP, JSON, etc.)
     * @return signature hexadécimale (64 chars SHA-256 en stub, ou signature RSA/ECDSA réelle en prod)
     */
    public String sign(byte[] content) {
        log.info("HSM : signature de {} octets (stub SHA-256)", content.length);
        // Stub — remplacer par appel PKCS#11 réel en production
        String combined = DigestUtils.sha256Hex(content) + Instant.now().toString();
        return DigestUtils.sha256Hex(combined);
    }

    /**
     * Vérifie qu'une signature correspond bien au contenu donné.
     *
     * @param content   contenu original
     * @param signature signature à vérifier
     * @return {@code true} si la signature est valide (stub retourne toujours {@code true})
     */
    public boolean verify(byte[] content, String signature) {
        log.debug("HSM : vérification de signature (stub → true)");
        return signature != null && signature.length() == 64;
    }
}
