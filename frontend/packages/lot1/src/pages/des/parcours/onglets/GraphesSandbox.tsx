import { useTranslation } from 'react-i18next'
import { BarresHorizontales, ChartCard, SerieTemporelle } from '@gimac/design-system'
import { useRepartitionSandbox } from '../../../../hooks/parcours'
import type { CampagneSandbox, RepartitionSandbox, TauxSandbox } from '../../../../modele/sandbox'
import { Donnees } from '../../liaisons/communs'

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

const CAS_MAX = 8
/** Jour du fuseau du service (AAAA-MM-JJ) placé à minuit local : l'axe du design-system est en heure locale. */
const minuit = (jour: string) => { const [a, m, j] = jour.split('-').map(Number); return new Date(a, m - 1, j).getTime() }

/** Verdicts par cas et chronologie des exécutions d'une campagne, comptés par MS-HOMOLOGATION (D-121). */
export function RepartitionCampagne({ liaisonId, campagneId }: { liaisonId: string; campagneId: string }) {
  const q = useRepartitionSandbox(liaisonId, campagneId)
  return <Donnees requete={q} estVide={(r) => r.parCas.length === 0}>{(r) => <Repartition r={r} />}</Donnees>
}

function Repartition({ r }: { r: RepartitionSandbox }) {
  const { t } = useTranslation()
  const cas = r.parCas.slice(0, CAS_MAX)
  return (
    <div className="gm-graphiques-grille">
      <ChartCard
        title={t('sandboxResultats.graphes.parCas')}
        subtitle={t('sandboxResultats.graphes.parCasAide', { n: cas.length, total: r.parCas.length })}
        type="bar"
        labels={cas.map((c) => c.identifiantCas ?? t('sandboxResultats.graphes.sansCas'))}
        xLabel={t('sandboxResultats.graphes.cas')}
        series={[
          { name: t('sandboxResultats.graphes.favorables'), data: cas.map((c) => c.favorables) },
          { name: t('sandboxResultats.graphes.defavorables'), data: cas.map((c) => c.defavorables) },
        ]}
      />
      <SerieTemporelle
        title={t('sandboxResultats.graphes.chronologie')}
        subtitle={t('sandboxResultats.graphes.chronologieAide', { fuseau: r.fuseau })}
        granularite="jour"
        aire={false}
        min={0}
        format={entier}
        series={[
          { nom: t('sandboxResultats.graphes.executions'), points: r.parJour.map((j) => ({ t: minuit(j.jour), v: j.executions })) },
          { nom: t('sandboxResultats.graphes.defavorables'), points: r.parJour.map((j) => ({ t: minuit(j.jour), v: j.defavorables })) },
        ]}
      />
    </div>
  )
}
