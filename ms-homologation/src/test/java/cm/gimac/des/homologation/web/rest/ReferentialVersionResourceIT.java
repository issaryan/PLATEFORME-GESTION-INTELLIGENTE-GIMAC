package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.ReferentialVersionAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.service.dto.ReferentialVersionDTO;
import cm.gimac.des.homologation.service.mapper.ReferentialVersionMapper;
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
 * Integration tests for the {@link ReferentialVersionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ReferentialVersionResourceIT {

    private static final TypeProjet DEFAULT_TYPE_PROJET = TypeProjet.CARTE_INTERBANCAIRE;
    private static final TypeProjet UPDATED_TYPE_PROJET = TypeProjet.PAIEMENT_MOBILE;

    private static final Integer DEFAULT_NUMERO = 1;
    private static final Integer UPDATED_NUMERO = 2;

    private static final String DEFAULT_LIBELLE = "AAAAAAAAAA";
    private static final String UPDATED_LIBELLE = "BBBBBBBBBB";

    private static final EtatVersionReferentiel DEFAULT_ETAT = EtatVersionReferentiel.BROUILLON;
    private static final EtatVersionReferentiel UPDATED_ETAT = EtatVersionReferentiel.PUBLIEE;

    private static final Instant DEFAULT_DATE_CREATION = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_CREATION = Instant.ofEpochMilli(1701958935371L);

    private static final Instant DEFAULT_DATE_PUBLICATION = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_PUBLICATION = Instant.ofEpochMilli(1701958935371L);

    private static final String DEFAULT_ORDRE_TOPOLOGIQUE = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_ORDRE_TOPOLOGIQUE = "[\"B\"]";

    private static final UUID DEFAULT_VERSION_SOURCE_ID = UUID.randomUUID();
    private static final UUID UPDATED_VERSION_SOURCE_ID = UUID.randomUUID();

    private static final String ENTITY_API_URL = "/api/referential-versions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReferentialVersionRepository referentialVersionRepository;

    @Autowired
    private ReferentialVersionMapper referentialVersionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReferentialVersionMockMvc;

    private ReferentialVersion referentialVersion;

    private ReferentialVersion insertedReferentialVersion;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReferentialVersion createEntity() {
        return new ReferentialVersion()
            .typeProjet(DEFAULT_TYPE_PROJET)
            .numero(DEFAULT_NUMERO)
            .libelle(DEFAULT_LIBELLE)
            .etat(DEFAULT_ETAT)
            .dateCreation(DEFAULT_DATE_CREATION)
            .datePublication(DEFAULT_DATE_PUBLICATION)
            .ordreTopologique(DEFAULT_ORDRE_TOPOLOGIQUE)
            .versionSourceId(DEFAULT_VERSION_SOURCE_ID);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReferentialVersion createUpdatedEntity() {
        return new ReferentialVersion()
            .typeProjet(UPDATED_TYPE_PROJET)
            .numero(UPDATED_NUMERO)
            .libelle(UPDATED_LIBELLE)
            .etat(UPDATED_ETAT)
            .dateCreation(UPDATED_DATE_CREATION)
            .datePublication(UPDATED_DATE_PUBLICATION)
            .ordreTopologique(UPDATED_ORDRE_TOPOLOGIQUE)
            .versionSourceId(UPDATED_VERSION_SOURCE_ID);
    }

    @BeforeEach
    void initTest() {
        referentialVersion = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedReferentialVersion != null) {
            referentialVersionRepository.delete(insertedReferentialVersion);
            insertedReferentialVersion = null;
        }
    }

    @Test
    @Transactional
    void createReferentialVersion() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ReferentialVersion
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);
        var returnedReferentialVersionDTO = om.readValue(
            restReferentialVersionMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(referentialVersionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ReferentialVersionDTO.class
        );

        // Validate the ReferentialVersion in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedReferentialVersion = referentialVersionMapper.toEntity(returnedReferentialVersionDTO);
        assertReferentialVersionUpdatableFieldsEquals(
            returnedReferentialVersion,
            getPersistedReferentialVersion(returnedReferentialVersion)
        );

        insertedReferentialVersion = returnedReferentialVersion;
    }

    @Test
    @Transactional
    void createReferentialVersionWithExistingId() throws Exception {
        // Create the ReferentialVersion with an existing ID
        insertedReferentialVersion = referentialVersionRepository.saveAndFlush(referentialVersion);
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReferentialVersionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferentialVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTypeProjetIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referentialVersion.setTypeProjet(null);

        // Create the ReferentialVersion, which fails.
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);

        restReferentialVersionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLibelleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referentialVersion.setLibelle(null);

        // Create the ReferentialVersion, which fails.
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);

        restReferentialVersionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEtatIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referentialVersion.setEtat(null);

        // Create the ReferentialVersion, which fails.
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);

        restReferentialVersionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateCreationIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referentialVersion.setDateCreation(null);

        // Create the ReferentialVersion, which fails.
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);

        restReferentialVersionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllReferentialVersions() throws Exception {
        // Initialize the database
        insertedReferentialVersion = referentialVersionRepository.saveAndFlush(referentialVersion);

        // Get all the referentialVersionList
        restReferentialVersionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(referentialVersion.getId().toString())))
            .andExpect(jsonPath("$.[*].typeProjet").value(hasItem(DEFAULT_TYPE_PROJET.toString())))
            .andExpect(jsonPath("$.[*].numero").value(hasItem(DEFAULT_NUMERO)))
            .andExpect(jsonPath("$.[*].libelle").value(hasItem(DEFAULT_LIBELLE)))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())))
            .andExpect(jsonPath("$.[*].dateCreation").value(hasItem(DEFAULT_DATE_CREATION.toString())))
            .andExpect(jsonPath("$.[*].datePublication").value(hasItem(DEFAULT_DATE_PUBLICATION.toString())))
            .andExpect(jsonPath("$.[*].ordreTopologique").value(hasItem(DEFAULT_ORDRE_TOPOLOGIQUE)))
            .andExpect(jsonPath("$.[*].versionSourceId").value(hasItem(DEFAULT_VERSION_SOURCE_ID.toString())));
    }

    @Test
    @Transactional
    void getReferentialVersion() throws Exception {
        // Initialize the database
        insertedReferentialVersion = referentialVersionRepository.saveAndFlush(referentialVersion);

        // Get the referentialVersion
        restReferentialVersionMockMvc
            .perform(get(ENTITY_API_URL_ID, referentialVersion.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(referentialVersion.getId().toString()))
            .andExpect(jsonPath("$.typeProjet").value(DEFAULT_TYPE_PROJET.toString()))
            .andExpect(jsonPath("$.numero").value(DEFAULT_NUMERO))
            .andExpect(jsonPath("$.libelle").value(DEFAULT_LIBELLE))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT.toString()))
            .andExpect(jsonPath("$.dateCreation").value(DEFAULT_DATE_CREATION.toString()))
            .andExpect(jsonPath("$.datePublication").value(DEFAULT_DATE_PUBLICATION.toString()))
            .andExpect(jsonPath("$.ordreTopologique").value(DEFAULT_ORDRE_TOPOLOGIQUE))
            .andExpect(jsonPath("$.versionSourceId").value(DEFAULT_VERSION_SOURCE_ID.toString()));
    }

    @Test
    @Transactional
    void getNonExistingReferentialVersion() throws Exception {
        // Get the referentialVersion
        restReferentialVersionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReferentialVersion() throws Exception {
        // Initialize the database
        insertedReferentialVersion = referentialVersionRepository.saveAndFlush(referentialVersion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referentialVersion
        ReferentialVersion updatedReferentialVersion = referentialVersionRepository.findById(referentialVersion.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedReferentialVersion are not directly saved in db
        em.detach(updatedReferentialVersion);
        updatedReferentialVersion
            .typeProjet(UPDATED_TYPE_PROJET)
            .numero(UPDATED_NUMERO)
            .libelle(UPDATED_LIBELLE)
            .etat(UPDATED_ETAT)
            .dateCreation(UPDATED_DATE_CREATION)
            .datePublication(UPDATED_DATE_PUBLICATION)
            .ordreTopologique(UPDATED_ORDRE_TOPOLOGIQUE)
            .versionSourceId(UPDATED_VERSION_SOURCE_ID);
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(updatedReferentialVersion);

        restReferentialVersionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, referentialVersionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isOk());

        // Validate the ReferentialVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReferentialVersionToMatchAllProperties(updatedReferentialVersion);
    }

    @Test
    @Transactional
    void putNonExistingReferentialVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialVersion.setId(UUID.randomUUID());

        // Create the ReferentialVersion
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferentialVersionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, referentialVersionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferentialVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReferentialVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialVersion.setId(UUID.randomUUID());

        // Create the ReferentialVersion
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentialVersionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferentialVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReferentialVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialVersion.setId(UUID.randomUUID());

        // Create the ReferentialVersion
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentialVersionMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReferentialVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReferentialVersionWithPatch() throws Exception {
        // Initialize the database
        insertedReferentialVersion = referentialVersionRepository.saveAndFlush(referentialVersion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referentialVersion using partial update
        ReferentialVersion partialUpdatedReferentialVersion = new ReferentialVersion();
        partialUpdatedReferentialVersion.setId(referentialVersion.getId());

        partialUpdatedReferentialVersion
            .numero(UPDATED_NUMERO)
            .etat(UPDATED_ETAT)
            .dateCreation(UPDATED_DATE_CREATION)
            .datePublication(UPDATED_DATE_PUBLICATION)
            .ordreTopologique(UPDATED_ORDRE_TOPOLOGIQUE);

        restReferentialVersionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferentialVersion.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferentialVersion))
            )
            .andExpect(status().isOk());

        // Validate the ReferentialVersion in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferentialVersionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedReferentialVersion, referentialVersion),
            getPersistedReferentialVersion(referentialVersion)
        );
    }

    @Test
    @Transactional
    void fullUpdateReferentialVersionWithPatch() throws Exception {
        // Initialize the database
        insertedReferentialVersion = referentialVersionRepository.saveAndFlush(referentialVersion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referentialVersion using partial update
        ReferentialVersion partialUpdatedReferentialVersion = new ReferentialVersion();
        partialUpdatedReferentialVersion.setId(referentialVersion.getId());

        partialUpdatedReferentialVersion
            .typeProjet(UPDATED_TYPE_PROJET)
            .numero(UPDATED_NUMERO)
            .libelle(UPDATED_LIBELLE)
            .etat(UPDATED_ETAT)
            .dateCreation(UPDATED_DATE_CREATION)
            .datePublication(UPDATED_DATE_PUBLICATION)
            .ordreTopologique(UPDATED_ORDRE_TOPOLOGIQUE)
            .versionSourceId(UPDATED_VERSION_SOURCE_ID);

        restReferentialVersionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferentialVersion.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferentialVersion))
            )
            .andExpect(status().isOk());

        // Validate the ReferentialVersion in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferentialVersionUpdatableFieldsEquals(
            partialUpdatedReferentialVersion,
            getPersistedReferentialVersion(partialUpdatedReferentialVersion)
        );
    }

    @Test
    @Transactional
    void patchNonExistingReferentialVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialVersion.setId(UUID.randomUUID());

        // Create the ReferentialVersion
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferentialVersionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, referentialVersionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferentialVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReferentialVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialVersion.setId(UUID.randomUUID());

        // Create the ReferentialVersion
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentialVersionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferentialVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReferentialVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentialVersion.setId(UUID.randomUUID());

        // Create the ReferentialVersion
        ReferentialVersionDTO referentialVersionDTO = referentialVersionMapper.toDto(referentialVersion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentialVersionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referentialVersionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReferentialVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReferentialVersion() throws Exception {
        // Initialize the database
        insertedReferentialVersion = referentialVersionRepository.saveAndFlush(referentialVersion);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the referentialVersion
        restReferentialVersionMockMvc
            .perform(delete(ENTITY_API_URL_ID, referentialVersion.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return referentialVersionRepository.count();
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

    protected ReferentialVersion getPersistedReferentialVersion(ReferentialVersion referentialVersion) {
        return referentialVersionRepository.findById(referentialVersion.getId()).orElseThrow();
    }

    protected void assertPersistedReferentialVersionToMatchAllProperties(ReferentialVersion expectedReferentialVersion) {
        assertReferentialVersionAllPropertiesEquals(expectedReferentialVersion, getPersistedReferentialVersion(expectedReferentialVersion));
    }

    protected void assertPersistedReferentialVersionToMatchUpdatableProperties(ReferentialVersion expectedReferentialVersion) {
        assertReferentialVersionAllUpdatablePropertiesEquals(
            expectedReferentialVersion,
            getPersistedReferentialVersion(expectedReferentialVersion)
        );
    }
}
