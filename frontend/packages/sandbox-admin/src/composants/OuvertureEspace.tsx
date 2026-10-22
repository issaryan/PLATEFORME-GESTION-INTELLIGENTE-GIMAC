import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CrudDialog, InlineAlert, TextField } from '@gimac/design-system'
import { nouvelleCle, useOuvrirEspace } from '../hooks'
import type { Ouverture } from '../modele'
import { useMessageErreur, type MessageErreur } from './communs'

// Ouverture de l'espace d'une institution (UC-INS-01, saga OUVERTURE de SBX-ESPACE) : code participant (cinq chiffres)
// et correspondant actif de l'institution, qui devient le premier administrateur de l'espace. Une clé
// d'idempotence est tirée à l'ouverture du dialogue : une nouvelle tentative après un échec réseau ne rouvre rien.

const CODE = /^[0-9]{5}$/
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

export function OuvertureEspace({ codeParticipant, onFermer, onOuvert }: { codeParticipant?: string; onFermer: () => void; onOuvert: (o: Ouverture) => void }) {
  const { t } = useTranslation()
  const erreur = useMessageErreur()
  const ouvrir = useOuvrirEspace()
  const [cle] = useState(nouvelleCle)
  const [code, setCode] = useState(codeParticipant ?? '')
  const [correspondant, setCorrespondant] = useState('')
  const [echec, setEchec] = useState<MessageErreur | undefined>()
  const codeOk = CODE.test(code)
  const correspondantOk = UUID.test(correspondant.trim())
  return (
    <CrudDialog context="create" title={t('sbx.ouverture.titre')} subject={codeParticipant ? t('sbx.espace.institution', { code: codeParticipant }) : undefined}
      onClose={onFermer} submitLabel={t('sbx.ouverture.valider')} submitIcon="plus" submitting={ouvrir.isPending} error={echec}
      dirty={!!correspondant || (!codeParticipant && !!code)} submitDisabled={!codeOk || !correspondantOk} submitDisabledReason={t('sbx.ouverture.incomplet')}
      consequences={[t('sbx.ouverture.consequence')]}
      onSubmit={() => {
        setEchec(undefined)
        ouvrir.mutate({ demande: { codeParticipant: code, correspondantId: correspondant.trim() }, cle }, { onSuccess: onOuvert, onError: (e) => setEchec(erreur(e)) })
      }}>
      <InlineAlert level="info" title={t('sbx.ouverture.aide')} />
      <TextField name="codeParticipant" label={t('sbx.espace.codeParticipant')} required mono value={code} onChange={setCode} readOnly={!!codeParticipant} maxLength={5} inputMode="numeric"
        help={t('sbx.ouverture.codeAide')} error={code && !codeOk ? t('sbx.ouverture.codeInvalide') : undefined} />
      <TextField name="correspondantId" label={t('sbx.ouverture.correspondant')} required mono value={correspondant} onChange={setCorrespondant}
        help={t('sbx.ouverture.correspondantAide')} error={correspondant && !correspondantOk ? t('sbx.ouverture.correspondantInvalide') : undefined} />
    </CrudDialog>
  )
}
