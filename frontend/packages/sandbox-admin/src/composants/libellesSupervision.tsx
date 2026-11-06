import { useTranslation } from 'react-i18next'

// Libellés de la supervision : nom usuel des services, modules et natures (code en sous-titre discret et en infobulle),
// nombres et pourcentages dans la langue de l'interface (Intl.NumberFormat).

const DISCRET = { color: 'var(--gm-color-text-muted)' } as const

export function useLibellesSupervision() {
  const { t, i18n } = useTranslation()
  const langue = i18n.language
  const libelle = (prefixe: string, code: string) => (i18n.exists(`${prefixe}.${code}`) ? t(`${prefixe}.${code}`) : code)
  const nom = (code: string) => (code.toLowerCase().startsWith('sbx-') ? libelle('sbx.supervision.serviceNom', code.toLowerCase()) : libelle('sbx.supervision.moduleNom', code))
  return {
    nom,
    nature: (code: string) => libelle('sbx.supervision.natureNom', code),
    nombre: (v: number, decimales = 0) => new Intl.NumberFormat(langue, { minimumFractionDigits: decimales, maximumFractionDigits: decimales }).format(v),
    /** Valeur en centièmes de pour cent (10 000 = 100 %). */
    pourcent: (centiemes: number) => new Intl.NumberFormat(langue, { style: 'percent', minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(centiemes / 10_000),
  }
}

/** Nom usuel, code technique en dessous (petit, atténué) et en infobulle. */
export function NomCode({ code }: { code: string }) {
  const l = useLibellesSupervision()
  const nom = l.nom(code)
  if (nom === code) return <span className="gm-mono">{code}</span>
  return <span className="gm-col" style={{ gap: 0 }} title={code}>
    <span>{nom}</span>
    <span className="gm-mono gm-type-small" style={DISCRET}>{code}</span>
  </span>
}

export function Discret({ children }: { children: string }) {
  return <span className="gm-type-small" style={DISCRET}>{children}</span>
}
