package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.MediaKeyEpochAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.domain.MediaKeyEpoch;
import cm.gimac.des.collaboration.domain.enumeration.MediaKeyRotationReason;
import cm.gimac.des.collaboration.repository.MediaKeyEpochRepository;
import cm.gimac.des.collaboration.service.MediaKeyEpochService;
import cm.gimac.des.collaboration.service.mapper.MediaKeyEpochMapper;
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
 * Integration tests for the {@link MediaKeyEpochResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class MediaKeyEpochResourceIT {

    private static final Integer DEFAULT_EPOCH = 0;
    private static final Integer UPDATED_EPOCH = 1;

    private static final MediaKeyRotationReason DEFAULT_REASON = MediaKeyRotationReason.OUVERTURE;
    private static final MediaKeyRotationReason UPDATED_REASON = MediaKeyRotationReason.ENTREE_PARTICIPANT;

    private static final Instant DEFAULT_ROTATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ROTATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/media-key-epoches";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MediaKeyEpochRepository mediaKeyEpochRepository;

    @Mock
    private MediaKeyEpochRepository mediaKeyEpochRepositoryMock;

    @Autowired
    private MediaKeyEpochMapper mediaKeyEpochMapper;

    @Mock
    private MediaKeyEpochService mediaKeyEpochServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMediaKeyEpochMockMvc;

    private MediaKeyEpoch mediaKeyEpoch;

    private MediaKeyEpoch insertedMediaKeyEpoch;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MediaKeyEpoch createEntity(EntityManager em) {
        MediaKeyEpoch mediaKeyEpoch = new MediaKeyEpoch().epoch(DEFAULT_EPOCH).reason(DEFAULT_REASON).rotatedAt(DEFAULT_ROTATED_AT);
        // Add required entity
        Conference conference;
        if (TestUtil.findAll(em, Conference.class).isEmpty()) {
            conference = ConferenceResourceIT.createEntity(em);
            em.persist(conference);
            em.flush();
        } else {
            conference = TestUtil.findAll(em, Conference.class).get(0);
        }
        mediaKeyEpoch.setConference(conference);
        return mediaKeyEpoch;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MediaKeyEpoch createUpdatedEntity(EntityManager em) {
        MediaKeyEpoch updatedMediaKeyEpoch = new MediaKeyEpoch().epoch(UPDATED_EPOCH).reason(UPDATED_REASON).rotatedAt(UPDATED_ROTATED_AT);
        // Add required entity
        Conference conference;
        if (TestUtil.findAll(em, Conference.class).isEmpty()) {
            conference = ConferenceResourceIT.createUpdatedEntity(em);
            em.persist(conference);
            em.flush();
        } else {
            conference = TestUtil.findAll(em, Conference.class).get(0);
        }
        updatedMediaKeyEpoch.setConference(conference);
        return updatedMediaKeyEpoch;
    }

    @BeforeEach
    void initTest() {
        mediaKeyEpoch = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedMediaKeyEpoch != null) {
            mediaKeyEpochRepository.delete(insertedMediaKeyEpoch);
            insertedMediaKeyEpoch = null;
        }
    }

    @Test
    @Transactional
    void getAllMediaKeyEpoches() throws Exception {
        // Initialize the database
        insertedMediaKeyEpoch = mediaKeyEpochRepository.saveAndFlush(mediaKeyEpoch);

        // Get all the mediaKeyEpochList
        restMediaKeyEpochMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(mediaKeyEpoch.getId().toString())))
            .andExpect(jsonPath("$.[*].epoch").value(hasItem(DEFAULT_EPOCH)))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON.toString())))
            .andExpect(jsonPath("$.[*].rotatedAt").value(hasItem(DEFAULT_ROTATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMediaKeyEpochesWithEagerRelationshipsIsEnabled() throws Exception {
        when(mediaKeyEpochServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMediaKeyEpochMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(mediaKeyEpochServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMediaKeyEpochesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(mediaKeyEpochServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMediaKeyEpochMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(mediaKeyEpochRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getMediaKeyEpoch() throws Exception {
        // Initialize the database
        insertedMediaKeyEpoch = mediaKeyEpochRepository.saveAndFlush(mediaKeyEpoch);

        // Get the mediaKeyEpoch
        restMediaKeyEpochMockMvc
            .perform(get(ENTITY_API_URL_ID, mediaKeyEpoch.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(mediaKeyEpoch.getId().toString()))
            .andExpect(jsonPath("$.epoch").value(DEFAULT_EPOCH))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON.toString()))
            .andExpect(jsonPath("$.rotatedAt").value(DEFAULT_ROTATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingMediaKeyEpoch() throws Exception {
        // Get the mediaKeyEpoch
        restMediaKeyEpochMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return mediaKeyEpochRepository.count();
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

    protected MediaKeyEpoch getPersistedMediaKeyEpoch(MediaKeyEpoch mediaKeyEpoch) {
        return mediaKeyEpochRepository.findById(mediaKeyEpoch.getId()).orElseThrow();
    }

    protected void assertPersistedMediaKeyEpochToMatchAllProperties(MediaKeyEpoch expectedMediaKeyEpoch) {
        assertMediaKeyEpochAllPropertiesEquals(expectedMediaKeyEpoch, getPersistedMediaKeyEpoch(expectedMediaKeyEpoch));
    }

    protected void assertPersistedMediaKeyEpochToMatchUpdatableProperties(MediaKeyEpoch expectedMediaKeyEpoch) {
        assertMediaKeyEpochAllUpdatablePropertiesEquals(expectedMediaKeyEpoch, getPersistedMediaKeyEpoch(expectedMediaKeyEpoch));
    }
}
