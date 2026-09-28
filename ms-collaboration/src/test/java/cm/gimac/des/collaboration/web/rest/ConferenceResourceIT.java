package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.ConferenceAsserts.*;
import static cm.gimac.des.collaboration.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.domain.Room;
import cm.gimac.des.collaboration.repository.ConferenceRepository;
import cm.gimac.des.collaboration.service.ConferenceService;
import cm.gimac.des.collaboration.service.dto.ConferenceDTO;
import cm.gimac.des.collaboration.service.mapper.ConferenceMapper;
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
 * Integration tests for the {@link ConferenceResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ConferenceResourceIT {

    private static final String DEFAULT_SFU_ROOM = "AAAAAAAAAA";
    private static final String UPDATED_SFU_ROOM = "BBBBBBBBBB";

    private static final Instant DEFAULT_STARTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_ENDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ENDED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_SFRAME_REQUIRED = false;
    private static final Boolean UPDATED_SFRAME_REQUIRED = true;

    private static final Boolean DEFAULT_SCREEN_SHARE = false;
    private static final Boolean UPDATED_SCREEN_SHARE = true;

    private static final Boolean DEFAULT_RECORDING = false;
    private static final Boolean UPDATED_RECORDING = true;

    private static final Integer DEFAULT_KEY_EPOCH = 0;
    private static final Integer UPDATED_KEY_EPOCH = 1;
    private static final Integer SMALLER_KEY_EPOCH = 0 - 1;

    private static final String ENTITY_API_URL = "/api/conferences";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Mock
    private ConferenceRepository conferenceRepositoryMock;

    @Autowired
    private ConferenceMapper conferenceMapper;

    @Mock
    private ConferenceService conferenceServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restConferenceMockMvc;

    private Conference conference;

    private Conference insertedConference;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Conference createEntity(EntityManager em) {
        Conference conference = new Conference()
            .sfuRoom(DEFAULT_SFU_ROOM)
            .startedAt(DEFAULT_STARTED_AT)
            .endedAt(DEFAULT_ENDED_AT)
            .sframeRequired(DEFAULT_SFRAME_REQUIRED)
            .screenShare(DEFAULT_SCREEN_SHARE)
            .recording(DEFAULT_RECORDING)
            .keyEpoch(DEFAULT_KEY_EPOCH);
        // Add required entity
        Room room;
        if (TestUtil.findAll(em, Room.class).isEmpty()) {
            room = RoomResourceIT.createEntity();
            em.persist(room);
            em.flush();
        } else {
            room = TestUtil.findAll(em, Room.class).get(0);
        }
        conference.setRoom(room);
        return conference;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Conference createUpdatedEntity(EntityManager em) {
        Conference updatedConference = new Conference()
            .sfuRoom(UPDATED_SFU_ROOM)
            .startedAt(UPDATED_STARTED_AT)
            .endedAt(UPDATED_ENDED_AT)
            .sframeRequired(UPDATED_SFRAME_REQUIRED)
            .screenShare(UPDATED_SCREEN_SHARE)
            .recording(UPDATED_RECORDING)
            .keyEpoch(UPDATED_KEY_EPOCH);
        // Add required entity
        Room room;
        if (TestUtil.findAll(em, Room.class).isEmpty()) {
            room = RoomResourceIT.createUpdatedEntity();
            em.persist(room);
            em.flush();
        } else {
            room = TestUtil.findAll(em, Room.class).get(0);
        }
        updatedConference.setRoom(room);
        return updatedConference;
    }

    @BeforeEach
    void initTest() {
        conference = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedConference != null) {
            conferenceRepository.delete(insertedConference);
            insertedConference = null;
        }
    }

    @Test
    @Transactional
    void createConference() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Conference
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);
        var returnedConferenceDTO = om.readValue(
            restConferenceMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conferenceDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ConferenceDTO.class
        );

        // Validate the Conference in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedConference = conferenceMapper.toEntity(returnedConferenceDTO);
        assertConferenceUpdatableFieldsEquals(returnedConference, getPersistedConference(returnedConference));

        insertedConference = returnedConference;
    }

    @Test
    @Transactional
    void createConferenceWithExistingId() throws Exception {
        // Create the Conference with an existing ID
        insertedConference = conferenceRepository.saveAndFlush(conference);
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restConferenceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conferenceDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Conference in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkSfuRoomIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        conference.setSfuRoom(null);

        // Create the Conference, which fails.
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        restConferenceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conferenceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStartedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        conference.setStartedAt(null);

        // Create the Conference, which fails.
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        restConferenceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conferenceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSframeRequiredIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        conference.setSframeRequired(null);

        // Create the Conference, which fails.
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        restConferenceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conferenceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkScreenShareIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        conference.setScreenShare(null);

        // Create the Conference, which fails.
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        restConferenceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conferenceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRecordingIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        conference.setRecording(null);

        // Create the Conference, which fails.
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        restConferenceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conferenceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkKeyEpochIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        conference.setKeyEpoch(null);

        // Create the Conference, which fails.
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        restConferenceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conferenceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllConferences() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList
        restConferenceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(conference.getId().toString())))
            .andExpect(jsonPath("$.[*].sfuRoom").value(hasItem(DEFAULT_SFU_ROOM)))
            .andExpect(jsonPath("$.[*].startedAt").value(hasItem(DEFAULT_STARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].endedAt").value(hasItem(DEFAULT_ENDED_AT.toString())))
            .andExpect(jsonPath("$.[*].sframeRequired").value(hasItem(DEFAULT_SFRAME_REQUIRED)))
            .andExpect(jsonPath("$.[*].screenShare").value(hasItem(DEFAULT_SCREEN_SHARE)))
            .andExpect(jsonPath("$.[*].recording").value(hasItem(DEFAULT_RECORDING)))
            .andExpect(jsonPath("$.[*].keyEpoch").value(hasItem(DEFAULT_KEY_EPOCH)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllConferencesWithEagerRelationshipsIsEnabled() throws Exception {
        when(conferenceServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restConferenceMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(conferenceServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllConferencesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(conferenceServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restConferenceMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(conferenceRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getConference() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get the conference
        restConferenceMockMvc
            .perform(get(ENTITY_API_URL_ID, conference.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(conference.getId().toString()))
            .andExpect(jsonPath("$.sfuRoom").value(DEFAULT_SFU_ROOM))
            .andExpect(jsonPath("$.startedAt").value(DEFAULT_STARTED_AT.toString()))
            .andExpect(jsonPath("$.endedAt").value(DEFAULT_ENDED_AT.toString()))
            .andExpect(jsonPath("$.sframeRequired").value(DEFAULT_SFRAME_REQUIRED))
            .andExpect(jsonPath("$.screenShare").value(DEFAULT_SCREEN_SHARE))
            .andExpect(jsonPath("$.recording").value(DEFAULT_RECORDING))
            .andExpect(jsonPath("$.keyEpoch").value(DEFAULT_KEY_EPOCH));
    }

    @Test
    @Transactional
    void getConferencesByIdFiltering() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        UUID id = conference.getId();

        defaultConferenceFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllConferencesBySfuRoomIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where sfuRoom equals to
        defaultConferenceFiltering("sfuRoom.equals=" + DEFAULT_SFU_ROOM, "sfuRoom.equals=" + UPDATED_SFU_ROOM);
    }

    @Test
    @Transactional
    void getAllConferencesBySfuRoomIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where sfuRoom in
        defaultConferenceFiltering("sfuRoom.in=" + DEFAULT_SFU_ROOM + "," + UPDATED_SFU_ROOM, "sfuRoom.in=" + UPDATED_SFU_ROOM);
    }

    @Test
    @Transactional
    void getAllConferencesBySfuRoomIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where sfuRoom is not null
        defaultConferenceFiltering("sfuRoom.specified=true", "sfuRoom.specified=false");
    }

    @Test
    @Transactional
    void getAllConferencesBySfuRoomContainsSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where sfuRoom contains
        defaultConferenceFiltering("sfuRoom.contains=" + DEFAULT_SFU_ROOM, "sfuRoom.contains=" + UPDATED_SFU_ROOM);
    }

    @Test
    @Transactional
    void getAllConferencesBySfuRoomNotContainsSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where sfuRoom does not contain
        defaultConferenceFiltering("sfuRoom.doesNotContain=" + UPDATED_SFU_ROOM, "sfuRoom.doesNotContain=" + DEFAULT_SFU_ROOM);
    }

    @Test
    @Transactional
    void getAllConferencesByStartedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where startedAt equals to
        defaultConferenceFiltering("startedAt.equals=" + DEFAULT_STARTED_AT, "startedAt.equals=" + UPDATED_STARTED_AT);
    }

    @Test
    @Transactional
    void getAllConferencesByStartedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where startedAt in
        defaultConferenceFiltering("startedAt.in=" + DEFAULT_STARTED_AT + "," + UPDATED_STARTED_AT, "startedAt.in=" + UPDATED_STARTED_AT);
    }

    @Test
    @Transactional
    void getAllConferencesByStartedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where startedAt is not null
        defaultConferenceFiltering("startedAt.specified=true", "startedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllConferencesByEndedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where endedAt equals to
        defaultConferenceFiltering("endedAt.equals=" + DEFAULT_ENDED_AT, "endedAt.equals=" + UPDATED_ENDED_AT);
    }

    @Test
    @Transactional
    void getAllConferencesByEndedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where endedAt in
        defaultConferenceFiltering("endedAt.in=" + DEFAULT_ENDED_AT + "," + UPDATED_ENDED_AT, "endedAt.in=" + UPDATED_ENDED_AT);
    }

    @Test
    @Transactional
    void getAllConferencesByEndedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where endedAt is not null
        defaultConferenceFiltering("endedAt.specified=true", "endedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllConferencesBySframeRequiredIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where sframeRequired equals to
        defaultConferenceFiltering("sframeRequired.equals=" + DEFAULT_SFRAME_REQUIRED, "sframeRequired.equals=" + UPDATED_SFRAME_REQUIRED);
    }

    @Test
    @Transactional
    void getAllConferencesBySframeRequiredIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where sframeRequired in
        defaultConferenceFiltering(
            "sframeRequired.in=" + DEFAULT_SFRAME_REQUIRED + "," + UPDATED_SFRAME_REQUIRED,
            "sframeRequired.in=" + UPDATED_SFRAME_REQUIRED
        );
    }

    @Test
    @Transactional
    void getAllConferencesBySframeRequiredIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where sframeRequired is not null
        defaultConferenceFiltering("sframeRequired.specified=true", "sframeRequired.specified=false");
    }

    @Test
    @Transactional
    void getAllConferencesByScreenShareIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where screenShare equals to
        defaultConferenceFiltering("screenShare.equals=" + DEFAULT_SCREEN_SHARE, "screenShare.equals=" + UPDATED_SCREEN_SHARE);
    }

    @Test
    @Transactional
    void getAllConferencesByScreenShareIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where screenShare in
        defaultConferenceFiltering(
            "screenShare.in=" + DEFAULT_SCREEN_SHARE + "," + UPDATED_SCREEN_SHARE,
            "screenShare.in=" + UPDATED_SCREEN_SHARE
        );
    }

    @Test
    @Transactional
    void getAllConferencesByScreenShareIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where screenShare is not null
        defaultConferenceFiltering("screenShare.specified=true", "screenShare.specified=false");
    }

    @Test
    @Transactional
    void getAllConferencesByRecordingIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where recording equals to
        defaultConferenceFiltering("recording.equals=" + DEFAULT_RECORDING, "recording.equals=" + UPDATED_RECORDING);
    }

    @Test
    @Transactional
    void getAllConferencesByRecordingIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where recording in
        defaultConferenceFiltering("recording.in=" + DEFAULT_RECORDING + "," + UPDATED_RECORDING, "recording.in=" + UPDATED_RECORDING);
    }

    @Test
    @Transactional
    void getAllConferencesByRecordingIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where recording is not null
        defaultConferenceFiltering("recording.specified=true", "recording.specified=false");
    }

    @Test
    @Transactional
    void getAllConferencesByKeyEpochIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where keyEpoch equals to
        defaultConferenceFiltering("keyEpoch.equals=" + DEFAULT_KEY_EPOCH, "keyEpoch.equals=" + UPDATED_KEY_EPOCH);
    }

    @Test
    @Transactional
    void getAllConferencesByKeyEpochIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where keyEpoch in
        defaultConferenceFiltering("keyEpoch.in=" + DEFAULT_KEY_EPOCH + "," + UPDATED_KEY_EPOCH, "keyEpoch.in=" + UPDATED_KEY_EPOCH);
    }

    @Test
    @Transactional
    void getAllConferencesByKeyEpochIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where keyEpoch is not null
        defaultConferenceFiltering("keyEpoch.specified=true", "keyEpoch.specified=false");
    }

    @Test
    @Transactional
    void getAllConferencesByKeyEpochIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where keyEpoch is greater than or equal to
        defaultConferenceFiltering("keyEpoch.greaterThanOrEqual=" + DEFAULT_KEY_EPOCH, "keyEpoch.greaterThanOrEqual=" + UPDATED_KEY_EPOCH);
    }

    @Test
    @Transactional
    void getAllConferencesByKeyEpochIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where keyEpoch is less than or equal to
        defaultConferenceFiltering("keyEpoch.lessThanOrEqual=" + DEFAULT_KEY_EPOCH, "keyEpoch.lessThanOrEqual=" + SMALLER_KEY_EPOCH);
    }

    @Test
    @Transactional
    void getAllConferencesByKeyEpochIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where keyEpoch is less than
        defaultConferenceFiltering("keyEpoch.lessThan=" + UPDATED_KEY_EPOCH, "keyEpoch.lessThan=" + DEFAULT_KEY_EPOCH);
    }

    @Test
    @Transactional
    void getAllConferencesByKeyEpochIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList where keyEpoch is greater than
        defaultConferenceFiltering("keyEpoch.greaterThan=" + SMALLER_KEY_EPOCH, "keyEpoch.greaterThan=" + DEFAULT_KEY_EPOCH);
    }

    @Test
    @Transactional
    void getAllConferencesByRoomIsEqualToSomething() throws Exception {
        Room room;
        if (TestUtil.findAll(em, Room.class).isEmpty()) {
            conferenceRepository.saveAndFlush(conference);
            room = RoomResourceIT.createEntity();
        } else {
            room = TestUtil.findAll(em, Room.class).get(0);
        }
        em.persist(room);
        em.flush();
        conference.setRoom(room);
        conferenceRepository.saveAndFlush(conference);
        UUID roomId = room.getId();
        // Get all the conferenceList where room equals to roomId
        defaultConferenceShouldBeFound("roomId.equals=" + roomId);

        // Get all the conferenceList where room equals to UUID.randomUUID()
        defaultConferenceShouldNotBeFound("roomId.equals=" + UUID.randomUUID());
    }

    private void defaultConferenceFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultConferenceShouldBeFound(shouldBeFound);
        defaultConferenceShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultConferenceShouldBeFound(String filter) throws Exception {
        restConferenceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(conference.getId().toString())))
            .andExpect(jsonPath("$.[*].sfuRoom").value(hasItem(DEFAULT_SFU_ROOM)))
            .andExpect(jsonPath("$.[*].startedAt").value(hasItem(DEFAULT_STARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].endedAt").value(hasItem(DEFAULT_ENDED_AT.toString())))
            .andExpect(jsonPath("$.[*].sframeRequired").value(hasItem(DEFAULT_SFRAME_REQUIRED)))
            .andExpect(jsonPath("$.[*].screenShare").value(hasItem(DEFAULT_SCREEN_SHARE)))
            .andExpect(jsonPath("$.[*].recording").value(hasItem(DEFAULT_RECORDING)))
            .andExpect(jsonPath("$.[*].keyEpoch").value(hasItem(DEFAULT_KEY_EPOCH)));

        // Check, that the count call also returns 1
        restConferenceMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultConferenceShouldNotBeFound(String filter) throws Exception {
        restConferenceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restConferenceMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingConference() throws Exception {
        // Get the conference
        restConferenceMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingConference() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the conference
        Conference updatedConference = conferenceRepository.findById(conference.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedConference are not directly saved in db
        em.detach(updatedConference);
        updatedConference
            .sfuRoom(UPDATED_SFU_ROOM)
            .startedAt(UPDATED_STARTED_AT)
            .endedAt(UPDATED_ENDED_AT)
            .sframeRequired(UPDATED_SFRAME_REQUIRED)
            .screenShare(UPDATED_SCREEN_SHARE)
            .recording(UPDATED_RECORDING)
            .keyEpoch(UPDATED_KEY_EPOCH);
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(updatedConference);

        restConferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, conferenceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(conferenceDTO))
            )
            .andExpect(status().isOk());

        // Validate the Conference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedConferenceToMatchAllProperties(updatedConference);
    }

    @Test
    @Transactional
    void putNonExistingConference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conference.setId(UUID.randomUUID());

        // Create the Conference
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, conferenceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(conferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchConference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conference.setId(UUID.randomUUID());

        // Create the Conference
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(conferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamConference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conference.setId(UUID.randomUUID());

        // Create the Conference
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conferenceDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Conference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateConferenceWithPatch() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the conference using partial update
        Conference partialUpdatedConference = new Conference();
        partialUpdatedConference.setId(conference.getId());

        partialUpdatedConference
            .sfuRoom(UPDATED_SFU_ROOM)
            .endedAt(UPDATED_ENDED_AT)
            .sframeRequired(UPDATED_SFRAME_REQUIRED)
            .screenShare(UPDATED_SCREEN_SHARE)
            .recording(UPDATED_RECORDING)
            .keyEpoch(UPDATED_KEY_EPOCH);

        restConferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedConference.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedConference))
            )
            .andExpect(status().isOk());

        // Validate the Conference in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertConferenceUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedConference, conference),
            getPersistedConference(conference)
        );
    }

    @Test
    @Transactional
    void fullUpdateConferenceWithPatch() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the conference using partial update
        Conference partialUpdatedConference = new Conference();
        partialUpdatedConference.setId(conference.getId());

        partialUpdatedConference
            .sfuRoom(UPDATED_SFU_ROOM)
            .startedAt(UPDATED_STARTED_AT)
            .endedAt(UPDATED_ENDED_AT)
            .sframeRequired(UPDATED_SFRAME_REQUIRED)
            .screenShare(UPDATED_SCREEN_SHARE)
            .recording(UPDATED_RECORDING)
            .keyEpoch(UPDATED_KEY_EPOCH);

        restConferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedConference.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedConference))
            )
            .andExpect(status().isOk());

        // Validate the Conference in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertConferenceUpdatableFieldsEquals(partialUpdatedConference, getPersistedConference(partialUpdatedConference));
    }

    @Test
    @Transactional
    void patchNonExistingConference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conference.setId(UUID.randomUUID());

        // Create the Conference
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, conferenceDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(conferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchConference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conference.setId(UUID.randomUUID());

        // Create the Conference
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(conferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamConference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conference.setId(UUID.randomUUID());

        // Create the Conference
        ConferenceDTO conferenceDTO = conferenceMapper.toDto(conference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(conferenceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Conference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteConference() throws Exception {
        // Initialize the database
        insertedConference = conferenceRepository.saveAndFlush(conference);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the conference
        restConferenceMockMvc
            .perform(delete(ENTITY_API_URL_ID, conference.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return conferenceRepository.count();
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

    protected Conference getPersistedConference(Conference conference) {
        return conferenceRepository.findById(conference.getId()).orElseThrow();
    }

    protected void assertPersistedConferenceToMatchAllProperties(Conference expectedConference) {
        assertConferenceAllPropertiesEquals(expectedConference, getPersistedConference(expectedConference));
    }

    protected void assertPersistedConferenceToMatchUpdatableProperties(Conference expectedConference) {
        assertConferenceAllUpdatablePropertiesEquals(expectedConference, getPersistedConference(expectedConference));
    }
}
