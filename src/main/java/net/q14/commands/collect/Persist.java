package net.q14.commands.collect;


import net.q14.store.CsvHandler;
import net.q14.store.ItemData;
import net.q14.store.ItemsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;

/*
    Prints content of database into csv file
 */
@ShellComponent
public class Persist {
    @Value("${folder.source}")
    public String source;

    @Autowired
    CsvHandler csvFile;

    @Autowired
    ItemsRepository itemsRepository;

    private long counter = 0;

    @ShellMethod(key = "persist", value="Writes database content into editable CSV", group = "collect")
    public void shellMethod() {

        this.csvFile.backup();
        this.csvFile.reset();

        this.itemsRepository.findAll().forEach(id -> {
            try {
                this.csvFile.store(id);
                if ((this.counter++ % 1000) == 0) this.csvFile.getPrinter().flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

}
