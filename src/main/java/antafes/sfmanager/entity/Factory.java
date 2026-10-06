package antafes.sfmanager.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class Factory
{
    private UUID id;
    private String name;
    private List<String> productions = new ArrayList<>();
}
