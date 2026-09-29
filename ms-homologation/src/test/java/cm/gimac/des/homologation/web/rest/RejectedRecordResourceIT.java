package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.RejectedRecordAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.RejectedRecord;
import cm.gimac.des.homologation.repository.RejectedRecordRepository;
import cm.gimac.des.homologation.service.dto.RejectedRecordDTO;
import cm.gimac.des.homologation.service.mapper.RejectedRecordMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link RejectedRecordResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class RejectedRecordResourceIT {

    private static final String DEFAULT_SOURCE = "AAAAAAAAAA";
    private static final String UPDATED_SOURCE = "BBBBBBBBBB";

    private static final String DEFAULT_CHARGE_UTILE = "AAAAAAAAAA";
    private static final String UPDATED_CHARGE_UTILE = "BBBBBBBBBB";

    private static final String DEFAULT_EMPREINTE = "AAAAAAAAAA";
    private static final String UPDATED_EMPREINTE = "BBBBBBBBBB";

    private static final String DEFAULT_MOTIF = "AAAAAAAAAA";
    private static final String UPDATED_MOTIF = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_REJET = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_REJET = Instant.ofEpochMilli(1701958935371L);

    private static final Boolean DEFAULT_RELANCE = false;
    private static final Boolean UPDATED_RELANCE = true;

    private static final String ENTITY_API_URL = "/api/rejected-records";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RejectedRecordRepository rejectedRecordRepository;

    @Autowired
    private RejectedRecordMapper rejectedRecordMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRejectedRecordMockMvc;

    private RejectedRecord rejectedRecord;

    private RejectedRecord insertedRejectedRecord;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RejectedRecord createEntity() {
        return new RejectedRecord()
            .source(DEFAULT_SOURCE)
            .chargeUtile(DEFAULT_CHARGE_UTILE)
            .empreinte(DEFAULT_EMPREINTE)
            .motif(DEFAULT_MOTIF)
            .dateRejet(DEFAULT_DATE_REJET)
            .relance(DEFAULT_RELANCE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RejectedRecord createUpdatedEntity() {
        return new RejectedRecord()
            .source(UPDATED_SOURCE)
            .chargeUtile(UPDATED_CHARGE_UTILE)
            .empreinte(UPDATED_EMPREINTE)
            .motif(UPDATED_MOTIF)
            .dateRejet(UPDATED_DATE_REJET)
            .relance(UPDATED_RELANCE);
    }

    @BeforeEach
    void initTest() {
        rejectedRecord = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRejectedRecord != null) {
            rejectedRecordRepository.delete(insertedRejectedRecord);
            insertedRejectedRecord = null;
        }
    }

    @Test
    @Transactional
    void createRejectedRecord() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the RejectedRecord
        RejectedRecordDTO rejectedRecordDTO = rejectedRecordMapper.toDto(rejectedRecord);
        var returnedRejectedRecordDTO = om.readValue(
            restRejectedRecordMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(rejectedRecordDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RejectedRecordDTO.class
        );

        // Validate the RejectedRecord in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRejectedRecord = rejectedRecordMapper.toEntity(returnedRejectedRecordDTO);
        assertRejectedRecordUpdatableFieldsEquals(returnedRejectedRecord, getPersistedRejectedRecord(returnedRejectedRecord));

        insertedRejectedRecord = returnedRejectedRecord;
    }

    @Test
    @Transactional
    void createRejectedRecordWithExistingId() throws Exception {
        // Create the RejectedRecord with an existing ID
        insertedRejectedRecord = rejectedRecordRepository.saveAndFlush(rejectedRecord);
        RejectedRecordDTO rejectedRecordDTO = rejectedRecordMapper.toDto(rejectedRecord);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRejectedRecordMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(rejectedRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RejectedRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllRejectedRecords() throws Exception {
        // Initialize the database
        insertedRejectedRecord = rejectedRecordRepository.saveAndFlush(rejectedRecord);

        // Get all the rejectedRecordList
        restRejectedRecordMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(rejectedRecord.getId().toString())))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE)))
            .andExpect(jsonPath("$.[*].chargeUtile").value(hasItem(DEFAULT_CHARGE_UTILE)))
            .andExpect(jsonPath("$.[*].empreinte").value(hasItem(DEFAULT_EMPREINTE)))
            .andExpect(jsonPath("$.[*].motif").value(hasItem(DEFAULT_MOTIF)))
            .andExpect(jsonPath("$.[*].dateRejet").value(hasItem(DEFAULT_DATE_REJET.toString())))
            .andExpect(jsonPath("$.[*].relance").value(hasItem(DEFAULT_RELANCE)));
    }

    @Test
    @Transactional
    void getRejectedRecord() throws Exception {
        // Initialize the database
        insertedRejectedRecord = rejectedRecordRepository.saveAndFlush(rejectedRecord);

        // Get the rejectedRecord
        restRejectedRecordMockMvc
            .perform(get(ENTITY_API_URL_ID, rejectedRecord.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(rejectedRecord.getId().toString()))
            .andExpect(jsonPath("$.source").value(DEFAULT_SOURCE))
            .andExpect(jsonPath("$.chargeUtile").value(DEFAULT_CHARGE_UTILE))
            .andExpect(jsonPath("$.empreinte").value(DEFAULT_EMPREINTE))
            .andExpect(jsonPath("$.motif").value(DEFAULT_MOTIF))
            .andExpect(jsonPath("$.dateRejet").value(DEFAULT_DATE_REJET.toString()))
            .andExpect(jsonPath("$.relance").value(DEFAULT_RELANCE));
    }

    @Test
    @Transactional
    void getNonExistingRejectedRecord() throws Exception {
        // Get the rejectedRecord
        restRejectedRecordMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRejectedRecord() throws Exception {
        // Initialize the database
        insertedRejectedRecord = rejectedRecordRepository.saveAndFlush(rejectedRecord);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the rejectedRecord
        RejectedRecord updatedRejectedRecord = rejectedRecordRepository.findById(rejectedRecord.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRejectedRecord are not directly saved in db
        em.detach(updatedRejectedRecord);
        updatedRejectedRecord
            .source(UPDATED_SOURCE)
            .chargeUtile(UPDATED_CHARGE_UTILE)
            .empreinte(UPDATED_EMPREINTE)
            .motif(UPDATED_MOTIF)
            .dateRejet(UPDATED_DATE_REJET)
            .relance(UPDATED_RELANCE);
        RejectedRecordDTO rejectedRecordDTO = rejectedRecordMapper.toDto(updatedRejectedRecord);

        restRejectedRecordMockMvc
            .perform(
                put(ENTITY_API_URL_ID, rejectedRecordDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(rejectedRecordDTO))
            )
            .andExpect(status().isOk());

        // Validate the RejectedRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRejectedRecordToMatchAllProperties(updatedRejectedRecord);
    }

    @Test
    @Transactional
    void putNonExistingRejectedRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        rejectedRecord.setId(UUID.randomUUID());

        // Create the RejectedRecord
        RejectedRecordDTO rejectedRecordDTO = rejectedRecordMapper.toDto(rejectedRecord);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRejectedRecordMockMvc
            .perform(
                put(ENTITY_API_URL_ID, rejectedRecordDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(rejectedRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RejectedRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRejectedRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        rejectedRecord.setId(UUID.randomUUID());

        // Create the RejectedRecord
        RejectedRecordDTO rejectedRecordDTO = rejectedRecordMapper.toDto(rejectedRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRejectedRecordMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(rejectedRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RejectedRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRejectedRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        rejectedRecord.setId(UUID.randomUUID());

        // Create the RejectedRecord
        RejectedRecordDTO rejectedRecordDTO = rejectedRecordMapper.toDto(rejectedRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRejectedRecordMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(rejectedRecordDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the RejectedRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRejectedRecordWithPatch() throws Exception {
        // Initialize the database
        insertedRejectedRecord = rejectedRecordRepository.saveAndFlush(rejectedRecord);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the rejectedRecord using partial update
        RejectedRecord partialUpdatedRejectedRecord = new RejectedRecord();
        partialUpdatedRejectedRecord.setId(rejectedRecord.getId());

        partialUpdatedRejectedRecord.source(UPDATED_SOURCE).motif(UPDATED_MOTIF).relance(UPDATED_RELANCE);

        restRejectedRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRejectedRecord.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRejectedRecord))
            )
            .andExpect(status().isOk());

        // Validate the RejectedRecord in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRejectedRecordUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRejectedRecord, rejectedRecord),
            getPersistedRejectedRecord(rejectedRecord)
        );
    }

    @Test
    @Transactional
    void fullUpdateRejectedRecordWithPatch() throws Exception {
        // Initialize the database
        insertedRejectedRecord = rejectedRecordRepository.saveAndFlush(rejectedRecord);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the rejectedRecord using partial update
        RejectedRecord partialUpdatedRejectedRecord = new RejectedRecord();
        partialUpdatedRejectedRecord.setId(rejectedRecord.getId());

        partialUpdatedRejectedRecord
            .source(UPDATED_SOURCE)
            .chargeUtile(UPDATED_CHARGE_UTILE)
            .empreinte(UPDATED_EMPREINTE)
            .motif(UPDATED_MOTIF)
            .dateRejet(UPDATED_DATE_REJET)
            .relance(UPDATED_RELANCE);

        restRejectedRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRejectedRecord.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRejectedRecord))
            )
            .andExpect(status().isOk());

        // Validate the RejectedRecord in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRejectedRecordUpdatableFieldsEquals(partialUpdatedRejectedRecord, getPersistedRejectedRecord(partialUpdatedRejectedRecord));
    }

    @Test
    @Transactional
    void patchNonExistingRejectedRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        rejectedRecord.setId(UUID.randomUUID());

        // Create the RejectedRecord
        RejectedRecordDTO rejectedRecordDTO = rejectedRecordMapper.toDto(rejectedRecord);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRejectedRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, rejectedRecordDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(rejectedRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RejectedRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRejectedRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        rejectedRecord.setId(UUID.randomUUID());

        // Create the RejectedRecord
        RejectedRecordDTO rejectedRecordDTO = rejectedRecordMapper.toDto(rejectedRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRejectedRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(rejectedRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RejectedRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRejectedRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        rejectedRecord.setId(UUID.randomUUID());

        // Create the RejectedRecord
        RejectedRecordDTO rejectedRecordDTO = rejectedRecordMapper.toDto(rejectedRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRejectedRecordMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(rejectedRecordDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the RejectedRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRejectedRecord() throws Exception {
        // Initialize the database
        insertedRejectedRecord = rejectedRecordRepository.saveAndFlush(rejectedRecord);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the rejectedRecord
        restRejectedRecordMockMvc
            .perform(delete(ENTITY_API_URL_ID, rejectedRecord.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return rejectedRecordRepository.count();
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

    protected RejectedRecord getPersistedRejectedRecord(RejectedRecord rejectedRecord) {
        return rejectedRecordRepository.findById(rejectedRecord.getId()).orElseThrow();
    }

    protected void assertPersistedRejectedRecordToMatchAllProperties(RejectedRecord expectedRejectedRecord) {
        assertRejectedRecordAllPropertiesEquals(expectedRejectedRecord, getPersistedRejectedRecord(expectedRejectedRecord));
    }

    protected void assertPersistedRejectedRecordToMatchUpdatableProperties(RejectedRecord expectedRejectedRecord) {
        assertRejectedRecordAllUpdatablePropertiesEquals(expectedRejectedRecord, getPersistedRejectedRecord(expectedRejectedRecord));
    }
}
