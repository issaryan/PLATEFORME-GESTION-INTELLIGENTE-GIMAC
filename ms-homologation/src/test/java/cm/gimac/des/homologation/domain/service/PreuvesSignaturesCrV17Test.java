package cm.gimac.des.homologation.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Declencheurs;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.GrainLivrable;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Données D-109 (preuves_signatures_cr_v17.json) appliquées à l'annexe A v1.7 : une entrée par opération et par type,
 * versions publiables (complétude, graphe, livrables et signatures), règles du CR (preuve au grain de l'opération,
 * aucune signature sur une opération close par un geste dédié ou un événement, procès-verbal signé du GIMAC sous le
 * code du circuit de MS-CERTIFICATION, une seule pièce pour une clé ou une valeur de référence).
 */
class PreuvesSignaturesCrV17Test {

    static Map<String, ReferenceOperation> version(TypeProjet t) {
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(t, PreuvesSignaturesCrV17.libelleVersion(), Instant.parse("2026-10-15T00:00:00Z"));
        PreuvesSignaturesCrV17.version(t).forEach(o -> v.upsertOperation(o.code(), o.data()));
        assertThat(new ReferentialCompletenessChecker().check(v.getOperations())).isEmpty();
        var rapport = new ReferentialGraphValidator().controler(v.noeuds());
        assertThat(rapport.anomalies()).as("anomalies de publication " + t).isEmpty();
        return v.getOperations().stream().collect(Collectors.toMap(ReferenceOperation::getCode, Function.identity()));
    }

    @Test
    @DisplayName("Libellé de la version : mentionne « CR v1.7, preuves et signatures », 200 caractères au plus")
    void libelle() {
        assertThat(PreuvesSignaturesCrV17.libelleVersion()).contains("CR v1.7, preuves et signatures").hasSizeLessThanOrEqualTo(200);
        assertThat(PreuvesSignaturesCrV17.lire().decision()).isEqualTo("D-109");
    }

    @ParameterizedTest
    @ValueSource(strings = { "CARTE_INTERBANCAIRE", "PAIEMENT_MOBILE", "DELEGATAIRE" })
    @DisplayName("Chaque opération porte au moins un livrable décrit, sa preuve attendue, ses délais ; version publiable")
    void completude(String code) {
        TypeProjet t = TypeProjet.valueOf(code);
        Map<String, ReferenceOperation> ops = version(t);
        assertThat(ops).hasSize(Map.of(TypeProjet.CARTE_INTERBANCAIRE, 43, TypeProjet.PAIEMENT_MOBILE, 49, TypeProjet.DELEGATAIRE, 67).get(t));
        for (ReferenceOperation o : ops.values()) {
            assertThat(o.getLivrables()).as("livrables de " + o.getCode()).isNotEmpty();
            assertThat(o.getLivrables()).allSatisfy(l -> {
                assertThat(l.libelle()).isNotBlank();
                assertThat(l.deposant()).isNotNull();
            });
            assertThat(o.getPreuveAttendue()).contains("description").contains("source");
            assertThat(o.getDescription()).startsWith("Preuves et livrables attendus");
            assertThat(o.delaiMaxEffectif()).isGreaterThanOrEqualTo(o.delaiEffectif());
            if (o.isCle() || o.getValeurReference() != null) {
                assertThat(o.getLivrables()).as("une seule pièce pour " + o.getCode()).hasSize(1);
            }
            if (o.gesteEffectif() == Geste.EVENEMENT_EXTERNE || o.gesteEffectif().dedie() || o.gesteEffectif() == Geste.CONSTATER_COUVERTURE) {
                if (!"19".equals(o.getCode())) {
                    assertThat(o.getSignatures()).as("aucune signature sur " + o.getCode()).isEmpty();
                }
            }
        }
    }

    @Test
    @DisplayName("Exemples du CR et des procédures : ping au rang 13, points de terminaison dans PayWay (M8), fichier XML de fin de journée (C10), grain du service (16, 30)")
    void exemples() {
        Map<String, ReferenceOperation> carte = version(TypeProjet.CARTE_INTERBANCAIRE);
        Map<String, ReferenceOperation> mobile = version(TypeProjet.PAIEMENT_MOBILE);
        assertThat(carte.get("13").getLivrables().get(0).libelle()).contains("ping").contains("adresse et le port d'essai");
        assertThat(mobile.get("M8").getLivrables().get(0).libelle()).contains("PayWay").contains("points de terminaison");
        assertThat(mobile.get("M11").getLivrables().get(0).libelle()).contains("ping");
        assertThat(carte.get("C10").getLivrables()).extracting(Livrable::typesPieces).first().asList().contains("application/xml");
        assertThat(carte.get("16").getLivrables().get(0).grainEffectif()).isEqualTo(GrainLivrable.SERVICE);
        assertThat(mobile.get("16").getLivrables().get(0).grainEffectif()).isEqualTo(GrainLivrable.OPERATION);
        assertThat(carte.get("30").getLivrables().get(0).parService()).isTrue();
        assertThat(carte.get("21").getLivrables().get(0).libelle()).contains("PCI DSS");
        assertThat(mobile.get("21").getLivrables().get(0).libelle()).contains("ISO 27001");
        assertThat(carte.get("15").getLivrables().get(0).libelle()).contains("switch interbancaire");
        assertThat(mobile.get("15").getLivrables()).hasSize(2);
        assertThat(carte.get("4").getLivrables().get(0).consigne()).contains("sans objet");
    }

    @Test
    @DisplayName("Signatures : ordre et fonctions ; procès-verbal sous le code GIMAC du chef du DES ; validateurs du cadrage et du rapport")
    void signatures() {
        Map<String, ReferenceOperation> ops = version(TypeProjet.DELEGATAIRE);
        assertThat(ops.get("19").getSignatures()).extracting(SignatureOperation::code, SignatureOperation::signataire).containsExactly(org.assertj.core.groups.Tuple.tuple("GIMAC", Fonction.CHEF_DES));
        assertThat(ops.get("19").getDeclencheur()).isEqualTo(Declencheurs.PV_SIGNE);
        assertThat(ops.get("7").getSignatures()).extracting(SignatureOperation::signataire, SignatureOperation::ordreEffectif).containsExactly(
            org.assertj.core.groups.Tuple.tuple(Fonction.CHEF_DDRM, 1),
            org.assertj.core.groups.Tuple.tuple(Fonction.CHEF_DES, 2)
        );
        assertThat(ops.get("31").getSignatures()).extracting(SignatureOperation::signataire).containsExactly(Fonction.PRODUCTION_MONETIQUE, Fonction.CHEF_DES);
        assertThat(ops.get("3").getValidateur()).isEqualTo(Fonction.CHEF_DDRM);
        assertThat(ops.get("17").getValidateur()).isEqualTo(Fonction.CHEF_DES);
        assertThat(ops.get("D1").getExternes()).contains(Fonction.PERSONNALISATEUR);
        assertThat(ops.get("D29").getExternes()).contains(Fonction.BANQUE_PARTENAIRE);
        assertThat(ops.get("9").getLivrables().get(1).deposant()).isEqualTo(Fonction.PARTICIPANT);
        assertThat(ops.get("22").getLivrables().get(0).obligatoire()).isFalse();
    }

    @Test
    @DisplayName("Entrée absente ou ambiguë : refus explicite")
    void entree() {
        var f = PreuvesSignaturesCrV17.lire();
        assertThatThrownBy(() -> PreuvesSignaturesCrV17.entree(f, TypeProjet.PAIEMENT_MOBILE, "ZZ")).isInstanceOf(IllegalStateException.class).hasMessageContaining("0 entrée");
        var e = PreuvesSignaturesCrV17.entree(f, TypeProjet.PAIEMENT_MOBILE, "M8");
        assertThat(e.vaut(TypeProjet.PAIEMENT_MOBILE)).isTrue();
        assertThat(new PreuvesSignaturesCrV17.Entree("X", null, null, null, null, null, null, null, null, null, "p", null, "s").vaut(TypeProjet.DELEGATAIRE)).isTrue();
        assertThat(new PreuvesSignaturesCrV17.Entree("X", List.of(), null, null, null, null, null, null, null, null, "p", null, "s").livrables()).isEmpty();
    }
}
