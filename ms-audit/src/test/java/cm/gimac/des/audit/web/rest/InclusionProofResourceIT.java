package cm.gimac.des.audit.web.rest;

import static cm.gimac.des.audit.domain.InclusionProofAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.audit.IntegrationTest;
import cm.gimac.des.audit.domain.InclusionProof;
import cm.gimac.des.audit.domain.PeriodRoot;
import cm.gimac.des.audit.repository.InclusionProofRepository;
import cm.gimac.des.audit.service.InclusionProofService;
import cm.gimac.des.audit.service.mapper.InclusionProofMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link InclusionProofResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class InclusionProofResourceIT {

    private static final Long DEFAULT_EVENT_INDEX = 0L;
    private static final Long UPDATED_EVENT_INDEX = 1L;

    private static final Long DEFAULT_TREE_SIZE = 1L;
    private static final Long UPDATED_TREE_SIZE = 2L;

    private static final String DEFAULT_AUDIT_PATH = "AAAAAAAAAA";
    private static final String UPDATED_AUDIT_PATH = "BBBBBBBBBB";

    private static final String DEFAULT_ROOT_HASH = "c530321c36c26203fde48ec43f5685c88db44ae5bece3c95d14adb9369ea7859";
    private static final String UPDATED_ROOT_HASH = "7683145dac0a8c25a8c0418341df315dd40716df4830522659b2ee9967dd8991";

    private static final Instant DEFAULT_GENERATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_GENERATED_AT = Instant.ofEpochMilli(1701661978853L);

    private static final String ENTITY_API_URL = "/api/inclusion-proofs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private InclusionProofRepository inclusionProofRepository;

    @Mock
    private InclusionProofRepository inclusionProofRepositoryMock;

    @Autowired
    private InclusionProofMapper inclusionProofMapper;

    @Mock
    private InclusionProofService inclusionProofServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restInclusionProofMockMvc;

    private InclusionProof inclusionProof;

    private InclusionProof insertedInclusionProof;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static InclusionProof createEntity(EntityManager em) {
        InclusionProof inclusionProof = new InclusionProof()
            .eventIndex(DEFAULT_EVENT_INDEX)
            .treeSize(DEFAULT_TREE_SIZE)
            .auditPath(DEFAULT_AUDIT_PATH)
            .rootHash(DEFAULT_ROOT_HASH)
            .generatedAt(DEFAULT_GENERATED_AT);
        // Add required entity
        PeriodRoot periodRoot;
        if (TestUtil.findAll(em, PeriodRoot.class).isEmpty()) {
            periodRoot = PeriodRootResourceIT.createEntity();
            em.persist(periodRoot);
            em.flush();
        } else {
            periodRoot = TestUtil.findAll(em, PeriodRoot.class).getFirst();
        }
        inclusionProof.setPeriodRoot(periodRoot);
        return inclusionProof;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static InclusionProof createUpdatedEntity(EntityManager em) {
        InclusionProof updatedInclusionProof = new InclusionProof()
            .eventIndex(UPDATED_EVENT_INDEX)
            .treeSize(UPDATED_TREE_SIZE)
            .auditPath(UPDATED_AUDIT_PATH)
            .rootHash(UPDATED_ROOT_HASH)
            .generatedAt(UPDATED_GENERATED_AT);
        // Add required entity
        PeriodRoot periodRoot;
        if (TestUtil.findAll(em, PeriodRoot.class).isEmpty()) {
            periodRoot = PeriodRootResourceIT.createUpdatedEntity();
            em.persist(periodRoot);
            em.flush();
        } else {
            periodRoot = TestUtil.findAll(em, PeriodRoot.class).getFirst();
        }
        updatedInclusionProof.setPeriodRoot(periodRoot);
        return updatedInclusionProof;
    }

    @BeforeEach
    void initTest() {
        inclusionProof = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedInclusionProof != null) {
            inclusionProofRepository.delete(insertedInclusionProof);
            insertedInclusionProof = null;
        }
    }

    @Test
    @Transactional
    void getAllInclusionProofs() throws Exception {
        // Initialize the database
        insertedInclusionProof = inclusionProofRepository.saveAndFlush(inclusionProof);

        // Get all the inclusionProofList
        restInclusionProofMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(inclusionProof.getId().toString())))
            .andExpect(jsonPath("$.[*].eventIndex").value(hasItem(DEFAULT_EVENT_INDEX.intValue())))
            .andExpect(jsonPath("$.[*].treeSize").value(hasItem(DEFAULT_TREE_SIZE.intValue())))
            .andExpect(jsonPath("$.[*].auditPath").value(hasItem(DEFAULT_AUDIT_PATH)))
            .andExpect(jsonPath("$.[*].rootHash").value(hasItem(DEFAULT_ROOT_HASH)))
            .andExpect(jsonPath("$.[*].generatedAt").value(hasItem(DEFAULT_GENERATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllInclusionProofsWithEagerRelationshipsIsEnabled() throws Exception {
        when(inclusionProofServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restInclusionProofMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(inclusionProofServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllInclusionProofsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(inclusionProofServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restInclusionProofMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(inclusionProofRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getInclusionProof() throws Exception {
        // Initialize the database
        insertedInclusionProof = inclusionProofRepository.saveAndFlush(inclusionProof);

        // Get the inclusionProof
        restInclusionProofMockMvc
            .perform(get(ENTITY_API_URL_ID, inclusionProof.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(inclusionProof.getId().toString()))
            .andExpect(jsonPath("$.eventIndex").value(DEFAULT_EVENT_INDEX.intValue()))
            .andExpect(jsonPath("$.treeSize").value(DEFAULT_TREE_SIZE.intValue()))
            .andExpect(jsonPath("$.auditPath").value(DEFAULT_AUDIT_PATH))
            .andExpect(jsonPath("$.rootHash").value(DEFAULT_ROOT_HASH))
            .andExpect(jsonPath("$.generatedAt").value(DEFAULT_GENERATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingInclusionProof() throws Exception {
        // Get the inclusionProof
        restInclusionProofMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return inclusionProofRepository.count();
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

    protected InclusionProof getPersistedInclusionProof(InclusionProof inclusionProof) {
        return inclusionProofRepository.findById(inclusionProof.getId()).orElseThrow();
    }

    protected void assertPersistedInclusionProofToMatchAllProperties(InclusionProof expectedInclusionProof) {
        assertInclusionProofAllPropertiesEquals(expectedInclusionProof, getPersistedInclusionProof(expectedInclusionProof));
    }

    protected void assertPersistedInclusionProofToMatchUpdatableProperties(InclusionProof expectedInclusionProof) {
        assertInclusionProofAllUpdatablePropertiesEquals(expectedInclusionProof, getPersistedInclusionProof(expectedInclusionProof));
    }
}
