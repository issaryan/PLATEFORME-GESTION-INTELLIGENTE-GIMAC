package cm.gimac.des.participant.infrastructure.client;

import cm.gimac.des.participant.application.port.out.StrongReauthVerifier;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut de la réauthentification forte (profils dev et test, risque IAM-07 du PI) : toute
 * assertion non vide est acceptée. Jamais sélectionné en production.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.reauth", havingValue = "stub")
public class StubStrongReauthVerifier implements StrongReauthVerifier {

    @Override
    public boolean verifier(String assertion, UUID sujet, String finalite) {
        return assertion != null && !assertion.isBlank();
    }
}
