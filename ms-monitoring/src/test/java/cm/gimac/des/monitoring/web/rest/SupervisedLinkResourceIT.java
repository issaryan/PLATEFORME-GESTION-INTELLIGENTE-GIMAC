package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.SupervisedLinkAsserts.*;
import static cm.gimac.des.monitoring.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.domain.enumeration.LinkScope;
import cm.gimac.des.monitoring.domain.enumeration.LinkState;
import cm.gimac.des.monitoring.repository.SupervisedLinkRepository;
import cm.gimac.des.monitoring.service.SupervisedLinkService;
import cm.gimac.des.monitoring.service.dto.SupervisedLinkDTO;
import cm.gimac.des.monitoring.service.mapper.SupervisedLinkMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Duration;
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
 * Integration tests for the {@link SupervisedLinkResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class SupervisedLinkResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final LinkScope DEFAULT_SCOPE = LinkScope.INSTITUTION;
    private static final LinkScope UPDATED_SCOPE = LinkScope.PLATEFORME;

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final String DEFAULT_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_TYPE = "BBBBBBBBBB";

    private static final LinkState DEFAULT_STATE = LinkState.NOMINALE;
    private static final LinkState UPDATED_STATE = LinkState.DEGRADEE;

    private static final Instant DEFAULT_STATE_SINCE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STATE_SINCE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LAST_MEASURE_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_MEASURE_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Duration DEFAULT_MEASURE_PERIOD = Duration.ofHours(6);
    private static final Duration UPDATED_MEASURE_PERIOD = Duration.ofHours(12);
    private static final Duration SMALLER_MEASURE_PERIOD = Duration.ofHours(5);

    private static final Instant DEFAULT_MAINTENANCE_FROM = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_MAINTENANCE_FROM = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_MAINTENANCE_UNTIL = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_MAINTENANCE_UNTIL = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/supervised-links";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SupervisedLinkRepository supervisedLinkRepository;

    @Mock
    private SupervisedLinkRepository supervisedLinkRepositoryMock;

    @Autowired
    private SupervisedLinkMapper supervisedLinkMapper;

    @Mock
    private SupervisedLinkService supervisedLinkServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSupervisedLinkMockMvc;

    private SupervisedLink supervisedLink;

    private SupervisedLink insertedSupervisedLink;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SupervisedLink createEntity() {
        return new SupervisedLink()
            .code(DEFAULT_CODE)
            .label(DEFAULT_LABEL)
            .scope(DEFAULT_SCOPE)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .type(DEFAULT_TYPE)
            .state(DEFAULT_STATE)
            .stateSince(DEFAULT_STATE_SINCE)
            .lastMeasureAt(DEFAULT_LAST_MEASURE_AT)
            .measurePeriod(DEFAULT_MEASURE_PERIOD)
            .maintenanceFrom(DEFAULT_MAINTENANCE_FROM)
            .maintenanceUntil(DEFAULT_MAINTENANCE_UNTIL);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SupervisedLink createUpdatedEntity() {
        return new SupervisedLink()
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .scope(UPDATED_SCOPE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .type(UPDATED_TYPE)
            .state(UPDATED_STATE)
            .stateSince(UPDATED_STATE_SINCE)
            .lastMeasureAt(UPDATED_LAST_MEASURE_AT)
            .measurePeriod(UPDATED_MEASURE_PERIOD)
            .maintenanceFrom(UPDATED_MAINTENANCE_FROM)
            .maintenanceUntil(UPDATED_MAINTENANCE_UNTIL);
    }

    @BeforeEach
    void initTest() {
        supervisedLink = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedSupervisedLink != null) {
            supervisedLinkRepository.delete(insertedSupervisedLink);
            insertedSupervisedLink = null;
        }
    }

    @Test
    @Transactional
    void createSupervisedLink() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the SupervisedLink
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);
        var returnedSupervisedLinkDTO = om.readValue(
            restSupervisedLinkMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(supervisedLinkDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SupervisedLinkDTO.class
        );

        // Validate the SupervisedLink in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSupervisedLink = supervisedLinkMapper.toEntity(returnedSupervisedLinkDTO);
        assertSupervisedLinkUpdatableFieldsEquals(returnedSupervisedLink, getPersistedSupervisedLink(returnedSupervisedLink));

        insertedSupervisedLink = returnedSupervisedLink;
    }

    @Test
    @Transactional
    void createSupervisedLinkWithExistingId() throws Exception {
        // Create the SupervisedLink with an existing ID
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSupervisedLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SupervisedLink in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        supervisedLink.setCode(null);

        // Create the SupervisedLink, which fails.
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        restSupervisedLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLabelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        supervisedLink.setLabel(null);

        // Create the SupervisedLink, which fails.
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        restSupervisedLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkScopeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        supervisedLink.setScope(null);

        // Create the SupervisedLink, which fails.
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        restSupervisedLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        supervisedLink.setType(null);

        // Create the SupervisedLink, which fails.
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        restSupervisedLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        supervisedLink.setState(null);

        // Create the SupervisedLink, which fails.
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        restSupervisedLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateSinceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        supervisedLink.setStateSince(null);

        // Create the SupervisedLink, which fails.
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        restSupervisedLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMeasurePeriodIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        supervisedLink.setMeasurePeriod(null);

        // Create the SupervisedLink, which fails.
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        restSupervisedLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSupervisedLinks() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList
        restSupervisedLinkMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(supervisedLink.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].scope").value(hasItem(DEFAULT_SCOPE.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].stateSince").value(hasItem(DEFAULT_STATE_SINCE.toString())))
            .andExpect(jsonPath("$.[*].lastMeasureAt").value(hasItem(DEFAULT_LAST_MEASURE_AT.toString())))
            .andExpect(jsonPath("$.[*].measurePeriod").value(hasItem(DEFAULT_MEASURE_PERIOD.toString())))
            .andExpect(jsonPath("$.[*].maintenanceFrom").value(hasItem(DEFAULT_MAINTENANCE_FROM.toString())))
            .andExpect(jsonPath("$.[*].maintenanceUntil").value(hasItem(DEFAULT_MAINTENANCE_UNTIL.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSupervisedLinksWithEagerRelationshipsIsEnabled() throws Exception {
        when(supervisedLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSupervisedLinkMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(supervisedLinkServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSupervisedLinksWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(supervisedLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSupervisedLinkMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(supervisedLinkRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getSupervisedLink() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get the supervisedLink
        restSupervisedLinkMockMvc
            .perform(get(ENTITY_API_URL_ID, supervisedLink.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(supervisedLink.getId().toString()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andExpect(jsonPath("$.scope").value(DEFAULT_SCOPE.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.stateSince").value(DEFAULT_STATE_SINCE.toString()))
            .andExpect(jsonPath("$.lastMeasureAt").value(DEFAULT_LAST_MEASURE_AT.toString()))
            .andExpect(jsonPath("$.measurePeriod").value(DEFAULT_MEASURE_PERIOD.toString()))
            .andExpect(jsonPath("$.maintenanceFrom").value(DEFAULT_MAINTENANCE_FROM.toString()))
            .andExpect(jsonPath("$.maintenanceUntil").value(DEFAULT_MAINTENANCE_UNTIL.toString()));
    }

    @Test
    @Transactional
    void getSupervisedLinksByIdFiltering() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        UUID id = supervisedLink.getId();

        defaultSupervisedLinkFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where code equals to
        defaultSupervisedLinkFiltering("code.equals=" + DEFAULT_CODE, "code.equals=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where code in
        defaultSupervisedLinkFiltering("code.in=" + DEFAULT_CODE + "," + UPDATED_CODE, "code.in=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where code is not null
        defaultSupervisedLinkFiltering("code.specified=true", "code.specified=false");
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where code contains
        defaultSupervisedLinkFiltering("code.contains=" + DEFAULT_CODE, "code.contains=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where code does not contain
        defaultSupervisedLinkFiltering("code.doesNotContain=" + UPDATED_CODE, "code.doesNotContain=" + DEFAULT_CODE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByLabelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where label equals to
        defaultSupervisedLinkFiltering("label.equals=" + DEFAULT_LABEL, "label.equals=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByLabelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where label in
        defaultSupervisedLinkFiltering("label.in=" + DEFAULT_LABEL + "," + UPDATED_LABEL, "label.in=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByLabelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where label is not null
        defaultSupervisedLinkFiltering("label.specified=true", "label.specified=false");
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByLabelContainsSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where label contains
        defaultSupervisedLinkFiltering("label.contains=" + DEFAULT_LABEL, "label.contains=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByLabelNotContainsSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where label does not contain
        defaultSupervisedLinkFiltering("label.doesNotContain=" + UPDATED_LABEL, "label.doesNotContain=" + DEFAULT_LABEL);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByScopeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where scope equals to
        defaultSupervisedLinkFiltering("scope.equals=" + DEFAULT_SCOPE, "scope.equals=" + UPDATED_SCOPE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByScopeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where scope in
        defaultSupervisedLinkFiltering("scope.in=" + DEFAULT_SCOPE + "," + UPDATED_SCOPE, "scope.in=" + UPDATED_SCOPE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByScopeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where scope is not null
        defaultSupervisedLinkFiltering("scope.specified=true", "scope.specified=false");
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where institutionId equals to
        defaultSupervisedLinkFiltering("institutionId.equals=" + DEFAULT_INSTITUTION_ID, "institutionId.equals=" + UPDATED_INSTITUTION_ID);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where institutionId in
        defaultSupervisedLinkFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where institutionId is not null
        defaultSupervisedLinkFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where type equals to
        defaultSupervisedLinkFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where type in
        defaultSupervisedLinkFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where type is not null
        defaultSupervisedLinkFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByTypeContainsSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where type contains
        defaultSupervisedLinkFiltering("type.contains=" + DEFAULT_TYPE, "type.contains=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByTypeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where type does not contain
        defaultSupervisedLinkFiltering("type.doesNotContain=" + UPDATED_TYPE, "type.doesNotContain=" + DEFAULT_TYPE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where state equals to
        defaultSupervisedLinkFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where state in
        defaultSupervisedLinkFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where state is not null
        defaultSupervisedLinkFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByStateSinceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where stateSince equals to
        defaultSupervisedLinkFiltering("stateSince.equals=" + DEFAULT_STATE_SINCE, "stateSince.equals=" + UPDATED_STATE_SINCE);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByStateSinceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where stateSince in
        defaultSupervisedLinkFiltering(
            "stateSince.in=" + DEFAULT_STATE_SINCE + "," + UPDATED_STATE_SINCE,
            "stateSince.in=" + UPDATED_STATE_SINCE
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByStateSinceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where stateSince is not null
        defaultSupervisedLinkFiltering("stateSince.specified=true", "stateSince.specified=false");
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByLastMeasureAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where lastMeasureAt equals to
        defaultSupervisedLinkFiltering(
            "lastMeasureAt.equals=" + DEFAULT_LAST_MEASURE_AT,
            "lastMeasureAt.equals=" + UPDATED_LAST_MEASURE_AT
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByLastMeasureAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where lastMeasureAt in
        defaultSupervisedLinkFiltering(
            "lastMeasureAt.in=" + DEFAULT_LAST_MEASURE_AT + "," + UPDATED_LAST_MEASURE_AT,
            "lastMeasureAt.in=" + UPDATED_LAST_MEASURE_AT
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByLastMeasureAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where lastMeasureAt is not null
        defaultSupervisedLinkFiltering("lastMeasureAt.specified=true", "lastMeasureAt.specified=false");
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMeasurePeriodIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where measurePeriod equals to
        defaultSupervisedLinkFiltering("measurePeriod.equals=" + DEFAULT_MEASURE_PERIOD, "measurePeriod.equals=" + UPDATED_MEASURE_PERIOD);
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMeasurePeriodIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where measurePeriod in
        defaultSupervisedLinkFiltering(
            "measurePeriod.in=" + DEFAULT_MEASURE_PERIOD + "," + UPDATED_MEASURE_PERIOD,
            "measurePeriod.in=" + UPDATED_MEASURE_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMeasurePeriodIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where measurePeriod is not null
        defaultSupervisedLinkFiltering("measurePeriod.specified=true", "measurePeriod.specified=false");
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMeasurePeriodIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where measurePeriod is greater than or equal to
        defaultSupervisedLinkFiltering(
            "measurePeriod.greaterThanOrEqual=" + DEFAULT_MEASURE_PERIOD,
            "measurePeriod.greaterThanOrEqual=" + UPDATED_MEASURE_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMeasurePeriodIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where measurePeriod is less than or equal to
        defaultSupervisedLinkFiltering(
            "measurePeriod.lessThanOrEqual=" + DEFAULT_MEASURE_PERIOD,
            "measurePeriod.lessThanOrEqual=" + SMALLER_MEASURE_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMeasurePeriodIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where measurePeriod is less than
        defaultSupervisedLinkFiltering(
            "measurePeriod.lessThan=" + UPDATED_MEASURE_PERIOD,
            "measurePeriod.lessThan=" + DEFAULT_MEASURE_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMeasurePeriodIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where measurePeriod is greater than
        defaultSupervisedLinkFiltering(
            "measurePeriod.greaterThan=" + SMALLER_MEASURE_PERIOD,
            "measurePeriod.greaterThan=" + DEFAULT_MEASURE_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMaintenanceFromIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where maintenanceFrom equals to
        defaultSupervisedLinkFiltering(
            "maintenanceFrom.equals=" + DEFAULT_MAINTENANCE_FROM,
            "maintenanceFrom.equals=" + UPDATED_MAINTENANCE_FROM
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMaintenanceFromIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where maintenanceFrom in
        defaultSupervisedLinkFiltering(
            "maintenanceFrom.in=" + DEFAULT_MAINTENANCE_FROM + "," + UPDATED_MAINTENANCE_FROM,
            "maintenanceFrom.in=" + UPDATED_MAINTENANCE_FROM
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMaintenanceFromIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where maintenanceFrom is not null
        defaultSupervisedLinkFiltering("maintenanceFrom.specified=true", "maintenanceFrom.specified=false");
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMaintenanceUntilIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where maintenanceUntil equals to
        defaultSupervisedLinkFiltering(
            "maintenanceUntil.equals=" + DEFAULT_MAINTENANCE_UNTIL,
            "maintenanceUntil.equals=" + UPDATED_MAINTENANCE_UNTIL
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMaintenanceUntilIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where maintenanceUntil in
        defaultSupervisedLinkFiltering(
            "maintenanceUntil.in=" + DEFAULT_MAINTENANCE_UNTIL + "," + UPDATED_MAINTENANCE_UNTIL,
            "maintenanceUntil.in=" + UPDATED_MAINTENANCE_UNTIL
        );
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByMaintenanceUntilIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        // Get all the supervisedLinkList where maintenanceUntil is not null
        defaultSupervisedLinkFiltering("maintenanceUntil.specified=true", "maintenanceUntil.specified=false");
    }

    @Test
    @Transactional
    void getAllSupervisedLinksByParentPlatformIsEqualToSomething() throws Exception {
        SupervisedLink parentPlatform;
        if (TestUtil.findAll(em, SupervisedLink.class).isEmpty()) {
            supervisedLinkRepository.saveAndFlush(supervisedLink);
            parentPlatform = SupervisedLinkResourceIT.createUpdatedEntity(); // champs uniques distincts de l'entité testée
        } else {
            parentPlatform = TestUtil.findAll(em, SupervisedLink.class).get(0);
        }
        em.persist(parentPlatform);
        em.flush();
        supervisedLink.setParentPlatform(parentPlatform);
        supervisedLinkRepository.saveAndFlush(supervisedLink);
        UUID parentPlatformId = parentPlatform.getId();
        // Get all the supervisedLinkList where parentPlatform equals to parentPlatformId
        defaultSupervisedLinkShouldBeFound("parentPlatformId.equals=" + parentPlatformId);

        // Get all the supervisedLinkList where parentPlatform equals to UUID.randomUUID()
        defaultSupervisedLinkShouldNotBeFound("parentPlatformId.equals=" + UUID.randomUUID());
    }

    private void defaultSupervisedLinkFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultSupervisedLinkShouldBeFound(shouldBeFound);
        defaultSupervisedLinkShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultSupervisedLinkShouldBeFound(String filter) throws Exception {
        restSupervisedLinkMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(supervisedLink.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].scope").value(hasItem(DEFAULT_SCOPE.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].stateSince").value(hasItem(DEFAULT_STATE_SINCE.toString())))
            .andExpect(jsonPath("$.[*].lastMeasureAt").value(hasItem(DEFAULT_LAST_MEASURE_AT.toString())))
            .andExpect(jsonPath("$.[*].measurePeriod").value(hasItem(DEFAULT_MEASURE_PERIOD.toString())))
            .andExpect(jsonPath("$.[*].maintenanceFrom").value(hasItem(DEFAULT_MAINTENANCE_FROM.toString())))
            .andExpect(jsonPath("$.[*].maintenanceUntil").value(hasItem(DEFAULT_MAINTENANCE_UNTIL.toString())));

        // Check, that the count call also returns 1
        restSupervisedLinkMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultSupervisedLinkShouldNotBeFound(String filter) throws Exception {
        restSupervisedLinkMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restSupervisedLinkMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingSupervisedLink() throws Exception {
        // Get the supervisedLink
        restSupervisedLinkMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSupervisedLink() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the supervisedLink
        SupervisedLink updatedSupervisedLink = supervisedLinkRepository.findById(supervisedLink.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSupervisedLink are not directly saved in db
        em.detach(updatedSupervisedLink);
        updatedSupervisedLink
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .scope(UPDATED_SCOPE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .type(UPDATED_TYPE)
            .state(UPDATED_STATE)
            .stateSince(UPDATED_STATE_SINCE)
            .lastMeasureAt(UPDATED_LAST_MEASURE_AT)
            .measurePeriod(UPDATED_MEASURE_PERIOD)
            .maintenanceFrom(UPDATED_MAINTENANCE_FROM)
            .maintenanceUntil(UPDATED_MAINTENANCE_UNTIL);
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(updatedSupervisedLink);

        restSupervisedLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, supervisedLinkDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isOk());

        // Validate the SupervisedLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSupervisedLinkToMatchAllProperties(updatedSupervisedLink);
    }

    @Test
    @Transactional
    void putNonExistingSupervisedLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        supervisedLink.setId(UUID.randomUUID());

        // Create the SupervisedLink
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSupervisedLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, supervisedLinkDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SupervisedLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSupervisedLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        supervisedLink.setId(UUID.randomUUID());

        // Create the SupervisedLink
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSupervisedLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SupervisedLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSupervisedLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        supervisedLink.setId(UUID.randomUUID());

        // Create the SupervisedLink
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSupervisedLinkMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SupervisedLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSupervisedLinkWithPatch() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the supervisedLink using partial update
        SupervisedLink partialUpdatedSupervisedLink = new SupervisedLink();
        partialUpdatedSupervisedLink.setId(supervisedLink.getId());

        partialUpdatedSupervisedLink
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .scope(UPDATED_SCOPE)
            .type(UPDATED_TYPE)
            .state(UPDATED_STATE)
            .lastMeasureAt(UPDATED_LAST_MEASURE_AT);

        restSupervisedLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSupervisedLink.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSupervisedLink))
            )
            .andExpect(status().isOk());

        // Validate the SupervisedLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSupervisedLinkUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedSupervisedLink, supervisedLink),
            getPersistedSupervisedLink(supervisedLink)
        );
    }

    @Test
    @Transactional
    void fullUpdateSupervisedLinkWithPatch() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the supervisedLink using partial update
        SupervisedLink partialUpdatedSupervisedLink = new SupervisedLink();
        partialUpdatedSupervisedLink.setId(supervisedLink.getId());

        partialUpdatedSupervisedLink
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .scope(UPDATED_SCOPE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .type(UPDATED_TYPE)
            .state(UPDATED_STATE)
            .stateSince(UPDATED_STATE_SINCE)
            .lastMeasureAt(UPDATED_LAST_MEASURE_AT)
            .measurePeriod(UPDATED_MEASURE_PERIOD)
            .maintenanceFrom(UPDATED_MAINTENANCE_FROM)
            .maintenanceUntil(UPDATED_MAINTENANCE_UNTIL);

        restSupervisedLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSupervisedLink.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSupervisedLink))
            )
            .andExpect(status().isOk());

        // Validate the SupervisedLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSupervisedLinkUpdatableFieldsEquals(partialUpdatedSupervisedLink, getPersistedSupervisedLink(partialUpdatedSupervisedLink));
    }

    @Test
    @Transactional
    void patchNonExistingSupervisedLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        supervisedLink.setId(UUID.randomUUID());

        // Create the SupervisedLink
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSupervisedLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, supervisedLinkDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SupervisedLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSupervisedLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        supervisedLink.setId(UUID.randomUUID());

        // Create the SupervisedLink
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSupervisedLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SupervisedLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSupervisedLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        supervisedLink.setId(UUID.randomUUID());

        // Create the SupervisedLink
        SupervisedLinkDTO supervisedLinkDTO = supervisedLinkMapper.toDto(supervisedLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSupervisedLinkMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(supervisedLinkDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SupervisedLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSupervisedLink() throws Exception {
        // Initialize the database
        insertedSupervisedLink = supervisedLinkRepository.saveAndFlush(supervisedLink);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the supervisedLink
        restSupervisedLinkMockMvc
            .perform(delete(ENTITY_API_URL_ID, supervisedLink.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return supervisedLinkRepository.count();
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

    protected SupervisedLink getPersistedSupervisedLink(SupervisedLink supervisedLink) {
        return supervisedLinkRepository.findById(supervisedLink.getId()).orElseThrow();
    }

    protected void assertPersistedSupervisedLinkToMatchAllProperties(SupervisedLink expectedSupervisedLink) {
        assertSupervisedLinkAllPropertiesEquals(expectedSupervisedLink, getPersistedSupervisedLink(expectedSupervisedLink));
    }

    protected void assertPersistedSupervisedLinkToMatchUpdatableProperties(SupervisedLink expectedSupervisedLink) {
        assertSupervisedLinkAllUpdatablePropertiesEquals(expectedSupervisedLink, getPersistedSupervisedLink(expectedSupervisedLink));
    }
}
