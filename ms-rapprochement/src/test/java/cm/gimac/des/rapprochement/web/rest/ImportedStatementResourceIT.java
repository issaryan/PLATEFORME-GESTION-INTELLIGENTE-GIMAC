package cm.gimac.des.rapprochement.web.rest;

import static cm.gimac.des.rapprochement.domain.ImportedStatementAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.rapprochement.IntegrationTest;
import cm.gimac.des.rapprochement.domain.ImportedStatement;
import cm.gimac.des.rapprochement.domain.enumeration.StatementFormat;
import cm.gimac.des.rapprochement.domain.enumeration.StatementSource;
import cm.gimac.des.rapprochement.repository.ImportedStatementRepository;
import cm.gimac.des.rapprochement.service.mapper.ImportedStatementMapper;
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
 * Integration tests for the {@link ImportedStatementResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ImportedStatementResourceIT {

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final StatementSource DEFAULT_SOURCE = StatementSource.COMMUTATEUR;
    private static final StatementSource UPDATED_SOURCE = StatementSource.PARTICIPANT;

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final LocalDate DEFAULT_BUSINESS_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_BUSINESS_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_BUSINESS_DATE = LocalDate.ofEpochDay(-1L);

    private static final Instant DEFAULT_IMPORTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_IMPORTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_IMPORTER_ID = UUID.randomUUID();
    private static final UUID UPDATED_IMPORTER_ID = UUID.randomUUID();

    private static final StatementFormat DEFAULT_FORMAT = StatementFormat.CSV;
    private static final StatementFormat UPDATED_FORMAT = StatementFormat.JSON;

    private static final String DEFAULT_DIGEST = "c7e3dc744daf950daa93d5807afec0f548f28ec5f7fa95fdbca3d1d4426d56bb";
    private static final String UPDATED_DIGEST = "2169a58a671ecd8eea3317cd2b82117b47db33ca9007ad25df06730f4427038f";

    private static final Boolean DEFAULT_MASKING_PASSED = false;
    private static final Boolean UPDATED_MASKING_PASSED = true;

    private static final Integer DEFAULT_MASKING_REJECTED_LINES = 0;
    private static final Integer UPDATED_MASKING_REJECTED_LINES = 1;
    private static final Integer SMALLER_MASKING_REJECTED_LINES = 0 - 1;

    private static final Instant DEFAULT_MASKING_CHECKED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_MASKING_CHECKED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_LINE_COUNT = 0;
    private static final Integer UPDATED_LINE_COUNT = 1;
    private static final Integer SMALLER_LINE_COUNT = 0 - 1;

    private static final UUID DEFAULT_DOCUMENT_REF = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_REF = UUID.randomUUID();

    private static final LocalDate DEFAULT_LINES_RETAINED_UNTIL = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_LINES_RETAINED_UNTIL = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_LINES_RETAINED_UNTIL = LocalDate.ofEpochDay(-1L);

    private static final String ENTITY_API_URL = "/api/imported-statements";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ImportedStatementRepository importedStatementRepository;

    @Autowired
    private ImportedStatementMapper importedStatementMapper;

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
            .reference(DEFAULT_REFERENCE)
            .source(DEFAULT_SOURCE)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .businessDate(DEFAULT_BUSINESS_DATE)
            .importedAt(DEFAULT_IMPORTED_AT)
            .importerId(DEFAULT_IMPORTER_ID)
            .format(DEFAULT_FORMAT)
            .digest(DEFAULT_DIGEST)
            .maskingPassed(DEFAULT_MASKING_PASSED)
            .maskingRejectedLines(DEFAULT_MASKING_REJECTED_LINES)
            .maskingCheckedAt(DEFAULT_MASKING_CHECKED_AT)
            .lineCount(DEFAULT_LINE_COUNT)
            .documentRef(DEFAULT_DOCUMENT_REF)
            .linesRetainedUntil(DEFAULT_LINES_RETAINED_UNTIL);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ImportedStatement createUpdatedEntity() {
        return new ImportedStatement()
            .reference(UPDATED_REFERENCE)
            .source(UPDATED_SOURCE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .businessDate(UPDATED_BUSINESS_DATE)
            .importedAt(UPDATED_IMPORTED_AT)
            .importerId(UPDATED_IMPORTER_ID)
            .format(UPDATED_FORMAT)
            .digest(UPDATED_DIGEST)
            .maskingPassed(UPDATED_MASKING_PASSED)
            .maskingRejectedLines(UPDATED_MASKING_REJECTED_LINES)
            .maskingCheckedAt(UPDATED_MASKING_CHECKED_AT)
            .lineCount(UPDATED_LINE_COUNT)
            .documentRef(UPDATED_DOCUMENT_REF)
            .linesRetainedUntil(UPDATED_LINES_RETAINED_UNTIL);
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
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].businessDate").value(hasItem(DEFAULT_BUSINESS_DATE.toString())))
            .andExpect(jsonPath("$.[*].importedAt").value(hasItem(DEFAULT_IMPORTED_AT.toString())))
            .andExpect(jsonPath("$.[*].importerId").value(hasItem(DEFAULT_IMPORTER_ID.toString())))
            .andExpect(jsonPath("$.[*].format").value(hasItem(DEFAULT_FORMAT.toString())))
            .andExpect(jsonPath("$.[*].digest").value(hasItem(DEFAULT_DIGEST)))
            .andExpect(jsonPath("$.[*].maskingPassed").value(hasItem(DEFAULT_MASKING_PASSED)))
            .andExpect(jsonPath("$.[*].maskingRejectedLines").value(hasItem(DEFAULT_MASKING_REJECTED_LINES)))
            .andExpect(jsonPath("$.[*].maskingCheckedAt").value(hasItem(DEFAULT_MASKING_CHECKED_AT.toString())))
            .andExpect(jsonPath("$.[*].lineCount").value(hasItem(DEFAULT_LINE_COUNT)))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())))
            .andExpect(jsonPath("$.[*].linesRetainedUntil").value(hasItem(DEFAULT_LINES_RETAINED_UNTIL.toString())));
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
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.source").value(DEFAULT_SOURCE.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.businessDate").value(DEFAULT_BUSINESS_DATE.toString()))
            .andExpect(jsonPath("$.importedAt").value(DEFAULT_IMPORTED_AT.toString()))
            .andExpect(jsonPath("$.importerId").value(DEFAULT_IMPORTER_ID.toString()))
            .andExpect(jsonPath("$.format").value(DEFAULT_FORMAT.toString()))
            .andExpect(jsonPath("$.digest").value(DEFAULT_DIGEST))
            .andExpect(jsonPath("$.maskingPassed").value(DEFAULT_MASKING_PASSED))
            .andExpect(jsonPath("$.maskingRejectedLines").value(DEFAULT_MASKING_REJECTED_LINES))
            .andExpect(jsonPath("$.maskingCheckedAt").value(DEFAULT_MASKING_CHECKED_AT.toString()))
            .andExpect(jsonPath("$.lineCount").value(DEFAULT_LINE_COUNT))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF.toString()))
            .andExpect(jsonPath("$.linesRetainedUntil").value(DEFAULT_LINES_RETAINED_UNTIL.toString()));
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
    void getAllImportedStatementsByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where reference equals to
        defaultImportedStatementFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where reference in
        defaultImportedStatementFiltering(
            "reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE,
            "reference.in=" + UPDATED_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where reference is not null
        defaultImportedStatementFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where reference contains
        defaultImportedStatementFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where reference does not contain
        defaultImportedStatementFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
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
    void getAllImportedStatementsByBusinessDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where businessDate equals to
        defaultImportedStatementFiltering("businessDate.equals=" + DEFAULT_BUSINESS_DATE, "businessDate.equals=" + UPDATED_BUSINESS_DATE);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByBusinessDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where businessDate in
        defaultImportedStatementFiltering(
            "businessDate.in=" + DEFAULT_BUSINESS_DATE + "," + UPDATED_BUSINESS_DATE,
            "businessDate.in=" + UPDATED_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByBusinessDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where businessDate is not null
        defaultImportedStatementFiltering("businessDate.specified=true", "businessDate.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByBusinessDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where businessDate is greater than or equal to
        defaultImportedStatementFiltering(
            "businessDate.greaterThanOrEqual=" + DEFAULT_BUSINESS_DATE,
            "businessDate.greaterThanOrEqual=" + UPDATED_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByBusinessDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where businessDate is less than or equal to
        defaultImportedStatementFiltering(
            "businessDate.lessThanOrEqual=" + DEFAULT_BUSINESS_DATE,
            "businessDate.lessThanOrEqual=" + SMALLER_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByBusinessDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where businessDate is less than
        defaultImportedStatementFiltering(
            "businessDate.lessThan=" + UPDATED_BUSINESS_DATE,
            "businessDate.lessThan=" + DEFAULT_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByBusinessDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where businessDate is greater than
        defaultImportedStatementFiltering(
            "businessDate.greaterThan=" + SMALLER_BUSINESS_DATE,
            "businessDate.greaterThan=" + DEFAULT_BUSINESS_DATE
        );
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
    void getAllImportedStatementsByFormatIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where format equals to
        defaultImportedStatementFiltering("format.equals=" + DEFAULT_FORMAT, "format.equals=" + UPDATED_FORMAT);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByFormatIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where format in
        defaultImportedStatementFiltering("format.in=" + DEFAULT_FORMAT + "," + UPDATED_FORMAT, "format.in=" + UPDATED_FORMAT);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByFormatIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where format is not null
        defaultImportedStatementFiltering("format.specified=true", "format.specified=false");
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
    void getAllImportedStatementsByMaskingRejectedLinesIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingRejectedLines equals to
        defaultImportedStatementFiltering(
            "maskingRejectedLines.equals=" + DEFAULT_MASKING_REJECTED_LINES,
            "maskingRejectedLines.equals=" + UPDATED_MASKING_REJECTED_LINES
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingRejectedLinesIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingRejectedLines in
        defaultImportedStatementFiltering(
            "maskingRejectedLines.in=" + DEFAULT_MASKING_REJECTED_LINES + "," + UPDATED_MASKING_REJECTED_LINES,
            "maskingRejectedLines.in=" + UPDATED_MASKING_REJECTED_LINES
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingRejectedLinesIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingRejectedLines is not null
        defaultImportedStatementFiltering("maskingRejectedLines.specified=true", "maskingRejectedLines.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingRejectedLinesIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingRejectedLines is greater than or equal to
        defaultImportedStatementFiltering(
            "maskingRejectedLines.greaterThanOrEqual=" + DEFAULT_MASKING_REJECTED_LINES,
            "maskingRejectedLines.greaterThanOrEqual=" + UPDATED_MASKING_REJECTED_LINES
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingRejectedLinesIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingRejectedLines is less than or equal to
        defaultImportedStatementFiltering(
            "maskingRejectedLines.lessThanOrEqual=" + DEFAULT_MASKING_REJECTED_LINES,
            "maskingRejectedLines.lessThanOrEqual=" + SMALLER_MASKING_REJECTED_LINES
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingRejectedLinesIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingRejectedLines is less than
        defaultImportedStatementFiltering(
            "maskingRejectedLines.lessThan=" + UPDATED_MASKING_REJECTED_LINES,
            "maskingRejectedLines.lessThan=" + DEFAULT_MASKING_REJECTED_LINES
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByMaskingRejectedLinesIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where maskingRejectedLines is greater than
        defaultImportedStatementFiltering(
            "maskingRejectedLines.greaterThan=" + SMALLER_MASKING_REJECTED_LINES,
            "maskingRejectedLines.greaterThan=" + DEFAULT_MASKING_REJECTED_LINES
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
    void getAllImportedStatementsByLineCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where lineCount equals to
        defaultImportedStatementFiltering("lineCount.equals=" + DEFAULT_LINE_COUNT, "lineCount.equals=" + UPDATED_LINE_COUNT);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLineCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where lineCount in
        defaultImportedStatementFiltering(
            "lineCount.in=" + DEFAULT_LINE_COUNT + "," + UPDATED_LINE_COUNT,
            "lineCount.in=" + UPDATED_LINE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLineCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where lineCount is not null
        defaultImportedStatementFiltering("lineCount.specified=true", "lineCount.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLineCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where lineCount is greater than or equal to
        defaultImportedStatementFiltering(
            "lineCount.greaterThanOrEqual=" + DEFAULT_LINE_COUNT,
            "lineCount.greaterThanOrEqual=" + UPDATED_LINE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLineCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where lineCount is less than or equal to
        defaultImportedStatementFiltering(
            "lineCount.lessThanOrEqual=" + DEFAULT_LINE_COUNT,
            "lineCount.lessThanOrEqual=" + SMALLER_LINE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLineCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where lineCount is less than
        defaultImportedStatementFiltering("lineCount.lessThan=" + UPDATED_LINE_COUNT, "lineCount.lessThan=" + DEFAULT_LINE_COUNT);
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLineCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where lineCount is greater than
        defaultImportedStatementFiltering("lineCount.greaterThan=" + SMALLER_LINE_COUNT, "lineCount.greaterThan=" + DEFAULT_LINE_COUNT);
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
    void getAllImportedStatementsByLinesRetainedUntilIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where linesRetainedUntil equals to
        defaultImportedStatementFiltering(
            "linesRetainedUntil.equals=" + DEFAULT_LINES_RETAINED_UNTIL,
            "linesRetainedUntil.equals=" + UPDATED_LINES_RETAINED_UNTIL
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLinesRetainedUntilIsInShouldWork() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where linesRetainedUntil in
        defaultImportedStatementFiltering(
            "linesRetainedUntil.in=" + DEFAULT_LINES_RETAINED_UNTIL + "," + UPDATED_LINES_RETAINED_UNTIL,
            "linesRetainedUntil.in=" + UPDATED_LINES_RETAINED_UNTIL
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLinesRetainedUntilIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where linesRetainedUntil is not null
        defaultImportedStatementFiltering("linesRetainedUntil.specified=true", "linesRetainedUntil.specified=false");
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLinesRetainedUntilIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where linesRetainedUntil is greater than or equal to
        defaultImportedStatementFiltering(
            "linesRetainedUntil.greaterThanOrEqual=" + DEFAULT_LINES_RETAINED_UNTIL,
            "linesRetainedUntil.greaterThanOrEqual=" + UPDATED_LINES_RETAINED_UNTIL
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLinesRetainedUntilIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where linesRetainedUntil is less than or equal to
        defaultImportedStatementFiltering(
            "linesRetainedUntil.lessThanOrEqual=" + DEFAULT_LINES_RETAINED_UNTIL,
            "linesRetainedUntil.lessThanOrEqual=" + SMALLER_LINES_RETAINED_UNTIL
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLinesRetainedUntilIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where linesRetainedUntil is less than
        defaultImportedStatementFiltering(
            "linesRetainedUntil.lessThan=" + UPDATED_LINES_RETAINED_UNTIL,
            "linesRetainedUntil.lessThan=" + DEFAULT_LINES_RETAINED_UNTIL
        );
    }

    @Test
    @Transactional
    void getAllImportedStatementsByLinesRetainedUntilIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedImportedStatement = importedStatementRepository.saveAndFlush(importedStatement);

        // Get all the importedStatementList where linesRetainedUntil is greater than
        defaultImportedStatementFiltering(
            "linesRetainedUntil.greaterThan=" + SMALLER_LINES_RETAINED_UNTIL,
            "linesRetainedUntil.greaterThan=" + DEFAULT_LINES_RETAINED_UNTIL
        );
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
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].businessDate").value(hasItem(DEFAULT_BUSINESS_DATE.toString())))
            .andExpect(jsonPath("$.[*].importedAt").value(hasItem(DEFAULT_IMPORTED_AT.toString())))
            .andExpect(jsonPath("$.[*].importerId").value(hasItem(DEFAULT_IMPORTER_ID.toString())))
            .andExpect(jsonPath("$.[*].format").value(hasItem(DEFAULT_FORMAT.toString())))
            .andExpect(jsonPath("$.[*].digest").value(hasItem(DEFAULT_DIGEST)))
            .andExpect(jsonPath("$.[*].maskingPassed").value(hasItem(DEFAULT_MASKING_PASSED)))
            .andExpect(jsonPath("$.[*].maskingRejectedLines").value(hasItem(DEFAULT_MASKING_REJECTED_LINES)))
            .andExpect(jsonPath("$.[*].maskingCheckedAt").value(hasItem(DEFAULT_MASKING_CHECKED_AT.toString())))
            .andExpect(jsonPath("$.[*].lineCount").value(hasItem(DEFAULT_LINE_COUNT)))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())))
            .andExpect(jsonPath("$.[*].linesRetainedUntil").value(hasItem(DEFAULT_LINES_RETAINED_UNTIL.toString())));

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
