package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService;
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService.VueDelegation;
import cm.gimac.des.iam.application.usecase.sandbox.JournalSandbox;
import cm.gimac.des.iam.application.usecase.sandbox.Memoires;
import cm.gimac.des.iam.application.usecase.sandbox.ProprietesSandbox;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatEffectif;
import cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation;
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
 * Mandats de partenaire sur les délégations temporaires de MS-IAM (D-119, US-EXI-04), sans Docker : octroi borné,
 * révocation immédiate, prolongation, étapes rejouées sans double effet.
 */
@ExtendWith(MockitoExtension.class)
class DelegationsSandboxServiceTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");
    static final Acteur SAGA = new Acteur("sa-sbx-espace", "service-account-sbx-espace", Set.of("INTERNAL_IAM_SBX_SAGA"));

    @Mock
    JournalSandbox journal;

    AnnuaireEnMemoire annuaire;
    Memoires.Delegations delegations;
    Memoires.Perimetres perimetres;
    Memoires.Etapes etapes;
    Memoires.Horloge horloge;
    DelegationsSandboxService service;
    String partenaire;
    String membre;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(DesactivationInactifsServiceTest.proprietes().profils().attribuables()));
        for (String p : DesactivationInactifsServiceTest.proprietes().profils().externesInitiaux()) {
            annuaire.definirExterne(p, true);
        }
        delegations = new Memoires.Delegations();
        perimetres = new Memoires.Perimetres();
        etapes = new Memoires.Etapes();
        horloge = new Memoires.Horloge(T);
        CatalogueProfils catalogue = new CatalogueProfils(DesactivationInactifsServiceTest.proprietes(), new DemandesEtProfilsServiceTest.ProfilsCreesEnMemoire(), annuaire);
        service = new DelegationsSandboxService(delegations, perimetres, annuaire, catalogue, etapes, journal, new ProprietesSandbox(90, Duration.ofDays(90), Duration.ofHours(8)), horloge);
        partenaire = annuaire.creer(new NouveauCompte("integrateur", "Paul", "Ebode", "paul@integrateur.test", null, null, "88001", Set.of("ROLE_SBX_TESTEUR")), List.of());
        membre = annuaire.creer(new NouveauCompte("membre", "Awa", "Ndiaye", "awa@99001.test", null, null, "99001", Set.of("ROLE_SBX_TESTEUR")), List.of());
        annuaire.ouvrirSession(partenaire, "10.0.0.9");
    }

    VueDelegation accorder(String cle) {
        return service.accorderMandat(SAGA, partenaire, "99001", List.of("SBX_CER_EXECUTION", "SBX_CER_CONSULTATION"), T, T.plus(Duration.ofDays(30)), cle);
    }

    static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    @Test
    @DisplayName("US-EXI-04 scénario 1 : la délégation porte l'institution mandante, les seuls droits du mandat et ses dates ; rejeu sans second mandat")
    void octroi() {
        VueDelegation v = accorder("saga-m/4");
        assertThat(v.rejouee()).isFalse();
        assertThat(v.etat()).isEqualTo(EtatEffectif.ACTIVE);
        assertThat(v.nomCompte()).isEqualTo("Paul Ebode");
        assertThat(v.delegation().institutionId()).isEqualTo("99001");
        assertThat(v.delegation().permissions()).containsExactlyInAnyOrder("SBX_CER_EXECUTION", "SBX_CER_CONSULTATION");
        assertThat(v.delegation().debut()).isEqualTo(T);
        assertThat(v.delegation().fin()).isEqualTo(T.plus(Duration.ofDays(30)));
        verify(journal).compte(eq(SAGA), eq("MANDAT_ACCORDE"), eq(partenaire), eq("integrateur"), any(), any());
        VueDelegation rejeu = accorder("saga-m/4");
        assertThat(rejeu.rejouee()).isTrue();
        assertThat(rejeu.delegation().id()).isEqualTo(v.delegation().id());
        assertThat(delegations.lignes).hasSize(1);
        // Sans clé : un second mandat.
        accorder(null);
        accorder(" ");
        assertThat(delegations.lignes).hasSize(3);
        assertThat(service.lire(v.delegation().id()).delegation().id()).isEqualTo(v.delegation().id());
        horloge.avancer(Duration.ofDays(31));
        assertThat(service.lire(v.delegation().id()).etat()).isEqualTo(EtatEffectif.ECHUE);
    }

    @Test
    @DisplayName("US-EXI-04 scénario 3 : révocation anticipée, délégation close et sessions du partenaire révoquées immédiatement")
    void revocation() {
        String id = accorder("saga-m/4").delegation().id();
        VueDelegation r = service.revoquer(SAGA, id, "Fin anticipée du contrat", "saga-r/1");
        assertThat(r.etat()).isEqualTo(EtatEffectif.REVOQUEE);
        assertThat(r.delegation().closeLe()).isEqualTo(T);
        assertThat(r.delegation().motif()).isEqualTo("Fin anticipée du contrat");
        assertThat(annuaire.sessions(partenaire)).isEmpty();
        verify(journal).compte(eq(SAGA), eq("DELEGATION_REVOQUEE"), eq(partenaire), any(), eq("Fin anticipée du contrat"), any());
        assertThat(service.revoquer(SAGA, id, null, "saga-r/1").rejouee()).isTrue();
        assertThat(service.revoquer(SAGA, id, null, null).rejouee()).isTrue();
        verify(journal, times(1)).compte(any(), eq("DELEGATION_REVOQUEE"), any(), any(), any(), any());
        refus(() -> service.revoquer(SAGA, "inconnue", null, null), "IAM_SBX_DELEGATION_INTROUVABLE", 404);
        // Titulaire supprimé de l'annuaire : la délégation est close sans session à fermer.
        String id2 = accorder(null).delegation().id();
        annuaire.supprimer(partenaire);
        assertThat(service.revoquer(SAGA, id2, null, null).nomCompte()).isEqualTo(partenaire);
    }

    @Test
    @DisplayName("US-EXI-04 scénario 4 : durée excessive refusée par un problème normalisé, sans état partiel")
    void dureeExcessive() {
        refus(() -> service.accorderMandat(SAGA, partenaire, "99001", List.of("SBX_CER_EXECUTION"), T, T.plus(Duration.ofDays(91)), "saga-x/4"), "IAM_SBX_DUREE_EXCESSIVE", 422);
        assertThat(delegations.lignes).isEmpty();
        assertThat(etapes.lignes).isEmpty();
        verify(journal, never()).compte(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Prolongation : fin reportée sous le même contrôle de durée, rejouée sans effet")
    void prolongation() {
        String id = accorder(null).delegation().id();
        VueDelegation p = service.prolonger(SAGA, id, T.plus(Duration.ofDays(60)), "saga-p/1");
        assertThat(p.delegation().fin()).isEqualTo(T.plus(Duration.ofDays(60)));
        assertThat(service.prolonger(SAGA, id, T.plus(Duration.ofDays(80)), "saga-p/1").rejouee()).isTrue();
        assertThat(delegations.lignes.get(id).fin()).isEqualTo(T.plus(Duration.ofDays(60)));
        refus(() -> service.prolonger(SAGA, id, T.plus(Duration.ofDays(91)), null), "IAM_SBX_DUREE_EXCESSIVE", 422);
        verify(journal).compte(eq(SAGA), eq("DELEGATION_PROLONGEE"), eq(partenaire), any(), any(), any());
    }

    @Test
    @DisplayName("Prolongation d'un accès de prestataire : bornée par la durée maximale des prestataires (8 heures)")
    void prolongationPrestataire() {
        delegations.enregistrer(new cm.gimac.des.iam.domain.sandbox.Delegation("p1", TypeDelegation.PRESTATAIRE, partenaire, null, Set.of(), Set.of("SBX-TRACE"),
            T, T.plus(Duration.ofHours(4)), EtatDelegation.ACCORDEE, "u1", "rssi", null, T, null));
        assertThat(service.prolonger(SAGA, "p1", T.plus(Duration.ofHours(8)), null).delegation().fin()).isEqualTo(T.plus(Duration.ofHours(8)));
        refus(() -> service.prolonger(SAGA, "p1", T.plus(Duration.ofHours(9)), null), "IAM_SBX_DUREE_EXCESSIVE", 422);
    }

    @Test
    @DisplayName("Contrôles de l'octroi : compte inconnu ou inactif, compte de l'institution mandante, périmètre suspendu, droits hors portée")
    void controles() {
        refus(() -> service.accorderMandat(SAGA, "inconnu", "99001", List.of("SBX_CER_EXECUTION"), T, T.plusSeconds(60), null), "IAM_ACCOUNT_NOT_FOUND", 404);
        refus(() -> service.accorderMandat(SAGA, membre, "99001", List.of("SBX_CER_EXECUTION"), T, T.plusSeconds(60), null), "IAM_SBX_MANDAT_PROPRE_INSTITUTION", 422);
        // D-134 : compte de l'institution mandante portant son identifiant au registre, refusé de même.
        java.util.UUID id99001 = java.util.UUID.fromString("6f1c2a3e-6666-4a6b-9c1d-000000000001");
        String membreUuid = annuaire.creer(new NouveauCompte("membre.uuid", "Ama", "N", "ama@99001.test", null, null, id99001.toString(), Set.of("ROLE_SBX_TESTEUR")), List.of());
        var registre = new cm.gimac.des.iam.application.port.out.sandbox.RegistreInstitutions() {
            @Override
            public java.util.Optional<String> codeParticipant(java.util.UUID i) {
                return id99001.equals(i) ? java.util.Optional.of("99001") : java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<java.util.UUID> identifiant(String code) {
                return "99001".equals(code) ? java.util.Optional.of(id99001) : java.util.Optional.empty();
            }
        };
        CatalogueProfils cat = new CatalogueProfils(DesactivationInactifsServiceTest.proprietes(), new DemandesEtProfilsServiceTest.ProfilsCreesEnMemoire(), annuaire);
        DelegationsSandboxService traduit = new DelegationsSandboxService(delegations, perimetres, annuaire, cat, etapes, journal, new ProprietesSandbox(90, Duration.ofDays(90), Duration.ofHours(8)), horloge,
            new cm.gimac.des.iam.application.usecase.sandbox.TraductionInstitution(registre, horloge, Duration.ofMinutes(5)));
        refus(() -> traduit.accorderMandat(SAGA, membreUuid, "99001", List.of("SBX_CER_EXECUTION"), T, T.plusSeconds(60), null), "IAM_SBX_MANDAT_PROPRE_INSTITUTION", 422);
        refus(() -> service.accorderMandat(SAGA, partenaire, "99001", List.of("SBX_CER_VALIDATION"), T, T.plusSeconds(60), null), "IAM_SBX_VALIDATION_RESERVEE", 422);
        refus(() -> service.accorderMandat(SAGA, partenaire, "99001", List.of("SBX_TRA_ADMINISTRATION"), T, T.plusSeconds(60), null), "IAM_SBX_PERIMETRE_HORS_PORTEE", 403);
        refus(() -> service.accorderMandat(SAGA, partenaire, "9 9", List.of("SBX_CER_EXECUTION"), T, T.plusSeconds(60), null), "IAM_SBX_PERIMETRE_INVALIDE", 400);
        perimetres.poser("99001", "sa", null, T);
        refus(() -> service.accorderMandat(SAGA, partenaire, "99001", List.of("SBX_CER_EXECUTION"), T, T.plusSeconds(60), null), "IAM_SBX_PERIMETRE_SUSPENDU", 403);
        annuaire.definirActif(partenaire, false);
        refus(() -> service.accorderMandat(SAGA, partenaire, "99002", List.of("SBX_CER_EXECUTION"), T, T.plusSeconds(60), null), "IAM_SBX_COMPTE_INACTIF", 422);
    }

    @Test
    @DisplayName("Console : délégations paginées (100 au plus), filtre par type")
    void liste() {
        accorder(null);
        horloge.avancer(Duration.ofMinutes(1));
        accorder(null);
        var page = service.lister(TypeDelegation.MANDAT, -1, 500);
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.taille()).isEqualTo(100);
        assertThat(page.contenu().get(0).delegation().creeLe()).isEqualTo(T.plusSeconds(60));
        assertThat(service.lister(null, 0, 0).contenu()).hasSize(1);
        assertThat(service.lister(TypeDelegation.PRESTATAIRE, 0, 20).total()).isZero();
        assertThat(delegations.lignes.values()).allMatch(d -> d.etat() == EtatDelegation.ACCORDEE);
    }

    static final Acteur EXPLOITANT = new Acteur("exploitant", "Exploitant", Set.of("IAM_ACCOUNT_SECURITY"));
    static final Acteur RSSI = new Acteur("rssi", "RSSI", Set.of("IAM_ACCOUNT_SECURITY"));

    String prestataire() {
        return annuaire.creer(new NouveauCompte("presta.maintenance", "Jean", "Mbarga", "jean@presta.test", null, null, null, Set.of()), List.of());
    }

    @Test
    @DisplayName("US-EXI-24 scénario 1 : demande sur SBX-TRACE approuvée par le RSSI pour quatre heures, octroi journalisé")
    void accesPrestataire() {
        String compte = prestataire();
        VueDelegation d = service.demanderAccesPrestataire(EXPLOITANT, compte, List.of("SBX-TRACE"), T, T.plus(Duration.ofHours(4)), "Correctif de SBX-TRACE");
        assertThat(d.etat()).isEqualTo(EtatEffectif.DEMANDEE);
        assertThat(d.delegation().type()).isEqualTo(TypeDelegation.PRESTATAIRE);
        assertThat(d.delegation().institutionId()).isNull();
        assertThat(d.delegation().permissions()).isEmpty();
        assertThat(d.delegation().composants()).containsExactly("SBX-TRACE");
        verify(journal).compte(eq(EXPLOITANT), eq("ACCES_PRESTATAIRE_DEMANDE"), eq(compte), eq("presta.maintenance"), eq("Correctif de SBX-TRACE"), any());
        // Quatre yeux : l'auteur de la demande ne l'approuve pas.
        refus(() -> service.approuverAccesPrestataire(EXPLOITANT, d.delegation().id()), "IAM_SBX_APPROBATION_PROPRE", 403);
        VueDelegation a = service.approuverAccesPrestataire(RSSI, d.delegation().id());
        assertThat(a.etat()).isEqualTo(EtatEffectif.ACTIVE);
        assertThat(a.delegation().approbateurId()).isEqualTo("rssi");
        verify(journal).compte(eq(RSSI), eq("ACCES_PRESTATAIRE_ACCORDE"), eq(compte), eq(compte), eq("Correctif de SBX-TRACE"), any());
        // Demande déjà décidée.
        refus(() -> service.approuverAccesPrestataire(RSSI, d.delegation().id()), "IAM_SBX_DELEGATION_CLOSE", 409);
        refus(() -> service.refuserAccesPrestataire(RSSI, d.delegation().id(), "x"), "IAM_SBX_DELEGATION_CLOSE", 409);
        // Révocation depuis la console : sessions fermées, introspection refusée.
        annuaire.ouvrirSession(compte, "10.0.0.7");
        VueDelegation r = service.revoquerAccesPrestataire(RSSI, d.delegation().id(), "Intervention terminée");
        assertThat(r.etat()).isEqualTo(EtatEffectif.REVOQUEE);
        assertThat(annuaire.sessions(compte)).isEmpty();
    }

    @Test
    @DisplayName("US-EXI-24 : refus motivé, échéance d'une demande, durée et composants bornés, prestataire hors institution")
    void controlesPrestataire() {
        String compte = prestataire();
        VueDelegation d = service.demanderAccesPrestataire(EXPLOITANT, compte, List.of("SBX-TRACE", "MIFOS"), T, T.plus(Duration.ofHours(2)), "Mise à jour");
        VueDelegation r = service.refuserAccesPrestataire(RSSI, d.delegation().id(), "Fenêtre non validée");
        assertThat(r.etat()).isEqualTo(EtatEffectif.REFUSEE);
        assertThat(r.delegation().closeLe()).isEqualTo(T);
        verify(journal).compte(eq(RSSI), eq("ACCES_PRESTATAIRE_REFUSE"), eq(compte), any(), eq("Fenêtre non validée"), any());
        VueDelegation e = service.demanderAccesPrestataire(EXPLOITANT, compte, List.of("SBX-TRACE"), T, T.plus(Duration.ofHours(1)), "Mise à jour");
        horloge.avancer(Duration.ofHours(2));
        refus(() -> service.approuverAccesPrestataire(RSSI, e.delegation().id()), "IAM_SBX_DELEGATION_CLOSE", 409);
        Instant t = horloge.instant();
        refus(() -> service.demanderAccesPrestataire(EXPLOITANT, compte, List.of("SBX-TRACE"), t, t.plus(Duration.ofHours(9)), "x"), "IAM_SBX_DUREE_EXCESSIVE", 422);
        refus(() -> service.demanderAccesPrestataire(EXPLOITANT, compte, List.of("SBX-INCONNU"), t, t.plusSeconds(60), "x"), "IAM_SBX_COMPOSANT_INCONNU", 400);
        refus(() -> service.demanderAccesPrestataire(EXPLOITANT, compte, List.of(), t, t.plusSeconds(60), "x"), "IAM_SBX_COMPOSANT_REQUIS", 400);
        refus(() -> service.demanderAccesPrestataire(EXPLOITANT, membre, List.of("SBX-TRACE"), t, t.plusSeconds(60), "x"), "IAM_SBX_PRESTATAIRE_INSTITUTION", 422);
        refus(() -> service.demanderAccesPrestataire(EXPLOITANT, "inconnu", List.of("SBX-TRACE"), t, t.plusSeconds(60), "x"), "IAM_ACCOUNT_NOT_FOUND", 404);
        // Un mandat n'est ni approuvé ni révoqué comme un accès de prestataire.
        String mandat = accorder(null).delegation().id();
        refus(() -> service.approuverAccesPrestataire(RSSI, mandat), "IAM_SBX_DELEGATION_INTROUVABLE", 404);
        refus(() -> service.revoquerAccesPrestataire(RSSI, mandat, "x"), "IAM_SBX_DELEGATION_INTROUVABLE", 404);
        annuaire.definirActif(compte, false);
        refus(() -> service.demanderAccesPrestataire(EXPLOITANT, compte, List.of("SBX-TRACE"), t, t.plusSeconds(60), "x"), "IAM_SBX_COMPTE_INACTIF", 422);
    }
}
