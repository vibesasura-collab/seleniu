import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class E {

    private static final boolean TODAY_OFF = false;

    public static void main(String[] args) {

        if (TODAY_OFF) {
            System.out.println("Bot OFF today. Exiting.");
            return;
        }

        String user = System.getenv("GAME_ID");
        String pass = System.getenv("GAME_PASSWORD");

        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            throw new RuntimeException("GAME_ID or GAME_PASSWORD not found in GitHub Secrets.");
        }

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

            // Login Block
            driver.get("https://elem.cards/login/");

            List<WebElement> userInputs = driver.findElements(By.name("plogin"));
            List<WebElement> passInputs = driver.findElements(By.name("ppass"));
            List<WebElement> submitBtns = driver.findElements(By.cssSelector("input[type='submit']"));

            if (!userInputs.isEmpty() && !passInputs.isEmpty() && !submitBtns.isEmpty()) {
                userInputs.get(0).sendKeys(user);
                passInputs.get(0).sendKeys(pass);
                submitBtns.get(0).click();
                sleep(1000);
            } else {
                System.out.println("Could not find login fields. Exiting.");
                return;
            }

            // Step 1: Click Store link
            List<WebElement> storeBtns = driver.findElements(By.xpath("//a[@href='/shop/']"));
            if (!storeBtns.isEmpty()) {
                storeBtns.get(0).click();
                System.out.println("Clicked Store link.");
                sleep(1000);
            } else {
                System.out.println("Store link not present.");
            }

            // Step 2: Click Buffs section link
            List<WebElement> buffsBtns = driver.findElements(By.xpath("//a[@href='/shop/buffs/']"));
            if (!buffsBtns.isEmpty()) {
                buffsBtns.get(0).click();
                System.out.println("Clicked Buffs section link.");
                sleep(1000);
            } else {
                System.out.println("Buffs section link not present.");
            }

            // Step 3: Check and click EXP buff purchase link if present
            List<WebElement> expBuffBtns = driver.findElements(By.xpath("//a[contains(@href, '/shop/buffs/buy/exp100/')]"));
            if (!expBuffBtns.isEmpty()) {
                expBuffBtns.get(0).click();
                System.out.println("Clicked EXP buff purchase link.");
                sleep(1000);
            } else {
                System.out.println("EXP buff purchase link not present.");
            }

            // Step 4: Check and click Silver buff purchase link if present
            List<WebElement> silverBuffBtns = driver.findElements(By.xpath("//a[contains(@href, '/shop/buffs/buy/silver100/')]"));
            if (!silverBuffBtns.isEmpty()) {
                silverBuffBtns.get(0).click();
                System.out.println("Clicked Silver buff purchase link.");
                sleep(1000);
            } else {
                System.out.println("Silver buff purchase link not present.");
            }

            System.out.println("Completed checks. Exiting process.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
