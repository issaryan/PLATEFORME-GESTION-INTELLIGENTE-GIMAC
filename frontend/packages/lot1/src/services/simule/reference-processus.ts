/** Référence simulée selon le même gabarit que ReferenceProcessus de MS-PILOTAGE. */
export function referenceSimulee(modele: string | null | undefined, code: string, date: Date, sequence: number): string | null {
  if (!modele) return null
  const [aaaa, mm, jj] = date.toISOString().slice(0, 10).split('-')
  return modele.replaceAll('{CODE}', code).replaceAll('{AAAA}', aaaa).replaceAll('{MM}', mm).replaceAll('{JJ}', jj).replaceAll('{NNNNNN}', String(sequence).padStart(6, '0'))
}
