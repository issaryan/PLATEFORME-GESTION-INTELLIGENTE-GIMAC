package cm.gimac.des.homologation.domain.parcours;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static cm.gimac.des.homologation.domain.parcours.ParcoursTest.J;
import static cm.gimac.des.homologation.domain.parcours.ParcoursTest.PIECE;
import static cm.gimac.des.homologation.domain.parcours.ParcoursTest.op;
import static cm.gimac.des.homologation.domain.parcours.ParcoursTest.refus;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Défauts trouvés sur la pile réelle (01/10/2026) : les verrous de la décision se fermaient par une pièce libre
 * (liaison CERTIFIEE sans procès-verbal), et la fonction « exploitation » n'avait aucun titulaire.
 */
class GestesDediesTest {

    @ParameterizedTest(name = "rang {0} : ni livrable ni preuve libres")
    @ValueSource(strings = { "6", "18", "19", "20", "21", "22", "23", "24", "25", "32", "33" })
    void gesteDedieRefuse(String rang) {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        OperationParcours o = op(p, rang);
        assertThat(Parcours.gesteDedie(o)).isTrue();
        refus(() -> p.deposerPreuve(o, PIECE, null, null, null), "HOM_OPERATION_DEDICATED_GESTURE");
        refus(() -> p.deposerLivrable(o, PIECE, J), "HOM_OPERATION_DEDICATED_GESTURE");
    }

    @Test
    @DisplayName("Verrou 1 ouvert : le rang 18 n'offre que l'attribution, jamais un dépôt ni une validation")
    void actionsDuVerrou() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(p, "17", J);
        OperationParcours o18 = op(p, "18");
        assertThat(o18.getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
        Acteur tout = new Acteur(UUID.randomUUID(), "Tout", Set.of("HOM_STEP_MANAGE", "HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "HOM_OPERATION_ASSIGN"), null);
        assertThat(new ParcoursProjection().actions(o18, tout)).containsExactly("ATTRIBUER");
        // Une opération ordinaire garde son geste.
        assertThat(Parcours.gesteDedie(op(p, "26"))).isFalse();
        // Onglet du geste : le suivi de l'exploitation (34) se prouve dans l'onglet des opérations.
        assertThat(ParcoursProjection.onglet(op(p, "34"))).isEqualTo("operations");
        assertThat(ParcoursProjection.onglet(op(p, "33"))).isEqualTo("mise-en-service");
        assertThat(ParcoursProjection.onglet(op(p, "35"))).isEqualTo("mise-en-service");
    }

    @Test
    @DisplayName("Rang 26 : ouvert après l'approbation, clos par la fiche déjà référencée (sinon bloqué, la fiche n'étant jamais refaite)")
    void rang26CloseParLaFicheReferencee() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(p, "22", J);
        p.ficheChangementReferencee("CHG-2026-0501", PIECE, J);
        assertThat(op(p, "26").getEtat()).isEqualTo(EtatOperationParcours.VERROUILLEE);
        p.securiteApprouvee(PIECE, J);
        assertThat(op(p, "25").faite()).isTrue();
        assertThat(op(p, "26").faite()).isTrue();
        assertThat(op(p, "26").getPreuve().documentRef()).isEqualTo("CHG-2026-0501");
        // Sans fiche référencée, le rang 26 reste à faire.
        Parcours q = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(q, "22", J);
        q.securiteApprouvee(PIECE, J);
        assertThat(op(q, "26").getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
    }

    @Test
    @DisplayName("Exploitation : tenue par la production monétique, qui porte aussi sa propre fonction")
    void exploitationParLaProductionMonetique() {
        assertThat(Fonction.EXPLOITATION.profils()).containsExactly("ROLE_PRODUCTION_MONETIQUE");
        assertThat(Fonction.desProfils(List.of("ROLE_PRODUCTION_MONETIQUE"))).containsExactlyInAnyOrder(Fonction.PRODUCTION_MONETIQUE, Fonction.EXPLOITATION);
        assertThat(Fonction.desProfils(List.of("ROLE_CHEF_DES"))).containsExactly(Fonction.CHEF_DES);
        assertThat(Fonction.duProfil("ROLE_PRODUCTION_MONETIQUE")).contains(Fonction.PRODUCTION_MONETIQUE);
    }

    @Test
    @DisplayName("Périmètre par défaut : le catalogue du type de projet")
    void catalogueParType() {
        assertThat(CatalogueServices.parType(TypeProjet.PAIEMENT_MOBILE)).containsExactly("PAIEMENT_MARCHAND", "REMITTANCE", "FACTURE", "CASH_IN");
        assertThat(CatalogueServices.parType(TypeProjet.CARTE_INTERBANCAIRE)).containsExactly("RETRAIT", "PAIEMENT", "TRANSFERT", "COMMERCE_ELECTRONIQUE");
        assertThat(CatalogueServices.parType(TypeProjet.DELEGATAIRE)).isEqualTo(CatalogueServices.parType(TypeProjet.CARTE_INTERBANCAIRE));
    }
}
