package antafes.sfmanager.repository.xml.productions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "productions")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductionsData
{
    @XmlElement(name = "production")
    public List<ProductionData> productions = new ArrayList<>();
}
