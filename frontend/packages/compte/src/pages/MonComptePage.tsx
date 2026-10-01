import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, DataTable, InlineAlert, KeyValueList, Select, StatusBadge, Tag, TextField, type Column, Note, PageHeader } from '@gimac/design-system'
import { EtatsRequete, type CleSecurite } from '@gimac/socle'
import { LANGUES_COMPTE, type LangueCompte, type Moi, type SessionCompte } from '../modele'
import { useEnrolerCle, useFermerSession, useFermerSessions, useMesCles, useMesSessions, useModifierFiche, useMoi, useSupprimerCle } from '../hooks'
import { dateHeure, useLibelleProfil, useMessageErreur, type MessageErreur } from '../outils'
import { ModaleCompte, type ErreurModale } from '../composants/ModaleSaisie'
import { useSaisieMotDePasse } from '../composants/saisieMotDePasse'
import { useSaisieTotp } from '../composants/saisieTotp'

// Mon compte (D-62, D-65), commun aux trois couches : fiche personnelle, mot de passe et code TOTP,
// clés de sécurité FIDO2, sessions ouvertes. Toute modification se fait dans une fenêtre modale, sur la
// plateforme : aucune page du fournisseur d'identité n'est ouverte. Modale CRUD standard du système de
// conception (ModaleCompte), adaptée au contexte : modification, création, suppression, confirmation ; une
// saisie modifiée demande confirmation avant l'abandon.

type Retour = { niveau: 'info' | 'crit' | 'warn'; message: MessageErreur } | null

/** Résultat d'une action : InlineAlert du catalogue, annoncé (role status ou alert). */
function Annonce({ retour }: { retour: Retour }) {
  return retour ? <InlineAlert level={retour.niveau} title={retour.message.texte} correlationId={retour.message.correlationId} /> : null
}

function DialogueFiche({ moi, onFait, onFermer }: { moi: Moi; onFait: () => void; onFermer: () => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const modifier = useModifierFiche()
  const c = moi.compte
  const [initiale] = useState({ prenom: c.prenom ?? '', nom: c.nom ?? '', telephone: c.telephone ?? '', fonction: c.fonction ?? '', langue: (c.langue ?? 'fr') as LangueCompte })
  const [v, setV] = useState(initiale)
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const [erreur, setErreur] = useState<ErreurModale | null>(null)
  const champ = (k: keyof typeof v) => (x: string) => setV({ ...v, [k]: x })
  const soumettre = async () => {
    setErreurs({})
    setErreur(null)
    try {
      await modifier.mutateAsync({ prenom: v.prenom.trim(), nom: v.nom.trim(), telephone: v.telephone.trim() || null, fonction: v.fonction.trim() || null, langue: v.langue })
      onFait()
    } catch (x) {
      const m = message(x)
      if (m.code === 'IAM_PHONE_INVALID') setErreurs({ telephone: m.texte })
      else if (m.code === 'IAM_LANGUAGE_INVALID') setErreurs({ langue: m.texte })
      else setErreur({ title: m.texte, correlationId: m.correlationId })
    }
  }
  return (
    <ModaleCompte id="gm-dialogue-fiche" title={t('compte.fiche.modifier')} description={t('compte.fiche.administration')} onClose={onFermer}
      submitLabel={t('compte.fiche.enregistrer')} onSubmit={() => void soumettre()} submitting={modifier.isPending} error={erreur}
      dirty={JSON.stringify(v) !== JSON.stringify(initiale)}>
      <div className="gm-form-grid">
        <div className="gm-form-field"><TextField name="prenom" label={t('compte.prenom')} value={v.prenom} onChange={champ('prenom')} autoComplete="given-name" /></div>
        <div className="gm-form-field"><TextField name="nom" label={t('compte.nom')} value={v.nom} onChange={champ('nom')} autoComplete="family-name" /></div>
        <div className="gm-form-field"><TextField name="telephone" type="tel" label={t('compte.telephone')} value={v.telephone} onChange={champ('telephone')} error={erreurs.telephone} help={t('compte.telephone.aide')} autoComplete="tel" /></div>
        <div className="gm-form-field"><TextField name="fonction" label={t('compte.fonction')} value={v.fonction} onChange={champ('fonction')} /></div>
        <div className="gm-form-field">
          <Select name="langue" label={t('compte.langue')} value={v.langue} onChange={champ('langue')} error={erreurs.langue} options={LANGUES_COMPTE.map((l) => ({ value: l, label: t(`compte.langue.${l}`) }))} />
        </div>
      </div>
    </ModaleCompte>
  )
}

function ModaleMotDePasse({ onFait, onFermer }: { onFait: () => void; onFermer: () => void }) {
  const { t } = useTranslation()
  const saisie = useSaisieMotDePasse({ onFait })
  return (
    <ModaleCompte id="gm-dialogue-mdp" size="md" title={t('compte.securite.motDePasse')} description={t('compte.securite.motDePasse.texte')} onClose={onFermer}
      submitLabel={t('motDePasse.enregistrer')} submitIcon="key-round" onSubmit={() => void saisie.soumettre()} submitting={saisie.enCours} error={saisie.erreur}
      dirty={saisie.modifie}>
      {saisie.contenu}
    </ModaleCompte>
  )
}

function ModaleTotp({ onFait, onFermer }: { onFait: () => void; onFermer: () => void }) {
  const { t } = useTranslation()
  const saisie = useSaisieTotp({ onFait, remplacement: true })
  return (
    <ModaleCompte id="gm-dialogue-totp" title={t('compte.securite.secondFacteur')} description={t('compte.securite.secondFacteur.texte')} onClose={onFermer}
      submitLabel={t('totp.activer')} submitIcon="shield-check" onSubmit={() => void saisie.soumettre()} submitting={saisie.enCours}
      submitDisabled={!saisie.pret} submitDisabledReason={t('totp.attente')} dirty={saisie.modifie}>
      {saisie.contenu}
    </ModaleCompte>
  )
}

function Fiche({ moi }: { moi: Moi }) {
  const { t } = useTranslation()
  const libelleProfil = useLibelleProfil()
  const [ouvert, setOuvert] = useState(false)
  const [retour, setRetour] = useState<Retour>(null)
  const c = moi.compte
  return (
    <Card title={t('compte.fiche.titre')} actions={<Button size="sm" icon="pen-line" onClick={() => { setRetour(null); setOuvert(true) }}>{t('compte.fiche.modifier')}</Button>}>
      <div className="gm-col">
        <KeyValueList
          items={[
            { key: t('compte.identifiant'), value: c.identifiant, mono: true },
            { key: t('compte.email'), value: c.email ?? '—' },
            { key: t('compte.profils'), value: <span className="gm-row">{c.profils.map((p) => <Tag key={p}>{libelleProfil(p)}</Tag>)}</span> },
            { key: t('compte.prenom'), value: c.prenom || '—' },
            { key: t('compte.nom'), value: c.nom || '—' },
            { key: t('compte.telephone'), value: c.telephone || '—' },
            { key: t('compte.fonction'), value: c.fonction || '—' },
            { key: t('compte.langue'), value: c.langue ? t(`compte.langue.${c.langue}`) : '—' },
          ]}
        />
        <Note>{t('compte.fiche.administration')}</Note>
        <Annonce retour={retour} />
      </div>
      {ouvert ? (
        <DialogueFiche moi={moi} onFermer={() => setOuvert(false)}
          onFait={() => { setOuvert(false); setRetour({ niveau: 'info', message: { texte: t('compte.fiche.enregistree') } }) }} />
      ) : null}
    </Card>
  )
}

function Securite({ moi }: { moi: Moi }) {
  const { t } = useTranslation()
  const [dialogue, setDialogue] = useState<'MOT_DE_PASSE' | 'TOTP' | null>(null)
  const [retour, setRetour] = useState<Retour>(null)
  const ouvrir = (d: 'MOT_DE_PASSE' | 'TOTP') => {
    setRetour(null)
    setDialogue(d)
  }
  const fait = (cle: string) => {
    setDialogue(null)
    setRetour({ niveau: 'info', message: { texte: t(cle) } })
  }
  return (
    <Card title={t('compte.securite.titre')}>
      <div className="gm-col">
        <KeyValueList
          items={[
            { key: t('compte.secondFacteur'), value: <StatusBadge status={moi.compte.secondFacteur ? 'ok' : 'warn'} label={t(moi.compte.secondFacteur ? 'compte.secondFacteur.actif' : 'compte.secondFacteur.absent')} /> },
            { key: t('compte.email'), value: <StatusBadge status={moi.compte.emailVerifie ? 'ok' : 'warn'} label={t(moi.compte.emailVerifie ? 'compte.email.verifie' : 'compte.email.nonVerifie')} /> },
          ]}
        />
        <Note>{t('compte.securite.texte')}</Note>
        <div className="gm-row">
          <Button icon="key-round" onClick={() => ouvrir('MOT_DE_PASSE')}>{t('compte.securite.motDePasse')}</Button>
          <Button icon="smartphone" onClick={() => ouvrir('TOTP')}>{t('compte.securite.secondFacteur')}</Button>
        </div>
        <Annonce retour={retour} />
      </div>
      {dialogue === 'MOT_DE_PASSE' ? <ModaleMotDePasse onFait={() => fait('compte.securite.motDePasse.fait')} onFermer={() => setDialogue(null)} /> : null}
      {dialogue === 'TOTP' ? <ModaleTotp onFait={() => fait('compte.securite.secondFacteur.fait')} onFermer={() => setDialogue(null)} /> : null}
    </Card>
  )
}

function DialogueCle({ onFait, onFermer }: { onFait: (c: CleSecurite) => void; onFermer: () => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const enroler = useEnrolerCle()
  const [nom, setNom] = useState(t('premierAcces.cle.nomDefaut'))
  const [champ, setChamp] = useState<string | undefined>()
  const [erreur, setErreur] = useState<ErreurModale | null>(null)
  const soumettre = async () => {
    setErreur(null)
    setChamp(nom.trim() ? undefined : t('compte.cles.nom.requis'))
    if (!nom.trim()) return
    try {
      onFait(await enroler.mutateAsync(nom))
    } catch (x) {
      const m = message(x)
      setErreur({ title: m.texte, correlationId: m.correlationId })
    }
  }
  return (
    <ModaleCompte id="gm-dialogue-cle" context="create" size="md" title={t('compte.cles.enroler')} description={t('compte.cles.texte')} onClose={onFermer}
      submitLabel={t('compte.cles.enroler')} submitIcon="key-round" onSubmit={() => void soumettre()} submitting={enroler.isPending} error={erreur}>
      <TextField name="nomNouvelleCle" label={t('compte.cles.nom')} value={nom} onChange={setNom} maxLength={100} required error={champ} />
    </ModaleCompte>
  )
}

function Cles() {
  const { t, i18n } = useTranslation()
  const message = useMessageErreur()
  const cles = useMesCles()
  const supprimer = useSupprimerCle()
  const [ajout, setAjout] = useState(false)
  const [aSupprimer, setASupprimer] = useState<CleSecurite | null>(null)
  const [retour, setRetour] = useState<Retour>(null)
  const retirer = async () => {
    const c = aSupprimer!
    setASupprimer(null)
    try {
      await supprimer.mutateAsync(c.id)
      setRetour({ niveau: 'info', message: { texte: t('compte.cles.supprimee', { nom: c.nom }) } })
    } catch (x) {
      setRetour({ niveau: 'crit', message: message(x) })
    }
  }
  const colonnes: Column<CleSecurite>[] = [
    { key: 'nom', header: t('compte.cles.nom') },
    { key: 'creeeLe', header: t('compte.cles.creeeLe'), render: (c) => dateHeure(c.creeeLe, i18n.language) },
    { key: 'derniereUtilisation', header: t('compte.cles.derniereUtilisation'), render: (c) => (c.derniereUtilisation ? dateHeure(c.derniereUtilisation, i18n.language) : t('compte.cles.jamais')) },
    { key: 'actions', header: '', type: 'actions', render: (c) => <Button size="sm" variant="danger" onClick={() => { setRetour(null); setASupprimer(c) }}>{t('compte.cles.supprimer')}</Button> },
  ]
  return (
    <Card title={t('compte.cles.titre')} actions={<Button size="sm" variant="primary" icon="key-round" onClick={() => { setRetour(null); setAjout(true) }}>{t('compte.cles.enroler')}</Button>}>
      <div className="gm-col">
        <Note>{t('compte.cles.texte')}</Note>
        <EtatsRequete etat={cles} isEmpty={(l) => l.length === 0} emptyTitle={t('compte.cles.aucune')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
          {(l) => <DataTable columns={colonnes} rows={l} rowKey={(c) => c.id} density="compact" />}
        </EtatsRequete>
        <Annonce retour={retour} />
      </div>
      {ajout ? (
        <DialogueCle onFermer={() => setAjout(false)}
          onFait={(c) => { setAjout(false); setRetour({ niveau: 'info', message: { texte: t('compte.cles.enrolee', { nom: c.nom }) } }) }} />
      ) : null}
      {aSupprimer ? (
        <ModaleCompte id="gm-dialogue-suppression-cle" context="delete" title={t('compte.cles.supprimer')} description={t('compte.cles.supprimer.texte')}
          submitLabel={t('compte.cles.supprimer')} object={[{ key: t('compte.cles.nom'), value: aSupprimer.nom }]} onSubmit={() => void retirer()} onClose={() => setASupprimer(null)} />
      ) : null}
    </Card>
  )
}

function Sessions() {
  const { t, i18n } = useTranslation()
  const message = useMessageErreur()
  const sessions = useMesSessions()
  const fermer = useFermerSession()
  const fermerTout = useFermerSessions()
  const [aFermer, setAFermer] = useState<SessionCompte | 'TOUTES' | null>(null)
  const [retour, setRetour] = useState<Retour>(null)
  const agir = async (f: () => Promise<unknown>, succes: string) => {
    setAFermer(null)
    setRetour(null)
    try {
      await f()
      setRetour({ niveau: 'info', message: { texte: succes } })
    } catch (x) {
      setRetour({ niveau: 'crit', message: message(x) })
    }
  }
  const colonnes: Column<SessionCompte>[] = [
    { key: 'adresseIp', header: t('compte.sessions.adresse'), render: (s) => <span className="gm-mono">{s.adresseIp ?? '—'}</span> },
    { key: 'debut', header: t('compte.sessions.debut'), render: (s) => dateHeure(s.debut, i18n.language) },
    { key: 'dernierAcces', header: t('compte.sessions.dernierAcces'), render: (s) => dateHeure(s.dernierAcces, i18n.language) },
    { key: 'applications', header: t('compte.sessions.applications'), render: (s) => s.applications.join(', ') },
    { key: 'actions', header: '', type: 'actions', render: (s) => <Button size="sm" onClick={() => setAFermer(s)}>{t('compte.sessions.fermer')}</Button> },
  ]
  return (
    <Card title={t('compte.sessions.titre')} actions={<Button size="sm" variant="danger" onClick={() => setAFermer('TOUTES')}>{t('compte.sessions.fermerToutes')}</Button>}>
      <div className="gm-col">
        <EtatsRequete etat={sessions} isEmpty={(l) => l.length === 0} emptyTitle={t('compte.sessions.aucune')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
          {(l) => <DataTable columns={colonnes} rows={l} rowKey={(s) => s.id} density="compact" />}
        </EtatsRequete>
        <Annonce retour={retour} />
      </div>
      {aFermer === 'TOUTES' ? (
        <ModaleCompte id="gm-dialogue-sessions" context="delete" irreversible={false} title={t('compte.sessions.fermerToutes')} description={t('compte.sessions.fermerToutes.texte')}
          submitLabel={t('compte.sessions.fermerToutes')} onSubmit={() => void agir(() => fermerTout.mutateAsync(undefined), t('compte.sessions.fermees'))} onClose={() => setAFermer(null)} />
      ) : aFermer ? (
        <ModaleCompte id="gm-dialogue-session" context="confirm" title={t('compte.sessions.fermer.titre')} description={t('compte.sessions.fermer.texte')}
          submitLabel={t('compte.sessions.fermer.titre')}
          object={[{ key: t('compte.sessions.adresse'), value: aFermer.adresseIp ?? '—' }, { key: t('compte.sessions.debut'), value: dateHeure(aFermer.debut, i18n.language) }]}
          onSubmit={() => void agir(() => fermer.mutateAsync(aFermer.id), t('compte.sessions.fermee'))} onClose={() => setAFermer(null)} />
      ) : null}
    </Card>
  )
}

export default function MonComptePage() {
  const { t } = useTranslation()
  const moi = useMoi()
  useEffect(() => {
    document.title = `GIMAC · ${t('compte.titre')}`
  }, [t])
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)' }}>
      <PageHeader title={t('compte.titre')} subtitle={t('compte.sousTitre')} />
      <EtatsRequete etat={moi} emptyTitle={t('commun.vide')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
        {(m) => (
          <>
            <Fiche moi={m} />
            <Securite moi={m} />
            <Cles />
            <Sessions />
          </>
        )}
      </EtatsRequete>
    </div>
  )
}
