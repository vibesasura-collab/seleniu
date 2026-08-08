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

public class Newone {

    private static final int MAX_RUN_MINUTES = 345;
    private static final boolean TODAY_OFF = false;
    private static final int LOOP_INTERVAL_MS = 2000; // 2-second poll rate

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

        // Speed Optimization 1: Fast Page Load Strategy (don't wait for full image/asset loads)
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        // Speed Optimization 2: Disable image loading to save network bandwidth and DOM rendering time
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        Instant startTime = Instant.now();

        try {
            // Speed Optimization 3: Minimal implicit wait so missing elements return immediately
            driver.manage().timeouts().implicitlyWait(Duration.ofMillis(300));

            driver.get("https://elem.cards/login/");

            // Login Block
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

            // Navigate to Invasion section
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

            // Fast Execution Loop - Executes every 2 seconds
            while (true) {
                long loopStart = System.currentTimeMillis();

                if (shouldStopNow(startTime)) {
                    System.out.println("Stopping now due to runtime limit.");
                    break;
                }

                // -------- 1. Check Pass Now Gold Cost (<= 40) --------
                List<WebElement> passNowBtns = driver.findElements(By.xpath("//a[contains(@href, '/urfin/auto/')]"));
                if (!passNowBtns.isEmpty()) {
                    try {
                        WebElement passBtn = passNowBtns.get(0);
                        String text = passBtn.getText();
                        String number = text.replaceAll("[^0-9]", "");

                        if (!number.isEmpty()) {
                            int cost = Integer.parseInt(number);

                            if (cost <= 30) {
                                passBtn.click();
                                System.out.println("Clicked 'Pass now for " + cost + "'");
                                sleep(300);

                                // Fast Confirm 'Yes!'
                                List<WebElement> yesBtns = driver.findElements(By.xpath("//a[contains(@href, 'confirmed')] | //span[text()='Yes!']"));
                                if (!yesBtns.isEmpty()) {
                                    yesBtns.get(0).click();
                                    System.out.println("Clicked 'Yes!' confirmation");
                                    sleep(300);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error processing Pass now: " + e.getMessage());
                    }
                }

                // -------- 2. Check Next Button --------
                List<WebElement> nextBtns = driver.findElements(By.xpath("//a[contains(@href, '/urfin/')]//span[text()='Next'] | //span[text()='Next']"));
                if (!nextBtns.isEmpty()) {
                    try {
                        nextBtns.get(0).click();
                        System.out.println("Clicked 'Next' button.");
                        sleep(300);
                    } catch (Exception e) {
                        System.out.println("Error clicking Next button: " + e.getMessage());
                    }
                }

                // Refresh page to load updated state instantly
                driver.navigate().refresh();

                // Maintain strictly 2-second cycle rate
                long elapsed = System.currentTimeMillis() - loopStart;
                long sleepTime = LOOP_INTERVAL_MS - elapsed;
                if (sleepTime > 0) {
                    sleep((int) sleepTime);
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
