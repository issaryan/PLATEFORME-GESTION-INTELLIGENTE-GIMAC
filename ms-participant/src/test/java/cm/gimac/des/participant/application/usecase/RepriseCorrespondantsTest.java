package cm.gimac.des.participant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Acteur;
import cm.gimac.des.participant.application.port.in.RepriseCorrespondantsUseCase.CorrespondantRepris;
import cm.gimac.des.participant.application.port.in.RepriseCorrespondantsUseCase.Rapport;
import cm.gimac.des.participant.application.port.in.RepriseCorrespondantsUseCase.Resultat;
import cm.gimac.des.participant.domain.Correspondent;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.repository.CorrespondentRepository;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.InteractionRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Reprise des correspondants (D-132) : bornes du lot, simulation annulée, refus de la base, sans base réelle. */
@ExtendWith(MockitoExtension.class)
class RepriseCorrespondantsTest {

    static final Acteur REPRISE = new Acteur(UUID.randomUUID(), "service-account-reprise-service");
    static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-05T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    InstitutionRepository institutions;

    @Mock
    CorrespondentRepository correspondants;

    @Mock
    InteractionRepository interactions;

    @Mock
    EntityManager em;

    @Mock
    PlatformTransactionManager tm;

    RepriseCorrespondants reprise;
    final List<SimpleTransactionStatus> statuts = new ArrayList<>();

    @BeforeEach
    void preparer() {
        reprise = new RepriseCorrespondants(institutions, correspondants, interactions, em, tm, HORLOGE);
        lenient().when(tm.getTransaction(any())).thenAnswer(inv -> {
            SimpleTransactionStatus s = new SimpleTransactionStatus();
            statuts.add(s);
            return s;
        });
        Institution i = new Institution().status(InstitutionStatus.ADHERENT);
        i.setId(UUID.randomUUID());
        i.setParticipantCode("99001");
        lenient().when(institutions.findByParticipantCode(anyString())).thenReturn(Optional.of(i));
        lenient().when(correspondants.findAll(any(Specification.class))).thenReturn(List.of());
        // Identifiant posé à la persistance, comme le générateur de JPA.
        lenient().doAnswer(inv -> {
            inv.<Correspondent>getArgument(0).setId(UUID.randomUUID());
            return null;
        }).when(em).persist(any(Correspondent.class));
    }

    static CorrespondantRepris ligne(String code) {
        return new CorrespondantRepris(code, UUID.randomUUID(), "Awa Fictive", null, null, null, null, null);
    }

    @Test
    @DisplayName("Lot vide ou trop volumineux refusé en entier")
    void bornes() {
        assertThatThrownBy(() -> reprise.importer(List.of(), true, null, REPRISE)).isInstanceOf(ParticipantException.class).hasMessageContaining("Aucun correspondant");
        assertThatThrownBy(() -> reprise.importer(null, true, null, REPRISE)).isInstanceOf(ParticipantException.class);
        List<CorrespondantRepris> trop = Collections.nCopies(RepriseCorrespondants.LIGNES_MAX + 1, ligne("99001"));
        assertThatThrownBy(() -> reprise.importer(trop, true, null, REPRISE)).isInstanceOf(ParticipantException.class).hasMessageContaining("volumineux");
    }

    @Test
    @DisplayName("Simulation : la transaction de chaque ligne est annulée ; une ligne vide est refusée")
    void simulation() {
        List<CorrespondantRepris> lignes = new ArrayList<>();
        lignes.add(ligne("99001"));
        lignes.add(null);
        Rapport r = reprise.importer(lignes, true, "x".repeat(300), REPRISE);
        assertThat(r.totaux()).containsEntry(Resultat.CREE, 1L).containsEntry(Resultat.REFUSE, 1L);
        assertThat(r.lignes().get(1).code()).isEqualTo("PAR_REPRISE_LIGNE_INVALIDE");
        assertThat(statuts).singleElement().matches(SimpleTransactionStatus::isRollbackOnly);
        verify(em).persist(any(Correspondent.class));
    }

    @Test
    @DisplayName("Écriture refusée par la base : ligne refusée, les autres continuent")
    void refusBase() {
        doThrow(new DataIntegrityViolationException("contrainte")).doNothing().when(em).flush();
        Rapport r = reprise.importer(List.of(ligne("99001"), ligne("99001")), false, null, REPRISE);
        assertThat(r.lignes().get(0).code()).isEqualTo("PAR_REPRISE_ECRITURE_REFUSEE");
        assertThat(r.lignes().get(0).details()).containsEntry("cause", "DataIntegrityViolationException");
        assertThat(r.lignes().get(1).resultat()).isEqualTo(Resultat.CREE);
    }

    @Test
    @DisplayName("Valeurs normalisées : code en majuscules, courriel en minuscules, longueurs bornées")
    void normalisation() {
        var f = RepriseCorrespondants.valider(new CorrespondantRepris(" ab-1 ", UUID.randomUUID(), "N".repeat(300), "securite", " A@B.invalid ", "1".repeat(40), " ", true));
        assertThat(f.codeMembre()).isEqualTo("AB-1");
        assertThat(f.nom()).hasSize(255);
        assertThat(f.email()).isEqualTo("a@b.invalid");
        assertThat(f.telephone()).hasSize(30);
        assertThat(f.canaux()).isEqualTo("messagerie");
        assertThat(f.astreinte()).isTrue();
        verify(interactions, never()).save(any());
    }

    @Test
    @DisplayName("Existant : inchangé, puis mis à jour et réactivé ; institution inconnue ou close refusée")
    void existantEtRefus() {
        UUID compte = UUID.randomUUID();
        Correspondent c = new Correspondent().fullName("Awa Fictive").duty(cm.gimac.des.participant.domain.enumeration.CorrespondentFunction.MONETIQUE).channels("messagerie").onCall247(false).active(true).userId(compte);
        c.setId(UUID.randomUUID());
        when(correspondants.findAll(any(Specification.class))).thenReturn(List.of(c));
        CorrespondantRepris meme = new CorrespondantRepris("99001", compte, "Awa Fictive", null, null, null, null, null);
        assertThat(reprise.importer(List.of(meme), false, null, REPRISE).lignes().get(0).resultat()).isEqualTo(Resultat.INCHANGE);
        c.setActive(false);
        CorrespondantRepris change = new CorrespondantRepris("99001", compte, "Awa Fictive", "DIRECTION", "a@b.invalid", "+237 1", "messagerie", true);
        var l = reprise.importer(List.of(change), false, null, REPRISE).lignes().get(0);
        assertThat(l.resultat()).isEqualTo(Resultat.MIS_A_JOUR);
        assertThat(l.changements()).containsExactly("fonction", "astreinte247", "actif", "email", "telephone");
        Institution close = new Institution().status(InstitutionStatus.CLOS_SANS_SUITE);
        close.setId(UUID.randomUUID());
        when(institutions.findByParticipantCode("99002")).thenReturn(Optional.of(close));
        when(institutions.findByParticipantCode("99009")).thenReturn(Optional.empty());
        Rapport r = reprise.importer(List.of(ligne("99002"), ligne("99009")), false, null, REPRISE);
        assertThat(r.lignes()).extracting(x -> x.code()).containsExactly("PAR_REPRISE_INSTITUTION_CLOSE", "PAR_REPRISE_INSTITUTION_INCONNUE");
    }

    @Test
    @DisplayName("Contrôles : champs obligatoires, fonction inconnue, courriel invalide, doublon du fichier")
    void controles() {
        UUID compte = UUID.randomUUID();
        Rapport r = reprise.importer(
            List.of(
                new CorrespondantRepris(" ", null, " ", null, null, null, null, null),
                new CorrespondantRepris("99001", UUID.randomUUID(), "N", "PRESIDENT", null, null, null, null),
                new CorrespondantRepris("99001", UUID.randomUUID(), "N", null, "pas-un-courriel", null, null, null),
                new CorrespondantRepris("99001", compte, "N", null, null, null, null, null),
                new CorrespondantRepris("99001", compte, "N", null, null, null, null, null)
            ),
            true, null, REPRISE
        );
        assertThat(r.lignes()).extracting(x -> x.code()).containsExactly(
            "PAR_REPRISE_CHAMP_OBLIGATOIRE", "PAR_REPRISE_FONCTION_INCONNUE", "PAR_REPRISE_COURRIEL_INVALIDE", null, "PAR_REPRISE_DOUBLON_FICHIER"
        );
    }
}
