// Séparateur de milliers (espace fine insécable U+202F) sans décimales, comme le système de conception publié.
export function formatMoney(n: number): string {
  return String(Math.round(n)).replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
}
