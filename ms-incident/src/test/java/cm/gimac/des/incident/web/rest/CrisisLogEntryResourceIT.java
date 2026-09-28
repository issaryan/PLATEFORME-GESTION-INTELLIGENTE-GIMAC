package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.CrisisLogEntryAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.CrisisCell;
import cm.gimac.des.incident.domain.CrisisLogEntry;
import cm.gimac.des.incident.domain.enumeration.CrisisLogNature;
import cm.gimac.des.incident.repository.CrisisLogEntryRepository;
import cm.gimac.des.incident.service.CrisisLogEntryService;
import cm.gimac.des.incident.service.mapper.CrisisLogEntryMapper;
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
 * Integration tests for the {@link CrisisLogEntryResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class CrisisLogEntryResourceIT {

    private static final Long DEFAULT_CHAIN_INDEX = 1L;
    private static final Long UPDATED_CHAIN_INDEX = 2L;
    private static final Long SMALLER_CHAIN_INDEX = 1L - 1L;

    private static final Instant DEFAULT_OCCURRED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OCCURRED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_AUTHOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_AUTHOR_ID = UUID.randomUUID();

    private static final CrisisLogNature DEFAULT_NATURE = CrisisLogNature.PRESENCE;
    private static final CrisisLogNature UPDATED_NATURE = CrisisLogNature.HYPOTHESE;

    private static final String DEFAULT_TEXT = "AAAAAAAAAA";
    private static final String UPDATED_TEXT = "BBBBBBBBBB";

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_PREVIOUS_HASH = "7cbfbf706dda6cc57c71781ec9be4a9c824db9d5f76c1e564f88097f464c478d";
    private static final String UPDATED_PREVIOUS_HASH = "3db1caeb3e8e9e7ced94f926f7fb156e51e7be05b703edd15f1e07f871c450d7";

    private static final String DEFAULT_HASH = "0a7a135c020a8718a581690a436227e07ea8adcbd50f293c32f23f17673f660b";
    private static final String UPDATED_HASH = "ac1bf23c00d3c49e5bf5cded228981c8d6db746ef3223a4ec76d3f1607ab2bfb";

    private static final String ENTITY_API_URL = "/api/crisis-log-entries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CrisisLogEntryRepository crisisLogEntryRepository;

    @Mock
    private CrisisLogEntryRepository crisisLogEntryRepositoryMock;

    @Autowired
    private CrisisLogEntryMapper crisisLogEntryMapper;

    @Mock
    private CrisisLogEntryService crisisLogEntryServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCrisisLogEntryMockMvc;

    private CrisisLogEntry crisisLogEntry;

    private CrisisLogEntry insertedCrisisLogEntry;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CrisisLogEntry createEntity(EntityManager em) {
        CrisisLogEntry crisisLogEntry = new CrisisLogEntry()
            .chainIndex(DEFAULT_CHAIN_INDEX)
            .occurredAt(DEFAULT_OCCURRED_AT)
            .authorId(DEFAULT_AUTHOR_ID)
            .nature(DEFAULT_NATURE)
            .text(DEFAULT_TEXT)
            .reference(DEFAULT_REFERENCE)
            .previousHash(DEFAULT_PREVIOUS_HASH)
            .hash(DEFAULT_HASH);
        // Add required entity
        CrisisCell crisisCell;
        if (TestUtil.findAll(em, CrisisCell.class).isEmpty()) {
            crisisCell = CrisisCellResourceIT.createEntity(em);
            em.persist(crisisCell);
            em.flush();
        } else {
            crisisCell = TestUtil.findAll(em, CrisisCell.class).get(0);
        }
        crisisLogEntry.setCell(crisisCell);
        return crisisLogEntry;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CrisisLogEntry createUpdatedEntity(EntityManager em) {
        CrisisLogEntry updatedCrisisLogEntry = new CrisisLogEntry()
            .chainIndex(UPDATED_CHAIN_INDEX)
            .occurredAt(UPDATED_OCCURRED_AT)
            .authorId(UPDATED_AUTHOR_ID)
            .nature(UPDATED_NATURE)
            .text(UPDATED_TEXT)
            .reference(UPDATED_REFERENCE)
            .previousHash(UPDATED_PREVIOUS_HASH)
            .hash(UPDATED_HASH);
        // Add required entity
        CrisisCell crisisCell;
        if (TestUtil.findAll(em, CrisisCell.class).isEmpty()) {
            crisisCell = CrisisCellResourceIT.createUpdatedEntity(em);
            em.persist(crisisCell);
            em.flush();
        } else {
            crisisCell = TestUtil.findAll(em, CrisisCell.class).get(0);
        }
        updatedCrisisLogEntry.setCell(crisisCell);
        return updatedCrisisLogEntry;
    }

    @BeforeEach
    void initTest() {
        crisisLogEntry = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedCrisisLogEntry != null) {
            crisisLogEntryRepository.delete(insertedCrisisLogEntry);
            insertedCrisisLogEntry = null;
        }
    }

    @Test
    @Transactional
    void getAllCrisisLogEntries() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList
        restCrisisLogEntryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(crisisLogEntry.getId().toString())))
            .andExpect(jsonPath("$.[*].chainIndex").value(hasItem(DEFAULT_CHAIN_INDEX.intValue())))
            .andExpect(jsonPath("$.[*].occurredAt").value(hasItem(DEFAULT_OCCURRED_AT.toString())))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].nature").value(hasItem(DEFAULT_NATURE.toString())))
            .andExpect(jsonPath("$.[*].text").value(hasItem(DEFAULT_TEXT)))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].previousHash").value(hasItem(DEFAULT_PREVIOUS_HASH)))
            .andExpect(jsonPath("$.[*].hash").value(hasItem(DEFAULT_HASH)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCrisisLogEntriesWithEagerRelationshipsIsEnabled() throws Exception {
        when(crisisLogEntryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCrisisLogEntryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(crisisLogEntryServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCrisisLogEntriesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(crisisLogEntryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCrisisLogEntryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(crisisLogEntryRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getCrisisLogEntry() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get the crisisLogEntry
        restCrisisLogEntryMockMvc
            .perform(get(ENTITY_API_URL_ID, crisisLogEntry.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(crisisLogEntry.getId().toString()))
            .andExpect(jsonPath("$.chainIndex").value(DEFAULT_CHAIN_INDEX.intValue()))
            .andExpect(jsonPath("$.occurredAt").value(DEFAULT_OCCURRED_AT.toString()))
            .andExpect(jsonPath("$.authorId").value(DEFAULT_AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.nature").value(DEFAULT_NATURE.toString()))
            .andExpect(jsonPath("$.text").value(DEFAULT_TEXT))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.previousHash").value(DEFAULT_PREVIOUS_HASH))
            .andExpect(jsonPath("$.hash").value(DEFAULT_HASH));
    }

    @Test
    @Transactional
    void getCrisisLogEntriesByIdFiltering() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        UUID id = crisisLogEntry.getId();

        defaultCrisisLogEntryFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByChainIndexIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where chainIndex equals to
        defaultCrisisLogEntryFiltering("chainIndex.equals=" + DEFAULT_CHAIN_INDEX, "chainIndex.equals=" + UPDATED_CHAIN_INDEX);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByChainIndexIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where chainIndex in
        defaultCrisisLogEntryFiltering(
            "chainIndex.in=" + DEFAULT_CHAIN_INDEX + "," + UPDATED_CHAIN_INDEX,
            "chainIndex.in=" + UPDATED_CHAIN_INDEX
        );
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByChainIndexIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where chainIndex is not null
        defaultCrisisLogEntryFiltering("chainIndex.specified=true", "chainIndex.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByChainIndexIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where chainIndex is greater than or equal to
        defaultCrisisLogEntryFiltering(
            "chainIndex.greaterThanOrEqual=" + DEFAULT_CHAIN_INDEX,
            "chainIndex.greaterThanOrEqual=" + UPDATED_CHAIN_INDEX
        );
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByChainIndexIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where chainIndex is less than or equal to
        defaultCrisisLogEntryFiltering(
            "chainIndex.lessThanOrEqual=" + DEFAULT_CHAIN_INDEX,
            "chainIndex.lessThanOrEqual=" + SMALLER_CHAIN_INDEX
        );
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByChainIndexIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where chainIndex is less than
        defaultCrisisLogEntryFiltering("chainIndex.lessThan=" + UPDATED_CHAIN_INDEX, "chainIndex.lessThan=" + DEFAULT_CHAIN_INDEX);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByChainIndexIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where chainIndex is greater than
        defaultCrisisLogEntryFiltering("chainIndex.greaterThan=" + SMALLER_CHAIN_INDEX, "chainIndex.greaterThan=" + DEFAULT_CHAIN_INDEX);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByOccurredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where occurredAt equals to
        defaultCrisisLogEntryFiltering("occurredAt.equals=" + DEFAULT_OCCURRED_AT, "occurredAt.equals=" + UPDATED_OCCURRED_AT);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByOccurredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where occurredAt in
        defaultCrisisLogEntryFiltering(
            "occurredAt.in=" + DEFAULT_OCCURRED_AT + "," + UPDATED_OCCURRED_AT,
            "occurredAt.in=" + UPDATED_OCCURRED_AT
        );
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByOccurredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where occurredAt is not null
        defaultCrisisLogEntryFiltering("occurredAt.specified=true", "occurredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByAuthorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where authorId equals to
        defaultCrisisLogEntryFiltering("authorId.equals=" + DEFAULT_AUTHOR_ID, "authorId.equals=" + UPDATED_AUTHOR_ID);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByAuthorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where authorId in
        defaultCrisisLogEntryFiltering("authorId.in=" + DEFAULT_AUTHOR_ID + "," + UPDATED_AUTHOR_ID, "authorId.in=" + UPDATED_AUTHOR_ID);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByAuthorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where authorId is not null
        defaultCrisisLogEntryFiltering("authorId.specified=true", "authorId.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByNatureIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where nature equals to
        defaultCrisisLogEntryFiltering("nature.equals=" + DEFAULT_NATURE, "nature.equals=" + UPDATED_NATURE);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByNatureIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where nature in
        defaultCrisisLogEntryFiltering("nature.in=" + DEFAULT_NATURE + "," + UPDATED_NATURE, "nature.in=" + UPDATED_NATURE);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByNatureIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where nature is not null
        defaultCrisisLogEntryFiltering("nature.specified=true", "nature.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByTextIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where text equals to
        defaultCrisisLogEntryFiltering("text.equals=" + DEFAULT_TEXT, "text.equals=" + UPDATED_TEXT);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByTextIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where text in
        defaultCrisisLogEntryFiltering("text.in=" + DEFAULT_TEXT + "," + UPDATED_TEXT, "text.in=" + UPDATED_TEXT);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByTextIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where text is not null
        defaultCrisisLogEntryFiltering("text.specified=true", "text.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByTextContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where text contains
        defaultCrisisLogEntryFiltering("text.contains=" + DEFAULT_TEXT, "text.contains=" + UPDATED_TEXT);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByTextNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where text does not contain
        defaultCrisisLogEntryFiltering("text.doesNotContain=" + UPDATED_TEXT, "text.doesNotContain=" + DEFAULT_TEXT);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where reference equals to
        defaultCrisisLogEntryFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where reference in
        defaultCrisisLogEntryFiltering("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE, "reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where reference is not null
        defaultCrisisLogEntryFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where reference contains
        defaultCrisisLogEntryFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where reference does not contain
        defaultCrisisLogEntryFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByPreviousHashIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where previousHash equals to
        defaultCrisisLogEntryFiltering("previousHash.equals=" + DEFAULT_PREVIOUS_HASH, "previousHash.equals=" + UPDATED_PREVIOUS_HASH);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByPreviousHashIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where previousHash in
        defaultCrisisLogEntryFiltering(
            "previousHash.in=" + DEFAULT_PREVIOUS_HASH + "," + UPDATED_PREVIOUS_HASH,
            "previousHash.in=" + UPDATED_PREVIOUS_HASH
        );
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByPreviousHashIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where previousHash is not null
        defaultCrisisLogEntryFiltering("previousHash.specified=true", "previousHash.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByPreviousHashContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where previousHash contains
        defaultCrisisLogEntryFiltering("previousHash.contains=" + DEFAULT_PREVIOUS_HASH, "previousHash.contains=" + UPDATED_PREVIOUS_HASH);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByPreviousHashNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where previousHash does not contain
        defaultCrisisLogEntryFiltering(
            "previousHash.doesNotContain=" + UPDATED_PREVIOUS_HASH,
            "previousHash.doesNotContain=" + DEFAULT_PREVIOUS_HASH
        );
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByHashIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where hash equals to
        defaultCrisisLogEntryFiltering("hash.equals=" + DEFAULT_HASH, "hash.equals=" + UPDATED_HASH);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByHashIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where hash in
        defaultCrisisLogEntryFiltering("hash.in=" + DEFAULT_HASH + "," + UPDATED_HASH, "hash.in=" + UPDATED_HASH);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByHashIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where hash is not null
        defaultCrisisLogEntryFiltering("hash.specified=true", "hash.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByHashContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where hash contains
        defaultCrisisLogEntryFiltering("hash.contains=" + DEFAULT_HASH, "hash.contains=" + UPDATED_HASH);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByHashNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisLogEntry = crisisLogEntryRepository.saveAndFlush(crisisLogEntry);

        // Get all the crisisLogEntryList where hash does not contain
        defaultCrisisLogEntryFiltering("hash.doesNotContain=" + UPDATED_HASH, "hash.doesNotContain=" + DEFAULT_HASH);
    }

    @Test
    @Transactional
    void getAllCrisisLogEntriesByCellIsEqualToSomething() throws Exception {
        CrisisCell cell;
        if (TestUtil.findAll(em, CrisisCell.class).isEmpty()) {
            crisisLogEntryRepository.saveAndFlush(crisisLogEntry);
            cell = CrisisCellResourceIT.createEntity(em);
        } else {
            cell = TestUtil.findAll(em, CrisisCell.class).get(0);
        }
        em.persist(cell);
        em.flush();
        crisisLogEntry.setCell(cell);
        crisisLogEntryRepository.saveAndFlush(crisisLogEntry);
        UUID cellId = cell.getId();
        // Get all the crisisLogEntryList where cell equals to cellId
        defaultCrisisLogEntryShouldBeFound("cellId.equals=" + cellId);

        // Get all the crisisLogEntryList where cell equals to UUID.randomUUID()
        defaultCrisisLogEntryShouldNotBeFound("cellId.equals=" + UUID.randomUUID());
    }

    private void defaultCrisisLogEntryFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultCrisisLogEntryShouldBeFound(shouldBeFound);
        defaultCrisisLogEntryShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultCrisisLogEntryShouldBeFound(String filter) throws Exception {
        restCrisisLogEntryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(crisisLogEntry.getId().toString())))
            .andExpect(jsonPath("$.[*].chainIndex").value(hasItem(DEFAULT_CHAIN_INDEX.intValue())))
            .andExpect(jsonPath("$.[*].occurredAt").value(hasItem(DEFAULT_OCCURRED_AT.toString())))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].nature").value(hasItem(DEFAULT_NATURE.toString())))
            .andExpect(jsonPath("$.[*].text").value(hasItem(DEFAULT_TEXT)))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].previousHash").value(hasItem(DEFAULT_PREVIOUS_HASH)))
            .andExpect(jsonPath("$.[*].hash").value(hasItem(DEFAULT_HASH)));

        // Check, that the count call also returns 1
        restCrisisLogEntryMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultCrisisLogEntryShouldNotBeFound(String filter) throws Exception {
        restCrisisLogEntryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCrisisLogEntryMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingCrisisLogEntry() throws Exception {
        // Get the crisisLogEntry
        restCrisisLogEntryMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return crisisLogEntryRepository.count();
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

    protected CrisisLogEntry getPersistedCrisisLogEntry(CrisisLogEntry crisisLogEntry) {
        return crisisLogEntryRepository.findById(crisisLogEntry.getId()).orElseThrow();
    }

    protected void assertPersistedCrisisLogEntryToMatchAllProperties(CrisisLogEntry expectedCrisisLogEntry) {
        assertCrisisLogEntryAllPropertiesEquals(expectedCrisisLogEntry, getPersistedCrisisLogEntry(expectedCrisisLogEntry));
    }

    protected void assertPersistedCrisisLogEntryToMatchUpdatableProperties(CrisisLogEntry expectedCrisisLogEntry) {
        assertCrisisLogEntryAllUpdatablePropertiesEquals(expectedCrisisLogEntry, getPersistedCrisisLogEntry(expectedCrisisLogEntry));
    }
}
