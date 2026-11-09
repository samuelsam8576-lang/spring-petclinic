package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * CT04 - Data de nascimento do pet em torno de hoje (RF04).
 * <p>
 * Técnica: análise do valor limite. As datas são calculadas a partir de
 * {@link LocalDate#now()} para o teste não quebrar com o passar do tempo.
 */
@WebMvcTest(value = PetController.class,
		includeFilters = @ComponentScan.Filter(value = PetTypeFormatter.class, type = FilterType.ASSIGNABLE_TYPE))
@DisabledInNativeImage
@DisabledInAotMode
class PetBirthDateBoundaryTest {

	private static final int OWNER_ID = 1;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetTypeRepository types;

	@BeforeEach
	void setup() {
		PetType dog = new PetType();
		dog.setId(2);
		dog.setName("dog");
		given(this.types.findPetTypes()).willReturn(List.of(dog));

		Owner owner = new Owner();
		owner.setId(OWNER_ID);
		given(this.owners.findById(OWNER_ID)).willReturn(Optional.of(owner));
	}

	@ParameterizedTest(name = "nascimento em hoje {0} dia(s) é aceito")
	@ValueSource(ints = { -1, 0 })
	void aceitaNascimentoAteHoje(int deslocamentoEmDias) throws Exception {
		this.mockMvc.perform(cadastro(LocalDate.now().plusDays(deslocamentoEmDias)))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void rejeitaNascimentoAmanha() throws Exception {
		this.mockMvc.perform(cadastro(LocalDate.now().plusDays(1)))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("pet", "birthDate"))
			.andExpect(view().name("pets/createOrUpdatePetForm"));
	}

	private RequestBuilder cadastro(LocalDate nascimento) {
		return post("/owners/{ownerId}/pets/new", OWNER_ID).param("name", "Thor")
			.param("type", "dog")
			.param("birthDate", nascimento.toString());
	}

}
