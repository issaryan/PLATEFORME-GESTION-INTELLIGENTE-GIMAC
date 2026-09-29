package cm.gimac.des.iam.web.rest;

import static cm.gimac.des.iam.domain.UserSessionAsserts.*;
import static cm.gimac.des.iam.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.iam.IntegrationTest;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.UserSession;
import cm.gimac.des.iam.domain.enumeration.SessionState;
import cm.gimac.des.iam.repository.UserSessionRepository;
import cm.gimac.des.iam.service.UserSessionService;
import cm.gimac.des.iam.service.dto.UserSessionDTO;
import cm.gimac.des.iam.service.mapper.UserSessionMapper;
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
 * Integration tests for the {@link UserSessionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.iam.security.AuthoritiesConstants.ADMIN)
class UserSessionResourceIT {

    private static final String DEFAULT_CERTIFICATE_THUMBPRINT = "AAAAAAAAAA";
    private static final String UPDATED_CERTIFICATE_THUMBPRINT = "BBBBBBBBBB";

    private static final String DEFAULT_AUTH_METHODS = "AAAAAAAAAA";
    private static final String UPDATED_AUTH_METHODS = "BBBBBBBBBB";

    private static final Instant DEFAULT_ISSUED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ISSUED_AT = Instant.ofEpochMilli(1702849447300L);

    private static final Instant DEFAULT_EXPIRES_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXPIRES_AT = Instant.ofEpochMilli(1702849447300L);

    private static final SessionState DEFAULT_STATE = SessionState.ACTIVE;
    private static final SessionState UPDATED_STATE = SessionState.EXPIRED;

    private static final Instant DEFAULT_REVOKED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REVOKED_AT = Instant.ofEpochMilli(1702849447300L);

    private static final String DEFAULT_REVOCATION_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REVOCATION_REASON = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/user-sessions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Mock
    private UserSessionRepository userSessionRepositoryMock;

    @Autowired
    private UserSessionMapper userSessionMapper;

    @Mock
    private UserSessionService userSessionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restUserSessionMockMvc;

    private UserSession userSession;

    private UserSession insertedUserSession;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserSession createEntity(EntityManager em) {
        UserSession userSession = new UserSession()
            .certificateThumbprint(DEFAULT_CERTIFICATE_THUMBPRINT)
            .authMethods(DEFAULT_AUTH_METHODS)
            .issuedAt(DEFAULT_ISSUED_AT)
            .expiresAt(DEFAULT_EXPIRES_AT)
            .state(DEFAULT_STATE)
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
        userSession.setCollaborator(collaborator);
        return userSession;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserSession createUpdatedEntity(EntityManager em) {
        UserSession updatedUserSession = new UserSession()
            .certificateThumbprint(UPDATED_CERTIFICATE_THUMBPRINT)
            .authMethods(UPDATED_AUTH_METHODS)
            .issuedAt(UPDATED_ISSUED_AT)
            .expiresAt(UPDATED_EXPIRES_AT)
            .state(UPDATED_STATE)
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
        updatedUserSession.setCollaborator(collaborator);
        return updatedUserSession;
    }

    @BeforeEach
    void initTest() {
        userSession = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedUserSession != null) {
            userSessionRepository.delete(insertedUserSession);
            insertedUserSession = null;
        }
    }

    @Test
    @Transactional
    void createUserSession() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the UserSession
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);
        var returnedUserSessionDTO = om.readValue(
            restUserSessionMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userSessionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            UserSessionDTO.class
        );

        // Validate the UserSession in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedUserSession = userSessionMapper.toEntity(returnedUserSessionDTO);
        assertUserSessionUpdatableFieldsEquals(returnedUserSession, getPersistedUserSession(returnedUserSession));

        insertedUserSession = returnedUserSession;
    }

    @Test
    @Transactional
    void createUserSessionWithExistingId() throws Exception {
        // Create the UserSession with an existing ID
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserSessionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserSession in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCertificateThumbprintIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        userSession.setCertificateThumbprint(null);

        // Create the UserSession, which fails.
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        restUserSessionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAuthMethodsIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        userSession.setAuthMethods(null);

        // Create the UserSession, which fails.
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        restUserSessionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkIssuedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        userSession.setIssuedAt(null);

        // Create the UserSession, which fails.
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        restUserSessionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkExpiresAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        userSession.setExpiresAt(null);

        // Create the UserSession, which fails.
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        restUserSessionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        userSession.setState(null);

        // Create the UserSession, which fails.
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        restUserSessionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllUserSessions() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList
        restUserSessionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(userSession.getId().toString())))
            .andExpect(jsonPath("$.[*].certificateThumbprint").value(hasItem(DEFAULT_CERTIFICATE_THUMBPRINT)))
            .andExpect(jsonPath("$.[*].authMethods").value(hasItem(DEFAULT_AUTH_METHODS)))
            .andExpect(jsonPath("$.[*].issuedAt").value(hasItem(DEFAULT_ISSUED_AT.toString())))
            .andExpect(jsonPath("$.[*].expiresAt").value(hasItem(DEFAULT_EXPIRES_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].revokedAt").value(hasItem(DEFAULT_REVOKED_AT.toString())))
            .andExpect(jsonPath("$.[*].revocationReason").value(hasItem(DEFAULT_REVOCATION_REASON)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllUserSessionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(userSessionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restUserSessionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(userSessionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllUserSessionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(userSessionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restUserSessionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(userSessionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getUserSession() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get the userSession
        restUserSessionMockMvc
            .perform(get(ENTITY_API_URL_ID, userSession.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(userSession.getId().toString()))
            .andExpect(jsonPath("$.certificateThumbprint").value(DEFAULT_CERTIFICATE_THUMBPRINT))
            .andExpect(jsonPath("$.authMethods").value(DEFAULT_AUTH_METHODS))
            .andExpect(jsonPath("$.issuedAt").value(DEFAULT_ISSUED_AT.toString()))
            .andExpect(jsonPath("$.expiresAt").value(DEFAULT_EXPIRES_AT.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.revokedAt").value(DEFAULT_REVOKED_AT.toString()))
            .andExpect(jsonPath("$.revocationReason").value(DEFAULT_REVOCATION_REASON));
    }

    @Test
    @Transactional
    void getUserSessionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        UUID id = userSession.getId();

        defaultUserSessionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllUserSessionsByCertificateThumbprintIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where certificateThumbprint equals to
        defaultUserSessionFiltering(
            "certificateThumbprint.equals=" + DEFAULT_CERTIFICATE_THUMBPRINT,
            "certificateThumbprint.equals=" + UPDATED_CERTIFICATE_THUMBPRINT
        );
    }

    @Test
    @Transactional
    void getAllUserSessionsByCertificateThumbprintIsInShouldWork() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where certificateThumbprint in
        defaultUserSessionFiltering(
            "certificateThumbprint.in=" + DEFAULT_CERTIFICATE_THUMBPRINT + "," + UPDATED_CERTIFICATE_THUMBPRINT,
            "certificateThumbprint.in=" + UPDATED_CERTIFICATE_THUMBPRINT
        );
    }

    @Test
    @Transactional
    void getAllUserSessionsByCertificateThumbprintIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where certificateThumbprint is not null
        defaultUserSessionFiltering("certificateThumbprint.specified=true", "certificateThumbprint.specified=false");
    }

    @Test
    @Transactional
    void getAllUserSessionsByCertificateThumbprintContainsSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where certificateThumbprint contains
        defaultUserSessionFiltering(
            "certificateThumbprint.contains=" + DEFAULT_CERTIFICATE_THUMBPRINT,
            "certificateThumbprint.contains=" + UPDATED_CERTIFICATE_THUMBPRINT
        );
    }

    @Test
    @Transactional
    void getAllUserSessionsByCertificateThumbprintNotContainsSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where certificateThumbprint does not contain
        defaultUserSessionFiltering(
            "certificateThumbprint.doesNotContain=" + UPDATED_CERTIFICATE_THUMBPRINT,
            "certificateThumbprint.doesNotContain=" + DEFAULT_CERTIFICATE_THUMBPRINT
        );
    }

    @Test
    @Transactional
    void getAllUserSessionsByAuthMethodsIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where authMethods equals to
        defaultUserSessionFiltering("authMethods.equals=" + DEFAULT_AUTH_METHODS, "authMethods.equals=" + UPDATED_AUTH_METHODS);
    }

    @Test
    @Transactional
    void getAllUserSessionsByAuthMethodsIsInShouldWork() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where authMethods in
        defaultUserSessionFiltering(
            "authMethods.in=" + DEFAULT_AUTH_METHODS + "," + UPDATED_AUTH_METHODS,
            "authMethods.in=" + UPDATED_AUTH_METHODS
        );
    }

    @Test
    @Transactional
    void getAllUserSessionsByAuthMethodsIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where authMethods is not null
        defaultUserSessionFiltering("authMethods.specified=true", "authMethods.specified=false");
    }

    @Test
    @Transactional
    void getAllUserSessionsByAuthMethodsContainsSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where authMethods contains
        defaultUserSessionFiltering("authMethods.contains=" + DEFAULT_AUTH_METHODS, "authMethods.contains=" + UPDATED_AUTH_METHODS);
    }

    @Test
    @Transactional
    void getAllUserSessionsByAuthMethodsNotContainsSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where authMethods does not contain
        defaultUserSessionFiltering(
            "authMethods.doesNotContain=" + UPDATED_AUTH_METHODS,
            "authMethods.doesNotContain=" + DEFAULT_AUTH_METHODS
        );
    }

    @Test
    @Transactional
    void getAllUserSessionsByIssuedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where issuedAt equals to
        defaultUserSessionFiltering("issuedAt.equals=" + DEFAULT_ISSUED_AT, "issuedAt.equals=" + UPDATED_ISSUED_AT);
    }

    @Test
    @Transactional
    void getAllUserSessionsByIssuedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where issuedAt in
        defaultUserSessionFiltering("issuedAt.in=" + DEFAULT_ISSUED_AT + "," + UPDATED_ISSUED_AT, "issuedAt.in=" + UPDATED_ISSUED_AT);
    }

    @Test
    @Transactional
    void getAllUserSessionsByIssuedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where issuedAt is not null
        defaultUserSessionFiltering("issuedAt.specified=true", "issuedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllUserSessionsByExpiresAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where expiresAt equals to
        defaultUserSessionFiltering("expiresAt.equals=" + DEFAULT_EXPIRES_AT, "expiresAt.equals=" + UPDATED_EXPIRES_AT);
    }

    @Test
    @Transactional
    void getAllUserSessionsByExpiresAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where expiresAt in
        defaultUserSessionFiltering("expiresAt.in=" + DEFAULT_EXPIRES_AT + "," + UPDATED_EXPIRES_AT, "expiresAt.in=" + UPDATED_EXPIRES_AT);
    }

    @Test
    @Transactional
    void getAllUserSessionsByExpiresAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where expiresAt is not null
        defaultUserSessionFiltering("expiresAt.specified=true", "expiresAt.specified=false");
    }

    @Test
    @Transactional
    void getAllUserSessionsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where state equals to
        defaultUserSessionFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllUserSessionsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where state in
        defaultUserSessionFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllUserSessionsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where state is not null
        defaultUserSessionFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllUserSessionsByRevokedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where revokedAt equals to
        defaultUserSessionFiltering("revokedAt.equals=" + DEFAULT_REVOKED_AT, "revokedAt.equals=" + UPDATED_REVOKED_AT);
    }

    @Test
    @Transactional
    void getAllUserSessionsByRevokedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where revokedAt in
        defaultUserSessionFiltering("revokedAt.in=" + DEFAULT_REVOKED_AT + "," + UPDATED_REVOKED_AT, "revokedAt.in=" + UPDATED_REVOKED_AT);
    }

    @Test
    @Transactional
    void getAllUserSessionsByRevokedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where revokedAt is not null
        defaultUserSessionFiltering("revokedAt.specified=true", "revokedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllUserSessionsByRevocationReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where revocationReason equals to
        defaultUserSessionFiltering(
            "revocationReason.equals=" + DEFAULT_REVOCATION_REASON,
            "revocationReason.equals=" + UPDATED_REVOCATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllUserSessionsByRevocationReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where revocationReason in
        defaultUserSessionFiltering(
            "revocationReason.in=" + DEFAULT_REVOCATION_REASON + "," + UPDATED_REVOCATION_REASON,
            "revocationReason.in=" + UPDATED_REVOCATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllUserSessionsByRevocationReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where revocationReason is not null
        defaultUserSessionFiltering("revocationReason.specified=true", "revocationReason.specified=false");
    }

    @Test
    @Transactional
    void getAllUserSessionsByRevocationReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where revocationReason contains
        defaultUserSessionFiltering(
            "revocationReason.contains=" + DEFAULT_REVOCATION_REASON,
            "revocationReason.contains=" + UPDATED_REVOCATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllUserSessionsByRevocationReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        // Get all the userSessionList where revocationReason does not contain
        defaultUserSessionFiltering(
            "revocationReason.doesNotContain=" + UPDATED_REVOCATION_REASON,
            "revocationReason.doesNotContain=" + DEFAULT_REVOCATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllUserSessionsByCollaboratorIsEqualToSomething() throws Exception {
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            userSessionRepository.saveAndFlush(userSession);
            collaborator = CollaboratorResourceIT.createEntity();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        em.persist(collaborator);
        em.flush();
        userSession.setCollaborator(collaborator);
        userSessionRepository.saveAndFlush(userSession);
        UUID collaboratorId = collaborator.getId();
        // Get all the userSessionList where collaborator equals to collaboratorId
        defaultUserSessionShouldBeFound("collaboratorId.equals=" + collaboratorId);

        // Get all the userSessionList where collaborator equals to UUID.randomUUID()
        defaultUserSessionShouldNotBeFound("collaboratorId.equals=" + UUID.randomUUID());
    }

    private void defaultUserSessionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultUserSessionShouldBeFound(shouldBeFound);
        defaultUserSessionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultUserSessionShouldBeFound(String filter) throws Exception {
        restUserSessionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(userSession.getId().toString())))
            .andExpect(jsonPath("$.[*].certificateThumbprint").value(hasItem(DEFAULT_CERTIFICATE_THUMBPRINT)))
            .andExpect(jsonPath("$.[*].authMethods").value(hasItem(DEFAULT_AUTH_METHODS)))
            .andExpect(jsonPath("$.[*].issuedAt").value(hasItem(DEFAULT_ISSUED_AT.toString())))
            .andExpect(jsonPath("$.[*].expiresAt").value(hasItem(DEFAULT_EXPIRES_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].revokedAt").value(hasItem(DEFAULT_REVOKED_AT.toString())))
            .andExpect(jsonPath("$.[*].revocationReason").value(hasItem(DEFAULT_REVOCATION_REASON)));

        // Check, that the count call also returns 1
        restUserSessionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultUserSessionShouldNotBeFound(String filter) throws Exception {
        restUserSessionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restUserSessionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingUserSession() throws Exception {
        // Get the userSession
        restUserSessionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingUserSession() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the userSession
        UserSession updatedUserSession = userSessionRepository.findById(userSession.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedUserSession are not directly saved in db
        em.detach(updatedUserSession);
        updatedUserSession
            .certificateThumbprint(UPDATED_CERTIFICATE_THUMBPRINT)
            .authMethods(UPDATED_AUTH_METHODS)
            .issuedAt(UPDATED_ISSUED_AT)
            .expiresAt(UPDATED_EXPIRES_AT)
            .state(UPDATED_STATE)
            .revokedAt(UPDATED_REVOKED_AT)
            .revocationReason(UPDATED_REVOCATION_REASON);
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(updatedUserSession);

        restUserSessionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, userSessionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isOk());

        // Validate the UserSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedUserSessionToMatchAllProperties(updatedUserSession);
    }

    @Test
    @Transactional
    void putNonExistingUserSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userSession.setId(UUID.randomUUID());

        // Create the UserSession
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUserSessionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, userSessionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchUserSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userSession.setId(UUID.randomUUID());

        // Create the UserSession
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserSessionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamUserSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userSession.setId(UUID.randomUUID());

        // Create the UserSession
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserSessionMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userSessionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the UserSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateUserSessionWithPatch() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the userSession using partial update
        UserSession partialUpdatedUserSession = new UserSession();
        partialUpdatedUserSession.setId(userSession.getId());

        partialUpdatedUserSession.issuedAt(UPDATED_ISSUED_AT).expiresAt(UPDATED_EXPIRES_AT).revokedAt(UPDATED_REVOKED_AT);

        restUserSessionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedUserSession.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedUserSession))
            )
            .andExpect(status().isOk());

        // Validate the UserSession in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertUserSessionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedUserSession, userSession),
            getPersistedUserSession(userSession)
        );
    }

    @Test
    @Transactional
    void fullUpdateUserSessionWithPatch() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the userSession using partial update
        UserSession partialUpdatedUserSession = new UserSession();
        partialUpdatedUserSession.setId(userSession.getId());

        partialUpdatedUserSession
            .certificateThumbprint(UPDATED_CERTIFICATE_THUMBPRINT)
            .authMethods(UPDATED_AUTH_METHODS)
            .issuedAt(UPDATED_ISSUED_AT)
            .expiresAt(UPDATED_EXPIRES_AT)
            .state(UPDATED_STATE)
            .revokedAt(UPDATED_REVOKED_AT)
            .revocationReason(UPDATED_REVOCATION_REASON);

        restUserSessionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedUserSession.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedUserSession))
            )
            .andExpect(status().isOk());

        // Validate the UserSession in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertUserSessionUpdatableFieldsEquals(partialUpdatedUserSession, getPersistedUserSession(partialUpdatedUserSession));
    }

    @Test
    @Transactional
    void patchNonExistingUserSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userSession.setId(UUID.randomUUID());

        // Create the UserSession
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUserSessionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, userSessionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchUserSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userSession.setId(UUID.randomUUID());

        // Create the UserSession
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserSessionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamUserSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userSession.setId(UUID.randomUUID());

        // Create the UserSession
        UserSessionDTO userSessionDTO = userSessionMapper.toDto(userSession);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserSessionMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(userSessionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the UserSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteUserSession() throws Exception {
        // Initialize the database
        insertedUserSession = userSessionRepository.saveAndFlush(userSession);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the userSession
        restUserSessionMockMvc
            .perform(delete(ENTITY_API_URL_ID, userSession.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return userSessionRepository.count();
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

    protected UserSession getPersistedUserSession(UserSession userSession) {
        return userSessionRepository.findById(userSession.getId()).orElseThrow();
    }

    protected void assertPersistedUserSessionToMatchAllProperties(UserSession expectedUserSession) {
        assertUserSessionAllPropertiesEquals(expectedUserSession, getPersistedUserSession(expectedUserSession));
    }

    protected void assertPersistedUserSessionToMatchUpdatableProperties(UserSession expectedUserSession) {
        assertUserSessionAllUpdatablePropertiesEquals(expectedUserSession, getPersistedUserSession(expectedUserSession));
    }
}
