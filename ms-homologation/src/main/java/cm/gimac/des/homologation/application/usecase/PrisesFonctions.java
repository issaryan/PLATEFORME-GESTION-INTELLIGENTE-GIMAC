package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Personne;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prise d'une fonction à affecter (D-93, D-94, docs/lot1/effet-reel-referentiels.md §8.8) : la tâche à prendre offerte
 * dans MS-PILOTAGE (référence {@code <réf.>/AFFECTER/<FONCTION>}) a été prise ou attribuée (pilotage.task-taken.v1). La
 * personne est affectée à la fonction sur la liaison, comme par {@code PUT /liaisons/{id}/fonctions/{fonction}} : profil
 * de la fonction ou l'une de ses permissions exigé, fonction à affecter et ancien titulaire remplacés sur les opérations
 * non faites ; les tâches suivent (SuiviTachesParcours). Une prise devenue sans objet (fonction déjà affectée, liaison
 * inconnue, personne non éligible) est consignée et ignorée ; MS-IAM indisponible : l'exception remonte (réessai).
 */
@Service
@Transactional
public class PrisesFonctions {

    static final String SEGMENT = "/AFFECTER/";
    private static final Logger LOG = LoggerFactory.getLogger(PrisesFonctions.class);

    private final ParcoursDepot depot;
    private final Intervenants intervenants;

    public PrisesFonctions(ParcoursDepot depot, Intervenants intervenants) {
        this.depot = depot;
        this.intervenants = intervenants;
    }

    /** Fonction d'une référence d'origine {@code <réf.>/AFFECTER/<FONCTION>}, sinon vide. */
    static Optional<String> fonctionDe(String reference) {
        if (reference == null) {
            return Optional.empty();
        }
        int i = reference.lastIndexOf(SEGMENT);
        if (i <= 0) {
            return Optional.empty();
        }
        String code = reference.substring(i + SEGMENT.length());
        return Fonction.CODE.matcher(code).matches() ? Optional.of(code) : Optional.empty();
    }

    /**
     * Affecte {@code personneId} à la fonction de la référence sur la liaison ; {@code acteurId} : la personne qui a pris
     * la tâche, ou le chef qui l'a attribuée. Rend vrai si l'affectation est faite.
     */
    public boolean affecter(UUID liaisonId, String reference, String personneId, String acteurId, String mode) {
        Optional<String> code = fonctionDe(reference);
        if (liaisonId == null || code.isEmpty() || personneId == null || personneId.isBlank()) {
            return false;
        }
        Parcours p = depot.trouver(liaisonId, true).orElse(null);
        if (p == null || !reference.equals(p.etat().getReference() + SEGMENT + code.orElseThrow())) {
            LOG.warn("Prise de la tâche {} ignorée : liaison {} inconnue ou référence étrangère", reference, liaisonId);
            return false;
        }
        Optional<Fonction> f = Fonction.duCatalogue(code.orElseThrow()).filter(x -> !x.externe());
        if (f.isEmpty() || !SuiviTachesParcours.aAffecter(p).containsKey(code.orElseThrow())) {
            LOG.info("Prise de la tâche {} sans objet : la fonction n'est plus à affecter sur la liaison {}", reference, liaisonId);
            return false;
        }
        Personne personne;
        try {
            personne = intervenants.pourFonction(personneId, f.orElseThrow());
        } catch (HomologationException e) {
            if (e.status() >= 500) {
                throw e;
            }
            LOG.warn("Prise de la tâche {} par {} refusée ({}) : la fonction reste à affecter", reference, personneId, e.code());
            return false;
        }
        p.affecterFonction(f.orElseThrow(), personne);
        LOG.info(
            "Fonction {} affectée sur la liaison {} par {} de la tâche à prendre ({}, acteur {}) : {}",
            f.orElseThrow(),
            liaisonId,
            "ATTRIBUTION".equals(mode) ? "attribution" : "prise",
            reference,
            acteurId,
            personne.id()
        );
        return true;
    }
}
