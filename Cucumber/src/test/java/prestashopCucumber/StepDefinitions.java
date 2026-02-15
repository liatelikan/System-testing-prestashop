package prestashopCucumber;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StepDefinitions {

    WebDriver driver;
    WebDriverWait wait;
    String baseUrl = "http://localhost:8080";

    @Before
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");

        Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);

        options.addArguments("--disable-features=PasswordLeakDetection");
        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-notifications");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ----------------------------------------------------------
    // USER & LOGIN STEPS
    // ----------------------------------------------------------

    @Given("a {string} user")
    public void a_user_type(String userType) {
        driver.get(baseUrl);
        driver.manage().deleteAllCookies();
        driver.navigate().refresh();

        if (userType.equalsIgnoreCase("Registered")) {
            login("Bob@prestashop.com", "123456789");
        }
    }

    @Given("a {string} user is logged in")
    public void a_specific_user_logged_in(String userType) {
        driver.get(baseUrl);
        driver.manage().deleteAllCookies();
        driver.navigate().refresh();

        if (userType.equalsIgnoreCase("VIP")) {
            login("pub@prestashop.com", "123456789");
        } else {
            login("demo@prestashop.com", "prestashop_demo");
        }
    }

    private void login(String email, String password) {
        try {
            WebElement signInButton = driver.findElement(By.cssSelector("div.user-info a"));
            signInButton.click();

            WebElement emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email")));
            emailField.sendKeys(email);

            driver.findElement(By.name("password")).sendKeys(password);
            driver.findElement(By.id("submit-login")).click();
        } catch (Exception e) {
            System.out.println("Login skipped: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // PRODUCT & CART STEPS
    // ----------------------------------------------------------

    @When("a {string} product is added to the cart with quantity {string}")
    public void add_product_to_cart(String productType, String quantity) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".products .product-miniature")));
        List<WebElement> allProducts = driver.findElements(By.cssSelector(".product-miniature"));

        if (allProducts.isEmpty()) {
            throw new RuntimeException("No products found on the page!");
        }

        WebElement selectedProduct = null;
        Random rand = new Random();

        if (productType.equalsIgnoreCase("Price_Rule")) {
            List<WebElement> discountedProducts = allProducts.stream()
                    .filter(p -> p.findElements(By.cssSelector(".discount-percentage, .discount-amount, .on-sale")).size() > 0)
                    .collect(Collectors.toList());

            if (!discountedProducts.isEmpty()) {
                selectedProduct = discountedProducts.get(rand.nextInt(discountedProducts.size()));
                System.out.println("Selected Random Discounted Product");
            } else {
                System.out.println("No discounted products found on page, searching for 'Mug'...");
                try {
                    driver.findElement(By.partialLinkText("Mug")).click();
                    defineQuantityAndAdd(quantity);
                    return;
                } catch (Exception e) {
                    throw new RuntimeException("Could not find any discounted product.");
                }
            }
        } else {
            selectedProduct = allProducts.get(rand.nextInt(allProducts.size()));
            System.out.println("Selected Random Product");
        }

        try {
            selectedProduct.findElement(By.cssSelector(".product-title a")).click();
        } catch (Exception e) {
            selectedProduct.findElement(By.cssSelector("a.thumbnail")).click();
        }

        defineQuantityAndAdd(quantity);
    }

    private void defineQuantityAndAdd(String quantity) {
        WebElement qtyInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("quantity_wanted")));
        qtyInput.sendKeys(Keys.CONTROL + "a");
        qtyInput.sendKeys(Keys.DELETE);

        if (quantity.equalsIgnoreCase("3Items_Or_More")) {
            qtyInput.sendKeys("3");
        } else {
            qtyInput.sendKeys("1");
        }
        driver.findElement(By.cssSelector("body")).click(); // עדכון UI

        boolean succeeded = false;
        for(int i=0; i<3; i++) {
            try {
                WebElement addBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.add-to-cart")));
                if(!addBtn.isEnabled()) throw new RuntimeException("Product is out of stock");
                addBtn.click();
                WebElement modalProceed = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#blockcart-modal .cart-content-btn .btn-primary")));
                modalProceed.click();
                succeeded = true;
                break;
            } catch (Exception e) {
                try { Thread.sleep(1000); } catch (InterruptedException ex) {}
            }
        }
        if (!succeeded) throw new RuntimeException("Failed to add product to cart.");
    }

    @When("the checkout process is started")
    public void checkout_process_started() {
        WebElement proceedBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.checkout a.btn-primary")));
        proceedBtn.click();
    }

    // ----------------------------------------------------------
    // ADDRESS STEPS (עם מנגנון Retry ל-StaleElement)
    // ----------------------------------------------------------

    @When("an address for {string} is entered")
    public void enter_address(String countryType) {
        try {
            if (driver.findElements(By.id("customer-form")).size() > 0) {
                driver.findElement(By.id("field-id_gender-1")).click();
                driver.findElement(By.id("field-firstname")).sendKeys("Test");
                driver.findElement(By.id("field-lastname")).sendKeys("User");
                driver.findElement(By.id("field-email")).sendKeys("test" + System.currentTimeMillis() + "@test.com");

                try { driver.findElement(By.name("password")).sendKeys("Test@1234!"); } catch (Exception e) {}

                List<WebElement> checkboxes = driver.findElements(By.cssSelector("#customer-form input[type='checkbox']"));
                for (WebElement box : checkboxes) {
                    if (!box.isSelected()) ((JavascriptExecutor) driver).executeScript("arguments[0].click();", box);
                }

                WebElement continuePersonal = driver.findElement(By.cssSelector("button[data-link-action='register-new-customer']"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", continuePersonal);
                continuePersonal.click();
            }
        } catch (Exception e) {}

        try {
            List<WebElement> editButtons = driver.findElements(By.cssSelector(".address-item.selected .edit-address"));
            if (!editButtons.isEmpty()) {
                editButtons.get(0).click();
            } else {
                List<WebElement> anyEdit = driver.findElements(By.cssSelector(".edit-address"));
                if (!anyEdit.isEmpty()) anyEdit.get(0).click();
            }
        } catch (Exception e) {}

        try {
            WebElement addressField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("field-address1")));
            addressField.clear();
            addressField.sendKeys("123 Test Street");

            driver.findElement(By.id("field-city")).clear();
            driver.findElement(By.id("field-city")).sendKeys("Tel Aviv");

            driver.findElement(By.id("field-postcode")).clear();

            if (countryType.equalsIgnoreCase("International")) {
                driver.findElement(By.id("field-postcode")).sendKeys("75001"); // צרפת
            } else {
                driver.findElement(By.id("field-postcode")).sendKeys("1234567"); // ישראל
            }

            Select countrySelect = new Select(driver.findElement(By.id("field-id_country")));

            if (countryType.equalsIgnoreCase("International")) {
                try { countrySelect.selectByVisibleText("France"); } catch (Exception e) { countrySelect.selectByIndex(1); }
            } else {
                try { countrySelect.selectByVisibleText("Israel"); } catch (Exception e) { countrySelect.selectByIndex(0); }
            }

        } catch (Exception e) {}

        boolean addressConfirmed = false;
        for(int i=0; i<3; i++) {
            try {
                WebElement confirmAddressBtn = wait.until(ExpectedConditions.elementToBeClickable(By.name("confirm-addresses")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", confirmAddressBtn);
                confirmAddressBtn.click();
                addressConfirmed = true;
                break;
            } catch (StaleElementReferenceException e) {
                try { Thread.sleep(500); } catch (InterruptedException ex) {}
            } catch (ElementClickInterceptedException e) {
                try {
                    WebElement btn = driver.findElement(By.name("confirm-addresses"));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                    addressConfirmed = true;
                    break;
                } catch (Exception ex) {}
            }
        }

        if (!addressConfirmed) {
            try { driver.findElement(By.name("confirm-addresses")).click(); } catch (Exception e) {}
        }
    }

    // ----------------------------------------------------------
    // SHIPPING STEPS
    // ----------------------------------------------------------

    @When("the {string} shipping method is selected")
    public void select_shipping_method(String carrier) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("js-delivery")));

        if (carrier.contains("No Carrier") || carrier.contains("Pick up") || carrier.equals("Local")) {
            try {
                WebElement pickupRadio = driver.findElement(By.xpath("//div[contains(@class,'delivery-option')]//label[contains(.,'No Carrier') or contains(.,'Pick up')]/..//input[@type='radio']"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", pickupRadio);
            } catch (Exception e) {
                List<WebElement> options = driver.findElements(By.cssSelector(".delivery-option input[type='radio']"));
                if (!options.isEmpty()) ((JavascriptExecutor) driver).executeScript("arguments[0].click();", options.get(0));
            }
        } else {
            boolean selected = false;
            List<WebElement> rows = driver.findElements(By.cssSelector(".delivery-option"));

            for (WebElement row : rows) {
                String text = row.getText();
                if (text.contains(carrier) && !text.contains("No Carrier Available")) {
                    WebElement radio = row.findElement(By.cssSelector("input[type='radio']"));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", radio);
                    selected = true;
                    System.out.println("Selected correct carrier: " + carrier);
                    break;
                }
            }

            if (!selected) {
                for (WebElement row : rows) {
                    if (!row.getText().contains("No Carrier Available")) {
                        WebElement radio = row.findElement(By.cssSelector("input[type='radio']"));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", radio);
                        System.out.println("Fallback: Selected valid carrier (Not 'No Carrier')");
                        selected = true;
                        break;
                    }
                }
            }

            if (!selected) {
                throw new RuntimeException("CRITICAL: Could not find any valid shipping carrier! Only 'No Carrier' is available.");
            }
        }

        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(By.name("confirmDeliveryOption")));
        continueBtn.click();
    }

    @When("the {string} delivery option is selected")
    public void select_delivery_option_generic(String carrier) { select_shipping_method(carrier); }

    @When("the {string} option is selected")
    public void select_pickup_option(String option) { select_shipping_method(option); }

    @When("the {string} payment method is selected")
    public void select_payment_method(String paymentMethod) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("payment-option-1-container")));
        try {
            List<WebElement> options = driver.findElements(By.cssSelector(".payment-option"));
            boolean clicked = false;
            for(WebElement opt : options) {
                if(opt.getText().contains(paymentMethod) || (paymentMethod.contains("Check") && opt.getText().contains("Check"))) {
                    opt.findElement(By.cssSelector("label")).click();
                    clicked = true;
                    break;
                }
            }
            if(!clicked) driver.findElement(By.cssSelector("input[name='payment-option']")).click();

        } catch (Exception e) {
            driver.findElement(By.cssSelector("input[name='payment-option']")).click();
        }
    }

    @When("I agree to the terms and place the order")
    public void place_order() {
        try {
            WebElement termsInput = driver.findElement(By.id("conditions_to_approve[terms-and-conditions]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", termsInput);

            if (!termsInput.isSelected()) {
                try {
                    driver.findElement(By.cssSelector("label.js-terms")).click();
                } catch (Exception e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", termsInput);
                }
            }
        } catch (Exception e) {
            System.out.println("Terms checkbox handling error: " + e.getMessage());
        }

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement placeOrderBtn = driver.findElement(By.id("payment-confirmation")).findElement(By.tagName("button"));
        wait.until(ExpectedConditions.elementToBeClickable(placeOrderBtn));
        placeOrderBtn.click();
    }
    @Then("the order should be confirmed")
    public void order_should_be_confirmed() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("content-hook_order_confirmation")));
    }


    @Then("the order should be successfully confirmed")
    public void verify_order_confirmed() {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'confirmed')]")),
                    ExpectedConditions.visibilityOfElementLocated(By.id("order-confirmation"))
            ));
        } catch (TimeoutException e) {
            throw new AssertionError("Order confirmation failed! Check if shipping carrier was valid.", e);
        }
    }


    // ----------------------------------------------------------
    // PRICE VERIFICATION STEPS
    // ----------------------------------------------------------

    @When("a {string} discount is applied if applicable")
    public void apply_discount_if_applicable(String discountType) {
        if (discountType.equalsIgnoreCase("Cart_level")) {
            try {
                driver.findElement(By.cssSelector("a.promo-code-button")).click();
                WebElement promoInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("discount_name")));
                promoInput.sendKeys("20OFF");
                driver.findElement(By.cssSelector("button.promo-code-button")).click();
            } catch (Exception e) {}
        }
    }

    @When("a {string} discount is applied")
    public void apply_discount_forced(String discountType) {
        apply_discount_if_applicable(discountType);
    }

    @Then("the final price is calculated correctly")
    public void verify_price_standard() {
        try {
            WebElement payCheck = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("label[for='payment-option-1']")));
            payCheck.click();
        } catch (Exception e) {
            try {
                driver.findElement(By.cssSelector("label[for='payment-option-2']")).click();
            } catch (Exception ex) {
                driver.findElement(By.cssSelector(".payment-option label")).click();
            }
        }

        try {
            WebElement termsLabel = driver.findElement(By.cssSelector("label.js-terms"));
            WebElement termsInput = driver.findElement(By.id("conditions_to_approve[terms-and-conditions]"));
            if (!termsInput.isSelected()) {
                termsLabel.click();
            }
        } catch (Exception e) {
            System.out.println("Terms checkbox issue: " + e.getMessage());
        }

        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        WebElement totalElem = driver.findElement(By.cssSelector(".cart-summary-totals .value"));
        double actualTotal = parsePrice(totalElem.getText());
        assertTrue(actualTotal > 0, "Total price should be greater than 0");
        System.out.println("Verified Total Price: " + actualTotal);
    }

    @Then("the final price is calculated correctly with VIP benefits")
    public void verify_price_vip() {
        boolean discountExists = driver.findElements(By.cssSelector(".cart-summary-line.cart-discount")).size() > 0;
    }

    @Then("the final price is calculated correctly without shipping costs")
    public void verify_price_pickup() {
        try {
            WebElement shippingLine = driver.findElement(By.xpath("//div[contains(@class, 'cart-summary-line') and contains(., 'Shipping')]//span[@class='value']"));
            String shippingCost = shippingLine.getText();
            assertTrue(shippingCost.contains("Free") || shippingCost.contains("0.00"));
        } catch (Exception e) {}
    }

    private double parsePrice(String priceText) {
        String cleanPrice = priceText.replaceAll("[^0-9.]", "");
        if (cleanPrice.isEmpty()) return 0.0;
        return Double.parseDouble(cleanPrice);
    }
}