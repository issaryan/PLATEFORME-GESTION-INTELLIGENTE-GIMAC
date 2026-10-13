import { describe, expect, it } from 'vitest'
import { servicesSimules } from '.'
import { definitionDemo, empreinteDemo } from './processus'
import { magasinPouvoirs } from './pouvoirs'
import { champsDeLancement, cleDesignation, designationsAttendues, etapeVierge, intervenantNormalise, type EtapeProcessus, type SaisieProcessus } from '../../modele/processus'

// MS-PILOTAGE simulé des processus d'unité (D-87, §5) : contrôle, publication avec levée motivée, versions, retrait,
// lancement et désignations, livrables, champs, signatures dans l'ordre, validation et renvoi, réaffectation,
// suspension, reprise, annulation, et tâches d'origine PROCESSUS de « Mon activité ».
const code = (p: Promise<unknown>) => p.then(() => 'ok', (e: { status: number; code: string }) => `${e.status} ${e.code}`)
const PERSONNES: Record<string, string> = { 'U-001': 'Paul Mbarga Atangana', 'u-resp-info': 'Gaston Ndzana Ebode', 'u-res': 'Idriss Mahamat Saleh', 'u-collab': 'Nadège Mvondo Assiga', 'u-sna': 'Rachel Biyong Mengue' }
/** Services de plusieurs personnes sur le même magasin des pouvoirs (organisation partagée). */
function monde() {
  const magasin = magasinPouvoirs()
  return Object.assign((id: string) => servicesSimules({ latenceMs: 0, utilisateur: { id, nom: PERSONNES[id] ?? id }, pouvoirs: magasin }), { magasin })
}
const saisie = (etapes: EtapeProcessus[], o: Partial<SaisieProcessus> = {}): SaisieProcessus => ({ code: 'CHANGEMENT_PAREFEU', unite: 'POLE_RESEAU', libelle: 'Changement de règle pare-feu', description: '', categorie: 'Réseau', delaiCibleJours: 5, etapes, ...o })
const fichier = (nom: string, type = 'application/pdf') => new File(['contenu'], nom, { type })

describe('D-87 : modèle', () => {
  it('désignations attendues, champs de lancement, intervenant réduit à son mode, étape vierge', () => {
    const d = definitionDemo()
    expect(designationsAttendues(d).map((x) => x.cle)).toEqual(['MISE_EN_OEUVRE'])
    const e = { ...etapeVierge(1), validateur: { mode: 'AU_LANCEMENT' as const, remplacement: 'ROLE_SNA' }, signatures: [{ code: 'S1', libelle: 'S', signataire: { mode: 'AU_LANCEMENT' as const, remplacement: 'ROLE_SNA' }, porte: [], ordre: 1 }] }
    expect(designationsAttendues([e]).map((x) => x.cle)).toEqual(['ETAPE_1.validateur', 'ETAPE_1.signature.S1'])
    expect(cleDesignation.signature('A', 'B')).toBe('A.signature.B')
    expect(champsDeLancement(d).map((c) => c.code)).toEqual(['SOURCE', 'DESTINATION', 'PORT', 'PROTOCOLE', 'DATE_SOUHAITEE'])
    expect(intervenantNormalise({ mode: 'LANCEUR', profil: 'X', personneId: 'y' })).toEqual({ mode: 'LANCEUR' })
    expect(intervenantNormalise({ mode: 'PERSONNE', personneId: 'y', unite: 'Z' })).toEqual({ mode: 'PERSONNE', personneId: 'y', remplacement: null })
    expect(intervenantNormalise({ mode: 'CHEF_UNITE', unite: '' })).toEqual({ mode: 'CHEF_UNITE', unite: null })
    expect(intervenantNormalise({ mode: 'AU_LANCEMENT', remplacement: 'R', unite: 'U' })).toEqual({ mode: 'AU_LANCEMENT', remplacement: 'R', unite: 'U' })
    expect(empreinteDemo('a')).toHaveLength(64)
    expect(empreinteDemo('a')).toBe(empreinteDemo('a'))
    expect(empreinteDemo('a')).not.toBe(empreinteDemo('b'))
  })
})

describe('D-87 : définitions', () => {
  it('catalogue : visibilité, droits, filtres ; brouillons pour qui définit seulement', async () => {
    const qui = monde()
    const chef = qui('U-001').processus
    const l = await chef.lister({})
    // D-98 : la demande de changement d'adresse IP du pôle réseau figure aussi au catalogue de l'unité.
    expect(l.map((p) => [p.code, p.etat, p.droits.modifier, p.droits.lancer])).toEqual([
      ['ACCES_VPN_PARTICIPANT', 'BROUILLON', true, false], ['CREATION_MACHINE_VIRTUELLE', 'PUBLIEE', false, false], ['CHANGEMENT_ADRESSE_IP', 'PUBLIEE', false, true], ['OUVERTURE_FLUX_RESEAU', 'PUBLIEE', false, true],
    ])
    // D-100 : le chef du DES lit le processus du pôle système réservé à deux personnes, sans pouvoir le lancer.
    expect((await chef.lister({ etat: 'PUBLIEE' })).length).toBe(3)
    expect((await chef.lister({ q: 'vpn' })).map((p) => p.code)).toEqual(['ACCES_VPN_PARTICIPANT'])
    expect(await chef.lister({ unite: 'SNA' })).toEqual([])
    const membre = qui('u-res').processus
    expect((await membre.lister({})).map((p) => p.code)).toEqual(['CHANGEMENT_ADRESSE_IP', 'OUVERTURE_FLUX_RESEAU'])
    // D-106 : refus explicite (403) comme MS-PILOTAGE ; 404 pour une définition inexistante seulement.
    expect(await code(membre.lire(l[0].id))).toBe('403 PIL_PROCESSUS_INTERDIT')
    expect(await code(membre.lire('PRC-INCONNU'))).toBe('404 PIL_PROCESSUS_INTROUVABLE')
    expect(await qui('u-sna').processus.lister({})).toEqual([])
  })

  it('création, modification, contrôle (Kahn, intervenants, signatures, couverture), suppression', async () => {
    const chef = monde()('U-001').processus
    expect(await code(chef.creer(saisie([], { code: 'x' })))).toBe('400 PIL_REQUETE_INVALIDE')
    expect(await code(chef.creer(saisie([], { unite: 'INCONNUE' })))).toBe('422 PIL_PROCESSUS_UNITE_INCONNUE')
    expect(await code(chef.creer(saisie([], { unite: 'DDRM' })))).toBe('403 PIL_PROCESSUS_INTERDIT')
    expect(await code(chef.creer(saisie([], { code: 'OUVERTURE_FLUX_RESEAU' })))).toBe('409 PIL_PROCESSUS_CODE_PRIS')
    const p = await chef.creer(saisie([]))
    expect(p).toMatchObject({ version: 1, etat: 'BROUILLON', droits: { modifier: true, publier: true } })
    expect((await chef.controler(p.id)).anomalies.map((a) => a.code)).toEqual(['AUCUNE_ETAPE'])
    const a: EtapeProcessus = { ...etapeVierge(1), code: 'A', libelle: 'A', predecesseurs: ['C'], intervenant: { mode: 'PROFIL' } }
    const b: EtapeProcessus = {
      ...etapeVierge(2), code: 'B', libelle: 'B', predecesseurs: ['A', 'Z'], intervenant: { mode: 'PERSONNE', personneId: 'u-res' }, validateur: { mode: 'PERSONNE', personneId: 'u-res', remplacement: 'ROLE_POLE_RESEAU' },
      livrables: [{ code: 'L', libelle: 'L', typesPieces: [], obligatoire: true, modele: null }],
      signatures: [{ code: 'S', libelle: 'S', signataire: { mode: 'CHEF_UNITE' }, porte: ['X'], ordre: 1 }, { code: 'S', libelle: 'S', signataire: { mode: 'PROFIL', profil: 'ROLE_INCONNU' }, porte: [], ordre: 1 }],
      champs: [{ code: 'C', libelle: 'C', type: 'CHOIX', choix: [], obligatoire: true }, { code: 'C', libelle: 'C', type: 'TEXTE', choix: [], obligatoire: false }],
    }
    const c: EtapeProcessus = { ...etapeVierge(3), code: 'C', libelle: 'C', predecesseurs: ['B'], livrables: [{ code: 'L', libelle: 'L', typesPieces: [], obligatoire: true, modele: null }] }
    await chef.modifier(p.id, saisie([a, b, c, { ...c }]))
    const codes = (await chef.controler(p.id)).anomalies.map((x) => x.code)
    expect(codes).toEqual(expect.arrayContaining(['CODE_DUPLIQUE', 'PREDECESSEUR_INCONNU', 'AUCUNE_ETAPE_INITIALE', 'INTERVENANT_INCOMPLET', 'REMPLACEMENT_ABSENT', 'VALIDATEUR_IDENTIQUE',
      'LIVRABLE_CODE_DUPLIQUE', 'SIGNATURE_CODE_DUPLIQUE', 'CHAMP_CODE_DUPLIQUE', 'CHAMP_CHOIX_ABSENT', 'SIGNATURE_SANS_LIVRABLE', 'SIGNATURE_PORTEE_INVALIDE', 'COUVERTURE_PROFIL', 'CHEF_ABSENT']))
    expect(await code(chef.publier(p.id, {}))).toBe('422 PIL_PROCESSUS_ANOMALIES')
    // Cycle : toutes les étapes ont un prédécesseur sauf une, dont les suivantes bouclent.
    const d: EtapeProcessus = { ...etapeVierge(4), code: 'D', libelle: 'D' }
    await chef.modifier(p.id, saisie([d, { ...a, intervenant: { mode: 'LANCEUR' }, predecesseurs: ['D', 'C'] }, { ...c, livrables: [], predecesseurs: ['A'] }]))
    expect((await chef.controler(p.id)).anomalies.map((x) => x.code)).toEqual(['CYCLE'])
    expect(await code(chef.modifier(p.id, saisie([{ ...d, code: 'd minuscule' }])))).toBe('400 PIL_REQUETE_INVALIDE')
    expect(await code(chef.modifier(p.id, saisie([{ ...d, delaiJours: 400 }])))).toBe('400 PIL_REQUETE_INVALIDE')
    await chef.supprimer(p.id)
    expect(await code(chef.lire(p.id))).toBe('404 PIL_PROCESSUS_INTROUVABLE')
  })

  it('publication : levée motivée des avertissements, version précédente retirée, nouvelle version, retrait', async () => {
    const chef = monde()('U-001').processus
    const vpn = (await chef.lister({ q: 'vpn' }))[0]
    expect((await chef.controler(vpn.id)).anomalies.map((a) => [a.code, a.gravite])).toEqual([['REMPLACEMENT_ABSENT', 'BLOQUANTE'], ['CHEF_ABSENT', 'LEVABLE']])
    const def = await chef.lire(vpn.id)
    await chef.modifier(vpn.id, saisie(def.etapes.map((e) => (e.code === 'CONFIGURATION' ? { ...e, intervenant: { ...e.intervenant, remplacement: 'ROLE_POLE_RESEAU' } } : e)), { code: def.code }))
    expect(await code(chef.publier(vpn.id, {}))).toBe('422 PIL_PROCESSUS_AVERTISSEMENTS')
    expect(await code(chef.publier(vpn.id, { leverAvertissements: true }))).toBe('400 PIL_REQUETE_INVALIDE')
    const publie = await chef.publier(vpn.id, { leverAvertissements: true, motif: 'Le chef du service valide en attendant la nomination' })
    expect(publie).toMatchObject({ etat: 'PUBLIEE', avertissementsLeves: true, motifPublication: 'Le chef du service valide en attendant la nomination', droits: { retirer: true, lancer: true } })
    expect(await code(chef.modifier(vpn.id, saisie([])))).toBe('409 PIL_PROCESSUS_ETAT')
    expect(await code(chef.publier(vpn.id, {}))).toBe('409 PIL_PROCESSUS_ETAT')
    const v2 = await chef.nouvelleVersion(vpn.id)
    expect(v2).toMatchObject({ version: 2, etat: 'BROUILLON', versions: [{ version: 2 }, { version: 1 }] })
    expect(await code(chef.nouvelleVersion(vpn.id))).toBe('409 PIL_PROCESSUS_BROUILLON_EXISTANT')
    expect(await code(chef.nouvelleVersion(v2.id))).toBe('409 PIL_PROCESSUS_ETAT')
    const v2p = await chef.publier(v2.id, { leverAvertissements: true, motif: 'Version 2 publiée' })
    expect(v2p.versions.map((v) => v.etat)).toEqual(['PUBLIEE', 'RETIREE'])
    expect((await chef.lire(vpn.id)).motifRetrait).toBe('Remplacée par la version 2')
    expect(await code(chef.retirer(v2.id, ' '))).toBe('400 PIL_REQUETE_INVALIDE')
    expect((await chef.retirer(v2.id, 'Accès VPN arrêté')).etat).toBe('RETIREE')
    expect(await code(chef.retirer(v2.id, 'encore'))).toBe('409 PIL_PROCESSUS_ETAT')
    expect(await code(chef.supprimer(v2.id))).toBe('409 PIL_PROCESSUS_ETAT')
    expect(await code(monde()('u-res').processus.creer(saisie([])))).toBe('403 PIL_PROCESSUS_INTERDIT')
  })
})

describe('D-87 : instances', () => {
  it('lancement : désignations exigées et vérifiées, étape initiale active, tâche du lanceur ; vues', async () => {
    const qui = monde()
    qui.magasin.organisation.push({ id: 'u-res-bis', nomAffiche: 'Armel Nkoulou Beti', fonction: 'Technicien réseau', unite: 'POLE_RESEAU', responsableId: 'u-resp-info', actif: true })
    qui.magasin.definirProfils('u-res-bis', ['ROLE_POLE_RESEAU'])
    const membre = qui('u-res')
    const flux = (await membre.processus.lister({ etat: 'PUBLIEE' })).find((p) => p.code === 'OUVERTURE_FLUX_RESEAU')!
    const base = { intitule: 'Flux de test', champs: { SOURCE: '10.0.0.1' }, designations: {} as Record<string, string> }
    // D-99 : le lanceur ne se désigne pas lui-même pour une étape de sa demande.
    expect(await code(membre.processus.lancer(flux.id, { ...base, designations: { MISE_EN_OEUVRE: 'u-res' } }))).toBe('422 PIL_PROCESSUS_SEPARATION_FONCTIONS')
    expect(await code(membre.processus.lancer(flux.id, base))).toBe('422 PIL_PROCESSUS_DESIGNATION_MANQUANTE')
    expect(await code(membre.processus.lancer(flux.id, { ...base, designations: { MISE_EN_OEUVRE: 'inconnu' } }))).toBe('422 PIL_PROCESSUS_DESIGNATION_INVALIDE')
    // Ni porteuse du profil de remplacement, ni dans la portée du lanceur : refusée.
    expect(await code(membre.processus.lancer(flux.id, { ...base, designations: { MISE_EN_OEUVRE: 'u-collab' } }))).toBe('422 PIL_PROCESSUS_DESIGNATION_INVALIDE')
    expect(await code(membre.processus.lancer(flux.id, { ...base, intitule: ' ', designations: { MISE_EN_OEUVRE: 'u-res-bis' } }))).toBe('400 PIL_REQUETE_INVALIDE')
    const i = await membre.processus.lancer(flux.id, { ...base, designations: { MISE_EN_OEUVRE: 'u-res-bis' } })
    expect(i).toMatchObject({ etat: 'EN_COURS', lanceur: { id: 'u-res' }, designations: { MISE_EN_OEUVRE: { nomAffiche: 'Armel Nkoulou Beti' } } })
    const demande = i.etapes[0]
    expect(demande).toMatchObject({ code: 'DEMANDE', etat: 'ACTIVE', executant: { id: 'u-res' }, moi: { executer: true } })
    expect(demande.champs.find((c) => c.code === 'SOURCE')?.valeur).toBe('10.0.0.1')
    const taches = (await membre.pilotage.taches({ vue: 'moi' })).contenu
    expect(taches.find((t) => t.origine.reference === `${i.id}/DEMANDE`)).toMatchObject({ origine: { type: 'PROCESSUS', lien: `/des/processus/instances/${i.id}?etape=DEMANDE` } })
    expect((await membre.processus.instances({ vue: 'lancees' })).contenu[0].id).toBe(i.id)
    expect((await membre.processus.instances({ vue: 'miennes', etat: 'EN_COURS' })).total).toBeGreaterThan(0)
    expect((await qui('U-001').processus.instances({ vue: 'unite', taille: 1, page: 1 })).contenu).toHaveLength(1)
    // QUO-12 : refus explicite, comme MS-PILOTAGE ; « introuvable » pour une instance inexistante seulement.
    expect(await code(qui('u-sna').processus.instance(i.id))).toBe('403 PIL_PROCESSUS_INTERDIT')
    expect(await code(qui('u-sna').processus.instance('INS-INCONNUE'))).toBe('404 PIL_PROCESSUS_INSTANCE_INTROUVABLE')
    expect(await code(qui('u-sna').processus.lancer(flux.id, { ...base, designations: { MISE_EN_OEUVRE: 'u-collab' } }))).toBe('403 PIL_PROCESSUS_NON_INITIATEUR')
  })

  it('étape : champs contrôlés, fin refusée sans champs ni livrables, tâche à prendre par profil, prise, livrable, validation, renvoi', async () => {
    const qui = monde()
    // Deux porteurs du profil au pôle : l'analyse est à prendre (un seul porteur la recevrait directement).
    qui.magasin.organisation.push({ id: 'u-res-bis', nomAffiche: 'Armel Nkoulou Beti', fonction: 'Technicien réseau', unite: 'POLE_RESEAU', responsableId: 'u-resp-info', actif: true })
    qui.magasin.definirProfils('u-res-bis', ['ROLE_POLE_RESEAU'])
    const membre = qui('u-res')
    // D-99 : ce scénario du moteur fait agir le lanceur sur les étapes de sa demande ; la définition l'autorise.
    const chefP = qui('u-resp-info').processus
    const brouillon = await chefP.creer(saisie(definitionDemo(), { code: 'FLUX_MOTEUR', initiateurs: { regles: ['MEMBRES_UNITE'], unites: [], profils: [], permissions: [], initiateurIntervenant: true } }))
    const flux = await chefP.publier(brouillon.id, { leverAvertissements: true, motif: 'Essai du moteur' })
    const lanceur = membre
    let i = await lanceur.processus.lancer(flux.id, { intitule: 'Flux B', champs: {}, designations: { MISE_EN_OEUVRE: 'u-res-bis' } })
    expect(await code(lanceur.processus.terminer(i.id, 'DEMANDE'))).toBe('422 PIL_PROCESSUS_CHAMPS_MANQUANTS')
    expect(await code(lanceur.processus.saisirChamps(i.id, 'DEMANDE', { PORT: 'abc' }))).toBe('422 PIL_PROCESSUS_CHAMP_INVALIDE')
    expect(await code(lanceur.processus.saisirChamps(i.id, 'DEMANDE', { PROTOCOLE: 'ICMP' }))).toBe('422 PIL_PROCESSUS_CHAMP_INVALIDE')
    expect(await code(lanceur.processus.saisirChamps(i.id, 'DEMANDE', { DATE_SOUHAITEE: '12/10/2026' }))).toBe('422 PIL_PROCESSUS_CHAMP_INVALIDE')
    i = await lanceur.processus.saisirChamps(i.id, 'DEMANDE', { SOURCE: '10.0.0.1', DESTINATION: '10.0.0.2', PORT: '22', PROTOCOLE: 'TCP', DATE_SOUHAITEE: null })
    expect(i.etapes[0].champs.find((c) => c.code === 'PORT')?.valeur).toBe(22)
    expect(await code(qui('u-collab').processus.saisirChamps(i.id, 'DEMANDE', {}))).toBe('403 PIL_PROCESSUS_NON_INTERVENANT')
    i = await lanceur.processus.terminer(i.id, 'DEMANDE')
    expect(i.etapes.map((e) => e.etat)).toEqual(['TERMINEE', 'ACTIVE', 'EN_ATTENTE', 'EN_ATTENTE'])
    expect(await code(lanceur.processus.terminer(i.id, 'DEMANDE'))).toBe('409 PIL_PROCESSUS_ETAPE_ETAT')
    // ANALYSE : à prendre par les porteurs de ROLE_POLE_RESEAU ; la prise de la tâche désigne l'exécutant.
    const analyse = i.etapes[1]
    expect(analyse).toMatchObject({ executant: null, aPrendre: { profil: 'ROLE_POLE_RESEAU' }, moi: { prendre: true } })
    expect((await qui('u-collab').processus.instance(i.id)).etapes[1].moi.prendre).toBe(false)
    // D-99 : l'offre d'une étape n'est jamais listée à l'initiateur ; autorisé par la définition, il la prend depuis l'instance.
    expect((await membre.pilotage.taches({ vue: 'unite' })).contenu.some((t) => t.id === analyse.tacheId)).toBe(false)
    const tache = await membre.pilotage.tache(analyse.tacheId!)
    expect(tache).toMatchObject({ etat: 'A_PRENDRE', profilRequis: 'ROLE_POLE_RESEAU' })
    expect(await code(qui('u-collab').pilotage.prendre(tache.id))).toBe('404 PIL_TACHE_INTROUVABLE')
    await membre.pilotage.prendre(tache.id)
    i = await membre.processus.instance(i.id)
    expect(i.etapes[1]).toMatchObject({ executant: { id: 'u-res' }, moi: { executer: true } })
    expect(i.historique.at(-1)?.action).toBe('ETAPE_PRISE')
    expect(await code(membre.pilotage.transition(tache.id, 'TERMINEE'))).toBe('409 PIL_TRANSITION_INTERDITE')
    expect(await code(membre.processus.terminer(i.id, 'ANALYSE'))).toBe('422 PIL_PROCESSUS_LIVRABLES_MANQUANTS')
    const texte = await membre.processus.televerserLivrable(fichier('note.txt', 'text/plain'), i.id)
    expect(await code(membre.processus.deposerLivrable(i.id, 'ANALYSE', 'NOTE_ANALYSE', texte.documentId))).toBe('422 PIL_PROCESSUS_TYPE_PIECE')
    expect(await code(membre.processus.deposerLivrable(i.id, 'ANALYSE', 'NOTE_ANALYSE', 'DOC-INCONNU'))).toBe('422 PIL_PROCESSUS_DOCUMENT_INVALIDE')
    expect(await code(membre.processus.deposerLivrable(i.id, 'ANALYSE', 'INCONNU', texte.documentId))).toBe('404 PIL_PROCESSUS_LIVRABLE_INCONNU')
    const progression: string[] = []
    const pdf = await membre.processus.televerserLivrable(fichier('note.pdf'), i.id, { surProgression: (p) => progression.push(p.phase) })
    expect(progression).toEqual(expect.arrayContaining(['envoi', 'verification', 'pret']))
    i = await membre.processus.deposerLivrable(i.id, 'ANALYSE', 'NOTE_ANALYSE', pdf.documentId)
    expect(i.etapes[1].livrables[0].depot).toMatchObject({ nomFichier: 'note.pdf', sha256: pdf.sha256 })
    i = await membre.processus.terminer(i.id, 'ANALYSE')
    expect(i.etapes[1].etat).toBe('A_VALIDER')
    // Validation par le chef d'unité (responsable de l'unité parente) ; le renvoi exige un motif.
    const chef = qui('u-resp-info').processus
    expect((await chef.instance(i.id)).etapes[1].moi.valider).toBe(true)
    expect(await code(membre.processus.valider(i.id, 'ANALYSE', 'VALIDER'))).toBe('403 PIL_PROCESSUS_NON_VALIDATEUR')
    expect(await code(chef.valider(i.id, 'ANALYSE', 'RENVOYER'))).toBe('400 PIL_REQUETE_INVALIDE')
    i = await chef.valider(i.id, 'ANALYSE', 'RENVOYER', 'Préciser la règle de retour')
    expect(i.etapes[1]).toMatchObject({ etat: 'ACTIVE', renvois: 1 })
    await membre.processus.terminer(i.id, 'ANALYSE')
    i = await chef.valider(i.id, 'ANALYSE', 'VALIDER')
    expect(i.etapes.map((e) => e.etat)).toEqual(['TERMINEE', 'TERMINEE', 'ACTIVE', 'EN_ATTENTE'])
    expect(i.etapes[2].executant?.id).toBe('u-res-bis')
    // Réaffectation par le chef, refusée au membre.
    expect(await code(membre.processus.reaffecter(i.id, 'MISE_EN_OEUVRE', 'u-res', 'Absence'))).toBe('403 PIL_PROCESSUS_INTERDIT')
    expect(await code(chef.reaffecter(i.id, 'MISE_EN_OEUVRE', 'inconnu', 'Absence'))).toBe('422 PIL_PROCESSUS_PERSONNE_INVALIDE')
    expect(await code(chef.reaffecter(i.id, 'DEMANDE', 'u-res', 'Absence'))).toBe('409 PIL_PROCESSUS_ETAPE_ETAT')
    i = await chef.reaffecter(i.id, 'MISE_EN_OEUVRE', 'u-res', 'Absence d’Armel')
    expect(i.etapes[2].executant?.id).toBe('u-res')
    expect((await membre.pilotage.taches({ vue: 'moi' })).contenu.some((t) => t.id === i.etapes[2].tacheId)).toBe(true)
  })

  it('désignation hors de l’unité : contrôle informatif, un porteur ailleurs confié, plusieurs offerts, aucun au chef ; candidats', async () => {
    const qui = monde()
    const chef = qui('U-001')
    const etape = (profil: string): EtapeProcessus => ({ ...etapeVierge(1), code: 'TRAITEMENT', libelle: 'Traitement', intervenant: { mode: 'PROFIL', profil } })
    // ROLE_SNA : porté par une seule personne, au service SNA, hors du pôle réseau.
    const p = await chef.processus.creer(saisie([etape('ROLE_SNA')], { code: 'AVIS_SNA' }))
    const c = await chef.processus.controler(p.id)
    expect(c.publiable).toBe(true)
    expect(c.anomalies.map((a) => [a.code, a.gravite])).toEqual([['COUVERTURE_HORS_UNITE', 'AVERTISSEMENT']])
    expect(c.anomalies[0].message).toContain('SNA : 1')
    const publie = await chef.processus.publier(p.id, {})
    expect(publie.avertissements.map((a) => a.code)).toEqual(['COUVERTURE_HORS_UNITE'])
    expect(await chef.processus.candidats({ unite: 'POLE_RESEAU', profil: 'ROLE_SNA' })).toMatchObject({ dansUnite: [], ailleurs: [{ id: 'u-sna', unite: 'SNA' }] })
    expect((await chef.processus.candidats({ unite: 'POLE_RESEAU' })).dansUnite.map((x) => x.id)).toEqual(expect.arrayContaining(['u-res', 'u-collab']))
    expect(await code(chef.processus.candidats({ unite: 'FANTOME', profil: 'ROLE_SNA' }))).toBe('422 PIL_PROCESSUS_UNITE_INCONNUE')
    let i = await chef.processus.lancer(p.id, { intitule: 'Avis 1', champs: {}, designations: {} })
    expect(i.etapes[0]).toMatchObject({ etat: 'ACTIVE', executant: { id: 'u-sna' }, aPrendre: null })
    // Deux porteurs ailleurs : étape offerte sans unité, prise par l'un d'eux ; le lanceur peut aussi choisir.
    qui.magasin.definirProfils('U-101', ['ROLE_INGENIEUR_HOMOLOGATION', 'ROLE_SNA'])
    i = await chef.processus.lancer(p.id, { intitule: 'Avis 2', champs: {}, designations: {} })
    expect(i.etapes[0]).toMatchObject({ executant: null, aPrendre: { unite: null, profil: 'ROLE_SNA' }, moi: { reaffecter: true } })
    expect((await chef.processus.candidatsEtape(i.id, 'TRAITEMENT')).ailleurs.map((x) => x.id)).toEqual(['U-101', 'u-sna'])
    expect(await code(qui('u-res').processus.candidatsEtape(i.id, 'TRAITEMENT'))).toBe('403 PIL_PROCESSUS_INTERDIT')
    const ingenieur = qui('U-101')
    expect((await ingenieur.processus.instance(i.id)).etapes[0].moi.prendre).toBe(true)
    // La tâche (à prendre sans unité) est dans l'état des tâches du lanceur : visible et prenable par un porteur.
    const tache = (await chef.pilotage.taches({ vue: 'creees' })).contenu.find((t) => t.id === i.etapes[0].tacheId)!
    expect(tache).toMatchObject({ etat: 'A_PRENDRE', uniteCible: null, profilRequis: 'ROLE_SNA' })
    expect(await code(chef.processus.reaffecter(i.id, 'TRAITEMENT', 'u-conf', 'Essai'))).toBe('422 PIL_PROCESSUS_PERSONNE_INVALIDE')
    i = await chef.processus.reaffecter(i.id, 'TRAITEMENT', 'u-sna', 'Dossier connu')
    expect(i.etapes[0].executant?.id).toBe('u-sna')
    // Personne ne porte le profil : contrôle levable ; au lancement, l'étape va au chef de l'unité pour désigner.
    const vide = await chef.processus.creer(saisie([etape('ROLE_ADMIN_SYSTEME')], { code: 'AVIS_VIDE' }))
    expect((await chef.processus.controler(vide.id)).anomalies.map((a) => [a.code, a.gravite])).toEqual([['COUVERTURE_PROFIL', 'LEVABLE']])
    await chef.processus.publier(vide.id, { leverAvertissements: true, motif: 'Recrutement en cours' })
    expect((await chef.processus.lancer(vide.id, { intitule: 'Avis 4', champs: {}, designations: {} })).etapes[0].executant?.id).toBe('u-resp-info')
  })

  it('signature : ordre, réauthentification exigée, signataire attendu ; instance close ; suspension, reprise, annulation', async () => {
    const qui = monde()
    const chef = qui('u-resp-info')
    const i0 = (await chef.processus.instances({ vue: 'miennes' })).contenu.find((x) => x.intitule.startsWith('Flux SFTP'))!
    const i = await chef.processus.instance(i0.id)
    const recette = i.etapes.find((e) => e.code === 'RECETTE')!
    expect(recette).toMatchObject({ etat: 'A_SIGNER', moi: { signer: ['SIGNATURE_RECETTE'] } })
    expect(await code(qui('u-res').processus.signer(i.id, 'RECETTE', 'SIGNATURE_RECETTE', 'a'))).toBe('403 PIL_PROCESSUS_NON_SIGNATAIRE')
    expect(await code(chef.processus.signer(i.id, 'RECETTE', 'SIGNATURE_RECETTE', ''))).toBe('428 PIL_PROCESSUS_REAUTH_REQUISE')
    expect(await code(chef.processus.signer(i.id, 'RECETTE', 'INCONNUE', 'a'))).toBe('404 PIL_PROCESSUS_SIGNATURE_INCONNUE')
    const close = await chef.processus.signer(i.id, 'RECETTE', 'SIGNATURE_RECETTE', 'assertion')
    const s = close.etapes[3].signatures[0]
    expect(s).toMatchObject({ etat: 'SIGNEE', signePar: { id: 'u-resp-info' }, documents: [{ livrable: 'PV_RECETTE' }, { livrable: 'NOTE_ANALYSE' }] })
    expect(s.empreinte).toHaveLength(64)
    expect(close).toMatchObject({ etat: 'CLOSE', avancement: { terminees: 4, total: 4 } })
    expect(await code(chef.processus.signer(i.id, 'RECETTE', 'SIGNATURE_RECETTE', 'a'))).toBe('409 PIL_PROCESSUS_INSTANCE_ETAT')
    // Instance B (analyse à prendre) : suspension, reprise, annulation par le chef ; refusées au membre.
    const b = (await chef.processus.instances({ vue: 'unite' })).contenu.find((x) => x.intitule.startsWith('Ouverture du port'))!
    expect(await code(qui('u-res').processus.suspendre(b.id, 'x'))).toBe('403 PIL_PROCESSUS_INTERDIT')
    expect(await code(chef.processus.suspendre(b.id, ''))).toBe('400 PIL_REQUETE_INVALIDE')
    const susp = await chef.processus.suspendre(b.id, 'Gel des changements')
    expect(susp).toMatchObject({ etat: 'SUSPENDUE', motif: 'Gel des changements', droits: { reprendre: true, suspendre: false } })
    expect(await code(qui('u-res').processus.terminer(b.id, 'ANALYSE'))).toBe('409 PIL_PROCESSUS_INSTANCE_ETAT')
    expect(await code(chef.processus.suspendre(b.id, 'encore'))).toBe('409 PIL_PROCESSUS_INSTANCE_ETAT')
    expect((await chef.processus.reprendre(b.id)).etat).toBe('EN_COURS')
    expect(await code(chef.processus.reprendre(b.id))).toBe('409 PIL_PROCESSUS_INSTANCE_ETAT')
    expect(await code(qui('u-res').processus.reprendre(b.id))).toBe('403 PIL_PROCESSUS_INTERDIT')
    expect(await code(qui('u-res').processus.annuler(b.id, 'x'))).toBe('403 PIL_PROCESSUS_INTERDIT')
    const annulee = await chef.processus.annuler(b.id, 'Demande abandonnée')
    expect(annulee.etapes.map((e) => e.etat)).toEqual(['TERMINEE', 'ANNULEE', 'ANNULEE', 'ANNULEE'])
    expect(await code(chef.processus.annuler(b.id, 'encore'))).toBe('409 PIL_PROCESSUS_INSTANCE_ETAT')
    const tache = (await chef.pilotage.taches({ vue: 'creees' })).contenu
    expect(tache).toBeDefined()
    expect(await code(chef.processus.instance('INCONNUE'))).toBe('404 PIL_PROCESSUS_INSTANCE_INTROUVABLE')
    expect(await code(chef.processus.terminer(i.id, 'INCONNUE'))).toBe('409 PIL_PROCESSUS_INSTANCE_ETAT')
  })

  it('signatures dans l’ordre, validateur après signature ; étape inconnue ; lecture par un intervenant', async () => {
    const qui = monde()
    const chef = qui('U-001').processus
    const etapes: EtapeProcessus[] = [{
      ...etapeVierge(1), code: 'UNIQUE', libelle: 'Unique', intervenant: { mode: 'LANCEUR' }, validateur: { mode: 'N_PLUS_1_LANCEUR' },
      livrables: [{ code: 'L', libelle: 'L', typesPieces: ['application/*'], obligatoire: true, modele: null }],
      signatures: [
        { code: 'S2', libelle: 'Deuxième', signataire: { mode: 'LANCEUR' }, porte: ['L'], ordre: 2 },
        { code: 'S1', libelle: 'Première', signataire: { mode: 'PERSONNE', personneId: 'U-001', remplacement: 'ROLE_CHEF_DES' }, porte: ['L'], ordre: 1 },
      ],
    }]
    const p = await chef.creer(saisie(etapes, { code: 'DOUBLE_SIGNATURE', unite: 'DES' }))
    await chef.publier(p.id, {})
    const lanceur = qui('u-resp-info').processus
    let i = await lanceur.lancer(p.id, { intitule: 'Essai', champs: {}, designations: {} })
    const doc = await lanceur.televerserLivrable(fichier('a.pdf'), i.id)
    await lanceur.deposerLivrable(i.id, 'UNIQUE', 'L', doc.documentId)
    i = await lanceur.terminer(i.id, 'UNIQUE')
    expect(i.etapes[0]).toMatchObject({ etat: 'A_SIGNER', moi: { signer: [] } })
    expect(await code(lanceur.signer(i.id, 'UNIQUE', 'S2', 'a'))).toBe('409 PIL_PROCESSUS_SIGNATURE_ORDRE')
    expect(await code(lanceur.signer(i.id, 'INCONNUE', 'S2', 'a'))).toBe('404 PIL_PROCESSUS_ETAPE_INCONNUE')
    i = await chef.signer(i.id, 'UNIQUE', 'S1', 'a')
    expect(await code(chef.signer(i.id, 'UNIQUE', 'S1', 'a'))).toBe('409 PIL_PROCESSUS_DEJA_SIGNEE')
    i = await lanceur.signer(i.id, 'UNIQUE', 'S2', 'a')
    expect(i.etapes[0].etat).toBe('A_VALIDER')
    i = await chef.valider(i.id, 'UNIQUE', 'VALIDER')
    expect(i.etat).toBe('CLOSE')
  })
})
