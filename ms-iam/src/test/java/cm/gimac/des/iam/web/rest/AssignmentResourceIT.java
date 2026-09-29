package cm.gimac.des.iam.web.rest;

import static cm.gimac.des.iam.domain.AssignmentAsserts.*;
import static cm.gimac.des.iam.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.iam.IntegrationTest;
import cm.gimac.des.iam.domain.Assignment;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.repository.AssignmentRepository;
import cm.gimac.des.iam.service.AssignmentService;
import cm.gimac.des.iam.service.dto.AssignmentDTO;
import cm.gimac.des.iam.service.mapper.AssignmentMapper;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link AssignmentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.iam.security.AuthoritiesConstants.ADMIN)
class AssignmentResourceIT {

    private static final String DEFAULT_DEPARTMENT = "AAAAAAAAAA";
    private static final String UPDATED_DEPARTMENT = "BBBBBBBBBB";

    private static final String DEFAULT_UNIT = "AAAAAAAAAA";
    private static final String UPDATED_UNIT = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_STARTS_AT = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_STARTS_AT = LocalDate.parse("2023-12-17");

    private static final LocalDate DEFAULT_ENDS_AT = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_ENDS_AT = LocalDate.parse("2023-12-17");

    private static final String ENTITY_API_URL = "/api/assignments";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentRepository assignmentRepositoryMock;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Mock
    private AssignmentService assignmentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAssignmentMockMvc;

    private Assignment assignment;

    private Assignment insertedAssignment;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Assignment createEntity(EntityManager em) {
        Assignment assignment = new Assignment()
            .department(DEFAULT_DEPARTMENT)
            .unit(DEFAULT_UNIT)
            .startsAt(DEFAULT_STARTS_AT)
            .endsAt(DEFAULT_ENDS_AT);
        // Add required entity
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            collaborator = CollaboratorResourceIT.createEntity();
            em.persist(collaborator);
            em.flush();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        assignment.setCollaborator(collaborator);
        return assignment;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Assignment createUpdatedEntity(EntityManager em) {
        Assignment updatedAssignment = new Assignment()
            .department(UPDATED_DEPARTMENT)
            .unit(UPDATED_UNIT)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT);
        // Add required entity
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            collaborator = CollaboratorResourceIT.createUpdatedEntity();
            em.persist(collaborator);
            em.flush();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        updatedAssignment.setCollaborator(collaborator);
        return updatedAssignment;
    }

    @BeforeEach
    void initTest() {
        assignment = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAssignment != null) {
            assignmentRepository.delete(insertedAssignment);
            insertedAssignment = null;
        }
    }

    @Test
    @Transactional
    void createAssignment() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Assignment
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(assignment);
        var returnedAssignmentDTO = om.readValue(
            restAssignmentMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assignmentDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AssignmentDTO.class
        );

        // Validate the Assignment in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAssignment = assignmentMapper.toEntity(returnedAssignmentDTO);
        assertAssignmentUpdatableFieldsEquals(returnedAssignment, getPersistedAssignment(returnedAssignment));

        insertedAssignment = returnedAssignment;
    }

    @Test
    @Transactional
    void createAssignmentWithExistingId() throws Exception {
        // Create the Assignment with an existing ID
        insertedAssignment = assignmentRepository.saveAndFlush(assignment);
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(assignment);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAssignmentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assignmentDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDepartmentIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assignment.setDepartment(null);

        // Create the Assignment, which fails.
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(assignment);

        restAssignmentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assignmentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUnitIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assignment.setUnit(null);

        // Create the Assignment, which fails.
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(assignment);

        restAssignmentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assignmentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStartsAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assignment.setStartsAt(null);

        // Create the Assignment, which fails.
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(assignment);

        restAssignmentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assignmentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAssignments() throws Exception {
        // Initialize the database
        insertedAssignment = assignmentRepository.saveAndFlush(assignment);

        // Get all the assignmentList
        restAssignmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(assignment.getId().toString())))
            .andExpect(jsonPath("$.[*].department").value(hasItem(DEFAULT_DEPARTMENT)))
            .andExpect(jsonPath("$.[*].unit").value(hasItem(DEFAULT_UNIT)))
            .andExpect(jsonPath("$.[*].startsAt").value(hasItem(DEFAULT_STARTS_AT.toString())))
            .andExpect(jsonPath("$.[*].endsAt").value(hasItem(DEFAULT_ENDS_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAssignmentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(assignmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAssignmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(assignmentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAssignmentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(assignmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAssignmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(assignmentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getAssignment() throws Exception {
        // Initialize the database
        insertedAssignment = assignmentRepository.saveAndFlush(assignment);

        // Get the assignment
        restAssignmentMockMvc
            .perform(get(ENTITY_API_URL_ID, assignment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(assignment.getId().toString()))
            .andExpect(jsonPath("$.department").value(DEFAULT_DEPARTMENT))
            .andExpect(jsonPath("$.unit").value(DEFAULT_UNIT))
            .andExpect(jsonPath("$.startsAt").value(DEFAULT_STARTS_AT.toString()))
            .andExpect(jsonPath("$.endsAt").value(DEFAULT_ENDS_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingAssignment() throws Exception {
        // Get the assignment
        restAssignmentMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAssignment() throws Exception {
        // Initialize the database
        insertedAssignment = assignmentRepository.saveAndFlush(assignment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the assignment
        Assignment updatedAssignment = assignmentRepository.findById(assignment.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAssignment are not directly saved in db
        em.detach(updatedAssignment);
        updatedAssignment.department(UPDATED_DEPARTMENT).unit(UPDATED_UNIT).startsAt(UPDATED_STARTS_AT).endsAt(UPDATED_ENDS_AT);
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(updatedAssignment);

        restAssignmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, assignmentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(assignmentDTO))
            )
            .andExpect(status().isOk());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAssignmentToMatchAllProperties(updatedAssignment);
    }

    @Test
    @Transactional
    void putNonExistingAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(UUID.randomUUID());

        // Create the Assignment
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(assignment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, assignmentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(assignmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(UUID.randomUUID());

        // Create the Assignment
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(assignment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(assignmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(UUID.randomUUID());

        // Create the Assignment
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(assignment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assignmentDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAssignmentWithPatch() throws Exception {
        // Initialize the database
        insertedAssignment = assignmentRepository.saveAndFlush(assignment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the assignment using partial update
        Assignment partialUpdatedAssignment = new Assignment();
        partialUpdatedAssignment.setId(assignment.getId());

        restAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAssignment.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAssignment))
            )
            .andExpect(status().isOk());

        // Validate the Assignment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAssignmentUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAssignment, assignment),
            getPersistedAssignment(assignment)
        );
    }

    @Test
    @Transactional
    void fullUpdateAssignmentWithPatch() throws Exception {
        // Initialize the database
        insertedAssignment = assignmentRepository.saveAndFlush(assignment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the assignment using partial update
        Assignment partialUpdatedAssignment = new Assignment();
        partialUpdatedAssignment.setId(assignment.getId());

        partialUpdatedAssignment.department(UPDATED_DEPARTMENT).unit(UPDATED_UNIT).startsAt(UPDATED_STARTS_AT).endsAt(UPDATED_ENDS_AT);

        restAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAssignment.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAssignment))
            )
            .andExpect(status().isOk());

        // Validate the Assignment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAssignmentUpdatableFieldsEquals(partialUpdatedAssignment, getPersistedAssignment(partialUpdatedAssignment));
    }

    @Test
    @Transactional
    void patchNonExistingAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(UUID.randomUUID());

        // Create the Assignment
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(assignment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, assignmentDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(assignmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(UUID.randomUUID());

        // Create the Assignment
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(assignment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(assignmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(UUID.randomUUID());

        // Create the Assignment
        AssignmentDTO assignmentDTO = assignmentMapper.toDto(assignment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(assignmentDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAssignment() throws Exception {
        // Initialize the database
        insertedAssignment = assignmentRepository.saveAndFlush(assignment);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the assignment
        restAssignmentMockMvc
            .perform(delete(ENTITY_API_URL_ID, assignment.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return assignmentRepository.count();
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

    protected Assignment getPersistedAssignment(Assignment assignment) {
        return assignmentRepository.findById(assignment.getId()).orElseThrow();
    }

    protected void assertPersistedAssignmentToMatchAllProperties(Assignment expectedAssignment) {
        assertAssignmentAllPropertiesEquals(expectedAssignment, getPersistedAssignment(expectedAssignment));
    }

    protected void assertPersistedAssignmentToMatchUpdatableProperties(Assignment expectedAssignment) {
        assertAssignmentAllUpdatablePropertiesEquals(expectedAssignment, getPersistedAssignment(expectedAssignment));
    }
}
