// Assemble des noms de classes en ignorant les valeurs fausses.
export function cx(...classes: Array<string | false | null | undefined | 0>): string {
  return classes.filter(Boolean).join(' ')
}
