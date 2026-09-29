import { useTranslation } from 'react-i18next'
import { FileDropzone, Hash, InlineAlert, KeyValueList, StatusBadge, type MaskingResultProps } from '@gimac/design-system'
import type { DepotResultat } from '../../../modele/homologation'
import { tailleLisible } from '../../../adaptateurs/cloture'

// Compositions de DES-10, faites uniquement de composants du catalogue.

/** Corps du SignDialog de validation : effet de la signature (horodatage réessayé par le service). */
export function ConsequenceValidation() {
  const { t } = useTranslation()
  return <InlineAlert level="info">{t('cloture.valider.consequence')}</InlineAlert>
}

const MASQUAGE: Record<DepotResultat['controle']['resultat'], MaskingResultProps['result']> = { ACCEPTEE: 'accepted', MASQUEE: 'masked', REFUSEE: 'rejected' }

/**
 * Sélection du fichier de preuve. Le FileDropzone du catalogue n'expose pas de sélecteur : un champ fichier
 * natif masqué est déclenché par clic, clavier (Entrée, Espace) ou glisser-déposer sur la zone.
 */
export function SelecteurPreuve(p: { fichier: File | null; onFichier: (f: File) => void; dernierDepot: DepotResultat | null; disabled?: boolean }) {
  const { t, i18n } = useTranslation()
  const controle = p.dernierDepot?.controle
  // Clic, clavier et dépôt par glisser sont ceux du FileDropzone du catalogue (onFile).
  return (
    <FileDropzone
      name="preuve"
      label={t('cloture.fichier')}
      required
      help={t('cloture.fichier.aide')}
      accept="PDF, PNG, JPEG"
      maxSize={t('cloture.fichier.taille')}
      file={p.fichier ? { name: p.fichier.name, size: tailleLisible(p.fichier.size, i18n.language) } : undefined}
      state={p.fichier ? 'done' : 'idle'}
      masking={controle && !p.fichier ? { result: MASQUAGE[controle.resultat], count: controle.sequencesMasquees, text: controle.motif ?? t(`cloture.masquage.${controle.resultat}`) } : undefined}
      onFile={p.disabled ? undefined : p.onFichier}
    />
  )
}

/** Dernière version déposée, telle que scellée par MS-DOCUMENT. */
export function DernierDepot({ depot }: { depot: DepotResultat }) {
  const { t } = useTranslation()
  return (
    <KeyValueList
      items={[
        { key: t('cloture.depot.reference'), value: depot.documentRef, mono: true },
        { key: t('cloture.depot.version'), value: String(depot.version) },
        { key: t('cloture.depot.empreinte'), value: <Hash value={depot.empreinte} /> },
        {
          key: t('cloture.depot.scellement'),
          value: <StatusBadge status={depot.scelle ? 'ok' : 'warn'} icon={depot.scelle ? 'lock' : undefined} label={t(depot.scelle ? 'cloture.depot.scelle' : 'cloture.depot.nonScelle')} />,
        },
      ]}
    />
  )
}
