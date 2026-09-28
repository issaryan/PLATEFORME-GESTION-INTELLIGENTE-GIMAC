package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.RiskAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static cm.gimac.des.pilotage.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.Project;
import cm.gimac.des.pilotage.domain.Risk;
import cm.gimac.des.pilotage.domain.enumeration.RiskZone;
import cm.gimac.des.pilotage.repository.RiskRepository;
import cm.gimac.des.pilotage.service.RiskService;
import cm.gimac.des.pilotage.service.dto.RiskDTO;
import cm.gimac.des.pilotage.service.mapper.RiskMapper;
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
 * Integration tests for the {@link RiskResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RiskResourceIT {

    private static final String DEFAULT_SCOPE = "AAAAAAAAAA";
    private static final String UPDATED_SCOPE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Integer DEFAULT_PROBABILITY = 1;
    private static final Integer UPDATED_PROBABILITY = 2;
    private static final Integer SMALLER_PROBABILITY = 1 - 1;

    private static final Integer DEFAULT_IMPACT = 1;
    private static final Integer UPDATED_IMPACT = 2;
    private static final Integer SMALLER_IMPACT = 1 - 1;

    private static final String DEFAULT_CONTROLS = "AAAAAAAAAA";
    private static final String UPDATED_CONTROLS = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_CONTROL_COEFFICIENT = new BigDecimal(0);
    private static final BigDecimal UPDATED_CONTROL_COEFFICIENT = new BigDecimal(1);
    private static final BigDecimal SMALLER_CONTROL_COEFFICIENT = new BigDecimal(0 - 1);

    private static final Integer DEFAULT_GROSS_SCORE = 1;
    private static final Integer UPDATED_GROSS_SCORE = 2;
    private static final Integer SMALLER_GROSS_SCORE = 1 - 1;

    private static final BigDecimal DEFAULT_RESIDUAL_SCORE = new BigDecimal(0);
    private static final BigDecimal UPDATED_RESIDUAL_SCORE = new BigDecimal(1);
    private static final BigDecimal SMALLER_RESIDUAL_SCORE = new BigDecimal(0 - 1);

    private static final RiskZone DEFAULT_ZONE = RiskZone.FAIBLE;
    private static final RiskZone UPDATED_ZONE = RiskZone.MODERE;

    private static final UUID DEFAULT_OWNER_ID = UUID.randomUUID();
    private static final UUID UPDATED_OWNER_ID = UUID.randomUUID();

    private static final String DEFAULT_MITIGATION_PLAN = "AAAAAAAAAA";
    private static final String UPDATED_MITIGATION_PLAN = "BBBBBBBBBB";

    private static final Instant DEFAULT_REVIEWED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REVIEWED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/risks";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RiskRepository riskRepository;

    @Mock
    private RiskRepository riskRepositoryMock;

    @Autowired
    private RiskMapper riskMapper;

    @Mock
    private RiskService riskServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRiskMockMvc;

    private Risk risk;

    private Risk insertedRisk;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Risk createEntity() {
        return new Risk()
            .scope(DEFAULT_SCOPE)
            .description(DEFAULT_DESCRIPTION)
            .probability(DEFAULT_PROBABILITY)
            .impact(DEFAULT_IMPACT)
            .controls(DEFAULT_CONTROLS)
            .controlCoefficient(DEFAULT_CONTROL_COEFFICIENT)
            .grossScore(DEFAULT_GROSS_SCORE)
            .residualScore(DEFAULT_RESIDUAL_SCORE)
            .zone(DEFAULT_ZONE)
            .ownerId(DEFAULT_OWNER_ID)
            .mitigationPlan(DEFAULT_MITIGATION_PLAN)
            .reviewedAt(DEFAULT_REVIEWED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Risk createUpdatedEntity() {
        return new Risk()
            .scope(UPDATED_SCOPE)
            .description(UPDATED_DESCRIPTION)
            .probability(UPDATED_PROBABILITY)
            .impact(UPDATED_IMPACT)
            .controls(UPDATED_CONTROLS)
            .controlCoefficient(UPDATED_CONTROL_COEFFICIENT)
            .grossScore(UPDATED_GROSS_SCORE)
            .residualScore(UPDATED_RESIDUAL_SCORE)
            .zone(UPDATED_ZONE)
            .ownerId(UPDATED_OWNER_ID)
            .mitigationPlan(UPDATED_MITIGATION_PLAN)
            .reviewedAt(UPDATED_REVIEWED_AT);
    }

    @BeforeEach
    void initTest() {
        risk = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRisk != null) {
            riskRepository.delete(insertedRisk);
            insertedRisk = null;
        }
    }

    @Test
    @Transactional
    void createRisk() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Risk
        RiskDTO riskDTO = riskMapper.toDto(risk);
        var returnedRiskDTO = om.readValue(
            restRiskMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RiskDTO.class
        );

        // Validate the Risk in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRisk = riskMapper.toEntity(returnedRiskDTO);
        assertRiskUpdatableFieldsEquals(returnedRisk, getPersistedRisk(returnedRisk));

        insertedRisk = returnedRisk;
    }

    @Test
    @Transactional
    void createRiskWithExistingId() throws Exception {
        // Create the Risk with an existing ID
        insertedRisk = riskRepository.saveAndFlush(risk);
        RiskDTO riskDTO = riskMapper.toDto(risk);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRiskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Risk in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkScopeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        risk.setScope(null);

        // Create the Risk, which fails.
        RiskDTO riskDTO = riskMapper.toDto(risk);

        restRiskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDescriptionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        risk.setDescription(null);

        // Create the Risk, which fails.
        RiskDTO riskDTO = riskMapper.toDto(risk);

        restRiskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkProbabilityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        risk.setProbability(null);

        // Create the Risk, which fails.
        RiskDTO riskDTO = riskMapper.toDto(risk);

        restRiskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkImpactIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        risk.setImpact(null);

        // Create the Risk, which fails.
        RiskDTO riskDTO = riskMapper.toDto(risk);

        restRiskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkControlCoefficientIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        risk.setControlCoefficient(null);

        // Create the Risk, which fails.
        RiskDTO riskDTO = riskMapper.toDto(risk);

        restRiskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkGrossScoreIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        risk.setGrossScore(null);

        // Create the Risk, which fails.
        RiskDTO riskDTO = riskMapper.toDto(risk);

        restRiskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkResidualScoreIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        risk.setResidualScore(null);

        // Create the Risk, which fails.
        RiskDTO riskDTO = riskMapper.toDto(risk);

        restRiskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkZoneIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        risk.setZone(null);

        // Create the Risk, which fails.
        RiskDTO riskDTO = riskMapper.toDto(risk);

        restRiskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOwnerIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        risk.setOwnerId(null);

        // Create the Risk, which fails.
        RiskDTO riskDTO = riskMapper.toDto(risk);

        restRiskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRisks() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList
        restRiskMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(risk.getId().toString())))
            .andExpect(jsonPath("$.[*].scope").value(hasItem(DEFAULT_SCOPE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].probability").value(hasItem(DEFAULT_PROBABILITY)))
            .andExpect(jsonPath("$.[*].impact").value(hasItem(DEFAULT_IMPACT)))
            .andExpect(jsonPath("$.[*].controls").value(hasItem(DEFAULT_CONTROLS)))
            .andExpect(jsonPath("$.[*].controlCoefficient").value(hasItem(sameNumber(DEFAULT_CONTROL_COEFFICIENT))))
            .andExpect(jsonPath("$.[*].grossScore").value(hasItem(DEFAULT_GROSS_SCORE)))
            .andExpect(jsonPath("$.[*].residualScore").value(hasItem(sameNumber(DEFAULT_RESIDUAL_SCORE))))
            .andExpect(jsonPath("$.[*].zone").value(hasItem(DEFAULT_ZONE.toString())))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].mitigationPlan").value(hasItem(DEFAULT_MITIGATION_PLAN)))
            .andExpect(jsonPath("$.[*].reviewedAt").value(hasItem(DEFAULT_REVIEWED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRisksWithEagerRelationshipsIsEnabled() throws Exception {
        when(riskServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRiskMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(riskServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRisksWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(riskServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRiskMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(riskRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRisk() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get the risk
        restRiskMockMvc
            .perform(get(ENTITY_API_URL_ID, risk.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(risk.getId().toString()))
            .andExpect(jsonPath("$.scope").value(DEFAULT_SCOPE))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.probability").value(DEFAULT_PROBABILITY))
            .andExpect(jsonPath("$.impact").value(DEFAULT_IMPACT))
            .andExpect(jsonPath("$.controls").value(DEFAULT_CONTROLS))
            .andExpect(jsonPath("$.controlCoefficient").value(sameNumber(DEFAULT_CONTROL_COEFFICIENT)))
            .andExpect(jsonPath("$.grossScore").value(DEFAULT_GROSS_SCORE))
            .andExpect(jsonPath("$.residualScore").value(sameNumber(DEFAULT_RESIDUAL_SCORE)))
            .andExpect(jsonPath("$.zone").value(DEFAULT_ZONE.toString()))
            .andExpect(jsonPath("$.ownerId").value(DEFAULT_OWNER_ID.toString()))
            .andExpect(jsonPath("$.mitigationPlan").value(DEFAULT_MITIGATION_PLAN))
            .andExpect(jsonPath("$.reviewedAt").value(DEFAULT_REVIEWED_AT.toString()));
    }

    @Test
    @Transactional
    void getRisksByIdFiltering() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        UUID id = risk.getId();

        defaultRiskFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllRisksByScopeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where scope equals to
        defaultRiskFiltering("scope.equals=" + DEFAULT_SCOPE, "scope.equals=" + UPDATED_SCOPE);
    }

    @Test
    @Transactional
    void getAllRisksByScopeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where scope in
        defaultRiskFiltering("scope.in=" + DEFAULT_SCOPE + "," + UPDATED_SCOPE, "scope.in=" + UPDATED_SCOPE);
    }

    @Test
    @Transactional
    void getAllRisksByScopeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where scope is not null
        defaultRiskFiltering("scope.specified=true", "scope.specified=false");
    }

    @Test
    @Transactional
    void getAllRisksByScopeContainsSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where scope contains
        defaultRiskFiltering("scope.contains=" + DEFAULT_SCOPE, "scope.contains=" + UPDATED_SCOPE);
    }

    @Test
    @Transactional
    void getAllRisksByScopeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where scope does not contain
        defaultRiskFiltering("scope.doesNotContain=" + UPDATED_SCOPE, "scope.doesNotContain=" + DEFAULT_SCOPE);
    }

    @Test
    @Transactional
    void getAllRisksByDescriptionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where description equals to
        defaultRiskFiltering("description.equals=" + DEFAULT_DESCRIPTION, "description.equals=" + UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllRisksByDescriptionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where description in
        defaultRiskFiltering("description.in=" + DEFAULT_DESCRIPTION + "," + UPDATED_DESCRIPTION, "description.in=" + UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllRisksByDescriptionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where description is not null
        defaultRiskFiltering("description.specified=true", "description.specified=false");
    }

    @Test
    @Transactional
    void getAllRisksByDescriptionContainsSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where description contains
        defaultRiskFiltering("description.contains=" + DEFAULT_DESCRIPTION, "description.contains=" + UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllRisksByDescriptionNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where description does not contain
        defaultRiskFiltering("description.doesNotContain=" + UPDATED_DESCRIPTION, "description.doesNotContain=" + DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllRisksByProbabilityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where probability equals to
        defaultRiskFiltering("probability.equals=" + DEFAULT_PROBABILITY, "probability.equals=" + UPDATED_PROBABILITY);
    }

    @Test
    @Transactional
    void getAllRisksByProbabilityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where probability in
        defaultRiskFiltering("probability.in=" + DEFAULT_PROBABILITY + "," + UPDATED_PROBABILITY, "probability.in=" + UPDATED_PROBABILITY);
    }

    @Test
    @Transactional
    void getAllRisksByProbabilityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where probability is not null
        defaultRiskFiltering("probability.specified=true", "probability.specified=false");
    }

    @Test
    @Transactional
    void getAllRisksByProbabilityIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where probability is greater than or equal to
        defaultRiskFiltering(
            "probability.greaterThanOrEqual=" + DEFAULT_PROBABILITY,
            "probability.greaterThanOrEqual=" + (DEFAULT_PROBABILITY + 1)
        );
    }

    @Test
    @Transactional
    void getAllRisksByProbabilityIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where probability is less than or equal to
        defaultRiskFiltering("probability.lessThanOrEqual=" + DEFAULT_PROBABILITY, "probability.lessThanOrEqual=" + SMALLER_PROBABILITY);
    }

    @Test
    @Transactional
    void getAllRisksByProbabilityIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where probability is less than
        defaultRiskFiltering("probability.lessThan=" + (DEFAULT_PROBABILITY + 1), "probability.lessThan=" + DEFAULT_PROBABILITY);
    }

    @Test
    @Transactional
    void getAllRisksByProbabilityIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where probability is greater than
        defaultRiskFiltering("probability.greaterThan=" + SMALLER_PROBABILITY, "probability.greaterThan=" + DEFAULT_PROBABILITY);
    }

    @Test
    @Transactional
    void getAllRisksByImpactIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where impact equals to
        defaultRiskFiltering("impact.equals=" + DEFAULT_IMPACT, "impact.equals=" + UPDATED_IMPACT);
    }

    @Test
    @Transactional
    void getAllRisksByImpactIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where impact in
        defaultRiskFiltering("impact.in=" + DEFAULT_IMPACT + "," + UPDATED_IMPACT, "impact.in=" + UPDATED_IMPACT);
    }

    @Test
    @Transactional
    void getAllRisksByImpactIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where impact is not null
        defaultRiskFiltering("impact.specified=true", "impact.specified=false");
    }

    @Test
    @Transactional
    void getAllRisksByImpactIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where impact is greater than or equal to
        defaultRiskFiltering("impact.greaterThanOrEqual=" + DEFAULT_IMPACT, "impact.greaterThanOrEqual=" + (DEFAULT_IMPACT + 1));
    }

    @Test
    @Transactional
    void getAllRisksByImpactIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where impact is less than or equal to
        defaultRiskFiltering("impact.lessThanOrEqual=" + DEFAULT_IMPACT, "impact.lessThanOrEqual=" + SMALLER_IMPACT);
    }

    @Test
    @Transactional
    void getAllRisksByImpactIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where impact is less than
        defaultRiskFiltering("impact.lessThan=" + (DEFAULT_IMPACT + 1), "impact.lessThan=" + DEFAULT_IMPACT);
    }

    @Test
    @Transactional
    void getAllRisksByImpactIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where impact is greater than
        defaultRiskFiltering("impact.greaterThan=" + SMALLER_IMPACT, "impact.greaterThan=" + DEFAULT_IMPACT);
    }

    @Test
    @Transactional
    void getAllRisksByControlCoefficientIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where controlCoefficient equals to
        defaultRiskFiltering(
            "controlCoefficient.equals=" + DEFAULT_CONTROL_COEFFICIENT,
            "controlCoefficient.equals=" + UPDATED_CONTROL_COEFFICIENT
        );
    }

    @Test
    @Transactional
    void getAllRisksByControlCoefficientIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where controlCoefficient in
        defaultRiskFiltering(
            "controlCoefficient.in=" + DEFAULT_CONTROL_COEFFICIENT + "," + UPDATED_CONTROL_COEFFICIENT,
            "controlCoefficient.in=" + UPDATED_CONTROL_COEFFICIENT
        );
    }

    @Test
    @Transactional
    void getAllRisksByControlCoefficientIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where controlCoefficient is not null
        defaultRiskFiltering("controlCoefficient.specified=true", "controlCoefficient.specified=false");
    }

    @Test
    @Transactional
    void getAllRisksByControlCoefficientIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where controlCoefficient is greater than or equal to
        defaultRiskFiltering(
            "controlCoefficient.greaterThanOrEqual=" + DEFAULT_CONTROL_COEFFICIENT,
            "controlCoefficient.greaterThanOrEqual=" + (DEFAULT_CONTROL_COEFFICIENT.add(BigDecimal.ONE))
        );
    }

    @Test
    @Transactional
    void getAllRisksByControlCoefficientIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where controlCoefficient is less than or equal to
        defaultRiskFiltering(
            "controlCoefficient.lessThanOrEqual=" + DEFAULT_CONTROL_COEFFICIENT,
            "controlCoefficient.lessThanOrEqual=" + SMALLER_CONTROL_COEFFICIENT
        );
    }

    @Test
    @Transactional
    void getAllRisksByControlCoefficientIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where controlCoefficient is less than
        defaultRiskFiltering(
            "controlCoefficient.lessThan=" + (DEFAULT_CONTROL_COEFFICIENT.add(BigDecimal.ONE)),
            "controlCoefficient.lessThan=" + DEFAULT_CONTROL_COEFFICIENT
        );
    }

    @Test
    @Transactional
    void getAllRisksByControlCoefficientIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where controlCoefficient is greater than
        defaultRiskFiltering(
            "controlCoefficient.greaterThan=" + SMALLER_CONTROL_COEFFICIENT,
            "controlCoefficient.greaterThan=" + DEFAULT_CONTROL_COEFFICIENT
        );
    }

    @Test
    @Transactional
    void getAllRisksByGrossScoreIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where grossScore equals to
        defaultRiskFiltering("grossScore.equals=" + DEFAULT_GROSS_SCORE, "grossScore.equals=" + UPDATED_GROSS_SCORE);
    }

    @Test
    @Transactional
    void getAllRisksByGrossScoreIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where grossScore in
        defaultRiskFiltering("grossScore.in=" + DEFAULT_GROSS_SCORE + "," + UPDATED_GROSS_SCORE, "grossScore.in=" + UPDATED_GROSS_SCORE);
    }

    @Test
    @Transactional
    void getAllRisksByGrossScoreIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where grossScore is not null
        defaultRiskFiltering("grossScore.specified=true", "grossScore.specified=false");
    }

    @Test
    @Transactional
    void getAllRisksByGrossScoreIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where grossScore is greater than or equal to
        defaultRiskFiltering(
            "grossScore.greaterThanOrEqual=" + DEFAULT_GROSS_SCORE,
            "grossScore.greaterThanOrEqual=" + (DEFAULT_GROSS_SCORE + 1)
        );
    }

    @Test
    @Transactional
    void getAllRisksByGrossScoreIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where grossScore is less than or equal to
        defaultRiskFiltering("grossScore.lessThanOrEqual=" + DEFAULT_GROSS_SCORE, "grossScore.lessThanOrEqual=" + SMALLER_GROSS_SCORE);
    }

    @Test
    @Transactional
    void getAllRisksByGrossScoreIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where grossScore is less than
        defaultRiskFiltering("grossScore.lessThan=" + (DEFAULT_GROSS_SCORE + 1), "grossScore.lessThan=" + DEFAULT_GROSS_SCORE);
    }

    @Test
    @Transactional
    void getAllRisksByGrossScoreIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where grossScore is greater than
        defaultRiskFiltering("grossScore.greaterThan=" + SMALLER_GROSS_SCORE, "grossScore.greaterThan=" + DEFAULT_GROSS_SCORE);
    }

    @Test
    @Transactional
    void getAllRisksByResidualScoreIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where residualScore equals to
        defaultRiskFiltering("residualScore.equals=" + DEFAULT_RESIDUAL_SCORE, "residualScore.equals=" + UPDATED_RESIDUAL_SCORE);
    }

    @Test
    @Transactional
    void getAllRisksByResidualScoreIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where residualScore in
        defaultRiskFiltering(
            "residualScore.in=" + DEFAULT_RESIDUAL_SCORE + "," + UPDATED_RESIDUAL_SCORE,
            "residualScore.in=" + UPDATED_RESIDUAL_SCORE
        );
    }

    @Test
    @Transactional
    void getAllRisksByResidualScoreIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where residualScore is not null
        defaultRiskFiltering("residualScore.specified=true", "residualScore.specified=false");
    }

    @Test
    @Transactional
    void getAllRisksByResidualScoreIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where residualScore is greater than or equal to
        defaultRiskFiltering(
            "residualScore.greaterThanOrEqual=" + DEFAULT_RESIDUAL_SCORE,
            "residualScore.greaterThanOrEqual=" + (DEFAULT_RESIDUAL_SCORE.add(BigDecimal.ONE))
        );
    }

    @Test
    @Transactional
    void getAllRisksByResidualScoreIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where residualScore is less than or equal to
        defaultRiskFiltering(
            "residualScore.lessThanOrEqual=" + DEFAULT_RESIDUAL_SCORE,
            "residualScore.lessThanOrEqual=" + SMALLER_RESIDUAL_SCORE
        );
    }

    @Test
    @Transactional
    void getAllRisksByResidualScoreIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where residualScore is less than
        defaultRiskFiltering(
            "residualScore.lessThan=" + (DEFAULT_RESIDUAL_SCORE.add(BigDecimal.ONE)),
            "residualScore.lessThan=" + DEFAULT_RESIDUAL_SCORE
        );
    }

    @Test
    @Transactional
    void getAllRisksByResidualScoreIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where residualScore is greater than
        defaultRiskFiltering("residualScore.greaterThan=" + SMALLER_RESIDUAL_SCORE, "residualScore.greaterThan=" + DEFAULT_RESIDUAL_SCORE);
    }

    @Test
    @Transactional
    void getAllRisksByZoneIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where zone equals to
        defaultRiskFiltering("zone.equals=" + DEFAULT_ZONE, "zone.equals=" + UPDATED_ZONE);
    }

    @Test
    @Transactional
    void getAllRisksByZoneIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where zone in
        defaultRiskFiltering("zone.in=" + DEFAULT_ZONE + "," + UPDATED_ZONE, "zone.in=" + UPDATED_ZONE);
    }

    @Test
    @Transactional
    void getAllRisksByZoneIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where zone is not null
        defaultRiskFiltering("zone.specified=true", "zone.specified=false");
    }

    @Test
    @Transactional
    void getAllRisksByOwnerIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where ownerId equals to
        defaultRiskFiltering("ownerId.equals=" + DEFAULT_OWNER_ID, "ownerId.equals=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllRisksByOwnerIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where ownerId in
        defaultRiskFiltering("ownerId.in=" + DEFAULT_OWNER_ID + "," + UPDATED_OWNER_ID, "ownerId.in=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllRisksByOwnerIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where ownerId is not null
        defaultRiskFiltering("ownerId.specified=true", "ownerId.specified=false");
    }

    @Test
    @Transactional
    void getAllRisksByMitigationPlanIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where mitigationPlan equals to
        defaultRiskFiltering("mitigationPlan.equals=" + DEFAULT_MITIGATION_PLAN, "mitigationPlan.equals=" + UPDATED_MITIGATION_PLAN);
    }

    @Test
    @Transactional
    void getAllRisksByMitigationPlanIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where mitigationPlan in
        defaultRiskFiltering(
            "mitigationPlan.in=" + DEFAULT_MITIGATION_PLAN + "," + UPDATED_MITIGATION_PLAN,
            "mitigationPlan.in=" + UPDATED_MITIGATION_PLAN
        );
    }

    @Test
    @Transactional
    void getAllRisksByMitigationPlanIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where mitigationPlan is not null
        defaultRiskFiltering("mitigationPlan.specified=true", "mitigationPlan.specified=false");
    }

    @Test
    @Transactional
    void getAllRisksByMitigationPlanContainsSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where mitigationPlan contains
        defaultRiskFiltering("mitigationPlan.contains=" + DEFAULT_MITIGATION_PLAN, "mitigationPlan.contains=" + UPDATED_MITIGATION_PLAN);
    }

    @Test
    @Transactional
    void getAllRisksByMitigationPlanNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where mitigationPlan does not contain
        defaultRiskFiltering(
            "mitigationPlan.doesNotContain=" + UPDATED_MITIGATION_PLAN,
            "mitigationPlan.doesNotContain=" + DEFAULT_MITIGATION_PLAN
        );
    }

    @Test
    @Transactional
    void getAllRisksByReviewedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where reviewedAt equals to
        defaultRiskFiltering("reviewedAt.equals=" + DEFAULT_REVIEWED_AT, "reviewedAt.equals=" + UPDATED_REVIEWED_AT);
    }

    @Test
    @Transactional
    void getAllRisksByReviewedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where reviewedAt in
        defaultRiskFiltering("reviewedAt.in=" + DEFAULT_REVIEWED_AT + "," + UPDATED_REVIEWED_AT, "reviewedAt.in=" + UPDATED_REVIEWED_AT);
    }

    @Test
    @Transactional
    void getAllRisksByReviewedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        // Get all the riskList where reviewedAt is not null
        defaultRiskFiltering("reviewedAt.specified=true", "reviewedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRisksByProjectIsEqualToSomething() throws Exception {
        Project project;
        if (TestUtil.findAll(em, Project.class).isEmpty()) {
            riskRepository.saveAndFlush(risk);
            project = ProjectResourceIT.createEntity();
        } else {
            project = TestUtil.findAll(em, Project.class).get(0);
        }
        em.persist(project);
        em.flush();
        risk.setProject(project);
        riskRepository.saveAndFlush(risk);
        UUID projectId = project.getId();
        // Get all the riskList where project equals to projectId
        defaultRiskShouldBeFound("projectId.equals=" + projectId);

        // Get all the riskList where project equals to UUID.randomUUID()
        defaultRiskShouldNotBeFound("projectId.equals=" + UUID.randomUUID());
    }

    private void defaultRiskFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultRiskShouldBeFound(shouldBeFound);
        defaultRiskShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultRiskShouldBeFound(String filter) throws Exception {
        restRiskMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(risk.getId().toString())))
            .andExpect(jsonPath("$.[*].scope").value(hasItem(DEFAULT_SCOPE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].probability").value(hasItem(DEFAULT_PROBABILITY)))
            .andExpect(jsonPath("$.[*].impact").value(hasItem(DEFAULT_IMPACT)))
            .andExpect(jsonPath("$.[*].controls").value(hasItem(DEFAULT_CONTROLS)))
            .andExpect(jsonPath("$.[*].controlCoefficient").value(hasItem(sameNumber(DEFAULT_CONTROL_COEFFICIENT))))
            .andExpect(jsonPath("$.[*].grossScore").value(hasItem(DEFAULT_GROSS_SCORE)))
            .andExpect(jsonPath("$.[*].residualScore").value(hasItem(sameNumber(DEFAULT_RESIDUAL_SCORE))))
            .andExpect(jsonPath("$.[*].zone").value(hasItem(DEFAULT_ZONE.toString())))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].mitigationPlan").value(hasItem(DEFAULT_MITIGATION_PLAN)))
            .andExpect(jsonPath("$.[*].reviewedAt").value(hasItem(DEFAULT_REVIEWED_AT.toString())));

        // Check, that the count call also returns 1
        restRiskMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultRiskShouldNotBeFound(String filter) throws Exception {
        restRiskMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restRiskMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingRisk() throws Exception {
        // Get the risk
        restRiskMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRisk() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the risk
        Risk updatedRisk = riskRepository.findById(risk.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRisk are not directly saved in db
        em.detach(updatedRisk);
        updatedRisk
            .scope(UPDATED_SCOPE)
            .description(UPDATED_DESCRIPTION)
            .probability(UPDATED_PROBABILITY)
            .impact(UPDATED_IMPACT)
            .controls(UPDATED_CONTROLS)
            .controlCoefficient(UPDATED_CONTROL_COEFFICIENT)
            .grossScore(UPDATED_GROSS_SCORE)
            .residualScore(UPDATED_RESIDUAL_SCORE)
            .zone(UPDATED_ZONE)
            .ownerId(UPDATED_OWNER_ID)
            .mitigationPlan(UPDATED_MITIGATION_PLAN)
            .reviewedAt(UPDATED_REVIEWED_AT);
        RiskDTO riskDTO = riskMapper.toDto(updatedRisk);

        restRiskMockMvc
            .perform(
                put(ENTITY_API_URL_ID, riskDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(riskDTO))
            )
            .andExpect(status().isOk());

        // Validate the Risk in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRiskToMatchAllProperties(updatedRisk);
    }

    @Test
    @Transactional
    void putNonExistingRisk() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        risk.setId(UUID.randomUUID());

        // Create the Risk
        RiskDTO riskDTO = riskMapper.toDto(risk);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRiskMockMvc
            .perform(
                put(ENTITY_API_URL_ID, riskDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(riskDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Risk in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRisk() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        risk.setId(UUID.randomUUID());

        // Create the Risk
        RiskDTO riskDTO = riskMapper.toDto(risk);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRiskMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(riskDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Risk in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRisk() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        risk.setId(UUID.randomUUID());

        // Create the Risk
        RiskDTO riskDTO = riskMapper.toDto(risk);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRiskMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Risk in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRiskWithPatch() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the risk using partial update
        Risk partialUpdatedRisk = new Risk();
        partialUpdatedRisk.setId(risk.getId());

        partialUpdatedRisk
            .scope(UPDATED_SCOPE)
            .probability(UPDATED_PROBABILITY)
            .impact(UPDATED_IMPACT)
            .controls(UPDATED_CONTROLS)
            .controlCoefficient(UPDATED_CONTROL_COEFFICIENT)
            .grossScore(UPDATED_GROSS_SCORE)
            .ownerId(UPDATED_OWNER_ID)
            .mitigationPlan(UPDATED_MITIGATION_PLAN);

        restRiskMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRisk.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRisk))
            )
            .andExpect(status().isOk());

        // Validate the Risk in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRiskUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedRisk, risk), getPersistedRisk(risk));
    }

    @Test
    @Transactional
    void fullUpdateRiskWithPatch() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the risk using partial update
        Risk partialUpdatedRisk = new Risk();
        partialUpdatedRisk.setId(risk.getId());

        partialUpdatedRisk
            .scope(UPDATED_SCOPE)
            .description(UPDATED_DESCRIPTION)
            .probability(UPDATED_PROBABILITY)
            .impact(UPDATED_IMPACT)
            .controls(UPDATED_CONTROLS)
            .controlCoefficient(UPDATED_CONTROL_COEFFICIENT)
            .grossScore(UPDATED_GROSS_SCORE)
            .residualScore(UPDATED_RESIDUAL_SCORE)
            .zone(UPDATED_ZONE)
            .ownerId(UPDATED_OWNER_ID)
            .mitigationPlan(UPDATED_MITIGATION_PLAN)
            .reviewedAt(UPDATED_REVIEWED_AT);

        restRiskMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRisk.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRisk))
            )
            .andExpect(status().isOk());

        // Validate the Risk in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRiskUpdatableFieldsEquals(partialUpdatedRisk, getPersistedRisk(partialUpdatedRisk));
    }

    @Test
    @Transactional
    void patchNonExistingRisk() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        risk.setId(UUID.randomUUID());

        // Create the Risk
        RiskDTO riskDTO = riskMapper.toDto(risk);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRiskMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, riskDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(riskDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Risk in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRisk() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        risk.setId(UUID.randomUUID());

        // Create the Risk
        RiskDTO riskDTO = riskMapper.toDto(risk);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRiskMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(riskDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Risk in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRisk() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        risk.setId(UUID.randomUUID());

        // Create the Risk
        RiskDTO riskDTO = riskMapper.toDto(risk);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRiskMockMvc
            .perform(patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(riskDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Risk in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRisk() throws Exception {
        // Initialize the database
        insertedRisk = riskRepository.saveAndFlush(risk);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the risk
        restRiskMockMvc
            .perform(delete(ENTITY_API_URL_ID, risk.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return riskRepository.count();
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

    protected Risk getPersistedRisk(Risk risk) {
        return riskRepository.findById(risk.getId()).orElseThrow();
    }

    protected void assertPersistedRiskToMatchAllProperties(Risk expectedRisk) {
        assertRiskAllPropertiesEquals(expectedRisk, getPersistedRisk(expectedRisk));
    }

    protected void assertPersistedRiskToMatchUpdatableProperties(Risk expectedRisk) {
        assertRiskAllUpdatablePropertiesEquals(expectedRisk, getPersistedRisk(expectedRisk));
    }
}
