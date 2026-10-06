package cm.gimac.des.iam.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.demande.ContenuDemande;
import cm.gimac.des.iam.domain.demande.EtatDemande;
import cm.gimac.des.iam.domain.demande.TypeDemande;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** D-81 §4 et §5 : forme des demandes, unicité, profils après l'acte ; règles des profils créés. */
class ReglesDemandesEtProfilsTest {

    static final Acteur SUPER = new Acteur("super", "Super", Set.of("IAM_ROLE_PRIVILEGED_GRANT"));
    static final Acteur RSSI = new Acteur("rssi", "RSSI", Set.of());

    static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    static ContenuDemande creation(String identifiant) {
        return ContenuDemande.creation(identifiant, " Awa ", "Mballa", "awa@gimac.cm", null, "Technicienne", "POLE_RESEAU", "chef", Set.of("ROLE_COLLABORATEUR"));
    }

    @Test
    @DisplayName("Type → action de la matrice et permission de l'acte")
    void actionsEtPermissions() {
        assertThat(ReglesDemandes.action(TypeDemande.CREATION_COMPTE)).isEqualTo(ActionPouvoir.COMPTE_DEMANDER);
        assertThat(ReglesDemandes.action(TypeDemande.PROFILS)).isEqualTo(ActionPouvoir.PROFIL_DEMANDER);
        assertThat(ReglesDemandes.action(TypeDemande.STATUT)).isEqualTo(ActionPouvoir.STATUT_DEMANDER);
        assertThat(ReglesDemandes.permissionActe(TypeDemande.CREATION_COMPTE)).isEqualTo("IAM_ACCOUNT_CREATE");
        assertThat(ReglesDemandes.permissionActe(TypeDemande.PROFILS)).isEqualTo("IAM_ACCOUNT_ROLES");
        assertThat(ReglesDemandes.permissionActe(TypeDemande.STATUT)).isEqualTo("IAM_ACCOUNT_STATUS");
    }

    @Test
    @DisplayName("Motif de 5 à 500 caractères ; contenu selon le type ; cible exigée ou interdite")
    void forme() {
        assertThat(ReglesDemandes.motif("  Renfort  ", "motif")).isEqualTo("Renfort");
        refus(() -> ReglesDemandes.motif("abc", "motif"), "ORG_DEMANDE_INVALIDE", 400);
        refus(() -> ReglesDemandes.motif("x".repeat(501), "motif"), "ORG_DEMANDE_INVALIDE", 400);
        refus(() -> ReglesDemandes.motif(null, "motif"), "ORG_DEMANDE_INVALIDE", 400);

        ContenuDemande c = ReglesDemandes.normaliser(TypeDemande.CREATION_COMPTE, null, creation("Awa.Mballa"));
        assertThat(c.identifiant()).isEqualTo("awa.mballa");
        assertThat(c.prenom()).isEqualTo("Awa");
        assertThat(c.ajouter()).isNull();
        refus(() -> ReglesDemandes.normaliser(TypeDemande.CREATION_COMPTE, "cible", creation("awa")), "ORG_DEMANDE_INVALIDE", 400);
        refus(() -> ReglesDemandes.normaliser(TypeDemande.CREATION_COMPTE, null, creation("1x")), "IAM_USERNAME_INVALID", 400);
        refus(
            () -> ReglesDemandes.normaliser(TypeDemande.CREATION_COMPTE, null, ContenuDemande.creation("awa", "A", "B", "pas-un-courriel", null, "F", "U", "R", Set.of("ROLE_X"))),
            "ORG_DEMANDE_INVALIDE",
            400
        );
        refus(() -> ReglesDemandes.normaliser(TypeDemande.CREATION_COMPTE, null, ContenuDemande.creation("awa", "A", "B", "a@b.cm", null, "F", null, "R", Set.of("ROLE_X"))), "ORG_DEMANDE_INVALIDE", 400);
        refus(() -> ReglesDemandes.normaliser(TypeDemande.CREATION_COMPTE, null, ContenuDemande.creation("awa", "A", "B", "a@b.cm", null, "F", "U", "R", Set.of())), "ORG_DEMANDE_INVALIDE", 400);

        refus(() -> ReglesDemandes.normaliser(TypeDemande.PROFILS, null, ContenuDemande.profils(Set.of("ROLE_A"), Set.of())), "ORG_DEMANDE_INVALIDE", 400);
        refus(() -> ReglesDemandes.normaliser(TypeDemande.PROFILS, "c", ContenuDemande.profils(Set.of(), null)), "ORG_DEMANDE_INVALIDE", 400);
        refus(() -> ReglesDemandes.normaliser(TypeDemande.PROFILS, "c", ContenuDemande.profils(Set.of("ROLE_A"), Set.of("ROLE_A"))), "ORG_DEMANDE_INVALIDE", 400);
        assertThat(ReglesDemandes.normaliser(TypeDemande.PROFILS, "c", ContenuDemande.profils(Set.of("ROLE_A"), null)).retirer()).isEmpty();
        refus(() -> ReglesDemandes.normaliser(TypeDemande.STATUT, "c", new ContenuDemande(null, null, null, null, null, null, null, null, null, null, null, null)), "ORG_DEMANDE_INVALIDE", 400);
        assertThat(ReglesDemandes.normaliser(TypeDemande.STATUT, "c", ContenuDemande.statut(false)).actif()).isFalse();
        refus(() -> ReglesDemandes.normaliser(null, "c", ContenuDemande.statut(false)), "ORG_DEMANDE_INVALIDE", 400);
    }

    @Test
    @DisplayName("Unicité par cible et type ; profils proposés et profils après l'acte ; demande décidée immuable")
    void uniciteEtActe() {
        ContenuDemande c = ReglesDemandes.normaliser(TypeDemande.CREATION_COMPTE, null, creation("awa"));
        assertThat(ReglesDemandes.cleCible(TypeDemande.CREATION_COMPTE, null, c)).isEqualTo("identifiant:awa");
        assertThat(ReglesDemandes.cleCible(TypeDemande.STATUT, "id-1", ContenuDemande.statut(true))).isEqualTo("id-1");
        ContenuDemande p = ContenuDemande.profils(Set.of("ROLE_SNA"), Set.of("ROLE_COLLABORATEUR"));
        assertThat(ReglesDemandes.profilsProposes(p)).containsExactly("ROLE_COLLABORATEUR", "ROLE_SNA");
        assertThat(ReglesDemandes.profilsApres(Set.of("ROLE_COLLABORATEUR", "ROLE_X"), p)).containsExactly("ROLE_SNA", "ROLE_X");
        ReglesDemandes.exigerEnAttente(EtatDemande.EN_ATTENTE);
        for (EtatDemande e : List.of(EtatDemande.ACCEPTEE, EtatDemande.REFUSEE, EtatDemande.ANNULEE)) {
            refus(() -> ReglesDemandes.exigerEnAttente(e), "ORG_DEMANDE_TRAITEE", 409);
        }
    }

    @Test
    @DisplayName("Profils créés : code, permissions internes interdites (422), privilégié réservé à l'octroi privilégié, permissions communes")
    void profilsCrees() {
        ReglesProfils.verifierCode("ROLE_QUALITE");
        for (String code : List.of("QUALITE", "ROLE_qualite", "ROLE_AB", "ROLE_" + "A".repeat(42), "ROLE_1ABC")) {
            refus(() -> ReglesProfils.verifierCode(code), "IAM_PROFIL_CODE_INVALIDE", 400);
        }
        List<String> connues = List.of("HOM_DASHBOARD_READ", "IAM_ACCOUNT_READ", "PLATFORM_OPS_READ", "INTERNAL_IAM_READ", "COL_MESSAGE_READ");
        refus(() -> ReglesProfils.verifierComposition(SUPER, Set.of("INTERNAL_IAM_READ"), connues), "IAM_PROFIL_PERMISSION_INTERNE", 422);
        refus(() -> ReglesProfils.verifierComposition(SUPER, Set.of("INCONNUE_X"), connues), "IAM_PERMISSION_UNKNOWN", 400);
        refus(() -> ReglesProfils.verifierComposition(RSSI, Set.of("IAM_ACCOUNT_READ"), connues), "IAM_PRIVILEGED_PROFILE", 403);
        refus(() -> ReglesProfils.verifierComposition(RSSI, Set.of("PLATFORM_OPS_READ"), connues), "IAM_PRIVILEGED_PROFILE", 403);
        ReglesProfils.verifierComposition(SUPER, Set.of("IAM_ACCOUNT_READ"), connues);
        ReglesProfils.verifierComposition(RSSI, Set.of("HOM_DASHBOARD_READ"), connues);
        assertThat(ReglesProfils.privilegie(Set.of("HOM_DASHBOARD_READ"))).isFalse();
        assertThat(ReglesProfils.privilegie(Set.of("PLATFORM_OPS_READ"))).isTrue();
        assertThat(ReglesProfils.avecCommunes(Set.of("HOM_DASHBOARD_READ"))).containsExactly(
            "COL_MESSAGE_READ",
            "COL_MESSAGE_SEND",
            "HOM_DASHBOARD_READ",
            "NOT_INBOX_READ",
            "PIL_ACTIVITY_OWN"
        );
    }

    @Test
    @DisplayName("D-86 : libellé normalisé de 1 à 80 caractères, description de 0 à 500 (obligatoire à la création), sinon IAM_PROFIL_INVALIDE")
    void presentationDesProfils() {
        assertThat(ReglesProfils.libelle("  Pôle réseau ")).isEqualTo("Pôle réseau");
        assertThat(ReglesProfils.libelle("x".repeat(80))).hasSize(80);
        refus(() -> ReglesProfils.libelle(null), "IAM_PROFIL_INVALIDE", 400);
        refus(() -> ReglesProfils.libelle("   "), "IAM_PROFIL_INVALIDE", 400);
        refus(() -> ReglesProfils.libelle("x".repeat(81)), "IAM_PROFIL_INVALIDE", 400);
        assertThat(ReglesProfils.description(null, false)).isEmpty();
        assertThat(ReglesProfils.description(" Mission ", false)).isEqualTo("Mission");
        assertThat(ReglesProfils.description("d".repeat(500), true)).hasSize(500);
        refus(() -> ReglesProfils.description("d".repeat(501), false), "IAM_PROFIL_INVALIDE", 400);
        refus(() -> ReglesProfils.description(" ", true), "IAM_PROFIL_INVALIDE", 400);
    }
}
