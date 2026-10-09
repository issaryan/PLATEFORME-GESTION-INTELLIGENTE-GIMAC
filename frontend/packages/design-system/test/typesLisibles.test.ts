import { describe, expect, it } from 'vitest'
import { typesLisibles } from '../src/saisie/FileDropzone/FileDropzone'

// QUO-07 (recette du travail quotidien) : les types de fichier admis ne s'affichent jamais en type MIME brut.
describe('typesLisibles', () => {
  it('nomme les types courants, les familles et les extensions', () => {
    expect(typesLisibles('application/pdf,image/*', (f) => (f === 'image' ? 'images' : f))).toBe('PDF, images')
    expect(typesLisibles(['application/vnd.openxmlformats-officedocument.wordprocessingml.document', '.csv', 'text/plain'])).toBe('Word, CSV, TXT')
    expect(typesLisibles('application/x-pkcs12')).toBe('PKCS12')
    expect(typesLisibles('')).toBe('')
  })
})
