import { useEffect, useRef, useState } from 'react'
import adresseTravailleur from 'pdfjs-dist/legacy/build/pdf.worker.min.mjs?url'
import { InlineAlert } from '../../conteneurs/InlineAlert/InlineAlert'
import { Skeleton } from '../../affichage/Skeleton/Skeleton'
import { useLibelle } from '../../i18n/LangueProvider'

// Aperçu d'un PDF (D-73) : pages dessinées par pdf.js dans des canevas, sans cadre ni adresse blob:. Un
// cadre blob: hérite de la politique de sécurité de la page (style-src 'self'), qui bloque la visionneuse
// intégrée d'Edge et de Chrome ; ici rien n'est exécuté du document (ni script, ni eval), et la bibliothèque
// n'est chargée qu'à la première ouverture d'un PDF. Version « legacy » : la version courante exige des
// fonctions JavaScript trop récentes pour les navigateurs du parc (Map.getOrInsertComputed).

/** Pages dessinées au plus (au-delà : téléchargement), pour borner la mémoire d'un très gros document. */
export const PAGES_MAX_APERCU = 50

type Etat = { phase: 'chargement' } | { phase: 'pret'; pages: number; total: number } | { phase: 'erreur' }

export function ApercuPdf({ blob, titre }: { blob: Blob; titre: string }) {
  const t = useLibelle()
  const pages = useRef<HTMLDivElement>(null)
  const [etat, setEtat] = useState<Etat>({ phase: 'chargement' })
  // Libellés lus au dessin des pages, sans relancer le rendu quand la fonction de traduction change.
  const libelle = useRef(t)
  libelle.current = t
  useEffect(() => {
    let annule = false
    let detruire: (() => void) | undefined
    setEtat({ phase: 'chargement' })
    void (async () => {
      try {
        const pdfjs = await import('pdfjs-dist/legacy/build/pdf.mjs')
        pdfjs.GlobalWorkerOptions.workerSrc = adresseTravailleur
        const tache = pdfjs.getDocument({ data: new Uint8Array(await blob.arrayBuffer()) })
        detruire = () => void tache.destroy()
        const doc = await tache.promise
        const conteneur = pages.current
        if (annule || !conteneur) return
        conteneur.replaceChildren()
        const total = doc.numPages
        const nombre = Math.min(total, PAGES_MAX_APERCU)
        const largeur = Math.max(320, conteneur.clientWidth || 800)
        const densite = Math.min(2, window.devicePixelRatio || 1)
        for (let n = 1; n <= nombre && !annule; n++) {
          const page = await doc.getPage(n)
          const vue = page.getViewport({ scale: (largeur / page.getViewport({ scale: 1 }).width) * densite })
          const canevas = document.createElement('canvas')
          canevas.className = 'gm-apercu-page'
          canevas.width = Math.floor(vue.width)
          canevas.height = Math.floor(vue.height)
          canevas.setAttribute('role', 'img')
          canevas.setAttribute('aria-label', libelle.current('messagerie.apercu.page', { n, total }))
          conteneur.appendChild(canevas)
          await page.render({ canvas: canevas, viewport: vue }).promise
          if (n === 1 && !annule) setEtat({ phase: 'pret', pages: 1, total })
        }
        if (!annule) setEtat({ phase: 'pret', pages: nombre, total })
      } catch (e) {
        // Cause relevée pour le diagnostic (console du navigateur) ; l'utilisateur garde le téléchargement.
        console.warn('Aperçu PDF indisponible', e)
        if (!annule) setEtat({ phase: 'erreur' })
      }
    })()
    return () => {
      annule = true
      detruire?.()
    }
  }, [blob])
  return (
    <div className="gm-apercu-pdf" role="document" aria-label={t('messagerie.apercu.document', { nom: titre })} aria-busy={etat.phase === 'chargement'} tabIndex={0}>
      {etat.phase === 'chargement' ? <Skeleton height={420} /> : null}
      {etat.phase === 'erreur' ? <InlineAlert level="warn" title={t('messagerie.apercu.illisible')}>{t('messagerie.apercu.indisponibleTexte')}</InlineAlert> : null}
      {etat.phase === 'pret' && etat.total > PAGES_MAX_APERCU ? (
        <InlineAlert level="info">{t('messagerie.apercu.tronque', { n: PAGES_MAX_APERCU, total: etat.total })}</InlineAlert>
      ) : null}
      <div ref={pages} className="gm-apercu-pages" />
    </div>
  )
}
