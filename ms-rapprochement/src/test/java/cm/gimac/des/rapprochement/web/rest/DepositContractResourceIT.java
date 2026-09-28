package cm.gimac.des.rapprochement.web.rest;

import static cm.gimac.des.rapprochement.domain.DepositContractAsserts.*;
import static cm.gimac.des.rapprochement.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.rapprochement.IntegrationTest;
import cm.gimac.des.rapprochement.domain.DepositContract;
import cm.gimac.des.rapprochement.domain.enumeration.StatementFormat;
import cm.gimac.des.rapprochement.domain.enumeration.StatementSource;
import cm.gimac.des.rapprochement.repository.DepositContractRepository;
import cm.gimac.des.rapprochement.service.dto.DepositContractDTO;
import cm.gimac.des.rapprochement.service.mapper.DepositContractMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Duration;
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
 * Integration tests for the {@link DepositContractResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DepositContractResourceIT {

    private static final StatementSource DEFAULT_SOURCE = StatementSource.COMMUTATEUR;
    private static final StatementSource UPDATED_SOURCE = StatementSource.PARTICIPANT;

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final StatementFormat DEFAULT_FORMAT = StatementFormat.CSV;
    private static final StatementFormat UPDATED_FORMAT = StatementFormat.JSON;

    private static final String DEFAULT_REQUIRED_COLUMNS = "AAAAAAAAAA";
    private static final String UPDATED_REQUIRED_COLUMNS = "BBBBBBBBBB";

    private static final String DEFAULT_ENCODING = "AAAAAAAAAA";
    private static final String UPDATED_ENCODING = "BBBBBBBBBB";

    private static final String DEFAULT_SEPARATOR = "A";
    private static final String UPDATED_SEPARATOR = "B";

    private static final Long DEFAULT_MAX_SIZE_BYTES = 1L;
    private static final Long UPDATED_MAX_SIZE_BYTES = 2L;

    private static final Duration DEFAULT_TIMESTAMP_TOLERANCE = Duration.ofHours(6);
    private static final Duration UPDATED_TIMESTAMP_TOLERANCE = Duration.ofHours(12);

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/deposit-contracts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DepositContractRepository depositContractRepository;

    @Autowired
    private DepositContractMapper depositContractMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDepositContractMockMvc;

    private DepositContract depositContract;

    private DepositContract insertedDepositContract;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DepositContract createEntity() {
        return new DepositContract()
            .source(DEFAULT_SOURCE)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .format(DEFAULT_FORMAT)
            .requiredColumns(DEFAULT_REQUIRED_COLUMNS)
            .encoding(DEFAULT_ENCODING)
            .separator(DEFAULT_SEPARATOR)
            .maxSizeBytes(DEFAULT_MAX_SIZE_BYTES)
            .timestampTolerance(DEFAULT_TIMESTAMP_TOLERANCE)
            .active(DEFAULT_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DepositContract createUpdatedEntity() {
        return new DepositContract()
            .source(UPDATED_SOURCE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .format(UPDATED_FORMAT)
            .requiredColumns(UPDATED_REQUIRED_COLUMNS)
            .encoding(UPDATED_ENCODING)
            .separator(UPDATED_SEPARATOR)
            .maxSizeBytes(UPDATED_MAX_SIZE_BYTES)
            .timestampTolerance(UPDATED_TIMESTAMP_TOLERANCE)
            .active(UPDATED_ACTIVE);
    }

    @BeforeEach
    void initTest() {
        depositContract = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedDepositContract != null) {
            depositContractRepository.delete(insertedDepositContract);
            insertedDepositContract = null;
        }
    }

    @Test
    @Transactional
    void createDepositContract() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the DepositContract
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);
        var returnedDepositContractDTO = om.readValue(
            restDepositContractMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(depositContractDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            DepositContractDTO.class
        );

        // Validate the DepositContract in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedDepositContract = depositContractMapper.toEntity(returnedDepositContractDTO);
        assertDepositContractUpdatableFieldsEquals(returnedDepositContract, getPersistedDepositContract(returnedDepositContract));

        insertedDepositContract = returnedDepositContract;
    }

    @Test
    @Transactional
    void createDepositContractWithExistingId() throws Exception {
        // Create the DepositContract with an existing ID
        insertedDepositContract = depositContractRepository.saveAndFlush(depositContract);
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDepositContractMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DepositContract in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkSourceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        depositContract.setSource(null);

        // Create the DepositContract, which fails.
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        restDepositContractMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkFormatIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        depositContract.setFormat(null);

        // Create the DepositContract, which fails.
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        restDepositContractMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRequiredColumnsIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        depositContract.setRequiredColumns(null);

        // Create the DepositContract, which fails.
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        restDepositContractMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEncodingIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        depositContract.setEncoding(null);

        // Create the DepositContract, which fails.
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        restDepositContractMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMaxSizeBytesIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        depositContract.setMaxSizeBytes(null);

        // Create the DepositContract, which fails.
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        restDepositContractMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTimestampToleranceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        depositContract.setTimestampTolerance(null);

        // Create the DepositContract, which fails.
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        restDepositContractMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        depositContract.setActive(null);

        // Create the DepositContract, which fails.
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        restDepositContractMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllDepositContracts() throws Exception {
        // Initialize the database
        insertedDepositContract = depositContractRepository.saveAndFlush(depositContract);

        // Get all the depositContractList
        restDepositContractMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(depositContract.getId().toString())))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].format").value(hasItem(DEFAULT_FORMAT.toString())))
            .andExpect(jsonPath("$.[*].requiredColumns").value(hasItem(DEFAULT_REQUIRED_COLUMNS)))
            .andExpect(jsonPath("$.[*].encoding").value(hasItem(DEFAULT_ENCODING)))
            .andExpect(jsonPath("$.[*].separator").value(hasItem(DEFAULT_SEPARATOR)))
            .andExpect(jsonPath("$.[*].maxSizeBytes").value(hasItem(DEFAULT_MAX_SIZE_BYTES.intValue())))
            .andExpect(jsonPath("$.[*].timestampTolerance").value(hasItem(DEFAULT_TIMESTAMP_TOLERANCE.toString())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @Test
    @Transactional
    void getDepositContract() throws Exception {
        // Initialize the database
        insertedDepositContract = depositContractRepository.saveAndFlush(depositContract);

        // Get the depositContract
        restDepositContractMockMvc
            .perform(get(ENTITY_API_URL_ID, depositContract.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(depositContract.getId().toString()))
            .andExpect(jsonPath("$.source").value(DEFAULT_SOURCE.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.format").value(DEFAULT_FORMAT.toString()))
            .andExpect(jsonPath("$.requiredColumns").value(DEFAULT_REQUIRED_COLUMNS))
            .andExpect(jsonPath("$.encoding").value(DEFAULT_ENCODING))
            .andExpect(jsonPath("$.separator").value(DEFAULT_SEPARATOR))
            .andExpect(jsonPath("$.maxSizeBytes").value(DEFAULT_MAX_SIZE_BYTES.intValue()))
            .andExpect(jsonPath("$.timestampTolerance").value(DEFAULT_TIMESTAMP_TOLERANCE.toString()))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingDepositContract() throws Exception {
        // Get the depositContract
        restDepositContractMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDepositContract() throws Exception {
        // Initialize the database
        insertedDepositContract = depositContractRepository.saveAndFlush(depositContract);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the depositContract
        DepositContract updatedDepositContract = depositContractRepository.findById(depositContract.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedDepositContract are not directly saved in db
        em.detach(updatedDepositContract);
        updatedDepositContract
            .source(UPDATED_SOURCE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .format(UPDATED_FORMAT)
            .requiredColumns(UPDATED_REQUIRED_COLUMNS)
            .encoding(UPDATED_ENCODING)
            .separator(UPDATED_SEPARATOR)
            .maxSizeBytes(UPDATED_MAX_SIZE_BYTES)
            .timestampTolerance(UPDATED_TIMESTAMP_TOLERANCE)
            .active(UPDATED_ACTIVE);
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(updatedDepositContract);

        restDepositContractMockMvc
            .perform(
                put(ENTITY_API_URL_ID, depositContractDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isOk());

        // Validate the DepositContract in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDepositContractToMatchAllProperties(updatedDepositContract);
    }

    @Test
    @Transactional
    void putNonExistingDepositContract() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        depositContract.setId(UUID.randomUUID());

        // Create the DepositContract
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDepositContractMockMvc
            .perform(
                put(ENTITY_API_URL_ID, depositContractDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DepositContract in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDepositContract() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        depositContract.setId(UUID.randomUUID());

        // Create the DepositContract
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDepositContractMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DepositContract in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDepositContract() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        depositContract.setId(UUID.randomUUID());

        // Create the DepositContract
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDepositContractMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the DepositContract in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDepositContractWithPatch() throws Exception {
        // Initialize the database
        insertedDepositContract = depositContractRepository.saveAndFlush(depositContract);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the depositContract using partial update
        DepositContract partialUpdatedDepositContract = new DepositContract();
        partialUpdatedDepositContract.setId(depositContract.getId());

        partialUpdatedDepositContract.source(UPDATED_SOURCE).encoding(UPDATED_ENCODING).separator(UPDATED_SEPARATOR);

        restDepositContractMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDepositContract.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDepositContract))
            )
            .andExpect(status().isOk());

        // Validate the DepositContract in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDepositContractUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedDepositContract, depositContract),
            getPersistedDepositContract(depositContract)
        );
    }

    @Test
    @Transactional
    void fullUpdateDepositContractWithPatch() throws Exception {
        // Initialize the database
        insertedDepositContract = depositContractRepository.saveAndFlush(depositContract);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the depositContract using partial update
        DepositContract partialUpdatedDepositContract = new DepositContract();
        partialUpdatedDepositContract.setId(depositContract.getId());

        partialUpdatedDepositContract
            .source(UPDATED_SOURCE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .format(UPDATED_FORMAT)
            .requiredColumns(UPDATED_REQUIRED_COLUMNS)
            .encoding(UPDATED_ENCODING)
            .separator(UPDATED_SEPARATOR)
            .maxSizeBytes(UPDATED_MAX_SIZE_BYTES)
            .timestampTolerance(UPDATED_TIMESTAMP_TOLERANCE)
            .active(UPDATED_ACTIVE);

        restDepositContractMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDepositContract.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDepositContract))
            )
            .andExpect(status().isOk());

        // Validate the DepositContract in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDepositContractUpdatableFieldsEquals(
            partialUpdatedDepositContract,
            getPersistedDepositContract(partialUpdatedDepositContract)
        );
    }

    @Test
    @Transactional
    void patchNonExistingDepositContract() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        depositContract.setId(UUID.randomUUID());

        // Create the DepositContract
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDepositContractMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, depositContractDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DepositContract in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDepositContract() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        depositContract.setId(UUID.randomUUID());

        // Create the DepositContract
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDepositContractMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DepositContract in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDepositContract() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        depositContract.setId(UUID.randomUUID());

        // Create the DepositContract
        DepositContractDTO depositContractDTO = depositContractMapper.toDto(depositContract);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDepositContractMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(depositContractDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the DepositContract in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDepositContract() throws Exception {
        // Initialize the database
        insertedDepositContract = depositContractRepository.saveAndFlush(depositContract);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the depositContract
        restDepositContractMockMvc
            .perform(delete(ENTITY_API_URL_ID, depositContract.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return depositContractRepository.count();
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

    protected DepositContract getPersistedDepositContract(DepositContract depositContract) {
        return depositContractRepository.findById(depositContract.getId()).orElseThrow();
    }

    protected void assertPersistedDepositContractToMatchAllProperties(DepositContract expectedDepositContract) {
        assertDepositContractAllPropertiesEquals(expectedDepositContract, getPersistedDepositContract(expectedDepositContract));
    }

    protected void assertPersistedDepositContractToMatchUpdatableProperties(DepositContract expectedDepositContract) {
        assertDepositContractAllUpdatablePropertiesEquals(expectedDepositContract, getPersistedDepositContract(expectedDepositContract));
    }
}
