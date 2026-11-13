package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase.PerimetreProposable;
import cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase.ServiceProposable;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.PerimetreModification;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** D-145 : périmètre retenu par le gestionnaire à la création, journal de la version 1 et services proposables. */
class PerimetreCreationTest {

    static final List<String> PROPOSABLES = List.of("TRANSVERSE", "W2W", "ISO.W2W", "CRT.NET");

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Sans saisie : services proposables ; avec saisie : TRANSVERSE et services retenus dans l'ordre des proposables")
    void retenir() {
        assertThat(PerimetreCreation.retenir(null, PROPOSABLES)).isEqualTo(PROPOSABLES);
        assertThat(PerimetreCreation.retenir(Arrays.asList(" CRT.NET ", "W2W", "W2W", "TRANSVERSE", " ", null), PROPOSABLES)).containsExactly("TRANSVERSE", "W2W", "CRT.NET");
    }

    @Test
    @DisplayName("Refus : aucun service à certifier, service non proposable")
    void refus() {
        refus(() -> PerimetreCreation.retenir(List.of(), PROPOSABLES), "HOM_PERIMETRE_VIDE");
        refus(() -> PerimetreCreation.retenir(List.of("TRANSVERSE"), PROPOSABLES), "HOM_PERIMETRE_VIDE");
        assertThatThrownBy(() -> PerimetreCreation.retenir(List.of("W2W", "BILL"), PROPOSABLES)).isInstanceOfSatisfying(HomologationException.class, e -> {
            assertThat(e.code()).isEqualTo("HOM_PERIMETRE_SERVICE_NON_SOUSCRIT");
            assertThat(e.details()).containsEntry("services", List.of("BILL"));
        });
    }

    @Test
    @DisplayName("Journal : version 1, permission d'ouverture, proposables avant et retenus après")
    void ligne() {
        UUID liaison = UUID.randomUUID();
        UUID auteur = UUID.randomUUID();
        Instant t = Instant.parse("2026-11-12T08:00:00Z");
        PerimetreModification m = PerimetreCreation.ligne(liaison, auteur, PROPOSABLES, List.of("TRANSVERSE", "W2W"), t);
        assertThat(m.getLiaisonId()).isEqualTo(liaison);
        assertThat(m.getVersionPerimetre()).isEqualTo(1);
        assertThat(m.getAuteurId()).isEqualTo(auteur);
        assertThat(m.getPermissionUtilisee()).isEqualTo("HOM_LIAISON_OPEN");
        assertThat(m.getAvant()).isEqualTo("TRANSVERSE,W2W,ISO.W2W,CRT.NET");
        assertThat(m.getApres()).isEqualTo("TRANSVERSE,W2W");
        assertThat(m.getMotif()).isEqualTo(PerimetreCreation.MOTIF);
        assertThat(m.getModifieLe()).isEqualTo(t);
    }

    @Test
    @DisplayName("Proposables : TRANSVERSE exclu, interface d'après le préfixe, catalogue du type sans souscription")
    void proposable() {
        UUID id = UUID.randomUUID();
        InstitutionView souscrite = new InstitutionView(id, "A", "ADHERENT", "BANQUE", List.of(new InstitutionView.ServiceSouscrit("W2W", null), new InstitutionView.ServiceSouscrit("TRANSVERSE", null)));
        PerimetreProposable p = PerimetreCreation.proposable(PROPOSABLES, souscrite);
        assertThat(p.souscrits()).isTrue();
        assertThat(p.services()).extracting(ServiceProposable::code).containsExactly("W2W", "ISO.W2W", "CRT.NET");
        assertThat(p.services()).extracting(ServiceProposable::interfaceSandbox).containsExactly("MOBILE", "ISO20022", "CARTE");
        InstitutionView sans = new InstitutionView(id, "A", "ADHERENT", "BANQUE", Arrays.asList(new InstitutionView.ServiceSouscrit("TRANSVERSE", null), new InstitutionView.ServiceSouscrit(" ", null), new InstitutionView.ServiceSouscrit(null, null)));
        PerimetreProposable catalogue = PerimetreCreation.proposable(List.of("TRANSVERSE", "RETRAIT"), sans);
        assertThat(catalogue.souscrits()).isFalse();
        assertThat(catalogue.services()).containsExactly(new ServiceProposable("RETRAIT", "Retrait", null));
    }
}
