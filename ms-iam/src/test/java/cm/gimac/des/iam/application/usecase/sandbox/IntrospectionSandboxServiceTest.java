package cm.gimac.des.iam.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

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
    Map<String, Map<String, Object>> jetons;
    IntrospectionSandboxService service;
    String testeur;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(Set.of("ROLE_SBX_TESTEUR")));
        perimetres = new Memoires.Perimetres();
        jetons = new HashMap<>();
        service = new IntrospectionSandboxService(j -> Optional.ofNullable(jetons.get(j)), annuaire, perimetres);
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
        assertThat(service.introspecter("j-testeur", "99002").motifRefus()).isEqualTo("HORS_PERIMETRE");
        annuaire.definirActif(testeur, false);
        assertThat(service.introspecter("j-testeur", null).motifRefus()).isEqualTo("COMPTE_INACTIF");
        assertThat(IntrospectionSandboxService.expiration(Map.of("exp", "x"))).isNull();
    }
}
