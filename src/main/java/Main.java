import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final int MAX_RUN_MINUTES = 345;
    private static final LocalTime DAILY_STOP_START = LocalTime.of(23, 30);
    private static final LocalTime DAILY_STOP_END = LocalTime.of(1, 0);

    private static final int GOLD_LIMIT = 20;

    public static void main(String[] args) {

        String user = System.getenv("GAME_ID");
        String pass = System.getenv("GAME_PASSWORD");

        if (user == null || pass == null) {
            throw new RuntimeException("Secrets missing");
        }

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        Instant startTime = Instant.now();

        try {

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

            // LOGIN
            driver.get("https://elem.cards/login/");
            sleep(2500);

            driver.findElement(By.name("plogin")).sendKeys(user);
            driver.findElement(By.name("ppass")).sendKeys(pass);
            driver.findElement(By.cssSelector("input[type='submit']")).click();

            sleep(3500);
            driver.findElement(By.cssSelector("a.urfin")).click();
            sleep(3000);

            while (true) {

                if (shouldStopNow(startTime)) break;

                boolean actionDone = false;

                // ================= PASS NOW =================
                List<WebElement> passNow = driver.findElements(
                        By.xpath("//*[contains(text(),'Pass now for')]")
                );

                if (!passNow.isEmpty()) {

                    String text = passNow.get(0).getText();
                    String number = text.replaceAll("[^0-9]", "");

                    if (!number.isEmpty() && Integer.parseInt(number) <= GOLD_LIMIT) {

                        click(driver, passNow.get(0));
                        sleep(800);

                        List<WebElement> yes = driver.findElements(
                                By.xpath("//span[text()='Yes!']")
                        );

                        if (!yes.isEmpty()) click(driver, yes.get(0));

                        sleep(1200);
                        actionDone = true;
                    }
                }

                // ================= WAIT LOGIC (10s × 6 + 1min refresh) =================
                if (!actionDone) {

                    boolean found = false;

                    for (int i = 0; i < 6; i++) {

                        sleep(10000);
                        driver.navigate().refresh();

                        passNow = driver.findElements(
                                By.xpath("//*[contains(text(),'Pass now for')]")
                        );

                        if (!passNow.isEmpty()) {

                            String text = passNow.get(0).getText();
                            String number = text.replaceAll("[^0-9]", "");

                            if (!number.isEmpty() && Integer.parseInt(number) <= GOLD_LIMIT) {

                                click(driver, passNow.get(0));
                                sleep(800);

                                List<WebElement> yes = driver.findElements(
                                        By.xpath("//span[text()='Yes!']")
                                );

                                if (!yes.isEmpty()) click(driver, yes.get(0));

                                found = true;
                                actionDone = true;
                                break;
                            }
                        }
                    }

                    if (!found) {
                        sleep(60000);
                        driver.navigate().refresh();
                    }
                }

                // ================= ATTACK FLOW =================
                try {

                    List<String> links = new ArrayList<>();

                    for (WebElement e : driver.findElements(By.cssSelector("a[href*='attack0']")))
                        links.add(e.getAttribute("href"));

                    for (WebElement e : driver.findElements(By.cssSelector("a[href*='attack1']")))
                        links.add(e.getAttribute("href"));

                    for (WebElement e : driver.findElements(By.cssSelector("a[href*='attack2']")))
                        links.add(e.getAttribute("href"));

                    for (String link : links) {
                        driver.get(link);
                        sleep(500);
                    }

                } catch (Exception ignored) {}

                // ================= NEXT BUTTON (IMPORTANT FIX) =================
                try {
                    List<WebElement> nextBtn = driver.findElements(
                            By.xpath("//a[contains(@href,'/urfin/next/')]")
                    );

                    if (!nextBtn.isEmpty()) {
                        click(driver, nextBtn.get(0));
                        sleep(800);
                    }
                } catch (Exception ignored) {}

            }

        } finally {
            driver.quit();
        }
    }

    // CLICK SAFE
    public static void click(WebDriver driver, WebElement element) {
        try {
            element.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", element);
        }
    }

    public static boolean shouldStopNow(Instant startTime) {
        long mins = Duration.between(startTime, Instant.now()).toMinutes();
        return mins >= MAX_RUN_MINUTES || isInShutdownWindow();
    }

    public static boolean isInShutdownWindow() {
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        return !now.isBefore(DAILY_STOP_START)
                || now.isBefore(DAILY_STOP_END);
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
