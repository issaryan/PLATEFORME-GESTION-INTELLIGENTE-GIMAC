package cm.gimac.des.participant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.participant.application.port.out.DocumentRegistry;
import cm.gimac.des.participant.application.port.out.StrongReauthVerifier;
import cm.gimac.des.participant.application.AdhesionProperties;
import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Acteur;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.TransitionState;
import cm.gimac.des.participant.domain.event.StatusChangedPayload;
import cm.gimac.des.participant.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.participant.repository.AdhesionPieceRepository;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.InteractionRepository;
import cm.gimac.des.participant.repository.StatusTransitionRepository;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Version de statut dans participant.status-changed.v1 (US-EXI-20, D-120) : incrément strict par institution dans la
 * transaction du changement de statut, contrat lisible par machine, compatibilité ascendante.
 */
@ExtendWith(MockitoExtension.class)
class VersionStatutRegistreTest {

    static final Instant MAINTENANT = Instant.parse("2026-10-22T08:00:00Z");
    static final Acteur CHARGE = new Acteur(UUID.randomUUID(), "Chargé DDRM");
    static final Acteur CHEF = new Acteur(UUID.randomUUID(), "Chef DDRM");

    @Mock
    InstitutionRepository institutions;

    @Mock
    AdhesionPieceRepository pieces;

    @Mock
    StatusTransitionRepository transitions;

    @Mock
    InteractionRepository interactions;

    @Mock
    DocumentRegistry documents;

    @Mock
    StrongReauthVerifier reauth;

    @Mock
    OutboxPublisher outbox;

    final VersionsStatutMemoire versions = new VersionsStatutMemoire();
    InstitutionLifecycleService service;
    Institution banque;

    @BeforeEach
    void preparer() {
        service = new InstitutionLifecycleService(
            institutions,
            pieces,
            transitions,
            interactions,
            documents,
            reauth,
            outbox,
            new AdhesionProperties(List.of()),
            new DossierSnapshots(pieces),
            Clock.fixed(MAINTENANT, ZoneOffset.UTC),
            versions
        );
        banque = new Institution()
            .name("Banque fictive 99001")
            .country("CM")
            .category(InstitutionCategory.BANQUE)
            .status(InstitutionStatus.ACTIF)
            .participantCode("99001")
            .complianceState(ComplianceState.CONFORME)
            .openLinkCount(0)
            .openSuspenseCount(0)
            .createdAt(MAINTENANT);
        banque.setId(UUID.randomUUID());
        lenient().when(institutions.findByIdForUpdate(banque.getId())).thenReturn(Optional.of(banque));
        lenient().when(transitions.save(any(StatusTransition.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(reauth.verifier(any(), any(), any())).thenReturn(true);
        // L'institution 99001 a déjà connu six changements de statut.
        versions.versions.put(banque.getId(), 6L);
    }

    StatusTransition proposition(InstitutionStatus de, InstitutionStatus vers) {
        StatusTransition t = new StatusTransition()
            .fromStatus(de)
            .toStatus(vers)
            .reason("Décision du conseil")
            .requesterId(CHARGE.id())
            .requestedAt(MAINTENANT)
            .automaticTransition(false)
            .requiredApprovals(1)
            .state(TransitionState.PROPOSEE)
            .evidenceDocumentId(UUID.randomUUID())
            .institution(banque);
        t.setId(UUID.randomUUID());
        when(transitions.findById(t.getId())).thenReturn(Optional.of(t));
        return t;
    }

    List<StatusChangedPayload> publies(int n) {
        ArgumentCaptor<Object> evt = ArgumentCaptor.forClass(Object.class);
        verify(outbox, times(n)).publish(eq("institution"), eq(banque.getId()), eq(StatusChangedPayload.TYPE), evt.capture());
        return evt.getAllValues().stream().map(StatusChangedPayload.class::cast).toList();
    }

    @Test
    @DisplayName("Critère 1 : suspension ordonnée, 99001 passe de la version 6 (ACTIF) à la version 7 (SUSPENDU)")
    void suspensionOrdonnee() {
        service.approuverTransition(banque.getId(), proposition(InstitutionStatus.ACTIF, InstitutionStatus.SUSPENDU).getId(), CHEF, "a");
        StatusChangedPayload p = publies(1).get(0);
        assertThat(p.participantCode()).isEqualTo("99001");
        assertThat(p.statut()).isEqualTo("SUSPENDU");
        assertThat(p.versionStatutRegistre()).isEqualTo(7);
        assertThat(versions.courante(banque.getId())).isEqualTo(7);
    }

    @Test
    @DisplayName("Contrat : présence et monotonie stricte de la version de statut sur des changements successifs")
    void monotonie() {
        service.approuverTransition(banque.getId(), proposition(InstitutionStatus.ACTIF, InstitutionStatus.A_REGULARISER).getId(), CHEF, "a");
        service.approuverTransition(banque.getId(), proposition(InstitutionStatus.A_REGULARISER, InstitutionStatus.SUSPENDU).getId(), CHEF, "a");
        assertThat(publies(2)).extracting(StatusChangedPayload::versionStatutRegistre).containsExactly(7L, 8L);
        // Une autre institution a sa propre suite, qui commence à 1.
        assertThat(versions.suivante(UUID.randomUUID())).isEqualTo(1);
    }

    @Test
    @DisplayName("Contrat lisible par machine : schéma JSON, champ ajouté par extension et optionnel pour les lecteurs")
    void contratLisibleParMachine() throws Exception {
        ObjectMapper json = JsonMapper.builder().build();
        JsonNode schema;
        try (InputStream in = getClass().getResourceAsStream("/config/contrats/participant.status-changed.v1.schema.json")) {
            schema = json.readTree(in);
        }
        assertThat(schema.path("title").asString()).isEqualTo(StatusChangedPayload.TYPE);
        assertThat(schema.path("properties").path("versionStatutRegistre").path("type").asString()).isEqualTo("integer");
        assertThat(schema.path("properties").path("versionStatutRegistre").path("minimum").asInt()).isEqualTo(1);
        // Compatibilité ascendante : la version n'est pas exigée des lecteurs, les champs lus par MS-HOMOLOGATION le restent.
        List<String> requis = schema.path("required").valueStream().map(JsonNode::asString).toList();
        assertThat(requis).doesNotContain("versionStatutRegistre").contains("institutionId", "statut");
        // Chaque champ publié est décrit par le schéma.
        StatusChangedPayload p = new StatusChangedPayload(banque.getId(), "99001", "ACTIF", "SUSPENDU", "m", UUID.randomUUID(), MAINTENANT, 7);
        JsonNode publie = json.valueToTree(p);
        java.util.Collection<String> decrits = schema.path("properties").propertyNames();
        assertThat(publie.propertyNames()).allMatch(decrits::contains);
        assertThat(publie.path("versionStatutRegistre").asLong()).isEqualTo(7);
        // Un événement antérieur à D-120 (sans version) reste lisible : le champ est simplement absent.
        JsonNode ancien = json.readTree("{\"institutionId\":\"" + banque.getId() + "\",\"statut\":\"ACTIF\"}");
        assertThat(ancien.has("versionStatutRegistre")).isFalse();
    }
}
