package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.RoomMemberAsserts.*;
import static cm.gimac.des.collaboration.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.Room;
import cm.gimac.des.collaboration.domain.RoomMember;
import cm.gimac.des.collaboration.domain.enumeration.MemberRole;
import cm.gimac.des.collaboration.repository.RoomMemberRepository;
import cm.gimac.des.collaboration.service.RoomMemberService;
import cm.gimac.des.collaboration.service.dto.RoomMemberDTO;
import cm.gimac.des.collaboration.service.mapper.RoomMemberMapper;
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
 * Integration tests for the {@link RoomMemberResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.collaboration.security.AuthoritiesConstants.ADMIN)
class RoomMemberResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final MemberRole DEFAULT_ROLE = MemberRole.ANIMATEUR;
    private static final MemberRole UPDATED_ROLE = MemberRole.MEMBRE;

    private static final Instant DEFAULT_INVITED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_INVITED_AT = Instant.ofEpochMilli(1790746747003L);

    private static final Instant DEFAULT_ACKNOWLEDGED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ACKNOWLEDGED_AT = Instant.ofEpochMilli(1790746747003L);

    private static final UUID DEFAULT_REPLACES_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_REPLACES_USER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_JOINED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_JOINED_AT = Instant.ofEpochMilli(1790746747003L);

    private static final Instant DEFAULT_LEFT_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LEFT_AT = Instant.ofEpochMilli(1790746747003L);

    private static final String ENTITY_API_URL = "/api/room-members";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private RoomMemberRepository roomMemberRepositoryMock;

    @Autowired
    private RoomMemberMapper roomMemberMapper;

    @Mock
    private RoomMemberService roomMemberServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRoomMemberMockMvc;

    private RoomMember roomMember;

    private RoomMember insertedRoomMember;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomMember createEntity(EntityManager em) {
        RoomMember roomMember = new RoomMember()
            .userId(DEFAULT_USER_ID)
            .role(DEFAULT_ROLE)
            .invitedAt(DEFAULT_INVITED_AT)
            .acknowledgedAt(DEFAULT_ACKNOWLEDGED_AT)
            .replacesUserId(DEFAULT_REPLACES_USER_ID)
            .joinedAt(DEFAULT_JOINED_AT)
            .leftAt(DEFAULT_LEFT_AT);
        // Add required entity
        Room room;
        if (TestUtil.findAll(em, Room.class).isEmpty()) {
            room = RoomResourceIT.createEntity();
            em.persist(room);
            em.flush();
        } else {
            room = TestUtil.findAll(em, Room.class).getFirst();
        }
        roomMember.setRoom(room);
        return roomMember;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomMember createUpdatedEntity(EntityManager em) {
        RoomMember updatedRoomMember = new RoomMember()
            .userId(UPDATED_USER_ID)
            .role(UPDATED_ROLE)
            .invitedAt(UPDATED_INVITED_AT)
            .acknowledgedAt(UPDATED_ACKNOWLEDGED_AT)
            .replacesUserId(UPDATED_REPLACES_USER_ID)
            .joinedAt(UPDATED_JOINED_AT)
            .leftAt(UPDATED_LEFT_AT);
        // Add required entity
        Room room;
        if (TestUtil.findAll(em, Room.class).isEmpty()) {
            room = RoomResourceIT.createUpdatedEntity();
            em.persist(room);
            em.flush();
        } else {
            room = TestUtil.findAll(em, Room.class).getFirst();
        }
        updatedRoomMember.setRoom(room);
        return updatedRoomMember;
    }

    @BeforeEach
    void initTest() {
        roomMember = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedRoomMember != null) {
            roomMemberRepository.delete(insertedRoomMember);
            insertedRoomMember = null;
        }
    }

    @Test
    @Transactional
    void createRoomMember() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);
        var returnedRoomMemberDTO = om.readValue(
            restRoomMemberMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomMemberDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RoomMemberDTO.class
        );

        // Validate the RoomMember in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRoomMember = roomMemberMapper.toEntity(returnedRoomMemberDTO);
        assertRoomMemberUpdatableFieldsEquals(returnedRoomMember, getPersistedRoomMember(returnedRoomMember));

        insertedRoomMember = returnedRoomMember;
    }

    @Test
    @Transactional
    void createRoomMemberWithExistingId() throws Exception {
        // Create the RoomMember with an existing ID
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRoomMemberMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomMemberDTO)))
            .andExpect(status().isBadRequest());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUserIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomMember.setUserId(null);

        // Create the RoomMember, which fails.
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        restRoomMemberMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomMemberDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRoleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomMember.setRole(null);

        // Create the RoomMember, which fails.
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        restRoomMemberMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomMemberDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkInvitedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomMember.setInvitedAt(null);

        // Create the RoomMember, which fails.
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        restRoomMemberMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomMemberDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRoomMembers() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList
        restRoomMemberMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(roomMember.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].role").value(hasItem(DEFAULT_ROLE.toString())))
            .andExpect(jsonPath("$.[*].invitedAt").value(hasItem(DEFAULT_INVITED_AT.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())))
            .andExpect(jsonPath("$.[*].replacesUserId").value(hasItem(DEFAULT_REPLACES_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].joinedAt").value(hasItem(DEFAULT_JOINED_AT.toString())))
            .andExpect(jsonPath("$.[*].leftAt").value(hasItem(DEFAULT_LEFT_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoomMembersWithEagerRelationshipsIsEnabled() throws Exception {
        when(roomMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoomMemberMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(roomMemberServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoomMembersWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(roomMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoomMemberMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(roomMemberRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRoomMember() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get the roomMember
        restRoomMemberMockMvc
            .perform(get(ENTITY_API_URL_ID, roomMember.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(roomMember.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.role").value(DEFAULT_ROLE.toString()))
            .andExpect(jsonPath("$.invitedAt").value(DEFAULT_INVITED_AT.toString()))
            .andExpect(jsonPath("$.acknowledgedAt").value(DEFAULT_ACKNOWLEDGED_AT.toString()))
            .andExpect(jsonPath("$.replacesUserId").value(DEFAULT_REPLACES_USER_ID.toString()))
            .andExpect(jsonPath("$.joinedAt").value(DEFAULT_JOINED_AT.toString()))
            .andExpect(jsonPath("$.leftAt").value(DEFAULT_LEFT_AT.toString()));
    }

    @Test
    @Transactional
    void getRoomMembersByIdFiltering() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        UUID id = roomMember.getId();

        defaultRoomMemberFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllRoomMembersByUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where userId equals to
        defaultRoomMemberFiltering("userId.equals=" + DEFAULT_USER_ID, "userId.equals=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllRoomMembersByUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where userId in
        defaultRoomMemberFiltering("userId.in=" + DEFAULT_USER_ID + "," + UPDATED_USER_ID, "userId.in=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllRoomMembersByUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where userId is not null
        defaultRoomMemberFiltering("userId.specified=true", "userId.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomMembersByRoleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where role equals to
        defaultRoomMemberFiltering("role.equals=" + DEFAULT_ROLE, "role.equals=" + UPDATED_ROLE);
    }

    @Test
    @Transactional
    void getAllRoomMembersByRoleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where role in
        defaultRoomMemberFiltering("role.in=" + DEFAULT_ROLE + "," + UPDATED_ROLE, "role.in=" + UPDATED_ROLE);
    }

    @Test
    @Transactional
    void getAllRoomMembersByRoleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where role is not null
        defaultRoomMemberFiltering("role.specified=true", "role.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomMembersByInvitedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where invitedAt equals to
        defaultRoomMemberFiltering("invitedAt.equals=" + DEFAULT_INVITED_AT, "invitedAt.equals=" + UPDATED_INVITED_AT);
    }

    @Test
    @Transactional
    void getAllRoomMembersByInvitedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where invitedAt in
        defaultRoomMemberFiltering("invitedAt.in=" + DEFAULT_INVITED_AT + "," + UPDATED_INVITED_AT, "invitedAt.in=" + UPDATED_INVITED_AT);
    }

    @Test
    @Transactional
    void getAllRoomMembersByInvitedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where invitedAt is not null
        defaultRoomMemberFiltering("invitedAt.specified=true", "invitedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomMembersByAcknowledgedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where acknowledgedAt equals to
        defaultRoomMemberFiltering("acknowledgedAt.equals=" + DEFAULT_ACKNOWLEDGED_AT, "acknowledgedAt.equals=" + UPDATED_ACKNOWLEDGED_AT);
    }

    @Test
    @Transactional
    void getAllRoomMembersByAcknowledgedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where acknowledgedAt in
        defaultRoomMemberFiltering(
            "acknowledgedAt.in=" + DEFAULT_ACKNOWLEDGED_AT + "," + UPDATED_ACKNOWLEDGED_AT,
            "acknowledgedAt.in=" + UPDATED_ACKNOWLEDGED_AT
        );
    }

    @Test
    @Transactional
    void getAllRoomMembersByAcknowledgedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where acknowledgedAt is not null
        defaultRoomMemberFiltering("acknowledgedAt.specified=true", "acknowledgedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomMembersByReplacesUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where replacesUserId equals to
        defaultRoomMemberFiltering(
            "replacesUserId.equals=" + DEFAULT_REPLACES_USER_ID,
            "replacesUserId.equals=" + UPDATED_REPLACES_USER_ID
        );
    }

    @Test
    @Transactional
    void getAllRoomMembersByReplacesUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where replacesUserId in
        defaultRoomMemberFiltering(
            "replacesUserId.in=" + DEFAULT_REPLACES_USER_ID + "," + UPDATED_REPLACES_USER_ID,
            "replacesUserId.in=" + UPDATED_REPLACES_USER_ID
        );
    }

    @Test
    @Transactional
    void getAllRoomMembersByReplacesUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where replacesUserId is not null
        defaultRoomMemberFiltering("replacesUserId.specified=true", "replacesUserId.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomMembersByJoinedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where joinedAt equals to
        defaultRoomMemberFiltering("joinedAt.equals=" + DEFAULT_JOINED_AT, "joinedAt.equals=" + UPDATED_JOINED_AT);
    }

    @Test
    @Transactional
    void getAllRoomMembersByJoinedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where joinedAt in
        defaultRoomMemberFiltering("joinedAt.in=" + DEFAULT_JOINED_AT + "," + UPDATED_JOINED_AT, "joinedAt.in=" + UPDATED_JOINED_AT);
    }

    @Test
    @Transactional
    void getAllRoomMembersByJoinedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where joinedAt is not null
        defaultRoomMemberFiltering("joinedAt.specified=true", "joinedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomMembersByLeftAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where leftAt equals to
        defaultRoomMemberFiltering("leftAt.equals=" + DEFAULT_LEFT_AT, "leftAt.equals=" + UPDATED_LEFT_AT);
    }

    @Test
    @Transactional
    void getAllRoomMembersByLeftAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where leftAt in
        defaultRoomMemberFiltering("leftAt.in=" + DEFAULT_LEFT_AT + "," + UPDATED_LEFT_AT, "leftAt.in=" + UPDATED_LEFT_AT);
    }

    @Test
    @Transactional
    void getAllRoomMembersByLeftAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList where leftAt is not null
        defaultRoomMemberFiltering("leftAt.specified=true", "leftAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomMembersByRoomIsEqualToSomething() throws Exception {
        Room room;
        if (TestUtil.findAll(em, Room.class).isEmpty()) {
            roomMemberRepository.saveAndFlush(roomMember);
            room = RoomResourceIT.createEntity();
        } else {
            room = TestUtil.findAll(em, Room.class).getFirst();
        }
        em.persist(room);
        em.flush();
        roomMember.setRoom(room);
        roomMemberRepository.saveAndFlush(roomMember);
        UUID roomId = room.getId();
        // Get all the roomMemberList where room equals to roomId
        defaultRoomMemberShouldBeFound("roomId.equals=" + roomId);

        // Get all the roomMemberList where room equals to UUID.randomUUID()
        defaultRoomMemberShouldNotBeFound("roomId.equals=" + UUID.randomUUID());
    }

    private void defaultRoomMemberFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultRoomMemberShouldBeFound(shouldBeFound);
        defaultRoomMemberShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultRoomMemberShouldBeFound(String filter) throws Exception {
        restRoomMemberMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(roomMember.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].role").value(hasItem(DEFAULT_ROLE.toString())))
            .andExpect(jsonPath("$.[*].invitedAt").value(hasItem(DEFAULT_INVITED_AT.toString())))
            .andExpect(jsonPath("$.[*].acknowledgedAt").value(hasItem(DEFAULT_ACKNOWLEDGED_AT.toString())))
            .andExpect(jsonPath("$.[*].replacesUserId").value(hasItem(DEFAULT_REPLACES_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].joinedAt").value(hasItem(DEFAULT_JOINED_AT.toString())))
            .andExpect(jsonPath("$.[*].leftAt").value(hasItem(DEFAULT_LEFT_AT.toString())));

        // Check, that the count call also returns 1
        restRoomMemberMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultRoomMemberShouldNotBeFound(String filter) throws Exception {
        restRoomMemberMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restRoomMemberMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingRoomMember() throws Exception {
        // Get the roomMember
        restRoomMemberMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRoomMember() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomMember
        RoomMember updatedRoomMember = roomMemberRepository.findById(roomMember.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRoomMember are not directly saved in db
        em.detach(updatedRoomMember);
        updatedRoomMember
            .userId(UPDATED_USER_ID)
            .role(UPDATED_ROLE)
            .invitedAt(UPDATED_INVITED_AT)
            .acknowledgedAt(UPDATED_ACKNOWLEDGED_AT)
            .replacesUserId(UPDATED_REPLACES_USER_ID)
            .joinedAt(UPDATED_JOINED_AT)
            .leftAt(UPDATED_LEFT_AT);
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(updatedRoomMember);

        restRoomMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomMemberDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomMemberDTO))
            )
            .andExpect(status().isOk());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRoomMemberToMatchAllProperties(updatedRoomMember);
    }

    @Test
    @Transactional
    void putNonExistingRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomMemberDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomMemberDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRoomMemberWithPatch() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomMember using partial update
        RoomMember partialUpdatedRoomMember = new RoomMember();
        partialUpdatedRoomMember.setId(roomMember.getId());

        partialUpdatedRoomMember.userId(UPDATED_USER_ID).role(UPDATED_ROLE).invitedAt(UPDATED_INVITED_AT).leftAt(UPDATED_LEFT_AT);

        restRoomMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoomMember.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoomMember))
            )
            .andExpect(status().isOk());

        // Validate the RoomMember in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomMemberUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRoomMember, roomMember),
            getPersistedRoomMember(roomMember)
        );
    }

    @Test
    @Transactional
    void fullUpdateRoomMemberWithPatch() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomMember using partial update
        RoomMember partialUpdatedRoomMember = new RoomMember();
        partialUpdatedRoomMember.setId(roomMember.getId());

        partialUpdatedRoomMember
            .userId(UPDATED_USER_ID)
            .role(UPDATED_ROLE)
            .invitedAt(UPDATED_INVITED_AT)
            .acknowledgedAt(UPDATED_ACKNOWLEDGED_AT)
            .replacesUserId(UPDATED_REPLACES_USER_ID)
            .joinedAt(UPDATED_JOINED_AT)
            .leftAt(UPDATED_LEFT_AT);

        restRoomMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoomMember.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoomMember))
            )
            .andExpect(status().isOk());

        // Validate the RoomMember in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomMemberUpdatableFieldsEquals(partialUpdatedRoomMember, getPersistedRoomMember(partialUpdatedRoomMember));
    }

    @Test
    @Transactional
    void patchNonExistingRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, roomMemberDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(roomMemberDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRoomMember() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the roomMember
        restRoomMemberMockMvc
            .perform(delete(ENTITY_API_URL_ID, roomMember.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return roomMemberRepository.count();
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

    protected RoomMember getPersistedRoomMember(RoomMember roomMember) {
        return roomMemberRepository.findById(roomMember.getId()).orElseThrow();
    }

    protected void assertPersistedRoomMemberToMatchAllProperties(RoomMember expectedRoomMember) {
        assertRoomMemberAllPropertiesEquals(expectedRoomMember, getPersistedRoomMember(expectedRoomMember));
    }

    protected void assertPersistedRoomMemberToMatchUpdatableProperties(RoomMember expectedRoomMember) {
        assertRoomMemberAllUpdatablePropertiesEquals(expectedRoomMember, getPersistedRoomMember(expectedRoomMember));
    }
}
