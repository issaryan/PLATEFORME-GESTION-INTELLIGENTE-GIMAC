package cm.gimac.des.notification.infrastructure.client;

import cm.gimac.des.notification.application.port.out.AnnuairePort;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution de l'annuaire (développement, tests) : quelques acteurs internes du GIMAC,
 * mêmes identifiants que les adaptateurs de substitution de MS-COLLABORATION.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.annuaire", havingValue = "stub", matchIfMissing = true)
public class AnnuaireStubAdapter implements AnnuairePort {

    public static final String CHEF_DES = "00000000-0000-4000-8000-00000000c001";
    public static final String CONDUCTEUR = "00000000-0000-4000-8000-00000000c002";
    public static final String INGENIEUR = "00000000-0000-4000-8000-00000000c003";
    public static final String RSSI = "00000000-0000-4000-8000-00000000c004";

    private final Map<String, String> noms = new ConcurrentHashMap<>();
    private volatile boolean indisponible;

    public AnnuaireStubAdapter() {
        reinitialiser();
    }

    public final void reinitialiser() {
        noms.clear();
        noms.put(CHEF_DES, "Paul Mbarga Atangana");
        noms.put(CONDUCTEUR, "Aminata Ngono");
        noms.put(INGENIEUR, "Serge Ndzana");
        noms.put(RSSI, "Clarisse Bilong");
        indisponible = false;
    }

    public void enregistrer(String id, String nom) {
        noms.put(id, nom);
    }

    /** Simule un annuaire injoignable (tests de dégradation). */
    public void rendreIndisponible(boolean indisponible) {
        this.indisponible = indisponible;
    }

    @Override
    public Optional<String> nomAffiche(String compteId) {
        if (indisponible) {
            throw cm.gimac.des.notification.domain.exception.NotificationException.indisponible("NOT_ANNUAIRE_INDISPONIBLE", "Annuaire des collaborateurs indisponible");
        }
        return Optional.ofNullable(compteId == null ? null : noms.get(compteId));
    }
}
