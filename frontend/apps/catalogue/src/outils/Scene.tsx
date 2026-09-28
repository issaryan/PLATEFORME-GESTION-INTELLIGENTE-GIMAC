import type { ReactNode } from 'react'

// Scène des aperçus de dialogues : une page en arrière-plan sous le voile gm-color-overlay,
// reprise à l'identique de la fonction stage() des aperçus publiés.
export function Scene({ hauteur, titre = 'Institutions membres', aide = true, children }: { hauteur: number; titre?: string; aide?: boolean; children: ReactNode }) {
  return (
    <div className="gm-stage" style={{ height: hauteur }}>
      <div className="gm-pad" aria-hidden="true">
        <h1 className="gm-heading-display">{titre}</h1>
        {aide ? <p className="gm-field-help">Page en arrière-plan, sous le voile gm-color-overlay.</p> : null}
      </div>
      {children}
    </div>
  )
}
