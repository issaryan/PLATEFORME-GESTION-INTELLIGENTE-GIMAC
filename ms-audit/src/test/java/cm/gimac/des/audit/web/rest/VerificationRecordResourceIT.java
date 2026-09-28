package cm.gimac.des.audit.web.rest;

import static cm.gimac.des.audit.domain.VerificationRecordAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.audit.IntegrationTest;
import cm.gimac.des.audit.domain.InclusionProof;
import cm.gimac.des.audit.domain.VerificationRecord;
import cm.gimac.des.audit.domain.enumeration.VerificationResult;
import cm.gimac.des.audit.repository.VerificationRecordRepository;
import cm.gimac.des.audit.service.mapper.VerificationRecordMapper;
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
 * Integration tests for the {@link VerificationRecordResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class VerificationRecordResourceIT {

    private static final UUID DEFAULT_VERIFIER_ID = UUID.randomUUID();
    private static final UUID UPDATED_VERIFIER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_VERIFIED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_VERIFIED_AT = Instant.ofEpochMilli(1701661978853L);

    private static final String DEFAULT_EXPECTED_ROOT = "dd1a290cfd13cae770c63b2eeb9e0fa224a1bc0955899e8084f4e7cc015e2298";
    private static final String UPDATED_EXPECTED_ROOT = "f0b60c0d9db91f3325404c226284e0466b87357135103484202cc87f7cca279d";

    private static final String DEFAULT_RECOMPUTED_ROOT = "a80ee4007f9927e972c11c4b8505c5b4d4c4104410c9ef071aa754e02690104d";
    private static final String UPDATED_RECOMPUTED_ROOT = "390fa4ab19eff081e6ce062993948cd9b5b9203df30cfbae1d138c9ce4d1f802";

    private static final Boolean DEFAULT_TOKEN_VALID = false;
    private static final Boolean UPDATED_TOKEN_VALID = true;

    private static final VerificationResult DEFAULT_RESULT = VerificationResult.EN_ATTENTE;
    private static final VerificationResult UPDATED_RESULT = VerificationResult.CONFORME;

    private static final String DEFAULT_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_COMMENT = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/verification-records";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private VerificationRecordRepository verificationRecordRepository;

    @Autowired
    private VerificationRecordMapper verificationRecordMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restVerificationRecordMockMvc;

    private VerificationRecord verificationRecord;

    private VerificationRecord insertedVerificationRecord;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static VerificationRecord createEntity(EntityManager em) {
        VerificationRecord verificationRecord = new VerificationRecord()
            .verifierId(DEFAULT_VERIFIER_ID)
            .verifiedAt(DEFAULT_VERIFIED_AT)
            .expectedRoot(DEFAULT_EXPECTED_ROOT)
            .recomputedRoot(DEFAULT_RECOMPUTED_ROOT)
            .tokenValid(DEFAULT_TOKEN_VALID)
            .result(DEFAULT_RESULT)
            .comment(DEFAULT_COMMENT);
        // Add required entity
        InclusionProof inclusionProof;
        if (TestUtil.findAll(em, InclusionProof.class).isEmpty()) {
            inclusionProof = InclusionProofResourceIT.createEntity(em);
            em.persist(inclusionProof);
            em.flush();
        } else {
            inclusionProof = TestUtil.findAll(em, InclusionProof.class).getFirst();
        }
        verificationRecord.setProof(inclusionProof);
        return verificationRecord;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static VerificationRecord createUpdatedEntity(EntityManager em) {
        VerificationRecord updatedVerificationRecord = new VerificationRecord()
            .verifierId(UPDATED_VERIFIER_ID)
            .verifiedAt(UPDATED_VERIFIED_AT)
            .expectedRoot(UPDATED_EXPECTED_ROOT)
            .recomputedRoot(UPDATED_RECOMPUTED_ROOT)
            .tokenValid(UPDATED_TOKEN_VALID)
            .result(UPDATED_RESULT)
            .comment(UPDATED_COMMENT);
        // Add required entity
        InclusionProof inclusionProof;
        if (TestUtil.findAll(em, InclusionProof.class).isEmpty()) {
            inclusionProof = InclusionProofResourceIT.createUpdatedEntity(em);
            em.persist(inclusionProof);
            em.flush();
        } else {
            inclusionProof = TestUtil.findAll(em, InclusionProof.class).getFirst();
        }
        updatedVerificationRecord.setProof(inclusionProof);
        return updatedVerificationRecord;
    }

    @BeforeEach
    void initTest() {
        verificationRecord = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedVerificationRecord != null) {
            verificationRecordRepository.delete(insertedVerificationRecord);
            insertedVerificationRecord = null;
        }
    }

    @Test
    @Transactional
    void getAllVerificationRecords() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList
        restVerificationRecordMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(verificationRecord.getId().toString())))
            .andExpect(jsonPath("$.[*].verifierId").value(hasItem(DEFAULT_VERIFIER_ID.toString())))
            .andExpect(jsonPath("$.[*].verifiedAt").value(hasItem(DEFAULT_VERIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].expectedRoot").value(hasItem(DEFAULT_EXPECTED_ROOT)))
            .andExpect(jsonPath("$.[*].recomputedRoot").value(hasItem(DEFAULT_RECOMPUTED_ROOT)))
            .andExpect(jsonPath("$.[*].tokenValid").value(hasItem(DEFAULT_TOKEN_VALID)))
            .andExpect(jsonPath("$.[*].result").value(hasItem(DEFAULT_RESULT.toString())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)));
    }

    @Test
    @Transactional
    void getVerificationRecord() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get the verificationRecord
        restVerificationRecordMockMvc
            .perform(get(ENTITY_API_URL_ID, verificationRecord.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(verificationRecord.getId().toString()))
            .andExpect(jsonPath("$.verifierId").value(DEFAULT_VERIFIER_ID.toString()))
            .andExpect(jsonPath("$.verifiedAt").value(DEFAULT_VERIFIED_AT.toString()))
            .andExpect(jsonPath("$.expectedRoot").value(DEFAULT_EXPECTED_ROOT))
            .andExpect(jsonPath("$.recomputedRoot").value(DEFAULT_RECOMPUTED_ROOT))
            .andExpect(jsonPath("$.tokenValid").value(DEFAULT_TOKEN_VALID))
            .andExpect(jsonPath("$.result").value(DEFAULT_RESULT.toString()))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT));
    }

    @Test
    @Transactional
    void getVerificationRecordsByIdFiltering() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        UUID id = verificationRecord.getId();

        defaultVerificationRecordFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByVerifierIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where verifierId equals to
        defaultVerificationRecordFiltering("verifierId.equals=" + DEFAULT_VERIFIER_ID, "verifierId.equals=" + UPDATED_VERIFIER_ID);
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByVerifierIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where verifierId in
        defaultVerificationRecordFiltering(
            "verifierId.in=" + DEFAULT_VERIFIER_ID + "," + UPDATED_VERIFIER_ID,
            "verifierId.in=" + UPDATED_VERIFIER_ID
        );
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByVerifierIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where verifierId is not null
        defaultVerificationRecordFiltering("verifierId.specified=true", "verifierId.specified=false");
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByVerifiedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where verifiedAt equals to
        defaultVerificationRecordFiltering("verifiedAt.equals=" + DEFAULT_VERIFIED_AT, "verifiedAt.equals=" + UPDATED_VERIFIED_AT);
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByVerifiedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where verifiedAt in
        defaultVerificationRecordFiltering(
            "verifiedAt.in=" + DEFAULT_VERIFIED_AT + "," + UPDATED_VERIFIED_AT,
            "verifiedAt.in=" + UPDATED_VERIFIED_AT
        );
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByVerifiedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where verifiedAt is not null
        defaultVerificationRecordFiltering("verifiedAt.specified=true", "verifiedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByExpectedRootIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where expectedRoot equals to
        defaultVerificationRecordFiltering("expectedRoot.equals=" + DEFAULT_EXPECTED_ROOT, "expectedRoot.equals=" + UPDATED_EXPECTED_ROOT);
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByExpectedRootIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where expectedRoot in
        defaultVerificationRecordFiltering(
            "expectedRoot.in=" + DEFAULT_EXPECTED_ROOT + "," + UPDATED_EXPECTED_ROOT,
            "expectedRoot.in=" + UPDATED_EXPECTED_ROOT
        );
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByExpectedRootIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where expectedRoot is not null
        defaultVerificationRecordFiltering("expectedRoot.specified=true", "expectedRoot.specified=false");
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByExpectedRootContainsSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where expectedRoot contains
        defaultVerificationRecordFiltering(
            "expectedRoot.contains=" + DEFAULT_EXPECTED_ROOT,
            "expectedRoot.contains=" + UPDATED_EXPECTED_ROOT
        );
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByExpectedRootNotContainsSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where expectedRoot does not contain
        defaultVerificationRecordFiltering(
            "expectedRoot.doesNotContain=" + UPDATED_EXPECTED_ROOT,
            "expectedRoot.doesNotContain=" + DEFAULT_EXPECTED_ROOT
        );
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByRecomputedRootIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where recomputedRoot equals to
        defaultVerificationRecordFiltering(
            "recomputedRoot.equals=" + DEFAULT_RECOMPUTED_ROOT,
            "recomputedRoot.equals=" + UPDATED_RECOMPUTED_ROOT
        );
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByRecomputedRootIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where recomputedRoot in
        defaultVerificationRecordFiltering(
            "recomputedRoot.in=" + DEFAULT_RECOMPUTED_ROOT + "," + UPDATED_RECOMPUTED_ROOT,
            "recomputedRoot.in=" + UPDATED_RECOMPUTED_ROOT
        );
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByRecomputedRootIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where recomputedRoot is not null
        defaultVerificationRecordFiltering("recomputedRoot.specified=true", "recomputedRoot.specified=false");
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByRecomputedRootContainsSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where recomputedRoot contains
        defaultVerificationRecordFiltering(
            "recomputedRoot.contains=" + DEFAULT_RECOMPUTED_ROOT,
            "recomputedRoot.contains=" + UPDATED_RECOMPUTED_ROOT
        );
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByRecomputedRootNotContainsSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where recomputedRoot does not contain
        defaultVerificationRecordFiltering(
            "recomputedRoot.doesNotContain=" + UPDATED_RECOMPUTED_ROOT,
            "recomputedRoot.doesNotContain=" + DEFAULT_RECOMPUTED_ROOT
        );
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByTokenValidIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where tokenValid equals to
        defaultVerificationRecordFiltering("tokenValid.equals=" + DEFAULT_TOKEN_VALID, "tokenValid.equals=" + UPDATED_TOKEN_VALID);
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByTokenValidIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where tokenValid in
        defaultVerificationRecordFiltering(
            "tokenValid.in=" + DEFAULT_TOKEN_VALID + "," + UPDATED_TOKEN_VALID,
            "tokenValid.in=" + UPDATED_TOKEN_VALID
        );
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByTokenValidIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where tokenValid is not null
        defaultVerificationRecordFiltering("tokenValid.specified=true", "tokenValid.specified=false");
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByResultIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where result equals to
        defaultVerificationRecordFiltering("result.equals=" + DEFAULT_RESULT, "result.equals=" + UPDATED_RESULT);
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByResultIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where result in
        defaultVerificationRecordFiltering("result.in=" + DEFAULT_RESULT + "," + UPDATED_RESULT, "result.in=" + UPDATED_RESULT);
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByResultIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where result is not null
        defaultVerificationRecordFiltering("result.specified=true", "result.specified=false");
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByCommentIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where comment equals to
        defaultVerificationRecordFiltering("comment.equals=" + DEFAULT_COMMENT, "comment.equals=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByCommentIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where comment in
        defaultVerificationRecordFiltering("comment.in=" + DEFAULT_COMMENT + "," + UPDATED_COMMENT, "comment.in=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByCommentIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where comment is not null
        defaultVerificationRecordFiltering("comment.specified=true", "comment.specified=false");
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByCommentContainsSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where comment contains
        defaultVerificationRecordFiltering("comment.contains=" + DEFAULT_COMMENT, "comment.contains=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByCommentNotContainsSomething() throws Exception {
        // Initialize the database
        insertedVerificationRecord = verificationRecordRepository.saveAndFlush(verificationRecord);

        // Get all the verificationRecordList where comment does not contain
        defaultVerificationRecordFiltering("comment.doesNotContain=" + UPDATED_COMMENT, "comment.doesNotContain=" + DEFAULT_COMMENT);
    }

    @Test
    @Transactional
    void getAllVerificationRecordsByProofIsEqualToSomething() throws Exception {
        InclusionProof proof;
        if (TestUtil.findAll(em, InclusionProof.class).isEmpty()) {
            verificationRecordRepository.saveAndFlush(verificationRecord);
            proof = InclusionProofResourceIT.createEntity(em);
        } else {
            proof = TestUtil.findAll(em, InclusionProof.class).getFirst();
        }
        em.persist(proof);
        em.flush();
        verificationRecord.setProof(proof);
        verificationRecordRepository.saveAndFlush(verificationRecord);
        UUID proofId = proof.getId();
        // Get all the verificationRecordList where proof equals to proofId
        defaultVerificationRecordShouldBeFound("proofId.equals=" + proofId);

        // Get all the verificationRecordList where proof equals to UUID.randomUUID()
        defaultVerificationRecordShouldNotBeFound("proofId.equals=" + UUID.randomUUID());
    }

    private void defaultVerificationRecordFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultVerificationRecordShouldBeFound(shouldBeFound);
        defaultVerificationRecordShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultVerificationRecordShouldBeFound(String filter) throws Exception {
        restVerificationRecordMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(verificationRecord.getId().toString())))
            .andExpect(jsonPath("$.[*].verifierId").value(hasItem(DEFAULT_VERIFIER_ID.toString())))
            .andExpect(jsonPath("$.[*].verifiedAt").value(hasItem(DEFAULT_VERIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].expectedRoot").value(hasItem(DEFAULT_EXPECTED_ROOT)))
            .andExpect(jsonPath("$.[*].recomputedRoot").value(hasItem(DEFAULT_RECOMPUTED_ROOT)))
            .andExpect(jsonPath("$.[*].tokenValid").value(hasItem(DEFAULT_TOKEN_VALID)))
            .andExpect(jsonPath("$.[*].result").value(hasItem(DEFAULT_RESULT.toString())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)));

        // Check, that the count call also returns 1
        restVerificationRecordMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultVerificationRecordShouldNotBeFound(String filter) throws Exception {
        restVerificationRecordMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restVerificationRecordMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingVerificationRecord() throws Exception {
        // Get the verificationRecord
        restVerificationRecordMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return verificationRecordRepository.count();
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

    protected VerificationRecord getPersistedVerificationRecord(VerificationRecord verificationRecord) {
        return verificationRecordRepository.findById(verificationRecord.getId()).orElseThrow();
    }

    protected void assertPersistedVerificationRecordToMatchAllProperties(VerificationRecord expectedVerificationRecord) {
        assertVerificationRecordAllPropertiesEquals(expectedVerificationRecord, getPersistedVerificationRecord(expectedVerificationRecord));
    }

    protected void assertPersistedVerificationRecordToMatchUpdatableProperties(VerificationRecord expectedVerificationRecord) {
        assertVerificationRecordAllUpdatablePropertiesEquals(
            expectedVerificationRecord,
            getPersistedVerificationRecord(expectedVerificationRecord)
        );
    }
}
