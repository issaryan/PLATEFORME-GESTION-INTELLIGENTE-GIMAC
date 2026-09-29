package cm.gimac.des.reporting.web.rest;

import static cm.gimac.des.reporting.domain.ImportedStatementAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.reporting.IntegrationTest;
import cm.gimac.des.reporting.domain.ImportedStatement;
import cm.gimac.des.reporting.domain.ImportedStatement;
import cm.gimac.des.reporting.domain.enumeration.StatementSource;
import cm.gimac.des.reporting.repository.ImportedStatementRepository;
import cm.gimac.des.reporting.service.ImportedStatementService;
import cm.gimac.des.reporting.service.mapper.ImportedStatementMapper;
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
 * Integration tests for the {@link ImportedStatementResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ImportedStatementResourceIT {

    private static final StatementSource DEFAULT_SOURCE = StatementSource.COMMUTATEUR;
    private static final StatementSource UPDATED_SOURCE = StatementSource.PARTICIPANT;

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final String DEFAULT_PERIOD = "3842-03-14";
    private static final String UPDATED_PERIOD = "2147-W61";

    private static final Instant DEFAULT_IMPORTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_IMPORTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_IMPORTER_ID = UUID.randomUUID();
    private static final UUID UPDATED_IMPORTER_ID = UUID.randomUUID();

    private static final String DEFAULT_DIGEST = "4087fc9ebf8b5ff130d2e60cbe169e0d82108200eeaff58b10a00cf1e0886207";
    private static final String UPDATED_DIGEST = "9647ec6bc94013f796dd1d6cd6b9570002f8ce611f62e6159f3fb260561661c9";

    private static final Boolean DEFAULT_MASKING_PASSED = false;
    private static final Boolean UPDATED_MASKING_PASSED = true;

    private static final Integer DEFAULT_REJECTED_ROWS = 0;
    private static final Integer UPDATED_REJECTED_ROWS = 1;
    private static final Integer SMALLER_REJECTED_ROWS = 0 - 1;

    private static final Instant DEFAULT_MASKING_CHECKED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_MASKING_CHECKED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_ROW_COUNT = 0;
    private static final Integer UPDATED_ROW_COUNT = 1;
    private static final Integer SMALLER_ROW_COUNT = 0 - 1;

    private static final UUID DEFAULT_DOCUMENT_REF = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_REF = UUID.randomUUID();

    private static final Boolean DEFAULT_CURRENT_VERSION = false;
    private static final Boolean UPDATED_CURRENT_VERSION = true;

    private static final String ENTITY_API_URL = "/api/imported-statements";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ImportedStatementRepository importedStatementRepository;

    @Mock
    private ImportedStatementRepository importedStatementRepositoryMock;

    @Autowired
    private ImportedStatementMapper importedStatementMapper;

    @Mock
    private ImportedStatementService importedStatementServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restImportedStatementMockMvc;

    private ImportedStatement importedStatement;

    private ImportedStatement insertedImportedStatement;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ImportedStatement createEntity() {
        return new ImportedStatement()
            .source(DEFAULT_SOURCE)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .period(DEFAULT_PERIOD)
            .importedAt(DEFAULT_IMPORTED_AT)
            .importerId(DEFAULT_IMPORTER_ID)
            .digest(DEFAULT_DIGEST)
            .maskingPassed(DEFAULT_MASKING_PASSED)
            .rejectedRows(DEFAULT_REJECTED_ROWS)
            .maskingCheckedAt(DEFAULT_MASKING_CHECKED_AT)
            .rowCount(DEFAULT_ROW_COUNT)
            .documentRef(DEFAULT_DOCUMENT_REF)
            .currentVersion(DEFAULT_CURRENT_VERSION);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ImportedStatement createUpdatedEntity() {
        return new ImportedStatement()
            .source(UPDATED_SOURCE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .period(UPDATED_PERIOD)
            .importedAt(UPDATED_IMPORTED_AT)
            .importerId(UPDATED_IMPORTER_ID)
            .digest(UPDATED_DIGEST)
            .maskingPassed(UPDATED_MASKING_PASSED)
            .rejectedRows(UPDATED_REJECTED_ROWS)
            .maskingCheckedAt(UPDATED_MASKING_CHECKED_AT)
            .rowCount(UPDATED_ROW_COUNT)
            .documentRef(UPDATED_DOCUMENT_REF)
            .currentVersion(UPDATED_CURRENT_VERSION);
    }

    @BeforeEach
    void initTest() {
        importedStatement = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedImportedStatement != null) {
            importedStatementRepository.delete(insertedImportedStatement);
            insertedImportedStatement = null;
        }
    }

    @Test
    @Transactional
    void getAllImportedStatements() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList
        restImportedStatementMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(importedStatement.getId().toString())))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].period").value(hasItem(DEFAULT_PERIOD)))
            .andExpect(jsonPath("$.[*].importedAt").value(hasItem(DEFAULT_IMPORTED_AT.toString())))
            .andExpect(jsonPath("$.[*].importerId").value(hasItem(DEFAULT_IMPORTER_ID.toString())))
            .andExpect(jsonPath("$.[*].digest").value(hasItem(DEFAULT_DIGEST)))
            .andExpect(jsonPath("$.[*].maskingPassed").value(hasItem(DEFAULT_MASKING_PASSED)))
            .andExpect(jsonPath("$.[*].rejectedRows").value(hasItem(DEFAULT_REJECTED_ROWS)))
            .andExpect(jsonPath("$.[*].maskingCheckedAt").value(hasItem(DEFAULT_MASKING_CHECKED_AT.toString())))
            .andExpect(jsonPath("$.[*].rowCount").value(hasItem(DEFAULT_ROW_COUNT)))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())))
            .andExpect(jsonPath("$.[*].currentVersion").value(hasItem(DEFAULT_CURRENT_VERSION)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllImportedStatementsWithEagerRelationshipsIsEnabled() throws Exception {
        when(importedStatementServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restImportedStatementMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(importedStatementServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllImportedStatementsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(importedStatementServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restImportedStatementMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(importedStatementRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getImportedStatement() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get the importedStatement
        restImportedStatementMockMvc
            .perform(get(ENTITY_API_URL_ID, importedStatement.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(importedStatement.getId().toString()))
            .andExpect(jsonPath("$.source").value(DEFAULT_SOURCE.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.period").value(DEFAULT_PERIOD))
            .andExpect(jsonPath("$.importedAt").value(DEFAULT_IMPORTED_AT.toString()))
            .andExpect(jsonPath("$.importerId").value(DEFAULT_IMPORTER_ID.toString()))
            .andExpect(jsonPath("$.digest").value(DEFAULT_DIGEST))
            .andExpect(jsonPath("$.maskingPassed").value(DEFAULT_MASKING_PASSED))
            .andExpect(jsonPath("$.rejectedRows").value(DEFAULT_REJECTED_ROWS))
            .andExpect(jsonPath("$.maskingCheckedAt").value(DEFAULT_MASKING_CHECKED_AT.toString()))
            .andExpect(jsonPath("$.rowCount").value(DEFAULT_ROW_COUNT))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF.toString()))
            .andExpect(jsonPath("$.currentVersion").value(DEFAULT_CURRENT_VERSION));
    }

    @Test
    @Transactional
    void getImportedStatementsByIdFiltering() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        UUID id = importedStatement.getId();

        defaultImportedStatementFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllImportedStatementsBySourceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where source equals to
        defaultImportedStatementFiltering("source.equals=" + DEFAULT_SOURCE, "source.equals=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllImportedStatementsBySourceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where source in
        defaultImportedStatementFiltering("source.in=" + DEFAULT_SOURCE + "," + UPDATED_SOURCE, "source.in=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllImportedStatementsBySourceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where source is not null
        defaultImportedStatementFiltering("source.specified=true", "source.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where institutionId equals to
        defaultImportedStatementFiltering(
            "institutionId.equals=" + DEFAULT_INSTITUTION_ID,
            "institutionId.equals=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where institutionId in
        defaultImportedStatementFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where institutionId is not null
        defaultImportedStatementFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByPeriodIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where period equals to
        defaultImportedStatementFiltering("period.equals=" + DEFAULT_PERIOD, "period.equals=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByPeriodIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where period in
        defaultImportedStatementFiltering("period.in=" + DEFAULT_PERIOD + "," + UPDATED_PERIOD, "period.in=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByPeriodIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where period is not null
        defaultImportedStatementFiltering("period.specified=true", "period.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByPeriodContainsSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where period contains
        defaultImportedStatementFiltering("period.contains=" + DEFAULT_PERIOD, "period.contains=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByPeriodNotContainsSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where period does not contain
        defaultImportedStatementFiltering("period.doesNotContain=" + UPDATED_PERIOD, "period.doesNotContain=" + DEFAULT_PERIOD);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByImportedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where importedAt equals to
        defaultImportedStatementFiltering("importedAt.equals=" + DEFAULT_IMPORTED_AT, "importedAt.equals=" + UPDATED_IMPORTED_AT);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByImportedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where importedAt in
        defaultImportedStatementFiltering(
            "importedAt.in=" + DEFAULT_IMPORTED_AT + "," + UPDATED_IMPORTED_AT,
            "importedAt.in=" + UPDATED_IMPORTED_AT
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByImportedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where importedAt is not null
        defaultImportedStatementFiltering("importedAt.specified=true", "importedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByImporterIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where importerId equals to
        defaultImportedStatementFiltering("importerId.equals=" + DEFAULT_IMPORTER_ID, "importerId.equals=" + UPDATED_IMPORTER_ID);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByImporterIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where importerId in
        defaultImportedStatementFiltering(
            "importerId.in=" + DEFAULT_IMPORTER_ID + "," + UPDATED_IMPORTER_ID,
            "importerId.in=" + UPDATED_IMPORTER_ID
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByImporterIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where importerId is not null
        defaultImportedStatementFiltering("importerId.specified=true", "importerId.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where digest equals to
        defaultImportedStatementFiltering("digest.equals=" + DEFAULT_DIGEST, "digest.equals=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where digest in
        defaultImportedStatementFiltering("digest.in=" + DEFAULT_DIGEST + "," + UPDATED_DIGEST, "digest.in=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where digest is not null
        defaultImportedStatementFiltering("digest.specified=true", "digest.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where digest contains
        defaultImportedStatementFiltering("digest.contains=" + DEFAULT_DIGEST, "digest.contains=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where digest does not contain
        defaultImportedStatementFiltering("digest.doesNotContain=" + UPDATED_DIGEST, "digest.doesNotContain=" + DEFAULT_DIGEST);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingPassedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingPassed equals to
        defaultImportedStatementFiltering(
            "maskingPassed.equals=" + DEFAULT_MASKING_PASSED,
            "maskingPassed.equals=" + UPDATED_MASKING_PASSED
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingPassedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingPassed in
        defaultImportedStatementFiltering(
            "maskingPassed.in=" + DEFAULT_MASKING_PASSED + "," + UPDATED_MASKING_PASSED,
            "maskingPassed.in=" + UPDATED_MASKING_PASSED
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingPassedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingPassed is not null
        defaultImportedStatementFiltering("maskingPassed.specified=true", "maskingPassed.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRejectedRowsIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rejectedRows equals to
        defaultImportedStatementFiltering("rejectedRows.equals=" + DEFAULT_REJECTED_ROWS, "rejectedRows.equals=" + UPDATED_REJECTED_ROWS);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRejectedRowsIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rejectedRows in
        defaultImportedStatementFiltering(
            "rejectedRows.in=" + DEFAULT_REJECTED_ROWS + "," + UPDATED_REJECTED_ROWS,
            "rejectedRows.in=" + UPDATED_REJECTED_ROWS
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRejectedRowsIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rejectedRows is not null
        defaultImportedStatementFiltering("rejectedRows.specified=true", "rejectedRows.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRejectedRowsIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rejectedRows is greater than or equal to
        defaultImportedStatementFiltering(
            "rejectedRows.greaterThanOrEqual=" + DEFAULT_REJECTED_ROWS,
            "rejectedRows.greaterThanOrEqual=" + UPDATED_REJECTED_ROWS
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRejectedRowsIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rejectedRows is less than or equal to
        defaultImportedStatementFiltering(
            "rejectedRows.lessThanOrEqual=" + DEFAULT_REJECTED_ROWS,
            "rejectedRows.lessThanOrEqual=" + SMALLER_REJECTED_ROWS
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRejectedRowsIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rejectedRows is less than
        defaultImportedStatementFiltering(
            "rejectedRows.lessThan=" + UPDATED_REJECTED_ROWS,
            "rejectedRows.lessThan=" + DEFAULT_REJECTED_ROWS
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRejectedRowsIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rejectedRows is greater than
        defaultImportedStatementFiltering(
            "rejectedRows.greaterThan=" + SMALLER_REJECTED_ROWS,
            "rejectedRows.greaterThan=" + DEFAULT_REJECTED_ROWS
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingCheckedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingCheckedAt equals to
        defaultImportedStatementFiltering(
            "maskingCheckedAt.equals=" + DEFAULT_MASKING_CHECKED_AT,
            "maskingCheckedAt.equals=" + UPDATED_MASKING_CHECKED_AT
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingCheckedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingCheckedAt in
        defaultImportedStatementFiltering(
            "maskingCheckedAt.in=" + DEFAULT_MASKING_CHECKED_AT + "," + UPDATED_MASKING_CHECKED_AT,
            "maskingCheckedAt.in=" + UPDATED_MASKING_CHECKED_AT
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingCheckedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingCheckedAt is not null
        defaultImportedStatementFiltering("maskingCheckedAt.specified=true", "maskingCheckedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRowCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rowCount equals to
        defaultImportedStatementFiltering("rowCount.equals=" + DEFAULT_ROW_COUNT, "rowCount.equals=" + UPDATED_ROW_COUNT);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRowCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rowCount in
        defaultImportedStatementFiltering("rowCount.in=" + DEFAULT_ROW_COUNT + "," + UPDATED_ROW_COUNT, "rowCount.in=" + UPDATED_ROW_COUNT);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRowCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rowCount is not null
        defaultImportedStatementFiltering("rowCount.specified=true", "rowCount.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRowCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rowCount is greater than or equal to
        defaultImportedStatementFiltering(
            "rowCount.greaterThanOrEqual=" + DEFAULT_ROW_COUNT,
            "rowCount.greaterThanOrEqual=" + UPDATED_ROW_COUNT
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRowCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rowCount is less than or equal to
        defaultImportedStatementFiltering("rowCount.lessThanOrEqual=" + DEFAULT_ROW_COUNT, "rowCount.lessThanOrEqual=" + SMALLER_ROW_COUNT);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRowCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rowCount is less than
        defaultImportedStatementFiltering("rowCount.lessThan=" + UPDATED_ROW_COUNT, "rowCount.lessThan=" + DEFAULT_ROW_COUNT);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByRowCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where rowCount is greater than
        defaultImportedStatementFiltering("rowCount.greaterThan=" + SMALLER_ROW_COUNT, "rowCount.greaterThan=" + DEFAULT_ROW_COUNT);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByDocumentRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where documentRef equals to
        defaultImportedStatementFiltering("documentRef.equals=" + DEFAULT_DOCUMENT_REF, "documentRef.equals=" + UPDATED_DOCUMENT_REF);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByDocumentRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where documentRef in
        defaultImportedStatementFiltering(
            "documentRef.in=" + DEFAULT_DOCUMENT_REF + "," + UPDATED_DOCUMENT_REF,
            "documentRef.in=" + UPDATED_DOCUMENT_REF
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByDocumentRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where documentRef is not null
        defaultImportedStatementFiltering("documentRef.specified=true", "documentRef.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByCurrentVersionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where currentVersion equals to
        defaultImportedStatementFiltering(
            "currentVersion.equals=" + DEFAULT_CURRENT_VERSION,
            "currentVersion.equals=" + UPDATED_CURRENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByCurrentVersionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where currentVersion in
        defaultImportedStatementFiltering(
            "currentVersion.in=" + DEFAULT_CURRENT_VERSION + "," + UPDATED_CURRENT_VERSION,
            "currentVersion.in=" + UPDATED_CURRENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByCurrentVersionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where currentVersion is not null
        defaultImportedStatementFiltering("currentVersion.specified=true", "currentVersion.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsBySupersededByIsEqualToSomething() throws Exception {
        ImportedStatement supersededBy;
        if (TestUtil.findAll(em, ImportedStatement.class).isEmpty()) {
            importedStatementRepository.saveAndFlush(importedStatement);
            supersededBy = ImportedStatementResourceIT.createUpdatedEntity(); // champs uniques distincts de l'entité testée
        } else {
            supersededBy = TestUtil.findAll(em, ImportedStatement.class).get(0);
        }
        em.persist(supersededBy);
        em.flush();
        importedStatement.setSupersededBy(supersededBy);
        importedStatementRepository.saveAndFlush(importedStatement);
        UUID supersededById = supersededBy.getId();
        // Get all the importedStatementList where supersededBy equals to supersededById
        defaultImportedStatementShouldBeFound("supersededById.equals=" + supersededById);

        // Get all the importedStatementList where supersededBy equals to UUID.randomUUID()
        defaultImportedStatementShouldNotBeFound("supersededById.equals=" + UUID.randomUUID());
    }

    private void defaultImportedStatementFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultImportedStatementShouldBeFound(shouldBeFound);
        defaultImportedStatementShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultImportedStatementShouldBeFound(String filter) throws Exception {
        restImportedStatementMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(importedStatement.getId().toString())))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].period").value(hasItem(DEFAULT_PERIOD)))
            .andExpect(jsonPath("$.[*].importedAt").value(hasItem(DEFAULT_IMPORTED_AT.toString())))
            .andExpect(jsonPath("$.[*].importerId").value(hasItem(DEFAULT_IMPORTER_ID.toString())))
            .andExpect(jsonPath("$.[*].digest").value(hasItem(DEFAULT_DIGEST)))
            .andExpect(jsonPath("$.[*].maskingPassed").value(hasItem(DEFAULT_MASKING_PASSED)))
            .andExpect(jsonPath("$.[*].rejectedRows").value(hasItem(DEFAULT_REJECTED_ROWS)))
            .andExpect(jsonPath("$.[*].maskingCheckedAt").value(hasItem(DEFAULT_MASKING_CHECKED_AT.toString())))
            .andExpect(jsonPath("$.[*].rowCount").value(hasItem(DEFAULT_ROW_COUNT)))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())))
            .andExpect(jsonPath("$.[*].currentVersion").value(hasItem(DEFAULT_CURRENT_VERSION)));

        // Check, that the count call also returns 1
        restImportedStatementMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultImportedStatementShouldNotBeFound(String filter) throws Exception {
        restImportedStatementMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restImportedStatementMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingImportedStatement() throws Exception {
        // Get the importedStatement
        restImportedStatementMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return importedStatementRepository.count();
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

    protected ImportedStatement getPersistedImportedStatement(ImportedStatement importedStatement) {
        return importedStatementRepository.findById(importedStatement.getId()).orElseThrow();
    }

    protected void assertPersistedImportedStatementToMatchAllProperties(ImportedStatement expectedImportedStatement) {
        assertImportedStatementAllPropertiesEquals(expectedImportedStatement, getPersistedImportedStatement(expectedImportedStatement));
    }

    protected void assertPersistedImportedStatementToMatchUpdatableProperties(ImportedStatement expectedImportedStatement) {
        assertImportedStatementAllUpdatablePropertiesEquals(
            expectedImportedStatement,
            getPersistedImportedStatement(expectedImportedStatement)
        );
    }
}
