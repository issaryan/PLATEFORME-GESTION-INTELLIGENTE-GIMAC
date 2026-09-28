package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.DeduplicationEntryAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.DeduplicationEntry;
import cm.gimac.des.certification.repository.DeduplicationEntryRepository;
import cm.gimac.des.certification.service.mapper.DeduplicationEntryMapper;
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
 * Integration tests for the {@link DeduplicationEntryResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DeduplicationEntryResourceIT {

    private static final UUID DEFAULT_EVENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_EVENT_ID = UUID.randomUUID();

    private static final String DEFAULT_SUBJECT = "AAAAAAAAAA";
    private static final String UPDATED_SUBJECT = "BBBBBBBBBB";

    private static final Instant DEFAULT_PROCESSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PROCESSED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

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
        return new DeduplicationEntry().eventId(DEFAULT_EVENT_ID).subject(DEFAULT_SUBJECT).processedAt(DEFAULT_PROCESSED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DeduplicationEntry createUpdatedEntity() {
        return new DeduplicationEntry().eventId(UPDATED_EVENT_ID).subject(UPDATED_SUBJECT).processedAt(UPDATED_PROCESSED_AT);
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
            .andExpect(jsonPath("$.[*].subject").value(hasItem(DEFAULT_SUBJECT)))
            .andExpect(jsonPath("$.[*].processedAt").value(hasItem(DEFAULT_PROCESSED_AT.toString())));
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
            .andExpect(jsonPath("$.subject").value(DEFAULT_SUBJECT))
            .andExpect(jsonPath("$.processedAt").value(DEFAULT_PROCESSED_AT.toString()));
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
