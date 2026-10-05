package antafes.sfmanager.entity;

import antafes.sfmanager.entity.item.Production;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Item
{
    private String key;
    private Name name;
    private String icon;
    private List<Production> productions;
}
