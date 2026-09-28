package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.EvidenceItemAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.CommissioningFile;
import cm.gimac.des.certification.domain.EvidenceItem;
import cm.gimac.des.certification.domain.enumeration.EvidenceKind;
import cm.gimac.des.certification.repository.EvidenceItemRepository;
import cm.gimac.des.certification.service.EvidenceItemService;
import cm.gimac.des.certification.service.mapper.EvidenceItemMapper;
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
 * Integration tests for the {@link EvidenceItemResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class EvidenceItemResourceIT {

    private static final UUID DEFAULT_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_ID = UUID.randomUUID();

    private static final EvidenceKind DEFAULT_KIND = EvidenceKind.SOCLE_EVALUATION;
    private static final EvidenceKind UPDATED_KIND = EvidenceKind.DECISION_QUITUS;

    private static final String DEFAULT_DIGEST = "10b2c1a7a2de99bffe033c7b14e7c207210d46d2c8592bc07a07ede3e46e0155";
    private static final String UPDATED_DIGEST = "baa271d8f48bc9c65b9b236a32ad5a11b4c8c2f20dcb70210e65be92e75bc91f";

    private static final Instant DEFAULT_SEALED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SEALED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_SEAL_CONFIRMED = false;
    private static final Boolean UPDATED_SEAL_CONFIRMED = true;

    private static final Instant DEFAULT_LAST_VERIFIED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_VERIFIED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_ATTACHED_BY = UUID.randomUUID();
    private static final UUID UPDATED_ATTACHED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_ATTACHED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ATTACHED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/evidence-items";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EvidenceItemRepository evidenceItemRepository;

    @Mock
    private EvidenceItemRepository evidenceItemRepositoryMock;

    @Autowired
    private EvidenceItemMapper evidenceItemMapper;

    @Mock
    private EvidenceItemService evidenceItemServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEvidenceItemMockMvc;

    private EvidenceItem evidenceItem;

    private EvidenceItem insertedEvidenceItem;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EvidenceItem createEntity(EntityManager em) {
        EvidenceItem evidenceItem = new EvidenceItem()
            .documentId(DEFAULT_DOCUMENT_ID)
            .kind(DEFAULT_KIND)
            .digest(DEFAULT_DIGEST)
            .sealedAt(DEFAULT_SEALED_AT)
            .sealConfirmed(DEFAULT_SEAL_CONFIRMED)
            .lastVerifiedAt(DEFAULT_LAST_VERIFIED_AT)
            .attachedBy(DEFAULT_ATTACHED_BY)
            .attachedAt(DEFAULT_ATTACHED_AT);
        // Add required entity
        CommissioningFile commissioningFile;
        if (TestUtil.findAll(em, CommissioningFile.class).isEmpty()) {
            commissioningFile = CommissioningFileResourceIT.createEntity();
            em.persist(commissioningFile);
            em.flush();
        } else {
            commissioningFile = TestUtil.findAll(em, CommissioningFile.class).get(0);
        }
        evidenceItem.setFile(commissioningFile);
        return evidenceItem;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EvidenceItem createUpdatedEntity(EntityManager em) {
        EvidenceItem updatedEvidenceItem = new EvidenceItem()
            .documentId(UPDATED_DOCUMENT_ID)
            .kind(UPDATED_KIND)
            .digest(UPDATED_DIGEST)
            .sealedAt(UPDATED_SEALED_AT)
            .sealConfirmed(UPDATED_SEAL_CONFIRMED)
            .lastVerifiedAt(UPDATED_LAST_VERIFIED_AT)
            .attachedBy(UPDATED_ATTACHED_BY)
            .attachedAt(UPDATED_ATTACHED_AT);
        // Add required entity
        CommissioningFile commissioningFile;
        if (TestUtil.findAll(em, CommissioningFile.class).isEmpty()) {
            commissioningFile = CommissioningFileResourceIT.createUpdatedEntity();
            em.persist(commissioningFile);
            em.flush();
        } else {
            commissioningFile = TestUtil.findAll(em, CommissioningFile.class).get(0);
        }
        updatedEvidenceItem.setFile(commissioningFile);
        return updatedEvidenceItem;
    }

    @BeforeEach
    void initTest() {
        evidenceItem = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedEvidenceItem != null) {
            evidenceItemRepository.delete(insertedEvidenceItem);
            insertedEvidenceItem = null;
        }
    }

    @Test
    @Transactional
    void getAllEvidenceItems() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList
        restEvidenceItemMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(evidenceItem.getId().toString())))
            .andExpect(jsonPath("$.[*].documentId").value(hasItem(DEFAULT_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].digest").value(hasItem(DEFAULT_DIGEST)))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].sealConfirmed").value(hasItem(DEFAULT_SEAL_CONFIRMED)))
            .andExpect(jsonPath("$.[*].lastVerifiedAt").value(hasItem(DEFAULT_LAST_VERIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].attachedBy").value(hasItem(DEFAULT_ATTACHED_BY.toString())))
            .andExpect(jsonPath("$.[*].attachedAt").value(hasItem(DEFAULT_ATTACHED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEvidenceItemsWithEagerRelationshipsIsEnabled() throws Exception {
        when(evidenceItemServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEvidenceItemMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(evidenceItemServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEvidenceItemsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(evidenceItemServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEvidenceItemMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(evidenceItemRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getEvidenceItem() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get the evidenceItem
        restEvidenceItemMockMvc
            .perform(get(ENTITY_API_URL_ID, evidenceItem.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(evidenceItem.getId().toString()))
            .andExpect(jsonPath("$.documentId").value(DEFAULT_DOCUMENT_ID.toString()))
            .andExpect(jsonPath("$.kind").value(DEFAULT_KIND.toString()))
            .andExpect(jsonPath("$.digest").value(DEFAULT_DIGEST))
            .andExpect(jsonPath("$.sealedAt").value(DEFAULT_SEALED_AT.toString()))
            .andExpect(jsonPath("$.sealConfirmed").value(DEFAULT_SEAL_CONFIRMED))
            .andExpect(jsonPath("$.lastVerifiedAt").value(DEFAULT_LAST_VERIFIED_AT.toString()))
            .andExpect(jsonPath("$.attachedBy").value(DEFAULT_ATTACHED_BY.toString()))
            .andExpect(jsonPath("$.attachedAt").value(DEFAULT_ATTACHED_AT.toString()));
    }

    @Test
    @Transactional
    void getEvidenceItemsByIdFiltering() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        UUID id = evidenceItem.getId();

        defaultEvidenceItemFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByDocumentIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where documentId equals to
        defaultEvidenceItemFiltering("documentId.equals=" + DEFAULT_DOCUMENT_ID, "documentId.equals=" + UPDATED_DOCUMENT_ID);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByDocumentIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where documentId in
        defaultEvidenceItemFiltering(
            "documentId.in=" + DEFAULT_DOCUMENT_ID + "," + UPDATED_DOCUMENT_ID,
            "documentId.in=" + UPDATED_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByDocumentIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where documentId is not null
        defaultEvidenceItemFiltering("documentId.specified=true", "documentId.specified=false");
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByKindIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where kind equals to
        defaultEvidenceItemFiltering("kind.equals=" + DEFAULT_KIND, "kind.equals=" + UPDATED_KIND);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByKindIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where kind in
        defaultEvidenceItemFiltering("kind.in=" + DEFAULT_KIND + "," + UPDATED_KIND, "kind.in=" + UPDATED_KIND);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByKindIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where kind is not null
        defaultEvidenceItemFiltering("kind.specified=true", "kind.specified=false");
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where digest equals to
        defaultEvidenceItemFiltering("digest.equals=" + DEFAULT_DIGEST, "digest.equals=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where digest in
        defaultEvidenceItemFiltering("digest.in=" + DEFAULT_DIGEST + "," + UPDATED_DIGEST, "digest.in=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where digest is not null
        defaultEvidenceItemFiltering("digest.specified=true", "digest.specified=false");
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where digest contains
        defaultEvidenceItemFiltering("digest.contains=" + DEFAULT_DIGEST, "digest.contains=" + UPDATED_DIGEST);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where digest does not contain
        defaultEvidenceItemFiltering("digest.doesNotContain=" + UPDATED_DIGEST, "digest.doesNotContain=" + DEFAULT_DIGEST);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsBySealedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where sealedAt equals to
        defaultEvidenceItemFiltering("sealedAt.equals=" + DEFAULT_SEALED_AT, "sealedAt.equals=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsBySealedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where sealedAt in
        defaultEvidenceItemFiltering("sealedAt.in=" + DEFAULT_SEALED_AT + "," + UPDATED_SEALED_AT, "sealedAt.in=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsBySealedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where sealedAt is not null
        defaultEvidenceItemFiltering("sealedAt.specified=true", "sealedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllEvidenceItemsBySealConfirmedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where sealConfirmed equals to
        defaultEvidenceItemFiltering("sealConfirmed.equals=" + DEFAULT_SEAL_CONFIRMED, "sealConfirmed.equals=" + UPDATED_SEAL_CONFIRMED);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsBySealConfirmedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where sealConfirmed in
        defaultEvidenceItemFiltering(
            "sealConfirmed.in=" + DEFAULT_SEAL_CONFIRMED + "," + UPDATED_SEAL_CONFIRMED,
            "sealConfirmed.in=" + UPDATED_SEAL_CONFIRMED
        );
    }

    @Test
    @Transactional
    void getAllEvidenceItemsBySealConfirmedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where sealConfirmed is not null
        defaultEvidenceItemFiltering("sealConfirmed.specified=true", "sealConfirmed.specified=false");
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByLastVerifiedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where lastVerifiedAt equals to
        defaultEvidenceItemFiltering(
            "lastVerifiedAt.equals=" + DEFAULT_LAST_VERIFIED_AT,
            "lastVerifiedAt.equals=" + UPDATED_LAST_VERIFIED_AT
        );
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByLastVerifiedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where lastVerifiedAt in
        defaultEvidenceItemFiltering(
            "lastVerifiedAt.in=" + DEFAULT_LAST_VERIFIED_AT + "," + UPDATED_LAST_VERIFIED_AT,
            "lastVerifiedAt.in=" + UPDATED_LAST_VERIFIED_AT
        );
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByLastVerifiedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where lastVerifiedAt is not null
        defaultEvidenceItemFiltering("lastVerifiedAt.specified=true", "lastVerifiedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByAttachedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where attachedBy equals to
        defaultEvidenceItemFiltering("attachedBy.equals=" + DEFAULT_ATTACHED_BY, "attachedBy.equals=" + UPDATED_ATTACHED_BY);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByAttachedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where attachedBy in
        defaultEvidenceItemFiltering(
            "attachedBy.in=" + DEFAULT_ATTACHED_BY + "," + UPDATED_ATTACHED_BY,
            "attachedBy.in=" + UPDATED_ATTACHED_BY
        );
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByAttachedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where attachedBy is not null
        defaultEvidenceItemFiltering("attachedBy.specified=true", "attachedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByAttachedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where attachedAt equals to
        defaultEvidenceItemFiltering("attachedAt.equals=" + DEFAULT_ATTACHED_AT, "attachedAt.equals=" + UPDATED_ATTACHED_AT);
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByAttachedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where attachedAt in
        defaultEvidenceItemFiltering(
            "attachedAt.in=" + DEFAULT_ATTACHED_AT + "," + UPDATED_ATTACHED_AT,
            "attachedAt.in=" + UPDATED_ATTACHED_AT
        );
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByAttachedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvidenceItem = evidenceItemRepository.saveAndFlush(evidenceItem);

        // Get all the evidenceItemList where attachedAt is not null
        defaultEvidenceItemFiltering("attachedAt.specified=true", "attachedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllEvidenceItemsByFileIsEqualToSomething() throws Exception {
        CommissioningFile file;
        if (TestUtil.findAll(em, CommissioningFile.class).isEmpty()) {
            evidenceItemRepository.saveAndFlush(evidenceItem);
            file = CommissioningFileResourceIT.createEntity();
        } else {
            file = TestUtil.findAll(em, CommissioningFile.class).get(0);
        }
        em.persist(file);
        em.flush();
        evidenceItem.setFile(file);
        evidenceItemRepository.saveAndFlush(evidenceItem);
        UUID fileId = file.getId();
        // Get all the evidenceItemList where file equals to fileId
        defaultEvidenceItemShouldBeFound("fileId.equals=" + fileId);

        // Get all the evidenceItemList where file equals to UUID.randomUUID()
        defaultEvidenceItemShouldNotBeFound("fileId.equals=" + UUID.randomUUID());
    }

    private void defaultEvidenceItemFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultEvidenceItemShouldBeFound(shouldBeFound);
        defaultEvidenceItemShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultEvidenceItemShouldBeFound(String filter) throws Exception {
        restEvidenceItemMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(evidenceItem.getId().toString())))
            .andExpect(jsonPath("$.[*].documentId").value(hasItem(DEFAULT_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].digest").value(hasItem(DEFAULT_DIGEST)))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].sealConfirmed").value(hasItem(DEFAULT_SEAL_CONFIRMED)))
            .andExpect(jsonPath("$.[*].lastVerifiedAt").value(hasItem(DEFAULT_LAST_VERIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].attachedBy").value(hasItem(DEFAULT_ATTACHED_BY.toString())))
            .andExpect(jsonPath("$.[*].attachedAt").value(hasItem(DEFAULT_ATTACHED_AT.toString())));

        // Check, that the count call also returns 1
        restEvidenceItemMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultEvidenceItemShouldNotBeFound(String filter) throws Exception {
        restEvidenceItemMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restEvidenceItemMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingEvidenceItem() throws Exception {
        // Get the evidenceItem
        restEvidenceItemMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return evidenceItemRepository.count();
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

    protected EvidenceItem getPersistedEvidenceItem(EvidenceItem evidenceItem) {
        return evidenceItemRepository.findById(evidenceItem.getId()).orElseThrow();
    }

    protected void assertPersistedEvidenceItemToMatchAllProperties(EvidenceItem expectedEvidenceItem) {
        assertEvidenceItemAllPropertiesEquals(expectedEvidenceItem, getPersistedEvidenceItem(expectedEvidenceItem));
    }

    protected void assertPersistedEvidenceItemToMatchUpdatableProperties(EvidenceItem expectedEvidenceItem) {
        assertEvidenceItemAllUpdatablePropertiesEquals(expectedEvidenceItem, getPersistedEvidenceItem(expectedEvidenceItem));
    }
}
