package net.q14.commands;

import lombok.extern.java.Log;
import net.q14.store.CsvHandler;
import net.q14.store.ItemsRepository;
import org.hibernate.annotations.AttributeAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@Log
public class Count {

    @Autowired
    CsvHandler csv;

    @Autowired
    ItemsRepository db;



    @ShellMethod(key="count", value = "Counts the number of items in the sheet")
    public void run() {
        log.info(db.count() + " items in the database.");
        log.info(csv.all().size() + " items in the sheet.");


    }

}
