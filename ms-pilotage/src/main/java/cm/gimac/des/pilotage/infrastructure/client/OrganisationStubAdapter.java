package cm.gimac.des.pilotage.infrastructure.client;

import cm.gimac.des.pilotage.application.port.out.OrganisationPort;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution de l'organisation (développement, tests) : une petite hiérarchie du GIMAC
 * (DG, chef du DES, chef de service et ses agents, chef de la DDRM), un compte sans responsable, un
 * compte désactivé et un correspondant d'institution. Les tests peuvent en enregistrer d'autres.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.organisation", havingValue = "stub", matchIfMissing = true)
public class OrganisationStubAdapter implements OrganisationPort {

    public static final String DG = "00000000-0000-4000-8000-0000000d0001";
    public static final String CHEF_DES = "00000000-0000-4000-8000-0000000d0002";
    public static final String CHEF_SERVICE = "00000000-0000-4000-8000-0000000d0003";
    public static final String AGENT = "00000000-0000-4000-8000-0000000d0004";
    public static final String AGENT_BIS = "00000000-0000-4000-8000-0000000d0005";
    public static final String CHEF_DDRM = "00000000-0000-4000-8000-0000000d0006";
    public static final String SANS_RESPONSABLE = "00000000-0000-4000-8000-0000000d0007";
    public static final String INACTIF = "00000000-0000-4000-8000-0000000d0008";
    public static final String CORRESPONDANT = "00000000-0000-4000-8000-0000000d0009";

    /** Compte de l'organisation simulée. */
    public record Compte(String id, String nomAffiche, String fonction, String unite, String responsableId, boolean actif, boolean interne) {}

    /** Profondeur de la chaîne des responsables (§1.3) et taille de l'équipe rendue. */
    static final int CHAINE_MAX = 10;
    static final int EQUIPE_MAX = 500;

    private final Map<String, Compte> comptes = new ConcurrentHashMap<>();

    public OrganisationStubAdapter() {
        reinitialiser();
    }

    /** Jeu initial ; les comptes ajoutés par un test sont retirés. */
    public final void reinitialiser() {
        comptes.clear();
        enregistrer(new Compte(DG, "Directeur Général", "Directeur Général", "DG", null, true, true));
        enregistrer(new Compte(CHEF_DES, "Paul Mbarga Atangana", "Chef du DES", "DES", DG, true, true));
        enregistrer(new Compte(CHEF_SERVICE, "Aminata Ngono", "Cheffe du service Normes et Agréments", "SNA", CHEF_DES, true, true));
        enregistrer(new Compte(AGENT, "Serge Ndzana", "Ingénieur d'homologation", "SNA", CHEF_SERVICE, true, true));
        enregistrer(new Compte(AGENT_BIS, "Clarisse Bilong", "Chargée d'études", "SNA", CHEF_SERVICE, true, true));
        enregistrer(new Compte(CHEF_DDRM, "Hervé Tchoua", "Chef de la DDRM", "DDRM", DG, true, true));
        enregistrer(new Compte(SANS_RESPONSABLE, "Jean Essomba", "Chargé de mission", null, null, true, true));
        enregistrer(new Compte(INACTIF, "Compte Désactivé", "Ancien agent", "SNA", CHEF_SERVICE, false, true));
        enregistrer(new Compte(CORRESPONDANT, "Jeanne Owona", "Correspondante", null, null, true, false));
    }

    public void enregistrer(Compte c) {
        comptes.put(c.id(), c);
    }

    @Override
    public Optional<Rattachement> rattachement(String compteId) {
        Compte c = comptes.get(compteId);
        if (c == null || !c.interne()) {
            return Optional.empty();
        }
        Compte resp = c.responsableId() == null ? null : comptes.get(c.responsableId());
        List<String> chaine = new ArrayList<>();
        for (Compte r = resp; r != null && chaine.size() < CHAINE_MAX && !chaine.contains(r.id()); r = r.responsableId() == null ? null : comptes.get(r.responsableId())) {
            chaine.add(r.id());
        }
        Personne responsable = resp == null ? null : personne(resp);
        return Optional.of(new Rattachement(c.id(), c.nomAffiche(), c.unite(), responsable, List.copyOf(chaine)));
    }

    @Override
    public List<Membre> equipe(String compteId) {
        List<Membre> equipe = new ArrayList<>();
        Deque<Map.Entry<String, Integer>> file = new ArrayDeque<>();
        file.add(Map.entry(compteId, 0));
        while (!file.isEmpty() && equipe.size() < EQUIPE_MAX) {
            Map.Entry<String, Integer> e = file.poll();
            int niveau = e.getValue() + 1;
            if (niveau > CHAINE_MAX) {
                continue;
            }
            comptes
                .values()
                .stream()
                .filter(c -> e.getKey().equals(c.responsableId()) && !c.id().equals(compteId))
                .sorted(java.util.Comparator.comparing(Compte::nomAffiche))
                .forEach(c -> {
                    if (equipe.stream().noneMatch(m -> m.id().equals(c.id())) && equipe.size() < EQUIPE_MAX) {
                        equipe.add(new Membre(c.id(), c.nomAffiche(), c.fonction(), c.unite(), c.responsableId(), niveau, c.actif()));
                        file.add(Map.entry(c.id(), niveau));
                    }
                });
        }
        return List.copyOf(equipe);
    }

    @Override
    public Optional<Personne> collaborateur(String compteId) {
        return Optional.ofNullable(compteId == null ? null : comptes.get(compteId)).map(OrganisationStubAdapter::personne);
    }

    private static Personne personne(Compte c) {
        return new Personne(c.id(), c.nomAffiche(), c.fonction(), c.actif(), c.interne());
    }
}
