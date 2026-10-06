import { Sparkline } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col" style={{ maxWidth: 360 }}>
      <Sparkline label="Processeur de la passerelle" unit="%" aire points={[12, 14, 13, 18, 22, 19, 25, 31, 28, 34, 30, 33]} />
      <Sparkline label="Mémoire de MS-IAM" unit="Mo" serie={1} points={[512, 520, 518, null, null, 530, 541, 538, 552, 549]} />
      <Sparkline label="Latence de Keycloak (contexte)" unit="ms" serie="autre" points={[40, 38, 45, 52, 47, 44, 41, 39]} />
    </div>
  )
}
