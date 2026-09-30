package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.MessageReceiptAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.MessageMetadata;
import cm.gimac.des.collaboration.domain.MessageReceipt;
import cm.gimac.des.collaboration.domain.enumeration.ReceiptKind;
import cm.gimac.des.collaboration.repository.MessageReceiptRepository;
import cm.gimac.des.collaboration.service.MessageReceiptService;
import cm.gimac.des.collaboration.service.mapper.MessageReceiptMapper;
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
 * Integration tests for the {@link MessageReceiptResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.collaboration.security.AuthoritiesConstants.ADMIN)
class MessageReceiptResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final ReceiptKind DEFAULT_KIND = ReceiptKind.REMIS;
    private static final ReceiptKind UPDATED_KIND = ReceiptKind.LU;

    private static final Instant DEFAULT_RECEIVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RECEIVED_AT = Instant.ofEpochMilli(1790746747003L);

    private static final String ENTITY_API_URL = "/api/message-receipts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MessageReceiptRepository messageReceiptRepository;

    @Mock
    private MessageReceiptRepository messageReceiptRepositoryMock;

    @Autowired
    private MessageReceiptMapper messageReceiptMapper;

    @Mock
    private MessageReceiptService messageReceiptServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMessageReceiptMockMvc;

    private MessageReceipt messageReceipt;

    private MessageReceipt insertedMessageReceipt;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MessageReceipt createEntity(EntityManager em) {
        MessageReceipt messageReceipt = new MessageReceipt().userId(DEFAULT_USER_ID).kind(DEFAULT_KIND).receivedAt(DEFAULT_RECEIVED_AT);
        // Add required entity
        MessageMetadata messageMetadata;
        if (TestUtil.findAll(em, MessageMetadata.class).isEmpty()) {
            messageMetadata = MessageMetadataResourceIT.createEntity(em);
            em.persist(messageMetadata);
            em.flush();
        } else {
            messageMetadata = TestUtil.findAll(em, MessageMetadata.class).getFirst();
        }
        messageReceipt.setMessage(messageMetadata);
        return messageReceipt;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MessageReceipt createUpdatedEntity(EntityManager em) {
        MessageReceipt updatedMessageReceipt = new MessageReceipt()
            .userId(UPDATED_USER_ID)
            .kind(UPDATED_KIND)
            .receivedAt(UPDATED_RECEIVED_AT);
        // Add required entity
        MessageMetadata messageMetadata;
        if (TestUtil.findAll(em, MessageMetadata.class).isEmpty()) {
            messageMetadata = MessageMetadataResourceIT.createUpdatedEntity(em);
            em.persist(messageMetadata);
            em.flush();
        } else {
            messageMetadata = TestUtil.findAll(em, MessageMetadata.class).getFirst();
        }
        updatedMessageReceipt.setMessage(messageMetadata);
        return updatedMessageReceipt;
    }

    @BeforeEach
    void initTest() {
        messageReceipt = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedMessageReceipt != null) {
            messageReceiptRepository.delete(insertedMessageReceipt);
            insertedMessageReceipt = null;
        }
    }

    @Test
    @Transactional
    void getAllMessageReceipts() throws Exception {
        // Initialize the database
        insertedMessageReceipt = messageReceiptRepository.saveAndFlush(messageReceipt);

        // Get all the messageReceiptList
        restMessageReceiptMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(messageReceipt.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].receivedAt").value(hasItem(DEFAULT_RECEIVED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMessageReceiptsWithEagerRelationshipsIsEnabled() throws Exception {
        when(messageReceiptServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMessageReceiptMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(messageReceiptServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMessageReceiptsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(messageReceiptServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMessageReceiptMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(messageReceiptRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getMessageReceipt() throws Exception {
        // Initialize the database
        insertedMessageReceipt = messageReceiptRepository.saveAndFlush(messageReceipt);

        // Get the messageReceipt
        restMessageReceiptMockMvc
            .perform(get(ENTITY_API_URL_ID, messageReceipt.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(messageReceipt.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.kind").value(DEFAULT_KIND.toString()))
            .andExpect(jsonPath("$.receivedAt").value(DEFAULT_RECEIVED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingMessageReceipt() throws Exception {
        // Get the messageReceipt
        restMessageReceiptMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return messageReceiptRepository.count();
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

    protected MessageReceipt getPersistedMessageReceipt(MessageReceipt messageReceipt) {
        return messageReceiptRepository.findById(messageReceipt.getId()).orElseThrow();
    }

    protected void assertPersistedMessageReceiptToMatchAllProperties(MessageReceipt expectedMessageReceipt) {
        assertMessageReceiptAllPropertiesEquals(expectedMessageReceipt, getPersistedMessageReceipt(expectedMessageReceipt));
    }

    protected void assertPersistedMessageReceiptToMatchUpdatableProperties(MessageReceipt expectedMessageReceipt) {
        assertMessageReceiptAllUpdatablePropertiesEquals(expectedMessageReceipt, getPersistedMessageReceipt(expectedMessageReceipt));
    }
}
