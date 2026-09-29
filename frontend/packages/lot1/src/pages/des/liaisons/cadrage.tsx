import { useTranslation } from 'react-i18next'
import { Card, KeyValueList, StatusBadge, Tag } from '@gimac/design-system'
import type { Liaison, RoleIntervenant } from '../../../modele/homologation'

// Résumé du cadrage d'une liaison (étape 3, D-57, D-58), rendu sous l'en-tête de sa fiche : phase,
// périmètre de services (codes renvoyés par MS-HOMOLOGATION), intervenants et conducteur de projet.
// Composition de Card, KeyValueList, StatusBadge et Tag du catalogue.

const ORDRE: RoleIntervenant[] = ['INSTITUTION', 'BANQUE_PARTENAIRE', 'CHEF_DE_PROJET']

export function ResumeCadrage({ liaison }: { liaison: Liaison }) {
  const { t } = useTranslation()
  const intervenants = [...(liaison.intervenants ?? [])].sort((a, b) => ORDRE.indexOf(a.role) - ORDRE.indexOf(b.role))
  const conducteur = intervenants.some((i) => i.role === 'CHEF_DE_PROJET')
  return (
    <Card title={t('cadrage.titre')}>
      <KeyValueList
        items={[
          {
            key: t('cadrage.phase'),
            value: <StatusBadge status={liaison.phaseCourante === 'CADRAGE' ? 'info' : 'neutral'} label={t(`phase.${liaison.phaseCourante}`, { defaultValue: liaison.phaseCourante })} />,
          },
          {
            key: t('cadrage.perimetre'),
            value: (
              <span className="gm-row" title={t('cadrage.perimetre.aide')}>
                {(liaison.perimetre ?? []).map((c) => <Tag key={c}>{t(`service.${c}`, { defaultValue: c })}</Tag>)}
              </span>
            ),
          },
          ...intervenants.map((i) => ({ key: t(`intervenant.${i.role}`), value: i.nom })),
          ...(conducteur ? [] : [{ key: t('intervenant.CHEF_DE_PROJET'), value: <span className="gm-field-help">{t('cadrage.conducteur.aucun')}</span> }]),
        ]}
      />
    </Card>
  )
}
