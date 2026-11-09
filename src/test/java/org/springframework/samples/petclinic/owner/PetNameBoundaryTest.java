package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.time.LocalDate;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CT03 - Nome do pet com 0 a 31 caracteres (RF04).
 * <p>
 * Técnica: análise do valor limite. Teste unitário puro, sem Spring: chama o
 * {@link PetValidator} diretamente.
 */
@DisabledInNativeImage
class PetNameBoundaryTest {

	private final PetValidator validator = new PetValidator();

	private Pet pet;

	private Errors errors;

	@BeforeEach
	void setUp() {
		PetType dog = new PetType();
		dog.setName("dog");
		this.pet = new Pet();
		this.pet.setType(dog);
		this.pet.setBirthDate(LocalDate.of(2020, 1, 1));
		this.errors = new MapBindingResult(new HashMap<>(), "pet");
	}

	@ParameterizedTest(name = "nome com {0} caractere(s) é aceito")
	@ValueSource(ints = { 1, 29, 30 })
	void aceitaNomeDentroDoLimite(int tamanho) {
		this.pet.setName("A".repeat(tamanho));
		this.validator.validate(this.pet, this.errors);
		assertThat(this.errors.hasFieldErrors("name")).isFalse();
	}

	@ParameterizedTest(name = "nome com {0} caractere(s) é rejeitado")
	@ValueSource(ints = { 0, 31 })
	void rejeitaNomeForaDoLimite(int tamanho) {
		this.pet.setName("A".repeat(tamanho));
		this.validator.validate(this.pet, this.errors);
		assertThat(this.errors.hasFieldErrors("name")).isTrue();
	}

	@ParameterizedTest(name = "nome só com espaços (\"{0}\") é rejeitado")
	@ValueSource(strings = { " ", "   " })
	void rejeitaNomeSoComEspacos(String nome) {
		this.pet.setName(nome);
		this.validator.validate(this.pet, this.errors);
		assertThat(this.errors.hasFieldErrors("name")).isTrue();
	}

}
