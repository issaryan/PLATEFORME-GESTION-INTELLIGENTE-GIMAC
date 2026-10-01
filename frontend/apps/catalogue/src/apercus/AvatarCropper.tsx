import { useState } from 'react'
import { AvatarCropper, RECADRAGE_INITIAL } from '@gimac/design-system'
import { photoDemoCatalogue } from '../outils/photoDemo'

// Recadrage carré d'une photo de profil : glisser, flèches, + et −, curseur de zoom, aperçus ronds.
export default function Apercu() {
  const [r, setR] = useState({ ...RECADRAGE_INITIAL, zoom: 1.4 })
  return <AvatarCropper src={photoDemoCatalogue('catalogue', 480, 320)} label="Cadrer la photo de profil" value={r} onChange={setR} name="Rachel Biyong" size={200} />
}
