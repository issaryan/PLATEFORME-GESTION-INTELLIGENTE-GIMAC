package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.MilestoneApprovalAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.Milestone;
import cm.gimac.des.pilotage.domain.MilestoneApproval;
import cm.gimac.des.pilotage.domain.enumeration.ApprovalAuthority;
import cm.gimac.des.pilotage.repository.MilestoneApprovalRepository;
import cm.gimac.des.pilotage.service.MilestoneApprovalService;
import cm.gimac.des.pilotage.service.dto.MilestoneApprovalDTO;
import cm.gimac.des.pilotage.service.mapper.MilestoneApprovalMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link MilestoneApprovalResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class MilestoneApprovalResourceIT {

    private static final ApprovalAuthority DEFAULT_AUTHORITY = ApprovalAuthority.PROJETS;
    private static final ApprovalAuthority UPDATED_AUTHORITY = ApprovalAuthority.SECURITE;

    private static final UUID DEFAULT_APPROVER_ID = UUID.randomUUID();
    private static final UUID UPDATED_APPROVER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_APPROVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_APPROVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_COMMENT = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/milestone-approvals";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MilestoneApprovalRepository milestoneApprovalRepository;

    @Mock
    private MilestoneApprovalRepository milestoneApprovalRepositoryMock;

    @Autowired
    private MilestoneApprovalMapper milestoneApprovalMapper;

    @Mock
    private MilestoneApprovalService milestoneApprovalServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMilestoneApprovalMockMvc;

    private MilestoneApproval milestoneApproval;

    private MilestoneApproval insertedMilestoneApproval;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MilestoneApproval createEntity(EntityManager em) {
        MilestoneApproval milestoneApproval = new MilestoneApproval()
            .authority(DEFAULT_AUTHORITY)
            .approverId(DEFAULT_APPROVER_ID)
            .approvedAt(DEFAULT_APPROVED_AT)
            .comment(DEFAULT_COMMENT);
        // Add required entity
        Milestone milestone;
        if (TestUtil.findAll(em, Milestone.class).isEmpty()) {
            milestone = MilestoneResourceIT.createEntity(em);
            em.persist(milestone);
            em.flush();
        } else {
            milestone = TestUtil.findAll(em, Milestone.class).get(0);
        }
        milestoneApproval.setMilestone(milestone);
        return milestoneApproval;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MilestoneApproval createUpdatedEntity(EntityManager em) {
        MilestoneApproval updatedMilestoneApproval = new MilestoneApproval()
            .authority(UPDATED_AUTHORITY)
            .approverId(UPDATED_APPROVER_ID)
            .approvedAt(UPDATED_APPROVED_AT)
            .comment(UPDATED_COMMENT);
        // Add required entity
        Milestone milestone;
        if (TestUtil.findAll(em, Milestone.class).isEmpty()) {
            milestone = MilestoneResourceIT.createUpdatedEntity(em);
            em.persist(milestone);
            em.flush();
        } else {
            milestone = TestUtil.findAll(em, Milestone.class).get(0);
        }
        updatedMilestoneApproval.setMilestone(milestone);
        return updatedMilestoneApproval;
    }

    @BeforeEach
    void initTest() {
        milestoneApproval = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedMilestoneApproval != null) {
            milestoneApprovalRepository.delete(insertedMilestoneApproval);
            insertedMilestoneApproval = null;
        }
    }

    @Test
    @Transactional
    void createMilestoneApproval() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the MilestoneApproval
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(milestoneApproval);
        var returnedMilestoneApprovalDTO = om.readValue(
            restMilestoneApprovalMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(milestoneApprovalDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            MilestoneApprovalDTO.class
        );

        // Validate the MilestoneApproval in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedMilestoneApproval = milestoneApprovalMapper.toEntity(returnedMilestoneApprovalDTO);
        assertMilestoneApprovalUpdatableFieldsEquals(returnedMilestoneApproval, getPersistedMilestoneApproval(returnedMilestoneApproval));

        insertedMilestoneApproval = returnedMilestoneApproval;
    }

    @Test
    @Transactional
    void createMilestoneApprovalWithExistingId() throws Exception {
        // Create the MilestoneApproval with an existing ID
        insertedMilestoneApproval = milestoneApprovalRepository.saveAndFlush(milestoneApproval);
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(milestoneApproval);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restMilestoneApprovalMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(milestoneApprovalDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MilestoneApproval in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkAuthorityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        milestoneApproval.setAuthority(null);

        // Create the MilestoneApproval, which fails.
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(milestoneApproval);

        restMilestoneApprovalMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(milestoneApprovalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkApproverIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        milestoneApproval.setApproverId(null);

        // Create the MilestoneApproval, which fails.
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(milestoneApproval);

        restMilestoneApprovalMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(milestoneApprovalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkApprovedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        milestoneApproval.setApprovedAt(null);

        // Create the MilestoneApproval, which fails.
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(milestoneApproval);

        restMilestoneApprovalMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(milestoneApprovalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllMilestoneApprovals() throws Exception {
        // Initialize the database
        insertedMilestoneApproval = milestoneApprovalRepository.saveAndFlush(milestoneApproval);

        // Get all the milestoneApprovalList
        restMilestoneApprovalMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(milestoneApproval.getId().toString())))
            .andExpect(jsonPath("$.[*].authority").value(hasItem(DEFAULT_AUTHORITY.toString())))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].approvedAt").value(hasItem(DEFAULT_APPROVED_AT.toString())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMilestoneApprovalsWithEagerRelationshipsIsEnabled() throws Exception {
        when(milestoneApprovalServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMilestoneApprovalMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(milestoneApprovalServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMilestoneApprovalsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(milestoneApprovalServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMilestoneApprovalMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(milestoneApprovalRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getMilestoneApproval() throws Exception {
        // Initialize the database
        insertedMilestoneApproval = milestoneApprovalRepository.saveAndFlush(milestoneApproval);

        // Get the milestoneApproval
        restMilestoneApprovalMockMvc
            .perform(get(ENTITY_API_URL_ID, milestoneApproval.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(milestoneApproval.getId().toString()))
            .andExpect(jsonPath("$.authority").value(DEFAULT_AUTHORITY.toString()))
            .andExpect(jsonPath("$.approverId").value(DEFAULT_APPROVER_ID.toString()))
            .andExpect(jsonPath("$.approvedAt").value(DEFAULT_APPROVED_AT.toString()))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT));
    }

    @Test
    @Transactional
    void getNonExistingMilestoneApproval() throws Exception {
        // Get the milestoneApproval
        restMilestoneApprovalMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingMilestoneApproval() throws Exception {
        // Initialize the database
        insertedMilestoneApproval = milestoneApprovalRepository.saveAndFlush(milestoneApproval);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the milestoneApproval
        MilestoneApproval updatedMilestoneApproval = milestoneApprovalRepository.findById(milestoneApproval.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedMilestoneApproval are not directly saved in db
        em.detach(updatedMilestoneApproval);
        updatedMilestoneApproval
            .authority(UPDATED_AUTHORITY)
            .approverId(UPDATED_APPROVER_ID)
            .approvedAt(UPDATED_APPROVED_AT)
            .comment(UPDATED_COMMENT);
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(updatedMilestoneApproval);

        restMilestoneApprovalMockMvc
            .perform(
                put(ENTITY_API_URL_ID, milestoneApprovalDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(milestoneApprovalDTO))
            )
            .andExpect(status().isOk());

        // Validate the MilestoneApproval in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedMilestoneApprovalToMatchAllProperties(updatedMilestoneApproval);
    }

    @Test
    @Transactional
    void putNonExistingMilestoneApproval() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestoneApproval.setId(UUID.randomUUID());

        // Create the MilestoneApproval
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(milestoneApproval);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMilestoneApprovalMockMvc
            .perform(
                put(ENTITY_API_URL_ID, milestoneApprovalDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(milestoneApprovalDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MilestoneApproval in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchMilestoneApproval() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestoneApproval.setId(UUID.randomUUID());

        // Create the MilestoneApproval
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(milestoneApproval);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMilestoneApprovalMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(milestoneApprovalDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MilestoneApproval in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamMilestoneApproval() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestoneApproval.setId(UUID.randomUUID());

        // Create the MilestoneApproval
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(milestoneApproval);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMilestoneApprovalMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(milestoneApprovalDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the MilestoneApproval in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateMilestoneApprovalWithPatch() throws Exception {
        // Initialize the database
        insertedMilestoneApproval = milestoneApprovalRepository.saveAndFlush(milestoneApproval);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the milestoneApproval using partial update
        MilestoneApproval partialUpdatedMilestoneApproval = new MilestoneApproval();
        partialUpdatedMilestoneApproval.setId(milestoneApproval.getId());

        partialUpdatedMilestoneApproval.approverId(UPDATED_APPROVER_ID);

        restMilestoneApprovalMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMilestoneApproval.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMilestoneApproval))
            )
            .andExpect(status().isOk());

        // Validate the MilestoneApproval in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMilestoneApprovalUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedMilestoneApproval, milestoneApproval),
            getPersistedMilestoneApproval(milestoneApproval)
        );
    }

    @Test
    @Transactional
    void fullUpdateMilestoneApprovalWithPatch() throws Exception {
        // Initialize the database
        insertedMilestoneApproval = milestoneApprovalRepository.saveAndFlush(milestoneApproval);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the milestoneApproval using partial update
        MilestoneApproval partialUpdatedMilestoneApproval = new MilestoneApproval();
        partialUpdatedMilestoneApproval.setId(milestoneApproval.getId());

        partialUpdatedMilestoneApproval
            .authority(UPDATED_AUTHORITY)
            .approverId(UPDATED_APPROVER_ID)
            .approvedAt(UPDATED_APPROVED_AT)
            .comment(UPDATED_COMMENT);

        restMilestoneApprovalMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMilestoneApproval.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMilestoneApproval))
            )
            .andExpect(status().isOk());

        // Validate the MilestoneApproval in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMilestoneApprovalUpdatableFieldsEquals(
            partialUpdatedMilestoneApproval,
            getPersistedMilestoneApproval(partialUpdatedMilestoneApproval)
        );
    }

    @Test
    @Transactional
    void patchNonExistingMilestoneApproval() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestoneApproval.setId(UUID.randomUUID());

        // Create the MilestoneApproval
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(milestoneApproval);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMilestoneApprovalMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, milestoneApprovalDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(milestoneApprovalDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MilestoneApproval in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchMilestoneApproval() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestoneApproval.setId(UUID.randomUUID());

        // Create the MilestoneApproval
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(milestoneApproval);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMilestoneApprovalMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(milestoneApprovalDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MilestoneApproval in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamMilestoneApproval() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        milestoneApproval.setId(UUID.randomUUID());

        // Create the MilestoneApproval
        MilestoneApprovalDTO milestoneApprovalDTO = milestoneApprovalMapper.toDto(milestoneApproval);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMilestoneApprovalMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(milestoneApprovalDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the MilestoneApproval in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteMilestoneApproval() throws Exception {
        // Initialize the database
        insertedMilestoneApproval = milestoneApprovalRepository.saveAndFlush(milestoneApproval);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the milestoneApproval
        restMilestoneApprovalMockMvc
            .perform(delete(ENTITY_API_URL_ID, milestoneApproval.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return milestoneApprovalRepository.count();
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

    protected MilestoneApproval getPersistedMilestoneApproval(MilestoneApproval milestoneApproval) {
        return milestoneApprovalRepository.findById(milestoneApproval.getId()).orElseThrow();
    }

    protected void assertPersistedMilestoneApprovalToMatchAllProperties(MilestoneApproval expectedMilestoneApproval) {
        assertMilestoneApprovalAllPropertiesEquals(expectedMilestoneApproval, getPersistedMilestoneApproval(expectedMilestoneApproval));
    }

    protected void assertPersistedMilestoneApprovalToMatchUpdatableProperties(MilestoneApproval expectedMilestoneApproval) {
        assertMilestoneApprovalAllUpdatablePropertiesEquals(
            expectedMilestoneApproval,
            getPersistedMilestoneApproval(expectedMilestoneApproval)
        );
    }
}
