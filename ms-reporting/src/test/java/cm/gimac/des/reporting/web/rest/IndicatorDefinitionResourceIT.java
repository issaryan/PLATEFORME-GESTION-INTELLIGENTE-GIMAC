package cm.gimac.des.reporting.web.rest;

import static cm.gimac.des.reporting.domain.IndicatorDefinitionAsserts.*;
import static cm.gimac.des.reporting.web.rest.TestUtil.createUpdateProxyForBean;
import static cm.gimac.des.reporting.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.reporting.IntegrationTest;
import cm.gimac.des.reporting.domain.CockpitView;
import cm.gimac.des.reporting.domain.IndicatorDefinition;
import cm.gimac.des.reporting.domain.enumeration.IndicatorSource;
import cm.gimac.des.reporting.repository.IndicatorDefinitionRepository;
import cm.gimac.des.reporting.service.dto.IndicatorDefinitionDTO;
import cm.gimac.des.reporting.service.mapper.IndicatorDefinitionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
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
 * Integration tests for the {@link IndicatorDefinitionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class IndicatorDefinitionResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final IndicatorSource DEFAULT_SOURCE = IndicatorSource.EVENEMENT;
    private static final IndicatorSource UPDATED_SOURCE = IndicatorSource.ETAT_IMPORTE;

    private static final String DEFAULT_UNIT = "AAAAAAAAAA";
    private static final String UPDATED_UNIT = "BBBBBBBBBB";

    private static final Duration DEFAULT_WINDOW = Duration.ofHours(6);
    private static final Duration UPDATED_WINDOW = Duration.ofHours(12);
    private static final Duration SMALLER_WINDOW = Duration.ofHours(5);

    private static final Integer DEFAULT_SEGMENT_COUNT = 1;
    private static final Integer UPDATED_SEGMENT_COUNT = 2;
    private static final Integer SMALLER_SEGMENT_COUNT = 1 - 1;

    private static final Duration DEFAULT_SEGMENT_LENGTH = Duration.ofHours(6);
    private static final Duration UPDATED_SEGMENT_LENGTH = Duration.ofHours(12);
    private static final Duration SMALLER_SEGMENT_LENGTH = Duration.ofHours(5);

    private static final BigDecimal DEFAULT_WARNING_THRESHOLD = new BigDecimal(1);
    private static final BigDecimal UPDATED_WARNING_THRESHOLD = new BigDecimal(2);
    private static final BigDecimal SMALLER_WARNING_THRESHOLD = new BigDecimal(1 - 1);

    private static final BigDecimal DEFAULT_CRITICAL_THRESHOLD = new BigDecimal(1);
    private static final BigDecimal UPDATED_CRITICAL_THRESHOLD = new BigDecimal(2);
    private static final BigDecimal SMALLER_CRITICAL_THRESHOLD = new BigDecimal(1 - 1);

    private static final Boolean DEFAULT_HIGHER_IS_WORSE = false;
    private static final Boolean UPDATED_HIGHER_IS_WORSE = true;

    private static final String DEFAULT_AUDIENCE = "AAAAAAAAAA";
    private static final String UPDATED_AUDIENCE = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/indicator-definitions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private IndicatorDefinitionRepository indicatorDefinitionRepository;

    @Autowired
    private IndicatorDefinitionMapper indicatorDefinitionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restIndicatorDefinitionMockMvc;

    private IndicatorDefinition indicatorDefinition;

    private IndicatorDefinition insertedIndicatorDefinition;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static IndicatorDefinition createEntity() {
        return new IndicatorDefinition()
            .code(DEFAULT_CODE)
            .label(DEFAULT_LABEL)
            .source(DEFAULT_SOURCE)
            .unit(DEFAULT_UNIT)
            .window(DEFAULT_WINDOW)
            .segmentCount(DEFAULT_SEGMENT_COUNT)
            .segmentLength(DEFAULT_SEGMENT_LENGTH)
            .warningThreshold(DEFAULT_WARNING_THRESHOLD)
            .criticalThreshold(DEFAULT_CRITICAL_THRESHOLD)
            .higherIsWorse(DEFAULT_HIGHER_IS_WORSE)
            .audience(DEFAULT_AUDIENCE)
            .active(DEFAULT_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static IndicatorDefinition createUpdatedEntity() {
        return new IndicatorDefinition()
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .source(UPDATED_SOURCE)
            .unit(UPDATED_UNIT)
            .window(UPDATED_WINDOW)
            .segmentCount(UPDATED_SEGMENT_COUNT)
            .segmentLength(UPDATED_SEGMENT_LENGTH)
            .warningThreshold(UPDATED_WARNING_THRESHOLD)
            .criticalThreshold(UPDATED_CRITICAL_THRESHOLD)
            .higherIsWorse(UPDATED_HIGHER_IS_WORSE)
            .audience(UPDATED_AUDIENCE)
            .active(UPDATED_ACTIVE);
    }

    @BeforeEach
    void initTest() {
        indicatorDefinition = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedIndicatorDefinition != null) {
            indicatorDefinitionRepository.delete(insertedIndicatorDefinition);
            insertedIndicatorDefinition = null;
        }
    }

    @Test
    @Transactional
    void createIndicatorDefinition() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the IndicatorDefinition
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);
        var returnedIndicatorDefinitionDTO = om.readValue(
            restIndicatorDefinitionMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(indicatorDefinitionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            IndicatorDefinitionDTO.class
        );

        // Validate the IndicatorDefinition in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedIndicatorDefinition = indicatorDefinitionMapper.toEntity(returnedIndicatorDefinitionDTO);
        assertIndicatorDefinitionUpdatableFieldsEquals(
            returnedIndicatorDefinition,
            getPersistedIndicatorDefinition(returnedIndicatorDefinition)
        );

        insertedIndicatorDefinition = returnedIndicatorDefinition;
    }

    @Test
    @Transactional
    void createIndicatorDefinitionWithExistingId() throws Exception {
        // Create the IndicatorDefinition with an existing ID
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restIndicatorDefinitionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the IndicatorDefinition in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        indicatorDefinition.setCode(null);

        // Create the IndicatorDefinition, which fails.
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        restIndicatorDefinitionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLabelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        indicatorDefinition.setLabel(null);

        // Create the IndicatorDefinition, which fails.
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        restIndicatorDefinitionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSourceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        indicatorDefinition.setSource(null);

        // Create the IndicatorDefinition, which fails.
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        restIndicatorDefinitionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUnitIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        indicatorDefinition.setUnit(null);

        // Create the IndicatorDefinition, which fails.
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        restIndicatorDefinitionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkWindowIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        indicatorDefinition.setWindow(null);

        // Create the IndicatorDefinition, which fails.
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        restIndicatorDefinitionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSegmentCountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        indicatorDefinition.setSegmentCount(null);

        // Create the IndicatorDefinition, which fails.
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        restIndicatorDefinitionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSegmentLengthIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        indicatorDefinition.setSegmentLength(null);

        // Create the IndicatorDefinition, which fails.
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        restIndicatorDefinitionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkHigherIsWorseIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        indicatorDefinition.setHigherIsWorse(null);

        // Create the IndicatorDefinition, which fails.
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        restIndicatorDefinitionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        indicatorDefinition.setActive(null);

        // Create the IndicatorDefinition, which fails.
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        restIndicatorDefinitionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitions() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList
        restIndicatorDefinitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(indicatorDefinition.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].unit").value(hasItem(DEFAULT_UNIT)))
            .andExpect(jsonPath("$.[*].window").value(hasItem(DEFAULT_WINDOW.toString())))
            .andExpect(jsonPath("$.[*].segmentCount").value(hasItem(DEFAULT_SEGMENT_COUNT)))
            .andExpect(jsonPath("$.[*].segmentLength").value(hasItem(DEFAULT_SEGMENT_LENGTH.toString())))
            .andExpect(jsonPath("$.[*].warningThreshold").value(hasItem(sameNumber(DEFAULT_WARNING_THRESHOLD))))
            .andExpect(jsonPath("$.[*].criticalThreshold").value(hasItem(sameNumber(DEFAULT_CRITICAL_THRESHOLD))))
            .andExpect(jsonPath("$.[*].higherIsWorse").value(hasItem(DEFAULT_HIGHER_IS_WORSE)))
            .andExpect(jsonPath("$.[*].audience").value(hasItem(DEFAULT_AUDIENCE)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @Test
    @Transactional
    void getIndicatorDefinition() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get the indicatorDefinition
        restIndicatorDefinitionMockMvc
            .perform(get(ENTITY_API_URL_ID, indicatorDefinition.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(indicatorDefinition.getId().toString()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andExpect(jsonPath("$.source").value(DEFAULT_SOURCE.toString()))
            .andExpect(jsonPath("$.unit").value(DEFAULT_UNIT))
            .andExpect(jsonPath("$.window").value(DEFAULT_WINDOW.toString()))
            .andExpect(jsonPath("$.segmentCount").value(DEFAULT_SEGMENT_COUNT))
            .andExpect(jsonPath("$.segmentLength").value(DEFAULT_SEGMENT_LENGTH.toString()))
            .andExpect(jsonPath("$.warningThreshold").value(sameNumber(DEFAULT_WARNING_THRESHOLD)))
            .andExpect(jsonPath("$.criticalThreshold").value(sameNumber(DEFAULT_CRITICAL_THRESHOLD)))
            .andExpect(jsonPath("$.higherIsWorse").value(DEFAULT_HIGHER_IS_WORSE))
            .andExpect(jsonPath("$.audience").value(DEFAULT_AUDIENCE))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getIndicatorDefinitionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        UUID id = indicatorDefinition.getId();

        defaultIndicatorDefinitionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where code equals to
        defaultIndicatorDefinitionFiltering("code.equals=" + DEFAULT_CODE, "code.equals=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where code in
        defaultIndicatorDefinitionFiltering("code.in=" + DEFAULT_CODE + "," + UPDATED_CODE, "code.in=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where code is not null
        defaultIndicatorDefinitionFiltering("code.specified=true", "code.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where code contains
        defaultIndicatorDefinitionFiltering("code.contains=" + DEFAULT_CODE, "code.contains=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where code does not contain
        defaultIndicatorDefinitionFiltering("code.doesNotContain=" + UPDATED_CODE, "code.doesNotContain=" + DEFAULT_CODE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByLabelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where label equals to
        defaultIndicatorDefinitionFiltering("label.equals=" + DEFAULT_LABEL, "label.equals=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByLabelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where label in
        defaultIndicatorDefinitionFiltering("label.in=" + DEFAULT_LABEL + "," + UPDATED_LABEL, "label.in=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByLabelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where label is not null
        defaultIndicatorDefinitionFiltering("label.specified=true", "label.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByLabelContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where label contains
        defaultIndicatorDefinitionFiltering("label.contains=" + DEFAULT_LABEL, "label.contains=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByLabelNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where label does not contain
        defaultIndicatorDefinitionFiltering("label.doesNotContain=" + UPDATED_LABEL, "label.doesNotContain=" + DEFAULT_LABEL);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySourceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where source equals to
        defaultIndicatorDefinitionFiltering("source.equals=" + DEFAULT_SOURCE, "source.equals=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySourceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where source in
        defaultIndicatorDefinitionFiltering("source.in=" + DEFAULT_SOURCE + "," + UPDATED_SOURCE, "source.in=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySourceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where source is not null
        defaultIndicatorDefinitionFiltering("source.specified=true", "source.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByUnitIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where unit equals to
        defaultIndicatorDefinitionFiltering("unit.equals=" + DEFAULT_UNIT, "unit.equals=" + UPDATED_UNIT);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByUnitIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where unit in
        defaultIndicatorDefinitionFiltering("unit.in=" + DEFAULT_UNIT + "," + UPDATED_UNIT, "unit.in=" + UPDATED_UNIT);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByUnitIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where unit is not null
        defaultIndicatorDefinitionFiltering("unit.specified=true", "unit.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByUnitContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where unit contains
        defaultIndicatorDefinitionFiltering("unit.contains=" + DEFAULT_UNIT, "unit.contains=" + UPDATED_UNIT);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByUnitNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where unit does not contain
        defaultIndicatorDefinitionFiltering("unit.doesNotContain=" + UPDATED_UNIT, "unit.doesNotContain=" + DEFAULT_UNIT);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWindowIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where window equals to
        defaultIndicatorDefinitionFiltering("window.equals=" + DEFAULT_WINDOW, "window.equals=" + UPDATED_WINDOW);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWindowIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where window in
        defaultIndicatorDefinitionFiltering("window.in=" + DEFAULT_WINDOW + "," + UPDATED_WINDOW, "window.in=" + UPDATED_WINDOW);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWindowIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where window is not null
        defaultIndicatorDefinitionFiltering("window.specified=true", "window.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWindowIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where window is greater than or equal to
        defaultIndicatorDefinitionFiltering("window.greaterThanOrEqual=" + DEFAULT_WINDOW, "window.greaterThanOrEqual=" + UPDATED_WINDOW);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWindowIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where window is less than or equal to
        defaultIndicatorDefinitionFiltering("window.lessThanOrEqual=" + DEFAULT_WINDOW, "window.lessThanOrEqual=" + SMALLER_WINDOW);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWindowIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where window is less than
        defaultIndicatorDefinitionFiltering("window.lessThan=" + UPDATED_WINDOW, "window.lessThan=" + DEFAULT_WINDOW);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWindowIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where window is greater than
        defaultIndicatorDefinitionFiltering("window.greaterThan=" + SMALLER_WINDOW, "window.greaterThan=" + DEFAULT_WINDOW);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentCount equals to
        defaultIndicatorDefinitionFiltering("segmentCount.equals=" + DEFAULT_SEGMENT_COUNT, "segmentCount.equals=" + UPDATED_SEGMENT_COUNT);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentCount in
        defaultIndicatorDefinitionFiltering(
            "segmentCount.in=" + DEFAULT_SEGMENT_COUNT + "," + UPDATED_SEGMENT_COUNT,
            "segmentCount.in=" + UPDATED_SEGMENT_COUNT
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentCount is not null
        defaultIndicatorDefinitionFiltering("segmentCount.specified=true", "segmentCount.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentCount is greater than or equal to
        defaultIndicatorDefinitionFiltering(
            "segmentCount.greaterThanOrEqual=" + DEFAULT_SEGMENT_COUNT,
            "segmentCount.greaterThanOrEqual=" + (DEFAULT_SEGMENT_COUNT + 1)
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentCount is less than or equal to
        defaultIndicatorDefinitionFiltering(
            "segmentCount.lessThanOrEqual=" + DEFAULT_SEGMENT_COUNT,
            "segmentCount.lessThanOrEqual=" + SMALLER_SEGMENT_COUNT
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentCount is less than
        defaultIndicatorDefinitionFiltering(
            "segmentCount.lessThan=" + (DEFAULT_SEGMENT_COUNT + 1),
            "segmentCount.lessThan=" + DEFAULT_SEGMENT_COUNT
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentCount is greater than
        defaultIndicatorDefinitionFiltering(
            "segmentCount.greaterThan=" + SMALLER_SEGMENT_COUNT,
            "segmentCount.greaterThan=" + DEFAULT_SEGMENT_COUNT
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentLengthIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentLength equals to
        defaultIndicatorDefinitionFiltering(
            "segmentLength.equals=" + DEFAULT_SEGMENT_LENGTH,
            "segmentLength.equals=" + UPDATED_SEGMENT_LENGTH
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentLengthIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentLength in
        defaultIndicatorDefinitionFiltering(
            "segmentLength.in=" + DEFAULT_SEGMENT_LENGTH + "," + UPDATED_SEGMENT_LENGTH,
            "segmentLength.in=" + UPDATED_SEGMENT_LENGTH
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentLengthIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentLength is not null
        defaultIndicatorDefinitionFiltering("segmentLength.specified=true", "segmentLength.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentLengthIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentLength is greater than or equal to
        defaultIndicatorDefinitionFiltering(
            "segmentLength.greaterThanOrEqual=" + DEFAULT_SEGMENT_LENGTH,
            "segmentLength.greaterThanOrEqual=" + UPDATED_SEGMENT_LENGTH
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentLengthIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentLength is less than or equal to
        defaultIndicatorDefinitionFiltering(
            "segmentLength.lessThanOrEqual=" + DEFAULT_SEGMENT_LENGTH,
            "segmentLength.lessThanOrEqual=" + SMALLER_SEGMENT_LENGTH
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentLengthIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentLength is less than
        defaultIndicatorDefinitionFiltering(
            "segmentLength.lessThan=" + UPDATED_SEGMENT_LENGTH,
            "segmentLength.lessThan=" + DEFAULT_SEGMENT_LENGTH
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsBySegmentLengthIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where segmentLength is greater than
        defaultIndicatorDefinitionFiltering(
            "segmentLength.greaterThan=" + SMALLER_SEGMENT_LENGTH,
            "segmentLength.greaterThan=" + DEFAULT_SEGMENT_LENGTH
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWarningThresholdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where warningThreshold equals to
        defaultIndicatorDefinitionFiltering(
            "warningThreshold.equals=" + DEFAULT_WARNING_THRESHOLD,
            "warningThreshold.equals=" + UPDATED_WARNING_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWarningThresholdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where warningThreshold in
        defaultIndicatorDefinitionFiltering(
            "warningThreshold.in=" + DEFAULT_WARNING_THRESHOLD + "," + UPDATED_WARNING_THRESHOLD,
            "warningThreshold.in=" + UPDATED_WARNING_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWarningThresholdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where warningThreshold is not null
        defaultIndicatorDefinitionFiltering("warningThreshold.specified=true", "warningThreshold.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWarningThresholdIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where warningThreshold is greater than or equal to
        defaultIndicatorDefinitionFiltering(
            "warningThreshold.greaterThanOrEqual=" + DEFAULT_WARNING_THRESHOLD,
            "warningThreshold.greaterThanOrEqual=" + UPDATED_WARNING_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWarningThresholdIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where warningThreshold is less than or equal to
        defaultIndicatorDefinitionFiltering(
            "warningThreshold.lessThanOrEqual=" + DEFAULT_WARNING_THRESHOLD,
            "warningThreshold.lessThanOrEqual=" + SMALLER_WARNING_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWarningThresholdIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where warningThreshold is less than
        defaultIndicatorDefinitionFiltering(
            "warningThreshold.lessThan=" + UPDATED_WARNING_THRESHOLD,
            "warningThreshold.lessThan=" + DEFAULT_WARNING_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByWarningThresholdIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where warningThreshold is greater than
        defaultIndicatorDefinitionFiltering(
            "warningThreshold.greaterThan=" + SMALLER_WARNING_THRESHOLD,
            "warningThreshold.greaterThan=" + DEFAULT_WARNING_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCriticalThresholdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where criticalThreshold equals to
        defaultIndicatorDefinitionFiltering(
            "criticalThreshold.equals=" + DEFAULT_CRITICAL_THRESHOLD,
            "criticalThreshold.equals=" + UPDATED_CRITICAL_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCriticalThresholdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where criticalThreshold in
        defaultIndicatorDefinitionFiltering(
            "criticalThreshold.in=" + DEFAULT_CRITICAL_THRESHOLD + "," + UPDATED_CRITICAL_THRESHOLD,
            "criticalThreshold.in=" + UPDATED_CRITICAL_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCriticalThresholdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where criticalThreshold is not null
        defaultIndicatorDefinitionFiltering("criticalThreshold.specified=true", "criticalThreshold.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCriticalThresholdIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where criticalThreshold is greater than or equal to
        defaultIndicatorDefinitionFiltering(
            "criticalThreshold.greaterThanOrEqual=" + DEFAULT_CRITICAL_THRESHOLD,
            "criticalThreshold.greaterThanOrEqual=" + UPDATED_CRITICAL_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCriticalThresholdIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where criticalThreshold is less than or equal to
        defaultIndicatorDefinitionFiltering(
            "criticalThreshold.lessThanOrEqual=" + DEFAULT_CRITICAL_THRESHOLD,
            "criticalThreshold.lessThanOrEqual=" + SMALLER_CRITICAL_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCriticalThresholdIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where criticalThreshold is less than
        defaultIndicatorDefinitionFiltering(
            "criticalThreshold.lessThan=" + UPDATED_CRITICAL_THRESHOLD,
            "criticalThreshold.lessThan=" + DEFAULT_CRITICAL_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCriticalThresholdIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where criticalThreshold is greater than
        defaultIndicatorDefinitionFiltering(
            "criticalThreshold.greaterThan=" + SMALLER_CRITICAL_THRESHOLD,
            "criticalThreshold.greaterThan=" + DEFAULT_CRITICAL_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByHigherIsWorseIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where higherIsWorse equals to
        defaultIndicatorDefinitionFiltering(
            "higherIsWorse.equals=" + DEFAULT_HIGHER_IS_WORSE,
            "higherIsWorse.equals=" + UPDATED_HIGHER_IS_WORSE
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByHigherIsWorseIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where higherIsWorse in
        defaultIndicatorDefinitionFiltering(
            "higherIsWorse.in=" + DEFAULT_HIGHER_IS_WORSE + "," + UPDATED_HIGHER_IS_WORSE,
            "higherIsWorse.in=" + UPDATED_HIGHER_IS_WORSE
        );
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByHigherIsWorseIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where higherIsWorse is not null
        defaultIndicatorDefinitionFiltering("higherIsWorse.specified=true", "higherIsWorse.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByAudienceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where audience equals to
        defaultIndicatorDefinitionFiltering("audience.equals=" + DEFAULT_AUDIENCE, "audience.equals=" + UPDATED_AUDIENCE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByAudienceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where audience in
        defaultIndicatorDefinitionFiltering("audience.in=" + DEFAULT_AUDIENCE + "," + UPDATED_AUDIENCE, "audience.in=" + UPDATED_AUDIENCE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByAudienceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where audience is not null
        defaultIndicatorDefinitionFiltering("audience.specified=true", "audience.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByAudienceContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where audience contains
        defaultIndicatorDefinitionFiltering("audience.contains=" + DEFAULT_AUDIENCE, "audience.contains=" + UPDATED_AUDIENCE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByAudienceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where audience does not contain
        defaultIndicatorDefinitionFiltering("audience.doesNotContain=" + UPDATED_AUDIENCE, "audience.doesNotContain=" + DEFAULT_AUDIENCE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByActiveIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where active equals to
        defaultIndicatorDefinitionFiltering("active.equals=" + DEFAULT_ACTIVE, "active.equals=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByActiveIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where active in
        defaultIndicatorDefinitionFiltering("active.in=" + DEFAULT_ACTIVE + "," + UPDATED_ACTIVE, "active.in=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByActiveIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        // Get all the indicatorDefinitionList where active is not null
        defaultIndicatorDefinitionFiltering("active.specified=true", "active.specified=false");
    }

    @Test
    @Transactional
    void getAllIndicatorDefinitionsByCockpitsIsEqualToSomething() throws Exception {
        CockpitView cockpits;
        if (TestUtil.findAll(em, CockpitView.class).isEmpty()) {
            indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);
            cockpits = CockpitViewResourceIT.createEntity();
        } else {
            cockpits = TestUtil.findAll(em, CockpitView.class).get(0);
        }
        em.persist(cockpits);
        em.flush();
        indicatorDefinition.addCockpits(cockpits);
        indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);
        UUID cockpitsId = cockpits.getId();
        // Get all the indicatorDefinitionList where cockpits equals to cockpitsId
        defaultIndicatorDefinitionShouldBeFound("cockpitsId.equals=" + cockpitsId);

        // Get all the indicatorDefinitionList where cockpits equals to UUID.randomUUID()
        defaultIndicatorDefinitionShouldNotBeFound("cockpitsId.equals=" + UUID.randomUUID());
    }

    private void defaultIndicatorDefinitionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultIndicatorDefinitionShouldBeFound(shouldBeFound);
        defaultIndicatorDefinitionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultIndicatorDefinitionShouldBeFound(String filter) throws Exception {
        restIndicatorDefinitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(indicatorDefinition.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].unit").value(hasItem(DEFAULT_UNIT)))
            .andExpect(jsonPath("$.[*].window").value(hasItem(DEFAULT_WINDOW.toString())))
            .andExpect(jsonPath("$.[*].segmentCount").value(hasItem(DEFAULT_SEGMENT_COUNT)))
            .andExpect(jsonPath("$.[*].segmentLength").value(hasItem(DEFAULT_SEGMENT_LENGTH.toString())))
            .andExpect(jsonPath("$.[*].warningThreshold").value(hasItem(sameNumber(DEFAULT_WARNING_THRESHOLD))))
            .andExpect(jsonPath("$.[*].criticalThreshold").value(hasItem(sameNumber(DEFAULT_CRITICAL_THRESHOLD))))
            .andExpect(jsonPath("$.[*].higherIsWorse").value(hasItem(DEFAULT_HIGHER_IS_WORSE)))
            .andExpect(jsonPath("$.[*].audience").value(hasItem(DEFAULT_AUDIENCE)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));

        // Check, that the count call also returns 1
        restIndicatorDefinitionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultIndicatorDefinitionShouldNotBeFound(String filter) throws Exception {
        restIndicatorDefinitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restIndicatorDefinitionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingIndicatorDefinition() throws Exception {
        // Get the indicatorDefinition
        restIndicatorDefinitionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingIndicatorDefinition() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the indicatorDefinition
        IndicatorDefinition updatedIndicatorDefinition = indicatorDefinitionRepository.findById(indicatorDefinition.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedIndicatorDefinition are not directly saved in db
        em.detach(updatedIndicatorDefinition);
        updatedIndicatorDefinition
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .source(UPDATED_SOURCE)
            .unit(UPDATED_UNIT)
            .window(UPDATED_WINDOW)
            .segmentCount(UPDATED_SEGMENT_COUNT)
            .segmentLength(UPDATED_SEGMENT_LENGTH)
            .warningThreshold(UPDATED_WARNING_THRESHOLD)
            .criticalThreshold(UPDATED_CRITICAL_THRESHOLD)
            .higherIsWorse(UPDATED_HIGHER_IS_WORSE)
            .audience(UPDATED_AUDIENCE)
            .active(UPDATED_ACTIVE);
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(updatedIndicatorDefinition);

        restIndicatorDefinitionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, indicatorDefinitionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isOk());

        // Validate the IndicatorDefinition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedIndicatorDefinitionToMatchAllProperties(updatedIndicatorDefinition);
    }

    @Test
    @Transactional
    void putNonExistingIndicatorDefinition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        indicatorDefinition.setId(UUID.randomUUID());

        // Create the IndicatorDefinition
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restIndicatorDefinitionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, indicatorDefinitionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the IndicatorDefinition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchIndicatorDefinition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        indicatorDefinition.setId(UUID.randomUUID());

        // Create the IndicatorDefinition
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIndicatorDefinitionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the IndicatorDefinition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamIndicatorDefinition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        indicatorDefinition.setId(UUID.randomUUID());

        // Create the IndicatorDefinition
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIndicatorDefinitionMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the IndicatorDefinition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateIndicatorDefinitionWithPatch() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the indicatorDefinition using partial update
        IndicatorDefinition partialUpdatedIndicatorDefinition = new IndicatorDefinition();
        partialUpdatedIndicatorDefinition.setId(indicatorDefinition.getId());

        partialUpdatedIndicatorDefinition
            .label(UPDATED_LABEL)
            .unit(UPDATED_UNIT)
            .window(UPDATED_WINDOW)
            .segmentCount(UPDATED_SEGMENT_COUNT)
            .segmentLength(UPDATED_SEGMENT_LENGTH)
            .warningThreshold(UPDATED_WARNING_THRESHOLD)
            .higherIsWorse(UPDATED_HIGHER_IS_WORSE)
            .audience(UPDATED_AUDIENCE);

        restIndicatorDefinitionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedIndicatorDefinition.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedIndicatorDefinition))
            )
            .andExpect(status().isOk());

        // Validate the IndicatorDefinition in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertIndicatorDefinitionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedIndicatorDefinition, indicatorDefinition),
            getPersistedIndicatorDefinition(indicatorDefinition)
        );
    }

    @Test
    @Transactional
    void fullUpdateIndicatorDefinitionWithPatch() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the indicatorDefinition using partial update
        IndicatorDefinition partialUpdatedIndicatorDefinition = new IndicatorDefinition();
        partialUpdatedIndicatorDefinition.setId(indicatorDefinition.getId());

        partialUpdatedIndicatorDefinition
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .source(UPDATED_SOURCE)
            .unit(UPDATED_UNIT)
            .window(UPDATED_WINDOW)
            .segmentCount(UPDATED_SEGMENT_COUNT)
            .segmentLength(UPDATED_SEGMENT_LENGTH)
            .warningThreshold(UPDATED_WARNING_THRESHOLD)
            .criticalThreshold(UPDATED_CRITICAL_THRESHOLD)
            .higherIsWorse(UPDATED_HIGHER_IS_WORSE)
            .audience(UPDATED_AUDIENCE)
            .active(UPDATED_ACTIVE);

        restIndicatorDefinitionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedIndicatorDefinition.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedIndicatorDefinition))
            )
            .andExpect(status().isOk());

        // Validate the IndicatorDefinition in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertIndicatorDefinitionUpdatableFieldsEquals(
            partialUpdatedIndicatorDefinition,
            getPersistedIndicatorDefinition(partialUpdatedIndicatorDefinition)
        );
    }

    @Test
    @Transactional
    void patchNonExistingIndicatorDefinition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        indicatorDefinition.setId(UUID.randomUUID());

        // Create the IndicatorDefinition
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restIndicatorDefinitionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, indicatorDefinitionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the IndicatorDefinition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchIndicatorDefinition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        indicatorDefinition.setId(UUID.randomUUID());

        // Create the IndicatorDefinition
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIndicatorDefinitionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the IndicatorDefinition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamIndicatorDefinition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        indicatorDefinition.setId(UUID.randomUUID());

        // Create the IndicatorDefinition
        IndicatorDefinitionDTO indicatorDefinitionDTO = indicatorDefinitionMapper.toDto(indicatorDefinition);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIndicatorDefinitionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(indicatorDefinitionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the IndicatorDefinition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteIndicatorDefinition() throws Exception {
        // Initialize the database
        insertedIndicatorDefinition = indicatorDefinitionRepository.saveAndFlush(indicatorDefinition);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the indicatorDefinition
        restIndicatorDefinitionMockMvc
            .perform(delete(ENTITY_API_URL_ID, indicatorDefinition.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return indicatorDefinitionRepository.count();
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

    protected IndicatorDefinition getPersistedIndicatorDefinition(IndicatorDefinition indicatorDefinition) {
        return indicatorDefinitionRepository.findById(indicatorDefinition.getId()).orElseThrow();
    }

    protected void assertPersistedIndicatorDefinitionToMatchAllProperties(IndicatorDefinition expectedIndicatorDefinition) {
        assertIndicatorDefinitionAllPropertiesEquals(
            expectedIndicatorDefinition,
            getPersistedIndicatorDefinition(expectedIndicatorDefinition)
        );
    }

    protected void assertPersistedIndicatorDefinitionToMatchUpdatableProperties(IndicatorDefinition expectedIndicatorDefinition) {
        assertIndicatorDefinitionAllUpdatablePropertiesEquals(
            expectedIndicatorDefinition,
            getPersistedIndicatorDefinition(expectedIndicatorDefinition)
        );
    }
}
