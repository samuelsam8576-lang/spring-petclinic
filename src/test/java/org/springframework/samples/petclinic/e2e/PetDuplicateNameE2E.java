package org.springframework.samples.petclinic.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CT05 - Nome de pet duplicado com maiúsculas diferentes (RF05).
 * <p>
 * Técnica: tabela de decisão (regras R5 e R6). O proprietário 1 (George Franklin) já tem o
 * pet "Leo". Os testes tentam cadastrar "leo" e "LEO", que devem ser rejeitados, e "Lucky",
 * nome que existe em outro proprietário e deve ser aceito. Prints salvos em
 * target/evidencias.
 */
class PetDuplicateNameE2E extends E2ESupport {

	private static final String FORMULARIO = "/owners/1/pets/new";

	private static final By ERRO_NO_CAMPO = By.cssSelector(".has-error .help-inline");

	@Test
	void rejeitaNomeDuplicadoComMaiusculasDiferentes() {
		cadastrar("leo");
		wait.until(ExpectedConditions.presenceOfElementLocated(ERRO_NO_CAMPO));
		salvarEvidencia("CT05-leo-rejeitado");
		assertThat(driver.getCurrentUrl()).endsWith(FORMULARIO);
		assertThat(driver.findElements(ERRO_NO_CAMPO)).isNotEmpty();

		cadastrar("LEO");
		wait.until(ExpectedConditions.presenceOfElementLocated(ERRO_NO_CAMPO));
		salvarEvidencia("CT05-LEO-rejeitado");
		assertThat(driver.getCurrentUrl()).endsWith(FORMULARIO);
		assertThat(driver.findElements(ERRO_NO_CAMPO)).isNotEmpty();
	}

	@Test
	void aceitaNomeQueSoExisteEmOutroProprietario() {
		cadastrar("Lucky");
		wait.until(ExpectedConditions.urlMatches(".*/owners/1$"));
		salvarEvidencia("CT05-lucky-aceito");
		assertThat(textoDaPagina()).contains("Lucky");
	}

	private void cadastrar(String nome) {
		driver.get(BASE_URL + FORMULARIO);
		driver.findElement(By.id("name")).sendKeys(nome);
		preencherData(By.id("birthDate"), LocalDate.of(2023, 5, 10));
		new Select(driver.findElement(By.id("type"))).selectByVisibleText("cat");
		driver.findElement(By.cssSelector("form.form-horizontal button[type='submit']")).click();
	}

}
