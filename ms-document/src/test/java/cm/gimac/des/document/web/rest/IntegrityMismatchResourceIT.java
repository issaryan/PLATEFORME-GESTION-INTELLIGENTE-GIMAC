package cm.gimac.des.document.web.rest;

import static cm.gimac.des.document.domain.IntegrityMismatchAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.domain.Document;
import cm.gimac.des.document.domain.IntegrityCheck;
import cm.gimac.des.document.domain.IntegrityMismatch;
import cm.gimac.des.document.domain.enumeration.MismatchKind;
import cm.gimac.des.document.repository.IntegrityMismatchRepository;
import cm.gimac.des.document.service.IntegrityMismatchService;
import cm.gimac.des.document.service.mapper.IntegrityMismatchMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
 * Integration tests for the {@link IntegrityMismatchResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class IntegrityMismatchResourceIT {

    private static final MismatchKind DEFAULT_KIND = MismatchKind.EMPREINTE_DIVERGENTE;
    private static final MismatchKind UPDATED_KIND = MismatchKind.OBJET_ILLISIBLE;

    private static final String DEFAULT_OBJECT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_OBJECT_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_EXPECTED_SHA_256 = "2218be5395ba0cebcf5e820e7c1f0ce686a9f014de80597344e5cf55631221fc";
    private static final String UPDATED_EXPECTED_SHA_256 = "2f831df0214e8f1c05c788ebe2acef290e809a2068d981526a1a6ef2aed256c2";

    private static final String DEFAULT_ACTUAL_SHA_256 = "c4c47f3c905d08ef319708c58b82153c7d47efde8ba1736bd28e17549aee7ea7";
    private static final String UPDATED_ACTUAL_SHA_256 = "6dd0669da2723100c8b862789c3441a1956f453768d55fd198adfe4c1ea2ea45";

    private static final Instant DEFAULT_DETECTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DETECTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/integrity-mismatches";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private IntegrityMismatchRepository integrityMismatchRepository;

    @Mock
    private IntegrityMismatchRepository integrityMismatchRepositoryMock;

    @Autowired
    private IntegrityMismatchMapper integrityMismatchMapper;

    @Mock
    private IntegrityMismatchService integrityMismatchServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restIntegrityMismatchMockMvc;

    private IntegrityMismatch integrityMismatch;

    private IntegrityMismatch insertedIntegrityMismatch;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static IntegrityMismatch createEntity(EntityManager em) {
        IntegrityMismatch integrityMismatch = new IntegrityMismatch()
            .kind(DEFAULT_KIND)
            .objectKey(DEFAULT_OBJECT_KEY)
            .expectedSha256(DEFAULT_EXPECTED_SHA_256)
            .actualSha256(DEFAULT_ACTUAL_SHA_256)
            .detectedAt(DEFAULT_DETECTED_AT);
        // Add required entity
        IntegrityCheck integrityCheck;
        if (TestUtil.findAll(em, IntegrityCheck.class).isEmpty()) {
            integrityCheck = IntegrityCheckResourceIT.createEntity();
            em.persist(integrityCheck);
            em.flush();
        } else {
            integrityCheck = TestUtil.findAll(em, IntegrityCheck.class).get(0);
        }
        integrityMismatch.setIntegrityCheck(integrityCheck);
        // Add required entity
        Document document;
        if (TestUtil.findAll(em, Document.class).isEmpty()) {
            document = DocumentResourceIT.createEntity(em);
            em.persist(document);
            em.flush();
        } else {
            document = TestUtil.findAll(em, Document.class).get(0);
        }
        integrityMismatch.setDocument(document);
        return integrityMismatch;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static IntegrityMismatch createUpdatedEntity(EntityManager em) {
        IntegrityMismatch updatedIntegrityMismatch = new IntegrityMismatch()
            .kind(UPDATED_KIND)
            .objectKey(UPDATED_OBJECT_KEY)
            .expectedSha256(UPDATED_EXPECTED_SHA_256)
            .actualSha256(UPDATED_ACTUAL_SHA_256)
            .detectedAt(UPDATED_DETECTED_AT);
        // Add required entity
        IntegrityCheck integrityCheck;
        if (TestUtil.findAll(em, IntegrityCheck.class).isEmpty()) {
            integrityCheck = IntegrityCheckResourceIT.createUpdatedEntity();
            em.persist(integrityCheck);
            em.flush();
        } else {
            integrityCheck = TestUtil.findAll(em, IntegrityCheck.class).get(0);
        }
        updatedIntegrityMismatch.setIntegrityCheck(integrityCheck);
        // Add required entity
        Document document;
        if (TestUtil.findAll(em, Document.class).isEmpty()) {
            document = DocumentResourceIT.createUpdatedEntity(em);
            em.persist(document);
            em.flush();
        } else {
            document = TestUtil.findAll(em, Document.class).get(0);
        }
        updatedIntegrityMismatch.setDocument(document);
        return updatedIntegrityMismatch;
    }

    @BeforeEach
    void initTest() {
        integrityMismatch = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedIntegrityMismatch != null) {
            integrityMismatchRepository.delete(insertedIntegrityMismatch);
            insertedIntegrityMismatch = null;
        }
    }

    @Test
    @Transactional
    void getAllIntegrityMismatches() throws Exception {
        // Initialize the database
        insertedIntegrityMismatch = integrityMismatchRepository.saveAndFlush(integrityMismatch);

        // Get all the integrityMismatchList
        restIntegrityMismatchMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(integrityMismatch.getId().toString())))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].objectKey").value(hasItem(DEFAULT_OBJECT_KEY)))
            .andExpect(jsonPath("$.[*].expectedSha256").value(hasItem(DEFAULT_EXPECTED_SHA_256)))
            .andExpect(jsonPath("$.[*].actualSha256").value(hasItem(DEFAULT_ACTUAL_SHA_256)))
            .andExpect(jsonPath("$.[*].detectedAt").value(hasItem(DEFAULT_DETECTED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllIntegrityMismatchesWithEagerRelationshipsIsEnabled() throws Exception {
        when(integrityMismatchServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restIntegrityMismatchMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(integrityMismatchServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllIntegrityMismatchesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(integrityMismatchServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restIntegrityMismatchMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(integrityMismatchRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getIntegrityMismatch() throws Exception {
        // Initialize the database
        insertedIntegrityMismatch = integrityMismatchRepository.saveAndFlush(integrityMismatch);

        // Get the integrityMismatch
        restIntegrityMismatchMockMvc
            .perform(get(ENTITY_API_URL_ID, integrityMismatch.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(integrityMismatch.getId().toString()))
            .andExpect(jsonPath("$.kind").value(DEFAULT_KIND.toString()))
            .andExpect(jsonPath("$.objectKey").value(DEFAULT_OBJECT_KEY))
            .andExpect(jsonPath("$.expectedSha256").value(DEFAULT_EXPECTED_SHA_256))
            .andExpect(jsonPath("$.actualSha256").value(DEFAULT_ACTUAL_SHA_256))
            .andExpect(jsonPath("$.detectedAt").value(DEFAULT_DETECTED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingIntegrityMismatch() throws Exception {
        // Get the integrityMismatch
        restIntegrityMismatchMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return integrityMismatchRepository.count();
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

    protected IntegrityMismatch getPersistedIntegrityMismatch(IntegrityMismatch integrityMismatch) {
        return integrityMismatchRepository.findById(integrityMismatch.getId()).orElseThrow();
    }

    protected void assertPersistedIntegrityMismatchToMatchAllProperties(IntegrityMismatch expectedIntegrityMismatch) {
        assertIntegrityMismatchAllPropertiesEquals(expectedIntegrityMismatch, getPersistedIntegrityMismatch(expectedIntegrityMismatch));
    }

    protected void assertPersistedIntegrityMismatchToMatchUpdatableProperties(IntegrityMismatch expectedIntegrityMismatch) {
        assertIntegrityMismatchAllUpdatablePropertiesEquals(
            expectedIntegrityMismatch,
            getPersistedIntegrityMismatch(expectedIntegrityMismatch)
        );
    }
}
