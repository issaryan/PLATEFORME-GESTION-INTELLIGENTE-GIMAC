package cm.gimac.des.document.web.rest;

import static cm.gimac.des.document.domain.DocumentTypePolicyAsserts.*;
import static cm.gimac.des.document.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.domain.DocumentTypePolicy;
import cm.gimac.des.document.domain.enumeration.DocumentType;
import cm.gimac.des.document.domain.enumeration.MaskingRule;
import cm.gimac.des.document.domain.enumeration.RetentionMode;
import cm.gimac.des.document.repository.DocumentTypePolicyRepository;
import cm.gimac.des.document.service.dto.DocumentTypePolicyDTO;
import cm.gimac.des.document.service.mapper.DocumentTypePolicyMapper;
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
 * Integration tests for the {@link DocumentTypePolicyResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DocumentTypePolicyResourceIT {

    private static final DocumentType DEFAULT_DOCUMENT_TYPE = DocumentType.RAPPORT_DE_QUART;
    private static final DocumentType UPDATED_DOCUMENT_TYPE = DocumentType.ATTESTATION;

    private static final Integer DEFAULT_POLICY_VERSION = 1;
    private static final Integer UPDATED_POLICY_VERSION = 2;
    private static final Integer SMALLER_POLICY_VERSION = 1 - 1;

    private static final String DEFAULT_RETENTION_PERIOD = "P32350Y";
    // "P" (valeur générée) est contenue dans la valeur par défaut : le filtre « contient » ne pouvait pas être vide.
    private static final String UPDATED_RETENTION_PERIOD = "P30D";

    private static final RetentionMode DEFAULT_RETENTION_MODE = RetentionMode.CONFORMITE;
    private static final RetentionMode UPDATED_RETENTION_MODE = RetentionMode.GOUVERNANCE;

    private static final MaskingRule DEFAULT_MASKING_RULE = MaskingRule.REJET;
    private static final MaskingRule UPDATED_MASKING_RULE = MaskingRule.MASQUAGE;

    private static final Boolean DEFAULT_RESTRICTED_ACCESS = false;
    private static final Boolean UPDATED_RESTRICTED_ACCESS = true;

    private static final Boolean DEFAULT_PDFA_REQUIRED = false;
    private static final Boolean UPDATED_PDFA_REQUIRED = true;

    private static final String DEFAULT_ALLOWED_MEDIA_TYPES = "AAAAAAAAAA";
    private static final String UPDATED_ALLOWED_MEDIA_TYPES = "BBBBBBBBBB";

    private static final Long DEFAULT_MAX_SIZE_BYTES = 1L;
    private static final Long UPDATED_MAX_SIZE_BYTES = 2L;
    private static final Long SMALLER_MAX_SIZE_BYTES = 1L - 1L;

    private static final String DEFAULT_ALLOWED_CALLERS = "AAAAAAAAAA";
    private static final String UPDATED_ALLOWED_CALLERS = "BBBBBBBBBB";

    private static final Instant DEFAULT_EFFECTIVE_FROM = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EFFECTIVE_FROM = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/document-type-policies";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DocumentTypePolicyRepository documentTypePolicyRepository;

    @Autowired
    private DocumentTypePolicyMapper documentTypePolicyMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDocumentTypePolicyMockMvc;

    private DocumentTypePolicy documentTypePolicy;

    private DocumentTypePolicy insertedDocumentTypePolicy;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DocumentTypePolicy createEntity() {
        return new DocumentTypePolicy()
            .documentType(DEFAULT_DOCUMENT_TYPE)
            .policyVersion(DEFAULT_POLICY_VERSION)
            .retentionPeriod(DEFAULT_RETENTION_PERIOD)
            .retentionMode(DEFAULT_RETENTION_MODE)
            .maskingRule(DEFAULT_MASKING_RULE)
            .restrictedAccess(DEFAULT_RESTRICTED_ACCESS)
            .pdfaRequired(DEFAULT_PDFA_REQUIRED)
            .allowedMediaTypes(DEFAULT_ALLOWED_MEDIA_TYPES)
            .maxSizeBytes(DEFAULT_MAX_SIZE_BYTES)
            .allowedCallers(DEFAULT_ALLOWED_CALLERS)
            .effectiveFrom(DEFAULT_EFFECTIVE_FROM)
            .active(DEFAULT_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DocumentTypePolicy createUpdatedEntity() {
        return new DocumentTypePolicy()
            .documentType(UPDATED_DOCUMENT_TYPE)
            .policyVersion(UPDATED_POLICY_VERSION)
            .retentionPeriod(UPDATED_RETENTION_PERIOD)
            .retentionMode(UPDATED_RETENTION_MODE)
            .maskingRule(UPDATED_MASKING_RULE)
            .restrictedAccess(UPDATED_RESTRICTED_ACCESS)
            .pdfaRequired(UPDATED_PDFA_REQUIRED)
            .allowedMediaTypes(UPDATED_ALLOWED_MEDIA_TYPES)
            .maxSizeBytes(UPDATED_MAX_SIZE_BYTES)
            .allowedCallers(UPDATED_ALLOWED_CALLERS)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .active(UPDATED_ACTIVE);
    }

    @BeforeEach
    void initTest() {
        documentTypePolicy = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedDocumentTypePolicy != null) {
            documentTypePolicyRepository.delete(insertedDocumentTypePolicy);
            insertedDocumentTypePolicy = null;
        }
    }

    @Test
    @Transactional
    void createDocumentTypePolicy() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the DocumentTypePolicy
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);
        var returnedDocumentTypePolicyDTO = om.readValue(
            restDocumentTypePolicyMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(documentTypePolicyDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            DocumentTypePolicyDTO.class
        );

        // Validate the DocumentTypePolicy in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedDocumentTypePolicy = documentTypePolicyMapper.toEntity(returnedDocumentTypePolicyDTO);
        assertDocumentTypePolicyUpdatableFieldsEquals(
            returnedDocumentTypePolicy,
            getPersistedDocumentTypePolicy(returnedDocumentTypePolicy)
        );

        insertedDocumentTypePolicy = returnedDocumentTypePolicy;
    }

    @Test
    @Transactional
    void createDocumentTypePolicyWithExistingId() throws Exception {
        // Create the DocumentTypePolicy with an existing ID
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DocumentTypePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDocumentTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        documentTypePolicy.setDocumentType(null);

        // Create the DocumentTypePolicy, which fails.
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPolicyVersionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        documentTypePolicy.setPolicyVersion(null);

        // Create the DocumentTypePolicy, which fails.
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRetentionPeriodIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        documentTypePolicy.setRetentionPeriod(null);

        // Create the DocumentTypePolicy, which fails.
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRetentionModeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        documentTypePolicy.setRetentionMode(null);

        // Create the DocumentTypePolicy, which fails.
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMaskingRuleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        documentTypePolicy.setMaskingRule(null);

        // Create the DocumentTypePolicy, which fails.
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRestrictedAccessIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        documentTypePolicy.setRestrictedAccess(null);

        // Create the DocumentTypePolicy, which fails.
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPdfaRequiredIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        documentTypePolicy.setPdfaRequired(null);

        // Create the DocumentTypePolicy, which fails.
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAllowedMediaTypesIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        documentTypePolicy.setAllowedMediaTypes(null);

        // Create the DocumentTypePolicy, which fails.
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMaxSizeBytesIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        documentTypePolicy.setMaxSizeBytes(null);

        // Create the DocumentTypePolicy, which fails.
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEffectiveFromIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        documentTypePolicy.setEffectiveFrom(null);

        // Create the DocumentTypePolicy, which fails.
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        documentTypePolicy.setActive(null);

        // Create the DocumentTypePolicy, which fails.
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllDocumentTypePolicies() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList
        restDocumentTypePolicyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(documentTypePolicy.getId().toString())))
            .andExpect(jsonPath("$.[*].documentType").value(hasItem(DEFAULT_DOCUMENT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].policyVersion").value(hasItem(DEFAULT_POLICY_VERSION)))
            .andExpect(jsonPath("$.[*].retentionPeriod").value(hasItem(DEFAULT_RETENTION_PERIOD)))
            .andExpect(jsonPath("$.[*].retentionMode").value(hasItem(DEFAULT_RETENTION_MODE.toString())))
            .andExpect(jsonPath("$.[*].maskingRule").value(hasItem(DEFAULT_MASKING_RULE.toString())))
            .andExpect(jsonPath("$.[*].restrictedAccess").value(hasItem(DEFAULT_RESTRICTED_ACCESS)))
            .andExpect(jsonPath("$.[*].pdfaRequired").value(hasItem(DEFAULT_PDFA_REQUIRED)))
            .andExpect(jsonPath("$.[*].allowedMediaTypes").value(hasItem(DEFAULT_ALLOWED_MEDIA_TYPES)))
            .andExpect(jsonPath("$.[*].maxSizeBytes").value(hasItem(DEFAULT_MAX_SIZE_BYTES.intValue())))
            .andExpect(jsonPath("$.[*].allowedCallers").value(hasItem(DEFAULT_ALLOWED_CALLERS)))
            .andExpect(jsonPath("$.[*].effectiveFrom").value(hasItem(DEFAULT_EFFECTIVE_FROM.toString())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @Test
    @Transactional
    void getDocumentTypePolicy() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get the documentTypePolicy
        restDocumentTypePolicyMockMvc
            .perform(get(ENTITY_API_URL_ID, documentTypePolicy.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(documentTypePolicy.getId().toString()))
            .andExpect(jsonPath("$.documentType").value(DEFAULT_DOCUMENT_TYPE.toString()))
            .andExpect(jsonPath("$.policyVersion").value(DEFAULT_POLICY_VERSION))
            .andExpect(jsonPath("$.retentionPeriod").value(DEFAULT_RETENTION_PERIOD))
            .andExpect(jsonPath("$.retentionMode").value(DEFAULT_RETENTION_MODE.toString()))
            .andExpect(jsonPath("$.maskingRule").value(DEFAULT_MASKING_RULE.toString()))
            .andExpect(jsonPath("$.restrictedAccess").value(DEFAULT_RESTRICTED_ACCESS))
            .andExpect(jsonPath("$.pdfaRequired").value(DEFAULT_PDFA_REQUIRED))
            .andExpect(jsonPath("$.allowedMediaTypes").value(DEFAULT_ALLOWED_MEDIA_TYPES))
            .andExpect(jsonPath("$.maxSizeBytes").value(DEFAULT_MAX_SIZE_BYTES.intValue()))
            .andExpect(jsonPath("$.allowedCallers").value(DEFAULT_ALLOWED_CALLERS))
            .andExpect(jsonPath("$.effectiveFrom").value(DEFAULT_EFFECTIVE_FROM.toString()))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getDocumentTypePoliciesByIdFiltering() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        UUID id = documentTypePolicy.getId();

        defaultDocumentTypePolicyFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByDocumentTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where documentType equals to
        defaultDocumentTypePolicyFiltering("documentType.equals=" + DEFAULT_DOCUMENT_TYPE, "documentType.equals=" + UPDATED_DOCUMENT_TYPE);
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByDocumentTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where documentType in
        defaultDocumentTypePolicyFiltering(
            "documentType.in=" + DEFAULT_DOCUMENT_TYPE + "," + UPDATED_DOCUMENT_TYPE,
            "documentType.in=" + UPDATED_DOCUMENT_TYPE
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByDocumentTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where documentType is not null
        defaultDocumentTypePolicyFiltering("documentType.specified=true", "documentType.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByPolicyVersionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where policyVersion equals to
        defaultDocumentTypePolicyFiltering(
            "policyVersion.equals=" + DEFAULT_POLICY_VERSION,
            "policyVersion.equals=" + UPDATED_POLICY_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByPolicyVersionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where policyVersion in
        defaultDocumentTypePolicyFiltering(
            "policyVersion.in=" + DEFAULT_POLICY_VERSION + "," + UPDATED_POLICY_VERSION,
            "policyVersion.in=" + UPDATED_POLICY_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByPolicyVersionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where policyVersion is not null
        defaultDocumentTypePolicyFiltering("policyVersion.specified=true", "policyVersion.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByPolicyVersionIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where policyVersion is greater than or equal to
        defaultDocumentTypePolicyFiltering(
            "policyVersion.greaterThanOrEqual=" + DEFAULT_POLICY_VERSION,
            "policyVersion.greaterThanOrEqual=" + UPDATED_POLICY_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByPolicyVersionIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where policyVersion is less than or equal to
        defaultDocumentTypePolicyFiltering(
            "policyVersion.lessThanOrEqual=" + DEFAULT_POLICY_VERSION,
            "policyVersion.lessThanOrEqual=" + SMALLER_POLICY_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByPolicyVersionIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where policyVersion is less than
        defaultDocumentTypePolicyFiltering(
            "policyVersion.lessThan=" + UPDATED_POLICY_VERSION,
            "policyVersion.lessThan=" + DEFAULT_POLICY_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByPolicyVersionIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where policyVersion is greater than
        defaultDocumentTypePolicyFiltering(
            "policyVersion.greaterThan=" + SMALLER_POLICY_VERSION,
            "policyVersion.greaterThan=" + DEFAULT_POLICY_VERSION
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByRetentionPeriodIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where retentionPeriod equals to
        defaultDocumentTypePolicyFiltering(
            "retentionPeriod.equals=" + DEFAULT_RETENTION_PERIOD,
            "retentionPeriod.equals=" + UPDATED_RETENTION_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByRetentionPeriodIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where retentionPeriod in
        defaultDocumentTypePolicyFiltering(
            "retentionPeriod.in=" + DEFAULT_RETENTION_PERIOD + "," + UPDATED_RETENTION_PERIOD,
            "retentionPeriod.in=" + UPDATED_RETENTION_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByRetentionPeriodIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where retentionPeriod is not null
        defaultDocumentTypePolicyFiltering("retentionPeriod.specified=true", "retentionPeriod.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByRetentionPeriodContainsSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where retentionPeriod contains
        defaultDocumentTypePolicyFiltering(
            "retentionPeriod.contains=" + DEFAULT_RETENTION_PERIOD,
            "retentionPeriod.contains=" + UPDATED_RETENTION_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByRetentionPeriodNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where retentionPeriod does not contain
        defaultDocumentTypePolicyFiltering(
            "retentionPeriod.doesNotContain=" + UPDATED_RETENTION_PERIOD,
            "retentionPeriod.doesNotContain=" + DEFAULT_RETENTION_PERIOD
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByRetentionModeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where retentionMode equals to
        defaultDocumentTypePolicyFiltering(
            "retentionMode.equals=" + DEFAULT_RETENTION_MODE,
            "retentionMode.equals=" + UPDATED_RETENTION_MODE
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByRetentionModeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where retentionMode in
        defaultDocumentTypePolicyFiltering(
            "retentionMode.in=" + DEFAULT_RETENTION_MODE + "," + UPDATED_RETENTION_MODE,
            "retentionMode.in=" + UPDATED_RETENTION_MODE
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByRetentionModeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where retentionMode is not null
        defaultDocumentTypePolicyFiltering("retentionMode.specified=true", "retentionMode.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByMaskingRuleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where maskingRule equals to
        defaultDocumentTypePolicyFiltering("maskingRule.equals=" + DEFAULT_MASKING_RULE, "maskingRule.equals=" + UPDATED_MASKING_RULE);
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByMaskingRuleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where maskingRule in
        defaultDocumentTypePolicyFiltering(
            "maskingRule.in=" + DEFAULT_MASKING_RULE + "," + UPDATED_MASKING_RULE,
            "maskingRule.in=" + UPDATED_MASKING_RULE
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByMaskingRuleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where maskingRule is not null
        defaultDocumentTypePolicyFiltering("maskingRule.specified=true", "maskingRule.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByRestrictedAccessIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where restrictedAccess equals to
        defaultDocumentTypePolicyFiltering(
            "restrictedAccess.equals=" + DEFAULT_RESTRICTED_ACCESS,
            "restrictedAccess.equals=" + UPDATED_RESTRICTED_ACCESS
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByRestrictedAccessIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where restrictedAccess in
        defaultDocumentTypePolicyFiltering(
            "restrictedAccess.in=" + DEFAULT_RESTRICTED_ACCESS + "," + UPDATED_RESTRICTED_ACCESS,
            "restrictedAccess.in=" + UPDATED_RESTRICTED_ACCESS
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByRestrictedAccessIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where restrictedAccess is not null
        defaultDocumentTypePolicyFiltering("restrictedAccess.specified=true", "restrictedAccess.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByPdfaRequiredIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where pdfaRequired equals to
        defaultDocumentTypePolicyFiltering("pdfaRequired.equals=" + DEFAULT_PDFA_REQUIRED, "pdfaRequired.equals=" + UPDATED_PDFA_REQUIRED);
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByPdfaRequiredIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where pdfaRequired in
        defaultDocumentTypePolicyFiltering(
            "pdfaRequired.in=" + DEFAULT_PDFA_REQUIRED + "," + UPDATED_PDFA_REQUIRED,
            "pdfaRequired.in=" + UPDATED_PDFA_REQUIRED
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByPdfaRequiredIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where pdfaRequired is not null
        defaultDocumentTypePolicyFiltering("pdfaRequired.specified=true", "pdfaRequired.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByAllowedMediaTypesIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where allowedMediaTypes equals to
        defaultDocumentTypePolicyFiltering(
            "allowedMediaTypes.equals=" + DEFAULT_ALLOWED_MEDIA_TYPES,
            "allowedMediaTypes.equals=" + UPDATED_ALLOWED_MEDIA_TYPES
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByAllowedMediaTypesIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where allowedMediaTypes in
        defaultDocumentTypePolicyFiltering(
            "allowedMediaTypes.in=" + DEFAULT_ALLOWED_MEDIA_TYPES + "," + UPDATED_ALLOWED_MEDIA_TYPES,
            "allowedMediaTypes.in=" + UPDATED_ALLOWED_MEDIA_TYPES
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByAllowedMediaTypesIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where allowedMediaTypes is not null
        defaultDocumentTypePolicyFiltering("allowedMediaTypes.specified=true", "allowedMediaTypes.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByAllowedMediaTypesContainsSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where allowedMediaTypes contains
        defaultDocumentTypePolicyFiltering(
            "allowedMediaTypes.contains=" + DEFAULT_ALLOWED_MEDIA_TYPES,
            "allowedMediaTypes.contains=" + UPDATED_ALLOWED_MEDIA_TYPES
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByAllowedMediaTypesNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where allowedMediaTypes does not contain
        defaultDocumentTypePolicyFiltering(
            "allowedMediaTypes.doesNotContain=" + UPDATED_ALLOWED_MEDIA_TYPES,
            "allowedMediaTypes.doesNotContain=" + DEFAULT_ALLOWED_MEDIA_TYPES
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByMaxSizeBytesIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where maxSizeBytes equals to
        defaultDocumentTypePolicyFiltering(
            "maxSizeBytes.equals=" + DEFAULT_MAX_SIZE_BYTES,
            "maxSizeBytes.equals=" + UPDATED_MAX_SIZE_BYTES
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByMaxSizeBytesIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where maxSizeBytes in
        defaultDocumentTypePolicyFiltering(
            "maxSizeBytes.in=" + DEFAULT_MAX_SIZE_BYTES + "," + UPDATED_MAX_SIZE_BYTES,
            "maxSizeBytes.in=" + UPDATED_MAX_SIZE_BYTES
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByMaxSizeBytesIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where maxSizeBytes is not null
        defaultDocumentTypePolicyFiltering("maxSizeBytes.specified=true", "maxSizeBytes.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByMaxSizeBytesIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where maxSizeBytes is greater than or equal to
        defaultDocumentTypePolicyFiltering(
            "maxSizeBytes.greaterThanOrEqual=" + DEFAULT_MAX_SIZE_BYTES,
            "maxSizeBytes.greaterThanOrEqual=" + UPDATED_MAX_SIZE_BYTES
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByMaxSizeBytesIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where maxSizeBytes is less than or equal to
        defaultDocumentTypePolicyFiltering(
            "maxSizeBytes.lessThanOrEqual=" + DEFAULT_MAX_SIZE_BYTES,
            "maxSizeBytes.lessThanOrEqual=" + SMALLER_MAX_SIZE_BYTES
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByMaxSizeBytesIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where maxSizeBytes is less than
        defaultDocumentTypePolicyFiltering(
            "maxSizeBytes.lessThan=" + UPDATED_MAX_SIZE_BYTES,
            "maxSizeBytes.lessThan=" + DEFAULT_MAX_SIZE_BYTES
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByMaxSizeBytesIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where maxSizeBytes is greater than
        defaultDocumentTypePolicyFiltering(
            "maxSizeBytes.greaterThan=" + SMALLER_MAX_SIZE_BYTES,
            "maxSizeBytes.greaterThan=" + DEFAULT_MAX_SIZE_BYTES
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByAllowedCallersIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where allowedCallers equals to
        defaultDocumentTypePolicyFiltering(
            "allowedCallers.equals=" + DEFAULT_ALLOWED_CALLERS,
            "allowedCallers.equals=" + UPDATED_ALLOWED_CALLERS
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByAllowedCallersIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where allowedCallers in
        defaultDocumentTypePolicyFiltering(
            "allowedCallers.in=" + DEFAULT_ALLOWED_CALLERS + "," + UPDATED_ALLOWED_CALLERS,
            "allowedCallers.in=" + UPDATED_ALLOWED_CALLERS
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByAllowedCallersIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where allowedCallers is not null
        defaultDocumentTypePolicyFiltering("allowedCallers.specified=true", "allowedCallers.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByAllowedCallersContainsSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where allowedCallers contains
        defaultDocumentTypePolicyFiltering(
            "allowedCallers.contains=" + DEFAULT_ALLOWED_CALLERS,
            "allowedCallers.contains=" + UPDATED_ALLOWED_CALLERS
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByAllowedCallersNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where allowedCallers does not contain
        defaultDocumentTypePolicyFiltering(
            "allowedCallers.doesNotContain=" + UPDATED_ALLOWED_CALLERS,
            "allowedCallers.doesNotContain=" + DEFAULT_ALLOWED_CALLERS
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByEffectiveFromIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where effectiveFrom equals to
        defaultDocumentTypePolicyFiltering(
            "effectiveFrom.equals=" + DEFAULT_EFFECTIVE_FROM,
            "effectiveFrom.equals=" + UPDATED_EFFECTIVE_FROM
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByEffectiveFromIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where effectiveFrom in
        defaultDocumentTypePolicyFiltering(
            "effectiveFrom.in=" + DEFAULT_EFFECTIVE_FROM + "," + UPDATED_EFFECTIVE_FROM,
            "effectiveFrom.in=" + UPDATED_EFFECTIVE_FROM
        );
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByEffectiveFromIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where effectiveFrom is not null
        defaultDocumentTypePolicyFiltering("effectiveFrom.specified=true", "effectiveFrom.specified=false");
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByActiveIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where active equals to
        defaultDocumentTypePolicyFiltering("active.equals=" + DEFAULT_ACTIVE, "active.equals=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByActiveIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where active in
        defaultDocumentTypePolicyFiltering("active.in=" + DEFAULT_ACTIVE + "," + UPDATED_ACTIVE, "active.in=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllDocumentTypePoliciesByActiveIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        // Get all the documentTypePolicyList where active is not null
        defaultDocumentTypePolicyFiltering("active.specified=true", "active.specified=false");
    }

    private void defaultDocumentTypePolicyFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultDocumentTypePolicyShouldBeFound(shouldBeFound);
        defaultDocumentTypePolicyShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultDocumentTypePolicyShouldBeFound(String filter) throws Exception {
        restDocumentTypePolicyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(documentTypePolicy.getId().toString())))
            .andExpect(jsonPath("$.[*].documentType").value(hasItem(DEFAULT_DOCUMENT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].policyVersion").value(hasItem(DEFAULT_POLICY_VERSION)))
            .andExpect(jsonPath("$.[*].retentionPeriod").value(hasItem(DEFAULT_RETENTION_PERIOD)))
            .andExpect(jsonPath("$.[*].retentionMode").value(hasItem(DEFAULT_RETENTION_MODE.toString())))
            .andExpect(jsonPath("$.[*].maskingRule").value(hasItem(DEFAULT_MASKING_RULE.toString())))
            .andExpect(jsonPath("$.[*].restrictedAccess").value(hasItem(DEFAULT_RESTRICTED_ACCESS)))
            .andExpect(jsonPath("$.[*].pdfaRequired").value(hasItem(DEFAULT_PDFA_REQUIRED)))
            .andExpect(jsonPath("$.[*].allowedMediaTypes").value(hasItem(DEFAULT_ALLOWED_MEDIA_TYPES)))
            .andExpect(jsonPath("$.[*].maxSizeBytes").value(hasItem(DEFAULT_MAX_SIZE_BYTES.intValue())))
            .andExpect(jsonPath("$.[*].allowedCallers").value(hasItem(DEFAULT_ALLOWED_CALLERS)))
            .andExpect(jsonPath("$.[*].effectiveFrom").value(hasItem(DEFAULT_EFFECTIVE_FROM.toString())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));

        // Check, that the count call also returns 1
        restDocumentTypePolicyMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultDocumentTypePolicyShouldNotBeFound(String filter) throws Exception {
        restDocumentTypePolicyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restDocumentTypePolicyMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingDocumentTypePolicy() throws Exception {
        // Get the documentTypePolicy
        restDocumentTypePolicyMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDocumentTypePolicy() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the documentTypePolicy
        DocumentTypePolicy updatedDocumentTypePolicy = documentTypePolicyRepository.findById(documentTypePolicy.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedDocumentTypePolicy are not directly saved in db
        em.detach(updatedDocumentTypePolicy);
        updatedDocumentTypePolicy
            .documentType(UPDATED_DOCUMENT_TYPE)
            .policyVersion(UPDATED_POLICY_VERSION)
            .retentionPeriod(UPDATED_RETENTION_PERIOD)
            .retentionMode(UPDATED_RETENTION_MODE)
            .maskingRule(UPDATED_MASKING_RULE)
            .restrictedAccess(UPDATED_RESTRICTED_ACCESS)
            .pdfaRequired(UPDATED_PDFA_REQUIRED)
            .allowedMediaTypes(UPDATED_ALLOWED_MEDIA_TYPES)
            .maxSizeBytes(UPDATED_MAX_SIZE_BYTES)
            .allowedCallers(UPDATED_ALLOWED_CALLERS)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .active(UPDATED_ACTIVE);
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(updatedDocumentTypePolicy);

        restDocumentTypePolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, documentTypePolicyDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isOk());

        // Validate the DocumentTypePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDocumentTypePolicyToMatchAllProperties(updatedDocumentTypePolicy);
    }

    @Test
    @Transactional
    void putNonExistingDocumentTypePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        documentTypePolicy.setId(UUID.randomUUID());

        // Create the DocumentTypePolicy
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDocumentTypePolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, documentTypePolicyDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DocumentTypePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDocumentTypePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        documentTypePolicy.setId(UUID.randomUUID());

        // Create the DocumentTypePolicy
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDocumentTypePolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DocumentTypePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDocumentTypePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        documentTypePolicy.setId(UUID.randomUUID());

        // Create the DocumentTypePolicy
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDocumentTypePolicyMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the DocumentTypePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDocumentTypePolicyWithPatch() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the documentTypePolicy using partial update
        DocumentTypePolicy partialUpdatedDocumentTypePolicy = new DocumentTypePolicy();
        partialUpdatedDocumentTypePolicy.setId(documentTypePolicy.getId());

        partialUpdatedDocumentTypePolicy
            .documentType(UPDATED_DOCUMENT_TYPE)
            .policyVersion(UPDATED_POLICY_VERSION)
            .retentionPeriod(UPDATED_RETENTION_PERIOD)
            .maskingRule(UPDATED_MASKING_RULE)
            .restrictedAccess(UPDATED_RESTRICTED_ACCESS)
            .maxSizeBytes(UPDATED_MAX_SIZE_BYTES)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .active(UPDATED_ACTIVE);

        restDocumentTypePolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDocumentTypePolicy.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDocumentTypePolicy))
            )
            .andExpect(status().isOk());

        // Validate the DocumentTypePolicy in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDocumentTypePolicyUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedDocumentTypePolicy, documentTypePolicy),
            getPersistedDocumentTypePolicy(documentTypePolicy)
        );
    }

    @Test
    @Transactional
    void fullUpdateDocumentTypePolicyWithPatch() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the documentTypePolicy using partial update
        DocumentTypePolicy partialUpdatedDocumentTypePolicy = new DocumentTypePolicy();
        partialUpdatedDocumentTypePolicy.setId(documentTypePolicy.getId());

        partialUpdatedDocumentTypePolicy
            .documentType(UPDATED_DOCUMENT_TYPE)
            .policyVersion(UPDATED_POLICY_VERSION)
            .retentionPeriod(UPDATED_RETENTION_PERIOD)
            .retentionMode(UPDATED_RETENTION_MODE)
            .maskingRule(UPDATED_MASKING_RULE)
            .restrictedAccess(UPDATED_RESTRICTED_ACCESS)
            .pdfaRequired(UPDATED_PDFA_REQUIRED)
            .allowedMediaTypes(UPDATED_ALLOWED_MEDIA_TYPES)
            .maxSizeBytes(UPDATED_MAX_SIZE_BYTES)
            .allowedCallers(UPDATED_ALLOWED_CALLERS)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .active(UPDATED_ACTIVE);

        restDocumentTypePolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDocumentTypePolicy.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDocumentTypePolicy))
            )
            .andExpect(status().isOk());

        // Validate the DocumentTypePolicy in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDocumentTypePolicyUpdatableFieldsEquals(
            partialUpdatedDocumentTypePolicy,
            getPersistedDocumentTypePolicy(partialUpdatedDocumentTypePolicy)
        );
    }

    @Test
    @Transactional
    void patchNonExistingDocumentTypePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        documentTypePolicy.setId(UUID.randomUUID());

        // Create the DocumentTypePolicy
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDocumentTypePolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, documentTypePolicyDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DocumentTypePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDocumentTypePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        documentTypePolicy.setId(UUID.randomUUID());

        // Create the DocumentTypePolicy
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDocumentTypePolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DocumentTypePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDocumentTypePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        documentTypePolicy.setId(UUID.randomUUID());

        // Create the DocumentTypePolicy
        DocumentTypePolicyDTO documentTypePolicyDTO = documentTypePolicyMapper.toDto(documentTypePolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDocumentTypePolicyMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(documentTypePolicyDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the DocumentTypePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDocumentTypePolicy() throws Exception {
        // Initialize the database
        insertedDocumentTypePolicy = documentTypePolicyRepository.saveAndFlush(documentTypePolicy);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the documentTypePolicy
        restDocumentTypePolicyMockMvc
            .perform(delete(ENTITY_API_URL_ID, documentTypePolicy.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return documentTypePolicyRepository.count();
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

    protected DocumentTypePolicy getPersistedDocumentTypePolicy(DocumentTypePolicy documentTypePolicy) {
        return documentTypePolicyRepository.findById(documentTypePolicy.getId()).orElseThrow();
    }

    protected void assertPersistedDocumentTypePolicyToMatchAllProperties(DocumentTypePolicy expectedDocumentTypePolicy) {
        assertDocumentTypePolicyAllPropertiesEquals(expectedDocumentTypePolicy, getPersistedDocumentTypePolicy(expectedDocumentTypePolicy));
    }

    protected void assertPersistedDocumentTypePolicyToMatchUpdatableProperties(DocumentTypePolicy expectedDocumentTypePolicy) {
        assertDocumentTypePolicyAllUpdatablePropertiesEquals(
            expectedDocumentTypePolicy,
            getPersistedDocumentTypePolicy(expectedDocumentTypePolicy)
        );
    }
}
