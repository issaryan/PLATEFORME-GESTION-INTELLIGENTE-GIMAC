package cm.gimac.des.gateway.service.connexion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.ClientConnexion;
import cm.gimac.des.gateway.domain.connexion.DemandeConnexion;
import cm.gimac.des.gateway.domain.connexion.EchecConnexion;
import cm.gimac.des.gateway.domain.connexion.ResultatConnexion;
import cm.gimac.des.gateway.domain.connexion.port.AnnuaireComptes;
import cm.gimac.des.gateway.domain.connexion.port.FournisseurIdentite;
import cm.gimac.des.gateway.domain.connexion.port.JournalConnexion;
import cm.gimac.des.gateway.domain.connexion.port.LimiteurTentatives;
import cm.gimac.des.gateway.domain.live.Couche;
import cm.gimac.des.gateway.security.session.OuvertureSession;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Déroulé de la connexion (D-65 §2) : limitation, pannes inattendues, mot de passe oublié. */
class ServiceConnexionTest {

    FournisseurIdentite fi = mock(FournisseurIdentite.class);
    AnnuaireComptes annuaire = mock(AnnuaireComptes.class);
    LimiteurTentatives limiteur = mock(LimiteurTentatives.class);
    JournalConnexion journal = mock(JournalConnexion.class);
    ServiceConnexion service = new ServiceConnexion(fi, annuaire, limiteur, journal, mock(OuvertureSession.class));
    DemandeConnexion demande = new DemandeConnexion("agent", "mdp", null, Couche.DES, "10.0.0.1");

    @BeforeEach
    void admettre() {
        when(limiteur.consommer(anyString())).thenReturn(Mono.just(true));
    }

    @Test
    @DisplayName("Limite de l'adresse IP atteinte : 429 sans consulter l'identifiant ni Keycloak")
    void limiteAdresse() {
        when(limiteur.consommer("cnx:ip:10.0.0.1")).thenReturn(Mono.just(false));
        StepVerifier.create(service.connecter(demande, null))
            .expectErrorSatisfies(e -> assertThat(((EchecConnexion) e).causeEchec()).isEqualTo(CauseEchec.TROP_DE_TENTATIVES))
            .verify();
        verify(limiteur, never()).consommer(org.mockito.ArgumentMatchers.startsWith("cnx:id:"));
        verify(journal).echouee(eq(demande), eq(CauseEchec.TROP_DE_TENTATIVES), anyString());
    }

    @Test
    @DisplayName("Panne inattendue : SERVICE_INDISPONIBLE, journalisée")
    void panne() {
        when(fi.octroyer(eq(ClientConnexion.MOT_DE_PASSE), anyString(), anyString(), isNull())).thenReturn(Mono.error(new IllegalStateException("boum")));
        StepVerifier.create(service.connecter(demande, null))
            .expectErrorSatisfies(e -> assertThat(((EchecConnexion) e).causeEchec()).isEqualTo(CauseEchec.SERVICE_INDISPONIBLE))
            .verify();
        verify(journal).echouee(eq(demande), eq(CauseEchec.SERVICE_INDISPONIBLE), anyString());
    }

    @Test
    @DisplayName("Mot de passe oublié : identifiant vide ignoré, limite atteinte ou MS-IAM en panne sans erreur")
    void motDePasseOublie() {
        StepVerifier.create(service.motDePasseOublie(" ", "des", "ip")).verifyComplete();
        StepVerifier.create(service.motDePasseOublie(null, "des", "ip")).verifyComplete();
        when(annuaire.motDePasseOublie(anyString(), any())).thenReturn(Mono.error(new IllegalStateException("panne")));
        StepVerifier.create(service.motDePasseOublie(" agent ", "PORTAIL", "ip")).verifyComplete();
        verify(annuaire).motDePasseOublie("agent", "portail");
        when(limiteur.consommer("oubli:ip:ip2")).thenReturn(Mono.just(false));
        StepVerifier.create(service.motDePasseOublie("agent", "inconnue", "ip2")).verifyComplete();
        verify(annuaire, org.mockito.Mockito.times(1)).motDePasseOublie(anyString(), any());
    }

    @Test
    @DisplayName("Nouveau mot de passe : réponse de MS-IAM relayée")
    void nouveauMotDePasse() {
        when(annuaire.nouveauMotDePasse("j", "m")).thenReturn(Mono.just(new AnnuaireComptes.ReponseRelayee(204, null, "")));
        assertThat(service.nouveauMotDePasse("j", "m").block().statut()).isEqualTo(204);
    }

    @Test
    @DisplayName("Empreinte de l'identifiant (clé de limitation) et nom des états")
    void outils() {
        assertThat(ServiceConnexion.empreinte(" Agent ")).isEqualTo(ServiceConnexion.empreinte("agent")).hasSize(32);
        assertThat(ServiceConnexion.etat(new ResultatConnexion.CodeRequis())).isEqualTo("CODE_REQUIS");
        assertThat(ServiceConnexion.etat(new ResultatConnexion.PremierAcces(List.of()))).isEqualTo("PREMIER_ACCES");
        assertThat(ServiceConnexion.etat(new ResultatConnexion.Connecte())).isEqualTo("CONNECTE");
    }
}
