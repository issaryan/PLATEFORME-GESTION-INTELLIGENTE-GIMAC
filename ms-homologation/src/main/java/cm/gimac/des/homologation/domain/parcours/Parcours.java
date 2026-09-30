package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat du parcours d'une liaison, étapes 3 à 10 (docs/lot1/parcours-backend.md) : opérations
 * instanciées du référentiel de l'annexe A, graphe des prédécesseurs, gestes (livrable, preuve,
 * validation, qualités, ressources, blocages, constat de la couverture, coordonnées d'essai, fiche de
 * suivi), projection des verrous tenus par MS-CERTIFICATION, et grandeurs calculées (étape courante,
 * statut du CR, SLA, couvertures). Sans Spring : les règles sont celles du serveur de substitution de
 * l'interface ({@code frontend/packages/lot1/src/services/simule/parcours.ts}).
 */
public final class Parcours {

    /** Une échéance est proche à cinq jours ou moins (gimac.homologation.deadlines.warning-days). */
    public static final int JOURS_ECHEANCE_PROCHE = 5;

    static final Pattern CODE_CONTROLE = Pattern.compile("^[0-9A-F]{4,8}$");
    static final Pattern HOTE = Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?$");
    static final Pattern PORT = Pattern.compile("^\\d{2,5}$");

    /** Désigne la personne qui tient une fonction lors de l'instanciation (annuaire, conducteur, correspondant). */
    @FunctionalInterface
    public interface Designation {
        Personne pour(Fonction f);
    }

    /** Ligne de la fiche de suivi renseignée par le participant. */
    public record LigneFiche(String serviceCode, Boolean ouvert, String commentaire) {}

    private final Liaison liaison;
    private final ParcoursLiaison etat;
    private final List<OperationParcours> operations;
    private final List<ServiceParcours> services;
    private final List<BlocageParcours> blocages;
    /** Taux de couverture repris du simulateur par service (table coverage, HOM-12) ; vide si aucune reprise. */
    private Map<String, Integer> mesures = Map.of();

    public Parcours(Liaison liaison, ParcoursLiaison etat, List<OperationParcours> operations, List<ServiceParcours> services, List<BlocageParcours> blocages) {
        this.liaison = Objects.requireNonNull(liaison);
        this.etat = Objects.requireNonNull(etat);
        this.operations = new ArrayList<>(operations);
        this.operations.sort(Comparator.comparingInt(OperationParcours::getOrdre));
        this.services = new ArrayList<>(services);
        this.services.sort(Comparator.comparing(ServiceParcours::getServiceCode));
        this.blocages = new ArrayList<>(blocages);
        this.blocages.sort(Comparator.comparing(BlocageParcours::getDepuis));
    }

    /**
     * Instancie le parcours à l'ouverture de la liaison : rangs 3 et suivants du tronc commun et la seule
     * branche du type de projet (CR §5.2.3), le rang 27 succédant à la dernière opération de la branche ;
     * échéances planifiées depuis le démarrage, qualités initiales par fonction, opérations sans
     * prédécesseur en attente ouvertes.
     */
    public static Parcours instancier(
        Liaison liaison,
        ParcoursLiaison etat,
        List<ReferentielOperation> referentiel,
        List<ServiceParcours> services,
        Designation designation
    ) {
        TypeProjet type = liaison.getTypeProjet();
        List<ReferentielOperation> suite = referentiel
            .stream()
            .filter(o -> o.getTypes().contains(type))
            .filter(o -> o.rangNumerique() < 0 || o.rangNumerique() >= 3)
            .sorted(Comparator.comparingInt(ReferentielOperation::getOrdre))
            .toList();
        String finBranche = suite.stream().filter(o -> o.getSegment() == SegmentParcours.BRANCHE).reduce((a, b) -> b).map(ReferentielOperation::getRang).orElse(null);
        List<OperationParcours> ops = new ArrayList<>();
        int cumul = 0;
        for (int i = 0; i < suite.size(); i++) {
            ReferentielOperation o = suite.get(i);
            List<String> preds = o.getSegment() == SegmentParcours.AVAL && o.getPredecesseurs().isEmpty() && finBranche != null ? List.of(finBranche) : o.getPredecesseurs();
            OperationParcours op = new OperationParcours(liaison.getId(), o, i, preds);
            if ("15".equals(o.getRang()) || "16".equals(o.getRang())) {
                op.setPlateforme(type == TypeProjet.PAIEMENT_MOBILE ? PlateformeExecution.SIMULATEUR : null);
            }
            cumul += o.getDelaiJours();
            op.setEcheance(etat.getDateDemarrage().plusDays(1L + cumul));
            op.setRaci(raciInitial(o.getPorteurs(), designation));
            ops.add(op);
        }
        Parcours p = new Parcours(liaison, etat, ops, services, List.of());
        p.ouvrirSuivantes();
        p.rafraichir();
        return p;
    }

    /** Qualités initiales : R le premier porteur, C et E les autres, V le conducteur (ou le chef du DES pour le conducteur), I le chef de la DDRM. */
    static Raci raciInitial(List<Fonction> porteurs, Designation d) {
        Personne r = d.pour(porteurs.get(0));
        List<Personne> autres = porteurs.stream().skip(1).map(d::pour).toList();
        Personne v = porteurs.get(0) == Fonction.CONDUCTEUR ? d.pour(Fonction.CHEF_DES) : d.pour(Fonction.CONDUCTEUR);
        return new Raci(r, autres.stream().filter(p -> !p.fonction().externe()).toList(), v, List.of(d.pour(Fonction.CHEF_DDRM)), autres.stream().filter(p -> p.fonction().externe()).toList());
    }

    // ------------------------------------------------------------------ accès

    public Liaison liaison() {
        return liaison;
    }

    public ParcoursLiaison etat() {
        return etat;
    }

    public UUID id() {
        return liaison.getId();
    }

    public List<OperationParcours> operations() {
        return List.copyOf(operations);
    }

    public List<ServiceParcours> services() {
        return List.copyOf(services);
    }

    public List<BlocageParcours> blocages() {
        return List.copyOf(blocages);
    }

    public Map<String, Integer> mesures() {
        return mesures;
    }

    /** Taux repris du simulateur (HOM-12), par service ; la certification d'un service mesuré exige 100 %. */
    public Parcours avecMesures(Map<String, Integer> m) {
        this.mesures = Map.copyOf(m);
        return this;
    }

    public Optional<OperationParcours> operation(String rang) {
        return operations.stream().filter(o -> o.getRang().equals(rang)).findFirst();
    }

    public OperationParcours operation(UUID id) {
        return operations
            .stream()
            .filter(o -> o.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> HomologationException.notFound("HOM_OPERATION_NOT_FOUND", id));
    }

    public Optional<BlocageParcours> blocage(UUID id) {
        return blocages.stream().filter(b -> b.getId().equals(id)).findFirst();
    }

    public List<ServiceParcours> servicesInclus() {
        return services.stream().filter(ServiceParcours::isInclus).toList();
    }

    public boolean operationDansEtat(String rang, EtatOperationParcours e) {
        return operation(rang).map(o -> o.getEtat() == e).orElse(false);
    }

    // ------------------------------------------------------------------ grandeurs calculées

    /** Prédécesseurs instanciés et non faits (les rangs 1 et 2, tenus par MS-PARTICIPANT, sont réputés faits). */
    public List<String> predecesseursNonFaits(OperationParcours op) {
        if (op.faite()) {
            return List.of();
        }
        return op.getPredecesseurs().stream().filter(r -> operation(r).map(x -> !x.faite()).orElse(false)).toList();
    }

    /** Étape de la première opération non faite, dans l'ordre du référentiel ; SUIVI si tout est fait. */
    public EtapeCode etapeCourante() {
        return operations.stream().filter(o -> !o.faite() && o.getEtat() != EtatOperationParcours.SANS_OBJET).findFirst().map(OperationParcours::getEtape).orElse(EtapeCode.SUIVI);
    }

    /** Statut du CR (D-57), déduit des opérations closes. */
    public StatutParcours statutCalcule() {
        if (etat.getStatut() == StatutParcours.SUSPENDUE || etat.getStatut() == StatutParcours.CLOSE_SANS_SUITE) {
            return etat.getStatut();
        }
        if (fait("31")) {
            return StatutParcours.EN_PRODUCTION;
        }
        if ((fait("23") && fait("24") && fait("25")) || etat.getEtatApprobation() == EtatApprobation.APPROUVEE) {
            return StatutParcours.APPROUVEE_SECURITE;
        }
        if (fait("19")) {
            return StatutParcours.CERTIFIEE;
        }
        EtapeCode e = etapeCourante();
        if (e == EtapeCode.TESTS || e == EtapeCode.CERTIFICATION) {
            return StatutParcours.EN_TEST;
        }
        return e == EtapeCode.INTEGRATION ? StatutParcours.EN_INTEGRATION : StatutParcours.CADREE;
    }

    private boolean fait(String rang) {
        return operation(rang).map(OperationParcours::faite).orElse(false);
    }

    public boolean bloquee() {
        return blocages.stream().anyMatch(BlocageParcours::actif);
    }

    /** SLA d'une opération en cours : dépassé après l'échéance, proche à cinq jours ou moins. */
    public static Sla sla(OperationParcours o, LocalDate aujourdhui) {
        if (!o.getEtat().enCours() || o.getEcheance() == null) {
            return null;
        }
        if (o.getEcheance().isBefore(aujourdhui)) {
            return Sla.DEPASSE;
        }
        return ChronoUnit.DAYS.between(aujourdhui, o.getEcheance()) <= JOURS_ECHEANCE_PROCHE ? Sla.ECHEANCE_PROCHE : Sla.DANS_LES_DELAIS;
    }

    public long tachesEnRetard(LocalDate aujourdhui) {
        return operations.stream().filter(o -> sla(o, aujourdhui) == Sla.DEPASSE).count();
    }

    public LocalDate prochaineEcheance() {
        return operations.stream().filter(o -> !o.faite() && o.getEcheance() != null).map(OperationParcours::getEcheance).min(Comparator.naturalOrder()).orElse(null);
    }

    /** Date cible de mise en production : échéance planifiée du rapport consolidé (rang 31). */
    public LocalDate dateCibleProduction() {
        return operation("31").map(OperationParcours::getEcheance).orElse(null);
    }

    /** Couverture de certification : services inclus certifiés (ouverts ou non). */
    public Couverture couvertureCertification() {
        List<ServiceParcours> inclus = servicesInclus();
        return Couverture.de((int) inclus.stream().filter(s -> s.certifie() || s.getOuvert() != null).count(), inclus.size());
    }

    /** Couverture de mise en service : services inclus déclarés ouverts sur la fiche de suivi. */
    public Couverture couvertureMiseEnService() {
        List<ServiceParcours> inclus = servicesInclus();
        return Couverture.de((int) inclus.stream().filter(s -> Boolean.TRUE.equals(s.getOuvert())).count(), inclus.size());
    }

    public List<String> servicesCouverts() {
        return servicesInclus().stream().filter(s -> s.certifie() || s.getOuvert() != null).map(ServiceParcours::getServiceCode).toList();
    }

    /** Attestations exigées du participant et non encore fournies (tâche du portail, après le procès-verbal signé). */
    public List<TypeAttestation> attestationsAFournir() {
        if (etat.getEtatPv() != EtatProcesVerbal.SIGNE || etat.getEtatApprobation() == EtatApprobation.VERIFIE_ARCHIVE || etat.getEtatApprobation() == EtatApprobation.APPROUVEE) {
            return List.of();
        }
        List<TypeAttestation> deposees = etat.getAttestationsDeposees();
        return TypeAttestation.exigees(liaison.getTypeProjet(), etat.isFintech()).stream().filter(t -> !deposees.contains(t)).toList();
    }

    /** Recalcule les champs dénormalisés (statut, étape courante, blocage) et projette l'automate technique (D-57). */
    public void rafraichir() {
        etat.setEtapeCourante(etapeCourante());
        etat.setStatut(statutCalcule());
        etat.setBloquee(bloquee());
        if (liaison.getEtat() != EtatLiaison.GELEE) {
            liaison.setEtat(etatTechnique(etat.getStatut(), fait("17")));
        }
    }

    /** Correspondance du statut du CR vers l'automate technique du plan (HOM-19), conservé et projeté (D-57). */
    static EtatLiaison etatTechnique(StatutParcours s, boolean couvertureConstatee) {
        return switch (s) {
            case CADREE -> EtatLiaison.OUVERTE;
            case EN_INTEGRATION -> EtatLiaison.EN_HOMOLOGATION;
            case EN_TEST -> couvertureConstatee ? EtatLiaison.APTE_CERTIFICATION : EtatLiaison.EN_HOMOLOGATION;
            case CERTIFIEE -> EtatLiaison.CERTIFIEE;
            case APPROUVEE_SECURITE -> EtatLiaison.MISE_EN_SERVICE_AUTORISEE;
            case EN_PRODUCTION, CLOSE_SANS_SUITE -> EtatLiaison.CLOSE;
            case SUSPENDUE -> EtatLiaison.GELEE;
        };
    }

    // ------------------------------------------------------------------ gestes

    /** Clôt une opération, puis ouvre les suivantes dont tous les prédécesseurs sont faits. */
    public void terminer(OperationParcours o, Preuve preuve, LocalDate aujourdhui) {
        o.setEtat(EtatOperationParcours.FAITE);
        o.setFaiteLe(aujourdhui);
        o.setPreuve(preuve);
        if ("15".equals(o.getRang())) {
            servicesInclus().stream().filter(s -> s.getEntreeEssai() == null).forEach(s -> s.setEntreeEssai(aujourdhui));
        }
        if ("16".equals(o.getRang())) {
            certifierServices(aujourdhui);
        }
        ouvrirSuivantes();
        rafraichir();
    }

    /** Clôture imposée par un événement ou un constat (rangs 18 à 26, 32, 33) : sans effet si l'opération est faite. */
    public void forcerCloture(String rang, Preuve preuve, LocalDate aujourdhui) {
        operation(rang).filter(o -> !o.faite()).ifPresent(o -> terminer(o, preuve, aujourdhui));
    }

    private void ouvrirSuivantes() {
        boolean bloque = bloquee();
        for (OperationParcours x : operations) {
            if (x.getEtat() == EtatOperationParcours.VERROUILLEE && predecesseursNonFaits(x).isEmpty()) {
                x.setEtat(bloque ? EtatOperationParcours.BLOQUEE : EtatOperationParcours.A_FAIRE);
            }
        }
    }

    /**
     * Certification des services à la fin des essais (rang 16) et au constat (rang 17) : un service mesuré
     * par le simulateur est certifié à 100 % ; sans aucune mesure reprise, les essais validés certifient
     * le périmètre inclus.
     */
    void certifierServices(LocalDate aujourdhui) {
        for (ServiceParcours s : servicesInclus()) {
            Integer m = mesures.get(s.getServiceCode());
            boolean certifie = mesures.isEmpty() || (m != null && m >= 100);
            if (certifie && s.getCertifieLe() == null) {
                s.setCertifieLe(aujourdhui);
                if (s.getSortieEssai() == null) {
                    s.setSortieEssai(aujourdhui);
                }
            } else if (!certifie) {
                s.setCertifieLe(null);
            }
        }
    }

    /** Geste attendu sur une opération ouverte : constat (rang 17), livrable (pièces de phase, rangs 7 à 11), sinon preuve. */
    public static String geste(OperationParcours o) {
        if ("17".equals(o.getRang())) {
            return "CONSTATER_COUVERTURE";
        }
        return o.getGrain() == GrainPreuve.PHASE || Set.of("7", "8", "9", "10", "11").contains(o.getRang()) ? "DEPOSER_LIVRABLE" : "DEPOSER_PREUVE";
    }

    /** Livrable (compte rendu, document de paramétrage…) : clôt l'opération ouverte ; jamais sur une clé ni une valeur de référence. */
    public void deposerLivrable(OperationParcours o, Preuve preuve, LocalDate aujourdhui) {
        if (!o.getEtat().ouverte() || "17".equals(o.getRang()) || o.isCle() || o.isValeurReference()) {
            throw HomologationException.conflict("HOM_OPERATION_NOT_OPEN", "Cette opération n'attend pas de livrable.", Map.of("etat", o.getEtat().name(), "rang", o.getRang()));
        }
        terminer(o, preuve, aujourdhui);
    }

    /**
     * Preuve au grain de l'opération (CR §6.4.3, D-59) : une clé ne porte que son code de contrôle (4 à 8
     * hexadécimaux), la cérémonie C1 sa date, ses trois porteurs et leurs trois codes ; un secret ne porte
     * qu'une trace ; une valeur de référence est exigée quand l'opération en attend une. L'opération passe
     * en validation.
     */
    public void deposerPreuve(OperationParcours o, Preuve piece, String valeurReference, String codeControle, Ceremonie ceremonie) {
        if (!o.getEtat().ouverte() || "17".equals(o.getRang())) {
            throw HomologationException.conflict("HOM_OPERATION_NOT_OPEN", "Cette opération n'est pas ouverte.", Map.of("etat", o.getEtat().name(), "rang", o.getRang()));
        }
        String code = codeControle == null || codeControle.isBlank() ? null : codeControle.trim().toUpperCase(java.util.Locale.ROOT);
        Ceremonie c = null;
        if ("C1".equals(o.getRang())) {
            c = ceremonieControlee(ceremonie);
        } else if (o.isCle() && code == null) {
            throw HomologationException.unprocessable("HOM_KEY_CHECK_VALUE_REQUIRED", "Le code de contrôle de la clé est exigé ; aucune valeur de clé n'est admise.");
        }
        if (o.isCle() && code != null && !CODE_CONTROLE.matcher(code).matches()) {
            throw invalide();
        }
        if (o.isValeurReference() && (valeurReference == null || valeurReference.isBlank())) {
            throw HomologationException.unprocessable("HOM_REFERENCE_VALUE_REQUIRED", "Cette preuve doit porter la valeur de référence relevée.");
        }
        Preuve p = piece;
        if (o.isValeurReference()) {
            p = p.avecValeurReference(new ValeurReference("parcours.valeur." + o.getRang(), valeurReference.trim()));
        }
        if (o.isCle()) {
            p = p.avecCle(code, c);
        }
        o.setPreuve(p);
        o.setEtat(EtatOperationParcours.EN_VALIDATION);
    }

    private static Ceremonie ceremonieControlee(Ceremonie c) {
        boolean dateValide = c != null && c.tenueLe() != null && dateValide(c.tenueLe());
        if (!dateValide || c.porteurs().stream().filter(x -> x != null && !x.isBlank()).count() != 3 || c.codesControle().size() != 3) {
            throw HomologationException.unprocessable(
                "HOM_KEY_CEREMONY_INCOMPLETE",
                "La cérémonie exige sa date de tenue, les trois porteurs de composantes et leurs trois codes de contrôle."
            );
        }
        List<String> codes = c.codesControle().stream().map(x -> x == null ? "" : x.trim().toUpperCase(java.util.Locale.ROOT)).toList();
        if (!codes.stream().allMatch(x -> CODE_CONTROLE.matcher(x).matches())) {
            throw invalide();
        }
        return new Ceremonie(c.tenueLe(), c.porteurs().stream().map(String::trim).toList(), codes);
    }

    private static boolean dateValide(String s) {
        try {
            LocalDate.parse(s);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    private static HomologationException invalide() {
        return HomologationException.unprocessable("HOM_KEY_CHECK_VALUE_INVALID", "Un code de contrôle compte de 4 à 8 caractères hexadécimaux.");
    }

    /** Validation par le validateur (V), jamais le responsable (R) de la même opération. */
    public void valider(OperationParcours o, Acteur acteur, LocalDate aujourdhui) {
        if (o.getEtat() != EtatOperationParcours.EN_VALIDATION) {
            throw HomologationException.conflict("HOM_OPERATION_NOT_IN_VALIDATION", "L'opération n'attend pas de validation.", Map.of("etat", o.getEtat().name()));
        }
        if (acteur.est(o.getRaci().r())) {
            throw validateurResponsable();
        }
        terminer(o, o.getPreuve(), aujourdhui);
    }

    private static HomologationException validateurResponsable() {
        return HomologationException.conflict("HOM_VALIDATOR_IS_RESPONSIBLE", "Le validateur doit être distinct du responsable.", Map.of());
    }

    /** Attribution d'une qualité (R, C, V, I, E) : R et V restent distincts. */
    public void attribuer(OperationParcours o, Qualite q, Personne p) {
        Raci r = o.getRaci();
        if ((q == Qualite.V && r.r() != null && p.id().equals(r.r().id())) || (q == Qualite.R && r.v() != null && p.id().equals(r.v().id()))) {
            throw validateurResponsable();
        }
        o.setRaci(r.ajouter(q, p));
    }

    /** Affectation des ressources (rang 6) : chaque personne remplace la fonction qu'elle tient sur les opérations non faites. */
    public void affecterRessources(List<Personne> affectations, Preuve preuve, LocalDate aujourdhui) {
        OperationParcours o6 = operation("6").filter(o -> o.getEtat() == EtatOperationParcours.A_FAIRE).orElseThrow(() ->
            HomologationException.conflict("HOM_STEP_NOT_OPEN", "L'affectation des ressources n'est pas ouverte.", Map.of())
        );
        if (affectations.isEmpty()) {
            throw HomologationException.unprocessable("HOM_RESOURCES_REQUIRED", "Au moins une ressource doit être affectée.");
        }
        Map<Fonction, Personne> ressources = new LinkedHashMap<>(etat.getRessources());
        affectations.forEach(p -> ressources.put(p.fonction(), p));
        etat.setRessources(ressources);
        for (OperationParcours o : operations) {
            if (!o.faite()) {
                Raci r = o.getRaci();
                for (Personne p : affectations) {
                    r = r.remplacer(p.fonction(), p);
                }
                o.setRaci(r);
            }
        }
        terminer(o6, preuve, aujourdhui);
    }

    /** Conducteur désigné ou redésigné : il tient désormais sa fonction sur les opérations non faites. */
    public void designerConducteur(Personne conducteur) {
        etat.setConducteurADesigner(false);
        operations.stream().filter(o -> !o.faite()).forEach(o -> o.setRaci(o.getRaci().remplacer(Fonction.CONDUCTEUR, conducteur)));
    }

    /** Blocage daté (CR annexe B) : les opérations ouvertes sont bloquées jusqu'à la levée. */
    public BlocageParcours declarerBlocage(String motif, String prochaineAction, Personne responsable, LocalDate echeanceLevee, UUID declarePar, LocalDate aujourdhui) {
        BlocageParcours b = new BlocageParcours(id(), motif.trim(), aujourdhui, prochaineAction.trim(), responsable, echeanceLevee, etapeCourante(), declarePar);
        blocages.add(b);
        operations.stream().filter(o -> o.getEtat().ouverte()).forEach(o -> o.setEtat(EtatOperationParcours.BLOQUEE));
        rafraichir();
        return b;
    }

    /** Levée d'un blocage : les opérations bloquées sont rouvertes quand plus aucun blocage n'est actif. */
    public void leverBlocage(BlocageParcours b, String commentaire, LocalDate aujourdhui) {
        if (!b.actif()) {
            throw HomologationException.conflict("HOM_BLOCKAGE_ALREADY_LIFTED", "Ce blocage est déjà levé.", Map.of("leveLe", b.getLeveLe().toString()));
        }
        b.lever(aujourdhui, commentaire);
        if (!bloquee()) {
            operations.stream().filter(o -> o.getEtat() == EtatOperationParcours.BLOQUEE).forEach(o -> o.setEtat(EtatOperationParcours.A_FAIRE));
        }
        rafraichir();
    }

    /** Constat de la couverture (rang 17) : ouvert à la fin des essais ; la couverture constatée est rendue. */
    public Couverture constaterCouverture(Preuve preuve, LocalDate aujourdhui) {
        OperationParcours o17 = operation("17").filter(o -> o.getEtat().ouverte()).orElseThrow(() ->
            HomologationException.conflict("HOM_COVERAGE_NOT_READY", "Les essais ne sont pas terminés : la couverture ne peut pas être constatée.", Map.of())
        );
        certifierServices(aujourdhui);
        etat.constaterCouverture(aujourdhui);
        terminer(o17, preuve, aujourdhui);
        return couvertureCertification();
    }

    /** Coordonnées techniques d'essai communiquées par le participant (rang 12) : hôte et port contrôlés, relisibles. */
    public void communiquerCoordonnees(String adresse, String port, String pointsTerminaison, Preuve piece, LocalDate aujourdhui) {
        String a = adresse == null ? "" : adresse.trim();
        String p = port == null ? "" : port.trim();
        if (!HOTE.matcher(a).matches() || !PORT.matcher(p).matches() || Integer.parseInt(p) > 65535 || (pointsTerminaison != null && pointsTerminaison.length() > 500)) {
            throw HomologationException.unprocessable("HOM_TECHNICAL_DATA_INVALID", "Adresse ou port invalide.");
        }
        OperationParcours o12 = operation("12").filter(o -> o.getEtat() == EtatOperationParcours.A_FAIRE).orElseThrow(() ->
            HomologationException.conflict("HOM_OPERATION_NOT_OPEN", "Les coordonnées techniques ne sont pas attendues à cette étape.", Map.of())
        );
        etat.setPointsTerminaison(pointsTerminaison == null || pointsTerminaison.isBlank() ? null : pointsTerminaison.trim());
        terminer(o12, piece.avecValeurReference(new ValeurReference("parcours.valeur.12", a + ":" + p)), aujourdhui);
    }

    /** Remise de la fiche de suivi des services au participant (étape 10). */
    public void remettreFiche() {
        if (etat.getFicheEtat() != EtatFicheSuivi.A_REMETTRE) {
            throw HomologationException.conflict("HOM_SERVICE_SHEET_ALREADY_SENT", "La fiche de suivi a déjà été remise.", Map.of("etat", etat.getFicheEtat().name()));
        }
        etat.setFicheEtat(EtatFicheSuivi.REMISE);
    }

    /** Fiche renseignée par le participant (rang 32) : chaque service du périmètre, ouvert ou non. */
    public void renseignerFiche(List<LigneFiche> lignes, Preuve preuve, LocalDate aujourdhui) {
        if (etat.getFicheEtat() != EtatFicheSuivi.REMISE) {
            throw HomologationException.conflict("HOM_SERVICE_SHEET_NOT_OPEN", "La fiche de suivi n'est pas ouverte à la saisie.", Map.of("etat", etat.getFicheEtat().name()));
        }
        List<ServiceParcours> inclus = servicesInclus();
        Map<String, LigneFiche> parCode = new HashMap<>();
        lignes.forEach(l -> parCode.put(l.serviceCode(), l));
        if (lignes.size() != inclus.size() || inclus.stream().anyMatch(s -> parCode.get(s.getServiceCode()) == null || parCode.get(s.getServiceCode()).ouvert() == null)) {
            throw HomologationException.unprocessable("HOM_SERVICE_SHEET_INCOMPLETE", "Chaque service du périmètre doit être renseigné.");
        }
        for (ServiceParcours s : inclus) {
            LigneFiche l = parCode.get(s.getServiceCode());
            s.setOuvert(l.ouvert());
            s.setCommentaire(l.commentaire() == null || l.commentaire().isBlank() ? null : l.commentaire().trim());
        }
        etat.setFicheEtat(EtatFicheSuivi.RENSEIGNEE);
        etat.setFicheRenseigneeLe(aujourdhui);
        forcerCloture("32", preuve, aujourdhui);
    }

    /** Constat de mise en service (rang 33), sur la fiche renseignée. */
    public void constaterMiseEnService(Preuve preuve, LocalDate aujourdhui) {
        if (etat.getFicheEtat() != EtatFicheSuivi.RENSEIGNEE) {
            throw HomologationException.conflict("HOM_SERVICE_SHEET_INCOMPLETE", "La fiche de suivi n'est pas encore renseignée par le participant.", Map.of("etat", etat.getFicheEtat().name()));
        }
        etat.setFicheEtat(EtatFicheSuivi.CONSTATEE);
        etat.setFicheConstateeLe(aujourdhui);
        forcerCloture("33", preuve, aujourdhui);
    }

    // ------------------------------------------------------------------ projection des verrous (événements de MS-CERTIFICATION)

    /** certification.pv-established.v1 : rang 18 clos avec le procès-verbal. */
    public void pvEtabli(Preuve preuve, LocalDate aujourdhui) {
        if (etat.getEtatPv() == EtatProcesVerbal.NON_ETABLI) {
            etat.setEtatPv(EtatProcesVerbal.ETABLI);
        }
        forcerCloture("18", preuve, aujourdhui);
    }

    /** certification.pv-signed.v1 : signé des deux parties, rang 19 clos et liaison CERTIFIEE ; une seule signature est projetée. */
    public void pvSigne(boolean gimac, boolean participant, Preuve preuve, LocalDate aujourdhui) {
        EtatProcesVerbal e = etat.getEtatPv();
        boolean g = gimac || e == EtatProcesVerbal.SIGNE_GIMAC || e == EtatProcesVerbal.SIGNE;
        boolean pa = participant || e == EtatProcesVerbal.SIGNE_PARTICIPANT || e == EtatProcesVerbal.SIGNE;
        if (g && pa) {
            etat.setEtatPv(EtatProcesVerbal.SIGNE);
            forcerCloture("18", preuve, aujourdhui);
            forcerCloture("19", preuve, aujourdhui);
        } else if (g || pa) {
            etat.setEtatPv(g ? EtatProcesVerbal.SIGNE_GIMAC : EtatProcesVerbal.SIGNE_PARTICIPANT);
        }
        rafraichir();
    }

    /** certification.change-record-constituted.v1 : rang 20 clos. */
    public void dossierChangementConstitue(Preuve preuve, LocalDate aujourdhui) {
        if (etat.getEtatApprobation() == EtatApprobation.EN_ATTENTE_DOSSIER) {
            etat.setEtatApprobation(EtatApprobation.DOSSIER_CONSTITUE);
        }
        forcerCloture("20", preuve, aujourdhui);
    }

    /** certification.change-record-referenced.v1 : rang 26 clos s'il est ouvert. */
    public void ficheChangementReferencee(String reference, Preuve preuve, LocalDate aujourdhui) {
        etat.setFicheChangementReference(reference);
        operation("26").filter(o -> o.getEtat().ouverte()).ifPresent(o -> terminer(o, preuve, aujourdhui));
    }

    /** certification.attestations-archived.v1 : rangs 21 et 22 clos. */
    public void attestationsArchivees(Preuve preuve, LocalDate aujourdhui) {
        etat.setEtatApprobation(EtatApprobation.VERIFIE_ARCHIVE);
        etat.setAttestationsDeposees(List.of());
        forcerCloture("21", preuve, aujourdhui);
        forcerCloture("22", preuve, aujourdhui);
    }

    /** certification.security-approved.v1 : rangs 23 à 25 clos, liaison APPROUVEE_SECURITE. */
    public void securiteApprouvee(Preuve preuve, LocalDate aujourdhui) {
        etat.setEtatApprobation(EtatApprobation.APPROUVEE);
        etat.setVisasAttendus(List.of());
        for (String r : List.of("23", "24", "25")) {
            forcerCloture(r, preuve, aujourdhui);
        }
        rafraichir();
    }

    /** certification.attestation-deposited.v1 (facultatif) : attestation à vérifier par la sécurité. */
    public void attestationDeposee(TypeAttestation t) {
        List<TypeAttestation> l = new ArrayList<>(etat.getAttestationsDeposees());
        if (!l.contains(t)) {
            l.add(t);
        }
        etat.setAttestationsDeposees(l);
    }

    /** certification.attestation-verified.v1 (facultatif) : l'attestation vérifiée ou rejetée n'est plus à vérifier. */
    public void attestationVerifiee(TypeAttestation t) {
        etat.setAttestationsDeposees(etat.getAttestationsDeposees().stream().filter(x -> x != t).toList());
        if (etat.getEtatApprobation() == EtatApprobation.DOSSIER_CONSTITUE) {
            etat.setEtatApprobation(EtatApprobation.EN_VERIFICATION);
        }
    }

    /** certification.visas-required.v1 et certification.visa-signed.v1 (facultatifs) : visas optionnels attendus. */
    public void visas(List<String> attendus) {
        etat.setVisasAttendus(attendus.stream().distinct().toList());
    }

    public void visaSigne(String nature) {
        etat.setVisasAttendus(etat.getVisasAttendus().stream().filter(v -> !v.equals(nature)).toList());
    }
}
