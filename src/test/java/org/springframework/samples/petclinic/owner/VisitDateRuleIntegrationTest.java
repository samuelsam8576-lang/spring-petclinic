package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * CT06 (parte do servidor) - Visita marcada para ontem, hoje, amanhã e sem data (RF06).
 * <p>
 * O formulário tem o atributo HTML {@code min} igual a amanhã, então o navegador bloqueia
 * datas passadas antes de enviar (verificado em {@code VisitBookingE2E}). Este teste
 * envia as requisições direto ao servidor, sem navegador, para verificar a regra do
 * lado do servidor, que é a que realmente protege os dados.
 * <p>
 * A classe não usa {@code @Transactional} de propósito: o controller adiciona a visita ao
 * pet antes de validar, e uma transação de teste aberta faria essa visita rejeitada ser
 * gravada, distorcendo o resultado.
 */
@SpringBootTest
class VisitDateRuleIntegrationTest {

	private static final String FORMULARIO = "/owners/1/pets/1/visits/new";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private OwnerRepository owners;

	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@ParameterizedTest(name = "visita em hoje {0} dia(s) é rejeitada")
	@ValueSource(ints = { -1, 0 })
	void rejeitaVisitaQueNaoEstaNoFuturo(int deslocamentoEmDias) throws Exception {
		int antes = visitasDoLeo();
		this.mockMvc
			.perform(post(FORMULARIO).param("date", LocalDate.now().plusDays(deslocamentoEmDias).toString())
				.param("description", "Vacina antirrábica"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("visit", "date"))
			.andExpect(view().name("pets/createOrUpdateVisitForm"));
		assertThat(visitasDoLeo()).isEqualTo(antes);
	}

	@Test
	void aceitaVisitaAmanha() throws Exception {
		int antes = visitasDoLeo();
		this.mockMvc
			.perform(post(FORMULARIO).param("date", LocalDate.now().plusDays(1).toString())
				.param("description", "Vacina antirrábica"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owners/1"));
		assertThat(visitasDoLeo()).isEqualTo(antes + 1);
	}

	/**
	 * Teste exploratório. O código só verifica a data quando ela é informada, e a coluna
	 * {@code visit_date} aceita nulo. Pelo requisito RF06, uma visita sem data deve ser
	 * rejeitada. Se este teste falhar, registre o defeito na planilha (seção 9.3 do
	 * relatório) com a saída do Maven como evidência.
	 */
	@Test
	void rejeitaVisitaSemData() throws Exception {
		int antes = visitasDoLeo();
		this.mockMvc.perform(post(FORMULARIO).param("date", "").param("description", "Retorno sem data"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("visit", "date"));
		assertThat(visitasDoLeo()).isEqualTo(antes);
	}

	private int visitasDoLeo() {
		return this.owners.findById(1).orElseThrow().getPet(1).getVisits().size();
	}

}
