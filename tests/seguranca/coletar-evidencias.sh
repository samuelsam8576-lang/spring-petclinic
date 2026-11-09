#!/usr/bin/env bash
# Coleta evidências dos casos CT09 (página de erro) e CT10 (Actuator e console H2).
#
# Uso, com a aplicação rodando em localhost:8080:
#   bash tests/seguranca/coletar-evidencias.sh jar        (aplicação iniciada com java -jar)
#   bash tests/seguranca/coletar-evidencias.sh devtools   (aplicação iniciada com ./mvnw spring-boot:run)
#
# Funciona no Linux, no macOS e no Git Bash do Windows.
set -u

BASE="${BASE_URL:-http://localhost:8080}"
ROTULO="${1:-execucao}"
DESTINO="evidencias/seguranca-${ROTULO}-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$DESTINO"

registrar() {
  local caso="$1" rota="$2" arquivo="$3" status
  status=$(curl -s -o "$DESTINO/$arquivo" -w "%{http_code}" "$BASE$rota")
  echo "$caso;$rota;$status" | tee -a "$DESTINO/resumo.csv"
}

echo "caso;rota;status_http" > "$DESTINO/resumo.csv"

# CT09 - erros provocados
registrar CT09 /oups ct09-oups.html
registrar CT09 /owners/9999 ct09-proprietario-inexistente.html
registrar CT09 /rota-inexistente ct09-rota-inexistente.html

# CT10 - endpoints administrativos sem credencial
registrar CT10 /actuator ct10-actuator.json
registrar CT10 /actuator/env ct10-actuator-env.json
registrar CT10 /actuator/beans ct10-actuator-beans.json
registrar CT10 /h2-console ct10-h2-console.html
# O heap dump é grande: registra só o status e descarta o conteúdo
status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/actuator/heapdump")
echo "CT10;/actuator/heapdump;$status" | tee -a "$DESTINO/resumo.csv"

echo
echo "CT09 - procurando detalhes internos nas páginas de erro:"
if grep -i -l -E "exception|at org\.springframework|\.java:[0-9]+|not found with id" "$DESTINO"/ct09-*.html; then
  echo "(os arquivos acima contêm detalhes internos; anexe-os como evidência)"
else
  echo "Nenhum detalhe interno encontrado."
fi

echo
echo "Evidências salvas em $DESTINO"
