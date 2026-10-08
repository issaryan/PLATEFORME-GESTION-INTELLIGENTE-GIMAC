package cm.gimac.des.homologation.domain.parcours;

import static cm.gimac.des.homologation.domain.parcours.SignaturesSamples.J;
import static cm.gimac.des.homologation.domain.parcours.SignaturesSamples.piece;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Signatures d'une opération à l'exécution (D-91, docs/lot1/types-projet.md §3). */
class SignaturesOperationTest {

    static final Instant T = Instant.parse("2026-10-06T09:00:00Z");

    Parcours p;

    @BeforeEach
    void init() {
        SignaturesSamples.brancherCatalogue();
        p = SignaturesSamples.parcours();
    }

    @AfterEach
    void fin() {
        CatalogueFonctions.brancher(null);
    }

    static void refus(ThrowingCallable c, String code) {
        assertThatThrownBy(c).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    static String sha256(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    OperationParcours op(String rang) {
        return p.operation(rang).orElseThrow();
    }

    Personne conducteur() {
        return new Personne(SignaturesSamples.CONDUCTEUR.toString(), "Conducteur de projet 1", Fonction.CONDUCTEUR);
    }

    @Test
    @DisplayName("Signatures recopiées, ordonnées ; l'opération attend ses signatures avant d'être close, dans leur ordre")
    void signaturesOrdonnees() throws Exception {
        OperationParcours sig = op("SIG");
        assertThat(sig.getSignatures()).extracting(SignatureOperation::code).containsExactly("VISA_JURIDIQUE", "APPROBATION");
        assertThat(op("OUV").getSignatures()).isEmpty();
        refus(() -> p.signer(sig, "VISA_JURIDIQUE", conducteur(), T, J), "HOM_SIGNATURE_ETAT");
        p.deposerLivrable(op("OUV"), piece("DOC-OUV"), J);
        assertThat(op("OUV").faite()).isTrue();
        // Une opération déjà en attente de signature ne se clôt pas deux fois.
        p.deposerLivrable(sig, piece("DOC-SIG"), J);
        assertThat(sig.getEtat()).isEqualTo(EtatOperationParcours.EN_SIGNATURE);
        assertThat(sig.getEtat().enCours()).isTrue();
        assertThat(sig.getEtat().ouverte()).isFalse();
        assertThat(sig.getFaiteLe()).isNull();
        p.forcerCloture(sig, piece("AUTRE"), J);
        assertThat(sig.getPreuve().documentRef()).isEqualTo("DOC-SIG");
        assertThat(sig.signaturesSuivantes()).extracting(SignatureOperation::code).containsExactly("VISA_JURIDIQUE");
        refus(() -> p.signer(sig, "APPROBATION", conducteur(), T, J), "HOM_SIGNATURE_ORDRE");
        refus(() -> p.signer(sig, "INCONNUE", conducteur(), T, J), "HOM_SIGNATURE_INCONNUE");

        Personne juriste = new Personne(UUID.randomUUID().toString(), "Juriste", SignaturesSamples.JURISTE);
        SignatureRecueillie r = p.signer(sig, "VISA_JURIDIQUE", juriste, T, J);
        assertThat(r.empreinte()).isEqualTo(sha256("L1:DOC-SIG:" + "a".repeat(64) + "\nOUV/L1:DOC-OUV:" + "a".repeat(64) + "\n"));
        assertThat(r.documents()).extracting(SignatureRecueillie.DocumentSigne::livrable).containsExactly("L1", "OUV/L1");
        assertThat(r.fonction()).isEqualTo(SignaturesSamples.JURISTE);
        assertThat(sig.getEtat()).isEqualTo(EtatOperationParcours.EN_SIGNATURE);
        refus(() -> p.signer(sig, "VISA_JURIDIQUE", juriste, T, J), "HOM_SIGNATURE_DEJA_SIGNEE");
        assertThat(sig.recueillie("VISA_JURIDIQUE")).isPresent();
        assertThat(sig.signaturesSuivantes()).extracting(SignatureOperation::code).containsExactly("APPROBATION");

        p.signer(sig, "APPROBATION", conducteur(), T, J);
        assertThat(sig.faite()).isTrue();
        assertThat(sig.getFaiteLe()).isEqualTo(J);
        assertThat(sig.signaturesAttendues()).isEmpty();
        assertThat(sig.signaturesSuivantes()).isEmpty();
        assertThat(sig.getSignaturesRecueillies()).hasSize(2);
        assertThat(p.etat().getStatut()).isNotNull();
    }

    @Test
    @DisplayName("Signataire attendu : qualités de l'opération, ressource affectée, conducteur, sinon aucun ; empreinte sans pièce")
    void titulaireEtEmpreinte() throws Exception {
        OperationParcours sig = op("SIG");
        assertThat(p.titulaire(sig, Fonction.CONDUCTEUR)).map(Personne::id).contains(SignaturesSamples.CONDUCTEUR.toString());
        assertThat(p.titulaire(sig, SignaturesSamples.JURISTE)).isEmpty();
        assertThat(p.titulaire(op("OUV"), SignaturesSamples.JURISTE)).isEmpty();
        Personne j = new Personne(UUID.randomUUID().toString(), "Juriste désigné", SignaturesSamples.JURISTE);
        p.etat().setRessources(Map.of(SignaturesSamples.JURISTE, j));
        assertThat(p.titulaire(sig, SignaturesSamples.JURISTE)).contains(j);
        // Conducteur à désigner : personne n'est attendu pour la fonction en dehors des qualités.
        p.etat().setConducteurADesigner(true);
        assertThat(p.titulaire(op("SIG"), Fonction.DG)).isEmpty();
        Parcours.Empreinte e = p.empreinte(sig, Parcours.signature(sig, "VISA_JURIDIQUE"));
        assertThat(e.documents()).isEmpty();
        assertThat(e.empreinte()).isEqualTo(sha256(""));
        assertThat(SignatureOperation.operationDe("L1")).isNull();
        assertThat(SignatureOperation.operationDe("OUV/L1")).isEqualTo("OUV");
        assertThat(SignatureOperation.livrableDe("OUV/L1")).isEqualTo("L1");
        assertThat(new SignatureOperation("SA", "A", Fonction.DG, null, null).ordreEffectif()).isEqualTo(1);
        assertThat(new SignatureRecueillie("A", "x", "X", Fonction.DG, T, "e", null).documents()).isEmpty();
    }

    @Test
    @DisplayName("Migration : une opération en attente de signature dont la nouvelle version n'exige plus rien se clôt")
    void migrationSansSignature() {
        OperationParcours sig = op("SIG");
        p.deposerLivrable(op("OUV"), piece("DOC-OUV"), J);
        p.deposerLivrable(sig, piece("DOC-SIG"), J);
        ReferentialVersion v2 = SignaturesSamples.version(List.of());
        p.migrer(v2.getId(), 2, v2.getOperations(), SignaturesSamples.designation(), J);
        assertThat(op("SIG").faite()).isTrue();
        assertThat(op("SIG").getSignatures()).isEmpty();
    }

    @Test
    @DisplayName("Opération sans signature : clôture inchangée")
    void sansSignature() {
        ReferentialVersion v = SignaturesSamples.version(List.of());
        assertThat(v.operation("SIG").orElseThrow().getSignatures()).isEmpty();
        OperationParcours ouv = op("OUV");
        p.terminer(ouv, piece("D"), J);
        assertThat(ouv.faite()).isTrue();
        assertThat(ouv.getSignaturesRecueillies()).isEmpty();
    }
}
