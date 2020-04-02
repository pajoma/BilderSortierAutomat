package net.q14.store;

/* This class represents all identified items. Anything is stored in a (editable) CSV Backend
 */

import lombok.extern.java.Log;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@Log
public class CsvHandler {


    public void backup() {
    }

    enum Headers {
        name, path, created, modified, taken, size, width, height, hash, duplicate, labels, migrate
    }

    @Value("${store.file}")
    String filename;

    @Value("${folder.source}")
    String folder;

    Path folderPath;

    List<ItemData> items;

    CSVPrinter printer;

    public void loadFile() throws IOException {

        log.info("Loading CSV Data from " + filename);
        items = new ArrayList<>();


        try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(filename))) {
            Iterable<CSVRecord> parsed = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(bufferedReader);

            for (CSVRecord it : parsed) {
                if(it.get(Headers.name).equalsIgnoreCase("name")) continue;

                ItemData id = ItemData.builder()
                        .filename(it.get(Headers.name))
                        .creationTime(LocalDateTime.parse(it.get(Headers.created)))
                        .build();

                items.add(id);
            }

        } catch (IOException e) {
            System.err.println("Failed to load CSV Data");
            System.err.println(e.getLocalizedMessage());
            throw e;
        }

    }

    public void reset() {
        try {
            Files.newBufferedWriter(Paths.get(folder, filename)).close();
        } catch (IOException e) {
            System.err.println("Failed to reset");
        }

    }


    @PostConstruct
    public void init() throws IOException {
        try {
            this.folderPath = Paths.get(this.folder);
            OpenOption openOption =  StandardOpenOption.CREATE;

            if (Files.exists(Paths.get(filename))) {
                this.loadFile();
                openOption = StandardOpenOption.APPEND;
            }

            BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(filename), openOption);
            this.printer = CSVFormat.DEFAULT.withHeader(Headers.class).print(bufferedWriter);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @PreDestroy
    public void clean() throws IOException {
        this.printer.close();
    }

    public List<ItemData> all() {
        return this.items;
    }

    public void store(ItemData currentData) throws IOException {
        List<Object> values = new ArrayList();
        values.add(currentData.filename);
        //values.add(this.folderPath.relativize(Paths.get(currentData.path).getParent()));
        values.add(currentData.getRelativeSourcePath());
        values.add(currentData.getCreationTime());
        values.add(currentData.getModifiedTime());
        values.add(currentData.getDateTaken());
        values.add(currentData.getSize());
        values.add(currentData.getImageWidth());
        values.add(currentData.getImageHeight());
        values.add(currentData.hashCode());

        printer.printRecord(values);
    }

    public CSVPrinter getPrinter() {
        return this.printer;
    }


}
