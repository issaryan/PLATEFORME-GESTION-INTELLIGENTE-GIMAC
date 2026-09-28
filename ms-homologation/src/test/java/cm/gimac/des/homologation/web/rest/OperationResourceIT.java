package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.OperationAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.Operation;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.enumeration.OperationState;
import cm.gimac.des.homologation.repository.OperationRepository;
import cm.gimac.des.homologation.service.OperationService;
import cm.gimac.des.homologation.service.mapper.OperationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
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
 * Integration tests for the {@link OperationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class OperationResourceIT {

    private static final String DEFAULT_OPERATION_CODE = "OPE-6767-2414";
    private static final String UPDATED_OPERATION_CODE = "OPE-1830-9097773";

    private static final OperationState DEFAULT_STATE = OperationState.VERROUILLEE;
    private static final OperationState UPDATED_STATE = OperationState.ELIGIBLE;

    private static final UUID DEFAULT_OWNER_ID = UUID.randomUUID();
    private static final UUID UPDATED_OWNER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_EVIDENCE_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_EVIDENCE_DOCUMENT_ID = UUID.randomUUID();

    private static final String DEFAULT_EVIDENCE_DIGEST = "caeba21bdf8ce39f468c2964568ae775c9d4196c040675750c235e6c00e46e48";
    private static final String UPDATED_EVIDENCE_DIGEST = "8f70d99483f12ff15e93d55ac57da5c94e71c38e5b4edefb541ad6af2a23e23c";

    private static final Instant DEFAULT_EVIDENCE_SEALED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EVIDENCE_SEALED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Duration DEFAULT_EVIDENCE_RETENTION = Duration.ofHours(6);
    private static final Duration UPDATED_EVIDENCE_RETENTION = Duration.ofHours(12);
    private static final Duration SMALLER_EVIDENCE_RETENTION = Duration.ofHours(5);

    private static final Boolean DEFAULT_EVIDENCE_SEAL_CONFIRMED = false;
    private static final Boolean UPDATED_EVIDENCE_SEAL_CONFIRMED = true;

    private static final String DEFAULT_EVIDENCE_TRACE_REF = "AAAAAAAAAA";
    private static final String UPDATED_EVIDENCE_TRACE_REF = "BBBBBBBBBB";

    private static final byte[] DEFAULT_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/png";

    private static final String DEFAULT_TIMESTAMP_IMPRINT = "ef747116ebcde57f6b2dd4f3e849be3211c6c17ff88b5c49a0e34a38415e0f5f";
    private static final String UPDATED_TIMESTAMP_IMPRINT = "806caf47acdcfe2d6325df0d64b17207f812b44bcc4b842ee5f3b3f3a0aa6809";

    private static final Instant DEFAULT_TIMESTAMP_GEN_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_TIMESTAMP_GEN_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_VALIDATOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_VALIDATOR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_VALIDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_VALIDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_CLOSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLOSED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_STATE_REASON = "AAAAAAAAAA";
    private static final String UPDATED_STATE_REASON = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/operations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OperationRepository operationRepository;

    @Mock
    private OperationRepository operationRepositoryMock;

    @Autowired
    private OperationMapper operationMapper;

    @Mock
    private OperationService operationServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOperationMockMvc;

    private Operation operation;

    private Operation insertedOperation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Operation createEntity(EntityManager em) {
        Operation operation = new Operation()
            .operationCode(DEFAULT_OPERATION_CODE)
            .state(DEFAULT_STATE)
            .ownerId(DEFAULT_OWNER_ID)
            .evidenceDocumentId(DEFAULT_EVIDENCE_DOCUMENT_ID)
            .evidenceDigest(DEFAULT_EVIDENCE_DIGEST)
            .evidenceSealedAt(DEFAULT_EVIDENCE_SEALED_AT)
            .evidenceRetention(DEFAULT_EVIDENCE_RETENTION)
            .evidenceSealConfirmed(DEFAULT_EVIDENCE_SEAL_CONFIRMED)
            .evidenceTraceRef(DEFAULT_EVIDENCE_TRACE_REF)
            .timestampToken(DEFAULT_TIMESTAMP_TOKEN)
            .timestampTokenContentType(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .timestampImprint(DEFAULT_TIMESTAMP_IMPRINT)
            .timestampGenTime(DEFAULT_TIMESTAMP_GEN_TIME)
            .validatorId(DEFAULT_VALIDATOR_ID)
            .validatedAt(DEFAULT_VALIDATED_AT)
            .closedAt(DEFAULT_CLOSED_AT)
            .stateReason(DEFAULT_STATE_REASON);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        operation.setLiaison(liaison);
        // Add required entity
        ReferenceOperation referenceOperation;
        if (TestUtil.findAll(em, ReferenceOperation.class).isEmpty()) {
            referenceOperation = ReferenceOperationResourceIT.createEntity(em);
            em.persist(referenceOperation);
            em.flush();
        } else {
            referenceOperation = TestUtil.findAll(em, ReferenceOperation.class).get(0);
        }
        operation.setReference(referenceOperation);
        return operation;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Operation createUpdatedEntity(EntityManager em) {
        Operation updatedOperation = new Operation()
            .operationCode(UPDATED_OPERATION_CODE)
            .state(UPDATED_STATE)
            .ownerId(UPDATED_OWNER_ID)
            .evidenceDocumentId(UPDATED_EVIDENCE_DOCUMENT_ID)
            .evidenceDigest(UPDATED_EVIDENCE_DIGEST)
            .evidenceSealedAt(UPDATED_EVIDENCE_SEALED_AT)
            .evidenceRetention(UPDATED_EVIDENCE_RETENTION)
            .evidenceSealConfirmed(UPDATED_EVIDENCE_SEAL_CONFIRMED)
            .evidenceTraceRef(UPDATED_EVIDENCE_TRACE_REF)
            .timestampToken(UPDATED_TIMESTAMP_TOKEN)
            .timestampTokenContentType(UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .timestampImprint(UPDATED_TIMESTAMP_IMPRINT)
            .timestampGenTime(UPDATED_TIMESTAMP_GEN_TIME)
            .validatorId(UPDATED_VALIDATOR_ID)
            .validatedAt(UPDATED_VALIDATED_AT)
            .closedAt(UPDATED_CLOSED_AT)
            .stateReason(UPDATED_STATE_REASON);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        updatedOperation.setLiaison(liaison);
        // Add required entity
        ReferenceOperation referenceOperation;
        if (TestUtil.findAll(em, ReferenceOperation.class).isEmpty()) {
            referenceOperation = ReferenceOperationResourceIT.createUpdatedEntity(em);
            em.persist(referenceOperation);
            em.flush();
        } else {
            referenceOperation = TestUtil.findAll(em, ReferenceOperation.class).get(0);
        }
        updatedOperation.setReference(referenceOperation);
        return updatedOperation;
    }

    @BeforeEach
    void initTest() {
        operation = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedOperation != null) {
            operationRepository.delete(insertedOperation);
            insertedOperation = null;
        }
    }

    @Test
    @Transactional
    void getAllOperations() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList
        restOperationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(operation.getId().toString())))
            .andExpect(jsonPath("$.[*].operationCode").value(hasItem(DEFAULT_OPERATION_CODE)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDocumentId").value(hasItem(DEFAULT_EVIDENCE_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDigest").value(hasItem(DEFAULT_EVIDENCE_DIGEST)))
            .andExpect(jsonPath("$.[*].evidenceSealedAt").value(hasItem(DEFAULT_EVIDENCE_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].evidenceRetention").value(hasItem(DEFAULT_EVIDENCE_RETENTION.toString())))
            .andExpect(jsonPath("$.[*].evidenceSealConfirmed").value(hasItem(DEFAULT_EVIDENCE_SEAL_CONFIRMED)))
            .andExpect(jsonPath("$.[*].evidenceTraceRef").value(hasItem(DEFAULT_EVIDENCE_TRACE_REF)))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].timestampImprint").value(hasItem(DEFAULT_TIMESTAMP_IMPRINT)))
            .andExpect(jsonPath("$.[*].timestampGenTime").value(hasItem(DEFAULT_TIMESTAMP_GEN_TIME.toString())))
            .andExpect(jsonPath("$.[*].validatorId").value(hasItem(DEFAULT_VALIDATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].validatedAt").value(hasItem(DEFAULT_VALIDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())))
            .andExpect(jsonPath("$.[*].stateReason").value(hasItem(DEFAULT_STATE_REASON)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOperationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(operationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOperationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(operationServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOperationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(operationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOperationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(operationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOperation() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get the operation
        restOperationMockMvc
            .perform(get(ENTITY_API_URL_ID, operation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(operation.getId().toString()))
            .andExpect(jsonPath("$.operationCode").value(DEFAULT_OPERATION_CODE))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.ownerId").value(DEFAULT_OWNER_ID.toString()))
            .andExpect(jsonPath("$.evidenceDocumentId").value(DEFAULT_EVIDENCE_DOCUMENT_ID.toString()))
            .andExpect(jsonPath("$.evidenceDigest").value(DEFAULT_EVIDENCE_DIGEST))
            .andExpect(jsonPath("$.evidenceSealedAt").value(DEFAULT_EVIDENCE_SEALED_AT.toString()))
            .andExpect(jsonPath("$.evidenceRetention").value(DEFAULT_EVIDENCE_RETENTION.toString()))
            .andExpect(jsonPath("$.evidenceSealConfirmed").value(DEFAULT_EVIDENCE_SEAL_CONFIRMED))
            .andExpect(jsonPath("$.evidenceTraceRef").value(DEFAULT_EVIDENCE_TRACE_REF))
            .andExpect(jsonPath("$.timestampTokenContentType").value(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE))
            .andExpect(jsonPath("$.timestampToken").value(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN)))
            .andExpect(jsonPath("$.timestampImprint").value(DEFAULT_TIMESTAMP_IMPRINT))
            .andExpect(jsonPath("$.timestampGenTime").value(DEFAULT_TIMESTAMP_GEN_TIME.toString()))
            .andExpect(jsonPath("$.validatorId").value(DEFAULT_VALIDATOR_ID.toString()))
            .andExpect(jsonPath("$.validatedAt").value(DEFAULT_VALIDATED_AT.toString()))
            .andExpect(jsonPath("$.closedAt").value(DEFAULT_CLOSED_AT.toString()))
            .andExpect(jsonPath("$.stateReason").value(DEFAULT_STATE_REASON));
    }

    @Test
    @Transactional
    void getOperationsByIdFiltering() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        UUID id = operation.getId();

        defaultOperationFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllOperationsByOperationCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where operationCode equals to
        defaultOperationFiltering("operationCode.equals=" + DEFAULT_OPERATION_CODE, "operationCode.equals=" + UPDATED_OPERATION_CODE);
    }

    @Test
    @Transactional
    void getAllOperationsByOperationCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where operationCode in
        defaultOperationFiltering(
            "operationCode.in=" + DEFAULT_OPERATION_CODE + "," + UPDATED_OPERATION_CODE,
            "operationCode.in=" + UPDATED_OPERATION_CODE
        );
    }

    @Test
    @Transactional
    void getAllOperationsByOperationCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where operationCode is not null
        defaultOperationFiltering("operationCode.specified=true", "operationCode.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByOperationCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where operationCode contains
        defaultOperationFiltering("operationCode.contains=" + DEFAULT_OPERATION_CODE, "operationCode.contains=" + UPDATED_OPERATION_CODE);
    }

    @Test
    @Transactional
    void getAllOperationsByOperationCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where operationCode does not contain
        defaultOperationFiltering(
            "operationCode.doesNotContain=" + UPDATED_OPERATION_CODE,
            "operationCode.doesNotContain=" + DEFAULT_OPERATION_CODE
        );
    }

    @Test
    @Transactional
    void getAllOperationsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where state equals to
        defaultOperationFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllOperationsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where state in
        defaultOperationFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllOperationsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where state is not null
        defaultOperationFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByOwnerIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where ownerId equals to
        defaultOperationFiltering("ownerId.equals=" + DEFAULT_OWNER_ID, "ownerId.equals=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllOperationsByOwnerIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where ownerId in
        defaultOperationFiltering("ownerId.in=" + DEFAULT_OWNER_ID + "," + UPDATED_OWNER_ID, "ownerId.in=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllOperationsByOwnerIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where ownerId is not null
        defaultOperationFiltering("ownerId.specified=true", "ownerId.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceDocumentIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceDocumentId equals to
        defaultOperationFiltering(
            "evidenceDocumentId.equals=" + DEFAULT_EVIDENCE_DOCUMENT_ID,
            "evidenceDocumentId.equals=" + UPDATED_EVIDENCE_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceDocumentIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceDocumentId in
        defaultOperationFiltering(
            "evidenceDocumentId.in=" + DEFAULT_EVIDENCE_DOCUMENT_ID + "," + UPDATED_EVIDENCE_DOCUMENT_ID,
            "evidenceDocumentId.in=" + UPDATED_EVIDENCE_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceDocumentIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceDocumentId is not null
        defaultOperationFiltering("evidenceDocumentId.specified=true", "evidenceDocumentId.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceDigest equals to
        defaultOperationFiltering("evidenceDigest.equals=" + DEFAULT_EVIDENCE_DIGEST, "evidenceDigest.equals=" + UPDATED_EVIDENCE_DIGEST);
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceDigest in
        defaultOperationFiltering(
            "evidenceDigest.in=" + DEFAULT_EVIDENCE_DIGEST + "," + UPDATED_EVIDENCE_DIGEST,
            "evidenceDigest.in=" + UPDATED_EVIDENCE_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceDigest is not null
        defaultOperationFiltering("evidenceDigest.specified=true", "evidenceDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceDigest contains
        defaultOperationFiltering(
            "evidenceDigest.contains=" + DEFAULT_EVIDENCE_DIGEST,
            "evidenceDigest.contains=" + UPDATED_EVIDENCE_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceDigest does not contain
        defaultOperationFiltering(
            "evidenceDigest.doesNotContain=" + UPDATED_EVIDENCE_DIGEST,
            "evidenceDigest.doesNotContain=" + DEFAULT_EVIDENCE_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceSealedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceSealedAt equals to
        defaultOperationFiltering(
            "evidenceSealedAt.equals=" + DEFAULT_EVIDENCE_SEALED_AT,
            "evidenceSealedAt.equals=" + UPDATED_EVIDENCE_SEALED_AT
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceSealedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceSealedAt in
        defaultOperationFiltering(
            "evidenceSealedAt.in=" + DEFAULT_EVIDENCE_SEALED_AT + "," + UPDATED_EVIDENCE_SEALED_AT,
            "evidenceSealedAt.in=" + UPDATED_EVIDENCE_SEALED_AT
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceSealedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceSealedAt is not null
        defaultOperationFiltering("evidenceSealedAt.specified=true", "evidenceSealedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceRetentionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceRetention equals to
        defaultOperationFiltering(
            "evidenceRetention.equals=" + DEFAULT_EVIDENCE_RETENTION,
            "evidenceRetention.equals=" + UPDATED_EVIDENCE_RETENTION
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceRetentionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceRetention in
        defaultOperationFiltering(
            "evidenceRetention.in=" + DEFAULT_EVIDENCE_RETENTION + "," + UPDATED_EVIDENCE_RETENTION,
            "evidenceRetention.in=" + UPDATED_EVIDENCE_RETENTION
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceRetentionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceRetention is not null
        defaultOperationFiltering("evidenceRetention.specified=true", "evidenceRetention.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceRetentionIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceRetention is greater than or equal to
        defaultOperationFiltering(
            "evidenceRetention.greaterThanOrEqual=" + DEFAULT_EVIDENCE_RETENTION,
            "evidenceRetention.greaterThanOrEqual=" + UPDATED_EVIDENCE_RETENTION
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceRetentionIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceRetention is less than or equal to
        defaultOperationFiltering(
            "evidenceRetention.lessThanOrEqual=" + DEFAULT_EVIDENCE_RETENTION,
            "evidenceRetention.lessThanOrEqual=" + SMALLER_EVIDENCE_RETENTION
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceRetentionIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceRetention is less than
        defaultOperationFiltering(
            "evidenceRetention.lessThan=" + UPDATED_EVIDENCE_RETENTION,
            "evidenceRetention.lessThan=" + DEFAULT_EVIDENCE_RETENTION
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceRetentionIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceRetention is greater than
        defaultOperationFiltering(
            "evidenceRetention.greaterThan=" + SMALLER_EVIDENCE_RETENTION,
            "evidenceRetention.greaterThan=" + DEFAULT_EVIDENCE_RETENTION
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceSealConfirmedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceSealConfirmed equals to
        defaultOperationFiltering(
            "evidenceSealConfirmed.equals=" + DEFAULT_EVIDENCE_SEAL_CONFIRMED,
            "evidenceSealConfirmed.equals=" + UPDATED_EVIDENCE_SEAL_CONFIRMED
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceSealConfirmedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceSealConfirmed in
        defaultOperationFiltering(
            "evidenceSealConfirmed.in=" + DEFAULT_EVIDENCE_SEAL_CONFIRMED + "," + UPDATED_EVIDENCE_SEAL_CONFIRMED,
            "evidenceSealConfirmed.in=" + UPDATED_EVIDENCE_SEAL_CONFIRMED
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceSealConfirmedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceSealConfirmed is not null
        defaultOperationFiltering("evidenceSealConfirmed.specified=true", "evidenceSealConfirmed.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceTraceRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceTraceRef equals to
        defaultOperationFiltering(
            "evidenceTraceRef.equals=" + DEFAULT_EVIDENCE_TRACE_REF,
            "evidenceTraceRef.equals=" + UPDATED_EVIDENCE_TRACE_REF
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceTraceRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceTraceRef in
        defaultOperationFiltering(
            "evidenceTraceRef.in=" + DEFAULT_EVIDENCE_TRACE_REF + "," + UPDATED_EVIDENCE_TRACE_REF,
            "evidenceTraceRef.in=" + UPDATED_EVIDENCE_TRACE_REF
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceTraceRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceTraceRef is not null
        defaultOperationFiltering("evidenceTraceRef.specified=true", "evidenceTraceRef.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceTraceRefContainsSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceTraceRef contains
        defaultOperationFiltering(
            "evidenceTraceRef.contains=" + DEFAULT_EVIDENCE_TRACE_REF,
            "evidenceTraceRef.contains=" + UPDATED_EVIDENCE_TRACE_REF
        );
    }

    @Test
    @Transactional
    void getAllOperationsByEvidenceTraceRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where evidenceTraceRef does not contain
        defaultOperationFiltering(
            "evidenceTraceRef.doesNotContain=" + UPDATED_EVIDENCE_TRACE_REF,
            "evidenceTraceRef.doesNotContain=" + DEFAULT_EVIDENCE_TRACE_REF
        );
    }

    @Test
    @Transactional
    void getAllOperationsByTimestampImprintIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where timestampImprint equals to
        defaultOperationFiltering(
            "timestampImprint.equals=" + DEFAULT_TIMESTAMP_IMPRINT,
            "timestampImprint.equals=" + UPDATED_TIMESTAMP_IMPRINT
        );
    }

    @Test
    @Transactional
    void getAllOperationsByTimestampImprintIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where timestampImprint in
        defaultOperationFiltering(
            "timestampImprint.in=" + DEFAULT_TIMESTAMP_IMPRINT + "," + UPDATED_TIMESTAMP_IMPRINT,
            "timestampImprint.in=" + UPDATED_TIMESTAMP_IMPRINT
        );
    }

    @Test
    @Transactional
    void getAllOperationsByTimestampImprintIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where timestampImprint is not null
        defaultOperationFiltering("timestampImprint.specified=true", "timestampImprint.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByTimestampImprintContainsSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where timestampImprint contains
        defaultOperationFiltering(
            "timestampImprint.contains=" + DEFAULT_TIMESTAMP_IMPRINT,
            "timestampImprint.contains=" + UPDATED_TIMESTAMP_IMPRINT
        );
    }

    @Test
    @Transactional
    void getAllOperationsByTimestampImprintNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where timestampImprint does not contain
        defaultOperationFiltering(
            "timestampImprint.doesNotContain=" + UPDATED_TIMESTAMP_IMPRINT,
            "timestampImprint.doesNotContain=" + DEFAULT_TIMESTAMP_IMPRINT
        );
    }

    @Test
    @Transactional
    void getAllOperationsByTimestampGenTimeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where timestampGenTime equals to
        defaultOperationFiltering(
            "timestampGenTime.equals=" + DEFAULT_TIMESTAMP_GEN_TIME,
            "timestampGenTime.equals=" + UPDATED_TIMESTAMP_GEN_TIME
        );
    }

    @Test
    @Transactional
    void getAllOperationsByTimestampGenTimeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where timestampGenTime in
        defaultOperationFiltering(
            "timestampGenTime.in=" + DEFAULT_TIMESTAMP_GEN_TIME + "," + UPDATED_TIMESTAMP_GEN_TIME,
            "timestampGenTime.in=" + UPDATED_TIMESTAMP_GEN_TIME
        );
    }

    @Test
    @Transactional
    void getAllOperationsByTimestampGenTimeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where timestampGenTime is not null
        defaultOperationFiltering("timestampGenTime.specified=true", "timestampGenTime.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByValidatorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where validatorId equals to
        defaultOperationFiltering("validatorId.equals=" + DEFAULT_VALIDATOR_ID, "validatorId.equals=" + UPDATED_VALIDATOR_ID);
    }

    @Test
    @Transactional
    void getAllOperationsByValidatorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where validatorId in
        defaultOperationFiltering(
            "validatorId.in=" + DEFAULT_VALIDATOR_ID + "," + UPDATED_VALIDATOR_ID,
            "validatorId.in=" + UPDATED_VALIDATOR_ID
        );
    }

    @Test
    @Transactional
    void getAllOperationsByValidatorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where validatorId is not null
        defaultOperationFiltering("validatorId.specified=true", "validatorId.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByValidatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where validatedAt equals to
        defaultOperationFiltering("validatedAt.equals=" + DEFAULT_VALIDATED_AT, "validatedAt.equals=" + UPDATED_VALIDATED_AT);
    }

    @Test
    @Transactional
    void getAllOperationsByValidatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where validatedAt in
        defaultOperationFiltering(
            "validatedAt.in=" + DEFAULT_VALIDATED_AT + "," + UPDATED_VALIDATED_AT,
            "validatedAt.in=" + UPDATED_VALIDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllOperationsByValidatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where validatedAt is not null
        defaultOperationFiltering("validatedAt.specified=true", "validatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByClosedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where closedAt equals to
        defaultOperationFiltering("closedAt.equals=" + DEFAULT_CLOSED_AT, "closedAt.equals=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllOperationsByClosedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where closedAt in
        defaultOperationFiltering("closedAt.in=" + DEFAULT_CLOSED_AT + "," + UPDATED_CLOSED_AT, "closedAt.in=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllOperationsByClosedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where closedAt is not null
        defaultOperationFiltering("closedAt.specified=true", "closedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByStateReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where stateReason equals to
        defaultOperationFiltering("stateReason.equals=" + DEFAULT_STATE_REASON, "stateReason.equals=" + UPDATED_STATE_REASON);
    }

    @Test
    @Transactional
    void getAllOperationsByStateReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where stateReason in
        defaultOperationFiltering(
            "stateReason.in=" + DEFAULT_STATE_REASON + "," + UPDATED_STATE_REASON,
            "stateReason.in=" + UPDATED_STATE_REASON
        );
    }

    @Test
    @Transactional
    void getAllOperationsByStateReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where stateReason is not null
        defaultOperationFiltering("stateReason.specified=true", "stateReason.specified=false");
    }

    @Test
    @Transactional
    void getAllOperationsByStateReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where stateReason contains
        defaultOperationFiltering("stateReason.contains=" + DEFAULT_STATE_REASON, "stateReason.contains=" + UPDATED_STATE_REASON);
    }

    @Test
    @Transactional
    void getAllOperationsByStateReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOperation = operationRepository.saveAndFlush(operation);

        // Get all the operationList where stateReason does not contain
        defaultOperationFiltering(
            "stateReason.doesNotContain=" + UPDATED_STATE_REASON,
            "stateReason.doesNotContain=" + DEFAULT_STATE_REASON
        );
    }

    @Test
    @Transactional
    void getAllOperationsByLiaisonIsEqualToSomething() throws Exception {
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            operationRepository.saveAndFlush(operation);
            liaison = LiaisonResourceIT.createEntity(em);
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        em.persist(liaison);
        em.flush();
        operation.setLiaison(liaison);
        operationRepository.saveAndFlush(operation);
        UUID liaisonId = liaison.getId();
        // Get all the operationList where liaison equals to liaisonId
        defaultOperationShouldBeFound("liaisonId.equals=" + liaisonId);

        // Get all the operationList where liaison equals to UUID.randomUUID()
        defaultOperationShouldNotBeFound("liaisonId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllOperationsByReferenceIsEqualToSomething() throws Exception {
        ReferenceOperation reference;
        if (TestUtil.findAll(em, ReferenceOperation.class).isEmpty()) {
            operationRepository.saveAndFlush(operation);
            reference = ReferenceOperationResourceIT.createEntity(em);
        } else {
            reference = TestUtil.findAll(em, ReferenceOperation.class).get(0);
        }
        em.persist(reference);
        em.flush();
        operation.setReference(reference);
        operationRepository.saveAndFlush(operation);
        UUID referenceId = reference.getId();
        // Get all the operationList where reference equals to referenceId
        defaultOperationShouldBeFound("referenceId.equals=" + referenceId);

        // Get all the operationList where reference equals to UUID.randomUUID()
        defaultOperationShouldNotBeFound("referenceId.equals=" + UUID.randomUUID());
    }

    private void defaultOperationFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultOperationShouldBeFound(shouldBeFound);
        defaultOperationShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultOperationShouldBeFound(String filter) throws Exception {
        restOperationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(operation.getId().toString())))
            .andExpect(jsonPath("$.[*].operationCode").value(hasItem(DEFAULT_OPERATION_CODE)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDocumentId").value(hasItem(DEFAULT_EVIDENCE_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDigest").value(hasItem(DEFAULT_EVIDENCE_DIGEST)))
            .andExpect(jsonPath("$.[*].evidenceSealedAt").value(hasItem(DEFAULT_EVIDENCE_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].evidenceRetention").value(hasItem(DEFAULT_EVIDENCE_RETENTION.toString())))
            .andExpect(jsonPath("$.[*].evidenceSealConfirmed").value(hasItem(DEFAULT_EVIDENCE_SEAL_CONFIRMED)))
            .andExpect(jsonPath("$.[*].evidenceTraceRef").value(hasItem(DEFAULT_EVIDENCE_TRACE_REF)))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].timestampImprint").value(hasItem(DEFAULT_TIMESTAMP_IMPRINT)))
            .andExpect(jsonPath("$.[*].timestampGenTime").value(hasItem(DEFAULT_TIMESTAMP_GEN_TIME.toString())))
            .andExpect(jsonPath("$.[*].validatorId").value(hasItem(DEFAULT_VALIDATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].validatedAt").value(hasItem(DEFAULT_VALIDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())))
            .andExpect(jsonPath("$.[*].stateReason").value(hasItem(DEFAULT_STATE_REASON)));

        // Check, that the count call also returns 1
        restOperationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultOperationShouldNotBeFound(String filter) throws Exception {
        restOperationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restOperationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingOperation() throws Exception {
        // Get the operation
        restOperationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return operationRepository.count();
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

    protected Operation getPersistedOperation(Operation operation) {
        return operationRepository.findById(operation.getId()).orElseThrow();
    }

    protected void assertPersistedOperationToMatchAllProperties(Operation expectedOperation) {
        assertOperationAllPropertiesEquals(expectedOperation, getPersistedOperation(expectedOperation));
    }

    protected void assertPersistedOperationToMatchUpdatableProperties(Operation expectedOperation) {
        assertOperationAllUpdatablePropertiesEquals(expectedOperation, getPersistedOperation(expectedOperation));
    }
}
