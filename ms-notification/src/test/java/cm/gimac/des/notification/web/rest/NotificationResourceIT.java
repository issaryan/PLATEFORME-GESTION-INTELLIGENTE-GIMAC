package cm.gimac.des.notification.web.rest;

import static cm.gimac.des.notification.domain.NotificationAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.notification.IntegrationTest;
import cm.gimac.des.notification.domain.ConsolidationWindow;
import cm.gimac.des.notification.domain.Notification;
import cm.gimac.des.notification.domain.enumeration.NotificationState;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import cm.gimac.des.notification.repository.NotificationRepository;
import cm.gimac.des.notification.service.NotificationService;
import cm.gimac.des.notification.service.mapper.NotificationMapper;
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
 * Integration tests for the {@link NotificationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class NotificationResourceIT {

    private static final String DEFAULT_NOTIFICATION_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_NOTIFICATION_TYPE = "BBBBBBBBBB";

    private static final UrgencyLevel DEFAULT_URGENCY = UrgencyLevel.INFORMATION;
    private static final UrgencyLevel UPDATED_URGENCY = UrgencyLevel.AVERTISSEMENT;

    private static final String DEFAULT_SUBJECT = "AAAAAAAAAA";
    private static final String UPDATED_SUBJECT = "BBBBBBBBBB";

    private static final String DEFAULT_OBJECT_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_OBJECT_TYPE = "BBBBBBBBBB";

    private static final String DEFAULT_OBJECT_ID = "AAAAAAAAAA";
    private static final String UPDATED_OBJECT_ID = "BBBBBBBBBB";

    private static final String DEFAULT_VARIABLES = "AAAAAAAAAA";
    private static final String UPDATED_VARIABLES = "BBBBBBBBBB";

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final UUID DEFAULT_SOURCE_EVENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_SOURCE_EVENT_ID = UUID.randomUUID();

    private static final String DEFAULT_SOURCE_SERVICE = "AAAAAAAAAA";
    private static final String UPDATED_SOURCE_SERVICE = "BBBBBBBBBB";

    private static final Boolean DEFAULT_URGENT_PORT = false;
    private static final Boolean UPDATED_URGENT_PORT = true;

    private static final UUID DEFAULT_CORRELATION_ID = UUID.randomUUID();
    private static final UUID UPDATED_CORRELATION_ID = UUID.randomUUID();

    private static final String DEFAULT_GROUP_KEY = "AAAAAAAAAA";
    private static final String UPDATED_GROUP_KEY = "BBBBBBBBBB";

    private static final Instant DEFAULT_REQUESTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REQUESTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_ACK_DEADLINE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ACK_DEADLINE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_ESCALATION_LEVEL = 0;
    private static final Integer UPDATED_ESCALATION_LEVEL = 1;
    private static final Integer SMALLER_ESCALATION_LEVEL = 0 - 1;

    private static final NotificationState DEFAULT_STATE = NotificationState.RECUE;
    private static final NotificationState UPDATED_STATE = NotificationState.CONSOLIDEE;

    private static final UUID DEFAULT_ACKNOWLEDGED_BY = UUID.randomUUID();
    private static final UUID UPDATED_ACKNOWLEDGED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_ACKNOWLEDGED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ACKNOWLEDGED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_SUMMARY = false;
    private static final Boolean UPDATED_SUMMARY = true;

    private static final Integer DEFAULT_OCCURRENCE_COUNT = 0;
    private static final Integer UPDATED_OCCURRENCE_COUNT = 1;
    private static final Integer SMALLER_OCCURRENCE_COUNT = 0 - 1;

    private static final String DEFAULT_SIGNATURE = "AAAAAAAAAA";
    private static final String UPDATED_SIGNATURE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/notifications";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationRepository notificationRepositoryMock;

    @Autowired
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationService notificationServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restNotificationMockMvc;

    private Notification notification;

    private Notification insertedNotification;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Notification createEntity() {
        return new Notification()
            .notificationType(DEFAULT_NOTIFICATION_TYPE)
            .urgency(DEFAULT_URGENCY)
            .subject(DEFAULT_SUBJECT)
            .objectType(DEFAULT_OBJECT_TYPE)
            .objectId(DEFAULT_OBJECT_ID)
            .variables(DEFAULT_VARIABLES)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .sourceEventId(DEFAULT_SOURCE_EVENT_ID)
            .sourceService(DEFAULT_SOURCE_SERVICE)
            .urgentPort(DEFAULT_URGENT_PORT)
            .correlationId(DEFAULT_CORRELATION_ID)
            .groupKey(DEFAULT_GROUP_KEY)
            .requestedAt(DEFAULT_REQUESTED_AT)
            .ackDeadline(DEFAULT_ACK_DEADLINE)
            .escalationLevel(DEFAULT_ESCALATION_LEVEL)
            .state(DEFAULT_STATE)
            .acknowledgedBy(DEFAULT_ACKNOWLEDGED_BY)
            .acknowledgedAt(DEFAULT_ACKNOWLEDGED_AT)
            .summary(DEFAULT_SUMMARY)
            .occurrenceCount(DEFAULT_OCCURRENCE_COUNT)
            .signature(DEFAULT_SIGNATURE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Notification createUpdatedEntity() {
        return new Notification()
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .urgency(UPDATED_URGENCY)
            .subject(UPDATED_SUBJECT)
            .objectType(UPDATED_OBJECT_TYPE)
            .objectId(UPDATED_OBJECT_ID)
            .variables(UPDATED_VARIABLES)
            .institutionId(UPDATED_INSTITUTION_ID)
            .sourceEventId(UPDATED_SOURCE_EVENT_ID)
            .sourceService(UPDATED_SOURCE_SERVICE)
            .urgentPort(UPDATED_URGENT_PORT)
            .correlationId(UPDATED_CORRELATION_ID)
            .groupKey(UPDATED_GROUP_KEY)
            .requestedAt(UPDATED_REQUESTED_AT)
            .ackDeadline(UPDATED_ACK_DEADLINE)
            .escalationLevel(UPDATED_ESCALATION_LEVEL)
            .state(UPDATED_STATE)
            .acknowledgedBy(UPDATED_ACKNOWLEDGED_BY)
            .acknowledgedAt(UPDATED_ACKNOWLEDGED_AT)
            .summary(UPDATED_SUMMARY)
            .occurrenceCount(UPDATED_OCCURRENCE_COUNT)
            .signature(UPDATED_SIGNATURE);
    }

    @BeforeEach
    void initTest() {
        notification = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedNotification != null) {
            notificationRepository.delete(insertedNotification);
            insertedNotification = null;
        }
    }

    @Test
    @Transactional
    void getAllNotifications() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList
        restNotificationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(notification.getId().toString())))
            .andExpect(jsonPath("$.[*].notificationType").value(hasItem(DEFAULT_NOTIFICATION_TYPE)))
            .andExpect(jsonPath("$.[*].urgency").value(hasItem(DEFAULT_URGENCY.toString())))
            .andExpect(jsonPath("$.[*].subject").value(hasItem(DEFAULT_SUBJECT)))
            .andExpect(jsonPath("$.[*].objectType").value(hasItem(DEFAULT_OBJECT_TYPE)))
            .andExpect(jsonPath("$.[*].objectId").value(hasItem(DEFAULT_OBJECT_ID)))
            .andExpect(jsonPath("$.[*].variables").value(hasItem(DEFAULT_VARIABLES)))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].sourceEventId").value(hasItem(DEFAULT_SOURCE_EVENT_ID.toString())))
            .andExpect(jsonPath("$.[*].sourceService").value(hasItem(DEFAULT_SOURCE_SERVICE)))
            .andExpect(jsonPath("$.[*].urgentPort").value(hasItem(DEFAULT_URGENT_PORT)))
            .andExpect(jsonPath("$.[*].correlationId").value(hasItem(DEFAULT_CORRELATION_ID.toString())))
            .andExpect(jsonPath("$.[*].groupKey").value(hasItem(DEFAULT_GROUP_KEY)))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].ackDeadline").value(hasItem(DEFAULT_ACK_DEADLINE.toString())))
            .andExpect(jsonPath("$.[*].escalationLevel").value(hasItem(DEFAULT_ESCALATION_LEVEL)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedBy").value(hasItem(DEFAULT_ACKNOWLEDGED_BY.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())))
            .andExpect(jsonPath("$.[*].summary").value(hasItem(DEFAULT_SUMMARY)))
            .andExpect(jsonPath("$.[*].occurrenceCount").value(hasItem(DEFAULT_OCCURRENCE_COUNT)))
            .andExpect(jsonPath("$.[*].signature").value(hasItem(DEFAULT_SIGNATURE)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllNotificationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(notificationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restNotificationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(notificationServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllNotificationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(notificationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restNotificationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(notificationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getNotification() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get the notification
        restNotificationMockMvc
            .perform(get(ENTITY_API_URL_ID, notification.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(notification.getId().toString()))
            .andExpect(jsonPath("$.notificationType").value(DEFAULT_NOTIFICATION_TYPE))
            .andExpect(jsonPath("$.urgency").value(DEFAULT_URGENCY.toString()))
            .andExpect(jsonPath("$.subject").value(DEFAULT_SUBJECT))
            .andExpect(jsonPath("$.objectType").value(DEFAULT_OBJECT_TYPE))
            .andExpect(jsonPath("$.objectId").value(DEFAULT_OBJECT_ID))
            .andExpect(jsonPath("$.variables").value(DEFAULT_VARIABLES))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.sourceEventId").value(DEFAULT_SOURCE_EVENT_ID.toString()))
            .andExpect(jsonPath("$.sourceService").value(DEFAULT_SOURCE_SERVICE))
            .andExpect(jsonPath("$.urgentPort").value(DEFAULT_URGENT_PORT))
            .andExpect(jsonPath("$.correlationId").value(DEFAULT_CORRELATION_ID.toString()))
            .andExpect(jsonPath("$.groupKey").value(DEFAULT_GROUP_KEY))
            .andExpect(jsonPath("$.requestedAt").value(DEFAULT_REQUESTED_AT.toString()))
            .andExpect(jsonPath("$.ackDeadline").value(DEFAULT_ACK_DEADLINE.toString()))
            .andExpect(jsonPath("$.escalationLevel").value(DEFAULT_ESCALATION_LEVEL))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.acknowledgedBy").value(DEFAULT_ACKNOWLEDGED_BY.toString()))
            .andExpect(jsonPath("$.acknowledgedAt").value(DEFAULT_ACKNOWLEDGED_AT.toString()))
            .andExpect(jsonPath("$.summary").value(DEFAULT_SUMMARY))
            .andExpect(jsonPath("$.occurrenceCount").value(DEFAULT_OCCURRENCE_COUNT))
            .andExpect(jsonPath("$.signature").value(DEFAULT_SIGNATURE));
    }

    @Test
    @Transactional
    void getNotificationsByIdFiltering() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        UUID id = notification.getId();

        defaultNotificationFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllNotificationsByNotificationTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where notificationType equals to
        defaultNotificationFiltering(
            "notificationType.equals=" + DEFAULT_NOTIFICATION_TYPE,
            "notificationType.equals=" + UPDATED_NOTIFICATION_TYPE
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByNotificationTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where notificationType in
        defaultNotificationFiltering(
            "notificationType.in=" + DEFAULT_NOTIFICATION_TYPE + "," + UPDATED_NOTIFICATION_TYPE,
            "notificationType.in=" + UPDATED_NOTIFICATION_TYPE
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByNotificationTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where notificationType is not null
        defaultNotificationFiltering("notificationType.specified=true", "notificationType.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByNotificationTypeContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where notificationType contains
        defaultNotificationFiltering(
            "notificationType.contains=" + DEFAULT_NOTIFICATION_TYPE,
            "notificationType.contains=" + UPDATED_NOTIFICATION_TYPE
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByNotificationTypeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where notificationType does not contain
        defaultNotificationFiltering(
            "notificationType.doesNotContain=" + UPDATED_NOTIFICATION_TYPE,
            "notificationType.doesNotContain=" + DEFAULT_NOTIFICATION_TYPE
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByUrgencyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where urgency equals to
        defaultNotificationFiltering("urgency.equals=" + DEFAULT_URGENCY, "urgency.equals=" + UPDATED_URGENCY);
    }

    @Test
    @Transactional
    void getAllNotificationsByUrgencyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where urgency in
        defaultNotificationFiltering("urgency.in=" + DEFAULT_URGENCY + "," + UPDATED_URGENCY, "urgency.in=" + UPDATED_URGENCY);
    }

    @Test
    @Transactional
    void getAllNotificationsByUrgencyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where urgency is not null
        defaultNotificationFiltering("urgency.specified=true", "urgency.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsBySubjectIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where subject equals to
        defaultNotificationFiltering("subject.equals=" + DEFAULT_SUBJECT, "subject.equals=" + UPDATED_SUBJECT);
    }

    @Test
    @Transactional
    void getAllNotificationsBySubjectIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where subject in
        defaultNotificationFiltering("subject.in=" + DEFAULT_SUBJECT + "," + UPDATED_SUBJECT, "subject.in=" + UPDATED_SUBJECT);
    }

    @Test
    @Transactional
    void getAllNotificationsBySubjectIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where subject is not null
        defaultNotificationFiltering("subject.specified=true", "subject.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsBySubjectContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where subject contains
        defaultNotificationFiltering("subject.contains=" + DEFAULT_SUBJECT, "subject.contains=" + UPDATED_SUBJECT);
    }

    @Test
    @Transactional
    void getAllNotificationsBySubjectNotContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where subject does not contain
        defaultNotificationFiltering("subject.doesNotContain=" + UPDATED_SUBJECT, "subject.doesNotContain=" + DEFAULT_SUBJECT);
    }

    @Test
    @Transactional
    void getAllNotificationsByObjectTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where objectType equals to
        defaultNotificationFiltering("objectType.equals=" + DEFAULT_OBJECT_TYPE, "objectType.equals=" + UPDATED_OBJECT_TYPE);
    }

    @Test
    @Transactional
    void getAllNotificationsByObjectTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where objectType in
        defaultNotificationFiltering(
            "objectType.in=" + DEFAULT_OBJECT_TYPE + "," + UPDATED_OBJECT_TYPE,
            "objectType.in=" + UPDATED_OBJECT_TYPE
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByObjectTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where objectType is not null
        defaultNotificationFiltering("objectType.specified=true", "objectType.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByObjectTypeContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where objectType contains
        defaultNotificationFiltering("objectType.contains=" + DEFAULT_OBJECT_TYPE, "objectType.contains=" + UPDATED_OBJECT_TYPE);
    }

    @Test
    @Transactional
    void getAllNotificationsByObjectTypeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where objectType does not contain
        defaultNotificationFiltering(
            "objectType.doesNotContain=" + UPDATED_OBJECT_TYPE,
            "objectType.doesNotContain=" + DEFAULT_OBJECT_TYPE
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByObjectIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where objectId equals to
        defaultNotificationFiltering("objectId.equals=" + DEFAULT_OBJECT_ID, "objectId.equals=" + UPDATED_OBJECT_ID);
    }

    @Test
    @Transactional
    void getAllNotificationsByObjectIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where objectId in
        defaultNotificationFiltering("objectId.in=" + DEFAULT_OBJECT_ID + "," + UPDATED_OBJECT_ID, "objectId.in=" + UPDATED_OBJECT_ID);
    }

    @Test
    @Transactional
    void getAllNotificationsByObjectIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where objectId is not null
        defaultNotificationFiltering("objectId.specified=true", "objectId.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByObjectIdContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where objectId contains
        defaultNotificationFiltering("objectId.contains=" + DEFAULT_OBJECT_ID, "objectId.contains=" + UPDATED_OBJECT_ID);
    }

    @Test
    @Transactional
    void getAllNotificationsByObjectIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where objectId does not contain
        defaultNotificationFiltering("objectId.doesNotContain=" + UPDATED_OBJECT_ID, "objectId.doesNotContain=" + DEFAULT_OBJECT_ID);
    }

    @Test
    @Transactional
    void getAllNotificationsByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where institutionId equals to
        defaultNotificationFiltering("institutionId.equals=" + DEFAULT_INSTITUTION_ID, "institutionId.equals=" + UPDATED_INSTITUTION_ID);
    }

    @Test
    @Transactional
    void getAllNotificationsByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where institutionId in
        defaultNotificationFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where institutionId is not null
        defaultNotificationFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsBySourceEventIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where sourceEventId equals to
        defaultNotificationFiltering("sourceEventId.equals=" + DEFAULT_SOURCE_EVENT_ID, "sourceEventId.equals=" + UPDATED_SOURCE_EVENT_ID);
    }

    @Test
    @Transactional
    void getAllNotificationsBySourceEventIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where sourceEventId in
        defaultNotificationFiltering(
            "sourceEventId.in=" + DEFAULT_SOURCE_EVENT_ID + "," + UPDATED_SOURCE_EVENT_ID,
            "sourceEventId.in=" + UPDATED_SOURCE_EVENT_ID
        );
    }

    @Test
    @Transactional
    void getAllNotificationsBySourceEventIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where sourceEventId is not null
        defaultNotificationFiltering("sourceEventId.specified=true", "sourceEventId.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsBySourceServiceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where sourceService equals to
        defaultNotificationFiltering("sourceService.equals=" + DEFAULT_SOURCE_SERVICE, "sourceService.equals=" + UPDATED_SOURCE_SERVICE);
    }

    @Test
    @Transactional
    void getAllNotificationsBySourceServiceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where sourceService in
        defaultNotificationFiltering(
            "sourceService.in=" + DEFAULT_SOURCE_SERVICE + "," + UPDATED_SOURCE_SERVICE,
            "sourceService.in=" + UPDATED_SOURCE_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllNotificationsBySourceServiceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where sourceService is not null
        defaultNotificationFiltering("sourceService.specified=true", "sourceService.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsBySourceServiceContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where sourceService contains
        defaultNotificationFiltering(
            "sourceService.contains=" + DEFAULT_SOURCE_SERVICE,
            "sourceService.contains=" + UPDATED_SOURCE_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllNotificationsBySourceServiceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where sourceService does not contain
        defaultNotificationFiltering(
            "sourceService.doesNotContain=" + UPDATED_SOURCE_SERVICE,
            "sourceService.doesNotContain=" + DEFAULT_SOURCE_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByUrgentPortIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where urgentPort equals to
        defaultNotificationFiltering("urgentPort.equals=" + DEFAULT_URGENT_PORT, "urgentPort.equals=" + UPDATED_URGENT_PORT);
    }

    @Test
    @Transactional
    void getAllNotificationsByUrgentPortIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where urgentPort in
        defaultNotificationFiltering(
            "urgentPort.in=" + DEFAULT_URGENT_PORT + "," + UPDATED_URGENT_PORT,
            "urgentPort.in=" + UPDATED_URGENT_PORT
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByUrgentPortIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where urgentPort is not null
        defaultNotificationFiltering("urgentPort.specified=true", "urgentPort.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByCorrelationIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where correlationId equals to
        defaultNotificationFiltering("correlationId.equals=" + DEFAULT_CORRELATION_ID, "correlationId.equals=" + UPDATED_CORRELATION_ID);
    }

    @Test
    @Transactional
    void getAllNotificationsByCorrelationIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where correlationId in
        defaultNotificationFiltering(
            "correlationId.in=" + DEFAULT_CORRELATION_ID + "," + UPDATED_CORRELATION_ID,
            "correlationId.in=" + UPDATED_CORRELATION_ID
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByCorrelationIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where correlationId is not null
        defaultNotificationFiltering("correlationId.specified=true", "correlationId.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByGroupKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where groupKey equals to
        defaultNotificationFiltering("groupKey.equals=" + DEFAULT_GROUP_KEY, "groupKey.equals=" + UPDATED_GROUP_KEY);
    }

    @Test
    @Transactional
    void getAllNotificationsByGroupKeyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where groupKey in
        defaultNotificationFiltering("groupKey.in=" + DEFAULT_GROUP_KEY + "," + UPDATED_GROUP_KEY, "groupKey.in=" + UPDATED_GROUP_KEY);
    }

    @Test
    @Transactional
    void getAllNotificationsByGroupKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where groupKey is not null
        defaultNotificationFiltering("groupKey.specified=true", "groupKey.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByGroupKeyContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where groupKey contains
        defaultNotificationFiltering("groupKey.contains=" + DEFAULT_GROUP_KEY, "groupKey.contains=" + UPDATED_GROUP_KEY);
    }

    @Test
    @Transactional
    void getAllNotificationsByGroupKeyNotContainsSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where groupKey does not contain
        defaultNotificationFiltering("groupKey.doesNotContain=" + UPDATED_GROUP_KEY, "groupKey.doesNotContain=" + DEFAULT_GROUP_KEY);
    }

    @Test
    @Transactional
    void getAllNotificationsByRequestedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where requestedAt equals to
        defaultNotificationFiltering("requestedAt.equals=" + DEFAULT_REQUESTED_AT, "requestedAt.equals=" + UPDATED_REQUESTED_AT);
    }

    @Test
    @Transactional
    void getAllNotificationsByRequestedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where requestedAt in
        defaultNotificationFiltering(
            "requestedAt.in=" + DEFAULT_REQUESTED_AT + "," + UPDATED_REQUESTED_AT,
            "requestedAt.in=" + UPDATED_REQUESTED_AT
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByRequestedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where requestedAt is not null
        defaultNotificationFiltering("requestedAt.specified=true", "requestedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByAckDeadlineIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where ackDeadline equals to
        defaultNotificationFiltering("ackDeadline.equals=" + DEFAULT_ACK_DEADLINE, "ackDeadline.equals=" + UPDATED_ACK_DEADLINE);
    }

    @Test
    @Transactional
    void getAllNotificationsByAckDeadlineIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where ackDeadline in
        defaultNotificationFiltering(
            "ackDeadline.in=" + DEFAULT_ACK_DEADLINE + "," + UPDATED_ACK_DEADLINE,
            "ackDeadline.in=" + UPDATED_ACK_DEADLINE
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByAckDeadlineIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where ackDeadline is not null
        defaultNotificationFiltering("ackDeadline.specified=true", "ackDeadline.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByEscalationLevelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where escalationLevel equals to
        defaultNotificationFiltering(
            "escalationLevel.equals=" + DEFAULT_ESCALATION_LEVEL,
            "escalationLevel.equals=" + UPDATED_ESCALATION_LEVEL
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByEscalationLevelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where escalationLevel in
        defaultNotificationFiltering(
            "escalationLevel.in=" + DEFAULT_ESCALATION_LEVEL + "," + UPDATED_ESCALATION_LEVEL,
            "escalationLevel.in=" + UPDATED_ESCALATION_LEVEL
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByEscalationLevelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where escalationLevel is not null
        defaultNotificationFiltering("escalationLevel.specified=true", "escalationLevel.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByEscalationLevelIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where escalationLevel is greater than or equal to
        defaultNotificationFiltering(
            "escalationLevel.greaterThanOrEqual=" + DEFAULT_ESCALATION_LEVEL,
            "escalationLevel.greaterThanOrEqual=" + UPDATED_ESCALATION_LEVEL
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByEscalationLevelIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where escalationLevel is less than or equal to
        defaultNotificationFiltering(
            "escalationLevel.lessThanOrEqual=" + DEFAULT_ESCALATION_LEVEL,
            "escalationLevel.lessThanOrEqual=" + SMALLER_ESCALATION_LEVEL
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByEscalationLevelIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where escalationLevel is less than
        defaultNotificationFiltering(
            "escalationLevel.lessThan=" + UPDATED_ESCALATION_LEVEL,
            "escalationLevel.lessThan=" + DEFAULT_ESCALATION_LEVEL
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByEscalationLevelIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where escalationLevel is greater than
        defaultNotificationFiltering(
            "escalationLevel.greaterThan=" + SMALLER_ESCALATION_LEVEL,
            "escalationLevel.greaterThan=" + DEFAULT_ESCALATION_LEVEL
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where state equals to
        defaultNotificationFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllNotificationsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where state in
        defaultNotificationFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllNotificationsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where state is not null
        defaultNotificationFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByAcknowledgedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where acknowledgedBy equals to
        defaultNotificationFiltering(
            "acknowledgedBy.equals=" + DEFAULT_ACKNOWLEDGED_BY,
            "acknowledgedBy.equals=" + UPDATED_ACKNOWLEDGED_BY
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByAcknowledgedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where acknowledgedBy in
        defaultNotificationFiltering(
            "acknowledgedBy.in=" + DEFAULT_ACKNOWLEDGED_BY + "," + UPDATED_ACKNOWLEDGED_BY,
            "acknowledgedBy.in=" + UPDATED_ACKNOWLEDGED_BY
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByAcknowledgedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where acknowledgedBy is not null
        defaultNotificationFiltering("acknowledgedBy.specified=true", "acknowledgedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByAcknowledgedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where acknowledgedAt equals to
        defaultNotificationFiltering(
            "acknowledgedAt.equals=" + DEFAULT_ACKNOWLEDGED_AT,
            "acknowledgedAt.equals=" + UPDATED_ACKNOWLEDGED_AT
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByAcknowledgedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where acknowledgedAt in
        defaultNotificationFiltering(
            "acknowledgedAt.in=" + DEFAULT_ACKNOWLEDGED_AT + "," + UPDATED_ACKNOWLEDGED_AT,
            "acknowledgedAt.in=" + UPDATED_ACKNOWLEDGED_AT
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByAcknowledgedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where acknowledgedAt is not null
        defaultNotificationFiltering("acknowledgedAt.specified=true", "acknowledgedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsBySummaryIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where summary equals to
        defaultNotificationFiltering("summary.equals=" + DEFAULT_SUMMARY, "summary.equals=" + UPDATED_SUMMARY);
    }

    @Test
    @Transactional
    void getAllNotificationsBySummaryIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where summary in
        defaultNotificationFiltering("summary.in=" + DEFAULT_SUMMARY + "," + UPDATED_SUMMARY, "summary.in=" + UPDATED_SUMMARY);
    }

    @Test
    @Transactional
    void getAllNotificationsBySummaryIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where summary is not null
        defaultNotificationFiltering("summary.specified=true", "summary.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByOccurrenceCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where occurrenceCount equals to
        defaultNotificationFiltering(
            "occurrenceCount.equals=" + DEFAULT_OCCURRENCE_COUNT,
            "occurrenceCount.equals=" + UPDATED_OCCURRENCE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByOccurrenceCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where occurrenceCount in
        defaultNotificationFiltering(
            "occurrenceCount.in=" + DEFAULT_OCCURRENCE_COUNT + "," + UPDATED_OCCURRENCE_COUNT,
            "occurrenceCount.in=" + UPDATED_OCCURRENCE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByOccurrenceCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where occurrenceCount is not null
        defaultNotificationFiltering("occurrenceCount.specified=true", "occurrenceCount.specified=false");
    }

    @Test
    @Transactional
    void getAllNotificationsByOccurrenceCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where occurrenceCount is greater than or equal to
        defaultNotificationFiltering(
            "occurrenceCount.greaterThanOrEqual=" + DEFAULT_OCCURRENCE_COUNT,
            "occurrenceCount.greaterThanOrEqual=" + UPDATED_OCCURRENCE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByOccurrenceCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where occurrenceCount is less than or equal to
        defaultNotificationFiltering(
            "occurrenceCount.lessThanOrEqual=" + DEFAULT_OCCURRENCE_COUNT,
            "occurrenceCount.lessThanOrEqual=" + SMALLER_OCCURRENCE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByOccurrenceCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where occurrenceCount is less than
        defaultNotificationFiltering(
            "occurrenceCount.lessThan=" + UPDATED_OCCURRENCE_COUNT,
            "occurrenceCount.lessThan=" + DEFAULT_OCCURRENCE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByOccurrenceCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedNotification = notificationRepository.saveAndFlush(notification);

        // Get all the notificationList where occurrenceCount is greater than
        defaultNotificationFiltering(
            "occurrenceCount.greaterThan=" + SMALLER_OCCURRENCE_COUNT,
            "occurrenceCount.greaterThan=" + DEFAULT_OCCURRENCE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllNotificationsByConsolidationWindowIsEqualToSomething() throws Exception {
        ConsolidationWindow consolidationWindow;
        if (TestUtil.findAll(em, ConsolidationWindow.class).isEmpty()) {
            notificationRepository.saveAndFlush(notification);
            consolidationWindow = ConsolidationWindowResourceIT.createEntity();
        } else {
            consolidationWindow = TestUtil.findAll(em, ConsolidationWindow.class).get(0);
        }
        em.persist(consolidationWindow);
        em.flush();
        notification.setConsolidationWindow(consolidationWindow);
        notificationRepository.saveAndFlush(notification);
        UUID consolidationWindowId = consolidationWindow.getId();
        // Get all the notificationList where consolidationWindow equals to consolidationWindowId
        defaultNotificationShouldBeFound("consolidationWindowId.equals=" + consolidationWindowId);

        // Get all the notificationList where consolidationWindow equals to UUID.randomUUID()
        defaultNotificationShouldNotBeFound("consolidationWindowId.equals=" + UUID.randomUUID());
    }

    private void defaultNotificationFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultNotificationShouldBeFound(shouldBeFound);
        defaultNotificationShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultNotificationShouldBeFound(String filter) throws Exception {
        restNotificationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(notification.getId().toString())))
            .andExpect(jsonPath("$.[*].notificationType").value(hasItem(DEFAULT_NOTIFICATION_TYPE)))
            .andExpect(jsonPath("$.[*].urgency").value(hasItem(DEFAULT_URGENCY.toString())))
            .andExpect(jsonPath("$.[*].subject").value(hasItem(DEFAULT_SUBJECT)))
            .andExpect(jsonPath("$.[*].objectType").value(hasItem(DEFAULT_OBJECT_TYPE)))
            .andExpect(jsonPath("$.[*].objectId").value(hasItem(DEFAULT_OBJECT_ID)))
            .andExpect(jsonPath("$.[*].variables").value(hasItem(DEFAULT_VARIABLES)))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].sourceEventId").value(hasItem(DEFAULT_SOURCE_EVENT_ID.toString())))
            .andExpect(jsonPath("$.[*].sourceService").value(hasItem(DEFAULT_SOURCE_SERVICE)))
            .andExpect(jsonPath("$.[*].urgentPort").value(hasItem(DEFAULT_URGENT_PORT)))
            .andExpect(jsonPath("$.[*].correlationId").value(hasItem(DEFAULT_CORRELATION_ID.toString())))
            .andExpect(jsonPath("$.[*].groupKey").value(hasItem(DEFAULT_GROUP_KEY)))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].ackDeadline").value(hasItem(DEFAULT_ACK_DEADLINE.toString())))
            .andExpect(jsonPath("$.[*].escalationLevel").value(hasItem(DEFAULT_ESCALATION_LEVEL)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedBy").value(hasItem(DEFAULT_ACKNOWLEDGED_BY.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())))
            .andExpect(jsonPath("$.[*].summary").value(hasItem(DEFAULT_SUMMARY)))
            .andExpect(jsonPath("$.[*].occurrenceCount").value(hasItem(DEFAULT_OCCURRENCE_COUNT)))
            .andExpect(jsonPath("$.[*].signature").value(hasItem(DEFAULT_SIGNATURE)));

        // Check, that the count call also returns 1
        restNotificationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultNotificationShouldNotBeFound(String filter) throws Exception {
        restNotificationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restNotificationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingNotification() throws Exception {
        // Get the notification
        restNotificationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return notificationRepository.count();
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

    protected Notification getPersistedNotification(Notification notification) {
        return notificationRepository.findById(notification.getId()).orElseThrow();
    }

    protected void assertPersistedNotificationToMatchAllProperties(Notification expectedNotification) {
        assertNotificationAllPropertiesEquals(expectedNotification, getPersistedNotification(expectedNotification));
    }

    protected void assertPersistedNotificationToMatchUpdatableProperties(Notification expectedNotification) {
        assertNotificationAllUpdatablePropertiesEquals(expectedNotification, getPersistedNotification(expectedNotification));
    }
}
