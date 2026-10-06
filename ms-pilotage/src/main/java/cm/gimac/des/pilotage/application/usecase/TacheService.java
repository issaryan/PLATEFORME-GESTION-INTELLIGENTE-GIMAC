package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.Page;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Membre;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.domain.activite.ActionHistorique;
import cm.gimac.des.pilotage.domain.activite.EtatTache;
import cm.gimac.des.pilotage.domain.activite.PrioriteTache;
import cm.gimac.des.pilotage.domain.activite.Tache;
import cm.gimac.des.pilotage.domain.activite.TacheHistorique;
import cm.gimac.des.pilotage.domain.activite.TypeOrigine;
import cm.gimac.des.pilotage.domain.event.TacheAffecteePayload;
import cm.gimac.des.pilotage.domain.event.TacheCreeePayload;
import cm.gimac.des.pilotage.domain.event.TacheOffertePayload;
import cm.gimac.des.pilotage.domain.event.TacheRelanceePayload;
import cm.gimac.des.pilotage.domain.event.TacheTermineePayload;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import cm.gimac.des.pilotage.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.pilotage.repository.TacheHistoriqueRepository;
import cm.gimac.des.pilotage.repository.TacheRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tâches (§2.3) : responsable unique interne et actif (RG-20) ; création pour soi, pour une personne, pour
 * plusieurs (groupe) ou pour une unité (à prendre), D-81 §6 ; modification par le créateur ou le
 * responsable, réaffectation par le créateur ; transitions du CC ; prise, attribution, relance ;
 * historique en ajout seul ; événements pilotage.task-* par l'outbox.
 *
 * <p>Portées (matrice des pouvoirs de MS-IAM) : confier, réaffecter, attribuer exigent PIL_TASK_ASSIGN et
 * TACHE_CONFIER couvrant la personne ou l'unité ; la vue equipe, la lecture des tâches d'un subordonné et
 * la relance exigent PIL_TEAM_READ et TACHE_SUIVRE couvrant le responsable (le créateur relance sans
 * condition de portée).
 */
@Service
@Transactional
public class TacheService implements TachesUseCase {

    static final String AGREGAT = "tache";
    /** Nombre de personnes d'un envoi groupé (D-81 §6). */
    static final int GROUPE_MIN = 2;
    static final int GROUPE_MAX = 50;
    private static final Pattern UNITE = Pattern.compile("^[A-Z][A-Z0-9_]{1,31}$");
    private static final Pattern SERVICE = Pattern.compile("^[a-z][a-z0-9-]{1,59}$");

    private final TacheRepository taches;
    private final TacheHistoriqueRepository historique;
    private final Equipes equipes;
    private final OutboxPublisher outbox;
    private final Clock horloge;
    private final ApplicationEventPublisher evenements;

    public TacheService(
        TacheRepository taches,
        TacheHistoriqueRepository historique,
        Equipes equipes,
        OutboxPublisher outbox,
        Clock horloge,
        ApplicationEventPublisher evenements
    ) {
        this.taches = taches;
        this.historique = historique;
        this.equipes = equipes;
        this.outbox = outbox;
        this.horloge = horloge;
        this.evenements = evenements;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TacheLue> taches(Appelant appelant, String vue, String etat, Integer page, Integer taille) {
        EtatTache filtre = etat == null || etat.isBlank() ? null : ReglesActivite.valeur("etat", EtatTache.class, etat, null);
        PageRequest p = PageRequest.of(ReglesActivite.page(page), ReglesActivite.taille(taille));
        org.springframework.data.domain.Page<Tache> r;
        switch (vue == null || vue.isBlank() ? "moi" : vue.strip()) {
            case "moi" -> r = taches.deResponsables(List.of(appelant.id()), filtre, p);
            case "creees" -> r = taches.confiees(appelant.id(), filtre, p);
            case "equipe" -> {
                if (!appelant.litEquipe()) {
                    throw PilotageException.horsEquipe();
                }
                List<String> ids = equipes.membres(appelant, Pouvoir.TACHE_SUIVRE).stream().map(Membre::id).toList();
                r = ids.isEmpty() ? org.springframework.data.domain.Page.empty(p) : taches.deResponsables(ids, filtre, p);
            }
            case "unite" -> {
                List<String> unites = equipes.unitesDe(appelant.id());
                List<String> profils = new ArrayList<>(equipes.profilsDe(appelant.id()));
                if (profils.isEmpty()) {
                    // Liste vide interdite dans « in » : aucun profil ne s'appelle ainsi.
                    profils.add("-");
                }
                r = unites.isEmpty()
                    ? org.springframework.data.domain.Page.empty(p)
                    : taches.deUnites(unites, filtre == null ? EtatTache.A_PRENDRE : filtre, profils, p);
            }
            default -> throw PilotageException.champ("vue", "Vue moi, creees, equipe ou unite attendue");
        }
        LocalDate jour = ReglesActivite.aujourdhui(horloge);
        Map<String, String> noms = equipes.noms(r.getContent().stream().flatMap(t -> Stream.of(t.getResponsableId(), t.getCreateurId())).filter(Objects::nonNull).toList());
        return new Page<>(r.getContent().stream().map(t -> new TacheLue(t, List.of(), noms, t.enRetard(jour))).toList(), r.getTotalElements(), p.getPageNumber(), p.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public TacheLue tache(Appelant appelant, UUID id) {
        Tache t = trouver(id);
        if (!peutLire(t, appelant)) {
            throw PilotageException.horsEquipe();
        }
        return lue(t);
    }

    @Override
    public TacheLue creer(Appelant appelant, TacheSaisie saisie) {
        if (saisie == null) {
            throw PilotageException.champ("titre", "Corps de la requête absent");
        }
        if (saisie.responsables() != null) {
            throw PilotageException.champ("responsables", "Une tâche à plusieurs personnes se crée en groupe");
        }
        String unite = texte(saisie.uniteCible());
        if (unite != null) {
            if (texte(saisie.responsableId()) != null) {
                throw PilotageException.champ("uniteCible", "Une tâche se confie à une personne, à plusieurs ou à une unité, pas à la fois");
            }
            return creerPourUnite(appelant, saisie, unite);
        }
        String responsable = saisie.responsableId() == null || saisie.responsableId().isBlank() ? appelant.id() : saisie.responsableId().strip();
        if (!responsable.equals(appelant.id())) {
            exigerConfiable(appelant, responsable);
        }
        Instant le = horloge.instant();
        Tache t = new Tache(appelant.id(), responsable, le);
        decrire(t, saisie, le);
        taches.save(t);
        consigner(t, appelant.id(), ActionHistorique.CREEE, responsable.equals(appelant.id()) ? null : "Confiée à " + nom(responsable), le);
        publierCreation(t, appelant.id(), le);
        return lue(t);
    }

    @Override
    public GroupeLu creerPourPlusieurs(Appelant appelant, TacheSaisie saisie) {
        if (saisie == null) {
            throw PilotageException.champ("titre", "Corps de la requête absent");
        }
        if (texte(saisie.responsableId()) != null || texte(saisie.uniteCible()) != null) {
            throw PilotageException.champ("responsables", "Une tâche se confie à une personne, à plusieurs ou à une unité, pas à la fois");
        }
        LinkedHashSet<String> personnes = new LinkedHashSet<>();
        if (saisie.responsables() != null) {
            saisie.responsables().stream().map(TacheService::texte).filter(Objects::nonNull).forEach(personnes::add);
        }
        if (personnes.size() < GROUPE_MIN || personnes.size() > GROUPE_MAX) {
            throw PilotageException.invalide(
                "PIL_REQUETE_INVALIDE",
                "De " + GROUPE_MIN + " à " + GROUPE_MAX + " personnes distinctes attendues",
                Map.of("champ", "responsables", "nombre", personnes.size())
            );
        }
        for (String r : personnes) {
            if (!r.equals(appelant.id())) {
                exigerConfiable(appelant, r);
            }
        }
        Instant le = horloge.instant();
        UUID groupe = UUID.randomUUID();
        List<Tache> creees = new ArrayList<>();
        for (String r : personnes) {
            Tache t = new Tache(appelant.id(), r, le);
            decrire(t, saisie, le);
            t.rattacherAuGroupe(groupe);
            taches.save(t);
            consigner(t, appelant.id(), ActionHistorique.CREEE, (r.equals(appelant.id()) ? "Créée" : "Confiée à " + nom(r)) + " (envoi à " + personnes.size() + " personnes)", le);
            publierCreation(t, appelant.id(), le);
            creees.add(t);
        }
        return groupeLu(groupe, appelant.id(), creees);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupeLu groupe(Appelant appelant, UUID groupeId) {
        List<Tache> toutes = taches.findByGroupeIdOrderByCreeLeAsc(groupeId);
        if (toutes.isEmpty()) {
            throw PilotageException.introuvable("PIL_GROUPE_INTROUVABLE", groupeId);
        }
        String createur = toutes.get(0).getCreateurId();
        List<Tache> visibles = appelant.id().equals(createur) ? toutes : toutes.stream().filter(t -> peutLire(t, appelant)).toList();
        if (visibles.isEmpty()) {
            throw PilotageException.horsEquipe();
        }
        return groupeLu(groupeId, createur, visibles);
    }

    @Override
    public TacheLue prendre(Appelant appelant, UUID id) {
        Tache t = trouver(id);
        String unite = t.getUniteCible();
        if (unite == null || !equipes.dansUnite(appelant.id(), unite)) {
            if (peutLire(t, appelant)) {
                throw PilotageException.reservee("Seul un membre de l'unité visée prend la tâche");
            }
            throw PilotageException.horsEquipe();
        }
        if (t.getProfilRequis() != null && t.getEtat() == EtatTache.A_PRENDRE && !equipes.profilsDe(appelant.id()).contains(t.getProfilRequis())) {
            throw PilotageException.interdit("PIL_PROFIL_REQUIS", "Seuls les porteurs du profil " + t.getProfilRequis() + " prennent cette tâche");
        }
        Instant le = horloge.instant();
        t.confierA(appelant.id(), le);
        enregistrer(t);
        consigner(t, appelant.id(), ActionHistorique.PRISE, "Prise par " + nom(appelant.id()), le);
        publierAffectation(t, null, appelant.id(), le);
        signalerProcessus(t, appelant.id(), true);
        return lue(t);
    }

    @Override
    public TacheLue attribuer(Appelant appelant, UUID id, String responsableId) {
        Tache t = trouver(id);
        String unite = t.getUniteCible();
        boolean createur = appelant.id().equals(t.getCreateurId());
        if (unite == null || !(createur || equipes.pouvoirs(appelant.id()).dirige(unite))) {
            if (peutLire(t, appelant)) {
                throw PilotageException.reservee("Seuls le responsable de l'unité et le créateur attribuent la tâche");
            }
            throw PilotageException.horsEquipe();
        }
        String cible = ReglesActivite.texteObligatoire("responsableId", responsableId, 80);
        return attribuerA(appelant, t, cible);
    }

    @Override
    public TacheLue relancer(Appelant appelant, UUID id) {
        Tache t = trouver(id);
        boolean createur = appelant.id().equals(t.getCreateurId());
        boolean suivi = t.getResponsableId() != null && appelant.litEquipe() && equipes.couvert(appelant, Pouvoir.TACHE_SUIVRE, t.getResponsableId()).isPresent();
        if (!createur && !suivi) {
            if (peutLire(t, appelant)) {
                throw PilotageException.reservee("Seuls le créateur et les responsables qui suivent la tâche la relancent");
            }
            throw PilotageException.horsEquipe();
        }
        if (appelant.id().equals(t.getResponsableId())) {
            throw PilotageException.conflit("PIL_RELANCE_IMPOSSIBLE", "Le responsable ne se relance pas lui-même", Map.of());
        }
        Instant le = horloge.instant();
        t.relancer(le);
        enregistrer(t);
        consigner(t, appelant.id(), ActionHistorique.RELANCEE, "Relance adressée à " + nom(t.getResponsableId()), le);
        outbox.publish(AGREGAT, t.getId(), TacheRelanceePayload.TYPE, new TacheRelanceePayload(t.getId(), t.getTitre(), t.getResponsableId(), appelant.id(), le));
        return lue(t);
    }

    /** Tâche confiée à une unité : PIL_TASK_ASSIGN et TACHE_CONFIER couvrant l'unité ; à prendre ; task-offered aux membres. */
    private TacheLue creerPourUnite(Appelant appelant, TacheSaisie saisie, String unite) {
        if (!UNITE.matcher(unite).matches()) {
            throw PilotageException.champ("uniteCible", "Code d'unité attendu (par exemple POLE_RESEAU)");
        }
        if (!appelant.confie()) {
            throw PilotageException.reservee("Confier une tâche exige la permission PIL_TASK_ASSIGN");
        }
        if (!equipes.couvreUnite(appelant, Pouvoir.TACHE_CONFIER, unite)) {
            throw PilotageException.horsEquipe();
        }
        Instant le = horloge.instant();
        Tache t = Tache.pourUnite(appelant.id(), unite, le);
        decrire(t, saisie, le);
        taches.save(t);
        consigner(t, appelant.id(), ActionHistorique.CREEE, "Confiée à l'unité " + unite + ", à prendre", le);
        publierCreation(t, appelant.id(), le);
        List<String> destinataires = equipes.membresUnite(appelant, unite).stream().map(Membre::id).filter(m -> !m.equals(appelant.id())).toList();
        outbox.publish(AGREGAT, t.getId(), TacheOffertePayload.TYPE, new TacheOffertePayload(t.getId(), t.getTitre(), unite, destinataires, appelant.id(), le));
        return lue(t);
    }

    /** Attribution d'une tâche à prendre : membre de l'unité (422 sinon), TACHE_CONFIER le couvrant s'il n'est pas l'appelant. */
    private TacheLue attribuerA(Appelant appelant, Tache t, String cible) {
        if (t.getEtat() != EtatTache.A_PRENDRE) {
            throw PilotageException.conflit("PIL_TRANSITION_INTERDITE", "La tâche n'est plus à prendre", Map.of("de", t.getEtat().name(), "vers", EtatTache.OUVERTE.name()));
        }
        if (!cible.equals(appelant.id())) {
            exigerConfiable(appelant, cible);
        }
        if (!equipes.dansUnite(cible, t.getUniteCible())) {
            throw PilotageException.regle("PIL_RESPONSABLE_INVALIDE", "Le responsable doit être membre de l'unité visée", Map.of("responsableId", cible, "uniteCible", t.getUniteCible()));
        }
        Instant le = horloge.instant();
        t.confierA(cible, le);
        enregistrer(t);
        consigner(t, appelant.id(), ActionHistorique.ATTRIBUEE, "Attribuée à " + nom(cible), le);
        publierAffectation(t, null, appelant.id(), le);
        signalerProcessus(t, appelant.id(), false);
        return lue(t);
    }

    /** Prise concurrente : la première l'emporte, la suivante reçoit 409. */
    private void enregistrer(Tache t) {
        try {
            taches.saveAndFlush(t);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw PilotageException.conflit("PIL_TRANSITION_INTERDITE", "La tâche a été modifiée entre-temps", Map.of("id", String.valueOf(t.getId())));
        }
    }

    private void publierAffectation(Tache t, String ancien, String acteur, Instant le) {
        outbox.publish(
            AGREGAT,
            t.getId(),
            TacheAffecteePayload.TYPE,
            new TacheAffecteePayload(t.getId(), t.getEtat().name(), t.getResponsableId(), ancien, acteur, le, t.getTitre(), echeanceTexte(t))
        );
    }

    private GroupeLu groupeLu(UUID groupe, String createur, List<Tache> liste) {
        LocalDate jour = ReglesActivite.aujourdhui(horloge);
        List<String> ids = new ArrayList<>();
        ids.add(createur);
        liste.stream().map(Tache::getResponsableId).filter(Objects::nonNull).forEach(ids::add);
        Map<String, String> noms = equipes.noms(ids);
        List<TacheLue> lues = liste.stream().map(t -> new TacheLue(t, List.of(), noms, t.enRetard(jour))).toList();
        int total = (int) liste.stream().filter(t -> t.getEtat() != EtatTache.ANNULEE).count();
        int terminees = (int) liste.stream().filter(t -> t.getEtat() == EtatTache.TERMINEE).count();
        int enRetard = (int) lues.stream().filter(TacheLue::enRetard).count();
        return new GroupeLu(groupe, liste.get(0).getTitre(), createur, lues, noms, new Avancement(total, terminees, enRetard));
    }

    /**
     * Lecture d'une tâche : créateur et responsable ; membres et responsables de l'unité visée ; responsables
     * dont TACHE_SUIVRE couvre le responsable de la tâche (PIL_TEAM_READ).
     */
    private boolean peutLire(Tache t, Appelant appelant) {
        if (liee(t, appelant)) {
            return true;
        }
        String unite = t.getUniteCible();
        if (unite != null && (equipes.dansUnite(appelant.id(), unite) || equipes.pouvoirs(appelant.id()).dirige(unite))) {
            return true;
        }
        return t.getResponsableId() != null && equipes.peutLire(appelant, Pouvoir.TACHE_SUIVRE, t.getResponsableId());
    }

    private static String texte(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    @Override
    public TacheLue modifier(Appelant appelant, UUID id, TacheSaisie saisie) {
        Tache t = trouver(id);
        if (!liee(t, appelant)) {
            if (peutLire(t, appelant)) {
                throw PilotageException.reservee("Seuls le créateur et le responsable modifient la tâche");
            }
            throw PilotageException.horsEquipe();
        }
        if (saisie == null) {
            throw PilotageException.champ("titre", "Corps de la requête absent");
        }
        Instant le = horloge.instant();
        String avant = resume(t);
        decrire(t, saisie, le);
        String apres = resume(t);
        if (!avant.equals(apres)) {
            consigner(t, appelant.id(), ActionHistorique.MODIFIEE, apres, le);
        }
        String nouveau = saisie.responsableId() == null || saisie.responsableId().isBlank() ? null : saisie.responsableId().strip();
        if (nouveau != null && !nouveau.equals(t.getResponsableId())) {
            if (!t.getCreateurId().equals(appelant.id())) {
                throw PilotageException.reservee("Seul le créateur réaffecte la tâche");
            }
            if (t.getOrigineType() == TypeOrigine.PROCESSUS) {
                throw tacheProcessus("La tâche d'une étape se réaffecte depuis l'instance du processus");
            }
            if (t.getEtat() == EtatTache.A_PRENDRE) {
                // Tâche à prendre : la désignation d'un responsable est une attribution.
                return attribuerA(appelant, t, nouveau);
            }
            if (!nouveau.equals(appelant.id())) {
                exigerConfiable(appelant, nouveau);
            }
            String ancien = t.getResponsableId();
            t.reaffecter(nouveau, le);
            consigner(t, appelant.id(), ActionHistorique.REAFFECTEE, "De " + nom(ancien) + " à " + nom(nouveau), le);
            publierAffectation(t, ancien, appelant.id(), le);
        }
        return lue(t);
    }

    @Override
    public TacheLue transiter(Appelant appelant, UUID id, String etat, String motif) {
        Tache t = trouver(id);
        if (!liee(t, appelant)) {
            if (peutLire(t, appelant)) {
                throw PilotageException.reservee("Seul le responsable fait les transitions ; le créateur peut annuler");
            }
            throw PilotageException.horsEquipe();
        }
        EtatTache cible = etat == null || etat.isBlank() ? null : ReglesActivite.valeur("etat", EtatTache.class, etat, null);
        if (t.getOrigineType() == TypeOrigine.PROCESSUS && (cible == EtatTache.TERMINEE || cible == EtatTache.ANNULEE)) {
            throw tacheProcessus("La tâche d'une étape se termine avec l'étape (fin, signatures, validation) et s'annule avec l'instance");
        }
        Instant le = horloge.instant();
        String m = motif == null || motif.isBlank() ? null : motif.strip();
        EtatTache avant = t.transiter(cible, m, appelant.id(), le);
        consigner(t, appelant.id(), ActionHistorique.TRANSITION, avant.name() + " vers " + cible.name() + (m == null ? "" : " : " + m), le);
        if (cible == EtatTache.TERMINEE) {
            outbox.publish(
                AGREGAT,
                t.getId(),
                TacheTermineePayload.TYPE,
                new TacheTermineePayload(
                    t.getId(),
                    t.getEtat().name(),
                    t.getResponsableId(),
                    t.getCreateurId(),
                    t.getOrigineType().name(),
                    t.getOrigineReference(),
                    appelant.id(),
                    le
                )
            );
        }
        return lue(t);
    }

    @Override
    public Creation creerPourService(String client, TacheInterne d) {
        if (d == null || d.origine() == null) {
            throw PilotageException.champ("origine", "Origine absente");
        }
        TypeOrigine type = ReglesActivite.valeur("origine.type", TypeOrigine.class, d.origine().type(), null);
        if (type == TypeOrigine.INTERNE || type == TypeOrigine.PROCESSUS) {
            throw PilotageException.champ("origine.type", "Une tâche d'un autre service n'a ni l'origine INTERNE ni l'origine PROCESSUS");
        }
        String reference = ReglesActivite.texteObligatoire("origine.reference", d.origine().reference(), 200);
        String service = ReglesActivite.texteFacultatif("origine.service", d.origine().service(), 60);
        if (service != null && !SERVICE.matcher(service).matches()) {
            throw PilotageException.champ("origine.service", "Nom de service en minuscules attendu (par exemple homologation)");
        }
        String lien = ReglesActivite.texteFacultatif("origine.lien", d.origine().lien(), 500);
        if (lien != null && !(lien.startsWith("/") && !lien.startsWith("//"))) {
            // Lien relatif à l'interface seulement : aucune redirection vers un autre site.
            throw PilotageException.champ("origine.lien", "Chemin relatif de l'interface attendu (par exemple /des/parcours/…)");
        }
        String responsable = ReglesActivite.texteObligatoire("responsableId", d.responsableId(), 80);
        Optional<Tache> existante = taches.findFirstByOrigineReferenceAndResponsableId(reference, responsable);
        if (existante.isPresent()) {
            return new Creation(lue(existante.orElseThrow()), false);
        }
        if (!equipes.interneActif(responsable)) {
            throw PilotageException.regle("PIL_RESPONSABLE_INVALIDE", "Le responsable doit être un compte interne actif", Map.of("responsableId", responsable));
        }
        String createur = d.createurId() == null || d.createurId().isBlank() ? client : d.createurId().strip();
        Instant le = horloge.instant();
        Tache t = new Tache(createur, responsable, le);
        decrire(t, new TacheSaisie(d.titre(), d.description(), d.echeance(), d.priorite(), responsable), le);
        t.provenirDe(type, reference, service, lien);
        try {
            taches.saveAndFlush(t);
        } catch (DataIntegrityViolationException e) {
            // Demande concurrente pour la même origine et le même responsable : la première l'emporte.
            throw PilotageException.conflit("PIL_TACHE_EXISTANTE", "Tâche déjà créée pour cette origine", Map.of("reference", reference));
        }
        consigner(t, createur, ActionHistorique.CREEE, "Créée par le service " + (service == null ? client : service), le);
        publierCreation(t, createur, le);
        return new Creation(lue(t), true);
    }

    @Override
    public List<TacheLue> terminerPourService(String client, CibleInterne cible) {
        Instant le = horloge.instant();
        List<Tache> visees = visees(cible);
        for (Tache t : visees) {
            EtatTache avant = t.terminerParService(le);
            if (avant != null) {
                consigner(t, client, ActionHistorique.TRANSITION, avant.name() + " vers TERMINEE : terminée par le service " + serviceDe(t, client), le);
                outbox.publish(
                    AGREGAT,
                    t.getId(),
                    TacheTermineePayload.TYPE,
                    new TacheTermineePayload(t.getId(), t.getEtat().name(), t.getResponsableId(), t.getCreateurId(), t.getOrigineType().name(), t.getOrigineReference(), client, le)
                );
            }
        }
        return visees.stream().map(this::lue).toList();
    }

    @Override
    public List<TacheLue> rouvrirPourService(String client, CibleInterne cible) {
        Instant le = horloge.instant();
        List<Tache> visees = visees(cible);
        for (Tache t : visees) {
            EtatTache avant = t.rouvrirParService(le);
            if (avant != null) {
                consigner(t, client, ActionHistorique.TRANSITION, "TERMINEE vers EN_COURS : rouverte par le service " + serviceDe(t, client), le);
            }
        }
        return visees.stream().map(this::lue).toList();
    }

    /**
     * Tâches visées par un autre service : celles de l'origine (référence, et type s'il est donné), créées par un
     * service (jamais une tâche INTERNE), restreintes au responsable s'il est donné.
     */
    private List<Tache> visees(CibleInterne c) {
        if (c == null || c.origine() == null) {
            throw PilotageException.champ("origine", "Origine absente");
        }
        String reference = ReglesActivite.texteObligatoire("origine.reference", c.origine().reference(), 200);
        TypeOrigine type = c.origine().type() == null || c.origine().type().isBlank() ? null : ReglesActivite.valeur("origine.type", TypeOrigine.class, c.origine().type(), null);
        String responsable = c.responsableId() == null || c.responsableId().isBlank() ? null : c.responsableId().strip();
        return taches
            .findByOrigineReferenceOrderByCreeLe(reference)
            .stream()
            .filter(t -> t.getOrigineType() != TypeOrigine.INTERNE)
            .filter(t -> type == null || t.getOrigineType() == type)
            .filter(t -> responsable == null || responsable.equals(t.getResponsableId()))
            .toList();
    }

    private static String serviceDe(Tache t, String client) {
        return t.getOrigineService() == null ? client : t.getOrigineService();
    }

    private void publierCreation(Tache t, String acteur, Instant le) {
        outbox.publish(
            AGREGAT,
            t.getId(),
            TacheCreeePayload.TYPE,
            new TacheCreeePayload(
                t.getId(),
                t.getEtat().name(),
                t.getPriorite().name(),
                t.getResponsableId(),
                t.getCreateurId(),
                t.getOrigineType().name(),
                t.getOrigineReference(),
                acteur,
                le,
                t.getGroupeId(),
                t.getUniteCible()
            )
        );
        if (t.getResponsableId() != null && !t.getResponsableId().equals(t.getCreateurId())) {
            publierAffectation(t, null, acteur, le);
        }
    }

    /**
     * Confier à un autre : PIL_TASK_ASSIGN (403 PIL_ACTION_RESERVEE), TACHE_CONFIER couvrant la personne
     * (403 PIL_HORS_EQUIPE), compte actif (422 PIL_RESPONSABLE_INVALIDE).
     */
    private void exigerConfiable(Appelant appelant, String responsable) {
        if (!appelant.confie()) {
            throw PilotageException.reservee("Confier une tâche exige la permission PIL_TASK_ASSIGN");
        }
        Membre m = equipes.couvert(appelant, Pouvoir.TACHE_CONFIER, responsable).orElseThrow(PilotageException::horsEquipe);
        if (!m.actif()) {
            throw PilotageException.regle("PIL_RESPONSABLE_INVALIDE", "Le responsable doit être un compte interne actif", Map.of("responsableId", responsable));
        }
    }

    private void decrire(Tache t, TacheSaisie s, Instant le) {
        String titre = ReglesActivite.texteObligatoire("titre", s.titre(), ReglesActivite.TITRE_MAX);
        String description = ReglesActivite.texteFacultatif("description", s.description(), ReglesActivite.DESCRIPTION_MAX);
        LocalDate echeance = ReglesActivite.lireJourFacultatif("echeance", s.echeance());
        PrioriteTache priorite = ReglesActivite.valeur("priorite", PrioriteTache.class, s.priorite(), PrioriteTache.NORMALE);
        t.decrire(titre, description, echeance, priorite, le);
    }

    private static String resume(Tache t) {
        return "Intitulé « " + t.getTitre() + " », échéance " + (t.getEcheance() == null ? "aucune" : t.getEcheance()) + ", priorité " + t.getPriorite() +
            (t.getDescription() == null ? "" : ", description de " + t.getDescription().length() + " caractères");
    }

    private void consigner(Tache t, String acteur, ActionHistorique action, String detail, Instant le) {
        String d = detail == null ? null : detail.length() > 1200 ? detail.substring(0, 1200) : detail;
        historique.save(new TacheHistorique(t.getId(), le, acteur, action, d));
    }

    private Tache trouver(UUID id) {
        return taches.findById(id).orElseThrow(() -> PilotageException.introuvable("PIL_TACHE_INTROUVABLE", id));
    }

    private static boolean liee(Tache t, Appelant appelant) {
        return appelant.id().equals(t.getResponsableId()) || appelant.id().equals(t.getCreateurId());
    }

    private String nom(String id) {
        String n = equipes.noms(List.of(id)).get(id);
        return n == null ? id : n;
    }

    private TacheLue lue(Tache t) {
        List<TacheHistorique> h = historique.findByTacheIdOrderByQuandAsc(t.getId());
        List<String> ids = new ArrayList<>();
        ids.add(t.getCreateurId());
        if (t.getResponsableId() != null) {
            ids.add(t.getResponsableId());
        }
        h.stream().map(TacheHistorique::getActeurId).filter(Objects::nonNull).forEach(ids::add);
        return new TacheLue(t, h, equipes.noms(ids), t.enRetard(ReglesActivite.aujourdhui(horloge)));
    }

    private static String echeanceTexte(Tache t) {
        return t.getEcheance() == null ? null : t.getEcheance().toString();
    }

    // ------------------------------------------------------------------ tâches des étapes de processus (D-87)

    /** 409 PIL_TACHE_PROCESSUS : la tâche suit son étape. */
    private static PilotageException tacheProcessus(String message) {
        return PilotageException.conflit("PIL_TACHE_PROCESSUS", message, Map.of());
    }

    private void signalerProcessus(Tache t, String acteur, boolean prise) {
        if (t.getOrigineType() == TypeOrigine.PROCESSUS) {
            evenements.publishEvent(new TacheProcessusConfiee(t.getId(), t.getOrigineReference(), t.getResponsableId(), acteur, prise));
        }
    }

    /** Demande de tâche d'une étape : à une personne ({@code responsable}) ou à prendre ({@code unite}, {@code profil}). */
    public record TacheEtape(
        String createur,
        String responsable,
        String unite,
        String profil,
        String titre,
        String description,
        LocalDate echeance,
        String reference,
        String lien,
        List<String> destinatairesOffre
    ) {}

    /** Tâche d'une étape activée (origine PROCESSUS, service pilotage), avec ses événements pilotage.task-*. */
    public Tache creerPourEtape(TacheEtape d, Instant le) {
        Tache t = d.responsable() == null ? Tache.pourUniteEtProfil(d.createur(), d.unite(), d.profil(), le) : new Tache(d.createur(), d.responsable(), le);
        t.decrire(tronquer(d.titre(), ReglesActivite.TITRE_MAX), d.description() == null ? null : tronquer(d.description(), ReglesActivite.DESCRIPTION_MAX), d.echeance(), PrioriteTache.NORMALE, le);
        t.provenirDe(TypeOrigine.PROCESSUS, d.reference(), "pilotage", d.lien());
        taches.save(t);
        consigner(
            t,
            d.createur(),
            ActionHistorique.CREEE,
            d.responsable() == null ? "Étape à prendre (unité " + d.unite() + (d.profil() == null ? "" : ", profil " + d.profil()) + ")" : "Étape confiée à " + nom(d.responsable()),
            le
        );
        publierCreation(t, d.createur(), le);
        if (d.responsable() == null) {
            outbox.publish(AGREGAT, t.getId(), TacheOffertePayload.TYPE, new TacheOffertePayload(t.getId(), t.getTitre(), d.unite(), d.destinatairesOffre(), d.createur(), le));
        }
        return t;
    }

    public Optional<Tache> tacheEtape(UUID id) {
        return id == null ? Optional.empty() : taches.findById(id);
    }

    public List<Tache> tachesEtapes(java.util.Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : taches.findAllById(ids);
    }

    /** Étapes en cours dont le compte est responsable (départ d'une personne). */
    public List<Tache> tachesEtapesOuvertesDe(String compte) {
        return taches.processusOuvertesDe(compte, EtatTache.OUVERTS);
    }

    /** L'étape est terminée : sa tâche aussi (pilotage.task-completed.v1). */
    public void terminerPourEtape(UUID id, String acteur, Instant le) {
        tacheEtape(id).ifPresent(t -> {
            EtatTache avant = t.terminerParService(le);
            if (avant != null) {
                consigner(t, acteur, ActionHistorique.TRANSITION, avant.name() + " vers TERMINEE : étape du processus terminée", le);
                outbox.publish(
                    AGREGAT,
                    t.getId(),
                    TacheTermineePayload.TYPE,
                    new TacheTermineePayload(t.getId(), t.getEtat().name(), t.getResponsableId(), t.getCreateurId(), t.getOrigineType().name(), t.getOrigineReference(), acteur, le)
                );
            }
        });
    }

    /** L'instance est annulée : la tâche de l'étape aussi. */
    public void annulerPourEtape(UUID id, String acteur, String motif, Instant le) {
        tacheEtape(id).ifPresent(t -> {
            EtatTache avant = t.annulerParService(le);
            if (avant != null) {
                consigner(t, acteur, ActionHistorique.TRANSITION, avant.name() + " vers ANNULEE : instance annulée" + (motif == null ? "" : " : " + motif), le);
            }
        });
    }

    /** Réaffectation de l'étape à une personne (pilotage.task-assigned.v1). */
    public void confierEtape(UUID id, String responsable, String acteur, String detail, Instant le) {
        tacheEtape(id).ifPresent(t -> {
            String ancien = t.getResponsableId();
            t.confierParService(responsable, le);
            enregistrer(t);
            consigner(t, acteur, ActionHistorique.REAFFECTEE, detail, le);
            publierAffectation(t, ancien, acteur, le);
        });
    }

    /** Remise à prendre de l'étape (départ de son responsable) : pilotage.task-offered.v1 aux porteurs du profil. */
    public void remettreEtapeAPrendre(UUID id, String unite, String profil, String acteur, String detail, List<String> destinataires, Instant le) {
        tacheEtape(id).ifPresent(t -> {
            if (t.remettreAPrendre(unite, profil, le) != null) {
                enregistrer(t);
                consigner(t, acteur, ActionHistorique.REAFFECTEE, detail, le);
                outbox.publish(AGREGAT, t.getId(), TacheOffertePayload.TYPE, new TacheOffertePayload(t.getId(), t.getTitre(), unite, destinataires, acteur, le));
            }
        });
    }

    private static String tronquer(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
