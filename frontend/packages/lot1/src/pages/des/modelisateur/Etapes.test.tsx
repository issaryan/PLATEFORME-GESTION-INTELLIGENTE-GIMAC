import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import ReferentielsOperationsPage from '../ReferentielsOperationsPage'

// DES-12, D-89 : modale « Étapes » (ordre, ajout, renommage, couleur, retrait d'une étape vide, refus restitués),
// sélecteur d'étape de la modale d'opération tiré du catalogue de la version, lecture filtrée (bandeau, fantômes
// grisés et non ouvrables), export CSV.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
const MOTIF = '/des/referentiels/operations'
const EDITION = ['HOM_REFERENTIEL_READ', 'HOM_REFERENTIEL_EDIT', 'HOM_REFERENTIEL_PUBLISH']
const ouvrir = (suffixe: string, permissions = EDITION, services = servicesSimules({ latenceMs: 0 })) =>
  ({ ...rendrePage(<ReferentielsOperationsPage />, { chemin: `${MOTIF}${suffixe}`, motif: MOTIF, services, session: sessionTest(permissions) }), services })

describe('DES-12 : étapes configurables (D-89)', () => {
  it('modale Étapes : ajout coloré, déplacement, renommage, retrait ; refus du service ; nouvelle étape dans la modale d’opération', async () => {
    const { container, services } = ouvrir('?version=REF-CI-4')
    const ordonner = vi.spyOn(services.homologation, 'ordonnerEtapes')
    fireEvent.click(await screen.findByRole('button', { name: 'Étapes' }))
    const d = await screen.findByRole('dialog', { name: 'Étapes de la version' })
    const liste = within(d).getByRole('list', { name: 'Étapes, dans leur ordre' })
    expect(within(liste).getAllByRole('listitem')).toHaveLength(11)
    expect(within(liste).getAllByText('Protégée')).toHaveLength(3)
    // Ajout : code contrôlé avant envoi, puis couleur Accent.
    fireEvent.click(within(d).getByRole('button', { name: 'Ajouter une étape' }))
    const form = within(d).getByRole('region', { name: 'Nouvelle étape' })
    fireEvent.click(within(form).getByRole('button', { name: 'Enregistrer l\'étape' }))
    expect(within(form).getByText(/Code invalide/)).toBeTruthy()
    fireEvent.change(within(form).getByLabelText(/^Code/), { target: { value: 'recette_interne' } })
    fireEvent.change(within(form).getByLabelText(/^Libellé/), { target: { value: 'Recette interne' } })
    fireEvent.click(within(form).getByRole('radio', { name: 'Accent' }))
    fireEvent.click(within(form).getByRole('button', { name: 'Enregistrer l\'étape' }))
    await waitFor(() => expect(within(liste).getAllByRole('listitem')).toHaveLength(12))
    expect(screen.getByRole('status').textContent).toBe('Étape Recette interne ajoutée.')
    // Monter la nouvelle étape : PUT …/etapes/ordre avec l'ordre complet.
    fireEvent.click(within(d).getByRole('button', { name: 'Monter Recette interne' }))
    await waitFor(() => expect(ordonner).toHaveBeenCalled())
    expect(ordonner.mock.calls[0][1].slice(-2)).toEqual(['RECETTE_INTERNE', 'SUIVI'])
    await waitFor(() => expect(screen.getByRole('status').textContent).toBe('Recette interne : position 11 sur 12.'))
    expect(within(d).getByRole('button', { name: 'Monter Prise de contact' })).toHaveProperty('disabled', true)
    // Renommer ; retirer une étape protégée est impossible (pas de bouton), une étape vide oui.
    fireEvent.click(within(d).getByRole('button', { name: 'Modifier Recette interne' }))
    const edition = within(d).getByRole('region', { name: 'Modifier l\'étape Recette interne' })
    fireEvent.change(within(edition).getByLabelText(/^Libellé/), { target: { value: 'Recette' } })
    fireEvent.change(within(edition).getByLabelText(/^Étape canonique/), { target: { value: 'TESTS' } })
    fireEvent.click(within(edition).getByRole('button', { name: 'Enregistrer l\'étape' }))
    await waitFor(() => expect(within(liste).getByText('Recette')).toBeTruthy())
    expect(within(d).queryByRole('button', { name: 'Retirer Certification' })).toBeNull()
    // Refus du service restitué : une étape protégée ne change pas d'étape canonique.
    fireEvent.click(within(d).getByRole('button', { name: 'Modifier Certification' }))
    const protegee = within(d).getByRole('region', { name: 'Modifier l\'étape Certification' })
    expect(within(protegee).getByLabelText(/^Étape canonique/)).toHaveProperty('disabled', true)
    vi.spyOn(services.homologation, 'modifierEtape').mockRejectedValueOnce(Object.assign(new Error('x'), { code: 'HOM_ETAPE_CANONIQUE', status: 409, correlationId: 'c-1' }))
    fireEvent.click(within(protegee).getByRole('button', { name: 'Enregistrer l\'étape' }))
    expect(await within(d).findByText(/porte un jalon ou le verrou 2/)).toBeTruthy()
    fireEvent.click(within(protegee).getByRole('button', { name: 'Annuler' }))
    fireEvent.click(within(d).getByRole('button', { name: 'Retirer Prise de contact' }))
    const confirmation = await screen.findByRole('alertdialog')
    fireEvent.click(within(confirmation).getByRole('button', { name: 'Retirer Prise de contact' }))
    await waitFor(() => expect(within(liste).queryByText('Prise de contact')).toBeNull())
    fireEvent.click(within(d).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Étapes de la version' })).toBeNull())
    // Le couloir de la nouvelle étape, coloré ; la modale d'opération propose les étapes de la version.
    await waitFor(() => expect([...container.querySelectorAll('.gm-mod-couloir-etape')].map((c) => c.textContent)).toContain('Recette'))
    fireEvent.click(screen.getByRole('button', { name: 'Modifier l\'opération C11' }))
    const op = await screen.findByRole('dialog', { name: 'Opération C11' })
    const etape = within(op).getByLabelText(/^Étape/) as HTMLSelectElement
    expect([...etape.options].map((o) => o.text)).toContain('Recette')
    expect([...etape.options].map((o) => o.text)).not.toContain('Prise de contact')
    // Scénario complet (une vingtaine d'interactions) : plus de 20 s sous mesure de couverture, comme ParcoursPage.
  }, 60_000)

  it('version publiée : Étapes en consultation seulement ; export CSV', async () => {
    const { services } = ouvrir('?version=REF-CI-3')
    const exporter = vi.spyOn(services.homologation, 'exporterReferentiel')
    const createObjectURL = vi.fn(() => 'blob:x')
    Object.assign(URL, { createObjectURL, revokeObjectURL: vi.fn() })
    fireEvent.click(await screen.findByRole('button', { name: 'Étapes' }))
    const d = await screen.findByRole('dialog', { name: 'Étapes de la version' })
    expect(within(d).queryByRole('button', { name: 'Ajouter une étape' })).toBeNull()
    expect(within(d).queryByRole('button', { name: /^Monter/ })).toBeNull()
    fireEvent.click(within(d).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    fireEvent.click(screen.getByRole('button', { name: 'Exporter (CSV)' }))
    await waitFor(() => expect(exporter).toHaveBeenCalledWith('REF-CI-3'))
    await waitFor(() => expect(createObjectURL).toHaveBeenCalled())
  })

  it('lecture filtrée : bandeau, fantômes grisés et non ouvrables, liste et onglet Opérations', async () => {
    const services = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-res', nom: 'Idriss Mahamat Saleh', profils: ['ROLE_POLE_RESEAU'] } })
    const { container } = ouvrir('?version=REF-CI-4', ['HOM_REFERENTIEL_READ'], services)
    expect(await screen.findByText('Vous voyez les opérations qui vous concernent')).toBeTruthy()
    await waitFor(() => expect(container.querySelectorAll('.gm-mod-carte-fantome').length).toBeGreaterThan(0))
    const fantome = container.querySelector<HTMLElement>('.gm-mod-carte-fantome')!
    expect(fantome.getAttribute('aria-label')).toMatch(/hors de votre périmètre$/)
    expect(within(fantome).queryByRole('button')).toBeNull()
    fireEvent.doubleClick(fantome.closest('.react-flow__node')!)
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Ajouter une opération' })).toBeNull()
    expect([...container.querySelectorAll('.gm-mod-couloir-etape')].map((c) => c.textContent)).toContain('Hors de votre périmètre')
    // Vue liste : le fantôme n'a ni consultation ni arcs.
    cleanup()
    const liste = ouvrir('?version=REF-CI-4&vue=liste', ['HOM_REFERENTIEL_READ'], services)
    const ligne = (await screen.findAllByText('Hors périmètre'))[0].closest('tr')!
    expect(within(ligne).queryByRole('button')).toBeNull()
    expect(liste.container.querySelectorAll('tbody tr').length).toBeGreaterThan(1)
    cleanup()
    ouvrir('?version=REF-CI-4&onglet=operations', ['HOM_REFERENTIEL_READ'], services)
    expect(await screen.findByRole('heading', { name: 'Opérations voisines' })).toBeTruthy()
    expect(screen.getAllByText('Hors de votre périmètre').length).toBeGreaterThan(0)
  })
})
