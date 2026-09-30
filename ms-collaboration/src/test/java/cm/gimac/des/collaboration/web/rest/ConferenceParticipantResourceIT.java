package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.ConferenceParticipantAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.domain.ConferenceParticipant;
import cm.gimac.des.collaboration.repository.ConferenceParticipantRepository;
import cm.gimac.des.collaboration.service.ConferenceParticipantService;
import cm.gimac.des.collaboration.service.mapper.ConferenceParticipantMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link ConferenceParticipantResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.collaboration.security.AuthoritiesConstants.ADMIN)
class ConferenceParticipantResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final String DEFAULT_TERMINAL_ID = "AAAAAAAAAA";
    private static final String UPDATED_TERMINAL_ID = "BBBBBBBBBB";

    private static final Instant DEFAULT_JOINED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_JOINED_AT = Instant.ofEpochMilli(1790746747003L);

    private static final Instant DEFAULT_LEFT_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LEFT_AT = Instant.ofEpochMilli(1790746747003L);

    private static final String ENTITY_API_URL = "/api/conference-participants";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ConferenceParticipantRepository conferenceParticipantRepository;

    @Mock
    private ConferenceParticipantRepository conferenceParticipantRepositoryMock;

    @Autowired
    private ConferenceParticipantMapper conferenceParticipantMapper;

    @Mock
    private ConferenceParticipantService conferenceParticipantServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restConferenceParticipantMockMvc;

    private ConferenceParticipant conferenceParticipant;

    private ConferenceParticipant insertedConferenceParticipant;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ConferenceParticipant createEntity(EntityManager em) {
        ConferenceParticipant conferenceParticipant = new ConferenceParticipant()
            .userId(DEFAULT_USER_ID)
            .terminalId(DEFAULT_TERMINAL_ID)
            .joinedAt(DEFAULT_JOINED_AT)
            .leftAt(DEFAULT_LEFT_AT);
        // Add required entity
        Conference conference;
        if (TestUtil.findAll(em, Conference.class).isEmpty()) {
            conference = ConferenceResourceIT.createEntity(em);
            em.persist(conference);
            em.flush();
        } else {
            conference = TestUtil.findAll(em, Conference.class).getFirst();
        }
        conferenceParticipant.setConference(conference);
        return conferenceParticipant;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ConferenceParticipant createUpdatedEntity(EntityManager em) {
        ConferenceParticipant updatedConferenceParticipant = new ConferenceParticipant()
            .userId(UPDATED_USER_ID)
            .terminalId(UPDATED_TERMINAL_ID)
            .joinedAt(UPDATED_JOINED_AT)
            .leftAt(UPDATED_LEFT_AT);
        // Add required entity
        Conference conference;
        if (TestUtil.findAll(em, Conference.class).isEmpty()) {
            conference = ConferenceResourceIT.createUpdatedEntity(em);
            em.persist(conference);
            em.flush();
        } else {
            conference = TestUtil.findAll(em, Conference.class).getFirst();
        }
        updatedConferenceParticipant.setConference(conference);
        return updatedConferenceParticipant;
    }

    @BeforeEach
    void initTest() {
        conferenceParticipant = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedConferenceParticipant != null) {
            conferenceParticipantRepository.delete(insertedConferenceParticipant);
            insertedConferenceParticipant = null;
        }
    }

    @Test
    @Transactional
    void getAllConferenceParticipants() throws Exception {
        // Initialize the database
        insertedConferenceParticipant = conferenceParticipantRepository.saveAndFlush(conferenceParticipant);

        // Get all the conferenceParticipantList
        restConferenceParticipantMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(conferenceParticipant.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].terminalId").value(hasItem(DEFAULT_TERMINAL_ID)))
            .andExpect(jsonPath("$.[*].joinedAt").value(hasItem(DEFAULT_JOINED_AT.toString())))
            .andExpect(jsonPath("$.[*].leftAt").value(hasItem(DEFAULT_LEFT_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllConferenceParticipantsWithEagerRelationshipsIsEnabled() throws Exception {
        when(conferenceParticipantServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restConferenceParticipantMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(conferenceParticipantServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllConferenceParticipantsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(conferenceParticipantServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restConferenceParticipantMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(conferenceParticipantRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getConferenceParticipant() throws Exception {
        // Initialize the database
        insertedConferenceParticipant = conferenceParticipantRepository.saveAndFlush(conferenceParticipant);

        // Get the conferenceParticipant
        restConferenceParticipantMockMvc
            .perform(get(ENTITY_API_URL_ID, conferenceParticipant.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(conferenceParticipant.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.terminalId").value(DEFAULT_TERMINAL_ID))
            .andExpect(jsonPath("$.joinedAt").value(DEFAULT_JOINED_AT.toString()))
            .andExpect(jsonPath("$.leftAt").value(DEFAULT_LEFT_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingConferenceParticipant() throws Exception {
        // Get the conferenceParticipant
        restConferenceParticipantMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return conferenceParticipantRepository.count();
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

    protected ConferenceParticipant getPersistedConferenceParticipant(ConferenceParticipant conferenceParticipant) {
        return conferenceParticipantRepository.findById(conferenceParticipant.getId()).orElseThrow();
    }

    protected void assertPersistedConferenceParticipantToMatchAllProperties(ConferenceParticipant expectedConferenceParticipant) {
        assertConferenceParticipantAllPropertiesEquals(
            expectedConferenceParticipant,
            getPersistedConferenceParticipant(expectedConferenceParticipant)
        );
    }

    protected void assertPersistedConferenceParticipantToMatchUpdatableProperties(ConferenceParticipant expectedConferenceParticipant) {
        assertConferenceParticipantAllUpdatablePropertiesEquals(
            expectedConferenceParticipant,
            getPersistedConferenceParticipant(expectedConferenceParticipant)
        );
    }
}
