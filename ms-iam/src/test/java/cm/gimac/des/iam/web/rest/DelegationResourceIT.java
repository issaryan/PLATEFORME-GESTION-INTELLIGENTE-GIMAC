package cm.gimac.des.iam.web.rest;

import static cm.gimac.des.iam.domain.DelegationAsserts.*;
import static cm.gimac.des.iam.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.iam.IntegrationTest;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.Delegation;
import cm.gimac.des.iam.domain.enumeration.DelegationState;
import cm.gimac.des.iam.domain.enumeration.DelegationType;
import cm.gimac.des.iam.domain.enumeration.HabilitationLevel;
import cm.gimac.des.iam.repository.DelegationRepository;
import cm.gimac.des.iam.service.DelegationService;
import cm.gimac.des.iam.service.dto.DelegationDTO;
import cm.gimac.des.iam.service.mapper.DelegationMapper;
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
 * Integration tests for the {@link DelegationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.iam.security.AuthoritiesConstants.ADMIN)
class DelegationResourceIT {

    private static final DelegationType DEFAULT_TYPE = DelegationType.ELEVATION_JIT;
    private static final DelegationType UPDATED_TYPE = DelegationType.SUPPLEANCE;

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final HabilitationLevel DEFAULT_DELEGATED_LEVEL = HabilitationLevel.CONSULTATION;
    private static final HabilitationLevel UPDATED_DELEGATED_LEVEL = HabilitationLevel.EXECUTION;

    private static final UUID DEFAULT_REQUESTER_ID = UUID.randomUUID();
    private static final UUID UPDATED_REQUESTER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_APPROVER_ID = UUID.randomUUID();
    private static final UUID UPDATED_APPROVER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_STARTS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTS_AT = Instant.ofEpochMilli(1702849447300L);

    private static final Instant DEFAULT_ENDS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ENDS_AT = Instant.ofEpochMilli(1702849447300L);

    private static final DelegationState DEFAULT_STATE = DelegationState.REQUESTED;
    private static final DelegationState UPDATED_STATE = DelegationState.APPROVED;

    private static final String ENTITY_API_URL = "/api/delegations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DelegationRepository delegationRepository;

    @Mock
    private DelegationRepository delegationRepositoryMock;

    @Autowired
    private DelegationMapper delegationMapper;

    @Mock
    private DelegationService delegationServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDelegationMockMvc;

    private Delegation delegation;

    private Delegation insertedDelegation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Delegation createEntity(EntityManager em) {
        Delegation delegation = new Delegation()
            .type(DEFAULT_TYPE)
            .reason(DEFAULT_REASON)
            .delegatedLevel(DEFAULT_DELEGATED_LEVEL)
            .requesterId(DEFAULT_REQUESTER_ID)
            .approverId(DEFAULT_APPROVER_ID)
            .startsAt(DEFAULT_STARTS_AT)
            .endsAt(DEFAULT_ENDS_AT)
            .state(DEFAULT_STATE);
        // Add required entity
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            collaborator = CollaboratorResourceIT.createEntity();
            em.persist(collaborator);
            em.flush();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        delegation.setCollaborator(collaborator);
        return delegation;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Delegation createUpdatedEntity(EntityManager em) {
        Delegation updatedDelegation = new Delegation()
            .type(UPDATED_TYPE)
            .reason(UPDATED_REASON)
            .delegatedLevel(UPDATED_DELEGATED_LEVEL)
            .requesterId(UPDATED_REQUESTER_ID)
            .approverId(UPDATED_APPROVER_ID)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT)
            .state(UPDATED_STATE);
        // Add required entity
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            collaborator = CollaboratorResourceIT.createUpdatedEntity();
            em.persist(collaborator);
            em.flush();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        updatedDelegation.setCollaborator(collaborator);
        return updatedDelegation;
    }

    @BeforeEach
    void initTest() {
        delegation = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedDelegation != null) {
            delegationRepository.delete(insertedDelegation);
            insertedDelegation = null;
        }
    }

    @Test
    @Transactional
    void createDelegation() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Delegation
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);
        var returnedDelegationDTO = om.readValue(
            restDelegationMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(delegationDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            DelegationDTO.class
        );

        // Validate the Delegation in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedDelegation = delegationMapper.toEntity(returnedDelegationDTO);
        assertDelegationUpdatableFieldsEquals(returnedDelegation, getPersistedDelegation(returnedDelegation));

        insertedDelegation = returnedDelegation;
    }

    @Test
    @Transactional
    void createDelegationWithExistingId() throws Exception {
        // Create the Delegation with an existing ID
        insertedDelegation = delegationRepository.saveAndFlush(delegation);
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDelegationMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(delegationDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Delegation in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        delegation.setType(null);

        // Create the Delegation, which fails.
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        restDelegationMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(delegationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReasonIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        delegation.setReason(null);

        // Create the Delegation, which fails.
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        restDelegationMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(delegationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDelegatedLevelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        delegation.setDelegatedLevel(null);

        // Create the Delegation, which fails.
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        restDelegationMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(delegationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRequesterIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        delegation.setRequesterId(null);

        // Create the Delegation, which fails.
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        restDelegationMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(delegationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStartsAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        delegation.setStartsAt(null);

        // Create the Delegation, which fails.
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        restDelegationMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(delegationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEndsAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        delegation.setEndsAt(null);

        // Create the Delegation, which fails.
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        restDelegationMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(delegationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        delegation.setState(null);

        // Create the Delegation, which fails.
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        restDelegationMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(delegationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllDelegations() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList
        restDelegationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(delegation.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].delegatedLevel").value(hasItem(DEFAULT_DELEGATED_LEVEL.toString())))
            .andExpect(jsonPath("$.[*].requesterId").value(hasItem(DEFAULT_REQUESTER_ID.toString())))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].startsAt").value(hasItem(DEFAULT_STARTS_AT.toString())))
            .andExpect(jsonPath("$.[*].endsAt").value(hasItem(DEFAULT_ENDS_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDelegationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(delegationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restDelegationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(delegationServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDelegationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(delegationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restDelegationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(delegationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getDelegation() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get the delegation
        restDelegationMockMvc
            .perform(get(ENTITY_API_URL_ID, delegation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(delegation.getId().toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.delegatedLevel").value(DEFAULT_DELEGATED_LEVEL.toString()))
            .andExpect(jsonPath("$.requesterId").value(DEFAULT_REQUESTER_ID.toString()))
            .andExpect(jsonPath("$.approverId").value(DEFAULT_APPROVER_ID.toString()))
            .andExpect(jsonPath("$.startsAt").value(DEFAULT_STARTS_AT.toString()))
            .andExpect(jsonPath("$.endsAt").value(DEFAULT_ENDS_AT.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()));
    }

    @Test
    @Transactional
    void getDelegationsByIdFiltering() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        UUID id = delegation.getId();

        defaultDelegationFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllDelegationsByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where type equals to
        defaultDelegationFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllDelegationsByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where type in
        defaultDelegationFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllDelegationsByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where type is not null
        defaultDelegationFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllDelegationsByReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where reason equals to
        defaultDelegationFiltering("reason.equals=" + DEFAULT_REASON, "reason.equals=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllDelegationsByReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where reason in
        defaultDelegationFiltering("reason.in=" + DEFAULT_REASON + "," + UPDATED_REASON, "reason.in=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllDelegationsByReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where reason is not null
        defaultDelegationFiltering("reason.specified=true", "reason.specified=false");
    }

    @Test
    @Transactional
    void getAllDelegationsByReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where reason contains
        defaultDelegationFiltering("reason.contains=" + DEFAULT_REASON, "reason.contains=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllDelegationsByReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where reason does not contain
        defaultDelegationFiltering("reason.doesNotContain=" + UPDATED_REASON, "reason.doesNotContain=" + DEFAULT_REASON);
    }

    @Test
    @Transactional
    void getAllDelegationsByDelegatedLevelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where delegatedLevel equals to
        defaultDelegationFiltering("delegatedLevel.equals=" + DEFAULT_DELEGATED_LEVEL, "delegatedLevel.equals=" + UPDATED_DELEGATED_LEVEL);
    }

    @Test
    @Transactional
    void getAllDelegationsByDelegatedLevelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where delegatedLevel in
        defaultDelegationFiltering(
            "delegatedLevel.in=" + DEFAULT_DELEGATED_LEVEL + "," + UPDATED_DELEGATED_LEVEL,
            "delegatedLevel.in=" + UPDATED_DELEGATED_LEVEL
        );
    }

    @Test
    @Transactional
    void getAllDelegationsByDelegatedLevelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where delegatedLevel is not null
        defaultDelegationFiltering("delegatedLevel.specified=true", "delegatedLevel.specified=false");
    }

    @Test
    @Transactional
    void getAllDelegationsByRequesterIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where requesterId equals to
        defaultDelegationFiltering("requesterId.equals=" + DEFAULT_REQUESTER_ID, "requesterId.equals=" + UPDATED_REQUESTER_ID);
    }

    @Test
    @Transactional
    void getAllDelegationsByRequesterIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where requesterId in
        defaultDelegationFiltering(
            "requesterId.in=" + DEFAULT_REQUESTER_ID + "," + UPDATED_REQUESTER_ID,
            "requesterId.in=" + UPDATED_REQUESTER_ID
        );
    }

    @Test
    @Transactional
    void getAllDelegationsByRequesterIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where requesterId is not null
        defaultDelegationFiltering("requesterId.specified=true", "requesterId.specified=false");
    }

    @Test
    @Transactional
    void getAllDelegationsByApproverIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where approverId equals to
        defaultDelegationFiltering("approverId.equals=" + DEFAULT_APPROVER_ID, "approverId.equals=" + UPDATED_APPROVER_ID);
    }

    @Test
    @Transactional
    void getAllDelegationsByApproverIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where approverId in
        defaultDelegationFiltering(
            "approverId.in=" + DEFAULT_APPROVER_ID + "," + UPDATED_APPROVER_ID,
            "approverId.in=" + UPDATED_APPROVER_ID
        );
    }

    @Test
    @Transactional
    void getAllDelegationsByApproverIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where approverId is not null
        defaultDelegationFiltering("approverId.specified=true", "approverId.specified=false");
    }

    @Test
    @Transactional
    void getAllDelegationsByStartsAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where startsAt equals to
        defaultDelegationFiltering("startsAt.equals=" + DEFAULT_STARTS_AT, "startsAt.equals=" + UPDATED_STARTS_AT);
    }

    @Test
    @Transactional
    void getAllDelegationsByStartsAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where startsAt in
        defaultDelegationFiltering("startsAt.in=" + DEFAULT_STARTS_AT + "," + UPDATED_STARTS_AT, "startsAt.in=" + UPDATED_STARTS_AT);
    }

    @Test
    @Transactional
    void getAllDelegationsByStartsAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where startsAt is not null
        defaultDelegationFiltering("startsAt.specified=true", "startsAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDelegationsByEndsAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where endsAt equals to
        defaultDelegationFiltering("endsAt.equals=" + DEFAULT_ENDS_AT, "endsAt.equals=" + UPDATED_ENDS_AT);
    }

    @Test
    @Transactional
    void getAllDelegationsByEndsAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where endsAt in
        defaultDelegationFiltering("endsAt.in=" + DEFAULT_ENDS_AT + "," + UPDATED_ENDS_AT, "endsAt.in=" + UPDATED_ENDS_AT);
    }

    @Test
    @Transactional
    void getAllDelegationsByEndsAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where endsAt is not null
        defaultDelegationFiltering("endsAt.specified=true", "endsAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDelegationsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where state equals to
        defaultDelegationFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllDelegationsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where state in
        defaultDelegationFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllDelegationsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        // Get all the delegationList where state is not null
        defaultDelegationFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllDelegationsByCollaboratorIsEqualToSomething() throws Exception {
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            delegationRepository.saveAndFlush(delegation);
            collaborator = CollaboratorResourceIT.createEntity();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        em.persist(collaborator);
        em.flush();
        delegation.setCollaborator(collaborator);
        delegationRepository.saveAndFlush(delegation);
        UUID collaboratorId = collaborator.getId();
        // Get all the delegationList where collaborator equals to collaboratorId
        defaultDelegationShouldBeFound("collaboratorId.equals=" + collaboratorId);

        // Get all the delegationList where collaborator equals to UUID.randomUUID()
        defaultDelegationShouldNotBeFound("collaboratorId.equals=" + UUID.randomUUID());
    }

    private void defaultDelegationFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultDelegationShouldBeFound(shouldBeFound);
        defaultDelegationShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultDelegationShouldBeFound(String filter) throws Exception {
        restDelegationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(delegation.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].delegatedLevel").value(hasItem(DEFAULT_DELEGATED_LEVEL.toString())))
            .andExpect(jsonPath("$.[*].requesterId").value(hasItem(DEFAULT_REQUESTER_ID.toString())))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].startsAt").value(hasItem(DEFAULT_STARTS_AT.toString())))
            .andExpect(jsonPath("$.[*].endsAt").value(hasItem(DEFAULT_ENDS_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())));

        // Check, that the count call also returns 1
        restDelegationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultDelegationShouldNotBeFound(String filter) throws Exception {
        restDelegationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restDelegationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingDelegation() throws Exception {
        // Get the delegation
        restDelegationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDelegation() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the delegation
        Delegation updatedDelegation = delegationRepository.findById(delegation.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedDelegation are not directly saved in db
        em.detach(updatedDelegation);
        updatedDelegation
            .type(UPDATED_TYPE)
            .reason(UPDATED_REASON)
            .delegatedLevel(UPDATED_DELEGATED_LEVEL)
            .requesterId(UPDATED_REQUESTER_ID)
            .approverId(UPDATED_APPROVER_ID)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT)
            .state(UPDATED_STATE);
        DelegationDTO delegationDTO = delegationMapper.toDto(updatedDelegation);

        restDelegationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, delegationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(delegationDTO))
            )
            .andExpect(status().isOk());

        // Validate the Delegation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDelegationToMatchAllProperties(updatedDelegation);
    }

    @Test
    @Transactional
    void putNonExistingDelegation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        delegation.setId(UUID.randomUUID());

        // Create the Delegation
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDelegationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, delegationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(delegationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Delegation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDelegation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        delegation.setId(UUID.randomUUID());

        // Create the Delegation
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDelegationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(delegationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Delegation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDelegation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        delegation.setId(UUID.randomUUID());

        // Create the Delegation
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDelegationMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(delegationDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Delegation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDelegationWithPatch() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the delegation using partial update
        Delegation partialUpdatedDelegation = new Delegation();
        partialUpdatedDelegation.setId(delegation.getId());

        partialUpdatedDelegation
            .requesterId(UPDATED_REQUESTER_ID)
            .approverId(UPDATED_APPROVER_ID)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT)
            .state(UPDATED_STATE);

        restDelegationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDelegation.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDelegation))
            )
            .andExpect(status().isOk());

        // Validate the Delegation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDelegationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedDelegation, delegation),
            getPersistedDelegation(delegation)
        );
    }

    @Test
    @Transactional
    void fullUpdateDelegationWithPatch() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the delegation using partial update
        Delegation partialUpdatedDelegation = new Delegation();
        partialUpdatedDelegation.setId(delegation.getId());

        partialUpdatedDelegation
            .type(UPDATED_TYPE)
            .reason(UPDATED_REASON)
            .delegatedLevel(UPDATED_DELEGATED_LEVEL)
            .requesterId(UPDATED_REQUESTER_ID)
            .approverId(UPDATED_APPROVER_ID)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT)
            .state(UPDATED_STATE);

        restDelegationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDelegation.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDelegation))
            )
            .andExpect(status().isOk());

        // Validate the Delegation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDelegationUpdatableFieldsEquals(partialUpdatedDelegation, getPersistedDelegation(partialUpdatedDelegation));
    }

    @Test
    @Transactional
    void patchNonExistingDelegation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        delegation.setId(UUID.randomUUID());

        // Create the Delegation
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDelegationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, delegationDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(delegationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Delegation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDelegation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        delegation.setId(UUID.randomUUID());

        // Create the Delegation
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDelegationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(delegationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Delegation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDelegation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        delegation.setId(UUID.randomUUID());

        // Create the Delegation
        DelegationDTO delegationDTO = delegationMapper.toDto(delegation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDelegationMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(delegationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Delegation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDelegation() throws Exception {
        // Initialize the database
        insertedDelegation = delegationRepository.saveAndFlush(delegation);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the delegation
        restDelegationMockMvc
            .perform(delete(ENTITY_API_URL_ID, delegation.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return delegationRepository.count();
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

    protected Delegation getPersistedDelegation(Delegation delegation) {
        return delegationRepository.findById(delegation.getId()).orElseThrow();
    }

    protected void assertPersistedDelegationToMatchAllProperties(Delegation expectedDelegation) {
        assertDelegationAllPropertiesEquals(expectedDelegation, getPersistedDelegation(expectedDelegation));
    }

    protected void assertPersistedDelegationToMatchUpdatableProperties(Delegation expectedDelegation) {
        assertDelegationAllUpdatablePropertiesEquals(expectedDelegation, getPersistedDelegation(expectedDelegation));
    }
}
