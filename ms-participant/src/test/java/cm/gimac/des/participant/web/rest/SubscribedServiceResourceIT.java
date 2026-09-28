package cm.gimac.des.participant.web.rest;

import static cm.gimac.des.participant.domain.SubscribedServiceAsserts.*;
import static cm.gimac.des.participant.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.participant.IntegrationTest;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.SubscribedService;
import cm.gimac.des.participant.domain.enumeration.SubscriptionState;
import cm.gimac.des.participant.repository.SubscribedServiceRepository;
import cm.gimac.des.participant.service.SubscribedServiceService;
import cm.gimac.des.participant.service.dto.SubscribedServiceDTO;
import cm.gimac.des.participant.service.mapper.SubscribedServiceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Integration tests for the {@link SubscribedServiceResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class SubscribedServiceResourceIT {

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_SUBSCRIBED_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_SUBSCRIBED_ON = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_SUBSCRIBED_ON = LocalDate.ofEpochDay(-1L);

    private static final LocalDate DEFAULT_TERMINATED_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_TERMINATED_ON = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_TERMINATED_ON = LocalDate.ofEpochDay(-1L);

    private static final SubscriptionState DEFAULT_STATE = SubscriptionState.ACTIF;
    private static final SubscriptionState UPDATED_STATE = SubscriptionState.RESILIE;

    private static final String ENTITY_API_URL = "/api/subscribed-services";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SubscribedServiceRepository subscribedServiceRepository;

    @Mock
    private SubscribedServiceRepository subscribedServiceRepositoryMock;

    @Autowired
    private SubscribedServiceMapper subscribedServiceMapper;

    @Mock
    private SubscribedServiceService subscribedServiceServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSubscribedServiceMockMvc;

    private SubscribedService subscribedService;

    private SubscribedService insertedSubscribedService;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SubscribedService createEntity(EntityManager em) {
        SubscribedService subscribedService = new SubscribedService()
            .serviceCode(DEFAULT_SERVICE_CODE)
            .subscribedOn(DEFAULT_SUBSCRIBED_ON)
            .terminatedOn(DEFAULT_TERMINATED_ON)
            .state(DEFAULT_STATE);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).get(0);
        }
        subscribedService.setInstitution(institution);
        return subscribedService;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SubscribedService createUpdatedEntity(EntityManager em) {
        SubscribedService updatedSubscribedService = new SubscribedService()
            .serviceCode(UPDATED_SERVICE_CODE)
            .subscribedOn(UPDATED_SUBSCRIBED_ON)
            .terminatedOn(UPDATED_TERMINATED_ON)
            .state(UPDATED_STATE);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createUpdatedEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).get(0);
        }
        updatedSubscribedService.setInstitution(institution);
        return updatedSubscribedService;
    }

    @BeforeEach
    void initTest() {
        subscribedService = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSubscribedService != null) {
            subscribedServiceRepository.delete(insertedSubscribedService);
            insertedSubscribedService = null;
        }
    }

    @Test
    @Transactional
    void createSubscribedService() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the SubscribedService
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(subscribedService);
        var returnedSubscribedServiceDTO = om.readValue(
            restSubscribedServiceMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(subscribedServiceDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SubscribedServiceDTO.class
        );

        // Validate the SubscribedService in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSubscribedService = subscribedServiceMapper.toEntity(returnedSubscribedServiceDTO);
        assertSubscribedServiceUpdatableFieldsEquals(returnedSubscribedService, getPersistedSubscribedService(returnedSubscribedService));

        insertedSubscribedService = returnedSubscribedService;
    }

    @Test
    @Transactional
    void createSubscribedServiceWithExistingId() throws Exception {
        // Create the SubscribedService with an existing ID
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(subscribedService);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSubscribedServiceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(subscribedServiceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SubscribedService in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkServiceCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        subscribedService.setServiceCode(null);

        // Create the SubscribedService, which fails.
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(subscribedService);

        restSubscribedServiceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(subscribedServiceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSubscribedOnIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        subscribedService.setSubscribedOn(null);

        // Create the SubscribedService, which fails.
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(subscribedService);

        restSubscribedServiceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(subscribedServiceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        subscribedService.setState(null);

        // Create the SubscribedService, which fails.
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(subscribedService);

        restSubscribedServiceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(subscribedServiceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSubscribedServices() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList
        restSubscribedServiceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(subscribedService.getId().toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].subscribedOn").value(hasItem(DEFAULT_SUBSCRIBED_ON.toString())))
            .andExpect(jsonPath("$.[*].terminatedOn").value(hasItem(DEFAULT_TERMINATED_ON.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSubscribedServicesWithEagerRelationshipsIsEnabled() throws Exception {
        when(subscribedServiceServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSubscribedServiceMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(subscribedServiceServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSubscribedServicesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(subscribedServiceServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSubscribedServiceMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(subscribedServiceRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getSubscribedService() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get the subscribedService
        restSubscribedServiceMockMvc
            .perform(get(ENTITY_API_URL_ID, subscribedService.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(subscribedService.getId().toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.subscribedOn").value(DEFAULT_SUBSCRIBED_ON.toString()))
            .andExpect(jsonPath("$.terminatedOn").value(DEFAULT_TERMINATED_ON.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()));
    }

    @Test
    @Transactional
    void getSubscribedServicesByIdFiltering() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        UUID id = subscribedService.getId();

        defaultSubscribedServiceFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByServiceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where serviceCode equals to
        defaultSubscribedServiceFiltering("serviceCode.equals=" + DEFAULT_SERVICE_CODE, "serviceCode.equals=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByServiceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where serviceCode in
        defaultSubscribedServiceFiltering(
            "serviceCode.in=" + DEFAULT_SERVICE_CODE + "," + UPDATED_SERVICE_CODE,
            "serviceCode.in=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByServiceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where serviceCode is not null
        defaultSubscribedServiceFiltering("serviceCode.specified=true", "serviceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByServiceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where serviceCode contains
        defaultSubscribedServiceFiltering("serviceCode.contains=" + DEFAULT_SERVICE_CODE, "serviceCode.contains=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByServiceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where serviceCode does not contain
        defaultSubscribedServiceFiltering(
            "serviceCode.doesNotContain=" + UPDATED_SERVICE_CODE,
            "serviceCode.doesNotContain=" + DEFAULT_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesBySubscribedOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where subscribedOn equals to
        defaultSubscribedServiceFiltering("subscribedOn.equals=" + DEFAULT_SUBSCRIBED_ON, "subscribedOn.equals=" + UPDATED_SUBSCRIBED_ON);
    }

    @Test
    @Transactional
    void getAllSubscribedServicesBySubscribedOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where subscribedOn in
        defaultSubscribedServiceFiltering(
            "subscribedOn.in=" + DEFAULT_SUBSCRIBED_ON + "," + UPDATED_SUBSCRIBED_ON,
            "subscribedOn.in=" + UPDATED_SUBSCRIBED_ON
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesBySubscribedOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where subscribedOn is not null
        defaultSubscribedServiceFiltering("subscribedOn.specified=true", "subscribedOn.specified=false");
    }

    @Test
    @Transactional
    void getAllSubscribedServicesBySubscribedOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where subscribedOn is greater than or equal to
        defaultSubscribedServiceFiltering(
            "subscribedOn.greaterThanOrEqual=" + DEFAULT_SUBSCRIBED_ON,
            "subscribedOn.greaterThanOrEqual=" + UPDATED_SUBSCRIBED_ON
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesBySubscribedOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where subscribedOn is less than or equal to
        defaultSubscribedServiceFiltering(
            "subscribedOn.lessThanOrEqual=" + DEFAULT_SUBSCRIBED_ON,
            "subscribedOn.lessThanOrEqual=" + SMALLER_SUBSCRIBED_ON
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesBySubscribedOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where subscribedOn is less than
        defaultSubscribedServiceFiltering(
            "subscribedOn.lessThan=" + UPDATED_SUBSCRIBED_ON,
            "subscribedOn.lessThan=" + DEFAULT_SUBSCRIBED_ON
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesBySubscribedOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where subscribedOn is greater than
        defaultSubscribedServiceFiltering(
            "subscribedOn.greaterThan=" + SMALLER_SUBSCRIBED_ON,
            "subscribedOn.greaterThan=" + DEFAULT_SUBSCRIBED_ON
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByTerminatedOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where terminatedOn equals to
        defaultSubscribedServiceFiltering("terminatedOn.equals=" + DEFAULT_TERMINATED_ON, "terminatedOn.equals=" + UPDATED_TERMINATED_ON);
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByTerminatedOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where terminatedOn in
        defaultSubscribedServiceFiltering(
            "terminatedOn.in=" + DEFAULT_TERMINATED_ON + "," + UPDATED_TERMINATED_ON,
            "terminatedOn.in=" + UPDATED_TERMINATED_ON
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByTerminatedOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where terminatedOn is not null
        defaultSubscribedServiceFiltering("terminatedOn.specified=true", "terminatedOn.specified=false");
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByTerminatedOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where terminatedOn is greater than or equal to
        defaultSubscribedServiceFiltering(
            "terminatedOn.greaterThanOrEqual=" + DEFAULT_TERMINATED_ON,
            "terminatedOn.greaterThanOrEqual=" + UPDATED_TERMINATED_ON
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByTerminatedOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where terminatedOn is less than or equal to
        defaultSubscribedServiceFiltering(
            "terminatedOn.lessThanOrEqual=" + DEFAULT_TERMINATED_ON,
            "terminatedOn.lessThanOrEqual=" + SMALLER_TERMINATED_ON
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByTerminatedOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where terminatedOn is less than
        defaultSubscribedServiceFiltering(
            "terminatedOn.lessThan=" + UPDATED_TERMINATED_ON,
            "terminatedOn.lessThan=" + DEFAULT_TERMINATED_ON
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByTerminatedOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where terminatedOn is greater than
        defaultSubscribedServiceFiltering(
            "terminatedOn.greaterThan=" + SMALLER_TERMINATED_ON,
            "terminatedOn.greaterThan=" + DEFAULT_TERMINATED_ON
        );
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where state equals to
        defaultSubscribedServiceFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where state in
        defaultSubscribedServiceFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        // Get all the subscribedServiceList where state is not null
        defaultSubscribedServiceFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllSubscribedServicesByInstitutionIsEqualToSomething() throws Exception {
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            subscribedServiceRepository.saveAndFlush(subscribedService);
            institution = InstitutionResourceIT.createEntity();
        } else {
            institution = TestUtil.findAll(em, Institution.class).get(0);
        }
        em.persist(institution);
        em.flush();
        subscribedService.setInstitution(institution);
        subscribedServiceRepository.saveAndFlush(subscribedService);
        UUID institutionId = institution.getId();
        // Get all the subscribedServiceList where institution equals to institutionId
        defaultSubscribedServiceShouldBeFound("institutionId.equals=" + institutionId);

        // Get all the subscribedServiceList where institution equals to UUID.randomUUID()
        defaultSubscribedServiceShouldNotBeFound("institutionId.equals=" + UUID.randomUUID());
    }

    private void defaultSubscribedServiceFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultSubscribedServiceShouldBeFound(shouldBeFound);
        defaultSubscribedServiceShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultSubscribedServiceShouldBeFound(String filter) throws Exception {
        restSubscribedServiceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(subscribedService.getId().toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].subscribedOn").value(hasItem(DEFAULT_SUBSCRIBED_ON.toString())))
            .andExpect(jsonPath("$.[*].terminatedOn").value(hasItem(DEFAULT_TERMINATED_ON.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())));

        // Check, that the count call also returns 1
        restSubscribedServiceMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultSubscribedServiceShouldNotBeFound(String filter) throws Exception {
        restSubscribedServiceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restSubscribedServiceMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingSubscribedService() throws Exception {
        // Get the subscribedService
        restSubscribedServiceMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSubscribedService() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the subscribedService
        SubscribedService updatedSubscribedService = subscribedServiceRepository.findById(subscribedService.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSubscribedService are not directly saved in db
        em.detach(updatedSubscribedService);
        updatedSubscribedService
            .serviceCode(UPDATED_SERVICE_CODE)
            .subscribedOn(UPDATED_SUBSCRIBED_ON)
            .terminatedOn(UPDATED_TERMINATED_ON)
            .state(UPDATED_STATE);
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(updatedSubscribedService);

        restSubscribedServiceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, subscribedServiceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(subscribedServiceDTO))
            )
            .andExpect(status().isOk());

        // Validate the SubscribedService in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSubscribedServiceToMatchAllProperties(updatedSubscribedService);
    }

    @Test
    @Transactional
    void putNonExistingSubscribedService() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        subscribedService.setId(UUID.randomUUID());

        // Create the SubscribedService
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(subscribedService);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSubscribedServiceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, subscribedServiceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(subscribedServiceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SubscribedService in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSubscribedService() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        subscribedService.setId(UUID.randomUUID());

        // Create the SubscribedService
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(subscribedService);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSubscribedServiceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(subscribedServiceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SubscribedService in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSubscribedService() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        subscribedService.setId(UUID.randomUUID());

        // Create the SubscribedService
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(subscribedService);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSubscribedServiceMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(subscribedServiceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SubscribedService in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSubscribedServiceWithPatch() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the subscribedService using partial update
        SubscribedService partialUpdatedSubscribedService = new SubscribedService();
        partialUpdatedSubscribedService.setId(subscribedService.getId());

        partialUpdatedSubscribedService.state(UPDATED_STATE);

        restSubscribedServiceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSubscribedService.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSubscribedService))
            )
            .andExpect(status().isOk());

        // Validate the SubscribedService in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSubscribedServiceUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedSubscribedService, subscribedService),
            getPersistedSubscribedService(subscribedService)
        );
    }

    @Test
    @Transactional
    void fullUpdateSubscribedServiceWithPatch() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the subscribedService using partial update
        SubscribedService partialUpdatedSubscribedService = new SubscribedService();
        partialUpdatedSubscribedService.setId(subscribedService.getId());

        partialUpdatedSubscribedService
            .serviceCode(UPDATED_SERVICE_CODE)
            .subscribedOn(UPDATED_SUBSCRIBED_ON)
            .terminatedOn(UPDATED_TERMINATED_ON)
            .state(UPDATED_STATE);

        restSubscribedServiceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSubscribedService.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSubscribedService))
            )
            .andExpect(status().isOk());

        // Validate the SubscribedService in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSubscribedServiceUpdatableFieldsEquals(
            partialUpdatedSubscribedService,
            getPersistedSubscribedService(partialUpdatedSubscribedService)
        );
    }

    @Test
    @Transactional
    void patchNonExistingSubscribedService() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        subscribedService.setId(UUID.randomUUID());

        // Create the SubscribedService
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(subscribedService);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSubscribedServiceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, subscribedServiceDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(subscribedServiceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SubscribedService in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSubscribedService() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        subscribedService.setId(UUID.randomUUID());

        // Create the SubscribedService
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(subscribedService);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSubscribedServiceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(subscribedServiceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SubscribedService in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSubscribedService() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        subscribedService.setId(UUID.randomUUID());

        // Create the SubscribedService
        SubscribedServiceDTO subscribedServiceDTO = subscribedServiceMapper.toDto(subscribedService);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSubscribedServiceMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(subscribedServiceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SubscribedService in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSubscribedService() throws Exception {
        // Initialize the database
        insertedSubscribedService = subscribedServiceRepository.saveAndFlush(subscribedService);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the subscribedService
        restSubscribedServiceMockMvc
            .perform(delete(ENTITY_API_URL_ID, subscribedService.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return subscribedServiceRepository.count();
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

    protected SubscribedService getPersistedSubscribedService(SubscribedService subscribedService) {
        return subscribedServiceRepository.findById(subscribedService.getId()).orElseThrow();
    }

    protected void assertPersistedSubscribedServiceToMatchAllProperties(SubscribedService expectedSubscribedService) {
        assertSubscribedServiceAllPropertiesEquals(expectedSubscribedService, getPersistedSubscribedService(expectedSubscribedService));
    }

    protected void assertPersistedSubscribedServiceToMatchUpdatableProperties(SubscribedService expectedSubscribedService) {
        assertSubscribedServiceAllUpdatablePropertiesEquals(
            expectedSubscribedService,
            getPersistedSubscribedService(expectedSubscribedService)
        );
    }
}
