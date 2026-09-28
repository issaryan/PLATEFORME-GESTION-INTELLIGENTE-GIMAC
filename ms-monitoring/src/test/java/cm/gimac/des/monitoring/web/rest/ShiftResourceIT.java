package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.ShiftAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import cm.gimac.des.monitoring.repository.ShiftRepository;
import cm.gimac.des.monitoring.service.ShiftService;
import cm.gimac.des.monitoring.service.mapper.ShiftMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Integration tests for the {@link ShiftResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ShiftResourceIT {

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_SHIFT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_SHIFT_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_SHIFT_DATE = LocalDate.ofEpochDay(-1L);

    private static final ShiftSlot DEFAULT_SLOT = ShiftSlot.MATIN;
    private static final ShiftSlot UPDATED_SLOT = ShiftSlot.APRES_MIDI;

    private static final String DEFAULT_OPERATOR_IDS = "AAAAAAAAAA";
    private static final String UPDATED_OPERATOR_IDS = "BBBBBBBBBB";

    private static final UUID DEFAULT_SHIFT_LEAD_ID = UUID.randomUUID();
    private static final UUID UPDATED_SHIFT_LEAD_ID = UUID.randomUUID();

    private static final Instant DEFAULT_STARTS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTS_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_ENDS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ENDS_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/shifts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ShiftRepository shiftRepository;

    @Mock
    private ShiftRepository shiftRepositoryMock;

    @Autowired
    private ShiftMapper shiftMapper;

    @Mock
    private ShiftService shiftServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restShiftMockMvc;

    private Shift shift;

    private Shift insertedShift;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Shift createEntity(EntityManager em) {
        Shift shift = new Shift()
            .reference(DEFAULT_REFERENCE)
            .shiftDate(DEFAULT_SHIFT_DATE)
            .slot(DEFAULT_SLOT)
            .operatorIds(DEFAULT_OPERATOR_IDS)
            .shiftLeadId(DEFAULT_SHIFT_LEAD_ID)
            .startsAt(DEFAULT_STARTS_AT)
            .endsAt(DEFAULT_ENDS_AT);
        // Add required entity
        OnCallRoster onCallRoster;
        if (TestUtil.findAll(em, OnCallRoster.class).isEmpty()) {
            onCallRoster = OnCallRosterResourceIT.createEntity();
            em.persist(onCallRoster);
            em.flush();
        } else {
            onCallRoster = TestUtil.findAll(em, OnCallRoster.class).get(0);
        }
        shift.setRoster(onCallRoster);
        return shift;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Shift createUpdatedEntity(EntityManager em) {
        Shift updatedShift = new Shift()
            .reference(UPDATED_REFERENCE)
            .shiftDate(UPDATED_SHIFT_DATE)
            .slot(UPDATED_SLOT)
            .operatorIds(UPDATED_OPERATOR_IDS)
            .shiftLeadId(UPDATED_SHIFT_LEAD_ID)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT);
        // Add required entity
        OnCallRoster onCallRoster;
        if (TestUtil.findAll(em, OnCallRoster.class).isEmpty()) {
            onCallRoster = OnCallRosterResourceIT.createUpdatedEntity();
            em.persist(onCallRoster);
            em.flush();
        } else {
            onCallRoster = TestUtil.findAll(em, OnCallRoster.class).get(0);
        }
        updatedShift.setRoster(onCallRoster);
        return updatedShift;
    }

    @BeforeEach
    void initTest() {
        shift = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedShift != null) {
            shiftRepository.delete(insertedShift);
            insertedShift = null;
        }
    }

    @Test
    @Transactional
    void getAllShifts() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList
        restShiftMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(shift.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].shiftDate").value(hasItem(DEFAULT_SHIFT_DATE.toString())))
            .andExpect(jsonPath("$.[*].slot").value(hasItem(DEFAULT_SLOT.toString())))
            .andExpect(jsonPath("$.[*].operatorIds").value(hasItem(DEFAULT_OPERATOR_IDS)))
            .andExpect(jsonPath("$.[*].shiftLeadId").value(hasItem(DEFAULT_SHIFT_LEAD_ID.toString())))
            .andExpect(jsonPath("$.[*].startsAt").value(hasItem(DEFAULT_STARTS_AT.toString())))
            .andExpect(jsonPath("$.[*].endsAt").value(hasItem(DEFAULT_ENDS_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllShiftsWithEagerRelationshipsIsEnabled() throws Exception {
        when(shiftServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restShiftMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(shiftServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllShiftsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(shiftServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restShiftMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(shiftRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getShift() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get the shift
        restShiftMockMvc
            .perform(get(ENTITY_API_URL_ID, shift.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(shift.getId().toString()))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.shiftDate").value(DEFAULT_SHIFT_DATE.toString()))
            .andExpect(jsonPath("$.slot").value(DEFAULT_SLOT.toString()))
            .andExpect(jsonPath("$.operatorIds").value(DEFAULT_OPERATOR_IDS))
            .andExpect(jsonPath("$.shiftLeadId").value(DEFAULT_SHIFT_LEAD_ID.toString()))
            .andExpect(jsonPath("$.startsAt").value(DEFAULT_STARTS_AT.toString()))
            .andExpect(jsonPath("$.endsAt").value(DEFAULT_ENDS_AT.toString()));
    }

    @Test
    @Transactional
    void getShiftsByIdFiltering() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        UUID id = shift.getId();

        defaultShiftFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllShiftsByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where reference equals to
        defaultShiftFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftsByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where reference in
        defaultShiftFiltering("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE, "reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftsByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where reference is not null
        defaultShiftFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftsByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where reference contains
        defaultShiftFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftsByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where reference does not contain
        defaultShiftFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
    }

    @Test
    @Transactional
    void getAllShiftsByShiftDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where shiftDate equals to
        defaultShiftFiltering("shiftDate.equals=" + DEFAULT_SHIFT_DATE, "shiftDate.equals=" + UPDATED_SHIFT_DATE);
    }

    @Test
    @Transactional
    void getAllShiftsByShiftDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where shiftDate in
        defaultShiftFiltering("shiftDate.in=" + DEFAULT_SHIFT_DATE + "," + UPDATED_SHIFT_DATE, "shiftDate.in=" + UPDATED_SHIFT_DATE);
    }

    @Test
    @Transactional
    void getAllShiftsByShiftDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where shiftDate is not null
        defaultShiftFiltering("shiftDate.specified=true", "shiftDate.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftsByShiftDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where shiftDate is greater than or equal to
        defaultShiftFiltering("shiftDate.greaterThanOrEqual=" + DEFAULT_SHIFT_DATE, "shiftDate.greaterThanOrEqual=" + UPDATED_SHIFT_DATE);
    }

    @Test
    @Transactional
    void getAllShiftsByShiftDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where shiftDate is less than or equal to
        defaultShiftFiltering("shiftDate.lessThanOrEqual=" + DEFAULT_SHIFT_DATE, "shiftDate.lessThanOrEqual=" + SMALLER_SHIFT_DATE);
    }

    @Test
    @Transactional
    void getAllShiftsByShiftDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where shiftDate is less than
        defaultShiftFiltering("shiftDate.lessThan=" + UPDATED_SHIFT_DATE, "shiftDate.lessThan=" + DEFAULT_SHIFT_DATE);
    }

    @Test
    @Transactional
    void getAllShiftsByShiftDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where shiftDate is greater than
        defaultShiftFiltering("shiftDate.greaterThan=" + SMALLER_SHIFT_DATE, "shiftDate.greaterThan=" + DEFAULT_SHIFT_DATE);
    }

    @Test
    @Transactional
    void getAllShiftsBySlotIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where slot equals to
        defaultShiftFiltering("slot.equals=" + DEFAULT_SLOT, "slot.equals=" + UPDATED_SLOT);
    }

    @Test
    @Transactional
    void getAllShiftsBySlotIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where slot in
        defaultShiftFiltering("slot.in=" + DEFAULT_SLOT + "," + UPDATED_SLOT, "slot.in=" + UPDATED_SLOT);
    }

    @Test
    @Transactional
    void getAllShiftsBySlotIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where slot is not null
        defaultShiftFiltering("slot.specified=true", "slot.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftsByOperatorIdsIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where operatorIds equals to
        defaultShiftFiltering("operatorIds.equals=" + DEFAULT_OPERATOR_IDS, "operatorIds.equals=" + UPDATED_OPERATOR_IDS);
    }

    @Test
    @Transactional
    void getAllShiftsByOperatorIdsIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where operatorIds in
        defaultShiftFiltering(
            "operatorIds.in=" + DEFAULT_OPERATOR_IDS + "," + UPDATED_OPERATOR_IDS,
            "operatorIds.in=" + UPDATED_OPERATOR_IDS
        );
    }

    @Test
    @Transactional
    void getAllShiftsByOperatorIdsIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where operatorIds is not null
        defaultShiftFiltering("operatorIds.specified=true", "operatorIds.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftsByOperatorIdsContainsSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where operatorIds contains
        defaultShiftFiltering("operatorIds.contains=" + DEFAULT_OPERATOR_IDS, "operatorIds.contains=" + UPDATED_OPERATOR_IDS);
    }

    @Test
    @Transactional
    void getAllShiftsByOperatorIdsNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where operatorIds does not contain
        defaultShiftFiltering("operatorIds.doesNotContain=" + UPDATED_OPERATOR_IDS, "operatorIds.doesNotContain=" + DEFAULT_OPERATOR_IDS);
    }

    @Test
    @Transactional
    void getAllShiftsByShiftLeadIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where shiftLeadId equals to
        defaultShiftFiltering("shiftLeadId.equals=" + DEFAULT_SHIFT_LEAD_ID, "shiftLeadId.equals=" + UPDATED_SHIFT_LEAD_ID);
    }

    @Test
    @Transactional
    void getAllShiftsByShiftLeadIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where shiftLeadId in
        defaultShiftFiltering(
            "shiftLeadId.in=" + DEFAULT_SHIFT_LEAD_ID + "," + UPDATED_SHIFT_LEAD_ID,
            "shiftLeadId.in=" + UPDATED_SHIFT_LEAD_ID
        );
    }

    @Test
    @Transactional
    void getAllShiftsByShiftLeadIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where shiftLeadId is not null
        defaultShiftFiltering("shiftLeadId.specified=true", "shiftLeadId.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftsByStartsAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where startsAt equals to
        defaultShiftFiltering("startsAt.equals=" + DEFAULT_STARTS_AT, "startsAt.equals=" + UPDATED_STARTS_AT);
    }

    @Test
    @Transactional
    void getAllShiftsByStartsAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where startsAt in
        defaultShiftFiltering("startsAt.in=" + DEFAULT_STARTS_AT + "," + UPDATED_STARTS_AT, "startsAt.in=" + UPDATED_STARTS_AT);
    }

    @Test
    @Transactional
    void getAllShiftsByStartsAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where startsAt is not null
        defaultShiftFiltering("startsAt.specified=true", "startsAt.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftsByEndsAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where endsAt equals to
        defaultShiftFiltering("endsAt.equals=" + DEFAULT_ENDS_AT, "endsAt.equals=" + UPDATED_ENDS_AT);
    }

    @Test
    @Transactional
    void getAllShiftsByEndsAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where endsAt in
        defaultShiftFiltering("endsAt.in=" + DEFAULT_ENDS_AT + "," + UPDATED_ENDS_AT, "endsAt.in=" + UPDATED_ENDS_AT);
    }

    @Test
    @Transactional
    void getAllShiftsByEndsAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShift = shiftRepository.saveAndFlush(shift);

        // Get all the shiftList where endsAt is not null
        defaultShiftFiltering("endsAt.specified=true", "endsAt.specified=false");
    }

    @Test
    @Transactional
    void getAllShiftsByRosterIsEqualToSomething() throws Exception {
        OnCallRoster roster;
        if (TestUtil.findAll(em, OnCallRoster.class).isEmpty()) {
            shiftRepository.saveAndFlush(shift);
            roster = OnCallRosterResourceIT.createEntity();
        } else {
            roster = TestUtil.findAll(em, OnCallRoster.class).get(0);
        }
        em.persist(roster);
        em.flush();
        shift.setRoster(roster);
        shiftRepository.saveAndFlush(shift);
        UUID rosterId = roster.getId();
        // Get all the shiftList where roster equals to rosterId
        defaultShiftShouldBeFound("rosterId.equals=" + rosterId);

        // Get all the shiftList where roster equals to UUID.randomUUID()
        defaultShiftShouldNotBeFound("rosterId.equals=" + UUID.randomUUID());
    }

    private void defaultShiftFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultShiftShouldBeFound(shouldBeFound);
        defaultShiftShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultShiftShouldBeFound(String filter) throws Exception {
        restShiftMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(shift.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].shiftDate").value(hasItem(DEFAULT_SHIFT_DATE.toString())))
            .andExpect(jsonPath("$.[*].slot").value(hasItem(DEFAULT_SLOT.toString())))
            .andExpect(jsonPath("$.[*].operatorIds").value(hasItem(DEFAULT_OPERATOR_IDS)))
            .andExpect(jsonPath("$.[*].shiftLeadId").value(hasItem(DEFAULT_SHIFT_LEAD_ID.toString())))
            .andExpect(jsonPath("$.[*].startsAt").value(hasItem(DEFAULT_STARTS_AT.toString())))
            .andExpect(jsonPath("$.[*].endsAt").value(hasItem(DEFAULT_ENDS_AT.toString())));

        // Check, that the count call also returns 1
        restShiftMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultShiftShouldNotBeFound(String filter) throws Exception {
        restShiftMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restShiftMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingShift() throws Exception {
        // Get the shift
        restShiftMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return shiftRepository.count();
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

    protected Shift getPersistedShift(Shift shift) {
        return shiftRepository.findById(shift.getId()).orElseThrow();
    }

    protected void assertPersistedShiftToMatchAllProperties(Shift expectedShift) {
        assertShiftAllPropertiesEquals(expectedShift, getPersistedShift(expectedShift));
    }

    protected void assertPersistedShiftToMatchUpdatableProperties(Shift expectedShift) {
        assertShiftAllUpdatablePropertiesEquals(expectedShift, getPersistedShift(expectedShift));
    }
}
