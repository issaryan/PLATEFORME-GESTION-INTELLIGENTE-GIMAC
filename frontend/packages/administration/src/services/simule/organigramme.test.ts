import { afterEach, describe, expect, it, vi } from 'vitest'
import { servicesConsoleSimules } from './index'

// D-102 (docs/lot1/organigramme.md) : affectations, retrait et organigramme en PDF des services simulés, mêmes règles et
// mêmes refus que AffectationsService et OrganigrammePdfService de MS-IAM.
const s = (o = {}) => servicesConsoleSimules({ latenceMs: 0, ...o })
afterEach(() => vi.restoreAllMocks())

describe('affectations simulées', () => {
  it('plusieurs personnes affectées en un geste, n+1 résolu, motif au journal', async () => {
    const x = s()
    const r = await x.organisation.affecter('POLE_SYSTEME', { comptes: ['u-sans-2fa', 'u-ingenieur', 'u-sans-2fa'], motif: '  Renfort du pôle  ' })
    // Le pôle et son service n'ont pas de chef : n+1 du chef du DES.
    expect(r).toEqual([
      { id: 'u-sans-2fa', nomAffiche: 'Bachir Hamadou Ali', avant: { unite: null, responsableId: null }, apres: { unite: 'POLE_SYSTEME', responsableId: 'u-chef-des' } },
      { id: 'u-ingenieur', nomAffiche: 'Patrick Mbida Essomba', avant: { unite: 'SNA', responsableId: 'u-chef-des' }, apres: { unite: 'POLE_SYSTEME', responsableId: 'u-chef-des' } },
    ])
    expect((await x.comptes.lire('u-sans-2fa')).unite).toBe('POLE_SYSTEME')
    const j = (await x.comptes.journal({ page: 1, taille: 10 })).contenu
    expect(j[0]).toMatchObject({ action: 'COMPTE_RATTACHEMENT_MODIFIE', cibleId: 'u-ingenieur', motif: 'Renfort du pôle' })
    expect(j[0].details).toContain('affectation=POLE_SYSTEME')
    // n+1 demandé ; la personne désignée reçoit le n+1 résolu ; null : aucun n+1.
    const d = await x.organisation.affecter('SNA', { comptes: ['u-sans-2fa', 'u-ingenieur'], responsableId: 'u-ingenieur', motif: 'Encadrement' })
    expect(d.map((y) => y.apres.responsableId)).toEqual(['u-ingenieur', 'u-chef-des'])
    expect((await x.organisation.affecter('DES', { comptes: ['u-sans-2fa'], responsableId: null, motif: 'Sans n+1' }))[0].apres).toEqual({ unite: 'DES', responsableId: null })
    // Chef de l'unité affecté chez lui : n+1 du chef de l'unité parente.
    expect((await x.organisation.affecter('DES', { comptes: ['u-chef-des'], motif: 'Rattachement du chef' }))[0].apres.responsableId).toBe('u-dg')
  })

  it('refus d’ensemble avec le détail par compte, aucune modification ; contrôles', async () => {
    const x = s()
    await expect(x.organisation.affecter('SNA', { comptes: ['u-sans-2fa', 'inconnu', 'u-correspondant', 'u-inactif'], motif: 'Renfort' })).rejects.toMatchObject({
      status: 422,
      code: 'ORG_AFFECTATION_REFUSEE',
      details: { unite: 'SNA', refus: [{ id: 'inconnu', code: 'IAM_ACCOUNT_NOT_FOUND' }, { id: 'u-correspondant', code: 'ORG_COMPTE_EXTERNE' }, { id: 'u-inactif', code: 'ORG_COMPTE_INACTIF' }] },
    })
    expect((await x.comptes.lire('u-sans-2fa')).unite).toBeNull()
    // Cycle : le DG sous un agent du DES.
    await expect(x.organisation.affecter('SNA', { comptes: ['u-dg'], responsableId: 'u-sna', motif: 'Essai' })).rejects.toMatchObject({ details: { refus: [{ id: 'u-dg', code: 'ORG_CYCLE' }] } })
    await expect(x.organisation.affecter('SNA', { comptes: ['u-sans-2fa'], motif: 'a' })).rejects.toMatchObject({ status: 400, code: 'IAM_REASON_REQUIRED' })
    await expect(x.organisation.affecter('SNA', { comptes: [], motif: 'Motif' })).rejects.toMatchObject({ code: 'ORG_AFFECTATION_INVALIDE' })
    await expect(x.organisation.affecter('SNA', { comptes: Array.from({ length: 51 }, (_, i) => `c${i}`), motif: 'Motif' })).rejects.toMatchObject({ code: 'ORG_AFFECTATION_INVALIDE' })
    await expect(x.organisation.affecter('SNA', { comptes: [' '], motif: 'Motif' })).rejects.toMatchObject({ code: 'ORG_AFFECTATION_INVALIDE' })
    await expect(x.organisation.affecter('INCONNUE', { comptes: ['u-sna'], motif: 'Motif' })).rejects.toMatchObject({ status: 404, code: 'ORG_UNITE_INTROUVABLE' })
    await x.organisation.modifierUnite('POLE_RESEAU', { code: 'POLE_RESEAU', libelle: 'Pôle réseau', type: 'POLE', parent: 'PROD_INFO', active: false }, 'Fermeture')
    await expect(x.organisation.affecter('POLE_RESEAU', { comptes: ['u-sna'], motif: 'Motif' })).rejects.toMatchObject({ status: 422, code: 'ORG_UNITE_INCONNUE' })
  })

  it('chef sans droit d’administration : unités qu’il dirige seulement', async () => {
    const chef = s({ acteur: { id: 'u-chef-des', nom: 'Christian Ndong Obiang', permissions: ['PIL_ACTIVITY_OWN'] } })
    expect(await chef.organisation.affecter('SNA', { comptes: ['u-sans-2fa'], motif: 'Renfort' })).toHaveLength(1)
    await expect(chef.organisation.affecter('DDRM', { comptes: ['u-sna'], motif: 'Renfort' })).rejects.toMatchObject({ status: 403, code: 'ORG_HORS_PERIMETRE' })
  })

  it('retrait : unité et n+1 venu de l’unité retirés ; absent, chef de l’unité, motif', async () => {
    const x = s()
    expect(await x.organisation.retirer('SNA', 'u-sna', 'Départ du service')).toEqual({
      id: 'u-sna', nomAffiche: 'Rachel Biyong Mengue', avant: { unite: 'SNA', responsableId: 'u-chef-des' }, apres: { unite: null, responsableId: null },
    })
    // n+1 qui ne vient pas de l'unité : gardé.
    await x.organisation.rattacher('u-ingenieur', { unite: 'SNA', responsableId: 'u-dg' })
    expect((await x.organisation.retirer('SNA', 'u-ingenieur', 'Départ')).apres).toEqual({ unite: null, responsableId: 'u-dg' })
    await expect(x.organisation.retirer('SNA', 'u-sna', 'Encore')).rejects.toMatchObject({ status: 409, code: 'ORG_AFFECTATION_ABSENTE' })
    await expect(x.organisation.retirer('DES', 'u-chef-des', 'Départ')).rejects.toMatchObject({ status: 409, code: 'ORG_CHEF_UNITE' })
    await expect(x.organisation.retirer('INCONNUE', 'u-sna', 'Départ')).rejects.toMatchObject({ status: 404, code: 'ORG_UNITE_INTROUVABLE' })
    await expect(x.organisation.retirer('DDRM', 'u-charge-ddrm', '')).rejects.toMatchObject({ status: 400, code: 'IAM_REASON_REQUIRED' })
  })
})

describe('organigramme en PDF simulé', () => {
  it('exemple de démonstration ; paramètres refusés ; exemple illisible : 503', async () => {
    const x = s()
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response('%PDF-1.7 démonstration', { status: 200 }))
    const b = await x.organisation.organigrammePdf({ format: 'A3', orientation: 'paysage', membres: true })
    expect(b.type).toBe('application/pdf')
    expect(await b.text()).toContain('%PDF-1.7')
    await expect(x.organisation.organigrammePdf({ format: 'A5' as 'A4', orientation: 'paysage', membres: false })).rejects.toMatchObject({ status: 400, code: 'ORG_PDF_PARAMETRE' })
    await expect(x.organisation.organigrammePdf({ format: 'A4', orientation: 'oblique' as 'portrait', membres: false })).rejects.toMatchObject({ code: 'ORG_PDF_PARAMETRE' })
    await expect(x.organisation.organigrammePdf({ racine: 'INCONNUE', format: 'A4', orientation: 'portrait', membres: false })).rejects.toMatchObject({ status: 404 })
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response('', { status: 404 }))
    await expect(x.organisation.organigrammePdf({ racine: 'DES', format: 'A4', orientation: 'portrait', membres: false })).rejects.toMatchObject({ status: 503, code: 'ORG_PDF_INDISPONIBLE' })
  })
})
