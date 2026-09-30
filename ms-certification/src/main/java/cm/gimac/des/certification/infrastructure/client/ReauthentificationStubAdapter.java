package cm.gimac.des.certification.infrastructure.client;

import cm.gimac.des.certification.application.port.out.ReauthentificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut de la réauthentification forte (profils dev et test, risque IAM-07 du PI) : toute assertion
 * non vide est acceptée, sauf {@value #ASSERTION_REFUSEE} qui simule un refus de MS-IAM. Jamais en production.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.reauthentication", havingValue = "stub", matchIfMissing = true)
public class ReauthentificationStubAdapter implements ReauthentificationPort {

    public static final String ASSERTION_REFUSEE = "assertion-refusee";

    @Override
    public boolean verifier(String assertion, String sujet, String finalite) {
        return assertion != null && !assertion.isBlank() && !ASSERTION_REFUSEE.equals(assertion);
    }
}
