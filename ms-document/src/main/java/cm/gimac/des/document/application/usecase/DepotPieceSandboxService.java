package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Commande;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Deposant;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Fichier;
import cm.gimac.des.document.application.port.out.ExtracteurTexte;
import cm.gimac.des.document.application.port.out.PiecesSandbox;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.sandbox.PieceSandbox;
import cm.gimac.des.document.domain.sandbox.PieceSandbox.Etat;
import cm.gimac.des.document.domain.sandbox.TypePieceSandbox;
import cm.gimac.des.document.domain.service.ControleNumerosCarte;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.domain.service.ValidationDepot;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Dépôt d'une pièce de la Sandbox GIMAC par SBX-CAMPAGNE, SBX-TRACE ou SBX-ISO20022 (US-EXI-08, D-120) : type actif et
 * déposant admis, recherche d'un numéro de carte (quarantaine ou refus selon la règle du type, jamais de conservation
 * inaltérable), empreinte calculée comparée à l'empreinte annoncée, déduplication par institution, type et empreinte,
 * puis dépôt par la chaîne existante (finalité = code du type), inscription de la pièce et document.document-sealed.v1
 * dans l'outbox, dans une même transaction.
 */
public class DepotPieceSandboxService {

    private static final Logger LOG = LoggerFactory.getLogger(DepotPieceSandboxService.class);
    public static final String EVENEMENT_SCELLE = "document.document-sealed.v1";
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");
    private static final Pattern CODE_PARTICIPANT = Pattern.compile("^[A-Z0-9-]{1,20}$");

    /** Demande de dépôt : fichier, type, empreinte annoncée (hexadécimal), institution, référence (campagne, trace…). */
    public record Demande(Fichier fichier, String type, String empreinteAnnoncee, String codeParticipant, String reference) {}

    /** Résultat : pièce scellée (nouvelle ou déjà conservée) ou mise en quarantaine. */
    public record Resultat(UUID id, Etat etat, String sha256, Instant conserveeJusquAu, boolean dejaConservee) {}

    private final PiecesSandbox pieces;
    private final DeposerDocumentUseCase depot;
    private final ExtracteurTexte extracteur;
    private final Clock horloge;
    private final ObjectMapper json = new ObjectMapper();

    public DepotPieceSandboxService(PiecesSandbox pieces, DeposerDocumentUseCase depot, ExtracteurTexte extracteur, Clock horloge) {
        this.pieces = pieces;
        this.depot = depot;
        this.extracteur = extracteur;
        this.horloge = horloge;
    }

    @Transactional
    public Resultat deposer(Demande d, String service) {
        TypePieceSandbox type = pieces
            .type(d.type() == null ? "" : d.type().trim())
            .filter(TypePieceSandbox::actif)
            .orElseThrow(() -> DocumentException.invalide("DOC_SBX_TYPE_INCONNU", "Type de pièce de la sandbox inconnu ou inactif"));
        if (!type.autorise(service)) {
            LOG.warn("Dépôt d'une pièce {} refusé au service {}", type.code(), service);
            throw DocumentException.interdit("DOC_SBX_DEPOSANT_NON_AUTORISE", "Ce service ne dépose pas ce type de pièce");
        }
        String annoncee = d.empreinteAnnoncee() == null ? "" : d.empreinteAnnoncee().trim().toLowerCase(java.util.Locale.ROOT);
        if (!SHA256.matcher(annoncee).matches()) {
            throw DocumentException.invalide("DOC_SBX_EMPREINTE_INVALIDE", "Empreinte SHA-256 annoncée absente ou mal formée");
        }
        String code = d.codeParticipant() == null ? "" : d.codeParticipant().trim();
        if (!CODE_PARTICIPANT.matcher(code).matches()) {
            throw DocumentException.invalide("DOC_SBX_INSTITUTION_INVALIDE", "Code participant absent ou mal formé");
        }
        byte[] contenu = d.fichier() == null ? null : d.fichier().contenu();
        ValidationDepot.verifierContenu(contenu, type.tailleMax());
        String reference = ValidationDepot.reference(d.reference());
        Instant maintenant = horloge.instant();

        int numeros = extracteur.extraire(contenu, ValidationDepot.typeMime(d.fichier().typeMime(), contenu)).map(ControleNumerosCarte::compter).orElse(0);
        String calculee = EmpreinteSha256.de(contenu);
        if (numeros > 0) {
            return quarantaine(type, code, reference, calculee, contenu.length, service, numeros, maintenant);
        }
        if (!calculee.equals(annoncee)) {
            // Pièce non probante : rien n'est conservé ; le service déposant réessaie.
            LOG.error("ALERTE_EXPLOITATION pièce {} de {} : empreinte divergente (annoncée {}, calculée {}), service {}", type.code(), code, annoncee, calculee, service);
            throw DocumentException.invalide(
                "DOC_SBX_EMPREINTE_DIVERGENTE",
                "L'empreinte calculée diffère de l'empreinte annoncée : la pièce n'est pas probante",
                Map.of("annoncee", annoncee, "calculee", calculee)
            );
        }
        var existante = pieces.scellee(code, type.code(), calculee);
        if (existante.isPresent()) {
            PieceSandbox p = existante.orElseThrow();
            LOG.info("Pièce {} déjà conservée ({}), dépôt rejoué par {} sans second dépôt", p.id(), type.code(), service);
            return new Resultat(p.id(), Etat.SCELLEE, p.sha256(), p.conserveeJusquAu(), true);
        }
        var depose = depot.deposer(new Commande(d.fichier(), type.code(), reference), Deposant.pourLeCompteDe("service-account-" + service, service));
        Instant fin = type.conserveeJusquAu(maintenant);
        PieceSandbox p = new PieceSandbox(depose.id(), type.code(), code, reference, depose.sha256(), depose.taille(), Etat.SCELLEE, maintenant, fin, service, 0);
        pieces.inscrire(p);
        pieces.publier(EVENEMENT_SCELLE, depose.id().toString(), donnees(p, depose.nomFichier()), maintenant);
        return new Resultat(p.id(), Etat.SCELLEE, p.sha256(), fin, false);
    }

    private Resultat quarantaine(TypePieceSandbox type, String code, String reference, String sha256, long taille, String service, int numeros, Instant le) {
        LOG.error("ALERTE_EXPLOITATION pièce {} de {} : {} numéro(s) de carte en clair, règle {}, service {}", type.code(), code, numeros, type.reglePan(), service);
        if (type.reglePan() == TypePieceSandbox.ReglePan.REFUS) {
            throw DocumentException.refuse("DOC_PAN_DETECTED", "La pièce contient un numéro de carte non masqué", Map.of("occurrences", numeros));
        }
        PieceSandbox p = new PieceSandbox(UUID.randomUUID(), type.code(), code, reference, sha256, taille, Etat.QUARANTAINE, le, null, service, numeros);
        pieces.inscrire(p);
        return new Resultat(p.id(), Etat.QUARANTAINE, sha256, null, false);
    }

    /** Données de document.document-sealed.v1 : identifiants, codes, empreinte et dates, jamais le contenu. */
    String donnees(PieceSandbox p, String nomFichier) {
        Map<String, Object> d = new java.util.LinkedHashMap<>();
        d.put("documentId", p.id().toString());
        d.put("typePiece", p.type());
        d.put("codeParticipant", p.codeParticipant());
        d.put("reference", p.reference());
        d.put("sha256", p.sha256());
        d.put("taille", p.taille());
        d.put("nomFichier", nomFichier);
        d.put("serviceDeposant", p.serviceDeposant());
        d.put("scelleeLe", p.deposeeLe().toString());
        d.put("conserveeJusquAu", p.conserveeJusquAu().toString());
        return json.valueToTree(d).toString();
    }
}
