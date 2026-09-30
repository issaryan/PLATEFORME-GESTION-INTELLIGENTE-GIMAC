import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import ComptesPage from './ComptesPage'
import CreationComptePage from './CreationComptePage'
import JournalPage from './JournalPage'
import { LECTEUR, RSSI, SUPER_ADMIN, avec, rendrePage, simules } from '../test/rendu'
import type { FiltreComptes, NouveauCompte } from '../modele/comptes'

afterEach(cleanup)
const liste = (permissions = SUPER_ADMIN, o: Partial<Parameters<typeof rendrePage>[1]> = {}) =>
  rendrePage(<ComptesPage />, { chemin: '/console/comptes', motif: '/console/comptes', permissions, ...o })
const creation = (permissions = SUPER_ADMIN, o: Partial<Parameters<typeof rendrePage>[1]> = {}) =>
  rendrePage(<CreationComptePage />, { chemin: '/console/comptes/nouveau', motif: '/console/comptes/nouveau', permissions, ...o })

describe('Comptes : liste', () => {
  it('liste, recherche, filtres de profil et d’état acceptés par MS-IAM, pagination, ouverture', async () => {
    const filtres: FiltreComptes[] = []
    const base = simules()
    const services = avec('comptes', { lister: async (f) => { filtres.push(f); return base.comptes.lister(f) } }, base)
    const { container } = liste(SUPER_ADMIN, { services })
    expect(await screen.findByText('super.admin')).toBeTruthy()
    expect(screen.getByText('13 compte(s)')).toBeTruthy()
    expect(screen.getAllByText('sans second facteur').length).toBeGreaterThan(0)
    fireEvent.change(container.querySelector('input[type="search"]')!, { target: { value: 'ndong' } })
    await waitFor(() => expect(filtres.at(-1)).toMatchObject({ q: 'ndong', page: 1 }))
    expect(await screen.findByText('c.ndong')).toBeTruthy()
    fireEvent.change(screen.getByRole('combobox', { name: 'État' }), { target: { value: 'SANS_2FA' } })
    await waitFor(() => expect(filtres.at(-1)).toMatchObject({ etat: 'SANS_2FA' }))
    await screen.findByRole('option', { name: 'Chef de la DDRM' })
    fireEvent.change(screen.getByRole('combobox', { name: 'Profil' }), { target: { value: 'ROLE_RSSI' } })
    await waitFor(() => expect(filtres.at(-1)).toMatchObject({ profil: 'ROLE_RSSI', etat: 'SANS_2FA', q: 'ndong' }))
    expect(await screen.findByText('Aucun compte ne correspond')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Réinitialiser' }))
    await waitFor(() => expect(filtres.at(-1)).toEqual({ page: 1, taille: 25 }))
    fireEvent.change(await screen.findByRole('combobox', { name: 'Taille de page' }), { target: { value: '50' } })
    await waitFor(() => expect(filtres.at(-1)).toMatchObject({ taille: 50 }))
    fireEvent.click(await screen.findByText('Carine Ebongue Ndjock'))
    await waitFor(() => expect(screen.getByTestId('emplacement').textContent).toBe('/console/comptes/u-rssi'))
  })

  it('pagination serveur : page suivante', async () => {
    const filtres: FiltreComptes[] = []
    const base = simules()
    liste(SUPER_ADMIN, { services: avec('comptes', { lister: async (f) => { filtres.push(f); return { ...(await base.comptes.lister(f)), total: 60 } } }, base) })
    fireEvent.click(await screen.findByRole('button', { name: 'Suivante' }))
    await waitFor(() => expect(filtres.at(-1)).toMatchObject({ page: 2 }))
  })

  it('création réservée à IAM_ACCOUNT_CREATE ; erreur avec corrélation et réessai', async () => {
    liste(LECTEUR)
    await screen.findByText('super.admin')
    expect(screen.queryByRole('button', { name: 'Créer un compte' })).toBeNull()
    cleanup()
    liste(SUPER_ADMIN)
    fireEvent.click(await screen.findByRole('button', { name: 'Créer un compte' }))
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/comptes/nouveau')
    cleanup()
    let echec = true
    const base = simules()
    liste(SUPER_ADMIN, { services: avec('comptes', { lister: async (f) => { if (echec) throw new ApiError(503, 'X', 'indisponible', 'RQ-IAM-503'); return base.comptes.lister(f) } }, base) })
    expect(await screen.findByText('RQ-IAM-503')).toBeTruthy()
    echec = false
    fireEvent.click(screen.getByRole('button', { name: 'Réessayer' }))
    expect(await screen.findByText('super.admin')).toBeTruthy()
  })
})

describe('Comptes : création (aucune inscription libre)', () => {
  const remplir = (c: HTMLElement, v: Record<string, string>) =>
    Object.entries(v).forEach(([k, x]) => fireEvent.change(c.querySelector(`input[name="${k}"]`)!, { target: { value: x } }))

  it('validation locale : champs obligatoires et au moins un profil', async () => {
    const { container } = creation()
    await screen.findByLabelText('Chef de la DDRM')
    fireEvent.submit(container.querySelector('form')!)
    expect((await screen.findAllByText('Champ obligatoire.')).length).toBe(4)
    expect(screen.getByText('Choisissez au moins un profil.')).toBeTruthy()
  })

  it('mot de passe provisoire : affiché une seule fois avec copie et avertissement', async () => {
    let recu: NouveauCompte | undefined
    const base = simules()
    const { container } = creation(SUPER_ADMIN, { services: avec('comptes', { creer: async (n) => { recu = n; return base.comptes.creer(n) } }, base) })
    await screen.findByLabelText('Super administrateur')
    remplir(container, { identifiant: 'N.Agent', prenom: 'Nadia', nom: 'Agent', email: 'n.agent@gimac-afr.org', telephone: '+237 699 44 55 66', fonction: 'Analyste', institutionId: '' })
    fireEvent.click(screen.getByLabelText('Normes et Agréments'))
    fireEvent.click(screen.getByLabelText('Les deux'))
    fireEvent.submit(container.querySelector('form')!)
    expect(await screen.findByText('Compte n.agent créé')).toBeTruthy()
    expect(recu).toMatchObject({ identifiant: 'n.agent', profils: ['ROLE_SNA'], envoyerInvitation: true, motDePasseProvisoire: true, institutionId: null })
    expect(screen.getByTestId('secret-valeur').textContent).toBe('Kp7#vQ2m!Lx9Tz4w')
    expect(screen.getByText(/n’est affiché qu’une seule fois/)).toBeTruthy()
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: async () => undefined }, configurable: true })
    fireEvent.click(screen.getByRole('button', { name: 'Copier' }))
    expect(await screen.findByText('Copié dans le presse-papiers.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Créer un autre compte' }))
    expect((await screen.findByLabelText(/Identifiant/) as HTMLInputElement).value).toBe('')
  })

  it('invitation seule : pas de mot de passe affiché ; ouverture de la fiche', async () => {
    const { container } = creation()
    await screen.findByLabelText('Normes et Agréments')
    remplir(container, { identifiant: 'b.essono', prenom: 'Blaise', nom: 'Essono', email: 'b.essono@gimac-afr.org' })
    fireEvent.click(screen.getByLabelText('Pôle réseau'))
    fireEvent.submit(container.querySelector('form')!)
    expect(await screen.findByText('Compte b.essono créé')).toBeTruthy()
    expect(screen.queryByTestId('secret-valeur')).toBeNull()
    expect(screen.getByText('envoyée')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir la fiche du compte' }))
    expect(screen.getByTestId('emplacement').textContent).toMatch(/^\/console\/comptes\/u-sim-/)
  })

  it('RSSI : les profils privilégiés ne sont pas proposés ; refus du service rattaché au champ ou général', async () => {
    let code = 'IAM_ACCOUNT_CONFLICT'
    const services = avec('comptes', { creer: async () => { throw new ApiError(409, code, 'x', 'RQ-C-1') } })
    const { container } = creation(RSSI, { services })
    await screen.findByLabelText('Normes et Agréments')
    expect(screen.queryByLabelText('Super administrateur')).toBeNull()
    expect(screen.queryByLabelText('RSSI')).toBeNull()
    remplir(container, { identifiant: 'rssi', prenom: 'R', nom: 'S', email: 'rssi@gimac-afr.org' })
    fireEvent.click(screen.getByLabelText('Normes et Agréments'))
    fireEvent.submit(container.querySelector('form')!)
    const champ = container.querySelector('input[name="identifiant"]')!
    await waitFor(() => expect(champ.getAttribute('aria-invalid')).toBe('true'))
    expect(screen.getByText('Identifiant ou courriel déjà utilisé.')).toBeTruthy()
    code = 'IAM_IDP_UNAVAILABLE'
    fireEvent.submit(container.querySelector('form')!)
    expect(await screen.findByText('RQ-C-1')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Annuler' }))
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/comptes')
  })
})

describe('Journal d’administration', () => {
  it('actes paginés, filtre par compte, lien vers le compte, corrélation', async () => {
    const { container } = rendrePage(<JournalPage />, { chemin: '/console/journal', motif: '/console/journal', permissions: RSSI })
    expect(await screen.findByText('Création du compte')).toBeTruthy()
    expect(screen.getByText('Alignement sur la PSSI 2026 : 14 caractères')).toBeTruthy()
    expect(container.querySelectorAll('[data-testid="correlation-id"]').length).toBeGreaterThan(0)
    const filtre = screen.getByRole('combobox', { name: 'Compte concerné' })
    await within(filtre).findByRole('option', { name: 'Kevin Ateba Owona (k.ateba)' })
    fireEvent.change(filtre, { target: { value: 'u-inactif' } })
    await waitFor(() => expect(screen.getByTestId('emplacement').textContent).toBe('/console/journal?compte=u-inactif'))
    expect(await screen.findByText('Désactivation')).toBeTruthy()
    await waitFor(() => expect(screen.queryByText('Création du compte')).toBeNull())
    expect(screen.getByRole('link', { name: 'k.ateba' }).getAttribute('href')).toBe('/console/comptes/u-inactif')
    fireEvent.change(filtre, { target: { value: '' } })
    await waitFor(() => expect(screen.getByTestId('emplacement').textContent).toBe('/console/journal'))
    fireEvent.change(await screen.findByRole('combobox', { name: 'Taille de page' }), { target: { value: '50' } })
    expect(await screen.findByText('Création du compte')).toBeTruthy()
  })

  it('journal vide et erreur', async () => {
    rendrePage(<JournalPage />, { chemin: '/console/journal', motif: '/console/journal', permissions: ['IAM_JOURNAL_READ'], services: avec('comptes', { journal: async () => ({ contenu: [], total: 0 }) }) })
    expect(await screen.findByText('Aucun acte d’administration')).toBeTruthy()
    cleanup()
    rendrePage(<JournalPage />, { chemin: '/console/journal', motif: '/console/journal', permissions: ['IAM_JOURNAL_READ'], services: avec('comptes', { journal: async () => { throw new ApiError(500, 'X', 'x', 'RQ-J') } }) })
    expect(await screen.findByText('RQ-J')).toBeTruthy()
  })
})
