package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.JsonTexte;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Compteur du tableau de bord d'acteur décrit en données (D-94 §3, {@code config/tableau-acteur-compteurs.json}) :
 * aucune règle de compteur n'est codée par fonction.
 * <ul>
 * <li>{@code fonctions} : fonctions du catalogue qui l'affichent (vide : tout acteur) ;</li>
 * <li>{@code portee} : LIAISON (liaisons qui satisfont {@code liaison} et, s'il est donné, ont une opération qui
 * satisfait {@code operation}), OPERATION (opérations qui satisfont {@code operation} sur une liaison qui satisfait
 * {@code liaison}) ou TACHE (« mes tâches » de l'acteur, filtrées par {@code sla}) ;</li>
 * <li>{@code mesure} : NOMBRE (par défaut), ATTESTATIONS_DEPOSEES ou ATTESTATIONS_A_FOURNIR (somme sur les
 * liaisons) ;</li>
 * <li>{@code lien} : lien du compteur ; {@code lienUnique} : lien quand il ne compte qu'un élément (OPERATION :
 * l'opération, DOSSIER:&lt;onglet&gt; : l'onglet du dossier, TACHE : le lien de la tâche) ;</li>
 * <li>{@code ton} : info, warn ou crit (ok quand la valeur est nulle).</li>
 * </ul>
 */
public record DefinitionCompteur(
    String cle,
    String libelle,
    List<String> fonctions,
    String portee,
    String mesure,
    FiltreLiaison liaison,
    FiltreOperation operation,
    Sla sla,
    String ton,
    String lien,
    String lienUnique
) {
    public DefinitionCompteur {
        fonctions = fonctions == null ? List.of() : List.copyOf(fonctions);
    }

    /** Critères sur la liaison ; un critère absent ne filtre pas. */
    public record FiltreLiaison(
        List<EtapeCode> etapesCourantes,
        Boolean conducteurADesigner,
        Boolean mesProjets,
        List<EtatProcesVerbal> etatsPv,
        List<EtatApprobation> etatsApprobation,
        List<EtatFicheSuivi> etatsFiche,
        Boolean fintech,
        String visaAttendu
    ) {}

    /**
     * Critères sur l'opération ; un critère absent ne filtre pas. {@code engagee} : ni faite ni verrouillée ;
     * {@code responsables} : fonction responsable de référence ; {@code fonctionsR} : fonction qui tient R ;
     * {@code premiereDeLEtape} : première opération de son étape canonique.
     */
    public record FiltreOperation(
        List<EtatOperationParcours> etats,
        Boolean engagee,
        List<EtapeCode> etapes,
        List<String> etapesVersion,
        List<Geste> gestes,
        List<Jalon> jalons,
        List<String> declencheurs,
        List<Particularite> particularites,
        List<SegmentParcours> segments,
        List<Fonction> responsables,
        List<Fonction> fonctionsR,
        Boolean cle,
        Boolean oubliSansSymptome,
        Boolean premiereDeLEtape,
        List<VueOperation> vues
    ) {}

    /** Ressource des compteurs livrés avec le service. */
    public static final String RESSOURCE = "/config/tableau-acteur-compteurs.json";

    /** Compteurs d'un document JSON (liste de définitions). */
    public static List<DefinitionCompteur> lire(String json) {
        return JsonTexte.liste(json, DefinitionCompteur.class);
    }

    /** Compteurs livrés avec le service. */
    public static List<DefinitionCompteur> parDefaut() {
        return charger(RESSOURCE);
    }

    /** Compteurs d'une ressource du chemin de classes ; ressource absente : IllegalStateException. */
    static List<DefinitionCompteur> charger(String ressource) {
        try (InputStream in = DefinitionCompteur.class.getResourceAsStream(ressource)) {
            if (in == null) {
                throw new IllegalStateException("Ressource absente : " + ressource);
            }
            return lire(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
