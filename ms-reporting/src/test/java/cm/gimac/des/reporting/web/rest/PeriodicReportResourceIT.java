package cm.gimac.des.reporting.web.rest;

import static cm.gimac.des.reporting.domain.PeriodicReportAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.reporting.IntegrationTest;
import cm.gimac.des.reporting.domain.PeriodicReport;
import cm.gimac.des.reporting.domain.enumeration.Periodicity;
import cm.gimac.des.reporting.domain.enumeration.ReportState;
import cm.gimac.des.reporting.repository.PeriodicReportRepository;
import cm.gimac.des.reporting.service.mapper.PeriodicReportMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
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
 * Integration tests for the {@link PeriodicReportResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class PeriodicReportResourceIT {

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final Periodicity DEFAULT_PERIODICITY = Periodicity.HEBDOMADAIRE;
    private static final Periodicity UPDATED_PERIODICITY = Periodicity.MENSUEL;

    private static final String DEFAULT_PERIOD = "9065-53";
    private static final String UPDATED_PERIOD = "4157-06";

    private static final Integer DEFAULT_REPORT_VERSION = 1;
    private static final Integer UPDATED_REPORT_VERSION = 2;
    private static final Integer SMALLER_REPORT_VERSION = 1 - 1;

    private static final Boolean DEFAULT_MANUAL = false;
    private static final Boolean UPDATED_MANUAL = true;

    private static final String DEFAULT_FROZEN_INDICATORS = "AAAAAAAAAA";
    private static final String UPDATED_FROZEN_INDICATORS = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENT_DIGEST = "851b3173d1f64d216b3d2493e7e3eed295f521106355a7dcd0b671fbae3de0c1";
    private static final String UPDATED_CONTENT_DIGEST = "79f6828e11150aa77f02f0cf7c090d6c5b27542428219933f56d730ca0c7a9a7";

    private static final Instant DEFAULT_GENERATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_GENERATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_SIGNER_ID = UUID.randomUUID();
    private static final UUID UPDATED_SIGNER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_SIGNED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SIGNED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_DOCUMENT_REF = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_REF = UUID.randomUUID();

    private static final Instant DEFAULT_NOTIFIED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_NOTIFIED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final ReportState DEFAULT_STATE = ReportState.GENERE;
    private static final ReportState UPDATED_STATE = ReportState.SIGNE;

    private static final String ENTITY_API_URL = "/api/periodic-reports";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PeriodicReportRepository periodicReportRepository;

    @Autowired
    private PeriodicReportMapper periodicReportMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPeriodicReportMockMvc;

    private PeriodicReport periodicReport;

    private PeriodicReport insertedPeriodicReport;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PeriodicReport createEntity() {
        return new PeriodicReport()
            .serviceCode(DEFAULT_SERVICE_CODE)
            .periodicity(DEFAULT_PERIODICITY)
            .period(DEFAULT_PERIOD)
            .reportVersion(DEFAULT_REPORT_VERSION)
            .manual(DEFAULT_MANUAL)
            .frozenIndicators(DEFAULT_FROZEN_INDICATORS)
            .contentDigest(DEFAULT_CONTENT_DIGEST)
            .generatedAt(DEFAULT_GENERATED_AT)
            .signerId(DEFAULT_SIGNER_ID)
            .signedAt(DEFAULT_SIGNED_AT)
            .documentRef(DEFAULT_DOCUMENT_REF)
            .notifiedAt(DEFAULT_NOTIFIED_AT)
            .state(DEFAULT_STATE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PeriodicReport createUpdatedEntity() {
        return new PeriodicReport()
            .serviceCode(UPDATED_SERVICE_CODE)
            .periodicity(UPDATED_PERIODICITY)
            .period(UPDATED_PERIOD)
            .reportVersion(UPDATED_REPORT_VERSION)
            .manual(UPDATED_MANUAL)
            .frozenIndicators(UPDATED_FROZEN_INDICATORS)
            .contentDigest(UPDATED_CONTENT_DIGEST)
            .generatedAt(UPDATED_GENERATED_AT)
            .signerId(UPDATED_SIGNER_ID)
            .signedAt(UPDATED_SIGNED_AT)
            .documentRef(UPDATED_DOCUMENT_REF)
            .notifiedAt(UPDATED_NOTIFIED_AT)
            .state(UPDATED_STATE);
    }

    @BeforeEach
    void initTest() {
        periodicReport = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedPeriodicReport != null) {
            periodicReportRepository.delete(insertedPeriodicReport);
            insertedPeriodicReport = null;
        }
    }

    @Test
    @Transactional
    void getAllPeriodicReports() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList
        restPeriodicReportMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(periodicReport.getId().toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].periodicity").value(hasItem(DEFAULT_PERIODICITY.toString())))
            .andExpect(jsonPath("$.[*].period").value(hasItem(DEFAULT_PERIOD)))
            .andExpect(jsonPath("$.[*].reportVersion").value(hasItem(DEFAULT_REPORT_VERSION)))
            .andExpect(jsonPath("$.[*].manual").value(hasItem(DEFAULT_MANUAL)))
            .andExpect(jsonPath("$.[*].frozenIndicators").value(hasItem(DEFAULT_FROZEN_INDICATORS)))
            .andExpect(jsonPath("$.[*].contentDigest").value(hasItem(DEFAULT_CONTENT_DIGEST)))
            .andExpect(jsonPath("$.[*].generatedAt").value(hasItem(DEFAULT_GENERATED_AT.toString())))
            .andExpect(jsonPath("$.[*].signerId").value(hasItem(DEFAULT_SIGNER_ID.toString())))
            .andExpect(jsonPath("$.[*].signedAt").value(hasItem(DEFAULT_SIGNED_AT.toString())))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())))
            .andExpect(jsonPath("$.[*].notifiedAt").value(hasItem(DEFAULT_NOTIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())));
    }

    @Test
    @Transactional
    void getPeriodicReport() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get the periodicReport
        restPeriodicReportMockMvc
            .perform(get(ENTITY_API_URL_ID, periodicReport.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(periodicReport.getId().toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.periodicity").value(DEFAULT_PERIODICITY.toString()))
            .andExpect(jsonPath("$.period").value(DEFAULT_PERIOD))
            .andExpect(jsonPath("$.reportVersion").value(DEFAULT_REPORT_VERSION))
            .andExpect(jsonPath("$.manual").value(DEFAULT_MANUAL))
            .andExpect(jsonPath("$.frozenIndicators").value(DEFAULT_FROZEN_INDICATORS))
            .andExpect(jsonPath("$.contentDigest").value(DEFAULT_CONTENT_DIGEST))
            .andExpect(jsonPath("$.generatedAt").value(DEFAULT_GENERATED_AT.toString()))
            .andExpect(jsonPath("$.signerId").value(DEFAULT_SIGNER_ID.toString()))
            .andExpect(jsonPath("$.signedAt").value(DEFAULT_SIGNED_AT.toString()))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF.toString()))
            .andExpect(jsonPath("$.notifiedAt").value(DEFAULT_NOTIFIED_AT.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()));
    }

    @Test
    @Transactional
    void getPeriodicReportsByIdFiltering() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        UUID id = periodicReport.getId();

        defaultPeriodicReportFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByServiceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where serviceCode equals to
        defaultPeriodicReportFiltering("serviceCode.equals=" + DEFAULT_SERVICE_CODE, "serviceCode.equals=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByServiceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where serviceCode in
        defaultPeriodicReportFiltering(
            "serviceCode.in=" + DEFAULT_SERVICE_CODE + "," + UPDATED_SERVICE_CODE,
            "serviceCode.in=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByServiceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where serviceCode is not null
        defaultPeriodicReportFiltering("serviceCode.specified=true", "serviceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByServiceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where serviceCode contains
        defaultPeriodicReportFiltering("serviceCode.contains=" + DEFAULT_SERVICE_CODE, "serviceCode.contains=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByServiceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where serviceCode does not contain
        defaultPeriodicReportFiltering(
            "serviceCode.doesNotContain=" + UPDATED_SERVICE_CODE,
            "serviceCode.doesNotContain=" + DEFAULT_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByPeriodicityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where periodicity equals to
        defaultPeriodicReportFiltering("periodicity.equals=" + DEFAULT_PERIODICITY, "periodicity.equals=" + UPDATED_PERIODICITY);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByPeriodicityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where periodicity in
        defaultPeriodicReportFiltering(
            "periodicity.in=" + DEFAULT_PERIODICITY + "," + UPDATED_PERIODICITY,
            "periodicity.in=" + UPDATED_PERIODICITY
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByPeriodicityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where periodicity is not null
        defaultPeriodicReportFiltering("periodicity.specified=true", "periodicity.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByPeriodIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where period equals to
        defaultPeriodicReportFiltering("period.equals=" + DEFAULT_PERIOD, "period.equals=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByPeriodIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where period in
        defaultPeriodicReportFiltering("period.in=" + DEFAULT_PERIOD + "," + UPDATED_PERIOD, "period.in=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByPeriodIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where period is not null
        defaultPeriodicReportFiltering("period.specified=true", "period.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByPeriodContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where period contains
        defaultPeriodicReportFiltering("period.contains=" + DEFAULT_PERIOD, "period.contains=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByPeriodNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where period does not contain
        defaultPeriodicReportFiltering("period.doesNotContain=" + UPDATED_PERIOD, "period.doesNotContain=" + DEFAULT_PERIOD);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByReportVersionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where reportVersion equals to
        defaultPeriodicReportFiltering("reportVersion.equals=" + DEFAULT_REPORT_VERSION, "reportVersion.equals=" + UPDATED_REPORT_VERSION);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByReportVersionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where reportVersion in
        defaultPeriodicReportFiltering(
            "reportVersion.in=" + DEFAULT_REPORT_VERSION + "," + UPDATED_REPORT_VERSION,
            "reportVersion.in=" + UPDATED_REPORT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByReportVersionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where reportVersion is not null
        defaultPeriodicReportFiltering("reportVersion.specified=true", "reportVersion.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByReportVersionIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where reportVersion is greater than or equal to
        defaultPeriodicReportFiltering(
            "reportVersion.greaterThanOrEqual=" + DEFAULT_REPORT_VERSION,
            "reportVersion.greaterThanOrEqual=" + UPDATED_REPORT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByReportVersionIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where reportVersion is less than or equal to
        defaultPeriodicReportFiltering(
            "reportVersion.lessThanOrEqual=" + DEFAULT_REPORT_VERSION,
            "reportVersion.lessThanOrEqual=" + SMALLER_REPORT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByReportVersionIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where reportVersion is less than
        defaultPeriodicReportFiltering(
            "reportVersion.lessThan=" + UPDATED_REPORT_VERSION,
            "reportVersion.lessThan=" + DEFAULT_REPORT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByReportVersionIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where reportVersion is greater than
        defaultPeriodicReportFiltering(
            "reportVersion.greaterThan=" + SMALLER_REPORT_VERSION,
            "reportVersion.greaterThan=" + DEFAULT_REPORT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByManualIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where manual equals to
        defaultPeriodicReportFiltering("manual.equals=" + DEFAULT_MANUAL, "manual.equals=" + UPDATED_MANUAL);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByManualIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where manual in
        defaultPeriodicReportFiltering("manual.in=" + DEFAULT_MANUAL + "," + UPDATED_MANUAL, "manual.in=" + UPDATED_MANUAL);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByManualIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where manual is not null
        defaultPeriodicReportFiltering("manual.specified=true", "manual.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByContentDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where contentDigest equals to
        defaultPeriodicReportFiltering("contentDigest.equals=" + DEFAULT_CONTENT_DIGEST, "contentDigest.equals=" + UPDATED_CONTENT_DIGEST);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByContentDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where contentDigest in
        defaultPeriodicReportFiltering(
            "contentDigest.in=" + DEFAULT_CONTENT_DIGEST + "," + UPDATED_CONTENT_DIGEST,
            "contentDigest.in=" + UPDATED_CONTENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByContentDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where contentDigest is not null
        defaultPeriodicReportFiltering("contentDigest.specified=true", "contentDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByContentDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where contentDigest contains
        defaultPeriodicReportFiltering(
            "contentDigest.contains=" + DEFAULT_CONTENT_DIGEST,
            "contentDigest.contains=" + UPDATED_CONTENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByContentDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where contentDigest does not contain
        defaultPeriodicReportFiltering(
            "contentDigest.doesNotContain=" + UPDATED_CONTENT_DIGEST,
            "contentDigest.doesNotContain=" + DEFAULT_CONTENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByGeneratedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where generatedAt equals to
        defaultPeriodicReportFiltering("generatedAt.equals=" + DEFAULT_GENERATED_AT, "generatedAt.equals=" + UPDATED_GENERATED_AT);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByGeneratedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where generatedAt in
        defaultPeriodicReportFiltering(
            "generatedAt.in=" + DEFAULT_GENERATED_AT + "," + UPDATED_GENERATED_AT,
            "generatedAt.in=" + UPDATED_GENERATED_AT
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByGeneratedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where generatedAt is not null
        defaultPeriodicReportFiltering("generatedAt.specified=true", "generatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodicReportsBySignerIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where signerId equals to
        defaultPeriodicReportFiltering("signerId.equals=" + DEFAULT_SIGNER_ID, "signerId.equals=" + UPDATED_SIGNER_ID);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsBySignerIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where signerId in
        defaultPeriodicReportFiltering("signerId.in=" + DEFAULT_SIGNER_ID + "," + UPDATED_SIGNER_ID, "signerId.in=" + UPDATED_SIGNER_ID);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsBySignerIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where signerId is not null
        defaultPeriodicReportFiltering("signerId.specified=true", "signerId.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodicReportsBySignedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where signedAt equals to
        defaultPeriodicReportFiltering("signedAt.equals=" + DEFAULT_SIGNED_AT, "signedAt.equals=" + UPDATED_SIGNED_AT);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsBySignedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where signedAt in
        defaultPeriodicReportFiltering("signedAt.in=" + DEFAULT_SIGNED_AT + "," + UPDATED_SIGNED_AT, "signedAt.in=" + UPDATED_SIGNED_AT);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsBySignedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where signedAt is not null
        defaultPeriodicReportFiltering("signedAt.specified=true", "signedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByDocumentRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where documentRef equals to
        defaultPeriodicReportFiltering("documentRef.equals=" + DEFAULT_DOCUMENT_REF, "documentRef.equals=" + UPDATED_DOCUMENT_REF);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByDocumentRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where documentRef in
        defaultPeriodicReportFiltering(
            "documentRef.in=" + DEFAULT_DOCUMENT_REF + "," + UPDATED_DOCUMENT_REF,
            "documentRef.in=" + UPDATED_DOCUMENT_REF
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByDocumentRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where documentRef is not null
        defaultPeriodicReportFiltering("documentRef.specified=true", "documentRef.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByNotifiedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where notifiedAt equals to
        defaultPeriodicReportFiltering("notifiedAt.equals=" + DEFAULT_NOTIFIED_AT, "notifiedAt.equals=" + UPDATED_NOTIFIED_AT);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByNotifiedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where notifiedAt in
        defaultPeriodicReportFiltering(
            "notifiedAt.in=" + DEFAULT_NOTIFIED_AT + "," + UPDATED_NOTIFIED_AT,
            "notifiedAt.in=" + UPDATED_NOTIFIED_AT
        );
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByNotifiedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where notifiedAt is not null
        defaultPeriodicReportFiltering("notifiedAt.specified=true", "notifiedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where state equals to
        defaultPeriodicReportFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where state in
        defaultPeriodicReportFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllPeriodicReportsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodicReport = periodicReportRepository.saveAndFlush(periodicReport);

        // Get all the periodicReportList where state is not null
        defaultPeriodicReportFiltering("state.specified=true", "state.specified=false");
    }

    private void defaultPeriodicReportFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultPeriodicReportShouldBeFound(shouldBeFound);
        defaultPeriodicReportShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultPeriodicReportShouldBeFound(String filter) throws Exception {
        restPeriodicReportMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(periodicReport.getId().toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].periodicity").value(hasItem(DEFAULT_PERIODICITY.toString())))
            .andExpect(jsonPath("$.[*].period").value(hasItem(DEFAULT_PERIOD)))
            .andExpect(jsonPath("$.[*].reportVersion").value(hasItem(DEFAULT_REPORT_VERSION)))
            .andExpect(jsonPath("$.[*].manual").value(hasItem(DEFAULT_MANUAL)))
            .andExpect(jsonPath("$.[*].frozenIndicators").value(hasItem(DEFAULT_FROZEN_INDICATORS)))
            .andExpect(jsonPath("$.[*].contentDigest").value(hasItem(DEFAULT_CONTENT_DIGEST)))
            .andExpect(jsonPath("$.[*].generatedAt").value(hasItem(DEFAULT_GENERATED_AT.toString())))
            .andExpect(jsonPath("$.[*].signerId").value(hasItem(DEFAULT_SIGNER_ID.toString())))
            .andExpect(jsonPath("$.[*].signedAt").value(hasItem(DEFAULT_SIGNED_AT.toString())))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())))
            .andExpect(jsonPath("$.[*].notifiedAt").value(hasItem(DEFAULT_NOTIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())));

        // Check, that the count call also returns 1
        restPeriodicReportMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultPeriodicReportShouldNotBeFound(String filter) throws Exception {
        restPeriodicReportMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restPeriodicReportMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingPeriodicReport() throws Exception {
        // Get the periodicReport
        restPeriodicReportMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return periodicReportRepository.count();
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

    protected PeriodicReport getPersistedPeriodicReport(PeriodicReport periodicReport) {
        return periodicReportRepository.findById(periodicReport.getId()).orElseThrow();
    }

    protected void assertPersistedPeriodicReportToMatchAllProperties(PeriodicReport expectedPeriodicReport) {
        assertPeriodicReportAllPropertiesEquals(expectedPeriodicReport, getPersistedPeriodicReport(expectedPeriodicReport));
    }

    protected void assertPersistedPeriodicReportToMatchUpdatableProperties(PeriodicReport expectedPeriodicReport) {
        assertPeriodicReportAllUpdatablePropertiesEquals(expectedPeriodicReport, getPersistedPeriodicReport(expectedPeriodicReport));
    }
}
