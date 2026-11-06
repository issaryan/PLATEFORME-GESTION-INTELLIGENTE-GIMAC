package cm.gimac.des.iam.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.application.usecase.AdministrationComptesService;
import cm.gimac.des.iam.application.usecase.CatalogueProfils;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComptesSandboxSagaServiceTest {

    static final Acteur SERVICE = new Acteur("sa-sbx", "SBX-ESPACE", Set.of("INTERNAL_IAM_SBX_SAGA"));
    static final Instant T = Instant.parse("2026-10-27T10:00:00Z");

    @Mock AdministrationComptesService comptes;
    @Mock CatalogueProfils catalogue;
    Memoires.Etapes etapes;
    ComptesSandboxSagaService service;

    @BeforeEach
    void init() {
        etapes = new Memoires.Etapes();
        service = new ComptesSandboxSagaService(comptes, catalogue, etapes);
        lenient().when(catalogue.externes()).thenReturn(Set.of("ROLE_SBX_TESTEUR", "ROLE_SBX_LECTEUR"));
        lenient().when(catalogue.compositions()).thenReturn(Map.of(
            "ROLE_SBX_TESTEUR", Set.of("SBX_CER_EXECUTION", "SBX_CER_CONSULTATION"),
            "ROLE_SBX_LECTEUR", Set.of("SBX_LEC_CONSULTATION"),
            "ROLE_CORRESPONDANT", Set.of("PAR_INSTITUTION_READ")
        ));
    }

    static Compte compte(String id, String institution, Set<String> profils) {
        return new Compte(id, "awa.test", "Awa", "Test", "awa@test.cm", true, true, T, null, null, "fr", institution, true, profils, List.of(), true);
    }

    static NouveauCompte nouveau(Set<String> profils) {
        return new NouveauCompte(" Awa.Test ", "Awa", "Test", "awa@test.cm", null, null, "99001", profils);
    }

    @Test
    void ouvertureEtRejeu() {
        Compte cree = compte("c1", "99001", Set.of("ROLE_SBX_TESTEUR"));
        when(comptes.creer(any(), any(), eq(true), eq(false))).thenReturn(new AdministrationComptesService.Creation(cree, true, null));
        when(comptes.lire("c1")).thenReturn(cree);
        var premiere = service.ouvrir(SERVICE, nouveau(Set.of("ROLE_SBX_TESTEUR")), " saga-1/1 ");
        assertThat(premiere.id()).isEqualTo("c1");
        assertThat(premiere.rejouee()).isFalse();
        assertThat(premiere.permissionsSbx()).containsExactly("SBX_CER_CONSULTATION", "SBX_CER_EXECUTION");
        verify(comptes).creer(any(), eq(new NouveauCompte("awa.test", "Awa", "Test", "awa@test.cm", null, null, "99001", Set.of("ROLE_SBX_TESTEUR"))), eq(true), eq(false));

        when(comptes.lister(any(), eq(0), eq(100))).thenReturn(new AdministrationComptesService.PageComptes(List.of(cree), 1));
        var rejeu = service.ouvrir(SERVICE, nouveau(Set.of("ROLE_SBX_TESTEUR")), "saga-1/1");
        assertThat(rejeu.rejouee()).isTrue();
        verify(comptes, org.mockito.Mockito.times(1)).creer(any(), any(), eq(true), eq(false));
        assertThat(service.ouvrir(SERVICE, nouveau(Set.of("ROLE_SBX_TESTEUR")), null).rejouee()).isFalse();
        assertThat(service.lire("c1").id()).isEqualTo("c1");
    }

    @Test
    void ouvertureRefuseProfilNonSandboxOuVide() {
        assertThatThrownBy(() -> service.ouvrir(SERVICE, nouveau(Set.of()), "x")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_PROFIL_REQUIS"));
        assertThatThrownBy(() -> service.ouvrir(SERVICE, nouveau(Set.of("ROLE_CORRESPONDANT")), "x")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_PROFIL_INVALIDE"));
        verify(comptes, never()).creer(any(), any(), eq(true), eq(false));
    }

    @Test
    void matriceConserveLesAutresAccesEtRejoue() {
        Compte avant = compte("c1", "99001", Set.of("ROLE_CORRESPONDANT", "ROLE_SBX_TESTEUR"));
        Compte apres = compte("c1", "99001", Set.of("ROLE_CORRESPONDANT", "ROLE_SBX_LECTEUR"));
        when(comptes.lire("c1")).thenReturn(avant, apres, avant);
        when(comptes.definirProfils(any(), eq("c1"), eq(Set.of("ROLE_CORRESPONDANT", "ROLE_SBX_LECTEUR")), eq("Revue"))).thenReturn(apres);
        var vue = service.matrice(SERVICE, "c1", Set.of("ROLE_SBX_LECTEUR"), "Revue", "saga-2/3");
        assertThat(vue.profils()).containsExactly("ROLE_CORRESPONDANT", "ROLE_SBX_LECTEUR");
        assertThat(vue.permissionsSbx()).containsExactly("SBX_LEC_CONSULTATION");
        assertThat(service.matrice(SERVICE, "c1", Set.of("ROLE_SBX_LECTEUR"), "Revue", "saga-2/3").rejouee()).isTrue();
        verify(comptes, org.mockito.Mockito.times(1)).definirProfils(any(), any(), any(), any());
    }

    @Test
    void matriceRefuseCompteInterneEtProfilInvalide() {
        when(comptes.lire("c1")).thenReturn(compte("c1", null, Set.of("ROLE_SBX_TESTEUR")));
        assertThatThrownBy(() -> service.matrice(SERVICE, "c1", Set.of("ROLE_SBX_LECTEUR"), "x", null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_COMPTE_HORS_INSTITUTION"));
        assertThatThrownBy(() -> service.matrice(SERVICE, "c1", Set.of("ROLE_CORRESPONDANT"), "x", null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_PROFIL_INVALIDE"));
        assertThatThrownBy(() -> service.matrice(SERVICE, "c1", null, "x", null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_PROFIL_REQUIS"));
        when(catalogue.externes()).thenReturn(Set.of("ROLE_CORRESPONDANT"));
        assertThatThrownBy(() -> service.matrice(SERVICE, "c1", Set.of("ROLE_CORRESPONDANT"), "x", null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_PROFIL_INVALIDE"));
        verify(comptes, never()).definirProfils(any(), any(), any(), any());
    }

    @Test
    void rejeuOuvertureSansCompteEtMatriceSansCle() {
        etapes.enregistrer("saga-o/1", ComptesSandboxSagaService.OUVERTURE, "awa.test", SERVICE.id());
        when(comptes.lister(any(), eq(0), eq(100))).thenReturn(new AdministrationComptesService.PageComptes(List.of(), 0));
        assertThatThrownBy(() -> service.ouvrir(SERVICE, nouveau(Set.of("ROLE_SBX_TESTEUR")), "saga-o/1"))
            .isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_ACCOUNT_NOT_FOUND"));
        Compte c = compte("c1", "99001", Set.of("ROLE_SBX_LECTEUR"));
        when(comptes.lire("c1")).thenReturn(c);
        when(comptes.definirProfils(any(), eq("c1"), eq(Set.of("ROLE_SBX_LECTEUR")), eq("Revue"))).thenReturn(c);
        assertThat(service.matrice(SERVICE, "c1", Set.of("ROLE_SBX_LECTEUR"), "Revue", " ").rejouee()).isFalse();
    }

    static final String UUID_99001 = "6f1c2a3e-8888-4a6b-9c1d-000000000001";
    static final boolean[] PANNE = { false };

    @Test
    void identifiantAuRegistreEtCodeParticipant() {
        ComptesSandboxSagaService traduit = new ComptesSandboxSagaService(comptes, catalogue, etapes, new TraductionInstitution(new cm.gimac.des.iam.application.port.out.sandbox.RegistreInstitutions() {
            @Override
            public java.util.Optional<String> codeParticipant(java.util.UUID i) {
                if (PANNE[0]) {
                    throw new RegistreIndisponible("injoignable", null);
                }
                return java.util.Optional.of("99001");
            }

            @Override
            public java.util.Optional<java.util.UUID> identifiant(String code) {
                if (PANNE[0]) {
                    throw new RegistreIndisponible("injoignable", null);
                }
                return java.util.Optional.of(java.util.UUID.fromString(UUID_99001));
            }
        }, java.time.Clock.fixed(T, java.time.ZoneOffset.UTC), java.time.Duration.ZERO));
        when(comptes.lire("c7")).thenReturn(compte("c7", UUID_99001, Set.of("ROLE_SBX_TESTEUR")));
        ComptesSandboxSagaService.CompteSaga v = traduit.lire("c7");
        assertThat(v.institutionId()).isEqualTo(UUID_99001);
        assertThat(v.codeParticipant()).isEqualTo("99001");
        PANNE[0] = true;
        try {
            assertThat(traduit.lire("c7").codeParticipant()).isNull();
        } finally {
            PANNE[0] = false;
        }
    }
}
