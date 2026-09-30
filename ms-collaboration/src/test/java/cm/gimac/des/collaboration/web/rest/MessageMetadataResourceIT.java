package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.MessageMetadataAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.MessageMetadata;
import cm.gimac.des.collaboration.domain.Room;
import cm.gimac.des.collaboration.repository.MessageMetadataRepository;
import cm.gimac.des.collaboration.service.MessageMetadataService;
import cm.gimac.des.collaboration.service.mapper.MessageMetadataMapper;
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
 * Integration tests for the {@link MessageMetadataResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.collaboration.security.AuthoritiesConstants.ADMIN)
class MessageMetadataResourceIT {

    private static final String DEFAULT_MATRIX_EVENT_ID = "AAAAAAAAAA";
    private static final String UPDATED_MATRIX_EVENT_ID = "BBBBBBBBBB";

    private static final UUID DEFAULT_AUTHOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_AUTHOR_ID = UUID.randomUUID();

    private static final String DEFAULT_TERMINAL_ID = "AAAAAAAAAA";
    private static final String UPDATED_TERMINAL_ID = "BBBBBBBBBB";

    private static final Instant DEFAULT_SENT_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SENT_AT = Instant.ofEpochMilli(1790746747003L);

    private static final String DEFAULT_SESSION_ID = "AAAAAAAAAA";
    private static final String UPDATED_SESSION_ID = "BBBBBBBBBB";

    private static final Long DEFAULT_RATCHET_INDEX = 0L;
    private static final Long UPDATED_RATCHET_INDEX = 1L;
    private static final Long SMALLER_RATCHET_INDEX = 0L - 1L;

    private static final Integer DEFAULT_CIPHERTEXT_SIZE = 0;
    private static final Integer UPDATED_CIPHERTEXT_SIZE = 1;
    private static final Integer SMALLER_CIPHERTEXT_SIZE = 0 - 1;

    private static final String DEFAULT_ATTACHMENT_REFS = "AAAAAAAAAA";
    private static final String UPDATED_ATTACHMENT_REFS = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/message-metadata";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MessageMetadataRepository messageMetadataRepository;

    @Mock
    private MessageMetadataRepository messageMetadataRepositoryMock;

    @Autowired
    private MessageMetadataMapper messageMetadataMapper;

    @Mock
    private MessageMetadataService messageMetadataServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMessageMetadataMockMvc;

    private MessageMetadata messageMetadata;

    private MessageMetadata insertedMessageMetadata;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MessageMetadata createEntity(EntityManager em) {
        MessageMetadata messageMetadata = new MessageMetadata()
            .matrixEventId(DEFAULT_MATRIX_EVENT_ID)
            .authorId(DEFAULT_AUTHOR_ID)
            .terminalId(DEFAULT_TERMINAL_ID)
            .sentAt(DEFAULT_SENT_AT)
            .sessionId(DEFAULT_SESSION_ID)
            .ratchetIndex(DEFAULT_RATCHET_INDEX)
            .ciphertextSize(DEFAULT_CIPHERTEXT_SIZE)
            .attachmentRefs(DEFAULT_ATTACHMENT_REFS);
        // Add required entity
        Room room;
        if (TestUtil.findAll(em, Room.class).isEmpty()) {
            room = RoomResourceIT.createEntity();
            em.persist(room);
            em.flush();
        } else {
            room = TestUtil.findAll(em, Room.class).getFirst();
        }
        messageMetadata.setRoom(room);
        return messageMetadata;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MessageMetadata createUpdatedEntity(EntityManager em) {
        MessageMetadata updatedMessageMetadata = new MessageMetadata()
            .matrixEventId(UPDATED_MATRIX_EVENT_ID)
            .authorId(UPDATED_AUTHOR_ID)
            .terminalId(UPDATED_TERMINAL_ID)
            .sentAt(UPDATED_SENT_AT)
            .sessionId(UPDATED_SESSION_ID)
            .ratchetIndex(UPDATED_RATCHET_INDEX)
            .ciphertextSize(UPDATED_CIPHERTEXT_SIZE)
            .attachmentRefs(UPDATED_ATTACHMENT_REFS);
        // Add required entity
        Room room;
        if (TestUtil.findAll(em, Room.class).isEmpty()) {
            room = RoomResourceIT.createUpdatedEntity();
            em.persist(room);
            em.flush();
        } else {
            room = TestUtil.findAll(em, Room.class).getFirst();
        }
        updatedMessageMetadata.setRoom(room);
        return updatedMessageMetadata;
    }

    @BeforeEach
    void initTest() {
        messageMetadata = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedMessageMetadata != null) {
            messageMetadataRepository.delete(insertedMessageMetadata);
            insertedMessageMetadata = null;
        }
    }

    @Test
    @Transactional
    void getAllMessageMetadatas() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList
        restMessageMetadataMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(messageMetadata.getId().toString())))
            .andExpect(jsonPath("$.[*].matrixEventId").value(hasItem(DEFAULT_MATRIX_EVENT_ID)))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].terminalId").value(hasItem(DEFAULT_TERMINAL_ID)))
            .andExpect(jsonPath("$.[*].sentAt").value(hasItem(DEFAULT_SENT_AT.toString())))
            .andExpect(jsonPath("$.[*].sessionId").value(hasItem(DEFAULT_SESSION_ID)))
            .andExpect(jsonPath("$.[*].ratchetIndex").value(hasItem(DEFAULT_RATCHET_INDEX.intValue())))
            .andExpect(jsonPath("$.[*].ciphertextSize").value(hasItem(DEFAULT_CIPHERTEXT_SIZE)))
            .andExpect(jsonPath("$.[*].attachmentRefs").value(hasItem(DEFAULT_ATTACHMENT_REFS)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMessageMetadatasWithEagerRelationshipsIsEnabled() throws Exception {
        when(messageMetadataServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMessageMetadataMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(messageMetadataServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMessageMetadatasWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(messageMetadataServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMessageMetadataMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(messageMetadataRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getMessageMetadata() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get the messageMetadata
        restMessageMetadataMockMvc
            .perform(get(ENTITY_API_URL_ID, messageMetadata.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(messageMetadata.getId().toString()))
            .andExpect(jsonPath("$.matrixEventId").value(DEFAULT_MATRIX_EVENT_ID))
            .andExpect(jsonPath("$.authorId").value(DEFAULT_AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.terminalId").value(DEFAULT_TERMINAL_ID))
            .andExpect(jsonPath("$.sentAt").value(DEFAULT_SENT_AT.toString()))
            .andExpect(jsonPath("$.sessionId").value(DEFAULT_SESSION_ID))
            .andExpect(jsonPath("$.ratchetIndex").value(DEFAULT_RATCHET_INDEX.intValue()))
            .andExpect(jsonPath("$.ciphertextSize").value(DEFAULT_CIPHERTEXT_SIZE))
            .andExpect(jsonPath("$.attachmentRefs").value(DEFAULT_ATTACHMENT_REFS));
    }

    @Test
    @Transactional
    void getMessageMetadatasByIdFiltering() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        UUID id = messageMetadata.getId();

        defaultMessageMetadataFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByMatrixEventIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where matrixEventId equals to
        defaultMessageMetadataFiltering(
            "matrixEventId.equals=" + DEFAULT_MATRIX_EVENT_ID,
            "matrixEventId.equals=" + UPDATED_MATRIX_EVENT_ID
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByMatrixEventIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where matrixEventId in
        defaultMessageMetadataFiltering(
            "matrixEventId.in=" + DEFAULT_MATRIX_EVENT_ID + "," + UPDATED_MATRIX_EVENT_ID,
            "matrixEventId.in=" + UPDATED_MATRIX_EVENT_ID
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByMatrixEventIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where matrixEventId is not null
        defaultMessageMetadataFiltering("matrixEventId.specified=true", "matrixEventId.specified=false");
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByMatrixEventIdContainsSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where matrixEventId contains
        defaultMessageMetadataFiltering(
            "matrixEventId.contains=" + DEFAULT_MATRIX_EVENT_ID,
            "matrixEventId.contains=" + UPDATED_MATRIX_EVENT_ID
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByMatrixEventIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where matrixEventId does not contain
        defaultMessageMetadataFiltering(
            "matrixEventId.doesNotContain=" + UPDATED_MATRIX_EVENT_ID,
            "matrixEventId.doesNotContain=" + DEFAULT_MATRIX_EVENT_ID
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByAuthorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where authorId equals to
        defaultMessageMetadataFiltering("authorId.equals=" + DEFAULT_AUTHOR_ID, "authorId.equals=" + UPDATED_AUTHOR_ID);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByAuthorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where authorId in
        defaultMessageMetadataFiltering("authorId.in=" + DEFAULT_AUTHOR_ID + "," + UPDATED_AUTHOR_ID, "authorId.in=" + UPDATED_AUTHOR_ID);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByAuthorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where authorId is not null
        defaultMessageMetadataFiltering("authorId.specified=true", "authorId.specified=false");
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByTerminalIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where terminalId equals to
        defaultMessageMetadataFiltering("terminalId.equals=" + DEFAULT_TERMINAL_ID, "terminalId.equals=" + UPDATED_TERMINAL_ID);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByTerminalIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where terminalId in
        defaultMessageMetadataFiltering(
            "terminalId.in=" + DEFAULT_TERMINAL_ID + "," + UPDATED_TERMINAL_ID,
            "terminalId.in=" + UPDATED_TERMINAL_ID
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByTerminalIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where terminalId is not null
        defaultMessageMetadataFiltering("terminalId.specified=true", "terminalId.specified=false");
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByTerminalIdContainsSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where terminalId contains
        defaultMessageMetadataFiltering("terminalId.contains=" + DEFAULT_TERMINAL_ID, "terminalId.contains=" + UPDATED_TERMINAL_ID);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByTerminalIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where terminalId does not contain
        defaultMessageMetadataFiltering(
            "terminalId.doesNotContain=" + UPDATED_TERMINAL_ID,
            "terminalId.doesNotContain=" + DEFAULT_TERMINAL_ID
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasBySentAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where sentAt equals to
        defaultMessageMetadataFiltering("sentAt.equals=" + DEFAULT_SENT_AT, "sentAt.equals=" + UPDATED_SENT_AT);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasBySentAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where sentAt in
        defaultMessageMetadataFiltering("sentAt.in=" + DEFAULT_SENT_AT + "," + UPDATED_SENT_AT, "sentAt.in=" + UPDATED_SENT_AT);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasBySentAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where sentAt is not null
        defaultMessageMetadataFiltering("sentAt.specified=true", "sentAt.specified=false");
    }

    @Test
    @Transactional
    void getAllMessageMetadatasBySessionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where sessionId equals to
        defaultMessageMetadataFiltering("sessionId.equals=" + DEFAULT_SESSION_ID, "sessionId.equals=" + UPDATED_SESSION_ID);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasBySessionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where sessionId in
        defaultMessageMetadataFiltering(
            "sessionId.in=" + DEFAULT_SESSION_ID + "," + UPDATED_SESSION_ID,
            "sessionId.in=" + UPDATED_SESSION_ID
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasBySessionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where sessionId is not null
        defaultMessageMetadataFiltering("sessionId.specified=true", "sessionId.specified=false");
    }

    @Test
    @Transactional
    void getAllMessageMetadatasBySessionIdContainsSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where sessionId contains
        defaultMessageMetadataFiltering("sessionId.contains=" + DEFAULT_SESSION_ID, "sessionId.contains=" + UPDATED_SESSION_ID);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasBySessionIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where sessionId does not contain
        defaultMessageMetadataFiltering("sessionId.doesNotContain=" + UPDATED_SESSION_ID, "sessionId.doesNotContain=" + DEFAULT_SESSION_ID);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByRatchetIndexIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ratchetIndex equals to
        defaultMessageMetadataFiltering("ratchetIndex.equals=" + DEFAULT_RATCHET_INDEX, "ratchetIndex.equals=" + UPDATED_RATCHET_INDEX);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByRatchetIndexIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ratchetIndex in
        defaultMessageMetadataFiltering(
            "ratchetIndex.in=" + DEFAULT_RATCHET_INDEX + "," + UPDATED_RATCHET_INDEX,
            "ratchetIndex.in=" + UPDATED_RATCHET_INDEX
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByRatchetIndexIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ratchetIndex is not null
        defaultMessageMetadataFiltering("ratchetIndex.specified=true", "ratchetIndex.specified=false");
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByRatchetIndexIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ratchetIndex is greater than or equal to
        defaultMessageMetadataFiltering(
            "ratchetIndex.greaterThanOrEqual=" + DEFAULT_RATCHET_INDEX,
            "ratchetIndex.greaterThanOrEqual=" + UPDATED_RATCHET_INDEX
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByRatchetIndexIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ratchetIndex is less than or equal to
        defaultMessageMetadataFiltering(
            "ratchetIndex.lessThanOrEqual=" + DEFAULT_RATCHET_INDEX,
            "ratchetIndex.lessThanOrEqual=" + SMALLER_RATCHET_INDEX
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByRatchetIndexIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ratchetIndex is less than
        defaultMessageMetadataFiltering("ratchetIndex.lessThan=" + UPDATED_RATCHET_INDEX, "ratchetIndex.lessThan=" + DEFAULT_RATCHET_INDEX);
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByRatchetIndexIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ratchetIndex is greater than
        defaultMessageMetadataFiltering(
            "ratchetIndex.greaterThan=" + SMALLER_RATCHET_INDEX,
            "ratchetIndex.greaterThan=" + DEFAULT_RATCHET_INDEX
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByCiphertextSizeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ciphertextSize equals to
        defaultMessageMetadataFiltering(
            "ciphertextSize.equals=" + DEFAULT_CIPHERTEXT_SIZE,
            "ciphertextSize.equals=" + UPDATED_CIPHERTEXT_SIZE
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByCiphertextSizeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ciphertextSize in
        defaultMessageMetadataFiltering(
            "ciphertextSize.in=" + DEFAULT_CIPHERTEXT_SIZE + "," + UPDATED_CIPHERTEXT_SIZE,
            "ciphertextSize.in=" + UPDATED_CIPHERTEXT_SIZE
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByCiphertextSizeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ciphertextSize is not null
        defaultMessageMetadataFiltering("ciphertextSize.specified=true", "ciphertextSize.specified=false");
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByCiphertextSizeIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ciphertextSize is greater than or equal to
        defaultMessageMetadataFiltering(
            "ciphertextSize.greaterThanOrEqual=" + DEFAULT_CIPHERTEXT_SIZE,
            "ciphertextSize.greaterThanOrEqual=" + UPDATED_CIPHERTEXT_SIZE
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByCiphertextSizeIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ciphertextSize is less than or equal to
        defaultMessageMetadataFiltering(
            "ciphertextSize.lessThanOrEqual=" + DEFAULT_CIPHERTEXT_SIZE,
            "ciphertextSize.lessThanOrEqual=" + SMALLER_CIPHERTEXT_SIZE
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByCiphertextSizeIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ciphertextSize is less than
        defaultMessageMetadataFiltering(
            "ciphertextSize.lessThan=" + UPDATED_CIPHERTEXT_SIZE,
            "ciphertextSize.lessThan=" + DEFAULT_CIPHERTEXT_SIZE
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByCiphertextSizeIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where ciphertextSize is greater than
        defaultMessageMetadataFiltering(
            "ciphertextSize.greaterThan=" + SMALLER_CIPHERTEXT_SIZE,
            "ciphertextSize.greaterThan=" + DEFAULT_CIPHERTEXT_SIZE
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByAttachmentRefsIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where attachmentRefs equals to
        defaultMessageMetadataFiltering(
            "attachmentRefs.equals=" + DEFAULT_ATTACHMENT_REFS,
            "attachmentRefs.equals=" + UPDATED_ATTACHMENT_REFS
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByAttachmentRefsIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where attachmentRefs in
        defaultMessageMetadataFiltering(
            "attachmentRefs.in=" + DEFAULT_ATTACHMENT_REFS + "," + UPDATED_ATTACHMENT_REFS,
            "attachmentRefs.in=" + UPDATED_ATTACHMENT_REFS
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByAttachmentRefsIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where attachmentRefs is not null
        defaultMessageMetadataFiltering("attachmentRefs.specified=true", "attachmentRefs.specified=false");
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByAttachmentRefsContainsSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where attachmentRefs contains
        defaultMessageMetadataFiltering(
            "attachmentRefs.contains=" + DEFAULT_ATTACHMENT_REFS,
            "attachmentRefs.contains=" + UPDATED_ATTACHMENT_REFS
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByAttachmentRefsNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMessageMetadata = messageMetadataRepository.saveAndFlush(messageMetadata);

        // Get all the messageMetadataList where attachmentRefs does not contain
        defaultMessageMetadataFiltering(
            "attachmentRefs.doesNotContain=" + UPDATED_ATTACHMENT_REFS,
            "attachmentRefs.doesNotContain=" + DEFAULT_ATTACHMENT_REFS
        );
    }

    @Test
    @Transactional
    void getAllMessageMetadatasByRoomIsEqualToSomething() throws Exception {
        Room room;
        if (TestUtil.findAll(em, Room.class).isEmpty()) {
            messageMetadataRepository.saveAndFlush(messageMetadata);
            room = RoomResourceIT.createEntity();
        } else {
            room = TestUtil.findAll(em, Room.class).getFirst();
        }
        em.persist(room);
        em.flush();
        messageMetadata.setRoom(room);
        messageMetadataRepository.saveAndFlush(messageMetadata);
        UUID roomId = room.getId();
        // Get all the messageMetadataList where room equals to roomId
        defaultMessageMetadataShouldBeFound("roomId.equals=" + roomId);

        // Get all the messageMetadataList where room equals to UUID.randomUUID()
        defaultMessageMetadataShouldNotBeFound("roomId.equals=" + UUID.randomUUID());
    }

    private void defaultMessageMetadataFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultMessageMetadataShouldBeFound(shouldBeFound);
        defaultMessageMetadataShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultMessageMetadataShouldBeFound(String filter) throws Exception {
        restMessageMetadataMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(messageMetadata.getId().toString())))
            .andExpect(jsonPath("$.[*].matrixEventId").value(hasItem(DEFAULT_MATRIX_EVENT_ID)))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].terminalId").value(hasItem(DEFAULT_TERMINAL_ID)))
            .andExpect(jsonPath("$.[*].sentAt").value(hasItem(DEFAULT_SENT_AT.toString())))
            .andExpect(jsonPath("$.[*].sessionId").value(hasItem(DEFAULT_SESSION_ID)))
            .andExpect(jsonPath("$.[*].ratchetIndex").value(hasItem(DEFAULT_RATCHET_INDEX.intValue())))
            .andExpect(jsonPath("$.[*].ciphertextSize").value(hasItem(DEFAULT_CIPHERTEXT_SIZE)))
            .andExpect(jsonPath("$.[*].attachmentRefs").value(hasItem(DEFAULT_ATTACHMENT_REFS)));

        // Check, that the count call also returns 1
        restMessageMetadataMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultMessageMetadataShouldNotBeFound(String filter) throws Exception {
        restMessageMetadataMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restMessageMetadataMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingMessageMetadata() throws Exception {
        // Get the messageMetadata
        restMessageMetadataMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return messageMetadataRepository.count();
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

    protected MessageMetadata getPersistedMessageMetadata(MessageMetadata messageMetadata) {
        return messageMetadataRepository.findById(messageMetadata.getId()).orElseThrow();
    }

    protected void assertPersistedMessageMetadataToMatchAllProperties(MessageMetadata expectedMessageMetadata) {
        assertMessageMetadataAllPropertiesEquals(expectedMessageMetadata, getPersistedMessageMetadata(expectedMessageMetadata));
    }

    protected void assertPersistedMessageMetadataToMatchUpdatableProperties(MessageMetadata expectedMessageMetadata) {
        assertMessageMetadataAllUpdatablePropertiesEquals(expectedMessageMetadata, getPersistedMessageMetadata(expectedMessageMetadata));
    }
}
