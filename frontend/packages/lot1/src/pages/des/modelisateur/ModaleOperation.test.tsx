import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import i18next from 'i18next'
import { ApiError, initI18n } from '@gimac/socle'
import { catalogues } from '../../../i18n'
import { CATALOGUE, calculerGraphe, versionsInitiales } from '../../../services/simule/referentiels'
import { operationVierge } from '../../../adaptateurs/modelisateur'
import type { ReferenceOperation } from '../../../modele/homologation'
import { ModaleOperation } from './ModaleOperation'

// Modale de l'opération (D-77, §6) : validations avant envoi (V ≠ R, déclencheur requis, secret sans valeur,
// livrables), onglets, refus du service lisible, lecture seule.
afterEach(cleanup)
if (!i18next.isInitialized) initI18n(catalogues, 'fr')
const { versions, operations } = versionsInitiales()
const graphe = calculerGraphe(versions[1], operations['REF-CI-4'])
const op = (code: string) => graphe.noeuds.find((n) => n.code === code)!
const monter = (o: Partial<Parameters<typeof ModaleOperation>[0]> & { operation: ReferenceOperation }) => {
  const enregistrer = vi.fn()
  const supprimer = vi.fn()
  const fermer = vi.fn()
  render(<ModaleOperation creation={false} lectureSeule={false} catalogue={CATALOGUE} noeuds={graphe.noeuds} onEnregistrer={enregistrer} onSupprimer={supprimer} onFermer={fermer} {...o} />)
  return { enregistrer, supprimer, fermer, d: screen.getByRole('dialog') }
}
const onglet = (d: HTMLElement, nom: string) => fireEvent.click(within(d).getByRole('tab', { name: new RegExp(`^${nom}`) }))

describe('Modale de l\'opération', () => {
  it('création : champs requis, code invalide ou existant, V ≠ R ; le premier onglet en faute est montré', () => {
    const { d, enregistrer } = monter({ operation: operationVierge(), creation: true })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l\'opération' }))
    expect(enregistrer).not.toHaveBeenCalled()
    expect(within(d).getAllByText('Ce champ est obligatoire.').length).toBeGreaterThan(1)
    expect(within(d).getByRole('tab', { name: 'Rôles (2)' })).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: 'a b' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l\'opération' }))
    expect(within(d).getByText(/Code invalide/)).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: 'c1' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l\'opération' }))
    expect(within(d).getByText('Ce code d\'opération existe déjà dans cette version.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: 'NOUVELLE-1' } })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Revue' } })
    fireEvent.change(within(d).getByLabelText(/^Étape/), { target: { value: 'TESTS' } })
    fireEvent.change(within(d).getByLabelText(/^Ordre/), { target: { value: '4' } })
    fireEvent.change(within(d).getByLabelText(/^Description/), { target: { value: 'Relire' } })
    onglet(d, 'Rôles')
    fireEvent.change(within(d).getByLabelText(/Responsable \(R\)/), { target: { value: 'SNA' } })
    fireEvent.change(within(d).getByLabelText(/Validateur \(V\)/), { target: { value: 'SNA' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l\'opération' }))
    expect(within(d).getByText('Le validateur doit être distinct du responsable.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/Validateur \(V\)/), { target: { value: 'CONDUCTEUR' } })
    // Qualités C, I, E par jetons (suggestions filtrées par la saisie).
    const consultes = within(d).getByRole('combobox', { name: 'Consultés (C)' })
    fireEvent.change(consultes, { target: { value: 'réseau' } })
    fireEvent.mouseDown(within(screen.getAllByRole('listbox').at(-1)!).getByRole('option', { name: 'Pôle réseau' }))
    fireEvent.change(within(d).getByRole('combobox', { name: 'Externes (E)' }), { target: { value: '' } })
    fireEvent.focus(within(d).getByRole('combobox', { name: 'Externes (E)' }))
    fireEvent.mouseDown(within(screen.getAllByRole('listbox').at(-1)!).getByRole('option', { name: 'Participant' }))
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l\'opération' }))
    expect(enregistrer).toHaveBeenCalledWith(expect.objectContaining({ code: 'NOUVELLE-1', libelle: 'Revue', etape: 'TESTS', ordre: 4, responsable: 'SNA', validateur: 'CONDUCTEUR',
      consultes: ['POLE_RESEAU'], externes: ['PARTICIPANT'], description: 'Relire' }))
  })

  it('exécution : déclencheur exigé par un événement externe ; secret désactive la valeur de référence', () => {
    const { d, enregistrer } = monter({ operation: op('12') })
    onglet(d, 'Exécution')
    expect((within(d).getByLabelText(/^Déclencheur/) as HTMLSelectElement).disabled).toBe(true)
    fireEvent.change(within(d).getByLabelText(/^Geste/), { target: { value: 'EVENEMENT_EXTERNE' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(within(d).getByText('Un événement d\'un autre service exige son déclencheur.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Déclencheur/), { target: { value: 'certification.pv-signed.v1' } })
    expect((within(d).getByLabelText(/^Valeur de référence/) as HTMLInputElement).value).not.toBe('')
    fireEvent.click(within(d).getByLabelText('Secret (aucune valeur saisie)'))
    expect((within(d).getByLabelText(/^Valeur de référence/) as HTMLInputElement).disabled).toBe(true)
    for (const c of ['Clé cryptographique (trace et code de contrôle seulement)', 'Oubli sans symptôme à la bascule', 'Pièce pivot']) fireEvent.click(within(d).getByLabelText(c))
    for (const [champ, v] of [['Jalon', 'CERTIFIEE'], ['Particularité', 'FIN_ESSAIS'], ['Plateforme d\'exécution', 'SIMULATEUR'], ['Grain de preuve', 'PHASE']]) {
      fireEvent.change(within(d).getByLabelText(new RegExp(`^${champ}`)), { target: { value: v } })
    }
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(enregistrer).toHaveBeenCalledWith(expect.objectContaining({
      geste: 'EVENEMENT_EXTERNE', declencheur: 'certification.pv-signed.v1', secret: true, valeurReference: null, cle: true, oubliSansSymptome: true, pivot: true,
      jalon: 'CERTIFIEE', particularite: 'FIN_ESSAIS', plateforme: 'SIMULATEUR', grain: 'PHASE',
    }))
  })

  it('livrables et preuves : ajout, champs requis, code unique, retrait', () => {
    const { d, enregistrer } = monter({ operation: op('15') })
    onglet(d, 'Livrables et preuves')
    expect(within(d).getByRole('group', { name: 'Livrable 1' })).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Ajouter un livrable ou une preuve' }))
    const l2 = within(d).getByRole('group', { name: 'Livrable 2' })
    fireEvent.change(within(l2).getByLabelText(/^Code/), { target: { value: 'l1' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(within(l2).getByText('Ce code est déjà employé par un autre livrable de l\'opération.')).toBeTruthy()
    expect(within(l2).getByText('Ce champ est obligatoire.')).toBeTruthy()
    fireEvent.change(within(l2).getByLabelText(/^Code/), { target: { value: 'p2' } })
    fireEvent.change(within(l2).getByLabelText(/^Libellé/), { target: { value: 'Journal des essais' } })
    fireEvent.change(within(l2).getByLabelText(/^Nature/), { target: { value: 'PREUVE' } })
    fireEvent.change(within(l2).getByLabelText(/^Déposant/), { target: { value: 'PARTICIPANT' } })
    fireEvent.focus(within(l2).getByRole('combobox', { name: 'Types de pièces admis' }))
    fireEvent.mouseDown(within(screen.getAllByRole('listbox').at(-1)!).getByRole('option', { name: 'Archive ZIP' }))
    fireEvent.click(within(l2).getByLabelText('Obligatoire'))
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(enregistrer.mock.calls[0][0].livrables[1]).toEqual({ code: 'P2', libelle: 'Journal des essais', nature: 'PREUVE', deposant: 'PARTICIPANT', typesPieces: ['application/pdf', 'application/zip'], obligatoire: false })
    fireEvent.click(within(d).getByRole('button', { name: 'Retirer le livrable 1' }))
    expect(within(d).queryByRole('group', { name: 'Livrable 2' })).toBeNull()
    fireEvent.click(within(d).getByRole('button', { name: 'Retirer le livrable 1' }))
    expect(within(d).getByText('Aucun livrable ni preuve attendu.')).toBeTruthy()
  })

  it('chronologie : prédécesseurs, successeurs calculés, délai borné, dates au plus tôt', () => {
    const { d, enregistrer } = monter({ operation: op('C2') })
    onglet(d, 'Chronologie')
    expect(within(d).getByText('C10, C3, C4')).toBeTruthy()
    expect(within(d).getAllByText(/^jour \d+$/).length).toBe(2)
    fireEvent.change(within(d).getByLabelText(/^Délai cible/), { target: { value: '400' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(within(d).getByText('Délai entier de 0 à 365 jours.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Délai cible/), { target: { value: '5' } })
    // D-106 : délai maximal, au moins le délai cible.
    fireEvent.change(within(d).getByLabelText(/^Délai maximal/), { target: { value: '4' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(within(d).getByText('Délai maximal entier, du délai cible à 365 jours.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Délai maximal/), { target: { value: '8' } })
    fireEvent.focus(within(d).getByRole('combobox', { name: 'Prédécesseurs' }))
    fireEvent.mouseDown(within(screen.getAllByRole('listbox').at(-1)!).getByRole('option', { name: /^C1 · / }))
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(enregistrer).toHaveBeenCalledWith(expect.objectContaining({ delaiJours: 5, delaiMaxJours: 8, predecesseurs: ['26', 'C1'] }))
  })

  it('refus du service lisible ; suppression confirmée ; annulation', () => {
    const { d, supprimer, fermer } = monter({ operation: op('C2'), erreurServeur: new ApiError(409, 'HOM_REFERENTIAL_FROZEN', 'x', 'cid-gel') })
    expect(within(d).getByText('Une version publiée est figée : dérivez un brouillon pour la modifier.')).toBeTruthy()
    expect(within(d).getByText(/cid-gel/)).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Supprimer l\'opération' }))
    const c = screen.getByRole('alertdialog')
    fireEvent.click(within(c).getByRole('button', { name: 'Supprimer l\'opération' }))
    expect(supprimer).toHaveBeenCalled()
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    expect(fermer).toHaveBeenCalled()
  })

  it('lecture seule : champs figés, jetons en lecture, aucune action d\'écriture', () => {
    const { d } = monter({ operation: op('30'), lectureSeule: true })
    expect(within(d).getByText(/lecture seule/)).toBeTruthy()
    expect((within(d).getByLabelText(/^Libellé/) as HTMLInputElement).readOnly).toBe(true)
    onglet(d, 'Rôles')
    expect(within(d).getAllByText('Participant').length).toBeGreaterThan(0)
    onglet(d, 'Livrables et preuves')
    expect(within(d).queryByRole('button', { name: /Ajouter un livrable/ })).toBeNull()
    expect(within(d).queryByRole('button', { name: 'Enregistrer' })).toBeNull()
    expect(within(d).queryByRole('button', { name: 'Supprimer l\'opération' })).toBeNull()
  })
})
