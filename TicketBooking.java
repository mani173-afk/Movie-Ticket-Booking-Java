import java.sql.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;


/**
 * Movie Ticket Booking - pure AWT (no Swing).
 * Compile:  javac TicketBooking.java
 * Run:      java TicketBooking
 */
public class TicketBooking extends Frame {

    // ---------- constants ----------
    static final String[] MOVIES = {"RaanBali", "Avengers:Doomsday", "Jailer 2", "ltlu Arjun","418"};
    static final String[] TIMES  = {"10:00 AM", "2:00 PM", "6:00 PM", "9:00 PM"};
    static final int ROWS = 6, COLS = 8, MAX_SEATS = 6;
    static final int PREMIUM = 250, STANDARD = 150;      // rows A,B premium
    static final int POPCORN = 150, DRINK = 100;
    static final double GST = 0.18;

    // ---------- components ----------
    Choice movieChoice = new Choice();
    CheckboxGroup showGroup = new CheckboxGroup();
    Checkbox[] shows = new Checkbox[TIMES.length];
    TextField nameField = new TextField(14);
    Checkbox popcorn = new Checkbox("Popcorn (Rs." + POPCORN + ")");
    Checkbox drink = new Checkbox("Cold Drink (Rs." + DRINK + ")");
    Button[][] seats = new Button[ROWS][COLS];
    TextArea summary = new TextArea("", 14, 26, TextArea.SCROLLBARS_VERTICAL_ONLY);
    Canvas legend;

    // ---------- data ----------
    // key = "movie|time"  ->  set of booked seat IDs for that show
    HashMap<String, HashSet<String>> booked = new HashMap<>();
    TreeSet<String> selected = new TreeSet<>();          // seats picked by the current user

    // =====================================================
    TicketBooking() {
        super("Movie Ticket Booking");
        setLayout(new BorderLayout(6, 6));
        setSize(760, 600);
        setLocationRelativeTo(null);                      // center on screen

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });

        buildTopPanel();
        buildSeatGrid();
        buildSummaryPanel();
        buildBottomPanel();

        refreshSeats();
        setVisible(true);                                  // always last
    }

    // ---------- UI building ----------
    void buildTopPanel() {
        Panel top = new Panel(new GridLayout(3, 1));

        Label title = new Label("MOVIE TICKET BOOKING", Label.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        top.add(title);

        Panel row2 = new Panel(new FlowLayout(FlowLayout.LEFT));
        for (String m : MOVIES) movieChoice.add(m);
        movieChoice.addItemListener(e -> refreshSeats());
        row2.add(new Label("Movie:"));
        row2.add(movieChoice);
        row2.add(new Label("   Show:"));
        for (int i = 0; i < TIMES.length; i++) {
            shows[i] = new Checkbox(TIMES[i], showGroup, i == 0);   // first show selected by default
            shows[i].addItemListener(e -> refreshSeats());
            row2.add(shows[i]);
        }
        top.add(row2);

        Panel row3 = new Panel(new FlowLayout(FlowLayout.LEFT));
        row3.add(new Label("Name:"));
        row3.add(nameField);
        popcorn.addItemListener(e -> updateSummary());
        drink.addItemListener(e -> updateSummary());
        row3.add(popcorn);
        row3.add(drink);
        top.add(row3);

        add(top, BorderLayout.NORTH);
    }

    void buildSeatGrid() {
        Panel center = new Panel(new BorderLayout(4, 4));

        Label screen = new Label("=============  SCREEN  =============", Label.CENTER);
        screen.setBackground(Color.LIGHT_GRAY);
        center.add(screen, BorderLayout.NORTH);

        Panel grid = new Panel(new GridLayout(ROWS, COLS, 4, 4));
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                final String id = "" + (char) ('A' + r) + (c + 1);
                final Button b = new Button(id);
                b.addActionListener(e -> seatClicked(id, b));       // one handler style for all 48 seats
                seats[r][c] = b;
                grid.add(b);
            }
        }
        center.add(grid, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    void buildSummaryPanel() {
        Panel east = new Panel(new BorderLayout());
        east.add(new Label("Booking Summary", Label.CENTER), BorderLayout.NORTH);
        summary.setEditable(false);
        east.add(summary, BorderLayout.CENTER);
        add(east, BorderLayout.EAST);
    }

    void buildBottomPanel() {
        Panel south = new Panel(new BorderLayout());

        Panel buttons = new Panel(new FlowLayout());
        Button book = new Button("Book");
        Button clear = new Button("Clear Selection");
        Button cancel = new Button("Cancel Booking");
        Button exit = new Button("Exit");
        book.addActionListener(e -> bookSeats());
        clear.addActionListener(e -> { selected.clear(); refreshSeats(); });
        cancel.addActionListener(e -> cancelBooking());
        exit.addActionListener(e -> System.exit(0));
        buttons.add(book);
        buttons.add(clear);
        buttons.add(cancel);
        buttons.add(exit);
        south.add(buttons, BorderLayout.NORTH);

        // Canvas: colour legend + "percentage booked" bar
        legend = new Canvas() {
            public Dimension getPreferredSize() { return new Dimension(700, 40); }
            public void paint(Graphics g) {
                Color[] colors = {Color.GREEN, Color.YELLOW, Color.RED};
                String[] labels = {"Available", "Selected", "Booked"};
                int x = 10;
                for (int i = 0; i < 3; i++) {
                    g.setColor(colors[i]);
                    g.fillRect(x, 12, 16, 16);
                    g.setColor(Color.BLACK);
                    g.drawRect(x, 12, 16, 16);
                    g.drawString(labels[i], x + 22, 25);
                    x += 105;
                }
                int total = ROWS * COLS, b = bookedFor().size();
                g.drawString("Booked: " + b + "/" + total, 345, 25);
                g.setColor(Color.RED);
                g.fillRect(450, 12, 150 * b / total, 16);
                g.setColor(Color.BLACK);
                g.drawRect(450, 12, 150, 16);
            }
        };
        south.add(legend, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    // ---------- logic ----------
    String showKey() {
        Checkbox c = showGroup.getSelectedCheckbox();
        return movieChoice.getSelectedItem() + "|" + (c == null ? "" : c.getLabel());
    }

    HashSet<String> bookedFor() {
        return booked.computeIfAbsent(showKey(), k -> new HashSet<>());
    }

    int price(String id) { return id.charAt(0) <= 'B' ? PREMIUM : STANDARD; }

    void seatClicked(String id, Button b) {
        if (selected.contains(id)) {
            selected.remove(id);
            b.setBackground(Color.GREEN);
        } else {
            if (selected.size() >= MAX_SEATS) {
                showMsg("Limit", "You can select at most " + MAX_SEATS + " seats.");
                return;
            }
            selected.add(id);
            b.setBackground(Color.YELLOW);
        }
        updateSummary();
    }

    /** Repaints every seat from the data of the currently chosen show. */
    void refreshSeats() {
        selected.clear();
        HashSet<String> taken = bookedFor();
        for (Button[] row : seats) {
            for (Button s : row) {
                boolean isBooked = taken.contains(s.getLabel());
                s.setBackground(isBooked ? Color.RED : Color.GREEN);
                s.setEnabled(!isBooked);
            }
        }
        updateSummary();
        if (legend != null) legend.repaint();
    }

    int snackTotal() { return (popcorn.getState() ? POPCORN : 0) + (drink.getState() ? DRINK : 0); }

    int ticketTotal() {
        int sum = 0;
        for (String s : selected) sum += price(s);
        return sum;
    }

    void updateSummary() {
        int tickets = ticketTotal(), snacks = snackTotal();
        double gst = (tickets + snacks) * GST;
        summary.setText(
            "Movie : " + movieChoice.getSelectedItem() + "\n" +
            "Show  : " + (showGroup.getSelectedCheckbox() == null ? "-" : showGroup.getSelectedCheckbox().getLabel()) + "\n\n" +
            "Seats : " + selected + "\n" +
            "Count : " + selected.size() + "\n\n" +
            "Tickets : Rs." + tickets + "\n" +
            "Snacks  : Rs." + snacks + "\n" +
            String.format("GST 18%% : Rs.%.2f%n", gst) +
            String.format("TOTAL    : Rs.%.2f", tickets + snacks + gst));
    }

    void bookSeats() {

        String name = nameField.getText().trim();

    if (name.isEmpty()) {
        showMsg("Error", "Please enter the customer name.");
        return;
    }

    if (showGroup.getSelectedCheckbox() == null) {
        showMsg("Error", "Please choose a show time.");
        return;
    }

    if (selected.isEmpty()) {
        showMsg("Error", "Please select at least one seat.");
        return;
    }

    int tickets = ticketTotal();
    int snacksAmount = snackTotal();

    double amount = tickets + snacksAmount;
    double gst = amount * GST;
    double total = amount + gst;

    String movie = movieChoice.getSelectedItem();
    String time = showGroup.getSelectedCheckbox().getLabel();
    String seatIds = selected.toString();

    String snacks = "";
    if (popcorn.getState()) snacks += "Popcorn ";
    if (drink.getState()) snacks += "Cold Drink";
    if (snacks.isEmpty()) snacks = "None";

    // Save to MySQL before confirming the booking
    boolean saved = saveBooking(
        name, movie, time, seatIds,
        selected.size(), amount, snacks, gst, total
    );

    if (!saved) {
        showMsg("Database Error",
                "Booking was not saved. Please check the database connection.");
        return;
    }

    bookedFor().addAll(selected);

    String ticket =
        "*** TICKET CONFIRMED ***\n\n" +
        "Name: " + name + "\n" +
        "Movie: " + movie + "\n" +
        "Show: " + time + "\n" +
        "Seats: " + seatIds + "\n" +
        "Tickets: " + selected.size() + "\n" +
        "Amount: Rs." + String.format("%.2f", amount) + "\n" +
        "GST: Rs." + String.format("%.2f", gst) + "\n" +
        "Total: Rs." + String.format("%.2f", total);

    selected.clear();
    refreshSeats();

    showMsg("Booking Successful", ticket);
}
    void cancelBooking() {
        String id = askInput("Cancel Booking", "Enter seat ID to cancel (e.g. A3):").trim().toUpperCase();
        if (id.isEmpty()) return;
        if (bookedFor().remove(id)) {
            refreshSeats();
            showMsg("Cancelled", "Seat " + id + " is now available.");
        } else {
            showMsg("Error", "Seat " + id + " is not booked for this show.");
        }
    }

    // ---------- dialog helpers ----------
    void showMsg(String title, String text) {
        final Dialog d = new Dialog(this, title, true);          // true = modal
        d.setLayout(new BorderLayout(6, 6));
        TextArea ta = new TextArea(text, 8, 34, TextArea.SCROLLBARS_NONE);
        ta.setEditable(false);
        Button ok = new Button("OK");
        ok.addActionListener(e -> d.dispose());
        Panel p = new Panel(new FlowLayout());
        p.add(ok);
        d.add(ta, BorderLayout.CENTER);
        d.add(p, BorderLayout.SOUTH);
        d.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { d.dispose(); }
        });
        d.setSize(340, 240);
        d.setLocationRelativeTo(this);
        d.setVisible(true);                                       // blocks until closed
    }

    String askInput(String title, String prompt) {
        final String[] result = {""};
        final Dialog d = new Dialog(this, title, true);
        d.setLayout(new FlowLayout());
        final TextField tf = new TextField(10);
        Button ok = new Button("OK");
        ok.addActionListener(e -> { result[0] = tf.getText(); d.dispose(); });
        d.add(new Label(prompt));
        d.add(tf);
        d.add(ok);
        d.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { d.dispose(); }
        });
        d.setSize(320, 120);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
        return result[0];
    }
      boolean saveBooking(
        String name,
        String show,
        String time,
        String seats,
        int tickets,
        double amount,
        String snacks,
        double gst,
        double total) {

    String sql =
        "INSERT INTO bookings " +
        "(customer_name, show_name, show_time, seats, " +
        "tickets, amount, snacks, gst, total) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, name);
        ps.setString(2, show);
        ps.setString(3, time);
        ps.setString(4, seats);
        ps.setInt(5, tickets);
        ps.setDouble(6, amount);
        ps.setString(7, snacks);
        ps.setDouble(8, gst);
        ps.setDouble(9, total);

        int rows = ps.executeUpdate();

        return rows > 0;

    } catch (SQLException e) {
        System.err.println("Failed to save booking:");
        e.printStackTrace();
        return false;

    } catch (Exception e) {
        System.err.println("Database connection error:");
        e.printStackTrace();
        return false;
    }
}

    public static void main(String[] args) {
        new TicketBooking();
    }
}
