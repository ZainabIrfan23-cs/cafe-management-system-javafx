import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.geometry.Insets;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

// ============================================================
//   MAIN APPLICATION CLASS — Entry point of the program
// ============================================================
public class centralPerk extends Application {

    // ── Global stage so we can switch scenes from anywhere ──
    private static Stage primaryStage;

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    // ── Order history stored in memory (ArrayList) ──
    public static ArrayList<String> orderHistory = new ArrayList<>();

    // ── Currently logged-in user ──
    public static String currentUser = "";

    // ── Tax rate (10%) ──
    public static final double TAX_RATE = 0.10;

    // ── Pastel color palette ──
    public static final String COLOR_BG       = "#FFF5F0";   // soft cream
    public static final String COLOR_PRIMARY  = "#F2A7BB";   // pastel pink
    public static final String COLOR_ACCENT   = "#D4845A";   // warm brown
    public static final String COLOR_DARK     = "#5C3D2E";   // deep coffee
    public static final String COLOR_LIGHT    = "#FDE8D8";   // pale peach
    public static final String COLOR_WHITE    = "#FFFFFF";
    public static final String COLOR_CARD     = "#FFF9F5";   // card bg

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("☕ Central Perk ");
        stage.setWidth(1100);
        stage.setHeight(750);
        stage.setResizable(true);

        // Start with the welcome/login screen
        Scene loginScene = LoginScreen.create();
        stage.setScene(loginScene);

        stage.show();
    }

    // ── Helper: switch scenes cleanly ──
    public static void setScene(javafx.scene.Scene scene) {
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

// ============================================================
//   MENU ITEM MODEL — Represents one item on the menu
// ============================================================
class MenuItem {
    private String name;        // e.g., "Cappuccino"
    private double price;       // e.g., 4.50
    private String category;    // "Drinks" or "Food" or "Desserts"
    private String imageName;   // filename in images/ folder
    private String emoji;       // fallback emoji if image missing

    public MenuItem(String name, double price, String category, String imageName, String emoji) {
        this.name      = name;
        this.price     = price;
        this.category  = category;
        this.imageName = imageName;
        this.emoji     = emoji;
    }

    // Getters
    public String getName()      { return name; }
    public double getPrice()     { return price; }
    public String getCategory()  { return category; }
    public String getImageName() { return imageName; }
    public String getEmoji()     { return emoji; }

    public String getFormattedPrice() { return String.format("$%.2f", price); }
}

// ============================================================
//   CART ITEM MODEL — A menu item + chosen quantity
// ============================================================
class CartItem {
    private MenuItem menuItem;
    private int quantity;

    public CartItem(MenuItem menuItem, int quantity) {
        this.menuItem = menuItem;
        this.quantity = quantity;
    }

    public MenuItem getMenuItem() { return menuItem; }
    public int getQuantity()      { return quantity; }
    public void setQuantity(int q){ this.quantity = q; }

    // Total price for this line: price × quantity
    public double getSubtotal()   { return menuItem.getPrice() * quantity; }

    // For TableView columns
    public String getName()       { return menuItem.getName(); }
    public String getUnitPrice()  { return menuItem.getFormattedPrice(); }
    public String getQtyStr()     { return String.valueOf(quantity); }
    public String getSubtotalStr(){ return String.format("$%.2f", getSubtotal()); }
}

// ============================================================
//   ORDER HISTORY RECORD — stores a completed order
// ============================================================
class OrderRecord {
    private String orderId;
    private String timestamp;
    private String user;
    private double total;
    private String itemsSummary;

    public OrderRecord(String orderId, String timestamp, String user, double total, String itemsSummary) {
        this.orderId      = orderId;
        this.timestamp    = timestamp;
        this.user         = user;
        this.total        = total;
        this.itemsSummary = itemsSummary;
    }

    // Getters used by TableView
    public String getOrderId()      { return orderId; }
    public String getTimestamp()    { return timestamp; }
    public String getUser()         { return user; }
    public String getTotalStr()     { return String.format("$%.2f", total); }
    public String getItemsSummary() { return itemsSummary; }
}

// ============================================================
//   DATA STORE — Holds menu items & all cart logic
// ============================================================
class DataStore {

    // ── The global shopping cart ──
    public static ObservableList<CartItem> cart = FXCollections.observableArrayList();

    // ── All placed orders (history) ──
    public static ArrayList<OrderRecord> allOrders = new ArrayList<>();

    // ── Order counter for IDs ──
    private static int orderCounter = 1000;

    // ── Build the full cafe menu ──
    public static ArrayList<MenuItem> menu = new ArrayList<>();

    public static ArrayList<MenuItem> getMenuItems() {

        if(!menu.isEmpty()) return menu;

        // ☕ DRINKS
        menu.add(new MenuItem("Coffee",     3.50, "Drinks",   "coffee.png",     "☕"));
        menu.add(new MenuItem("Tea",        2.50, "Drinks",   "tea.png",        "🍵"));
        menu.add(new MenuItem("Latte",      4.50, "Drinks",   "latte.png",      "🥛"));
        menu.add(new MenuItem("Cappuccino", 4.00, "Drinks",   "cappuccino.png", "☕"));

        // 🍔 FOOD
        menu.add(new MenuItem("Burger",     7.50, "Food",     "burger.png",     "🍔"));
        menu.add(new MenuItem("Pasta",      8.50, "Food",     "pasta.png",      "🍝"));
        menu.add(new MenuItem("Pizza",      9.00, "Food",     "pizza.png",      "🍕"));
        menu.add(new MenuItem("Sandwich",   6.00, "Food",     "sandwich.png",   "🥪"));
        menu.add(new MenuItem("Fries",      3.50, "Food",     "fries.png",      "🍟"));

        // 🍰 DESSERTS
        menu.add(new MenuItem("Pancakes",   5.50, "Desserts", "pancakes.png",   "🥞"));
        menu.add(new MenuItem("Waffles",    5.50, "Desserts", "waffles.png",    "🧇"));
        menu.add(new MenuItem("Cake",       4.50, "Desserts", "cake.png",       "🎂"));
        menu.add(new MenuItem("Brownie",    3.50, "Desserts", "brownie.png",    "🍫"));
        menu.add(new MenuItem("Ice Cream",  3.00, "Desserts", "icecream.png",   "🍦"));

        return menu;
    }

    // ── Add item to cart or increase its quantity ──
    public static void addToCart(MenuItem item, int qty) {
        for (CartItem ci : cart) {
            if (ci.getMenuItem().getName().equals(item.getName())) {
                ci.setQuantity(ci.getQuantity() + qty);
                return;
            }
        }
        cart.add(new CartItem(item, qty));
    }

    // ── Remove item from cart ──
    public static void removeFromCart(String itemName) {
        cart.removeIf(ci -> ci.getMenuItem().getName().equals(itemName));
    }

    // ── Calculate subtotal (before tax) ──
    public static double getSubtotal() {
        double sum = 0;
        for (CartItem ci : cart) sum += ci.getSubtotal();
        return sum;
    }

    // ── Calculate tax ──
    public static double getTax() { return getSubtotal() * centralPerk.TAX_RATE; }

    // ── Calculate grand total ──
    public static double getTotal() { return getSubtotal() + getTax(); }

    // ── Place order: record it, clear cart ──
    public static String placeOrder() {
        if (cart.isEmpty()) return null;

        String id = "CC-" + (++orderCounter);
        String time = new SimpleDateFormat("dd MMM yyyy  HH:mm").format(new Date());

        // Build items summary string
        StringBuilder sb = new StringBuilder();
        for (CartItem ci : cart) {
            sb.append(ci.getName()).append(" x").append(ci.getQuantity()).append(", ");
        }
        String summary = sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "";

        OrderRecord rec = new OrderRecord(id, time, centralPerk.currentUser, getTotal(), summary);
        allOrders.add(rec);
        centralPerk.orderHistory.add(id + " | " + time + " | $" + String.format("%.2f", getTotal()));

        // Save to text file too (simple persistence)
        saveOrderToFile(rec);

        cart.clear();
        return id;
    }

    // ── Save order to orders.txt ──
    private static void saveOrderToFile(OrderRecord rec) {
        try (FileWriter fw = new FileWriter("orders.txt", true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(rec.getOrderId() + " | " + rec.getTimestamp() +
                    " | " + rec.getUser() + " | " + rec.getTotalStr() +
                    " | " + rec.getItemsSummary());
            bw.newLine();
        } catch (IOException e) {
            // silently skip if file can't be written
        }
    }
}

// ============================================================
//   STYLE HELPER — Centralized styling utilities
// ============================================================
class Styles {

    // ── Standard pastel button ──
    public static String btnPrimary() {
        return "-fx-background-color: " + centralPerk.COLOR_PRIMARY + ";" +
                "-fx-text-fill: " + centralPerk.COLOR_DARK + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 10 24 10 24;" +
                "-fx-cursor: hand;";
    }

    // ── Accent button (warm brown) ──
    public static String btnAccent() {
        return "-fx-background-color: " + centralPerk.COLOR_ACCENT + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 10 24 10 24;" +
                "-fx-cursor: hand;";
    }

    // ── Danger/remove button (muted red) ──
    public static String btnDanger() {
        return "-fx-background-color: #E07070;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 15;" +
                "-fx-padding: 6 16 6 16;" +
                "-fx-cursor: hand;";
    }

    // ── Small quiet button ──
    public static String btnSmall() {
        return "-fx-background-color: " + centralPerk.COLOR_LIGHT + ";" +
                "-fx-text-fill: " + centralPerk.COLOR_DARK + ";" +
                "-fx-font-size: 12px;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 6 14 6 14;" +
                "-fx-cursor: hand;";
    }

    // ── Card panel style ──
    public static String card() {
        return "-fx-background-color: " + centralPerk.COLOR_CARD + ";" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #F0D0C0;" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: dropshadow(gaussian, rgba(200,150,130,0.18), 10, 0, 0, 3);";
    }

    // ── Header label ──
    public static String header() {
        return "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + centralPerk.COLOR_DARK + ";";
    }

    // ── Sub-header label ──
    public static String subHeader() {
        return "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + centralPerk.COLOR_ACCENT + ";";
    }

    // ── Text field ──
    public static String textField() {
        return "-fx-background-color: white;" +
                "-fx-border-color: #F2A7BB;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-border-width: 1.5;" +
                "-fx-padding: 8 14;" +
                "-fx-font-size: 13px;";
    }

    // ── Main background ──
    public static String mainBg() {
        return "-fx-background-color: " + centralPerk.COLOR_BG + ";";
    }

    // ── Sidebar style ──
    public static String sidebar() {
        return "-fx-background-color: linear-gradient(to bottom, #F2A7BB, #F5C5A3);" +
                "-fx-padding: 20;";
    }
}

// ============================================================
//   LOGIN SCREEN
// ============================================================
class LoginScreen {

    public static Scene create() {
        // ── Root layout ──
        BorderPane root = new BorderPane();
        root.setStyle(Styles.mainBg());

        // ── Decorative left panel ──
        VBox leftPanel = new VBox(20);
        leftPanel.setPrefWidth(400);
        leftPanel.setAlignment(Pos.CENTER);
        leftPanel.setStyle("-fx-background-color: linear-gradient(to bottom right, #F2A7BB, #D4845A);" +
                "-fx-padding: 40;");

        Label logoEmoji = new Label("☕");
        logoEmoji.setStyle("-fx-font-size: 70px;");

        Label logoTitle = new Label("Central Perk Cafe");
        logoTitle.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label logoSub = new Label("Cafe & Bakery");
        logoSub.setStyle("-fx-font-size: 16px; -fx-text-fill: #FFF5F0;");

        Label tagline = new Label("🌸  Brewed with love, served with joy  🌸");
        tagline.setStyle("-fx-font-size: 13px; -fx-text-fill: #FFF5F0; -fx-padding: 20 0 0 0;");
        tagline.setWrapText(true);
        tagline.setTextAlignment(TextAlignment.CENTER);

        leftPanel.getChildren().addAll(logoEmoji, logoTitle, logoSub, tagline);

        // ── Right panel: login form ──
        VBox rightPanel = new VBox(18);
        rightPanel.setAlignment(Pos.CENTER);
        rightPanel.setPadding(new Insets(60, 60, 60, 60));
        rightPanel.setMaxWidth(420);

        Label welcomeLabel = new Label("Welcome Back! 👋");
        welcomeLabel.setStyle(Styles.header());

        Label subLabel = new Label("Please sign in to continue");
        subLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #A0826D;");

        // Username field
        Label userLabel = new Label("Username");
        userLabel.setStyle(Styles.subHeader());
        TextField userField = new TextField();
        userField.setPromptText("Enter your username...");
        userField.setStyle(Styles.textField());
        userField.setMaxWidth(280);

        // Password field
        Label passLabel = new Label("Password");
        passLabel.setStyle(Styles.subHeader());
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password...");
        passField.setStyle(Styles.textField());
        passField.setMaxWidth(280);

        // Error message label
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #C0504D; -fx-font-size: 12px;");

        // Login button
        Button loginBtn = new Button("☕  Sign In");
        loginBtn.setStyle(Styles.btnAccent());
        loginBtn.setMaxWidth(280);
        loginBtn.setPrefHeight(44);

        // Hint label
        Label hintLabel = new Label("💡 User: user/123");
        hintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #B09080; -fx-padding: 10 0 0 0;");
        hintLabel.setWrapText(true);
        hintLabel.setTextAlignment(TextAlignment.CENTER);

        // ── Login button action ──
        loginBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = passField.getText().trim();
            if (authenticate(user, pass)) {
                centralPerk.currentUser = user;
                centralPerk.setScene(DashboardScreen.create());
            } else {
                errorLabel.setText("❌ Invalid username or password. Please try again.");
                passField.clear();
            }
        });

        // Allow pressing Enter to login
        passField.setOnAction(e -> loginBtn.fire());

        rightPanel.getChildren().addAll(
                welcomeLabel, subLabel,
                new Separator(),
                userLabel, userField,
                passLabel, passField,
                errorLabel,
                loginBtn,
                hintLabel
        );

        // ── Wrap right panel in a centered container ──
        VBox rightWrapper = new VBox();
        rightWrapper.setAlignment(Pos.CENTER);
        rightWrapper.getChildren().add(rightPanel);
        rightWrapper.setStyle(Styles.mainBg());

        root.setLeft(leftPanel);
        root.setCenter(rightWrapper);

        return new Scene(root, 1100, 750);
    }

    // ── Check username/password ──
    private static boolean authenticate(String user, String pass) {
        return (user.equals("user") && pass.equals("123"));
    }
}

// ============================================================
//   DASHBOARD SCREEN — main navigation hub
// ============================================================
class DashboardScreen {

    public static Scene create() {
        BorderPane root = new BorderPane();
        root.setStyle(Styles.mainBg());

        // ── Top bar ──
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // ── Sidebar ──
        VBox sidebar = createSidebar(root);
        root.setLeft(sidebar);

        // ── Default center: show menu ──
        root.setCenter(MenuScreen.createContent(root));

        return new Scene(root, 1100, 750);
    }

    // ── Top navigation bar ──
    private static HBox createTopBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setStyle("-fx-background-color: " + centralPerk.COLOR_DARK + ";");

        Label logo = new Label("☕ Central Perk Cafe");
        logo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #FFF5F0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label("👤 " + centralPerk.currentUser);
        userLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #F2A7BB;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle(Styles.btnSmall());
        logoutBtn.setOnAction(e -> {
            DataStore.cart.clear();
            centralPerk.setScene(LoginScreen.create());
        });

        bar.getChildren().addAll(logo, spacer, userLabel, logoutBtn);
        return bar;
    }

    // ── Sidebar navigation ──
    private static VBox createSidebar(BorderPane root) {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(190);
        sidebar.setPadding(new Insets(24, 14, 24, 14));
        sidebar.setStyle(Styles.sidebar());

        Label navTitle = new Label("Navigation");
        navTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #5C3D2E; -fx-font-weight: bold; -fx-padding: 0 0 8 4;");

        // Navigation buttons
        Button menuBtn     = makeSidebarBtn("🍽️  Menu",         root, () -> MenuScreen.createContent(root));
        Button cartBtn     = makeSidebarBtn("🛒  Cart",         root, () -> CartScreen.createContent(root));
        Button historyBtn  = makeSidebarBtn("📋  Order History", root, () -> HistoryScreen.createContent());




        // Decorative label at bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label tagline = new Label("🌸 Brewed with love");
        tagline.setStyle("-fx-font-size: 11px; -fx-text-fill: #7A4A3A; -fx-text-alignment: center;");

        sidebar.getChildren().addAll(navTitle, menuBtn, cartBtn, historyBtn, spacer, tagline);
        return sidebar;
    }

    // ── Helper: create a styled sidebar navigation button ──
    private static Button makeSidebarBtn(String text, BorderPane root, java.util.function.Supplier<javafx.scene.Node> contentFn) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: rgba(255,255,255,0.55);" +
                "-fx-text-fill: " + centralPerk.COLOR_DARK + ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 14;" +
                "-fx-padding: 12 16;" +
                "-fx-alignment: CENTER_LEFT;" +
                "-fx-cursor: hand;");

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + centralPerk.COLOR_WHITE + ";" +
                        "-fx-text-fill: " + centralPerk.COLOR_ACCENT + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 12 16;" +
                        "-fx-alignment: CENTER_LEFT;" +
                        "-fx-cursor: hand;"));

        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.55);" +
                        "-fx-text-fill: " + centralPerk.COLOR_DARK + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 12 16;" +
                        "-fx-alignment: CENTER_LEFT;" +
                        "-fx-cursor: hand;"));

        btn.setOnAction(e -> root.setCenter(contentFn.get()));
        return btn;
    }
}

// ============================================================
//   MENU SCREEN — Browse & add items to cart
// ============================================================
class MenuScreen {

    public static javafx.scene.Node createContent(BorderPane root) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle(Styles.mainBg());

        // ── Page header ──
        Label title = new Label("🍽️  Our Menu");
        title.setStyle(Styles.header());

        Label subtitle = new Label("Tap any item to add it to your cart!");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #A0826D;");

        // ── Search bar ──
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search menu items...");
        searchField.setStyle(Styles.textField());
        searchField.setPrefWidth(280);

        Label cartStatus = new Label("🛒 Cart: " + DataStore.cart.size() + " items");
        cartStatus.setStyle(Styles.subHeader());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewCartBtn = new Button("View Cart →");
        viewCartBtn.setStyle(Styles.btnAccent());
        viewCartBtn.setOnAction(e -> root.setCenter(CartScreen.createContent(root)));

        searchBox.getChildren().addAll(searchField, spacer, cartStatus, viewCartBtn);

        // ── Category tabs ──
        HBox categoryBar = new HBox(10);
        categoryBar.setAlignment(Pos.CENTER_LEFT);

        // Container for the menu grid (we'll rebuild it on search/filter)
        VBox menuContainer = new VBox();

        // Show all items initially
        buildMenuGrid(menuContainer, DataStore.getMenuItems(), root, cartStatus);

        // Filter buttons
        String[] cats = {"All", "Drinks", "Food", "Desserts"};
        for (String cat : cats) {
            Button catBtn = new Button(cat);
            catBtn.setStyle(Styles.btnSmall());
            catBtn.setOnAction(e -> {
                ArrayList<MenuItem> all = DataStore.getMenuItems();
                ArrayList<MenuItem> filtered = new ArrayList<>();
                for (MenuItem m : all) {
                    if (cat.equals("All") || m.getCategory().equals(cat)) {
                        filtered.add(m);
                    }
                }
                buildMenuGrid(menuContainer, filtered, root, cartStatus);
            });
            categoryBar.getChildren().add(catBtn);
        }

        // Live search filter
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            ArrayList<MenuItem> all  = DataStore.getMenuItems();
            ArrayList<MenuItem> results = new ArrayList<>();
            String q = newVal.toLowerCase().trim();
            for (MenuItem m : all) {
                if (q.isEmpty() || m.getName().toLowerCase().contains(q) ||
                        m.getCategory().toLowerCase().contains(q)) {
                    results.add(m);
                }
            }
            buildMenuGrid(menuContainer, results, root, cartStatus);
        });

        content.getChildren().addAll(title, subtitle, searchBox, categoryBar, menuContainer);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + centralPerk.COLOR_BG + "; -fx-background: " + centralPerk.COLOR_BG + ";");
        return scroll;
    }

    // ── Build the menu grid cards ──
    private static void buildMenuGrid(VBox container, ArrayList<MenuItem> items, BorderPane root, Label cartStatus) {
        container.getChildren().clear();

        // Category groupings
        String[] categories = {"Drinks", "Food", "Desserts"};
        String[] catEmojis  = {"☕", "🍔", "🍰"};

        for (int c = 0; c < categories.length; c++) {
            String cat = categories[c];
            ArrayList<MenuItem> catItems = new ArrayList<>();
            for (MenuItem m : items) {
                if (m.getCategory().equals(cat)) catItems.add(m);
            }
            if (catItems.isEmpty()) continue;

            // Section header
            Label sectionLabel = new Label(catEmojis[c] + "  " + cat);
            sectionLabel.setStyle(Styles.subHeader() +
                    "-fx-padding: 14 0 6 0; -fx-font-size: 18px;");

            // Grid of cards
            FlowPane grid = new FlowPane(14, 14);
            grid.setPadding(new Insets(0, 0, 10, 0));

            for (MenuItem item : catItems) {
                grid.getChildren().add(buildMenuCard(item, root, cartStatus));
            }

            container.getChildren().addAll(sectionLabel, grid);
        }
    }

    // ── Single menu item card ──
    private static VBox buildMenuCard(MenuItem item, BorderPane root, Label cartStatus) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14));
        card.setPrefWidth(165);
        card.setPrefHeight(210);
        card.setStyle(Styles.card());

        // Image or emoji fallback
        ImageView imgView = loadImage(item.getImageName(), item.getEmoji());

        // Item name
        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + centralPerk.COLOR_DARK + ";");
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(TextAlignment.CENTER);

        // Price
        Label priceLabel = new Label(item.getFormattedPrice());
        priceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + centralPerk.COLOR_ACCENT + "; -fx-font-weight: bold;");

        // Quantity spinner (1–20)
        Spinner<Integer> spinner = new Spinner<>(1, 20, 1);
        spinner.setPrefWidth(90);
        spinner.setStyle("-fx-font-size: 12px;");

        // Add to cart button
        Button addBtn = new Button("+ Add to Cart");
        addBtn.setStyle(Styles.btnPrimary() + "-fx-font-size: 12px; -fx-padding: 6 12;");
        addBtn.setMaxWidth(140);

        addBtn.setOnAction(e -> {
            int qty = spinner.getValue();
            DataStore.addToCart(item, qty);
            cartStatus.setText("🛒 Cart: " + DataStore.cart.size() + " items");

            // Brief visual feedback
            addBtn.setText("✓ Added!");
            addBtn.setStyle(Styles.btnAccent() + "-fx-font-size: 12px; -fx-padding: 6 12;");
            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ex) {}
                Platform.runLater(() -> {
                    addBtn.setText("+ Add to Cart");
                    addBtn.setStyle(Styles.btnPrimary() + "-fx-font-size: 12px; -fx-padding: 6 12;");
                });
            }).start();
        });

        card.getChildren().addAll(imgView, nameLabel, priceLabel, spinner, addBtn);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                Styles.card() + "-fx-effect: dropshadow(gaussian, rgba(200,130,100,0.35), 16, 0, 0, 5);"));
        card.setOnMouseExited(e -> card.setStyle(Styles.card()));

        return card;
    }

    // ── Load image from images/ folder, fallback to emoji label ──
    private static ImageView loadImage(String filename, String emoji) {
        ImageView imgView = new ImageView();
        imgView.setFitWidth(80);
        imgView.setFitHeight(70);
        imgView.setPreserveRatio(true);

        try {
            File imgFile = new File("images/" + filename);
            if (imgFile.exists()) {
                imgView.setImage(new Image(imgFile.toURI().toString()));
                return imgView;
            }
        } catch (Exception ex) {
            // ignore
        }

        // Fallback: emoji in a colored label (wrapped in StackPane trick via canvas)
        // We return a view with a placeholder color background
        imgView.setStyle("-fx-background-color: " + centralPerk.COLOR_LIGHT + ";");
        return imgView;
    }
}

// ============================================================
//   CART SCREEN — Review cart & checkout
// ============================================================
class CartScreen {

    public static javafx.scene.Node createContent(BorderPane root) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle(Styles.mainBg());

        Label title = new Label("🛒  Your Cart");
        title.setStyle(Styles.header());

        if (DataStore.cart.isEmpty()) {
            // Empty cart message
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60));

            Label emptyEmoji = new Label("🛒");
            emptyEmoji.setStyle("-fx-font-size: 60px;");
            Label emptyLabel = new Label("Your cart is empty!");
            emptyLabel.setStyle(Styles.subHeader());
            Label emptyHint = new Label("Head over to the menu to add some delicious items 😊");
            emptyHint.setStyle("-fx-font-size: 13px; -fx-text-fill: #A0826D;");

            Button menuBtn = new Button("← Browse Menu");
            menuBtn.setStyle(Styles.btnPrimary());
            menuBtn.setOnAction(e -> root.setCenter(MenuScreen.createContent(root)));

            emptyBox.getChildren().addAll(emptyEmoji, emptyLabel, emptyHint, menuBtn);
            content.getChildren().addAll(title, emptyBox);
        } else {
            // ── Cart items table ──
            TableView<CartItem> table = new TableView<>();
            table.setStyle("-fx-background-radius: 12; -fx-font-size: 13px;");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<CartItem, String> nameCol = new TableColumn<>("Item");

            nameCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));

            TableColumn<CartItem, String> priceCol = new TableColumn<>("Unit Price");

            priceCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUnitPrice()));
            priceCol.setPrefWidth(100);

            TableColumn<CartItem, String> qtyCol = new TableColumn<>("Qty");

            qtyCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getQtyStr()));
            qtyCol.setPrefWidth(70);

            TableColumn<CartItem, String> subCol = new TableColumn<>("Subtotal");

            subCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSubtotalStr()));
            subCol.setPrefWidth(100);

            table.getColumns().addAll(nameCol, priceCol, qtyCol, subCol);
            table.setItems(DataStore.cart);
            table.setPrefHeight(280);

            // ── Remove selected item ──
            Button removeBtn = new Button("🗑️  Remove Selected");
            removeBtn.setStyle(Styles.btnDanger());
            removeBtn.setOnAction(e -> {
                CartItem selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    DataStore.removeFromCart(selected.getName());
                    root.setCenter(CartScreen.createContent(root)); // refresh
                }
            });

            Button clearBtn = new Button("Clear Cart");
            clearBtn.setStyle(Styles.btnSmall());
            clearBtn.setOnAction(e -> {
                DataStore.cart.clear();
                root.setCenter(CartScreen.createContent(root));
            });

            HBox tableActions = new HBox(10, removeBtn, clearBtn);

            // ── Billing summary panel ──
            VBox billBox = new VBox(10);
            billBox.setPadding(new Insets(20));
            billBox.setStyle(Styles.card());
            billBox.setMaxWidth(340);
            billBox.setAlignment(Pos.CENTER_LEFT);

            Label billTitle = new Label("💳  Bill Summary");
            billTitle.setStyle(Styles.subHeader() + "-fx-font-size: 17px;");

            Label subTotalLbl  = new Label(String.format("Subtotal:   $%.2f", DataStore.getSubtotal()));
            Label taxLbl       = new Label(String.format("Tax (10%%):  $%.2f", DataStore.getTax()));
            Label totalLbl     = new Label(String.format("TOTAL:      $%.2f", DataStore.getTotal()));
            subTotalLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: " + centralPerk.COLOR_DARK + ";");
            taxLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: " + centralPerk.COLOR_DARK + ";");
            totalLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + centralPerk.COLOR_ACCENT + ";");

            Button checkoutBtn = new Button("✅  Place Order");
            checkoutBtn.setStyle(Styles.btnAccent());
            checkoutBtn.setPrefWidth(240);
            checkoutBtn.setPrefHeight(44);
            checkoutBtn.setOnAction(e -> {
                String orderId = DataStore.placeOrder();
                if (orderId != null) {
                    showReceipt(orderId, root);
                }
            });

            billBox.getChildren().addAll(billTitle, new Separator(),
                    subTotalLbl, taxLbl, new Separator(), totalLbl,
                    new Label(""), checkoutBtn);

            HBox bottomSection = new HBox(24, new VBox(10, table, tableActions), billBox);
            HBox.setHgrow(bottomSection.getChildren().get(0), Priority.ALWAYS);
            bottomSection.setAlignment(Pos.TOP_LEFT);

            content.getChildren().addAll(title, bottomSection);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + centralPerk.COLOR_BG + "; -fx-background: " + centralPerk.COLOR_BG + ";");
        return scroll;
    }

    // ── Show receipt popup after order ──
    private static void showReceipt(String orderId, BorderPane root) {
        // Find the order we just placed
        OrderRecord lastOrder = null;
        for (OrderRecord rec : DataStore.allOrders) {
            if (rec.getOrderId().equals(orderId)) { lastOrder = rec; break; }
        }

        Stage receiptStage = new Stage();
        receiptStage.setTitle("🧾 Receipt — " + orderId);
        receiptStage.initOwner(centralPerk.getPrimaryStage());   // ties it to the main window
        receiptStage.initModality(Modality.WINDOW_MODAL);        // blocks interaction with main window until closed
        receiptStage.setAlwaysOnTop(true);                       // forces it above everything, incl. recorder overlays

        VBox receiptBox = new VBox(14);
        receiptBox.setPadding(new Insets(30));
        receiptBox.setAlignment(Pos.CENTER);
        receiptBox.setStyle("-fx-background-color: " + centralPerk.COLOR_BG + ";");
        receiptBox.setPrefWidth(380);

        Label logoLbl = new Label("☕ Central Perk Cafe");
        logoLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + centralPerk.COLOR_DARK + ";");

        Label idLbl = new Label("Order #" + orderId);
        idLbl.setStyle(Styles.subHeader());

        Label dateLbl = new Label("📅 " + new SimpleDateFormat("dd MMM yyyy  HH:mm").format(new Date()));
        dateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #808080;");

        Separator sep1 = new Separator();

        // Items
        VBox itemsBox = new VBox(6);
        for (CartItem ci : lastOrder != null ? new ArrayList<CartItem>() : new ArrayList<CartItem>()) {
            // The cart is already cleared; show from lastOrder items summary
        }
        // We'll use the summary string instead since cart is cleared
        Label itemsLbl = new Label(lastOrder != null ? lastOrder.getItemsSummary() : "");
        itemsLbl.setWrapText(true);
        itemsLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + centralPerk.COLOR_DARK + ";");

        Separator sep2 = new Separator();

        Label totalLbl = new Label("TOTAL: " + (lastOrder != null ? lastOrder.getTotalStr() : ""));
        totalLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + centralPerk.COLOR_ACCENT + ";");

        Label thankYou = new Label("🌸  Thank you for visiting!  🌸");
        thankYou.setStyle("-fx-font-size: 15px; -fx-text-fill: " + centralPerk.COLOR_PRIMARY + "; -fx-font-weight: bold;");

        Label comeback = new Label("Come back soon for more cozy treats! ☕");
        comeback.setStyle("-fx-font-size: 12px; -fx-text-fill: #A0826D;");

        Button closeBtn = new Button("Done ✓");
        closeBtn.setStyle(Styles.btnPrimary());
        closeBtn.setOnAction(e -> {
            receiptStage.close();
            root.setCenter(MenuScreen.createContent(root)); // back to menu
        });

        receiptBox.getChildren().addAll(
                logoLbl, idLbl, dateLbl, sep1,
                new Label("Items:"), itemsLbl,
                sep2, totalLbl, new Label(""),
                thankYou, comeback, new Label(""), closeBtn
        );

        Scene receiptScene = new Scene(receiptBox, 380, 440);
        receiptStage.setScene(receiptScene);
        receiptStage.setResizable(false);
        receiptStage.show();
        receiptStage.centerOnScreen();   // must be called AFTER show(), once the stage has a real size
        receiptStage.toFront();          // extra nudge to bring it above the main window
        receiptStage.requestFocus();
    }
}

// ============================================================
//   ORDER HISTORY SCREEN
// ============================================================
class HistoryScreen {

    public static javafx.scene.Node createContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle(Styles.mainBg());

        Label title = new Label("📋  Order History");
        title.setStyle(Styles.header());

        Label subtitle = new Label("All orders placed during this session");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #A0826D;");

        if (DataStore.allOrders.isEmpty()) {
            Label noOrders = new Label("No orders yet. Place your first order from the menu! ☕");
            noOrders.setStyle(Styles.subHeader());
            content.getChildren().addAll(title, subtitle, noOrders);
        } else {
            // Table of orders
            TableView<OrderRecord> table = new TableView<>();
            table.setStyle("-fx-background-radius: 12; -fx-font-size: 13px;");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<OrderRecord, String> idCol = new TableColumn<>("Order ID");

            idCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOrderId()));
            idCol.setPrefWidth(110);

            TableColumn<OrderRecord, String> timeCol = new TableColumn<>("Date & Time");

            timeCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTimestamp()));
            timeCol.setPrefWidth(160);

            TableColumn<OrderRecord, String> userCol = new TableColumn<>("Served By");

            userCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUser()));
            userCol.setPrefWidth(90);

            TableColumn<OrderRecord, String> totalCol = new TableColumn<>("Total");

            totalCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTotalStr()));
            totalCol.setPrefWidth(90);

            TableColumn<OrderRecord, String> itemsCol = new TableColumn<>("Items");

            itemsCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getItemsSummary()));

            table.getColumns().addAll(idCol, timeCol, userCol, totalCol, itemsCol);
            table.setItems(FXCollections.observableArrayList(DataStore.allOrders));
            table.setPrefHeight(400);

            // Summary stats
            double totalRevenue = 0;
            for (OrderRecord r : DataStore.allOrders) totalRevenue += Double.parseDouble(r.getTotalStr().replace("$", ""));
            Label statsLabel = new Label(String.format("📊  Total Orders: %d   |   Total Revenue: $%.2f",
                    DataStore.allOrders.size(), totalRevenue));
            statsLabel.setStyle(Styles.subHeader());

            content.getChildren().addAll(title, subtitle, statsLabel, table);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + centralPerk.COLOR_BG + "; -fx-background: " + centralPerk.COLOR_BG + ";");
        return scroll;
    }
}