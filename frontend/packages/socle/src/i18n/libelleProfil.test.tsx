import { describe, expect, it } from 'vitest'
import { renderHook } from '@testing-library/react'
import { I18nextProvider } from 'react-i18next'
import i18next from 'i18next'
import type { ReactNode } from 'react'
import { useLibelleProfil } from './libelleProfil'

// Libellé d'un profil, unique pour toutes les applications (D-86) : serveur d'abord, catalogue i18n ensuite, code à défaut.
const i18n = i18next.createInstance()
void i18n.init({ lng: 'fr', resources: { fr: { translation: { 'compte.profil.ROLE_DG': 'Directeur général' } } } })
const enveloppe = ({ children }: { children: ReactNode }) => <I18nextProvider i18n={i18n}>{children}</I18nextProvider>

describe('useLibelleProfil', () => {
  it('libellé de l’appel, puis catalogue du serveur (liste ou table), puis i18n, puis code', () => {
    const liste = renderHook(() => useLibelleProfil([{ code: 'ROLE_X', libelle: 'Profil X' }, { code: 'ROLE_DG', libelle: 'ROLE_DG' }]), { wrapper: enveloppe }).result.current
    expect(liste('ROLE_X')).toBe('Profil X')
    expect(liste('ROLE_X', 'Libellé direct')).toBe('Libellé direct')
    // Libellé égal au code (attribut absent) : catalogue i18n.
    expect(liste('ROLE_DG')).toBe('Directeur général')
    const table = renderHook(() => useLibelleProfil({ ROLE_Y: 'Profil Y' }), { wrapper: enveloppe }).result.current
    expect(table('ROLE_Y')).toBe('Profil Y')
    const seul = renderHook(() => useLibelleProfil(), { wrapper: enveloppe }).result.current
    expect(seul('ROLE_DG')).toBe('Directeur général')
    expect(seul('ROLE_INCONNU')).toBe('ROLE_INCONNU')
  })
})
