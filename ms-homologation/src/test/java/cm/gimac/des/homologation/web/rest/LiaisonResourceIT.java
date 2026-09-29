package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.LiaisonAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.mapper.LiaisonMapper;
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
 * Integration tests for the {@link LiaisonResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class LiaisonResourceIT {

    private static final TypeProjet DEFAULT_TYPE_PROJET = TypeProjet.CARTE_INTERBANCAIRE;
    private static final TypeProjet UPDATED_TYPE_PROJET = TypeProjet.PAIEMENT_MOBILE;

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final String DEFAULT_INSTITUTION_NOM = "AAAAAAAAAA";
    private static final String UPDATED_INSTITUTION_NOM = "BBBBBBBBBB";

    private static final EtatLiaison DEFAULT_ETAT = EtatLiaison.OUVERTE;
    private static final EtatLiaison UPDATED_ETAT = EtatLiaison.EN_HOMOLOGATION;

    private static final UUID DEFAULT_CHEF_DE_PROJET_ID = UUID.randomUUID();
    private static final UUID UPDATED_CHEF_DE_PROJET_ID = UUID.randomUUID();

    private static final UUID DEFAULT_BANQUE_PARTENAIRE_ID = UUID.randomUUID();
    private static final UUID UPDATED_BANQUE_PARTENAIRE_ID = UUID.randomUUID();

    private static final Instant DEFAULT_DATE_OUVERTURE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_OUVERTURE = Instant.ofEpochMilli(1701958935371L);

    private static final String DEFAULT_MOTIF_ETAT = "AAAAAAAAAA";
    private static final String UPDATED_MOTIF_ETAT = "BBBBBBBBBB";

    private static final Integer DEFAULT_CYCLE_APTITUDE = 1;
    private static final Integer UPDATED_CYCLE_APTITUDE = 2;

    private static final String DEFAULT_ORDRE_INSTANCIATION = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_ORDRE_INSTANCIATION = "[\"B\"]";

    private static final String DEFAULT_ETAT_AVANT_GEL = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_ETAT_AVANT_GEL = "[\"B\"]";

    private static final String ENTITY_API_URL = "/api/liaisons";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private LiaisonRepository liaisonRepository;

    @Autowired
    private LiaisonMapper liaisonMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restLiaisonMockMvc;

    private Liaison liaison;

    private Liaison insertedLiaison;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Liaison createEntity(EntityManager em) {
        Liaison liaison = new Liaison()
            .typeProjet(DEFAULT_TYPE_PROJET)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .institutionNom(DEFAULT_INSTITUTION_NOM)
            .etat(DEFAULT_ETAT)
            .chefDeProjetId(DEFAULT_CHEF_DE_PROJET_ID)
            .banquePartenaireId(DEFAULT_BANQUE_PARTENAIRE_ID)
            .dateOuverture(DEFAULT_DATE_OUVERTURE)
            .motifEtat(DEFAULT_MOTIF_ETAT)
            .cycleAptitude(DEFAULT_CYCLE_APTITUDE)
            .ordreInstanciation(DEFAULT_ORDRE_INSTANCIATION)
            .etatAvantGel(DEFAULT_ETAT_AVANT_GEL);
        // Add required entity
        ReferentialVersion referentialVersion;
        if (TestUtil.findAll(em, ReferentialVersion.class).isEmpty()) {
            referentialVersion = ReferentialVersionResourceIT.createEntity();
            em.persist(referentialVersion);
            em.flush();
        } else {
            referentialVersion = TestUtil.findAll(em, ReferentialVersion.class).getFirst();
        }
        liaison.setVersion(referentialVersion);
        return liaison;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Liaison createUpdatedEntity(EntityManager em) {
        Liaison updatedLiaison = new Liaison()
            .typeProjet(UPDATED_TYPE_PROJET)
            .institutionId(UPDATED_INSTITUTION_ID)
            .institutionNom(UPDATED_INSTITUTION_NOM)
            .etat(UPDATED_ETAT)
            .chefDeProjetId(UPDATED_CHEF_DE_PROJET_ID)
            .banquePartenaireId(UPDATED_BANQUE_PARTENAIRE_ID)
            .dateOuverture(UPDATED_DATE_OUVERTURE)
            .motifEtat(UPDATED_MOTIF_ETAT)
            .cycleAptitude(UPDATED_CYCLE_APTITUDE)
            .ordreInstanciation(UPDATED_ORDRE_INSTANCIATION)
            .etatAvantGel(UPDATED_ETAT_AVANT_GEL);
        // Add required entity
        ReferentialVersion referentialVersion;
        if (TestUtil.findAll(em, ReferentialVersion.class).isEmpty()) {
            referentialVersion = ReferentialVersionResourceIT.createUpdatedEntity();
            em.persist(referentialVersion);
            em.flush();
        } else {
            referentialVersion = TestUtil.findAll(em, ReferentialVersion.class).getFirst();
        }
        updatedLiaison.setVersion(referentialVersion);
        return updatedLiaison;
    }

    @BeforeEach
    void initTest() {
        liaison = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedLiaison != null) {
            liaisonRepository.delete(insertedLiaison);
            insertedLiaison = null;
        }
    }

    @Test
    @Transactional
    void createLiaison() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);
        var returnedLiaisonDTO = om.readValue(
            restLiaisonMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            LiaisonDTO.class
        );

        // Validate the Liaison in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedLiaison = liaisonMapper.toEntity(returnedLiaisonDTO);
        assertLiaisonUpdatableFieldsEquals(returnedLiaison, getPersistedLiaison(returnedLiaison));

        insertedLiaison = returnedLiaison;
    }

    @Test
    @Transactional
    void createLiaisonWithExistingId() throws Exception {
        // Create the Liaison with an existing ID
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTypeProjetIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setTypeProjet(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkInstitutionIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setInstitutionId(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEtatIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setEtat(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkChefDeProjetIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setChefDeProjetId(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateOuvertureIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setDateOuverture(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCycleAptitudeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setCycleAptitude(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllLiaisons() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList
        restLiaisonMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(liaison.getId().toString())))
            .andExpect(jsonPath("$.[*].typeProjet").value(hasItem(DEFAULT_TYPE_PROJET.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].institutionNom").value(hasItem(DEFAULT_INSTITUTION_NOM)))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())))
            .andExpect(jsonPath("$.[*].chefDeProjetId").value(hasItem(DEFAULT_CHEF_DE_PROJET_ID.toString())))
            .andExpect(jsonPath("$.[*].banquePartenaireId").value(hasItem(DEFAULT_BANQUE_PARTENAIRE_ID.toString())))
            .andExpect(jsonPath("$.[*].dateOuverture").value(hasItem(DEFAULT_DATE_OUVERTURE.toString())))
            .andExpect(jsonPath("$.[*].motifEtat").value(hasItem(DEFAULT_MOTIF_ETAT)))
            .andExpect(jsonPath("$.[*].cycleAptitude").value(hasItem(DEFAULT_CYCLE_APTITUDE)))
            .andExpect(jsonPath("$.[*].ordreInstanciation").value(hasItem(DEFAULT_ORDRE_INSTANCIATION)))
            .andExpect(jsonPath("$.[*].etatAvantGel").value(hasItem(DEFAULT_ETAT_AVANT_GEL)));
    }

    @Test
    @Transactional
    void getLiaison() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get the liaison
        restLiaisonMockMvc
            .perform(get(ENTITY_API_URL_ID, liaison.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(liaison.getId().toString()))
            .andExpect(jsonPath("$.typeProjet").value(DEFAULT_TYPE_PROJET.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.institutionNom").value(DEFAULT_INSTITUTION_NOM))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT.toString()))
            .andExpect(jsonPath("$.chefDeProjetId").value(DEFAULT_CHEF_DE_PROJET_ID.toString()))
            .andExpect(jsonPath("$.banquePartenaireId").value(DEFAULT_BANQUE_PARTENAIRE_ID.toString()))
            .andExpect(jsonPath("$.dateOuverture").value(DEFAULT_DATE_OUVERTURE.toString()))
            .andExpect(jsonPath("$.motifEtat").value(DEFAULT_MOTIF_ETAT))
            .andExpect(jsonPath("$.cycleAptitude").value(DEFAULT_CYCLE_APTITUDE))
            .andExpect(jsonPath("$.ordreInstanciation").value(DEFAULT_ORDRE_INSTANCIATION))
            .andExpect(jsonPath("$.etatAvantGel").value(DEFAULT_ETAT_AVANT_GEL));
    }

    @Test
    @Transactional
    void getNonExistingLiaison() throws Exception {
        // Get the liaison
        restLiaisonMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingLiaison() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the liaison
        Liaison updatedLiaison = liaisonRepository.findById(liaison.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedLiaison are not directly saved in db
        em.detach(updatedLiaison);
        updatedLiaison
            .typeProjet(UPDATED_TYPE_PROJET)
            .institutionId(UPDATED_INSTITUTION_ID)
            .institutionNom(UPDATED_INSTITUTION_NOM)
            .etat(UPDATED_ETAT)
            .chefDeProjetId(UPDATED_CHEF_DE_PROJET_ID)
            .banquePartenaireId(UPDATED_BANQUE_PARTENAIRE_ID)
            .dateOuverture(UPDATED_DATE_OUVERTURE)
            .motifEtat(UPDATED_MOTIF_ETAT)
            .cycleAptitude(UPDATED_CYCLE_APTITUDE)
            .ordreInstanciation(UPDATED_ORDRE_INSTANCIATION)
            .etatAvantGel(UPDATED_ETAT_AVANT_GEL);
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(updatedLiaison);

        restLiaisonMockMvc
            .perform(
                put(ENTITY_API_URL_ID, liaisonDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isOk());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedLiaisonToMatchAllProperties(updatedLiaison);
    }

    @Test
    @Transactional
    void putNonExistingLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(
                put(ENTITY_API_URL_ID, liaisonDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateLiaisonWithPatch() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the liaison using partial update
        Liaison partialUpdatedLiaison = new Liaison();
        partialUpdatedLiaison.setId(liaison.getId());

        partialUpdatedLiaison
            .institutionNom(UPDATED_INSTITUTION_NOM)
            .etat(UPDATED_ETAT)
            .chefDeProjetId(UPDATED_CHEF_DE_PROJET_ID)
            .dateOuverture(UPDATED_DATE_OUVERTURE)
            .motifEtat(UPDATED_MOTIF_ETAT)
            .cycleAptitude(UPDATED_CYCLE_APTITUDE)
            .etatAvantGel(UPDATED_ETAT_AVANT_GEL);

        restLiaisonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLiaison.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLiaison))
            )
            .andExpect(status().isOk());

        // Validate the Liaison in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLiaisonUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedLiaison, liaison), getPersistedLiaison(liaison));
    }

    @Test
    @Transactional
    void fullUpdateLiaisonWithPatch() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the liaison using partial update
        Liaison partialUpdatedLiaison = new Liaison();
        partialUpdatedLiaison.setId(liaison.getId());

        partialUpdatedLiaison
            .typeProjet(UPDATED_TYPE_PROJET)
            .institutionId(UPDATED_INSTITUTION_ID)
            .institutionNom(UPDATED_INSTITUTION_NOM)
            .etat(UPDATED_ETAT)
            .chefDeProjetId(UPDATED_CHEF_DE_PROJET_ID)
            .banquePartenaireId(UPDATED_BANQUE_PARTENAIRE_ID)
            .dateOuverture(UPDATED_DATE_OUVERTURE)
            .motifEtat(UPDATED_MOTIF_ETAT)
            .cycleAptitude(UPDATED_CYCLE_APTITUDE)
            .ordreInstanciation(UPDATED_ORDRE_INSTANCIATION)
            .etatAvantGel(UPDATED_ETAT_AVANT_GEL);

        restLiaisonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLiaison.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLiaison))
            )
            .andExpect(status().isOk());

        // Validate the Liaison in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLiaisonUpdatableFieldsEquals(partialUpdatedLiaison, getPersistedLiaison(partialUpdatedLiaison));
    }

    @Test
    @Transactional
    void patchNonExistingLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, liaisonDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteLiaison() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the liaison
        restLiaisonMockMvc
            .perform(delete(ENTITY_API_URL_ID, liaison.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return liaisonRepository.count();
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

    protected Liaison getPersistedLiaison(Liaison liaison) {
        return liaisonRepository.findById(liaison.getId()).orElseThrow();
    }

    protected void assertPersistedLiaisonToMatchAllProperties(Liaison expectedLiaison) {
        assertLiaisonAllPropertiesEquals(expectedLiaison, getPersistedLiaison(expectedLiaison));
    }

    protected void assertPersistedLiaisonToMatchUpdatableProperties(Liaison expectedLiaison) {
        assertLiaisonAllUpdatablePropertiesEquals(expectedLiaison, getPersistedLiaison(expectedLiaison));
    }
}
