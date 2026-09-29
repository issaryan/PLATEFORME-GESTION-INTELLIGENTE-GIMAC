package cm.gimac.des.homologation.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Règles d'ouverture d'une liaison au cadrage (étape 3, D-57, D-58). */
class LiaisonOpeningPolicyTest {

    final LiaisonOpeningPolicy politique = new LiaisonOpeningPolicy();
    static final CollaboratorView CONDUCTEUR = new CollaboratorView(UUID.randomUUID(), "Conducteur", Set.of("ROLE_CONDUCTEUR_PROJET"), null, true);

    static InstitutionView institution(String statut, String categorie) {
        return new InstitutionView(UUID.randomUUID(), "Institution", statut, categorie, List.of());
    }

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(HomologationException.class, e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo(code));
    }

    @ParameterizedTest
    @ValueSource(strings = { "ADHERENT", "EN_HOMOLOGATION" })
    @DisplayName("Ouverture possible sur un adhérent ou une institution déjà en homologation")
    void eligible(String statut) {
        assertThatCode(() -> politique.verifier(institution(statut, "BANQUE"), null, CONDUCTEUR)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "CANDIDAT", "CLOS_SANS_SUITE", "SUSPENDU", "RETIRE", "ACTIF" })
    @DisplayName("Institution non adhérente : HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE")
    void nonEligible(String statut) {
        refus(() -> politique.verifier(institution(statut, "BANQUE"), null, CONDUCTEUR), "HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE");
    }

    @Test
    @DisplayName("Porteur : une banque distincte de l'institution")
    void porteur() {
        InstitutionView ep = institution("ADHERENT", "ETABLISSEMENT_PAIEMENT");
        assertThatCode(() -> politique.verifier(ep, institution("ADHERENT", "BANQUE"), CONDUCTEUR)).doesNotThrowAnyException();
        refus(() -> politique.verifier(ep, institution("ADHERENT", "MICROFINANCE"), CONDUCTEUR), "HOM_LIAISON_CARRIER_NOT_BANK");
        refus(() -> politique.verifier(ep, ep, CONDUCTEUR), "HOM_LIAISON_CARRIER_INVALID");
    }

    @Test
    @DisplayName("Conducteur : actif, du GIMAC, portant le rôle CONDUCTEUR_PROJET")
    void conducteur() {
        InstitutionView i = institution("ADHERENT", "BANQUE");
        UUID id = UUID.randomUUID();
        refus(() -> politique.verifier(i, null, null), "HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED");
        refus(() -> politique.verifier(i, null, new CollaboratorView(id, "x", Set.of("ROLE_CONDUCTEUR_PROJET"), null, false)), "HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED");
        refus(() -> politique.verifier(i, null, new CollaboratorView(id, "x", Set.of("ROLE_SNA"), null, true)), "HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED");
        refus(() -> politique.verifier(i, null, new CollaboratorView(id, "x", Set.of("ROLE_CONDUCTEUR_PROJET"), "INST-0042", true)), "HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED");
        assertThatCode(() -> politique.verifierConducteur(new CollaboratorView(id, "x", Set.of("ROLE_CONDUCTEUR_PROJET"), " ", true))).doesNotThrowAnyException();
    }
}
