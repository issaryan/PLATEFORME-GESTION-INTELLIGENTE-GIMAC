package cm.gimac.des.participant.web.rest;

import static cm.gimac.des.participant.domain.ConventionAsserts.*;
import static cm.gimac.des.participant.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.participant.IntegrationTest;
import cm.gimac.des.participant.domain.Convention;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.enumeration.ConventionType;
import cm.gimac.des.participant.repository.ConventionRepository;
import cm.gimac.des.participant.service.ConventionService;
import cm.gimac.des.participant.service.dto.ConventionDTO;
import cm.gimac.des.participant.service.mapper.ConventionMapper;
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
 * Integration tests for the {@link ConventionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ConventionResourceIT {

    private static final ConventionType DEFAULT_TYPE = ConventionType.ADHESION;
    private static final ConventionType UPDATED_TYPE = ConventionType.SERVICE;

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_SIGNED_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_SIGNED_ON = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_SIGNED_ON = LocalDate.ofEpochDay(-1L);

    private static final LocalDate DEFAULT_EXPIRES_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_EXPIRES_ON = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_EXPIRES_ON = LocalDate.ofEpochDay(-1L);

    private static final UUID DEFAULT_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_ID = UUID.randomUUID();

    private static final String DEFAULT_DOCUMENT_SHA_256 = "c0cd8a7ac7a80e385a6752f93f7526ae5f40b3185b8b38045a44cc25049ad623";
    private static final String UPDATED_DOCUMENT_SHA_256 = "2230a0243bf8975542be0ff997fd311c999c904eadd35853f8a61b7157c183be";

    private static final String ENTITY_API_URL = "/api/conventions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ConventionRepository conventionRepository;

    @Mock
    private ConventionRepository conventionRepositoryMock;

    @Autowired
    private ConventionMapper conventionMapper;

    @Mock
    private ConventionService conventionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restConventionMockMvc;

    private Convention convention;

    private Convention insertedConvention;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Convention createEntity(EntityManager em) {
        Convention convention = new Convention()
            .type(DEFAULT_TYPE)
            .reference(DEFAULT_REFERENCE)
            .signedOn(DEFAULT_SIGNED_ON)
            .expiresOn(DEFAULT_EXPIRES_ON)
            .documentId(DEFAULT_DOCUMENT_ID)
            .documentSha256(DEFAULT_DOCUMENT_SHA_256);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).get(0);
        }
        convention.setInstitution(institution);
        return convention;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Convention createUpdatedEntity(EntityManager em) {
        Convention updatedConvention = new Convention()
            .type(UPDATED_TYPE)
            .reference(UPDATED_REFERENCE)
            .signedOn(UPDATED_SIGNED_ON)
            .expiresOn(UPDATED_EXPIRES_ON)
            .documentId(UPDATED_DOCUMENT_ID)
            .documentSha256(UPDATED_DOCUMENT_SHA_256);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createUpdatedEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).get(0);
        }
        updatedConvention.setInstitution(institution);
        return updatedConvention;
    }

    @BeforeEach
    void initTest() {
        convention = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedConvention != null) {
            conventionRepository.delete(insertedConvention);
            insertedConvention = null;
        }
    }

    @Test
    @Transactional
    void createConvention() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Convention
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);
        var returnedConventionDTO = om.readValue(
            restConventionMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conventionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ConventionDTO.class
        );

        // Validate the Convention in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedConvention = conventionMapper.toEntity(returnedConventionDTO);
        assertConventionUpdatableFieldsEquals(returnedConvention, getPersistedConvention(returnedConvention));

        insertedConvention = returnedConvention;
    }

    @Test
    @Transactional
    void createConventionWithExistingId() throws Exception {
        // Create the Convention with an existing ID
        insertedConvention = conventionRepository.saveAndFlush(convention);
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restConventionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conventionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Convention in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        convention.setType(null);

        // Create the Convention, which fails.
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);

        restConventionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conventionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReferenceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        convention.setReference(null);

        // Create the Convention, which fails.
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);

        restConventionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conventionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSignedOnIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        convention.setSignedOn(null);

        // Create the Convention, which fails.
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);

        restConventionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conventionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDocumentIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        convention.setDocumentId(null);

        // Create the Convention, which fails.
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);

        restConventionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conventionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllConventions() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList
        restConventionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(convention.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].signedOn").value(hasItem(DEFAULT_SIGNED_ON.toString())))
            .andExpect(jsonPath("$.[*].expiresOn").value(hasItem(DEFAULT_EXPIRES_ON.toString())))
            .andExpect(jsonPath("$.[*].documentId").value(hasItem(DEFAULT_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].documentSha256").value(hasItem(DEFAULT_DOCUMENT_SHA_256)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllConventionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(conventionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restConventionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(conventionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllConventionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(conventionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restConventionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(conventionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getConvention() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get the convention
        restConventionMockMvc
            .perform(get(ENTITY_API_URL_ID, convention.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(convention.getId().toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.signedOn").value(DEFAULT_SIGNED_ON.toString()))
            .andExpect(jsonPath("$.expiresOn").value(DEFAULT_EXPIRES_ON.toString()))
            .andExpect(jsonPath("$.documentId").value(DEFAULT_DOCUMENT_ID.toString()))
            .andExpect(jsonPath("$.documentSha256").value(DEFAULT_DOCUMENT_SHA_256));
    }

    @Test
    @Transactional
    void getConventionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        UUID id = convention.getId();

        defaultConventionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllConventionsByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where type equals to
        defaultConventionFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllConventionsByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where type in
        defaultConventionFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllConventionsByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where type is not null
        defaultConventionFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllConventionsByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where reference equals to
        defaultConventionFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllConventionsByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where reference in
        defaultConventionFiltering("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE, "reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllConventionsByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where reference is not null
        defaultConventionFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllConventionsByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where reference contains
        defaultConventionFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllConventionsByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where reference does not contain
        defaultConventionFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
    }

    @Test
    @Transactional
    void getAllConventionsBySignedOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where signedOn equals to
        defaultConventionFiltering("signedOn.equals=" + DEFAULT_SIGNED_ON, "signedOn.equals=" + UPDATED_SIGNED_ON);
    }

    @Test
    @Transactional
    void getAllConventionsBySignedOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where signedOn in
        defaultConventionFiltering("signedOn.in=" + DEFAULT_SIGNED_ON + "," + UPDATED_SIGNED_ON, "signedOn.in=" + UPDATED_SIGNED_ON);
    }

    @Test
    @Transactional
    void getAllConventionsBySignedOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where signedOn is not null
        defaultConventionFiltering("signedOn.specified=true", "signedOn.specified=false");
    }

    @Test
    @Transactional
    void getAllConventionsBySignedOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where signedOn is greater than or equal to
        defaultConventionFiltering("signedOn.greaterThanOrEqual=" + DEFAULT_SIGNED_ON, "signedOn.greaterThanOrEqual=" + UPDATED_SIGNED_ON);
    }

    @Test
    @Transactional
    void getAllConventionsBySignedOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where signedOn is less than or equal to
        defaultConventionFiltering("signedOn.lessThanOrEqual=" + DEFAULT_SIGNED_ON, "signedOn.lessThanOrEqual=" + SMALLER_SIGNED_ON);
    }

    @Test
    @Transactional
    void getAllConventionsBySignedOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where signedOn is less than
        defaultConventionFiltering("signedOn.lessThan=" + UPDATED_SIGNED_ON, "signedOn.lessThan=" + DEFAULT_SIGNED_ON);
    }

    @Test
    @Transactional
    void getAllConventionsBySignedOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where signedOn is greater than
        defaultConventionFiltering("signedOn.greaterThan=" + SMALLER_SIGNED_ON, "signedOn.greaterThan=" + DEFAULT_SIGNED_ON);
    }

    @Test
    @Transactional
    void getAllConventionsByExpiresOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where expiresOn equals to
        defaultConventionFiltering("expiresOn.equals=" + DEFAULT_EXPIRES_ON, "expiresOn.equals=" + UPDATED_EXPIRES_ON);
    }

    @Test
    @Transactional
    void getAllConventionsByExpiresOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where expiresOn in
        defaultConventionFiltering("expiresOn.in=" + DEFAULT_EXPIRES_ON + "," + UPDATED_EXPIRES_ON, "expiresOn.in=" + UPDATED_EXPIRES_ON);
    }

    @Test
    @Transactional
    void getAllConventionsByExpiresOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where expiresOn is not null
        defaultConventionFiltering("expiresOn.specified=true", "expiresOn.specified=false");
    }

    @Test
    @Transactional
    void getAllConventionsByExpiresOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where expiresOn is greater than or equal to
        defaultConventionFiltering(
            "expiresOn.greaterThanOrEqual=" + DEFAULT_EXPIRES_ON,
            "expiresOn.greaterThanOrEqual=" + UPDATED_EXPIRES_ON
        );
    }

    @Test
    @Transactional
    void getAllConventionsByExpiresOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where expiresOn is less than or equal to
        defaultConventionFiltering("expiresOn.lessThanOrEqual=" + DEFAULT_EXPIRES_ON, "expiresOn.lessThanOrEqual=" + SMALLER_EXPIRES_ON);
    }

    @Test
    @Transactional
    void getAllConventionsByExpiresOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where expiresOn is less than
        defaultConventionFiltering("expiresOn.lessThan=" + UPDATED_EXPIRES_ON, "expiresOn.lessThan=" + DEFAULT_EXPIRES_ON);
    }

    @Test
    @Transactional
    void getAllConventionsByExpiresOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where expiresOn is greater than
        defaultConventionFiltering("expiresOn.greaterThan=" + SMALLER_EXPIRES_ON, "expiresOn.greaterThan=" + DEFAULT_EXPIRES_ON);
    }

    @Test
    @Transactional
    void getAllConventionsByDocumentIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where documentId equals to
        defaultConventionFiltering("documentId.equals=" + DEFAULT_DOCUMENT_ID, "documentId.equals=" + UPDATED_DOCUMENT_ID);
    }

    @Test
    @Transactional
    void getAllConventionsByDocumentIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where documentId in
        defaultConventionFiltering(
            "documentId.in=" + DEFAULT_DOCUMENT_ID + "," + UPDATED_DOCUMENT_ID,
            "documentId.in=" + UPDATED_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllConventionsByDocumentIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where documentId is not null
        defaultConventionFiltering("documentId.specified=true", "documentId.specified=false");
    }

    @Test
    @Transactional
    void getAllConventionsByDocumentSha256IsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where documentSha256 equals to
        defaultConventionFiltering(
            "documentSha256.equals=" + DEFAULT_DOCUMENT_SHA_256,
            "documentSha256.equals=" + UPDATED_DOCUMENT_SHA_256
        );
    }

    @Test
    @Transactional
    void getAllConventionsByDocumentSha256IsInShouldWork() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where documentSha256 in
        defaultConventionFiltering(
            "documentSha256.in=" + DEFAULT_DOCUMENT_SHA_256 + "," + UPDATED_DOCUMENT_SHA_256,
            "documentSha256.in=" + UPDATED_DOCUMENT_SHA_256
        );
    }

    @Test
    @Transactional
    void getAllConventionsByDocumentSha256IsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where documentSha256 is not null
        defaultConventionFiltering("documentSha256.specified=true", "documentSha256.specified=false");
    }

    @Test
    @Transactional
    void getAllConventionsByDocumentSha256ContainsSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where documentSha256 contains
        defaultConventionFiltering(
            "documentSha256.contains=" + DEFAULT_DOCUMENT_SHA_256,
            "documentSha256.contains=" + UPDATED_DOCUMENT_SHA_256
        );
    }

    @Test
    @Transactional
    void getAllConventionsByDocumentSha256NotContainsSomething() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        // Get all the conventionList where documentSha256 does not contain
        defaultConventionFiltering(
            "documentSha256.doesNotContain=" + UPDATED_DOCUMENT_SHA_256,
            "documentSha256.doesNotContain=" + DEFAULT_DOCUMENT_SHA_256
        );
    }

    @Test
    @Transactional
    void getAllConventionsByInstitutionIsEqualToSomething() throws Exception {
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            conventionRepository.saveAndFlush(convention);
            institution = InstitutionResourceIT.createEntity();
        } else {
            institution = TestUtil.findAll(em, Institution.class).get(0);
        }
        em.persist(institution);
        em.flush();
        convention.setInstitution(institution);
        conventionRepository.saveAndFlush(convention);
        UUID institutionId = institution.getId();
        // Get all the conventionList where institution equals to institutionId
        defaultConventionShouldBeFound("institutionId.equals=" + institutionId);

        // Get all the conventionList where institution equals to UUID.randomUUID()
        defaultConventionShouldNotBeFound("institutionId.equals=" + UUID.randomUUID());
    }

    private void defaultConventionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultConventionShouldBeFound(shouldBeFound);
        defaultConventionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultConventionShouldBeFound(String filter) throws Exception {
        restConventionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(convention.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].signedOn").value(hasItem(DEFAULT_SIGNED_ON.toString())))
            .andExpect(jsonPath("$.[*].expiresOn").value(hasItem(DEFAULT_EXPIRES_ON.toString())))
            .andExpect(jsonPath("$.[*].documentId").value(hasItem(DEFAULT_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].documentSha256").value(hasItem(DEFAULT_DOCUMENT_SHA_256)));

        // Check, that the count call also returns 1
        restConventionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultConventionShouldNotBeFound(String filter) throws Exception {
        restConventionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restConventionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingConvention() throws Exception {
        // Get the convention
        restConventionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingConvention() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the convention
        Convention updatedConvention = conventionRepository.findById(convention.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedConvention are not directly saved in db
        em.detach(updatedConvention);
        updatedConvention
            .type(UPDATED_TYPE)
            .reference(UPDATED_REFERENCE)
            .signedOn(UPDATED_SIGNED_ON)
            .expiresOn(UPDATED_EXPIRES_ON)
            .documentId(UPDATED_DOCUMENT_ID)
            .documentSha256(UPDATED_DOCUMENT_SHA_256);
        ConventionDTO conventionDTO = conventionMapper.toDto(updatedConvention);

        restConventionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, conventionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(conventionDTO))
            )
            .andExpect(status().isOk());

        // Validate the Convention in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedConventionToMatchAllProperties(updatedConvention);
    }

    @Test
    @Transactional
    void putNonExistingConvention() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        convention.setId(UUID.randomUUID());

        // Create the Convention
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restConventionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, conventionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(conventionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Convention in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchConvention() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        convention.setId(UUID.randomUUID());

        // Create the Convention
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConventionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(conventionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Convention in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamConvention() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        convention.setId(UUID.randomUUID());

        // Create the Convention
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConventionMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conventionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Convention in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateConventionWithPatch() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the convention using partial update
        Convention partialUpdatedConvention = new Convention();
        partialUpdatedConvention.setId(convention.getId());

        partialUpdatedConvention
            .type(UPDATED_TYPE)
            .reference(UPDATED_REFERENCE)
            .expiresOn(UPDATED_EXPIRES_ON)
            .documentId(UPDATED_DOCUMENT_ID)
            .documentSha256(UPDATED_DOCUMENT_SHA_256);

        restConventionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedConvention.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedConvention))
            )
            .andExpect(status().isOk());

        // Validate the Convention in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertConventionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedConvention, convention),
            getPersistedConvention(convention)
        );
    }

    @Test
    @Transactional
    void fullUpdateConventionWithPatch() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the convention using partial update
        Convention partialUpdatedConvention = new Convention();
        partialUpdatedConvention.setId(convention.getId());

        partialUpdatedConvention
            .type(UPDATED_TYPE)
            .reference(UPDATED_REFERENCE)
            .signedOn(UPDATED_SIGNED_ON)
            .expiresOn(UPDATED_EXPIRES_ON)
            .documentId(UPDATED_DOCUMENT_ID)
            .documentSha256(UPDATED_DOCUMENT_SHA_256);

        restConventionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedConvention.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedConvention))
            )
            .andExpect(status().isOk());

        // Validate the Convention in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertConventionUpdatableFieldsEquals(partialUpdatedConvention, getPersistedConvention(partialUpdatedConvention));
    }

    @Test
    @Transactional
    void patchNonExistingConvention() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        convention.setId(UUID.randomUUID());

        // Create the Convention
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restConventionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, conventionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(conventionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Convention in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchConvention() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        convention.setId(UUID.randomUUID());

        // Create the Convention
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConventionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(conventionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Convention in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamConvention() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        convention.setId(UUID.randomUUID());

        // Create the Convention
        ConventionDTO conventionDTO = conventionMapper.toDto(convention);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConventionMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(conventionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Convention in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteConvention() throws Exception {
        // Initialize the database
        insertedConvention = conventionRepository.saveAndFlush(convention);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the convention
        restConventionMockMvc
            .perform(delete(ENTITY_API_URL_ID, convention.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return conventionRepository.count();
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

    protected Convention getPersistedConvention(Convention convention) {
        return conventionRepository.findById(convention.getId()).orElseThrow();
    }

    protected void assertPersistedConventionToMatchAllProperties(Convention expectedConvention) {
        assertConventionAllPropertiesEquals(expectedConvention, getPersistedConvention(expectedConvention));
    }

    protected void assertPersistedConventionToMatchUpdatableProperties(Convention expectedConvention) {
        assertConventionAllUpdatablePropertiesEquals(expectedConvention, getPersistedConvention(expectedConvention));
    }
}
