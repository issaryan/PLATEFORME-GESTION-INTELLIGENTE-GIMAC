package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.security.AuthoritiesConstants;
import static cm.gimac.des.participant.domain.EscalationContactAsserts.*;
import static cm.gimac.des.participant.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.participant.IntegrationTest;
import cm.gimac.des.participant.domain.Correspondent;
import cm.gimac.des.participant.domain.EscalationContact;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.repository.EscalationContactRepository;
import cm.gimac.des.participant.service.EscalationContactService;
import cm.gimac.des.participant.service.dto.EscalationContactDTO;
import cm.gimac.des.participant.service.mapper.EscalationContactMapper;
import jakarta.persistence.EntityManager;
import java.time.Duration;
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
 * Integration tests for the {@link EscalationContactResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class EscalationContactResourceIT {

    private static final Integer DEFAULT_SEVERITY = 1;
    private static final Integer UPDATED_SEVERITY = 2;
    private static final Integer SMALLER_SEVERITY = 1 - 1;

    private static final Integer DEFAULT_ESCALATION_RANK = 1;
    private static final Integer UPDATED_ESCALATION_RANK = 2;
    private static final Integer SMALLER_ESCALATION_RANK = 1 - 1;

    private static final Duration DEFAULT_ACK_DELAY = Duration.ofHours(6);
    private static final Duration UPDATED_ACK_DELAY = Duration.ofHours(12);
    private static final Duration SMALLER_ACK_DELAY = Duration.ofHours(5);

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/escalation-contacts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EscalationContactRepository escalationContactRepository;

    @Mock
    private EscalationContactRepository escalationContactRepositoryMock;

    @Autowired
    private EscalationContactMapper escalationContactMapper;

    @Mock
    private EscalationContactService escalationContactServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEscalationContactMockMvc;

    private EscalationContact escalationContact;

    private EscalationContact insertedEscalationContact;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EscalationContact createEntity(EntityManager em) {
        EscalationContact escalationContact = new EscalationContact()
            .severity(DEFAULT_SEVERITY)
            .escalationRank(DEFAULT_ESCALATION_RANK)
            .ackDelay(DEFAULT_ACK_DELAY)
            .active(DEFAULT_ACTIVE);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        escalationContact.setInstitution(institution);
        // Add required entity
        Correspondent correspondent;
        if (TestUtil.findAll(em, Correspondent.class).isEmpty()) {
            correspondent = CorrespondentResourceIT.createEntity(em);
            em.persist(correspondent);
            em.flush();
        } else {
            correspondent = TestUtil.findAll(em, Correspondent.class).getFirst();
        }
        escalationContact.setCorrespondent(correspondent);
        return escalationContact;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EscalationContact createUpdatedEntity(EntityManager em) {
        EscalationContact updatedEscalationContact = new EscalationContact()
            .severity(UPDATED_SEVERITY)
            .escalationRank(UPDATED_ESCALATION_RANK)
            .ackDelay(UPDATED_ACK_DELAY)
            .active(UPDATED_ACTIVE);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createUpdatedEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        updatedEscalationContact.setInstitution(institution);
        // Add required entity
        Correspondent correspondent;
        if (TestUtil.findAll(em, Correspondent.class).isEmpty()) {
            correspondent = CorrespondentResourceIT.createUpdatedEntity(em);
            em.persist(correspondent);
            em.flush();
        } else {
            correspondent = TestUtil.findAll(em, Correspondent.class).getFirst();
        }
        updatedEscalationContact.setCorrespondent(correspondent);
        return updatedEscalationContact;
    }

    @BeforeEach
    void initTest() {
        escalationContact = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedEscalationContact != null) {
            escalationContactRepository.delete(insertedEscalationContact);
            insertedEscalationContact = null;
        }
    }

    @Test
    @Transactional
    void createEscalationContact() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the EscalationContact
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);
        var returnedEscalationContactDTO = om.readValue(
            restEscalationContactMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(escalationContactDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            EscalationContactDTO.class
        );

        // Validate the EscalationContact in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedEscalationContact = escalationContactMapper.toEntity(returnedEscalationContactDTO);
        assertEscalationContactUpdatableFieldsEquals(returnedEscalationContact, getPersistedEscalationContact(returnedEscalationContact));

        insertedEscalationContact = returnedEscalationContact;
    }

    @Test
    @Transactional
    void createEscalationContactWithExistingId() throws Exception {
        // Create the EscalationContact with an existing ID
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restEscalationContactMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EscalationContact in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkSeverityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        escalationContact.setSeverity(null);

        // Create the EscalationContact, which fails.
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);

        restEscalationContactMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEscalationRankIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        escalationContact.setEscalationRank(null);

        // Create the EscalationContact, which fails.
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);

        restEscalationContactMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAckDelayIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        escalationContact.setAckDelay(null);

        // Create the EscalationContact, which fails.
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);

        restEscalationContactMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        escalationContact.setActive(null);

        // Create the EscalationContact, which fails.
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);

        restEscalationContactMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllEscalationContacts() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList
        restEscalationContactMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(escalationContact.getId().toString())))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY)))
            .andExpect(jsonPath("$.[*].escalationRank").value(hasItem(DEFAULT_ESCALATION_RANK)))
            .andExpect(jsonPath("$.[*].ackDelay").value(hasItem(DEFAULT_ACK_DELAY.toString())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEscalationContactsWithEagerRelationshipsIsEnabled() throws Exception {
        when(escalationContactServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEscalationContactMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(escalationContactServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEscalationContactsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(escalationContactServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEscalationContactMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(escalationContactRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getEscalationContact() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get the escalationContact
        restEscalationContactMockMvc
            .perform(get(ENTITY_API_URL_ID, escalationContact.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(escalationContact.getId().toString()))
            .andExpect(jsonPath("$.severity").value(DEFAULT_SEVERITY))
            .andExpect(jsonPath("$.escalationRank").value(DEFAULT_ESCALATION_RANK))
            .andExpect(jsonPath("$.ackDelay").value(DEFAULT_ACK_DELAY.toString()))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getEscalationContactsByIdFiltering() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        UUID id = escalationContact.getId();

        defaultEscalationContactFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllEscalationContactsBySeverityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where severity equals to
        defaultEscalationContactFiltering("severity.equals=" + DEFAULT_SEVERITY, "severity.equals=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllEscalationContactsBySeverityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where severity in
        defaultEscalationContactFiltering("severity.in=" + DEFAULT_SEVERITY + "," + UPDATED_SEVERITY, "severity.in=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllEscalationContactsBySeverityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where severity is not null
        defaultEscalationContactFiltering("severity.specified=true", "severity.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationContactsBySeverityIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where severity is greater than or equal to
        defaultEscalationContactFiltering(
            "severity.greaterThanOrEqual=" + DEFAULT_SEVERITY,
            "severity.greaterThanOrEqual=" + (DEFAULT_SEVERITY + 1)
        );
    }

    @Test
    @Transactional
    void getAllEscalationContactsBySeverityIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where severity is less than or equal to
        defaultEscalationContactFiltering("severity.lessThanOrEqual=" + DEFAULT_SEVERITY, "severity.lessThanOrEqual=" + SMALLER_SEVERITY);
    }

    @Test
    @Transactional
    void getAllEscalationContactsBySeverityIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where severity is less than
        defaultEscalationContactFiltering("severity.lessThan=" + (DEFAULT_SEVERITY + 1), "severity.lessThan=" + DEFAULT_SEVERITY);
    }

    @Test
    @Transactional
    void getAllEscalationContactsBySeverityIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where severity is greater than
        defaultEscalationContactFiltering("severity.greaterThan=" + SMALLER_SEVERITY, "severity.greaterThan=" + DEFAULT_SEVERITY);
    }

    @Test
    @Transactional
    void getAllEscalationContactsByEscalationRankIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where escalationRank equals to
        defaultEscalationContactFiltering(
            "escalationRank.equals=" + DEFAULT_ESCALATION_RANK,
            "escalationRank.equals=" + UPDATED_ESCALATION_RANK
        );
    }

    @Test
    @Transactional
    void getAllEscalationContactsByEscalationRankIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where escalationRank in
        defaultEscalationContactFiltering(
            "escalationRank.in=" + DEFAULT_ESCALATION_RANK + "," + UPDATED_ESCALATION_RANK,
            "escalationRank.in=" + UPDATED_ESCALATION_RANK
        );
    }

    @Test
    @Transactional
    void getAllEscalationContactsByEscalationRankIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where escalationRank is not null
        defaultEscalationContactFiltering("escalationRank.specified=true", "escalationRank.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationContactsByEscalationRankIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where escalationRank is greater than or equal to
        defaultEscalationContactFiltering(
            "escalationRank.greaterThanOrEqual=" + DEFAULT_ESCALATION_RANK,
            "escalationRank.greaterThanOrEqual=" + UPDATED_ESCALATION_RANK
        );
    }

    @Test
    @Transactional
    void getAllEscalationContactsByEscalationRankIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where escalationRank is less than or equal to
        defaultEscalationContactFiltering(
            "escalationRank.lessThanOrEqual=" + DEFAULT_ESCALATION_RANK,
            "escalationRank.lessThanOrEqual=" + SMALLER_ESCALATION_RANK
        );
    }

    @Test
    @Transactional
    void getAllEscalationContactsByEscalationRankIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where escalationRank is less than
        defaultEscalationContactFiltering(
            "escalationRank.lessThan=" + UPDATED_ESCALATION_RANK,
            "escalationRank.lessThan=" + DEFAULT_ESCALATION_RANK
        );
    }

    @Test
    @Transactional
    void getAllEscalationContactsByEscalationRankIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where escalationRank is greater than
        defaultEscalationContactFiltering(
            "escalationRank.greaterThan=" + SMALLER_ESCALATION_RANK,
            "escalationRank.greaterThan=" + DEFAULT_ESCALATION_RANK
        );
    }

    @Test
    @Transactional
    void getAllEscalationContactsByAckDelayIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where ackDelay equals to
        defaultEscalationContactFiltering("ackDelay.equals=" + DEFAULT_ACK_DELAY, "ackDelay.equals=" + UPDATED_ACK_DELAY);
    }

    @Test
    @Transactional
    void getAllEscalationContactsByAckDelayIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where ackDelay in
        defaultEscalationContactFiltering("ackDelay.in=" + DEFAULT_ACK_DELAY + "," + UPDATED_ACK_DELAY, "ackDelay.in=" + UPDATED_ACK_DELAY);
    }

    @Test
    @Transactional
    void getAllEscalationContactsByAckDelayIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where ackDelay is not null
        defaultEscalationContactFiltering("ackDelay.specified=true", "ackDelay.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationContactsByAckDelayIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where ackDelay is greater than or equal to
        defaultEscalationContactFiltering(
            "ackDelay.greaterThanOrEqual=" + DEFAULT_ACK_DELAY,
            "ackDelay.greaterThanOrEqual=" + UPDATED_ACK_DELAY
        );
    }

    @Test
    @Transactional
    void getAllEscalationContactsByAckDelayIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where ackDelay is less than or equal to
        defaultEscalationContactFiltering("ackDelay.lessThanOrEqual=" + DEFAULT_ACK_DELAY, "ackDelay.lessThanOrEqual=" + SMALLER_ACK_DELAY);
    }

    @Test
    @Transactional
    void getAllEscalationContactsByAckDelayIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where ackDelay is less than
        defaultEscalationContactFiltering("ackDelay.lessThan=" + UPDATED_ACK_DELAY, "ackDelay.lessThan=" + DEFAULT_ACK_DELAY);
    }

    @Test
    @Transactional
    void getAllEscalationContactsByAckDelayIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where ackDelay is greater than
        defaultEscalationContactFiltering("ackDelay.greaterThan=" + SMALLER_ACK_DELAY, "ackDelay.greaterThan=" + DEFAULT_ACK_DELAY);
    }

    @Test
    @Transactional
    void getAllEscalationContactsByActiveIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where active equals to
        defaultEscalationContactFiltering("active.equals=" + DEFAULT_ACTIVE, "active.equals=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllEscalationContactsByActiveIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where active in
        defaultEscalationContactFiltering("active.in=" + DEFAULT_ACTIVE + "," + UPDATED_ACTIVE, "active.in=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllEscalationContactsByActiveIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        // Get all the escalationContactList where active is not null
        defaultEscalationContactFiltering("active.specified=true", "active.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationContactsByInstitutionIsEqualToSomething() throws Exception {
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            escalationContactRepository.saveAndFlush(escalationContact);
            institution = InstitutionResourceIT.createEntity();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        em.persist(institution);
        em.flush();
        escalationContact.setInstitution(institution);
        escalationContactRepository.saveAndFlush(escalationContact);
        UUID institutionId = institution.getId();
        // Get all the escalationContactList where institution equals to institutionId
        defaultEscalationContactShouldBeFound("institutionId.equals=" + institutionId);

        // Get all the escalationContactList where institution equals to UUID.randomUUID()
        defaultEscalationContactShouldNotBeFound("institutionId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllEscalationContactsByCorrespondentIsEqualToSomething() throws Exception {
        Correspondent correspondent;
        if (TestUtil.findAll(em, Correspondent.class).isEmpty()) {
            escalationContactRepository.saveAndFlush(escalationContact);
            correspondent = CorrespondentResourceIT.createEntity(em);
        } else {
            correspondent = TestUtil.findAll(em, Correspondent.class).getFirst();
        }
        em.persist(correspondent);
        em.flush();
        escalationContact.setCorrespondent(correspondent);
        escalationContactRepository.saveAndFlush(escalationContact);
        UUID correspondentId = correspondent.getId();
        // Get all the escalationContactList where correspondent equals to correspondentId
        defaultEscalationContactShouldBeFound("correspondentId.equals=" + correspondentId);

        // Get all the escalationContactList where correspondent equals to UUID.randomUUID()
        defaultEscalationContactShouldNotBeFound("correspondentId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllEscalationContactsByDeputyIsEqualToSomething() throws Exception {
        Correspondent deputy;
        if (TestUtil.findAll(em, Correspondent.class).isEmpty()) {
            escalationContactRepository.saveAndFlush(escalationContact);
            deputy = CorrespondentResourceIT.createEntity(em);
        } else {
            deputy = TestUtil.findAll(em, Correspondent.class).getFirst();
        }
        em.persist(deputy);
        em.flush();
        escalationContact.setDeputy(deputy);
        escalationContactRepository.saveAndFlush(escalationContact);
        UUID deputyId = deputy.getId();
        // Get all the escalationContactList where deputy equals to deputyId
        defaultEscalationContactShouldBeFound("deputyId.equals=" + deputyId);

        // Get all the escalationContactList where deputy equals to UUID.randomUUID()
        defaultEscalationContactShouldNotBeFound("deputyId.equals=" + UUID.randomUUID());
    }

    private void defaultEscalationContactFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultEscalationContactShouldBeFound(shouldBeFound);
        defaultEscalationContactShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultEscalationContactShouldBeFound(String filter) throws Exception {
        restEscalationContactMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(escalationContact.getId().toString())))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY)))
            .andExpect(jsonPath("$.[*].escalationRank").value(hasItem(DEFAULT_ESCALATION_RANK)))
            .andExpect(jsonPath("$.[*].ackDelay").value(hasItem(DEFAULT_ACK_DELAY.toString())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));

        // Check, that the count call also returns 1
        restEscalationContactMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultEscalationContactShouldNotBeFound(String filter) throws Exception {
        restEscalationContactMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restEscalationContactMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingEscalationContact() throws Exception {
        // Get the escalationContact
        restEscalationContactMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingEscalationContact() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the escalationContact
        EscalationContact updatedEscalationContact = escalationContactRepository.findById(escalationContact.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedEscalationContact are not directly saved in db
        em.detach(updatedEscalationContact);
        updatedEscalationContact
            .severity(UPDATED_SEVERITY)
            .escalationRank(UPDATED_ESCALATION_RANK)
            .ackDelay(UPDATED_ACK_DELAY)
            .active(UPDATED_ACTIVE);
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(updatedEscalationContact);

        restEscalationContactMockMvc
            .perform(
                put(ENTITY_API_URL_ID, escalationContactDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isOk());

        // Validate the EscalationContact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedEscalationContactToMatchAllProperties(updatedEscalationContact);
    }

    @Test
    @Transactional
    void putNonExistingEscalationContact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        escalationContact.setId(UUID.randomUUID());

        // Create the EscalationContact
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEscalationContactMockMvc
            .perform(
                put(ENTITY_API_URL_ID, escalationContactDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EscalationContact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchEscalationContact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        escalationContact.setId(UUID.randomUUID());

        // Create the EscalationContact
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEscalationContactMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EscalationContact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamEscalationContact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        escalationContact.setId(UUID.randomUUID());

        // Create the EscalationContact
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEscalationContactMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the EscalationContact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateEscalationContactWithPatch() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the escalationContact using partial update
        EscalationContact partialUpdatedEscalationContact = new EscalationContact();
        partialUpdatedEscalationContact.setId(escalationContact.getId());

        partialUpdatedEscalationContact.escalationRank(UPDATED_ESCALATION_RANK);

        restEscalationContactMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEscalationContact.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEscalationContact))
            )
            .andExpect(status().isOk());

        // Validate the EscalationContact in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEscalationContactUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedEscalationContact, escalationContact),
            getPersistedEscalationContact(escalationContact)
        );
    }

    @Test
    @Transactional
    void fullUpdateEscalationContactWithPatch() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the escalationContact using partial update
        EscalationContact partialUpdatedEscalationContact = new EscalationContact();
        partialUpdatedEscalationContact.setId(escalationContact.getId());

        partialUpdatedEscalationContact
            .severity(UPDATED_SEVERITY)
            .escalationRank(UPDATED_ESCALATION_RANK)
            .ackDelay(UPDATED_ACK_DELAY)
            .active(UPDATED_ACTIVE);

        restEscalationContactMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEscalationContact.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEscalationContact))
            )
            .andExpect(status().isOk());

        // Validate the EscalationContact in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEscalationContactUpdatableFieldsEquals(
            partialUpdatedEscalationContact,
            getPersistedEscalationContact(partialUpdatedEscalationContact)
        );
    }

    @Test
    @Transactional
    void patchNonExistingEscalationContact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        escalationContact.setId(UUID.randomUUID());

        // Create the EscalationContact
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEscalationContactMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, escalationContactDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EscalationContact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchEscalationContact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        escalationContact.setId(UUID.randomUUID());

        // Create the EscalationContact
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEscalationContactMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EscalationContact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamEscalationContact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        escalationContact.setId(UUID.randomUUID());

        // Create the EscalationContact
        EscalationContactDTO escalationContactDTO = escalationContactMapper.toDto(escalationContact);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEscalationContactMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(escalationContactDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the EscalationContact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteEscalationContact() throws Exception {
        // Initialize the database
        insertedEscalationContact = escalationContactRepository.saveAndFlush(escalationContact);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the escalationContact
        restEscalationContactMockMvc
            .perform(delete(ENTITY_API_URL_ID, escalationContact.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return escalationContactRepository.count();
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

    protected EscalationContact getPersistedEscalationContact(EscalationContact escalationContact) {
        return escalationContactRepository.findById(escalationContact.getId()).orElseThrow();
    }

    protected void assertPersistedEscalationContactToMatchAllProperties(EscalationContact expectedEscalationContact) {
        assertEscalationContactAllPropertiesEquals(expectedEscalationContact, getPersistedEscalationContact(expectedEscalationContact));
    }

    protected void assertPersistedEscalationContactToMatchUpdatableProperties(EscalationContact expectedEscalationContact) {
        assertEscalationContactAllUpdatablePropertiesEquals(
            expectedEscalationContact,
            getPersistedEscalationContact(expectedEscalationContact)
        );
    }
}
