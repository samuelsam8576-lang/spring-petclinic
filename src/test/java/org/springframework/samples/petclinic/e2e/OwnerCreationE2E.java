package org.springframework.samples.petclinic.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CT01 - Cadastro de proprietário com dados válidos (RF01).
 * <p>
 * Técnica: partição de equivalência (classe válida de cada campo). Evidência salva em
 * target/evidencias/CT01-cadastro-proprietario.png.
 */
class OwnerCreationE2E extends E2ESupport {

	@Test
	void cadastraProprietarioComDadosValidos() {
		driver.get(BASE_URL + "/owners/new");
		driver.findElement(By.id("firstName")).sendKeys("Ana");
		driver.findElement(By.id("lastName")).sendKeys("Souza");
		driver.findElement(By.id("address")).sendKeys("Rua das Flores, 100");
		driver.findElement(By.id("city")).sendKeys("Cruzeiro");
		driver.findElement(By.id("telephone")).sendKeys("1231234567");
		driver.findElement(By.cssSelector("#add-owner-form button[type='submit']")).click();

		wait.until(ExpectedConditions.urlMatches(".*/owners/\\d+$"));
		// A mensagem some da tela após 3 segundos, mas continua no HTML
		String mensagem = driver.findElement(By.id("success-message")).getDomProperty("textContent");
		salvarEvidencia("CT01-cadastro-proprietario");

		assertThat(mensagem).contains("New Owner Created");
		assertThat(textoDaPagina()).contains("Ana Souza", "Rua das Flores, 100", "Cruzeiro", "1231234567");
		System.out.println("CT01: proprietário criado em " + driver.getCurrentUrl());
	}

}
