package cm.gimac.des.iam.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.sandbox.Introspection;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Introspection des jetons pour la sandbox (D-119, US-EXI-06), sans Docker : décision à l'instant, sans cache. */
class IntrospectionSandboxServiceTest {

    static final Instant EXP = Instant.parse("2026-10-22T08:05:00Z");

    AnnuaireEnMemoire annuaire;
    Memoires.Perimetres perimetres;
    Memoires.Delegations delegations;
    Memoires.Horloge horloge;
    Map<String, Map<String, Object>> jetons;
    IntrospectionSandboxService service;
    JournalSandbox journal;
    String testeur;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(Set.of("ROLE_SBX_TESTEUR")));
        perimetres = new Memoires.Perimetres();
        jetons = new HashMap<>();
        delegations = new Memoires.Delegations();
        horloge = new Memoires.Horloge(Instant.parse("2026-10-22T08:00:00Z"));
        journal = mock(JournalSandbox.class);
        service = new IntrospectionSandboxService(j -> Optional.ofNullable(jetons.get(j)), annuaire, perimetres, delegations, journal, horloge);
        testeur = annuaire.creer(new NouveauCompte("testeur.99001", "Awa", "N", "awa@t.test", null, null, "99001", Set.of("ROLE_SBX_TESTEUR")), List.of());
        jetons.put("j-testeur", Map.of("sub", testeur, "roles", List.of("ROLE_SBX_TESTEUR", "SBX_CER_EXECUTION", "offline_access"), "exp", EXP, "institution_id", "99001"));
        jetons.put("j-rssi", Map.of("sub", "demo-rssi", "roles", List.of("ROLE_RSSI"), "exp", EXP));
        jetons.put("j-sans-sujet", Map.of("roles", List.of("ROLE_RSSI")));
        jetons.put("j-inconnu", Map.of("sub", "inconnu", "exp", "x"));
    }

    @Test
    @DisplayName("Jeton admis : portée du compte (institution du compte, permissions du jeton, échéance du jeton)")
    void admis() {
        Introspection i = service.introspecter("j-testeur", null);
        assertThat(i.active()).isTrue();
        assertThat(i.institutionId()).isEqualTo("99001");
        assertThat(i.permissions()).containsExactlyInAnyOrder("ROLE_SBX_TESTEUR", "SBX_CER_EXECUTION");
        assertThat(i.type()).isEqualTo(Introspection.COMPTE);
        assertThat(i.expiration()).isEqualTo(EXP);
        assertThat(service.introspecter("j-testeur", " 99001 ").active()).isTrue();
        assertThat(service.introspecter("j-testeur", " ").active()).isTrue();
        assertThat(service.introspecter("j-rssi", null).active()).isTrue();
    }

    @Test
    @DisplayName("US-EXI-06 scénario 1 : introspection refusée pour tout jeton rattaché au périmètre suspendu, dès la pose de l'indicateur")
    void perimetreSuspendu() {
        perimetres.poser("99001", "sa", null, EXP);
        Introspection i = service.introspecter("j-testeur", null);
        assertThat(i.active()).isFalse();
        assertThat(i.motifRefus()).isEqualTo("PERIMETRE_SUSPENDU");
        // Un compte interne du GIMAC n'est pas concerné.
        assertThat(service.introspecter("j-rssi", null).active()).isTrue();
        perimetres.lever("99001");
        assertThat(service.introspecter("j-testeur", null).active()).isTrue();
    }

    @Test
    @DisplayName("Refus : jeton invalide, sans sujet, compte inconnu ou inactif, institution hors du compte")
    void refus() {
        assertThat(service.introspecter("faux", null).motifRefus()).isEqualTo("JETON_INVALIDE");
        assertThat(service.introspecter("j-sans-sujet", null).motifRefus()).isEqualTo("COMPTE_INCONNU");
        Introspection inconnu = service.introspecter("j-inconnu", null);
        assertThat(inconnu.motifRefus()).isEqualTo("COMPTE_INCONNU");
        assertThat(inconnu.sujet()).isEqualTo("inconnu");
        assertThat(service.introspecter("j-testeur", "99002").motifRefus()).isEqualTo("HORS_MANDAT");
        annuaire.definirActif(testeur, false);
        assertThat(service.introspecter("j-testeur", null).motifRefus()).isEqualTo("COMPTE_INACTIF");
        assertThat(IntrospectionSandboxService.expiration(Map.of("exp", "x"))).isNull();
    }

    cm.gimac.des.iam.domain.sandbox.Delegation mandat(String id, Instant debut, Instant fin, cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation etat) {
        var d = new cm.gimac.des.iam.domain.sandbox.Delegation(id, cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation.MANDAT, testeur, "99002",
            Set.of("SBX_RAC_CONSULTATION"), Set.of(), debut, fin, etat, "sa", null, null, debut, null);
        delegations.enregistrer(d);
        return d;
    }

    @Test
    @DisplayName("US-EXI-04 scénario 1 : jeton délégué projeté sur l'institution mandante, avec les seuls droits du mandat, jusqu'à sa fin")
    void jetonDelegue() {
        Instant t = horloge.instant();
        mandat("m1", t.minusSeconds(3600), t.plusSeconds(120), cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.ACCORDEE);
        Introspection i = service.introspecter("j-testeur", "99002");
        assertThat(i.active()).isTrue();
        assertThat(i.institutionId()).isEqualTo("99002");
        assertThat(i.permissions()).containsExactly("SBX_RAC_CONSULTATION");
        assertThat(i.type()).isEqualTo("MANDAT");
        assertThat(i.delegationId()).isEqualTo("m1");
        assertThat(i.expiration()).isEqualTo(t.plusSeconds(120));
        // Jeton qui expire avant la fin du mandat : échéance du jeton.
        mandat("m2", t.minusSeconds(3600), t.plusSeconds(86400), cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.ACCORDEE);
        delegations.lignes.remove("m1");
        assertThat(service.introspecter("j-testeur", "99002").expiration()).isEqualTo(EXP);
        jetons.put("j-sans-exp", Map.of("sub", testeur));
        assertThat(service.introspecter("j-sans-exp", "99002").expiration()).isEqualTo(t.plusSeconds(86400));
    }

    @Test
    @DisplayName("US-EXI-04 scénario 2 : introspection refusée dès la date de fin, même si le planificateur n'a rien constaté")
    void echeanceSansPlanificateur() {
        Instant t = horloge.instant();
        mandat("m1", t.minusSeconds(3600), t.plusSeconds(60), cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.ACCORDEE);
        assertThat(service.introspecter("j-testeur", "99002").active()).isTrue();
        horloge.avancer(java.time.Duration.ofSeconds(60));
        Introspection i = service.introspecter("j-testeur", "99002");
        assertThat(i.active()).isFalse();
        assertThat(i.motifRefus()).isEqualTo("HORS_MANDAT");
        assertThat(delegations.lignes.get("m1").etat()).isEqualTo(cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.ACCORDEE);
    }

    @Test
    @DisplayName("Mandat programmé, révoqué ou d'une autre institution, périmètre mandant suspendu : refus")
    void mandatsRefuses() {
        Instant t = horloge.instant();
        mandat("p", t.plusSeconds(60), t.plusSeconds(3600), cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.ACCORDEE);
        assertThat(service.introspecter("j-testeur", "99002").active()).isFalse();
        mandat("r", t.minusSeconds(60), t.plusSeconds(3600), cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.REVOQUEE);
        assertThat(service.introspecter("j-testeur", "99002").active()).isFalse();
        mandat("a", t.minusSeconds(60), t.plusSeconds(3600), cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.ACCORDEE);
        assertThat(service.introspecter("j-testeur", "99003").active()).isFalse();
        assertThat(service.introspecter("j-testeur", "99002").active()).isTrue();
        perimetres.poser("99002", "sa", null, t);
        assertThat(service.introspecter("j-testeur", "99002").motifRefus()).isEqualTo("PERIMETRE_SUSPENDU");
    }

    String prestataire(String id, Instant debut, Instant fin, cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation etat) {
        String compte = annuaire.parIdentifiant("presta.maintenance").map(cm.gimac.des.iam.domain.compte.Compte::id)
            .orElseGet(() -> annuaire.creer(new NouveauCompte("presta.maintenance", "Jean", "Mbarga", "jean@presta.test", null, null, null, Set.of()), List.of()));
        delegations.enregistrer(new cm.gimac.des.iam.domain.sandbox.Delegation(id, cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation.PRESTATAIRE, compte, null,
            Set.of(), Set.of("SBX-TRACE"), debut, fin, etat, "exploitant", "rssi", "Maintenance", debut, null));
        jetons.put("j-presta", Map.of("sub", compte, "roles", List.of("ROLE_USER", "SBX_TRA_CONSULTATION"), "exp", EXP));
        return compte;
    }

    @Test
    @DisplayName("US-EXI-24 scénario 1 : accès de prestataire approuvé, borné au composant désigné, sans permission ni institution")
    void accesPrestataire() {
        Instant t = horloge.instant();
        prestataire("a1", t.minusSeconds(60), t.plusSeconds(4 * 3600), cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.ACCORDEE);
        Introspection i = service.introspecter("j-presta", null, "SBX-TRACE");
        assertThat(i.active()).isTrue();
        assertThat(i.type()).isEqualTo("PRESTATAIRE");
        assertThat(i.institutionId()).isNull();
        assertThat(i.permissions()).isEmpty();
        assertThat(i.composants()).containsExactly("SBX-TRACE");
        assertThat(i.delegationId()).isEqualTo("a1");
        assertThat(i.expiration()).isEqualTo(EXP);
        assertThat(service.introspecter("j-presta", null).active()).isTrue();
        assertThat(service.introspecter("j-presta", " ", " ").active()).isTrue();
        // Autre composant : refusé.
        assertThat(service.introspecter("j-presta", null, "SBX-CAMPAGNE").motifRefus()).isEqualTo("COMPOSANT_HORS_ACCES");
        verify(journal, never()).compte(any(), any(), any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("US-EXI-24 scénario 2 : à l'échéance, l'introspection du prestataire est refusée ; demande non approuvée : refus")
    void echeancePrestataire() {
        Instant t = horloge.instant();
        prestataire("a1", t.minusSeconds(3600), t.plusSeconds(60), cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.ACCORDEE);
        jetons.put("j-presta-long", Map.of("sub", delegations.lignes.get("a1").compteId()));
        assertThat(service.introspecter("j-presta", null, "SBX-TRACE").expiration()).isEqualTo(t.plusSeconds(60));
        assertThat(service.introspecter("j-presta-long", null, "SBX-TRACE").expiration()).isEqualTo(t.plusSeconds(60));
        horloge.avancer(java.time.Duration.ofSeconds(60));
        assertThat(service.introspecter("j-presta", null, "SBX-TRACE").motifRefus()).isEqualTo("COMPOSANT_HORS_ACCES");
        assertThat(service.introspecter("j-presta", null).motifRefus()).isEqualTo("ACCES_PRESTATAIRE_INACTIF");
        prestataire("a2", t, t.plusSeconds(7200), cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.DEMANDEE);
        assertThat(service.introspecter("j-presta", null).active()).isFalse();
        prestataire("a3", t.plusSeconds(120), t.plusSeconds(7200), cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.ACCORDEE);
        assertThat(service.introspecter("j-presta", null, "SBX-TRACE").motifRefus()).isEqualTo("COMPOSANT_HORS_ACCES");
    }

    @Test
    @DisplayName("US-EXI-24 scénario 3 : données d'une institution demandées par le prestataire, refus journalisé")
    void prestataireHorsDonnees() {
        Instant t = horloge.instant();
        String compte = prestataire("a1", t.minusSeconds(60), t.plusSeconds(3600), cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation.ACCORDEE);
        Introspection i = service.introspecter("j-presta", "99001", "SBX-TRACE");
        assertThat(i.active()).isFalse();
        assertThat(i.motifRefus()).isEqualTo("PRESTATAIRE_HORS_DONNEES");
        verify(journal).compte(any(), eq("TENTATIVE_ACCES_PRESTATAIRE"), eq(compte), eq("presta.maintenance"), any(), eq(List.of("99001", "SBX-TRACE")));
    }
}
