import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { ChartCard, Link, StatCard } from '@gimac/design-system'
import type { EtapeCode, IndicateursPilotage } from '../../../modele/parcours'
import { usePilotage } from '../../../hooks/parcours'
import { Donnees } from '../liaisons/communs'
import { QuestionsPilotage } from '../parcours/decision/QuestionsPilotage'

// Cockpit de pilotage (CR §7.3 ; vision diapositives 11 et 12 ; D-59) pour la direction générale et les
// chefs de département : quatre compteurs d'alerte, répartition des projets par étape, délai moyen de
// certification, taux de respect des délais, et les six questions de l'annexe B. Tous les chiffres viennent
// du service ; chaque compteur mène à la liste filtrée correspondante.

const grille = { alignItems: 'stretch', gap: 'var(--gm-space-4)' }

function Cockpit({ i }: { i: IndicateursPilotage }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const vers = (chemin: string) => () => void navigate(chemin)
  const repartition = i.repartitionParEtape
  const nc = t('cockpit.nc')
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)' }}>
      <div className="gm-row" style={grille} aria-label={t('cockpit.compteurs')}>
        <StatCard label={t('cockpit.projetsActifs')} value={i.projetsActifs} onClick={vers('/des/parcours')} />
        <StatCard
          label={t('cockpit.projetsBloques')}
          value={i.projetsBloques}
          status={i.projetsBloques > 0 ? 'crit' : 'ok'}
          statusLabel={t(i.projetsBloques > 0 ? 'cockpit.aLever' : 'cockpit.aucunBlocage')}
          onClick={vers('/des/parcours?bloquees=1')}
        />
        <StatCard
          label={t('cockpit.tachesEnRetard')}
          value={i.tachesEnRetard}
          status={i.tachesEnRetard > 0 ? 'crit' : 'ok'}
          statusLabel={t(i.tachesEnRetard > 0 ? 'cockpit.enRetard' : 'cockpit.aJour')}
        />
        <StatCard
          label={t('cockpit.echeancesProches')}
          value={i.echeancesProches}
          status={i.echeancesProches > 0 ? 'warn' : 'ok'}
          statusLabel={t(i.echeancesProches > 0 ? 'cockpit.aSurveiller' : 'cockpit.aJour')}
        />
      </div>
      <div className="gm-row" style={grille}>
        <div style={{ flex: '2 1 420px', minWidth: 0, display: 'grid' }}>
          <ChartCard
            type="bar"
            title={t('cockpit.repartition.titre')}
            subtitle={t('cockpit.repartition.sousTitre')}
            labels={repartition.map((r) => t(`cockpit.etapeCourte.${r.etape}`))}
            series={[{ name: t('cockpit.repartition.serie'), data: repartition.map((r) => r.nombre) }]}
            xLabel={t('cockpit.repartition.axe')}
            width={720}
          />
        </div>
        <div className="gm-col" style={{ flex: '1 1 240px', gap: 'var(--gm-space-4)' }}>
          <StatCard
            label={t('cockpit.delaiCertification')}
            value={i.delaiMoyenCertificationJours ?? nc}
            unit={i.delaiMoyenCertificationJours === null ? undefined : t('cockpit.unite.jours')}
          />
          <StatCard
            label={t('cockpit.tauxDelais')}
            value={i.tauxRespectDelais ?? nc}
            unit={i.tauxRespectDelais === null ? undefined : '%'}
            status={i.tauxRespectDelais === null ? undefined : i.tauxRespectDelais >= 80 ? 'ok' : 'warn'}
            statusLabel={i.tauxRespectDelais === null ? undefined : t('cockpit.tauxDelais.aide')}
          />
        </div>
      </div>
      <nav aria-label={t('cockpit.parEtape')} className="gm-row" style={{ gap: 'var(--gm-space-3)' }}>
        <span className="gm-field-help">{t('cockpit.parEtape')}</span>
        {repartition.map((r: { etape: EtapeCode; nombre: number }) => (
          <Link key={r.etape} href={`/des/parcours?etape=${r.etape}`}>{t('cockpit.lienEtape', { etape: t(`parcours.etape.${r.etape}`), count: r.nombre })}</Link>
        ))}
      </nav>
      <QuestionsPilotage indicateurs={i} />
    </div>
  )
}

export function SectionCockpit() {
  const { t } = useTranslation()
  const pilotage = usePilotage()
  return (
    <section className="gm-col" aria-label={t('cockpit.titre')}>
      <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
        <h2 className="gm-type-h3" style={{ margin: 0 }}>{t('cockpit.titre')}</h2>
        <span className="gm-field-help">{t('cockpit.sousTitre')}</span>
      </div>
      <Donnees requete={pilotage}>{(i) => <Cockpit i={i} />}</Donnees>
    </section>
  )
}
