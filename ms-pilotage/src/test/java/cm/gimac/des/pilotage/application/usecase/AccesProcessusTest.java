package cm.gimac.des.pilotage.application.usecase;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT_BIS;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DDRM;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.SUPER_ADMIN;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.PorteeCatalogue;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Portee;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoirs;
import cm.gimac.des.pilotage.domain.processus.Initiateurs;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.RegleInitiateur;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Droits des processus d'unité (D-87) et administration de tous les domaines (D-92) : REF_METIER_ADMIN du jeton,
 * portée TOUTES de la matrice, chef limité à ses unités, HOM_REFERENTIEL_EDIT sans effet ; mention du journal.
 * D-94 : IAM_PROFILE_EDIT (administration des comptes, RSSI) n'ouvre plus les processus des autres unités.
 */
class AccesProcessusTest {

    private static final List<String> UNITES = List.of("POLE_RESEAU", "DDRM", "SSI", "DG", "UNITE_SANS_MEMBRE");
    private OrganisationStubAdapter organisation;
    private AccesProcessus acces;

    @BeforeEach
    void preparer() {
        organisation = new OrganisationStubAdapter(true);
        acces = new AccesProcessus(new Equipes(organisation));
    }

    private static Appelant appelant(String id, String... permissions) {
        return new Appelant(id, id, Set.of(permissions));
    }

    @Test
    @DisplayName("REF_METIER_ADMIN, sans aucun pouvoir de matrice : tout, dans toutes les unités, et la mention au journal")
    void referentielsMetier() {
        for (String id : List.of(SUPER_ADMIN, AGENT)) {
            assertThat(organisation.pouvoirs(id).actions()).isEmpty();
            Appelant a = appelant(id, "PIL_ACTIVITY_OWN", "REF_METIER_ADMIN");
            for (String u : UNITES) {
                assertThat(acces.definit(a, u)).as("définir %s", u).isTrue();
                assertThat(acces.lance(a, u)).as("lancer %s", u).isTrue();
                assertThat(acces.lit(a, u)).as("lire %s", u).isTrue();
                assertThat(acces.pilote(a, u)).as("piloter %s", u).isTrue();
            }
            assertThat(acces.mention(a, "POLE_RESEAU", AccesProcessus.Acte.DEFINIR)).isEqualTo(" (au titre de REF_METIER_ADMIN)");
            assertThat(acces.mention(a, "DDRM", AccesProcessus.Acte.PILOTER)).isEqualTo(" (au titre de REF_METIER_ADMIN)");
            PorteeCatalogue p = acces.portee(a);
            assertThat(p.portee()).isEqualTo("TOUS_DOMAINES");
            assertThat(p.definirToutesUnites()).isTrue();
            assertThat(p.lancerToutesUnites()).isTrue();
            assertThat(p.auTitreDe()).containsExactly("REF_METIER_ADMIN");
            assertThat(p.unites()).isEmpty();
        }
        // Membre de l'unité : le lancement relève de la règle de l'unité, sans mention.
        Appelant agent = appelant(AGENT, "REF_METIER_ADMIN");
        assertThat(acces.mention(agent, "SNA", AccesProcessus.Acte.LANCER)).isEmpty();
    }

    @Test
    @DisplayName("Chef : ses unités seulement ; HOM_REFERENTIEL_EDIT seul n'ouvre pas les autres unités")
    void chefLimite() {
        Appelant chef = appelant(CHEF_RESEAU, "PIL_ACTIVITY_OWN", "PIL_TASK_ASSIGN");
        assertThat(acces.definit(chef, "POLE_RESEAU")).isTrue();
        assertThat(acces.pilote(chef, "POLE_RESEAU")).isTrue();
        assertThat(acces.mention(chef, "POLE_RESEAU", AccesProcessus.Acte.DEFINIR)).isEmpty();
        assertThat(acces.definit(chef, "DDRM")).isFalse();
        assertThat(acces.lance(chef, "DDRM")).isFalse();
        assertThat(acces.lit(chef, "DDRM")).isFalse();
        assertThat(acces.pilote(chef, "DDRM")).isFalse();
        PorteeCatalogue p = acces.portee(chef);
        assertThat(p.portee()).isEqualTo("MES_UNITES");
        assertThat(p.definirToutesUnites()).isFalse();
        assertThat(p.auTitreDe()).isEmpty();
        assertThat(p.unites()).contains("POLE_RESEAU");
        assertThat(p.unitesDefinissables()).containsExactly("POLE_RESEAU");

        Appelant chefDepartement = appelant(CHEF_DDRM, "PIL_ACTIVITY_OWN", "HOM_REFERENTIEL_EDIT");
        assertThat(acces.definit(chefDepartement, "DDRM")).isTrue();
        for (String u : List.of("POLE_RESEAU", "SSI", "SNA")) {
            assertThat(acces.definit(chefDepartement, u)).as("définir %s", u).isFalse();
            assertThat(acces.lance(chefDepartement, u)).as("lancer %s", u).isFalse();
            assertThat(acces.pilote(chefDepartement, u)).as("piloter %s", u).isFalse();
        }
        assertThat(acces.portee(chefDepartement).portee()).isEqualTo("MES_UNITES");
        assertThat(acces.litToutes(appelant(TECHNICIEN_RESEAU, "PIL_ACTIVITY_OWN"))).isFalse();
    }

    @Test
    @DisplayName("D-94 : un RSSI avec IAM_PROFILE_EDIT seul ne voit ni ne définit les processus des autres unités")
    void administrationDesComptesSansEffet() {
        // Compte sans unité ni pouvoir de matrice (comme un RSSI), avec l'administration des profils.
        Appelant rssi = appelant(SUPER_ADMIN, "PIL_ACTIVITY_OWN", "IAM_PROFILE_EDIT", "IAM_ACCOUNT_READ");
        for (String u : UNITES) {
            assertThat(acces.definit(rssi, u)).as("définir %s", u).isFalse();
            assertThat(acces.lit(rssi, u)).as("lire %s", u).isFalse();
            assertThat(acces.lance(rssi, u)).as("lancer %s", u).isFalse();
            assertThat(acces.pilote(rssi, u)).as("piloter %s", u).isFalse();
        }
        assertThat(acces.litToutes(rssi)).isFalse();
        assertThat(acces.definitToutes(rssi)).isFalse();
        PorteeCatalogue p = acces.portee(rssi);
        assertThat(p.portee()).isEqualTo("MES_UNITES");
        assertThat(p.auTitreDe()).isEmpty();
        assertThat(p.definirToutesUnites()).isFalse();
        // Membre d'une unité : la règle de l'unité seule, sans repli sur l'administration.
        Appelant agent = appelant(AGENT, "PIL_ACTIVITY_OWN", "IAM_PROFILE_EDIT");
        assertThat(acces.definit(agent, "POLE_RESEAU")).isFalse();
        assertThat(acces.lit(agent, "DDRM")).isFalse();
        assertThat(acces.mention(agent, "DDRM", AccesProcessus.Acte.DEFINIR)).isEmpty();
    }

    @Test
    @DisplayName("Portée TOUTES de la matrice, sans la permission : PROCESSUS_DEFINIR définit partout, PROCESSUS_LANCER lit et pilote partout")
    void porteeToutes() {
        organisation.definirPouvoirs(AGENT_BIS, Map.of(Pouvoir.PROCESSUS_DEFINIR, Portee.TOUTES));
        Appelant definit = appelant(AGENT_BIS, "PIL_ACTIVITY_OWN");
        for (String u : UNITES) {
            assertThat(acces.definit(definit, u)).as("définir %s", u).isTrue();
            assertThat(acces.lit(definit, u)).as("lire %s", u).isTrue();
            assertThat(acces.pilote(definit, u)).as("piloter %s", u).isFalse();
        }
        assertThat(acces.lance(definit, "DDRM")).isFalse();
        assertThat(acces.mention(definit, "DDRM", AccesProcessus.Acte.DEFINIR)).isEqualTo(" (au titre de PROCESSUS_DEFINIR portée TOUTES)");
        PorteeCatalogue p = acces.portee(definit);
        assertThat(p.portee()).isEqualTo("TOUS_DOMAINES");
        assertThat(p.definirToutesUnites()).isTrue();
        assertThat(p.lancerToutesUnites()).isFalse();
        assertThat(p.auTitreDe()).containsExactly("PROCESSUS_DEFINIR:TOUTES");

        organisation.definirPouvoirs(AGENT_BIS, Map.of(Pouvoir.PROCESSUS_LANCER, Portee.TOUTES));
        Appelant lance = appelant(AGENT_BIS, "PIL_ACTIVITY_OWN");
        for (String u : UNITES) {
            assertThat(acces.lance(lance, u)).as("lancer %s", u).isTrue();
            assertThat(acces.pilote(lance, u)).as("piloter %s", u).isTrue();
            assertThat(acces.definit(lance, u)).as("définir %s", u).isFalse();
        }
        assertThat(acces.mention(lance, "DDRM", AccesProcessus.Acte.PILOTER)).isEqualTo(" (au titre de PROCESSUS_LANCER portée TOUTES)");
        assertThat(acces.portee(lance).lancerToutesUnites()).isTrue();
    }

    @Test
    @DisplayName("Portées : TOUTES au-dessus de UNITES, lue sans casse ; inconnue ignorée ; non admise ramenée à UNITES")
    void portees() {
        assertThat(Portee.TOUTES).isGreaterThan(Portee.UNITES);
        assertThat(Portee.lire(" toutes ")).isEqualTo(Portee.TOUTES);
        assertThat(Portee.lire("PARTOUT")).isEqualTo(Portee.AUCUNE);
        Pouvoirs p = new Pouvoirs(Map.of(Pouvoir.TACHE_CONFIER, Portee.TOUTES, Pouvoir.PROCESSUS_LANCER, Portee.TOUTES), null, null, null);
        assertThat(p.portee(Pouvoir.TACHE_CONFIER)).isEqualTo(Portee.UNITES);
        assertThat(p.toutes(Pouvoir.PROCESSUS_LANCER)).isTrue();
        assertThat(Pouvoir.PROCESSUS_DEFINIR.admetToutes()).isTrue();
        assertThat(Pouvoir.TACHE_CONFIER.admetToutes()).isFalse();
    }

    private static Processus publie(String unite, Initiateurs initiateurs) {
        Processus p = Processus.brouillon("DEMANDE_TEST", unite, "a", java.time.Instant.EPOCH);
        p.definirLancement(initiateurs, null, "a", java.time.Instant.EPOCH);
        return p;
    }

    @Test
    @DisplayName("D-98 : initiateurs en données, cumulables ; D-100 : PROCESSUS_LANCER et REF_METIER_ADMIN n'y ajoutent rien")
    void initiateurs() {
        Appelant agentSna = appelant(AGENT, "PIL_ACTIVITY_OWN");
        Appelant technicien = appelant(TECHNICIEN_RESEAU, "PIL_ACTIVITY_OWN");
        Appelant chefDdrm = appelant(CHEF_DDRM, "PIL_ACTIVITY_OWN", "CER_DOSSIER_VERIFY");
        // Défaut : membres de l'unité propriétaire seulement (reprise des définitions existantes).
        Processus defaut = publie("POLE_RESEAU", null);
        assertThat(defaut.getInitiateurs().regles()).containsExactly(RegleInitiateur.MEMBRES_UNITE);
        assertThat(acces.lance(technicien, defaut)).isTrue();
        assertThat(acces.lance(agentSna, defaut)).isFalse();
        // Tout le GIMAC : tout compte interne actif, d'un autre service compris ; pas un compte inactif ni externe.
        Processus tous = publie("POLE_RESEAU", new Initiateurs(List.of(RegleInitiateur.TOUT_LE_GIMAC), null, null, null, null, false));
        assertThat(acces.lance(agentSna, tous)).isTrue();
        assertThat(acces.lance(appelant(OrganisationStubAdapter.INACTIF, "PIL_ACTIVITY_OWN"), tous)).isFalse();
        assertThat(acces.lance(appelant(OrganisationStubAdapter.CORRESPONDANT, "PIL_ACTIVITY_OWN"), tous)).isFalse();
        // Unités désignées (sous-unités comprises), profils, permissions du jeton ; règles cumulées.
        Processus unites = publie("POLE_RESEAU", new Initiateurs(List.of(RegleInitiateur.UNITES), List.of("DES"), null, null, null, false));
        assertThat(acces.lance(agentSna, unites)).as("SNA est une sous-unité du DES").isTrue();
        assertThat(acces.lance(chefDdrm, unites)).isFalse();
        Processus profils = publie("POLE_RESEAU", new Initiateurs(List.of(RegleInitiateur.PROFILS), null, List.of("ROLE_CHEF_DDRM"), null, null, false));
        assertThat(acces.lance(chefDdrm, profils)).isTrue();
        assertThat(acces.lance(agentSna, profils)).isFalse();
        Processus permissions = publie("POLE_RESEAU", new Initiateurs(List.of(RegleInitiateur.PERMISSIONS), null, null, List.of("CER_DOSSIER_VERIFY"), null, false));
        assertThat(acces.lance(chefDdrm, permissions)).isTrue();
        assertThat(acces.lance(agentSna, permissions)).isFalse();
        Processus cumul = publie("POLE_RESEAU", new Initiateurs(List.of(RegleInitiateur.PROFILS, RegleInitiateur.UNITES), List.of("SNA"), List.of("ROLE_CHEF_DDRM"), null, null, false));
        assertThat(acces.lance(chefDdrm, cumul)).isTrue();
        assertThat(acces.lance(agentSna, cumul)).isTrue();
        assertThat(acces.lance(technicien, cumul)).as("MEMBRES_UNITE non retenue").isFalse();
        // D-100 : les pouvoirs de pilotage n'ouvrent plus le lancement ; seul le public défini par le créateur compte.
        assertThat(acces.lance(appelant(CHEF_RESEAU, "PIL_ACTIVITY_OWN"), cumul)).isFalse();
        assertThat(acces.lance(appelant(SUPER_ADMIN, "REF_METIER_ADMIN"), cumul)).isFalse();
        // Refus explicite : les règles dans le message et en détail, sans mention d'un passe-droit.
        var refus = AccesProcessus.nonInitiateur(cumul);
        assertThat(refus.code()).isEqualTo("PIL_PROCESSUS_NON_INITIATEUR");
        assertThat(refus.status()).isEqualTo(403);
        assertThat(refus.getMessage()).contains("ROLE_CHEF_DDRM").contains("SNA").doesNotContain("PROCESSUS_LANCER");
        assertThat(refus.details()).containsEntry("regles", List.of("PROFILS", "UNITES")).containsEntry("personnes", List.of());
    }

    @Test
    @DisplayName("D-100 : le public défini par le créateur fait seul foi ; REF_METIER_ADMIN et PROCESSUS_LANCER à TOUTES ne lancent plus")
    void publicStrict() {
        Processus personnes = publie("POLE_SYSTEME", new Initiateurs(List.of(RegleInitiateur.PERSONNES), null, null, null, List.of(AGENT_BIS, OrganisationStubAdapter.INACTIF), false));
        Processus profils = publie("CONFORMITE", new Initiateurs(List.of(RegleInitiateur.PROFILS), null, List.of("ROLE_CONFORMITE"), null, null, false));
        // Super administrateur (REF_METIER_ADMIN) : ni l'un ni l'autre, mais il garde la définition, la lecture et le pilotage.
        Appelant admin = appelant(SUPER_ADMIN, "PIL_ACTIVITY_OWN", "REF_METIER_ADMIN");
        assertThat(acces.lance(admin, personnes)).isFalse();
        assertThat(acces.lance(admin, profils)).isFalse();
        assertThat(acces.definit(admin, "POLE_SYSTEME")).isTrue();
        assertThat(acces.lit(admin, "CONFORMITE")).isTrue();
        assertThat(acces.pilote(admin, "CONFORMITE")).isTrue();
        assertThat(acces.mention(admin, "CONFORMITE", AccesProcessus.Acte.LANCER)).isEmpty();
        // Portée TOUTES de PROCESSUS_LANCER : pilotage partout, lancement nulle part hors du public.
        organisation.definirPouvoirs(AGENT, Map.of(Pouvoir.PROCESSUS_LANCER, Portee.TOUTES));
        Appelant lanceur = appelant(AGENT, "PIL_ACTIVITY_OWN");
        assertThat(acces.lance(lanceur, personnes)).isFalse();
        assertThat(acces.lance(lanceur, profils)).isFalse();
        assertThat(acces.pilote(lanceur, "POLE_SYSTEME")).isTrue();
        // Personne désignée, compte interne actif : voit et lance ; désignée mais inactive : non.
        assertThat(acces.lance(appelant(AGENT_BIS, "PIL_ACTIVITY_OWN"), personnes)).isTrue();
        assertThat(acces.lance(appelant(OrganisationStubAdapter.INACTIF, "PIL_ACTIVITY_OWN"), personnes)).isFalse();
        // Porteur du profil désigné.
        organisation.attribuerProfils(AGENT_BIS, "ROLE_CONFORMITE");
        assertThat(acces.lance(appelant(AGENT_BIS, "PIL_ACTIVITY_OWN"), profils)).isTrue();
        // Refus : le message nomme les personnes désignées par leur nombre, le détail leurs identifiants.
        var refus = AccesProcessus.nonInitiateur(personnes);
        assertThat(refus.getMessage()).isEqualTo("Ce processus ne peut être lancé que par les 2 personnes désignées par son créateur");
        assertThat(refus.details()).containsEntry("regles", List.of("PERSONNES")).containsEntry("personnes", List.of(AGENT_BIS, OrganisationStubAdapter.INACTIF));
        assertThat(AccesProcessus.nonInitiateur(publie("SNA", new Initiateurs(List.of(RegleInitiateur.PERSONNES), null, null, null, List.of(AGENT), false))).getMessage())
            .endsWith("la personne désignée par son créateur");
    }

    @Test
    @DisplayName("D-100 : MEMBRES_UNITE couvre les membres et les responsables de l'unité (chef qui la dirige, sous-unités comprises)")
    void membresEtResponsables() {
        Processus defaut = publie("POLE_RESEAU", null);
        assertThat(acces.lance(appelant(TECHNICIEN_RESEAU, "PIL_ACTIVITY_OWN"), defaut)).as("membre").isTrue();
        assertThat(acces.lance(appelant(CHEF_RESEAU, "PIL_ACTIVITY_OWN"), defaut)).as("chef du pôle").isTrue();
        assertThat(acces.lance(appelant(OrganisationStubAdapter.CHEF_DES, "PIL_ACTIVITY_OWN"), defaut)).as("chef du DES, unité parente dirigée").isTrue();
        assertThat(acces.lance(appelant(CHEF_DDRM, "PIL_ACTIVITY_OWN"), defaut)).as("chef d'une autre unité").isFalse();
        assertThat(acces.lance(appelant(SUPER_ADMIN, "REF_METIER_ADMIN"), defaut)).as("administrateur hors de l'unité").isFalse();
        // Le chef de l'unité qui n'en est pas membre : unité sans membre, dirigée par la cheffe du SNA (sous-unité ajoutée).
        organisation.enregistrerUnite(new OrganisationStubAdapter.Unite("SNA_LABO", "SNA", null));
        Processus labo = publie("SNA_LABO", null);
        assertThat(acces.lance(appelant(OrganisationStubAdapter.CHEF_SERVICE, "PIL_ACTIVITY_OWN"), labo)).isTrue();
        assertThat(acces.lance(appelant(AGENT, "PIL_ACTIVITY_OWN"), labo)).isFalse();
    }
}
