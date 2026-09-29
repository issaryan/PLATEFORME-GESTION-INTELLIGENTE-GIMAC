package cm.gimac.des.iam.web.rest;

import static cm.gimac.des.iam.domain.CollaboratorAsserts.*;
import static cm.gimac.des.iam.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.iam.IntegrationTest;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.enumeration.CollaboratorStatus;
import cm.gimac.des.iam.domain.enumeration.HabilitationLevel;
import cm.gimac.des.iam.repository.CollaboratorRepository;
import cm.gimac.des.iam.service.dto.CollaboratorDTO;
import cm.gimac.des.iam.service.mapper.CollaboratorMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link CollaboratorResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.iam.security.AuthoritiesConstants.ADMIN)
class CollaboratorResourceIT {

    private static final String DEFAULT_FEDERATED_ID = "AAAAAAAAAA";
    private static final String UPDATED_FEDERATED_ID = "BBBBBBBBBB";

    private static final String DEFAULT_DIRECTORY_ID = "AAAAAAAAAA";
    private static final String UPDATED_DIRECTORY_ID = "BBBBBBBBBB";

    private static final String DEFAULT_DISPLAY_NAME = "AAAAAAAAAA";
    private static final String UPDATED_DISPLAY_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_EMAIL = "AAAAAAAAAA";
    private static final String UPDATED_EMAIL = "BBBBBBBBBB";

    private static final CollaboratorStatus DEFAULT_STATUS = CollaboratorStatus.ACTIVE;
    private static final CollaboratorStatus UPDATED_STATUS = CollaboratorStatus.LOCKED;

    private static final HabilitationLevel DEFAULT_LEVEL = HabilitationLevel.CONSULTATION;
    private static final HabilitationLevel UPDATED_LEVEL = HabilitationLevel.EXECUTION;

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final String DEFAULT_LANGUAGE = "AAAAA";
    private static final String UPDATED_LANGUAGE = "BBBBB";

    private static final Integer DEFAULT_FAILED_ATTEMPTS = 0;
    private static final Integer UPDATED_FAILED_ATTEMPTS = 1;
    private static final Integer SMALLER_FAILED_ATTEMPTS = 0 - 1;

    private static final Instant DEFAULT_LAST_LOGIN_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_LOGIN_AT = Instant.ofEpochMilli(1702849447300L);

    private static final Long DEFAULT_DIRECTORY_CURSOR = 1L;
    private static final Long UPDATED_DIRECTORY_CURSOR = 2L;
    private static final Long SMALLER_DIRECTORY_CURSOR = 1L - 1L;

    private static final String ENTITY_API_URL = "/api/collaborators";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private CollaboratorMapper collaboratorMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCollaboratorMockMvc;

    private Collaborator collaborator;

    private Collaborator insertedCollaborator;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Collaborator createEntity() {
        return new Collaborator()
            .federatedId(DEFAULT_FEDERATED_ID)
            .directoryId(DEFAULT_DIRECTORY_ID)
            .displayName(DEFAULT_DISPLAY_NAME)
            .email(DEFAULT_EMAIL)
            .status(DEFAULT_STATUS)
            .level(DEFAULT_LEVEL)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .language(DEFAULT_LANGUAGE)
            .failedAttempts(DEFAULT_FAILED_ATTEMPTS)
            .lastLoginAt(DEFAULT_LAST_LOGIN_AT)
            .directoryCursor(DEFAULT_DIRECTORY_CURSOR);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Collaborator createUpdatedEntity() {
        return new Collaborator()
            .federatedId(UPDATED_FEDERATED_ID)
            .directoryId(UPDATED_DIRECTORY_ID)
            .displayName(UPDATED_DISPLAY_NAME)
            .email(UPDATED_EMAIL)
            .status(UPDATED_STATUS)
            .level(UPDATED_LEVEL)
            .institutionId(UPDATED_INSTITUTION_ID)
            .language(UPDATED_LANGUAGE)
            .failedAttempts(UPDATED_FAILED_ATTEMPTS)
            .lastLoginAt(UPDATED_LAST_LOGIN_AT)
            .directoryCursor(UPDATED_DIRECTORY_CURSOR);
    }

    @BeforeEach
    void initTest() {
        collaborator = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCollaborator != null) {
            collaboratorRepository.delete(insertedCollaborator);
            insertedCollaborator = null;
        }
    }

    @Test
    @Transactional
    void createCollaborator() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Collaborator
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);
        var returnedCollaboratorDTO = om.readValue(
            restCollaboratorMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(collaboratorDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CollaboratorDTO.class
        );

        // Validate the Collaborator in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCollaborator = collaboratorMapper.toEntity(returnedCollaboratorDTO);
        assertCollaboratorUpdatableFieldsEquals(returnedCollaborator, getPersistedCollaborator(returnedCollaborator));

        insertedCollaborator = returnedCollaborator;
    }

    @Test
    @Transactional
    void createCollaboratorWithExistingId() throws Exception {
        // Create the Collaborator with an existing ID
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCollaboratorMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Collaborator in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkFederatedIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        collaborator.setFederatedId(null);

        // Create the Collaborator, which fails.
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        restCollaboratorMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDisplayNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        collaborator.setDisplayName(null);

        // Create the Collaborator, which fails.
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        restCollaboratorMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        collaborator.setStatus(null);

        // Create the Collaborator, which fails.
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        restCollaboratorMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLevelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        collaborator.setLevel(null);

        // Create the Collaborator, which fails.
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        restCollaboratorMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLanguageIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        collaborator.setLanguage(null);

        // Create the Collaborator, which fails.
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        restCollaboratorMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkFailedAttemptsIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        collaborator.setFailedAttempts(null);

        // Create the Collaborator, which fails.
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        restCollaboratorMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCollaborators() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList
        restCollaboratorMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(collaborator.getId().toString())))
            .andExpect(jsonPath("$.[*].federatedId").value(hasItem(DEFAULT_FEDERATED_ID)))
            .andExpect(jsonPath("$.[*].directoryId").value(hasItem(DEFAULT_DIRECTORY_ID)))
            .andExpect(jsonPath("$.[*].displayName").value(hasItem(DEFAULT_DISPLAY_NAME)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].level").value(hasItem(DEFAULT_LEVEL.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].language").value(hasItem(DEFAULT_LANGUAGE)))
            .andExpect(jsonPath("$.[*].failedAttempts").value(hasItem(DEFAULT_FAILED_ATTEMPTS)))
            .andExpect(jsonPath("$.[*].lastLoginAt").value(hasItem(DEFAULT_LAST_LOGIN_AT.toString())))
            .andExpect(jsonPath("$.[*].directoryCursor").value(hasItem(DEFAULT_DIRECTORY_CURSOR.intValue())));
    }

    @Test
    @Transactional
    void getCollaborator() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get the collaborator
        restCollaboratorMockMvc
            .perform(get(ENTITY_API_URL_ID, collaborator.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(collaborator.getId().toString()))
            .andExpect(jsonPath("$.federatedId").value(DEFAULT_FEDERATED_ID))
            .andExpect(jsonPath("$.directoryId").value(DEFAULT_DIRECTORY_ID))
            .andExpect(jsonPath("$.displayName").value(DEFAULT_DISPLAY_NAME))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.level").value(DEFAULT_LEVEL.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.language").value(DEFAULT_LANGUAGE))
            .andExpect(jsonPath("$.failedAttempts").value(DEFAULT_FAILED_ATTEMPTS))
            .andExpect(jsonPath("$.lastLoginAt").value(DEFAULT_LAST_LOGIN_AT.toString()))
            .andExpect(jsonPath("$.directoryCursor").value(DEFAULT_DIRECTORY_CURSOR.intValue()));
    }

    @Test
    @Transactional
    void getCollaboratorsByIdFiltering() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        UUID id = collaborator.getId();

        defaultCollaboratorFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFederatedIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where federatedId equals to
        defaultCollaboratorFiltering("federatedId.equals=" + DEFAULT_FEDERATED_ID, "federatedId.equals=" + UPDATED_FEDERATED_ID);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFederatedIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where federatedId in
        defaultCollaboratorFiltering(
            "federatedId.in=" + DEFAULT_FEDERATED_ID + "," + UPDATED_FEDERATED_ID,
            "federatedId.in=" + UPDATED_FEDERATED_ID
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFederatedIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where federatedId is not null
        defaultCollaboratorFiltering("federatedId.specified=true", "federatedId.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFederatedIdContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where federatedId contains
        defaultCollaboratorFiltering("federatedId.contains=" + DEFAULT_FEDERATED_ID, "federatedId.contains=" + UPDATED_FEDERATED_ID);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFederatedIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where federatedId does not contain
        defaultCollaboratorFiltering(
            "federatedId.doesNotContain=" + UPDATED_FEDERATED_ID,
            "federatedId.doesNotContain=" + DEFAULT_FEDERATED_ID
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryId equals to
        defaultCollaboratorFiltering("directoryId.equals=" + DEFAULT_DIRECTORY_ID, "directoryId.equals=" + UPDATED_DIRECTORY_ID);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryId in
        defaultCollaboratorFiltering(
            "directoryId.in=" + DEFAULT_DIRECTORY_ID + "," + UPDATED_DIRECTORY_ID,
            "directoryId.in=" + UPDATED_DIRECTORY_ID
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryId is not null
        defaultCollaboratorFiltering("directoryId.specified=true", "directoryId.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryIdContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryId contains
        defaultCollaboratorFiltering("directoryId.contains=" + DEFAULT_DIRECTORY_ID, "directoryId.contains=" + UPDATED_DIRECTORY_ID);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryId does not contain
        defaultCollaboratorFiltering(
            "directoryId.doesNotContain=" + UPDATED_DIRECTORY_ID,
            "directoryId.doesNotContain=" + DEFAULT_DIRECTORY_ID
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDisplayNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where displayName equals to
        defaultCollaboratorFiltering("displayName.equals=" + DEFAULT_DISPLAY_NAME, "displayName.equals=" + UPDATED_DISPLAY_NAME);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDisplayNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where displayName in
        defaultCollaboratorFiltering(
            "displayName.in=" + DEFAULT_DISPLAY_NAME + "," + UPDATED_DISPLAY_NAME,
            "displayName.in=" + UPDATED_DISPLAY_NAME
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDisplayNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where displayName is not null
        defaultCollaboratorFiltering("displayName.specified=true", "displayName.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDisplayNameContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where displayName contains
        defaultCollaboratorFiltering("displayName.contains=" + DEFAULT_DISPLAY_NAME, "displayName.contains=" + UPDATED_DISPLAY_NAME);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDisplayNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where displayName does not contain
        defaultCollaboratorFiltering(
            "displayName.doesNotContain=" + UPDATED_DISPLAY_NAME,
            "displayName.doesNotContain=" + DEFAULT_DISPLAY_NAME
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByEmailIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where email equals to
        defaultCollaboratorFiltering("email.equals=" + DEFAULT_EMAIL, "email.equals=" + UPDATED_EMAIL);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByEmailIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where email in
        defaultCollaboratorFiltering("email.in=" + DEFAULT_EMAIL + "," + UPDATED_EMAIL, "email.in=" + UPDATED_EMAIL);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByEmailIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where email is not null
        defaultCollaboratorFiltering("email.specified=true", "email.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorsByEmailContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where email contains
        defaultCollaboratorFiltering("email.contains=" + DEFAULT_EMAIL, "email.contains=" + UPDATED_EMAIL);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByEmailNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where email does not contain
        defaultCollaboratorFiltering("email.doesNotContain=" + UPDATED_EMAIL, "email.doesNotContain=" + DEFAULT_EMAIL);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where status equals to
        defaultCollaboratorFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where status in
        defaultCollaboratorFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where status is not null
        defaultCollaboratorFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorsByLevelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where level equals to
        defaultCollaboratorFiltering("level.equals=" + DEFAULT_LEVEL, "level.equals=" + UPDATED_LEVEL);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByLevelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where level in
        defaultCollaboratorFiltering("level.in=" + DEFAULT_LEVEL + "," + UPDATED_LEVEL, "level.in=" + UPDATED_LEVEL);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByLevelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where level is not null
        defaultCollaboratorFiltering("level.specified=true", "level.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorsByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where institutionId equals to
        defaultCollaboratorFiltering("institutionId.equals=" + DEFAULT_INSTITUTION_ID, "institutionId.equals=" + UPDATED_INSTITUTION_ID);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where institutionId in
        defaultCollaboratorFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where institutionId is not null
        defaultCollaboratorFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorsByLanguageIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where language equals to
        defaultCollaboratorFiltering("language.equals=" + DEFAULT_LANGUAGE, "language.equals=" + UPDATED_LANGUAGE);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByLanguageIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where language in
        defaultCollaboratorFiltering("language.in=" + DEFAULT_LANGUAGE + "," + UPDATED_LANGUAGE, "language.in=" + UPDATED_LANGUAGE);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByLanguageIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where language is not null
        defaultCollaboratorFiltering("language.specified=true", "language.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorsByLanguageContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where language contains
        defaultCollaboratorFiltering("language.contains=" + DEFAULT_LANGUAGE, "language.contains=" + UPDATED_LANGUAGE);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByLanguageNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where language does not contain
        defaultCollaboratorFiltering("language.doesNotContain=" + UPDATED_LANGUAGE, "language.doesNotContain=" + DEFAULT_LANGUAGE);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFailedAttemptsIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where failedAttempts equals to
        defaultCollaboratorFiltering(
            "failedAttempts.equals=" + DEFAULT_FAILED_ATTEMPTS,
            "failedAttempts.equals=" + UPDATED_FAILED_ATTEMPTS
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFailedAttemptsIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where failedAttempts in
        defaultCollaboratorFiltering(
            "failedAttempts.in=" + DEFAULT_FAILED_ATTEMPTS + "," + UPDATED_FAILED_ATTEMPTS,
            "failedAttempts.in=" + UPDATED_FAILED_ATTEMPTS
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFailedAttemptsIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where failedAttempts is not null
        defaultCollaboratorFiltering("failedAttempts.specified=true", "failedAttempts.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFailedAttemptsIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where failedAttempts is greater than or equal to
        defaultCollaboratorFiltering(
            "failedAttempts.greaterThanOrEqual=" + DEFAULT_FAILED_ATTEMPTS,
            "failedAttempts.greaterThanOrEqual=" + UPDATED_FAILED_ATTEMPTS
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFailedAttemptsIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where failedAttempts is less than or equal to
        defaultCollaboratorFiltering(
            "failedAttempts.lessThanOrEqual=" + DEFAULT_FAILED_ATTEMPTS,
            "failedAttempts.lessThanOrEqual=" + SMALLER_FAILED_ATTEMPTS
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFailedAttemptsIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where failedAttempts is less than
        defaultCollaboratorFiltering(
            "failedAttempts.lessThan=" + UPDATED_FAILED_ATTEMPTS,
            "failedAttempts.lessThan=" + DEFAULT_FAILED_ATTEMPTS
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByFailedAttemptsIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where failedAttempts is greater than
        defaultCollaboratorFiltering(
            "failedAttempts.greaterThan=" + SMALLER_FAILED_ATTEMPTS,
            "failedAttempts.greaterThan=" + DEFAULT_FAILED_ATTEMPTS
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByLastLoginAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where lastLoginAt equals to
        defaultCollaboratorFiltering("lastLoginAt.equals=" + DEFAULT_LAST_LOGIN_AT, "lastLoginAt.equals=" + UPDATED_LAST_LOGIN_AT);
    }

    @Test
    @Transactional
    void getAllCollaboratorsByLastLoginAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where lastLoginAt in
        defaultCollaboratorFiltering(
            "lastLoginAt.in=" + DEFAULT_LAST_LOGIN_AT + "," + UPDATED_LAST_LOGIN_AT,
            "lastLoginAt.in=" + UPDATED_LAST_LOGIN_AT
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByLastLoginAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where lastLoginAt is not null
        defaultCollaboratorFiltering("lastLoginAt.specified=true", "lastLoginAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryCursorIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryCursor equals to
        defaultCollaboratorFiltering(
            "directoryCursor.equals=" + DEFAULT_DIRECTORY_CURSOR,
            "directoryCursor.equals=" + UPDATED_DIRECTORY_CURSOR
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryCursorIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryCursor in
        defaultCollaboratorFiltering(
            "directoryCursor.in=" + DEFAULT_DIRECTORY_CURSOR + "," + UPDATED_DIRECTORY_CURSOR,
            "directoryCursor.in=" + UPDATED_DIRECTORY_CURSOR
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryCursorIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryCursor is not null
        defaultCollaboratorFiltering("directoryCursor.specified=true", "directoryCursor.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryCursorIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryCursor is greater than or equal to
        defaultCollaboratorFiltering(
            "directoryCursor.greaterThanOrEqual=" + DEFAULT_DIRECTORY_CURSOR,
            "directoryCursor.greaterThanOrEqual=" + UPDATED_DIRECTORY_CURSOR
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryCursorIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryCursor is less than or equal to
        defaultCollaboratorFiltering(
            "directoryCursor.lessThanOrEqual=" + DEFAULT_DIRECTORY_CURSOR,
            "directoryCursor.lessThanOrEqual=" + SMALLER_DIRECTORY_CURSOR
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryCursorIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryCursor is less than
        defaultCollaboratorFiltering(
            "directoryCursor.lessThan=" + UPDATED_DIRECTORY_CURSOR,
            "directoryCursor.lessThan=" + DEFAULT_DIRECTORY_CURSOR
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorsByDirectoryCursorIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        // Get all the collaboratorList where directoryCursor is greater than
        defaultCollaboratorFiltering(
            "directoryCursor.greaterThan=" + SMALLER_DIRECTORY_CURSOR,
            "directoryCursor.greaterThan=" + DEFAULT_DIRECTORY_CURSOR
        );
    }

    private void defaultCollaboratorFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultCollaboratorShouldBeFound(shouldBeFound);
        defaultCollaboratorShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultCollaboratorShouldBeFound(String filter) throws Exception {
        restCollaboratorMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(collaborator.getId().toString())))
            .andExpect(jsonPath("$.[*].federatedId").value(hasItem(DEFAULT_FEDERATED_ID)))
            .andExpect(jsonPath("$.[*].directoryId").value(hasItem(DEFAULT_DIRECTORY_ID)))
            .andExpect(jsonPath("$.[*].displayName").value(hasItem(DEFAULT_DISPLAY_NAME)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].level").value(hasItem(DEFAULT_LEVEL.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].language").value(hasItem(DEFAULT_LANGUAGE)))
            .andExpect(jsonPath("$.[*].failedAttempts").value(hasItem(DEFAULT_FAILED_ATTEMPTS)))
            .andExpect(jsonPath("$.[*].lastLoginAt").value(hasItem(DEFAULT_LAST_LOGIN_AT.toString())))
            .andExpect(jsonPath("$.[*].directoryCursor").value(hasItem(DEFAULT_DIRECTORY_CURSOR.intValue())));

        // Check, that the count call also returns 1
        restCollaboratorMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultCollaboratorShouldNotBeFound(String filter) throws Exception {
        restCollaboratorMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCollaboratorMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingCollaborator() throws Exception {
        // Get the collaborator
        restCollaboratorMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCollaborator() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the collaborator
        Collaborator updatedCollaborator = collaboratorRepository.findById(collaborator.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCollaborator are not directly saved in db
        em.detach(updatedCollaborator);
        updatedCollaborator
            .federatedId(UPDATED_FEDERATED_ID)
            .directoryId(UPDATED_DIRECTORY_ID)
            .displayName(UPDATED_DISPLAY_NAME)
            .email(UPDATED_EMAIL)
            .status(UPDATED_STATUS)
            .level(UPDATED_LEVEL)
            .institutionId(UPDATED_INSTITUTION_ID)
            .language(UPDATED_LANGUAGE)
            .failedAttempts(UPDATED_FAILED_ATTEMPTS)
            .lastLoginAt(UPDATED_LAST_LOGIN_AT)
            .directoryCursor(UPDATED_DIRECTORY_CURSOR);
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(updatedCollaborator);

        restCollaboratorMockMvc
            .perform(
                put(ENTITY_API_URL_ID, collaboratorDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isOk());

        // Validate the Collaborator in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCollaboratorToMatchAllProperties(updatedCollaborator);
    }

    @Test
    @Transactional
    void putNonExistingCollaborator() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        collaborator.setId(UUID.randomUUID());

        // Create the Collaborator
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCollaboratorMockMvc
            .perform(
                put(ENTITY_API_URL_ID, collaboratorDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Collaborator in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCollaborator() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        collaborator.setId(UUID.randomUUID());

        // Create the Collaborator
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCollaboratorMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Collaborator in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCollaborator() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        collaborator.setId(UUID.randomUUID());

        // Create the Collaborator
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCollaboratorMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Collaborator in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCollaboratorWithPatch() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the collaborator using partial update
        Collaborator partialUpdatedCollaborator = new Collaborator();
        partialUpdatedCollaborator.setId(collaborator.getId());

        partialUpdatedCollaborator
            .federatedId(UPDATED_FEDERATED_ID)
            .directoryId(UPDATED_DIRECTORY_ID)
            .displayName(UPDATED_DISPLAY_NAME)
            .email(UPDATED_EMAIL)
            .status(UPDATED_STATUS)
            .level(UPDATED_LEVEL)
            .institutionId(UPDATED_INSTITUTION_ID)
            .directoryCursor(UPDATED_DIRECTORY_CURSOR);

        restCollaboratorMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCollaborator.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCollaborator))
            )
            .andExpect(status().isOk());

        // Validate the Collaborator in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCollaboratorUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedCollaborator, collaborator),
            getPersistedCollaborator(collaborator)
        );
    }

    @Test
    @Transactional
    void fullUpdateCollaboratorWithPatch() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the collaborator using partial update
        Collaborator partialUpdatedCollaborator = new Collaborator();
        partialUpdatedCollaborator.setId(collaborator.getId());

        partialUpdatedCollaborator
            .federatedId(UPDATED_FEDERATED_ID)
            .directoryId(UPDATED_DIRECTORY_ID)
            .displayName(UPDATED_DISPLAY_NAME)
            .email(UPDATED_EMAIL)
            .status(UPDATED_STATUS)
            .level(UPDATED_LEVEL)
            .institutionId(UPDATED_INSTITUTION_ID)
            .language(UPDATED_LANGUAGE)
            .failedAttempts(UPDATED_FAILED_ATTEMPTS)
            .lastLoginAt(UPDATED_LAST_LOGIN_AT)
            .directoryCursor(UPDATED_DIRECTORY_CURSOR);

        restCollaboratorMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCollaborator.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCollaborator))
            )
            .andExpect(status().isOk());

        // Validate the Collaborator in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCollaboratorUpdatableFieldsEquals(partialUpdatedCollaborator, getPersistedCollaborator(partialUpdatedCollaborator));
    }

    @Test
    @Transactional
    void patchNonExistingCollaborator() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        collaborator.setId(UUID.randomUUID());

        // Create the Collaborator
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCollaboratorMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, collaboratorDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Collaborator in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCollaborator() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        collaborator.setId(UUID.randomUUID());

        // Create the Collaborator
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCollaboratorMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Collaborator in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCollaborator() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        collaborator.setId(UUID.randomUUID());

        // Create the Collaborator
        CollaboratorDTO collaboratorDTO = collaboratorMapper.toDto(collaborator);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCollaboratorMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(collaboratorDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Collaborator in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCollaborator() throws Exception {
        // Initialize the database
        insertedCollaborator = collaboratorRepository.saveAndFlush(collaborator);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the collaborator
        restCollaboratorMockMvc
            .perform(delete(ENTITY_API_URL_ID, collaborator.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return collaboratorRepository.count();
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

    protected Collaborator getPersistedCollaborator(Collaborator collaborator) {
        return collaboratorRepository.findById(collaborator.getId()).orElseThrow();
    }

    protected void assertPersistedCollaboratorToMatchAllProperties(Collaborator expectedCollaborator) {
        assertCollaboratorAllPropertiesEquals(expectedCollaborator, getPersistedCollaborator(expectedCollaborator));
    }

    protected void assertPersistedCollaboratorToMatchUpdatableProperties(Collaborator expectedCollaborator) {
        assertCollaboratorAllUpdatablePropertiesEquals(expectedCollaborator, getPersistedCollaborator(expectedCollaborator));
    }
}
