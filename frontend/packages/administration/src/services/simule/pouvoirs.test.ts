import { describe, expect, it } from 'vitest'
import type { ApiError } from '@gimac/socle'
import { AUCUN_POUVOIR, magasinPouvoirs } from '@gimac/lot1'
import { servicesConsoleSimules } from './index'
import { PROFILS_DEMO } from '../../profilsDemo'

// MS-IAM simulé, côté administration (D-81) : matrice des pouvoirs, demandes à traiter (acceptation avec la
// permission de l'acte et FIDO2, refus motivé), profils créés et supprimés, responsable d'une unité.
const A = btoa('assertion-simulee')
const code = (p: Promise<unknown>) => p.then(() => 'ok', (e: ApiError) => `${e.status} ${e.code}`)
const admin = (permissions = PROFILS_DEMO.ROLE_SUPER_ADMIN.permissions, pouvoirs = magasinPouvoirs()) =>
  ({ s: servicesConsoleSimules({ latenceMs: 0, acteur: { id: 'u-super-admin', nom: 'Serge Mouelle Kombi', permissions }, pouvoirs }), pouvoirs })

describe('matrice des pouvoirs', () => {
  it('lecture, modification signée, journal ; refus sans permission ni clé', async () => {
    const { s } = admin()
    const m = await s.pouvoirs.matrice()
    expect(m.profils.find((l) => l.profil === 'ROLE_CHEF_DES')?.actions.TACHE_CONFIER).toBe('UNITES')
    const e = { actions: { ...AUCUN_POUVOIR, TACHE_CONFIER: 'EQUIPE' as const }, profilsDemandables: ['ROLE_COLLABORATEUR'] }
    expect(await code(s.pouvoirs.modifierMatrice('ROLE_SNA', e, 'Revue', ''))).toBe('428 IAM_REAUTH_REQUIRED')
    expect((await s.pouvoirs.modifierMatrice('ROLE_SNA', e, 'Revue annuelle', A)).actions.TACHE_CONFIER).toBe('EQUIPE')
    expect((await s.comptes.journal({ page: 1, taille: 5 })).contenu[0].action).toBe('MATRICE_POUVOIRS_MODIFIEE')
    expect(await code(admin(['IAM_ACCOUNT_READ']).s.pouvoirs.modifierMatrice('ROLE_SNA', e, 'Revue', A))).toBe('403 IAM_PERMISSION_REQUISE')
  })
})

describe('demandes à traiter', () => {
  it('liste (en attente d’abord), acceptation d’une création de compte et d’un profil, refus motivé', async () => {
    const { s, pouvoirs } = admin()
    const l = await s.pouvoirs.demandes({ page: 0, taille: 20 })
    expect(l.contenu.slice(0, 2).map((d) => d.etat)).toEqual(['EN_ATTENTE', 'EN_ATTENTE'])
    expect((await s.pouvoirs.demandes({ etat: 'ACCEPTEE', page: 0, taille: 20 })).total).toBe(1)
    expect(await code(s.pouvoirs.demande('X'))).toBe('404 ORG_DEMANDE_INTROUVABLE')
    expect(await code(s.pouvoirs.accepter('DEM-0001', 'ok', 'faux'))).toBe('403 IAM_REAUTH_INVALID')
    const acceptee = await s.pouvoirs.accepter('DEM-0001', 'Création validée', A)
    expect(acceptee).toMatchObject({ etat: 'ACCEPTEE', decision: { par: { id: 'u-super-admin' }, motif: 'Création validée' } })
    const cree = (await s.comptes.lister({ page: 1, taille: 50, q: 'i.fouda' })).contenu[0]
    expect(cree).toMatchObject({ unite: 'POLE_RESEAU', responsableId: 'u-resp-info', profils: ['ROLE_POLE_RESEAU'] })
    expect(pouvoirs.organisation.some((m) => m.id === cree.id)).toBe(true)
    expect(await code(s.pouvoirs.accepter('DEM-0001', 'encore', A))).toBe('409 ORG_DEMANDE_TRAITEE')
    await s.pouvoirs.accepter('DEM-0002', '', A)
    expect(pouvoirs.profilsDe('u-collab')).toEqual(['ROLE_COLLABORATEUR', 'ROLE_POLE_RESEAU'])
    expect(await code(s.pouvoirs.refuser('DEM-0002', 'Non'))).toBe('400 ORG_DEMANDE_INVALIDE')
  })
  it('statut : désactivation d’un compte connu ; permission de l’acte exigée ; refus', async () => {
    const pouvoirs = magasinPouvoirs()
    const d = pouvoirs.deposer((st, c, m) => ({ status: st, code: c, message: m }) as unknown as ApiError, 'U-001', { type: 'STATUT', cibleId: 'u-sna', contenu: { actif: false }, motif: 'Départ en retraite' })
    const lecteur = admin(['IAM_ACCOUNT_READ'], pouvoirs).s
    expect(await code(lecteur.pouvoirs.accepter(d.id, 'ok', A))).toBe('403 ORG_DEMANDE_INTERDITE')
    const { s } = admin(undefined, pouvoirs)
    await s.pouvoirs.accepter(d.id, 'Départ confirmé', A)
    expect((await s.comptes.lire('u-sna')).actif).toBe(false)
    expect(pouvoirs.organisation.find((m) => m.id === 'u-sna')?.actif).toBe(false)
    const r = pouvoirs.deposer((st, c, m) => ({ status: st, code: c, message: m }) as unknown as ApiError, 'U-001', { type: 'STATUT', cibleId: 'u-sys', contenu: { actif: false }, motif: 'Départ en retraite' })
    expect((await lecteur.pouvoirs.refuser(r.id, 'Départ non confirmé')).etat).toBe('REFUSEE')
  })
})

describe('profils créés et responsable d’unité', () => {
  it('création (code, unicité, permissions internes, privilège), suppression d’un profil créé sans titulaire', async () => {
    const { s, pouvoirs } = admin()
    const p = { code: 'ROLE_ASTREINTE', description: 'Astreinte de supervision', permissions: ['HOM_LIAISON_READ'] }
    expect(await code(s.configuration.creerProfil({ ...p, code: 'astreinte' }, 'Nouveau', A))).toBe('400 IAM_PROFIL_CODE_INVALIDE')
    expect(await code(s.configuration.creerProfil({ ...p, code: 'ROLE_SNA' }, 'Nouveau', A))).toBe('409 IAM_PROFIL_EXISTANT')
    expect(await code(s.configuration.creerProfil({ ...p, description: ' ' }, 'Nouveau', A))).toBe('400 IAM_PROFIL_INVALIDE')
    expect(await code(s.configuration.creerProfil({ ...p, permissions: ['INTERNAL_IAM_READ'] }, 'Nouveau', A))).toBe('422 IAM_PROFIL_PERMISSION_INTERNE')
    expect(await code(s.configuration.creerProfil({ ...p, permissions: ['X_Y'] }, 'Nouveau', A))).toBe('400 IAM_PERMISSION_UNKNOWN')
    const cree = await s.configuration.creerProfil(p, 'Astreinte de nuit', A)
    expect(cree).toMatchObject({ code: 'ROLE_ASTREINTE', privilegie: false, cree: true, titulaires: 0 })
    expect(cree.permissions).toEqual(expect.arrayContaining(['PIL_ACTIVITY_OWN', 'NOT_INBOX_READ']))
    expect(pouvoirs.matrice.some((l) => l.profil === 'ROLE_ASTREINTE')).toBe(true)
    const rssi = admin(PROFILS_DEMO.ROLE_RSSI.permissions).s
    expect(await code(rssi.configuration.creerProfil({ ...p, permissions: ['IAM_ACCOUNT_READ'] }, 'Nouveau', A))).toBe('403 IAM_PRIVILEGED_PROFILE')
    expect((await s.configuration.creerProfil({ ...p, code: 'ROLE_AUDIT', permissions: ['IAM_JOURNAL_READ'] }, 'Audit', A)).privilegie).toBe(true)
    expect(await code(admin(['IAM_ACCOUNT_READ']).s.configuration.creerProfil(p, 'x', A))).toBe('403 IAM_PERMISSION_REQUISE')
    expect(await code(s.configuration.supprimerProfil('ROLE_SNA', 'Ménage', A))).toBe('409 IAM_PROFIL_SYSTEME')
    expect(await code(s.configuration.supprimerProfil('ROLE_INCONNU', 'Ménage', A))).toBe('404 IAM_PROFILE_NOT_FOUND')
    const c = (await s.comptes.lister({ page: 1, taille: 50, q: 'r.biyong' })).contenu[0]
    await s.comptes.definirProfils(c.id, ['ROLE_SNA', 'ROLE_ASTREINTE'], 'Astreinte', A)
    expect(await code(s.configuration.supprimerProfil('ROLE_ASTREINTE', 'Ménage', A))).toBe('409 IAM_PROFIL_TITULAIRES')
    await s.comptes.definirProfils(c.id, ['ROLE_SNA'], 'Fin d’astreinte', A)
    await s.configuration.supprimerProfil('ROLE_ASTREINTE', 'Ménage', A)
    expect((await s.configuration.profils()).some((x) => x.code === 'ROLE_ASTREINTE')).toBe(false)
    expect(pouvoirs.matrice.some((l) => l.profil === 'ROLE_ASTREINTE')).toBe(false)
  })
  it('responsable d’une unité : compte interne actif seulement', async () => {
    const { s } = admin()
    const des = (await s.organisation.unites()).find((u) => u.code === 'DES')!
    expect(des.responsableId).toBe('u-chef-des')
    expect((await s.organisation.modifierUnite('PROD_INFO', { ...des, code: 'PROD_INFO', parent: 'DES', responsableId: 'u-ingenieur' })).responsableId).toBe('u-ingenieur')
    expect(await code(s.organisation.modifierUnite('PROD_INFO', { ...des, code: 'PROD_INFO', parent: 'DES', responsableId: 'u-inactif' }))).toBe('422 ORG_RESPONSABLE_INVALIDE')
    expect(await code(s.organisation.modifierUnite('PROD_INFO', { ...des, code: 'PROD_INFO', parent: 'DES', responsableId: 'u-correspondant' }))).toBe('422 ORG_RESPONSABLE_INVALIDE')
  })
})
