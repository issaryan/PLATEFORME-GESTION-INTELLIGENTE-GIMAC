package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.ProjectTestSamples.*;
import static cm.gimac.des.pilotage.domain.ProjectTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProjectTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Project.class);
        Project project1 = getProjectSample1();
        Project project2 = new Project();
        assertThat(project1).isNotEqualTo(project2);

        project2.setId(project1.getId());
        assertThat(project1).isEqualTo(project2);

        project2 = getProjectSample2();
        assertThat(project1).isNotEqualTo(project2);
    }

    @Test
    void dependenciesTest() {
        Project project = getProjectRandomSampleGenerator();
        Project projectBack = getProjectRandomSampleGenerator();

        project.addDependencies(projectBack);
        assertThat(project.getDependencies()).containsOnly(projectBack);

        project.removeDependencies(projectBack);
        assertThat(project.getDependencies()).doesNotContain(projectBack);

        project.dependencies(new HashSet<>(Set.of(projectBack)));
        assertThat(project.getDependencies()).containsOnly(projectBack);

        project.setDependencies(new HashSet<>());
        assertThat(project.getDependencies()).doesNotContain(projectBack);
    }

    @Test
    void dependentsTest() {
        Project project = getProjectRandomSampleGenerator();
        Project projectBack = getProjectRandomSampleGenerator();

        project.addDependents(projectBack);
        assertThat(project.getDependents()).containsOnly(projectBack);
        assertThat(projectBack.getDependencies()).containsOnly(project);

        project.removeDependents(projectBack);
        assertThat(project.getDependents()).doesNotContain(projectBack);
        assertThat(projectBack.getDependencies()).doesNotContain(project);

        project.dependents(new HashSet<>(Set.of(projectBack)));
        assertThat(project.getDependents()).containsOnly(projectBack);
        assertThat(projectBack.getDependencies()).containsOnly(project);

        project.setDependents(new HashSet<>());
        assertThat(project.getDependents()).doesNotContain(projectBack);
        assertThat(projectBack.getDependencies()).doesNotContain(project);
    }
}
