import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@gimac/socle'
import { servicesSimules } from './index'
import { ORGANISATION_DEMO, UNITES_DEMO, rattachementDemo } from './pilotage'
import { aujourdhui } from '../../modele/jours'

// « Mon activité » simulée (D-76) : mêmes formes et mêmes refus que MS-PILOTAGE et l'organisation de MS-IAM
// (docs/lot1/mon-activite.md) : équipe par la chaîne des responsables, transitions des tâches, rapport unique
// par période, destinataire fixé à la soumission, visa du destinataire, préremplissage, document déposé.
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-10-06T08:00:00Z'))
})
afterEach(() => vi.useRealTimers())

const svc = (id = 'u-collab', nom = 'Nadège Mvondo Assiga') => servicesSimules({ latenceMs: 0, utilisateur: { id, nom } }).pilotage
const code = (p: Promise<unknown>) => p.then(() => 'ok', (e: ApiError) => `${e.status} ${e.code}`)

describe('organisation de démonstration', () => {
  it('unités du §1.1, organisation cohérente, rattachement et chaîne', () => {
    expect(UNITES_DEMO.map((u) => u.code)).toEqual(['DG', 'DES', 'SNA', 'PROD_MONETIQUE', 'PROD_INFO', 'POLE_RESEAU', 'POLE_SYSTEME', 'DDRM', 'SSI', 'CONFORMITE', 'RH'])
    ORGANISATION_DEMO.forEach((m) => {
      expect(UNITES_DEMO.some((u) => u.code === m.unite)).toBe(true)
      if (m.responsableId) expect(ORGANISATION_DEMO.some((x) => x.id === m.responsableId)).toBe(true)
    })
    const r = rattachementDemo('u-collab')
    expect(r.unite?.code).toBe('POLE_RESEAU')
    expect(r.chemin.map((u) => u.code)).toEqual(['DG', 'DES', 'PROD_INFO', 'POLE_RESEAU'])
    expect(r.responsable).toEqual({ id: 'u-resp-info', nomAffiche: 'Gaston Ndzana Ebode', fonction: 'Chef du service Production informatique' })
    expect(r.chaine).toEqual(['u-resp-info', 'U-001', 'u-dg'])
    expect(rattachementDemo('u-resp-info').equipeDirecte).toBe(3)
    expect(rattachementDemo('inconnu', 'Personne')).toMatchObject({ nomAffiche: 'Personne', unite: null, responsable: null, chaine: [] })
  })
})

describe('pilotageSimule', () => {
  it('activités : jour trié, ajout, statut, report (copie « À faire »), modification, suppression, auteur seul', async () => {
    const s = svc()
    const jour = await s.activites('2026-10-06', '2026-10-06')
    expect(jour.map((a) => a.ordre)).toEqual([1, 2, 3])
    const a = await s.creerActivite({ jour: '2026-10-06', titre: ' Câbler la baie 3 ', description: '', dureeMinutes: null })
    expect(a).toMatchObject({ titre: 'Câbler la baie 3', statut: 'A_FAIRE', ordre: 4, auteur: { id: 'u-collab' } })
    expect(await code(s.creerActivite({ jour: '2026-10-06', titre: ' ', description: null, dureeMinutes: null }))).toBe('400 PIL_REQUETE_INVALIDE')
    expect((await s.changerStatut(a.id, 'FAITE')).statut).toBe('FAITE')
    expect(await code(s.changerStatut(a.id, 'REPORTEE'))).toBe('400 PIL_REQUETE_INVALIDE')
    const r = await s.changerStatut(a.id, 'REPORTEE', '2026-10-07')
    expect(r).toMatchObject({ statut: 'REPORTEE', reporteeAu: '2026-10-07' })
    expect((await s.activites('2026-10-07', '2026-10-07')).some((x) => x.titre === 'Câbler la baie 3' && x.statut === 'A_FAIRE')).toBe(true)
    expect(await code(s.modifierActivite(a.id, { jour: '2026-10-06', titre: 'X', description: null, dureeMinutes: 2000 }))).toBe('400 PIL_REQUETE_INVALIDE')
    expect(await code(s.modifierActivite(a.id, { jour: '2026-10-06', titre: '', description: null, dureeMinutes: null }))).toBe('400 PIL_REQUETE_INVALIDE')
    expect((await s.modifierActivite(a.id, { jour: '2026-10-06', titre: 'Câbler', description: 'baie', dureeMinutes: 45, ordre: 1 })).ordre).toBe(1)
    await s.supprimerActivite(a.id)
    expect(await code(s.supprimerActivite(a.id))).toBe('404 PIL_ACTIVITE_INTROUVABLE')
    // Hors équipe : lecture refusée ; équipe : lecture permise.
    expect(await code(s.activites('2026-10-06', '2026-10-06', 'u-res'))).toBe('403 PIL_HORS_EQUIPE')
    expect((await svc('u-resp-info', 'Gaston').activites('2026-10-06', '2026-10-06', 'u-res')).length).toBeGreaterThan(0)
  })
  it('agenda : semaine, invitation et réponse, création validée, lecture réduite d’un membre de l’équipe, propriétaire seul', async () => {
    const s = svc()
    const semaine = await s.agenda('2026-10-05', '2026-10-11')
    const invitation = semaine.find((e) => e.titre === 'Comité de pilotage des liaisons')!
    expect(invitation.moi).toBe('PARTICIPANT')
    expect((await s.repondre(invitation.id, 'ACCEPTE')).participants.find((p) => p.id === 'u-collab')?.reponse).toBe('ACCEPTE')
    expect(await code(s.modifierEvenement(invitation.id, { titre: 'x', description: null, lieu: null, debut: invitation.debut, fin: invitation.fin, journeeEntiere: false, categorie: 'REUNION', participants: [] }))).toBe('403 PIL_ACTION_RESERVEE')
    const base = { titre: 'Visite du centre', description: ' ', lieu: 'Douala', debut: '2026-10-08T10:00:00+01:00', fin: '2026-10-08T09:00:00+01:00', journeeEntiere: false, categorie: 'DEPLACEMENT' as const, participants: ['u-sys', 'u-collab'] }
    expect(await code(s.creerEvenement(base))).toBe('400 PIL_REQUETE_INVALIDE')
    expect(await code(s.creerEvenement({ ...base, fin: '2026-10-08T12:00:00+01:00', participants: Array.from({ length: 51 }, (_, i) => `p${i}`) }))).toBe('422 PIL_PARTICIPANT_INVALIDE')
    const e = await s.creerEvenement({ ...base, fin: '2026-10-08T12:00:00+01:00' })
    expect(e).toMatchObject({ moi: 'PROPRIETAIRE', description: null, participants: [{ id: 'u-sys', reponse: 'EN_ATTENTE' }] })
    const m = await s.modifierEvenement(e.id, { ...base, fin: '2026-10-08T12:00:00+01:00', participants: ['u-sys', 'u-res'] })
    expect(m.participants.map((p) => p.id)).toEqual(['u-sys', 'u-res'])
    await s.supprimerEvenement(e.id)
    expect(await code(s.supprimerEvenement(e.id))).toBe('404 PIL_EVENEMENT_INTROUVABLE')
    expect(await code(s.repondre('EVT-inconnu', 'DECLINE'))).toBe('404 PIL_EVENEMENT_INTROUVABLE')
    // Responsable : agenda d'un membre réduit à la catégorie, au titre et aux horaires.
    const chef = svc('u-resp-info', 'Gaston Ndzana Ebode')
    const autre = await chef.agenda('2026-10-06', '2026-10-06', 'u-sys')
    expect(autre.find((x) => x.categorie === 'ABSENCE')).toMatchObject({ moi: 'EQUIPE', lieu: null, participants: [] })
    expect(await code(s.agenda('2026-10-06', '2026-10-06', 'U-001'))).toBe('403 PIL_HORS_EQUIPE')
  })
  it('tâches : vues, création pour soi ou pour l’équipe, transitions motivées, réaffectation par le créateur, historique', async () => {
    const chef = svc('u-resp-info', 'Gaston Ndzana Ebode')
    const confiees = await chef.taches({ vue: 'creees' })
    expect(confiees.total).toBeGreaterThan(0)
    expect((await chef.taches({ vue: 'equipe', etat: 'TERMINEE', page: 0, taille: 2 })).contenu.every((t) => t.etat === 'TERMINEE')).toBe(true)
    expect(await code(chef.creerTache({ titre: 'x', description: null, echeance: null, priorite: 'NORMALE', responsableId: 'u-rssi' }))).toBe('403 PIL_HORS_EQUIPE')
    expect(await code(chef.creerTache({ titre: ' ', description: null, echeance: null, priorite: 'NORMALE' }))).toBe('400 PIL_REQUETE_INVALIDE')
    const t = await chef.creerTache({ titre: 'Préparer la maintenance', description: 'baie 3', echeance: '2026-10-05', priorite: 'URGENTE', responsableId: 'u-collab' })
    expect(t).toMatchObject({ etat: 'OUVERTE', enRetard: true, responsable: { id: 'u-collab' }, createur: { id: 'u-resp-info' } })
    // Le créateur ne fait pas les transitions du responsable, mais peut annuler.
    expect(await code(chef.transition(t.id, 'EN_COURS'))).toBe('409 PIL_TRANSITION_INTERDITE')
    // Le responsable : transitions permises seulement, motif exigé pour un blocage.
    const collab = svc()
    const sienne = (await collab.taches({ vue: 'moi', etat: 'OUVERTE' })).contenu[0]
    expect(sienne.createur.id).toBe('u-resp-info')
    expect(await code(collab.transition(sienne.id, 'TERMINEE'))).toBe('409 PIL_TRANSITION_INTERDITE')
    expect(await code(collab.transition(sienne.id, 'BLOQUEE', ' '))).toBe('422 PIL_MOTIF_OBLIGATOIRE')
    expect((await collab.transition(sienne.id, 'BLOQUEE', 'Attente du matériel')).etat).toBe('BLOQUEE')
    // Les instances simulées sont indépendantes : on poursuit sur celle du responsable.
    const autre = await chef.creerTache({ titre: 'Relire', description: null, echeance: null, priorite: 'BASSE' })
    expect((await chef.transition(autre.id, 'EN_COURS')).etat).toBe('EN_COURS')
    expect((await chef.transition(autre.id, 'TERMINEE')).termineeLe).not.toBeNull()
    expect(await code(chef.transition(autre.id, 'EN_COURS'))).toBe('422 PIL_MOTIF_OBLIGATOIRE')
    expect((await chef.transition(autre.id, 'EN_COURS', 'Relecture incomplète')).etat).toBe('EN_COURS')
    const modifiee = await chef.modifierTache(t.id, { titre: 'Préparer la maintenance', description: null, echeance: null, priorite: 'HAUTE', responsableId: 'u-res' })
    expect(modifiee.responsable?.id).toBe('u-res')
    expect(await code(chef.modifierTache(t.id, { titre: 'x', description: null, echeance: null, priorite: 'HAUTE', responsableId: 'u-dg' }))).toBe('403 PIL_HORS_EQUIPE')
    expect(await code(svc().modifierTache(t.id, { titre: 'x', description: null, echeance: null, priorite: 'HAUTE' }))).toBe('404 PIL_TACHE_INTROUVABLE')
    expect((await chef.tache(t.id)).historique?.map((h) => h.action)).toEqual(['CREEE', 'MODIFIEE', 'REAFFECTEE', 'MODIFIEE'])
    expect((await chef.transition(t.id, 'ANNULEE', 'Doublon de la tâche du pôle')).etat).toBe('ANNULEE')
    expect(await code(chef.tache('TCH-inconnue'))).toBe('404 PIL_TACHE_INTROUVABLE')
    expect(await code(collab.modifierTache(confiees.contenu[0].id, { titre: 'x', description: null, echeance: null, priorite: 'BASSE' }))).not.toBe('ok')
  })
  it('rapports : préremplissage, brouillon unique, période future, soumission au n+1, visa du destinataire, reçus', async () => {
    const s = svc()
    const pre = await s.preremplir('QUOTIDIEN', '2026-10-06')
    expect(pre.contenu).toContain('# Rapport quotidien du 2026-10-06')
    expect(pre.contenu).toContain('## Réalisé')
    expect(pre.contenu).toContain('## Prévu demain')
    expect((await s.preremplir('HEBDOMADAIRE', '2026-W41')).contenu).toContain('## Total des heures')
    expect(await code(s.preremplir('QUOTIDIEN', '2026-10-09'))).toBe('422 PIL_PERIODE_FUTURE')
    expect(await code(s.creerRapport({ type: 'HEBDOMADAIRE', periode: '2026-W45', mode: 'REDIGE', contenu: 'x' }))).toBe('422 PIL_PERIODE_FUTURE')
    expect(await code(s.creerRapport({ type: 'QUOTIDIEN', periode: '2026-10-05', mode: 'REDIGE', contenu: 'x' }))).toBe('409 PIL_RAPPORT_EXISTANT')
    const r = await s.creerRapport({ type: 'QUOTIDIEN', periode: '2026-10-06', mode: 'PREREMPLI', contenu: pre.contenu })
    expect(r).toMatchObject({ etat: 'BROUILLON', destinataire: null })
    expect(r.indicateurs.activitesPrevues).toBeGreaterThan(0)
    const autre = svc()
    const vide = await autre.creerRapport({ type: 'HEBDOMADAIRE', periode: '2026-W41', mode: 'REDIGE', contenu: null })
    expect(vide.etat).toBe('BROUILLON')
    expect(await code(autre.soumettre(vide.id))).toBe('422 PIL_RAPPORT_INCOMPLET')
    const soumis = await s.soumettre(r.id)
    expect(soumis).toMatchObject({ etat: 'SOUMIS', destinataire: { id: 'u-resp-info' } })
    expect(await code(s.modifierRapport(r.id, { type: 'QUOTIDIEN', periode: '2026-10-06', mode: 'REDIGE', contenu: 'x' }))).toBe('409 PIL_RAPPORT_NON_MODIFIABLE')
    expect(await code(s.viser(r.id, 'auto'))).toBe('409 PIL_TRANSITION_INTERDITE')
    expect((await s.rapports({ vue: 'moi', type: 'QUOTIDIEN', etat: 'SOUMIS' })).contenu.some((x) => x.id === r.id)).toBe(true)
    // Le chef de service reçoit les rapports soumis de son équipe et les vise.
    const chef = svc('u-resp-info', 'Gaston Ndzana Ebode')
    const recus = await chef.rapports({ vue: 'recus' })
    expect(recus.contenu.every((x) => x.etat !== 'BROUILLON' && x.auteur.id !== 'u-resp-info')).toBe(true)
    const aViser = recus.contenu.find((x) => x.etat === 'SOUMIS')!
    const vise = await chef.viser(aViser.id, 'Bien reçu.')
    expect(vise).toMatchObject({ etat: 'VISE', visa: { par: { id: 'u-resp-info' }, commentaire: 'Bien reçu.' } })
    expect(await code(chef.viser(aViser.id, ''))).toBe('409 PIL_TRANSITION_INTERDITE')
    expect(await code(chef.rapport('RAP-inconnu'))).toBe('404 PIL_RAPPORT_INTROUVABLE')
    // Sommet de l'organisation : personne au-dessus, le rapport est déposé (plus de refus).
    const dg = svc('u-dg', 'Jean-Baptiste Ella Mintsa')
    const brouillon = await dg.creerRapport({ type: 'QUOTIDIEN', periode: '2026-10-06', mode: 'REDIGE', contenu: 'Comité' })
    expect(await dg.destinatairesRapport(brouillon.id)).toEqual({ destinataire: null, motif: null, depot: true, choix: [] })
    expect(await dg.soumettre(brouillon.id)).toMatchObject({ etat: 'DEPOSE', destinataire: null, destinataireMotif: null })
    expect(await code(dg.viser(brouillon.id, ''))).toBe('409 PIL_TRANSITION_INTERDITE')
  })
  it('rapports : destinataire annoncé (n+1, chefs des unités au-dessus), choix dans la chaîne, refus hors de la chaîne', async () => {
    const membre = svc('u-res', 'Idriss Mahamat Saleh')
    const r = await membre.creerRapport({ type: 'QUOTIDIEN', periode: '2026-10-06', mode: 'REDIGE', contenu: 'Analyse des flux' })
    const d = await membre.destinatairesRapport(r.id)
    expect(d).toMatchObject({ destinataire: { id: 'u-resp-info' }, motif: 'N_PLUS_UN', depot: false })
    expect(d.choix.map((c) => [c.id, c.lien])).toEqual([['u-resp-info', 'N_PLUS_UN'], ['U-001', 'CHEF_UNITE_PARENTE'], ['u-dg', 'CHEF_UNITE_PARENTE']])
    expect(await code(svc('u-collab', 'Nadège Mvondo Assiga').destinatairesRapport(r.id))).toBe('404 PIL_RAPPORT_INTROUVABLE')
    expect(await code(membre.soumettre(r.id, 'u-sna'))).toBe('422 PIL_DESTINATAIRE_INVALIDE')
    expect(await membre.soumettre(r.id, 'U-001')).toMatchObject({ etat: 'SOUMIS', destinataire: { id: 'U-001' }, destinataireMotif: 'CHOISI' })
  })
  it('document d’un rapport : dépôt par morceaux avec progression, ticket vérifié, aperçu ; refus et annulation', async () => {
    vi.useRealTimers()
    const s = svc()
    const r = await s.creerRapport({ type: 'QUOTIDIEN', periode: aujourdhui(), mode: 'DOCUMENT', contenu: null })
    const phases: string[] = []
    const f = new File(['%PDF-1.4'], 'rapport.pdf', { type: 'application/pdf' })
    const depot = await s.deposerDocument(f, r.id, { surProgression: (p) => phases.push(p.phase) })
    expect(phases.at(-1)).toBe('pret')
    expect(phases).toContain('verification')
    expect(await code(s.modifierRapport(r.id, { type: r.type, periode: r.periode, mode: 'DOCUMENT', contenu: null, document: { ticket: 'faux' } }))).toBe('422 PIL_DOCUMENT_INVALIDE')
    const m = await s.modifierRapport(r.id, { type: r.type, periode: r.periode, mode: 'DOCUMENT', contenu: 'Joint', document: { ticket: depot.documentId } })
    expect(m.document?.nom).toBe('rapport.pdf')
    expect(await (await s.documentRapport(r.id)).blob.text()).toBe('%PDF-1.4')
    expect(await code(s.documentRapport((await s.rapports({ vue: 'moi' })).contenu.find((x) => !x.document)!.id))).toBe('404 PIL_DOCUMENT_INTROUVABLE')
    expect(await code(s.deposerDocument(new File(['x'], 'carte-client.pdf'), r.id))).toBe('422 DOC_UPLOAD_REJECTED')
    const arret = new AbortController()
    const envoi = s.deposerDocument(f, r.id, { signal: arret.signal })
    arret.abort()
    await expect(envoi).rejects.toMatchObject({ name: 'AbortError' })
    // Le document du pôle réseau, déposé par son auteur, se lit par le chef de service.
    const chef = svc('u-resp-info', 'Gaston Ndzana Ebode')
    const avecDocument = (await chef.rapports({ vue: 'recus' })).contenu.find((x) => x.mode === 'DOCUMENT')!
    expect((await chef.documentRapport(avecDocument.id)).blob.type).toBe('application/pdf')
  })
  it('équipe, synthèse, unités et rattachement', async () => {
    const chef = svc('u-resp-info', 'Gaston Ndzana Ebode')
    const equipe = await chef.equipe('2026-10-06')
    expect(equipe.map((m) => m.collaborateur.id).sort()).toEqual(['u-collab', 'u-res', 'u-sys'])
    expect(equipe.find((m) => m.collaborateur.id === 'u-sys')?.absent).toBe(true)
    expect(equipe.every((m) => m.collaborateur.niveau === 1)).toBe(true)
    expect((await svc('U-001', 'Paul').equipe('2026-10-06')).some((m) => m.collaborateur.niveau === 2)).toBe(true)
    expect(await svc().equipe('2026-10-06')).toEqual([])
    const synthese = await svc().synthese('2026-10-06')
    expect(synthese.rapportDuJour).toBe('ABSENT')
    expect(synthese.evenements.length).toBeLessThanOrEqual(5)
    expect(synthese.taches.ouvertes).toBeGreaterThan(0)
    expect((await chef.synthese('2026-10-06')).rapportsARecevoir).toBeGreaterThan(0)
    expect((await chef.unites()).length).toBe(11)
    expect((await chef.organisation()).equipeDirecte).toBe(3)
    // Compte hors de l'organisation simulée (super administrateur) : ni unité ni responsable.
    const admin = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-super-admin', nom: 'Serge Mouelle Kombi' } }).pilotage
    expect(await admin.organisation()).toMatchObject({ unite: null, responsable: null })
  })
})
