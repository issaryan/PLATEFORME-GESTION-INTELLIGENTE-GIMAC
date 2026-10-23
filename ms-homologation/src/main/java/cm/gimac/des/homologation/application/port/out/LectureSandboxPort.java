package cm.gimac.des.homologation.application.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contrat de lecture v1 de SBX-LECTURE (livrable L04, CC-SBX-GIMAC-02 annexe D, US-EXI-14) : consultation seule,
 * aucune méthode d'écriture (test de revue). Remplace l'adaptateur de lecture du simulateur existant (HOM-13).
 * Chemins (GET et HEAD) : /v1/liaisons/{id}/campagnes, /v1/campagnes/{c}, /v1/campagnes/{c}/executions?depuis=&taille=,
 * /v1/campagnes/{c}/pieces, /v1/campagnes/{c}/pieces/{n}/{v}. Les verdicts et les taux sont repris tels que restitués :
 * aucune normalisation, aucun recalcul (arbitrage D7).
 */
public interface LectureSandboxPort {
    /** Campagnes d'une liaison ; une campagne en collision de code participant est remplacée par un signalement. */
    List<CampagneListee> campagnes(UUID liaisonId);

    /** Détail d'une campagne : liaison, participant, interface, services, état, taux, version du bilan. */
    Campagne campagne(String campagneId);

    /**
     * Enregistrements d'exécution de position strictement supérieure au jeton {@code depuis} (null : première lecture,
     * depuis le début), au plus {@code taille}.
     */
    PageExecutions executions(String campagneId, String depuis, int taille);

    /** Pièces de la campagne, avec leur empreinte SHA-256 et leur taille annoncées avant tout téléchargement. */
    List<Piece> pieces(String campagneId);

    /** Empreinte annoncée par HEAD /v1/campagnes/{c}/pieces/{n}/{v} (en-tête Repr-Digest, sha-256). */
    String empreinte(String campagneId, String nature, int version);

    /** Contenu de la version {@code version} de la pièce de nature {@code nature}. */
    byte[] telecharger(String campagneId, String nature, int version);

    // ------------------------------------------------------------------ éléments du contrat

    /** Campagne listée : {@code signalement} non nul remplace la campagne d'un code participant en collision. */
    record CampagneListee(String identifiantCampagne, String code, String etat, String signalement) {}

    record Campagne(
        String identifiantCampagne,
        String code,
        String identifiantLiaison,
        String codeParticipant,
        String interfaceTest,
        List<String> services,
        String etat,
        List<Taux> taux,
        Integer versionBilan,
        String versionTable
    ) {
        public Campagne {
            services = services == null ? List.of() : List.copyOf(services);
            taux = taux == null ? List.of() : List.copyOf(taux);
        }
    }

    /**
     * Taux restitué sans recalcul (INT-05) : {@code service} nul pour le taux de la campagne ; {@code nature} AVANCEMENT ou
     * REUSSITE ; valeur décimale tronquée, telle que restituée.
     */
    record Taux(String service, String nature, int numerateur, int denominateur, String valeur, String versionFormule, Instant horodatage) {}

    /** Enregistrement d'exécution : les treize éléments d'INT-04, complétés de la position, la nature, la table et l'origine. */
    record Enregistrement(
        String identifiantCampagne,
        String identifiantLiaison,
        String codeParticipant,
        String identifiantCas,
        String identifiantScenario,
        String versionScenario,
        String identifiantExecution,
        Instant dateExecution,
        String verdictNatif,
        String verdictNormalise,
        String motif,
        String modeExecution,
        String referenceTrace,
        String position,
        String nature,
        String versionTable,
        String origine
    ) {}

    /** Page : au plus la taille demandée, jeton de la position suivante, page close (pleine) ou non, étiquette d'entité. */
    record PageExecutions(List<Enregistrement> enregistrements, String positionSuivante, boolean close, String etiquette) {
        public PageExecutions {
            enregistrements = enregistrements == null ? List.of() : List.copyOf(enregistrements);
        }
    }

    /** Pièce : bilan figé, résultats détaillés, fichiers remis au participant (INT-08). */
    record Piece(String nature, int version, String empreinte, long taille, String nom) {}

    /** 503 ou 429 avec Retry-After, ou service injoignable : aucune donnée, la relève reprend après le délai. */
    final class Indisponible extends RuntimeException {

        private final transient Duration delai;

        public Indisponible(String message, Duration delai) {
            super(message);
            this.delai = delai;
        }

        public Duration delai() {
            return delai;
        }
    }

    /** Réponse hors contrat (jeton refusé, forme invalide, 404 d'une campagne hors périmètre) : la page entière est écartée. */
    final class HorsContrat extends RuntimeException {

        public HorsContrat(String message) {
            super(message);
        }
    }
}
