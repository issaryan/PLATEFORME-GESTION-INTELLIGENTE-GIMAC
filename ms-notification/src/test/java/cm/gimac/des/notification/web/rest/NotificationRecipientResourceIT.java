package cm.gimac.des.notification.web.rest;

import static cm.gimac.des.notification.domain.NotificationRecipientAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.notification.IntegrationTest;
import cm.gimac.des.notification.domain.Notification;
import cm.gimac.des.notification.domain.NotificationRecipient;
import cm.gimac.des.notification.domain.NotificationSubscription;
import cm.gimac.des.notification.domain.enumeration.RuleType;
import cm.gimac.des.notification.repository.NotificationRecipientRepository;
import cm.gimac.des.notification.service.mapper.NotificationRecipientMapper;
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
 * Integration tests for the {@link NotificationRecipientResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class NotificationRecipientResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final RuleType DEFAULT_RESOLVED_BY = RuleType.ROLE;
    private static final RuleType UPDATED_RESOLVED_BY = RuleType.INSTITUTION;

    private static final String DEFAULT_CHANNELS = "AAAAAAAAAA";
    private static final String UPDATED_CHANNELS = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ESCALATED = false;
    private static final Boolean UPDATED_ESCALATED = true;

    private static final Instant DEFAULT_RESOLVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RESOLVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_READ_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_READ_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_ACKNOWLEDGED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ACKNOWLEDGED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/notification-recipients";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private NotificationRecipientRepository notificationRecipientRepository;

    @Autowired
    private NotificationRecipientMapper notificationRecipientMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restNotificationRecipientMockMvc;

    private NotificationRecipient notificationRecipient;

    private NotificationRecipient insertedNotificationRecipient;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static NotificationRecipient createEntity(EntityManager em) {
        NotificationRecipient notificationRecipient = new NotificationRecipient()
            .userId(DEFAULT_USER_ID)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .resolvedBy(DEFAULT_RESOLVED_BY)
            .channels(DEFAULT_CHANNELS)
            .escalated(DEFAULT_ESCALATED)
            .resolvedAt(DEFAULT_RESOLVED_AT)
            .readAt(DEFAULT_READ_AT)
            .acknowledgedAt(DEFAULT_ACKNOWLEDGED_AT);
        // Add required entity
        Notification notification;
        if (TestUtil.findAll(em, Notification.class).isEmpty()) {
            notification = NotificationResourceIT.createEntity();
            em.persist(notification);
            em.flush();
        } else {
            notification = TestUtil.findAll(em, Notification.class).get(0);
        }
        notificationRecipient.setNotification(notification);
        return notificationRecipient;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static NotificationRecipient createUpdatedEntity(EntityManager em) {
        NotificationRecipient updatedNotificationRecipient = new NotificationRecipient()
            .userId(UPDATED_USER_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .resolvedBy(UPDATED_RESOLVED_BY)
            .channels(UPDATED_CHANNELS)
            .escalated(UPDATED_ESCALATED)
            .resolvedAt(UPDATED_RESOLVED_AT)
            .readAt(UPDATED_READ_AT)
            .acknowledgedAt(UPDATED_ACKNOWLEDGED_AT);
        // Add required entity
        Notification notification;
        if (TestUtil.findAll(em, Notification.class).isEmpty()) {
            notification = NotificationResourceIT.createUpdatedEntity();
            em.persist(notification);
            em.flush();
        } else {
            notification = TestUtil.findAll(em, Notification.class).get(0);
        }
        updatedNotificationRecipient.setNotification(notification);
        return updatedNotificationRecipient;
    }

    @BeforeEach
    void initTest() {
        notificationRecipient = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedNotificationRecipient != null) {
            notificationRecipientRepository.delete(insertedNotificationRecipient);
            insertedNotificationRecipient = null;
        }
    }

    @Test
    @Transactional
    void getAllNotificationRecipients() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList
        restNotificationRecipientMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(notificationRecipient.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].resolvedBy").value(hasItem(DEFAULT_RESOLVED_BY.toString())))
            .andExpect(jsonPath("$.[*].channels").value(hasItem(DEFAULT_CHANNELS)))
            .andExpect(jsonPath("$.[*].escalated").value(hasItem(DEFAULT_ESCALATED)))
            .andExpect(jsonPath("$.[*].resolvedAt").value(hasItem(DEFAULT_RESOLVED_AT.toString())))
            .andExpect(jsonPath("$.[*].readAt").value(hasItem(DEFAULT_READ_AT.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())));
    }

    @Test
    @Transactional
    void getNotificationRecipient() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get the notificationRecipient
        restNotificationRecipientMockMvc
            .perform(get(ENTITY_API_URL_ID, notificationRecipient.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(notificationRecipient.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.resolvedBy").value(DEFAULT_RESOLVED_BY.toString()))
            .andExpect(jsonPath("$.channels").value(DEFAULT_CHANNELS))
            .andExpect(jsonPath("$.escalated").value(DEFAULT_ESCALATED))
            .andExpect(jsonPath("$.resolvedAt").value(DEFAULT_RESOLVED_AT.toString()))
            .andExpect(jsonPath("$.readAt").value(DEFAULT_READ_AT.toString()))
            .andExpect(jsonPath("$.acknowledgedAt").value(DEFAULT_ACKNOWLEDGED_AT.toString()));
    }

    @Test
    @Transactional
    void getNotificationRecipientsByIdFiltering() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        UUID id = notificationRecipient.getId();

        defaultNotificationRecipientFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where userId equals to
        defaultNotificationRecipientFiltering("userId.equals=" + DEFAULT_USER_ID, "userId.equals=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where userId in
        defaultNotificationRecipientFiltering("userId.in=" + DEFAULT_USER_ID + "," + UPDATED_USER_ID, "userId.in=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where userId is not null
        defaultNotificationRecipientFiltering("userId.specified=true", "userId.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where institutionId equals to
        defaultNotificationRecipientFiltering(
            "institutionId.equals=" + DEFAULT_INSTITUTION_ID,
            "institutionId.equals=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where institutionId in
        defaultNotificationRecipientFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where institutionId is not null
        defaultNotificationRecipientFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByResolvedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where resolvedBy equals to
        defaultNotificationRecipientFiltering("resolvedBy.equals=" + DEFAULT_RESOLVED_BY, "resolvedBy.equals=" + UPDATED_RESOLVED_BY);
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByResolvedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where resolvedBy in
        defaultNotificationRecipientFiltering(
            "resolvedBy.in=" + DEFAULT_RESOLVED_BY + "," + UPDATED_RESOLVED_BY,
            "resolvedBy.in=" + UPDATED_RESOLVED_BY
        );
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByResolvedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where resolvedBy is not null
        defaultNotificationRecipientFiltering("resolvedBy.specified=true", "resolvedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByChannelsIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where channels equals to
        defaultNotificationRecipientFiltering("channels.equals=" + DEFAULT_CHANNELS, "channels.equals=" + UPDATED_CHANNELS);
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByChannelsIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where channels in
        defaultNotificationRecipientFiltering(
            "channels.in=" + DEFAULT_CHANNELS + "," + UPDATED_CHANNELS,
            "channels.in=" + UPDATED_CHANNELS
        );
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByChannelsIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where channels is not null
        defaultNotificationRecipientFiltering("channels.specified=true", "channels.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByChannelsContainsSomething() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where channels contains
        defaultNotificationRecipientFiltering("channels.contains=" + DEFAULT_CHANNELS, "channels.contains=" + UPDATED_CHANNELS);
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByChannelsNotContainsSomething() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where channels does not contain
        defaultNotificationRecipientFiltering("channels.doesNotContain=" + UPDATED_CHANNELS, "channels.doesNotContain=" + DEFAULT_CHANNELS);
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByEscalatedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where escalated equals to
        defaultNotificationRecipientFiltering("escalated.equals=" + DEFAULT_ESCALATED, "escalated.equals=" + UPDATED_ESCALATED);
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByEscalatedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where escalated in
        defaultNotificationRecipientFiltering(
            "escalated.in=" + DEFAULT_ESCALATED + "," + UPDATED_ESCALATED,
            "escalated.in=" + UPDATED_ESCALATED
        );
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByEscalatedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where escalated is not null
        defaultNotificationRecipientFiltering("escalated.specified=true", "escalated.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByResolvedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where resolvedAt equals to
        defaultNotificationRecipientFiltering("resolvedAt.equals=" + DEFAULT_RESOLVED_AT, "resolvedAt.equals=" + UPDATED_RESOLVED_AT);
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByResolvedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where resolvedAt in
        defaultNotificationRecipientFiltering(
            "resolvedAt.in=" + DEFAULT_RESOLVED_AT + "," + UPDATED_RESOLVED_AT,
            "resolvedAt.in=" + UPDATED_RESOLVED_AT
        );
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByResolvedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where resolvedAt is not null
        defaultNotificationRecipientFiltering("resolvedAt.specified=true", "resolvedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByReadAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where readAt equals to
        defaultNotificationRecipientFiltering("readAt.equals=" + DEFAULT_READ_AT, "readAt.equals=" + UPDATED_READ_AT);
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByReadAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where readAt in
        defaultNotificationRecipientFiltering("readAt.in=" + DEFAULT_READ_AT + "," + UPDATED_READ_AT, "readAt.in=" + UPDATED_READ_AT);
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByReadAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where readAt is not null
        defaultNotificationRecipientFiltering("readAt.specified=true", "readAt.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByAcknowledgedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where acknowledgedAt equals to
        defaultNotificationRecipientFiltering(
            "acknowledgedAt.equals=" + DEFAULT_ACKNOWLEDGED_AT,
            "acknowledgedAt.equals=" + UPDATED_ACKNOWLEDGED_AT
        );
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByAcknowledgedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where acknowledgedAt in
        defaultNotificationRecipientFiltering(
            "acknowledgedAt.in=" + DEFAULT_ACKNOWLEDGED_AT + "," + UPDATED_ACKNOWLEDGED_AT,
            "acknowledgedAt.in=" + UPDATED_ACKNOWLEDGED_AT
        );
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByAcknowledgedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationRecipient = notificationRecipientRepository.saveAndFlush(notificationRecipient);

        // Get all the notificationRecipientList where acknowledgedAt is not null
        defaultNotificationRecipientFiltering("acknowledgedAt.specified=true", "acknowledgedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsByNotificationIsEqualToSomething() throws Exception {
        Notification notification;
        if (TestUtil.findAll(em, Notification.class).isEmpty()) {
            notificationRecipientRepository.saveAndFlush(notificationRecipient);
            notification = NotificationResourceIT.createEntity();
        } else {
            notification = TestUtil.findAll(em, Notification.class).get(0);
        }
        em.persist(notification);
        em.flush();
        notificationRecipient.setNotification(notification);
        notificationRecipientRepository.saveAndFlush(notificationRecipient);
        UUID notificationId = notification.getId();
        // Get all the notificationRecipientList where notification equals to notificationId
        defaultNotificationRecipientShouldBeFound("notificationId.equals=" + notificationId);

        // Get all the notificationRecipientList where notification equals to UUID.randomUUID()
        defaultNotificationRecipientShouldNotBeFound("notificationId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllNotificationRecipientsBySourceSubscriptionIsEqualToSomething() throws Exception {
        NotificationSubscription sourceSubscription;
        if (TestUtil.findAll(em, NotificationSubscription.class).isEmpty()) {
            notificationRecipientRepository.saveAndFlush(notificationRecipient);
            sourceSubscription = NotificationSubscriptionResourceIT.createEntity();
        } else {
            sourceSubscription = TestUtil.findAll(em, NotificationSubscription.class).get(0);
        }
        em.persist(sourceSubscription);
        em.flush();
        notificationRecipient.setSourceSubscription(sourceSubscription);
        notificationRecipientRepository.saveAndFlush(notificationRecipient);
        UUID sourceSubscriptionId = sourceSubscription.getId();
        // Get all the notificationRecipientList where sourceSubscription equals to sourceSubscriptionId
        defaultNotificationRecipientShouldBeFound("sourceSubscriptionId.equals=" + sourceSubscriptionId);

        // Get all the notificationRecipientList where sourceSubscription equals to UUID.randomUUID()
        defaultNotificationRecipientShouldNotBeFound("sourceSubscriptionId.equals=" + UUID.randomUUID());
    }

    private void defaultNotificationRecipientFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultNotificationRecipientShouldBeFound(shouldBeFound);
        defaultNotificationRecipientShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultNotificationRecipientShouldBeFound(String filter) throws Exception {
        restNotificationRecipientMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(notificationRecipient.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].resolvedBy").value(hasItem(DEFAULT_RESOLVED_BY.toString())))
            .andExpect(jsonPath("$.[*].channels").value(hasItem(DEFAULT_CHANNELS)))
            .andExpect(jsonPath("$.[*].escalated").value(hasItem(DEFAULT_ESCALATED)))
            .andExpect(jsonPath("$.[*].resolvedAt").value(hasItem(DEFAULT_RESOLVED_AT.toString())))
            .andExpect(jsonPath("$.[*].readAt").value(hasItem(DEFAULT_READ_AT.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())));

        // Check, that the count call also returns 1
        restNotificationRecipientMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultNotificationRecipientShouldNotBeFound(String filter) throws Exception {
        restNotificationRecipientMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restNotificationRecipientMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingNotificationRecipient() throws Exception {
        // Get the notificationRecipient
        restNotificationRecipientMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return notificationRecipientRepository.count();
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

    protected NotificationRecipient getPersistedNotificationRecipient(NotificationRecipient notificationRecipient) {
        return notificationRecipientRepository.findById(notificationRecipient.getId()).orElseThrow();
    }

    protected void assertPersistedNotificationRecipientToMatchAllProperties(NotificationRecipient expectedNotificationRecipient) {
        assertNotificationRecipientAllPropertiesEquals(
            expectedNotificationRecipient,
            getPersistedNotificationRecipient(expectedNotificationRecipient)
        );
    }

    protected void assertPersistedNotificationRecipientToMatchUpdatableProperties(NotificationRecipient expectedNotificationRecipient) {
        assertNotificationRecipientAllUpdatablePropertiesEquals(
            expectedNotificationRecipient,
            getPersistedNotificationRecipient(expectedNotificationRecipient)
        );
    }
}
