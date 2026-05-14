import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.*;
import java.util.*;

public class Main {

    private static final int MAX_RUN_MINUTES = 345;

    private static final LocalTime DAILY_STOP_START = LocalTime.of(23, 30);
    private static final LocalTime DAILY_STOP_END = LocalTime.of(1, 0);

    private static final boolean TODAY_OFF = false;

    public static void main(String[] args) {

        if (TODAY_OFF) return;

        String user = System.getenv("GAME_ID");
        String pass = System.getenv("GAME_PASSWORD");

        if (user == null || pass == null) {
            throw new RuntimeException("Secrets missing");
        }

        if (isInShutdownWindow()) return;

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
            sleep(2000);

            driver.findElement(By.name("plogin")).sendKeys(user);
            driver.findElement(By.name("ppass")).sendKeys(pass);
            driver.findElement(By.cssSelector("input[type='submit']")).click();

            sleep(3000);

            driver.findElement(By.cssSelector("a.urfin")).click();
            sleep(3000);

            int idleChecks = 0;

            while (true) {

                if (shouldStopNow(startTime)) break;

                boolean actionDone = false;

                // ================= PASS NOW (<= 20 GOLD) =================
                try {
                    List<WebElement> passNow = driver.findElements(
                            By.xpath("//*[contains(text(),'Pass now for')]")
                    );

                    if (!passNow.isEmpty()) {

                        String text = passNow.get(0).getText();
                        String number = text.replaceAll("[^0-9]", "");

                        if (!number.isEmpty()) {
                            int gold = Integer.parseInt(number);

                            if (gold <= 20) {
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
                                idleChecks = 0; // reset cycle
                            }
                        }
                    }
                } catch (Exception ignored) {}

                // ================= ATTACK NOW =================
                if (!actionDone) {
                    try {
                        List<WebElement> attackNow = driver.findElements(
                                By.xpath("//a[contains(@href,'/urfin/start')]")
                        );

                        if (!attackNow.isEmpty()) {
                            click(driver, attackNow.get(0));
                            sleep(1200);
                            actionDone = true;
                            idleChecks = 0;
                        }
                    } catch (Exception ignored) {}
                }

                // ================= ATTACK LINKS =================
                if (!actionDone) {
                    try {
                        List<WebElement> links = driver.findElements(By.cssSelector("a[href*='attack']"));

                        for (WebElement e : links) {
                            try {
                                driver.get(e.getAttribute("href"));
                                sleep(400);
                            } catch (Exception ignored) {}
                        }

                        actionDone = true;
                        idleChecks++;
                    } catch (Exception ignored) {}
                }

                // ================= NEXT =================
                if (!actionDone) {
                    try {
                        List<WebElement> next = driver.findElements(By.xpath("//span[text()='Next']"));
                        if (!next.isEmpty()) {
                            click(driver, next.get(0));
                            sleep(800);
                            actionDone = true;
                            idleChecks++;
                        }
                    } catch (Exception ignored) {}
                }

                // ================= PASSIVE MODE =================
                if (!actionDone) {

                    idleChecks++;

                    if (idleChecks <= 6) {
                        // check every 10 sec (your requirement)
                        sleep(10000);
                        driver.navigate().refresh();
                    } else {
                        // after 6 idle cycles → 1 min refresh
                        sleep(60000);
                        driver.navigate().refresh();
                        idleChecks = 0;
                    }
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

    // ================= SERVER TIME STOP (FIXED) =================
    public static boolean isInShutdownWindow() {
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        return !now.isBefore(DAILY_STOP_START) || now.isBefore(DAILY_STOP_END);
    }

    public static boolean shouldStopNow(Instant startTime) {
        long mins = Duration.between(startTime, Instant.now()).toMinutes();
        return mins >= MAX_RUN_MINUTES || isInShutdownWindow();
    }

    // ================= SLEEP =================
    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
