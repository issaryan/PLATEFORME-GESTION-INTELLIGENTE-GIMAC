package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.AlertAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.Alert;
import cm.gimac.des.monitoring.domain.Alert;
import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.enumeration.AlertState;
import cm.gimac.des.monitoring.repository.AlertRepository;
import cm.gimac.des.monitoring.service.AlertService;
import cm.gimac.des.monitoring.service.mapper.AlertMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AlertResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class AlertResourceIT {

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_SOURCE = "AAAAAAAAAA";
    private static final String UPDATED_SOURCE = "BBBBBBBBBB";

    private static final String DEFAULT_RULE = "AAAAAAAAAA";
    private static final String UPDATED_RULE = "BBBBBBBBBB";

    private static final String DEFAULT_FINGERPRINT = "78f99e5ae6142473b7739219eb4738b42c98370bd9e4b00c97c991aaf7448bb7";
    private static final String UPDATED_FINGERPRINT = "f5472fcdd5d27b18ab2dc0ee66bf3a9bddc679717722beeb885f815506d53aad";

    private static final AlertSeverity DEFAULT_SEVERITY = AlertSeverity.MINEURE;
    private static final AlertSeverity UPDATED_SEVERITY = AlertSeverity.MAJEURE;

    private static final AlertState DEFAULT_STATE = AlertState.OUVERTE;
    private static final AlertState UPDATED_STATE = AlertState.ACQUITTEE;

    private static final Instant DEFAULT_FIRST_SEEN_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FIRST_SEEN_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LAST_SEEN_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_SEEN_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_OCCURRENCES = 1;
    private static final Integer UPDATED_OCCURRENCES = 2;
    private static final Integer SMALLER_OCCURRENCES = 1 - 1;

    private static final Instant DEFAULT_ACK_DEADLINE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ACK_DEADLINE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_OVERDUE = false;
    private static final Boolean UPDATED_OVERDUE = true;

    private static final UUID DEFAULT_ACKNOWLEDGED_BY = UUID.randomUUID();
    private static final UUID UPDATED_ACKNOWLEDGED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_ACKNOWLEDGED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ACKNOWLEDGED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_TICKET_REQUESTED = false;
    private static final Boolean UPDATED_TICKET_REQUESTED = true;

    private static final String DEFAULT_ACK_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_ACK_COMMENT = "BBBBBBBBBB";

    private static final UUID DEFAULT_TICKET_ID = UUID.randomUUID();
    private static final UUID UPDATED_TICKET_ID = UUID.randomUUID();

    private static final Instant DEFAULT_RESOLVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RESOLVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/alerts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AlertRepository alertRepository;

    @Mock
    private AlertRepository alertRepositoryMock;

    @Autowired
    private AlertMapper alertMapper;

    @Mock
    private AlertService alertServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAlertMockMvc;

    private Alert alert;

    private Alert insertedAlert;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Alert createEntity(EntityManager em) {
        Alert alert = new Alert()
            .reference(DEFAULT_REFERENCE)
            .source(DEFAULT_SOURCE)
            .rule(DEFAULT_RULE)
            .fingerprint(DEFAULT_FINGERPRINT)
            .severity(DEFAULT_SEVERITY)
            .state(DEFAULT_STATE)
            .firstSeenAt(DEFAULT_FIRST_SEEN_AT)
            .lastSeenAt(DEFAULT_LAST_SEEN_AT)
            .occurrences(DEFAULT_OCCURRENCES)
            .ackDeadline(DEFAULT_ACK_DEADLINE)
            .overdue(DEFAULT_OVERDUE)
            .acknowledgedBy(DEFAULT_ACKNOWLEDGED_BY)
            .acknowledgedAt(DEFAULT_ACKNOWLEDGED_AT)
            .ticketRequested(DEFAULT_TICKET_REQUESTED)
            .ackComment(DEFAULT_ACK_COMMENT)
            .ticketId(DEFAULT_TICKET_ID)
            .resolvedAt(DEFAULT_RESOLVED_AT);
        // Add required entity
        SupervisedLink supervisedLink;
        if (TestUtil.findAll(em, SupervisedLink.class).isEmpty()) {
            supervisedLink = SupervisedLinkResourceIT.createEntity();
            em.persist(supervisedLink);
            em.flush();
        } else {
            supervisedLink = TestUtil.findAll(em, SupervisedLink.class).get(0);
        }
        alert.setLink(supervisedLink);
        return alert;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Alert createUpdatedEntity(EntityManager em) {
        Alert updatedAlert = new Alert()
            .reference(UPDATED_REFERENCE)
            .source(UPDATED_SOURCE)
            .rule(UPDATED_RULE)
            .fingerprint(UPDATED_FINGERPRINT)
            .severity(UPDATED_SEVERITY)
            .state(UPDATED_STATE)
            .firstSeenAt(UPDATED_FIRST_SEEN_AT)
            .lastSeenAt(UPDATED_LAST_SEEN_AT)
            .occurrences(UPDATED_OCCURRENCES)
            .ackDeadline(UPDATED_ACK_DEADLINE)
            .overdue(UPDATED_OVERDUE)
            .acknowledgedBy(UPDATED_ACKNOWLEDGED_BY)
            .acknowledgedAt(UPDATED_ACKNOWLEDGED_AT)
            .ticketRequested(UPDATED_TICKET_REQUESTED)
            .ackComment(UPDATED_ACK_COMMENT)
            .ticketId(UPDATED_TICKET_ID)
            .resolvedAt(UPDATED_RESOLVED_AT);
        // Add required entity
        SupervisedLink supervisedLink;
        if (TestUtil.findAll(em, SupervisedLink.class).isEmpty()) {
            supervisedLink = SupervisedLinkResourceIT.createUpdatedEntity();
            em.persist(supervisedLink);
            em.flush();
        } else {
            supervisedLink = TestUtil.findAll(em, SupervisedLink.class).get(0);
        }
        updatedAlert.setLink(supervisedLink);
        return updatedAlert;
    }

    @BeforeEach
    void initTest() {
        alert = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAlert != null) {
            alertRepository.delete(insertedAlert);
            insertedAlert = null;
        }
    }

    @Test
    @Transactional
    void getAllAlerts() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList
        restAlertMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(alert.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE)))
            .andExpect(jsonPath("$.[*].rule").value(hasItem(DEFAULT_RULE)))
            .andExpect(jsonPath("$.[*].fingerprint").value(hasItem(DEFAULT_FINGERPRINT)))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].firstSeenAt").value(hasItem(DEFAULT_FIRST_SEEN_AT.toString())))
            .andExpect(jsonPath("$.[*].lastSeenAt").value(hasItem(DEFAULT_LAST_SEEN_AT.toString())))
            .andExpect(jsonPath("$.[*].occurrences").value(hasItem(DEFAULT_OCCURRENCES)))
            .andExpect(jsonPath("$.[*].ackDeadline").value(hasItem(DEFAULT_ACK_DEADLINE.toString())))
            .andExpect(jsonPath("$.[*].overdue").value(hasItem(DEFAULT_OVERDUE)))
            .andExpect(jsonPath("$.[*].acknowledgedBy").value(hasItem(DEFAULT_ACKNOWLEDGED_BY.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())))
            .andExpect(jsonPath("$.[*].ticketRequested").value(hasItem(DEFAULT_TICKET_REQUESTED)))
            .andExpect(jsonPath("$.[*].ackComment").value(hasItem(DEFAULT_ACK_COMMENT)))
            .andExpect(jsonPath("$.[*].ticketId").value(hasItem(DEFAULT_TICKET_ID.toString())))
            .andExpect(jsonPath("$.[*].resolvedAt").value(hasItem(DEFAULT_RESOLVED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAlertsWithEagerRelationshipsIsEnabled() throws Exception {
        when(alertServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAlertMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(alertServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAlertsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(alertServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAlertMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(alertRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getAlert() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get the alert
        restAlertMockMvc
            .perform(get(ENTITY_API_URL_ID, alert.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(alert.getId().toString()))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.source").value(DEFAULT_SOURCE))
            .andExpect(jsonPath("$.rule").value(DEFAULT_RULE))
            .andExpect(jsonPath("$.fingerprint").value(DEFAULT_FINGERPRINT))
            .andExpect(jsonPath("$.severity").value(DEFAULT_SEVERITY.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.firstSeenAt").value(DEFAULT_FIRST_SEEN_AT.toString()))
            .andExpect(jsonPath("$.lastSeenAt").value(DEFAULT_LAST_SEEN_AT.toString()))
            .andExpect(jsonPath("$.occurrences").value(DEFAULT_OCCURRENCES))
            .andExpect(jsonPath("$.ackDeadline").value(DEFAULT_ACK_DEADLINE.toString()))
            .andExpect(jsonPath("$.overdue").value(DEFAULT_OVERDUE))
            .andExpect(jsonPath("$.acknowledgedBy").value(DEFAULT_ACKNOWLEDGED_BY.toString()))
            .andExpect(jsonPath("$.acknowledgedAt").value(DEFAULT_ACKNOWLEDGED_AT.toString()))
            .andExpect(jsonPath("$.ticketRequested").value(DEFAULT_TICKET_REQUESTED))
            .andExpect(jsonPath("$.ackComment").value(DEFAULT_ACK_COMMENT))
            .andExpect(jsonPath("$.ticketId").value(DEFAULT_TICKET_ID.toString()))
            .andExpect(jsonPath("$.resolvedAt").value(DEFAULT_RESOLVED_AT.toString()));
    }

    @Test
    @Transactional
    void getAlertsByIdFiltering() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        UUID id = alert.getId();

        defaultAlertFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllAlertsByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where reference equals to
        defaultAlertFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllAlertsByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where reference in
        defaultAlertFiltering("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE, "reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllAlertsByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where reference is not null
        defaultAlertFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where reference contains
        defaultAlertFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllAlertsByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where reference does not contain
        defaultAlertFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
    }

    @Test
    @Transactional
    void getAllAlertsBySourceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where source equals to
        defaultAlertFiltering("source.equals=" + DEFAULT_SOURCE, "source.equals=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllAlertsBySourceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where source in
        defaultAlertFiltering("source.in=" + DEFAULT_SOURCE + "," + UPDATED_SOURCE, "source.in=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllAlertsBySourceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where source is not null
        defaultAlertFiltering("source.specified=true", "source.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsBySourceContainsSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where source contains
        defaultAlertFiltering("source.contains=" + DEFAULT_SOURCE, "source.contains=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllAlertsBySourceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where source does not contain
        defaultAlertFiltering("source.doesNotContain=" + UPDATED_SOURCE, "source.doesNotContain=" + DEFAULT_SOURCE);
    }

    @Test
    @Transactional
    void getAllAlertsByRuleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where rule equals to
        defaultAlertFiltering("rule.equals=" + DEFAULT_RULE, "rule.equals=" + UPDATED_RULE);
    }

    @Test
    @Transactional
    void getAllAlertsByRuleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where rule in
        defaultAlertFiltering("rule.in=" + DEFAULT_RULE + "," + UPDATED_RULE, "rule.in=" + UPDATED_RULE);
    }

    @Test
    @Transactional
    void getAllAlertsByRuleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where rule is not null
        defaultAlertFiltering("rule.specified=true", "rule.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByRuleContainsSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where rule contains
        defaultAlertFiltering("rule.contains=" + DEFAULT_RULE, "rule.contains=" + UPDATED_RULE);
    }

    @Test
    @Transactional
    void getAllAlertsByRuleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where rule does not contain
        defaultAlertFiltering("rule.doesNotContain=" + UPDATED_RULE, "rule.doesNotContain=" + DEFAULT_RULE);
    }

    @Test
    @Transactional
    void getAllAlertsByFingerprintIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where fingerprint equals to
        defaultAlertFiltering("fingerprint.equals=" + DEFAULT_FINGERPRINT, "fingerprint.equals=" + UPDATED_FINGERPRINT);
    }

    @Test
    @Transactional
    void getAllAlertsByFingerprintIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where fingerprint in
        defaultAlertFiltering("fingerprint.in=" + DEFAULT_FINGERPRINT + "," + UPDATED_FINGERPRINT, "fingerprint.in=" + UPDATED_FINGERPRINT);
    }

    @Test
    @Transactional
    void getAllAlertsByFingerprintIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where fingerprint is not null
        defaultAlertFiltering("fingerprint.specified=true", "fingerprint.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByFingerprintContainsSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where fingerprint contains
        defaultAlertFiltering("fingerprint.contains=" + DEFAULT_FINGERPRINT, "fingerprint.contains=" + UPDATED_FINGERPRINT);
    }

    @Test
    @Transactional
    void getAllAlertsByFingerprintNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where fingerprint does not contain
        defaultAlertFiltering("fingerprint.doesNotContain=" + UPDATED_FINGERPRINT, "fingerprint.doesNotContain=" + DEFAULT_FINGERPRINT);
    }

    @Test
    @Transactional
    void getAllAlertsBySeverityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where severity equals to
        defaultAlertFiltering("severity.equals=" + DEFAULT_SEVERITY, "severity.equals=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllAlertsBySeverityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where severity in
        defaultAlertFiltering("severity.in=" + DEFAULT_SEVERITY + "," + UPDATED_SEVERITY, "severity.in=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllAlertsBySeverityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where severity is not null
        defaultAlertFiltering("severity.specified=true", "severity.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where state equals to
        defaultAlertFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllAlertsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where state in
        defaultAlertFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllAlertsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where state is not null
        defaultAlertFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByFirstSeenAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where firstSeenAt equals to
        defaultAlertFiltering("firstSeenAt.equals=" + DEFAULT_FIRST_SEEN_AT, "firstSeenAt.equals=" + UPDATED_FIRST_SEEN_AT);
    }

    @Test
    @Transactional
    void getAllAlertsByFirstSeenAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where firstSeenAt in
        defaultAlertFiltering(
            "firstSeenAt.in=" + DEFAULT_FIRST_SEEN_AT + "," + UPDATED_FIRST_SEEN_AT,
            "firstSeenAt.in=" + UPDATED_FIRST_SEEN_AT
        );
    }

    @Test
    @Transactional
    void getAllAlertsByFirstSeenAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where firstSeenAt is not null
        defaultAlertFiltering("firstSeenAt.specified=true", "firstSeenAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByLastSeenAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where lastSeenAt equals to
        defaultAlertFiltering("lastSeenAt.equals=" + DEFAULT_LAST_SEEN_AT, "lastSeenAt.equals=" + UPDATED_LAST_SEEN_AT);
    }

    @Test
    @Transactional
    void getAllAlertsByLastSeenAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where lastSeenAt in
        defaultAlertFiltering(
            "lastSeenAt.in=" + DEFAULT_LAST_SEEN_AT + "," + UPDATED_LAST_SEEN_AT,
            "lastSeenAt.in=" + UPDATED_LAST_SEEN_AT
        );
    }

    @Test
    @Transactional
    void getAllAlertsByLastSeenAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where lastSeenAt is not null
        defaultAlertFiltering("lastSeenAt.specified=true", "lastSeenAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByOccurrencesIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where occurrences equals to
        defaultAlertFiltering("occurrences.equals=" + DEFAULT_OCCURRENCES, "occurrences.equals=" + UPDATED_OCCURRENCES);
    }

    @Test
    @Transactional
    void getAllAlertsByOccurrencesIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where occurrences in
        defaultAlertFiltering("occurrences.in=" + DEFAULT_OCCURRENCES + "," + UPDATED_OCCURRENCES, "occurrences.in=" + UPDATED_OCCURRENCES);
    }

    @Test
    @Transactional
    void getAllAlertsByOccurrencesIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where occurrences is not null
        defaultAlertFiltering("occurrences.specified=true", "occurrences.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByOccurrencesIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where occurrences is greater than or equal to
        defaultAlertFiltering(
            "occurrences.greaterThanOrEqual=" + DEFAULT_OCCURRENCES,
            "occurrences.greaterThanOrEqual=" + UPDATED_OCCURRENCES
        );
    }

    @Test
    @Transactional
    void getAllAlertsByOccurrencesIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where occurrences is less than or equal to
        defaultAlertFiltering("occurrences.lessThanOrEqual=" + DEFAULT_OCCURRENCES, "occurrences.lessThanOrEqual=" + SMALLER_OCCURRENCES);
    }

    @Test
    @Transactional
    void getAllAlertsByOccurrencesIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where occurrences is less than
        defaultAlertFiltering("occurrences.lessThan=" + UPDATED_OCCURRENCES, "occurrences.lessThan=" + DEFAULT_OCCURRENCES);
    }

    @Test
    @Transactional
    void getAllAlertsByOccurrencesIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where occurrences is greater than
        defaultAlertFiltering("occurrences.greaterThan=" + SMALLER_OCCURRENCES, "occurrences.greaterThan=" + DEFAULT_OCCURRENCES);
    }

    @Test
    @Transactional
    void getAllAlertsByAckDeadlineIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ackDeadline equals to
        defaultAlertFiltering("ackDeadline.equals=" + DEFAULT_ACK_DEADLINE, "ackDeadline.equals=" + UPDATED_ACK_DEADLINE);
    }

    @Test
    @Transactional
    void getAllAlertsByAckDeadlineIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ackDeadline in
        defaultAlertFiltering(
            "ackDeadline.in=" + DEFAULT_ACK_DEADLINE + "," + UPDATED_ACK_DEADLINE,
            "ackDeadline.in=" + UPDATED_ACK_DEADLINE
        );
    }

    @Test
    @Transactional
    void getAllAlertsByAckDeadlineIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ackDeadline is not null
        defaultAlertFiltering("ackDeadline.specified=true", "ackDeadline.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByOverdueIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where overdue equals to
        defaultAlertFiltering("overdue.equals=" + DEFAULT_OVERDUE, "overdue.equals=" + UPDATED_OVERDUE);
    }

    @Test
    @Transactional
    void getAllAlertsByOverdueIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where overdue in
        defaultAlertFiltering("overdue.in=" + DEFAULT_OVERDUE + "," + UPDATED_OVERDUE, "overdue.in=" + UPDATED_OVERDUE);
    }

    @Test
    @Transactional
    void getAllAlertsByOverdueIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where overdue is not null
        defaultAlertFiltering("overdue.specified=true", "overdue.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByAcknowledgedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where acknowledgedBy equals to
        defaultAlertFiltering("acknowledgedBy.equals=" + DEFAULT_ACKNOWLEDGED_BY, "acknowledgedBy.equals=" + UPDATED_ACKNOWLEDGED_BY);
    }

    @Test
    @Transactional
    void getAllAlertsByAcknowledgedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where acknowledgedBy in
        defaultAlertFiltering(
            "acknowledgedBy.in=" + DEFAULT_ACKNOWLEDGED_BY + "," + UPDATED_ACKNOWLEDGED_BY,
            "acknowledgedBy.in=" + UPDATED_ACKNOWLEDGED_BY
        );
    }

    @Test
    @Transactional
    void getAllAlertsByAcknowledgedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where acknowledgedBy is not null
        defaultAlertFiltering("acknowledgedBy.specified=true", "acknowledgedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByAcknowledgedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where acknowledgedAt equals to
        defaultAlertFiltering("acknowledgedAt.equals=" + DEFAULT_ACKNOWLEDGED_AT, "acknowledgedAt.equals=" + UPDATED_ACKNOWLEDGED_AT);
    }

    @Test
    @Transactional
    void getAllAlertsByAcknowledgedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where acknowledgedAt in
        defaultAlertFiltering(
            "acknowledgedAt.in=" + DEFAULT_ACKNOWLEDGED_AT + "," + UPDATED_ACKNOWLEDGED_AT,
            "acknowledgedAt.in=" + UPDATED_ACKNOWLEDGED_AT
        );
    }

    @Test
    @Transactional
    void getAllAlertsByAcknowledgedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where acknowledgedAt is not null
        defaultAlertFiltering("acknowledgedAt.specified=true", "acknowledgedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByTicketRequestedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ticketRequested equals to
        defaultAlertFiltering("ticketRequested.equals=" + DEFAULT_TICKET_REQUESTED, "ticketRequested.equals=" + UPDATED_TICKET_REQUESTED);
    }

    @Test
    @Transactional
    void getAllAlertsByTicketRequestedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ticketRequested in
        defaultAlertFiltering(
            "ticketRequested.in=" + DEFAULT_TICKET_REQUESTED + "," + UPDATED_TICKET_REQUESTED,
            "ticketRequested.in=" + UPDATED_TICKET_REQUESTED
        );
    }

    @Test
    @Transactional
    void getAllAlertsByTicketRequestedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ticketRequested is not null
        defaultAlertFiltering("ticketRequested.specified=true", "ticketRequested.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByAckCommentIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ackComment equals to
        defaultAlertFiltering("ackComment.equals=" + DEFAULT_ACK_COMMENT, "ackComment.equals=" + UPDATED_ACK_COMMENT);
    }

    @Test
    @Transactional
    void getAllAlertsByAckCommentIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ackComment in
        defaultAlertFiltering("ackComment.in=" + DEFAULT_ACK_COMMENT + "," + UPDATED_ACK_COMMENT, "ackComment.in=" + UPDATED_ACK_COMMENT);
    }

    @Test
    @Transactional
    void getAllAlertsByAckCommentIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ackComment is not null
        defaultAlertFiltering("ackComment.specified=true", "ackComment.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByAckCommentContainsSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ackComment contains
        defaultAlertFiltering("ackComment.contains=" + DEFAULT_ACK_COMMENT, "ackComment.contains=" + UPDATED_ACK_COMMENT);
    }

    @Test
    @Transactional
    void getAllAlertsByAckCommentNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ackComment does not contain
        defaultAlertFiltering("ackComment.doesNotContain=" + UPDATED_ACK_COMMENT, "ackComment.doesNotContain=" + DEFAULT_ACK_COMMENT);
    }

    @Test
    @Transactional
    void getAllAlertsByTicketIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ticketId equals to
        defaultAlertFiltering("ticketId.equals=" + DEFAULT_TICKET_ID, "ticketId.equals=" + UPDATED_TICKET_ID);
    }

    @Test
    @Transactional
    void getAllAlertsByTicketIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ticketId in
        defaultAlertFiltering("ticketId.in=" + DEFAULT_TICKET_ID + "," + UPDATED_TICKET_ID, "ticketId.in=" + UPDATED_TICKET_ID);
    }

    @Test
    @Transactional
    void getAllAlertsByTicketIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where ticketId is not null
        defaultAlertFiltering("ticketId.specified=true", "ticketId.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByResolvedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where resolvedAt equals to
        defaultAlertFiltering("resolvedAt.equals=" + DEFAULT_RESOLVED_AT, "resolvedAt.equals=" + UPDATED_RESOLVED_AT);
    }

    @Test
    @Transactional
    void getAllAlertsByResolvedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where resolvedAt in
        defaultAlertFiltering("resolvedAt.in=" + DEFAULT_RESOLVED_AT + "," + UPDATED_RESOLVED_AT, "resolvedAt.in=" + UPDATED_RESOLVED_AT);
    }

    @Test
    @Transactional
    void getAllAlertsByResolvedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAlert = alertRepository.saveAndFlush(alert);

        // Get all the alertList where resolvedAt is not null
        defaultAlertFiltering("resolvedAt.specified=true", "resolvedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAlertsByLinkIsEqualToSomething() throws Exception {
        SupervisedLink link;
        if (TestUtil.findAll(em, SupervisedLink.class).isEmpty()) {
            alertRepository.saveAndFlush(alert);
            link = SupervisedLinkResourceIT.createEntity();
        } else {
            link = TestUtil.findAll(em, SupervisedLink.class).get(0);
        }
        em.persist(link);
        em.flush();
        alert.setLink(link);
        alertRepository.saveAndFlush(alert);
        UUID linkId = link.getId();
        // Get all the alertList where link equals to linkId
        defaultAlertShouldBeFound("linkId.equals=" + linkId);

        // Get all the alertList where link equals to UUID.randomUUID()
        defaultAlertShouldNotBeFound("linkId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllAlertsByCorrelationRootIsEqualToSomething() throws Exception {
        Alert correlationRoot;
        if (TestUtil.findAll(em, Alert.class).isEmpty()) {
            alertRepository.saveAndFlush(alert);
            correlationRoot = AlertResourceIT.createUpdatedEntity(em); // champs uniques distincts de l'entité testée
        } else {
            correlationRoot = TestUtil.findAll(em, Alert.class).get(0);
        }
        em.persist(correlationRoot);
        em.flush();
        alert.setCorrelationRoot(correlationRoot);
        alertRepository.saveAndFlush(alert);
        UUID correlationRootId = correlationRoot.getId();
        // Get all the alertList where correlationRoot equals to correlationRootId
        defaultAlertShouldBeFound("correlationRootId.equals=" + correlationRootId);

        // Get all the alertList where correlationRoot equals to UUID.randomUUID()
        defaultAlertShouldNotBeFound("correlationRootId.equals=" + UUID.randomUUID());
    }

    private void defaultAlertFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultAlertShouldBeFound(shouldBeFound);
        defaultAlertShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultAlertShouldBeFound(String filter) throws Exception {
        restAlertMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(alert.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE)))
            .andExpect(jsonPath("$.[*].rule").value(hasItem(DEFAULT_RULE)))
            .andExpect(jsonPath("$.[*].fingerprint").value(hasItem(DEFAULT_FINGERPRINT)))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].firstSeenAt").value(hasItem(DEFAULT_FIRST_SEEN_AT.toString())))
            .andExpect(jsonPath("$.[*].lastSeenAt").value(hasItem(DEFAULT_LAST_SEEN_AT.toString())))
            .andExpect(jsonPath("$.[*].occurrences").value(hasItem(DEFAULT_OCCURRENCES)))
            .andExpect(jsonPath("$.[*].ackDeadline").value(hasItem(DEFAULT_ACK_DEADLINE.toString())))
            .andExpect(jsonPath("$.[*].overdue").value(hasItem(DEFAULT_OVERDUE)))
            .andExpect(jsonPath("$.[*].acknowledgedBy").value(hasItem(DEFAULT_ACKNOWLEDGED_BY.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())))
            .andExpect(jsonPath("$.[*].ticketRequested").value(hasItem(DEFAULT_TICKET_REQUESTED)))
            .andExpect(jsonPath("$.[*].ackComment").value(hasItem(DEFAULT_ACK_COMMENT)))
            .andExpect(jsonPath("$.[*].ticketId").value(hasItem(DEFAULT_TICKET_ID.toString())))
            .andExpect(jsonPath("$.[*].resolvedAt").value(hasItem(DEFAULT_RESOLVED_AT.toString())));

        // Check, that the count call also returns 1
        restAlertMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultAlertShouldNotBeFound(String filter) throws Exception {
        restAlertMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restAlertMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingAlert() throws Exception {
        // Get the alert
        restAlertMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return alertRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Alert getPersistedAlert(Alert alert) {
        return alertRepository.findById(alert.getId()).orElseThrow();
    }

    protected void assertPersistedAlertToMatchAllProperties(Alert expectedAlert) {
        assertAlertAllPropertiesEquals(expectedAlert, getPersistedAlert(expectedAlert));
    }

    protected void assertPersistedAlertToMatchUpdatableProperties(Alert expectedAlert) {
        assertAlertAllUpdatablePropertiesEquals(expectedAlert, getPersistedAlert(expectedAlert));
    }
}
