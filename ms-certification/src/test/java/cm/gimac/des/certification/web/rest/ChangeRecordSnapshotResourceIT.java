package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.ChangeRecordSnapshotAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.ChangeRecordSnapshot;
import cm.gimac.des.certification.domain.GoLiveDossier;
import cm.gimac.des.certification.repository.ChangeRecordSnapshotRepository;
import cm.gimac.des.certification.service.dto.ChangeRecordSnapshotDTO;
import cm.gimac.des.certification.service.mapper.ChangeRecordSnapshotMapper;
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
 * Integration tests for the {@link ChangeRecordSnapshotResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ChangeRecordSnapshotResourceIT {

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_ETAT_APPROBATION = "AAAAAAAAAA";
    private static final String UPDATED_ETAT_APPROBATION = "BBBBBBBBBB";

    private static final Instant DEFAULT_FENETRE_DEBUT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FENETRE_DEBUT = Instant.ofEpochMilli(1702411306964L);

    private static final Instant DEFAULT_FENETRE_FIN = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FENETRE_FIN = Instant.ofEpochMilli(1702411306964L);

    private static final Instant DEFAULT_DATE_LECTURE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_LECTURE = Instant.ofEpochMilli(1702411306964L);

    private static final String DEFAULT_EMPREINTE_REPONSE = "AAAAAAAAAA";
    private static final String UPDATED_EMPREINTE_REPONSE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/change-record-snapshots";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ChangeRecordSnapshotRepository changeRecordSnapshotRepository;

    @Autowired
    private ChangeRecordSnapshotMapper changeRecordSnapshotMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restChangeRecordSnapshotMockMvc;

    private ChangeRecordSnapshot changeRecordSnapshot;

    private ChangeRecordSnapshot insertedChangeRecordSnapshot;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChangeRecordSnapshot createEntity(EntityManager em) {
        ChangeRecordSnapshot changeRecordSnapshot = new ChangeRecordSnapshot()
            .reference(DEFAULT_REFERENCE)
            .etatApprobation(DEFAULT_ETAT_APPROBATION)
            .fenetreDebut(DEFAULT_FENETRE_DEBUT)
            .fenetreFin(DEFAULT_FENETRE_FIN)
            .dateLecture(DEFAULT_DATE_LECTURE)
            .empreinteReponse(DEFAULT_EMPREINTE_REPONSE);
        // Add required entity
        GoLiveDossier goLiveDossier;
        if (TestUtil.findAll(em, GoLiveDossier.class).isEmpty()) {
            goLiveDossier = GoLiveDossierResourceIT.createEntity(em);
            em.persist(goLiveDossier);
            em.flush();
        } else {
            goLiveDossier = TestUtil.findAll(em, GoLiveDossier.class).getFirst();
        }
        changeRecordSnapshot.setDossier(goLiveDossier);
        return changeRecordSnapshot;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChangeRecordSnapshot createUpdatedEntity(EntityManager em) {
        ChangeRecordSnapshot updatedChangeRecordSnapshot = new ChangeRecordSnapshot()
            .reference(UPDATED_REFERENCE)
            .etatApprobation(UPDATED_ETAT_APPROBATION)
            .fenetreDebut(UPDATED_FENETRE_DEBUT)
            .fenetreFin(UPDATED_FENETRE_FIN)
            .dateLecture(UPDATED_DATE_LECTURE)
            .empreinteReponse(UPDATED_EMPREINTE_REPONSE);
        // Add required entity
        GoLiveDossier goLiveDossier;
        if (TestUtil.findAll(em, GoLiveDossier.class).isEmpty()) {
            goLiveDossier = GoLiveDossierResourceIT.createUpdatedEntity(em);
            em.persist(goLiveDossier);
            em.flush();
        } else {
            goLiveDossier = TestUtil.findAll(em, GoLiveDossier.class).getFirst();
        }
        updatedChangeRecordSnapshot.setDossier(goLiveDossier);
        return updatedChangeRecordSnapshot;
    }

    @BeforeEach
    void initTest() {
        changeRecordSnapshot = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedChangeRecordSnapshot != null) {
            changeRecordSnapshotRepository.delete(insertedChangeRecordSnapshot);
            insertedChangeRecordSnapshot = null;
        }
    }

    @Test
    @Transactional
    void createChangeRecordSnapshot() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ChangeRecordSnapshot
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(changeRecordSnapshot);
        var returnedChangeRecordSnapshotDTO = om.readValue(
            restChangeRecordSnapshotMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ChangeRecordSnapshotDTO.class
        );

        // Validate the ChangeRecordSnapshot in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedChangeRecordSnapshot = changeRecordSnapshotMapper.toEntity(returnedChangeRecordSnapshotDTO);
        assertChangeRecordSnapshotUpdatableFieldsEquals(
            returnedChangeRecordSnapshot,
            getPersistedChangeRecordSnapshot(returnedChangeRecordSnapshot)
        );

        insertedChangeRecordSnapshot = returnedChangeRecordSnapshot;
    }

    @Test
    @Transactional
    void createChangeRecordSnapshotWithExistingId() throws Exception {
        // Create the ChangeRecordSnapshot with an existing ID
        insertedChangeRecordSnapshot = changeRecordSnapshotRepository.saveAndFlush(changeRecordSnapshot);
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(changeRecordSnapshot);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restChangeRecordSnapshotMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChangeRecordSnapshot in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkReferenceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        changeRecordSnapshot.setReference(null);

        // Create the ChangeRecordSnapshot, which fails.
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(changeRecordSnapshot);

        restChangeRecordSnapshotMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEtatApprobationIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        changeRecordSnapshot.setEtatApprobation(null);

        // Create the ChangeRecordSnapshot, which fails.
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(changeRecordSnapshot);

        restChangeRecordSnapshotMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateLectureIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        changeRecordSnapshot.setDateLecture(null);

        // Create the ChangeRecordSnapshot, which fails.
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(changeRecordSnapshot);

        restChangeRecordSnapshotMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllChangeRecordSnapshots() throws Exception {
        // Initialize the database
        insertedChangeRecordSnapshot = changeRecordSnapshotRepository.saveAndFlush(changeRecordSnapshot);

        // Get all the changeRecordSnapshotList
        restChangeRecordSnapshotMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(changeRecordSnapshot.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].etatApprobation").value(hasItem(DEFAULT_ETAT_APPROBATION)))
            .andExpect(jsonPath("$.[*].fenetreDebut").value(hasItem(DEFAULT_FENETRE_DEBUT.toString())))
            .andExpect(jsonPath("$.[*].fenetreFin").value(hasItem(DEFAULT_FENETRE_FIN.toString())))
            .andExpect(jsonPath("$.[*].dateLecture").value(hasItem(DEFAULT_DATE_LECTURE.toString())))
            .andExpect(jsonPath("$.[*].empreinteReponse").value(hasItem(DEFAULT_EMPREINTE_REPONSE)));
    }

    @Test
    @Transactional
    void getChangeRecordSnapshot() throws Exception {
        // Initialize the database
        insertedChangeRecordSnapshot = changeRecordSnapshotRepository.saveAndFlush(changeRecordSnapshot);

        // Get the changeRecordSnapshot
        restChangeRecordSnapshotMockMvc
            .perform(get(ENTITY_API_URL_ID, changeRecordSnapshot.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(changeRecordSnapshot.getId().toString()))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.etatApprobation").value(DEFAULT_ETAT_APPROBATION))
            .andExpect(jsonPath("$.fenetreDebut").value(DEFAULT_FENETRE_DEBUT.toString()))
            .andExpect(jsonPath("$.fenetreFin").value(DEFAULT_FENETRE_FIN.toString()))
            .andExpect(jsonPath("$.dateLecture").value(DEFAULT_DATE_LECTURE.toString()))
            .andExpect(jsonPath("$.empreinteReponse").value(DEFAULT_EMPREINTE_REPONSE));
    }

    @Test
    @Transactional
    void getNonExistingChangeRecordSnapshot() throws Exception {
        // Get the changeRecordSnapshot
        restChangeRecordSnapshotMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingChangeRecordSnapshot() throws Exception {
        // Initialize the database
        insertedChangeRecordSnapshot = changeRecordSnapshotRepository.saveAndFlush(changeRecordSnapshot);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the changeRecordSnapshot
        ChangeRecordSnapshot updatedChangeRecordSnapshot = changeRecordSnapshotRepository
            .findById(changeRecordSnapshot.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedChangeRecordSnapshot are not directly saved in db
        em.detach(updatedChangeRecordSnapshot);
        updatedChangeRecordSnapshot
            .reference(UPDATED_REFERENCE)
            .etatApprobation(UPDATED_ETAT_APPROBATION)
            .fenetreDebut(UPDATED_FENETRE_DEBUT)
            .fenetreFin(UPDATED_FENETRE_FIN)
            .dateLecture(UPDATED_DATE_LECTURE)
            .empreinteReponse(UPDATED_EMPREINTE_REPONSE);
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(updatedChangeRecordSnapshot);

        restChangeRecordSnapshotMockMvc
            .perform(
                put(ENTITY_API_URL_ID, changeRecordSnapshotDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
            )
            .andExpect(status().isOk());

        // Validate the ChangeRecordSnapshot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedChangeRecordSnapshotToMatchAllProperties(updatedChangeRecordSnapshot);
    }

    @Test
    @Transactional
    void putNonExistingChangeRecordSnapshot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        changeRecordSnapshot.setId(UUID.randomUUID());

        // Create the ChangeRecordSnapshot
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(changeRecordSnapshot);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChangeRecordSnapshotMockMvc
            .perform(
                put(ENTITY_API_URL_ID, changeRecordSnapshotDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChangeRecordSnapshot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchChangeRecordSnapshot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        changeRecordSnapshot.setId(UUID.randomUUID());

        // Create the ChangeRecordSnapshot
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(changeRecordSnapshot);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChangeRecordSnapshotMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChangeRecordSnapshot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamChangeRecordSnapshot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        changeRecordSnapshot.setId(UUID.randomUUID());

        // Create the ChangeRecordSnapshot
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(changeRecordSnapshot);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChangeRecordSnapshotMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ChangeRecordSnapshot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateChangeRecordSnapshotWithPatch() throws Exception {
        // Initialize the database
        insertedChangeRecordSnapshot = changeRecordSnapshotRepository.saveAndFlush(changeRecordSnapshot);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the changeRecordSnapshot using partial update
        ChangeRecordSnapshot partialUpdatedChangeRecordSnapshot = new ChangeRecordSnapshot();
        partialUpdatedChangeRecordSnapshot.setId(changeRecordSnapshot.getId());

        partialUpdatedChangeRecordSnapshot.etatApprobation(UPDATED_ETAT_APPROBATION).fenetreFin(UPDATED_FENETRE_FIN);

        restChangeRecordSnapshotMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChangeRecordSnapshot.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChangeRecordSnapshot))
            )
            .andExpect(status().isOk());

        // Validate the ChangeRecordSnapshot in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChangeRecordSnapshotUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedChangeRecordSnapshot, changeRecordSnapshot),
            getPersistedChangeRecordSnapshot(changeRecordSnapshot)
        );
    }

    @Test
    @Transactional
    void fullUpdateChangeRecordSnapshotWithPatch() throws Exception {
        // Initialize the database
        insertedChangeRecordSnapshot = changeRecordSnapshotRepository.saveAndFlush(changeRecordSnapshot);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the changeRecordSnapshot using partial update
        ChangeRecordSnapshot partialUpdatedChangeRecordSnapshot = new ChangeRecordSnapshot();
        partialUpdatedChangeRecordSnapshot.setId(changeRecordSnapshot.getId());

        partialUpdatedChangeRecordSnapshot
            .reference(UPDATED_REFERENCE)
            .etatApprobation(UPDATED_ETAT_APPROBATION)
            .fenetreDebut(UPDATED_FENETRE_DEBUT)
            .fenetreFin(UPDATED_FENETRE_FIN)
            .dateLecture(UPDATED_DATE_LECTURE)
            .empreinteReponse(UPDATED_EMPREINTE_REPONSE);

        restChangeRecordSnapshotMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChangeRecordSnapshot.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChangeRecordSnapshot))
            )
            .andExpect(status().isOk());

        // Validate the ChangeRecordSnapshot in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChangeRecordSnapshotUpdatableFieldsEquals(
            partialUpdatedChangeRecordSnapshot,
            getPersistedChangeRecordSnapshot(partialUpdatedChangeRecordSnapshot)
        );
    }

    @Test
    @Transactional
    void patchNonExistingChangeRecordSnapshot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        changeRecordSnapshot.setId(UUID.randomUUID());

        // Create the ChangeRecordSnapshot
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(changeRecordSnapshot);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChangeRecordSnapshotMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, changeRecordSnapshotDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChangeRecordSnapshot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchChangeRecordSnapshot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        changeRecordSnapshot.setId(UUID.randomUUID());

        // Create the ChangeRecordSnapshot
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(changeRecordSnapshot);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChangeRecordSnapshotMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChangeRecordSnapshot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamChangeRecordSnapshot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        changeRecordSnapshot.setId(UUID.randomUUID());

        // Create the ChangeRecordSnapshot
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = changeRecordSnapshotMapper.toDto(changeRecordSnapshot);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChangeRecordSnapshotMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(changeRecordSnapshotDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ChangeRecordSnapshot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteChangeRecordSnapshot() throws Exception {
        // Initialize the database
        insertedChangeRecordSnapshot = changeRecordSnapshotRepository.saveAndFlush(changeRecordSnapshot);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the changeRecordSnapshot
        restChangeRecordSnapshotMockMvc
            .perform(delete(ENTITY_API_URL_ID, changeRecordSnapshot.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return changeRecordSnapshotRepository.count();
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

    protected ChangeRecordSnapshot getPersistedChangeRecordSnapshot(ChangeRecordSnapshot changeRecordSnapshot) {
        return changeRecordSnapshotRepository.findById(changeRecordSnapshot.getId()).orElseThrow();
    }

    protected void assertPersistedChangeRecordSnapshotToMatchAllProperties(ChangeRecordSnapshot expectedChangeRecordSnapshot) {
        assertChangeRecordSnapshotAllPropertiesEquals(
            expectedChangeRecordSnapshot,
            getPersistedChangeRecordSnapshot(expectedChangeRecordSnapshot)
        );
    }

    protected void assertPersistedChangeRecordSnapshotToMatchUpdatableProperties(ChangeRecordSnapshot expectedChangeRecordSnapshot) {
        assertChangeRecordSnapshotAllUpdatablePropertiesEquals(
            expectedChangeRecordSnapshot,
            getPersistedChangeRecordSnapshot(expectedChangeRecordSnapshot)
        );
    }
}
