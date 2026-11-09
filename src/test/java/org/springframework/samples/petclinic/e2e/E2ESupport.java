package org.springframework.samples.petclinic.e2e;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;

/**
 * Base dos testes de sistema com Selenium. Os testes precisam da aplicação já rodando
 * (por padrão em localhost:8080) e por isso ficam fora do {@code ./mvnw verify}: as
 * classes terminam em E2E, padrão que o Surefire não executa sozinho.
 * <p>
 * Propriedades opcionais: {@code -De2e.baseUrl=...} muda o endereço e
 * {@code -De2e.visivel=true} abre o Chrome com janela, útil para gravar a apresentação.
 */
abstract class E2ESupport {

	protected static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:8080");

	protected static WebDriver driver;

	protected static WebDriverWait wait;

	@BeforeAll
	static void abrirNavegador() {
		ChromeOptions options = new ChromeOptions();
		if (!Boolean.getBoolean("e2e.visivel")) {
			options.addArguments("--headless=new");
		}
		options.addArguments("--window-size=1366,900");
		driver = new ChromeDriver(options);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		// O idioma fica guardado na sessão, então basta escolher uma vez
		driver.get(BASE_URL + "/?lang=pt");
	}

	@AfterAll
	static void fecharNavegador() {
		if (driver != null) {
			driver.quit();
		}
	}

	/**
	 * Salva um print em target/evidencias, para anexar ao campo "Resultado obtido".
	 */
	protected static void salvarEvidencia(String nome) {
		try {
			Path destino = Path.of("target", "evidencias", nome + ".png");
			Files.createDirectories(destino.getParent());
			Path origem = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
			Files.copy(origem, destino, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Não foi possível salvar a evidência " + nome, ex);
		}
	}

	/**
	 * Preenche um campo de data pelo valor ISO (aaaa-mm-dd). Digitar no campo dependeria
	 * do formato de data do sistema operacional.
	 */
	protected static void preencherData(By campo, LocalDate data) {
		WebElement elemento = driver.findElement(campo);
		((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", elemento, data.toString());
	}

	protected static String textoDaPagina() {
		return driver.findElement(By.tagName("body")).getText();
	}

}
