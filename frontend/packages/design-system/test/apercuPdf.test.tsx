// Aperçu PDF par pdf.js (D-73) : pages dessinées en canevas, sans cadre blob:, nombre de pages borné,
// document illisible signalé. La bibliothèque est simulée (jsdom ne dessine pas).
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { ApercuPdf, PAGES_MAX_APERCU } from '../src/domaine/Messagerie/ApercuPdf'
import { AttachmentViewer } from '../src'

const pdf = vi.hoisted(() => ({ total: 2, echec: false, rendus: 0, detruit: 0 }))
vi.mock('pdfjs-dist/legacy/build/pdf.worker.min.mjs?url', () => ({ default: '/assets/pdf.worker.mjs' }))
vi.mock('pdfjs-dist/legacy/build/pdf.mjs', () => ({
  GlobalWorkerOptions: { workerSrc: '' },
  getDocument: () => ({
    destroy: () => {
      pdf.detruit++
    },
    promise: pdf.echec
      ? Promise.reject(new Error('PDF invalide'))
      : Promise.resolve({
          numPages: pdf.total,
          getPage: async () => ({
            getViewport: ({ scale }: { scale: number }) => ({ width: 600 * scale, height: 800 * scale }),
            render: () => {
              pdf.rendus++
              return { promise: Promise.resolve() }
            },
          }),
        }),
  }),
}))

const blob = () => new Blob(['%PDF-1.4'], { type: 'application/pdf' })
afterEach(() => {
  cleanup()
  Object.assign(pdf, { total: 2, echec: false, rendus: 0, detruit: 0 })
})

describe('ApercuPdf', () => {
  it('dessine chaque page dans un canevas annoncé « Page n sur N », sans cadre', async () => {
    const { container, unmount } = render(<ApercuPdf blob={blob()} titre="rapport.pdf" />)
    expect(screen.getByRole('document', { name: 'Aperçu de rapport.pdf' }).getAttribute('aria-busy')).toBe('true')
    expect(await screen.findByRole('img', { name: 'Page 2 sur 2' })).toBeTruthy()
    expect(screen.getByRole('img', { name: 'Page 1 sur 2' }).tagName).toBe('CANVAS')
    expect(container.querySelector('iframe')).toBeNull()
    await waitFor(() => expect(screen.getByRole('document').getAttribute('aria-busy')).toBe('false'))
    expect(pdf.rendus).toBe(2)
    unmount()
    expect(pdf.detruit).toBe(1)
  })
  it('très gros document : pages bornées, invitation à télécharger', async () => {
    pdf.total = PAGES_MAX_APERCU + 7
    render(<ApercuPdf blob={blob()} titre="gros.pdf" />)
    expect(await screen.findByText(`Aperçu des ${PAGES_MAX_APERCU} premières pages sur ${PAGES_MAX_APERCU + 7} : téléchargez le document pour le lire en entier.`)).toBeTruthy()
    expect(pdf.rendus).toBe(PAGES_MAX_APERCU)
  })
  it('document illisible : message, pas de page', async () => {
    pdf.echec = true
    render(<ApercuPdf blob={blob()} titre="abime.pdf" />)
    expect(await screen.findByText('Ce PDF ne peut pas être affiché')).toBeTruthy()
    expect(screen.queryByRole('img')).toBeNull()
  })
  it('fenêtre d’aperçu : un PDF passe par pdf.js, même servi en octets bruts', async () => {
    render(<AttachmentViewer open nom="pv.pdf" typeMime="application/octet-stream" blob={blob()} onClose={() => undefined} onTelecharger={() => undefined} />)
    expect(await screen.findByRole('img', { name: 'Page 1 sur 2' })).toBeTruthy()
    expect(document.querySelector('iframe')).toBeNull()
  })
})
