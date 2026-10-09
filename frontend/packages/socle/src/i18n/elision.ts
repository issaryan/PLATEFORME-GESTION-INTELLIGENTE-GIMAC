// Élision et contraction de la préposition « de » devant un nom variable (recette ADM-07) : « Droits d’Agenda »,
// « Pouvoirs d’Aïssatou Ngo Mbarga », « Membres du Pôle réseau ». Règle générale, appliquée par le formateur
// i18next « de » ({{nom, de}}) des libellés français : élision devant une voyelle (accentuée ou non), contraction
// devant les articles « le » et « les ». Le h n'est pas élidé : h muet et h aspiré ne se distinguent pas sans
// dictionnaire. Les autres langues gardent leur propre tournure dans leur catalogue.

const VOYELLE = /^[aàâäeéèêëiîïoôöuùûüyÿæœ]/i

/** « de » suivi du nom, élidé (« d’ ») ou contracté (« du », « des ») selon la règle française. */
export function prepositionDe(nom: unknown): string {
  const n = String(nom ?? '').trim()
  if (!n) return 'de'
  const article = /^(les?)\s+(.*)$/i.exec(n)
  if (article && article[2]) return `${article[1].toLowerCase() === 'le' ? 'du' : 'des'} ${article[2]}`
  return VOYELLE.test(n) ? `d’${n}` : `de ${n}`
}
