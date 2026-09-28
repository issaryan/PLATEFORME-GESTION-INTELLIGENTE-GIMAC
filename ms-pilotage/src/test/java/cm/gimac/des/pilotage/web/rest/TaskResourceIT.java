package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.TaskAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.ActionSheet;
import cm.gimac.des.pilotage.domain.Board;
import cm.gimac.des.pilotage.domain.BoardColumn;
import cm.gimac.des.pilotage.domain.Task;
import cm.gimac.des.pilotage.domain.enumeration.OriginKind;
import cm.gimac.des.pilotage.domain.enumeration.TaskState;
import cm.gimac.des.pilotage.repository.TaskRepository;
import cm.gimac.des.pilotage.service.TaskService;
import cm.gimac.des.pilotage.service.dto.TaskDTO;
import cm.gimac.des.pilotage.service.mapper.TaskMapper;
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
 * Integration tests for the {@link TaskResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class TaskResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final UUID DEFAULT_ASSIGNEE_ID = UUID.randomUUID();
    private static final UUID UPDATED_ASSIGNEE_ID = UUID.randomUUID();

    private static final LocalDate DEFAULT_DUE_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DUE_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_DUE_DATE = LocalDate.ofEpochDay(-1L);

    private static final OriginKind DEFAULT_ORIGIN_KIND = OriginKind.TICKET;
    private static final OriginKind UPDATED_ORIGIN_KIND = OriginKind.OPERATION;

    private static final String DEFAULT_ORIGIN_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_ORIGIN_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_ORIGIN_SERVICE = "AAAAAAAAAA";
    private static final String UPDATED_ORIGIN_SERVICE = "BBBBBBBBBB";

    private static final Instant DEFAULT_STARTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_COMPLETED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_COMPLETED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final TaskState DEFAULT_STATE = TaskState.OUVERTE;
    private static final TaskState UPDATED_STATE = TaskState.EN_COURS;

    private static final Integer DEFAULT_REMINDER_LEVEL = 0;
    private static final Integer UPDATED_REMINDER_LEVEL = 1;
    private static final Integer SMALLER_REMINDER_LEVEL = 0 - 1;

    private static final Instant DEFAULT_CLOSURE_PROPOSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLOSURE_PROPOSED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/tasks";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TaskRepository taskRepository;

    @Mock
    private TaskRepository taskRepositoryMock;

    @Autowired
    private TaskMapper taskMapper;

    @Mock
    private TaskService taskServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTaskMockMvc;

    private Task task;

    private Task insertedTask;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Task createEntity(EntityManager em) {
        Task task = new Task()
            .title(DEFAULT_TITLE)
            .assigneeId(DEFAULT_ASSIGNEE_ID)
            .dueDate(DEFAULT_DUE_DATE)
            .originKind(DEFAULT_ORIGIN_KIND)
            .originReference(DEFAULT_ORIGIN_REFERENCE)
            .originService(DEFAULT_ORIGIN_SERVICE)
            .startedAt(DEFAULT_STARTED_AT)
            .completedAt(DEFAULT_COMPLETED_AT)
            .state(DEFAULT_STATE)
            .reminderLevel(DEFAULT_REMINDER_LEVEL)
            .closureProposedAt(DEFAULT_CLOSURE_PROPOSED_AT);
        // Add required entity
        Board board;
        if (TestUtil.findAll(em, Board.class).isEmpty()) {
            board = BoardResourceIT.createEntity();
            em.persist(board);
            em.flush();
        } else {
            board = TestUtil.findAll(em, Board.class).get(0);
        }
        task.setBoard(board);
        // Add required entity
        BoardColumn boardColumn;
        if (TestUtil.findAll(em, BoardColumn.class).isEmpty()) {
            boardColumn = BoardColumnResourceIT.createEntity(em);
            em.persist(boardColumn);
            em.flush();
        } else {
            boardColumn = TestUtil.findAll(em, BoardColumn.class).get(0);
        }
        task.setBoardColumn(boardColumn);
        return task;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Task createUpdatedEntity(EntityManager em) {
        Task updatedTask = new Task()
            .title(UPDATED_TITLE)
            .assigneeId(UPDATED_ASSIGNEE_ID)
            .dueDate(UPDATED_DUE_DATE)
            .originKind(UPDATED_ORIGIN_KIND)
            .originReference(UPDATED_ORIGIN_REFERENCE)
            .originService(UPDATED_ORIGIN_SERVICE)
            .startedAt(UPDATED_STARTED_AT)
            .completedAt(UPDATED_COMPLETED_AT)
            .state(UPDATED_STATE)
            .reminderLevel(UPDATED_REMINDER_LEVEL)
            .closureProposedAt(UPDATED_CLOSURE_PROPOSED_AT);
        // Add required entity
        Board board;
        if (TestUtil.findAll(em, Board.class).isEmpty()) {
            board = BoardResourceIT.createUpdatedEntity();
            em.persist(board);
            em.flush();
        } else {
            board = TestUtil.findAll(em, Board.class).get(0);
        }
        updatedTask.setBoard(board);
        // Add required entity
        BoardColumn boardColumn;
        if (TestUtil.findAll(em, BoardColumn.class).isEmpty()) {
            boardColumn = BoardColumnResourceIT.createUpdatedEntity(em);
            em.persist(boardColumn);
            em.flush();
        } else {
            boardColumn = TestUtil.findAll(em, BoardColumn.class).get(0);
        }
        updatedTask.setBoardColumn(boardColumn);
        return updatedTask;
    }

    @BeforeEach
    void initTest() {
        task = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedTask != null) {
            taskRepository.delete(insertedTask);
            insertedTask = null;
        }
    }

    @Test
    @Transactional
    void createTask() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Task
        TaskDTO taskDTO = taskMapper.toDto(task);
        var returnedTaskDTO = om.readValue(
            restTaskMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(taskDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            TaskDTO.class
        );

        // Validate the Task in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedTask = taskMapper.toEntity(returnedTaskDTO);
        assertTaskUpdatableFieldsEquals(returnedTask, getPersistedTask(returnedTask));

        insertedTask = returnedTask;
    }

    @Test
    @Transactional
    void createTaskWithExistingId() throws Exception {
        // Create the Task with an existing ID
        insertedTask = taskRepository.saveAndFlush(task);
        TaskDTO taskDTO = taskMapper.toDto(task);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restTaskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Task in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        task.setTitle(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.toDto(task);

        restTaskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAssigneeIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        task.setAssigneeId(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.toDto(task);

        restTaskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDueDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        task.setDueDate(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.toDto(task);

        restTaskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOriginKindIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        task.setOriginKind(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.toDto(task);

        restTaskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        task.setState(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.toDto(task);

        restTaskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReminderLevelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        task.setReminderLevel(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.toDto(task);

        restTaskMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllTasks() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(task.getId().toString())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].assigneeId").value(hasItem(DEFAULT_ASSIGNEE_ID.toString())))
            .andExpect(jsonPath("$.[*].dueDate").value(hasItem(DEFAULT_DUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].originKind").value(hasItem(DEFAULT_ORIGIN_KIND.toString())))
            .andExpect(jsonPath("$.[*].originReference").value(hasItem(DEFAULT_ORIGIN_REFERENCE)))
            .andExpect(jsonPath("$.[*].originService").value(hasItem(DEFAULT_ORIGIN_SERVICE)))
            .andExpect(jsonPath("$.[*].startedAt").value(hasItem(DEFAULT_STARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].completedAt").value(hasItem(DEFAULT_COMPLETED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].reminderLevel").value(hasItem(DEFAULT_REMINDER_LEVEL)))
            .andExpect(jsonPath("$.[*].closureProposedAt").value(hasItem(DEFAULT_CLOSURE_PROPOSED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTasksWithEagerRelationshipsIsEnabled() throws Exception {
        when(taskServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTaskMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(taskServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTasksWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(taskServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTaskMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(taskRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getTask() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get the task
        restTaskMockMvc
            .perform(get(ENTITY_API_URL_ID, task.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(task.getId().toString()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.assigneeId").value(DEFAULT_ASSIGNEE_ID.toString()))
            .andExpect(jsonPath("$.dueDate").value(DEFAULT_DUE_DATE.toString()))
            .andExpect(jsonPath("$.originKind").value(DEFAULT_ORIGIN_KIND.toString()))
            .andExpect(jsonPath("$.originReference").value(DEFAULT_ORIGIN_REFERENCE))
            .andExpect(jsonPath("$.originService").value(DEFAULT_ORIGIN_SERVICE))
            .andExpect(jsonPath("$.startedAt").value(DEFAULT_STARTED_AT.toString()))
            .andExpect(jsonPath("$.completedAt").value(DEFAULT_COMPLETED_AT.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.reminderLevel").value(DEFAULT_REMINDER_LEVEL))
            .andExpect(jsonPath("$.closureProposedAt").value(DEFAULT_CLOSURE_PROPOSED_AT.toString()));
    }

    @Test
    @Transactional
    void getTasksByIdFiltering() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        UUID id = task.getId();

        defaultTaskFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllTasksByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where title equals to
        defaultTaskFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllTasksByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where title in
        defaultTaskFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllTasksByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where title is not null
        defaultTaskFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllTasksByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where title contains
        defaultTaskFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllTasksByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where title does not contain
        defaultTaskFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllTasksByAssigneeIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where assigneeId equals to
        defaultTaskFiltering("assigneeId.equals=" + DEFAULT_ASSIGNEE_ID, "assigneeId.equals=" + UPDATED_ASSIGNEE_ID);
    }

    @Test
    @Transactional
    void getAllTasksByAssigneeIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where assigneeId in
        defaultTaskFiltering("assigneeId.in=" + DEFAULT_ASSIGNEE_ID + "," + UPDATED_ASSIGNEE_ID, "assigneeId.in=" + UPDATED_ASSIGNEE_ID);
    }

    @Test
    @Transactional
    void getAllTasksByAssigneeIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where assigneeId is not null
        defaultTaskFiltering("assigneeId.specified=true", "assigneeId.specified=false");
    }

    @Test
    @Transactional
    void getAllTasksByDueDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where dueDate equals to
        defaultTaskFiltering("dueDate.equals=" + DEFAULT_DUE_DATE, "dueDate.equals=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllTasksByDueDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where dueDate in
        defaultTaskFiltering("dueDate.in=" + DEFAULT_DUE_DATE + "," + UPDATED_DUE_DATE, "dueDate.in=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllTasksByDueDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where dueDate is not null
        defaultTaskFiltering("dueDate.specified=true", "dueDate.specified=false");
    }

    @Test
    @Transactional
    void getAllTasksByDueDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where dueDate is greater than or equal to
        defaultTaskFiltering("dueDate.greaterThanOrEqual=" + DEFAULT_DUE_DATE, "dueDate.greaterThanOrEqual=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllTasksByDueDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where dueDate is less than or equal to
        defaultTaskFiltering("dueDate.lessThanOrEqual=" + DEFAULT_DUE_DATE, "dueDate.lessThanOrEqual=" + SMALLER_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllTasksByDueDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where dueDate is less than
        defaultTaskFiltering("dueDate.lessThan=" + UPDATED_DUE_DATE, "dueDate.lessThan=" + DEFAULT_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllTasksByDueDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where dueDate is greater than
        defaultTaskFiltering("dueDate.greaterThan=" + SMALLER_DUE_DATE, "dueDate.greaterThan=" + DEFAULT_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllTasksByOriginKindIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originKind equals to
        defaultTaskFiltering("originKind.equals=" + DEFAULT_ORIGIN_KIND, "originKind.equals=" + UPDATED_ORIGIN_KIND);
    }

    @Test
    @Transactional
    void getAllTasksByOriginKindIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originKind in
        defaultTaskFiltering("originKind.in=" + DEFAULT_ORIGIN_KIND + "," + UPDATED_ORIGIN_KIND, "originKind.in=" + UPDATED_ORIGIN_KIND);
    }

    @Test
    @Transactional
    void getAllTasksByOriginKindIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originKind is not null
        defaultTaskFiltering("originKind.specified=true", "originKind.specified=false");
    }

    @Test
    @Transactional
    void getAllTasksByOriginReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originReference equals to
        defaultTaskFiltering("originReference.equals=" + DEFAULT_ORIGIN_REFERENCE, "originReference.equals=" + UPDATED_ORIGIN_REFERENCE);
    }

    @Test
    @Transactional
    void getAllTasksByOriginReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originReference in
        defaultTaskFiltering(
            "originReference.in=" + DEFAULT_ORIGIN_REFERENCE + "," + UPDATED_ORIGIN_REFERENCE,
            "originReference.in=" + UPDATED_ORIGIN_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllTasksByOriginReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originReference is not null
        defaultTaskFiltering("originReference.specified=true", "originReference.specified=false");
    }

    @Test
    @Transactional
    void getAllTasksByOriginReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originReference contains
        defaultTaskFiltering(
            "originReference.contains=" + DEFAULT_ORIGIN_REFERENCE,
            "originReference.contains=" + UPDATED_ORIGIN_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllTasksByOriginReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originReference does not contain
        defaultTaskFiltering(
            "originReference.doesNotContain=" + UPDATED_ORIGIN_REFERENCE,
            "originReference.doesNotContain=" + DEFAULT_ORIGIN_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllTasksByOriginServiceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originService equals to
        defaultTaskFiltering("originService.equals=" + DEFAULT_ORIGIN_SERVICE, "originService.equals=" + UPDATED_ORIGIN_SERVICE);
    }

    @Test
    @Transactional
    void getAllTasksByOriginServiceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originService in
        defaultTaskFiltering(
            "originService.in=" + DEFAULT_ORIGIN_SERVICE + "," + UPDATED_ORIGIN_SERVICE,
            "originService.in=" + UPDATED_ORIGIN_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllTasksByOriginServiceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originService is not null
        defaultTaskFiltering("originService.specified=true", "originService.specified=false");
    }

    @Test
    @Transactional
    void getAllTasksByOriginServiceContainsSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originService contains
        defaultTaskFiltering("originService.contains=" + DEFAULT_ORIGIN_SERVICE, "originService.contains=" + UPDATED_ORIGIN_SERVICE);
    }

    @Test
    @Transactional
    void getAllTasksByOriginServiceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where originService does not contain
        defaultTaskFiltering(
            "originService.doesNotContain=" + UPDATED_ORIGIN_SERVICE,
            "originService.doesNotContain=" + DEFAULT_ORIGIN_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllTasksByStartedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where startedAt equals to
        defaultTaskFiltering("startedAt.equals=" + DEFAULT_STARTED_AT, "startedAt.equals=" + UPDATED_STARTED_AT);
    }

    @Test
    @Transactional
    void getAllTasksByStartedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where startedAt in
        defaultTaskFiltering("startedAt.in=" + DEFAULT_STARTED_AT + "," + UPDATED_STARTED_AT, "startedAt.in=" + UPDATED_STARTED_AT);
    }

    @Test
    @Transactional
    void getAllTasksByStartedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where startedAt is not null
        defaultTaskFiltering("startedAt.specified=true", "startedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllTasksByCompletedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where completedAt equals to
        defaultTaskFiltering("completedAt.equals=" + DEFAULT_COMPLETED_AT, "completedAt.equals=" + UPDATED_COMPLETED_AT);
    }

    @Test
    @Transactional
    void getAllTasksByCompletedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where completedAt in
        defaultTaskFiltering(
            "completedAt.in=" + DEFAULT_COMPLETED_AT + "," + UPDATED_COMPLETED_AT,
            "completedAt.in=" + UPDATED_COMPLETED_AT
        );
    }

    @Test
    @Transactional
    void getAllTasksByCompletedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where completedAt is not null
        defaultTaskFiltering("completedAt.specified=true", "completedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllTasksByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where state equals to
        defaultTaskFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllTasksByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where state in
        defaultTaskFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllTasksByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where state is not null
        defaultTaskFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllTasksByReminderLevelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where reminderLevel equals to
        defaultTaskFiltering("reminderLevel.equals=" + DEFAULT_REMINDER_LEVEL, "reminderLevel.equals=" + UPDATED_REMINDER_LEVEL);
    }

    @Test
    @Transactional
    void getAllTasksByReminderLevelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where reminderLevel in
        defaultTaskFiltering(
            "reminderLevel.in=" + DEFAULT_REMINDER_LEVEL + "," + UPDATED_REMINDER_LEVEL,
            "reminderLevel.in=" + UPDATED_REMINDER_LEVEL
        );
    }

    @Test
    @Transactional
    void getAllTasksByReminderLevelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where reminderLevel is not null
        defaultTaskFiltering("reminderLevel.specified=true", "reminderLevel.specified=false");
    }

    @Test
    @Transactional
    void getAllTasksByReminderLevelIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where reminderLevel is greater than or equal to
        defaultTaskFiltering(
            "reminderLevel.greaterThanOrEqual=" + DEFAULT_REMINDER_LEVEL,
            "reminderLevel.greaterThanOrEqual=" + (DEFAULT_REMINDER_LEVEL + 1)
        );
    }

    @Test
    @Transactional
    void getAllTasksByReminderLevelIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where reminderLevel is less than or equal to
        defaultTaskFiltering(
            "reminderLevel.lessThanOrEqual=" + DEFAULT_REMINDER_LEVEL,
            "reminderLevel.lessThanOrEqual=" + SMALLER_REMINDER_LEVEL
        );
    }

    @Test
    @Transactional
    void getAllTasksByReminderLevelIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where reminderLevel is less than
        defaultTaskFiltering("reminderLevel.lessThan=" + (DEFAULT_REMINDER_LEVEL + 1), "reminderLevel.lessThan=" + DEFAULT_REMINDER_LEVEL);
    }

    @Test
    @Transactional
    void getAllTasksByReminderLevelIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where reminderLevel is greater than
        defaultTaskFiltering("reminderLevel.greaterThan=" + SMALLER_REMINDER_LEVEL, "reminderLevel.greaterThan=" + DEFAULT_REMINDER_LEVEL);
    }

    @Test
    @Transactional
    void getAllTasksByClosureProposedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where closureProposedAt equals to
        defaultTaskFiltering(
            "closureProposedAt.equals=" + DEFAULT_CLOSURE_PROPOSED_AT,
            "closureProposedAt.equals=" + UPDATED_CLOSURE_PROPOSED_AT
        );
    }

    @Test
    @Transactional
    void getAllTasksByClosureProposedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where closureProposedAt in
        defaultTaskFiltering(
            "closureProposedAt.in=" + DEFAULT_CLOSURE_PROPOSED_AT + "," + UPDATED_CLOSURE_PROPOSED_AT,
            "closureProposedAt.in=" + UPDATED_CLOSURE_PROPOSED_AT
        );
    }

    @Test
    @Transactional
    void getAllTasksByClosureProposedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        // Get all the taskList where closureProposedAt is not null
        defaultTaskFiltering("closureProposedAt.specified=true", "closureProposedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllTasksByBoardIsEqualToSomething() throws Exception {
        Board board;
        if (TestUtil.findAll(em, Board.class).isEmpty()) {
            taskRepository.saveAndFlush(task);
            board = BoardResourceIT.createEntity();
        } else {
            board = TestUtil.findAll(em, Board.class).get(0);
        }
        em.persist(board);
        em.flush();
        task.setBoard(board);
        taskRepository.saveAndFlush(task);
        UUID boardId = board.getId();
        // Get all the taskList where board equals to boardId
        defaultTaskShouldBeFound("boardId.equals=" + boardId);

        // Get all the taskList where board equals to UUID.randomUUID()
        defaultTaskShouldNotBeFound("boardId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllTasksByBoardColumnIsEqualToSomething() throws Exception {
        BoardColumn boardColumn;
        if (TestUtil.findAll(em, BoardColumn.class).isEmpty()) {
            taskRepository.saveAndFlush(task);
            boardColumn = BoardColumnResourceIT.createEntity(em);
        } else {
            boardColumn = TestUtil.findAll(em, BoardColumn.class).get(0);
        }
        em.persist(boardColumn);
        em.flush();
        task.setBoardColumn(boardColumn);
        taskRepository.saveAndFlush(task);
        UUID boardColumnId = boardColumn.getId();
        // Get all the taskList where boardColumn equals to boardColumnId
        defaultTaskShouldBeFound("boardColumnId.equals=" + boardColumnId);

        // Get all the taskList where boardColumn equals to UUID.randomUUID()
        defaultTaskShouldNotBeFound("boardColumnId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllTasksByActionSheetIsEqualToSomething() throws Exception {
        ActionSheet actionSheet;
        if (TestUtil.findAll(em, ActionSheet.class).isEmpty()) {
            taskRepository.saveAndFlush(task);
            actionSheet = ActionSheetResourceIT.createEntity();
        } else {
            actionSheet = TestUtil.findAll(em, ActionSheet.class).get(0);
        }
        em.persist(actionSheet);
        em.flush();
        task.setActionSheet(actionSheet);
        taskRepository.saveAndFlush(task);
        UUID actionSheetId = actionSheet.getId();
        // Get all the taskList where actionSheet equals to actionSheetId
        defaultTaskShouldBeFound("actionSheetId.equals=" + actionSheetId);

        // Get all the taskList where actionSheet equals to UUID.randomUUID()
        defaultTaskShouldNotBeFound("actionSheetId.equals=" + UUID.randomUUID());
    }

    private void defaultTaskFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultTaskShouldBeFound(shouldBeFound);
        defaultTaskShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultTaskShouldBeFound(String filter) throws Exception {
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(task.getId().toString())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].assigneeId").value(hasItem(DEFAULT_ASSIGNEE_ID.toString())))
            .andExpect(jsonPath("$.[*].dueDate").value(hasItem(DEFAULT_DUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].originKind").value(hasItem(DEFAULT_ORIGIN_KIND.toString())))
            .andExpect(jsonPath("$.[*].originReference").value(hasItem(DEFAULT_ORIGIN_REFERENCE)))
            .andExpect(jsonPath("$.[*].originService").value(hasItem(DEFAULT_ORIGIN_SERVICE)))
            .andExpect(jsonPath("$.[*].startedAt").value(hasItem(DEFAULT_STARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].completedAt").value(hasItem(DEFAULT_COMPLETED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].reminderLevel").value(hasItem(DEFAULT_REMINDER_LEVEL)))
            .andExpect(jsonPath("$.[*].closureProposedAt").value(hasItem(DEFAULT_CLOSURE_PROPOSED_AT.toString())));

        // Check, that the count call also returns 1
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultTaskShouldNotBeFound(String filter) throws Exception {
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingTask() throws Exception {
        // Get the task
        restTaskMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingTask() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the task
        Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedTask are not directly saved in db
        em.detach(updatedTask);
        updatedTask
            .title(UPDATED_TITLE)
            .assigneeId(UPDATED_ASSIGNEE_ID)
            .dueDate(UPDATED_DUE_DATE)
            .originKind(UPDATED_ORIGIN_KIND)
            .originReference(UPDATED_ORIGIN_REFERENCE)
            .originService(UPDATED_ORIGIN_SERVICE)
            .startedAt(UPDATED_STARTED_AT)
            .completedAt(UPDATED_COMPLETED_AT)
            .state(UPDATED_STATE)
            .reminderLevel(UPDATED_REMINDER_LEVEL)
            .closureProposedAt(UPDATED_CLOSURE_PROPOSED_AT);
        TaskDTO taskDTO = taskMapper.toDto(updatedTask);

        restTaskMockMvc
            .perform(
                put(ENTITY_API_URL_ID, taskDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(taskDTO))
            )
            .andExpect(status().isOk());

        // Validate the Task in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedTaskToMatchAllProperties(updatedTask);
    }

    @Test
    @Transactional
    void putNonExistingTask() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        task.setId(UUID.randomUUID());

        // Create the Task
        TaskDTO taskDTO = taskMapper.toDto(task);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTaskMockMvc
            .perform(
                put(ENTITY_API_URL_ID, taskDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(taskDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Task in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchTask() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        task.setId(UUID.randomUUID());

        // Create the Task
        TaskDTO taskDTO = taskMapper.toDto(task);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTaskMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(taskDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Task in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamTask() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        task.setId(UUID.randomUUID());

        // Create the Task
        TaskDTO taskDTO = taskMapper.toDto(task);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTaskMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(taskDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Task in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateTaskWithPatch() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the task using partial update
        Task partialUpdatedTask = new Task();
        partialUpdatedTask.setId(task.getId());

        partialUpdatedTask
            .title(UPDATED_TITLE)
            .dueDate(UPDATED_DUE_DATE)
            .originService(UPDATED_ORIGIN_SERVICE)
            .startedAt(UPDATED_STARTED_AT)
            .reminderLevel(UPDATED_REMINDER_LEVEL);

        restTaskMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTask.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTask))
            )
            .andExpect(status().isOk());

        // Validate the Task in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTaskUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedTask, task), getPersistedTask(task));
    }

    @Test
    @Transactional
    void fullUpdateTaskWithPatch() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the task using partial update
        Task partialUpdatedTask = new Task();
        partialUpdatedTask.setId(task.getId());

        partialUpdatedTask
            .title(UPDATED_TITLE)
            .assigneeId(UPDATED_ASSIGNEE_ID)
            .dueDate(UPDATED_DUE_DATE)
            .originKind(UPDATED_ORIGIN_KIND)
            .originReference(UPDATED_ORIGIN_REFERENCE)
            .originService(UPDATED_ORIGIN_SERVICE)
            .startedAt(UPDATED_STARTED_AT)
            .completedAt(UPDATED_COMPLETED_AT)
            .state(UPDATED_STATE)
            .reminderLevel(UPDATED_REMINDER_LEVEL)
            .closureProposedAt(UPDATED_CLOSURE_PROPOSED_AT);

        restTaskMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTask.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTask))
            )
            .andExpect(status().isOk());

        // Validate the Task in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTaskUpdatableFieldsEquals(partialUpdatedTask, getPersistedTask(partialUpdatedTask));
    }

    @Test
    @Transactional
    void patchNonExistingTask() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        task.setId(UUID.randomUUID());

        // Create the Task
        TaskDTO taskDTO = taskMapper.toDto(task);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTaskMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, taskDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(taskDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Task in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchTask() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        task.setId(UUID.randomUUID());

        // Create the Task
        TaskDTO taskDTO = taskMapper.toDto(task);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTaskMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(taskDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Task in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamTask() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        task.setId(UUID.randomUUID());

        // Create the Task
        TaskDTO taskDTO = taskMapper.toDto(task);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTaskMockMvc
            .perform(patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(taskDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Task in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteTask() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the task
        restTaskMockMvc
            .perform(delete(ENTITY_API_URL_ID, task.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return taskRepository.count();
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

    protected Task getPersistedTask(Task task) {
        return taskRepository.findById(task.getId()).orElseThrow();
    }

    protected void assertPersistedTaskToMatchAllProperties(Task expectedTask) {
        assertTaskAllPropertiesEquals(expectedTask, getPersistedTask(expectedTask));
    }

    protected void assertPersistedTaskToMatchUpdatableProperties(Task expectedTask) {
        assertTaskAllUpdatablePropertiesEquals(expectedTask, getPersistedTask(expectedTask));
    }
}
