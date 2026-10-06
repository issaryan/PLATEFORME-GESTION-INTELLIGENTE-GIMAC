package cm.gimac.des.pilotage.infrastructure.client;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Membre;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Portee;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import java.util.Map;
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

    @Test
    @DisplayName("D-81 : chemin des unités, unités dirigées, matrice par défaut des responsables, périmètres par portée")
    void pouvoirs() {
        assertThat(organisation.rattachement(AGENT).orElseThrow().chemin()).containsExactly("DG", "DES", "SNA");
        assertThat(organisation.rattachement(AGENT).orElseThrow().dans("DES")).isTrue();
        assertThat(organisation.rattachement(AGENT).orElseThrow().dans("DDRM")).isFalse();

        var chef = organisation.pouvoirs(CHEF_DES);
        assertThat(chef.portee(Pouvoir.TACHE_CONFIER)).isEqualTo(Portee.UNITES);
        assertThat(chef.portee(Pouvoir.ACTIVITE_LIRE)).isEqualTo(Portee.EQUIPE);
        assertThat(chef.unitesDirigees()).contains("DES", "SNA", "PROD_INFO", "POLE_RESEAU").doesNotContain("DDRM", "DG");
        assertThat(chef.dirige("POLE_SYSTEME")).isTrue();
        assertThat(chef.profilsDemandables()).isNotEmpty();
        assertThat(organisation.pouvoirs(AGENT).portee(Pouvoir.TACHE_CONFIER)).isEqualTo(Portee.AUCUNE);
        assertThat(organisation.pouvoirs(AGENT).unitesDirigees()).isEmpty();

        assertThat(organisation.perimetre(CHEF_DES, Portee.DIRECTS).comptes().stream().map(Membre::id)).containsExactly(CHEF_SERVICE);
        assertThat(organisation.perimetre(CHEF_DES, Portee.EQUIPE).comptes().stream().map(Membre::id)).contains(CHEF_SERVICE, AGENT, AGENT_BIS);
        assertThat(organisation.perimetre(CHEF_DES, Portee.AUCUNE).comptes()).isEmpty();
        // UNITES : équipe et membres des unités dirigées, même sans lien hiérarchique.
        organisation.enregistrer(new OrganisationStubAdapter.Compte("reseau-1", "Ingrid Mballa", "Ingénieure réseau", "POLE_RESEAU", null, true, true));
        var unites = organisation.perimetre(CHEF_DES, Portee.UNITES);
        assertThat(unites.comptes().stream().map(Membre::id)).contains("reseau-1", AGENT);
        assertThat(unites.unites()).contains("POLE_RESEAU");
        assertThat(organisation.perimetre(CHEF_DES, Portee.EQUIPE).membre("reseau-1")).isEmpty();

        organisation.definirPouvoirs(AGENT, Map.of(Pouvoir.TACHE_SUIVRE, Portee.DIRECTS));
        assertThat(organisation.pouvoirs(AGENT).portee(Pouvoir.TACHE_SUIVRE)).isEqualTo(Portee.DIRECTS);
        organisation.reinitialiser();
        assertThat(organisation.pouvoirs(AGENT).portee(Pouvoir.TACHE_SUIVRE)).isEqualTo(Portee.AUCUNE);
        assertThat(organisation.collaborateur("reseau-1")).isEmpty();
    }
}
