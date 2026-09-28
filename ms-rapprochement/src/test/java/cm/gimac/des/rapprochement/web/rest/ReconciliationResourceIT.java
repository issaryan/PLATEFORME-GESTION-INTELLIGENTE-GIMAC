package cm.gimac.des.rapprochement.web.rest;

import static cm.gimac.des.rapprochement.domain.ReconciliationAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.rapprochement.IntegrationTest;
import cm.gimac.des.rapprochement.domain.ImportedStatement;
import cm.gimac.des.rapprochement.domain.Reconciliation;
import cm.gimac.des.rapprochement.domain.enumeration.ReconciliationState;
import cm.gimac.des.rapprochement.repository.ReconciliationRepository;
import cm.gimac.des.rapprochement.service.ReconciliationService;
import cm.gimac.des.rapprochement.service.mapper.ReconciliationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link ReconciliationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ReconciliationResourceIT {

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_BUSINESS_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_BUSINESS_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_BUSINESS_DATE = LocalDate.ofEpochDay(-1L);

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final ReconciliationState DEFAULT_STATE = ReconciliationState.EN_ATTENTE_ETATS;
    private static final ReconciliationState UPDATED_STATE = ReconciliationState.EN_COURS;

    private static final Duration DEFAULT_TIMESTAMP_TOLERANCE = Duration.ofHours(6);
    private static final Duration UPDATED_TIMESTAMP_TOLERANCE = Duration.ofHours(12);
    private static final Duration SMALLER_TIMESTAMP_TOLERANCE = Duration.ofHours(5);

    private static final Integer DEFAULT_MATCHED_COUNT = 0;
    private static final Integer UPDATED_MATCHED_COUNT = 1;
    private static final Integer SMALLER_MATCHED_COUNT = 0 - 1;

    private static final Integer DEFAULT_DISCREPANCY_COUNT = 0;
    private static final Integer UPDATED_DISCREPANCY_COUNT = 1;
    private static final Integer SMALLER_DISCREPANCY_COUNT = 0 - 1;

    private static final String DEFAULT_DISCREPANCY_COUNTS_BY_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_DISCREPANCY_COUNTS_BY_TYPE = "BBBBBBBBBB";

    private static final Instant DEFAULT_STARTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_COMPLETED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_COMPLETED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_CLOSED_BY = UUID.randomUUID();
    private static final UUID UPDATED_CLOSED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_CLOSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLOSED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/reconciliations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReconciliationRepository reconciliationRepository;

    @Mock
    private ReconciliationRepository reconciliationRepositoryMock;

    @Autowired
    private ReconciliationMapper reconciliationMapper;

    @Mock
    private ReconciliationService reconciliationServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReconciliationMockMvc;

    private Reconciliation reconciliation;

    private Reconciliation insertedReconciliation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Reconciliation createEntity() {
        return new Reconciliation()
            .reference(DEFAULT_REFERENCE)
            .businessDate(DEFAULT_BUSINESS_DATE)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .state(DEFAULT_STATE)
            .timestampTolerance(DEFAULT_TIMESTAMP_TOLERANCE)
            .matchedCount(DEFAULT_MATCHED_COUNT)
            .discrepancyCount(DEFAULT_DISCREPANCY_COUNT)
            .discrepancyCountsByType(DEFAULT_DISCREPANCY_COUNTS_BY_TYPE)
            .startedAt(DEFAULT_STARTED_AT)
            .completedAt(DEFAULT_COMPLETED_AT)
            .closedBy(DEFAULT_CLOSED_BY)
            .closedAt(DEFAULT_CLOSED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Reconciliation createUpdatedEntity() {
        return new Reconciliation()
            .reference(UPDATED_REFERENCE)
            .businessDate(UPDATED_BUSINESS_DATE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .state(UPDATED_STATE)
            .timestampTolerance(UPDATED_TIMESTAMP_TOLERANCE)
            .matchedCount(UPDATED_MATCHED_COUNT)
            .discrepancyCount(UPDATED_DISCREPANCY_COUNT)
            .discrepancyCountsByType(UPDATED_DISCREPANCY_COUNTS_BY_TYPE)
            .startedAt(UPDATED_STARTED_AT)
            .completedAt(UPDATED_COMPLETED_AT)
            .closedBy(UPDATED_CLOSED_BY)
            .closedAt(UPDATED_CLOSED_AT);
    }

    @BeforeEach
    void initTest() {
        reconciliation = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedReconciliation != null) {
            reconciliationRepository.delete(insertedReconciliation);
            insertedReconciliation = null;
        }
    }

    @Test
    @Transactional
    void getAllReconciliations() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList
        restReconciliationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(reconciliation.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].businessDate").value(hasItem(DEFAULT_BUSINESS_DATE.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].timestampTolerance").value(hasItem(DEFAULT_TIMESTAMP_TOLERANCE.toString())))
            .andExpect(jsonPath("$.[*].matchedCount").value(hasItem(DEFAULT_MATCHED_COUNT)))
            .andExpect(jsonPath("$.[*].discrepancyCount").value(hasItem(DEFAULT_DISCREPANCY_COUNT)))
            .andExpect(jsonPath("$.[*].discrepancyCountsByType").value(hasItem(DEFAULT_DISCREPANCY_COUNTS_BY_TYPE)))
            .andExpect(jsonPath("$.[*].startedAt").value(hasItem(DEFAULT_STARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].completedAt").value(hasItem(DEFAULT_COMPLETED_AT.toString())))
            .andExpect(jsonPath("$.[*].closedBy").value(hasItem(DEFAULT_CLOSED_BY.toString())))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllReconciliationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(reconciliationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restReconciliationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(reconciliationServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllReconciliationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(reconciliationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restReconciliationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(reconciliationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getReconciliation() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get the reconciliation
        restReconciliationMockMvc
            .perform(get(ENTITY_API_URL_ID, reconciliation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(reconciliation.getId().toString()))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.businessDate").value(DEFAULT_BUSINESS_DATE.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.timestampTolerance").value(DEFAULT_TIMESTAMP_TOLERANCE.toString()))
            .andExpect(jsonPath("$.matchedCount").value(DEFAULT_MATCHED_COUNT))
            .andExpect(jsonPath("$.discrepancyCount").value(DEFAULT_DISCREPANCY_COUNT))
            .andExpect(jsonPath("$.discrepancyCountsByType").value(DEFAULT_DISCREPANCY_COUNTS_BY_TYPE))
            .andExpect(jsonPath("$.startedAt").value(DEFAULT_STARTED_AT.toString()))
            .andExpect(jsonPath("$.completedAt").value(DEFAULT_COMPLETED_AT.toString()))
            .andExpect(jsonPath("$.closedBy").value(DEFAULT_CLOSED_BY.toString()))
            .andExpect(jsonPath("$.closedAt").value(DEFAULT_CLOSED_AT.toString()));
    }

    @Test
    @Transactional
    void getReconciliationsByIdFiltering() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        UUID id = reconciliation.getId();

        defaultReconciliationFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllReconciliationsByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where reference equals to
        defaultReconciliationFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllReconciliationsByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where reference in
        defaultReconciliationFiltering("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE, "reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllReconciliationsByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where reference is not null
        defaultReconciliationFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllReconciliationsByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where reference contains
        defaultReconciliationFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllReconciliationsByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where reference does not contain
        defaultReconciliationFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
    }

    @Test
    @Transactional
    void getAllReconciliationsByBusinessDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where businessDate equals to
        defaultReconciliationFiltering("businessDate.equals=" + DEFAULT_BUSINESS_DATE, "businessDate.equals=" + UPDATED_BUSINESS_DATE);
    }

    @Test
    @Transactional
    void getAllReconciliationsByBusinessDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where businessDate in
        defaultReconciliationFiltering(
            "businessDate.in=" + DEFAULT_BUSINESS_DATE + "," + UPDATED_BUSINESS_DATE,
            "businessDate.in=" + UPDATED_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByBusinessDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where businessDate is not null
        defaultReconciliationFiltering("businessDate.specified=true", "businessDate.specified=false");
    }

    @Test
    @Transactional
    void getAllReconciliationsByBusinessDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where businessDate is greater than or equal to
        defaultReconciliationFiltering(
            "businessDate.greaterThanOrEqual=" + DEFAULT_BUSINESS_DATE,
            "businessDate.greaterThanOrEqual=" + UPDATED_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByBusinessDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where businessDate is less than or equal to
        defaultReconciliationFiltering(
            "businessDate.lessThanOrEqual=" + DEFAULT_BUSINESS_DATE,
            "businessDate.lessThanOrEqual=" + SMALLER_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByBusinessDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where businessDate is less than
        defaultReconciliationFiltering("businessDate.lessThan=" + UPDATED_BUSINESS_DATE, "businessDate.lessThan=" + DEFAULT_BUSINESS_DATE);
    }

    @Test
    @Transactional
    void getAllReconciliationsByBusinessDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where businessDate is greater than
        defaultReconciliationFiltering(
            "businessDate.greaterThan=" + SMALLER_BUSINESS_DATE,
            "businessDate.greaterThan=" + DEFAULT_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where institutionId equals to
        defaultReconciliationFiltering("institutionId.equals=" + DEFAULT_INSTITUTION_ID, "institutionId.equals=" + UPDATED_INSTITUTION_ID);
    }

    @Test
    @Transactional
    void getAllReconciliationsByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where institutionId in
        defaultReconciliationFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where institutionId is not null
        defaultReconciliationFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllReconciliationsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where state equals to
        defaultReconciliationFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllReconciliationsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where state in
        defaultReconciliationFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllReconciliationsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where state is not null
        defaultReconciliationFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllReconciliationsByTimestampToleranceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where timestampTolerance equals to
        defaultReconciliationFiltering(
            "timestampTolerance.equals=" + DEFAULT_TIMESTAMP_TOLERANCE,
            "timestampTolerance.equals=" + UPDATED_TIMESTAMP_TOLERANCE
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByTimestampToleranceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where timestampTolerance in
        defaultReconciliationFiltering(
            "timestampTolerance.in=" + DEFAULT_TIMESTAMP_TOLERANCE + "," + UPDATED_TIMESTAMP_TOLERANCE,
            "timestampTolerance.in=" + UPDATED_TIMESTAMP_TOLERANCE
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByTimestampToleranceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where timestampTolerance is not null
        defaultReconciliationFiltering("timestampTolerance.specified=true", "timestampTolerance.specified=false");
    }

    @Test
    @Transactional
    void getAllReconciliationsByTimestampToleranceIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where timestampTolerance is greater than or equal to
        defaultReconciliationFiltering(
            "timestampTolerance.greaterThanOrEqual=" + DEFAULT_TIMESTAMP_TOLERANCE,
            "timestampTolerance.greaterThanOrEqual=" + UPDATED_TIMESTAMP_TOLERANCE
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByTimestampToleranceIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where timestampTolerance is less than or equal to
        defaultReconciliationFiltering(
            "timestampTolerance.lessThanOrEqual=" + DEFAULT_TIMESTAMP_TOLERANCE,
            "timestampTolerance.lessThanOrEqual=" + SMALLER_TIMESTAMP_TOLERANCE
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByTimestampToleranceIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where timestampTolerance is less than
        defaultReconciliationFiltering(
            "timestampTolerance.lessThan=" + UPDATED_TIMESTAMP_TOLERANCE,
            "timestampTolerance.lessThan=" + DEFAULT_TIMESTAMP_TOLERANCE
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByTimestampToleranceIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where timestampTolerance is greater than
        defaultReconciliationFiltering(
            "timestampTolerance.greaterThan=" + SMALLER_TIMESTAMP_TOLERANCE,
            "timestampTolerance.greaterThan=" + DEFAULT_TIMESTAMP_TOLERANCE
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByMatchedCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where matchedCount equals to
        defaultReconciliationFiltering("matchedCount.equals=" + DEFAULT_MATCHED_COUNT, "matchedCount.equals=" + UPDATED_MATCHED_COUNT);
    }

    @Test
    @Transactional
    void getAllReconciliationsByMatchedCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where matchedCount in
        defaultReconciliationFiltering(
            "matchedCount.in=" + DEFAULT_MATCHED_COUNT + "," + UPDATED_MATCHED_COUNT,
            "matchedCount.in=" + UPDATED_MATCHED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByMatchedCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where matchedCount is not null
        defaultReconciliationFiltering("matchedCount.specified=true", "matchedCount.specified=false");
    }

    @Test
    @Transactional
    void getAllReconciliationsByMatchedCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where matchedCount is greater than or equal to
        defaultReconciliationFiltering(
            "matchedCount.greaterThanOrEqual=" + DEFAULT_MATCHED_COUNT,
            "matchedCount.greaterThanOrEqual=" + UPDATED_MATCHED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByMatchedCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where matchedCount is less than or equal to
        defaultReconciliationFiltering(
            "matchedCount.lessThanOrEqual=" + DEFAULT_MATCHED_COUNT,
            "matchedCount.lessThanOrEqual=" + SMALLER_MATCHED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByMatchedCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where matchedCount is less than
        defaultReconciliationFiltering("matchedCount.lessThan=" + UPDATED_MATCHED_COUNT, "matchedCount.lessThan=" + DEFAULT_MATCHED_COUNT);
    }

    @Test
    @Transactional
    void getAllReconciliationsByMatchedCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where matchedCount is greater than
        defaultReconciliationFiltering(
            "matchedCount.greaterThan=" + SMALLER_MATCHED_COUNT,
            "matchedCount.greaterThan=" + DEFAULT_MATCHED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByDiscrepancyCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where discrepancyCount equals to
        defaultReconciliationFiltering(
            "discrepancyCount.equals=" + DEFAULT_DISCREPANCY_COUNT,
            "discrepancyCount.equals=" + UPDATED_DISCREPANCY_COUNT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByDiscrepancyCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where discrepancyCount in
        defaultReconciliationFiltering(
            "discrepancyCount.in=" + DEFAULT_DISCREPANCY_COUNT + "," + UPDATED_DISCREPANCY_COUNT,
            "discrepancyCount.in=" + UPDATED_DISCREPANCY_COUNT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByDiscrepancyCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where discrepancyCount is not null
        defaultReconciliationFiltering("discrepancyCount.specified=true", "discrepancyCount.specified=false");
    }

    @Test
    @Transactional
    void getAllReconciliationsByDiscrepancyCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where discrepancyCount is greater than or equal to
        defaultReconciliationFiltering(
            "discrepancyCount.greaterThanOrEqual=" + DEFAULT_DISCREPANCY_COUNT,
            "discrepancyCount.greaterThanOrEqual=" + UPDATED_DISCREPANCY_COUNT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByDiscrepancyCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where discrepancyCount is less than or equal to
        defaultReconciliationFiltering(
            "discrepancyCount.lessThanOrEqual=" + DEFAULT_DISCREPANCY_COUNT,
            "discrepancyCount.lessThanOrEqual=" + SMALLER_DISCREPANCY_COUNT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByDiscrepancyCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where discrepancyCount is less than
        defaultReconciliationFiltering(
            "discrepancyCount.lessThan=" + UPDATED_DISCREPANCY_COUNT,
            "discrepancyCount.lessThan=" + DEFAULT_DISCREPANCY_COUNT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByDiscrepancyCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where discrepancyCount is greater than
        defaultReconciliationFiltering(
            "discrepancyCount.greaterThan=" + SMALLER_DISCREPANCY_COUNT,
            "discrepancyCount.greaterThan=" + DEFAULT_DISCREPANCY_COUNT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByStartedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where startedAt equals to
        defaultReconciliationFiltering("startedAt.equals=" + DEFAULT_STARTED_AT, "startedAt.equals=" + UPDATED_STARTED_AT);
    }

    @Test
    @Transactional
    void getAllReconciliationsByStartedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where startedAt in
        defaultReconciliationFiltering(
            "startedAt.in=" + DEFAULT_STARTED_AT + "," + UPDATED_STARTED_AT,
            "startedAt.in=" + UPDATED_STARTED_AT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByStartedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where startedAt is not null
        defaultReconciliationFiltering("startedAt.specified=true", "startedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllReconciliationsByCompletedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where completedAt equals to
        defaultReconciliationFiltering("completedAt.equals=" + DEFAULT_COMPLETED_AT, "completedAt.equals=" + UPDATED_COMPLETED_AT);
    }

    @Test
    @Transactional
    void getAllReconciliationsByCompletedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where completedAt in
        defaultReconciliationFiltering(
            "completedAt.in=" + DEFAULT_COMPLETED_AT + "," + UPDATED_COMPLETED_AT,
            "completedAt.in=" + UPDATED_COMPLETED_AT
        );
    }

    @Test
    @Transactional
    void getAllReconciliationsByCompletedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where completedAt is not null
        defaultReconciliationFiltering("completedAt.specified=true", "completedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllReconciliationsByClosedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where closedBy equals to
        defaultReconciliationFiltering("closedBy.equals=" + DEFAULT_CLOSED_BY, "closedBy.equals=" + UPDATED_CLOSED_BY);
    }

    @Test
    @Transactional
    void getAllReconciliationsByClosedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where closedBy in
        defaultReconciliationFiltering("closedBy.in=" + DEFAULT_CLOSED_BY + "," + UPDATED_CLOSED_BY, "closedBy.in=" + UPDATED_CLOSED_BY);
    }

    @Test
    @Transactional
    void getAllReconciliationsByClosedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where closedBy is not null
        defaultReconciliationFiltering("closedBy.specified=true", "closedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllReconciliationsByClosedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where closedAt equals to
        defaultReconciliationFiltering("closedAt.equals=" + DEFAULT_CLOSED_AT, "closedAt.equals=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllReconciliationsByClosedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where closedAt in
        defaultReconciliationFiltering("closedAt.in=" + DEFAULT_CLOSED_AT + "," + UPDATED_CLOSED_AT, "closedAt.in=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllReconciliationsByClosedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReconciliation = reconciliationRepository.saveAndFlush(reconciliation);

        // Get all the reconciliationList where closedAt is not null
        defaultReconciliationFiltering("closedAt.specified=true", "closedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllReconciliationsBySwitchStatementIsEqualToSomething() throws Exception {
        ImportedStatement switchStatement;
        if (TestUtil.findAll(em, ImportedStatement.class).isEmpty()) {
            reconciliationRepository.saveAndFlush(reconciliation);
            switchStatement = ImportedStatementResourceIT.createEntity();
        } else {
            switchStatement = TestUtil.findAll(em, ImportedStatement.class).get(0);
        }
        em.persist(switchStatement);
        em.flush();
        reconciliation.setSwitchStatement(switchStatement);
        reconciliationRepository.saveAndFlush(reconciliation);
        UUID switchStatementId = switchStatement.getId();
        // Get all the reconciliationList where switchStatement equals to switchStatementId
        defaultReconciliationShouldBeFound("switchStatementId.equals=" + switchStatementId);

        // Get all the reconciliationList where switchStatement equals to UUID.randomUUID()
        defaultReconciliationShouldNotBeFound("switchStatementId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllReconciliationsByParticipantStatementIsEqualToSomething() throws Exception {
        ImportedStatement participantStatement;
        if (TestUtil.findAll(em, ImportedStatement.class).isEmpty()) {
            reconciliationRepository.saveAndFlush(reconciliation);
            participantStatement = ImportedStatementResourceIT.createEntity();
        } else {
            participantStatement = TestUtil.findAll(em, ImportedStatement.class).get(0);
        }
        em.persist(participantStatement);
        em.flush();
        reconciliation.setParticipantStatement(participantStatement);
        reconciliationRepository.saveAndFlush(reconciliation);
        UUID participantStatementId = participantStatement.getId();
        // Get all the reconciliationList where participantStatement equals to participantStatementId
        defaultReconciliationShouldBeFound("participantStatementId.equals=" + participantStatementId);

        // Get all the reconciliationList where participantStatement equals to UUID.randomUUID()
        defaultReconciliationShouldNotBeFound("participantStatementId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllReconciliationsBySettlementStatementIsEqualToSomething() throws Exception {
        ImportedStatement settlementStatement;
        if (TestUtil.findAll(em, ImportedStatement.class).isEmpty()) {
            reconciliationRepository.saveAndFlush(reconciliation);
            settlementStatement = ImportedStatementResourceIT.createEntity();
        } else {
            settlementStatement = TestUtil.findAll(em, ImportedStatement.class).get(0);
        }
        em.persist(settlementStatement);
        em.flush();
        reconciliation.setSettlementStatement(settlementStatement);
        reconciliationRepository.saveAndFlush(reconciliation);
        UUID settlementStatementId = settlementStatement.getId();
        // Get all the reconciliationList where settlementStatement equals to settlementStatementId
        defaultReconciliationShouldBeFound("settlementStatementId.equals=" + settlementStatementId);

        // Get all the reconciliationList where settlementStatement equals to UUID.randomUUID()
        defaultReconciliationShouldNotBeFound("settlementStatementId.equals=" + UUID.randomUUID());
    }

    private void defaultReconciliationFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultReconciliationShouldBeFound(shouldBeFound);
        defaultReconciliationShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultReconciliationShouldBeFound(String filter) throws Exception {
        restReconciliationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(reconciliation.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].businessDate").value(hasItem(DEFAULT_BUSINESS_DATE.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].timestampTolerance").value(hasItem(DEFAULT_TIMESTAMP_TOLERANCE.toString())))
            .andExpect(jsonPath("$.[*].matchedCount").value(hasItem(DEFAULT_MATCHED_COUNT)))
            .andExpect(jsonPath("$.[*].discrepancyCount").value(hasItem(DEFAULT_DISCREPANCY_COUNT)))
            .andExpect(jsonPath("$.[*].discrepancyCountsByType").value(hasItem(DEFAULT_DISCREPANCY_COUNTS_BY_TYPE)))
            .andExpect(jsonPath("$.[*].startedAt").value(hasItem(DEFAULT_STARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].completedAt").value(hasItem(DEFAULT_COMPLETED_AT.toString())))
            .andExpect(jsonPath("$.[*].closedBy").value(hasItem(DEFAULT_CLOSED_BY.toString())))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())));

        // Check, that the count call also returns 1
        restReconciliationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultReconciliationShouldNotBeFound(String filter) throws Exception {
        restReconciliationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restReconciliationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingReconciliation() throws Exception {
        // Get the reconciliation
        restReconciliationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return reconciliationRepository.count();
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

    protected Reconciliation getPersistedReconciliation(Reconciliation reconciliation) {
        return reconciliationRepository.findById(reconciliation.getId()).orElseThrow();
    }

    protected void assertPersistedReconciliationToMatchAllProperties(Reconciliation expectedReconciliation) {
        assertReconciliationAllPropertiesEquals(expectedReconciliation, getPersistedReconciliation(expectedReconciliation));
    }

    protected void assertPersistedReconciliationToMatchUpdatableProperties(Reconciliation expectedReconciliation) {
        assertReconciliationAllUpdatablePropertiesEquals(expectedReconciliation, getPersistedReconciliation(expectedReconciliation));
    }
}
