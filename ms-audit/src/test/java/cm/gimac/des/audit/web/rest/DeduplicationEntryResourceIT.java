package cm.gimac.des.audit.web.rest;

import static cm.gimac.des.audit.domain.DeduplicationEntryAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.audit.IntegrationTest;
import cm.gimac.des.audit.domain.DeduplicationEntry;
import cm.gimac.des.audit.repository.DeduplicationEntryRepository;
import cm.gimac.des.audit.service.mapper.DeduplicationEntryMapper;
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
 * Integration tests for the {@link DeduplicationEntryResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DeduplicationEntryResourceIT {

    private static final UUID DEFAULT_EVENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_EVENT_ID = UUID.randomUUID();

    private static final Long DEFAULT_AUDIT_INDEX = 0L;
    private static final Long UPDATED_AUDIT_INDEX = 1L;

    private static final Instant DEFAULT_RECORDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RECORDED_AT = Instant.ofEpochMilli(1701661978853L);

    private static final String ENTITY_API_URL = "/api/deduplication-entries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DeduplicationEntryRepository deduplicationEntryRepository;

    @Autowired
    private DeduplicationEntryMapper deduplicationEntryMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDeduplicationEntryMockMvc;

    private DeduplicationEntry deduplicationEntry;

    private DeduplicationEntry insertedDeduplicationEntry;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DeduplicationEntry createEntity() {
        return new DeduplicationEntry().eventId(DEFAULT_EVENT_ID).auditIndex(DEFAULT_AUDIT_INDEX).recordedAt(DEFAULT_RECORDED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DeduplicationEntry createUpdatedEntity() {
        return new DeduplicationEntry().eventId(UPDATED_EVENT_ID).auditIndex(UPDATED_AUDIT_INDEX).recordedAt(UPDATED_RECORDED_AT);
    }

    @BeforeEach
    void initTest() {
        deduplicationEntry = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedDeduplicationEntry != null) {
            deduplicationEntryRepository.delete(insertedDeduplicationEntry);
            insertedDeduplicationEntry = null;
        }
    }

    @Test
    @Transactional
    void getAllDeduplicationEntries() throws Exception {
        // Initialize the database
        insertedDeduplicationEntry = deduplicationEntryRepository.saveAndFlush(deduplicationEntry);

        // Get all the deduplicationEntryList
        restDeduplicationEntryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(deduplicationEntry.getId().toString())))
            .andExpect(jsonPath("$.[*].eventId").value(hasItem(DEFAULT_EVENT_ID.toString())))
            .andExpect(jsonPath("$.[*].auditIndex").value(hasItem(DEFAULT_AUDIT_INDEX.intValue())))
            .andExpect(jsonPath("$.[*].recordedAt").value(hasItem(DEFAULT_RECORDED_AT.toString())));
    }

    @Test
    @Transactional
    void getDeduplicationEntry() throws Exception {
        // Initialize the database
        insertedDeduplicationEntry = deduplicationEntryRepository.saveAndFlush(deduplicationEntry);

        // Get the deduplicationEntry
        restDeduplicationEntryMockMvc
            .perform(get(ENTITY_API_URL_ID, deduplicationEntry.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(deduplicationEntry.getId().toString()))
            .andExpect(jsonPath("$.eventId").value(DEFAULT_EVENT_ID.toString()))
            .andExpect(jsonPath("$.auditIndex").value(DEFAULT_AUDIT_INDEX.intValue()))
            .andExpect(jsonPath("$.recordedAt").value(DEFAULT_RECORDED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingDeduplicationEntry() throws Exception {
        // Get the deduplicationEntry
        restDeduplicationEntryMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return deduplicationEntryRepository.count();
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

    protected DeduplicationEntry getPersistedDeduplicationEntry(DeduplicationEntry deduplicationEntry) {
        return deduplicationEntryRepository.findById(deduplicationEntry.getId()).orElseThrow();
    }

    protected void assertPersistedDeduplicationEntryToMatchAllProperties(DeduplicationEntry expectedDeduplicationEntry) {
        assertDeduplicationEntryAllPropertiesEquals(expectedDeduplicationEntry, getPersistedDeduplicationEntry(expectedDeduplicationEntry));
    }

    protected void assertPersistedDeduplicationEntryToMatchUpdatableProperties(DeduplicationEntry expectedDeduplicationEntry) {
        assertDeduplicationEntryAllUpdatablePropertiesEquals(
            expectedDeduplicationEntry,
            getPersistedDeduplicationEntry(expectedDeduplicationEntry)
        );
    }
}
