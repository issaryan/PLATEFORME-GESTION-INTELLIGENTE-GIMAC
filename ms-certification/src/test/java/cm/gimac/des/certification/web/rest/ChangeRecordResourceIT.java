package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.ChangeRecordAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.ChangeRecord;
import cm.gimac.des.certification.domain.enumeration.ChangeApproval;
import cm.gimac.des.certification.repository.ChangeRecordRepository;
import cm.gimac.des.certification.service.mapper.ChangeRecordMapper;
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
 * Integration tests for the {@link ChangeRecordResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ChangeRecordResourceIT {

    private static final String DEFAULT_EXTERNAL_REF = "AAAAAAAAAA";
    private static final String UPDATED_EXTERNAL_REF = "BBBBBBBBBB";

    private static final ChangeApproval DEFAULT_APPROVAL = ChangeApproval.SOUMISE;
    private static final ChangeApproval UPDATED_APPROVAL = ChangeApproval.APPROUVEE;

    private static final Instant DEFAULT_APPROVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_APPROVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_WINDOW_START = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_WINDOW_START = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_WINDOW_END = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_WINDOW_END = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_READ_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_READ_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_CONTENT_DIGEST = "98bac635f1c1106a4645e6db45200a80f3c11f4e7932c55cd1173aa82e06d600";
    private static final String UPDATED_CONTENT_DIGEST = "a330b4eb4b8a7ccba8ded4ae20415057a2170682a116649932bbb541b20e7e51";

    private static final UUID DEFAULT_REFERENCED_BY = UUID.randomUUID();
    private static final UUID UPDATED_REFERENCED_BY = UUID.randomUUID();

    private static final String ENTITY_API_URL = "/api/change-records";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ChangeRecordRepository changeRecordRepository;

    @Autowired
    private ChangeRecordMapper changeRecordMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restChangeRecordMockMvc;

    private ChangeRecord changeRecord;

    private ChangeRecord insertedChangeRecord;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChangeRecord createEntity() {
        return new ChangeRecord()
            .externalRef(DEFAULT_EXTERNAL_REF)
            .approval(DEFAULT_APPROVAL)
            .approvedAt(DEFAULT_APPROVED_AT)
            .windowStart(DEFAULT_WINDOW_START)
            .windowEnd(DEFAULT_WINDOW_END)
            .readAt(DEFAULT_READ_AT)
            .contentDigest(DEFAULT_CONTENT_DIGEST)
            .referencedBy(DEFAULT_REFERENCED_BY);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChangeRecord createUpdatedEntity() {
        return new ChangeRecord()
            .externalRef(UPDATED_EXTERNAL_REF)
            .approval(UPDATED_APPROVAL)
            .approvedAt(UPDATED_APPROVED_AT)
            .windowStart(UPDATED_WINDOW_START)
            .windowEnd(UPDATED_WINDOW_END)
            .readAt(UPDATED_READ_AT)
            .contentDigest(UPDATED_CONTENT_DIGEST)
            .referencedBy(UPDATED_REFERENCED_BY);
    }

    @BeforeEach
    void initTest() {
        changeRecord = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedChangeRecord != null) {
            changeRecordRepository.delete(insertedChangeRecord);
            insertedChangeRecord = null;
        }
    }

    @Test
    @Transactional
    void getAllChangeRecords() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList
        restChangeRecordMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(changeRecord.getId().toString())))
            .andExpect(jsonPath("$.[*].externalRef").value(hasItem(DEFAULT_EXTERNAL_REF)))
            .andExpect(jsonPath("$.[*].approval").value(hasItem(DEFAULT_APPROVAL.toString())))
            .andExpect(jsonPath("$.[*].approvedAt").value(hasItem(DEFAULT_APPROVED_AT.toString())))
            .andExpect(jsonPath("$.[*].windowStart").value(hasItem(DEFAULT_WINDOW_START.toString())))
            .andExpect(jsonPath("$.[*].windowEnd").value(hasItem(DEFAULT_WINDOW_END.toString())))
            .andExpect(jsonPath("$.[*].readAt").value(hasItem(DEFAULT_READ_AT.toString())))
            .andExpect(jsonPath("$.[*].contentDigest").value(hasItem(DEFAULT_CONTENT_DIGEST)))
            .andExpect(jsonPath("$.[*].referencedBy").value(hasItem(DEFAULT_REFERENCED_BY.toString())));
    }

    @Test
    @Transactional
    void getChangeRecord() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get the changeRecord
        restChangeRecordMockMvc
            .perform(get(ENTITY_API_URL_ID, changeRecord.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(changeRecord.getId().toString()))
            .andExpect(jsonPath("$.externalRef").value(DEFAULT_EXTERNAL_REF))
            .andExpect(jsonPath("$.approval").value(DEFAULT_APPROVAL.toString()))
            .andExpect(jsonPath("$.approvedAt").value(DEFAULT_APPROVED_AT.toString()))
            .andExpect(jsonPath("$.windowStart").value(DEFAULT_WINDOW_START.toString()))
            .andExpect(jsonPath("$.windowEnd").value(DEFAULT_WINDOW_END.toString()))
            .andExpect(jsonPath("$.readAt").value(DEFAULT_READ_AT.toString()))
            .andExpect(jsonPath("$.contentDigest").value(DEFAULT_CONTENT_DIGEST))
            .andExpect(jsonPath("$.referencedBy").value(DEFAULT_REFERENCED_BY.toString()));
    }

    @Test
    @Transactional
    void getChangeRecordsByIdFiltering() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        UUID id = changeRecord.getId();

        defaultChangeRecordFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByExternalRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where externalRef equals to
        defaultChangeRecordFiltering("externalRef.equals=" + DEFAULT_EXTERNAL_REF, "externalRef.equals=" + UPDATED_EXTERNAL_REF);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByExternalRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where externalRef in
        defaultChangeRecordFiltering(
            "externalRef.in=" + DEFAULT_EXTERNAL_REF + "," + UPDATED_EXTERNAL_REF,
            "externalRef.in=" + UPDATED_EXTERNAL_REF
        );
    }

    @Test
    @Transactional
    void getAllChangeRecordsByExternalRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where externalRef is not null
        defaultChangeRecordFiltering("externalRef.specified=true", "externalRef.specified=false");
    }

    @Test
    @Transactional
    void getAllChangeRecordsByExternalRefContainsSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where externalRef contains
        defaultChangeRecordFiltering("externalRef.contains=" + DEFAULT_EXTERNAL_REF, "externalRef.contains=" + UPDATED_EXTERNAL_REF);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByExternalRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where externalRef does not contain
        defaultChangeRecordFiltering(
            "externalRef.doesNotContain=" + UPDATED_EXTERNAL_REF,
            "externalRef.doesNotContain=" + DEFAULT_EXTERNAL_REF
        );
    }

    @Test
    @Transactional
    void getAllChangeRecordsByApprovalIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where approval equals to
        defaultChangeRecordFiltering("approval.equals=" + DEFAULT_APPROVAL, "approval.equals=" + UPDATED_APPROVAL);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByApprovalIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where approval in
        defaultChangeRecordFiltering("approval.in=" + DEFAULT_APPROVAL + "," + UPDATED_APPROVAL, "approval.in=" + UPDATED_APPROVAL);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByApprovalIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where approval is not null
        defaultChangeRecordFiltering("approval.specified=true", "approval.specified=false");
    }

    @Test
    @Transactional
    void getAllChangeRecordsByApprovedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where approvedAt equals to
        defaultChangeRecordFiltering("approvedAt.equals=" + DEFAULT_APPROVED_AT, "approvedAt.equals=" + UPDATED_APPROVED_AT);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByApprovedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where approvedAt in
        defaultChangeRecordFiltering(
            "approvedAt.in=" + DEFAULT_APPROVED_AT + "," + UPDATED_APPROVED_AT,
            "approvedAt.in=" + UPDATED_APPROVED_AT
        );
    }

    @Test
    @Transactional
    void getAllChangeRecordsByApprovedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where approvedAt is not null
        defaultChangeRecordFiltering("approvedAt.specified=true", "approvedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllChangeRecordsByWindowStartIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where windowStart equals to
        defaultChangeRecordFiltering("windowStart.equals=" + DEFAULT_WINDOW_START, "windowStart.equals=" + UPDATED_WINDOW_START);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByWindowStartIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where windowStart in
        defaultChangeRecordFiltering(
            "windowStart.in=" + DEFAULT_WINDOW_START + "," + UPDATED_WINDOW_START,
            "windowStart.in=" + UPDATED_WINDOW_START
        );
    }

    @Test
    @Transactional
    void getAllChangeRecordsByWindowStartIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where windowStart is not null
        defaultChangeRecordFiltering("windowStart.specified=true", "windowStart.specified=false");
    }

    @Test
    @Transactional
    void getAllChangeRecordsByWindowEndIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where windowEnd equals to
        defaultChangeRecordFiltering("windowEnd.equals=" + DEFAULT_WINDOW_END, "windowEnd.equals=" + UPDATED_WINDOW_END);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByWindowEndIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where windowEnd in
        defaultChangeRecordFiltering("windowEnd.in=" + DEFAULT_WINDOW_END + "," + UPDATED_WINDOW_END, "windowEnd.in=" + UPDATED_WINDOW_END);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByWindowEndIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where windowEnd is not null
        defaultChangeRecordFiltering("windowEnd.specified=true", "windowEnd.specified=false");
    }

    @Test
    @Transactional
    void getAllChangeRecordsByReadAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where readAt equals to
        defaultChangeRecordFiltering("readAt.equals=" + DEFAULT_READ_AT, "readAt.equals=" + UPDATED_READ_AT);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByReadAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where readAt in
        defaultChangeRecordFiltering("readAt.in=" + DEFAULT_READ_AT + "," + UPDATED_READ_AT, "readAt.in=" + UPDATED_READ_AT);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByReadAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where readAt is not null
        defaultChangeRecordFiltering("readAt.specified=true", "readAt.specified=false");
    }

    @Test
    @Transactional
    void getAllChangeRecordsByContentDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where contentDigest equals to
        defaultChangeRecordFiltering("contentDigest.equals=" + DEFAULT_CONTENT_DIGEST, "contentDigest.equals=" + UPDATED_CONTENT_DIGEST);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByContentDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where contentDigest in
        defaultChangeRecordFiltering(
            "contentDigest.in=" + DEFAULT_CONTENT_DIGEST + "," + UPDATED_CONTENT_DIGEST,
            "contentDigest.in=" + UPDATED_CONTENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllChangeRecordsByContentDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where contentDigest is not null
        defaultChangeRecordFiltering("contentDigest.specified=true", "contentDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllChangeRecordsByContentDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where contentDigest contains
        defaultChangeRecordFiltering(
            "contentDigest.contains=" + DEFAULT_CONTENT_DIGEST,
            "contentDigest.contains=" + UPDATED_CONTENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllChangeRecordsByContentDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where contentDigest does not contain
        defaultChangeRecordFiltering(
            "contentDigest.doesNotContain=" + UPDATED_CONTENT_DIGEST,
            "contentDigest.doesNotContain=" + DEFAULT_CONTENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllChangeRecordsByReferencedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where referencedBy equals to
        defaultChangeRecordFiltering("referencedBy.equals=" + DEFAULT_REFERENCED_BY, "referencedBy.equals=" + UPDATED_REFERENCED_BY);
    }

    @Test
    @Transactional
    void getAllChangeRecordsByReferencedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where referencedBy in
        defaultChangeRecordFiltering(
            "referencedBy.in=" + DEFAULT_REFERENCED_BY + "," + UPDATED_REFERENCED_BY,
            "referencedBy.in=" + UPDATED_REFERENCED_BY
        );
    }

    @Test
    @Transactional
    void getAllChangeRecordsByReferencedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChangeRecord = changeRecordRepository.saveAndFlush(changeRecord);

        // Get all the changeRecordList where referencedBy is not null
        defaultChangeRecordFiltering("referencedBy.specified=true", "referencedBy.specified=false");
    }

    private void defaultChangeRecordFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultChangeRecordShouldBeFound(shouldBeFound);
        defaultChangeRecordShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultChangeRecordShouldBeFound(String filter) throws Exception {
        restChangeRecordMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(changeRecord.getId().toString())))
            .andExpect(jsonPath("$.[*].externalRef").value(hasItem(DEFAULT_EXTERNAL_REF)))
            .andExpect(jsonPath("$.[*].approval").value(hasItem(DEFAULT_APPROVAL.toString())))
            .andExpect(jsonPath("$.[*].approvedAt").value(hasItem(DEFAULT_APPROVED_AT.toString())))
            .andExpect(jsonPath("$.[*].windowStart").value(hasItem(DEFAULT_WINDOW_START.toString())))
            .andExpect(jsonPath("$.[*].windowEnd").value(hasItem(DEFAULT_WINDOW_END.toString())))
            .andExpect(jsonPath("$.[*].readAt").value(hasItem(DEFAULT_READ_AT.toString())))
            .andExpect(jsonPath("$.[*].contentDigest").value(hasItem(DEFAULT_CONTENT_DIGEST)))
            .andExpect(jsonPath("$.[*].referencedBy").value(hasItem(DEFAULT_REFERENCED_BY.toString())));

        // Check, that the count call also returns 1
        restChangeRecordMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultChangeRecordShouldNotBeFound(String filter) throws Exception {
        restChangeRecordMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restChangeRecordMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingChangeRecord() throws Exception {
        // Get the changeRecord
        restChangeRecordMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return changeRecordRepository.count();
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

    protected ChangeRecord getPersistedChangeRecord(ChangeRecord changeRecord) {
        return changeRecordRepository.findById(changeRecord.getId()).orElseThrow();
    }

    protected void assertPersistedChangeRecordToMatchAllProperties(ChangeRecord expectedChangeRecord) {
        assertChangeRecordAllPropertiesEquals(expectedChangeRecord, getPersistedChangeRecord(expectedChangeRecord));
    }

    protected void assertPersistedChangeRecordToMatchUpdatableProperties(ChangeRecord expectedChangeRecord) {
        assertChangeRecordAllUpdatablePropertiesEquals(expectedChangeRecord, getPersistedChangeRecord(expectedChangeRecord));
    }
}
