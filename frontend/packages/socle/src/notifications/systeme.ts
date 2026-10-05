// Notification du système et son discret (D-80, docs/lot1/notifications.md §4). L'API Notification du
// navigateur n'est employée qu'en contexte sécurisé (HTTPS, ou localhost), onglet caché, préférence
// `systeme` active et permission accordée ; la permission n'est demandée que par un bouton des préférences,
// jamais à l'ouverture. En http par l'adresse IP, l'option est indiquée comme indisponible (HTTPS exigé).

/**
 * État de la notification du système dans ce navigateur : `https` (contexte non sécurisé), `absente` (API
 * non prise en charge), puis la permission du navigateur : `a-demander`, `accordee`, `refusee`.
 */
export type EtatSysteme = 'https' | 'absente' | 'a-demander' | 'accordee' | 'refusee'

export function etatNotificationSysteme(): EtatSysteme {
  if (typeof window === 'undefined' || !window.isSecureContext) return 'https'
  if (typeof window.Notification === 'undefined') return 'absente'
  const p = window.Notification.permission
  return p === 'granted' ? 'accordee' : p === 'denied' ? 'refusee' : 'a-demander'
}

/** Demande de permission, sur un geste de l'utilisateur seulement (bouton des préférences). */
export async function demanderNotificationSysteme(): Promise<EtatSysteme> {
  if (etatNotificationSysteme() !== 'a-demander') return etatNotificationSysteme()
  await window.Notification.requestPermission()
  return etatNotificationSysteme()
}

/**
 * Affiche une notification du système si l'onglet est caché et la permission accordée ; un clic ramène à
 * l'onglet puis exécute `ouvrir`. Rend vrai si la notification a été affichée.
 */
export function notifierSysteme(o: { titre: string; corps?: string | null; etiquette: string; ouvrir: () => void }): boolean {
  if (!document.hidden || etatNotificationSysteme() !== 'accordee') return false
  const n = new window.Notification(o.titre, { body: o.corps ?? undefined, tag: o.etiquette })
  n.onclick = () => {
    window.focus()
    o.ouvrir()
    n.close()
  }
  return true
}

/**
 * Son discret (deux notes brèves, douces) produit par Web Audio : aucun fichier binaire. Muet sous un
 * navigateur piloté (tests de bout en bout) et sans Web Audio (tests unitaires).
 */
export function jouerSon(): boolean {
  const Contexte = typeof window === 'undefined' ? undefined : (window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext)
  if (!Contexte || navigator.webdriver) return false
  try {
    const ctx = new Contexte()
    const debut = ctx.currentTime
    ;[880, 1175].forEach((frequence, i) => {
      const osc = ctx.createOscillator()
      const volume = ctx.createGain()
      osc.type = 'sine'
      osc.frequency.value = frequence
      const t = debut + i * 0.12
      volume.gain.setValueAtTime(0.0001, t)
      volume.gain.exponentialRampToValueAtTime(0.06, t + 0.02)
      volume.gain.exponentialRampToValueAtTime(0.0001, t + 0.18)
      osc.connect(volume).connect(ctx.destination)
      osc.start(t)
      osc.stop(t + 0.2)
    })
    window.setTimeout(() => void ctx.close(), 600)
    return true
  } catch {
    return false
  }
}
