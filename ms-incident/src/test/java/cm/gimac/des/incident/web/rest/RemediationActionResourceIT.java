package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.RemediationActionAsserts.*;
import static cm.gimac.des.incident.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.CrisisDecision;
import cm.gimac.des.incident.domain.Incident;
import cm.gimac.des.incident.domain.RemediationAction;
import cm.gimac.des.incident.domain.enumeration.RemediationKind;
import cm.gimac.des.incident.domain.enumeration.RemediationState;
import cm.gimac.des.incident.repository.RemediationActionRepository;
import cm.gimac.des.incident.service.dto.RemediationActionDTO;
import cm.gimac.des.incident.service.mapper.RemediationActionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Integration tests for the {@link RemediationActionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class RemediationActionResourceIT {

    private static final RemediationKind DEFAULT_KIND = RemediationKind.CORRECTIVE;
    private static final RemediationKind UPDATED_KIND = RemediationKind.PREVENTIVE;

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final UUID DEFAULT_OWNER_ID = UUID.randomUUID();
    private static final UUID UPDATED_OWNER_ID = UUID.randomUUID();

    private static final LocalDate DEFAULT_DUE_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DUE_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_DUE_DATE = LocalDate.ofEpochDay(-1L);

    private static final Instant DEFAULT_REQUESTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REQUESTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_TASK_REF = "AAAAAAAAAA";
    private static final String UPDATED_TASK_REF = "BBBBBBBBBB";

    private static final RemediationState DEFAULT_STATE = RemediationState.DEMANDEE;
    private static final RemediationState UPDATED_STATE = RemediationState.EN_COURS;

    private static final Instant DEFAULT_COMPLETED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_COMPLETED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/remediation-actions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RemediationActionRepository remediationActionRepository;

    @Autowired
    private RemediationActionMapper remediationActionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRemediationActionMockMvc;

    private RemediationAction remediationAction;

    private RemediationAction insertedRemediationAction;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RemediationAction createEntity(EntityManager em) {
        RemediationAction remediationAction = new RemediationAction()
            .kind(DEFAULT_KIND)
            .description(DEFAULT_DESCRIPTION)
            .ownerId(DEFAULT_OWNER_ID)
            .dueDate(DEFAULT_DUE_DATE)
            .requestedAt(DEFAULT_REQUESTED_AT)
            .taskRef(DEFAULT_TASK_REF)
            .state(DEFAULT_STATE)
            .completedAt(DEFAULT_COMPLETED_AT);
        // Add required entity
        Incident incident;
        if (TestUtil.findAll(em, Incident.class).isEmpty()) {
            incident = IncidentResourceIT.createEntity(em);
            em.persist(incident);
            em.flush();
        } else {
            incident = TestUtil.findAll(em, Incident.class).get(0);
        }
        remediationAction.setIncident(incident);
        return remediationAction;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RemediationAction createUpdatedEntity(EntityManager em) {
        RemediationAction updatedRemediationAction = new RemediationAction()
            .kind(UPDATED_KIND)
            .description(UPDATED_DESCRIPTION)
            .ownerId(UPDATED_OWNER_ID)
            .dueDate(UPDATED_DUE_DATE)
            .requestedAt(UPDATED_REQUESTED_AT)
            .taskRef(UPDATED_TASK_REF)
            .state(UPDATED_STATE)
            .completedAt(UPDATED_COMPLETED_AT);
        // Add required entity
        Incident incident;
        if (TestUtil.findAll(em, Incident.class).isEmpty()) {
            incident = IncidentResourceIT.createUpdatedEntity(em);
            em.persist(incident);
            em.flush();
        } else {
            incident = TestUtil.findAll(em, Incident.class).get(0);
        }
        updatedRemediationAction.setIncident(incident);
        return updatedRemediationAction;
    }

    @BeforeEach
    void initTest() {
        remediationAction = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedRemediationAction != null) {
            remediationActionRepository.delete(insertedRemediationAction);
            insertedRemediationAction = null;
        }
    }

    @Test
    @Transactional
    void createRemediationAction() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the RemediationAction
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);
        var returnedRemediationActionDTO = om.readValue(
            restRemediationActionMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(remediationActionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RemediationActionDTO.class
        );

        // Validate the RemediationAction in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRemediationAction = remediationActionMapper.toEntity(returnedRemediationActionDTO);
        assertRemediationActionUpdatableFieldsEquals(returnedRemediationAction, getPersistedRemediationAction(returnedRemediationAction));

        insertedRemediationAction = returnedRemediationAction;
    }

    @Test
    @Transactional
    void createRemediationActionWithExistingId() throws Exception {
        // Create the RemediationAction with an existing ID
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRemediationActionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RemediationAction in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkKindIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        remediationAction.setKind(null);

        // Create the RemediationAction, which fails.
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        restRemediationActionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDescriptionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        remediationAction.setDescription(null);

        // Create the RemediationAction, which fails.
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        restRemediationActionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOwnerIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        remediationAction.setOwnerId(null);

        // Create the RemediationAction, which fails.
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        restRemediationActionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDueDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        remediationAction.setDueDate(null);

        // Create the RemediationAction, which fails.
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        restRemediationActionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRequestedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        remediationAction.setRequestedAt(null);

        // Create the RemediationAction, which fails.
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        restRemediationActionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        remediationAction.setState(null);

        // Create the RemediationAction, which fails.
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        restRemediationActionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRemediationActions() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList
        restRemediationActionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(remediationAction.getId().toString())))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].dueDate").value(hasItem(DEFAULT_DUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].taskRef").value(hasItem(DEFAULT_TASK_REF)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].completedAt").value(hasItem(DEFAULT_COMPLETED_AT.toString())));
    }

    @Test
    @Transactional
    void getRemediationAction() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get the remediationAction
        restRemediationActionMockMvc
            .perform(get(ENTITY_API_URL_ID, remediationAction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(remediationAction.getId().toString()))
            .andExpect(jsonPath("$.kind").value(DEFAULT_KIND.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.ownerId").value(DEFAULT_OWNER_ID.toString()))
            .andExpect(jsonPath("$.dueDate").value(DEFAULT_DUE_DATE.toString()))
            .andExpect(jsonPath("$.requestedAt").value(DEFAULT_REQUESTED_AT.toString()))
            .andExpect(jsonPath("$.taskRef").value(DEFAULT_TASK_REF))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.completedAt").value(DEFAULT_COMPLETED_AT.toString()));
    }

    @Test
    @Transactional
    void getRemediationActionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        UUID id = remediationAction.getId();

        defaultRemediationActionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByKindIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where kind equals to
        defaultRemediationActionFiltering("kind.equals=" + DEFAULT_KIND, "kind.equals=" + UPDATED_KIND);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByKindIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where kind in
        defaultRemediationActionFiltering("kind.in=" + DEFAULT_KIND + "," + UPDATED_KIND, "kind.in=" + UPDATED_KIND);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByKindIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where kind is not null
        defaultRemediationActionFiltering("kind.specified=true", "kind.specified=false");
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDescriptionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where description equals to
        defaultRemediationActionFiltering("description.equals=" + DEFAULT_DESCRIPTION, "description.equals=" + UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDescriptionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where description in
        defaultRemediationActionFiltering(
            "description.in=" + DEFAULT_DESCRIPTION + "," + UPDATED_DESCRIPTION,
            "description.in=" + UPDATED_DESCRIPTION
        );
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDescriptionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where description is not null
        defaultRemediationActionFiltering("description.specified=true", "description.specified=false");
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDescriptionContainsSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where description contains
        defaultRemediationActionFiltering("description.contains=" + DEFAULT_DESCRIPTION, "description.contains=" + UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDescriptionNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where description does not contain
        defaultRemediationActionFiltering(
            "description.doesNotContain=" + UPDATED_DESCRIPTION,
            "description.doesNotContain=" + DEFAULT_DESCRIPTION
        );
    }

    @Test
    @Transactional
    void getAllRemediationActionsByOwnerIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where ownerId equals to
        defaultRemediationActionFiltering("ownerId.equals=" + DEFAULT_OWNER_ID, "ownerId.equals=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByOwnerIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where ownerId in
        defaultRemediationActionFiltering("ownerId.in=" + DEFAULT_OWNER_ID + "," + UPDATED_OWNER_ID, "ownerId.in=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByOwnerIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where ownerId is not null
        defaultRemediationActionFiltering("ownerId.specified=true", "ownerId.specified=false");
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDueDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where dueDate equals to
        defaultRemediationActionFiltering("dueDate.equals=" + DEFAULT_DUE_DATE, "dueDate.equals=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDueDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where dueDate in
        defaultRemediationActionFiltering("dueDate.in=" + DEFAULT_DUE_DATE + "," + UPDATED_DUE_DATE, "dueDate.in=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDueDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where dueDate is not null
        defaultRemediationActionFiltering("dueDate.specified=true", "dueDate.specified=false");
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDueDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where dueDate is greater than or equal to
        defaultRemediationActionFiltering(
            "dueDate.greaterThanOrEqual=" + DEFAULT_DUE_DATE,
            "dueDate.greaterThanOrEqual=" + UPDATED_DUE_DATE
        );
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDueDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where dueDate is less than or equal to
        defaultRemediationActionFiltering("dueDate.lessThanOrEqual=" + DEFAULT_DUE_DATE, "dueDate.lessThanOrEqual=" + SMALLER_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDueDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where dueDate is less than
        defaultRemediationActionFiltering("dueDate.lessThan=" + UPDATED_DUE_DATE, "dueDate.lessThan=" + DEFAULT_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByDueDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where dueDate is greater than
        defaultRemediationActionFiltering("dueDate.greaterThan=" + SMALLER_DUE_DATE, "dueDate.greaterThan=" + DEFAULT_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByRequestedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where requestedAt equals to
        defaultRemediationActionFiltering("requestedAt.equals=" + DEFAULT_REQUESTED_AT, "requestedAt.equals=" + UPDATED_REQUESTED_AT);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByRequestedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where requestedAt in
        defaultRemediationActionFiltering(
            "requestedAt.in=" + DEFAULT_REQUESTED_AT + "," + UPDATED_REQUESTED_AT,
            "requestedAt.in=" + UPDATED_REQUESTED_AT
        );
    }

    @Test
    @Transactional
    void getAllRemediationActionsByRequestedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where requestedAt is not null
        defaultRemediationActionFiltering("requestedAt.specified=true", "requestedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRemediationActionsByTaskRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where taskRef equals to
        defaultRemediationActionFiltering("taskRef.equals=" + DEFAULT_TASK_REF, "taskRef.equals=" + UPDATED_TASK_REF);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByTaskRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where taskRef in
        defaultRemediationActionFiltering("taskRef.in=" + DEFAULT_TASK_REF + "," + UPDATED_TASK_REF, "taskRef.in=" + UPDATED_TASK_REF);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByTaskRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where taskRef is not null
        defaultRemediationActionFiltering("taskRef.specified=true", "taskRef.specified=false");
    }

    @Test
    @Transactional
    void getAllRemediationActionsByTaskRefContainsSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where taskRef contains
        defaultRemediationActionFiltering("taskRef.contains=" + DEFAULT_TASK_REF, "taskRef.contains=" + UPDATED_TASK_REF);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByTaskRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where taskRef does not contain
        defaultRemediationActionFiltering("taskRef.doesNotContain=" + UPDATED_TASK_REF, "taskRef.doesNotContain=" + DEFAULT_TASK_REF);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where state equals to
        defaultRemediationActionFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where state in
        defaultRemediationActionFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where state is not null
        defaultRemediationActionFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllRemediationActionsByCompletedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where completedAt equals to
        defaultRemediationActionFiltering("completedAt.equals=" + DEFAULT_COMPLETED_AT, "completedAt.equals=" + UPDATED_COMPLETED_AT);
    }

    @Test
    @Transactional
    void getAllRemediationActionsByCompletedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where completedAt in
        defaultRemediationActionFiltering(
            "completedAt.in=" + DEFAULT_COMPLETED_AT + "," + UPDATED_COMPLETED_AT,
            "completedAt.in=" + UPDATED_COMPLETED_AT
        );
    }

    @Test
    @Transactional
    void getAllRemediationActionsByCompletedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        // Get all the remediationActionList where completedAt is not null
        defaultRemediationActionFiltering("completedAt.specified=true", "completedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRemediationActionsByIncidentIsEqualToSomething() throws Exception {
        Incident incident;
        if (TestUtil.findAll(em, Incident.class).isEmpty()) {
            remediationActionRepository.saveAndFlush(remediationAction);
            incident = IncidentResourceIT.createEntity(em);
        } else {
            incident = TestUtil.findAll(em, Incident.class).get(0);
        }
        em.persist(incident);
        em.flush();
        remediationAction.setIncident(incident);
        remediationActionRepository.saveAndFlush(remediationAction);
        UUID incidentId = incident.getId();
        // Get all the remediationActionList where incident equals to incidentId
        defaultRemediationActionShouldBeFound("incidentId.equals=" + incidentId);

        // Get all the remediationActionList where incident equals to UUID.randomUUID()
        defaultRemediationActionShouldNotBeFound("incidentId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllRemediationActionsByCrisisDecisionIsEqualToSomething() throws Exception {
        CrisisDecision crisisDecision;
        if (TestUtil.findAll(em, CrisisDecision.class).isEmpty()) {
            remediationActionRepository.saveAndFlush(remediationAction);
            crisisDecision = CrisisDecisionResourceIT.createEntity(em);
        } else {
            crisisDecision = TestUtil.findAll(em, CrisisDecision.class).get(0);
        }
        em.persist(crisisDecision);
        em.flush();
        remediationAction.setCrisisDecision(crisisDecision);
        remediationActionRepository.saveAndFlush(remediationAction);
        UUID crisisDecisionId = crisisDecision.getId();
        // Get all the remediationActionList where crisisDecision equals to crisisDecisionId
        defaultRemediationActionShouldBeFound("crisisDecisionId.equals=" + crisisDecisionId);

        // Get all the remediationActionList where crisisDecision equals to UUID.randomUUID()
        defaultRemediationActionShouldNotBeFound("crisisDecisionId.equals=" + UUID.randomUUID());
    }

    private void defaultRemediationActionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultRemediationActionShouldBeFound(shouldBeFound);
        defaultRemediationActionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultRemediationActionShouldBeFound(String filter) throws Exception {
        restRemediationActionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(remediationAction.getId().toString())))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].dueDate").value(hasItem(DEFAULT_DUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].taskRef").value(hasItem(DEFAULT_TASK_REF)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].completedAt").value(hasItem(DEFAULT_COMPLETED_AT.toString())));

        // Check, that the count call also returns 1
        restRemediationActionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultRemediationActionShouldNotBeFound(String filter) throws Exception {
        restRemediationActionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restRemediationActionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingRemediationAction() throws Exception {
        // Get the remediationAction
        restRemediationActionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRemediationAction() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the remediationAction
        RemediationAction updatedRemediationAction = remediationActionRepository.findById(remediationAction.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRemediationAction are not directly saved in db
        em.detach(updatedRemediationAction);
        updatedRemediationAction
            .kind(UPDATED_KIND)
            .description(UPDATED_DESCRIPTION)
            .ownerId(UPDATED_OWNER_ID)
            .dueDate(UPDATED_DUE_DATE)
            .requestedAt(UPDATED_REQUESTED_AT)
            .taskRef(UPDATED_TASK_REF)
            .state(UPDATED_STATE)
            .completedAt(UPDATED_COMPLETED_AT);
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(updatedRemediationAction);

        restRemediationActionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, remediationActionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isOk());

        // Validate the RemediationAction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRemediationActionToMatchAllProperties(updatedRemediationAction);
    }

    @Test
    @Transactional
    void putNonExistingRemediationAction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        remediationAction.setId(UUID.randomUUID());

        // Create the RemediationAction
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRemediationActionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, remediationActionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RemediationAction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRemediationAction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        remediationAction.setId(UUID.randomUUID());

        // Create the RemediationAction
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRemediationActionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RemediationAction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRemediationAction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        remediationAction.setId(UUID.randomUUID());

        // Create the RemediationAction
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRemediationActionMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the RemediationAction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRemediationActionWithPatch() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the remediationAction using partial update
        RemediationAction partialUpdatedRemediationAction = new RemediationAction();
        partialUpdatedRemediationAction.setId(remediationAction.getId());

        partialUpdatedRemediationAction
            .kind(UPDATED_KIND)
            .description(UPDATED_DESCRIPTION)
            .dueDate(UPDATED_DUE_DATE)
            .requestedAt(UPDATED_REQUESTED_AT)
            .taskRef(UPDATED_TASK_REF)
            .completedAt(UPDATED_COMPLETED_AT);

        restRemediationActionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRemediationAction.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRemediationAction))
            )
            .andExpect(status().isOk());

        // Validate the RemediationAction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRemediationActionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRemediationAction, remediationAction),
            getPersistedRemediationAction(remediationAction)
        );
    }

    @Test
    @Transactional
    void fullUpdateRemediationActionWithPatch() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the remediationAction using partial update
        RemediationAction partialUpdatedRemediationAction = new RemediationAction();
        partialUpdatedRemediationAction.setId(remediationAction.getId());

        partialUpdatedRemediationAction
            .kind(UPDATED_KIND)
            .description(UPDATED_DESCRIPTION)
            .ownerId(UPDATED_OWNER_ID)
            .dueDate(UPDATED_DUE_DATE)
            .requestedAt(UPDATED_REQUESTED_AT)
            .taskRef(UPDATED_TASK_REF)
            .state(UPDATED_STATE)
            .completedAt(UPDATED_COMPLETED_AT);

        restRemediationActionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRemediationAction.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRemediationAction))
            )
            .andExpect(status().isOk());

        // Validate the RemediationAction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRemediationActionUpdatableFieldsEquals(
            partialUpdatedRemediationAction,
            getPersistedRemediationAction(partialUpdatedRemediationAction)
        );
    }

    @Test
    @Transactional
    void patchNonExistingRemediationAction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        remediationAction.setId(UUID.randomUUID());

        // Create the RemediationAction
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRemediationActionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, remediationActionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RemediationAction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRemediationAction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        remediationAction.setId(UUID.randomUUID());

        // Create the RemediationAction
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRemediationActionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RemediationAction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRemediationAction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        remediationAction.setId(UUID.randomUUID());

        // Create the RemediationAction
        RemediationActionDTO remediationActionDTO = remediationActionMapper.toDto(remediationAction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRemediationActionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(remediationActionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the RemediationAction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRemediationAction() throws Exception {
        // Initialize the database
        insertedRemediationAction = remediationActionRepository.saveAndFlush(remediationAction);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the remediationAction
        restRemediationActionMockMvc
            .perform(delete(ENTITY_API_URL_ID, remediationAction.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return remediationActionRepository.count();
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

    protected RemediationAction getPersistedRemediationAction(RemediationAction remediationAction) {
        return remediationActionRepository.findById(remediationAction.getId()).orElseThrow();
    }

    protected void assertPersistedRemediationActionToMatchAllProperties(RemediationAction expectedRemediationAction) {
        assertRemediationActionAllPropertiesEquals(expectedRemediationAction, getPersistedRemediationAction(expectedRemediationAction));
    }

    protected void assertPersistedRemediationActionToMatchUpdatableProperties(RemediationAction expectedRemediationAction) {
        assertRemediationActionAllUpdatablePropertiesEquals(
            expectedRemediationAction,
            getPersistedRemediationAction(expectedRemediationAction)
        );
    }
}
