import { useState, type ReactNode } from 'react'
import { formatCompact } from '../../lib/graphiques'
import { Tabs } from '../../navigation/Tabs/Tabs'
import { useLangue, useLibelle } from '../../i18n/LangueProvider'

export interface ElementBarre {
  /** Identifiant stable (clé React) ; le libellé sert à défaut. */
  cle?: string
  libelle: string
  valeur: number
  /** Action au clic (filtrer la liste correspondante). */
  onClick?: () => void
}

export interface BarresHorizontalesProps {
  title: string
  subtitle?: string
  elements: ElementBarre[]
  unit?: string
  format?: (v: number) => string
  /** Au-delà, les plus petites catégories sont regroupées en une barre « Autres » (8 par défaut). */
  maximum?: number
  /** Tri décroissant par valeur (par défaut) ; false conserve l'ordre fourni (étapes d'un parcours). */
  trier?: boolean
  vide?: string
  actions?: ReactNode
}

// Barres horizontales de répartition (comptes par profil, liaisons par étape, causes) : une seule teinte
// (comparaison de grandeurs, pas d'identités), barres de 20 px à bout arrondi partant d'une même ligne de
// base (bout arrondi de 4 px côté valeur, carré côté base), valeur au bout de la barre, survol par barre, vue Tableau équivalente, regroupement « Autres ».
export function BarresHorizontales(p: BarresHorizontalesProps) {
  const t = useLibelle()
  const langue = useLangue()
  const [vue, setVue] = useState('graphique')
  const [actif, setActif] = useState<string | null>(null)
  const fmt = (v: number) => (p.format ? p.format(v) : formatCompact(v, langue) + (p.unit ? ' ' + p.unit : ''))
  const maximum = Math.max(1, p.maximum ?? 8)
  const tries = p.trier === false ? p.elements : [...p.elements].sort((a, b) => b.valeur - a.valeur)
  let elements = tries
  if (tries.length > maximum) {
    const reste = tries.slice(maximum - 1)
    elements = [...tries.slice(0, maximum - 1), { cle: '__autres', libelle: t('graphique.autres', { nombre: reste.length }), valeur: reste.reduce((s, e) => s + e.valeur, 0) }]
  }
  const haut = Math.max(0, ...elements.map((e) => e.valeur)) || 1

  let corps: ReactNode
  if (!elements.length) corps = <div className="gm-graphique-vide">{p.vide ?? t('graphique.aucuneMesure')}</div>
  else if (vue === 'tableau')
    corps = (
      <table className="gm-table gm-table-compact">
        <thead>
          <tr>
            <th scope="col">{t('graphique.categorie')}</th>
            <th scope="col" className="gm-num">{t('graphique.valeur')}</th>
          </tr>
        </thead>
        <tbody>
          {elements.map((e) => (
            <tr key={e.cle ?? e.libelle}>
              <td>{e.libelle}</td>
              <td className="gm-num">{fmt(e.valeur)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    )
  else
    corps = (
      <div className="gm-barres-h" role="list">
        {elements.map((e) => {
          const cle = e.cle ?? e.libelle
          const part = Math.max(0, Math.min(100, (e.valeur / haut) * 100))
          return (
            <div
              key={cle}
              className="gm-barres-h-ligne"
              role="listitem"
              aria-label={e.libelle + ' : ' + fmt(e.valeur)}
              onPointerEnter={() => setActif(cle)}
              onPointerLeave={() => setActif(null)}
            >
              <span className="gm-barres-h-libelle">
                {e.onClick ? (
                  <button type="button" className="gm-link" onClick={e.onClick}>{e.libelle}</button>
                ) : (
                  e.libelle
                )}
              </span>
              <span className="gm-barres-h-piste">
                <span className="gm-barres-h-fond gm-serie-1" aria-hidden="true">
                  <span className={'gm-barres-h-barre' + (actif === cle ? ' gm-actif' : '')} style={{ width: part + '%' }} />
                </span>
                <span className="gm-barres-h-valeur">{fmt(e.valeur)}</span>
              </span>
            </div>
          )
        })}
      </div>
    )

  return (
    <section className="gm-card gm-chartcard" aria-label={p.title}>
      <header className="gm-card-header">
        <div>
          <h2 className="gm-card-title">{p.title}</h2>
          {p.subtitle ? <div className="gm-field-help">{p.subtitle}</div> : null}
        </div>
        <div className="gm-row">
          {p.actions}
          <Tabs items={[{ key: 'graphique', label: t('graphique.graphique') }, { key: 'tableau', label: t('graphique.tableau') }]} value={vue} onChange={setVue} />
        </div>
      </header>
      <div className="gm-card-body">{corps}</div>
    </section>
  )
}
