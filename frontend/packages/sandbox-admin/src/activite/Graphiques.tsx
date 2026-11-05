import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { BarresHorizontales, Button, InlineAlert, SerieTemporelle, StatGrid } from '@gimac/design-system'
import { useActivite, useCampagnesActives, useCas, useCodesRetour, useSynthese } from './hooks'
import { pas, pourcent, serie, type Vue } from './filtres'
import { INTERFACES, type Verdicts } from './modele'
import { useFormat } from '../composants/communs'

// Graphiques de l'écran « Activité des tests » (règles dataviz) : une seule teinte pour les grandeurs, petits multiples
// pour les interfaces (même échelle, pas de couleur d'identité), libellés directs, vue tableau dans chaque carte.

const TOP = 8
export const entier = (v: number) => (Number.isInteger(v) ? String(v) : '')
const pc = (v: number) => `${v.toLocaleString(undefined, { maximumFractionDigits: 1 })} %`

function Erreur({ refetch }: { refetch: () => unknown }) {
  const { t } = useTranslation()
  return <InlineAlert level="crit" title={t('sbx.activite.erreur')}><Button variant="ghost" onClick={() => void refetch()}>{t('sbx.activite.reessayer')}</Button></InlineAlert>
}

export function Indicateurs({ vue }: { vue: Vue }) {
  const { t } = useTranslation()
  const f = useFormat()
  const s = useSynthese(vue.filtre)
  if (s.isError) return <Erreur refetch={s.refetch} />
  if (!s.data) return <p>{t('sbx.activite.chargement')}</p>
  const d = s.data
  return <div className="gm-col">
    <StatGrid
      label={t('sbx.activite.indicateurs')}
      items={[
        { key: 'executions', label: t('sbx.activite.executions'), value: d.executions.toLocaleString() },
        { key: 'participants', label: t('sbx.activite.participantsActifs'), value: d.participantsActifs },
        { key: 'cas', label: t('sbx.activite.tauxCas'), value: pc(pourcent(d.tauxReussiteCas.valeur)), context: t('sbx.activite.tauxCasContexte', { n: d.tauxReussiteCas.numerateur, d: d.tauxReussiteCas.denominateur }) },
        { key: 'campagnes', label: t('sbx.activite.campagnesEnCours'), value: d.campagnesParEtat.EN_COURS ?? 0, context: t('sbx.activite.campagnesContexte', { count: d.campagnes }) },
      ]}
    />
    <p className="gm-text-muted">{t('sbx.activite.calculeLe', { date: f.instant(d.calculeLe) })}</p>
  </div>
}

export function ActiviteParInterface({ vue }: { vue: Vue }) {
  const { t } = useTranslation()
  const a = useActivite(vue.filtre, vue.granularite)
  if (a.isError) return <Erreur refetch={a.refetch} />
  const points = a.data?.points ?? []
  const debuts = pas(vue.filtre, vue.granularite)
  const interfaces = vue.filtre.interface ? [vue.filtre.interface] : [...INTERFACES]
  const max = Math.max(4, ...points.map((p) => p.executions))
  return <div className="gm-graphiques-grille">
    {interfaces.map((i) => (
      <SerieTemporelle
        key={i}
        title={t(`sbx.activite.interface.${i}`)}
        subtitle={t('sbx.activite.sousTitreActivite')}
        series={[{ nom: t('sbx.activite.serieExecutions'), points: serie(points, i, debuts) }]}
        granularite={vue.granularite === 'MOIS' ? 'mois' : 'jour'}
        min={0}
        max={max}
        format={entier}
        chargement={!a.data}
        rechargement={a.isFetching && !!a.data}
        vide={t('sbx.activite.aucuneExecution')}
      />
    ))}
  </div>
}

const VERDICTS: (keyof Verdicts)[] = ['favorables', 'defavorables', 'nonExecutes', 'enAttente']

export function VerdictsParStatut({ vue }: { vue: Vue }) {
  const { t } = useTranslation()
  const s = useSynthese(vue.filtre)
  if (s.isError) return <Erreur refetch={s.refetch} />
  return <BarresHorizontales
    title={t('sbx.activite.titreVerdicts')}
    subtitle={t('sbx.activite.sousTitreVerdicts')}
    trier={false}
    format={entier}
    elements={s.data ? VERDICTS.map((v) => ({ cle: v, libelle: t(`sbx.activite.verdict.${v}`), valeur: s.data.verdicts[v] })) : []}
    vide={t(s.data ? 'sbx.activite.aucuneExecution' : 'sbx.activite.chargement')}
  />
}

export function TauxCampagnes({ vue }: { vue: Vue }) {
  const { t } = useTranslation()
  const naviguer = useNavigate()
  const c = useCampagnesActives(vue.filtre, 'TAUX_REUSSITE', 0, TOP)
  if (c.isError) return <Erreur refetch={c.refetch} />
  const elements = (c.data?.elements ?? []).filter((x) => x.tauxReussite.denominateur > 0).map((x) => ({
    cle: x.identifiantCampagne,
    libelle: `${x.code ?? x.identifiantCampagne} · ${x.codeParticipant}`,
    valeur: pourcent(x.tauxReussite.valeur),
    onClick: x.identifiantLiaison ? () => naviguer(`/des/parcours/${encodeURIComponent(x.identifiantLiaison as string)}?onglet=sandbox`) : undefined,
  }))
  return <BarresHorizontales title={t('sbx.activite.titreTauxCampagnes')} subtitle={t('sbx.activite.sousTitreTauxCampagnes')} trier={false} maximum={TOP} format={pc} elements={elements} vide={t(c.data ? 'sbx.activite.aucuneCampagne' : 'sbx.activite.chargement')} />
}

export function CasDefavorables({ vue }: { vue: Vue }) {
  const { t } = useTranslation()
  const c = useCas(vue.filtre, 0, TOP)
  if (c.isError) return <Erreur refetch={c.refetch} />
  const elements = (c.data?.elements ?? []).filter((x) => x.verdicts.defavorables > 0).map((x) => ({
    cle: `${x.codeCas}|${x.interfaceTest}`,
    libelle: x.interfaceTest ? `${x.codeCas} · ${t(`sbx.activite.interface.${x.interfaceTest}`)}` : x.codeCas,
    valeur: x.verdicts.defavorables,
  }))
  return <BarresHorizontales title={t('sbx.activite.titreCas')} subtitle={t('sbx.activite.sousTitreCas')} trier={false} maximum={TOP} format={entier} elements={elements} vide={t(c.data ? 'sbx.activite.aucuneExecution' : 'sbx.activite.chargement')} />
}

export function CodesRejet({ vue }: { vue: Vue }) {
  const { t } = useTranslation()
  const c = useCodesRetour(vue.filtre, 0, TOP)
  if (c.isError) return <Erreur refetch={c.refetch} />
  if (c.data === undefined) return <p>{t('sbx.activite.chargement')}</p>
  if (c.data === null) {
    return <InlineAlert level="info" title={`${t('sbx.activite.titreCodes')} : ${t('sbx.activite.codesIndisponibles')}`}>{t('sbx.activite.codesIndisponiblesAide')}</InlineAlert>
  }
  const elements = c.data.elements.map((x) => ({ cle: `${x.interface}|${x.code}`, libelle: `${x.code} · ${t(`sbx.activite.interface.${x.interface}`)}`, valeur: x.occurrences }))
  return <BarresHorizontales title={t('sbx.activite.titreCodes')} subtitle={t('sbx.activite.sousTitreCodes')} trier={false} maximum={TOP} format={entier} elements={elements} vide={t('sbx.activite.aucunCode')} />
}
