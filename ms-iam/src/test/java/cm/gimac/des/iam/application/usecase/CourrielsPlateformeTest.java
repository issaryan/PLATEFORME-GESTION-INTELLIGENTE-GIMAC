package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.infrastructure.courriel.CourrielsEnMemoire;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.Courriel;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Gabarits des courriels de la plateforme (D-65) : langue du compte (fr, en, es), français à défaut, HTML échappé. */
class CourrielsPlateformeTest {

    static Compte compte(String langue, String prenom) {
        return new Compte("id", "agent", prenom, "<Mballa>", "awa@gimac.test", true, true, null, null, null, langue, null, true, Set.of(), List.of(), true);
    }

    @Test
    @DisplayName("Langue du compte, français pour une langue absente ou inconnue ; durées en heures ou en minutes")
    void langues() {
        Courriel en = CourrielsPlateforme.composer(compte("en", "Awa"), CourrielsPlateforme.Modele.INVITATION, "https://des.gimac.test/x", Duration.ofHours(24));
        assertThat(en.objet()).isEqualTo("Your account on the GIMAC Moabi platform");
        assertThat(en.texte()).contains("24 hours").contains("Hello Awa <Mballa>,");
        assertThat(en.html()).contains("lang=\"en\"").contains("&lt;Mballa&gt;").doesNotContain("<Mballa>");
        Courriel es = CourrielsPlateforme.composer(compte("es", "Awa"), CourrielsPlateforme.Modele.REINITIALISATION, "https://des.gimac.test/x", Duration.ofMinutes(90));
        assertThat(es.texte()).contains("90 minutos");
        assertThat(CourrielsPlateforme.composer(compte(null, null), CourrielsPlateforme.Modele.ACTIONS, "https://des.gimac.test/connexion", null).objet()).isEqualTo("Actions demandées sur votre compte");
        assertThat(CourrielsPlateforme.composer(compte("de", "Awa"), CourrielsPlateforme.Modele.ACTIONS, "l", null).html()).contains("lang=\"fr\"");
        assertThat(CourrielsPlateforme.langue("es")).isEqualTo("es");
    }

    @Test
    @DisplayName("Informations de sécurité (mon-compte.md §2, §3) : mot de passe modifié, nouvel appareil ; avertissement propre ; fr, en, es")
    void informationsSecurite() {
        Courriel mdp = CourrielsPlateforme.composer(compte("fr", "Awa"), CourrielsPlateforme.Modele.MOT_DE_PASSE_MODIFIE, "https://des.gimac.test/des/mon-compte", null);
        assertThat(mdp.objet()).isEqualTo("Votre mot de passe a été modifié");
        assertThat(mdp.texte()).contains("agent").contains("https://des.gimac.test/des/mon-compte").contains("RSSI").doesNotContain("ignorez ce message");
        assertThat(mdp.html()).contains("RSSI");
        CourrielsEnMemoire envoyes = new CourrielsEnMemoire();
        CourrielsPlateforme c = new CourrielsPlateforme(envoyes);
        c.alerterNouvelAppareil(compte("en", "Awa"), "https://des.gimac.test/des/mon-compte", "Firefox 131", "Windows 10", "41.202.10.20", "01/10/2026 08:30");
        Courriel en = envoyes.dernierPour("awa@gimac.test").orElseThrow();
        assertThat(en.objet()).isEqualTo("New sign-in to your account");
        assertThat(en.texte()).contains("Firefox 131 on Windows 10, IP address 41.202.10.20, on 01/10/2026 08:30 (UTC)").contains("CISO");
        c.alerterNouvelAppareil(compte("es", "Awa"), "l", "Safari 17", "iOS 17", "41.202.10.20", "01/10/2026 08:30");
        assertThat(envoyes.dernierPour("awa@gimac.test").orElseThrow().texte()).contains("Safari 17 en iOS 17");
        c.alerterNouvelAppareil(compte("fr", "Awa"), "l", "Chrome 128", "Android 14", "41.202.10.20", "01/10/2026 08:30");
        assertThat(envoyes.dernierPour("awa@gimac.test").orElseThrow().texte()).contains("Chrome 128 sur Android 14, adresse IP 41.202.10.20");
    }

    @Test
    @DisplayName("D-94 : couche du compte choisie par les permissions de la console, jamais par un nom de profil ; lien vers « Mon compte »")
    void lienMonCompte() {
        var p = AdministrationComptesServiceTest.proprietes();
        var compositions = java.util.Map.of(
            "ROLE_GARDIEN", Set.of("PLATFORM_OPS_READ"),
            "ROLE_SUPER_ADMIN", Set.of("HOM_LIAISON_READ"),
            "ROLE_AUDIT", Set.of("IAM_ACCOUNT_READ")
        );
        assertThat(CatalogueProfils.couche(compte("fr", "A"), compositions, p)).isEqualTo(cm.gimac.des.iam.domain.compte.Couche.DES);
        // Un profil créé quelconque qui reçoit une permission de la console mène à la console.
        Compte gardien = new Compte("id", "gardien", "A", "B", "g@gimac.test", true, true, null, null, null, "fr", null, true, Set.of("ROLE_GARDIEN"), List.of(), true);
        assertThat(CatalogueProfils.couche(gardien, compositions, p)).isEqualTo(cm.gimac.des.iam.domain.compte.Couche.CONSOLE);
        // Le profil livré ROLE_SUPER_ADMIN privé de toute permission de la console mène au département.
        Compte superAdmin = new Compte("id", "admin", "A", "B", "a@gimac.test", true, true, null, null, null, "fr", null, true, Set.of("ROLE_SUPER_ADMIN"), List.of(), true);
        assertThat(CatalogueProfils.couche(superAdmin, compositions, p)).isEqualTo(cm.gimac.des.iam.domain.compte.Couche.DES);
        // Liste configurable : IAM_ACCOUNT_READ mène à la console par défaut, au département si la liste ne la porte pas.
        Compte audit = new Compte("id", "audit", "A", "B", "x@gimac.test", true, true, null, null, null, "fr", null, true, Set.of("ROLE_AUDIT"), List.of(), true);
        assertThat(CatalogueProfils.couche(audit, compositions, p)).isEqualTo(cm.gimac.des.iam.domain.compte.Couche.CONSOLE);
        var l = p.liens();
        var restreinte = new cm.gimac.des.iam.application.IamProprietes(p.keycloak(), p.premiereConnexion(), p.profils(), p.webauthn(),
            new cm.gimac.des.iam.application.IamProprietes.Liens(l.des(), l.portail(), l.console(), null, null, null, Set.of("PLATFORM_OPS_READ")), p.connexion(), p.chiffrement());
        assertThat(CatalogueProfils.couche(audit, compositions, restreinte)).isEqualTo(cm.gimac.des.iam.domain.compte.Couche.DES);
        assertThat(CatalogueProfils.couche(gardien, compositions, restreinte)).isEqualTo(cm.gimac.des.iam.domain.compte.Couche.CONSOLE);
        Compte correspondant = new Compte("id", "corr", "A", "B", "c@gimac.test", true, true, null, null, null, "fr", "INST-1", true, Set.of("ROLE_GARDIEN"), List.of(), true);
        assertThat(CatalogueProfils.couche(correspondant, compositions, p)).isEqualTo(cm.gimac.des.iam.domain.compte.Couche.PORTAIL);
        assertThat(CourrielsPlateforme.lienMonCompte(p, cm.gimac.des.iam.domain.compte.Couche.PORTAIL)).endsWith("/portail/mon-compte").startsWith(p.liens().adresse(cm.gimac.des.iam.domain.compte.Couche.PORTAIL));
    }
}
