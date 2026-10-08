package cm.gimac.des.homologation.application.usecase;

import static cm.gimac.des.homologation.domain.parcours.SignaturesSamples.J;
import static cm.gimac.des.homologation.domain.parcours.SignaturesSamples.piece;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import cm.gimac.des.homologation.application.port.out.StrongReauthPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.event.OperationSignedPayload;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.CatalogueFonctions;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.SignaturesSamples;
import cm.gimac.des.homologation.domain.parcours.Vues;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Signature d'une opération (D-91) : commande, projection (SIGNER) et tâche du signataire attendu. */
class SignaturesParcoursTest {

    static final Instant T = Instant.parse("2026-10-06T09:00:00Z");
    static final UUID JURISTE = UUID.randomUUID();
    static final Acteur JURISTE_ACTEUR = new Acteur(JURISTE, "Juriste du GIMAC", Set.of("HOM_LIAISON_READ", "ROLE_JURISTE"), null);
    static final Acteur CONDUCTEUR = new Acteur(SignaturesSamples.CONDUCTEUR, " ", Set.of("HOM_LIAISON_READ", "ROLE_CONDUCTEUR_PROJET"), null);

    ParcoursDepot depot;
    StrongReauthPort reauth;
    OutboxPublisher outbox;
    ParcoursCommandes commandes;
    Parcours p;

    @BeforeEach
    void init() {
        SignaturesSamples.brancherCatalogue();
        depot = mock(ParcoursDepot.class);
        reauth = mock(StrongReauthPort.class);
        outbox = mock(OutboxPublisher.class);
        commandes = new ParcoursCommandes(
            depot,
            mock(ParcoursInstanciation.class),
            mock(Intervenants.class),
            mock(DocumentRegistryPort.class),
            reauth,
            outbox,
            mock(LiaisonRepository.class),
            mock(ReferentialVersionRepository.class),
            mock(ContractualScopeRepository.class),
            mock(ScopeItemRepository.class),
            Clock.fixed(T, ZoneOffset.UTC)
        );
        p = SignaturesSamples.parcours();
        when(depot.trouver(p.id(), true)).thenReturn(Optional.of(p));
        when(reauth.verifier(eq("ok"), any(), eq(ParcoursCommandes.FINALITE_SIGNATURE))).thenReturn(true);
        p.deposerLivrable(op("OUV"), piece("DOC-OUV"), J);
        p.deposerLivrable(op("SIG"), piece("DOC-SIG"), J);
    }

    @AfterEach
    void fin() {
        CatalogueFonctions.brancher(null);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    OperationParcours op(String rang) {
        return p.operation(rang).orElseThrow();
    }

    static void refus(ThrowingCallable c, String code) {
        assertThatThrownBy(c).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Commande : assertion exigée, jeton utilisateur, opération et signature connues, ordre, signataire, FIDO2, événement")
    void signer() {
        UUID id = p.id();
        refus(() -> commandes.signer(id, "SIG", "VISA_JURIDIQUE", null, JURISTE_ACTEUR), "IAM_REAUTH_REQUIRED");
        refus(() -> commandes.signer(id, "SIG", "VISA_JURIDIQUE", " ", JURISTE_ACTEUR), "IAM_REAUTH_REQUIRED");
        refus(() -> commandes.signer(id, "SIG", "VISA_JURIDIQUE", "ok", new Acteur(null, "x", Set.of(), null)), "HOM_USER_TOKEN_REQUIRED");
        refus(() -> commandes.signer(id, "INCONNUE", "VISA_JURIDIQUE", "ok", JURISTE_ACTEUR), "HOM_OPERATION_NOT_FOUND");
        refus(() -> commandes.signer(id, "SIG", "AUTRE", "ok", JURISTE_ACTEUR), "HOM_SIGNATURE_INCONNUE");
        refus(() -> commandes.signer(id, "SIG", "APPROBATION", "ok", CONDUCTEUR), "HOM_SIGNATURE_ORDRE");
        refus(() -> commandes.signer(id, "SIG", "VISA_JURIDIQUE", "ok", CONDUCTEUR), "HOM_SIGNATURE_NON_SIGNATAIRE");
        refus(() -> commandes.signer(id, "SIG", "VISA_JURIDIQUE", "refusee", JURISTE_ACTEUR), "HOM_REAUTH_INVALID");
        verify(outbox, never()).publish(anyString(), any(UUID.class), anyString(), any(), anyString());

        commandes.signer(id, "SIG", "VISA_JURIDIQUE", "ok", JURISTE_ACTEUR);
        refus(() -> commandes.signer(id, "SIG", "APPROBATION", "ok", JURISTE_ACTEUR), "HOM_SIGNATURE_NON_SIGNATAIRE");
        commandes.signer(id, "SIG", "APPROBATION", "ok", CONDUCTEUR);
        assertThat(op("SIG").faite()).isTrue();
        ArgumentCaptor<Object> c = ArgumentCaptor.forClass(Object.class);
        verify(outbox, org.mockito.Mockito.times(2)).publish(eq("liaison"), eq(id), eq(OperationSignedPayload.TYPE), c.capture(), anyString());
        OperationSignedPayload premiere = (OperationSignedPayload) c.getAllValues().get(0);
        OperationSignedPayload derniere = (OperationSignedPayload) c.getAllValues().get(1);
        assertThat(premiere.signataireNom()).isEqualTo("Juriste du GIMAC");
        assertThat(premiere.restantes()).containsExactly("APPROBATION");
        assertThat(premiere.operationClose()).isFalse();
        assertThat(premiere.documents()).hasSize(2);
        assertThat(derniere.signataireNom()).isEqualTo("Conducteur de projet");
        assertThat(derniere.operationClose()).isTrue();
        assertThat(derniere.lien()).contains("operation=" + op("SIG").getId());
    }

    @Test
    @DisplayName("Projection : signatures attendues ou signées, action SIGNER pour le signataire admis seulement")
    void projection() {
        ParcoursProjection projection = new ParcoursProjection();
        Vues.Operation juriste = projection.operation(p, op("SIG"), JURISTE_ACTEUR, J);
        assertThat(juriste.etat()).isEqualTo(EtatOperationParcours.EN_SIGNATURE);
        assertThat(juriste.actionsPermises()).contains("SIGNER");
        assertThat(juriste.signatures()).extracting(Vues.Signature::code).containsExactly("VISA_JURIDIQUE", "APPROBATION");
        assertThat(juriste.signatures().get(0).suivante()).isTrue();
        assertThat(juriste.signatures().get(0).attendu()).isNull();
        assertThat(juriste.signatures().get(1).attendu().id()).isEqualTo(SignaturesSamples.CONDUCTEUR.toString());
        assertThat(projection.operation(p, op("SIG"), CONDUCTEUR, J).actionsPermises()).doesNotContain("SIGNER");
        assertThat(projection.operation(p, op("SIG"), new Acteur(null, "x", Set.of("ROLE_JURISTE"), null), J).actionsPermises()).doesNotContain("SIGNER");
        assertThat(projection.operation(p, op("OUV"), JURISTE_ACTEUR, J).signatures()).isEmpty();
        p.signer(op("SIG"), "VISA_JURIDIQUE", new cm.gimac.des.homologation.domain.parcours.Personne(JURISTE.toString(), "Juriste", SignaturesSamples.JURISTE), T, J);
        Vues.Operation apres = projection.operation(p, op("SIG"), CONDUCTEUR, J);
        assertThat(apres.actionsPermises()).contains("SIGNER");
        assertThat(apres.signatures().get(0).etat()).isEqualTo("SIGNEE");
        assertThat(apres.signatures().get(0).signePar()).isEqualTo(JURISTE.toString());
        assertThat(apres.signatures().get(0).empreinte()).hasSize(64);
    }

    @Test
    @DisplayName("Tâches : le signataire attendu désigné reçoit « Signer : … », terminée à sa signature ; R terminé en attente de signature")
    void taches() {
        DemandesTaches demandes = mock(DemandesTaches.class);
        SuiviTachesParcours suivi = new SuiviTachesParcours(demandes);
        TransactionSynchronizationManager.initSynchronization();
        suivi.observer(p);
        p.signer(op("SIG"), "VISA_JURIDIQUE", new cm.gimac.des.homologation.domain.parcours.Personne(JURISTE.toString(), "Juriste", SignaturesSamples.JURISTE), T, J);
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.beforeCommit(false);
        }
        ArgumentCaptor<PilotageTachesPort.Tache> c = ArgumentCaptor.forClass(PilotageTachesPort.Tache.class);
        verify(demandes).creer(c.capture(), any());
        assertThat(c.getValue().titre()).startsWith("Signer : ");
        assertThat(c.getValue().description()).contains("Approbation du conducteur");
        assertThat(c.getValue().responsableId()).isEqualTo(SignaturesSamples.CONDUCTEUR.toString());
        assertThat(c.getValue().origine().reference()).endsWith("/SIG/S/APPROBATION");
        TransactionSynchronizationManager.clearSynchronization();

        TransactionSynchronizationManager.initSynchronization();
        suivi.observer(p);
        p.signer(op("SIG"), "APPROBATION", new cm.gimac.des.homologation.domain.parcours.Personne(SignaturesSamples.CONDUCTEUR.toString(), "Conducteur", cm.gimac.des.homologation.domain.parcours.Fonction.CONDUCTEUR), T, J);
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.beforeCommit(false);
        }
        ArgumentCaptor<PilotageTachesPort.Designation> d = ArgumentCaptor.forClass(PilotageTachesPort.Designation.class);
        verify(demandes, org.mockito.Mockito.atLeastOnce()).cloturer(d.capture());
        assertThat(d.getAllValues()).anyMatch(x -> x.origine().reference().endsWith("/SIG/S/APPROBATION"));
        assertThat(SuiviTachesParcours.signataires(p, op("SIG"))).isEmpty();
        assertThat(List.of(SuiviTachesParcours.DEPASSE_R, SuiviTachesParcours.DEPASSE_V)).allMatch(e -> e.contains(EtatOperationParcours.EN_SIGNATURE));
    }
}
