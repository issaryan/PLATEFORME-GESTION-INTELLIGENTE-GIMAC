package cm.gimac.des.audit.web.rest;

import static cm.gimac.des.audit.domain.PeriodRootAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.audit.IntegrationTest;
import cm.gimac.des.audit.domain.ConsistencyProof;
import cm.gimac.des.audit.domain.PeriodRoot;
import cm.gimac.des.audit.domain.PeriodRoot;
import cm.gimac.des.audit.domain.enumeration.PeriodRootState;
import cm.gimac.des.audit.repository.PeriodRootRepository;
import cm.gimac.des.audit.service.PeriodRootService;
import cm.gimac.des.audit.service.mapper.PeriodRootMapper;
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
 * Integration tests for the {@link PeriodRootResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class PeriodRootResourceIT {

    private static final String DEFAULT_PERIOD = "3160-39-23";
    private static final String UPDATED_PERIOD = "1911-58-54";

    private static final Long DEFAULT_TREE_SIZE = 0L;
    private static final Long UPDATED_TREE_SIZE = 1L;
    private static final Long SMALLER_TREE_SIZE = 0L - 1L;

    private static final String DEFAULT_ROOT_HASH = "96a4ca1a3cd9d3f4ad7a8d8439e363abd4794a9c2c10505ffbd40c79ac89d488";
    private static final String UPDATED_ROOT_HASH = "ab321fee879e56b194d7c84ac81cf1cd0c8290a8f6e4c9ae42a6c4df52e3301d";

    private static final Instant DEFAULT_SEALED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SEALED_AT = Instant.ofEpochMilli(1701661978853L);

    private static final PeriodRootState DEFAULT_STATE = PeriodRootState.OUVERTE;
    private static final PeriodRootState UPDATED_STATE = PeriodRootState.SCELLEE;

    private static final byte[] DEFAULT_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/png";

    private static final String DEFAULT_TIMESTAMP_SERIAL_NUMBER = "AAAAAAAAAA";
    private static final String UPDATED_TIMESTAMP_SERIAL_NUMBER = "BBBBBBBBBB";

    private static final Instant DEFAULT_TIMESTAMP_GEN_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_TIMESTAMP_GEN_TIME = Instant.ofEpochMilli(1701661978853L);

    private static final String DEFAULT_TIMESTAMP_IMPRINT = "983b3b66f3f2108a8ebb394b3acfba648f94ac221749b475791524537bc5086b";
    private static final String UPDATED_TIMESTAMP_IMPRINT = "567073a22d91c94c3cfaac9b44c677c7d2606bad35e6eaf819baaf344a5631bb";

    private static final String DEFAULT_PUBLICATION_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_PUBLICATION_REFERENCE = "BBBBBBBBBB";

    private static final Instant DEFAULT_PUBLISHED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PUBLISHED_AT = Instant.ofEpochMilli(1701661978853L);

    private static final String ENTITY_API_URL = "/api/period-roots";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PeriodRootRepository periodRootRepository;

    @Mock
    private PeriodRootRepository periodRootRepositoryMock;

    @Autowired
    private PeriodRootMapper periodRootMapper;

    @Mock
    private PeriodRootService periodRootServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPeriodRootMockMvc;

    private PeriodRoot periodRoot;

    private PeriodRoot insertedPeriodRoot;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PeriodRoot createEntity() {
        return new PeriodRoot()
            .period(DEFAULT_PERIOD)
            .treeSize(DEFAULT_TREE_SIZE)
            .rootHash(DEFAULT_ROOT_HASH)
            .sealedAt(DEFAULT_SEALED_AT)
            .state(DEFAULT_STATE)
            .timestampToken(DEFAULT_TIMESTAMP_TOKEN)
            .timestampTokenContentType(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .timestampSerialNumber(DEFAULT_TIMESTAMP_SERIAL_NUMBER)
            .timestampGenTime(DEFAULT_TIMESTAMP_GEN_TIME)
            .timestampImprint(DEFAULT_TIMESTAMP_IMPRINT)
            .publicationReference(DEFAULT_PUBLICATION_REFERENCE)
            .publishedAt(DEFAULT_PUBLISHED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PeriodRoot createUpdatedEntity() {
        return new PeriodRoot()
            .period(UPDATED_PERIOD)
            .treeSize(UPDATED_TREE_SIZE)
            .rootHash(UPDATED_ROOT_HASH)
            .sealedAt(UPDATED_SEALED_AT)
            .state(UPDATED_STATE)
            .timestampToken(UPDATED_TIMESTAMP_TOKEN)
            .timestampTokenContentType(UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .timestampSerialNumber(UPDATED_TIMESTAMP_SERIAL_NUMBER)
            .timestampGenTime(UPDATED_TIMESTAMP_GEN_TIME)
            .timestampImprint(UPDATED_TIMESTAMP_IMPRINT)
            .publicationReference(UPDATED_PUBLICATION_REFERENCE)
            .publishedAt(UPDATED_PUBLISHED_AT);
    }

    @BeforeEach
    void initTest() {
        periodRoot = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedPeriodRoot != null) {
            periodRootRepository.delete(insertedPeriodRoot);
            insertedPeriodRoot = null;
        }
    }

    @Test
    @Transactional
    void getAllPeriodRoots() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList
        restPeriodRootMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(periodRoot.getId().toString())))
            .andExpect(jsonPath("$.[*].period").value(hasItem(DEFAULT_PERIOD)))
            .andExpect(jsonPath("$.[*].treeSize").value(hasItem(DEFAULT_TREE_SIZE.intValue())))
            .andExpect(jsonPath("$.[*].rootHash").value(hasItem(DEFAULT_ROOT_HASH)))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].timestampSerialNumber").value(hasItem(DEFAULT_TIMESTAMP_SERIAL_NUMBER)))
            .andExpect(jsonPath("$.[*].timestampGenTime").value(hasItem(DEFAULT_TIMESTAMP_GEN_TIME.toString())))
            .andExpect(jsonPath("$.[*].timestampImprint").value(hasItem(DEFAULT_TIMESTAMP_IMPRINT)))
            .andExpect(jsonPath("$.[*].publicationReference").value(hasItem(DEFAULT_PUBLICATION_REFERENCE)))
            .andExpect(jsonPath("$.[*].publishedAt").value(hasItem(DEFAULT_PUBLISHED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPeriodRootsWithEagerRelationshipsIsEnabled() throws Exception {
        when(periodRootServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPeriodRootMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(periodRootServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPeriodRootsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(periodRootServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPeriodRootMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(periodRootRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getPeriodRoot() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get the periodRoot
        restPeriodRootMockMvc
            .perform(get(ENTITY_API_URL_ID, periodRoot.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(periodRoot.getId().toString()))
            .andExpect(jsonPath("$.period").value(DEFAULT_PERIOD))
            .andExpect(jsonPath("$.treeSize").value(DEFAULT_TREE_SIZE.intValue()))
            .andExpect(jsonPath("$.rootHash").value(DEFAULT_ROOT_HASH))
            .andExpect(jsonPath("$.sealedAt").value(DEFAULT_SEALED_AT.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.timestampTokenContentType").value(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE))
            .andExpect(jsonPath("$.timestampToken").value(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN)))
            .andExpect(jsonPath("$.timestampSerialNumber").value(DEFAULT_TIMESTAMP_SERIAL_NUMBER))
            .andExpect(jsonPath("$.timestampGenTime").value(DEFAULT_TIMESTAMP_GEN_TIME.toString()))
            .andExpect(jsonPath("$.timestampImprint").value(DEFAULT_TIMESTAMP_IMPRINT))
            .andExpect(jsonPath("$.publicationReference").value(DEFAULT_PUBLICATION_REFERENCE))
            .andExpect(jsonPath("$.publishedAt").value(DEFAULT_PUBLISHED_AT.toString()));
    }

    @Test
    @Transactional
    void getPeriodRootsByIdFiltering() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        UUID id = periodRoot.getId();

        defaultPeriodRootFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPeriodIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where period equals to
        defaultPeriodRootFiltering("period.equals=" + DEFAULT_PERIOD, "period.equals=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPeriodIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where period in
        defaultPeriodRootFiltering("period.in=" + DEFAULT_PERIOD + "," + UPDATED_PERIOD, "period.in=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPeriodIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where period is not null
        defaultPeriodRootFiltering("period.specified=true", "period.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPeriodContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where period contains
        defaultPeriodRootFiltering("period.contains=" + DEFAULT_PERIOD, "period.contains=" + UPDATED_PERIOD);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPeriodNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where period does not contain
        defaultPeriodRootFiltering("period.doesNotContain=" + UPDATED_PERIOD, "period.doesNotContain=" + DEFAULT_PERIOD);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTreeSizeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where treeSize equals to
        defaultPeriodRootFiltering("treeSize.equals=" + DEFAULT_TREE_SIZE, "treeSize.equals=" + UPDATED_TREE_SIZE);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTreeSizeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where treeSize in
        defaultPeriodRootFiltering("treeSize.in=" + DEFAULT_TREE_SIZE + "," + UPDATED_TREE_SIZE, "treeSize.in=" + UPDATED_TREE_SIZE);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTreeSizeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where treeSize is not null
        defaultPeriodRootFiltering("treeSize.specified=true", "treeSize.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTreeSizeIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where treeSize is greater than or equal to
        defaultPeriodRootFiltering("treeSize.greaterThanOrEqual=" + DEFAULT_TREE_SIZE, "treeSize.greaterThanOrEqual=" + UPDATED_TREE_SIZE);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTreeSizeIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where treeSize is less than or equal to
        defaultPeriodRootFiltering("treeSize.lessThanOrEqual=" + DEFAULT_TREE_SIZE, "treeSize.lessThanOrEqual=" + SMALLER_TREE_SIZE);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTreeSizeIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where treeSize is less than
        defaultPeriodRootFiltering("treeSize.lessThan=" + UPDATED_TREE_SIZE, "treeSize.lessThan=" + DEFAULT_TREE_SIZE);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTreeSizeIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where treeSize is greater than
        defaultPeriodRootFiltering("treeSize.greaterThan=" + SMALLER_TREE_SIZE, "treeSize.greaterThan=" + DEFAULT_TREE_SIZE);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByRootHashIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where rootHash equals to
        defaultPeriodRootFiltering("rootHash.equals=" + DEFAULT_ROOT_HASH, "rootHash.equals=" + UPDATED_ROOT_HASH);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByRootHashIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where rootHash in
        defaultPeriodRootFiltering("rootHash.in=" + DEFAULT_ROOT_HASH + "," + UPDATED_ROOT_HASH, "rootHash.in=" + UPDATED_ROOT_HASH);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByRootHashIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where rootHash is not null
        defaultPeriodRootFiltering("rootHash.specified=true", "rootHash.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodRootsByRootHashContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where rootHash contains
        defaultPeriodRootFiltering("rootHash.contains=" + DEFAULT_ROOT_HASH, "rootHash.contains=" + UPDATED_ROOT_HASH);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByRootHashNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where rootHash does not contain
        defaultPeriodRootFiltering("rootHash.doesNotContain=" + UPDATED_ROOT_HASH, "rootHash.doesNotContain=" + DEFAULT_ROOT_HASH);
    }

    @Test
    @Transactional
    void getAllPeriodRootsBySealedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where sealedAt equals to
        defaultPeriodRootFiltering("sealedAt.equals=" + DEFAULT_SEALED_AT, "sealedAt.equals=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllPeriodRootsBySealedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where sealedAt in
        defaultPeriodRootFiltering("sealedAt.in=" + DEFAULT_SEALED_AT + "," + UPDATED_SEALED_AT, "sealedAt.in=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllPeriodRootsBySealedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where sealedAt is not null
        defaultPeriodRootFiltering("sealedAt.specified=true", "sealedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodRootsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where state equals to
        defaultPeriodRootFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where state in
        defaultPeriodRootFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where state is not null
        defaultPeriodRootFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampSerialNumberIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampSerialNumber equals to
        defaultPeriodRootFiltering(
            "timestampSerialNumber.equals=" + DEFAULT_TIMESTAMP_SERIAL_NUMBER,
            "timestampSerialNumber.equals=" + UPDATED_TIMESTAMP_SERIAL_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampSerialNumberIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampSerialNumber in
        defaultPeriodRootFiltering(
            "timestampSerialNumber.in=" + DEFAULT_TIMESTAMP_SERIAL_NUMBER + "," + UPDATED_TIMESTAMP_SERIAL_NUMBER,
            "timestampSerialNumber.in=" + UPDATED_TIMESTAMP_SERIAL_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampSerialNumberIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampSerialNumber is not null
        defaultPeriodRootFiltering("timestampSerialNumber.specified=true", "timestampSerialNumber.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampSerialNumberContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampSerialNumber contains
        defaultPeriodRootFiltering(
            "timestampSerialNumber.contains=" + DEFAULT_TIMESTAMP_SERIAL_NUMBER,
            "timestampSerialNumber.contains=" + UPDATED_TIMESTAMP_SERIAL_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampSerialNumberNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampSerialNumber does not contain
        defaultPeriodRootFiltering(
            "timestampSerialNumber.doesNotContain=" + UPDATED_TIMESTAMP_SERIAL_NUMBER,
            "timestampSerialNumber.doesNotContain=" + DEFAULT_TIMESTAMP_SERIAL_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampGenTimeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampGenTime equals to
        defaultPeriodRootFiltering(
            "timestampGenTime.equals=" + DEFAULT_TIMESTAMP_GEN_TIME,
            "timestampGenTime.equals=" + UPDATED_TIMESTAMP_GEN_TIME
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampGenTimeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampGenTime in
        defaultPeriodRootFiltering(
            "timestampGenTime.in=" + DEFAULT_TIMESTAMP_GEN_TIME + "," + UPDATED_TIMESTAMP_GEN_TIME,
            "timestampGenTime.in=" + UPDATED_TIMESTAMP_GEN_TIME
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampGenTimeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampGenTime is not null
        defaultPeriodRootFiltering("timestampGenTime.specified=true", "timestampGenTime.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampImprintIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampImprint equals to
        defaultPeriodRootFiltering(
            "timestampImprint.equals=" + DEFAULT_TIMESTAMP_IMPRINT,
            "timestampImprint.equals=" + UPDATED_TIMESTAMP_IMPRINT
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampImprintIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampImprint in
        defaultPeriodRootFiltering(
            "timestampImprint.in=" + DEFAULT_TIMESTAMP_IMPRINT + "," + UPDATED_TIMESTAMP_IMPRINT,
            "timestampImprint.in=" + UPDATED_TIMESTAMP_IMPRINT
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampImprintIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampImprint is not null
        defaultPeriodRootFiltering("timestampImprint.specified=true", "timestampImprint.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampImprintContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampImprint contains
        defaultPeriodRootFiltering(
            "timestampImprint.contains=" + DEFAULT_TIMESTAMP_IMPRINT,
            "timestampImprint.contains=" + UPDATED_TIMESTAMP_IMPRINT
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByTimestampImprintNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where timestampImprint does not contain
        defaultPeriodRootFiltering(
            "timestampImprint.doesNotContain=" + UPDATED_TIMESTAMP_IMPRINT,
            "timestampImprint.doesNotContain=" + DEFAULT_TIMESTAMP_IMPRINT
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPublicationReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where publicationReference equals to
        defaultPeriodRootFiltering(
            "publicationReference.equals=" + DEFAULT_PUBLICATION_REFERENCE,
            "publicationReference.equals=" + UPDATED_PUBLICATION_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPublicationReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where publicationReference in
        defaultPeriodRootFiltering(
            "publicationReference.in=" + DEFAULT_PUBLICATION_REFERENCE + "," + UPDATED_PUBLICATION_REFERENCE,
            "publicationReference.in=" + UPDATED_PUBLICATION_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPublicationReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where publicationReference is not null
        defaultPeriodRootFiltering("publicationReference.specified=true", "publicationReference.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPublicationReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where publicationReference contains
        defaultPeriodRootFiltering(
            "publicationReference.contains=" + DEFAULT_PUBLICATION_REFERENCE,
            "publicationReference.contains=" + UPDATED_PUBLICATION_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPublicationReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where publicationReference does not contain
        defaultPeriodRootFiltering(
            "publicationReference.doesNotContain=" + UPDATED_PUBLICATION_REFERENCE,
            "publicationReference.doesNotContain=" + DEFAULT_PUBLICATION_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPublishedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where publishedAt equals to
        defaultPeriodRootFiltering("publishedAt.equals=" + DEFAULT_PUBLISHED_AT, "publishedAt.equals=" + UPDATED_PUBLISHED_AT);
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPublishedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where publishedAt in
        defaultPeriodRootFiltering(
            "publishedAt.in=" + DEFAULT_PUBLISHED_AT + "," + UPDATED_PUBLISHED_AT,
            "publishedAt.in=" + UPDATED_PUBLISHED_AT
        );
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPublishedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPeriodRoot = periodRootRepository.saveAndFlush(periodRoot);

        // Get all the periodRootList where publishedAt is not null
        defaultPeriodRootFiltering("publishedAt.specified=true", "publishedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPeriodRootsByPreviousRootIsEqualToSomething() throws Exception {
        PeriodRoot previousRoot;
        if (TestUtil.findAll(em, PeriodRoot.class).isEmpty()) {
            periodRootRepository.saveAndFlush(periodRoot);
            previousRoot = PeriodRootResourceIT.createEntity();
            // La période est unique : la racine précédente doit porter une autre période
            // (défaut du test généré par JHipster pour une auto-référence sur champ unique).
            previousRoot.setPeriod(UPDATED_PERIOD);
        } else {
            previousRoot = TestUtil.findAll(em, PeriodRoot.class).getFirst();
        }
        em.persist(previousRoot);
        em.flush();
        periodRoot.setPreviousRoot(previousRoot);
        periodRootRepository.saveAndFlush(periodRoot);
        UUID previousRootId = previousRoot.getId();
        // Get all the periodRootList where previousRoot equals to previousRootId
        defaultPeriodRootShouldBeFound("previousRootId.equals=" + previousRootId);

        // Get all the periodRootList where previousRoot equals to UUID.randomUUID()
        defaultPeriodRootShouldNotBeFound("previousRootId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllPeriodRootsByConsistencyWithPreviousIsEqualToSomething() throws Exception {
        ConsistencyProof consistencyWithPrevious;
        if (TestUtil.findAll(em, ConsistencyProof.class).isEmpty()) {
            periodRootRepository.saveAndFlush(periodRoot);
            consistencyWithPrevious = ConsistencyProofResourceIT.createEntity();
        } else {
            consistencyWithPrevious = TestUtil.findAll(em, ConsistencyProof.class).getFirst();
        }
        em.persist(consistencyWithPrevious);
        em.flush();
        periodRoot.setConsistencyWithPrevious(consistencyWithPrevious);
        periodRootRepository.saveAndFlush(periodRoot);
        UUID consistencyWithPreviousId = consistencyWithPrevious.getId();
        // Get all the periodRootList where consistencyWithPrevious equals to consistencyWithPreviousId
        defaultPeriodRootShouldBeFound("consistencyWithPreviousId.equals=" + consistencyWithPreviousId);

        // Get all the periodRootList where consistencyWithPrevious equals to UUID.randomUUID()
        defaultPeriodRootShouldNotBeFound("consistencyWithPreviousId.equals=" + UUID.randomUUID());
    }

    private void defaultPeriodRootFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultPeriodRootShouldBeFound(shouldBeFound);
        defaultPeriodRootShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultPeriodRootShouldBeFound(String filter) throws Exception {
        restPeriodRootMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(periodRoot.getId().toString())))
            .andExpect(jsonPath("$.[*].period").value(hasItem(DEFAULT_PERIOD)))
            .andExpect(jsonPath("$.[*].treeSize").value(hasItem(DEFAULT_TREE_SIZE.intValue())))
            .andExpect(jsonPath("$.[*].rootHash").value(hasItem(DEFAULT_ROOT_HASH)))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].timestampSerialNumber").value(hasItem(DEFAULT_TIMESTAMP_SERIAL_NUMBER)))
            .andExpect(jsonPath("$.[*].timestampGenTime").value(hasItem(DEFAULT_TIMESTAMP_GEN_TIME.toString())))
            .andExpect(jsonPath("$.[*].timestampImprint").value(hasItem(DEFAULT_TIMESTAMP_IMPRINT)))
            .andExpect(jsonPath("$.[*].publicationReference").value(hasItem(DEFAULT_PUBLICATION_REFERENCE)))
            .andExpect(jsonPath("$.[*].publishedAt").value(hasItem(DEFAULT_PUBLISHED_AT.toString())));

        // Check, that the count call also returns 1
        restPeriodRootMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultPeriodRootShouldNotBeFound(String filter) throws Exception {
        restPeriodRootMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restPeriodRootMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingPeriodRoot() throws Exception {
        // Get the periodRoot
        restPeriodRootMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return periodRootRepository.count();
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

    protected PeriodRoot getPersistedPeriodRoot(PeriodRoot periodRoot) {
        return periodRootRepository.findById(periodRoot.getId()).orElseThrow();
    }

    protected void assertPersistedPeriodRootToMatchAllProperties(PeriodRoot expectedPeriodRoot) {
        assertPeriodRootAllPropertiesEquals(expectedPeriodRoot, getPersistedPeriodRoot(expectedPeriodRoot));
    }

    protected void assertPersistedPeriodRootToMatchUpdatableProperties(PeriodRoot expectedPeriodRoot) {
        assertPeriodRootAllUpdatablePropertiesEquals(expectedPeriodRoot, getPersistedPeriodRoot(expectedPeriodRoot));
    }
}
