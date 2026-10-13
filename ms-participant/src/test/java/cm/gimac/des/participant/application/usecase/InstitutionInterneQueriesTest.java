package cm.gimac.des.participant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.SubscribedService;
import cm.gimac.des.participant.domain.enumeration.SubscriptionState;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.SubscribedServiceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionInterneQueriesTest {

    @Mock
    InstitutionRepository institutions;

    @Mock
    SubscribedServiceRepository services;

    @Test
    @DisplayName("Institution et ses seuls services souscrits actifs ; inconnue : PAR_INSTITUTION_NOT_FOUND")
    void lecture() {
        UUID id = UUID.randomUUID();
        Institution i = new Institution().name("Banque B");
        i.setId(id);
        SubscribedService s = new SubscribedService().serviceCode("CARTE_INTERBANCAIRE");
        when(institutions.findById(id)).thenReturn(Optional.of(i));
        when(services.findByInstitution_IdAndStateOrderByServiceCode(id, SubscriptionState.ACTIF)).thenReturn(List.of(s));
        var q = new InstitutionInterneQueries(institutions, services);
        assertThat(q.lire(id).institution()).isSameAs(i);
        assertThat(q.lire(id).servicesSouscrits()).containsExactly(s);
        UUID autre = UUID.randomUUID();
        assertThatThrownBy(() -> q.lire(autre)).isInstanceOfSatisfying(ParticipantException.class, e -> assertThat(e.code()).isEqualTo("PAR_INSTITUTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("Lecture par code membre (D-107) : institution du code, sinon PAR_INSTITUTION_NOT_FOUND")
    void parCode() {
        UUID id = UUID.randomUUID();
        Institution i = new Institution().name("Banque B");
        i.setId(id);
        when(institutions.findByParticipantCode("BQ-1")).thenReturn(Optional.of(i));
        when(institutions.findById(id)).thenReturn(Optional.of(i));
        var q = new InstitutionInterneQueries(institutions, services);
        assertThat(q.parCode(" BQ-1 ").institution()).isSameAs(i);
        when(institutions.findByParticipantCode("")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> q.parCode(null)).isInstanceOfSatisfying(ParticipantException.class, e -> assertThat(e.code()).isEqualTo("PAR_INSTITUTION_NOT_FOUND"));
    }
}
