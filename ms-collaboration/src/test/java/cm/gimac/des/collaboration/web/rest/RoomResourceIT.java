package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.RoomAsserts.*;
import static cm.gimac.des.collaboration.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.Room;
import cm.gimac.des.collaboration.domain.enumeration.AttachmentKind;
import cm.gimac.des.collaboration.domain.enumeration.RoomState;
import cm.gimac.des.collaboration.domain.enumeration.RoomType;
import cm.gimac.des.collaboration.repository.RoomRepository;
import cm.gimac.des.collaboration.service.dto.RoomDTO;
import cm.gimac.des.collaboration.service.mapper.RoomMapper;
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
 * Integration tests for the {@link RoomResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class RoomResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final RoomType DEFAULT_TYPE = RoomType.QUART;
    private static final RoomType UPDATED_TYPE = RoomType.THEME;

    private static final AttachmentKind DEFAULT_ATTACHMENT_KIND = AttachmentKind.VACATION;
    private static final AttachmentKind UPDATED_ATTACHMENT_KIND = AttachmentKind.THEME;

    private static final String DEFAULT_ATTACHMENT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_ATTACHMENT_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_ATTACHMENT_SERVICE = "AAAAAAAAAA";
    private static final String UPDATED_ATTACHMENT_SERVICE = "BBBBBBBBBB";

    private static final String DEFAULT_TICKET_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_TICKET_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_MATRIX_ROOM_ID = "AAAAAAAAAA";
    private static final String UPDATED_MATRIX_ROOM_ID = "BBBBBBBBBB";

    private static final String DEFAULT_ENCRYPTION = "AAAAAAAAAA";
    private static final String UPDATED_ENCRYPTION = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final RoomState DEFAULT_STATE = RoomState.ACTIF;
    private static final RoomState UPDATED_STATE = RoomState.GELE;

    private static final Duration DEFAULT_RETENTION = Duration.ofHours(6);
    private static final Duration UPDATED_RETENTION = Duration.ofHours(12);
    private static final Duration SMALLER_RETENTION = Duration.ofHours(5);

    private static final Integer DEFAULT_GROUP_SESSION_EPOCH = 0;
    private static final Integer UPDATED_GROUP_SESSION_EPOCH = 1;
    private static final Integer SMALLER_GROUP_SESSION_EPOCH = 0 - 1;

    private static final Instant DEFAULT_FROZEN_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FROZEN_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_ARCHIVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ARCHIVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_PURGE_AFTER = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PURGE_AFTER = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/rooms";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRoomMockMvc;

    private Room room;

    private Room insertedRoom;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Room createEntity() {
        return new Room()
            .code(DEFAULT_CODE)
            .type(DEFAULT_TYPE)
            .attachmentKind(DEFAULT_ATTACHMENT_KIND)
            .attachmentReference(DEFAULT_ATTACHMENT_REFERENCE)
            .attachmentService(DEFAULT_ATTACHMENT_SERVICE)
            .ticketReference(DEFAULT_TICKET_REFERENCE)
            .matrixRoomId(DEFAULT_MATRIX_ROOM_ID)
            .encryption(DEFAULT_ENCRYPTION)
            .createdAt(DEFAULT_CREATED_AT)
            .state(DEFAULT_STATE)
            .retention(DEFAULT_RETENTION)
            .groupSessionEpoch(DEFAULT_GROUP_SESSION_EPOCH)
            .frozenAt(DEFAULT_FROZEN_AT)
            .archivedAt(DEFAULT_ARCHIVED_AT)
            .purgeAfter(DEFAULT_PURGE_AFTER);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Room createUpdatedEntity() {
        return new Room()
            .code(UPDATED_CODE)
            .type(UPDATED_TYPE)
            .attachmentKind(UPDATED_ATTACHMENT_KIND)
            .attachmentReference(UPDATED_ATTACHMENT_REFERENCE)
            .attachmentService(UPDATED_ATTACHMENT_SERVICE)
            .ticketReference(UPDATED_TICKET_REFERENCE)
            .matrixRoomId(UPDATED_MATRIX_ROOM_ID)
            .encryption(UPDATED_ENCRYPTION)
            .createdAt(UPDATED_CREATED_AT)
            .state(UPDATED_STATE)
            .retention(UPDATED_RETENTION)
            .groupSessionEpoch(UPDATED_GROUP_SESSION_EPOCH)
            .frozenAt(UPDATED_FROZEN_AT)
            .archivedAt(UPDATED_ARCHIVED_AT)
            .purgeAfter(UPDATED_PURGE_AFTER);
    }

    @BeforeEach
    void initTest() {
        room = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRoom != null) {
            roomRepository.delete(insertedRoom);
            insertedRoom = null;
        }
    }

    @Test
    @Transactional
    void createRoom() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Room
        RoomDTO roomDTO = roomMapper.toDto(room);
        var returnedRoomDTO = om.readValue(
            restRoomMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RoomDTO.class
        );

        // Validate the Room in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRoom = roomMapper.toEntity(returnedRoomDTO);
        assertRoomUpdatableFieldsEquals(returnedRoom, getPersistedRoom(returnedRoom));

        insertedRoom = returnedRoom;
    }

    @Test
    @Transactional
    void createRoomWithExistingId() throws Exception {
        // Create the Room with an existing ID
        insertedRoom = roomRepository.saveAndFlush(room);
        RoomDTO roomDTO = roomMapper.toDto(room);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Room in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        room.setCode(null);

        // Create the Room, which fails.
        RoomDTO roomDTO = roomMapper.toDto(room);

        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        room.setType(null);

        // Create the Room, which fails.
        RoomDTO roomDTO = roomMapper.toDto(room);

        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAttachmentKindIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        room.setAttachmentKind(null);

        // Create the Room, which fails.
        RoomDTO roomDTO = roomMapper.toDto(room);

        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAttachmentReferenceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        room.setAttachmentReference(null);

        // Create the Room, which fails.
        RoomDTO roomDTO = roomMapper.toDto(room);

        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAttachmentServiceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        room.setAttachmentService(null);

        // Create the Room, which fails.
        RoomDTO roomDTO = roomMapper.toDto(room);

        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMatrixRoomIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        room.setMatrixRoomId(null);

        // Create the Room, which fails.
        RoomDTO roomDTO = roomMapper.toDto(room);

        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEncryptionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        room.setEncryption(null);

        // Create the Room, which fails.
        RoomDTO roomDTO = roomMapper.toDto(room);

        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        room.setCreatedAt(null);

        // Create the Room, which fails.
        RoomDTO roomDTO = roomMapper.toDto(room);

        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        room.setState(null);

        // Create the Room, which fails.
        RoomDTO roomDTO = roomMapper.toDto(room);

        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRetentionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        room.setRetention(null);

        // Create the Room, which fails.
        RoomDTO roomDTO = roomMapper.toDto(room);

        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkGroupSessionEpochIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        room.setGroupSessionEpoch(null);

        // Create the Room, which fails.
        RoomDTO roomDTO = roomMapper.toDto(room);

        restRoomMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRooms() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList
        restRoomMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(room.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].attachmentKind").value(hasItem(DEFAULT_ATTACHMENT_KIND.toString())))
            .andExpect(jsonPath("$.[*].attachmentReference").value(hasItem(DEFAULT_ATTACHMENT_REFERENCE)))
            .andExpect(jsonPath("$.[*].attachmentService").value(hasItem(DEFAULT_ATTACHMENT_SERVICE)))
            .andExpect(jsonPath("$.[*].ticketReference").value(hasItem(DEFAULT_TICKET_REFERENCE)))
            .andExpect(jsonPath("$.[*].matrixRoomId").value(hasItem(DEFAULT_MATRIX_ROOM_ID)))
            .andExpect(jsonPath("$.[*].encryption").value(hasItem(DEFAULT_ENCRYPTION)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].retention").value(hasItem(DEFAULT_RETENTION.toString())))
            .andExpect(jsonPath("$.[*].groupSessionEpoch").value(hasItem(DEFAULT_GROUP_SESSION_EPOCH)))
            .andExpect(jsonPath("$.[*].frozenAt").value(hasItem(DEFAULT_FROZEN_AT.toString())))
            .andExpect(jsonPath("$.[*].archivedAt").value(hasItem(DEFAULT_ARCHIVED_AT.toString())))
            .andExpect(jsonPath("$.[*].purgeAfter").value(hasItem(DEFAULT_PURGE_AFTER.toString())));
    }

    @Test
    @Transactional
    void getRoom() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get the room
        restRoomMockMvc
            .perform(get(ENTITY_API_URL_ID, room.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(room.getId().toString()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.attachmentKind").value(DEFAULT_ATTACHMENT_KIND.toString()))
            .andExpect(jsonPath("$.attachmentReference").value(DEFAULT_ATTACHMENT_REFERENCE))
            .andExpect(jsonPath("$.attachmentService").value(DEFAULT_ATTACHMENT_SERVICE))
            .andExpect(jsonPath("$.ticketReference").value(DEFAULT_TICKET_REFERENCE))
            .andExpect(jsonPath("$.matrixRoomId").value(DEFAULT_MATRIX_ROOM_ID))
            .andExpect(jsonPath("$.encryption").value(DEFAULT_ENCRYPTION))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.retention").value(DEFAULT_RETENTION.toString()))
            .andExpect(jsonPath("$.groupSessionEpoch").value(DEFAULT_GROUP_SESSION_EPOCH))
            .andExpect(jsonPath("$.frozenAt").value(DEFAULT_FROZEN_AT.toString()))
            .andExpect(jsonPath("$.archivedAt").value(DEFAULT_ARCHIVED_AT.toString()))
            .andExpect(jsonPath("$.purgeAfter").value(DEFAULT_PURGE_AFTER.toString()));
    }

    @Test
    @Transactional
    void getRoomsByIdFiltering() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        UUID id = room.getId();

        defaultRoomFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllRoomsByCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where code equals to
        defaultRoomFiltering("code.equals=" + DEFAULT_CODE, "code.equals=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllRoomsByCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where code in
        defaultRoomFiltering("code.in=" + DEFAULT_CODE + "," + UPDATED_CODE, "code.in=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllRoomsByCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where code is not null
        defaultRoomFiltering("code.specified=true", "code.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where code contains
        defaultRoomFiltering("code.contains=" + DEFAULT_CODE, "code.contains=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllRoomsByCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where code does not contain
        defaultRoomFiltering("code.doesNotContain=" + UPDATED_CODE, "code.doesNotContain=" + DEFAULT_CODE);
    }

    @Test
    @Transactional
    void getAllRoomsByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where type equals to
        defaultRoomFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllRoomsByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where type in
        defaultRoomFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllRoomsByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where type is not null
        defaultRoomFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentKindIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentKind equals to
        defaultRoomFiltering("attachmentKind.equals=" + DEFAULT_ATTACHMENT_KIND, "attachmentKind.equals=" + UPDATED_ATTACHMENT_KIND);
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentKindIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentKind in
        defaultRoomFiltering(
            "attachmentKind.in=" + DEFAULT_ATTACHMENT_KIND + "," + UPDATED_ATTACHMENT_KIND,
            "attachmentKind.in=" + UPDATED_ATTACHMENT_KIND
        );
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentKindIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentKind is not null
        defaultRoomFiltering("attachmentKind.specified=true", "attachmentKind.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentReference equals to
        defaultRoomFiltering(
            "attachmentReference.equals=" + DEFAULT_ATTACHMENT_REFERENCE,
            "attachmentReference.equals=" + UPDATED_ATTACHMENT_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentReference in
        defaultRoomFiltering(
            "attachmentReference.in=" + DEFAULT_ATTACHMENT_REFERENCE + "," + UPDATED_ATTACHMENT_REFERENCE,
            "attachmentReference.in=" + UPDATED_ATTACHMENT_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentReference is not null
        defaultRoomFiltering("attachmentReference.specified=true", "attachmentReference.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentReference contains
        defaultRoomFiltering(
            "attachmentReference.contains=" + DEFAULT_ATTACHMENT_REFERENCE,
            "attachmentReference.contains=" + UPDATED_ATTACHMENT_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentReference does not contain
        defaultRoomFiltering(
            "attachmentReference.doesNotContain=" + UPDATED_ATTACHMENT_REFERENCE,
            "attachmentReference.doesNotContain=" + DEFAULT_ATTACHMENT_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentServiceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentService equals to
        defaultRoomFiltering(
            "attachmentService.equals=" + DEFAULT_ATTACHMENT_SERVICE,
            "attachmentService.equals=" + UPDATED_ATTACHMENT_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentServiceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentService in
        defaultRoomFiltering(
            "attachmentService.in=" + DEFAULT_ATTACHMENT_SERVICE + "," + UPDATED_ATTACHMENT_SERVICE,
            "attachmentService.in=" + UPDATED_ATTACHMENT_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentServiceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentService is not null
        defaultRoomFiltering("attachmentService.specified=true", "attachmentService.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentServiceContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentService contains
        defaultRoomFiltering(
            "attachmentService.contains=" + DEFAULT_ATTACHMENT_SERVICE,
            "attachmentService.contains=" + UPDATED_ATTACHMENT_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllRoomsByAttachmentServiceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where attachmentService does not contain
        defaultRoomFiltering(
            "attachmentService.doesNotContain=" + UPDATED_ATTACHMENT_SERVICE,
            "attachmentService.doesNotContain=" + DEFAULT_ATTACHMENT_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllRoomsByTicketReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where ticketReference equals to
        defaultRoomFiltering("ticketReference.equals=" + DEFAULT_TICKET_REFERENCE, "ticketReference.equals=" + UPDATED_TICKET_REFERENCE);
    }

    @Test
    @Transactional
    void getAllRoomsByTicketReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where ticketReference in
        defaultRoomFiltering(
            "ticketReference.in=" + DEFAULT_TICKET_REFERENCE + "," + UPDATED_TICKET_REFERENCE,
            "ticketReference.in=" + UPDATED_TICKET_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomsByTicketReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where ticketReference is not null
        defaultRoomFiltering("ticketReference.specified=true", "ticketReference.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByTicketReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where ticketReference contains
        defaultRoomFiltering(
            "ticketReference.contains=" + DEFAULT_TICKET_REFERENCE,
            "ticketReference.contains=" + UPDATED_TICKET_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomsByTicketReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where ticketReference does not contain
        defaultRoomFiltering(
            "ticketReference.doesNotContain=" + UPDATED_TICKET_REFERENCE,
            "ticketReference.doesNotContain=" + DEFAULT_TICKET_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomsByMatrixRoomIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where matrixRoomId equals to
        defaultRoomFiltering("matrixRoomId.equals=" + DEFAULT_MATRIX_ROOM_ID, "matrixRoomId.equals=" + UPDATED_MATRIX_ROOM_ID);
    }

    @Test
    @Transactional
    void getAllRoomsByMatrixRoomIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where matrixRoomId in
        defaultRoomFiltering(
            "matrixRoomId.in=" + DEFAULT_MATRIX_ROOM_ID + "," + UPDATED_MATRIX_ROOM_ID,
            "matrixRoomId.in=" + UPDATED_MATRIX_ROOM_ID
        );
    }

    @Test
    @Transactional
    void getAllRoomsByMatrixRoomIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where matrixRoomId is not null
        defaultRoomFiltering("matrixRoomId.specified=true", "matrixRoomId.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByMatrixRoomIdContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where matrixRoomId contains
        defaultRoomFiltering("matrixRoomId.contains=" + DEFAULT_MATRIX_ROOM_ID, "matrixRoomId.contains=" + UPDATED_MATRIX_ROOM_ID);
    }

    @Test
    @Transactional
    void getAllRoomsByMatrixRoomIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where matrixRoomId does not contain
        defaultRoomFiltering(
            "matrixRoomId.doesNotContain=" + UPDATED_MATRIX_ROOM_ID,
            "matrixRoomId.doesNotContain=" + DEFAULT_MATRIX_ROOM_ID
        );
    }

    @Test
    @Transactional
    void getAllRoomsByEncryptionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where encryption equals to
        defaultRoomFiltering("encryption.equals=" + DEFAULT_ENCRYPTION, "encryption.equals=" + UPDATED_ENCRYPTION);
    }

    @Test
    @Transactional
    void getAllRoomsByEncryptionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where encryption in
        defaultRoomFiltering("encryption.in=" + DEFAULT_ENCRYPTION + "," + UPDATED_ENCRYPTION, "encryption.in=" + UPDATED_ENCRYPTION);
    }

    @Test
    @Transactional
    void getAllRoomsByEncryptionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where encryption is not null
        defaultRoomFiltering("encryption.specified=true", "encryption.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByEncryptionContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where encryption contains
        defaultRoomFiltering("encryption.contains=" + DEFAULT_ENCRYPTION, "encryption.contains=" + UPDATED_ENCRYPTION);
    }

    @Test
    @Transactional
    void getAllRoomsByEncryptionNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where encryption does not contain
        defaultRoomFiltering("encryption.doesNotContain=" + UPDATED_ENCRYPTION, "encryption.doesNotContain=" + DEFAULT_ENCRYPTION);
    }

    @Test
    @Transactional
    void getAllRoomsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where createdAt equals to
        defaultRoomFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllRoomsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where createdAt in
        defaultRoomFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllRoomsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where createdAt is not null
        defaultRoomFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where state equals to
        defaultRoomFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllRoomsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where state in
        defaultRoomFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllRoomsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where state is not null
        defaultRoomFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByRetentionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where retention equals to
        defaultRoomFiltering("retention.equals=" + DEFAULT_RETENTION, "retention.equals=" + UPDATED_RETENTION);
    }

    @Test
    @Transactional
    void getAllRoomsByRetentionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where retention in
        defaultRoomFiltering("retention.in=" + DEFAULT_RETENTION + "," + UPDATED_RETENTION, "retention.in=" + UPDATED_RETENTION);
    }

    @Test
    @Transactional
    void getAllRoomsByRetentionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where retention is not null
        defaultRoomFiltering("retention.specified=true", "retention.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByRetentionIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where retention is greater than or equal to
        defaultRoomFiltering("retention.greaterThanOrEqual=" + DEFAULT_RETENTION, "retention.greaterThanOrEqual=" + UPDATED_RETENTION);
    }

    @Test
    @Transactional
    void getAllRoomsByRetentionIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where retention is less than or equal to
        defaultRoomFiltering("retention.lessThanOrEqual=" + DEFAULT_RETENTION, "retention.lessThanOrEqual=" + SMALLER_RETENTION);
    }

    @Test
    @Transactional
    void getAllRoomsByRetentionIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where retention is less than
        defaultRoomFiltering("retention.lessThan=" + UPDATED_RETENTION, "retention.lessThan=" + DEFAULT_RETENTION);
    }

    @Test
    @Transactional
    void getAllRoomsByRetentionIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where retention is greater than
        defaultRoomFiltering("retention.greaterThan=" + SMALLER_RETENTION, "retention.greaterThan=" + DEFAULT_RETENTION);
    }

    @Test
    @Transactional
    void getAllRoomsByGroupSessionEpochIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where groupSessionEpoch equals to
        defaultRoomFiltering(
            "groupSessionEpoch.equals=" + DEFAULT_GROUP_SESSION_EPOCH,
            "groupSessionEpoch.equals=" + UPDATED_GROUP_SESSION_EPOCH
        );
    }

    @Test
    @Transactional
    void getAllRoomsByGroupSessionEpochIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where groupSessionEpoch in
        defaultRoomFiltering(
            "groupSessionEpoch.in=" + DEFAULT_GROUP_SESSION_EPOCH + "," + UPDATED_GROUP_SESSION_EPOCH,
            "groupSessionEpoch.in=" + UPDATED_GROUP_SESSION_EPOCH
        );
    }

    @Test
    @Transactional
    void getAllRoomsByGroupSessionEpochIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where groupSessionEpoch is not null
        defaultRoomFiltering("groupSessionEpoch.specified=true", "groupSessionEpoch.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByGroupSessionEpochIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where groupSessionEpoch is greater than or equal to
        defaultRoomFiltering(
            "groupSessionEpoch.greaterThanOrEqual=" + DEFAULT_GROUP_SESSION_EPOCH,
            "groupSessionEpoch.greaterThanOrEqual=" + UPDATED_GROUP_SESSION_EPOCH
        );
    }

    @Test
    @Transactional
    void getAllRoomsByGroupSessionEpochIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where groupSessionEpoch is less than or equal to
        defaultRoomFiltering(
            "groupSessionEpoch.lessThanOrEqual=" + DEFAULT_GROUP_SESSION_EPOCH,
            "groupSessionEpoch.lessThanOrEqual=" + SMALLER_GROUP_SESSION_EPOCH
        );
    }

    @Test
    @Transactional
    void getAllRoomsByGroupSessionEpochIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where groupSessionEpoch is less than
        defaultRoomFiltering(
            "groupSessionEpoch.lessThan=" + UPDATED_GROUP_SESSION_EPOCH,
            "groupSessionEpoch.lessThan=" + DEFAULT_GROUP_SESSION_EPOCH
        );
    }

    @Test
    @Transactional
    void getAllRoomsByGroupSessionEpochIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where groupSessionEpoch is greater than
        defaultRoomFiltering(
            "groupSessionEpoch.greaterThan=" + SMALLER_GROUP_SESSION_EPOCH,
            "groupSessionEpoch.greaterThan=" + DEFAULT_GROUP_SESSION_EPOCH
        );
    }

    @Test
    @Transactional
    void getAllRoomsByFrozenAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where frozenAt equals to
        defaultRoomFiltering("frozenAt.equals=" + DEFAULT_FROZEN_AT, "frozenAt.equals=" + UPDATED_FROZEN_AT);
    }

    @Test
    @Transactional
    void getAllRoomsByFrozenAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where frozenAt in
        defaultRoomFiltering("frozenAt.in=" + DEFAULT_FROZEN_AT + "," + UPDATED_FROZEN_AT, "frozenAt.in=" + UPDATED_FROZEN_AT);
    }

    @Test
    @Transactional
    void getAllRoomsByFrozenAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where frozenAt is not null
        defaultRoomFiltering("frozenAt.specified=true", "frozenAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByArchivedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where archivedAt equals to
        defaultRoomFiltering("archivedAt.equals=" + DEFAULT_ARCHIVED_AT, "archivedAt.equals=" + UPDATED_ARCHIVED_AT);
    }

    @Test
    @Transactional
    void getAllRoomsByArchivedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where archivedAt in
        defaultRoomFiltering("archivedAt.in=" + DEFAULT_ARCHIVED_AT + "," + UPDATED_ARCHIVED_AT, "archivedAt.in=" + UPDATED_ARCHIVED_AT);
    }

    @Test
    @Transactional
    void getAllRoomsByArchivedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where archivedAt is not null
        defaultRoomFiltering("archivedAt.specified=true", "archivedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomsByPurgeAfterIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where purgeAfter equals to
        defaultRoomFiltering("purgeAfter.equals=" + DEFAULT_PURGE_AFTER, "purgeAfter.equals=" + UPDATED_PURGE_AFTER);
    }

    @Test
    @Transactional
    void getAllRoomsByPurgeAfterIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where purgeAfter in
        defaultRoomFiltering("purgeAfter.in=" + DEFAULT_PURGE_AFTER + "," + UPDATED_PURGE_AFTER, "purgeAfter.in=" + UPDATED_PURGE_AFTER);
    }

    @Test
    @Transactional
    void getAllRoomsByPurgeAfterIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        // Get all the roomList where purgeAfter is not null
        defaultRoomFiltering("purgeAfter.specified=true", "purgeAfter.specified=false");
    }

    private void defaultRoomFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultRoomShouldBeFound(shouldBeFound);
        defaultRoomShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultRoomShouldBeFound(String filter) throws Exception {
        restRoomMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(room.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].attachmentKind").value(hasItem(DEFAULT_ATTACHMENT_KIND.toString())))
            .andExpect(jsonPath("$.[*].attachmentReference").value(hasItem(DEFAULT_ATTACHMENT_REFERENCE)))
            .andExpect(jsonPath("$.[*].attachmentService").value(hasItem(DEFAULT_ATTACHMENT_SERVICE)))
            .andExpect(jsonPath("$.[*].ticketReference").value(hasItem(DEFAULT_TICKET_REFERENCE)))
            .andExpect(jsonPath("$.[*].matrixRoomId").value(hasItem(DEFAULT_MATRIX_ROOM_ID)))
            .andExpect(jsonPath("$.[*].encryption").value(hasItem(DEFAULT_ENCRYPTION)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].retention").value(hasItem(DEFAULT_RETENTION.toString())))
            .andExpect(jsonPath("$.[*].groupSessionEpoch").value(hasItem(DEFAULT_GROUP_SESSION_EPOCH)))
            .andExpect(jsonPath("$.[*].frozenAt").value(hasItem(DEFAULT_FROZEN_AT.toString())))
            .andExpect(jsonPath("$.[*].archivedAt").value(hasItem(DEFAULT_ARCHIVED_AT.toString())))
            .andExpect(jsonPath("$.[*].purgeAfter").value(hasItem(DEFAULT_PURGE_AFTER.toString())));

        // Check, that the count call also returns 1
        restRoomMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultRoomShouldNotBeFound(String filter) throws Exception {
        restRoomMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restRoomMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingRoom() throws Exception {
        // Get the room
        restRoomMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRoom() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the room
        Room updatedRoom = roomRepository.findById(room.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRoom are not directly saved in db
        em.detach(updatedRoom);
        updatedRoom
            .code(UPDATED_CODE)
            .type(UPDATED_TYPE)
            .attachmentKind(UPDATED_ATTACHMENT_KIND)
            .attachmentReference(UPDATED_ATTACHMENT_REFERENCE)
            .attachmentService(UPDATED_ATTACHMENT_SERVICE)
            .ticketReference(UPDATED_TICKET_REFERENCE)
            .matrixRoomId(UPDATED_MATRIX_ROOM_ID)
            .encryption(UPDATED_ENCRYPTION)
            .createdAt(UPDATED_CREATED_AT)
            .state(UPDATED_STATE)
            .retention(UPDATED_RETENTION)
            .groupSessionEpoch(UPDATED_GROUP_SESSION_EPOCH)
            .frozenAt(UPDATED_FROZEN_AT)
            .archivedAt(UPDATED_ARCHIVED_AT)
            .purgeAfter(UPDATED_PURGE_AFTER);
        RoomDTO roomDTO = roomMapper.toDto(updatedRoom);

        restRoomMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomDTO))
            )
            .andExpect(status().isOk());

        // Validate the Room in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRoomToMatchAllProperties(updatedRoom);
    }

    @Test
    @Transactional
    void putNonExistingRoom() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        room.setId(UUID.randomUUID());

        // Create the Room
        RoomDTO roomDTO = roomMapper.toDto(room);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Room in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRoom() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        room.setId(UUID.randomUUID());

        // Create the Room
        RoomDTO roomDTO = roomMapper.toDto(room);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Room in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRoom() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        room.setId(UUID.randomUUID());

        // Create the Room
        RoomDTO roomDTO = roomMapper.toDto(room);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Room in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRoomWithPatch() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the room using partial update
        Room partialUpdatedRoom = new Room();
        partialUpdatedRoom.setId(room.getId());

        partialUpdatedRoom
            .code(UPDATED_CODE)
            .attachmentReference(UPDATED_ATTACHMENT_REFERENCE)
            .attachmentService(UPDATED_ATTACHMENT_SERVICE)
            .ticketReference(UPDATED_TICKET_REFERENCE)
            .matrixRoomId(UPDATED_MATRIX_ROOM_ID)
            .encryption(UPDATED_ENCRYPTION)
            .retention(UPDATED_RETENTION)
            .frozenAt(UPDATED_FROZEN_AT)
            .archivedAt(UPDATED_ARCHIVED_AT)
            .purgeAfter(UPDATED_PURGE_AFTER);

        restRoomMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoom.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoom))
            )
            .andExpect(status().isOk());

        // Validate the Room in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedRoom, room), getPersistedRoom(room));
    }

    @Test
    @Transactional
    void fullUpdateRoomWithPatch() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the room using partial update
        Room partialUpdatedRoom = new Room();
        partialUpdatedRoom.setId(room.getId());

        partialUpdatedRoom
            .code(UPDATED_CODE)
            .type(UPDATED_TYPE)
            .attachmentKind(UPDATED_ATTACHMENT_KIND)
            .attachmentReference(UPDATED_ATTACHMENT_REFERENCE)
            .attachmentService(UPDATED_ATTACHMENT_SERVICE)
            .ticketReference(UPDATED_TICKET_REFERENCE)
            .matrixRoomId(UPDATED_MATRIX_ROOM_ID)
            .encryption(UPDATED_ENCRYPTION)
            .createdAt(UPDATED_CREATED_AT)
            .state(UPDATED_STATE)
            .retention(UPDATED_RETENTION)
            .groupSessionEpoch(UPDATED_GROUP_SESSION_EPOCH)
            .frozenAt(UPDATED_FROZEN_AT)
            .archivedAt(UPDATED_ARCHIVED_AT)
            .purgeAfter(UPDATED_PURGE_AFTER);

        restRoomMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoom.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoom))
            )
            .andExpect(status().isOk());

        // Validate the Room in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomUpdatableFieldsEquals(partialUpdatedRoom, getPersistedRoom(partialUpdatedRoom));
    }

    @Test
    @Transactional
    void patchNonExistingRoom() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        room.setId(UUID.randomUUID());

        // Create the Room
        RoomDTO roomDTO = roomMapper.toDto(room);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, roomDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Room in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRoom() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        room.setId(UUID.randomUUID());

        // Create the Room
        RoomDTO roomDTO = roomMapper.toDto(room);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Room in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRoom() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        room.setId(UUID.randomUUID());

        // Create the Room
        RoomDTO roomDTO = roomMapper.toDto(room);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMockMvc
            .perform(patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(roomDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Room in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRoom() throws Exception {
        // Initialize the database
        insertedRoom = roomRepository.saveAndFlush(room);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the room
        restRoomMockMvc
            .perform(delete(ENTITY_API_URL_ID, room.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return roomRepository.count();
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

    protected Room getPersistedRoom(Room room) {
        return roomRepository.findById(room.getId()).orElseThrow();
    }

    protected void assertPersistedRoomToMatchAllProperties(Room expectedRoom) {
        assertRoomAllPropertiesEquals(expectedRoom, getPersistedRoom(expectedRoom));
    }

    protected void assertPersistedRoomToMatchUpdatableProperties(Room expectedRoom) {
        assertRoomAllUpdatablePropertiesEquals(expectedRoom, getPersistedRoom(expectedRoom));
    }
}
