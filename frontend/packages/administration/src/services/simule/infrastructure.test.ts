import { afterEach, describe, expect, it, vi } from 'vitest'
import { servicesConsoleSimules } from './index'
import { gestesPermis } from './infrastructure'
import type { Charge, Operation } from '../../modele/infrastructure'

// Exploitation simulée de l'infrastructure (D-78) : mêmes gestes permis et mêmes refus que la passerelle (§5).
const ASSERTION = btoa('assertion-simulee')
const EXECUTANT = { id: 'u-admin-systeme', nom: 'Admin', permissions: ['PLATFORM_OPS_READ', 'PLATFORM_OPS_EXECUTE'] }
const sim = (o: Parameters<typeof servicesConsoleSimules>[0] = {}) => servicesConsoleSimules({ latenceMs: 0, dureeGesteMs: 0, acteur: EXECUTANT, ...o }).infrastructure
const base = (o: Partial<Charge>): Omit<Charge, 'gestes'> => ({
  nom: 'x', orchestrateur: 'KUBERNETES', etat: 'EN_COURS', sante: 'SAINE', repliques: { voulues: 1, pretes: 1 }, redemarrages: 0, image: null, demarreeLe: null, protegee: false, ...o,
})
afterEach(() => vi.useRealTimers())

describe('gestes permis', () => {
  it('Kubernetes : redémarrer et arrêter avec répliques, démarrer sans, échelle toujours ; charge protégée sans arrêt ; aucun geste pendant une opération', () => {
    expect(gestesPermis(base({}), false)).toEqual(['REDEMARRER', 'ARRETER', 'ECHELLE'])
    expect(gestesPermis(base({ protegee: true }), false)).toEqual(['REDEMARRER', 'ECHELLE'])
    expect(gestesPermis(base({ repliques: { voulues: 0, pretes: 0 } }), false)).toEqual(['DEMARRER', 'ECHELLE'])
    expect(gestesPermis(base({}), true)).toEqual([])
  })
  it('Kubernetes : rétablir une charge active qui a une version précédente d’image différente, jamais sous Docker ni à zéro réplique', () => {
    expect(gestesPermis(base({ imagePrecedente: 'x:1', protegee: true }), false)).toEqual(['REDEMARRER', 'ECHELLE', 'RETABLIR'])
    expect(gestesPermis(base({ imagePrecedente: 'x:1', repliques: { voulues: 0, pretes: 0 } }), false)).toEqual(['DEMARRER', 'ECHELLE'])
    expect(gestesPermis(base({ imagePrecedente: 'x:1', orchestrateur: 'DOCKER' }), false)).toEqual(['REDEMARRER', 'ARRETER'])
  })
  it('Docker : redémarrer et arrêter une charge non arrêtée ; démarrer une charge arrêtée, partielle ou en erreur', () => {
    expect(gestesPermis(base({ orchestrateur: 'DOCKER' }), false)).toEqual(['REDEMARRER', 'ARRETER'])
    expect(gestesPermis(base({ orchestrateur: 'DOCKER', etat: 'ARRETEE' }), false)).toEqual(['DEMARRER'])
    expect(gestesPermis(base({ orchestrateur: 'DOCKER', etat: 'EN_ERREUR', protegee: true }), false)).toEqual(['REDEMARRER', 'DEMARRER'])
  })
})

describe('service simulé', () => {
  it('charges, journaux et historique ; paramètres hors bornes refusés', async () => {
    const s = sim()
    const e = await s.charges()
    expect(e.orchestrateur).toBe('KUBERNETES')
    expect(e.charges.find((c) => c.nom === 'gateway')).toMatchObject({ protegee: true, gestes: ['REDEMARRER', 'ECHELLE', 'RETABLIR'], imagePrecedente: 'registre.gimac.local/gimac/gateway:1.3.0' })
    const j = await s.journaux('iam', 10, 'PT5M')
    expect(j.lignes).toHaveLength(10)
    expect(j.tronque).toBe(true)
    expect(j.lignes.every((l) => l.flux === 'stdout')).toBe(true)
    expect((await s.journaux('collaboration', 200, 'PT30M')).lignes).toEqual([])
    await expect(s.journaux('iam', 0, 'PT5M')).rejects.toMatchObject({ code: 'OPS_PARAMETRE_INVALIDE' })
    await expect(s.journaux('iam', 10, 'PT31M')).rejects.toMatchObject({ code: 'OPS_PARAMETRE_INVALIDE' })
    await expect(s.journaux('iam', 10, 'P1D')).rejects.toMatchObject({ code: 'OPS_PARAMETRE_INVALIDE' })
    await expect(s.journaux('inconnue', 10, 'PT10S')).rejects.toMatchObject({ status: 404, code: 'OPS_CHARGE_INCONNUE' })
    const h = await s.operations(0, 2)
    expect(h).toMatchObject({ total: 4, page: 0, taille: 2 })
    expect(h.contenu).toHaveLength(2)
    await expect(s.operations(0, 101)).rejects.toMatchObject({ code: 'OPS_PARAMETRE_INVALIDE' })
    // Docker : flux stdout et stderr distingués.
    expect((await sim({ orchestrateur: 'DOCKER' }).journaux('iam', 10, 'PT10M')).lignes.some((l) => l.flux === 'stderr')).toBe(true)
  })

  it('exploitation non configurée : liste vide, journaux et gestes refusés (OPS_NON_CONFIGURE)', async () => {
    const s = sim({ orchestrateur: 'AUCUN' })
    expect(await s.charges()).toEqual({ orchestrateur: 'AUCUN', charges: [] })
    await expect(s.journaux('iam', 10, 'PT10M')).rejects.toMatchObject({ status: 422, code: 'OPS_NON_CONFIGURE' })
    await expect(s.executer('iam', 'REDEMARRER', { motif: 'Motif valable' }, ASSERTION)).rejects.toMatchObject({ code: 'OPS_NON_CONFIGURE' })
  })

  it('refus des gestes : permission, clé de sécurité, motif, répliques, charge protégée, geste sans objet, Docker sans échelle', async () => {
    const s = sim()
    const m = { motif: 'Motif valable' }
    await expect(sim({ acteur: { ...EXECUTANT, permissions: ['PLATFORM_OPS_READ'] } }).executer('iam', 'REDEMARRER', m, ASSERTION)).rejects.toMatchObject({ status: 403 })
    await expect(s.executer('iam', 'REDEMARRER', m, '')).rejects.toMatchObject({ status: 401, code: 'REAUTH_REQUIRED' })
    await expect(s.executer('iam', 'REDEMARRER', { motif: 'abc' }, ASSERTION)).rejects.toMatchObject({ code: 'OPS_MOTIF_INVALIDE' })
    await expect(s.executer('iam', 'REDEMARRER', { motif: 'x'.repeat(501) }, ASSERTION)).rejects.toMatchObject({ code: 'OPS_MOTIF_INVALIDE' })
    await expect(s.executer('iam', 'ECHELLE', { ...m, repliques: 6 }, ASSERTION)).rejects.toMatchObject({ code: 'OPS_REPLIQUES_INVALIDES' })
    await expect(s.executer('iam', 'ECHELLE', m, ASSERTION)).rejects.toMatchObject({ code: 'OPS_REPLIQUES_INVALIDES' })
    await expect(s.executer('gateway', 'ECHELLE', { ...m, repliques: 0 }, ASSERTION)).rejects.toMatchObject({ status: 409, code: 'OPS_CHARGE_PROTEGEE' })
    await expect(s.executer('gateway', 'ARRETER', m, ASSERTION)).rejects.toMatchObject({ code: 'OPS_CHARGE_PROTEGEE' })
    await expect(s.executer('collaboration', 'ARRETER', m, ASSERTION)).rejects.toMatchObject({ code: 'OPS_GESTE_NON_PERMIS' })
    await expect(s.executer('inconnue', 'ARRETER', m, ASSERTION)).rejects.toMatchObject({ code: 'OPS_CHARGE_INCONNUE' })
    await expect(sim({ orchestrateur: 'DOCKER' }).executer('iam', 'ECHELLE', { ...m, repliques: 2 }, ASSERTION)).rejects.toMatchObject({ status: 422, code: 'OPS_NON_PRIS_EN_CHARGE' })
  })

  it('geste accepté : opération EN_COURS poussée, une seule à la fois, puis issue poussée et charge mise à jour', async () => {
    vi.useFakeTimers()
    const s = servicesConsoleSimules({ latenceMs: 0, dureeGesteMs: 100, acteur: EXECUTANT }).infrastructure
    const poussees: Operation[] = []
    const etats: string[] = []
    const fermer = s.flux({ onEtat: (e) => etats.push(e), onOperation: (o) => poussees.push(o) })
    const attendre = async <T,>(p: Promise<T>) => {
      await vi.advanceTimersByTimeAsync(1)
      return p
    }
    const op = await attendre(s.executer('iam', 'REDEMARRER', { motif: 'Prise en compte de la configuration' }, ASSERTION))
    expect(op).toMatchObject({ charge: 'iam', geste: 'REDEMARRER', resultat: 'EN_COURS', avant: 'EN_COURS' })
    expect((await attendre(s.charges())).charges.find((c) => c.nom === 'iam')).toMatchObject({ etat: 'REDEMARRAGE', gestes: [] })
    const refus = expect(s.executer('iam', 'REDEMARRER', { motif: 'Deuxième demande' }, ASSERTION)).rejects.toMatchObject({ code: 'OPS_OPERATION_EN_COURS' })
    await vi.advanceTimersByTimeAsync(1)
    await refus
    await vi.advanceTimersByTimeAsync(100)
    expect(poussees.map((o) => o.resultat)).toEqual(['EN_COURS', 'REUSSIE'])
    expect((await attendre(s.charges())).charges.find((c) => c.nom === 'iam')).toMatchObject({ etat: 'EN_COURS', redemarrages: 2 })
    // Échec simulé : redémarrer une charge en erreur ; arrêt puis démarrage ; mise à l'échelle à zéro.
    await attendre(s.executer('pilotage', 'REDEMARRER', { motif: 'Erreurs de connexion' }, ASSERTION))
    await attendre(s.executer('homologation', 'ARRETER', { motif: 'Maintenance programmée' }, ASSERTION))
    await attendre(s.executer('collaboration', 'DEMARRER', { motif: 'Fin de maintenance' }, ASSERTION))
    await attendre(s.executer('certification', 'ECHELLE', { motif: 'Montée en charge', repliques: 3 }, ASSERTION))
    await attendre(s.executer('frontend-portail', 'ECHELLE', { motif: 'Mise en veille', repliques: 0 }, ASSERTION))
    await vi.advanceTimersByTimeAsync(100)
    const fin = (await attendre(s.charges())).charges
    expect(poussees.find((o) => o.charge === 'pilotage' && o.resultat !== 'EN_COURS')).toMatchObject({ resultat: 'ECHOUEE', erreur: expect.any(String) })
    expect(fin.find((c) => c.nom === 'homologation')).toMatchObject({ etat: 'ARRETEE', repliques: { voulues: 0, pretes: 0 } })
    expect(fin.find((c) => c.nom === 'collaboration')).toMatchObject({ etat: 'EN_COURS', repliques: { voulues: 1, pretes: 1 } })
    expect(fin.find((c) => c.nom === 'certification')).toMatchObject({ repliques: { voulues: 3, pretes: 3 } })
    expect(fin.find((c) => c.nom === 'frontend-portail')).toMatchObject({ etat: 'ARRETEE' })
    expect((await attendre(s.operations(0, 20))).total).toBe(10)
    expect(etats).toEqual(['connexion', 'connecte'])
    fermer()
  })

  it('retour à la version précédente : image rétablie, la version quittée devient la précédente ; sous Docker : 422', async () => {
    const s = sim()
    await s.executer('gateway', 'RETABLIR', { motif: 'Livraison 1.4.0 fautive' }, ASSERTION)
    await new Promise((r) => setTimeout(r, 20))
    expect((await s.charges()).charges.find((c) => c.nom === 'gateway')).toMatchObject({
      image: 'registre.gimac.local/gimac/gateway:1.3.0', imagePrecedente: 'registre.gimac.local/gimac/gateway:1.4.0', gestes: ['REDEMARRER', 'ECHELLE', 'RETABLIR'],
    })
    await expect(s.executer('iam', 'RETABLIR', { motif: 'Sans version précédente' }, ASSERTION)).rejects.toMatchObject({ status: 409, code: 'OPS_GESTE_NON_PERMIS' })
    await expect(sim({ orchestrateur: 'DOCKER' }).executer('iam', 'RETABLIR', { motif: 'Motif suffisant' }, ASSERTION)).rejects.toMatchObject({ status: 422, code: 'OPS_NON_PRIS_EN_CHARGE' })
  })
})
