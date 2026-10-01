package cm.gimac.des.homologation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReferentialVersionHom01Test {

    private static final Instant T0 = Instant.parse("2026-10-05T08:00:00Z");

    @Test
    @DisplayName("HOM-01 : toute écriture sur une version publiée est refusée avec proposition de brouillon dérivé")
    void refuseEcritureSurVersionPubliee() {
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(TypeProjet.CARTE_INTERBANCAIRE, "Carte 2026", T0);
        v.upsertOperation("CI-AMONT-01", ReferenceOperationSamples.complete(Segment.TRONC_COMMUN_AMONT).data());
        v.publier(1, T0.plusSeconds(60), List.of("CI-AMONT-01"));

        assertThatThrownBy(() -> v.upsertOperation("CI-AMONT-01", ReferenceOperationSamples.complete(Segment.TRONC_COMMUN_AMONT).data()))
            .isInstanceOfSatisfying(HomologationException.class, ex -> {
                assertThat(ex.code()).isEqualTo("HOM_REFERENTIAL_FROZEN");
                assertThat(ex.status()).isEqualTo(409);
                assertThat(ex.details()).containsKey("brouillonDerivePropose");
            });
        assertThat(v.actionsPermises()).containsExactly("DERIVER", "MIGRER");
        assertThat(v.ordreTopologiqueListe()).containsExactly("CI-AMONT-01");
    }

    @Test
    @DisplayName("HOM-01 : la complétude nomme l'opération et ses attributs manquants")
    void completudeNommeLesAttributsManquants() {
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(TypeProjet.PAIEMENT_MOBILE, "Mobile 2026", T0);
        v.upsertOperation("PM-BR-04", ReferenceOperationSamples.complete(Segment.BRANCHE).sansPartie().sansPreuveAttendue().data());

        var manquants = new ReferentialCompletenessChecker().check(v.getOperations());

        assertThat(manquants).singleElement().satisfies(m -> {
            assertThat(m.code()).isEqualTo("PM-BR-04");
            assertThat(m.attributs()).containsExactly("partie", "preuveAttendue");
        });
    }

    @Test
    @DisplayName("HOM-01 : la dérivation copie les opérations dans un nouveau brouillon rattaché à sa source")
    void derivationCopieLesOperations() {
        ReferentialVersion publiee = ReferentialVersionSamples.publiee(TypeProjet.DELEGATAIRE, 2);
        ReferentialVersion derive = ReferentialVersion.deriverDe(publiee, "Délégataire 2027", T0.plusSeconds(3600));

        assertThat(derive.getEtat()).isEqualTo(EtatVersionReferentiel.BROUILLON);
        assertThat(derive.getVersionSourceId()).isEqualTo(publiee.getId());
        assertThat(derive.getOperations()).extracting(ReferenceOperation::getCode).containsExactly("DL-AMONT-01", "DL-BR-01", "DL-AVAL-01");
        assertThat(derive.getOperations()).allSatisfy(op -> assertThat(op.getVersion()).isSameAs(derive));
        assertThat(derive.actionsPermises()).containsExactly("EDITER", "CONTROLER", "PUBLIER");
    }

    @Test
    @DisplayName("HOM-01 : retirer ne concerne qu'une version publiée")
    void retrait() {
        ReferentialVersion publiee = ReferentialVersionSamples.publiee(TypeProjet.DELEGATAIRE, 1);
        publiee.retirer();
        assertThat(publiee.getEtat()).isEqualTo(EtatVersionReferentiel.RETIREE);
        ReferentialVersion brouillon = ReferentialVersionSamples.brouillonComplet(TypeProjet.DELEGATAIRE);
        brouillon.retirer();
        assertThat(brouillon.getEtat()).isEqualTo(EtatVersionReferentiel.BROUILLON);
    }
}
