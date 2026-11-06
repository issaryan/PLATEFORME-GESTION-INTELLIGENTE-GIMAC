package cm.gimac.des.pilotage.application.usecase;

import java.util.Locale;
import java.util.Map;

/** Libellés du PDF d'une instance (D-139) en fr, en et es : cartouche, sections, états et formats de date. */
final class TextesPdfInstance {

    private TextesPdfInstance() {}

    private static final Map<String, Map<String, String>> TEXTES = Map.of(
        "fr", Map.ofEntries(
            Map.entry("langue", "fr"), Map.entry("oui", "Oui"), Map.entry("non", "Non"),
            Map.entry("reference", "Référence"), Map.entry("etat", "État"), Map.entry("demandeur", "Demandeur"), Map.entry("date", "Date de la demande"),
            Map.entry("donnees", "Données du demandeur"), Map.entry("signature", "Signature"), Map.entry("signataire", "Signataire"),
            Map.entry("signeeLe", "Signée le"), Map.entry("empreinte", "Empreinte"), Map.entry("aucune", "Aucune donnée saisie."),
            Map.entry("EN_COURS", "En cours"), Map.entry("SUSPENDUE", "Suspendue"), Map.entry("CLOSE", "Close"), Map.entry("ANNULEE", "Annulée"),
            Map.entry("EN_ATTENTE", "En attente"), Map.entry("ACTIVE", "En cours"), Map.entry("A_SIGNER", "À signer"), Map.entry("A_VALIDER", "À valider"),
            Map.entry("TERMINEE", "Terminée"), Map.entry("ATTENDUE", "Attendue"), Map.entry("SIGNEE", "Signée"),
            Map.entry("heure", "dd/MM/yyyy HH:mm"), Map.entry("jour", "dd/MM/yyyy")
        ),
        "en", Map.ofEntries(
            Map.entry("langue", "en"), Map.entry("oui", "Yes"), Map.entry("non", "No"),
            Map.entry("reference", "Reference"), Map.entry("etat", "Status"), Map.entry("demandeur", "Requester"), Map.entry("date", "Request date"),
            Map.entry("donnees", "Requester data"), Map.entry("signature", "Signature"), Map.entry("signataire", "Signatory"),
            Map.entry("signeeLe", "Signed on"), Map.entry("empreinte", "Fingerprint"), Map.entry("aucune", "No data entered."),
            Map.entry("EN_COURS", "In progress"), Map.entry("SUSPENDUE", "Suspended"), Map.entry("CLOSE", "Closed"), Map.entry("ANNULEE", "Cancelled"),
            Map.entry("EN_ATTENTE", "Pending"), Map.entry("ACTIVE", "In progress"), Map.entry("A_SIGNER", "To sign"), Map.entry("A_VALIDER", "To validate"),
            Map.entry("TERMINEE", "Completed"), Map.entry("ATTENDUE", "Awaited"), Map.entry("SIGNEE", "Signed"),
            Map.entry("heure", "yyyy-MM-dd HH:mm"), Map.entry("jour", "yyyy-MM-dd")
        ),
        "es", Map.ofEntries(
            Map.entry("langue", "es"), Map.entry("oui", "Sí"), Map.entry("non", "No"),
            Map.entry("reference", "Referencia"), Map.entry("etat", "Estado"), Map.entry("demandeur", "Solicitante"), Map.entry("date", "Fecha de la solicitud"),
            Map.entry("donnees", "Datos del solicitante"), Map.entry("signature", "Firma"), Map.entry("signataire", "Firmante"),
            Map.entry("signeeLe", "Firmada el"), Map.entry("empreinte", "Huella"), Map.entry("aucune", "Ningún dato introducido."),
            Map.entry("EN_COURS", "En curso"), Map.entry("SUSPENDUE", "Suspendida"), Map.entry("CLOSE", "Cerrada"), Map.entry("ANNULEE", "Anulada"),
            Map.entry("EN_ATTENTE", "En espera"), Map.entry("ACTIVE", "En curso"), Map.entry("A_SIGNER", "Por firmar"), Map.entry("A_VALIDER", "Por validar"),
            Map.entry("TERMINEE", "Terminada"), Map.entry("ATTENDUE", "Pendiente"), Map.entry("SIGNEE", "Firmada"),
            Map.entry("heure", "dd/MM/yyyy HH:mm"), Map.entry("jour", "dd/MM/yyyy")
        )
    );

    /** Langue retenue : fr, en ou es, fr par défaut. */
    static String langue(String l) {
        String x = l == null ? "" : l.strip().toLowerCase(Locale.ROOT);
        return TEXTES.containsKey(x) ? x : "fr";
    }

    static Map<String, String> de(String langue) {
        return TEXTES.get(langue(langue));
    }
}
