package cm.gimac.des.certification.application.usecase;

import static cm.gimac.des.certification.application.usecase.DecisionVues.jour;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.application.port.out.ContexteCertificationPort.ContexteCertification;
import cm.gimac.des.certification.application.usecase.DecisionVues.CouvertureVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.ProcesVerbalVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.SignatureVue;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.application.usecase.DecisionVues.EtapeCircuitVue;
import cm.gimac.des.certification.domain.decision.EtapeSignature;
import cm.gimac.des.certification.domain.decision.ProcesVerbalCertification;
import cm.gimac.des.certification.domain.decision.SignatureVerrou;
import cm.gimac.des.certification.domain.enumeration.EtatProcesVerbal;
import cm.gimac.des.certification.domain.enumeration.ObjetSignature;
import cm.gimac.des.certification.domain.event.DecisionEvenements;
import cm.gimac.des.certification.domain.operation.GesteCertification;
import cm.gimac.des.certification.domain.operation.OperationLiaison;
import cm.gimac.des.certification.domain.service.DecisionCanonique;
import cm.gimac.des.certification.domain.service.ProcesVerbalPolicy;
import cm.gimac.des.certification.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.certification.repository.ProcesVerbalCertificationRepository;
import cm.gimac.des.certification.repository.SignatureVerrouRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verrou 1 (D-56, rangs 17 à 19) : procès-verbal de certification. Établi par le conducteur de projet quand
 * la couverture de certification vaut 100 % et que son constat est fait (contexte de MS-HOMOLOGATION ou
 * événement homologation.coverage-established.v1), puis signé selon le circuit de signature configuré (D-94 :
 * signatures de l'opération du parcours, sinon circuit par défaut, côté DES et côté portail), chaque signature avec
 * réauthentification forte puis cachet horodaté (D-35). Signé de tout le circuit, il ouvre le verrou 2 et la
 * liaison passe à CERTIFIEE côté MS-HOMOLOGATION (certification.pv-signed.v1). Chaque geste exige l'opération du
 * parcours qui le porte ouverte et un acteur désigné (D-93 §4, {@link OperationsLiaison}).
 */
@Service
@Transactional
public class ProcesVerbalService {

    static final String AGREGAT = "ProcesVerbal";
    private static final Logger LOG = LoggerFactory.getLogger(ProcesVerbalService.class);

    private final ProcesVerbalCertificationRepository pvs;
    private final SignatureVerrouRepository signatures;
    private final AccesLiaison acces;
    private final ScellementSignatures scellement;
    private final ApprobationSecuriteService approbations;
    private final OutboxPublisher outbox;
    private final Clock clock;
    private final OperationsLiaison operations;
    private final CircuitsSignature circuits;

    public ProcesVerbalService(
        ProcesVerbalCertificationRepository pvs,
        SignatureVerrouRepository signatures,
        AccesLiaison acces,
        ScellementSignatures scellement,
        ApprobationSecuriteService approbations,
        OutboxPublisher outbox,
        Clock clock,
        OperationsLiaison operations,
        CircuitsSignature circuits
    ) {
        this.operations = operations;
        this.circuits = circuits;
        this.pvs = pvs;
        this.signatures = signatures;
        this.acces = acces;
        this.scellement = scellement;
        this.approbations = approbations;
        this.outbox = outbox;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ DES

    @Transactional(readOnly = true)
    public ProcesVerbalVue consulter(UUID liaisonId, Acteur acteur) {
        Optional<ProcesVerbalCertification> pv = pvs.findByLiaisonId(liaisonId);
        if (pv.isPresent() && pv.orElseThrow().estEtabli()) {
            return vue(pv.orElseThrow(), acteur, false);
        }
        ContexteCertification c = acces.contexte(liaisonId);
        return nonEtabli(c, constatee(c, pv), ProcesVerbalPolicy.actionsDes(EtatProcesVerbal.NON_ETABLI, constatee(c, pv), c.couvertureCertification().taux(), acteur.permissions(), false));
    }

    /** Rang 18 : établissement, à 100 % de couverture après le constat ; couverture et identité figées, empreinte. */
    public ProcesVerbalVue etablir(UUID liaisonId, Acteur acteur) {
        ContexteCertification c = acces.contexte(liaisonId);
        Optional<ProcesVerbalCertification> existant = pvs.findByLiaisonId(liaisonId);
        ProcesVerbalCertification pv = existant.orElseGet(() -> new ProcesVerbalCertification(liaisonId));
        ProcesVerbalPolicy.verifierEtablissement(c.couvertureCertification().taux(), constatee(c, existant), pv.getEtat());
        operations.exiger(liaisonId, GesteCertification.ETABLIR_PV, acteur, c::conducteurId);
        Instant le = clock.instant();
        var couverture = new ProcesVerbalCertification.Couverture(
            c.couvertureCertification().taux(),
            c.couvertureCertification().numerateur(),
            c.couvertureCertification().denominateur()
        );
        String empreinte = DecisionCanonique.de("PROCES_VERBAL_CERTIFICATION")
            .avec("liaison", liaisonId)
            .avec("reference", c.reference())
            .avec("institution", c.institutionId())
            .avec("typeProjet", c.typeProjet())
            .avec("couverture", couverture.taux() + "/" + couverture.numerateur() + "/" + couverture.denominateur())
            .avec("services", String.join(",", c.servicesCouverts()))
            .avec("etabliPar", acteur.id())
            .avec("etabliLe", le)
            .empreinte();
        pv.etablir(
            new ProcesVerbalCertification.Identite(c.reference(), c.institutionId(), c.institutionNom(), c.typeProjet(), c.fintech(), c.modele()),
            couverture,
            c.servicesCouverts(),
            acteur.id(),
            acteur.nom(),
            le,
            empreinte
        );
        pv = pvs.save(pv);
        outbox.publish(
            AGREGAT,
            liaisonId,
            DecisionEvenements.PV_ETABLI,
            new DecisionEvenements.ProcesVerbalEtabli(liaisonId, acteur.nom(), acteur.id(), empreinte),
            pv.getInstitutionId()
        );
        return vue(pv, acteur, false);
    }

    /** Signature du DES : la prochaine signature du circuit que l'acteur tient, assertion FIDO2 puis cachet horodaté. */
    public ProcesVerbalVue signerGimac(UUID liaisonId, Acteur acteur, String assertion) {
        ScellementSignatures.exigerAssertion(assertion);
        ProcesVerbalCertification pv = etabli(liaisonId);
        signer(pv, false, acteur, assertion);
        return vue(pv, acteur, false);
    }

    // ------------------------------------------------------------------ Portail

    @Transactional(readOnly = true)
    public ProcesVerbalVue consulterPortail(UUID liaisonId, Acteur acteur) {
        Optional<ProcesVerbalCertification> pv = pvs.findByLiaisonId(liaisonId);
        if (pv.isPresent() && pv.orElseThrow().estEtabli()) {
            AccesLiaison.verifierInstitution(pv.orElseThrow().getInstitutionId(), acteur);
            return vue(pv.orElseThrow(), acteur, true);
        }
        ContexteCertification c = acces.contexte(liaisonId);
        AccesLiaison.verifierInstitution(c.institutionId(), acteur);
        return nonEtabli(c, constatee(c, pv), List.of());
    }

    /** Signature du portail : la prochaine signature du circuit côté participant, assertion FIDO2 puis cachet horodaté. */
    public ProcesVerbalVue signerParticipant(UUID liaisonId, Acteur acteur, String assertion) {
        ScellementSignatures.exigerAssertion(assertion);
        Optional<ProcesVerbalCertification> trouve = pvs.findByLiaisonId(liaisonId).filter(ProcesVerbalCertification::estEtabli);
        if (trouve.isEmpty()) {
            AccesLiaison.verifierInstitution(acces.contexte(liaisonId).institutionId(), acteur);
            throw CertificationException.conflit("CER_PV_NOT_SIGNABLE", "Le procès-verbal n'attend pas votre signature.");
        }
        ProcesVerbalCertification pv = trouve.orElseThrow();
        AccesLiaison.verifierInstitution(pv.getInstitutionId(), acteur);
        signer(pv, true, acteur, assertion);
        return vue(pv, acteur, true);
    }

    // ------------------------------------------------------------------ Événement consommé

    /**
     * homologation.coverage-established.v1 : le constat de la couverture est fait (rang 17) ; le procès-verbal
     * devient établissable (ETABLIR à 100 %, sinon ETABLIR_BLOQUE). Sans effet une fois le procès-verbal établi.
     */
    public void enregistrerCouvertureEtablie(UUID liaisonId, Integer taux, List<String> services) {
        ProcesVerbalCertification pv = pvs.findByLiaisonId(liaisonId).orElseGet(() -> new ProcesVerbalCertification(liaisonId));
        if (pv.estEtabli()) {
            LOG.debug("Couverture de {} reçue après l'établissement du procès-verbal : ignorée", liaisonId);
            return;
        }
        pv.constaterCouverture(taux, services);
        pvs.save(pv);
    }

    // ------------------------------------------------------------------ Outils

    private ProcesVerbalCertification etabli(UUID liaisonId) {
        return pvs
            .findByLiaisonId(liaisonId)
            .filter(ProcesVerbalCertification::estEtabli)
            .orElseThrow(() -> {
                acces.contexte(liaisonId);
                return CertificationException.conflit("CER_PV_NOT_SIGNABLE", "Le procès-verbal n'est pas établi.");
            });
    }

    private void signer(ProcesVerbalCertification pv, boolean portail, Acteur acteur, String assertion) {
        UUID liaisonId = pv.getLiaisonId();
        if (pv.getEtat() == EtatProcesVerbal.SIGNE) {
            throw CertificationException.conflit("CER_PV_NOT_SIGNABLE", "Le procès-verbal est déjà signé.");
        }
        List<OperationLiaison> ouvertes = operations.ouvertes(liaisonId, GesteCertification.SIGNER_PV);
        List<EtapeSignature> circuit = pv.getCircuit().isEmpty() ? circuits.procesVerbal(ouvertes) : pv.getCircuit();
        Set<String> signees = signeesDe(liaisonId);
        List<EtapeSignature> signables = ProcesVerbalPolicy.signables(circuit, signees, portail);
        if (signables.isEmpty()) {
            throw ProcesVerbalPolicy.nonSignable(circuit, signees, portail);
        }
        Supplier<String> conducteur = conducteur(liaisonId);
        EtapeSignature etape = portail
            ? signables.get(0)
            : signables
                .stream()
                .filter(e -> operations.admetSignataire(ouvertes, e, acteur, conducteur))
                .findFirst()
                .orElseThrow(() -> OperationsLiaison.signataireNonDesigne(signables.get(0)));
        pv.figerCircuit(circuit);
        SignatureVerrou s = scellement.signer(liaisonId, ObjetSignature.PROCES_VERBAL, etape, acteur, true, null, pv.getEmpreinte(), assertion);
        signees.add(etape.code());
        EtatProcesVerbal suivant = ProcesVerbalPolicy.etat(circuit, signees);
        pv.setEtat(suivant);
        pvs.save(pv);
        outbox.publish(
            AGREGAT,
            pv.getLiaisonId(),
            DecisionEvenements.PV_SIGNATURE_ENREGISTREE,
            new DecisionEvenements.SignatureProcesVerbalEnregistree(pv.getLiaisonId(), s.getFonction(), s.getSigneLe(), suivant.name(), s.getEtape()),
            pv.getInstitutionId()
        );
        if (suivant == EtatProcesVerbal.SIGNE) {
            List<DecisionEvenements.SignatureResume> resume = signaturesDe(pv.getLiaisonId())
                .stream()
                .sorted(Comparator.comparing(SignatureVerrou::getSigneLe))
                .map(x -> new DecisionEvenements.SignatureResume(x.getFonction(), x.getSignataireNom(), x.getSigneLe(), x.getEtape()))
                .toList();
            outbox.publish(
                AGREGAT,
                pv.getLiaisonId(),
                DecisionEvenements.PV_SIGNE,
                new DecisionEvenements.ProcesVerbalSigne(pv.getLiaisonId(), resume, pv.getEmpreinte()),
                pv.getInstitutionId()
            );
            approbations.ouvrir(pv);
        }
    }

    private List<SignatureVerrou> signaturesDe(UUID liaisonId) {
        return signatures.findByLiaisonIdAndObjet(liaisonId, ObjetSignature.PROCES_VERBAL);
    }

    private Set<String> signeesDe(UUID liaisonId) {
        return signaturesDe(liaisonId).stream().map(SignatureVerrou::getEtape).collect(Collectors.toCollection(HashSet::new));
    }

    /** Conducteur de la liaison, lu dans MS-HOMOLOGATION seulement si le contrôle en a besoin (une fois). */
    private Supplier<String> conducteur(UUID liaisonId) {
        String[] lu = new String[1];
        boolean[] fait = new boolean[1];
        return () -> {
            if (!fait[0]) {
                lu[0] = acces.contexte(liaisonId).conducteurId();
                fait[0] = true;
            }
            return lu[0];
        };
    }

    /** Circuit affiché : figé, sinon celui que donneraient les opérations ouvertes connues. */
    private List<EtapeSignature> circuitDe(ProcesVerbalCertification pv, List<OperationLiaison> ouvertes) {
        return pv.getCircuit().isEmpty() ? circuits.procesVerbal(ouvertes) : pv.getCircuit();
    }

    /** Opérations ouvertes de la signature, sans refus (lecture) : vide si aucune ou si le geste est refusé. */
    private List<OperationLiaison> ouvertesSansRefus(UUID liaisonId) {
        try {
            return operations.ouvertes(liaisonId, GesteCertification.SIGNER_PV);
        } catch (CertificationException e) {
            return null;
        }
    }

    private static boolean constatee(ContexteCertification c, Optional<ProcesVerbalCertification> pv) {
        return c.couvertureConstatee() || pv.map(ProcesVerbalCertification::isCouvertureConstatee).orElse(false);
    }

    private static ProcesVerbalVue nonEtabli(ContexteCertification c, boolean constatee, List<String> actions) {
        var cv = c.couvertureCertification();
        return new ProcesVerbalVue(
            c.liaisonId(),
            EtatProcesVerbal.NON_ETABLI.name(),
            CouvertureVue.de(cv.taux(), cv.numerateur(), cv.denominateur()),
            c.servicesCouverts(),
            null,
            null,
            null,
            null,
            null,
            List.of(),
            actions,
            List.of()
        );
    }

    /** Représentation ; au portail, l'établissement et la signature du GIMAC sont désignés par la fonction. */
    private ProcesVerbalVue vue(ProcesVerbalCertification pv, Acteur acteur, boolean portail) {
        List<OperationLiaison> ouvertes = ouvertesSansRefus(pv.getLiaisonId());
        List<EtapeSignature> circuit = circuitDe(pv, ouvertes == null ? List.of() : ouvertes);
        List<SignatureVerrou> donnees = signaturesDe(pv.getLiaisonId()).stream().sorted(Comparator.comparing(SignatureVerrou::getSigneLe)).toList();
        Set<String> signees = donnees.stream().map(SignatureVerrou::getEtape).collect(Collectors.toCollection(HashSet::new));
        Set<String> codesPortail = circuit.stream().filter(EtapeSignature::portail).map(EtapeSignature::code).collect(Collectors.toSet());
        SignatureVue gimac = null;
        SignatureVue participant = null;
        List<EtapeCircuitVue> etapes = new java.util.ArrayList<>();
        for (EtapeSignature e : circuit) {
            SignatureVerrou s = donnees.stream().filter(x -> e.code().equals(x.getEtape())).findFirst().orElse(null);
            SignatureVue sv = s == null ? null : signatureVue(s, portail && !e.portail());
            etapes.add(new EtapeCircuitVue(e.code(), e.libelle(), e.fonction(), e.ordre(), e.portail(), s == null ? "ATTENDUE" : "SIGNEE", sv));
        }
        for (SignatureVerrou s : donnees) {
            boolean cotePortail = codesPortail.contains(s.getEtape());
            if (cotePortail && participant == null) {
                participant = signatureVue(s, false);
            } else if (!cotePortail && gimac == null) {
                gimac = signatureVue(s, portail);
            }
        }
        boolean signable = ouvertes != null && pv.getEtat() != EtatProcesVerbal.SIGNE;
        List<String> actions;
        if (portail) {
            actions = ProcesVerbalPolicy.actionsPortail(signable && !ProcesVerbalPolicy.signables(circuit, signees, true).isEmpty(), acteur.permissions());
        } else {
            List<EtapeSignature> des = signable ? ProcesVerbalPolicy.signables(circuit, signees, false) : List.of();
            Supplier<String> conducteur = conducteur(pv.getLiaisonId());
            boolean signableDes =
                !des.isEmpty() &&
                acteur.permissions().contains(cm.gimac.des.certification.domain.service.PermissionsDecision.SIGNER_PV_GIMAC) &&
                des.stream().anyMatch(e -> operations.admetSignataire(ouvertes, e, acteur, conducteur));
            actions = ProcesVerbalPolicy.actionsDes(pv.getEtat(), pv.isCouvertureConstatee(), pv.getCouvertureTaux(), acteur.permissions(), signableDes);
        }
        return new ProcesVerbalVue(
            pv.getLiaisonId(),
            pv.getEtat().name(),
            CouvertureVue.de(pv.getCouvertureTaux(), pv.getCouvertureNumerateur(), pv.getCouvertureDenominateur()),
            pv.getServicesCouverts(),
            portail ? "CONDUCTEUR" : pv.getEtabliParNom(),
            jour(pv.getEtabliLe()),
            pv.getEmpreinte(),
            gimac,
            participant,
            List.of(),
            actions,
            etapes
        );
    }

    /** Au portail, une signature du GIMAC est désignée par sa fonction, jamais par le nom de la personne. */
    private static SignatureVue signatureVue(SignatureVerrou s, boolean parFonction) {
        return new SignatureVue(parFonction ? s.getFonction() : s.getSignataireNom(), s.getFonction(), jour(s.getSigneLe()), s.getHorodatage());
    }
}
