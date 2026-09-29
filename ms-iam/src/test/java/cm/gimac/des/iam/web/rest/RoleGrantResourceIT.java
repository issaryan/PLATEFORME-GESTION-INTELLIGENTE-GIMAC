package cm.gimac.des.iam.web.rest;

import static cm.gimac.des.iam.domain.RoleGrantAsserts.*;
import static cm.gimac.des.iam.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.iam.IntegrationTest;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.Role;
import cm.gimac.des.iam.domain.RoleGrant;
import cm.gimac.des.iam.domain.enumeration.GrantState;
import cm.gimac.des.iam.repository.RoleGrantRepository;
import cm.gimac.des.iam.service.RoleGrantService;
import cm.gimac.des.iam.service.dto.RoleGrantDTO;
import cm.gimac.des.iam.service.mapper.RoleGrantMapper;
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
 * Integration tests for the {@link RoleGrantResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.iam.security.AuthoritiesConstants.ADMIN)
class RoleGrantResourceIT {

    private static final GrantState DEFAULT_STATE = GrantState.PROPOSE;
    private static final GrantState UPDATED_STATE = GrantState.APPROUVE;

    private static final UUID DEFAULT_REQUESTED_BY = UUID.randomUUID();
    private static final UUID UPDATED_REQUESTED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_REQUESTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REQUESTED_AT = Instant.ofEpochMilli(1702849447300L);

    private static final UUID DEFAULT_APPROVED_BY = UUID.randomUUID();
    private static final UUID UPDATED_APPROVED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_APPROVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_APPROVED_AT = Instant.ofEpochMilli(1702849447300L);

    private static final Instant DEFAULT_REVOKED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REVOKED_AT = Instant.ofEpochMilli(1702849447300L);

    private static final String DEFAULT_REVOCATION_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REVOCATION_REASON = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/role-grants";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RoleGrantRepository roleGrantRepository;

    @Mock
    private RoleGrantRepository roleGrantRepositoryMock;

    @Autowired
    private RoleGrantMapper roleGrantMapper;

    @Mock
    private RoleGrantService roleGrantServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRoleGrantMockMvc;

    private RoleGrant roleGrant;

    private RoleGrant insertedRoleGrant;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoleGrant createEntity(EntityManager em) {
        RoleGrant roleGrant = new RoleGrant()
            .state(DEFAULT_STATE)
            .requestedBy(DEFAULT_REQUESTED_BY)
            .requestedAt(DEFAULT_REQUESTED_AT)
            .approvedBy(DEFAULT_APPROVED_BY)
            .approvedAt(DEFAULT_APPROVED_AT)
            .revokedAt(DEFAULT_REVOKED_AT)
            .revocationReason(DEFAULT_REVOCATION_REASON);
        // Add required entity
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            collaborator = CollaboratorResourceIT.createEntity();
            em.persist(collaborator);
            em.flush();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        roleGrant.setCollaborator(collaborator);
        // Add required entity
        Role role;
        if (TestUtil.findAll(em, Role.class).isEmpty()) {
            role = RoleResourceIT.createEntity();
            em.persist(role);
            em.flush();
        } else {
            role = TestUtil.findAll(em, Role.class).getFirst();
        }
        roleGrant.setRole(role);
        return roleGrant;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoleGrant createUpdatedEntity(EntityManager em) {
        RoleGrant updatedRoleGrant = new RoleGrant()
            .state(UPDATED_STATE)
            .requestedBy(UPDATED_REQUESTED_BY)
            .requestedAt(UPDATED_REQUESTED_AT)
            .approvedBy(UPDATED_APPROVED_BY)
            .approvedAt(UPDATED_APPROVED_AT)
            .revokedAt(UPDATED_REVOKED_AT)
            .revocationReason(UPDATED_REVOCATION_REASON);
        // Add required entity
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            collaborator = CollaboratorResourceIT.createUpdatedEntity();
            em.persist(collaborator);
            em.flush();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        updatedRoleGrant.setCollaborator(collaborator);
        // Add required entity
        Role role;
        if (TestUtil.findAll(em, Role.class).isEmpty()) {
            role = RoleResourceIT.createUpdatedEntity();
            em.persist(role);
            em.flush();
        } else {
            role = TestUtil.findAll(em, Role.class).getFirst();
        }
        updatedRoleGrant.setRole(role);
        return updatedRoleGrant;
    }

    @BeforeEach
    void initTest() {
        roleGrant = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedRoleGrant != null) {
            roleGrantRepository.delete(insertedRoleGrant);
            insertedRoleGrant = null;
        }
    }

    @Test
    @Transactional
    void createRoleGrant() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the RoleGrant
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(roleGrant);
        var returnedRoleGrantDTO = om.readValue(
            restRoleGrantMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roleGrantDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RoleGrantDTO.class
        );

        // Validate the RoleGrant in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRoleGrant = roleGrantMapper.toEntity(returnedRoleGrantDTO);
        assertRoleGrantUpdatableFieldsEquals(returnedRoleGrant, getPersistedRoleGrant(returnedRoleGrant));

        insertedRoleGrant = returnedRoleGrant;
    }

    @Test
    @Transactional
    void createRoleGrantWithExistingId() throws Exception {
        // Create the RoleGrant with an existing ID
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(roleGrant);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRoleGrantMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roleGrantDTO)))
            .andExpect(status().isBadRequest());

        // Validate the RoleGrant in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roleGrant.setState(null);

        // Create the RoleGrant, which fails.
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(roleGrant);

        restRoleGrantMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roleGrantDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRequestedByIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roleGrant.setRequestedBy(null);

        // Create the RoleGrant, which fails.
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(roleGrant);

        restRoleGrantMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roleGrantDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRequestedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roleGrant.setRequestedAt(null);

        // Create the RoleGrant, which fails.
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(roleGrant);

        restRoleGrantMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roleGrantDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRoleGrants() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList
        restRoleGrantMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(roleGrant.getId().toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].requestedBy").value(hasItem(DEFAULT_REQUESTED_BY.toString())))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].approvedBy").value(hasItem(DEFAULT_APPROVED_BY.toString())))
            .andExpect(jsonPath("$.[*].approvedAt").value(hasItem(DEFAULT_APPROVED_AT.toString())))
            .andExpect(jsonPath("$.[*].revokedAt").value(hasItem(DEFAULT_REVOKED_AT.toString())))
            .andExpect(jsonPath("$.[*].revocationReason").value(hasItem(DEFAULT_REVOCATION_REASON)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoleGrantsWithEagerRelationshipsIsEnabled() throws Exception {
        when(roleGrantServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoleGrantMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(roleGrantServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoleGrantsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(roleGrantServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoleGrantMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(roleGrantRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRoleGrant() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get the roleGrant
        restRoleGrantMockMvc
            .perform(get(ENTITY_API_URL_ID, roleGrant.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(roleGrant.getId().toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.requestedBy").value(DEFAULT_REQUESTED_BY.toString()))
            .andExpect(jsonPath("$.requestedAt").value(DEFAULT_REQUESTED_AT.toString()))
            .andExpect(jsonPath("$.approvedBy").value(DEFAULT_APPROVED_BY.toString()))
            .andExpect(jsonPath("$.approvedAt").value(DEFAULT_APPROVED_AT.toString()))
            .andExpect(jsonPath("$.revokedAt").value(DEFAULT_REVOKED_AT.toString()))
            .andExpect(jsonPath("$.revocationReason").value(DEFAULT_REVOCATION_REASON));
    }

    @Test
    @Transactional
    void getRoleGrantsByIdFiltering() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        UUID id = roleGrant.getId();

        defaultRoleGrantFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllRoleGrantsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where state equals to
        defaultRoleGrantFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllRoleGrantsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where state in
        defaultRoleGrantFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllRoleGrantsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where state is not null
        defaultRoleGrantFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRequestedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where requestedBy equals to
        defaultRoleGrantFiltering("requestedBy.equals=" + DEFAULT_REQUESTED_BY, "requestedBy.equals=" + UPDATED_REQUESTED_BY);
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRequestedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where requestedBy in
        defaultRoleGrantFiltering(
            "requestedBy.in=" + DEFAULT_REQUESTED_BY + "," + UPDATED_REQUESTED_BY,
            "requestedBy.in=" + UPDATED_REQUESTED_BY
        );
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRequestedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where requestedBy is not null
        defaultRoleGrantFiltering("requestedBy.specified=true", "requestedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRequestedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where requestedAt equals to
        defaultRoleGrantFiltering("requestedAt.equals=" + DEFAULT_REQUESTED_AT, "requestedAt.equals=" + UPDATED_REQUESTED_AT);
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRequestedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where requestedAt in
        defaultRoleGrantFiltering(
            "requestedAt.in=" + DEFAULT_REQUESTED_AT + "," + UPDATED_REQUESTED_AT,
            "requestedAt.in=" + UPDATED_REQUESTED_AT
        );
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRequestedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where requestedAt is not null
        defaultRoleGrantFiltering("requestedAt.specified=true", "requestedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoleGrantsByApprovedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where approvedBy equals to
        defaultRoleGrantFiltering("approvedBy.equals=" + DEFAULT_APPROVED_BY, "approvedBy.equals=" + UPDATED_APPROVED_BY);
    }

    @Test
    @Transactional
    void getAllRoleGrantsByApprovedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where approvedBy in
        defaultRoleGrantFiltering(
            "approvedBy.in=" + DEFAULT_APPROVED_BY + "," + UPDATED_APPROVED_BY,
            "approvedBy.in=" + UPDATED_APPROVED_BY
        );
    }

    @Test
    @Transactional
    void getAllRoleGrantsByApprovedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where approvedBy is not null
        defaultRoleGrantFiltering("approvedBy.specified=true", "approvedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllRoleGrantsByApprovedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where approvedAt equals to
        defaultRoleGrantFiltering("approvedAt.equals=" + DEFAULT_APPROVED_AT, "approvedAt.equals=" + UPDATED_APPROVED_AT);
    }

    @Test
    @Transactional
    void getAllRoleGrantsByApprovedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where approvedAt in
        defaultRoleGrantFiltering(
            "approvedAt.in=" + DEFAULT_APPROVED_AT + "," + UPDATED_APPROVED_AT,
            "approvedAt.in=" + UPDATED_APPROVED_AT
        );
    }

    @Test
    @Transactional
    void getAllRoleGrantsByApprovedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where approvedAt is not null
        defaultRoleGrantFiltering("approvedAt.specified=true", "approvedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRevokedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where revokedAt equals to
        defaultRoleGrantFiltering("revokedAt.equals=" + DEFAULT_REVOKED_AT, "revokedAt.equals=" + UPDATED_REVOKED_AT);
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRevokedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where revokedAt in
        defaultRoleGrantFiltering("revokedAt.in=" + DEFAULT_REVOKED_AT + "," + UPDATED_REVOKED_AT, "revokedAt.in=" + UPDATED_REVOKED_AT);
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRevokedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where revokedAt is not null
        defaultRoleGrantFiltering("revokedAt.specified=true", "revokedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRevocationReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where revocationReason equals to
        defaultRoleGrantFiltering(
            "revocationReason.equals=" + DEFAULT_REVOCATION_REASON,
            "revocationReason.equals=" + UPDATED_REVOCATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRevocationReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where revocationReason in
        defaultRoleGrantFiltering(
            "revocationReason.in=" + DEFAULT_REVOCATION_REASON + "," + UPDATED_REVOCATION_REASON,
            "revocationReason.in=" + UPDATED_REVOCATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRevocationReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where revocationReason is not null
        defaultRoleGrantFiltering("revocationReason.specified=true", "revocationReason.specified=false");
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRevocationReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where revocationReason contains
        defaultRoleGrantFiltering(
            "revocationReason.contains=" + DEFAULT_REVOCATION_REASON,
            "revocationReason.contains=" + UPDATED_REVOCATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRevocationReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        // Get all the roleGrantList where revocationReason does not contain
        defaultRoleGrantFiltering(
            "revocationReason.doesNotContain=" + UPDATED_REVOCATION_REASON,
            "revocationReason.doesNotContain=" + DEFAULT_REVOCATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllRoleGrantsByCollaboratorIsEqualToSomething() throws Exception {
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            roleGrantRepository.saveAndFlush(roleGrant);
            collaborator = CollaboratorResourceIT.createEntity();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        em.persist(collaborator);
        em.flush();
        roleGrant.setCollaborator(collaborator);
        roleGrantRepository.saveAndFlush(roleGrant);
        UUID collaboratorId = collaborator.getId();
        // Get all the roleGrantList where collaborator equals to collaboratorId
        defaultRoleGrantShouldBeFound("collaboratorId.equals=" + collaboratorId);

        // Get all the roleGrantList where collaborator equals to UUID.randomUUID()
        defaultRoleGrantShouldNotBeFound("collaboratorId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllRoleGrantsByRoleIsEqualToSomething() throws Exception {
        Role role;
        if (TestUtil.findAll(em, Role.class).isEmpty()) {
            roleGrantRepository.saveAndFlush(roleGrant);
            role = RoleResourceIT.createEntity();
        } else {
            role = TestUtil.findAll(em, Role.class).getFirst();
        }
        em.persist(role);
        em.flush();
        roleGrant.setRole(role);
        roleGrantRepository.saveAndFlush(roleGrant);
        UUID roleId = role.getId();
        // Get all the roleGrantList where role equals to roleId
        defaultRoleGrantShouldBeFound("roleId.equals=" + roleId);

        // Get all the roleGrantList where role equals to UUID.randomUUID()
        defaultRoleGrantShouldNotBeFound("roleId.equals=" + UUID.randomUUID());
    }

    private void defaultRoleGrantFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultRoleGrantShouldBeFound(shouldBeFound);
        defaultRoleGrantShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultRoleGrantShouldBeFound(String filter) throws Exception {
        restRoleGrantMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(roleGrant.getId().toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].requestedBy").value(hasItem(DEFAULT_REQUESTED_BY.toString())))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].approvedBy").value(hasItem(DEFAULT_APPROVED_BY.toString())))
            .andExpect(jsonPath("$.[*].approvedAt").value(hasItem(DEFAULT_APPROVED_AT.toString())))
            .andExpect(jsonPath("$.[*].revokedAt").value(hasItem(DEFAULT_REVOKED_AT.toString())))
            .andExpect(jsonPath("$.[*].revocationReason").value(hasItem(DEFAULT_REVOCATION_REASON)));

        // Check, that the count call also returns 1
        restRoleGrantMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultRoleGrantShouldNotBeFound(String filter) throws Exception {
        restRoleGrantMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restRoleGrantMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingRoleGrant() throws Exception {
        // Get the roleGrant
        restRoleGrantMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRoleGrant() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roleGrant
        RoleGrant updatedRoleGrant = roleGrantRepository.findById(roleGrant.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRoleGrant are not directly saved in db
        em.detach(updatedRoleGrant);
        updatedRoleGrant
            .state(UPDATED_STATE)
            .requestedBy(UPDATED_REQUESTED_BY)
            .requestedAt(UPDATED_REQUESTED_AT)
            .approvedBy(UPDATED_APPROVED_BY)
            .approvedAt(UPDATED_APPROVED_AT)
            .revokedAt(UPDATED_REVOKED_AT)
            .revocationReason(UPDATED_REVOCATION_REASON);
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(updatedRoleGrant);

        restRoleGrantMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roleGrantDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roleGrantDTO))
            )
            .andExpect(status().isOk());

        // Validate the RoleGrant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRoleGrantToMatchAllProperties(updatedRoleGrant);
    }

    @Test
    @Transactional
    void putNonExistingRoleGrant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roleGrant.setId(UUID.randomUUID());

        // Create the RoleGrant
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(roleGrant);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoleGrantMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roleGrantDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roleGrantDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoleGrant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRoleGrant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roleGrant.setId(UUID.randomUUID());

        // Create the RoleGrant
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(roleGrant);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoleGrantMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roleGrantDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoleGrant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRoleGrant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roleGrant.setId(UUID.randomUUID());

        // Create the RoleGrant
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(roleGrant);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoleGrantMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roleGrantDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoleGrant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRoleGrantWithPatch() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roleGrant using partial update
        RoleGrant partialUpdatedRoleGrant = new RoleGrant();
        partialUpdatedRoleGrant.setId(roleGrant.getId());

        partialUpdatedRoleGrant.requestedAt(UPDATED_REQUESTED_AT).approvedAt(UPDATED_APPROVED_AT);

        restRoleGrantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoleGrant.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoleGrant))
            )
            .andExpect(status().isOk());

        // Validate the RoleGrant in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoleGrantUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRoleGrant, roleGrant),
            getPersistedRoleGrant(roleGrant)
        );
    }

    @Test
    @Transactional
    void fullUpdateRoleGrantWithPatch() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roleGrant using partial update
        RoleGrant partialUpdatedRoleGrant = new RoleGrant();
        partialUpdatedRoleGrant.setId(roleGrant.getId());

        partialUpdatedRoleGrant
            .state(UPDATED_STATE)
            .requestedBy(UPDATED_REQUESTED_BY)
            .requestedAt(UPDATED_REQUESTED_AT)
            .approvedBy(UPDATED_APPROVED_BY)
            .approvedAt(UPDATED_APPROVED_AT)
            .revokedAt(UPDATED_REVOKED_AT)
            .revocationReason(UPDATED_REVOCATION_REASON);

        restRoleGrantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoleGrant.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoleGrant))
            )
            .andExpect(status().isOk());

        // Validate the RoleGrant in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoleGrantUpdatableFieldsEquals(partialUpdatedRoleGrant, getPersistedRoleGrant(partialUpdatedRoleGrant));
    }

    @Test
    @Transactional
    void patchNonExistingRoleGrant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roleGrant.setId(UUID.randomUUID());

        // Create the RoleGrant
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(roleGrant);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoleGrantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, roleGrantDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roleGrantDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoleGrant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRoleGrant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roleGrant.setId(UUID.randomUUID());

        // Create the RoleGrant
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(roleGrant);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoleGrantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roleGrantDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoleGrant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRoleGrant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roleGrant.setId(UUID.randomUUID());

        // Create the RoleGrant
        RoleGrantDTO roleGrantDTO = roleGrantMapper.toDto(roleGrant);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoleGrantMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(roleGrantDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoleGrant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRoleGrant() throws Exception {
        // Initialize the database
        insertedRoleGrant = roleGrantRepository.saveAndFlush(roleGrant);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the roleGrant
        restRoleGrantMockMvc
            .perform(delete(ENTITY_API_URL_ID, roleGrant.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return roleGrantRepository.count();
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

    protected RoleGrant getPersistedRoleGrant(RoleGrant roleGrant) {
        return roleGrantRepository.findById(roleGrant.getId()).orElseThrow();
    }

    protected void assertPersistedRoleGrantToMatchAllProperties(RoleGrant expectedRoleGrant) {
        assertRoleGrantAllPropertiesEquals(expectedRoleGrant, getPersistedRoleGrant(expectedRoleGrant));
    }

    protected void assertPersistedRoleGrantToMatchUpdatableProperties(RoleGrant expectedRoleGrant) {
        assertRoleGrantAllUpdatablePropertiesEquals(expectedRoleGrant, getPersistedRoleGrant(expectedRoleGrant));
    }
}
