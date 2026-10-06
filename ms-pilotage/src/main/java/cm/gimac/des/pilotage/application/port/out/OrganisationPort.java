package cm.gimac.des.pilotage.application.port.out;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Organisation portée par MS-IAM (docs/lot1/mon-activite.md §1.3), lue avec le jeton du client
 * pilotage-service (INTERNAL_IAM_READ) : rattachement d'un compte, son équipe, un collaborateur, et
 * ses pouvoirs sur ses subordonnés (docs/lot1/pouvoirs-hierarchiques.md §1 à §3, D-81). Les erreurs de
 * joignabilité lèvent 503 PIL_ORGANISATION_INDISPONIBLE, sans valeur supposée.
 */
public interface OrganisationPort {
    /** Compte de l'annuaire : un compte interne n'a pas d'institution. */
    record Personne(String id, String nomAffiche, String fonction, boolean actif, boolean interne) {}

    /**
     * GET /api/v1/interne/organisation/{id} : unité la plus fine (code), chemin des unités de la plus haute à
     * la plus fine (codes), responsable (n+1), chaîne des responsables.
     */
    record Rattachement(String id, String nomAffiche, String unite, Personne responsable, List<String> chaine, List<String> chemin) {
        public Rattachement {
            chaine = chaine == null ? List.of() : List.copyOf(chaine);
            chemin = chemin == null ? List.of() : List.copyOf(chemin);
        }

        /** Le compte est rattaché à {@code code} ou à l'une de ses sous-unités. */
        public boolean dans(String code) {
            return code != null && chemin.contains(code);
        }
    }

    /** Élément de GET /api/v1/interne/organisation/{id}/equipe (récursif, 500 au plus) et d'un périmètre. */
    record Membre(String id, String nomAffiche, String fonction, String unite, String responsableId, int niveau, boolean actif) {}

    /** Portée d'un pouvoir (D-81 §1), de la plus étroite à la plus large ; UNITES = équipe et membres des unités dirigées. */
    enum Portee {
        AUCUNE,
        DIRECTS,
        EQUIPE,
        UNITES;

        /** Portée lue dans une réponse de MS-IAM ; une valeur inconnue ne donne aucun pouvoir. */
        public static Portee lire(String texte) {
            if (texte == null) {
                return AUCUNE;
            }
            try {
                return valueOf(texte.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return AUCUNE;
            }
        }
    }

    /** Actions de la matrice des pouvoirs (D-81 §3). */
    enum Pouvoir {
        TACHE_CONFIER,
        TACHE_SUIVRE,
        ACTIVITE_LIRE,
        RAPPORT_VISER,
        RATTACHEMENT_MODIFIER,
        COMPTE_DEMANDER,
        PROFIL_DEMANDER,
        STATUT_DEMANDER,
        /** Créer et publier les processus d'une unité couverte (D-86 §4, D-87) ; absente de la réponse de MS-IAM : AUCUNE. */
        PROCESSUS_DEFINIR,
        /** Lancer un processus d'une unité couverte (D-86 §4, D-87) ; absente de la réponse de MS-IAM : AUCUNE. */
        PROCESSUS_LANCER,
    }

    /** GET /api/v1/interne/pouvoirs/{id} : portée de chaque action, profils demandables, subordonnés et unités dirigées. */
    record Pouvoirs(Map<Pouvoir, Portee> actions, List<String> profilsDemandables, List<Membre> subordonnes, List<String> unitesDirigees) {
        public Pouvoirs {
            EnumMap<Pouvoir, Portee> m = new EnumMap<>(Pouvoir.class);
            if (actions != null) {
                actions.forEach((k, v) -> {
                    if (k != null && v != null) {
                        m.put(k, v);
                    }
                });
            }
            actions = Map.copyOf(m);
            profilsDemandables = profilsDemandables == null ? List.of() : List.copyOf(profilsDemandables);
            subordonnes = subordonnes == null ? List.of() : List.copyOf(subordonnes);
            unitesDirigees = unitesDirigees == null ? List.of() : List.copyOf(unitesDirigees);
        }

        public static Pouvoirs aucun() {
            return new Pouvoirs(Map.of(), List.of(), List.of(), List.of());
        }

        public Portee portee(Pouvoir p) {
            return actions.getOrDefault(p, Portee.AUCUNE);
        }

        /** L'unité (ou l'une de ses unités parentes) est dirigée par ce compte : {@code unitesDirigees} comprend les sous-unités. */
        public boolean dirige(String unite) {
            return unite != null && unitesDirigees.contains(unite);
        }
    }

    /** GET /api/v1/interne/organisation/{id}/perimetre?portee= : comptes et unités couverts par une portée. */
    record Perimetre(Portee portee, List<Membre> comptes, List<String> unites) {
        public Perimetre {
            portee = portee == null ? Portee.AUCUNE : portee;
            comptes = comptes == null ? List.of() : List.copyOf(comptes);
            unites = unites == null ? List.of() : List.copyOf(unites);
        }

        public static Perimetre vide() {
            return new Perimetre(Portee.AUCUNE, List.of(), List.of());
        }

        public Optional<Membre> membre(String id) {
            return id == null ? Optional.empty() : comptes.stream().filter(m -> id.equals(m.id())).findFirst();
        }
    }

    /** Membre d'une unité (sous-unités comprises) avec ses profils (rôles composites ROLE_…). */
    record MembreProfil(String id, String nomAffiche, String fonction, String unite, boolean actif, Set<String> profils) {
        public MembreProfil {
            profils = profils == null ? Set.of() : Set.copyOf(profils);
        }
    }

    /**
     * Unité et membres (processus d'unité, D-87) : GET /api/v1/interne/organisation/unites/{code} (libellé, état,
     * responsable, même sans membre) puis GET /api/v1/interne/organisation/unites/{code}/membres?sousUnites=true&profil=
     * (membres actifs, sous-unités comprises, filtrés sur le profil s'il est donné) ; {@code responsableId} : chef de l'unité ; {@code profilConnu} faux si le profil demandé n'existe pas (seul
     * l'adaptateur de substitution le sait ; MS-IAM rend alors une liste vide).
     */
    record UniteMembres(String code, String libelle, String responsableId, boolean active, boolean profilConnu, List<MembreProfil> membres) {
        public UniteMembres {
            membres = membres == null ? List.of() : List.copyOf(membres);
        }

        public List<MembreProfil> actifs() {
            return membres.stream().filter(MembreProfil::actif).toList();
        }
    }

    Optional<Rattachement> rattachement(String compteId);

    /**
     * Unité et membres portant {@code profil} (null : tous les membres), sous-unités comprises ; unité inconnue :
     * vide. Sert à résoudre les intervenants des processus d'unité (D-87).
     */
    Optional<UniteMembres> membresUnite(String unite, String profil);

    /** Profils (ROLE_…) du compte ; compte inconnu : aucun. */
    Set<String> profilsDe(String compteId);

    /** Comptes dont {@code compteId} figure dans la chaîne des responsables ; vide s'il n'en a aucun. */
    List<Membre> equipe(String compteId);

    Optional<Personne> collaborateur(String compteId);

    /** Pouvoirs du compte (matrice de ses profils, portée la plus large par action) ; compte inconnu : aucun pouvoir. */
    Pouvoirs pouvoirs(String compteId);

    /** Comptes et unités couverts par {@code portee} pour ce compte ; AUCUNE : périmètre vide. */
    Perimetre perimetre(String compteId, Portee portee);
}
