package server;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import parser.FHEMParser;
import parser.fhemJson.*;

/**
 * Created by Rafael on 31.05.17.
 */
class Server {

    private static final FHEMParser parser = FHEMParser.getInstance();

    public static void main(String[] args) {
        try {
            String jsonString = new String(Files.readAllBytes(Paths.get("jsonList2.json")));
            JsonList2 list = parser.parse(jsonString);

            for (FHEMDevice d : list.getResults().get()) {
                d.isSensor();
                System.out.println(d.getName());
                System.out.println(d.isFileLog());
            }
            /* The other way around - just for testing.
            *  (Convert objects to json again and print)
            * */
            Gson gson = new Gson();
            System.out.println(gson.toJson(list));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
