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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * CT07 - Transições da busca por sobrenome (RF02).
 * <p>
 * Técnica: teste baseado em estados (diagrama da seção 4.4 do relatório). Usa a
 * aplicação inteira com o banco H2 e os dados de {@code data.sql}, sem mocks, para
 * exercitar também a consulta do repositório.
 */
@SpringBootTest
class OwnerSearchStateTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	void doisOuMaisResultadosMostramALista() throws Exception {
		this.mockMvc.perform(get("/owners").param("lastName", "Davis"))
			.andExpect(status().isOk())
			.andExpect(view().name("owners/ownersList"))
			.andExpect(model().attribute("listOwners", hasSize(2)));
	}

	@Test
	void umResultadoVaiDiretoParaOsDetalhes() throws Exception {
		this.mockMvc.perform(get("/owners").param("lastName", "Franklin"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owners/1"));
	}

	@Test
	void nenhumResultadoVoltaAoFormularioComErro() throws Exception {
		this.mockMvc.perform(get("/owners").param("lastName", "Xyz"))
			.andExpect(status().isOk())
			.andExpect(view().name("owners/findOwners"))
			.andExpect(model().attributeHasFieldErrorCode("owner", "lastName", "notFound"));
	}

	@Test
	void sobrenomeVazioListaTodosEmDuasPaginas() throws Exception {
		this.mockMvc.perform(get("/owners").param("lastName", ""))
			.andExpect(status().isOk())
			.andExpect(view().name("owners/ownersList"))
			.andExpect(model().attribute("listOwners", hasSize(5)))
			.andExpect(model().attribute("totalPages", 2));
	}

	@Test
	void segundaPaginaTrazOsOutrosCinco() throws Exception {
		this.mockMvc.perform(get("/owners").param("lastName", "").param("page", "2"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("listOwners", hasSize(5)))
			.andExpect(model().attribute("currentPage", 2));
	}

	@ParameterizedTest(name = "página {0} volta para a página 1")
	@ValueSource(strings = { "0", "99" })
	void paginaForaDoIntervaloVoltaParaAPrimeira(String pagina) throws Exception {
		this.mockMvc.perform(get("/owners").param("lastName", "").param("page", pagina))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", containsString("page=1")));
	}

}
