import { afterEach, describe, expect, it } from 'vitest'
import { servicesSimules } from '.'
import { magasinPouvoirs } from './pouvoirs'
import { COMPTES_ORGANISATION_DEMO, roleDeCleDemo } from '../../profilsDemo'

// Recette du travail quotidien (docs/recette/quotidien.md) : chaque anomalie majeure corrigée dans les services simulés,
// mêmes règles que MS-PILOTAGE (ProcessusSeparationFonctionsIT).

const code = (p: Promise<unknown>) => p.then(() => 'ok', (e: { status: number; code: string }) => `${e.status} ${e.code}`)
const NOMS: Record<string, string> = {
  'U-001': 'Chef Département DES', 'u-dg': 'Agent Conformité', 'u-collab': 'Collaboratrice Réseau', 'u-rh': 'Gestionnaire RH Démo', 'u-sna': 'Responsable SNA',
  'u-res': 'Agent Pôle Réseau', 'u-resp-info': 'Responsable Production', 'u-sys': 'Opératrice Pôle Système',
}
function monde() {
  const magasin = magasinPouvoirs()
  return Object.assign((id: string) => servicesSimules({ latenceMs: 0, utilisateur: { id, nom: NOMS[id] ?? id }, pouvoirs: magasin }), { magasin })
}
const lancables = async (s: ReturnType<ReturnType<typeof monde>>, code: string) =>
  (await s.processus.lancables({})).groupes.flatMap((g) => g.processus).find((p) => p.code === code)!
const periode = { debut: '2026-12-21', fin: '2026-12-31' }

afterEach(() => sessionStorage.clear())

describe('recette du travail quotidien : services simulés', () => {
  it('QUO-01 : comptes de démonstration du service RH avec leur profil', () => {
    expect(COMPTES_ORGANISATION_DEMO['u-rh']).toMatchObject({ identifiant: 'g.rh', nom: 'Gestionnaire RH Démo', role: 'ROLE_COLLABORATEUR' })
    expect(COMPTES_ORGANISATION_DEMO['u-rh-chef']).toMatchObject({ identifiant: 'c.ressources', nom: 'Cheffe Ressources Humaines', role: 'ROLE_RESPONSABLE_SERVICE' })
    expect(roleDeCleDemo('u-rh')).toBe('ROLE_COLLABORATEUR')
    expect(roleDeCleDemo('ROLE_DG')).toBe('ROLE_DG')
  })

  it('QUO-02 : la demandeuse n’est ni proposée ni réaffectée sur sa propre demande (séparation des fonctions)', async () => {
    const qui = monde()
    const collaboratrice = qui('u-collab')
    const conge = await lancables(collaboratrice, 'DEMANDE_CONGE')
    const i = await collaboratrice.processus.lancer(conge.id, { intitule: 'Congé annuel de décembre', champs: {}, designations: {}, formulaire: { PERIODE: periode, MOTIF: 'Congé annuel.' } })
    // D-138 : la vérification des droits est adressée au rôle du gestionnaire RH ; la demandeuse n'y a aucune action.
    expect(i.etapes[0]).toMatchObject({ code: 'ANALYSE', executant: null, aPrendre: expect.objectContaining({ unite: 'RH' }) })
    // Aucune exécution ni prise ; étape à prendre, la demandeuse peut seulement choisir l'exécutant (lanceur, D-87).
    expect(i.etapes[0].moi).toMatchObject({ executer: false, prendre: false })
    // Réaffectation par le DG : candidats sans la demandeuse, nommée dans « exclus » ; la lui confier est refusé.
    const dg = qui('u-dg')
    const candidats = await dg.processus.candidatsEtape(i.id, 'ANALYSE')
    expect([...candidats.dansUnite, ...candidats.ailleurs].map((c) => c.id)).not.toContain('u-collab')
    expect(candidats.exclus).toEqual(['u-collab'])
    expect(await code(dg.processus.reaffecter(i.id, 'ANALYSE', 'u-collab', 'Absence du gestionnaire'))).toBe('422 PIL_PROCESSUS_SEPARATION_FONCTIONS')
    // Le gestionnaire RH lance lui-même une demande : sa vérification va à l'autre porteur du profil, jamais à lui.
    const gestrh = qui('u-rh')
    const j = await gestrh.processus.lancer(conge.id, { intitule: 'Congé du gestionnaire', champs: {}, designations: {}, formulaire: { PERIODE: periode, MOTIF: 'Congé annuel.' } })
    expect(j.etapes[0].executant?.id).not.toBe('u-rh')
  })

  it('QUO-03, QUO-12 : un porteur lit l’étape à prendre ; un tiers reçoit un refus explicite, jamais « introuvable »', async () => {
    const qui = monde()
    const chef = qui('U-001').processus
    // Processus du SNA confié au profil Collaborateur, porté seulement hors du SNA (pôle réseau, RH) : étape à prendre.
    const p = await chef.creer({
      code: 'RECENSEMENT_POSTES', unite: 'SNA', libelle: 'Recensement des postes', description: '', categorie: 'Essai', delaiCibleJours: 5,
      etapes: [{ code: 'RECENSEMENT', libelle: 'Recensement', action: 'Recenser les postes.', intervenant: { mode: 'PROFIL', profil: 'ROLE_COLLABORATEUR' }, validateur: null, predecesseurs: [], delaiJours: 2, livrables: [], signatures: [], champs: [], position: { x: 0, y: 0 } }],
    })
    await chef.publier(p.id, { leverAvertissements: true, motif: 'Essai' })
    const i = await qui('u-sna').processus.lancer(p.id, { intitule: 'Recensement d’octobre', champs: {}, designations: {} })
    expect(i.etapes[0]).toMatchObject({ executant: null, aPrendre: { profil: 'ROLE_COLLABORATEUR', unite: null } })
    // Avant la correction : 404 « Instance introuvable » pour la collaboratrice qui doit prendre l'étape.
    const vue = await qui('u-collab').processus.instance(i.id)
    expect(vue.etapes[0].moi.prendre).toBe(true)
    expect(await code(qui('u-sys').processus.instance(i.id))).toBe('403 PIL_PROCESSUS_INTERDIT')
    expect(await code(qui('u-sys').processus.instance('INS-9999'))).toBe('404 PIL_PROCESSUS_INSTANCE_INTROUVABLE')
  })

  it('QUO-04 : la pièce justificative se télécharge par qui lit la demande', async () => {
    const qui = monde()
    const collaboratrice = qui('u-collab')
    const conge = await lancables(collaboratrice, 'DEMANDE_CONGE')
    const depot = await collaboratrice.processus.televerserPieceLancement(new File(['%PDF-1.4 justificatif'], 'note.pdf', { type: 'application/pdf' }), conge.id)
    const i = await collaboratrice.processus.lancer(conge.id, { intitule: 'Congé avec justificatif', champs: {}, designations: {}, formulaire: { PERIODE: periode, MOTIF: 'Événement familial.', JUSTIFICATIF: { documentId: depot.documentId } } })
    for (const lecteur of ['u-collab', 'u-rh']) {
      const d = await qui(lecteur).processus.documentInstance(i.id, depot.documentId)
      expect(d.blob).toMatchObject({ size: '%PDF-1.4 justificatif'.length, type: 'application/pdf' })
    }
    expect(await code(qui('u-sys').processus.documentInstance(i.id, depot.documentId))).toBe('403 PIL_PROCESSUS_INTERDIT')
    expect(await code(collaboratrice.processus.documentInstance(i.id, 'DOC-INCONNU'))).toBe('404 PIL_PROCESSUS_DOCUMENT_INCONNU')
  })

  it('QUO-05 : notifications d’étape, d’avancement et de clôture de la demande', async () => {
    const qui = monde()
    const resp = qui('u-sna')
    const ip = await lancables(resp, 'CHANGEMENT_ADRESSE_IP')
    const i = await resp.processus.lancer(ip.id, {
      intitule: 'Poste d’accueil du rez-de-chaussée', champs: {}, designations: {},
      formulaire: { EQUIPEMENT: 'PC-ACC-01', ADRESSE_ACTUELLE: '10.20.4.15', ADRESSE_DEMANDEE: '10.20.4.60', JUSTIFICATION: 'Déménagement du poste.' },
    })
    const notes = resp.processusDemo
    // D-138 : étape adressée au rôle, notifiée à chaque porteur comme une étape à prendre.
    expect(notes.pour('u-res').map((n) => n.titre)).toContain('Étape à prendre : Analyse technique')
    const agentreseau = qui('u-res').processus
    await agentreseau.saisirChamps(i.id, 'ANALYSE', { ADRESSE_DISPONIBLE: 'Oui' })
    await agentreseau.terminer(i.id, 'ANALYSE')
    expect(notes.pour('u-sna').map((n) => n.titre)).toContain('Votre demande avance : Validation du chef de pôle')
    const prod = qui('u-resp-info').processus
    expect(notes.pour('u-resp-info').map((n) => n.titre)).toContain('Étape à réaliser : Validation du chef de pôle')
    await prod.saisirChamps(i.id, 'VALIDATION', { DECISION: 'Accordée' })
    await prod.terminer(i.id, 'VALIDATION')
    await agentreseau.saisirChamps(i.id, 'EXECUTION', { ADRESSE_ATTRIBUEE: '10.20.4.60' })
    await agentreseau.terminer(i.id, 'EXECUTION')
    const close = await agentreseau.terminer(i.id, 'CLOTURE')
    expect(close.etat).toBe('CLOSE')
    const cloture = notes.pour('u-sna').find((n) => n.titre === 'Processus terminé : Poste d’accueil du rez-de-chaussée')
    expect(cloture).toMatchObject({ type: 'PROCESSUS', lien: `/des/processus/instances/${i.id}` })
    // Jamais pour l'acteur lui-même.
    expect(notes.pour('u-res').some((n) => n.acteur?.id === 'u-res')).toBe(false)
  })

  it('QUO-09 : sans lecture des liaisons, la messagerie de démonstration ne mène à aucune liaison', async () => {
    const collab = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-collab', nom: 'Collaboratrice Réseau', profils: ['ROLE_COLLABORATEUR'] } })
    const conversations = (await collab.messagerie.conversations({})).contenu
    expect(conversations.length).toBeGreaterThan(0)
    expect(conversations.every((c) => !c.liaison)).toBe(true)
    const fil = await collab.messagerie.messages(conversations.find((c) => c.titre === 'Essais de LIA-2026-030')!.id)
    expect(fil.contenu.every((m) => !m.etiquettes?.length)).toBe(true)
    // Qui lit les liaisons garde le lien de la conversation.
    const sna = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-sna', nom: 'Responsable SNA', profils: ['ROLE_SNA'] } })
    expect((await sna.messagerie.conversations({})).contenu.some((c) => c.liaison?.reference === 'LIA-2026-030')).toBe(true)
  })

  it('QUO-19 : les tâches de processus terminées survivent au rechargement de la démonstration', async () => {
    sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: 'c.reseau' }))
    const utilisateur = { id: 'u-collab', nom: 'Collaboratrice Réseau' }
    const avant = servicesSimules({ latenceMs: 0, utilisateur, pouvoirs: magasinPouvoirs() })
    const terminees = (await avant.pilotage.taches({ vue: 'moi' })).contenu.filter((t) => t.origine.type === 'PROCESSUS' && t.etat === 'TERMINEE')
    expect(terminees.length).toBeGreaterThan(0)
    // Rechargement de la page : nouvel état des services, processus repris de la session.
    const apres = servicesSimules({ latenceMs: 0, utilisateur, pouvoirs: magasinPouvoirs() })
    const reprises = (await apres.pilotage.taches({ vue: 'moi' })).contenu.filter((t) => t.origine.type === 'PROCESSUS' && t.etat === 'TERMINEE')
    expect(reprises.map((t) => t.origine.reference).sort()).toEqual(terminees.map((t) => t.origine.reference).sort())
  })
})
