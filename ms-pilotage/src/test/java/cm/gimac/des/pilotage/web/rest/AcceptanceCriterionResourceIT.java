package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.AcceptanceCriterionAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.AcceptanceCriterion;
import cm.gimac.des.pilotage.domain.Task;
import cm.gimac.des.pilotage.repository.AcceptanceCriterionRepository;
import cm.gimac.des.pilotage.service.AcceptanceCriterionService;
import cm.gimac.des.pilotage.service.dto.AcceptanceCriterionDTO;
import cm.gimac.des.pilotage.service.mapper.AcceptanceCriterionMapper;
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
 * Integration tests for the {@link AcceptanceCriterionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class AcceptanceCriterionResourceIT {

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final Boolean DEFAULT_SATISFIED = false;
    private static final Boolean UPDATED_SATISFIED = true;

    private static final Boolean DEFAULT_EVIDENCE_REQUIRED = false;
    private static final Boolean UPDATED_EVIDENCE_REQUIRED = true;

    private static final UUID DEFAULT_EVIDENCE_REF = UUID.randomUUID();
    private static final UUID UPDATED_EVIDENCE_REF = UUID.randomUUID();

    private static final Instant DEFAULT_SATISFIED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SATISFIED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/acceptance-criteria";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AcceptanceCriterionRepository acceptanceCriterionRepository;

    @Mock
    private AcceptanceCriterionRepository acceptanceCriterionRepositoryMock;

    @Autowired
    private AcceptanceCriterionMapper acceptanceCriterionMapper;

    @Mock
    private AcceptanceCriterionService acceptanceCriterionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAcceptanceCriterionMockMvc;

    private AcceptanceCriterion acceptanceCriterion;

    private AcceptanceCriterion insertedAcceptanceCriterion;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AcceptanceCriterion createEntity(EntityManager em) {
        AcceptanceCriterion acceptanceCriterion = new AcceptanceCriterion()
            .label(DEFAULT_LABEL)
            .satisfied(DEFAULT_SATISFIED)
            .evidenceRequired(DEFAULT_EVIDENCE_REQUIRED)
            .evidenceRef(DEFAULT_EVIDENCE_REF)
            .satisfiedAt(DEFAULT_SATISFIED_AT);
        // Add required entity
        Task task;
        if (TestUtil.findAll(em, Task.class).isEmpty()) {
            task = TaskResourceIT.createEntity(em);
            em.persist(task);
            em.flush();
        } else {
            task = TestUtil.findAll(em, Task.class).get(0);
        }
        acceptanceCriterion.setTask(task);
        return acceptanceCriterion;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AcceptanceCriterion createUpdatedEntity(EntityManager em) {
        AcceptanceCriterion updatedAcceptanceCriterion = new AcceptanceCriterion()
            .label(UPDATED_LABEL)
            .satisfied(UPDATED_SATISFIED)
            .evidenceRequired(UPDATED_EVIDENCE_REQUIRED)
            .evidenceRef(UPDATED_EVIDENCE_REF)
            .satisfiedAt(UPDATED_SATISFIED_AT);
        // Add required entity
        Task task;
        if (TestUtil.findAll(em, Task.class).isEmpty()) {
            task = TaskResourceIT.createUpdatedEntity(em);
            em.persist(task);
            em.flush();
        } else {
            task = TestUtil.findAll(em, Task.class).get(0);
        }
        updatedAcceptanceCriterion.setTask(task);
        return updatedAcceptanceCriterion;
    }

    @BeforeEach
    void initTest() {
        acceptanceCriterion = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAcceptanceCriterion != null) {
            acceptanceCriterionRepository.delete(insertedAcceptanceCriterion);
            insertedAcceptanceCriterion = null;
        }
    }

    @Test
    @Transactional
    void createAcceptanceCriterion() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AcceptanceCriterion
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(acceptanceCriterion);
        var returnedAcceptanceCriterionDTO = om.readValue(
            restAcceptanceCriterionMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(acceptanceCriterionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AcceptanceCriterionDTO.class
        );

        // Validate the AcceptanceCriterion in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAcceptanceCriterion = acceptanceCriterionMapper.toEntity(returnedAcceptanceCriterionDTO);
        assertAcceptanceCriterionUpdatableFieldsEquals(
            returnedAcceptanceCriterion,
            getPersistedAcceptanceCriterion(returnedAcceptanceCriterion)
        );

        insertedAcceptanceCriterion = returnedAcceptanceCriterion;
    }

    @Test
    @Transactional
    void createAcceptanceCriterionWithExistingId() throws Exception {
        // Create the AcceptanceCriterion with an existing ID
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(acceptanceCriterion);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAcceptanceCriterionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acceptanceCriterionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AcceptanceCriterion in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkLabelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        acceptanceCriterion.setLabel(null);

        // Create the AcceptanceCriterion, which fails.
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(acceptanceCriterion);

        restAcceptanceCriterionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acceptanceCriterionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSatisfiedIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        acceptanceCriterion.setSatisfied(null);

        // Create the AcceptanceCriterion, which fails.
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(acceptanceCriterion);

        restAcceptanceCriterionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acceptanceCriterionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEvidenceRequiredIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        acceptanceCriterion.setEvidenceRequired(null);

        // Create the AcceptanceCriterion, which fails.
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(acceptanceCriterion);

        restAcceptanceCriterionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acceptanceCriterionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteria() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList
        restAcceptanceCriterionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(acceptanceCriterion.getId().toString())))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].satisfied").value(hasItem(DEFAULT_SATISFIED)))
            .andExpect(jsonPath("$.[*].evidenceRequired").value(hasItem(DEFAULT_EVIDENCE_REQUIRED)))
            .andExpect(jsonPath("$.[*].evidenceRef").value(hasItem(DEFAULT_EVIDENCE_REF.toString())))
            .andExpect(jsonPath("$.[*].satisfiedAt").value(hasItem(DEFAULT_SATISFIED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAcceptanceCriteriaWithEagerRelationshipsIsEnabled() throws Exception {
        when(acceptanceCriterionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAcceptanceCriterionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(acceptanceCriterionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAcceptanceCriteriaWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(acceptanceCriterionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAcceptanceCriterionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(acceptanceCriterionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getAcceptanceCriterion() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get the acceptanceCriterion
        restAcceptanceCriterionMockMvc
            .perform(get(ENTITY_API_URL_ID, acceptanceCriterion.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(acceptanceCriterion.getId().toString()))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andExpect(jsonPath("$.satisfied").value(DEFAULT_SATISFIED))
            .andExpect(jsonPath("$.evidenceRequired").value(DEFAULT_EVIDENCE_REQUIRED))
            .andExpect(jsonPath("$.evidenceRef").value(DEFAULT_EVIDENCE_REF.toString()))
            .andExpect(jsonPath("$.satisfiedAt").value(DEFAULT_SATISFIED_AT.toString()));
    }

    @Test
    @Transactional
    void getAcceptanceCriteriaByIdFiltering() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        UUID id = acceptanceCriterion.getId();

        defaultAcceptanceCriterionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByLabelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where label equals to
        defaultAcceptanceCriterionFiltering("label.equals=" + DEFAULT_LABEL, "label.equals=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByLabelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where label in
        defaultAcceptanceCriterionFiltering("label.in=" + DEFAULT_LABEL + "," + UPDATED_LABEL, "label.in=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByLabelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where label is not null
        defaultAcceptanceCriterionFiltering("label.specified=true", "label.specified=false");
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByLabelContainsSomething() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where label contains
        defaultAcceptanceCriterionFiltering("label.contains=" + DEFAULT_LABEL, "label.contains=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByLabelNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where label does not contain
        defaultAcceptanceCriterionFiltering("label.doesNotContain=" + UPDATED_LABEL, "label.doesNotContain=" + DEFAULT_LABEL);
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaBySatisfiedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where satisfied equals to
        defaultAcceptanceCriterionFiltering("satisfied.equals=" + DEFAULT_SATISFIED, "satisfied.equals=" + UPDATED_SATISFIED);
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaBySatisfiedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where satisfied in
        defaultAcceptanceCriterionFiltering(
            "satisfied.in=" + DEFAULT_SATISFIED + "," + UPDATED_SATISFIED,
            "satisfied.in=" + UPDATED_SATISFIED
        );
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaBySatisfiedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where satisfied is not null
        defaultAcceptanceCriterionFiltering("satisfied.specified=true", "satisfied.specified=false");
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByEvidenceRequiredIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where evidenceRequired equals to
        defaultAcceptanceCriterionFiltering(
            "evidenceRequired.equals=" + DEFAULT_EVIDENCE_REQUIRED,
            "evidenceRequired.equals=" + UPDATED_EVIDENCE_REQUIRED
        );
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByEvidenceRequiredIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where evidenceRequired in
        defaultAcceptanceCriterionFiltering(
            "evidenceRequired.in=" + DEFAULT_EVIDENCE_REQUIRED + "," + UPDATED_EVIDENCE_REQUIRED,
            "evidenceRequired.in=" + UPDATED_EVIDENCE_REQUIRED
        );
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByEvidenceRequiredIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where evidenceRequired is not null
        defaultAcceptanceCriterionFiltering("evidenceRequired.specified=true", "evidenceRequired.specified=false");
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByEvidenceRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where evidenceRef equals to
        defaultAcceptanceCriterionFiltering("evidenceRef.equals=" + DEFAULT_EVIDENCE_REF, "evidenceRef.equals=" + UPDATED_EVIDENCE_REF);
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByEvidenceRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where evidenceRef in
        defaultAcceptanceCriterionFiltering(
            "evidenceRef.in=" + DEFAULT_EVIDENCE_REF + "," + UPDATED_EVIDENCE_REF,
            "evidenceRef.in=" + UPDATED_EVIDENCE_REF
        );
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByEvidenceRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where evidenceRef is not null
        defaultAcceptanceCriterionFiltering("evidenceRef.specified=true", "evidenceRef.specified=false");
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaBySatisfiedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where satisfiedAt equals to
        defaultAcceptanceCriterionFiltering("satisfiedAt.equals=" + DEFAULT_SATISFIED_AT, "satisfiedAt.equals=" + UPDATED_SATISFIED_AT);
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaBySatisfiedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where satisfiedAt in
        defaultAcceptanceCriterionFiltering(
            "satisfiedAt.in=" + DEFAULT_SATISFIED_AT + "," + UPDATED_SATISFIED_AT,
            "satisfiedAt.in=" + UPDATED_SATISFIED_AT
        );
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaBySatisfiedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        // Get all the acceptanceCriterionList where satisfiedAt is not null
        defaultAcceptanceCriterionFiltering("satisfiedAt.specified=true", "satisfiedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAcceptanceCriteriaByTaskIsEqualToSomething() throws Exception {
        Task task;
        if (TestUtil.findAll(em, Task.class).isEmpty()) {
            acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);
            task = TaskResourceIT.createEntity(em);
        } else {
            task = TestUtil.findAll(em, Task.class).get(0);
        }
        em.persist(task);
        em.flush();
        acceptanceCriterion.setTask(task);
        acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);
        UUID taskId = task.getId();
        // Get all the acceptanceCriterionList where task equals to taskId
        defaultAcceptanceCriterionShouldBeFound("taskId.equals=" + taskId);

        // Get all the acceptanceCriterionList where task equals to UUID.randomUUID()
        defaultAcceptanceCriterionShouldNotBeFound("taskId.equals=" + UUID.randomUUID());
    }

    private void defaultAcceptanceCriterionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultAcceptanceCriterionShouldBeFound(shouldBeFound);
        defaultAcceptanceCriterionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultAcceptanceCriterionShouldBeFound(String filter) throws Exception {
        restAcceptanceCriterionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(acceptanceCriterion.getId().toString())))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].satisfied").value(hasItem(DEFAULT_SATISFIED)))
            .andExpect(jsonPath("$.[*].evidenceRequired").value(hasItem(DEFAULT_EVIDENCE_REQUIRED)))
            .andExpect(jsonPath("$.[*].evidenceRef").value(hasItem(DEFAULT_EVIDENCE_REF.toString())))
            .andExpect(jsonPath("$.[*].satisfiedAt").value(hasItem(DEFAULT_SATISFIED_AT.toString())));

        // Check, that the count call also returns 1
        restAcceptanceCriterionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultAcceptanceCriterionShouldNotBeFound(String filter) throws Exception {
        restAcceptanceCriterionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restAcceptanceCriterionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingAcceptanceCriterion() throws Exception {
        // Get the acceptanceCriterion
        restAcceptanceCriterionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAcceptanceCriterion() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the acceptanceCriterion
        AcceptanceCriterion updatedAcceptanceCriterion = acceptanceCriterionRepository.findById(acceptanceCriterion.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAcceptanceCriterion are not directly saved in db
        em.detach(updatedAcceptanceCriterion);
        updatedAcceptanceCriterion
            .label(UPDATED_LABEL)
            .satisfied(UPDATED_SATISFIED)
            .evidenceRequired(UPDATED_EVIDENCE_REQUIRED)
            .evidenceRef(UPDATED_EVIDENCE_REF)
            .satisfiedAt(UPDATED_SATISFIED_AT);
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(updatedAcceptanceCriterion);

        restAcceptanceCriterionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, acceptanceCriterionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acceptanceCriterionDTO))
            )
            .andExpect(status().isOk());

        // Validate the AcceptanceCriterion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAcceptanceCriterionToMatchAllProperties(updatedAcceptanceCriterion);
    }

    @Test
    @Transactional
    void putNonExistingAcceptanceCriterion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acceptanceCriterion.setId(UUID.randomUUID());

        // Create the AcceptanceCriterion
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(acceptanceCriterion);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAcceptanceCriterionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, acceptanceCriterionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acceptanceCriterionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AcceptanceCriterion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAcceptanceCriterion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acceptanceCriterion.setId(UUID.randomUUID());

        // Create the AcceptanceCriterion
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(acceptanceCriterion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAcceptanceCriterionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acceptanceCriterionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AcceptanceCriterion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAcceptanceCriterion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acceptanceCriterion.setId(UUID.randomUUID());

        // Create the AcceptanceCriterion
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(acceptanceCriterion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAcceptanceCriterionMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acceptanceCriterionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AcceptanceCriterion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAcceptanceCriterionWithPatch() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the acceptanceCriterion using partial update
        AcceptanceCriterion partialUpdatedAcceptanceCriterion = new AcceptanceCriterion();
        partialUpdatedAcceptanceCriterion.setId(acceptanceCriterion.getId());

        partialUpdatedAcceptanceCriterion.evidenceRef(UPDATED_EVIDENCE_REF);

        restAcceptanceCriterionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAcceptanceCriterion.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAcceptanceCriterion))
            )
            .andExpect(status().isOk());

        // Validate the AcceptanceCriterion in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAcceptanceCriterionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAcceptanceCriterion, acceptanceCriterion),
            getPersistedAcceptanceCriterion(acceptanceCriterion)
        );
    }

    @Test
    @Transactional
    void fullUpdateAcceptanceCriterionWithPatch() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the acceptanceCriterion using partial update
        AcceptanceCriterion partialUpdatedAcceptanceCriterion = new AcceptanceCriterion();
        partialUpdatedAcceptanceCriterion.setId(acceptanceCriterion.getId());

        partialUpdatedAcceptanceCriterion
            .label(UPDATED_LABEL)
            .satisfied(UPDATED_SATISFIED)
            .evidenceRequired(UPDATED_EVIDENCE_REQUIRED)
            .evidenceRef(UPDATED_EVIDENCE_REF)
            .satisfiedAt(UPDATED_SATISFIED_AT);

        restAcceptanceCriterionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAcceptanceCriterion.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAcceptanceCriterion))
            )
            .andExpect(status().isOk());

        // Validate the AcceptanceCriterion in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAcceptanceCriterionUpdatableFieldsEquals(
            partialUpdatedAcceptanceCriterion,
            getPersistedAcceptanceCriterion(partialUpdatedAcceptanceCriterion)
        );
    }

    @Test
    @Transactional
    void patchNonExistingAcceptanceCriterion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acceptanceCriterion.setId(UUID.randomUUID());

        // Create the AcceptanceCriterion
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(acceptanceCriterion);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAcceptanceCriterionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, acceptanceCriterionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(acceptanceCriterionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AcceptanceCriterion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAcceptanceCriterion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acceptanceCriterion.setId(UUID.randomUUID());

        // Create the AcceptanceCriterion
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(acceptanceCriterion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAcceptanceCriterionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(acceptanceCriterionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AcceptanceCriterion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAcceptanceCriterion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acceptanceCriterion.setId(UUID.randomUUID());

        // Create the AcceptanceCriterion
        AcceptanceCriterionDTO acceptanceCriterionDTO = acceptanceCriterionMapper.toDto(acceptanceCriterion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAcceptanceCriterionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(acceptanceCriterionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AcceptanceCriterion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAcceptanceCriterion() throws Exception {
        // Initialize the database
        insertedAcceptanceCriterion = acceptanceCriterionRepository.saveAndFlush(acceptanceCriterion);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the acceptanceCriterion
        restAcceptanceCriterionMockMvc
            .perform(delete(ENTITY_API_URL_ID, acceptanceCriterion.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return acceptanceCriterionRepository.count();
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

    protected AcceptanceCriterion getPersistedAcceptanceCriterion(AcceptanceCriterion acceptanceCriterion) {
        return acceptanceCriterionRepository.findById(acceptanceCriterion.getId()).orElseThrow();
    }

    protected void assertPersistedAcceptanceCriterionToMatchAllProperties(AcceptanceCriterion expectedAcceptanceCriterion) {
        assertAcceptanceCriterionAllPropertiesEquals(
            expectedAcceptanceCriterion,
            getPersistedAcceptanceCriterion(expectedAcceptanceCriterion)
        );
    }

    protected void assertPersistedAcceptanceCriterionToMatchUpdatableProperties(AcceptanceCriterion expectedAcceptanceCriterion) {
        assertAcceptanceCriterionAllUpdatablePropertiesEquals(
            expectedAcceptanceCriterion,
            getPersistedAcceptanceCriterion(expectedAcceptanceCriterion)
        );
    }
}
