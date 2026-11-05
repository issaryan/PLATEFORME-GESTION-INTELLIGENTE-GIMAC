package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.in.RepriseCloturesUseCase;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.Acteur;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.event.LiaisonClosedWithoutFollowUpPayload;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.StatutParcours;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Clôture sans suite des liaisons reprises (D-137). Une transaction par liaison, comme la reprise : en simulation,
 * chaque ligne est jouée jusqu'à l'écriture en base puis annulée. Refus : liaison inconnue, liaison en production, et
 * liaison qui porte un travail réel de la plateforme (opération faite sans preuve de reprise, ou en validation ou en
 * signature) : celle-ci se clôt par son parcours, jamais par la reprise. Effets : statut CLOSE_SANS_SUITE (le parcours
 * le conserve), état technique CLOSE, motif dans {@code motif_etat}, instant de reprise ; événements
 * homologation.liaison-closed-without-follow-up.v1 (trace : auteur, motif, statut d'avant) et, par l'observation du
 * dépôt, homologation.liaison-status-changed.v1. Rien n'est supprimé.
 */
@Service
public class RepriseClotures implements RepriseCloturesUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RepriseClotures.class);
    private static final Set<EtatOperationParcours> EN_COURS_SUR_LA_PLATEFORME = Set.of(EtatOperationParcours.EN_VALIDATION, EtatOperationParcours.EN_SIGNATURE);

    private final LiaisonRepository liaisons;
    private final ParcoursDepot depot;
    private final OutboxPublisher outbox;
    private final EntityManager em;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public RepriseClotures(LiaisonRepository liaisons, ParcoursDepot depot, OutboxPublisher outbox, EntityManager em, PlatformTransactionManager transactions, Clock clock) {
        this.liaisons = liaisons;
        this.depot = depot;
        this.outbox = outbox;
        this.em = em;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    @Override
    public Rapport clore(List<ClotureReprise> lignes, boolean simulation, String motif, Acteur acteur) {
        if (lignes == null || lignes.isEmpty()) {
            throw HomologationException.unprocessable("HOM_REPRISE_VIDE", "Aucune liaison à clore");
        }
        if (lignes.size() > LIGNES_MAX) {
            throw HomologationException.unprocessableDetails("HOM_REPRISE_TROP_DE_LIGNES", "Lot trop volumineux", Map.of("max", LIGNES_MAX, "recues", lignes.size()));
        }
        String m = motif == null ? "" : motif.trim();
        if (m.length() < MOTIF_MIN || m.length() > MOTIF_MAX) {
            throw HomologationException.unprocessableDetails("HOM_REPRISE_MOTIF_OBLIGATOIRE", "Motif de clôture obligatoire, de 3 à 500 caractères", Map.of("motif", m.length()));
        }
        if (acteur == null || acteur.id() == null) {
            throw HomologationException.forbidden("HOM_USER_TOKEN_REQUIRED", "Jeton identifiant le compte de reprise exigé");
        }
        Set<String> vues = new HashSet<>();
        List<Ligne> rapport = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            ClotureReprise l = lignes.get(i);
            String ref = l == null || l.referenceOrigine() == null || l.referenceOrigine().isBlank() ? null : l.referenceOrigine().trim();
            if (ref == null) {
                rapport.add(refus(i, null, "HOM_REPRISE_LIGNE_INVALIDE", "Référence d'origine absente", Map.of()));
            } else if (!vues.add(ref)) {
                rapport.add(refus(i, ref, "HOM_REPRISE_DOUBLON_FICHIER", "Référence d'origine déjà présente plus haut dans le fichier", Map.of("referenceOrigine", ref)));
            } else {
                rapport.add(executer(i, ref, motifLigne(m, l.motif()), simulation, acteur));
            }
        }
        Map<Resultat, Long> totaux = new EnumMap<>(Resultat.class);
        for (Resultat r : Resultat.values()) {
            totaux.put(r, rapport.stream().filter(x -> x.resultat() == r).count());
        }
        LOG.info("Clôture sans suite des liaisons reprises ({}) par {} : {}", simulation ? "simulation" : "réelle", acteur.id(), totaux);
        return new Rapport(simulation, lignes.size(), totaux, rapport);
    }

    static String motifLigne(String lot, String propre) {
        String texte = propre == null || propre.isBlank() ? lot : lot + " : " + propre.trim();
        return texte.length() > MOTIF_MAX ? texte.substring(0, MOTIF_MAX) : texte;
    }

    private Ligne executer(int rang, String ref, String motif, boolean simulation, Acteur acteur) {
        try {
            return transaction.execute(statut -> {
                Ligne r = traiter(rang, ref, motif, acteur);
                em.flush();
                if (simulation) {
                    statut.setRollbackOnly();
                }
                return r;
            });
        } catch (HomologationException e) {
            return refus(rang, ref, e.code(), e.getMessage(), e.details());
        } catch (DataAccessException | PersistenceException e) {
            LOG.warn("Clôture de la liaison {} refusée par la base : {}", ref, e.getClass().getSimpleName());
            return refus(rang, ref, "HOM_REPRISE_ECRITURE_REFUSEE", "Écriture refusée par la base de données", Map.of("cause", e.getClass().getSimpleName()));
        }
    }

    private Ligne traiter(int rang, String ref, String motif, Acteur acteur) {
        Liaison trouvee = liaisons
            .findByReferenceOrigine(ref)
            .orElseThrow(() -> HomologationException.notFound("HOM_LIAISON_INTROUVABLE", "Aucune liaison ne porte cette référence d'origine", ref));
        Parcours p = depot.trouver(trouvee.getId(), true).orElseThrow(() -> ParcoursDepot.introuvable(trouvee.getId()));
        StatutParcours avant = p.etat().getStatut();
        if (avant == StatutParcours.CLOSE_SANS_SUITE) {
            return new Ligne(rang, ref, Resultat.INCHANGEE, trouvee.getId(), avant.name(), null, null, Map.of());
        }
        if (avant == StatutParcours.EN_PRODUCTION) {
            throw HomologationException.conflict("HOM_REPRISE_CLOTURE_EN_PRODUCTION", "Une liaison en production ne se clôt pas sans suite", Map.of("statut", avant.name()));
        }
        long traitees = p
            .operations(o -> EN_COURS_SUR_LA_PLATEFORME.contains(o.getEtat()) || (o.getEtat() == EtatOperationParcours.FAITE && (o.getPreuve() == null || !o.getPreuve().estReprise())))
            .size();
        if (traitees > 0) {
            throw HomologationException.conflict(
                "HOM_REPRISE_CLOTURE_TRAITEE",
                "La liaison porte un travail réel de la plateforme : elle se clôt par son parcours, pas par la reprise",
                Map.of("operations", traitees)
            );
        }
        Instant maintenant = clock.instant();
        Liaison liaison = p.liaison();
        p.etat().setStatut(StatutParcours.CLOSE_SANS_SUITE);
        p.rafraichir();
        liaison.setEtat(EtatLiaison.CLOSE);
        String texte = "Close sans suite par la reprise de l'existant : " + motif;
        liaison.setMotifEtat(texte.length() > MOTIF_MAX ? texte.substring(0, MOTIF_MAX) : texte);
        liaison.setRepriseLe(maintenant);
        outbox.publish(
            RepriseLiaisons.AGREGAT,
            liaison.getId(),
            LiaisonClosedWithoutFollowUpPayload.TYPE,
            new LiaisonClosedWithoutFollowUpPayload(liaison.getId(), liaison.getInstitutionId(), ref, avant.name(), motif, acteur.id(), acteur.nom(), maintenant)
        );
        return new Ligne(rang, ref, Resultat.CLOSE, liaison.getId(), avant.name(), null, null, Map.of());
    }

    private static Ligne refus(int rang, String ref, String code, String motif, Map<String, Object> details) {
        return new Ligne(rang, ref, Resultat.REFUSEE, null, null, code, motif, details == null ? Map.of() : details);
    }
}
