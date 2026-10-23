package cm.gimac.des.homologation.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Campagne;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Piece;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Taux;
import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.sandbox.PieceSandbox;
import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.repository.PieceSandboxRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

/** Pièces (contrôle d'empreinte, pas de redépôt) et mesures de certification déduites des taux restitués (US-EXI-14). */
class PiecesEtCouverturesSandboxTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");
    static final byte[] OCTETS = "bilan".getBytes(StandardCharsets.UTF_8);
    static final String HEX = ConservationPiecesSandbox.sha256(OCTETS);

    @Test
    @DisplayName("HEAD divergent de la liste : rien n'est déposé ; inscription concurrente : comptée une seule fois ; nom par défaut")
    void pieces() {
        LectureSandboxPort lecture = mock(LectureSandboxPort.class);
        DocumentRegistryPort documents = mock(DocumentRegistryPort.class);
        PieceSandboxRepository repo = mock(PieceSandboxRepository.class);
        when(repo.findFirstByEmpreinteAndDocumentIdIsNotNull(anyString())).thenReturn(Optional.empty());
        when(documents.parEmpreinte(anyString())).thenReturn(Optional.empty());
        when(documents.deposer(any(), anyString(), anyString(), anyString(), anyString(), any())).thenReturn(new DocumentRegistryPort.Depot(UUID.randomUUID(), HEX, "n"));
        when(lecture.pieces("C")).thenReturn(List.of(new Piece("BILAN", 1, HEX, 5, null), new Piece("BILAN", 2, HEX, 5, null)));
        when(lecture.telecharger(eq("C"), anyString(), any(Integer.class))).thenReturn(OCTETS);
        when(lecture.empreinte("C", "BILAN", 1)).thenReturn("ff".repeat(32));
        when(lecture.empreinte("C", "BILAN", 2)).thenReturn(HEX);
        when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0)).thenThrow(new DataIntegrityViolationException("uk_sbx_piece"));
        ConservationPiecesSandbox c = new ConservationPiecesSandbox(lecture, documents, repo, ReleveSandboxTest.transactions(), Clock.fixed(T, ZoneOffset.UTC));

        assertThat(c.conserver("C")).isEqualTo(1);

        ArgumentCaptor<String> nom = ArgumentCaptor.forClass(String.class);
        verify(documents).deposer(eq(OCTETS), nom.capture(), eq("application/json"), eq(ConservationPiecesSandbox.FINALITE), eq("C/BILAN/2"), eq(null));
        assertThat(nom.getValue()).isEqualTo("bilan-v2");
    }

    @Test
    @DisplayName("Pièce de même empreinte déjà déposée par une relève : référence reprise, aucun appel au registre")
    void dejaDeposeeParUneReleve() {
        LectureSandboxPort lecture = mock(LectureSandboxPort.class);
        DocumentRegistryPort documents = mock(DocumentRegistryPort.class);
        PieceSandboxRepository repo = mock(PieceSandboxRepository.class);
        UUID doc = UUID.randomUUID();
        when(repo.findFirstByEmpreinteAndDocumentIdIsNotNull(HEX)).thenReturn(Optional.of(new PieceSandbox("X", "BILAN", 1, HEX, 5, "n", doc, PieceSandbox.Statut.DEPOSEE, T)));
        when(lecture.pieces("C")).thenReturn(List.of(new Piece("BILAN", 1, HEX, 5, "bilan.json")));
        when(lecture.empreinte("C", "BILAN", 1)).thenReturn(HEX);
        when(lecture.telecharger("C", "BILAN", 1)).thenReturn(OCTETS);
        when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        new ConservationPiecesSandbox(lecture, documents, repo, ReleveSandboxTest.transactions(), Clock.fixed(T, ZoneOffset.UTC)).conserver("C");
        ArgumentCaptor<PieceSandbox> p = ArgumentCaptor.forClass(PieceSandbox.class);
        verify(repo).saveAndFlush(p.capture());
        assertThat(p.getValue().getDocumentId()).isEqualTo(doc);
        assertThat(p.getValue().getStatut()).isEqualTo(PieceSandbox.Statut.DEJA_CONSERVEE);
        assertThat(p.getValue().getNom()).isEqualTo("bilan.json");
        assertThat(p.getValue().getTaille()).isEqualTo(5);
        assertThat(p.getValue().getReleveLe()).isEqualTo(T);
        assertThat(p.getValue().getId()).isNotNull();
        assertThat(p.getValue().getCampagneId()).isEqualTo("C");
        assertThat(p.getValue().getVersion()).isEqualTo(1);
        assertThat(p.getValue().getEmpreinte()).isEqualTo(HEX);
        assertThat(p.getValue().getNature()).isEqualTo("BILAN");
        verify(documents, never()).parEmpreinte(anyString());
    }

    @Test
    @DisplayName("Couvertures : campagne terminée seulement, taux de réussite par service, jamais remplacés par plus ancien")
    void couvertures() {
        CoverageRepository repo = mock(CoverageRepository.class);
        CouverturesSandbox c = new CouverturesSandbox(repo);
        Liaison l = new Liaison().id(UUID.randomUUID());
        Taux retrait = new Taux("RETRAIT", "REUSSITE", 10, 10, "100.00", "1.0", T);
        Taux transfert = new Taux("TRANSFERT", "REUSSITE", 9, 10, "90.00", "1.0", T);
        Taux campagne = new Taux(null, "REUSSITE", 19, 20, "95.00", "1.0", T);
        Taux avancement = new Taux("RETRAIT", "AVANCEMENT", 10, 10, "100.00", "1.0", T);
        Campagne terminee = new Campagne("C", "CMP", l.getId().toString(), "99001", "MOB", List.of(), "TERMINEE", List.of(retrait, transfert, campagne, avancement), 1, "1.0");
        when(repo.findFirstByLiaison_IdAndNatureAndServiceCode(l.getId(), NatureCouverture.CERTIFICATION, "RETRAIT")).thenReturn(Optional.of(new Coverage().serviceCode("RETRAIT").dateCalcul(T)));
        when(repo.findFirstByLiaison_IdAndNatureAndServiceCode(l.getId(), NatureCouverture.CERTIFICATION, "TRANSFERT")).thenReturn(Optional.empty());

        assertThat(c.appliquer(l, new Campagne("C", "CMP", l.getId().toString(), "99001", "MOB", List.of(), "EN_COURS", List.of(retrait), null, null))).isZero();
        assertThat(c.appliquer(l, terminee)).isEqualTo(1);
        ArgumentCaptor<Coverage> cov = ArgumentCaptor.forClass(Coverage.class);
        verify(repo).save(cov.capture());
        assertThat(cov.getValue().getServiceCode()).isEqualTo("TRANSFERT");
        assertThat(cov.getValue().getTaux()).isEqualByComparingTo("90.00");
        assertThat(cov.getValue().getCalculable()).isTrue();
    }
}
