package cm.gimac.des.homologation.application.usecase.sandbox;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Republication de l'état de toutes les liaisons pour la Sandbox GIMAC (D-122, route d'exploitation interne) : sert à
 * alimenter la projection vide de SBX-CAMPAGNE ou de SBX-LECTURE à leur mise en service. Idempotente : chaque liaison
 * repart avec sa version d'état courante (table liaison_sandbox) ; seule une liaison jamais publiée, ou dont le statut a
 * changé sans publication, reçoit une version nouvelle. Chaque appel est journalisé avec son demandeur.
 */
@Service
public class RepublicationEtatsLiaison {

    static final int PAGE = 200;
    private static final Logger LOG = LoggerFactory.getLogger(RepublicationEtatsLiaison.class);

    public record Rapport(int liaisons, int republiees, int nouvelles, Instant le) {}

    private final LiaisonRepository liaisons;
    private final PublicationEtatsLiaison publication;
    private final Clock clock;

    public RepublicationEtatsLiaison(LiaisonRepository liaisons, PublicationEtatsLiaison publication, Clock clock) {
        this.liaisons = liaisons;
        this.publication = publication;
        this.clock = clock;
    }

    @Transactional
    public Rapport republier(String demandeur) {
        int total = 0;
        int nouvelles = 0;
        Page<Liaison> page = liaisons.findAll(PageRequest.of(0, PAGE, Sort.by("id")));
        while (true) {
            for (Liaison l : page) {
                total++;
                if (publication.republier(l)) {
                    nouvelles++;
                }
            }
            if (!page.hasNext()) {
                break;
            }
            page = liaisons.findAll(page.nextPageable());
        }
        Rapport r = new Rapport(total, total - nouvelles, nouvelles, clock.instant());
        LOG.info("Republication des états de liaison pour la sandbox demandée par {} : {} liaison(s), {} republiée(s), {} version(s) nouvelle(s)", demandeur, total, r.republiees(), nouvelles);
        return r;
    }
}
