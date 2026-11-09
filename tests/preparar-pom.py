#!/usr/bin/env python3
"""Prepara o pom.xml do fork para a rodada 2, sem edição manual.

Acrescenta a dependência do Selenium (escopo test) e a regra de cobertura do JaCoCo
(75% de desvios). Pode ser executado mais de uma vez: não duplica nada.
Uso, na raiz do repositório:  python3 tests/preparar-pom.py
"""
import pathlib
import sys
import xml.etree.ElementTree as ET

POM = pathlib.Path("pom.xml")
if not POM.exists():
    sys.exit("pom.xml não encontrado: rode o script na raiz do repositório.")
texto = POM.read_text(encoding="utf-8")

DEPENDENCIA = """    <dependency>
      <groupId>org.seleniumhq.selenium</groupId>
      <artifactId>selenium-java</artifactId>
      <scope>test</scope>
    </dependency>
"""
REGRA = """          <execution>
            <id>check</id>
            <goals>
              <goal>check</goal>
            </goals>
            <configuration>
              <rules>
                <rule>
                  <element>BUNDLE</element>
                  <limits>
                    <limit>
                      <counter>BRANCH</counter>
                      <value>COVEREDRATIO</value>
                      <minimum>0.75</minimum>
                    </limit>
                  </limits>
                </rule>
              </rules>
            </configuration>
          </execution>
"""

feito = []
if "selenium-java" not in texto:
    fim = texto.index("</dependencies>")
    if fim > texto.index("<build>"):
        sys.exit("Estrutura inesperada: </dependencies> depois de <build>.")
    linha = texto.rfind("\n", 0, fim) + 1
    texto = texto[:linha] + DEPENDENCIA + texto[linha:]
    feito.append("dependência do Selenium")

plugin = texto.index("jacoco-maven-plugin")
trecho_jacoco = texto[plugin: texto.index("</plugin>", plugin)]
if "<id>check</id>" not in trecho_jacoco:
    fim = texto.index("</executions>", plugin)
    linha = texto.rfind("\n", 0, fim) + 1
    texto = texto[:linha] + REGRA + texto[linha:]
    feito.append("regra de cobertura do JaCoCo")

ET.fromstring(texto)  # garante que o XML continua válido
POM.write_text(texto, encoding="utf-8")
print("pom.xml preparado:", ", ".join(feito) if feito else "nada a alterar (já estava pronto)")
