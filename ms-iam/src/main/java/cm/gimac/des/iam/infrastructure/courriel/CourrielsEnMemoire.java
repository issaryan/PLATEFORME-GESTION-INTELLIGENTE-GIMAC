package cm.gimac.des.iam.infrastructure.courriel;

import cm.gimac.des.iam.application.port.out.ServiceCourriel;
import cm.gimac.des.iam.domain.compte.Courriel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Messagerie de substitution (profils dev et test, PI §0.8) : les courriels sont gardés en mémoire
 * (100 au plus) pour la démonstration et les tests, jamais envoyés. Jamais sélectionnée en production.
 */
public class CourrielsEnMemoire implements ServiceCourriel {

    private static final Logger LOG = LoggerFactory.getLogger(CourrielsEnMemoire.class);
    private static final int CAPACITE = 100;

    private final List<Courriel> envoyes = new ArrayList<>();

    @Override
    public synchronized void envoyer(Courriel courriel) {
        if (envoyes.size() == CAPACITE) {
            envoyes.remove(0);
        }
        envoyes.add(courriel);
        LOG.info("Courriel « {} » retenu en mémoire (messagerie de substitution)", courriel.objet());
    }

    public synchronized List<Courriel> envoyes() {
        return List.copyOf(envoyes);
    }

    /** Dernier courriel adressé à ce destinataire. */
    public synchronized Optional<Courriel> dernierPour(String destinataire) {
        for (int i = envoyes.size() - 1; i >= 0; i--) {
            if (envoyes.get(i).destinataire().equalsIgnoreCase(destinataire)) {
                return Optional.of(envoyes.get(i));
            }
        }
        return Optional.empty();
    }
}
