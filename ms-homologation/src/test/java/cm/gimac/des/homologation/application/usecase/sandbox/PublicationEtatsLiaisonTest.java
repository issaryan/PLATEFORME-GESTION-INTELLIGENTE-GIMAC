package cm.gimac.des.homologation.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.event.LiaisonOpenedPayload;
import cm.gimac.des.homologation.domain.event.LiaisonStatusChangedPayload;
import cm.gimac.des.homologation.domain.sandbox.EtatLiaisonSandbox;
import cm.gimac.des.homologation.domain.sandbox.StatutSandbox;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.EtatLiaisonSandboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** US-EXI-16 : publication de l'ouverture et des changements d'état d'une liaison pour la Sandbox GIMAC. */
class PublicationEtatsLiaisonTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");
    static final UUID INSTITUTION = UUID.fromString("00000000-0000-4000-8000-000000099001");

    EtatLiaisonSandboxRepository etats;
    OutboxPublisher outbox;
    ParticipantDirectoryPort participants;
    PublicationEtatsLiaison publication;

    @BeforeEach
    void init() {
        etats = mock(EtatLiaisonSandboxRepository.class);
        when(etats.save(any(EtatLiaisonSandbox.class))).thenAnswer(i -> i.getArgument(0));
        outbox = mock(OutboxPublisher.class);
        participants = mock(ParticipantDirectoryPort.class);
        when(participants.getInstitution(INSTITUTION)).thenReturn(new InstitutionView(INSTITUTION, "Institution 99001", "EN_HOMOLOGATION", "BANQUE", List.of(), "99001"));
        publication = new PublicationEtatsLiaison(etats, outbox, participants, Clock.fixed(T, ZoneOffset.UTC));
    }

    @AfterEach
    void fin() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
    }

    static Liaison liaison(EtatLiaison etat) {
        return new Liaison().id(UUID.randomUUID()).institutionId(INSTITUTION).typeProjet(TypeProjet.PAIEMENT_MOBILE).etat(etat);
    }

    static LiaisonOpenedPayload charge(Liaison l) {
        return new LiaisonOpenedPayload(l.getId(), INSTITUTION, "PAIEMENT_MOBILE", UUID.randomUUID(), 1, UUID.randomUUID(), null, List.of("TRANSVERSE"), UUID.randomUUID(), "PAIEMENT_MOBILE");
    }

    LiaisonStatusChangedPayload changement() {
        ArgumentCaptor<Object> c = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("liaison"), any(UUID.class), eq(LiaisonStatusChangedPayload.TYPE), c.capture(), eq(INSTITUTION.toString()));
        return (LiaisonStatusChangedPayload) c.getValue();
    }

    @Test
    @DisplayName("CA1 nominal : ouverture d'une liaison de 99001, liaison-opened puis liaison-status-changed de version 1")
    void ca1OuvertureDeLaLiaison() {
        Liaison l = liaison(EtatLiaison.OUVERTE);
        publication.ouverture(l, "99001", charge(l));

        ArgumentCaptor<Object> ouverte = ArgumentCaptor.forClass(Object.class);
        InOrder ordre = Mockito.inOrder(outbox);
        ordre.verify(outbox).publish(eq("liaison"), eq(l.getId()), eq(LiaisonOpenedPayload.TYPE), ouverte.capture(), eq(INSTITUTION.toString()));
        ordre.verify(outbox).publish(eq("liaison"), eq(l.getId()), eq(LiaisonStatusChangedPayload.TYPE), any(), eq(INSTITUTION.toString()));
        LiaisonOpenedPayload o = (LiaisonOpenedPayload) ouverte.getValue();
        assertThat(o.codeParticipant()).isEqualTo("99001");
        assertThat(o.versionEtat()).isEqualTo(1L);
        assertThat(o.typeProjet()).isEqualTo("PAIEMENT_MOBILE");
        LiaisonStatusChangedPayload s = changement();
        assertThat(s.liaisonId()).isEqualTo(l.getId());
        assertThat(s.institutionId()).isEqualTo(INSTITUTION);
        assertThat(s.codeParticipant()).isEqualTo("99001");
        assertThat(s.typeProjet()).isEqualTo("PAIEMENT_MOBILE");
        assertThat(s.statut()).isEqualTo("OUVERTE");
        assertThat(s.statutPrecedent()).isNull();
        assertThat(s.etatLiaison()).isEqualTo("OUVERTE");
        assertThat(s.versionEtat()).isEqualTo(1L);
        assertThat(s.survenuLe()).isEqualTo(T);
        verify(participants, never()).getInstitution(any());
    }

    @Test
    @DisplayName("CA2 alternatif : gel de la liaison, liaison-status-changed porte une version d'état supérieure")
    void ca2GelDeLaLiaison() {
        Liaison l = liaison(EtatLiaison.GELEE);
        when(etats.verrouiller(l.getId())).thenReturn(Optional.of(new EtatLiaisonSandbox(l.getId(), "99001", StatutSandbox.OUVERTE, T.minusSeconds(60))));

        assertThat(publication.constater(l)).isTrue();

        LiaisonStatusChangedPayload s = changement();
        assertThat(s.statut()).isEqualTo("GELEE");
        assertThat(s.statutPrecedent()).isEqualTo("OUVERTE");
        assertThat(s.versionEtat()).isEqualTo(2L);
        assertThat(s.survenuLe()).isEqualTo(T);
    }

    @Test
    @DisplayName("Clôture : liaison en production, statut CLOSE, version suivante")
    void clotureDeLaLiaison() {
        Liaison l = liaison(EtatLiaison.CLOSE);
        EtatLiaisonSandbox gelee = new EtatLiaisonSandbox(l.getId(), "99001", StatutSandbox.OUVERTE, T);
        gelee.constater(StatutSandbox.GELEE, T);
        when(etats.verrouiller(l.getId())).thenReturn(Optional.of(gelee));

        assertThat(publication.constater(l)).isTrue();

        LiaisonStatusChangedPayload s = changement();
        assertThat(s.statut()).isEqualTo("CLOSE");
        assertThat(s.statutPrecedent()).isEqualTo("GELEE");
        assertThat(s.versionEtat()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Statut inchangé (en homologation, apte : ouverte pour la sandbox) : aucun événement, version gardée")
    void statutInchange() {
        Liaison l = liaison(EtatLiaison.APTE_CERTIFICATION);
        EtatLiaisonSandbox connu = new EtatLiaisonSandbox(l.getId(), "99001", StatutSandbox.OUVERTE, T);
        when(etats.verrouiller(l.getId())).thenReturn(Optional.of(connu));

        assertThat(publication.constater(l)).isFalse();

        verify(outbox, never()).publish(anyString(), any(UUID.class), anyString(), any(), anyString());
        assertThat(connu.getVersionEtat()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Liaison antérieure à US-EXI-16 : version 1 à sa première écriture, code relu au registre")
    void liaisonAnterieure() {
        Liaison l = liaison(EtatLiaison.EN_HOMOLOGATION);
        when(etats.verrouiller(l.getId())).thenReturn(Optional.empty());

        assertThat(publication.constater(l)).isTrue();

        LiaisonStatusChangedPayload s = changement();
        assertThat(s.versionEtat()).isEqualTo(1L);
        assertThat(s.codeParticipant()).isEqualTo("99001");
        assertThat(s.etatLiaison()).isEqualTo("EN_HOMOLOGATION");
    }

    @Test
    @DisplayName("Registre injoignable : l'événement part sans code, le code est complété à la publication suivante")
    void registreInjoignable() {
        Liaison l = liaison(EtatLiaison.GELEE);
        when(participants.getInstitution(INSTITUTION)).thenThrow(HomologationException.conflict("HOM_LIAISON_INSTITUTION_UNKNOWN", "inconnue", Map.of()));
        EtatLiaisonSandbox sansCode = new EtatLiaisonSandbox(l.getId(), null, StatutSandbox.OUVERTE, T);
        when(etats.verrouiller(l.getId())).thenReturn(Optional.of(sansCode));

        assertThat(publication.constater(l)).isTrue();
        assertThat(changement().codeParticipant()).isNull();

        // Registre revenu : le code est complété, jamais remplacé ensuite.
        Mockito.doReturn(new InstitutionView(INSTITUTION, "Institution 99001", "ACTIF", "BANQUE", List.of(), "99001")).when(participants).getInstitution(INSTITUTION);
        l.setEtat(EtatLiaison.EN_HOMOLOGATION);
        assertThat(publication.constater(l)).isTrue();
        assertThat(sansCode.getCodeParticipant()).isEqualTo("99001");
        sansCode.completerCode("99999");
        assertThat(sansCode.getCodeParticipant()).isEqualTo("99001");
    }

    @Test
    @DisplayName("Code inconnu à l'ouverture : relu au registre")
    void ouvertureSansCode() {
        Liaison l = liaison(EtatLiaison.OUVERTE);
        publication.ouverture(l, null, charge(l));
        assertThat(changement().codeParticipant()).isEqualTo("99001");
    }

    @Test
    @DisplayName("Service supplémentaire : code de la liaison principale, version 1 pour la nouvelle liaison")
    void serviceSupplementaire() {
        UUID principale = UUID.randomUUID();
        when(etats.findById(principale)).thenReturn(Optional.of(new EtatLiaisonSandbox(principale, "99001", StatutSandbox.CLOSE, T)));
        ReferentialVersion v = new ReferentialVersion();
        v.setId(UUID.randomUUID());
        v.setNumero(2);
        Liaison n = liaison(EtatLiaison.OUVERTE).version(v).chefDeProjetId(UUID.randomUUID());

        publication.ouvertureServiceSupplementaire(n, principale, List.of("TRANSVERSE", "QR"), UUID.randomUUID());

        ArgumentCaptor<Object> ouverte = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("liaison"), eq(n.getId()), eq(LiaisonOpenedPayload.TYPE), ouverte.capture(), eq(INSTITUTION.toString()));
        LiaisonOpenedPayload o = (LiaisonOpenedPayload) ouverte.getValue();
        assertThat(o.codeParticipant()).isEqualTo("99001");
        assertThat(o.versionEtat()).isEqualTo(1L);
        assertThat(o.versionNumero()).isEqualTo(2);
        assertThat(o.perimetre()).containsExactly("TRANSVERSE", "QR");
        verify(participants, never()).getInstitution(any());
    }

    @Test
    @DisplayName("Observation : sans transaction ou en lecture seule, rien n'est enregistré")
    void observationHorsEcriture() {
        publication.observer(liaison(EtatLiaison.GELEE));
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        publication.observer(liaison(EtatLiaison.GELEE));
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    @DisplayName("Observation : une seule constatation par liaison avant la validation, aucune en lecture seule")
    void observationAvantValidation() {
        TransactionSynchronizationManager.initSynchronization();
        Liaison a = liaison(EtatLiaison.GELEE);
        Liaison b = liaison(EtatLiaison.CLOSE);
        when(etats.verrouiller(any())).thenReturn(Optional.empty());

        publication.observer(a);
        publication.observer(a);
        publication.observer(b);
        List<TransactionSynchronization> s = TransactionSynchronizationManager.getSynchronizations();
        assertThat(s).hasSize(1);

        s.get(0).beforeCommit(true);
        verify(etats, never()).verrouiller(any());

        s.get(0).beforeCommit(false);
        verify(etats, times(1)).verrouiller(a.getId());
        verify(etats, times(1)).verrouiller(b.getId());
        verify(outbox, times(2)).publish(eq("liaison"), any(UUID.class), eq(LiaisonStatusChangedPayload.TYPE), any(), eq(INSTITUTION.toString()));
    }

    @Test
    @DisplayName("Ouverture dans une transaction : la liaison reste observée jusqu'à la validation")
    void ouvertureObservee() {
        TransactionSynchronizationManager.initSynchronization();
        Liaison l = liaison(EtatLiaison.OUVERTE);
        publication.ouverture(l, "99001", charge(l));
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
    }

    @Test
    @DisplayName("Statut vu par la sandbox : états actifs ouverts, gel et clôture distincts")
    void statutSandbox() {
        assertThat(StatutSandbox.de(EtatLiaison.OUVERTE)).isEqualTo(StatutSandbox.OUVERTE);
        assertThat(StatutSandbox.de(EtatLiaison.EN_HOMOLOGATION)).isEqualTo(StatutSandbox.OUVERTE);
        assertThat(StatutSandbox.de(EtatLiaison.APTE_CERTIFICATION)).isEqualTo(StatutSandbox.OUVERTE);
        assertThat(StatutSandbox.de(EtatLiaison.MISE_EN_SERVICE_AUTORISEE)).isEqualTo(StatutSandbox.OUVERTE);
        assertThat(StatutSandbox.de(EtatLiaison.CERTIFIEE)).isEqualTo(StatutSandbox.OUVERTE);
        assertThat(StatutSandbox.de(EtatLiaison.GELEE)).isEqualTo(StatutSandbox.GELEE);
        assertThat(StatutSandbox.de(EtatLiaison.CLOSE)).isEqualTo(StatutSandbox.CLOSE);
    }
}
