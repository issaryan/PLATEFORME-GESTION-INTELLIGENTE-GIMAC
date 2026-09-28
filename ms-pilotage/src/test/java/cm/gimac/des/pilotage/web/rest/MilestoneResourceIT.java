package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.MilestoneAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static cm.gimac.des.pilotage.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.Milestone;
import cm.gimac.des.pilotage.domain.Milestone;
import cm.gimac.des.pilotage.domain.Project;
import cm.gimac.des.pilotage.domain.enumeration.MilestoneState;
import cm.gimac.des.pilotage.repository.MilestoneRepository;
import cm.gimac.des.pilotage.service.MilestoneService;
import cm.gimac.des.pilotage.service.dto.MilestoneDTO;
import cm.gimac.des.pilotage.service.mapper.MilestoneMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
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
 * Integration tests for the {@link MilestoneResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class MilestoneResourceIT {

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final Duration DEFAULT_OPTIMISTIC = Duration.ofHours(6);
    private static final Duration UPDATED_OPTIMISTIC = Duration.ofHours(12);
    private static final Duration SMALLER_OPTIMISTIC = Duration.ofHours(5);

    private static final Duration DEFAULT_MOST_LIKELY = Duration.ofHours(6);
    private static final Duration UPDATED_MOST_LIKELY = Duration.ofHours(12);
    private static final Duration SMALLER_MOST_LIKELY = Duration.ofHours(5);

    private static final Duration DEFAULT_PESSIMISTIC = Duration.ofHours(6);
    private static final Duration UPDATED_PESSIMISTIC = Duration.ofHours(12);
    private static final Duration SMALLER_PESSIMISTIC = Duration.ofHours(5);

    private static final Duration DEFAULT_EXPECTED_DURATION = Duration.ofHours(6);
    private static final Duration UPDATED_EXPECTED_DURATION = Duration.ofHours(12);
    private static final Duration SMALLER_EXPECTED_DURATION = Duration.ofHours(5);

    private static final BigDecimal DEFAULT_VARIANCE = new BigDecimal(0);
    private static final BigDecimal UPDATED_VARIANCE = new BigDecimal(1);
    private static final BigDecimal SMALLER_VARIANCE = new BigDecimal(0 - 1);

    private static final LocalDate DEFAULT_EARLIEST_START = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_EARLIEST_START = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_EARLIEST_START = LocalDate.ofEpochDay(-1L);

    private static final LocalDate DEFAULT_LATEST_START = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_LATEST_START = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_LATEST_START = LocalDate.ofEpochDay(-1L);

    private static final Duration DEFAULT_TOTAL_FLOAT = Duration.ofHours(6);
    private static final Duration UPDATED_TOTAL_FLOAT = Duration.ofHours(12);
    private static final Duration SMALLER_TOTAL_FLOAT = Duration.ofHours(5);

    private static final Duration DEFAULT_FREE_FLOAT = Duration.ofHours(6);
    private static final Duration UPDATED_FREE_FLOAT = Duration.ofHours(12);
    private static final Duration SMALLER_FREE_FLOAT = Duration.ofHours(5);

    private static final Boolean DEFAULT_CRITICAL = false;
    private static final Boolean UPDATED_CRITICAL = true;

    private static final MilestoneState DEFAULT_STATE = MilestoneState.PREVU;
    private static final MilestoneState UPDATED_STATE = MilestoneState.EN_ATTENTE_VISA;

    private static final Duration DEFAULT_ACTUAL_DURATION = Duration.ofHours(6);
    private static final Duration UPDATED_ACTUAL_DURATION = Duration.ofHours(12);
    private static final Duration SMALLER_ACTUAL_DURATION = Duration.ofHours(5);

    private static final Instant DEFAULT_REACHED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REACHED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/milestones";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Mock
    private MilestoneRepository milestoneRepositoryMock;

    @Autowired
    private MilestoneMapper milestoneMapper;

    @Mock
    private MilestoneService milestoneServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMilestoneMockMvc;

    private Milestone milestone;

    private Milestone insertedMilestone;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Milestone createEntity(EntityManager em) {
        Milestone milestone = new Milestone()
            .label(DEFAULT_LABEL)
            .optimistic(DEFAULT_OPTIMISTIC)
            .mostLikely(DEFAULT_MOST_LIKELY)
            .pessimistic(DEFAULT_PESSIMISTIC)
            .expectedDuration(DEFAULT_EXPECTED_DURATION)
            .variance(DEFAULT_VARIANCE)
            .earliestStart(DEFAULT_EARLIEST_START)
            .latestStart(DEFAULT_LATEST_START)
            .totalFloat(DEFAULT_TOTAL_FLOAT)
            .freeFloat(DEFAULT_FREE_FLOAT)
            .critical(DEFAULT_CRITICAL)
            .state(DEFAULT_STATE)
            .actualDuration(DEFAULT_ACTUAL_DURATION)
            .reachedAt(DEFAULT_REACHED_AT);
        // Add required entity
        Project project;
        if (TestUtil.findAll(em, Project.class).isEmpty()) {
            project = ProjectResourceIT.createEntity();
            em.persist(project);
            em.flush();
        } else {
            project = TestUtil.findAll(em, Project.class).get(0);
        }
        milestone.setProject(project);
        return milestone;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Milestone createUpdatedEntity(EntityManager em) {
        Milestone updatedMilestone = new Milestone()
            .label(UPDATED_LABEL)
            .optimistic(UPDATED_OPTIMISTIC)
            .mostLikely(UPDATED_MOST_LIKELY)
            .pessimistic(UPDATED_PESSIMISTIC)
            .expectedDuration(UPDATED_EXPECTED_DURATION)
            .variance(UPDATED_VARIANCE)
            .earliestStart(UPDATED_EARLIEST_START)
            .latestStart(UPDATED_LATEST_START)
            .totalFloat(UPDATED_TOTAL_FLOAT)
            .freeFloat(UPDATED_FREE_FLOAT)
            .critical(UPDATED_CRITICAL)
            .state(UPDATED_STATE)
            .actualDuration(UPDATED_ACTUAL_DURATION)
            .reachedAt(UPDATED_REACHED_AT);
        // Add required entity
        Project project;
        if (TestUtil.findAll(em, Project.class).isEmpty()) {
            project = ProjectResourceIT.createUpdatedEntity();
            em.persist(project);
            em.flush();
        } else {
            project = TestUtil.findAll(em, Project.class).get(0);
        }
        updatedMilestone.setProject(project);
        return updatedMilestone;
    }

    @BeforeEach
    void initTest() {
        milestone = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedMilestone != null) {
            milestoneRepository.delete(insertedMilestone);
            insertedMilestone = null;
        }
    }

    @Test
    @Transactional
    void createMilestone() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Milestone
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);
        var returnedMilestoneDTO = om.readValue(
            restMilestoneMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(milestoneDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            MilestoneDTO.class
        );

        // Validate the Milestone in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedMilestone = milestoneMapper.toEntity(returnedMilestoneDTO);
        assertMilestoneUpdatableFieldsEquals(returnedMilestone, getPersistedMilestone(returnedMilestone));

        insertedMilestone = returnedMilestone;
    }

    @Test
    @Transactional
    void createMilestoneWithExistingId() throws Exception {
        // Create the Milestone with an existing ID
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restMilestoneMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(milestoneDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Milestone in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkLabelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        milestone.setLabel(null);

        // Create the Milestone, which fails.
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        restMilestoneMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(milestoneDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOptimisticIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        milestone.setOptimistic(null);

        // Create the Milestone, which fails.
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        restMilestoneMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(milestoneDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMostLikelyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        milestone.setMostLikely(null);

        // Create the Milestone, which fails.
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        restMilestoneMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(milestoneDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPessimisticIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        milestone.setPessimistic(null);

        // Create the Milestone, which fails.
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        restMilestoneMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(milestoneDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCriticalIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        milestone.setCritical(null);

        // Create the Milestone, which fails.
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        restMilestoneMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(milestoneDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        milestone.setState(null);

        // Create the Milestone, which fails.
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        restMilestoneMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(milestoneDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllMilestones() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList
        restMilestoneMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(milestone.getId().toString())))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].optimistic").value(hasItem(DEFAULT_OPTIMISTIC.toString())))
            .andExpect(jsonPath("$.[*].mostLikely").value(hasItem(DEFAULT_MOST_LIKELY.toString())))
            .andExpect(jsonPath("$.[*].pessimistic").value(hasItem(DEFAULT_PESSIMISTIC.toString())))
            .andExpect(jsonPath("$.[*].expectedDuration").value(hasItem(DEFAULT_EXPECTED_DURATION.toString())))
            .andExpect(jsonPath("$.[*].variance").value(hasItem(sameNumber(DEFAULT_VARIANCE))))
            .andExpect(jsonPath("$.[*].earliestStart").value(hasItem(DEFAULT_EARLIEST_START.toString())))
            .andExpect(jsonPath("$.[*].latestStart").value(hasItem(DEFAULT_LATEST_START.toString())))
            .andExpect(jsonPath("$.[*].totalFloat").value(hasItem(DEFAULT_TOTAL_FLOAT.toString())))
            .andExpect(jsonPath("$.[*].freeFloat").value(hasItem(DEFAULT_FREE_FLOAT.toString())))
            .andExpect(jsonPath("$.[*].critical").value(hasItem(DEFAULT_CRITICAL)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].actualDuration").value(hasItem(DEFAULT_ACTUAL_DURATION.toString())))
            .andExpect(jsonPath("$.[*].reachedAt").value(hasItem(DEFAULT_REACHED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMilestonesWithEagerRelationshipsIsEnabled() throws Exception {
        when(milestoneServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMilestoneMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(milestoneServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMilestonesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(milestoneServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMilestoneMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(milestoneRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getMilestone() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get the milestone
        restMilestoneMockMvc
            .perform(get(ENTITY_API_URL_ID, milestone.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(milestone.getId().toString()))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andExpect(jsonPath("$.optimistic").value(DEFAULT_OPTIMISTIC.toString()))
            .andExpect(jsonPath("$.mostLikely").value(DEFAULT_MOST_LIKELY.toString()))
            .andExpect(jsonPath("$.pessimistic").value(DEFAULT_PESSIMISTIC.toString()))
            .andExpect(jsonPath("$.expectedDuration").value(DEFAULT_EXPECTED_DURATION.toString()))
            .andExpect(jsonPath("$.variance").value(sameNumber(DEFAULT_VARIANCE)))
            .andExpect(jsonPath("$.earliestStart").value(DEFAULT_EARLIEST_START.toString()))
            .andExpect(jsonPath("$.latestStart").value(DEFAULT_LATEST_START.toString()))
            .andExpect(jsonPath("$.totalFloat").value(DEFAULT_TOTAL_FLOAT.toString()))
            .andExpect(jsonPath("$.freeFloat").value(DEFAULT_FREE_FLOAT.toString()))
            .andExpect(jsonPath("$.critical").value(DEFAULT_CRITICAL))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.actualDuration").value(DEFAULT_ACTUAL_DURATION.toString()))
            .andExpect(jsonPath("$.reachedAt").value(DEFAULT_REACHED_AT.toString()));
    }

    @Test
    @Transactional
    void getMilestonesByIdFiltering() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        UUID id = milestone.getId();

        defaultMilestoneFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllMilestonesByLabelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where label equals to
        defaultMilestoneFiltering("label.equals=" + DEFAULT_LABEL, "label.equals=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllMilestonesByLabelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where label in
        defaultMilestoneFiltering("label.in=" + DEFAULT_LABEL + "," + UPDATED_LABEL, "label.in=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllMilestonesByLabelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where label is not null
        defaultMilestoneFiltering("label.specified=true", "label.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByLabelContainsSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where label contains
        defaultMilestoneFiltering("label.contains=" + DEFAULT_LABEL, "label.contains=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllMilestonesByLabelNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where label does not contain
        defaultMilestoneFiltering("label.doesNotContain=" + UPDATED_LABEL, "label.doesNotContain=" + DEFAULT_LABEL);
    }

    @Test
    @Transactional
    void getAllMilestonesByOptimisticIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where optimistic equals to
        defaultMilestoneFiltering("optimistic.equals=" + DEFAULT_OPTIMISTIC, "optimistic.equals=" + UPDATED_OPTIMISTIC);
    }

    @Test
    @Transactional
    void getAllMilestonesByOptimisticIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where optimistic in
        defaultMilestoneFiltering("optimistic.in=" + DEFAULT_OPTIMISTIC + "," + UPDATED_OPTIMISTIC, "optimistic.in=" + UPDATED_OPTIMISTIC);
    }

    @Test
    @Transactional
    void getAllMilestonesByOptimisticIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where optimistic is not null
        defaultMilestoneFiltering("optimistic.specified=true", "optimistic.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByOptimisticIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where optimistic is greater than or equal to
        defaultMilestoneFiltering(
            "optimistic.greaterThanOrEqual=" + DEFAULT_OPTIMISTIC,
            "optimistic.greaterThanOrEqual=" + UPDATED_OPTIMISTIC
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByOptimisticIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where optimistic is less than or equal to
        defaultMilestoneFiltering("optimistic.lessThanOrEqual=" + DEFAULT_OPTIMISTIC, "optimistic.lessThanOrEqual=" + SMALLER_OPTIMISTIC);
    }

    @Test
    @Transactional
    void getAllMilestonesByOptimisticIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where optimistic is less than
        defaultMilestoneFiltering("optimistic.lessThan=" + UPDATED_OPTIMISTIC, "optimistic.lessThan=" + DEFAULT_OPTIMISTIC);
    }

    @Test
    @Transactional
    void getAllMilestonesByOptimisticIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where optimistic is greater than
        defaultMilestoneFiltering("optimistic.greaterThan=" + SMALLER_OPTIMISTIC, "optimistic.greaterThan=" + DEFAULT_OPTIMISTIC);
    }

    @Test
    @Transactional
    void getAllMilestonesByMostLikelyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where mostLikely equals to
        defaultMilestoneFiltering("mostLikely.equals=" + DEFAULT_MOST_LIKELY, "mostLikely.equals=" + UPDATED_MOST_LIKELY);
    }

    @Test
    @Transactional
    void getAllMilestonesByMostLikelyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where mostLikely in
        defaultMilestoneFiltering(
            "mostLikely.in=" + DEFAULT_MOST_LIKELY + "," + UPDATED_MOST_LIKELY,
            "mostLikely.in=" + UPDATED_MOST_LIKELY
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByMostLikelyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where mostLikely is not null
        defaultMilestoneFiltering("mostLikely.specified=true", "mostLikely.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByMostLikelyIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where mostLikely is greater than or equal to
        defaultMilestoneFiltering(
            "mostLikely.greaterThanOrEqual=" + DEFAULT_MOST_LIKELY,
            "mostLikely.greaterThanOrEqual=" + UPDATED_MOST_LIKELY
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByMostLikelyIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where mostLikely is less than or equal to
        defaultMilestoneFiltering("mostLikely.lessThanOrEqual=" + DEFAULT_MOST_LIKELY, "mostLikely.lessThanOrEqual=" + SMALLER_MOST_LIKELY);
    }

    @Test
    @Transactional
    void getAllMilestonesByMostLikelyIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where mostLikely is less than
        defaultMilestoneFiltering("mostLikely.lessThan=" + UPDATED_MOST_LIKELY, "mostLikely.lessThan=" + DEFAULT_MOST_LIKELY);
    }

    @Test
    @Transactional
    void getAllMilestonesByMostLikelyIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where mostLikely is greater than
        defaultMilestoneFiltering("mostLikely.greaterThan=" + SMALLER_MOST_LIKELY, "mostLikely.greaterThan=" + DEFAULT_MOST_LIKELY);
    }

    @Test
    @Transactional
    void getAllMilestonesByPessimisticIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where pessimistic equals to
        defaultMilestoneFiltering("pessimistic.equals=" + DEFAULT_PESSIMISTIC, "pessimistic.equals=" + UPDATED_PESSIMISTIC);
    }

    @Test
    @Transactional
    void getAllMilestonesByPessimisticIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where pessimistic in
        defaultMilestoneFiltering(
            "pessimistic.in=" + DEFAULT_PESSIMISTIC + "," + UPDATED_PESSIMISTIC,
            "pessimistic.in=" + UPDATED_PESSIMISTIC
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByPessimisticIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where pessimistic is not null
        defaultMilestoneFiltering("pessimistic.specified=true", "pessimistic.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByPessimisticIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where pessimistic is greater than or equal to
        defaultMilestoneFiltering(
            "pessimistic.greaterThanOrEqual=" + DEFAULT_PESSIMISTIC,
            "pessimistic.greaterThanOrEqual=" + UPDATED_PESSIMISTIC
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByPessimisticIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where pessimistic is less than or equal to
        defaultMilestoneFiltering(
            "pessimistic.lessThanOrEqual=" + DEFAULT_PESSIMISTIC,
            "pessimistic.lessThanOrEqual=" + SMALLER_PESSIMISTIC
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByPessimisticIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where pessimistic is less than
        defaultMilestoneFiltering("pessimistic.lessThan=" + UPDATED_PESSIMISTIC, "pessimistic.lessThan=" + DEFAULT_PESSIMISTIC);
    }

    @Test
    @Transactional
    void getAllMilestonesByPessimisticIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where pessimistic is greater than
        defaultMilestoneFiltering("pessimistic.greaterThan=" + SMALLER_PESSIMISTIC, "pessimistic.greaterThan=" + DEFAULT_PESSIMISTIC);
    }

    @Test
    @Transactional
    void getAllMilestonesByExpectedDurationIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where expectedDuration equals to
        defaultMilestoneFiltering(
            "expectedDuration.equals=" + DEFAULT_EXPECTED_DURATION,
            "expectedDuration.equals=" + UPDATED_EXPECTED_DURATION
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByExpectedDurationIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where expectedDuration in
        defaultMilestoneFiltering(
            "expectedDuration.in=" + DEFAULT_EXPECTED_DURATION + "," + UPDATED_EXPECTED_DURATION,
            "expectedDuration.in=" + UPDATED_EXPECTED_DURATION
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByExpectedDurationIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where expectedDuration is not null
        defaultMilestoneFiltering("expectedDuration.specified=true", "expectedDuration.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByExpectedDurationIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where expectedDuration is greater than or equal to
        defaultMilestoneFiltering(
            "expectedDuration.greaterThanOrEqual=" + DEFAULT_EXPECTED_DURATION,
            "expectedDuration.greaterThanOrEqual=" + UPDATED_EXPECTED_DURATION
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByExpectedDurationIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where expectedDuration is less than or equal to
        defaultMilestoneFiltering(
            "expectedDuration.lessThanOrEqual=" + DEFAULT_EXPECTED_DURATION,
            "expectedDuration.lessThanOrEqual=" + SMALLER_EXPECTED_DURATION
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByExpectedDurationIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where expectedDuration is less than
        defaultMilestoneFiltering(
            "expectedDuration.lessThan=" + UPDATED_EXPECTED_DURATION,
            "expectedDuration.lessThan=" + DEFAULT_EXPECTED_DURATION
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByExpectedDurationIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where expectedDuration is greater than
        defaultMilestoneFiltering(
            "expectedDuration.greaterThan=" + SMALLER_EXPECTED_DURATION,
            "expectedDuration.greaterThan=" + DEFAULT_EXPECTED_DURATION
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByVarianceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where variance equals to
        defaultMilestoneFiltering("variance.equals=" + DEFAULT_VARIANCE, "variance.equals=" + UPDATED_VARIANCE);
    }

    @Test
    @Transactional
    void getAllMilestonesByVarianceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where variance in
        defaultMilestoneFiltering("variance.in=" + DEFAULT_VARIANCE + "," + UPDATED_VARIANCE, "variance.in=" + UPDATED_VARIANCE);
    }

    @Test
    @Transactional
    void getAllMilestonesByVarianceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where variance is not null
        defaultMilestoneFiltering("variance.specified=true", "variance.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByVarianceIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where variance is greater than or equal to
        defaultMilestoneFiltering("variance.greaterThanOrEqual=" + DEFAULT_VARIANCE, "variance.greaterThanOrEqual=" + UPDATED_VARIANCE);
    }

    @Test
    @Transactional
    void getAllMilestonesByVarianceIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where variance is less than or equal to
        defaultMilestoneFiltering("variance.lessThanOrEqual=" + DEFAULT_VARIANCE, "variance.lessThanOrEqual=" + SMALLER_VARIANCE);
    }

    @Test
    @Transactional
    void getAllMilestonesByVarianceIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where variance is less than
        defaultMilestoneFiltering("variance.lessThan=" + UPDATED_VARIANCE, "variance.lessThan=" + DEFAULT_VARIANCE);
    }

    @Test
    @Transactional
    void getAllMilestonesByVarianceIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where variance is greater than
        defaultMilestoneFiltering("variance.greaterThan=" + SMALLER_VARIANCE, "variance.greaterThan=" + DEFAULT_VARIANCE);
    }

    @Test
    @Transactional
    void getAllMilestonesByEarliestStartIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where earliestStart equals to
        defaultMilestoneFiltering("earliestStart.equals=" + DEFAULT_EARLIEST_START, "earliestStart.equals=" + UPDATED_EARLIEST_START);
    }

    @Test
    @Transactional
    void getAllMilestonesByEarliestStartIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where earliestStart in
        defaultMilestoneFiltering(
            "earliestStart.in=" + DEFAULT_EARLIEST_START + "," + UPDATED_EARLIEST_START,
            "earliestStart.in=" + UPDATED_EARLIEST_START
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByEarliestStartIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where earliestStart is not null
        defaultMilestoneFiltering("earliestStart.specified=true", "earliestStart.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByEarliestStartIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where earliestStart is greater than or equal to
        defaultMilestoneFiltering(
            "earliestStart.greaterThanOrEqual=" + DEFAULT_EARLIEST_START,
            "earliestStart.greaterThanOrEqual=" + UPDATED_EARLIEST_START
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByEarliestStartIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where earliestStart is less than or equal to
        defaultMilestoneFiltering(
            "earliestStart.lessThanOrEqual=" + DEFAULT_EARLIEST_START,
            "earliestStart.lessThanOrEqual=" + SMALLER_EARLIEST_START
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByEarliestStartIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where earliestStart is less than
        defaultMilestoneFiltering("earliestStart.lessThan=" + UPDATED_EARLIEST_START, "earliestStart.lessThan=" + DEFAULT_EARLIEST_START);
    }

    @Test
    @Transactional
    void getAllMilestonesByEarliestStartIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where earliestStart is greater than
        defaultMilestoneFiltering(
            "earliestStart.greaterThan=" + SMALLER_EARLIEST_START,
            "earliestStart.greaterThan=" + DEFAULT_EARLIEST_START
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByLatestStartIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where latestStart equals to
        defaultMilestoneFiltering("latestStart.equals=" + DEFAULT_LATEST_START, "latestStart.equals=" + UPDATED_LATEST_START);
    }

    @Test
    @Transactional
    void getAllMilestonesByLatestStartIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where latestStart in
        defaultMilestoneFiltering(
            "latestStart.in=" + DEFAULT_LATEST_START + "," + UPDATED_LATEST_START,
            "latestStart.in=" + UPDATED_LATEST_START
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByLatestStartIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where latestStart is not null
        defaultMilestoneFiltering("latestStart.specified=true", "latestStart.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByLatestStartIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where latestStart is greater than or equal to
        defaultMilestoneFiltering(
            "latestStart.greaterThanOrEqual=" + DEFAULT_LATEST_START,
            "latestStart.greaterThanOrEqual=" + UPDATED_LATEST_START
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByLatestStartIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where latestStart is less than or equal to
        defaultMilestoneFiltering(
            "latestStart.lessThanOrEqual=" + DEFAULT_LATEST_START,
            "latestStart.lessThanOrEqual=" + SMALLER_LATEST_START
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByLatestStartIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where latestStart is less than
        defaultMilestoneFiltering("latestStart.lessThan=" + UPDATED_LATEST_START, "latestStart.lessThan=" + DEFAULT_LATEST_START);
    }

    @Test
    @Transactional
    void getAllMilestonesByLatestStartIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where latestStart is greater than
        defaultMilestoneFiltering("latestStart.greaterThan=" + SMALLER_LATEST_START, "latestStart.greaterThan=" + DEFAULT_LATEST_START);
    }

    @Test
    @Transactional
    void getAllMilestonesByTotalFloatIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where totalFloat equals to
        defaultMilestoneFiltering("totalFloat.equals=" + DEFAULT_TOTAL_FLOAT, "totalFloat.equals=" + UPDATED_TOTAL_FLOAT);
    }

    @Test
    @Transactional
    void getAllMilestonesByTotalFloatIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where totalFloat in
        defaultMilestoneFiltering(
            "totalFloat.in=" + DEFAULT_TOTAL_FLOAT + "," + UPDATED_TOTAL_FLOAT,
            "totalFloat.in=" + UPDATED_TOTAL_FLOAT
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByTotalFloatIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where totalFloat is not null
        defaultMilestoneFiltering("totalFloat.specified=true", "totalFloat.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByTotalFloatIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where totalFloat is greater than or equal to
        defaultMilestoneFiltering(
            "totalFloat.greaterThanOrEqual=" + DEFAULT_TOTAL_FLOAT,
            "totalFloat.greaterThanOrEqual=" + UPDATED_TOTAL_FLOAT
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByTotalFloatIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where totalFloat is less than or equal to
        defaultMilestoneFiltering("totalFloat.lessThanOrEqual=" + DEFAULT_TOTAL_FLOAT, "totalFloat.lessThanOrEqual=" + SMALLER_TOTAL_FLOAT);
    }

    @Test
    @Transactional
    void getAllMilestonesByTotalFloatIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where totalFloat is less than
        defaultMilestoneFiltering("totalFloat.lessThan=" + UPDATED_TOTAL_FLOAT, "totalFloat.lessThan=" + DEFAULT_TOTAL_FLOAT);
    }

    @Test
    @Transactional
    void getAllMilestonesByTotalFloatIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where totalFloat is greater than
        defaultMilestoneFiltering("totalFloat.greaterThan=" + SMALLER_TOTAL_FLOAT, "totalFloat.greaterThan=" + DEFAULT_TOTAL_FLOAT);
    }

    @Test
    @Transactional
    void getAllMilestonesByFreeFloatIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where freeFloat equals to
        defaultMilestoneFiltering("freeFloat.equals=" + DEFAULT_FREE_FLOAT, "freeFloat.equals=" + UPDATED_FREE_FLOAT);
    }

    @Test
    @Transactional
    void getAllMilestonesByFreeFloatIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where freeFloat in
        defaultMilestoneFiltering("freeFloat.in=" + DEFAULT_FREE_FLOAT + "," + UPDATED_FREE_FLOAT, "freeFloat.in=" + UPDATED_FREE_FLOAT);
    }

    @Test
    @Transactional
    void getAllMilestonesByFreeFloatIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where freeFloat is not null
        defaultMilestoneFiltering("freeFloat.specified=true", "freeFloat.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByFreeFloatIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where freeFloat is greater than or equal to
        defaultMilestoneFiltering(
            "freeFloat.greaterThanOrEqual=" + DEFAULT_FREE_FLOAT,
            "freeFloat.greaterThanOrEqual=" + UPDATED_FREE_FLOAT
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByFreeFloatIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where freeFloat is less than or equal to
        defaultMilestoneFiltering("freeFloat.lessThanOrEqual=" + DEFAULT_FREE_FLOAT, "freeFloat.lessThanOrEqual=" + SMALLER_FREE_FLOAT);
    }

    @Test
    @Transactional
    void getAllMilestonesByFreeFloatIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where freeFloat is less than
        defaultMilestoneFiltering("freeFloat.lessThan=" + UPDATED_FREE_FLOAT, "freeFloat.lessThan=" + DEFAULT_FREE_FLOAT);
    }

    @Test
    @Transactional
    void getAllMilestonesByFreeFloatIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where freeFloat is greater than
        defaultMilestoneFiltering("freeFloat.greaterThan=" + SMALLER_FREE_FLOAT, "freeFloat.greaterThan=" + DEFAULT_FREE_FLOAT);
    }

    @Test
    @Transactional
    void getAllMilestonesByCriticalIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where critical equals to
        defaultMilestoneFiltering("critical.equals=" + DEFAULT_CRITICAL, "critical.equals=" + UPDATED_CRITICAL);
    }

    @Test
    @Transactional
    void getAllMilestonesByCriticalIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where critical in
        defaultMilestoneFiltering("critical.in=" + DEFAULT_CRITICAL + "," + UPDATED_CRITICAL, "critical.in=" + UPDATED_CRITICAL);
    }

    @Test
    @Transactional
    void getAllMilestonesByCriticalIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where critical is not null
        defaultMilestoneFiltering("critical.specified=true", "critical.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where state equals to
        defaultMilestoneFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllMilestonesByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where state in
        defaultMilestoneFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllMilestonesByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where state is not null
        defaultMilestoneFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByActualDurationIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where actualDuration equals to
        defaultMilestoneFiltering("actualDuration.equals=" + DEFAULT_ACTUAL_DURATION, "actualDuration.equals=" + UPDATED_ACTUAL_DURATION);
    }

    @Test
    @Transactional
    void getAllMilestonesByActualDurationIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where actualDuration in
        defaultMilestoneFiltering(
            "actualDuration.in=" + DEFAULT_ACTUAL_DURATION + "," + UPDATED_ACTUAL_DURATION,
            "actualDuration.in=" + UPDATED_ACTUAL_DURATION
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByActualDurationIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where actualDuration is not null
        defaultMilestoneFiltering("actualDuration.specified=true", "actualDuration.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByActualDurationIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where actualDuration is greater than or equal to
        defaultMilestoneFiltering(
            "actualDuration.greaterThanOrEqual=" + DEFAULT_ACTUAL_DURATION,
            "actualDuration.greaterThanOrEqual=" + UPDATED_ACTUAL_DURATION
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByActualDurationIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where actualDuration is less than or equal to
        defaultMilestoneFiltering(
            "actualDuration.lessThanOrEqual=" + DEFAULT_ACTUAL_DURATION,
            "actualDuration.lessThanOrEqual=" + SMALLER_ACTUAL_DURATION
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByActualDurationIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where actualDuration is less than
        defaultMilestoneFiltering(
            "actualDuration.lessThan=" + UPDATED_ACTUAL_DURATION,
            "actualDuration.lessThan=" + DEFAULT_ACTUAL_DURATION
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByActualDurationIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where actualDuration is greater than
        defaultMilestoneFiltering(
            "actualDuration.greaterThan=" + SMALLER_ACTUAL_DURATION,
            "actualDuration.greaterThan=" + DEFAULT_ACTUAL_DURATION
        );
    }

    @Test
    @Transactional
    void getAllMilestonesByReachedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where reachedAt equals to
        defaultMilestoneFiltering("reachedAt.equals=" + DEFAULT_REACHED_AT, "reachedAt.equals=" + UPDATED_REACHED_AT);
    }

    @Test
    @Transactional
    void getAllMilestonesByReachedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where reachedAt in
        defaultMilestoneFiltering("reachedAt.in=" + DEFAULT_REACHED_AT + "," + UPDATED_REACHED_AT, "reachedAt.in=" + UPDATED_REACHED_AT);
    }

    @Test
    @Transactional
    void getAllMilestonesByReachedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        // Get all the milestoneList where reachedAt is not null
        defaultMilestoneFiltering("reachedAt.specified=true", "reachedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllMilestonesByProjectIsEqualToSomething() throws Exception {
        Project project;
        if (TestUtil.findAll(em, Project.class).isEmpty()) {
            milestoneRepository.saveAndFlush(milestone);
            project = ProjectResourceIT.createEntity();
        } else {
            project = TestUtil.findAll(em, Project.class).get(0);
        }
        em.persist(project);
        em.flush();
        milestone.setProject(project);
        milestoneRepository.saveAndFlush(milestone);
        UUID projectId = project.getId();
        // Get all the milestoneList where project equals to projectId
        defaultMilestoneShouldBeFound("projectId.equals=" + projectId);

        // Get all the milestoneList where project equals to UUID.randomUUID()
        defaultMilestoneShouldNotBeFound("projectId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllMilestonesByPredecessorsIsEqualToSomething() throws Exception {
        Milestone predecessors;
        if (TestUtil.findAll(em, Milestone.class).isEmpty()) {
            milestoneRepository.saveAndFlush(milestone);
            predecessors = MilestoneResourceIT.createEntity(em);
        } else {
            predecessors = TestUtil.findAll(em, Milestone.class).get(0);
        }
        em.persist(predecessors);
        em.flush();
        milestone.addPredecessors(predecessors);
        milestoneRepository.saveAndFlush(milestone);
        UUID predecessorsId = predecessors.getId();
        // Get all the milestoneList where predecessors equals to predecessorsId
        defaultMilestoneShouldBeFound("predecessorsId.equals=" + predecessorsId);

        // Get all the milestoneList where predecessors equals to UUID.randomUUID()
        defaultMilestoneShouldNotBeFound("predecessorsId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllMilestonesBySuccessorsIsEqualToSomething() throws Exception {
        Milestone successors;
        if (TestUtil.findAll(em, Milestone.class).isEmpty()) {
            milestoneRepository.saveAndFlush(milestone);
            successors = MilestoneResourceIT.createEntity(em);
        } else {
            successors = TestUtil.findAll(em, Milestone.class).get(0);
        }
        em.persist(successors);
        em.flush();
        milestone.addSuccessors(successors);
        milestoneRepository.saveAndFlush(milestone);
        UUID successorsId = successors.getId();
        // Get all the milestoneList where successors equals to successorsId
        defaultMilestoneShouldBeFound("successorsId.equals=" + successorsId);

        // Get all the milestoneList where successors equals to UUID.randomUUID()
        defaultMilestoneShouldNotBeFound("successorsId.equals=" + UUID.randomUUID());
    }

    private void defaultMilestoneFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultMilestoneShouldBeFound(shouldBeFound);
        defaultMilestoneShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultMilestoneShouldBeFound(String filter) throws Exception {
        restMilestoneMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(milestone.getId().toString())))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].optimistic").value(hasItem(DEFAULT_OPTIMISTIC.toString())))
            .andExpect(jsonPath("$.[*].mostLikely").value(hasItem(DEFAULT_MOST_LIKELY.toString())))
            .andExpect(jsonPath("$.[*].pessimistic").value(hasItem(DEFAULT_PESSIMISTIC.toString())))
            .andExpect(jsonPath("$.[*].expectedDuration").value(hasItem(DEFAULT_EXPECTED_DURATION.toString())))
            .andExpect(jsonPath("$.[*].variance").value(hasItem(sameNumber(DEFAULT_VARIANCE))))
            .andExpect(jsonPath("$.[*].earliestStart").value(hasItem(DEFAULT_EARLIEST_START.toString())))
            .andExpect(jsonPath("$.[*].latestStart").value(hasItem(DEFAULT_LATEST_START.toString())))
            .andExpect(jsonPath("$.[*].totalFloat").value(hasItem(DEFAULT_TOTAL_FLOAT.toString())))
            .andExpect(jsonPath("$.[*].freeFloat").value(hasItem(DEFAULT_FREE_FLOAT.toString())))
            .andExpect(jsonPath("$.[*].critical").value(hasItem(DEFAULT_CRITICAL)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].actualDuration").value(hasItem(DEFAULT_ACTUAL_DURATION.toString())))
            .andExpect(jsonPath("$.[*].reachedAt").value(hasItem(DEFAULT_REACHED_AT.toString())));

        // Check, that the count call also returns 1
        restMilestoneMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultMilestoneShouldNotBeFound(String filter) throws Exception {
        restMilestoneMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restMilestoneMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingMilestone() throws Exception {
        // Get the milestone
        restMilestoneMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingMilestone() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the milestone
        Milestone updatedMilestone = milestoneRepository.findById(milestone.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedMilestone are not directly saved in db
        em.detach(updatedMilestone);
        updatedMilestone
            .label(UPDATED_LABEL)
            .optimistic(UPDATED_OPTIMISTIC)
            .mostLikely(UPDATED_MOST_LIKELY)
            .pessimistic(UPDATED_PESSIMISTIC)
            .expectedDuration(UPDATED_EXPECTED_DURATION)
            .variance(UPDATED_VARIANCE)
            .earliestStart(UPDATED_EARLIEST_START)
            .latestStart(UPDATED_LATEST_START)
            .totalFloat(UPDATED_TOTAL_FLOAT)
            .freeFloat(UPDATED_FREE_FLOAT)
            .critical(UPDATED_CRITICAL)
            .state(UPDATED_STATE)
            .actualDuration(UPDATED_ACTUAL_DURATION)
            .reachedAt(UPDATED_REACHED_AT);
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(updatedMilestone);

        restMilestoneMockMvc
            .perform(
                put(ENTITY_API_URL_ID, milestoneDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(milestoneDTO))
            )
            .andExpect(status().isOk());

        // Validate the Milestone in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedMilestoneToMatchAllProperties(updatedMilestone);
    }

    @Test
    @Transactional
    void putNonExistingMilestone() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestone.setId(UUID.randomUUID());

        // Create the Milestone
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMilestoneMockMvc
            .perform(
                put(ENTITY_API_URL_ID, milestoneDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(milestoneDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Milestone in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchMilestone() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestone.setId(UUID.randomUUID());

        // Create the Milestone
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMilestoneMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(milestoneDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Milestone in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamMilestone() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestone.setId(UUID.randomUUID());

        // Create the Milestone
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMilestoneMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(milestoneDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Milestone in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateMilestoneWithPatch() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the milestone using partial update
        Milestone partialUpdatedMilestone = new Milestone();
        partialUpdatedMilestone.setId(milestone.getId());

        partialUpdatedMilestone
            .label(UPDATED_LABEL)
            .optimistic(UPDATED_OPTIMISTIC)
            .mostLikely(UPDATED_MOST_LIKELY)
            .variance(UPDATED_VARIANCE)
            .critical(UPDATED_CRITICAL)
            .reachedAt(UPDATED_REACHED_AT);

        restMilestoneMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMilestone.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMilestone))
            )
            .andExpect(status().isOk());

        // Validate the Milestone in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMilestoneUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedMilestone, milestone),
            getPersistedMilestone(milestone)
        );
    }

    @Test
    @Transactional
    void fullUpdateMilestoneWithPatch() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the milestone using partial update
        Milestone partialUpdatedMilestone = new Milestone();
        partialUpdatedMilestone.setId(milestone.getId());

        partialUpdatedMilestone
            .label(UPDATED_LABEL)
            .optimistic(UPDATED_OPTIMISTIC)
            .mostLikely(UPDATED_MOST_LIKELY)
            .pessimistic(UPDATED_PESSIMISTIC)
            .expectedDuration(UPDATED_EXPECTED_DURATION)
            .variance(UPDATED_VARIANCE)
            .earliestStart(UPDATED_EARLIEST_START)
            .latestStart(UPDATED_LATEST_START)
            .totalFloat(UPDATED_TOTAL_FLOAT)
            .freeFloat(UPDATED_FREE_FLOAT)
            .critical(UPDATED_CRITICAL)
            .state(UPDATED_STATE)
            .actualDuration(UPDATED_ACTUAL_DURATION)
            .reachedAt(UPDATED_REACHED_AT);

        restMilestoneMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMilestone.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMilestone))
            )
            .andExpect(status().isOk());

        // Validate the Milestone in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMilestoneUpdatableFieldsEquals(partialUpdatedMilestone, getPersistedMilestone(partialUpdatedMilestone));
    }

    @Test
    @Transactional
    void patchNonExistingMilestone() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestone.setId(UUID.randomUUID());

        // Create the Milestone
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMilestoneMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, milestoneDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(milestoneDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Milestone in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchMilestone() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestone.setId(UUID.randomUUID());

        // Create the Milestone
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMilestoneMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(milestoneDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Milestone in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamMilestone() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestone.setId(UUID.randomUUID());

        // Create the Milestone
        MilestoneDTO milestoneDTO = milestoneMapper.toDto(milestone);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMilestoneMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(milestoneDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Milestone in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteMilestone() throws Exception {
        // Initialize the database
        insertedMilestone = milestoneRepository.saveAndFlush(milestone);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the milestone
        restMilestoneMockMvc
            .perform(delete(ENTITY_API_URL_ID, milestone.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return milestoneRepository.count();
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

    protected Milestone getPersistedMilestone(Milestone milestone) {
        return milestoneRepository.findById(milestone.getId()).orElseThrow();
    }

    protected void assertPersistedMilestoneToMatchAllProperties(Milestone expectedMilestone) {
        assertMilestoneAllPropertiesEquals(expectedMilestone, getPersistedMilestone(expectedMilestone));
    }

    protected void assertPersistedMilestoneToMatchUpdatableProperties(Milestone expectedMilestone) {
        assertMilestoneAllUpdatablePropertiesEquals(expectedMilestone, getPersistedMilestone(expectedMilestone));
    }
}
