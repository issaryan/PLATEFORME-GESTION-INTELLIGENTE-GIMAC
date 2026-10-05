package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.AgendaUseCase.EvenementLu;
import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.EquipeUseCase;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Membre;
import cm.gimac.des.pilotage.domain.activite.CategorieEvenement;
import cm.gimac.des.pilotage.domain.activite.EtatRapport;
import cm.gimac.des.pilotage.domain.activite.EtatTache;
import cm.gimac.des.pilotage.domain.activite.EvenementAgenda;
import cm.gimac.des.pilotage.domain.activite.ParticipantEvenement;
import cm.gimac.des.pilotage.domain.activite.StatutActivite;
import cm.gimac.des.pilotage.domain.activite.TypeRapport;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import cm.gimac.des.pilotage.repository.ActiviteRepository;
import cm.gimac.des.pilotage.repository.EvenementAgendaRepository;
import cm.gimac.des.pilotage.repository.RapportRepository;
import cm.gimac.des.pilotage.repository.TacheRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vue d'équipe (§2.5, PIL_TEAM_READ) et synthèse du tableau de bord (§2.6). Les comptes sont lus par
 * requêtes groupées sur toute l'équipe (500 membres au plus), jamais un par un.
 */
@Service
@Transactional(readOnly = true)
public class EquipeService implements EquipeUseCase {

    /** Nombre de prochains événements de la synthèse. */
    static final int PROCHAINS = 5;
    /** Échéance proche : aujourd'hui et les trois jours suivants. */
    static final int ECHEANCE_PROCHE_JOURS = 3;

    private final ActiviteRepository activites;
    private final TacheRepository taches;
    private final RapportRepository rapports;
    private final EvenementAgendaRepository evenements;
    private final Equipes equipes;
    private final Clock horloge;

    public EquipeService(
        ActiviteRepository activites,
        TacheRepository taches,
        RapportRepository rapports,
        EvenementAgendaRepository evenements,
        Equipes equipes,
        Clock horloge
    ) {
        this.activites = activites;
        this.taches = taches;
        this.rapports = rapports;
        this.evenements = evenements;
        this.equipes = equipes;
        this.horloge = horloge;
    }

    @Override
    public List<MembreDuJour> equipe(Appelant appelant, String jour) {
        LocalDate j = jour(jour);
        List<Membre> membres = equipes.membres(appelant.id());
        if (membres.isEmpty()) {
            return List.of();
        }
        List<String> ids = membres.stream().map(Membre::id).toList();
        Map<String, Map<StatutActivite, Long>> comptes = new HashMap<>();
        for (Object[] l : activites.comptesDuJour(ids, j)) {
            comptes.computeIfAbsent((String) l[0], k -> new HashMap<>()).put((StatutActivite) l[1], (Long) l[2]);
        }
        Map<String, EtatRapport> rapportsDuJour = new HashMap<>();
        for (Object[] l : rapports.etats(ids, TypeRapport.QUOTIDIEN, j.toString())) {
            rapportsDuJour.put((String) l[0], (EtatRapport) l[1]);
        }
        Map<String, Long> ouvertes = parCompte(taches.comptesOuvertes(ids, EtatTache.OUVERTS));
        Map<String, Long> enRetard = parCompte(taches.comptesEnRetard(ids, EtatTache.OUVERTS, ReglesActivite.aujourdhui(horloge)));
        Set<String> absents = new HashSet<>(
            evenements.proprietairesAvec(ids, CategorieEvenement.ABSENCE, ReglesActivite.debutJour(j), ReglesActivite.debutJour(j.plusDays(1)))
        );
        List<MembreDuJour> r = new ArrayList<>();
        for (Membre m : membres) {
            Map<StatutActivite, Long> c = comptes.getOrDefault(m.id(), Map.of());
            r.add(
                new MembreDuJour(
                    m,
                    prevues(c),
                    c.getOrDefault(StatutActivite.FAITE, 0L),
                    c.getOrDefault(StatutActivite.EN_COURS, 0L),
                    rapportsDuJour.get(m.id()),
                    ouvertes.getOrDefault(m.id(), 0L),
                    enRetard.getOrDefault(m.id(), 0L),
                    absents.contains(m.id())
                )
            );
        }
        return r;
    }

    @Override
    public Synthese synthese(Appelant appelant, String jour) {
        LocalDate aujourdhui = ReglesActivite.aujourdhui(horloge);
        LocalDate j = jour(jour);
        List<String> moi = List.of(appelant.id());
        Map<StatutActivite, Long> c = new HashMap<>();
        for (Object[] l : activites.comptesDuJour(moi, j)) {
            c.put((StatutActivite) l[1], (Long) l[2]);
        }
        Instant apres = j.equals(aujourdhui) ? horloge.instant() : ReglesActivite.debutJour(j);
        List<EvenementAgenda> prochains = evenements.prochains(appelant.id(), apres, PageRequest.of(0, PROCHAINS));
        Map<String, String> noms = equipes.noms(
            prochains.stream().flatMap(e -> Stream.concat(Stream.of(e.getProprietaireId()), e.getParticipants().stream().map(ParticipantEvenement::getParticipantId))).toList()
        );
        List<EvenementLu> evts = prochains.stream().map(e -> new EvenementLu(e, AgendaService.relation(e, appelant.id()), noms)).toList();
        long ouvertes = parCompte(taches.comptesOuvertes(moi, EtatTache.OUVERTS)).getOrDefault(appelant.id(), 0L);
        long retard = parCompte(taches.comptesEnRetard(moi, EtatTache.OUVERTS, aujourdhui)).getOrDefault(appelant.id(), 0L);
        long proche = taches.echeancesEntre(appelant.id(), EtatTache.OUVERTS, aujourdhui, aujourdhui.plusDays(ECHEANCE_PROCHE_JOURS));
        EtatRapport rapport = rapports
            .findByAuteurIdAndTypeAndPeriode(appelant.id(), TypeRapport.QUOTIDIEN, j.toString())
            .map(r -> r.getEtat())
            .orElse(null);
        long aRecevoir = rapports.countByDestinataireIdAndEtat(appelant.id(), EtatRapport.SOUMIS);
        return new Synthese(
            prevues(c),
            c.getOrDefault(StatutActivite.FAITE, 0L),
            c.getOrDefault(StatutActivite.EN_COURS, 0L),
            evts,
            ouvertes,
            retard,
            proche,
            rapport,
            aRecevoir
        );
    }

    private LocalDate jour(String jour) {
        LocalDate j = ReglesActivite.lireJourFacultatif("jour", jour);
        return j == null ? ReglesActivite.aujourdhui(horloge) : j;
    }

    /** Activités prévues : toutes, hors annulées et reportées. */
    private static long prevues(Map<StatutActivite, Long> c) {
        return c
            .entrySet()
            .stream()
            .filter(e -> e.getKey() != StatutActivite.ANNULEE && e.getKey() != StatutActivite.REPORTEE)
            .mapToLong(Map.Entry::getValue)
            .sum();
    }

    private static Map<String, Long> parCompte(List<Object[]> lignes) {
        Map<String, Long> m = new HashMap<>();
        lignes.forEach(l -> m.put((String) l[0], (Long) l[1]));
        return m;
    }
}
