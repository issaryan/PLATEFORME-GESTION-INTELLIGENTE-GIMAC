#!/usr/bin/env bash
# Installe, en versions épinglées, les outils de validation de l'infrastructure (D-61).
# Utilisé par la CI (.github/workflows/ci.yml) et utilisable sur un poste Linux x86_64.
#
#   infra/ci/installer-outils.sh                    # tous les outils, dans ~/.local/bin
#   infra/ci/installer-outils.sh gitleaks opa       # outils choisis
#   GIMAC_DOSSIER_OUTILS=/opt/outils infra/ci/installer-outils.sh
#   export PATH="$HOME/.local/bin:$PATH"
#
# Outils : shellcheck opa kustomize kubeconform promtool amtool hadolint actionlint gitleaks yamllint
#
# Les versions d'OPA, de promtool et d'amtool suivent celles des images du Compose
# (openpolicyagent/opa, prom/prometheus, prom/alertmanager) : on valide avec l'outil
# qui exécutera réellement la configuration.
set -euo pipefail

DOSSIER="${GIMAC_DOSSIER_OUTILS:-$HOME/.local/bin}"
TOUS=(shellcheck opa kustomize kubeconform promtool amtool hadolint actionlint gitleaks yamllint)
demandes=("$@")
[ "${#demandes[@]}" -gt 0 ] || demandes=("${TOUS[@]}")
for o in "${demandes[@]}"; do
  [[ " ${TOUS[*]} " == *" $o "* ]] || { echo "Outil inconnu : $o (${TOUS[*]})" >&2; exit 2; }
done
voulu() { [[ " ${demandes[*]} " == *" $1 "* ]]; }

VERSION_SHELLCHECK=0.11.0
VERSION_OPA=1.21.0
VERSION_KUSTOMIZE=5.8.1
VERSION_KUBECONFORM=0.8.0
VERSION_PROMETHEUS=3.14.0
VERSION_ALERTMANAGER=0.34.1
VERSION_HADOLINT=2.14.0
VERSION_ACTIONLINT=1.7.12
VERSION_GITLEAKS=8.30.1
VERSION_YAMLLINT=1.37.1

mkdir -p "$DOSSIER"
temp="$(mktemp -d)"
trap 'rm -rf "$temp"' EXIT

telecharger() { curl -fsSL --retry 3 --retry-delay 2 -o "$2" "$1"; }
installe() { echo "  $1 $2 -> $DOSSIER/$1"; }

gh="https://github.com"

if voulu shellcheck; then
  telecharger "$gh/koalaman/shellcheck/releases/download/v$VERSION_SHELLCHECK/shellcheck-v$VERSION_SHELLCHECK.linux.x86_64.tar.xz" "$temp/sc.tar.xz"
  tar -xJf "$temp/sc.tar.xz" -C "$temp"
  install -m 0755 "$temp/shellcheck-v$VERSION_SHELLCHECK/shellcheck" "$DOSSIER/shellcheck"
  installe shellcheck "$VERSION_SHELLCHECK"
fi

if voulu opa; then
  telecharger "$gh/open-policy-agent/opa/releases/download/v$VERSION_OPA/opa_linux_amd64_static" "$DOSSIER/opa"
  chmod 0755 "$DOSSIER/opa"
  installe opa "$VERSION_OPA"
fi

if voulu kustomize; then
  telecharger "$gh/kubernetes-sigs/kustomize/releases/download/kustomize%2Fv$VERSION_KUSTOMIZE/kustomize_v${VERSION_KUSTOMIZE}_linux_amd64.tar.gz" "$temp/kustomize.tgz"
  tar -xzf "$temp/kustomize.tgz" -C "$DOSSIER" kustomize
  installe kustomize "$VERSION_KUSTOMIZE"
fi

if voulu kubeconform; then
  telecharger "$gh/yannh/kubeconform/releases/download/v$VERSION_KUBECONFORM/kubeconform-linux-amd64.tar.gz" "$temp/kubeconform.tgz"
  tar -xzf "$temp/kubeconform.tgz" -C "$DOSSIER" kubeconform
  installe kubeconform "$VERSION_KUBECONFORM"
fi

if voulu promtool; then
  telecharger "$gh/prometheus/prometheus/releases/download/v$VERSION_PROMETHEUS/prometheus-$VERSION_PROMETHEUS.linux-amd64.tar.gz" "$temp/prometheus.tgz"
  tar -xzf "$temp/prometheus.tgz" -C "$DOSSIER" --strip-components=1 "prometheus-$VERSION_PROMETHEUS.linux-amd64/promtool"
  installe promtool "$VERSION_PROMETHEUS"
fi

if voulu amtool; then
  telecharger "$gh/prometheus/alertmanager/releases/download/v$VERSION_ALERTMANAGER/alertmanager-$VERSION_ALERTMANAGER.linux-amd64.tar.gz" "$temp/alertmanager.tgz"
  tar -xzf "$temp/alertmanager.tgz" -C "$DOSSIER" --strip-components=1 "alertmanager-$VERSION_ALERTMANAGER.linux-amd64/amtool"
  installe amtool "$VERSION_ALERTMANAGER"
fi

if voulu hadolint; then
  telecharger "$gh/hadolint/hadolint/releases/download/v$VERSION_HADOLINT/hadolint-linux-x86_64" "$DOSSIER/hadolint"
  chmod 0755 "$DOSSIER/hadolint"
  installe hadolint "$VERSION_HADOLINT"
fi

if voulu actionlint; then
  telecharger "$gh/rhysd/actionlint/releases/download/v$VERSION_ACTIONLINT/actionlint_${VERSION_ACTIONLINT}_linux_amd64.tar.gz" "$temp/actionlint.tgz"
  tar -xzf "$temp/actionlint.tgz" -C "$DOSSIER" actionlint
  installe actionlint "$VERSION_ACTIONLINT"
fi

if voulu gitleaks; then
  telecharger "$gh/gitleaks/gitleaks/releases/download/v$VERSION_GITLEAKS/gitleaks_${VERSION_GITLEAKS}_linux_x64.tar.gz" "$temp/gitleaks.tgz"
  tar -xzf "$temp/gitleaks.tgz" -C "$DOSSIER" gitleaks
  installe gitleaks "$VERSION_GITLEAKS"
fi

if voulu yamllint; then
  if command -v pipx >/dev/null 2>&1; then
    pipx install --force "yamllint==$VERSION_YAMLLINT" >/dev/null
  else
    python3 -m pip install --user --quiet "yamllint==$VERSION_YAMLLINT"
  fi
  echo "  yamllint $VERSION_YAMLLINT -> dossier des commandes de pipx ou pip --user"
fi

echo "Outils installés dans $DOSSIER (à ajouter au PATH)."
