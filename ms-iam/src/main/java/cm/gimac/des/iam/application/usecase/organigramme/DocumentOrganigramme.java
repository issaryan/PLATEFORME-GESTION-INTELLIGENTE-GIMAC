package cm.gimac.des.iam.application.usecase.organigramme;

import cm.gimac.des.iam.application.port.out.RenduPdf;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.Collator;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Document HTML autonome de l'organigramme (D-102, docs/lot1/organigramme.md §4), imprimé en PDF par le moteur de
 * MS-DOCUMENT : en-tête aux logos de Moabi et du GIMAC, organigramme descendant mis à l'échelle de la page (cartes aux
 * couleurs de la charte, chef avec sa photo ou ses initiales, effectif), légende des types d'unité, pages de détail des
 * branches quand la vue d'ensemble est trop réduite, puis, sur demande, l'annexe des membres par unité. Polices de la
 * charte (Ubuntu, Inter) et images embarquées en data URI : aucune ressource externe, aucun script. Tout texte issu
 * des données est échappé.
 */
public final class DocumentOrganigramme {

    /** Personne interne active : identité, fonction, rattachement et libellés de ses profils. */
    public record Personne(String id, String nomAffiche, String fonction, String unite, String responsableId, List<String> profils) {}

    /**
     * Données du document.
     *
     * @param unites unités du périmètre (racine et sous-unités, ou toute l'organisation), actives
     * @param racine code de la branche demandée, nul pour toute l'organisation
     * @param personnes personnes internes actives, par identifiant (toutes, pour nommer les n+1 hors périmètre)
     * @param photos photo de profil (data URI) des chefs qui en ont une, par identifiant
     */
    public record Contenu(List<Unite> unites, String racine, Map<String, Personne> personnes, Map<String, String> photos, ZonedDateTime edition, RenduPdf.Mise mise, boolean membres) {}

    /** Document et pied de page. */
    public record Rendu(String html, String piedDePage) {}

    static final double MARGE_MM = 12;
    static final double MARGE_BAS_MM = 16;
    static final double PX_PAR_MM = 96 / 25.4;
    /** En deçà de cette échelle, chaque branche principale a en plus sa page de détail. */
    static final double SEUIL_DETAIL = 0.62;
    static final double ECHELLE_MAX = 1.25;

    static final Map<TypeUnite, String> COULEURS = new EnumMap<>(
        Map.of(TypeUnite.DIRECTION, "#15569E", TypeUnite.DEPARTEMENT, "#1F80C9", TypeUnite.SERVICE, "#0B6938", TypeUnite.POLE, "#6A4FB8", TypeUnite.CELLULE, "#8A5A14")
    );
    static final Map<TypeUnite, String> LIBELLES = new EnumMap<>(
        Map.of(TypeUnite.DIRECTION, "Direction", TypeUnite.DEPARTEMENT, "Département", TypeUnite.SERVICE, "Service", TypeUnite.POLE, "Pôle", TypeUnite.CELLULE, "Cellule")
    );
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    private final Contenu c;
    private final Map<String, Unite> parCode = new LinkedHashMap<>();
    private final Map<String, List<String>> enfants = new LinkedHashMap<>();
    private final Map<String, List<Personne>> membres = new LinkedHashMap<>();
    private final Collator ordre = Collator.getInstance(Locale.FRENCH);

    private DocumentOrganigramme(Contenu c) {
        this.c = c;
        c.unites().forEach(u -> parCode.put(u.code(), u));
        for (Unite u : c.unites()) {
            String p = u.parent() != null && parCode.containsKey(u.parent()) && !u.code().equals(c.racine()) ? u.parent() : null;
            enfants.computeIfAbsent(p, k -> new ArrayList<>()).add(u.code());
        }
        enfants.values().forEach(l -> l.sort(Comparator.comparing((String x) -> parCode.get(x).libelle(), ordre)));
        c.personnes().values().forEach(p -> membres.computeIfAbsent(p.unite(), k -> new ArrayList<>()).add(p));
    }

    /** Compose le document et son pied de page. */
    public static Rendu composer(Contenu c) {
        return new DocumentOrganigramme(c).composer();
    }

    private Rendu composer() {
        StringBuilder h = new StringBuilder(64_000);
        double largeurMm = c.mise().largeurMm() - 2 * MARGE_MM;
        double hauteurMm = c.mise().hauteurMm() - MARGE_MM - MARGE_BAS_MM;
        h.append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"utf-8\"><title>Organigramme</title><style>");
        h.append(Ressources.polices()).append(style(largeurMm, hauteurMm));
        h.append("</style></head><body").append(largeurMm < 250 ? " class=\"etroit\"" : "").append(">");

        List<String> racines = enfants.getOrDefault(null, List.of());
        // Vue d'ensemble.
        double hautTete = 33;
        double hautLegende = 11;
        double zoneH = hauteurMm - hautTete - hautLegende - 6;
        MiseEnPageArbre.Plan plan = MiseEnPageArbre.planifier(racines, k -> enfants.getOrDefault(k, List.of()));
        double echelle = echelle(plan, largeurMm, zoneH);
        h.append("<section class=\"page\">");
        entete(h, null);
        h.append("<div class=\"zone\">");
        if (racines.isEmpty()) {
            h.append("<p class=\"vide\">Aucune unité active dans ce périmètre.</p>");
        } else {
            dessin(h, plan, echelle);
        }
        h.append("</div>");
        legende(h);
        h.append("</section>");

        // Détail des branches principales quand la vue d'ensemble est trop réduite.
        if (echelle < SEUIL_DETAIL) {
            List<String> branches = new ArrayList<>();
            for (String r : racines) {
                List<String> l = enfants.getOrDefault(r, List.of());
                (l.isEmpty() ? List.of(r) : l).forEach(branches::add);
            }
            for (String b : branches) {
                if (enfants.getOrDefault(b, List.of()).isEmpty()) {
                    continue;
                }
                MiseEnPageArbre.Plan p = MiseEnPageArbre.planifier(List.of(b), k -> enfants.getOrDefault(k, List.of()));
                double z = hauteurMm - 18 - hautLegende - 6;
                h.append("<section class=\"page\">");
                entete(h, parCode.get(b));
                h.append("<div class=\"zone\">");
                dessin(h, p, echelle(p, largeurMm, z));
                h.append("</div>");
                legende(h);
                h.append("</section>");
            }
        }

        if (c.membres()) {
            annexe(h);
        }
        h.append("</body></html>");
        return new Rendu(h.toString(), pied());
    }

    private static double echelle(MiseEnPageArbre.Plan p, double largeurMm, double hauteurMm) {
        if (p.largeur() <= 0 || p.hauteur() <= 0) {
            return 1;
        }
        return Math.min(ECHELLE_MAX, Math.min(largeurMm * PX_PAR_MM / p.largeur(), hauteurMm * PX_PAR_MM / p.hauteur()));
    }

    // ------------------------------------------------------------------ en-tête et légende

    private void entete(StringBuilder h, Unite branche) {
        int personnes = 0;
        for (Unite u : parCode.values()) {
            personnes += membres.getOrDefault(u.code(), List.of()).size();
        }
        String perimetre = c.racine() == null
            ? "Toute l'organisation"
            : "Branche « " + esc(parCode.containsKey(c.racine()) ? parCode.get(c.racine()).libelle() : c.racine()) + " »";
        if (branche != null) {
            h.append("<header class=\"tete tete-courte\"><img class=\"logo-moabi\" alt=\"Moabi\" src=\"").append(Ressources.logoMoabi()).append("\">");
            h.append("<div class=\"titres\"><div class=\"surtitre\">Organigramme · détail d'une branche</div><h1>")
                .append(esc(branche.libelle()))
                .append("</h1></div>");
            h.append("<img class=\"logo-gimac\" alt=\"GIMAC\" src=\"").append(Ressources.logoGimac()).append("\"></header>");
            return;
        }
        h.append("<header class=\"tete\"><img class=\"logo-moabi\" alt=\"Moabi\" src=\"").append(Ressources.logoMoabi()).append("\">");
        h.append("<div class=\"titres\"><div class=\"surtitre\">Groupement Interbancaire Monétique de l'Afrique Centrale</div><h1>Organigramme</h1>");
        h.append("<div class=\"sous-titre\">")
            .append(perimetre)
            .append(" · ")
            .append(parCode.size())
            .append(parCode.size() > 1 ? " unités" : " unité")
            .append(" · ")
            .append(personnes)
            .append(personnes > 1 ? " personnes" : " personne")
            .append("</div>");
        h.append("<div class=\"edition\">Édité le ").append(esc(DATE.format(c.edition()))).append(" (heure de Douala)</div></div>");
        h.append("<img class=\"logo-gimac\" alt=\"GIMAC\" src=\"").append(Ressources.logoGimac()).append("\"></header>");
    }

    private void legende(StringBuilder h) {
        h.append("<footer class=\"legende\"><span class=\"legende-titre\">Types d'unité</span>");
        for (TypeUnite t : TypeUnite.values()) {
            h.append("<span class=\"leg\"><i style=\"background:").append(COULEURS.get(t)).append("\"></i>").append(LIBELLES.get(t)).append("</span>");
        }
        h.append("<span class=\"leg leg-sep\"><span class=\"avatar avatar-mini\">AB</span>Chef d'unité (photo ou initiales)</span>");
        h.append("<span class=\"leg\">").append(ICONE_PERSONNES).append("Effectif : personnes de l'unité et de ses sous-unités</span>");
        h.append("</footer>");
    }

    // ------------------------------------------------------------------ organigramme

    private void dessin(StringBuilder h, MiseEnPageArbre.Plan plan, double echelle) {
        double w = plan.largeur();
        double ht = plan.hauteur();
        h.append("<div class=\"plan\" style=\"width:").append(px(w * echelle)).append(";height:").append(px(ht * echelle)).append("\">");
        h.append("<div class=\"plan-interne\" style=\"width:").append(px(w)).append(";height:").append(px(ht)).append(";zoom:").append(String.format(Locale.ROOT, "%.4f", echelle)).append("\">");
        h.append("<svg class=\"liens\" xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(n(w)).append("\" height=\"").append(n(ht)).append("\" viewBox=\"0 0 ").append(n(w)).append(' ').append(n(ht)).append("\">");
        for (String d : plan.traits()) {
            h.append("<path d=\"").append(d).append("\"/>");
        }
        h.append("</svg>");
        plan.positions().forEach((code, p) -> carte(h, parCode.get(code), p));
        h.append("</div></div>");
    }

    private void carte(StringBuilder h, Unite u, MiseEnPageArbre.Position p) {
        String type = u.type().name().toLowerCase(Locale.ROOT);
        h.append("<div class=\"carte t-").append(type).append("\" style=\"left:").append(px(p.x())).append(";top:").append(px(p.y())).append("\">");
        int effectif = effectif(u.code());
        h.append("<div class=\"carte-tete\"><span class=\"carte-type\">").append(LIBELLES.get(u.type())).append("</span><span class=\"carte-effectif\">");
        h.append(ICONE_PERSONNES).append(effectif).append("</span></div>");
        h.append("<div class=\"carte-libelle\">").append(esc(u.libelle())).append("</div>");
        Personne chef = u.responsableId() == null ? null : c.personnes().get(u.responsableId());
        if (chef == null) {
            h.append("<div class=\"carte-chef sans-chef\"><span class=\"avatar avatar-vide\"></span><span class=\"chef-texte\"><span class=\"chef-nom\">Chef non désigné</span></span></div>");
        } else {
            h.append("<div class=\"carte-chef\">");
            avatar(h, chef, "");
            h.append("<span class=\"chef-texte\"><span class=\"chef-nom\">").append(esc(chef.nomAffiche())).append("</span>");
            h.append("<span class=\"chef-fonction\">").append(esc(chef.fonction() == null || chef.fonction().isBlank() ? "Chef d'unité" : chef.fonction())).append("</span></span></div>");
        }
        h.append("</div>");
    }

    private void avatar(StringBuilder h, Personne p, String classe) {
        String photo = c.photos().get(p.id());
        if (photo != null) {
            h.append("<img class=\"avatar ").append(classe).append("\" alt=\"\" src=\"").append(photo).append("\">");
        } else {
            h.append("<span class=\"avatar ").append(classe).append("\">").append(esc(initiales(p.nomAffiche()))).append("</span>");
        }
    }

    /** Personnes de l'unité et de ses sous-unités. */
    int effectif(String code) {
        int n = 0;
        Set<String> vus = new HashSet<>();
        java.util.Deque<String> a = new java.util.ArrayDeque<>(List.of(code));
        while (!a.isEmpty()) {
            String x = a.poll();
            if (vus.add(x)) {
                n += membres.getOrDefault(x, List.of()).size();
                a.addAll(enfants.getOrDefault(x, List.of()));
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ annexe des membres

    private void annexe(StringBuilder h) {
        h.append("<section class=\"annexe\"><header class=\"annexe-tete\"><img class=\"logo-moabi petit\" alt=\"Moabi\" src=\"").append(Ressources.logoMoabi()).append("\">");
        h.append("<div><div class=\"surtitre\">Annexe</div><h2>Membres par unité</h2>");
        h.append("<p class=\"annexe-aide\">Personnes rattachées directement à chaque unité, chef en tête : fonction, profils et n+1.</p></div></header>");
        h.append("<div class=\"colonnes\">");
        List<String> ordreUnites = new ArrayList<>();
        parcourir(null, ordreUnites);
        for (String code : ordreUnites) {
            bloc(h, parCode.get(code));
        }
        if (c.racine() == null) {
            List<Personne> sans = new ArrayList<>(membres.getOrDefault(null, List.of()));
            if (!sans.isEmpty()) {
                h.append("<div class=\"bloc\"><h3><span class=\"pastille\" style=\"background:#8A8F98\"></span>Sans unité</h3>");
                h.append("<div class=\"chemin\">Personnes internes actives sans rattachement</div>");
                tableau(h, sans, null);
                h.append("</div>");
            }
        }
        h.append("</div></section>");
    }

    private void parcourir(String code, List<String> acc) {
        for (String e : enfants.getOrDefault(code, List.of())) {
            if (!acc.contains(e)) {
                acc.add(e);
                parcourir(e, acc);
            }
        }
    }

    private void bloc(StringBuilder h, Unite u) {
        h.append("<div class=\"bloc\"><h3><span class=\"pastille\" style=\"background:").append(COULEURS.get(u.type())).append("\"></span>");
        h.append(esc(u.libelle())).append(" <span class=\"code\">").append(esc(u.code())).append("</span></h3>");
        List<String> chemin = new ArrayList<>();
        Set<String> vus = new HashSet<>();
        for (Unite p = u.parent() == null ? null : parCode.get(u.parent()); p != null && vus.add(p.code()); p = p.parent() == null ? null : parCode.get(p.parent())) {
            chemin.add(0, esc(p.libelle()));
        }
        h.append("<div class=\"chemin\">").append(LIBELLES.get(u.type()));
        if (!chemin.isEmpty()) {
            h.append(" · ").append(String.join(" › ", chemin));
        }
        h.append("</div>");
        tableau(h, new ArrayList<>(membres.getOrDefault(u.code(), List.of())), u.responsableId());
        h.append("</div>");
    }

    private void tableau(StringBuilder h, List<Personne> l, String chef) {
        if (l.isEmpty()) {
            h.append("<p class=\"aucun\">Aucun membre rattaché directement.</p>");
            return;
        }
        l.sort(Comparator.comparing((Personne p) -> !p.id().equals(chef)).thenComparing(Personne::nomAffiche, ordre));
        h.append("<table><colgroup><col style=\"width:40%\"><col style=\"width:36%\"><col style=\"width:24%\"></colgroup>");
        h.append("<thead><tr><th>Nom</th><th>Fonction · profils</th><th>n+1</th></tr></thead><tbody>");
        for (Personne p : l) {
            boolean estChef = p.id().equals(chef);
            h.append("<tr").append(estChef ? " class=\"chef\"" : "").append("><td><div class=\"nom\">");
            avatar(h, p, "avatar-ligne");
            h.append("<span>").append(esc(p.nomAffiche()));
            if (estChef) {
                h.append("<span class=\"badge\">Chef d'unité</span>");
            }
            h.append("</span></div></td><td><div>").append(esc(p.fonction() == null ? "" : p.fonction())).append("</div>");
            if (!p.profils().isEmpty()) {
                h.append("<div class=\"profils\">").append(esc(String.join(" · ", p.profils()))).append("</div>");
            }
            Personne n1 = p.responsableId() == null ? null : c.personnes().get(p.responsableId());
            h.append("</td><td>").append(n1 == null ? "<span class=\"muet\">Aucun</span>" : esc(n1.nomAffiche())).append("</td></tr>");
        }
        h.append("</tbody></table>");
    }

    // ------------------------------------------------------------------ pied de page

    private static String pied() {
        return """
            <!DOCTYPE html><html><head><meta charset="utf-8"><style>
            html,body{margin:0;padding:0;-webkit-print-color-adjust:exact;print-color-adjust:exact}
            .pied{width:100%;box-sizing:border-box;padding:0 12mm;display:flex;justify-content:space-between;align-items:center;
            font-family:"Inter","Liberation Sans","DejaVu Sans",Arial,sans-serif;font-size:7.5pt;color:#5C6268}
            .m{color:#15569E;font-weight:600}.c{letter-spacing:.06em;text-transform:uppercase;font-size:6.5pt}
            </style></head><body><div class="pied"><span><span class="m">Moabi</span> · Organigramme · page <span class="pageNumber"></span> sur <span class="totalPages"></span></span>
            <span class="c">Document confidentiel · usage interne</span></div></body></html>""";
    }

    // ------------------------------------------------------------------ outils

    static String initiales(String nom) {
        if (nom == null || nom.isBlank()) {
            return "?";
        }
        String[] mots = nom.trim().split("\\s+");
        String a = mots[0].substring(0, 1);
        String b = mots.length > 1 ? mots[1].substring(0, 1) : "";
        return (a + b).toUpperCase(Locale.FRENCH);
    }

    /** Échappement HTML de tout texte issu des données. */
    static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&#39;");
                default -> b.append(ch < 0x20 && ch != '\n' && ch != '\t' ? ' ' : ch);
            }
        }
        return b.toString();
    }

    private static String px(double v) {
        return String.format(Locale.ROOT, "%.1fpx", v);
    }

    private static String n(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    static final String ICONE_PERSONNES =
        "<svg class=\"ico\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2\"/><circle cx=\"9\" cy=\"7\" r=\"4\"/><path d=\"M22 21v-2a4 4 0 0 0-3-3.87\"/><path d=\"M16 3.13a4 4 0 0 1 0 7.75\"/></svg>";

    private String style(double largeurMm, double hauteurMm) {
        StringBuilder s = new StringBuilder(8_000);
        s.append(":root{--bleu:#15569E;--bleu-fonce:#0F3D70;--vert:#0B6938;--texte:#14161A;--second:#5C6268;--bord:#D5D8DC;--fond:#F5F6F8}");
        s.append("*{box-sizing:border-box}html,body{margin:0;padding:0;-webkit-print-color-adjust:exact;print-color-adjust:exact}");
        s.append("body{font-family:'Inter','Liberation Sans','DejaVu Sans',Arial,sans-serif;color:var(--texte);font-size:9pt;line-height:1.35}");
        s.append("h1,h2,h3,.carte-libelle{font-family:'Ubuntu','Inter','Liberation Sans','DejaVu Sans',sans-serif}");
        s.append(".page{width:").append(mm(largeurMm)).append(";height:").append(mm(hauteurMm - 0.6)).append(";display:flex;flex-direction:column;overflow:hidden;break-after:page;page-break-after:always}");
        // En-tête.
        s.append(".tete{display:flex;align-items:center;gap:8mm;height:31mm;padding-bottom:3mm;border-bottom:0.6mm solid var(--bleu);position:relative;flex:none}");
        s.append(".tete::after{content:'';position:absolute;left:0;bottom:-1.6mm;width:42mm;height:0.6mm;background:var(--vert)}");
        s.append(".tete-courte{height:16mm}.tete-courte .logo-moabi{height:10mm}.tete-courte .logo-gimac{height:9mm}.tete-courte h1{font-size:17pt}");
        s.append(".logo-moabi{height:15mm;flex:none}.logo-moabi.petit{height:9mm}.logo-gimac{height:15mm;flex:none}");
        s.append(".titres{flex:1;text-align:center;min-width:0}");
        s.append(".etroit .tete{gap:4mm}.etroit .logo-moabi,.etroit .logo-gimac{height:10mm}.etroit .surtitre{font-size:6.3pt;letter-spacing:.05em}.etroit h1{font-size:21pt}");
        s.append(".surtitre{font-size:8pt;letter-spacing:.12em;text-transform:uppercase;color:var(--second)}");
        s.append("h1{margin:1mm 0 1mm;font-size:26pt;font-weight:700;color:var(--bleu);letter-spacing:.01em}");
        s.append(".sous-titre{font-size:10pt;color:var(--texte);font-weight:500}.edition{font-size:8pt;color:var(--second);margin-top:.8mm}");
        // Zone de l'organigramme.
        s.append(".zone{flex:1;display:flex;align-items:center;justify-content:center;margin-top:4mm;min-height:0}");
        // zoom (et non transform) : la boîte mise à l'échelle ne déborde pas de la page, que Chromium réduirait sinon en entier.
        s.append(".plan{position:relative;flex:none}.plan-interne{position:absolute;left:0;top:0}");
        s.append(".liens{position:absolute;left:0;top:0;overflow:visible}.liens path{fill:none;stroke:#98A2AD;stroke-width:1.6;stroke-linecap:square}");
        s.append(".carte{position:absolute;width:").append(MiseEnPageArbre.LARGEUR).append("px;height:").append(MiseEnPageArbre.HAUTEUR).append("px;background:#fff;border:1px solid var(--bord);border-top:5px solid var(--c);padding:7px 10px 8px;display:flex;flex-direction:column;gap:4px}");
        for (TypeUnite t : TypeUnite.values()) {
            s.append(".t-").append(t.name().toLowerCase(Locale.ROOT)).append("{--c:").append(COULEURS.get(t)).append("}");
        }
        s.append(".t-direction{background:#F3F7FC}");
        s.append(".carte-tete{display:flex;justify-content:space-between;align-items:center;font-size:7.5px;letter-spacing:.1em;text-transform:uppercase;color:var(--c);font-weight:600}");
        s.append(".carte-effectif{display:inline-flex;align-items:center;gap:3px;color:var(--second);letter-spacing:0;font-size:9px}");
        s.append(".ico{width:11px;height:11px;fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}");
        s.append(".carte-libelle{font-size:12.5px;font-weight:500;line-height:1.2;height:30px;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;color:var(--texte)}");
        s.append(".carte-chef{display:flex;align-items:center;gap:7px;margin-top:auto;padding-top:5px;border-top:1px solid #EEF0F2;min-width:0}");
        s.append(".chef-texte{display:flex;flex-direction:column;min-width:0}");
        s.append(".chef-nom{font-size:10.5px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}");
        s.append(".chef-fonction{font-size:8.5px;color:var(--second);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}");
        s.append(".sans-chef .chef-nom{font-weight:400;font-style:italic;color:var(--second)}");
        s.append(".avatar{flex:none;width:28px;height:28px;border-radius:50%;background:var(--c,#15569E);color:#fff;font-size:10px;font-weight:600;display:inline-flex;align-items:center;justify-content:center;object-fit:cover;letter-spacing:.02em}");
        s.append(".avatar-vide{background:#fff;border:1.5px dashed #B4B9C0}");
        s.append(".avatar-mini{width:5mm;height:5mm;font-size:5.5pt;margin-right:1.5mm;--c:#15569E}");
        s.append(".avatar-ligne{width:6mm;height:6mm;font-size:6pt;--c:#5C6268;margin-right:2mm}");
        s.append("tr.chef .avatar-ligne{--c:#15569E}");
        // Légende.
        s.append(".legende{flex:none;display:flex;flex-wrap:wrap;align-items:center;gap:2mm 5mm;margin-top:3mm;padding-top:2.5mm;border-top:0.3mm solid var(--bord);font-size:7.5pt;color:var(--second)}");
        s.append(".legende-titre{font-weight:600;color:var(--texte);text-transform:uppercase;letter-spacing:.08em;font-size:7pt}");
        s.append(".leg{display:inline-flex;align-items:center;gap:1.5mm}.leg i{display:inline-block;width:3.5mm;height:3.5mm}.leg .ico{width:3.5mm;height:3.5mm}.leg-sep{margin-left:3mm}");
        s.append(".vide{color:var(--second);font-style:italic}");
        // Annexe.
        s.append(".annexe{width:").append(mm(largeurMm)).append("}");
        s.append(".annexe-tete{display:flex;align-items:center;gap:6mm;border-bottom:0.6mm solid var(--bleu);padding-bottom:3mm;margin-bottom:5mm}");
        s.append("h2{margin:.5mm 0;font-size:18pt;color:var(--bleu);font-weight:700}.annexe-aide{margin:0;color:var(--second);font-size:8.5pt}");
        boolean deux = largeurMm > 250;
        s.append(".colonnes{").append(deux ? "column-count:2;column-gap:10mm" : "").append("}");
        s.append(".bloc{break-inside:avoid;page-break-inside:avoid;margin-bottom:6mm}");
        s.append("h3{margin:0;font-size:11pt;font-weight:500;display:flex;align-items:center;gap:2mm;color:var(--texte)}");
        s.append(".pastille{display:inline-block;width:3mm;height:3mm;flex:none}.code{font-family:'Liberation Mono','DejaVu Sans Mono',monospace;font-size:7.5pt;color:var(--second);font-weight:400}");
        s.append(".chemin{font-size:7.5pt;color:var(--second);margin:.6mm 0 1.8mm 5mm}");
        s.append("table{width:100%;border-collapse:collapse;table-layout:fixed;font-size:8pt}");
        s.append("thead th{text-align:left;font-weight:600;font-size:6.8pt;text-transform:uppercase;letter-spacing:.07em;color:var(--second);padding:1.4mm 2mm;border-bottom:0.4mm solid var(--bleu);background:var(--fond)}");
        s.append("td{padding:1.5mm 2mm;border-bottom:0.2mm solid #E7E9EC;vertical-align:middle}tbody tr:nth-child(even) td{background:#FAFBFC}");
        s.append(".nom{display:flex;align-items:center;font-weight:500}tr.chef td{background:#EEF4FB!important}");
        s.append(".badge{display:inline-block;margin-left:2mm;padding:.2mm 1.6mm;font-size:6.2pt;font-weight:600;text-transform:uppercase;letter-spacing:.06em;color:#fff;background:var(--bleu)}");
        s.append(".profils{font-size:7pt;color:var(--second)}.muet{color:var(--second);font-style:italic}.aucun{margin:0 0 0 5mm;font-size:8pt;color:var(--second);font-style:italic}");
        return s.toString();
    }

    private static String mm(double v) {
        return String.format(Locale.ROOT, "%.2fmm", v);
    }

    // ------------------------------------------------------------------ ressources embarquées

    /** Polices de la charte et logos, lus une fois dans les ressources de MS-IAM et rendus en data URI. */
    static final class Ressources {

        private static volatile String polices;
        private static volatile String moabi;
        private static volatile String gimac;

        private Ressources() {}

        static String logoMoabi() {
            if (moabi == null) {
                moabi = "data:image/svg+xml;base64," + base64("organigramme/moabi-logo.svg");
            }
            return moabi;
        }

        static String logoGimac() {
            if (gimac == null) {
                gimac = "data:image/png;base64," + base64("organigramme/gimac-logo-couleur.png");
            }
            return gimac;
        }

        static String polices() {
            if (polices == null) {
                String latin = "U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0304,U+0308,U+0329,U+2000-206F,U+20AC,U+2122,U+2191,U+2193,U+2212,U+2215,U+FEFF,U+FFFD";
                String latinExt = "U+0100-02BA,U+02BD-02C5,U+02C7-02CC,U+02CE-02D7,U+02DD-02FF,U+1D00-1DBF,U+1E00-1E9F,U+1EF2-1EFF,U+2020,U+20A0-20AB,U+20AD-20C0,U+2113,U+2C60-2C7F,U+A720-A7FF";
                StringBuilder b = new StringBuilder();
                for (Object[] p : new Object[][] { { "Ubuntu", "ubuntu", 500 }, { "Ubuntu", "ubuntu", 700 }, { "Inter", "inter", 400 }, { "Inter", "inter", 500 }, { "Inter", "inter", 600 } }) {
                    for (String sous : List.of("latin", "latin-ext")) {
                        b.append("@font-face{font-family:'").append(p[0]).append("';font-style:normal;font-weight:").append(p[2]);
                        b.append(";src:url(data:font/woff2;base64,").append(base64("organigramme/polices/" + p[1] + "-" + sous + "-" + p[2] + "-normal.woff2")).append(") format('woff2')");
                        b.append(";unicode-range:").append(sous.equals("latin") ? latin : latinExt).append("}");
                    }
                }
                polices = b.toString();
            }
            return polices;
        }

        private static String base64(String chemin) {
            try (InputStream in = DocumentOrganigramme.class.getClassLoader().getResourceAsStream(chemin)) {
                if (in == null) {
                    throw new IllegalStateException("Ressource absente : " + chemin);
                }
                return Base64.getEncoder().encodeToString(in.readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
