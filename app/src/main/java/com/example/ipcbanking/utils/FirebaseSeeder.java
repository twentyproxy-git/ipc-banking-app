package com.example.ipcbanking.utils;

import android.content.Context;
import android.util.Log;

import com.example.ipcbanking.models.CinemaBrand;
import com.example.ipcbanking.models.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSeeder {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final List<UserData> seedList = new ArrayList<>();

    private final Map<String, String> createdUserIds = new HashMap<>();

    public FirebaseSeeder(Context context) {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    // ==================== PUBLIC CALL ====================
    public void seedUsers() {
        seedBankConfig();
        seedBankBranches();
        seedBills(); // Seed bills
        seedMovieData(); // Seed movie data
        seedFlightData(); // Seed flight data

        seedList.clear();
        createdUserIds.clear();

        // OFFICERS
        seedList.add(new UserData("topaz@ipc.com", "topaz123", "Topaz Numby", "0901000111",
                "Pier Point, Trụ sở IPC", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319532/qmr7tdydrnneiiflkxhb.png"));

        seedList.add(new UserData("aventurine@ipc.com", "aventurine123", "Aventurine Stratos", "0901000222",
                "Sigonia-IV, Khu Tài Chính", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319490/jvn1en9l5g1dxzqx7twg.png"));

        seedList.add(new UserData("sunday@ipc.com", "sunday123", "Sunday Halovian", "0901000333",
                "Khách sạn Reverie, Penacony", "OFFICER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319526/eouerxvjagmdisvagc6d.png"));

        // CUSTOMERS
        seedList.add(new UserData("kafka@gmail.com", "kafka123", "Kafka", "0909666777",
                "Pteruges-V, Hầm trú ẩn Stellaron", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319508/rnbwc3kipbbtg4y7puhr.png"));

        seedList.add(new UserData("silverwolf@gmail.com", "silverwolf123", "Silver Wolf", "0909888999",
                "Punklorde, Tiệm Net 8-bit", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319517/mgbqst17h39kfukr8zsf.png"));

        seedList.add(new UserData("firefly@gmail.com", "firefly123", "Firefly", "0909111222",
                "Glamoth, Căn cứ quân sự", "CUSTOMER",
                "https://res.cloudinary.com/ipc-media/image/upload/v1764319501/hbqh21rwcyn5rorjlz2h.png"));

        processUserAtIndex(0);
    }

    // ==================== BANK CONFIG ====================
    private void seedBankConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("savings_rate", 5.5);
        config.put("loan_rate", 7.5);

        db.collection("bank_config").document("rates")
                .set(config, SetOptions.merge());
    }

    private void seedBankBranches() {
        List<Map<String, Object>> branches = new ArrayList<>();
        branches.add(createBranchMap("IPC Main HQ (Bitexco)", "2 Hai Trieu, Ben Nghe, Q1", 10.771661, 106.704372, "8:00 - 17:00"));
        branches.add(createBranchMap("IPC Landmark 81", "720A Dien Bien Phu, Binh Thanh", 10.795005, 106.721846, "9:00 - 20:00"));
        branches.add(createBranchMap("IPC Independence Palace", "135 Nam Ky Khoi Nghia, Q1", 10.776996, 106.695333, "8:00 - 16:00"));
        branches.add(createBranchMap("IPC Ben Thanh Market", "Cho Ben Thanh, Le Loi, Q1", 10.772109, 106.698275, "8:00 - 18:00"));

        for (int i = 0; i < branches.size(); i++) {
            String docId = "branch_0" + (i + 1);
            db.collection("bank_branches").document(docId)
                    .set(branches.get(i), SetOptions.merge());
        }
    }

    private Map<String, Object> createBranchMap(String name, String address, double lat, double lng, String hours) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("address", address);
        map.put("latitude", lat);
        map.put("longitude", lng);
        map.put("opening_hours", hours);
        return map;
    }

    // ==================== BILLS SEED ====================
    private void seedBills() {
        List<Map<String, Object>> bills = new ArrayList<>();
        bills.add(createBill("Electricity", "PC0101", 150000));
        bills.add(createBill("Water", "WA0202", 80000));
        bills.add(createBill("Internet", "IN0303", 250000));

        for (Map<String, Object> bill : bills) {
            db.collection("bills").add(bill);
        }
    }

    private Map<String, Object> createBill(String type, String customerCode, double amount) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("customer_code", customerCode);
        map.put("amount", amount);
        map.put("status", "UNPAID");
        return map;
    }

    // ==================== MOVIE TICKET SEED ====================
    private void seedMovieData() {
        seedMovies();
        seedCinemas();
        seedCinemaBrands();
        seedShowtimes();
    }

    private void seedMovies() {
        List<Map<String, Object>> movies = new ArrayList<>();
        movies.add(createMovie("AVATAR01", "Avatar: Lửa và Tro Tàn", "https://nhipsongonline.vn/wp-content/uploads/2025/07/654ds.png", "Khoa Học Viễn Tưởng, Giả Tưởng, Phiêu Lưu", "13+", "3 giờ 15 phút", "19/12/2025", "Phụ đề", "Lửa sẽ thiêu rụi. Tro sẽ tái sinh..."));
        movies.add(createMovie("FIVENA02", "Năm Đêm Kinh Hoàng 2", "https://image.tmdb.org/t/p/w500/5gzzkR7y3hnY8AD1wXjCnVlHba5.jpg", "Kinh Dị", "18+", "1 giờ 45 phút", "10/11/2025", "Phụ đề", "Một câu chuyện kinh dị mới tại nhà hàng pizza bị ma ám."));

        for (Map<String, Object> movie : movies) {
            db.collection("movies").document((String) movie.get("movieId")).set(movie, SetOptions.merge());
        }
    }

    private Map<String, Object> createMovie(String movieId, String title, String posterUrl, String genre, String rating, String duration, String releaseDate, String language, String synopsis) {
        Map<String, Object> map = new HashMap<>();
        map.put("movieId", movieId);
        map.put("title", title);
        map.put("posterUrl", posterUrl);
        map.put("genre", genre);
        map.put("rating", rating);
        map.put("duration", duration);
        map.put("releaseDate", releaseDate);
        map.put("language", language);
        map.put("synopsis", synopsis);
        return map;
    }

    private void seedCinemas() {
        List<Map<String, Object>> cinemas = new ArrayList<>();
        cinemas.add(createCinema("CGVAEONBT", "CGV", "CGV Aeon Bình Tân", "https://aeonmall-review-rikkei.cdn.vccloud.vn/public/wp/15/editors/M8mnyzQqI7C2dPjGuG3GAsua5eQjgAwWj39jAfjh.png", "Quận Bình Tân, TP.HCM", "Tầng 3, Trung tâm thương mại Aeon...", 2.5));
        cinemas.add(createCinema("CGVCRESCENT", "CGV", "CGV Crescent Mall", "https://rapchieuphim.com/photos/2/cgv-crescent-mall-1.png", "Quận 7, TP.HCM", "Tầng 5, Crescent Mall, số 101 đường...", 8.1));

        for (Map<String, Object> cinema : cinemas) {
            db.collection("cinemas").document((String) cinema.get("cinemaId")).set(cinema, SetOptions.merge());
        }
    }

    private Map<String, Object> createCinema(String cinemaId, String brand, String name, String brandLogoUrl, String location, String address, double distance) {
        Map<String, Object> map = new HashMap<>();
        map.put("cinemaId", cinemaId);
        map.put("brand", brand);
        map.put("name", name);
        map.put("brandLogoUrl", brandLogoUrl);
        map.put("location", location);
        map.put("address", address);
        map.put("distance", distance);
        return map;
    }

    private void seedCinemaBrands() {
        List<CinemaBrand> brands = new ArrayList<>();
        brands.add(createCinemaBrand("CGV", "https://play-lh.googleusercontent.com/nxo4BC4BQ5hXuNi-UCdPM5kC0uZH1lq7bglINlWNUA_v8yMfHHOtTjhLTvo5NDjVeqx-"));
        brands.add(createCinemaBrand("Lotte Cinema", "https://www.lottecinemavn.com/LCCS/Image/thum/img_outline01_1.jpg"));
        brands.add(createCinemaBrand("Galaxy Cinema", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcREh6rPafuxrjDqFmgfbL31O0v4IuYy28BmiQ&s"));

        for (CinemaBrand brand : brands) {
            db.collection("cinema_brands").document(brand.getName()).set(brand, SetOptions.merge());
        }
    }

    private CinemaBrand createCinemaBrand(String name, String logoUrl) {
        CinemaBrand brand = new CinemaBrand();
        brand.setName(name);
        brand.setLogoUrl(logoUrl);
        return brand;
    }

    private void seedShowtimes() {
        List<Map<String, Object>> showtimes = new ArrayList<>();
        showtimes.add(createShowtime("SHOW101", "AVATAR01", "CGVAEONBT", "CGV Aeon Bình Tân", "2025-12-18T08:30:00", "2D Phụ đề", "145/146", "Cinema 6"));
        showtimes.add(createShowtime("SHOW102", "AVATAR01", "CGVAEONBT", "CGV Aeon Bình Tân", "2025-12-18T11:10:00", "2D Phụ đề", "161/162", "Cinema 6"));

        for (Map<String, Object> showtime : showtimes) {
            db.collection("showtimes").document((String) showtime.get("showtimeId")).set(showtime, SetOptions.merge());
        }
    }

    private Map<String, Object> createShowtime(String showtimeId, String movieId, String cinemaId, String cinemaName, String dateTime, String format, String availability, String auditorium) {
        Map<String, Object> map = new HashMap<>();
        map.put("showtimeId", showtimeId);
        map.put("movieId", movieId);
        map.put("cinemaId", cinemaId);
        map.put("cinemaName", cinemaName);
        map.put("dateTime", dateTime);
        map.put("format", format);
        map.put("availability", availability);
        map.put("auditorium", auditorium);
        return map;
    }

    // ==================== FLIGHT TICKET SEED ====================
    private void seedFlightData() {
        seedAirlines();
        seedAirports();
        seedFlights();
    }

    private void seedAirlines() {
        List<Map<String, Object>> airlines = new ArrayList<>();
        airlines.add(createAirline("VNA", "Vietnam Airlines", "https://static.wixstatic.com/media/9d8ed5_2e2d0b38daff4e92ac7cf7aa7375b1be~mv2.png/v1/fit/w_500,h_500,q_90/file.png"));
        airlines.add(createAirline("BAMBOO", "Bamboo Airways", "https://cdn.haitrieu.com/wp-content/uploads/2022/01/Logo-Bamboo-Airways-V.png"));
        airlines.add(createAirline("VIETJET", "Vietjet Air", "https://brasol.vn/wp-content/uploads/2022/08/logo-vietjet-air.jpg"));

        for (Map<String, Object> airline : airlines) {
            db.collection("airlines").document((String) airline.get("airlineId")).set(airline, SetOptions.merge());
        }
    }

    private Map<String, Object> createAirline(String airlineId, String name, String logoUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("airlineId", airlineId);
        map.put("name", name);
        map.put("logoUrl", logoUrl);
        return map;
    }

    private void seedAirports() {
        List<Map<String, Object>> airports = new ArrayList<>();
        airports.add(createAirport("SGN", "Tân Sơn Nhất", "Hồ Chí Minh", "Việt Nam"));
        airports.add(createAirport("HAN", "Nội Bài", "Hà Nội", "Việt Nam"));
        airports.add(createAirport("DAD", "Đà Nẵng", "Đà Nẵng", "Việt Nam"));
        airports.add(createAirport("HPH", "Cát Bi", "Hải Phòng", "Việt Nam"));
        airports.add(createAirport("VCA", "Cần Thơ", "Cần Thơ", "Việt Nam"));
        airports.add(createAirport("HUI", "Phú Bài", "Huế", "Việt Nam"));
        airports.add(createAirport("PQC", "Phú Quốc", "Kiên Giang", "Việt Nam"));
        airports.add(createAirport("BKK", "Suvarnabhumi", "Bangkok", "Thái Lan"));
        airports.add(createAirport("SIN", "Changi", "Singapore", "Singapore"));
        airports.add(createAirport("NRT", "Narita", "Tokyo", "Nhật Bản"));
        airports.add(createAirport("LAX", "Los Angeles International", "Los Angeles", "Hoa Kỳ"));
        airports.add(createAirport("JFK", "John F. Kennedy International", "New York", "Hoa Kỳ"));
        airports.add(createAirport("CDG", "Charles de Gaulle", "Paris", "Pháp"));
        airports.add(createAirport("FRA", "Frankfurt am Main", "Frankfurt", "Đức"));
        airports.add(createAirport("LHR", "Heathrow", "London", "Vương quốc Anh"));
        airports.add(createAirport("DXB", "Dubai International", "Dubai", "Các Tiểu vương quốc Ả Rập Thống Nhất"));
        airports.add(createAirport("ICN", "Incheon International", "Seoul", "Hàn Quốc"));
        airports.add(createAirport("HKG", "Hong Kong International", "Hồng Kông", "Trung Quốc"));
        airports.add(createAirport("SYD", "Kingsford Smith", "Sydney", "Úc"));
        airports.add(createAirport("AMS", "Schiphol", "Amsterdam", "Hà Lan"));


        for (Map<String, Object> airport : airports) {
            db.collection("airports").document((String) airport.get("airportCode")).set(airport, SetOptions.merge());
        }
    }

    private Map<String, Object> createAirport(String code, String name, String city, String country) {
        Map<String, Object> map = new HashMap<>();
        map.put("airportCode", code);
        map.put("name", name);
        map.put("city", city);
        map.put("country", country);
        return map;
    }

    private void seedFlights() {
        List<Map<String, Object>> flights = new ArrayList<>();

        Map<String, Object> flight1Classes = new HashMap<>();
        flight1Classes.put("economy", createFlightClass("Economy Saver Max", 1287000, 150, 120));
        flight1Classes.put("business", createFlightClass("Business Flex", 3500000, 20, 15));
        flights.add(createFlight("FL001", "BAMBOO", "QH202", "SGN", "HAN", "2025-12-15T23:25:00", "2025-12-16T01:35:00", 130, "Duy nhất 15.12 | nhập BAY15 giảm đến 100k", flight1Classes));

        Map<String, Object> flight2Classes = new HashMap<>();
        flight2Classes.put("economy", createFlightClass("Eco", 1163000, 180, 50));
        flight2Classes.put("skyboss", createFlightClass("SkyBoss", 4100000, 12, 5));
        flights.add(createFlight("FL002", "VIETJET", "VJ124", "SGN", "HAN", "2025-12-15T18:00:00", "2025-12-15T20:05:00", 125, null, flight2Classes));

        Map<String, Object> flight3Classes = new HashMap<>();
        flight3Classes.put("economy", createFlightClass("Phổ thông", 2100000, 160, 100));
        flights.add(createFlight("FL003", "VNA", "VN240", "SGN", "HAN", "2025-12-15T06:30:00", "2025-12-15T08:40:00", 130, "Giảm 20% cho thành viên Bông Sen Vàng", flight3Classes));

        for (Map<String, Object> flight : flights) {
            db.collection("flights").document((String) flight.get("flightId")).set(flight, SetOptions.merge());
        }
    }

    private Map<String, Object> createFlightClass(String name, double price, int totalSeats, int availableSeats) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("price", price);
        map.put("totalSeats", totalSeats);
        map.put("availableSeats", availableSeats);
        return map;
    }

    private Map<String, Object> createFlight(String flightId, String airlineId, String flightNumber, String depAirport, String arrAirport, String depTime, String arrTime, int duration, String promo, Map<String, Object> classes) {
        Map<String, Object> map = new HashMap<>();
        map.put("flightId", flightId);
        map.put("airlineId", airlineId);
        map.put("flightNumber", flightNumber);
        map.put("departureAirport", depAirport);
        map.put("arrivalAirport", arrAirport);
        map.put("departureTime", depTime);
        map.put("arrivalTime", arrTime);
        map.put("durationMinutes", duration);
        map.put("promoBadge", promo);
        map.put("classes", classes);
        map.put("isDirectFlight", true);
        return map;
    }

    // ==================== USER SEED ====================
    private void processUserAtIndex(int index) {
        if (index >= seedList.size()) {
            Log.d("FirebaseSeeder", "Users Seeded → Seeding Transactions...");
            seedTransactions();
            return;
        }
        UserData user = seedList.get(index);
        createOrUpdateUser(user, () -> processUserAtIndex(index + 1));
    }

    private void createOrUpdateUser(UserData u, Runnable onComplete) {
        auth.createUserWithEmailAndPassword(u.getEmail(), u.getPassword())
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) saveUserToFirestore(user.getUid(), u, onComplete);
                    else onComplete.run();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        auth.signInWithEmailAndPassword(u.getEmail(), u.getPassword())
                                .addOnSuccessListener(authResult -> {
                                    FirebaseUser user = authResult.getUser();
                                    if (user != null) saveUserToFirestore(user.getUid(), u, onComplete);
                                    else onComplete.run();
                                })
                                .addOnFailureListener(e2 -> onComplete.run());
                    } else onComplete.run();
                });
    }

    private void saveUserToFirestore(String uid, UserData u, Runnable onComplete) {
        createdUserIds.put(u.getEmail(), uid);

        Map<String, Object> data = new HashMap<>();
        data.put("full_name", u.getFullName());
        data.put("email", u.getEmail());
        data.put("phone_number", u.getPhoneNumber());
        data.put("address", u.getAddress());
        data.put("role", u.getRole());
        data.put("created_at", FieldValue.serverTimestamp());
        data.put("device_token", "");
        data.put("avatar_url", u.getAvatarUrl());

        boolean verified = u.getRole().equals("OFFICER") ||
                u.getEmail().contains("kafka") ||
                u.getEmail().contains("firefly");

        data.put("is_kyced", verified);
        data.put("kyc_status", verified ? "VERIFIED" : "UNVERIFIED");

        // ==================== KYC DATA ====================
        Map<String, Object> kyc = new HashMap<>();

        if (verified) {
            if (u.getEmail().contains("kafka")) {
                kyc.put("face_image_url", "https://res.cloudinary.com/ipc-media/image/upload/v1764142585/nwawkaoucf9mq0n1rtcp.png");
                kyc.put("id_card_url", "https://res.cloudinary.com/ipc-media/image/upload/v1764318945/zuzey4lxwoeavdmrhlmo.png");
                kyc.put("id_card_number", "079200040001");
            } else if (u.getEmail().contains("firefly")) {
                kyc.put("face_image_url", "https://res.cloudinary.com/ipc-media/image/upload/v1764142600/rhufwnt3zyvtr7xtuzyq.png");
                kyc.put("id_card_url", "https://res.cloudinary.com/ipc-media/image/upload/v1764320933/hokhd098u0iicbivmti2.png");
                kyc.put("id_card_number", "079200040002");
            } else {
                kyc.put("face_image_url", null);
                kyc.put("id_card_url", null);
                kyc.put("id_card_number", null);
            }
        } else {
            kyc.put("face_image_url", null);
            kyc.put("id_card_url", null);
            kyc.put("id_card_number", null);
        }

        data.put("kyc_data", kyc);

        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (u.getRole().equals("CUSTOMER")) {
                        seedAccountsForCustomer(uid, u.getEmail());
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> onComplete.run());
    }

    private void seedAccountsForCustomer(String uid, String email) {
        createAccount(uid, uid + "_CHECKING", "101" + uid.substring(0, 5).toUpperCase(),
                "CHECKING", 50000000.0, 0, 0);

        if (email.startsWith("kafka"))
            createAccount(uid, uid + "_SAVING", "202" + uid.substring(0, 5).toUpperCase(),
                    "SAVING", 200000000.0, 5.5, 0);

        if (email.startsWith("firefly"))
            createAccount(uid, uid + "_MORTGAGE", "303" + uid.substring(0, 5).toUpperCase(),
                    "MORTGAGE", -1000000000.0, 7.5, 15000000.0);
    }

    private void createAccount(String ownerId, String docId, String accNum, String type,
                               double balance, double rate, double monthlyPay) {

        Map<String, Object> map = new HashMap<>();
        map.put("owner_id", ownerId);
        map.put("account_number", accNum);
        map.put("account_type", type);
        map.put("balance", balance);
        map.put("created_at", FieldValue.serverTimestamp());

        if (!type.equals("CHECKING")) map.put("profit_rate", rate);
        if (type.equals("MORTGAGE")) map.put("monthly_payment", monthlyPay);

        db.collection("accounts").document(docId)
                .set(map, SetOptions.merge());
    }

    // ==================== TRANSACTION SEED ====================
    private void seedTransactions() {
        Log.d("FirebaseSeeder", "Seeding Transactions...");

        String kafkaUid = createdUserIds.get("kafka@gmail.com");
        String swUid = createdUserIds.get("silverwolf@gmail.com");
        String fireflyUid = createdUserIds.get("firefly@gmail.com");
        if (kafkaUid == null || swUid == null || fireflyUid == null) return;

        String kafkaAcc = "101" + kafkaUid.substring(0, 5).toUpperCase();
        String swAcc = "101" + swUid.substring(0, 5).toUpperCase();
        String fireflyAcc = "101" + fireflyUid.substring(0, 5).toUpperCase();

        List<Map<String, Object>> list = new ArrayList<>();

        // Internal Transfer
        list.add(createTransaction("TRANSFER", kafkaAcc, "Kafka",
                swAcc, "Silver Wolf",
                "IPC Bank", 500000, "Buy Stellaron Games"));

        list.add(createTransaction("TRANSFER", swAcc, "Silver Wolf",
                kafkaAcc, "Kafka",
                "IPC Bank", 150000, "Pay back dinner"));

        // Deposit from MoMo to IPC
        list.add(createTransaction("DEPOSIT", "EXTERNAL", "Kafka (MoMo)",
                kafkaAcc, "Kafka",
                "MoMo", 1000000, "Top up from MoMo"));

        // Withdrawal from IPC to Vietcombank
        list.add(createTransaction("WITHDRAWAL", swAcc, "Silver Wolf",
                "EXTERNAL", "Silver Wolf (Vietcombank)",
                "Vietcombank", 2000000, "Cash out to VCB"));

        for (Map<String, Object> t : list) {
            db.collection("transactions").add(t);
        }
        Log.d("FirebaseSeeder", "DONE!");
    }

    private Map<String, Object> createTransaction(
            String type,
            String senderAcc, String senderName,
            String receiverAcc, String receiverName,
            String counterpartyBank,
            double amount, String msg) {

        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("sender_account", senderAcc);
        map.put("sender_name", senderName);
        map.put("receiver_account", receiverAcc);
        map.put("receiver_name", receiverName);
        map.put("counterparty_bank", counterpartyBank);
        map.put("amount", amount);
        map.put("message", msg);
        map.put("status", "SUCCESS");
        map.put("created_at", FieldValue.serverTimestamp());
        return map;
    }
}
