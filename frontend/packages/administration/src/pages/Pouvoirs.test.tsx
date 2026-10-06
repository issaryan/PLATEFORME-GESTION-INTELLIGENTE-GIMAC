import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import DemandesPage from './DemandesPage'
import MatricePouvoirsPage from './MatricePouvoirsPage'
import ProfilsPage from './ProfilsPage'
import OrganisationPage from './OrganisationPage'
import { LECTEUR, RSSI, SUPER_ADMIN, avec, confirmer, rendrePage, signer, simules } from '../test/rendu'

// Pouvoirs hiérarchiques côté administration (D-81 §7) : demandes à traiter (accepter avec la clé de sécurité,
// refuser avec un motif), matrice des pouvoirs (portée par cellule, profils demandables, signature), création et
// suppression d'un profil, responsable d'une unité.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
const services = (permissions = SUPER_ADMIN) => simules({ acteur: { id: 'u-super-admin', nom: 'Serge Mouelle Kombi', permissions } })

describe('Demandes à traiter', () => {
  it('liste en attente, fiche, acceptation signée, refus motivé, filtre', async () => {
    rendrePage(<DemandesPage />, { chemin: '/console/demandes?demande=DEM-0001', motif: '/console/demandes', services: services() })
    const fiche = await screen.findByRole('dialog', { name: 'Création de compte' })
    expect(await within(fiche).findByText('i.fouda')).toBeTruthy()
    expect(within(fiche).getByText('Renfort du pôle réseau pour les liaisons ouvertes en 2026.')).toBeTruthy()
    fireEvent.click(within(fiche).getByRole('button', { name: 'Accepter avec ma clé' }))
    await signer('Création validée par la DSI')
    expect(await screen.findByText('Signature confirmée')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText('Demande acceptée : Nouveau compte : Ingrid Fouda Ngono.')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Profils' }))
    const p = await screen.findByRole('dialog', { name: 'Profils' })
    fireEvent.click(await within(p).findByRole('button', { name: 'Refuser la demande' }))
    await confirmer('Refuser la demande', 'Profil non justifié par la fiche de poste')
    expect(await screen.findByText('Demande refusée : Nadège Mvondo Assiga.')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('État'), { target: { value: '' } })
    expect(await screen.findByText('Bertrand Nguema Obiang')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Désactivation ou réactivation' }))
    const r = await screen.findByRole('dialog', { name: 'Désactivation ou réactivation' })
    expect(await within(r).findByText('Le départ n’est pas encore confirmé par les ressources humaines.')).toBeTruthy()
    expect(within(r).queryByRole('button', { name: 'Refuser la demande' })).toBeNull()
  })
  it('lecteur sans permission de l’acte : refus seulement ; fiche introuvable', async () => {
    rendrePage(<DemandesPage />, { chemin: '/des/administration/demandes?demande=DEM-0002', motif: '/des/administration/demandes', permissions: LECTEUR, services: services(LECTEUR) })
    const fiche = await screen.findByRole('dialog', { name: 'Profils' })
    expect(await within(fiche).findByText('Vous ne détenez pas la permission IAM_ACCOUNT_ROLES : vous pouvez refuser cette demande, pas l’accepter.')).toBeTruthy()
    expect(within(fiche).queryByRole('button', { name: 'Accepter avec ma clé' })).toBeNull()
    cleanup()
    rendrePage(<DemandesPage />, { chemin: '/console/demandes?demande=X', motif: '/console/demandes', services: services() })
    expect(await screen.findByText('Demande introuvable.')).toBeTruthy()
  })
})

describe('Matrice des pouvoirs', () => {
  it('tableau profils × actions, modification d’une ligne signée', async () => {
    rendrePage(<MatricePouvoirsPage />, { chemin: '/console/matrice-pouvoirs', motif: '/console/matrice-pouvoirs', services: services() })
    expect(await screen.findByRole('heading', { level: 1, name: 'Matrice des pouvoirs' })).toBeTruthy()
    expect(await screen.findByText('ROLE_CHEF_DES')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Pouvoirs du profil Normes et Agréments' }))
    const d = await screen.findByRole('dialog', { name: /Pouvoirs du profil/ })
    fireEvent.change(within(d).getByLabelText(/^Confier une tâche/), { target: { value: 'DIRECTS' } })
    fireEvent.click(within(d).getByLabelText(/Collaborateur|ROLE_COLLABORATEUR/))
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    await signer('Revue annuelle des pouvoirs')
    expect(await screen.findByText('Signature confirmée')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText(/^Pouvoirs du profil .* enregistrés\.$/)).toBeTruthy()
  })
  it('lecture seule sans IAM_PROFILE_EDIT ; matrice indisponible', async () => {
    rendrePage(<MatricePouvoirsPage />, { chemin: '/console/matrice-pouvoirs', motif: '/console/matrice-pouvoirs', permissions: LECTEUR, services: services(LECTEUR) })
    expect(await screen.findByText('ROLE_CHEF_DES')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Modifier' })).toBeNull()
    cleanup()
    const s = avec('pouvoirs', { matrice: () => Promise.reject(new ApiError(503, 'IAM_INDISPONIBLE', 'x', 'cid-m')) })
    rendrePage(<MatricePouvoirsPage />, { chemin: '/console/matrice-pouvoirs', motif: '/console/matrice-pouvoirs', services: s })
    expect(await screen.findByText('cid-m', { exact: false })).toBeTruthy()
  })
})

describe('Profils créés', () => {
  it('création signée d’un profil (privilégié signalé), puis suppression', async () => {
    rendrePage(<ProfilsPage />, { chemin: '/console/profils', motif: '/console/profils', services: services() })
    fireEvent.click(await screen.findByRole('button', { name: 'Nouveau profil' }))
    const d = await screen.findByRole('dialog', { name: 'Nouveau profil' })
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: 'role_x' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    expect(within(d).getByText('Code invalide : ROLE_ puis une majuscule, puis majuscules, chiffres ou souligné.')).toBeTruthy()
    expect(within(d).getByText('La description est obligatoire.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: 'ROLE_ASTREINTE' } })
    fireEvent.change(within(d).getByLabelText(/^Description/), { target: { value: 'Astreinte de supervision' } })
    // D-90 : composition par fonctionnalité ; un droit d'administration rend le profil privilégié.
    fireEvent.click(within(d).getByRole('checkbox', { name: 'Journal d’administration' }))
    expect(within(d).getByText(/Profil privilégié/)).toBeTruthy()
    fireEvent.click(within(d).getByRole('checkbox', { name: 'Journal d’administration' }))
    expect(within(d).queryByText(/Profil privilégié/)).toBeNull()
    fireEvent.click(within(d).getByRole('button', { name: 'Droits de Liaisons et parcours' }))
    fireEvent.click(within(d).getByRole('checkbox', { name: /HOM_EVIDENCE_SUBMIT/ }))
    expect((within(d).getByRole('checkbox', { name: /HOM_LIAISON_READ/ }) as HTMLInputElement).checked).toBe(true)
    expect(within(d).getByRole('checkbox', { name: 'Liaisons et parcours' }).getAttribute('aria-checked')).toBe('mixed')
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Astreinte' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    await signer('Astreinte de nuit du NOC')
    fireEvent.click(await screen.findByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText('Profil ROLE_ASTREINTE créé.')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Supprimer le profil ROLE_ASTREINTE' }))
    await signer('Astreinte supprimée')
    fireEvent.click(await screen.findByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText('Profil ROLE_ASTREINTE supprimé.')).toBeTruthy()
    await waitFor(() => expect(screen.queryByText('ROLE_ASTREINTE')).toBeNull())
  })
  it('sans IAM_PROFILE_EDIT : ni création ni suppression', async () => {
    rendrePage(<ProfilsPage />, { chemin: '/console/profils', motif: '/console/profils', permissions: LECTEUR, services: services(LECTEUR) })
    expect(await screen.findByText('ROLE_SNA')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Nouveau profil' })).toBeNull()
  })
})

describe('Organisation : responsable d’une unité', () => {
  it('fiche avec son responsable, modification du responsable, refus expliqué', async () => {
    const s = services(RSSI)
    rendrePage(<OrganisationPage />, { chemin: '/console/organisation', motif: '/console/organisation', permissions: RSSI, services: s })
    const arbre = await screen.findByRole('tree')
    fireEvent.click(within(arbre).getByText('Département Exploitation et Supervision'))
    expect(await screen.findByText('Responsable de l’unité')).toBeTruthy()
    expect(await screen.findByText('Christian Ndong Obiang')).toBeTruthy()
    fireEvent.click(within(arbre).getByText('Service Production informatique'))
    fireEvent.click(await screen.findByRole('button', { name: 'Modifier l’unité' }))
    const d = await screen.findByRole('dialog', { name: 'Modifier l’unité' })
    const choix = within(d).getByLabelText(/^Responsable de l’unité/)
    await waitFor(() => expect(within(choix).getAllByRole('option').length).toBeGreaterThan(3))
    vi.spyOn(s.organisation, 'modifierUnite').mockRejectedValueOnce(new ApiError(422, 'ORG_RESPONSABLE_INVALIDE', 'x', 'cid-r', { responsableId: 'u-x' }))
    fireEvent.change(choix, { target: { value: 'u-ingenieur' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer l’unité' }))
    expect(await within(d).findByText('Le responsable doit être un compte interne actif, distinct du titulaire.')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer l’unité' }))
    expect(await screen.findByText('Unité « Service Production informatique » modifiée.')).toBeTruthy()
    expect(await screen.findByText('Patrick Mbida Essomba')).toBeTruthy()
  })
})
