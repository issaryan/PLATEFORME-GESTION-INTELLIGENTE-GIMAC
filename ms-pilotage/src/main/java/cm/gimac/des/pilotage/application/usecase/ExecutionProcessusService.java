package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.application.port.in.Page;
import cm.gimac.des.pilotage.application.port.out.DocumentPort;
import cm.gimac.des.pilotage.application.port.out.DocumentPort.Metadonnees;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.application.port.out.ReauthentificationPort;
import cm.gimac.des.pilotage.domain.activite.EtatTache;
import cm.gimac.des.pilotage.domain.activite.Tache;
import cm.gimac.des.pilotage.domain.event.EtapeActiveePayload;
import cm.gimac.des.pilotage.domain.event.EtapeReaffecteePayload;
import cm.gimac.des.pilotage.domain.event.EtapeSigneePayload;
import cm.gimac.des.pilotage.domain.event.EtapeSoumisePayload;
import cm.gimac.des.pilotage.domain.event.InstanceClosePayload;
import cm.gimac.des.pilotage.domain.event.InstanceLanceePayload;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.processus.ActionProcessus;
import cm.gimac.des.pilotage.domain.processus.Affectation;
import cm.gimac.des.pilotage.domain.processus.Champ;
import cm.gimac.des.pilotage.domain.processus.Depot;
import cm.gimac.des.pilotage.domain.processus.DocumentSigne;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.EtapeInstance;
import cm.gimac.des.pilotage.domain.processus.EtatEtape;
import cm.gimac.des.pilotage.domain.processus.EtatInstance;
import cm.gimac.des.pilotage.domain.processus.EtatProcessus;
import cm.gimac.des.pilotage.domain.processus.HistoriqueProcessus;
import cm.gimac.des.pilotage.domain.processus.InstanceProcessus;
import cm.gimac.des.pilotage.domain.processus.Intervenant;
import cm.gimac.des.pilotage.domain.processus.Livrable;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.SignatureInstance;
import cm.gimac.des.pilotage.domain.processus.SignatureRequise;
import cm.gimac.des.pilotage.domain.service.ControleProcessus;
import cm.gimac.des.pilotage.domain.service.EmpreinteSignature;
import cm.gimac.des.pilotage.domain.service.GrapheProcessus;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import cm.gimac.des.pilotage.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.pilotage.repository.EtapeInstanceRepository;
import cm.gimac.des.pilotage.repository.HistoriqueProcessusRepository;
import cm.gimac.des.pilotage.repository.InstanceProcessusRepository;
import cm.gimac.des.pilotage.repository.ProcessusRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moteur des processus d'unité (D-87 §2) : lancement d'une version publiée, activation des étapes dont tous
 * les prédécesseurs sont terminés (tâche d'origine PROCESSUS : à la personne résolue, ou à prendre par les
 * porteurs du profil), dépôts des livrables (MS-DOCUMENT, empreinte SHA-256), champs, fin, signatures dans
 * l'ordre (FIDO2 vérifiée par MS-IAM, finalité SIGNATURE_PROCESSUS), validation ou renvoi, clôture ;
 * réaffectation, suspension, reprise, annulation par le chef de l'unité ; départ d'une personne. La tâche d'une
 * étape se termine avec l'étape. Historique en ajout seul ; événements pilotage.process-* par l'outbox.
 */
@Service
@Transactional
public class ExecutionProcessusService implements InstancesProcessusUseCase {

    static final String AGREGAT = ProcessusService.AGREGAT;
    static final String LIEN = "/des/processus/instances/";
    static final String SERVICE_IAM = "iam";

    private final ProcessusRepository processus;
    private final InstanceProcessusRepository instances;
    private final EtapeInstanceRepository etapes;
    private final HistoriqueProcessusRepository historique;
    private final TacheService taches;
    private final Equipes equipes;
    private final AccesProcessus acces;
    private final ResolutionIntervenants resolution;
    private final DocumentPort documents;
    private final ReauthentificationPort reauthentification;
    private final OutboxPublisher outbox;
    private final Clock horloge;

    @SuppressWarnings("java:S107")
    public ExecutionProcessusService(
        ProcessusRepository processus,
        InstanceProcessusRepository instances,
        EtapeInstanceRepository etapes,
        HistoriqueProcessusRepository historique,
        TacheService taches,
        Equipes equipes,
        AccesProcessus acces,
        ResolutionIntervenants resolution,
        DocumentPort documents,
        ReauthentificationPort reauthentification,
        OutboxPublisher outbox,
        Clock horloge
    ) {
        this.processus = processus;
        this.instances = instances;
        this.etapes = etapes;
        this.historique = historique;
        this.taches = taches;
        this.equipes = equipes;
        this.acces = acces;
        this.resolution = resolution;
        this.documents = documents;
        this.reauthentification = reauthentification;
        this.outbox = outbox;
        this.horloge = horloge;
    }

    // ------------------------------------------------------------------ lecture

    @Override
    @Transactional(readOnly = true)
    public Page<InstanceLue> instances(Appelant appelant, String vue, String etat, Integer page, Integer taille) {
        EtatInstance filtre = etat == null || etat.isBlank() ? null : ReglesActivite.valeur("etat", EtatInstance.class, etat, null);
        PageRequest p = PageRequest.of(ReglesActivite.page(page), ReglesActivite.taille(taille));
        org.springframework.data.domain.Page<InstanceProcessus> r;
        switch (vue == null || vue.isBlank() ? "miennes" : vue.strip()) {
            case "miennes" -> r = instances.miennes(appelant.id(), filtre == null ? null : filtre.name(), p);
            case "lancees" -> r = instances.lancees(appelant.id(), filtre, p);
            case "unite" -> {
                boolean admin = acces.administration(appelant);
                List<String> unites = admin ? List.of("-") : acces.unitesLisibles(appelant);
                r = unites.isEmpty() ? org.springframework.data.domain.Page.empty(p) : instances.deUnites(admin, unites, filtre, p);
            }
            default -> throw PilotageException.champ("vue", "Vue miennes, unite ou lancees attendue");
        }
        List<InstanceProcessus> l = r.getContent();
        Map<UUID, List<EtapeInstance>> parInstance = l.isEmpty()
            ? Map.of()
            : etapes.findByInstanceIdInOrderByRangAsc(l.stream().map(InstanceProcessus::getId).toList()).stream().collect(Collectors.groupingBy(EtapeInstance::getInstanceId));
        Map<UUID, Processus> definitions = new HashMap<>();
        l.forEach(i -> definitions.computeIfAbsent(i.getProcessusId(), id -> processus.findById(id).orElse(null)));
        Map<String, String> noms = equipes.noms(l.stream().map(InstanceProcessus::getLanceurId).toList());
        LocalDate jour = ReglesActivite.aujourdhui(horloge);
        List<InstanceLue> lues = l
            .stream()
            .map(i -> new InstanceLue(i, definitions.get(i.getProcessusId()), parInstance.getOrDefault(i.getId(), List.of()), Map.of(), List.of(), noms, Map.of(), null, jour))
            .toList();
        return new Page<>(lues, r.getTotalElements(), p.getPageNumber(), p.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public InstanceLue instance(Appelant appelant, UUID id) {
        InstanceProcessus i = trouver(id);
        List<EtapeInstance> l = etapes.findByInstanceIdOrderByRangAsc(id);
        Map<UUID, Tache> t = tachesDe(l);
        if (!acces.lit(appelant, i.getUnite()) && !participant(appelant, i, l, t)) {
            throw AccesProcessus.interdit("Cette instance n'est lisible que de l'unité et de ses intervenants");
        }
        return lue(appelant, i, definition(i), l, t);
    }

    // ------------------------------------------------------------------ lancement

    @Override
    public InstanceLue lancer(Appelant appelant, UUID processusId, Lancement l) {
        Processus p = processus.findById(processusId).orElseThrow(() -> PilotageException.introuvable("PIL_PROCESSUS_INTROUVABLE", processusId));
        if (!acces.lance(appelant, p.getUnite())) {
            throw AccesProcessus.interdit("Lancer un processus de l'unité " + p.getUnite() + " exige d'en être membre ou PROCESSUS_LANCER");
        }
        if (p.getEtat() != EtatProcessus.PUBLIEE) {
            throw PilotageException.conflit("PIL_PROCESSUS_NON_PUBLIE", "Seule une version publiée se lance", Map.of("etat", p.getEtat().name()));
        }
        if (l == null) {
            throw PilotageException.champ("intitule", "Corps de la requête absent");
        }
        String intitule = ReglesActivite.texteObligatoire("intitule", l.intitule(), SaisieProcessus.LIBELLE_MAX);
        Map<String, String> designations = designations(appelant, p, l.designations());
        Map<String, Map<String, String>> champsInitiaux = champsDeLancement(p, l.champs());
        String nPlus1 = equipes.rattachement(appelant.id()).map(r -> r.responsable() == null ? null : r.responsable().id()).orElse(null);
        Instant le = horloge.instant();
        InstanceProcessus i = new InstanceProcessus(p, intitule, appelant.id(), nPlus1, designations, le);
        instances.save(i);
        List<String> ordre = GrapheProcessus.kahn(p.getEtapes()).ordre();
        List<EtapeInstance> l2 = new ArrayList<>();
        for (int rang = 0; rang < ordre.size(); rang++) {
            EtapeInstance ei = new EtapeInstance(i.getId(), ordre.get(rang), rang);
            Map<String, String> v = champsInitiaux.get(ordre.get(rang));
            if (v != null && !v.isEmpty()) {
                ei.preremplir(v);
            }
            etapes.save(ei);
            l2.add(ei);
        }
        consigner(i, null, appelant.id(), ActionProcessus.LANCEE, "« " + intitule + " », " + p.getCode() + " version " + p.getVersion(), le);
        String chef = resolution.chef(p.getUnite());
        outbox.publish(
            AGREGAT,
            i.getId(),
            InstanceLanceePayload.TYPE,
            new InstanceLanceePayload(i.getId(), p.getId(), p.getCode(), p.getVersion(), p.getUnite(), intitule, appelant.id(), chef == null || chef.equals(appelant.id()) ? List.of() : List.of(chef), lien(i, null), le)
        );
        for (EtapeInstance ei : l2) {
            if (p.etape(ei.getCode()).map(e -> e.predecesseurs().isEmpty()).orElse(false)) {
                activer(i, p, ei, appelant.id(), le);
            }
        }
        return relire(appelant, i);
    }

    /**
     * Désignations au lancement : une par intervenant AU_LANCEMENT (clé CODE_ETAPE, CODE_ETAPE.validateur,
     * CODE_ETAPE.signature.CODE) ; manquante : 422 PIL_PROCESSUS_DESIGNATION_MANQUANTE ; clé inconnue : 400 ;
     * personne non interne active, ou ni porteuse du profil de remplacement dans l'unité, ni dans la portée du
     * lanceur (TACHE_CONFIER), ni le lanceur : 422 PIL_PROCESSUS_DESIGNATION_INVALIDE.
     */
    private Map<String, String> designations(Appelant appelant, Processus p, Map<String, String> saisies) {
        Map<String, Intervenant> attendues = new LinkedHashMap<>();
        for (ControleProcessus.Role r : ControleProcessus.roles(p.getEtapes())) {
            if (r.intervenant() != null && r.intervenant().mode() == ModeIntervenant.AU_LANCEMENT) {
                attendues.put(ResolutionIntervenants.cle(r.etape(), r.element()), r.intervenant());
            }
        }
        Map<String, String> d = saisies == null ? Map.of() : saisies;
        for (String cle : d.keySet()) {
            if (!attendues.containsKey(cle)) {
                throw PilotageException.champ("designations." + cle, "Aucun intervenant n'est désigné au lancement sous cette clé");
            }
        }
        List<String> manquantes = attendues.keySet().stream().filter(c -> d.get(c) == null || d.get(c).isBlank()).toList();
        if (!manquantes.isEmpty()) {
            throw PilotageException.regle("PIL_PROCESSUS_DESIGNATION_MANQUANTE", "Désignez une personne pour chaque intervenant au lancement", Map.of("cles", manquantes));
        }
        Map<String, String> r = new LinkedHashMap<>();
        attendues.forEach((cle, intervenant) -> {
            String id = d.get(cle).strip();
            String unite = intervenant.uniteOu(p.getUnite());
            boolean admissible =
                equipes.interneActif(id) &&
                (id.equals(appelant.id()) ||
                    (intervenant.remplacement() != null && equipes.porteProfilDans(id, unite, intervenant.remplacement())) ||
                    equipes.couvert(appelant, Pouvoir.TACHE_CONFIER, id).isPresent());
            if (!admissible) {
                throw PilotageException.regle(
                    "PIL_PROCESSUS_DESIGNATION_INVALIDE",
                    "Personne désignée inactive, ou ni porteuse du profil de remplacement dans l'unité, ni dans votre portée",
                    Map.of("cle", cle, "personneId", id)
                );
            }
            r.put(cle, id);
        });
        return r;
    }

    /** Valeurs de lancement : champs des étapes initiales, par code de champ ; code inconnu : 400. */
    private static Map<String, Map<String, String>> champsDeLancement(Processus p, Map<String, Object> saisis) {
        Map<String, Map<String, String>> r = new HashMap<>();
        if (saisis == null || saisis.isEmpty()) {
            return r;
        }
        List<Etape> initiales = p.getEtapes().stream().filter(e -> e.predecesseurs().isEmpty()).toList();
        saisis.forEach((code, valeur) -> {
            boolean connu = false;
            for (Etape e : initiales) {
                for (Champ c : e.champs()) {
                    if (c.code().equals(code)) {
                        connu = true;
                        String v = SaisieProcessus.valeurChamp(c, valeur);
                        if (v != null) {
                            r.computeIfAbsent(e.code(), k -> new LinkedHashMap<>()).put(code, v);
                        }
                    }
                }
            }
            if (!connu) {
                throw PilotageException.champ("champs." + code, "Aucune étape initiale n'a ce champ");
            }
        });
        return r;
    }

    // ------------------------------------------------------------------ activation

    /** Activation d'une étape : tâche à la personne résolue, ou à prendre par les porteurs du profil. */
    private void activer(InstanceProcessus i, Processus p, EtapeInstance ei, String acteur, Instant le) {
        Etape def = definitionEtape(p, ei.getCode());
        ResolutionIntervenants.Resolution r = resolution.resoudre(def.intervenant(), p.getUnite(), i.getLanceurId(), i.getNPlus1LanceurId(), i.getDesignations(), def.code());
        Affectation validateur = def.validateur() == null
            ? null
            : resolution.resoudre(def.validateur(), p.getUnite(), i.getLanceurId(), i.getNPlus1LanceurId(), i.getDesignations(), ResolutionIntervenants.cle(def.code(), ControleProcessus.VALIDATEUR)).affectation();
        List<SignatureInstance> signatures = def
            .signatures()
            .stream()
            .sorted(Comparator.comparingInt(SignatureRequise::ordre).thenComparing(SignatureRequise::code))
            .map(s ->
                SignatureInstance.attendue(
                    s,
                    resolution.resoudre(s.signataire(), p.getUnite(), i.getLanceurId(), i.getNPlus1LanceurId(), i.getDesignations(), ResolutionIntervenants.cle(def.code(), "signature." + s.code())).affectation()
                )
            )
            .toList();
        LocalDate echeance = ReglesActivite.aujourdhui(horloge).plusDays(def.delaiJours());
        Affectation a = r.affectation();
        List<String> destinataires = resolution.destinataires(a, null);
        Tache t = taches.creerPourEtape(
            new TacheService.TacheEtape(
                i.getLanceurId(),
                a.personneId(),
                a.unite(),
                a.profil(),
                def.libelle() + " : " + i.getIntitule(),
                def.action(),
                echeance,
                i.getId() + "/" + def.code(),
                lien(i, def.code()),
                a.personneId() == null ? destinataires : List.of()
            ),
            le
        );
        ei.activer(t.getId(), validateur, signatures, echeance, le);
        etapes.save(ei);
        String detail = a.personneId() != null
            ? "Confiée à " + nom(a.personneId())
            : "À prendre par " + (a.profil() == null ? "les membres" : "les porteurs du profil " + a.profil()) + " de l'unité " + a.unite();
        consigner(i, ei.getCode(), acteur, ActionProcessus.ETAPE_ACTIVEE, detail + (r.note() == null ? "" : " (" + r.note() + ")"), le);
        outbox.publish(
            AGREGAT,
            i.getId(),
            EtapeActiveePayload.TYPE,
            new EtapeActiveePayload(
                i.getId(),
                p.getId(),
                p.getCode(),
                i.getIntitule(),
                def.code(),
                def.libelle(),
                t.getId(),
                a.personneId(),
                a.profil(),
                a.unite(),
                echeance.toString(),
                false,
                null,
                destinataires,
                lien(i, def.code()),
                le
            )
        );
        if (r.parti() != null) {
            // Contrôle à l'activation : la personne prévue est partie, le chef de l'unité est averti.
            consigner(i, ei.getCode(), acteur, ActionProcessus.DEPART, r.note(), le);
            String chef = resolution.chef(a.unite());
            publierReaffectation(i, def, "DEPART", List.of("EXECUTANT"), r.parti(), null, a, chef, acteur, null, avec(destinataires, chef), le);
        }
    }

    // ------------------------------------------------------------------ travail sur une étape

    @Override
    public InstanceLue deposer(Appelant appelant, UUID id, String etape, String livrable, String documentId) {
        Contexte c = contexte(appelant, id, etape);
        c.ei().exigerEtat(EtatEtape.ACTIVE);
        exigerExecutant(appelant, c);
        String code = ReglesActivite.texteObligatoire("livrable", livrable, 40);
        Livrable def = c.def().livrables().stream().filter(l -> l.code().equals(code)).findFirst().orElseThrow(() -> PilotageException.introuvable("PIL_PROCESSUS_LIVRABLE_INCONNU", code));
        String doc = ReglesActivite.texteObligatoire("documentId", documentId, 80);
        UUID docId;
        try {
            docId = UUID.fromString(doc);
        } catch (IllegalArgumentException e) {
            throw PilotageException.champ("documentId", "Identifiant de document attendu");
        }
        Metadonnees m = documents
            .metadonnees(docId)
            .filter(d -> DocumentPort.FINALITE_PROCESSUS.equals(d.finalite()))
            .filter(d -> appelant.id().equals(d.deposant()))
            .filter(d -> d.reference() == null || d.reference().equals(id.toString()))
            .orElseThrow(() ->
                PilotageException.regle(
                    "PIL_PROCESSUS_DOCUMENT_INVALIDE",
                    "Document inconnu, ou non déposé par vous pour cette instance (finalité " + DocumentPort.FINALITE_PROCESSUS + ")",
                    Map.of("documentId", doc)
                )
            );
        String type = m.typeMime() == null ? "" : m.typeMime().toLowerCase(Locale.ROOT);
        if (!def.typesPieces().isEmpty() && def.typesPieces().stream().noneMatch(t -> typeAdmis(t, type))) {
            throw PilotageException.regle("PIL_PROCESSUS_TYPE_PIECE", "Type de pièce non admis pour ce livrable", Map.of("typeMime", type, "admis", def.typesPieces()));
        }
        String sha = m.sha256() == null ? null : m.sha256().toLowerCase(Locale.ROOT);
        if (!EmpreinteSignature.valide(sha)) {
            throw PilotageException.regle("PIL_PROCESSUS_DOCUMENT_INVALIDE", "Empreinte SHA-256 du document absente", Map.of("documentId", doc));
        }
        Instant le = horloge.instant();
        c.ei().deposer(new Depot(code, doc, m.nomFichier(), type, m.taille(), sha, appelant.id(), le.toString()));
        enregistrer(c.ei());
        consigner(c.i(), c.ei().getCode(), appelant.id(), ActionProcessus.LIVRABLE_DEPOSE, "Livrable " + code + " : " + m.nomFichier() + " (SHA-256 " + sha.substring(0, 12) + "…)", le);
        return relire(appelant, c.i());
    }

    @Override
    public InstanceLue renseigner(Appelant appelant, UUID id, String etape, Map<String, Object> valeurs) {
        Contexte c = contexte(appelant, id, etape);
        c.ei().exigerEtat(EtatEtape.ACTIVE);
        exigerExecutant(appelant, c);
        if (valeurs == null) {
            throw PilotageException.champ("valeurs", "Valeurs des champs absentes");
        }
        Map<String, String> v = new LinkedHashMap<>();
        valeurs.forEach((code, valeur) -> {
            Champ champ = c.def().champs().stream().filter(x -> x.code().equals(code)).findFirst().orElseThrow(() -> PilotageException.champ("valeurs." + code, "Champ inconnu dans cette étape"));
            v.put(code, SaisieProcessus.valeurChamp(champ, valeur));
        });
        Instant le = horloge.instant();
        c.ei().renseigner(v);
        enregistrer(c.ei());
        consigner(c.i(), c.ei().getCode(), appelant.id(), ActionProcessus.CHAMPS_RENSEIGNES, String.join(", ", v.keySet()), le);
        return relire(appelant, c.i());
    }

    @Override
    public InstanceLue terminer(Appelant appelant, UUID id, String etape) {
        Contexte c = contexte(appelant, id, etape);
        c.ei().exigerEtat(EtatEtape.ACTIVE);
        exigerExecutant(appelant, c);
        Instant le = horloge.instant();
        c.ei().soumettre(c.def(), le);
        enregistrer(c.ei());
        consigner(c.i(), c.ei().getCode(), appelant.id(), ActionProcessus.ETAPE_SOUMISE, "Fin déclarée : " + suite(c.ei()), le);
        if (c.ei().getEtat() == EtatEtape.TERMINEE) {
            apresTerminee(c.i(), c.p(), c.ei(), appelant.id(), le);
        } else {
            publierAttente(c, appelant.id(), le);
        }
        return relire(appelant, c.i());
    }

    @Override
    public InstanceLue signer(Appelant appelant, UUID id, String etape, String signature, String assertion) {
        Contexte c = contexte(appelant, id, etape);
        String code = ReglesActivite.texteObligatoire("signature", signature, 40);
        c.ei().exigerSignable(code);
        SignatureInstance s = c.ei().signature(code);
        if (!resolution.affecte(appelant.id(), s.attendu())) {
            throw PilotageException.interdit("PIL_PROCESSUS_NON_SIGNATAIRE", "Vous n'êtes pas le signataire attendu");
        }
        if (assertion == null || assertion.isBlank()) {
            throw PilotageException.preconditionRequise("PIL_PROCESSUS_REAUTH_REQUISE", "Signature : réauthentification FIDO2 exigée (en-tête X-Reauth-Assertion)");
        }
        if (!reauthentification.verifier(assertion, appelant.id(), ReauthentificationPort.SIGNATURE_PROCESSUS)) {
            throw PilotageException.interdit("PIL_PROCESSUS_REAUTH_REFUSEE", "Réauthentification FIDO2 refusée");
        }
        List<EtapeInstance> toutes = etapes.findByInstanceIdOrderByRangAsc(id);
        List<DocumentSigne> docs = new ArrayList<>();
        for (String l : s.porte()) {
            toutes.stream().map(e -> e.depot(l)).filter(Objects::nonNull).findFirst().ifPresent(d -> docs.add(new DocumentSigne(l, d.documentId(), d.sha256())));
        }
        String empreinte = EmpreinteSignature.calculer(docs);
        Instant le = horloge.instant();
        c.ei().signer(code, appelant.id(), empreinte, docs, le);
        enregistrer(c.ei());
        consigner(c.i(), c.ei().getCode(), appelant.id(), ActionProcessus.SIGNEE, "Signature " + code + " apposée, empreinte SHA-256 " + empreinte, le);
        List<String> suivants = switch (c.ei().getEtat()) {
            case A_SIGNER -> signatairesOuverts(c.ei(), appelant.id());
            case A_VALIDER -> resolution.destinataires(c.ei().getValidateur(), appelant.id());
            default -> executant(c.ei()).filter(x -> !x.equals(appelant.id())).map(List::of).orElse(List.of());
        };
        outbox.publish(
            AGREGAT,
            c.i().getId(),
            EtapeSigneePayload.TYPE,
            new EtapeSigneePayload(
                c.i().getId(),
                c.i().getCode(),
                c.i().getIntitule(),
                c.ei().getCode(),
                c.def().libelle(),
                code,
                appelant.id(),
                empreinte,
                docs.stream().map(d -> new EtapeSigneePayload.Document(d.livrable(), d.documentId(), d.sha256())).toList(),
                c.ei().getEtat().name(),
                suivants,
                lien(c.i(), c.ei().getCode()),
                le
            )
        );
        if (c.ei().getEtat() == EtatEtape.TERMINEE) {
            apresTerminee(c.i(), c.p(), c.ei(), appelant.id(), le);
        }
        return relire(appelant, c.i());
    }

    @Override
    public InstanceLue valider(Appelant appelant, UUID id, String etape, String decision, String motif) {
        Contexte c = contexte(appelant, id, etape);
        c.ei().exigerEtat(EtatEtape.A_VALIDER);
        if (!resolution.affecte(appelant.id(), c.ei().getValidateur())) {
            throw PilotageException.interdit("PIL_PROCESSUS_NON_VALIDATEUR", "Vous n'êtes pas le validateur de cette étape");
        }
        if (executant(c.ei()).filter(appelant.id()::equals).isPresent()) {
            throw PilotageException.interdit("PIL_PROCESSUS_NON_VALIDATEUR", "L'exécutant ne valide pas sa propre étape");
        }
        String d = decision == null ? "" : decision.strip().toUpperCase(Locale.ROOT);
        Instant le = horloge.instant();
        switch (d) {
            case "VALIDER" -> {
                String m = SaisieProcessus.motif(motif, false);
                c.ei().valider(le);
                enregistrer(c.ei());
                consigner(c.i(), c.ei().getCode(), appelant.id(), ActionProcessus.VALIDEE, m, le);
                apresTerminee(c.i(), c.p(), c.ei(), appelant.id(), le);
            }
            case "RENVOYER" -> {
                String m = SaisieProcessus.motif(motif, true);
                c.ei().renvoyer();
                enregistrer(c.ei());
                consigner(c.i(), c.ei().getCode(), appelant.id(), ActionProcessus.RENVOYEE, m, le);
                Optional<Tache> t = taches.tacheEtape(c.ei().getTacheId());
                String executant = t.map(Tache::getResponsableId).orElse(null);
                outbox.publish(
                    AGREGAT,
                    c.i().getId(),
                    EtapeActiveePayload.TYPE,
                    new EtapeActiveePayload(
                        c.i().getId(),
                        c.p().getId(),
                        c.p().getCode(),
                        c.i().getIntitule(),
                        c.ei().getCode(),
                        c.def().libelle(),
                        c.ei().getTacheId(),
                        executant,
                        t.map(Tache::getProfilRequis).orElse(null),
                        t.map(Tache::getUniteCible).orElse(null),
                        c.ei().getEcheance() == null ? null : c.ei().getEcheance().toString(),
                        true,
                        m,
                        executant == null ? List.of() : List.of(executant),
                        lien(c.i(), c.ei().getCode()),
                        le
                    )
                );
            }
            default -> throw PilotageException.champ("decision", "VALIDER ou RENVOYER attendu");
        }
        return relire(appelant, c.i());
    }

    // ------------------------------------------------------------------ pilotage par le chef de l'unité

    @Override
    public InstanceLue reaffecter(Appelant appelant, UUID id, String etape, String personneId, String motif) {
        Contexte c = contexte(appelant, id, etape);
        if (!acces.pilote(appelant, c.i().getUnite())) {
            throw AccesProcessus.interdit("Réaffecter une étape est réservé au chef de l'unité (ou TACHE_CONFIER)");
        }
        if (!c.ei().getEtat().enCours()) {
            c.ei().exigerEtat(EtatEtape.ACTIVE, EtatEtape.A_SIGNER, EtatEtape.A_VALIDER);
        }
        String personne = ReglesActivite.texteObligatoire("personneId", personneId, 80);
        String m = SaisieProcessus.motif(motif, true);
        String unite = c.def().intervenant() == null ? c.p().getUnite() : c.def().intervenant().uniteOu(c.p().getUnite());
        boolean admissible =
            equipes.interneActif(personne) && (equipes.porteProfilDans(personne, unite, null) || equipes.couvert(appelant, Pouvoir.TACHE_CONFIER, personne).isPresent());
        if (!admissible) {
            throw PilotageException.regle("PIL_PROCESSUS_PERSONNE_INVALIDE", "La personne doit être active et membre de l'unité, ou dans votre portée", Map.of("personneId", personne));
        }
        String ancien = executant(c.ei()).orElse(null);
        if (personne.equals(ancien)) {
            throw PilotageException.conflit("PIL_PROCESSUS_DEJA_AFFECTEE", "L'étape est déjà confiée à cette personne", Map.of("personneId", personne));
        }
        Instant le = horloge.instant();
        String detail = "Étape réaffectée " + (ancien == null ? "" : "de " + nom(ancien) + " ") + "à " + nom(personne) + " : " + m;
        taches.confierEtape(c.ei().getTacheId(), personne, appelant.id(), detail, le);
        consigner(c.i(), c.ei().getCode(), appelant.id(), ActionProcessus.REAFFECTEE, detail, le);
        publierReaffectation(c.i(), c.def(), "REAFFECTATION", List.of("EXECUTANT"), ancien, personne, null, null, appelant.id(), m, List.of(personne), le);
        return relire(appelant, c.i());
    }

    @Override
    public InstanceLue suspendre(Appelant appelant, UUID id, String motif) {
        InstanceProcessus i = pilotable(appelant, id);
        String m = SaisieProcessus.motif(motif, true);
        Instant le = horloge.instant();
        i.suspendre(m, le);
        enregistrer(i);
        consigner(i, null, appelant.id(), ActionProcessus.SUSPENDUE, m, le);
        return relire(appelant, i);
    }

    @Override
    public InstanceLue reprendre(Appelant appelant, UUID id, String motif) {
        InstanceProcessus i = pilotable(appelant, id);
        String m = SaisieProcessus.motif(motif, false);
        Instant le = horloge.instant();
        i.reprendre(m, le);
        enregistrer(i);
        consigner(i, null, appelant.id(), ActionProcessus.REPRISE, m, le);
        return relire(appelant, i);
    }

    @Override
    public InstanceLue annuler(Appelant appelant, UUID id, String motif) {
        InstanceProcessus i = pilotable(appelant, id);
        String m = SaisieProcessus.motif(motif, true);
        Instant le = horloge.instant();
        i.annuler(m, le);
        enregistrer(i);
        for (EtapeInstance ei : etapes.findByInstanceIdOrderByRangAsc(id)) {
            if (!ei.getEtat().estFinal()) {
                if (ei.getTacheId() != null) {
                    taches.annulerPourEtape(ei.getTacheId(), appelant.id(), m, le);
                }
                ei.annuler(le);
                etapes.save(ei);
            }
        }
        consigner(i, null, appelant.id(), ActionProcessus.ANNULEE, m, le);
        publierCloture(i, appelant.id(), m, le);
        return relire(appelant, i);
    }

    // ------------------------------------------------------------------ départ d'une personne

    @Override
    public int traiterDepart(String compteId, String acteurId) {
        if (compteId == null || compteId.isBlank()) {
            return 0;
        }
        String acteur = acteurId == null || acteurId.isBlank() ? SERVICE_IAM : acteurId;
        Instant le = horloge.instant();
        Set<UUID> traitees = new LinkedHashSet<>();
        for (Tache t : taches.tachesEtapesOuvertesDe(compteId)) {
            Optional<EtapeInstance> oe = etapes.findByTacheId(t.getId());
            if (oe.isEmpty() || !oe.orElseThrow().getEtat().enCours()) {
                continue;
            }
            EtapeInstance ei = oe.orElseThrow();
            InstanceProcessus i = trouver(ei.getInstanceId());
            if (i.getEtat().estFinal()) {
                continue;
            }
            Processus p = definition(i);
            Etape def = definitionEtape(p, ei.getCode());
            String unite = def.intervenant().uniteOu(p.getUnite());
            String profil = def.intervenant().remplacement();
            List<String> porteurs = resolution.porteurs(unite, profil, compteId);
            String detail = "Départ de " + nom(compteId) + " : étape à prendre par " + (profil == null ? "les membres" : "les porteurs du profil " + profil) + " de l'unité " + unite;
            taches.remettreEtapeAPrendre(t.getId(), unite, profil, acteur, detail, porteurs, le);
            consigner(i, ei.getCode(), acteur, ActionProcessus.DEPART, detail, le);
            String chef = resolution.chef(unite);
            publierReaffectation(i, def, "DEPART", List.of("EXECUTANT"), compteId, null, Affectation.profil(profil, unite), chef, acteur, null, avec(porteurs, chef), le);
            traitees.add(ei.getId());
        }
        for (EtapeInstance ei : etapes.attenduesDe(compteId)) {
            InstanceProcessus i = trouver(ei.getInstanceId());
            if (i.getEtat().estFinal()) {
                continue;
            }
            Processus p = definition(i);
            Etape def = definitionEtape(p, ei.getCode());
            List<String> roles = new ArrayList<>();
            Affectation remplacement = null;
            if (ei.getValidateur() != null && ei.getValidateur().nomme(compteId) && def.validateur() != null) {
                remplacement = Affectation.profil(def.validateur().remplacement(), def.validateur().uniteOu(p.getUnite()));
                ei.affecterValidateur(remplacement);
                roles.add("VALIDATEUR");
            }
            Map<String, SignatureRequise> parCode = def.signatures().stream().collect(Collectors.toMap(SignatureRequise::code, Function.identity(), (a, b) -> a));
            List<String> codes = ei.reaffecterSignatures(compteId, s -> {
                SignatureRequise sr = parCode.get(s.code());
                Intervenant sig = sr == null ? null : sr.signataire();
                return s.reaffectee(Affectation.profil(sig == null ? null : sig.remplacement(), sig == null ? p.getUnite() : sig.uniteOu(p.getUnite())));
            });
            codes.forEach(code -> roles.add("SIGNATURE." + code));
            if (roles.isEmpty()) {
                continue;
            }
            etapes.save(ei);
            String detail = "Départ de " + nom(compteId) + " : " + String.join(", ", roles) + " repris par profil de remplacement";
            consigner(i, ei.getCode(), acteur, ActionProcessus.DEPART, detail, le);
            Affectation cible = remplacement != null ? remplacement : codes.isEmpty() ? null : ei.signature(codes.get(0)).attendu();
            String unite = cible == null ? p.getUnite() : cible.unite();
            String chef = resolution.chef(unite);
            List<String> porteurs = cible == null ? List.of() : resolution.porteurs(cible.unite(), cible.profil(), compteId);
            publierReaffectation(i, def, "DEPART", roles, compteId, null, cible, chef, acteur, null, avec(porteurs, chef), le);
            traitees.add(ei.getId());
        }
        return traitees.size();
    }

    /** Prise ou attribution de la tâche d'une étape par la voie des tâches : historisée dans l'instance. */
    @EventListener
    public void tacheConfiee(TacheProcessusConfiee e) {
        etapes
            .findByTacheId(e.tacheId())
            .ifPresent(ei ->
                consigner(
                    trouver(ei.getInstanceId()),
                    ei.getCode(),
                    e.acteurId(),
                    ActionProcessus.ETAPE_PRISE,
                    (e.prise() ? "Prise par " : "Attribuée à ") + nom(e.responsableId()),
                    horloge.instant()
                )
            );
    }

    // ------------------------------------------------------------------ enchaînement

    /** Étape terminée : sa tâche aussi ; successeurs prêts activés ; toutes terminées : instance close. */
    private void apresTerminee(InstanceProcessus i, Processus p, EtapeInstance ei, String acteur, Instant le) {
        if (ei.getTacheId() != null) {
            taches.terminerPourEtape(ei.getTacheId(), acteur, le);
        }
        consigner(i, ei.getCode(), acteur, ActionProcessus.ETAPE_TERMINEE, null, le);
        List<EtapeInstance> toutes = etapes.findByInstanceIdOrderByRangAsc(i.getId());
        Map<String, EtapeInstance> parCode = toutes.stream().collect(Collectors.toMap(EtapeInstance::getCode, Function.identity()));
        parCode.put(ei.getCode(), ei);
        for (String s : GrapheProcessus.successeurs(p.getEtapes(), ei.getCode())) {
            EtapeInstance suivante = parCode.get(s);
            Etape def = definitionEtape(p, s);
            boolean prete = suivante != null && suivante.getEtat() == EtatEtape.EN_ATTENTE &&
                def.predecesseurs().stream().allMatch(x -> parCode.get(x) != null && parCode.get(x).getEtat() == EtatEtape.TERMINEE);
            if (prete) {
                activer(i, p, suivante, acteur, le);
            }
        }
        if (parCode.values().stream().allMatch(x -> x.getEtat() == EtatEtape.TERMINEE)) {
            i.clore(le);
            enregistrer(i);
            consigner(i, null, acteur, ActionProcessus.CLOSE, "Toutes les étapes sont terminées", le);
            publierCloture(i, acteur, null, le);
        } else {
            i.toucher(le);
            enregistrer(i);
        }
    }

    private void publierAttente(Contexte c, String acteur, Instant le) {
        boolean signatures = c.ei().getEtat() == EtatEtape.A_SIGNER;
        List<String> destinataires = signatures ? signatairesOuverts(c.ei(), acteur) : resolution.destinataires(c.ei().getValidateur(), acteur);
        outbox.publish(
            AGREGAT,
            c.i().getId(),
            EtapeSoumisePayload.TYPE,
            new EtapeSoumisePayload(
                c.i().getId(),
                c.i().getCode(),
                c.i().getIntitule(),
                c.ei().getCode(),
                c.def().libelle(),
                signatures ? "SIGNATURE" : "VALIDATION",
                c.ei().signaturesOuvertes().stream().map(SignatureInstance::code).toList(),
                acteur,
                destinataires,
                lien(c.i(), c.ei().getCode()),
                le
            )
        );
    }

    private List<String> signatairesOuverts(EtapeInstance ei, String sauf) {
        return ei.signaturesOuvertes().stream().flatMap(s -> resolution.destinataires(s.attendu(), sauf).stream()).distinct().toList();
    }

    private void publierCloture(InstanceProcessus i, String acteur, String motif, Instant le) {
        outbox.publish(
            AGREGAT,
            i.getId(),
            InstanceClosePayload.TYPE,
            new InstanceClosePayload(
                i.getId(),
                i.getProcessusId(),
                i.getCode(),
                i.getIntitule(),
                i.getEtat().name(),
                i.getLanceurId(),
                acteur,
                motif,
                i.getLanceurId().equals(acteur) ? List.of() : List.of(i.getLanceurId()),
                lien(i, null),
                le
            )
        );
    }

    @SuppressWarnings("java:S107")
    private void publierReaffectation(
        InstanceProcessus i,
        Etape etape,
        String cause,
        List<String> roles,
        String ancien,
        String executant,
        Affectation cible,
        String chef,
        String acteur,
        String motif,
        List<String> destinataires,
        Instant le
    ) {
        outbox.publish(
            AGREGAT,
            i.getId(),
            EtapeReaffecteePayload.TYPE,
            new EtapeReaffecteePayload(
                i.getId(),
                i.getCode(),
                i.getIntitule(),
                etape.code(),
                etape.libelle(),
                cause,
                roles,
                ancien,
                executant,
                cible == null ? null : cible.profil(),
                cible == null ? null : cible.unite(),
                chef,
                acteur,
                motif,
                destinataires,
                lien(i, etape.code()),
                le
            )
        );
    }

    private static List<String> avec(List<String> l, String autre) {
        if (autre == null || l.contains(autre)) {
            return l;
        }
        return Stream.concat(l.stream(), Stream.of(autre)).toList();
    }

    // ------------------------------------------------------------------ outils

    private record Contexte(InstanceProcessus i, Processus p, EtapeInstance ei, Etape def) {}

    /** Instance en cours (409 sinon), étape connue (404 PIL_PROCESSUS_ETAPE_INCONNUE), lisible par l'appelant. */
    private Contexte contexte(Appelant appelant, UUID id, String etape) {
        InstanceProcessus i = trouver(id);
        EtapeInstance ei = etapes.findByInstanceIdAndCode(id, etape).orElseThrow(() -> PilotageException.introuvable("PIL_PROCESSUS_ETAPE_INCONNUE", etape));
        List<EtapeInstance> l = etapes.findByInstanceIdOrderByRangAsc(id);
        if (!acces.lit(appelant, i.getUnite()) && !participant(appelant, i, l, tachesDe(l))) {
            throw AccesProcessus.interdit("Cette instance n'est lisible que de l'unité et de ses intervenants");
        }
        i.exigerEnCours();
        Processus p = definition(i);
        return new Contexte(i, p, ei, definitionEtape(p, etape));
    }

    private InstanceProcessus pilotable(Appelant appelant, UUID id) {
        InstanceProcessus i = trouver(id);
        if (!acces.pilote(appelant, i.getUnite())) {
            throw AccesProcessus.interdit("Suspendre, reprendre ou annuler une instance est réservé au chef de l'unité (ou TACHE_CONFIER)");
        }
        return i;
    }

    /** Exécutant : responsable de la tâche de l'étape ; tâche à prendre : 403 (la prendre d'abord). */
    private void exigerExecutant(Appelant appelant, Contexte c) {
        Optional<Tache> t = taches.tacheEtape(c.ei().getTacheId());
        if (t.map(Tache::getResponsableId).filter(appelant.id()::equals).isPresent()) {
            return;
        }
        if (t.map(Tache::getEtat).filter(e -> e == EtatTache.A_PRENDRE).isPresent()) {
            throw PilotageException.interdit("PIL_PROCESSUS_NON_INTERVENANT", "L'étape est à prendre : prenez d'abord sa tâche");
        }
        throw PilotageException.interdit("PIL_PROCESSUS_NON_INTERVENANT", "Seul l'exécutant de l'étape agit sur elle");
    }

    private Optional<String> executant(EtapeInstance ei) {
        return taches.tacheEtape(ei.getTacheId()).map(Tache::getResponsableId);
    }

    /** Lanceur, exécutant, validateur ou signataire (attendu ou ayant signé) de l'une des étapes. */
    private boolean participant(Appelant appelant, InstanceProcessus i, List<EtapeInstance> l, Map<UUID, Tache> t) {
        String moi = appelant.id();
        if (moi.equals(i.getLanceurId())) {
            return true;
        }
        for (EtapeInstance ei : l) {
            Tache tache = ei.getTacheId() == null ? null : t.get(ei.getTacheId());
            if (tache != null && moi.equals(tache.getResponsableId())) {
                return true;
            }
            if (ei.getValidateur() != null && ei.getValidateur().nomme(moi)) {
                return true;
            }
            if (ei.getSignatures().stream().anyMatch(s -> (s.attendu() != null && s.attendu().nomme(moi)) || moi.equals(s.signePar()))) {
                return true;
            }
        }
        return false;
    }

    private Map<UUID, Tache> tachesDe(List<EtapeInstance> l) {
        List<UUID> ids = l.stream().map(EtapeInstance::getTacheId).filter(Objects::nonNull).toList();
        Map<UUID, Tache> m = new HashMap<>();
        taches.tachesEtapes(ids).forEach(t -> m.put(t.getId(), t));
        return m;
    }

    private InstanceLue relire(Appelant appelant, InstanceProcessus i) {
        List<EtapeInstance> l = etapes.findByInstanceIdOrderByRangAsc(i.getId());
        return lue(appelant, i, definition(i), l, tachesDe(l));
    }

    private InstanceLue lue(Appelant appelant, InstanceProcessus i, Processus p, List<EtapeInstance> l, Map<UUID, Tache> t) {
        boolean pilote = acces.pilote(appelant, i.getUnite());
        boolean enCours = i.getEtat() == EtatInstance.EN_COURS;
        Map<String, MoiEtape> moi = new LinkedHashMap<>();
        for (EtapeInstance ei : l) {
            Tache tache = ei.getTacheId() == null ? null : t.get(ei.getTacheId());
            String executant = tache == null ? null : tache.getResponsableId();
            boolean executer = enCours && ei.getEtat() == EtatEtape.ACTIVE && appelant.id().equals(executant);
            boolean prendre = enCours && tache != null && tache.getEtat() == EtatTache.A_PRENDRE && equipes.porteProfilDans(appelant.id(), tache.getUniteCible(), tache.getProfilRequis());
            List<String> signer = enCours ? ei.signaturesOuvertes().stream().filter(s -> resolution.affecte(appelant.id(), s.attendu())).map(SignatureInstance::code).toList() : List.of();
            boolean valider = enCours && ei.getEtat() == EtatEtape.A_VALIDER && !appelant.id().equals(executant) && resolution.affecte(appelant.id(), ei.getValidateur());
            moi.put(ei.getCode(), new MoiEtape(executer, prendre, signer, valider, enCours && pilote && ei.getEtat().enCours()));
        }
        DroitsInstance droits = new DroitsInstance(pilote && enCours, pilote && i.getEtat() == EtatInstance.SUSPENDUE, pilote && !i.getEtat().estFinal(), pilote && enCours);
        List<HistoriqueProcessus> h = historique.findByInstanceIdOrderByQuandAsc(i.getId());
        List<String> ids = new ArrayList<>();
        ids.add(i.getLanceurId());
        t.values().stream().map(Tache::getResponsableId).filter(Objects::nonNull).forEach(ids::add);
        for (EtapeInstance ei : l) {
            if (ei.getValidateur() != null && ei.getValidateur().personneId() != null) {
                ids.add(ei.getValidateur().personneId());
            }
            ei.getSignatures().forEach(s -> {
                if (s.attendu() != null && s.attendu().personneId() != null) {
                    ids.add(s.attendu().personneId());
                }
                if (s.signePar() != null) {
                    ids.add(s.signePar());
                }
            });
            ei.getDepots().forEach(d -> ids.add(d.deposePar()));
        }
        h.forEach(x -> ids.add(x.getActeurId()));
        return new InstanceLue(i, p, l, t, h, equipes.noms(ids), moi, droits, ReglesActivite.aujourdhui(horloge));
    }

    private static String suite(EtapeInstance ei) {
        return switch (ei.getEtat()) {
            case A_SIGNER -> "signatures attendues";
            case A_VALIDER -> "validation attendue";
            default -> "étape terminée";
        };
    }

    private static boolean typeAdmis(String admis, String type) {
        if (admis.endsWith("/*")) {
            return type.startsWith(admis.substring(0, admis.length() - 1));
        }
        return admis.equals(type);
    }

    private InstanceProcessus trouver(UUID id) {
        return instances.findById(id).orElseThrow(() -> PilotageException.introuvable("PIL_PROCESSUS_INSTANCE_INTROUVABLE", id));
    }

    private Processus definition(InstanceProcessus i) {
        return processus.findById(i.getProcessusId()).orElseThrow(() -> PilotageException.introuvable("PIL_PROCESSUS_INTROUVABLE", i.getProcessusId()));
    }

    private static Etape definitionEtape(Processus p, String code) {
        return p.etape(code).orElseThrow(() -> PilotageException.introuvable("PIL_PROCESSUS_ETAPE_INCONNUE", code));
    }

    private void enregistrer(EtapeInstance ei) {
        try {
            etapes.saveAndFlush(ei);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw PilotageException.conflit("PIL_PROCESSUS_CONCURRENCE", "L'étape a été modifiée entre-temps", Map.of("etape", ei.getCode()));
        }
    }

    private void enregistrer(InstanceProcessus i) {
        try {
            instances.saveAndFlush(i);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw PilotageException.conflit("PIL_PROCESSUS_CONCURRENCE", "L'instance a été modifiée entre-temps", Map.of("id", String.valueOf(i.getId())));
        }
    }

    private void consigner(InstanceProcessus i, String etape, String acteur, ActionProcessus action, String detail, Instant le) {
        historique.save(new HistoriqueProcessus(i.getProcessusId(), i.getId(), etape, le, acteur, action, detail));
    }

    private String nom(String id) {
        if (id == null) {
            return "personne";
        }
        String n = equipes.noms(List.of(id)).get(id);
        return n == null ? id : n;
    }

    private static String lien(InstanceProcessus i, String etape) {
        return LIEN + i.getId() + (etape == null ? "" : "?etape=" + etape);
    }
}
