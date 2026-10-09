package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.CatalogueUnites;
import cm.gimac.des.iam.application.port.out.RenduPdf;
import cm.gimac.des.iam.application.usecase.organigramme.DocumentOrganigramme;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.domain.service.ReglesOrganisation;
import java.text.Collator;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Organigramme en PDF (D-102, docs/lot1/organigramme.md §4) : document composé par MS-IAM à partir de ses seules données
 * (unités actives, personnes internes actives, libellés des profils, photos des chefs), jamais d'un HTML fourni par le
 * client, puis imprimé par le moteur de MS-DOCUMENT. Toute l'organisation ou une branche ({@code racine}), A4 ou A3,
 * paysage ou portrait, annexe des membres en option. Date d'édition dans le fuseau Africa/Douala.
 */
@Service
@Transactional(readOnly = true)
public class OrganigrammePdfService {

    /** Fuseau de la date d'édition et du nom de fichier. */
    public static final ZoneId DOUALA = ZoneId.of("Africa/Douala");

    /** Demande : branche (nulle pour toute l'organisation), papier, annexe des membres. */
    public record Demande(String racine, RenduPdf.Format format, boolean paysage, boolean membres) {}

    /** PDF produit et nom de fichier proposé. */
    public record Fichier(String nom, byte[] contenu) {}

    private final CatalogueUnites catalogue;
    private final Annuaire annuaire;
    private final AvatarService avatars;
    private final RenduPdf rendu;
    private final Clock horloge;

    public OrganigrammePdfService(CatalogueUnites catalogue, Annuaire annuaire, AvatarService avatars, RenduPdf rendu, Clock horloge) {
        this.catalogue = catalogue;
        this.annuaire = annuaire;
        this.avatars = avatars;
        this.rendu = rendu;
        this.horloge = horloge;
    }

    /** Lit les paramètres de la route : format A4 ou A3, orientation paysage ou portrait (400 ORG_PDF_PARAMETRE sinon). */
    public static Demande demande(String racine, String format, String orientation, boolean membres) {
        RenduPdf.Format f;
        try {
            f = RenduPdf.Format.valueOf(format == null || format.isBlank() ? "A3" : format.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw IamException.invalide("ORG_PDF_PARAMETRE", "Format de papier A4 ou A3", Map.of("champ", "format"));
        }
        String o = orientation == null || orientation.isBlank() ? "paysage" : orientation.trim().toLowerCase(Locale.ROOT);
        if (!o.equals("paysage") && !o.equals("portrait")) {
            throw IamException.invalide("ORG_PDF_PARAMETRE", "Orientation paysage ou portrait", Map.of("champ", "orientation"));
        }
        return new Demande(OrganisationService.vide(racine), f, o.equals("paysage"), membres);
    }

    public Fichier produire(Demande d) {
        Map<String, Unite> parCode = new LinkedHashMap<>();
        catalogue.toutes().forEach(u -> parCode.put(u.code(), u));
        List<Unite> unites;
        if (d.racine() == null) {
            unites = parCode.values().stream().filter(Unite::active).toList();
        } else {
            if (!parCode.containsKey(d.racine())) {
                throw IamException.introuvable("ORG_UNITE_INTROUVABLE", d.racine());
            }
            Set<String> branche = new HashSet<>(ReglesOrganisation.sousUnites(d.racine(), parCode.values()));
            unites = parCode.values().stream().filter(u -> branche.contains(u.code()) && (u.active() || u.code().equals(d.racine()))).toList();
        }

        Map<String, Set<String>> profils = annuaire.profilsParCompte();
        Map<String, String> libelles = new LinkedHashMap<>();
        annuaire.presentationsProfils().forEach((code, p) -> libelles.put(code, p.libelle() == null || p.libelle().isBlank() ? code : p.libelle()));
        Collator ordre = Collator.getInstance(Locale.FRENCH);
        Map<String, DocumentOrganigramme.Personne> personnes = new LinkedHashMap<>();
        for (Compte c : annuaire.lister()) {
            if (c.actif() && c.interne()) {
                List<String> siens = new ArrayList<>(profils.getOrDefault(c.id(), Set.of()).stream().map(p -> libelles.getOrDefault(p, p)).toList());
                siens.sort(ordre);
                personnes.put(c.id(), new DocumentOrganigramme.Personne(c.id(), c.nomAffiche(), c.fonction(), c.unite(), c.responsableId(), List.copyOf(siens)));
            }
        }
        Map<String, String> photos = new LinkedHashMap<>();
        unites.stream().map(Unite::responsableId).filter(id -> id != null && personnes.containsKey(id)).distinct().forEach(id ->
            avatars.lire(id).ifPresent(a -> photos.put(id, "data:" + a.type() + ";base64," + Base64.getEncoder().encodeToString(a.octets())))
        );

        ZonedDateTime edition = ZonedDateTime.now(horloge).withZoneSameInstant(DOUALA);
        RenduPdf.Mise mise = new RenduPdf.Mise(d.format(), d.paysage());
        DocumentOrganigramme.Rendu r = DocumentOrganigramme.composer(
            new DocumentOrganigramme.Contenu(unites.stream().sorted(Comparator.comparing(Unite::code)).toList(), d.racine(), personnes, photos, edition, mise, d.membres())
        );
        byte[] pdf = rendu.rendre(r.html(), r.piedDePage(), mise);
        return new Fichier("organigramme-moabi-" + DateTimeFormatter.ISO_LOCAL_DATE.format(edition) + ".pdf", pdf);
    }
}
