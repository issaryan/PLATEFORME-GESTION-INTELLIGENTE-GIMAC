package cm.gimac.des.notification.infrastructure.client;

import cm.gimac.des.notification.application.port.out.DestinatairesSandboxPort;
import cm.gimac.des.notification.domain.exception.NotificationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Destinataires de la sandbox de substitution (développement, tests) : institution fictive 99001 (administrateur, chef de
 * projet, testeur) et administrateurs du GIMAC de démonstration ; d'autres peuvent être enregistrés par les tests.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.annuaire", havingValue = "stub", matchIfMissing = true)
public class DestinatairesSandboxStubAdapter implements DestinatairesSandboxPort {

    public static final String ADMIN_99001 = "00000000-0000-4000-8000-000000099a01";
    public static final String CHEF_99001 = "00000000-0000-4000-8000-000000099c01";
    public static final String TESTEUR_99001 = "00000000-0000-4000-8000-000000099e01";
    public static final String ADMIN_GIMAC = "00000000-0000-4000-8000-0000000a0001";
    public static final String RSSI = "00000000-0000-4000-8000-0000000a0002";

    private final Map<String, Map<String, Set<String>>> institutions = new ConcurrentHashMap<>();
    private final Map<String, List<String>> porteurs = new ConcurrentHashMap<>();
    private volatile boolean indisponible;

    public DestinatairesSandboxStubAdapter() {
        reinitialiser();
    }

    public final void reinitialiser() {
        institutions.clear();
        porteurs.clear();
        institutions.put(
            "99001",
            Map.of(ADMIN_99001, Set.of("ROLE_SBX_ADMIN_INSTITUTION"), CHEF_99001, Set.of("ROLE_SBX_CHEF_PROJET"), TESTEUR_99001, Set.of("ROLE_SBX_TESTEUR"))
        );
        porteurs.put("SBX_HAB_ADMINISTRATION", List.of(ADMIN_GIMAC));
        porteurs.put("SBX_LEC_ADMINISTRATION", List.of(RSSI, ADMIN_GIMAC));
        indisponible = false;
    }

    /** Simule MS-IAM injoignable (tests de reprise). */
    public void rendreIndisponible(boolean indisponible) {
        this.indisponible = indisponible;
    }

    @Override
    public Map<String, Set<String>> comptesInstitution(String codeParticipant) {
        disponible();
        return codeParticipant == null ? Map.of() : institutions.getOrDefault(codeParticipant.strip(), Map.of());
    }

    @Override
    public List<String> porteurs(String permission) {
        disponible();
        return porteurs.getOrDefault(permission, List.of());
    }

    private void disponible() {
        if (indisponible) {
            throw NotificationException.indisponible("NOT_ANNUAIRE_INDISPONIBLE", "Annuaire des comptes indisponible");
        }
    }
}
