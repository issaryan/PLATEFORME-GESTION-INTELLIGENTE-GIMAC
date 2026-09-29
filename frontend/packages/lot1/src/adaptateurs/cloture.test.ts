import { describe, expect, it } from 'vitest'
import { etapeDepuis, tailleLisible, versQuatreElements } from './cloture'

const t = (k: string) => `[${k}]`

describe('DES-10 adaptateur des quatre éléments', () => {
  it('reprend l\'état du service sans l\'évaluer', () => {
    const p = versQuatreElements(
      [
        { key: 'responsable', etat: 'REUNI', detail: 'Ingénieur A' },
        { key: 'preuve', etat: 'REUNI' },
        { key: 'horodatage', etat: 'EN_ATTENTE' },
        { key: 'validation', etat: 'MANQUANT', actionManquante: { labelKey: 'cloture.action.valider' } },
      ],
      t,
    )
    expect(p.elements).toEqual({ owner: 'Ingénieur A', proof: true, timestamp: 'pending', validation: false })
    expect(p.missing).toEqual({ timestamp: '[cloture.element.horodatage.EN_ATTENTE]', validation: '[cloture.element.validation.MANQUANT]' })
    expect(p.actions).toEqual({ validation: '[cloture.action.valider]' })
  })
  it('lit l\'étape dans l\'adresse et retombe sur la preuve', () => {
    expect(etapeDepuis('signature')).toBe('signature')
    expect(etapeDepuis('inconnue')).toBe('preuve')
    expect(etapeDepuis(null)).toBe('preuve')
  })
  it('affiche une taille lisible', () => {
    expect(tailleLisible(2048, 'fr')).toBe('2 Ko')
    expect(tailleLisible(1.5 * 1024 * 1024, 'en')).toBe('1.5 Mo')
  })
})
