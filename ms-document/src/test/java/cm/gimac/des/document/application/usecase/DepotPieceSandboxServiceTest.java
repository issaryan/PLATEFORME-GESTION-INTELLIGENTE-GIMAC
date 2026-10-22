package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Commande;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Deposant;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Fichier;
import cm.gimac.des.document.application.usecase.DepotPieceSandboxService.Demande;
import cm.gimac.des.document.application.usecase.DepotPieceSandboxService.Resultat;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.sandbox.PieceSandbox.Etat;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Dépôt des pièces de la sandbox (US-EXI-08), horloge figée au 22/10/2026, sans base ni dépôt objet. */
class DepotPieceSandboxServiceTest {

    static final Instant MAINTENANT = Instant.parse("2026-10-22T10:00:00Z");
    static final byte[] BILAN = "Bilan fige de la campagne C-12 de l'institution 99001".getBytes(StandardCharsets.UTF_8);
    static final byte[] TRACE_PAN = "Trace : carte 4970 1012 3456 7893 servie".getBytes(StandardCharsets.UTF_8);

    PiecesSandboxMemoire pieces;
    DeposerDocumentUseCase depot;
    DepotPieceSandboxService service;

    @BeforeEach
    void preparer() {
        pieces = new PiecesSandboxMemoire();
        depot = mock(DeposerDocumentUseCase.class);
        when(depot.deposer(any(), any())).thenAnswer(i -> {
            Commande c = i.getArgument(0);
            return new DeposerDocumentUseCase.Resultat(UUID.randomUUID(), EmpreinteSha256.de(c.fichier().contenu()), c.fichier().nomFichier(), c.fichier().contenu().length, "text/plain");
        });
        service = new DepotPieceSandboxService(pieces, depot, (contenu, type) -> Optional.of(new String(contenu, StandardCharsets.UTF_8)), Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    static Demande demande(String type, byte[] contenu, String empreinte) {
        return new Demande(new Fichier("piece.txt", "text/plain", contenu), type, empreinte, "99001", "campagne/C-12");
    }

    @Test
    @DisplayName("Critère 1 : bilan figé de 99001 déposé par SBX-CAMPAGNE, scellé avec la rétention du type, document-sealed publié")
    void depotDuBilanFige() {
        String sha = EmpreinteSha256.de(BILAN);
        Resultat r = service.deposer(demande("SBX_BILAN_CERTIFICATION", BILAN, sha.toUpperCase()), "sbx-campagne");
        assertThat(r.etat()).isEqualTo(Etat.SCELLEE);
        assertThat(r.dejaConservee()).isFalse();
        assertThat(r.sha256()).isEqualTo(sha);
        assertThat(r.conserveeJusquAu()).isEqualTo(Instant.parse("2036-10-22T10:00:00Z"));
        ArgumentCaptor<Commande> commande = ArgumentCaptor.forClass(Commande.class);
        ArgumentCaptor<Deposant> deposant = ArgumentCaptor.forClass(Deposant.class);
        verify(depot).deposer(commande.capture(), deposant.capture());
        assertThat(commande.getValue().finalite()).isEqualTo("SBX_BILAN_CERTIFICATION");
        assertThat(deposant.getValue().service()).isEqualTo("sbx-campagne");
        assertThat(pieces.pieces).singleElement().satisfies(p -> {
            assertThat(p.id()).isEqualTo(r.id());
            assertThat(p.codeParticipant()).isEqualTo("99001");
        });
        assertThat(pieces.evenements).singleElement().satisfies(e -> {
            assertThat(e.type()).isEqualTo(DepotPieceSandboxService.EVENEMENT_SCELLE);
            assertThat(e.cle()).isEqualTo(r.id().toString());
            assertThat(e.donnees()).contains("\"sha256\":\"" + sha + "\"", "\"typePiece\":\"SBX_BILAN_CERTIFICATION\"", "\"conserveeJusquAu\":\"2036-10-22T10:00:00Z\"");
        });
    }

    @Test
    @DisplayName("Critère 2 : trace déjà conservée redéposée après une reprise, aucun second dépôt, identifiant existant restitué")
    void pieceDejaConservee() {
        byte[] trace = "trace exportee 2026-10-21".getBytes(StandardCharsets.UTF_8);
        String sha = EmpreinteSha256.de(trace);
        Resultat premier = service.deposer(demande("SBX_TRACE_EXPORTEE", trace, sha), "sbx-trace");
        Resultat second = service.deposer(demande("SBX_TRACE_EXPORTEE", trace, sha), "sbx-trace");
        assertThat(second.id()).isEqualTo(premier.id());
        assertThat(second.dejaConservee()).isTrue();
        assertThat(pieces.pieces).hasSize(1);
        assertThat(pieces.evenements).hasSize(1);
        // Même contenu pour une autre institution : pièce distincte (aucune fuite entre institutions).
        Demande autre = new Demande(new Fichier("t.txt", "text/plain", trace), "SBX_TRACE_EXPORTEE", sha, "99002", "trace/2");
        assertThat(service.deposer(autre, "sbx-trace").id()).isNotEqualTo(premier.id());
    }

    @Test
    @DisplayName("Critère 3 : empreinte divergente, pièce non probante, rien n'est conservé, erreur que le déposant réessaie")
    void empreinteDivergente() {
        byte[] rapport = "rapport d'ecarts pacs.008".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> service.deposer(demande("SBX_RAPPORT_ECARTS_ISO20022", rapport, "0".repeat(64)), "sbx-iso20022"))
            .isInstanceOfSatisfying(DocumentException.class, e -> {
                assertThat(e.code()).isEqualTo("DOC_SBX_EMPREINTE_DIVERGENTE");
                assertThat(e.details()).containsEntry("calculee", EmpreinteSha256.de(rapport));
            });
        verify(depot, never()).deposer(any(), any());
        assertThat(pieces.pieces).isEmpty();
        assertThat(pieces.evenements).isEmpty();
    }

    @Test
    @DisplayName("Critère 4 : trace portant un numéro de carte en clair placée en quarantaine selon la règle du type, jamais conservée")
    void numeroDeCarteEnClair() {
        Resultat r = service.deposer(demande("SBX_TRACE_EXPORTEE", TRACE_PAN, EmpreinteSha256.de(TRACE_PAN)), "sbx-trace");
        assertThat(r.etat()).isEqualTo(Etat.QUARANTAINE);
        assertThat(r.conserveeJusquAu()).isNull();
        verify(depot, never()).deposer(any(), any());
        assertThat(pieces.nombreQuarantaines()).isEqualTo(1);
        assertThat(pieces.pieces.get(0).occurrencesPan()).isEqualTo(1);
        assertThat(pieces.evenements).isEmpty();
        // Fichier remis au participant : règle REFUS, rien n'est inscrit.
        assertThatThrownBy(() -> service.deposer(demande("SBX_FICHIER_REMIS_PARTICIPANT", TRACE_PAN, EmpreinteSha256.de(TRACE_PAN)), "sbx-trace"))
            .isInstanceOfSatisfying(DocumentException.class, e -> assertThat(e.code()).isEqualTo("DOC_PAN_DETECTED"));
        assertThat(pieces.pieces).hasSize(1);
    }

    @Test
    @DisplayName("Refus avant tout contrôle : type inconnu ou inactif, déposant non admis, empreinte ou institution mal formées")
    void refus() {
        String sha = EmpreinteSha256.de(BILAN);
        assertThatThrownBy(() -> service.deposer(demande("INCONNU", BILAN, sha), "sbx-campagne")).hasFieldOrPropertyWithValue("code", "DOC_SBX_TYPE_INCONNU");
        assertThatThrownBy(() -> service.deposer(demande(null, BILAN, sha), "sbx-campagne")).hasFieldOrPropertyWithValue("code", "DOC_SBX_TYPE_INCONNU");
        var inactif = pieces.types.get("SBX_TRACE_EXPORTEE");
        pieces.types.put(inactif.code(), new cm.gimac.des.document.domain.sandbox.TypePieceSandbox(inactif.code(), inactif.libelles(), inactif.retention(), true, inactif.servicesAutorises(), inactif.reglePan(), 10, false, 1));
        assertThatThrownBy(() -> service.deposer(demande("SBX_TRACE_EXPORTEE", BILAN, sha), "sbx-trace")).hasFieldOrPropertyWithValue("code", "DOC_SBX_TYPE_INCONNU");
        assertThatThrownBy(() -> service.deposer(demande("SBX_BILAN_CERTIFICATION", BILAN, sha), "sbx-trace")).hasFieldOrPropertyWithValue("code", "DOC_SBX_DEPOSANT_NON_AUTORISE");
        assertThatThrownBy(() -> service.deposer(demande("SBX_BILAN_CERTIFICATION", BILAN, null), "sbx-campagne")).hasFieldOrPropertyWithValue("code", "DOC_SBX_EMPREINTE_INVALIDE");
        assertThatThrownBy(() -> service.deposer(demande("SBX_BILAN_CERTIFICATION", BILAN, "abc"), "sbx-campagne")).hasFieldOrPropertyWithValue("code", "DOC_SBX_EMPREINTE_INVALIDE");
        Demande sansCode = new Demande(new Fichier("b.txt", "text/plain", BILAN), "SBX_BILAN_CERTIFICATION", sha, null, "c");
        assertThatThrownBy(() -> service.deposer(sansCode, "sbx-campagne")).hasFieldOrPropertyWithValue("code", "DOC_SBX_INSTITUTION_INVALIDE");
        Demande sansFichier = new Demande(null, "SBX_BILAN_CERTIFICATION", sha, "99001", "c");
        assertThatThrownBy(() -> service.deposer(sansFichier, "sbx-campagne")).isInstanceOf(DocumentException.class);
        verify(depot, never()).deposer(any(), any());
    }
}
