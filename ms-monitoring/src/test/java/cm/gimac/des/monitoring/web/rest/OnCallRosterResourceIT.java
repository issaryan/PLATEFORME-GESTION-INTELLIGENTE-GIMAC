package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.OnCallRosterAsserts.*;
import static cm.gimac.des.monitoring.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.domain.enumeration.RosterState;
import cm.gimac.des.monitoring.domain.enumeration.SolverStatus;
import cm.gimac.des.monitoring.repository.OnCallRosterRepository;
import cm.gimac.des.monitoring.service.OnCallRosterService;
import cm.gimac.des.monitoring.service.mapper.OnCallRosterMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
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
 * Integration tests for the {@link OnCallRosterResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class OnCallRosterResourceIT {

    private static final String DEFAULT_ISO_WEEK = "0969-S95";
    private static final String UPDATED_ISO_WEEK = "4075-S50";

    private static final Integer DEFAULT_REVISION = 1;
    private static final Integer UPDATED_REVISION = 2;

    private static final RosterState DEFAULT_STATE = RosterState.GENEREE;
    private static final RosterState UPDATED_STATE = RosterState.VALIDEE;

    private static final SolverStatus DEFAULT_SOLVER_STATUS = SolverStatus.OPTIMALE;
    private static final SolverStatus UPDATED_SOLVER_STATUS = SolverStatus.REALISABLE;

    private static final BigDecimal DEFAULT_OBJECTIVE_VALUE = new BigDecimal(1);
    private static final BigDecimal UPDATED_OBJECTIVE_VALUE = new BigDecimal(2);

    private static final BigDecimal DEFAULT_GAP = new BigDecimal(0);
    private static final BigDecimal UPDATED_GAP = new BigDecimal(1);

    private static final Duration DEFAULT_SOLVE_DURATION = Duration.ofHours(6);
    private static final Duration UPDATED_SOLVE_DURATION = Duration.ofHours(12);

    private static final String DEFAULT_UNCOVERED_SLOTS = "AAAAAAAAAA";
    private static final String UPDATED_UNCOVERED_SLOTS = "BBBBBBBBBB";

    private static final Instant DEFAULT_GENERATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_GENERATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_GENERATED_BY = UUID.randomUUID();
    private static final UUID UPDATED_GENERATED_BY = UUID.randomUUID();

    private static final UUID DEFAULT_VALIDATED_BY = UUID.randomUUID();
    private static final UUID UPDATED_VALIDATED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_VALIDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_VALIDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_PUBLISHED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PUBLISHED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/on-call-rosters";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OnCallRosterRepository onCallRosterRepository;

    @Mock
    private OnCallRosterRepository onCallRosterRepositoryMock;

    @Autowired
    private OnCallRosterMapper onCallRosterMapper;

    @Mock
    private OnCallRosterService onCallRosterServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOnCallRosterMockMvc;

    private OnCallRoster onCallRoster;

    private OnCallRoster insertedOnCallRoster;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OnCallRoster createEntity() {
        return new OnCallRoster()
            .isoWeek(DEFAULT_ISO_WEEK)
            .revision(DEFAULT_REVISION)
            .state(DEFAULT_STATE)
            .solverStatus(DEFAULT_SOLVER_STATUS)
            .objectiveValue(DEFAULT_OBJECTIVE_VALUE)
            .gap(DEFAULT_GAP)
            .solveDuration(DEFAULT_SOLVE_DURATION)
            .uncoveredSlots(DEFAULT_UNCOVERED_SLOTS)
            .generatedAt(DEFAULT_GENERATED_AT)
            .generatedBy(DEFAULT_GENERATED_BY)
            .validatedBy(DEFAULT_VALIDATED_BY)
            .validatedAt(DEFAULT_VALIDATED_AT)
            .publishedAt(DEFAULT_PUBLISHED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OnCallRoster createUpdatedEntity() {
        return new OnCallRoster()
            .isoWeek(UPDATED_ISO_WEEK)
            .revision(UPDATED_REVISION)
            .state(UPDATED_STATE)
            .solverStatus(UPDATED_SOLVER_STATUS)
            .objectiveValue(UPDATED_OBJECTIVE_VALUE)
            .gap(UPDATED_GAP)
            .solveDuration(UPDATED_SOLVE_DURATION)
            .uncoveredSlots(UPDATED_UNCOVERED_SLOTS)
            .generatedAt(UPDATED_GENERATED_AT)
            .generatedBy(UPDATED_GENERATED_BY)
            .validatedBy(UPDATED_VALIDATED_BY)
            .validatedAt(UPDATED_VALIDATED_AT)
            .publishedAt(UPDATED_PUBLISHED_AT);
    }

    @BeforeEach
    void initTest() {
        onCallRoster = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedOnCallRoster != null) {
            onCallRosterRepository.delete(insertedOnCallRoster);
            insertedOnCallRoster = null;
        }
    }

    @Test
    @Transactional
    void getAllOnCallRosters() throws Exception {
        // Initialize the database
        insertedOnCallRoster = onCallRosterRepository.saveAndFlush(onCallRoster);

        // Get all the onCallRosterList
        restOnCallRosterMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(onCallRoster.getId().toString())))
            .andExpect(jsonPath("$.[*].isoWeek").value(hasItem(DEFAULT_ISO_WEEK)))
            .andExpect(jsonPath("$.[*].revision").value(hasItem(DEFAULT_REVISION)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].solverStatus").value(hasItem(DEFAULT_SOLVER_STATUS.toString())))
            .andExpect(jsonPath("$.[*].objectiveValue").value(hasItem(sameNumber(DEFAULT_OBJECTIVE_VALUE))))
            .andExpect(jsonPath("$.[*].gap").value(hasItem(sameNumber(DEFAULT_GAP))))
            .andExpect(jsonPath("$.[*].solveDuration").value(hasItem(DEFAULT_SOLVE_DURATION.toString())))
            .andExpect(jsonPath("$.[*].uncoveredSlots").value(hasItem(DEFAULT_UNCOVERED_SLOTS)))
            .andExpect(jsonPath("$.[*].generatedAt").value(hasItem(DEFAULT_GENERATED_AT.toString())))
            .andExpect(jsonPath("$.[*].generatedBy").value(hasItem(DEFAULT_GENERATED_BY.toString())))
            .andExpect(jsonPath("$.[*].validatedBy").value(hasItem(DEFAULT_VALIDATED_BY.toString())))
            .andExpect(jsonPath("$.[*].validatedAt").value(hasItem(DEFAULT_VALIDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].publishedAt").value(hasItem(DEFAULT_PUBLISHED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOnCallRostersWithEagerRelationshipsIsEnabled() throws Exception {
        when(onCallRosterServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOnCallRosterMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(onCallRosterServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOnCallRostersWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(onCallRosterServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOnCallRosterMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(onCallRosterRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOnCallRoster() throws Exception {
        // Initialize the database
        insertedOnCallRoster = onCallRosterRepository.saveAndFlush(onCallRoster);

        // Get the onCallRoster
        restOnCallRosterMockMvc
            .perform(get(ENTITY_API_URL_ID, onCallRoster.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(onCallRoster.getId().toString()))
            .andExpect(jsonPath("$.isoWeek").value(DEFAULT_ISO_WEEK))
            .andExpect(jsonPath("$.revision").value(DEFAULT_REVISION))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.solverStatus").value(DEFAULT_SOLVER_STATUS.toString()))
            .andExpect(jsonPath("$.objectiveValue").value(sameNumber(DEFAULT_OBJECTIVE_VALUE)))
            .andExpect(jsonPath("$.gap").value(sameNumber(DEFAULT_GAP)))
            .andExpect(jsonPath("$.solveDuration").value(DEFAULT_SOLVE_DURATION.toString()))
            .andExpect(jsonPath("$.uncoveredSlots").value(DEFAULT_UNCOVERED_SLOTS))
            .andExpect(jsonPath("$.generatedAt").value(DEFAULT_GENERATED_AT.toString()))
            .andExpect(jsonPath("$.generatedBy").value(DEFAULT_GENERATED_BY.toString()))
            .andExpect(jsonPath("$.validatedBy").value(DEFAULT_VALIDATED_BY.toString()))
            .andExpect(jsonPath("$.validatedAt").value(DEFAULT_VALIDATED_AT.toString()))
            .andExpect(jsonPath("$.publishedAt").value(DEFAULT_PUBLISHED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingOnCallRoster() throws Exception {
        // Get the onCallRoster
        restOnCallRosterMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return onCallRosterRepository.count();
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

    protected OnCallRoster getPersistedOnCallRoster(OnCallRoster onCallRoster) {
        return onCallRosterRepository.findById(onCallRoster.getId()).orElseThrow();
    }

    protected void assertPersistedOnCallRosterToMatchAllProperties(OnCallRoster expectedOnCallRoster) {
        assertOnCallRosterAllPropertiesEquals(expectedOnCallRoster, getPersistedOnCallRoster(expectedOnCallRoster));
    }

    protected void assertPersistedOnCallRosterToMatchUpdatableProperties(OnCallRoster expectedOnCallRoster) {
        assertOnCallRosterAllUpdatablePropertiesEquals(expectedOnCallRoster, getPersistedOnCallRoster(expectedOnCallRoster));
    }
}
