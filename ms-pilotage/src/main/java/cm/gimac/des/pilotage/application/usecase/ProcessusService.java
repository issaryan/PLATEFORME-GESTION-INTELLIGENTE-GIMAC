package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.MembreProfil;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.UniteMembres;
import cm.gimac.des.pilotage.domain.event.ProcessusPubliePayload;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.processus.ActionProcessus;
import cm.gimac.des.pilotage.domain.processus.Anomalie;
import cm.gimac.des.pilotage.domain.processus.ChampFormulaire;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.EtatProcessus;
import cm.gimac.des.pilotage.domain.processus.Formulaire;
import cm.gimac.des.pilotage.domain.processus.GraviteAnomalie;
import cm.gimac.des.pilotage.domain.processus.HistoriqueProcessus;
import cm.gimac.des.pilotage.domain.processus.Initiateurs;
import cm.gimac.des.pilotage.domain.processus.Intervenant;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.RegleInitiateur;
import cm.gimac.des.pilotage.domain.processus.TypeChampFormulaire;
import cm.gimac.des.pilotage.domain.service.ControleProcessus;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import cm.gimac.des.pilotage.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.pilotage.repository.HistoriqueProcessusRepository;
import cm.gimac.des.pilotage.repository.ProcessusRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Définitions des processus d'unité (D-87 §1) : brouillon (création, modification, suppression), contrôle de
 * publication (structure : ControleProcessus ; organisation : unité connue, profil existant, personne interne
 * active, au moins une personne active couvrant chaque profil exigé dans l'unité ou, à défaut, ailleurs dans le GIMAC,
 * chef d'unité désigné), candidats d'un intervenant, publication (la
 * version publiée précédente est retirée), nouvelle version, retrait. Historique en ajout seul ; événement
 * pilotage.process-published.v1 par l'outbox.
 */
@Service
@Transactional
public class ProcessusService implements ProcessusUseCase {

    static final String AGREGAT = "process";

    private final ProcessusRepository processus;
    private final HistoriqueProcessusRepository historique;
    private final Equipes equipes;
    private final AccesProcessus acces;
    private final ResolutionIntervenants resolution;
    private final OutboxPublisher outbox;
    private final Clock horloge;

    public ProcessusService(
        ProcessusRepository processus,
        HistoriqueProcessusRepository historique,
        Equipes equipes,
        AccesProcessus acces,
        ResolutionIntervenants resolution,
        OutboxPublisher outbox,
        Clock horloge
    ) {
        this.processus = processus;
        this.historique = historique;
        this.equipes = equipes;
        this.acces = acces;
        this.resolution = resolution;
        this.outbox = outbox;
        this.horloge = horloge;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessusLu> lister(Appelant appelant, String unite, String etat, String q) {
        boolean admin = acces.litToutes(appelant);
        EtatProcessus filtre = etat == null || etat.isBlank() ? null : ReglesActivite.valeur("etat", EtatProcessus.class, etat, null);
        String u = unite == null || unite.isBlank() ? null : SaisieProcessus.unite("unite", unite);
        List<String> unites = u != null ? List.of(u) : admin ? List.of("-") : new ArrayList<>(acces.unitesLisibles(appelant));
        if (unites.isEmpty()) {
            return List.of();
        }
        String motif = q == null || q.isBlank() ? null : "%" + q.strip().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
        List<Processus> l = processus.visibles(admin && u == null, unites, filtre, motif);
        Map<String, Boolean> lecture = new HashMap<>();
        Map<String, Boolean> definition = new HashMap<>();
        Map<String, Boolean> lancement = new HashMap<>();
        List<Processus> visibles = l
            .stream()
            .filter(p -> lecture.computeIfAbsent(p.getUnite(), x -> acces.lit(appelant, x)))
            .filter(p -> p.getEtat() != EtatProcessus.BROUILLON || definition.computeIfAbsent(p.getUnite(), x -> acces.definit(appelant, x)))
            .toList();
        // D-100 : noms des personnes désignées du public, pour le résumé « Visible et lançable par … » de la liste.
        Map<String, String> noms = equipes.noms(
            visibles
                .stream()
                .flatMap(p -> Stream.concat(Stream.of(p.getCreePar(), p.getModifiePar(), p.getPublieePar()), p.getInitiateurs().personnes().stream()))
                .filter(Objects::nonNull)
                .toList()
        );
        return visibles
            .stream()
            .map(p ->
                new ProcessusLu(
                    p,
                    noms,
                    droits(
                        p,
                        definition.computeIfAbsent(p.getUnite(), x -> acces.definit(appelant, x)),
                        p.getEtat() == EtatProcessus.PUBLIEE && lancement.computeIfAbsent(p.getId().toString(), x -> acces.lance(appelant, p))
                    ),
                    List.of(),
                    List.of(),
                    List.of()
                )
            )
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupeLancables> lancables(Appelant appelant, String q) {
        String motif = q == null || q.isBlank() ? null : "%" + q.strip().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
        List<Processus> publiees = processus.visibles(true, List.of("-"), EtatProcessus.PUBLIEE, motif).stream().filter(p -> acces.lance(appelant, p)).toList();
        Map<String, String> noms = equipes.noms(
            publiees
                .stream()
                .flatMap(p -> Stream.concat(Stream.of(p.getModifiePar(), p.getPublieePar()), p.getInitiateurs().personnes().stream()))
                .filter(Objects::nonNull)
                .toList()
        );
        Map<String, List<ProcessusLu>> parUnite = new java.util.TreeMap<>();
        for (Processus p : publiees) {
            parUnite.computeIfAbsent(p.getUnite(), u -> new ArrayList<>()).add(new ProcessusLu(p, noms, droits(p, false, true), List.of(), List.of(), List.of()));
        }
        return parUnite.entrySet().stream().map(e -> new GroupeLancables(e.getKey(), List.copyOf(e.getValue()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PorteeCatalogue portee(Appelant appelant) {
        return acces.portee(appelant);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessusLu lire(Appelant appelant, UUID id) {
        Processus p = trouver(id);
        exigerLecture(appelant, p);
        return lu(appelant, p);
    }

    @Override
    public ProcessusLu creer(Appelant appelant, ProcessusSaisie s) {
        if (s == null) {
            throw PilotageException.champ("code", "Corps de la requête absent");
        }
        String code = SaisieProcessus.codeProcessus(s.code());
        String unite = SaisieProcessus.unite("unite", s.unite());
        if (!acces.definit(appelant, unite)) {
            throw AccesProcessus.interdit("Définir les processus de l'unité " + unite + " exige PROCESSUS_DEFINIR couvrant l'unité ou REF_METIER_ADMIN");
        }
        if (equipes.membresUnite(unite, null).isEmpty()) {
            throw PilotageException.regle("PIL_PROCESSUS_UNITE_INCONNUE", "Unité inconnue", Map.of("unite", unite));
        }
        if (processus.existsByCode(code)) {
            throw PilotageException.conflit("PIL_PROCESSUS_CODE_PRIS", "Ce code de processus est déjà pris", Map.of("code", code));
        }
        Instant le = horloge.instant();
        Processus p = Processus.brouillon(code, unite, appelant.id(), le);
        decrire(p, s, appelant, le);
        try {
            processus.saveAndFlush(p);
        } catch (DataIntegrityViolationException e) {
            throw PilotageException.conflit("PIL_PROCESSUS_CODE_PRIS", "Ce code de processus est déjà pris", Map.of("code", code));
        }
        consigner(p, appelant.id(), ActionProcessus.CREE, "Brouillon v1 de " + code + " (unité " + unite + ")" + titre(appelant, p), le);
        return lu(appelant, p);
    }

    @Override
    public ProcessusLu modifier(Appelant appelant, UUID id, ProcessusSaisie s) {
        Processus p = trouver(id);
        exigerDefinition(appelant, p);
        p.exigerBrouillon();
        if (s == null) {
            throw PilotageException.champ("libelle", "Corps de la requête absent");
        }
        if (s.code() != null && !s.code().isBlank() && !s.code().strip().equals(p.getCode())) {
            throw PilotageException.champ("code", "Le code d'un processus ne change pas");
        }
        if (s.unite() != null && !s.unite().isBlank() && !s.unite().strip().equals(p.getUnite())) {
            throw PilotageException.champ("unite", "L'unité propriétaire d'un processus ne change pas");
        }
        Instant le = horloge.instant();
        decrire(p, s, appelant, le);
        processus.save(p);
        consigner(p, appelant.id(), ActionProcessus.MODIFIE, p.getEtapes().size() + " étape(s)" + titre(appelant, p), le);
        return lu(appelant, p);
    }

    @Override
    public void supprimer(Appelant appelant, UUID id) {
        Processus p = trouver(id);
        exigerDefinition(appelant, p);
        p.exigerBrouillon();
        consigner(p, appelant.id(), ActionProcessus.SUPPRIME, "Brouillon v" + p.getVersion() + " supprimé" + titre(appelant, p), horloge.instant());
        processus.delete(p);
    }

    @Override
    @Transactional(readOnly = true)
    public Controle controler(Appelant appelant, UUID id) {
        Processus p = trouver(id);
        exigerLecture(appelant, p);
        List<Anomalie> a = anomalies(p);
        return new Controle(a.stream().noneMatch(x -> x.gravite() == GraviteAnomalie.BLOQUANTE), a);
    }

    @Override
    public ProcessusLu publier(Appelant appelant, UUID id, String motif, boolean leverAvertissements) {
        Processus p = trouver(id);
        exigerDefinition(appelant, p);
        p.exigerBrouillon();
        List<Anomalie> a = anomalies(p);
        List<Anomalie> bloquantes = a.stream().filter(x -> x.gravite() == GraviteAnomalie.BLOQUANTE).toList();
        if (!bloquantes.isEmpty()) {
            throw PilotageException.regle("PIL_PROCESSUS_ANOMALIES", "La définition comporte des anomalies bloquantes", Map.of("anomalies", bloquantes));
        }
        List<Anomalie> levables = a.stream().filter(x -> x.gravite() == GraviteAnomalie.LEVABLE).toList();
        if (!levables.isEmpty() && !leverAvertissements) {
            throw PilotageException.regle(
                "PIL_PROCESSUS_AVERTISSEMENTS",
                "Avertissements bloquants : publier en les levant, avec un motif",
                Map.of("anomalies", levables)
            );
        }
        String m = SaisieProcessus.motif(motif, !levables.isEmpty());
        Instant le = horloge.instant();
        UUID retiree = null;
        Optional<Processus> precedente = processus.findFirstByCodeAndEtat(p.getCode(), EtatProcessus.PUBLIEE);
        if (precedente.isPresent()) {
            Processus v = precedente.orElseThrow();
            v.retirer(appelant.id(), "Remplacée par la version " + p.getVersion(), le);
            processus.saveAndFlush(v);
            consigner(v, appelant.id(), ActionProcessus.RETIRE, "Remplacée par la version " + p.getVersion() + titre(appelant, p), le);
            retiree = v.getId();
        }
        p.publier(appelant.id(), m, !levables.isEmpty(), le);
        processus.saveAndFlush(p);
        consigner(
            p,
            appelant.id(),
            ActionProcessus.PUBLIE,
            "Version " + p.getVersion() + " publiée" + (m == null ? "" : " : " + m) + (levables.isEmpty() ? "" : " (" + levables.size() + " avertissement(s) levé(s))") + titre(appelant, p),
            le
        );
        outbox.publish(
            AGREGAT,
            p.getId(),
            ProcessusPubliePayload.TYPE,
            new ProcessusPubliePayload(p.getId(), p.getCode(), p.getVersion(), p.getUnite(), p.getLibelle(), appelant.id(), m, !levables.isEmpty(), retiree, le)
        );
        return lu(appelant, p);
    }

    @Override
    public ProcessusLu nouvelleVersion(Appelant appelant, UUID id) {
        Processus p = trouver(id);
        exigerDefinition(appelant, p);
        if (processus.findFirstByCodeAndEtat(p.getCode(), EtatProcessus.BROUILLON).isPresent()) {
            throw PilotageException.conflit("PIL_PROCESSUS_BROUILLON_EXISTANT", "Un brouillon de ce processus existe déjà", Map.of("code", p.getCode()));
        }
        Instant le = horloge.instant();
        Processus v = p.nouvelleVersion(processus.derniereVersion(p.getCode()) + 1, appelant.id(), le);
        processus.saveAndFlush(v);
        consigner(v, appelant.id(), ActionProcessus.VERSION_CREEE, "Brouillon v" + v.getVersion() + " depuis la version " + p.getVersion() + titre(appelant, p), le);
        return lu(appelant, v);
    }

    @Override
    public ProcessusLu retirer(Appelant appelant, UUID id, String motif) {
        Processus p = trouver(id);
        exigerDefinition(appelant, p);
        String m = SaisieProcessus.motif(motif, true);
        Instant le = horloge.instant();
        p.retirer(appelant.id(), m, le);
        processus.save(p);
        consigner(p, appelant.id(), ActionProcessus.RETIRE, "Version " + p.getVersion() + " retirée : " + m + titre(appelant, p), le);
        return lu(appelant, p);
    }

    @Override
    @Transactional(readOnly = true)
    public Candidats candidats(Appelant appelant, String unite, String profil) {
        String u = SaisieProcessus.unite("unite", unite);
        String p = profil == null || profil.isBlank() ? null : SaisieProcessus.profil("profil", profil);
        if (equipes.membresUnite(u, null).isEmpty()) {
            throw PilotageException.regle("PIL_PROCESSUS_UNITE_INCONNUE", "Unité inconnue", Map.of("unite", u));
        }
        return resolution.candidats(u, p);
    }

    // ------------------------------------------------------------------ contrôle de publication (§1)

    /** Anomalies de structure et d'organisation, sans doublon. */
    List<Anomalie> anomalies(Processus p) {
        List<Anomalie> a = new ArrayList<>(ControleProcessus.structure(p.getEtapes()));
        Map<String, Optional<UniteMembres>> cache = new HashMap<>();
        if (membres(cache, p.getUnite(), null).isEmpty()) {
            a.add(Anomalie.bloquante("UNITE_INCONNUE", null, "unite", "Unité propriétaire inconnue : " + p.getUnite()));
        }
        boolean brouillon = p.getEtat() == EtatProcessus.BROUILLON;
        for (ControleProcessus.Role r : ControleProcessus.roles(p.getEtapes())) {
            Intervenant i = r.intervenant();
            if (i == null || i.mode() == null) {
                continue;
            }
            String unite = i.uniteOu(p.getUnite());
            if (membres(cache, unite, null).isEmpty()) {
                a.add(Anomalie.bloquante("UNITE_INCONNUE", r.etape(), r.element(), "Unité inconnue : " + unite));
                continue;
            }
            if (i.mode() == ModeIntervenant.PROFIL && i.profil() != null) {
                profil(cache, a, r, unite, i.profil(), r.element());
            }
            if (i.remplacement() != null) {
                profil(cache, a, r, unite, i.remplacement(), r.element() + ".remplacement");
            }
            if (i.mode() == ModeIntervenant.PERSONNE && i.personneId() != null) {
                personne(a, r, i.personneId(), brouillon);
            }
            if (i.mode() == ModeIntervenant.CHEF_UNITE && membres(cache, unite, null).map(UniteMembres::responsableId).isEmpty()) {
                a.add(Anomalie.levable("CHEF_ABSENT", r.etape(), r.element(), "L'unité " + unite + " n'a pas de chef désigné"));
            }
        }
        lancement(p, a, cache);
        Set<String> vues = new LinkedHashSet<>();
        return a.stream().filter(x -> vues.add(x.code() + "|" + x.etape() + "|" + x.element() + "|" + x.message())).toList();
    }

    /**
     * Public et formulaire de lancement (D-98, D-100) : une règle UNITES, PROFILS, PERMISSIONS ou PERSONNES sans
     * paramètre, une unité ou un profil inconnus, une personne désignée inconnue, externe ou inactive, un code de champ en
     * double, une liste sans valeur sont bloquants.
     */
    private void lancement(Processus p, List<Anomalie> a, Map<String, Optional<UniteMembres>> cache) {
        Initiateurs i = p.getInitiateurs();
        if (i.a(RegleInitiateur.UNITES) && i.unites().isEmpty()) {
            a.add(Anomalie.bloquante("INITIATEURS_INCOMPLETS", null, "initiateurs.unites", "Règle UNITES sans unité désignée"));
        }
        if (i.a(RegleInitiateur.PROFILS) && i.profils().isEmpty()) {
            a.add(Anomalie.bloquante("INITIATEURS_INCOMPLETS", null, "initiateurs.profils", "Règle PROFILS sans profil désigné"));
        }
        if (i.a(RegleInitiateur.PERMISSIONS) && i.permissions().isEmpty()) {
            a.add(Anomalie.bloquante("INITIATEURS_INCOMPLETS", null, "initiateurs.permissions", "Règle PERMISSIONS sans permission désignée"));
        }
        if (i.a(RegleInitiateur.PERSONNES) && i.personnes().isEmpty()) {
            a.add(Anomalie.bloquante("INITIATEURS_INCOMPLETS", null, "initiateurs.personnes", "Règle PERSONNES sans personne désignée"));
        }
        if (i.a(RegleInitiateur.UNITES)) {
            i.unites().stream().filter(u -> membres(cache, u, null).isEmpty()).forEach(u -> a.add(Anomalie.bloquante("UNITE_INCONNUE", null, "initiateurs.unites", "Unité inconnue : " + u)));
        }
        if (i.a(RegleInitiateur.PROFILS)) {
            i
                .profils()
                .stream()
                .filter(x -> membres(cache, p.getUnite(), x).map(m -> !m.profilConnu()).orElse(false))
                .forEach(x -> a.add(Anomalie.bloquante("PROFIL_INCONNU", null, "initiateurs.profils", "Profil inconnu : " + x)));
        }
        if (i.a(RegleInitiateur.PERSONNES)) {
            for (String id : i.personnes()) {
                var c = equipes.collaborateur(id);
                if (c.isEmpty() || !c.orElseThrow().interne() || !c.orElseThrow().actif()) {
                    String qui = c.map(OrganisationPort.Personne::nomAffiche).filter(Objects::nonNull).orElse(id);
                    a.add(Anomalie.bloquante("PERSONNE_INCONNUE", null, "initiateurs.personnes", "Personne désignée inconnue, externe ou inactive : " + qui));
                }
            }
        }
        Set<String> codes = new java.util.HashSet<>();
        for (ChampFormulaire c : p.getFormulaire().champs()) {
            if (!codes.add(c.code())) {
                a.add(Anomalie.bloquante("CHAMP_CODE_DUPLIQUE", null, "formulaire." + c.code(), "Code de champ du formulaire en double : " + c.code()));
            }
            if (c.type() == TypeChampFormulaire.LISTE && c.choix().isEmpty()) {
                a.add(Anomalie.bloquante("CHAMP_CHOIX_ABSENT", null, "formulaire." + c.code(), "Liste sans valeur : " + c.code()));
            }
        }
    }

    private Optional<UniteMembres> membres(Map<String, Optional<UniteMembres>> cache, String unite, String profil) {
        return cache.computeIfAbsent(unite + "|" + profil, k -> equipes.membresUnite(unite, profil));
    }

    /**
     * Couverture d'un profil exigé (désignation hors de l'unité) : porteurs actifs dans l'unité, sinon ailleurs dans le
     * GIMAC (simple information COUVERTURE_HORS_UNITE, avec leur nombre et leurs unités), sinon nulle part
     * (COUVERTURE_PROFIL, levable par motif).
     */
    private void profil(Map<String, Optional<UniteMembres>> cache, List<Anomalie> a, ControleProcessus.Role r, String unite, String profil, String element) {
        Optional<UniteMembres> u = membres(cache, unite, profil);
        if (u.isEmpty()) {
            return;
        }
        if (!u.orElseThrow().profilConnu()) {
            a.add(Anomalie.bloquante("PROFIL_INCONNU", r.etape(), element, "Profil inconnu : " + profil));
        } else if (u.orElseThrow().actifs().isEmpty()) {
            List<MembreProfil> ailleurs = equipes.porteursActifs(profil);
            String absent = "Aucune personne active ne porte le profil " + profil + " dans l'unité " + unite;
            if (ailleurs.isEmpty()) {
                a.add(Anomalie.levable("COUVERTURE_PROFIL", r.etape(), element, absent + " ni ailleurs dans le GIMAC"));
            } else {
                String suite = ailleurs.size() == 1 ? " porteur ailleurs dans le GIMAC (" + ResolutionIntervenants.repartition(ailleurs) + ") : l'étape lui sera confiée"
                    : " porteurs ailleurs dans le GIMAC (" + ResolutionIntervenants.repartition(ailleurs) + ") : l'étape leur sera offerte";
                a.add(Anomalie.avertissement("COUVERTURE_HORS_UNITE", r.etape(), element, absent + " ; " + ailleurs.size() + suite));
            }
        }
    }

    private void personne(List<Anomalie> a, ControleProcessus.Role r, String id, boolean brouillon) {
        var c = equipes.collaborateur(id);
        if (c.isEmpty() || !c.orElseThrow().interne()) {
            a.add(Anomalie.bloquante("PERSONNE_INCONNUE", r.etape(), r.element(), "Personne inconnue ou externe : " + id));
        } else if (!c.orElseThrow().actif()) {
            a.add(
                brouillon
                    ? Anomalie.bloquante("PERSONNE_INACTIVE", r.etape(), r.element(), "Personne nommée inactive : " + c.orElseThrow().nomAffiche())
                    : Anomalie.avertissement("PERSONNE_PARTIE", r.etape(), r.element(), "Personne partie (" + c.orElseThrow().nomAffiche() + "), remplacement par profil")
            );
        }
    }

    // ------------------------------------------------------------------ lecture

    private ProcessusLu lu(Appelant appelant, Processus p) {
        List<VersionResume> versions = processus
            .findByCodeOrderByVersionDesc(p.getCode())
            .stream()
            .filter(v -> v.getEtat() != EtatProcessus.BROUILLON || acces.definit(appelant, v.getUnite()))
            .map(v -> new VersionResume(v.getId(), v.getVersion(), v.getEtat().name()))
            .toList();
        // Version publiée : informations (personne partie, profil porté seulement hors de l'unité).
        List<Anomalie> avertissements = List.of();
        if (p.getEtat() == EtatProcessus.PUBLIEE) {
            try {
                avertissements = anomalies(p).stream().filter(x -> x.gravite() == GraviteAnomalie.AVERTISSEMENT).toList();
            } catch (PilotageException e) {
                // Annuaire injoignable : la définition reste lisible, sans avertissement.
                avertissements = List.of();
            }
        }
        List<HistoriqueProcessus> h = p.isNew() ? List.of() : historique.findByProcessusIdAndInstanceIdIsNullOrderByQuandAsc(p.getId());
        List<String> ids = new ArrayList<>(Stream.of(p.getCreePar(), p.getModifiePar(), p.getPublieePar(), p.getRetireePar()).filter(Objects::nonNull).toList());
        h.stream().map(HistoriqueProcessus::getActeurId).forEach(ids::add);
        ids.addAll(p.getInitiateurs().personnes());
        p.getEtapes().stream().flatMap(e -> ControleProcessus.roles(List.of(e)).stream()).map(ControleProcessus.Role::intervenant).filter(Objects::nonNull).map(Intervenant::personneId).filter(Objects::nonNull).forEach(ids::add);
        Map<String, String> noms = equipes.noms(ids);
        return new ProcessusLu(p, noms, droits(p, acces.definit(appelant, p.getUnite()), p.getEtat() == EtatProcessus.PUBLIEE && acces.lance(appelant, p)), versions, avertissements, h);
    }

    private static Droits droits(Processus p, boolean definit, boolean lance) {
        boolean brouillon = p.getEtat() == EtatProcessus.BROUILLON;
        boolean publiee = p.getEtat() == EtatProcessus.PUBLIEE;
        return new Droits(definit && brouillon, definit && brouillon, lance && publiee, definit && publiee, definit && brouillon, definit && !brouillon);
    }

    private void decrire(Processus p, ProcessusSaisie s, Appelant appelant, Instant le) {
        String libelle = ReglesActivite.texteObligatoire("libelle", s.libelle(), SaisieProcessus.LIBELLE_MAX);
        String description = ReglesActivite.texteFacultatif("description", s.description(), SaisieProcessus.TEXTE_MAX);
        String categorie = ReglesActivite.texteFacultatif("categorie", s.categorie(), SaisieProcessus.CATEGORIE_MAX);
        Integer delai = SaisieProcessus.delaiCible(s.delaiCibleJours());
        List<Etape> etapes = SaisieProcessus.etapes(s.etapes());
        // D-98 : initiateurs et formulaire de lancement ; absents d'une modification, ceux de la définition sont gardés.
        Initiateurs initiateurs = s.initiateurs() == null ? p.getInitiateurs() : SaisieProcessus.initiateurs(s.initiateurs());
        Formulaire formulaire = s.formulaire() == null ? p.getFormulaire() : SaisieProcessus.formulaire(s.formulaire());
        p.decrire(libelle, description, categorie, delai, etapes, appelant.id(), le);
        p.definirLancement(initiateurs, formulaire, appelant.id(), le);
    }

    private void exigerLecture(Appelant appelant, Processus p) {
        // D-98, D-100 : une version publiée se lit aussi de son public (formulaire de lancement, désignations), et de lui seul
        // hors des lecteurs de l'unité.
        if (p.getEtat() == EtatProcessus.PUBLIEE && acces.lance(appelant, p)) {
            return;
        }
        if (!acces.lit(appelant, p.getUnite()) || (p.getEtat() == EtatProcessus.BROUILLON && !acces.definit(appelant, p.getUnite()))) {
            throw AccesProcessus.interdit("Ce processus n'est pas lisible depuis votre unité");
        }
    }

    private void exigerDefinition(Appelant appelant, Processus p) {
        if (!acces.definit(appelant, p.getUnite())) {
            throw AccesProcessus.interdit("Définir les processus de l'unité exige PROCESSUS_DEFINIR couvrant l'unité ou REF_METIER_ADMIN");
        }
    }

    Processus trouver(UUID id) {
        return processus.findById(id).orElseThrow(() -> PilotageException.introuvable("PIL_PROCESSUS_INTROUVABLE", id));
    }

    /** Droit utilisé quand la règle de l'unité ne suffit pas (D-92 : REF_METIER_ADMIN, portée TOUTES, administration). */
    private String titre(Appelant appelant, Processus p) {
        return acces.mention(appelant, p.getUnite(), AccesProcessus.Acte.DEFINIR);
    }

    private void consigner(Processus p, String acteur, ActionProcessus action, String detail, Instant le) {
        historique.save(new HistoriqueProcessus(p.getId(), null, null, le, acteur, action, detail));
    }
}
