import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Button, Card, Hash, InlineAlert, KeyValueList, Stepper } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import type { PortailOperation } from '../../modele/portail'
import { useOperationAFaire } from '../../hooks/portail'
import { actionDeposerPreuvePortail } from '../../ressources/portail'
import { useExecuteurActions } from '../../ressources/actions'
import { ChampFichier } from '../commun/ChampFichier'

// POR-03 Dépôt d'une preuve (gabarit G4 réduit, HOM-23) : choix du fichier, confirmation, accusé de dépôt
// avec l'empreinte calculée par MS-DOCUMENT. Le contrôle du contenu (antivirus, masquage) reste au service.

function Depot({ op }: { op: PortailOperation }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const executeur = useExecuteurActions()
  const [fichier, setFichier] = useState<File | null>(null)
  const depose = !!op.accuse
  const retour = () => void navigate(`/portail/liaisons/${op.liaisonId}?onglet=a-faire`)
  return (
    <div className="gm-col" style={{ maxWidth: 720 }}>
      <h1 className="gm-heading-display" style={{ margin: 0 }}>{t('portail.depot.titre', { code: op.code })}</h1>
      <Stepper steps={[{ label: t('portail.depot.etape.preuve'), state: depose ? 'done' : 'current' }, { label: t('portail.depot.etape.accuse'), state: depose ? 'current' : 'todo' }]} />
      <Card title={op.libelle}>
        <div className="gm-col" style={{ gap: 'var(--gm-space-4)' }}>
          <KeyValueList
            items={[
              { key: t('portail.operation.code'), value: op.code, mono: true },
              { key: t('portail.preuveAttendue'), value: op.preuveAttendue },
              { key: t('portail.operation.echeance'), value: op.echeance ?? t('commun.nonDisponible') },
            ]}
          />
          {op.accuse ? (
            <>
              <InlineAlert level="info" title={t('portail.accuse.titre')}>{t('portail.accuse.texte')}</InlineAlert>
              <KeyValueList
                items={[
                  { key: t('portail.accuse.empreinte'), value: <Hash value={op.accuse.empreinte} /> },
                  { key: t('portail.accuse.date'), value: op.accuse.dateDepot },
                ]}
              />
            </>
          ) : null}
          {op.actionsPermises.includes('DEPOSER_PREUVE') ? (
            <>
              <ChampFichier name="preuve" label={t('portail.depot.fichier')} help={t('portail.depot.aide')} accept="PDF, PNG, JPEG" maxSize="20 Mo" required fichier={fichier} onFichier={setFichier} />
              <div className="gm-row">
                <Button variant="secondary" size="lg" onClick={retour}>{t('portail.depot.retour')}</Button>
                <Button
                  variant="primary"
                  size="lg"
                  icon="upload"
                  disabled={!fichier}
                  disabledReason={t('portail.depot.fichierRequis')}
                  onClick={() => {
                    executeur.lancer(actionDeposerPreuvePortail, { ...op, fichier }, `${op.code} · ${op.libelle}`, 'homologation')
                    setFichier(null)
                  }}
                >
                  {t(depose ? 'portail.action.deposerNouvelle' : 'portail.action.confirmerDepot')}
                </Button>
              </div>
            </>
          ) : (
            <Button variant="secondary" size="lg" onClick={retour}>{t('portail.depot.retour')}</Button>
          )}
        </div>
      </Card>
    </div>
  )
}

export default function DepotPreuvePage() {
  const { t } = useTranslation()
  const { id = '', op = '' } = useParams()
  const q = useOperationAFaire(id, op)
  return (
    <EtatsRequete etat={q} emptyTitle={t('portail.introuvable')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
      {(o) => <Depot op={o} />}
    </EtatsRequete>
  )
}
