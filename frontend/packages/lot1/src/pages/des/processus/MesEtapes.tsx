import { useTranslation } from 'react-i18next'
import { Card, InlineAlert, Link, LinkList, Skeleton } from '@gimac/design-system'
import { useInstances } from '../../../hooks/processus'
import { useDates } from '../activite/communs'
import { BadgeEtape, useErreurProcessus } from './communs'

// « Mes étapes » (D-87 §4) dans Mon activité : étapes en cours des instances qui me concernent (vue « miennes » de
// MS-PILOTAGE : exécutant, validateur, signataire attendu, ou tâche à prendre par mon profil), avec leur échéance.
// D-98 : seules les étapes qui attendent ma décision (attendMoi servi par le serveur : exécuter, prendre, signer, valider) ;
// une étape soumise qui attend le validateur ou un signataire n'y figure plus pour son exécutant.
export function MesEtapes() {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreurProcessus()
  const instances = useInstances({ vue: 'miennes', etat: 'EN_COURS', taille: 50 })
  const items = (instances.data?.contenu ?? []).flatMap((i) => i.etapesEnCours.filter((e) => e.attendMoi ?? true).map((e) => ({
    key: `${i.id}-${e.code}`, href: `/des/processus/instances/${encodeURIComponent(i.id)}?etape=${encodeURIComponent(e.code)}`,
    label: `${i.intitule} : ${e.libelle}`, meta: <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}><BadgeEtape etat={e.etat} />{e.echeance ? <span className="gm-field-help">{dates.jour(e.echeance)}</span> : null}</span>,
  })))
  return (
    <Card title={t('proc.mesEtapes.titre')} actions={<Link href="/des/processus?vue=instances">{t('proc.mesEtapes.toutes')}</Link>}>
      {instances.isPending ? <Skeleton height={48} /> : instances.isError ? <InlineAlert level="crit">{erreur(instances.error)?.titre}</InlineAlert> : (
        <LinkList label={t('proc.mesEtapes.titre')} pagination={false} empty={t('proc.mesEtapes.aucune')} items={items} />
      )}
    </Card>
  )
}
