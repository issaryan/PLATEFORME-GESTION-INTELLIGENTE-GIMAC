import { afterEach, describe, expect, it, vi } from 'vitest'
import { telechargerPdfInstance } from './pdf'
import { telechargerProcessusPdf } from '../../../services/http/processus-pdf'
import type { ServiceProcessus } from '../../../services/ports'

afterEach(() => vi.restoreAllMocks())

describe('PDF de processus', () => {
  it('appelle la route PDF et déclenche le téléchargement avec la référence', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('%PDF-1.4', { status: 200, headers: { 'Content-Type': 'application/pdf' } }))
    const blob = await telechargerProcessusPdf('I/1')
    expect(blob.type).toBe('application/pdf')
    expect(new URL(String(fetchMock.mock.calls[0][0])).pathname).toBe('/bff/des/pilotage/processus/instances/I%2F1/pdf')
    const creation = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test')
    const revocation = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
    const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
    const service = { telechargerPdf: vi.fn().mockResolvedValue(blob) } as unknown as ServiceProcessus
    await telechargerPdfInstance(service, { id: 'I1', reference: 'CHG-2026-10-28-000001' })
    expect(clic).toHaveBeenCalled()
    expect(revocation).toHaveBeenCalledWith('blob:test')
    const a = clic.mock.instances[0] as HTMLAnchorElement
    expect(a.download).toBe('CHG-2026-10-28-000001.pdf')
    await telechargerPdfInstance(service, { id: 'I2', reference: null }, 'en')
    expect((clic.mock.instances[1] as HTMLAnchorElement).download).toBe('I2.pdf')
    expect(service.telechargerPdf).toHaveBeenNthCalledWith(1, 'I1')
    expect(service.telechargerPdf).toHaveBeenNthCalledWith(2, 'I2', 'en')
    expect(creation).toHaveBeenCalledTimes(2)
    // Langue de l'interface transmise à MS-PILOTAGE (D-139).
    await telechargerProcessusPdf('I1', 'es')
    expect(new URL(String(fetchMock.mock.calls[1][0])).search).toBe('?langue=es')
  })

  it('ne fabrique pas d’URL si le service refuse', async () => {
    const creation = vi.spyOn(URL, 'createObjectURL')
    const service = { telechargerPdf: vi.fn().mockRejectedValue(new Error('refus')) } as unknown as ServiceProcessus
    await expect(telechargerPdfInstance(service, { id: 'I1', reference: null })).rejects.toThrow('refus')
    expect(creation).not.toHaveBeenCalled()
  })
})
