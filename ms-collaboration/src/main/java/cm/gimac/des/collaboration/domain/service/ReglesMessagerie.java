package cm.gimac.des.collaboration.domain.service;

import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    /** Mentions par message (D-82). */
    public static final int MENTIONS_MAX = 20;
    /** Longueur de l'extrait d'un message cité (D-82), points de suspension compris. */
    public static final int EXTRAIT_CITATION_MAX = 120;

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

    /**
     * Formats admis pour les documents déposés par envoi fragmenté (D-74, enregistrements d'ateliers) :
     * ceux des pièces jointes, plus vidéo, audio et archives.
     */
    private static final Map<String, List<String>> FORMATS_DOCUMENTS = formatsDocuments();

    private static final String MESSAGE_TYPE = "Format de pièce jointe non admis : PDF, PNG, JPEG, texte, CSV, Word, Excel, PowerPoint ou ZIP";
    private static final String MESSAGE_TYPE_DOCUMENT =
        "Format de document non admis : PDF, PNG, JPEG, texte, CSV, Word, Excel, PowerPoint, vidéo (MP4, MOV, WebM, MKV, AVI), audio (MP3, WAV, M4A) ou archive (ZIP, 7z, TAR, GZ)";

    private static Map<String, List<String>> formatsDocuments() {
        Map<String, List<String>> f = new HashMap<>(FORMATS);
        f.put("mp4", List.of("video/mp4"));
        f.put("mov", List.of("video/quicktime"));
        f.put("webm", List.of("video/webm"));
        f.put("mkv", List.of("video/x-matroska"));
        f.put("avi", List.of("video/x-msvideo"));
        f.put("mp3", List.of("audio/mpeg"));
        f.put("wav", List.of("audio/wav", "audio/x-wav"));
        f.put("m4a", List.of("audio/mp4"));
        f.put("7z", List.of("application/x-7z-compressed"));
        f.put("tar", List.of("application/x-tar"));
        f.put("gz", List.of("application/gzip"));
        return Map.copyOf(f);
    }

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

    /** Nom obligatoire d'un groupe de discussion (D-68), 1 à 200 caractères, sans espaces aux bords. */
    public String titreGroupe(String brut) {
        String t = titre(brut);
        if (t == null) {
            throw CollaborationException.invalide("COL_GROUP_TITLE_REQUIRED", "Donner un nom au groupe de discussion");
        }
        return t;
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
            admises.add(f.avecNomEtType(nom, typeAdmis(FORMATS, MESSAGE_TYPE, nom, f.typeMime(), f.taille())));
        }
        return List.copyOf(admises);
    }

    /** Identifiants distincts (tickets de documents ou pièces à transférer, D-74), sans valeur nulle. */
    public List<UUID> identifiants(List<UUID> bruts) {
        if (bruts == null || bruts.isEmpty()) {
            return List.of();
        }
        Set<UUID> distincts = new LinkedHashSet<>();
        bruts.stream().filter(Objects::nonNull).forEach(distincts::add);
        return List.copyOf(distincts);
    }

    /** Pièces d'un message toutes sources confondues (fichiers, documents, transferts, D-74) : 5 au plus. */
    public void nombrePieces(int nombre) {
        if (nombre > PIECES_MAX) {
            throw CollaborationException.invalide(
                "COL_TOO_MANY_ATTACHMENTS",
                "Au plus " + PIECES_MAX + " pièces jointes par message",
                Map.of("nombre", nombre, "maximum", PIECES_MAX)
            );
        }
    }

    /**
     * Document déposé par envoi fragmenté (D-74) : sans limite de taille propre à la messagerie (celle du
     * dépôt s'applique), formats des pièces jointes étendus à la vidéo, à l'audio et aux archives. Le
     * type des métadonnées doit correspondre à l'extension du nom (application/octet-stream admis) ;
     * rend le nom simple et le type canonique.
     */
    public NomEtType document(String nomFichier, String typeMime, long taille) {
        String nom = nomSimple(nomFichier);
        return new NomEtType(nom, typeAdmis(FORMATS_DOCUMENTS, MESSAGE_TYPE_DOCUMENT, nom, typeMime, taille));
    }

    /** Nom simple et type canonique retenus pour une pièce. */
    public record NomEtType(String nom, String typeMime) {}

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

    /**
     * Mentions d'un message (D-82) : identifiants distincts (une mention répétée est dédoublonnée), dans
     * l'ordre donné, 20 au plus, chacun membre de la conversation et distinct de l'auteur ; sinon 422
     * COL_MENTION_INVALIDE. Le service ne déduit jamais une mention du texte.
     */
    public List<String> mentions(List<String> bruts, String auteurId, Set<String> membres) {
        if (bruts == null || bruts.isEmpty()) {
            return List.of();
        }
        Set<String> distinctes = new LinkedHashSet<>();
        for (String brut : bruts) {
            String id = brut == null ? "" : brut.strip();
            if (id.isEmpty() || id.equals(auteurId) || !membres.contains(id)) {
                throw CollaborationException.nonTraitable(
                    "COL_MENTION_INVALIDE",
                    id.equals(auteurId) ? "L'auteur ne peut pas se mentionner lui-même" : "Seul un membre de la conversation peut être mentionné",
                    Map.of("id", id)
                );
            }
            distinctes.add(id);
        }
        if (distinctes.size() > MENTIONS_MAX) {
            throw CollaborationException.nonTraitable(
                "COL_MENTION_INVALIDE",
                "Au plus " + MENTIONS_MAX + " mentions par message",
                Map.of("nombre", distinctes.size(), "maximum", MENTIONS_MAX)
            );
        }
        return List.copyOf(distinctes);
    }

    /**
     * Message cité par une réponse (D-82) : connu, de la même conversation et pas un événement de groupe ;
     * sinon 422 COL_REPONSE_INVALIDE, sans révéler si le message existe ailleurs.
     *
     * @param conversationCitee conversation du message cité, nulle s'il est inconnu
     */
    public void citation(UUID reponseA, UUID conversationId, UUID conversationCitee, boolean evenementCite) {
        if (conversationCitee == null || !conversationCitee.equals(conversationId) || evenementCite) {
            throw CollaborationException.nonTraitable(
                "COL_REPONSE_INVALIDE",
                evenementCite && conversationId.equals(conversationCitee)
                    ? "Un événement du groupe ne peut pas être cité"
                    : "Le message cité n'appartient pas à cette conversation",
                Map.of("id", String.valueOf(reponseA))
            );
        }
    }

    /**
     * Extrait d'un message cité (D-82) : espaces et sauts de ligne réduits à une espace, 120 caractères
     * au plus. Un texte plus long est coupé proprement : à la dernière espace des 30 derniers caractères
     * s'il y en a une, jamais au milieu d'un caractère hors du plan de base (émoji), suivi de « … ».
     */
    public String extraitCitation(String contenu) {
        if (contenu == null) {
            return "";
        }
        String c = contenu.strip().replaceAll("\\s+", " ");
        if (c.length() <= EXTRAIT_CITATION_MAX) {
            return c;
        }
        int fin = EXTRAIT_CITATION_MAX - 1;
        int espace = c.lastIndexOf(' ', fin);
        if (espace >= fin - 30) {
            fin = espace;
        } else if (Character.isHighSurrogate(c.charAt(fin - 1))) {
            fin--;
        }
        return c.substring(0, fin).stripTrailing() + "…";
    }

    private static String typeAdmis(Map<String, List<String>> formats, String message, String nom, String declare, long taille) {
        int point = nom.lastIndexOf('.');
        String extension = point < 0 ? "" : nom.substring(point + 1).toLowerCase(Locale.ROOT);
        List<String> types = formats.get(extension);
        String type = declare == null ? OCTETS : declare.toLowerCase(Locale.ROOT).split(";")[0].strip();
        if (types == null || taille <= 0 || !(type.isEmpty() || OCTETS.equals(type) || types.contains(type))) {
            throw CollaborationException.invalide("COL_ATTACHMENT_TYPE", message, Map.of("nom", nom, "typeMime", type));
        }
        return types.get(0);
    }

    private static String nomSimple(String brut) {
        String n = brut == null ? "" : brut.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).strip();
        return n.isEmpty() ? "piece-jointe" : n;
    }
}
