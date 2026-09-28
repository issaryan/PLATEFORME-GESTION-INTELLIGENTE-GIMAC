package cm.gimac.des.iam.domain;

import static cm.gimac.des.iam.domain.CollaboratorTestSamples.*;
import static cm.gimac.des.iam.domain.UserSessionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class UserSessionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(UserSession.class);
        UserSession userSession1 = getUserSessionSample1();
        UserSession userSession2 = new UserSession();
        assertThat(userSession1).isNotEqualTo(userSession2);

        userSession2.setId(userSession1.getId());
        assertThat(userSession1).isEqualTo(userSession2);

        userSession2 = getUserSessionSample2();
        assertThat(userSession1).isNotEqualTo(userSession2);
    }

    @Test
    void collaboratorTest() {
        UserSession userSession = getUserSessionRandomSampleGenerator();
        Collaborator collaboratorBack = getCollaboratorRandomSampleGenerator();

        userSession.setCollaborator(collaboratorBack);
        assertThat(userSession.getCollaborator()).isEqualTo(collaboratorBack);

        userSession.collaborator(null);
        assertThat(userSession.getCollaborator()).isNull();
    }
}
