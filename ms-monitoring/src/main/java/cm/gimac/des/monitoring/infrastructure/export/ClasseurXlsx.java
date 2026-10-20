package cm.gimac.des.monitoring.infrastructure.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Classeur Excel (Office Open XML, .xlsx) écrit sans dépendance (D-117, programme téléchargeable) : une feuille, cellules
 * de texte en ligne, styles titre, en-tête et cellule bordée, largeurs de colonnes, cellules fusionnées.
 */
public final class ClasseurXlsx {

    public enum Style {
        NORMAL(0),
        TITRE(1),
        ENTETE(2),
        CELLULE(3),
        NOTE(4);

        final int indice;

        Style(int indice) {
            this.indice = indice;
        }
    }

    private record Cellule(String texte, Style style) {}

    private final String feuille;
    private final List<List<Cellule>> lignes = new ArrayList<>();
    private final List<String> fusions = new ArrayList<>();
    private double[] largeurs = new double[0];

    public ClasseurXlsx(String feuille) {
        this.feuille = feuille.length() > 31 ? feuille.substring(0, 31) : feuille;
    }

    public ClasseurXlsx largeurs(double... l) {
        this.largeurs = l.clone();
        return this;
    }

    /** Ajoute une ligne ; rend son numéro (à partir de 1). */
    public int ligne(Style style, String... textes) {
        List<Cellule> l = new ArrayList<>();
        for (String t : textes) {
            l.add(new Cellule(t == null ? "" : t, style));
        }
        lignes.add(l);
        return lignes.size();
    }

    /** Fusionne des cellules d'une ligne, colonnes numérotées à partir de 0. */
    public ClasseurXlsx fusionner(int ligne, int de, int a) {
        fusions.add(colonne(de) + ligne + ":" + colonne(a) + ligne);
        return this;
    }

    static String colonne(int i) {
        StringBuilder s = new StringBuilder();
        for (int n = i + 1; n > 0; n = (n - 1) / 26) {
            s.insert(0, (char) ('A' + (n - 1) % 26));
        }
        return s.toString();
    }

    static String xml(String t) {
        StringBuilder s = new StringBuilder();
        for (char c : t.toCharArray()) {
            switch (c) {
                case '&' -> s.append("&amp;");
                case '<' -> s.append("&lt;");
                case '>' -> s.append("&gt;");
                case '"' -> s.append("&quot;");
                default -> {
                    if (c >= 0x20 || c == '\t' || c == '\n') {
                        s.append(c);
                    }
                }
            }
        }
        return s.toString();
    }

    public byte[] octets() {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(sortie, StandardCharsets.UTF_8)) {
            ecrire(zip, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
            ecrire(zip, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            ecrire(zip, "xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets><sheet name=\"" + xml(feuille) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            ecrire(zip, "xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
            ecrire(zip, "xl/styles.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                "<fonts count=\"4\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"14\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font><font><i/><sz val=\"10\"/><name val=\"Calibri\"/></font></fonts>" +
                "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE8EEF4\"/></patternFill></fill></fills>" +
                "<borders count=\"2\"><border/><border><left style=\"thin\"/><right style=\"thin\"/><top style=\"thin\"/><bottom style=\"thin\"/></border></borders>" +
                "<cellStyleXfs count=\"1\"><xf/></cellStyleXfs><cellXfs count=\"5\"><xf/>" +
                "<xf fontId=\"1\" applyFont=\"1\"/>" +
                "<xf fontId=\"2\" fillId=\"2\" borderId=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\" wrapText=\"1\"/></xf>" +
                "<xf borderId=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\" wrapText=\"1\"/></xf>" +
                "<xf fontId=\"3\" applyFont=\"1\"/></cellXfs></styleSheet>");
            StringBuilder s = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
            if (largeurs.length > 0) {
                s.append("<cols>");
                for (int i = 0; i < largeurs.length; i++) {
                    s.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1).append("\" width=\"").append(largeurs[i]).append("\" customWidth=\"1\"/>");
                }
                s.append("</cols>");
            }
            s.append("<sheetData>");
            for (int r = 0; r < lignes.size(); r++) {
                s.append("<row r=\"").append(r + 1).append("\">");
                List<Cellule> l = lignes.get(r);
                for (int c = 0; c < l.size(); c++) {
                    Cellule x = l.get(c);
                    s.append("<c r=\"").append(colonne(c)).append(r + 1).append("\" s=\"").append(x.style().indice).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(x.texte())).append("</t></is></c>");
                }
                s.append("</row>");
            }
            s.append("</sheetData>");
            if (!fusions.isEmpty()) {
                s.append("<mergeCells count=\"").append(fusions.size()).append("\">");
                fusions.forEach(f -> s.append("<mergeCell ref=\"").append(f).append("\"/>"));
                s.append("</mergeCells>");
            }
            s.append("<pageSetup orientation=\"landscape\" paperSize=\"9\"/></worksheet>");
            ecrire(zip, "xl/worksheets/sheet1.xml", s.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sortie.toByteArray();
    }

    private static void ecrire(ZipOutputStream zip, String nom, String contenu) throws IOException {
        zip.putNextEntry(new ZipEntry(nom));
        zip.write(contenu.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
