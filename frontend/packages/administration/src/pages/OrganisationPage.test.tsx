import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import OrganisationPage from './OrganisationPage'
import ComptePage from './ComptePage'
import { LECTEUR, RSSI, avec, rendrePage, simules } from '../test/rendu'
import { organisationHttp } from '../services/http/organisation'

// Organisation du GIMAC (D-76) : catalogue des unités en arbre, création, modification et désactivation en
// fenêtre modale ; rattachement d'un compte (unité, responsable) avec les refus ORG_* expliqués.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
const organisation = (o: Partial<Parameters<typeof rendrePage>[1]> = {}) =>
  rendrePage(<OrganisationPage />, { chemin: '/des/administration/organisation', motif: '/des/administration/organisation', permissions: RSSI, ...o })
const fiche = (id: string, o: Partial<Parameters<typeof rendrePage>[1]> = {}) =>
  rendrePage(<ComptePage />, { chemin: `/des/administration/comptes/${id}`, motif: '/des/administration/comptes/:id', permissions: RSSI, ...o })

describe('Organisation : catalogue des unités', () => {
  it('arbre des unités, fiche d’une unité, création validée puis refusée (code existant) puis acceptée', async () => {
    organisation()
    const arbre = await screen.findByRole('tree', { name: 'Unités organisationnelles' })
    expect(within(arbre).getByText('Pôle réseau')).toBeTruthy()
    fireEvent.click(within(arbre).getByText('Service Production informatique'))
    expect(await screen.findByText('Sous-unités')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Nouvelle unité' }))
    const d = await screen.findByRole('dialog', { name: 'Nouvelle unité' })
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: '1x' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l’unité' }))
    expect(await within(d).findByText(/Code attendu/)).toBeTruthy()
    expect(within(d).getByText('Indiquez le libellé de l’unité.')).toBeTruthy()
    expect(within(d).getByText('Choisissez l’unité parente.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: 'pole_reseau' } })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Cellule réseau bis' } })
    fireEvent.change(within(d).getByLabelText('Type'), { target: { value: 'CELLULE' } })
    fireEvent.change(within(d).getByLabelText(/^Unité parente/), { target: { value: 'PROD_INFO' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l’unité' }))
    expect(await within(d).findByText('Une unité porte déjà ce code.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: 'CELLULE_SUPERVISION' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l’unité' }))
    expect(await screen.findByText('Unité « Cellule réseau bis » créée.')).toBeTruthy()
    expect(await within(arbre).findByText('Cellule réseau bis')).toBeTruthy()
  })

  it('modification et désactivation ; refus avec des sous-unités actives ; lecture seule sans IAM_ACCOUNT_EDIT', async () => {
    organisation()
    const arbre = await screen.findByRole('tree')
    fireEvent.click(within(arbre).getByText('Service Production informatique'))
    fireEvent.click(await screen.findByRole('button', { name: 'Désactiver l’unité' }))
    const c = await screen.findByRole('alertdialog', { name: 'Désactiver l’unité' })
    fireEvent.click(within(c).getByRole('button', { name: 'Désactiver l’unité' }))
    expect(await screen.findByText('Désactivez d’abord les sous-unités actives de cette unité.')).toBeTruthy()
    fireEvent.click(within(arbre).getByText('Pôle système'))
    fireEvent.click(await screen.findByRole('button', { name: 'Modifier l’unité' }))
    const m = await screen.findByRole('dialog', { name: 'Modifier l’unité' })
    fireEvent.change(within(m).getByLabelText(/^Libellé/), { target: { value: 'Pôle systèmes et stockage' } })
    fireEvent.click(within(m).getByRole('button', { name: 'Enregistrer l’unité' }))
    expect(await screen.findByText('Unité « Pôle systèmes et stockage » modifiée.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Désactiver l’unité' }))
    fireEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Désactiver l’unité' }))
    expect(await screen.findByText('Unité « Pôle systèmes et stockage » désactivée.')).toBeTruthy()
    expect(await within(arbre).findByText('inactive')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Réactiver l’unité' }))
    fireEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Réactiver l’unité' }))
    expect(await screen.findByText('Unité « Pôle systèmes et stockage » réactivée.')).toBeTruthy()
    cleanup()
    organisation({ permissions: LECTEUR })
    fireEvent.click(within(await screen.findByRole('tree')).getByText('Conformité'))
    expect(await screen.findByText('Sous-unités')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Nouvelle unité' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Modifier l’unité' })).toBeNull()
  })

  it('direction sans parent ; refus inattendu du service affiché dans la fenêtre', async () => {
    organisation({ services: avec('organisation', { creerUnite: () => Promise.reject(new ApiError(503, 'HTTP_503', 'Service indisponible', 'cid-org')) }) })
    fireEvent.click(await screen.findByRole('button', { name: 'Nouvelle unité' }))
    const d = await screen.findByRole('dialog', { name: 'Nouvelle unité' })
    fireEvent.change(within(d).getByLabelText('Type'), { target: { value: 'DIRECTION' } })
    expect(within(d).queryByLabelText(/^Unité parente/)).toBeNull()
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: 'DGA' } })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Direction générale adjointe' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l’unité' }))
    expect(await within(d).findByText('Service indisponible')).toBeTruthy()
  })
})

describe('Fiche d’un compte : rattachement', () => {
  it('lecture de l’unité et du responsable ; modification ; boucle hiérarchique refusée (ORG_CYCLE)', async () => {
    fiche('u-chef-des')
    expect(await screen.findByRole('heading', { name: 'Rattachement' })).toBeTruthy()
    expect(await screen.findByText('Département Exploitation et Supervision (Direction Générale)')).toBeTruthy()
    expect(await screen.findByText('François Tchoumi Kamga (f.tchoumi)')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Modifier le rattachement' }))
    const d = await screen.findByRole('dialog', { name: 'Modifier le rattachement' })
    // Le chef du DES ne peut avoir pour responsable un compte dont il est lui-même le responsable.
    fireEvent.click(await within(d).findByRole('button', { name: /Retirer|François Tchoumi Kamga/ }))
    fireEvent.change(within(d).getByRole('combobox', { name: /responsable/i }), { target: { value: 'mbida' } })
    fireEvent.mouseDown(await within(d).findByRole('option', { name: /Patrick Mbida Essomba/ }))
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le rattachement' }))
    expect(await within(d).findByText(/Boucle hiérarchique/)).toBeTruthy()
    fireEvent.change(within(d).getByRole('combobox', { name: /responsable/i }), { target: { value: 'tchoumi' } })
    fireEvent.mouseDown(await within(d).findByRole('option', { name: /François Tchoumi Kamga/ }))
    fireEvent.change(within(d).getByLabelText('Unité'), { target: { value: 'SNA' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le rattachement' }))
    expect(await screen.findByText('Le rattachement du compte a été enregistré.')).toBeTruthy()
    expect(await screen.findByText('Service Normes et Agréments (Département Exploitation et Supervision)')).toBeTruthy()
  })

  it('compte sans rattachement ; unité inactive refusée (ORG_UNITE_INCONNUE) ; compte d’institution ; lecture seule', async () => {
    fiche('u-sans-2fa', { services: avec('organisation', { rattacher: () => Promise.reject(new ApiError(422, 'ORG_UNITE_INCONNUE', 'x', 'cid-u')) }) })
    expect(await screen.findByText('aucune unité')).toBeTruthy()
    expect(screen.getByText('aucun responsable')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Modifier le rattachement' }))
    const d = await screen.findByRole('dialog', { name: 'Modifier le rattachement' })
    await waitFor(() => expect(within(within(d).getByLabelText('Unité')).getAllByRole('option').length).toBeGreaterThan(5))
    fireEvent.change(within(d).getByLabelText('Unité'), { target: { value: 'POLE_SYSTEME' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le rattachement' }))
    expect(await within(d).findByText('Unité inconnue ou inactive.')).toBeTruthy()
    cleanup()
    fiche('u-correspondant')
    expect(await screen.findByText('Compte d’institution : ni unité ni responsable.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Modifier le rattachement' })).toBeNull()
    cleanup()
    fiche('u-chef-des', { permissions: LECTEUR })
    expect(await screen.findByRole('heading', { name: 'Rattachement' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Modifier le rattachement' })).toBeNull()
  })
})

describe('organisation simulée et HTTP', () => {
  it('règles de MS-IAM imitées : unités, rattachement, refus ORG_*', async () => {
    const s = simules().organisation
    const code = (p: Promise<unknown>) => p.then(() => 'ok', (e: ApiError) => `${e.status} ${e.code}`)
    expect(await code(s.creerUnite({ code: 'X1', libelle: ' ', type: 'SERVICE', parent: 'DES', active: true }))).toBe('400 ORG_UNITE_INVALIDE')
    expect(await code(s.creerUnite({ code: 'DG2', libelle: 'DG bis', type: 'DIRECTION', parent: 'DG', active: true }))).toBe('400 ORG_UNITE_INVALIDE')
    expect(await code(s.creerUnite({ code: 'C1', libelle: 'Cellule', type: 'CELLULE', parent: 'INCONNUE', active: true }))).toBe('422 ORG_UNITE_INCONNUE')
    expect(await code(s.modifierUnite('DES', { code: 'DES', libelle: 'DES', type: 'DEPARTEMENT', parent: 'POLE_RESEAU', active: true }))).toBe('409 ORG_CYCLE')
    expect(await code(s.modifierUnite('NUL', { code: 'NUL', libelle: 'x', type: 'SERVICE', parent: 'DES', active: true }))).toBe('404 ORG_UNITE_INTROUVABLE')
    expect(await code(s.rattacher('u-correspondant', { unite: 'DES', responsableId: null }))).toBe('422 ORG_COMPTE_EXTERNE')
    expect(await code(s.rattacher('u-sna', { unite: null, responsableId: 'u-sna' }))).toBe('422 ORG_RESPONSABLE_INVALIDE')
    expect(await code(s.rattacher('u-sna', { unite: null, responsableId: 'u-inactif' }))).toBe('422 ORG_RESPONSABLE_INVALIDE')
    expect(await code(s.rattacher('u-dg', { unite: 'DG', responsableId: 'u-sna' }))).toBe('409 ORG_CYCLE')
    expect(await s.rattacher('u-sna', { unite: ' ', responsableId: null })).toMatchObject({ unite: null, responsableId: null })
  })
  it('chemins exacts : /bff/{des,console}/iam/organisation/unites et /comptes/{id}/rattachement', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }))
    const u = { code: 'POLE_RESEAU', libelle: 'Pôle réseau', type: 'POLE' as const, parent: 'PROD_INFO', active: true }
    await organisationHttp('des').unites()
    await organisationHttp('des').creerUnite(u)
    await organisationHttp('console').modifierUnite('POLE_RESEAU', u)
    await organisationHttp('des').rattacher('u 1', { unite: 'POLE_RESEAU', responsableId: null })
    expect(spy.mock.calls.map(([x, i]) => `${(i as RequestInit).method} ${new URL(String(x)).pathname}`)).toEqual([
      'GET /bff/des/iam/organisation/unites',
      'POST /bff/des/iam/organisation/unites',
      'PUT /bff/console/iam/organisation/unites/POLE_RESEAU',
      'PUT /bff/des/iam/comptes/u%201/rattachement',
    ])
    expect(JSON.parse(String((spy.mock.calls[3][1] as RequestInit).body))).toEqual({ unite: 'POLE_RESEAU', responsableId: null })
  })
})
