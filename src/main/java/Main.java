import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Main {

    private static final int MAX_RUN_MINUTES = 345;
    private static final Random random = new Random();

    public static void main(String[] args) {

        String user = System.getenv("GAME_ID");
        String pass = System.getenv("GAME_PASSWORD");

        if (user == null || pass == null) {
            throw new RuntimeException("Missing credentials");
        }

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

        Instant start = Instant.now();

        try {

            // LOGIN
            safeNavigate(driver, "https://elem.cards/login/");
            sleep(2500);

            safeType(driver, By.name("plogin"), user);
            safeType(driver, By.name("ppass"), pass);

            safeClick(driver, By.cssSelector("input[type='submit']"));
            sleep(3000);

            // ENTER GAME
            safeClick(driver, By.cssSelector("a.urfin"));
            sleep(2500);

            while (!shouldStop(start)) {

                // attack paths
                attackPath(driver, "attack0");
                attackPath(driver, "attack1");
                attackPath(driver, "attack2");

                // normal attack
                safeClick(driver, By.xpath("//span[text()='Attack']"));

                // gold attack (safe handling only if present)
                handleGoldAttack(driver);

                // next boss / page
                safeClick(driver, By.xpath("//span[text()='Next']"));

                // stable delay (no aggressive spam)
                smartWait();

                // refresh safely
                safeRefresh(driver);
            }

        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
        } finally {
            driver.quit();
        }
    }

    // ---------------- SAFE HELPERS ----------------

    static boolean shouldStop(Instant start) {
        return Duration.between(start, Instant.now()).toMinutes() >= MAX_RUN_MINUTES;
    }

    static void safeNavigate(WebDriver driver, String url) {
        try {
            driver.get(url);
        } catch (Exception ignored) {}
    }

    static void safeType(WebDriver driver, By by, String text) {
        try {
            WebElement el = driver.findElement(by);
            el.clear();
            el.sendKeys(text);
        } catch (Exception ignored) {}
    }

    static void safeClick(WebDriver driver, By by) {
        try {
            List<WebElement> el = driver.findElements(by);
            if (!el.isEmpty()) {
                el.get(0).click();
                sleep(800 + random.nextInt(600));
            }
        } catch (Exception ignored) {}
    }

    static void safeRefresh(WebDriver driver) {
        try {
            driver.navigate().refresh();
        } catch (Exception ignored) {}
    }

    static void attackPath(WebDriver driver, String type) {
        try {
            List<WebElement> links = driver.findElements(By.cssSelector("a[href*='" + type + "']"));
            for (WebElement e : links) {
                try {
                    driver.get(e.getAttribute("href"));
                    sleep(600 + random.nextInt(600));
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    static void handleGoldAttack(WebDriver driver) {
        try {
            List<WebElement> gold = driver.findElements(By.xpath("//span[contains(text(),'Attack now for')]"));

            if (!gold.isEmpty()) {
                String text = gold.get(0).getText();
                String num = text.replaceAll("[^0-9]", "");

                if (!num.isEmpty()) {
                    int cost = Integer.parseInt(num);

                    if (cost <= 10) {
                        gold.get(0).click();
                        sleep(800);

                        safeClick(driver, By.xpath("//span[text()='Yes!']"));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    static void smartWait() {
        try {
            Thread.sleep(8000 + random.nextInt(4000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
