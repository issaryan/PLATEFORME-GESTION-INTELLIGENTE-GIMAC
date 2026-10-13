import { describe, expect, it } from 'vitest'
import { ApiError } from '@gimac/socle'
import { siAbsente } from './homologation'

describe('siAbsente : lecture secondaire tolérante à une route absente', () => {
  it('rend la valeur lue quand la route répond', async () => {
    await expect(siAbsente(Promise.resolve([1, 2]), [])).resolves.toEqual([1, 2])
  })
  it('rend la valeur neutre sur un 404', async () => {
    await expect(siAbsente(Promise.reject(new ApiError(404, 'HTTP_404', '', '')), null)).resolves.toBeNull()
  })
  it('laisse passer toute autre erreur', async () => {
    const e = new ApiError(500, 'HTTP_500', 'panne', '')
    await expect(siAbsente(Promise.reject(e), [])).rejects.toBe(e)
    await expect(siAbsente(Promise.reject(new Error('réseau')), [])).rejects.toThrow('réseau')
  })
})
