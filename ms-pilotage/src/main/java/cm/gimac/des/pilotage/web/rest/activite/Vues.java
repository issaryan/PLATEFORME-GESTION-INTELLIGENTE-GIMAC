package cm.gimac.des.pilotage.web.rest.activite;

import cm.gimac.des.pilotage.application.port.in.ActivitesUseCase.ActiviteLue;
import cm.gimac.des.pilotage.application.port.in.AgendaUseCase.EvenementLu;
import cm.gimac.des.pilotage.application.port.in.AgendaUseCase.Relation;
import cm.gimac.des.pilotage.application.port.in.EquipeUseCase.MembreDuJour;
import cm.gimac.des.pilotage.application.port.in.EquipeUseCase.Synthese;
import cm.gimac.des.pilotage.application.port.in.Page;
import cm.gimac.des.pilotage.application.port.in.RapportsUseCase;
import cm.gimac.des.pilotage.application.port.in.RapportsUseCase.RapportLu;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.GroupeLu;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.TacheLue;
import cm.gimac.des.pilotage.domain.activite.Activite;
import cm.gimac.des.pilotage.domain.activite.EtatRapport;
import cm.gimac.des.pilotage.domain.activite.EvenementAgenda;
import cm.gimac.des.pilotage.domain.activite.Indicateurs;
import cm.gimac.des.pilotage.domain.activite.Rapport;
import cm.gimac.des.pilotage.domain.activite.Tache;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Représentations JSON de Mon activité (docs/lot1/mon-activite.md §2). */
final class Vues {

    private Vues() {}

    record PersonneVue(String id, String nomAffiche) {
        static PersonneVue de(String id, Map<String, String> noms) {
            return id == null ? null : new PersonneVue(id, noms.get(id));
        }
    }

    record PageVue<T>(List<T> contenu, long total, int page, int taille) {
        static <S, T> PageVue<T> de(Page<S> p, Function<S, T> vue) {
            return new PageVue<>(p.contenu().stream().map(vue).toList(), p.total(), p.page(), p.taille());
        }
    }

    // ------------------------------------------------------------------ agenda (§2.1)

    record ParticipantVue(String id, String nomAffiche, String reponse) {}

    record EvenementVue(
        UUID id,
        String titre,
        String description,
        String lieu,
        String debut,
        String fin,
        boolean journeeEntiere,
        String categorie,
        PersonneVue proprietaire,
        List<ParticipantVue> participants,
        String moi
    ) {
        /** Vue d'équipe : catégorie, titre et horaires seulement (§3). */
        static EvenementVue de(EvenementLu l) {
            EvenementAgenda e = l.evenement();
            boolean equipe = l.relation() == Relation.EQUIPE;
            List<ParticipantVue> participants = equipe
                ? List.of()
                : e.getParticipants().stream().map(p -> new ParticipantVue(p.getParticipantId(), l.noms().get(p.getParticipantId()), p.getReponse().name())).toList();
            return new EvenementVue(
                e.getId(),
                e.getTitre(),
                equipe ? null : e.getDescription(),
                equipe ? null : e.getLieu(),
                ReglesActivite.horodatage(e.getDebut()),
                ReglesActivite.horodatage(e.getFin()),
                e.isJourneeEntiere(),
                e.getCategorie().name(),
                PersonneVue.de(e.getProprietaireId(), l.noms()),
                participants,
                l.relation().name()
            );
        }
    }

    record EvenementRequete(
        String titre,
        String description,
        String lieu,
        String debut,
        String fin,
        Boolean journeeEntiere,
        String categorie,
        List<String> participants
    ) {}

    record ReponseRequete(String reponse) {}

    // ------------------------------------------------------------------ activités (§2.2)

    record ActiviteVue(
        UUID id,
        String jour,
        String titre,
        String description,
        String statut,
        Integer dureeMinutes,
        int ordre,
        UUID tacheId,
        String reporteeAu,
        PersonneVue auteur,
        String modifieeLe
    ) {
        static ActiviteVue de(ActiviteLue l) {
            Activite a = l.activite();
            return new ActiviteVue(
                a.getId(),
                Vues.jour(a.getJour()),
                a.getTitre(),
                a.getDescription(),
                a.getStatut().name(),
                a.getDureeMinutes(),
                a.getOrdre(),
                a.getTacheId(),
                Vues.jour(a.getReporteeAu()),
                new PersonneVue(a.getAuteurId(), l.nomAuteur()),
                ReglesActivite.horodatage(a.getModifieLe())
            );
        }
    }

    record ActiviteRequete(String jour, String titre, String description, Integer dureeMinutes, Integer ordre, String tacheId) {}

    record StatutRequete(String statut, String reporteeAu) {}

    // ------------------------------------------------------------------ tâches (§2.3)

    record OrigineVue(String type, String reference, String service, String lien) {}

    record HistoriqueVue(String quand, PersonneVue acteur, String action, String detail) {}

    /**
     * Tâche ; D-81 §6 : {@code groupeId} (envoi à plusieurs), {@code uniteCible} (unité visée), {@code relanceeLe} ;
     * D-87 : {@code profilRequis} (étape de processus) ; D-93 : {@code profilsRequis} (offre d'un autre service, vide
     * sinon), {@code permissionsRequises} (offre par permission, D-94, vide sinon) et {@code aPrendre} (sans
     * responsable, à prendre par sa prise).
     */
    record TacheVue(
        UUID id,
        String titre,
        String description,
        String echeance,
        String priorite,
        String etat,
        PersonneVue responsable,
        PersonneVue createur,
        OrigineVue origine,
        boolean enRetard,
        String creeeLe,
        String termineeLe,
        UUID groupeId,
        String uniteCible,
        String relanceeLe,
        List<HistoriqueVue> historique,
        String profilRequis,
        List<String> profilsRequis,
        List<String> permissionsRequises,
        boolean aPrendre
    ) {
        static TacheVue de(TacheLue l) {
            Tache t = l.tache();
            Map<String, String> noms = l.noms();
            return new TacheVue(
                t.getId(),
                t.getTitre(),
                t.getDescription(),
                Vues.jour(t.getEcheance()),
                t.getPriorite().name(),
                t.getEtat().name(),
                PersonneVue.de(t.getResponsableId(), noms),
                PersonneVue.de(t.getCreateurId(), noms),
                new OrigineVue(t.getOrigineType().name(), t.getOrigineReference(), t.getOrigineService(), t.getOrigineLien()),
                l.enRetard(),
                ReglesActivite.horodatage(t.getCreeLe()),
                ReglesActivite.horodatage(t.getTermineeLe()),
                t.getGroupeId(),
                t.getUniteCible(),
                ReglesActivite.horodatage(t.getRelanceeLe()),
                l
                    .historique()
                    .stream()
                    .map(h -> new HistoriqueVue(ReglesActivite.horodatage(h.getQuand()), PersonneVue.de(h.getActeurId(), noms), h.getAction().name(), h.getDetail()))
                    .toList(),
                t.getProfilRequis(),
                List.copyOf(t.getProfilsRequis()),
                List.copyOf(t.getPermissionsRequises()),
                t.getEtat() == cm.gimac.des.pilotage.domain.activite.EtatTache.A_PRENDRE
            );
        }
    }

    /** Création ou modification ; à la création, {@code responsableId}, {@code responsables} (2 à 50) ou {@code uniteCible}. */
    record TacheRequete(String titre, String description, String echeance, String priorite, String responsableId, List<String> responsables, String uniteCible) {}

    record AttributionRequete(String responsableId) {}

    record AvancementVue(int total, int terminees, int enRetard) {}

    /** Envoi à plusieurs personnes (D-81 §6) : tâches visibles de l'appelant et leur avancement. */
    record GroupeVue(UUID groupeId, String titre, PersonneVue createur, List<TacheVue> taches, AvancementVue avancement) {
        static GroupeVue de(GroupeLu g) {
            return new GroupeVue(
                g.groupeId(),
                g.titre(),
                PersonneVue.de(g.createurId(), g.noms()),
                g.taches().stream().map(TacheVue::de).toList(),
                new AvancementVue(g.avancement().total(), g.avancement().terminees(), g.avancement().enRetard())
            );
        }
    }

    record TransitionRequete(String etat, String motif) {}

    record OrigineRequete(String type, String reference, String service, String lien) {}

    /** Clôture ou réouverture par un autre service (D-77) : origine (type facultatif, référence) et responsable facultatif. */
    /** {@code aPrendre} vrai : l'offre de l'origine seule, remise à prendre à sa réouverture (D-94). */
    record CibleInterneRequete(OrigineRequete origine, String responsableId, Boolean aPrendre) {}

    record TacheInterneRequete(
        String titre,
        String description,
        String echeance,
        String priorite,
        String responsableId,
        String createurId,
        OrigineRequete origine,
        List<String> profilsRequis,
        String uniteCible,
        List<String> permissionsRequises
    ) {}

    // ------------------------------------------------------------------ rapports (§2.4)

    record DocumentVue(UUID ticket, String nom, String typeMime, Long taille) {}

    record VisaVue(PersonneVue par, String commentaire) {}

    record IndicateursVue(int activitesFaites, int activitesPrevues, int minutes, int tachesTerminees) {
        static IndicateursVue de(Indicateurs i) {
            return i == null ? null : new IndicateursVue(i.activitesFaites(), i.activitesPrevues(), i.minutes(), i.tachesTerminees());
        }
    }

    record RapportVue(
        UUID id,
        String type,
        String periode,
        String mode,
        String contenu,
        DocumentVue document,
        String etat,
        PersonneVue auteur,
        PersonneVue destinataire,
        String soumisLe,
        String viseLe,
        VisaVue visa,
        IndicateursVue indicateurs,
        /** N_PLUS_UN, CHEF_UNITE, CHEF_UNITE_PARENTE, CHOISI ; nul pour un brouillon ou un rapport déposé. */
        String destinataireMotif
    ) {
        static RapportVue de(RapportLu l) {
            Rapport r = l.rapport();
            Map<String, String> noms = l.noms();
            return new RapportVue(
                r.getId(),
                r.getType().name(),
                r.getPeriode(),
                r.getMode().name(),
                r.getContenu(),
                r.getDocumentId() == null ? null : new DocumentVue(r.getDocumentId(), r.getDocumentNom(), r.getDocumentTypeMime(), r.getDocumentTaille()),
                r.getEtat().name(),
                PersonneVue.de(r.getAuteurId(), noms),
                PersonneVue.de(r.getDestinataireId(), noms),
                ReglesActivite.horodatage(r.getSoumisLe()),
                ReglesActivite.horodatage(r.getViseLe()),
                r.getEtat() == EtatRapport.VISE ? new VisaVue(PersonneVue.de(r.getVisaParId(), noms), r.getVisaCommentaire()) : null,
                IndicateursVue.de(l.indicateurs()),
                r.getDestinataireMotif() == null ? null : r.getDestinataireMotif().name()
            );
        }
    }

    /** Soumission : destinataire choisi par l'auteur dans sa chaîne hiérarchique (facultatif). */
    record SoumissionRequete(String destinataireId) {}

    record ChoixDestinataireVue(String id, String nomAffiche, String lien, String unite) {}

    /** GET /rapports/{id}/destinataires : destinataire par défaut et motif (nuls avec {@code depot}), choix possibles. */
    record DestinatairesVue(PersonneVue destinataire, String motif, boolean depot, List<ChoixDestinataireVue> choix) {
        static DestinatairesVue de(RapportsUseCase.Destinataires d) {
            return new DestinatairesVue(
                PersonneVue.de(d.destinataireId(), d.noms()),
                d.motif() == null ? null : d.motif().name(),
                d.depot(),
                d.choix().stream().map(c -> new ChoixDestinataireVue(c.id(), d.noms().get(c.id()), c.lien(), c.unite())).toList()
            );
        }
    }

    record DocumentRequete(String ticket) {}

    record RapportRequete(String type, String periode, String mode, String contenu, DocumentRequete document) {}

    record PreremplissageRequete(String type, String periode) {}

    record PreremplissageVue(String contenu) {}

    record VisaRequete(String commentaire) {}

    // ------------------------------------------------------------------ équipe et synthèse (§2.5, §2.6)

    record CollaborateurVue(String id, String nomAffiche, String fonction, String unite, int niveau) {}

    record ActivitesDuJourVue(long prevues, long faites, long enCours) {}

    record MembreVue(
        CollaborateurVue collaborateur,
        ActivitesDuJourVue activites,
        String rapportDuJour,
        long tachesOuvertes,
        long tachesEnRetard,
        boolean absent
    ) {
        static MembreVue de(MembreDuJour m) {
            return new MembreVue(
                new CollaborateurVue(m.membre().id(), m.membre().nomAffiche(), m.membre().fonction(), m.membre().unite(), m.membre().niveau()),
                new ActivitesDuJourVue(m.prevues(), m.faites(), m.enCours()),
                rapport(m.rapportDuJour()),
                m.tachesOuvertes(),
                m.tachesEnRetard(),
                m.absent()
            );
        }
    }

    record TachesSyntheseVue(long ouvertes, long enRetard, long echeanceProche) {}

    record SyntheseVue(ActivitesDuJourVue activites, List<EvenementVue> evenements, TachesSyntheseVue taches, String rapportDuJour, long rapportsARecevoir) {
        static SyntheseVue de(Synthese s) {
            return new SyntheseVue(
                new ActivitesDuJourVue(s.prevues(), s.faites(), s.enCours()),
                s.evenements().stream().map(EvenementVue::de).toList(),
                new TachesSyntheseVue(s.tachesOuvertes(), s.tachesEnRetard(), s.echeanceProche()),
                rapport(s.rapportDuJour()),
                s.rapportsARecevoir()
            );
        }
    }

    /** État du rapport du jour : ABSENT s'il n'y en a pas. */
    static String rapport(EtatRapport e) {
        return e == null ? "ABSENT" : e.name();
    }

    static String jour(LocalDate j) {
        return j == null ? null : j.toString();
    }
}
