import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AttachmentViewer, Icon, typesLisibles } from '@gimac/design-system'
import { useServices } from '../../../services/ServicesProvider'
import { enregistrerFichier } from '../../../ressources/fichier'
import { useErreurProcessus } from './communs'

// QUO-04 (recette du travail quotidien) : pièce jointe du formulaire de lancement et livrable déposé d'une étape, ouverts
// en aperçu (PDF, images, documents bureautiques convertis) et téléchargeables par qui lit l'instance
// (GET /processus/instances/{id}/documents/{documentId}, contrôle de lecture de MS-PILOTAGE).

export const tailleLisible = (o: number) => (o < 1024 ? `${o} o` : o < 1_048_576 ? `${Math.round(o / 1024)} Ko` : `${(o / 1_048_576).toFixed(1)} Mo`)

/** Types admis lisibles (« PDF, images ») au lieu des types MIME bruts (QUO-07). */
export function useTypesLisibles() {
  const { t } = useTranslation()
  return (types: readonly string[] | string) => typesLisibles(types, (f) => t(`proc.types.${f}`, { defaultValue: f }))
}

export function DocumentProcessus({ instanceId, documentId, nom, typeMime, taille }: { instanceId: string; documentId: string; nom: string; typeMime: string; taille: number }) {
  const { t } = useTranslation()
  const services = useServices()
  const erreur = useErreurProcessus()
  const [apercu, setApercu] = useState<{ blob?: Blob; erreur?: string } | null>(null)
  const ouvrir = () => {
    setApercu({})
    services.processus.documentInstance(instanceId, documentId).then(
      (x) => setApercu((a) => (a ? { blob: x.blob } : a)),
      (e: unknown) => setApercu((a) => (a ? { erreur: erreur(e)?.titre } : a)),
    )
  }
  return (
    <>
      <button type="button" className="gm-link gm-proc-document" aria-label={t('proc.document.ouvrir', { nom })} onClick={ouvrir}>
        <Icon name="file-text" size={16} /> {nom} · {tailleLisible(taille)}
      </button>
      {apercu ? (
        <AttachmentViewer nom={nom} typeMime={typeMime} taille={tailleLisible(taille)} blob={apercu.blob} erreur={apercu.erreur} onClose={() => setApercu(null)}
          onTelecharger={() => (apercu.blob ? enregistrerFichier(apercu.blob, nom) : undefined)} convertir={services.apercu?.convertir} />
      ) : null}
    </>
  )
}
