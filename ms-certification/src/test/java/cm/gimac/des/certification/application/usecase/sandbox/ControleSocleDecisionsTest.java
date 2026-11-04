package cm.gimac.des.certification.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.certification.domain.CertificationCircuit;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.domain.enumeration.EtatCircuit;
import cm.gimac.des.certification.infrastructure.persistence.CircuitsLiaisonRepository;
import cm.gimac.des.certification.repository.QuitusRepository;
import cm.gimac.des.certification.service.dto.QuitusDTO;
import cm.gimac.des.certification.service.dto.SignedDecisionDTO;
import cm.gimac.des.certification.service.impl.SignedDecisionServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CER-06, US-EXI-25 : contrôle du socle posé devant le service généré des décisions signées. */
class ControleSocleDecisionsTest {

    static final String A = "a".repeat(64);
    static final String B = "b".repeat(64);
    static final UUID LIAISON = UUID.randomUUID();

    SignedDecisionServiceImpl genere;
    QuitusRepository quitus;
    CircuitsLiaisonRepository circuits;
    ControleSocleDecisions controle;
    CertificationCircuit circuit;
    UUID quitusId;

    @BeforeEach
    void init() {
        genere = mock(SignedDecisionServiceImpl.class);
        quitus = mock(QuitusRepository.class);
        circuits = mock(CircuitsLiaisonRepository.class);
        controle = new ControleSocleDecisions(genere, quitus, circuits);
        circuit = circuit(A, EtatCircuit.OUVERT);
        quitusId = UUID.randomUUID();
        when(quitus.findById(quitusId)).thenReturn(Optional.of(new Quitus().circuit(circuit)));
    }

    static CertificationCircuit circuit(String socle, EtatCircuit etat) {
        CertificationCircuit c = new CertificationCircuit().liaisonId(LIAISON).socleEmpreinte(socle).etat(etat);
        c.setId(UUID.randomUUID());
        return c;
    }

    SignedDecisionDTO decision(UUID quitus, String socle) {
        QuitusDTO q = new QuitusDTO();
        q.setId(quitus);
        SignedDecisionDTO d = new SignedDecisionDTO();
        d.setQuitus(q);
        d.setSocleEmpreinte(socle);
        return d;
    }

    static void socleObsolete(Runnable r, String aJour) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(CertificationException.class, e -> {
            assertThat(e.code()).isEqualTo("CER_SOCLE_OUTDATED");
            assertThat(e.details()).containsEntry("socleAJour", aJour);
        });
    }

    @Test
    @DisplayName("Socle du circuit ouvert courant : décision transmise au service généré, circuit seul de sa liaison compris")
    void socleCourant() {
        SignedDecisionDTO d = decision(quitusId, A);
        when(genere.save(d)).thenReturn(d);
        when(circuits.findFirstByLiaisonIdOrderByCycleDesc(LIAISON)).thenReturn(Optional.of(circuit));
        assertThat(controle.save(d)).isSameAs(d);
        when(circuits.findFirstByLiaisonIdOrderByCycleDesc(LIAISON)).thenReturn(Optional.empty());
        assertThat(controle.save(d)).isSameAs(d);
    }

    @Test
    @DisplayName("Quitus inconnu, circuit remplacé, circuit clos ou empreinte différente : refus, rien n'est enregistré")
    void refus() {
        assertThatThrownBy(() -> controle.save(decision(UUID.randomUUID(), A)))
            .isInstanceOfSatisfying(CertificationException.class, e -> assertThat(e.code()).isEqualTo("CER_QUITUS_NOT_FOUND"));
        when(circuits.findFirstByLiaisonIdOrderByCycleDesc(LIAISON)).thenReturn(Optional.of(circuit(B, EtatCircuit.OUVERT)));
        socleObsolete(() -> controle.save(decision(quitusId, A)), B);
        when(circuits.findFirstByLiaisonIdOrderByCycleDesc(LIAISON)).thenReturn(Optional.of(circuit));
        socleObsolete(() -> controle.save(decision(quitusId, B)), A);
        circuit.setEtat(EtatCircuit.INTERROMPU);
        socleObsolete(() -> controle.save(decision(quitusId, A)), A);
        verify(genere, never()).save(any());
    }

    @Test
    @DisplayName("Autres opérations déléguées telles quelles au service généré")
    void delegation() {
        SignedDecisionDTO d = decision(quitusId, A);
        UUID id = UUID.randomUUID();
        when(genere.update(d)).thenReturn(d);
        when(genere.partialUpdate(d)).thenReturn(Optional.of(d));
        when(genere.findAll()).thenReturn(List.of(d));
        when(genere.findOne(id)).thenReturn(Optional.of(d));
        assertThat(controle.update(d)).isSameAs(d);
        assertThat(controle.partialUpdate(d)).containsSame(d);
        assertThat(controle.findAll()).containsExactly(d);
        assertThat(controle.findOne(id)).containsSame(d);
        controle.delete(id);
        verify(genere).delete(id);
    }
}
