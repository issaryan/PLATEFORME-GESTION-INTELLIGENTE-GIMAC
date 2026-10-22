package cm.gimac.des.participant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.participant.AbstractParticipantIT;
import cm.gimac.des.participant.application.port.out.VersionsStatutRegistre;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Versions de statut dans PostgreSQL (Testcontainers, table Liquibase institution_version_statut, US-EXI-20). */
class VersionsStatutRegistreJdbcIT extends AbstractParticipantIT {

    @Autowired
    VersionsStatutRegistre versions;

    @Autowired
    InstitutionRepository institutions;

    @Autowired
    PlatformTransactionManager tm;

    static Institution institution(String code) {
        return new Institution()
            .name("Banque fictive " + code)
            .country("CM")
            .category(InstitutionCategory.BANQUE)
            .status(InstitutionStatus.ACTIF)
            .participantCode(code)
            .complianceState(ComplianceState.CONFORME)
            .openLinkCount(0)
            .openSuspenseCount(0)
            .createdAt(Instant.parse("2026-10-22T08:00:00Z"));
    }

    @Test
    @DisplayName("Incrément strict par institution dans la transaction ; 0 sans changement ; hors transaction refusé")
    void incrementStrict() {
        Institution a = institutions.saveAndFlush(institution("99001"));
        Institution b = institutions.saveAndFlush(institution("99002"));
        TransactionTemplate tx = new TransactionTemplate(tm);
        assertThat(versions).isInstanceOf(VersionsStatutRegistreJdbc.class);
        assertThat(versions.courante(a.getId())).isZero();
        assertThat(tx.<Long>execute(s -> versions.suivante(a.getId()))).isEqualTo(1L);
        assertThat(tx.<Long>execute(s -> versions.suivante(a.getId()))).isEqualTo(2L);
        assertThat(tx.<Long>execute(s -> versions.suivante(b.getId()))).isEqualTo(1L);
        // Transaction annulée : la version n'est pas consommée.
        tx.executeWithoutResult(s -> {
            versions.suivante(a.getId());
            s.setRollbackOnly();
        });
        assertThat(versions.courante(a.getId())).isEqualTo(2L);
        assertThat(versions.courante(UUID.randomUUID())).isZero();
        assertThatThrownBy(() -> versions.suivante(a.getId())).isInstanceOf(IllegalTransactionStateException.class);
    }
}
