package cm.gimac.des.document.web.rest;

import static cm.gimac.des.document.domain.IntegrityCheckAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.domain.IntegrityCheck;
import cm.gimac.des.document.domain.enumeration.IntegrityResult;
import cm.gimac.des.document.repository.IntegrityCheckRepository;
import cm.gimac.des.document.service.mapper.IntegrityCheckMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link IntegrityCheckResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class IntegrityCheckResourceIT {

    private static final Instant DEFAULT_STARTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_FINISHED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FINISHED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_SAMPLE_SIZE = 1;
    private static final Integer UPDATED_SAMPLE_SIZE = 2;

    private static final Integer DEFAULT_MISMATCHES = 0;
    private static final Integer UPDATED_MISMATCHES = 1;

    private static final Integer DEFAULT_UNREADABLE = 0;
    private static final Integer UPDATED_UNREADABLE = 1;

    private static final IntegrityResult DEFAULT_RESULT = IntegrityResult.EN_COURS;
    private static final IntegrityResult UPDATED_RESULT = IntegrityResult.CONFORME;

    private static final String ENTITY_API_URL = "/api/integrity-checks";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private IntegrityCheckRepository integrityCheckRepository;

    @Autowired
    private IntegrityCheckMapper integrityCheckMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restIntegrityCheckMockMvc;

    private IntegrityCheck integrityCheck;

    private IntegrityCheck insertedIntegrityCheck;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static IntegrityCheck createEntity() {
        return new IntegrityCheck()
            .startedAt(DEFAULT_STARTED_AT)
            .finishedAt(DEFAULT_FINISHED_AT)
            .sampleSize(DEFAULT_SAMPLE_SIZE)
            .mismatches(DEFAULT_MISMATCHES)
            .unreadable(DEFAULT_UNREADABLE)
            .result(DEFAULT_RESULT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static IntegrityCheck createUpdatedEntity() {
        return new IntegrityCheck()
            .startedAt(UPDATED_STARTED_AT)
            .finishedAt(UPDATED_FINISHED_AT)
            .sampleSize(UPDATED_SAMPLE_SIZE)
            .mismatches(UPDATED_MISMATCHES)
            .unreadable(UPDATED_UNREADABLE)
            .result(UPDATED_RESULT);
    }

    @BeforeEach
    void initTest() {
        integrityCheck = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedIntegrityCheck != null) {
            integrityCheckRepository.delete(insertedIntegrityCheck);
            insertedIntegrityCheck = null;
        }
    }

    @Test
    @Transactional
    void getAllIntegrityChecks() throws Exception {
        // Initialize the database
        insertedIntegrityCheck = integrityCheckRepository.saveAndFlush(integrityCheck);

        // Get all the integrityCheckList
        restIntegrityCheckMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(integrityCheck.getId().toString())))
            .andExpect(jsonPath("$.[*].startedAt").value(hasItem(DEFAULT_STARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].finishedAt").value(hasItem(DEFAULT_FINISHED_AT.toString())))
            .andExpect(jsonPath("$.[*].sampleSize").value(hasItem(DEFAULT_SAMPLE_SIZE)))
            .andExpect(jsonPath("$.[*].mismatches").value(hasItem(DEFAULT_MISMATCHES)))
            .andExpect(jsonPath("$.[*].unreadable").value(hasItem(DEFAULT_UNREADABLE)))
            .andExpect(jsonPath("$.[*].result").value(hasItem(DEFAULT_RESULT.toString())));
    }

    @Test
    @Transactional
    void getIntegrityCheck() throws Exception {
        // Initialize the database
        insertedIntegrityCheck = integrityCheckRepository.saveAndFlush(integrityCheck);

        // Get the integrityCheck
        restIntegrityCheckMockMvc
            .perform(get(ENTITY_API_URL_ID, integrityCheck.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(integrityCheck.getId().toString()))
            .andExpect(jsonPath("$.startedAt").value(DEFAULT_STARTED_AT.toString()))
            .andExpect(jsonPath("$.finishedAt").value(DEFAULT_FINISHED_AT.toString()))
            .andExpect(jsonPath("$.sampleSize").value(DEFAULT_SAMPLE_SIZE))
            .andExpect(jsonPath("$.mismatches").value(DEFAULT_MISMATCHES))
            .andExpect(jsonPath("$.unreadable").value(DEFAULT_UNREADABLE))
            .andExpect(jsonPath("$.result").value(DEFAULT_RESULT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingIntegrityCheck() throws Exception {
        // Get the integrityCheck
        restIntegrityCheckMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return integrityCheckRepository.count();
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

    protected IntegrityCheck getPersistedIntegrityCheck(IntegrityCheck integrityCheck) {
        return integrityCheckRepository.findById(integrityCheck.getId()).orElseThrow();
    }

    protected void assertPersistedIntegrityCheckToMatchAllProperties(IntegrityCheck expectedIntegrityCheck) {
        assertIntegrityCheckAllPropertiesEquals(expectedIntegrityCheck, getPersistedIntegrityCheck(expectedIntegrityCheck));
    }

    protected void assertPersistedIntegrityCheckToMatchUpdatableProperties(IntegrityCheck expectedIntegrityCheck) {
        assertIntegrityCheckAllUpdatablePropertiesEquals(expectedIntegrityCheck, getPersistedIntegrityCheck(expectedIntegrityCheck));
    }
}
