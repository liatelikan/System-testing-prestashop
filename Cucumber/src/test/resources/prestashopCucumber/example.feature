Feature: Checkout Price Verification
  As a user of the PrestaShop website
  I want to checkout with various product and shipping combinations
  So that I can verify the final total price is calculated correctly

  # Group A: Guests and Registered Users using Standard/Express Shipping
  # Covers Tests: 1, 2, 4, 5, 10
  Scenario Outline: Non-VIP checkout with shipping
    Given a "<Customer_Type>" user
    When a "<Product_Variant>" product is added to the cart with quantity "<Quantity>"
    And the checkout process is started
    And an address for "<Country>" is entered
    And the "<Carrier>" shipping method is selected
    And a "<Discount_Type>" discount is applied if applicable
    Then the final price is calculated correctly
    When I agree to the terms and place the order
    Then the order should be confirmed

    Examples:
      | Customer_Type | Product_Variant | Carrier  | Discount_Type | Country       | Quantity       |
      | Guest         | Price_Rule      | Standard | Cart_level    | International | 3Items_Or_More |
      | Guest         | Regular         | Express  | No discount   | Local         | Under3Items    |
      | Registered    | Regular         | Standard | No discount   | International | Under3Items    |
      | Registered    | Price_Rule      | Express  | Product_level | International | 3Items_Or_More |
      | Guest         | Price_Rule      | Standard | Product_level | International | Under3Items    |

  # Group B: VIP Users (Requires Login Step)
  # Covers Tests: 7, 8, 9
  # Note: Test 9 includes "No Carrier Available" but is grouped here due to VIP login requirement
  Scenario Outline: VIP checkout flow
    Given a "VIP" user is logged in
    When a "<Product_Variant>" product is added to the cart with quantity "<Quantity>"
    And the checkout process is started
    And an address for "<Country>" is entered
    And the "<Carrier>" delivery option is selected
    And a "<Discount_Type>" discount is applied
    Then the final price is calculated correctly with VIP benefits
    When I agree to the terms and place the order
    Then the order should be confirmed

    Examples:
      | Customer_Type | Product_Variant | Carrier              | Discount_Type | Country       | Quantity       |
      | VIP           | Price_Rule      | Standard             | Cart_level    | Local         | Under3Items    |
      | VIP           | Regular         | Express              | Cart_level    | International | 3Items_Or_More |
      | VIP           | Regular         | No Carrier Available | Cart_level    | Local         | Under3Items    |

  # Group C: Non-VIP Local Pickup (No Shipping Cost)
  # Covers Tests: 3, 6, 11
  Scenario Outline: Non-VIP checkout with In-Store Pickup
    Given a "<Customer_Type>" user
    When a "<Product_Variant>" product is added to the cart with quantity "<Quantity>"
    And the checkout process is started
    And an address for "<Country>" is entered
    And the "Local" option is selected
    And a "<Discount_Type>" discount is applied if applicable
    Then the final price is calculated correctly without shipping costs
    When I agree to the terms and place the order
    Then the order should be confirmed

    Examples:
      | Customer_Type | Product_Variant | Carrier              | Discount_Type | Country | Quantity       |
      | Guest         | Price_Rule      | No Carrier Available | Product_level | Local   | 3Items_Or_More |
      | Registered    | Regular         | No Carrier Available | Cart_level    | Local   | Under3Items    |
      | Guest         | Price_Rule      | No Carrier Available | No discount   | Local   | 3Items_Or_More |



