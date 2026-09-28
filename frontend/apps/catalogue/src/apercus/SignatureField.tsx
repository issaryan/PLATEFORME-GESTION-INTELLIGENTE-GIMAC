import { SignatureField } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col" style={{ maxWidth: 640 }}>
      <SignatureField state="idle" action="Signer le quitus technique" hash="3f9a1c7e2b4d8f01a6c5e9b2d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f" />
      <SignatureField state="waiting" action="Clore l'opération" />
      <SignatureField state="done" at="28/09/2026 14:05" />
    </div>
  )
}
