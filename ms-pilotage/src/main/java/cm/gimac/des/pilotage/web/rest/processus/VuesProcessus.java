package cm.gimac.des.pilotage.web.rest.processus;

import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase.DroitsInstance;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase.InstanceLue;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase.MoiEtape;
import cm.gimac.des.pilotage.application.port.in.Page;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.Droits;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.ProcessusLu;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.VersionResume;
import cm.gimac.des.pilotage.domain.activite.EtatTache;
import cm.gimac.des.pilotage.domain.activite.Tache;
import cm.gimac.des.pilotage.domain.processus.Affectation;
import cm.gimac.des.pilotage.domain.processus.Anomalie;
import cm.gimac.des.pilotage.domain.processus.Champ;
import cm.gimac.des.pilotage.domain.processus.Depot;
import cm.gimac.des.pilotage.domain.processus.DocumentSigne;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.EtapeInstance;
import cm.gimac.des.pilotage.domain.processus.EtatEtape;
import cm.gimac.des.pilotage.domain.processus.HistoriqueProcessus;
import cm.gimac.des.pilotage.domain.processus.InstanceProcessus;
import cm.gimac.des.pilotage.domain.processus.Position;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.SignatureInstance;
import cm.gimac.des.pilotage.domain.processus.TypeChamp;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/** Représentations JSON des processus d'unité (docs/lot1/processus-unite.md §1 à §3). */
final class VuesProcessus {

    private VuesProcessus() {}

    record PersonneVue(String id, String nomAffiche) {
        static PersonneVue de(String id, Map<String, String> noms) {
            return id == null ? null : new PersonneVue(id, noms.get(id));
        }
    }

    record PageVue<T>(List<T> contenu, long total, int page, int taille) {
        static <S, T> PageVue<T> de(Page<S> p, Function<S, T> vue) {
            return new PageVue<>(p.contenu().stream().map(vue).toList(), p.total(), p.page(), p.taille());
        }
    }

    record HistoriqueVue(String quand, PersonneVue acteur, String action, String etape, String detail) {
        static HistoriqueVue de(HistoriqueProcessus h, Map<String, String> noms) {
            return new HistoriqueVue(ReglesActivite.horodatage(h.getQuand()), PersonneVue.de(h.getActeurId(), noms), h.getAction().name(), h.getEtape(), h.getDetail());
        }
    }

    // ------------------------------------------------------------------ définitions (§1)

    record ProcessusVue(
        UUID id,
        String code,
        int version,
        String etat,
        String unite,
        String libelle,
        String description,
        String categorie,
        Integer delaiCibleJours,
        List<Etape> etapes,
        String creeLe,
        PersonneVue creePar,
        String modifieLe,
        PersonneVue modifiePar,
        String publieeLe,
        PersonneVue publieePar,
        String motifPublication,
        boolean avertissementsLeves,
        String retireeLe,
        PersonneVue retireePar,
        String motifRetrait,
        Droits droits,
        List<VersionResume> versions,
        List<Anomalie> avertissements,
        Map<String, String> personnes,
        List<HistoriqueVue> historique
    ) {
        static ProcessusVue de(ProcessusLu l) {
            Processus p = l.processus();
            Map<String, String> noms = l.noms();
            Map<String, String> personnes = new LinkedHashMap<>();
            p
                .getEtapes()
                .forEach(e ->
                    java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(e.intervenant(), e.validateur()),
                        e.signatures().stream().map(s -> s.signataire())
                    )
                        .filter(Objects::nonNull)
                        .map(i -> i.personneId())
                        .filter(Objects::nonNull)
                        .forEach(id -> personnes.put(id, noms.get(id)))
                );
            return new ProcessusVue(
                p.getId(),
                p.getCode(),
                p.getVersion(),
                p.getEtat().name(),
                p.getUnite(),
                p.getLibelle(),
                p.getDescription(),
                p.getCategorie(),
                p.getDelaiCibleJours(),
                p.getEtapes(),
                ReglesActivite.horodatage(p.getCreeLe()),
                PersonneVue.de(p.getCreePar(), noms),
                ReglesActivite.horodatage(p.getModifieLe()),
                PersonneVue.de(p.getModifiePar(), noms),
                ReglesActivite.horodatage(p.getPublieeLe()),
                PersonneVue.de(p.getPublieePar(), noms),
                p.getMotifPublication(),
                p.isAvertissementsLeves(),
                ReglesActivite.horodatage(p.getRetireeLe()),
                PersonneVue.de(p.getRetireePar(), noms),
                p.getMotifRetrait(),
                l.droits(),
                l.versions(),
                l.avertissements(),
                personnes,
                l.historique().stream().map(h -> HistoriqueVue.de(h, noms)).toList()
            );
        }
    }

    record ProcessusResumeVue(
        UUID id,
        String code,
        int version,
        String etat,
        String unite,
        String libelle,
        String description,
        String categorie,
        Integer delaiCibleJours,
        int nombreEtapes,
        String publieeLe,
        String modifieLe,
        PersonneVue modifiePar,
        Droits droits
    ) {
        static ProcessusResumeVue de(ProcessusLu l) {
            Processus p = l.processus();
            return new ProcessusResumeVue(
                p.getId(),
                p.getCode(),
                p.getVersion(),
                p.getEtat().name(),
                p.getUnite(),
                p.getLibelle(),
                p.getDescription(),
                p.getCategorie(),
                p.getDelaiCibleJours(),
                p.getEtapes().size(),
                ReglesActivite.horodatage(p.getPublieeLe()),
                ReglesActivite.horodatage(p.getModifieLe()),
                PersonneVue.de(p.getModifiePar(), l.noms()),
                l.droits()
            );
        }
    }

    // ------------------------------------------------------------------ instances (§2)

    record ProcessusRef(UUID id, String code, int version, String libelle) {
        static ProcessusRef de(InstanceProcessus i) {
            return new ProcessusRef(i.getProcessusId(), i.getCode(), i.getVersionProcessus(), i.getLibelle());
        }
    }

    record Avancement(int total, int terminees) {
        static Avancement de(List<EtapeInstance> l) {
            return new Avancement(l.size(), (int) l.stream().filter(e -> e.getEtat() == EtatEtape.TERMINEE).count());
        }
    }

    record AffectationVue(PersonneVue personne, String profil, String unite) {
        static AffectationVue de(Affectation a, Map<String, String> noms) {
            return a == null ? null : new AffectationVue(PersonneVue.de(a.personneId(), noms), a.profil(), a.unite());
        }
    }

    record APrendreVue(String unite, String profil) {}

    record DepotVue(String documentId, String nomFichier, String typeMime, long taille, String sha256, PersonneVue deposePar, String deposeLe) {
        static DepotVue de(Depot d, Map<String, String> noms) {
            return d == null ? null : new DepotVue(d.documentId(), d.nomFichier(), d.typeMime(), d.taille(), d.sha256(), PersonneVue.de(d.deposePar(), noms), d.deposeLe());
        }
    }

    record LivrableVue(String code, String libelle, boolean obligatoire, List<String> typesPieces, String modele, DepotVue depot) {}

    record ChampVue(String code, String libelle, String type, List<String> choix, boolean obligatoire, Object valeur) {
        static ChampVue de(Champ c, String valeur) {
            Object v = valeur;
            if (valeur != null && c.type() == TypeChamp.NOMBRE) {
                try {
                    v = new BigDecimal(valeur);
                } catch (NumberFormatException e) {
                    v = valeur;
                }
            }
            return new ChampVue(c.code(), c.libelle(), c.type().name(), c.choix(), c.obligatoire(), v);
        }
    }

    record SignatureVue(
        String code,
        String libelle,
        int ordre,
        List<String> porte,
        AffectationVue attendu,
        String etat,
        PersonneVue signePar,
        String signeLe,
        String empreinte,
        List<DocumentSigne> documents
    ) {
        static SignatureVue de(SignatureInstance s, Map<String, String> noms) {
            return new SignatureVue(
                s.code(),
                s.libelle(),
                s.ordre(),
                s.porte(),
                AffectationVue.de(s.attendu(), noms),
                s.etat().name(),
                PersonneVue.de(s.signePar(), noms),
                s.signeLe(),
                s.empreinte(),
                s.documents()
            );
        }
    }

    record EtapeInstanceVue(
        String code,
        String libelle,
        String action,
        List<String> predecesseurs,
        Position position,
        int delaiJours,
        String etat,
        String activeeLe,
        String echeance,
        String soumiseLe,
        String termineeLe,
        boolean enRetard,
        int renvois,
        UUID tacheId,
        PersonneVue executant,
        APrendreVue aPrendre,
        AffectationVue validateur,
        List<LivrableVue> livrables,
        List<ChampVue> champs,
        List<SignatureVue> signatures,
        MoiEtape moi
    ) {}

    record InstanceVue(
        UUID id,
        ProcessusRef processus,
        String unite,
        String intitule,
        String etat,
        PersonneVue lanceur,
        String lanceeLe,
        String closeLe,
        String motif,
        Map<String, PersonneVue> designations,
        Avancement avancement,
        List<EtapeInstanceVue> etapes,
        List<HistoriqueVue> historique,
        DroitsInstance droits
    ) {
        static InstanceVue de(InstanceLue l) {
            InstanceProcessus i = l.instance();
            Map<String, String> noms = l.noms();
            Map<String, PersonneVue> designations = new LinkedHashMap<>();
            i.getDesignations().forEach((k, v) -> designations.put(k, PersonneVue.de(v, noms)));
            return new InstanceVue(
                i.getId(),
                ProcessusRef.de(i),
                i.getUnite(),
                i.getIntitule(),
                i.getEtat().name(),
                PersonneVue.de(i.getLanceurId(), noms),
                ReglesActivite.horodatage(i.getLanceeLe()),
                ReglesActivite.horodatage(i.getCloseLe()),
                i.getMotif(),
                designations,
                Avancement.de(l.etapes()),
                l.etapes().stream().map(e -> etape(l, e)).toList(),
                l.historique().stream().map(h -> HistoriqueVue.de(h, noms)).toList(),
                l.droits()
            );
        }

        private static EtapeInstanceVue etape(InstanceLue l, EtapeInstance e) {
            Map<String, String> noms = l.noms();
            Etape def = l.processus() == null ? null : l.processus().etape(e.getCode()).orElse(null);
            Tache t = e.getTacheId() == null ? null : l.taches().get(e.getTacheId());
            boolean aPrendre = t != null && t.getEtat() == EtatTache.A_PRENDRE;
            boolean enRetard = e.getEcheance() != null && e.getEtat().enCours() && e.getEcheance().isBefore(l.aujourdhui());
            List<LivrableVue> livrables = def == null
                ? List.of()
                : def.livrables().stream().map(x -> new LivrableVue(x.code(), x.libelle(), x.obligatoire(), x.typesPieces(), x.modele(), DepotVue.de(e.depot(x.code()), noms))).toList();
            List<ChampVue> champs = def == null ? List.of() : def.champs().stream().map(c -> ChampVue.de(c, e.getValeurs().get(c.code()))).toList();
            return new EtapeInstanceVue(
                e.getCode(),
                def == null ? e.getCode() : def.libelle(),
                def == null ? null : def.action(),
                def == null ? List.of() : def.predecesseurs(),
                def == null ? null : def.position(),
                def == null ? 0 : def.delaiJours(),
                e.getEtat().name(),
                horodatage(e.getActiveeLe()),
                e.getEcheance() == null ? null : e.getEcheance().toString(),
                horodatage(e.getSoumiseLe()),
                horodatage(e.getTermineeLe()),
                enRetard,
                e.getRenvois(),
                e.getTacheId(),
                t == null || aPrendre ? null : PersonneVue.de(t.getResponsableId(), noms),
                aPrendre ? new APrendreVue(t.getUniteCible(), t.getProfilRequis()) : null,
                AffectationVue.de(e.getValidateur(), noms),
                livrables,
                champs,
                e.getSignatures().stream().map(s -> SignatureVue.de(s, noms)).toList(),
                l.moi().get(e.getCode())
            );
        }
    }

    record EtapeResumeVue(String code, String libelle, String etat, String echeance) {}

    record InstanceResumeVue(
        UUID id,
        ProcessusRef processus,
        String unite,
        String intitule,
        String etat,
        PersonneVue lanceur,
        String lanceeLe,
        String closeLe,
        Avancement avancement,
        List<EtapeResumeVue> etapesEnCours
    ) {
        static InstanceResumeVue de(InstanceLue l) {
            InstanceProcessus i = l.instance();
            return new InstanceResumeVue(
                i.getId(),
                ProcessusRef.de(i),
                i.getUnite(),
                i.getIntitule(),
                i.getEtat().name(),
                PersonneVue.de(i.getLanceurId(), l.noms()),
                ReglesActivite.horodatage(i.getLanceeLe()),
                ReglesActivite.horodatage(i.getCloseLe()),
                Avancement.de(l.etapes()),
                l
                    .etapes()
                    .stream()
                    .filter(e -> e.getEtat().enCours())
                    .map(e ->
                        new EtapeResumeVue(
                            e.getCode(),
                            l.processus() == null ? e.getCode() : l.processus().etape(e.getCode()).map(Etape::libelle).orElse(e.getCode()),
                            e.getEtat().name(),
                            e.getEcheance() == null ? null : e.getEcheance().toString()
                        )
                    )
                    .toList()
            );
        }
    }

    // ------------------------------------------------------------------ requêtes

    record PublicationRequete(String motif, Boolean leverAvertissements) {}

    record MotifRequete(String motif) {}

    record LivrableRequete(String livrable, String documentId) {}

    record ChampsRequete(Map<String, Object> valeurs) {}

    record SignatureRequete(String signature) {}

    record ValidationRequete(String decision, String motif) {}

    record ReaffectationRequete(String personneId, String motif) {}

    record ControleVue(boolean publiable, List<Anomalie> anomalies) {}

    private static String horodatage(Instant i) {
        return ReglesActivite.horodatage(i);
    }
}
