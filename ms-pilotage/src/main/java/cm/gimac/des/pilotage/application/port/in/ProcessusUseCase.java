package cm.gimac.des.pilotage.application.port.in;

import cm.gimac.des.pilotage.domain.processus.Anomalie;
import cm.gimac.des.pilotage.domain.processus.HistoriqueProcessus;
import cm.gimac.des.pilotage.domain.processus.Processus;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Définitions des processus d'unité (D-87, docs/lot1/processus-unite.md §1) : catalogue, brouillon, contrôle,
 * publication, nouvelle version, retrait. Définir et publier : PROCESSUS_DEFINIR couvrant l'unité (D-86 §4) ou
 * IAM_PROFILE_EDIT (administration) ; lire : membres de l'unité, ceux qui la dirigent ou la couvrent,
 * l'administration.
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

    /** Corps de POST /processus (code et unité) et de PUT /processus/{id} (code et unité immuables). */
    record ProcessusSaisie(String code, String unite, String libelle, String description, String categorie, Integer delaiCibleJours, List<EtapeSaisie> etapes) {}

    /** Ce que l'appelant peut faire sur cette version. */
    record Droits(boolean modifier, boolean publier, boolean lancer, boolean retirer) {}

    record VersionResume(UUID id, int version, String etat) {}

    /** Une version lue, avec les noms affichés, les autres versions du code et les avertissements (personne partie…). */
    record ProcessusLu(Processus processus, Map<String, String> noms, Droits droits, List<VersionResume> versions, List<Anomalie> avertissements, List<HistoriqueProcessus> historique) {}

    record Controle(boolean publiable, List<Anomalie> anomalies) {}

    List<ProcessusLu> lister(Appelant appelant, String unite, String etat, String q);

    ProcessusLu lire(Appelant appelant, UUID id);

    ProcessusLu creer(Appelant appelant, ProcessusSaisie saisie);

    ProcessusLu modifier(Appelant appelant, UUID id, ProcessusSaisie saisie);

    void supprimer(Appelant appelant, UUID id);

    Controle controler(Appelant appelant, UUID id);

    ProcessusLu publier(Appelant appelant, UUID id, String motif, boolean leverAvertissements);

    ProcessusLu nouvelleVersion(Appelant appelant, UUID id);

    ProcessusLu retirer(Appelant appelant, UUID id, String motif);
}
