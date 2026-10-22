package cm.gimac.des.iam.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatPerimetre;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatProtection;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Suspension du périmètre d'une institution et protection du dernier administrateur (D-119, US-EXI-06) sur l'annuaire de
 * substitution, sans Docker : indicateur superposé, sessions révoquées, état des comptes jamais modifié, étapes rejouées
 * sans second effet.
 */
@ExtendWith(MockitoExtension.class)
class PerimetresSandboxServiceTest {

    static final String INST = "99001";
    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");
    static final Acteur SAGA = new Acteur("sa-sbx-espace", "service-account-sbx-espace", Set.of("INTERNAL_IAM_SBX_SAGA"));

    @Mock
    JournalSandbox journal;

    AnnuaireEnMemoire annuaire;
    Memoires.Perimetres perimetres;
    Memoires.Protections protections;
    Memoires.Etapes etapes;
    Memoires.Horloge horloge;
    PerimetresSandboxService service;
    String admin;
    String testeur;
    String suspenduAvant;
    String autre;

    String compte(String identifiant, String institution, String profil) {
        return annuaire.creer(new NouveauCompte(identifiant, "Prénom", identifiant, identifiant + "@institution.test", null, null, institution, Set.of(profil)), List.of());
    }

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(Set.of("ROLE_SBX_ADMIN_INSTITUTION", "ROLE_SBX_TESTEUR")));
        perimetres = new Memoires.Perimetres();
        protections = new Memoires.Protections();
        etapes = new Memoires.Etapes();
        horloge = new Memoires.Horloge(T);
        service = new PerimetresSandboxService(perimetres, protections, annuaire, etapes, journal, horloge);
        admin = compte("admin.99001", INST, "ROLE_SBX_ADMIN_INSTITUTION");
        testeur = compte("testeur.99001", INST, "ROLE_SBX_TESTEUR");
        suspenduAvant = compte("ancien.99001", INST, "ROLE_SBX_TESTEUR");
        annuaire.definirActif(suspenduAvant, false);
        autre = compte("testeur.99002", "99002", "ROLE_SBX_TESTEUR");
        for (String id : List.of(admin, testeur, autre)) {
            annuaire.ouvrirSession(id, "10.0.0.1");
        }
    }

    @Test
    @DisplayName("US-EXI-06 scénario 1 : la saga SUSPENSION pose l'indicateur et révoque les sessions sans modifier l'état des comptes")
    void suspensionDuPerimetre() {
        EtatPerimetre e = service.suspendre(SAGA, INST, true, "Statut SUSPENDU au registre", "saga-1/3");
        assertThat(e.suspendu()).isTrue();
        assertThat(e.depuis()).isEqualTo(T);
        assertThat(e.rejouee()).isFalse();
        assertThat(perimetres.suspendu(INST)).isTrue();
        assertThat(annuaire.sessions(admin)).isEmpty();
        assertThat(annuaire.sessions(testeur)).isEmpty();
        assertThat(annuaire.sessions(autre)).hasSize(1);
        assertThat(annuaire.lire(admin).orElseThrow().actif()).isTrue();
        assertThat(annuaire.lire(suspenduAvant).orElseThrow().actif()).isFalse();
        verify(journal).objet(eq(SAGA), eq("PERIMETRE_SUSPENDU"), eq(INST), eq(INST), eq("Statut SUSPENDU au registre"), any());
        verify(journal).compte(eq(SAGA), eq("PERIMETRE_SUSPENDU"), eq(testeur), any(), any(), eq(List.of(INST)));
        verify(journal, never()).compte(any(), any(), eq(autre), any(), any(), any());
    }

    @Test
    @DisplayName("US-EXI-06 scénario 2 : le RETABLISSEMENT lève l'indicateur ; le compte suspendu avant la suspension le reste")
    void retablissement() {
        service.suspendre(SAGA, INST, true, null, "saga-1/3");
        EtatPerimetre e = service.suspendre(SAGA, INST, false, "Décision de l'administrateur GIMAC", "saga-2/1");
        assertThat(e.suspendu()).isFalse();
        assertThat(e.depuis()).isNull();
        assertThat(perimetres.suspendu(INST)).isFalse();
        assertThat(annuaire.lire(admin).orElseThrow().actif()).isTrue();
        assertThat(annuaire.lire(testeur).orElseThrow().actif()).isTrue();
        assertThat(annuaire.lire(suspenduAvant).orElseThrow().actif()).isFalse();
        verify(journal).objet(eq(SAGA), eq("PERIMETRE_RETABLI"), eq(INST), eq(INST), any(), any());
        // Rétablir un périmètre ouvert : sans effet ni trace.
        service.suspendre(SAGA, INST, false, null, "saga-3/1");
        verify(journal, times(1)).objet(eq(SAGA), eq("PERIMETRE_RETABLI"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("US-EXI-06 scénario 4 : l'étape reprise après indisponibilité avec la même clé pose l'indicateur une seule fois")
    void repriseIdempotente() {
        service.suspendre(SAGA, INST, true, null, " saga-1/3 ");
        horloge.avancer(Duration.ofMinutes(5));
        annuaire.ouvrirSession(admin, "10.0.0.2");
        EtatPerimetre rejeu = service.suspendre(SAGA, INST, true, null, "saga-1/3");
        assertThat(rejeu.rejouee()).isTrue();
        assertThat(rejeu.depuis()).isEqualTo(T);
        assertThat(etapes.lignes).containsOnlyKeys("saga-1/3/PERIMETRE_SUSPENSION");
        assertThat(annuaire.sessions(admin)).hasSize(1);
        verify(journal, times(1)).objet(any(), eq("PERIMETRE_SUSPENDU"), any(), any(), any(), any());
        // Nouvelle clé sur un périmètre déjà suspendu : sessions de nouveau révoquées, indicateur inchangé, aucune trace.
        EtatPerimetre encore = service.suspendre(SAGA, INST, true, null, null);
        assertThat(encore.sessionsRevoquees()).isEqualTo(3);
        assertThat(encore.depuis()).isEqualTo(T);
        assertThat(annuaire.sessions(admin)).isEmpty();
        verify(journal, times(1)).objet(any(), eq("PERIMETRE_SUSPENDU"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Clé d'idempotence vide : étape appliquée sans réservation")
    void cleVide() {
        assertThat(service.suspendre(SAGA, INST, true, null, "  ").suspendu()).isTrue();
        assertThat(etapes.lignes).isEmpty();
    }

    @Test
    @DisplayName("Code de périmètre invalide : 400 avant tout effet")
    void codeInvalide() {
        assertThatThrownBy(() -> service.suspendre(SAGA, "99 001", true, null, "k")).isInstanceOf(IamException.class);
        assertThat(etapes.lignes).isEmpty();
        assertThatThrownBy(() -> service.lire("x/y")).isInstanceOf(IamException.class);
    }

    @Test
    @DisplayName("Console : périmètres suspendus paginés (100 au plus) et état d'un périmètre")
    void lecture() {
        service.suspendre(SAGA, INST, true, "m", null);
        horloge.avancer(Duration.ofHours(1));
        service.suspendre(SAGA, "99002", true, "m", null);
        var page = service.suspendus(-1, 500);
        assertThat(page.page()).isZero();
        assertThat(page.taille()).isEqualTo(100);
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.contenu()).extracting(x -> x.code()).containsExactly("99002", INST);
        assertThat(service.suspendus(1, 1).contenu()).extracting(x -> x.code()).containsExactly(INST);
        assertThat(service.suspendus(0, 0).taille()).isEqualTo(1);
        assertThat(service.lire(INST).suspendu()).isTrue();
        assertThat(service.lire("99003").suspendu()).isFalse();
    }

    @Test
    @DisplayName("Protection du dernier administrateur : posée et levée par la saga, rejouée sans effet, comptes d'institution seulement")
    void protection() {
        EtatProtection p = service.proteger(SAGA, admin, true, "saga-9/2");
        assertThat(p.protege()).isTrue();
        assertThat(p.institutionId()).isEqualTo(INST);
        assertThat(p.rejouee()).isFalse();
        assertThat(service.proteger(SAGA, admin, true, "saga-9/2").rejouee()).isTrue();
        verify(journal, times(1)).compte(eq(SAGA), eq("PROTECTION_DERNIER_ADMINISTRATEUR_POSEE"), eq(admin), any(), any(), eq(List.of(INST)));
        var page = service.protections(0, 20);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.contenu().get(0).nom()).isEqualTo("Prénom admin.99001");
        assertThat(page.contenu().get(0).depuis()).isEqualTo(T);
        assertThat(service.protections(-3, 1000).taille()).isEqualTo(100);
        // Pose répétée avec une autre clé : sans changement, sans trace.
        service.proteger(SAGA, admin, true, "saga-9/3");
        verify(journal, times(1)).compte(any(), eq("PROTECTION_DERNIER_ADMINISTRATEUR_POSEE"), any(), any(), any(), any());
        assertThat(service.proteger(SAGA, admin, false, null).protege()).isFalse();
        verify(journal).compte(eq(SAGA), eq("PROTECTION_DERNIER_ADMINISTRATEUR_LEVEE"), eq(admin), any(), any(), any());
        assertThatThrownBy(() -> service.proteger(SAGA, "demo-rssi", true, null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.status()).isEqualTo(422));
        assertThatThrownBy(() -> service.proteger(SAGA, "inconnu", true, null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.status()).isEqualTo(404));
    }

    @Test
    @DisplayName("Compte protégé supprimé de l'annuaire : la console affiche son identifiant")
    void protectionCompteDisparu() {
        protections.definir("disparu", INST, true, "sa", T);
        assertThat(service.protections(0, 20).contenu().get(0).nom()).isEqualTo("disparu");
    }
}
