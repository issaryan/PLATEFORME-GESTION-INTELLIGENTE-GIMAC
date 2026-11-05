import { useTranslation } from 'react-i18next'
import { BarresHorizontales, ChartCard } from '@gimac/design-system'
import type { CampagneSandbox, TauxSandbox } from '../../../../modele/sandbox'

// Vue graphique d'une campagne de l'onglet sandbox (D-121, règles dataviz) : jauges d'avancement et de réussite de la
// campagne (numérateur sur dénominateur repris tels quels), répartition des verdicts qui s'en déduit, réussite par
// service. Une seule teinte ; chaque carte garde sa vue tableau. Rien n'est relu dans la sandbox.

const pourcent = (x: TauxSandbox) => (x.denominateur ? Math.floor((x.numerateur / x.denominateur) * 1000) / 10 : 0)
const entier = (v: number) => (Number.isInteger(v) ? String(v) : '')
const pc = (v: number) => `${v.toLocaleString(undefined, { maximumFractionDigits: 1 })} %`

export function GraphesCampagne({ campagne: c }: { campagne: CampagneSandbox }) {
  const { t } = useTranslation()
  const global = (nature: string) => c.taux.find((x) => x.service === null && x.nature === nature)
  const avancement = global('AVANCEMENT')
  const reussite = global('REUSSITE')
  const parService = c.taux.filter((x) => x.service !== null && x.nature === 'REUSSITE')
  const jauge = (titre: string, x: TauxSandbox) => (
    <ChartCard
      title={titre}
      subtitle={t('sandboxResultats.graphes.ratio', { n: x.numerateur, d: x.denominateur })}
      type="gauge"
      min={0}
      max={100}
      unit="%"
      width={260}
      series={[{ name: titre, data: [pourcent(x)] }]}
    />
  )
  // Verdicts de la campagne : favorables (réussite), défavorables (jugés moins favorables), restants (cas non jugés).
  const verdicts = avancement && reussite
    ? [
        { cle: 'favorables', libelle: t('sandboxResultats.graphes.favorables'), valeur: reussite.numerateur },
        { cle: 'defavorables', libelle: t('sandboxResultats.graphes.defavorables'), valeur: Math.max(0, avancement.numerateur - reussite.numerateur) },
        { cle: 'restants', libelle: t('sandboxResultats.graphes.restants'), valeur: Math.max(0, avancement.denominateur - avancement.numerateur) },
      ]
    : []
  return (
    <div className="gm-graphiques-grille" aria-label={t('sandboxResultats.graphes.titre')}>
      {avancement ? jauge(t('sandboxResultats.graphes.avancement'), avancement) : null}
      {reussite ? jauge(t('sandboxResultats.graphes.reussite'), reussite) : null}
      {verdicts.length ? <BarresHorizontales title={t('sandboxResultats.graphes.verdicts')} subtitle={t('sandboxResultats.graphes.verdictsAide')} trier={false} format={entier} elements={verdicts} /> : null}
      <BarresHorizontales
        title={t('sandboxResultats.graphes.parService')}
        subtitle={t('sandboxResultats.graphes.parServiceAide')}
        trier={false}
        format={pc}
        elements={parService.map((x) => ({ cle: x.service as string, libelle: x.service as string, valeur: pourcent(x) }))}
        vide={t('sandboxResultats.graphes.parServiceVide')}
      />
    </div>
  )
}
