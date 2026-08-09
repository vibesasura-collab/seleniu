import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Che {

    private static final int MAX_RUN_MINUTES = 345;
    private static final boolean TODAY_OFF = false;
    private static final int LOOP_INTERVAL_MS = 1000;

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
        Instant startTime = Instant.now();

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofMillis(300));

            // Login Phase
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

            // Step 1: Check for main Chest shop banner/link
            List<WebElement> chestShopLinks = driver.findElements(By.xpath("//a[contains(@href, '/chests/')]"));
            if (!chestShopLinks.isEmpty()) {
                System.out.println("Chest shop link found. Navigating to Chests.");
                chestShopLinks.get(0).click();
                sleep(1000);
            }

            // Step 2: Chest action loop (Start -> Unlock -> Next)
            int idleCycles = 0;
            while (true) {
                if (shouldStopNow(startTime)) {
                    System.out.println("Stopping now due to runtime limit.");
                    break;
                }

                boolean actionTaken = false;

                // Priority A: Check for "Start / Let's go!" link
                List<WebElement> startBtns = driver.findElements(By.xpath("//a[contains(@href, '/chests/start/')]"));
                if (!startBtns.isEmpty()) {
                    System.out.println("Clicked 'Let's go!' button.");
                    startBtns.get(0).click();
                    actionTaken = true;
                    idleCycles = 0;
                    sleep(500);
                }

                // Priority B: Check for "Unlock" link
                if (!actionTaken) {
                    List<WebElement> unlockBtns = driver.findElements(By.xpath("//a[contains(@href, '/chests/open/')]"));
                    if (!unlockBtns.isEmpty()) {
                        System.out.println("Clicked 'Unlock' button.");
                        unlockBtns.get(0).click();
                        actionTaken = true;
                        idleCycles = 0;
                        sleep(500);
                    }
                }

                // Priority C: Check for "Next" link
                if (!actionTaken) {
                    List<WebElement> nextBtns = driver.findElements(By.xpath("//a[contains(@href, '/chests/next/')]"));
                    if (!nextBtns.isEmpty()) {
                        System.out.println("Clicked 'Next' button.");
                        nextBtns.get(0).click();
                        actionTaken = true;
                        idleCycles = 0;
                        sleep(500);
                    }
                }

                // Priority D: Re-check main chest entry if bounced back
                if (!actionTaken) {
                    List<WebElement> reCheckShop = driver.findElements(By.xpath("//a[contains(@href, '/chests/')]"));
                    if (!reCheckShop.isEmpty()) {
                        System.out.println("Re-entering Chest shop.");
                        reCheckShop.get(0).click();
                        actionTaken = true;
                        idleCycles = 0;
                        sleep(500);
                    }
                }

                // Exit condition: No clickable chest elements found after consecutive checks
                if (!actionTaken) {
                    idleCycles++;
                    if (idleCycles >= 3) {
                        System.out.println("No more chest actions available. Exiting process.");
                        break;
                    }
                }

                sleep(LOOP_INTERVAL_MS);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    public static boolean shouldStopNow(Instant startTime) {
        return Duration.between(startTime, Instant.now()).toMinutes() >= MAX_RUN_MINUTES;
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
