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
import cm.gimac.des.pilotage.domain.event.TachePrisePayload;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private static final Pattern PROFIL = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]*$");
    private static final Pattern PERMISSION = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    /** Nombre de profils requis d'une offre (D-93). */
    static final int PROFILS_MAX = 20;

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
            case "moi" -> r = taches.miennes(
                appelant.id(),
                auMoinsUn(equipes.unitesDe(appelant.id())),
                auMoinsUn(equipes.profilsDe(appelant.id())),
                auMoinsUn(permissionsOffertesPortees(appelant.id())),
                filtre,
                p
            );
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
                r = unites.isEmpty()
                    ? org.springframework.data.domain.Page.empty(p)
                    : taches.deUnites(unites, filtre == null ? EtatTache.A_PRENDRE : filtre, auMoinsUn(equipes.profilsDe(appelant.id())), appelant.id(), p);
            }
            default -> throw PilotageException.champ("vue", "Vue moi, creees, equipe ou unite attendue");
        }
        LocalDate jour = ReglesActivite.aujourdhui(horloge);
        Map<String, String> noms = equipes.noms(r.getContent().stream().flatMap(t -> Stream.of(t.getResponsableId(), t.getCreateurId())).filter(Objects::nonNull).toList());
        return new Page<>(r.getContent().stream().map(t -> new TacheLue(t, List.of(), noms, t.enRetard(jour))).toList(), r.getTotalElements(), p.getPageNumber(), p.getPageSize());
    }

    /**
     * Permissions des offres encore à prendre que porte le compte (D-94) ; annuaire injoignable : aucune (« Mes tâches »
     * ne montre alors que les offres par profil), sans échec de la lecture.
     */
    private Set<String> permissionsOffertesPortees(String compteId) {
        try {
            return equipes.permissionsDe(compteId, taches.permissionsOffertes());
        } catch (PilotageException e) {
            return java.util.Set.of();
        }
    }

    /**
     * Le compte peut prendre l'offre : il porte l'un des profils requis ou l'une des permissions requises (porteurs lus
     * dans MS-IAM, D-94) ; l'unité visée se vérifie à part. Annuaire injoignable : 503 PIL_ORGANISATION_INDISPONIBLE.
     */
    private boolean candidat(Tache t, String compteId) {
        return t.prenablePar(equipes.profilsDe(compteId), equipes.permissionsDe(compteId, t.getPermissionsRequises()));
    }

    /** Requis d'une offre, pour les messages : profils puis permissions. */
    private static String requis(Tache t) {
        List<String> l = new ArrayList<>(t.getProfilsRequis());
        l.addAll(t.getPermissionsRequises());
        return String.join(", ", l);
    }

    /** Liste vide interdite dans « in » : un code qu'aucune unité ni aucun profil ne porte la remplace. */
    private static List<String> auMoinsUn(java.util.Collection<String> codes) {
        return codes.isEmpty() ? List.of("-") : List.copyOf(codes);
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
        if (t.isOffreProfils()) {
            return prendreOffre(appelant, t);
        }
        String unite = t.getUniteCible();
        if (!horsUnite(t) && (unite == null || !equipes.dansUnite(appelant.id(), unite))) {
            if (peutLire(t, appelant)) {
                throw PilotageException.reservee("Seul un membre de l'unité visée prend la tâche");
            }
            throw PilotageException.horsEquipe();
        }
        if (t.getProfilRequis() != null && t.getEtat() == EtatTache.A_PRENDRE && !equipes.profilsDe(appelant.id()).contains(t.getProfilRequis())) {
            if (horsUnite(t) && !peutLire(t, appelant)) {
                throw PilotageException.horsEquipe();
            }
            throw PilotageException.interdit("PIL_PROFIL_REQUIS", "Seuls les porteurs du profil " + t.getProfilRequis() + " prennent cette tâche");
        }
        verifierPriseProcessus(t, appelant.id(), false);
        Instant le = horloge.instant();
        t.confierA(appelant.id(), le);
        enregistrer(t);
        consigner(t, appelant.id(), ActionHistorique.PRISE, "Prise par " + nom(appelant.id()), le);
        publierAffectation(t, null, appelant.id(), le);
        publierPrise(t, appelant.id(), true, le);
        signalerProcessus(t, appelant.id(), true);
        return lue(t);
    }

    /**
     * Prise d'une offre d'un autre service (D-93) : déjà prise, terminée ou annulée : 409 PIL_TRANSITION_INTERDITE (403
     * PIL_HORS_EQUIPE pour qui ne porte aucun des profils et ne lit pas la tâche) ;
     * appelant hors de l'unité visée : 403 PIL_ACTION_RESERVEE ; sans l'un des profils ni l'une des permissions requis :
     * 403 PIL_PROFIL_REQUIS.
     */
    private TacheLue prendreOffre(Appelant appelant, Tache t) {
        if (t.getEtat() != EtatTache.A_PRENDRE) {
            boolean candidat = t.getProfilsRequis().stream().anyMatch(equipes.profilsDe(appelant.id())::contains)
                || !equipes.permissionsDe(appelant.id(), t.getPermissionsRequises()).isEmpty();
            if (!candidat && !peutLire(t, appelant)) {
                throw PilotageException.horsEquipe();
            }
            throw PilotageException.conflit("PIL_TRANSITION_INTERDITE", "La tâche n'est plus à prendre", Map.of("de", t.getEtat().name(), "vers", EtatTache.OUVERTE.name()));
        }
        if (t.getUniteCible() != null && !equipes.dansUnite(appelant.id(), t.getUniteCible())) {
            throw PilotageException.reservee("Seul un membre de l'unité visée prend la tâche");
        }
        if (!candidat(t, appelant.id())) {
            throw PilotageException.interdit("PIL_PROFIL_REQUIS", "Seuls les porteurs de l'un des profils ou de l'une des permissions " + requis(t) + " prennent cette tâche");
        }
        Instant le = horloge.instant();
        t.confierA(appelant.id(), le);
        enregistrer(t);
        consigner(t, appelant.id(), ActionHistorique.PRISE, "Prise par " + nom(appelant.id()), le);
        publierAffectation(t, null, appelant.id(), le);
        publierPrise(t, appelant.id(), true, le);
        return lue(t);
    }

    /** pilotage.task-taken.v1 : la tâche à prendre a trouvé son responsable (prise ou attribution, D-93). */
    private void publierPrise(Tache t, String acteur, boolean prise, Instant le) {
        outbox.publish(
            AGREGAT,
            t.getId(),
            TachePrisePayload.TYPE,
            new TachePrisePayload(
                t.getId(),
                new TachePrisePayload.Origine(t.getOrigineType().name(), t.getOrigineReference(), t.getOrigineService(), t.getOrigineLien()),
                t.getResponsableId(),
                acteur,
                prise ? "PRISE" : "ATTRIBUTION",
                List.copyOf(t.getProfilsRequis()),
                t.getProfilRequis(),
                t.getUniteCible(),
                t.getTitre(),
                le,
                List.copyOf(t.getPermissionsRequises())
            )
        );
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
        if (t.isOffreProfils() && !candidat(t, cible)) {
            throw PilotageException.regle(
                "PIL_RESPONSABLE_INVALIDE",
                "Le responsable doit porter l'un des profils ou l'une des permissions requis",
                Map.of("responsableId", cible, "profilsRequis", List.copyOf(t.getProfilsRequis()), "permissionsRequises", List.copyOf(t.getPermissionsRequises()))
            );
        }
        verifierPriseProcessus(t, cible, true);
        Instant le = horloge.instant();
        t.confierA(cible, le);
        enregistrer(t);
        consigner(t, appelant.id(), ActionHistorique.ATTRIBUEE, "Attribuée à " + nom(cible), le);
        publierAffectation(t, null, appelant.id(), le);
        publierPrise(t, appelant.id(), false, le);
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
        if (t.isOffreProfils() && candidatTolerant(t, appelant.id()) && (unite == null || equipes.dansUnite(appelant.id(), unite))) {
            return true;
        }
        if (horsUnite(t) && t.getEtat() == EtatTache.A_PRENDRE && equipes.profilsDe(appelant.id()).contains(t.getProfilRequis())) {
            return true;
        }
        if (unite != null && (equipes.dansUnite(appelant.id(), unite) || equipes.pouvoirs(appelant.id()).dirige(unite))) {
            return true;
        }
        return t.getResponsableId() != null && equipes.peutLire(appelant, Pouvoir.TACHE_SUIVRE, t.getResponsableId());
    }

    /**
     * Étape de processus offerte hors de l'unité (désignation hors de l'unité, D-87) : à prendre sans unité visée par
     * tout porteur du profil, où qu'il soit dans le GIMAC.
     */
    private static boolean horsUnite(Tache t) {
        return !t.isOffreProfils() && t.getUniteCible() == null && t.getProfilRequis() != null && t.getOrigineType() == TypeOrigine.PROCESSUS;
    }

    /** {@link #candidat} sans échec : annuaire injoignable, seuls les profils comptent (lecture d'une tâche). */
    private boolean candidatTolerant(Tache t, String compteId) {
        try {
            return candidat(t, compteId);
        } catch (PilotageException e) {
            return t.prenableParProfils(equipes.profilsDe(compteId));
        }
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
        // DDR-28 : la tâche d'une décision (transition de statut à prononcer) se clôt quand le service prononce ou refuse.
        if (t.getOrigineType() == TypeOrigine.DECISION && (cible == EtatTache.TERMINEE || cible == EtatTache.ANNULEE)) {
            throw PilotageException.conflit(
                "PIL_TACHE_DECISION",
                "La tâche d'une décision se clôt quand le service prononce ou refuse la proposition",
                Map.of()
            );
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
        if (d.profilsRequis() != null || d.permissionsRequises() != null || texte(d.uniteCible()) != null) {
            if (texte(d.responsableId()) != null) {
                throw PilotageException.champ(
                    d.profilsRequis() == null && d.permissionsRequises() != null ? "permissionsRequises" : "profilsRequis",
                    "Une tâche se confie à un responsable ou se propose à des profils ou des permissions, pas les deux"
                );
            }
            return offrirPourService(client, d, type, reference, service, lien);
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

    /**
     * Offre d'un autre service (D-93, D-94) : 1 à 20 profils (codes de MS-IAM) et/ou 1 à 20 permissions (l'un des deux
     * au moins ; l'un des profils ou l'une des permissions suffit), unité facultative et connue ; idempotente par
     * origine.reference seule ; pilotage.task-created.v1 puis pilotage.task-offered.v1 aux porteurs actifs des profils et
     * des permissions (dans l'unité si elle est donnée).
     */
    private Creation offrirPourService(String client, TacheInterne d, TypeOrigine type, String reference, String service, String lien) {
        if (d.profilsRequis() == null && d.permissionsRequises() == null) {
            throw PilotageException.champ("profilsRequis", "Une tâche à prendre exige des profils requis ou des permissions requises");
        }
        List<String> profils = d.profilsRequis() == null ? List.of() : profilsRequis(d.profilsRequis());
        List<String> permissions = d.permissionsRequises() == null ? List.of() : permissionsRequises(d.permissionsRequises());
        String unite = texte(d.uniteCible());
        if (unite != null && !UNITE.matcher(unite).matches()) {
            throw PilotageException.champ("uniteCible", "Code d'unité attendu (par exemple POLE_RESEAU)");
        }
        Optional<Tache> existante = taches.findFirstByOrigineReferenceAndOffreProfilsTrue(reference);
        if (existante.isPresent()) {
            return new Creation(lue(existante.orElseThrow()), false);
        }
        if (unite != null && equipes.membresUnite(unite, null).isEmpty()) {
            throw PilotageException.regle("PIL_UNITE_INCONNUE", "Unité inconnue de l'organisation", Map.of("uniteCible", unite));
        }
        String createur = d.createurId() == null || d.createurId().isBlank() ? client : d.createurId().strip();
        Instant le = horloge.instant();
        Tache t = Tache.pourOffre(createur, unite, profils, permissions, le);
        decrire(t, new TacheSaisie(d.titre(), d.description(), d.echeance(), d.priorite(), null), le);
        t.provenirDe(type, reference, service, lien);
        try {
            taches.saveAndFlush(t);
        } catch (DataIntegrityViolationException e) {
            throw PilotageException.conflit("PIL_TACHE_EXISTANTE", "Tâche déjà créée pour cette origine", Map.of("reference", reference));
        }
        consigner(
            t,
            createur,
            ActionHistorique.CREEE,
            "Créée par le service " + (service == null ? client : service) + ", à prendre par les porteurs de " + requis(t) + (unite == null ? "" : " (unité " + unite + ")"),
            le
        );
        publierCreation(t, createur, le);
        List<String> destinataires = destinatairesOffre(unite, profils, permissions);
        outbox.publish(AGREGAT, t.getId(), TacheOffertePayload.TYPE, new TacheOffertePayload(t.getId(), t.getTitre(), unite, destinataires, createur, le, lienNotification(t)));
        return new Creation(lue(t), true);
    }

    /**
     * Destinataires de pilotage.task-offered.v1 d'une offre : porteurs actifs des profils et des permissions, dans
     * l'unité (sous-unités comprises) si elle est donnée.
     */
    private List<String> destinatairesOffre(String unite, List<String> profils, List<String> permissions) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (unite == null) {
            ids.addAll(equipes.porteursProfils(profils));
            ids.addAll(equipes.porteursPermissions(permissions));
            return List.copyOf(ids);
        }
        profils.stream().flatMap(p -> equipes.membresUnite(unite, p).map(u -> u.actifs()).orElse(List.of()).stream()).map(m -> m.id()).forEach(ids::add);
        if (!permissions.isEmpty()) {
            Set<String> membres = new HashSet<>();
            equipes.membresUnite(unite, null).map(u -> u.actifs()).orElse(List.of()).forEach(m -> membres.add(m.id()));
            equipes.porteursPermissions(permissions).stream().filter(membres::contains).forEach(ids::add);
        }
        return List.copyOf(ids);
    }

    /** Permissions requises : 1 à 20 codes distincts (majuscule, puis majuscules, chiffres ou « _ »), 80 caractères au plus. */
    static List<String> permissionsRequises(List<String> saisies) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (String c : saisies) {
            String v = texte(c);
            if (v == null || v.length() > 80 || !PERMISSION.matcher(v).matches()) {
                throw PilotageException.champ("permissionsRequises", "Code de permission invalide : " + c);
            }
            codes.add(v);
        }
        if (codes.isEmpty() || codes.size() > PROFILS_MAX) {
            throw PilotageException.invalide(
                "PIL_REQUETE_INVALIDE",
                "De 1 à " + PROFILS_MAX + " permissions requises attendues",
                Map.of("champ", "permissionsRequises", "nombre", codes.size())
            );
        }
        return List.copyOf(codes);
    }

    /** Profils requis : 1 à 20 codes distincts (lettres, chiffres, « _ », « - », « . »), 80 caractères au plus. */
    static List<String> profilsRequis(List<String> saisis) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        if (saisis != null) {
            for (String c : saisis) {
                String v = texte(c);
                if (v == null || v.length() > 80 || !PROFIL.matcher(v).matches()) {
                    throw PilotageException.champ("profilsRequis", "Code de profil invalide : " + c);
                }
                codes.add(v);
            }
        }
        if (codes.isEmpty() || codes.size() > PROFILS_MAX) {
            throw PilotageException.invalide(
                "PIL_REQUETE_INVALIDE",
                "De 1 à " + PROFILS_MAX + " profils requis attendus",
                Map.of("champ", "profilsRequis", "nombre", codes.size())
            );
        }
        return List.copyOf(codes);
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
            if (cible.aPrendre()) {
                remettreOffre(t, client, le);
                continue;
            }
            EtatTache avant = t.rouvrirParService(le);
            if (avant != null) {
                consigner(t, client, ActionHistorique.TRANSITION, "TERMINEE vers " + t.getEtat().name() + " : rouverte par le service " + serviceDe(t, client), le);
            }
        }
        return visees.stream().map(this::lue).toList();
    }

    /**
     * Offre remise à prendre (D-94) : sans responsable, pilotage.task-offered.v1 de nouveau aux porteurs des profils et
     * des permissions (dans l'unité si elle est donnée).
     */
    private void remettreOffre(Tache t, String client, Instant le) {
        String ancien = t.getResponsableId();
        EtatTache avant = t.remettreOffreAPrendre(le);
        if (avant == null) {
            return;
        }
        enregistrer(t);
        consigner(
            t,
            client,
            ActionHistorique.TRANSITION,
            avant.name() + " vers A_PRENDRE : remise à prendre par le service " + serviceDe(t, client) + (ancien == null ? "" : " (retirée à " + nom(ancien) + ")"),
            le
        );
        List<String> destinataires = destinatairesOffre(t.getUniteCible(), List.copyOf(t.getProfilsRequis()), List.copyOf(t.getPermissionsRequises()));
        outbox.publish(
            AGREGAT,
            t.getId(),
            TacheOffertePayload.TYPE,
            new TacheOffertePayload(t.getId(), t.getTitre(), t.getUniteCible(), destinataires, t.getCreateurId(), le, lienNotification(t))
        );
    }

    /**
     * Lien de la notification d'une offre (D-98) : celui de l'origine pour une offre d'origine DECISION (l'acte se fait sur
     * cet écran, sans prise préalable : prononcer une transition de statut) ; sinon nul (tâches à prendre).
     */
    static String lienNotification(Tache t) {
        return t.getOrigineType() == TypeOrigine.DECISION ? t.getOrigineLien() : null;
    }

    /**
     * Tâches visées par un autre service : celles de l'origine (référence, et type s'il est donné), créées par un
     * service (jamais une tâche INTERNE), restreintes au responsable s'il est donné ; {@code aPrendre} : l'offre seule.
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
            .filter(t -> c.aPrendre() ? t.isOffreProfils() : responsable == null || responsable.equals(t.getResponsableId()))
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

    /** D-99 : la tâche d'une étape n'est ni prise par l'initiateur de l'instance ni attribuée à lui (refus du moteur). */
    private void verifierPriseProcessus(Tache t, String compte, boolean attribution) {
        if (t.getOrigineType() == TypeOrigine.PROCESSUS) {
            evenements.publishEvent(new PriseTacheProcessus(t.getId(), t.getOrigineReference(), compte, attribution));
        }
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
        // L'historique porte une clé étrangère vers la tâche : sur PostgreSQL, son insertion ne doit pas la devancer.
        taches.saveAndFlush(t);
        consigner(
            t,
            d.createur(),
            ActionHistorique.CREEE,
            d.responsable() == null ? "Étape à prendre (" + cible(d.unite(), d.profil()) + ")" : "Étape confiée à " + nom(d.responsable()),
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

    /** L'étape est terminée : sa tâche aussi (pilotage.task-completed.v1), au nom de l'acteur si elle était à prendre. */
    public void terminerPourEtape(UUID id, String acteur, Instant le) {
        tacheEtape(id).ifPresent(t -> {
            if (t.getResponsableId() == null && t.getEtat() == EtatTache.A_PRENDRE && acteur != null) {
                // Remise à prendre pendant les signatures ou la validation (départ) : terminée au nom de l'acteur.
                t.confierParService(acteur, le);
            }
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

    // ------------------------------------------------------------------ tâches d'attente : validation et signatures (D-93)

    /** Tâche d'une étape (origine PROCESSUS) portant cette référence, la plus récente. */
    public Optional<Tache> tacheEtapeParReference(String reference) {
        List<Tache> l = taches.findByOrigineReferenceOrderByCreeLe(reference).stream().filter(t -> t.getOrigineType() == TypeOrigine.PROCESSUS).toList();
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(l.size() - 1));
    }

    /**
     * Tâche d'attente (validation ou signature d'une étape, D-93) ouverte pour l'affectation demandée : créée si
     * elle n'existe pas, rouverte si elle était terminée ou annulée, réalignée si elle est ouverte pour une autre
     * affectation (confiée à la personne, ou remise à prendre par le profil). Une seule tâche par référence.
     */
    public Tache ouvrirAttente(TacheEtape d, Instant le) {
        Optional<Tache> existante = tacheEtapeParReference(d.reference());
        if (existante.isEmpty()) {
            return creerPourEtape(d, le);
        }
        Tache t = existante.orElseThrow();
        EtatTache rouverte = t.rouvrirPour(d.responsable(), d.unite(), d.profil(), le);
        if (rouverte != null) {
            t.decrire(t.getTitre(), t.getDescription(), d.echeance(), t.getPriorite(), le);
            enregistrer(t);
            consigner(t, d.createur(), ActionHistorique.TRANSITION, rouverte.name() + " vers " + t.getEtat().name() + " : nouvelle attente de l'étape", le);
            publierAttente(t, null, d, le);
            return t;
        }
        if (d.responsable() != null) {
            if (!d.responsable().equals(t.getResponsableId())) {
                String ancien = t.getResponsableId();
                t.confierParService(d.responsable(), le);
                enregistrer(t);
                consigner(t, d.createur(), ActionHistorique.REAFFECTEE, "Confiée à " + nom(d.responsable()), le);
                publierAffectation(t, ancien, d.createur(), le);
            }
        } else if (t.getResponsableId() != null || !Objects.equals(t.getProfilRequis(), d.profil()) || !Objects.equals(t.getUniteCible(), d.unite())) {
            String ancien = t.getResponsableId();
            if (t.remettreAPrendre(d.unite(), d.profil(), le) != null) {
                enregistrer(t);
                consigner(t, d.createur(), ActionHistorique.REAFFECTEE, (ancien == null ? "" : "Retirée à " + nom(ancien) + " : ") + "à prendre (" + cible(d.unite(), d.profil()) + ")", le);
                outbox.publish(AGREGAT, t.getId(), TacheOffertePayload.TYPE, new TacheOffertePayload(t.getId(), t.getTitre(), d.unite(), d.destinatairesOffre(), d.createur(), le));
            }
        }
        return t;
    }

    private void publierAttente(Tache t, String ancien, TacheEtape d, Instant le) {
        if (t.getResponsableId() != null) {
            publierAffectation(t, ancien, d.createur(), le);
        } else {
            outbox.publish(AGREGAT, t.getId(), TacheOffertePayload.TYPE, new TacheOffertePayload(t.getId(), t.getTitre(), d.unite(), d.destinatairesOffre(), d.createur(), le));
        }
    }

    /**
     * Attente satisfaite (signature apposée, validation ou renvoi décidé) : la tâche encore ouverte est confiée à
     * l'acteur si elle était à prendre ou à un autre, puis terminée (pilotage.task-completed.v1).
     */
    public void clore(String reference, String acteur, Instant le) {
        tacheEtapeParReference(reference).filter(t -> t.getEtat().estOuvert()).ifPresent(t -> {
            if (acteur != null && !acteur.equals(t.getResponsableId())) {
                t.confierParService(acteur, le);
            }
            terminerPourEtape(t.getId(), acteur, le);
        });
    }

    /** Attente sans objet (instance annulée) : la tâche encore ouverte est annulée. */
    public void annulerAttente(String reference, String acteur, String motif, Instant le) {
        tacheEtapeParReference(reference).ifPresent(t -> annulerPourEtape(t.getId(), acteur, motif, le));
    }

    /** « unité U, profil P », ou « tout le GIMAC, profil P » pour une étape offerte hors de l'unité. */
    private static String cible(String unite, String profil) {
        return (unite == null ? "tout le GIMAC" : "unité " + unite) + (profil == null ? "" : ", profil " + profil);
    }

    private static String tronquer(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
