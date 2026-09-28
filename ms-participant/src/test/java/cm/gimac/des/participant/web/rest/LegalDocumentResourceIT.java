package cm.gimac.des.participant.web.rest;

import static cm.gimac.des.participant.domain.LegalDocumentAsserts.*;
import static cm.gimac.des.participant.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.participant.IntegrationTest;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.LegalDocument;
import cm.gimac.des.participant.domain.LegalDocument;
import cm.gimac.des.participant.domain.enumeration.LegalDocumentState;
import cm.gimac.des.participant.domain.enumeration.LegalDocumentType;
import cm.gimac.des.participant.repository.LegalDocumentRepository;
import cm.gimac.des.participant.service.LegalDocumentService;
import cm.gimac.des.participant.service.dto.LegalDocumentDTO;
import cm.gimac.des.participant.service.mapper.LegalDocumentMapper;
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
 * Integration tests for the {@link LegalDocumentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class LegalDocumentResourceIT {

    private static final LegalDocumentType DEFAULT_TYPE = LegalDocumentType.AGREMENT;
    private static final LegalDocumentType UPDATED_TYPE = LegalDocumentType.ARRETE;

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_ISSUING_AUTHORITY = "AAAAAAAAAA";
    private static final String UPDATED_ISSUING_AUTHORITY = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_ISSUED_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_ISSUED_ON = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_ISSUED_ON = LocalDate.ofEpochDay(-1L);

    private static final LocalDate DEFAULT_EXPIRES_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_EXPIRES_ON = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_EXPIRES_ON = LocalDate.ofEpochDay(-1L);

    private static final UUID DEFAULT_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_ID = UUID.randomUUID();

    private static final Integer DEFAULT_DOCUMENT_VERSION = 1;
    private static final Integer UPDATED_DOCUMENT_VERSION = 2;
    private static final Integer SMALLER_DOCUMENT_VERSION = 1 - 1;

    private static final String DEFAULT_DIGEST = "dd39e57c008f6c990ea1fb6409e593ed756b2b40f4fdc43fa0935d7d96f35130";
    private static final String UPDATED_DIGEST = "aa614a91dbc76283b0a6115d6a8db5636d7ffe4d7e325d906c116dd55e017ea9";

    private static final Boolean DEFAULT_SEALED = false;
    private static final Boolean UPDATED_SEALED = true;

    private static final LegalDocumentState DEFAULT_STATE = LegalDocumentState.EN_ATTENTE_SCELLEMENT;
    private static final LegalDocumentState UPDATED_STATE = LegalDocumentState.VALIDE;

    private static final String ENTITY_API_URL = "/api/legal-documents";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private LegalDocumentRepository legalDocumentRepository;

    @Mock
    private LegalDocumentRepository legalDocumentRepositoryMock;

    @Autowired
    private LegalDocumentMapper legalDocumentMapper;

    @Mock
    private LegalDocumentService legalDocumentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restLegalDocumentMockMvc;

    private LegalDocument legalDocument;

    private LegalDocument insertedLegalDocument;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static LegalDocument createEntity(EntityManager em) {
        LegalDocument legalDocument = new LegalDocument()
            .type(DEFAULT_TYPE)
            .reference(DEFAULT_REFERENCE)
            .issuingAuthority(DEFAULT_ISSUING_AUTHORITY)
            .issuedOn(DEFAULT_ISSUED_ON)
            .expiresOn(DEFAULT_EXPIRES_ON)
            .documentId(DEFAULT_DOCUMENT_ID)
            .documentVersion(DEFAULT_DOCUMENT_VERSION)
            .digest(DEFAULT_DIGEST)
            .sealed(DEFAULT_SEALED)
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
        legalDocument.setInstitution(institution);
        return legalDocument;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static LegalDocument createUpdatedEntity(EntityManager em) {
        LegalDocument updatedLegalDocument = new LegalDocument()
            .type(UPDATED_TYPE)
            .reference(UPDATED_REFERENCE)
            .issuingAuthority(UPDATED_ISSUING_AUTHORITY)
            .issuedOn(UPDATED_ISSUED_ON)
            .expiresOn(UPDATED_EXPIRES_ON)
            .documentId(UPDATED_DOCUMENT_ID)
            .documentVersion(UPDATED_DOCUMENT_VERSION)
            .digest(UPDATED_DIGEST)
            .sealed(UPDATED_SEALED)
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
        updatedLegalDocument.setInstitution(institution);
        return updatedLegalDocument;
    }

    @BeforeEach
    void initTest() {
        legalDocument = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedLegalDocument != null) {
            legalDocumentRepository.delete(insertedLegalDocument);
            insertedLegalDocument = null;
        }
    }

    @Test
    @Transactional
    void createLegalDocument() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the LegalDocument
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);
        var returnedLegalDocumentDTO = om.readValue(
            restLegalDocumentMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(legalDocumentDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            LegalDocumentDTO.class
        );

        // Validate the LegalDocument in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedLegalDocument = legalDocumentMapper.toEntity(returnedLegalDocumentDTO);
        assertLegalDocumentUpdatableFieldsEquals(returnedLegalDocument, getPersistedLegalDocument(returnedLegalDocument));

        insertedLegalDocument = returnedLegalDocument;
    }

    @Test
    @Transactional
    void createLegalDocumentWithExistingId() throws Exception {
        // Create the LegalDocument with an existing ID
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restLegalDocumentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the LegalDocument in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        legalDocument.setType(null);

        // Create the LegalDocument, which fails.
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        restLegalDocumentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReferenceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        legalDocument.setReference(null);

        // Create the LegalDocument, which fails.
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        restLegalDocumentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkIssuedOnIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        legalDocument.setIssuedOn(null);

        // Create the LegalDocument, which fails.
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        restLegalDocumentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkExpiresOnIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        legalDocument.setExpiresOn(null);

        // Create the LegalDocument, which fails.
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        restLegalDocumentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDocumentIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        legalDocument.setDocumentId(null);

        // Create the LegalDocument, which fails.
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        restLegalDocumentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSealedIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        legalDocument.setSealed(null);

        // Create the LegalDocument, which fails.
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        restLegalDocumentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        legalDocument.setState(null);

        // Create the LegalDocument, which fails.
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        restLegalDocumentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllLegalDocuments() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList
        restLegalDocumentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(legalDocument.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].issuingAuthority").value(hasItem(DEFAULT_ISSUING_AUTHORITY)))
            .andExpect(jsonPath("$.[*].issuedOn").value(hasItem(DEFAULT_ISSUED_ON.toString())))
            .andExpect(jsonPath("$.[*].expiresOn").value(hasItem(DEFAULT_EXPIRES_ON.toString())))
            .andExpect(jsonPath("$.[*].documentId").value(hasItem(DEFAULT_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].documentVersion").value(hasItem(DEFAULT_DOCUMENT_VERSION)))
            .andExpect(jsonPath("$.[*].digest").value(hasItem(DEFAULT_DIGEST)))
            .andExpect(jsonPath("$.[*].sealed").value(hasItem(DEFAULT_SEALED)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLegalDocumentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(legalDocumentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restLegalDocumentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(legalDocumentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLegalDocumentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(legalDocumentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restLegalDocumentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(legalDocumentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getLegalDocument() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get the legalDocument
        restLegalDocumentMockMvc
            .perform(get(ENTITY_API_URL_ID, legalDocument.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(legalDocument.getId().toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.issuingAuthority").value(DEFAULT_ISSUING_AUTHORITY))
            .andExpect(jsonPath("$.issuedOn").value(DEFAULT_ISSUED_ON.toString()))
            .andExpect(jsonPath("$.expiresOn").value(DEFAULT_EXPIRES_ON.toString()))
            .andExpect(jsonPath("$.documentId").value(DEFAULT_DOCUMENT_ID.toString()))
            .andExpect(jsonPath("$.documentVersion").value(DEFAULT_DOCUMENT_VERSION))
            .andExpect(jsonPath("$.digest").value(DEFAULT_DIGEST))
            .andExpect(jsonPath("$.sealed").value(DEFAULT_SEALED))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()));
    }

    @Test
    @Transactional
    void getLegalDocumentsByIdFiltering() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        UUID id = legalDocument.getId();

        defaultLegalDocumentFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where type equals to
        defaultLegalDocumentFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where type in
        defaultLegalDocumentFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where type is not null
        defaultLegalDocumentFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where reference equals to
        defaultLegalDocumentFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where reference in
        defaultLegalDocumentFiltering("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE, "reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where reference is not null
        defaultLegalDocumentFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where reference contains
        defaultLegalDocumentFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where reference does not contain
        defaultLegalDocumentFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuingAuthorityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuingAuthority equals to
        defaultLegalDocumentFiltering(
            "issuingAuthority.equals=" + DEFAULT_ISSUING_AUTHORITY,
            "issuingAuthority.equals=" + UPDATED_ISSUING_AUTHORITY
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuingAuthorityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuingAuthority in
        defaultLegalDocumentFiltering(
            "issuingAuthority.in=" + DEFAULT_ISSUING_AUTHORITY + "," + UPDATED_ISSUING_AUTHORITY,
            "issuingAuthority.in=" + UPDATED_ISSUING_AUTHORITY
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuingAuthorityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuingAuthority is not null
        defaultLegalDocumentFiltering("issuingAuthority.specified=true", "issuingAuthority.specified=false");
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuingAuthorityContainsSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuingAuthority contains
        defaultLegalDocumentFiltering(
            "issuingAuthority.contains=" + DEFAULT_ISSUING_AUTHORITY,
            "issuingAuthority.contains=" + UPDATED_ISSUING_AUTHORITY
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuingAuthorityNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuingAuthority does not contain
        defaultLegalDocumentFiltering(
            "issuingAuthority.doesNotContain=" + UPDATED_ISSUING_AUTHORITY,
            "issuingAuthority.doesNotContain=" + DEFAULT_ISSUING_AUTHORITY
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuedOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuedOn equals to
        defaultLegalDocumentFiltering("issuedOn.equals=" + DEFAULT_ISSUED_ON, "issuedOn.equals=" + UPDATED_ISSUED_ON);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuedOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuedOn in
        defaultLegalDocumentFiltering("issuedOn.in=" + DEFAULT_ISSUED_ON + "," + UPDATED_ISSUED_ON, "issuedOn.in=" + UPDATED_ISSUED_ON);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuedOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuedOn is not null
        defaultLegalDocumentFiltering("issuedOn.specified=true", "issuedOn.specified=false");
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuedOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuedOn is greater than or equal to
        defaultLegalDocumentFiltering(
            "issuedOn.greaterThanOrEqual=" + DEFAULT_ISSUED_ON,
            "issuedOn.greaterThanOrEqual=" + UPDATED_ISSUED_ON
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuedOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuedOn is less than or equal to
        defaultLegalDocumentFiltering("issuedOn.lessThanOrEqual=" + DEFAULT_ISSUED_ON, "issuedOn.lessThanOrEqual=" + SMALLER_ISSUED_ON);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuedOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuedOn is less than
        defaultLegalDocumentFiltering("issuedOn.lessThan=" + UPDATED_ISSUED_ON, "issuedOn.lessThan=" + DEFAULT_ISSUED_ON);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByIssuedOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where issuedOn is greater than
        defaultLegalDocumentFiltering("issuedOn.greaterThan=" + SMALLER_ISSUED_ON, "issuedOn.greaterThan=" + DEFAULT_ISSUED_ON);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByExpiresOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where expiresOn equals to
        defaultLegalDocumentFiltering("expiresOn.equals=" + DEFAULT_EXPIRES_ON, "expiresOn.equals=" + UPDATED_EXPIRES_ON);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByExpiresOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where expiresOn in
        defaultLegalDocumentFiltering(
            "expiresOn.in=" + DEFAULT_EXPIRES_ON + "," + UPDATED_EXPIRES_ON,
            "expiresOn.in=" + UPDATED_EXPIRES_ON
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByExpiresOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where expiresOn is not null
        defaultLegalDocumentFiltering("expiresOn.specified=true", "expiresOn.specified=false");
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByExpiresOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where expiresOn is greater than or equal to
        defaultLegalDocumentFiltering(
            "expiresOn.greaterThanOrEqual=" + DEFAULT_EXPIRES_ON,
            "expiresOn.greaterThanOrEqual=" + UPDATED_EXPIRES_ON
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByExpiresOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where expiresOn is less than or equal to
        defaultLegalDocumentFiltering("expiresOn.lessThanOrEqual=" + DEFAULT_EXPIRES_ON, "expiresOn.lessThanOrEqual=" + SMALLER_EXPIRES_ON);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByExpiresOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where expiresOn is less than
        defaultLegalDocumentFiltering("expiresOn.lessThan=" + UPDATED_EXPIRES_ON, "expiresOn.lessThan=" + DEFAULT_EXPIRES_ON);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByExpiresOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where expiresOn is greater than
        defaultLegalDocumentFiltering("expiresOn.greaterThan=" + SMALLER_EXPIRES_ON, "expiresOn.greaterThan=" + DEFAULT_EXPIRES_ON);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDocumentIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where documentId equals to
        defaultLegalDocumentFiltering("documentId.equals=" + DEFAULT_DOCUMENT_ID, "documentId.equals=" + UPDATED_DOCUMENT_ID);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDocumentIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where documentId in
        defaultLegalDocumentFiltering(
            "documentId.in=" + DEFAULT_DOCUMENT_ID + "," + UPDATED_DOCUMENT_ID,
            "documentId.in=" + UPDATED_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDocumentIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where documentId is not null
        defaultLegalDocumentFiltering("documentId.specified=true", "documentId.specified=false");
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDocumentVersionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where documentVersion equals to
        defaultLegalDocumentFiltering(
            "documentVersion.equals=" + DEFAULT_DOCUMENT_VERSION,
            "documentVersion.equals=" + UPDATED_DOCUMENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDocumentVersionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where documentVersion in
        defaultLegalDocumentFiltering(
            "documentVersion.in=" + DEFAULT_DOCUMENT_VERSION + "," + UPDATED_DOCUMENT_VERSION,
            "documentVersion.in=" + UPDATED_DOCUMENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDocumentVersionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where documentVersion is not null
        defaultLegalDocumentFiltering("documentVersion.specified=true", "documentVersion.specified=false");
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDocumentVersionIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where documentVersion is greater than or equal to
        defaultLegalDocumentFiltering(
            "documentVersion.greaterThanOrEqual=" + DEFAULT_DOCUMENT_VERSION,
            "documentVersion.greaterThanOrEqual=" + UPDATED_DOCUMENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDocumentVersionIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where documentVersion is less than or equal to
        defaultLegalDocumentFiltering(
            "documentVersion.lessThanOrEqual=" + DEFAULT_DOCUMENT_VERSION,
            "documentVersion.lessThanOrEqual=" + SMALLER_DOCUMENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDocumentVersionIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where documentVersion is less than
        defaultLegalDocumentFiltering(
            "documentVersion.lessThan=" + UPDATED_DOCUMENT_VERSION,
            "documentVersion.lessThan=" + DEFAULT_DOCUMENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDocumentVersionIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where documentVersion is greater than
        defaultLegalDocumentFiltering(
            "documentVersion.greaterThan=" + SMALLER_DOCUMENT_VERSION,
            "documentVersion.greaterThan=" + DEFAULT_DOCUMENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where digest equals to
        defaultLegalDocumentFiltering("digest.equals=" + DEFAULT_DIGEST, "digest.equals=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where digest in
        defaultLegalDocumentFiltering("digest.in=" + DEFAULT_DIGEST + "," + UPDATED_DIGEST, "digest.in=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where digest is not null
        defaultLegalDocumentFiltering("digest.specified=true", "digest.specified=false");
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where digest contains
        defaultLegalDocumentFiltering("digest.contains=" + DEFAULT_DIGEST, "digest.contains=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where digest does not contain
        defaultLegalDocumentFiltering("digest.doesNotContain=" + UPDATED_DIGEST, "digest.doesNotContain=" + DEFAULT_DIGEST);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsBySealedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where sealed equals to
        defaultLegalDocumentFiltering("sealed.equals=" + DEFAULT_SEALED, "sealed.equals=" + UPDATED_SEALED);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsBySealedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where sealed in
        defaultLegalDocumentFiltering("sealed.in=" + DEFAULT_SEALED + "," + UPDATED_SEALED, "sealed.in=" + UPDATED_SEALED);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsBySealedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where sealed is not null
        defaultLegalDocumentFiltering("sealed.specified=true", "sealed.specified=false");
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where state equals to
        defaultLegalDocumentFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where state in
        defaultLegalDocumentFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        // Get all the legalDocumentList where state is not null
        defaultLegalDocumentFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByPreviousDocumentIsEqualToSomething() throws Exception {
        LegalDocument previousDocument;
        if (TestUtil.findAll(em, LegalDocument.class).isEmpty()) {
            legalDocumentRepository.saveAndFlush(legalDocument);
            previousDocument = LegalDocumentResourceIT.createEntity(em);
        } else {
            previousDocument = TestUtil.findAll(em, LegalDocument.class).get(0);
        }
        em.persist(previousDocument);
        em.flush();
        legalDocument.setPreviousDocument(previousDocument);
        legalDocumentRepository.saveAndFlush(legalDocument);
        UUID previousDocumentId = previousDocument.getId();
        // Get all the legalDocumentList where previousDocument equals to previousDocumentId
        defaultLegalDocumentShouldBeFound("previousDocumentId.equals=" + previousDocumentId);

        // Get all the legalDocumentList where previousDocument equals to UUID.randomUUID()
        defaultLegalDocumentShouldNotBeFound("previousDocumentId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllLegalDocumentsByInstitutionIsEqualToSomething() throws Exception {
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            legalDocumentRepository.saveAndFlush(legalDocument);
            institution = InstitutionResourceIT.createEntity();
        } else {
            institution = TestUtil.findAll(em, Institution.class).get(0);
        }
        em.persist(institution);
        em.flush();
        legalDocument.setInstitution(institution);
        legalDocumentRepository.saveAndFlush(legalDocument);
        UUID institutionId = institution.getId();
        // Get all the legalDocumentList where institution equals to institutionId
        defaultLegalDocumentShouldBeFound("institutionId.equals=" + institutionId);

        // Get all the legalDocumentList where institution equals to UUID.randomUUID()
        defaultLegalDocumentShouldNotBeFound("institutionId.equals=" + UUID.randomUUID());
    }

    private void defaultLegalDocumentFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultLegalDocumentShouldBeFound(shouldBeFound);
        defaultLegalDocumentShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultLegalDocumentShouldBeFound(String filter) throws Exception {
        restLegalDocumentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(legalDocument.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].issuingAuthority").value(hasItem(DEFAULT_ISSUING_AUTHORITY)))
            .andExpect(jsonPath("$.[*].issuedOn").value(hasItem(DEFAULT_ISSUED_ON.toString())))
            .andExpect(jsonPath("$.[*].expiresOn").value(hasItem(DEFAULT_EXPIRES_ON.toString())))
            .andExpect(jsonPath("$.[*].documentId").value(hasItem(DEFAULT_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].documentVersion").value(hasItem(DEFAULT_DOCUMENT_VERSION)))
            .andExpect(jsonPath("$.[*].digest").value(hasItem(DEFAULT_DIGEST)))
            .andExpect(jsonPath("$.[*].sealed").value(hasItem(DEFAULT_SEALED)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())));

        // Check, that the count call also returns 1
        restLegalDocumentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultLegalDocumentShouldNotBeFound(String filter) throws Exception {
        restLegalDocumentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restLegalDocumentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingLegalDocument() throws Exception {
        // Get the legalDocument
        restLegalDocumentMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingLegalDocument() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the legalDocument
        LegalDocument updatedLegalDocument = legalDocumentRepository.findById(legalDocument.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedLegalDocument are not directly saved in db
        em.detach(updatedLegalDocument);
        updatedLegalDocument
            .type(UPDATED_TYPE)
            .reference(UPDATED_REFERENCE)
            .issuingAuthority(UPDATED_ISSUING_AUTHORITY)
            .issuedOn(UPDATED_ISSUED_ON)
            .expiresOn(UPDATED_EXPIRES_ON)
            .documentId(UPDATED_DOCUMENT_ID)
            .documentVersion(UPDATED_DOCUMENT_VERSION)
            .digest(UPDATED_DIGEST)
            .sealed(UPDATED_SEALED)
            .state(UPDATED_STATE);
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(updatedLegalDocument);

        restLegalDocumentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, legalDocumentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isOk());

        // Validate the LegalDocument in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedLegalDocumentToMatchAllProperties(updatedLegalDocument);
    }

    @Test
    @Transactional
    void putNonExistingLegalDocument() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        legalDocument.setId(UUID.randomUUID());

        // Create the LegalDocument
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLegalDocumentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, legalDocumentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the LegalDocument in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchLegalDocument() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        legalDocument.setId(UUID.randomUUID());

        // Create the LegalDocument
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLegalDocumentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the LegalDocument in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamLegalDocument() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        legalDocument.setId(UUID.randomUUID());

        // Create the LegalDocument
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLegalDocumentMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the LegalDocument in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateLegalDocumentWithPatch() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the legalDocument using partial update
        LegalDocument partialUpdatedLegalDocument = new LegalDocument();
        partialUpdatedLegalDocument.setId(legalDocument.getId());

        partialUpdatedLegalDocument
            .issuingAuthority(UPDATED_ISSUING_AUTHORITY)
            .documentId(UPDATED_DOCUMENT_ID)
            .documentVersion(UPDATED_DOCUMENT_VERSION)
            .sealed(UPDATED_SEALED);

        restLegalDocumentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLegalDocument.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLegalDocument))
            )
            .andExpect(status().isOk());

        // Validate the LegalDocument in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLegalDocumentUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedLegalDocument, legalDocument),
            getPersistedLegalDocument(legalDocument)
        );
    }

    @Test
    @Transactional
    void fullUpdateLegalDocumentWithPatch() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the legalDocument using partial update
        LegalDocument partialUpdatedLegalDocument = new LegalDocument();
        partialUpdatedLegalDocument.setId(legalDocument.getId());

        partialUpdatedLegalDocument
            .type(UPDATED_TYPE)
            .reference(UPDATED_REFERENCE)
            .issuingAuthority(UPDATED_ISSUING_AUTHORITY)
            .issuedOn(UPDATED_ISSUED_ON)
            .expiresOn(UPDATED_EXPIRES_ON)
            .documentId(UPDATED_DOCUMENT_ID)
            .documentVersion(UPDATED_DOCUMENT_VERSION)
            .digest(UPDATED_DIGEST)
            .sealed(UPDATED_SEALED)
            .state(UPDATED_STATE);

        restLegalDocumentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLegalDocument.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLegalDocument))
            )
            .andExpect(status().isOk());

        // Validate the LegalDocument in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLegalDocumentUpdatableFieldsEquals(partialUpdatedLegalDocument, getPersistedLegalDocument(partialUpdatedLegalDocument));
    }

    @Test
    @Transactional
    void patchNonExistingLegalDocument() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        legalDocument.setId(UUID.randomUUID());

        // Create the LegalDocument
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLegalDocumentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, legalDocumentDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the LegalDocument in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchLegalDocument() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        legalDocument.setId(UUID.randomUUID());

        // Create the LegalDocument
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLegalDocumentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the LegalDocument in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamLegalDocument() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        legalDocument.setId(UUID.randomUUID());

        // Create the LegalDocument
        LegalDocumentDTO legalDocumentDTO = legalDocumentMapper.toDto(legalDocument);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLegalDocumentMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(legalDocumentDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the LegalDocument in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteLegalDocument() throws Exception {
        // Initialize the database
        insertedLegalDocument = legalDocumentRepository.saveAndFlush(legalDocument);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the legalDocument
        restLegalDocumentMockMvc
            .perform(delete(ENTITY_API_URL_ID, legalDocument.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return legalDocumentRepository.count();
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

    protected LegalDocument getPersistedLegalDocument(LegalDocument legalDocument) {
        return legalDocumentRepository.findById(legalDocument.getId()).orElseThrow();
    }

    protected void assertPersistedLegalDocumentToMatchAllProperties(LegalDocument expectedLegalDocument) {
        assertLegalDocumentAllPropertiesEquals(expectedLegalDocument, getPersistedLegalDocument(expectedLegalDocument));
    }

    protected void assertPersistedLegalDocumentToMatchUpdatableProperties(LegalDocument expectedLegalDocument) {
        assertLegalDocumentAllUpdatablePropertiesEquals(expectedLegalDocument, getPersistedLegalDocument(expectedLegalDocument));
    }
}
