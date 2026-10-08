import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import DossierLiaisonPage from './DossierLiaisonPage'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import { PROFILS } from '../../../permissions'
import { axeSansViolation } from './axe'

afterEach(cleanup)
const cleBrute = /\b(parcours[A-Z][a-zA-Z]*)\.[a-zA-Z]/
const avec = (surcharge: Partial<Services['parcours']>): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, parcours: { ...s.parcours, ...surcharge } }
}
const page = (chemin: string, o: { services?: Services; langue?: 'fr' | 'en' | 'es'; role?: string } = {}) =>
  rendrePage(<DossierLiaisonPage />, {
    chemin, motif: '/des/parcours/:id', langue: o.langue, services: o.services,
    session: sessionTest(PROFILS[o.role ?? 'ROLE_CHEF_DDRM'], [o.role ?? 'ROLE_CHEF_DDRM']),
  })
const dialogue = () => screen.getByRole('dialog')

describe('Dossier numérique de la liaison (CR Fig. 7.3)', () => {
  it('en-tête, identification, pilotage et écran principal du périmètre avec ses deux couvertures', async () => {
    page('/des/parcours/LIA-2026-030')
    expect((await screen.findByRole('heading', { level: 1 })).textContent).toBe('Établissement de paiement A')
    expect(screen.getAllByText('LIA-2026-030').length).toBeGreaterThan(0)
    expect(screen.getByText('Étape courante : Intégration')).toBeTruthy()
    expect(screen.getByText('Banque B')).toBeTruthy()
    expect(screen.getByText('version 4')).toBeTruthy()
    expect(screen.getByText('Couvertures calculées sur le périmètre contractualisé')).toBeTruthy()
    expect(screen.getByText('Hors périmètre')).toBeTruthy()
    expect(screen.getByRole('tab', { name: 'Blocages (1)' })).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Graphe des opérations' }).getAttribute('href')).toBe('/des/liaisons/LIA-2026-030?onglet=graphe')
    // Messagerie (D-64) : un message rattaché à la liaison se prépare depuis son dossier.
    expect(screen.getByRole('link', { name: 'Discuter de cette liaison' }).getAttribute('href')).toBe('/des/messagerie?nouveau=1&liaison=LIA-2026-030')
  })
  it('branche mobile : avancement repris du simulateur ; liaison d’origine d’un service supplémentaire', async () => {
    page('/des/parcours/LIA-2025-088')
    await screen.findByRole('heading', { level: 1 })
    expect(screen.getAllByText('repris du simulateur').length).toBeGreaterThan(0)
    cleanup()
    page('/des/parcours/LIA-2026-044')
    await screen.findByRole('heading', { level: 1 })
    expect(screen.getByRole('link', { name: 'LIA-2026-022' }).getAttribute('href')).toBe('/des/parcours/LIA-2026-022')
    expect(screen.getAllByText('À désigner').length).toBeGreaterThan(0)
  })
  it('erreur de chargement : message et identifiant de corrélation', async () => {
    page('/des/parcours/LIA-X', { services: avec({ dossier: async () => { throw new ApiError(404, 'HOM_LIAISON_NOT_FOUND', 'Liaison introuvable.', 'RQ-404') } }) })
    expect(await screen.findByText('RQ-404')).toBeTruthy()
  })
  it('onglet Étapes : six éléments de l’étape choisie, verrou de la sécurité, sélection par ligne', async () => {
    page('/des/parcours/LIA-2026-036?onglet=etapes')
    expect(await screen.findByText('5. Ateliers', { selector: '.gm-card-title' })).toBeTruthy()
    expect(screen.getByText('Responsable désigné')).toBeTruthy()
    expect(screen.getByText(/Une étape n'est officiellement close/)).toBeTruthy()
    expect(await screen.findByText('Ateliers : comptes rendus')).toBeTruthy()
    fireEvent.click(screen.getByText('Verrou : Approbation de la sécurité').closest('tr') as HTMLElement)
    expect(await screen.findByText(/n'est pas une étape/)).toBeTruthy()
    fireEvent.click(screen.getByText('2. Adhésion').closest('tr') as HTMLElement)
    expect(await screen.findAllByText('Réuni')).toHaveLength(6)
  })
  it('étape 5 : dépôt du compte rendu d’un atelier, fichier exigé', async () => {
    page('/des/parcours/LIA-2026-036?onglet=etapes&etape=ATELIERS')
    const boutons = await screen.findAllByRole('button', { name: 'Déposer le compte rendu' })
    fireEvent.click(boutons[0])
    fireEvent.click(within(dialogue()).getByRole('button', { name: 'Déposer le compte rendu' }))
    expect(await within(dialogue()).findByText('Joignez le compte rendu.')).toBeTruthy()
    const champ = dialogue().querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(champ, { target: { files: [new File(['x'], 'cr-atelier.pdf', { type: 'application/pdf' })] } })
    fireEvent.change(within(dialogue()).getByLabelText('Commentaire'), { target: { value: 'RAS' } })
    fireEvent.click(within(dialogue()).getByRole('button', { name: 'Déposer le compte rendu' }))
    expect(await screen.findByText(/Compte rendu déposé/)).toBeTruthy()
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  })
  it('étape 4 : affectation des ressources par un chef de département, une personne au moins, fonctions du RACI servi', async () => {
    page('/des/parcours/LIA-2026-044?action=affecter', { role: 'ROLE_CHEF_DES' })
    const d = await screen.findByRole('dialog')
    expect(within(d).getByText(/Aucun conducteur/)).toBeTruthy()
    // Fonctions internes du RACI de la liaison (intervenants servis), ni participant ni conducteur.
    expect(within(d).queryByLabelText(/^Conducteur de projet/)).toBeNull()
    d.querySelectorAll('select').forEach((x) => fireEvent.change(x, { target: { value: '' } }))
    fireEvent.click(within(d).getByRole('button', { name: 'Affecter les ressources' }))
    expect(await within(d).findByText('Désignez au moins une personne.')).toBeTruthy()
    await waitFor(() => expect(within(d).getByLabelText(/Pôle réseau/).querySelectorAll('option').length).toBeGreaterThan(1))
    fireEvent.change(within(d).getByLabelText(/Pôle réseau/), { target: { value: 'U-RES-1' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Affecter les ressources' }))
    expect(await screen.findByText('Ressources du projet affectées.')).toBeTruthy()
  })
  it('affectation refusée par le service : message et corrélation', async () => {
    page('/des/parcours/LIA-2026-044?action=affecter', { role: 'ROLE_CHEF_DES', services: avec({ affecterRessources: async () => { throw new ApiError(409, 'HOM_STEP_NOT_OPEN', 'fermée', 'RQ-AFF') } }) })
    const d = await screen.findByRole('dialog')
    fireEvent.click(within(d).getByRole('button', { name: 'Affecter les ressources' }))
    expect(await within(d).findByText('RQ-AFF')).toBeTruthy()
    expect(within(d).getByText(/l'affectation des ressources n'est pas attendue/)).toBeTruthy()
  })
  it('désignation du conducteur, puis redésignation motivée', async () => {
    page('/des/parcours/LIA-2026-044')
    fireEvent.click(await screen.findByRole('button', { name: 'Désigner le conducteur' }))
    let d = dialogue()
    expect(within(d).queryByLabelText(/Motif/)).toBeNull()
    await waitFor(() => expect(within(d).getByLabelText(/Conducteur désigné/).querySelectorAll('option').length).toBe(3))
    fireEvent.change(within(d).getByLabelText(/Conducteur désigné/), { target: { value: 'U-COND-1' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Désigner le conducteur' }))
    expect(await screen.findByText('Conducteur de projet 1 conduit désormais cette liaison.')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Redésigner le conducteur' }))
    d = dialogue()
    fireEvent.change(within(d).getByLabelText(/Conducteur désigné/), { target: { value: 'U-COND-2' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Redésigner le conducteur' }))
    expect(await within(d).findByText(/Motivez la redésignation/)).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/Motif de la redésignation/), { target: { value: 'Rééquilibrage du plan de charge' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Redésigner le conducteur' }))
    expect(await screen.findByText('Conducteur de projet 2 conduit désormais cette liaison.')).toBeTruthy()
  })
  it('désignation refusée : refus du service affiché', async () => {
    page('/des/parcours/LIA-2026-044?action=designer', { services: avec({ designerConducteur: async () => { throw new ApiError(422, 'HOM_LEAD_NOT_ELIGIBLE', 'x', 'RQ-DES') } }) })
    const d = await screen.findByRole('dialog')
    await waitFor(() => expect(within(d).getByLabelText(/Conducteur désigné/).querySelectorAll('option').length).toBe(3))
    fireEvent.change(within(d).getByLabelText(/Conducteur désigné/), { target: { value: 'U-COND-1' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Désigner le conducteur' }))
    expect(await within(d).findByText(/ne détient pas le rôle de conducteur/)).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    expect(screen.queryByRole('dialog')).toBeNull()
  })
  it('blocages : déclaration datée et contrôlée, puis levée motivée', async () => {
    page('/des/parcours/LIA-2026-036?onglet=blocages')
    expect(await screen.findByText('Aucun blocage en cours')).toBeTruthy()
    fireEvent.click(screen.getAllByRole('button', { name: 'Déclarer un blocage' })[0])
    const d = dialogue()
    fireEvent.click(within(d).getByRole('button', { name: 'Déclarer un blocage' }))
    expect(await within(d).findByText(/Décrivez le motif/)).toBeTruthy()
    expect(within(d).getByText(/format JJ\/MM\/AAAA/)).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Motif/), { target: { value: 'Certificat de test du participant expiré' } })
    fireEvent.change(within(d).getByLabelText(/Prochaine action/), { target: { value: 'Relancer le participant' } })
    fireEvent.change(within(d).getByLabelText(/Fonction du responsable/), { target: { value: 'POLE_RESEAU' } })
    await waitFor(() => expect(within(d).getByLabelText(/Responsable de la levée/).querySelectorAll('option').length).toBe(2))
    fireEvent.change(within(d).getByLabelText(/Responsable de la levée/), { target: { value: 'U-RES-1' } })
    fireEvent.change(within(d).getByLabelText(/Échéance de levée/), { target: { value: '31/02/2026' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Déclarer un blocage' }))
    expect(within(d).getByText(/Saisissez une date valide/)).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/Échéance de levée/), { target: { value: '15/10/2026' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Déclarer un blocage' }))
    expect(await screen.findByText('Blocage déclaré.')).toBeTruthy()
    expect(await screen.findByText('Certificat de test du participant expiré')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Lever le blocage' }))
    const c = screen.getByRole('alertdialog')
    fireEvent.change(within(c).getByLabelText(/Motif/), { target: { value: 'Certificat renouvelé et vérifié' } })
    fireEvent.click(within(c).getByRole('button', { name: 'Lever le blocage' }))
    expect(await screen.findByText('Blocage levé.')).toBeTruthy()
    expect(await screen.findByText(/Blocage levé \(1\)/)).toBeTruthy()
  })
  it('levée refusée : message du service', async () => {
    page('/des/parcours/LIA-2026-030?onglet=blocages', { services: avec({ leverBlocage: async () => { throw new ApiError(404, 'HOM_BLOCKAGE_NOT_FOUND', 'x', 'RQ-LEV') } }) })
    fireEvent.click(await screen.findByRole('button', { name: 'Lever le blocage' }))
    const c = screen.getByRole('alertdialog')
    fireEvent.change(within(c).getByLabelText(/Motif/), { target: { value: 'Levée demandée en réunion' } })
    fireEvent.click(within(c).getByRole('button', { name: 'Lever le blocage' }))
    expect(await screen.findByText('RQ-LEV')).toBeTruthy()
  })
  it('gouvernance : RACI+E par étape, interlocuteurs et tiers', async () => {
    page('/des/parcours/LIA-2026-019?onglet=gouvernance')
    expect(await screen.findByText('Intervenants par étape')).toBeTruthy()
    expect(screen.getAllByText('Personnalisateur P').length).toBeGreaterThan(0)
    expect(await screen.findByText('R · Responsable')).toBeTruthy()
    expect(screen.getAllByText(/Correspondant technique/).length).toBeGreaterThan(0)
  })
  it('matrice des rôles : un lecteur ne voit aucun geste ; les onglets suivent l’adresse', async () => {
    page('/des/parcours/LIA-2026-030?action=designer', { role: 'ROLE_POLE_RESEAU' })
    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(screen.queryByRole('button', { name: /conducteur|blocage|ressources/ })).toBeNull()
    fireEvent.click(screen.getByRole('tab', { name: 'Gouvernance' }))
    expect(await screen.findByText('Intervenants par étape')).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Périmètre' }))
    expect(await screen.findByText('Couvertures calculées sur le périmètre contractualisé')).toBeTruthy()
  })
  it('bandeau après ouverture, refermable', async () => {
    page('/des/parcours/LIA-2026-044?nouvelle=1')
    expect(await screen.findByText(/ouverte au cadrage/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Fermer' }))
    await waitFor(() => expect(screen.queryByText(/ouverte au cadrage/)).toBeNull())
  })
  it.each(['en', 'es'] as const)('rendu en %s sans clé brute, sur chaque onglet du cadrage', async (langue) => {
    for (const onglet of ['perimetre', 'etapes', 'blocages', 'gouvernance']) {
      const { container } = page(`/des/parcours/LIA-2026-030?onglet=${onglet}`, { langue })
      await screen.findByRole('heading', { level: 1 })
      await waitFor(() => expect(container.querySelector('.gm-skeleton')).toBeNull())
      expect(container.textContent).not.toMatch(cleBrute)
      cleanup()
    }
  })
  it('accessibilité : axe sans violation sur le périmètre, les étapes et les dialogues', async () => {
    const { container } = page('/des/parcours/LIA-2026-030')
    await screen.findByText('Couvertures calculées sur le périmètre contractualisé')
    await axeSansViolation(container)
    cleanup()
    const r = page('/des/parcours/LIA-2026-044?onglet=etapes&etape=LANCEMENT&action=affecter', { role: 'ROLE_CHEF_DES' })
    await screen.findByRole('dialog')
    await screen.findByText('Lancement : ressources et réunion de lancement')
    await axeSansViolation(r.container)
  })
})
