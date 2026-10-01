// Photo de démonstration du catalogue (paysage simple en SVG, sans visage) : illustre l'Avatar avec photo
// et le recadrage, sans ressource distante.
const PALETTES = [
  ['#9cc3d5', '#f3d27a', '#4f7a4a', '#2f5233'],
  ['#e8b48a', '#fbe3b0', '#8a5a3c', '#5b3a29'],
  ['#a9d1cf', '#f8f4e3', '#3f8a83', '#22524e'],
]

export function photoDemoCatalogue(graine: string, largeur = 256, hauteur = 256): string {
  let h = 0
  for (const c of graine) h = (h * 31 + c.charCodeAt(0)) >>> 0
  const [ciel, soleil, colline, sol] = PALETTES[h % PALETTES.length]
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${largeur}" height="${hauteur}" viewBox="0 0 ${largeur} ${hauteur}">` +
    `<rect width="${largeur}" height="${hauteur}" fill="${ciel}"/><circle cx="${largeur * 0.3}" cy="${hauteur * 0.3}" r="${Math.min(largeur, hauteur) * 0.11}" fill="${soleil}"/>` +
    `<path d="M0 ${hauteur * 0.55} Q ${largeur * 0.25} ${hauteur * 0.4} ${largeur * 0.5} ${hauteur * 0.55} T ${largeur} ${hauteur * 0.5} V${hauteur} H0 Z" fill="${colline}"/>` +
    `<path d="M0 ${hauteur * 0.75} Q ${largeur * 0.4} ${hauteur * 0.6} ${largeur * 0.7} ${hauteur * 0.72} T ${largeur} ${hauteur * 0.65} V${hauteur} H0 Z" fill="${sol}"/></svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}
