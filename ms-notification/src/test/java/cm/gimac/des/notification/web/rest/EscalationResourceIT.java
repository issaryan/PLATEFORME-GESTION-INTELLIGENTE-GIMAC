package cm.gimac.des.notification.web.rest;

import static cm.gimac.des.notification.domain.EscalationAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.notification.IntegrationTest;
import cm.gimac.des.notification.domain.Escalation;
import cm.gimac.des.notification.domain.Notification;
import cm.gimac.des.notification.domain.enumeration.EscalationSource;
import cm.gimac.des.notification.repository.EscalationRepository;
import cm.gimac.des.notification.service.mapper.EscalationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link EscalationResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class EscalationResourceIT {

    private static final UUID DEFAULT_FROM_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_FROM_USER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_TO_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_TO_USER_ID = UUID.randomUUID();

    private static final Integer DEFAULT_LEVEL = 1;
    private static final Integer UPDATED_LEVEL = 2;
    private static final Integer SMALLER_LEVEL = 1 - 1;

    private static final EscalationSource DEFAULT_SOURCE = EscalationSource.MATRICE_INTERNE;
    private static final EscalationSource UPDATED_SOURCE = EscalationSource.ANNUAIRE_INSTITUTION;

    private static final Instant DEFAULT_TRIGGERED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_TRIGGERED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/escalations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EscalationRepository escalationRepository;

    @Autowired
    private EscalationMapper escalationMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEscalationMockMvc;

    private Escalation escalation;

    private Escalation insertedEscalation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Escalation createEntity(EntityManager em) {
        Escalation escalation = new Escalation()
            .fromUserId(DEFAULT_FROM_USER_ID)
            .toUserId(DEFAULT_TO_USER_ID)
            .level(DEFAULT_LEVEL)
            .source(DEFAULT_SOURCE)
            .triggeredAt(DEFAULT_TRIGGERED_AT)
            .reason(DEFAULT_REASON);
        // Add required entity
        Notification notification;
        if (TestUtil.findAll(em, Notification.class).isEmpty()) {
            notification = NotificationResourceIT.createEntity();
            em.persist(notification);
            em.flush();
        } else {
            notification = TestUtil.findAll(em, Notification.class).get(0);
        }
        escalation.setNotification(notification);
        return escalation;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Escalation createUpdatedEntity(EntityManager em) {
        Escalation updatedEscalation = new Escalation()
            .fromUserId(UPDATED_FROM_USER_ID)
            .toUserId(UPDATED_TO_USER_ID)
            .level(UPDATED_LEVEL)
            .source(UPDATED_SOURCE)
            .triggeredAt(UPDATED_TRIGGERED_AT)
            .reason(UPDATED_REASON);
        // Add required entity
        Notification notification;
        if (TestUtil.findAll(em, Notification.class).isEmpty()) {
            notification = NotificationResourceIT.createUpdatedEntity();
            em.persist(notification);
            em.flush();
        } else {
            notification = TestUtil.findAll(em, Notification.class).get(0);
        }
        updatedEscalation.setNotification(notification);
        return updatedEscalation;
    }

    @BeforeEach
    void initTest() {
        escalation = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedEscalation != null) {
            escalationRepository.delete(insertedEscalation);
            insertedEscalation = null;
        }
    }

    @Test
    @Transactional
    void getAllEscalations() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(escalation.getId().toString())))
            .andExpect(jsonPath("$.[*].fromUserId").value(hasItem(DEFAULT_FROM_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].toUserId").value(hasItem(DEFAULT_TO_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].level").value(hasItem(DEFAULT_LEVEL)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].triggeredAt").value(hasItem(DEFAULT_TRIGGERED_AT.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)));
    }

    @Test
    @Transactional
    void getEscalation() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get the escalation
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL_ID, escalation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(escalation.getId().toString()))
            .andExpect(jsonPath("$.fromUserId").value(DEFAULT_FROM_USER_ID.toString()))
            .andExpect(jsonPath("$.toUserId").value(DEFAULT_TO_USER_ID.toString()))
            .andExpect(jsonPath("$.level").value(DEFAULT_LEVEL))
            .andExpect(jsonPath("$.source").value(DEFAULT_SOURCE.toString()))
            .andExpect(jsonPath("$.triggeredAt").value(DEFAULT_TRIGGERED_AT.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON));
    }

    @Test
    @Transactional
    void getEscalationsByIdFiltering() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        UUID id = escalation.getId();

        defaultEscalationFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllEscalationsByFromUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where fromUserId equals to
        defaultEscalationFiltering("fromUserId.equals=" + DEFAULT_FROM_USER_ID, "fromUserId.equals=" + UPDATED_FROM_USER_ID);
    }

    @Test
    @Transactional
    void getAllEscalationsByFromUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where fromUserId in
        defaultEscalationFiltering(
            "fromUserId.in=" + DEFAULT_FROM_USER_ID + "," + UPDATED_FROM_USER_ID,
            "fromUserId.in=" + UPDATED_FROM_USER_ID
        );
    }

    @Test
    @Transactional
    void getAllEscalationsByFromUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where fromUserId is not null
        defaultEscalationFiltering("fromUserId.specified=true", "fromUserId.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByToUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where toUserId equals to
        defaultEscalationFiltering("toUserId.equals=" + DEFAULT_TO_USER_ID, "toUserId.equals=" + UPDATED_TO_USER_ID);
    }

    @Test
    @Transactional
    void getAllEscalationsByToUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where toUserId in
        defaultEscalationFiltering("toUserId.in=" + DEFAULT_TO_USER_ID + "," + UPDATED_TO_USER_ID, "toUserId.in=" + UPDATED_TO_USER_ID);
    }

    @Test
    @Transactional
    void getAllEscalationsByToUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where toUserId is not null
        defaultEscalationFiltering("toUserId.specified=true", "toUserId.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level equals to
        defaultEscalationFiltering("level.equals=" + DEFAULT_LEVEL, "level.equals=" + UPDATED_LEVEL);
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level in
        defaultEscalationFiltering("level.in=" + DEFAULT_LEVEL + "," + UPDATED_LEVEL, "level.in=" + UPDATED_LEVEL);
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level is not null
        defaultEscalationFiltering("level.specified=true", "level.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level is greater than or equal to
        defaultEscalationFiltering("level.greaterThanOrEqual=" + DEFAULT_LEVEL, "level.greaterThanOrEqual=" + (DEFAULT_LEVEL + 1));
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level is less than or equal to
        defaultEscalationFiltering("level.lessThanOrEqual=" + DEFAULT_LEVEL, "level.lessThanOrEqual=" + SMALLER_LEVEL);
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level is less than
        defaultEscalationFiltering("level.lessThan=" + (DEFAULT_LEVEL + 1), "level.lessThan=" + DEFAULT_LEVEL);
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level is greater than
        defaultEscalationFiltering("level.greaterThan=" + SMALLER_LEVEL, "level.greaterThan=" + DEFAULT_LEVEL);
    }

    @Test
    @Transactional
    void getAllEscalationsBySourceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where source equals to
        defaultEscalationFiltering("source.equals=" + DEFAULT_SOURCE, "source.equals=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllEscalationsBySourceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where source in
        defaultEscalationFiltering("source.in=" + DEFAULT_SOURCE + "," + UPDATED_SOURCE, "source.in=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllEscalationsBySourceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where source is not null
        defaultEscalationFiltering("source.specified=true", "source.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByTriggeredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where triggeredAt equals to
        defaultEscalationFiltering("triggeredAt.equals=" + DEFAULT_TRIGGERED_AT, "triggeredAt.equals=" + UPDATED_TRIGGERED_AT);
    }

    @Test
    @Transactional
    void getAllEscalationsByTriggeredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where triggeredAt in
        defaultEscalationFiltering(
            "triggeredAt.in=" + DEFAULT_TRIGGERED_AT + "," + UPDATED_TRIGGERED_AT,
            "triggeredAt.in=" + UPDATED_TRIGGERED_AT
        );
    }

    @Test
    @Transactional
    void getAllEscalationsByTriggeredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where triggeredAt is not null
        defaultEscalationFiltering("triggeredAt.specified=true", "triggeredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where reason equals to
        defaultEscalationFiltering("reason.equals=" + DEFAULT_REASON, "reason.equals=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllEscalationsByReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where reason in
        defaultEscalationFiltering("reason.in=" + DEFAULT_REASON + "," + UPDATED_REASON, "reason.in=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllEscalationsByReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where reason is not null
        defaultEscalationFiltering("reason.specified=true", "reason.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where reason contains
        defaultEscalationFiltering("reason.contains=" + DEFAULT_REASON, "reason.contains=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllEscalationsByReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where reason does not contain
        defaultEscalationFiltering("reason.doesNotContain=" + UPDATED_REASON, "reason.doesNotContain=" + DEFAULT_REASON);
    }

    @Test
    @Transactional
    void getAllEscalationsByNotificationIsEqualToSomething() throws Exception {
        Notification notification;
        if (TestUtil.findAll(em, Notification.class).isEmpty()) {
            escalationRepository.saveAndFlush(escalation);
            notification = NotificationResourceIT.createEntity();
        } else {
            notification = TestUtil.findAll(em, Notification.class).get(0);
        }
        em.persist(notification);
        em.flush();
        escalation.setNotification(notification);
        escalationRepository.saveAndFlush(escalation);
        UUID notificationId = notification.getId();
        // Get all the escalationList where notification equals to notificationId
        defaultEscalationShouldBeFound("notificationId.equals=" + notificationId);

        // Get all the escalationList where notification equals to UUID.randomUUID()
        defaultEscalationShouldNotBeFound("notificationId.equals=" + UUID.randomUUID());
    }

    private void defaultEscalationFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultEscalationShouldBeFound(shouldBeFound);
        defaultEscalationShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultEscalationShouldBeFound(String filter) throws Exception {
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(escalation.getId().toString())))
            .andExpect(jsonPath("$.[*].fromUserId").value(hasItem(DEFAULT_FROM_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].toUserId").value(hasItem(DEFAULT_TO_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].level").value(hasItem(DEFAULT_LEVEL)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].triggeredAt").value(hasItem(DEFAULT_TRIGGERED_AT.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)));

        // Check, that the count call also returns 1
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultEscalationShouldNotBeFound(String filter) throws Exception {
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingEscalation() throws Exception {
        // Get the escalation
        restEscalationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return escalationRepository.count();
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

    protected Escalation getPersistedEscalation(Escalation escalation) {
        return escalationRepository.findById(escalation.getId()).orElseThrow();
    }

    protected void assertPersistedEscalationToMatchAllProperties(Escalation expectedEscalation) {
        assertEscalationAllPropertiesEquals(expectedEscalation, getPersistedEscalation(expectedEscalation));
    }

    protected void assertPersistedEscalationToMatchUpdatableProperties(Escalation expectedEscalation) {
        assertEscalationAllUpdatablePropertiesEquals(expectedEscalation, getPersistedEscalation(expectedEscalation));
    }
}
