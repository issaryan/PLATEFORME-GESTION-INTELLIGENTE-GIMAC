import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Dialog, InlineAlert, KeyValueList, TextArea } from '@gimac/design-system'
import type { OperationParcours } from '../../../modele/parcours'
import { useDeposerLivrable } from '../../../hooks/parcours'
import { ChampFichier } from '../../commun/ChampFichier'
import { useErreurService } from '../liaisons/communs'

// Étapes 4 et 5 (rangs 7 à 11) : dépôt du compte rendu de la réunion de lancement ou d'un atelier. Le
// fichier part au dépôt documentaire (empreinte revérifiée par le service), puis l'opération est close.

export function DialogueLivrable({ operation, onFermer, onDepose }: { operation: OperationParcours; onFermer: () => void; onDepose: () => void }) {
  const { t } = useTranslation()
  const deposer = useDeposerLivrable()
  const erreurService = useErreurService()
  const [fichier, setFichier] = useState<File | null>(null)
  const [commentaire, setCommentaire] = useState('')
  const [tente, setTente] = useState(false)
  const refus = erreurService(deposer.error)
  const soumettre = () => {
    setTente(true)
    if (!fichier) return
    deposer.mutate({ operationId: operation.id, fichier, commentaire: commentaire.trim() }, { onSuccess: onDepose })
  }
  return (
    <Dialog
      open
      id="gm-livrable"
      title={t('parcoursLivrable.titre')}
      description={t(`parcours.op.${operation.rang}`)}
      onClose={onFermer}
      footer={
        <>
          <Button variant="secondary" onClick={onFermer}>{t('commun.annuler')}</Button>
          <Button variant="primary" icon="upload" loading={deposer.isPending} onClick={soumettre}>{t('parcoursLivrable.deposer')}</Button>
        </>
      }
    >
      <div className="gm-col">
        <KeyValueList
          items={[
            { key: t('parcoursEtapes.rang'), value: operation.rang, mono: true },
            { key: t('parcoursLivrable.attendu'), value: t(`parcours.preuveAttendue.${operation.rang}`) },
            { key: t('parcoursEtapes.responsable'), value: operation.raci.R?.nom ?? t('commun.nonRenseigne') },
          ]}
        />
        {refus ? (
          <InlineAlert level="crit" title={t('parcoursLivrable.refus')} correlationId={refus.correlationId || undefined}>
            {refus.message}
          </InlineAlert>
        ) : null}
        <ChampFichier
          name="fichier"
          label={t('parcoursLivrable.fichier')}
          required
          accept=".pdf,.docx,.odt"
          maxSize="20 Mo"
          help={t('parcoursLivrable.fichier.aide')}
          error={tente && !fichier ? t('parcoursLivrable.erreur.fichier') : undefined}
          state={deposer.isPending ? 'uploading' : undefined}
          fichier={fichier}
          onFichier={setFichier}
        />
        <TextArea name="commentaire" label={t('parcoursLivrable.commentaire')} help={t('parcoursLivrable.commentaire.aide')} value={commentaire} onChange={setCommentaire} />
      </div>
    </Dialog>
  )
}
