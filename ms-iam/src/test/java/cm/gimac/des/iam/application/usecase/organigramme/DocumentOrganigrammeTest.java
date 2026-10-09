package cm.gimac.des.iam.application.usecase.organigramme;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.application.port.out.RenduPdf;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Document HTML de l'organigramme (D-102 §4) : autonome (polices et logos embarqués, aucune adresse externe, aucun
 * script), textes échappés, cartes, légende, annexe des membres, pied de page « page x sur y ».
 */
class DocumentOrganigrammeTest {

    static final ZonedDateTime EDITION = ZonedDateTime.of(2026, 10, 9, 14, 32, 0, 0, ZoneId.of("Africa/Douala"));

    static List<Unite> unites() {
        return List.of(
            new Unite("DG", "Direction Générale", TypeUnite.DIRECTION, null, true, "u-dg"),
            new Unite("DES", "Département Exploitation et Supervision", TypeUnite.DEPARTEMENT, "DG", true, "u-chef-des"),
            new Unite("SNA", "Service Normes et Agréments", TypeUnite.SERVICE, "DES", true, "u-chef-sna"),
            new Unite("PROD_MONETIQUE", "Service Production monétique", TypeUnite.SERVICE, "DES", true, null),
            new Unite("PROD_INFO", "Service Production informatique", TypeUnite.SERVICE, "DES", true, "u-resp-info"),
            new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", true, "u-res"),
            new Unite("POLE_SYSTEME", "Pôle système", TypeUnite.POLE, "PROD_INFO", true, null),
            new Unite("DDRM", "Département Développement du Réseau et du Marché", TypeUnite.DEPARTEMENT, "DG", true, "u-chef-ddrm"),
            new Unite("SSI", "Sécurité des systèmes d'information", TypeUnite.SERVICE, "DG", true, "u-rssi"),
            new Unite("CONFORMITE", "Conformité", TypeUnite.SERVICE, "DG", true, "u-conf"),
            new Unite("RH", "Service des ressources humaines", TypeUnite.SERVICE, "DG", true, "u-rh-chef"),
            new Unite("CELLULE_QUALITE", "Cellule qualité", TypeUnite.CELLULE, "SNA", true, null)
        );
    }

    static Map<String, DocumentOrganigramme.Personne> personnes() {
        Map<String, DocumentOrganigramme.Personne> m = new LinkedHashMap<>();
        Object[][] l = {
            { "u-dg", "Jean-Baptiste Ella Mintsa", "Directeur général", "DG", null, "Direction générale" },
            { "u-chef-des", "Christian Ndong Obiang", "Chef du DES", "DES", "u-dg", "Chef du DES" },
            { "u-chef-sna", "Rachel Biyong Mengue", "Cheffe du service Normes et Agréments", "SNA", "u-chef-des", "Normes et Agréments" },
            { "u-ing", "Patrick Mbida Essomba", "Ingénieur d'homologation", "SNA", "u-chef-sna", "Ingénieur d'homologation" },
            { "u-cond", "Aïcha Mahamat Nour", "Conductrice de projet", "SNA", "u-chef-sna", "Conducteur de projet" },
            { "u-prod", "Laetitia Nguema Ondo", "Opératrice production monétique", "PROD_MONETIQUE", "u-chef-des", "Production monétique" },
            { "u-resp-info", "Gaston Ndzana Ebode", "Chef du service Production informatique", "PROD_INFO", "u-chef-des", "Responsable de service" },
            { "u-res", "Idriss Mahamat Saleh", "Ingénieur réseau", "POLE_RESEAU", "u-resp-info", "Pôle réseau" },
            { "u-tech", "Nadège Mvondo Assiga", "Technicienne réseau", "POLE_RESEAU", "u-res", "Pôle réseau" },
            { "u-sys", "Bachir Hamadou Ali", "Ingénieur système", "POLE_SYSTEME", "u-resp-info", "Pôle système" },
            { "u-chef-ddrm", "Aïssatou Ngo Mbarga", "Cheffe de la DDRM", "DDRM", "u-dg", "Chef de la DDRM" },
            { "u-charge", "Joël Essomba Atangana", "Chargé d'études DDRM", "DDRM", "u-chef-ddrm", "Chargé DDRM" },
            { "u-rssi", "Hervé Kamga Tchoupo", "Responsable de la sécurité des SI", "SSI", "u-dg", "RSSI" },
            { "u-conf", "Mariam Oumarou Bello", "Responsable conformité", "CONFORMITE", "u-dg", "Conformité" },
            { "u-rh-chef", "Solange Ateba Mvondo", "Cheffe du service des ressources humaines", "RH", "u-dg", "Responsable de service" },
            { "u-rh", "Thierry Bekolo Ngoa", "Gestionnaire des ressources humaines", "RH", "u-rh-chef", "Collaborateur" },
            { "u-admin", "Serge Mouelle Kombi", "Super administrateur", null, null, "Super administrateur" },
        };
        for (Object[] p : l) {
            m.put((String) p[0], new DocumentOrganigramme.Personne((String) p[0], (String) p[1], (String) p[2], (String) p[3], (String) p[4], List.of((String) p[5])));
        }
        return m;
    }

    static DocumentOrganigramme.Rendu rendu(String racine, RenduPdf.Format format, boolean paysage, boolean membres) {
        return rendu(racine, format, paysage, membres, Map.of());
    }

    static DocumentOrganigramme.Rendu rendu(String racine, RenduPdf.Format format, boolean paysage, boolean membres, Map<String, String> photos) {
        List<Unite> u = racine == null ? unites() : unites().stream().filter(x -> List.of("DES", "SNA", "PROD_MONETIQUE", "PROD_INFO", "POLE_RESEAU", "POLE_SYSTEME", "CELLULE_QUALITE").contains(x.code())).toList();
        return DocumentOrganigramme.composer(new DocumentOrganigramme.Contenu(u, racine, personnes(), photos, EDITION, new RenduPdf.Mise(format, paysage), membres));
    }

    @Test
    @DisplayName("Document autonome : polices et logos embarqués, aucune adresse externe, aucun script")
    void autonome() {
        String h = rendu(null, RenduPdf.Format.A3, true, true).html();
        assertThat(h).startsWith("<!DOCTYPE html>").contains("@font-face{font-family:'Ubuntu'").contains("@font-face{font-family:'Inter'");
        assertThat(h).contains("src=\"data:image/svg+xml;base64,").contains("src=\"data:image/png;base64,");
        assertThat(h).doesNotContainIgnoringCase("<script").doesNotContainIgnoringCase("<link").doesNotContain("https://");
        // Seul l'espace de noms SVG porte une adresse, jamais une ressource à charger.
        assertThat(Pattern.compile("(src|href)\\s*=\\s*\"(?!data:)").matcher(h).find()).isFalse();
        assertThat(h.replace("xmlns=\"http://www.w3.org/2000/svg\"", "")).doesNotContain("http://");
    }

    @Test
    @DisplayName("Vue d'ensemble : en-tête, date de Douala, une carte par unité, chefs, effectifs, légende des types")
    void vueEnsemble() {
        String h = rendu(null, RenduPdf.Format.A3, true, false).html();
        assertThat(h).contains("<h1>Organigramme</h1>").contains("Toute l'organisation · 12 unités · 16 personnes");
        assertThat(h).contains("Édité le 9 octobre 2026 à 14:32 (heure de Douala)");
        assertThat(h.split("class=\"carte t-", -1)).hasSize(13);
        assertThat(h).contains("class=\"carte t-direction\"").contains("class=\"carte t-cellule\"");
        assertThat(h).contains("Jean-Baptiste Ella Mintsa").contains("Chef non désigné");
        // Effectif de la direction générale : toutes les personnes rattachées (hors celle sans unité).
        assertThat(h).contains("</svg>16</span>");
        assertThat(h).contains("Types d'unité").contains(">Direction<").contains(">Département<").contains(">Service<").contains(">Pôle<").contains(">Cellule<");
        assertThat(h).doesNotContain("Membres par unité");
    }

    @Test
    @DisplayName("Annexe des membres : chef en tête, fonction et profils, n+1, personnes sans unité")
    void annexe() {
        String h = rendu(null, RenduPdf.Format.A4, false, true).html();
        assertThat(h).contains("Membres par unité");
        int chef = h.indexOf("Rachel Biyong Mengue<span class=\"badge\">Chef d'unité</span>");
        int membre = h.indexOf("Patrick Mbida Essomba", chef);
        assertThat(chef).isPositive();
        assertThat(membre).isGreaterThan(chef);
        assertThat(h).contains("Sans unité").contains("Serge Mouelle Kombi").contains("Cellule qualité").contains("Aucun membre rattaché directement.");
        // A4 portrait : une colonne.
        assertThat(h).contains(".colonnes{}");
    }

    @Test
    @DisplayName("Branche : périmètre nommé, unités de la branche seulement, pas de section « Sans unité »")
    void branche() {
        String h = rendu("DES", RenduPdf.Format.A3, true, true).html();
        assertThat(h).contains("Branche « Département Exploitation et Supervision » · 7 unités");
        assertThat(h).doesNotContain("Direction Générale").doesNotContain("Sans unité");
    }

    @Test
    @DisplayName("Photo du chef : image embarquée à la place des initiales")
    void photoDuChef() throws IOException {
        String h = rendu(null, RenduPdf.Format.A3, true, false, Map.of("u-dg", photo(java.awt.Color.BLUE))).html();
        assertThat(h).contains("<img class=\"avatar \" alt=\"\" src=\"data:image/png;base64,");
    }

    @Test
    @DisplayName("Textes des données échappés : aucun balisage injecté")
    void echappement() {
        List<Unite> u = List.of(new Unite("X", "<script>alert(1)</script> & \"co\"", TypeUnite.DIRECTION, null, true, null));
        String h = DocumentOrganigramme.composer(new DocumentOrganigramme.Contenu(u, null, Map.of(), Map.of(), EDITION, new RenduPdf.Mise(RenduPdf.Format.A4, true), true)).html();
        assertThat(h).doesNotContain("<script>").contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; &quot;co&quot;");
        assertThat(DocumentOrganigramme.esc("l'a\u0001")).isEqualTo("l&#39;a ");
        assertThat(DocumentOrganigramme.initiales("  aïcha  mahamat nour")).isEqualTo("AM");
        assertThat(DocumentOrganigramme.initiales(null)).isEqualTo("?");
    }

    @Test
    @DisplayName("Pied de page : Moabi · Organigramme · page x sur y, document confidentiel")
    void pied() {
        String p = rendu(null, RenduPdf.Format.A3, true, false).piedDePage();
        assertThat(p).contains("class=\"pageNumber\"").contains("class=\"totalPages\"").contains("Organigramme · page").contains("Document confidentiel · usage interne");
    }

    @Test
    @DisplayName("Organisation large en A4 : pages de détail des branches principales")
    void detailDesBranches() {
        String a4 = rendu(null, RenduPdf.Format.A4, false, false).html();
        assertThat(a4).contains("Organigramme · détail d'une branche");
        String a3 = rendu("DES", RenduPdf.Format.A3, true, false).html();
        assertThat(a3).doesNotContain("détail d'une branche");
    }

    @Test
    @DisplayName("Aucune unité : page explicite")
    void vide() {
        String h = DocumentOrganigramme.composer(new DocumentOrganigramme.Contenu(List.of(), null, Map.of(), Map.of(), EDITION, new RenduPdf.Mise(RenduPdf.Format.A3, true), false)).html();
        assertThat(h).contains("Aucune unité active dans ce périmètre.");
    }

    /**
     * Écrit le document de démonstration (A3 paysage avec annexe, A4 portrait) dans le dossier GIMAC_ORGANIGRAMME_ESSAI_DOSSIER,
     * pour une impression réelle par le moteur (ConvertisseurHtmlGotenbergReelTest de MS-DOCUMENT).
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "GIMAC_ORGANIGRAMME_ESSAI_DOSSIER", matches = ".+")
    void ecrireEssai() throws IOException {
        Path d = Path.of(System.getenv("GIMAC_ORGANIGRAMME_ESSAI_DOSSIER"));
        Files.createDirectories(d);
        Map<String, String> photos = Map.of("u-dg", photo(new java.awt.Color(0x15, 0x56, 0x9E)), "u-rssi", photo(new java.awt.Color(0x0B, 0x69, 0x38)));
        DocumentOrganigramme.Rendu a3 = rendu(null, RenduPdf.Format.A3, true, true, photos);
        Files.writeString(d.resolve("organigramme-a3.html"), a3.html(), StandardCharsets.UTF_8);
        Files.writeString(d.resolve("pied.html"), a3.piedDePage(), StandardCharsets.UTF_8);
        Files.writeString(d.resolve("organigramme-a4.html"), rendu(null, RenduPdf.Format.A4, false, false, photos).html(), StandardCharsets.UTF_8);
        Files.writeString(d.resolve("organigramme-branche.html"), rendu("DES", RenduPdf.Format.A4, true, true, photos).html(), StandardCharsets.UTF_8);
    }

    /**
     * Exemple de démonstration du frontend (services simulés de la console, D-102 §5) : mêmes unités, chefs et personnes
     * que le jeu de démonstration (frontend/packages/administration/src/services/simule), filigrane et pied de page
     * « Démonstration · données fictives ». Imprimé ensuite par ConvertisseurHtmlGotenbergReelTest de MS-DOCUMENT et copié
     * dans frontend/packages/administration/src/assets/organigramme-demonstration.pdf.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "GIMAC_ORGANIGRAMME_ESSAI_DOSSIER", matches = ".+")
    void ecrireDemonstration() throws IOException {
        List<Unite> u = List.of(
            new Unite("DG", "Direction Générale", TypeUnite.DIRECTION, null, true, "u-dg"),
            new Unite("DES", "Département Exploitation et Supervision", TypeUnite.DEPARTEMENT, "DG", true, "u-chef-des"),
            new Unite("SNA", "Service Normes et Agréments", TypeUnite.SERVICE, "DES", true, null),
            new Unite("PROD_MONETIQUE", "Service Production monétique", TypeUnite.SERVICE, "DES", true, null),
            new Unite("PROD_INFO", "Service Production informatique", TypeUnite.SERVICE, "DES", true, null),
            new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", true, null),
            new Unite("POLE_SYSTEME", "Pôle système", TypeUnite.POLE, "PROD_INFO", true, null),
            new Unite("DDRM", "Département Développement du Réseau et du Marché", TypeUnite.DEPARTEMENT, "DG", true, "u-chef-ddrm"),
            new Unite("SSI", "Sécurité des systèmes d'information", TypeUnite.SERVICE, "DG", true, "u-rssi"),
            new Unite("CONFORMITE", "Conformité", TypeUnite.SERVICE, "DG", true, "u-conformite"),
            new Unite("RH", "Service des ressources humaines", TypeUnite.SERVICE, "DG", true, null)
        );
        Map<String, DocumentOrganigramme.Personne> m = new LinkedHashMap<>();
        Object[][] l = {
            { "u-super-admin", "Serge Mouelle Kombi", "Super administrateur de la plateforme", null, null, "Super administrateur" },
            { "u-rssi", "Hervé Kamga Tchoupo", "Responsable de la sécurité des SI", "SSI", "u-dg", "RSSI" },
            { "u-admin-systeme", "Thierry Abessolo Nkoulou", "Administrateur système de la plateforme", "PROD_INFO", "u-chef-des", "Administrateur système" },
            { "u-chef-des", "Christian Ndong Obiang", "Chef du DES", "DES", "u-dg", "Chef du DES" },
            { "u-ingenieur", "Patrick Mbida Essomba", "Ingénieur d'homologation", "SNA", "u-chef-des", "Ingénieur d'homologation" },
            { "u-chef-ddrm", "Aïssatou Ngo Mbarga", "Cheffe de la DDRM", "DDRM", "u-dg", "Chef de la DDRM" },
            { "u-charge-ddrm", "Joël Essomba Atangana", "Chargé d'études DDRM", "DDRM", "u-chef-ddrm", "Chargé DDRM" },
            { "u-conformite", "Mariam Oumarou Bello", "Responsable conformité", "CONFORMITE", "u-dg", "Conformité" },
            { "u-dg", "François Tchoumi Kamga", "Directeur général adjoint", "DG", null, "Direction générale" },
            { "u-sna", "Rachel Biyong Mengue", "Analyste Normes et Agréments", "SNA", "u-chef-des", "Normes et Agréments" },
            { "u-premier-acces", "Laetitia Nguema Ondo", "Opératrice production monétique", "PROD_MONETIQUE", "u-chef-des", "Production monétique" },
            { "u-sans-2fa", "Bachir Hamadou Ali", "Ingénieur système", null, null, "Pôle système" },
        };
        for (Object[] p : l) {
            m.put((String) p[0], new DocumentOrganigramme.Personne((String) p[0], (String) p[1], (String) p[2], (String) p[3], (String) p[4], List.of((String) p[5])));
        }
        DocumentOrganigramme.Rendu r = DocumentOrganigramme.composer(new DocumentOrganigramme.Contenu(u, null, m, Map.of(), EDITION, new RenduPdf.Mise(RenduPdf.Format.A3, true), true));
        String filigrane =
            "<style>.filigrane{position:fixed;top:42%;left:0;right:0;text-align:center;font-family:'Ubuntu',sans-serif;font-size:80pt;font-weight:700;" +
            "letter-spacing:.2em;color:rgba(21,86,158,.07);transform:rotate(-14deg);z-index:10}</style><div class=\"filigrane\">DÉMONSTRATION</div>";
        String html = r.html().replaceFirst("<body>", "<body>" + filigrane).replace("(heure de Douala)</div>", "(heure de Douala) · exemple de démonstration, données fictives</div>");
        Path d = Path.of(System.getenv("GIMAC_ORGANIGRAMME_ESSAI_DOSSIER"));
        Files.createDirectories(d);
        Files.writeString(d.resolve("organigramme-demonstration.html"), html, StandardCharsets.UTF_8);
        Files.writeString(d.resolve("pied-demonstration.html"), r.piedDePage().replace("Document confidentiel · usage interne", "Démonstration · données fictives"), StandardCharsets.UTF_8);
    }

    /** Photo de profil fictive (silhouette sur fond uni), en data URI PNG. */
    static String photo(java.awt.Color fond) throws IOException {
        java.awt.image.BufferedImage i = new java.awt.image.BufferedImage(96, 96, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = i.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new java.awt.GradientPaint(0, 0, fond.brighter(), 96, 96, fond.darker()));
        g.fillRect(0, 0, 96, 96);
        g.setColor(new java.awt.Color(255, 255, 255, 220));
        g.fillOval(30, 16, 36, 36);
        g.fillOval(14, 58, 68, 60);
        g.dispose();
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(i, "png", o);
        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(o.toByteArray());
    }
}
