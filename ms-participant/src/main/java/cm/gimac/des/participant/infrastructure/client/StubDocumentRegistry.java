package cm.gimac.des.participant.infrastructure.client;

import cm.gimac.des.participant.application.port.out.DocumentRegistry;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut du registre documentaire (profils dev et test, PI §0.8) : toute référence est réputée
 * conforme. Jamais sélectionné en production.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.document-registry", havingValue = "stub")
public class StubDocumentRegistry implements DocumentRegistry {

    @Override
    public Verification verifier(UUID documentId, String sha256) {
        return Verification.CONFORME;
    }
}
