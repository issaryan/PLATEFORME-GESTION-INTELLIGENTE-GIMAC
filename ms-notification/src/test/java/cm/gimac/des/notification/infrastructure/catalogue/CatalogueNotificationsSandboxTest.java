package cm.gimac.des.notification.infrastructure.catalogue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.notification.domain.service.ReglesSandbox;
import cm.gimac.des.notification.domain.service.ReglesSandbox.Modele;
import cm.gimac.des.notification.domain.service.ReglesSandbox.Portee;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Catalogue unique des modèles de la sandbox (contrat §11) : chargé tel quel, rôles identiques aux règles. */
class CatalogueNotificationsSandboxTest {

    @Test
    @DisplayName("Chaque modèle du catalogue est connu des règles, avec sa portée ; gabarits sans autre variable que {code}")
    void catalogueCharge() {
        ReglesSandbox regles = CatalogueNotificationsSandbox.regles();
        List<Modele> modeles = regles.modeles();
        assertThat(modeles).hasSize(CatalogueNotificationsSandbox.lire().path("modeles").size());
        assertThat(modeles).extracting(Modele::code).doesNotHaveDuplicates().allMatch(c -> c.matches("SBX_(ESP|RAC|CER|LEC|TRA|DON|REF|SIM|MOB)_[A-Z0-9_]+"));
        assertThat(regles.modele("SBX_CER_CAS_AFFECTES")).isPresent();
        assertThat(regles.modele("SBX_RAC_IDENTIFIANT_VERROUILLE").orElseThrow().portee()).isEqualTo(Portee.MIXTE);
        assertThat(regles.modele("SBX_REF_VERSION_A_APPROUVER").orElseThrow().portee()).isEqualTo(Portee.GIMAC);
        assertThat(regles.modele("SBX_ESP_MANDAT_ACCORDE").orElseThrow().intervenants()).isTrue();
        assertThat(regles.modele("SBX_ESP_ESPACE_SUSPENDU").orElseThrow().intervenants()).isFalse();
        assertThat(modeles).allSatisfy(m -> assertThat((m.titre() + m.sousTitre()).replace("{code}", "")).doesNotContain("{", "}"));
    }

    @Test
    @DisplayName("Rôles du catalogue identiques à ceux des règles ; un code en double est refusé")
    void rolesEtDoublons() {
        assertThat(CatalogueNotificationsSandbox.roles()).isEqualTo(new java.util.TreeMap<>(ReglesSandbox.ROLES));
        Modele m = new Modele("SBX_TRA_ALERTE", "t", "s", Portee.GIMAC, false);
        assertThatThrownBy(() -> new ReglesSandbox(List.of(m, m))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("SBX_TRA_ALERTE");
    }
}
