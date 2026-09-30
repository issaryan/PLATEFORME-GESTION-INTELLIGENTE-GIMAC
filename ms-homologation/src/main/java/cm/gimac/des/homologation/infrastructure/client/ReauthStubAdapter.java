package cm.gimac.des.homologation.infrastructure.client;

import cm.gimac.des.homologation.application.port.out.StrongReauthPort;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut de la réauthentification forte (profils dev et test, risque IAM-07 du PI) : toute assertion
 * non vide est acceptée, sauf {@value #ASSERTION_REFUSEE} qui simule un refus de MS-IAM. Jamais
 * sélectionné en production.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.reauthentication", havingValue = "stub", matchIfMissing = true)
public class ReauthStubAdapter implements StrongReauthPort {

    public static final String ASSERTION_REFUSEE = "assertion-refusee";

    @Override
    public boolean verifier(String assertion, UUID sujet, String finalite) {
        return assertion != null && !assertion.isBlank() && !ASSERTION_REFUSEE.equals(assertion);
    }
}
