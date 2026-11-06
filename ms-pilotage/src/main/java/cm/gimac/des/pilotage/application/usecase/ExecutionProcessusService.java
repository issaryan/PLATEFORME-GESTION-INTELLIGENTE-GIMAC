package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.application.port.in.Page;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase;
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
import cm.gimac.des.pilotage.domain.processus.ReferenceProcessus;
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
            case "miennes" -> r = instances.miennes(appelant.id(), auMoinsUn(equipes.profilsDe(appelant.id())), auMoinsUn(equipes.unitesDe(appelant.id())), filtre == null ? null : filtre.name(), p);
            case "lancees" -> r = instances.lancees(appelant.id(), filtre, p);
            case "unite" -> {
                boolean admin = acces.litToutes(appelant);
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
        // D-98 : actions permises à l'appelant par étape (« Mes étapes » ne garde que celles qui attendent sa décision).
        Map<UUID, Tache> t = tachesDe(parInstance.values().stream().flatMap(List::stream).toList());
        List<InstanceLue> lues = l
            .stream()
            .map(i -> {
                List<EtapeInstance> e = parInstance.getOrDefault(i.getId(), List.of());
                return new InstanceLue(i, definitions.get(i.getProcessusId()), e, Map.of(), List.of(), noms, moiDe(appelant, i, definitions.get(i.getProcessusId()), e, t), null, jour);
            })
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

    @Override
    @Transactional(readOnly = true)
    public DocumentInstance document(Appelant appelant, UUID id, String documentId) {
        InstanceProcessus i = trouver(id);
        List<EtapeInstance> l = etapes.findByInstanceIdOrderByRangAsc(id);
        if (!acces.lit(appelant, i.getUnite()) && !participant(appelant, i, l, tachesDe(l))) {
            throw AccesProcessus.interdit("Cette instance n'est lisible que de l'unité et de ses intervenants");
        }
        String doc = documentId == null ? "" : documentId.strip();
        // Pièces du formulaire de lancement : { documentId, nomFichier, typeMime, taille, sha256 }.
        for (Object v : i.getFormulaire().values()) {
            if (v instanceof Map<?, ?> m && doc.equals(String.valueOf(m.get("documentId")))) {
                return lireDocument(doc, (String) m.get("nomFichier"), (String) m.get("typeMime"), m.get("taille") instanceof Number n ? n.longValue() : -1);
            }
        }
        for (EtapeInstance ei : l) {
            for (Depot d : ei.getDepots()) {
                if (doc.equals(d.documentId())) {
                    return lireDocument(doc, d.nomFichier(), d.typeMime(), d.taille());
                }
            }
        }
        throw PilotageException.introuvable("PIL_PROCESSUS_DOCUMENT_INCONNU", doc);
    }

    private DocumentInstance lireDocument(String id, String nom, String type, long taille) {
        DocumentPort.Contenu c = documents.lire(UUID.fromString(id));
        return new DocumentInstance(nom, type, taille >= 0 ? taille : c.taille(), c.flux());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstanceLue> demandes(Appelant appelant, String etat, String q, Integer page, Integer taille) {
        EtatInstance filtre = etat == null || etat.isBlank() ? null : ReglesActivite.valeur("etat", EtatInstance.class, etat, null);
        String motif = q == null || q.isBlank() ? null : "%" + q.strip().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
        PageRequest p = PageRequest.of(ReglesActivite.page(page), ReglesActivite.taille(taille));
        org.springframework.data.domain.Page<InstanceProcessus> r = instances.demandes(appelant.id(), filtre, motif, p);
        List<InstanceProcessus> l = r.getContent();
        List<EtapeInstance> toutes = l.isEmpty() ? List.of() : etapes.findByInstanceIdInOrderByRangAsc(l.stream().map(InstanceProcessus::getId).toList());
        Map<UUID, List<EtapeInstance>> parInstance = toutes.stream().collect(Collectors.groupingBy(EtapeInstance::getInstanceId));
        Map<UUID, Tache> t = tachesDe(toutes);
        Map<UUID, Processus> definitions = new HashMap<>();
        l.forEach(i -> definitions.computeIfAbsent(i.getProcessusId(), id -> processus.findById(id).orElse(null)));
        List<String> ids = new ArrayList<>(l.stream().map(InstanceProcessus::getLanceurId).toList());
        t.values().stream().map(Tache::getResponsableId).filter(Objects::nonNull).forEach(ids::add);
        Map<UUID, List<HistoriqueProcessus>> derniers = new HashMap<>();
        for (InstanceProcessus i : l) {
            List<HistoriqueProcessus> h = historique.findByInstanceIdOrderByQuandAsc(i.getId());
            List<HistoriqueProcessus> dernier = h.isEmpty() ? List.of() : List.of(h.get(h.size() - 1));
            dernier.forEach(x -> ids.add(x.getActeurId()));
            derniers.put(i.getId(), dernier);
        }
        Map<String, String> noms = equipes.noms(ids);
        LocalDate jour = ReglesActivite.aujourdhui(horloge);
        List<InstanceLue> lues = l
            .stream()
            .map(i -> {
                List<EtapeInstance> e = parInstance.getOrDefault(i.getId(), List.of());
                Map<UUID, Tache> ti = new HashMap<>();
                e.stream().map(EtapeInstance::getTacheId).filter(Objects::nonNull).filter(t::containsKey).forEach(x -> ti.put(x, t.get(x)));
                return new InstanceLue(i, definitions.get(i.getProcessusId()), e, ti, derniers.get(i.getId()), noms, Map.of(), null, jour);
            })
            .toList();
        return new Page<>(lues, r.getTotalElements(), p.getPageNumber(), p.getPageSize());
    }

    // ------------------------------------------------------------------ lancement

    @Override
    public InstanceLue lancer(Appelant appelant, UUID processusId, Lancement l) {
        Processus p = processus.findById(processusId).orElseThrow(() -> PilotageException.introuvable("PIL_PROCESSUS_INTROUVABLE", processusId));
        if (!acces.lance(appelant, p)) {
            // D-98 §1 : initiateurs en données dans la définition, refus explicite.
            throw AccesProcessus.nonInitiateur(p);
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
        // D-98 §2 : formulaire de lancement contrôlé (422 PIL_PROCESSUS_FORMULAIRE_INVALIDE, fieldErrors).
        Map<String, Object> formulaire = FormulaireLancement.valeurs(p.getFormulaire(), l.formulaire(), p.getId(), appelant.id(), documents::metadonnees);
        String nPlus1 = equipes.rattachement(appelant.id()).map(r -> r.responsable() == null ? null : r.responsable().id()).orElse(null);
        Instant le = horloge.instant();
        InstanceProcessus i = new InstanceProcessus(p, intitule, appelant.id(), nPlus1, designations, le);
        i.renseignerFormulaire(formulaire);
        if (p.getReferenceModele() != null) {
            i.definirReference(ReferenceProcessus.suivante(p.getReferenceModele(), p.getCode(), ReglesActivite.aujourdhui(horloge), instances.prochaineReference()));
        }
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
        consigner(i, null, appelant.id(), ActionProcessus.LANCEE, "« " + intitule + " », " + p.getCode() + " version " + p.getVersion() + acces.mention(appelant, p.getUnite(), AccesProcessus.Acte.LANCER), le);
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
     * personne non interne active, ou ni porteuse du profil de remplacement (dans l'unité ou ailleurs dans le GIMAC,
     * désignation hors de l'unité), ni dans la portée du lanceur (TACHE_CONFIER), ni le lanceur : 422
     * PIL_PROCESSUS_DESIGNATION_INVALIDE.
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
            if (id.equals(appelant.id()) && !p.getInitiateurs().initiateurIntervenant()) {
                // D-99 : l'initiateur ne se désigne pas pour une étape de sa propre demande.
                throw separation(422, Map.of("cle", cle, "personneId", id));
            }
            String unite = intervenant.uniteOu(p.getUnite());
            // D-142 : sans profil de remplacement, la désignation est ouverte à toute personne interne active du GIMAC.
            boolean admissible =
                equipes.interneActif(id) &&
                (id.equals(appelant.id()) ||
                    intervenant.remplacement() == null ||
                    (intervenant.remplacement() != null &&
                        (equipes.porteProfilDans(id, unite, intervenant.remplacement()) || resolution.portePartout(id, intervenant.remplacement()))) ||
                    equipes.couvert(appelant, Pouvoir.TACHE_CONFIER, id).isPresent());
            if (!admissible) {
                throw PilotageException.regle(
                    "PIL_PROCESSUS_DESIGNATION_INVALIDE",
                    "Personne désignée inactive, ou ne portant pas le profil requis (dans l'unité ou ailleurs dans le GIMAC), ni dans votre portée",
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
        String exclu = exclu(i, p);
        ResolutionIntervenants.Resolution r = resolution.resoudre(def.intervenant(), p.getUnite(), i.getLanceurId(), i.getNPlus1LanceurId(), i.getDesignations(), def.code(), exclu);
        Affectation validateur = def.validateur() == null
            ? null
            : resolution.resoudre(def.validateur(), p.getUnite(), i.getLanceurId(), i.getNPlus1LanceurId(), i.getDesignations(), ResolutionIntervenants.cle(def.code(), ControleProcessus.VALIDATEUR), exclu).affectation();
        List<SignatureInstance> signatures = def
            .signatures()
            .stream()
            .sorted(Comparator.comparingInt(SignatureRequise::ordre).thenComparing(SignatureRequise::code))
            .map(s ->
                SignatureInstance.attendue(
                    s,
                    resolution.resoudre(s.signataire(), p.getUnite(), i.getLanceurId(), i.getNPlus1LanceurId(), i.getDesignations(), ResolutionIntervenants.cle(def.code(), "signature." + s.code()), exclu).affectation()
                )
            )
            .toList();
        // D-106 : la tâche porte l'échéance maximale (au-delà, retard) ; l'étape garde aussi l'échéance cible.
        LocalDate echeanceCible = ReglesActivite.aujourdhui(horloge).plusDays(def.delaiJours());
        LocalDate echeance = ReglesActivite.aujourdhui(horloge).plusDays(def.delaiMaxEffectif());
        Affectation a = r.affectation();
        List<String> destinataires = resolution.destinataires(a, null, exclu);
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
        ei.activer(t.getId(), validateur, signatures, echeanceCible, echeance, le);
        etapes.save(ei);
        String detail = a.personneId() != null ? "Confiée à " + nom(a.personneId()) : "À prendre par " + aPrendrePar(a);
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
                le,
                i.getLanceurId()
            )
        );
        if (r.parti() != null) {
            // Contrôle à l'activation : la personne prévue est partie, le chef de l'unité est averti.
            consigner(i, ei.getCode(), acteur, ActionProcessus.DEPART, r.note(), le);
            String chef = resolution.chef(a.unite());
            publierReaffectation(i, def, "DEPART", List.of("EXECUTANT"), r.parti(), a.personneId(), a, chef, acteur, null, avec(destinataires, chef), le);
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
            synchroniserAttentes(c.i(), c.def(), c.ei(), appelant.id(), le);
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
        exigerSeparation(appelant.id(), c, signataire(c.def(), code));
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
        synchroniserAttentes(c.i(), c.def(), c.ei(), appelant.id(), le);
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
        exigerSeparation(appelant.id(), c, c.def().validateur());
        String d = decision == null ? "" : decision.strip().toUpperCase(Locale.ROOT);
        Instant le = horloge.instant();
        switch (d) {
            case "VALIDER" -> {
                String m = SaisieProcessus.motif(motif, false);
                c.ei().valider(le);
                enregistrer(c.ei());
                consigner(c.i(), c.ei().getCode(), appelant.id(), ActionProcessus.VALIDEE, m, le);
                synchroniserAttentes(c.i(), c.def(), c.ei(), appelant.id(), le);
                apresTerminee(c.i(), c.p(), c.ei(), appelant.id(), le);
            }
            case "RENVOYER" -> {
                String m = SaisieProcessus.motif(motif, true);
                c.ei().renvoyer();
                enregistrer(c.ei());
                consigner(c.i(), c.ei().getCode(), appelant.id(), ActionProcessus.RENVOYEE, m, le);
                synchroniserAttentes(c.i(), c.def(), c.ei(), appelant.id(), le);
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
                        le,
                        c.i().getLanceurId()
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
        exigerReaffectation(appelant, c);
        if (!c.ei().getEtat().enCours()) {
            c.ei().exigerEtat(EtatEtape.ACTIVE, EtatEtape.A_SIGNER, EtatEtape.A_VALIDER);
        }
        String personne = ReglesActivite.texteObligatoire("personneId", personneId, 80);
        String m = SaisieProcessus.motif(motif, true);
        if (personne.equals(exclu(c.i(), c.p()))) {
            // D-99 : l'étape d'une demande n'est jamais confiée à son initiateur (sauf autorisation de la définition).
            throw separation(422, Map.of("personneId", personne));
        }
        String unite = uniteEtape(c);
        String profil = profilExige(c);
        boolean admissible =
            equipes.interneActif(personne) &&
            (equipes.porteProfilDans(personne, unite, null) ||
                (profil != null && resolution.portePartout(personne, profil)) ||
                equipes.couvert(appelant, Pouvoir.TACHE_CONFIER, personne).isPresent());
        if (!admissible) {
            throw PilotageException.regle(
                "PIL_PROCESSUS_PERSONNE_INVALIDE",
                "La personne doit être active, et membre de l'unité, porteuse du profil requis (ailleurs dans le GIMAC compris) ou dans votre portée",
                Map.of("personneId", personne)
            );
        }
        String ancien = executant(c.ei()).orElse(null);
        if (personne.equals(ancien)) {
            throw PilotageException.conflit("PIL_PROCESSUS_DEJA_AFFECTEE", "L'étape est déjà confiée à cette personne", Map.of("personneId", personne));
        }
        Instant le = horloge.instant();
        String detail = "Étape réaffectée " + (ancien == null ? "" : "de " + nom(ancien) + " ") + "à " + nom(personne) + " : " + m;
        taches.confierEtape(c.ei().getTacheId(), personne, appelant.id(), detail, le);
        consigner(c.i(), c.ei().getCode(), appelant.id(), ActionProcessus.REAFFECTEE, detail + titre(appelant, c.i()), le);
        publierReaffectation(c.i(), c.def(), "REAFFECTATION", List.of("EXECUTANT"), ancien, personne, null, null, appelant.id(), m, List.of(personne), le);
        return relire(appelant, c.i());
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessusUseCase.Candidats candidats(Appelant appelant, UUID id, String etape) {
        Contexte c = contexte(appelant, id, etape);
        exigerReaffectation(appelant, c);
        return resolution.candidats(uniteEtape(c), profilExige(c), exclu(c.i(), c.p()));
    }

    /**
     * Réaffecter une étape : chef de l'unité (ou TACHE_CONFIER, REF_METIER_ADMIN), ou lanceur de l'instance tant que
     * l'étape est à prendre ou lui est confiée (désignation hors de l'unité : il choisit parmi les porteurs).
     */
    private void exigerReaffectation(Appelant appelant, Contexte c) {
        if (!acces.pilote(appelant, c.i().getUnite()) && !lanceurDesigne(appelant.id(), c.i(), taches.tacheEtape(c.ei().getTacheId()).orElse(null))) {
            throw AccesProcessus.interdit("Réaffecter une étape est réservé au chef de l'unité (ou TACHE_CONFIER, REF_METIER_ADMIN) et au lanceur pour une étape à prendre");
        }
    }

    /** Le lanceur choisit l'exécutant d'une étape à prendre, ou de celle qui lui est revenue faute de porteur. */
    private static boolean lanceurDesigne(String moi, InstanceProcessus i, Tache t) {
        return moi.equals(i.getLanceurId()) && t != null && (t.getEtat() == EtatTache.A_PRENDRE || (t.getEtat().estOuvert() && moi.equals(t.getResponsableId())));
    }

    /** Unité de l'exécutant d'une étape : celle de l'intervenant, à défaut l'unité propriétaire. */
    private static String uniteEtape(Contexte c) {
        return c.def().intervenant() == null ? c.p().getUnite() : c.def().intervenant().uniteOu(c.p().getUnite());
    }

    /** Profil exigé de l'exécutant : celui de la tâche à prendre, de l'intervenant PROFIL, à défaut son profil de remplacement. */
    private String profilExige(Contexte c) {
        String requis = taches.tacheEtape(c.ei().getTacheId()).filter(t -> t.getEtat() == EtatTache.A_PRENDRE).map(Tache::getProfilRequis).orElse(null);
        if (requis != null) {
            return requis;
        }
        Intervenant i = c.def().intervenant();
        if (i == null) {
            return null;
        }
        return i.mode() == ModeIntervenant.PROFIL ? i.profil() : i.remplacement();
    }

    @Override
    public InstanceLue suspendre(Appelant appelant, UUID id, String motif) {
        InstanceProcessus i = pilotable(appelant, id);
        String m = SaisieProcessus.motif(motif, true);
        Instant le = horloge.instant();
        i.suspendre(m, le);
        enregistrer(i);
        consigner(i, null, appelant.id(), ActionProcessus.SUSPENDUE, motifTitre(m, appelant, i), le);
        return relire(appelant, i);
    }

    @Override
    public InstanceLue reprendre(Appelant appelant, UUID id, String motif) {
        InstanceProcessus i = pilotable(appelant, id);
        String m = SaisieProcessus.motif(motif, false);
        Instant le = horloge.instant();
        i.reprendre(m, le);
        enregistrer(i);
        consigner(i, null, appelant.id(), ActionProcessus.REPRISE, motifTitre(m, appelant, i), le);
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
                annulerAttentes(i, ei, appelant.id(), m, le);
                ei.annuler(le);
                etapes.save(ei);
            }
        }
        consigner(i, null, appelant.id(), ActionProcessus.ANNULEE, motifTitre(m, appelant, i), le);
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
        equipes.rafraichir();
        Instant le = horloge.instant();
        Set<UUID> traitees = new LinkedHashSet<>();
        for (Tache t : taches.tachesEtapesOuvertesDe(compteId)) {
            Optional<EtapeInstance> oe = etapes.findByTacheId(t.getId());
            // Seules les tâches d'exécution sont liées à l'étape ; celles d'attente suivent l'affectation, revue plus bas.
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
            Affectation a = remplacement(i, def.intervenant().remplacement(), unite, compteId);
            List<String> porteurs = resolution.destinataires(a, compteId);
            String detail = "Départ de " + nom(compteId) + " : étape " + (a.personneId() != null ? "confiée à " + nom(a.personneId()) : "à prendre par " + aPrendrePar(a));
            if (a.personneId() != null) {
                taches.confierEtape(t.getId(), a.personneId(), acteur, detail, le);
            } else {
                taches.remettreEtapeAPrendre(t.getId(), a.unite(), a.profil(), acteur, detail, porteurs, le);
            }
            consigner(i, ei.getCode(), acteur, ActionProcessus.DEPART, detail, le);
            String chef = resolution.chef(unite);
            publierReaffectation(i, def, "DEPART", List.of("EXECUTANT"), compteId, a.personneId(), a, chef, acteur, null, avec(porteurs, chef), le);
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
                remplacement = remplacement(i, def.validateur().remplacement(), def.validateur().uniteOu(p.getUnite()), compteId);
                ei.affecterValidateur(remplacement);
                roles.add("VALIDATEUR");
            }
            Map<String, SignatureRequise> parCode = def.signatures().stream().collect(Collectors.toMap(SignatureRequise::code, Function.identity(), (a, b) -> a));
            List<String> codes = ei.reaffecterSignatures(compteId, s -> {
                SignatureRequise sr = parCode.get(s.code());
                Intervenant sig = sr == null ? null : sr.signataire();
                return s.reaffectee(remplacement(i, sig == null ? null : sig.remplacement(), sig == null ? p.getUnite() : sig.uniteOu(p.getUnite()), compteId));
            });
            codes.forEach(code -> roles.add("SIGNATURE." + code));
            if (roles.isEmpty()) {
                continue;
            }
            etapes.save(ei);
            synchroniserAttentes(i, def, ei, acteur, le);
            String detail = "Départ de " + nom(compteId) + " : " + String.join(", ", roles) + " repris par profil de remplacement";
            consigner(i, ei.getCode(), acteur, ActionProcessus.DEPART, detail, le);
            Affectation cible = remplacement != null ? remplacement : codes.isEmpty() ? null : ei.signature(codes.get(0)).attendu();
            String unite = cible == null || cible.unite() == null ? p.getUnite() : cible.unite();
            String chef = resolution.chef(unite);
            List<String> porteurs = cible == null ? List.of() : resolution.destinataires(cible, compteId);
            publierReaffectation(i, def, "DEPART", roles, compteId, null, cible, chef, acteur, null, avec(porteurs, chef), le);
            traitees.add(ei.getId());
        }
        return traitees.size();
    }

    /**
     * Remplaçant d'une personne partie : par le profil de remplacement (dans l'unité, à défaut ailleurs dans le GIMAC ;
     * un seul porteur : à lui ; aucun : chef de l'unité, à défaut lanceur), jamais la personne partie elle-même.
     */
    private Affectation remplacement(InstanceProcessus i, String profil, String unite, String parti) {
        Affectation a = resolution.parProfil(profil, unite, i.getLanceurId(), parti, null, exclu(i, definition(i))).affectation();
        return parti.equals(a.personneId()) ? Affectation.profil(profil, unite) : a;
    }

    /** « les porteurs du profil P de l'unité U », « … dans tout le GIMAC », « les membres de l'unité U ». */
    private static String aPrendrePar(Affectation a) {
        String qui = a.profil() == null ? "les membres" : "les porteurs du profil " + a.profil();
        return qui + (a.unite() == null ? " dans tout le GIMAC" : " de l'unité " + a.unite());
    }

    /** Prise ou attribution de la tâche d'une étape par la voie des tâches : historisée dans l'instance. */
    @EventListener
    public void tacheConfiee(TacheProcessusConfiee e) {
        Optional<EtapeInstance> execution = etapes.findByTacheId(e.tacheId());
        if (execution.isPresent()) {
            EtapeInstance ei = execution.orElseThrow();
            consigner(trouver(ei.getInstanceId()), ei.getCode(), e.acteurId(), ActionProcessus.ETAPE_PRISE, (e.prise() ? "Prise par " : "Attribuée à ") + nom(e.responsableId()), horloge.instant());
            return;
        }
        attente(e.reference()).ifPresent(a -> prendreAttente(a, e));
    }


    // ------------------------------------------------------------------ séparation des fonctions (D-99)

    static final String SEPARATION = "PIL_PROCESSUS_SEPARATION_FONCTIONS";
    static final String MESSAGE_SEPARATION =
        "L'initiateur d'une demande ne peut pas exécuter, valider ou signer les étapes de sa propre demande (séparation des fonctions).";

    /** Initiateur à écarter des étapes de l'instance (D-99), null si la définition l'autorise. */
    private static String exclu(InstanceProcessus i, Processus p) {
        return p != null && p.getInitiateurs().initiateurIntervenant() ? null : i.getLanceurId();
    }

    /** Le compte est l'initiateur écarté du rôle (intervenant de mode LANCEUR : choix explicite de la définition, admis). */
    private static boolean separe(String compte, InstanceProcessus i, Processus p, Intervenant role) {
        return compte != null && compte.equals(exclu(i, p)) && (role == null || role.mode() != ModeIntervenant.LANCEUR);
    }

    private void exigerSeparation(String compte, Contexte c, Intervenant role) {
        if (separe(compte, c.i(), c.p(), role)) {
            throw separation(403, Map.of("lanceurId", c.i().getLanceurId()));
        }
    }

    /** Refus de la séparation des fonctions : 403 (agir), 422 (désigner ou réaffecter l'initiateur). */
    static PilotageException separation(int statut, Map<String, Object> details) {
        return statut == 403 ? PilotageException.interditDetaille(SEPARATION, MESSAGE_SEPARATION, details) : PilotageException.regle(SEPARATION, MESSAGE_SEPARATION, details);
    }

    private static Intervenant signataire(Etape def, String code) {
        return def.signatures().stream().filter(s -> s.code().equals(code)).map(SignatureRequise::signataire).findFirst().orElse(null);
    }

    /**
     * Prise ou attribution de la tâche d'une étape, ou d'une validation ou signature à prendre (voie des tâches) : jamais
     * par l'initiateur de l'instance (D-99), sauf autorisation de la définition ou intervenant LANCEUR.
     */
    @EventListener
    public void verifierPrise(PriseTacheProcessus e) {
        Optional<EtapeInstance> execution = etapes.findByTacheId(e.tacheId());
        Optional<Attente> a = execution.isPresent() ? Optional.empty() : attente(e.reference());
        UUID instance = execution.map(EtapeInstance::getInstanceId).orElse(a.map(Attente::instance).orElse(null));
        if (instance == null) {
            return;
        }
        InstanceProcessus i = instances.findById(instance).orElse(null);
        if (i == null) {
            return;
        }
        Processus p = definition(i);
        String code = execution.map(EtapeInstance::getCode).orElse(a.map(Attente::etape).orElse(null));
        Etape def = p.etape(code).orElse(null);
        if (def == null) {
            return;
        }
        Intervenant role = execution.isPresent() ? def.intervenant() : a.orElseThrow().signature() == null ? def.validateur() : signataire(def, a.orElseThrow().signature());
        if (separe(e.compteId(), i, p, role)) {
            throw separation(e.attribution() ? 422 : 403, Map.of("personneId", e.compteId()));
        }
    }

    // ------------------------------------------------------------------ tâches d'attente : validateur et signataires (D-93)

    static final String VALIDATION = "VALIDATION";
    static final String SIGNATURE = "SIGNATURE";

    /** Référence de la tâche d'une validation ({@code <instance>/<ETAPE>/VALIDATION}) ou d'une signature ({@code …/SIGNATURE/<CODE>}). */
    static String referenceAttente(UUID instance, String etape, String signature) {
        return instance + "/" + etape + "/" + (signature == null ? VALIDATION : SIGNATURE + "/" + signature);
    }

    /** Tâche d'attente reconnue à sa référence : instance, étape, signature (null : validation). */
    private record Attente(UUID instance, String etape, String signature) {}

    private static Optional<Attente> attente(String reference) {
        String[] p = reference == null ? new String[0] : reference.split("/");
        try {
            if (p.length == 3 && VALIDATION.equals(p[2])) {
                return Optional.of(new Attente(UUID.fromString(p[0]), p[1], null));
            }
            if (p.length == 4 && SIGNATURE.equals(p[2])) {
                return Optional.of(new Attente(UUID.fromString(p[0]), p[1], p[3]));
            }
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * Prise (ou attribution) de la tâche d'une validation ou d'une signature attendue d'un profil : la personne
     * devient le validateur ou le signataire attendu (elle seule agit ensuite), historique ETAPE_PRISE.
     */
    private void prendreAttente(Attente a, TacheProcessusConfiee e) {
        etapes.findByInstanceIdAndCode(a.instance(), a.etape()).ifPresent(ei -> {
            String unite;
            if (a.signature() == null) {
                if (ei.getValidateur() == null || ei.getEtat() != EtatEtape.A_VALIDER) {
                    return;
                }
                unite = ei.getValidateur().unite();
                ei.affecterValidateur(Affectation.personne(e.responsableId(), unite));
            } else {
                SignatureInstance s = ei.signature(a.signature());
                if (s == null || s.attendu() == null) {
                    return;
                }
                ei.reaffecterSignature(a.signature(), Affectation.personne(e.responsableId(), s.attendu().unite()));
            }
            enregistrer(ei);
            String quoi = a.signature() == null ? "Validation" : "Signature " + a.signature();
            consigner(trouver(a.instance()), a.etape(), e.acteurId(), ActionProcessus.ETAPE_PRISE, quoi + (e.prise() ? " prise par " : " attribuée à ") + nom(e.responsableId()), horloge.instant());
        });
    }

    /**
     * Tâches d'attente alignées sur l'état de l'étape : à valider, une tâche au validateur (à prendre par son profil
     * s'il n'est pas nommé) ; à signer, une tâche par signature ouverte (ordre minimal) au signataire attendu ;
     * signature apposée : sa tâche terminée au nom du signataire ; validation ou renvoi décidé : tâche de validation
     * terminée au nom de l'acteur.
     */
    private void synchroniserAttentes(InstanceProcessus i, Etape def, EtapeInstance ei, String acteur, Instant le) {
        String validation = referenceAttente(i.getId(), ei.getCode(), null);
        if (ei.getEtat() == EtatEtape.A_VALIDER && ei.getValidateur() != null) {
            ouvrirAttente(i, def, ei, validation, ei.getValidateur(), "Valider", le);
        } else {
            taches.clore(validation, acteur, le);
        }
        Set<String> ouvertes = ei.signaturesOuvertes().stream().map(SignatureInstance::code).collect(Collectors.toSet());
        for (SignatureInstance s : ei.getSignatures()) {
            String ref = referenceAttente(i.getId(), ei.getCode(), s.code());
            if (ouvertes.contains(s.code()) && s.attendu() != null) {
                ouvrirAttente(i, def, ei, ref, s.attendu(), "Signer « " + s.libelle() + " »", le);
            } else if (s.etat() == cm.gimac.des.pilotage.domain.processus.EtatSignature.SIGNEE) {
                taches.clore(ref, s.signePar(), le);
            }
        }
    }

    private void ouvrirAttente(InstanceProcessus i, Etape def, EtapeInstance ei, String reference, Affectation a, String verbe, Instant le) {
        taches.ouvrirAttente(
            new TacheService.TacheEtape(
                i.getLanceurId(),
                a.personneId(),
                a.unite(),
                a.profil(),
                verbe + " : " + def.libelle() + " : " + i.getIntitule(),
                def.action(),
                ei.getEcheance(),
                reference,
                lien(i, def.code()),
                a.personneId() == null ? resolution.destinataires(a, null, exclu(i, definition(i))) : List.of()
            ),
            le
        );
    }

    private void annulerAttentes(InstanceProcessus i, EtapeInstance ei, String acteur, String motif, Instant le) {
        taches.annulerAttente(referenceAttente(i.getId(), ei.getCode(), null), acteur, motif, le);
        ei.getSignatures().forEach(s -> taches.annulerAttente(referenceAttente(i.getId(), ei.getCode(), s.code()), acteur, motif, le));
    }

    // ------------------------------------------------------------------ changements d'organisation (D-93)

    /** Rôle tenu sur une étape : clé de désignation, rôle (EXECUTANT, VALIDATEUR, SIGNATURE.CODE), unité effective, titulaire. */
    private record Role(String cle, String role, Intervenant intervenant, String unite, String titulaire) {}

    @Override
    public int traiterChangementCompte(String compteId, String cause, String acteurId) {
        if (compteId == null || compteId.isBlank()) {
            return 0;
        }
        equipes.rafraichir();
        String acteur = acteurId == null || acteurId.isBlank() ? SERVICE_IAM : acteurId;
        Instant le = horloge.instant();
        Map<UUID, EtapeInstance> visees = new LinkedHashMap<>();
        for (Tache t : taches.tachesEtapesOuvertesDe(compteId)) {
            etapes.findByTacheId(t.getId()).ifPresent(ei -> visees.put(ei.getId(), ei));
        }
        etapes.attenduesDe(compteId).forEach(ei -> visees.putIfAbsent(ei.getId(), ei));
        int n = 0;
        for (EtapeInstance ei : visees.values()) {
            if (revoir(ei, r -> compteId.equals(r.titulaire()), cause, acteur, le)) {
                n++;
            }
        }
        return n;
    }

    @Override
    public int traiterChangementChef(String unite, String acteurId) {
        if (unite == null || unite.isBlank()) {
            return 0;
        }
        equipes.rafraichir();
        String acteur = acteurId == null || acteurId.isBlank() ? SERVICE_IAM : acteurId;
        Instant le = horloge.instant();
        int n = 0;
        for (EtapeInstance ei : etapes.enCours()) {
            if (revoir(ei, r -> r.intervenant().mode() == ModeIntervenant.CHEF_UNITE && unite.equals(r.unite()), "CHANGEMENT_CHEF", acteur, le)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Revue des rôles concernés d'une étape en cours : exécutant (étape active, sauf réaffectation par le chef
     * d'unité, respectée), validateur et signataires attendus. Un rôle qui n'est plus tenu passe à la nouvelle
     * affectation ; tâches alignées, historique ORGANISATION, pilotage.process-step-reassigned.v1 ({@code cause}).
     *
     * @return vrai si l'étape a changé.
     */
    private boolean revoir(EtapeInstance ei, java.util.function.Predicate<Role> concerne, String cause, String acteur, Instant le) {
        InstanceProcessus i = trouver(ei.getInstanceId());
        if (i.getEtat().estFinal() || !ei.getEtat().enCours()) {
            return false;
        }
        Processus p = definition(i);
        Etape def = definitionEtape(p, ei.getCode());
        List<String> roles = new ArrayList<>();
        List<String> details = new ArrayList<>();
        List<Affectation> nouvelles = new ArrayList<>();
        String ancien = null;
        Optional<Tache> tache = taches.tacheEtape(ei.getTacheId());
        boolean reaffecteeParLeChef = historique.existsByInstanceIdAndEtapeAndAction(i.getId(), ei.getCode(), ActionProcessus.REAFFECTEE);
        if (ei.getEtat() == EtatEtape.ACTIVE && def.intervenant() != null && tache.filter(t -> t.getEtat().estOuvert()).isPresent() && !reaffecteeParLeChef) {
            Tache t = tache.orElseThrow();
            Role r = new Role(ResolutionIntervenants.cle(def.code(), ControleProcessus.INTERVENANT), "EXECUTANT", def.intervenant(), def.intervenant().uniteOu(p.getUnite()), t.getResponsableId());
            Optional<Affectation> n = concerne.test(r) ? nouvelleAffectation(i, p, r) : Optional.empty();
            if (n.isPresent()) {
                Affectation a = n.orElseThrow();
                String detail = "EXECUTANT : " + quitte(r.titulaire()) + vers(a);
                if (a.personneId() != null) {
                    taches.confierEtape(t.getId(), a.personneId(), acteur, detail, le);
                } else {
                    taches.remettreEtapeAPrendre(t.getId(), a.unite(), a.profil(), acteur, detail, resolution.porteurs(a.unite(), a.profil(), r.titulaire()), le);
                }
                roles.add(r.role());
                details.add(detail);
                nouvelles.add(a);
                ancien = r.titulaire();
            }
        }
        if (def.validateur() != null && ei.getValidateur() != null) {
            Role r = new Role(ResolutionIntervenants.cle(def.code(), ControleProcessus.VALIDATEUR), "VALIDATEUR", def.validateur(), def.validateur().uniteOu(p.getUnite()), ei.getValidateur().personneId());
            Optional<Affectation> n = concerne.test(r) ? nouvelleAffectation(i, p, r) : Optional.empty();
            if (n.isPresent()) {
                ei.affecterValidateur(n.orElseThrow());
                roles.add(r.role());
                details.add("VALIDATEUR : " + quitte(r.titulaire()) + vers(n.orElseThrow()));
                nouvelles.add(n.orElseThrow());
                ancien = ancien == null ? r.titulaire() : ancien;
            }
        }
        Map<String, SignatureRequise> requises = def.signatures().stream().collect(Collectors.toMap(SignatureRequise::code, Function.identity(), (a, b) -> a));
        for (SignatureInstance s : ei.getSignatures()) {
            SignatureRequise sr = requises.get(s.code());
            if (s.etat() != cm.gimac.des.pilotage.domain.processus.EtatSignature.ATTENDUE || s.attendu() == null || sr == null || sr.signataire() == null) {
                continue;
            }
            Role r = new Role(ResolutionIntervenants.cle(def.code(), "signature." + s.code()), "SIGNATURE." + s.code(), sr.signataire(), sr.signataire().uniteOu(p.getUnite()), s.attendu().personneId());
            Optional<Affectation> n = concerne.test(r) ? nouvelleAffectation(i, p, r) : Optional.empty();
            if (n.isPresent()) {
                ei.reaffecterSignature(s.code(), n.orElseThrow());
                roles.add(r.role());
                details.add(r.role() + " : " + quitte(r.titulaire()) + vers(n.orElseThrow()));
                nouvelles.add(n.orElseThrow());
                ancien = ancien == null ? r.titulaire() : ancien;
            }
        }
        if (roles.isEmpty()) {
            return false;
        }
        enregistrer(ei);
        synchroniserAttentes(i, def, ei, acteur, le);
        consigner(i, ei.getCode(), acteur, ActionProcessus.ORGANISATION, libelleCause(cause) + " : " + String.join(" ; ", details), le);
        Affectation cible = nouvelles.get(0);
        String chef = resolution.chef(cible.unite() == null ? p.getUnite() : cible.unite());
        List<String> destinataires = nouvelles.stream().flatMap(a -> resolution.destinataires(a, null).stream()).distinct().toList();
        publierReaffectation(i, def, cause, roles, ancien, cible.personneId(), cible, chef, acteur, null, avec(destinataires, chef), le);
        return true;
    }

    /**
     * Nouvelle affectation d'un rôle, ou rien s'il est encore tenu. Le titulaire tient le rôle s'il est la personne
     * résolue (personne nommée, désignée au lancement, chef de l'unité, lanceur, n+1) ou, hors CHEF_UNITE, s'il porte
     * dans l'unité le profil du rôle (PROFIL) ou son profil de remplacement (autres modes). Sinon : la personne
     * résolue, à défaut tâche à prendre par ce profil. Rôle à prendre (sans titulaire) : confié à la personne résolue
     * s'il y en a une (nouveau chef d'unité).
     */
    private Optional<Affectation> nouvelleAffectation(InstanceProcessus i, Processus p, Role r) {
        ResolutionIntervenants.Resolution res = resolution.resoudre(r.intervenant(), p.getUnite(), i.getLanceurId(), i.getNPlus1LanceurId(), i.getDesignations(), r.cle(), exclu(i, p));
        Affectation a = res.affectation();
        String x = r.titulaire();
        if (x == null) {
            return a.personneId() != null ? Optional.of(a) : Optional.empty();
        }
        if (a.personneId() != null) {
            if (a.personneId().equals(x)) {
                return Optional.empty();
            }
            if (r.intervenant().mode() == ModeIntervenant.CHEF_UNITE) {
                return Optional.of(a);
            }
        }
        String profil = r.intervenant().mode() == ModeIntervenant.PROFIL ? r.intervenant().profil() : r.intervenant().remplacement();
        if (equipes.interneActif(x) && tient(x, r.unite(), profil)) {
            return Optional.empty();
        }
        return Optional.of(a);
    }

    /**
     * Le titulaire tient encore un rôle par profil : il le porte dans l'unité, ou, personne ne le portant dans l'unité,
     * ailleurs dans le GIMAC (désignation hors de l'unité).
     */
    private boolean tient(String titulaire, String unite, String profil) {
        if (equipes.porteProfilDans(titulaire, unite, profil)) {
            return true;
        }
        return profil != null && resolution.porteurs(unite, profil, null).isEmpty() && resolution.portePartout(titulaire, profil);
    }

    private String quitte(String titulaire) {
        return titulaire == null ? "à prendre" : nom(titulaire);
    }

    private String vers(Affectation a) {
        return " vers " + (a.personneId() != null ? nom(a.personneId()) : "à prendre par " + aPrendrePar(a));
    }

    private static String libelleCause(String cause) {
        return switch (cause == null ? "" : cause) {
            case "PROFIL_RETIRE" -> "Profil retiré";
            case "CHANGEMENT_UNITE" -> "Changement d'unité";
            case "CHANGEMENT_CHEF" -> "Nouveau chef d'unité";
            default -> "Organisation modifiée";
        };
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
            throw AccesProcessus.interdit("Suspendre, reprendre ou annuler une instance est réservé au chef de l'unité (ou TACHE_CONFIER, REF_METIER_ADMIN)");
        }
        return i;
    }

    /** Exécutant : responsable de la tâche de l'étape ; tâche à prendre : 403 (la prendre d'abord). */
    private void exigerExecutant(Appelant appelant, Contexte c) {
        Optional<Tache> t = taches.tacheEtape(c.ei().getTacheId());
        if (t.map(Tache::getResponsableId).filter(appelant.id()::equals).isPresent()) {
            exigerSeparation(appelant.id(), c, c.def().intervenant());
            return;
        }
        // D-138 : étape adressée à un rôle, tout porteur du rôle dans l'unité visée agit sans prise préalable ; le
        // premier acte de fin l'emporte (version optimiste : 409 PIL_PROCESSUS_ETAPE_ETAT chez le second).
        if (t.filter(x -> taches.porteurDuRole(x, appelant.id())).isPresent()) {
            exigerSeparation(appelant.id(), c, c.def().intervenant());
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

    /**
     * Lanceur, exécutant, validateur ou signataire (attendu ou ayant signé) de l'une des étapes ; porteur qui peut prendre
     * une étape à prendre, une validation ou une signature attendue de son profil (QUO-03 : il lit l'instance avant de
     * la prendre).
     */
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
            if (tache != null && tache.getEtat() == EtatTache.A_PRENDRE && resolution.affecte(moi, new Affectation(null, tache.getProfilRequis(), tache.getUniteCible()))) {
                return true;
            }
            if (ei.getEtat() == EtatEtape.A_VALIDER && resolution.affecte(moi, ei.getValidateur())) {
                return true;
            }
            if (ei.signaturesOuvertes().stream().anyMatch(s -> resolution.affecte(moi, s.attendu()))) {
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

    /** Actions permises à l'appelant sur chaque étape (calculées par le serveur, jamais par l'interface). */
    private Map<String, MoiEtape> moiDe(Appelant appelant, InstanceProcessus i, Processus p, List<EtapeInstance> l, Map<UUID, Tache> t) {
        boolean enCours = i.getEtat() == EtatInstance.EN_COURS;
        Map<String, MoiEtape> moi = new LinkedHashMap<>();
        if (!enCours) {
            l.forEach(ei -> moi.put(ei.getCode(), new MoiEtape(false, false, List.of(), false, false)));
            return moi;
        }
        boolean pilote = acces.pilote(appelant, i.getUnite());
        for (EtapeInstance ei : l) {
            Tache tache = ei.getTacheId() == null ? null : t.get(ei.getTacheId());
            String executant = tache == null ? null : tache.getResponsableId();
            // D-99 : l'initiateur n'exécute, ne prend, ne valide ni ne signe les étapes de sa demande (sauf LANCEUR ou autorisation).
            Etape def = p == null ? null : p.etape(ei.getCode()).orElse(null);
            boolean separeExecution = def != null && separe(appelant.id(), i, p, def.intervenant());
            boolean executer = ei.getEtat() == EtatEtape.ACTIVE && (appelant.id().equals(executant) || taches.porteurDuRole(tache, appelant.id())) && !separeExecution;
            boolean prendre = tache != null && tache.getEtat() == EtatTache.A_PRENDRE && !separeExecution &&
                resolution.affecte(appelant.id(), new Affectation(null, tache.getProfilRequis(), tache.getUniteCible()));
            List<String> signer = ei
                .signaturesOuvertes()
                .stream()
                .filter(s -> resolution.affecte(appelant.id(), s.attendu()))
                .filter(s -> def == null || !separe(appelant.id(), i, p, signataire(def, s.code())))
                .map(SignatureInstance::code)
                .toList();
            boolean valider = ei.getEtat() == EtatEtape.A_VALIDER && !appelant.id().equals(executant) && resolution.affecte(appelant.id(), ei.getValidateur()) &&
                (def == null || !separe(appelant.id(), i, p, def.validateur()));
            boolean reaffecter = ei.getEtat().enCours() && (pilote || lanceurDesigne(appelant.id(), i, tache));
            moi.put(ei.getCode(), new MoiEtape(executer, prendre, signer, valider, reaffecter));
        }
        return moi;
    }

    private InstanceLue lue(Appelant appelant, InstanceProcessus i, Processus p, List<EtapeInstance> l, Map<UUID, Tache> t) {
        boolean pilote = acces.pilote(appelant, i.getUnite());
        boolean enCours = i.getEtat() == EtatInstance.EN_COURS;
        Map<String, MoiEtape> moi = moiDe(appelant, i, p, l, t);
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

    /** Droit utilisé pour piloter l'instance quand la règle de l'unité ne suffit pas (D-92). */
    private String titre(Appelant appelant, InstanceProcessus i) {
        return acces.mention(appelant, i.getUnite(), AccesProcessus.Acte.PILOTER);
    }

    private String motifTitre(String motif, Appelant appelant, InstanceProcessus i) {
        String t = titre(appelant, i);
        if (t.isEmpty()) {
            return motif;
        }
        return motif == null ? t.strip() : motif + t;
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

    /** Liste vide interdite dans « in » : un code qu'aucune unité ni aucun profil ne porte la remplace. */
    private static List<String> auMoinsUn(java.util.Collection<String> codes) {
        return codes.isEmpty() ? List.of("-") : List.copyOf(codes);
    }
}
