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

            // GO TO INVASION
            driver.findElement(By.cssSelector("a.urfin")).click();
            sleep(3000 + random.nextInt(2000));

            while (true) {

                long loopStart = System.currentTimeMillis();

                if (shouldStopNow(startTime)) break;

                // ---------------- FIRST 3 PASS NOW FAST ----------------

                for (int i = 0; i < 3; i++) {

                    try {

                        List<WebElement> passNow = driver.findElements(
                                By.xpath("//span[contains(text(),'Pass now for')]")
                        );

                        if (passNow.isEmpty()) {
                            break;
                        }

                        String text = passNow.get(0).getText();
                        String number = text.replaceAll("[^0-9]", "");

                        if (!number.isEmpty() && Integer.parseInt(number) <= 10) {

                            // CLICK PASS NOW
                            click(driver, passNow.get(0));

                            sleep(900);

                            // CLICK YES
                            List<WebElement> yesBtn = driver.findElements(
                                    By.xpath("//span[text()='Yes!']")
                            );

                            if (!yesBtn.isEmpty()) {

                                click(driver, yesBtn.get(0));

                                System.out.println("Fast pass used: " + (i + 1));

                                sleep(900);
                            }
                        }

                    } catch (Exception ignored) {}
                }

                // ---------------- NORMAL OLD ATTACK CODE ----------------

                List<String> attackLinks = new ArrayList<>();

                List<WebElement> attack0 = driver.findElements(By.cssSelector("a[href*='attack0']"));
                List<WebElement> attack1 = driver.findElements(By.cssSelector("a[href*='attack1']"));
                List<WebElement> attack2 = driver.findElements(By.cssSelector("a[href*='attack2']"));

                for (WebElement e : attack0) attackLinks.add(e.getAttribute("href"));
                for (WebElement e : attack1) attackLinks.add(e.getAttribute("href"));
                for (WebElement e : attack2) attackLinks.add(e.getAttribute("href"));

                for (String link : attackLinks) {

                    try {

                        driver.get(link);

                        sleep(600 + random.nextInt(800));

                    } catch (Exception ignored) {}
                }

                // ATTACK BUTTON

                List<WebElement> attackBtn = driver.findElements(
                        By.xpath("//span[text()='Attack']")
                );

                if (!attackBtn.isEmpty()) {

                    try {

                        click(driver, attackBtn.get(0));

                        sleep(1000 + random.nextInt(1000));

                    } catch (Exception ignored) {}
                }

                // GOLD ATTACK

                List<WebElement> goldAttack = driver.findElements(
                        By.xpath("//span[contains(text(),'Attack now for')]")
                );

                if (!goldAttack.isEmpty()) {

                    try {

                        String text = goldAttack.get(0).getText();
                        String number = text.replaceAll("[^0-9]", "");

                        if (!number.isEmpty() && Integer.parseInt(number) <= 10) {

                            click(driver, goldAttack.get(0));

                            sleep(800 + random.nextInt(800));

                            List<WebElement> yes = driver.findElements(
                                    By.xpath("//span[text()='Yes!']")
                            );

                            if (!yes.isEmpty()) {

                                click(driver, yes.get(0));
                            }
                        }

                    } catch (Exception ignored) {}
                }

                // NEXT BUTTON

                List<WebElement> nextBtn = driver.findElements(
                        By.xpath("//span[text()='Next']")
                );

                if (!nextBtn.isEmpty()) {

                    try {

                        click(driver, nextBtn.get(0));

                        sleep(1000 + random.nextInt(1000));

                    } catch (Exception ignored) {}
                }

                if (shouldStopNow(startTime)) break;

                long elapsed = System.currentTimeMillis() - loopStart;

                long wait = 9000 + random.nextInt(4000) - elapsed;

                if (wait > 0) {
                    sleep((int) wait);
                }

                driver.navigate().refresh();
            }

        } finally {

            driver.quit();
        }
    }

    // ---------------- CLICK ----------------

    public static void click(WebDriver driver, WebElement element) {

        try {

            element.click();

        } catch (Exception e) {

            try {

                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].click();", element);

            } catch (Exception ignored) {}
        }
    }

    // ---------------- STOP CONDITIONS ----------------

    public static boolean shouldStopNow(Instant startTime) {

        long mins = Duration.between(startTime, Instant.now()).toMinutes();

        return mins >= MAX_RUN_MINUTES || isInShutdownWindow();
    }

    public static boolean isInShutdownWindow() {

        LocalTime now = LocalTime.now(ZoneOffset.UTC);

        return !now.isBefore(DAILY_STOP_START)
                || now.isBefore(DAILY_STOP_END);
    }

    // ---------------- SLEEP ----------------

    public static void sleep(int ms) {

        try {

            Thread.sleep(ms);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }
}
