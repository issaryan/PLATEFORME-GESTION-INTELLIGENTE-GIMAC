package cm.gimac.des.notification.web.rest;

import static cm.gimac.des.notification.domain.ConsolidationWindowAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.notification.IntegrationTest;
import cm.gimac.des.notification.domain.ConsolidationWindow;
import cm.gimac.des.notification.repository.ConsolidationWindowRepository;
import cm.gimac.des.notification.service.mapper.ConsolidationWindowMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Duration;
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
 * Integration tests for the {@link ConsolidationWindowResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ConsolidationWindowResourceIT {

    private static final String DEFAULT_GROUP_KEY = "AAAAAAAAAA";
    private static final String UPDATED_GROUP_KEY = "BBBBBBBBBB";

    private static final Instant DEFAULT_OPENS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OPENS_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_CLOSES_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLOSES_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Duration DEFAULT_WINDOW_DURATION = Duration.ofHours(6);
    private static final Duration UPDATED_WINDOW_DURATION = Duration.ofHours(12);

    private static final Integer DEFAULT_MEMBER_COUNT = 0;
    private static final Integer UPDATED_MEMBER_COUNT = 1;

    private static final Boolean DEFAULT_CLOSED = false;
    private static final Boolean UPDATED_CLOSED = true;

    private static final Boolean DEFAULT_CLOSED_EARLY = false;
    private static final Boolean UPDATED_CLOSED_EARLY = true;

    private static final String ENTITY_API_URL = "/api/consolidation-windows";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ConsolidationWindowRepository consolidationWindowRepository;

    @Autowired
    private ConsolidationWindowMapper consolidationWindowMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restConsolidationWindowMockMvc;

    private ConsolidationWindow consolidationWindow;

    private ConsolidationWindow insertedConsolidationWindow;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ConsolidationWindow createEntity() {
        return new ConsolidationWindow()
            .groupKey(DEFAULT_GROUP_KEY)
            .opensAt(DEFAULT_OPENS_AT)
            .closesAt(DEFAULT_CLOSES_AT)
            .windowDuration(DEFAULT_WINDOW_DURATION)
            .memberCount(DEFAULT_MEMBER_COUNT)
            .closed(DEFAULT_CLOSED)
            .closedEarly(DEFAULT_CLOSED_EARLY);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ConsolidationWindow createUpdatedEntity() {
        return new ConsolidationWindow()
            .groupKey(UPDATED_GROUP_KEY)
            .opensAt(UPDATED_OPENS_AT)
            .closesAt(UPDATED_CLOSES_AT)
            .windowDuration(UPDATED_WINDOW_DURATION)
            .memberCount(UPDATED_MEMBER_COUNT)
            .closed(UPDATED_CLOSED)
            .closedEarly(UPDATED_CLOSED_EARLY);
    }

    @BeforeEach
    void initTest() {
        consolidationWindow = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedConsolidationWindow != null) {
            consolidationWindowRepository.delete(insertedConsolidationWindow);
            insertedConsolidationWindow = null;
        }
    }

    @Test
    @Transactional
    void getAllConsolidationWindows() throws Exception {
        // Initialize the database
        insertedConsolidationWindow = consolidationWindowRepository.saveAndFlush(consolidationWindow);

        // Get all the consolidationWindowList
        restConsolidationWindowMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(consolidationWindow.getId().toString())))
            .andExpect(jsonPath("$.[*].groupKey").value(hasItem(DEFAULT_GROUP_KEY)))
            .andExpect(jsonPath("$.[*].opensAt").value(hasItem(DEFAULT_OPENS_AT.toString())))
            .andExpect(jsonPath("$.[*].closesAt").value(hasItem(DEFAULT_CLOSES_AT.toString())))
            .andExpect(jsonPath("$.[*].windowDuration").value(hasItem(DEFAULT_WINDOW_DURATION.toString())))
            .andExpect(jsonPath("$.[*].memberCount").value(hasItem(DEFAULT_MEMBER_COUNT)))
            .andExpect(jsonPath("$.[*].closed").value(hasItem(DEFAULT_CLOSED)))
            .andExpect(jsonPath("$.[*].closedEarly").value(hasItem(DEFAULT_CLOSED_EARLY)));
    }

    @Test
    @Transactional
    void getConsolidationWindow() throws Exception {
        // Initialize the database
        insertedConsolidationWindow = consolidationWindowRepository.saveAndFlush(consolidationWindow);

        // Get the consolidationWindow
        restConsolidationWindowMockMvc
            .perform(get(ENTITY_API_URL_ID, consolidationWindow.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(consolidationWindow.getId().toString()))
            .andExpect(jsonPath("$.groupKey").value(DEFAULT_GROUP_KEY))
            .andExpect(jsonPath("$.opensAt").value(DEFAULT_OPENS_AT.toString()))
            .andExpect(jsonPath("$.closesAt").value(DEFAULT_CLOSES_AT.toString()))
            .andExpect(jsonPath("$.windowDuration").value(DEFAULT_WINDOW_DURATION.toString()))
            .andExpect(jsonPath("$.memberCount").value(DEFAULT_MEMBER_COUNT))
            .andExpect(jsonPath("$.closed").value(DEFAULT_CLOSED))
            .andExpect(jsonPath("$.closedEarly").value(DEFAULT_CLOSED_EARLY));
    }

    @Test
    @Transactional
    void getNonExistingConsolidationWindow() throws Exception {
        // Get the consolidationWindow
        restConsolidationWindowMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return consolidationWindowRepository.count();
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

    protected ConsolidationWindow getPersistedConsolidationWindow(ConsolidationWindow consolidationWindow) {
        return consolidationWindowRepository.findById(consolidationWindow.getId()).orElseThrow();
    }

    protected void assertPersistedConsolidationWindowToMatchAllProperties(ConsolidationWindow expectedConsolidationWindow) {
        assertConsolidationWindowAllPropertiesEquals(
            expectedConsolidationWindow,
            getPersistedConsolidationWindow(expectedConsolidationWindow)
        );
    }

    protected void assertPersistedConsolidationWindowToMatchUpdatableProperties(ConsolidationWindow expectedConsolidationWindow) {
        assertConsolidationWindowAllUpdatablePropertiesEquals(
            expectedConsolidationWindow,
            getPersistedConsolidationWindow(expectedConsolidationWindow)
        );
    }
}
