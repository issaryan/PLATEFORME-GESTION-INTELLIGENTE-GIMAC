package cm.gimac.des.notification.web.rest;

import static cm.gimac.des.notification.domain.NotificationSubscriptionAsserts.*;
import static cm.gimac.des.notification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.notification.IntegrationTest;
import cm.gimac.des.notification.domain.NotificationSubscription;
import cm.gimac.des.notification.domain.enumeration.RuleType;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import cm.gimac.des.notification.repository.NotificationSubscriptionRepository;
import cm.gimac.des.notification.service.dto.NotificationSubscriptionDTO;
import cm.gimac.des.notification.service.mapper.NotificationSubscriptionMapper;
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
 * Integration tests for the {@link NotificationSubscriptionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class NotificationSubscriptionResourceIT {

    private static final String DEFAULT_NOTIFICATION_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_NOTIFICATION_TYPE = "BBBBBBBBBB";

    private static final RuleType DEFAULT_RULE_TYPE = RuleType.ROLE;
    private static final RuleType UPDATED_RULE_TYPE = RuleType.INSTITUTION;

    private static final String DEFAULT_RULE_VALUE = "AAAAAAAAAA";
    private static final String UPDATED_RULE_VALUE = "BBBBBBBBBB";

    private static final UrgencyLevel DEFAULT_MINIMUM_URGENCY = UrgencyLevel.INFORMATION;
    private static final UrgencyLevel UPDATED_MINIMUM_URGENCY = UrgencyLevel.AVERTISSEMENT;

    private static final Boolean DEFAULT_ON_DUTY_ONLY = false;
    private static final Boolean UPDATED_ON_DUTY_ONLY = true;

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final Integer DEFAULT_RULE_VERSION = 1;
    private static final Integer UPDATED_RULE_VERSION = 2;
    private static final Integer SMALLER_RULE_VERSION = 1 - 1;

    private static final UUID DEFAULT_UPDATED_BY = UUID.randomUUID();
    private static final UUID UPDATED_UPDATED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/notification-subscriptions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private NotificationSubscriptionRepository notificationSubscriptionRepository;

    @Autowired
    private NotificationSubscriptionMapper notificationSubscriptionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restNotificationSubscriptionMockMvc;

    private NotificationSubscription notificationSubscription;

    private NotificationSubscription insertedNotificationSubscription;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static NotificationSubscription createEntity() {
        return new NotificationSubscription()
            .notificationType(DEFAULT_NOTIFICATION_TYPE)
            .ruleType(DEFAULT_RULE_TYPE)
            .ruleValue(DEFAULT_RULE_VALUE)
            .minimumUrgency(DEFAULT_MINIMUM_URGENCY)
            .onDutyOnly(DEFAULT_ON_DUTY_ONLY)
            .active(DEFAULT_ACTIVE)
            .ruleVersion(DEFAULT_RULE_VERSION)
            .updatedBy(DEFAULT_UPDATED_BY)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static NotificationSubscription createUpdatedEntity() {
        return new NotificationSubscription()
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .ruleType(UPDATED_RULE_TYPE)
            .ruleValue(UPDATED_RULE_VALUE)
            .minimumUrgency(UPDATED_MINIMUM_URGENCY)
            .onDutyOnly(UPDATED_ON_DUTY_ONLY)
            .active(UPDATED_ACTIVE)
            .ruleVersion(UPDATED_RULE_VERSION)
            .updatedBy(UPDATED_UPDATED_BY)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        notificationSubscription = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedNotificationSubscription != null) {
            notificationSubscriptionRepository.delete(insertedNotificationSubscription);
            insertedNotificationSubscription = null;
        }
    }

    @Test
    @Transactional
    void createNotificationSubscription() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the NotificationSubscription
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);
        var returnedNotificationSubscriptionDTO = om.readValue(
            restNotificationSubscriptionMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(notificationSubscriptionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            NotificationSubscriptionDTO.class
        );

        // Validate the NotificationSubscription in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedNotificationSubscription = notificationSubscriptionMapper.toEntity(returnedNotificationSubscriptionDTO);
        assertNotificationSubscriptionUpdatableFieldsEquals(
            returnedNotificationSubscription,
            getPersistedNotificationSubscription(returnedNotificationSubscription)
        );

        insertedNotificationSubscription = returnedNotificationSubscription;
    }

    @Test
    @Transactional
    void createNotificationSubscriptionWithExistingId() throws Exception {
        // Create the NotificationSubscription with an existing ID
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restNotificationSubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NotificationSubscription in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNotificationTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        notificationSubscription.setNotificationType(null);

        // Create the NotificationSubscription, which fails.
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        restNotificationSubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRuleTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        notificationSubscription.setRuleType(null);

        // Create the NotificationSubscription, which fails.
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        restNotificationSubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRuleValueIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        notificationSubscription.setRuleValue(null);

        // Create the NotificationSubscription, which fails.
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        restNotificationSubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMinimumUrgencyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        notificationSubscription.setMinimumUrgency(null);

        // Create the NotificationSubscription, which fails.
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        restNotificationSubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOnDutyOnlyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        notificationSubscription.setOnDutyOnly(null);

        // Create the NotificationSubscription, which fails.
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        restNotificationSubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        notificationSubscription.setActive(null);

        // Create the NotificationSubscription, which fails.
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        restNotificationSubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRuleVersionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        notificationSubscription.setRuleVersion(null);

        // Create the NotificationSubscription, which fails.
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        restNotificationSubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUpdatedByIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        notificationSubscription.setUpdatedBy(null);

        // Create the NotificationSubscription, which fails.
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        restNotificationSubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUpdatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        notificationSubscription.setUpdatedAt(null);

        // Create the NotificationSubscription, which fails.
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        restNotificationSubscriptionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptions() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList
        restNotificationSubscriptionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(notificationSubscription.getId().toString())))
            .andExpect(jsonPath("$.[*].notificationType").value(hasItem(DEFAULT_NOTIFICATION_TYPE)))
            .andExpect(jsonPath("$.[*].ruleType").value(hasItem(DEFAULT_RULE_TYPE.toString())))
            .andExpect(jsonPath("$.[*].ruleValue").value(hasItem(DEFAULT_RULE_VALUE)))
            .andExpect(jsonPath("$.[*].minimumUrgency").value(hasItem(DEFAULT_MINIMUM_URGENCY.toString())))
            .andExpect(jsonPath("$.[*].onDutyOnly").value(hasItem(DEFAULT_ON_DUTY_ONLY)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
            .andExpect(jsonPath("$.[*].ruleVersion").value(hasItem(DEFAULT_RULE_VERSION)))
            .andExpect(jsonPath("$.[*].updatedBy").value(hasItem(DEFAULT_UPDATED_BY.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getNotificationSubscription() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get the notificationSubscription
        restNotificationSubscriptionMockMvc
            .perform(get(ENTITY_API_URL_ID, notificationSubscription.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(notificationSubscription.getId().toString()))
            .andExpect(jsonPath("$.notificationType").value(DEFAULT_NOTIFICATION_TYPE))
            .andExpect(jsonPath("$.ruleType").value(DEFAULT_RULE_TYPE.toString()))
            .andExpect(jsonPath("$.ruleValue").value(DEFAULT_RULE_VALUE))
            .andExpect(jsonPath("$.minimumUrgency").value(DEFAULT_MINIMUM_URGENCY.toString()))
            .andExpect(jsonPath("$.onDutyOnly").value(DEFAULT_ON_DUTY_ONLY))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE))
            .andExpect(jsonPath("$.ruleVersion").value(DEFAULT_RULE_VERSION))
            .andExpect(jsonPath("$.updatedBy").value(DEFAULT_UPDATED_BY.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNotificationSubscriptionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        UUID id = notificationSubscription.getId();

        defaultNotificationSubscriptionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByNotificationTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where notificationType equals to
        defaultNotificationSubscriptionFiltering(
            "notificationType.equals=" + DEFAULT_NOTIFICATION_TYPE,
            "notificationType.equals=" + UPDATED_NOTIFICATION_TYPE
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByNotificationTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where notificationType in
        defaultNotificationSubscriptionFiltering(
            "notificationType.in=" + DEFAULT_NOTIFICATION_TYPE + "," + UPDATED_NOTIFICATION_TYPE,
            "notificationType.in=" + UPDATED_NOTIFICATION_TYPE
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByNotificationTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where notificationType is not null
        defaultNotificationSubscriptionFiltering("notificationType.specified=true", "notificationType.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByNotificationTypeContainsSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where notificationType contains
        defaultNotificationSubscriptionFiltering(
            "notificationType.contains=" + DEFAULT_NOTIFICATION_TYPE,
            "notificationType.contains=" + UPDATED_NOTIFICATION_TYPE
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByNotificationTypeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where notificationType does not contain
        defaultNotificationSubscriptionFiltering(
            "notificationType.doesNotContain=" + UPDATED_NOTIFICATION_TYPE,
            "notificationType.doesNotContain=" + DEFAULT_NOTIFICATION_TYPE
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleType equals to
        defaultNotificationSubscriptionFiltering("ruleType.equals=" + DEFAULT_RULE_TYPE, "ruleType.equals=" + UPDATED_RULE_TYPE);
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleType in
        defaultNotificationSubscriptionFiltering(
            "ruleType.in=" + DEFAULT_RULE_TYPE + "," + UPDATED_RULE_TYPE,
            "ruleType.in=" + UPDATED_RULE_TYPE
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleType is not null
        defaultNotificationSubscriptionFiltering("ruleType.specified=true", "ruleType.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleValueIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleValue equals to
        defaultNotificationSubscriptionFiltering("ruleValue.equals=" + DEFAULT_RULE_VALUE, "ruleValue.equals=" + UPDATED_RULE_VALUE);
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleValueIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleValue in
        defaultNotificationSubscriptionFiltering(
            "ruleValue.in=" + DEFAULT_RULE_VALUE + "," + UPDATED_RULE_VALUE,
            "ruleValue.in=" + UPDATED_RULE_VALUE
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleValueIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleValue is not null
        defaultNotificationSubscriptionFiltering("ruleValue.specified=true", "ruleValue.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleValueContainsSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleValue contains
        defaultNotificationSubscriptionFiltering("ruleValue.contains=" + DEFAULT_RULE_VALUE, "ruleValue.contains=" + UPDATED_RULE_VALUE);
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleValueNotContainsSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleValue does not contain
        defaultNotificationSubscriptionFiltering(
            "ruleValue.doesNotContain=" + UPDATED_RULE_VALUE,
            "ruleValue.doesNotContain=" + DEFAULT_RULE_VALUE
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByMinimumUrgencyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where minimumUrgency equals to
        defaultNotificationSubscriptionFiltering(
            "minimumUrgency.equals=" + DEFAULT_MINIMUM_URGENCY,
            "minimumUrgency.equals=" + UPDATED_MINIMUM_URGENCY
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByMinimumUrgencyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where minimumUrgency in
        defaultNotificationSubscriptionFiltering(
            "minimumUrgency.in=" + DEFAULT_MINIMUM_URGENCY + "," + UPDATED_MINIMUM_URGENCY,
            "minimumUrgency.in=" + UPDATED_MINIMUM_URGENCY
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByMinimumUrgencyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where minimumUrgency is not null
        defaultNotificationSubscriptionFiltering("minimumUrgency.specified=true", "minimumUrgency.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByOnDutyOnlyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where onDutyOnly equals to
        defaultNotificationSubscriptionFiltering("onDutyOnly.equals=" + DEFAULT_ON_DUTY_ONLY, "onDutyOnly.equals=" + UPDATED_ON_DUTY_ONLY);
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByOnDutyOnlyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where onDutyOnly in
        defaultNotificationSubscriptionFiltering(
            "onDutyOnly.in=" + DEFAULT_ON_DUTY_ONLY + "," + UPDATED_ON_DUTY_ONLY,
            "onDutyOnly.in=" + UPDATED_ON_DUTY_ONLY
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByOnDutyOnlyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where onDutyOnly is not null
        defaultNotificationSubscriptionFiltering("onDutyOnly.specified=true", "onDutyOnly.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByActiveIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where active equals to
        defaultNotificationSubscriptionFiltering("active.equals=" + DEFAULT_ACTIVE, "active.equals=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByActiveIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where active in
        defaultNotificationSubscriptionFiltering("active.in=" + DEFAULT_ACTIVE + "," + UPDATED_ACTIVE, "active.in=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByActiveIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where active is not null
        defaultNotificationSubscriptionFiltering("active.specified=true", "active.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleVersionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleVersion equals to
        defaultNotificationSubscriptionFiltering(
            "ruleVersion.equals=" + DEFAULT_RULE_VERSION,
            "ruleVersion.equals=" + UPDATED_RULE_VERSION
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleVersionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleVersion in
        defaultNotificationSubscriptionFiltering(
            "ruleVersion.in=" + DEFAULT_RULE_VERSION + "," + UPDATED_RULE_VERSION,
            "ruleVersion.in=" + UPDATED_RULE_VERSION
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleVersionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleVersion is not null
        defaultNotificationSubscriptionFiltering("ruleVersion.specified=true", "ruleVersion.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleVersionIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleVersion is greater than or equal to
        defaultNotificationSubscriptionFiltering(
            "ruleVersion.greaterThanOrEqual=" + DEFAULT_RULE_VERSION,
            "ruleVersion.greaterThanOrEqual=" + UPDATED_RULE_VERSION
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleVersionIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleVersion is less than or equal to
        defaultNotificationSubscriptionFiltering(
            "ruleVersion.lessThanOrEqual=" + DEFAULT_RULE_VERSION,
            "ruleVersion.lessThanOrEqual=" + SMALLER_RULE_VERSION
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleVersionIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleVersion is less than
        defaultNotificationSubscriptionFiltering(
            "ruleVersion.lessThan=" + UPDATED_RULE_VERSION,
            "ruleVersion.lessThan=" + DEFAULT_RULE_VERSION
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByRuleVersionIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where ruleVersion is greater than
        defaultNotificationSubscriptionFiltering(
            "ruleVersion.greaterThan=" + SMALLER_RULE_VERSION,
            "ruleVersion.greaterThan=" + DEFAULT_RULE_VERSION
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByUpdatedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where updatedBy equals to
        defaultNotificationSubscriptionFiltering("updatedBy.equals=" + DEFAULT_UPDATED_BY, "updatedBy.equals=" + UPDATED_UPDATED_BY);
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByUpdatedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where updatedBy in
        defaultNotificationSubscriptionFiltering(
            "updatedBy.in=" + DEFAULT_UPDATED_BY + "," + UPDATED_UPDATED_BY,
            "updatedBy.in=" + UPDATED_UPDATED_BY
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByUpdatedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where updatedBy is not null
        defaultNotificationSubscriptionFiltering("updatedBy.specified=true", "updatedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where updatedAt equals to
        defaultNotificationSubscriptionFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where updatedAt in
        defaultNotificationSubscriptionFiltering(
            "updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT,
            "updatedAt.in=" + UPDATED_UPDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllNotificationSubscriptionsByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        // Get all the notificationSubscriptionList where updatedAt is not null
        defaultNotificationSubscriptionFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    private void defaultNotificationSubscriptionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultNotificationSubscriptionShouldBeFound(shouldBeFound);
        defaultNotificationSubscriptionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultNotificationSubscriptionShouldBeFound(String filter) throws Exception {
        restNotificationSubscriptionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(notificationSubscription.getId().toString())))
            .andExpect(jsonPath("$.[*].notificationType").value(hasItem(DEFAULT_NOTIFICATION_TYPE)))
            .andExpect(jsonPath("$.[*].ruleType").value(hasItem(DEFAULT_RULE_TYPE.toString())))
            .andExpect(jsonPath("$.[*].ruleValue").value(hasItem(DEFAULT_RULE_VALUE)))
            .andExpect(jsonPath("$.[*].minimumUrgency").value(hasItem(DEFAULT_MINIMUM_URGENCY.toString())))
            .andExpect(jsonPath("$.[*].onDutyOnly").value(hasItem(DEFAULT_ON_DUTY_ONLY)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
            .andExpect(jsonPath("$.[*].ruleVersion").value(hasItem(DEFAULT_RULE_VERSION)))
            .andExpect(jsonPath("$.[*].updatedBy").value(hasItem(DEFAULT_UPDATED_BY.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restNotificationSubscriptionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultNotificationSubscriptionShouldNotBeFound(String filter) throws Exception {
        restNotificationSubscriptionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restNotificationSubscriptionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingNotificationSubscription() throws Exception {
        // Get the notificationSubscription
        restNotificationSubscriptionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingNotificationSubscription() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the notificationSubscription
        NotificationSubscription updatedNotificationSubscription = notificationSubscriptionRepository
            .findById(notificationSubscription.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedNotificationSubscription are not directly saved in db
        em.detach(updatedNotificationSubscription);
        updatedNotificationSubscription
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .ruleType(UPDATED_RULE_TYPE)
            .ruleValue(UPDATED_RULE_VALUE)
            .minimumUrgency(UPDATED_MINIMUM_URGENCY)
            .onDutyOnly(UPDATED_ON_DUTY_ONLY)
            .active(UPDATED_ACTIVE)
            .ruleVersion(UPDATED_RULE_VERSION)
            .updatedBy(UPDATED_UPDATED_BY)
            .updatedAt(UPDATED_UPDATED_AT);
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(updatedNotificationSubscription);

        restNotificationSubscriptionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, notificationSubscriptionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isOk());

        // Validate the NotificationSubscription in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedNotificationSubscriptionToMatchAllProperties(updatedNotificationSubscription);
    }

    @Test
    @Transactional
    void putNonExistingNotificationSubscription() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificationSubscription.setId(UUID.randomUUID());

        // Create the NotificationSubscription
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restNotificationSubscriptionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, notificationSubscriptionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NotificationSubscription in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchNotificationSubscription() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificationSubscription.setId(UUID.randomUUID());

        // Create the NotificationSubscription
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNotificationSubscriptionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NotificationSubscription in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamNotificationSubscription() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificationSubscription.setId(UUID.randomUUID());

        // Create the NotificationSubscription
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNotificationSubscriptionMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the NotificationSubscription in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateNotificationSubscriptionWithPatch() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the notificationSubscription using partial update
        NotificationSubscription partialUpdatedNotificationSubscription = new NotificationSubscription();
        partialUpdatedNotificationSubscription.setId(notificationSubscription.getId());

        partialUpdatedNotificationSubscription
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .minimumUrgency(UPDATED_MINIMUM_URGENCY)
            .onDutyOnly(UPDATED_ON_DUTY_ONLY)
            .active(UPDATED_ACTIVE)
            .ruleVersion(UPDATED_RULE_VERSION)
            .updatedBy(UPDATED_UPDATED_BY)
            .updatedAt(UPDATED_UPDATED_AT);

        restNotificationSubscriptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedNotificationSubscription.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedNotificationSubscription))
            )
            .andExpect(status().isOk());

        // Validate the NotificationSubscription in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertNotificationSubscriptionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedNotificationSubscription, notificationSubscription),
            getPersistedNotificationSubscription(notificationSubscription)
        );
    }

    @Test
    @Transactional
    void fullUpdateNotificationSubscriptionWithPatch() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the notificationSubscription using partial update
        NotificationSubscription partialUpdatedNotificationSubscription = new NotificationSubscription();
        partialUpdatedNotificationSubscription.setId(notificationSubscription.getId());

        partialUpdatedNotificationSubscription
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .ruleType(UPDATED_RULE_TYPE)
            .ruleValue(UPDATED_RULE_VALUE)
            .minimumUrgency(UPDATED_MINIMUM_URGENCY)
            .onDutyOnly(UPDATED_ON_DUTY_ONLY)
            .active(UPDATED_ACTIVE)
            .ruleVersion(UPDATED_RULE_VERSION)
            .updatedBy(UPDATED_UPDATED_BY)
            .updatedAt(UPDATED_UPDATED_AT);

        restNotificationSubscriptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedNotificationSubscription.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedNotificationSubscription))
            )
            .andExpect(status().isOk());

        // Validate the NotificationSubscription in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertNotificationSubscriptionUpdatableFieldsEquals(
            partialUpdatedNotificationSubscription,
            getPersistedNotificationSubscription(partialUpdatedNotificationSubscription)
        );
    }

    @Test
    @Transactional
    void patchNonExistingNotificationSubscription() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificationSubscription.setId(UUID.randomUUID());

        // Create the NotificationSubscription
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restNotificationSubscriptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, notificationSubscriptionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NotificationSubscription in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchNotificationSubscription() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificationSubscription.setId(UUID.randomUUID());

        // Create the NotificationSubscription
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNotificationSubscriptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NotificationSubscription in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamNotificationSubscription() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificationSubscription.setId(UUID.randomUUID());

        // Create the NotificationSubscription
        NotificationSubscriptionDTO notificationSubscriptionDTO = notificationSubscriptionMapper.toDto(notificationSubscription);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNotificationSubscriptionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(notificationSubscriptionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the NotificationSubscription in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteNotificationSubscription() throws Exception {
        // Initialize the database
        insertedNotificationSubscription = notificationSubscriptionRepository.saveAndFlush(notificationSubscription);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the notificationSubscription
        restNotificationSubscriptionMockMvc
            .perform(delete(ENTITY_API_URL_ID, notificationSubscription.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return notificationSubscriptionRepository.count();
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

    protected NotificationSubscription getPersistedNotificationSubscription(NotificationSubscription notificationSubscription) {
        return notificationSubscriptionRepository.findById(notificationSubscription.getId()).orElseThrow();
    }

    protected void assertPersistedNotificationSubscriptionToMatchAllProperties(NotificationSubscription expectedNotificationSubscription) {
        assertNotificationSubscriptionAllPropertiesEquals(
            expectedNotificationSubscription,
            getPersistedNotificationSubscription(expectedNotificationSubscription)
        );
    }

    protected void assertPersistedNotificationSubscriptionToMatchUpdatableProperties(
        NotificationSubscription expectedNotificationSubscription
    ) {
        assertNotificationSubscriptionAllUpdatablePropertiesEquals(
            expectedNotificationSubscription,
            getPersistedNotificationSubscription(expectedNotificationSubscription)
        );
    }
}
