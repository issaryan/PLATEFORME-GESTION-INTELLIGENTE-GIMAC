import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AttachmentViewer, Button, Dialog, Hash, KeyValueList, Note, type KeyValueItem } from '@gimac/design-system'
import { useServices } from '../../../../services/ServicesProvider'
import { useTelechargerPieceParcours } from '../../../../hooks/parcours'
import { useFonctionsCatalogue } from '../../../../hooks/referentiels'
import type { DepotLivrable, EtatLivrable, LivrableAttendu, OperationParcours } from '../../../../modele/parcours'
import { enregistrerFichier } from '../../../../ressources/fichier'
import { libelleOperation } from '../../../../adaptateurs/modelisateur'
import { useFormatDate } from '../communs'
import { BadgeLivrable, verifierPiece } from './Preuves'

// Détail d'une preuve attendue dans une modale (demande du 19/10/2026) : de quoi il s'agit exactement (nature,
// consigne, formats admis, déposant, obligation, grain), son état et son dépôt (pièce, empreinte, auteur, date,
// motif d'un « sans objet ») ; la pièce déposée s'affiche dans la visionneuse de la plateforme (PDF, image, texte,
// bureautique convertie, D-68 et D-83) et se télécharge avec contrôle de son empreinte.

export type PreuveDetaillee = { op: OperationParcours; livrable: LivrableAttendu; service?: string; etat: EtatLivrable; depot?: DepotLivrable | null }

/** Type MIME déduit du nom, pour la visionneuse quand le service ne le donne pas. */
export function typeMimeDe(nom: string): string {
  const ext = nom.includes('.') ? nom.slice(nom.lastIndexOf('.') + 1).toLowerCase() : ''
  const types: Record<string, string> = { pdf: 'application/pdf', png: 'image/png', jpg: 'image/jpeg', jpeg: 'image/jpeg', gif: 'image/gif', webp: 'image/webp', txt: 'text/plain', csv: 'text/csv' }
  return types[ext] ?? 'application/octet-stream'
}

export function DetailPreuve({ preuve, onFermer }: { preuve: PreuveDetaillee; onFermer: () => void }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const f = useFonctionsCatalogue()
  const services = useServices()
  const telecharger = useTelechargerPieceParcours()
  const { op, livrable: l, service, etat, depot } = preuve
  const [piece, setPiece] = useState<{ blob: Blob | null; chargement: boolean; erreur: string | null; conforme: boolean | null } | null>(null)
  const nom = depot?.nom ?? depot?.documentRef ?? ''

  const charger = async (): Promise<Blob | null> => {
    if (!depot?.documentRef) return null
    setPiece({ blob: null, chargement: true, erreur: null, conforme: null })
    try {
      const r = await telecharger.mutateAsync({ operationId: op.id, documentId: depot.documentRef })
      setPiece({ blob: r.blob, chargement: false, erreur: null, conforme: await verifierPiece(r.blob, depot.empreinte, r.empreinte) })
      return r.blob
    } catch (e) {
      setPiece({ blob: null, chargement: false, erreur: (e as { message?: string }).message ?? t('commun.erreur.titre'), conforme: null })
      return null
    }
  }
  const [voir, setVoir] = useState(false)

  const quoi: KeyValueItem[] = [
    { key: t('preuves.detail.operation'), value: `${op.rang} · ${libelleOperation(t, op)}` },
    { key: t('preuves.detail.code'), value: service ? `${l.code} · ${service}` : l.code, mono: true },
    { key: t('preuves.detail.nature'), value: t(`preuves.nature.${l.nature}`) },
    { key: t('preuves.detail.obligation'), value: l.obligatoire ? t('preuves.obligatoire') : t('parcoursOps.livrable.facultatif') },
    { key: t('preuves.detail.grain'), value: l.grain === 'SERVICE' ? t('preuves.detail.parService', { service: service ?? '' }) : t('preuves.detail.parOperation') },
    { key: t('preuves.detail.deposant'), value: l.deposant ? l.libelleDeposant ?? f.libelle(l.deposant) : t('preuves.detail.deposantLibre') },
    { key: t('preuves.detail.formats'), value: l.typesPieces?.length ? l.typesPieces.join(', ') : t('preuves.detail.formatsLibres') },
    { key: t('preuves.detail.etat'), value: <BadgeLivrable etat={etat} /> },
  ]
  const depose: KeyValueItem[] = depot
    ? [
        ...(depot.sansObjet ? [{ key: t('preuves.detail.sansObjet'), value: depot.motif ?? '' }] : []),
        ...(nom && !depot.sansObjet ? [{ key: t('preuves.detail.piece'), value: nom }] : []),
        ...(depot.empreinte ? [{ key: t('preuves.detail.empreinte'), value: <Hash value={depot.empreinte} verified={piece?.conforme ?? undefined} /> }] : []),
        { key: t('preuves.detail.deposeLe'), value: depot.deposeLe ? date(depot.deposeLe) : '' },
        { key: t('preuves.detail.deposePar'), value: depot.deposePar ?? '' },
        ...(depot.origine || depot.repris ? [{ key: t('preuves.detail.origine'), value: depot.repris ? t('preuves.detail.reprise') : depot.origine ?? '' }] : []),
      ]
    : []

  return (
    <>
      <Dialog
        open
        id="gm-detail-preuve"
        size="lg"
        title={l.libelle}
        subtitle={`${op.rang} · ${libelleOperation(t, op)}`}
        onClose={onFermer}
        footer={
          <>
            {depot?.documentRef && !depot.sansObjet ? (
              <>
                <Button variant="secondary" icon="download" loading={piece?.chargement && !voir}
                  onClick={() => void charger().then((b) => (b ? enregistrerFichier(b, nom) : undefined))}>
                  {t('preuves.detail.telecharger')}
                </Button>
                <Button variant="primary" icon="eye" onClick={() => { setVoir(true); if (!piece?.blob) void charger() }}>{t('preuves.detail.voirPiece')}</Button>
              </>
            ) : null}
            <Button variant="ghost" onClick={onFermer}>{t('commun.fermer')}</Button>
          </>
        }
      >
        <div className="gm-col">
          {l.consigne ? <Note>{l.consigne}</Note> : null}
          <KeyValueList items={quoi} />
          {depose.length ? (
            <section aria-label={t('preuves.detail.depot')}>
              <h3 className="gm-type-h3">{t('preuves.detail.depot')}</h3>
              <KeyValueList items={depose} />
            </section>
          ) : <Note>{t('preuves.detail.aucunDepot')}</Note>}
          {piece?.conforme === false ? <p role="alert" className="gm-field-help">{t('preuves.empreinteDivergente')}</p> : null}
        </div>
      </Dialog>
      {voir ? (
        <AttachmentViewer
          open
          nom={nom}
          typeMime={typeMimeDe(nom) !== 'application/octet-stream' ? typeMimeDe(nom) : piece?.blob?.type || typeMimeDe(nom)}
          blob={piece?.blob}
          chargement={!piece || piece.chargement}
          erreur={piece?.erreur ?? undefined}
          onTelecharger={() => (piece?.blob ? enregistrerFichier(piece.blob, nom) : undefined)}
          onClose={() => setVoir(false)}
          convertir={services.apercu?.convertir}
        />
      ) : null}
    </>
  )
}
