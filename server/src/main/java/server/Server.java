package server;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.GeoPoint;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import constants.Constants;
import io.javalin.Javalin;
import message.Message;
import message.MessageFinder;
import message.MessageFinderImpl;
import responses.MessagesResponse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class Server {
    private static final Gson gson = new Gson();

    private static Javalin app;
    private static MessageFinder messageFinder;

    private static void setup() {
        FileInputStream serviceAccount;
        FirebaseOptions firebaseOptions;

        try {
            serviceAccount = new FileInputStream(Constants.FIREBASE_SERVICE_ACCOUNT_FILE);
            firebaseOptions = FirebaseOptions
                    .builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(Constants.FIRESTORE_URL)
                    .build();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find ServiceAccount file");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Firestore");
        }

        FirebaseApp.initializeApp(firebaseOptions);

        Firestore firestore = FirestoreClient.getFirestore();

        app = Javalin.create().start(Constants.PORT);
        messageFinder = new MessageFinderImpl(firestore);
    }

    public static void start() {
        setup();

        app.get("/messages", ctx -> {
            Double latitudeTop = ctx.queryParam("latitude_top", Double.class).get();
            Double longitudeLeft = ctx.queryParam("longitude_left", Double.class).get();
            Double latitudeBottom = ctx.queryParam("latitude_bottom", Double.class).get();
            Double longitudeRight = ctx.queryParam("longitude_right", Double.class).get();
            Integer maxRecords = ctx.queryParam("max_records", Integer.class).get();

            System.out.println("Getting messages for latitude_top " + latitudeTop + ", latitude_bottom " +
                    latitudeBottom + ", longitude_left " + longitudeLeft + ", latitude_bottom " + latitudeBottom +
                    ", max_records " + maxRecords);

            if (latitudeBottom < -90 || latitudeBottom > 90) {
                ctx.result("Invalid latitude_bottom");
            } else if (latitudeTop < -90 || latitudeTop > 90) {
                ctx.result("Invalid latitude_top");
            } else if (longitudeLeft < -180 || longitudeLeft > 180) {
                ctx.result("Invalid longitude_left");
            } else if (longitudeRight < -180 || longitudeRight > 180) {
                ctx.result("Invalid longitude_right");
            } else {
                GeoPoint lesserPoint = new GeoPoint(latitudeBottom, longitudeLeft);
                GeoPoint greaterPoint = new GeoPoint(latitudeTop, longitudeRight);

                if (lesserPoint.getLatitude() > greaterPoint.getLatitude()) {
                    ctx.result("Passed bottom_latitude that is greater than top_latitude");
                } else if (lesserPoint.getLongitude() > greaterPoint.getLongitude()) {
                    ctx.result("Passed left_longitude that is greater than right_latitude");
                } else {
                    List<Message> messages = messageFinder.findByLongitudeAndLatitude(
                            lesserPoint,
                            greaterPoint,
                            maxRecords
                    );

                    ctx.result(gson.toJson(new MessagesResponse(messages)));
                }
            }
        });

        app.get("/messages/:user_id", ctx -> {
            String userId = ctx.pathParam("user_id");

            System.out.println("Getting messages for user_id " + userId);
            List<Message> messages = messageFinder.findByUserId(userId);

            ctx.result(gson.toJson(new MessagesResponse(messages)));
        });
    }

    public static void stop() {
        app.stop();
    }

    public static void main(String[] args) {
        start();
    }
}