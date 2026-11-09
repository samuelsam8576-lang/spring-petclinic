package org.springframework.samples.petclinic.e2e;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CT06 (parte do navegador) - Visita marcada para ontem, hoje e amanhã (RF06).
 * <p>
 * Técnica: análise do valor limite. O campo de data tem o atributo HTML {@code min} igual
 * a amanhã, então para ontem e hoje o esperado é o navegador impedir o envio. A regra do
 * servidor é verificada em {@code VisitDateRuleIntegrationTest}.
 */
class VisitBookingE2E extends E2ESupport {

	private static final String FORMULARIO = "/owners/1/pets/1/visits/new";

	@Test
	void agendaVisitaParaAmanha() {
		LocalDate amanha = LocalDate.now().plusDays(1);
		driver.get(BASE_URL + FORMULARIO);
		preencherData(By.id("date"), amanha);
		driver.findElement(By.id("description")).sendKeys("Vacina antirrábica");
		enviar();

		wait.until(ExpectedConditions.urlMatches(".*/owners/1$"));
		salvarEvidencia("CT06-visita-amanha");
		assertThat(textoDaPagina()).contains(amanha.toString(), "Vacina antirrábica");
	}

	@ParameterizedTest(name = "visita em hoje {0} dia(s) não é enviada")
	@ValueSource(ints = { -1, 0 })
	void navegadorBloqueiaVisitaQueNaoEstaNoFuturo(int deslocamentoEmDias) {
		driver.get(BASE_URL + FORMULARIO);
		preencherData(By.id("date"), LocalDate.now().plusDays(deslocamentoEmDias));
		driver.findElement(By.id("description")).sendKeys("Vacina antirrábica");
		enviar();

		Object abaixoDoMinimo = ((JavascriptExecutor) driver)
			.executeScript("return document.getElementById('date').validity.rangeUnderflow;");
		salvarEvidencia("CT06-visita-" + (deslocamentoEmDias == 0 ? "hoje" : "ontem"));
		assertThat(abaixoDoMinimo).isEqualTo(Boolean.TRUE);
		assertThat(driver.getCurrentUrl()).endsWith(FORMULARIO);
	}

	private void enviar() {
		driver.findElement(By.cssSelector("form.form-horizontal button[type='submit']")).click();
	}

}
