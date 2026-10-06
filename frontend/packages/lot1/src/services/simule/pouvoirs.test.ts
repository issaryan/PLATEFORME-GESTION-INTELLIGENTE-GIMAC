import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@gimac/socle'
import { servicesSimules } from './index'
import { magasinPouvoirs, pouvoirsParDefaut } from './pouvoirs'
import { AUCUN_POUVOIR, couvre, couverts, couvreUnites, rangPortee, type NouvelleDemande } from '../../modele/pouvoirs'

// Pouvoirs hiérarchiques simulés (D-81, docs/lot1/pouvoirs-hierarchiques.md) : matrice par défaut des
// responsables, périmètre (chaîne et unités dirigées), demandes (portée, profils demandables, unicité,
// annulation), rattachement ; tâches confiées à plusieurs personnes ou à une unité, prise, attribution, relance.
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-10-06T08:00:00Z'))
})
afterEach(() => vi.useRealTimers())

const services = (id: string, nom = id) => servicesSimules({ latenceMs: 0, utilisateur: { id, nom } })
const code = (p: Promise<unknown>) => p.then(() => 'ok', (e: ApiError) => `${e.status} ${e.code}`)
const creation = (o: Partial<{ unite: string; responsableId: string; profils: string[]; identifiant: string; motif: string }> = {}): NouvelleDemande => ({
  type: 'CREATION_COMPTE', cibleId: null, motif: o.motif ?? 'Renfort du pôle système',
  contenu: { identifiant: o.identifiant ?? 'b.ondo', prenom: 'Blaise', nom: 'Ondo', email: 'b.ondo@gimac-afr.org', fonction: 'Technicien système', unite: o.unite ?? 'POLE_SYSTEME', responsableId: o.responsableId ?? 'u-resp-info', profils: o.profils ?? ['ROLE_POLE_SYSTEME'] },
})

describe('modèle des portées', () => {
  it('couverture d’un compte et des unités selon la portée', () => {
    const p = (portee: 'AUCUNE' | 'DIRECTS' | 'EQUIPE' | 'UNITES') => ({ actions: { ...AUCUN_POUVOIR, TACHE_CONFIER: portee } })
    expect(couvre(undefined, 'TACHE_CONFIER', { niveau: 1 })).toBe(false)
    expect(couvre(p('DIRECTS'), 'TACHE_CONFIER', { niveau: 1 })).toBe(true)
    expect(couvre(p('DIRECTS'), 'TACHE_CONFIER', { niveau: 2 })).toBe(false)
    expect(couvre(p('EQUIPE'), 'TACHE_CONFIER', { niveau: 2 })).toBe(true)
    expect(couvre(p('EQUIPE'), 'TACHE_CONFIER', { niveau: 0 })).toBe(false)
    expect(couvre(p('UNITES'), 'TACHE_CONFIER', { niveau: 0 })).toBe(true)
    expect(couvreUnites(p('UNITES'), 'TACHE_CONFIER')).toBe(true)
    expect(couvreUnites(p('EQUIPE'), 'TACHE_CONFIER')).toBe(false)
    expect(rangPortee(undefined)).toBe(0)
    expect(couverts(undefined, 'TACHE_CONFIER')).toEqual([])
    expect(pouvoirsParDefaut('ROLE_SNA').actions).toEqual(AUCUN_POUVOIR)
  })
})

describe('mes pouvoirs', () => {
  it('chef du DES : portées par défaut, équipe et unités dirigées', async () => {
    const p = await services('U-001').pouvoirs.moi()
    expect(p.actions).toMatchObject({ TACHE_CONFIER: 'UNITES', ACTIVITE_LIRE: 'EQUIPE', RATTACHEMENT_MODIFIER: 'DIRECTS' })
    expect(p.unitesDirigees).toEqual(expect.arrayContaining(['DES', 'SNA', 'PROD_INFO', 'POLE_RESEAU', 'POLE_SYSTEME']))
    expect(p.subordonnes.find((s) => s.id === 'u-resp-info')?.niveau).toBe(1)
    expect(p.subordonnes.find((s) => s.id === 'u-res')?.niveau).toBe(2)
    expect(p.profilsDemandables).not.toContain('ROLE_SUPER_ADMIN')
  })
  it('collaborateur : aucun pouvoir, aucun subordonné ; profils explicites et membres d’unité hors chaîne', async () => {
    const p = await services('u-collab').pouvoirs.moi()
    expect(rangPortee(p.actions.TACHE_CONFIER)).toBe(0)
    expect(p.subordonnes).toEqual([])
    expect(p.unitesDirigees).toEqual([])
    const m = magasinPouvoirs()
    m.unites.find((u) => u.code === 'POLE_RESEAU')!.responsableId = 'u-sys'
    m.definirProfils('u-sys', ['ROLE_RESPONSABLE_SERVICE'])
    expect(m.pouvoirsDe('u-sys').subordonnes.map((s) => [s.id, s.niveau])).toEqual(expect.arrayContaining([['u-res', 0], ['u-collab', 0]]))
    // Portée DIRECTS seule : pas de descente dans la chaîne.
    m.modifierMatrice((s, c, x) => new ApiError(s, c, x, ''), 'ROLE_CHEF_DES', { actions: { ...AUCUN_POUVOIR, ACTIVITE_LIRE: 'DIRECTS' }, profilsDemandables: [] })
    expect(m.pouvoirsDe('U-001').subordonnes.every((s) => s.niveau === 1)).toBe(true)
  })
})

describe('demandes à l’administration', () => {
  it('création de compte, refus du contrat, annulation', async () => {
    const s = services('U-001').pouvoirs
    const d = await s.demander(creation())
    expect(d).toMatchObject({ type: 'CREATION_COMPTE', etat: 'EN_ATTENTE', cible: null, demandeur: { id: 'U-001' } })
    expect(await code(s.demander(creation()))).toBe('409 ORG_DEMANDE_EXISTANTE')
    expect(await code(s.demander(creation({ identifiant: 'x.y', unite: 'DDRM' })))).toBe('403 ORG_HORS_PERIMETRE')
    expect(await code(s.demander(creation({ identifiant: 'x.y', responsableId: 'U-020' })))).toBe('403 ORG_HORS_PERIMETRE')
    expect(await code(s.demander(creation({ identifiant: 'x.y', profils: ['ROLE_SUPER_ADMIN'] })))).toBe('422 ORG_PROFIL_NON_DEMANDABLE')
    expect(await code(s.demander(creation({ identifiant: 'x.y', profils: [] })))).toBe('400 ORG_DEMANDE_INVALIDE')
    expect(await code(s.demander(creation({ identifiant: 'x.y', motif: 'abc' })))).toBe('400 ORG_DEMANDE_INVALIDE')
    const page = await s.demandes({})
    expect(page).toMatchObject({ page: 0, taille: 20 })
    expect(page.contenu[0].id).toBe(d.id)
    expect((await s.demandes({ etat: 'REFUSEE' })).contenu.map((x) => x.id)).toEqual(['DEM-0003'])
    expect((await s.annuler(d.id)).etat).toBe('ANNULEE')
    expect(await code(s.annuler(d.id))).toBe('409 ORG_DEMANDE_TRAITEE')
    expect((await s.demande('DEM-0003')).decision?.motif).toMatch(/ressources humaines/)
    expect(await code(s.demande('DEM-0002'))).toBe('404 ORG_DEMANDE_INTROUVABLE')
  })
  it('profils et statut : portée et profils demandables', async () => {
    const s = services('U-001').pouvoirs
    expect((await s.demander({ type: 'PROFILS', cibleId: 'u-res', contenu: { ajouter: ['ROLE_POLE_SYSTEME'], retirer: [] }, motif: 'Polyvalence' })).cible?.id).toBe('u-res')
    expect(await code(s.demander({ type: 'PROFILS', cibleId: 'u-sys', contenu: { ajouter: [], retirer: [] }, motif: 'Polyvalence' }))).toBe('400 ORG_DEMANDE_INVALIDE')
    expect(await code(s.demander({ type: 'PROFILS', cibleId: 'u-sys', contenu: { ajouter: ['ROLE_RSSI'], retirer: [] }, motif: 'Polyvalence' }))).toBe('422 ORG_PROFIL_NON_DEMANDABLE')
    expect(await code(s.demander({ type: 'STATUT', cibleId: 'sim-charge', contenu: { actif: false }, motif: 'Départ' }))).toBe('403 ORG_HORS_PERIMETRE')
    expect((await s.demander({ type: 'STATUT', cibleId: 'u-sys', contenu: { actif: false }, motif: 'Départ en retraite' })).etat).toBe('EN_ATTENTE')
  })
  it('rattachement d’un subordonné direct', async () => {
    const s = services('U-001').pouvoirs
    expect(await s.rattacher('u-sna', { unite: 'PROD_INFO', responsableId: 'u-resp-info' })).toMatchObject({ unite: 'PROD_INFO', responsableId: 'u-resp-info' })
    expect(await code(s.rattacher('u-res', { unite: null, responsableId: null }))).toBe('403 ORG_HORS_PERIMETRE')
    expect(await code(s.rattacher('U-201', { unite: 'DDRM', responsableId: null }))).toBe('403 ORG_HORS_PERIMETRE')
    expect(await code(s.rattacher('U-201', { unite: null, responsableId: 'U-020' }))).toBe('403 ORG_HORS_PERIMETRE')
    expect(await code(s.rattacher('U-201', { unite: null, responsableId: 'U-201' }))).toBe('422 ORG_RESPONSABLE_INVALIDE')
    // Valeur nulle : retirée, comme MS-IAM (les deux valeurs sont toujours envoyées).
    expect(await s.rattacher('U-201', { unite: null, responsableId: null, motif: 'Départ du pôle' })).toMatchObject({ unite: null, responsableId: null })
  })
  it('matrice : ligne inconnue, portée invalide, profil privilégié, profils créés', () => {
    const m = magasinPouvoirs()
    const refus = (s: number, c: string, x: string) => new ApiError(s, c, x, '')
    expect(() => m.modifierMatrice(refus, 'ROLE_X', { actions: AUCUN_POUVOIR, profilsDemandables: [] })).toThrow(/Profil inconnu/)
    expect(() => m.modifierMatrice(refus, 'ROLE_SNA', { actions: { ...AUCUN_POUVOIR, TACHE_CONFIER: 'TOUT' as never }, profilsDemandables: [] })).toThrow(/portée/)
    expect(() => m.modifierMatrice(refus, 'ROLE_SNA', { actions: AUCUN_POUVOIR, profilsDemandables: ['ROLE_RSSI'] })).toThrow(/privilégié/)
    m.ajouterProfil('ROLE_ASTREINTE')
    m.ajouterProfil('ROLE_ASTREINTE')
    expect(m.matrice.filter((l) => l.profil === 'ROLE_ASTREINTE')).toHaveLength(1)
    m.retirerProfil('ROLE_ASTREINTE')
    m.retirerProfil('ROLE_ASTREINTE')
    expect(m.matrice.some((l) => l.profil === 'ROLE_ASTREINTE')).toBe(false)
    expect(m.personne('inconnu').nomAffiche).toBe('inconnu')
    expect(m.personne('U-101').nomAffiche).toBe('Joël Ekotto Mballa')
  })
})

describe('tâches confiées (D-81 §6)', () => {
  const base = { titre: 'Vérifier les sauvegardes', description: null, echeance: '2026-10-09', priorite: 'NORMALE' as const }
  it('à plusieurs personnes : groupe, avancement, bornes et portée', async () => {
    const s = services('U-001').pilotage
    const g = await s.creerGroupe({ ...base, responsables: ['u-res', 'u-sys', 'u-res'] })
    expect(g.avancement).toEqual({ total: 2, terminees: 0, enRetard: 0 })
    expect((await s.groupe(g.groupeId)).taches.map((t) => t.responsable?.id).sort()).toEqual(['u-res', 'u-sys'])
    expect(await code(s.creerGroupe({ ...base, responsables: ['u-res'] }))).toBe('400 PIL_REQUETE_INVALIDE')
    expect(await code(s.creerGroupe({ ...base, responsables: ['u-res', 'sim-charge'] }))).toBe('403 PIL_HORS_EQUIPE')
    expect(await code(s.creerGroupe({ ...base, titre: ' ', responsables: ['u-res', 'u-sys'] }))).toBe('400 PIL_REQUETE_INVALIDE')
    expect((await s.groupe('GRP-0001')).avancement).toMatchObject({ total: 2, terminees: 1 })
    expect(await code(services('u-collab').pilotage.groupe('GRP-0001'))).toBe('404 PIL_GROUPE_INTROUVABLE')
    expect((await services('u-sna').pilotage.groupe('GRP-0001')).titre).toMatch(/grille d’essais/)
  })
  it('à une unité : tâche à prendre, prise par un membre, attribution, refus', async () => {
    const chef = services('U-001').pilotage
    const t = await chef.creerTache({ ...base, uniteCible: 'POLE_RESEAU' })
    expect(t).toMatchObject({ etat: 'A_PRENDRE', responsable: null, uniteCible: 'POLE_RESEAU' })
    expect(await code(chef.creerTache({ ...base, uniteCible: 'DDRM' }))).toBe('403 PIL_HORS_EQUIPE')
    expect((await chef.taches({ vue: 'unite' })).contenu.some((x) => x.id === t.id)).toBe(true)
    expect(await code(chef.prendre(t.id))).toBe('403 PIL_HORS_EQUIPE')
    expect(await code(chef.attribuer(t.id, 'u-sna'))).toBe('422 PIL_RESPONSABLE_INVALIDE')
    expect((await chef.attribuer(t.id, 'u-collab')).responsable?.id).toBe('u-collab')
    expect(await code(chef.attribuer(t.id, 'u-res'))).toBe('409 PIL_TRANSITION_INTERDITE')
    // Le membre du pôle prend la tâche à prendre de démonstration.
    const membre = services('u-res', 'Idriss Mahamat Saleh').pilotage
    // Hors étapes de processus d'unité (D-87), à prendre par les porteurs du profil « Pôle réseau ».
    const aPrendre = (await membre.taches({ vue: 'unite' })).contenu.filter((x) => x.origine.type !== 'PROCESSUS')
    expect(aPrendre.map((x) => x.titre)).toEqual(['Vérifier la supervision des liens du participant'])
    expect(await code(membre.attribuer(aPrendre[0].id, 'u-res'))).toBe('403 PIL_ACTION_RESERVEE')
    const prise = await membre.prendre(aPrendre[0].id)
    expect(prise).toMatchObject({ etat: 'OUVERTE', responsable: { id: 'u-res' } })
    expect((await membre.tache(prise.id)).historique?.at(-1)?.action).toBe('PRISE')
    expect(await code(membre.prendre(aPrendre[0].id))).toBe('409 PIL_TRANSITION_INTERDITE')
    expect(await code(services('sim-charge').pilotage.tache(aPrendre[0].id))).toBe('404 PIL_TACHE_INTROUVABLE')
  })
  it('relance : créateur ou TACHE_SUIVRE, une par 24 heures', async () => {
    const chef = services('U-001').pilotage
    const t = await chef.creerTache({ ...base, responsableId: 'u-res' })
    await chef.relancer(t.id)
    expect(await code(chef.relancer(t.id))).toBe('429 PIL_RELANCE_RECENTE')
    vi.setSystemTime(new Date('2026-10-07T09:00:00Z'))
    await chef.relancer(t.id)
    expect((await chef.tache(t.id)).historique?.filter((h) => h.action === 'RELANCEE')).toHaveLength(2)
    const propre = await chef.creerTache(base)
    expect(await code(chef.relancer(propre.id))).toBe('409 PIL_RELANCE_IMPOSSIBLE')
    // TACHE_SUIVRE : relance d'une tâche confiée par un autre ; sans ce pouvoir, refusée.
    const magasin = magasinPouvoirs()
    const suivi = servicesSimules({ latenceMs: 0, utilisateur: { id: 'U-001', nom: 'Paul' }, pouvoirs: magasin }).pilotage
    const deSonEquipe = (await suivi.taches({ vue: 'equipe' })).contenu.find((x) => x.createur.id === 'u-resp-info' && x.responsable?.id === 'u-res' && x.etat === 'OUVERTE')!
    await suivi.relancer(deSonEquipe.id)
    vi.setSystemTime(new Date('2026-10-09T09:00:00Z'))
    magasin.modifierMatrice((st, c, x) => new ApiError(st, c, x, ''), 'ROLE_CHEF_DES', { actions: { ...AUCUN_POUVOIR }, profilsDemandables: [] })
    expect(await code(suivi.relancer(deSonEquipe.id))).toBe('403 PIL_HORS_EQUIPE')
    const collab = services('u-collab').pilotage
    const mienne = await collab.creerTache(base)
    expect(await code(collab.relancer(mienne.id))).toBe('409 PIL_RELANCE_IMPOSSIBLE')
  })
})
