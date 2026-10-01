import { PHOTO_PROFIL } from '../modele'

// Contrôles d'une photo de profil avant l'envoi (docs/lot1/mon-compte.md §1) : mêmes règles et mêmes codes
// que MS-IAM (IAM_AVATAR_*), pour expliquer le refus sans attendre le service, qui refait tout le contrôle.

/** Image choisie : fichier d'origine (recadré avec son orientation EXIF appliquée), aperçu data:, dimensions orientées. */
export type ImageChoisie = { fichier: File; src: string; largeur: number; hauteur: number; type: string; nom: string; taille: number }

export type CodeRefusPhoto = 'IAM_AVATAR_TYPE' | 'IAM_AVATAR_TOO_LARGE' | 'IAM_AVATAR_DIMENSIONS' | 'IAM_AVATAR_UNREADABLE'

export class RefusPhoto extends Error {
  readonly code: CodeRefusPhoto
  constructor(code: CodeRefusPhoto) {
    super(code)
    this.code = code
  }
}

/** Type réel d'une image par ses premiers octets (PNG, JPEG, WebP), indépendamment de l'extension. */
export function typeReel(o: Uint8Array): string | null {
  if (o.length >= 8 && [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a].every((v, i) => o[i] === v)) return 'image/png'
  if (o.length >= 3 && o[0] === 0xff && o[1] === 0xd8 && o[2] === 0xff) return 'image/jpeg'
  const ascii = (de: number, a: number) => String.fromCharCode(...o.slice(de, a))
  if (o.length >= 12 && ascii(0, 4) === 'RIFF' && ascii(8, 12) === 'WEBP') return 'image/webp'
  return null
}

const lireOctets = (f: Blob, n: number): Promise<Uint8Array> =>
  new Promise((res, rej) => {
    const r = new FileReader()
    r.onload = () => res(new Uint8Array(r.result as ArrayBuffer))
    r.onerror = () => rej(new RefusPhoto('IAM_AVATAR_UNREADABLE'))
    r.readAsArrayBuffer(f.slice(0, n))
  })

const lireAdresse = (f: Blob): Promise<string> =>
  new Promise((res, rej) => {
    const r = new FileReader()
    r.onload = () => res(String(r.result))
    r.onerror = () => rej(new RefusPhoto('IAM_AVATAR_UNREADABLE'))
    r.readAsDataURL(f)
  })

const dimensions = (src: string): Promise<{ largeur: number; hauteur: number }> =>
  new Promise((res, rej) => {
    const img = new Image()
    img.onload = () => res({ largeur: img.naturalWidth, hauteur: img.naturalHeight })
    img.onerror = () => rej(new RefusPhoto('IAM_AVATAR_UNREADABLE'))
    img.src = src
  })

/** Lit et contrôle l'image choisie : type réel, poids, lisibilité, dimensions ; RefusPhoto sinon. */
export async function lireImage(f: File): Promise<ImageChoisie> {
  const type = typeReel(await lireOctets(f, 12))
  if (!type || !PHOTO_PROFIL.types.includes(type)) throw new RefusPhoto('IAM_AVATAR_TYPE')
  if (f.size > PHOTO_PROFIL.tailleMaxOctets) throw new RefusPhoto('IAM_AVATAR_TOO_LARGE')
  const src = await lireAdresse(f)
  const { largeur, hauteur } = await dimensions(src)
  const borne = (v: number) => v >= PHOTO_PROFIL.dimensionMin && v <= PHOTO_PROFIL.dimensionMax
  if (!borne(largeur) || !borne(hauteur)) throw new RefusPhoto('IAM_AVATAR_DIMENSIONS')
  return { fichier: f, src, largeur, hauteur, type, nom: f.name, taille: f.size }
}

/** Taille lisible d'un fichier, unités fournies dans la langue de l'interface (Ko, Mo ; KB, MB). */
export const tailleLisible = (octets: number, langue: string, ko: string, mo: string) =>
  octets < 1024 * 1024
    ? `${new Intl.NumberFormat(langue, { maximumFractionDigits: 0 }).format(Math.max(1, octets / 1024))} ${ko}`
    : `${new Intl.NumberFormat(langue, { maximumFractionDigits: 1 }).format(octets / 1024 / 1024)} ${mo}`
