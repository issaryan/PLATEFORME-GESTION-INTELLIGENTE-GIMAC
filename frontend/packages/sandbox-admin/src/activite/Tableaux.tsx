import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { DataTable, InlineAlert, PageSection, StateMachineBar, StatusBadge, type Column, type Status } from '@gimac/design-system'
import { usePermissions } from '@gimac/socle'
import { useCampagnesActives, useLiaisonCertification, useParticipantsActifs } from './hooks'
import { jours, pourcent, type Vue } from './filtres'
import { CHAINE, PERMISSION_LIAISON, type EtatLiaison, type LigneCampagne, type LigneParticipant } from './modele'
import { etatListe, useFormat } from '../composants/communs'

// Tableaux de l'écran « Activité des tests » : avancement de la certification par participant (chaîne sandbox puis
// états de la liaison lus dans MS-HOMOLOGATION) et délais des campagnes. Listes paginées par le service.

export const TAILLE_TABLEAU = 10
const STATUT_CAMPAGNE: Record<string, Status> = { EN_COURS: 'info', TERMINEE: 'ok', SUSPENDUE: 'warn', ANNULEE: 'neutral', BROUILLON: 'neutral' }
const dossier = (liaison: string) => `/des/parcours/${encodeURIComponent(liaison)}?onglet=sandbox`

/** Étapes franchies : la sandbox dès qu'une exécution existe, puis les états parcourus et l'état courant de la liaison. */
export function etapes(etat: EtatLiaison | null, parcourus: EtatLiaison[] | null): { courant: string; franchis: string[] } {
  const atteints = new Set<string>(['SANDBOX', ...(parcourus ?? []), ...(etat ? [etat] : [])])
  const franchis = CHAINE.filter((e) => atteints.has(e))
  const courant = etat && (CHAINE as readonly string[]).includes(etat) ? etat : franchis[franchis.length - 1]
  return { courant, franchis: etat === 'CERTIFIEE' ? [...CHAINE] : franchis }
}

function Chaine({ ligne, autorise }: { ligne: LigneParticipant; autorise: boolean }) {
  const { t } = useTranslation()
  const l = useLiaisonCertification(ligne.identifiantLiaison, autorise)
  const e = etapes(l.data?.etat ?? null, l.data?.etatsParcourus ?? null)
  const hors = l.data && !(CHAINE as readonly string[]).includes(l.data.etat)
  return <div className="gm-col">
    <StateMachineBar label={t('sbx.activite.colChaine')} states={CHAINE.map((k) => ({ key: k, label: t(`sbx.activite.etape.${k}`) }))} current={e.courant} traversed={e.franchis} />
    {hors ? <StatusBadge status="warn" label={t(`sbx.activite.etatLiaison.${l.data!.etat}`)} /> : null}
    {l.isError ? <span className="gm-text-muted">{t('sbx.activite.liaisonIndisponible')}</span> : null}
  </div>
}

export function Certification({ vue }: { vue: Vue }) {
  const { t } = useTranslation()
  const autorise = (usePermissions() ?? []).includes(PERMISSION_LIAISON)
  const [page, setPage] = useState(1)
  const q = useParticipantsActifs(vue.filtre, page - 1, TAILLE_TABLEAU)
  const lignes = q.data?.elements ?? []
  const colonnes: Column<LigneParticipant>[] = [
    { key: 'codeParticipant', header: t('sbx.activite.colParticipant'), render: (x) => <Link className="gm-link gm-mono" to={`/des/sandbox/espaces/${encodeURIComponent(x.codeParticipant)}`}>{x.codeParticipant}</Link> },
    { key: 'interfaces', header: t('sbx.activite.colInterfaces'), render: (x) => x.interfaces.map((i) => t(`sbx.activite.interface.${i}`)).join(', ') },
    { key: 'executions', header: t('sbx.activite.colExecutions'), type: 'number' },
    { key: 'tauxReussite', header: t('sbx.activite.colReussite'), render: (x) => `${pourcent(x.tauxReussite.valeur)} %` },
    { key: 'chaine', header: t('sbx.activite.colChaine'), render: (x) => <Chaine ligne={x} autorise={autorise} /> },
    { key: 'dossier', header: t('sbx.activite.colDossier'), render: (x) => (x.identifiantLiaison ? <Link className="gm-link" to={dossier(x.identifiantLiaison)}>{t('sbx.activite.ouvrirDossier')}</Link> : null) },
  ]
  return <PageSection title={t('sbx.activite.titreCertification')} description={t('sbx.activite.sousTitreCertification')}>
    {autorise ? null : <InlineAlert level="info" title={t('sbx.activite.degrade')} />}
    <DataTable columns={colonnes} rows={lignes} rowKey={(x) => x.codeParticipant} total={q.data?.total ?? 0} page={page} pageSize={TAILLE_TABLEAU} onPage={setPage}
      emptyText={t('sbx.activite.aucunParticipant')} {...etatListe(q, lignes)} onRetry={() => void q.refetch()} />
  </PageSection>
}

export function Delais({ vue }: { vue: Vue }) {
  const { t } = useTranslation()
  const f = useFormat()
  const [page, setPage] = useState(1)
  const q = useCampagnesActives(vue.filtre, 'DERNIER_ESSAI', page - 1, TAILLE_TABLEAU)
  const lignes = q.data?.elements ?? []
  const duree = (x: LigneCampagne) => {
    if (x.ouverteLe && x.closeLe) return t('sbx.activite.duree', { count: jours(x.ouverteLe, x.closeLe) })
    if (x.ouverteLe) return t('sbx.activite.enCours', { count: jours(x.ouverteLe, vue.filtre.au) })
    return t('sbx.activite.sansJalon')
  }
  const colonnes: Column<LigneCampagne>[] = [
    { key: 'code', header: t('sbx.activite.colCampagne'), render: (x) => (x.identifiantLiaison ? <Link className="gm-link gm-mono" to={dossier(x.identifiantLiaison)}>{x.code ?? x.identifiantCampagne}</Link> : <span className="gm-mono">{x.code ?? x.identifiantCampagne}</span>) },
    { key: 'codeParticipant', header: t('sbx.activite.colParticipant'), type: 'mono' },
    { key: 'interfaceTest', header: t('sbx.activite.interface'), render: (x) => (x.interfaceTest ? t(`sbx.activite.interface.${x.interfaceTest}`) : '') },
    { key: 'etat', header: t('sbx.activite.colEtat'), render: (x) => <StatusBadge status={STATUT_CAMPAGNE[x.etat] ?? 'neutral'} label={t(`sbx.activite.etatCampagne.${x.etat}`, { defaultValue: x.etat })} /> },
    { key: 'premierEssai', header: t('sbx.activite.colPremier'), render: (x) => f.instant(x.premierEssai) },
    { key: 'dernierEssai', header: t('sbx.activite.colDernier'), render: (x) => f.instant(x.dernierEssai) },
    { key: 'duree', header: t('sbx.activite.colDuree'), render: duree },
  ]
  return <PageSection title={t('sbx.activite.titreDelais')} description={t('sbx.activite.sousTitreDelais')}>
    <DataTable columns={colonnes} rows={lignes} rowKey={(x) => x.identifiantCampagne} total={q.data?.total ?? 0} page={page} pageSize={TAILLE_TABLEAU} onPage={setPage}
      emptyText={t('sbx.activite.aucuneCampagne')} {...etatListe(q, lignes)} onRetry={() => void q.refetch()} />
  </PageSection>
}
