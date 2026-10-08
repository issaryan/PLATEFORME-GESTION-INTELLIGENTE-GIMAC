package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.CatalogueFonctionnalites;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite;
import cm.gimac.des.iam.domain.fonctionnalite.ReglesFonctionnalites;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalogue des fonctionnalités servi et configurable (D-94, docs/lot1/ecrans-par-configuration.md §2) : lecture
 * par tout compte interne (construction de sa navigation), modification par IAM_PROFILE_EDIT avec motif et clé de
 * sécurité, journal CONFIGURATION_FONCTIONNALITE_MODIFIEE (avant, après).
 *
 * <p>Valeurs de départ : registre de l'interface (config/fonctionnalites-initiales.json), chargé au démarrage. Une
 * clé absente est ajoutée ; une valeur de départ jamais modifiée depuis la plateforme suit le registre livré (elle
 * est remplacée, ou retirée si le registre ne la porte plus) ; une modification faite depuis la plateforme n'est
 * jamais écrasée.
 */
@Service
@Transactional
public class FonctionnalitesService {

    private static final Logger LOG = LoggerFactory.getLogger(FonctionnalitesService.class);

    /** Bilan du chargement des valeurs de départ. */
    public record Chargement(int ajoutees, int remplacees, int retirees, int conservees, int ignorees) {}

    private final CatalogueFonctionnalites catalogue;
    private final Annuaire annuaire;
    private final JournalActes journal;
    private final Clock clock;

    public FonctionnalitesService(CatalogueFonctionnalites catalogue, Annuaire annuaire, JournalActes journal, Clock clock) {
        this.catalogue = catalogue;
        this.annuaire = annuaire;
        this.journal = journal;
        this.clock = clock;
    }

    /**
     * Fonctionnalités, triées par application, ordre et clé ; {@code application} (des, console, portail) filtre,
     * absente : toutes ; inconnue : 400 IAM_FONCTIONNALITE_INVALIDE (champ application).
     */
    @Transactional(readOnly = true)
    public List<Fonctionnalite> lister(String application) {
        String a = application == null || application.isBlank() ? null : application.strip().toLowerCase(Locale.ROOT);
        if (a != null && !ReglesFonctionnalites.APPLICATIONS.contains(a)) {
            throw IamException.invalide("IAM_FONCTIONNALITE_INVALIDE", "Application inconnue", Map.of("champ", "application", "application", application));
        }
        return catalogue.toutes().stream().filter(f -> a == null || a.equals(f.application())).toList();
    }

    /**
     * Modifie une fonctionnalité connue : motif obligatoire (400 IAM_REASON_REQUIRED) ; clé inconnue : 404
     * IAM_FONCTIONNALITE_INTROUVABLE ; clé du corps différente du chemin : 400 IAM_FONCTIONNALITE_INVALIDE ;
     * application changée : 422 IAM_FONCTIONNALITE_APPLICATION_IMMUABLE ; permission inconnue du catalogue des
     * permissions : 400 IAM_PERMISSION_UNKNOWN ; permission interne : 422 IAM_FONCTIONNALITE_PERMISSION_INTERNE.
     * Sans changement, rien n'est écrit ni journalisé. Rend la fonctionnalité enregistrée.
     */
    public Fonctionnalite modifier(Acteur acteur, String cle, Fonctionnalite demandee, String motif) {
        ReglesAdministration.exigerMotif(motif);
        Fonctionnalite avant = catalogue.lire(cle).orElseThrow(() -> IamException.introuvable("IAM_FONCTIONNALITE_INTROUVABLE", cle));
        if (demandee.cle() != null && !demandee.cle().equals(cle)) {
            throw IamException.invalide("IAM_FONCTIONNALITE_INVALIDE", "La clé d'une fonctionnalité ne change pas", Map.of("champ", "cle"));
        }
        Fonctionnalite apres = new Fonctionnalite(
            cle,
            demandee.application(),
            demandee.module(),
            demandee.libelle() == null ? null : demandee.libelle().strip(),
            demandee.groupe(),
            demandee.ordre(),
            demandee.icone(),
            demandee.visible(),
            demandee.permissions(),
            demandee.widgets(),
            demandee.droits(),
            null,
            null
        );
        ReglesFonctionnalites.verifierModification(avant, apres, annuaire.permissions());
        List<String> diff = ReglesFonctionnalites.differences(avant.sansTrace(), apres);
        if (diff.get(0).isEmpty() && diff.get(1).isEmpty()) {
            return avant;
        }
        Fonctionnalite enregistree = apres.modifiee(clock.instant(), acteur.id(), acteur.nom());
        catalogue.modifier(enregistree);
        journal.fonctionnalite(acteur, cle, motif.strip(), diff.get(0), diff.get(1));
        return enregistree;
    }

    /**
     * Charge les valeurs de départ (registre de l'interface) : ajoute les clés absentes, met à jour ou retire les
     * valeurs de départ jamais modifiées depuis la plateforme, conserve toute modification faite depuis la
     * plateforme. Une entrée de forme invalide ou en double est ignorée (et tracée). Une instance concurrente qui
     * ajoute la même clé ne fait rien échouer (insertion sans effet en cas de conflit).
     */
    public Chargement charger(List<Fonctionnalite> initiales) {
        int ajoutees = 0;
        int remplacees = 0;
        int retirees = 0;
        int conservees = 0;
        int ignorees = 0;
        Map<String, Fonctionnalite> actuelles = new java.util.HashMap<>();
        catalogue.toutes().forEach(f -> actuelles.put(f.cle(), f));
        Set<String> vues = new HashSet<>();
        for (Fonctionnalite f : initiales) {
            try {
                ReglesFonctionnalites.verifierForme(f);
            } catch (IamException e) {
                LOG.warn("Fonctionnalité de départ ignorée ({}, {})", f.cle(), e.getMessage());
                ignorees++;
                continue;
            }
            if (!vues.add(f.cle())) {
                LOG.warn("Fonctionnalité de départ en double ignorée ({})", f.cle());
                ignorees++;
                continue;
            }
            Fonctionnalite initiale = f.sansTrace();
            Fonctionnalite actuelle = actuelles.get(f.cle());
            if (actuelle == null) {
                if (catalogue.ajouterInitiale(initiale)) {
                    ajoutees++;
                }
            } else if (actuelle.modifieLe() != null) {
                conservees++;
            } else if (!actuelle.equals(initiale) && catalogue.remplacerInitiale(initiale)) {
                remplacees++;
            }
        }
        for (Fonctionnalite a : actuelles.values()) {
            if (!vues.contains(a.cle())) {
                if (a.modifieLe() == null && catalogue.retirerInitiale(a.cle())) {
                    retirees++;
                } else if (a.modifieLe() != null) {
                    conservees++;
                }
            }
        }
        return new Chargement(ajoutees, remplacees, retirees, conservees, ignorees);
    }
}
