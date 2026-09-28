package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.CoverageAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.service.CoverageService;
import cm.gimac.des.homologation.service.mapper.CoverageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Integration tests for the {@link CoverageResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class CoverageResourceIT {

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_CERTIFICATION_RATE = new BigDecimal(0);
    private static final BigDecimal UPDATED_CERTIFICATION_RATE = new BigDecimal(1);
    private static final BigDecimal SMALLER_CERTIFICATION_RATE = new BigDecimal(0 - 1);

    private static final BigDecimal DEFAULT_COMMISSIONING_RATE = new BigDecimal(0);
    private static final BigDecimal UPDATED_COMMISSIONING_RATE = new BigDecimal(1);
    private static final BigDecimal SMALLER_COMMISSIONING_RATE = new BigDecimal(0 - 1);

    private static final Integer DEFAULT_REQUIRED_COUNT = 0;
    private static final Integer UPDATED_REQUIRED_COUNT = 1;
    private static final Integer SMALLER_REQUIRED_COUNT = 0 - 1;

    private static final Integer DEFAULT_CLOSED_COUNT = 0;
    private static final Integer UPDATED_CLOSED_COUNT = 1;
    private static final Integer SMALLER_CLOSED_COUNT = 0 - 1;

    private static final Instant DEFAULT_COMPUTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_COMPUTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/coverages";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CoverageRepository coverageRepository;

    @Mock
    private CoverageRepository coverageRepositoryMock;

    @Autowired
    private CoverageMapper coverageMapper;

    @Mock
    private CoverageService coverageServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCoverageMockMvc;

    private Coverage coverage;

    private Coverage insertedCoverage;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Coverage createEntity(EntityManager em) {
        Coverage coverage = new Coverage()
            .serviceCode(DEFAULT_SERVICE_CODE)
            .certificationRate(DEFAULT_CERTIFICATION_RATE)
            .commissioningRate(DEFAULT_COMMISSIONING_RATE)
            .requiredCount(DEFAULT_REQUIRED_COUNT)
            .closedCount(DEFAULT_CLOSED_COUNT)
            .computedAt(DEFAULT_COMPUTED_AT);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        coverage.setLiaison(liaison);
        return coverage;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Coverage createUpdatedEntity(EntityManager em) {
        Coverage updatedCoverage = new Coverage()
            .serviceCode(UPDATED_SERVICE_CODE)
            .certificationRate(UPDATED_CERTIFICATION_RATE)
            .commissioningRate(UPDATED_COMMISSIONING_RATE)
            .requiredCount(UPDATED_REQUIRED_COUNT)
            .closedCount(UPDATED_CLOSED_COUNT)
            .computedAt(UPDATED_COMPUTED_AT);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        updatedCoverage.setLiaison(liaison);
        return updatedCoverage;
    }

    @BeforeEach
    void initTest() {
        coverage = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedCoverage != null) {
            coverageRepository.delete(insertedCoverage);
            insertedCoverage = null;
        }
    }

    @Test
    @Transactional
    void getAllCoverages() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList
        restCoverageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(coverage.getId().toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].certificationRate").value(hasItem(sameNumber(DEFAULT_CERTIFICATION_RATE))))
            .andExpect(jsonPath("$.[*].commissioningRate").value(hasItem(sameNumber(DEFAULT_COMMISSIONING_RATE))))
            .andExpect(jsonPath("$.[*].requiredCount").value(hasItem(DEFAULT_REQUIRED_COUNT)))
            .andExpect(jsonPath("$.[*].closedCount").value(hasItem(DEFAULT_CLOSED_COUNT)))
            .andExpect(jsonPath("$.[*].computedAt").value(hasItem(DEFAULT_COMPUTED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCoveragesWithEagerRelationshipsIsEnabled() throws Exception {
        when(coverageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCoverageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(coverageServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCoveragesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(coverageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCoverageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(coverageRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getCoverage() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get the coverage
        restCoverageMockMvc
            .perform(get(ENTITY_API_URL_ID, coverage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(coverage.getId().toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.certificationRate").value(sameNumber(DEFAULT_CERTIFICATION_RATE)))
            .andExpect(jsonPath("$.commissioningRate").value(sameNumber(DEFAULT_COMMISSIONING_RATE)))
            .andExpect(jsonPath("$.requiredCount").value(DEFAULT_REQUIRED_COUNT))
            .andExpect(jsonPath("$.closedCount").value(DEFAULT_CLOSED_COUNT))
            .andExpect(jsonPath("$.computedAt").value(DEFAULT_COMPUTED_AT.toString()));
    }

    @Test
    @Transactional
    void getCoveragesByIdFiltering() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        UUID id = coverage.getId();

        defaultCoverageFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllCoveragesByServiceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where serviceCode equals to
        defaultCoverageFiltering("serviceCode.equals=" + DEFAULT_SERVICE_CODE, "serviceCode.equals=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllCoveragesByServiceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where serviceCode in
        defaultCoverageFiltering(
            "serviceCode.in=" + DEFAULT_SERVICE_CODE + "," + UPDATED_SERVICE_CODE,
            "serviceCode.in=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByServiceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where serviceCode is not null
        defaultCoverageFiltering("serviceCode.specified=true", "serviceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllCoveragesByServiceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where serviceCode contains
        defaultCoverageFiltering("serviceCode.contains=" + DEFAULT_SERVICE_CODE, "serviceCode.contains=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllCoveragesByServiceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where serviceCode does not contain
        defaultCoverageFiltering(
            "serviceCode.doesNotContain=" + UPDATED_SERVICE_CODE,
            "serviceCode.doesNotContain=" + DEFAULT_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCertificationRateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where certificationRate equals to
        defaultCoverageFiltering(
            "certificationRate.equals=" + DEFAULT_CERTIFICATION_RATE,
            "certificationRate.equals=" + UPDATED_CERTIFICATION_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCertificationRateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where certificationRate in
        defaultCoverageFiltering(
            "certificationRate.in=" + DEFAULT_CERTIFICATION_RATE + "," + UPDATED_CERTIFICATION_RATE,
            "certificationRate.in=" + UPDATED_CERTIFICATION_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCertificationRateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where certificationRate is not null
        defaultCoverageFiltering("certificationRate.specified=true", "certificationRate.specified=false");
    }

    @Test
    @Transactional
    void getAllCoveragesByCertificationRateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where certificationRate is greater than or equal to
        defaultCoverageFiltering(
            "certificationRate.greaterThanOrEqual=" + DEFAULT_CERTIFICATION_RATE,
            "certificationRate.greaterThanOrEqual=" + (DEFAULT_CERTIFICATION_RATE.add(BigDecimal.ONE))
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCertificationRateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where certificationRate is less than or equal to
        defaultCoverageFiltering(
            "certificationRate.lessThanOrEqual=" + DEFAULT_CERTIFICATION_RATE,
            "certificationRate.lessThanOrEqual=" + SMALLER_CERTIFICATION_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCertificationRateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where certificationRate is less than
        defaultCoverageFiltering(
            "certificationRate.lessThan=" + (DEFAULT_CERTIFICATION_RATE.add(BigDecimal.ONE)),
            "certificationRate.lessThan=" + DEFAULT_CERTIFICATION_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCertificationRateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where certificationRate is greater than
        defaultCoverageFiltering(
            "certificationRate.greaterThan=" + SMALLER_CERTIFICATION_RATE,
            "certificationRate.greaterThan=" + DEFAULT_CERTIFICATION_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCommissioningRateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where commissioningRate equals to
        defaultCoverageFiltering(
            "commissioningRate.equals=" + DEFAULT_COMMISSIONING_RATE,
            "commissioningRate.equals=" + UPDATED_COMMISSIONING_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCommissioningRateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where commissioningRate in
        defaultCoverageFiltering(
            "commissioningRate.in=" + DEFAULT_COMMISSIONING_RATE + "," + UPDATED_COMMISSIONING_RATE,
            "commissioningRate.in=" + UPDATED_COMMISSIONING_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCommissioningRateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where commissioningRate is not null
        defaultCoverageFiltering("commissioningRate.specified=true", "commissioningRate.specified=false");
    }

    @Test
    @Transactional
    void getAllCoveragesByCommissioningRateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where commissioningRate is greater than or equal to
        defaultCoverageFiltering(
            "commissioningRate.greaterThanOrEqual=" + DEFAULT_COMMISSIONING_RATE,
            "commissioningRate.greaterThanOrEqual=" + (DEFAULT_COMMISSIONING_RATE.add(BigDecimal.ONE))
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCommissioningRateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where commissioningRate is less than or equal to
        defaultCoverageFiltering(
            "commissioningRate.lessThanOrEqual=" + DEFAULT_COMMISSIONING_RATE,
            "commissioningRate.lessThanOrEqual=" + SMALLER_COMMISSIONING_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCommissioningRateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where commissioningRate is less than
        defaultCoverageFiltering(
            "commissioningRate.lessThan=" + (DEFAULT_COMMISSIONING_RATE.add(BigDecimal.ONE)),
            "commissioningRate.lessThan=" + DEFAULT_COMMISSIONING_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByCommissioningRateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where commissioningRate is greater than
        defaultCoverageFiltering(
            "commissioningRate.greaterThan=" + SMALLER_COMMISSIONING_RATE,
            "commissioningRate.greaterThan=" + DEFAULT_COMMISSIONING_RATE
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByRequiredCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where requiredCount equals to
        defaultCoverageFiltering("requiredCount.equals=" + DEFAULT_REQUIRED_COUNT, "requiredCount.equals=" + UPDATED_REQUIRED_COUNT);
    }

    @Test
    @Transactional
    void getAllCoveragesByRequiredCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where requiredCount in
        defaultCoverageFiltering(
            "requiredCount.in=" + DEFAULT_REQUIRED_COUNT + "," + UPDATED_REQUIRED_COUNT,
            "requiredCount.in=" + UPDATED_REQUIRED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByRequiredCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where requiredCount is not null
        defaultCoverageFiltering("requiredCount.specified=true", "requiredCount.specified=false");
    }

    @Test
    @Transactional
    void getAllCoveragesByRequiredCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where requiredCount is greater than or equal to
        defaultCoverageFiltering(
            "requiredCount.greaterThanOrEqual=" + DEFAULT_REQUIRED_COUNT,
            "requiredCount.greaterThanOrEqual=" + UPDATED_REQUIRED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByRequiredCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where requiredCount is less than or equal to
        defaultCoverageFiltering(
            "requiredCount.lessThanOrEqual=" + DEFAULT_REQUIRED_COUNT,
            "requiredCount.lessThanOrEqual=" + SMALLER_REQUIRED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByRequiredCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where requiredCount is less than
        defaultCoverageFiltering("requiredCount.lessThan=" + UPDATED_REQUIRED_COUNT, "requiredCount.lessThan=" + DEFAULT_REQUIRED_COUNT);
    }

    @Test
    @Transactional
    void getAllCoveragesByRequiredCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where requiredCount is greater than
        defaultCoverageFiltering(
            "requiredCount.greaterThan=" + SMALLER_REQUIRED_COUNT,
            "requiredCount.greaterThan=" + DEFAULT_REQUIRED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByClosedCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where closedCount equals to
        defaultCoverageFiltering("closedCount.equals=" + DEFAULT_CLOSED_COUNT, "closedCount.equals=" + UPDATED_CLOSED_COUNT);
    }

    @Test
    @Transactional
    void getAllCoveragesByClosedCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where closedCount in
        defaultCoverageFiltering(
            "closedCount.in=" + DEFAULT_CLOSED_COUNT + "," + UPDATED_CLOSED_COUNT,
            "closedCount.in=" + UPDATED_CLOSED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByClosedCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where closedCount is not null
        defaultCoverageFiltering("closedCount.specified=true", "closedCount.specified=false");
    }

    @Test
    @Transactional
    void getAllCoveragesByClosedCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where closedCount is greater than or equal to
        defaultCoverageFiltering(
            "closedCount.greaterThanOrEqual=" + DEFAULT_CLOSED_COUNT,
            "closedCount.greaterThanOrEqual=" + UPDATED_CLOSED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByClosedCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where closedCount is less than or equal to
        defaultCoverageFiltering(
            "closedCount.lessThanOrEqual=" + DEFAULT_CLOSED_COUNT,
            "closedCount.lessThanOrEqual=" + SMALLER_CLOSED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByClosedCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where closedCount is less than
        defaultCoverageFiltering("closedCount.lessThan=" + UPDATED_CLOSED_COUNT, "closedCount.lessThan=" + DEFAULT_CLOSED_COUNT);
    }

    @Test
    @Transactional
    void getAllCoveragesByClosedCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where closedCount is greater than
        defaultCoverageFiltering("closedCount.greaterThan=" + SMALLER_CLOSED_COUNT, "closedCount.greaterThan=" + DEFAULT_CLOSED_COUNT);
    }

    @Test
    @Transactional
    void getAllCoveragesByComputedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where computedAt equals to
        defaultCoverageFiltering("computedAt.equals=" + DEFAULT_COMPUTED_AT, "computedAt.equals=" + UPDATED_COMPUTED_AT);
    }

    @Test
    @Transactional
    void getAllCoveragesByComputedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where computedAt in
        defaultCoverageFiltering(
            "computedAt.in=" + DEFAULT_COMPUTED_AT + "," + UPDATED_COMPUTED_AT,
            "computedAt.in=" + UPDATED_COMPUTED_AT
        );
    }

    @Test
    @Transactional
    void getAllCoveragesByComputedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList where computedAt is not null
        defaultCoverageFiltering("computedAt.specified=true", "computedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCoveragesByLiaisonIsEqualToSomething() throws Exception {
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            coverageRepository.saveAndFlush(coverage);
            liaison = LiaisonResourceIT.createEntity(em);
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        em.persist(liaison);
        em.flush();
        coverage.setLiaison(liaison);
        coverageRepository.saveAndFlush(coverage);
        UUID liaisonId = liaison.getId();
        // Get all the coverageList where liaison equals to liaisonId
        defaultCoverageShouldBeFound("liaisonId.equals=" + liaisonId);

        // Get all the coverageList where liaison equals to UUID.randomUUID()
        defaultCoverageShouldNotBeFound("liaisonId.equals=" + UUID.randomUUID());
    }

    private void defaultCoverageFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultCoverageShouldBeFound(shouldBeFound);
        defaultCoverageShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultCoverageShouldBeFound(String filter) throws Exception {
        restCoverageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(coverage.getId().toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].certificationRate").value(hasItem(sameNumber(DEFAULT_CERTIFICATION_RATE))))
            .andExpect(jsonPath("$.[*].commissioningRate").value(hasItem(sameNumber(DEFAULT_COMMISSIONING_RATE))))
            .andExpect(jsonPath("$.[*].requiredCount").value(hasItem(DEFAULT_REQUIRED_COUNT)))
            .andExpect(jsonPath("$.[*].closedCount").value(hasItem(DEFAULT_CLOSED_COUNT)))
            .andExpect(jsonPath("$.[*].computedAt").value(hasItem(DEFAULT_COMPUTED_AT.toString())));

        // Check, that the count call also returns 1
        restCoverageMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultCoverageShouldNotBeFound(String filter) throws Exception {
        restCoverageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCoverageMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingCoverage() throws Exception {
        // Get the coverage
        restCoverageMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return coverageRepository.count();
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

    protected Coverage getPersistedCoverage(Coverage coverage) {
        return coverageRepository.findById(coverage.getId()).orElseThrow();
    }

    protected void assertPersistedCoverageToMatchAllProperties(Coverage expectedCoverage) {
        assertCoverageAllPropertiesEquals(expectedCoverage, getPersistedCoverage(expectedCoverage));
    }

    protected void assertPersistedCoverageToMatchUpdatableProperties(Coverage expectedCoverage) {
        assertCoverageAllUpdatablePropertiesEquals(expectedCoverage, getPersistedCoverage(expectedCoverage));
    }
}
