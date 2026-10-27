package cm.gimac.des.certification.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.certification.AbstractCertificationIT;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.domain.enumeration.EtatCircuit;
import cm.gimac.des.certification.domain.enumeration.EtatQuitus;
import cm.gimac.des.certification.domain.enumeration.NatureQuitus;
import cm.gimac.des.certification.repository.CertificationCircuitRepository;
import cm.gimac.des.certification.repository.QuitusRepository;
import cm.gimac.des.certification.repository.SignedDecisionRepository;
import cm.gimac.des.certification.service.SignedDecisionService;
import cm.gimac.des.certification.service.dto.SignedDecisionDTO;
import cm.gimac.des.certification.service.mapper.QuitusMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** US-EXI-25 sur PostgreSQL : idempotence, conservation, caducité et refus d'une signature sur ancien socle. */
class BilanSandboxIT extends AbstractCertificationIT {

    static final String A = "a".repeat(64);
    static final String B = "b".repeat(64);

    @Autowired
    BilanSandboxConsumer consumer;

    @Autowired
    CertificationCircuitRepository circuits;

    @Autowired
    QuitusRepository quitus;

    @Autowired
    SignedDecisionRepository decisions;

    @Autowired
    SignedDecisionService decisionService;

    @Autowired
    QuitusMapper quitusMapper;

    @AfterEach
    void nettoyerCircuit() {
        decisions.deleteAll();
        quitus.deleteAll();
        circuits.deleteAll();
    }

    static String evenement(String id, UUID liaison, String empreinte) {
        return "{\"specversion\":\"1.0\",\"id\":\"" + id + "\",\"type\":\"homologation.campaign-result-imported.v1\",\"data\":{\"liaisonId\":\"" + liaison + "\",\"versionBilan\":1,\"empreinteBilan\":\"" + empreinte + "\"}}";
    }

    SignedDecisionDTO decision(Quitus q, String socle) {
        SignedDecisionDTO d = new SignedDecisionDTO();
        d.setDecisionFavorable(true);
        d.setMotif("Bilan vérifié");
        d.setSignataireId(UUID.randomUUID());
        d.setSignataireRole("CHEF_DES");
        d.setDateSignature(Instant.parse("2026-10-27T10:00:00Z"));
        d.setSocleEmpreinte(socle);
        d.setEmpreinteDecision("c".repeat(64));
        d.setSignature("assertion-webauthn");
        d.setOperationsDesignees("[]");
        d.setServicesDesignes("[]");
        d.setQuitus(quitusMapper.toDto(q));
        return d;
    }

    @Test
    @DisplayName("Doublons sans effet, même socle conservé, nouveau socle CADUC et ancienne signature refusée")
    void circuitComplet() {
        UUID liaison = UUID.randomUUID();
        String premier = UUID.randomUUID().toString();
        consumer.recevoir(evenement(premier, liaison, A));
        consumer.recevoir(evenement(premier, liaison, A));
        consumer.recevoir(evenement(UUID.randomUUID().toString(), liaison, A));

        assertThat(circuits.count()).isEqualTo(1);
        var circuit = circuits.findFirstByLiaisonIdOrderByCycleDesc(liaison).orElseThrow();
        Quitus technique = quitus.findByCircuit_Id(circuit.getId()).stream().filter(q -> q.getNature() == NatureQuitus.TECHNIQUE).findFirst().orElseThrow();
        decisionService.save(decision(technique, A));
        technique.setEtat(EtatQuitus.SIGNE_FAVORABLE);
        quitus.saveAndFlush(technique);

        consumer.recevoir(evenement(UUID.randomUUID().toString(), liaison, B));

        assertThat(circuits.count()).isEqualTo(2);
        Quitus ancien = quitus.findById(technique.getId()).orElseThrow();
        assertThat(ancien.getEtat()).isEqualTo(EtatQuitus.CADUC);
        assertThat(ancien.getMotifCaducite()).contains(A).contains(B);
        assertThat(circuits.findById(circuit.getId()).orElseThrow().getEtat()).isEqualTo(EtatCircuit.INTERROMPU);
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where event_type = 'certification.quitus-lapsed.v1'", Long.class)).isEqualTo(1);

        assertThatThrownBy(() -> decisionService.save(decision(ancien, A)))
            .isInstanceOfSatisfying(CertificationException.class, e -> {
                assertThat(e.code()).isEqualTo("CER_SOCLE_OUTDATED");
                assertThat(e.status()).isEqualTo(409);
                assertThat(e.details()).containsEntry("socleAJour", B);
            });
        assertThat(decisions.count()).isEqualTo(1);
    }
}
