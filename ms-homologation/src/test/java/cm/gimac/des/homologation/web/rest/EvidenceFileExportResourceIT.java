package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.EvidenceFileExportAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.EvidenceFileExport;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.repository.EvidenceFileExportRepository;
import cm.gimac.des.homologation.service.dto.EvidenceFileExportDTO;
import cm.gimac.des.homologation.service.mapper.EvidenceFileExportMapper;
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
 * Integration tests for the {@link EvidenceFileExportResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class EvidenceFileExportResourceIT {

    private static final String DEFAULT_MANIFESTE = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_MANIFESTE = "[\"B\"]";

    private static final String DEFAULT_EMPREINTE_MANIFESTE = "AAAAAAAAAA";
    private static final String UPDATED_EMPREINTE_MANIFESTE = "BBBBBBBBBB";

    private static final String DEFAULT_DOCUMENT_REF = "AAAAAAAAAA";
    private static final String UPDATED_DOCUMENT_REF = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_EXPORT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_EXPORT = Instant.ofEpochMilli(1701958935371L);

    private static final UUID DEFAULT_DEMANDEUR_ID = UUID.randomUUID();
    private static final UUID UPDATED_DEMANDEUR_ID = UUID.randomUUID();

    private static final String DEFAULT_SOCLE_EMPREINTE = "AAAAAAAAAA";
    private static final String UPDATED_SOCLE_EMPREINTE = "BBBBBBBBBB";

    private static final String DEFAULT_ETAT = "AAAAAAAAAA";
    private static final String UPDATED_ETAT = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/evidence-file-exports";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EvidenceFileExportRepository evidenceFileExportRepository;

    @Autowired
    private EvidenceFileExportMapper evidenceFileExportMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEvidenceFileExportMockMvc;

    private EvidenceFileExport evidenceFileExport;

    private EvidenceFileExport insertedEvidenceFileExport;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EvidenceFileExport createEntity(EntityManager em) {
        EvidenceFileExport evidenceFileExport = new EvidenceFileExport()
            .manifeste(DEFAULT_MANIFESTE)
            .empreinteManifeste(DEFAULT_EMPREINTE_MANIFESTE)
            .documentRef(DEFAULT_DOCUMENT_REF)
            .dateExport(DEFAULT_DATE_EXPORT)
            .demandeurId(DEFAULT_DEMANDEUR_ID)
            .socleEmpreinte(DEFAULT_SOCLE_EMPREINTE)
            .etat(DEFAULT_ETAT);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        evidenceFileExport.setLiaison(liaison);
        return evidenceFileExport;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EvidenceFileExport createUpdatedEntity(EntityManager em) {
        EvidenceFileExport updatedEvidenceFileExport = new EvidenceFileExport()
            .manifeste(UPDATED_MANIFESTE)
            .empreinteManifeste(UPDATED_EMPREINTE_MANIFESTE)
            .documentRef(UPDATED_DOCUMENT_REF)
            .dateExport(UPDATED_DATE_EXPORT)
            .demandeurId(UPDATED_DEMANDEUR_ID)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .etat(UPDATED_ETAT);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        updatedEvidenceFileExport.setLiaison(liaison);
        return updatedEvidenceFileExport;
    }

    @BeforeEach
    void initTest() {
        evidenceFileExport = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedEvidenceFileExport != null) {
            evidenceFileExportRepository.delete(insertedEvidenceFileExport);
            insertedEvidenceFileExport = null;
        }
    }

    @Test
    @Transactional
    void createEvidenceFileExport() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the EvidenceFileExport
        EvidenceFileExportDTO evidenceFileExportDTO = evidenceFileExportMapper.toDto(evidenceFileExport);
        var returnedEvidenceFileExportDTO = om.readValue(
            restEvidenceFileExportMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(evidenceFileExportDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            EvidenceFileExportDTO.class
        );

        // Validate the EvidenceFileExport in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedEvidenceFileExport = evidenceFileExportMapper.toEntity(returnedEvidenceFileExportDTO);
        assertEvidenceFileExportUpdatableFieldsEquals(
            returnedEvidenceFileExport,
            getPersistedEvidenceFileExport(returnedEvidenceFileExport)
        );

        insertedEvidenceFileExport = returnedEvidenceFileExport;
    }

    @Test
    @Transactional
    void createEvidenceFileExportWithExistingId() throws Exception {
        // Create the EvidenceFileExport with an existing ID
        insertedEvidenceFileExport = evidenceFileExportRepository.saveAndFlush(evidenceFileExport);
        EvidenceFileExportDTO evidenceFileExportDTO = evidenceFileExportMapper.toDto(evidenceFileExport);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restEvidenceFileExportMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceFileExportDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvidenceFileExport in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllEvidenceFileExports() throws Exception {
        // Initialize the database
        insertedEvidenceFileExport = evidenceFileExportRepository.saveAndFlush(evidenceFileExport);

        // Get all the evidenceFileExportList
        restEvidenceFileExportMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(evidenceFileExport.getId().toString())))
            .andExpect(jsonPath("$.[*].manifeste").value(hasItem(DEFAULT_MANIFESTE)))
            .andExpect(jsonPath("$.[*].empreinteManifeste").value(hasItem(DEFAULT_EMPREINTE_MANIFESTE)))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF)))
            .andExpect(jsonPath("$.[*].dateExport").value(hasItem(DEFAULT_DATE_EXPORT.toString())))
            .andExpect(jsonPath("$.[*].demandeurId").value(hasItem(DEFAULT_DEMANDEUR_ID.toString())))
            .andExpect(jsonPath("$.[*].socleEmpreinte").value(hasItem(DEFAULT_SOCLE_EMPREINTE)))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT)));
    }

    @Test
    @Transactional
    void getEvidenceFileExport() throws Exception {
        // Initialize the database
        insertedEvidenceFileExport = evidenceFileExportRepository.saveAndFlush(evidenceFileExport);

        // Get the evidenceFileExport
        restEvidenceFileExportMockMvc
            .perform(get(ENTITY_API_URL_ID, evidenceFileExport.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(evidenceFileExport.getId().toString()))
            .andExpect(jsonPath("$.manifeste").value(DEFAULT_MANIFESTE))
            .andExpect(jsonPath("$.empreinteManifeste").value(DEFAULT_EMPREINTE_MANIFESTE))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF))
            .andExpect(jsonPath("$.dateExport").value(DEFAULT_DATE_EXPORT.toString()))
            .andExpect(jsonPath("$.demandeurId").value(DEFAULT_DEMANDEUR_ID.toString()))
            .andExpect(jsonPath("$.socleEmpreinte").value(DEFAULT_SOCLE_EMPREINTE))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT));
    }

    @Test
    @Transactional
    void getNonExistingEvidenceFileExport() throws Exception {
        // Get the evidenceFileExport
        restEvidenceFileExportMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingEvidenceFileExport() throws Exception {
        // Initialize the database
        insertedEvidenceFileExport = evidenceFileExportRepository.saveAndFlush(evidenceFileExport);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evidenceFileExport
        EvidenceFileExport updatedEvidenceFileExport = evidenceFileExportRepository.findById(evidenceFileExport.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedEvidenceFileExport are not directly saved in db
        em.detach(updatedEvidenceFileExport);
        updatedEvidenceFileExport
            .manifeste(UPDATED_MANIFESTE)
            .empreinteManifeste(UPDATED_EMPREINTE_MANIFESTE)
            .documentRef(UPDATED_DOCUMENT_REF)
            .dateExport(UPDATED_DATE_EXPORT)
            .demandeurId(UPDATED_DEMANDEUR_ID)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .etat(UPDATED_ETAT);
        EvidenceFileExportDTO evidenceFileExportDTO = evidenceFileExportMapper.toDto(updatedEvidenceFileExport);

        restEvidenceFileExportMockMvc
            .perform(
                put(ENTITY_API_URL_ID, evidenceFileExportDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceFileExportDTO))
            )
            .andExpect(status().isOk());

        // Validate the EvidenceFileExport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedEvidenceFileExportToMatchAllProperties(updatedEvidenceFileExport);
    }

    @Test
    @Transactional
    void putNonExistingEvidenceFileExport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceFileExport.setId(UUID.randomUUID());

        // Create the EvidenceFileExport
        EvidenceFileExportDTO evidenceFileExportDTO = evidenceFileExportMapper.toDto(evidenceFileExport);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEvidenceFileExportMockMvc
            .perform(
                put(ENTITY_API_URL_ID, evidenceFileExportDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceFileExportDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvidenceFileExport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchEvidenceFileExport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceFileExport.setId(UUID.randomUUID());

        // Create the EvidenceFileExport
        EvidenceFileExportDTO evidenceFileExportDTO = evidenceFileExportMapper.toDto(evidenceFileExport);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceFileExportMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceFileExportDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvidenceFileExport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamEvidenceFileExport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceFileExport.setId(UUID.randomUUID());

        // Create the EvidenceFileExport
        EvidenceFileExportDTO evidenceFileExportDTO = evidenceFileExportMapper.toDto(evidenceFileExport);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceFileExportMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceFileExportDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the EvidenceFileExport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateEvidenceFileExportWithPatch() throws Exception {
        // Initialize the database
        insertedEvidenceFileExport = evidenceFileExportRepository.saveAndFlush(evidenceFileExport);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evidenceFileExport using partial update
        EvidenceFileExport partialUpdatedEvidenceFileExport = new EvidenceFileExport();
        partialUpdatedEvidenceFileExport.setId(evidenceFileExport.getId());

        partialUpdatedEvidenceFileExport
            .documentRef(UPDATED_DOCUMENT_REF)
            .demandeurId(UPDATED_DEMANDEUR_ID)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE);

        restEvidenceFileExportMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvidenceFileExport.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvidenceFileExport))
            )
            .andExpect(status().isOk());

        // Validate the EvidenceFileExport in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEvidenceFileExportUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedEvidenceFileExport, evidenceFileExport),
            getPersistedEvidenceFileExport(evidenceFileExport)
        );
    }

    @Test
    @Transactional
    void fullUpdateEvidenceFileExportWithPatch() throws Exception {
        // Initialize the database
        insertedEvidenceFileExport = evidenceFileExportRepository.saveAndFlush(evidenceFileExport);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evidenceFileExport using partial update
        EvidenceFileExport partialUpdatedEvidenceFileExport = new EvidenceFileExport();
        partialUpdatedEvidenceFileExport.setId(evidenceFileExport.getId());

        partialUpdatedEvidenceFileExport
            .manifeste(UPDATED_MANIFESTE)
            .empreinteManifeste(UPDATED_EMPREINTE_MANIFESTE)
            .documentRef(UPDATED_DOCUMENT_REF)
            .dateExport(UPDATED_DATE_EXPORT)
            .demandeurId(UPDATED_DEMANDEUR_ID)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .etat(UPDATED_ETAT);

        restEvidenceFileExportMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvidenceFileExport.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvidenceFileExport))
            )
            .andExpect(status().isOk());

        // Validate the EvidenceFileExport in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEvidenceFileExportUpdatableFieldsEquals(
            partialUpdatedEvidenceFileExport,
            getPersistedEvidenceFileExport(partialUpdatedEvidenceFileExport)
        );
    }

    @Test
    @Transactional
    void patchNonExistingEvidenceFileExport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceFileExport.setId(UUID.randomUUID());

        // Create the EvidenceFileExport
        EvidenceFileExportDTO evidenceFileExportDTO = evidenceFileExportMapper.toDto(evidenceFileExport);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEvidenceFileExportMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, evidenceFileExportDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(evidenceFileExportDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvidenceFileExport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchEvidenceFileExport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceFileExport.setId(UUID.randomUUID());

        // Create the EvidenceFileExport
        EvidenceFileExportDTO evidenceFileExportDTO = evidenceFileExportMapper.toDto(evidenceFileExport);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceFileExportMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(evidenceFileExportDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvidenceFileExport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamEvidenceFileExport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceFileExport.setId(UUID.randomUUID());

        // Create the EvidenceFileExport
        EvidenceFileExportDTO evidenceFileExportDTO = evidenceFileExportMapper.toDto(evidenceFileExport);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceFileExportMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(evidenceFileExportDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the EvidenceFileExport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteEvidenceFileExport() throws Exception {
        // Initialize the database
        insertedEvidenceFileExport = evidenceFileExportRepository.saveAndFlush(evidenceFileExport);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the evidenceFileExport
        restEvidenceFileExportMockMvc
            .perform(delete(ENTITY_API_URL_ID, evidenceFileExport.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return evidenceFileExportRepository.count();
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

    protected EvidenceFileExport getPersistedEvidenceFileExport(EvidenceFileExport evidenceFileExport) {
        return evidenceFileExportRepository.findById(evidenceFileExport.getId()).orElseThrow();
    }

    protected void assertPersistedEvidenceFileExportToMatchAllProperties(EvidenceFileExport expectedEvidenceFileExport) {
        assertEvidenceFileExportAllPropertiesEquals(expectedEvidenceFileExport, getPersistedEvidenceFileExport(expectedEvidenceFileExport));
    }

    protected void assertPersistedEvidenceFileExportToMatchUpdatableProperties(EvidenceFileExport expectedEvidenceFileExport) {
        assertEvidenceFileExportAllUpdatablePropertiesEquals(
            expectedEvidenceFileExport,
            getPersistedEvidenceFileExport(expectedEvidenceFileExport)
        );
    }
}
