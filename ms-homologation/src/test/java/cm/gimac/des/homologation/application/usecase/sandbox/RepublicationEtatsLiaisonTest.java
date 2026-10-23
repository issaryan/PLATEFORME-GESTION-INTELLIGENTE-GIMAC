package cm.gimac.des.homologation.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Republication des états de liaison pour la sandbox : toutes les pages, versions courantes et nouvelles comptées. */
class RepublicationEtatsLiaisonTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");

    @Test
    @DisplayName("Deux pages de liaisons : chacune republiée, versions nouvelles comptées à part")
    void deuxPages() {
        LiaisonRepository liaisons = mock(LiaisonRepository.class);
        PublicationEtatsLiaison publication = mock(PublicationEtatsLiaison.class);
        Liaison a = new Liaison().id(UUID.randomUUID());
        Liaison b = new Liaison().id(UUID.randomUUID());
        Liaison c = new Liaison().id(UUID.randomUUID());
        Pageable p0 = PageRequest.of(0, 2, Sort.by("id"));
        when(liaisons.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(a, b), p0, 3), new PageImpl<>(List.of(c), p0.next(), 3));
        when(publication.republier(a)).thenReturn(false);
        when(publication.republier(b)).thenReturn(true);
        when(publication.republier(c)).thenReturn(false);

        RepublicationEtatsLiaison.Rapport r = new RepublicationEtatsLiaison(liaisons, publication, Clock.fixed(T, ZoneOffset.UTC)).republier("svc-exploitation");

        assertThat(r).isEqualTo(new RepublicationEtatsLiaison.Rapport(3, 2, 1, T));
        verify(publication).republier(c);
    }
}
