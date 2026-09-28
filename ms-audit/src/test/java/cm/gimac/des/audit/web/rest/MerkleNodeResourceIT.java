package cm.gimac.des.audit.web.rest;

import static cm.gimac.des.audit.domain.MerkleNodeAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.audit.IntegrationTest;
import cm.gimac.des.audit.domain.MerkleNode;
import cm.gimac.des.audit.repository.MerkleNodeRepository;
import cm.gimac.des.audit.service.mapper.MerkleNodeMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link MerkleNodeResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class MerkleNodeResourceIT {

    private static final Integer DEFAULT_LEVEL = 0;
    private static final Integer UPDATED_LEVEL = 1;

    private static final Long DEFAULT_POSITION = 0L;
    private static final Long UPDATED_POSITION = 1L;

    private static final String DEFAULT_HASH = "cf9810b90dd15aff727d5569b7a2d00af2ef00da5912f3dbf309f4d27ef9a964";
    private static final String UPDATED_HASH = "60917ed9df160876d53e43d479ffdf4492be5b82c7b19aa45ba20bb6ce26ce01";

    private static final String ENTITY_API_URL = "/api/merkle-nodes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MerkleNodeRepository merkleNodeRepository;

    @Autowired
    private MerkleNodeMapper merkleNodeMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMerkleNodeMockMvc;

    private MerkleNode merkleNode;

    private MerkleNode insertedMerkleNode;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MerkleNode createEntity() {
        return new MerkleNode().level(DEFAULT_LEVEL).position(DEFAULT_POSITION).hash(DEFAULT_HASH);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MerkleNode createUpdatedEntity() {
        return new MerkleNode().level(UPDATED_LEVEL).position(UPDATED_POSITION).hash(UPDATED_HASH);
    }

    @BeforeEach
    void initTest() {
        merkleNode = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedMerkleNode != null) {
            merkleNodeRepository.delete(insertedMerkleNode);
            insertedMerkleNode = null;
        }
    }

    @Test
    @Transactional
    void getAllMerkleNodes() throws Exception {
        // Initialize the database
        insertedMerkleNode = merkleNodeRepository.saveAndFlush(merkleNode);

        // Get all the merkleNodeList
        restMerkleNodeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(merkleNode.getId().toString())))
            .andExpect(jsonPath("$.[*].level").value(hasItem(DEFAULT_LEVEL)))
            .andExpect(jsonPath("$.[*].position").value(hasItem(DEFAULT_POSITION.intValue())))
            .andExpect(jsonPath("$.[*].hash").value(hasItem(DEFAULT_HASH)));
    }

    @Test
    @Transactional
    void getMerkleNode() throws Exception {
        // Initialize the database
        insertedMerkleNode = merkleNodeRepository.saveAndFlush(merkleNode);

        // Get the merkleNode
        restMerkleNodeMockMvc
            .perform(get(ENTITY_API_URL_ID, merkleNode.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(merkleNode.getId().toString()))
            .andExpect(jsonPath("$.level").value(DEFAULT_LEVEL))
            .andExpect(jsonPath("$.position").value(DEFAULT_POSITION.intValue()))
            .andExpect(jsonPath("$.hash").value(DEFAULT_HASH));
    }

    @Test
    @Transactional
    void getNonExistingMerkleNode() throws Exception {
        // Get the merkleNode
        restMerkleNodeMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return merkleNodeRepository.count();
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

    protected MerkleNode getPersistedMerkleNode(MerkleNode merkleNode) {
        return merkleNodeRepository.findById(merkleNode.getId()).orElseThrow();
    }

    protected void assertPersistedMerkleNodeToMatchAllProperties(MerkleNode expectedMerkleNode) {
        assertMerkleNodeAllPropertiesEquals(expectedMerkleNode, getPersistedMerkleNode(expectedMerkleNode));
    }

    protected void assertPersistedMerkleNodeToMatchUpdatableProperties(MerkleNode expectedMerkleNode) {
        assertMerkleNodeAllUpdatablePropertiesEquals(expectedMerkleNode, getPersistedMerkleNode(expectedMerkleNode));
    }
}
