package cm.gimac.des.certification.application.usecase;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.decision.EtapeSignature;
import cm.gimac.des.certification.domain.operation.EtatOperation;
import cm.gimac.des.certification.domain.operation.GesteCertification;
import cm.gimac.des.certification.domain.operation.Intervenant;
import cm.gimac.des.certification.domain.operation.OperationLiaison;
import cm.gimac.des.certification.repository.OperationLiaisonRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opérations du parcours vues par MS-CERTIFICATION (D-93 §4). Source de vérité : MS-HOMOLOGATION, par ses événements
 * d'opération (projection locale idempotente, {@link OperationLiaison}) ; aucune route interne ne sert l'état des
 * opérations. Un geste de certification n'est admis que si une opération qui le porte (déclencheur = événement émis au
 * terme du geste) est ouverte, et si l'acteur y est désigné (R ou V), ou est le conducteur de la liaison ; une fonction
 * à affecter laisse le geste à tout porteur de sa permission, comme MS-HOMOLOGATION (D-93 §8.3).
 * <p>Codes d'erreur : 409 {@code CER_OPERATION_INCONNUE} (liaison sans projection, mode REFUSER),
 * {@code CER_OPERATION_ABSENTE} (la version ne porte pas le geste), {@code CER_OPERATION_NON_OUVERTE} ; 403
 * {@code CER_ACTEUR_NON_DESIGNE}, {@code CER_SIGNATAIRE_NON_DESIGNE}. Un refus ne marque pas la transaction de
 * l'appelant à annuler : une lecture peut le rattraper (actions proposées), un geste le laisse remonter.
 */
@Service
@Transactional(noRollbackFor = CertificationException.class)
public class OperationsLiaison {

    private static final Logger LOG = LoggerFactory.getLogger(OperationsLiaison.class);

    private final OperationLiaisonRepository operations;
    private final ParametresOperations parametres;

    public OperationsLiaison(OperationLiaisonRepository operations, ParametresOperations parametres) {
        this.operations = operations;
        this.parametres = parametres;
    }

    /** Opération décrite par homologation.operation-activated.v1 ou -validation-requested.v1. */
    public record OperationRecue(
        UUID liaisonId,
        UUID operationId,
        Instant le,
        OperationLiaison.Description description,
        List<Intervenant> intervenants,
        List<String> institutionsConcernees,
        List<EtapeSignature> signatures
    ) {
        public OperationRecue {
            intervenants = intervenants == null ? List.of() : List.copyOf(intervenants);
            institutionsConcernees = institutionsConcernees == null ? List.of() : List.copyOf(institutionsConcernees);
        }
    }

    // ------------------------------------------------------------------ projection

    /** homologation.operation-activated.v1 : l'opération est ouverte (ouverture, renvoi, reprise). */
    public void activee(OperationRecue o) {
        appliquer(o, EtatOperation.OUVERTE);
    }

    /** homologation.operation-validation-requested.v1 : l'opération attend son validateur. */
    public void enValidation(OperationRecue o) {
        appliquer(o, EtatOperation.EN_VALIDATION);
    }

    /** homologation.operation-signed.v1 : signature D-91 donnée ; l'opération est faite à la dernière. */
    public void signee(UUID liaisonId, UUID operationId, String code, String libelle, boolean close, Instant le) {
        changerEtat(liaisonId, operationId, new OperationLiaison.Description(code, libelle, null, null, null, null), close ? EtatOperation.FAITE : EtatOperation.EN_SIGNATURE, le);
    }

    /** homologation.liaison-milestone-reached.v1 : l'opération qui porte le jalon est faite. */
    public void jalonAtteint(UUID liaisonId, UUID operationId, String code, String libelle, String jalon, Instant le) {
        changerEtat(liaisonId, operationId, new OperationLiaison.Description(code, libelle, null, null, jalon, null), EtatOperation.FAITE, le);
    }

    /**
     * homologation.operation-closed.v1 : l'opération est faite ou sans objet (clôtures forcées comprises) ; son
     * déclencheur et son jalon, s'ils sont publiés, complètent la projection.
     */
    public void cloturee(UUID liaisonId, UUID operationId, String etat, OperationLiaison.Description description, Instant le) {
        EtatOperation e = "SANS_OBJET".equals(etat) ? EtatOperation.SANS_OBJET : EtatOperation.FAITE;
        if (!"SANS_OBJET".equals(etat) && !"FAITE".equals(etat)) {
            LOG.warn("Clôture de l'opération {} à l'état inattendu {} : tenue pour faite", operationId, etat);
        }
        changerEtat(liaisonId, operationId, description, e, le);
    }

    private void appliquer(OperationRecue o, EtatOperation etat) {
        OperationLiaison op = operations.findById(o.operationId()).orElseGet(() -> new OperationLiaison(o.operationId(), o.liaisonId()));
        if (!op.retenir(o.le())) {
            LOG.debug("Événement d'opération {} antérieur au dernier appliqué : ignoré", o.operationId());
            return;
        }
        op.decrire(o.description());
        op.definirDeclencheur(o.description().declencheur());
        op.designer(o.intervenants(), o.institutionsConcernees(), o.signatures());
        op.setEtat(etat);
        operations.save(op);
    }

    private void changerEtat(UUID liaisonId, UUID operationId, OperationLiaison.Description description, EtatOperation etat, Instant le) {
        if (operationId == null) {
            return;
        }
        OperationLiaison op = operations.findById(operationId).orElseGet(() -> new OperationLiaison(operationId, liaisonId));
        if (!op.retenir(le)) {
            LOG.debug("Événement d'opération {} antérieur au dernier appliqué : ignoré", operationId);
            return;
        }
        op.decrire(description);
        op.setEtat(etat);
        operations.save(op);
    }

    // ------------------------------------------------------------------ contrôles des gestes

    /**
     * Opération ouverte qui porte le geste, l'acteur y étant désigné (R, V), conducteur de la liaison ou, au portail,
     * de l'institution qui tient une fonction de l'opération. Vide : liaison inconnue de la projection, admise en mode
     * ADMETTRE (liaison antérieure au contrôle).
     */
    @Transactional(readOnly = true, noRollbackFor = CertificationException.class)
    public Optional<OperationLiaison> exiger(UUID liaisonId, GesteCertification geste, Acteur acteur, Supplier<String> conducteur) {
        List<OperationLiaison> ouvertes = ouvertes(liaisonId, geste);
        if (ouvertes.isEmpty()) {
            return Optional.empty();
        }
        if (acteur.institutionId() != null) {
            return Optional.of(
                ouvertes
                    .stream()
                    .filter(o -> o.concerne(acteur.institutionId()))
                    .findFirst()
                    .orElseThrow(() -> nonDesigne(geste, ouvertes))
            );
        }
        Optional<OperationLiaison> designee = ouvertes.stream().filter(o -> o.designe(acteur.id())).findFirst();
        if (designee.isPresent()) {
            return designee;
        }
        if (estConducteur(acteur, conducteur)) {
            return Optional.of(ouvertes.get(0));
        }
        return Optional.of(ouvertes.stream().filter(OperationLiaison::aAffecter).findFirst().orElseThrow(() -> nonDesigne(geste, ouvertes)));
    }

    /**
     * Opérations ouvertes qui portent le geste, sans contrôle de l'acteur (signatures : le signataire est contrôlé par
     * le circuit). Vide : liaison inconnue de la projection, admise en mode ADMETTRE.
     */
    @Transactional(readOnly = true, noRollbackFor = CertificationException.class)
    public List<OperationLiaison> ouvertes(UUID liaisonId, GesteCertification geste) {
        List<OperationLiaison> toutes = operations.findByLiaisonId(liaisonId);
        if (toutes.isEmpty()) {
            if (parametres.refuserSansProjection()) {
                throw CertificationException.conflit(
                    "CER_OPERATION_INCONNUE",
                    "Aucune opération du parcours de cette liaison n'est connue : le geste ne peut être contrôlé.",
                    Map.of("geste", geste.name())
                );
            }
            LOG.debug("Liaison {} sans projection d'opérations : geste {} admis (liaison antérieure au contrôle D-93)", liaisonId, geste);
            return List.of();
        }
        List<OperationLiaison> portant = toutes.stream().filter(o -> geste.declencheur().equals(o.getDeclencheur())).toList();
        if (portant.isEmpty()) {
            throw CertificationException.conflit(
                "CER_OPERATION_ABSENTE",
                "Le référentiel de cette liaison ne porte aucune opération pour ce geste : " + geste.libelle() + ".",
                Map.of("geste", geste.name(), "declencheur", geste.declencheur())
            );
        }
        List<OperationLiaison> ouvertes = portant.stream().filter(OperationLiaison::estOuverte).toList();
        if (ouvertes.isEmpty()) {
            throw CertificationException.conflit(
                "CER_OPERATION_NON_OUVERTE",
                "L'opération qui porte ce geste n'est pas ouverte : " + geste.libelle() + ".",
                Map.of("geste", geste.name(), "operations", resume(portant))
            );
        }
        return ouvertes;
    }

    /**
     * Signataire admis pour une étape interne du circuit sur l'une des opérations ouvertes : la personne qui tient la
     * fonction sur l'opération, sinon le conducteur ; une fonction que personne ne tient (absente ou à affecter) est
     * laissée au porteur de la permission de la route. Sans opération connue (liaison antérieure) : admis.
     */
    public boolean admetSignataire(List<OperationLiaison> ouvertes, EtapeSignature etape, Acteur acteur, Supplier<String> conducteur) {
        if (ouvertes.isEmpty()) {
            return true;
        }
        List<Intervenant> tenants = ouvertes.stream().flatMap(o -> o.tenantsDe(etape.fonction()).stream()).toList();
        if (tenants.stream().anyMatch(i -> i.tenuPar(acteur.id()))) {
            return true;
        }
        if (tenants.stream().noneMatch(i -> i.compteId() != null)) {
            return true;
        }
        return estConducteur(acteur, conducteur);
    }

    /** 403 CER_SIGNATAIRE_NON_DESIGNE. */
    public static CertificationException signataireNonDesigne(EtapeSignature etape) {
        return CertificationException.interdit(
            "CER_SIGNATAIRE_NON_DESIGNE",
            "Vous ne tenez pas la fonction signataire attendue : " + etape.libelle() + ".",
            Map.of("signature", etape.code(), "fonction", etape.fonction())
        );
    }

    private static boolean estConducteur(Acteur acteur, Supplier<String> conducteur) {
        String c = conducteur == null ? null : conducteur.get();
        return c != null && c.equalsIgnoreCase(acteur.id());
    }

    private static CertificationException nonDesigne(GesteCertification geste, List<OperationLiaison> ouvertes) {
        return CertificationException.interdit(
            "CER_ACTEUR_NON_DESIGNE",
            "Ce geste revient à la personne désignée sur l'opération ou au conducteur de la liaison.",
            Map.of("geste", geste.name(), "operations", resume(ouvertes))
        );
    }

    private static List<Map<String, String>> resume(List<OperationLiaison> ops) {
        return ops
            .stream()
            .map(o -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("code", o.getCode() == null ? "" : o.getCode());
                m.put("etat", o.getEtat().name());
                return m;
            })
            .toList();
    }
}
