package uz.falconmobile.bilim_scan.catalog.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "fanlar")
public class Fan {
    @Id
    private String id;
    private String name;
    private Kafedra kafedra;
}
