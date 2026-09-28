package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.ProcessedEventAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.ProcessedEvent;
import cm.gimac.des.collaboration.repository.ProcessedEventRepository;
import cm.gimac.des.collaboration.service.mapper.ProcessedEventMapper;
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
 * Integration tests for the {@link ProcessedEventResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ProcessedEventResourceIT {

    private static final UUID DEFAULT_EVENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_EVENT_ID = UUID.randomUUID();

    private static final String DEFAULT_SUBJECT = "AAAAAAAAAA";
    private static final String UPDATED_SUBJECT = "BBBBBBBBBB";

    private static final Instant DEFAULT_PROCESSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PROCESSED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/processed-events";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ProcessedEventMapper processedEventMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProcessedEventMockMvc;

    private ProcessedEvent processedEvent;

    private ProcessedEvent insertedProcessedEvent;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ProcessedEvent createEntity() {
        return new ProcessedEvent().eventId(DEFAULT_EVENT_ID).subject(DEFAULT_SUBJECT).processedAt(DEFAULT_PROCESSED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ProcessedEvent createUpdatedEntity() {
        return new ProcessedEvent().eventId(UPDATED_EVENT_ID).subject(UPDATED_SUBJECT).processedAt(UPDATED_PROCESSED_AT);
    }

    @BeforeEach
    void initTest() {
        processedEvent = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedProcessedEvent != null) {
            processedEventRepository.delete(insertedProcessedEvent);
            insertedProcessedEvent = null;
        }
    }

    @Test
    @Transactional
    void getAllProcessedEvents() throws Exception {
        // Initialize the database
        insertedProcessedEvent = processedEventRepository.saveAndFlush(processedEvent);

        // Get all the processedEventList
        restProcessedEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(processedEvent.getId().toString())))
            .andExpect(jsonPath("$.[*].eventId").value(hasItem(DEFAULT_EVENT_ID.toString())))
            .andExpect(jsonPath("$.[*].subject").value(hasItem(DEFAULT_SUBJECT)))
            .andExpect(jsonPath("$.[*].processedAt").value(hasItem(DEFAULT_PROCESSED_AT.toString())));
    }

    @Test
    @Transactional
    void getProcessedEvent() throws Exception {
        // Initialize the database
        insertedProcessedEvent = processedEventRepository.saveAndFlush(processedEvent);

        // Get the processedEvent
        restProcessedEventMockMvc
            .perform(get(ENTITY_API_URL_ID, processedEvent.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(processedEvent.getId().toString()))
            .andExpect(jsonPath("$.eventId").value(DEFAULT_EVENT_ID.toString()))
            .andExpect(jsonPath("$.subject").value(DEFAULT_SUBJECT))
            .andExpect(jsonPath("$.processedAt").value(DEFAULT_PROCESSED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingProcessedEvent() throws Exception {
        // Get the processedEvent
        restProcessedEventMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return processedEventRepository.count();
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

    protected ProcessedEvent getPersistedProcessedEvent(ProcessedEvent processedEvent) {
        return processedEventRepository.findById(processedEvent.getId()).orElseThrow();
    }

    protected void assertPersistedProcessedEventToMatchAllProperties(ProcessedEvent expectedProcessedEvent) {
        assertProcessedEventAllPropertiesEquals(expectedProcessedEvent, getPersistedProcessedEvent(expectedProcessedEvent));
    }

    protected void assertPersistedProcessedEventToMatchUpdatableProperties(ProcessedEvent expectedProcessedEvent) {
        assertProcessedEventAllUpdatablePropertiesEquals(expectedProcessedEvent, getPersistedProcessedEvent(expectedProcessedEvent));
    }
}
