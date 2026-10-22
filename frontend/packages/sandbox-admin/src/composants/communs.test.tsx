import { describe, expect, it } from 'vitest'
import { renderHook } from '@testing-library/react'
import i18next from 'i18next'
import { ApiError, initI18n } from '@gimac/socle'
import { catalogues as cataloguesLot1 } from '@gimac/lot1'
import { catalogues } from '../i18n'
import { etatListe, useMessageErreur } from './communs'

// Refus traduits par leur code (problèmes RFC 9457 de SBX-ESPACE) et états des listes.
describe('communs de l’espace sandbox', () => {
  it('refus : code traduit, code inconnu avec ou sans message, erreur quelconque', () => {
    if (!i18next.isInitialized) initI18n([...cataloguesLot1, ...catalogues], 'fr')
    else void i18next.changeLanguage('fr')
    const { result } = renderHook(() => useMessageErreur())
    expect(result.current(new ApiError(409, 'SBX_ESP_NON_RETIRE', 'x', 'c-1'))).toEqual({ title: 'L’institution n’est pas retirée du registre', text: undefined, correlationId: 'c-1' })
    expect(result.current(new ApiError(500, 'HTTP_500', 'panne', ''))).toEqual({ title: 'La sandbox a refusé l’opération.', text: 'panne', correlationId: undefined })
    expect(result.current(new ApiError(500, 'HTTP_500', '', ''))).toEqual({ title: 'La sandbox a refusé l’opération.', text: undefined, correlationId: undefined })
    expect(result.current(new Error('x'))).toEqual({ title: 'La sandbox a refusé l’opération.' })
  })

  it('état d’une liste : chargement, erreur, vide, rempli', () => {
    expect(etatListe({ isPending: true, isError: false }, [])).toEqual({ state: 'loading' })
    expect(etatListe({ isPending: false, isError: true }, [])).toEqual({ state: 'error' })
    expect(etatListe({ isPending: false, isError: false }, [])).toEqual({ state: 'empty' })
    expect(etatListe({ isPending: false, isError: false }, [1])).toEqual({ state: 'ok' })
  })
})
