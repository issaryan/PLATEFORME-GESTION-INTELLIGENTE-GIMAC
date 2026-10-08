package cm.gimac.des.notification.infrastructure.client;

import cm.gimac.des.notification.application.port.out.ComptesInstitutionPort;
import cm.gimac.des.notification.domain.exception.NotificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution des comptes d'institution (développement, tests) : une institution de démonstration et ses
 * deux correspondants ; d'autres peuvent être enregistrées par les tests.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.annuaire", havingValue = "stub", matchIfMissing = true)
public class ComptesInstitutionStubAdapter implements ComptesInstitutionPort {

    public static final String INSTITUTION_A = "00000000-0000-4000-8000-0000000000a1";
    public static final String CORRESPONDANT_A1 = "00000000-0000-4000-8000-00000000e0a1";
    public static final String CORRESPONDANT_A2 = "00000000-0000-4000-8000-00000000e0a2";

    private final Map<String, List<String>> comptes = new ConcurrentHashMap<>();
    private volatile boolean indisponible;

    public ComptesInstitutionStubAdapter() {
        reinitialiser();
    }

    public final void reinitialiser() {
        comptes.clear();
        comptes.put(INSTITUTION_A, List.of(CORRESPONDANT_A1, CORRESPONDANT_A2));
        indisponible = false;
    }

    public void enregistrer(String institutionId, List<String> ids) {
        comptes.put(institutionId, List.copyOf(ids));
    }

    /** Simule MS-IAM injoignable (tests de reprise). */
    public void rendreIndisponible(boolean indisponible) {
        this.indisponible = indisponible;
    }

    @Override
    public List<String> comptes(String institutionId) {
        if (indisponible) {
            throw NotificationException.indisponible("NOT_ANNUAIRE_INDISPONIBLE", "Comptes de l'institution indisponibles");
        }
        return institutionId == null ? List.of() : comptes.getOrDefault(institutionId, List.of());
    }
}
