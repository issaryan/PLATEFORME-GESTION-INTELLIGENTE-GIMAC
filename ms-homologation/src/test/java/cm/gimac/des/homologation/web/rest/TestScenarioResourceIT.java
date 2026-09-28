package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.TestScenarioAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.Operation;
import cm.gimac.des.homologation.domain.TestScenario;
import cm.gimac.des.homologation.domain.enumeration.ScenarioMatch;
import cm.gimac.des.homologation.domain.enumeration.ScenarioOutcome;
import cm.gimac.des.homologation.repository.TestScenarioRepository;
import cm.gimac.des.homologation.service.TestScenarioService;
import cm.gimac.des.homologation.service.mapper.TestScenarioMapper;
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
 * Integration tests for the {@link TestScenarioResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class TestScenarioResourceIT {

    private static final String DEFAULT_SIMULATOR_SCENARIO_ID = "AAAAAAAAAA";
    private static final String UPDATED_SIMULATOR_SCENARIO_ID = "BBBBBBBBBB";

    private static final ScenarioOutcome DEFAULT_OUTCOME = ScenarioOutcome.SUCCES;
    private static final ScenarioOutcome UPDATED_OUTCOME = ScenarioOutcome.ECHEC;

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final Instant DEFAULT_EXECUTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXECUTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_TRACE_REF = "AAAAAAAAAA";
    private static final String UPDATED_TRACE_REF = "BBBBBBBBBB";

    private static final Instant DEFAULT_REPLAYED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REPLAYED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final ScenarioMatch DEFAULT_MATCH = ScenarioMatch.RAPPROCHE;
    private static final ScenarioMatch UPDATED_MATCH = ScenarioMatch.EN_ATTENTE_RAPPROCHEMENT;

    private static final String ENTITY_API_URL = "/api/test-scenarios";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TestScenarioRepository testScenarioRepository;

    @Mock
    private TestScenarioRepository testScenarioRepositoryMock;

    @Autowired
    private TestScenarioMapper testScenarioMapper;

    @Mock
    private TestScenarioService testScenarioServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTestScenarioMockMvc;

    private TestScenario testScenario;

    private TestScenario insertedTestScenario;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TestScenario createEntity(EntityManager em) {
        TestScenario testScenario = new TestScenario()
            .simulatorScenarioId(DEFAULT_SIMULATOR_SCENARIO_ID)
            .outcome(DEFAULT_OUTCOME)
            .reason(DEFAULT_REASON)
            .executedAt(DEFAULT_EXECUTED_AT)
            .traceRef(DEFAULT_TRACE_REF)
            .replayedAt(DEFAULT_REPLAYED_AT)
            .match(DEFAULT_MATCH);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        testScenario.setLiaison(liaison);
        return testScenario;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TestScenario createUpdatedEntity(EntityManager em) {
        TestScenario updatedTestScenario = new TestScenario()
            .simulatorScenarioId(UPDATED_SIMULATOR_SCENARIO_ID)
            .outcome(UPDATED_OUTCOME)
            .reason(UPDATED_REASON)
            .executedAt(UPDATED_EXECUTED_AT)
            .traceRef(UPDATED_TRACE_REF)
            .replayedAt(UPDATED_REPLAYED_AT)
            .match(UPDATED_MATCH);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        updatedTestScenario.setLiaison(liaison);
        return updatedTestScenario;
    }

    @BeforeEach
    void initTest() {
        testScenario = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedTestScenario != null) {
            testScenarioRepository.delete(insertedTestScenario);
            insertedTestScenario = null;
        }
    }

    @Test
    @Transactional
    void getAllTestScenarios() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList
        restTestScenarioMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(testScenario.getId().toString())))
            .andExpect(jsonPath("$.[*].simulatorScenarioId").value(hasItem(DEFAULT_SIMULATOR_SCENARIO_ID)))
            .andExpect(jsonPath("$.[*].outcome").value(hasItem(DEFAULT_OUTCOME.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].executedAt").value(hasItem(DEFAULT_EXECUTED_AT.toString())))
            .andExpect(jsonPath("$.[*].traceRef").value(hasItem(DEFAULT_TRACE_REF)))
            .andExpect(jsonPath("$.[*].replayedAt").value(hasItem(DEFAULT_REPLAYED_AT.toString())))
            .andExpect(jsonPath("$.[*].match").value(hasItem(DEFAULT_MATCH.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTestScenariosWithEagerRelationshipsIsEnabled() throws Exception {
        when(testScenarioServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTestScenarioMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(testScenarioServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTestScenariosWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(testScenarioServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTestScenarioMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(testScenarioRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getTestScenario() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get the testScenario
        restTestScenarioMockMvc
            .perform(get(ENTITY_API_URL_ID, testScenario.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(testScenario.getId().toString()))
            .andExpect(jsonPath("$.simulatorScenarioId").value(DEFAULT_SIMULATOR_SCENARIO_ID))
            .andExpect(jsonPath("$.outcome").value(DEFAULT_OUTCOME.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.executedAt").value(DEFAULT_EXECUTED_AT.toString()))
            .andExpect(jsonPath("$.traceRef").value(DEFAULT_TRACE_REF))
            .andExpect(jsonPath("$.replayedAt").value(DEFAULT_REPLAYED_AT.toString()))
            .andExpect(jsonPath("$.match").value(DEFAULT_MATCH.toString()));
    }

    @Test
    @Transactional
    void getTestScenariosByIdFiltering() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        UUID id = testScenario.getId();

        defaultTestScenarioFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllTestScenariosBySimulatorScenarioIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where simulatorScenarioId equals to
        defaultTestScenarioFiltering(
            "simulatorScenarioId.equals=" + DEFAULT_SIMULATOR_SCENARIO_ID,
            "simulatorScenarioId.equals=" + UPDATED_SIMULATOR_SCENARIO_ID
        );
    }

    @Test
    @Transactional
    void getAllTestScenariosBySimulatorScenarioIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where simulatorScenarioId in
        defaultTestScenarioFiltering(
            "simulatorScenarioId.in=" + DEFAULT_SIMULATOR_SCENARIO_ID + "," + UPDATED_SIMULATOR_SCENARIO_ID,
            "simulatorScenarioId.in=" + UPDATED_SIMULATOR_SCENARIO_ID
        );
    }

    @Test
    @Transactional
    void getAllTestScenariosBySimulatorScenarioIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where simulatorScenarioId is not null
        defaultTestScenarioFiltering("simulatorScenarioId.specified=true", "simulatorScenarioId.specified=false");
    }

    @Test
    @Transactional
    void getAllTestScenariosBySimulatorScenarioIdContainsSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where simulatorScenarioId contains
        defaultTestScenarioFiltering(
            "simulatorScenarioId.contains=" + DEFAULT_SIMULATOR_SCENARIO_ID,
            "simulatorScenarioId.contains=" + UPDATED_SIMULATOR_SCENARIO_ID
        );
    }

    @Test
    @Transactional
    void getAllTestScenariosBySimulatorScenarioIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where simulatorScenarioId does not contain
        defaultTestScenarioFiltering(
            "simulatorScenarioId.doesNotContain=" + UPDATED_SIMULATOR_SCENARIO_ID,
            "simulatorScenarioId.doesNotContain=" + DEFAULT_SIMULATOR_SCENARIO_ID
        );
    }

    @Test
    @Transactional
    void getAllTestScenariosByOutcomeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where outcome equals to
        defaultTestScenarioFiltering("outcome.equals=" + DEFAULT_OUTCOME, "outcome.equals=" + UPDATED_OUTCOME);
    }

    @Test
    @Transactional
    void getAllTestScenariosByOutcomeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where outcome in
        defaultTestScenarioFiltering("outcome.in=" + DEFAULT_OUTCOME + "," + UPDATED_OUTCOME, "outcome.in=" + UPDATED_OUTCOME);
    }

    @Test
    @Transactional
    void getAllTestScenariosByOutcomeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where outcome is not null
        defaultTestScenarioFiltering("outcome.specified=true", "outcome.specified=false");
    }

    @Test
    @Transactional
    void getAllTestScenariosByReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where reason equals to
        defaultTestScenarioFiltering("reason.equals=" + DEFAULT_REASON, "reason.equals=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllTestScenariosByReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where reason in
        defaultTestScenarioFiltering("reason.in=" + DEFAULT_REASON + "," + UPDATED_REASON, "reason.in=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllTestScenariosByReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where reason is not null
        defaultTestScenarioFiltering("reason.specified=true", "reason.specified=false");
    }

    @Test
    @Transactional
    void getAllTestScenariosByReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where reason contains
        defaultTestScenarioFiltering("reason.contains=" + DEFAULT_REASON, "reason.contains=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllTestScenariosByReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where reason does not contain
        defaultTestScenarioFiltering("reason.doesNotContain=" + UPDATED_REASON, "reason.doesNotContain=" + DEFAULT_REASON);
    }

    @Test
    @Transactional
    void getAllTestScenariosByExecutedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where executedAt equals to
        defaultTestScenarioFiltering("executedAt.equals=" + DEFAULT_EXECUTED_AT, "executedAt.equals=" + UPDATED_EXECUTED_AT);
    }

    @Test
    @Transactional
    void getAllTestScenariosByExecutedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where executedAt in
        defaultTestScenarioFiltering(
            "executedAt.in=" + DEFAULT_EXECUTED_AT + "," + UPDATED_EXECUTED_AT,
            "executedAt.in=" + UPDATED_EXECUTED_AT
        );
    }

    @Test
    @Transactional
    void getAllTestScenariosByExecutedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where executedAt is not null
        defaultTestScenarioFiltering("executedAt.specified=true", "executedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllTestScenariosByTraceRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where traceRef equals to
        defaultTestScenarioFiltering("traceRef.equals=" + DEFAULT_TRACE_REF, "traceRef.equals=" + UPDATED_TRACE_REF);
    }

    @Test
    @Transactional
    void getAllTestScenariosByTraceRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where traceRef in
        defaultTestScenarioFiltering("traceRef.in=" + DEFAULT_TRACE_REF + "," + UPDATED_TRACE_REF, "traceRef.in=" + UPDATED_TRACE_REF);
    }

    @Test
    @Transactional
    void getAllTestScenariosByTraceRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where traceRef is not null
        defaultTestScenarioFiltering("traceRef.specified=true", "traceRef.specified=false");
    }

    @Test
    @Transactional
    void getAllTestScenariosByTraceRefContainsSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where traceRef contains
        defaultTestScenarioFiltering("traceRef.contains=" + DEFAULT_TRACE_REF, "traceRef.contains=" + UPDATED_TRACE_REF);
    }

    @Test
    @Transactional
    void getAllTestScenariosByTraceRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where traceRef does not contain
        defaultTestScenarioFiltering("traceRef.doesNotContain=" + UPDATED_TRACE_REF, "traceRef.doesNotContain=" + DEFAULT_TRACE_REF);
    }

    @Test
    @Transactional
    void getAllTestScenariosByReplayedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where replayedAt equals to
        defaultTestScenarioFiltering("replayedAt.equals=" + DEFAULT_REPLAYED_AT, "replayedAt.equals=" + UPDATED_REPLAYED_AT);
    }

    @Test
    @Transactional
    void getAllTestScenariosByReplayedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where replayedAt in
        defaultTestScenarioFiltering(
            "replayedAt.in=" + DEFAULT_REPLAYED_AT + "," + UPDATED_REPLAYED_AT,
            "replayedAt.in=" + UPDATED_REPLAYED_AT
        );
    }

    @Test
    @Transactional
    void getAllTestScenariosByReplayedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where replayedAt is not null
        defaultTestScenarioFiltering("replayedAt.specified=true", "replayedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllTestScenariosByMatchIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where match equals to
        defaultTestScenarioFiltering("match.equals=" + DEFAULT_MATCH, "match.equals=" + UPDATED_MATCH);
    }

    @Test
    @Transactional
    void getAllTestScenariosByMatchIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where match in
        defaultTestScenarioFiltering("match.in=" + DEFAULT_MATCH + "," + UPDATED_MATCH, "match.in=" + UPDATED_MATCH);
    }

    @Test
    @Transactional
    void getAllTestScenariosByMatchIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTestScenario = testScenarioRepository.saveAndFlush(testScenario);

        // Get all the testScenarioList where match is not null
        defaultTestScenarioFiltering("match.specified=true", "match.specified=false");
    }

    @Test
    @Transactional
    void getAllTestScenariosByLiaisonIsEqualToSomething() throws Exception {
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            testScenarioRepository.saveAndFlush(testScenario);
            liaison = LiaisonResourceIT.createEntity(em);
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        em.persist(liaison);
        em.flush();
        testScenario.setLiaison(liaison);
        testScenarioRepository.saveAndFlush(testScenario);
        UUID liaisonId = liaison.getId();
        // Get all the testScenarioList where liaison equals to liaisonId
        defaultTestScenarioShouldBeFound("liaisonId.equals=" + liaisonId);

        // Get all the testScenarioList where liaison equals to UUID.randomUUID()
        defaultTestScenarioShouldNotBeFound("liaisonId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllTestScenariosByOperationIsEqualToSomething() throws Exception {
        Operation operation;
        if (TestUtil.findAll(em, Operation.class).isEmpty()) {
            testScenarioRepository.saveAndFlush(testScenario);
            operation = OperationResourceIT.createEntity(em);
        } else {
            operation = TestUtil.findAll(em, Operation.class).get(0);
        }
        em.persist(operation);
        em.flush();
        testScenario.setOperation(operation);
        testScenarioRepository.saveAndFlush(testScenario);
        UUID operationId = operation.getId();
        // Get all the testScenarioList where operation equals to operationId
        defaultTestScenarioShouldBeFound("operationId.equals=" + operationId);

        // Get all the testScenarioList where operation equals to UUID.randomUUID()
        defaultTestScenarioShouldNotBeFound("operationId.equals=" + UUID.randomUUID());
    }

    private void defaultTestScenarioFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultTestScenarioShouldBeFound(shouldBeFound);
        defaultTestScenarioShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultTestScenarioShouldBeFound(String filter) throws Exception {
        restTestScenarioMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(testScenario.getId().toString())))
            .andExpect(jsonPath("$.[*].simulatorScenarioId").value(hasItem(DEFAULT_SIMULATOR_SCENARIO_ID)))
            .andExpect(jsonPath("$.[*].outcome").value(hasItem(DEFAULT_OUTCOME.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].executedAt").value(hasItem(DEFAULT_EXECUTED_AT.toString())))
            .andExpect(jsonPath("$.[*].traceRef").value(hasItem(DEFAULT_TRACE_REF)))
            .andExpect(jsonPath("$.[*].replayedAt").value(hasItem(DEFAULT_REPLAYED_AT.toString())))
            .andExpect(jsonPath("$.[*].match").value(hasItem(DEFAULT_MATCH.toString())));

        // Check, that the count call also returns 1
        restTestScenarioMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultTestScenarioShouldNotBeFound(String filter) throws Exception {
        restTestScenarioMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restTestScenarioMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingTestScenario() throws Exception {
        // Get the testScenario
        restTestScenarioMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return testScenarioRepository.count();
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

    protected TestScenario getPersistedTestScenario(TestScenario testScenario) {
        return testScenarioRepository.findById(testScenario.getId()).orElseThrow();
    }

    protected void assertPersistedTestScenarioToMatchAllProperties(TestScenario expectedTestScenario) {
        assertTestScenarioAllPropertiesEquals(expectedTestScenario, getPersistedTestScenario(expectedTestScenario));
    }

    protected void assertPersistedTestScenarioToMatchUpdatableProperties(TestScenario expectedTestScenario) {
        assertTestScenarioAllUpdatablePropertiesEquals(expectedTestScenario, getPersistedTestScenario(expectedTestScenario));
    }
}
