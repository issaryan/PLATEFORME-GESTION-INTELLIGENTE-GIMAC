import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import ReferentielsOperationsPage from '../ReferentielsOperationsPage'

// DES-12, onglet « Modélisateur » (D-77, §6) : canevas réel (@xyflow/react sous jsdom), vue liste équivalente,
// barre d'outils, refus du service restitués (cycle), lecture seule d'une version publiée.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
const MOTIF = '/des/referentiels/operations'
const EDITION = ['HOM_REFERENTIEL_READ', 'HOM_REFERENTIEL_EDIT', 'HOM_REFERENTIEL_PUBLISH', 'HOM_REFERENTIEL_MIGRATE']
const ouvrir = (suffixe: string, permissions = EDITION, services = servicesSimules({ latenceMs: 0 })) =>
  ({ ...rendrePage(<ReferentielsOperationsPage />, { chemin: `${MOTIF}${suffixe}`, motif: MOTIF, services, session: sessionTest(permissions) }), services })

describe('DES-12 : modélisateur', () => {
  it('canevas d\'un brouillon : cartes, couloirs, anomalie en rouge, chemin critique, outils d\'édition', async () => {
    const { container } = ouvrir('?version=REF-CI-4')
    expect(await screen.findByTestId('modelisateur-canevas')).toBeTruthy()
    await waitFor(() => expect(container.querySelectorAll('.gm-mod-carte').length).toBeGreaterThan(40))
    const c11 = container.querySelector('.gm-mod-carte[data-code="C11"]')!
    expect(c11.className).toContain('gm-mod-carte-anomalie')
    expect(c11.textContent).toContain('Validateur absent')
    expect(container.querySelectorAll('.gm-mod-carte-critique').length).toBeGreaterThan(5)
    // Couloirs = étapes de la version (D-89), dans leur ordre, colorés par leur jeton ; les étapes vides gardent leur couloir.
    const couloirs = [...container.querySelectorAll<HTMLElement>('.gm-mod-couloir-etape')]
    expect(couloirs.map((c) => c.textContent).slice(0, 3)).toEqual(['Prise de contact', 'Adhésion', 'Cadrage'])
    expect(couloirs[2].style.getPropertyValue('--gm-mod-couloir-couleur')).toBe('var(--gm-color-status-info-fg)')
    expect(container.querySelector('.gm-mod-carte[data-code="17"]')?.textContent).toContain('Apte à la certification')
    const outils = screen.getByRole('toolbar', { name: 'Outils du modélisateur' })
    for (const n of ['Ajouter une opération', 'Disposition automatique', 'Contrôler', 'Comparer', 'Publier la version']) expect(within(outils).getByRole('button', { name: n })).toBeTruthy()
    expect(within(outils).getByRole('combobox', { name: /Pilote du brouillon/ })).toBeTruthy()
    expect(screen.getByText(/Chemin critique : \d+ jours/)).toBeTruthy()
  })

  it('double-clic ou « Modifier » sur une carte : modale de l\'opération, enregistrement (PUT)', async () => {
    const { container, services } = ouvrir('?version=REF-CI-4')
    const enregistrer = vi.spyOn(services.homologation, 'enregistrerOperation')
    fireEvent.click(await screen.findByRole('button', { name: 'Modifier l\'opération C11' }))
    const d = await screen.findByRole('dialog', { name: 'Opération C11' })
    fireEvent.click(within(d).getByRole('tab', { name: 'Rôles' }))
    fireEvent.change(within(d).getByLabelText(/Validateur \(V\)/), { target: { value: 'CHEF_DES' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    await waitFor(() => expect(enregistrer).toHaveBeenCalledWith('REF-CI-4', 'C11', expect.objectContaining({ validateur: 'CHEF_DES', responsable: 'PRODUCTION_MONETIQUE' })))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Opération C11' })).toBeNull())
    await waitFor(() => expect(container.querySelector('.gm-mod-carte[data-code="C11"]')?.className).not.toContain('gm-mod-carte-anomalie'))
    fireEvent.doubleClick(container.querySelector('.react-flow__node[data-id="17"]')!)
    expect(await screen.findByRole('dialog', { name: 'Opération 17' })).toBeTruthy()
  })

  it('ajout d\'une opération (POST), puis suppression confirmée (DELETE)', async () => {
    const { services } = ouvrir('?version=REF-CI-4&vue=liste')
    const creer = vi.spyOn(services.homologation, 'creerOperation')
    const supprimer = vi.spyOn(services.homologation, 'supprimerOperation')
    fireEvent.click(await screen.findByRole('button', { name: 'Ajouter une opération' }))
    const d = await screen.findByRole('dialog', { name: 'Nouvelle opération' })
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: 'c12' } })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Revue des journaux du switch' } })
    fireEvent.change(within(d).getByLabelText(/^Étape/), { target: { value: 'MISE_EN_PRODUCTION' } })
    fireEvent.click(within(d).getByRole('tab', { name: 'Rôles' }))
    fireEvent.change(within(d).getByLabelText(/Responsable \(R\)/), { target: { value: 'PRODUCTION_MONETIQUE' } })
    fireEvent.change(within(d).getByLabelText(/Validateur \(V\)/), { target: { value: 'CONDUCTEUR' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l\'opération' }))
    await waitFor(() => expect(creer).toHaveBeenCalledWith('REF-CI-4', expect.objectContaining({ code: 'C12', libelle: 'Revue des journaux du switch', etape: 'MISE_EN_PRODUCTION', segment: 'BRANCHE' })))
    fireEvent.change(screen.getByRole('searchbox', { name: 'Filtrer les opérations (code ou libellé)' }), { target: { value: 'journaux du switch' } })
    expect(await screen.findByRole('cell', { name: 'Revue des journaux du switch' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Modifier l\'opération C12' }))
    const m = await screen.findByRole('dialog', { name: 'Opération C12' })
    fireEvent.click(within(m).getByRole('button', { name: 'Supprimer l\'opération' }))
    const confirmation = await screen.findByRole('alertdialog')
    fireEvent.click(within(confirmation).getByRole('button', { name: 'Supprimer l\'opération' }))
    await waitFor(() => expect(supprimer).toHaveBeenCalledWith('REF-CI-4', 'C12'))
    await waitFor(() => expect(screen.queryByRole('cell', { name: 'Revue des journaux du switch' })).toBeNull())
  })

  it('vue liste : relier au clavier (PUT /arcs), cycle refusé et expliqué, délier', async () => {
    const { services } = ouvrir('?version=REF-CI-4&vue=liste')
    const arc = vi.spyOn(services.homologation, 'arcReferentiel')
    fireEvent.click(await screen.findByRole('button', { name: 'Arcs de l\'opération 3' }))
    let d = await screen.findByRole('dialog', { name: 'Arcs de l\'opération 3' })
    expect(within(d).getByText('Aucun prédécesseur : opération d\'entrée.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText('Ajouter un prédécesseur'), { target: { value: '35' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Relier' }))
    await waitFor(() => expect(arc).toHaveBeenCalledWith('REF-CI-4', { de: '35', vers: '3', present: true }))
    expect(await screen.findByText(/fermerait un cycle/)).toBeTruthy()
    fireEvent.click(within(d).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    fireEvent.change(screen.getByRole('searchbox', { name: 'Filtrer les opérations (code ou libellé)' }), { target: { value: 'c11' } })
    fireEvent.click(await screen.findByRole('button', { name: 'Arcs de l\'opération C11' }))
    d = await screen.findByRole('dialog', { name: 'Arcs de l\'opération C11' })
    fireEvent.click(within(d).getByRole('button', { name: 'Retirer l\'arc C8 vers C11' }))
    await waitFor(() => expect(arc).toHaveBeenCalledWith('REF-CI-4', { de: 'C8', vers: 'C11', present: false }))
    await waitFor(() => expect(within(d).queryByRole('button', { name: 'Retirer l\'arc C8 vers C11' })).toBeNull())
    fireEvent.change(within(d).getByLabelText('Ajouter un prédécesseur'), { target: { value: 'C7' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Relier' }))
    await waitFor(() => expect(arc).toHaveBeenCalledWith('REF-CI-4', { de: 'C7', vers: 'C11', present: true }))
    expect(await within(d).findByRole('button', { name: 'Retirer l\'arc C7 vers C11' })).toBeTruthy()
  })

  it('contrôler : anomalies listées et surlignées ; disposition automatique (PUT /positions) ; pilote ; comparer', async () => {
    const { container, services } = ouvrir('?version=REF-CI-4')
    const placer = vi.spyOn(services.homologation, 'placerOperations')
    const pilote = vi.spyOn(services.homologation, 'designerPilote')
    fireEvent.click(await screen.findByRole('button', { name: 'Contrôler' }))
    const anomalie = await screen.findByRole('button', { name: /Validateur absent · C11/ })
    fireEvent.click(anomalie)
    await waitFor(() => expect(container.querySelector('.gm-mod-carte[data-code="C11"]')?.className).toContain('gm-mod-carte-surlignee'))
    fireEvent.click(screen.getByRole('button', { name: 'Masquer le contrôle' }))
    fireEvent.click(screen.getByRole('button', { name: 'Disposition automatique' }))
    await waitFor(() => expect(placer).toHaveBeenCalled())
    expect(placer.mock.calls[0][1].length).toBeGreaterThan(40)
    expect(await screen.findByText('Disposition automatique enregistrée.')).toBeTruthy()
    fireEvent.change(screen.getByRole('combobox', { name: /Pilote du brouillon/ }), { target: { value: 'U-102' } })
    await waitFor(() => expect(pilote).toHaveBeenCalledWith('REF-CI-4', 'U-102'))
    expect(await screen.findByText('Pilote : Arnaud Mvogo Eyenga')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Comparer' }))
    await waitFor(() => expect(document.querySelector('.gm-diff-del')).not.toBeNull())
  })

  it('contrôler en vue liste : la ligne de l\'opération reçoit le focus ; publier depuis la barre d\'outils', async () => {
    const { services } = ouvrir('?version=REF-CI-4&vue=liste')
    const publier = vi.spyOn(services.homologation, 'publierReferentiel')
    fireEvent.click(await screen.findByRole('button', { name: 'Contrôler' }))
    fireEvent.click(await screen.findByRole('button', { name: /Validateur absent · C11/ }))
    await waitFor(() => expect(document.activeElement?.getAttribute('aria-label')).toBe('Modifier l\'opération C11'))
    fireEvent.click(within(screen.getByRole('toolbar', { name: 'Outils du modélisateur' })).getByRole('button', { name: 'Publier la version' }))
    const d = await screen.findByRole('alertdialog')
    fireEvent.click(within(d).getByRole('button', { name: 'Publier la version' }))
    await waitFor(() => expect(publier).toHaveBeenCalledWith('REF-CI-4'))
  })

  it('version publiée : lecture seule (aucun outil d\'édition, modale en consultation)', async () => {
    const { container } = ouvrir('?version=REF-CI-3')
    await waitFor(() => expect(container.querySelectorAll('.gm-mod-carte').length).toBeGreaterThan(40))
    expect(screen.queryByRole('button', { name: 'Ajouter une opération' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Disposition automatique' })).toBeNull()
    expect(screen.queryByRole('combobox', { name: /Pilote/ })).toBeNull()
    expect(screen.getAllByText(/lecture seule/).length).toBeGreaterThan(0)
    fireEvent.click(screen.getByRole('button', { name: 'Consulter l\'opération 19' }))
    const d = await screen.findByRole('dialog', { name: 'Opération 19' })
    expect(within(d).queryByRole('button', { name: 'Enregistrer' })).toBeNull()
    fireEvent.click(within(d).getByRole('tab', { name: 'Exécution' }))
    expect((within(d).getByLabelText(/^Déclencheur/) as HTMLSelectElement).value).toBe('certification.pv-signed.v1')
    expect((within(d).getByLabelText(/^Jalon/) as HTMLSelectElement).value).toBe('CERTIFIEE')
    fireEvent.click(within(d).getByRole('tab', { name: 'Chronologie' }))
    expect(within(d).getByText('20')).toBeTruthy()
    fireEvent.click(within(d).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
  })

  it('lecteur seul : le brouillon s\'affiche en lecture ; erreur du graphe expliquée', async () => {
    ouvrir('?version=REF-CI-4&vue=liste', ['HOM_REFERENTIEL_READ'])
    fireEvent.change(await screen.findByRole('searchbox', { name: 'Filtrer les opérations (code ou libellé)' }), { target: { value: 'C11' } })
    expect(await screen.findByRole('button', { name: 'Consulter l\'opération C11' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: /^Arcs/ })).toBeNull()
    cleanup()
    const ko = servicesSimules({ latenceMs: 0 })
    ko.homologation.grapheReferentiel = async () => { throw Object.assign(new Error('x'), { status: 503, code: 'HTTP_503', correlationId: 'cid-graphe' }) }
    ouvrir('?version=REF-CI-4', EDITION, ko)
    expect((await screen.findByRole('alert')).textContent).toContain('cid-graphe')
  })
})
