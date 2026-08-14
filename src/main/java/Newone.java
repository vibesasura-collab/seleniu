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
import java.util.Random;

public class Newone {

    private static final int MAX_RUN_MINUTES = 345;
    private static final boolean TODAY_OFF = false;

    public static void main(String[] args) {

        if (TODAY_OFF) {
            System.out.println("Bot OFF today. Exiting.");
            return;
        }

        String user = System.getenv("GAME_ID");
        String pass = System.getenv("GAME_PASSWORD");

        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            throw new RuntimeException("GAME_ID or GAME_PASSWORD not found in environment secrets.");
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
        Random random = new Random();

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

            // 1. Login Block
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

            // 2. Navigate to Invasion section (/urfin/)
            boolean navigated = false;
            for (int attempt = 1; attempt <= 5; attempt++) {
                List<WebElement> urfinLinks = driver.findElements(By.cssSelector("a.urfin, a[href*='/urfin/']"));
                if (!urfinLinks.isEmpty()) {
                    urfinLinks.get(0).click();
                    navigated = true;
                    sleep(1000);
                    break;
                }
                sleep(500);
            }

            if (!navigated) {
                System.out.println("Could not find Invasion link. Exiting.");
                return;
            }

            // Main Automation Loop
            while (true) {
                if (shouldStopNow(startTime)) {
                    System.out.println("Stopping now due to runtime limit.");
                    break;
                }

                // 3. Click Initial "Напасть" (Start) button if present
                List<WebElement> startBtns = driver.findElements(By.xpath("//a[contains(@href, '/urfin/start/') and not(contains(@class, 'orange'))]"));
                if (!startBtns.isEmpty()) {
                    startBtns.get(0).click();
                    System.out.println("Clicked initial 'Напасть' button.");
                    sleep(1000);
                }

                // XPath condition: attack0, attack1, or attack2 links excluding hide2 and chide2
                String attackXPath = "//a[(contains(@href, '/urfin/battle/attack0/') or " +
                        "contains(@href, '/urfin/battle/attack1/') or " +
                        "contains(@href, '/urfin/battle/attack2/')) and " +
                        "not(contains(@href, 'hide2')) and not(contains(@href, 'chide2'))]";

                // 4. Attack Loop (Attack 0, 1, 2)
                while (true) {
                    List<WebElement> attacks = driver.findElements(By.xpath(attackXPath));

                    if (!attacks.isEmpty()) {
                        attacks.get(0).click();
                        System.out.println("Clicked attack card.");
                        sleep(1000);
                    } else {
                        // Double-check condition: wait 10 seconds and check a 2nd time before proceeding
                        System.out.println("No attack cards found. Waiting 10 seconds to verify...");
                        sleep(10000);

                        List<WebElement> recheckAttacks = driver.findElements(By.xpath(attackXPath));
                        if (recheckAttacks.isEmpty()) {
                            System.out.println("Confirmed no attack cards remaining.");
                            break; // Exit attack sub-loop
                        }
                    }
                }

                // 5. Click "Далее" (Next) button
                List<WebElement> nextBtns = driver.findElements(By.xpath("//a[contains(@href, '/urfin/') and .//span[contains(text(), 'Далее')]]"));
                if (!nextBtns.isEmpty()) {
                    nextBtns.get(0).click();
                    System.out.println("Clicked 'Далее' (Next).");
                    sleep(1000);
                }

                // 6. Check Gold Cost for Immediate Attack ("Напасть сразу")
                List<WebElement> goldAttackBtns = driver.findElements(By.xpath("//a[contains(@href, '/urfin/start/') and contains(@class, 'orange')]"));

                if (!goldAttackBtns.isEmpty()) {
                    String btnText = goldAttackBtns.get(0).getText();
                    String goldDigits = btnText.replaceAll("[^0-9]", "");
                    int goldCost = goldDigits.isEmpty() ? 0 : Integer.parseInt(goldDigits);

                    System.out.println("Detected gold cost: " + goldCost);

                    if (goldCost <= 20) {
                        // Click gold attack button
                        goldAttackBtns.get(0).click();
                        System.out.println("Clicked 'Напасть сразу' for " + goldCost + " gold.");
                        sleep(1000);

                        // Click confirmation "Да!"
                        List<WebElement> confirmBtns = driver.findElements(By.xpath("//a[contains(@href, '/urfin/start/confirmed/')]"));
                        if (!confirmBtns.isEmpty()) {
                            confirmBtns.get(0).click();
                            System.out.println("Clicked 'Да!' confirmation.");
                            sleep(1000);
                        }

                        // Loop re-enters attack sequence
                        continue;
                    } else {
                        // Gold cost > 20: Sleep for 16 to 20 minutes
                        int sleepMinutes = 16 + random.nextInt(5); // Random between 16 and 20
                        System.out.println("Gold cost (" + goldCost + ") > 20. Sleeping for " + sleepMinutes + " minutes...");
                        sleep(sleepMinutes * 60 * 1000);

                        // Navigate back to invasion page after waking up
                        driver.navigate().to("https://elem.cards/urfin/");
                        sleep(2000);
                    }
                } else {
                    // Fallback refresh if element state isn't updated
                    sleep(2000);
                    driver.navigate().refresh();
                }
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
