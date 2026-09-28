package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.ClosedOperationSnapshotAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.ClosedOperationSnapshot;
import cm.gimac.des.certification.repository.ClosedOperationSnapshotRepository;
import cm.gimac.des.certification.service.mapper.ClosedOperationSnapshotMapper;
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
 * Integration tests for the {@link ClosedOperationSnapshotResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ClosedOperationSnapshotResourceIT {

    private static final UUID DEFAULT_LIAISON_ID = UUID.randomUUID();
    private static final UUID UPDATED_LIAISON_ID = UUID.randomUUID();

    private static final UUID DEFAULT_OPERATION_ID = UUID.randomUUID();
    private static final UUID UPDATED_OPERATION_ID = UUID.randomUUID();

    private static final String DEFAULT_OPERATION_CODE = "AAAAAAAAAA";
    private static final String UPDATED_OPERATION_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_REFERENCE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final UUID DEFAULT_OWNER_ID = UUID.randomUUID();
    private static final UUID UPDATED_OWNER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_VALIDATOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_VALIDATOR_ID = UUID.randomUUID();

    private static final UUID DEFAULT_EVIDENCE_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_EVIDENCE_DOCUMENT_ID = UUID.randomUUID();

    private static final String DEFAULT_EVIDENCE_DIGEST = "ce3aebf7a2811b3289f372991833c7e50be4bfab138f9073b93e8560bbe48b49";
    private static final String UPDATED_EVIDENCE_DIGEST = "ff2c0536b8605328d95607d80bb4ee1c5d3f902c1638d322b81d438c93a31400";

    private static final Instant DEFAULT_CLOSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLOSED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_SOURCE_EVENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_SOURCE_EVENT_ID = UUID.randomUUID();

    private static final String ENTITY_API_URL = "/api/closed-operation-snapshots";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ClosedOperationSnapshotRepository closedOperationSnapshotRepository;

    @Autowired
    private ClosedOperationSnapshotMapper closedOperationSnapshotMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restClosedOperationSnapshotMockMvc;

    private ClosedOperationSnapshot closedOperationSnapshot;

    private ClosedOperationSnapshot insertedClosedOperationSnapshot;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ClosedOperationSnapshot createEntity() {
        return new ClosedOperationSnapshot()
            .liaisonId(DEFAULT_LIAISON_ID)
            .operationId(DEFAULT_OPERATION_ID)
            .operationCode(DEFAULT_OPERATION_CODE)
            .referenceCode(DEFAULT_REFERENCE_CODE)
            .serviceCode(DEFAULT_SERVICE_CODE)
            .ownerId(DEFAULT_OWNER_ID)
            .validatorId(DEFAULT_VALIDATOR_ID)
            .evidenceDocumentId(DEFAULT_EVIDENCE_DOCUMENT_ID)
            .evidenceDigest(DEFAULT_EVIDENCE_DIGEST)
            .closedAt(DEFAULT_CLOSED_AT)
            .sourceEventId(DEFAULT_SOURCE_EVENT_ID);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ClosedOperationSnapshot createUpdatedEntity() {
        return new ClosedOperationSnapshot()
            .liaisonId(UPDATED_LIAISON_ID)
            .operationId(UPDATED_OPERATION_ID)
            .operationCode(UPDATED_OPERATION_CODE)
            .referenceCode(UPDATED_REFERENCE_CODE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .ownerId(UPDATED_OWNER_ID)
            .validatorId(UPDATED_VALIDATOR_ID)
            .evidenceDocumentId(UPDATED_EVIDENCE_DOCUMENT_ID)
            .evidenceDigest(UPDATED_EVIDENCE_DIGEST)
            .closedAt(UPDATED_CLOSED_AT)
            .sourceEventId(UPDATED_SOURCE_EVENT_ID);
    }

    @BeforeEach
    void initTest() {
        closedOperationSnapshot = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedClosedOperationSnapshot != null) {
            closedOperationSnapshotRepository.delete(insertedClosedOperationSnapshot);
            insertedClosedOperationSnapshot = null;
        }
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshots() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList
        restClosedOperationSnapshotMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(closedOperationSnapshot.getId().toString())))
            .andExpect(jsonPath("$.[*].liaisonId").value(hasItem(DEFAULT_LIAISON_ID.toString())))
            .andExpect(jsonPath("$.[*].operationId").value(hasItem(DEFAULT_OPERATION_ID.toString())))
            .andExpect(jsonPath("$.[*].operationCode").value(hasItem(DEFAULT_OPERATION_CODE)))
            .andExpect(jsonPath("$.[*].referenceCode").value(hasItem(DEFAULT_REFERENCE_CODE)))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].validatorId").value(hasItem(DEFAULT_VALIDATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDocumentId").value(hasItem(DEFAULT_EVIDENCE_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDigest").value(hasItem(DEFAULT_EVIDENCE_DIGEST)))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())))
            .andExpect(jsonPath("$.[*].sourceEventId").value(hasItem(DEFAULT_SOURCE_EVENT_ID.toString())));
    }

    @Test
    @Transactional
    void getClosedOperationSnapshot() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get the closedOperationSnapshot
        restClosedOperationSnapshotMockMvc
            .perform(get(ENTITY_API_URL_ID, closedOperationSnapshot.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(closedOperationSnapshot.getId().toString()))
            .andExpect(jsonPath("$.liaisonId").value(DEFAULT_LIAISON_ID.toString()))
            .andExpect(jsonPath("$.operationId").value(DEFAULT_OPERATION_ID.toString()))
            .andExpect(jsonPath("$.operationCode").value(DEFAULT_OPERATION_CODE))
            .andExpect(jsonPath("$.referenceCode").value(DEFAULT_REFERENCE_CODE))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.ownerId").value(DEFAULT_OWNER_ID.toString()))
            .andExpect(jsonPath("$.validatorId").value(DEFAULT_VALIDATOR_ID.toString()))
            .andExpect(jsonPath("$.evidenceDocumentId").value(DEFAULT_EVIDENCE_DOCUMENT_ID.toString()))
            .andExpect(jsonPath("$.evidenceDigest").value(DEFAULT_EVIDENCE_DIGEST))
            .andExpect(jsonPath("$.closedAt").value(DEFAULT_CLOSED_AT.toString()))
            .andExpect(jsonPath("$.sourceEventId").value(DEFAULT_SOURCE_EVENT_ID.toString()));
    }

    @Test
    @Transactional
    void getClosedOperationSnapshotsByIdFiltering() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        UUID id = closedOperationSnapshot.getId();

        defaultClosedOperationSnapshotFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByLiaisonIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where liaisonId equals to
        defaultClosedOperationSnapshotFiltering("liaisonId.equals=" + DEFAULT_LIAISON_ID, "liaisonId.equals=" + UPDATED_LIAISON_ID);
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByLiaisonIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where liaisonId in
        defaultClosedOperationSnapshotFiltering(
            "liaisonId.in=" + DEFAULT_LIAISON_ID + "," + UPDATED_LIAISON_ID,
            "liaisonId.in=" + UPDATED_LIAISON_ID
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByLiaisonIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where liaisonId is not null
        defaultClosedOperationSnapshotFiltering("liaisonId.specified=true", "liaisonId.specified=false");
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByOperationIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where operationId equals to
        defaultClosedOperationSnapshotFiltering("operationId.equals=" + DEFAULT_OPERATION_ID, "operationId.equals=" + UPDATED_OPERATION_ID);
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByOperationIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where operationId in
        defaultClosedOperationSnapshotFiltering(
            "operationId.in=" + DEFAULT_OPERATION_ID + "," + UPDATED_OPERATION_ID,
            "operationId.in=" + UPDATED_OPERATION_ID
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByOperationIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where operationId is not null
        defaultClosedOperationSnapshotFiltering("operationId.specified=true", "operationId.specified=false");
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByOperationCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where operationCode equals to
        defaultClosedOperationSnapshotFiltering(
            "operationCode.equals=" + DEFAULT_OPERATION_CODE,
            "operationCode.equals=" + UPDATED_OPERATION_CODE
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByOperationCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where operationCode in
        defaultClosedOperationSnapshotFiltering(
            "operationCode.in=" + DEFAULT_OPERATION_CODE + "," + UPDATED_OPERATION_CODE,
            "operationCode.in=" + UPDATED_OPERATION_CODE
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByOperationCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where operationCode is not null
        defaultClosedOperationSnapshotFiltering("operationCode.specified=true", "operationCode.specified=false");
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByOperationCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where operationCode contains
        defaultClosedOperationSnapshotFiltering(
            "operationCode.contains=" + DEFAULT_OPERATION_CODE,
            "operationCode.contains=" + UPDATED_OPERATION_CODE
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByOperationCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where operationCode does not contain
        defaultClosedOperationSnapshotFiltering(
            "operationCode.doesNotContain=" + UPDATED_OPERATION_CODE,
            "operationCode.doesNotContain=" + DEFAULT_OPERATION_CODE
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByReferenceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where referenceCode equals to
        defaultClosedOperationSnapshotFiltering(
            "referenceCode.equals=" + DEFAULT_REFERENCE_CODE,
            "referenceCode.equals=" + UPDATED_REFERENCE_CODE
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByReferenceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where referenceCode in
        defaultClosedOperationSnapshotFiltering(
            "referenceCode.in=" + DEFAULT_REFERENCE_CODE + "," + UPDATED_REFERENCE_CODE,
            "referenceCode.in=" + UPDATED_REFERENCE_CODE
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByReferenceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where referenceCode is not null
        defaultClosedOperationSnapshotFiltering("referenceCode.specified=true", "referenceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByReferenceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where referenceCode contains
        defaultClosedOperationSnapshotFiltering(
            "referenceCode.contains=" + DEFAULT_REFERENCE_CODE,
            "referenceCode.contains=" + UPDATED_REFERENCE_CODE
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByReferenceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where referenceCode does not contain
        defaultClosedOperationSnapshotFiltering(
            "referenceCode.doesNotContain=" + UPDATED_REFERENCE_CODE,
            "referenceCode.doesNotContain=" + DEFAULT_REFERENCE_CODE
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByServiceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where serviceCode equals to
        defaultClosedOperationSnapshotFiltering("serviceCode.equals=" + DEFAULT_SERVICE_CODE, "serviceCode.equals=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByServiceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where serviceCode in
        defaultClosedOperationSnapshotFiltering(
            "serviceCode.in=" + DEFAULT_SERVICE_CODE + "," + UPDATED_SERVICE_CODE,
            "serviceCode.in=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByServiceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where serviceCode is not null
        defaultClosedOperationSnapshotFiltering("serviceCode.specified=true", "serviceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByServiceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where serviceCode contains
        defaultClosedOperationSnapshotFiltering(
            "serviceCode.contains=" + DEFAULT_SERVICE_CODE,
            "serviceCode.contains=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByServiceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where serviceCode does not contain
        defaultClosedOperationSnapshotFiltering(
            "serviceCode.doesNotContain=" + UPDATED_SERVICE_CODE,
            "serviceCode.doesNotContain=" + DEFAULT_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByOwnerIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where ownerId equals to
        defaultClosedOperationSnapshotFiltering("ownerId.equals=" + DEFAULT_OWNER_ID, "ownerId.equals=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByOwnerIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where ownerId in
        defaultClosedOperationSnapshotFiltering(
            "ownerId.in=" + DEFAULT_OWNER_ID + "," + UPDATED_OWNER_ID,
            "ownerId.in=" + UPDATED_OWNER_ID
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByOwnerIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where ownerId is not null
        defaultClosedOperationSnapshotFiltering("ownerId.specified=true", "ownerId.specified=false");
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByValidatorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where validatorId equals to
        defaultClosedOperationSnapshotFiltering("validatorId.equals=" + DEFAULT_VALIDATOR_ID, "validatorId.equals=" + UPDATED_VALIDATOR_ID);
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByValidatorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where validatorId in
        defaultClosedOperationSnapshotFiltering(
            "validatorId.in=" + DEFAULT_VALIDATOR_ID + "," + UPDATED_VALIDATOR_ID,
            "validatorId.in=" + UPDATED_VALIDATOR_ID
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByValidatorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where validatorId is not null
        defaultClosedOperationSnapshotFiltering("validatorId.specified=true", "validatorId.specified=false");
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByEvidenceDocumentIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where evidenceDocumentId equals to
        defaultClosedOperationSnapshotFiltering(
            "evidenceDocumentId.equals=" + DEFAULT_EVIDENCE_DOCUMENT_ID,
            "evidenceDocumentId.equals=" + UPDATED_EVIDENCE_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByEvidenceDocumentIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where evidenceDocumentId in
        defaultClosedOperationSnapshotFiltering(
            "evidenceDocumentId.in=" + DEFAULT_EVIDENCE_DOCUMENT_ID + "," + UPDATED_EVIDENCE_DOCUMENT_ID,
            "evidenceDocumentId.in=" + UPDATED_EVIDENCE_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByEvidenceDocumentIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where evidenceDocumentId is not null
        defaultClosedOperationSnapshotFiltering("evidenceDocumentId.specified=true", "evidenceDocumentId.specified=false");
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByEvidenceDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where evidenceDigest equals to
        defaultClosedOperationSnapshotFiltering(
            "evidenceDigest.equals=" + DEFAULT_EVIDENCE_DIGEST,
            "evidenceDigest.equals=" + UPDATED_EVIDENCE_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByEvidenceDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where evidenceDigest in
        defaultClosedOperationSnapshotFiltering(
            "evidenceDigest.in=" + DEFAULT_EVIDENCE_DIGEST + "," + UPDATED_EVIDENCE_DIGEST,
            "evidenceDigest.in=" + UPDATED_EVIDENCE_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByEvidenceDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where evidenceDigest is not null
        defaultClosedOperationSnapshotFiltering("evidenceDigest.specified=true", "evidenceDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByEvidenceDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where evidenceDigest contains
        defaultClosedOperationSnapshotFiltering(
            "evidenceDigest.contains=" + DEFAULT_EVIDENCE_DIGEST,
            "evidenceDigest.contains=" + UPDATED_EVIDENCE_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByEvidenceDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where evidenceDigest does not contain
        defaultClosedOperationSnapshotFiltering(
            "evidenceDigest.doesNotContain=" + UPDATED_EVIDENCE_DIGEST,
            "evidenceDigest.doesNotContain=" + DEFAULT_EVIDENCE_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByClosedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where closedAt equals to
        defaultClosedOperationSnapshotFiltering("closedAt.equals=" + DEFAULT_CLOSED_AT, "closedAt.equals=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByClosedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where closedAt in
        defaultClosedOperationSnapshotFiltering(
            "closedAt.in=" + DEFAULT_CLOSED_AT + "," + UPDATED_CLOSED_AT,
            "closedAt.in=" + UPDATED_CLOSED_AT
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsByClosedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where closedAt is not null
        defaultClosedOperationSnapshotFiltering("closedAt.specified=true", "closedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsBySourceEventIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where sourceEventId equals to
        defaultClosedOperationSnapshotFiltering(
            "sourceEventId.equals=" + DEFAULT_SOURCE_EVENT_ID,
            "sourceEventId.equals=" + UPDATED_SOURCE_EVENT_ID
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsBySourceEventIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where sourceEventId in
        defaultClosedOperationSnapshotFiltering(
            "sourceEventId.in=" + DEFAULT_SOURCE_EVENT_ID + "," + UPDATED_SOURCE_EVENT_ID,
            "sourceEventId.in=" + UPDATED_SOURCE_EVENT_ID
        );
    }

    @Test
    @Transactional
    void getAllClosedOperationSnapshotsBySourceEventIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedClosedOperationSnapshot = closedOperationSnapshotRepository.saveAndFlush(closedOperationSnapshot);

        // Get all the closedOperationSnapshotList where sourceEventId is not null
        defaultClosedOperationSnapshotFiltering("sourceEventId.specified=true", "sourceEventId.specified=false");
    }

    private void defaultClosedOperationSnapshotFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultClosedOperationSnapshotShouldBeFound(shouldBeFound);
        defaultClosedOperationSnapshotShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultClosedOperationSnapshotShouldBeFound(String filter) throws Exception {
        restClosedOperationSnapshotMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(closedOperationSnapshot.getId().toString())))
            .andExpect(jsonPath("$.[*].liaisonId").value(hasItem(DEFAULT_LIAISON_ID.toString())))
            .andExpect(jsonPath("$.[*].operationId").value(hasItem(DEFAULT_OPERATION_ID.toString())))
            .andExpect(jsonPath("$.[*].operationCode").value(hasItem(DEFAULT_OPERATION_CODE)))
            .andExpect(jsonPath("$.[*].referenceCode").value(hasItem(DEFAULT_REFERENCE_CODE)))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].validatorId").value(hasItem(DEFAULT_VALIDATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDocumentId").value(hasItem(DEFAULT_EVIDENCE_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDigest").value(hasItem(DEFAULT_EVIDENCE_DIGEST)))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())))
            .andExpect(jsonPath("$.[*].sourceEventId").value(hasItem(DEFAULT_SOURCE_EVENT_ID.toString())));

        // Check, that the count call also returns 1
        restClosedOperationSnapshotMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultClosedOperationSnapshotShouldNotBeFound(String filter) throws Exception {
        restClosedOperationSnapshotMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restClosedOperationSnapshotMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingClosedOperationSnapshot() throws Exception {
        // Get the closedOperationSnapshot
        restClosedOperationSnapshotMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return closedOperationSnapshotRepository.count();
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

    protected ClosedOperationSnapshot getPersistedClosedOperationSnapshot(ClosedOperationSnapshot closedOperationSnapshot) {
        return closedOperationSnapshotRepository.findById(closedOperationSnapshot.getId()).orElseThrow();
    }

    protected void assertPersistedClosedOperationSnapshotToMatchAllProperties(ClosedOperationSnapshot expectedClosedOperationSnapshot) {
        assertClosedOperationSnapshotAllPropertiesEquals(
            expectedClosedOperationSnapshot,
            getPersistedClosedOperationSnapshot(expectedClosedOperationSnapshot)
        );
    }

    protected void assertPersistedClosedOperationSnapshotToMatchUpdatableProperties(
        ClosedOperationSnapshot expectedClosedOperationSnapshot
    ) {
        assertClosedOperationSnapshotAllUpdatablePropertiesEquals(
            expectedClosedOperationSnapshot,
            getPersistedClosedOperationSnapshot(expectedClosedOperationSnapshot)
        );
    }
}
