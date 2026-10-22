package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.sandbox.PieceSandbox;
import cm.gimac.des.document.domain.sandbox.TypePieceSandbox;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Catalogue des types de pièces de la sandbox et quarantaines depuis la console (US-EXI-08). */
class TypesPiecesSandboxServiceTest {

    final PiecesSandboxMemoire pieces = new PiecesSandboxMemoire();
    final TypesPiecesSandboxService service = new TypesPiecesSandboxService(pieces, Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC));

    @Test
    @DisplayName("Types de pièces de la sandbox et politiques de rétention au catalogue")
    void catalogue() {
        assertThat(service.types()).extracting(TypePieceSandbox::code).containsExactly(
            "SBX_BILAN_CERTIFICATION",
            "SBX_RAPPORT_ECARTS_ISO20022",
            "SBX_TRACE_EXPORTEE",
            "SBX_FICHIER_REMIS_PARTICIPANT"
        );
    }

    @Test
    @DisplayName("Rétention modifiée avec motif, version incrémentée ; dix ans au moins pour un type probant")
    void modifierRetention() {
        TypePieceSandbox t = service.modifierRetention("SBX_TRACE_EXPORTEE", "P12Y", "Allongement demandé par le RSSI", 1, "rssi");
        assertThat(t.retention()).isEqualTo(Period.ofYears(12));
        assertThat(t.version()).isEqualTo(2);
        assertThat(service.modifierRetention("SBX_FICHIER_REMIS_PARTICIPANT", "P1Y", "motif", 1, "rssi").retention()).isEqualTo(Period.ofYears(1));
        assertThatThrownBy(() -> service.modifierRetention("SBX_BILAN_CERTIFICATION", "P9Y", "motif", 1, "rssi"))
            .hasFieldOrPropertyWithValue("code", "DOC_SBX_RETENTION_INSUFFISANTE");
        assertThatThrownBy(() -> service.modifierRetention("SBX_BILAN_CERTIFICATION", "P0D", "motif", 1, "rssi")).hasFieldOrPropertyWithValue("code", "DOC_SBX_RETENTION_INVALIDE");
        assertThatThrownBy(() -> service.modifierRetention("SBX_BILAN_CERTIFICATION", "dix ans", "motif", 1, "rssi")).hasFieldOrPropertyWithValue("code", "DOC_SBX_RETENTION_INVALIDE");
        assertThatThrownBy(() -> service.modifierRetention("SBX_BILAN_CERTIFICATION", null, "motif", 1, "rssi")).hasFieldOrPropertyWithValue("code", "DOC_SBX_RETENTION_INVALIDE");
        assertThatThrownBy(() -> service.modifierRetention("SBX_BILAN_CERTIFICATION", "P10Y", " ", 1, "rssi")).hasFieldOrPropertyWithValue("code", "DOC_SBX_MOTIF_REQUIS");
        assertThatThrownBy(() -> service.modifierRetention("SBX_BILAN_CERTIFICATION", "P10Y", null, 1, "rssi")).hasFieldOrPropertyWithValue("code", "DOC_SBX_MOTIF_REQUIS");
        assertThatThrownBy(() -> service.modifierRetention("SBX_BILAN_CERTIFICATION", "P10Y", "x".repeat(501), 1, "rssi")).hasFieldOrPropertyWithValue("code", "DOC_SBX_MOTIF_REQUIS");
        assertThatThrownBy(() -> service.modifierRetention("INCONNU", "P10Y", "m", 1, "rssi")).isInstanceOf(DocumentException.class);
    }

    @Test
    @DisplayName("Version périmée ou modification concurrente : 409 DOC_SBX_VERSION_PERIMEE")
    void versionPerimee() {
        assertThatThrownBy(() -> service.modifierRetention("SBX_TRACE_EXPORTEE", "P11Y", "m", 7, "rssi")).hasFieldOrPropertyWithValue("code", "DOC_SBX_VERSION_PERIMEE");
        pieces.modificationConcurrente = true;
        assertThatThrownBy(() -> service.modifierRetention("SBX_TRACE_EXPORTEE", "P11Y", "m", 1, "rssi")).hasFieldOrPropertyWithValue("code", "DOC_SBX_VERSION_PERIMEE");
    }

    @Test
    @DisplayName("Supervision des quarantaines : plus récentes d'abord, paginées, taille de 1 à 100")
    void quarantaines() {
        pieces.inscrire(piece(Instant.parse("2026-10-20T08:00:00Z")));
        pieces.inscrire(piece(Instant.parse("2026-10-21T08:00:00Z")));
        TypesPiecesSandboxService.PageQuarantaines p = service.quarantaines(0, 1);
        assertThat(p.total()).isEqualTo(2);
        assertThat(p.contenu()).singleElement().extracting(PieceSandbox::deposeeLe).isEqualTo(Instant.parse("2026-10-21T08:00:00Z"));
        for (int[] pt : new int[][] { { -1, 1 }, { 0, 0 }, { 0, 101 } }) {
            assertThatThrownBy(() -> service.quarantaines(pt[0], pt[1])).hasFieldOrPropertyWithValue("code", "DOC_SBX_PAGINATION_INVALIDE");
        }
    }

    static PieceSandbox piece(Instant le) {
        return new PieceSandbox(UUID.randomUUID(), "SBX_TRACE_EXPORTEE", "99001", "trace/1", "a".repeat(64), 10, PieceSandbox.Etat.QUARANTAINE, le, null, "sbx-trace", 2);
    }
}
