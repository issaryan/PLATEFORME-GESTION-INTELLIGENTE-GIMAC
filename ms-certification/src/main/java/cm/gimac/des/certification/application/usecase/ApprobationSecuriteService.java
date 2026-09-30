package cm.gimac.des.certification.application.usecase;

import static cm.gimac.des.certification.application.usecase.DecisionVues.jour;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.application.port.out.ContexteCertificationPort.ContexteCertification;
import cm.gimac.des.certification.application.port.out.DocumentPort;
import cm.gimac.des.certification.application.port.out.DocumentPort.DocumentDepose;
import cm.gimac.des.certification.application.port.out.DocumentPort.Fichier;
import cm.gimac.des.certification.application.port.out.FicheChangementPort;
import cm.gimac.des.certification.application.usecase.DecisionVues.ApprobationVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.AttestationVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.FicheVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.PreuveVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.SignatureVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.VisaVue;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.decision.ApprobationSecurite;
import cm.gimac.des.certification.domain.decision.AttestationSecurite;
import cm.gimac.des.certification.domain.decision.ProcesVerbalCertification;
import cm.gimac.des.certification.domain.decision.SignatureVerrou;
import cm.gimac.des.certification.domain.decision.VisaSecurite;
import cm.gimac.des.certification.domain.enumeration.EtatApprobation;
import cm.gimac.des.certification.domain.enumeration.EtatVisa;
import cm.gimac.des.certification.domain.enumeration.NatureVisa;
import cm.gimac.des.certification.domain.enumeration.ObjetSignature;
import cm.gimac.des.certification.domain.enumeration.RoleSignature;
import cm.gimac.des.certification.domain.enumeration.TypeAttestation;
import cm.gimac.des.certification.domain.event.DecisionEvenements;
import cm.gimac.des.certification.domain.service.ApprobationPolicy;
import cm.gimac.des.certification.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.certification.repository.ApprobationSecuriteRepository;
import cm.gimac.des.certification.repository.ProcesVerbalCertificationRepository;
import cm.gimac.des.certification.repository.SignatureVerrouRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verrou 2 (D-56, rangs 20 à 26) : approbation du changement par la sécurité des SI. Dossier de changement
 * (production monétique), fiche de changement référencée et lue sur le dispositif existant, attestations
 * déposées au portail, vérifiées avec rejet motivé puis archivées (sécurité des SI), visas optionnels
 * (conformité, Direction Générale) signés avec réauthentification forte. Chaque acte émet son événement par
 * l'outbox (sujet certification.approbation).
 */
@Service
@Transactional
public class ApprobationSecuriteService {

    static final String AGREGAT = "ApprobationSecurite";
    static final String FINALITE_DOCUMENT = "ATTESTATION_SECURITE";
    private static final byte[] ENTETE_PDF = "%PDF".getBytes(StandardCharsets.US_ASCII);

    private final ApprobationSecuriteRepository approbations;
    private final ProcesVerbalCertificationRepository pvs;
    private final SignatureVerrouRepository signatures;
    private final AccesLiaison acces;
    private final ScellementSignatures scellement;
    private final DocumentPort documents;
    private final FicheChangementPort fiches;
    private final OutboxPublisher outbox;
    private final Clock clock;
    private final Duration fraicheurFiche;
    private final Set<String> visaConformite;
    private final Set<String> visaDg;

    public ApprobationSecuriteService(
        ApprobationSecuriteRepository approbations,
        ProcesVerbalCertificationRepository pvs,
        SignatureVerrouRepository signatures,
        AccesLiaison acces,
        ScellementSignatures scellement,
        DocumentPort documents,
        FicheChangementPort fiches,
        OutboxPublisher outbox,
        Clock clock,
        @Value("${gimac.certification.change-record.freshness:PT15M}") Duration fraicheurFiche,
        @Value("${gimac.certification.visas.conformite:}") Set<String> visaConformite,
        @Value("${gimac.certification.visas.direction-generale:}") Set<String> visaDg
    ) {
        this.approbations = approbations;
        this.pvs = pvs;
        this.signatures = signatures;
        this.acces = acces;
        this.scellement = scellement;
        this.documents = documents;
        this.fiches = fiches;
        this.outbox = outbox;
        this.clock = clock;
        this.fraicheurFiche = fraicheurFiche;
        this.visaConformite = Set.copyOf(visaConformite);
        this.visaDg = Set.copyOf(visaDg);
    }

    /** Visas activés pour ce type de projet (paramétrage, désactivés par défaut : D-56). */
    Set<NatureVisa> visasActifs(String typeProjet) {
        Set<NatureVisa> actifs = EnumSet.noneOf(NatureVisa.class);
        if (typeProjet != null && visaConformite.contains(typeProjet)) {
            actifs.add(NatureVisa.CONFORMITE);
        }
        if (typeProjet != null && visaDg.contains(typeProjet)) {
            actifs.add(NatureVisa.DIRECTION_GENERALE);
        }
        return actifs;
    }

    /** Ouverture du verrou 2 quand le procès-verbal est signé des deux parties (appelée par le verrou 1). */
    public ApprobationSecurite ouvrir(ProcesVerbalCertification pv) {
        return approbations
            .findByLiaisonId(pv.getLiaisonId())
            .orElseGet(() ->
                approbations.save(
                    new ApprobationSecurite(
                        pv.getLiaisonId(),
                        pv.getInstitutionId(),
                        pv.getTypeProjet(),
                        pv.isFintech(),
                        ApprobationPolicy.attestationsExigees(pv.getTypeProjet(), pv.isFintech()),
                        visasActifs(pv.getTypeProjet())
                    )
                )
            );
    }

    // ------------------------------------------------------------------ Lecture (DES)

    @Transactional(readOnly = true)
    public ApprobationVue consulter(UUID liaisonId, Acteur acteur) {
        Optional<ApprobationSecurite> a = approbations.findByLiaisonId(liaisonId);
        if (a.isPresent()) {
            return vue(a.orElseThrow(), ApprobationPolicy.actionsDes(a.orElseThrow(), true, acteur.permissions()), false);
        }
        return vue(provisoire(liaisonId), List.of(), false);
    }

    /** Avant la signature du procès-verbal : approbation en attente, attestations exigées selon l'activité. */
    private ApprobationSecurite provisoire(UUID liaisonId) {
        Optional<ProcesVerbalCertification> pv = pvs.findByLiaisonId(liaisonId).filter(ProcesVerbalCertification::estEtabli);
        if (pv.isPresent()) {
            ProcesVerbalCertification p = pv.orElseThrow();
            return new ApprobationSecurite(
                liaisonId,
                p.getInstitutionId(),
                p.getTypeProjet(),
                p.isFintech(),
                ApprobationPolicy.attestationsExigees(p.getTypeProjet(), p.isFintech()),
                visasActifs(p.getTypeProjet())
            );
        }
        ContexteCertification c = acces.contexte(liaisonId);
        return new ApprobationSecurite(
            liaisonId,
            c.institutionId(),
            c.typeProjet(),
            c.fintech(),
            ApprobationPolicy.attestationsExigees(c.typeProjet(), c.fintech()),
            visasActifs(c.typeProjet())
        );
    }

    // ------------------------------------------------------------------ Actes du DES

    /** Rang 20 : dossier de changement déposé dans MS-DOCUMENT, rattaché par référence et empreinte revérifiée. */
    public ApprobationVue constituerDossier(UUID liaisonId, UUID documentId, String empreinte, String nom, Acteur acteur) {
        ApprobationSecurite a = ouverte(liaisonId);
        ApprobationPolicy.verifierOuverte(a);
        if (a.aDossier()) {
            throw CertificationException.conflit("CER_CHANGE_DOSSIER_ALREADY_CONSTITUTED", "Le dossier de changement est déjà constitué.");
        }
        if (documentId == null || empreinte == null || !documents.empreinteConforme(documentId, empreinte.toLowerCase(Locale.ROOT))) {
            throw CertificationException.irrecevable("CER_DOCUMENT_INVALID", "La pièce déposée est introuvable ou son empreinte diffère.");
        }
        a.constituerDossier(documentId.toString(), nom, empreinte.toLowerCase(Locale.ROOT), clock.instant(), acteur.id(), acteur.nom());
        a = enregistrer(a);
        publier(
            a,
            DecisionEvenements.DOSSIER_CHANGEMENT_CONSTITUE,
            new DecisionEvenements.DossierChangementConstitue(liaisonId, a.getDossierDocumentRef(), a.getDossierNom(), a.getDossierEmpreinte())
        );
        return vueDes(a, acteur);
    }

    /** Rang 26 : référence de la fiche de changement, lue aussitôt sur le dispositif existant. */
    public ApprobationVue referencerFiche(UUID liaisonId, String reference, Acteur acteur) {
        ApprobationPolicy.verifierReferenceFiche(reference);
        ApprobationSecurite a = ouverte(liaisonId);
        ApprobationPolicy.verifierOuverte(a);
        a.referencerFiche(reference, fiches.lire(reference), clock.instant());
        publier(a, DecisionEvenements.FICHE_CHANGEMENT_REFERENCEE, new DecisionEvenements.FicheChangementReferencee(liaisonId, reference));
        tenterApprobation(a);
        return vueDes(enregistrer(a), acteur);
    }

    /**
     * Lecture de la fiche, jamais refaite (D-56) : l'état d'approbation vient du dispositif de gestion des
     * changements ; l'approbation de la sécurité est acquise si le dossier est constitué, les attestations
     * vérifiées et archivées, la fiche approuvée et les visas actifs favorables.
     */
    public ApprobationVue relireFiche(UUID liaisonId, Acteur acteur) {
        ApprobationSecurite a = ouverte(liaisonId);
        if (a.getFicheReference() == null) {
            throw CertificationException.conflit("CER_CHANGE_RECORD_MISSING", "Aucune fiche de changement n'est référencée.");
        }
        if (!a.estApprouvee()) {
            a.lireFiche(fiches.lire(a.getFicheReference()), clock.instant());
            tenterApprobation(a);
        }
        return vueDes(enregistrer(a), acteur);
    }

    /** Rang 23 : vérification d'une attestation, conforme ou rejetée avec motif (la pièce rejetée est écartée). */
    public ApprobationVue verifierAttestation(UUID liaisonId, String type, boolean conforme, String motif, Acteur acteur) {
        ApprobationSecurite a = ouverte(liaisonId);
        AttestationSecurite at = exigee(a, type);
        ApprobationPolicy.verifierOuverte(a);
        ApprobationPolicy.verifierNonArchivee(a);
        if (!at.estFournie()) {
            throw CertificationException.conflit("CER_ATTESTATION_MISSING", "L'attestation n'a pas été fournie par le participant.");
        }
        ApprobationPolicy.verifierMotif(conforme, motif, "Un rejet doit être motivé.");
        if (conforme) {
            at.verifierConforme(clock.instant(), acteur.id(), acteur.nom());
        } else {
            at.rejeter(motif.trim());
        }
        publier(
            a,
            DecisionEvenements.ATTESTATION_VERIFIEE,
            new DecisionEvenements.AttestationVerifiee(liaisonId, at.getType().name(), conforme, conforme ? null : motif.trim())
        );
        return vueDes(enregistrer(a), acteur);
    }

    /** Rang 23 : archivage des attestations, toutes vérifiées ; clôt les rangs 21 et 22 côté MS-HOMOLOGATION. */
    public ApprobationVue archiver(UUID liaisonId, Acteur acteur) {
        ApprobationSecurite a = ouverte(liaisonId);
        ApprobationPolicy.verifierOuverte(a);
        ApprobationPolicy.verifierNonArchivee(a);
        if (!ApprobationPolicy.toutesVerifiees(a)) {
            throw CertificationException.conflit(
                "CER_ATTESTATIONS_INCOMPLETE",
                "Toutes les attestations exigées doivent être vérifiées avant l'archivage."
            );
        }
        a.archiver(clock.instant());
        publier(
            a,
            DecisionEvenements.ATTESTATIONS_ARCHIVEES,
            new DecisionEvenements.AttestationsArchivees(
                liaisonId,
                a.getAttestations().stream().map(x -> new DecisionEvenements.AttestationArchivee(x.getType().name(), x.getDocumentRef())).toList()
            )
        );
        return vueDes(enregistrer(a), acteur);
    }

    /** Visa optionnel (conformité ou Direction Générale), signé après réauthentification forte (D-35), immuable. */
    public ApprobationVue signerVisa(UUID liaisonId, NatureVisa nature, boolean favorable, String motif, Acteur acteur, String assertion) {
        ScellementSignatures.exigerAssertion(assertion);
        ApprobationSecurite a = ouverte(liaisonId);
        VisaSecurite v = a.visa(nature).filter(VisaSecurite::isActif).orElseThrow(() ->
            CertificationException.conflit("CER_VISA_NOT_REQUIRED", "Ce visa n'est pas requis pour ce type de projet.")
        );
        if (v.getEtat() != EtatVisa.ATTENDU) {
            throw CertificationException.conflit("CER_VISA_ALREADY_SIGNED", "Ce visa est déjà signé : une signature est immuable.");
        }
        ApprobationPolicy.verifierMotif(favorable, motif, "Un avis défavorable doit être motivé.");
        ApprobationPolicy.verifierOuverte(a);
        String motifRetenu = motif == null || motif.isBlank() ? null : motif.trim();
        RoleSignature role = nature == NatureVisa.CONFORMITE ? RoleSignature.CONFORMITE : RoleSignature.DIRECTION_GENERALE;
        String empreinteObjet = a.getDossierEmpreinte() != null
            ? a.getDossierEmpreinte()
            : pvs.findByLiaisonId(liaisonId).map(ProcesVerbalCertification::getEmpreinte).orElse(null);
        SignatureVerrou s = scellement.signer(liaisonId, ObjetSignature.VISA, role, acteur, favorable, motifRetenu, empreinteObjet, assertion);
        v.signer(favorable, motifRetenu, s.getId());
        publier(a, DecisionEvenements.VISA_SIGNE, new DecisionEvenements.VisaSigne(liaisonId, nature.name(), favorable));
        return vueDes(enregistrer(a), acteur);
    }

    // ------------------------------------------------------------------ Portail du participant

    @Transactional(readOnly = true)
    public ApprobationVue consulterPortail(UUID liaisonId, Acteur acteur) {
        Optional<ApprobationSecurite> trouvee = approbations.findByLiaisonId(liaisonId);
        if (trouvee.isPresent()) {
            ApprobationSecurite a = trouvee.orElseThrow();
            AccesLiaison.verifierInstitution(a.getInstitutionId(), acteur);
            return vue(a, ApprobationPolicy.actionsPortail(a, true, acteur.permissions()), true);
        }
        ApprobationSecurite a = provisoire(liaisonId);
        AccesLiaison.verifierInstitution(a.getInstitutionId(), acteur);
        return vue(a, List.of(), true);
    }

    /** Rangs 21 et 22 : dépôt d'une attestation PDF, conservée par MS-DOCUMENT pour le compte du correspondant. */
    public ApprobationVue deposerAttestation(UUID liaisonId, String type, Fichier fichier, Acteur acteur) {
        Optional<ApprobationSecurite> trouvee = approbations.findByLiaisonId(liaisonId);
        if (trouvee.isEmpty()) {
            AccesLiaison.verifierInstitution(provisoire(liaisonId).getInstitutionId(), acteur);
            throw pvNonSigne("Les attestations sont attendues une fois le procès-verbal signé des deux parties.");
        }
        ApprobationSecurite a = trouvee.orElseThrow();
        AccesLiaison.verifierInstitution(a.getInstitutionId(), acteur);
        AttestationSecurite at = exigee(a, type);
        if (fichier == null || fichier.nom() == null || !fichier.nom().toLowerCase(Locale.ROOT).endsWith(".pdf") || !estPdf(fichier.octets())) {
            throw CertificationException.irrecevable("CER_ATTESTATION_FORMAT", "L'attestation doit être déposée au format PDF.");
        }
        ApprobationPolicy.verifierOuverte(a);
        ApprobationPolicy.verifierNonArchivee(a);
        if (at.estVerifiee()) {
            throw CertificationException.conflit("CER_ATTESTATION_ALREADY_VERIFIED", "Cette attestation est déjà vérifiée conforme.");
        }
        DocumentDepose depot = documents.deposer(fichier, FINALITE_DOCUMENT, liaisonId + "/" + at.getType().name(), acteur.id());
        at.deposer(depot.id(), fichier.nom(), depot.sha256(), clock.instant(), acteur.nom());
        publier(a, DecisionEvenements.ATTESTATION_DEPOSEE, new DecisionEvenements.AttestationDeposee(liaisonId, at.getType().name(), depot.id()));
        a = enregistrer(a);
        return vue(a, ApprobationPolicy.actionsPortail(a, true, acteur.permissions()), true);
    }

    // ------------------------------------------------------------------ Outils

    private static boolean estPdf(byte[] octets) {
        return octets != null && octets.length >= ENTETE_PDF.length && Arrays.equals(Arrays.copyOf(octets, ENTETE_PDF.length), ENTETE_PDF);
    }

    /** Approbation ouverte (procès-verbal signé des deux parties) ; 404 si la liaison est inconnue, sinon CER_PV_NOT_SIGNED. */
    private ApprobationSecurite ouverte(UUID liaisonId) {
        return approbations
            .findByLiaisonId(liaisonId)
            .orElseThrow(() -> {
                if (pvs.findByLiaisonId(liaisonId).isEmpty()) {
                    acces.contexte(liaisonId);
                }
                return pvNonSigne("Le procès-verbal de certification doit être signé des deux parties.");
            });
    }

    private static CertificationException pvNonSigne(String message) {
        return CertificationException.conflit("CER_PV_NOT_SIGNED", message);
    }

    private static AttestationSecurite exigee(ApprobationSecurite a, String type) {
        Optional<TypeAttestation> t = Arrays.stream(TypeAttestation.values()).filter(x -> x.name().equals(type)).findFirst();
        return t
            .flatMap(a::attestation)
            .orElseThrow(() ->
                CertificationException.irrecevable("CER_ATTESTATION_NOT_REQUIRED", "Cette attestation n'est pas exigée pour cette activité.")
            );
    }

    private void tenterApprobation(ApprobationSecurite a) {
        if (ApprobationPolicy.approuvable(a)) {
            a.approuver(clock.instant());
            publier(a, DecisionEvenements.SECURITE_APPROUVEE, new DecisionEvenements.SecuriteApprouvee(a.getLiaisonId(), a.getApprouveeLe()));
        }
    }

    private ApprobationSecurite enregistrer(ApprobationSecurite a) {
        a.setEtat(ApprobationPolicy.etat(a));
        return approbations.save(a);
    }

    private void publier(ApprobationSecurite a, String type, Object charge) {
        outbox.publish(AGREGAT, a.getLiaisonId(), type, charge, a.getInstitutionId());
    }

    private ApprobationVue vueDes(ApprobationSecurite a, Acteur acteur) {
        return vue(a, ApprobationPolicy.actionsDes(a, true, acteur.permissions()), false);
    }

    /**
     * Représentation de l'interface. Portail : sans nom interne ni visa, les personnes du GIMAC par leur
     * fonction (docs/lot1/parcours-backend.md §4 ; simulateur, portailParcours.approbation).
     */
    ApprobationVue vue(ApprobationSecurite a, List<String> actions, boolean portail) {
        Instant maintenant = clock.instant();
        PreuveVue dossier = a.aDossier()
            ? new PreuveVue(
                a.getDossierDocumentRef(),
                a.getDossierNom(),
                a.getDossierEmpreinte(),
                jour(a.getDossierDeposeLe()),
                portail ? "PRODUCTION_MONETIQUE" : a.getDossierDeposeParNom()
            )
            : null;
        FicheVue fiche = a.getFicheReference() == null
            ? null
            : new FicheVue(
                a.getFicheReference(),
                a.getFicheEtat() == null ? null : a.getFicheEtat().name(),
                jour(a.getFicheLueLe()),
                ApprobationPolicy.ficheARelire(a, maintenant, fraicheurFiche)
            );
        List<AttestationVue> attestations = a
            .getAttestations()
            .stream()
            .map(x ->
                new AttestationVue(
                    x.getType().name(),
                    x.isRequise(),
                    x.estFournie() ? new PreuveVue(x.getDocumentRef(), x.getNom(), x.getEmpreinte(), jour(x.getDeposeLe()), x.getDeposePar()) : null,
                    jour(x.getVerifieeLe()),
                    x.estVerifiee() ? (portail ? "RSSI" : x.getVerifieeParNom()) : null,
                    jour(x.getArchiveeLe()),
                    x.getMotifRejet()
                )
            )
            .toList();
        List<VisaVue> visas = portail ? List.of() : visas(a);
        EtatApprobation etat = ApprobationPolicy.etat(a);
        return new ApprobationVue(
            a.getLiaisonId(),
            etat.name(),
            dossier,
            fiche,
            null,
            attestations,
            a.isFintech(),
            visas,
            jour(a.getApprouveeLe()),
            List.of(),
            actions
        );
    }

    private List<VisaVue> visas(ApprobationSecurite a) {
        Map<UUID, SignatureVerrou> signees = signatures
            .findByLiaisonIdAndObjet(a.getLiaisonId(), ObjetSignature.VISA)
            .stream()
            .collect(Collectors.toMap(SignatureVerrou::getId, Function.identity()));
        return a
            .getVisas()
            .stream()
            .map(v -> {
                SignatureVerrou s = v.getSignatureId() == null ? null : signees.get(v.getSignatureId());
                SignatureVue sv = s == null ? null : new SignatureVue(s.getSignataireNom(), s.getFonction(), jour(s.getSigneLe()), s.getHorodatage());
                return new VisaVue(v.getNature().name(), v.isActif(), v.getEtat().name(), sv, v.getMotif());
            })
            .toList();
    }
}
