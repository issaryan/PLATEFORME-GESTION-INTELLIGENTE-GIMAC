package cm.gimac.des.document.application.usecase.gabarit;

/**
 * Feuille de style du gabarit commun (D-139), reprise de l'organigramme de MS-IAM (étalon) : couleurs de la charte,
 * en-tête aux deux logos avec filet bleu et accent vert, titres en Ubuntu, texte en Inter, tableaux à en-tête
 * capitalisé souligné de bleu et lignes alternées. Classes offertes aux documents : {@code h2} (section), {@code h3},
 * {@code table} (liste), {@code table.fiche} (libellé et valeur), {@code table.compact} (dense), {@code .colonnes} (deux colonnes), {@code .bloc}
 * (insécable), {@code .encart} (note), {@code .alerte}, {@code .mention}, {@code .badge} et ses variantes
 * {@code .ok}, {@code .ko}, {@code .attente}, {@code .empreinte}, {@code .code}, {@code .muet}, {@code .valeur}.
 */
final class StylesGabarit {

    private StylesGabarit() {}

    static final String FEUILLE =
        ":root{--bleu:#15569E;--bleu-fonce:#0F3D70;--vert:#0B6938;--brun:#8A5A14;--rouge:#B42318;--texte:#14161A;--second:#5C6268;--bord:#D5D8DC;--fond:#F5F6F8}" +
        "*{box-sizing:border-box}html,body{margin:0;padding:0;-webkit-print-color-adjust:exact;print-color-adjust:exact}" +
        "body{font-family:'Inter','Liberation Sans','DejaVu Sans',Arial,sans-serif;color:var(--texte);font-size:9pt;line-height:1.4}" +
        "h1,h2,h3{font-family:'Ubuntu','Inter','Liberation Sans','DejaVu Sans',sans-serif}" +
        // En-tête de la première page, identique à celui de l'organigramme.
        ".tete{display:flex;align-items:center;gap:8mm;min-height:31mm;padding-bottom:3mm;border-bottom:0.6mm solid var(--bleu);position:relative;margin-bottom:6mm}" +
        ".tete::after{content:'';position:absolute;left:0;bottom:-1.6mm;width:42mm;height:0.6mm;background:var(--vert)}" +
        ".logo-moabi{height:15mm;flex:none}.logo-gimac{height:15mm;flex:none}.titres{flex:1;text-align:center;min-width:0}" +
        ".surtitre{font-size:8pt;letter-spacing:.12em;text-transform:uppercase;color:var(--second)}" +
        "h1{margin:1mm 0 1mm;font-size:24pt;font-weight:700;color:var(--bleu);letter-spacing:.01em;line-height:1.15}" +
        ".sous-titre{font-size:10pt;font-weight:500}.edition{font-size:8pt;color:var(--second);margin-top:.8mm}" +
        "h1.long{font-size:15pt}.etroit h1.long{font-size:13pt}" +
        ".etroit .tete{gap:4mm;min-height:26mm}.etroit .logo-moabi,.etroit .logo-gimac{height:10mm}.etroit .surtitre{font-size:6.3pt;letter-spacing:.05em}.etroit h1{font-size:19pt}" +
        // Cartouche des données clés.
        ".faits{display:flex;flex-wrap:wrap;border:0.3mm solid var(--bord);background:var(--fond);margin:0 0 6mm;break-inside:avoid}" +
        ".fait{flex:1 1 0;min-width:30mm;padding:2mm 3.5mm;border-left:0.3mm solid var(--bord)}.fait:first-child{border-left:0}" +
        ".fait-libelle{font-size:6.6pt;text-transform:uppercase;letter-spacing:.08em;color:var(--second);font-weight:600}" +
        ".fait-valeur{font-size:9.5pt;font-weight:600;margin-top:.5mm;overflow-wrap:anywhere}" +
        // Sections.
        "h2{display:flex;align-items:center;gap:2.2mm;margin:6mm 0 2.5mm;padding-bottom:1.2mm;border-bottom:0.3mm solid var(--bord);font-size:12.5pt;font-weight:700;color:var(--bleu);break-after:avoid;page-break-after:avoid}" +
        "h2::before{content:'';flex:none;width:1.2mm;height:4.4mm;background:var(--vert)}" +
        "h3{margin:4mm 0 1.8mm;font-size:10.5pt;font-weight:500;break-after:avoid}p{margin:0 0 2mm}" +
        // Tableaux, comme l'annexe des membres de l'organigramme.
        "table{width:100%;border-collapse:collapse;font-size:8.5pt;margin:0 0 4mm}thead{display:table-header-group}tr{break-inside:avoid;page-break-inside:avoid}" +
        "thead th{text-align:left;font-weight:600;font-size:6.8pt;text-transform:uppercase;letter-spacing:.07em;color:var(--second);padding:1.6mm 2.4mm;border-bottom:0.4mm solid var(--bleu);background:var(--fond)}" +
        "td{padding:1.6mm 2.4mm;border-bottom:0.2mm solid #E7E9EC;vertical-align:top}tbody tr:nth-child(even) td{background:#FAFBFC}" +
        "table.fiche{table-layout:fixed}table.fiche th{width:32%;text-align:left;vertical-align:top;font-weight:600;color:var(--second);padding:1.6mm 2.4mm;border-bottom:0.2mm solid #E7E9EC;background:var(--fond)}" +
        "table.fiche tbody tr:nth-child(even) td{background:none}" +
        "table.compact{font-size:8pt}table.compact td,table.compact thead th{padding:1.1mm 2mm}" +
        ".colonnes{display:grid;grid-template-columns:1fr 1fr;gap:0 6mm}.bloc{break-inside:avoid;page-break-inside:avoid}" +
        // Notes, états et valeurs.
        ".encart{border-left:1mm solid var(--brun);background:#FBF7F1;padding:2.5mm 3.5mm;margin:0 0 4mm;break-inside:avoid}" +
        ".alerte{color:var(--rouge)}.encart.alerte{border-left-color:var(--rouge);background:#FDF3F2}" +
        ".mention{margin-top:6mm;text-align:right;color:var(--second);font-size:8pt}.muet{color:var(--second);font-style:italic}" +
        ".badge{display:inline-block;padding:.3mm 1.8mm;font-size:6.6pt;font-weight:600;text-transform:uppercase;letter-spacing:.06em;color:#fff;background:var(--bleu);white-space:nowrap}" +
        ".badge.ok{background:var(--vert)}.badge.ko{background:var(--rouge)}.badge.attente{background:var(--brun)}" +
        ".valeur{white-space:pre-wrap;overflow-wrap:anywhere}.code,.empreinte{font-family:'Liberation Mono','DejaVu Sans Mono',monospace}" +
        ".code{font-size:7.8pt}.empreinte{font-size:6.8pt;color:var(--second);word-break:break-all}" +
        // Filigrane répété sur chaque page (position fixe à l'impression).
        ".filigrane{position:fixed;top:42%;left:0;right:0;text-align:center;font-family:'Ubuntu',sans-serif;font-size:72pt;font-weight:700;" +
        "letter-spacing:.2em;text-transform:uppercase;color:rgba(21,86,158,.07);transform:rotate(-14deg);z-index:10;pointer-events:none}";

    /** Pied de page, dans la marge basse ; police de repli (le moteur n'embarque pas les polices du document). */
    static final String PIED =
        "html,body{margin:0;padding:0;-webkit-print-color-adjust:exact;print-color-adjust:exact}" +
        ".pied{width:100%;box-sizing:border-box;padding:0 12mm;display:flex;justify-content:space-between;align-items:center;gap:6mm;" +
        "font-family:\"Inter\",\"Liberation Sans\",\"DejaVu Sans\",Arial,sans-serif;font-size:7.5pt;color:#5C6268}" +
        ".g{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.m{color:#15569E;font-weight:600}" +
        ".d{white-space:nowrap}.c{letter-spacing:.06em;text-transform:uppercase;font-size:6.5pt}";
}
