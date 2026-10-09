import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Avatar, AvatarCropper, Button, Card, FileDropzone, Note, RECADRAGE_INITIAL, recadrerImage, type Recadrage } from '@gimac/design-system'
import { urlAvatar, useConnexion } from '@gimac/socle'
import { PHOTO_PROFIL, type Moi } from '../../modele'
import { useChangerPhoto, useRetirerPhoto } from '../../hooks'
import { useMessageErreur } from '../../outils'
import { ModaleCompte, type ErreurModale } from '../../composants/ModaleSaisie'
import { Annonce, type Retour } from '../../composants/Annonce'
import { RefusPhoto, lireImage, tailleLisible, type ImageChoisie } from '../../composants/photo'

// Photo de profil (docs/lot1/mon-compte.md §1, §4) : choix d'une image (PNG, JPEG, WebP, 2 Mo au plus,
// 64 à 4 096 px), contrôle avant l'envoi (type réel, poids, dimensions), recadrage carré (zoom et position,
// à la souris ou au clavier) avec aperçu aux tailles de l'avatar, envoi de l'image recadrée, retrait. Tout
// geste dans la modale CRUD standard ; refus IAM_AVATAR_* expliqués sur le champ ou en tête de la modale.

const TYPES_ANNONCES = 'PNG, JPEG, JPG, WebP'

function ModalePhoto({ nom, onFait, onFermer }: { nom: string; onFait: () => void; onFermer: () => void }) {
  const { t, i18n } = useTranslation()
  const message = useMessageErreur()
  const changer = useChangerPhoto()
  const [image, setImage] = useState<ImageChoisie | null>(null)
  const [recadrage, setRecadrage] = useState<Recadrage>(RECADRAGE_INITIAL)
  const [lecture, setLecture] = useState(false)
  const [champ, setChamp] = useState<string | undefined>()
  const [erreur, setErreur] = useState<ErreurModale | null>(null)
  const [preparation, setPreparation] = useState(false)
  const choisir = async (f: File) => {
    setChamp(undefined)
    setErreur(null)
    setLecture(true)
    try {
      setImage(await lireImage(f))
      setRecadrage(RECADRAGE_INITIAL)
    } catch (x) {
      setImage(null)
      setChamp(t(`erreur.${x instanceof RefusPhoto ? x.code : 'IAM_AVATAR_UNREADABLE'}`))
    } finally {
      setLecture(false)
    }
  }
  const envoyer = async () => {
    if (!image) return
    setErreur(null)
    setPreparation(true)
    try {
      // Fichier d'origine : orientation EXIF appliquée dans le navigateur (le service ne la lit pas).
      const recadree = await recadrerImage(image.fichier, recadrage, PHOTO_PROFIL.coteEnvoi)
      await changer.mutateAsync(recadree)
      onFait()
    } catch (x) {
      const m = message(x)
      setErreur({ title: m.code?.startsWith('IAM_AVATAR_') || m.correlationId ? m.texte : t('erreur.IAM_AVATAR_UNREADABLE'), correlationId: m.correlationId })
    } finally {
      setPreparation(false)
    }
  }
  return (
    <ModaleCompte id="gm-dialogue-photo" title={t('photo.changer')} description={t('photo.modale.texte')} onClose={onFermer}
      submitLabel={t('photo.enregistrer')} submitIcon="image" onSubmit={() => void envoyer()} submitting={preparation || changer.isPending} error={erreur}
      submitDisabled={!image} submitDisabledReason={t('photo.choisirAvant')} dirty={!!image}>
      {image ? (
        <div className="gm-col">
          <AvatarCropper src={image.src} label={t('photo.recadrer')} value={recadrage} onChange={setRecadrage} name={nom} />
          <FileDropzone name="photo" label={t('photo.autre')} accept={TYPES_ANNONCES} maxSize={t('photo.poidsMax')} help={t('photo.formats')}
            file={{ name: image.nom, size: `${image.largeur} × ${image.hauteur} px · ${tailleLisible(image.taille, i18n.language, t('photo.ko'), t('photo.mo'))}` }}
            error={champ} onFile={(f) => void choisir(f)} onRemove={() => { setImage(null); setChamp(undefined) }} />
        </div>
      ) : (
        <FileDropzone name="photo" label={t('photo.image')} required accept={TYPES_ANNONCES} maxSize={t('photo.poidsMax')} help={t('photo.formats')}
          state={lecture ? 'uploading' : 'idle'} error={champ} onFile={(f) => void choisir(f)} />
      )}
    </ModaleCompte>
  )
}

export function Photo({ moi }: { moi: Moi }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const { couche } = useConnexion()
  const retirer = useRetirerPhoto()
  const [dialogue, setDialogue] = useState<'CHANGER' | 'RETIRER' | null>(null)
  const [retour, setRetour] = useState<Retour>(null)
  const c = moi.compte
  const src = urlAvatar(couche, c.id, c.avatarVersion)
  const ouvrir = (d: 'CHANGER' | 'RETIRER') => {
    setRetour(null)
    setDialogue(d)
  }
  const confirmerRetrait = async () => {
    try {
      await retirer.mutateAsync(undefined)
      setDialogue(null)
      setRetour({ niveau: 'info', message: { texte: t('photo.retiree') } })
    } catch (x) {
      setDialogue(null)
      setRetour({ niveau: 'crit', message: message(x) })
    }
  }
  return (
    <Card title={t('photo.titre')}>
      <div className="gm-col">
        <div className="gm-row" style={{ gap: 'var(--gm-space-5)', alignItems: 'center', flexWrap: 'wrap' }}>
          <Avatar name={c.nomAffiche} src={src} size="xl" />
          <div className="gm-col" style={{ flex: 1, minWidth: 240 }}>
            <Note>{t(`${src ? 'photo.texte.avec' : 'photo.texte.sans'}${couche === 'portail' ? 'Portail' : ''}`)}</Note>
            <div className="gm-row">
              <Button icon="camera" onClick={() => ouvrir('CHANGER')}>{t(src ? 'photo.changer' : 'photo.ajouter')}</Button>
              {src ? <Button variant="danger" icon="trash" onClick={() => ouvrir('RETIRER')}>{t('photo.retirer')}</Button> : null}
            </div>
          </div>
        </div>
        <Annonce retour={retour} />
      </div>
      {dialogue === 'CHANGER' ? (
        <ModalePhoto nom={c.nomAffiche} onFermer={() => setDialogue(null)}
          onFait={() => { setDialogue(null); setRetour({ niveau: 'info', message: { texte: t('photo.enregistree') } }) }} />
      ) : null}
      {dialogue === 'RETIRER' ? (
        <ModaleCompte id="gm-dialogue-photo-retrait" context="delete" irreversible={false} title={t('photo.retirer')} description={t('photo.retirer.texte')}
          submitLabel={t('photo.retirer')} submitting={retirer.isPending} onSubmit={() => void confirmerRetrait()} onClose={() => setDialogue(null)} />
      ) : null}
    </Card>
  )
}
