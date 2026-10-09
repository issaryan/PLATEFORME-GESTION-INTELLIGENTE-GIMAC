import { describe, expect, it } from 'vitest'
import i18next from 'i18next'
import { prepositionDe } from './elision'
import { initI18n } from './i18n'

describe('élision de « de » (recette ADM-07)', () => {
  it('élide devant une voyelle, contracte devant le et les, garde « de » ailleurs', () => {
    expect(prepositionDe('Agenda')).toBe('d’Agenda')
    expect(prepositionDe('Institutions membres')).toBe('d’Institutions membres')
    expect(prepositionDe('Administration de tous les domaines')).toBe('d’Administration de tous les domaines')
    expect(prepositionDe('Organisation')).toBe('d’Organisation')
    expect(prepositionDe('Écrans')).toBe('d’Écrans')
    expect(prepositionDe('Aïssatou Ngo Mbarga')).toBe('d’Aïssatou Ngo Mbarga')
    expect(prepositionDe('Tableau de bord')).toBe('de Tableau de bord')
    expect(prepositionDe('Le Pôle réseau')).toBe('du Pôle réseau')
    expect(prepositionDe('Les institutions')).toBe('des institutions')
    expect(prepositionDe('Hamadou Ali')).toBe('de Hamadou Ali')
  })
  it('formateur i18next « de » : {{nom, de}}', () => {
    initI18n([{ fr: { 'test.droits': 'Droits {{f, de}}' }, en: { 'test.droits': 'Rights of {{f}}' }, es: { 'test.droits': 'Derechos de {{f}}' } }], 'fr')
    expect(i18next.t('test.droits', { f: 'Agenda' })).toBe('Droits d’Agenda')
    expect(i18next.t('test.droits', { f: 'Messagerie' })).toBe('Droits de Messagerie')
  })
})
