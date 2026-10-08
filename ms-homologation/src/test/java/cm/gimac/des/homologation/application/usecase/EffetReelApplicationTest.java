package cm.gimac.des.homologation.application.usecase;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort;
import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import cm.gimac.des.homologation.application.port.out.StrongReauthPort;
import cm.gimac.des.homologation.application.usecase.ParcoursCommandes.Piece;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.event.LiaisonMilestoneReachedPayload;
import cm.gimac.des.homologation.domain.event.OperationParcoursPayload;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.OperationParcoursRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/** Effet réel des référentiels côté cas d'utilisation (D-93, D-94) : commandes, tâches, événements, départs, porteurs. */
class EffetReelApplicationTest {

    static final Instant T = Instant.parse("2026-10-06T09:00:00Z");
    static final LocalDate J = LocalDate.parse("2026-10-06");
    static final String SHA = "e".repeat(64);
    static final Acteur CONDUCTEUR = new Acteur(ParcoursSamples.CONDUCTEUR, "Conducteur de projet 1", Set.of("HOM_OPERATION_VALIDATE"), null);

    ParcoursDepot depot;
    DocumentRegistryPort documents;
    StrongReauthPort reauth;
    Intervenants intervenants;
    ParticipantDirectoryPort participants;
    ParcoursCommandes commandes;
    Parcours p;

    @BeforeEach
    void init() {
        depot = mock(ParcoursDepot.class);
        documents = mock(DocumentRegistryPort.class);
        reauth = mock(StrongReauthPort.class);
        intervenants = mock(Intervenants.class);
        participants = mock(ParticipantDirectoryPort.class);
        commandes = new ParcoursCommandes(
            depot,
            mock(ParcoursInstanciation.class),
            intervenants,
            documents,
            reauth,
            mock(OutboxPublisher.class),
            mock(LiaisonRepository.class),
            mock(ReferentialVersionRepository.class),
            mock(ContractualScopeRepository.class),
            mock(ScopeItemRepository.class),
            Clock.fixed(T, ZoneOffset.UTC)
        );
        commandes.setParticipants(participants);
        p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        when(depot.trouver(p.id(), true)).thenReturn(Optional.of(p));
        when(documents.verifier(any(), anyString())).thenReturn(DocumentRegistryPort.Verification.CONFORME);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void fin() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    static void refus(ThrowingCallable c, String code) {
        assertThatThrownBy(c).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    OperationParcours op(String rang) {
        OperationParcours o = p.operation(rang).orElseThrow();
        when(depot.deLOperation(o.getId())).thenReturn(p);
        return o;
    }

    Piece piece() {
        return new Piece(UUID.randomUUID(), SHA, "cr.pdf", "relu");
    }

    @Test
    @DisplayName("Commandes : dépôt refusé hors RACI, type de pièce contrôlé, preuve par code, validation réservée au V, renvoi")
    void commandes() {
        OperationParcours o7 = p
            .operations()
            .stream()
            .filter(o -> !o.getLivrables().isEmpty() && o.getGeste() == cm.gimac.des.homologation.domain.parcours.Geste.DEPOSER_LIVRABLE && !o.isCle() && !o.isValeurReference())
            .findFirst()
            .orElseThrow();
        o7 = op(o7.getRang());
        o7.setEtat(EtatOperationParcours.A_FAIRE);
        assertThat(o7.getLivrables()).extracting(cm.gimac.des.homologation.domain.parcours.Livrable::code).containsExactly("L1");
        Acteur inconnu = new Acteur(UUID.randomUUID(), "Inconnu", Set.of(), null);
        UUID id7 = o7.getId();
        refus(() -> commandes.deposerLivrable(id7, "L1", piece(), inconnu), "HOM_DEPOT_NON_AUTORISE");
        when(documents.typeMime(any())).thenReturn("image/png");
        refus(() -> commandes.deposerLivrable(id7, "L1", piece(), CONDUCTEUR), "HOM_LIVRABLE_TYPE_REFUSE");
        when(documents.typeMime(any())).thenReturn("application/pdf");
        Acteur superviseur = new Acteur(UUID.randomUUID(), "Superviseur", Set.of("HOM_STEP_MANAGE"), null);
        commandes.deposerLivrable(o7.getId(), "L1", piece(), superviseur);
        assertThat(o7.faite()).isTrue();
        assertThat(o7.depot("L1").orElseThrow().supervision()).isTrue();
        assertThat(o7.depot("L1").orElseThrow().typeMime()).isEqualTo("application/pdf");

        OperationParcours o3 = p.operation("3").orElseThrow();
        when(depot.deLOperation(o3.getId())).thenReturn(p);
        o3.setEtat(EtatOperationParcours.A_FAIRE);
        commandes.deposerPreuve(o3.getId(), null, piece(), null, null, null, CONDUCTEUR);
        assertThat(o3.getEtat()).isEqualTo(EtatOperationParcours.EN_VALIDATION);
        Acteur autre = new Acteur(UUID.randomUUID(), "Autre", Set.of(), null);
        refus(() -> commandes.valider(o3.getId(), "a", autre), "HOM_VALIDATION_NON_VALIDATEUR");
        verify(reauth, never()).verifier(any(), any(), any());
        refus(() -> commandes.renvoyer(o3.getId(), "m", new Acteur(null, null, Set.of(), null)), "HOM_USER_TOKEN_REQUIRED");
        refus(() -> commandes.renvoyer(o3.getId(), "m", autre), "HOM_VALIDATION_NON_VALIDATEUR");
        commandes.renvoyer(o3.getId(), "Pièce illisible", CONDUCTEUR);
        assertThat(o3.getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
        refus(() -> commandes.renvoyer(o3.getId(), "x", CONDUCTEUR), "HOM_OPERATION_NOT_IN_VALIDATION");
    }

    @Test
    @DisplayName("Affectation d'une fonction : personne de l'annuaire (interne), institution du registre (externe), fonctions non affectables")
    void affectation() {
        refus(() -> commandes.affecterFonction(p.id(), Fonction.PARTICIPANT, "x", null, "m", CONDUCTEUR), "HOM_FONCTION_NON_AFFECTABLE");
        refus(() -> commandes.affecterFonction(p.id(), Fonction.CONDUCTEUR, "x", null, "m", CONDUCTEUR), "HOM_FONCTION_NON_AFFECTABLE");
        refus(() -> commandes.affecterFonction(p.id(), Fonction.valueOf("INCONNUE"), "x", null, "m", CONDUCTEUR), "HOM_FONCTION_NON_AFFECTABLE");
        Personne rssi = new Personne(UUID.randomUUID().toString(), "RSSI", Fonction.RSSI);
        when(intervenants.pourFonction(rssi.id(), Fonction.RSSI)).thenReturn(rssi);
        commandes.affecterFonction(p.id(), Fonction.RSSI, rssi.id(), null, "relève", CONDUCTEUR);
        assertThat(p.tenant(Fonction.RSSI)).contains(rssi);
        refus(() -> commandes.affecterFonction(p.id(), Fonction.PERSONNALISATEUR, null, null, "m", CONDUCTEUR), "HOM_INSTITUTION_REQUISE");
        UUID inst = UUID.randomUUID();
        when(participants.getInstitution(inst)).thenReturn(new InstitutionView(inst, "Perso SA", "ACTIF", "PERSONNALISATEUR", List.of()));
        commandes.affecterFonction(p.id(), Fonction.PERSONNALISATEUR, null, inst, "m", CONDUCTEUR);
        assertThat(p.tenant(Fonction.PERSONNALISATEUR)).map(Personne::institutionId).contains(inst.toString());
    }

    @Test
    @DisplayName("Tâches : fonction à affecter confiée au conducteur, consultés sollicités, fonction externe sans institution au conducteur ; refus remonté")
    void taches() {
        DemandesTaches demandes = mock(DemandesTaches.class);
        SuiviTachesParcours suivi = new SuiviTachesParcours(demandes);
        OperationParcours o3 = p.operation("3").orElseThrow();
        suivi.observer(p);
        o3.setRaci(o3.getRaci().avecR(Personne.aAffecter(Fonction.DDRM)));
        valider();
        ArgumentCaptor<PilotageTachesPort.Tache> c = ArgumentCaptor.forClass(PilotageTachesPort.Tache.class);
        verify(demandes, atLeast(1)).creer(c.capture(), any());
        assertThat(c.getAllValues()).anySatisfy(t -> {
            assertThat(t.origine().reference()).isEqualTo("LIA-2026-001/AFFECTER/DDRM");
            assertThat(t.responsableId()).isEqualTo(ParcoursSamples.CONDUCTEUR.toString());
            assertThat(t.titre()).startsWith("Affecter ");
        });
        verify(demandes).rouvrir(new PilotageTachesPort.Designation(c.getAllValues().stream().filter(t -> t.origine().reference().endsWith("/AFFECTER/DDRM")).findFirst().orElseThrow().origine(), ParcoursSamples.CONDUCTEUR.toString()));

        // R externe sans institution liée : l'opération reste à la main du conducteur.
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();
        demandes = mock(DemandesTaches.class);
        suivi = new SuiviTachesParcours(demandes);
        suivi.observer(p);
        o3.setRaci(o3.getRaci().avecR(Personne.aAffecter(Fonction.PERSONNALISATEUR)));
        valider();
        c = ArgumentCaptor.forClass(PilotageTachesPort.Tache.class);
        verify(demandes, atLeast(1)).creer(c.capture(), any());
        assertThat(c.getAllValues()).anySatisfy(t -> {
            assertThat(t.origine().reference()).isEqualTo("LIA-2026-001/3/R");
            assertThat(t.responsableId()).isEqualTo(ParcoursSamples.CONDUCTEUR.toString());
            assertThat(t.description()).contains("reste à votre main");
        });
        assertThat(SuiviTachesParcours.responsableR(p, o3)).isEqualTo(ParcoursSamples.CONDUCTEUR.toString());

        // Consultés sollicités : tâche « Avis attendu » aux C internes désignés.
        String c1 = UUID.randomUUID().toString();
        o3.setRaci(o3.getRaci().ajouter(cm.gimac.des.homologation.domain.parcours.Qualite.C, new Personne(c1, "Consulté", Fonction.RSSI)));
        assertThat(SuiviTachesParcours.consultes(o3)).isEmpty();
        assertThat(SuiviTachesParcours.aAffecter(p)).doesNotContainKey("PERSONNALISATEUR");
        assertThat(SuiviTachesParcours.tache(p, o3, "C", c1, new PilotageTachesPort.Origine("OPERATION", "r", "homologation", null)).titre()).startsWith("Avis attendu : ");
    }

    void valider() {
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.beforeCommit(false);
        }
    }

    @Test
    @DisplayName("Événements : opération activée (ouverture, renvoi, reprise), validation demandée, jalon atteint, destinataires C et I")
    void evenements() {
        OutboxPublisher outbox = mock(OutboxPublisher.class);
        EvenementsParcours ev = new EvenementsParcours(outbox, Clock.fixed(T, ZoneOffset.UTC));
        avancer(p, "17", J);
        OperationParcours o18 = p.operation("18").orElseThrow();
        Map<UUID, EtatOperationParcours> avant = new HashMap<>();
        p.operations().forEach(o -> avant.put(o.getId(), o.faite() ? EtatOperationParcours.FAITE : EtatOperationParcours.VERROUILLEE));
        avant.put(o18.getId(), EtatOperationParcours.EN_VALIDATION);
        ev.emettre(p, avant, EnumSet.noneOf(Jalon.class));
        ArgumentCaptor<Object> charge = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        verify(outbox, atLeast(2)).publish(eq("liaison"), eq(p.id()), type.capture(), charge.capture(), eq(p.liaison().getInstitutionId().toString()));
        assertThat(type.getAllValues()).contains(OperationParcoursPayload.TYPE_ACTIVEE, LiaisonMilestoneReachedPayload.TYPE);
        OperationParcoursPayload a = charge.getAllValues().stream().filter(OperationParcoursPayload.class::isInstance).map(OperationParcoursPayload.class::cast).filter(x -> x.code().equals("18")).findFirst().orElseThrow();
        assertThat(a.motif()).isEqualTo("RENVOI");
        assertThat(a.typeProjet()).isEqualTo("CARTE_INTERBANCAIRE");
        assertThat(a.destinataires()).extracting(d -> d.qualite()).contains("I");
        LiaisonMilestoneReachedPayload m = charge.getAllValues().stream().filter(LiaisonMilestoneReachedPayload.class::isInstance).map(LiaisonMilestoneReachedPayload.class::cast).findFirst().orElseThrow();
        assertThat(m.jalon()).isEqualTo("APTE_CERTIFICATION");
        assertThat(m.etatLiaison()).isNotNull();

        OutboxPublisher outbox2 = mock(OutboxPublisher.class);
        EvenementsParcours ev2 = new EvenementsParcours(outbox2, Clock.fixed(T, ZoneOffset.UTC));
        Map<UUID, EtatOperationParcours> av2 = new HashMap<>();
        p.operations().forEach(o -> av2.put(o.getId(), o.getEtat()));
        av2.put(o18.getId(), EtatOperationParcours.A_FAIRE);
        o18.setEtat(EtatOperationParcours.EN_VALIDATION);
        ev2.emettre(p, av2, EvenementsParcours.jalons(p));
        verify(outbox2).publish(eq("liaison"), eq(p.id()), eq(OperationParcoursPayload.TYPE_VALIDATION), any(), anyString());
        assertThat(EvenementsParcours.destinataire("E", Personne.correspondant("i", "n")).institutionId()).isEqualTo("i");
        assertThat(EvenementsParcours.destinataire("C", Personne.aAffecter(Fonction.RSSI)).profils()).containsExactly("ROLE_RSSI");
    }

    @Test
    @DisplayName("Départ d'un compte : rôles à affecter sur ses liaisons ; compte vide ou non impliqué : rien")
    void depart() {
        OperationParcoursRepository repo = mock(OperationParcoursRepository.class);
        DepartsIntervenants d = new DepartsIntervenants(repo, depot);
        String sna = ParcoursSamples.SNA.toString();
        UUID absente = UUID.randomUUID();
        when(repo.liaisonsImpliquant(sna, "%\"" + sna + "\"%")).thenReturn(List.of(p.id(), absente));
        when(depot.trouver(absente, true)).thenReturn(Optional.empty());
        assertThat(d.traiterDepart(sna)).isEqualTo(1);
        assertThat(p.implique(sna)).isFalse();
        assertThat(d.traiterDepart(" ")).isZero();
        assertThat(d.traiterDepart(null)).isZero();
    }

    @Test
    @DisplayName("Porteurs d'une permission : lecture par permission dans MS-IAM, sans repli ; internes actifs seulement ; MS-IAM indisponible : 503")
    void porteurs() {
        CollaboratorDirectoryPort annuaire = mock(CollaboratorDirectoryPort.class);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        CollaboratorView va = new CollaboratorView(a, "Alpha", Set.of("ROLE_X"), null, true);
        CollaboratorView vb = new CollaboratorView(b, "Bêta", Set.of("ROLE_CONDUCTEUR_PROJET"), null, true);
        when(annuaire.parPermission("HOM_PROJECT_CONDUCT")).thenReturn(List.of(va, new CollaboratorView(UUID.randomUUID(), "Externe", Set.of(), "INST", true)));
        when(annuaire.parPermission("HOM_REFERENTIEL_EDIT")).thenReturn(List.of(vb, new CollaboratorView(UUID.randomUUID(), "Inactif", Set.of(), null, false)));
        when(annuaire.parPermission("HOM_INCONNUE")).thenReturn(List.of());
        when(annuaire.parPermission("HOM_PANNE")).thenThrow(HomologationException.unavailable("HOM_IAM_UNAVAILABLE", "indisponible"));
        PorteursPermission p = new PorteursPermission(annuaire);
        assertThat(p.porteurs("HOM_PROJECT_CONDUCT", "HOM_REFERENTIEL_EDIT")).extracting(CollaboratorView::nomAffiche).containsExactly("Alpha", "Bêta");
        assertThat(p.porte(a, "HOM_PROJECT_CONDUCT")).isTrue();
        assertThat(p.porte(b, "HOM_PROJECT_CONDUCT", "HOM_REFERENTIEL_EDIT")).isTrue();
        assertThat(p.porte(UUID.randomUUID(), "HOM_REFERENTIEL_EDIT")).isFalse();
        assertThat(p.porte(null, "HOM_PROJECT_CONDUCT")).isFalse();
        assertThat(p.porteurs("HOM_INCONNUE")).isEmpty();
        assertThatThrownBy(() -> p.porteurs("HOM_PANNE")).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_IAM_UNAVAILABLE"));
        verify(annuaire, never()).parProfil(any());
    }
}
