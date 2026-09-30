package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.RoomArchiveAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.Room;
import cm.gimac.des.collaboration.domain.RoomArchive;
import cm.gimac.des.collaboration.domain.enumeration.ArchiveState;
import cm.gimac.des.collaboration.repository.RoomArchiveRepository;
import cm.gimac.des.collaboration.service.RoomArchiveService;
import cm.gimac.des.collaboration.service.mapper.RoomArchiveMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
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
 * Integration tests for the {@link RoomArchiveResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.collaboration.security.AuthoritiesConstants.ADMIN)
class RoomArchiveResourceIT {

    private static final String DEFAULT_CRISIS_CELL_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_CRISIS_CELL_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_TICKET_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_TICKET_REFERENCE = "BBBBBBBBBB";

    private static final Integer DEFAULT_MESSAGE_COUNT = 0;
    private static final Integer UPDATED_MESSAGE_COUNT = 1;
    private static final Integer SMALLER_MESSAGE_COUNT = 0 - 1;

    private static final Integer DEFAULT_UNDECRYPTABLE_COUNT = 0;
    private static final Integer UPDATED_UNDECRYPTABLE_COUNT = 1;
    private static final Integer SMALLER_UNDECRYPTABLE_COUNT = 0 - 1;

    private static final Integer DEFAULT_MASKED_SEQUENCES = 0;
    private static final Integer UPDATED_MASKED_SEQUENCES = 1;
    private static final Integer SMALLER_MASKED_SEQUENCES = 0 - 1;

    private static final String DEFAULT_CHAIN_HEAD = "542253761566573184f891ea840eadd8548c89c91409b93b261b5b57be1154c1";
    private static final String UPDATED_CHAIN_HEAD = "23b838a7954be8cb701a36fd29893d7f363a0073e90bf0d9137954739df5384e";

    private static final String DEFAULT_CONTENT_DIGEST = "95af23813bf35071a95c38d40e1fe8819044c7b3523f8f7ac8bd918d2d389539";
    private static final String UPDATED_CONTENT_DIGEST = "2c59b4bb3d3bbc0ab3c1ce274c69a0ba73ce9cbeedfde1b3b0b043ae024f296c";

    private static final byte[] DEFAULT_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/png";

    private static final UUID DEFAULT_DOCUMENT_REF = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_REF = UUID.randomUUID();

    private static final Integer DEFAULT_RETENTION_YEARS = 1;
    private static final Integer UPDATED_RETENTION_YEARS = 2;
    private static final Integer SMALLER_RETENTION_YEARS = 1 - 1;

    private static final ArchiveState DEFAULT_STATE = ArchiveState.EN_CONSTITUTION;
    private static final ArchiveState UPDATED_STATE = ArchiveState.EN_ATTENTE_HORODATAGE;

    private static final Instant DEFAULT_STARTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTED_AT = Instant.ofEpochMilli(1790746747003L);

    private static final Instant DEFAULT_SEALED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SEALED_AT = Instant.ofEpochMilli(1790746747003L);

    private static final String ENTITY_API_URL = "/api/room-archives";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RoomArchiveRepository roomArchiveRepository;

    @Mock
    private RoomArchiveRepository roomArchiveRepositoryMock;

    @Autowired
    private RoomArchiveMapper roomArchiveMapper;

    @Mock
    private RoomArchiveService roomArchiveServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRoomArchiveMockMvc;

    private RoomArchive roomArchive;

    private RoomArchive insertedRoomArchive;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomArchive createEntity(EntityManager em) {
        RoomArchive roomArchive = new RoomArchive()
            .crisisCellReference(DEFAULT_CRISIS_CELL_REFERENCE)
            .ticketReference(DEFAULT_TICKET_REFERENCE)
            .messageCount(DEFAULT_MESSAGE_COUNT)
            .undecryptableCount(DEFAULT_UNDECRYPTABLE_COUNT)
            .maskedSequences(DEFAULT_MASKED_SEQUENCES)
            .chainHead(DEFAULT_CHAIN_HEAD)
            .contentDigest(DEFAULT_CONTENT_DIGEST)
            .timestampToken(DEFAULT_TIMESTAMP_TOKEN)
            .timestampTokenContentType(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .documentRef(DEFAULT_DOCUMENT_REF)
            .retentionYears(DEFAULT_RETENTION_YEARS)
            .state(DEFAULT_STATE)
            .startedAt(DEFAULT_STARTED_AT)
            .sealedAt(DEFAULT_SEALED_AT);
        // Add required entity
        Room room;
        if (TestUtil.findAll(em, Room.class).isEmpty()) {
            room = RoomResourceIT.createEntity();
            em.persist(room);
            em.flush();
        } else {
            room = TestUtil.findAll(em, Room.class).getFirst();
        }
        roomArchive.setRoom(room);
        return roomArchive;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomArchive createUpdatedEntity(EntityManager em) {
        RoomArchive updatedRoomArchive = new RoomArchive()
            .crisisCellReference(UPDATED_CRISIS_CELL_REFERENCE)
            .ticketReference(UPDATED_TICKET_REFERENCE)
            .messageCount(UPDATED_MESSAGE_COUNT)
            .undecryptableCount(UPDATED_UNDECRYPTABLE_COUNT)
            .maskedSequences(UPDATED_MASKED_SEQUENCES)
            .chainHead(UPDATED_CHAIN_HEAD)
            .contentDigest(UPDATED_CONTENT_DIGEST)
            .timestampToken(UPDATED_TIMESTAMP_TOKEN)
            .timestampTokenContentType(UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .documentRef(UPDATED_DOCUMENT_REF)
            .retentionYears(UPDATED_RETENTION_YEARS)
            .state(UPDATED_STATE)
            .startedAt(UPDATED_STARTED_AT)
            .sealedAt(UPDATED_SEALED_AT);
        // Add required entity
        Room room;
        if (TestUtil.findAll(em, Room.class).isEmpty()) {
            room = RoomResourceIT.createUpdatedEntity();
            em.persist(room);
            em.flush();
        } else {
            room = TestUtil.findAll(em, Room.class).getFirst();
        }
        updatedRoomArchive.setRoom(room);
        return updatedRoomArchive;
    }

    @BeforeEach
    void initTest() {
        roomArchive = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedRoomArchive != null) {
            roomArchiveRepository.delete(insertedRoomArchive);
            insertedRoomArchive = null;
        }
    }

    @Test
    @Transactional
    void getAllRoomArchives() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList
        restRoomArchiveMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(roomArchive.getId().toString())))
            .andExpect(jsonPath("$.[*].crisisCellReference").value(hasItem(DEFAULT_CRISIS_CELL_REFERENCE)))
            .andExpect(jsonPath("$.[*].ticketReference").value(hasItem(DEFAULT_TICKET_REFERENCE)))
            .andExpect(jsonPath("$.[*].messageCount").value(hasItem(DEFAULT_MESSAGE_COUNT)))
            .andExpect(jsonPath("$.[*].undecryptableCount").value(hasItem(DEFAULT_UNDECRYPTABLE_COUNT)))
            .andExpect(jsonPath("$.[*].maskedSequences").value(hasItem(DEFAULT_MASKED_SEQUENCES)))
            .andExpect(jsonPath("$.[*].chainHead").value(hasItem(DEFAULT_CHAIN_HEAD)))
            .andExpect(jsonPath("$.[*].contentDigest").value(hasItem(DEFAULT_CONTENT_DIGEST)))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())))
            .andExpect(jsonPath("$.[*].retentionYears").value(hasItem(DEFAULT_RETENTION_YEARS)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].startedAt").value(hasItem(DEFAULT_STARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoomArchivesWithEagerRelationshipsIsEnabled() throws Exception {
        when(roomArchiveServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoomArchiveMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(roomArchiveServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoomArchivesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(roomArchiveServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoomArchiveMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(roomArchiveRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRoomArchive() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get the roomArchive
        restRoomArchiveMockMvc
            .perform(get(ENTITY_API_URL_ID, roomArchive.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(roomArchive.getId().toString()))
            .andExpect(jsonPath("$.crisisCellReference").value(DEFAULT_CRISIS_CELL_REFERENCE))
            .andExpect(jsonPath("$.ticketReference").value(DEFAULT_TICKET_REFERENCE))
            .andExpect(jsonPath("$.messageCount").value(DEFAULT_MESSAGE_COUNT))
            .andExpect(jsonPath("$.undecryptableCount").value(DEFAULT_UNDECRYPTABLE_COUNT))
            .andExpect(jsonPath("$.maskedSequences").value(DEFAULT_MASKED_SEQUENCES))
            .andExpect(jsonPath("$.chainHead").value(DEFAULT_CHAIN_HEAD))
            .andExpect(jsonPath("$.contentDigest").value(DEFAULT_CONTENT_DIGEST))
            .andExpect(jsonPath("$.timestampTokenContentType").value(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE))
            .andExpect(jsonPath("$.timestampToken").value(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN)))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF.toString()))
            .andExpect(jsonPath("$.retentionYears").value(DEFAULT_RETENTION_YEARS))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.startedAt").value(DEFAULT_STARTED_AT.toString()))
            .andExpect(jsonPath("$.sealedAt").value(DEFAULT_SEALED_AT.toString()));
    }

    @Test
    @Transactional
    void getRoomArchivesByIdFiltering() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        UUID id = roomArchive.getId();

        defaultRoomArchiveFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByCrisisCellReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where crisisCellReference equals to
        defaultRoomArchiveFiltering(
            "crisisCellReference.equals=" + DEFAULT_CRISIS_CELL_REFERENCE,
            "crisisCellReference.equals=" + UPDATED_CRISIS_CELL_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByCrisisCellReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where crisisCellReference in
        defaultRoomArchiveFiltering(
            "crisisCellReference.in=" + DEFAULT_CRISIS_CELL_REFERENCE + "," + UPDATED_CRISIS_CELL_REFERENCE,
            "crisisCellReference.in=" + UPDATED_CRISIS_CELL_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByCrisisCellReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where crisisCellReference is not null
        defaultRoomArchiveFiltering("crisisCellReference.specified=true", "crisisCellReference.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesByCrisisCellReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where crisisCellReference contains
        defaultRoomArchiveFiltering(
            "crisisCellReference.contains=" + DEFAULT_CRISIS_CELL_REFERENCE,
            "crisisCellReference.contains=" + UPDATED_CRISIS_CELL_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByCrisisCellReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where crisisCellReference does not contain
        defaultRoomArchiveFiltering(
            "crisisCellReference.doesNotContain=" + UPDATED_CRISIS_CELL_REFERENCE,
            "crisisCellReference.doesNotContain=" + DEFAULT_CRISIS_CELL_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByTicketReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where ticketReference equals to
        defaultRoomArchiveFiltering(
            "ticketReference.equals=" + DEFAULT_TICKET_REFERENCE,
            "ticketReference.equals=" + UPDATED_TICKET_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByTicketReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where ticketReference in
        defaultRoomArchiveFiltering(
            "ticketReference.in=" + DEFAULT_TICKET_REFERENCE + "," + UPDATED_TICKET_REFERENCE,
            "ticketReference.in=" + UPDATED_TICKET_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByTicketReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where ticketReference is not null
        defaultRoomArchiveFiltering("ticketReference.specified=true", "ticketReference.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesByTicketReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where ticketReference contains
        defaultRoomArchiveFiltering(
            "ticketReference.contains=" + DEFAULT_TICKET_REFERENCE,
            "ticketReference.contains=" + UPDATED_TICKET_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByTicketReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where ticketReference does not contain
        defaultRoomArchiveFiltering(
            "ticketReference.doesNotContain=" + UPDATED_TICKET_REFERENCE,
            "ticketReference.doesNotContain=" + DEFAULT_TICKET_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMessageCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where messageCount equals to
        defaultRoomArchiveFiltering("messageCount.equals=" + DEFAULT_MESSAGE_COUNT, "messageCount.equals=" + UPDATED_MESSAGE_COUNT);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMessageCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where messageCount in
        defaultRoomArchiveFiltering(
            "messageCount.in=" + DEFAULT_MESSAGE_COUNT + "," + UPDATED_MESSAGE_COUNT,
            "messageCount.in=" + UPDATED_MESSAGE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMessageCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where messageCount is not null
        defaultRoomArchiveFiltering("messageCount.specified=true", "messageCount.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMessageCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where messageCount is greater than or equal to
        defaultRoomArchiveFiltering(
            "messageCount.greaterThanOrEqual=" + DEFAULT_MESSAGE_COUNT,
            "messageCount.greaterThanOrEqual=" + UPDATED_MESSAGE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMessageCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where messageCount is less than or equal to
        defaultRoomArchiveFiltering(
            "messageCount.lessThanOrEqual=" + DEFAULT_MESSAGE_COUNT,
            "messageCount.lessThanOrEqual=" + SMALLER_MESSAGE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMessageCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where messageCount is less than
        defaultRoomArchiveFiltering("messageCount.lessThan=" + UPDATED_MESSAGE_COUNT, "messageCount.lessThan=" + DEFAULT_MESSAGE_COUNT);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMessageCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where messageCount is greater than
        defaultRoomArchiveFiltering(
            "messageCount.greaterThan=" + SMALLER_MESSAGE_COUNT,
            "messageCount.greaterThan=" + DEFAULT_MESSAGE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByUndecryptableCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where undecryptableCount equals to
        defaultRoomArchiveFiltering(
            "undecryptableCount.equals=" + DEFAULT_UNDECRYPTABLE_COUNT,
            "undecryptableCount.equals=" + UPDATED_UNDECRYPTABLE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByUndecryptableCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where undecryptableCount in
        defaultRoomArchiveFiltering(
            "undecryptableCount.in=" + DEFAULT_UNDECRYPTABLE_COUNT + "," + UPDATED_UNDECRYPTABLE_COUNT,
            "undecryptableCount.in=" + UPDATED_UNDECRYPTABLE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByUndecryptableCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where undecryptableCount is not null
        defaultRoomArchiveFiltering("undecryptableCount.specified=true", "undecryptableCount.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesByUndecryptableCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where undecryptableCount is greater than or equal to
        defaultRoomArchiveFiltering(
            "undecryptableCount.greaterThanOrEqual=" + DEFAULT_UNDECRYPTABLE_COUNT,
            "undecryptableCount.greaterThanOrEqual=" + UPDATED_UNDECRYPTABLE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByUndecryptableCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where undecryptableCount is less than or equal to
        defaultRoomArchiveFiltering(
            "undecryptableCount.lessThanOrEqual=" + DEFAULT_UNDECRYPTABLE_COUNT,
            "undecryptableCount.lessThanOrEqual=" + SMALLER_UNDECRYPTABLE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByUndecryptableCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where undecryptableCount is less than
        defaultRoomArchiveFiltering(
            "undecryptableCount.lessThan=" + UPDATED_UNDECRYPTABLE_COUNT,
            "undecryptableCount.lessThan=" + DEFAULT_UNDECRYPTABLE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByUndecryptableCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where undecryptableCount is greater than
        defaultRoomArchiveFiltering(
            "undecryptableCount.greaterThan=" + SMALLER_UNDECRYPTABLE_COUNT,
            "undecryptableCount.greaterThan=" + DEFAULT_UNDECRYPTABLE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMaskedSequencesIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where maskedSequences equals to
        defaultRoomArchiveFiltering(
            "maskedSequences.equals=" + DEFAULT_MASKED_SEQUENCES,
            "maskedSequences.equals=" + UPDATED_MASKED_SEQUENCES
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMaskedSequencesIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where maskedSequences in
        defaultRoomArchiveFiltering(
            "maskedSequences.in=" + DEFAULT_MASKED_SEQUENCES + "," + UPDATED_MASKED_SEQUENCES,
            "maskedSequences.in=" + UPDATED_MASKED_SEQUENCES
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMaskedSequencesIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where maskedSequences is not null
        defaultRoomArchiveFiltering("maskedSequences.specified=true", "maskedSequences.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMaskedSequencesIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where maskedSequences is greater than or equal to
        defaultRoomArchiveFiltering(
            "maskedSequences.greaterThanOrEqual=" + DEFAULT_MASKED_SEQUENCES,
            "maskedSequences.greaterThanOrEqual=" + UPDATED_MASKED_SEQUENCES
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMaskedSequencesIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where maskedSequences is less than or equal to
        defaultRoomArchiveFiltering(
            "maskedSequences.lessThanOrEqual=" + DEFAULT_MASKED_SEQUENCES,
            "maskedSequences.lessThanOrEqual=" + SMALLER_MASKED_SEQUENCES
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMaskedSequencesIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where maskedSequences is less than
        defaultRoomArchiveFiltering(
            "maskedSequences.lessThan=" + UPDATED_MASKED_SEQUENCES,
            "maskedSequences.lessThan=" + DEFAULT_MASKED_SEQUENCES
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByMaskedSequencesIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where maskedSequences is greater than
        defaultRoomArchiveFiltering(
            "maskedSequences.greaterThan=" + SMALLER_MASKED_SEQUENCES,
            "maskedSequences.greaterThan=" + DEFAULT_MASKED_SEQUENCES
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByChainHeadIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where chainHead equals to
        defaultRoomArchiveFiltering("chainHead.equals=" + DEFAULT_CHAIN_HEAD, "chainHead.equals=" + UPDATED_CHAIN_HEAD);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByChainHeadIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where chainHead in
        defaultRoomArchiveFiltering("chainHead.in=" + DEFAULT_CHAIN_HEAD + "," + UPDATED_CHAIN_HEAD, "chainHead.in=" + UPDATED_CHAIN_HEAD);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByChainHeadIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where chainHead is not null
        defaultRoomArchiveFiltering("chainHead.specified=true", "chainHead.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesByChainHeadContainsSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where chainHead contains
        defaultRoomArchiveFiltering("chainHead.contains=" + DEFAULT_CHAIN_HEAD, "chainHead.contains=" + UPDATED_CHAIN_HEAD);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByChainHeadNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where chainHead does not contain
        defaultRoomArchiveFiltering("chainHead.doesNotContain=" + UPDATED_CHAIN_HEAD, "chainHead.doesNotContain=" + DEFAULT_CHAIN_HEAD);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByContentDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where contentDigest equals to
        defaultRoomArchiveFiltering("contentDigest.equals=" + DEFAULT_CONTENT_DIGEST, "contentDigest.equals=" + UPDATED_CONTENT_DIGEST);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByContentDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where contentDigest in
        defaultRoomArchiveFiltering(
            "contentDigest.in=" + DEFAULT_CONTENT_DIGEST + "," + UPDATED_CONTENT_DIGEST,
            "contentDigest.in=" + UPDATED_CONTENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByContentDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where contentDigest is not null
        defaultRoomArchiveFiltering("contentDigest.specified=true", "contentDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesByContentDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where contentDigest contains
        defaultRoomArchiveFiltering("contentDigest.contains=" + DEFAULT_CONTENT_DIGEST, "contentDigest.contains=" + UPDATED_CONTENT_DIGEST);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByContentDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where contentDigest does not contain
        defaultRoomArchiveFiltering(
            "contentDigest.doesNotContain=" + UPDATED_CONTENT_DIGEST,
            "contentDigest.doesNotContain=" + DEFAULT_CONTENT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByDocumentRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where documentRef equals to
        defaultRoomArchiveFiltering("documentRef.equals=" + DEFAULT_DOCUMENT_REF, "documentRef.equals=" + UPDATED_DOCUMENT_REF);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByDocumentRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where documentRef in
        defaultRoomArchiveFiltering(
            "documentRef.in=" + DEFAULT_DOCUMENT_REF + "," + UPDATED_DOCUMENT_REF,
            "documentRef.in=" + UPDATED_DOCUMENT_REF
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByDocumentRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where documentRef is not null
        defaultRoomArchiveFiltering("documentRef.specified=true", "documentRef.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesByRetentionYearsIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where retentionYears equals to
        defaultRoomArchiveFiltering("retentionYears.equals=" + DEFAULT_RETENTION_YEARS, "retentionYears.equals=" + UPDATED_RETENTION_YEARS);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByRetentionYearsIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where retentionYears in
        defaultRoomArchiveFiltering(
            "retentionYears.in=" + DEFAULT_RETENTION_YEARS + "," + UPDATED_RETENTION_YEARS,
            "retentionYears.in=" + UPDATED_RETENTION_YEARS
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByRetentionYearsIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where retentionYears is not null
        defaultRoomArchiveFiltering("retentionYears.specified=true", "retentionYears.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesByRetentionYearsIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where retentionYears is greater than or equal to
        defaultRoomArchiveFiltering(
            "retentionYears.greaterThanOrEqual=" + DEFAULT_RETENTION_YEARS,
            "retentionYears.greaterThanOrEqual=" + (DEFAULT_RETENTION_YEARS + 1)
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByRetentionYearsIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where retentionYears is less than or equal to
        defaultRoomArchiveFiltering(
            "retentionYears.lessThanOrEqual=" + DEFAULT_RETENTION_YEARS,
            "retentionYears.lessThanOrEqual=" + SMALLER_RETENTION_YEARS
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByRetentionYearsIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where retentionYears is less than
        defaultRoomArchiveFiltering(
            "retentionYears.lessThan=" + (DEFAULT_RETENTION_YEARS + 1),
            "retentionYears.lessThan=" + DEFAULT_RETENTION_YEARS
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByRetentionYearsIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where retentionYears is greater than
        defaultRoomArchiveFiltering(
            "retentionYears.greaterThan=" + SMALLER_RETENTION_YEARS,
            "retentionYears.greaterThan=" + DEFAULT_RETENTION_YEARS
        );
    }

    @Test
    @Transactional
    void getAllRoomArchivesByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where state equals to
        defaultRoomArchiveFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where state in
        defaultRoomArchiveFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where state is not null
        defaultRoomArchiveFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesByStartedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where startedAt equals to
        defaultRoomArchiveFiltering("startedAt.equals=" + DEFAULT_STARTED_AT, "startedAt.equals=" + UPDATED_STARTED_AT);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByStartedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where startedAt in
        defaultRoomArchiveFiltering("startedAt.in=" + DEFAULT_STARTED_AT + "," + UPDATED_STARTED_AT, "startedAt.in=" + UPDATED_STARTED_AT);
    }

    @Test
    @Transactional
    void getAllRoomArchivesByStartedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where startedAt is not null
        defaultRoomArchiveFiltering("startedAt.specified=true", "startedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesBySealedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where sealedAt equals to
        defaultRoomArchiveFiltering("sealedAt.equals=" + DEFAULT_SEALED_AT, "sealedAt.equals=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllRoomArchivesBySealedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where sealedAt in
        defaultRoomArchiveFiltering("sealedAt.in=" + DEFAULT_SEALED_AT + "," + UPDATED_SEALED_AT, "sealedAt.in=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllRoomArchivesBySealedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRoomArchive = roomArchiveRepository.saveAndFlush(roomArchive);

        // Get all the roomArchiveList where sealedAt is not null
        defaultRoomArchiveFiltering("sealedAt.specified=true", "sealedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllRoomArchivesByRoomIsEqualToSomething() throws Exception {
        // Get already existing entity
        Room room = roomArchive.getRoom();
        roomArchiveRepository.saveAndFlush(roomArchive);
        UUID roomId = room.getId();
        // Get all the roomArchiveList where room equals to roomId
        defaultRoomArchiveShouldBeFound("roomId.equals=" + roomId);

        // Get all the roomArchiveList where room equals to UUID.randomUUID()
        defaultRoomArchiveShouldNotBeFound("roomId.equals=" + UUID.randomUUID());
    }

    private void defaultRoomArchiveFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultRoomArchiveShouldBeFound(shouldBeFound);
        defaultRoomArchiveShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultRoomArchiveShouldBeFound(String filter) throws Exception {
        restRoomArchiveMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(roomArchive.getId().toString())))
            .andExpect(jsonPath("$.[*].crisisCellReference").value(hasItem(DEFAULT_CRISIS_CELL_REFERENCE)))
            .andExpect(jsonPath("$.[*].ticketReference").value(hasItem(DEFAULT_TICKET_REFERENCE)))
            .andExpect(jsonPath("$.[*].messageCount").value(hasItem(DEFAULT_MESSAGE_COUNT)))
            .andExpect(jsonPath("$.[*].undecryptableCount").value(hasItem(DEFAULT_UNDECRYPTABLE_COUNT)))
            .andExpect(jsonPath("$.[*].maskedSequences").value(hasItem(DEFAULT_MASKED_SEQUENCES)))
            .andExpect(jsonPath("$.[*].chainHead").value(hasItem(DEFAULT_CHAIN_HEAD)))
            .andExpect(jsonPath("$.[*].contentDigest").value(hasItem(DEFAULT_CONTENT_DIGEST)))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF.toString())))
            .andExpect(jsonPath("$.[*].retentionYears").value(hasItem(DEFAULT_RETENTION_YEARS)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].startedAt").value(hasItem(DEFAULT_STARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())));

        // Check, that the count call also returns 1
        restRoomArchiveMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultRoomArchiveShouldNotBeFound(String filter) throws Exception {
        restRoomArchiveMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restRoomArchiveMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingRoomArchive() throws Exception {
        // Get the roomArchive
        restRoomArchiveMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return roomArchiveRepository.count();
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

    protected RoomArchive getPersistedRoomArchive(RoomArchive roomArchive) {
        return roomArchiveRepository.findById(roomArchive.getId()).orElseThrow();
    }

    protected void assertPersistedRoomArchiveToMatchAllProperties(RoomArchive expectedRoomArchive) {
        assertRoomArchiveAllPropertiesEquals(expectedRoomArchive, getPersistedRoomArchive(expectedRoomArchive));
    }

    protected void assertPersistedRoomArchiveToMatchUpdatableProperties(RoomArchive expectedRoomArchive) {
        assertRoomArchiveAllUpdatablePropertiesEquals(expectedRoomArchive, getPersistedRoomArchive(expectedRoomArchive));
    }
}
