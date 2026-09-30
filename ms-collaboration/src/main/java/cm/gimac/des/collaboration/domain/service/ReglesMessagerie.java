package cm.gimac.des.collaboration.domain.service;

import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Règles de la messagerie de travail (D-64, docs/lot1/messagerie.md §1) : contenu, destinataires,
 * étiquettes de liaison, pièces jointes et reconnaissance d'une conversation directe.
 * Service de domaine pur, sans Spring ni accès aux données.
 */
public final class ReglesMessagerie {

    public static final int CONTENU_MAX = 4000;
    public static final int DESTINATAIRES_MAX = 50;
    public static final int ETIQUETTES_MAX = 10;
    public static final int PIECES_MAX = 5;
    public static final long PIECE_TAILLE_MAX = 20L * 1024 * 1024;
    public static final int TITRE_MAX = 200;

    private static final Pattern REFERENCE_LIAISON = Pattern.compile("^LIA-\\d{4}-\\d{3,6}$");
    private static final String OCTETS = "application/octet-stream";

    /** Formats admis, par extension : type MIME canonique et types acceptés tels que déclarés par les navigateurs. */
    private static final Map<String, List<String>> FORMATS = Map.ofEntries(
        Map.entry("pdf", List.of("application/pdf")),
        Map.entry("png", List.of("image/png")),
        Map.entry("jpg", List.of("image/jpeg", "image/pjpeg")),
        Map.entry("jpeg", List.of("image/jpeg", "image/pjpeg")),
        Map.entry("txt", List.of("text/plain")),
        Map.entry("csv", List.of("text/csv", "text/plain", "application/csv", "application/vnd.ms-excel")),
        Map.entry("doc", List.of("application/msword")),
        Map.entry("docx", List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
        Map.entry("xls", List.of("application/vnd.ms-excel")),
        Map.entry("xlsx", List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
        Map.entry("ppt", List.of("application/vnd.ms-powerpoint")),
        Map.entry("pptx", List.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
        Map.entry("zip", List.of("application/zip", "application/x-zip-compressed"))
    );

    /** Contenu sans les espaces aux bords ; vide admis seulement avec au moins une pièce jointe. */
    public String contenu(String brut, int nombrePieces) {
        String c = brut == null ? "" : brut.strip();
        if (c.isEmpty() && nombrePieces == 0) {
            throw CollaborationException.invalide("COL_MESSAGE_EMPTY", "Le message doit comporter un texte ou une pièce jointe");
        }
        if (c.length() > CONTENU_MAX) {
            throw CollaborationException.invalide(
                "COL_MESSAGE_TOO_LONG",
                "Le message dépasse " + CONTENU_MAX + " caractères",
                Map.of("longueur", c.length(), "maximum", CONTENU_MAX)
            );
        }
        return c;
    }

    /** Titre facultatif d'une conversation de groupe, sans espaces aux bords. */
    public String titre(String brut) {
        if (brut == null || brut.isBlank()) {
            return null;
        }
        String t = brut.strip();
        return t.length() > TITRE_MAX ? t.substring(0, TITRE_MAX) : t;
    }

    /** Destinataires distincts, 1 à 50, l'expéditeur exclu. */
    public List<String> destinataires(List<String> bruts, String expediteurId) {
        Set<String> distincts = new LinkedHashSet<>();
        if (bruts != null) {
            bruts.stream().filter(d -> d != null && !d.isBlank()).map(String::strip).forEach(distincts::add);
        }
        if (distincts.isEmpty()) {
            throw CollaborationException.invalide("COL_RECIPIENT_REQUIRED", "Choisir au moins un destinataire dans l'annuaire");
        }
        if (distincts.contains(expediteurId)) {
            throw CollaborationException.invalide("COL_RECIPIENT_SELF", "L'expéditeur ne peut pas être son propre destinataire");
        }
        if (distincts.size() > DESTINATAIRES_MAX) {
            throw CollaborationException.invalide(
                "COL_TOO_MANY_RECIPIENTS",
                "Au plus " + DESTINATAIRES_MAX + " destinataires",
                Map.of("nombre", distincts.size(), "maximum", DESTINATAIRES_MAX)
            );
        }
        return List.copyOf(distincts);
    }

    /** Étiquettes de liaison : 10 au plus, référence LIA-AAAA-NNN, sans doublon. */
    public List<Etiquette> etiquettes(List<Etiquette> brutes) {
        if (brutes == null || brutes.isEmpty()) {
            return List.of();
        }
        if (brutes.size() > ETIQUETTES_MAX) {
            throw CollaborationException.invalide(
                "COL_TOO_MANY_TAGS",
                "Au plus " + ETIQUETTES_MAX + " étiquettes par message",
                Map.of("nombre", brutes.size(), "maximum", ETIQUETTES_MAX)
            );
        }
        Set<Etiquette> distinctes = new LinkedHashSet<>();
        for (Etiquette e : brutes) {
            String reference = e == null || e.reference() == null ? "" : e.reference().strip();
            if (e == null || e.liaisonId() == null || !REFERENCE_LIAISON.matcher(reference).matches()) {
                throw CollaborationException.invalide(
                    "COL_TAG_INVALID",
                    "Étiquette de liaison invalide : référence attendue au format LIA-AAAA-NNN",
                    Map.of("reference", reference)
                );
            }
            distinctes.add(new Etiquette(e.liaisonId(), reference));
        }
        return List.copyOf(distinctes);
    }

    /**
     * Pièces jointes : 5 au plus, 20 Mo chacune, formats admis (PDF, PNG, JPEG, texte, CSV, Word,
     * Excel, PowerPoint, ZIP). Le nom est réduit à son dernier segment et le type rendu canonique.
     */
    public List<FichierJoint> pieces(List<FichierJoint> brutes) {
        if (brutes == null || brutes.isEmpty()) {
            return List.of();
        }
        if (brutes.size() > PIECES_MAX) {
            throw CollaborationException.invalide(
                "COL_TOO_MANY_ATTACHMENTS",
                "Au plus " + PIECES_MAX + " pièces jointes par message",
                Map.of("nombre", brutes.size(), "maximum", PIECES_MAX)
            );
        }
        List<FichierJoint> admises = new ArrayList<>();
        for (FichierJoint f : brutes) {
            String nom = nomSimple(f.nom());
            if (f.taille() > PIECE_TAILLE_MAX) {
                throw CollaborationException.invalide(
                    "COL_ATTACHMENT_TOO_LARGE",
                    "La pièce « " + nom + " » dépasse 20 Mo",
                    Map.of("nom", nom, "taille", f.taille(), "maximum", PIECE_TAILLE_MAX)
                );
            }
            admises.add(f.avecNomEtType(nom, typeAdmis(nom, f.typeMime(), f.taille())));
        }
        return List.copyOf(admises);
    }

    /** Deux personnes, sans titre ni liaison : conversation directe. */
    public boolean estDirecte(List<String> destinataires, String titre, UUID liaisonId) {
        return destinataires.size() == 1 && titre == null && liaisonId == null;
    }

    /** Clé d'unicité d'une conversation directe : les deux identifiants triés. */
    public String cleDirecte(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    /** Extrait d'un message pour la liste des conversations : 120 premiers caractères. */
    public String extrait(String contenu) {
        if (contenu == null) {
            return "";
        }
        return contenu.length() <= 120 ? contenu : contenu.substring(0, 120);
    }

    private static String typeAdmis(String nom, String declare, long taille) {
        int point = nom.lastIndexOf('.');
        String extension = point < 0 ? "" : nom.substring(point + 1).toLowerCase(Locale.ROOT);
        List<String> types = FORMATS.get(extension);
        String type = declare == null ? OCTETS : declare.toLowerCase(Locale.ROOT).split(";")[0].strip();
        if (types == null || taille <= 0 || !(type.isEmpty() || OCTETS.equals(type) || types.contains(type))) {
            throw CollaborationException.invalide(
                "COL_ATTACHMENT_TYPE",
                "Format de pièce jointe non admis : PDF, PNG, JPEG, texte, CSV, Word, Excel, PowerPoint ou ZIP",
                Map.of("nom", nom, "typeMime", type)
            );
        }
        return types.get(0);
    }

    private static String nomSimple(String brut) {
        String n = brut == null ? "" : brut.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).strip();
        return n.isEmpty() ? "piece-jointe" : n;
    }
}
