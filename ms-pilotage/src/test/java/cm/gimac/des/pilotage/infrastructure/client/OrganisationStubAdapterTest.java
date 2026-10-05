package cm.gimac.des.pilotage.infrastructure.client;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Membre;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrganisationStubAdapterTest {

    private final OrganisationStubAdapter organisation = new OrganisationStubAdapter();

    @Test
    @DisplayName("Équipe récursive avec niveaux ; chaîne des responsables ; correspondant sans rattachement")
    void hierarchie() {
        assertThat(organisation.equipe(CHEF_SERVICE).stream().map(Membre::id)).containsExactlyInAnyOrder(AGENT, AGENT_BIS, INACTIF);
        assertThat(organisation.equipe(CHEF_DES)).anySatisfy(m -> {
            assertThat(m.id()).isEqualTo(AGENT);
            assertThat(m.niveau()).isEqualTo(2);
        });
        assertThat(organisation.equipe(AGENT)).isEmpty();
        assertThat(organisation.rattachement(AGENT).orElseThrow().chaine()).containsExactly(CHEF_SERVICE, CHEF_DES, DG);
        assertThat(organisation.rattachement(AGENT).orElseThrow().responsable().id()).isEqualTo(CHEF_SERVICE);
        assertThat(organisation.rattachement(SANS_RESPONSABLE).orElseThrow().responsable()).isNull();
        assertThat(organisation.rattachement(CORRESPONDANT)).isEmpty();
        assertThat(organisation.collaborateur(CORRESPONDANT).orElseThrow().interne()).isFalse();
    }
}
