import { useId, useRef, useState, type CSSProperties, type KeyboardEvent, type PointerEvent } from 'react'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

/** Recadrage carré : zoom (1 à 4) et position du cadre (-1 à 1 sur chaque axe, 0 au centre). */
export type Recadrage = { zoom: number; x: number; y: number }

export const RECADRAGE_INITIAL: Recadrage = { zoom: 1, x: 0, y: 0 }
export const ZOOM_MAX = 4

const borne = (v: number, min: number, max: number) => Math.min(max, Math.max(min, v))
const arrondi = (v: number) => Math.round(v * 1000) / 1000

/** Zone carrée retenue dans l'image d'origine (pixels de l'image) : côté et coin supérieur gauche. */
export function zoneRecadree(largeur: number, hauteur: number, r: Recadrage): { sx: number; sy: number; cote: number } {
  const cote = Math.min(largeur, hauteur) / borne(r.zoom, 1, ZOOM_MAX)
  const cx = largeur / 2 + borne(r.x, -1, 1) * ((largeur - cote) / 2)
  const cy = hauteur / 2 + borne(r.y, -1, 1) * ((hauteur - cote) / 2)
  return { sx: cx - cote / 2, sy: cy - cote / 2, cote }
}

/** Place l'image dans un cadre carré de `taille` pixels pour montrer la zone retenue. */
function placement(largeur: number, hauteur: number, r: Recadrage, taille: number): CSSProperties {
  const { sx, sy, cote } = zoneRecadree(largeur, hauteur, r)
  const echelle = taille / cote
  return { position: 'absolute', left: -sx * echelle, top: -sy * echelle, width: largeur * echelle, height: hauteur * echelle, maxWidth: 'none' }
}

/** Image décodée, orientation EXIF appliquée (une photo de téléphone n'arrive jamais tournée). */
async function decoder(source: string | Blob): Promise<{ image: CanvasImageSource; largeur: number; hauteur: number }> {
  // Fichier d'origine : createImageBitmap applique l'orientation EXIF (imageOrientation « from-image »).
  if (typeof source !== 'string' && typeof createImageBitmap === 'function') {
    try {
      const b = await createImageBitmap(source, { imageOrientation: 'from-image' })
      return { image: b, largeur: b.width, hauteur: b.height }
    } catch {
      throw new Error('Image illisible')
    }
  }
  // Repli : élément image décodé (le navigateur applique aussi l'orientation, image-orientation: from-image) ;
  // adresse data: (la politique de sécurité du contenu n'admet pas blob:).
  const adresse = typeof source === 'string' ? source : await new Promise<string>((res, rej) => {
    const r = new FileReader()
    r.onload = () => res(String(r.result))
    r.onerror = () => rej(new Error('Image illisible'))
    r.readAsDataURL(source)
  })
  const img = new Image()
  await new Promise<void>((res, rej) => {
    img.onload = () => res()
    img.onerror = () => rej(new Error('Image illisible'))
    img.src = adresse
  })
  return { image: img, largeur: img.naturalWidth, hauteur: img.naturalHeight }
}

/**
 * Image recadrée et redimensionnée (carré de `sortie` pixels), calculée dans le navigateur par un canevas,
 * orientation EXIF appliquée (le service ne la lit pas) ; WebP si le navigateur sait l'encoder, PNG sinon.
 * `source` : le fichier choisi (de préférence) ou son adresse data:.
 */
export async function recadrerImage(source: string | Blob, r: Recadrage, sortie = 256): Promise<Blob> {
  const { image, largeur, hauteur } = await decoder(source)
  const canevas = document.createElement('canvas')
  canevas.width = sortie
  canevas.height = sortie
  const ctx = canevas.getContext('2d')
  if (!ctx) throw new Error('Canevas indisponible')
  const { sx, sy, cote } = zoneRecadree(largeur, hauteur, r)
  ctx.imageSmoothingQuality = 'high'
  ctx.drawImage(image, sx, sy, cote, cote, 0, 0, sortie, sortie)
  if ('close' in image) image.close()
  const encoder = (type: string) => new Promise<Blob | null>((res) => canevas.toBlob(res, type, 0.9))
  const webp = await encoder('image/webp')
  const blob = webp && webp.type === 'image/webp' ? webp : await encoder('image/png')
  if (!blob) throw new Error('Encodage impossible')
  return blob
}

export interface AvatarCropperProps {
  /** Image choisie (adresse data:). */
  src: string
  /** Nom accessible du cadre. */
  label: string
  value: Recadrage
  onChange: (r: Recadrage) => void
  /** Côté du cadre en pixels (240 par défaut). */
  size?: number
  /** Nom de la personne (aperçus de l'avatar). */
  name?: string
}

// Recadrage carré d'une photo de profil (docs/lot1/mon-compte.md §4) : cadre carré à angles droits, guide
// circulaire de l'avatar, image déplacée à la souris, au doigt ou au clavier (flèches ; + et − pour le zoom ;
// 0 pour recentrer), curseur de zoom, aperçus aux tailles de l'avatar (96, 40, 24 px). Aucune ombre, aucun
// voile : la zone retenue est le cadre lui-même. Le recadrage final se calcule par recadrerImage.
export function AvatarCropper(p: AvatarCropperProps) {
  const t = useLibelle()
  const id = useId()
  const taille = p.size ?? 240
  const [dims, setDims] = useState<{ l: number; h: number } | null>(null)
  const glisse = useRef<{ px: number; py: number; r: Recadrage } | null>(null)
  const r = p.value
  const changer = (v: Partial<Recadrage>) => {
    const n = { ...r, ...v }
    p.onChange({ zoom: arrondi(borne(n.zoom, 1, ZOOM_MAX)), x: arrondi(borne(n.x, -1, 1)), y: arrondi(borne(n.y, -1, 1)) })
  }
  // Débattement du cadre en pixels affichés, pour convertir un glissé en position relative.
  const debattement = () => {
    if (!dims) return { x: 0, y: 0 }
    const cote = Math.min(dims.l, dims.h) / r.zoom
    const echelle = taille / cote
    return { x: ((dims.l - cote) / 2) * echelle, y: ((dims.h - cote) / 2) * echelle }
  }
  const touche = (e: KeyboardEvent<HTMLDivElement>) => {
    const pas = e.shiftKey ? 0.2 : 0.05
    const actions: Record<string, () => void> = {
      ArrowLeft: () => changer({ x: r.x - pas }),
      ArrowRight: () => changer({ x: r.x + pas }),
      ArrowUp: () => changer({ y: r.y - pas }),
      ArrowDown: () => changer({ y: r.y + pas }),
      '+': () => changer({ zoom: r.zoom + 0.1 }),
      '=': () => changer({ zoom: r.zoom + 0.1 }),
      '-': () => changer({ zoom: r.zoom - 0.1 }),
      '0': () => p.onChange(RECADRAGE_INITIAL),
    }
    const a = actions[e.key]
    if (a) {
      e.preventDefault()
      a()
    }
  }
  const debut = (e: PointerEvent<HTMLDivElement>) => {
    glisse.current = { px: e.clientX, py: e.clientY, r }
    e.currentTarget.setPointerCapture?.(e.pointerId)
  }
  const deplacer = (e: PointerEvent<HTMLDivElement>) => {
    const g = glisse.current
    if (!g) return
    const d = debattement()
    // L'image suit le pointeur : le cadre se déplace en sens inverse.
    changer({ x: d.x ? g.r.x - (e.clientX - g.px) / d.x : g.r.x, y: d.y ? g.r.y - (e.clientY - g.py) / d.y : g.r.y })
  }
  const fin = () => {
    glisse.current = null
  }
  const vue = (cote: number) => (dims ? placement(dims.l, dims.h, r, cote) : { visibility: 'hidden' as const })
  return (
    <div className="gm-crop">
      <div className="gm-crop-zone">
        <div
          className="gm-crop-cadre"
          style={{ width: taille, height: taille }}
          tabIndex={0}
          role="group"
          aria-label={p.label}
          aria-describedby={`${id}-aide`}
          aria-roledescription={t('recadrage.cadre')}
          onKeyDown={touche}
          onPointerDown={debut}
          onPointerMove={deplacer}
          onPointerUp={fin}
          onPointerCancel={fin}
          data-testid="gm-crop-cadre"
        >
          <img src={p.src} alt="" draggable={false} style={vue(taille)} onLoad={(e) => setDims({ l: e.currentTarget.naturalWidth || 1, h: e.currentTarget.naturalHeight || 1 })} />
          <span className="gm-crop-guide" aria-hidden="true" />
        </div>
        <div className="gm-crop-apercus" aria-label={t('recadrage.apercu')} role="group">
          <span className="gm-crop-apercus-titre">{t('recadrage.apercu')}</span>
          <span className="gm-row" style={{ alignItems: 'flex-end' }}>
            {[96, 40, 24].map((c) => (
              <span key={c} className="gm-crop-mini" style={{ width: c, height: c }} aria-hidden="true">
                <img src={p.src} alt="" draggable={false} style={vue(c)} />
              </span>
            ))}
          </span>
        </div>
      </div>
      <p className="gm-field-help" id={`${id}-aide`}>{t('recadrage.aide')}</p>
      <div className="gm-crop-reglages">
        <div className="gm-crop-zoom">
          <label className="gm-field-label" htmlFor={`${id}-zoom`}>{t('recadrage.zoom')}</label>
          <input
            id={`${id}-zoom`}
            className="gm-range"
            type="range"
            min={1}
            max={ZOOM_MAX}
            step={0.05}
            value={r.zoom}
            aria-valuetext={`${Math.round(r.zoom * 100)} %`}
            onChange={(e) => changer({ zoom: Number(e.target.value) })}
          />
          <span className="gm-num gm-crop-valeur" aria-hidden="true">{`${Math.round(r.zoom * 100)} %`}</span>
        </div>
        <Button size="sm" variant="ghost" icon="rotate-ccw" onClick={() => p.onChange(RECADRAGE_INITIAL)}>{t('recadrage.recentrer')}</Button>
      </div>
    </div>
  )
}
