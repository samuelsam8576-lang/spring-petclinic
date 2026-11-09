package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * CT02 - Telefone com 9, 10 e 11 dígitos (RF01).
 * <p>
 * Técnicas: análise do valor limite e partição de equivalência. O repositório é simulado
 * com Mockito, então o teste verifica só a regra do controller e da validação.
 */
@WebMvcTest(OwnerController.class)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerTelephoneBoundaryTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@ParameterizedTest(name = "telefone \"{0}\" é aceito")
	@ValueSource(strings = { "1234567890" })
	void aceitaTelefoneComExatamenteDezDigitos(String telefone) throws Exception {
		this.mockMvc.perform(cadastro(telefone)).andExpect(status().is3xxRedirection());
		verify(this.owners).save(any(Owner.class));
	}

	@ParameterizedTest(name = "telefone \"{0}\" é rejeitado")
	@ValueSource(strings = { "123456789", "12345678901", "(12)3456-7890", "12345abcde", "" })
	void rejeitaTelefoneForaDoPadrao(String telefone) throws Exception {
		this.mockMvc.perform(cadastro(telefone))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("owner", "telephone"))
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
		verify(this.owners, never()).save(any(Owner.class));
	}

	private RequestBuilder cadastro(String telefone) {
		return post("/owners/new").param("firstName", "Ana")
			.param("lastName", "Souza")
			.param("address", "Rua das Flores, 100")
			.param("city", "Cruzeiro")
			.param("telephone", telefone);
	}

}
