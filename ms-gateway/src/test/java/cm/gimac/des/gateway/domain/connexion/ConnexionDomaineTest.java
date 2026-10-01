package cm.gimac.des.gateway.domain.connexion;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.live.Couche;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Règles de la connexion (D-65) : étapes du premier accès, couche, masquage, causes et modèles. */
class ConnexionDomaineTest {

    @Test
    @DisplayName("Étapes : ordre de l'assistant, SECOND_FACTEUR ajouté sans code configuré, autres actions à la fin")
    void etapes() {
        assertThat(EtapesPremierAcces.de(new EtatCompte(true, true, List.of()))).isEmpty();
        assertThat(EtapesPremierAcces.de(new EtatCompte(true, false, null))).containsExactly("SECOND_FACTEUR");
        assertThat(EtapesPremierAcces.de(new EtatCompte(true, false, List.of("CHARTE", "VERIFIER_COURRIEL", "FICHE", "MOT_DE_PASSE", " ")))).containsExactly(
            "MOT_DE_PASSE",
            "SECOND_FACTEUR",
            "FICHE",
            "CHARTE",
            "VERIFIER_COURRIEL"
        );
        assertThat(EtapesPremierAcces.de(new EtatCompte(true, true, Arrays.asList("SECOND_FACTEUR", null, "SECOND_FACTEUR")))).containsExactly("SECOND_FACTEUR");
    }

    @Test
    @DisplayName("Couche : institution_id n'ouvre que le portail, son absence le département et la console")
    void couche() {
        assertThat(RegleCouche.autorise(Couche.PORTAIL, "INST-1")).isTrue();
        assertThat(RegleCouche.autorise(Couche.DES, "INST-1")).isFalse();
        assertThat(RegleCouche.autorise(Couche.CONSOLE, "INST-1")).isFalse();
        assertThat(RegleCouche.autorise(Couche.PORTAIL, "")).isFalse();
        assertThat(RegleCouche.autorise(Couche.PORTAIL, null)).isFalse();
        assertThat(RegleCouche.autorise(Couche.DES, " ")).isTrue();
        assertThat(RegleCouche.autorise(Couche.CONSOLE, null)).isTrue();
    }

    @Test
    @DisplayName("Masquage : deux premiers et dernier caractères, domaine conservé ; identifiant court entièrement masqué")
    void masque() {
        assertThat(MasqueIdentifiant.masquer("Jean.Dupont")).isEqualTo("je***t");
        assertThat(MasqueIdentifiant.masquer(" jean@gimac.org ")).isEqualTo("je***n@gimac.org");
        assertThat(MasqueIdentifiant.masquer("abc")).isEqualTo("***");
        assertThat(MasqueIdentifiant.masquer("ab@x.org")).isEqualTo("***@x.org");
        assertThat(MasqueIdentifiant.masquer(null)).isEmpty();
        assertThat(MasqueIdentifiant.masquer(" ")).isEmpty();
    }

    @Test
    @DisplayName("Causes : statuts et codes du contrat")
    void causes() {
        assertThat(CauseEchec.IDENTIFIANTS_INVALIDES.statut()).isEqualTo(401);
        assertThat(CauseEchec.COUCHE_REFUSEE.code()).isEqualTo("CNX_COUCHE_REFUSEE");
        assertThat(CauseEchec.COMPTE_INACTIF.statut()).isEqualTo(403);
        assertThat(CauseEchec.TROP_DE_TENTATIVES.statut()).isEqualTo(429);
        assertThat(CauseEchec.SESSION_EXPIREE.code()).isEqualTo("CNX_SESSION_EXPIREE");
        assertThat(CauseEchec.SESSION_EXPIREE.message()).isNotBlank();
        EchecConnexion e = new EchecConnexion(CauseEchec.COMPTE_INACTIF);
        assertThat(e.causeEchec()).isEqualTo(CauseEchec.COMPTE_INACTIF);
        assertThat(e.getMessage()).isEqualTo(CauseEchec.COMPTE_INACTIF.message());
        assertThat(new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "détail").getMessage()).isEqualTo("détail");
    }

    @Test
    @DisplayName("Modèles : aucun jeton ni mot de passe dans toString ; claims lus dans l'identité puis l'accès")
    void modeles() {
        Jetons j = new Jetons("eyJacces", "rt-1", "eyJid", 300, "openid");
        assertThat(j.toString()).doesNotContain("eyJ").doesNotContain("rt-1");
        Identite id = new Identite("eyJid", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), Map.of("sub", "u-1"), Map.of("institution_id", "INST-1", "sub", "u-1"));
        assertThat(id.sujet()).isEqualTo("u-1");
        assertThat(id.texte("institution_id")).isEqualTo("INST-1");
        assertThat(id.texte("absent")).isEmpty();
        assertThat(id.toString()).doesNotContain("eyJ");
        DemandeConnexion d = new DemandeConnexion("agent.sna", "secret", " ", Couche.DES, "10.0.0.1");
        assertThat(d.avecCode()).isFalse();
        assertThat(new DemandeConnexion("a", "b", "123456", Couche.DES, "ip").avecCode()).isTrue();
        assertThat(new DemandeConnexion("a", "b", null, Couche.DES, "ip").avecCode()).isFalse();
        assertThat(d.toString()).doesNotContain("secret").contains("ag***a");
        assertThat(ClientConnexion.MOT_DE_PASSE.enregistrement()).isEqualTo("connexion");
        assertThat(ClientConnexion.DEUX_FACTEURS.enregistrement()).isEqualTo("oidc");
        assertThat(new ResultatConnexion.PremierAcces(List.of("FICHE")).etapes()).containsExactly("FICHE");
    }
}
