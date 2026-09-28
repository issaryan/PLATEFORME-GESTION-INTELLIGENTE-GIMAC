package cm.gimac.des.reporting.web.rest;

import static cm.gimac.des.reporting.domain.DataExportAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.reporting.IntegrationTest;
import cm.gimac.des.reporting.domain.DataExport;
import cm.gimac.des.reporting.repository.DataExportRepository;
import cm.gimac.des.reporting.service.mapper.DataExportMapper;
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
 * Integration tests for the {@link DataExportResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DataExportResourceIT {

    private static final UUID DEFAULT_REQUESTER_ID = UUID.randomUUID();
    private static final UUID UPDATED_REQUESTER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_REQUESTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REQUESTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_INDICATOR_CODES = "AAAAAAAAAA";
    private static final String UPDATED_INDICATOR_CODES = "BBBBBBBBBB";

    private static final String DEFAULT_PERIOD = "AAAAAAAAAA";
    private static final String UPDATED_PERIOD = "BBBBBBBBBB";

    private static final String DEFAULT_DIMENSION_FILTER = "AAAAAAAAAA";
    private static final String UPDATED_DIMENSION_FILTER = "BBBBBBBBBB";

    private static final Integer DEFAULT_ROW_COUNT = 0;
    private static final Integer UPDATED_ROW_COUNT = 1;
    private static final Integer SMALLER_ROW_COUNT = 0 - 1;

    private static final String DEFAULT_WATERMARK = "AAAAAAAAAA";
    private static final String UPDATED_WATERMARK = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENT_DIGEST = "ae9800926ccdda35ced7656f0c0dabfe8bf2c2dfee407b9e755564651be2f601";
    private static final String UPDATED_CONTENT_DIGEST = "d039a47651a8ca5f9b5185340503a0ebe590753cf52a4c15f7262320eb206ad7";

    private static final UUID DEFAULT_DOCUMENT_REF = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_REF = UUID.randomUUID();

    private static final String ENTITY_API_URL = "/api/data-exports";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DataExportRepository dataExportRepository;

    @Autowired
    private DataExportMapper dataExportMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDataExportMockMvc;

    private DataExport dataExport;

    private DataExport insertedDataExport;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DataExport createEntity() {
        return new DataExport()
            .requesterId(DEFAULT_REQUESTER_ID)
            .requestedAt(DEFAULT_REQUESTED_AT)
            .indicatorCodes(DEFAULT_INDICATOR_CODES)
            .period(DEFAULT_PERIOD)
            .dimensionFilter(DEFAULT_DIMENSION_FILTER)
            .rowCount(DEFAULT_ROW_COUNT)
            .watermark(DEFAULT_WATERMARK)
            .contentDigest(DEFAULT_CONTENT_DIGEST)
            .documentRef(DEFAULT_DOCUMENT_REF);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DataExport createUpdatedEntity() {
        return new DataExport()
            .requesterId(UPDATED_REQUESTER_ID)
            .requestedAt(UPDATED_REQUESTED_AT)
            .indicatorCodes(UPDATED_INDICATOR_CODES)
            .period(UPDATED_PERIOD)
            .dimensionFilter(UPDATED_DIMENSION_FILTER)
            .rowCount(UPDATED_ROW_COUNT)
            .watermark(UPDATED_WATERMARK)
            .contentDigest(UPDATED_CONTENT_DIGEST)
            .documentRef(UPDATED_DOCUMENT_REF);
    }

    @BeforeEach
    void initTest() {
        dataExport = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedDataExport != null) {
            dataExportRepository.delete(insertedDataExport);
            insertedDataExport = null;
        }
    }

    @Test
    @Transactional
    void getAllDataExports() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList
        restDataExportMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(dataExport.getId().toString())))
            .andExpect(jsonPath("$.[*].requesterId").value(hasItem(DEFAULT_REQUESTER_ID.toString())))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].indicatorCodes").value(hasItem(DEFAULT_INDICATOR_CODES)))
            .andExpect(jsonPath("$.[*].period").value(hasItem(DEFAULT_PERIOD)))
            .andExpect(jsonPath("$.[*].dimensionFilter").value(hasItem(DEFAULT_DIMENSION_FILTER)))
            .andExpect(jsonPath("$.[*].rowCount").value(hasItem(DEFAULT_ROW_COUNT)))
            .andExpect(jsonPath("$.[*].watermark").value(hasItem(DEFAULT_WATERMARK)))
            .andExpect(jsonPath("$.[*].contentDigest").value(hasItem(DEFAULT_CONTENT_DIGEST)))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())));
    }

    @Test
    @Transactional
    void getDataExport() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get the dataExport
        restDataExportMockMvc
            .perform(get(ENTITY_API_URL_ID, dataExport.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(dataExport.getId().toString()))
            .andExpect(jsonPath("$.requesterId").value(DEFAULT_REQUESTER_ID.toString()))
            .andExpect(jsonPath("$.requestedAt").value(DEFAULT_REQUESTED_AT.toString()))
            .andExpect(jsonPath("$.indicatorCodes").value(DEFAULT_INDICATOR_CODES))
            .andExpect(jsonPath("$.period").value(DEFAULT_PERIOD))
            .andExpect(jsonPath("$.dimensionFilter").value(DEFAULT_DIMENSION_FILTER))
            .andExpect(jsonPath("$.rowCount").value(DEFAULT_ROW_COUNT))
            .andExpect(jsonPath("$.watermark").value(DEFAULT_WATERMARK))
            .andExpect(jsonPath("$.contentDigest").value(DEFAULT_CONTENT_DIGEST))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF.toString()));
    }

    @Test
    @Transactional
    void getDataExportsByIdFiltering() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        UUID id = dataExport.getId();

        defaultDataExportFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllDataExportsByRequesterIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where requesterId equals to
        defaultDataExportFiltering("requesterId.equals=" + DEFAULT_REQUESTER_ID, "requesterId.equals=" + UPDATED_REQUESTER_ID);
    }

    @Test
    @Transactional
    void getAllDataExportsByRequesterIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where requesterId in
        defaultDataExportFiltering(
            "requesterId.in=" + DEFAULT_REQUESTER_ID + "," + UPDATED_REQUESTER_ID,
            "requesterId.in=" + UPDATED_REQUESTER_ID
        );
    }

    @Test
    @Transactional
    void getAllDataExportsByRequesterIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where requesterId is not null
        defaultDataExportFiltering("requesterId.specified=true", "requesterId.specified=false");
    }

    @Test
    @Transactional
    void getAllDataExportsByRequestedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where requestedAt equals to
        defaultDataExportFiltering("requestedAt.equals=" + DEFAULT_REQUESTED_AT, "requestedAt.equals=" + UPDATED_REQUESTED_AT);
    }

    @Test
    @Transactional
    void getAllDataExportsByRequestedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where requestedAt in
        defaultDataExportFiltering(
            "requestedAt.in=" + DEFAULT_REQUESTED_AT + "," + UPDATED_REQUESTED_AT,
            "requestedAt.in=" + UPDATED_REQUESTED_AT
        );
    }

    @Test
    @Transactional
    void getAllDataExportsByRequestedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where requestedAt is not null
        defaultDataExportFiltering("requestedAt.specified=true", "requestedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDataExportsByIndicatorCodesIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where indicatorCodes equals to
        defaultDataExportFiltering("indicatorCodes.equals=" + DEFAULT_INDICATOR_CODES, "indicatorCodes.equals=" + UPDATED_INDICATOR_CODES);
    }

    @Test
    @Transactional
    void getAllDataExportsByIndicatorCodesIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where indicatorCodes in
        defaultDataExportFiltering(
            "indicatorCodes.in=" + DEFAULT_INDICATOR_CODES + "," + UPDATED_INDICATOR_CODES,
            "indicatorCodes.in=" + UPDATED_INDICATOR_CODES
        );
    }

    @Test
    @Transactional
    void getAllDataExportsByIndicatorCodesIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where indicatorCodes is not null
        defaultDataExportFiltering("indicatorCodes.specified=true", "indicatorCodes.specified=false");
    }

    @Test
    @Transactional
    void getAllDataExportsByIndicatorCodesContainsSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where indicatorCodes contains
        defaultDataExportFiltering(
            "indicatorCodes.contains=" + DEFAULT_INDICATOR_CODES,
            "indicatorCodes.contains=" + UPDATED_INDICATOR_CODES
        );
    }

    @Test
    @Transactional
    void getAllDataExportsByIndicatorCodesNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where indicatorCodes does not contain
        defaultDataExportFiltering(
            "indicatorCodes.doesNotContain=" + UPDATED_INDICATOR_CODES,
            "indicatorCodes.doesNotContain=" + DEFAULT_INDICATOR_CODES
        );
    }

    @Test
    @Transactional
    void getAllDataExportsByPeriodIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where period equals to
        defaultDataExportFiltering("period.equals=" + DEFAULT_PERIOD, "period.equals=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllDataExportsByPeriodIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where period in
        defaultDataExportFiltering("period.in=" + DEFAULT_PERIOD + "," + UPDATED_PERIOD, "period.in=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllDataExportsByPeriodIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where period is not null
        defaultDataExportFiltering("period.specified=true", "period.specified=false");
    }

    @Test
    @Transactional
    void getAllDataExportsByPeriodContainsSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where period contains
        defaultDataExportFiltering("period.contains=" + DEFAULT_PERIOD, "period.contains=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllDataExportsByPeriodNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where period does not contain
        defaultDataExportFiltering("period.doesNotContain=" + UPDATED_PERIOD, "period.doesNotContain=" + DEFAULT_PERIOD);
    }

    @Test
    @Transactional
    void getAllDataExportsByRowCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where rowCount equals to
        defaultDataExportFiltering("rowCount.equals=" + DEFAULT_ROW_COUNT, "rowCount.equals=" + UPDATED_ROW_COUNT);
    }

    @Test
    @Transactional
    void getAllDataExportsByRowCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where rowCount in
        defaultDataExportFiltering("rowCount.in=" + DEFAULT_ROW_COUNT + "," + UPDATED_ROW_COUNT, "rowCount.in=" + UPDATED_ROW_COUNT);
    }

    @Test
    @Transactional
    void getAllDataExportsByRowCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where rowCount is not null
        defaultDataExportFiltering("rowCount.specified=true", "rowCount.specified=false");
    }

    @Test
    @Transactional
    void getAllDataExportsByRowCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where rowCount is greater than or equal to
        defaultDataExportFiltering("rowCount.greaterThanOrEqual=" + DEFAULT_ROW_COUNT, "rowCount.greaterThanOrEqual=" + UPDATED_ROW_COUNT);
    }

    @Test
    @Transactional
    void getAllDataExportsByRowCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where rowCount is less than or equal to
        defaultDataExportFiltering("rowCount.lessThanOrEqual=" + DEFAULT_ROW_COUNT, "rowCount.lessThanOrEqual=" + SMALLER_ROW_COUNT);
    }

    @Test
    @Transactional
    void getAllDataExportsByRowCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where rowCount is less than
        defaultDataExportFiltering("rowCount.lessThan=" + UPDATED_ROW_COUNT, "rowCount.lessThan=" + DEFAULT_ROW_COUNT);
    }

    @Test
    @Transactional
    void getAllDataExportsByRowCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where rowCount is greater than
        defaultDataExportFiltering("rowCount.greaterThan=" + SMALLER_ROW_COUNT, "rowCount.greaterThan=" + DEFAULT_ROW_COUNT);
    }

    @Test
    @Transactional
    void getAllDataExportsByWatermarkIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where watermark equals to
        defaultDataExportFiltering("watermark.equals=" + DEFAULT_WATERMARK, "watermark.equals=" + UPDATED_WATERMARK);
    }

    @Test
    @Transactional
    void getAllDataExportsByWatermarkIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where watermark in
        defaultDataExportFiltering("watermark.in=" + DEFAULT_WATERMARK + "," + UPDATED_WATERMARK, "watermark.in=" + UPDATED_WATERMARK);
    }

    @Test
    @Transactional
    void getAllDataExportsByWatermarkIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where watermark is not null
        defaultDataExportFiltering("watermark.specified=true", "watermark.specified=false");
    }

    @Test
    @Transactional
    void getAllDataExportsByWatermarkContainsSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where watermark contains
        defaultDataExportFiltering("watermark.contains=" + DEFAULT_WATERMARK, "watermark.contains=" + UPDATED_WATERMARK);
    }

    @Test
    @Transactional
    void getAllDataExportsByWatermarkNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where watermark does not contain
        defaultDataExportFiltering("watermark.doesNotContain=" + UPDATED_WATERMARK, "watermark.doesNotContain=" + DEFAULT_WATERMARK);
    }

    @Test
    @Transactional
    void getAllDataExportsByContentDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where contentDigest equals to
        defaultDataExportFiltering("contentDigest.equals=" + DEFAULT_CONTENT_DIGEST, "contentDigest.equals=" + UPDATED_CONTENT_DIGEST);
    }

    @Test
    @Transactional
    void getAllDataExportsByContentDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where contentDigest in
        defaultDataExportFiltering(
            "contentDigest.in=" + DEFAULT_CONTENT_DIGEST + "," + UPDATED_CONTENT_DIGEST,
            "contentDigest.in=" + UPDATED_CONTENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllDataExportsByContentDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where contentDigest is not null
        defaultDataExportFiltering("contentDigest.specified=true", "contentDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllDataExportsByContentDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where contentDigest contains
        defaultDataExportFiltering("contentDigest.contains=" + DEFAULT_CONTENT_DIGEST, "contentDigest.contains=" + UPDATED_CONTENT_DIGEST);
    }

    @Test
    @Transactional
    void getAllDataExportsByContentDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where contentDigest does not contain
        defaultDataExportFiltering(
            "contentDigest.doesNotContain=" + UPDATED_CONTENT_DIGEST,
            "contentDigest.doesNotContain=" + DEFAULT_CONTENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllDataExportsByDocumentRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where documentRef equals to
        defaultDataExportFiltering("documentRef.equals=" + DEFAULT_DOCUMENT_REF, "documentRef.equals=" + UPDATED_DOCUMENT_REF);
    }

    @Test
    @Transactional
    void getAllDataExportsByDocumentRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where documentRef in
        defaultDataExportFiltering(
            "documentRef.in=" + DEFAULT_DOCUMENT_REF + "," + UPDATED_DOCUMENT_REF,
            "documentRef.in=" + UPDATED_DOCUMENT_REF
        );
    }

    @Test
    @Transactional
    void getAllDataExportsByDocumentRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDataExport = dataExportRepository.saveAndFlush(dataExport);

        // Get all the dataExportList where documentRef is not null
        defaultDataExportFiltering("documentRef.specified=true", "documentRef.specified=false");
    }

    private void defaultDataExportFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultDataExportShouldBeFound(shouldBeFound);
        defaultDataExportShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultDataExportShouldBeFound(String filter) throws Exception {
        restDataExportMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(dataExport.getId().toString())))
            .andExpect(jsonPath("$.[*].requesterId").value(hasItem(DEFAULT_REQUESTER_ID.toString())))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].indicatorCodes").value(hasItem(DEFAULT_INDICATOR_CODES)))
            .andExpect(jsonPath("$.[*].period").value(hasItem(DEFAULT_PERIOD)))
            .andExpect(jsonPath("$.[*].dimensionFilter").value(hasItem(DEFAULT_DIMENSION_FILTER)))
            .andExpect(jsonPath("$.[*].rowCount").value(hasItem(DEFAULT_ROW_COUNT)))
            .andExpect(jsonPath("$.[*].watermark").value(hasItem(DEFAULT_WATERMARK)))
            .andExpect(jsonPath("$.[*].contentDigest").value(hasItem(DEFAULT_CONTENT_DIGEST)))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())));

        // Check, that the count call also returns 1
        restDataExportMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultDataExportShouldNotBeFound(String filter) throws Exception {
        restDataExportMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restDataExportMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingDataExport() throws Exception {
        // Get the dataExport
        restDataExportMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return dataExportRepository.count();
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

    protected DataExport getPersistedDataExport(DataExport dataExport) {
        return dataExportRepository.findById(dataExport.getId()).orElseThrow();
    }

    protected void assertPersistedDataExportToMatchAllProperties(DataExport expectedDataExport) {
        assertDataExportAllPropertiesEquals(expectedDataExport, getPersistedDataExport(expectedDataExport));
    }

    protected void assertPersistedDataExportToMatchUpdatableProperties(DataExport expectedDataExport) {
        assertDataExportAllUpdatablePropertiesEquals(expectedDataExport, getPersistedDataExport(expectedDataExport));
    }
}
