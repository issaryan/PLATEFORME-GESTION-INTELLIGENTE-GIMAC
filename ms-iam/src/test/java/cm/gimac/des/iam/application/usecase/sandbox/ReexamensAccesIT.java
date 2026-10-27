package cm.gimac.des.iam.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.AbstractIamIT;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.enumeration.ReviewDecision;
import cm.gimac.des.iam.domain.enumeration.ReviewState;
import cm.gimac.des.iam.domain.sandbox.ReexamenAcces;
import cm.gimac.des.iam.repository.AccessReviewDecisionRepository;
import cm.gimac.des.iam.repository.AccessReviewRepository;
import cm.gimac.des.iam.repository.CollaboratorRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Campagne réelle sur PostgreSQL : instantané SBX, décisions, réduction et scellement stable. */
class ReexamensAccesIT extends AbstractIamIT {

    @Autowired Annuaire annuaire;
    @Autowired ReexamensAccesService service;
    @Autowired AccessReviewRepository reviews;
    @Autowired AccessReviewDecisionRepository decisions;
    @Autowired CollaboratorRepository collaborateurs;

    private String compteId;
    private UUID campagneId;

    @AfterEach
    void nettoyer() {
        if (campagneId != null) {
            decisions.deleteAll(decisions.findByReviewIdOrderByCollaboratorFederatedId(campagneId));
            decisions.flush();
            reviews.deleteById(campagneId);
            reviews.flush();
        }
        if (compteId != null) {
            for (String id : List.of(SUPER_ADMIN, RSSI, compteId)) {
                collaborateurs.findByFederatedId(id).ifPresent(collaborateurs::delete);
            }
            collaborateurs.flush();
            annuaire.supprimer(compteId);
        }
    }

    @Test
    @DisplayName("US-EXI-23 critère 2 : campagne complète, permissions SBX et empreinte de clôture")
    void campagneComplete() {
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        compteId = annuaire.creer(new NouveauCompte("revue." + suffixe, "Awa", "Revue", "revue." + suffixe + "@test.cm", null, null, "99001", Set.of("ROLE_SBX_TESTEUR", "ROLE_SBX_LECTEUR")), List.of());
        var campagne = service.ouvrirCourante(ReexamensAccesService.SYSTEME);
        campagneId = campagne.id();
        Acteur approbateur = new Acteur(UUID.randomUUID().toString(), "Approbateur de recette", Set.of("IAM_ROLE_PRIVILEGED_GRANT"));
        var acces = service.lire(campagne.id()).acces();
        assertThat(acces).filteredOn(a -> a.compteId().equals(compteId)).singleElement().satisfies(a -> {
            assertThat(a.permissions()).contains("SBX_CER_EXECUTION", "SBX_CER_CONSULTATION");
            assertThat(a.decision()).isEqualTo(ReviewDecision.EN_ATTENTE);
        });
        acces.forEach(a -> {
            if (a.decision() == ReviewDecision.EN_ATTENTE) {
                if (a.compteId().equals(compteId)) {
                    service.decider(approbateur, campagne.id(), compteId, ReviewDecision.REDUIT, Set.of("ROLE_SBX_LECTEUR"), "Fin de mission de test");
                } else {
                    service.decider(approbateur, campagne.id(), a.compteId(), ReviewDecision.MAINTENU, null, null);
                }
            }
        });
        var close = service.cloturer(approbateur, campagne.id());
        assertThat(close.state()).isEqualTo(ReviewState.CLOSE);
        assertThat(close.enAttente()).isZero();
        assertThat(annuaire.lire(compteId).orElseThrow().profils()).containsExactly("ROLE_SBX_LECTEUR");
        String calculee = ReexamenAcces.empreinte(close.period(), decisions.findByReviewIdOrderByCollaboratorFederatedId(close.id()));
        assertThat(close.digest()).isEqualTo(calculee);
        assertThat(jdbc.queryForObject("select count(*) from journal_administration where action = 'REEXAMEN_CLOS' and cible_id = ?", Integer.class, close.id().toString())).isEqualTo(1);
    }
}
