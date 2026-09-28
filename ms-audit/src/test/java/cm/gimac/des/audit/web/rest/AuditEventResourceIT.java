package cm.gimac.des.audit.web.rest;

import static cm.gimac.des.audit.domain.AuditEventAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.audit.IntegrationTest;
import cm.gimac.des.audit.domain.AuditEvent;
import cm.gimac.des.audit.repository.AuditEventRepository;
import cm.gimac.des.audit.service.mapper.AuditEventMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link AuditEventResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AuditEventResourceIT {

    private static final Long DEFAULT_LOG_INDEX = 0L;
    private static final Long UPDATED_LOG_INDEX = 1L;
    private static final Long SMALLER_LOG_INDEX = 0L - 1L;

    private static final UUID DEFAULT_EVENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_EVENT_ID = UUID.randomUUID();

    private static final Instant DEFAULT_OCCURRED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OCCURRED_AT = Instant.ofEpochMilli(1701661978853L);

    private static final Instant DEFAULT_RECORDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RECORDED_AT = Instant.ofEpochMilli(1701661978853L);

    private static final String DEFAULT_ACTOR_ID = "AAAAAAAAAA";
    private static final String UPDATED_ACTOR_ID = "BBBBBBBBBB";

    private static final String DEFAULT_ACTION_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_ACTION_TYPE = "BBBBBBBBBB";

    private static final String DEFAULT_DOMAIN = "AAAAAAAAAA";
    private static final String UPDATED_DOMAIN = "BBBBBBBBBB";

    private static final String DEFAULT_SUBJECT = "AAAAAAAAAA";
    private static final String UPDATED_SUBJECT = "BBBBBBBBBB";

    private static final UUID DEFAULT_CORRELATION_ID = UUID.randomUUID();
    private static final UUID UPDATED_CORRELATION_ID = UUID.randomUUID();

    private static final String DEFAULT_PAYLOAD = "AAAAAAAAAA";
    private static final String UPDATED_PAYLOAD = "BBBBBBBBBB";

    private static final String DEFAULT_LEAF_HASH = "aa5dbd8eaa3e7e7143cbd9af73fbfc27ac2bb244b5e95fe92a813d2f56c5fb99";
    private static final String UPDATED_LEAF_HASH = "d8e3abb8b692e54148abc4a80cdc04dc7374e276539d40b7740caac9b158baf1";

    private static final String ENTITY_API_URL = "/api/audit-events";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditEventMapper auditEventMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAuditEventMockMvc;

    private AuditEvent auditEvent;

    private AuditEvent insertedAuditEvent;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AuditEvent createEntity() {
        return new AuditEvent()
            .logIndex(DEFAULT_LOG_INDEX)
            .eventId(DEFAULT_EVENT_ID)
            .occurredAt(DEFAULT_OCCURRED_AT)
            .recordedAt(DEFAULT_RECORDED_AT)
            .actorId(DEFAULT_ACTOR_ID)
            .actionType(DEFAULT_ACTION_TYPE)
            .domain(DEFAULT_DOMAIN)
            .subject(DEFAULT_SUBJECT)
            .correlationId(DEFAULT_CORRELATION_ID)
            .payload(DEFAULT_PAYLOAD)
            .leafHash(DEFAULT_LEAF_HASH);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AuditEvent createUpdatedEntity() {
        return new AuditEvent()
            .logIndex(UPDATED_LOG_INDEX)
            .eventId(UPDATED_EVENT_ID)
            .occurredAt(UPDATED_OCCURRED_AT)
            .recordedAt(UPDATED_RECORDED_AT)
            .actorId(UPDATED_ACTOR_ID)
            .actionType(UPDATED_ACTION_TYPE)
            .domain(UPDATED_DOMAIN)
            .subject(UPDATED_SUBJECT)
            .correlationId(UPDATED_CORRELATION_ID)
            .payload(UPDATED_PAYLOAD)
            .leafHash(UPDATED_LEAF_HASH);
    }

    @BeforeEach
    void initTest() {
        auditEvent = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAuditEvent != null) {
            auditEventRepository.delete(insertedAuditEvent);
            insertedAuditEvent = null;
        }
    }

    @Test
    @Transactional
    void getAllAuditEvents() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList
        restAuditEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(auditEvent.getId().toString())))
            .andExpect(jsonPath("$.[*].logIndex").value(hasItem(DEFAULT_LOG_INDEX.intValue())))
            .andExpect(jsonPath("$.[*].eventId").value(hasItem(DEFAULT_EVENT_ID.toString())))
            .andExpect(jsonPath("$.[*].occurredAt").value(hasItem(DEFAULT_OCCURRED_AT.toString())))
            .andExpect(jsonPath("$.[*].recordedAt").value(hasItem(DEFAULT_RECORDED_AT.toString())))
            .andExpect(jsonPath("$.[*].actorId").value(hasItem(DEFAULT_ACTOR_ID)))
            .andExpect(jsonPath("$.[*].actionType").value(hasItem(DEFAULT_ACTION_TYPE)))
            .andExpect(jsonPath("$.[*].domain").value(hasItem(DEFAULT_DOMAIN)))
            .andExpect(jsonPath("$.[*].subject").value(hasItem(DEFAULT_SUBJECT)))
            .andExpect(jsonPath("$.[*].correlationId").value(hasItem(DEFAULT_CORRELATION_ID.toString())))
            .andExpect(jsonPath("$.[*].payload").value(hasItem(DEFAULT_PAYLOAD)))
            .andExpect(jsonPath("$.[*].leafHash").value(hasItem(DEFAULT_LEAF_HASH)));
    }

    @Test
    @Transactional
    void getAuditEvent() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get the auditEvent
        restAuditEventMockMvc
            .perform(get(ENTITY_API_URL_ID, auditEvent.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(auditEvent.getId().toString()))
            .andExpect(jsonPath("$.logIndex").value(DEFAULT_LOG_INDEX.intValue()))
            .andExpect(jsonPath("$.eventId").value(DEFAULT_EVENT_ID.toString()))
            .andExpect(jsonPath("$.occurredAt").value(DEFAULT_OCCURRED_AT.toString()))
            .andExpect(jsonPath("$.recordedAt").value(DEFAULT_RECORDED_AT.toString()))
            .andExpect(jsonPath("$.actorId").value(DEFAULT_ACTOR_ID))
            .andExpect(jsonPath("$.actionType").value(DEFAULT_ACTION_TYPE))
            .andExpect(jsonPath("$.domain").value(DEFAULT_DOMAIN))
            .andExpect(jsonPath("$.subject").value(DEFAULT_SUBJECT))
            .andExpect(jsonPath("$.correlationId").value(DEFAULT_CORRELATION_ID.toString()))
            .andExpect(jsonPath("$.payload").value(DEFAULT_PAYLOAD))
            .andExpect(jsonPath("$.leafHash").value(DEFAULT_LEAF_HASH));
    }

    @Test
    @Transactional
    void getAuditEventsByIdFiltering() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        UUID id = auditEvent.getId();

        defaultAuditEventFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllAuditEventsByLogIndexIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where logIndex equals to
        defaultAuditEventFiltering("logIndex.equals=" + DEFAULT_LOG_INDEX, "logIndex.equals=" + UPDATED_LOG_INDEX);
    }

    @Test
    @Transactional
    void getAllAuditEventsByLogIndexIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where logIndex in
        defaultAuditEventFiltering("logIndex.in=" + DEFAULT_LOG_INDEX + "," + UPDATED_LOG_INDEX, "logIndex.in=" + UPDATED_LOG_INDEX);
    }

    @Test
    @Transactional
    void getAllAuditEventsByLogIndexIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where logIndex is not null
        defaultAuditEventFiltering("logIndex.specified=true", "logIndex.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditEventsByLogIndexIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where logIndex is greater than or equal to
        defaultAuditEventFiltering("logIndex.greaterThanOrEqual=" + DEFAULT_LOG_INDEX, "logIndex.greaterThanOrEqual=" + UPDATED_LOG_INDEX);
    }

    @Test
    @Transactional
    void getAllAuditEventsByLogIndexIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where logIndex is less than or equal to
        defaultAuditEventFiltering("logIndex.lessThanOrEqual=" + DEFAULT_LOG_INDEX, "logIndex.lessThanOrEqual=" + SMALLER_LOG_INDEX);
    }

    @Test
    @Transactional
    void getAllAuditEventsByLogIndexIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where logIndex is less than
        defaultAuditEventFiltering("logIndex.lessThan=" + UPDATED_LOG_INDEX, "logIndex.lessThan=" + DEFAULT_LOG_INDEX);
    }

    @Test
    @Transactional
    void getAllAuditEventsByLogIndexIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where logIndex is greater than
        defaultAuditEventFiltering("logIndex.greaterThan=" + SMALLER_LOG_INDEX, "logIndex.greaterThan=" + DEFAULT_LOG_INDEX);
    }

    @Test
    @Transactional
    void getAllAuditEventsByEventIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where eventId equals to
        defaultAuditEventFiltering("eventId.equals=" + DEFAULT_EVENT_ID, "eventId.equals=" + UPDATED_EVENT_ID);
    }

    @Test
    @Transactional
    void getAllAuditEventsByEventIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where eventId in
        defaultAuditEventFiltering("eventId.in=" + DEFAULT_EVENT_ID + "," + UPDATED_EVENT_ID, "eventId.in=" + UPDATED_EVENT_ID);
    }

    @Test
    @Transactional
    void getAllAuditEventsByEventIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where eventId is not null
        defaultAuditEventFiltering("eventId.specified=true", "eventId.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditEventsByOccurredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where occurredAt equals to
        defaultAuditEventFiltering("occurredAt.equals=" + DEFAULT_OCCURRED_AT, "occurredAt.equals=" + UPDATED_OCCURRED_AT);
    }

    @Test
    @Transactional
    void getAllAuditEventsByOccurredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where occurredAt in
        defaultAuditEventFiltering(
            "occurredAt.in=" + DEFAULT_OCCURRED_AT + "," + UPDATED_OCCURRED_AT,
            "occurredAt.in=" + UPDATED_OCCURRED_AT
        );
    }

    @Test
    @Transactional
    void getAllAuditEventsByOccurredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where occurredAt is not null
        defaultAuditEventFiltering("occurredAt.specified=true", "occurredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditEventsByRecordedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where recordedAt equals to
        defaultAuditEventFiltering("recordedAt.equals=" + DEFAULT_RECORDED_AT, "recordedAt.equals=" + UPDATED_RECORDED_AT);
    }

    @Test
    @Transactional
    void getAllAuditEventsByRecordedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where recordedAt in
        defaultAuditEventFiltering(
            "recordedAt.in=" + DEFAULT_RECORDED_AT + "," + UPDATED_RECORDED_AT,
            "recordedAt.in=" + UPDATED_RECORDED_AT
        );
    }

    @Test
    @Transactional
    void getAllAuditEventsByRecordedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where recordedAt is not null
        defaultAuditEventFiltering("recordedAt.specified=true", "recordedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditEventsByActorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where actorId equals to
        defaultAuditEventFiltering("actorId.equals=" + DEFAULT_ACTOR_ID, "actorId.equals=" + UPDATED_ACTOR_ID);
    }

    @Test
    @Transactional
    void getAllAuditEventsByActorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where actorId in
        defaultAuditEventFiltering("actorId.in=" + DEFAULT_ACTOR_ID + "," + UPDATED_ACTOR_ID, "actorId.in=" + UPDATED_ACTOR_ID);
    }

    @Test
    @Transactional
    void getAllAuditEventsByActorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where actorId is not null
        defaultAuditEventFiltering("actorId.specified=true", "actorId.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditEventsByActorIdContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where actorId contains
        defaultAuditEventFiltering("actorId.contains=" + DEFAULT_ACTOR_ID, "actorId.contains=" + UPDATED_ACTOR_ID);
    }

    @Test
    @Transactional
    void getAllAuditEventsByActorIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where actorId does not contain
        defaultAuditEventFiltering("actorId.doesNotContain=" + UPDATED_ACTOR_ID, "actorId.doesNotContain=" + DEFAULT_ACTOR_ID);
    }

    @Test
    @Transactional
    void getAllAuditEventsByActionTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where actionType equals to
        defaultAuditEventFiltering("actionType.equals=" + DEFAULT_ACTION_TYPE, "actionType.equals=" + UPDATED_ACTION_TYPE);
    }

    @Test
    @Transactional
    void getAllAuditEventsByActionTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where actionType in
        defaultAuditEventFiltering(
            "actionType.in=" + DEFAULT_ACTION_TYPE + "," + UPDATED_ACTION_TYPE,
            "actionType.in=" + UPDATED_ACTION_TYPE
        );
    }

    @Test
    @Transactional
    void getAllAuditEventsByActionTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where actionType is not null
        defaultAuditEventFiltering("actionType.specified=true", "actionType.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditEventsByActionTypeContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where actionType contains
        defaultAuditEventFiltering("actionType.contains=" + DEFAULT_ACTION_TYPE, "actionType.contains=" + UPDATED_ACTION_TYPE);
    }

    @Test
    @Transactional
    void getAllAuditEventsByActionTypeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where actionType does not contain
        defaultAuditEventFiltering("actionType.doesNotContain=" + UPDATED_ACTION_TYPE, "actionType.doesNotContain=" + DEFAULT_ACTION_TYPE);
    }

    @Test
    @Transactional
    void getAllAuditEventsByDomainIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where domain equals to
        defaultAuditEventFiltering("domain.equals=" + DEFAULT_DOMAIN, "domain.equals=" + UPDATED_DOMAIN);
    }

    @Test
    @Transactional
    void getAllAuditEventsByDomainIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where domain in
        defaultAuditEventFiltering("domain.in=" + DEFAULT_DOMAIN + "," + UPDATED_DOMAIN, "domain.in=" + UPDATED_DOMAIN);
    }

    @Test
    @Transactional
    void getAllAuditEventsByDomainIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where domain is not null
        defaultAuditEventFiltering("domain.specified=true", "domain.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditEventsByDomainContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where domain contains
        defaultAuditEventFiltering("domain.contains=" + DEFAULT_DOMAIN, "domain.contains=" + UPDATED_DOMAIN);
    }

    @Test
    @Transactional
    void getAllAuditEventsByDomainNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where domain does not contain
        defaultAuditEventFiltering("domain.doesNotContain=" + UPDATED_DOMAIN, "domain.doesNotContain=" + DEFAULT_DOMAIN);
    }

    @Test
    @Transactional
    void getAllAuditEventsBySubjectIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where subject equals to
        defaultAuditEventFiltering("subject.equals=" + DEFAULT_SUBJECT, "subject.equals=" + UPDATED_SUBJECT);
    }

    @Test
    @Transactional
    void getAllAuditEventsBySubjectIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where subject in
        defaultAuditEventFiltering("subject.in=" + DEFAULT_SUBJECT + "," + UPDATED_SUBJECT, "subject.in=" + UPDATED_SUBJECT);
    }

    @Test
    @Transactional
    void getAllAuditEventsBySubjectIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where subject is not null
        defaultAuditEventFiltering("subject.specified=true", "subject.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditEventsBySubjectContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where subject contains
        defaultAuditEventFiltering("subject.contains=" + DEFAULT_SUBJECT, "subject.contains=" + UPDATED_SUBJECT);
    }

    @Test
    @Transactional
    void getAllAuditEventsBySubjectNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where subject does not contain
        defaultAuditEventFiltering("subject.doesNotContain=" + UPDATED_SUBJECT, "subject.doesNotContain=" + DEFAULT_SUBJECT);
    }

    @Test
    @Transactional
    void getAllAuditEventsByCorrelationIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where correlationId equals to
        defaultAuditEventFiltering("correlationId.equals=" + DEFAULT_CORRELATION_ID, "correlationId.equals=" + UPDATED_CORRELATION_ID);
    }

    @Test
    @Transactional
    void getAllAuditEventsByCorrelationIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where correlationId in
        defaultAuditEventFiltering(
            "correlationId.in=" + DEFAULT_CORRELATION_ID + "," + UPDATED_CORRELATION_ID,
            "correlationId.in=" + UPDATED_CORRELATION_ID
        );
    }

    @Test
    @Transactional
    void getAllAuditEventsByCorrelationIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where correlationId is not null
        defaultAuditEventFiltering("correlationId.specified=true", "correlationId.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditEventsByLeafHashIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where leafHash equals to
        defaultAuditEventFiltering("leafHash.equals=" + DEFAULT_LEAF_HASH, "leafHash.equals=" + UPDATED_LEAF_HASH);
    }

    @Test
    @Transactional
    void getAllAuditEventsByLeafHashIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where leafHash in
        defaultAuditEventFiltering("leafHash.in=" + DEFAULT_LEAF_HASH + "," + UPDATED_LEAF_HASH, "leafHash.in=" + UPDATED_LEAF_HASH);
    }

    @Test
    @Transactional
    void getAllAuditEventsByLeafHashIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where leafHash is not null
        defaultAuditEventFiltering("leafHash.specified=true", "leafHash.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditEventsByLeafHashContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where leafHash contains
        defaultAuditEventFiltering("leafHash.contains=" + DEFAULT_LEAF_HASH, "leafHash.contains=" + UPDATED_LEAF_HASH);
    }

    @Test
    @Transactional
    void getAllAuditEventsByLeafHashNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditEvent = auditEventRepository.saveAndFlush(auditEvent);

        // Get all the auditEventList where leafHash does not contain
        defaultAuditEventFiltering("leafHash.doesNotContain=" + UPDATED_LEAF_HASH, "leafHash.doesNotContain=" + DEFAULT_LEAF_HASH);
    }

    private void defaultAuditEventFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultAuditEventShouldBeFound(shouldBeFound);
        defaultAuditEventShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultAuditEventShouldBeFound(String filter) throws Exception {
        restAuditEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(auditEvent.getId().toString())))
            .andExpect(jsonPath("$.[*].logIndex").value(hasItem(DEFAULT_LOG_INDEX.intValue())))
            .andExpect(jsonPath("$.[*].eventId").value(hasItem(DEFAULT_EVENT_ID.toString())))
            .andExpect(jsonPath("$.[*].occurredAt").value(hasItem(DEFAULT_OCCURRED_AT.toString())))
            .andExpect(jsonPath("$.[*].recordedAt").value(hasItem(DEFAULT_RECORDED_AT.toString())))
            .andExpect(jsonPath("$.[*].actorId").value(hasItem(DEFAULT_ACTOR_ID)))
            .andExpect(jsonPath("$.[*].actionType").value(hasItem(DEFAULT_ACTION_TYPE)))
            .andExpect(jsonPath("$.[*].domain").value(hasItem(DEFAULT_DOMAIN)))
            .andExpect(jsonPath("$.[*].subject").value(hasItem(DEFAULT_SUBJECT)))
            .andExpect(jsonPath("$.[*].correlationId").value(hasItem(DEFAULT_CORRELATION_ID.toString())))
            .andExpect(jsonPath("$.[*].payload").value(hasItem(DEFAULT_PAYLOAD)))
            .andExpect(jsonPath("$.[*].leafHash").value(hasItem(DEFAULT_LEAF_HASH)));

        // Check, that the count call also returns 1
        restAuditEventMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultAuditEventShouldNotBeFound(String filter) throws Exception {
        restAuditEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restAuditEventMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingAuditEvent() throws Exception {
        // Get the auditEvent
        restAuditEventMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return auditEventRepository.count();
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

    protected AuditEvent getPersistedAuditEvent(AuditEvent auditEvent) {
        return auditEventRepository.findById(auditEvent.getId()).orElseThrow();
    }

    protected void assertPersistedAuditEventToMatchAllProperties(AuditEvent expectedAuditEvent) {
        assertAuditEventAllPropertiesEquals(expectedAuditEvent, getPersistedAuditEvent(expectedAuditEvent));
    }

    protected void assertPersistedAuditEventToMatchUpdatableProperties(AuditEvent expectedAuditEvent) {
        assertAuditEventAllUpdatablePropertiesEquals(expectedAuditEvent, getPersistedAuditEvent(expectedAuditEvent));
    }
}
