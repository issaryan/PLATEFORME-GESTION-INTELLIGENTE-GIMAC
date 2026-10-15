package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Preuves, livrables, signatures, RACI et délais de chaque opération de l'annexe A du CR-CERT-GIMAC-01 v1.7 (D-109,
 * docs/lot1/referentiel-preuves-signatures.md), lus dans {@code preuves_signatures_cr_v17.json} : une entrée par rang,
 * propre à certains types de projet quand {@code types} est renseigné. Appliqués à la version de l'annexe A v1.7
 * ({@link ConversionAnnexeA}, édition V1_7) : validateur (absent : celui de la conversion), contributeurs et externes
 * ajoutés à ceux de l'annexe, informés (absents : ceux de la conversion), délais cible et maximal, livrables,
 * signatures et preuve attendue décrite. Le fichier n'est jamais modifié une fois repris.
 */
public final class PreuvesSignaturesCrV17 {

    /** Ressource des données (jamais modifiée une fois reprise). */
    public static final String RESSOURCE = "/config/liquibase/data/preuves_signatures_cr_v17.json";

    /** Entrée du fichier : une opération, pour tous ses types ou pour certains. */
    public record Entree(
        String rang,
        List<TypeProjet> types,
        Fonction validateur,
        List<Fonction> consultes,
        List<Fonction> informes,
        List<Fonction> externes,
        Integer delaiJours,
        Integer delaiMaxJours,
        List<Livrable> livrables,
        List<SignatureOperation> signatures,
        String preuveAttendue,
        String note,
        String source
    ) {
        public Entree {
            consultes = consultes == null ? List.of() : List.copyOf(consultes);
            informes = informes == null ? List.of() : List.copyOf(informes);
            externes = externes == null ? List.of() : List.copyOf(externes);
            livrables = livrables == null ? List.of() : List.copyOf(livrables);
            signatures = signatures == null ? List.of() : List.copyOf(signatures);
        }

        /** L'entrée vaut pour ce type de projet. */
        public boolean vaut(TypeProjet t) {
            return types == null || types.isEmpty() || types.contains(t);
        }
    }

    /** Contenu du fichier. */
    public record Fichier(String decision, String libelleVersion, List<Entree> operations) {}

    private PreuvesSignaturesCrV17() {}

    /** Contenu du fichier de données. */
    public static Fichier lire() {
        try (InputStream in = PreuvesSignaturesCrV17.class.getResourceAsStream(RESSOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Ressource absente : " + RESSOURCE);
            }
            return JsonTexte.lire(new String(in.readAllBytes(), StandardCharsets.UTF_8), Fichier.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Libellé de la version publiée par la reprise ; clé d'idempotence. */
    public static String libelleVersion() {
        return lire().libelleVersion();
    }

    /** Entrée d'un rang pour un type de projet (exactement une, contrôlée par les tests). */
    public static Entree entree(Fichier f, TypeProjet type, String rang) {
        List<Entree> l = f.operations().stream().filter(e -> e.rang().equals(rang) && e.vaut(type)).toList();
        if (l.size() != 1) {
            throw new IllegalStateException(l.size() + " entrée(s) de preuves pour le rang " + rang + " du type " + type);
        }
        return l.get(0);
    }

    /** Version fidèle à l'annexe A v1.7, complétée des preuves, livrables, signatures, RACI et délais (D-109). */
    public static List<ConversionAnnexeA.Operation> version(TypeProjet type) {
        Fichier f = lire();
        List<ConversionAnnexeA.Operation> l = new ArrayList<>();
        for (ConversionAnnexeA.Operation o : ConversionAnnexeA.version(type, ConversionAnnexeA.Edition.V1_7)) {
            l.add(new ConversionAnnexeA.Operation(o.code(), completer(o.data(), entree(f, type, o.code()))));
        }
        return l;
    }

    /** Saisie complétée d'une entrée. */
    static ReferenceOperationData completer(ReferenceOperationData d, Entree e) {
        Map<String, Object> preuve = new LinkedHashMap<>();
        if (d.preuveAttendue() != null) {
            preuve.putAll(d.preuveAttendue());
        }
        preuve.put("grain", d.grain() == null ? "OPERATION" : d.grain().name());
        if (d.plateforme() != null) {
            preuve.put("plateforme", d.plateforme().name());
        }
        preuve.put("description", e.preuveAttendue());
        preuve.put("livrables", e.livrables().stream().map(Livrable::code).toList());
        preuve.put("source", e.source());
        if (e.note() != null) {
            preuve.put("note", e.note());
        }
        String description = e.preuveAttendue() + (e.note() == null ? "" : " " + e.note());
        return d.avecPreuves(
            preuve,
            description.length() > 4000 ? description.substring(0, 4000) : description,
            e.validateur(),
            fusion(d.consultes(), e.consultes()),
            e.informes().isEmpty() ? null : e.informes(),
            fusion(d.externes(), e.externes()),
            e.delaiJours(),
            e.delaiMaxJours(),
            e.livrables(),
            e.signatures()
        );
    }

    private static List<Fonction> fusion(List<Fonction> a, List<Fonction> b) {
        LinkedHashSet<Fonction> s = new LinkedHashSet<>(a == null ? List.of() : a);
        s.addAll(b);
        return List.copyOf(s);
    }
}
