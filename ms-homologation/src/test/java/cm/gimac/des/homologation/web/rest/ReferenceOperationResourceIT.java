package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.ReferenceOperationAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.repository.ReferenceOperationRepository;
import cm.gimac.des.homologation.service.dto.ReferenceOperationDTO;
import cm.gimac.des.homologation.service.mapper.ReferenceOperationMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link ReferenceOperationResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ReferenceOperationResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_LIBELLE = "AAAAAAAAAA";
    private static final String UPDATED_LIBELLE = "BBBBBBBBBB";

    private static final String DEFAULT_PHASE = "AAAAAAAAAA";
    private static final String UPDATED_PHASE = "BBBBBBBBBB";

    private static final Segment DEFAULT_SEGMENT = Segment.TRONC_COMMUN_AMONT;
    private static final Segment UPDATED_SEGMENT = Segment.BRANCHE;

    private static final Partie DEFAULT_PARTIE = Partie.DES;
    private static final Partie UPDATED_PARTIE = Partie.PARTICIPANT;

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final NatureCouverture DEFAULT_NATURE_COUVERTURE = NatureCouverture.CERTIFICATION;
    private static final NatureCouverture UPDATED_NATURE_COUVERTURE = NatureCouverture.MISE_EN_SERVICE;

    private static final String DEFAULT_PREUVE_ATTENDUE = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_PREUVE_ATTENDUE = "[\"B\"]";

    private static final String DEFAULT_ID_SCENARIO = "AAAAAAAAAA";
    private static final String UPDATED_ID_SCENARIO = "BBBBBBBBBB";

    private static final String DEFAULT_PREDECESSEURS = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_PREDECESSEURS = "[\"B\"]";

    private static final String DEFAULT_FONCTION_RESPONSABLE = "AAAAAAAAAA";
    private static final String UPDATED_FONCTION_RESPONSABLE = "BBBBBBBBBB";

    private static final String DEFAULT_GRILLE = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_GRILLE = "[\"B\"]";

    private static final String ENTITY_API_URL = "/api/reference-operations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReferenceOperationRepository referenceOperationRepository;

    @Autowired
    private ReferenceOperationMapper referenceOperationMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReferenceOperationMockMvc;

    private ReferenceOperation referenceOperation;

    private ReferenceOperation insertedReferenceOperation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReferenceOperation createEntity(EntityManager em) {
        ReferenceOperation referenceOperation = new ReferenceOperation()
            .code(DEFAULT_CODE)
            .libelle(DEFAULT_LIBELLE)
            .phase(DEFAULT_PHASE)
            .segment(DEFAULT_SEGMENT)
            .partie(DEFAULT_PARTIE)
            .serviceCode(DEFAULT_SERVICE_CODE)
            .natureCouverture(DEFAULT_NATURE_COUVERTURE)
            .preuveAttendue(DEFAULT_PREUVE_ATTENDUE)
            .idScenario(DEFAULT_ID_SCENARIO)
            .predecesseurs(DEFAULT_PREDECESSEURS)
            .fonctionResponsable(DEFAULT_FONCTION_RESPONSABLE)
            .grille(DEFAULT_GRILLE);
        // Add required entity
        ReferentialVersion referentialVersion;
        if (TestUtil.findAll(em, ReferentialVersion.class).isEmpty()) {
            referentialVersion = ReferentialVersionResourceIT.createEntity();
            em.persist(referentialVersion);
            em.flush();
        } else {
            referentialVersion = TestUtil.findAll(em, ReferentialVersion.class).getFirst();
        }
        referenceOperation.setVersion(referentialVersion);
        return referenceOperation;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReferenceOperation createUpdatedEntity(EntityManager em) {
        ReferenceOperation updatedReferenceOperation = new ReferenceOperation()
            .code(UPDATED_CODE)
            .libelle(UPDATED_LIBELLE)
            .phase(UPDATED_PHASE)
            .segment(UPDATED_SEGMENT)
            .partie(UPDATED_PARTIE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .natureCouverture(UPDATED_NATURE_COUVERTURE)
            .preuveAttendue(UPDATED_PREUVE_ATTENDUE)
            .idScenario(UPDATED_ID_SCENARIO)
            .predecesseurs(UPDATED_PREDECESSEURS)
            .fonctionResponsable(UPDATED_FONCTION_RESPONSABLE)
            .grille(UPDATED_GRILLE);
        // Add required entity
        ReferentialVersion referentialVersion;
        if (TestUtil.findAll(em, ReferentialVersion.class).isEmpty()) {
            referentialVersion = ReferentialVersionResourceIT.createUpdatedEntity();
            em.persist(referentialVersion);
            em.flush();
        } else {
            referentialVersion = TestUtil.findAll(em, ReferentialVersion.class).getFirst();
        }
        updatedReferenceOperation.setVersion(referentialVersion);
        return updatedReferenceOperation;
    }

    @BeforeEach
    void initTest() {
        referenceOperation = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedReferenceOperation != null) {
            referenceOperationRepository.delete(insertedReferenceOperation);
            insertedReferenceOperation = null;
        }
    }

    @Test
    @Transactional
    void createReferenceOperation() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);
        var returnedReferenceOperationDTO = om.readValue(
            restReferenceOperationMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(referenceOperationDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ReferenceOperationDTO.class
        );

        // Validate the ReferenceOperation in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedReferenceOperation = referenceOperationMapper.toEntity(returnedReferenceOperationDTO);
        assertReferenceOperationUpdatableFieldsEquals(
            returnedReferenceOperation,
            getPersistedReferenceOperation(returnedReferenceOperation)
        );

        insertedReferenceOperation = returnedReferenceOperation;
    }

    @Test
    @Transactional
    void createReferenceOperationWithExistingId() throws Exception {
        // Create the ReferenceOperation with an existing ID
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setCode(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLibelleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setLibelle(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPhaseIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setPhase(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSegmentIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setSegment(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPartieIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setPartie(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkNatureCouvertureIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setNatureCouverture(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllReferenceOperations() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList
        restReferenceOperationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(referenceOperation.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].libelle").value(hasItem(DEFAULT_LIBELLE)))
            .andExpect(jsonPath("$.[*].phase").value(hasItem(DEFAULT_PHASE)))
            .andExpect(jsonPath("$.[*].segment").value(hasItem(DEFAULT_SEGMENT.toString())))
            .andExpect(jsonPath("$.[*].partie").value(hasItem(DEFAULT_PARTIE.toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].natureCouverture").value(hasItem(DEFAULT_NATURE_COUVERTURE.toString())))
            .andExpect(jsonPath("$.[*].preuveAttendue").value(hasItem(DEFAULT_PREUVE_ATTENDUE)))
            .andExpect(jsonPath("$.[*].idScenario").value(hasItem(DEFAULT_ID_SCENARIO)))
            .andExpect(jsonPath("$.[*].predecesseurs").value(hasItem(DEFAULT_PREDECESSEURS)))
            .andExpect(jsonPath("$.[*].fonctionResponsable").value(hasItem(DEFAULT_FONCTION_RESPONSABLE)))
            .andExpect(jsonPath("$.[*].grille").value(hasItem(DEFAULT_GRILLE)));
    }

    @Test
    @Transactional
    void getReferenceOperation() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get the referenceOperation
        restReferenceOperationMockMvc
            .perform(get(ENTITY_API_URL_ID, referenceOperation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(referenceOperation.getId().toString()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.libelle").value(DEFAULT_LIBELLE))
            .andExpect(jsonPath("$.phase").value(DEFAULT_PHASE))
            .andExpect(jsonPath("$.segment").value(DEFAULT_SEGMENT.toString()))
            .andExpect(jsonPath("$.partie").value(DEFAULT_PARTIE.toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.natureCouverture").value(DEFAULT_NATURE_COUVERTURE.toString()))
            .andExpect(jsonPath("$.preuveAttendue").value(DEFAULT_PREUVE_ATTENDUE))
            .andExpect(jsonPath("$.idScenario").value(DEFAULT_ID_SCENARIO))
            .andExpect(jsonPath("$.predecesseurs").value(DEFAULT_PREDECESSEURS))
            .andExpect(jsonPath("$.fonctionResponsable").value(DEFAULT_FONCTION_RESPONSABLE))
            .andExpect(jsonPath("$.grille").value(DEFAULT_GRILLE));
    }

    @Test
    @Transactional
    void getNonExistingReferenceOperation() throws Exception {
        // Get the referenceOperation
        restReferenceOperationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReferenceOperation() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referenceOperation
        ReferenceOperation updatedReferenceOperation = referenceOperationRepository.findById(referenceOperation.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedReferenceOperation are not directly saved in db
        em.detach(updatedReferenceOperation);
        updatedReferenceOperation
            .code(UPDATED_CODE)
            .libelle(UPDATED_LIBELLE)
            .phase(UPDATED_PHASE)
            .segment(UPDATED_SEGMENT)
            .partie(UPDATED_PARTIE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .natureCouverture(UPDATED_NATURE_COUVERTURE)
            .preuveAttendue(UPDATED_PREUVE_ATTENDUE)
            .idScenario(UPDATED_ID_SCENARIO)
            .predecesseurs(UPDATED_PREDECESSEURS)
            .fonctionResponsable(UPDATED_FONCTION_RESPONSABLE)
            .grille(UPDATED_GRILLE);
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(updatedReferenceOperation);

        restReferenceOperationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, referenceOperationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isOk());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReferenceOperationToMatchAllProperties(updatedReferenceOperation);
    }

    @Test
    @Transactional
    void putNonExistingReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, referenceOperationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReferenceOperationWithPatch() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referenceOperation using partial update
        ReferenceOperation partialUpdatedReferenceOperation = new ReferenceOperation();
        partialUpdatedReferenceOperation.setId(referenceOperation.getId());

        partialUpdatedReferenceOperation
            .code(UPDATED_CODE)
            .phase(UPDATED_PHASE)
            .segment(UPDATED_SEGMENT)
            .partie(UPDATED_PARTIE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .idScenario(UPDATED_ID_SCENARIO)
            .predecesseurs(UPDATED_PREDECESSEURS)
            .fonctionResponsable(UPDATED_FONCTION_RESPONSABLE)
            .grille(UPDATED_GRILLE);

        restReferenceOperationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferenceOperation.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferenceOperation))
            )
            .andExpect(status().isOk());

        // Validate the ReferenceOperation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferenceOperationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedReferenceOperation, referenceOperation),
            getPersistedReferenceOperation(referenceOperation)
        );
    }

    @Test
    @Transactional
    void fullUpdateReferenceOperationWithPatch() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referenceOperation using partial update
        ReferenceOperation partialUpdatedReferenceOperation = new ReferenceOperation();
        partialUpdatedReferenceOperation.setId(referenceOperation.getId());

        partialUpdatedReferenceOperation
            .code(UPDATED_CODE)
            .libelle(UPDATED_LIBELLE)
            .phase(UPDATED_PHASE)
            .segment(UPDATED_SEGMENT)
            .partie(UPDATED_PARTIE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .natureCouverture(UPDATED_NATURE_COUVERTURE)
            .preuveAttendue(UPDATED_PREUVE_ATTENDUE)
            .idScenario(UPDATED_ID_SCENARIO)
            .predecesseurs(UPDATED_PREDECESSEURS)
            .fonctionResponsable(UPDATED_FONCTION_RESPONSABLE)
            .grille(UPDATED_GRILLE);

        restReferenceOperationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferenceOperation.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferenceOperation))
            )
            .andExpect(status().isOk());

        // Validate the ReferenceOperation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferenceOperationUpdatableFieldsEquals(
            partialUpdatedReferenceOperation,
            getPersistedReferenceOperation(partialUpdatedReferenceOperation)
        );
    }

    @Test
    @Transactional
    void patchNonExistingReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, referenceOperationDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReferenceOperation() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the referenceOperation
        restReferenceOperationMockMvc
            .perform(delete(ENTITY_API_URL_ID, referenceOperation.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return referenceOperationRepository.count();
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

    protected ReferenceOperation getPersistedReferenceOperation(ReferenceOperation referenceOperation) {
        return referenceOperationRepository.findById(referenceOperation.getId()).orElseThrow();
    }

    protected void assertPersistedReferenceOperationToMatchAllProperties(ReferenceOperation expectedReferenceOperation) {
        assertReferenceOperationAllPropertiesEquals(expectedReferenceOperation, getPersistedReferenceOperation(expectedReferenceOperation));
    }

    protected void assertPersistedReferenceOperationToMatchUpdatableProperties(ReferenceOperation expectedReferenceOperation) {
        assertReferenceOperationAllUpdatablePropertiesEquals(
            expectedReferenceOperation,
            getPersistedReferenceOperation(expectedReferenceOperation)
        );
    }
}
