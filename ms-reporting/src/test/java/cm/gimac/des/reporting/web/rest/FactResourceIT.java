package cm.gimac.des.reporting.web.rest;

import static cm.gimac.des.reporting.domain.FactAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.reporting.IntegrationTest;
import cm.gimac.des.reporting.domain.Fact;
import cm.gimac.des.reporting.domain.ImportedStatement;
import cm.gimac.des.reporting.domain.enumeration.FactDomain;
import cm.gimac.des.reporting.repository.FactRepository;
import cm.gimac.des.reporting.service.FactService;
import cm.gimac.des.reporting.service.mapper.FactMapper;
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
 * Integration tests for the {@link FactResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class FactResourceIT {

    private static final FactDomain DEFAULT_DOMAIN = FactDomain.INCIDENT;
    private static final FactDomain UPDATED_DOMAIN = FactDomain.MONITORING;

    private static final LocalDate DEFAULT_TIME_KEY = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_TIME_KEY = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_TIME_KEY = LocalDate.ofEpochDay(-1L);

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_SEVERITY = "AAAAAAAAAA";
    private static final String UPDATED_SEVERITY = "BBBBBBBBBB";

    private static final String DEFAULT_MEASURES = "AAAAAAAAAA";
    private static final String UPDATED_MEASURES = "BBBBBBBBBB";

    private static final UUID DEFAULT_SOURCE_EVENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_SOURCE_EVENT_ID = UUID.randomUUID();

    private static final Instant DEFAULT_RECORDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RECORDED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_SUPERSEDED = false;
    private static final Boolean UPDATED_SUPERSEDED = true;

    private static final Boolean DEFAULT_COMPACTED = false;
    private static final Boolean UPDATED_COMPACTED = true;

    private static final String ENTITY_API_URL = "/api/facts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private FactRepository factRepository;

    @Mock
    private FactRepository factRepositoryMock;

    @Autowired
    private FactMapper factMapper;

    @Mock
    private FactService factServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restFactMockMvc;

    private Fact fact;

    private Fact insertedFact;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Fact createEntity() {
        return new Fact()
            .domain(DEFAULT_DOMAIN)
            .timeKey(DEFAULT_TIME_KEY)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .serviceCode(DEFAULT_SERVICE_CODE)
            .severity(DEFAULT_SEVERITY)
            .measures(DEFAULT_MEASURES)
            .sourceEventId(DEFAULT_SOURCE_EVENT_ID)
            .recordedAt(DEFAULT_RECORDED_AT)
            .superseded(DEFAULT_SUPERSEDED)
            .compacted(DEFAULT_COMPACTED);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Fact createUpdatedEntity() {
        return new Fact()
            .domain(UPDATED_DOMAIN)
            .timeKey(UPDATED_TIME_KEY)
            .institutionId(UPDATED_INSTITUTION_ID)
            .serviceCode(UPDATED_SERVICE_CODE)
            .severity(UPDATED_SEVERITY)
            .measures(UPDATED_MEASURES)
            .sourceEventId(UPDATED_SOURCE_EVENT_ID)
            .recordedAt(UPDATED_RECORDED_AT)
            .superseded(UPDATED_SUPERSEDED)
            .compacted(UPDATED_COMPACTED);
    }

    @BeforeEach
    void initTest() {
        fact = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedFact != null) {
            factRepository.delete(insertedFact);
            insertedFact = null;
        }
    }

    @Test
    @Transactional
    void getAllFacts() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList
        restFactMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(fact.getId().toString())))
            .andExpect(jsonPath("$.[*].domain").value(hasItem(DEFAULT_DOMAIN.toString())))
            .andExpect(jsonPath("$.[*].timeKey").value(hasItem(DEFAULT_TIME_KEY.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY)))
            .andExpect(jsonPath("$.[*].measures").value(hasItem(DEFAULT_MEASURES)))
            .andExpect(jsonPath("$.[*].sourceEventId").value(hasItem(DEFAULT_SOURCE_EVENT_ID.toString())))
            .andExpect(jsonPath("$.[*].recordedAt").value(hasItem(DEFAULT_RECORDED_AT.toString())))
            .andExpect(jsonPath("$.[*].superseded").value(hasItem(DEFAULT_SUPERSEDED)))
            .andExpect(jsonPath("$.[*].compacted").value(hasItem(DEFAULT_COMPACTED)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllFactsWithEagerRelationshipsIsEnabled() throws Exception {
        when(factServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restFactMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(factServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllFactsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(factServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restFactMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(factRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getFact() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get the fact
        restFactMockMvc
            .perform(get(ENTITY_API_URL_ID, fact.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(fact.getId().toString()))
            .andExpect(jsonPath("$.domain").value(DEFAULT_DOMAIN.toString()))
            .andExpect(jsonPath("$.timeKey").value(DEFAULT_TIME_KEY.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.severity").value(DEFAULT_SEVERITY))
            .andExpect(jsonPath("$.measures").value(DEFAULT_MEASURES))
            .andExpect(jsonPath("$.sourceEventId").value(DEFAULT_SOURCE_EVENT_ID.toString()))
            .andExpect(jsonPath("$.recordedAt").value(DEFAULT_RECORDED_AT.toString()))
            .andExpect(jsonPath("$.superseded").value(DEFAULT_SUPERSEDED))
            .andExpect(jsonPath("$.compacted").value(DEFAULT_COMPACTED));
    }

    @Test
    @Transactional
    void getFactsByIdFiltering() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        UUID id = fact.getId();

        defaultFactFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllFactsByDomainIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where domain equals to
        defaultFactFiltering("domain.equals=" + DEFAULT_DOMAIN, "domain.equals=" + UPDATED_DOMAIN);
    }

    @Test
    @Transactional
    void getAllFactsByDomainIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where domain in
        defaultFactFiltering("domain.in=" + DEFAULT_DOMAIN + "," + UPDATED_DOMAIN, "domain.in=" + UPDATED_DOMAIN);
    }

    @Test
    @Transactional
    void getAllFactsByDomainIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where domain is not null
        defaultFactFiltering("domain.specified=true", "domain.specified=false");
    }

    @Test
    @Transactional
    void getAllFactsByTimeKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where timeKey equals to
        defaultFactFiltering("timeKey.equals=" + DEFAULT_TIME_KEY, "timeKey.equals=" + UPDATED_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllFactsByTimeKeyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where timeKey in
        defaultFactFiltering("timeKey.in=" + DEFAULT_TIME_KEY + "," + UPDATED_TIME_KEY, "timeKey.in=" + UPDATED_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllFactsByTimeKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where timeKey is not null
        defaultFactFiltering("timeKey.specified=true", "timeKey.specified=false");
    }

    @Test
    @Transactional
    void getAllFactsByTimeKeyIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where timeKey is greater than or equal to
        defaultFactFiltering("timeKey.greaterThanOrEqual=" + DEFAULT_TIME_KEY, "timeKey.greaterThanOrEqual=" + UPDATED_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllFactsByTimeKeyIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where timeKey is less than or equal to
        defaultFactFiltering("timeKey.lessThanOrEqual=" + DEFAULT_TIME_KEY, "timeKey.lessThanOrEqual=" + SMALLER_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllFactsByTimeKeyIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where timeKey is less than
        defaultFactFiltering("timeKey.lessThan=" + UPDATED_TIME_KEY, "timeKey.lessThan=" + DEFAULT_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllFactsByTimeKeyIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where timeKey is greater than
        defaultFactFiltering("timeKey.greaterThan=" + SMALLER_TIME_KEY, "timeKey.greaterThan=" + DEFAULT_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllFactsByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where institutionId equals to
        defaultFactFiltering("institutionId.equals=" + DEFAULT_INSTITUTION_ID, "institutionId.equals=" + UPDATED_INSTITUTION_ID);
    }

    @Test
    @Transactional
    void getAllFactsByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where institutionId in
        defaultFactFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllFactsByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where institutionId is not null
        defaultFactFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllFactsByServiceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where serviceCode equals to
        defaultFactFiltering("serviceCode.equals=" + DEFAULT_SERVICE_CODE, "serviceCode.equals=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllFactsByServiceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where serviceCode in
        defaultFactFiltering(
            "serviceCode.in=" + DEFAULT_SERVICE_CODE + "," + UPDATED_SERVICE_CODE,
            "serviceCode.in=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllFactsByServiceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where serviceCode is not null
        defaultFactFiltering("serviceCode.specified=true", "serviceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllFactsByServiceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where serviceCode contains
        defaultFactFiltering("serviceCode.contains=" + DEFAULT_SERVICE_CODE, "serviceCode.contains=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllFactsByServiceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where serviceCode does not contain
        defaultFactFiltering("serviceCode.doesNotContain=" + UPDATED_SERVICE_CODE, "serviceCode.doesNotContain=" + DEFAULT_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllFactsBySeverityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where severity equals to
        defaultFactFiltering("severity.equals=" + DEFAULT_SEVERITY, "severity.equals=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllFactsBySeverityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where severity in
        defaultFactFiltering("severity.in=" + DEFAULT_SEVERITY + "," + UPDATED_SEVERITY, "severity.in=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllFactsBySeverityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where severity is not null
        defaultFactFiltering("severity.specified=true", "severity.specified=false");
    }

    @Test
    @Transactional
    void getAllFactsBySeverityContainsSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where severity contains
        defaultFactFiltering("severity.contains=" + DEFAULT_SEVERITY, "severity.contains=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllFactsBySeverityNotContainsSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where severity does not contain
        defaultFactFiltering("severity.doesNotContain=" + UPDATED_SEVERITY, "severity.doesNotContain=" + DEFAULT_SEVERITY);
    }

    @Test
    @Transactional
    void getAllFactsBySourceEventIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where sourceEventId equals to
        defaultFactFiltering("sourceEventId.equals=" + DEFAULT_SOURCE_EVENT_ID, "sourceEventId.equals=" + UPDATED_SOURCE_EVENT_ID);
    }

    @Test
    @Transactional
    void getAllFactsBySourceEventIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where sourceEventId in
        defaultFactFiltering(
            "sourceEventId.in=" + DEFAULT_SOURCE_EVENT_ID + "," + UPDATED_SOURCE_EVENT_ID,
            "sourceEventId.in=" + UPDATED_SOURCE_EVENT_ID
        );
    }

    @Test
    @Transactional
    void getAllFactsBySourceEventIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where sourceEventId is not null
        defaultFactFiltering("sourceEventId.specified=true", "sourceEventId.specified=false");
    }

    @Test
    @Transactional
    void getAllFactsByRecordedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where recordedAt equals to
        defaultFactFiltering("recordedAt.equals=" + DEFAULT_RECORDED_AT, "recordedAt.equals=" + UPDATED_RECORDED_AT);
    }

    @Test
    @Transactional
    void getAllFactsByRecordedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where recordedAt in
        defaultFactFiltering("recordedAt.in=" + DEFAULT_RECORDED_AT + "," + UPDATED_RECORDED_AT, "recordedAt.in=" + UPDATED_RECORDED_AT);
    }

    @Test
    @Transactional
    void getAllFactsByRecordedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where recordedAt is not null
        defaultFactFiltering("recordedAt.specified=true", "recordedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllFactsBySupersededIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where superseded equals to
        defaultFactFiltering("superseded.equals=" + DEFAULT_SUPERSEDED, "superseded.equals=" + UPDATED_SUPERSEDED);
    }

    @Test
    @Transactional
    void getAllFactsBySupersededIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where superseded in
        defaultFactFiltering("superseded.in=" + DEFAULT_SUPERSEDED + "," + UPDATED_SUPERSEDED, "superseded.in=" + UPDATED_SUPERSEDED);
    }

    @Test
    @Transactional
    void getAllFactsBySupersededIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where superseded is not null
        defaultFactFiltering("superseded.specified=true", "superseded.specified=false");
    }

    @Test
    @Transactional
    void getAllFactsByCompactedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where compacted equals to
        defaultFactFiltering("compacted.equals=" + DEFAULT_COMPACTED, "compacted.equals=" + UPDATED_COMPACTED);
    }

    @Test
    @Transactional
    void getAllFactsByCompactedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where compacted in
        defaultFactFiltering("compacted.in=" + DEFAULT_COMPACTED + "," + UPDATED_COMPACTED, "compacted.in=" + UPDATED_COMPACTED);
    }

    @Test
    @Transactional
    void getAllFactsByCompactedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFact = factRepository.saveAndFlush(fact);

        // Get all the factList where compacted is not null
        defaultFactFiltering("compacted.specified=true", "compacted.specified=false");
    }

    @Test
    @Transactional
    void getAllFactsByStatementIsEqualToSomething() throws Exception {
        ImportedStatement statement;
        if (TestUtil.findAll(em, ImportedStatement.class).isEmpty()) {
            factRepository.saveAndFlush(fact);
            statement = ImportedStatementResourceIT.createEntity();
        } else {
            statement = TestUtil.findAll(em, ImportedStatement.class).get(0);
        }
        em.persist(statement);
        em.flush();
        fact.setStatement(statement);
        factRepository.saveAndFlush(fact);
        UUID statementId = statement.getId();
        // Get all the factList where statement equals to statementId
        defaultFactShouldBeFound("statementId.equals=" + statementId);

        // Get all the factList where statement equals to UUID.randomUUID()
        defaultFactShouldNotBeFound("statementId.equals=" + UUID.randomUUID());
    }

    private void defaultFactFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultFactShouldBeFound(shouldBeFound);
        defaultFactShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultFactShouldBeFound(String filter) throws Exception {
        restFactMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(fact.getId().toString())))
            .andExpect(jsonPath("$.[*].domain").value(hasItem(DEFAULT_DOMAIN.toString())))
            .andExpect(jsonPath("$.[*].timeKey").value(hasItem(DEFAULT_TIME_KEY.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY)))
            .andExpect(jsonPath("$.[*].measures").value(hasItem(DEFAULT_MEASURES)))
            .andExpect(jsonPath("$.[*].sourceEventId").value(hasItem(DEFAULT_SOURCE_EVENT_ID.toString())))
            .andExpect(jsonPath("$.[*].recordedAt").value(hasItem(DEFAULT_RECORDED_AT.toString())))
            .andExpect(jsonPath("$.[*].superseded").value(hasItem(DEFAULT_SUPERSEDED)))
            .andExpect(jsonPath("$.[*].compacted").value(hasItem(DEFAULT_COMPACTED)));

        // Check, that the count call also returns 1
        restFactMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultFactShouldNotBeFound(String filter) throws Exception {
        restFactMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restFactMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingFact() throws Exception {
        // Get the fact
        restFactMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return factRepository.count();
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

    protected Fact getPersistedFact(Fact fact) {
        return factRepository.findById(fact.getId()).orElseThrow();
    }

    protected void assertPersistedFactToMatchAllProperties(Fact expectedFact) {
        assertFactAllPropertiesEquals(expectedFact, getPersistedFact(expectedFact));
    }

    protected void assertPersistedFactToMatchUpdatableProperties(Fact expectedFact) {
        assertFactAllUpdatablePropertiesEquals(expectedFact, getPersistedFact(expectedFact));
    }
}
