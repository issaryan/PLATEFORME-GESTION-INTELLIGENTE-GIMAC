package cm.gimac.des.pilotage.infrastructure.client;

import cm.gimac.des.pilotage.application.port.out.ReauthentificationPort;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut de la réauthentification FIDO2 (développement, tests ; jamais en production) : toute assertion non
 * vide est acceptée, sauf {@value #ASSERTION_REFUSEE} qui simule un refus de MS-IAM ; les vérifications sont
 * retenues (sujet, finalité) pour les tests.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.reauthentification", havingValue = "stub", matchIfMissing = true)
public class ReauthentificationStubAdapter implements ReauthentificationPort {

    public static final String ASSERTION_REFUSEE = "assertion-refusee";

    /** Vérification demandée : sujet et finalité. */
    public record Verification(String sujet, String finalite, boolean valide) {}

    private final List<Verification> verifications = new CopyOnWriteArrayList<>();

    @Override
    public boolean verifier(String assertion, String sujet, String finalite) {
        boolean valide = assertion != null && !assertion.isBlank() && !ASSERTION_REFUSEE.equals(assertion) && sujet != null && !sujet.isBlank();
        verifications.add(new Verification(sujet, finalite, valide));
        return valide;
    }

    public List<Verification> verifications() {
        return List.copyOf(verifications);
    }

    public void effacer() {
        verifications.clear();
    }
}
