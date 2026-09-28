package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.QualificationMatrixAsserts.*;
import static cm.gimac.des.incident.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.QualificationMatrix;
import cm.gimac.des.incident.repository.QualificationMatrixRepository;
import cm.gimac.des.incident.service.dto.QualificationMatrixDTO;
import cm.gimac.des.incident.service.mapper.QualificationMatrixMapper;
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
 * Integration tests for the {@link QualificationMatrixResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class QualificationMatrixResourceIT {

    private static final String DEFAULT_MATRIX_VERSION = "AAAAAAAAAA";
    private static final String UPDATED_MATRIX_VERSION = "BBBBBBBBBB";

    private static final String DEFAULT_CELLS = "AAAAAAAAAA";
    private static final String UPDATED_CELLS = "BBBBBBBBBB";

    private static final String DEFAULT_REGULATORY_RULES = "AAAAAAAAAA";
    private static final String UPDATED_REGULATORY_RULES = "BBBBBBBBBB";

    private static final Instant DEFAULT_EFFECTIVE_FROM = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EFFECTIVE_FROM = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/qualification-matrices";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private QualificationMatrixRepository qualificationMatrixRepository;

    @Autowired
    private QualificationMatrixMapper qualificationMatrixMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restQualificationMatrixMockMvc;

    private QualificationMatrix qualificationMatrix;

    private QualificationMatrix insertedQualificationMatrix;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static QualificationMatrix createEntity() {
        return new QualificationMatrix()
            .matrixVersion(DEFAULT_MATRIX_VERSION)
            .cells(DEFAULT_CELLS)
            .regulatoryRules(DEFAULT_REGULATORY_RULES)
            .effectiveFrom(DEFAULT_EFFECTIVE_FROM)
            .active(DEFAULT_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static QualificationMatrix createUpdatedEntity() {
        return new QualificationMatrix()
            .matrixVersion(UPDATED_MATRIX_VERSION)
            .cells(UPDATED_CELLS)
            .regulatoryRules(UPDATED_REGULATORY_RULES)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .active(UPDATED_ACTIVE);
    }

    @BeforeEach
    void initTest() {
        qualificationMatrix = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedQualificationMatrix != null) {
            qualificationMatrixRepository.delete(insertedQualificationMatrix);
            insertedQualificationMatrix = null;
        }
    }

    @Test
    @Transactional
    void createQualificationMatrix() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the QualificationMatrix
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(qualificationMatrix);
        var returnedQualificationMatrixDTO = om.readValue(
            restQualificationMatrixMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(qualificationMatrixDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            QualificationMatrixDTO.class
        );

        // Validate the QualificationMatrix in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedQualificationMatrix = qualificationMatrixMapper.toEntity(returnedQualificationMatrixDTO);
        assertQualificationMatrixUpdatableFieldsEquals(
            returnedQualificationMatrix,
            getPersistedQualificationMatrix(returnedQualificationMatrix)
        );

        insertedQualificationMatrix = returnedQualificationMatrix;
    }

    @Test
    @Transactional
    void createQualificationMatrixWithExistingId() throws Exception {
        // Create the QualificationMatrix with an existing ID
        insertedQualificationMatrix = qualificationMatrixRepository.saveAndFlush(qualificationMatrix);
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(qualificationMatrix);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restQualificationMatrixMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(qualificationMatrixDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the QualificationMatrix in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkMatrixVersionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        qualificationMatrix.setMatrixVersion(null);

        // Create the QualificationMatrix, which fails.
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(qualificationMatrix);

        restQualificationMatrixMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(qualificationMatrixDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEffectiveFromIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        qualificationMatrix.setEffectiveFrom(null);

        // Create the QualificationMatrix, which fails.
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(qualificationMatrix);

        restQualificationMatrixMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(qualificationMatrixDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        qualificationMatrix.setActive(null);

        // Create the QualificationMatrix, which fails.
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(qualificationMatrix);

        restQualificationMatrixMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(qualificationMatrixDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllQualificationMatrices() throws Exception {
        // Initialize the database
        insertedQualificationMatrix = qualificationMatrixRepository.saveAndFlush(qualificationMatrix);

        // Get all the qualificationMatrixList
        restQualificationMatrixMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(qualificationMatrix.getId().toString())))
            .andExpect(jsonPath("$.[*].matrixVersion").value(hasItem(DEFAULT_MATRIX_VERSION)))
            .andExpect(jsonPath("$.[*].cells").value(hasItem(DEFAULT_CELLS)))
            .andExpect(jsonPath("$.[*].regulatoryRules").value(hasItem(DEFAULT_REGULATORY_RULES)))
            .andExpect(jsonPath("$.[*].effectiveFrom").value(hasItem(DEFAULT_EFFECTIVE_FROM.toString())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @Test
    @Transactional
    void getQualificationMatrix() throws Exception {
        // Initialize the database
        insertedQualificationMatrix = qualificationMatrixRepository.saveAndFlush(qualificationMatrix);

        // Get the qualificationMatrix
        restQualificationMatrixMockMvc
            .perform(get(ENTITY_API_URL_ID, qualificationMatrix.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(qualificationMatrix.getId().toString()))
            .andExpect(jsonPath("$.matrixVersion").value(DEFAULT_MATRIX_VERSION))
            .andExpect(jsonPath("$.cells").value(DEFAULT_CELLS))
            .andExpect(jsonPath("$.regulatoryRules").value(DEFAULT_REGULATORY_RULES))
            .andExpect(jsonPath("$.effectiveFrom").value(DEFAULT_EFFECTIVE_FROM.toString()))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingQualificationMatrix() throws Exception {
        // Get the qualificationMatrix
        restQualificationMatrixMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingQualificationMatrix() throws Exception {
        // Initialize the database
        insertedQualificationMatrix = qualificationMatrixRepository.saveAndFlush(qualificationMatrix);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the qualificationMatrix
        QualificationMatrix updatedQualificationMatrix = qualificationMatrixRepository.findById(qualificationMatrix.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedQualificationMatrix are not directly saved in db
        em.detach(updatedQualificationMatrix);
        updatedQualificationMatrix
            .matrixVersion(UPDATED_MATRIX_VERSION)
            .cells(UPDATED_CELLS)
            .regulatoryRules(UPDATED_REGULATORY_RULES)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .active(UPDATED_ACTIVE);
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(updatedQualificationMatrix);

        restQualificationMatrixMockMvc
            .perform(
                put(ENTITY_API_URL_ID, qualificationMatrixDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(qualificationMatrixDTO))
            )
            .andExpect(status().isOk());

        // Validate the QualificationMatrix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedQualificationMatrixToMatchAllProperties(updatedQualificationMatrix);
    }

    @Test
    @Transactional
    void putNonExistingQualificationMatrix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qualificationMatrix.setId(UUID.randomUUID());

        // Create the QualificationMatrix
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(qualificationMatrix);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restQualificationMatrixMockMvc
            .perform(
                put(ENTITY_API_URL_ID, qualificationMatrixDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(qualificationMatrixDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the QualificationMatrix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchQualificationMatrix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qualificationMatrix.setId(UUID.randomUUID());

        // Create the QualificationMatrix
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(qualificationMatrix);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQualificationMatrixMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(qualificationMatrixDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the QualificationMatrix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamQualificationMatrix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qualificationMatrix.setId(UUID.randomUUID());

        // Create the QualificationMatrix
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(qualificationMatrix);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQualificationMatrixMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(qualificationMatrixDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the QualificationMatrix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateQualificationMatrixWithPatch() throws Exception {
        // Initialize the database
        insertedQualificationMatrix = qualificationMatrixRepository.saveAndFlush(qualificationMatrix);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the qualificationMatrix using partial update
        QualificationMatrix partialUpdatedQualificationMatrix = new QualificationMatrix();
        partialUpdatedQualificationMatrix.setId(qualificationMatrix.getId());

        partialUpdatedQualificationMatrix.effectiveFrom(UPDATED_EFFECTIVE_FROM);

        restQualificationMatrixMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedQualificationMatrix.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedQualificationMatrix))
            )
            .andExpect(status().isOk());

        // Validate the QualificationMatrix in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertQualificationMatrixUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedQualificationMatrix, qualificationMatrix),
            getPersistedQualificationMatrix(qualificationMatrix)
        );
    }

    @Test
    @Transactional
    void fullUpdateQualificationMatrixWithPatch() throws Exception {
        // Initialize the database
        insertedQualificationMatrix = qualificationMatrixRepository.saveAndFlush(qualificationMatrix);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the qualificationMatrix using partial update
        QualificationMatrix partialUpdatedQualificationMatrix = new QualificationMatrix();
        partialUpdatedQualificationMatrix.setId(qualificationMatrix.getId());

        partialUpdatedQualificationMatrix
            .matrixVersion(UPDATED_MATRIX_VERSION)
            .cells(UPDATED_CELLS)
            .regulatoryRules(UPDATED_REGULATORY_RULES)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .active(UPDATED_ACTIVE);

        restQualificationMatrixMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedQualificationMatrix.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedQualificationMatrix))
            )
            .andExpect(status().isOk());

        // Validate the QualificationMatrix in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertQualificationMatrixUpdatableFieldsEquals(
            partialUpdatedQualificationMatrix,
            getPersistedQualificationMatrix(partialUpdatedQualificationMatrix)
        );
    }

    @Test
    @Transactional
    void patchNonExistingQualificationMatrix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qualificationMatrix.setId(UUID.randomUUID());

        // Create the QualificationMatrix
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(qualificationMatrix);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restQualificationMatrixMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, qualificationMatrixDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(qualificationMatrixDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the QualificationMatrix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchQualificationMatrix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qualificationMatrix.setId(UUID.randomUUID());

        // Create the QualificationMatrix
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(qualificationMatrix);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQualificationMatrixMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(qualificationMatrixDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the QualificationMatrix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamQualificationMatrix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qualificationMatrix.setId(UUID.randomUUID());

        // Create the QualificationMatrix
        QualificationMatrixDTO qualificationMatrixDTO = qualificationMatrixMapper.toDto(qualificationMatrix);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQualificationMatrixMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(qualificationMatrixDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the QualificationMatrix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteQualificationMatrix() throws Exception {
        // Initialize the database
        insertedQualificationMatrix = qualificationMatrixRepository.saveAndFlush(qualificationMatrix);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the qualificationMatrix
        restQualificationMatrixMockMvc
            .perform(delete(ENTITY_API_URL_ID, qualificationMatrix.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return qualificationMatrixRepository.count();
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

    protected QualificationMatrix getPersistedQualificationMatrix(QualificationMatrix qualificationMatrix) {
        return qualificationMatrixRepository.findById(qualificationMatrix.getId()).orElseThrow();
    }

    protected void assertPersistedQualificationMatrixToMatchAllProperties(QualificationMatrix expectedQualificationMatrix) {
        assertQualificationMatrixAllPropertiesEquals(
            expectedQualificationMatrix,
            getPersistedQualificationMatrix(expectedQualificationMatrix)
        );
    }

    protected void assertPersistedQualificationMatrixToMatchUpdatableProperties(QualificationMatrix expectedQualificationMatrix) {
        assertQualificationMatrixAllUpdatablePropertiesEquals(
            expectedQualificationMatrix,
            getPersistedQualificationMatrix(expectedQualificationMatrix)
        );
    }
}
