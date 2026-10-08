package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Courriel;
import cm.gimac.des.iam.domain.compte.EtatConnexion;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.service.Totp;
import cm.gimac.des.iam.infrastructure.courriel.CourrielsEnMemoire;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import cm.gimac.des.iam.infrastructure.persistence.JetonLien;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Connexion sur la plateforme (D-65, connexion.md §3.1) : état de connexion, mot de passe oublié sans
 * révéler l'existence d'un compte, nouveau mot de passe par un lien à usage unique (30 min ou 24 h).
 */
@ExtendWith(MockitoExtension.class)
class ConnexionServiceTest {

    static final Instant DEBUT = Instant.parse("2026-09-30T08:00:00Z");

    @Mock
    JournalActes journal;

    AnnuaireEnMemoire annuaire;
    CourrielsEnMemoire courriels;
    List<JetonLien> jetons;
    AtomicReference<Instant> maintenant;
    ConnexionService service;
    String agent;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(Set.of("ROLE_SNA")));
        courriels = new CourrielsEnMemoire();
        jetons = new ArrayList<>();
        maintenant = new AtomicReference<>(DEBUT);
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return maintenant.get();
            }
        };
        LiensCourriel liens = new LiensCourriel(JetonsEnMemoire.creer(jetons), new CourrielsPlateforme(courriels), AdministrationComptesServiceTest.proprietes(), clock);
        service = new ConnexionService(annuaire, liens, journal);
        agent = annuaire.creer(new NouveauCompte("agent.sna", "Awa", "Mballa", "awa@gimac.test", null, null, null, Set.of("ROLE_SNA")), List.of(ActionRequise.MOT_DE_PASSE, ActionRequise.FICHE, ActionRequise.CHARTE));
    }

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    String jetonRecu() {
        Courriel c = courriels.dernierPour("awa@gimac.test").orElseThrow();
        return JetonsEnMemoire.jeton(c.texte()).orElseThrow();
    }

    @Test
    @DisplayName("État de connexion : actif, second facteur, actions exigées dans l'ordre de l'assistant ; compte inconnu 404")
    void etat() {
        EtatConnexion e = service.etat(agent);
        assertThat(e.actif()).isTrue();
        assertThat(e.secondFacteur()).isFalse();
        assertThat(e.actionsRequises()).containsExactly(ActionRequise.MOT_DE_PASSE, ActionRequise.SECOND_FACTEUR, ActionRequise.FICHE, ActionRequise.CHARTE);
        EtatConnexion configure = service.etat("demo-rssi");
        assertThat(configure.secondFacteur()).isTrue();
        assertThat(configure.actionsRequises()).isEmpty();
        refus(() -> service.etat("inconnu"), "IAM_ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("Mot de passe oublié : courriel par identifiant ou par adresse, rien pour un compte inconnu, inactif ou sans adresse")
    void motDePasseOublie() {
        service.motDePasseOublie("inconnu", "des");
        service.motDePasseOublie(" ", "des");
        service.motDePasseOublie(null, "des");
        service.motDePasseOublie("personne@gimac.test", "des");
        assertThat(courriels.envoyes()).isEmpty();

        service.motDePasseOublie(" AGENT.SNA ", "console");
        Courriel c = courriels.dernierPour("awa@gimac.test").orElseThrow();
        assertThat(c.objet()).isEqualTo("Réinitialisation de votre mot de passe");
        assertThat(c.texte()).contains("https://console.gimac.test/connexion/nouveau-mot-de-passe?jeton=").contains("30 minutes");
        assertThat(jetons).singleElement().satisfies(j -> {
            assertThat(j.getFinalite()).isEqualTo("REINITIALISATION");
            assertThat(j.getExpireLe()).isEqualTo(DEBUT.plusSeconds(1800));
            assertThat(j.getCouche()).isEqualTo("CONSOLE");
        });
        verify(journal).compte(any(), eq("MOT_DE_PASSE_OUBLIE"), eq(agent), eq("agent.sna"), isNull(), eq(List.of("CONSOLE")));

        service.motDePasseOublie("Awa@Gimac.test", "portail");
        assertThat(courriels.envoyes()).hasSize(2);
        assertThat(courriels.envoyes().get(1).texte()).contains("https://des.gimac.test/connexion/");
        assertThat(jetons.get(0).getUtiliseLe()).isNotNull();

        annuaire.definirActif(agent, false);
        service.motDePasseOublie("agent.sna", "des");
        assertThat(courriels.envoyes()).hasSize(2);
    }

    @Test
    @DisplayName("Mot de passe oublié : panne de la messagerie tracée, jamais révélée")
    void messagerieEnPanne() {
        LiensCourriel liens = new LiensCourriel(
            JetonsEnMemoire.creer(jetons),
            new CourrielsPlateforme(c -> {
                throw IamException.indisponible("IAM_SMTP_FAILED", "refus");
            }),
            AdministrationComptesServiceTest.proprietes(),
            Clock.fixed(DEBUT, ZoneOffset.UTC)
        );
        new ConnexionService(annuaire, liens, journal).motDePasseOublie("agent.sna", "des");
        verify(journal).compte(any(), eq("MOT_DE_PASSE_OUBLIE_ECHEC"), eq(agent), eq("agent.sna"), isNull(), eq(List.of("IAM_SMTP_FAILED")));
    }

    @Test
    @DisplayName("Nouveau mot de passe : politique contrôlée sans consommer le lien, puis usage unique, courriel vérifié, actions retirées")
    void nouveauMotDePasse() {
        service.motDePasseOublie("agent.sna", "des");
        String jeton = jetonRecu();
        annuaire.ouvrirSession(agent, "10.0.0.1");
        assertThatThrownBy(() -> service.nouveauMotDePasse(jeton, "court")).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo("IAM_PASSWORD_POLICY");
            assertThat(e.details().get("violations")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).contains("LONGUEUR", "MAJUSCULE");
        });
        refus(() -> service.nouveauMotDePasse(jeton, "Agent.Sna-2026!"), "IAM_PASSWORD_POLICY");
        service.nouveauMotDePasse(jeton, "Nouveau-Mot-2026!");
        assertThat(annuaire.verifierMotDePasse("agent.sna", "Nouveau-Mot-2026!")).isTrue();
        var c = annuaire.lire(agent).orElseThrow();
        assertThat(c.actionsRequises()).doesNotContain(ActionRequise.MOT_DE_PASSE);
        assertThat(c.emailVerifie()).isTrue();
        assertThat(annuaire.sessions(agent)).isEmpty();
        verify(journal).compte(any(), eq("NOUVEAU_MOT_DE_PASSE"), eq(agent), eq("agent.sna"), isNull(), eq(List.of("REINITIALISATION")));
        refus(() -> service.nouveauMotDePasse(jeton, "Autre-Mot-2026!"), "CNX_JETON_INVALIDE");
    }

    @Test
    @DisplayName("Nouveau mot de passe : jeton mal formé, inconnu, expiré ou d'un compte désactivé refusé (CNX_JETON_INVALIDE)")
    void jetonsRefuses() {
        refus(() -> service.nouveauMotDePasse(null, "Nouveau-Mot-2026!"), "CNX_JETON_INVALIDE");
        refus(() -> service.nouveauMotDePasse("court", "Nouveau-Mot-2026!"), "CNX_JETON_INVALIDE");
        refus(() -> service.nouveauMotDePasse("A".repeat(43), "Nouveau-Mot-2026!"), "CNX_JETON_INVALIDE");
        service.motDePasseOublie("agent.sna", "des");
        String jeton = jetonRecu();
        maintenant.set(DEBUT.plusSeconds(1801));
        refus(() -> service.nouveauMotDePasse(jeton, "Nouveau-Mot-2026!"), "CNX_JETON_INVALIDE");
        maintenant.set(DEBUT);
        annuaire.definirActif(agent, false);
        refus(() -> service.nouveauMotDePasse(jeton, "Nouveau-Mot-2026!"), "CNX_JETON_INVALIDE");
        verify(journal, never()).compte(any(), eq("NOUVEAU_MOT_DE_PASSE"), any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("Invitation (24 h) : le lien fixe le premier mot de passe sans fermer de session ; le TOTP reste à configurer")
    void invitation() {
        var liens = new LiensCourriel(JetonsEnMemoire.creer(jetons), new CourrielsPlateforme(courriels), AdministrationComptesServiceTest.proprietes(), Clock.fixed(DEBUT, ZoneOffset.UTC));
        liens.envoyer(annuaire.lire(agent).orElseThrow(), cm.gimac.des.iam.domain.compte.FinaliteJeton.INVITATION, cm.gimac.des.iam.domain.compte.Couche.DES);
        annuaire.ouvrirSession(agent, "10.0.0.1");
        service.nouveauMotDePasse(jetonRecu(), "Premier-Mot-2026!");
        assertThat(annuaire.sessions(agent)).hasSize(1);
        assertThat(service.etat(agent).actionsRequises()).containsExactly(ActionRequise.SECOND_FACTEUR, ActionRequise.FICHE, ActionRequise.CHARTE);
        assertThat(Totp.base32(new byte[] { 1 })).isEqualTo("AE");
    }

    @Test
    @DisplayName("mon-compte.md §3 : alerte « nouvel appareil » au titulaire actif, dans sa couche, tracée ; rien pour un compte inactif ; 404 sinon")
    void alerteNouvelAppareil() {
        var connexion = new ConnexionService.NouvelleConnexion("Firefox 131", "Windows 10", "ORDINATEUR", "41.202.10.20", "des", Instant.parse("2026-10-01T08:30:00Z"));
        assertThat(service.alerterNouvelAppareil(agent, connexion)).isTrue();
        Courriel c = courriels.dernierPour("awa@gimac.test").orElseThrow();
        assertThat(c.objet()).isEqualTo("Nouvelle connexion à votre compte");
        assertThat(c.texte()).contains("Firefox 131 sur Windows 10, adresse IP 41.202.10.20, le 01/10/2026 08:30 (UTC)").contains("/des/mon-compte");
        org.mockito.Mockito.verify(journal).compte(any(), org.mockito.ArgumentMatchers.eq("ALERTE_NOUVEL_APPAREIL"), org.mockito.ArgumentMatchers.eq(agent), org.mockito.ArgumentMatchers.eq("agent.sna"), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(List.of("Firefox 131 / Windows 10", "DES")));
        // Valeurs absentes : remplacées, jamais « null ».
        assertThat(service.alerterNouvelAppareil(agent, new ConnexionService.NouvelleConnexion(null, " ", null, null, null, null))).isTrue();
        assertThat(courriels.dernierPour("awa@gimac.test").orElseThrow().texte()).contains("? sur ?, adresse IP ?").doesNotContain("null");
        annuaire.definirActif(agent, false);
        int avant = courriels.envoyes().size();
        assertThat(service.alerterNouvelAppareil(agent, connexion)).isFalse();
        assertThat(courriels.envoyes()).hasSize(avant);
        refus(() -> service.alerterNouvelAppareil("inconnu", connexion), "IAM_ACCOUNT_NOT_FOUND");
    }
}
