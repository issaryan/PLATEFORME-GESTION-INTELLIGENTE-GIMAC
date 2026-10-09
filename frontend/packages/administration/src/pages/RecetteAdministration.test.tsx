import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import ProfilsPage from './ProfilsPage'
import OrganigrammePage from './OrganigrammePage'
import { RSSI, SUPER_ADMIN, avec, rendrePage, signer, simules } from '../test/rendu'

// Corrections de la recette du lot « administration » (docs/recette/administration.md, ADM-03 à ADM-08).
afterEach(cleanup)
const profils = (o: Partial<Parameters<typeof rendrePage>[1]> = {}) => rendrePage(<ProfilsPage />, { chemin: '/console/profils', motif: '/console/profils', ...o })
const acteurRssi = { id: 'u-rssi', nom: 'Hervé Kamga Tchoupo', permissions: RSSI }

describe('ADM-03 : refus du service pendant une signature', () => {
  it('la clé a répondu : « Acte refusé par le service », message et code du service, sans « Réessayer » ni mise en cause de la clé', async () => {
    profils({ permissions: SUPER_ADMIN })
    const sna = (await screen.findByText('ROLE_SNA')).closest('tr')!
    fireEvent.click(within(sna).getByRole('button', { name: /libellé et la description du profil Normes et Agréments/ }))
    const d = await screen.findByRole('dialog', { name: 'Modifier le profil' })
    fireEvent.click(within(d).getByRole('checkbox', { name: /Profil externe/ }))
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    const sig = await signer('Profil rendu externe pour essai')
    expect(await within(sig).findByText('Acte refusé par le service')).toBeTruthy()
    expect(within(sig).getByText(/Le caractère externe d’un profil ne change pas tant qu’il a des titulaires/)).toBeTruthy()
    expect(within(sig).getByText('Code du refus : IAM_PROFIL_EXTERNE_TITULAIRES')).toBeTruthy()
    expect(within(sig).queryByText(/La clé de sécurité n'a pas répondu/)).toBeNull()
    expect(within(sig).queryByRole('button', { name: 'Réessayer' })).toBeNull()
  })

  it('panne de la clé (aucune clé enrôlée) : toujours « La clé de sécurité n’a pas répondu » avec « Réessayer »', async () => {
    profils({ permissions: SUPER_ADMIN, services: simules({ reauth: 'sansCle' }) })
    const sna = (await screen.findByText('ROLE_SNA')).closest('tr')!
    fireEvent.click(within(sna).getByRole('button', { name: /libellé et la description du profil Normes et Agréments/ }))
    const d = await screen.findByRole('dialog', { name: 'Modifier le profil' })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Normes' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    const sig = await signer('Libellé raccourci pour essai')
    expect(await within(sig).findByText(/La clé de sécurité n'a pas répondu/)).toBeTruthy()
    expect(within(sig).getByRole('button', { name: 'Réessayer' })).toBeTruthy()
  })
})

describe('ADM-04 : droits privilégiés réservés à l’octroi, avant la signature', () => {
  it('RSSI : « Journal d’administration » non cochable, raison affichée ; le super administrateur le coche', async () => {
    profils({ permissions: RSSI, services: simules({ acteur: acteurRssi }) })
    fireEvent.click(await screen.findByRole('button', { name: 'Nouveau profil' }))
    const d = await screen.findByRole('dialog', { name: 'Nouveau profil' })
    expect(await within(d).findByText(/Les droits privilégiés .* ne peuvent être ajoutés que par un compte habilité/)).toBeTruthy()
    const journal = within(d).getByRole('checkbox', { name: 'Journal d’administration' }) as HTMLInputElement
    expect(journal.disabled).toBe(true)
    expect(journal.getAttribute('aria-describedby')).toBeTruthy()
    expect(document.getElementById(journal.getAttribute('aria-describedby')!)!.textContent).toBe('Réservé à qui peut attribuer les profils privilégiés')
    // Un droit ordinaire reste cochable.
    expect((within(d).getByRole('checkbox', { name: 'Agenda' }) as HTMLInputElement).disabled).toBe(false)
    cleanup()
    profils({ permissions: SUPER_ADMIN })
    fireEvent.click(await screen.findByRole('button', { name: 'Nouveau profil' }))
    const d2 = await screen.findByRole('dialog', { name: 'Nouveau profil' })
    const journal2 = await within(d2).findByRole('checkbox', { name: 'Journal d’administration' }) as HTMLInputElement
    expect(journal2.disabled).toBe(false)
    expect(within(d2).queryByText('Réservé à qui peut attribuer les profils privilégiés')).toBeNull()
  })

  it('RSSI, fiche d’un profil : aucune permission privilégiée nouvelle proposée dans la vue Droits', async () => {
    profils({ permissions: RSSI, services: simules({ acteur: acteurRssi }) })
    const sna = (await screen.findByText('ROLE_SNA')).closest('tr')!
    fireEvent.click(within(sna).getByRole('button', { name: 'Modifier la composition' }))
    const d = await screen.findByRole('dialog', { name: 'Profil Normes et Agréments' })
    fireEvent.click(within(d).getByRole('tab', { name: 'Droits' }))
    await waitFor(() => expect(within(d).getByLabelText('HOM_REPLAY_ADMIN')).toBeTruthy())
    expect(within(d).queryByLabelText('IAM_JOURNAL_READ')).toBeNull()
    expect(within(d).queryByLabelText('PLATFORM_OPS_READ')).toBeNull()
  })
})

describe('ADM-06 : aperçu de la barre latérale d’un nouveau profil', () => {
  it('messagerie, notifications et Mon activité, ajoutées d’office par MS-IAM, figurent dans l’aperçu', async () => {
    profils({ permissions: SUPER_ADMIN })
    fireEvent.click(await screen.findByRole('button', { name: 'Nouveau profil' }))
    const d = await screen.findByRole('dialog', { name: 'Nouveau profil' })
    const apercu = within(d).getByRole('complementary', { name: 'Aperçu de la barre latérale' })
    await waitFor(() => expect(within(apercu).getByText('Messagerie')).toBeTruthy())
    expect(within(apercu).getByText('Notifications')).toBeTruthy()
    expect(within(apercu).getByText('Ma journée')).toBeTruthy()
  })
})

describe('ADM-07 : élision dans les noms accessibles', () => {
  it('« Droits d’Agenda », « Droits d’Organisation », jamais « Droits de Agenda »', async () => {
    profils({ permissions: SUPER_ADMIN })
    fireEvent.click(await screen.findByRole('button', { name: 'Nouveau profil' }))
    const d = await screen.findByRole('dialog', { name: 'Nouveau profil' })
    expect(await within(d).findByRole('button', { name: 'Droits d’Agenda' })).toBeTruthy()
    expect(within(d).getByRole('button', { name: 'Droits d’Organisation' })).toBeTruthy()
    expect(within(d).getByRole('button', { name: 'Droits de Messagerie' })).toBeTruthy()
    expect(within(d).queryByRole('button', { name: /Droits de [AEIOUÉ]/ })).toBeNull()
  })
})

describe('ADM-08 : organigramme pendant le chargement des comptes', () => {
  it('aucun identifiant technique ni effectif faux tant que les comptes ne sont pas lus', async () => {
    const base = simules()
    let lire: () => void = () => undefined
    const attente = new Promise<void>((r) => { lire = r })
    const services = avec('comptes', { lister: async (f) => { await attente; return base.comptes.lister(f) } }, base)
    rendrePage(<OrganigrammePage />, { chemin: '/console/organigramme', motif: '/console/organigramme', permissions: RSSI, services })
    await waitFor(() => expect(document.querySelector('[data-unite="DES"]')).toBeTruthy())
    const des = document.querySelector('[data-unite="DES"]')!
    expect(des.textContent).toContain('Chef : chargement…')
    expect(des.textContent).not.toMatch(/u-chef-des|0 personne/)
    lire()
    await waitFor(() => expect(des.textContent).toMatch(/Chef : Christian Ndong Obiang · 5 personnes/))
  })
})
