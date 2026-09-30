#!/usr/bin/env bash
# Validation des microservices JHipster (D-61) : compilation, Checkstyle, tests unitaires
# (Surefire), tests d'intégration Testcontainers (Failsafe), tests d'acceptation Cucumber,
# Modernizer et seuils JaCoCo (≥ 80 %) déclarés dans chaque pom.xml.
#
#   infra/ci/valider-backend.sh                    # les 14 services, l'un après l'autre
#   infra/ci/valider-backend.sh ms-iam ms-gateway  # services choisis
#   infra/ci/valider-backend.sh --lister-json      # services munis de tests (matrice de la CI)
#
# Variables :
#   GIMAC_MAVEN_OBJECTIFS  objectifs Maven (défaut : « clean verify »)
#   GIMAC_MAVEN_OPTIONS    options Maven supplémentaires (ex. « -Pprod »)
#   GIMAC_SANS_INTEGRATION=1  saute les tests d'intégration (-DskipITs) ; poste sans Docker
#
# Les tests d'intégration démarrent PostgreSQL, Kafka, etc. par Testcontainers : un démon
# Docker est nécessaire (présent sur les exécuteurs ubuntu de GitHub Actions).
set -euo pipefail

# shellcheck source=infra/ci/commun.sh
source "$(dirname "${BASH_SOURCE[0]}")/commun.sh"
cd "$RACINE_DEPOT"

# Nombre de classes de test (unitaires et d'intégration) d'un service.
compter_tests() {
  find "$RACINE_DEPOT/$1/src/test" -name '*.java' \( -name '*Test.java' -o -name '*IT.java' -o -name '*IntTest.java' -o -name '*Tests.java' \) 2>/dev/null | wc -l
}

# Le service déclare-t-il un contrôle de seuil JaCoCo (goal « check ») ?
a_seuil_jacoco() {
  sed -n '/<artifactId>jacoco-maven-plugin<\/artifactId>/,/<\/plugin>/p' "$RACINE_DEPOT/$1/pom.xml" | grep -q '<goal>check</goal>'
}

if [ "${1:-}" = "--lister-json" ]; then
  premiers=1
  printf '['
  while read -r s; do
    [ "$(compter_tests "$s")" -gt 0 ] || continue
    if [ "$premiers" = 1 ]; then premiers=0; else printf ','; fi
    printf '"%s"' "$s"
  done < <(lister_services)
  printf ']\n'
  exit 0
fi

services=("$@")
[ "${#services[@]}" -gt 0 ] || mapfile -t services < <(lister_services)

read -r -a objectifs <<<"${GIMAC_MAVEN_OBJECTIFS:-clean verify}"
read -r -a options <<<"${GIMAC_MAVEN_OPTIONS:-}"

section "Prérequis"
if ! outil_present java; then
  echec "Java introuvable (JDK 21 requis)"; fin_section; bilan "Validation du backend"; exit 1
fi
info "$(java -version 2>&1 | head -1)"
if [ "${GIMAC_SANS_INTEGRATION:-0}" = 1 ]; then
  options+=(-DskipITs)
  ignore "tests d'intégration sautés (GIMAC_SANS_INTEGRATION=1)"
elif ! outil_present docker || ! docker info >/dev/null 2>&1; then
  # Sans Docker, Testcontainers échoue : on le dit tout de suite plutôt qu'au bout du build.
  if [ "${GIMAC_CI_STRICT:-0}" = 1 ]; then
    echec "démon Docker inaccessible : tests d'intégration Testcontainers impossibles"
  else
    options+=(-DskipITs)
    ignore "démon Docker inaccessible : tests d'intégration sautés (-DskipITs), seuil JaCoCo global calculé sur les seuls tests unitaires"
  fi
fi
fin_section

for service in "${services[@]}"; do
  service="${service%/}"
  section "$service"
  if [ ! -f "$service/pom.xml" ]; then
    echec "$service : pom.xml introuvable"; fin_section; continue
  fi
  nb_tests="$(compter_tests "$service")"
  if [ "$nb_tests" -eq 0 ]; then
    # Squelette sans test : seule la compilation est vérifiée, et c'est signalé (D-61).
    ignore "$service : aucun test, compilation seule"
    controler "$service : compilation" "$service/mvnw" -f "$service/pom.xml" -ntp -B "${options[@]}" -DskipTests package
    fin_section; continue
  fi
  if a_seuil_jacoco "$service"; then
    info "$nb_tests classes de test ; seuils JaCoCo déclarés dans le pom.xml"
  else
    info "$nb_tests classes de test ; aucun seuil JaCoCo déclaré (rapport de couverture seul)"
    if [ -n "${GITHUB_ACTIONS:-}" ]; then echo "::warning title=Couverture $service::aucun seuil JaCoCo (jacoco:check) dans $service/pom.xml"; fi
  fi
  controler "$service : mvnw ${objectifs[*]} ${options[*]}" \
    "$service/mvnw" -f "$service/pom.xml" -ntp -B "${options[@]}" "${objectifs[@]}"
  fin_section
done

bilan "Validation du backend"
