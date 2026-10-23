package cm.gimac.des.homologation.application.usecase.sandbox;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.sandbox.StatutInstitutionApplique;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.StatutInstitutionAppliqueRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gel des liaisons sur changement de statut de l'institution (HOM-06, D-122), déclenché par
 * participant.status-changed.v1 : une institution SUSPENDU ou RETIRE fait passer ses liaisons actives à GELEE (état
 * d'avant le gel retenu dans {@code etat_avant_gel}, motif dans {@code motif_etat}) ; au retour à un autre statut,
 * chaque liaison gelée retrouve l'état qu'elle avait. Chaque liaison changée est observée par
 * {@link PublicationEtatsLiaison} : homologation.liaison-status-changed.v1 part avec une version d'état supérieure.
 */
@Service
public class GelLiaisonsInstitution {

    private static final Logger LOG = LoggerFactory.getLogger(GelLiaisonsInstitution.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public enum Effet {
        GEL,
        DEGEL,
        PERIME,
    }

    public record Resultat(Effet effet, int liaisons) {}

    private final StatutInstitutionAppliqueRepository statuts;
    private final LiaisonRepository liaisons;
    private final PublicationEtatsLiaison publication;
    private final Clock clock;

    public GelLiaisonsInstitution(StatutInstitutionAppliqueRepository statuts, LiaisonRepository liaisons, PublicationEtatsLiaison publication, Clock clock) {
        this.statuts = statuts;
        this.liaisons = liaisons;
        this.publication = publication;
        this.clock = clock;
    }

    @Transactional
    public Resultat appliquer(UUID institutionId, String statut, Long version, Instant dateEffet, String motif) {
        Instant t = clock.instant();
        Optional<StatutInstitutionApplique> connu = statuts.verrouiller(institutionId);
        if (connu.isPresent() && !connu.orElseThrow().depasse(version, dateEffet)) {
            LOG.info("Statut {} de l'institution {} (version {}, effet {}) périmé : sans effet", statut, institutionId, version, dateEffet);
            return new Resultat(Effet.PERIME, 0);
        }
        if (connu.isPresent()) {
            connu.orElseThrow().retenir(statut, version, dateEffet, t);
        } else {
            statuts.save(new StatutInstitutionApplique(institutionId, statut, version, dateEffet, t));
        }
        boolean gel = StatutInstitutionApplique.gelant(statut);
        int n = 0;
        for (Liaison trouvee : liaisons.findByInstitutionId(institutionId)) {
            Liaison l = liaisons.findByIdForUpdate(trouvee.getId()).orElseThrow();
            if (gel ? geler(l, statut, motif) : degeler(l)) {
                publication.observer(l);
                n++;
            }
        }
        LOG.info("Institution {} au statut {} : {} liaison(s) {}", institutionId, statut, n, gel ? "gelée(s)" : "rétablie(s)");
        return new Resultat(gel ? Effet.GEL : Effet.DEGEL, n);
    }

    private static boolean geler(Liaison l, String statut, String motif) {
        if (l.getEtat() == EtatLiaison.CLOSE || l.getEtat() == EtatLiaison.GELEE) {
            return false;
        }
        l.setEtatAvantGel(JSON.createObjectNode().put("etat", l.getEtat().name()).put("statutInstitution", statut).toString());
        l.setEtat(EtatLiaison.GELEE);
        String texte = "Liaison gelée : institution " + statut + " au registre" + (motif == null || motif.isBlank() ? "" : " (" + motif.trim() + ")");
        l.setMotifEtat(texte.length() > 500 ? texte.substring(0, 500) : texte);
        return true;
    }

    private static boolean degeler(Liaison l) {
        if (l.getEtat() != EtatLiaison.GELEE || l.getEtatAvantGel() == null) {
            return false;
        }
        JsonNode avant = JSON.readTree(l.getEtatAvantGel());
        l.setEtat(EtatLiaison.valueOf(avant.path("etat").asString()));
        l.setEtatAvantGel(null);
        l.setMotifEtat(null);
        return true;
    }
}
