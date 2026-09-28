package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.OnCallAssignmentAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.OnCallAssignment;
import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import cm.gimac.des.monitoring.repository.OnCallAssignmentRepository;
import cm.gimac.des.monitoring.service.OnCallAssignmentService;
import cm.gimac.des.monitoring.service.mapper.OnCallAssignmentMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Integration tests for the {@link OnCallAssignmentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class OnCallAssignmentResourceIT {

    private static final LocalDate DEFAULT_DUTY_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DUTY_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_DUTY_DATE = LocalDate.ofEpochDay(-1L);

    private static final ShiftSlot DEFAULT_SLOT = ShiftSlot.MATIN;
    private static final ShiftSlot UPDATED_SLOT = ShiftSlot.APRES_MIDI;

    private static final Integer DEFAULT_LEVEL = 1;
    private static final Integer UPDATED_LEVEL = 2;
    private static final Integer SMALLER_LEVEL = 1 - 1;

    private static final UUID DEFAULT_OPERATOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_OPERATOR_ID = UUID.randomUUID();

    private static final Boolean DEFAULT_SHIFT_LEAD = false;
    private static final Boolean UPDATED_SHIFT_LEAD = true;

    private static final String ENTITY_API_URL = "/api/on-call-assignments";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OnCallAssignmentRepository onCallAssignmentRepository;

    @Mock
    private OnCallAssignmentRepository onCallAssignmentRepositoryMock;

    @Autowired
    private OnCallAssignmentMapper onCallAssignmentMapper;

    @Mock
    private OnCallAssignmentService onCallAssignmentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOnCallAssignmentMockMvc;

    private OnCallAssignment onCallAssignment;

    private OnCallAssignment insertedOnCallAssignment;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OnCallAssignment createEntity(EntityManager em) {
        OnCallAssignment onCallAssignment = new OnCallAssignment()
            .dutyDate(DEFAULT_DUTY_DATE)
            .slot(DEFAULT_SLOT)
            .level(DEFAULT_LEVEL)
            .operatorId(DEFAULT_OPERATOR_ID)
            .shiftLead(DEFAULT_SHIFT_LEAD);
        // Add required entity
        OnCallRoster onCallRoster;
        if (TestUtil.findAll(em, OnCallRoster.class).isEmpty()) {
            onCallRoster = OnCallRosterResourceIT.createEntity();
            em.persist(onCallRoster);
            em.flush();
        } else {
            onCallRoster = TestUtil.findAll(em, OnCallRoster.class).get(0);
        }
        onCallAssignment.setRoster(onCallRoster);
        return onCallAssignment;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OnCallAssignment createUpdatedEntity(EntityManager em) {
        OnCallAssignment updatedOnCallAssignment = new OnCallAssignment()
            .dutyDate(UPDATED_DUTY_DATE)
            .slot(UPDATED_SLOT)
            .level(UPDATED_LEVEL)
            .operatorId(UPDATED_OPERATOR_ID)
            .shiftLead(UPDATED_SHIFT_LEAD);
        // Add required entity
        OnCallRoster onCallRoster;
        if (TestUtil.findAll(em, OnCallRoster.class).isEmpty()) {
            onCallRoster = OnCallRosterResourceIT.createUpdatedEntity();
            em.persist(onCallRoster);
            em.flush();
        } else {
            onCallRoster = TestUtil.findAll(em, OnCallRoster.class).get(0);
        }
        updatedOnCallAssignment.setRoster(onCallRoster);
        return updatedOnCallAssignment;
    }

    @BeforeEach
    void initTest() {
        onCallAssignment = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedOnCallAssignment != null) {
            onCallAssignmentRepository.delete(insertedOnCallAssignment);
            insertedOnCallAssignment = null;
        }
    }

    @Test
    @Transactional
    void getAllOnCallAssignments() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList
        restOnCallAssignmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(onCallAssignment.getId().toString())))
            .andExpect(jsonPath("$.[*].dutyDate").value(hasItem(DEFAULT_DUTY_DATE.toString())))
            .andExpect(jsonPath("$.[*].slot").value(hasItem(DEFAULT_SLOT.toString())))
            .andExpect(jsonPath("$.[*].level").value(hasItem(DEFAULT_LEVEL)))
            .andExpect(jsonPath("$.[*].operatorId").value(hasItem(DEFAULT_OPERATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].shiftLead").value(hasItem(DEFAULT_SHIFT_LEAD)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOnCallAssignmentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(onCallAssignmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOnCallAssignmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(onCallAssignmentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOnCallAssignmentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(onCallAssignmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOnCallAssignmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(onCallAssignmentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOnCallAssignment() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get the onCallAssignment
        restOnCallAssignmentMockMvc
            .perform(get(ENTITY_API_URL_ID, onCallAssignment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(onCallAssignment.getId().toString()))
            .andExpect(jsonPath("$.dutyDate").value(DEFAULT_DUTY_DATE.toString()))
            .andExpect(jsonPath("$.slot").value(DEFAULT_SLOT.toString()))
            .andExpect(jsonPath("$.level").value(DEFAULT_LEVEL))
            .andExpect(jsonPath("$.operatorId").value(DEFAULT_OPERATOR_ID.toString()))
            .andExpect(jsonPath("$.shiftLead").value(DEFAULT_SHIFT_LEAD));
    }

    @Test
    @Transactional
    void getOnCallAssignmentsByIdFiltering() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        UUID id = onCallAssignment.getId();

        defaultOnCallAssignmentFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByDutyDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where dutyDate equals to
        defaultOnCallAssignmentFiltering("dutyDate.equals=" + DEFAULT_DUTY_DATE, "dutyDate.equals=" + UPDATED_DUTY_DATE);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByDutyDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where dutyDate in
        defaultOnCallAssignmentFiltering("dutyDate.in=" + DEFAULT_DUTY_DATE + "," + UPDATED_DUTY_DATE, "dutyDate.in=" + UPDATED_DUTY_DATE);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByDutyDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where dutyDate is not null
        defaultOnCallAssignmentFiltering("dutyDate.specified=true", "dutyDate.specified=false");
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByDutyDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where dutyDate is greater than or equal to
        defaultOnCallAssignmentFiltering(
            "dutyDate.greaterThanOrEqual=" + DEFAULT_DUTY_DATE,
            "dutyDate.greaterThanOrEqual=" + UPDATED_DUTY_DATE
        );
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByDutyDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where dutyDate is less than or equal to
        defaultOnCallAssignmentFiltering("dutyDate.lessThanOrEqual=" + DEFAULT_DUTY_DATE, "dutyDate.lessThanOrEqual=" + SMALLER_DUTY_DATE);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByDutyDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where dutyDate is less than
        defaultOnCallAssignmentFiltering("dutyDate.lessThan=" + UPDATED_DUTY_DATE, "dutyDate.lessThan=" + DEFAULT_DUTY_DATE);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByDutyDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where dutyDate is greater than
        defaultOnCallAssignmentFiltering("dutyDate.greaterThan=" + SMALLER_DUTY_DATE, "dutyDate.greaterThan=" + DEFAULT_DUTY_DATE);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsBySlotIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where slot equals to
        defaultOnCallAssignmentFiltering("slot.equals=" + DEFAULT_SLOT, "slot.equals=" + UPDATED_SLOT);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsBySlotIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where slot in
        defaultOnCallAssignmentFiltering("slot.in=" + DEFAULT_SLOT + "," + UPDATED_SLOT, "slot.in=" + UPDATED_SLOT);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsBySlotIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where slot is not null
        defaultOnCallAssignmentFiltering("slot.specified=true", "slot.specified=false");
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByLevelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where level equals to
        defaultOnCallAssignmentFiltering("level.equals=" + DEFAULT_LEVEL, "level.equals=" + UPDATED_LEVEL);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByLevelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where level in
        defaultOnCallAssignmentFiltering("level.in=" + DEFAULT_LEVEL + "," + UPDATED_LEVEL, "level.in=" + UPDATED_LEVEL);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByLevelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where level is not null
        defaultOnCallAssignmentFiltering("level.specified=true", "level.specified=false");
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByLevelIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where level is greater than or equal to
        defaultOnCallAssignmentFiltering("level.greaterThanOrEqual=" + DEFAULT_LEVEL, "level.greaterThanOrEqual=" + (DEFAULT_LEVEL + 1));
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByLevelIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where level is less than or equal to
        defaultOnCallAssignmentFiltering("level.lessThanOrEqual=" + DEFAULT_LEVEL, "level.lessThanOrEqual=" + SMALLER_LEVEL);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByLevelIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where level is less than
        defaultOnCallAssignmentFiltering("level.lessThan=" + (DEFAULT_LEVEL + 1), "level.lessThan=" + DEFAULT_LEVEL);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByLevelIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where level is greater than
        defaultOnCallAssignmentFiltering("level.greaterThan=" + SMALLER_LEVEL, "level.greaterThan=" + DEFAULT_LEVEL);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByOperatorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where operatorId equals to
        defaultOnCallAssignmentFiltering("operatorId.equals=" + DEFAULT_OPERATOR_ID, "operatorId.equals=" + UPDATED_OPERATOR_ID);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByOperatorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where operatorId in
        defaultOnCallAssignmentFiltering(
            "operatorId.in=" + DEFAULT_OPERATOR_ID + "," + UPDATED_OPERATOR_ID,
            "operatorId.in=" + UPDATED_OPERATOR_ID
        );
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByOperatorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where operatorId is not null
        defaultOnCallAssignmentFiltering("operatorId.specified=true", "operatorId.specified=false");
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByShiftLeadIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where shiftLead equals to
        defaultOnCallAssignmentFiltering("shiftLead.equals=" + DEFAULT_SHIFT_LEAD, "shiftLead.equals=" + UPDATED_SHIFT_LEAD);
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByShiftLeadIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where shiftLead in
        defaultOnCallAssignmentFiltering(
            "shiftLead.in=" + DEFAULT_SHIFT_LEAD + "," + UPDATED_SHIFT_LEAD,
            "shiftLead.in=" + UPDATED_SHIFT_LEAD
        );
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByShiftLeadIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOnCallAssignment = onCallAssignmentRepository.saveAndFlush(onCallAssignment);

        // Get all the onCallAssignmentList where shiftLead is not null
        defaultOnCallAssignmentFiltering("shiftLead.specified=true", "shiftLead.specified=false");
    }

    @Test
    @Transactional
    void getAllOnCallAssignmentsByRosterIsEqualToSomething() throws Exception {
        OnCallRoster roster;
        if (TestUtil.findAll(em, OnCallRoster.class).isEmpty()) {
            onCallAssignmentRepository.saveAndFlush(onCallAssignment);
            roster = OnCallRosterResourceIT.createEntity();
        } else {
            roster = TestUtil.findAll(em, OnCallRoster.class).get(0);
        }
        em.persist(roster);
        em.flush();
        onCallAssignment.setRoster(roster);
        onCallAssignmentRepository.saveAndFlush(onCallAssignment);
        UUID rosterId = roster.getId();
        // Get all the onCallAssignmentList where roster equals to rosterId
        defaultOnCallAssignmentShouldBeFound("rosterId.equals=" + rosterId);

        // Get all the onCallAssignmentList where roster equals to UUID.randomUUID()
        defaultOnCallAssignmentShouldNotBeFound("rosterId.equals=" + UUID.randomUUID());
    }

    private void defaultOnCallAssignmentFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultOnCallAssignmentShouldBeFound(shouldBeFound);
        defaultOnCallAssignmentShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultOnCallAssignmentShouldBeFound(String filter) throws Exception {
        restOnCallAssignmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(onCallAssignment.getId().toString())))
            .andExpect(jsonPath("$.[*].dutyDate").value(hasItem(DEFAULT_DUTY_DATE.toString())))
            .andExpect(jsonPath("$.[*].slot").value(hasItem(DEFAULT_SLOT.toString())))
            .andExpect(jsonPath("$.[*].level").value(hasItem(DEFAULT_LEVEL)))
            .andExpect(jsonPath("$.[*].operatorId").value(hasItem(DEFAULT_OPERATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].shiftLead").value(hasItem(DEFAULT_SHIFT_LEAD)));

        // Check, that the count call also returns 1
        restOnCallAssignmentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultOnCallAssignmentShouldNotBeFound(String filter) throws Exception {
        restOnCallAssignmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restOnCallAssignmentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingOnCallAssignment() throws Exception {
        // Get the onCallAssignment
        restOnCallAssignmentMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return onCallAssignmentRepository.count();
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

    protected OnCallAssignment getPersistedOnCallAssignment(OnCallAssignment onCallAssignment) {
        return onCallAssignmentRepository.findById(onCallAssignment.getId()).orElseThrow();
    }

    protected void assertPersistedOnCallAssignmentToMatchAllProperties(OnCallAssignment expectedOnCallAssignment) {
        assertOnCallAssignmentAllPropertiesEquals(expectedOnCallAssignment, getPersistedOnCallAssignment(expectedOnCallAssignment));
    }

    protected void assertPersistedOnCallAssignmentToMatchUpdatableProperties(OnCallAssignment expectedOnCallAssignment) {
        assertOnCallAssignmentAllUpdatablePropertiesEquals(
            expectedOnCallAssignment,
            getPersistedOnCallAssignment(expectedOnCallAssignment)
        );
    }
}
