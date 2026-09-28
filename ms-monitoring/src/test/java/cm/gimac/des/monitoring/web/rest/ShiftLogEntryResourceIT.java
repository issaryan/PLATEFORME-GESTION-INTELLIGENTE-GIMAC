package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.ShiftLogEntryAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.ShiftLogEntry;
import cm.gimac.des.monitoring.domain.ShiftLogEntry;
import cm.gimac.des.monitoring.domain.enumeration.ShiftLogNature;
import cm.gimac.des.monitoring.repository.ShiftLogEntryRepository;
import cm.gimac.des.monitoring.service.ShiftLogEntryService;
import cm.gimac.des.monitoring.service.mapper.ShiftLogEntryMapper;
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
 * Integration tests for the {@link ShiftLogEntryResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ShiftLogEntryResourceIT {

    private static final Long DEFAULT_CHAIN_INDEX = 1L;
    private static final Long UPDATED_CHAIN_INDEX = 2L;
    private static final Long SMALLER_CHAIN_INDEX = 1L - 1L;

    private static final Instant DEFAULT_OCCURRED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OCCURRED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_AUTHOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_AUTHOR_ID = UUID.randomUUID();

    private static final ShiftLogNature DEFAULT_NATURE = ShiftLogNature.EVENEMENT;
    private static final ShiftLogNature UPDATED_NATURE = ShiftLogNature.ALERTE;

    private static final String DEFAULT_TEXT = "AAAAAAAAAA";
    private static final String UPDATED_TEXT = "BBBBBBBBBB";

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_PREVIOUS_HASH = "e6f6675e2b8b612aab3d9fad623ff343e5445200f41562d71b9390ae1be2e4ec";
    private static final String UPDATED_PREVIOUS_HASH = "0527e0531e9f18c35fd39a00494f1c1d49ad11ae368e39b9b344702dc2af11fb";

    private static final String DEFAULT_HASH = "f15f713a83e8dc93d74c067c6d7af2ecf07b8699404f32b45cc7e87cba138ca3";
    private static final String UPDATED_HASH = "f2f6fbf3377e9f888c899b099d6bf0bb68f858f245818b2ccbef775d35eb2d26";

    private static final String ENTITY_API_URL = "/api/shift-log-entries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ShiftLogEntryRepository shiftLogEntryRepository;

    @Mock
    private ShiftLogEntryRepository shiftLogEntryRepositoryMock;

    @Autowired
    private ShiftLogEntryMapper shiftLogEntryMapper;

    @Mock
    private ShiftLogEntryService shiftLogEntryServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restShiftLogEntryMockMvc;

    private ShiftLogEntry shiftLogEntry;

    private ShiftLogEntry insertedShiftLogEntry;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ShiftLogEntry createEntity(EntityManager em) {
        ShiftLogEntry shiftLogEntry = new ShiftLogEntry()
            .chainIndex(DEFAULT_CHAIN_INDEX)
            .occurredAt(DEFAULT_OCCURRED_AT)
            .authorId(DEFAULT_AUTHOR_ID)
            .nature(DEFAULT_NATURE)
            .text(DEFAULT_TEXT)
            .reference(DEFAULT_REFERENCE)
            .previousHash(DEFAULT_PREVIOUS_HASH)
            .hash(DEFAULT_HASH);
        // Add required entity
        Shift shift;
        if (TestUtil.findAll(em, Shift.class).isEmpty()) {
            shift = ShiftResourceIT.createEntity(em);
            em.persist(shift);
            em.flush();
        } else {
            shift = TestUtil.findAll(em, Shift.class).get(0);
        }
        shiftLogEntry.setShift(shift);
        return shiftLogEntry;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ShiftLogEntry createUpdatedEntity(EntityManager em) {
        ShiftLogEntry updatedShiftLogEntry = new ShiftLogEntry()
            .chainIndex(UPDATED_CHAIN_INDEX)
            .occurredAt(UPDATED_OCCURRED_AT)
            .authorId(UPDATED_AUTHOR_ID)
            .nature(UPDATED_NATURE)
            .text(UPDATED_TEXT)
            .reference(UPDATED_REFERENCE)
            .previousHash(UPDATED_PREVIOUS_HASH)
            .hash(UPDATED_HASH);
        // Add required entity
        Shift shift;
        if (TestUtil.findAll(em, Shift.class).isEmpty()) {
            shift = ShiftResourceIT.createUpdatedEntity(em);
            em.persist(shift);
            em.flush();
        } else {
            shift = TestUtil.findAll(em, Shift.class).get(0);
        }
        updatedShiftLogEntry.setShift(shift);
        return updatedShiftLogEntry;
    }

    @BeforeEach
    void initTest() {
        shiftLogEntry = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedShiftLogEntry != null) {
            shiftLogEntryRepository.delete(insertedShiftLogEntry);
            insertedShiftLogEntry = null;
        }
    }

    @Test
    @Transactional
    void getAllShiftLogEntries() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList
        restShiftLogEntryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(shiftLogEntry.getId().toString())))
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
    void getAllShiftLogEntriesWithEagerRelationshipsIsEnabled() throws Exception {
        when(shiftLogEntryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restShiftLogEntryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(shiftLogEntryServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllShiftLogEntriesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(shiftLogEntryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restShiftLogEntryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(shiftLogEntryRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getShiftLogEntry() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get the shiftLogEntry
        restShiftLogEntryMockMvc
            .perform(get(ENTITY_API_URL_ID, shiftLogEntry.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(shiftLogEntry.getId().toString()))
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
    void getShiftLogEntriesByIdFiltering() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        UUID id = shiftLogEntry.getId();

        defaultShiftLogEntryFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByChainIndexIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where chainIndex equals to
        defaultShiftLogEntryFiltering("chainIndex.equals=" + DEFAULT_CHAIN_INDEX, "chainIndex.equals=" + UPDATED_CHAIN_INDEX);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByChainIndexIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where chainIndex in
        defaultShiftLogEntryFiltering(
            "chainIndex.in=" + DEFAULT_CHAIN_INDEX + "," + UPDATED_CHAIN_INDEX,
            "chainIndex.in=" + UPDATED_CHAIN_INDEX
        );
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByChainIndexIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where chainIndex is not null
        defaultShiftLogEntryFiltering("chainIndex.specified=true", "chainIndex.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByChainIndexIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where chainIndex is greater than or equal to
        defaultShiftLogEntryFiltering(
            "chainIndex.greaterThanOrEqual=" + DEFAULT_CHAIN_INDEX,
            "chainIndex.greaterThanOrEqual=" + UPDATED_CHAIN_INDEX
        );
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByChainIndexIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where chainIndex is less than or equal to
        defaultShiftLogEntryFiltering(
            "chainIndex.lessThanOrEqual=" + DEFAULT_CHAIN_INDEX,
            "chainIndex.lessThanOrEqual=" + SMALLER_CHAIN_INDEX
        );
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByChainIndexIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where chainIndex is less than
        defaultShiftLogEntryFiltering("chainIndex.lessThan=" + UPDATED_CHAIN_INDEX, "chainIndex.lessThan=" + DEFAULT_CHAIN_INDEX);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByChainIndexIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where chainIndex is greater than
        defaultShiftLogEntryFiltering("chainIndex.greaterThan=" + SMALLER_CHAIN_INDEX, "chainIndex.greaterThan=" + DEFAULT_CHAIN_INDEX);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByOccurredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where occurredAt equals to
        defaultShiftLogEntryFiltering("occurredAt.equals=" + DEFAULT_OCCURRED_AT, "occurredAt.equals=" + UPDATED_OCCURRED_AT);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByOccurredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where occurredAt in
        defaultShiftLogEntryFiltering(
            "occurredAt.in=" + DEFAULT_OCCURRED_AT + "," + UPDATED_OCCURRED_AT,
            "occurredAt.in=" + UPDATED_OCCURRED_AT
        );
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByOccurredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where occurredAt is not null
        defaultShiftLogEntryFiltering("occurredAt.specified=true", "occurredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByAuthorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where authorId equals to
        defaultShiftLogEntryFiltering("authorId.equals=" + DEFAULT_AUTHOR_ID, "authorId.equals=" + UPDATED_AUTHOR_ID);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByAuthorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where authorId in
        defaultShiftLogEntryFiltering("authorId.in=" + DEFAULT_AUTHOR_ID + "," + UPDATED_AUTHOR_ID, "authorId.in=" + UPDATED_AUTHOR_ID);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByAuthorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where authorId is not null
        defaultShiftLogEntryFiltering("authorId.specified=true", "authorId.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByNatureIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where nature equals to
        defaultShiftLogEntryFiltering("nature.equals=" + DEFAULT_NATURE, "nature.equals=" + UPDATED_NATURE);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByNatureIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where nature in
        defaultShiftLogEntryFiltering("nature.in=" + DEFAULT_NATURE + "," + UPDATED_NATURE, "nature.in=" + UPDATED_NATURE);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByNatureIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where nature is not null
        defaultShiftLogEntryFiltering("nature.specified=true", "nature.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByTextIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where text equals to
        defaultShiftLogEntryFiltering("text.equals=" + DEFAULT_TEXT, "text.equals=" + UPDATED_TEXT);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByTextIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where text in
        defaultShiftLogEntryFiltering("text.in=" + DEFAULT_TEXT + "," + UPDATED_TEXT, "text.in=" + UPDATED_TEXT);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByTextIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where text is not null
        defaultShiftLogEntryFiltering("text.specified=true", "text.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByTextContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where text contains
        defaultShiftLogEntryFiltering("text.contains=" + DEFAULT_TEXT, "text.contains=" + UPDATED_TEXT);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByTextNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where text does not contain
        defaultShiftLogEntryFiltering("text.doesNotContain=" + UPDATED_TEXT, "text.doesNotContain=" + DEFAULT_TEXT);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where reference equals to
        defaultShiftLogEntryFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where reference in
        defaultShiftLogEntryFiltering("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE, "reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where reference is not null
        defaultShiftLogEntryFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where reference contains
        defaultShiftLogEntryFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where reference does not contain
        defaultShiftLogEntryFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByPreviousHashIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where previousHash equals to
        defaultShiftLogEntryFiltering("previousHash.equals=" + DEFAULT_PREVIOUS_HASH, "previousHash.equals=" + UPDATED_PREVIOUS_HASH);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByPreviousHashIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where previousHash in
        defaultShiftLogEntryFiltering(
            "previousHash.in=" + DEFAULT_PREVIOUS_HASH + "," + UPDATED_PREVIOUS_HASH,
            "previousHash.in=" + UPDATED_PREVIOUS_HASH
        );
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByPreviousHashIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where previousHash is not null
        defaultShiftLogEntryFiltering("previousHash.specified=true", "previousHash.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByPreviousHashContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where previousHash contains
        defaultShiftLogEntryFiltering("previousHash.contains=" + DEFAULT_PREVIOUS_HASH, "previousHash.contains=" + UPDATED_PREVIOUS_HASH);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByPreviousHashNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where previousHash does not contain
        defaultShiftLogEntryFiltering(
            "previousHash.doesNotContain=" + UPDATED_PREVIOUS_HASH,
            "previousHash.doesNotContain=" + DEFAULT_PREVIOUS_HASH
        );
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByHashIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where hash equals to
        defaultShiftLogEntryFiltering("hash.equals=" + DEFAULT_HASH, "hash.equals=" + UPDATED_HASH);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByHashIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where hash in
        defaultShiftLogEntryFiltering("hash.in=" + DEFAULT_HASH + "," + UPDATED_HASH, "hash.in=" + UPDATED_HASH);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByHashIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where hash is not null
        defaultShiftLogEntryFiltering("hash.specified=true", "hash.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByHashContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where hash contains
        defaultShiftLogEntryFiltering("hash.contains=" + DEFAULT_HASH, "hash.contains=" + UPDATED_HASH);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByHashNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShiftLogEntry = shiftLogEntryRepository.saveAndFlush(shiftLogEntry);

        // Get all the shiftLogEntryList where hash does not contain
        defaultShiftLogEntryFiltering("hash.doesNotContain=" + UPDATED_HASH, "hash.doesNotContain=" + DEFAULT_HASH);
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByShiftIsEqualToSomething() throws Exception {
        Shift shift;
        if (TestUtil.findAll(em, Shift.class).isEmpty()) {
            shiftLogEntryRepository.saveAndFlush(shiftLogEntry);
            shift = ShiftResourceIT.createEntity(em);
        } else {
            shift = TestUtil.findAll(em, Shift.class).get(0);
        }
        em.persist(shift);
        em.flush();
        shiftLogEntry.setShift(shift);
        shiftLogEntryRepository.saveAndFlush(shiftLogEntry);
        UUID shiftId = shift.getId();
        // Get all the shiftLogEntryList where shift equals to shiftId
        defaultShiftLogEntryShouldBeFound("shiftId.equals=" + shiftId);

        // Get all the shiftLogEntryList where shift equals to UUID.randomUUID()
        defaultShiftLogEntryShouldNotBeFound("shiftId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllShiftLogEntriesByCorrectsIsEqualToSomething() throws Exception {
        ShiftLogEntry corrects;
        if (TestUtil.findAll(em, ShiftLogEntry.class).isEmpty()) {
            shiftLogEntryRepository.saveAndFlush(shiftLogEntry);
            corrects = ShiftLogEntryResourceIT.createEntity(em);
        } else {
            corrects = TestUtil.findAll(em, ShiftLogEntry.class).get(0);
        }
        em.persist(corrects);
        em.flush();
        shiftLogEntry.setCorrects(corrects);
        shiftLogEntryRepository.saveAndFlush(shiftLogEntry);
        UUID correctsId = corrects.getId();
        // Get all the shiftLogEntryList where corrects equals to correctsId
        defaultShiftLogEntryShouldBeFound("correctsId.equals=" + correctsId);

        // Get all the shiftLogEntryList where corrects equals to UUID.randomUUID()
        defaultShiftLogEntryShouldNotBeFound("correctsId.equals=" + UUID.randomUUID());
    }

    private void defaultShiftLogEntryFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultShiftLogEntryShouldBeFound(shouldBeFound);
        defaultShiftLogEntryShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultShiftLogEntryShouldBeFound(String filter) throws Exception {
        restShiftLogEntryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(shiftLogEntry.getId().toString())))
            .andExpect(jsonPath("$.[*].chainIndex").value(hasItem(DEFAULT_CHAIN_INDEX.intValue())))
            .andExpect(jsonPath("$.[*].occurredAt").value(hasItem(DEFAULT_OCCURRED_AT.toString())))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].nature").value(hasItem(DEFAULT_NATURE.toString())))
            .andExpect(jsonPath("$.[*].text").value(hasItem(DEFAULT_TEXT)))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].previousHash").value(hasItem(DEFAULT_PREVIOUS_HASH)))
            .andExpect(jsonPath("$.[*].hash").value(hasItem(DEFAULT_HASH)));

        // Check, that the count call also returns 1
        restShiftLogEntryMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultShiftLogEntryShouldNotBeFound(String filter) throws Exception {
        restShiftLogEntryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restShiftLogEntryMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingShiftLogEntry() throws Exception {
        // Get the shiftLogEntry
        restShiftLogEntryMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return shiftLogEntryRepository.count();
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

    protected ShiftLogEntry getPersistedShiftLogEntry(ShiftLogEntry shiftLogEntry) {
        return shiftLogEntryRepository.findById(shiftLogEntry.getId()).orElseThrow();
    }

    protected void assertPersistedShiftLogEntryToMatchAllProperties(ShiftLogEntry expectedShiftLogEntry) {
        assertShiftLogEntryAllPropertiesEquals(expectedShiftLogEntry, getPersistedShiftLogEntry(expectedShiftLogEntry));
    }

    protected void assertPersistedShiftLogEntryToMatchUpdatableProperties(ShiftLogEntry expectedShiftLogEntry) {
        assertShiftLogEntryAllUpdatablePropertiesEquals(expectedShiftLogEntry, getPersistedShiftLogEntry(expectedShiftLogEntry));
    }
}
