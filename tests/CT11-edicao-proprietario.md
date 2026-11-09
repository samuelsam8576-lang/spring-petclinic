# CT11 Edição de proprietário

Requisito RF03. Técnica: partição de equivalência e consistência de identificação.

Este caso formaliza cinco métodos existentes de OwnerControllerTests: initUpdateOwnerForm, processUpdateOwnerFormSuccess, processUpdateOwnerFormUnchangedSuccess, processUpdateOwnerFormHasErrors e processUpdateOwnerFormWithIdMismatch.

Os cinco métodos passaram nos XML Surefire da execução inicial e da verificação final do CI. Eles já integram os 108 testes Maven e não são somados outra vez à suíte.

A matriz passa a ter 11 casos CT e 2 CNF: 13 itens, 10 aprovados e 3 reprovados (76,9%). Evidência: evidencias/fechamento-20261109/target/surefire-reports/TEST-org.springframework.samples.petclinic.owner.OwnerControllerTests.xml.
