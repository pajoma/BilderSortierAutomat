package net.q14.commands.collect;

import lombok.extern.java.Log;
import net.q14.store.CsvHandler;
import net.q14.store.ItemsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;

@ShellComponent
@Log
public class Load {

    @Value("${folder.source}")
    public String source;

    @Autowired
    CsvHandler csvFile;

    @Autowired
    ItemsRepository itemsRepository;

    private long counter = 0;

    @ShellMethod(key = "load", value="Loads items from CSV file into database", group = "collect")
    public void shellMethod() throws IOException {
        this.itemsRepository.deleteAll();
        log.info("Database has been emptied");


        this.csvFile.loadFile();

        this.itemsRepository.saveAll(this.csvFile.all());


        log.info("Items loaded from file");
    }
}
