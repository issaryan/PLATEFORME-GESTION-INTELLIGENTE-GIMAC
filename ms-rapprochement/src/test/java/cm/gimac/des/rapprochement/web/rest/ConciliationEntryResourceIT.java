package cm.gimac.des.rapprochement.web.rest;

import static cm.gimac.des.rapprochement.domain.ConciliationEntryAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.rapprochement.IntegrationTest;
import cm.gimac.des.rapprochement.domain.ConciliationEntry;
import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.repository.ConciliationEntryRepository;
import cm.gimac.des.rapprochement.service.ConciliationEntryService;
import cm.gimac.des.rapprochement.service.mapper.ConciliationEntryMapper;
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
 * Integration tests for the {@link ConciliationEntryResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ConciliationEntryResourceIT {

    private static final UUID DEFAULT_AUTHOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_AUTHOR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_RECORDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RECORDED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_TEXT = "AAAAAAAAAA";
    private static final String UPDATED_TEXT = "BBBBBBBBBB";

    private static final UUID DEFAULT_DOCUMENT_REF = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_REF = UUID.randomUUID();

    private static final String ENTITY_API_URL = "/api/conciliation-entries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ConciliationEntryRepository conciliationEntryRepository;

    @Mock
    private ConciliationEntryRepository conciliationEntryRepositoryMock;

    @Autowired
    private ConciliationEntryMapper conciliationEntryMapper;

    @Mock
    private ConciliationEntryService conciliationEntryServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restConciliationEntryMockMvc;

    private ConciliationEntry conciliationEntry;

    private ConciliationEntry insertedConciliationEntry;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ConciliationEntry createEntity(EntityManager em) {
        ConciliationEntry conciliationEntry = new ConciliationEntry()
            .authorId(DEFAULT_AUTHOR_ID)
            .recordedAt(DEFAULT_RECORDED_AT)
            .text(DEFAULT_TEXT)
            .documentRef(DEFAULT_DOCUMENT_REF);
        // Add required entity
        Suspens suspens;
        if (TestUtil.findAll(em, Suspens.class).isEmpty()) {
            suspens = SuspensResourceIT.createEntity(em);
            em.persist(suspens);
            em.flush();
        } else {
            suspens = TestUtil.findAll(em, Suspens.class).get(0);
        }
        conciliationEntry.setSuspens(suspens);
        return conciliationEntry;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ConciliationEntry createUpdatedEntity(EntityManager em) {
        ConciliationEntry updatedConciliationEntry = new ConciliationEntry()
            .authorId(UPDATED_AUTHOR_ID)
            .recordedAt(UPDATED_RECORDED_AT)
            .text(UPDATED_TEXT)
            .documentRef(UPDATED_DOCUMENT_REF);
        // Add required entity
        Suspens suspens;
        if (TestUtil.findAll(em, Suspens.class).isEmpty()) {
            suspens = SuspensResourceIT.createUpdatedEntity(em);
            em.persist(suspens);
            em.flush();
        } else {
            suspens = TestUtil.findAll(em, Suspens.class).get(0);
        }
        updatedConciliationEntry.setSuspens(suspens);
        return updatedConciliationEntry;
    }

    @BeforeEach
    void initTest() {
        conciliationEntry = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedConciliationEntry != null) {
            conciliationEntryRepository.delete(insertedConciliationEntry);
            insertedConciliationEntry = null;
        }
    }

    @Test
    @Transactional
    void getAllConciliationEntries() throws Exception {
        // Initialize the database
        insertedConciliationEntry = conciliationEntryRepository.saveAndFlush(conciliationEntry);

        // Get all the conciliationEntryList
        restConciliationEntryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(conciliationEntry.getId().toString())))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].recordedAt").value(hasItem(DEFAULT_RECORDED_AT.toString())))
            .andExpect(jsonPath("$.[*].text").value(hasItem(DEFAULT_TEXT)))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllConciliationEntriesWithEagerRelationshipsIsEnabled() throws Exception {
        when(conciliationEntryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restConciliationEntryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(conciliationEntryServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllConciliationEntriesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(conciliationEntryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restConciliationEntryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(conciliationEntryRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getConciliationEntry() throws Exception {
        // Initialize the database
        insertedConciliationEntry = conciliationEntryRepository.saveAndFlush(conciliationEntry);

        // Get the conciliationEntry
        restConciliationEntryMockMvc
            .perform(get(ENTITY_API_URL_ID, conciliationEntry.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(conciliationEntry.getId().toString()))
            .andExpect(jsonPath("$.authorId").value(DEFAULT_AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.recordedAt").value(DEFAULT_RECORDED_AT.toString()))
            .andExpect(jsonPath("$.text").value(DEFAULT_TEXT))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF.toString()));
    }

    @Test
    @Transactional
    void getNonExistingConciliationEntry() throws Exception {
        // Get the conciliationEntry
        restConciliationEntryMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return conciliationEntryRepository.count();
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

    protected ConciliationEntry getPersistedConciliationEntry(ConciliationEntry conciliationEntry) {
        return conciliationEntryRepository.findById(conciliationEntry.getId()).orElseThrow();
    }

    protected void assertPersistedConciliationEntryToMatchAllProperties(ConciliationEntry expectedConciliationEntry) {
        assertConciliationEntryAllPropertiesEquals(expectedConciliationEntry, getPersistedConciliationEntry(expectedConciliationEntry));
    }

    protected void assertPersistedConciliationEntryToMatchUpdatableProperties(ConciliationEntry expectedConciliationEntry) {
        assertConciliationEntryAllUpdatablePropertiesEquals(
            expectedConciliationEntry,
            getPersistedConciliationEntry(expectedConciliationEntry)
        );
    }
}
