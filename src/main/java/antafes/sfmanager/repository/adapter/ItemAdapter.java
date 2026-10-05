package antafes.sfmanager.repository.adapter;

import antafes.sfmanager.entity.Item;
import antafes.sfmanager.repository.ItemRepository;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ItemAdapter extends XmlAdapter<String, Item>
{
    private final ItemRepository repository;

    @Override
    public String marshal(Item item) throws Exception
    {
        return item.getKey();
    }

    @Override
    public Item unmarshal(String key) throws Exception
    {
        return this.repository.findOne(key);
    }
}
