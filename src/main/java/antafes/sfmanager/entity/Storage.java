package antafes.sfmanager.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class Storage
{
    private UUID factoryId;
    private List<Entry> entries = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class Entry
    {
        private UUID productionId;
        private String itemKey;
    }
}
