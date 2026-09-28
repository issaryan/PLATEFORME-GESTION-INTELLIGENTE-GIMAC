package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.DecisionAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.Decision;
import cm.gimac.des.pilotage.domain.Decision;
import cm.gimac.des.pilotage.domain.Meeting;
import cm.gimac.des.pilotage.domain.Task;
import cm.gimac.des.pilotage.domain.enumeration.DecisionState;
import cm.gimac.des.pilotage.repository.DecisionRepository;
import cm.gimac.des.pilotage.service.DecisionService;
import cm.gimac.des.pilotage.service.dto.DecisionDTO;
import cm.gimac.des.pilotage.service.mapper.DecisionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Integration tests for the {@link DecisionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class DecisionResourceIT {

    private static final String DEFAULT_TEXT = "AAAAAAAAAA";
    private static final String UPDATED_TEXT = "BBBBBBBBBB";

    private static final UUID DEFAULT_OWNER_ID = UUID.randomUUID();
    private static final UUID UPDATED_OWNER_ID = UUID.randomUUID();

    private static final LocalDate DEFAULT_DUE_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DUE_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_DUE_DATE = LocalDate.ofEpochDay(-1L);

    private static final String DEFAULT_ACCEPTANCE_CRITERIA = "AAAAAAAAAA";
    private static final String UPDATED_ACCEPTANCE_CRITERIA = "BBBBBBBBBB";

    private static final DecisionState DEFAULT_STATE = DecisionState.SAISIE;
    private static final DecisionState UPDATED_STATE = DecisionState.RENVOYEE;

    private static final String DEFAULT_RETURN_REASON = "AAAAAAAAAA";
    private static final String UPDATED_RETURN_REASON = "BBBBBBBBBB";

    private static final Instant DEFAULT_CONVERTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CONVERTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/decisions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DecisionRepository decisionRepository;

    @Mock
    private DecisionRepository decisionRepositoryMock;

    @Autowired
    private DecisionMapper decisionMapper;

    @Mock
    private DecisionService decisionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDecisionMockMvc;

    private Decision decision;

    private Decision insertedDecision;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Decision createEntity(EntityManager em) {
        Decision decision = new Decision()
            .text(DEFAULT_TEXT)
            .ownerId(DEFAULT_OWNER_ID)
            .dueDate(DEFAULT_DUE_DATE)
            .acceptanceCriteria(DEFAULT_ACCEPTANCE_CRITERIA)
            .state(DEFAULT_STATE)
            .returnReason(DEFAULT_RETURN_REASON)
            .convertedAt(DEFAULT_CONVERTED_AT);
        // Add required entity
        Meeting meeting;
        if (TestUtil.findAll(em, Meeting.class).isEmpty()) {
            meeting = MeetingResourceIT.createEntity();
            em.persist(meeting);
            em.flush();
        } else {
            meeting = TestUtil.findAll(em, Meeting.class).get(0);
        }
        decision.setMeeting(meeting);
        return decision;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Decision createUpdatedEntity(EntityManager em) {
        Decision updatedDecision = new Decision()
            .text(UPDATED_TEXT)
            .ownerId(UPDATED_OWNER_ID)
            .dueDate(UPDATED_DUE_DATE)
            .acceptanceCriteria(UPDATED_ACCEPTANCE_CRITERIA)
            .state(UPDATED_STATE)
            .returnReason(UPDATED_RETURN_REASON)
            .convertedAt(UPDATED_CONVERTED_AT);
        // Add required entity
        Meeting meeting;
        if (TestUtil.findAll(em, Meeting.class).isEmpty()) {
            meeting = MeetingResourceIT.createUpdatedEntity();
            em.persist(meeting);
            em.flush();
        } else {
            meeting = TestUtil.findAll(em, Meeting.class).get(0);
        }
        updatedDecision.setMeeting(meeting);
        return updatedDecision;
    }

    @BeforeEach
    void initTest() {
        decision = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedDecision != null) {
            decisionRepository.delete(insertedDecision);
            insertedDecision = null;
        }
    }

    @Test
    @Transactional
    void createDecision() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Decision
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);
        var returnedDecisionDTO = om.readValue(
            restDecisionMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(decisionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            DecisionDTO.class
        );

        // Validate the Decision in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedDecision = decisionMapper.toEntity(returnedDecisionDTO);
        assertDecisionUpdatableFieldsEquals(returnedDecision, getPersistedDecision(returnedDecision));

        insertedDecision = returnedDecision;
    }

    @Test
    @Transactional
    void createDecisionWithExistingId() throws Exception {
        // Create the Decision with an existing ID
        insertedDecision = decisionRepository.saveAndFlush(decision);
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDecisionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(decisionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Decision in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTextIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        decision.setText(null);

        // Create the Decision, which fails.
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);

        restDecisionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(decisionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOwnerIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        decision.setOwnerId(null);

        // Create the Decision, which fails.
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);

        restDecisionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(decisionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDueDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        decision.setDueDate(null);

        // Create the Decision, which fails.
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);

        restDecisionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(decisionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        decision.setState(null);

        // Create the Decision, which fails.
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);

        restDecisionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(decisionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllDecisions() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList
        restDecisionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(decision.getId().toString())))
            .andExpect(jsonPath("$.[*].text").value(hasItem(DEFAULT_TEXT)))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].dueDate").value(hasItem(DEFAULT_DUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].acceptanceCriteria").value(hasItem(DEFAULT_ACCEPTANCE_CRITERIA)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].returnReason").value(hasItem(DEFAULT_RETURN_REASON)))
            .andExpect(jsonPath("$.[*].convertedAt").value(hasItem(DEFAULT_CONVERTED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDecisionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(decisionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restDecisionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(decisionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDecisionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(decisionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restDecisionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(decisionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getDecision() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get the decision
        restDecisionMockMvc
            .perform(get(ENTITY_API_URL_ID, decision.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(decision.getId().toString()))
            .andExpect(jsonPath("$.text").value(DEFAULT_TEXT))
            .andExpect(jsonPath("$.ownerId").value(DEFAULT_OWNER_ID.toString()))
            .andExpect(jsonPath("$.dueDate").value(DEFAULT_DUE_DATE.toString()))
            .andExpect(jsonPath("$.acceptanceCriteria").value(DEFAULT_ACCEPTANCE_CRITERIA))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.returnReason").value(DEFAULT_RETURN_REASON))
            .andExpect(jsonPath("$.convertedAt").value(DEFAULT_CONVERTED_AT.toString()));
    }

    @Test
    @Transactional
    void getDecisionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        UUID id = decision.getId();

        defaultDecisionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllDecisionsByTextIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where text equals to
        defaultDecisionFiltering("text.equals=" + DEFAULT_TEXT, "text.equals=" + UPDATED_TEXT);
    }

    @Test
    @Transactional
    void getAllDecisionsByTextIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where text in
        defaultDecisionFiltering("text.in=" + DEFAULT_TEXT + "," + UPDATED_TEXT, "text.in=" + UPDATED_TEXT);
    }

    @Test
    @Transactional
    void getAllDecisionsByTextIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where text is not null
        defaultDecisionFiltering("text.specified=true", "text.specified=false");
    }

    @Test
    @Transactional
    void getAllDecisionsByTextContainsSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where text contains
        defaultDecisionFiltering("text.contains=" + DEFAULT_TEXT, "text.contains=" + UPDATED_TEXT);
    }

    @Test
    @Transactional
    void getAllDecisionsByTextNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where text does not contain
        defaultDecisionFiltering("text.doesNotContain=" + UPDATED_TEXT, "text.doesNotContain=" + DEFAULT_TEXT);
    }

    @Test
    @Transactional
    void getAllDecisionsByOwnerIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where ownerId equals to
        defaultDecisionFiltering("ownerId.equals=" + DEFAULT_OWNER_ID, "ownerId.equals=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllDecisionsByOwnerIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where ownerId in
        defaultDecisionFiltering("ownerId.in=" + DEFAULT_OWNER_ID + "," + UPDATED_OWNER_ID, "ownerId.in=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllDecisionsByOwnerIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where ownerId is not null
        defaultDecisionFiltering("ownerId.specified=true", "ownerId.specified=false");
    }

    @Test
    @Transactional
    void getAllDecisionsByDueDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where dueDate equals to
        defaultDecisionFiltering("dueDate.equals=" + DEFAULT_DUE_DATE, "dueDate.equals=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllDecisionsByDueDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where dueDate in
        defaultDecisionFiltering("dueDate.in=" + DEFAULT_DUE_DATE + "," + UPDATED_DUE_DATE, "dueDate.in=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllDecisionsByDueDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where dueDate is not null
        defaultDecisionFiltering("dueDate.specified=true", "dueDate.specified=false");
    }

    @Test
    @Transactional
    void getAllDecisionsByDueDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where dueDate is greater than or equal to
        defaultDecisionFiltering("dueDate.greaterThanOrEqual=" + DEFAULT_DUE_DATE, "dueDate.greaterThanOrEqual=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllDecisionsByDueDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where dueDate is less than or equal to
        defaultDecisionFiltering("dueDate.lessThanOrEqual=" + DEFAULT_DUE_DATE, "dueDate.lessThanOrEqual=" + SMALLER_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllDecisionsByDueDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where dueDate is less than
        defaultDecisionFiltering("dueDate.lessThan=" + UPDATED_DUE_DATE, "dueDate.lessThan=" + DEFAULT_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllDecisionsByDueDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where dueDate is greater than
        defaultDecisionFiltering("dueDate.greaterThan=" + SMALLER_DUE_DATE, "dueDate.greaterThan=" + DEFAULT_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllDecisionsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where state equals to
        defaultDecisionFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllDecisionsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where state in
        defaultDecisionFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllDecisionsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where state is not null
        defaultDecisionFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllDecisionsByReturnReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where returnReason equals to
        defaultDecisionFiltering("returnReason.equals=" + DEFAULT_RETURN_REASON, "returnReason.equals=" + UPDATED_RETURN_REASON);
    }

    @Test
    @Transactional
    void getAllDecisionsByReturnReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where returnReason in
        defaultDecisionFiltering(
            "returnReason.in=" + DEFAULT_RETURN_REASON + "," + UPDATED_RETURN_REASON,
            "returnReason.in=" + UPDATED_RETURN_REASON
        );
    }

    @Test
    @Transactional
    void getAllDecisionsByReturnReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where returnReason is not null
        defaultDecisionFiltering("returnReason.specified=true", "returnReason.specified=false");
    }

    @Test
    @Transactional
    void getAllDecisionsByReturnReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where returnReason contains
        defaultDecisionFiltering("returnReason.contains=" + DEFAULT_RETURN_REASON, "returnReason.contains=" + UPDATED_RETURN_REASON);
    }

    @Test
    @Transactional
    void getAllDecisionsByReturnReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where returnReason does not contain
        defaultDecisionFiltering(
            "returnReason.doesNotContain=" + UPDATED_RETURN_REASON,
            "returnReason.doesNotContain=" + DEFAULT_RETURN_REASON
        );
    }

    @Test
    @Transactional
    void getAllDecisionsByConvertedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where convertedAt equals to
        defaultDecisionFiltering("convertedAt.equals=" + DEFAULT_CONVERTED_AT, "convertedAt.equals=" + UPDATED_CONVERTED_AT);
    }

    @Test
    @Transactional
    void getAllDecisionsByConvertedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where convertedAt in
        defaultDecisionFiltering(
            "convertedAt.in=" + DEFAULT_CONVERTED_AT + "," + UPDATED_CONVERTED_AT,
            "convertedAt.in=" + UPDATED_CONVERTED_AT
        );
    }

    @Test
    @Transactional
    void getAllDecisionsByConvertedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        // Get all the decisionList where convertedAt is not null
        defaultDecisionFiltering("convertedAt.specified=true", "convertedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDecisionsByTaskIsEqualToSomething() throws Exception {
        Task task;
        if (TestUtil.findAll(em, Task.class).isEmpty()) {
            decisionRepository.saveAndFlush(decision);
            task = TaskResourceIT.createEntity(em);
        } else {
            task = TestUtil.findAll(em, Task.class).get(0);
        }
        em.persist(task);
        em.flush();
        decision.setTask(task);
        decisionRepository.saveAndFlush(decision);
        UUID taskId = task.getId();
        // Get all the decisionList where task equals to taskId
        defaultDecisionShouldBeFound("taskId.equals=" + taskId);

        // Get all the decisionList where task equals to UUID.randomUUID()
        defaultDecisionShouldNotBeFound("taskId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllDecisionsByMeetingIsEqualToSomething() throws Exception {
        Meeting meeting;
        if (TestUtil.findAll(em, Meeting.class).isEmpty()) {
            decisionRepository.saveAndFlush(decision);
            meeting = MeetingResourceIT.createEntity();
        } else {
            meeting = TestUtil.findAll(em, Meeting.class).get(0);
        }
        em.persist(meeting);
        em.flush();
        decision.setMeeting(meeting);
        decisionRepository.saveAndFlush(decision);
        UUID meetingId = meeting.getId();
        // Get all the decisionList where meeting equals to meetingId
        defaultDecisionShouldBeFound("meetingId.equals=" + meetingId);

        // Get all the decisionList where meeting equals to UUID.randomUUID()
        defaultDecisionShouldNotBeFound("meetingId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllDecisionsByRectifiesIsEqualToSomething() throws Exception {
        Decision rectifies;
        if (TestUtil.findAll(em, Decision.class).isEmpty()) {
            decisionRepository.saveAndFlush(decision);
            rectifies = DecisionResourceIT.createEntity(em);
        } else {
            rectifies = TestUtil.findAll(em, Decision.class).get(0);
        }
        em.persist(rectifies);
        em.flush();
        decision.setRectifies(rectifies);
        decisionRepository.saveAndFlush(decision);
        UUID rectifiesId = rectifies.getId();
        // Get all the decisionList where rectifies equals to rectifiesId
        defaultDecisionShouldBeFound("rectifiesId.equals=" + rectifiesId);

        // Get all the decisionList where rectifies equals to UUID.randomUUID()
        defaultDecisionShouldNotBeFound("rectifiesId.equals=" + UUID.randomUUID());
    }

    private void defaultDecisionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultDecisionShouldBeFound(shouldBeFound);
        defaultDecisionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultDecisionShouldBeFound(String filter) throws Exception {
        restDecisionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(decision.getId().toString())))
            .andExpect(jsonPath("$.[*].text").value(hasItem(DEFAULT_TEXT)))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].dueDate").value(hasItem(DEFAULT_DUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].acceptanceCriteria").value(hasItem(DEFAULT_ACCEPTANCE_CRITERIA)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].returnReason").value(hasItem(DEFAULT_RETURN_REASON)))
            .andExpect(jsonPath("$.[*].convertedAt").value(hasItem(DEFAULT_CONVERTED_AT.toString())));

        // Check, that the count call also returns 1
        restDecisionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultDecisionShouldNotBeFound(String filter) throws Exception {
        restDecisionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restDecisionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingDecision() throws Exception {
        // Get the decision
        restDecisionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDecision() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the decision
        Decision updatedDecision = decisionRepository.findById(decision.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedDecision are not directly saved in db
        em.detach(updatedDecision);
        updatedDecision
            .text(UPDATED_TEXT)
            .ownerId(UPDATED_OWNER_ID)
            .dueDate(UPDATED_DUE_DATE)
            .acceptanceCriteria(UPDATED_ACCEPTANCE_CRITERIA)
            .state(UPDATED_STATE)
            .returnReason(UPDATED_RETURN_REASON)
            .convertedAt(UPDATED_CONVERTED_AT);
        DecisionDTO decisionDTO = decisionMapper.toDto(updatedDecision);

        restDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, decisionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(decisionDTO))
            )
            .andExpect(status().isOk());

        // Validate the Decision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDecisionToMatchAllProperties(updatedDecision);
    }

    @Test
    @Transactional
    void putNonExistingDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        decision.setId(UUID.randomUUID());

        // Create the Decision
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, decisionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(decisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Decision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        decision.setId(UUID.randomUUID());

        // Create the Decision
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(decisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Decision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        decision.setId(UUID.randomUUID());

        // Create the Decision
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDecisionMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(decisionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Decision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDecisionWithPatch() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the decision using partial update
        Decision partialUpdatedDecision = new Decision();
        partialUpdatedDecision.setId(decision.getId());

        partialUpdatedDecision
            .ownerId(UPDATED_OWNER_ID)
            .dueDate(UPDATED_DUE_DATE)
            .acceptanceCriteria(UPDATED_ACCEPTANCE_CRITERIA)
            .returnReason(UPDATED_RETURN_REASON)
            .convertedAt(UPDATED_CONVERTED_AT);

        restDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDecision.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDecision))
            )
            .andExpect(status().isOk());

        // Validate the Decision in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDecisionUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedDecision, decision), getPersistedDecision(decision));
    }

    @Test
    @Transactional
    void fullUpdateDecisionWithPatch() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the decision using partial update
        Decision partialUpdatedDecision = new Decision();
        partialUpdatedDecision.setId(decision.getId());

        partialUpdatedDecision
            .text(UPDATED_TEXT)
            .ownerId(UPDATED_OWNER_ID)
            .dueDate(UPDATED_DUE_DATE)
            .acceptanceCriteria(UPDATED_ACCEPTANCE_CRITERIA)
            .state(UPDATED_STATE)
            .returnReason(UPDATED_RETURN_REASON)
            .convertedAt(UPDATED_CONVERTED_AT);

        restDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDecision.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDecision))
            )
            .andExpect(status().isOk());

        // Validate the Decision in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDecisionUpdatableFieldsEquals(partialUpdatedDecision, getPersistedDecision(partialUpdatedDecision));
    }

    @Test
    @Transactional
    void patchNonExistingDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        decision.setId(UUID.randomUUID());

        // Create the Decision
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, decisionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(decisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Decision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        decision.setId(UUID.randomUUID());

        // Create the Decision
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(decisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Decision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        decision.setId(UUID.randomUUID());

        // Create the Decision
        DecisionDTO decisionDTO = decisionMapper.toDto(decision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(decisionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Decision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDecision() throws Exception {
        // Initialize the database
        insertedDecision = decisionRepository.saveAndFlush(decision);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the decision
        restDecisionMockMvc
            .perform(delete(ENTITY_API_URL_ID, decision.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return decisionRepository.count();
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

    protected Decision getPersistedDecision(Decision decision) {
        return decisionRepository.findById(decision.getId()).orElseThrow();
    }

    protected void assertPersistedDecisionToMatchAllProperties(Decision expectedDecision) {
        assertDecisionAllPropertiesEquals(expectedDecision, getPersistedDecision(expectedDecision));
    }

    protected void assertPersistedDecisionToMatchUpdatableProperties(Decision expectedDecision) {
        assertDecisionAllUpdatablePropertiesEquals(expectedDecision, getPersistedDecision(expectedDecision));
    }
}
