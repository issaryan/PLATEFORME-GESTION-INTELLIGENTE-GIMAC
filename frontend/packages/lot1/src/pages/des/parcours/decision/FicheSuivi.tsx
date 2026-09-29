import { useTranslation } from 'react-i18next'
import { Button, Card, CoverageBar, DataTable, InlineAlert, ProgressRing, StatusBadge, type Column } from '@gimac/design-system'
import type { DossierLiaison, FicheSuiviServices, ServicePerimetre } from '../../../../modele/parcours'
import { useConstaterMiseEnService, useRemettreFicheSuivi } from '../../../../hooks/parcours'
import { TON_FICHE_SUIVI, useLancer, usePeut, Vide, type Action, PERMISSION_GESTE } from './commun'

// Étape 10, rangs 32 et 33 : la fiche de suivi des services est remise au participant, qui la renseigne sur
// son portail (service par service : ouvert ou non, commentaire) ; Normes et Agréments constate alors les
// services effectivement ouverts. La couverture de mise en service est calculée par le service.

type Ligne = FicheSuiviServices['lignes'][number]

export function CarteFicheSuivi({ dossier, fiche }: { dossier: DossierLiaison; fiche: FicheSuiviServices }) {
  const { t } = useTranslation()
  const peut = usePeut()
  const lancer = useLancer()
  const remettre = useRemettreFicheSuivi()
  const constater = useConstaterMiseEnService()
  const titre = `${dossier.reference} · ${dossier.institutionNom}`
  const actionRemettre: Action<FicheSuiviServices> = {
    key: 'remettreFiche', permission: PERMISSION_GESTE.remettreFiche[0], label: 'miseEnService.fiche.action.remettre', icon: 'send', variant: 'primary', scope: 'row', confirm: 'confirm',
    consequences: () => <p style={{ margin: 0 }}>{t('miseEnService.fiche.remettre.consequence')}</p>,
    run: () => remettre.mutateAsync(fiche.liaisonId),
  }
  const actionConstater: Action<FicheSuiviServices> = {
    key: 'constaterMiseEnService', permission: PERMISSION_GESTE.constaterMiseEnService[0], label: 'miseEnService.fiche.action.constater', icon: 'square-check', variant: 'primary', scope: 'row', confirm: 'confirm',
    consequences: (f) => (
      <p style={{ margin: 0 }}>
        {t('miseEnService.fiche.constater.consequence', { ouverts: f.lignes.filter((l) => l.ouvert).length, total: f.lignes.length })}
      </p>
    ),
    run: () => constater.mutateAsync(fiche.liaisonId),
  }
  const colonnes: Column<Ligne>[] = [
    { key: 'libelle', header: t('miseEnService.fiche.service') },
    {
      key: 'ouvert', header: t('miseEnService.fiche.ouvert'),
      render: (l) => (l.ouvert === null
        ? <StatusBadge status="neutral" label={t('miseEnService.fiche.nonRenseigne')} />
        : <StatusBadge status={l.ouvert ? 'ok' : 'warn'} label={t(l.ouvert ? 'parcours.servicePerimetre.OUVERT' : 'parcours.servicePerimetre.NON_OUVERT')} />),
    },
    { key: 'commentaire', header: t('miseEnService.fiche.commentaire'), render: (l) => l.commentaire ?? <Vide /> },
  ]
  const peutRemettre = peut(fiche.actionsPermises, 'REMETTRE', 'remettreFiche')
  const peutConstater = peut(fiche.actionsPermises, 'CONSTATER', 'constaterMiseEnService')
  return (
    <Card title={t('miseEnService.fiche.titre')} actions={<StatusBadge status={TON_FICHE_SUIVI[fiche.etat] ?? 'neutral'} label={t(`parcours.fiche.${fiche.etat}`)} />}>
      <div className="gm-col">
        <p className="gm-field-help" style={{ margin: 0 }}>{t('miseEnService.fiche.explication')}</p>
        {fiche.etat === 'REMISE' ? <InlineAlert level="info">{t('miseEnService.fiche.attenteParticipant')}</InlineAlert> : null}
        {fiche.etat === 'RENSEIGNEE' ? <InlineAlert level="warn">{t('miseEnService.fiche.aConstater', { date: fiche.renseigneeLe ?? '' })}</InlineAlert> : null}
        {fiche.etat === 'CONSTATEE' ? <InlineAlert level="info">{t('miseEnService.fiche.constatee', { date: fiche.constateeLe ?? '' })}</InlineAlert> : null}
        <DataTable columns={colonnes} rows={fiche.lignes} rowKey={(l) => l.serviceCode} state={fiche.lignes.length ? 'ok' : 'empty'} emptyText={t('miseEnService.fiche.aucune')} />
        {peutRemettre || peutConstater ? (
          <div className="gm-row" data-testid="actions-fiche">
            {peutRemettre ? <Button variant="primary" icon="send" onClick={() => lancer(actionRemettre, fiche, titre)}>{t('miseEnService.fiche.action.remettre')}</Button> : null}
            {peutConstater ? <Button variant="primary" icon="square-check" onClick={() => lancer(actionConstater, fiche, titre)}>{t('miseEnService.fiche.action.constater')}</Button> : null}
          </div>
        ) : null}
      </div>
    </Card>
  )
}

const NIVEAU: Record<ServicePerimetre['etat'], 1 | 2 | 3 | 4> = { NON_DEMARRE: 1, EN_TEST: 2, CERTIFIE: 3, OUVERT: 4, NON_OUVERT: 3 }

/** Couverture de mise en service : services effectivement ouverts, rapportés au périmètre contractualisé. */
export function CarteCouvertureMiseEnService({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const c = dossier.couvertureMiseEnService
  const inclus = dossier.perimetre.filter((s) => s.inclus)
  return (
    <Card title={t('miseEnService.couverture.titre')}>
      <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-6)' }}>
        <div style={{ flex: '0 0 200px', display: 'flex', justifyContent: 'center' }}>
            <ProgressRing
            label={t('miseEnService.couverture.libelle')}
            value={c.taux}
            status={c.taux === 100 ? 'ok' : c.taux === null ? 'neutral' : 'warn'}
            statusLabel={c.calculable ? t('miseEnService.couverture.detail', { n: c.numerateur, d: c.denominateur }) : t('miseEnService.couverture.nonCalculable')}
          />
          </div>
        <div className="gm-col" style={{ flex: '1 1 280px', gap: 'var(--gm-space-2)' }}>
          {inclus.map((s) => <CoverageBar key={s.serviceCode} service={s.libelle} level={NIVEAU[s.etat]} />)}
          {inclus.some((s) => s.etat === 'NON_OUVERT') ? <p className="gm-field-help" style={{ margin: 0 }}>{t('miseEnService.couverture.nonOuverts')}</p> : null}
        </div>
      </div>
    </Card>
  )
}
