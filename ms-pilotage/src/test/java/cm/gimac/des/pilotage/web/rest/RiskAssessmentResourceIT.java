package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.RiskAssessmentAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.Risk;
import cm.gimac.des.pilotage.domain.RiskAssessment;
import cm.gimac.des.pilotage.domain.enumeration.RiskZone;
import cm.gimac.des.pilotage.domain.enumeration.RiskZone;
import cm.gimac.des.pilotage.repository.RiskAssessmentRepository;
import cm.gimac.des.pilotage.service.RiskAssessmentService;
import cm.gimac.des.pilotage.service.mapper.RiskAssessmentMapper;
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
 * Integration tests for the {@link RiskAssessmentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RiskAssessmentResourceIT {

    private static final Integer DEFAULT_PROBABILITY = 1;
    private static final Integer UPDATED_PROBABILITY = 2;
    private static final Integer SMALLER_PROBABILITY = 1 - 1;

    private static final Integer DEFAULT_IMPACT = 1;
    private static final Integer UPDATED_IMPACT = 2;
    private static final Integer SMALLER_IMPACT = 1 - 1;

    private static final BigDecimal DEFAULT_CONTROL_COEFFICIENT = new BigDecimal(0);
    private static final BigDecimal UPDATED_CONTROL_COEFFICIENT = new BigDecimal(1);
    private static final BigDecimal SMALLER_CONTROL_COEFFICIENT = new BigDecimal(0 - 1);

    private static final Integer DEFAULT_GROSS_SCORE = 1;
    private static final Integer UPDATED_GROSS_SCORE = 2;
    private static final Integer SMALLER_GROSS_SCORE = 1 - 1;

    private static final BigDecimal DEFAULT_RESIDUAL_SCORE = new BigDecimal(0);
    private static final BigDecimal UPDATED_RESIDUAL_SCORE = new BigDecimal(1);
    private static final BigDecimal SMALLER_RESIDUAL_SCORE = new BigDecimal(0 - 1);

    private static final RiskZone DEFAULT_PREVIOUS_ZONE = RiskZone.FAIBLE;
    private static final RiskZone UPDATED_PREVIOUS_ZONE = RiskZone.MODERE;

    private static final RiskZone DEFAULT_ZONE = RiskZone.FAIBLE;
    private static final RiskZone UPDATED_ZONE = RiskZone.MODERE;

    private static final UUID DEFAULT_ASSESSOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_ASSESSOR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_ASSESSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ASSESSED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/risk-assessments";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private RiskAssessmentRepository riskAssessmentRepositoryMock;

    @Autowired
    private RiskAssessmentMapper riskAssessmentMapper;

    @Mock
    private RiskAssessmentService riskAssessmentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRiskAssessmentMockMvc;

    private RiskAssessment riskAssessment;

    private RiskAssessment insertedRiskAssessment;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RiskAssessment createEntity(EntityManager em) {
        RiskAssessment riskAssessment = new RiskAssessment()
            .probability(DEFAULT_PROBABILITY)
            .impact(DEFAULT_IMPACT)
            .controlCoefficient(DEFAULT_CONTROL_COEFFICIENT)
            .grossScore(DEFAULT_GROSS_SCORE)
            .residualScore(DEFAULT_RESIDUAL_SCORE)
            .previousZone(DEFAULT_PREVIOUS_ZONE)
            .zone(DEFAULT_ZONE)
            .assessorId(DEFAULT_ASSESSOR_ID)
            .assessedAt(DEFAULT_ASSESSED_AT);
        // Add required entity
        Risk risk;
        if (TestUtil.findAll(em, Risk.class).isEmpty()) {
            risk = RiskResourceIT.createEntity();
            em.persist(risk);
            em.flush();
        } else {
            risk = TestUtil.findAll(em, Risk.class).get(0);
        }
        riskAssessment.setRisk(risk);
        return riskAssessment;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RiskAssessment createUpdatedEntity(EntityManager em) {
        RiskAssessment updatedRiskAssessment = new RiskAssessment()
            .probability(UPDATED_PROBABILITY)
            .impact(UPDATED_IMPACT)
            .controlCoefficient(UPDATED_CONTROL_COEFFICIENT)
            .grossScore(UPDATED_GROSS_SCORE)
            .residualScore(UPDATED_RESIDUAL_SCORE)
            .previousZone(UPDATED_PREVIOUS_ZONE)
            .zone(UPDATED_ZONE)
            .assessorId(UPDATED_ASSESSOR_ID)
            .assessedAt(UPDATED_ASSESSED_AT);
        // Add required entity
        Risk risk;
        if (TestUtil.findAll(em, Risk.class).isEmpty()) {
            risk = RiskResourceIT.createUpdatedEntity();
            em.persist(risk);
            em.flush();
        } else {
            risk = TestUtil.findAll(em, Risk.class).get(0);
        }
        updatedRiskAssessment.setRisk(risk);
        return updatedRiskAssessment;
    }

    @BeforeEach
    void initTest() {
        riskAssessment = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedRiskAssessment != null) {
            riskAssessmentRepository.delete(insertedRiskAssessment);
            insertedRiskAssessment = null;
        }
    }

    @Test
    @Transactional
    void getAllRiskAssessments() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList
        restRiskAssessmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(riskAssessment.getId().toString())))
            .andExpect(jsonPath("$.[*].probability").value(hasItem(DEFAULT_PROBABILITY)))
            .andExpect(jsonPath("$.[*].impact").value(hasItem(DEFAULT_IMPACT)))
            .andExpect(jsonPath("$.[*].controlCoefficient").value(hasItem(sameNumber(DEFAULT_CONTROL_COEFFICIENT))))
            .andExpect(jsonPath("$.[*].grossScore").value(hasItem(DEFAULT_GROSS_SCORE)))
            .andExpect(jsonPath("$.[*].residualScore").value(hasItem(sameNumber(DEFAULT_RESIDUAL_SCORE))))
            .andExpect(jsonPath("$.[*].previousZone").value(hasItem(DEFAULT_PREVIOUS_ZONE.toString())))
            .andExpect(jsonPath("$.[*].zone").value(hasItem(DEFAULT_ZONE.toString())))
            .andExpect(jsonPath("$.[*].assessorId").value(hasItem(DEFAULT_ASSESSOR_ID.toString())))
            .andExpect(jsonPath("$.[*].assessedAt").value(hasItem(DEFAULT_ASSESSED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRiskAssessmentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(riskAssessmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRiskAssessmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(riskAssessmentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRiskAssessmentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(riskAssessmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRiskAssessmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(riskAssessmentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRiskAssessment() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get the riskAssessment
        restRiskAssessmentMockMvc
            .perform(get(ENTITY_API_URL_ID, riskAssessment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(riskAssessment.getId().toString()))
            .andExpect(jsonPath("$.probability").value(DEFAULT_PROBABILITY))
            .andExpect(jsonPath("$.impact").value(DEFAULT_IMPACT))
            .andExpect(jsonPath("$.controlCoefficient").value(sameNumber(DEFAULT_CONTROL_COEFFICIENT)))
            .andExpect(jsonPath("$.grossScore").value(DEFAULT_GROSS_SCORE))
            .andExpect(jsonPath("$.residualScore").value(sameNumber(DEFAULT_RESIDUAL_SCORE)))
            .andExpect(jsonPath("$.previousZone").value(DEFAULT_PREVIOUS_ZONE.toString()))
            .andExpect(jsonPath("$.zone").value(DEFAULT_ZONE.toString()))
            .andExpect(jsonPath("$.assessorId").value(DEFAULT_ASSESSOR_ID.toString()))
            .andExpect(jsonPath("$.assessedAt").value(DEFAULT_ASSESSED_AT.toString()));
    }

    @Test
    @Transactional
    void getRiskAssessmentsByIdFiltering() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        UUID id = riskAssessment.getId();

        defaultRiskAssessmentFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByProbabilityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where probability equals to
        defaultRiskAssessmentFiltering("probability.equals=" + DEFAULT_PROBABILITY, "probability.equals=" + UPDATED_PROBABILITY);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByProbabilityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where probability in
        defaultRiskAssessmentFiltering(
            "probability.in=" + DEFAULT_PROBABILITY + "," + UPDATED_PROBABILITY,
            "probability.in=" + UPDATED_PROBABILITY
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByProbabilityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where probability is not null
        defaultRiskAssessmentFiltering("probability.specified=true", "probability.specified=false");
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByProbabilityIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where probability is greater than or equal to
        defaultRiskAssessmentFiltering(
            "probability.greaterThanOrEqual=" + DEFAULT_PROBABILITY,
            "probability.greaterThanOrEqual=" + (DEFAULT_PROBABILITY + 1)
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByProbabilityIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where probability is less than or equal to
        defaultRiskAssessmentFiltering(
            "probability.lessThanOrEqual=" + DEFAULT_PROBABILITY,
            "probability.lessThanOrEqual=" + SMALLER_PROBABILITY
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByProbabilityIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where probability is less than
        defaultRiskAssessmentFiltering("probability.lessThan=" + (DEFAULT_PROBABILITY + 1), "probability.lessThan=" + DEFAULT_PROBABILITY);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByProbabilityIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where probability is greater than
        defaultRiskAssessmentFiltering("probability.greaterThan=" + SMALLER_PROBABILITY, "probability.greaterThan=" + DEFAULT_PROBABILITY);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByImpactIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where impact equals to
        defaultRiskAssessmentFiltering("impact.equals=" + DEFAULT_IMPACT, "impact.equals=" + UPDATED_IMPACT);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByImpactIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where impact in
        defaultRiskAssessmentFiltering("impact.in=" + DEFAULT_IMPACT + "," + UPDATED_IMPACT, "impact.in=" + UPDATED_IMPACT);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByImpactIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where impact is not null
        defaultRiskAssessmentFiltering("impact.specified=true", "impact.specified=false");
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByImpactIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where impact is greater than or equal to
        defaultRiskAssessmentFiltering("impact.greaterThanOrEqual=" + DEFAULT_IMPACT, "impact.greaterThanOrEqual=" + (DEFAULT_IMPACT + 1));
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByImpactIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where impact is less than or equal to
        defaultRiskAssessmentFiltering("impact.lessThanOrEqual=" + DEFAULT_IMPACT, "impact.lessThanOrEqual=" + SMALLER_IMPACT);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByImpactIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where impact is less than
        defaultRiskAssessmentFiltering("impact.lessThan=" + (DEFAULT_IMPACT + 1), "impact.lessThan=" + DEFAULT_IMPACT);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByImpactIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where impact is greater than
        defaultRiskAssessmentFiltering("impact.greaterThan=" + SMALLER_IMPACT, "impact.greaterThan=" + DEFAULT_IMPACT);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByControlCoefficientIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where controlCoefficient equals to
        defaultRiskAssessmentFiltering(
            "controlCoefficient.equals=" + DEFAULT_CONTROL_COEFFICIENT,
            "controlCoefficient.equals=" + UPDATED_CONTROL_COEFFICIENT
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByControlCoefficientIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where controlCoefficient in
        defaultRiskAssessmentFiltering(
            "controlCoefficient.in=" + DEFAULT_CONTROL_COEFFICIENT + "," + UPDATED_CONTROL_COEFFICIENT,
            "controlCoefficient.in=" + UPDATED_CONTROL_COEFFICIENT
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByControlCoefficientIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where controlCoefficient is not null
        defaultRiskAssessmentFiltering("controlCoefficient.specified=true", "controlCoefficient.specified=false");
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByControlCoefficientIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where controlCoefficient is greater than or equal to
        defaultRiskAssessmentFiltering(
            "controlCoefficient.greaterThanOrEqual=" + DEFAULT_CONTROL_COEFFICIENT,
            "controlCoefficient.greaterThanOrEqual=" + (DEFAULT_CONTROL_COEFFICIENT.add(BigDecimal.ONE))
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByControlCoefficientIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where controlCoefficient is less than or equal to
        defaultRiskAssessmentFiltering(
            "controlCoefficient.lessThanOrEqual=" + DEFAULT_CONTROL_COEFFICIENT,
            "controlCoefficient.lessThanOrEqual=" + SMALLER_CONTROL_COEFFICIENT
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByControlCoefficientIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where controlCoefficient is less than
        defaultRiskAssessmentFiltering(
            "controlCoefficient.lessThan=" + (DEFAULT_CONTROL_COEFFICIENT.add(BigDecimal.ONE)),
            "controlCoefficient.lessThan=" + DEFAULT_CONTROL_COEFFICIENT
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByControlCoefficientIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where controlCoefficient is greater than
        defaultRiskAssessmentFiltering(
            "controlCoefficient.greaterThan=" + SMALLER_CONTROL_COEFFICIENT,
            "controlCoefficient.greaterThan=" + DEFAULT_CONTROL_COEFFICIENT
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByGrossScoreIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where grossScore equals to
        defaultRiskAssessmentFiltering("grossScore.equals=" + DEFAULT_GROSS_SCORE, "grossScore.equals=" + UPDATED_GROSS_SCORE);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByGrossScoreIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where grossScore in
        defaultRiskAssessmentFiltering(
            "grossScore.in=" + DEFAULT_GROSS_SCORE + "," + UPDATED_GROSS_SCORE,
            "grossScore.in=" + UPDATED_GROSS_SCORE
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByGrossScoreIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where grossScore is not null
        defaultRiskAssessmentFiltering("grossScore.specified=true", "grossScore.specified=false");
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByGrossScoreIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where grossScore is greater than or equal to
        defaultRiskAssessmentFiltering(
            "grossScore.greaterThanOrEqual=" + DEFAULT_GROSS_SCORE,
            "grossScore.greaterThanOrEqual=" + (DEFAULT_GROSS_SCORE + 1)
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByGrossScoreIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where grossScore is less than or equal to
        defaultRiskAssessmentFiltering(
            "grossScore.lessThanOrEqual=" + DEFAULT_GROSS_SCORE,
            "grossScore.lessThanOrEqual=" + SMALLER_GROSS_SCORE
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByGrossScoreIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where grossScore is less than
        defaultRiskAssessmentFiltering("grossScore.lessThan=" + (DEFAULT_GROSS_SCORE + 1), "grossScore.lessThan=" + DEFAULT_GROSS_SCORE);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByGrossScoreIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where grossScore is greater than
        defaultRiskAssessmentFiltering("grossScore.greaterThan=" + SMALLER_GROSS_SCORE, "grossScore.greaterThan=" + DEFAULT_GROSS_SCORE);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByResidualScoreIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where residualScore equals to
        defaultRiskAssessmentFiltering("residualScore.equals=" + DEFAULT_RESIDUAL_SCORE, "residualScore.equals=" + UPDATED_RESIDUAL_SCORE);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByResidualScoreIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where residualScore in
        defaultRiskAssessmentFiltering(
            "residualScore.in=" + DEFAULT_RESIDUAL_SCORE + "," + UPDATED_RESIDUAL_SCORE,
            "residualScore.in=" + UPDATED_RESIDUAL_SCORE
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByResidualScoreIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where residualScore is not null
        defaultRiskAssessmentFiltering("residualScore.specified=true", "residualScore.specified=false");
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByResidualScoreIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where residualScore is greater than or equal to
        defaultRiskAssessmentFiltering(
            "residualScore.greaterThanOrEqual=" + DEFAULT_RESIDUAL_SCORE,
            "residualScore.greaterThanOrEqual=" + (DEFAULT_RESIDUAL_SCORE.add(BigDecimal.ONE))
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByResidualScoreIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where residualScore is less than or equal to
        defaultRiskAssessmentFiltering(
            "residualScore.lessThanOrEqual=" + DEFAULT_RESIDUAL_SCORE,
            "residualScore.lessThanOrEqual=" + SMALLER_RESIDUAL_SCORE
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByResidualScoreIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where residualScore is less than
        defaultRiskAssessmentFiltering(
            "residualScore.lessThan=" + (DEFAULT_RESIDUAL_SCORE.add(BigDecimal.ONE)),
            "residualScore.lessThan=" + DEFAULT_RESIDUAL_SCORE
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByResidualScoreIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where residualScore is greater than
        defaultRiskAssessmentFiltering(
            "residualScore.greaterThan=" + SMALLER_RESIDUAL_SCORE,
            "residualScore.greaterThan=" + DEFAULT_RESIDUAL_SCORE
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByPreviousZoneIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where previousZone equals to
        defaultRiskAssessmentFiltering("previousZone.equals=" + DEFAULT_PREVIOUS_ZONE, "previousZone.equals=" + UPDATED_PREVIOUS_ZONE);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByPreviousZoneIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where previousZone in
        defaultRiskAssessmentFiltering(
            "previousZone.in=" + DEFAULT_PREVIOUS_ZONE + "," + UPDATED_PREVIOUS_ZONE,
            "previousZone.in=" + UPDATED_PREVIOUS_ZONE
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByPreviousZoneIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where previousZone is not null
        defaultRiskAssessmentFiltering("previousZone.specified=true", "previousZone.specified=false");
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByZoneIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where zone equals to
        defaultRiskAssessmentFiltering("zone.equals=" + DEFAULT_ZONE, "zone.equals=" + UPDATED_ZONE);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByZoneIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where zone in
        defaultRiskAssessmentFiltering("zone.in=" + DEFAULT_ZONE + "," + UPDATED_ZONE, "zone.in=" + UPDATED_ZONE);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByZoneIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where zone is not null
        defaultRiskAssessmentFiltering("zone.specified=true", "zone.specified=false");
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByAssessorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where assessorId equals to
        defaultRiskAssessmentFiltering("assessorId.equals=" + DEFAULT_ASSESSOR_ID, "assessorId.equals=" + UPDATED_ASSESSOR_ID);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByAssessorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where assessorId in
        defaultRiskAssessmentFiltering(
            "assessorId.in=" + DEFAULT_ASSESSOR_ID + "," + UPDATED_ASSESSOR_ID,
            "assessorId.in=" + UPDATED_ASSESSOR_ID
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByAssessorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where assessorId is not null
        defaultRiskAssessmentFiltering("assessorId.specified=true", "assessorId.specified=false");
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByAssessedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where assessedAt equals to
        defaultRiskAssessmentFiltering("assessedAt.equals=" + DEFAULT_ASSESSED_AT, "assessedAt.equals=" + UPDATED_ASSESSED_AT);
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByAssessedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where assessedAt in
        defaultRiskAssessmentFiltering(
            "assessedAt.in=" + DEFAULT_ASSESSED_AT + "," + UPDATED_ASSESSED_AT,
            "assessedAt.in=" + UPDATED_ASSESSED_AT
        );
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByAssessedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRiskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // Get all the riskAssessmentList where assessedAt is not null
        defaultRiskAssessmentFiltering("assessedAt.specified=true", "assessedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRiskAssessmentsByRiskIsEqualToSomething() throws Exception {
        Risk risk;
        if (TestUtil.findAll(em, Risk.class).isEmpty()) {
            riskAssessmentRepository.saveAndFlush(riskAssessment);
            risk = RiskResourceIT.createEntity();
        } else {
            risk = TestUtil.findAll(em, Risk.class).get(0);
        }
        em.persist(risk);
        em.flush();
        riskAssessment.setRisk(risk);
        riskAssessmentRepository.saveAndFlush(riskAssessment);
        UUID riskId = risk.getId();
        // Get all the riskAssessmentList where risk equals to riskId
        defaultRiskAssessmentShouldBeFound("riskId.equals=" + riskId);

        // Get all the riskAssessmentList where risk equals to UUID.randomUUID()
        defaultRiskAssessmentShouldNotBeFound("riskId.equals=" + UUID.randomUUID());
    }

    private void defaultRiskAssessmentFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultRiskAssessmentShouldBeFound(shouldBeFound);
        defaultRiskAssessmentShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultRiskAssessmentShouldBeFound(String filter) throws Exception {
        restRiskAssessmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(riskAssessment.getId().toString())))
            .andExpect(jsonPath("$.[*].probability").value(hasItem(DEFAULT_PROBABILITY)))
            .andExpect(jsonPath("$.[*].impact").value(hasItem(DEFAULT_IMPACT)))
            .andExpect(jsonPath("$.[*].controlCoefficient").value(hasItem(sameNumber(DEFAULT_CONTROL_COEFFICIENT))))
            .andExpect(jsonPath("$.[*].grossScore").value(hasItem(DEFAULT_GROSS_SCORE)))
            .andExpect(jsonPath("$.[*].residualScore").value(hasItem(sameNumber(DEFAULT_RESIDUAL_SCORE))))
            .andExpect(jsonPath("$.[*].previousZone").value(hasItem(DEFAULT_PREVIOUS_ZONE.toString())))
            .andExpect(jsonPath("$.[*].zone").value(hasItem(DEFAULT_ZONE.toString())))
            .andExpect(jsonPath("$.[*].assessorId").value(hasItem(DEFAULT_ASSESSOR_ID.toString())))
            .andExpect(jsonPath("$.[*].assessedAt").value(hasItem(DEFAULT_ASSESSED_AT.toString())));

        // Check, that the count call also returns 1
        restRiskAssessmentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultRiskAssessmentShouldNotBeFound(String filter) throws Exception {
        restRiskAssessmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restRiskAssessmentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingRiskAssessment() throws Exception {
        // Get the riskAssessment
        restRiskAssessmentMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return riskAssessmentRepository.count();
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

    protected RiskAssessment getPersistedRiskAssessment(RiskAssessment riskAssessment) {
        return riskAssessmentRepository.findById(riskAssessment.getId()).orElseThrow();
    }

    protected void assertPersistedRiskAssessmentToMatchAllProperties(RiskAssessment expectedRiskAssessment) {
        assertRiskAssessmentAllPropertiesEquals(expectedRiskAssessment, getPersistedRiskAssessment(expectedRiskAssessment));
    }

    protected void assertPersistedRiskAssessmentToMatchUpdatableProperties(RiskAssessment expectedRiskAssessment) {
        assertRiskAssessmentAllUpdatablePropertiesEquals(expectedRiskAssessment, getPersistedRiskAssessment(expectedRiskAssessment));
    }
}
