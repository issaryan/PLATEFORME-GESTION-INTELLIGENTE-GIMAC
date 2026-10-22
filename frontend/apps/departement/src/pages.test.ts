import { describe, expect, it } from 'vitest'
import { PAGES } from './pages'

// Chaque route de l'application charge une page existante : un import cassé ou un export par défaut manquant échoue ici.
describe('pages chargées à la demande', () => {
  it.each(Object.entries(PAGES))('%s se charge et exporte un composant', async (_nom, charger) => {
    const module = await charger()
    expect(typeof module.default).toBe('function')
  })
})
