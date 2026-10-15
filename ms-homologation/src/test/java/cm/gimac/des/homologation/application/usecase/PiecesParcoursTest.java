package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Preuve;
import cm.gimac.des.homologation.infrastructure.client.DocumentStubAdapter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pièces et chronologie du dossier (D-109) : pièce rattachée à l'opération, connue du registre, empreinte revérifiée. */
class PiecesParcoursTest {

    static final LocalDate J = LocalDate.parse("2026-10-15");

    ParcoursDepot depot;
    DocumentStubAdapter documents;
    PiecesParcours pieces;
    Parcours p;
    OperationParcours o3;

    static void refus(ThrowingCallable c, String code) {
        assertThatThrownBy(c).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @BeforeEach
    void init() {
        depot = mock(ParcoursDepot.class);
        documents = new DocumentStubAdapter();
        pieces = new PiecesParcours(depot, documents);
        p = ParcoursSamples.parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        o3 = p.operation("3").orElseThrow();
        when(depot.deLOperationLecture(o3.getId())).thenReturn(p);
        when(depot.trouver(p.id(), false)).thenReturn(Optional.of(p));
    }

    @Test
    @DisplayName("Pièce de la preuve : contenu, nom, type et empreinte recalculée égale à celle inscrite")
    void conforme() {
        var d = documents.deposer("capture du ping".getBytes(StandardCharsets.UTF_8), "ping.png", "image/png", "PREUVE", "x", null);
        p.deposerPreuve(o3, Preuve.piece(d.id().toString(), "ping.png", d.sha256(), J, "Agent"), null, null, null);
        PiecesParcours.PieceServie s = pieces.piece(o3.getId(), d.id().toString());
        assertThat(s.nomFichier()).isEqualTo("ping.png");
        assertThat(s.typeMime()).isEqualTo("image/png");
        assertThat(s.empreinte()).isEqualTo(d.sha256());
        assertThat(new String(s.octets(), StandardCharsets.UTF_8)).isEqualTo("capture du ping");
    }

    @Test
    @DisplayName("Pièce non rattachée, inconnue du registre, identifiant illisible, empreinte divergente : refus")
    void refus() {
        var d = documents.deposer("contenu".getBytes(StandardCharsets.UTF_8), "cr.pdf", "application/pdf", "LIVRABLE", "x", null);
        refus(() -> pieces.piece(o3.getId(), d.id().toString()), "HOM_PIECE_INCONNUE");
        refus(() -> pieces.piece(o3.getId(), " "), "HOM_PIECE_INCONNUE");
        p.deposerPreuve(o3, Preuve.piece("PV-LIA", "pv.pdf", "c".repeat(64), J, "Agent"), null, null, null);
        refus(() -> pieces.piece(o3.getId(), "PV-LIA"), "HOM_PIECE_INCONNUE");
        UUID absent = UUID.randomUUID();
        o3.setPreuve(Preuve.piece(absent.toString(), "x.pdf", "c".repeat(64), J, "Agent"));
        refus(() -> pieces.piece(o3.getId(), absent.toString()), "HOM_PIECE_INCONNUE");
        o3.setPreuve(Preuve.piece(d.id().toString(), "cr.pdf", "d".repeat(64), J, "Agent"));
        refus(() -> pieces.piece(o3.getId(), d.id().toString()), "HOM_DOCUMENT_HASH_MISMATCH");
    }

    @Test
    @DisplayName("Pièce d'un dépôt de livrable ; chronologie lue dans le parcours ; liaison sans parcours : vide")
    void depotEtChronologie() {
        var d = documents.deposer("cr".getBytes(StandardCharsets.UTF_8), "cr.pdf", "application/pdf", "LIVRABLE", "x", null);
        OperationParcours o = p.operation("3").orElseThrow();
        p.deposerPreuve(o, new cm.gimac.des.homologation.domain.parcours.Livrable("L1", "Fiche", cm.gimac.des.homologation.domain.parcours.NatureLivrable.LIVRABLE, null, null, true), Preuve.piece(d.id().toString(), "cr.pdf", d.sha256(), J, "Agent"), null, null, null);
        assertThat(PiecesParcours.empreinteInscrite(o, d.id().toString())).isEqualTo(d.sha256());
        assertThat(pieces.piece(o.getId(), d.id().toString()).empreinte()).isEqualTo(d.sha256());
        assertThat(pieces.chronologie(p.id())).isNotEmpty();
        UUID autre = UUID.randomUUID();
        when(depot.trouver(autre, false)).thenReturn(Optional.empty());
        assertThat(pieces.chronologie(autre)).isEmpty();
    }
}
