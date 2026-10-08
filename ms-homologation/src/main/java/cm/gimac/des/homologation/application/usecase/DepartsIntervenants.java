package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.repository.OperationParcoursRepository;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Départ d'un compte (iam.account-administered.v1, DESACTIVATION ou SUPPRESSION, D-93 §1) : sur chaque liaison non
 * close où il tient une qualité, une ressource ou la conduite, ses rôles repassent « à affecter » ; les tâches suivent
 * (SuiviTachesParcours) : la sienne est terminée, le conducteur reçoit « Affecter &lt;fonction&gt; ».
 */
@Service
@Transactional
public class DepartsIntervenants {

    private static final Logger LOG = LoggerFactory.getLogger(DepartsIntervenants.class);

    private final OperationParcoursRepository operations;
    private final ParcoursDepot depot;

    public DepartsIntervenants(OperationParcoursRepository operations, ParcoursDepot depot) {
        this.operations = operations;
        this.depot = depot;
    }

    /** Rend le nombre de liaisons touchées. */
    public int traiterDepart(String compteId) {
        if (compteId == null || compteId.isBlank()) {
            return 0;
        }
        int n = 0;
        for (UUID liaison : operations.liaisonsImpliquant(compteId, "%\"" + compteId + "\"%")) {
            Parcours p = depot.trouver(liaison, true).orElse(null);
            if (p == null || !p.implique(compteId)) {
                continue;
            }
            Set<Fonction> liberees = p.retirerCompte(compteId);
            p.rafraichir();
            n++;
            LOG.info("Départ du compte {} : fonctions {} à affecter sur la liaison {}", compteId, liberees, liaison);
        }
        return n;
    }
}
