import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { useLocation } from 'react-router'
import { ApiError } from '@gimac/socle'
import EquipePage from './EquipePage'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import { PROFILS } from '../../../permissions'

// Mon équipe (D-81 §7) : organisation (arbre des subordonnés et des unités dirigées, gestes selon la matrice des
// pouvoirs), « Demander un compte », demandes de profils, de désactivation, rattachement, « Mes demandes ».
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const CHEF = { id: 'U-001', nom: 'Paul Mbarga Atangana' }
const session = (u = CHEF, role = 'ROLE_CHEF_DES') => ({ ...sessionTest(PROFILS[role], [role]), sujet: u.id, nom: u.nom })
function Ici() {
  const l = useLocation()
  return <p>Destination {l.pathname}{l.search}</p>
}
const page = (chemin = '/des/equipe?vue=organisation', o: { s?: Services; u?: typeof CHEF; role?: string } = {}) => {
  const s = o.s ?? servicesSimules({ latenceMs: 0, utilisateur: o.u ?? CHEF })
  rendrePage(<EquipePage />, {
    chemin, motif: '/des/equipe', services: s, session: session(o.u, o.role),
    autres: [{ motif: '/des/equipe/demandes', element: <EquipePage /> }, { motif: '/des/taches', element: <Ici /> }],
  })
  return s
}
const choisir = async (nom: string, arbre = 'Mes subordonnés') => fireEvent.click(await within(await screen.findByRole('tree', { name: arbre })).findByText(nom))

describe('Mon équipe : organisation et demandes', () => {
  it('arbre, gestes ouverts par la matrice, confier une tâche à une personne ou à une unité', async () => {
    page()
    expect(await screen.findByRole('tab', { name: 'Organisation', selected: true })).toBeTruthy()
    expect(await screen.findByText('Choisissez une personne ou une unité')).toBeTruthy()
    await choisir('Idriss Mahamat Saleh')
    const gestes = screen.getByRole('group', { name: 'Gestes permis' })
    // Niveau 2 : confier, lire et viser, demander un profil ou une désactivation ; pas de rattachement (DIRECTS).
    expect(within(gestes).getByRole('link', { name: 'Journée' }).getAttribute('href')).toContain('collaborateur=u-res')
    expect(within(gestes).getByRole('link', { name: 'Rapports' })).toBeTruthy()
    expect(within(gestes).queryByRole('button', { name: 'Modifier le rattachement' })).toBeNull()
    expect(screen.getByText('Niveau 2')).toBeTruthy()
    fireEvent.click(within(gestes).getByRole('button', { name: 'Confier une tâche' }))
    expect(await screen.findByText('Destination /des/taches?confier=u-res')).toBeTruthy()
    cleanup()
    page()
    await choisir('Pôle réseau', 'Unités dirigées')
    expect(screen.getAllByText('POLE_RESEAU').length).toBeGreaterThan(1)
    fireEvent.click(screen.getByRole('button', { name: 'Confier une tâche à l’unité' }))
    expect(await screen.findByText('Destination /des/taches?unite=POLE_RESEAU')).toBeTruthy()
  })

  it('demander un compte : contrôles de saisie, refus du service, demande déposée et listée', async () => {
    const s = page()
    fireEvent.click(await screen.findByRole('button', { name: 'Demander un compte' }))
    const d = await screen.findByRole('dialog', { name: 'Demander un compte' })
    fireEvent.click(within(d).getByRole('button', { name: 'Envoyer la demande' }))
    expect(within(d).getByText('Identifiant invalide : minuscules, chiffres, point, tiret ou souligné, 3 à 50 caractères.')).toBeTruthy()
    expect(within(d).getByText('Choisissez au moins un profil.')).toBeTruthy()
    expect(within(d).getByText('Motif de 5 à 500 caractères.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Prénom/), { target: { value: 'Blaise' } })
    fireEvent.change(within(d).getByLabelText(/^Nom/), { target: { value: 'Ondo' } })
    fireEvent.change(within(d).getByLabelText(/^Identifiant/), { target: { value: 'B.Ondo' } })
    fireEvent.change(within(d).getByLabelText(/^Courriel/), { target: { value: 'b.ondo@gimac-afr.org' } })
    fireEvent.change(within(d).getByLabelText(/^Téléphone/), { target: { value: '+237 699 00 00 00' } })
    fireEvent.change(within(d).getByLabelText(/^Fonction/), { target: { value: 'Technicien système' } })
    fireEvent.change(within(d).getByLabelText(/^Unité/), { target: { value: 'POLE_SYSTEME' } })
    fireEvent.change(within(d).getByLabelText(/^Responsable \(n\+1\)/), { target: { value: 'u-resp-info' } })
    fireEvent.click(within(d).getByLabelText('Pôle système'))
    fireEvent.change(within(d).getByLabelText(/^Motif/), { target: { value: 'Renfort du pôle système' } })
    vi.spyOn(s.pouvoirs, 'demander').mockRejectedValueOnce(new ApiError(403, 'ORG_HORS_PERIMETRE', 'x', 'cid-1'))
    fireEvent.click(within(d).getByRole('button', { name: 'Envoyer la demande' }))
    expect(await within(d).findByText('Cette personne ou cette unité est hors de votre périmètre selon la matrice des pouvoirs.')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Envoyer la demande' }))
    expect(await screen.findByText('Demande adressée à l’administration.')).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Mes demandes' }))
    expect(await screen.findByText('Nouveau compte : Blaise Ondo')).toBeTruthy()
  })

  it('demander un profil, une désactivation ; modifier le rattachement d’un subordonné direct', async () => {
    page()
    await choisir('Gaston Ndzana Ebode')
    fireEvent.click(screen.getByRole('button', { name: 'Demander un profil' }))
    let d = await screen.findByRole('dialog', { name: 'Demander un profil' })
    fireEvent.click(within(d).getByRole('button', { name: 'Envoyer la demande' }))
    expect(within(d).getByText('Choisissez au moins un profil à ajouter ou à retirer.')).toBeTruthy()
    const [ajouter, retirer] = within(d).getAllByRole('group')
    fireEvent.click(within(ajouter).getByLabelText('Normes et Agréments'))
    fireEvent.click(within(retirer).getByLabelText('Normes et Agréments'))
    fireEvent.click(within(d).getByRole('button', { name: 'Envoyer la demande' }))
    expect(within(d).getByText('Un même profil ne peut être à la fois ajouté et retiré.')).toBeTruthy()
    fireEvent.click(within(retirer).getByLabelText('Normes et Agréments'))
    fireEvent.change(within(d).getByLabelText(/^Motif/), { target: { value: 'Suivi des agréments' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Envoyer la demande' }))
    expect(await screen.findByText('Demande adressée à l’administration.')).toBeTruthy()

    await choisir('Gaston Ndzana Ebode')
    fireEvent.click(screen.getByRole('button', { name: 'Demander la désactivation' }))
    d = await screen.findByRole('dialog', { name: 'Demander la désactivation' })
    fireEvent.change(within(d).getByLabelText(/^Motif/), { target: { value: 'Départ en retraite' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Envoyer la demande' }))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Demander la désactivation' })).toBeNull())

    await choisir('Gaston Ndzana Ebode')
    fireEvent.click(screen.getByRole('button', { name: 'Modifier le rattachement' }))
    d = await screen.findByRole('dialog', { name: 'Modifier le rattachement' })
    fireEvent.change(within(d).getByLabelText(/^Unité/), { target: { value: 'DES' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le rattachement' }))
    expect(await screen.findByText('Rattachement de Gaston Ndzana Ebode modifié.')).toBeTruthy()
  })

  it('mes demandes : filtre, fiche avec décision, annulation ; lien direct /des/equipe/demandes?demande=', async () => {
    page('/des/equipe/demandes?demande=DEM-0003')
    const fiche = await screen.findByRole('dialog', { name: 'Désactivation ou réactivation' })
    expect(await within(fiche).findByText('Le départ n’est pas encore confirmé par les ressources humaines.')).toBeTruthy()
    expect(within(fiche).queryByRole('button', { name: 'Annuler la demande' })).toBeNull()
    fireEvent.click(within(fiche).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    fireEvent.click(await screen.findByRole('button', { name: 'Création de compte' }))
    const c = await screen.findByRole('dialog', { name: 'Création de compte' })
    expect(await within(c).findByText('i.fouda')).toBeTruthy()
    fireEvent.click(within(c).getByRole('button', { name: 'Annuler la demande' }))
    expect(await screen.findByText('Demande annulée.')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('État'), { target: { value: 'ANNULEE' } })
    expect(await screen.findByText('Nouveau compte : Ingrid Fouda Ngono')).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Activité du jour' }))
    expect(await screen.findByRole('button', { name: 'Jour précédent' })).toBeTruthy()
  })

  it('pouvoirs indisponibles : erreur expliquée ; responsable sans pouvoir de demande de compte', async () => {
    const s = servicesSimules({ latenceMs: 0, utilisateur: CHEF })
    vi.spyOn(s.pouvoirs, 'moi').mockRejectedValue(new ApiError(503, 'PIL_ORGANISATION_INDISPONIBLE', 'x', 'cid-7'))
    page('/des/equipe?vue=organisation', { s })
    expect(await screen.findByText('L’organisation (équipe, responsables) est momentanément indisponible : réessayez dans un instant.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Demander un compte' })).toBeNull()
  })
})
