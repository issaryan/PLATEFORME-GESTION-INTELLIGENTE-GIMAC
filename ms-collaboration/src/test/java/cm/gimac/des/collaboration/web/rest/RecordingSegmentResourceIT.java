package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.RecordingSegmentAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.domain.RecordingSegment;
import cm.gimac.des.collaboration.repository.RecordingSegmentRepository;
import cm.gimac.des.collaboration.service.RecordingSegmentService;
import cm.gimac.des.collaboration.service.mapper.RecordingSegmentMapper;
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
 * Integration tests for the {@link RecordingSegmentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RecordingSegmentResourceIT {

    private static final Instant DEFAULT_STARTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_ENDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ENDED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_EGRESS_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_EGRESS_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_CONSENTING_USER_IDS = "AAAAAAAAAA";
    private static final String UPDATED_CONSENTING_USER_IDS = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/recording-segments";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RecordingSegmentRepository recordingSegmentRepository;

    @Mock
    private RecordingSegmentRepository recordingSegmentRepositoryMock;

    @Autowired
    private RecordingSegmentMapper recordingSegmentMapper;

    @Mock
    private RecordingSegmentService recordingSegmentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRecordingSegmentMockMvc;

    private RecordingSegment recordingSegment;

    private RecordingSegment insertedRecordingSegment;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RecordingSegment createEntity(EntityManager em) {
        RecordingSegment recordingSegment = new RecordingSegment()
            .startedAt(DEFAULT_STARTED_AT)
            .endedAt(DEFAULT_ENDED_AT)
            .egressReference(DEFAULT_EGRESS_REFERENCE)
            .consentingUserIds(DEFAULT_CONSENTING_USER_IDS);
        // Add required entity
        Conference conference;
        if (TestUtil.findAll(em, Conference.class).isEmpty()) {
            conference = ConferenceResourceIT.createEntity(em);
            em.persist(conference);
            em.flush();
        } else {
            conference = TestUtil.findAll(em, Conference.class).get(0);
        }
        recordingSegment.setConference(conference);
        return recordingSegment;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RecordingSegment createUpdatedEntity(EntityManager em) {
        RecordingSegment updatedRecordingSegment = new RecordingSegment()
            .startedAt(UPDATED_STARTED_AT)
            .endedAt(UPDATED_ENDED_AT)
            .egressReference(UPDATED_EGRESS_REFERENCE)
            .consentingUserIds(UPDATED_CONSENTING_USER_IDS);
        // Add required entity
        Conference conference;
        if (TestUtil.findAll(em, Conference.class).isEmpty()) {
            conference = ConferenceResourceIT.createUpdatedEntity(em);
            em.persist(conference);
            em.flush();
        } else {
            conference = TestUtil.findAll(em, Conference.class).get(0);
        }
        updatedRecordingSegment.setConference(conference);
        return updatedRecordingSegment;
    }

    @BeforeEach
    void initTest() {
        recordingSegment = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedRecordingSegment != null) {
            recordingSegmentRepository.delete(insertedRecordingSegment);
            insertedRecordingSegment = null;
        }
    }

    @Test
    @Transactional
    void getAllRecordingSegments() throws Exception {
        // Initialize the database
        insertedRecordingSegment = recordingSegmentRepository.saveAndFlush(recordingSegment);

        // Get all the recordingSegmentList
        restRecordingSegmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(recordingSegment.getId().toString())))
            .andExpect(jsonPath("$.[*].startedAt").value(hasItem(DEFAULT_STARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].endedAt").value(hasItem(DEFAULT_ENDED_AT.toString())))
            .andExpect(jsonPath("$.[*].egressReference").value(hasItem(DEFAULT_EGRESS_REFERENCE)))
            .andExpect(jsonPath("$.[*].consentingUserIds").value(hasItem(DEFAULT_CONSENTING_USER_IDS)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRecordingSegmentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(recordingSegmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRecordingSegmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(recordingSegmentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRecordingSegmentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(recordingSegmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRecordingSegmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(recordingSegmentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRecordingSegment() throws Exception {
        // Initialize the database
        insertedRecordingSegment = recordingSegmentRepository.saveAndFlush(recordingSegment);

        // Get the recordingSegment
        restRecordingSegmentMockMvc
            .perform(get(ENTITY_API_URL_ID, recordingSegment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(recordingSegment.getId().toString()))
            .andExpect(jsonPath("$.startedAt").value(DEFAULT_STARTED_AT.toString()))
            .andExpect(jsonPath("$.endedAt").value(DEFAULT_ENDED_AT.toString()))
            .andExpect(jsonPath("$.egressReference").value(DEFAULT_EGRESS_REFERENCE))
            .andExpect(jsonPath("$.consentingUserIds").value(DEFAULT_CONSENTING_USER_IDS));
    }

    @Test
    @Transactional
    void getNonExistingRecordingSegment() throws Exception {
        // Get the recordingSegment
        restRecordingSegmentMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return recordingSegmentRepository.count();
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

    protected RecordingSegment getPersistedRecordingSegment(RecordingSegment recordingSegment) {
        return recordingSegmentRepository.findById(recordingSegment.getId()).orElseThrow();
    }

    protected void assertPersistedRecordingSegmentToMatchAllProperties(RecordingSegment expectedRecordingSegment) {
        assertRecordingSegmentAllPropertiesEquals(expectedRecordingSegment, getPersistedRecordingSegment(expectedRecordingSegment));
    }

    protected void assertPersistedRecordingSegmentToMatchUpdatableProperties(RecordingSegment expectedRecordingSegment) {
        assertRecordingSegmentAllUpdatablePropertiesEquals(
            expectedRecordingSegment,
            getPersistedRecordingSegment(expectedRecordingSegment)
        );
    }
}
