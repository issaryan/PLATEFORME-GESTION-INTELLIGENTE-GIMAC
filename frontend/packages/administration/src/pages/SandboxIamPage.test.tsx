import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import SandboxIamPage from './SandboxIamPage'
import { avec, rendrePage, simules } from '../test/rendu'
import { DELEGATIONS_DEMO, etatEffectif, paginer, prestatairesDemo, sandboxSimulee } from '../services/simule/sandbox'
import { instantDouala } from '../composants/sandboxOnglets'
import { sandboxHttp } from '../services/http/sandbox'

// D-119, US-EXI-06 : la sandbox dans la console, lectures de MS-IAM (périmètres suspendus, comptes protégés).
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-10-22T08:00:00Z'))
})
afterEach(() => {
  cleanup()
  vi.useRealTimers()
  vi.restoreAllMocks()
})
const page = (chemin = '/console/sandbox', services = simules()) => rendrePage(<SandboxIamPage />, { chemin, motif: '/console/sandbox', permissions: ['IAM_ACCOUNT_READ'], services })

describe('Sandbox GIMAC dans la console', () => {
  it('US-EXI-06 scénario 1 : le périmètre suspendu de 99001 est affiché avec son motif et son auteur', async () => {
    page()
    expect(await screen.findByRole('heading', { name: 'Sandbox GIMAC' })).toBeTruthy()
    const ligne = (await screen.findByText('99001')).closest('tr') as HTMLElement
    expect(within(ligne).getByText('Suspendu')).toBeTruthy()
    expect(within(ligne).getByText('Statut SUSPENDU au registre des participants')).toBeTruthy()
    expect(within(ligne).getByText('service-account-sbx-espace')).toBeTruthy()
    expect(screen.getByText(/sans modifier l’état des comptes/)).toBeTruthy()
  })

  it('US-EXI-06 scénario 3 : onglet des comptes protégés, tenu dans l’adresse', async () => {
    page()
    fireEvent.click(await screen.findByRole('tab', { name: 'Comptes protégés' }))
    expect(await screen.findByText('Awa Ndiaye')).toBeTruthy()
    expect(screen.getAllByText('Dernier administrateur')).toHaveLength(2)
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/sandbox?onglet=protections')
  })

  it('onglet inconnu : périmètres ; listes vides et erreurs rendues', async () => {
    page('/console/sandbox?onglet=x', avec('sandbox', { perimetres: async () => paginer([], 0, 20) }))
    expect(await screen.findByText('Aucun périmètre d’institution n’est suspendu.')).toBeTruthy()
    cleanup()
    page('/console/sandbox?onglet=protections', avec('sandbox', { protections: async () => paginer([], 0, 20) }))
    expect(await screen.findByText('Aucun compte protégé.')).toBeTruthy()
    cleanup()
    page('/console/sandbox', avec('sandbox', { perimetres: async () => { throw new ApiError(503, 'IAM_INDISPONIBLE', 'MS-IAM indisponible', 'c-1') } }))
    expect(await screen.findByText('MS-IAM indisponible')).toBeTruthy()
  })

  it('pagination : taille changée, retour à la première page', async () => {
    const lus: [number, number][] = []
    const base = simules()
    page('/console/sandbox?onglet=protections', avec('sandbox', { protections: async (p, t) => { lus.push([p, t]); return base.sandbox.protections(p, t) } }, base))
    expect(await screen.findByText('Jean Mbarga')).toBeTruthy()
    expect(lus[0]).toEqual([0, 20])
  })
})

describe('Sandbox GIMAC : listes', () => {
  it('taille de page changée, nouvel essai après une erreur, motif absent', async () => {
    let echec = true
    const base = simules()
    const appels: number[] = []
    page('/console/sandbox', avec('sandbox', {
      perimetres: async (p, t) => {
        appels.push(t)
        if (echec) throw new ApiError(503, 'IAM_INDISPONIBLE', 'MS-IAM indisponible', 'c-2')
        return paginer([{ code: '99009', depuis: '2026-10-21T07:30:00Z', acteurId: 'sa', motif: null }], p, t)
      },
    }, base))
    expect(await screen.findByText('MS-IAM indisponible')).toBeTruthy()
    echec = false
    fireEvent.click(screen.getByRole('button', { name: 'Réessayer' }))
    expect(await screen.findByText('99009')).toBeTruthy()
    expect(screen.getByText('-')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Taille de page'), { target: { value: '50' } })
    await waitFor(() => expect(appels).toContain(50))
  })
})

describe('Sandbox GIMAC : mandats de partenaire (US-EXI-04)', () => {
  it('US-EXI-04 scénarios 1 à 3 : mandat actif avec ses droits et ses dates, programmé, échu à la date de fin, révoqué', async () => {
    page('/console/sandbox?onglet=mandats')
    const actif = (await screen.findAllByText('Campagnes de certification : exécuter les campagnes'))[0].closest('tr') as HTMLElement
    expect(within(actif).getByText('Active')).toBeTruthy()
    expect(within(actif).getByText('Paul Ebode (Intégrateur Afrique)')).toBeTruthy()
    expect(within(actif).getByText('99002')).toBeTruthy()
    expect(screen.getByText('Programmée')).toBeTruthy()
    expect(screen.getByText('Échue')).toBeTruthy()
    expect(screen.getByText('Révoquée')).toBeTruthy()
    expect(screen.getByText(/l’accès cesse à la date de fin/)).toBeTruthy()
  })

  it('l’échéance suit l’horloge : le mandat actif passe échu à sa date de fin', async () => {
    vi.setSystemTime(new Date('2026-12-31T18:00:00Z'))
    page('/console/sandbox?onglet=mandats')
    expect(await screen.findAllByText('Échue')).toHaveLength(3)
    expect(screen.queryByText('Active')).toBeNull()
  })
})

describe('Sandbox GIMAC : services', () => {
  it('état effectif simulé, comme MS-IAM', () => {
    const d = DELEGATIONS_DEMO[0][0]
    expect(etatEffectif({ ...d, etat: 'DEMANDEE' }, new Date('2026-10-22T08:00:00Z'))).toBe('DEMANDEE')
    expect(etatEffectif({ ...d, etat: 'DEMANDEE' }, new Date('2027-01-01T00:00:00Z'))).toBe('ECHUE')
    expect(etatEffectif({ ...d, etat: 'REFUSEE' })).toBe('REFUSEE')
    expect(etatEffectif(d, new Date('2026-09-01T00:00:00Z'))).toBe('PROGRAMMEE')
  })

  it('simulés : pages de 100 au plus, code invalide refusé, état d’un périmètre', async () => {
    const s = simules().sandbox
    expect(paginer([1, 2, 3], -1, 500)).toEqual({ contenu: [1, 2, 3], total: 3, page: 0, taille: 100 })
    expect(paginer([1, 2, 3], 1, 0)).toEqual({ contenu: [2], total: 3, page: 1, taille: 1 })
    expect((await s.perimetre('99001')).suspendu).toBe(true)
    expect(await s.perimetre('99002')).toEqual({ code: '99002', suspendu: false, depuis: null, sessionsRevoquees: 0, rejouee: false })
    await expect(s.perimetre('99 001')).rejects.toMatchObject({ code: 'IAM_SBX_PERIMETRE_INVALIDE' })
    const tri = sandboxSimulee({ attendre: async (v) => (typeof v === 'function' ? (v as () => unknown)() : v) as never, refus: () => new Error('x') as never }, [
      { code: 'A', depuis: '2026-10-01T00:00:00Z', acteurId: 's', motif: null },
      { code: 'B', depuis: '2026-10-02T00:00:00Z', acteurId: 's', motif: null },
    ])
    expect((await tri.perimetres(0, 20)).contenu.map((p) => p.code)).toEqual(['B', 'A'])
  })

  it('HTTP : /bff/console/iam/sandbox, pages à partir de 0', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }))
    const s = sandboxHttp()
    await s.perimetres(0, 20)
    await s.perimetre('99/01')
    await s.protections(1, 10)
    await s.delegations('MANDAT', 0, 20)
    await s.delegations(undefined, 2, 5)
    await waitFor(() => expect(spy).toHaveBeenCalledTimes(5))
    expect(spy.mock.calls.map(([u, i]) => `${(i as RequestInit).method} ${new URL(String(u)).pathname}${new URL(String(u)).search}`)).toEqual([
      'GET /bff/console/iam/sandbox/perimetres?page=0&taille=20',
      'GET /bff/console/iam/sandbox/perimetres/99%2F01',
      'GET /bff/console/iam/sandbox/protections?page=1&taille=10',
      'GET /bff/console/iam/sandbox/delegations?type=MANDAT&page=0&taille=20',
      'GET /bff/console/iam/sandbox/delegations?page=2&taille=5',
    ])
    expect((await simules().sandbox.delegations(undefined, 0, 20)).total).toBe(7)
  })
})

describe('Sandbox GIMAC : accès de prestataire (US-EXI-24)', () => {
  const RSSI_PERMS = ['IAM_ACCOUNT_READ', 'IAM_ACCOUNT_SECURITY']
  const pagePrestataires = (permissions = RSSI_PERMS, services = simules()) =>
    rendrePage(<SandboxIamPage />, { chemin: '/console/sandbox?onglet=prestataires', motif: '/console/sandbox', permissions, services })

  it('scénario 1 : demande sur SBX-TRACE approuvée pour quatre heures ; demande de la session refusée à son auteur', async () => {
    pagePrestataires()
    const demande = (await screen.findByText('Montée de version de SBX-TRACE')).closest('tr') as HTMLElement
    expect(within(demande).getByText('Demandée')).toBeTruthy()
    expect(within(demande).getByText('SBX-TRACE')).toBeTruthy()
    fireEvent.click(within(demande).getByRole('button', { name: 'Approuver l’accès' }))
    const dialogue = await screen.findByRole('alertdialog')
    expect(within(dialogue).getByText(/inscrit au registre d’audit/)).toBeTruthy()
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Approuver l’accès' }))
    expect(await screen.findByText('Accès de prestataire approuvé.')).toBeTruthy()
    await waitFor(() => expect(within((screen.getByText('Montée de version de SBX-TRACE')).closest('tr') as HTMLElement).getByText('Programmée')).toBeTruthy())

    // Nouvelle demande depuis la console : quatre heures sur SBX-TRACE, puis approbation refusée à son auteur.
    fireEvent.click(screen.getByRole('button', { name: 'Demander un accès de prestataire' }))
    const d = await screen.findByRole('dialog')
    expect(within(d).getByRole('button', { name: 'Envoyer la demande' }).hasAttribute('disabled')).toBe(true)
    fireEvent.change(within(d).getByLabelText(/Compte du prestataire/), { target: { value: 'presta.maintenance' } })
    fireEvent.click(within(d).getByLabelText('SBX-TRACE'))
    const debut = within(d).getByLabelText(/Début/)
    fireEvent.change(debut, { target: { value: '22/10/2026' } })
    fireEvent.change(within(d).getByLabelText('Heure'), { target: { value: '10:00' } })
    fireEvent.change(within(d).getByLabelText(/Motif de l’intervention/), { target: { value: 'Correctif de SBX-TRACE' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Envoyer la demande' }))
    expect(await screen.findByText('Demande d’accès enregistrée : elle attend l’approbation du RSSI.')).toBeTruthy()
    const nouvelle = (await screen.findByText('Correctif de SBX-TRACE')).closest('tr') as HTMLElement
    fireEvent.click(within(nouvelle).getByRole('button', { name: 'Approuver l’accès' }))
    fireEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Approuver l’accès' }))
    expect(await screen.findByText(/L’auteur de la demande ne l’approuve pas/)).toBeTruthy()
  })

  it('scénario 2 : accès échu à sa date de fin ; refus motivé ; révocation d’un accès actif', async () => {
    pagePrestataires()
    const echu = (await screen.findByText('Diagnostic de la façade ISO 20022')).closest('tr') as HTMLElement
    expect(within(echu).getByText('Échue')).toBeTruthy()
    expect(within(echu).queryAllByRole('button')).toHaveLength(0)
    const actif = screen.getByText('Correctif du connecteur Mifos').closest('tr') as HTMLElement
    expect(within(actif).getByText('Active')).toBeTruthy()
    fireEvent.click(within(actif).getByRole('button', { name: 'Révoquer' }))
    let dialogue = await screen.findByRole('alertdialog')
    fireEvent.change(within(dialogue).getByRole('textbox'), { target: { value: 'Intervention terminée plus tôt' } })
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Révoquer' }))
    expect(await screen.findByText('Accès de prestataire révoqué.')).toBeTruthy()
    await waitFor(() => expect(within(screen.getByText('Intervention terminée plus tôt').closest('tr') as HTMLElement).getByText('Révoquée')).toBeTruthy())
    const demande = screen.getByText('Montée de version de SBX-TRACE').closest('tr') as HTMLElement
    fireEvent.click(within(demande).getByRole('button', { name: 'Refuser' }))
    dialogue = await screen.findByRole('alertdialog')
    fireEvent.change(within(dialogue).getByRole('textbox'), { target: { value: 'Fenêtre de maintenance non validée' } })
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Refuser' }))
    expect(await screen.findByText('Demande d’accès refusée.')).toBeTruthy()
  })

  it('lecture seule sans IAM_ACCOUNT_SECURITY ; liste vide', async () => {
    pagePrestataires(['IAM_ACCOUNT_READ'])
    expect(await screen.findByText('Montée de version de SBX-TRACE')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Demander un accès de prestataire' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Approuver l’accès' })).toBeNull()
    cleanup()
    pagePrestataires(RSSI_PERMS, avec('sandbox', { delegations: async () => paginer([], 0, 20) }))
    expect(await screen.findByText('Aucun accès de prestataire.')).toBeTruthy()
  })

  it('services simulés : mêmes contrôles que MS-IAM ; saisie de l’heure de Douala', async () => {
    const s = sandboxSimulee({ attendre: async (v) => (typeof v === 'function' ? (v as () => unknown)() : v) as never, refus: (status, code) => Object.assign(new Error(code), { status, code }) as never }, [], [], prestatairesDemo())
    const d = { compteId: 'p', composants: ['SBX-TRACE'], debut: '2026-10-22T09:00:00Z', fin: '2026-10-22T13:00:00Z', motif: 'm' }
    await expect(s.demanderPrestataire({ ...d, composants: [] })).rejects.toMatchObject({ code: 'IAM_SBX_COMPOSANT_REQUIS' })
    await expect(s.demanderPrestataire({ ...d, composants: ['X'] })).rejects.toMatchObject({ code: 'IAM_SBX_COMPOSANT_INCONNU' })
    await expect(s.demanderPrestataire({ ...d, fin: d.debut })).rejects.toMatchObject({ code: 'IAM_SBX_PERIODE_INVALIDE' })
    await expect(s.demanderPrestataire({ ...d, debut: '2026-10-21T00:00:00Z', fin: '2026-10-21T01:00:00Z' })).rejects.toMatchObject({ code: 'IAM_SBX_PERIODE_ECHUE' })
    await expect(s.demanderPrestataire({ ...d, fin: '2026-10-22T18:00:00Z' })).rejects.toMatchObject({ code: 'IAM_SBX_DUREE_EXCESSIVE' })
    await expect(s.approuverPrestataire('inconnue')).rejects.toMatchObject({ code: 'IAM_SBX_DELEGATION_INTROUVABLE' })
    await expect(s.approuverPrestataire('d-presta-actif')).rejects.toMatchObject({ code: 'IAM_SBX_DELEGATION_CLOSE' })
    expect((await s.revoquerPrestataire('d-presta-actif', 'x')).etat).toBe('REVOQUEE')
    expect((await s.revoquerPrestataire('d-presta-actif', 'y')).delegation.motif).toBe('x')
    expect(instantDouala('22/10/2026', '10:00')).toBe('2026-10-22T09:00:00Z')
    expect(instantDouala('22/10/2026', '1000')).toBeNull()
    expect(instantDouala('31/02/2026x', '10:00')).toBeNull()
    expect(instantDouala('99/99/2026', '10:00')).toBeNull()
  })

  it('saisie de la demande : chaque manque est expliqué ; refus du service affiché ; accès sans motif ; approbation en échec ; fermeture', async () => {
    pagePrestataires(RSSI_PERMS, avec('sandbox', {
      demanderPrestataire: async () => { throw new ApiError(422, 'IAM_SBX_PRESTATAIRE_INSTITUTION', 'Compte d’une institution', 'c-presta') },
      approuverPrestataire: async () => { throw new ApiError(409, 'IAM_SBX_DELEGATION_CLOSE', 'Demande déjà décidée', 'c-appr') },
      delegations: async (type, p, t) => {
        const r = await simules().sandbox.delegations(type, p, t)
        return { ...r, contenu: r.contenu.map((v) => (v.delegation.motif === 'Diagnostic de la façade ISO 20022' ? { ...v, delegation: { ...v.delegation, motif: null } } : v)) }
      },
    }))
    expect(await screen.findByText('Montée de version de SBX-TRACE')).toBeTruthy()
    expect(screen.queryByText('Diagnostic de la façade ISO 20022')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Demander un accès de prestataire' }))
    const d = await screen.findByRole('dialog')
    const envoi = () => within(d).getByRole('button', { name: 'Envoyer la demande' })
    expect(envoi().getAttribute('title')).toBe('Indiquez le compte du prestataire.')
    fireEvent.change(within(d).getByLabelText(/Compte du prestataire/), { target: { value: 'presta.maintenance' } })
    expect(envoi().getAttribute('title')).toBe('Choisissez au moins un composant.')
    fireEvent.click(within(d).getByLabelText('SBX-TRACE'))
    expect(envoi().getAttribute('title')).toBe('Indiquez la date (JJ/MM/AAAA) et l’heure (HH:MM) de début.')
    fireEvent.change(within(d).getByLabelText(/Début/), { target: { value: '22/10/2026' } })
    fireEvent.change(within(d).getByLabelText('Heure'), { target: { value: '10:00' } })
    fireEvent.change(within(d).getByLabelText(/Durée/), { target: { value: '9' } })
    expect(envoi().getAttribute('title')).toBe('La durée va de une à huit heures.')
    fireEvent.change(within(d).getByLabelText(/Durée/), { target: { value: '2' } })
    expect(envoi().getAttribute('title')).toBe('Indiquez le motif de l’intervention.')
    fireEvent.change(within(d).getByLabelText(/Motif de l’intervention/), { target: { value: 'Correctif' } })
    fireEvent.click(envoi())
    expect(await within(d).findByText(/c-presta/)).toBeTruthy()
    fireEvent.click(within(d).getAllByRole('button', { name: /Annuler|Fermer/ })[0])
    fireEvent.click(await screen.findByRole('button', { name: 'Abandonner les modifications' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    const ligne = screen.getByText('Montée de version de SBX-TRACE').closest('tr') as HTMLElement
    fireEvent.click(within(ligne).getByRole('button', { name: 'Approuver l’accès' }))
    fireEvent.click(within(await screen.findByRole('alertdialog')).getAllByRole('button', { name: /Annuler|Fermer/ })[0])
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull())
    fireEvent.click(within(ligne).getByRole('button', { name: 'Approuver l’accès' }))
    fireEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Approuver l’accès' }))
    expect(await screen.findByText(/c-appr/)).toBeTruthy()
  })

  it('HTTP : demande, approbation, refus et révocation sous /bff/console/iam/sandbox/delegations', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }))
    const s = sandboxHttp()
    await s.demanderPrestataire({ compteId: 'p', composants: ['SBX-TRACE'], debut: 'a', fin: 'b', motif: 'm' })
    await s.approuverPrestataire('d/1')
    await s.refuserPrestataire('d1', 'non')
    await s.revoquerPrestataire('d1', 'fin')
    await waitFor(() => expect(spy).toHaveBeenCalledTimes(4))
    expect(spy.mock.calls.map(([u, i]) => `${(i as RequestInit).method} ${new URL(String(u)).pathname}`)).toEqual([
      'POST /bff/console/iam/sandbox/delegations/prestataire',
      'POST /bff/console/iam/sandbox/delegations/d%2F1/approbation',
      'POST /bff/console/iam/sandbox/delegations/d1/refus',
      'POST /bff/console/iam/sandbox/delegations/d1/revocation',
    ])
  })
})

describe('Sandbox GIMAC : registre d’audit (US-EXI-10)', () => {
  const RSSI_PERMS = ['IAM_ACCOUNT_READ', 'SBX_TRA_CONSULTATION']
  const pageRegistre = (permissions = RSSI_PERMS, services = simules()) =>
    rendrePage(<SandboxIamPage />, { chemin: '/console/sandbox?onglet=registre', motif: '/console/sandbox', permissions, services })

  it('scénario 2 : filtre par institution 99001, événements de la sandbox seuls, export au format ouvert', async () => {
    const urls: string[] = []
    Object.defineProperty(URL, 'createObjectURL', { value: () => 'blob:x', configurable: true })
    Object.defineProperty(URL, 'revokeObjectURL', { value: () => urls.push('revoque'), configurable: true })
    const base = simules()
    const filtres: (string | undefined)[] = []
    pageRegistre(RSSI_PERMS, avec('sandbox', { exporterRegistre: async (i) => { filtres.push(i); return base.sandbox.exporterRegistre(i) } }, base))
    expect(await screen.findByText('sbx-lecture.access-recorded.v1')).toBeTruthy()
    expect(screen.getAllByText('99002').length).toBeGreaterThan(0)
    fireEvent.change(screen.getByLabelText(/^Institution/), { target: { value: '99001' } })
    fireEvent.click(screen.getByRole('button', { name: 'Filtrer' }))
    await waitFor(() => expect(screen.queryAllByText('99002')).toHaveLength(0))
    expect(screen.getAllByText('99001').length).toBe(7)
    fireEvent.click(screen.getByRole('button', { name: 'Exporter (CSV)' }))
    expect(await screen.findByText('Export du registre téléchargé (CSV, UTF-8).')).toBeTruthy()
    expect(filtres).toEqual(['99001'])
    const csv = await base.sandbox.exporterRegistre('99001')
    expect(csv.type).toBe('text/csv;charset=utf-8')
    expect(csv.size).toBeGreaterThan(500)
    fireEvent.change(screen.getByLabelText(/^Institution/), { target: { value: '99999' } })
    fireEvent.click(screen.getByRole('button', { name: 'Filtrer' }))
    expect(await screen.findByText('Aucun événement de la sandbox pour l’institution 99999.')).toBeTruthy()
  })

  it('scénario 3 : rebut des enveloppes non conformes avec leur motif ; alerte au-dessus du seuil ; erreur d’export', async () => {
    pageRegistre()
    expect(await screen.findByText('Horodatage invalide')).toBeTruthy()
    expect(screen.getByText('JSON illisible')).toBeTruthy()
    expect(screen.queryByText(/Rebut au-dessus du seuil/)).toBeNull()
    cleanup()
    const base = simules()
    pageRegistre(RSSI_PERMS, avec('sandbox', {
      rebut: async () => ({ ...paginer([], 0, 20), total: 12, seuil: 10, alerte: true }),
      exporterRegistre: async () => { throw new ApiError(503, 'AUD_INDISPONIBLE', 'MS-AUDIT indisponible', 'c-aud') },
    }, base))
    expect(await screen.findByText('Rebut au-dessus du seuil : 12 enveloppes écartées pour un seuil de 10.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Exporter (CSV)' }))
    expect(await screen.findByText('MS-AUDIT indisponible')).toBeTruthy()
  })

  it('export sans filtre (fichier de toutes les institutions), lien révoqué ; navigateur sans URL.createObjectURL ; rebut rechargé et repaginé', async () => {
    const revoques: string[] = []
    Object.defineProperty(URL, 'createObjectURL', { value: () => 'blob:tout', configurable: true })
    Object.defineProperty(URL, 'revokeObjectURL', { value: (u: string) => revoques.push(u), configurable: true })
    const origine = window.setTimeout
    vi.spyOn(window, 'setTimeout').mockImplementation(((f: () => void, ms?: number) => (ms === 10_000 ? (f(), 0) : origine(f, ms))) as typeof window.setTimeout)
    let echec = true
    const tailles: number[] = []
    const base = simules()
    pageRegistre(RSSI_PERMS, avec('sandbox', {
      rebut: async (p, t) => {
        tailles.push(t)
        if (echec) throw new ApiError(503, 'AUD_INDISPONIBLE', 'MS-AUDIT indisponible', 'c-reb')
        return base.sandbox.rebut(p, t)
      },
    }, base))
    expect(await screen.findByText(/c-reb/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Exporter (CSV)' }))
    expect(await screen.findByText('Export du registre téléchargé (CSV, UTF-8).')).toBeTruthy()
    expect(revoques).toEqual(['blob:tout'])
    const ecrire = vi.fn(async () => { throw new Error('refus') })
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: ecrire }, configurable: true })
    fireEvent.click(screen.getAllByRole('button', { name: /copier/i })[0])
    await waitFor(() => expect(ecrire).toHaveBeenCalled())
    echec = false
    fireEvent.click(screen.getByRole('button', { name: 'Réessayer' }))
    expect(await screen.findByText('Horodatage invalide')).toBeTruthy()
    const tailleRebut = screen.getAllByLabelText('Taille de page').at(-1) as HTMLElement
    fireEvent.change(tailleRebut, { target: { value: '50' } })
    await waitFor(() => expect(tailles).toContain(50))
    vi.mocked(window.setTimeout).mockRestore()
    Object.defineProperty(URL, 'createObjectURL', { value: undefined, configurable: true })
    fireEvent.click(screen.getByRole('button', { name: 'Exporter (CSV)' }))
    await waitFor(() => expect(screen.getAllByText('Export du registre téléchargé (CSV, UTF-8).').length).toBeGreaterThan(0))
    expect(revoques).toHaveLength(1)
  })

  it('onglet absent sans SBX_TRA_CONSULTATION ; HTTP vers /bff/console/audit/sandbox', async () => {
    rendrePage(<SandboxIamPage />, { chemin: '/console/sandbox?onglet=registre', motif: '/console/sandbox', permissions: ['IAM_ACCOUNT_READ'], services: simules() })
    expect(await screen.findByRole('tab', { name: 'Périmètres suspendus' })).toBeTruthy()
    expect(screen.queryByRole('tab', { name: 'Registre d’audit' })).toBeNull()
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }))
    const s = sandboxHttp()
    await s.registre('99001', 0, 20)
    await s.registre(undefined, 1, 10)
    await s.rebut(0, 20)
    await s.exporterRegistre('99 001')
    await s.exporterRegistre(undefined)
    await waitFor(() => expect(spy).toHaveBeenCalledTimes(5))
    expect(spy.mock.calls.map(([u]) => { const x = new URL(String(u)); return `${x.pathname}${x.search}` })).toEqual([
      '/bff/console/audit/sandbox?institution=99001&page=0&taille=20',
      '/bff/console/audit/sandbox?page=1&taille=10',
      '/bff/console/audit/sandbox/rebut?page=0&taille=20',
      '/bff/console/audit/sandbox/export?institution=99%20001',
      '/bff/console/audit/sandbox/export',
    ])
  })
})
