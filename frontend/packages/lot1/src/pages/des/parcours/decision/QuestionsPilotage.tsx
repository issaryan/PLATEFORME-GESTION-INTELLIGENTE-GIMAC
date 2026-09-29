import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, DataTable, Link, type Column } from '@gimac/design-system'
import type { EtapeCode, IndicateursPilotage } from '../../../../modele/parcours'
import { Vide } from './commun'

// Les six questions de l'annexe B du compte rendu (critère d'acceptation, D-59) : chacune est un bloc lisible
// qui y répond directement, avec un lien vers la liste filtrée des liaisons quand elle existe.

type Q = IndicateursPilotage['questions']
const ETAPES_CERTIFICATION: EtapeCode[] = ['INTEGRATION', 'TESTS', 'CERTIFICATION']

/** Bloc d'une question : l'intitulé en titre, la réponse dessous. */
function Question({ numero, children }: { numero: number; children: ReactNode }) {
  const { t } = useTranslation()
  return (
    <div style={{ flex: '1 1 340px', minWidth: 0, display: 'grid' }}>
      <Card title={t(`cockpit.question.${numero}`)}>
        <div className="gm-col" style={{ gap: 'var(--gm-space-3)' }}>{children}</div>
      </Card>
    </div>
  )
}

/** Réponse chiffrée d'une question : la valeur en chiffre de statistique, l'unité et le commentaire. */
function Chiffre({ valeur, unite, aide }: { valeur: number | null; unite?: string; aide?: string }) {
  const { t } = useTranslation()
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
      <span className="gm-type-stat">
        {valeur === null ? t('cockpit.nc') : valeur}
        {valeur !== null && unite ? <span className="gm-type-small"> {unite}</span> : null}
      </span>
      {aide ? <span className="gm-field-help">{aide}</span> : null}
    </div>
  )
}

export function QuestionsPilotage({ indicateurs }: { indicateurs: IndicateursPilotage }) {
  const { t } = useTranslation()
  const q: Q = indicateurs.questions
  const parEtape = new Map(indicateurs.repartitionParEtape.map((r) => [r.etape, r.nombre]))
  const colDemandes: Column<Q['servicesPlusDemandes'][number]>[] = [
    { key: 'libelle', header: t('cockpit.colonne.service') },
    { key: 'nombre', header: t('cockpit.colonne.liaisons'), type: 'number' },
  ]
  const colLongs: Column<Q['servicesPlusLongs'][number]>[] = [
    { key: 'libelle', header: t('cockpit.colonne.service') },
    { key: 'delaiMoyenJours', header: t('cockpit.colonne.delaiMoyen'), type: 'number', render: (s) => t('cockpit.jours', { count: s.delaiMoyenJours }) },
  ]
  const colBloques: Column<Q['projetsBloques'][number]>[] = [
    { key: 'reference', header: t('cockpit.colonne.liaison'), render: (b) => <Link href={`/des/parcours/${b.liaisonId}`} mono>{b.reference}</Link> },
    { key: 'institutionNom', header: t('cockpit.colonne.participant') },
    { key: 'motif', header: t('cockpit.colonne.motif') },
    { key: 'depuis', header: t('cockpit.colonne.depuis'), render: (b) => t('cockpit.depuis', { date: b.depuis, count: b.jours }) },
  ]
  const colLancements: Column<Q['lancementsParService12Mois'][number]>[] = [
    { key: 'libelle', header: t('cockpit.colonne.service') },
    { key: 'nombre', header: t('cockpit.colonne.lancements'), type: 'number' },
  ]
  return (
    <section className="gm-col" aria-label={t('cockpit.questions.titre')}>
      <h3 className="gm-type-h3" style={{ margin: 0 }}>{t('cockpit.questions.titre')}</h3>
      <div className="gm-row" style={{ alignItems: 'stretch', gap: 'var(--gm-space-4)' }}>
        <Question numero={1}>
          <Chiffre valeur={q.enCertification} aide={t('cockpit.q1.aide')} />
          <ul className="gm-col" style={{ listStyle: 'none', margin: 0, padding: 0, gap: 'var(--gm-space-1)' }}>
            {ETAPES_CERTIFICATION.map((e) => (
              <li key={e}>
                <Link href={`/des/parcours?etape=${e}`}>{t('cockpit.q1.etape', { etape: t(`parcours.etape.${e}`), count: parEtape.get(e) ?? 0 })}</Link>
              </li>
            ))}
          </ul>
        </Question>
        <Question numero={2}>
          <DataTable columns={colDemandes} rows={q.servicesPlusDemandes} rowKey={(s) => s.serviceCode} density="compact" state={q.servicesPlusDemandes.length ? 'ok' : 'empty'} emptyText={t('cockpit.aucunService')} />
        </Question>
        <Question numero={3}>
          <DataTable columns={colLongs} rows={q.servicesPlusLongs} rowKey={(s) => s.serviceCode} density="compact" state={q.servicesPlusLongs.length ? 'ok' : 'empty'} emptyText={t('cockpit.aucunService')} />
        </Question>
        <Question numero={4}>
          <Chiffre valeur={q.delaiMoyenAdhesionProductionJours} unite={t('cockpit.unite.jours')} aide={t('cockpit.q4.aide')} />
        </Question>
        <Question numero={5}>
          {q.projetsBloques.length ? (
            <>
              <DataTable columns={colBloques} rows={q.projetsBloques} rowKey={(b) => `${b.liaisonId}-${b.depuis}`} density="compact" />
              <Link href="/des/parcours?bloquees=1">{t('cockpit.q5.voir')}</Link>
            </>
          ) : (
            <Vide texte={t('cockpit.q5.aucun')} />
          )}
        </Question>
        <Question numero={6}>
          <DataTable columns={colLancements} rows={q.lancementsParService12Mois} rowKey={(s) => s.serviceCode} density="compact" state={q.lancementsParService12Mois.length ? 'ok' : 'empty'} emptyText={t('cockpit.aucunService')} />
        </Question>
      </div>
    </section>
  )
}
