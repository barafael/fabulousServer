package server;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import parser.FHEMParser;
import parser.fhemJson.JsonList2;

/**
 * Created by Rafael on 31.05.17.
 */
public class Server {

    private static FHEMParser parser = FHEMParser.getInstance();

    public static void main(String[] args) {

        try {
            String jsonString = new String(Files.readAllBytes(Paths.get("temp.json")));
            System.out.println(jsonString);
            JsonList2 list = parser.parse(jsonString);

            Gson gson = new Gson();
            System.out.println(gson.toJson(list));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
