package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.security.AuthoritiesConstants;
import static cm.gimac.des.participant.domain.DeadlineAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.participant.IntegrationTest;
import cm.gimac.des.participant.domain.Convention;
import cm.gimac.des.participant.domain.Deadline;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.LegalDocument;
import cm.gimac.des.participant.domain.enumeration.DeadlineState;
import cm.gimac.des.participant.domain.enumeration.DeadlineThreshold;
import cm.gimac.des.participant.repository.DeadlineRepository;
import cm.gimac.des.participant.service.DeadlineService;
import cm.gimac.des.participant.service.mapper.DeadlineMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link DeadlineResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class DeadlineResourceIT {

    private static final DeadlineThreshold DEFAULT_THRESHOLD = DeadlineThreshold.J_90;
    private static final DeadlineThreshold UPDATED_THRESHOLD = DeadlineThreshold.J_60;

    private static final LocalDate DEFAULT_DUE_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DUE_ON = LocalDate.parse("2026-09-29");
    private static final LocalDate SMALLER_DUE_ON = LocalDate.ofEpochDay(-1L);

    private static final DeadlineState DEFAULT_STATE = DeadlineState.ARMEE;
    private static final DeadlineState UPDATED_STATE = DeadlineState.DECLENCHEE;

    private static final String DEFAULT_TRIGGERED_ACTION = "AAAAAAAAAA";
    private static final String UPDATED_TRIGGERED_ACTION = "BBBBBBBBBB";

    private static final Instant DEFAULT_TRIGGERED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_TRIGGERED_AT = Instant.ofEpochMilli(1790694080285L);

    private static final Boolean DEFAULT_ACKNOWLEDGEMENT_REQUIRED = false;
    private static final Boolean UPDATED_ACKNOWLEDGEMENT_REQUIRED = true;

    private static final UUID DEFAULT_NOTIFICATION_ID = UUID.randomUUID();
    private static final UUID UPDATED_NOTIFICATION_ID = UUID.randomUUID();

    private static final Instant DEFAULT_ACKNOWLEDGED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ACKNOWLEDGED_AT = Instant.ofEpochMilli(1790694080285L);

    private static final String ENTITY_API_URL = "/api/deadlines";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DeadlineRepository deadlineRepository;

    @Mock
    private DeadlineRepository deadlineRepositoryMock;

    @Autowired
    private DeadlineMapper deadlineMapper;

    @Mock
    private DeadlineService deadlineServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDeadlineMockMvc;

    private Deadline deadline;

    private Deadline insertedDeadline;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Deadline createEntity(EntityManager em) {
        Deadline deadline = new Deadline()
            .threshold(DEFAULT_THRESHOLD)
            .dueOn(DEFAULT_DUE_ON)
            .state(DEFAULT_STATE)
            .triggeredAction(DEFAULT_TRIGGERED_ACTION)
            .triggeredAt(DEFAULT_TRIGGERED_AT)
            .acknowledgementRequired(DEFAULT_ACKNOWLEDGEMENT_REQUIRED)
            .notificationId(DEFAULT_NOTIFICATION_ID)
            .acknowledgedAt(DEFAULT_ACKNOWLEDGED_AT);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        deadline.setInstitution(institution);
        return deadline;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Deadline createUpdatedEntity(EntityManager em) {
        Deadline updatedDeadline = new Deadline()
            .threshold(UPDATED_THRESHOLD)
            .dueOn(UPDATED_DUE_ON)
            .state(UPDATED_STATE)
            .triggeredAction(UPDATED_TRIGGERED_ACTION)
            .triggeredAt(UPDATED_TRIGGERED_AT)
            .acknowledgementRequired(UPDATED_ACKNOWLEDGEMENT_REQUIRED)
            .notificationId(UPDATED_NOTIFICATION_ID)
            .acknowledgedAt(UPDATED_ACKNOWLEDGED_AT);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createUpdatedEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        updatedDeadline.setInstitution(institution);
        return updatedDeadline;
    }

    @BeforeEach
    void initTest() {
        deadline = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedDeadline != null) {
            deadlineRepository.delete(insertedDeadline);
            insertedDeadline = null;
        }
    }

    @Test
    @Transactional
    void getAllDeadlines() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList
        restDeadlineMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(deadline.getId().toString())))
            .andExpect(jsonPath("$.[*].threshold").value(hasItem(DEFAULT_THRESHOLD.toString())))
            .andExpect(jsonPath("$.[*].dueOn").value(hasItem(DEFAULT_DUE_ON.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].triggeredAction").value(hasItem(DEFAULT_TRIGGERED_ACTION)))
            .andExpect(jsonPath("$.[*].triggeredAt").value(hasItem(DEFAULT_TRIGGERED_AT.toString())))
            .andExpect(jsonPath("$.[*].acknowledgementRequired").value(hasItem(DEFAULT_ACKNOWLEDGEMENT_REQUIRED)))
            .andExpect(jsonPath("$.[*].notificationId").value(hasItem(DEFAULT_NOTIFICATION_ID.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDeadlinesWithEagerRelationshipsIsEnabled() throws Exception {
        when(deadlineServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restDeadlineMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(deadlineServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDeadlinesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(deadlineServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restDeadlineMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(deadlineRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getDeadline() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get the deadline
        restDeadlineMockMvc
            .perform(get(ENTITY_API_URL_ID, deadline.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(deadline.getId().toString()))
            .andExpect(jsonPath("$.threshold").value(DEFAULT_THRESHOLD.toString()))
            .andExpect(jsonPath("$.dueOn").value(DEFAULT_DUE_ON.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.triggeredAction").value(DEFAULT_TRIGGERED_ACTION))
            .andExpect(jsonPath("$.triggeredAt").value(DEFAULT_TRIGGERED_AT.toString()))
            .andExpect(jsonPath("$.acknowledgementRequired").value(DEFAULT_ACKNOWLEDGEMENT_REQUIRED))
            .andExpect(jsonPath("$.notificationId").value(DEFAULT_NOTIFICATION_ID.toString()))
            .andExpect(jsonPath("$.acknowledgedAt").value(DEFAULT_ACKNOWLEDGED_AT.toString()));
    }

    @Test
    @Transactional
    void getDeadlinesByIdFiltering() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        UUID id = deadline.getId();

        defaultDeadlineFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllDeadlinesByThresholdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where threshold equals to
        defaultDeadlineFiltering("threshold.equals=" + DEFAULT_THRESHOLD, "threshold.equals=" + UPDATED_THRESHOLD);
    }

    @Test
    @Transactional
    void getAllDeadlinesByThresholdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where threshold in
        defaultDeadlineFiltering("threshold.in=" + DEFAULT_THRESHOLD + "," + UPDATED_THRESHOLD, "threshold.in=" + UPDATED_THRESHOLD);
    }

    @Test
    @Transactional
    void getAllDeadlinesByThresholdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where threshold is not null
        defaultDeadlineFiltering("threshold.specified=true", "threshold.specified=false");
    }

    @Test
    @Transactional
    void getAllDeadlinesByDueOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where dueOn equals to
        defaultDeadlineFiltering("dueOn.equals=" + DEFAULT_DUE_ON, "dueOn.equals=" + UPDATED_DUE_ON);
    }

    @Test
    @Transactional
    void getAllDeadlinesByDueOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where dueOn in
        defaultDeadlineFiltering("dueOn.in=" + DEFAULT_DUE_ON + "," + UPDATED_DUE_ON, "dueOn.in=" + UPDATED_DUE_ON);
    }

    @Test
    @Transactional
    void getAllDeadlinesByDueOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where dueOn is not null
        defaultDeadlineFiltering("dueOn.specified=true", "dueOn.specified=false");
    }

    @Test
    @Transactional
    void getAllDeadlinesByDueOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where dueOn is greater than or equal to
        defaultDeadlineFiltering("dueOn.greaterThanOrEqual=" + DEFAULT_DUE_ON, "dueOn.greaterThanOrEqual=" + UPDATED_DUE_ON);
    }

    @Test
    @Transactional
    void getAllDeadlinesByDueOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where dueOn is less than or equal to
        defaultDeadlineFiltering("dueOn.lessThanOrEqual=" + DEFAULT_DUE_ON, "dueOn.lessThanOrEqual=" + SMALLER_DUE_ON);
    }

    @Test
    @Transactional
    void getAllDeadlinesByDueOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where dueOn is less than
        defaultDeadlineFiltering("dueOn.lessThan=" + UPDATED_DUE_ON, "dueOn.lessThan=" + DEFAULT_DUE_ON);
    }

    @Test
    @Transactional
    void getAllDeadlinesByDueOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where dueOn is greater than
        defaultDeadlineFiltering("dueOn.greaterThan=" + SMALLER_DUE_ON, "dueOn.greaterThan=" + DEFAULT_DUE_ON);
    }

    @Test
    @Transactional
    void getAllDeadlinesByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where state equals to
        defaultDeadlineFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllDeadlinesByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where state in
        defaultDeadlineFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllDeadlinesByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where state is not null
        defaultDeadlineFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllDeadlinesByTriggeredActionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where triggeredAction equals to
        defaultDeadlineFiltering(
            "triggeredAction.equals=" + DEFAULT_TRIGGERED_ACTION,
            "triggeredAction.equals=" + UPDATED_TRIGGERED_ACTION
        );
    }

    @Test
    @Transactional
    void getAllDeadlinesByTriggeredActionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where triggeredAction in
        defaultDeadlineFiltering(
            "triggeredAction.in=" + DEFAULT_TRIGGERED_ACTION + "," + UPDATED_TRIGGERED_ACTION,
            "triggeredAction.in=" + UPDATED_TRIGGERED_ACTION
        );
    }

    @Test
    @Transactional
    void getAllDeadlinesByTriggeredActionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where triggeredAction is not null
        defaultDeadlineFiltering("triggeredAction.specified=true", "triggeredAction.specified=false");
    }

    @Test
    @Transactional
    void getAllDeadlinesByTriggeredActionContainsSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where triggeredAction contains
        defaultDeadlineFiltering(
            "triggeredAction.contains=" + DEFAULT_TRIGGERED_ACTION,
            "triggeredAction.contains=" + UPDATED_TRIGGERED_ACTION
        );
    }

    @Test
    @Transactional
    void getAllDeadlinesByTriggeredActionNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where triggeredAction does not contain
        defaultDeadlineFiltering(
            "triggeredAction.doesNotContain=" + UPDATED_TRIGGERED_ACTION,
            "triggeredAction.doesNotContain=" + DEFAULT_TRIGGERED_ACTION
        );
    }

    @Test
    @Transactional
    void getAllDeadlinesByTriggeredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where triggeredAt equals to
        defaultDeadlineFiltering("triggeredAt.equals=" + DEFAULT_TRIGGERED_AT, "triggeredAt.equals=" + UPDATED_TRIGGERED_AT);
    }

    @Test
    @Transactional
    void getAllDeadlinesByTriggeredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where triggeredAt in
        defaultDeadlineFiltering(
            "triggeredAt.in=" + DEFAULT_TRIGGERED_AT + "," + UPDATED_TRIGGERED_AT,
            "triggeredAt.in=" + UPDATED_TRIGGERED_AT
        );
    }

    @Test
    @Transactional
    void getAllDeadlinesByTriggeredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where triggeredAt is not null
        defaultDeadlineFiltering("triggeredAt.specified=true", "triggeredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDeadlinesByAcknowledgementRequiredIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where acknowledgementRequired equals to
        defaultDeadlineFiltering(
            "acknowledgementRequired.equals=" + DEFAULT_ACKNOWLEDGEMENT_REQUIRED,
            "acknowledgementRequired.equals=" + UPDATED_ACKNOWLEDGEMENT_REQUIRED
        );
    }

    @Test
    @Transactional
    void getAllDeadlinesByAcknowledgementRequiredIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where acknowledgementRequired in
        defaultDeadlineFiltering(
            "acknowledgementRequired.in=" + DEFAULT_ACKNOWLEDGEMENT_REQUIRED + "," + UPDATED_ACKNOWLEDGEMENT_REQUIRED,
            "acknowledgementRequired.in=" + UPDATED_ACKNOWLEDGEMENT_REQUIRED
        );
    }

    @Test
    @Transactional
    void getAllDeadlinesByAcknowledgementRequiredIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where acknowledgementRequired is not null
        defaultDeadlineFiltering("acknowledgementRequired.specified=true", "acknowledgementRequired.specified=false");
    }

    @Test
    @Transactional
    void getAllDeadlinesByNotificationIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where notificationId equals to
        defaultDeadlineFiltering("notificationId.equals=" + DEFAULT_NOTIFICATION_ID, "notificationId.equals=" + UPDATED_NOTIFICATION_ID);
    }

    @Test
    @Transactional
    void getAllDeadlinesByNotificationIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where notificationId in
        defaultDeadlineFiltering(
            "notificationId.in=" + DEFAULT_NOTIFICATION_ID + "," + UPDATED_NOTIFICATION_ID,
            "notificationId.in=" + UPDATED_NOTIFICATION_ID
        );
    }

    @Test
    @Transactional
    void getAllDeadlinesByNotificationIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where notificationId is not null
        defaultDeadlineFiltering("notificationId.specified=true", "notificationId.specified=false");
    }

    @Test
    @Transactional
    void getAllDeadlinesByAcknowledgedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where acknowledgedAt equals to
        defaultDeadlineFiltering("acknowledgedAt.equals=" + DEFAULT_ACKNOWLEDGED_AT, "acknowledgedAt.equals=" + UPDATED_ACKNOWLEDGED_AT);
    }

    @Test
    @Transactional
    void getAllDeadlinesByAcknowledgedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where acknowledgedAt in
        defaultDeadlineFiltering(
            "acknowledgedAt.in=" + DEFAULT_ACKNOWLEDGED_AT + "," + UPDATED_ACKNOWLEDGED_AT,
            "acknowledgedAt.in=" + UPDATED_ACKNOWLEDGED_AT
        );
    }

    @Test
    @Transactional
    void getAllDeadlinesByAcknowledgedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDeadline = deadlineRepository.saveAndFlush(deadline);

        // Get all the deadlineList where acknowledgedAt is not null
        defaultDeadlineFiltering("acknowledgedAt.specified=true", "acknowledgedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDeadlinesByInstitutionIsEqualToSomething() throws Exception {
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            deadlineRepository.saveAndFlush(deadline);
            institution = InstitutionResourceIT.createEntity();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        em.persist(institution);
        em.flush();
        deadline.setInstitution(institution);
        deadlineRepository.saveAndFlush(deadline);
        UUID institutionId = institution.getId();
        // Get all the deadlineList where institution equals to institutionId
        defaultDeadlineShouldBeFound("institutionId.equals=" + institutionId);

        // Get all the deadlineList where institution equals to UUID.randomUUID()
        defaultDeadlineShouldNotBeFound("institutionId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllDeadlinesByLegalDocumentIsEqualToSomething() throws Exception {
        LegalDocument legalDocument;
        if (TestUtil.findAll(em, LegalDocument.class).isEmpty()) {
            deadlineRepository.saveAndFlush(deadline);
            legalDocument = LegalDocumentResourceIT.createEntity(em);
        } else {
            legalDocument = TestUtil.findAll(em, LegalDocument.class).getFirst();
        }
        em.persist(legalDocument);
        em.flush();
        deadline.setLegalDocument(legalDocument);
        deadlineRepository.saveAndFlush(deadline);
        UUID legalDocumentId = legalDocument.getId();
        // Get all the deadlineList where legalDocument equals to legalDocumentId
        defaultDeadlineShouldBeFound("legalDocumentId.equals=" + legalDocumentId);

        // Get all the deadlineList where legalDocument equals to UUID.randomUUID()
        defaultDeadlineShouldNotBeFound("legalDocumentId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllDeadlinesByConventionIsEqualToSomething() throws Exception {
        Convention convention;
        if (TestUtil.findAll(em, Convention.class).isEmpty()) {
            deadlineRepository.saveAndFlush(deadline);
            convention = ConventionResourceIT.createEntity(em);
        } else {
            convention = TestUtil.findAll(em, Convention.class).getFirst();
        }
        em.persist(convention);
        em.flush();
        deadline.setConvention(convention);
        deadlineRepository.saveAndFlush(deadline);
        UUID conventionId = convention.getId();
        // Get all the deadlineList where convention equals to conventionId
        defaultDeadlineShouldBeFound("conventionId.equals=" + conventionId);

        // Get all the deadlineList where convention equals to UUID.randomUUID()
        defaultDeadlineShouldNotBeFound("conventionId.equals=" + UUID.randomUUID());
    }

    private void defaultDeadlineFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultDeadlineShouldBeFound(shouldBeFound);
        defaultDeadlineShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultDeadlineShouldBeFound(String filter) throws Exception {
        restDeadlineMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(deadline.getId().toString())))
            .andExpect(jsonPath("$.[*].threshold").value(hasItem(DEFAULT_THRESHOLD.toString())))
            .andExpect(jsonPath("$.[*].dueOn").value(hasItem(DEFAULT_DUE_ON.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].triggeredAction").value(hasItem(DEFAULT_TRIGGERED_ACTION)))
            .andExpect(jsonPath("$.[*].triggeredAt").value(hasItem(DEFAULT_TRIGGERED_AT.toString())))
            .andExpect(jsonPath("$.[*].acknowledgementRequired").value(hasItem(DEFAULT_ACKNOWLEDGEMENT_REQUIRED)))
            .andExpect(jsonPath("$.[*].notificationId").value(hasItem(DEFAULT_NOTIFICATION_ID.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())));

        // Check, that the count call also returns 1
        restDeadlineMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultDeadlineShouldNotBeFound(String filter) throws Exception {
        restDeadlineMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restDeadlineMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingDeadline() throws Exception {
        // Get the deadline
        restDeadlineMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return deadlineRepository.count();
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

    protected Deadline getPersistedDeadline(Deadline deadline) {
        return deadlineRepository.findById(deadline.getId()).orElseThrow();
    }

    protected void assertPersistedDeadlineToMatchAllProperties(Deadline expectedDeadline) {
        assertDeadlineAllPropertiesEquals(expectedDeadline, getPersistedDeadline(expectedDeadline));
    }

    protected void assertPersistedDeadlineToMatchUpdatableProperties(Deadline expectedDeadline) {
        assertDeadlineAllUpdatablePropertiesEquals(expectedDeadline, getPersistedDeadline(expectedDeadline));
    }
}
