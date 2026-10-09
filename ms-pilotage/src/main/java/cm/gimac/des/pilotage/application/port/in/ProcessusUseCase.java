package cm.gimac.des.pilotage.application.port.in;

import cm.gimac.des.pilotage.domain.processus.Anomalie;
import cm.gimac.des.pilotage.domain.processus.HistoriqueProcessus;
import cm.gimac.des.pilotage.domain.processus.Processus;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Définitions des processus d'unité (D-87, docs/lot1/processus-unite.md §1) : catalogue, brouillon, contrôle,
 * publication, nouvelle version, retrait. Définir et publier : PROCESSUS_DEFINIR couvrant l'unité (D-86 §4) ;
 * lire : membres de l'unité, ceux qui la dirigent ou la couvrent. Toutes les unités (D-92) : REF_METIER_ADMIN, ou
 * portée TOUTES de la matrice ; IAM_PROFILE_EDIT n'en ouvre aucune (D-94).
 */
public interface ProcessusUseCase {
    record IntervenantSaisie(String mode, String profil, String personneId, String unite, String remplacement) {}

    record LivrableSaisie(String code, String libelle, List<String> typesPieces, Boolean obligatoire, String modele) {}

    record SignatureSaisie(String code, String libelle, IntervenantSaisie signataire, List<String> porte, Integer ordre) {}

    record ChampSaisie(String code, String libelle, String type, List<String> choix, Boolean obligatoire) {}

    record PositionSaisie(Double x, Double y) {}

    record EtapeSaisie(
        String code,
        String libelle,
        String action,
        IntervenantSaisie intervenant,
        IntervenantSaisie validateur,
        List<String> predecesseurs,
        Integer delaiJours,
        List<LivrableSaisie> livrables,
        List<SignatureSaisie> signatures,
        List<ChampSaisie> champs,
        PositionSaisie position
    ) {}

    /**
     * Public du processus, qui peut le voir et le lancer (D-98 §1, D-100) : règles cumulables MEMBRES_UNITE, TOUT_LE_GIMAC,
     * UNITES, PROFILS, PERMISSIONS, PERSONNES ({@code personnes} : identifiants de comptes, 50 au plus) ;
     * {@code initiateurIntervenant} (D-99) : l'initiateur peut intervenir sur les étapes de sa demande (absent : non).
     */
    record InitiateursSaisie(
        List<String> regles,
        List<String> unites,
        List<String> profils,
        List<String> permissions,
        List<String> personnes,
        Boolean initiateurIntervenant
    ) {}

    /** Champ du formulaire de lancement (D-98 §2). */
    record ChampFormulaireSaisie(String code, String libelle, String type, List<String> choix, Boolean obligatoire, String aide, List<String> typesPieces) {}

    record FormulaireSaisie(List<ChampFormulaireSaisie> champs) {}

    /**
     * Corps de POST /processus (code et unité) et de PUT /processus/{id} (code et unité immuables) ; initiateurs et
     * formulaire de lancement facultatifs (D-98 : absents, les membres de l'unité et aucun champ).
     */
    record ProcessusSaisie(
        String code,
        String unite,
        String libelle,
        String description,
        String categorie,
        Integer delaiCibleJours,
        List<EtapeSaisie> etapes,
        InitiateursSaisie initiateurs,
        FormulaireSaisie formulaire
    ) {
        public ProcessusSaisie(String code, String unite, String libelle, String description, String categorie, Integer delaiCibleJours, List<EtapeSaisie> etapes) {
            this(code, unite, libelle, description, categorie, delaiCibleJours, etapes, null, null);
        }
    }

    /**
     * Ce que l'appelant peut faire sur cette version : modifier, publier et supprimer un brouillon ; lancer et retirer
     * une version publiée ; en tirer une nouvelle version (publiée ou retirée).
     */
    record Droits(boolean modifier, boolean publier, boolean lancer, boolean retirer, boolean supprimer, boolean nouvelleVersion) {}

    /**
     * Portée du catalogue (D-92 §3) : {@code portee} TOUS_DOMAINES (toutes les unités, actives ou non) ou MES_UNITES ;
     * {@code definirToutesUnites} : création et administration des définitions dans n'importe quelle unité ;
     * {@code lancerToutesUnites} : lecture et pilotage des instances dans n'importe quelle unité (D-100 : le lancement relève
     * du seul public de chaque processus, ce drapeau ne l'ouvre plus) ; {@code auTitreDe} :
     * droits qui ouvrent ces portées (REF_METIER_ADMIN, PROCESSUS_DEFINIR:TOUTES,
     * PROCESSUS_LANCER:TOUTES) ; {@code unites} : unités lisibles (MES_UNITES seulement, vide sinon) ;
     * {@code unitesDefinissables} : unités où créer (si definirToutesUnites est faux).
     */
    record PorteeCatalogue(
        String portee,
        boolean definirToutesUnites,
        boolean lancerToutesUnites,
        List<String> auTitreDe,
        List<String> unites,
        List<String> unitesDefinissables
    ) {}

    record VersionResume(UUID id, int version, String etat) {}

    /** Une version lue, avec les noms affichés, les autres versions du code et les avertissements (personne partie…). */
    record ProcessusLu(Processus processus, Map<String, String> noms, Droits droits, List<VersionResume> versions, List<Anomalie> avertissements, List<HistoriqueProcessus> historique) {}

    record Controle(boolean publiable, List<Anomalie> anomalies) {}

    /** Porteur actif d'un profil proposé pour un intervenant : unité de rattachement (code, null sans rattachement). */
    record Candidat(String id, String nomAffiche, String fonction, String unite) {}

    /**
     * Candidats d'un intervenant (désignation hors de l'unité) : porteurs actifs du profil dans l'unité (sous-unités
     * comprises), puis ailleurs dans le GIMAC (unités actives) ; sans profil : membres actifs de l'unité seulement.
     * {@code exclus} (D-99) : comptes à ne jamais proposer (initiateur de l'instance, séparation des fonctions).
     */
    record Candidats(String unite, String profil, List<Candidat> dansUnite, List<Candidat> ailleurs, List<String> exclus) {}

    List<ProcessusLu> lister(Appelant appelant, String unite, String etat, String q);

    /** Processus d'une unité propriétaire que l'appelant peut lancer (D-98 §3). */
    record GroupeLancables(String unite, List<ProcessusLu> processus) {}

    /**
     * Catalogue « Lancer une demande » (D-98 §3, D-100) : versions publiées dont l'appelant fait partie du public défini
     * par le créateur (règles d'initiateurs seules, sans passe-droit REF_METIER_ADMIN ni PROCESSUS_LANCER), groupées par
     * unité propriétaire ; recherche {@code q} sur le code, le libellé et la catégorie.
     */
    List<GroupeLancables> lancables(Appelant appelant, String q);

    /** Portée du catalogue pour l'appelant (GET /processus/portee). */
    PorteeCatalogue portee(Appelant appelant);

    ProcessusLu lire(Appelant appelant, UUID id);

    ProcessusLu creer(Appelant appelant, ProcessusSaisie saisie);

    ProcessusLu modifier(Appelant appelant, UUID id, ProcessusSaisie saisie);

    void supprimer(Appelant appelant, UUID id);

    Controle controler(Appelant appelant, UUID id);

    ProcessusLu publier(Appelant appelant, UUID id, String motif, boolean leverAvertissements);

    ProcessusLu nouvelleVersion(Appelant appelant, UUID id);

    ProcessusLu retirer(Appelant appelant, UUID id, String motif);

    /**
     * Candidats d'un intervenant pour le modélisateur et le lancement : unité obligatoire (connue, 422
     * PIL_PROCESSUS_UNITE_INCONNUE sinon), profil facultatif (400 s'il est mal formé).
     */
    Candidats candidats(Appelant appelant, String unite, String profil);
}
