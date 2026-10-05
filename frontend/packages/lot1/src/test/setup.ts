import { configure } from '@testing-library/react'

// Les pages attendent des services simulés et des chargements paresseux : sous la charge de la suite
// complète, le délai d'une seconde de Testing Library rend certains tests instables.
configure({ asyncUtilTimeout: 5000 })

// Contexte sécurisé du navigateur (HTTPS) : WebAuthn y est présent ; les tests qui simulent une clé de
// sécurité remplacent navigator.credentials, ceux du contexte non sécurisé (http) redéfinissent ces valeurs.
Object.defineProperty(globalThis, 'isSecureContext', { value: true, configurable: true, writable: true })
if (typeof (globalThis as { PublicKeyCredential?: unknown }).PublicKeyCredential === 'undefined') {
  Object.defineProperty(globalThis, 'PublicKeyCredential', { value: class {}, configurable: true, writable: true })
}
if (typeof navigator !== 'undefined' && !navigator.credentials) {
  Object.defineProperty(navigator, 'credentials', { value: {}, configurable: true, writable: true })
}

// Canevas @xyflow/react (modélisateur, D-77) sous jsdom : ResizeObserver, DOMMatrixReadOnly et dimensions des
// éléments, absents de jsdom, sont simulés comme le recommande la documentation de React Flow.
class ResizeObserverSimule {
  private rappel: ResizeObserverCallback
  constructor(rappel: ResizeObserverCallback) { this.rappel = rappel }
  observe(cible: Element) { this.rappel([{ target: cible, contentRect: { width: 1000, height: 640 } } as ResizeObserverEntry], this as unknown as ResizeObserver) }
  unobserve() {}
  disconnect() {}
}
if (typeof globalThis.ResizeObserver === 'undefined') Object.defineProperty(globalThis, 'ResizeObserver', { value: ResizeObserverSimule, configurable: true, writable: true })
if (typeof (globalThis as { DOMMatrixReadOnly?: unknown }).DOMMatrixReadOnly === 'undefined') {
  class DOMMatrixReadOnlySimule {
    m22: number
    constructor(transform?: string) {
      const echelle = transform?.match(/scale\(([0-9.]+)\)/)?.[1]
      this.m22 = echelle !== undefined ? Number(echelle) : 1
    }
  }
  Object.defineProperty(globalThis, 'DOMMatrixReadOnly', { value: DOMMatrixReadOnlySimule, configurable: true, writable: true })
}
Object.defineProperties(HTMLElement.prototype, {
  offsetHeight: { configurable: true, get() { return parseFloat((this as HTMLElement).style.height) || 1 } },
  offsetWidth: { configurable: true, get() { return parseFloat((this as HTMLElement).style.width) || 1 } },
})
if (typeof SVGElement !== 'undefined') (SVGElement.prototype as unknown as { getBBox: () => DOMRect }).getBBox = () => ({ x: 0, y: 0, width: 0, height: 0 }) as DOMRect
