package cm.gimac.des.certification.infrastructure.client;

import cm.gimac.des.certification.application.port.out.FicheChangementPort;
import cm.gimac.des.certification.domain.enumeration.EtatFicheChangement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut de la lecture des fiches de changement : le dispositif existant de gestion des changements
 * n'expose pas encore d'interface (CR p.32). Une fiche est lue approuvée, sauf état enregistré par un test.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.change-record-reader", havingValue = "stub", matchIfMissing = true)
public class FicheChangementStubAdapter implements FicheChangementPort {

    private final Map<String, EtatFicheChangement> etats = new ConcurrentHashMap<>();

    public void enregistrer(String reference, EtatFicheChangement etat) {
        etats.put(reference, etat);
    }

    @Override
    public EtatFicheChangement lire(String reference) {
        return etats.getOrDefault(reference, EtatFicheChangement.APPROUVEE);
    }
}
