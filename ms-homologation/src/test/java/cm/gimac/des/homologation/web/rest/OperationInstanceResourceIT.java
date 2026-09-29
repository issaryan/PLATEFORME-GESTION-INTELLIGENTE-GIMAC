package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.OperationInstanceAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.OperationInstance;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.enumeration.EtatOperation;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.repository.OperationInstanceRepository;
import cm.gimac.des.homologation.service.dto.OperationInstanceDTO;
import cm.gimac.des.homologation.service.mapper.OperationInstanceMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
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
 * Integration tests for the {@link OperationInstanceResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.homologation.security.AuthoritiesConstants.ADMIN)
class OperationInstanceResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_LIBELLE = "AAAAAAAAAA";
    private static final String UPDATED_LIBELLE = "BBBBBBBBBB";

    private static final Segment DEFAULT_SEGMENT = Segment.TRONC_COMMUN_AMONT;
    private static final Segment UPDATED_SEGMENT = Segment.BRANCHE;

    private static final Partie DEFAULT_PARTIE = Partie.DES;
    private static final Partie UPDATED_PARTIE = Partie.PARTICIPANT;

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final EtatOperation DEFAULT_ETAT = EtatOperation.VERROUILLEE;
    private static final EtatOperation UPDATED_ETAT = EtatOperation.ELIGIBLE;

    private static final Integer DEFAULT_DEGRE_ENTRANT = 1;
    private static final Integer UPDATED_DEGRE_ENTRANT = 2;

    private static final LocalDate DEFAULT_ECHEANCE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_ECHEANCE = LocalDate.parse("2023-12-07");

    private static final Instant DEFAULT_DATE_ETAT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_ETAT = Instant.ofEpochMilli(1701958935371L);

    private static final String DEFAULT_GRILLE_REPONSES = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_GRILLE_REPONSES = "[\"B\"]";

    private static final String ENTITY_API_URL = "/api/operation-instances";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OperationInstanceRepository operationInstanceRepository;

    @Autowired
    private OperationInstanceMapper operationInstanceMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOperationInstanceMockMvc;

    private OperationInstance operationInstance;

    private OperationInstance insertedOperationInstance;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OperationInstance createEntity(EntityManager em) {
        OperationInstance operationInstance = new OperationInstance()
            .code(DEFAULT_CODE)
            .libelle(DEFAULT_LIBELLE)
            .segment(DEFAULT_SEGMENT)
            .partie(DEFAULT_PARTIE)
            .serviceCode(DEFAULT_SERVICE_CODE)
            .etat(DEFAULT_ETAT)
            .degreEntrant(DEFAULT_DEGRE_ENTRANT)
            .echeance(DEFAULT_ECHEANCE)
            .dateEtat(DEFAULT_DATE_ETAT)
            .grilleReponses(DEFAULT_GRILLE_REPONSES);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        operationInstance.setLiaison(liaison);
        // Add required entity
        ReferenceOperation referenceOperation;
        if (TestUtil.findAll(em, ReferenceOperation.class).isEmpty()) {
            referenceOperation = ReferenceOperationResourceIT.createEntity(em);
            em.persist(referenceOperation);
            em.flush();
        } else {
            referenceOperation = TestUtil.findAll(em, ReferenceOperation.class).getFirst();
        }
        operationInstance.setReferenceOperation(referenceOperation);
        return operationInstance;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OperationInstance createUpdatedEntity(EntityManager em) {
        OperationInstance updatedOperationInstance = new OperationInstance()
            .code(UPDATED_CODE)
            .libelle(UPDATED_LIBELLE)
            .segment(UPDATED_SEGMENT)
            .partie(UPDATED_PARTIE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .etat(UPDATED_ETAT)
            .degreEntrant(UPDATED_DEGRE_ENTRANT)
            .echeance(UPDATED_ECHEANCE)
            .dateEtat(UPDATED_DATE_ETAT)
            .grilleReponses(UPDATED_GRILLE_REPONSES);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        updatedOperationInstance.setLiaison(liaison);
        // Add required entity
        ReferenceOperation referenceOperation;
        if (TestUtil.findAll(em, ReferenceOperation.class).isEmpty()) {
            referenceOperation = ReferenceOperationResourceIT.createUpdatedEntity(em);
            em.persist(referenceOperation);
            em.flush();
        } else {
            referenceOperation = TestUtil.findAll(em, ReferenceOperation.class).getFirst();
        }
        updatedOperationInstance.setReferenceOperation(referenceOperation);
        return updatedOperationInstance;
    }

    @BeforeEach
    void initTest() {
        operationInstance = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedOperationInstance != null) {
            operationInstanceRepository.delete(insertedOperationInstance);
            insertedOperationInstance = null;
        }
    }

    @Test
    @Transactional
    void createOperationInstance() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OperationInstance
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(operationInstance);
        var returnedOperationInstanceDTO = om.readValue(
            restOperationInstanceMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(operationInstanceDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OperationInstanceDTO.class
        );

        // Validate the OperationInstance in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOperationInstance = operationInstanceMapper.toEntity(returnedOperationInstanceDTO);
        assertOperationInstanceUpdatableFieldsEquals(returnedOperationInstance, getPersistedOperationInstance(returnedOperationInstance));

        insertedOperationInstance = returnedOperationInstance;
    }

    @Test
    @Transactional
    void createOperationInstanceWithExistingId() throws Exception {
        // Create the OperationInstance with an existing ID
        insertedOperationInstance = operationInstanceRepository.saveAndFlush(operationInstance);
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(operationInstance);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOperationInstanceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(operationInstanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OperationInstance in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        operationInstance.setCode(null);

        // Create the OperationInstance, which fails.
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(operationInstance);

        restOperationInstanceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(operationInstanceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEtatIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        operationInstance.setEtat(null);

        // Create the OperationInstance, which fails.
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(operationInstance);

        restOperationInstanceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(operationInstanceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDegreEntrantIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        operationInstance.setDegreEntrant(null);

        // Create the OperationInstance, which fails.
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(operationInstance);

        restOperationInstanceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(operationInstanceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOperationInstances() throws Exception {
        // Initialize the database
        insertedOperationInstance = operationInstanceRepository.saveAndFlush(operationInstance);

        // Get all the operationInstanceList
        restOperationInstanceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(operationInstance.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].libelle").value(hasItem(DEFAULT_LIBELLE)))
            .andExpect(jsonPath("$.[*].segment").value(hasItem(DEFAULT_SEGMENT.toString())))
            .andExpect(jsonPath("$.[*].partie").value(hasItem(DEFAULT_PARTIE.toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())))
            .andExpect(jsonPath("$.[*].degreEntrant").value(hasItem(DEFAULT_DEGRE_ENTRANT)))
            .andExpect(jsonPath("$.[*].echeance").value(hasItem(DEFAULT_ECHEANCE.toString())))
            .andExpect(jsonPath("$.[*].dateEtat").value(hasItem(DEFAULT_DATE_ETAT.toString())))
            .andExpect(jsonPath("$.[*].grilleReponses").value(hasItem(DEFAULT_GRILLE_REPONSES)));
    }

    @Test
    @Transactional
    void getOperationInstance() throws Exception {
        // Initialize the database
        insertedOperationInstance = operationInstanceRepository.saveAndFlush(operationInstance);

        // Get the operationInstance
        restOperationInstanceMockMvc
            .perform(get(ENTITY_API_URL_ID, operationInstance.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(operationInstance.getId().toString()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.libelle").value(DEFAULT_LIBELLE))
            .andExpect(jsonPath("$.segment").value(DEFAULT_SEGMENT.toString()))
            .andExpect(jsonPath("$.partie").value(DEFAULT_PARTIE.toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT.toString()))
            .andExpect(jsonPath("$.degreEntrant").value(DEFAULT_DEGRE_ENTRANT))
            .andExpect(jsonPath("$.echeance").value(DEFAULT_ECHEANCE.toString()))
            .andExpect(jsonPath("$.dateEtat").value(DEFAULT_DATE_ETAT.toString()))
            .andExpect(jsonPath("$.grilleReponses").value(DEFAULT_GRILLE_REPONSES));
    }

    @Test
    @Transactional
    void getNonExistingOperationInstance() throws Exception {
        // Get the operationInstance
        restOperationInstanceMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOperationInstance() throws Exception {
        // Initialize the database
        insertedOperationInstance = operationInstanceRepository.saveAndFlush(operationInstance);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the operationInstance
        OperationInstance updatedOperationInstance = operationInstanceRepository.findById(operationInstance.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOperationInstance are not directly saved in db
        em.detach(updatedOperationInstance);
        updatedOperationInstance
            .code(UPDATED_CODE)
            .libelle(UPDATED_LIBELLE)
            .segment(UPDATED_SEGMENT)
            .partie(UPDATED_PARTIE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .etat(UPDATED_ETAT)
            .degreEntrant(UPDATED_DEGRE_ENTRANT)
            .echeance(UPDATED_ECHEANCE)
            .dateEtat(UPDATED_DATE_ETAT)
            .grilleReponses(UPDATED_GRILLE_REPONSES);
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(updatedOperationInstance);

        restOperationInstanceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, operationInstanceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(operationInstanceDTO))
            )
            .andExpect(status().isOk());

        // Validate the OperationInstance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOperationInstanceToMatchAllProperties(updatedOperationInstance);
    }

    @Test
    @Transactional
    void putNonExistingOperationInstance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        operationInstance.setId(UUID.randomUUID());

        // Create the OperationInstance
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(operationInstance);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOperationInstanceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, operationInstanceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(operationInstanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OperationInstance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOperationInstance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        operationInstance.setId(UUID.randomUUID());

        // Create the OperationInstance
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(operationInstance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOperationInstanceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(operationInstanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OperationInstance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOperationInstance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        operationInstance.setId(UUID.randomUUID());

        // Create the OperationInstance
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(operationInstance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOperationInstanceMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(operationInstanceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the OperationInstance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOperationInstanceWithPatch() throws Exception {
        // Initialize the database
        insertedOperationInstance = operationInstanceRepository.saveAndFlush(operationInstance);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the operationInstance using partial update
        OperationInstance partialUpdatedOperationInstance = new OperationInstance();
        partialUpdatedOperationInstance.setId(operationInstance.getId());

        partialUpdatedOperationInstance
            .code(UPDATED_CODE)
            .segment(UPDATED_SEGMENT)
            .partie(UPDATED_PARTIE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .etat(UPDATED_ETAT)
            .echeance(UPDATED_ECHEANCE)
            .dateEtat(UPDATED_DATE_ETAT);

        restOperationInstanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOperationInstance.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOperationInstance))
            )
            .andExpect(status().isOk());

        // Validate the OperationInstance in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOperationInstanceUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOperationInstance, operationInstance),
            getPersistedOperationInstance(operationInstance)
        );
    }

    @Test
    @Transactional
    void fullUpdateOperationInstanceWithPatch() throws Exception {
        // Initialize the database
        insertedOperationInstance = operationInstanceRepository.saveAndFlush(operationInstance);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the operationInstance using partial update
        OperationInstance partialUpdatedOperationInstance = new OperationInstance();
        partialUpdatedOperationInstance.setId(operationInstance.getId());

        partialUpdatedOperationInstance
            .code(UPDATED_CODE)
            .libelle(UPDATED_LIBELLE)
            .segment(UPDATED_SEGMENT)
            .partie(UPDATED_PARTIE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .etat(UPDATED_ETAT)
            .degreEntrant(UPDATED_DEGRE_ENTRANT)
            .echeance(UPDATED_ECHEANCE)
            .dateEtat(UPDATED_DATE_ETAT)
            .grilleReponses(UPDATED_GRILLE_REPONSES);

        restOperationInstanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOperationInstance.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOperationInstance))
            )
            .andExpect(status().isOk());

        // Validate the OperationInstance in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOperationInstanceUpdatableFieldsEquals(
            partialUpdatedOperationInstance,
            getPersistedOperationInstance(partialUpdatedOperationInstance)
        );
    }

    @Test
    @Transactional
    void patchNonExistingOperationInstance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        operationInstance.setId(UUID.randomUUID());

        // Create the OperationInstance
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(operationInstance);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOperationInstanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, operationInstanceDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(operationInstanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OperationInstance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOperationInstance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        operationInstance.setId(UUID.randomUUID());

        // Create the OperationInstance
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(operationInstance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOperationInstanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(operationInstanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OperationInstance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOperationInstance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        operationInstance.setId(UUID.randomUUID());

        // Create the OperationInstance
        OperationInstanceDTO operationInstanceDTO = operationInstanceMapper.toDto(operationInstance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOperationInstanceMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(operationInstanceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the OperationInstance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOperationInstance() throws Exception {
        // Initialize the database
        insertedOperationInstance = operationInstanceRepository.saveAndFlush(operationInstance);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the operationInstance
        restOperationInstanceMockMvc
            .perform(delete(ENTITY_API_URL_ID, operationInstance.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return operationInstanceRepository.count();
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

    protected OperationInstance getPersistedOperationInstance(OperationInstance operationInstance) {
        return operationInstanceRepository.findById(operationInstance.getId()).orElseThrow();
    }

    protected void assertPersistedOperationInstanceToMatchAllProperties(OperationInstance expectedOperationInstance) {
        assertOperationInstanceAllPropertiesEquals(expectedOperationInstance, getPersistedOperationInstance(expectedOperationInstance));
    }

    protected void assertPersistedOperationInstanceToMatchUpdatableProperties(OperationInstance expectedOperationInstance) {
        assertOperationInstanceAllUpdatablePropertiesEquals(
            expectedOperationInstance,
            getPersistedOperationInstance(expectedOperationInstance)
        );
    }
}
