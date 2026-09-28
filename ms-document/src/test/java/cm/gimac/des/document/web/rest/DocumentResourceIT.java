package cm.gimac.des.document.web.rest;

import static cm.gimac.des.document.domain.DocumentAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.domain.Document;
import cm.gimac.des.document.domain.DocumentLineage;
import cm.gimac.des.document.domain.DocumentTypePolicy;
import cm.gimac.des.document.domain.enumeration.DocumentState;
import cm.gimac.des.document.domain.enumeration.DocumentType;
import cm.gimac.des.document.domain.enumeration.DocumentVisibility;
import cm.gimac.des.document.domain.enumeration.RetentionMode;
import cm.gimac.des.document.repository.DocumentRepository;
import cm.gimac.des.document.service.mapper.DocumentMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link DocumentResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DocumentResourceIT {

    private static final DocumentType DEFAULT_TYPE = DocumentType.RAPPORT_DE_QUART;
    private static final DocumentType UPDATED_TYPE = DocumentType.ATTESTATION;

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final Integer DEFAULT_VERSION = 1;
    private static final Integer UPDATED_VERSION = 2;
    private static final Integer SMALLER_VERSION = 1 - 1;

    private static final UUID DEFAULT_AUTHOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_AUTHOR_ID = UUID.randomUUID();

    private static final String DEFAULT_CALLER_SERVICE = "AAAAAAAAAA";
    private static final String UPDATED_CALLER_SERVICE = "BBBBBBBBBB";

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final DocumentVisibility DEFAULT_VISIBILITY = DocumentVisibility.TOUTES_INSTITUTIONS;
    private static final DocumentVisibility UPDATED_VISIBILITY = DocumentVisibility.UNE_INSTITUTION;

    private static final String DEFAULT_OWNER_UNIT = "AAAAAAAAAA";
    private static final String UPDATED_OWNER_UNIT = "BBBBBBBBBB";

    private static final String DEFAULT_RESTRICTED_READER_IDS = "AAAAAAAAAA";
    private static final String UPDATED_RESTRICTED_READER_IDS = "BBBBBBBBBB";

    private static final Instant DEFAULT_DEPOSITED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DEPOSITED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_SHA_256 = "2ba77f32dcf6a1887b09b58a54df142407980a14a6008501809514d9299ea047";
    private static final String UPDATED_SHA_256 = "d05b35fe59a3d749378d3a967700577ffe019a59fe7ecb97fd1d4ae55c2308cf";

    private static final Long DEFAULT_SIZE_BYTES = 0L;
    private static final Long UPDATED_SIZE_BYTES = 1L;
    private static final Long SMALLER_SIZE_BYTES = 0L - 1L;

    private static final String DEFAULT_MEDIA_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_MEDIA_TYPE = "BBBBBBBBBB";

    private static final String DEFAULT_OBJECT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_OBJECT_KEY = "BBBBBBBBBB";

    private static final DocumentState DEFAULT_STATE = DocumentState.RECU;
    private static final DocumentState UPDATED_STATE = DocumentState.EN_QUARANTAINE;

    private static final String DEFAULT_RETENTION_PERIOD = "P5D";
    private static final String UPDATED_RETENTION_PERIOD = "P9Y003M";

    private static final RetentionMode DEFAULT_RETENTION_MODE = RetentionMode.CONFORMITE;
    private static final RetentionMode UPDATED_RETENTION_MODE = RetentionMode.GOUVERNANCE;

    private static final Boolean DEFAULT_LEGAL_HOLD = false;
    private static final Boolean UPDATED_LEGAL_HOLD = true;

    private static final Instant DEFAULT_RETAIN_UNTIL = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RETAIN_UNTIL = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LOCKED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LOCKED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_SCAN_CLEAN = false;
    private static final Boolean UPDATED_SCAN_CLEAN = true;

    private static final String DEFAULT_SCAN_SIGNATURE_NAME = "AAAAAAAAAA";
    private static final String UPDATED_SCAN_SIGNATURE_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_SCAN_ENGINE_VERSION = "AAAAAAAAAA";
    private static final String UPDATED_SCAN_ENGINE_VERSION = "BBBBBBBBBB";

    private static final Instant DEFAULT_SCANNED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SCANNED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_MASKING_CANDIDATES = 0;
    private static final Integer UPDATED_MASKING_CANDIDATES = 1;
    private static final Integer SMALLER_MASKING_CANDIDATES = 0 - 1;

    private static final Integer DEFAULT_MASKING_LUHN_VALID = 0;
    private static final Integer UPDATED_MASKING_LUHN_VALID = 1;
    private static final Integer SMALLER_MASKING_LUHN_VALID = 0 - 1;

    private static final Integer DEFAULT_MASKING_MASKED = 0;
    private static final Integer UPDATED_MASKING_MASKED = 1;
    private static final Integer SMALLER_MASKING_MASKED = 0 - 1;

    private static final Boolean DEFAULT_MASKING_REJECTED = false;
    private static final Boolean UPDATED_MASKING_REJECTED = true;

    private static final Boolean DEFAULT_PDFA_COMPLIANT = false;
    private static final Boolean UPDATED_PDFA_COMPLIANT = true;

    private static final String DEFAULT_REJECTION_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REJECTION_REASON = "BBBBBBBBBB";

    private static final Instant DEFAULT_SEALED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SEALED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_PURGED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PURGED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/documents";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDocumentMockMvc;

    private Document document;

    private Document insertedDocument;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Document createEntity(EntityManager em) {
        Document document = new Document()
            .type(DEFAULT_TYPE)
            .title(DEFAULT_TITLE)
            .version(DEFAULT_VERSION)
            .authorId(DEFAULT_AUTHOR_ID)
            .callerService(DEFAULT_CALLER_SERVICE)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .visibility(DEFAULT_VISIBILITY)
            .ownerUnit(DEFAULT_OWNER_UNIT)
            .restrictedReaderIds(DEFAULT_RESTRICTED_READER_IDS)
            .depositedAt(DEFAULT_DEPOSITED_AT)
            .sha256(DEFAULT_SHA_256)
            .sizeBytes(DEFAULT_SIZE_BYTES)
            .mediaType(DEFAULT_MEDIA_TYPE)
            .objectKey(DEFAULT_OBJECT_KEY)
            .state(DEFAULT_STATE)
            .retentionPeriod(DEFAULT_RETENTION_PERIOD)
            .retentionMode(DEFAULT_RETENTION_MODE)
            .legalHold(DEFAULT_LEGAL_HOLD)
            .retainUntil(DEFAULT_RETAIN_UNTIL)
            .lockedAt(DEFAULT_LOCKED_AT)
            .scanClean(DEFAULT_SCAN_CLEAN)
            .scanSignatureName(DEFAULT_SCAN_SIGNATURE_NAME)
            .scanEngineVersion(DEFAULT_SCAN_ENGINE_VERSION)
            .scannedAt(DEFAULT_SCANNED_AT)
            .maskingCandidates(DEFAULT_MASKING_CANDIDATES)
            .maskingLuhnValid(DEFAULT_MASKING_LUHN_VALID)
            .maskingMasked(DEFAULT_MASKING_MASKED)
            .maskingRejected(DEFAULT_MASKING_REJECTED)
            .pdfaCompliant(DEFAULT_PDFA_COMPLIANT)
            .rejectionReason(DEFAULT_REJECTION_REASON)
            .sealedAt(DEFAULT_SEALED_AT)
            .purgedAt(DEFAULT_PURGED_AT);
        // Add required entity
        DocumentLineage documentLineage;
        if (TestUtil.findAll(em, DocumentLineage.class).isEmpty()) {
            documentLineage = DocumentLineageResourceIT.createEntity();
            em.persist(documentLineage);
            em.flush();
        } else {
            documentLineage = TestUtil.findAll(em, DocumentLineage.class).get(0);
        }
        document.setLineage(documentLineage);
        // Add required entity
        DocumentTypePolicy documentTypePolicy;
        if (TestUtil.findAll(em, DocumentTypePolicy.class).isEmpty()) {
            documentTypePolicy = DocumentTypePolicyResourceIT.createEntity();
            em.persist(documentTypePolicy);
            em.flush();
        } else {
            documentTypePolicy = TestUtil.findAll(em, DocumentTypePolicy.class).get(0);
        }
        document.setPolicy(documentTypePolicy);
        return document;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Document createUpdatedEntity(EntityManager em) {
        Document updatedDocument = new Document()
            .type(UPDATED_TYPE)
            .title(UPDATED_TITLE)
            .version(UPDATED_VERSION)
            .authorId(UPDATED_AUTHOR_ID)
            .callerService(UPDATED_CALLER_SERVICE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .visibility(UPDATED_VISIBILITY)
            .ownerUnit(UPDATED_OWNER_UNIT)
            .restrictedReaderIds(UPDATED_RESTRICTED_READER_IDS)
            .depositedAt(UPDATED_DEPOSITED_AT)
            .sha256(UPDATED_SHA_256)
            .sizeBytes(UPDATED_SIZE_BYTES)
            .mediaType(UPDATED_MEDIA_TYPE)
            .objectKey(UPDATED_OBJECT_KEY)
            .state(UPDATED_STATE)
            .retentionPeriod(UPDATED_RETENTION_PERIOD)
            .retentionMode(UPDATED_RETENTION_MODE)
            .legalHold(UPDATED_LEGAL_HOLD)
            .retainUntil(UPDATED_RETAIN_UNTIL)
            .lockedAt(UPDATED_LOCKED_AT)
            .scanClean(UPDATED_SCAN_CLEAN)
            .scanSignatureName(UPDATED_SCAN_SIGNATURE_NAME)
            .scanEngineVersion(UPDATED_SCAN_ENGINE_VERSION)
            .scannedAt(UPDATED_SCANNED_AT)
            .maskingCandidates(UPDATED_MASKING_CANDIDATES)
            .maskingLuhnValid(UPDATED_MASKING_LUHN_VALID)
            .maskingMasked(UPDATED_MASKING_MASKED)
            .maskingRejected(UPDATED_MASKING_REJECTED)
            .pdfaCompliant(UPDATED_PDFA_COMPLIANT)
            .rejectionReason(UPDATED_REJECTION_REASON)
            .sealedAt(UPDATED_SEALED_AT)
            .purgedAt(UPDATED_PURGED_AT);
        // Add required entity
        DocumentLineage documentLineage;
        if (TestUtil.findAll(em, DocumentLineage.class).isEmpty()) {
            documentLineage = DocumentLineageResourceIT.createUpdatedEntity();
            em.persist(documentLineage);
            em.flush();
        } else {
            documentLineage = TestUtil.findAll(em, DocumentLineage.class).get(0);
        }
        updatedDocument.setLineage(documentLineage);
        // Add required entity
        DocumentTypePolicy documentTypePolicy;
        if (TestUtil.findAll(em, DocumentTypePolicy.class).isEmpty()) {
            documentTypePolicy = DocumentTypePolicyResourceIT.createUpdatedEntity();
            em.persist(documentTypePolicy);
            em.flush();
        } else {
            documentTypePolicy = TestUtil.findAll(em, DocumentTypePolicy.class).get(0);
        }
        updatedDocument.setPolicy(documentTypePolicy);
        return updatedDocument;
    }

    @BeforeEach
    void initTest() {
        document = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedDocument != null) {
            documentRepository.delete(insertedDocument);
            insertedDocument = null;
        }
    }

    @Test
    @Transactional
    void getAllDocuments() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList
        restDocumentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(document.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION)))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].callerService").value(hasItem(DEFAULT_CALLER_SERVICE)))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].visibility").value(hasItem(DEFAULT_VISIBILITY.toString())))
            .andExpect(jsonPath("$.[*].ownerUnit").value(hasItem(DEFAULT_OWNER_UNIT)))
            .andExpect(jsonPath("$.[*].restrictedReaderIds").value(hasItem(DEFAULT_RESTRICTED_READER_IDS)))
            .andExpect(jsonPath("$.[*].depositedAt").value(hasItem(DEFAULT_DEPOSITED_AT.toString())))
            .andExpect(jsonPath("$.[*].sha256").value(hasItem(DEFAULT_SHA_256)))
            .andExpect(jsonPath("$.[*].sizeBytes").value(hasItem(DEFAULT_SIZE_BYTES.intValue())))
            .andExpect(jsonPath("$.[*].mediaType").value(hasItem(DEFAULT_MEDIA_TYPE)))
            .andExpect(jsonPath("$.[*].objectKey").value(hasItem(DEFAULT_OBJECT_KEY)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].retentionPeriod").value(hasItem(DEFAULT_RETENTION_PERIOD)))
            .andExpect(jsonPath("$.[*].retentionMode").value(hasItem(DEFAULT_RETENTION_MODE.toString())))
            .andExpect(jsonPath("$.[*].legalHold").value(hasItem(DEFAULT_LEGAL_HOLD)))
            .andExpect(jsonPath("$.[*].retainUntil").value(hasItem(DEFAULT_RETAIN_UNTIL.toString())))
            .andExpect(jsonPath("$.[*].lockedAt").value(hasItem(DEFAULT_LOCKED_AT.toString())))
            .andExpect(jsonPath("$.[*].scanClean").value(hasItem(DEFAULT_SCAN_CLEAN)))
            .andExpect(jsonPath("$.[*].scanSignatureName").value(hasItem(DEFAULT_SCAN_SIGNATURE_NAME)))
            .andExpect(jsonPath("$.[*].scanEngineVersion").value(hasItem(DEFAULT_SCAN_ENGINE_VERSION)))
            .andExpect(jsonPath("$.[*].scannedAt").value(hasItem(DEFAULT_SCANNED_AT.toString())))
            .andExpect(jsonPath("$.[*].maskingCandidates").value(hasItem(DEFAULT_MASKING_CANDIDATES)))
            .andExpect(jsonPath("$.[*].maskingLuhnValid").value(hasItem(DEFAULT_MASKING_LUHN_VALID)))
            .andExpect(jsonPath("$.[*].maskingMasked").value(hasItem(DEFAULT_MASKING_MASKED)))
            .andExpect(jsonPath("$.[*].maskingRejected").value(hasItem(DEFAULT_MASKING_REJECTED)))
            .andExpect(jsonPath("$.[*].pdfaCompliant").value(hasItem(DEFAULT_PDFA_COMPLIANT)))
            .andExpect(jsonPath("$.[*].rejectionReason").value(hasItem(DEFAULT_REJECTION_REASON)))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].purgedAt").value(hasItem(DEFAULT_PURGED_AT.toString())));
    }

    @Test
    @Transactional
    void getDocument() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get the document
        restDocumentMockMvc
            .perform(get(ENTITY_API_URL_ID, document.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(document.getId().toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.version").value(DEFAULT_VERSION))
            .andExpect(jsonPath("$.authorId").value(DEFAULT_AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.callerService").value(DEFAULT_CALLER_SERVICE))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.visibility").value(DEFAULT_VISIBILITY.toString()))
            .andExpect(jsonPath("$.ownerUnit").value(DEFAULT_OWNER_UNIT))
            .andExpect(jsonPath("$.restrictedReaderIds").value(DEFAULT_RESTRICTED_READER_IDS))
            .andExpect(jsonPath("$.depositedAt").value(DEFAULT_DEPOSITED_AT.toString()))
            .andExpect(jsonPath("$.sha256").value(DEFAULT_SHA_256))
            .andExpect(jsonPath("$.sizeBytes").value(DEFAULT_SIZE_BYTES.intValue()))
            .andExpect(jsonPath("$.mediaType").value(DEFAULT_MEDIA_TYPE))
            .andExpect(jsonPath("$.objectKey").value(DEFAULT_OBJECT_KEY))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.retentionPeriod").value(DEFAULT_RETENTION_PERIOD))
            .andExpect(jsonPath("$.retentionMode").value(DEFAULT_RETENTION_MODE.toString()))
            .andExpect(jsonPath("$.legalHold").value(DEFAULT_LEGAL_HOLD))
            .andExpect(jsonPath("$.retainUntil").value(DEFAULT_RETAIN_UNTIL.toString()))
            .andExpect(jsonPath("$.lockedAt").value(DEFAULT_LOCKED_AT.toString()))
            .andExpect(jsonPath("$.scanClean").value(DEFAULT_SCAN_CLEAN))
            .andExpect(jsonPath("$.scanSignatureName").value(DEFAULT_SCAN_SIGNATURE_NAME))
            .andExpect(jsonPath("$.scanEngineVersion").value(DEFAULT_SCAN_ENGINE_VERSION))
            .andExpect(jsonPath("$.scannedAt").value(DEFAULT_SCANNED_AT.toString()))
            .andExpect(jsonPath("$.maskingCandidates").value(DEFAULT_MASKING_CANDIDATES))
            .andExpect(jsonPath("$.maskingLuhnValid").value(DEFAULT_MASKING_LUHN_VALID))
            .andExpect(jsonPath("$.maskingMasked").value(DEFAULT_MASKING_MASKED))
            .andExpect(jsonPath("$.maskingRejected").value(DEFAULT_MASKING_REJECTED))
            .andExpect(jsonPath("$.pdfaCompliant").value(DEFAULT_PDFA_COMPLIANT))
            .andExpect(jsonPath("$.rejectionReason").value(DEFAULT_REJECTION_REASON))
            .andExpect(jsonPath("$.sealedAt").value(DEFAULT_SEALED_AT.toString()))
            .andExpect(jsonPath("$.purgedAt").value(DEFAULT_PURGED_AT.toString()));
    }

    @Test
    @Transactional
    void getDocumentsByIdFiltering() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        UUID id = document.getId();

        defaultDocumentFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllDocumentsByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where type equals to
        defaultDocumentFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllDocumentsByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where type in
        defaultDocumentFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllDocumentsByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where type is not null
        defaultDocumentFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where title equals to
        defaultDocumentFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllDocumentsByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where title in
        defaultDocumentFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllDocumentsByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where title is not null
        defaultDocumentFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where title contains
        defaultDocumentFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllDocumentsByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where title does not contain
        defaultDocumentFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllDocumentsByVersionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where version equals to
        defaultDocumentFiltering("version.equals=" + DEFAULT_VERSION, "version.equals=" + UPDATED_VERSION);
    }

    @Test
    @Transactional
    void getAllDocumentsByVersionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where version in
        defaultDocumentFiltering("version.in=" + DEFAULT_VERSION + "," + UPDATED_VERSION, "version.in=" + UPDATED_VERSION);
    }

    @Test
    @Transactional
    void getAllDocumentsByVersionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where version is not null
        defaultDocumentFiltering("version.specified=true", "version.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByVersionIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where version is greater than or equal to
        defaultDocumentFiltering("version.greaterThanOrEqual=" + DEFAULT_VERSION, "version.greaterThanOrEqual=" + UPDATED_VERSION);
    }

    @Test
    @Transactional
    void getAllDocumentsByVersionIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where version is less than or equal to
        defaultDocumentFiltering("version.lessThanOrEqual=" + DEFAULT_VERSION, "version.lessThanOrEqual=" + SMALLER_VERSION);
    }

    @Test
    @Transactional
    void getAllDocumentsByVersionIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where version is less than
        defaultDocumentFiltering("version.lessThan=" + UPDATED_VERSION, "version.lessThan=" + DEFAULT_VERSION);
    }

    @Test
    @Transactional
    void getAllDocumentsByVersionIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where version is greater than
        defaultDocumentFiltering("version.greaterThan=" + SMALLER_VERSION, "version.greaterThan=" + DEFAULT_VERSION);
    }

    @Test
    @Transactional
    void getAllDocumentsByAuthorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where authorId equals to
        defaultDocumentFiltering("authorId.equals=" + DEFAULT_AUTHOR_ID, "authorId.equals=" + UPDATED_AUTHOR_ID);
    }

    @Test
    @Transactional
    void getAllDocumentsByAuthorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where authorId in
        defaultDocumentFiltering("authorId.in=" + DEFAULT_AUTHOR_ID + "," + UPDATED_AUTHOR_ID, "authorId.in=" + UPDATED_AUTHOR_ID);
    }

    @Test
    @Transactional
    void getAllDocumentsByAuthorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where authorId is not null
        defaultDocumentFiltering("authorId.specified=true", "authorId.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByCallerServiceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where callerService equals to
        defaultDocumentFiltering("callerService.equals=" + DEFAULT_CALLER_SERVICE, "callerService.equals=" + UPDATED_CALLER_SERVICE);
    }

    @Test
    @Transactional
    void getAllDocumentsByCallerServiceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where callerService in
        defaultDocumentFiltering(
            "callerService.in=" + DEFAULT_CALLER_SERVICE + "," + UPDATED_CALLER_SERVICE,
            "callerService.in=" + UPDATED_CALLER_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByCallerServiceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where callerService is not null
        defaultDocumentFiltering("callerService.specified=true", "callerService.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByCallerServiceContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where callerService contains
        defaultDocumentFiltering("callerService.contains=" + DEFAULT_CALLER_SERVICE, "callerService.contains=" + UPDATED_CALLER_SERVICE);
    }

    @Test
    @Transactional
    void getAllDocumentsByCallerServiceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where callerService does not contain
        defaultDocumentFiltering(
            "callerService.doesNotContain=" + UPDATED_CALLER_SERVICE,
            "callerService.doesNotContain=" + DEFAULT_CALLER_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where institutionId equals to
        defaultDocumentFiltering("institutionId.equals=" + DEFAULT_INSTITUTION_ID, "institutionId.equals=" + UPDATED_INSTITUTION_ID);
    }

    @Test
    @Transactional
    void getAllDocumentsByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where institutionId in
        defaultDocumentFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where institutionId is not null
        defaultDocumentFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByVisibilityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where visibility equals to
        defaultDocumentFiltering("visibility.equals=" + DEFAULT_VISIBILITY, "visibility.equals=" + UPDATED_VISIBILITY);
    }

    @Test
    @Transactional
    void getAllDocumentsByVisibilityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where visibility in
        defaultDocumentFiltering("visibility.in=" + DEFAULT_VISIBILITY + "," + UPDATED_VISIBILITY, "visibility.in=" + UPDATED_VISIBILITY);
    }

    @Test
    @Transactional
    void getAllDocumentsByVisibilityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where visibility is not null
        defaultDocumentFiltering("visibility.specified=true", "visibility.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByOwnerUnitIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where ownerUnit equals to
        defaultDocumentFiltering("ownerUnit.equals=" + DEFAULT_OWNER_UNIT, "ownerUnit.equals=" + UPDATED_OWNER_UNIT);
    }

    @Test
    @Transactional
    void getAllDocumentsByOwnerUnitIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where ownerUnit in
        defaultDocumentFiltering("ownerUnit.in=" + DEFAULT_OWNER_UNIT + "," + UPDATED_OWNER_UNIT, "ownerUnit.in=" + UPDATED_OWNER_UNIT);
    }

    @Test
    @Transactional
    void getAllDocumentsByOwnerUnitIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where ownerUnit is not null
        defaultDocumentFiltering("ownerUnit.specified=true", "ownerUnit.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByOwnerUnitContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where ownerUnit contains
        defaultDocumentFiltering("ownerUnit.contains=" + DEFAULT_OWNER_UNIT, "ownerUnit.contains=" + UPDATED_OWNER_UNIT);
    }

    @Test
    @Transactional
    void getAllDocumentsByOwnerUnitNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where ownerUnit does not contain
        defaultDocumentFiltering("ownerUnit.doesNotContain=" + UPDATED_OWNER_UNIT, "ownerUnit.doesNotContain=" + DEFAULT_OWNER_UNIT);
    }

    @Test
    @Transactional
    void getAllDocumentsByRestrictedReaderIdsIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where restrictedReaderIds equals to
        defaultDocumentFiltering(
            "restrictedReaderIds.equals=" + DEFAULT_RESTRICTED_READER_IDS,
            "restrictedReaderIds.equals=" + UPDATED_RESTRICTED_READER_IDS
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRestrictedReaderIdsIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where restrictedReaderIds in
        defaultDocumentFiltering(
            "restrictedReaderIds.in=" + DEFAULT_RESTRICTED_READER_IDS + "," + UPDATED_RESTRICTED_READER_IDS,
            "restrictedReaderIds.in=" + UPDATED_RESTRICTED_READER_IDS
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRestrictedReaderIdsIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where restrictedReaderIds is not null
        defaultDocumentFiltering("restrictedReaderIds.specified=true", "restrictedReaderIds.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByRestrictedReaderIdsContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where restrictedReaderIds contains
        defaultDocumentFiltering(
            "restrictedReaderIds.contains=" + DEFAULT_RESTRICTED_READER_IDS,
            "restrictedReaderIds.contains=" + UPDATED_RESTRICTED_READER_IDS
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRestrictedReaderIdsNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where restrictedReaderIds does not contain
        defaultDocumentFiltering(
            "restrictedReaderIds.doesNotContain=" + UPDATED_RESTRICTED_READER_IDS,
            "restrictedReaderIds.doesNotContain=" + DEFAULT_RESTRICTED_READER_IDS
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByDepositedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where depositedAt equals to
        defaultDocumentFiltering("depositedAt.equals=" + DEFAULT_DEPOSITED_AT, "depositedAt.equals=" + UPDATED_DEPOSITED_AT);
    }

    @Test
    @Transactional
    void getAllDocumentsByDepositedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where depositedAt in
        defaultDocumentFiltering(
            "depositedAt.in=" + DEFAULT_DEPOSITED_AT + "," + UPDATED_DEPOSITED_AT,
            "depositedAt.in=" + UPDATED_DEPOSITED_AT
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByDepositedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where depositedAt is not null
        defaultDocumentFiltering("depositedAt.specified=true", "depositedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsBySha256IsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sha256 equals to
        defaultDocumentFiltering("sha256.equals=" + DEFAULT_SHA_256, "sha256.equals=" + UPDATED_SHA_256);
    }

    @Test
    @Transactional
    void getAllDocumentsBySha256IsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sha256 in
        defaultDocumentFiltering("sha256.in=" + DEFAULT_SHA_256 + "," + UPDATED_SHA_256, "sha256.in=" + UPDATED_SHA_256);
    }

    @Test
    @Transactional
    void getAllDocumentsBySha256IsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sha256 is not null
        defaultDocumentFiltering("sha256.specified=true", "sha256.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsBySha256ContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sha256 contains
        defaultDocumentFiltering("sha256.contains=" + DEFAULT_SHA_256, "sha256.contains=" + UPDATED_SHA_256);
    }

    @Test
    @Transactional
    void getAllDocumentsBySha256NotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sha256 does not contain
        defaultDocumentFiltering("sha256.doesNotContain=" + UPDATED_SHA_256, "sha256.doesNotContain=" + DEFAULT_SHA_256);
    }

    @Test
    @Transactional
    void getAllDocumentsBySizeBytesIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sizeBytes equals to
        defaultDocumentFiltering("sizeBytes.equals=" + DEFAULT_SIZE_BYTES, "sizeBytes.equals=" + UPDATED_SIZE_BYTES);
    }

    @Test
    @Transactional
    void getAllDocumentsBySizeBytesIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sizeBytes in
        defaultDocumentFiltering("sizeBytes.in=" + DEFAULT_SIZE_BYTES + "," + UPDATED_SIZE_BYTES, "sizeBytes.in=" + UPDATED_SIZE_BYTES);
    }

    @Test
    @Transactional
    void getAllDocumentsBySizeBytesIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sizeBytes is not null
        defaultDocumentFiltering("sizeBytes.specified=true", "sizeBytes.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsBySizeBytesIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sizeBytes is greater than or equal to
        defaultDocumentFiltering(
            "sizeBytes.greaterThanOrEqual=" + DEFAULT_SIZE_BYTES,
            "sizeBytes.greaterThanOrEqual=" + UPDATED_SIZE_BYTES
        );
    }

    @Test
    @Transactional
    void getAllDocumentsBySizeBytesIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sizeBytes is less than or equal to
        defaultDocumentFiltering("sizeBytes.lessThanOrEqual=" + DEFAULT_SIZE_BYTES, "sizeBytes.lessThanOrEqual=" + SMALLER_SIZE_BYTES);
    }

    @Test
    @Transactional
    void getAllDocumentsBySizeBytesIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sizeBytes is less than
        defaultDocumentFiltering("sizeBytes.lessThan=" + UPDATED_SIZE_BYTES, "sizeBytes.lessThan=" + DEFAULT_SIZE_BYTES);
    }

    @Test
    @Transactional
    void getAllDocumentsBySizeBytesIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sizeBytes is greater than
        defaultDocumentFiltering("sizeBytes.greaterThan=" + SMALLER_SIZE_BYTES, "sizeBytes.greaterThan=" + DEFAULT_SIZE_BYTES);
    }

    @Test
    @Transactional
    void getAllDocumentsByMediaTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where mediaType equals to
        defaultDocumentFiltering("mediaType.equals=" + DEFAULT_MEDIA_TYPE, "mediaType.equals=" + UPDATED_MEDIA_TYPE);
    }

    @Test
    @Transactional
    void getAllDocumentsByMediaTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where mediaType in
        defaultDocumentFiltering("mediaType.in=" + DEFAULT_MEDIA_TYPE + "," + UPDATED_MEDIA_TYPE, "mediaType.in=" + UPDATED_MEDIA_TYPE);
    }

    @Test
    @Transactional
    void getAllDocumentsByMediaTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where mediaType is not null
        defaultDocumentFiltering("mediaType.specified=true", "mediaType.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByMediaTypeContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where mediaType contains
        defaultDocumentFiltering("mediaType.contains=" + DEFAULT_MEDIA_TYPE, "mediaType.contains=" + UPDATED_MEDIA_TYPE);
    }

    @Test
    @Transactional
    void getAllDocumentsByMediaTypeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where mediaType does not contain
        defaultDocumentFiltering("mediaType.doesNotContain=" + UPDATED_MEDIA_TYPE, "mediaType.doesNotContain=" + DEFAULT_MEDIA_TYPE);
    }

    @Test
    @Transactional
    void getAllDocumentsByObjectKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where objectKey equals to
        defaultDocumentFiltering("objectKey.equals=" + DEFAULT_OBJECT_KEY, "objectKey.equals=" + UPDATED_OBJECT_KEY);
    }

    @Test
    @Transactional
    void getAllDocumentsByObjectKeyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where objectKey in
        defaultDocumentFiltering("objectKey.in=" + DEFAULT_OBJECT_KEY + "," + UPDATED_OBJECT_KEY, "objectKey.in=" + UPDATED_OBJECT_KEY);
    }

    @Test
    @Transactional
    void getAllDocumentsByObjectKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where objectKey is not null
        defaultDocumentFiltering("objectKey.specified=true", "objectKey.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByObjectKeyContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where objectKey contains
        defaultDocumentFiltering("objectKey.contains=" + DEFAULT_OBJECT_KEY, "objectKey.contains=" + UPDATED_OBJECT_KEY);
    }

    @Test
    @Transactional
    void getAllDocumentsByObjectKeyNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where objectKey does not contain
        defaultDocumentFiltering("objectKey.doesNotContain=" + UPDATED_OBJECT_KEY, "objectKey.doesNotContain=" + DEFAULT_OBJECT_KEY);
    }

    @Test
    @Transactional
    void getAllDocumentsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where state equals to
        defaultDocumentFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllDocumentsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where state in
        defaultDocumentFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllDocumentsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where state is not null
        defaultDocumentFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByRetentionPeriodIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where retentionPeriod equals to
        defaultDocumentFiltering(
            "retentionPeriod.equals=" + DEFAULT_RETENTION_PERIOD,
            "retentionPeriod.equals=" + UPDATED_RETENTION_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRetentionPeriodIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where retentionPeriod in
        defaultDocumentFiltering(
            "retentionPeriod.in=" + DEFAULT_RETENTION_PERIOD + "," + UPDATED_RETENTION_PERIOD,
            "retentionPeriod.in=" + UPDATED_RETENTION_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRetentionPeriodIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where retentionPeriod is not null
        defaultDocumentFiltering("retentionPeriod.specified=true", "retentionPeriod.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByRetentionPeriodContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where retentionPeriod contains
        defaultDocumentFiltering(
            "retentionPeriod.contains=" + DEFAULT_RETENTION_PERIOD,
            "retentionPeriod.contains=" + UPDATED_RETENTION_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRetentionPeriodNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where retentionPeriod does not contain
        defaultDocumentFiltering(
            "retentionPeriod.doesNotContain=" + UPDATED_RETENTION_PERIOD,
            "retentionPeriod.doesNotContain=" + DEFAULT_RETENTION_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRetentionModeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where retentionMode equals to
        defaultDocumentFiltering("retentionMode.equals=" + DEFAULT_RETENTION_MODE, "retentionMode.equals=" + UPDATED_RETENTION_MODE);
    }

    @Test
    @Transactional
    void getAllDocumentsByRetentionModeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where retentionMode in
        defaultDocumentFiltering(
            "retentionMode.in=" + DEFAULT_RETENTION_MODE + "," + UPDATED_RETENTION_MODE,
            "retentionMode.in=" + UPDATED_RETENTION_MODE
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRetentionModeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where retentionMode is not null
        defaultDocumentFiltering("retentionMode.specified=true", "retentionMode.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByLegalHoldIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where legalHold equals to
        defaultDocumentFiltering("legalHold.equals=" + DEFAULT_LEGAL_HOLD, "legalHold.equals=" + UPDATED_LEGAL_HOLD);
    }

    @Test
    @Transactional
    void getAllDocumentsByLegalHoldIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where legalHold in
        defaultDocumentFiltering("legalHold.in=" + DEFAULT_LEGAL_HOLD + "," + UPDATED_LEGAL_HOLD, "legalHold.in=" + UPDATED_LEGAL_HOLD);
    }

    @Test
    @Transactional
    void getAllDocumentsByLegalHoldIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where legalHold is not null
        defaultDocumentFiltering("legalHold.specified=true", "legalHold.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByRetainUntilIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where retainUntil equals to
        defaultDocumentFiltering("retainUntil.equals=" + DEFAULT_RETAIN_UNTIL, "retainUntil.equals=" + UPDATED_RETAIN_UNTIL);
    }

    @Test
    @Transactional
    void getAllDocumentsByRetainUntilIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where retainUntil in
        defaultDocumentFiltering(
            "retainUntil.in=" + DEFAULT_RETAIN_UNTIL + "," + UPDATED_RETAIN_UNTIL,
            "retainUntil.in=" + UPDATED_RETAIN_UNTIL
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRetainUntilIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where retainUntil is not null
        defaultDocumentFiltering("retainUntil.specified=true", "retainUntil.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByLockedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where lockedAt equals to
        defaultDocumentFiltering("lockedAt.equals=" + DEFAULT_LOCKED_AT, "lockedAt.equals=" + UPDATED_LOCKED_AT);
    }

    @Test
    @Transactional
    void getAllDocumentsByLockedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where lockedAt in
        defaultDocumentFiltering("lockedAt.in=" + DEFAULT_LOCKED_AT + "," + UPDATED_LOCKED_AT, "lockedAt.in=" + UPDATED_LOCKED_AT);
    }

    @Test
    @Transactional
    void getAllDocumentsByLockedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where lockedAt is not null
        defaultDocumentFiltering("lockedAt.specified=true", "lockedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByScanCleanIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanClean equals to
        defaultDocumentFiltering("scanClean.equals=" + DEFAULT_SCAN_CLEAN, "scanClean.equals=" + UPDATED_SCAN_CLEAN);
    }

    @Test
    @Transactional
    void getAllDocumentsByScanCleanIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanClean in
        defaultDocumentFiltering("scanClean.in=" + DEFAULT_SCAN_CLEAN + "," + UPDATED_SCAN_CLEAN, "scanClean.in=" + UPDATED_SCAN_CLEAN);
    }

    @Test
    @Transactional
    void getAllDocumentsByScanCleanIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanClean is not null
        defaultDocumentFiltering("scanClean.specified=true", "scanClean.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByScanSignatureNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanSignatureName equals to
        defaultDocumentFiltering(
            "scanSignatureName.equals=" + DEFAULT_SCAN_SIGNATURE_NAME,
            "scanSignatureName.equals=" + UPDATED_SCAN_SIGNATURE_NAME
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByScanSignatureNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanSignatureName in
        defaultDocumentFiltering(
            "scanSignatureName.in=" + DEFAULT_SCAN_SIGNATURE_NAME + "," + UPDATED_SCAN_SIGNATURE_NAME,
            "scanSignatureName.in=" + UPDATED_SCAN_SIGNATURE_NAME
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByScanSignatureNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanSignatureName is not null
        defaultDocumentFiltering("scanSignatureName.specified=true", "scanSignatureName.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByScanSignatureNameContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanSignatureName contains
        defaultDocumentFiltering(
            "scanSignatureName.contains=" + DEFAULT_SCAN_SIGNATURE_NAME,
            "scanSignatureName.contains=" + UPDATED_SCAN_SIGNATURE_NAME
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByScanSignatureNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanSignatureName does not contain
        defaultDocumentFiltering(
            "scanSignatureName.doesNotContain=" + UPDATED_SCAN_SIGNATURE_NAME,
            "scanSignatureName.doesNotContain=" + DEFAULT_SCAN_SIGNATURE_NAME
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByScanEngineVersionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanEngineVersion equals to
        defaultDocumentFiltering(
            "scanEngineVersion.equals=" + DEFAULT_SCAN_ENGINE_VERSION,
            "scanEngineVersion.equals=" + UPDATED_SCAN_ENGINE_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByScanEngineVersionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanEngineVersion in
        defaultDocumentFiltering(
            "scanEngineVersion.in=" + DEFAULT_SCAN_ENGINE_VERSION + "," + UPDATED_SCAN_ENGINE_VERSION,
            "scanEngineVersion.in=" + UPDATED_SCAN_ENGINE_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByScanEngineVersionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanEngineVersion is not null
        defaultDocumentFiltering("scanEngineVersion.specified=true", "scanEngineVersion.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByScanEngineVersionContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanEngineVersion contains
        defaultDocumentFiltering(
            "scanEngineVersion.contains=" + DEFAULT_SCAN_ENGINE_VERSION,
            "scanEngineVersion.contains=" + UPDATED_SCAN_ENGINE_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByScanEngineVersionNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scanEngineVersion does not contain
        defaultDocumentFiltering(
            "scanEngineVersion.doesNotContain=" + UPDATED_SCAN_ENGINE_VERSION,
            "scanEngineVersion.doesNotContain=" + DEFAULT_SCAN_ENGINE_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByScannedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scannedAt equals to
        defaultDocumentFiltering("scannedAt.equals=" + DEFAULT_SCANNED_AT, "scannedAt.equals=" + UPDATED_SCANNED_AT);
    }

    @Test
    @Transactional
    void getAllDocumentsByScannedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scannedAt in
        defaultDocumentFiltering("scannedAt.in=" + DEFAULT_SCANNED_AT + "," + UPDATED_SCANNED_AT, "scannedAt.in=" + UPDATED_SCANNED_AT);
    }

    @Test
    @Transactional
    void getAllDocumentsByScannedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where scannedAt is not null
        defaultDocumentFiltering("scannedAt.specified=true", "scannedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingCandidatesIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingCandidates equals to
        defaultDocumentFiltering(
            "maskingCandidates.equals=" + DEFAULT_MASKING_CANDIDATES,
            "maskingCandidates.equals=" + UPDATED_MASKING_CANDIDATES
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingCandidatesIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingCandidates in
        defaultDocumentFiltering(
            "maskingCandidates.in=" + DEFAULT_MASKING_CANDIDATES + "," + UPDATED_MASKING_CANDIDATES,
            "maskingCandidates.in=" + UPDATED_MASKING_CANDIDATES
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingCandidatesIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingCandidates is not null
        defaultDocumentFiltering("maskingCandidates.specified=true", "maskingCandidates.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingCandidatesIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingCandidates is greater than or equal to
        defaultDocumentFiltering(
            "maskingCandidates.greaterThanOrEqual=" + DEFAULT_MASKING_CANDIDATES,
            "maskingCandidates.greaterThanOrEqual=" + UPDATED_MASKING_CANDIDATES
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingCandidatesIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingCandidates is less than or equal to
        defaultDocumentFiltering(
            "maskingCandidates.lessThanOrEqual=" + DEFAULT_MASKING_CANDIDATES,
            "maskingCandidates.lessThanOrEqual=" + SMALLER_MASKING_CANDIDATES
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingCandidatesIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingCandidates is less than
        defaultDocumentFiltering(
            "maskingCandidates.lessThan=" + UPDATED_MASKING_CANDIDATES,
            "maskingCandidates.lessThan=" + DEFAULT_MASKING_CANDIDATES
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingCandidatesIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingCandidates is greater than
        defaultDocumentFiltering(
            "maskingCandidates.greaterThan=" + SMALLER_MASKING_CANDIDATES,
            "maskingCandidates.greaterThan=" + DEFAULT_MASKING_CANDIDATES
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingLuhnValidIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingLuhnValid equals to
        defaultDocumentFiltering(
            "maskingLuhnValid.equals=" + DEFAULT_MASKING_LUHN_VALID,
            "maskingLuhnValid.equals=" + UPDATED_MASKING_LUHN_VALID
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingLuhnValidIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingLuhnValid in
        defaultDocumentFiltering(
            "maskingLuhnValid.in=" + DEFAULT_MASKING_LUHN_VALID + "," + UPDATED_MASKING_LUHN_VALID,
            "maskingLuhnValid.in=" + UPDATED_MASKING_LUHN_VALID
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingLuhnValidIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingLuhnValid is not null
        defaultDocumentFiltering("maskingLuhnValid.specified=true", "maskingLuhnValid.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingLuhnValidIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingLuhnValid is greater than or equal to
        defaultDocumentFiltering(
            "maskingLuhnValid.greaterThanOrEqual=" + DEFAULT_MASKING_LUHN_VALID,
            "maskingLuhnValid.greaterThanOrEqual=" + UPDATED_MASKING_LUHN_VALID
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingLuhnValidIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingLuhnValid is less than or equal to
        defaultDocumentFiltering(
            "maskingLuhnValid.lessThanOrEqual=" + DEFAULT_MASKING_LUHN_VALID,
            "maskingLuhnValid.lessThanOrEqual=" + SMALLER_MASKING_LUHN_VALID
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingLuhnValidIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingLuhnValid is less than
        defaultDocumentFiltering(
            "maskingLuhnValid.lessThan=" + UPDATED_MASKING_LUHN_VALID,
            "maskingLuhnValid.lessThan=" + DEFAULT_MASKING_LUHN_VALID
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingLuhnValidIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingLuhnValid is greater than
        defaultDocumentFiltering(
            "maskingLuhnValid.greaterThan=" + SMALLER_MASKING_LUHN_VALID,
            "maskingLuhnValid.greaterThan=" + DEFAULT_MASKING_LUHN_VALID
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingMaskedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingMasked equals to
        defaultDocumentFiltering("maskingMasked.equals=" + DEFAULT_MASKING_MASKED, "maskingMasked.equals=" + UPDATED_MASKING_MASKED);
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingMaskedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingMasked in
        defaultDocumentFiltering(
            "maskingMasked.in=" + DEFAULT_MASKING_MASKED + "," + UPDATED_MASKING_MASKED,
            "maskingMasked.in=" + UPDATED_MASKING_MASKED
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingMaskedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingMasked is not null
        defaultDocumentFiltering("maskingMasked.specified=true", "maskingMasked.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingMaskedIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingMasked is greater than or equal to
        defaultDocumentFiltering(
            "maskingMasked.greaterThanOrEqual=" + DEFAULT_MASKING_MASKED,
            "maskingMasked.greaterThanOrEqual=" + UPDATED_MASKING_MASKED
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingMaskedIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingMasked is less than or equal to
        defaultDocumentFiltering(
            "maskingMasked.lessThanOrEqual=" + DEFAULT_MASKING_MASKED,
            "maskingMasked.lessThanOrEqual=" + SMALLER_MASKING_MASKED
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingMaskedIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingMasked is less than
        defaultDocumentFiltering("maskingMasked.lessThan=" + UPDATED_MASKING_MASKED, "maskingMasked.lessThan=" + DEFAULT_MASKING_MASKED);
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingMaskedIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingMasked is greater than
        defaultDocumentFiltering(
            "maskingMasked.greaterThan=" + SMALLER_MASKING_MASKED,
            "maskingMasked.greaterThan=" + DEFAULT_MASKING_MASKED
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingRejectedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingRejected equals to
        defaultDocumentFiltering(
            "maskingRejected.equals=" + DEFAULT_MASKING_REJECTED,
            "maskingRejected.equals=" + UPDATED_MASKING_REJECTED
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingRejectedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingRejected in
        defaultDocumentFiltering(
            "maskingRejected.in=" + DEFAULT_MASKING_REJECTED + "," + UPDATED_MASKING_REJECTED,
            "maskingRejected.in=" + UPDATED_MASKING_REJECTED
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByMaskingRejectedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where maskingRejected is not null
        defaultDocumentFiltering("maskingRejected.specified=true", "maskingRejected.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByPdfaCompliantIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where pdfaCompliant equals to
        defaultDocumentFiltering("pdfaCompliant.equals=" + DEFAULT_PDFA_COMPLIANT, "pdfaCompliant.equals=" + UPDATED_PDFA_COMPLIANT);
    }

    @Test
    @Transactional
    void getAllDocumentsByPdfaCompliantIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where pdfaCompliant in
        defaultDocumentFiltering(
            "pdfaCompliant.in=" + DEFAULT_PDFA_COMPLIANT + "," + UPDATED_PDFA_COMPLIANT,
            "pdfaCompliant.in=" + UPDATED_PDFA_COMPLIANT
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByPdfaCompliantIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where pdfaCompliant is not null
        defaultDocumentFiltering("pdfaCompliant.specified=true", "pdfaCompliant.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByRejectionReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where rejectionReason equals to
        defaultDocumentFiltering(
            "rejectionReason.equals=" + DEFAULT_REJECTION_REASON,
            "rejectionReason.equals=" + UPDATED_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRejectionReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where rejectionReason in
        defaultDocumentFiltering(
            "rejectionReason.in=" + DEFAULT_REJECTION_REASON + "," + UPDATED_REJECTION_REASON,
            "rejectionReason.in=" + UPDATED_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRejectionReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where rejectionReason is not null
        defaultDocumentFiltering("rejectionReason.specified=true", "rejectionReason.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByRejectionReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where rejectionReason contains
        defaultDocumentFiltering(
            "rejectionReason.contains=" + DEFAULT_REJECTION_REASON,
            "rejectionReason.contains=" + UPDATED_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllDocumentsByRejectionReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where rejectionReason does not contain
        defaultDocumentFiltering(
            "rejectionReason.doesNotContain=" + UPDATED_REJECTION_REASON,
            "rejectionReason.doesNotContain=" + DEFAULT_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllDocumentsBySealedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sealedAt equals to
        defaultDocumentFiltering("sealedAt.equals=" + DEFAULT_SEALED_AT, "sealedAt.equals=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllDocumentsBySealedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sealedAt in
        defaultDocumentFiltering("sealedAt.in=" + DEFAULT_SEALED_AT + "," + UPDATED_SEALED_AT, "sealedAt.in=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllDocumentsBySealedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where sealedAt is not null
        defaultDocumentFiltering("sealedAt.specified=true", "sealedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByPurgedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where purgedAt equals to
        defaultDocumentFiltering("purgedAt.equals=" + DEFAULT_PURGED_AT, "purgedAt.equals=" + UPDATED_PURGED_AT);
    }

    @Test
    @Transactional
    void getAllDocumentsByPurgedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where purgedAt in
        defaultDocumentFiltering("purgedAt.in=" + DEFAULT_PURGED_AT + "," + UPDATED_PURGED_AT, "purgedAt.in=" + UPDATED_PURGED_AT);
    }

    @Test
    @Transactional
    void getAllDocumentsByPurgedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocument = documentRepository.saveAndFlush(document);

        // Get all the documentList where purgedAt is not null
        defaultDocumentFiltering("purgedAt.specified=true", "purgedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentsByLineageIsEqualToSomething() throws Exception {
        DocumentLineage lineage;
        if (TestUtil.findAll(em, DocumentLineage.class).isEmpty()) {
            documentRepository.saveAndFlush(document);
            lineage = DocumentLineageResourceIT.createEntity();
        } else {
            lineage = TestUtil.findAll(em, DocumentLineage.class).get(0);
        }
        em.persist(lineage);
        em.flush();
        document.setLineage(lineage);
        documentRepository.saveAndFlush(document);
        UUID lineageId = lineage.getId();
        // Get all the documentList where lineage equals to lineageId
        defaultDocumentShouldBeFound("lineageId.equals=" + lineageId);

        // Get all the documentList where lineage equals to UUID.randomUUID()
        defaultDocumentShouldNotBeFound("lineageId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllDocumentsByPolicyIsEqualToSomething() throws Exception {
        DocumentTypePolicy policy;
        if (TestUtil.findAll(em, DocumentTypePolicy.class).isEmpty()) {
            documentRepository.saveAndFlush(document);
            policy = DocumentTypePolicyResourceIT.createEntity();
        } else {
            policy = TestUtil.findAll(em, DocumentTypePolicy.class).get(0);
        }
        em.persist(policy);
        em.flush();
        document.setPolicy(policy);
        documentRepository.saveAndFlush(document);
        UUID policyId = policy.getId();
        // Get all the documentList where policy equals to policyId
        defaultDocumentShouldBeFound("policyId.equals=" + policyId);

        // Get all the documentList where policy equals to UUID.randomUUID()
        defaultDocumentShouldNotBeFound("policyId.equals=" + UUID.randomUUID());
    }

    private void defaultDocumentFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultDocumentShouldBeFound(shouldBeFound);
        defaultDocumentShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultDocumentShouldBeFound(String filter) throws Exception {
        restDocumentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(document.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION)))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].callerService").value(hasItem(DEFAULT_CALLER_SERVICE)))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].visibility").value(hasItem(DEFAULT_VISIBILITY.toString())))
            .andExpect(jsonPath("$.[*].ownerUnit").value(hasItem(DEFAULT_OWNER_UNIT)))
            .andExpect(jsonPath("$.[*].restrictedReaderIds").value(hasItem(DEFAULT_RESTRICTED_READER_IDS)))
            .andExpect(jsonPath("$.[*].depositedAt").value(hasItem(DEFAULT_DEPOSITED_AT.toString())))
            .andExpect(jsonPath("$.[*].sha256").value(hasItem(DEFAULT_SHA_256)))
            .andExpect(jsonPath("$.[*].sizeBytes").value(hasItem(DEFAULT_SIZE_BYTES.intValue())))
            .andExpect(jsonPath("$.[*].mediaType").value(hasItem(DEFAULT_MEDIA_TYPE)))
            .andExpect(jsonPath("$.[*].objectKey").value(hasItem(DEFAULT_OBJECT_KEY)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].retentionPeriod").value(hasItem(DEFAULT_RETENTION_PERIOD)))
            .andExpect(jsonPath("$.[*].retentionMode").value(hasItem(DEFAULT_RETENTION_MODE.toString())))
            .andExpect(jsonPath("$.[*].legalHold").value(hasItem(DEFAULT_LEGAL_HOLD)))
            .andExpect(jsonPath("$.[*].retainUntil").value(hasItem(DEFAULT_RETAIN_UNTIL.toString())))
            .andExpect(jsonPath("$.[*].lockedAt").value(hasItem(DEFAULT_LOCKED_AT.toString())))
            .andExpect(jsonPath("$.[*].scanClean").value(hasItem(DEFAULT_SCAN_CLEAN)))
            .andExpect(jsonPath("$.[*].scanSignatureName").value(hasItem(DEFAULT_SCAN_SIGNATURE_NAME)))
            .andExpect(jsonPath("$.[*].scanEngineVersion").value(hasItem(DEFAULT_SCAN_ENGINE_VERSION)))
            .andExpect(jsonPath("$.[*].scannedAt").value(hasItem(DEFAULT_SCANNED_AT.toString())))
            .andExpect(jsonPath("$.[*].maskingCandidates").value(hasItem(DEFAULT_MASKING_CANDIDATES)))
            .andExpect(jsonPath("$.[*].maskingLuhnValid").value(hasItem(DEFAULT_MASKING_LUHN_VALID)))
            .andExpect(jsonPath("$.[*].maskingMasked").value(hasItem(DEFAULT_MASKING_MASKED)))
            .andExpect(jsonPath("$.[*].maskingRejected").value(hasItem(DEFAULT_MASKING_REJECTED)))
            .andExpect(jsonPath("$.[*].pdfaCompliant").value(hasItem(DEFAULT_PDFA_COMPLIANT)))
            .andExpect(jsonPath("$.[*].rejectionReason").value(hasItem(DEFAULT_REJECTION_REASON)))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].purgedAt").value(hasItem(DEFAULT_PURGED_AT.toString())));

        // Check, that the count call also returns 1
        restDocumentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultDocumentShouldNotBeFound(String filter) throws Exception {
        restDocumentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restDocumentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingDocument() throws Exception {
        // Get the document
        restDocumentMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return documentRepository.count();
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

    protected Document getPersistedDocument(Document document) {
        return documentRepository.findById(document.getId()).orElseThrow();
    }

    protected void assertPersistedDocumentToMatchAllProperties(Document expectedDocument) {
        assertDocumentAllPropertiesEquals(expectedDocument, getPersistedDocument(expectedDocument));
    }

    protected void assertPersistedDocumentToMatchUpdatableProperties(Document expectedDocument) {
        assertDocumentAllUpdatablePropertiesEquals(expectedDocument, getPersistedDocument(expectedDocument));
    }
}
