package antafes.sfmanager.repository.adapter;

import antafes.sfmanager.entity.Building;
import antafes.sfmanager.repository.BuildingRepository;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BuildingAdapter extends XmlAdapter<String, Building>
{
    private final BuildingRepository repository;

    @Override
    public String marshal(Building building) throws Exception
    {
        return building.getKey();
    }

    @Override
    public Building unmarshal(String key) throws Exception
    {
        return this.repository.findOne(key);
    }
}
