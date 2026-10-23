package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.usecase.JournalActes;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministration;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import cm.gimac.des.iam.infrastructure.security.CorrelationContext;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traces des actes de la sandbox dans MS-IAM (D-119) : un acte sur un compte passe par le journal de l'administration
 * (entrée et iam.account-administered.v1, équivalent de iam.user-changed.v1 du carnet) ; un acte sur un périmètre ou une
 * délégation est inscrit au même journal, sa cible étant le périmètre ou la délégation.
 */
@Component
public class JournalSandbox {

    private final JournalActes actes;
    private final JournalAdministrationRepository journal;
    private final Clock clock;

    public JournalSandbox(JournalActes actes, JournalAdministrationRepository journal, Clock clock) {
        this.actes = actes;
        this.journal = journal;
        this.clock = clock;
    }

    /** Acte sur un compte : entrée du journal et événement du compte. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void compte(Acteur acteur, String action, String compteId, String libelle, String motif, List<String> details) {
        actes.compte(acteur, action, compteId, libelle, motif, details);
    }

    /** Acte sur un objet de la sandbox (périmètre, délégation, compte de service) : entrée du journal. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void objet(Acteur acteur, String action, String cibleId, String libelle, String motif, List<String> details) {
        journal.save(
            new JournalAdministration(
                clock.instant(),
                acteur.id(),
                acteur.nom(),
                action,
                cibleId,
                libelle,
                motif == null ? null : tronquer(motif, 1000),
                tronquer(String.join(", ", details), 2000),
                CorrelationContext.current()
            )
        );
    }

    static String tronquer(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
