package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.ReferentialMigrationAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.ReferentialMigration;
import cm.gimac.des.homologation.domain.enumeration.EtatMigration;
import cm.gimac.des.homologation.repository.ReferentialMigrationRepository;
import cm.gimac.des.homologation.service.dto.ReferentialMigrationDTO;
import cm.gimac.des.homologation.service.mapper.ReferentialMigrationMapper;
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
 * Integration tests for the {@link ReferentialMigrationResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.homologation.security.AuthoritiesConstants.ADMIN)
class ReferentialMigrationResourceIT {

    private static final UUID DEFAULT_VERSION_SOURCE_ID = UUID.randomUUID();
    private static final UUID UPDATED_VERSION_SOURCE_ID = UUID.randomUUID();

    private static final UUID DEFAULT_VERSION_CIBLE_ID = UUID.randomUUID();
    private static final UUID UPDATED_VERSION_CIBLE_ID = UUID.randomUUID();

    private static final EtatMigration DEFAULT_ETAT = EtatMigration.SIMULEE;
    private static final EtatMigration UPDATED_ETAT = EtatMigration.APPROUVEE;

    private static final String DEFAULT_RAPPORT = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_RAPPORT = "[\"B\"]";

    private static final Instant DEFAULT_DATE_DEMANDE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_DEMANDE = Instant.ofEpochMilli(1701958935371L);

    private static final UUID DEFAULT_DECIDEUR_ID = UUID.randomUUID();
    private static final UUID UPDATED_DECIDEUR_ID = UUID.randomUUID();

    private static final String DEFAULT_LIAISONS_RETENUES = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_LIAISONS_RETENUES = "[\"B\"]";

    private static final String ENTITY_API_URL = "/api/referential-migrations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReferentialMigrationRepository referentialMigrationRepository;

    @Autowired
    private ReferentialMigrationMapper referentialMigrationMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReferentialMigrationMockMvc;

    private ReferentialMigration referentialMigration;

    private ReferentialMigration insertedReferentialMigration;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReferentialMigration createEntity() {
        return new ReferentialMigration()
            .versionSourceId(DEFAULT_VERSION_SOURCE_ID)
            .versionCibleId(DEFAULT_VERSION_CIBLE_ID)
            .etat(DEFAULT_ETAT)
            .rapport(DEFAULT_RAPPORT)
            .dateDemande(DEFAULT_DATE_DEMANDE)
            .decideurId(DEFAULT_DECIDEUR_ID)
            .liaisonsRetenues(DEFAULT_LIAISONS_RETENUES);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReferentialMigration createUpdatedEntity() {
        return new ReferentialMigration()
            .versionSourceId(UPDATED_VERSION_SOURCE_ID)
            .versionCibleId(UPDATED_VERSION_CIBLE_ID)
            .etat(UPDATED_ETAT)
            .rapport(UPDATED_RAPPORT)
            .dateDemande(UPDATED_DATE_DEMANDE)
            .decideurId(UPDATED_DECIDEUR_ID)
            .liaisonsRetenues(UPDATED_LIAISONS_RETENUES);
    }

    @BeforeEach
    void initTest() {
        referentialMigration = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedReferentialMigration != null) {
            referentialMigrationRepository.delete(insertedReferentialMigration);
            insertedReferentialMigration = null;
        }
    }

    @Test
    @Transactional
    void createReferentialMigration() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ReferentialMigration
        ReferentialMigrationDTO referentialMigrationDTO = referentialMigrationMapper.toDto(referentialMigration);
        var returnedReferentialMigrationDTO = om.readValue(
            restReferentialMigrationMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(referentialMigrationDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ReferentialMigrationDTO.class
        );

        // Validate the ReferentialMigration in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedReferentialMigration = referentialMigrationMapper.toEntity(returnedReferentialMigrationDTO);
        assertReferentialMigrationUpdatableFieldsEquals(
            returnedReferentialMigration,
            getPersistedReferentialMigration(returnedReferentialMigration)
        );

        insertedReferentialMigration = returnedReferentialMigration;
    }

    @Test
    @Transactional
    void createReferentialMigrationWithExistingId() throws Exception {
        // Create the ReferentialMigration with an existing ID
        insertedReferentialMigration = referentialMigrationRepository.saveAndFlush(referentialMigration);
        ReferentialMigrationDTO referentialMigrationDTO = referentialMigrationMapper.toDto(referentialMigration);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReferentialMigrationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialMigrationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferentialMigration in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllReferentialMigrations() throws Exception {
        // Initialize the database
        insertedReferentialMigration = referentialMigrationRepository.saveAndFlush(referentialMigration);

        // Get all the referentialMigrationList
        restReferentialMigrationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(referentialMigration.getId().toString())))
            .andExpect(jsonPath("$.[*].versionSourceId").value(hasItem(DEFAULT_VERSION_SOURCE_ID.toString())))
            .andExpect(jsonPath("$.[*].versionCibleId").value(hasItem(DEFAULT_VERSION_CIBLE_ID.toString())))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())))
            .andExpect(jsonPath("$.[*].rapport").value(hasItem(DEFAULT_RAPPORT)))
            .andExpect(jsonPath("$.[*].dateDemande").value(hasItem(DEFAULT_DATE_DEMANDE.toString())))
            .andExpect(jsonPath("$.[*].decideurId").value(hasItem(DEFAULT_DECIDEUR_ID.toString())))
            .andExpect(jsonPath("$.[*].liaisonsRetenues").value(hasItem(DEFAULT_LIAISONS_RETENUES)));
    }

    @Test
    @Transactional
    void getReferentialMigration() throws Exception {
        // Initialize the database
        insertedReferentialMigration = referentialMigrationRepository.saveAndFlush(referentialMigration);

        // Get the referentialMigration
        restReferentialMigrationMockMvc
            .perform(get(ENTITY_API_URL_ID, referentialMigration.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(referentialMigration.getId().toString()))
            .andExpect(jsonPath("$.versionSourceId").value(DEFAULT_VERSION_SOURCE_ID.toString()))
            .andExpect(jsonPath("$.versionCibleId").value(DEFAULT_VERSION_CIBLE_ID.toString()))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT.toString()))
            .andExpect(jsonPath("$.rapport").value(DEFAULT_RAPPORT))
            .andExpect(jsonPath("$.dateDemande").value(DEFAULT_DATE_DEMANDE.toString()))
            .andExpect(jsonPath("$.decideurId").value(DEFAULT_DECIDEUR_ID.toString()))
            .andExpect(jsonPath("$.liaisonsRetenues").value(DEFAULT_LIAISONS_RETENUES));
    }

    @Test
    @Transactional
    void getNonExistingReferentialMigration() throws Exception {
        // Get the referentialMigration
        restReferentialMigrationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReferentialMigration() throws Exception {
        // Initialize the database
        insertedReferentialMigration = referentialMigrationRepository.saveAndFlush(referentialMigration);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referentialMigration
        ReferentialMigration updatedReferentialMigration = referentialMigrationRepository
            .findById(referentialMigration.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedReferentialMigration are not directly saved in db
        em.detach(updatedReferentialMigration);
        updatedReferentialMigration
            .versionSourceId(UPDATED_VERSION_SOURCE_ID)
            .versionCibleId(UPDATED_VERSION_CIBLE_ID)
            .etat(UPDATED_ETAT)
            .rapport(UPDATED_RAPPORT)
            .dateDemande(UPDATED_DATE_DEMANDE)
            .decideurId(UPDATED_DECIDEUR_ID)
            .liaisonsRetenues(UPDATED_LIAISONS_RETENUES);
        ReferentialMigrationDTO referentialMigrationDTO = referentialMigrationMapper.toDto(updatedReferentialMigration);

        restReferentialMigrationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, referentialMigrationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialMigrationDTO))
            )
            .andExpect(status().isOk());

        // Validate the ReferentialMigration in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReferentialMigrationToMatchAllProperties(updatedReferentialMigration);
    }

    @Test
    @Transactional
    void putNonExistingReferentialMigration() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialMigration.setId(UUID.randomUUID());

        // Create the ReferentialMigration
        ReferentialMigrationDTO referentialMigrationDTO = referentialMigrationMapper.toDto(referentialMigration);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferentialMigrationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, referentialMigrationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialMigrationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferentialMigration in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReferentialMigration() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialMigration.setId(UUID.randomUUID());

        // Create the ReferentialMigration
        ReferentialMigrationDTO referentialMigrationDTO = referentialMigrationMapper.toDto(referentialMigration);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentialMigrationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialMigrationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferentialMigration in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReferentialMigration() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialMigration.setId(UUID.randomUUID());

        // Create the ReferentialMigration
        ReferentialMigrationDTO referentialMigrationDTO = referentialMigrationMapper.toDto(referentialMigration);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentialMigrationMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialMigrationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReferentialMigration in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReferentialMigrationWithPatch() throws Exception {
        // Initialize the database
        insertedReferentialMigration = referentialMigrationRepository.saveAndFlush(referentialMigration);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referentialMigration using partial update
        ReferentialMigration partialUpdatedReferentialMigration = new ReferentialMigration();
        partialUpdatedReferentialMigration.setId(referentialMigration.getId());

        partialUpdatedReferentialMigration
            .dateDemande(UPDATED_DATE_DEMANDE)
            .decideurId(UPDATED_DECIDEUR_ID)
            .liaisonsRetenues(UPDATED_LIAISONS_RETENUES);

        restReferentialMigrationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferentialMigration.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferentialMigration))
            )
            .andExpect(status().isOk());

        // Validate the ReferentialMigration in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferentialMigrationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedReferentialMigration, referentialMigration),
            getPersistedReferentialMigration(referentialMigration)
        );
    }

    @Test
    @Transactional
    void fullUpdateReferentialMigrationWithPatch() throws Exception {
        // Initialize the database
        insertedReferentialMigration = referentialMigrationRepository.saveAndFlush(referentialMigration);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referentialMigration using partial update
        ReferentialMigration partialUpdatedReferentialMigration = new ReferentialMigration();
        partialUpdatedReferentialMigration.setId(referentialMigration.getId());

        partialUpdatedReferentialMigration
            .versionSourceId(UPDATED_VERSION_SOURCE_ID)
            .versionCibleId(UPDATED_VERSION_CIBLE_ID)
            .etat(UPDATED_ETAT)
            .rapport(UPDATED_RAPPORT)
            .dateDemande(UPDATED_DATE_DEMANDE)
            .decideurId(UPDATED_DECIDEUR_ID)
            .liaisonsRetenues(UPDATED_LIAISONS_RETENUES);

        restReferentialMigrationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferentialMigration.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferentialMigration))
            )
            .andExpect(status().isOk());

        // Validate the ReferentialMigration in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferentialMigrationUpdatableFieldsEquals(
            partialUpdatedReferentialMigration,
            getPersistedReferentialMigration(partialUpdatedReferentialMigration)
        );
    }

    @Test
    @Transactional
    void patchNonExistingReferentialMigration() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialMigration.setId(UUID.randomUUID());

        // Create the ReferentialMigration
        ReferentialMigrationDTO referentialMigrationDTO = referentialMigrationMapper.toDto(referentialMigration);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferentialMigrationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, referentialMigrationDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referentialMigrationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferentialMigration in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReferentialMigration() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialMigration.setId(UUID.randomUUID());

        // Create the ReferentialMigration
        ReferentialMigrationDTO referentialMigrationDTO = referentialMigrationMapper.toDto(referentialMigration);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentialMigrationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referentialMigrationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferentialMigration in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReferentialMigration() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialMigration.setId(UUID.randomUUID());

        // Create the ReferentialMigration
        ReferentialMigrationDTO referentialMigrationDTO = referentialMigrationMapper.toDto(referentialMigration);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentialMigrationMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referentialMigrationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReferentialMigration in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReferentialMigration() throws Exception {
        // Initialize the database
        insertedReferentialMigration = referentialMigrationRepository.saveAndFlush(referentialMigration);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the referentialMigration
        restReferentialMigrationMockMvc
            .perform(delete(ENTITY_API_URL_ID, referentialMigration.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return referentialMigrationRepository.count();
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

    protected ReferentialMigration getPersistedReferentialMigration(ReferentialMigration referentialMigration) {
        return referentialMigrationRepository.findById(referentialMigration.getId()).orElseThrow();
    }

    protected void assertPersistedReferentialMigrationToMatchAllProperties(ReferentialMigration expectedReferentialMigration) {
        assertReferentialMigrationAllPropertiesEquals(
            expectedReferentialMigration,
            getPersistedReferentialMigration(expectedReferentialMigration)
        );
    }

    protected void assertPersistedReferentialMigrationToMatchUpdatableProperties(ReferentialMigration expectedReferentialMigration) {
        assertReferentialMigrationAllUpdatablePropertiesEquals(
            expectedReferentialMigration,
            getPersistedReferentialMigration(expectedReferentialMigration)
        );
    }
}
