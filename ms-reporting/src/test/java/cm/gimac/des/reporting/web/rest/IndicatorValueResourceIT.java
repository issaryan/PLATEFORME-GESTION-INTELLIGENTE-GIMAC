package cm.gimac.des.reporting.web.rest;

import static cm.gimac.des.reporting.domain.IndicatorValueAsserts.*;
import static cm.gimac.des.reporting.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.reporting.IntegrationTest;
import cm.gimac.des.reporting.domain.ImportedStatement;
import cm.gimac.des.reporting.domain.IndicatorDefinition;
import cm.gimac.des.reporting.domain.IndicatorValue;
import cm.gimac.des.reporting.domain.enumeration.ThresholdState;
import cm.gimac.des.reporting.domain.enumeration.ThresholdState;
import cm.gimac.des.reporting.domain.enumeration.Trend;
import cm.gimac.des.reporting.repository.IndicatorValueRepository;
import cm.gimac.des.reporting.service.IndicatorValueService;
import cm.gimac.des.reporting.service.mapper.IndicatorValueMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Integration tests for the {@link IndicatorValueResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class IndicatorValueResourceIT {

    private static final String DEFAULT_PERIOD = "1143-23";
    private static final String UPDATED_PERIOD = "7719";

    private static final LocalDate DEFAULT_TIME_KEY = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_TIME_KEY = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_TIME_KEY = LocalDate.ofEpochDay(-1L);

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_SEVERITY = "AAAAAAAAAA";
    private static final String UPDATED_SEVERITY = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_VALUE = new BigDecimal(1);
    private static final BigDecimal UPDATED_VALUE = new BigDecimal(2);
    private static final BigDecimal SMALLER_VALUE = new BigDecimal(1 - 1);

    private static final Boolean DEFAULT_NO_DATA = false;
    private static final Boolean UPDATED_NO_DATA = true;

    private static final ThresholdState DEFAULT_THRESHOLD_STATE = ThresholdState.NORMAL;
    private static final ThresholdState UPDATED_THRESHOLD_STATE = ThresholdState.ALERTE;

    private static final ThresholdState DEFAULT_PREVIOUS_THRESHOLD_STATE = ThresholdState.NORMAL;
    private static final ThresholdState UPDATED_PREVIOUS_THRESHOLD_STATE = ThresholdState.ALERTE;

    private static final Trend DEFAULT_TREND = Trend.HAUSSE;
    private static final Trend UPDATED_TREND = Trend.STABLE;

    private static final Instant DEFAULT_COMPUTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_COMPUTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/indicator-values";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private IndicatorValueRepository indicatorValueRepository;

    @Mock
    private IndicatorValueRepository indicatorValueRepositoryMock;

    @Autowired
    private IndicatorValueMapper indicatorValueMapper;

    @Mock
    private IndicatorValueService indicatorValueServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restIndicatorValueMockMvc;

    private IndicatorValue indicatorValue;

    private IndicatorValue insertedIndicatorValue;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static IndicatorValue createEntity(EntityManager em) {
        IndicatorValue indicatorValue = new IndicatorValue()
            .period(DEFAULT_PERIOD)
            .timeKey(DEFAULT_TIME_KEY)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .serviceCode(DEFAULT_SERVICE_CODE)
            .severity(DEFAULT_SEVERITY)
            .value(DEFAULT_VALUE)
            .noData(DEFAULT_NO_DATA)
            .thresholdState(DEFAULT_THRESHOLD_STATE)
            .previousThresholdState(DEFAULT_PREVIOUS_THRESHOLD_STATE)
            .trend(DEFAULT_TREND)
            .computedAt(DEFAULT_COMPUTED_AT);
        // Add required entity
        IndicatorDefinition indicatorDefinition;
        if (TestUtil.findAll(em, IndicatorDefinition.class).isEmpty()) {
            indicatorDefinition = IndicatorDefinitionResourceIT.createEntity();
            em.persist(indicatorDefinition);
            em.flush();
        } else {
            indicatorDefinition = TestUtil.findAll(em, IndicatorDefinition.class).get(0);
        }
        indicatorValue.setDefinition(indicatorDefinition);
        return indicatorValue;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static IndicatorValue createUpdatedEntity(EntityManager em) {
        IndicatorValue updatedIndicatorValue = new IndicatorValue()
            .period(UPDATED_PERIOD)
            .timeKey(UPDATED_TIME_KEY)
            .institutionId(UPDATED_INSTITUTION_ID)
            .serviceCode(UPDATED_SERVICE_CODE)
            .severity(UPDATED_SEVERITY)
            .value(UPDATED_VALUE)
            .noData(UPDATED_NO_DATA)
            .thresholdState(UPDATED_THRESHOLD_STATE)
            .previousThresholdState(UPDATED_PREVIOUS_THRESHOLD_STATE)
            .trend(UPDATED_TREND)
            .computedAt(UPDATED_COMPUTED_AT);
        // Add required entity
        IndicatorDefinition indicatorDefinition;
        if (TestUtil.findAll(em, IndicatorDefinition.class).isEmpty()) {
            indicatorDefinition = IndicatorDefinitionResourceIT.createUpdatedEntity();
            em.persist(indicatorDefinition);
            em.flush();
        } else {
            indicatorDefinition = TestUtil.findAll(em, IndicatorDefinition.class).get(0);
        }
        updatedIndicatorValue.setDefinition(indicatorDefinition);
        return updatedIndicatorValue;
    }

    @BeforeEach
    void initTest() {
        indicatorValue = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedIndicatorValue != null) {
            indicatorValueRepository.delete(insertedIndicatorValue);
            insertedIndicatorValue = null;
        }
    }

    @Test
    @Transactional
    void getAllIndicatorValues() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList
        restIndicatorValueMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(indicatorValue.getId().toString())))
            .andExpect(jsonPath("$.[*].period").value(hasItem(DEFAULT_PERIOD)))
            .andExpect(jsonPath("$.[*].timeKey").value(hasItem(DEFAULT_TIME_KEY.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY)))
            .andExpect(jsonPath("$.[*].value").value(hasItem(sameNumber(DEFAULT_VALUE))))
            .andExpect(jsonPath("$.[*].noData").value(hasItem(DEFAULT_NO_DATA)))
            .andExpect(jsonPath("$.[*].thresholdState").value(hasItem(DEFAULT_THRESHOLD_STATE.toString())))
            .andExpect(jsonPath("$.[*].previousThresholdState").value(hasItem(DEFAULT_PREVIOUS_THRESHOLD_STATE.toString())))
            .andExpect(jsonPath("$.[*].trend").value(hasItem(DEFAULT_TREND.toString())))
            .andExpect(jsonPath("$.[*].computedAt").value(hasItem(DEFAULT_COMPUTED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllIndicatorValuesWithEagerRelationshipsIsEnabled() throws Exception {
        when(indicatorValueServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restIndicatorValueMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(indicatorValueServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllIndicatorValuesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(indicatorValueServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restIndicatorValueMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(indicatorValueRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getIndicatorValue() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get the indicatorValue
        restIndicatorValueMockMvc
            .perform(get(ENTITY_API_URL_ID, indicatorValue.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(indicatorValue.getId().toString()))
            .andExpect(jsonPath("$.period").value(DEFAULT_PERIOD))
            .andExpect(jsonPath("$.timeKey").value(DEFAULT_TIME_KEY.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.severity").value(DEFAULT_SEVERITY))
            .andExpect(jsonPath("$.value").value(sameNumber(DEFAULT_VALUE)))
            .andExpect(jsonPath("$.noData").value(DEFAULT_NO_DATA))
            .andExpect(jsonPath("$.thresholdState").value(DEFAULT_THRESHOLD_STATE.toString()))
            .andExpect(jsonPath("$.previousThresholdState").value(DEFAULT_PREVIOUS_THRESHOLD_STATE.toString()))
            .andExpect(jsonPath("$.trend").value(DEFAULT_TREND.toString()))
            .andExpect(jsonPath("$.computedAt").value(DEFAULT_COMPUTED_AT.toString()));
    }

    @Test
    @Transactional
    void getIndicatorValuesByIdFiltering() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        UUID id = indicatorValue.getId();

        defaultIndicatorValueFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByPeriodIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where period equals to
        defaultIndicatorValueFiltering("period.equals=" + DEFAULT_PERIOD, "period.equals=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByPeriodIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where period in
        defaultIndicatorValueFiltering("period.in=" + DEFAULT_PERIOD + "," + UPDATED_PERIOD, "period.in=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByPeriodIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where period is not null
        defaultIndicatorValueFiltering("period.specified=true", "period.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByPeriodContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where period contains
        defaultIndicatorValueFiltering("period.contains=" + DEFAULT_PERIOD, "period.contains=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByPeriodNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where period does not contain
        defaultIndicatorValueFiltering("period.doesNotContain=" + UPDATED_PERIOD, "period.doesNotContain=" + DEFAULT_PERIOD);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByTimeKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where timeKey equals to
        defaultIndicatorValueFiltering("timeKey.equals=" + DEFAULT_TIME_KEY, "timeKey.equals=" + UPDATED_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByTimeKeyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where timeKey in
        defaultIndicatorValueFiltering("timeKey.in=" + DEFAULT_TIME_KEY + "," + UPDATED_TIME_KEY, "timeKey.in=" + UPDATED_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByTimeKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where timeKey is not null
        defaultIndicatorValueFiltering("timeKey.specified=true", "timeKey.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByTimeKeyIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where timeKey is greater than or equal to
        defaultIndicatorValueFiltering("timeKey.greaterThanOrEqual=" + DEFAULT_TIME_KEY, "timeKey.greaterThanOrEqual=" + UPDATED_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByTimeKeyIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where timeKey is less than or equal to
        defaultIndicatorValueFiltering("timeKey.lessThanOrEqual=" + DEFAULT_TIME_KEY, "timeKey.lessThanOrEqual=" + SMALLER_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByTimeKeyIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where timeKey is less than
        defaultIndicatorValueFiltering("timeKey.lessThan=" + UPDATED_TIME_KEY, "timeKey.lessThan=" + DEFAULT_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByTimeKeyIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where timeKey is greater than
        defaultIndicatorValueFiltering("timeKey.greaterThan=" + SMALLER_TIME_KEY, "timeKey.greaterThan=" + DEFAULT_TIME_KEY);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where institutionId equals to
        defaultIndicatorValueFiltering("institutionId.equals=" + DEFAULT_INSTITUTION_ID, "institutionId.equals=" + UPDATED_INSTITUTION_ID);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where institutionId in
        defaultIndicatorValueFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where institutionId is not null
        defaultIndicatorValueFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByServiceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where serviceCode equals to
        defaultIndicatorValueFiltering("serviceCode.equals=" + DEFAULT_SERVICE_CODE, "serviceCode.equals=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByServiceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where serviceCode in
        defaultIndicatorValueFiltering(
            "serviceCode.in=" + DEFAULT_SERVICE_CODE + "," + UPDATED_SERVICE_CODE,
            "serviceCode.in=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByServiceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where serviceCode is not null
        defaultIndicatorValueFiltering("serviceCode.specified=true", "serviceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByServiceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where serviceCode contains
        defaultIndicatorValueFiltering("serviceCode.contains=" + DEFAULT_SERVICE_CODE, "serviceCode.contains=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByServiceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where serviceCode does not contain
        defaultIndicatorValueFiltering(
            "serviceCode.doesNotContain=" + UPDATED_SERVICE_CODE,
            "serviceCode.doesNotContain=" + DEFAULT_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllIndicatorValuesBySeverityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where severity equals to
        defaultIndicatorValueFiltering("severity.equals=" + DEFAULT_SEVERITY, "severity.equals=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesBySeverityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where severity in
        defaultIndicatorValueFiltering("severity.in=" + DEFAULT_SEVERITY + "," + UPDATED_SEVERITY, "severity.in=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesBySeverityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where severity is not null
        defaultIndicatorValueFiltering("severity.specified=true", "severity.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorValuesBySeverityContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where severity contains
        defaultIndicatorValueFiltering("severity.contains=" + DEFAULT_SEVERITY, "severity.contains=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesBySeverityNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where severity does not contain
        defaultIndicatorValueFiltering("severity.doesNotContain=" + UPDATED_SEVERITY, "severity.doesNotContain=" + DEFAULT_SEVERITY);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByValueIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where value equals to
        defaultIndicatorValueFiltering("value.equals=" + DEFAULT_VALUE, "value.equals=" + UPDATED_VALUE);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByValueIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where value in
        defaultIndicatorValueFiltering("value.in=" + DEFAULT_VALUE + "," + UPDATED_VALUE, "value.in=" + UPDATED_VALUE);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByValueIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where value is not null
        defaultIndicatorValueFiltering("value.specified=true", "value.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByValueIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where value is greater than or equal to
        defaultIndicatorValueFiltering("value.greaterThanOrEqual=" + DEFAULT_VALUE, "value.greaterThanOrEqual=" + UPDATED_VALUE);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByValueIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where value is less than or equal to
        defaultIndicatorValueFiltering("value.lessThanOrEqual=" + DEFAULT_VALUE, "value.lessThanOrEqual=" + SMALLER_VALUE);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByValueIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where value is less than
        defaultIndicatorValueFiltering("value.lessThan=" + UPDATED_VALUE, "value.lessThan=" + DEFAULT_VALUE);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByValueIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where value is greater than
        defaultIndicatorValueFiltering("value.greaterThan=" + SMALLER_VALUE, "value.greaterThan=" + DEFAULT_VALUE);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByNoDataIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where noData equals to
        defaultIndicatorValueFiltering("noData.equals=" + DEFAULT_NO_DATA, "noData.equals=" + UPDATED_NO_DATA);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByNoDataIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where noData in
        defaultIndicatorValueFiltering("noData.in=" + DEFAULT_NO_DATA + "," + UPDATED_NO_DATA, "noData.in=" + UPDATED_NO_DATA);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByNoDataIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where noData is not null
        defaultIndicatorValueFiltering("noData.specified=true", "noData.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByThresholdStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where thresholdState equals to
        defaultIndicatorValueFiltering(
            "thresholdState.equals=" + DEFAULT_THRESHOLD_STATE,
            "thresholdState.equals=" + UPDATED_THRESHOLD_STATE
        );
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByThresholdStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where thresholdState in
        defaultIndicatorValueFiltering(
            "thresholdState.in=" + DEFAULT_THRESHOLD_STATE + "," + UPDATED_THRESHOLD_STATE,
            "thresholdState.in=" + UPDATED_THRESHOLD_STATE
        );
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByThresholdStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where thresholdState is not null
        defaultIndicatorValueFiltering("thresholdState.specified=true", "thresholdState.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByPreviousThresholdStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where previousThresholdState equals to
        defaultIndicatorValueFiltering(
            "previousThresholdState.equals=" + DEFAULT_PREVIOUS_THRESHOLD_STATE,
            "previousThresholdState.equals=" + UPDATED_PREVIOUS_THRESHOLD_STATE
        );
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByPreviousThresholdStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where previousThresholdState in
        defaultIndicatorValueFiltering(
            "previousThresholdState.in=" + DEFAULT_PREVIOUS_THRESHOLD_STATE + "," + UPDATED_PREVIOUS_THRESHOLD_STATE,
            "previousThresholdState.in=" + UPDATED_PREVIOUS_THRESHOLD_STATE
        );
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByPreviousThresholdStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where previousThresholdState is not null
        defaultIndicatorValueFiltering("previousThresholdState.specified=true", "previousThresholdState.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByTrendIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where trend equals to
        defaultIndicatorValueFiltering("trend.equals=" + DEFAULT_TREND, "trend.equals=" + UPDATED_TREND);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByTrendIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where trend in
        defaultIndicatorValueFiltering("trend.in=" + DEFAULT_TREND + "," + UPDATED_TREND, "trend.in=" + UPDATED_TREND);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByTrendIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where trend is not null
        defaultIndicatorValueFiltering("trend.specified=true", "trend.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByComputedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where computedAt equals to
        defaultIndicatorValueFiltering("computedAt.equals=" + DEFAULT_COMPUTED_AT, "computedAt.equals=" + UPDATED_COMPUTED_AT);
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByComputedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where computedAt in
        defaultIndicatorValueFiltering(
            "computedAt.in=" + DEFAULT_COMPUTED_AT + "," + UPDATED_COMPUTED_AT,
            "computedAt.in=" + UPDATED_COMPUTED_AT
        );
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByComputedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorValue = indicatorValueRepository.saveAndFlush(indicatorValue);

        // Get all the indicatorValueList where computedAt is not null
        defaultIndicatorValueFiltering("computedAt.specified=true", "computedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByDefinitionIsEqualToSomething() throws Exception {
        IndicatorDefinition definition;
        if (TestUtil.findAll(em, IndicatorDefinition.class).isEmpty()) {
            indicatorValueRepository.saveAndFlush(indicatorValue);
            definition = IndicatorDefinitionResourceIT.createEntity();
        } else {
            definition = TestUtil.findAll(em, IndicatorDefinition.class).get(0);
        }
        em.persist(definition);
        em.flush();
        indicatorValue.setDefinition(definition);
        indicatorValueRepository.saveAndFlush(indicatorValue);
        UUID definitionId = definition.getId();
        // Get all the indicatorValueList where definition equals to definitionId
        defaultIndicatorValueShouldBeFound("definitionId.equals=" + definitionId);

        // Get all the indicatorValueList where definition equals to UUID.randomUUID()
        defaultIndicatorValueShouldNotBeFound("definitionId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllIndicatorValuesByStatementIsEqualToSomething() throws Exception {
        ImportedStatement statement;
        if (TestUtil.findAll(em, ImportedStatement.class).isEmpty()) {
            indicatorValueRepository.saveAndFlush(indicatorValue);
            statement = ImportedStatementResourceIT.createEntity();
        } else {
            statement = TestUtil.findAll(em, ImportedStatement.class).get(0);
        }
        em.persist(statement);
        em.flush();
        indicatorValue.setStatement(statement);
        indicatorValueRepository.saveAndFlush(indicatorValue);
        UUID statementId = statement.getId();
        // Get all the indicatorValueList where statement equals to statementId
        defaultIndicatorValueShouldBeFound("statementId.equals=" + statementId);

        // Get all the indicatorValueList where statement equals to UUID.randomUUID()
        defaultIndicatorValueShouldNotBeFound("statementId.equals=" + UUID.randomUUID());
    }

    private void defaultIndicatorValueFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultIndicatorValueShouldBeFound(shouldBeFound);
        defaultIndicatorValueShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultIndicatorValueShouldBeFound(String filter) throws Exception {
        restIndicatorValueMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(indicatorValue.getId().toString())))
            .andExpect(jsonPath("$.[*].period").value(hasItem(DEFAULT_PERIOD)))
            .andExpect(jsonPath("$.[*].timeKey").value(hasItem(DEFAULT_TIME_KEY.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY)))
            .andExpect(jsonPath("$.[*].value").value(hasItem(sameNumber(DEFAULT_VALUE))))
            .andExpect(jsonPath("$.[*].noData").value(hasItem(DEFAULT_NO_DATA)))
            .andExpect(jsonPath("$.[*].thresholdState").value(hasItem(DEFAULT_THRESHOLD_STATE.toString())))
            .andExpect(jsonPath("$.[*].previousThresholdState").value(hasItem(DEFAULT_PREVIOUS_THRESHOLD_STATE.toString())))
            .andExpect(jsonPath("$.[*].trend").value(hasItem(DEFAULT_TREND.toString())))
            .andExpect(jsonPath("$.[*].computedAt").value(hasItem(DEFAULT_COMPUTED_AT.toString())));

        // Check, that the count call also returns 1
        restIndicatorValueMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultIndicatorValueShouldNotBeFound(String filter) throws Exception {
        restIndicatorValueMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restIndicatorValueMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingIndicatorValue() throws Exception {
        // Get the indicatorValue
        restIndicatorValueMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return indicatorValueRepository.count();
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

    protected IndicatorValue getPersistedIndicatorValue(IndicatorValue indicatorValue) {
        return indicatorValueRepository.findById(indicatorValue.getId()).orElseThrow();
    }

    protected void assertPersistedIndicatorValueToMatchAllProperties(IndicatorValue expectedIndicatorValue) {
        assertIndicatorValueAllPropertiesEquals(expectedIndicatorValue, getPersistedIndicatorValue(expectedIndicatorValue));
    }

    protected void assertPersistedIndicatorValueToMatchUpdatableProperties(IndicatorValue expectedIndicatorValue) {
        assertIndicatorValueAllUpdatablePropertiesEquals(expectedIndicatorValue, getPersistedIndicatorValue(expectedIndicatorValue));
    }
}
