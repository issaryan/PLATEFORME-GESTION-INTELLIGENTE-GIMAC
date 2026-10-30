package cm.gimac.des.iam.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.application.usecase.CatalogueProfils;
import cm.gimac.des.iam.application.usecase.ConfigurationAuthentificationService;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatriceSandboxSagaServiceTest {

    static final Acteur SERVICE = new Acteur("sa-sbx", "SBX-ESPACE", Set.of("INTERNAL_IAM_SBX_SAGA"));

    @Mock ConfigurationAuthentificationService profils;
    @Mock CatalogueProfils catalogue;
    Memoires.Etapes etapes;
    MatriceSandboxSagaService service;

    @BeforeEach
    void init() {
        etapes = new Memoires.Etapes();
        service = new MatriceSandboxSagaService(profils, catalogue, etapes);
        org.mockito.Mockito.lenient().when(catalogue.attribuables()).thenReturn(Set.of("ROLE_SBX_LECTEUR", "ROLE_SNA", "ROLE_CORRESPONDANT"));
        org.mockito.Mockito.lenient().when(catalogue.externes()).thenReturn(Set.of("ROLE_SBX_LECTEUR", "ROLE_CORRESPONDANT"));
        org.mockito.Mockito.lenient().when(catalogue.compositions()).thenReturn(Map.of(
            "ROLE_SBX_LECTEUR", Set.of("SBX_LEC_CONSULTATION", "NOT_INBOX_READ"),
            "ROLE_SNA", Set.of("SBX_CER_VALIDATION", "IAM_PROFILE_EDIT"),
            "ROLE_CORRESPONDANT", Set.of("PAR_INSTITUTION_READ")
        ));
    }

    @Test
    void publicationGlobaleConserveLesPermissionsHorsSbxEtRejoueSansEffet() {
        Map<String, Set<String>> matrice = Map.of(
            "ROLE_SBX_LECTEUR", Set.of("SBX_ISO_SIMULATION"),
            "ROLE_SNA", Set.of("SBX_CER_VALIDATION", "SBX_CER_CONSULTATION")
        );
        var premiere = service.publier(SERVICE, 7, matrice, " saga-7/1 ");
        assertThat(premiere.version()).isEqualTo(7);
        assertThat(premiere.rejouee()).isFalse();
        Acteur privilegie = new Acteur(SERVICE.id(), SERVICE.nom(), Set.of(ReglesAdministration.OCTROI_PRIVILEGIE));
        verify(profils).definirPermissions(privilegie, "ROLE_SBX_LECTEUR", Set.of("SBX_ISO_SIMULATION", "NOT_INBOX_READ"), "Publication de la matrice sandbox version 7");
        verify(profils).definirPermissions(privilegie, "ROLE_SNA", Set.of("SBX_CER_VALIDATION", "SBX_CER_CONSULTATION", "IAM_PROFILE_EDIT"), "Publication de la matrice sandbox version 7");

        var rejeu = service.publier(SERVICE, 99, matrice, "saga-7/1");
        assertThat(rejeu.version()).isEqualTo(7);
        assertThat(rejeu.rejouee()).isTrue();
        verify(profils, times(2)).definirPermissions(any(), any(), any(), any());
    }

    @Test
    void publicationSansCleEtCompositionVideSontAdmises() {
        assertThat(service.publier(SERVICE, 2, Map.of("ROLE_SBX_LECTEUR", Set.of()), null).rejouee()).isFalse();
        verify(profils).definirPermissions(any(), eq("ROLE_SBX_LECTEUR"), eq(Set.of("NOT_INBOX_READ")), eq("Publication de la matrice sandbox version 2"));
        assertThat(service.publier(SERVICE, 3, Map.of("ROLE_SBX_LECTEUR", Set.of("SBX_LEC_CONSULTATION")), " ").rejouee()).isFalse();
        Map<String, Set<String>> compositionNulle = new java.util.HashMap<>();
        compositionNulle.put("ROLE_SBX_LECTEUR", null);
        assertThat(service.publier(SERVICE, 4, compositionNulle, null).rejouee()).isFalse();
    }

    @Test
    void entreeInvalideOuProfilHorsMatriceNEcritRien() {
        assertThatThrownBy(() -> service.publier(SERVICE, 0, Map.of(), "x")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_MATRICE_INVALIDE"));
        assertThatThrownBy(() -> service.publier(SERVICE, 1, null, "x")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_MATRICE_INVALIDE"));
        assertThatThrownBy(() -> service.publier(SERVICE, 1, Map.of(), "x")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_MATRICE_INVALIDE"));
        assertThatThrownBy(() -> service.publier(SERVICE, 1, Map.of("ROLE_CORRESPONDANT", Set.of("SBX_LEC_CONSULTATION")), "x"))
            .isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_PROFIL_INVALIDE"));
        assertThatThrownBy(() -> service.publier(SERVICE, 1, Map.of("ROLE_INCONNU", Set.of("SBX_LEC_CONSULTATION")), "x"))
            .isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_PROFIL_INVALIDE"));
        assertThatThrownBy(() -> service.publier(SERVICE, 1, Map.of("ROLE_SBX_LECTEUR", Set.of("PAR_INSTITUTION_READ")), "x"))
            .isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SBX_PERMISSION_INVALIDE"));
        verify(profils, never()).definirPermissions(any(), any(), any(), any());
        assertThat(etapes.lignes).isEmpty();
    }

    @Test
    void validationExterneEstRefuseeAvantTouteRecomposition() {
        Map<String, Set<String>> matrice = Map.of(
            "ROLE_SNA", Set.of("SBX_CER_VALIDATION"),
            "ROLE_SBX_LECTEUR", Set.of("SBX_CER_VALIDATION")
        );
        assertThatThrownBy(() -> service.publier(SERVICE, 4, matrice, "x"))
            .isInstanceOfSatisfying(IamException.class, e -> {
                assertThat(e.code()).isEqualTo("IAM_SBX_VALIDATION_RESERVEE");
                assertThat(e.status()).isEqualTo(422);
            });
        verify(profils, never()).definirPermissions(any(), any(), any(), any());
        assertThat(etapes.lignes).isEmpty();
    }
}
