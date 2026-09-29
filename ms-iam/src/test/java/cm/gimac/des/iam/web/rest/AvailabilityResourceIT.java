package cm.gimac.des.iam.web.rest;

import static cm.gimac.des.iam.domain.AvailabilityAsserts.*;
import static cm.gimac.des.iam.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.iam.IntegrationTest;
import cm.gimac.des.iam.domain.Availability;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.enumeration.AvailabilityType;
import cm.gimac.des.iam.domain.enumeration.ValidationState;
import cm.gimac.des.iam.repository.AvailabilityRepository;
import cm.gimac.des.iam.service.AvailabilityService;
import cm.gimac.des.iam.service.dto.AvailabilityDTO;
import cm.gimac.des.iam.service.mapper.AvailabilityMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link AvailabilityResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.iam.security.AuthoritiesConstants.ADMIN)
class AvailabilityResourceIT {

    private static final AvailabilityType DEFAULT_TYPE = AvailabilityType.CONGE;
    private static final AvailabilityType UPDATED_TYPE = AvailabilityType.ABSENCE;

    private static final Instant DEFAULT_STARTS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTS_AT = Instant.ofEpochMilli(1702849447300L);

    private static final Instant DEFAULT_ENDS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ENDS_AT = Instant.ofEpochMilli(1702849447300L);

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final UUID DEFAULT_JUSTIFICATION_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_JUSTIFICATION_DOCUMENT_ID = UUID.randomUUID();

    private static final ValidationState DEFAULT_VALIDATION_STATE = ValidationState.DECLAREE;
    private static final ValidationState UPDATED_VALIDATION_STATE = ValidationState.VALIDEE;

    private static final UUID DEFAULT_VALIDATOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_VALIDATOR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_VALIDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_VALIDATED_AT = Instant.ofEpochMilli(1702849447300L);

    private static final String ENTITY_API_URL = "/api/availabilities";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Mock
    private AvailabilityRepository availabilityRepositoryMock;

    @Autowired
    private AvailabilityMapper availabilityMapper;

    @Mock
    private AvailabilityService availabilityServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAvailabilityMockMvc;

    private Availability availability;

    private Availability insertedAvailability;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Availability createEntity(EntityManager em) {
        Availability availability = new Availability()
            .type(DEFAULT_TYPE)
            .startsAt(DEFAULT_STARTS_AT)
            .endsAt(DEFAULT_ENDS_AT)
            .reason(DEFAULT_REASON)
            .justificationDocumentId(DEFAULT_JUSTIFICATION_DOCUMENT_ID)
            .validationState(DEFAULT_VALIDATION_STATE)
            .validatorId(DEFAULT_VALIDATOR_ID)
            .validatedAt(DEFAULT_VALIDATED_AT);
        // Add required entity
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            collaborator = CollaboratorResourceIT.createEntity();
            em.persist(collaborator);
            em.flush();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        availability.setCollaborator(collaborator);
        return availability;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Availability createUpdatedEntity(EntityManager em) {
        Availability updatedAvailability = new Availability()
            .type(UPDATED_TYPE)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT)
            .reason(UPDATED_REASON)
            .justificationDocumentId(UPDATED_JUSTIFICATION_DOCUMENT_ID)
            .validationState(UPDATED_VALIDATION_STATE)
            .validatorId(UPDATED_VALIDATOR_ID)
            .validatedAt(UPDATED_VALIDATED_AT);
        // Add required entity
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            collaborator = CollaboratorResourceIT.createUpdatedEntity();
            em.persist(collaborator);
            em.flush();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        updatedAvailability.setCollaborator(collaborator);
        return updatedAvailability;
    }

    @BeforeEach
    void initTest() {
        availability = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAvailability != null) {
            availabilityRepository.delete(insertedAvailability);
            insertedAvailability = null;
        }
    }

    @Test
    @Transactional
    void createAvailability() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Availability
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);
        var returnedAvailabilityDTO = om.readValue(
            restAvailabilityMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(availabilityDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AvailabilityDTO.class
        );

        // Validate the Availability in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAvailability = availabilityMapper.toEntity(returnedAvailabilityDTO);
        assertAvailabilityUpdatableFieldsEquals(returnedAvailability, getPersistedAvailability(returnedAvailability));

        insertedAvailability = returnedAvailability;
    }

    @Test
    @Transactional
    void createAvailabilityWithExistingId() throws Exception {
        // Create the Availability with an existing ID
        insertedAvailability = availabilityRepository.saveAndFlush(availability);
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAvailabilityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Availability in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        availability.setType(null);

        // Create the Availability, which fails.
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);

        restAvailabilityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStartsAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        availability.setStartsAt(null);

        // Create the Availability, which fails.
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);

        restAvailabilityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEndsAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        availability.setEndsAt(null);

        // Create the Availability, which fails.
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);

        restAvailabilityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkValidationStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        availability.setValidationState(null);

        // Create the Availability, which fails.
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);

        restAvailabilityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAvailabilities() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList
        restAvailabilityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(availability.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].startsAt").value(hasItem(DEFAULT_STARTS_AT.toString())))
            .andExpect(jsonPath("$.[*].endsAt").value(hasItem(DEFAULT_ENDS_AT.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].justificationDocumentId").value(hasItem(DEFAULT_JUSTIFICATION_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].validationState").value(hasItem(DEFAULT_VALIDATION_STATE.toString())))
            .andExpect(jsonPath("$.[*].validatorId").value(hasItem(DEFAULT_VALIDATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].validatedAt").value(hasItem(DEFAULT_VALIDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAvailabilitiesWithEagerRelationshipsIsEnabled() throws Exception {
        when(availabilityServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAvailabilityMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(availabilityServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAvailabilitiesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(availabilityServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAvailabilityMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(availabilityRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getAvailability() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get the availability
        restAvailabilityMockMvc
            .perform(get(ENTITY_API_URL_ID, availability.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(availability.getId().toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.startsAt").value(DEFAULT_STARTS_AT.toString()))
            .andExpect(jsonPath("$.endsAt").value(DEFAULT_ENDS_AT.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.justificationDocumentId").value(DEFAULT_JUSTIFICATION_DOCUMENT_ID.toString()))
            .andExpect(jsonPath("$.validationState").value(DEFAULT_VALIDATION_STATE.toString()))
            .andExpect(jsonPath("$.validatorId").value(DEFAULT_VALIDATOR_ID.toString()))
            .andExpect(jsonPath("$.validatedAt").value(DEFAULT_VALIDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getAvailabilitiesByIdFiltering() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        UUID id = availability.getId();

        defaultAvailabilityFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where type equals to
        defaultAvailabilityFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where type in
        defaultAvailabilityFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where type is not null
        defaultAvailabilityFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByStartsAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where startsAt equals to
        defaultAvailabilityFiltering("startsAt.equals=" + DEFAULT_STARTS_AT, "startsAt.equals=" + UPDATED_STARTS_AT);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByStartsAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where startsAt in
        defaultAvailabilityFiltering("startsAt.in=" + DEFAULT_STARTS_AT + "," + UPDATED_STARTS_AT, "startsAt.in=" + UPDATED_STARTS_AT);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByStartsAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where startsAt is not null
        defaultAvailabilityFiltering("startsAt.specified=true", "startsAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByEndsAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where endsAt equals to
        defaultAvailabilityFiltering("endsAt.equals=" + DEFAULT_ENDS_AT, "endsAt.equals=" + UPDATED_ENDS_AT);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByEndsAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where endsAt in
        defaultAvailabilityFiltering("endsAt.in=" + DEFAULT_ENDS_AT + "," + UPDATED_ENDS_AT, "endsAt.in=" + UPDATED_ENDS_AT);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByEndsAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where endsAt is not null
        defaultAvailabilityFiltering("endsAt.specified=true", "endsAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where reason equals to
        defaultAvailabilityFiltering("reason.equals=" + DEFAULT_REASON, "reason.equals=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where reason in
        defaultAvailabilityFiltering("reason.in=" + DEFAULT_REASON + "," + UPDATED_REASON, "reason.in=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where reason is not null
        defaultAvailabilityFiltering("reason.specified=true", "reason.specified=false");
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where reason contains
        defaultAvailabilityFiltering("reason.contains=" + DEFAULT_REASON, "reason.contains=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where reason does not contain
        defaultAvailabilityFiltering("reason.doesNotContain=" + UPDATED_REASON, "reason.doesNotContain=" + DEFAULT_REASON);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByJustificationDocumentIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where justificationDocumentId equals to
        defaultAvailabilityFiltering(
            "justificationDocumentId.equals=" + DEFAULT_JUSTIFICATION_DOCUMENT_ID,
            "justificationDocumentId.equals=" + UPDATED_JUSTIFICATION_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByJustificationDocumentIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where justificationDocumentId in
        defaultAvailabilityFiltering(
            "justificationDocumentId.in=" + DEFAULT_JUSTIFICATION_DOCUMENT_ID + "," + UPDATED_JUSTIFICATION_DOCUMENT_ID,
            "justificationDocumentId.in=" + UPDATED_JUSTIFICATION_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByJustificationDocumentIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where justificationDocumentId is not null
        defaultAvailabilityFiltering("justificationDocumentId.specified=true", "justificationDocumentId.specified=false");
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByValidationStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where validationState equals to
        defaultAvailabilityFiltering(
            "validationState.equals=" + DEFAULT_VALIDATION_STATE,
            "validationState.equals=" + UPDATED_VALIDATION_STATE
        );
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByValidationStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where validationState in
        defaultAvailabilityFiltering(
            "validationState.in=" + DEFAULT_VALIDATION_STATE + "," + UPDATED_VALIDATION_STATE,
            "validationState.in=" + UPDATED_VALIDATION_STATE
        );
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByValidationStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where validationState is not null
        defaultAvailabilityFiltering("validationState.specified=true", "validationState.specified=false");
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByValidatorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where validatorId equals to
        defaultAvailabilityFiltering("validatorId.equals=" + DEFAULT_VALIDATOR_ID, "validatorId.equals=" + UPDATED_VALIDATOR_ID);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByValidatorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where validatorId in
        defaultAvailabilityFiltering(
            "validatorId.in=" + DEFAULT_VALIDATOR_ID + "," + UPDATED_VALIDATOR_ID,
            "validatorId.in=" + UPDATED_VALIDATOR_ID
        );
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByValidatorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where validatorId is not null
        defaultAvailabilityFiltering("validatorId.specified=true", "validatorId.specified=false");
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByValidatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where validatedAt equals to
        defaultAvailabilityFiltering("validatedAt.equals=" + DEFAULT_VALIDATED_AT, "validatedAt.equals=" + UPDATED_VALIDATED_AT);
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByValidatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where validatedAt in
        defaultAvailabilityFiltering(
            "validatedAt.in=" + DEFAULT_VALIDATED_AT + "," + UPDATED_VALIDATED_AT,
            "validatedAt.in=" + UPDATED_VALIDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByValidatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        // Get all the availabilityList where validatedAt is not null
        defaultAvailabilityFiltering("validatedAt.specified=true", "validatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAvailabilitiesByCollaboratorIsEqualToSomething() throws Exception {
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            availabilityRepository.saveAndFlush(availability);
            collaborator = CollaboratorResourceIT.createEntity();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        em.persist(collaborator);
        em.flush();
        availability.setCollaborator(collaborator);
        availabilityRepository.saveAndFlush(availability);
        UUID collaboratorId = collaborator.getId();
        // Get all the availabilityList where collaborator equals to collaboratorId
        defaultAvailabilityShouldBeFound("collaboratorId.equals=" + collaboratorId);

        // Get all the availabilityList where collaborator equals to UUID.randomUUID()
        defaultAvailabilityShouldNotBeFound("collaboratorId.equals=" + UUID.randomUUID());
    }

    private void defaultAvailabilityFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultAvailabilityShouldBeFound(shouldBeFound);
        defaultAvailabilityShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultAvailabilityShouldBeFound(String filter) throws Exception {
        restAvailabilityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(availability.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].startsAt").value(hasItem(DEFAULT_STARTS_AT.toString())))
            .andExpect(jsonPath("$.[*].endsAt").value(hasItem(DEFAULT_ENDS_AT.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].justificationDocumentId").value(hasItem(DEFAULT_JUSTIFICATION_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].validationState").value(hasItem(DEFAULT_VALIDATION_STATE.toString())))
            .andExpect(jsonPath("$.[*].validatorId").value(hasItem(DEFAULT_VALIDATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].validatedAt").value(hasItem(DEFAULT_VALIDATED_AT.toString())));

        // Check, that the count call also returns 1
        restAvailabilityMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultAvailabilityShouldNotBeFound(String filter) throws Exception {
        restAvailabilityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restAvailabilityMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingAvailability() throws Exception {
        // Get the availability
        restAvailabilityMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAvailability() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the availability
        Availability updatedAvailability = availabilityRepository.findById(availability.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAvailability are not directly saved in db
        em.detach(updatedAvailability);
        updatedAvailability
            .type(UPDATED_TYPE)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT)
            .reason(UPDATED_REASON)
            .justificationDocumentId(UPDATED_JUSTIFICATION_DOCUMENT_ID)
            .validationState(UPDATED_VALIDATION_STATE)
            .validatorId(UPDATED_VALIDATOR_ID)
            .validatedAt(UPDATED_VALIDATED_AT);
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(updatedAvailability);

        restAvailabilityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, availabilityDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isOk());

        // Validate the Availability in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAvailabilityToMatchAllProperties(updatedAvailability);
    }

    @Test
    @Transactional
    void putNonExistingAvailability() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        availability.setId(UUID.randomUUID());

        // Create the Availability
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAvailabilityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, availabilityDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Availability in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAvailability() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        availability.setId(UUID.randomUUID());

        // Create the Availability
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAvailabilityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Availability in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAvailability() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        availability.setId(UUID.randomUUID());

        // Create the Availability
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAvailabilityMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Availability in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAvailabilityWithPatch() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the availability using partial update
        Availability partialUpdatedAvailability = new Availability();
        partialUpdatedAvailability.setId(availability.getId());

        partialUpdatedAvailability
            .type(UPDATED_TYPE)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT)
            .reason(UPDATED_REASON)
            .validatorId(UPDATED_VALIDATOR_ID);

        restAvailabilityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAvailability.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAvailability))
            )
            .andExpect(status().isOk());

        // Validate the Availability in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAvailabilityUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAvailability, availability),
            getPersistedAvailability(availability)
        );
    }

    @Test
    @Transactional
    void fullUpdateAvailabilityWithPatch() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the availability using partial update
        Availability partialUpdatedAvailability = new Availability();
        partialUpdatedAvailability.setId(availability.getId());

        partialUpdatedAvailability
            .type(UPDATED_TYPE)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT)
            .reason(UPDATED_REASON)
            .justificationDocumentId(UPDATED_JUSTIFICATION_DOCUMENT_ID)
            .validationState(UPDATED_VALIDATION_STATE)
            .validatorId(UPDATED_VALIDATOR_ID)
            .validatedAt(UPDATED_VALIDATED_AT);

        restAvailabilityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAvailability.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAvailability))
            )
            .andExpect(status().isOk());

        // Validate the Availability in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAvailabilityUpdatableFieldsEquals(partialUpdatedAvailability, getPersistedAvailability(partialUpdatedAvailability));
    }

    @Test
    @Transactional
    void patchNonExistingAvailability() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        availability.setId(UUID.randomUUID());

        // Create the Availability
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAvailabilityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, availabilityDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Availability in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAvailability() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        availability.setId(UUID.randomUUID());

        // Create the Availability
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAvailabilityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Availability in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAvailability() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        availability.setId(UUID.randomUUID());

        // Create the Availability
        AvailabilityDTO availabilityDTO = availabilityMapper.toDto(availability);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAvailabilityMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(availabilityDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Availability in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAvailability() throws Exception {
        // Initialize the database
        insertedAvailability = availabilityRepository.saveAndFlush(availability);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the availability
        restAvailabilityMockMvc
            .perform(delete(ENTITY_API_URL_ID, availability.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return availabilityRepository.count();
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

    protected Availability getPersistedAvailability(Availability availability) {
        return availabilityRepository.findById(availability.getId()).orElseThrow();
    }

    protected void assertPersistedAvailabilityToMatchAllProperties(Availability expectedAvailability) {
        assertAvailabilityAllPropertiesEquals(expectedAvailability, getPersistedAvailability(expectedAvailability));
    }

    protected void assertPersistedAvailabilityToMatchUpdatableProperties(Availability expectedAvailability) {
        assertAvailabilityAllUpdatablePropertiesEquals(expectedAvailability, getPersistedAvailability(expectedAvailability));
    }
}
