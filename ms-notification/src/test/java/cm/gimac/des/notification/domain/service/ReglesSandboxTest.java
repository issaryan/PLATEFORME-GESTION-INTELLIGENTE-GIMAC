package cm.gimac.des.notification.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.domain.service.ReglesSandbox.Destinataire;
import cm.gimac.des.notification.domain.service.ReglesSandbox.Modele;
import cm.gimac.des.notification.domain.service.ReglesSandbox.Portee;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Règles et gabarits de la sandbox (US-EXI-09) : portée, rôles, cloisonnement par institution, aucune donnée sensible. */
class ReglesSandboxTest {

    final ReglesSandbox regles = new ReglesSandbox();

    static final Map<String, Set<String>> INSTITUTION = Map.of(
        "adm",
        Set.of("ROLE_SBX_ADMIN_INSTITUTION"),
        "chef",
        Set.of("ROLE_SBX_CHEF_PROJET", "ROLE_SBX_TESTEUR"),
        "dev",
        Set.of("ROLE_SBX_DEVELOPPEUR")
    );

    static List<String> porteurs(String permission) {
        return switch (permission) {
            case "SBX_HAB_ADMINISTRATION" -> List.of("g1", "adm");
            case "SBX_LEC_ADMINISTRATION" -> List.of("rssi");
            default -> List.of();
        };
    }

    Modele m(String code) {
        return regles.modele(code).orElseThrow();
    }

    @Test
    @DisplayName("Invitation : compte nommé de l'institution sur le portail ; compte étranger à l'institution ignoré")
    void invitation() {
        List<Destinataire> d = regles.destinataires(m("SBX_ESP_INVITATION_ADMINISTRATEUR"), List.of("adm", "etranger", " ", "adm"), INSTITUTION, ReglesSandboxTest::porteurs);
        assertThat(d).containsExactly(new Destinataire("adm", true));
        assertThat(regles.titre(m("SBX_ESP_INVITATION_ADMINISTRATEUR"), "99001")).isEqualTo("Invitation : administrateur de l'espace sandbox 99001");
        assertThat(regles.lien(true, "99001")).isEqualTo("/portail");
    }

    @Test
    @DisplayName("Portée mixte : rôles de l'institution sur le portail, rôles du GIMAC sur le DES, premier titre retenu")
    void mixte() {
        List<Destinataire> d = regles.destinataires(
            m("SBX_ESP_ESPACE_SUSPENDU"),
            Arrays.asList(null, "ROLE:ADMINISTRATEUR_INSTITUTION", "ROLE:CHEF_DE_PROJET", "ROLE:ADMINISTRATEUR_GIMAC", "ROLE:INCONNU"),
            INSTITUTION,
            ReglesSandboxTest::porteurs
        );
        assertThat(d).containsExactly(new Destinataire("adm", true), new Destinataire("chef", true), new Destinataire("g1", false));
        assertThat(regles.lien(false, "99001")).isEqualTo("/des/sandbox/espaces/99001");
    }

    @Test
    @DisplayName("Portée GIMAC : rôle d'institution et compte nommé ignorés ; portée institution : rôle du GIMAC ignoré")
    void portees() {
        assertThat(regles.destinataires(m("SBX_ESP_FERMETURE_A_PRONONCER"), List.of("adm", "ROLE:ADMINISTRATEUR_INSTITUTION", "ROLE:RSSI"), INSTITUTION, ReglesSandboxTest::porteurs))
            .containsExactly(new Destinataire("rssi", false));
        assertThat(regles.destinataires(m("SBX_ESP_MANDAT_ACCORDE"), List.of("ROLE:ADMINISTRATEUR_GIMAC", "ROLE:DEVELOPPEUR"), INSTITUTION, ReglesSandboxTest::porteurs))
            .containsExactly(new Destinataire("dev", true));
        assertThat(regles.destinataires(m("SBX_ESP_MANDAT_ACCORDE"), null, INSTITUTION, ReglesSandboxTest::porteurs)).isEmpty();
    }

    @Test
    @DisplayName("Mandat : intervenant désigné admis hors de l'institution, valeur qui n'est pas un compte écartée ; ailleurs : écarté")
    void intervenantsDuMandat() {
        String partenaire = "0f0e0d0c-0b0a-4909-8807-060504030201";
        assertThat(regles.destinataires(m("SBX_ESP_MANDAT_ACCORDE"), List.of(partenaire, "pas-un-compte", "ROLE:ADMINISTRATEUR_INSTITUTION"), INSTITUTION, ReglesSandboxTest::porteurs))
            .containsExactly(new Destinataire(partenaire, true), new Destinataire("adm", true));
        assertThat(regles.destinataires(m("SBX_ESP_INVITATION_COLLABORATEUR"), List.of(partenaire), INSTITUTION, ReglesSandboxTest::porteurs)).isEmpty();
        assertThat(m("SBX_ESP_RAPPEL_ECHEANCE_MANDAT").intervenants()).isTrue();
        assertThat(m("SBX_DON_CLE_APPROUVEE").portee()).isEqualTo(Portee.GIMAC);
        assertThat(ReglesSandbox.ROLES.get("PRODUCTION_MONETIQUE").permissionGimac()).isEqualTo("SBX_DON_ADMINISTRATION");
    }

    @Test
    @DisplayName("Codes : valeur libre remplacée par « ? », lien vers la liste des espaces ; modèle inconnu ou nul absent")
    void codes() {
        assertThat(ReglesSandbox.code(" 99001 ")).isEqualTo("99001");
        assertThat(ReglesSandbox.code("<script>")).isEqualTo("?");
        assertThat(ReglesSandbox.code(null)).isEqualTo("?");
        assertThat(regles.titre(m("SBX_TRA_ALERTE"), "a b")).isEqualTo("Alerte de la sandbox (?)");
        assertThat(regles.lien(false, null)).isEqualTo("/des/sandbox/espaces");
        assertThat(regles.modele("SBX_INCONNU")).isEmpty();
        assertThat(regles.modele(null)).isEmpty();
        assertThat(regles.modele(" SBX_TRA_ALERTE ")).isPresent();
        assertThat(ReglesSandbox.rolesDemandes(Arrays.asList("a", null, " ROLE:RSSI", "ROLE:TESTEUR"))).containsExactly("RSSI", "TESTEUR");
        assertThat(ReglesSandbox.rolesDemandes(null)).isEmpty();
    }

    @Test
    @DisplayName("Gabarits : sans identifiant, secret, mot de passe, numéro de carte ni donnée de test ; portées couvertes")
    void gabaritsSansDonneeSensible() {
        Pattern sensible = Pattern.compile("(?i)(mot de passe|secret|jeton|identifiant de connexion|password|token|pan\\b|\\d{12,}|@)");
        assertThat(regles.modeles()).hasSizeGreaterThanOrEqualTo(30);
        for (Modele x : regles.modeles()) {
            assertThat(sensible.matcher(x.titre()).find()).as(x.code()).isFalse();
            assertThat(sensible.matcher(x.sousTitre()).find()).as(x.code()).isFalse();
            assertThat(x.code()).startsWith("SBX_");
        }
        assertThat(regles.modeles().stream().map(Modele::portee)).contains(Portee.INSTITUTION, Portee.GIMAC, Portee.MIXTE);
    }
}
