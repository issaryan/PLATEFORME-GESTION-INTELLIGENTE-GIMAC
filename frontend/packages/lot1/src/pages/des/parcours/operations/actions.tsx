import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { KeyValueList } from '@gimac/design-system'
import type { ResourceActionLot1 } from '@gimac/socle'
import {
  useAttribuer, useConstaterCouverture, useDeposerLivrable, useDeposerPreuveParcours, useValiderOperationParcours,
} from '../../../../hooks/parcours'
import type { Ceremonie, OperationParcours, Qualite } from '../../../../modele/parcours'
import type { Services } from '../../../../services/ports'
import { PERM } from '../../../../permissions'

// Actions engageantes de l'onglet « Opérations », exécutées par l'ExecuteurActions (confirmation, motif,
// signature FIDO2, retour). Chaque action appelle un hook du parcours, jamais un service ; ce qu'elle
// confirme est récapitulé dans le dialogue avant l'envoi.

export type DepotPreuve = { fichier: File; valeurReference?: string; codeControle?: string; ceremonie?: Ceremonie }
export type DepotLivrable = { fichier: File; commentaire: string }
export type Attribution = { qualite: Qualite; personneId: string; personneNom: string }
type Action<T> = ResourceActionLot1<T, Services>
type Ligne<T> = OperationParcours & { saisie: T }

export function useActionsOperation() {
  const { t } = useTranslation()
  const deposerPreuve = useDeposerPreuveParcours()
  const deposerLivrable = useDeposerLivrable()
  const valider = useValiderOperationParcours()
  const attribuer = useAttribuer()
  const constater = useConstaterCouverture()
  const { mutateAsync: envoyerPreuve } = deposerPreuve
  const { mutateAsync: envoyerLivrable } = deposerLivrable
  const { mutateAsync: envoyerValidation } = valider
  const { mutateAsync: envoyerAttribution } = attribuer
  const { mutateAsync: envoyerConstat } = constater

  return useMemo(() => {
    const preuve: Action<Ligne<DepotPreuve>> = {
      key: 'deposerPreuve', label: 'parcoursOps.action.deposerPreuve', icon: 'upload', variant: 'primary', scope: 'row', permission: PERM.preuveDeposer, confirm: 'confirm',
      consequences: ({ saisie: s, rang }) => (
        <KeyValueList
          items={[
            { key: t('parcoursOps.preuve.fichier'), value: s.fichier.name },
            ...(s.valeurReference ? [{ key: t(`parcours.valeur.${rang}`), value: s.valeurReference, mono: true }] : []),
            ...(s.codeControle ? [{ key: t('parcoursOps.preuve.codeControle'), value: s.codeControle, mono: true }] : []),
            ...(s.ceremonie
              ? [
                  { key: t('parcoursOps.ceremonie.tenueLe'), value: s.ceremonie.tenueLe },
                  ...s.ceremonie.porteurs.map((p, i) => ({ key: t('parcoursOps.ceremonie.composante', { n: i + 1 }), value: `${p} · ${s.ceremonie?.codesControle[i] ?? ''}` })),
                ]
              : []),
          ]}
        />
      ),
      run: ({ row }) => envoyerPreuve({ operationId: row.id, ...row.saisie }),
    }
    const livrable: Action<Ligne<DepotLivrable>> = {
      key: 'deposerLivrable', label: 'parcoursOps.action.deposerLivrable', icon: 'upload', variant: 'primary', scope: 'row', permission: PERM.preuveDeposer, confirm: 'confirm',
      consequences: ({ saisie: s }) => (
        <KeyValueList items={[{ key: t('parcoursOps.preuve.fichier'), value: s.fichier.name }, ...(s.commentaire.trim() ? [{ key: t('parcoursOps.livrable.commentaire'), value: s.commentaire }] : [])]} />
      ),
      run: ({ row }) => envoyerLivrable({ operationId: row.id, fichier: row.saisie.fichier, commentaire: row.saisie.commentaire.trim() }),
    }
    const validation: Action<OperationParcours> = {
      key: 'valider', label: 'parcoursOps.action.valider', icon: 'pen-line', variant: 'primary', scope: 'row', permission: PERM.operationValider, confirm: 'sign',
      sign: { purpose: 'OPERATION_VALIDATE', empreinte: (o) => o.preuve?.empreinte ?? '' },
      consequences: () => <p className="gm-field-help" style={{ margin: 0 }}>{t('parcoursOps.valider.consequence')}</p>,
      run: ({ row, assertion }) => envoyerValidation({ operationId: row.id, assertion }),
    }
    const attribution: Action<Ligne<Attribution>> = {
      key: 'attribuer', label: 'parcoursOps.action.attribuer', icon: 'users', variant: 'primary', scope: 'row', permission: PERM.operationAttribuer, confirm: 'confirm', reason: true,
      consequences: ({ saisie: s }) => (
        <KeyValueList items={[{ key: t('parcoursOps.attribution.qualite'), value: t(`parcours.qualite.${s.qualite}`) }, { key: t('parcoursOps.attribution.personne'), value: s.personneNom }]} />
      ),
      run: ({ row, motif }) => envoyerAttribution({ operationId: row.id, qualite: row.saisie.qualite, personneId: row.saisie.personneId, motif: motif ?? '' }),
    }
    const constat: Action<OperationParcours> = {
      key: 'constater', label: 'parcoursOps.action.constater', icon: 'square-check', variant: 'primary', scope: 'row', permission: PERM.dossierVerifier, confirm: 'confirm',
      consequences: () => <p className="gm-field-help" style={{ margin: 0 }}>{t('parcoursOps.couverture.consequence')}</p>,
      run: ({ row }) => envoyerConstat(row.liaisonId),
    }
    return { preuve, livrable, validation, attribution, constat }
  }, [t, envoyerPreuve, envoyerLivrable, envoyerValidation, envoyerAttribution, envoyerConstat])
}
