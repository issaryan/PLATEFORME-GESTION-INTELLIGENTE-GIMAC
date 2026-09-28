package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.RecordingConsentAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.domain.RecordingConsent;
import cm.gimac.des.collaboration.repository.RecordingConsentRepository;
import cm.gimac.des.collaboration.service.RecordingConsentService;
import cm.gimac.des.collaboration.service.mapper.RecordingConsentMapper;
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
 * Integration tests for the {@link RecordingConsentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RecordingConsentResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final Boolean DEFAULT_GRANTED = false;
    private static final Boolean UPDATED_GRANTED = true;

    private static final Instant DEFAULT_DECIDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DECIDED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/recording-consents";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RecordingConsentRepository recordingConsentRepository;

    @Mock
    private RecordingConsentRepository recordingConsentRepositoryMock;

    @Autowired
    private RecordingConsentMapper recordingConsentMapper;

    @Mock
    private RecordingConsentService recordingConsentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRecordingConsentMockMvc;

    private RecordingConsent recordingConsent;

    private RecordingConsent insertedRecordingConsent;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RecordingConsent createEntity(EntityManager em) {
        RecordingConsent recordingConsent = new RecordingConsent()
            .userId(DEFAULT_USER_ID)
            .granted(DEFAULT_GRANTED)
            .decidedAt(DEFAULT_DECIDED_AT);
        // Add required entity
        Conference conference;
        if (TestUtil.findAll(em, Conference.class).isEmpty()) {
            conference = ConferenceResourceIT.createEntity(em);
            em.persist(conference);
            em.flush();
        } else {
            conference = TestUtil.findAll(em, Conference.class).get(0);
        }
        recordingConsent.setConference(conference);
        return recordingConsent;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RecordingConsent createUpdatedEntity(EntityManager em) {
        RecordingConsent updatedRecordingConsent = new RecordingConsent()
            .userId(UPDATED_USER_ID)
            .granted(UPDATED_GRANTED)
            .decidedAt(UPDATED_DECIDED_AT);
        // Add required entity
        Conference conference;
        if (TestUtil.findAll(em, Conference.class).isEmpty()) {
            conference = ConferenceResourceIT.createUpdatedEntity(em);
            em.persist(conference);
            em.flush();
        } else {
            conference = TestUtil.findAll(em, Conference.class).get(0);
        }
        updatedRecordingConsent.setConference(conference);
        return updatedRecordingConsent;
    }

    @BeforeEach
    void initTest() {
        recordingConsent = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedRecordingConsent != null) {
            recordingConsentRepository.delete(insertedRecordingConsent);
            insertedRecordingConsent = null;
        }
    }

    @Test
    @Transactional
    void getAllRecordingConsents() throws Exception {
        // Initialize the database
        insertedRecordingConsent = recordingConsentRepository.saveAndFlush(recordingConsent);

        // Get all the recordingConsentList
        restRecordingConsentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(recordingConsent.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].granted").value(hasItem(DEFAULT_GRANTED)))
            .andExpect(jsonPath("$.[*].decidedAt").value(hasItem(DEFAULT_DECIDED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRecordingConsentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(recordingConsentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRecordingConsentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(recordingConsentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRecordingConsentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(recordingConsentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRecordingConsentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(recordingConsentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRecordingConsent() throws Exception {
        // Initialize the database
        insertedRecordingConsent = recordingConsentRepository.saveAndFlush(recordingConsent);

        // Get the recordingConsent
        restRecordingConsentMockMvc
            .perform(get(ENTITY_API_URL_ID, recordingConsent.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(recordingConsent.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.granted").value(DEFAULT_GRANTED))
            .andExpect(jsonPath("$.decidedAt").value(DEFAULT_DECIDED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingRecordingConsent() throws Exception {
        // Get the recordingConsent
        restRecordingConsentMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return recordingConsentRepository.count();
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

    protected RecordingConsent getPersistedRecordingConsent(RecordingConsent recordingConsent) {
        return recordingConsentRepository.findById(recordingConsent.getId()).orElseThrow();
    }

    protected void assertPersistedRecordingConsentToMatchAllProperties(RecordingConsent expectedRecordingConsent) {
        assertRecordingConsentAllPropertiesEquals(expectedRecordingConsent, getPersistedRecordingConsent(expectedRecordingConsent));
    }

    protected void assertPersistedRecordingConsentToMatchUpdatableProperties(RecordingConsent expectedRecordingConsent) {
        assertRecordingConsentAllUpdatablePropertiesEquals(
            expectedRecordingConsent,
            getPersistedRecordingConsent(expectedRecordingConsent)
        );
    }
}
