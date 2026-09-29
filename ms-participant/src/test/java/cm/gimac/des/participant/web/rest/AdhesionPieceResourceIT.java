package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.security.AuthoritiesConstants;
import static cm.gimac.des.participant.domain.AdhesionPieceAsserts.*;
import static cm.gimac.des.participant.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.participant.IntegrationTest;
import cm.gimac.des.participant.domain.AdhesionPiece;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import cm.gimac.des.participant.repository.AdhesionPieceRepository;
import cm.gimac.des.participant.service.AdhesionPieceService;
import cm.gimac.des.participant.service.dto.AdhesionPieceDTO;
import cm.gimac.des.participant.service.mapper.AdhesionPieceMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link AdhesionPieceResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class AdhesionPieceResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final Boolean DEFAULT_MANDATORY = false;
    private static final Boolean UPDATED_MANDATORY = true;

    private static final AdhesionPieceState DEFAULT_STATE = AdhesionPieceState.ATTENDUE;
    private static final AdhesionPieceState UPDATED_STATE = AdhesionPieceState.RECUE;

    private static final UUID DEFAULT_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_DOCUMENT_ID = UUID.randomUUID();

    private static final String DEFAULT_DOCUMENT_SHA_256 = "025ebc402faf0c768029ae7d0ced23957c8e9b81fe7e582885b9ec35b3d9fbbc";
    private static final String UPDATED_DOCUMENT_SHA_256 = "690101c90067f3f8c410e1cfcc08f4786eb0ac0e8f4ab69b3a38d6f204e788b0";

    private static final String DEFAULT_FILE_NAME = "AAAAAAAAAA";
    private static final String UPDATED_FILE_NAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_DEPOSITED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DEPOSITED_AT = Instant.ofEpochMilli(1790694080285L);

    private static final UUID DEFAULT_DEPOSITED_BY_ID = UUID.randomUUID();
    private static final UUID UPDATED_DEPOSITED_BY_ID = UUID.randomUUID();

    private static final Instant DEFAULT_REVIEWED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REVIEWED_AT = Instant.ofEpochMilli(1790694080285L);

    private static final UUID DEFAULT_REVIEWED_BY_ID = UUID.randomUUID();
    private static final UUID UPDATED_REVIEWED_BY_ID = UUID.randomUUID();

    private static final String DEFAULT_REJECTION_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REJECTION_REASON = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/adhesion-pieces";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AdhesionPieceRepository adhesionPieceRepository;

    @Mock
    private AdhesionPieceRepository adhesionPieceRepositoryMock;

    @Autowired
    private AdhesionPieceMapper adhesionPieceMapper;

    @Mock
    private AdhesionPieceService adhesionPieceServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAdhesionPieceMockMvc;

    private AdhesionPiece adhesionPiece;

    private AdhesionPiece insertedAdhesionPiece;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AdhesionPiece createEntity(EntityManager em) {
        AdhesionPiece adhesionPiece = new AdhesionPiece()
            .code(DEFAULT_CODE)
            .label(DEFAULT_LABEL)
            .mandatory(DEFAULT_MANDATORY)
            .state(DEFAULT_STATE)
            .documentId(DEFAULT_DOCUMENT_ID)
            .documentSha256(DEFAULT_DOCUMENT_SHA_256)
            .fileName(DEFAULT_FILE_NAME)
            .depositedAt(DEFAULT_DEPOSITED_AT)
            .depositedById(DEFAULT_DEPOSITED_BY_ID)
            .reviewedAt(DEFAULT_REVIEWED_AT)
            .reviewedById(DEFAULT_REVIEWED_BY_ID)
            .rejectionReason(DEFAULT_REJECTION_REASON);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        adhesionPiece.setInstitution(institution);
        return adhesionPiece;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AdhesionPiece createUpdatedEntity(EntityManager em) {
        AdhesionPiece updatedAdhesionPiece = new AdhesionPiece()
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .mandatory(UPDATED_MANDATORY)
            .state(UPDATED_STATE)
            .documentId(UPDATED_DOCUMENT_ID)
            .documentSha256(UPDATED_DOCUMENT_SHA_256)
            .fileName(UPDATED_FILE_NAME)
            .depositedAt(UPDATED_DEPOSITED_AT)
            .depositedById(UPDATED_DEPOSITED_BY_ID)
            .reviewedAt(UPDATED_REVIEWED_AT)
            .reviewedById(UPDATED_REVIEWED_BY_ID)
            .rejectionReason(UPDATED_REJECTION_REASON);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createUpdatedEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        updatedAdhesionPiece.setInstitution(institution);
        return updatedAdhesionPiece;
    }

    @BeforeEach
    void initTest() {
        adhesionPiece = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAdhesionPiece != null) {
            adhesionPieceRepository.delete(insertedAdhesionPiece);
            insertedAdhesionPiece = null;
        }
    }

    @Test
    @Transactional
    void createAdhesionPiece() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AdhesionPiece
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);
        var returnedAdhesionPieceDTO = om.readValue(
            restAdhesionPieceMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(adhesionPieceDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AdhesionPieceDTO.class
        );

        // Validate the AdhesionPiece in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAdhesionPiece = adhesionPieceMapper.toEntity(returnedAdhesionPieceDTO);
        assertAdhesionPieceUpdatableFieldsEquals(returnedAdhesionPiece, getPersistedAdhesionPiece(returnedAdhesionPiece));

        insertedAdhesionPiece = returnedAdhesionPiece;
    }

    @Test
    @Transactional
    void createAdhesionPieceWithExistingId() throws Exception {
        // Create the AdhesionPiece with an existing ID
        insertedAdhesionPiece = adhesionPieceRepository.saveAndFlush(adhesionPiece);
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAdhesionPieceMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AdhesionPiece in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        adhesionPiece.setCode(null);

        // Create the AdhesionPiece, which fails.
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);

        restAdhesionPieceMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLabelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        adhesionPiece.setLabel(null);

        // Create the AdhesionPiece, which fails.
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);

        restAdhesionPieceMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMandatoryIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        adhesionPiece.setMandatory(null);

        // Create the AdhesionPiece, which fails.
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);

        restAdhesionPieceMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        adhesionPiece.setState(null);

        // Create the AdhesionPiece, which fails.
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);

        restAdhesionPieceMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAdhesionPieces() throws Exception {
        // Initialize the database
        insertedAdhesionPiece = adhesionPieceRepository.saveAndFlush(adhesionPiece);

        // Get all the adhesionPieceList
        restAdhesionPieceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(adhesionPiece.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].mandatory").value(hasItem(DEFAULT_MANDATORY)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].documentId").value(hasItem(DEFAULT_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].documentSha256").value(hasItem(DEFAULT_DOCUMENT_SHA_256)))
            .andExpect(jsonPath("$.[*].fileName").value(hasItem(DEFAULT_FILE_NAME)))
            .andExpect(jsonPath("$.[*].depositedAt").value(hasItem(DEFAULT_DEPOSITED_AT.toString())))
            .andExpect(jsonPath("$.[*].depositedById").value(hasItem(DEFAULT_DEPOSITED_BY_ID.toString())))
            .andExpect(jsonPath("$.[*].reviewedAt").value(hasItem(DEFAULT_REVIEWED_AT.toString())))
            .andExpect(jsonPath("$.[*].reviewedById").value(hasItem(DEFAULT_REVIEWED_BY_ID.toString())))
            .andExpect(jsonPath("$.[*].rejectionReason").value(hasItem(DEFAULT_REJECTION_REASON)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAdhesionPiecesWithEagerRelationshipsIsEnabled() throws Exception {
        when(adhesionPieceServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restAdhesionPieceMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(adhesionPieceServiceMock, times(1)).findAllWithEagerRelationships();
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAdhesionPiecesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(adhesionPieceServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restAdhesionPieceMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(adhesionPieceRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getAdhesionPiece() throws Exception {
        // Initialize the database
        insertedAdhesionPiece = adhesionPieceRepository.saveAndFlush(adhesionPiece);

        // Get the adhesionPiece
        restAdhesionPieceMockMvc
            .perform(get(ENTITY_API_URL_ID, adhesionPiece.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(adhesionPiece.getId().toString()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andExpect(jsonPath("$.mandatory").value(DEFAULT_MANDATORY))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.documentId").value(DEFAULT_DOCUMENT_ID.toString()))
            .andExpect(jsonPath("$.documentSha256").value(DEFAULT_DOCUMENT_SHA_256))
            .andExpect(jsonPath("$.fileName").value(DEFAULT_FILE_NAME))
            .andExpect(jsonPath("$.depositedAt").value(DEFAULT_DEPOSITED_AT.toString()))
            .andExpect(jsonPath("$.depositedById").value(DEFAULT_DEPOSITED_BY_ID.toString()))
            .andExpect(jsonPath("$.reviewedAt").value(DEFAULT_REVIEWED_AT.toString()))
            .andExpect(jsonPath("$.reviewedById").value(DEFAULT_REVIEWED_BY_ID.toString()))
            .andExpect(jsonPath("$.rejectionReason").value(DEFAULT_REJECTION_REASON));
    }

    @Test
    @Transactional
    void getNonExistingAdhesionPiece() throws Exception {
        // Get the adhesionPiece
        restAdhesionPieceMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAdhesionPiece() throws Exception {
        // Initialize the database
        insertedAdhesionPiece = adhesionPieceRepository.saveAndFlush(adhesionPiece);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the adhesionPiece
        AdhesionPiece updatedAdhesionPiece = adhesionPieceRepository.findById(adhesionPiece.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAdhesionPiece are not directly saved in db
        em.detach(updatedAdhesionPiece);
        updatedAdhesionPiece
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .mandatory(UPDATED_MANDATORY)
            .state(UPDATED_STATE)
            .documentId(UPDATED_DOCUMENT_ID)
            .documentSha256(UPDATED_DOCUMENT_SHA_256)
            .fileName(UPDATED_FILE_NAME)
            .depositedAt(UPDATED_DEPOSITED_AT)
            .depositedById(UPDATED_DEPOSITED_BY_ID)
            .reviewedAt(UPDATED_REVIEWED_AT)
            .reviewedById(UPDATED_REVIEWED_BY_ID)
            .rejectionReason(UPDATED_REJECTION_REASON);
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(updatedAdhesionPiece);

        restAdhesionPieceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, adhesionPieceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isOk());

        // Validate the AdhesionPiece in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAdhesionPieceToMatchAllProperties(updatedAdhesionPiece);
    }

    @Test
    @Transactional
    void putNonExistingAdhesionPiece() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adhesionPiece.setId(UUID.randomUUID());

        // Create the AdhesionPiece
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAdhesionPieceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, adhesionPieceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AdhesionPiece in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAdhesionPiece() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adhesionPiece.setId(UUID.randomUUID());

        // Create the AdhesionPiece
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAdhesionPieceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AdhesionPiece in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAdhesionPiece() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adhesionPiece.setId(UUID.randomUUID());

        // Create the AdhesionPiece
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAdhesionPieceMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AdhesionPiece in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAdhesionPieceWithPatch() throws Exception {
        // Initialize the database
        insertedAdhesionPiece = adhesionPieceRepository.saveAndFlush(adhesionPiece);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the adhesionPiece using partial update
        AdhesionPiece partialUpdatedAdhesionPiece = new AdhesionPiece();
        partialUpdatedAdhesionPiece.setId(adhesionPiece.getId());

        partialUpdatedAdhesionPiece
            .label(UPDATED_LABEL)
            .mandatory(UPDATED_MANDATORY)
            .documentId(UPDATED_DOCUMENT_ID)
            .fileName(UPDATED_FILE_NAME)
            .reviewedById(UPDATED_REVIEWED_BY_ID)
            .rejectionReason(UPDATED_REJECTION_REASON);

        restAdhesionPieceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAdhesionPiece.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAdhesionPiece))
            )
            .andExpect(status().isOk());

        // Validate the AdhesionPiece in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAdhesionPieceUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAdhesionPiece, adhesionPiece),
            getPersistedAdhesionPiece(adhesionPiece)
        );
    }

    @Test
    @Transactional
    void fullUpdateAdhesionPieceWithPatch() throws Exception {
        // Initialize the database
        insertedAdhesionPiece = adhesionPieceRepository.saveAndFlush(adhesionPiece);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the adhesionPiece using partial update
        AdhesionPiece partialUpdatedAdhesionPiece = new AdhesionPiece();
        partialUpdatedAdhesionPiece.setId(adhesionPiece.getId());

        partialUpdatedAdhesionPiece
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .mandatory(UPDATED_MANDATORY)
            .state(UPDATED_STATE)
            .documentId(UPDATED_DOCUMENT_ID)
            .documentSha256(UPDATED_DOCUMENT_SHA_256)
            .fileName(UPDATED_FILE_NAME)
            .depositedAt(UPDATED_DEPOSITED_AT)
            .depositedById(UPDATED_DEPOSITED_BY_ID)
            .reviewedAt(UPDATED_REVIEWED_AT)
            .reviewedById(UPDATED_REVIEWED_BY_ID)
            .rejectionReason(UPDATED_REJECTION_REASON);

        restAdhesionPieceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAdhesionPiece.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAdhesionPiece))
            )
            .andExpect(status().isOk());

        // Validate the AdhesionPiece in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAdhesionPieceUpdatableFieldsEquals(partialUpdatedAdhesionPiece, getPersistedAdhesionPiece(partialUpdatedAdhesionPiece));
    }

    @Test
    @Transactional
    void patchNonExistingAdhesionPiece() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adhesionPiece.setId(UUID.randomUUID());

        // Create the AdhesionPiece
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAdhesionPieceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, adhesionPieceDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AdhesionPiece in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAdhesionPiece() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adhesionPiece.setId(UUID.randomUUID());

        // Create the AdhesionPiece
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAdhesionPieceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AdhesionPiece in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAdhesionPiece() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adhesionPiece.setId(UUID.randomUUID());

        // Create the AdhesionPiece
        AdhesionPieceDTO adhesionPieceDTO = adhesionPieceMapper.toDto(adhesionPiece);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAdhesionPieceMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(adhesionPieceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AdhesionPiece in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAdhesionPiece() throws Exception {
        // Initialize the database
        insertedAdhesionPiece = adhesionPieceRepository.saveAndFlush(adhesionPiece);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the adhesionPiece
        restAdhesionPieceMockMvc
            .perform(delete(ENTITY_API_URL_ID, adhesionPiece.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return adhesionPieceRepository.count();
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

    protected AdhesionPiece getPersistedAdhesionPiece(AdhesionPiece adhesionPiece) {
        return adhesionPieceRepository.findById(adhesionPiece.getId()).orElseThrow();
    }

    protected void assertPersistedAdhesionPieceToMatchAllProperties(AdhesionPiece expectedAdhesionPiece) {
        assertAdhesionPieceAllPropertiesEquals(expectedAdhesionPiece, getPersistedAdhesionPiece(expectedAdhesionPiece));
    }

    protected void assertPersistedAdhesionPieceToMatchUpdatableProperties(AdhesionPiece expectedAdhesionPiece) {
        assertAdhesionPieceAllUpdatablePropertiesEquals(expectedAdhesionPiece, getPersistedAdhesionPiece(expectedAdhesionPiece));
    }
}
