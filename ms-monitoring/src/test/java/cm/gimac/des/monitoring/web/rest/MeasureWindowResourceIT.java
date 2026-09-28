package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.MeasureWindowAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.MeasureWindow;
import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.repository.MeasureWindowRepository;
import cm.gimac.des.monitoring.service.MeasureWindowService;
import cm.gimac.des.monitoring.service.mapper.MeasureWindowMapper;
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
 * Integration tests for the {@link MeasureWindowResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class MeasureWindowResourceIT {

    private static final Instant DEFAULT_WINDOW_START = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_WINDOW_START = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_WINDOW_END = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_WINDOW_END = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_MEASURE_VALUES = "AAAAAAAAAA";
    private static final String UPDATED_MEASURE_VALUES = "BBBBBBBBBB";

    private static final Instant DEFAULT_RECEIVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RECEIVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/measure-windows";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MeasureWindowRepository measureWindowRepository;

    @Mock
    private MeasureWindowRepository measureWindowRepositoryMock;

    @Autowired
    private MeasureWindowMapper measureWindowMapper;

    @Mock
    private MeasureWindowService measureWindowServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMeasureWindowMockMvc;

    private MeasureWindow measureWindow;

    private MeasureWindow insertedMeasureWindow;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MeasureWindow createEntity(EntityManager em) {
        MeasureWindow measureWindow = new MeasureWindow()
            .windowStart(DEFAULT_WINDOW_START)
            .windowEnd(DEFAULT_WINDOW_END)
            .measureValues(DEFAULT_MEASURE_VALUES)
            .receivedAt(DEFAULT_RECEIVED_AT);
        // Add required entity
        SupervisedLink supervisedLink;
        if (TestUtil.findAll(em, SupervisedLink.class).isEmpty()) {
            supervisedLink = SupervisedLinkResourceIT.createEntity();
            em.persist(supervisedLink);
            em.flush();
        } else {
            supervisedLink = TestUtil.findAll(em, SupervisedLink.class).get(0);
        }
        measureWindow.setLink(supervisedLink);
        return measureWindow;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MeasureWindow createUpdatedEntity(EntityManager em) {
        MeasureWindow updatedMeasureWindow = new MeasureWindow()
            .windowStart(UPDATED_WINDOW_START)
            .windowEnd(UPDATED_WINDOW_END)
            .measureValues(UPDATED_MEASURE_VALUES)
            .receivedAt(UPDATED_RECEIVED_AT);
        // Add required entity
        SupervisedLink supervisedLink;
        if (TestUtil.findAll(em, SupervisedLink.class).isEmpty()) {
            supervisedLink = SupervisedLinkResourceIT.createUpdatedEntity();
            em.persist(supervisedLink);
            em.flush();
        } else {
            supervisedLink = TestUtil.findAll(em, SupervisedLink.class).get(0);
        }
        updatedMeasureWindow.setLink(supervisedLink);
        return updatedMeasureWindow;
    }

    @BeforeEach
    void initTest() {
        measureWindow = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedMeasureWindow != null) {
            measureWindowRepository.delete(insertedMeasureWindow);
            insertedMeasureWindow = null;
        }
    }

    @Test
    @Transactional
    void getAllMeasureWindows() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        // Get all the measureWindowList
        restMeasureWindowMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(measureWindow.getId().toString())))
            .andExpect(jsonPath("$.[*].windowStart").value(hasItem(DEFAULT_WINDOW_START.toString())))
            .andExpect(jsonPath("$.[*].windowEnd").value(hasItem(DEFAULT_WINDOW_END.toString())))
            .andExpect(jsonPath("$.[*].measureValues").value(hasItem(DEFAULT_MEASURE_VALUES)))
            .andExpect(jsonPath("$.[*].receivedAt").value(hasItem(DEFAULT_RECEIVED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMeasureWindowsWithEagerRelationshipsIsEnabled() throws Exception {
        when(measureWindowServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMeasureWindowMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(measureWindowServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMeasureWindowsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(measureWindowServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMeasureWindowMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(measureWindowRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getMeasureWindow() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        // Get the measureWindow
        restMeasureWindowMockMvc
            .perform(get(ENTITY_API_URL_ID, measureWindow.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(measureWindow.getId().toString()))
            .andExpect(jsonPath("$.windowStart").value(DEFAULT_WINDOW_START.toString()))
            .andExpect(jsonPath("$.windowEnd").value(DEFAULT_WINDOW_END.toString()))
            .andExpect(jsonPath("$.measureValues").value(DEFAULT_MEASURE_VALUES))
            .andExpect(jsonPath("$.receivedAt").value(DEFAULT_RECEIVED_AT.toString()));
    }

    @Test
    @Transactional
    void getMeasureWindowsByIdFiltering() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        UUID id = measureWindow.getId();

        defaultMeasureWindowFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllMeasureWindowsByWindowStartIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        // Get all the measureWindowList where windowStart equals to
        defaultMeasureWindowFiltering("windowStart.equals=" + DEFAULT_WINDOW_START, "windowStart.equals=" + UPDATED_WINDOW_START);
    }

    @Test
    @Transactional
    void getAllMeasureWindowsByWindowStartIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        // Get all the measureWindowList where windowStart in
        defaultMeasureWindowFiltering(
            "windowStart.in=" + DEFAULT_WINDOW_START + "," + UPDATED_WINDOW_START,
            "windowStart.in=" + UPDATED_WINDOW_START
        );
    }

    @Test
    @Transactional
    void getAllMeasureWindowsByWindowStartIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        // Get all the measureWindowList where windowStart is not null
        defaultMeasureWindowFiltering("windowStart.specified=true", "windowStart.specified=false");
    }

    @Test
    @Transactional
    void getAllMeasureWindowsByWindowEndIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        // Get all the measureWindowList where windowEnd equals to
        defaultMeasureWindowFiltering("windowEnd.equals=" + DEFAULT_WINDOW_END, "windowEnd.equals=" + UPDATED_WINDOW_END);
    }

    @Test
    @Transactional
    void getAllMeasureWindowsByWindowEndIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        // Get all the measureWindowList where windowEnd in
        defaultMeasureWindowFiltering(
            "windowEnd.in=" + DEFAULT_WINDOW_END + "," + UPDATED_WINDOW_END,
            "windowEnd.in=" + UPDATED_WINDOW_END
        );
    }

    @Test
    @Transactional
    void getAllMeasureWindowsByWindowEndIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        // Get all the measureWindowList where windowEnd is not null
        defaultMeasureWindowFiltering("windowEnd.specified=true", "windowEnd.specified=false");
    }

    @Test
    @Transactional
    void getAllMeasureWindowsByReceivedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        // Get all the measureWindowList where receivedAt equals to
        defaultMeasureWindowFiltering("receivedAt.equals=" + DEFAULT_RECEIVED_AT, "receivedAt.equals=" + UPDATED_RECEIVED_AT);
    }

    @Test
    @Transactional
    void getAllMeasureWindowsByReceivedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        // Get all the measureWindowList where receivedAt in
        defaultMeasureWindowFiltering(
            "receivedAt.in=" + DEFAULT_RECEIVED_AT + "," + UPDATED_RECEIVED_AT,
            "receivedAt.in=" + UPDATED_RECEIVED_AT
        );
    }

    @Test
    @Transactional
    void getAllMeasureWindowsByReceivedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeasureWindow = measureWindowRepository.saveAndFlush(measureWindow);

        // Get all the measureWindowList where receivedAt is not null
        defaultMeasureWindowFiltering("receivedAt.specified=true", "receivedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllMeasureWindowsByLinkIsEqualToSomething() throws Exception {
        SupervisedLink link;
        if (TestUtil.findAll(em, SupervisedLink.class).isEmpty()) {
            measureWindowRepository.saveAndFlush(measureWindow);
            link = SupervisedLinkResourceIT.createEntity();
        } else {
            link = TestUtil.findAll(em, SupervisedLink.class).get(0);
        }
        em.persist(link);
        em.flush();
        measureWindow.setLink(link);
        measureWindowRepository.saveAndFlush(measureWindow);
        UUID linkId = link.getId();
        // Get all the measureWindowList where link equals to linkId
        defaultMeasureWindowShouldBeFound("linkId.equals=" + linkId);

        // Get all the measureWindowList where link equals to UUID.randomUUID()
        defaultMeasureWindowShouldNotBeFound("linkId.equals=" + UUID.randomUUID());
    }

    private void defaultMeasureWindowFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultMeasureWindowShouldBeFound(shouldBeFound);
        defaultMeasureWindowShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultMeasureWindowShouldBeFound(String filter) throws Exception {
        restMeasureWindowMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(measureWindow.getId().toString())))
            .andExpect(jsonPath("$.[*].windowStart").value(hasItem(DEFAULT_WINDOW_START.toString())))
            .andExpect(jsonPath("$.[*].windowEnd").value(hasItem(DEFAULT_WINDOW_END.toString())))
            .andExpect(jsonPath("$.[*].measureValues").value(hasItem(DEFAULT_MEASURE_VALUES)))
            .andExpect(jsonPath("$.[*].receivedAt").value(hasItem(DEFAULT_RECEIVED_AT.toString())));

        // Check, that the count call also returns 1
        restMeasureWindowMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultMeasureWindowShouldNotBeFound(String filter) throws Exception {
        restMeasureWindowMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restMeasureWindowMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingMeasureWindow() throws Exception {
        // Get the measureWindow
        restMeasureWindowMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return measureWindowRepository.count();
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

    protected MeasureWindow getPersistedMeasureWindow(MeasureWindow measureWindow) {
        return measureWindowRepository.findById(measureWindow.getId()).orElseThrow();
    }

    protected void assertPersistedMeasureWindowToMatchAllProperties(MeasureWindow expectedMeasureWindow) {
        assertMeasureWindowAllPropertiesEquals(expectedMeasureWindow, getPersistedMeasureWindow(expectedMeasureWindow));
    }

    protected void assertPersistedMeasureWindowToMatchUpdatableProperties(MeasureWindow expectedMeasureWindow) {
        assertMeasureWindowAllUpdatablePropertiesEquals(expectedMeasureWindow, getPersistedMeasureWindow(expectedMeasureWindow));
    }
}
