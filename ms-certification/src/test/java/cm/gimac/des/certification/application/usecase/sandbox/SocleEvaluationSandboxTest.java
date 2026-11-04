package cm.gimac.des.certification.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.certification.domain.CertificationCircuit;
import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.domain.enumeration.EtatCircuit;
import cm.gimac.des.certification.domain.enumeration.EtatQuitus;
import cm.gimac.des.certification.domain.enumeration.NatureQuitus;
import cm.gimac.des.certification.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.certification.infrastructure.persistence.CircuitsLiaisonRepository;
import cm.gimac.des.certification.infrastructure.persistence.QuitusCircuitRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** US-EXI-25/CER-06 : règle de conservation et de caducité par empreinte du bilan. */
class SocleEvaluationSandboxTest {

    static final UUID LIAISON = UUID.randomUUID();
    static final Instant T = Instant.parse("2026-10-27T09:00:00Z");
    static final String A = "a".repeat(64);
    static final String B = "b".repeat(64);

    CircuitsLiaisonRepository circuits;
    QuitusCircuitRepository quitus;
    OutboxPublisher outbox;
    SocleEvaluationSandbox service;

    @BeforeEach
    void init() {
        circuits = mock(CircuitsLiaisonRepository.class);
        quitus = mock(QuitusCircuitRepository.class);
        outbox = mock(OutboxPublisher.class);
        service = new SocleEvaluationSandbox(circuits, quitus, outbox, Clock.fixed(T, ZoneOffset.UTC));
        when(circuits.save(any())).thenAnswer(i -> {
            CertificationCircuit c = i.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });
        when(quitus.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Premier bilan : circuit ouvert sur la version du bilan avec ses quatre quitus")
    void premierSocle() {
        when(circuits.findFirstByLiaisonIdOrderByCycleDesc(LIAISON)).thenReturn(Optional.empty());

        assertThat(service.reprendre(LIAISON, 2, A)).isEqualTo(SocleEvaluationSandbox.Issue.OUVERT);

        ArgumentCaptor<CertificationCircuit> captor = ArgumentCaptor.forClass(CertificationCircuit.class);
        verify(circuits).save(captor.capture());
        assertThat(captor.getValue().getCycle()).isEqualTo(2);
        assertThat(captor.getValue().getSocleEmpreinte()).isEqualTo(A);
        verify(quitus, org.mockito.Mockito.times(4)).save(any());
        verify(outbox, never()).publish(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(UUID.class), org.mockito.ArgumentMatchers.anyString(), any());
    }

    @Test
    @DisplayName("Même empreinte, y compris avec une casse différente : quitus et circuit conservés")
    void socleInchange() {
        CertificationCircuit courant = new CertificationCircuit().id(UUID.randomUUID()).liaisonId(LIAISON).cycle(1).socleEmpreinte(A).etat(EtatCircuit.OUVERT);
        when(circuits.findFirstByLiaisonIdOrderByCycleDesc(LIAISON)).thenReturn(Optional.of(courant));

        assertThat(service.reprendre(LIAISON, 1, A.toUpperCase(java.util.Locale.ROOT))).isEqualTo(SocleEvaluationSandbox.Issue.CONSERVE);

        verify(circuits, never()).save(any());
        verify(quitus, never()).save(any());
        verify(outbox, never()).publish(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(UUID.class), org.mockito.ArgumentMatchers.anyString(), any());
    }

    @Test
    @DisplayName("Empreinte changée : favorable antérieur CADUC, défavorable inchangé, événement unique")
    void socleChange() {
        CertificationCircuit ancien = new CertificationCircuit().id(UUID.randomUUID()).liaisonId(LIAISON).cycle(1).socleEmpreinte(A).etat(EtatCircuit.OUVERT);
        Quitus favorable = new Quitus().id(UUID.randomUUID()).circuit(ancien).nature(NatureQuitus.TECHNIQUE).etat(EtatQuitus.SIGNE_FAVORABLE);
        Quitus defavorable = new Quitus().id(UUID.randomUUID()).circuit(ancien).nature(NatureQuitus.SECURITE).etat(EtatQuitus.SIGNE_DEFAVORABLE);
        when(circuits.findFirstByLiaisonIdOrderByCycleDesc(LIAISON)).thenReturn(Optional.of(ancien));
        when(quitus.findByCircuit_Id(ancien.getId())).thenReturn(List.of(favorable, defavorable));

        assertThat(service.reprendre(LIAISON, 2, B)).isEqualTo(SocleEvaluationSandbox.Issue.OUVERT);

        assertThat(favorable.getEtat()).isEqualTo(EtatQuitus.CADUC);
        assertThat(favorable.getMotifCaducite()).contains(A).contains(B);
        assertThat(defavorable.getEtat()).isEqualTo(EtatQuitus.SIGNE_DEFAVORABLE);
        assertThat(ancien.getEtat()).isEqualTo(EtatCircuit.INTERROMPU);
        assertThat(ancien.getDateFin()).isEqualTo(T);
        verify(outbox).publish(org.mockito.ArgumentMatchers.eq("Quitus"), org.mockito.ArgumentMatchers.eq(ancien.getId()), org.mockito.ArgumentMatchers.eq(SocleEvaluationSandbox.QUITUS_CADUCS), org.mockito.ArgumentMatchers.argThat((SocleEvaluationSandbox.QuitusCaducs p) ->
            p.natures().equals(List.of(NatureQuitus.TECHNIQUE)) && p.cycle() == 1 && p.caducLe().equals(T)
        ));
    }

    @Test
    @DisplayName("Nouveau socle sans favorable : cycle ouvert sans événement de caducité")
    void socleChangeSansFavorable() {
        CertificationCircuit ancien = new CertificationCircuit().id(UUID.randomUUID()).liaisonId(LIAISON).cycle(4).socleEmpreinte(A).etat(EtatCircuit.OUVERT);
        when(circuits.findFirstByLiaisonIdOrderByCycleDesc(LIAISON)).thenReturn(Optional.of(ancien));
        when(quitus.findByCircuit_Id(ancien.getId())).thenReturn(List.of());

        service.reprendre(LIAISON, null, B);

        verify(circuits, org.mockito.Mockito.times(2)).save(any());
        verify(outbox, never()).publish(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(UUID.class), org.mockito.ArgumentMatchers.anyString(), any());
    }

    @Test
    @DisplayName("Événement historique sans empreinte : aucun effet ; empreinte invalide : rejet")
    void retrocompatibiliteEtValidation() {
        assertThat(service.reprendre(LIAISON, 1, null)).isEqualTo(SocleEvaluationSandbox.Issue.SANS_EMPREINTE);
        assertThatThrownBy(() -> service.reprendre(LIAISON, 1, "abc")).isInstanceOf(IllegalArgumentException.class);
        verify(circuits, never()).save(any());
        verify(quitus, never()).save(any());
    }
}
