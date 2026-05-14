import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.*;
import java.util.*;

public class Main {

    private static final int MAX_RUN_MINUTES = 345;

    // 🔥 CHANGE THIS TIMEZONE IF NEEDED
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private static final LocalTime DAILY_STOP_START = LocalTime.of(23, 30);
    private static final LocalTime DAILY_STOP_END = LocalTime.of(1, 0);

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
            sleep(3000);

            driver.findElement(By.name("plogin")).sendKeys(user);
            driver.findElement(By.name("ppass")).sendKeys(pass);
            driver.findElement(By.cssSelector("input[type='submit']")).click();

            sleep(4000);

            driver.findElement(By.cssSelector("a.urfin")).click();
            sleep(3000);

            while (true) {

                if (shouldStop(start)) break;

                // ================= PASS NOW =================
                for (int i = 0; i < 3; i++) {

                    try {
                        List<WebElement> passNowBtn = driver.findElements(
                                By.xpath("//span[contains(text(),'Pass now for')]")
                        );

                        if (passNowBtn.isEmpty()) break;

                        String text = passNowBtn.get(0).getText().replaceAll("[^0-9]", "");

                        if (!text.isEmpty() && Integer.parseInt(text) <= 10) {

                            safeClick(driver, passNowBtn.get(0));
                            sleep(1200);

                            List<WebElement> yesBtn = driver.findElements(
                                    By.xpath("//span[text()='Yes!']")
                            );

                            if (!yesBtn.isEmpty()) {
                                safeClick(driver, yesBtn.get(0));
                            }

                            sleep(2500);
                        }

                    } catch (Exception ignored) {
                        sleep(1500);
                    }
                }

                // ================= ATTACK LINKS =================
                List<String> links = new ArrayList<>();

                for (String x : new String[]{"attack0", "attack1", "attack2"}) {
                    for (WebElement e : driver.findElements(By.cssSelector("a[href*='" + x + "']"))) {
                        links.add(e.getAttribute("href"));
                    }
                }

                for (String l : links) {
                    try {
                        driver.get(l);
                        sleep(800 + random.nextInt(500));
                    } catch (Exception ignored) {}
                }

                // ================= NEXT =================
                try {
                    List<WebElement> next = driver.findElements(By.xpath("//span[text()='Next']"));
                    if (!next.isEmpty()) {
                        safeClick(driver, next.get(0));
                    }
                } catch (Exception ignored) {}

                sleep(2000);
                driver.navigate().refresh();
            }

        } finally {
            driver.quit();
        }
    }

    // ================= FORCE STOP (FIXED TIMEZONE) =================
    public static boolean isInShutdownWindow() {

        LocalTime now = LocalTime.now(ZONE);

        return (!now.isBefore(DAILY_STOP_START) || now.isBefore(DAILY_STOP_END));
    }

    public static boolean shouldStop(Instant start) {

        long mins = Duration.between(start, Instant.now()).toMinutes();

        return mins >= MAX_RUN_MINUTES || isInShutdownWindow();
    }

    // ================= CLICK =================
    public static void safeClick(WebDriver driver, WebElement el) {
        try {
            el.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", el);
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
