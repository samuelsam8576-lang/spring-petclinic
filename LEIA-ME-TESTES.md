# Testes do Spring PetClinic Avaliação 2

Pacote da disciplina TS09 com testes, configuração Maven e evidências reais. Fork: https://github.com/samuelsam8576-lang/spring-petclinic

Base: c5f0ab5. Complementação validada em 30cc151: https://github.com/samuelsam8576-lang/spring-petclinic/actions/runs/42718416467

Copie src/, tests/, pom.xml e .github/ para uma cópia desse fork, preservando as pastas. O POM inclui Selenium e PMD 3.28.0. O gate escolhido dispensa token e conta SonarQube.

## Casos e resultados

CT01 a CT05, CT07, CT08 e CT11 aprovados. CT06, CT09 e CT10 reprovados. CNF01 e CNF02 aprovados pelos critérios documentados no relatório. Total: 13 itens, 10 aprovados e 3 reprovados (76,9%).

CT11 formaliza cinco métodos existentes de edição de OwnerControllerTests. A suíte Maven permanece com 108 testes (107 aprovados e 1 falha); Selenium tem 6 aprovações. A formalização não cria cinco execuções adicionais.

## Execução

Java 17, Chrome, Node.js e Docker são necessários para o fluxo completo. Use o Maven Wrapper do fork:

```bash
python3 tests/preparar-pom.py
./mvnw spring-javaformat:apply
./mvnw verify
./mvnw org.apache.maven.plugins:maven-pmd-plugin:3.28.0:check
```

O CT06 detecta o DEF01 enquanto a regra da data vazia não for corrigida. O código de negócio foi preservado para manter os defeitos reproduzíveis.

O ci-testes.yml executa testes e PMD nos commits de main e nos pull requests. O script tests/ci/avaliar-gate.py retorna falha por teste reprovado, cobertura insuficiente (75% global ou 85% em owner), apontamento PMD de prioridade 1 a 3, erro de ferramenta ou relatório obrigatório ausente. Os artefatos são publicados mesmo com gate reprovado.

O evidencias-automatico.yml conserva a coleta completa manual. O nao-funcionais.yml programa carga e ZAP às segundas, às 03h de Brasília. O coletor publica resultados; o job CI sinaliza o veredito do gate.

## Complementação real

O gate reprovou por uma falha de teste, cobertura owner de 81,8% e 15 apontamentos PMD de prioridade 3. O PMD terminou sem erro de análise. Seus apontamentos exigem triagem e não foram somados automaticamente aos sete defeitos confirmados.

A carga adicional de 50 usuários teve p95 de 5,25 ms, 57.949 requisições e 0% de falhas. Foram coletadas 273 amostras: CPU média Java de 15,3% da capacidade do runner, pico de 37,0%, RSS máximo de 517,4 MiB. Aplicação e k6 rodaram na mesma máquina com H2 em memória.

```bash
python3 tests/performance/monitorar-recursos.py --pid PID_JAVA --output resultados/recursos.csv
python3 tests/performance/resumir-recursos.py --input resultados/recursos.csv --k6 resultados/k6-monitorado.json --output resultados/recursos-resumo.json
```

## Evidências

- evidencias/github-actions-20261109/: suíte inicial, Selenium, três cargas, estresse, ZAP e comparativo java -jar/DevTools.
- evidencias/fechamento-20261109/: reexecução CI, PMD, quality-gate.json, carga monitorada e recursos.csv.
- execucao-real-20261109/ e seguranca-jar-20261109/: histórico das verificações HTTP anteriores.

## Escopo complementar

Aceitação com representante de clínica e avaliação por dois avaliadores de usabilidade foram excluídas deste ciclo acadêmico, que prioriza validação, integração, desempenho e segurança. A planilha conserva um roteiro para ciclo futuro, sem notas ou resultados atribuídos a pessoas.

As datas de continuação são propostas internas. O enunciado recebido não informa o prazo oficial de postagem nem de apresentação oral.

## Fontes técnicas

https://maven.apache.org/plugins/maven-pmd-plugin/check-mojo.html

https://pmd.github.io/pmd/pmd_userdocs_tools_maven.html
