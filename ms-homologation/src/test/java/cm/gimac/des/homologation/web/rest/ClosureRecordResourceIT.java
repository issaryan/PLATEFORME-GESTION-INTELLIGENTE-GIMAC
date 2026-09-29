package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.ClosureRecordAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.ClosureRecord;
import cm.gimac.des.homologation.domain.OperationInstance;
import cm.gimac.des.homologation.repository.ClosureRecordRepository;
import cm.gimac.des.homologation.service.dto.ClosureRecordDTO;
import cm.gimac.des.homologation.service.mapper.ClosureRecordMapper;
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
 * Integration tests for the {@link ClosureRecordResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.homologation.security.AuthoritiesConstants.ADMIN)
class ClosureRecordResourceIT {

    private static final UUID DEFAULT_VALIDATEUR_ID = UUID.randomUUID();
    private static final UUID UPDATED_VALIDATEUR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_DATE_VALIDATION = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_VALIDATION = Instant.ofEpochMilli(1701958935371L);

    private static final String DEFAULT_EMPREINTE_CLOTURE = "AAAAAAAAAA";
    private static final String UPDATED_EMPREINTE_CLOTURE = "BBBBBBBBBB";

    private static final String DEFAULT_JETON_HORODATAGE = "AAAAAAAAAA";
    private static final String UPDATED_JETON_HORODATAGE = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_HORODATAGE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_HORODATAGE = Instant.ofEpochMilli(1701958935371L);

    private static final Boolean DEFAULT_ACTIF = false;
    private static final Boolean UPDATED_ACTIF = true;

    private static final String DEFAULT_MOTIF_REOUVERTURE = "AAAAAAAAAA";
    private static final String UPDATED_MOTIF_REOUVERTURE = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_REOUVERTURE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_REOUVERTURE = Instant.ofEpochMilli(1701958935371L);

    private static final String ENTITY_API_URL = "/api/closure-records";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ClosureRecordRepository closureRecordRepository;

    @Autowired
    private ClosureRecordMapper closureRecordMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restClosureRecordMockMvc;

    private ClosureRecord closureRecord;

    private ClosureRecord insertedClosureRecord;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ClosureRecord createEntity(EntityManager em) {
        ClosureRecord closureRecord = new ClosureRecord()
            .validateurId(DEFAULT_VALIDATEUR_ID)
            .dateValidation(DEFAULT_DATE_VALIDATION)
            .empreinteCloture(DEFAULT_EMPREINTE_CLOTURE)
            .jetonHorodatage(DEFAULT_JETON_HORODATAGE)
            .dateHorodatage(DEFAULT_DATE_HORODATAGE)
            .actif(DEFAULT_ACTIF)
            .motifReouverture(DEFAULT_MOTIF_REOUVERTURE)
            .dateReouverture(DEFAULT_DATE_REOUVERTURE);
        // Add required entity
        OperationInstance operationInstance;
        if (TestUtil.findAll(em, OperationInstance.class).isEmpty()) {
            operationInstance = OperationInstanceResourceIT.createEntity(em);
            em.persist(operationInstance);
            em.flush();
        } else {
            operationInstance = TestUtil.findAll(em, OperationInstance.class).getFirst();
        }
        closureRecord.setOperation(operationInstance);
        return closureRecord;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ClosureRecord createUpdatedEntity(EntityManager em) {
        ClosureRecord updatedClosureRecord = new ClosureRecord()
            .validateurId(UPDATED_VALIDATEUR_ID)
            .dateValidation(UPDATED_DATE_VALIDATION)
            .empreinteCloture(UPDATED_EMPREINTE_CLOTURE)
            .jetonHorodatage(UPDATED_JETON_HORODATAGE)
            .dateHorodatage(UPDATED_DATE_HORODATAGE)
            .actif(UPDATED_ACTIF)
            .motifReouverture(UPDATED_MOTIF_REOUVERTURE)
            .dateReouverture(UPDATED_DATE_REOUVERTURE);
        // Add required entity
        OperationInstance operationInstance;
        if (TestUtil.findAll(em, OperationInstance.class).isEmpty()) {
            operationInstance = OperationInstanceResourceIT.createUpdatedEntity(em);
            em.persist(operationInstance);
            em.flush();
        } else {
            operationInstance = TestUtil.findAll(em, OperationInstance.class).getFirst();
        }
        updatedClosureRecord.setOperation(operationInstance);
        return updatedClosureRecord;
    }

    @BeforeEach
    void initTest() {
        closureRecord = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedClosureRecord != null) {
            closureRecordRepository.delete(insertedClosureRecord);
            insertedClosureRecord = null;
        }
    }

    @Test
    @Transactional
    void createClosureRecord() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ClosureRecord
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);
        var returnedClosureRecordDTO = om.readValue(
            restClosureRecordMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(closureRecordDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ClosureRecordDTO.class
        );

        // Validate the ClosureRecord in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedClosureRecord = closureRecordMapper.toEntity(returnedClosureRecordDTO);
        assertClosureRecordUpdatableFieldsEquals(returnedClosureRecord, getPersistedClosureRecord(returnedClosureRecord));

        insertedClosureRecord = returnedClosureRecord;
    }

    @Test
    @Transactional
    void createClosureRecordWithExistingId() throws Exception {
        // Create the ClosureRecord with an existing ID
        insertedClosureRecord = closureRecordRepository.saveAndFlush(closureRecord);
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restClosureRecordMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ClosureRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkValidateurIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        closureRecord.setValidateurId(null);

        // Create the ClosureRecord, which fails.
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);

        restClosureRecordMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateValidationIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        closureRecord.setDateValidation(null);

        // Create the ClosureRecord, which fails.
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);

        restClosureRecordMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEmpreinteClotureIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        closureRecord.setEmpreinteCloture(null);

        // Create the ClosureRecord, which fails.
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);

        restClosureRecordMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActifIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        closureRecord.setActif(null);

        // Create the ClosureRecord, which fails.
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);

        restClosureRecordMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllClosureRecords() throws Exception {
        // Initialize the database
        insertedClosureRecord = closureRecordRepository.saveAndFlush(closureRecord);

        // Get all the closureRecordList
        restClosureRecordMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(closureRecord.getId().toString())))
            .andExpect(jsonPath("$.[*].validateurId").value(hasItem(DEFAULT_VALIDATEUR_ID.toString())))
            .andExpect(jsonPath("$.[*].dateValidation").value(hasItem(DEFAULT_DATE_VALIDATION.toString())))
            .andExpect(jsonPath("$.[*].empreinteCloture").value(hasItem(DEFAULT_EMPREINTE_CLOTURE)))
            .andExpect(jsonPath("$.[*].jetonHorodatage").value(hasItem(DEFAULT_JETON_HORODATAGE)))
            .andExpect(jsonPath("$.[*].dateHorodatage").value(hasItem(DEFAULT_DATE_HORODATAGE.toString())))
            .andExpect(jsonPath("$.[*].actif").value(hasItem(DEFAULT_ACTIF)))
            .andExpect(jsonPath("$.[*].motifReouverture").value(hasItem(DEFAULT_MOTIF_REOUVERTURE)))
            .andExpect(jsonPath("$.[*].dateReouverture").value(hasItem(DEFAULT_DATE_REOUVERTURE.toString())));
    }

    @Test
    @Transactional
    void getClosureRecord() throws Exception {
        // Initialize the database
        insertedClosureRecord = closureRecordRepository.saveAndFlush(closureRecord);

        // Get the closureRecord
        restClosureRecordMockMvc
            .perform(get(ENTITY_API_URL_ID, closureRecord.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(closureRecord.getId().toString()))
            .andExpect(jsonPath("$.validateurId").value(DEFAULT_VALIDATEUR_ID.toString()))
            .andExpect(jsonPath("$.dateValidation").value(DEFAULT_DATE_VALIDATION.toString()))
            .andExpect(jsonPath("$.empreinteCloture").value(DEFAULT_EMPREINTE_CLOTURE))
            .andExpect(jsonPath("$.jetonHorodatage").value(DEFAULT_JETON_HORODATAGE))
            .andExpect(jsonPath("$.dateHorodatage").value(DEFAULT_DATE_HORODATAGE.toString()))
            .andExpect(jsonPath("$.actif").value(DEFAULT_ACTIF))
            .andExpect(jsonPath("$.motifReouverture").value(DEFAULT_MOTIF_REOUVERTURE))
            .andExpect(jsonPath("$.dateReouverture").value(DEFAULT_DATE_REOUVERTURE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingClosureRecord() throws Exception {
        // Get the closureRecord
        restClosureRecordMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingClosureRecord() throws Exception {
        // Initialize the database
        insertedClosureRecord = closureRecordRepository.saveAndFlush(closureRecord);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the closureRecord
        ClosureRecord updatedClosureRecord = closureRecordRepository.findById(closureRecord.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedClosureRecord are not directly saved in db
        em.detach(updatedClosureRecord);
        updatedClosureRecord
            .validateurId(UPDATED_VALIDATEUR_ID)
            .dateValidation(UPDATED_DATE_VALIDATION)
            .empreinteCloture(UPDATED_EMPREINTE_CLOTURE)
            .jetonHorodatage(UPDATED_JETON_HORODATAGE)
            .dateHorodatage(UPDATED_DATE_HORODATAGE)
            .actif(UPDATED_ACTIF)
            .motifReouverture(UPDATED_MOTIF_REOUVERTURE)
            .dateReouverture(UPDATED_DATE_REOUVERTURE);
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(updatedClosureRecord);

        restClosureRecordMockMvc
            .perform(
                put(ENTITY_API_URL_ID, closureRecordDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isOk());

        // Validate the ClosureRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedClosureRecordToMatchAllProperties(updatedClosureRecord);
    }

    @Test
    @Transactional
    void putNonExistingClosureRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        closureRecord.setId(UUID.randomUUID());

        // Create the ClosureRecord
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restClosureRecordMockMvc
            .perform(
                put(ENTITY_API_URL_ID, closureRecordDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ClosureRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchClosureRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        closureRecord.setId(UUID.randomUUID());

        // Create the ClosureRecord
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restClosureRecordMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ClosureRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamClosureRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        closureRecord.setId(UUID.randomUUID());

        // Create the ClosureRecord
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restClosureRecordMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ClosureRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateClosureRecordWithPatch() throws Exception {
        // Initialize the database
        insertedClosureRecord = closureRecordRepository.saveAndFlush(closureRecord);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the closureRecord using partial update
        ClosureRecord partialUpdatedClosureRecord = new ClosureRecord();
        partialUpdatedClosureRecord.setId(closureRecord.getId());

        partialUpdatedClosureRecord
            .validateurId(UPDATED_VALIDATEUR_ID)
            .dateValidation(UPDATED_DATE_VALIDATION)
            .dateHorodatage(UPDATED_DATE_HORODATAGE)
            .actif(UPDATED_ACTIF)
            .motifReouverture(UPDATED_MOTIF_REOUVERTURE);

        restClosureRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedClosureRecord.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedClosureRecord))
            )
            .andExpect(status().isOk());

        // Validate the ClosureRecord in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertClosureRecordUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedClosureRecord, closureRecord),
            getPersistedClosureRecord(closureRecord)
        );
    }

    @Test
    @Transactional
    void fullUpdateClosureRecordWithPatch() throws Exception {
        // Initialize the database
        insertedClosureRecord = closureRecordRepository.saveAndFlush(closureRecord);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the closureRecord using partial update
        ClosureRecord partialUpdatedClosureRecord = new ClosureRecord();
        partialUpdatedClosureRecord.setId(closureRecord.getId());

        partialUpdatedClosureRecord
            .validateurId(UPDATED_VALIDATEUR_ID)
            .dateValidation(UPDATED_DATE_VALIDATION)
            .empreinteCloture(UPDATED_EMPREINTE_CLOTURE)
            .jetonHorodatage(UPDATED_JETON_HORODATAGE)
            .dateHorodatage(UPDATED_DATE_HORODATAGE)
            .actif(UPDATED_ACTIF)
            .motifReouverture(UPDATED_MOTIF_REOUVERTURE)
            .dateReouverture(UPDATED_DATE_REOUVERTURE);

        restClosureRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedClosureRecord.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedClosureRecord))
            )
            .andExpect(status().isOk());

        // Validate the ClosureRecord in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertClosureRecordUpdatableFieldsEquals(partialUpdatedClosureRecord, getPersistedClosureRecord(partialUpdatedClosureRecord));
    }

    @Test
    @Transactional
    void patchNonExistingClosureRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        closureRecord.setId(UUID.randomUUID());

        // Create the ClosureRecord
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restClosureRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, closureRecordDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ClosureRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchClosureRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        closureRecord.setId(UUID.randomUUID());

        // Create the ClosureRecord
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restClosureRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ClosureRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamClosureRecord() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        closureRecord.setId(UUID.randomUUID());

        // Create the ClosureRecord
        ClosureRecordDTO closureRecordDTO = closureRecordMapper.toDto(closureRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restClosureRecordMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(closureRecordDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ClosureRecord in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteClosureRecord() throws Exception {
        // Initialize the database
        insertedClosureRecord = closureRecordRepository.saveAndFlush(closureRecord);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the closureRecord
        restClosureRecordMockMvc
            .perform(delete(ENTITY_API_URL_ID, closureRecord.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return closureRecordRepository.count();
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

    protected ClosureRecord getPersistedClosureRecord(ClosureRecord closureRecord) {
        return closureRecordRepository.findById(closureRecord.getId()).orElseThrow();
    }

    protected void assertPersistedClosureRecordToMatchAllProperties(ClosureRecord expectedClosureRecord) {
        assertClosureRecordAllPropertiesEquals(expectedClosureRecord, getPersistedClosureRecord(expectedClosureRecord));
    }

    protected void assertPersistedClosureRecordToMatchUpdatableProperties(ClosureRecord expectedClosureRecord) {
        assertClosureRecordAllUpdatablePropertiesEquals(expectedClosureRecord, getPersistedClosureRecord(expectedClosureRecord));
    }
}
