package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.application.port.in.ReleveUseCase;
import cm.gimac.des.monitoring.application.port.in.Vues.Attendus;
import cm.gimac.des.monitoring.application.port.in.Vues.Horodatage;
import cm.gimac.des.monitoring.application.port.in.Vues.Personne;
import cm.gimac.des.monitoring.application.port.in.Vues.RapportVue;
import cm.gimac.des.monitoring.application.port.in.Vues.SignatureVue;
import cm.gimac.des.monitoring.application.port.in.Vues.VacationRapport;
import cm.gimac.des.monitoring.application.port.out.DepotDocumentPort;
import cm.gimac.des.monitoring.application.port.out.HorodatagePort;
import cm.gimac.des.monitoring.application.port.out.ReauthentificationPort;
import cm.gimac.des.monitoring.domain.Alert;
import cm.gimac.des.monitoring.domain.ReportSignature;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.ShiftReport;
import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.enumeration.AlertState;
import cm.gimac.des.monitoring.domain.enumeration.LinkState;
import cm.gimac.des.monitoring.domain.enumeration.ReportState;
import cm.gimac.des.monitoring.domain.enumeration.ShiftLogNature;
import cm.gimac.des.monitoring.domain.enumeration.SignatureRole;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.domain.exploitation.Empreintes;
import cm.gimac.des.monitoring.domain.exploitation.JsonCanonique;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.monitoring.infrastructure.persistance.AlertesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.LiaisonsRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.MainCouranteRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.RapportsQuartRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.SignaturesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.VacationsRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relève de quart à double signature et visa, puis scellement immédiat (algorithme 6.46, D-12, RG-17, RG-18,
 * docs/lot1/astreintes-monitoring.md §6) :
 * <ol>
 *   <li>signature du sortant : contenu figé J (sérialisation canonique), H1 = SHA-256(J) ;</li>
 *   <li>contresignature de l'entrant, distinct : H2 = SHA-256(J ∥ σs), transfert de la responsabilité ;</li>
 *   <li>visa du chef de quart, qui n'a pas signé : H3 = SHA-256(J ∥ σs ∥ σe), puis Hf = SHA-256(J ∥ σs ∥ σe ∥ σc),
 *       dépôt dans MS-DOCUMENT, horodatage et état SCELLE.</li>
 * </ol>
 * Chaque acte exige une assertion FIDO2 vérifiée par MS-IAM (finalité SIGNATURE_RELEVE).
 */
@Service
@Transactional
public class ReleveService implements ReleveUseCase {

    private final VacationsRepository vacations;
    private final RapportsQuartRepository rapports;
    private final SignaturesRepository signatures;
    private final AlertesRepository alertes;
    private final LiaisonsRepository liaisons;
    private final MainCouranteRepository entrees;
    private final MainCourante mainCourante;
    private final Quarts quarts;
    private final ReauthentificationPort reauth;
    private final DepotDocumentPort depot;
    private final HorodatagePort horodatage;
    private final OutboxPublisher outbox;

    public ReleveService(
        VacationsRepository vacations,
        RapportsQuartRepository rapports,
        SignaturesRepository signatures,
        AlertesRepository alertes,
        LiaisonsRepository liaisons,
        MainCouranteRepository entrees,
        MainCourante mainCourante,
        Quarts quarts,
        ReauthentificationPort reauth,
        DepotDocumentPort depot,
        HorodatagePort horodatage,
        OutboxPublisher outbox
    ) {
        this.vacations = vacations;
        this.rapports = rapports;
        this.signatures = signatures;
        this.alertes = alertes;
        this.liaisons = liaisons;
        this.entrees = entrees;
        this.mainCourante = mainCourante;
        this.quarts = quarts;
        this.reauth = reauth;
        this.depot = depot;
        this.horodatage = horodatage;
        this.outbox = outbox;
    }

    private Shift vacation(UUID id) {
        return vacations.findById(id).orElseThrow(() -> MonitoringException.introuvable("MON_VACATION_INTROUVABLE", id));
    }

    private ShiftReport lire(UUID id) {
        return rapports.findById(id).orElseThrow(() -> MonitoringException.introuvable("MON_RAPPORT_INTROUVABLE", id));
    }

    @Override
    public RapportVue generer(Appelant appelant, UUID vacationId, String consignes) {
        Shift v = vacation(vacationId);
        if (!Quarts.membre(v, appelant.id())) {
            throw MonitoringException.interdit("MON_SIGNATAIRE_NON_ATTENDU", "Réservé aux membres de la vacation sortante");
        }
        if (rapports.findByShiftId(v.getId()).isPresent()) {
            throw MonitoringException.conflit("MON_RAPPORT_EXISTANT", "Le rapport de cette vacation existe déjà");
        }
        String c = consignes(consignes);
        String j = canonique(v, c);
        ShiftReport r = new ShiftReport()
            .reference("RQ-" + v.getReference().replaceFirst("^VAC-", ""))
            .content(j)
            .instructions(c)
            .state(ReportState.BROUILLON)
            .contentHash(Empreintes.sha256(j))
            .logHeadHash(tete(v))
            .shift(v);
        rapports.save(r);
        return vue(r);
    }

    private static String consignes(String consignes) {
        String c = consignes == null || consignes.isBlank() ? null : consignes.trim();
        if (c != null && c.length() > 4000) {
            throw MonitoringException.invalide("consignes", "Consignes de 4000 caractères au plus");
        }
        return c;
    }

    private String tete(Shift v) {
        return entrees.findFirstByShiftIdOrderByChainIndexDesc(v.getId()).map(e -> e.getHash()).orElse(Empreintes.ORIGINE);
    }

    /** Contenu agrégé de la vacation (algorithme 6.46, étape 1), sérialisé de façon canonique. */
    private String canonique(Shift v, String consignes) {
        Map<String, Object> contenu = new TreeMap<>();
        Map<String, Object> vac = new TreeMap<>();
        vac.put("reference", v.getReference());
        vac.put("date", v.getShiftDate().toString());
        vac.put("plage", v.getSlot().name());
        vac.put("debut", v.getStartsAt().toString());
        vac.put("fin", v.getEndsAt().toString());
        vac.put("operateurs", Quarts.operateurs(v));
        vac.put("chefDeQuart", v.getShiftLeadId() == null ? null : v.getShiftLeadId().toString());
        contenu.put("vacation", vac);
        List<Alert> actives = alertes.activesSur(v.getStartsAt(), v.getEndsAt());
        Map<String, Object> parGravite = new TreeMap<>();
        for (AlertSeverity g : AlertSeverity.values()) {
            parGravite.put(g.name(), actives.stream().filter(a -> a.getSeverity() == g).count());
        }
        contenu.put("alertesParGravite", parGravite);
        List<Map<String, Object>> ouvertes = new ArrayList<>();
        actives
            .stream()
            .filter(a -> a.getState() != AlertState.RESOLUE)
            .sorted((a, b) -> a.getReference().compareTo(b.getReference()))
            .forEach(a -> {
                Map<String, Object> m = new TreeMap<>();
                m.put("reference", a.getReference());
                m.put("gravite", a.getSeverity().name());
                m.put("etat", a.getState().name());
                m.put("liaison", a.getLink().getCode());
                ouvertes.add(m);
            });
        contenu.put("alertesOuvertes", ouvertes);
        contenu.put("alertesHorsDelai", actives.stream().filter(a -> Boolean.TRUE.equals(a.getOverdue())).count());
        Map<String, Object> etats = new TreeMap<>();
        for (LinkState s : LinkState.values()) {
            etats.put(s.name(), 0L);
        }
        for (Object[] row : liaisons.parEtat()) {
            etats.put(((LinkState) row[0]).name(), row[1]);
        }
        contenu.put("liaisons", etats);
        Map<String, Object> mc = new TreeMap<>();
        mc.put("entrees", entrees.countByShiftId(v.getId()));
        mc.put("tete", tete(v));
        contenu.put("mainCourante", mc);
        contenu.put("consignes", consignes);
        return JsonCanonique.ecrire(contenu);
    }

    @Override
    @Transactional(readOnly = true)
    public RapportVue rapportDeVacation(Appelant appelant, UUID vacationId) {
        vacation(vacationId);
        return vue(rapports.findByShiftId(vacationId).orElseThrow(() -> MonitoringException.introuvable("MON_RAPPORT_INTROUVABLE", vacationId)));
    }

    @Override
    @Transactional(readOnly = true)
    public RapportVue rapport(Appelant appelant, UUID id) {
        return vue(lire(id));
    }

    private void verifierAssertion(Appelant appelant, String assertion) {
        if (assertion == null || assertion.isBlank()) {
            throw MonitoringException.preconditionRequise("MON_REAUTH_EXIGEE", "Réauthentification forte exigée (X-Reauth-Assertion)");
        }
        if (!reauth.verifier(assertion, appelant.id(), ReauthentificationPort.SIGNATURE_RELEVE)) {
            throw MonitoringException.interdit("MON_REAUTH_REFUSEE", "Réauthentification refusée");
        }
    }

    private UUID signataire(Appelant appelant) {
        return appelant.uuid().orElseThrow(() -> MonitoringException.interdit("MON_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé"));
    }

    private ReportSignature signature(ShiftReport r, SignatureRole role, UUID signataire, String defi, String assertion, String reserves, Instant le) {
        ReportSignature s = new ReportSignature().role(role).signerId(signataire).challenge(defi).assertion(assertion).signedAt(le).reservations(reserves).report(r);
        return signatures.save(s);
    }

    @Override
    public RapportVue signer(Appelant appelant, UUID id, String consignes, String assertion) {
        ShiftReport r = lire(id);
        if (r.getState() != ReportState.BROUILLON) {
            throw MonitoringException.conflit("MON_ORDRE_SIGNATURES", "Le rapport est déjà signé par le sortant");
        }
        Shift v = r.getShift();
        if (!Quarts.membre(v, appelant.id())) {
            throw MonitoringException.interdit("MON_SIGNATAIRE_NON_ATTENDU", "Réservé aux membres de la vacation sortante");
        }
        UUID moi = signataire(appelant);
        verifierAssertion(appelant, assertion);
        String c = consignes == null ? r.getInstructions() : consignes(consignes);
        String j = canonique(v, c);
        String h1 = Empreintes.sha256(j);
        r.setContent(j);
        r.setInstructions(c);
        r.setContentHash(h1);
        r.setLogHeadHash(tete(v));
        r.setState(ReportState.SIGNE_SORTANT);
        signature(r, SignatureRole.SORTANT, moi, h1, assertion, null, quarts.maintenant());
        List<String> aPrevenir = new ArrayList<>();
        quarts.suivante(v).ifPresent(s -> aPrevenir.addAll(Quarts.membres(s)));
        demande(r, aPrevenir, "RELEVE_A_CONTRESIGNER");
        return vue(r);
    }

    @Override
    public RapportVue contresigner(Appelant appelant, UUID id, String reserves, String assertion) {
        ShiftReport r = lire(id);
        if (r.getState() != ReportState.SIGNE_SORTANT) {
            throw MonitoringException.conflit("MON_ORDRE_SIGNATURES", "La contresignature suit la signature du sortant");
        }
        String res = reserves == null || reserves.isBlank() ? null : reserves.trim();
        if (res != null && res.length() > 500) {
            throw MonitoringException.invalide("reserves", "Réserves de 500 caractères au plus");
        }
        Shift v = r.getShift();
        Shift entrante = quarts.suivante(v).orElse(null);
        if (entrante == null || !Quarts.membre(entrante, appelant.id())) {
            throw MonitoringException.interdit("MON_SIGNATAIRE_NON_ATTENDU", "Réservé aux membres de la vacation entrante");
        }
        List<ReportSignature> faites = signatures.findByReportIdOrderBySignedAtAsc(r.getId());
        UUID moi = signataire(appelant);
        if (faites.stream().anyMatch(s -> s.getSignerId().equals(moi))) {
            throw MonitoringException.regle("MON_SIGNATAIRE_IDENTIQUE", "L'entrant doit être distinct du sortant");
        }
        verifierAssertion(appelant, assertion);
        String sigmaS = assertionDe(faites, SignatureRole.SORTANT);
        String h2 = Empreintes.sha256(r.getContent(), sigmaS);
        Instant maintenant = quarts.maintenant();
        signature(r, SignatureRole.ENTRANT, moi, h2, assertion, res, maintenant);
        r.setState(ReportState.CONTRESIGNE);
        r.setResponsibilityTransferredAt(maintenant);
        if (res != null) {
            mainCourante.ajouter(entrante, moi, ShiftLogNature.RESERVE_ENTRANT, "Réserve à la relève de " + v.getReference() + " : " + res, r.getReference(), null, maintenant);
            demande(r, quarts.porteursChefDes(), "RELEVE_RESERVE");
        }
        List<String> chefs = new ArrayList<>();
        chefsDe(v, entrante).forEach(chefs::add);
        demande(r, chefs, "RELEVE_A_VISER");
        return vue(r);
    }

    private static Set<String> chefsDe(Shift sortante, Shift entrante) {
        Set<String> chefs = new LinkedHashSet<>();
        if (sortante.getShiftLeadId() != null) {
            chefs.add(sortante.getShiftLeadId().toString());
        }
        if (entrante != null && entrante.getShiftLeadId() != null) {
            chefs.add(entrante.getShiftLeadId().toString());
        }
        return chefs;
    }

    private static String assertionDe(List<ReportSignature> faites, SignatureRole role) {
        return faites.stream().filter(s -> s.getRole() == role).map(ReportSignature::getAssertion).findFirst().orElse("");
    }

    @Override
    public RapportVue viser(Appelant appelant, UUID id, String assertion) {
        ShiftReport r = lire(id);
        if (r.getState() != ReportState.CONTRESIGNE) {
            throw MonitoringException.conflit("MON_ORDRE_SIGNATURES", "Le visa suit la contresignature de l'entrant");
        }
        Shift v = r.getShift();
        Shift entrante = quarts.suivante(v).orElse(null);
        if (!chefsDe(v, entrante).contains(appelant.id())) {
            throw MonitoringException.interdit("MON_SIGNATAIRE_NON_ATTENDU", "Réservé au chef de quart de la vacation sortante ou entrante");
        }
        List<ReportSignature> faites = signatures.findByReportIdOrderBySignedAtAsc(r.getId());
        UUID moi = signataire(appelant);
        if (faites.stream().anyMatch(s -> s.getSignerId().equals(moi))) {
            throw MonitoringException.regle("MON_SIGNATAIRE_IDENTIQUE", "Le chef de quart ne vise pas un rapport qu'il a signé");
        }
        verifierAssertion(appelant, assertion);
        String sigmaS = assertionDe(faites, SignatureRole.SORTANT);
        String sigmaE = assertionDe(faites, SignatureRole.ENTRANT);
        String h3 = Empreintes.sha256(r.getContent(), sigmaS, sigmaE);
        Instant maintenant = quarts.maintenant();
        signature(r, SignatureRole.VISA_CHEF_DE_QUART, moi, h3, assertion, null, maintenant);
        r.setState(ReportState.VISE);
        sceller(r, sigmaS, sigmaE, assertion, maintenant);
        return vue(r);
    }

    /** Empreinte finale, dépôt WORM et horodatage (algorithme 6.46, étapes 5 et 6, RG-18). */
    private void sceller(ShiftReport r, String sigmaS, String sigmaE, String sigmaC, Instant maintenant) {
        String hf = Empreintes.sha256(r.getContent(), sigmaS, sigmaE, sigmaC);
        Map<String, Object> document = new TreeMap<>();
        document.put("reference", r.getReference());
        document.put("contenu", JsonCanonique.lire(r.getContent()));
        document.put("empreinteContenu", r.getContentHash());
        document.put("empreinte", hf);
        List<Map<String, Object>> sigs = new ArrayList<>();
        for (ReportSignature s : signatures.findByReportIdOrderBySignedAtAsc(r.getId())) {
            Map<String, Object> m = new TreeMap<>();
            m.put("role", s.getRole().name());
            m.put("signataire", s.getSignerId().toString());
            m.put("le", s.getSignedAt().toString());
            m.put("defi", s.getChallenge());
            m.put("assertion", s.getAssertion());
            m.put("reserves", s.getReservations());
            sigs.add(m);
        }
        document.put("signatures", sigs);
        byte[] octets = JsonCanonique.ecrire(document).getBytes(StandardCharsets.UTF_8);
        String d = sha256(octets);
        String ref = depot.deposer(octets, r.getReference() + ".json", "application/json", d);
        HorodatagePort.Jeton jeton = horodatage.horodater(d);
        r.setDigest(hf);
        r.setDocumentDigest(d);
        r.setDocumentRef(ref);
        r.setTimestampToken(jeton.jeton());
        r.setTimestampTokenContentType("application/timestamp-reply");
        r.setTimestampSerialNumber(jeton.serie().length() > 100 ? jeton.serie().substring(0, 100) : jeton.serie());
        r.setState(ReportState.SCELLE);
        r.setSealedAt(maintenant);
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("rapportId", r.getId());
        evt.put("reference", r.getReference());
        evt.put("vacationId", r.getShift().getId());
        evt.put("empreinte", hf);
        evt.put("empreinteDocument", d);
        evt.put("documentRef", ref);
        evt.put("serieHorodatage", jeton.serie());
        outbox.publish("ShiftReport", r.getId(), "monitoring.shift-report-sealed.v1", evt);
    }

    private static String sha256(byte[] octets) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(octets));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void demande(ShiftReport r, List<String> destinataires, String objet) {
        if (destinataires.isEmpty()) {
            return;
        }
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("destinataires", List.copyOf(new LinkedHashSet<>(destinataires)));
        n.put("niveau", "URGENT");
        n.put("objet", objet);
        n.put("reference", r.getReference());
        n.put("lien", "/des/exploitation/releve?rapport=" + r.getId());
        outbox.publish("ShiftReport", r.getId(), "monitoring.notification-requested.v1", n);
    }

    RapportVue vue(ShiftReport r) {
        Shift v = r.getShift();
        Optional<Shift> entrante = quarts.suivante(v);
        List<ReportSignature> faites = r.getId() == null ? List.of() : signatures.findByReportIdOrderBySignedAtAsc(r.getId());
        Set<String> ids = new HashSet<>(Quarts.membres(v));
        entrante.ifPresent(e -> ids.addAll(Quarts.membres(e)));
        faites.forEach(s -> ids.add(s.getSignerId().toString()));
        Map<String, String> noms = quarts.noms(ids);
        List<SignatureVue> sigs = faites
            .stream()
            .map(s -> new SignatureVue(s.getRole().name(), Quarts.personne(s.getSignerId(), noms), s.getSignedAt(), s.getChallenge(), s.getReservations()))
            .toList();
        List<Personne> sortants = Quarts.membres(v).stream().map(i -> Quarts.personne(i, noms)).toList();
        List<Personne> entrants = entrante.map(e -> Quarts.membres(e).stream().map(i -> Quarts.personne(i, noms)).toList()).orElse(List.of());
        List<Personne> chefs = chefsDe(v, entrante.orElse(null)).stream().map(i -> Quarts.personne(i, noms)).toList();
        return new RapportVue(
            r.getId(),
            r.getReference(),
            new VacationRapport(v.getId(), v.getReference(), v.getSlot().name(), v.getShiftDate()),
            r.getState().name(),
            JsonCanonique.lire(r.getContent()),
            r.getInstructions(),
            r.getContentHash(),
            r.getDigest(),
            r.getDocumentRef(),
            r.getTimestampSerialNumber() == null ? null : new Horodatage(r.getTimestampSerialNumber()),
            sigs,
            r.getResponsibilityTransferredAt(),
            r.getSealedAt(),
            new Attendus(sortants, entrants, chefs)
        );
    }
}
