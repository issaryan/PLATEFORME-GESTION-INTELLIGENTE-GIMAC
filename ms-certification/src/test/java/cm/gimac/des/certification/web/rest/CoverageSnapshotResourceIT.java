package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.CoverageSnapshotAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.CoverageSnapshot;
import cm.gimac.des.certification.repository.CoverageSnapshotRepository;
import cm.gimac.des.certification.service.mapper.CoverageSnapshotMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Integration tests for the {@link CoverageSnapshotResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class CoverageSnapshotResourceIT {

    private static final UUID DEFAULT_LIAISON_ID = UUID.randomUUID();
    private static final UUID UPDATED_LIAISON_ID = UUID.randomUUID();

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final Boolean DEFAULT_INCLUDED = false;
    private static final Boolean UPDATED_INCLUDED = true;

    private static final BigDecimal DEFAULT_CERTIFICATION_RATE = new BigDecimal(0);
    private static final BigDecimal UPDATED_CERTIFICATION_RATE = new BigDecimal(1);
    private static final BigDecimal SMALLER_CERTIFICATION_RATE = new BigDecimal(0 - 1);

    private static final BigDecimal DEFAULT_COMMISSIONING_RATE = new BigDecimal(0);
    private static final BigDecimal UPDATED_COMMISSIONING_RATE = new BigDecimal(1);
    private static final BigDecimal SMALLER_COMMISSIONING_RATE = new BigDecimal(0 - 1);

    private static final UUID DEFAULT_SOURCE_EVENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_SOURCE_EVENT_ID = UUID.randomUUID();

    private static final Instant DEFAULT_COMPUTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_COMPUTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_RECEIVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RECEIVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/coverage-snapshots";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CoverageSnapshotRepository coverageSnapshotRepository;

    @Autowired
    private CoverageSnapshotMapper coverageSnapshotMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCoverageSnapshotMockMvc;

    private CoverageSnapshot coverageSnapshot;

    private CoverageSnapshot insertedCoverageSnapshot;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CoverageSnapshot createEntity() {
        return new CoverageSnapshot()
            .liaisonId(DEFAULT_LIAISON_ID)
            .serviceCode(DEFAULT_SERVICE_CODE)
            .included(DEFAULT_INCLUDED)
            .certificationRate(DEFAULT_CERTIFICATION_RATE)
            .commissioningRate(DEFAULT_COMMISSIONING_RATE)
            .sourceEventId(DEFAULT_SOURCE_EVENT_ID)
            .computedAt(DEFAULT_COMPUTED_AT)
            .receivedAt(DEFAULT_RECEIVED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CoverageSnapshot createUpdatedEntity() {
        return new CoverageSnapshot()
            .liaisonId(UPDATED_LIAISON_ID)
            .serviceCode(UPDATED_SERVICE_CODE)
            .included(UPDATED_INCLUDED)
            .certificationRate(UPDATED_CERTIFICATION_RATE)
            .commissioningRate(UPDATED_COMMISSIONING_RATE)
            .sourceEventId(UPDATED_SOURCE_EVENT_ID)
            .computedAt(UPDATED_COMPUTED_AT)
            .receivedAt(UPDATED_RECEIVED_AT);
    }

    @BeforeEach
    void initTest() {
        coverageSnapshot = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCoverageSnapshot != null) {
            coverageSnapshotRepository.delete(insertedCoverageSnapshot);
            insertedCoverageSnapshot = null;
        }
    }

    @Test
    @Transactional
    void getAllCoverageSnapshots() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList
        restCoverageSnapshotMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(coverageSnapshot.getId().toString())))
            .andExpect(jsonPath("$.[*].liaisonId").value(hasItem(DEFAULT_LIAISON_ID.toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].included").value(hasItem(DEFAULT_INCLUDED)))
            .andExpect(jsonPath("$.[*].certificationRate").value(hasItem(sameNumber(DEFAULT_CERTIFICATION_RATE))))
            .andExpect(jsonPath("$.[*].commissioningRate").value(hasItem(sameNumber(DEFAULT_COMMISSIONING_RATE))))
            .andExpect(jsonPath("$.[*].sourceEventId").value(hasItem(DEFAULT_SOURCE_EVENT_ID.toString())))
            .andExpect(jsonPath("$.[*].computedAt").value(hasItem(DEFAULT_COMPUTED_AT.toString())))
            .andExpect(jsonPath("$.[*].receivedAt").value(hasItem(DEFAULT_RECEIVED_AT.toString())));
    }

    @Test
    @Transactional
    void getCoverageSnapshot() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get the coverageSnapshot
        restCoverageSnapshotMockMvc
            .perform(get(ENTITY_API_URL_ID, coverageSnapshot.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(coverageSnapshot.getId().toString()))
            .andExpect(jsonPath("$.liaisonId").value(DEFAULT_LIAISON_ID.toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.included").value(DEFAULT_INCLUDED))
            .andExpect(jsonPath("$.certificationRate").value(sameNumber(DEFAULT_CERTIFICATION_RATE)))
            .andExpect(jsonPath("$.commissioningRate").value(sameNumber(DEFAULT_COMMISSIONING_RATE)))
            .andExpect(jsonPath("$.sourceEventId").value(DEFAULT_SOURCE_EVENT_ID.toString()))
            .andExpect(jsonPath("$.computedAt").value(DEFAULT_COMPUTED_AT.toString()))
            .andExpect(jsonPath("$.receivedAt").value(DEFAULT_RECEIVED_AT.toString()));
    }

    @Test
    @Transactional
    void getCoverageSnapshotsByIdFiltering() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        UUID id = coverageSnapshot.getId();

        defaultCoverageSnapshotFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByLiaisonIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where liaisonId equals to
        defaultCoverageSnapshotFiltering("liaisonId.equals=" + DEFAULT_LIAISON_ID, "liaisonId.equals=" + UPDATED_LIAISON_ID);
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByLiaisonIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where liaisonId in
        defaultCoverageSnapshotFiltering(
            "liaisonId.in=" + DEFAULT_LIAISON_ID + "," + UPDATED_LIAISON_ID,
            "liaisonId.in=" + UPDATED_LIAISON_ID
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByLiaisonIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where liaisonId is not null
        defaultCoverageSnapshotFiltering("liaisonId.specified=true", "liaisonId.specified=false");
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByServiceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where serviceCode equals to
        defaultCoverageSnapshotFiltering("serviceCode.equals=" + DEFAULT_SERVICE_CODE, "serviceCode.equals=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByServiceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where serviceCode in
        defaultCoverageSnapshotFiltering(
            "serviceCode.in=" + DEFAULT_SERVICE_CODE + "," + UPDATED_SERVICE_CODE,
            "serviceCode.in=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByServiceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where serviceCode is not null
        defaultCoverageSnapshotFiltering("serviceCode.specified=true", "serviceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByServiceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where serviceCode contains
        defaultCoverageSnapshotFiltering("serviceCode.contains=" + DEFAULT_SERVICE_CODE, "serviceCode.contains=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByServiceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where serviceCode does not contain
        defaultCoverageSnapshotFiltering(
            "serviceCode.doesNotContain=" + UPDATED_SERVICE_CODE,
            "serviceCode.doesNotContain=" + DEFAULT_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByIncludedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where included equals to
        defaultCoverageSnapshotFiltering("included.equals=" + DEFAULT_INCLUDED, "included.equals=" + UPDATED_INCLUDED);
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByIncludedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where included in
        defaultCoverageSnapshotFiltering("included.in=" + DEFAULT_INCLUDED + "," + UPDATED_INCLUDED, "included.in=" + UPDATED_INCLUDED);
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByIncludedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where included is not null
        defaultCoverageSnapshotFiltering("included.specified=true", "included.specified=false");
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCertificationRateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where certificationRate equals to
        defaultCoverageSnapshotFiltering(
            "certificationRate.equals=" + DEFAULT_CERTIFICATION_RATE,
            "certificationRate.equals=" + UPDATED_CERTIFICATION_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCertificationRateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where certificationRate in
        defaultCoverageSnapshotFiltering(
            "certificationRate.in=" + DEFAULT_CERTIFICATION_RATE + "," + UPDATED_CERTIFICATION_RATE,
            "certificationRate.in=" + UPDATED_CERTIFICATION_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCertificationRateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where certificationRate is not null
        defaultCoverageSnapshotFiltering("certificationRate.specified=true", "certificationRate.specified=false");
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCertificationRateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where certificationRate is greater than or equal to
        defaultCoverageSnapshotFiltering(
            "certificationRate.greaterThanOrEqual=" + DEFAULT_CERTIFICATION_RATE,
            "certificationRate.greaterThanOrEqual=" + (DEFAULT_CERTIFICATION_RATE.add(BigDecimal.ONE))
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCertificationRateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where certificationRate is less than or equal to
        defaultCoverageSnapshotFiltering(
            "certificationRate.lessThanOrEqual=" + DEFAULT_CERTIFICATION_RATE,
            "certificationRate.lessThanOrEqual=" + SMALLER_CERTIFICATION_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCertificationRateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where certificationRate is less than
        defaultCoverageSnapshotFiltering(
            "certificationRate.lessThan=" + (DEFAULT_CERTIFICATION_RATE.add(BigDecimal.ONE)),
            "certificationRate.lessThan=" + DEFAULT_CERTIFICATION_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCertificationRateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where certificationRate is greater than
        defaultCoverageSnapshotFiltering(
            "certificationRate.greaterThan=" + SMALLER_CERTIFICATION_RATE,
            "certificationRate.greaterThan=" + DEFAULT_CERTIFICATION_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCommissioningRateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where commissioningRate equals to
        defaultCoverageSnapshotFiltering(
            "commissioningRate.equals=" + DEFAULT_COMMISSIONING_RATE,
            "commissioningRate.equals=" + UPDATED_COMMISSIONING_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCommissioningRateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where commissioningRate in
        defaultCoverageSnapshotFiltering(
            "commissioningRate.in=" + DEFAULT_COMMISSIONING_RATE + "," + UPDATED_COMMISSIONING_RATE,
            "commissioningRate.in=" + UPDATED_COMMISSIONING_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCommissioningRateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where commissioningRate is not null
        defaultCoverageSnapshotFiltering("commissioningRate.specified=true", "commissioningRate.specified=false");
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCommissioningRateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where commissioningRate is greater than or equal to
        defaultCoverageSnapshotFiltering(
            "commissioningRate.greaterThanOrEqual=" + DEFAULT_COMMISSIONING_RATE,
            "commissioningRate.greaterThanOrEqual=" + (DEFAULT_COMMISSIONING_RATE.add(BigDecimal.ONE))
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCommissioningRateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where commissioningRate is less than or equal to
        defaultCoverageSnapshotFiltering(
            "commissioningRate.lessThanOrEqual=" + DEFAULT_COMMISSIONING_RATE,
            "commissioningRate.lessThanOrEqual=" + SMALLER_COMMISSIONING_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCommissioningRateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where commissioningRate is less than
        defaultCoverageSnapshotFiltering(
            "commissioningRate.lessThan=" + (DEFAULT_COMMISSIONING_RATE.add(BigDecimal.ONE)),
            "commissioningRate.lessThan=" + DEFAULT_COMMISSIONING_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByCommissioningRateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where commissioningRate is greater than
        defaultCoverageSnapshotFiltering(
            "commissioningRate.greaterThan=" + SMALLER_COMMISSIONING_RATE,
            "commissioningRate.greaterThan=" + DEFAULT_COMMISSIONING_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsBySourceEventIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where sourceEventId equals to
        defaultCoverageSnapshotFiltering(
            "sourceEventId.equals=" + DEFAULT_SOURCE_EVENT_ID,
            "sourceEventId.equals=" + UPDATED_SOURCE_EVENT_ID
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsBySourceEventIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where sourceEventId in
        defaultCoverageSnapshotFiltering(
            "sourceEventId.in=" + DEFAULT_SOURCE_EVENT_ID + "," + UPDATED_SOURCE_EVENT_ID,
            "sourceEventId.in=" + UPDATED_SOURCE_EVENT_ID
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsBySourceEventIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where sourceEventId is not null
        defaultCoverageSnapshotFiltering("sourceEventId.specified=true", "sourceEventId.specified=false");
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByComputedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where computedAt equals to
        defaultCoverageSnapshotFiltering("computedAt.equals=" + DEFAULT_COMPUTED_AT, "computedAt.equals=" + UPDATED_COMPUTED_AT);
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByComputedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where computedAt in
        defaultCoverageSnapshotFiltering(
            "computedAt.in=" + DEFAULT_COMPUTED_AT + "," + UPDATED_COMPUTED_AT,
            "computedAt.in=" + UPDATED_COMPUTED_AT
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByComputedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where computedAt is not null
        defaultCoverageSnapshotFiltering("computedAt.specified=true", "computedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByReceivedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where receivedAt equals to
        defaultCoverageSnapshotFiltering("receivedAt.equals=" + DEFAULT_RECEIVED_AT, "receivedAt.equals=" + UPDATED_RECEIVED_AT);
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByReceivedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where receivedAt in
        defaultCoverageSnapshotFiltering(
            "receivedAt.in=" + DEFAULT_RECEIVED_AT + "," + UPDATED_RECEIVED_AT,
            "receivedAt.in=" + UPDATED_RECEIVED_AT
        );
    }

    @Test
    @Transactional
    void getAllCoverageSnapshotsByReceivedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverageSnapshot = coverageSnapshotRepository.saveAndFlush(coverageSnapshot);

        // Get all the coverageSnapshotList where receivedAt is not null
        defaultCoverageSnapshotFiltering("receivedAt.specified=true", "receivedAt.specified=false");
    }

    private void defaultCoverageSnapshotFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultCoverageSnapshotShouldBeFound(shouldBeFound);
        defaultCoverageSnapshotShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultCoverageSnapshotShouldBeFound(String filter) throws Exception {
        restCoverageSnapshotMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(coverageSnapshot.getId().toString())))
            .andExpect(jsonPath("$.[*].liaisonId").value(hasItem(DEFAULT_LIAISON_ID.toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].included").value(hasItem(DEFAULT_INCLUDED)))
            .andExpect(jsonPath("$.[*].certificationRate").value(hasItem(sameNumber(DEFAULT_CERTIFICATION_RATE))))
            .andExpect(jsonPath("$.[*].commissioningRate").value(hasItem(sameNumber(DEFAULT_COMMISSIONING_RATE))))
            .andExpect(jsonPath("$.[*].sourceEventId").value(hasItem(DEFAULT_SOURCE_EVENT_ID.toString())))
            .andExpect(jsonPath("$.[*].computedAt").value(hasItem(DEFAULT_COMPUTED_AT.toString())))
            .andExpect(jsonPath("$.[*].receivedAt").value(hasItem(DEFAULT_RECEIVED_AT.toString())));

        // Check, that the count call also returns 1
        restCoverageSnapshotMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultCoverageSnapshotShouldNotBeFound(String filter) throws Exception {
        restCoverageSnapshotMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCoverageSnapshotMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingCoverageSnapshot() throws Exception {
        // Get the coverageSnapshot
        restCoverageSnapshotMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return coverageSnapshotRepository.count();
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

    protected CoverageSnapshot getPersistedCoverageSnapshot(CoverageSnapshot coverageSnapshot) {
        return coverageSnapshotRepository.findById(coverageSnapshot.getId()).orElseThrow();
    }

    protected void assertPersistedCoverageSnapshotToMatchAllProperties(CoverageSnapshot expectedCoverageSnapshot) {
        assertCoverageSnapshotAllPropertiesEquals(expectedCoverageSnapshot, getPersistedCoverageSnapshot(expectedCoverageSnapshot));
    }

    protected void assertPersistedCoverageSnapshotToMatchUpdatableProperties(CoverageSnapshot expectedCoverageSnapshot) {
        assertCoverageSnapshotAllUpdatablePropertiesEquals(
            expectedCoverageSnapshot,
            getPersistedCoverageSnapshot(expectedCoverageSnapshot)
        );
    }
}
