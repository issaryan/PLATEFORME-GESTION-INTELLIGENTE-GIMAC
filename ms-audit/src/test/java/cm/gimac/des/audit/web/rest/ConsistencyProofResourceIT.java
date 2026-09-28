package cm.gimac.des.audit.web.rest;

import static cm.gimac.des.audit.domain.ConsistencyProofAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.audit.IntegrationTest;
import cm.gimac.des.audit.domain.ConsistencyProof;
import cm.gimac.des.audit.repository.ConsistencyProofRepository;
import cm.gimac.des.audit.service.mapper.ConsistencyProofMapper;
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
 * Integration tests for the {@link ConsistencyProofResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ConsistencyProofResourceIT {

    private static final Long DEFAULT_FIRST_SIZE = 0L;
    private static final Long UPDATED_FIRST_SIZE = 1L;

    private static final Long DEFAULT_SECOND_SIZE = 0L;
    private static final Long UPDATED_SECOND_SIZE = 1L;

    private static final String DEFAULT_PROOF_HASHES = "AAAAAAAAAA";
    private static final String UPDATED_PROOF_HASHES = "BBBBBBBBBB";

    private static final Instant DEFAULT_COMPUTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_COMPUTED_AT = Instant.ofEpochMilli(1701661978853L);

    private static final String ENTITY_API_URL = "/api/consistency-proofs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ConsistencyProofRepository consistencyProofRepository;

    @Autowired
    private ConsistencyProofMapper consistencyProofMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restConsistencyProofMockMvc;

    private ConsistencyProof consistencyProof;

    private ConsistencyProof insertedConsistencyProof;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ConsistencyProof createEntity() {
        return new ConsistencyProof()
            .firstSize(DEFAULT_FIRST_SIZE)
            .secondSize(DEFAULT_SECOND_SIZE)
            .proofHashes(DEFAULT_PROOF_HASHES)
            .computedAt(DEFAULT_COMPUTED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ConsistencyProof createUpdatedEntity() {
        return new ConsistencyProof()
            .firstSize(UPDATED_FIRST_SIZE)
            .secondSize(UPDATED_SECOND_SIZE)
            .proofHashes(UPDATED_PROOF_HASHES)
            .computedAt(UPDATED_COMPUTED_AT);
    }

    @BeforeEach
    void initTest() {
        consistencyProof = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedConsistencyProof != null) {
            consistencyProofRepository.delete(insertedConsistencyProof);
            insertedConsistencyProof = null;
        }
    }

    @Test
    @Transactional
    void getAllConsistencyProofs() throws Exception {
        // Initialize the database
        insertedConsistencyProof = consistencyProofRepository.saveAndFlush(consistencyProof);

        // Get all the consistencyProofList
        restConsistencyProofMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(consistencyProof.getId().toString())))
            .andExpect(jsonPath("$.[*].firstSize").value(hasItem(DEFAULT_FIRST_SIZE.intValue())))
            .andExpect(jsonPath("$.[*].secondSize").value(hasItem(DEFAULT_SECOND_SIZE.intValue())))
            .andExpect(jsonPath("$.[*].proofHashes").value(hasItem(DEFAULT_PROOF_HASHES)))
            .andExpect(jsonPath("$.[*].computedAt").value(hasItem(DEFAULT_COMPUTED_AT.toString())));
    }

    @Test
    @Transactional
    void getConsistencyProof() throws Exception {
        // Initialize the database
        insertedConsistencyProof = consistencyProofRepository.saveAndFlush(consistencyProof);

        // Get the consistencyProof
        restConsistencyProofMockMvc
            .perform(get(ENTITY_API_URL_ID, consistencyProof.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(consistencyProof.getId().toString()))
            .andExpect(jsonPath("$.firstSize").value(DEFAULT_FIRST_SIZE.intValue()))
            .andExpect(jsonPath("$.secondSize").value(DEFAULT_SECOND_SIZE.intValue()))
            .andExpect(jsonPath("$.proofHashes").value(DEFAULT_PROOF_HASHES))
            .andExpect(jsonPath("$.computedAt").value(DEFAULT_COMPUTED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingConsistencyProof() throws Exception {
        // Get the consistencyProof
        restConsistencyProofMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return consistencyProofRepository.count();
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

    protected ConsistencyProof getPersistedConsistencyProof(ConsistencyProof consistencyProof) {
        return consistencyProofRepository.findById(consistencyProof.getId()).orElseThrow();
    }

    protected void assertPersistedConsistencyProofToMatchAllProperties(ConsistencyProof expectedConsistencyProof) {
        assertConsistencyProofAllPropertiesEquals(expectedConsistencyProof, getPersistedConsistencyProof(expectedConsistencyProof));
    }

    protected void assertPersistedConsistencyProofToMatchUpdatableProperties(ConsistencyProof expectedConsistencyProof) {
        assertConsistencyProofAllUpdatablePropertiesEquals(
            expectedConsistencyProof,
            getPersistedConsistencyProof(expectedConsistencyProof)
        );
    }
}
