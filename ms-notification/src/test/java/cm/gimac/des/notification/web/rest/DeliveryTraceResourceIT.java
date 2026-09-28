package cm.gimac.des.notification.web.rest;

import static cm.gimac.des.notification.domain.DeliveryTraceAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.notification.IntegrationTest;
import cm.gimac.des.notification.domain.DeliveryTrace;
import cm.gimac.des.notification.domain.MessageTemplate;
import cm.gimac.des.notification.domain.Notification;
import cm.gimac.des.notification.domain.enumeration.Channel;
import cm.gimac.des.notification.domain.enumeration.DeliveryState;
import cm.gimac.des.notification.repository.DeliveryTraceRepository;
import cm.gimac.des.notification.service.mapper.DeliveryTraceMapper;
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
 * Integration tests for the {@link DeliveryTraceResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DeliveryTraceResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final Channel DEFAULT_CHANNEL = Channel.PLATEFORME;
    private static final Channel UPDATED_CHANNEL = Channel.POUSSE;

    private static final DeliveryState DEFAULT_STATE = DeliveryState.EN_ATTENTE;
    private static final DeliveryState UPDATED_STATE = DeliveryState.REMISE;

    private static final Integer DEFAULT_ATTEMPTS = 0;
    private static final Integer UPDATED_ATTEMPTS = 1;
    private static final Integer SMALLER_ATTEMPTS = 0 - 1;

    private static final Instant DEFAULT_NEXT_ATTEMPT_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_NEXT_ATTEMPT_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_LAST_ERROR = "AAAAAAAAAA";
    private static final String UPDATED_LAST_ERROR = "BBBBBBBBBB";

    private static final String DEFAULT_PROVIDER_MESSAGE_ID = "AAAAAAAAAA";
    private static final String UPDATED_PROVIDER_MESSAGE_ID = "BBBBBBBBBB";

    private static final Instant DEFAULT_DELIVERED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DELIVERED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_ACKNOWLEDGED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ACKNOWLEDGED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/delivery-traces";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DeliveryTraceRepository deliveryTraceRepository;

    @Autowired
    private DeliveryTraceMapper deliveryTraceMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDeliveryTraceMockMvc;

    private DeliveryTrace deliveryTrace;

    private DeliveryTrace insertedDeliveryTrace;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DeliveryTrace createEntity(EntityManager em) {
        DeliveryTrace deliveryTrace = new DeliveryTrace()
            .userId(DEFAULT_USER_ID)
            .channel(DEFAULT_CHANNEL)
            .state(DEFAULT_STATE)
            .attempts(DEFAULT_ATTEMPTS)
            .nextAttemptAt(DEFAULT_NEXT_ATTEMPT_AT)
            .lastError(DEFAULT_LAST_ERROR)
            .providerMessageId(DEFAULT_PROVIDER_MESSAGE_ID)
            .deliveredAt(DEFAULT_DELIVERED_AT)
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
        deliveryTrace.setNotification(notification);
        return deliveryTrace;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DeliveryTrace createUpdatedEntity(EntityManager em) {
        DeliveryTrace updatedDeliveryTrace = new DeliveryTrace()
            .userId(UPDATED_USER_ID)
            .channel(UPDATED_CHANNEL)
            .state(UPDATED_STATE)
            .attempts(UPDATED_ATTEMPTS)
            .nextAttemptAt(UPDATED_NEXT_ATTEMPT_AT)
            .lastError(UPDATED_LAST_ERROR)
            .providerMessageId(UPDATED_PROVIDER_MESSAGE_ID)
            .deliveredAt(UPDATED_DELIVERED_AT)
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
        updatedDeliveryTrace.setNotification(notification);
        return updatedDeliveryTrace;
    }

    @BeforeEach
    void initTest() {
        deliveryTrace = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedDeliveryTrace != null) {
            deliveryTraceRepository.delete(insertedDeliveryTrace);
            insertedDeliveryTrace = null;
        }
    }

    @Test
    @Transactional
    void getAllDeliveryTraces() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList
        restDeliveryTraceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(deliveryTrace.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].channel").value(hasItem(DEFAULT_CHANNEL.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].attempts").value(hasItem(DEFAULT_ATTEMPTS)))
            .andExpect(jsonPath("$.[*].nextAttemptAt").value(hasItem(DEFAULT_NEXT_ATTEMPT_AT.toString())))
            .andExpect(jsonPath("$.[*].lastError").value(hasItem(DEFAULT_LAST_ERROR)))
            .andExpect(jsonPath("$.[*].providerMessageId").value(hasItem(DEFAULT_PROVIDER_MESSAGE_ID)))
            .andExpect(jsonPath("$.[*].deliveredAt").value(hasItem(DEFAULT_DELIVERED_AT.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())));
    }

    @Test
    @Transactional
    void getDeliveryTrace() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get the deliveryTrace
        restDeliveryTraceMockMvc
            .perform(get(ENTITY_API_URL_ID, deliveryTrace.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(deliveryTrace.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.channel").value(DEFAULT_CHANNEL.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.attempts").value(DEFAULT_ATTEMPTS))
            .andExpect(jsonPath("$.nextAttemptAt").value(DEFAULT_NEXT_ATTEMPT_AT.toString()))
            .andExpect(jsonPath("$.lastError").value(DEFAULT_LAST_ERROR))
            .andExpect(jsonPath("$.providerMessageId").value(DEFAULT_PROVIDER_MESSAGE_ID))
            .andExpect(jsonPath("$.deliveredAt").value(DEFAULT_DELIVERED_AT.toString()))
            .andExpect(jsonPath("$.acknowledgedAt").value(DEFAULT_ACKNOWLEDGED_AT.toString()));
    }

    @Test
    @Transactional
    void getDeliveryTracesByIdFiltering() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        UUID id = deliveryTrace.getId();

        defaultDeliveryTraceFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where userId equals to
        defaultDeliveryTraceFiltering("userId.equals=" + DEFAULT_USER_ID, "userId.equals=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where userId in
        defaultDeliveryTraceFiltering("userId.in=" + DEFAULT_USER_ID + "," + UPDATED_USER_ID, "userId.in=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where userId is not null
        defaultDeliveryTraceFiltering("userId.specified=true", "userId.specified=false");
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByChannelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where channel equals to
        defaultDeliveryTraceFiltering("channel.equals=" + DEFAULT_CHANNEL, "channel.equals=" + UPDATED_CHANNEL);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByChannelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where channel in
        defaultDeliveryTraceFiltering("channel.in=" + DEFAULT_CHANNEL + "," + UPDATED_CHANNEL, "channel.in=" + UPDATED_CHANNEL);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByChannelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where channel is not null
        defaultDeliveryTraceFiltering("channel.specified=true", "channel.specified=false");
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where state equals to
        defaultDeliveryTraceFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where state in
        defaultDeliveryTraceFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where state is not null
        defaultDeliveryTraceFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByAttemptsIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where attempts equals to
        defaultDeliveryTraceFiltering("attempts.equals=" + DEFAULT_ATTEMPTS, "attempts.equals=" + UPDATED_ATTEMPTS);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByAttemptsIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where attempts in
        defaultDeliveryTraceFiltering("attempts.in=" + DEFAULT_ATTEMPTS + "," + UPDATED_ATTEMPTS, "attempts.in=" + UPDATED_ATTEMPTS);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByAttemptsIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where attempts is not null
        defaultDeliveryTraceFiltering("attempts.specified=true", "attempts.specified=false");
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByAttemptsIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where attempts is greater than or equal to
        defaultDeliveryTraceFiltering(
            "attempts.greaterThanOrEqual=" + DEFAULT_ATTEMPTS,
            "attempts.greaterThanOrEqual=" + (DEFAULT_ATTEMPTS + 1)
        );
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByAttemptsIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where attempts is less than or equal to
        defaultDeliveryTraceFiltering("attempts.lessThanOrEqual=" + DEFAULT_ATTEMPTS, "attempts.lessThanOrEqual=" + SMALLER_ATTEMPTS);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByAttemptsIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where attempts is less than
        defaultDeliveryTraceFiltering("attempts.lessThan=" + (DEFAULT_ATTEMPTS + 1), "attempts.lessThan=" + DEFAULT_ATTEMPTS);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByAttemptsIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where attempts is greater than
        defaultDeliveryTraceFiltering("attempts.greaterThan=" + SMALLER_ATTEMPTS, "attempts.greaterThan=" + DEFAULT_ATTEMPTS);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByNextAttemptAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where nextAttemptAt equals to
        defaultDeliveryTraceFiltering("nextAttemptAt.equals=" + DEFAULT_NEXT_ATTEMPT_AT, "nextAttemptAt.equals=" + UPDATED_NEXT_ATTEMPT_AT);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByNextAttemptAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where nextAttemptAt in
        defaultDeliveryTraceFiltering(
            "nextAttemptAt.in=" + DEFAULT_NEXT_ATTEMPT_AT + "," + UPDATED_NEXT_ATTEMPT_AT,
            "nextAttemptAt.in=" + UPDATED_NEXT_ATTEMPT_AT
        );
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByNextAttemptAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where nextAttemptAt is not null
        defaultDeliveryTraceFiltering("nextAttemptAt.specified=true", "nextAttemptAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByLastErrorIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where lastError equals to
        defaultDeliveryTraceFiltering("lastError.equals=" + DEFAULT_LAST_ERROR, "lastError.equals=" + UPDATED_LAST_ERROR);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByLastErrorIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where lastError in
        defaultDeliveryTraceFiltering(
            "lastError.in=" + DEFAULT_LAST_ERROR + "," + UPDATED_LAST_ERROR,
            "lastError.in=" + UPDATED_LAST_ERROR
        );
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByLastErrorIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where lastError is not null
        defaultDeliveryTraceFiltering("lastError.specified=true", "lastError.specified=false");
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByLastErrorContainsSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where lastError contains
        defaultDeliveryTraceFiltering("lastError.contains=" + DEFAULT_LAST_ERROR, "lastError.contains=" + UPDATED_LAST_ERROR);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByLastErrorNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where lastError does not contain
        defaultDeliveryTraceFiltering("lastError.doesNotContain=" + UPDATED_LAST_ERROR, "lastError.doesNotContain=" + DEFAULT_LAST_ERROR);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByProviderMessageIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where providerMessageId equals to
        defaultDeliveryTraceFiltering(
            "providerMessageId.equals=" + DEFAULT_PROVIDER_MESSAGE_ID,
            "providerMessageId.equals=" + UPDATED_PROVIDER_MESSAGE_ID
        );
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByProviderMessageIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where providerMessageId in
        defaultDeliveryTraceFiltering(
            "providerMessageId.in=" + DEFAULT_PROVIDER_MESSAGE_ID + "," + UPDATED_PROVIDER_MESSAGE_ID,
            "providerMessageId.in=" + UPDATED_PROVIDER_MESSAGE_ID
        );
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByProviderMessageIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where providerMessageId is not null
        defaultDeliveryTraceFiltering("providerMessageId.specified=true", "providerMessageId.specified=false");
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByProviderMessageIdContainsSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where providerMessageId contains
        defaultDeliveryTraceFiltering(
            "providerMessageId.contains=" + DEFAULT_PROVIDER_MESSAGE_ID,
            "providerMessageId.contains=" + UPDATED_PROVIDER_MESSAGE_ID
        );
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByProviderMessageIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where providerMessageId does not contain
        defaultDeliveryTraceFiltering(
            "providerMessageId.doesNotContain=" + UPDATED_PROVIDER_MESSAGE_ID,
            "providerMessageId.doesNotContain=" + DEFAULT_PROVIDER_MESSAGE_ID
        );
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByDeliveredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where deliveredAt equals to
        defaultDeliveryTraceFiltering("deliveredAt.equals=" + DEFAULT_DELIVERED_AT, "deliveredAt.equals=" + UPDATED_DELIVERED_AT);
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByDeliveredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where deliveredAt in
        defaultDeliveryTraceFiltering(
            "deliveredAt.in=" + DEFAULT_DELIVERED_AT + "," + UPDATED_DELIVERED_AT,
            "deliveredAt.in=" + UPDATED_DELIVERED_AT
        );
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByDeliveredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where deliveredAt is not null
        defaultDeliveryTraceFiltering("deliveredAt.specified=true", "deliveredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByAcknowledgedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where acknowledgedAt equals to
        defaultDeliveryTraceFiltering(
            "acknowledgedAt.equals=" + DEFAULT_ACKNOWLEDGED_AT,
            "acknowledgedAt.equals=" + UPDATED_ACKNOWLEDGED_AT
        );
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByAcknowledgedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where acknowledgedAt in
        defaultDeliveryTraceFiltering(
            "acknowledgedAt.in=" + DEFAULT_ACKNOWLEDGED_AT + "," + UPDATED_ACKNOWLEDGED_AT,
            "acknowledgedAt.in=" + UPDATED_ACKNOWLEDGED_AT
        );
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByAcknowledgedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeliveryTrace = deliveryTraceRepository.saveAndFlush(deliveryTrace);

        // Get all the deliveryTraceList where acknowledgedAt is not null
        defaultDeliveryTraceFiltering("acknowledgedAt.specified=true", "acknowledgedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByNotificationIsEqualToSomething() throws Exception {
        Notification notification;
        if (TestUtil.findAll(em, Notification.class).isEmpty()) {
            deliveryTraceRepository.saveAndFlush(deliveryTrace);
            notification = NotificationResourceIT.createEntity();
        } else {
            notification = TestUtil.findAll(em, Notification.class).get(0);
        }
        em.persist(notification);
        em.flush();
        deliveryTrace.setNotification(notification);
        deliveryTraceRepository.saveAndFlush(deliveryTrace);
        UUID notificationId = notification.getId();
        // Get all the deliveryTraceList where notification equals to notificationId
        defaultDeliveryTraceShouldBeFound("notificationId.equals=" + notificationId);

        // Get all the deliveryTraceList where notification equals to UUID.randomUUID()
        defaultDeliveryTraceShouldNotBeFound("notificationId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllDeliveryTracesByTemplateIsEqualToSomething() throws Exception {
        MessageTemplate template;
        if (TestUtil.findAll(em, MessageTemplate.class).isEmpty()) {
            deliveryTraceRepository.saveAndFlush(deliveryTrace);
            template = MessageTemplateResourceIT.createEntity();
        } else {
            template = TestUtil.findAll(em, MessageTemplate.class).get(0);
        }
        em.persist(template);
        em.flush();
        deliveryTrace.setTemplate(template);
        deliveryTraceRepository.saveAndFlush(deliveryTrace);
        UUID templateId = template.getId();
        // Get all the deliveryTraceList where template equals to templateId
        defaultDeliveryTraceShouldBeFound("templateId.equals=" + templateId);

        // Get all the deliveryTraceList where template equals to UUID.randomUUID()
        defaultDeliveryTraceShouldNotBeFound("templateId.equals=" + UUID.randomUUID());
    }

    private void defaultDeliveryTraceFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultDeliveryTraceShouldBeFound(shouldBeFound);
        defaultDeliveryTraceShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultDeliveryTraceShouldBeFound(String filter) throws Exception {
        restDeliveryTraceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(deliveryTrace.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].channel").value(hasItem(DEFAULT_CHANNEL.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].attempts").value(hasItem(DEFAULT_ATTEMPTS)))
            .andExpect(jsonPath("$.[*].nextAttemptAt").value(hasItem(DEFAULT_NEXT_ATTEMPT_AT.toString())))
            .andExpect(jsonPath("$.[*].lastError").value(hasItem(DEFAULT_LAST_ERROR)))
            .andExpect(jsonPath("$.[*].providerMessageId").value(hasItem(DEFAULT_PROVIDER_MESSAGE_ID)))
            .andExpect(jsonPath("$.[*].deliveredAt").value(hasItem(DEFAULT_DELIVERED_AT.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())));

        // Check, that the count call also returns 1
        restDeliveryTraceMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultDeliveryTraceShouldNotBeFound(String filter) throws Exception {
        restDeliveryTraceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restDeliveryTraceMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingDeliveryTrace() throws Exception {
        // Get the deliveryTrace
        restDeliveryTraceMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return deliveryTraceRepository.count();
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

    protected DeliveryTrace getPersistedDeliveryTrace(DeliveryTrace deliveryTrace) {
        return deliveryTraceRepository.findById(deliveryTrace.getId()).orElseThrow();
    }

    protected void assertPersistedDeliveryTraceToMatchAllProperties(DeliveryTrace expectedDeliveryTrace) {
        assertDeliveryTraceAllPropertiesEquals(expectedDeliveryTrace, getPersistedDeliveryTrace(expectedDeliveryTrace));
    }

    protected void assertPersistedDeliveryTraceToMatchUpdatableProperties(DeliveryTrace expectedDeliveryTrace) {
        assertDeliveryTraceAllUpdatablePropertiesEquals(expectedDeliveryTrace, getPersistedDeliveryTrace(expectedDeliveryTrace));
    }
}
