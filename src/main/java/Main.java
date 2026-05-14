import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.*;
import java.util.*;

public class Main {

    private static final int MAX_RUN_MINUTES = 345;

    public static void main(String[] args) {

        String user = System.getenv("GAME_ID");
        String pass = System.getenv("GAME_PASSWORD");

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        Random random = new Random();
        Instant start = Instant.now();

        try {
            driver.get("https://elem.cards/login/");

            driver.findElement(By.name("plogin")).sendKeys(user);
            driver.findElement(By.name("ppass")).sendKeys(pass);
            driver.findElement(By.cssSelector("input[type='submit']")).click();

            sleep(4000);

            driver.findElement(By.cssSelector("a.urfin")).click();
            sleep(3000);

            while (true) {

                if (Duration.between(start, Instant.now()).toMinutes() >= MAX_RUN_MINUTES)
                    break;

                // ================= PASS NOW (STABLE) =================
                for (int i = 0; i < 3; i++) {

                    try {
                        List<WebElement> pass = driver.findElements(
                                By.xpath("//a[contains(@href,'/urfin/auto') or contains(.,'Pass now')]")
                        );

                        if (pass.isEmpty()) break;

                        String text = pass.get(0).getText().replaceAll("[^0-9]", "");

                        if (!text.isEmpty() && Integer.parseInt(text) <= 10) {

                            safeClick(driver, pass.get(0));
                            sleep(1200);

                            List<WebElement> yes = driver.findElements(
                                    By.xpath("//span[text()='Yes!']")
                            );

                            if (!yes.isEmpty()) {
                                safeClick(driver, yes.get(0));
                            }

                            sleep(2500); // stability gap
                        }

                    } catch (Exception e) {
                        sleep(1500);
                    }
                }

                // ================= ATTACK NOW FIX (IMPORTANT PART) =================
                try {
                    List<WebElement> attackNow = driver.findElements(
                            By.cssSelector("a[href='/urfin/start/']")
                    );

                    if (!attackNow.isEmpty()) {
                        safeClick(driver, attackNow.get(0));
                        sleep(1500);
                    }
                } catch (Exception ignored) {}

                // ================= NORMAL ATTACK LINKS =================
                List<String> links = new ArrayList<>();

                for (String css : new String[]{"attack0", "attack1", "attack2"}) {
                    for (WebElement e : driver.findElements(By.cssSelector("a[href*='" + css + "']"))) {
                        links.add(e.getAttribute("href"));
                    }
                }

                for (String l : links) {
                    try {
                        driver.get(l);
                        sleep(800);
                    } catch (Exception ignored) {}
                }

                // NEXT
                try {
                    List<WebElement> next = driver.findElements(By.xpath("//span[text()='Next']"));
                    if (!next.isEmpty()) {
                        safeClick(driver, next.get(0));
                    }
                } catch (Exception ignored) {}

                sleep(2000 + random.nextInt(2000));
                driver.navigate().refresh();
            }

        } finally {
            driver.quit();
        }
    }

    // ================= SAFE CLICK =================
    public static void safeClick(WebDriver driver, WebElement el) {
        try {
            el.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", el);
        }
    }

    public static void sleep(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
