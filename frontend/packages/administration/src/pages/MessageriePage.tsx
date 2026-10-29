import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, Checkbox, CrudDialog, KeyValueList, NumberField, PageHeader, StatusBadge, TextArea, TextField } from '@gimac/design-system'
import { useMessageErreur, type MessageErreur } from '@gimac/compte'
import { useEffet, useMessagerie } from '../hooks'
import type { ConfigurationMessagerie } from '../modele/configuration'
import { Annonce } from '../composants/Annonce'
import { type Retour } from '../composants/actes'
import { Donnees } from '../composants/communs'

// Messagerie de la plateforme (D-62, PLATFORM_MESSAGING_EDIT) : serveur d'envoi des invitations et
// des réinitialisations, sans passer par la console du fournisseur d'identité. Le mot de passe est en
// écriture seule : MS-IAM ne le restitue jamais (« défini ») ; un champ vide conserve l'existant.
// Configuration en lecture ; modification et courriel de test dans des fenêtres modales.

type Saisie = Omit<ConfigurationMessagerie, 'port' | 'utilisateur' | 'motDePasse'> & { port: string; utilisateur: string; motDePasse: string }
const versSaisie = (m: ConfigurationMessagerie): Saisie => ({ ...m, port: String(m.port), utilisateur: m.utilisateur ?? '', motDePasse: '' })

/** Modification du serveur d'envoi dans une fenêtre modale, avec motif (journal d'administration). */
function DialogueMessagerie({ messagerie, onFermer, onEnregistree }: { messagerie: ConfigurationMessagerie; onFermer: () => void; onEnregistree: () => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const appliquer = useEffet((s, v: { m: ConfigurationMessagerie; motif: string }) => s.configuration.appliquerMessagerie(v.m, v.motif))
  const [v, setV] = useState<Saisie>(() => versSaisie(messagerie))
  const [motif, setMotif] = useState('')
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const [refus, setRefus] = useState<MessageErreur | null>(null)
  const champ = (k: keyof Saisie) => (x: string | boolean) => setV((o) => ({ ...o, [k]: x }))

  const soumettre = async () => {
    const port = Number(v.port)
    const locales: Record<string, string> = {}
    if (!v.hote.trim()) locales.hote = t('console.creation.requis')
    if (!Number.isInteger(port) || port < 1 || port > 65535) locales.port = t('console.messagerie.port.invalide')
    if (!v.expediteur.trim()) locales.expediteur = t('console.creation.requis')
    if (v.authentification && !v.utilisateur.trim()) locales.utilisateur = t('console.creation.requis')
    if (motif.trim().length < 10) locales.motif = t('console.motif.aide')
    setErreurs(locales)
    setRefus(null)
    if (Object.keys(locales).length) return
    try {
      await appliquer.mutateAsync({
        m: { ...v, port, hote: v.hote.trim(), expediteur: v.expediteur.trim(), utilisateur: v.authentification ? v.utilisateur.trim() : null, motDePasse: v.motDePasse || null },
        motif: motif.trim(),
      })
      onEnregistree()
    } catch (x) {
      setRefus(message(x))
    }
  }

  return (
    <CrudDialog context="edit" id="gm-dlg-messagerie" title={t('console.messagerie.modifier')} subject={`${messagerie.hote}:${messagerie.port}`} description={t('console.messagerie.texte')} onClose={onFermer} onSubmit={() => void soumettre()}
      dirty={motif.trim() !== '' || JSON.stringify(v) !== JSON.stringify(versSaisie(messagerie))}
      submitLabel={t('console.messagerie.enregistrer')} submitIcon="pen-line" submitting={appliquer.isPending}
      error={refus ? { title: refus.texte, correlationId: refus.correlationId } : undefined}>
      <fieldset className="gm-col" style={{ border: 0, margin: 0, padding: 0 }}>
        <legend className="gm-type-h3" style={{ padding: 0, marginBottom: 'var(--gm-space-3)' }}>{t('console.messagerie.serveur')}</legend>
        <div className="gm-form-grid">
          <div className="gm-form-field"><TextField name="hote" label={t('console.messagerie.hote')} required mono value={v.hote} onChange={champ('hote')} error={erreurs.hote} /></div>
          <div className="gm-form-field"><NumberField name="port" label={t('console.messagerie.port')} required value={v.port} onChange={champ('port')} error={erreurs.port} help={t('console.messagerie.port.aide')} /></div>
          <div className="gm-form-field"><TextField name="expediteur" type="email" label={t('console.messagerie.expediteur')} required value={v.expediteur} onChange={champ('expediteur')} error={erreurs.expediteur} /></div>
          <div className="gm-form-field"><TextField name="nomExpediteur" label={t('console.messagerie.nomExpediteur')} value={v.nomExpediteur} onChange={champ('nomExpediteur')} /></div>
          <div className="gm-form-field"><Checkbox name="starttls" label={t('console.messagerie.starttls')} checked={v.starttls} onChange={champ('starttls')} /></div>
          <div className="gm-form-field"><Checkbox name="ssl" label={t('console.messagerie.ssl')} checked={v.ssl} onChange={champ('ssl')} /></div>
        </div>
      </fieldset>
      <fieldset className="gm-col" style={{ border: 0, margin: 0, padding: 0 }}>
        <legend className="gm-type-h3" style={{ padding: 0, marginBottom: 'var(--gm-space-3)' }}>{t('console.messagerie.authentification')}</legend>
        <div className="gm-form-grid">
          <div className="gm-form-field gm-form-wide"><Checkbox name="authentification" label={t('console.messagerie.authentification.active')} checked={v.authentification} onChange={champ('authentification')} /></div>
          <div className="gm-form-field">
            <TextField name="utilisateur" label={t('console.messagerie.utilisateur')} value={v.utilisateur} onChange={champ('utilisateur')} error={erreurs.utilisateur} disabled={!v.authentification} />
          </div>
          <div className="gm-form-field">
            <TextField name="motDePasse" type="password" label={t('console.messagerie.motDePasse')} value={v.motDePasse} onChange={champ('motDePasse')} disabled={!v.authentification}
              help={t(messagerie.motDePasseDefini ? 'console.messagerie.motDePasse.defini' : 'console.messagerie.motDePasse.nonDefini')} />
          </div>
        </div>
      </fieldset>
      <TextArea name="motifMessagerie" label={t('console.motif')} required value={motif} onChange={setMotif} error={erreurs.motif} help={t('console.motif.aide')} />
    </CrudDialog>
  )
}

/** Courriel de test dans une fenêtre modale : vérifie la configuration enregistrée. */
function DialogueTest({ onFermer, onEnvoye }: { onFermer: () => void; onEnvoye: (destinataire: string) => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const tester = useEffet((s, d: string) => s.configuration.testerMessagerie(d), [])
  const [destinataire, setDestinataire] = useState('')
  const [refus, setRefus] = useState<MessageErreur | null>(null)
  const envoyer = async () => {
    setRefus(null)
    try {
      await tester.mutateAsync(destinataire.trim())
      onEnvoye(destinataire.trim())
    } catch (x) {
      setRefus(message(x))
    }
  }
  return (
    <CrudDialog context="confirm" role="dialog" size="sm" id="gm-dlg-test" title={t('console.messagerie.test')} description={t('console.messagerie.test.texte')} onClose={onFermer} onSubmit={() => void envoyer()}
      submitLabel={t('console.messagerie.test.envoyer')} submitIcon="send" submitting={tester.isPending}
      error={refus ? { title: refus.texte, correlationId: refus.correlationId } : undefined}>
      <TextField name="destinataire" type="email" label={t('console.messagerie.destinataire')} value={destinataire} onChange={setDestinataire} />
    </CrudDialog>
  )
}

export default function MessageriePage() {
  const { t } = useTranslation()
  const messagerie = useMessagerie()
  const [retour, setRetour] = useState<Retour>(null)
  const [dialogue, setDialogue] = useState<'modifier' | 'test' | null>(null)
  const oui = (b: boolean) => t(b ? 'console.oui' : 'console.non')
  return (
    <div className="gm-col">
      <PageHeader id="gm-messagerie-titre" title={t('console.messagerie.titre')} subtitle={t('console.messagerie.texte')}
        actions={messagerie.data ? (
          <>
            <Button icon="send" onClick={() => { setRetour(null); setDialogue('test') }}>{t('console.messagerie.test.envoyer')}</Button>
            <Button variant="primary" icon="pen-line" onClick={() => { setRetour(null); setDialogue('modifier') }}>{t('console.messagerie.modifier')}</Button>
          </>
        ) : undefined} />
      <Annonce retour={retour} />
      <Donnees requete={messagerie}>
        {(m) => (
          <>
            <Card title={t('console.messagerie.serveur')}>
              <KeyValueList items={[
                { key: t('console.messagerie.hote'), value: m.hote, mono: true },
                { key: t('console.messagerie.port'), value: String(m.port), mono: true },
                { key: t('console.messagerie.expediteur'), value: m.expediteur },
                { key: t('console.messagerie.nomExpediteur'), value: m.nomExpediteur || '-' },
                { key: t('console.messagerie.starttls'), value: oui(m.starttls) },
                { key: t('console.messagerie.ssl'), value: oui(m.ssl) },
              ]} />
            </Card>
            <Card title={t('console.messagerie.authentification')}>
              <KeyValueList items={[
                { key: t('console.messagerie.authentification.active'), value: oui(m.authentification) },
                { key: t('console.messagerie.utilisateur'), value: m.utilisateur ?? '-', mono: true },
                { key: t('console.messagerie.motDePasse'), value: <StatusBadge status={m.motDePasseDefini ? 'ok' : 'warn'} label={t(m.motDePasseDefini ? 'console.messagerie.etat.defini' : 'console.messagerie.etat.nonDefini')} /> },
              ]} />
            </Card>
            {dialogue === 'modifier' ? (
              <DialogueMessagerie messagerie={m} onFermer={() => setDialogue(null)}
                onEnregistree={() => { setDialogue(null); setRetour({ niveau: 'info', message: { texte: t('console.messagerie.enregistree') } }) }} />
            ) : null}
          </>
        )}
      </Donnees>
      {dialogue === 'test' ? (
        <DialogueTest onFermer={() => setDialogue(null)}
          onEnvoye={(d) => { setDialogue(null); setRetour({ niveau: 'info', message: { texte: t('console.messagerie.test.envoye', { destinataire: d }) } }) }} />
      ) : null}
    </div>
  )
}
