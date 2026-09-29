package cm.gimac.des.document.web.rest;

import static cm.gimac.des.document.domain.DocumentLineageAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.domain.Document;
import cm.gimac.des.document.domain.DocumentLineage;
import cm.gimac.des.document.domain.enumeration.DocumentType;
import cm.gimac.des.document.repository.DocumentLineageRepository;
import cm.gimac.des.document.service.mapper.DocumentLineageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import cm.gimac.des.document.security.AuthoritiesConstants;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link DocumentLineageResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class DocumentLineageResourceIT {

    private static final DocumentType DEFAULT_TYPE = DocumentType.RAPPORT_DE_QUART;
    private static final DocumentType UPDATED_TYPE = DocumentType.ATTESTATION;

    private static final String DEFAULT_OWNER_SERVICE = "AAAAAAAAAA";
    private static final String UPDATED_OWNER_SERVICE = "BBBBBBBBBB";

    private static final UUID DEFAULT_OWNER_ID = UUID.randomUUID();
    private static final UUID UPDATED_OWNER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final Integer DEFAULT_CURRENT_VERSION = 0;
    private static final Integer UPDATED_CURRENT_VERSION = 1;
    private static final Integer SMALLER_CURRENT_VERSION = 0 - 1;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/document-lineages";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DocumentLineageRepository documentLineageRepository;

    @Autowired
    private DocumentLineageMapper documentLineageMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDocumentLineageMockMvc;

    private DocumentLineage documentLineage;

    private DocumentLineage insertedDocumentLineage;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DocumentLineage createEntity() {
        return new DocumentLineage()
            .type(DEFAULT_TYPE)
            .ownerService(DEFAULT_OWNER_SERVICE)
            .ownerId(DEFAULT_OWNER_ID)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .currentVersion(DEFAULT_CURRENT_VERSION)
            .createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DocumentLineage createUpdatedEntity() {
        return new DocumentLineage()
            .type(UPDATED_TYPE)
            .ownerService(UPDATED_OWNER_SERVICE)
            .ownerId(UPDATED_OWNER_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .currentVersion(UPDATED_CURRENT_VERSION)
            .createdAt(UPDATED_CREATED_AT);
    }

    @BeforeEach
    void initTest() {
        documentLineage = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedDocumentLineage != null) {
            documentLineageRepository.delete(insertedDocumentLineage);
            insertedDocumentLineage = null;
        }
    }

    @Test
    @Transactional
    void getAllDocumentLineages() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList
        restDocumentLineageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(documentLineage.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].ownerService").value(hasItem(DEFAULT_OWNER_SERVICE)))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].currentVersion").value(hasItem(DEFAULT_CURRENT_VERSION)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @Test
    @Transactional
    void getDocumentLineage() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get the documentLineage
        restDocumentLineageMockMvc
            .perform(get(ENTITY_API_URL_ID, documentLineage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(documentLineage.getId().toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.ownerService").value(DEFAULT_OWNER_SERVICE))
            .andExpect(jsonPath("$.ownerId").value(DEFAULT_OWNER_ID.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.currentVersion").value(DEFAULT_CURRENT_VERSION))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getDocumentLineagesByIdFiltering() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        UUID id = documentLineage.getId();

        defaultDocumentLineageFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where type equals to
        defaultDocumentLineageFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where type in
        defaultDocumentLineageFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where type is not null
        defaultDocumentLineageFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByOwnerServiceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where ownerService equals to
        defaultDocumentLineageFiltering("ownerService.equals=" + DEFAULT_OWNER_SERVICE, "ownerService.equals=" + UPDATED_OWNER_SERVICE);
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByOwnerServiceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where ownerService in
        defaultDocumentLineageFiltering(
            "ownerService.in=" + DEFAULT_OWNER_SERVICE + "," + UPDATED_OWNER_SERVICE,
            "ownerService.in=" + UPDATED_OWNER_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByOwnerServiceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where ownerService is not null
        defaultDocumentLineageFiltering("ownerService.specified=true", "ownerService.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByOwnerServiceContainsSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where ownerService contains
        defaultDocumentLineageFiltering("ownerService.contains=" + DEFAULT_OWNER_SERVICE, "ownerService.contains=" + UPDATED_OWNER_SERVICE);
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByOwnerServiceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where ownerService does not contain
        defaultDocumentLineageFiltering(
            "ownerService.doesNotContain=" + UPDATED_OWNER_SERVICE,
            "ownerService.doesNotContain=" + DEFAULT_OWNER_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByOwnerIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where ownerId equals to
        defaultDocumentLineageFiltering("ownerId.equals=" + DEFAULT_OWNER_ID, "ownerId.equals=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByOwnerIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where ownerId in
        defaultDocumentLineageFiltering("ownerId.in=" + DEFAULT_OWNER_ID + "," + UPDATED_OWNER_ID, "ownerId.in=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByOwnerIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where ownerId is not null
        defaultDocumentLineageFiltering("ownerId.specified=true", "ownerId.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where institutionId equals to
        defaultDocumentLineageFiltering("institutionId.equals=" + DEFAULT_INSTITUTION_ID, "institutionId.equals=" + UPDATED_INSTITUTION_ID);
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where institutionId in
        defaultDocumentLineageFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where institutionId is not null
        defaultDocumentLineageFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByCurrentVersionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where currentVersion equals to
        defaultDocumentLineageFiltering(
            "currentVersion.equals=" + DEFAULT_CURRENT_VERSION,
            "currentVersion.equals=" + UPDATED_CURRENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByCurrentVersionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where currentVersion in
        defaultDocumentLineageFiltering(
            "currentVersion.in=" + DEFAULT_CURRENT_VERSION + "," + UPDATED_CURRENT_VERSION,
            "currentVersion.in=" + UPDATED_CURRENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByCurrentVersionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where currentVersion is not null
        defaultDocumentLineageFiltering("currentVersion.specified=true", "currentVersion.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByCurrentVersionIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where currentVersion is greater than or equal to
        defaultDocumentLineageFiltering(
            "currentVersion.greaterThanOrEqual=" + DEFAULT_CURRENT_VERSION,
            "currentVersion.greaterThanOrEqual=" + UPDATED_CURRENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByCurrentVersionIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where currentVersion is less than or equal to
        defaultDocumentLineageFiltering(
            "currentVersion.lessThanOrEqual=" + DEFAULT_CURRENT_VERSION,
            "currentVersion.lessThanOrEqual=" + SMALLER_CURRENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByCurrentVersionIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where currentVersion is less than
        defaultDocumentLineageFiltering(
            "currentVersion.lessThan=" + UPDATED_CURRENT_VERSION,
            "currentVersion.lessThan=" + DEFAULT_CURRENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByCurrentVersionIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where currentVersion is greater than
        defaultDocumentLineageFiltering(
            "currentVersion.greaterThan=" + SMALLER_CURRENT_VERSION,
            "currentVersion.greaterThan=" + DEFAULT_CURRENT_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where createdAt equals to
        defaultDocumentLineageFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where createdAt in
        defaultDocumentLineageFiltering(
            "createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT,
            "createdAt.in=" + UPDATED_CREATED_AT
        );
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentLineage = documentLineageRepository.saveAndFlush(documentLineage);

        // Get all the documentLineageList where createdAt is not null
        defaultDocumentLineageFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentLineagesByCurrentDocumentIsEqualToSomething() throws Exception {
        Document currentDocument;
        if (TestUtil.findAll(em, Document.class).isEmpty()) {
            documentLineageRepository.saveAndFlush(documentLineage);
            currentDocument = DocumentResourceIT.createEntity(em);
        } else {
            currentDocument = TestUtil.findAll(em, Document.class).get(0);
        }
        em.persist(currentDocument);
        em.flush();
        documentLineage.setCurrentDocument(currentDocument);
        documentLineageRepository.saveAndFlush(documentLineage);
        UUID currentDocumentId = currentDocument.getId();
        // Get all the documentLineageList where currentDocument equals to currentDocumentId
        defaultDocumentLineageShouldBeFound("currentDocumentId.equals=" + currentDocumentId);

        // Get all the documentLineageList where currentDocument equals to UUID.randomUUID()
        defaultDocumentLineageShouldNotBeFound("currentDocumentId.equals=" + UUID.randomUUID());
    }

    private void defaultDocumentLineageFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultDocumentLineageShouldBeFound(shouldBeFound);
        defaultDocumentLineageShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultDocumentLineageShouldBeFound(String filter) throws Exception {
        restDocumentLineageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(documentLineage.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].ownerService").value(hasItem(DEFAULT_OWNER_SERVICE)))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].currentVersion").value(hasItem(DEFAULT_CURRENT_VERSION)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));

        // Check, that the count call also returns 1
        restDocumentLineageMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultDocumentLineageShouldNotBeFound(String filter) throws Exception {
        restDocumentLineageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restDocumentLineageMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingDocumentLineage() throws Exception {
        // Get the documentLineage
        restDocumentLineageMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return documentLineageRepository.count();
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

    protected DocumentLineage getPersistedDocumentLineage(DocumentLineage documentLineage) {
        return documentLineageRepository.findById(documentLineage.getId()).orElseThrow();
    }

    protected void assertPersistedDocumentLineageToMatchAllProperties(DocumentLineage expectedDocumentLineage) {
        assertDocumentLineageAllPropertiesEquals(expectedDocumentLineage, getPersistedDocumentLineage(expectedDocumentLineage));
    }

    protected void assertPersistedDocumentLineageToMatchUpdatableProperties(DocumentLineage expectedDocumentLineage) {
        assertDocumentLineageAllUpdatablePropertiesEquals(expectedDocumentLineage, getPersistedDocumentLineage(expectedDocumentLineage));
    }
}
