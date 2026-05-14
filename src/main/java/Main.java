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
import java.util.Random;

public class Main {

    private static final int MAX_RUN_MINUTES = 345;
    private static final LocalTime DAILY_STOP_START = LocalTime.of(23, 30);
    private static final LocalTime DAILY_STOP_END = LocalTime.of(1, 0);

    private static final boolean TODAY_OFF = false;

    public static void main(String[] args) {

        if (TODAY_OFF) {
            System.out.println("Bot OFF today. Exiting.");
            return;
        }

        String user = System.getenv("GAME_ID");
        String pass = System.getenv("GAME_PASSWORD");

        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            throw new RuntimeException("Secrets missing");
        }

        if (isInShutdownWindow()) {
            System.out.println("Shutdown window active. Exiting.");
            return;
        }

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        Random random = new Random();
        Instant startTime = Instant.now();

        try {

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

            // LOGIN
            driver.get("https://elem.cards/login/");
            sleep(2000 + random.nextInt(1000));

            driver.findElement(By.name("plogin")).sendKeys(user);
            driver.findElement(By.name("ppass")).sendKeys(pass);
            driver.findElement(By.cssSelector("input[type='submit']")).click();

            sleep(3000 + random.nextInt(2000));

            driver.findElement(By.cssSelector("a.urfin")).click();
            sleep(3000 + random.nextInt(2000));

            while (true) {

                if (shouldStopNow(startTime)) break;

                boolean actionDone = false;

                // ================= PASS NOW (PRIORITY 1) =================
                List<WebElement> passNow = driver.findElements(
                        By.xpath("//*[contains(text(),'Pass now for')]")
                );

                if (!passNow.isEmpty()) {

                    String text = passNow.get(0).getText();
                    String number = text.replaceAll("[^0-9]", "");

                    if (!number.isEmpty() && Integer.parseInt(number) <= 10) {

                        click(driver, passNow.get(0));
                        sleep(800);

                        List<WebElement> yes = driver.findElements(
                                By.xpath("//span[text()='Yes!']")
                        );

                        if (!yes.isEmpty()) {
                            click(driver, yes.get(0));
                        }

                        sleep(1200);
                        actionDone = true;
                    }
                }

                // ================= ATTACK NOW BUTTON =================
                if (!actionDone) {

                    List<WebElement> attackNow = driver.findElements(
                            By.xpath("//a[contains(@href,'/urfin/start')]")
                    );

                    if (!attackNow.isEmpty()) {
                        click(driver, attackNow.get(0));
                        sleep(1200);
                        actionDone = true;
                    }
                }

                // ================= ATTACK LINKS (attack0/1/2) =================
                if (!actionDone) {

                    List<String> links = new ArrayList<>();

                    List<WebElement> attack0 = driver.findElements(By.cssSelector("a[href*='attack0']"));
                    List<WebElement> attack1 = driver.findElements(By.cssSelector("a[href*='attack1']"));
                    List<WebElement> attack2 = driver.findElements(By.cssSelector("a[href*='attack2']"));

                    for (WebElement e : attack0) links.add(e.getAttribute("href"));
                    for (WebElement e : attack1) links.add(e.getAttribute("href"));
                    for (WebElement e : attack2) links.add(e.getAttribute("href"));

                    for (String link : links) {
                        try {
                            driver.get(link);
                            sleep(500);
                        } catch (Exception ignored) {}
                    }

                    actionDone = true;
                }

                // ================= NEXT BUTTON =================
                if (!actionDone) {

                    List<WebElement> nextBtn = driver.findElements(
                            By.xpath("//span[text()='Next']")
                    );

                    if (!nextBtn.isEmpty()) {
                        click(driver, nextBtn.get(0));
                        sleep(800);
                        actionDone = true;
                    }
                }

                // ================= NOTHING FOUND =================
                if (!actionDone) {
                    driver.get(driver.getCurrentUrl()); // safer than refresh
                    sleep(1200);
                }
            }

        } finally {
            driver.quit();
        }
    }

    // ================= CLICK SAFE =================
    public static void click(WebDriver driver, WebElement element) {
        try {
            element.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", element);
        }
    }

    // ================= STOP CONDITIONS =================
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
