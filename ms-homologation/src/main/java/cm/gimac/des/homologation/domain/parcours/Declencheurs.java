package cm.gimac.des.homologation.domain.parcours;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sujets d'événement d'un autre service qui closent une opération (§1, déclencheur) : ceux de MS-CERTIFICATION
 * consommés par le parcours. Seuls ces sujets sont admis à la publication (anomalie DECLENCHEUR_INCONNU).
 */
public final class Declencheurs {

    public static final String PV_ETABLI = "certification.pv-established.v1";
    public static final String PV_SIGNE = "certification.pv-signed.v1";
    public static final String DOSSIER_CHANGEMENT = "certification.change-record-constituted.v1";
    public static final String ATTESTATIONS_ARCHIVEES = "certification.attestations-archived.v1";
    public static final String SECURITE_APPROUVEE = "certification.security-approved.v1";
    public static final String FICHE_CHANGEMENT = "certification.change-record-referenced.v1";

    /** Sujets connus et leur libellé, dans l'ordre du parcours. */
    public static final Map<String, String> CONNUS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(PV_ETABLI, "Procès-verbal de certification établi");
        m.put(PV_SIGNE, "Procès-verbal signé des deux parties");
        m.put(DOSSIER_CHANGEMENT, "Dossier de changement constitué");
        m.put(ATTESTATIONS_ARCHIVEES, "Attestations vérifiées et archivées");
        m.put(SECURITE_APPROUVEE, "Changement approuvé par la sécurité");
        m.put(FICHE_CHANGEMENT, "Fiche de changement référencée");
        CONNUS = java.util.Collections.unmodifiableMap(m);
    }

    private Declencheurs() {}

    public static boolean connu(String sujet) {
        return sujet != null && CONNUS.containsKey(sujet);
    }
}
